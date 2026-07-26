# Redis 持久化与数据安全
> Redis 是内存数据库，断电即丢——持久化是数据不丢失的最后防线。RDB + AOF 混合持久化是生产环境的标准配置。

## 目录
1. [RDB 持久化](#1-rdb-持久化)
2. [AOF 持久化](#2-aof-持久化)
3. [混合持久化](#3-混合持久化)
4. [数据恢复与备份策略](#4-数据恢复与备份策略)
5. [持久化方案对比与选型](#5-持久化方案对比与选型)
6. [内存淘汰策略](#6-内存淘汰策略)
7. [性能优化与安全配置](#7-性能优化与安全配置)

---

## 1. RDB 持久化

RDB（Redis Database）通过定时生成全量内存快照（二进制 dump.rdb 文件），实现数据持久化。

### 1.1 原理

```text
RDB 文件格式（二进制）：
┌──────────┬──────────┬──────────┬──────────┬──────────┐
│ REDIS    │ RDB版本  │ 数据库   │ 键值对   │ 校验和   │
│ (5字节)  │ (4字节)  │ 选择器   │ 数据     │ (8字节)  │
└──────────┴──────────┴──────────┴──────────┴──────────┘
                          ↓
         采用 LZF 压缩算法，二进制紧凑存储
         加载时直接二进制解析到内存，速度极快
```

### 1.2 触发方式

| 触发方式 | 命令/配置 | 描述 | 是否阻塞 | 生产使用 |
|----------|-----------|------|----------|----------|
| 手动 SAVE | `SAVE` | 在主进程生成快照 | **阻塞所有请求** | ❌ 禁用 |
| 手动 BGSAVE | `BGSAVE` | fork 子进程后台生成 | 仅 fork 时阻塞 | ✅ 推荐 |
| 自动策略 | `save 900 1` | 900 秒内至少 1 次修改 | 后台执行 | ✅ 推荐 |
| 自动策略 | `save 300 10` | 300 秒内至少 10 次修改 | 后台执行 | ✅ 推荐 |
| 自动策略 | `save 60 10000` | 60 秒内至少 10000 次修改 | 后台执行 | ✅ 推荐 |
| 关闭时 | `shutdown` | 自动执行 SAVE | 关闭时阻塞 | ✅ |
| 主从全量复制 | 自动 | 主节点自动生成 | 后台执行 | ✅ |

```bash
# redis.conf RDB 配置
save 900 1           # 900 秒 ≥ 1 次写
save 300 10          # 300 秒 ≥ 10 次写
save 60 10000        # 60 秒 ≥ 10000 次写

dbfilename dump.rdb  # RDB 文件名
dir /usr/local/redis/data  # 文件目录
rdbcompression yes   # LZF 压缩（推荐启用，节省磁盘）
rdbchecksum yes      # CRC64 校验（检测文件损坏）
stop-writes-on-bgsave-error yes  # BGSAVE 失败时停止写入
rdb-del-sync-files no
```

### 1.3 BGSAVE 写时复制（Copy-on-Write）

```text
BGSAVE 执行流程：
1. 主进程调用 fork() 创建子进程
   ┌─────────────────────┐
   │     父进程           │
   │  ┌────────────────┐ │
   │  │ 内存数据        │ │──── fork() ────→ ┌─────────────────────┐
   │  └────────────────┘ │                   │     子进程           │
   └─────────────────────┘                   │  ┌────────────────┐ │
            │                                │  │ 共享内存页（只读）│ │
            ▼                                │  └────────────────┘ │
   主进程继续处理请求                          └─────────────────────┘
   当主进程修改某内存页时：                            │
   ┌─────────────────────┐                          ▼
   │ 复制该页 → 修改副本  │                 子进程将共享页写入
   └─────────────────────┘                 临时 RDB 文件
                                                    │
                                                    ▼
                                          写入完成，rename 替换
                                          旧 dump.rdb 文件
```

**COW 的代价：**

| 场景 | 影响 | 说明 |
|------|------|------|
| 写入频繁 | 内存翻倍 | 每修改一个内存页就复制一页，大量写入时可达到 2 倍内存 |
| fork 耗时 | 秒级延迟 | 内存越大 fork 越慢（1GB 约 10ms，10GB 约 100ms+） |
| CPU 消耗 | 短暂升高 | 子进程压缩 RDB 文件消耗 CPU |
| 磁盘 I/O | 大文件写入 | 全量数据写入磁盘，I/O 压力大 |

> ⚠️ COW 的关键风险：大量写操作时，主进程修改的每个内存页都需要复制，内存占用峰值约 2 倍正常值。**生产环境需预留足够内存**，建议留 50% 的余量给 COW。配置 `sysctl vm.overcommit_memory=1` 可避免 fork 时内存不足。

### 1.4 优缺点

| 优点 | 缺点 |
|------|------|
| 文件紧凑，体积小，适合冷备份和灾难恢复 | 可能丢失最后一次快照之后的所有数据 |
| 恢复速度极快（直接二进制加载到内存） | fork 子进程可能耗时（大实例可达秒级） |
| 子进程不影响主进程性能（仅 fork 时短暂阻塞） | 大量写时 COW 增加内存开销 |
| 加载优先级低于 AOF，但速度快 | 无法秒级恢复（按时钟周期触发） |

---

## 2. AOF 持久化

AOF（Append Only File）以日志形式记录每一条写命令，追加到 `appendonly.aof` 文件。重启时重放命令恢复数据。

### 2.1 原理

```text
AOF 工作流程：
客户端命令
    │
    ▼
① 追加到 aof_buf（内存缓冲区）
    │
    ▼
② 根据 appendfsync 策略刷盘
    │
    ▼
③ 写入 AOF 文件（磁盘）
    │
    ▼
④ AOF 重写（压缩文件大小）
```

### 2.2 AOF 文件格式

```text
AOF 文件内容（可读的 RESP 协议格式）：
*2          ← 数组长度
$6          ← 字符串长度
SELECT      ← 命令
$1          ← 参数长度
0           ← 参数（数据库号）
*3
$3
SET
$4
name
$5
redis
*3
$3
SET
$4
age
$2
25
```

> 💡 AOF 文件是纯文本 RESP 协议格式，可直接用 `cat` 查看，也可用 `redis-check-aof` 工具修复损坏的 AOF 文件。

### 2.3 刷盘策略

| 策略 | 配置 | 行为 | 安全性 | 性能 |
|------|------|------|--------|------|
| always | `appendfsync always` | 每条命令执行后立即 fsync | **最高**，最多丢 1 条命令 | 最慢（约 1/10 性能） |
| everysec | `appendfsync everysec` | 每秒 fsync 一次 | 中等，最多丢 1 秒数据 | **性能好（推荐）** |
| no | `appendfsync no` | 由操作系统决定刷盘时机 | 最低，可能丢数秒数据 | 最快 |

```
always 写入流程：
SET k1 v1 ──→ aof_buf ──→ fsync ──→ 磁盘 ✅完成（等磁盘确认）
SET k2 v2 ──→ aof_buf ──→ fsync ──→ 磁盘 ✅
延迟：每次写操作等待磁盘 IO

everysec 写入流程：
SET k1 v1 ──→ aof_buf ──→ 内存缓冲区
SET k2 v2 ──→ aof_buf ──→ 内存缓冲区
                │
            每 1 秒 ──→ fsync ──→ 磁盘 ✅
延迟：不等待磁盘，1 秒批量刷一次

no 写入流程：
SET k1 v1 ──→ aof_buf ──→ 内核 page cache
SET k2 v2 ──→ aof_buf ──→ 内核 page cache
                │
          OS 调度（30 秒后）──→ 磁盘 ✅
```

> 💡 **生产环境推荐 `everysec`**：安全性和性能的最佳平衡。最多丢 1 秒的数据，性能影响极小。

### 2.4 配置

```bash
# redis.conf AOF 配置
appendonly yes                          # 开启 AOF（默认 no）
appendfilename "appendonly.aof"         # 文件名
appendfsync everysec                    # 刷盘策略（生产推荐）
auto-aof-rewrite-percentage 100         # 文件增长 100% 触发重写
auto-aof-rewrite-min-size 64mb          # 至少 64MB 才触发重写
no-appendfsync-on-rewrite yes           # 重写期间不 fsync
aof-load-truncated yes                  # 加载时忽略截断（最后一行不完整）
aof-use-rdb-preamble yes                # 混合持久化（Redis 4.0+）
```

### 2.5 AOF 重写（BGREWRITEAOF）

AOF 文件会不断增长，重写机制将当前内存数据转换为最少的写命令集，替换旧的庞大 AOF 文件。

```
AOF 重写前后对比：
重写前 AOF 文件包含：
  SET count 0
  INCR count    → count = 1
  INCR count    → count = 2
  INCR count    → count = 3
  INCR count    → count = 4
  INCR count    → count = 5
  （大量冗余命令，文件膨胀）

重写后 AOF 文件：
  SET count 5
  （只保留最终状态，一行搞定）
```

**重写过程（BGREWRITEAOF）：**

```text
1. fork 子进程，读取当前内存数据
   ┌─────────────────┐
   │   父进程          │
   │ 处理新请求        │── fork ──→ ┌─────────────────┐
   │ 同时写入重写缓冲区 │             │   子进程          │
   └────────┬────────┘             │ 读取内存数据生成    │
            │                      │ 最小写命令集       │
            ▼                      │ 写入临时 AOF 文件  │
    ┌───────────────┐              └────────┬────────┘
    │ 重写缓冲区      │                      │
    │（增量命令）     │                       ▼
    └───────────────┘              临时 AOF 文件写完成
            │                              │
            └──────────────────────────────┤
                                           ▼
                                  合并缓冲区增量命令
                                           │
                                           ▼
                                  原子替换旧 AOF 文件
```

**触发条件：**
```bash
# 自动重写条件
auto-aof-rewrite-percentage 100   # 当前文件大小 > 上次重写时大小的 100%
auto-aof-rewrite-min-size 64mb    # 且文件至少 64MB

# 手动触发
redis-cli BGREWRITEAOF
```

### 2.6 优缺点

| 优点 | 缺点 |
|------|------|
| 数据安全性高（everysec 仅丢 1 秒） | AOF 文件比 RDB 大（未压缩的协议文本） |
| 文件可读可修复（RESP 协议文本） | 恢复速度比 RDB 慢（需重放命令） |
| 支持重写自动压缩 | 性能开销略高于 RDB |
| 每条命令格式标准化 | always 策略性能差 |

---

## 3. 混合持久化

### 3.1 原理（Redis 4.0+）

AOF 重写时，不再是纯 AOF 格式，而是采用 RDB + AOF 混合格式：

```text
混合持久化 AOF 文件格式：
┌─────────────────────────────┬──────────────────────────────┐
│     RDB 格式（全量数据）     │    AOF 格式（增量命令）       │
│                             │                              │
│  二进制格式，紧凑存储        │  RESP 协议格式               │
│  包含所有 key-value 数据    │  记录重写期间的新增命令       │
│                             │                              │
│  加载超快（直接内存加载）     │  加载完后重放（保证完整）      │
└─────────────────────────────┴──────────────────────────────┘
```

**配置：**
```bash
aof-use-rdb-preamble yes   # 开启混合持久化
```

**恢复流程：**
```text
Redis 启动
    │
    ▼
检查是否有 AOF 文件
    │
    ▼
读取文件头部 `REDIS` 标记（RDB 格式标识）
    │
    ├── 是 RDB preamble → 快速加载 RDB 全量数据
    │                       │
    │                       ▼
    │                   读取后续 AOF 增量命令
    │                       │
    │                       ▼
    │                   重放增量命令
    │
    └── 不是 RDB → 纯 AOF 模式，逐条重放命令
```

> 🎯 **生产环境强烈推荐开启混合持久化**：既保留了 RDB 的快速恢复能力，又兼顾了 AOF 的数据完整性。

### 3.2 三种持久化方案对比

| 对比项 | 纯 RDB | 纯 AOF（everysec） | 混合持久化 |
|--------|--------|-------------------|-----------|
| 数据安全性 | 可能丢分钟级数据 | 最多丢 1 秒 | 最多丢 1 秒 |
| 恢复速度 | **最快** | 慢（重放所有命令） | **快**（RDB 加载 + 少量重放）|
| 文件大小 | 小（二进制+压缩） | 大（文本协议） | 中（RDB + 少量 AOF）|
| 内存消耗 | fork + COW 翻倍 | fork + COW 翻倍（重写时） | 同上 |
| 写性能影响 | 最小 | 中等（everysec） | 中等 |
| 适合场景 | 允许分钟级丢数据 | 不允许丢数据 | **生产推荐** |

---

## 4. 数据恢复与备份策略

### 4.1 恢复优先级

当同时存在 RDB 和 AOF 文件，Redis 优先加载 AOF：

```text
优先顺序：
AOF 文件存在 ──→ 加载 AOF（或混合持久化文件）
       ↓
AOF 文件不存在 ──→ 加载 RDB 文件（dump.rdb）
       ↓
RDB 也不存在 ──→ 空数据库启动

原因：AOF 数据完整性更高（秒级丢失 vs 分钟级丢失）
```

### 4.2 手动恢复步骤

```bash
# 1. 安全关闭 Redis
redis-cli shutdown     # 自动 SAVE + 关闭

# 2. 备份当前数据文件（以防恢复失败）
cp /usr/local/redis/data/dump.rdb /backup/dump_$(date +%Y%m%d).rdb.bak
cp /usr/local/redis/data/appendonly.aof /backup/aof_$(date +%Y%m%d).bak

# 3. 将备份文件复制到工作目录
cp /backup/redis/dump_20260101.rdb /usr/local/redis/data/dump.rdb

# 4. 启动 Redis
redis-server /usr/local/redis/redis.conf

# 5. 验证数据
redis-cli
> dbsize
> INFO keyspace
> GET key_name
```

### 4.3 备份策略

```bash
# 每日凌晨自动备份 RDB
crontab -e
# 每天 2:00 备份 RDB，保留 30 天
0 2 * * * cp /usr/local/redis/data/dump.rdb /backup/redis/dump_$(date +\%Y\%m\%d).rdb
0 2 * * * find /backup/redis -name "*.rdb" -mtime +30 -delete

# 备份脚本 redis_backup.sh
#!/bin/bash
BACKUP_DIR="/backup/redis"
DATE=$(date +%Y%m%d_%H%M%S)
REDIS_DATA_DIR="/usr/local/redis/data"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 触发 BGSAVE（确保生成最新 RDB）
redis-cli bgsave
sleep 5

# 复制 RDB
cp $REDIS_DATA_DIR/dump.rdb $BACKUP_DIR/dump_$DATE.rdb

# 复制 AOF（运行时复制可能不完整，建议停止后复制）
# cp $REDIS_DATA_DIR/appendonly.aof $BACKUP_DIR/aof_$DATE.aof

# 删除 30 天前的备份
find $BACKUP_DIR -name "*.rdb" -mtime +30 -delete

echo "Redis backup completed: $DATE"
```

> ⚠️ AOF 文件在运行时复制可能不完整（写入中的尾部截断）。AOF 的热备份建议通过 `redis-cli` 的 `BGREWRITEAOF` 触发重写后复制临时文件，或使用 `SLVAEOF` 从节点做备份。

### 4.4 AOF 文件修复

```bash
# 如果 AOF 文件损坏，使用修复工具
redis-check-aof --fix appendonly.aof

# 检查 RDB 文件
redis-check-rdb dump.rdb
```

---

## 5. 持久化方案对比与选型

### 5.1 方案决策矩阵

| 方案 | 数据安全性 | 恢复速度 | 磁盘占用 | 性能影响 | 推荐场景 |
|------|-----------|---------|---------|---------|----------|
| 仅 RDB | 丢最近一次快照后数据 | **最快** | **最小** | **最小** | 允许分钟级丢失，纯缓存场景 |
| 仅 AOF（everysec） | 丢 1 秒数据 | 最慢 | 最大 | 中 | 不允许数据丢失 |
| **RDB + AOF（混合）** | **丢 1 秒数据** | **快** | **中** | 中 | **生产环境推荐** |
| RDB + AOF（纯 AOF） | 丢 1 秒数据 | 慢 | 中 | 中 | 老版本 Redis（<4.0） |
| 关闭持久化 | 重启即丢 | — | 无 | **最小** | 纯缓存，数据可从 DB 重建 |

### 5.2 不同场景推荐

```text
场景 1：业务缓存（秒杀活动、Session）
  推荐：关闭持久化 或 仅 RDB
  原因：数据可重建，丢数据影响小

场景 2：实时排行榜（积分、热榜）
  推荐：RDB + AOF（混合持久化）
  原因：可容忍秒级丢失，需要快速恢复

场景 3：金融类业务（交易记录、订单）
  推荐：RDB + AOF（混合）+ 数据库双写
  原因：Redis 做缓存加速，MySQL 做最终持久化

场景 4：计数器（阅读量、点赞数）
  推荐：RDB + AOF（everysec）
  原因：允许秒级丢失，但不可接受分钟级丢失

场景 5：消息队列（Stream）
  推荐：RDB + AOF（混合）
  原因：消息丢失可能造成业务故障
```

> 💡 纯缓存场景（如 Session、验证码、临时 token）可关闭持久化或仅用 RDB，重启自动从 DB 重建。**生产者消费者模式中，Stream 内的消息推荐开启混合持久化，避免消息丢失。**

---

## 6. 内存淘汰策略

### 6.1 8 种淘汰策略

| 策略 | 说明 | 适用范围 | 推荐场景 |
|------|------|----------|----------|
| **noeviction** | 不淘汰，写入报错 `OOM command not allowed` | 所有 key | 默认策略，**生产不可用** |
| **allkeys-lru** | 全部 key 中淘汰最近最少使用 | 所有 key | **通用推荐，多数场景首选** |
| **allkeys-lfu** | 全部 key 中淘汰最不经常使用 | 所有 key | 访问频率差异明显 |
| **allkeys-random** | 全部 key 中随机淘汰 | 所有 key | 较少用 |
| **volatile-lru** | 过期 key 中淘汰最近最少使用 | 已设置 TTL 的 key | 数据过期可再次加载 |
| **volatile-lfu** | 过期 key 中淘汰最不经常使用 | 已设置 TTL 的 key | 同上但看频率 |
| **volatile-random** | 过期 key 中随机淘汰 | 已设置 TTL 的 key | 较少用 |
| **volatile-ttl** | 过期 key 中淘汰 TTL 最短（即将过期） | 已设置 TTL 的 key | 对过期时间敏感 |

**核心区别：**
```text
allkeys-*    → 淘汰范围是所有 key，无论是否设置过期时间
volatile-*  → 淘汰范围仅限于设置过 TTL 的 key
              未设置 TTL 的 key 永远不会被淘汰

LRU vs LFU：
  LRU（Least Recently Used）  → 淘汰最近最少访问的
  LFU（Least Frequently Used）→ 淘汰访问频率最低的

  区别示例：
  一个 key 昨天访问 1000 次，今天没访问
    LRU：今天可能被淘汰（最近没访问）
    LFU：不会被淘汰（历史频率高）
```

### 6.2 配置

```bash
# redis.conf
maxmemory 2gb                   # 最大可用内存（物理内存 70%-80%）
maxmemory-policy allkeys-lru    # 淘汰策略
maxmemory-samples 5             # LRU/LFU 采样数（越大越精确）

# 运行时修改
redis-cli CONFIG SET maxmemory 2gb
redis-cli CONFIG SET maxmemory-policy allkeys-lru
redis-cli CONFIG REWRITE
```

### 6.3 LRU 近似算法

Redis 的 LRU 不是精确 LRU，而是近似 LRU：

```text
精确 LRU（LinkedHashMap）：
  维护一个双向链表，每次访问移动到头部
  淘汰时从尾部删除
  → 内存开销大（需要额外的 prev/next 指针）

Redis 近似 LRU（采样法）：
  1. 从数据库中随机采样 N 个 key（N = maxmemory-samples）
  2. 淘汰其中最久未访问的 key
  3. 下次淘汰时再次采样

  N=5（默认）：每次采样 5 个 key
  N=10：采样 10 个 key，淘汰更精确，但 CPU 消耗高

  实践证明，N=5 时淘汰效果已接近精确 LRU
```

> 💡 `maxmemory-samples` 采样数越大，淘汰越接近精确 LRU，但 CPU 消耗也越高。默认 5 已在精确度和性能之间取得良好平衡。

---

## 7. 性能优化与安全配置

### 7.1 慢命令替代

| ❌ 慢操作 | 问题 | ✅ 替代方案 |
|----------|------|------------|
| `KEYS *` | 全量遍历，**阻塞 Redis** 数秒 | `SCAN 0 MATCH pattern COUNT 100` |
| `HGETALL`（大 Hash） | 全量返回，可能 O(n) | `HSCAN` 分批扫描 |
| `SMEMBERS`（大 Set） | 全量返回 | `SSCAN` 分批扫描 |
| `ZRANGE`（大 ZSet 全量） | 全量返回 | 加 LIMIT 限制 |
| `FLUSHALL` / `FLUSHDB` | 同步删除所有数据 | `SCAN + DEL` 或 `UNLINK` 异步 |
| `MONITOR` | 实时监控，**降低 50%+ 吞吐** | 仅调试用，生产禁用 |
| `DEL`（大 Key 超 1MB） | 同步删除可能阻塞 | `UNLINK` 异步删除 |
| `SORT`（大集合） | 会阻塞 | 业务层排序或 Lua 脚本 |

### 7.2 监控指标

```bash
# 内存监控
redis-cli INFO memory
# used_memory: 当前内存使用量
# used_memory_rss: 操作系统角度看的内存占用
# used_memory_peak: 历史峰值内存
# mem_fragmentation_ratio: 内存碎片率（>1.5 需重启）

# 命中率
redis-cli INFO stats
# keyspace_hits: 缓存命中次数
# keyspace_misses: 未命中次数
# 命中率 = hits / (hits + misses) × 100%

# 持久化状态
redis-cli INFO persistence
# rdb_last_save_time: 上次 RDB 时间
# rdb_bgsave_in_progress: 是否正在 BGSAVE
# aof_enabled: AOF 是否启用
# aof_last_rewrite_time_sec: 上次重写耗时

# 慢查询
redis-cli SLOWLOG GET 100
redis-cli SLOWLOG LEN
redis-cli SLOWLOG RESET

# 客户端连接
redis-cli CLIENT LIST
redis-cli INFO clients

# DB 统计
redis-cli INFO keyspace
# db0:keys=10000,expires=5000,avg_ttl=3600000
```

### 7.3 安全配置速查

| 安全措施 | 配置 | 备注 |
|----------|------|------|
| 密码认证 | `requirepass "YourStrongPassword!"` | 生产强制，不要使用弱密码 |
| 绑定 IP | `bind 127.0.0.1` 或内网 IP | 公网暴露的危险极大 |
| 禁用危险命令 | `rename-command FLUSHALL ""` | 设为空字符串完全禁用 |
| 禁用 KEYS | `rename-command KEYS ""` | 或改为不易猜到的名字 |
| 禁用 CONFIG | `rename-command CONFIG ""` | 防止运行时修改配置（可保留后改为不易猜的名字） |
| 修改端口 | `port 6380` | 避开默认 6379，减少自动扫描攻击 |
| 非 root 运行 | 创建 `redis` 系统用户 | 最小权限原则 |
| 内核参数 | `vm.overcommit_memory = 1` | 避免 fork 时内存不足 |
| 透明大页禁用 | `echo never > /sys/kernel/mm/transparent_hugepage/enabled` | 减少 fork 后 COW 的页分裂开销 |
| ACL（6.0+） | `ACL SETUSER` | 细粒度权限控制 |
| TLS/SSL（6.0+） | `tls-port` + 证书 | 传输层加密 |
| 文件权限 | `chmod 600 /etc/redis/redis.conf` | 配置文件仅 root 可读 |

### 7.4 其他性能优化

```text
1. 合理设置 key 过期
   避免大量 key 同时过期导致延迟飙升
   建议过期时间加随机偏移：TTL + random(0, 3600)

2. 批量操作用 Pipeline
   减少网络 RTT，吞吐可提升 5-10 倍

3. 多级缓存
   本地缓存（Caffeine）→ Redis 缓存 → 数据库
   热点数据本地缓存，减少 Redis 压力

4. 连接池
   使用 Lettuce 连接池（默认内置连接池）
   连接池大小建议：maxTotal = CPU × 2 + 1

5. 大 Key 拆分
   单个 String > 10MB → 考虑拆分或压缩
   单个 Hash > 1 万元素 → 考虑分桶
   单个 ZSet > 1 万元素 → 考虑分片

6. 内存碎片处理
   mem_fragmentation_ratio > 1.5 → 考虑重启
   或使用 `MEMORY PURGE`（尽力而为，不保证）

7. 避免频繁差集运算
   SDIFF / SUNION / SINTER 在集合很大时 O(n)，可能阻塞

8. 关闭不必要的特性
   不用的功能全部关闭：如未用发布订阅，不做额外配置
```

---

## 面试核心问题

**Q: RDB 和 AOF 的区别与选择？**

```text
RDB = 定时快照（二进制，文件小恢复快，可能丢分钟级数据）
AOF = 命令日志（可读，文件大恢复慢，最多丢 1 秒数据）

生产推荐：
  RDB + AOF 混合持久化（Redis 4.0+）
  → 兼具两者的优点：快速恢复 + 数据完整
```

**Q: BGSAVE 的写时复制（COW）原理？**

```text
fork 子进程 → 父子共享内存页 → 主进程修改某页时复制该页 →
子进程继续写原共享页到 RDB → 主进程在副本上修改

代价：大量写入时内存占用可能翻倍
解决方案：预留 50% 空闲内存，配置 vm.overcommit_memory=1
```

**Q: AOF 重写的过程？**

```text
1. fork 子进程
2. 子进程将当前内存转为最小写命令集 → 写入临时文件
3. 同时主进程将增量命令写入重写缓冲区
4. 子进程完成后 → 通知主进程
5. 主进程将缓冲区增量追加到临时文件
6. 原子 rename 替换旧 AOF 文件
```

**Q: allkeys-lru 和 volatile-lru 的区别？**

```text
allkeys-lru  → 所有 key 都可淘汰（包括未设置 TTL 的）
volatile-lru → 仅淘汰设置了 TTL 的 key，永久 key 永不淘汰

如果你的业务 "既要缓存又要持久"（部分 key 不能丢），选 volatile-*
如果全部是缓存数据，选 allkeys-lru（更彻底）
```

**Q: Redis 内存满载时写操作会怎样？**

```text
取决于 maxmemory-policy：
  noeviction（默认）→ 报错 OOM command not allowed
  其他策略 → 淘汰一个旧 key 后写入新 key
  建议：线上一定要配置 maxmemory + 淘汰策略
```

**Q: RDB 和 AOF 同时开启时，Redis 启动加载哪个？**

```text
先检查 AOF 文件是否存在：
  存在 → 加载 AOF（比 RDB 数据更新）
  不存在 → 加载 RDB

如果开启混合持久化，AOF 文件头部是 RDB 格式：
  先加载 RDB 前导 → 再加载 AOF 增量
```

> 🎯 持久化与数据安全的核心：不要信任任何单一机制。RDB 负责快速恢复和备份，AOF 负责数据完整性，混合持久化平衡两者。配合合理的备份策略、淘汰策略和安全配置，才能构建可靠的 Redis 生产环境。

