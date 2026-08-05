# 06 - 分布式锁与 Leader 选举

> 🎯 这是 ZooKeeper 最核心的实战场景 — 互斥锁/读写锁/信号量、Leader 选举/计数器/屏障，Curator 封装了所有分布式协调原语

---

## 目录

1. [分布式锁](#1-分布式锁)
2. [羊群效应与优化](#2-羊群效应与优化)
3. [Leader 选举](#3-leader-选举)
4. [其他协调原语](#4-其他协调原语)

---

## 1. 分布式锁

### 1.1 互斥锁（InterProcessMutex）

```java
InterProcessMutex lock = new InterProcessMutex(client, "/locks/order");

if (lock.acquire(10, TimeUnit.SECONDS)) {
    try {
        // 临界区 — 只有一个线程能执行
        createOrder();
    } finally {
        lock.release();
    }
}
```

```text
实现原理（临时顺序节点 + Watch 前驱）：

Step 1: 所有线程在 /locks/order 下创建临时顺序节点
        → /locks/order/0000000001 (Thread A)
        → /locks/order/0000000002 (Thread B)
        → /locks/order/0000000003 (Thread C)

Step 2: Thread A（序号最小）→ 获得锁
        Thread B → Watch 0000000001（前驱）
        Thread C → Watch 0000000002（前驱）

Step 3: Thread A 释放（Session 断开 / release()）
        → 0000000001 自动删除
        → B 收到通知 → 发现自己是最小 → 获得锁！
```

### 1.2 读写锁（InterProcessReadWriteLock）

```java
InterProcessReadWriteLock rwLock = new InterProcessReadWriteLock(client, "/locks/data");
InterProcessMutex readLock = rwLock.readLock();
InterProcessMutex writeLock = rwLock.writeLock();

readLock.acquire();     // 多个线程可同时持有读锁
writeLock.acquire();    // 独占写锁
```

### 1.3 信号量（InterProcessSemaphoreV2）

```java
// 最多允许 5 个并发
InterProcessSemaphoreV2 semaphore = new InterProcessSemaphoreV2(client, "/semaphore/export", 5);
Lease lease = semaphore.acquire();   // 获取一个租约（许可证）
try {
    exportData();
} finally {
    semaphore.returnLease(lease);
}
```

---

## 2. 羊群效应与优化

```text
❌ 羊群效应（错误做法）：
  所有客户端 Watch 同一个 /lock 节点
  → /lock 变化 → 所有客户端同时被唤醒
  → 竞争激烈、大量无用唤醒

✅ 临时顺序节点 + Watch 前驱（Curator 做法）：
  每个客户端 Watch 自己的前驱节点
  → 只有前驱释放时，自己被唤醒
  → 精确唤醒，零竞争
```

```text
对比：
  方式           唤醒数量         竞争
  Watch 同一节点    N               N-1（失败）
  Watch 前驱       1（精确）        1（成功）
```

---

## 3. Leader 选举

```java
// ⭐ LeaderSelector — Curator 封装
LeaderSelectorListener listener = new LeaderSelectorListenerAdapter() {
    @Override
    public void takeLeadership(CuratorFramework client) throws Exception {
        // 当选 Leader → 执行业务逻辑
        System.out.println("我是 Leader！");
        while (!Thread.currentThread().isInterrupted()) {
            // Leader 持续工作
            Thread.sleep(1000);
        }
    }
};

LeaderSelector selector = new LeaderSelector(client, "/election/scheduler", listener);
selector.autoRequeue();    // 自动重新参与选举（失去 Leader 后重试）
selector.start();
// 只有当选 Leader 的实例才执行 takeLeadership() 中的逻辑
```

```text
Leader 选举原理：
  Step 1: 所有节点在 /election/scheduler 下创建临时顺序节点
  Step 2: 序号最小的节点当选 Leader
  Step 3: Leader 宕机 → Session 断开 → 临时节点自动删除
  Step 4: 序号第二小的节点收到 Watch 通知 → 发现自己序号最小 → 成为新 Leader
```

| Curator 类 | 用途 |
|-----------|------|
| `LeaderSelector` | Leader 选举（互斥执行） |
| `LeaderLatch` | Leader 闩（更简单的选举，适合一次性） |

---

## 4. 其他协调原语

| 原语 | 类 | 用途 |
|------|-----|------|
| **屏障** | `DistributedBarrier` | N 个节点全部到达后才继续 |
| **双屏障** | `DistributedDoubleBarrier` | 进入+离开都需全部节点 |
| **计数器** | `DistributedAtomicLong` | 分布式原子计数器 |
| **队列** | `DistributedQueue` | 分布式队列 |

```java
// 分布式屏障：等待 3 个节点全部就绪
DistributedBarrier barrier = new DistributedBarrier(client, "/barrier/init");
barrier.setBarrier();          // 设置屏障
barrier.waitOnBarrier();       // 阻塞，等待所有节点到达
// 3 个节点都调了 removeBarrier() 后 → 所有节点同时继续

// 分布式原子计数器
DistributedAtomicLong counter = new DistributedAtomicLong(client, "/counter/order",
    new RetryNTimes(3, 100));
AtomicValue<Long> result = counter.increment();
if (result.succeeded()) {
    System.out.println("订单号: " + result.postValue());
}
```

> 🎯 **核心思想**：临时顺序节点 + Watch 前驱 = 分布式锁 + Leader 选举。Session 断连自动释放 = 天然的故障容错。这组原语是 ZooKeeper 分布式协调的全部精华。
