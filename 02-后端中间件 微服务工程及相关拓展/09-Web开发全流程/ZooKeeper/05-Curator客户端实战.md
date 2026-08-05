# 05 - Curator 客户端实战

> 🎯 Apache Curator 是 ZooKeeper 的"最佳拍档" — 封装了连接重试、Watch 反复注册、分布式锁等，生产环境永远用 Curator 而不是原生 ZK API

---

## 目录

1. [Curator 概述](#1-curator-概述)
2. [连接管理](#2-连接管理)
3. [CRUD 操作](#3-crud-操作)
4. [异步操作](#4-异步操作)

---

## 1. Curator 概述

| 特性 | 说明 |
|------|------|
| **连接重试** | 指数退避、重试策略 |
| **Watch 自动恢复** | NodeCache / TreeCache |
| **分布式锁** | `InterProcessMutex` / `InterProcessReadWriteLock` |
| **Leader 选举** | `LeaderSelector` |
| **Barrier / Counter** | `DistributedBarrier` / `DistributedAtomicLong` |

```xml
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
    <version>5.5.0</version>
</dependency>
```

---

## 2. 连接管理

```java
// ⭐ 生产级连接配置
CuratorFramework client = CuratorFrameworkFactory.builder()
    .connectString("zk1:2181,zk2:2181,zk3:2181")
    .sessionTimeoutMs(60000)           // Session 超时 60s
    .connectionTimeoutMs(15000)        // 连接超时 15s
    .retryPolicy(new ExponentialBackoffRetry(1000, 3))  // 重试：1s → 2s → 4s
    .namespace("myapp")                // ⭐ 命名空间隔离（所有路径自动加 /myapp 前缀）
    .build();

client.start();
client.blockUntilConnected();          // 阻塞直到连接成功
```

| 重试策略 | 说明 |
|----------|------|
| `ExponentialBackoffRetry` | 指数退避：baseSleepMs × 2^retryCount |
| `RetryNTimes` | 重试 N 次 |
| `RetryOneTime` | 重试一次 |
| `RetryUntilElapsed` | 指定时间内无限重试 |

---

## 3. CRUD 操作

```java
// ═══ 创建 ═══
client.create()
    .creatingParentsIfNeeded()        // 自动创建父节点
    .withMode(CreateMode.PERSISTENT)
    .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
    .forPath("/config/db-url", "jdbc:mysql://...".getBytes());

// 临时顺序节点
client.create()
    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
    .forPath("/locks/order-");

// ═══ 读取 ═══
byte[] data = client.getData().forPath("/config/db-url");
Stat stat = client.checkExists().forPath("/config");

// ═══ 更新 ═══
client.setData()
    .withVersion(stat.getVersion())    // ⭐ 乐观锁（CAS）
    .forPath("/config/db-url", newValue.getBytes());

// ═══ 删除 ═══
client.delete()
    .guaranteed()                      // 保证删除（失败后重试直到成功）
    .deletingChildrenIfNeeded()        // 递归删除
    .withVersion(stat.getVersion())    // 乐观锁
    .forPath("/config");

// ═══ 事务 ═══
client.inTransaction()
    .create().forPath("/a", dataA)
    .and()
    .setData().forPath("/b", dataB)
    .and()
    .commit();
```

---

## 4. 异步操作

```java
// ═══ 方式1：BackgroundCallback ═══
client.getData()
    .inBackground((c, event) -> {
        if (event.getResultCode() == KeeperException.Code.OK.intValue()) {
            System.out.println("数据: " + new String(event.getData()));
        }
    })
    .forPath("/config");

// ═══ 方式2：线程池指定 ═══
ExecutorService executor = Executors.newFixedThreadPool(4);
client.setData()
    .inBackground((c, event) -> { /* callback */ }, executor)
    .forPath("/config", data);

// ═══ 方式3：CompletableFuture (Java 8+) ═══
AsyncCuratorFramework async = AsyncCuratorFramework.wrap(client);
async.getData().forPath("/config")
    .thenAccept(data -> System.out.println("数据: " + new String(data)))
    .exceptionally(e -> { e.printStackTrace(); return null; });
```

> 🎯 **Curator 三件套**：`namespace` 隔离应用、`creatingParentsIfNeeded` 省去创建父节点、`guaranteed` 保证删除成功。生产环境不要用原生 ZK API，Curator 是唯一选择。
