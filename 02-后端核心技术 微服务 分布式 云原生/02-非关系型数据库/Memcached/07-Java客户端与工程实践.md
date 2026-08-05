# Java客户端与工程实践
> Xmemcached 使用、连接池与序列化、Spring 集成、Cache-Aside 实战：Java 生态使用 Memcached 的完整指南。

---

## 📚 目录

1. [Xmemcached 客户端](#1-xmemcached-客户端)
2. [连接与配置](#2-连接与配置)
3. [核心操作](#3-核心操作)
4. [序列化策略](#4-序列化策略)
5. [Spring 集成](#5-spring-集成)
6. [工程模式与实战](#6-工程模式与实战)

---

## 1. Xmemcached 客户端

### 1.1 选型

| 客户端 | 特点 | 状态 |
|--------|------|------|
| **Xmemcached** | 主流、高性能、NIO | ✅ 推荐 |
| Memcached-java-client | 老牌、稳定 | 维护放缓 |
| Spymemcached | 功能全、异步 | 维护放缓 |

```text
Xmemcached 特点：
  ① NIO 实现（非阻塞，高并发连接）
  ② 二进制协议支持（默认）
  ③ 一致性哈希（Ketama）内置
  ④ 连接池/故障转移
  ⑤ Spring 集成友好
```

### 1.2 依赖

```xml
<dependency>
    <groupId>com.googlecode.xmemcached</groupId>
    <artifactId>xmemcached</artifactId>
    <version>2.4.8</version>
</dependency>
```

```text
注意：Xmemcached 依赖 slf4j（日志）
  版本兼容：2.4.x 支持 JDK 8+
  替代：spring-boot-starter-cache + memcached provider
```

---

## 2. 连接与配置

### 2.1 基础连接

```java
// 单节点
XMemcachedClient client = new XMemcachedClient("127.0.0.1", 11211);

// 多节点（一致性哈希路由）
XMemcachedClient client = new XMemcachedClient();
client.addServer("10.0.0.1:11211");
client.addServer("10.0.0.2:11211");
client.addServer("10.0.0.3:11211");
// → 客户端自动一致性哈希（Ketama）
```

### 2.2 连接池配置

```java
// Xmemcached 连接池（XMemcachedClientBuilder）
XMemcachedClientBuilder builder = new XMemcachedClientBuilder(
    AddrUtil.getAddresses("10.0.0.1:11211 10.0.0.2:11211"));

builder.setConnectionPoolSize(5);          // 每节点连接池大小
builder.setOpTimeout(3000);                // 操作超时（ms）
builder.setConnectTimeout(3000);           // 连接超时（ms）
builder.setFailureMode(true);              // 故障转移模式
builder.setCommandFactory(new BinaryCommandFactory());  // 二进制协议

XMemcachedClient client = (XMemcachedClient) builder.build();
```

| 配置 | 说明 | 建议 |
|------|------|------|
| connectionPoolSize | 每节点连接数 | 5-10（高并发可增） |
| opTimeout | 操作超时 | 1000-3000ms |
| failureMode | 故障转移 | true（节点失败路由他处） |
| commandFactory | 协议 | Binary（效率） |
| sessionName | 节点权重 | 大节点加权 |

### 2.3 生命周期管理

```java
// 优雅关闭
client.shutdown();

// Spring 管理：@Bean + @PreDestroy
@Configuration
public class MemcachedConfig {
    @Bean(destroyMethod = "shutdown")
    public XMemcachedClient memcachedClient() throws IOException {
        XMemcachedClientBuilder builder = new XMemcachedClientBuilder(
            AddrUtil.getAddresses("127.0.0.1:11211"));
        builder.setConnectionPoolSize(5);
        return (XMemcachedClient) builder.build();
    }
}
```

---

## 3. 核心操作

### 3.1 基础 CRUD

```java
// 存储（TTL 秒；0 = 永不过期）
client.set("key", 60, value);           // set（无条件）
client.add("key", 60, value);           // add（仅不存在）
client.replace("key", 60, value);       // replace（仅存在）
client.append("key", "suffix");         // append（追加）

// 读取
Object v = client.get("key");           // 单键
Map<String, Object> map = client.getBulk(keys);  // 批量（推荐）

// 删除
client.delete("key");
client.deleteWithNoReply("key");        // 异步删除

// 计数器
client.incr("counter", 1);              // 自增（键需存在）
client.decr("counter", 1);              // 自减
```

### 3.2 批量操作（性能关键）

```java
// getBulk：一次往返取多键（网络优化）
List<String> keys = List.of("u:1", "u:2", "u:3");
Map<String, Object> users = client.getBulk(keys);

// 异步写（noreply）：不等待响应
client.setWithNoReply("key", 60, value);

// 管道：连续发送（Xmemcached 自动批量）
for (int i = 0; i < 1000; i++) {
    client.setWithNoReply("k:" + i, 60, data);
}
```

### 3.3 CAS 操作

```java
// 并发安全更新（gets + cas）
GetsResponse<Integer> resp = client.gets("counter");
long cas = resp.getCas();                    // 版本号
int current = resp.getValue();

boolean success = client.cas("counter", 60, current + 1, cas);
if (!success) {
    // EXISTS：被其他客户端修改 → 重试
    // NOT_FOUND：键被删除/淘汰 → 重新初始化
}
```

---

## 4. 序列化策略

### 4.1 序列化方式

| 方式 | 优点 | 缺点 | 适用 |
|------|------|------|------|
| Java 序列化 | 内置 | 慢、大、不跨语言 | 不推荐 |
| JSON | 可读、跨语言 | 较大、较慢 | 通用 |
| Protobuf | 小、快、强类型 | 需定义 schema | 高性能 |
| Kryo | 小、快 | 兼容性注意 | 高性能 |

```text
Xmemcached 默认：Java 序列化（Transcoder）
  可自定义 Transcoder（JSON/Protobuf/Kryo）

序列化选择原则：
  ① 跨语言 → JSON/Protobuf
  ② 性能敏感 → Protobuf/Kryo
  ③ 简单内部 → JSON（平衡）
```

### 4.2 自定义 Transcoder（JSON）

```java
// 使用 Gson/Jackson 的 Transcoder
public class JsonTranscoder implements Transcoder<Object> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public CachedData encode(Object o) {
        try {
            byte[] data = mapper.writeValueAsBytes(o);
            return new CachedData(0, data, data.length);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public Object decode(CachedData d) {
        try {
            return mapper.readValue(d.getData(), Object.class);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}

// 使用
client.setTranscoder(new JsonTranscoder());
```

### 4.3 压缩策略

```text
大值压缩：
  值 > 阈值（如 1KB）→ gzip 压缩后存储
  读取时解压

权衡：
  压缩：CPU 换带宽（大值收益明显）
  小值：不压缩（CPU 开销 > 带宽节省）

工程建议：
  阈值 1-4KB 起步
  文本类压缩率高（JSON/HTML）
  已压缩格式（图片）不压缩
```

---

## 5. Spring 集成

### 5.1 Spring Cache 抽象

```java
// Spring Cache 注解（@Cacheable 等）
@Service
public class UserService {
    @Cacheable(value = "users", key = "#id")
    public User getUser(Long id) { return userDao.findById(id); }

    @CacheEvict(value = "users", key = "#user.id")
    public void updateUser(User user) { userDao.update(user); }
}
```

```text
需要：Memcached CacheManager 实现
  选项：
    ① xmemcached-spring（Xmemcached 提供）
    ② 自研 CacheManager（简单封装）
    ③ spring-data-redis 风格类比（Memcached 无官方 starter）

注意：Spring Cache 的注解模式
  → 适用于"简单 Cache-Aside"
  → 复杂场景（批量/计数器/手动控制）用客户端直连
```

### 5.2 手动 Cache-Aside（推荐）

```java
@Service
public class ProductService {
    private final XMemcachedClient cache;
    private final ProductDao dao;

    // 读：Cache-Aside
    public Product getProduct(Long id) {
        String key = "product:" + id;
        Product p = (Product) cache.get(key);      // ① 查缓存
        if (p != null) return p;
        p = dao.findById(id);                       // ② miss → 查库
        if (p != null) cache.set(key, 300, p);      // ③ 回填
        return p;
    }

    // 写：先写库后删缓存（避免脏读）
    @Transactional
    public void updateProduct(Product p) {
        dao.update(p);                              // ① 写库
        cache.deleteWithNoReply("product:" + p.getId());  // ② 删缓存
    }
}
```

### 5.3 工程模式总结

```text
推荐组合：
  简单缓存 → Spring @Cacheable（注解）
  业务缓存 → 手动 Cache-Aside（可控）
  高并发读 → 多级（本地 + Memcached）
  计数/限流 → incr/decr（Memcached 原生）
  防击穿/穿透 → 互斥重建/空值缓存（06 模块）

注意：
  Spring Cache 注解的 key 生成器（SpEL）
  TTL 配置（@Cacheable 无 TTL 参数 → CacheManager 统一）
  序列化统一（Transcoder 全局）
```

---

## 6. 工程模式与实战

### 6.1 热点缓存模板（防击穿）

```java
public Product getProductSafe(Long id) {
    String key = "product:" + id;
    Product p = (Product) cache.get(key);
    if (p != null) return p;

    // 互斥重建（防击穿：单请求回源）
    String lockKey = "lock:" + key;
    if (cache.add(lockKey, 10, "1")) {       // add：抢锁（仅不存在成功）
        try {
            p = dao.findById(id);
            if (p != null) cache.set(key, 300, p);
            else cache.set(key, 60, EMPTY);   // 空值缓存（防穿透）
        } finally {
            cache.delete(lockKey);
        }
    } else {
        // 其他请求：短暂等待后重试/返回旧值
        Thread.sleep(50);
        return (Product) cache.get(key);
    }
    return p;
}
```

### 6.2 会话缓存模式

```java
// 会话缓存（可丢失场景）
public void saveSession(String sessionId, SessionData data) {
    cache.set("session:" + sessionId, 1800, data);  // 30 分钟
}

public SessionData getSession(String sessionId) {
    SessionData data = (SessionData) cache.get("session:" + sessionId);
    if (data != null) {
        cache.touch("session:" + sessionId, 1800);  // 滑动过期
    }
    return data;
}
```

### 6.3 常见错误清单

| 错误 | 后果 | 对策 |
|------|------|------|
| 用 Java 序列化 | 慢/大/难升级 | JSON/Protobuf |
| 无连接池 | 频繁建连（慢） | builder 配置池 |
| 忽略批量 | 每键一次往返 | getBulk |
| TTL 不设置 | 永久占内存 | 明确 TTL |
| 缓存写后不删 | 脏数据 | 写库后删缓存 |
| 无降级路径 | 缓存挂 = 系统挂 | try-catch 降级 |
| 大对象直接缓存 | 1MB 限制/性能差 | 拆分/压缩/存引用 |

> 🎯 **核心要点**：Java 集成 = Xmemcached（NIO + Ketama + 连接池）+ 合理序列化（JSON/Protobuf）+ Spring 注解或手动 Cache-Aside。三个工程纪律：**批量（getBulk/异步写）提升吞吐、写库后删缓存保一致性、缓存异常必须有降级路径**。防击穿/穿透模板（互斥重建 + 空值缓存）是生产必备技能。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| Java 客户端？ | Xmemcached（NIO/二进制/Ketama） |
| 连接池配置？ | connectionPoolSize 5-10、failureMode=true |
| 批量操作？ | getBulk（读）/ setWithNoReply（写） |
| 序列化？ | Java 默认 → JSON/Protobuf（自定义 Transcoder） |
| Spring 集成？ | @Cacheable（简单）或手动 Cache-Aside（推荐） |
| 防击穿模板？ | add 抢锁 + 单请求回源 + 空值缓存 |

**下一模块**：[08-选型：Memcached vs Redis](08-选型：Memcached-vs-Redis.md)　**返回总览**：[00-Memcached知识体系总览](00-Memcached知识体系总览.md)
