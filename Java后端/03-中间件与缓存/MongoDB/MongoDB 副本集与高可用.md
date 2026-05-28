# MongoDB 副本集与高可用（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MongoDB 高可用方案详解
> **版本**：MongoDB 6.x/7.x
> **核心场景**：数据冗余、故障自动转移、读写分离

---

## 一、副本集架构

```
              Client
                 ↓
          ┌─────────────┐
          │  Primary     │ ← 读写
          │ (主节点)     │
          └──────┬───────┘
       oplog 同步
    ┌─────────┼─────────┐
    ↓         ↓         ↓
┌───────┐ ┌───────┐ ┌────────┐
│Secondary│Secondary│ Arbiter │
│ (只读)  │ (只读)  │ (仅投票)│
└───────┘ └───────┘ └────────┘
```

| 角色 | 数量建议 | 职责 |
|---|---|---|
| **Primary** | 1 | 唯一可读写，记录 oplog |
| **Secondary** | 1-2+ | 同步 oplog，提供只读查询 |
| **Arbiter** | 0-1 | 仅投票，不存数据，节省资源 |

---

## 二、oplog 核心机制

### 2.1 什么是 oplog

oplog（Operation Log）是 Primary 记录所有写操作的**有上限集合**（capped collection），存放在 `local.oplog.rs`：

```javascript
use local
db.oplog.rs.find().sort({ $natural: -1 }).limit(1).pretty()

// 示例 oplog 条目
{
  "ts": Timestamp(1705312000, 1),   // 操作时间戳
  "op": "i",                        // 操作类型：i=insert, u=update, d=delete
  "ns": "mydb.users",               // 命名空间（库.集合）
  "o": { "_id": ..., "name": "张三" } // 操作内容
}
```

### 2.2 同步流程

```
1. Primary 执行写操作 → 写入 oplog
2. Secondary 不断拉取 Primary 的 oplog
3. Secondary 在本机重放 oplog 条目
4. 有延迟 = Secondary 未及时同步到最新 oplog
```

### 2.3 oplog 大小

```javascript
// 查看 oplog 信息
db.getReplicationInfo()
// "logSizeMB": 2048          // oplog 总大小
// "timeDiff": 3600           // 覆盖小时数

// oplog 窗口公式：
// 覆盖时间 = oplog 大小 / 每小时产生的 oplog 量
// 建议至少覆盖 24 小时的写入量
```

---

## 三、选举机制

### 3.1 什么时候选举

- Primary 宕机 / 网络隔离
- Primary 被手动降级（`rs.stepDown()`）
- Secondary 发现 Primary 心跳超时（默认 10s）

### 3.2 Raft 协议核心

```
选举条件：
1. 节点健康（能与其他节点通信）
2. 数据比候选者新（oplog 的 ts 更大）
3. 获得多数投票（N/2 + 1）
```

### 3.3 防止脑裂

```
节点数必须是奇数：
  3 节点：需要 2 票 → 安全
  4 节点：需要 3 票 → 比 3 节点多一台但没有更强的可用性
  5 节点：需要 3 票 → 可容忍 2 个节点宕机

推荐：
  生产最小 = 3 节点（1 Primary + 2 Secondary）
  生产标准 = 5 节点（1 Primary + 3 Secondary + 1 Arbiter）
```

---

## 四、读写策略（Read Preference + Write Concern）

### 4.1 Read Preference（读偏好）

```javascript
// 连接串中指定
mongodb://host1,host2,host3/mydb?readPreference=secondary
```

| 策略 | 说明 | 适用场景 |
|---|---|---|
| `primary`（默认） | 只读主节点 | 需要最新数据 |
| `primaryPreferred` | 优先主，不可用读从 | 一般业务 |
| `secondary` | 只读从节点 | 报表、数据分析 |
| `secondaryPreferred` | 优先从，不可用读主 | |
| `nearest` | 延迟最低 | 多机房 |

### 4.2 Write Concern（写确认级别）

```javascript
// 插入时指定写关注
db.users.insertOne(
  { name: "张三" },
  { writeConcern: { w: "majority", j: true, wtimeout: 5000 } }
)
```

| 参数 | 值 | 含义 |
|---|---|---|
| **w** | `0` | 不等待确认（最快，可能丢失） |
| | `1`（默认） | Primary 确认即可 |
| | `"majority"` | 多数节点确认（推荐） |
| | `N` | N 个节点确认 |
| **j** | `true` | 写入 Journal 后返回 |
| **wtimeout** | 毫秒 | 超时时间 |

```yaml
# SpringBoot 连接串中配置
uri: mongodb://host1:27017,host2:27017,host3:27017/mydb?replicaSet=rs0&w=majority&readPreference=secondaryPreferred
```

---

## 五、副本集部署实战

### 5.1 配置示例

```yaml
# mongod.conf — 每个节点都要配
replication:
  replSetName: rs0
```

### 5.2 初始化副本集

```javascript
// 连接到其中一个节点
mongosh --port 27017

// 初始化（只需执行一次）
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "host1:27017", priority: 2 },    // 高优先级 = 优先选主
    { _id: 1, host: "host2:27017", priority: 1 },
    { _id: 2, host: "host3:27017", priority: 1 }
  ]
})
```

### 5.3 运维命令

```javascript
// 查看副本集状态
rs.status()

// 查看各节点同步情况
rs.printSecondaryReplicationInfo()

// 查看当前节点信息（是否是主）
db.isMaster()

// 手动切换主（从 Primary 执行）
rs.stepDown()

// 添加节点
rs.add("host4:27017")
// 或作为 Arbiter
rs.addArb("host5:27017")

// 移除节点
rs.remove("host4:27017")
```

---

## 六、副本集延迟监控

```javascript
// 查看各 Secondary 的同步延迟
rs.printSecondaryReplicationInfo()

// 输出：
// host2:27017 - 0 sec behind primary
// host3:27017 - 2 sec behind primary
```

**延迟过大后果**：
- 主库宕机时，延迟最大的从库可能丢失数据
- 从库读时返回旧数据

**解决办法**：
- 增大 oplog 大小
- 提升硬件（SSD、网络带宽）
- 减少不必要的写操作

---

## 七、面试核心要点

1. **副本集几个节点？** 最少 3 个（1主2从），推荐奇数
2. **选举原理是什么？** Raft 协议，获得 N/2+1 投票
3. **oplog 是什么？** Primary 写操作日志，从节点拉取重放
4. **Write Concern majority 什么意思？** 多数节点写入确认后才返回
5. **从节点读会读到旧数据吗？** 可能，取决于同步延迟，容忍延迟的场景可用

---

## 八、极简总结

```
副本集 = 1 Primary + N Secondary + 可选 Arbiter
oplog = Primary 写操作日志 → Secondary 拉取重放
选举 = Raft 协议，多数投票
读 = 默认 Primary，可配 secondary 分担压力
写 = 默认 Primary 确认，生产建议 majority
节点数 = 奇数，3 是最小生产部署
```
