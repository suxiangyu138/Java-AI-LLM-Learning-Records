# 16-分布式锁-Redisson整合
> 🎯 Redisson是Redis官方推荐的Java分布式锁框架 — 从锁误删、看门狗、RedLock到Lua原子性，彻底掌握生产级分布式锁的实现与坑

---

## 目录
1. [本章总览](#1-本章总览)
2. [分层理论讲解](#2-分层理论讲解)
3. [高频踩坑与误区](#3-高频踩坑与误区)
4. [随堂基础练习](#4-随堂基础练习)
5. [章节综合实操案例](#5-章节综合实操案例)
6. [分层综合习题](#6-分层综合习题)
7. [本章复盘速记清单](#7-本章复盘速记清单)
8. [精通拓展补充-P2](#8-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位
- **归属**：Spring全家桶与微服务 → 分布式中间件整合 → 层级2 P1 就业必备
- **前置依赖**：Java并发（JUC）+ Redis基础 + Spring Boot + Lua脚本基础
- **后续衔接**：分布式事务 Seata → 微服务全链路治理
- **重要性**：⭐⭐⭐⭐⭐（高并发分布式系统面试必考、实战必备）

### 1.2 三层学习目标

| 级别 | 目标 |
|------|------|
| **基础** | 理解分布式锁四大需求，掌握Redisson+Spring Boot整合，使用RLock做互斥控制 |
| **熟练** | 吃透看门狗自动续期机制，理解Lua脚本原子性原理，能解决锁误删问题 |
| **精通** | 掌握RedLock算法原理与缺陷，对比Redis/DB/ZK三种分布式锁的选型依据 |

### 1.3 本章知识点脑图

```
分布式锁 Redisson 整合
├── 分布式锁基础理论
│   ├── 四大需求：互斥 / 防死锁 / 容错 / 可重入
│   ├── 方案对比：DB → Redis → ZK
│   └── 为什么不能只用 SETNX
├── Redisson 核心功能
│   ├── RLock（可重入锁 + Watch Dog）
│   ├── RReadWriteLock（读写锁）
│   ├── RSemaphore（分布式信号量）
│   ├── RCountDownLatch（分布式倒计时器）
│   └── RedLock（红锁算法）
├── 底层原理
│   ├── Lua脚本保证原子性
│   ├── Watch Dog 自动续期机制
│   └── Pub/Sub 锁通知机制
├── 高频踩坑
│   ├── 锁误删问题与UUID方案
│   ├── 未设置leaseTime导致死锁
│   ├── RedLock在异步复制下的漏洞
│   └── 锁粒度过大导致性能瓶颈
└── 综合实战
    └── 订单防重提交（100并发压测）
```

---

## 2. 分层理论讲解

### 2.1 分布式锁基础理论

#### 2.1.1 什么是分布式锁

在单机多线程环境下，Java 的 `synchronized` 或 `ReentrantLock` 可以保证同一 JVM 内线程互斥。但在分布式系统中，多个服务实例运行在不同 JVM 上，本地锁无法跨进程生效，此时需要 **分布式锁**。

```java
// 单机锁 — 仅对当前JVM内的线程有效
public synchronized String createOrder(String userId) {
    // 无法阻止另一个服务实例的并发请求
}
```

#### 2.1.2 分布式锁的四大核心需求

| 需求 | 说明 | 不满足的后果 |
|------|------|-------------|
| **互斥性（Mutual Exclusion）** | 任意时刻，只有一个客户端持有锁 | 并发资源竞争，数据不一致 |
| **防死锁（Deadlock Free）** | 持有锁的客户端崩溃后，锁能自动释放 | 系统永久阻塞，服务不可用 |
| **容错性（Fault Tolerance）** | Redis集群部分节点宕机不影响锁服务 | 单点故障导致锁服务不可用 |
| **可重入性（Reentrancy）** | 同一个线程可重复获取同一把锁 | 递归/嵌套调用死锁 |

#### 2.1.3 为什么不用 Redis SETNX 实现？

使用原生 `SETNX` 实现锁时，开发者需要自行处理大量边界问题：

```java
// 初级版本 — 有问题
Boolean flag = redisTemplate.opsForValue().setIfAbsent("lock_key", "1");
if (flag) {
    // 执行业务逻辑
    redisTemplate.delete("lock_key"); // 问题：业务异常时锁无法释放
}
```

```java
// 中级版本 — 仍有问题
Boolean flag = redisTemplate.opsForValue()
    .setIfAbsent("lock_key", "1", 30, TimeUnit.SECONDS);
if (flag) {
    try {
        // 执行业务逻辑
    } finally {
        redisTemplate.delete("lock_key"); // 问题：可能删除别人的锁
    }
}
```

**原生 SETNX 方案的痛点**：

| 问题 | 说明 |
|------|------|
| 锁误删 | 线程A锁超时释放，线程B获取锁，A执行finally删除B的锁 |
| 未设置过期时间 | 实例宕机导致死锁 |
| 不可重入 | 同步方法内调用另一个同步方法导致死锁 |
| 无法续期 | 业务执行时间超过锁超时时间，锁自动释放 |
| 无阻塞等待 | 未获取到锁直接返回失败，不支持等待 |
| 无公平/非公平语义 | 无法控制锁获取顺序 |

**Redisson 正是为解决上述所有问题而生**。

---

### 2.2 Redisson 概述

#### 2.2.1 什么是 Redisson

Redisson 是 Redis 官方推荐的 Java 客户端，提供了 **分布式锁（Lock）、集合（Collection）、队列（Queue）、计数器（AtomicLong）** 等 30+ 种分布式数据结构和同步器。

> 💡 Redisson 本质是一个运行在 JVM 上的 **内存数据网格（In-Memory Data Grid）**，其分布式锁的功能远远超过原生 SETNX。

#### 2.2.2 Redisson 核心特性矩阵

| 特性 | Redisson | 原生 SETNX | 说明 |
|------|----------|------------|------|
| 自动续期（Watch Dog） | ✅ 内置 | ❌ 需手动实现 | 业务未完成时自动延长锁TTL |
| 可重入 | ✅ 支持 | ❌ 需手动实现 | 基于Redis Hash记录线程+重入计数 |
| 锁等待 | ✅ tryLock支持 | ❌ 不支持 | 可配置等待超时时间 |
| 公平锁 | ✅ FairLock | ❌ 不支持 | 按请求顺序排队 |
| 读写锁 | ✅ RReadWriteLock | ❌ 不支持 | 读读并发、读写互斥 |
| 信号量 | ✅ RSemaphore | ❌ 不支持 | 限流、资源池控制 |
| 原子释放 | ✅ Lua脚本 | ❌ 需自行实现 | 检查+删除原子操作 |
| 红锁（RedLock） | ✅ RedissonRedLock | ❌ 不支持 | 多主节点容灾 |

---

### 2.3 Redisson + Spring Boot 整合

#### 2.3.1 Maven 依赖

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Redisson 官方 Starter（Spring Boot 3.x 兼容） -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.27.2</version>
    </dependency>

    <!-- 可选：分布式集合与队列 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-data-33</artifactId>
        <version>3.27.2</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

> ⚠️ `redisson-spring-boot-starter` 会替换 Spring Boot 默认的 RedisTemplate 自动配置。如果需要同时使用 Redisson 和 RedisTemplate，需要额外配置。

#### 2.3.2 application.yml 配置

```yaml
spring:
  application:
    name: order-service

  # Redis 单节点模式
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: 
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4

# Redisson 独立配置（推荐使用独立配置而非 spring.redis.*）
redisson:
  address: redis://127.0.0.1:6379
  password:
  database: 0
  connection-pool-size: 64
  connection-minimum-idle-size: 16
  idle-connection-timeout: 10000
  connect-timeout: 5000
  retry-attempts: 3
  retry-interval: 1500
```

#### 2.3.3 Redisson 配置类

```java
package com.example.order.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${redisson.address}")
    private String address;

    @Value("${redisson.password:}")
    private String password;

    @Value("${redisson.database:0}")
    private int database;

    @Value("${redisson.connection-pool-size:64}")
    private int connectionPoolSize;

    @Value("${redisson.connection-minimum-idle-size:16}")
    private int connectionMinimumIdleSize;

    @Value("${redisson.connect-timeout:5000}")
    private int connectTimeout;

    /**
     * 单节点模式 RedissonClient
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password.isEmpty() ? null : password)
                .setDatabase(database)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectTimeout(connectTimeout)
                // Watch Dog 超时配置（默认30秒）
                .setTimeout(10000)
                // 心跳检测
                .setPingConnectionInterval(30000);

        return Redisson.create(config);
    }

    /**
     * 集群模式（适用于生产环境）
     * 当使用 RedLock 时，需要配置多个独立 Redis 节点
     */
    // @Bean
    // public RedissonClient redissonClientCluster() {
    //     Config config = new Config();
    //     config.useClusterServers()
    //             .addNodeAddress(
    //                 "redis://node1:6379",
    //                 "redis://node2:6379",
    //                 "redis://node3:6379"
    //             )
    //             .setPassword(password.isEmpty() ? null : password)
    //             .setMasterConnectionPoolSize(64)
    //             .setSlaveConnectionPoolSize(32);
    //     return Redisson.create(config);
    // }
}
```

#### 2.3.4 验证整合是否成功

```java
package com.example.order;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class OrderApplication {

    private final RedissonClient redissonClient;

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @PostConstruct
    public void checkRedisson() {
        log.info("RedissonClient initialized: {}", redissonClient);
        log.info("RedissonClient node: {}", redissonClient.getNodesGroup());
    }
}
```

---

### 2.4 RLock — 可重入分布式锁

#### 2.4.1 基本使用

`RLock` 是 Redisson 最核心的锁接口，实现了 `java.util.concurrent.locks.Lock` 接口。

```java
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final RedissonClient redissonClient;

    /**
     * 基础加锁 — 等同于 synchronized 语义
     */
    public void basicLock(String productId) {
        RLock lock = redissonClient.getLock("lock:product:" + productId);
        lock.lock(); // 阻塞等待，默认 leaseTime = 30秒（由Watch Dog自动续期）
        try {
            // 临界区代码
            doDeductStock(productId);
        } finally {
            lock.unlock(); // 必须在 finally 中释放
        }
    }

    private void doDeductStock(String productId) {
        // 执行库存扣减逻辑
    }
}
```

#### 2.4.2 tryLock — 带超时的非阻塞获取

```java
/**
 * tryLock — 尝试获取锁，最多等待 3 秒，获取后 10 秒自动释放
 * 返回 true 表示获取成功，false 表示获取失败
 */
public boolean tryLockExample(String productId) {
    RLock lock = redissonClient.getLock("lock:product:" + productId);
    boolean acquired = false;
    try {
        // 参数1: 等待时间（waitTime）— 获取不到锁时最大等待时长
        // 参数2: 租期时间（leaseTime）— 获取锁后自动释放时间（-1表示由Watch Dog续期）
        // 参数3: 时间单位
        acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
        if (acquired) {
            doDeductStock(productId);
            return true;
        } else {
            log.warn("获取锁失败，productId={}", productId);
            return false;
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("锁获取被中断", e);
        return false;
    } finally {
        if (acquired) {
            lock.unlock();
        }
    }
}
```

#### 2.4.3 lockInterruptibly — 可中断锁

```java
/**
 * lockInterruptibly — 可中断锁
 * 线程在等待锁的过程中可以被中断，避免死锁
 */
public void interruptibleLock(String productId) throws InterruptedException {
    RLock lock = redissonClient.getLock("lock:product:" + productId);
    lock.lockInterruptibly(10, TimeUnit.SECONDS); // 等待10秒，可被中断
    try {
        doDeductStock(productId);
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

#### 2.4.4 可重入性演示

```java
/**
 * 可重入性 — 同一个线程可多次获取同一把锁
 * 底层通过 Redis Hash + 重入计数实现
 */
public void reentrantDemo(String userId) {
    RLock lock = redissonClient.getLock("lock:user:" + userId);

    lock.lock(); // 第一次获取锁
    try {
        log.info("第一次获取锁成功");
        innerMethod(userId, lock); // 内部再次获取同一把锁
    } finally {
        lock.unlock(); // 第一次释放
    }
}

private void innerMethod(String userId, RLock lock) {
    lock.lock(); // 第二次获取锁（可重入）
    try {
        log.info("第二次获取锁成功 — 同一线程可重入");
        // 执行业务
    } finally {
        lock.unlock(); // 第二次释放
    }
}
```

#### 2.4.5 leaseTime 参数详解

| leaseTime | 行为 | 适用场景 |
|-----------|------|---------|
| `-1`（默认） | Watch Dog 自动续期，每 10 秒续一次，总时长 30 秒 | 业务执行时间不可预估 |
| 指定秒数（如 10L） | 到期自动释放，不启动 Watch Dog | 业务执行时间固定且有上限 |
| 不设置 | 调用 `lock()` 走 Watch Dog；`tryLock(wait, unit)` leaseTime=30 秒 | 需要兜底超时 |

> 💡 **最佳实践**：如果能预估业务执行时间，优先设置 `leaseTime` 避免 Watch Dog 的额外开销；如果不能预估（如复杂计算、远程调用），使用 Watch Dog 自动续期。

---

### 2.5 Watch Dog（看门狗）自动续期机制

#### 2.5.1 工作原理

Watch Dog 是 Redisson 分布式锁的 **核心创新机制**，解决了锁超时与业务执行时间不匹配的问题。

```
时序图：

线程A获取锁成功
    │
    ├── Redis中写入锁（key=lock:xxx, leaseTime=30s）
    │
    ├── 启动 Watch Dog 定时任务（每 internalLockLeaseTime/3 ≈ 10s 执行一次）
    │
    ├── 第10秒：检查锁是否仍被持有 → 是 → 续期到30秒
    │
    ├── 第20秒：检查锁是否仍被持有 → 是 → 续期到30秒
    │
    └── 第30秒：业务完成 → 手动 unlock() 释放锁 → 取消 Watch Dog
```

#### 2.5.2 关键参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `lockWatchdogTimeout` | 30_000 ms | 锁的租期时间（Watch Dog 续期目标） |
| 续期间隔 | `lockWatchdogTimeout / 3` ≈ 10 秒 | 每 10 秒检查并续期一次 |
| 最小超时时间 | 无 | 通过 `config.setLockWatchdogTimeout(ms)` 自定义 |

#### 2.5.3 配置 Watch Dog 超时

```java
@Bean
public RedissonClient redissonClient() {
    Config config = new Config();
    config.setLockWatchdogTimeout(15_000); // 修改默认30秒为15秒
    config.useSingleServer()
            .setAddress("redis://127.0.0.1:6379");

    return Redisson.create(config);
}
```

> ⚠️ **Watch Dog 的触发条件**：仅当使用 `lock.lock()` 或 `lock.tryLock(waitTime, -1, unit)` 且 `leaseTime` 为 `-1` 时，Watch Dog 才会启动。如果显式指定了 `leaseTime`，则到期强制释放，不启动 Watch Dog。

#### 2.5.4 Watch Dog 源码核心逻辑（伪代码）

```java
// RedissonLock 内部 — 简化逻辑
private void scheduleExpirationRenewal(long threadId) {
    // 创建定时任务，每 internalLockLeaseTime/3 执行一次
    Timeout task = commandExecutor.getConnectionManager()
        .newTimerTask(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                // Lua脚本：检查锁是否仍被当前线程持有，如是则重置TTL
                RFuture<Boolean> future = renewExpirationAsync(threadId);
                future.onComplete((res, e) -> {
                    if (res) {
                        // 续期成功，继续调度下一次续期
                        scheduleExpirationRenewal(threadId);
                    }
                });
            }
        }, internalLockLeaseTime / 3, TimeUnit.MILLISECONDS);
}

// 续期 Lua 脚本
// if redis.call('hexists', KEYS[1], ARGV[2]) == 1 then
//     redis.call('pexpire', KEYS[1], ARGV[1]);
//     return 1;
// end;
// return 0;
```

---

### 2.6 Lua 脚本原子性

#### 2.6.1 为什么需要 Lua

Redis 单条命令是原子性的，但**多条命令组合不是原子性的**。分布式锁的核心操作涉及：

1. **检查线程身份** → 2. **释放锁** = 两步操作，非原子

如果这两步之间发生上下文切换或其他线程干扰，就会导致 **锁误删**。

**Lua 脚本的优势**：

| 特性 | 说明 |
|------|------|
| 原子执行 | Redis 执行 Lua 脚本是单线程的，脚本执行期间不会插入其他命令 |
| 减少网络IO | 多条命令打包成一条，减少 RTT |
| 事务性 | 脚本执行过程中 Redis 不会处理其他请求 |
| 可编程 | 支持条件判断、循环等逻辑 |

#### 2.6.2 解锁 Lua 脚本

Redisson 解锁时运行的核心 Lua 脚本：

```lua
-- 解锁 Lua 脚本（KEYS[1] = 锁的key, ARGV[1] = leaseTime, ARGV[2] = 线程标识）
-- 检查锁是否存在
if (redis.call('hexists', KEYS[1], ARGV[2]) == 0) then
    -- 锁不存在或不属于当前线程，直接返回nil
    return nil;
end;

-- 递减重入计数
local counter = redis.call('hincrby', KEYS[1], ARGV[2], -1);

if (counter > 0) then
    -- 重入计数 > 0，表示有多次重入，仅重置过期时间
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return 0;
else
    -- 重入计数归零，删除锁
    redis.call('del', KEYS[1]);
    -- 发布解锁消息（通知等待线程）
    redis.call('publish', KEYS[2], ARGV[2]);
    return 1;
end;
```

#### 2.6.3 自定义 Lua 锁脚本

```lua
-- 自定义加锁脚本 — 带业务校验的锁
-- KEYS[1]: 锁的key
-- KEYS[2]: 业务状态key（如订单状态）
-- ARGV[1]: 线程标识（UUID:threadId）
-- ARGV[2]: 到期时间（毫秒）
-- ARGV[3]: 可接受的业务状态

-- 业务校验：检查订单状态是否可操作
local orderStatus = redis.call('get', KEYS[2]);
if (orderStatus == nil or orderStatus ~= ARGV[3]) then
    return 2;  -- 业务状态不满足，返回特殊值
end;

-- 加锁：使用Hash记录线程标识和重入计数
local exists = redis.call('hexists', KEYS[1], ARGV[1]);
if (exists == 1) then
    -- 同一线程重入，重入计数+1
    redis.call('hincrby', KEYS[1], ARGV[1], 1);
    redis.call('pexpire', KEYS[1], ARGV[2]);
    return 1;  -- 加锁成功
elseif (redis.call('exists', KEYS[1]) == 0) then
    -- 锁未被占用
    redis.call('hset', KEYS[1], ARGV[1], 1);
    redis.call('pexpire', KEYS[1], ARGV[2]);
    return 1;  -- 加锁成功
else
    return 0;  -- 锁被其他线程持有
end;
```

> 💡 Redis 使用内置的 Lua 5.1 解释器，脚本内不能使用 `math.random`、`os.time` 等部分标准库函数。需随机数时建议从客户端传入。

---

### 2.7 RReadWriteLock — 分布式读写锁

#### 2.7.1 读写锁规则

| 锁状态 | 读锁 | 写锁 |
|--------|------|------|
| **无锁** | 可获取 | 可获取 |
| **已加读锁** | 可获取（读读并发） | 不可获取（读写互斥） |
| **已加写锁** | 不可获取（读写互斥） | 不可获取（写写互斥） |

#### 2.7.2 使用示例

```java
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RLock;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedissonClient redissonClient;

    /**
     * 缓存写入 — 使用写锁（排他）
     */
    public void putCache(String key, Object value) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwlock:cache:" + key);
        RLock writeLock = rwLock.writeLock();
        writeLock.lock();
        try {
            // 模拟耗时写入操作
            log.info("写入缓存: key={}", key);
            // ... 写入逻辑
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 缓存读取 — 使用读锁（可并发）
     */
    public Object getCache(String key) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwlock:cache:" + key);
        RLock readLock = rwLock.readLock();
        readLock.lock();
        try {
            log.info("读取缓存: key={}", key);
            // ... 读取逻辑
            return null;
        } finally {
            readLock.unlock();
        }
    }
}
```

#### 2.7.3 读写锁适用场景

| 场景 | 推荐方案 | 原因 |
|------|----------|------|
| 读多写少的配置缓存 | RReadWriteLock | 读读不阻塞，提升并发性能 |
| 库存扣减 | RLock | 写多，读写锁无优势 |
| 分布式定时任务调度 | RLock | 只需互斥，简单即可 |
| 热点数据兜底缓存 | RReadWriteLock | 缓存穿透时写锁互斥重建 |

---

### 2.8 RSemaphore — 分布式信号量

#### 2.8.1 基本概念

信号量（Semaphore）是一种 **资源计数限流** 工具，设置一个总资源数，多个客户端竞争获取许可。

| 操作 | 效果 |
|------|------|
| `acquire()` | 获取一个许可，无可用则阻塞 |
| `tryAcquire()` | 尝试获取许可，立即返回 true/false |
| `release()` | 释放一个许可 |
| `availablePermits()` | 查看当前可用许可数 |

#### 2.8.2 限流示例

```java
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedissonClient redissonClient;
    private static final String SEMAPHORE_KEY = "semaphore:api:order";

    /**
     * 初始化信号量（设置并发上限为10）
     */
    @PostConstruct
    public void init() {
        RSemaphore semaphore = redissonClient.getSemaphore(SEMAPHORE_KEY);
        semaphore.trySetPermits(10); // 设置许可数为10
    }

    /**
     * 基于信号量的限流
     */
    public boolean tryProcessOrder(String orderId) {
        RSemaphore semaphore = redissonClient.getSemaphore(SEMAPHORE_KEY);
        try {
            // 尝试获取许可，最多等待1秒
            boolean acquired = semaphore.tryAcquire(1, 1, TimeUnit.SECONDS);
            if (acquired) {
                log.info("获取信号量成功，处理订单: {}", orderId);
                // 处理订单
                return true;
            } else {
                log.warn("系统繁忙，请稍后重试: {}", orderId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 订单处理完成后释放信号量
     */
    public void completeOrder(String orderId) {
        RSemaphore semaphore = redissonClient.getSemaphore(SEMAPHORE_KEY);
        semaphore.release();
        log.info("释放信号量: {}", orderId);
    }
}
```

> ⚠️ `trySetPermits` 只在信号量不存在时设置成功。如需重置，先 `delete()` 再 `trySetPermits()`。

#### 2.8.3 信号量使用场景

| 场景 | 说明 |
|------|------|
| API 限流 | 限制某个接口的最大并发数 |
| 资源池控制 | 限制数据库连接、线程池等资源的使用 |
| 闸机模式 | 秒杀开始时释放信号量，控制流量进入 |
| 分布式批处理 | 限制同时执行的任务数 |

---

### 2.9 RCountDownLatch — 分布式倒计时器

#### 2.9.1 基本概念

`RCountDownLatch` 类比 JUC 的 `CountDownLatch`，用于多个分布式节点之间的任务协调：一个节点等待所有其他节点完成任务后继续执行。

#### 2.9.2 使用示例

```java
/**
 * 分布式任务协调 — 主节点等待所有工作节点完成
 */
@Service
@RequiredArgsConstructor
public class DistributedTaskService {

    private final RedissonClient redissonClient;
    private static final String LATCH_KEY = "latch:batch:task-001";

    /**
     * 主节点 — 等待所有子任务完成
     */
    public void waitForAllWorkers(int workerCount) {
        RCountDownLatch latch = redissonClient.getCountDownLatch(LATCH_KEY);
        latch.trySetCount(workerCount); // 设置倒计时总数

        log.info("主节点等待 {} 个子任务完成...", workerCount);
        try {
            boolean finished = latch.await(30, TimeUnit.SECONDS); // 最多等30秒
            if (finished) {
                log.info("所有子任务已完成");
            } else {
                log.warn("子任务超时未完成");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 工作节点 — 完成任务后倒计时
     */
    public void workerDone(String workerId) {
        RCountDownLatch latch = redissonClient.getCountDownLatch(LATCH_KEY);
        latch.countDown(); // 倒计时 -1
        log.info("工作节点 {} 完成任务", workerId);
    }
}
```

#### 2.9.3 CountDownLatch 使用场景

| 场景 | 说明 |
|------|------|
| 分布式批处理 | 所有分片任务执行完毕后汇总结果 |
| 多服务依赖启动 | 等待依赖服务全部就绪 |
| 数据迁移 | 等待所有迁移线程完成 |
| 多阶段流水线 | 等待前序阶段全部完成进入下一阶段 |

---

### 2.10 RedLock（红锁）算法

#### 2.10.1 为什么需要 RedLock

在 Redis **主从（Master-Slave）架构** 中，存在以下故障场景：

```
时间线：
1. 客户端A 向 Master 请求锁，写入成功
2. Master 在异步复制给 Slave 之前崩溃 ❌
3. Slave 晋升为新的 Master（但无锁数据）
4. 客户端B 向新的 Master 请求同一把锁 → 也成功了
5. 此时客户端A 和 客户端B 同时认为自己持有锁 → 互斥性被破坏
```

> ⚠️ **核心问题**：Redis 主从使用的是异步复制，Master 宕机，锁数据丢失。

#### 2.10.2 RedLock 算法原理

RedLock 由 Redis 作者 **antirez** 提出，使用 **N/2+1** 个互相独立的 Redis 节点（典型是 5 个）：

```
5个独立Redis节点（各自独立，无主从关系）

客户端获取锁的步骤：
┌─────────────────────────────────────────┐
│  1. 获取当前时间戳 T1                      │
│  2. 依次向所有5个节点加锁（超时短，如10ms）   │
│  3. 计算成功加锁的节点数 count              │
│  4. 计算总耗时 elapsed = now() - T1        │
│  5. 判断：count >= 3 且 elapsed < leaseTime │
│     ✅ 成功 → 持有锁（剩余有效期=leaseTime-elapsed）│
│     ❌ 失败 → 向所有节点发解锁请求             │
└─────────────────────────────────────────┘
```

#### 2.10.3 Redisson RedLock 实现

```java
import org.redisson.RedissonRedLock;

/**
 * RedLock 使用示例 — 需要多个独立的 RedissonClient
 */
@Service
@RequiredArgsConstructor
public class RedLockService {

    // 5个独立的 RedissonClient，分别连接到不同的 Redis 实例
    private final RedissonClient redissonClient1;
    private final RedissonClient redissonClient2;
    private final RedissonClient redissonClient3;
    private final RedissonClient redissonClient4;
    private final RedissonClient redissonClient5;

    public boolean executeWithRedLock(String resourceId) {
        // 分别从每个 RedissonClient 获取 RLock
        RLock lock1 = redissonClient1.getLock("redlock:" + resourceId);
        RLock lock2 = redissonClient2.getLock("redlock:" + resourceId);
        RLock lock3 = redissonClient3.getLock("redlock:" + resourceId);
        RLock lock4 = redissonClient4.getLock("redlock:" + resourceId);
        RLock lock5 = redissonClient5.getLock("redlock:" + resourceId);

        // 构建 RedLock（需要至少3/5节点成功）
        RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3, lock4, lock5);

        boolean acquired = false;
        try {
            // 尝试获取 RedLock
            // 参数: waitTime(等锁最大时长), leaseTime(锁租期), unit
            acquired = redLock.tryLock(5, 30, TimeUnit.SECONDS);
            if (acquired) {
                log.info("RedLock 获取成功，执行关键业务");
                // 执行关键业务
                return true;
            } else {
                log.warn("RedLock 获取失败（未达到多数节点）");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (acquired) {
                redLock.unlock(); // 将向所有节点发送解锁请求
            }
        }
    }
}
```

#### 2.10.4 RedLock 的争议与缺陷

| 问题 | 说明 | 影响程度 |
|------|------|---------|
| **时钟漂移（Clock Drift）** | 依赖服务器时间，如果节点时间不同步，算法失效 | 高 |
| **GC 停顿** | 客户端 Full GC 导致锁实际过期但未感知 | 高 |
| **性能开销** | 需要 N 次网络 IO，延迟是单点锁的 N 倍 | 中 |
| **复杂度** | 需要部署 5 个独立 Redis，运维复杂 | 高 |
| **异步复制无法解决** | RedLock 假设节点独立，但实际生产多是主从 | 中 |
| **Martin Kleppmann 的批评** | 指出 RedLock 的安全性与 ZooKeeper 相比不可靠 | 参考 |

> 🎯 **结论**：RedLock 提供了理论上的安全保证，但实际生产中大多数场景使用**单节点 + 哨兵**或**Redis Cluster** 即可满足需求。只有对一致性要求极高的场景（如金融交易）才考虑 RedLock 或替换为 ZooKeeper/etcd。

---

### 2.11 分布式锁方案对比

#### 2.11.1 Redis vs MySQL vs ZooKeeper

| 维度 | Redis（Redisson） | MySQL（`SELECT ... FOR UPDATE`） | ZooKeeper（临时ZNode + Watcher） |
|------|------------------|-------------------------------|--------------------------------|
| **原理** | 内存键值对 + Lua脚本 | 行锁 + 事务隔离 | 临时顺序节点 + 会话超时 |
| **性能** | ⭐⭐⭐⭐⭐ 微秒级 | ⭐⭐ 毫秒级（磁盘IO） | ⭐⭐⭐ 毫秒级 |
| **可靠性** | ⭐⭐⭐ 异步复制可能丢锁 | ⭐⭐⭐⭐ ACID保证 | ⭐⭐⭐⭐⭐ ZAB协议强一致 |
| **可重入** | ✅ 内置支持 | ❌ 需自行实现 | ❌ 需自行实现 |
| **死锁防护** | ✅ 过期时间 + Watch Dog | ❌ 无超时，事务回滚需要主动处理 | ✅ Session过期自动删除 |
| **公平排队** | ✅ FairLock | ❌ 数据库锁队列 | ✅ 顺序节点天然有序 |
| **锁超时** | ✅ 可配置 | ❌ 依赖 `lock_wait_timeout` | ✅ Session超时时间 |
| **Watch/通知** | ✅ Pub/Sub 锁通知 | ❌ 需轮询 | ✅ Watcher 通知 |
| **运维成本** | ⭐⭐⭐ 低（已有Redis） | ⭐⭐⭐⭐⭐ 无额外组件 | ⭐⭐⭐ 额外维护ZK集群 |
| **适用场景** | 高并发、可接受最终一致 | 强事务、已有MySQL | 强一致性、配置中心 |

#### 2.11.2 选型建议

| 系统级别 | 推荐方案 | 理由 |
|----------|----------|------|
| 互联网高并发 | Redis（Redisson） | 性能优先，允许最终一致 |
| 金融/支付 | ZooKeeper / etcd | 强一致性要求，安全优先 |
| 中小团队快速落地 | Redis（Redisson） | 无需额外中间件，开箱即用 |
| 已有ZK基础设施 | ZooKeeper | 避免引入新组件 |
| 单体应用/小规模 | MySQL 乐观锁 | 最简单，已有数据库 |

---

## 3. 高频踩坑与误区

### 3.1 锁误删问题

#### 3.1.1 问题复现

```java
// ❌ 错误代码 — 锁误删
public void wrongUnlock(String key) {
    String lockKey = "lock:" + key;
    // 设置锁，过期时间10秒
    Boolean acquired = stringRedisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
    if (Boolean.TRUE.equals(acquired)) {
        try {
            // 执行业务 .... 耗时超过10秒
            TimeUnit.SECONDS.sleep(15); // 业务执行15秒 → 锁已过期
            // 此时锁已被 Redis 自动删除
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // ❌ 此时删除的是其他线程的锁（线程B在这5秒内获取了锁）
            stringRedisTemplate.delete(lockKey);
        }
    }
}
```

**时间线**：

| 时间 | 线程A | 线程B |
|------|-------|-------|
| T0 | SETNX lock=1 成功 | — |
| T5 | 执行业务中 | — |
| T10 | 锁过期自动删除 | — |
| T12 | 仍执行业务中 | SETNX lock=1 成功 |
| T15 | 执行 finally delete | — |
| T16 | — | 执行业务中，锁被A删除 ❌ |

#### 3.1.2 解决方案 — UUID 标记 + Lua 原子删除

```java
// ✅ 正确代码 — 使用 UUID 标记锁的归属
public void correctUnlock(String key) {
    String lockKey = "lock:" + key;
    String requestId = UUID.randomUUID().toString(); // 唯一标识
    Boolean acquired = stringRedisTemplate.opsForValue()
            .setIfAbsent(lockKey, requestId, 10, TimeUnit.SECONDS);
    if (Boolean.TRUE.equals(acquired)) {
        try {
            // 执行业务
            TimeUnit.SECONDS.sleep(15);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 使用 Lua 脚本：原子检查 + 删除
            String luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
            stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), requestId);
        }
    }
}
```

> 💡 **Redisson 默认解决**：使用 Redisson 时，`RLock` 内部自动使用 UUID:threadId 作为线程标识，配合 Lua 脚本释放锁，天然避免锁误删。

### 3.2 未设置 leaseTime 导致的死锁

```java
// ❌ 危险 — lock() 不设超时，且未解锁
public void dangerousLock(String key) {
    RLock lock = redissonClient.getLock(key);
    lock.lock(); // 获取锁，Watch Dog 会持续续期
    // 业务异常 → 忘记 unlock() → 锁被永久持有
}
```

> ⚠️ **千万注意**：`lock()` 和 `unlock()` 必须成对使用，`unlock()` 必须在 `finally` 块中调用，否则 Watch Dog 会一直续期，导致其他线程永远无法获取锁。

### 3.3 Redisson 与 RedisTemplate 冲突

```java
// ❌ 问题：引入 redisson-spring-boot-starter 后，RedisTemplate 自动配置失效
// 解决方案：手动配置 RedisTemplate Bean
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
    return template;
}
```

### 3.4 锁粒度问题

| 误区 | 说明 | 改进 |
|------|------|------|
| 锁粒度过大 | `getLock("lock:all")` 所有订单用同一把锁 | 按资源id分片 `lock:order:{orderId}` |
| 锁粒度过小 | `getLock("lock:product:sku:" + skuId)` 每个SKU单独锁 | 根据业务决定，注意死锁风险 |
| 未统一前缀 | 不同业务使用相同key格式可能冲突 | 使用业务前缀 `biz:resource:id` |

### 3.5 RedLock 时钟依赖问题

> ⚠️ RedLock 的多数节点判定依赖时间精度。如果某个 Redis 节点的系统时钟发生跳跃（如 NTP 同步），可能导致锁被错误释放或重复获取。生产环境使用 RedLock 时要确保所有节点的时间同步（启用 NTP 服务）。

---

## 4. 随堂基础练习

### 4.1 选择题

**1. 关于分布式锁的四大需求，以下哪个描述是错误的？**
- A. 互斥性：任意时刻只能有一个客户端持有锁
- B. 防死锁：客户端崩溃后锁能自动释放
- C. 可重入性：不同线程可以获取同一把锁
- D. 容错性：部分节点宕机不影响锁服务

<details>
<summary>答案</summary>
C。可重入是指**同一线程**可以多次获取同一把锁，不同线程不能。
</details>

---

**2. Redisson Watch Dog 默认的续期间隔是？**
- A. 30 秒
- B. 10 秒
- C. 3 秒
- D. 1 秒

<details>
<summary>答案</summary>
B。Watch Dog 默认锁租期 30 秒，每 30/3 = 10 秒续一次。
</details>

---

**3. 以下哪个不是 Redis 主从架构下锁丢失的原因？**
- A. Master 异步复制到 Slave 之前崩溃
- B. Slave 晋升为 Master 后无锁数据
- C. 网络分区导致客户端与 Master 断开
- D. 客户端执行时间过长触发 Watch Dog

<details>
<summary>答案</summary>
D。Watch Dog 会延长期限，不会导致锁丢失。
</details>

---

### 4.2 填空题

**4. Redisson 使用 _____ 数据结构来实现可重入锁，而不是简单的 String 键值对。**

<details>
<summary>答案</summary>
Hash（哈希表），field 为线程标识，value 为重入计数。
</details>

---

**5. RedLock 算法向 5 个独立节点加锁，成功条件是至少向 _____ 个节点加锁成功。**

<details>
<summary>答案</summary>
3（N/2 + 1 = 5/2 + 1 = 3）
</details>

---

### 4.3 简答题

**6. 简述使用原生 SETNX 实现分布式锁时，锁误删问题是如何产生的？如何解决？**

<details>
<summary>答案</summary>
线程A加锁后业务执行时间超过锁过期时间，锁被Redis自动释放；线程B获取锁开始执行；线程A finally块中执行delete操作，误删了线程B的锁。

解决：使用UUID作为锁的value标识线程身份，释放锁时使用Lua脚本原子检查value是否匹配再执行delete。
</details>

---

## 5. 章节综合实操案例

### 案例：Redisson + SpringBoot 订单防重提交

#### 5.1 业务背景

在电商系统中，用户由于网络抖动或前端重复点击，可能对同一个订单发起多次提交请求。需要保证：

1. 同一笔订单只创建一次（幂等性）
2. 高并发下订单处理不超过 100 QPS
3. 防止重复请求导致的库存多扣

#### 5.2 完整代码实现

#### 5.2.1 实体类

```java
package com.example.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private String orderNo;       // 订单号（业务唯一键）
    private Long userId;          // 用户ID
    private Long productId;       // 商品ID
    private Integer quantity;     // 数量
    private BigDecimal amount;    // 总金额
    private Integer status;       // 0-待支付 1-已支付 2-已取消
    private LocalDateTime createTime;
}
```

#### 5.2.2 DTO

```java
package com.example.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "数量不能小于1")
    private Integer quantity;

    // 前端生成的请求唯一标识（保证幂等）
    private String idempotentToken;
}
```

```java
package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String orderNo;
    private Boolean success;
    private String message;
}
```

#### 5.2.3 OrderService — 核心业务

```java
package com.example.order.service;

import com.example.order.dto.OrderCreateRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final RedissonClient redissonClient;

    // 模拟订单存储
    private static final java.util.Map<String, Order> ORDER_STORE = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 创建订单 — 基于分布式锁的防重机制
     *
     * 设计思路：
     * 1. 锁粒度：按用户+商品组合加锁，避免全局限锁
     * 2. 幂等性：通过 idempotentToken 判断是否已处理
     * 3. 限流：使用 tryLock 限制并发量
     */
    public OrderResponse createOrder(OrderCreateRequest request) {
        // 生成唯一标识作为锁key（用户ID + 商品ID）
        String lockKey = "lock:order:create:" + request.getUserId() + ":" + request.getProductId();
        RLock lock = redissonClient.getLock(lockKey);

        // 尝试获取锁 — 最多等待1秒，获取后leaseTime=5秒（Watch Dog不启动）
        boolean acquired = false;
        try {
            acquired = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("订单创建太频繁，请稍后重试 | userId={} productId={}",
                        request.getUserId(), request.getProductId());
                return OrderResponse.builder()
                        .success(false)
                        .message("系统繁忙，请稍后重试")
                        .build();
            }

            // ===== 业务幂等校验 =====
            String idempotentKey = "order:idempotent:" + request.getIdempotentToken();
            if (Boolean.TRUE.equals(
                    redissonClient.getBucket(idempotentKey).isExists())) {
                log.info("重复请求已拦截 | token={}", request.getIdempotentToken());
                return OrderResponse.builder()
                        .success(false)
                        .message("该请求已处理，请勿重复提交")
                        .build();
            }

            // ===== 核心业务逻辑 =====
            // 1. 生成订单号
            String orderNo = generateOrderNo(request.getUserId());

            // 2. 构建订单
            Order order = Order.builder()
                    .orderNo(orderNo)
                    .userId(request.getUserId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .amount(calculateAmount(request))
                    .status(0) // 待支付
                    .createTime(LocalDateTime.now())
                    .build();

            // 3. 保存订单（模拟持久化）
            ORDER_STORE.put(orderNo, order);

            // 4. 扣减库存（模拟）
            deductStock(request.getProductId(), request.getQuantity());

            // 5. 标记幂等 — 过期时间24小时
            redissonClient.getBucket(idempotentKey)
                    .set("processed", 24, TimeUnit.HOURS);

            log.info("订单创建成功 | orderNo={} userId={}", orderNo, request.getUserId());
            return OrderResponse.builder()
                    .orderId(1L)
                    .orderNo(orderNo)
                    .success(true)
                    .message("订单创建成功")
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("订单创建被中断 | userId={}", request.getUserId(), e);
            return OrderResponse.builder()
                    .success(false)
                    .message("创建订单异常")
                    .build();
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }

    /**
     * 模拟库存扣减
     */
    private void deductStock(Long productId, Integer quantity) {
        // 实际项目中调用库存服务
        log.info("扣减库存 | productId={} quantity={}", productId, quantity);
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo(Long userId) {
        return "ORD" + System.currentTimeMillis() + userId;
    }

    /**
     * 计算订单金额（模拟）
     */
    private BigDecimal calculateAmount(OrderCreateRequest request) {
        // 实际项目中查询商品价格计算
        return BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(request.getQuantity()));
    }
}
```

#### 5.2.4 OrderController

```java
package com.example.order.controller;

import com.example.order.dto.OrderCreateRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单接口
     * POST /api/orders/create
     */
    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.createOrder(request);
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(429).body(response); // 429 Too Many Requests
    }

    /**
     * 健康检查
     * GET /api/orders/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```

#### 5.2.5 并发压测模拟

```java
package com.example.order;

import com.example.order.dto.OrderCreateRequest;
import com.example.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟100个并发请求同时创建订单
 * 验证分布式锁是否能保证：
 * 1. 同一用户+同一商品只成功创建一次
 * 2. 锁互斥正常，不出现并发问题
 */
@Slf4j
@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Test
    void testConcurrentOrderCreation() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    OrderCreateRequest request = new OrderCreateRequest();
                    request.setUserId(1001L);       // 同一个用户
                    request.setProductId(2001L);     // 同一个商品
                    request.setQuantity(1);
                    request.setIdempotentToken("token-" + index);

                    var response = orderService.createOrder(request);
                    if (response.getSuccess()) {
                        successCount.incrementAndGet();
                        log.info("线程{} 订单创建成功: {}", index, response.getOrderNo());
                    } else {
                        failCount.incrementAndGet();
                        log.warn("线程{} 订单创建失败: {}", index, response.getMessage());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("线程{} 异常", index, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 等待所有线程完成
        long cost = System.currentTimeMillis() - startTime;

        log.info("========== 并发压测结果 ==========");
        log.info("总请求数: {}", threadCount);
        log.info("成功数: {}", successCount.get());
        log.info("失败数: {}", failCount.get());
        log.info("总耗时: {} ms", cost);
        log.info("================================");
    }
}
```

#### 5.2.6 压测预期结果

```
========== 并发压测结果 ==========
总请求数: 100
成功数: 1          ← 同一用户+同一商品，互斥锁保证只有一笔订单成功
失败数: 99         ← 99个请求被锁拦截或幂等拦截
总耗时: 325 ms     ← 快速响应，不会排队等待
================================
```

> 🎯 **关键设计要点**：
> 1. **锁粒度**：`lock:order:create:{userId}:{productId}` 按用户+商品分片，避免全局限锁
> 2. **幂等标记**：使用 `idempotentToken` 标记已处理的请求，过期时间 24 小时
> 3. **tryLock 策略**：等待 1 秒获取锁，获取后 5 秒自动释放（不启动 Watch Dog），适用于执行时间可控的订单场景
> 4. **leaseTime 选择**：这里显式设置了 5 秒，而不是使用 Watch Dog，因为订单创建操作通常很快完成

---

## 6. 分层综合习题

### 6.1 基础层

**1. 阅读以下代码，找出至少 3 个问题：**

```java
public void processOrder(int orderId) {
    RLock lock = redissonClient.getLock("order_lock");
    lock.lock();
    // 处理订单
    processPayment(orderId);
    // 发送通知
    sendNotification(orderId);
    lock.unlock();
}
```

<details>
<summary>答案</summary>
1. 锁未在 finally 中释放，业务异常时锁永久持有。
2. 锁粒度过大，使用固定字符串 "order_lock"，所有订单共用一把锁。
3. 缺少 tryLock 超时机制，获取不到锁时线程阻塞等待。
</details>

---

**2. 如果要实现一个分布式定时任务，要求每天 10:00 只允许一个节点执行，应该使用 Redisson 的哪种锁？**

<details>
<summary>答案</summary>
使用 RLock（互斥锁），加锁 key 为 `lock:scheduler:daily:2026-07-26`（按日期分片）。获取锁成功的节点执行任务，未获取到的直接跳过。
</details>

---

### 6.2 进阶层

**3. 以下代码在高并发下有什么问题？如何改进？**

```java
@Service
public class InventoryService {
    public void deductStock(Long productId, int quantity) {
        // 查库存
        int stock = getStock(productId);
        if (stock >= quantity) {
            // 扣库存
            updateStock(productId, stock - quantity);
        }
    }
}
```

<details>
<summary>答案</summary>
问题：存在竞态条件，查库存和扣库存不是原子操作，高并发下导致超卖。

改进方案：
1. 使用 Redisson 分布式锁保护整个操作。
2. 或者使用 Redis Lua 脚本原子执行。
3. 或者使用数据库乐观锁 `update stock set count = count - #{qty} where id = #{id} and count >= #{qty}`。
</details>

---

**4. 一个操作需要同时锁定资源 A 和资源 B，如何用 Redisson 避免死锁？**

<details>
<summary>答案</summary>
1. 始终按固定顺序加锁（如先锁A再锁B），避免循环等待。
2. 使用 `tryLock` 获取锁，获取失败时释放已持有的锁并重试。
3. 使用 Redisson 的 `RLock` 的 `lockInterruptibly` 支持中断。

最佳实践：使用一次性获取多把锁的 `RedissonMultiLock`。
</details>

---

### 6.3 精通层

**5. Redis 主从架构下，客户端A获取锁后 Master 宕机，Slave 晋升为 Master 但无锁数据，此时客户端B也获取同一把锁成功。请分析这个问题的根因，并提出至少两种解决方案。**

<details>
<summary>答案</summary>
**根因**：Redis 主从使用异步复制，Master 向 Slave 同步锁数据之前宕机，锁丢失。

**方案一**：使用 RedLock 算法，在多个独立节点加锁，多数节点成功才算获取锁。
**方案二**：使用 ZooKeeper（强一致）替代 Redis。ZK 的 ZAB 协议保证写操作在多数节点持久化后才返回。
**方案三**：使用 Redis 的 WAIT 命令（同步复制），配置 `WAIT 1 1000` 等待至少一个 Slave 写入，但会降低性能。
**方案四**（折中）：接受微小的丢锁概率，通过业务层幂等校验来保证最终一致性。
</details>

---

**6. 以下 Lua 脚本有什么问题？**

```lua
-- 解锁脚本
if redis.call('get', KEYS[1]) == ARGV[1] then
    redis.call('del', KEYS[1])
    return 1
end
return 0
```

<details>
<summary>答案</summary>
该 Lua 脚本本身没有问题，但不是 Redisson 的实现。Redisson 的解锁使用 Hash 结构（重入锁）而非 String 结构：
- 使用 `hexists` 检查线程标识
- 使用 `hincrby` 递减重入计数
- 计数归零才删除锁
- 增加发布通知机制

上述 String 版本的解锁脚本不支持**可重入**特性。
</details>

---

## 7. 本章复盘速记清单

### 7.1 核心概念速查

| 概念 | 一句话总结 | 面试关键词 |
|------|-----------|-----------|
| 分布式锁 | 跨 JVM 的互斥控制机制 | 互斥 / 防死锁 / 容错 / 可重入 |
| SETNX 缺陷 | 无重入、无续期、有锁误删风险 | UUID + Lua |
| Watch Dog | 锁到期前自动续期的后台定时任务 | 30s 租期 / 10s 间隔 |
| Lua 原子性 | 将多条 Redis 命令打包成一条原子操作 | hexists / hincrby / del |
| 锁误删 | A的锁过期后B获取锁，A删除了B的锁 | UUID 标记 + Lua 判等删除 |
| RedLock | 多数独立节点加锁，解决异步复制丢锁 | 5节点 / 3成功 / 时钟依赖 |
| RLock | 可重入分布式独占锁 | lock / tryLock / unlock |
| RReadWriteLock | 读读并发、读写/写写互斥 | 读多写少场景 |
| RSemaphore | 分布式资源计数限流 | acquire / release |
| RCountDownLatch | 分布式倒计时协调器 | await / countDown |

### 7.2 易错点 TOP 5

| # | 易错点 | 正确做法 |
|---|--------|---------|
| 1 | `lock()` 后不在 `finally` 中 `unlock()` | 任何时候 `lock()` 必须配 `try-finally-unlock()` |
| 2 | 锁的 key 粒度过粗（全局一把锁） | 按资源 ID 分片，如 `lock:biz:{resourceId}` |
| 3 | 使用 `@Transactional` 时锁在事务外 | 锁的释放要在事务提交之后 |
| 4 | 混淆 Watch Dog 与 leaseTime 的关系 | 指定 leaseTime 时不启动 Watch Dog |
| 5 | 认为 RedLock 万无一失 | RedLock 也有时钟漂移和 GC 停顿问题 |

### 7.3 面试高频问题

| 问题 | 核心要点 |
|------|---------|
| 请说出分布式锁的三要素 | 互斥性、防死锁、容错性（+可重入为四要素） |
| Redisson Watch Dog 原理 | 定时任务每 10s 续期到 30s，仅在不指定 leaseTime 时生效 |
| 为什么用 Lua 脚本实现锁 | 原子执行不需要事务，减少网络 IO，保证检查+删除的原子性 |
| Redis 主从丢锁怎么解决 | RedLock（多数节点）或更换 ZooKeeper |
| 分布式锁对比 | Redis 性能高 / ZK 一致性强 / MySQL 事务保证 |
| 如何设计一个分布式锁 | Redis Hash + UUID 标识 + Lua 脚本 + Watch Dog |

---

## 8. 精通拓展补充-P2

### 8.1 Redisson 高级配置

#### 8.1.1 集群模式配置

```java
@Bean
public RedissonClient redissonClusterClient() {
    Config config = new Config();
    config.useClusterServers()
            .addNodeAddress(
                "redis://192.168.1.10:6379",
                "redis://192.168.1.11:6379",
                "redis://192.168.1.12:6379"
            )
            .setScanInterval(2000) // 集群状态扫描间隔（毫秒）
            .setSlaveConnectionPoolSize(32)
            .setMasterConnectionPoolSize(64)
            .setRetryAttempts(3)
            .setTimeout(3000)
            .setPassword("redis-pass");
    return Redisson.create(config);
}
```

#### 8.1.2 哨兵模式配置

```java
@Bean
public RedissonClient redissonSentinelClient() {
    Config config = new Config();
    config.useSentinelServers()
            .addSentinelAddress(
                "redis://sentinel1:26379",
                "redis://sentinel2:26379",
                "redis://sentinel3:26379"
            )
            .setMasterName("mymaster")
            .setDatabase(0)
            .setMasterConnectionPoolSize(32)
            .setSlaveConnectionPoolSize(16);
    return Redisson.create(config);
}
```

### 8.2 Redisson 分布式对象扩展

#### 8.2.1 RAtomicLong — 分布式原子计数器

```java
@Service
@RequiredArgsConstructor
public class DistributedCounterService {

    private final RedissonClient redissonClient;

    public long incrementAndGet(String counterName) {
        RAtomicLong counter = redissonClient.getAtomicLong("counter:" + counterName);
        return counter.incrementAndGet();
    }

    public long getCurrentValue(String counterName) {
        RAtomicLong counter = redissonClient.getAtomicLong("counter:" + counterName);
        return counter.get();
    }
}
```

#### 8.2.2 RRateLimiter — 分布式速率限制器

```java
@Service
@RequiredArgsConstructor
public class ApiRateLimitService {

    private final RedissonClient redissonClient;

    @PostConstruct
    public void initRateLimiter() {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("ratelimiter:api:order");
        // 设置速率：每1秒产生10个令牌
        rateLimiter.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.SECONDS);
    }

    public boolean tryAccess() {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("ratelimiter:api:order");
        return rateLimiter.tryAcquire(); // 获取一个令牌
    }
}
```

### 8.3 分布式锁性能优化

| 策略 | 说明 | 效果 |
|------|------|------|
| **锁粒度最小化** | 按业务资源拆分 key | 减少锁竞争 |
| **使用 tryLock 替代 lock** | 避免线程无限阻塞 | 提升系统可用性 |
| **显式设置 leaseTime** | 业务耗时已知时不启动 Watch Dog | 减少 Watch Dog 开销 |
| **本地锁 + 分布式锁分层** | 本地缓存+Caffeine+Redisson读写锁 | 减少 Redis 访问 |
| **异步加锁** | Redisson 支持 Future/Reactive 加锁 | 非阻塞高性能 |

#### 8.3.1 本地锁 + 分布式锁双层缓存

```java
/**
 * 双层缓存：本地锁减少Redis压力，分布式锁保证跨进程一致性
 */
public Object getCachedData(String key) {
    // 1. 尝试本地缓存
    Object local = localCache.get(key);
    if (local != null) {
        return local;
    }

    // 2. 分布式读锁
    RReadWriteLock rwLock = redissonClient.getReadWriteLock("cache:" + key);
    rwLock.readLock().lock();
    try {
        // 双重检查
        Object cache = localCache.get(key);
        if (cache != null) return cache;
        return loadFromDB(key);
    } finally {
        rwLock.readLock().unlock();
    }
}

public void updateCache(String key, Object data) {
    RReadWriteLock rwLock = redissonClient.getReadWriteLock("cache:" + key);
    rwLock.writeLock().lock();
    try {
        // 更新数据库
        updateDB(key, data);
        // 更新本地缓存
        localCache.put(key, data);
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

### 8.4 Spring Boot 与 Redisson 最佳实践

#### 8.4.1 锁服务统一封装

```java
/**
 * 分布式锁统一抽象 — 提供声明式锁服务
 * 避免每个 Service 都直接操作 RedissonClient
 */
@Service
public class DistributedLockService {

    private final RedissonClient redissonClient;

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 带锁执行 — 默认模式
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                 TimeUnit unit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (!acquired) {
                throw new RuntimeException("获取锁失败: " + lockKey);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("锁获取被中断", e);
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }

    /**
     * 带锁执行 — Watch Dog 模式（leaseTime = -1）
     */
    public <T> T executeWithLockWatchDog(String lockKey, long waitTime,
                                         Supplier<T> supplier) {
        return executeWithLock(lockKey, waitTime, -1, TimeUnit.SECONDS, supplier);
    }

    /**
     * 带锁执行 — 无返回值
     */
    public void executeWithLock(String lockKey, long waitTime, long leaseTime,
                                TimeUnit unit, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, unit, () -> {
            runnable.run();
            return null;
        });
    }
}
```

#### 8.4.2 @Lock 注解（自定义 AOP）

```java
/**
 * 自定义分布式锁注解 — 声明式加锁
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Lock {
    String key();                  // 锁的key（支持SpEL表达式）
    long waitTime() default 3;    // 等待时间
    long leaseTime() default -1;  // 租期时间（-1=Watch Dog）
    TimeUnit unit() default TimeUnit.SECONDS;
}
```

```java
/**
 * 锁注解切面
 */
@Aspect
@Component
public class LockAspect {

    private final DistributedLockService lockService;

    @Around("@annotation(lockAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Lock lockAnnotation) throws Throwable {
        // 解析 SpEL 表达式获取锁 key
        String lockKey = resolveKey(lockAnnotation.key(), joinPoint);

        return lockService.executeWithLock(
                lockKey,
                lockAnnotation.waitTime(),
                lockAnnotation.leaseTime(),
                lockAnnotation.unit(),
                () -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private String resolveKey(String expr, ProceedingJoinPoint joinPoint) {
        // 实现 SpEL 表达式解析（略）
        return expr;
    }
}
```

#### 8.4.3 使用示例

```java
@Service
public class OrderService {

    @Lock(key = "'lock:order:create:' + #request.userId + ':' + #request.productId",
          waitTime = 2, leaseTime = 5)
    public Order createOrder(OrderCreateRequest request) {
        // 方法体直接执行业务，锁由切面管理
        return doCreateOrder(request);
    }
}
```

### 8.5 分布式锁的设计模式总结

| 模式 | 锁类型 | 使用场景 |
|------|--------|---------|
| 互斥执行 | RLock | 定时任务、库存扣减、资源独占 |
| 读写分离 | RReadWriteLock | 配置中心、缓存重建 |
| 并发限流 | RSemaphore / RRateLimiter | API限流、资源池 |
| 任务协调 | RCountDownLatch | 批量处理、多阶段流水线 |
| 多数决策 | RedissonRedLock | 金融级强一致场景 |
| 公平排队 | RLock 配合 FairLock | 票务系统、订单排队 |

---

> 🎯 **总结**：Redisson 分布式锁是 Redis 生态中最为成熟的分布式锁解决方案，它通过 RLock/RReadWriteLock/RSemaphore/RCountDownLatch 四种核心同步器覆盖了分布式环境下绝大多数的并发控制需求。掌握 Watch Dog 续期机制、Lua 脚本原子性、锁误删防范三大核心技术，配合合理的锁粒度设计，即可在生产环境中构建高可用的分布式锁体系。对于金融级强一致场景，仍需综合评估 RedLock 的局限性，必要时引入 ZooKeeper 或 etcd。
