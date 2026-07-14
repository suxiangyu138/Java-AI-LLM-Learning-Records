# Redis集群大批量查询删除优化技术笔记

## 1. 异常问题分析与解决

### 1.1 连接异常现象
在生产环境中频繁出现socket异常和read timeout问题，而测试环境少量数据时运行正常。这种环境差异性表明问题出现在高并发或大数据量场景下的连接稳定性上。

### 1.2 连接池配置优化
通过调整JedisPoolConfig的关键参数解决连接异常问题：
```java
JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
// 连接池容量配置
jedisPoolConfig.setMaxIdle(100);        // 最大空闲连接数
jedisPoolConfig.setMaxTotal(500);       // 最大连接数
jedisPoolConfig.setMinIdle(0);          // 最小空闲连接数
jedisPoolConfig.setMaxWaitMillis(600*1000); // 最大等待时间60秒
// 连接校验配置
jedisPoolConfig.setTestOnBorrow(true);  // 获取连接时校验
jedisPoolConfig.setTestWhileIdle(true); // 空闲时校验
jedisPoolConfig.setTestOnReturn(true);  // 归还连接时校验
```

### 1.3 JedisCluster初始化
采用双检锁模式确保线程安全的单例初始化：
```java
private boolean initRedisCon() {
    if(jedisCluster == null) {
        synchronized (HhHeadService.class) {
            if(jedisCluster == null) {
                Set<HostAndPort> nodes = new LinkedHashSet<>();
                // 配置集群节点
                String[] split = redis_cluter.split(",");
                for (String s : split) {
                    String[] split1 = s.split(":");
                    nodes.add(new HostAndPort(split1[0], Integer.valueOf(split1[1])));
                }
                jedisCluster = new JedisCluster(nodes, 0, 3, jedisPoolConfig);
            }
        }
    }
    return true;
}
```

## 2. 性能优化实践

### 2.1 原始性能瓶颈分析
**业务背景**：
- Redis集群环境：3主3从架构
- 数据规模：每个主节点约300万业务key，String类型value
- 业务需求：每10分钟获取所有相关key-value，整理成list实体推送
- 分批处理：每2万条数据为一批进行推送
    **性能表现**：
- 获取2万key耗时：2-3秒
- 推送2万key耗时：2-3秒
- 总处理时间无法满足业务频率要求

### 2.2 优化策略与实现

#### 2.2.1 Pipeline管道优化数据获取
采用Pipeline机制显著减少网络往返时间：
```java
// 获取所有集群节点
Map<String, JedisPool> nodesMap = jedisCluster.getClusterNodes();
for(String nodeStr : nodesMap.keySet()) {
    JedisPool pool = nodesMap.get(nodeStr);
    try(Jedis jedis = pool.getResource();
        Pipeline pipe = jedis.pipelined()) {
        if (!jedis.info("replication").contains("role:slave")) {
            Set<String> keys = jedis.keys(pattern);
            Map<String, Response<String>> dateMap = new HashedMap();
            // 使用pipeline批量获取value
            for(String key : keys) {
                dateMap.put(key, pipe.get(key));
                if (dateMap.size() > eachRedisNum) {
                    allDateMapList.add(dateMap);
                    dateMap = new HashedMap();
                }
            }
            pipe.sync(); // 批量执行
        }
    }
}
```

#### 2.2.2 多线程并发处理
通过线程池实现数据推送的并行处理：
```java
int times = 5; // 线程数
int threadNum = allDateMapList.size() / times;
List<Thread> allThreadList = new ArrayList<>();
for(int i = 0; i < times; i++) {
    int start = i * threadNum;
    int end = (i + 1) * threadNum;
    if(i == times - 1) end = allDateMapList.size() - 1;
    List<Map<String, Response<String>>> subDateMapList = allDateMapList.subList(start, end);
    Thread oneThread = new Thread(() -> {
        for(Map<String, Response<String>> oneDateMap : subDateMapList) {
            initAndSend(redisSign, oneDateMap, todayYMS); // 推送数据
            // 统计计数
            synchronized (HhHeadService.class) {
                jedisCluster.incrBy(keyName, oneDateMap.size());
            }
        }
    });
    oneThread.start();
    allThreadList.add(oneThread);
}
// 等待所有线程完成
for(Thread oneThread : allThreadList) {
    oneThread.join();
}
```

### 2.3 优化效果对比
| 指标 | 优化前 | 优化后 | 提升倍数 |
|------|--------|--------|----------|
| 单节点300万数据获取时间 | 未明确 | 3-5秒 | 显著提升 |
| 数据处理并发度 | 单线程 | 多线程(3并发) | 3倍 |
| 网络往返次数 | O(n) | O(1) per batch | 大幅减少 |

## 3. 批量删除技术难点

### 3.1 Redis集群删除限制
在Redis集群环境下，批量删除面临以下技术限制：
1. **哈希槽一致性要求**：Redis集群要求在同一命令中操作的key必须属于同一个哈希槽
2. **跨槽操作异常**：
   - `JedisClusterException: No way to dispatch this command to Redis Cluster because keys have different slots.`
   - `JedisDataException: CROSSSLOT Keys in request don't hash to the same slot`

### 3.2 解决方案要点
- **数据插入策略**：造测试数据时必须使用集群方式插入，确保哈希槽分布正确
- **分槽处理**：需要按哈希槽分组进行批量删除操作
- **Pipeline限制**：即使在Pipeline中也不能混合不同哈希槽的key

## 4. 架构设计启示

### 4.1 环境差异性处理
- 测试环境与生产环境的配置差异需要提前考虑
- 连接池参数需要根据实际负载进行调整
- 超时和重试机制在大数据量场景下尤为重要

### 4.2 性能优化层次
1. **连接层优化**：合理的连接池配置
2. **网络层优化**：Pipeline减少网络往返
3. **处理层优化**：多线程并行处理
4. **业务层优化**：合理的分批策略

### 4.3 团队协作价值
博客作者强调在技术卡壳时与同事讨论的重要性，体现了技术问题解决过程中团队协作和思路碰撞的价值。这种协作不仅解决了具体的技术问题，还可能产生新的优化思路和解决方案。
该优化方案通过连接池配置优化、Pipeline批量操作和多线程并发处理的组合策略，成功解决了Redis集群在大数据量场景下的性能和稳定性问题，为类似的大规模Redis应用提供了可借鉴的技术实践。
