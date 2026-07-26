# 04 - ZooKeeper Session 与 Watcher 机制

> 🎯 Session 是 ZK 客户端与服务器的"生命线"，Watch 是事件驱动的"神经系统" — 理解两者的生命周期和交互方式，才能真正驾驭 ZooKeeper

---

## 目录

1. [Session 机制详解](#1-session-机制详解)
2. [Session 生命周期](#2-session-生命周期)
3. [Watcher 机制详解](#3-watcher-机制详解)
4. [Watch 的坑与最佳实践](#4-watch-的坑与最佳实践)

---

## 1. Session 机制详解

### 1.1 Session 的四个状态

```text
  [NOT_CONNECTED] → [CONNECTING] → [CONNECTED] → [CLOSED]
                         │               │
                         └── 超时 ────────┘→ [EXPIRED]

状态转换：
  → 创建 ZK 连接 → CONNECTING
  → 连接成功，Server 分配 sessionId → CONNECTED
  → 心跳正常 → 保持 CONNECTED
  → 心跳超时 → EXPIRED（Session 不可恢复）
  → 主动关闭 → CLOSED
```

| 状态 | 说明 | 行为 |
|------|------|------|
| `CONNECTING` | 正在连接 | — |
| `CONNECTED` | 连接成功 | Watch 生效 |
| `EXPIRED` | Session 过期 | **所有临时节点被删除！所有 Watch 失效！** |
| `CLOSED` | 主动关闭 | 需要重建连接 |

### 1.2 Session 参数

```properties
# zoo.cfg — 服务端
tickTime=2000                      # 基本时间单位
minSessionTimeout=4000             # 最小超时 (2 × tickTime)
maxSessionTimeout=40000            # 最大超时 (20 × tickTime)

# 客户端指定超时（必须在 min-max 之间）
ZooKeeper zk = new ZooKeeper("127.0.0.1:2181", 10000, watcher);
```

```
Session 超时 = tickTime × N

服务端判定：超时时间内未收到客户端心跳 → Session 过期
  → 删除该 Session 的所有临时节点
  → 通知所有 Watch 这些节点的客户端
```

---

## 2. Session 生命周期

```java
// ⭐ Session 断开重连 vs 过期重建
CountDownLatch latch = new CountDownLatch(1);
ZooKeeper zk = new ZooKeeper("127.0.0.1:2181", 10000, event -> {
    switch (event.getState()) {
        case SyncConnected:
            System.out.println("连接成功, sessionId=" + zk.getSessionId());
            latch.countDown();
            break;
        case Disconnected:
            System.out.println("断开连接（网络抖动）— 等待重连...");
            // 临时节点还在！因为 Session 未过期
            break;
        case Expired:
            System.out.println("Session 过期！临时节点已丢失 — 需要重建");
            // ⚠️ 必须重建 ZooKeeper 连接，重新创建临时节点！
            zk.close();
            zk = new ZooKeeper("127.0.0.1:2181", 10000, this);
            break;
    }
});
```

| 事件 | 临时节点 | Connection Watch | 客户端操作 |
|------|:---:|:---:|------|
| 网络短暂断开 | ✅ 还在 | — | 等待自动重连 |
| Session 过期 | ❌ 被删除 | ✅ 触发 | 关闭老连接，**重建新连接** |

---

## 3. Watcher 机制详解

> ⚠️ **Watch 是一次性的！** 触发后必须重新注册。

### 3.1 三种注册方式

```java
// 方式1：构造函数全局 Watcher（初始连接、Session 事件）
ZooKeeper zk = new ZooKeeper("127.0.0.1:2181", 10000, event -> {
    if (event.getType() == Event.EventType.None) {
        System.out.println("Session 事件: " + event.getState());
    }
});

// 方式2：getData / exists (Watch 数据)
zk.getData("/config", event -> {
    System.out.println("/config 数据变更");
    // ⚠️ 需要重新注册！
    zk.getData("/config", this, stat);
}, stat);

// 方式3：getChildren (Watch 子节点列表)
zk.getChildren("/services", event -> {
    System.out.println("服务列表变更: " + event.getType());
    // 重新注册
    zk.getChildren("/services", this);
});
```

### 3.2 Watch 触发条件

| 操作 | NodeCreated | NodeDeleted | NodeDataChanged | NodeChildrenChanged |
|------|:---:|:---:|:---:|:---:|
| `create` | ✅ | ❌ | ❌ | 父节点 ✅ |
| `delete` | ❌ | ✅ | ❌ | 父节点 ✅ |
| `setData` | ❌ | ❌ | ✅ | ❌ |
| `getData` 注册 | ✅ | ✅ | ✅ | ❌ |
| `getChildren` 注册 | ❌ | ✅ | ❌ | ✅ |

### 3.3 服务端通知机制

```
客户端注册 Watch → 服务端 WatchManager 记录
数据变更 → WatchManager 触发通知 → 客户端回调
注意事项：
  → Watch 是一次性的（触发后从 WatchManager 移除）
  → 通知是异步的（服务端不等待客户端响应）
  → 数据变更和通知之间可能有窗口（客户端收到通知时数据可能又变了）
```

---

## 4. Watch 的坑与最佳实践

### ❌ 常见坑

| 坑 | 表现 | 解决 |
|----|------|------|
| Watch 丢失 | 触发后未重新注册，后续变更收不到 | 回调中重新注册 |
| 羊群效应 | 所有客户端 Watch 同一节点，同时被唤醒 | 用临时顺序节点 + Watch 前驱 |
| 空通知 | 收到了 Watch 但数据未实际变化 | 收到 Watch 后 `getData` 获取最新数据 |

### ✅ 最佳实践

```java
// ⭐ Curator 的 TreeCache — 自动反复注册，屏蔽 Watch 复杂性
CuratorFramework client = CuratorFrameworkFactory.newClient(...);
client.start();

TreeCache cache = new TreeCache(client, "/config");
cache.getListenable().addListener((c, event) -> {
    switch (event.getType()) {
        case NODE_ADDED:    /* 新增 */ break;
        case NODE_UPDATED:  /* 更新 */ break;
        case NODE_REMOVED:  /* 删除 */ break;
    }
});
cache.start();    // 自动注册 Watch，反复监听
```

> 🎯 **Curator 推荐**：原生 Watch 一次性和重注册太容易踩坑，生产环境一律用 Curator 的 `TreeCache`/`NodeCache`/`PathChildrenCache`。Session 过期必须重建整个 ZooKeeper 对象。
