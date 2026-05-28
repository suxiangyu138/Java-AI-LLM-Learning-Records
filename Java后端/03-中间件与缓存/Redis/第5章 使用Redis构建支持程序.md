# Redis 快速上手：构建程序支持服务（极简实战版）
我带你**10分钟快速掌握** Redis 核心用法，直接用在你的程序里做缓存、计数器、分布式锁、消息通知等支持服务，全程无废话、可直接复制运行。

## 一、先搞定环境（2分钟）

### 1. 安装 Redis
- Windows：[下载安装包](https://github.com/tporadowski/redis/releases) 双击运行
- Mac：`brew install redis`
- Linux：`apt install redis-server`

### 2. 启动+测试
```bash
# 启动服务
redis-server

# 新开终端连接客户端
redis-cli
# 测试：设置+获取值
set name redis
get name  # 输出 redis 说明成功
```

### 3. 程序连接（以Python为例，其他语言同理）
先装依赖：
```bash
pip install redis
```

基础连接代码（所有功能都基于这个）：
```python
import redis

# 连接本地Redis（默认无密码）
r = redis.Redis(
    host="localhost",
    port=6379,
    db=0,  # 用0号库
    decode_responses=True  # 自动转字符串，不用手动解码
)

# 测试连通
print(r.ping())  # True 说明连接成功
```

---

## 二、核心数据结构 + 程序实战用法（8分钟）
Redis 就5种常用结构，**程序里90%场景只用这5个**，我直接给你**业务代码**。

### 1. 字符串 String：缓存、配置、计数器
**用途**：用户信息缓存、接口限流计数、配置存储
```python
# 1. 存缓存（带过期时间，最常用！）
r.set("user:1001", '{"id":1001,"name":"张三"}', ex=3600)  # 过期1小时

# 2. 取缓存
user = r.get("user:1001")
print(user)

# 3. 计数器（点赞数、访问量、订单数）
r.incr("visit:count")  # 自增+1
r.incrby("order:count", 5)  # 自增5
```

### 2. 哈希 Hash：对象存储
**用途**：存储结构化数据（用户、商品、配置）
```python
# 存一个用户对象
r.hset("user:1002", mapping={
    "name": "李四",
    "age": 25,
    "phone": "13800138000"
})

# 取全部字段
print(r.hgetall("user:1002"))

# 取单个字段
print(r.hget("user:1002", "name"))
```

### 3. 列表 List：消息队列、历史记录
**用途**：消息队列、操作日志、浏览历史、任务队列
```python
# 右边加入消息
r.rpush("msg:queue", "订单创建", "订单支付", "订单发货")

# 左边取出消息（先进先出=队列）
msg = r.lpop("msg:queue")
print("处理消息：", msg)

# 取历史记录（最新10条）
history = r.lrange("user:1001:history", 0, 9)
```

### 4. 集合 Set：去重、共同好友、黑名单
**用途**：IP黑名单、用户标签、共同关注、去重统计
```python
# 添加黑名单IP
r.sadd("black:ip", "192.168.1.1", "192.168.1.2")

# 判断是否在黑名单
print(r.sismember("black:ip", "192.168.1.1"))  # True/False

# 统计UV（独立访客，自动去重）
r.sadd("uv:20250101", "user1", "user2", "user1")
print(r.scard("uv:20250101"))  # 输出 2
```

### 5. 有序集合 ZSet：排行榜、热门列表
**用途**：商品销量排行、文章热度榜、用户积分排行
```python
# 添加分数（score=分数，value=用户ID）
r.zadd("score:rank", {
    "user1": 98,
    "user2": 95,
    "user3": 100
})

# 取前三名（从大到小）
top3 = r.zrevrange("score:rank", 0, 2, withscores=True)
print("排行榜：", top3)
```

---

## 三、程序必备高级功能（直接用）

### 1. 分布式锁（解决并发问题）
秒杀、库存扣减、重复提交必备：
```python
# 加锁（过期时间防止死锁）
lock_key = "lock:order:1001"
if r.set(lock_key, "locked", nx=True, ex=10):
    try:
        # 执行业务逻辑
        print("处理订单...")
    finally:
        # 释放锁
        r.delete(lock_key)
else:
    print("请求频繁，请稍后再试")
```

### 2. 键过期 + 自动清理
```python
# 验证码5分钟过期
r.set("code:13800138000", "123456", ex=300)

# 设置已有键过期
r.expire("user:1001", 60)  # 60秒后过期
```

### 3. 批量操作（提升性能）
```python
# 管道批量执行，减少网络请求
pipe = r.pipeline()
pipe.set("a", 1)
pipe.set("b", 2)
pipe.incr("c")
pipe.execute()
```

---

## 四、程序里怎么用？（标准架构）
你直接套这个模板：
```python
def get_user_info(user_id):
    # 1. 先查Redis缓存
    key = f"user:{user_id}"
    cache = r.get(key)
    if cache:
        return json.loads(cache)
    
    # 2. 缓存没命中，查数据库
    user = db.query("select * from user where id=%s", user_id)
    
    # 3. 写入缓存（带过期）
    r.set(key, json.dumps(user), ex=3600)
    return user
```

---

## 五、必背常用命令（速查）
```bash
keys *  # 查看所有键
get key  # 取值
del key  # 删除键
expire key 秒  # 设置过期
flushdb  # 清空当前库（慎用）
info  # 查看服务状态
```

---

### 总结
1. Redis 就是**内存高速数据库**，用来给程序做**缓存、计数器、队列、锁、排行**
2. 5种结构：字符串(缓存)、哈希(对象)、列表(队列)、集合(去重)、ZSet(排行)
3. 程序核心用法：**先查缓存 → 没查到查库 → 写入缓存**
4. 分布式锁、过期时间是生产必备


# Java 快速上手 Redis 完整实战
适配 **Java 后端** 技术栈，使用目前企业标配：**SpringBoot + Spring Data Redis**，从零到可直接上线使用，覆盖日常开发全部场景。

## 一、环境依赖（Maven）
```xml
<!-- SpringBoot 整合 Redis 核心依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

## 二、全局配置（application.yml）
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 10000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 2
```

## 三、Redis 配置类（序列化+全局模板）
解决 JdkSerializationRedisSerializer 序列化乱码、key 超长问题，**企业级标准写法**
```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key 序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // value 序列化
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
```

## 四、注入使用
```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;
```

---

# 五、五大核心数据结构 实战代码

## 1. String 字符串（缓存、计数器、验证码）
```java
// 存入，指定过期时间 3600秒
redisTemplate.opsForValue().set("user:1001","张三",3600, TimeUnit.SECONDS);

// 读取
String name = (String) redisTemplate.opsForValue().get("user:1001");

// 计数器自增
redisTemplate.opsForValue().increment("visit:count");

// 验证码 5分钟过期
redisTemplate.opsForValue().set("sms:138000","666666",5,TimeUnit.MINUTES);
```

## 2. Hash 哈希（存储对象、用户信息）
```java
// 单字段设置
redisTemplate.opsForHash().put("user:1002","name","李四");
redisTemplate.opsForHash().put("user:1002","age",22);

// 获取单字段
Object name = redisTemplate.opsForHash().get("user:1002","name");

// 获取全部字段
Map<Object,Object> userMap = redisTemplate.opsForHash().entries("user:1002");
```

## 3. List 列表（消息队列、操作日志）
```java
// 右侧入队
redisTemplate.opsForList().rightPush("task:queue","订单创建");
redisTemplate.opsForList().rightPush("task:queue","订单支付");

// 左侧出队（FIFO 简单队列）
Object task = redisTemplate.opsForList().leftPop("task:queue");

// 查询范围数据
List<Object> history = redisTemplate.opsForList().range("user:history",0,10);
```

## 4. Set 集合（去重、黑名单、标签）
```java
// 添加黑名单
redisTemplate.opsForSet().add("black:ip","192.168.1.1","192.168.1.2");

// 判断是否存在
boolean isBlack = redisTemplate.opsForSet().isMember("black:ip","192.168.1.1");

// 统计数量（UV 统计）
Long uv = redisTemplate.opsForSet().size("uv:day");
```

## 5. ZSet 有序集合（排行榜、积分榜单）
```java
// 添加带分数数据
redisTemplate.opsForZSet().add("rank:score","user1",95);
redisTemplate.opsForZSet().add("rank:score","user2",100);

// 倒序查询 Top3
Set<ZSetOperations.TypedTuple<Object>> topList =
        redisTemplate.opsForZSet().reverseRangeWithScores("rank:score",0,2);
```

---

# 六、Java 开发高频必备功能

## 1. 通用过期 / 删除
```java
// 设置过期
redisTemplate.expire("user:1001",30,TimeUnit.MINUTES);

// 删除 key
redisTemplate.delete("user:1001");

// 判断 key 是否存在
boolean hasKey = redisTemplate.hasKey("sms:138000");
```

## 2. 分布式锁（Java 极简版，防重复提交/秒杀）
```java
public boolean tryLock(String key, long expireSec) {
    // setIfAbsent = NX 不存在才设置，原子操作
    return redisTemplate.opsForValue().setIfAbsent(key, "lock", expireSec, TimeUnit.SECONDS);
}

public void releaseLock(String key) {
    redisTemplate.delete(key);
}

// 使用示例
String lockKey = "lock:order:20260427";
if(tryLock(lockKey,10)){
    try {
        // 业务逻辑
    }finally {
        releaseLock(lockKey);
    }
}else {
    System.out.println("请勿重复提交");
}
```

## 3. 缓存标准业务模板（数据库+缓存）
```java
public User getUserById(Long userId){
    String key = "user:" + userId;
    // 1.查询缓存
    Object cache = redisTemplate.opsForValue().get(key);
    if(cache != null){
        return (User) cache;
    }
    // 2.查询数据库
    User user = userMapper.selectById(userId);
    // 3.写入缓存
    if(user != null){
        redisTemplate.opsForValue().set(key,user,1,TimeUnit.HOURS);
    }
    return user;
}
```

---

# 七、极简工具类（直接复制即用）
封装常用方法，项目中直接依赖调用
```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long time) {
        if(time > 0){
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
        }else{
            set(key,value);
        }
    }

    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    public boolean del(String... key) {
        if(key != null && key.length > 0){
            if(key.length == 1){
                return redisTemplate.delete(key[0]);
            }else{
                return redisTemplate.delete(CollectionUtils.arrayToList(key)) > 0;
            }
        }
        return false;
    }

    public boolean expire(String key, long time) {
        return redisTemplate.expire(key, time, TimeUnit.SECONDS);
    }

    public Boolean setNx(String key,Object value,long time){
        return redisTemplate.opsForValue().setIfAbsent(key,value,time,TimeUnit.SECONDS);
    }
}
```

---

# 八、核心总结
1. Java 后端统一技术栈：**SpringBoot + Spring Data Redis + Lettuce**
2. 开发核心场景：
   - String：接口缓存、限流、验证码
   - Hash：用户/商品结构化数据
   - List：简易任务队列、日志
   - Set：去重、黑名单
   - ZSet：排行榜
3. 生产必用：**过期时间 + 序列化配置 + 分布式锁 + 缓存穿透模板**
