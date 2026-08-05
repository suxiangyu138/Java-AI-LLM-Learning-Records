# SpringCloud微服务 面试宝典
> 基于黑马SpringCloud微服务课程大纲，全面覆盖面试高频考点，助力一线大厂面试

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答

> 高频面试题，每个问题要求在30秒内清晰回答核心要点。

### 1. 什么是微服务架构？
微服务架构是一种将单一应用程序划分为一组小服务的架构风格，每个服务运行在独立的进程中，围绕业务能力构建，采用轻量级通信机制（通常是HTTP RESTful API或RPC）。每个服务可以独立开发、部署和扩展，使用不同的技术栈和数据存储。与单体架构相比，微服务提升了系统的灵活性和可伸缩性，但也带来了分布式系统的复杂性，如服务发现、配置管理、分布式事务等问题。

### 2. SpringCloud与SpringCloud Alibaba核心组件对比

| 功能领域 | Spring Cloud | Spring Cloud Alibaba |
|---------|-------------|---------------------|
| 注册中心 | Eureka（已停更） | Nacos（功能更强） |
| 配置中心 | Spring Cloud Config | Nacos Config |
| 服务调用 | Feign + Ribbon | Feign + Nacos LoadBalancer |
| 熔断降级 | Hystrix（已停更） | Sentinel（功能更全面） |
| 网关 | Spring Cloud Gateway | Spring Cloud Gateway |
| 分布式事务 | 无 | Seata |
| 消息驱动 | Spring Cloud Stream | RocketMQ |

> 💡 **面试点**：Spring Cloud Alibaba已经是国内微服务事实标准，Nacos替代Eureka + Config，Sentinel替代Hystrix。

### 3. Eureka的自我保护机制是什么？
当Eureka Server在短时间内（默认15分钟内）丢失超过85%的客户端心跳时，会进入自我保护模式。在此模式下，Eureka不再剔除任何服务实例，而是保留所有注册信息等待网络恢复。这是基于AP设计理念的容错机制——优先保证可用性和分区容错性，牺牲一致性。生产环境中通常建议保留自我保护机制，避免网络抖动导致大规模误剔除。

### 4. Nacos的CP/AP切换原理是什么？
Nacos支持在CP（一致性）和AP（可用性）两种模式之间动态切换，核心取决于集群中注册的是临时实例还是持久化实例。当所有实例都是临时实例时，Nacos采用AP模式，使用Distro协议保证最终一致性；当注册了持久化实例时，Nacos自动切换到CP模式，采用Raft协议保证强一致性。配置方式是通过`spring.cloud.nacos.discovery.ephemeral=true/false`控制。

> ⚠️ 默认值为true（临时实例），绝大多数生产场景使用AP模式。

### 5. Nacos与Eureka的核心区别有哪些？

| 对比维度 | Nacos | Eureka |
|---------|------|--------|
| 一致性 | 支持CP/AP切换 | AP，弱一致性 |
| 健康检查 | 心跳 + TCP/HTTP主动探测 | 仅心跳 |
| 配置管理 | 内置配置中心 | 需要Config Server |
| 负载均衡 | 支持权重路由 | Round-Robin |
| 管理界面 | 功能丰富的控制台 | 基础页面 |
| 自动注销 | 支持主动摘除不健康实例 | 自我保护期不剔除 |
| 社区活跃度 | 阿里巴巴维护，持续更新 | Netflix维护，已停更 |

### 6. Ribbon的负载均衡策略有哪些？
Ribbon提供了8种内置负载均衡策略：`RoundRobinRule`（轮询）、`RandomRule`（随机）、`RetryRule`（重试，失败后重试下一个）、`WeightedResponseTimeRule`（根据响应时间加权）、`BestAvailableRule`（最小并发数）、`AvailabilityFilteringRule`（过滤宕机+高并发实例后轮询）、`ZoneAvoidanceRule`（区域感知，默认策略）、`ClientConfigEnabledRoundRobinRule`（客户端自定义）。在实际面试中，重点需要说明`ZoneAvoidanceRule`的区域亲和性策略以及如何开启**饥饿加载**（`ribbon.eager-load.enabled=true` + 指定服务名）来避免首次请求慢的问题。

### 7. Gateway与Zuul的区别是什么？
Spring Cloud Gateway基于**WebFlux**（Reactive编程模型），底层使用Netty，支持非阻塞异步IO，性能远优于Zuul。Zuul 1.x基于Servlet，是阻塞IO模型，每个请求占用一个线程。Gateway还支持WebSocket、更灵活的路由谓词工厂（Path/Header/Query/Cookie等）、内置过滤器链和更细粒度的限流支持。Zuul 2.x虽然也改为Netty，但由于Netflix内部问题迟迟未发布稳定版，Gateway已成为主流选择。

### 8. Sentinel与Hystrix的核心区别

| 对比维度 | Sentinel | Hystrix |
|---------|---------|---------|
| 隔离方式 | 信号量隔离（默认） | 线程池隔离 + 信号量 |
| 熔断策略 | 慢调用比例/异常比例/异常数 | 断路器（基于阈值） |
| 实时统计 | 滑动窗口（秒级精度） | 滚动窗口（分钟级） |
| 动态规则 | 支持Nacos/Push API动态下发 | 仅配置文件 |
| 限流能力 | 内置流控、热点、系统规则 | 需额外集成 |
| 控制台 | 功能完善，可直接操作规则 | Hystrix Dashboard只展示监控 |
| 资源粒度 | 支持方法/URL/参数级 | 仅方法级 |

### 9. Seata的AT模式实现原理是什么？
AT模式是Seata的侵入性最小的分布式事务方案，对业务代码基本无侵入。核心分为两个阶段：第一阶段，TM向TC注册全局事务，RM向TC注册分支事务并执行本地SQL，同时生成undo log回滚日志，直接提交本地事务。第二阶段，TC根据所有RM的执行结果决定全局commit或rollback：commit时异步删除undo log；rollback时通过undo log逆向生成补偿SQL恢复数据。AT模式依赖**全局锁**来保证写隔离性，适用于对一致性要求较高的金融场景。

### 10. Elasticsearch的倒排索引是什么？
倒排索引是ES实现快速全文搜索的核心数据结构。传统正排索引以文档ID为key映射到文档内容，而倒排索引以**词条（Term）** 为key映射到包含该词条的文档ID列表。ES在写入文档时，经过分词器（Analyzer）将文本拆分为词条，构建词典（Term Dictionary）和倒排列表（Posting List）。查询时只需在词典中查找词条，即可通过倒排列表直接定位到所有匹配文档，时间复杂度接近O(1)。倒排索引配合TF-IDF/BM25相关性算分算法，构成了ES搜索能力的基石。

### 11. Docker与虚拟机的核心区别
Docker是**操作系统级虚拟化**，共享宿主机内核，通过Namespace实现资源隔离，通过Cgroups实现资源限制。虚拟机是**硬件级虚拟化**，每个VM包含完整的Guest OS，通过Hypervisor虚拟化硬件。Docker的优势在于：启动时间毫秒级（vs VM分钟级）、镜像大小MB级（vs VM GB级）、资源利用率更高、部署密度可达VM的10倍以上。但Docker的隔离性弱于VM，因为所有容器共享宿主机内核，存在逃逸风险。

### 12. RabbitMQ的五种消息模型分别是什么？
RabbitMQ支持五种消息模型：**1）简单队列**（一个生产者一个消费者，直接使用默认交换机）；**2）Work Queue**（多个消费者竞争消费，支持轮询分发和公平分发）；**3）发布/订阅模式**（Fanout Exchange，广播给所有绑定队列）；**4）路由模式**（Direct Exchange，根据routing key精确匹配）；**5）主题模式**（Topic Exchange，支持通配符`*`匹配单词、`#`匹配零或多个单词）。实际项目中最常用的是Topic Exchange，因为它支持灵活的路由规则，可以实现消息的分类和分级。

### 13. CAP定理在分布式系统中的含义是什么？
CAP定理由Eric Brewer提出，指分布式系统最多只能同时满足三个特性中的两个：**一致性（Consistency）**——所有节点在同一时刻看到相同数据；**可用性（Availability）**——每个请求都能获得非错误响应；**分区容错性（Partition Tolerance）**——系统在网络分区时仍能正常工作。由于网络分区在分布式系统中不可避免，实际上必须在C和A之间权衡。典型选择：Eureka放弃C选择AP，ZooKeeper放弃A选择CP，Nacos支持CP/AP动态切换。

> 🎯 **面试重点**：不能笼统说"选CA"或"选CP"，要结合具体业务场景分析，比如支付系统偏CP、内容系统偏AP。

### 14. Redis三种高可用方案的区别
| 方案 | 原理 | 自动故障转移 | 数据一致性 | 适用场景 |
|-----|------|------------|-----------|---------|
| 主从复制 | 一主多从，异步复制 | 否，需手动切换 | 弱一致性，可能丢数据 | 读写分离，冷备 |
| 哨兵模式 | Sentinel监控+自动故障转移 | 是 | 最终一致性 | 中小规模高可用 |
| 集群模式 | 数据分片+去中心化 | 是 | 最终一致性 | 大规模高吞吐 |

### 15. 多级缓存架构中Caffeine的作用是什么？
Caffeine是基于Java 8的高性能本地缓存库，使用**W-TinyLFU**淘汰算法（近最优命中率），性能远超Guava Cache。在多级缓存架构中，Caffeine作为**一级缓存（JVM进程内缓存）**，响应时间在微秒级，远快于Redis的毫秒级。查询路径为：Caffeine（L1）→ Redis（L2）→ 数据库（L3）。Caffeine配置要点：设置合理的初始容量和最大容量，开启recordStats做命中率监控，根据业务设置过期策略（expireAfterWrite/expireAfterAccess）。

---

## 二、深度原理剖析

> 面试中拉开差距的部分，要求深入底层原理并结合源码分析。

### 1. Nacos服务注册的多级存储模型

Nacos将服务数据组织为三级分层模型：**Service → Cluster → Instance**。Service是业务层面的服务标识，Cluster表示该服务在不同数据中心的部署单元（如杭州集群、上海集群），Instance是具体的IP:Port节点。这种设计实现了**同集群优先调用**——客户端在服务发现时，优先返回同集群内的实例列表，跨集群调用作为兜底。

```java
// Nacos服务端核心模型
public class Service {
    private String name;                          // 服务名
    private Map<String, Cluster> clusterMap;      // 集群名 -> 集群
}
public class Cluster {
    private String name;                          // 集群名
    private Set<Instance> persistentInstances;    // 持久化实例
    private Set<Instance> ephemeralInstances;      // 临时实例
}
```

> 💡 多级存储配合Nacos的权重路由，可以实现精细化的灰度发布——给新版本实例设置较小权重，逐步放量。

### 2. Nacos心跳机制与健康检查

Nacos临时实例每**5秒**发送一次心跳到服务端，心跳请求包含服务名、IP、端口等信息。服务端在15秒内未收到心跳则将该实例标记为**不健康**，30秒未收到心跳则彻底摘除该实例。不同于Eureka的纯心跳模式，Nacos同时支持服务端主动健康检查——通过TCP或HTTP方式定期探测实例的健康状态。

```yaml
# Nacos客户端心跳配置
spring:
  cloud:
    nacos:
      discovery:
        ephemeral: true          # 临时实例
        heart-beat-interval: 5000     # 心跳间隔(ms)
        heart-beat-retry: 3          # 重试次数
        ip-delete-timeout: 30000     # 实例摘除超时(ms)
```

### 3. Nacos服务拉取与订阅机制（UDP推+拉）

Nacos的服务发现采用了**拉取+UDP推送**结合的机制。客户端首次启动时向服务端发起HTTP轮询请求，获取完整实例列表，同时建立一个**长轮询连接**。当服务端实例列表发生变化时，通过UDP连接主动推送变更给客户端（"推"模式），客户端收到变更通知后立即发起拉取请求获取最新数据。如果UDP推送失败（如网络丢包），客户端会在下一轮拉取（默认10秒）中自行补偿。

```
首次拉取: Client --HTTP GET--> Nacos Server (全量实例列表)
订阅变更: Client --HTTP 长轮询--> Nacos Server (等待变更)
推送变更: Nacos Server --UDP--> Client (通知有变更)
补偿拉取: Client --HTTP GET--> Nacos Server (拉取最新数据)
```

### 4. Sentinel ProcessorSlotChain责任链

Sentinel的核心架构是**ProcessorSlotChain**责任链模式，每个Slot负责一个独立的处理逻辑，按照固定顺序执行：

```text
NodeSelectorSlot(资源入口构建)
     → ClusterBuilderSlot(集群资源统计)
     → LogSlot(日志记录)
     → StatisticSlot(统计指标收集)
     → AuthoritySlot(授权规则校验)
     → SystemSlot(系统自适应保护)
     → FlowSlot(流控规则校验)
     → DegradeSlot(熔断降级规则校验)
     → ParamFlowSlot(热点参数限流)
```

每个Slot都有`entry()`和`exit()`两个方法，在请求进入和退出时执行相应逻辑。开发者可以通过SPI机制自定义Slot插入到链中任意位置，实现扩展。

> 🎯 **面试常考**：FlowSlot中流控规则的判断逻辑——调用FlowRuleChecker.checkFlow()，从clusterConfig中获取统计的Node，然后比较当前QPS与设定的阈值。

### 5. Sentinel滑动窗口算法实现流控

Sentinel使用滑动窗口（Sliding Window）实现秒级精度的流量统计。将时间划分为固定长度的时间片（默认500ms），每个时间片包含一个WindowWrap对象，存储该时间片的统计值（passQps、blockQps等）。滑动窗口通过**循环复用**时间片数组来避免频繁创建对象：

```java
public class LeapArray<T> {
    private int windowLengthMs;       // 窗口长度，默认500ms
    private int sampleCount;          // 窗口数，默认2
    private AtomicReferenceArray<WindowWrap<T>> array;  // 循环数组
    
    public WindowWrap<T> currentWindow() {
        long time = System.currentTimeMillis();
        int idx = calculateTimeIdx(time);         // 计算当前时间在数组中的索引
        long start = calculateWindowStart(time);   // 计算当前窗口的开始时间
        
        WindowWrap<T> old = array.get(idx);
        if (old == null) {
            // 创建新窗口并CAS写入
        } else if (start == old.windowStart()) {
            return old;  // 命中当前窗口
        } else {
            // 窗口已过期，重置
        }
    }
}
```

滑动窗口相比固定窗口的优势在于能够**平滑统计**，避免固定窗口在边界处出现流量突刺（如第59秒1000请求、第1秒1000请求，固定窗口都是达标，但实际是2000QPS）。

### 6. Sentinel漏桶算法实现

漏桶算法在Sentinel中用于实现**排队等待**模式的流量整形。漏桶的核心思想是：水以恒定速率从桶底流出（请求被匀速处理），水以任意速率流入（请求到达速率可变）。当桶满时，新流入的水溢出（请求被拒绝）：

```java
// 漏桶模式的核心配置
FlowRule rule = new FlowRule();
rule.setResource("leaky-bucket-api");
rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
rule.setCount(100);                    // 恒定的流出速率
rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER); // 漏桶模式
rule.setMaxQueueingTimeMs(500);        // 最大排队等待时间
```

当请求到达时，Sentinel计算期望通过时间（当前时间 + 1000/QPS），如果期望时间超过当前时间 + maxQueueingTimeMs，则拒绝该请求；否则让请求等待到期望时间再通过。

> 💡 漏桶算法适合保护下游系统不被突发流量冲垮，比如保护数据库写入接口。令牌桶（`CONTROL_BEHAVIOR_WARM_UP`）则适合应对突发流量场景。

### 7. Seata AT模式两阶段实现

AT模式的核心在于**数据源代理**和**undo log**的结合。

**第一阶段（Branch Registering and Local Commit）：**
1. TM向TC申请全局事务ID（XID），并通过RootContext绑定到当前线程
2. RM拦截业务SQL，解析SQL语义生成**前镜像**（before image，修改前数据快照）
3. 执行业务SQL，再生成**后镜像**（after image）
4. 生成undo log（包含前/后镜像、表名、SQL type等信息），和业务SQL在同一个本地事务中提交
5. RM向TC注册分支事务

**第二阶段（Global Commit or Rollback）：**
- **Commit**：TC通知所有RM异步删除undo log，资源释放
- **Rollback**：TC通知RM，RM从undo log读取before image，生成补偿SQL（update set x = before_image where pk = ?），通过数据源代理执行回滚前，会校验after image是否被其他事务修改（脏写检测），如果被修改则抛出异常需要人工介入

```java
// 第一阶段生成的undo log
{
  "branchId": 12345,
  "sqlUndoLogs": [{
    "tableName": "account",
    "beforeImage": {"rows": [{"id": 1, "money": 100}]},
    "afterImage": {"rows": [{"id": 1, "money": 80}]}
  }]
}
```

### 8. Seata TCC模式Try-Confirm-Cancel

TCC是Seata提供的**侵入性较高但性能更好**的分布式事务方案，要求业务系统实现三个接口：

| 阶段 | 行为 | 业务示例（转账） |
|-----|------|----------------|
| Try | 预留业务资源 | 冻结账户中的扣减金额，状态设为TRYING |
| Confirm | 确认执行业务操作 | 将冻结金额正式扣除，状态设为CONFIRMED |
| Cancel | 取消预留资源 | 解冻冻结金额，恢复可用余额 |

```java
@LocalTCC
public interface AccountTCCService {
    @TwoPhaseBusinessAction(
        name = "deduct",
        commitMethod = "confirm",
        rollbackMethod = "cancel"
    )
    void deduct(@BusinessActionContextParameter(paramName = "accountId") String accountId,
                @BusinessActionContextParameter(paramName = "amount") Double amount);
    
    boolean confirm(BusinessActionContext ctx);
    boolean cancel(BusinessActionContext ctx);
}
```

TCC的优势是**不需要全局锁**，性能优于AT模式。缺点是业务侵入性强，空回滚（Try未执行但收到了Cancel）、幂等（Confirm/Cancel可能执行多次）、悬挂（Cancel先于Try执行完成）是需要处理的三个经典问题。Seata通过**事务日志表**记录状态来保证幂等性。

### 9. ES集群：主分片、副本分片与脑裂问题

ES集群中每个索引被分为多个**主分片**（Primary Shard），写操作先写入主分片，然后同步到**副本分片**（Replica Shard）。主分片数在索引创建时确定不可更改，副本分片可以动态调整。查询时可以同时从主分片和副本分片读取（负载均衡），副本分片还提供故障转移能力。

**脑裂问题**：当网络分区导致集群中的节点无法互相通信时，可能分裂出多个"主节点"。ES通过**discovery.zen.minimum_master_nodes**参数防止脑裂，该公式为：`minimum_master_nodes = N/2 + 1`（N为候选主节点数）。例如3个候选主节点，设置为2。7.x版本后，ES引入了**集群协调**新机制，使用Quorum-based决策，更有效地防止脑裂。

```yaml
# ES集群配置（7.x+）
discovery.seed_hosts: ["node1:9300", "node2:9300", "node3:9300"]
cluster.initial_master_nodes: ["node1", "node2", "node3"]
# 不再需要 minimum_master_nodes，新版自动管理
```

### 10. Nacos并发读写冲突解决方案（CopyOnWrite）

Nacos服务端的数据存储在**ServiceManager**的Map结构中，当多个线程并发操作同一个服务下的实例列表时，通过Java的**CopyOnWrite**机制解决并发问题：

```java
public class ServiceManager {
    // 使用ConcurrentHashMap + CopyOnWrite保证并发安全
    private final ConcurrentHashMap<String, Service> serviceMap;
    
    public void updateInstance(String serviceName, Instance instance) {
        // 修改实例时采用"写时复制"——先拷贝一份，修改后再替换
        Service service = serviceMap.get(serviceName);
        Service newService = Service.copy(service);  // 浅拷贝
        newService.updateInstance(instance);
        serviceMap.put(serviceName, newService);
    }
}
```

对于读多写少的场景（注册中心读请求远多于写请求），CopyOnWrite提供了无锁读的性能优势，同时保证了写操作的最终一致性。但写操作需要拷贝整个数据结构，**不适用于大对象频繁写入的场景**。

### 11. Nacos高并发注册压力处理（Distro协议）

Distro是Nacos自研的**最终一致性**协议，核心思想是"每个节点只负责一部分数据的写入，但所有节点都可以响应读取请求"：

1. **写操作**：客户端根据服务名哈希选择目标节点，写请求只发送给该节点（负责节点）。负责节点将数据写入内存后，异步同步到其他节点
2. **读操作**：任何节点都能直接响应读请求，直接从本地内存返回数据（无一致性开销）
3. **同步机制**：节点间通过**批量同步任务**（默认每100ms执行一次），将变更数据同步到集群中所有节点
4. **新节点加入**：新节点启动时从其他节点全量拉取所有服务数据，之后接收增量同步

```java
// Distro协议——异步同步核心
@Component
public class DistroConsistencyServiceImpl implements KonsulConsistencyService {
    @Override
    public void put(String key, Record value) throws NacosException {
        onPut(key, value);           // 写入本地
        distroMapper.distroSync(key, value);  // 异步同步到其他节点
    }
}
```

> 🎯 Distro协议的定位是**高吞吐的注册中心**，适用于AP场景。对比CP协议（如Raft），Distro牺牲了强一致性换取了更高的写性能。

### 12. Gateway过滤器链的执行顺序

Gateway的过滤器分为**pre**和**post**两类，分别在请求路由前和响应返回后执行。过滤器链基于**责任链模式**，执行顺序由`getOrder()`方法返回的int值决定（值越小越先执行）。GateWay内置了以下排序规则：

1. **GatewayFilter**：通过`@Order`或实现`Ordered`接口排序
2. **GlobalFilter**：同样通过`@Order`或`Ordered`接口排序
3. **DefaultFilter**：配置文件中定义的默认过滤器，添加到每个路由
4. **组合规则**：实际执行时，`GatewayFilterChain`将所有过滤器按照order值合并为一个列表，依次执行。pre逻辑从低到高执行，post逻辑从高到低执行（类似AOP的环绕通知）

```java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Pre逻辑
        ServerHttpRequest request = exchange.getRequest();
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        // 传递给下游过滤器
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Post逻辑
            log.info("Response status: {}", exchange.getResponse().getStatusCode());
        }));
    }
    
    @Override
    public int getOrder() {
        return 0;  // order值越小越先执行
    }
}
```

### 13. Nacos配置热刷新机制（长轮询）

Nacos Config的热刷新基于**长轮询（Long Polling）**机制实现。客户端向服务端发起一个配置监听请求，如果配置没有变更，服务端不会立即返回，而是将请求挂起（默认超时30秒）。在这期间，如果配置发生变更，服务端立即返回变更内容；如果30秒内无变更，服务端返回304状态码让客户端重新发起长轮询：

```text
Client --POST /v1/cs/configs/listener--> Nacos Server (挂起连接，最长30s)
(配置变更事件发生)
Nacos Server --返回变更的dataId+group--> Client
Client --GET /v1/cs/configs?dataId=x&group=y--> Nacos Server (拉取最新配置)
Client --更新@RefreshScope注解的Bean--> 应用
```

```java
@RefreshScope   // 关键注解——配置刷新后重新创建Bean
@RestController
public class ConfigController {
    @Value("${order.timeout:5000}")
    private int orderTimeout;
    
    @GetMapping("/config")
    public String getConfig() {
        // 当Nacos中order.timeout变更时，此Bean自动刷新
        return "timeout: " + orderTimeout;
    }
}
```

> 💡 @RefreshScope的原理是Spring在配置变更后，会销毁并重新创建被该注解标注的Bean实例，从而实现配置热更新。未标注@RefreshScope的Bean无法自动刷新。

---

## 三、实战场景题

> 结合真实业务场景，给出完整的配置和代码示例。

### 1. 配置Nacos作为注册中心+配置中心

```yaml
# application.yml
spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.1.100:8848   # 注册中心地址
        namespace: prod                    # 命名空间隔离环境
        group: DEFAULT_GROUP
        cluster-name: Beijing              # 集群名称，同集群优先调用
        ephemeral: true                    # 临时实例
      config:
        server-addr: ${spring.cloud.nacos.discovery.server-addr}
        namespace: ${spring.cloud.nacos.discovery.namespace}
        group: DEFAULT_GROUP
        file-extension: yaml               # 配置文件的扩展名
        refresh-enabled: true              # 开启动态刷新
        extension-configs:                 # 共享配置（多应用通用）
          - dataId: common-db.yaml
            group: COMMON_GROUP
            refresh: true
          - dataId: common-redis.yaml
            group: COMMON_GROUP
            refresh: true
```

```yaml
# bootstrap.yml（必须使用bootstrap，优先加载）
spring:
  cloud:
    nacos:
      config:
        server-addr: 192.168.1.100:8848
        namespace: prod
  profiles:
    active: dev  # 支持profile粒度的配置：order-service-dev.yaml
```

### 2. Feign客户端自定义配置

```java
// Feign接口定义
@FeignClient(
    name = "user-service",
    path = "/api/user",
    configuration = UserFeignConfig.class,
    fallbackFactory = UserFeignFallbackFactory.class
)
public interface UserFeignClient {
    @GetMapping("/{id}")
    Result<UserVO> getUserById(@PathVariable("id") Long id);
}

// 自定义Feign配置——设置超时、拦截器、日志
@Configuration
public class UserFeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 传递TraceId用于链路追踪
            template.header("TraceId", IdUtil.fastSimpleUUID());
            // 传递认证信息
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String token = attrs.getRequest().getHeader("Authorization");
                if (token != null) template.header("Authorization", token);
            }
        };
    }
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;  // 生产环境建议设为BASIC
    }
}

// 降级处理
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {
    @Override
    public UserFeignClient create(Throwable cause) {
        return id -> {
            log.error("Feign调用用户服务异常", cause);
            return Result.error("用户服务暂时不可用");
        };
    }
}
```

```yaml
# Feign性能优化配置
feign:
  client:
    config:
      default:
        connectTimeout: 2000       # 连接超时
        readTimeout: 3000          # 读取超时
      user-service:                # 针对特定服务的配置
        connectTimeout: 1000
        readTimeout: 2000
  compression:
    request:
      enabled: true                # 请求压缩
      mime-types: application/json
      min-request-size: 2048
    response:
      enabled: true                # 响应压缩
  httpclient:
    enabled: true                  # 使用Apache HttpClient替代默认
    max-connections: 200
    max-connections-per-route: 50
```

### 3. Gateway全局过滤器实现JWT鉴权

```java
@Component
@Slf4j
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {
    
    // 白名单路径——不需要登录即可访问
    private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/user/login", "/api/user/register",
        "/api/goods/list", "/api/goods/detail"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // 白名单直接放行
        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        
        // 获取Token并校验
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing token");
        }
        
        try {
            // JWT校验
            Claims claims = Jwts.parser()
                .setSigningKey("secret-key".getBytes())
                .parseClaimsJws(token.substring(7))
                .getBody();
            
            // 将用户信息放入请求头，传递给下游服务
            String userId = claims.get("userId").toString();
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Name", URLEncoder.encode(claims.get("userName").toString(), "UTF-8"))
                .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (JwtException | IOException e) {
            return unauthorized(exchange, "Invalid token");
        }
    }
    
    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory()
            .wrap(Result.error(msg).toString().getBytes())));
    }
    
    @Override
    public int getOrder() {
        return -100;  // 高优先级，先执行
    }
}
```

```yaml
# Gateway跨域配置
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:8080"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600
```

### 4. Sentinel流控规则配置

```java
@Configuration
public class SentinelFlowConfig {
    
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        
        // 1. QPS限流——每秒最多100个请求
        FlowRule qpsRule = new FlowRule();
        qpsRule.setResource("order/create");
        qpsRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        qpsRule.setCount(100);
        qpsRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        // 冷启动模式——让流量缓慢增长到阈值
        // qpsRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);
        // qpsRule.setWarmUpPeriodSec(10);
        rules.add(qpsRule);
        
        // 2. 线程数限流——最多同时处理20个请求
        FlowRule threadRule = new FlowRule();
        threadRule.setResource("order/query");
        threadRule.setGrade(RuleConstant.FLOW_GRADE_THREAD);
        threadRule.setCount(20);
        rules.add(threadRule);
        
        // 加载规则
        FlowRuleManager.loadRules(rules);
    }
}
```

```yaml
# 在Nacos中配置Sentinel规则（动态加载）
spring:
  cloud:
    sentinel:
      datasource:
        ds-flow:
          nacos:
            server-addr: ${spring.cloud.nacos.discovery.server-addr}
            dataId: ${spring.application.name}-sentinel-flow.json
            rule-type: flow
        ds-degrade:
          nacos:
            server-addr: ${spring.cloud.nacos.discovery.server-addr}
            dataId: ${spring.application.name}-sentinel-degrade.json
            rule-type: degrade
```

### 5. Seata分布式事务（订单系统）

```java
@Service
@Slf4j
public class OrderService {
    
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateDTO dto) {
        // 1. 本地事务：创建订单（状态=待支付）
        Order order = new Order();
        order.setUserId(dto.getUserId());
        order.setAmount(dto.getTotalAmount());
        order.setStatus(OrderStatus.PENDING_PAY);
        orderService.save(order);
        
        // 2. 调用库存服务——扣减库存（跨服务）
        stockFeignClient.deductStock(dto.getSkuId(), dto.getQuantity());
        
        // 3. 调用账户服务——扣减余额（跨服务）
        accountFeignClient.deductBalance(dto.getUserId(), dto.getTotalAmount());
        
        // 4. 更新订单为成功状态
        order.setStatus(OrderStatus.PAID);
        orderService.updateById(order);
        
        return OrderVO.from(order);
    }
}
```

```yaml
# Seata配置
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: my-tx-group
  service:
    vgroup-mapping:
      my-tx-group: default
    grouplist:
      default: 192.168.1.100:8091
  config:
    type: nacos
    nacos:
      server-addr: ${spring.cloud.nacos.discovery.server-addr}
      group: SEATA_GROUP
  registry:
    type: nacos
    nacos:
      server-addr: ${spring.cloud.nacos.discovery.server-addr}
```

### 6. RabbitMQ可靠消息投递

```java
@Configuration
public class RabbitMQConfig {
    
    public static final String EXCHANGE = "order.business.exchange";
    public static final String QUEUE = "order.pay.queue";
    public static final String ROUTING_KEY = "order.pay";
    
    @Bean
    public DirectExchange businessExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE)
            .durable(true)
            .build();
    }
    
    @Bean
    public Queue payQueue() {
        return QueueBuilder.durable(QUEUE)
            .deadLetterExchange("dlx.exchange")     // 死信交换机
            .deadLetterRoutingKey("order.dead")      // 死信路由键
            .ttl(300000)                              // 消息TTL 5分钟
            .maxLength(10000)                         // 队列最大长度
            .build();
    }
    
    // 消息确认回调
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息成功到达交换机: {}", correlationData.getId());
            } else {
                log.error("消息未到达交换机: {}, cause: {}", correlationData.getId(), cause);
                // 补偿：将消息写入本地重试表或发送到补偿队列
            }
        });
        template.setReturnsCallback(returned -> {
            log.error("消息未到达队列: exchange={}, routingKey={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
            // 处理路由不到队列的消息
        });
        template.setMandatory(true);  // 开启return回调
        return template;
    }
}
```

```java
@Service
@Slf4j
public class OrderMessageSender {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendOrderMessage(Order order) {
        CorrelationData correlationData = new CorrelationData(order.getOrderId());
        // 设置confirm回调
        correlationData.getFuture().addCallback(
            result -> {
                if (result != null && result.isAck()) {
                    log.info("订单消息确认成功: {}", order.getOrderId());
                    // 更新本地消息状态为已发送
                } else if (result != null) {
                    log.warn("订单消息nack: {}, reason: {}", order.getOrderId(), result.getReason());
                    // 重试补偿
                }
            },
            ex -> log.error("订单消息异常: {}", order.getOrderId(), ex)
        );
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.ROUTING_KEY,
            order,
            correlationData
        );
    }
}
```

### 7. 多级缓存架构（Caffeine + Redis）

```java
@Service
public class GoodsService {
    
    // L1: Caffeine本地缓存
    private final Cache<Long, GoodsVO> localCache = Caffeine.newBuilder()
        .initialCapacity(1000)
        .maximumSize(10000)
        .expireAfterWrite(10, TimeUnit.SECONDS)     // 本地缓存10秒过期
        .recordStats()                               // 记录命中率
        .build();
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public GoodsVO getGoods(Long id) {
        // 1. 查本地缓存（L1）
        GoodsVO goods = localCache.getIfPresent(id);
        if (goods != null) {
            return goods;
        }
        
        // 2. 查Redis缓存（L2）
        String goodsJson = redisTemplate.opsForValue().get("goods:" + id);
        if (StringUtils.isNotBlank(goodsJson)) {
            goods = JSON.parseObject(goodsJson, GoodsVO.class);
            localCache.put(id, goods);            // 回填L1
            return goods;
        }
        
        // 3. 查数据库（L3），使用互斥锁防止缓存击穿
        String lockKey = "lock:goods:" + id;
        Boolean locked = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            try {
                goods = getFromDB(id);
                if (goods != null) {
                    redisTemplate.opsForValue()
                        .set("goods:" + id, JSON.toJSONString(goods), 30, TimeUnit.MINUTES);
                    localCache.put(id, goods);
                }
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // 等待重试——其他线程正在查DB
            Thread.sleep(100);
            return getGoods(id);  // 递归重试
        }
        
        return goods;
    }
    
    // 缓存预热
    @PostConstruct
    public void preloadHotGoods() {
        List<Long> hotIds = getHotGoodsIds();
        hotIds.forEach(this::getGoods);  // 触发缓存加载
    }
}
```

### 8. ES酒店搜索（带地理位置和聚合）

```json
// 酒店索引映射
PUT /hotel
{
  "mappings": {
    "properties": {
      "name": { "type": "text", "analyzer": "ik_max_word" },
      "address": { "type": "text", "analyzer": "ik_smart" },
      "price": { "type": "integer" },
      "score": { "type": "float" },
      "brand": { "type": "keyword" },
      "city": { "type": "keyword" },
      "location": { "type": "geo_point" },
      "star": { "type": "keyword" },
      "business": { "type": "keyword" }
    }
  }
}
```

```java
// 聚合搜索——按城市统计酒店数量
@Test
public void testAggregation() {
    SearchRequest request = new SearchRequest("hotel");
    request.source().size(0);  // 不需要原始数据
    
    // 按城市聚合
    AggregationBuilder agg = AggregationBuilders
        .terms("city_agg")
        .field("city")
        .size(20)
        .subAggregation(AggregationBuilders
            .avg("avg_price")
            .field("price"));  // 子聚合：每个城市的平均价格
    
    request.source().aggregation(agg);
    
    SearchResponse response = restClient.search(request, RequestOptions.DEFAULT);
    Terms terms = response.getAggregations().get("city_agg");
    for (Terms.Bucket bucket : terms.getBuckets()) {
        String city = bucket.getKeyAsString();
        long count = bucket.getDocCount();
        Avg avg = bucket.getAggregations().get("avg_price");
        System.out.println(city + ": " + count + "家, 均价: " + avg.getValue());
    }
}
```

---

## 四、手写代码题

> 面试中可能要求现场手写，需熟练掌握关键API。

### 1. Sentinel流控规则——Java代码配置

```java
public class SentinelDemo {
    public static void main(String[] args) {
        // 配置流控规则
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource("GET:/api/order/create");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(50);                    // 50 QPS
        rule.setLimitApp("default");
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
        
        // 配置熔断降级规则
        List<DegradeRule> degradeRules = new ArrayList<>();
        DegradeRule degradeRule = new DegradeRule();
        degradeRule.setResource("GET:/api/order/create");
        degradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);  // 慢调用比例
        degradeRule.setCount(200);                // RT > 200ms
        degradeRule.setTimeWindow(10);            // 熔断后恢复时间
        degradeRule.setMinRequestAmount(5);       // 最小请求数
        degradeRule.setSlowRatioThreshold(0.5);   // 慢调用比例阈值
        degradeRules.add(degradeRule);
        DegradeRuleManager.loadRules(degradeRules);
        
        // 业务代码中使用Sentinel资源
        while (true) {
            Entry entry = null;
            try {
                entry = SphU.entry("GET:/api/order/create");
                // 业务逻辑
                Thread.sleep(10);
            } catch (BlockException e) {
                // 被限流降级
                System.out.println("Blocked: " + e.getMessage());
            } finally {
                if (entry != null) entry.exit();
            }
        }
    }
}
```

### 2. Nacos服务发现——Java API

```java
public class NacosDiscoveryDemo {
    public static void main(String[] args) throws Exception {
        // 创建Nacos客户端
        NamingService naming = NamingFactory.createNamingService("192.168.1.100:8848");
        
        // 1. 注册服务
        naming.registerInstance("order-service", "192.168.1.10", 8080, "Beijing");
        
        // 2. 获取所有实例
        List<Instance> instances = naming.getAllInstances("order-service");
        for (Instance instance : instances) {
            System.out.printf("Instance: %s:%d, cluster=%s, weight=%.1f, healthy=%s%n",
                instance.getIp(), instance.getPort(), 
                instance.getClusterName(), instance.getWeight(), instance.isHealthy());
        }
        
        // 3. 获取一个健康实例（负载均衡）
        Instance instance = naming.selectOneHealthyInstance("order-service", "Beijing");
        System.out.println("Selected: " + instance.getIp() + ":" + instance.getPort());
        
        // 4. 订阅服务变更
        naming.subscribe("order-service", event -> {
            if (event instanceof NamingEvent) {
                System.out.println("服务变更: " + event);
                // 重新拉取实例列表
            }
        });
        
        // 5. 注册监听器
        Thread.sleep(60000);  // 保持进程存活
    }
}
```

### 3. Feign + Sentinel 降级处理

```java
// 1. 启用Sentinel对Feign的支持（配置类）
@Configuration
public class FeignSentinelConfig {
    @Bean
    @ConditionalOnMissingBean
    public Feign.Builder feignSentinelBuilder() {
        return SentinelFeign.builder();
    }
}

// 2. Feign客户端（带降级工厂）
@FeignClient(name = "inventory-service", 
             path = "/api/inventory",
             fallbackFactory = InventoryFeignFallbackFactory.class)
public interface InventoryFeignClient {
    
    @PostMapping("/deduct")
    Result<Void> deductStock(@RequestBody DeductStockDTO dto);
    
    @GetMapping("/stock/{skuId}")
    Result<Integer> getStock(@PathVariable("skuId") Long skuId);
}

// 3. 降级工厂实现
@Component
@Slf4j
public class InventoryFeignFallbackFactory implements FallbackFactory<InventoryFeignClient> {
    
    @Override
    public InventoryFeignClient create(Throwable cause) {
        return new InventoryFeignClient() {
            @Override
            public Result<Void> deductStock(DeductStockDTO dto) {
                log.error("库存扣减服务熔断降级, skuId={}, quantity={}, cause={}", 
                    dto.getSkuId(), dto.getQuantity(), cause.getMessage());
                return Result.error("库存服务繁忙，请稍后重试");
            }
            
            @Override
            public Result<Integer> getStock(Long skuId) {
                log.error("库存查询服务降级, skuId={}, cause={}", skuId, cause.getMessage());
                return Result.ok(0);  // 降级返回0，触发后续的补充逻辑
            }
        };
    }
}

// 4. 配置降级规则（Nacos动态配置）
// dataId: inventory-service-sentinel-degrade.json
// [
//   {
//     "resource": "POST:http://inventory-service/api/inventory/deduct",
//     "grade": 0,
//     "count": 200,
//     "timeWindow": 10,
//     "minRequestAmount": 5,
//     "slowRatioThreshold": 0.5
//   }
// ]
```

### 4. Seata @GlobalTransactional 使用示例

```java
@Service
@Slf4j
public class TransferServiceImpl implements TransferService {
    
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private TransferRecordMapper recordMapper;
    
    @Override
    @GlobalTransactional(name = "transfer", rollbackFor = Exception.class)
    public void transfer(TransferDTO dto) {
        log.info("开始跨行转账事务, from={}, to={}, amount={}", 
            dto.getFromAccount(), dto.getToAccount(), dto.getAmount());
        
        // 1. 扣减转出账户
        int fromRows = accountMapper.deductBalance(dto.getFromAccount(), dto.getAmount());
        if (fromRows <= 0) {
            throw new BusinessException("账户余额不足");
        }
        
        // 2. 调用远程银行服务增加转入账户
        Result<Void> result = bankFeignClient.increaseBalance(
            dto.getToAccount(), dto.getAmount(), dto.getTransferId());
        if (result.getCode() != 200) {
            throw new BusinessException("转入失败: " + result.getMsg());
        }
        
        // 3. 本地插入转账记录
        TransferRecord record = new TransferRecord();
        record.setTransferId(dto.getTransferId());
        record.setFromAccount(dto.getFromAccount());
        record.setToAccount(dto.getToAccount());
        record.setAmount(dto.getAmount());
        record.setStatus(TransferStatus.SUCCESS);
        recordMapper.insert(record);
        
        // 4. 任意一步抛出异常都会触发全局回滚
        // if (true) throw new RuntimeException("模拟异常测试全局回滚");
    }
}
```

### 5. RabbitMQ消息发送带Confirm回调

```java
@Component
@Slf4j
public class ReliableMessageSender {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendMessage(String exchange, String routingKey, Object message) {
        String msgId = IdUtil.fastSimpleUUID();
        CorrelationData correlationData = new CorrelationData(msgId);
        
        // 设置Confirm回调
        correlationData.getFuture().addCallback(
            result -> {
                if (result != null && result.isAck()) {
                    log.info("消息确认成功, id={}", msgId);
                    // 更新本地消息表状态为CONFIRMED
                    messageDao.updateStatus(msgId, MessageStatus.SENT);
                } else if (result != null) {
                    log.warn("消息被nack, id={}, reason={}", msgId, result.getReason());
                    handleNack(msgId, message);
                }
            },
            ex -> {
                log.error("Confirm回调异常, id={}", msgId, ex);
                handleNack(msgId, message);
            }
        );
        
        // 发送消息
        rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
            // 设置消息持久化和唯一ID
            msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            msg.getMessageProperties().setMessageId(msgId);
            msg.getMessageProperties().setHeader("x-retry-count", 0);
            return msg;
        }, correlationData);
    }
    
    private void handleNack(String msgId, Object message) {
        // 写入本地重试表
        RetryMessage retry = new RetryMessage(msgId, message, 1);
        retryMessageDao.insert(retry);
    }
}

// 手动ACK消费
@Component
public class ReliableConsumer {
    @RabbitListener(queues = "order.pay.queue")
    public void handle(Order order, Message message, Channel channel) {
        try {
            processOrder(order);
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("消费失败", e);
            // requeue=false发送到死信队列
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }
}
```

### 6. ES RestClient搜索（Bool Query + 聚合）

```java
@Service
public class HotelSearchService {
    
    @Autowired
    private RestHighLevelClient client;
    
    public SearchResult search(HotelSearchDTO dto) {
        SearchRequest request = new SearchRequest("hotel");
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // 1. 关键字搜索（全文检索）
        if (StringUtils.isNotBlank(dto.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("name", dto.getKeyword()));
        }
        
        // 2. 品牌过滤
        if (StringUtils.isNotBlank(dto.getBrand())) {
            boolQuery.filter(QueryBuilders.termQuery("brand", dto.getBrand()));
        }
        
        // 3. 价格范围
        if (dto.getMinPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(dto.getMinPrice()));
        }
        if (dto.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").lte(dto.getMaxPrice()));
        }
        
        // 4. 评分筛选
        if (dto.getMinScore() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("score").gte(dto.getMinScore()));
        }
        
        // 5. 地理排序（按距离排序）
        if (dto.getLat() != null && dto.getLon() != null) {
            GeoDistanceSortBuilder sortBuilder = SortBuilders
                .geoDistanceSort("location", dto.getLat(), dto.getLon())
                .order(SortOrder.ASC)
                .unit(DistanceUnit.KILOMETERS);
            request.source().sort(sortBuilder);
        }
        
        // 6. 分页 + 高亮
        request.source()
            .query(boolQuery)
            .from((dto.getPage() - 1) * dto.getSize())
            .size(dto.getSize())
            .highlighter(new HighlightBuilder()
                .field("name")
                .preTags("<em>")
                .postTags("</em>"));
        
        // 7. 品牌聚合
        request.source().aggregation(AggregationBuilders
            .terms("brand_agg")
            .field("brand")
            .size(50));
        
        // 执行搜索
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            return parseResponse(response);
        } catch (IOException e) {
            log.error("ES搜索失败", e);
            throw new BusinessException("搜索服务异常");
        }
    }
}
```

---

## 五、系统设计题

> 考察架构设计能力，需要从全局视角考虑高并发、高可用、一致性等问题。

### 1. 设计一个高并发秒杀系统

**核心挑战**：瞬间高并发（万级QPS）、超卖、系统雪崩

**架构方案**：

```text
CDN(静态资源)
    ↓
Nginx(限流+负载均衡，lua限流脚本)
    ↓
Gateway(Sentinel流控+熔断)
    ↓
秒杀服务集群(无状态水平扩展)
    ↓  ↓     ↓
Redis(预减库存+Lua原子操作)    RabbitMQ(削峰异步下单)    DB(最终库存扣减)
```

**详细设计**：

| 层级 | 方案 | 说明 |
|-----|------|------|
| 前端 | 按钮置灰+随机延迟 | 控制用户点太快 |
| 网关 | Sentinel QPS限流+令牌桶 | 按用户ID hash限流，每人每秒1次 |
| Redis | Lua脚本原子扣库存 | 避免超卖，SKU库存预热到Redis |
| MQ | 削峰填谷 | 秒杀请求转异步，限制队列长度 |
| DB | 乐观锁 | `update stock set version=version+1 where stock>0 and version=？` |
| 防刷 | 令牌桶 + 验证码 | 校验用户身份，限制单用户购买数量 |

```lua
-- Redis Lua扣减库存脚本
local key = KEYS[1]        -- 库存key: "stock:{skuId}"
local userKey = KEYS[2]    -- 用户购买记录: "user:{userId}:buy"
local quantity = tonumber(ARGV[1])
local userId = ARGV[2]
local limitPerUser = tonumber(ARGV[3])

-- 检查用户是否已购买
local bought = redis.call('get', userKey)
if bought and tonumber(bought) >= limitPerUser then
    return 2   -- 已达限购数量
end

-- 检查库存
local stock = redis.call('get', key)
if not stock or tonumber(stock) < quantity then
    return 0   -- 库存不足
end

-- 扣减库存 + 记录用户购买
redis.call('decrby', key, quantity)
redis.call('incrby', userKey, quantity)
redis.call('expire', userKey, 86400)   -- 24小时过期
return 1   -- 成功
```

### 2. 设计一个多级缓存架构

**背景**：商品详情页QPS 10万+，要求响应时间<50ms

**缓存层级**：

```
客户端浏览器缓存 (Cache-Control: max-age=60)
    ↓
CDN缓存 (静态资源，图片/css/js，TTL=1h)
    ↓
Nginx本地缓存 (lua_shared_dict，10MB，TTL=10s)
    ↓
Caffeine本地缓存 (JVM进程内，10万条，TTL=30s)
    ↓
Redis集群缓存 (主从+哨兵，TTL=30min)
    ↓
MySQL数据库 (主从读写分离)
```

**缓存一致性方案**：
- **更新策略**：DB更新后，通过MQ广播删除缓存消息（Cache-Aside模式，先更新DB再删缓存）
- **最终一致性**：MQ+延时双删（先删缓存 → 更新DB → 延时1s再删一次缓存）
- **兜底策略**：缓存设置过期时间，过期后自动从DB加载

**性能数据**：

| 缓存层 | 平均响应时间 | 吞吐量 |
|-------|------------|-------|
| Caffeine (L1) | <1ms | 百万级QPS |
| Redis (L2) | 1-5ms | 10万级QPS |
| MySQL (L3) | 10-50ms | 万级QPS |

### 3. 设计一个基于ES的搜索引擎

**业务场景**：电商平台商品搜索，支持关键词、分类、品牌、价格范围、销量排序、地理位置

**索引设计**：

```json
PUT /product
{
  "settings": {
    "number_of_shards": 5,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_smart_analyzer": { "type": "custom", "tokenizer": "ik_smart" },
        "ik_max_word_analyzer": { "type": "custom", "tokenizer": "ik_max_word" }
      }
    }
  },
  "mappings": {
    "properties": {
      "title": { "type": "text", "analyzer": "ik_max_word", "copy_to": "full_text" },
      "categoryName": { "type": "keyword" },
      "brandName": { "type": "keyword" },
      "price": { "type": "double" },
      "sales": { "type": "long" },
      "score": { "type": "float" },
      "createTime": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" },
      "location": { "type": "geo_point" },
      "tags": { "type": "keyword" },
      "full_text": { "type": "text", "analyzer": "ik_max_word" }
    }
  }
}
```

**架构设计**：

```text
业务系统 │ 数据同步 │ ES集群 │ 搜索服务
产品修改 ─→ Canal监听MySQL binlog ─→ MQ ─→ 同步到ES(增量)
                                         ─→ 定时全量重建(凌晨)
用户请求 ─→ Gateway ─→ SearchService ─→ ES集群(DSL查询)
                                         ├─ Bool Query(多条件组合)
                                         ├─ Function Score(销量、评分加权)
                                         └─ Aggregation(品牌、分类聚合)
```

> 💡 **搜索质量优化**：使用`function_score`结合销量、评分、时效性计算综合排序分数；使用`synonym_graph`同义词过滤器处理"手机=手机通讯"等场景。

### 4. 设计一个分布式日志收集系统

**架构方案**（ELK + Kafka）：

```text
微服务应用 ─(Filebeat)─→ Kafka ─(Logstash)─→ ES ─(Kibana)─→ 可视化
    │                    │
    └─(Logstash直接收集)─→  └─(数据缓冲，削峰填谷)─→
```

**关键设计**：

- **日志采集**：Filebeat采集容器日志，支持多行合并（异常栈跟踪）
- **消息队列**：Kafka 3分区+2副本，日志topic按服务名分类，保留7天
- **日志清洗**：Logstash Grok解析日志格式，提取traceId、timestamp、level等字段
- **ES存储**：按天创建索引（`log-2026-07-22`），设置3主分片+1副本，30天后自动删除（ILM策略）
- **查询优化**：traceId精确查询（keyword类型），时间范围查询（range filter），日志级别聚合

```logstash
# logstash配置
input {
  kafka {
    bootstrap_servers => "192.168.1.100:9092"
    topics_pattern => "log-.*"
    codec => "json"
    consumer_threads => 4
  }
}
filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:log_time}\s+%{LOGLEVEL:level}\s+%{DATA:trace_id}\s+%{GREEDYDATA:msg}" }
  }
  date {
    match => ["log_time", "yyyy-MM-dd HH:mm:ss.SSS"]
    target => "@timestamp"
  }
}
output {
  elasticsearch {
    hosts => ["192.168.1.200:9200"]
    index => "log-%{+YYYY-MM-dd}"
    # 按traceId路由，同一请求日志落同一分片
    routing => "%{[trace_id]}"
  }
}
```

---

## 六、常见坑点与最佳实践

| 组件 | 常见坑点 | 最佳实践 |
|-----|---------|---------|
| **Nacos注册中心** | 服务上线后调用方仍然找不到新实例 | 配置健康检查周期，设置合理的`heart-beat-interval`和`ip-delete-timeout` |
| **Nacos配置中心** | 修改配置后Bean未刷新 | 关键配置类加`@RefreshScope`，使用`extension-configs`共享公共配置 |
| **Feign调用** | 链式调用超时导致雪崩 | 设置`connectTimeout`+`readTimeout`，开启Sentinel熔断，配置`FallbackFactory` |
| **Gateway** | 跨域请求被拦截 | 配置`globalcors`设置`allowedOrigins`和`allowCredentials`，option请求直接放行 |
| **Gateway** | 过滤器顺序混乱 | 统一实现`Ordered`接口，pre过滤器低order值，post过滤器高order值，`-100`认证>`0`日志等 |
| **Ribbon** | 首次请求超时（饥饿加载） | 开启饥饿加载：`ribbon.eager-load.enabled=true`，指定需要预热的服务列表 |
| **Sentinel** | 规则不热生效 | 使用Nacos数据源持久化Sentinel规则，避免控制台规则重启丢失 |
| **MQ消息丢失** | 生产者发送后Broker宕机 | 开启`publisher-confirm-type=correlated` + `publisher-returns`，消息持久化，手动ACK消费 |
| **MQ重复消费** | 网络波动导致重复投递 | 消费端实现幂等性（唯一消息ID去重表+分布式锁或数据库唯一索引） |
| **MQ消息积压** | 业务故障恢复后消费能力不足 | 新增临时队列+消费者，直接调用旧队列数据；或扩容消费端并关闭重试 |
| **ES查询** | 深度分页导致性能问题 | 翻页>10000使用`search_after`代替`from+size`，避免`scroll`做实时查询 |
| **ES分片** | 分片过多导致集群压力 | 单分片大小控制在20-40GB，索引1-3副本，分片数=节点数×副本数 |
| **Seata AT** | 全局锁竞争导致性能下降 | AT模式适合短事务(<100ms)，长事务推荐TCC模式；隔离级别最高支持读已提交 |
| **Seata TCC** | 业务侵入性强，幂等性难保证 | 使用状态表+分布式锁保证Try/Confirm/Cancel幂等，注意空回滚和悬挂问题 |
| **Docker部署** | 容时区不同步 | Dockerfile中设置`ENV TZ=Asia/Shanghai`，挂载`/etc/localtime` |
| **Redis缓存** | 缓存穿透导致DB压力大 | 布隆过滤器拦截不存在key + 缓存空值（TTL短），双检锁防止缓存击穿 |
| **Redis缓存** | 缓存雪崩 | 缓存过期时间加随机值（基础TTL±5min），本地缓存兜底，限流降级 |
| **配置管理** | 敏感信息（数据库密码）明文 | 使用Jasypt加密配置`ENC(密文)`，或对接密钥管理服务（KMS） |

---

## 七、面试回答模板

> 针对最高频的5个问题，提供结构化回答模板，回答时遵循"总-分-总"结构。

### 模板1："讲一下你在微服务项目中的实战经验"

```
[总分总结构]

总：我参与的项目采用Spring Cloud Alibaba微服务架构，包含用户、订单、商品、支付等10+个微服务，
通过Nacos注册发现、Gateway统一网关、Sentinel熔断降级、Seata分布式事务等技术组件构建了完整的微服务体系。

分：
1. 服务拆分层面：按业务领域拆分，每个服务独立数据库，通过Feign进行服务间通信。
2. 高可用架构：Nacos集群3节点+Group2分区，Redis哨兵模式，RabbitMQ镜像队列。
3. 流量治理：Gateway层Sentinel限流（QPS阈值1000），核心接口配置熔断规则（RT>500ms熔断10s）。
4. 数据一致性：最终一致性场景用MQ（可靠投递+人工补偿），强一致性场景用Seata AT模式。
5. 可观测性：集成SkyWalking链路追踪，ELK日志收集，Prometheus+Grafana指标监控。

总：通过这套微服务架构，系统支撑了日均百万级订单量，核心接口可用性达到99.99%。
```

> 🎯 **加分点**：主动提到踩过的坑和解决方案——如Feign链式调用的超时问题、热key导致Redis缓存击穿。

### 模板2："Nacos架构设计及为什么选择它而不是Eureka"

```
[为什么选择Nacos]

Eureka 2.0已经停更，不再适合新项目。Nacos作为阿里开源的一站式解决方案，兼具
注册中心和配置中心能力，降低了运维和系统复杂度。

[Nacos核心优势]
1. AP/CP动态切换：临时实例AP（最终一致性，基于Distro协议），持久化实例CP（强一致性，基于Raft协议）
2. 多级存储模型：Service → Cluster → Instance三级架构，支持同集群优先调用
3. 健康检查更完善：心跳+TCP/HTTP主动探测双重机制，心跳5s/摘除30s
4. 内置配置中心：长轮询机制实现配置热刷新，支持多环境共享配置
5. 权重路由：支持实例级权重设置，实现灰度发布和流量调拨
6. 作为Spring Cloud Alibaba生态核心组件，与Sentinel、Seata、Gateway无缝集成

[踩过的坑]
Nacos 1.x版本在客户端较多时，UDP推送存在丢包问题导致服务列表更新不及时。
我们的解决方案：升级到Nacos 2.x（gRPC替代UDP推送），稳定性大幅提升。
```

### 模板3："Sentinel如何实现限流和熔断？"

```
[核心机制]
Sentinel基于ProcessorSlotChain责任链模式，通过FlowSlot和DegradeSlot分别实现限流和熔断。

[限流实现——滑动窗口]
1. 将时间分为500ms的时间片，使用循环数组复用WindowWrap对象
2. 每次请求到达，计算当前时间所在窗口，统计passQps、blockQps等指标
3. 支持4种限流模式：直接拒绝（快速失败）、Warm Up（冷启动）、排队等待（漏桶）、预热+排队
4. 每秒级精度统计，相比Guava RateLimiter的令牌桶更精细

[熔断实现——断路器]
1. 半开状态机制：熔断后进入Half-Open状态，允许探测请求通过
2. 基于响应时间（慢调用比例阈值）、异常比例（异常数/总数）、异常数（分钟级）
3. 统计窗口内达到阈值→熔断→TimeWindow后恢复→Half-Open→正常/再次熔断

[资源定义方式]
- 注解方式：@SentinelResource(value="resourceName", blockHandler="handleBlock", fallback="handleFallback")
- 编码方式：SphU.entry("resourceName") + Entry.exit()
- 配合Nacos动态规则源：规则存储在Nacos，修改后实时生效

[限流效果]
接入Sentinel后，QPS峰值限流准确率>99%，误拦截率<0.1%。
```

### 模板4："Seata如何实现分布式事务？"

```
[Seata的三大角色]
- TC (Transaction Coordinator)：事务协调器，维护全局事务状态
- TM (Transaction Manager)：事务管理器，负责开启/提交/回滚全局事务
- RM (Resource Manager)：资源管理器，管理分支事务的资源

[AT模式（最常用）]
侵入性最小，自动生成回滚SQL，通过全局锁保证写隔离

第一阶段：
1. TM向TC申请XID并注册全局事务
2. RM解析业务SQL→生成before image→执行SQL→生成after image
3. 将undo log和业务SQL在同一个本地事务提交
4. RM向TC注册分支事务（XID + BranchID + ResourceID）

第二阶段：
- 全局提交：TC通知所有RM删除undo log，异步操作
- 全局回滚：TC通知RM，RM根据undo log生成补偿SQL回滚

[TCC模式（高性能场景）]
业务侵入性较大但性能更好，无全局锁
- Try：预留资源（冻结库存或余额）
- Confirm：确认使用（实际扣减，Try中预留的资源）
- Cancel：取消预留（解冻资源，幂等性处理）

[选型建议]
- 短事务（<100ms）、一致性要求高→AT模式
- 长事务、性能敏感→TCC模式
- 跨语言、对一致性要求不高→Saga模式（Saga无需全局锁，通过补偿事务恢复）
```

### 模板5："如何保证RabbitMQ消息的可靠性？"

```
[消息可靠性的三个层面]
- 生产端：确保消息从Producer到达Exchange/Queue
- 服务端：确保消息在Broker不丢失
- 消费端：确保消息被正确消费

[生产端——Confrim+Return机制]
1. ConfirmCallback：消息发送后Broker异步回调ACK/NACK
2. ReturnsCallback：消息路由不到任何队列时回调
3. 补偿机制：Confirm失败或Return的消息持久化到本地重试表，定时任务重发
4. Mandatory参数：设置mandatory=true，路由失败时触发ReturnsCallback

[服务端——持久化+镜像队列]
1. 交换机持久化：ExchangeBuilder.durable(true)
2. 队列持久化：QueueBuilder.durable(true)
3. 消息持久化：MessageDeliveryMode.PERSISTENT
4. 镜像队列（HA）：队列在集群多个节点有副本，主节点宕机自动切换

[消费端——手动ACK+幂等]
1. 手动确认：channel.basicAck(deliveryTag, false)
2. 重试机制：catch异常判断重试次数，超限发死信队列
3. 幂等消费：每条消息携带全局唯一ID（MessageId），消费前查重
4. 死信队列：处理失败的消息，T+1人工补偿或自动分析

[生产实践]
// 发送方
template.setConfirmCallback((correlationData, ack, cause) -> {
    if (!ack) { // 写入本地消息重发表 }
});

// 消费方
@RabbitListener(queues = "queue")
public void handle(Message message, Channel channel) {
    try {
        if (idempotentService.isProcessed(message.getMessageProperties().getMessageId())) {
            channel.basicAck(deliveryTag, false); return;
        }
        process(message);
        idempotentService.markProcessed(message.getMessageProperties().getMessageId());
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        channel.basicNack(deliveryTag, false, false); // 丢弃到死信
    }
}
```

---

## 八、快速查漏补缺Checklist

> 面试前逐项核对，确保没有知识漏洞。

### 注册中心与配置中心
- [ ] 我能用一句话说清楚CAP定理，并举例说明CP和AP的应用场景
- [ ] 我知道Nacos临时实例和持久化实例的区别，以及对应的CP/AP模式
- [ ] 我能画出Nacos的三级存储模型（Service → Cluster → Instance）
- [ ] 我理解Nacos心跳机制（5s/15s/30s三个关键时间点）
- [ ] 我知道Nacos长轮询配置热刷新的原理（30s超时挂起）
- [ ] 我对比过Nacos和Eureka的优劣（至少说出3点以上）
- [ ] 我理解Distro协议和Raft协议在Nacos中的应用场景
- [ ] 我知道Nacos的CopyOnWrite机制解决读写并发冲突

### 服务调用与负载均衡
- [ ] 我能手写Feign接口并自定义配置（超时、拦截器、日志）
- [ ] 我知道Feign性能优化的几个方向（连接池、压缩、日志级别）
- [ ] 我理解Ribbon饥饿加载的配置方式和原理
- [ ] 我了解Gateway的filter chain执行顺序（order值排序）
- [ ] 我能手写Gateway全局过滤器实现JWT鉴权
- [ ] 我会配置Gateway的CORS跨域

### 流量治理
- [ ] 我使用过Sentinel的流控规则（QPS/线程数、WarmUp/排队）
- [ ] 我理解Sentinel滑动窗口算法的实现原理（500ms时间片）
- [ ] 我知道Sentinel的ProcessorSlotChain责任链组成
- [ ] 我配置过Sentinel的熔断规则（慢调用/异常比例/异常数）
- [ ] 我知道Sentinel和Hystrix的核心区别（至少4点）
- [ ] 我理解Sentinel漏桶和令牌桶算法的区别和应用场景

### 分布式事务
- [ ] 我能用AT模式和TCC模式的区别（至少4个维度）
- [ ] 我理解Seata AT模式的两阶段执行过程（before/after image）
- [ ] 我知道Seata全局锁的作用和写隔离原理
- [ ] 我了解TCC的空回滚、幂等性、悬挂三个经典问题
- [ ] 我理解Saga事务与AT/TCC的定位区别

### 消息队列
- [ ] 我能画出RabbitMQ的五种消息模型
- [ ] 我理解Fanout/Direct/Topic三种交换机的区别
- [ ] 我会配置RabbitMQ的可靠消息投递（Confirm+Return+手动ACK）
- [ ] 我知道死信队列的几种触发场景和用途
- [ ] 我处理过消息积压问题（临时消费者扩容、队列分流）
- [ ] 我知道Lazy Queue的设计初衷和使用场景

### 搜索引擎
- [ ] 我能解释ES倒排索引的底层原理（Term Dictionary + Posting List）
- [ ] 我会写ES的Bool Query（must/filter/should/must_not）
- [ ] 我用过ES的聚合查询（terms/avg/stats aggregation）
- [ ] 我理解ES的深度分页问题和search_after解决方案
- [ ] 我知道ES集群脑裂的原因和解决方案
- [ ] 我配置过Canal同步MySQL数据到ES

### 容器化
- [ ] 我能手写Dockerfile（多阶段构建、时区、健康检查）
- [ ] 我会编写DockerCompose编排多容器
- [ ] 我了解Docker和虚拟机的本质区别（OS级 vs 硬件级）
- [ ] 我配置过Docker镜像仓库（Harbor）

### 缓存与高可用
- [ ] 我理解Redis的RDB和AOF持久化机制
- [ ] 我能区分Redis三种高可用方案（主从/哨兵/集群）
- [ ] 我设计过多级缓存架构（Caffeine → Redis → DB）
- [ ] 我能解决缓存穿透（布隆过滤器/空值缓存）、击穿（互斥锁/逻辑过期）、雪崩（过期时间加随机值）
- [ ] 我使用过Caffeine的W-TinyLFU淘汰算法

### 综合
- [ ] 我能回答"服务架构的演进过程"（单体→垂直→SOA→微服务）
- [ ] 我知道微服务带来的主要挑战（服务发现、配置管理、分布式事务、链路追踪、日志聚合）
- [ ] 我参与过微服务项目的实际落地，能说清楚当时的架构选型理由
- [ ] 我理解前后端分离、容器化部署、CI/CD在微服务中的应用

---

> 🎯 **最后提醒**：面试中回答技术问题要用 **STAR法则**（Situation-Task-Action-Result）组织语言，先说结论再展开，配合画图和代码示例。面试官考察的不只是你会不会用，而是你**理解了多少**、**踩过多少坑**、**有没有自己的思考**。
