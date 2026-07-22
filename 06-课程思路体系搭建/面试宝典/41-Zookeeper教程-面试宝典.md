# Zookeeper教程 面试宝典
> 基于Zookeeper课程大纲，覆盖ZAB协议、分布式锁、集群选举等面试高频考点，结合Curator实战与原理深度剖析

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

### 1.1 Zookeeper是什么？主要应用场景有哪些？
Zookeeper是Apache开源的分布式协调服务，基于ZAB协议实现强一致性（CP），核心维护一个层级命名空间（类似文件树）的数据模型。主要应用场景：**分布式锁**（临时顺序节点+Watch互斥）、**配置管理**（动态下发监听）、**服务注册发现**、**命名服务**（全局唯一ID）、**集群管理**（Leader选举）。每个ZNode默认最大1MB，是协调工具而非数据库。

### 1.2 ZNode节点类型
| 类型 | 生命周期 | 适合场景 |
|------|---------|---------|
| PERSISTENT | 持久化，手动删除 | 配置数据 |
| EPHEMERAL | 会话绑定，断开自动删 | 服务注册、分布式锁 |
| PERSISTENT_SEQUENTIAL | 持久化+ZK自动递增编号 | 命名服务 |
| EPHEMERAL_SEQUENTIAL | 临时+自动编号 | 分布式锁核心实现 |

> 💡 EPHEMERAL节点不能创建子节点。SEQUENTIAL节点序号单调递增且唯一，由ZK内部维护。

### 1.3 Watcher机制原理
客户端在ZNode上注册Watcher，当节点数据/子节点/连接状态变化时，服务端异步发送通知给客户端。**三特性**：一次性触发（触发后失效，需重新注册）、轻量级推送（仅含事件类型+路径，不含数据内容）、异步串行（回调在EventThread中排队执行）。

> ⚠️ 一次性触发是高频陷阱——监听到变化后未重新注册则后续变更无法感知。

### 1.4 ZAB协议
ZAB（Zookeeper Atomic Broadcast）是ZK保证一致性的核心协议，分两阶段：**崩溃恢复**（Leader宕机后选举新Leader+数据同步）和**消息广播**（Leader生成zxid，以Proposal发给Follower，过半ACK后Commit）。保证同时最多一个Leader对外服务，事务按zxid全局有序执行。

### 1.5 Leader选举流程
采用FastLeaderElection算法，投票内容为`(myid, zxid, epoch)`。比较规则：**epoch**优先（越大越新），其次**zxid**优先（越大表示数据越新），最后**myid**优先。Server收到投票后比较并更新，当某节点获得Quorum票数（`n/2+1`）即当选。新Leader通过DIFF/TRUNC/SNAP方式同步数据。zxid高32位是epoch，低32位是事务计数器。

### 1.6 ZK集群角色
| 角色 | 功能 | 投票权 |
|------|------|--------|
| Leader | 处理写请求，协调ZAB广播 | 是 |
| Follower | 读请求，转发写请求，参与选举 | 是 |
| Observer | 只读扩展，不参与选举和投票 | 否 |

> 💡 Observer数量不受奇数限制，可在不影响写性能的前提下水平扩展读能力。

### 1.7 Session会话管理
客户端与服务端通过TCP长连接建立Session，设置`sessionTimeout`（通常5-10s）。ZK采用**分桶策略**管理大量会话，将超时时间相近的Session放入同一桶中批量处理过期检查，时间复杂度O(1)。客户端心跳Ping维持活跃，断开后重连且超时前恢复则Session保留，否则EPHEMERAL节点自动删除。

### 1.8 分布式锁实现对比
| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| ZK锁 | 临时顺序节点+Watch | 高可靠、无死锁、顺序抢占 | 性能较低（CP） |
| Redis锁 | SETNX+Lua+Redlock | 性能极高 | 主从切换丢锁风险（AP） |
| 数据库锁 | SELECT FOR UPDATE | 实现简单 | 性能瓶颈 |

> 💡 一致性要求极高（金融场景）选ZK；性能要求高且允许少量锁丢失选Redis。

### 1.9 Curator框架
Netflix开源的高阶ZK客户端。核心能力：自动重连/会话恢复、ExponentialBackoffRetry重试策略、InterProcessMutex可重入锁、NodeCache/PathChildrenCache/TreeCache自动处理Watcher重新注册。面试推荐使用Curator而非原生API。

### 1.10 为什么ZK集群用奇数节点
Quorum机制需要`n/2+1`节点同意。3节点集群容1台宕机，4节点同样只容1台——奇数节点节省资源且相同容错。避免偶数节点网络分区时两边都不达Quorum的问题。5节点容忍2台，6节点同样容忍2台。

### 1.11 ZK vs Nacos vs Eureka vs Consul vs Etcd
| 维度 | ZK | Nacos | Eureka | Consul | Etcd |
|------|-----|-------|--------|--------|------|
| CAP | CP | AP/CP切换 | AP | CP | CP |
| 一致性 | ZAB | Distro+Raft | 自我保护 | Raft | Raft |
| 监听 | Watcher一次性 | 长轮询 | 轮询 | 阻塞查询 | Watch长轮询 |
| 场景 | 分布式协调/锁 | 配置/注册中心 | 注册中心(AP) | 注册/配置/网络 | K8s/K-V |

> 🎯 选型：需要锁/选举选ZK；Spring Cloud生态选Nacos；K8s生态选Etcd；只做注册且接受AP选Eureka。

### 1.12 ZK的CAP为什么是CP
网络分区时ZK停止写入，选择Quorum大的一侧继续服务，少数分区拒绝写请求直到分区恢复。选举期间有几十到几百毫秒不可用窗口，这是CP模型下放弃可用性（A）的代价。

---

## 二、深度原理剖析

### 2.1 ZAB协议详解
**广播阶段**：Leader收到写请求 -> 生成zxid -> Proposal发给Follower -> Follower写事务日志返回ACK -> Leader收到过半ACK后发Commit。**恢复阶段**：Leader宕机 -> 集群进入LOOKING -> FastLeaderElection选举 -> 新Leader通过DIFF（增量）/TRUNC（截断）/SNAP（全量）同步Follower数据 -> FOLLOWING状态恢复服务。核心设计：Leader中心 + Quorum投票 + 顺序广播。

### 2.2 FastLeaderElection算法
投票结构`(proposedLeader, proposedZxid, proposedEpoch)`。每Server先投自己，收到外部投票后按epoch > zxid > myid比较。更新投票为更优者并广播，当获得Quorum票数后成为Leader。Leader收集所有Follower的ACKEpoch确定同步起点。zxid比myid优先级高是因为必须保证新Leader拥有最完整数据，避免已提交事务丢失。

### 2.3 Zab vs Paxos vs Raft
| 特性 | Zab | Paxos | Raft |
|------|-----|-------|------|
| 设计目标 | 原子广播 | 共识决策 | 共识+日志复制 |
| 选举 | FastLeaderElection | Multi-Paxos弱Leader | 超时随机化选举 |
| 复杂度 | 中等 | 高（难理解） | 低（易实现） |
| 实现 | Zookeeper | Chubby | Etcd/Consul |

> 💡 ZAB要求Leader的zxid必须最新；Raft允许落后节点当选后通过日志复制追赶。ZAB同步支持DIFF/TRUNC/SNAP三种模式。

### 2.4 Watcher机制详解
**注册**：客户端调`getData("/path", watcher)` -> 封装WatchRegistration附加到Packet -> 服务端存储到WatchManager。**触发**：写操作提交后，服务端从WatchManager查找关联Watcher -> 异步发送WatchedEvent -> 客户端EventThread回调process()。**移除**：触发后自动移除；连接断开时清理；节点删除时全部移除。

### 2.5 Session分桶管理
`SessionTracker`组件以`expirationInterval`为粒度将Session划分为时间桶：`Map<Long, SessionSet>`。独立线程定时扫描桶，当前时间超过桶过期时间则批量清理该桶，触发对应EPHEMERAL节点删除和Watcher通知。复杂度O(1)，避免逐个检查Session。

### 2.6 ZK分布式锁原理
创建EPHEMERAL_SEQUENTIAL节点 -> 获取所有子节点排序 -> 判断自己是否最小序号：是则获锁；否则监听前一个节点。前一个节点删除时触发Watcher，重新判断。**排他锁+公平锁**，序号天然决定等待顺序，避免了惊群效应。

### 2.7 事务日志与Snapshot
**事务日志**：每次写操作先写WAL再改内存，按zxid命名，64M滚动。**Snapshot**：每10000次事务将全量内存数据序列化到磁盘。重启时加载最新Snapshot，再回放后续事务日志重建内存数据。

### 2.8 NIO通信
Client端：`ClientCnxn`的SendThread发请求/心跳，EventThread串行执行Watcher回调。Server端：`NIOServerCnxnFactory`用Selector处理多路复用。请求处理责任链：`PrepRequestProcessor -> SyncRequestProcessor -> FinalRequestProcessor`。

### 2.9 ZAB如何保证强一致性
1. 全局有序：每个事务分配唯一递增zxid；2. Quorum提交：过半ACK才Commit；3. Leader唯一：同时最多一个Leader处理写请求；4. 同步保证：新Leader必须拥有所有已提交事务；5. FIFO顺序：同客户端请求FIFO处理。不保证任意时刻所有节点数据完全一致。

### 2.10 ZK ACL权限控制
5种Scheme：**world**（默认，所有人）、**auth**（已认证用户）、**digest**（用户名密码）、**ip**（IP限制）、**super**（绕过检查）。权限类型：CREATE(c)、READ(r)、WRITE(w)、DELETE(d)、ADMIN(a)。生产环境务必开启ACL，默认`world:anyone:cdrwa`存在安全隐患。

### 2.11 Follower vs Observer
| 对比 | Follower | Observer |
|------|---------|----------|
| 投票权 | 参与选举和投票 | 不参与 |
| 数据同步 | 同步Leader | 同步Leader |
| 功能 | 读+转发写 | 读+转发写 |
| 写性能影响 | 增加投票节点降低写性能 | 不影响 |

Observer解决读多写少场景下投票节点增多导致写性能下降的问题。

---

## 三、实战场景题

### 3.1 Curator连接ZK
```java
RetryPolicy retry = new ExponentialBackoffRetry(1000, 3);
CuratorFramework client = CuratorFrameworkFactory.builder()
    .connectString("192.168.1.100:2181,192.168.1.101:2181,192.168.1.102:2181")
    .sessionTimeoutMs(10000)
    .connectionTimeoutMs(5000)
    .retryPolicy(retry)
    .namespace("myapp")
    .build();
client.start();
```

### 3.2 Curator创建节点
```java
client.create().forPath("/config", "db-config".getBytes());
client.create().withMode(CreateMode.EPHEMERAL)
    .forPath("/services/order", "192.168.1.10:8080".getBytes());
client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
    .creatingParentContainersIfNeeded().forPath("/locks/lock-", "".getBytes());
client.create().withMode(CreateMode.PERSISTENT_SEQUENTIAL)
    .forPath("/seq-", "".getBytes());
```

### 3.3 Curator查询/更新/删除
```java
byte[] data = client.getData().forPath("/config");
List<String> children = client.getChildren().forPath("/services");
Stat stat = new Stat();
client.getData().storingStatIn(stat).forPath("/config");
client.setData().withVersion(1).forPath("/config", "new".getBytes());
client.delete().guaranteed().deletingChildrenIfNeeded().forPath("/config");
```

### 3.4 NodeCache配置监听
```java
NodeCache cache = new NodeCache(client, "/config");
cache.getListenable().addListener(() -> {
    ChildData current = cache.getCurrentData();
    if (current != null) {
        refreshLocalConfig(new String(current.getData()));
    }
});
cache.start();
```
NodeCache内部自动处理Watcher重新注册，适合配置中心动态刷新。

### 3.5 PathChildrenCache服务发现
```java
PathChildrenCache cache = new PathChildrenCache(client, "/services", true);
cache.getListenable().addListener((curator, event) -> {
    switch (event.getType()) {
        case CHILD_ADDED -> register(event.getData().getPath(), event.getData().getData());
        case CHILD_REMOVED -> unregister(event.getData().getPath());
        case CHILD_UPDATED -> update(event.getData().getPath(), event.getData().getData());
    }
});
cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
```

### 3.6 InterProcessMutex分布式锁
```java
InterProcessMutex lock = new InterProcessMutex(client, "/locks/order");
lock.acquire();
try { processOrder(orderId); } finally { lock.release(); }

// 超时版本
if (lock.acquire(3, TimeUnit.SECONDS)) {
    try { processOrder(orderId); } finally { lock.release(); }
} else { System.out.println("获取锁超时"); }
```

### 3.7 ZK集群搭建配置
```properties
tickTime=2000
dataDir=/data/zookeeper
dataLogDir=/data/zookeeper/logs
clientPort=2181
initLimit=10      # Follower初始化等待超时（tickTime倍数）
syncLimit=5       # Follower与Leader心跳超时
server.1=192.168.1.10:2888:3888
server.2=192.168.1.11:2888:3888
server.3=192.168.1.12:2888:3888
server.4=192.168.1.13:2888:3888:observer
```
每节点`dataDir/myid`文件内容为对应编号（1/2/3/4）。

### 3.8 ZK命令行操作
```bash
create /app "hello"                # 持久节点
create -e -s /lock/lock- ""       # 临时顺序节点
get /app                           # 获取数据+状态
set /app "new-value"               # 更新数据
deleteall /app                     # 递归删除
get /app watch                     # 注册Watcher监听
```

---

## 四、手写代码题

### 4.1 Curator分布式锁完整实现
```java
public class DistributedLockDemo {
    public static void main(String[] args) throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.newClient(
            "192.168.1.100:2181", new ExponentialBackoffRetry(1000, 3));
        client.start();
        InterProcessMutex lock = new InterProcessMutex(client, "/locks/order");
        for (int i = 0; i < 10; i++) {
            int tid = i;
            new Thread(() -> {
                try {
                    lock.acquire();
                    System.out.println("Thread-" + tid + " acquired");
                    Thread.sleep(2000);
                } catch (Exception e) { e.printStackTrace();
                } finally {
                    try { lock.release(); } catch (Exception e) {}
                }
            }).start();
        }
        Thread.sleep(30000); client.close();
    }
}
```

### 4.2 NodeCache配置动态更新
```java
public class ConfigCenter {
    private final AtomicReference<String> cache = new AtomicReference<>();
    public ConfigCenter(CuratorFramework client, String path) throws Exception {
        cache.set(new String(client.getData().forPath(path)));
        NodeCache nodeCache = new NodeCache(client, path);
        nodeCache.getListenable().addListener(() -> {
            ChildData data = nodeCache.getCurrentData();
            if (data != null) { cache.set(new String(data.getData())); refresh(); }
        });
        nodeCache.start();
    }
    public String get() { return cache.get(); }
    private void refresh() { /* 重建连接池等 */ }
}
```

### 4.3 PathChildrenCache服务发现
```java
public class ServiceDiscovery {
    private final Map<String, String> services = new ConcurrentHashMap<>();
    public ServiceDiscovery(CuratorFramework client, String basePath) throws Exception {
        PathChildrenCache cache = new PathChildrenCache(client, basePath, true);
        cache.getListenable().addListener((c, event) -> {
            String path = event.getData().getPath();
            String addr = event.getData().getData() != null ? new String(event.getData().getData()) : "";
            switch (event.getType()) {
                case CHILD_ADDED -> services.put(path, addr);
                case CHILD_REMOVED -> services.remove(path);
                case CHILD_UPDATED -> services.put(path, addr);
            }
        });
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
    }
}
```

### 4.4 原生API创建EPHEMERAL_SEQUENTIAL
```java
ZooKeeper zk = new ZooKeeper("127.0.0.1:2181", 5000, e -> {});
if (zk.exists("/locks", false) == null)
    zk.create("/locks", "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
String path = zk.create("/locks/lock-", "client1".getBytes(),
    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
List<String> children = zk.getChildren("/locks", false);
zk.delete(path, -1);
zk.close();
```

### 4.5 原生API分布式锁核心
```java
public class ZkDistributedLock {
    private final ZooKeeper zk; private final String lockPath;
    private String currentPath;

    public void lock() throws Exception {
        currentPath = zk.create(lockPath + "/lock-", "".getBytes(),
            ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        tryAcquire();
    }
    private void tryAcquire() throws Exception {
        List<String> children = zk.getChildren(lockPath, false);
        Collections.sort(children);
        String cur = currentPath.substring(currentPath.lastIndexOf("/") + 1);
        int idx = children.indexOf(cur);
        if (idx == 0) return; // 获锁成功
        String prev = lockPath + "/" + children.get(idx - 1);
        Stat stat = zk.exists(prev, event -> {
            if (event.getType() == Event.EventType.NodeDeleted) tryAcquire();
        });
        if (stat == null) tryAcquire(); // 前节点已删除
    }
    public void unlock() throws Exception { zk.delete(currentPath, -1); }
}
```

---

## 五、系统设计题

### 5.1 基于ZK的分布式锁服务
**需求**：高可用分布式锁，支持可重入、超时释放、阻塞/非阻塞获取。**方案**：Curator连接ZK集群 -> InterProcessMutex（可重入）和InterProcessReadWriteLock（读写锁）-> acquire(timeout, unit)超时控制 -> ConcurrentMap记录重入次数 -> EPHEMERAL节点兜底客户端宕机自动释放。**高可用**：集群至少3节点，sessionTimeout设3-5s，开启Curator自动重连。

> 🎯 EPHEMERAL节点天然解决"客户端挂了"的问题，这是ZK对比Redis锁的最大安全优势。

### 5.2 基于ZK的服务注册与发现
**数据模型**：`/services/{serviceName}/{instanceId}`（临时节点存IP:Port）。**注册**：服务启动创建临时节点，session断开自动删除。**发现**：消费者通过PathChildrenCache监听子节点变化 -> 更新本地缓存 -> 轮询/随机路由。**容量**：单节点子节点建议<1000，大规模用Observer扩展读能力。

> 💡 ZK服务发现优势是Watch实时推送而非Eureka轮询；短板是写瓶颈。

### 5.3 基于ZK的配置中心
**数据模型**：`/config-center/{appName}/{configKey}`。**实时推送**：客户端通过NodeCache监听配置节点 -> 变更时Watcher通知 -> 更新本地缓存。**版本管理**：利用Stat.version做乐观锁。示例：监听数据库配置变更 -> 重建HikariCP连接池。

### 5.4 网络分区应对
5节点集群[S1,S2,S3,S4,S5]分为分区A[S1,S2,S3]和分区B[S4,S5]。分区A达Quorum(3/5)可选举Leader继续服务；分区B未达Quorum(2/5)拒绝服务。分区恢复后B节点通过DIFF/TRUNC同步数据。ZK通过牺牲分区期间少数分区可用性换取强一致性。

---

## 六、常见坑点与最佳实践

| 类别 | 坑点 | 最佳实践 |
|------|------|---------|
| Watcher | 一次性触发，后续变更无感知 | 用Curator Cache自动重新注册 |
| Session | 超时导致EPHEMERAL节点丢失 | sessionTimeout设3-10s，避免过短 |
| ZNode | 大量子节点时getChildren性能极差 | 控制子节点<1000，超时做Hash分片 |
| ACL | 默认world:anyone:cdrwa无权限限制 | 生产开启digest/ip认证，最小权限 |
| 集群 | 偶数节点脑裂风险 | 3/5/7奇数节点 + Quorum过半 |
| 重连 | Session过期后需重建节点 | ConnectionStateListener监听RECONNECTED |
| 日志 | 日志/快照磁盘写满 | autopurge.purgeInterval定期清理 |
| 选举 | 选举期间不可写 | 确保网络稳定，避免频繁Leader切换 |
| 版本 | 并发更新版本号冲突 | setData()带version做乐观锁，失败重试 |
| 部署 | 2节点集群一台宕机即不可用 | 最少3节点，生产建议5节点 |

> ⚠️ Watcher一次性是最高频坑点。配置中心必须解决"监听回调中重新注册Watcher"问题，Curator的NodeCache已自动处理。

---

## 七、面试回答模板

### 7.1 "谈谈Zookeeper的ZAB协议"
> ZAB是ZK的原子广播协议，分两个阶段。消息广播：Leader为写请求生成zxid，以Proposal发给Follower，过半ACK后Commit。崩溃恢复：Leader宕机后FastLeaderElection选举新Leader，通过DIFF/TRUNC/SNAP同步Follower数据。核心保证：已提交事务不丢失，所有Server按相同顺序执行写请求。

### 7.2 "ZK Leader选举过程"
> 基于FastLeaderElection算法，按`epoch > zxid > myid`比较优先级。epoch是Leader任期，zxid表示数据新鲜度，myid是服务器编号。每Server先投自己，收到更好投票就更新并广播。获得Quorum（`n/2+1`）票数即当选。新Leader确认后Follower变FOLLOWING状态，完成选举，耗时几十到几百毫秒。

### 7.3 "ZK如何实现分布式锁"
> 核心是EPHEMERAL_SEQUENTIAL节点+Watcher。加锁时创建临时顺序节点，获取所有子节点排序，最小序号者获锁成功，否则监听前一个节点。前节点删除时Watcher触发，重新判断。释放时删除自己的节点，客户端宕机则EPHEMERAL自动删除，不会死锁。Curator的InterProcessMutex封装了此逻辑，支持可重入和超时。

### 7.4 "ZK的Watcher机制原理"
> 发布/订阅机制。通过`getData()`等API注册Watcher到服务端WatchManager。节点变化时服务端异步发送WatchedEvent。三特性：一次性触发（触发后移除，需重新注册）；轻量级（仅含事件类型+路径，不含数据内容）；异步串行（所有回调在EventThread中排队执行，避免并发问题）。

### 7.5 "为什么ZK适合做注册中心/配置中心"
> 三点原因：一是**临时节点**天然适合服务实例注册，session断开自动注销；二是**Watcher实时性**优于Eureka的轮询；三是**ZAB强一致性**不会读到旧数据。局限：不适合数千以上大规模实例（Leader写入压力），不适合存储配置变更历史。

> 🎯 回答要体现选型思维，同时说明适合和不适合的场景，对比Nacos/Eureka。

---

## 八、快速查漏补缺Checklist

- [ ] 能解释ZAB协议的两个阶段（崩溃恢复 + 消息广播）
- [ ] 能画出FastLeaderElection的投票比较流程（epoch > zxid > myid）
- [ ] 能说出ZK 4种节点类型及适用场景
- [ ] 能解释Watcher三特性（一次性、轻量级、异步串行）
- [ ] 能写出Curator连接ZK的完整配置代码
- [ ] 能实现InterProcessMutex分布式锁的acquire/release
- [ ] 能实现NodeCache监听配置变更的完整代码
- [ ] 能实现PathChildrenCache监听服务注册变化的代码
- [ ] 能说出ZK集群最少节点数及原因（3节点，Quorum过半）
- [ ] 能解释Session分桶策略的O(1)优化原理
- [ ] 能区分Follower和Observer的角色差异
- [ ] 能解释zxid的组成结构（epoch + 计数器）
- [ ] 能对比ZK vs Nacos vs Eureka vs Consul的CAP差异
- [ ] 能解释网络分区时ZK集群的Quorum决策过程
- [ ] 能说出ZK的事务日志和快照恢复流程
- [ ] 能说明EPHEMERAL节点自动释放避免死锁的原理
- [ ] 能配置zoo.cfg的集群参数（initLimit/syncLimit）
- [ ] 能实现ZK原生API创建EPHEMERAL_SEQUENTIAL节点
- [ ] 能说出ZK的ACL五种权限控制模式
- [ ] 能解释ZAB与Paxos/Raft的核心差异

> 💡 优先掌握前10个核心知识点，覆盖80%的ZK面试问题。代码题重点练Curator的三个Cache和InterProcessMutex。
