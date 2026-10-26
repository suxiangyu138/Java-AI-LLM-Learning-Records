# 07-HBase 性能调优与故障排查

> HBase 调优不是背参数，而是先定位瓶颈在哪一层：JVM/GC、MemStore、BlockCache、Compaction、Region 分布、HDFS。找错层，参数调得越多越糟。本篇给出一套"先测量、再归因、后调参"的方法，以及一张可对照的故障排查表。

---

## 目录

1. [调优方法论](#1-调优方法论)
2. [内存模型与配比](#2-内存模型与配比)
3. [JVM 与 GC 调优](#3-jvm-与-gc-调优)
4. [MemStore 调优](#4-memstore-调优)
5. [BlockCache 调优](#5-blockcache-调优)
6. [Compaction 调优](#6-compaction-调优)
7. [Region 与分裂调优](#7-region-与分裂调优)
8. [写入链路调优](#8-写入链路调优)
9. [读取链路调优](#9-读取链路调优)
10. [HDFS 侧配合](#10-hdfs-侧配合)
11. [故障排查表](#11-故障排查表)
12. [排查工具与命令](#12-排查工具与命令)
13. [压测方法](#13-压测方法)
14. [两个真实案例](#14-两个真实案例)
15. [小结](#15-小结)

---

## 1. 调优方法论

```text
第 1 步：定义问题
  是写延迟高、读延迟高、吞吐上不去，还是周期性抖动？
  是单 Region 慢，还是全集群慢？

第 2 步：分层定位
  客户端 → 网络 → RegionServer（RPC 队列/CPU）
  → JVM（GC/内存）→ MemStore/BlockCache
  → Compaction → HDFS（磁盘/网络）

第 3 步：量化基线
  记录正常时的 P50/P99、QPS、GC 停顿、Compaction 队列
  没有基线就没有"变慢"

第 4 步：单变量调整
  一次只改一个参数，观察 15-30 分钟
  记录调整前后的指标

第 5 步：回归与固化
  压测验证，写入配置基线，更新运维手册
```

最常见的错误是"看到延迟高就加内存/加机器"，而没有确认瓶颈到底在哪。例如：

```text
读延迟高 + CPU 低 + 磁盘 util 高
  → 瓶颈在磁盘 IO（BlockCache 不足或文件太多），加 CPU 无用

写延迟高 + GC 停顿长
  → 瓶颈在 JVM（堆太大或 MemStore 太多），加机器可能缓解但治标不治本

全集群吞吐上不去 + 单 RS CPU 低
  → 瓶颈在 Region 分布不均或客户端连接数不足
```

---

## 2. 内存模型与配比

RegionServer 的内存分三块：

```text
┌──────────────────────────────────────────────┐
│ JVM 堆                                        │
│  ├── MemStore 池（写缓冲）                     │
│  ├── BlockCache 堆内部分（INDEX/BLOOM/META）    │
│  └── 其他（RPC、对象、Region 元数据）           │
├──────────────────────────────────────────────┤
│ 堆外内存（Direct Memory）                       │
│  └── BucketCache（DATA 块）                    │
├──────────────────────────────────────────────┤
│ 操作系统页缓存（HDFS 读）                        │
└──────────────────────────────────────────────┘
```

配比建议：

| 场景 | MemStore | BlockCache（堆内+堆外） | 说明 |
|------|:--------:|:---------------------:|------|
| 写多读少（日志、时序） | 50-60% | 25-35% | 减少 flush 阻塞 |
| 读多写少（画像、配置） | 25-30% | 55-65% | 提高缓存命中 |
| 读写均衡 | 40% | 40% | 默认起点 |

计算示例（128GB 内存节点）：

```text
堆：32GB
  MemStore：32 × 0.4 = 12.8GB
  BlockCache 堆内：32 × 0.1 = 3.2GB（INDEX/BLOOM/META）
  其他：16GB（RPC、对象、Region）
堆外 BucketCache：64GB
系统预留：32GB（OS、HDFS、页缓存）

总计：32 + 64 + 32 = 128GB
```

> **警告**：堆外内存不计入 JVM 堆，但计入物理内存。BucketCache 配置过大（堆 + 堆外 + 系统 > 物理内存）会触发 OS OOM Killer，表现为 RS 进程"无故消失"。

---

## 3. JVM 与 GC 调优

### 3.1 参数基线

```bash
export HBASE_REGIONSERVER_OPTS="
  -Xms32g -Xmx32g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:InitiatingHeapOccupancyPercent=45
  -XX:G1NewSizePercent=5
  -XX:G1MaxNewSizePercent=30
  -XX:+ParallelRefProcEnabled
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/data/hbase/dump
  -Xlog:gc*:file=/data/hbase/logs/gc.log:time,uptime,level,tags:filecount=10,filesize=100m
"
```

### 3.2 为什么是 G1

```text
CMS：已废弃（JDK 14 移除），不再考虑
Parallel：停顿不可控，大堆下秒级停顿
G1：可设定目标停顿，适合 16-64GB 堆
ZGC：JDK 17 可用，停顿亚毫秒，但吞吐与内存开销略高
  → 超大堆（> 64GB）且延迟极敏感时可评估
```

### 3.3 常见 GC 问题

| 现象 | 根因 | 处理 |
|------|------|------|
| Full GC 频繁 | 堆太小、内存泄漏、Humongous 对象 | 加堆、查泄漏、调 G1HeapRegionSize |
| 年轻代 GC 频繁 | 新生代太小、分配速率高 | 调 G1NewSizePercent |
| GC 停顿 > 1s | 堆大、老年代碎片、Mixed GC 不及时 | 提前 IHOP、限制堆大小 |
| 堆外 OOM | BucketCache 过大 | 降低 offheap 配置 |
| `OutOfMemoryError: Metaspace` | 类加载泄漏（Coprocessor 热部署） | 加 Metaspace、排查动态类生成 |

G1 关键参数说明：

```text
MaxGCPauseMillis：目标停顿，不是硬保证
InitiatingHeapOccupancyPercent：老年代占比达到该值启动并发标记
  调低 → 更早回收，减少 Full GC，但 CPU 开销上升
G1HeapRegionSize：大对象（> 区域一半）进 Humongous
  KeyValue 很大的宽表场景要调大区域，避免 Humongous 分配
```

---

## 4. MemStore 调优

### 4.1 核心参数

| 参数 | 默认 | 作用 | 调整方向 |
|------|------|------|---------|
| `hbase.hregion.memstore.flush.size` | 128MB | 单 MemStore 刷写阈值 | 写多调大（256MB） |
| `hbase.hregion.memstore.block.multiplier` | 4 | Region 内总上限倍数 | 一般不动，调大缓解阻塞但增加内存风险 |
| `hbase.regionserver.global.memstore.size` | 0.4 | RS 堆占比 | 写多调 0.5-0.6 |
| `hbase.regionserver.global.memstore.size.lower.limit` | 0.95 | 全局刷写触发 | 默认即可 |
| `hbase.hregion.memstore.mslab.enabled` | true | 本地分配缓冲 | 保持开启，减少碎片 |
| `hbase.hregion.memstore.mslab.chunksize` | 2MB | MSLAB 块大小 | 大 KeyValue 场景调大 |

### 4.2 列族数量与 MemStore

```text
每个列族独立 MemStore：
  5 个列族 × 128MB = 640MB（同一 Region）
  → 很容易触发 Region 级阻塞

结论：
  列族数量是 MemStore 内存的乘数
  减少列族是最有效的 MemStore 优化
```

### 4.3 刷写调优

```text
刷写慢的原因：
  ① HDFS 写入带宽不足
  ② 单次刷写数据量大（MemStore 太大）
  ③ 磁盘 IO 被 Compaction 抢占

优化：
  控制 MemStore 大小（128-256MB）
  限制 Compaction 带宽（hbase.regionserver.compaction.throttle）
  错峰执行 major compaction
  使用本地盘（非网络存储）
```

---

## 5. BlockCache 调优

### 5.1 三种模式

```text
LRUBlockCache（纯堆内）：
  hfile.block.cache.size = 0.4
  简单，但高命中率下老年代压力大

BucketCache（纯堆外/文件）：
  hbase.bucketcache.ioengine = offheap
  hbase.bucketcache.size = 65536（MB）
  减少 GC，适合大缓存

CombinedBlockCache（默认）：
  INDEX/BLOOM/META → 堆内 LRU
  DATA → 堆外 Bucket
```

### 5.2 配置示例

```xml
<property>
  <name>hbase.bucketcache.ioengine</name>
  <value>offheap</value>
</property>
<property>
  <name>hbase.bucketcache.size</name>
  <value>65536</value>
</property>
<property>
  <name>hfile.block.cache.size</name>
  <value>0.1</value>
</property>
<property>
  <name>hbase.bucketcache.combinedcache.enabled</name>
  <value>true</value>
</property>
```

### 5.3 命中率优化

```text
观察：RegionServer UI 的 blockCacheHitRatio

低命中率的原因：
  ① 缓存太小（热点数据 > 缓存容量）
  ② 访问模式随机（无热点）
  ③ 扫描型查询污染缓存（大 Scan 把数据挤出去）
  ④ 块太大（64KB 块，读一个 key 也占整块）

优化：
  增大 BucketCache
  调整块大小（随机读多 → 16-32KB）
  避免全表 Scan
  冷数据列族关闭 BLOCKCACHE
```

> **提示**：Scan 会污染缓存。如果业务有大量范围扫描，考虑给该列族设置 `BLOCKCACHE => false`，或用单独的列族隔离冷热数据。

---

## 6. Compaction 调优

### 6.1 参数

| 参数 | 默认 | 说明 |
|------|------|------|
| `hbase.hstore.compaction.min` | 3 | minor 触发文件数 |
| `hbase.hstore.compaction.max` | 10 | 单次最多合并数 |
| `hbase.hstore.compaction.ratio` | 1.2 | 大小比例阈值 |
| `hbase.hregion.majorcompaction` | 604800000 | major 周期，0 为关闭 |
| `hbase.regionserver.thread.compaction.small` | 1 | 小合并线程 |
| `hbase.regionserver.thread.compaction.large` | 1 | 大合并线程 |
| `hbase.hstore.blockingStoreFiles` | 16 | 文件数上限，超限阻塞写 |
| `hbase.regionserver.compaction.throttle` | 无 | Compaction 带宽限流 |

### 6.2 调优策略

```text
写多场景：
  增加 compaction 线程（large=4，small=4）
  适当提高 compaction.min（减少频繁小合并）
  开启限流，避免 Compaction 吃满 HDFS 带宽

读多场景：
  降低 compaction.min（更快减少文件数）
  提高 BlockCache，减少读放大

Region 很大（> 50GB）：
  考虑 StripeCompactionPolicy
  或提前分裂，避免单 Store 文件过多

时序数据：
  考虑 DateTieredCompactionPolicy
  按时间分层，旧数据少合并
```

### 6.3 关闭自动 major

```xml
<property>
  <name>hbase.hregion.majorcompaction</name>
  <value>0</value>
</property>
```

改为低峰期调度：

```bash
# 每周日凌晨 2 点对指定表做 major compaction
0 2 * * 0 /opt/hbase/bin/hbase shell -n <<'EOF'
major_compact 'app:user_profile'
EOF
```

> **警告**：关闭自动 major 后，墓碑与过期版本不会自动清理。必须保证有定时任务，否则磁盘会持续增长，且读放大越来越严重。

---

## 7. Region 与分裂调优

```text
Region 大小的权衡：
  < 1GB：文件多、管理开销大、分裂频繁
  10-30GB：推荐区间
  > 50GB：分裂与恢复时间长，单点影响大

Region 数量：
  单 RS 20-200 个
  超过 200：MemStore 总量、文件句柄、心跳开销上升

分裂策略：
  预分区后建议 ConstantSizeRegionSplitPolicy（固定阈值，行为可预测）
  前缀有业务含义时用 KeyPrefixRegionSplitPolicy
  新表初期用 IncreasingToUpperBoundRegionSplitPolicy
```

分裂后的抖动：

```text
分裂瞬间：
  父 Region 短暂停止写入
  子 Region 重新分配、打开、Compaction

缓解：
  预分区，避免运行期频繁分裂
  低峰期手动 split
  控制单次分裂数量
```

---

## 8. 写入链路调优

### 8.1 客户端

```java
// 批量写：BufferedMutator
BufferedMutatorParams params = new BufferedMutatorParams(tableName)
        .writeBufferSize(8 * 1024 * 1024);
BufferedMutator mutator = connection.getBufferedMutator(params);

// 关闭自动刷写（HTable 旧 API）
table.setAutoFlush(false, false);

// 可接受少量丢失时降低持久性
put.setDurability(Durability.ASYNC_WAL);
```

批量大小建议：

```text
每批 100-1000 条，或 1-8MB
过小 → RPC 次数多
过大 → 单次请求超时、内存压力
```

### 8.2 服务端

| 参数 | 默认 | 调整 |
|------|------|------|
| `hbase.regionserver.handler.count` | 30 | 写多调到 64-128（注意线程上下文切换） |
| `hbase.ipc.server.max.callqueue.size` | 大 | 队列满会导致拒绝 |
| `hbase.regionserver.hlog.syncdfs.verbose` | false | 排查 WAL 延迟时打开 |
| `hbase.regionserver.wal.codec` | IndexedWALEditCodec | 保持默认 |

### 8.3 写热点治理

```text
识别：
  单 RS 的 writeRequestsCount 远高于其他
  单 Region 的写入 QPS 占集群大部分

处理：
  RowKey 加盐/哈希/反转
  预分区
  业务上拆分热点实体（如大客户分片）
  热点行加 Redis 缓冲，异步落 HBase
```

---

## 9. 读取链路调优

```text
客户端：
  get 代替 scan
  批量 get（List<Get>）
  scan 设 caching（100-1000）、batch、limit
  用 startRow/stopRow 缩小范围
  Filter 只用于必要过滤，不作为索引

表设计：
  Bloom Filter 开启（点查）
  FAST_DIFF 编码
  冷热列族分离
  块大小按访问模式调（随机读 16-32KB，扫描 64-128KB）

服务端：
  BlockCache 足够
  控制 StoreFile 数（< 10）
  短路读开启（RS 与 DN 同机）
```

读延迟分解：

```text
一次 get 的耗时组成：
  RPC 网络（0.1-1ms）
  Region 定位（缓存命中时忽略）
  MemStore 查找（微秒）
  BlockCache 查找（微秒）
  HDFS 读块（1-10ms，未命中缓存时）
  多文件合并（取决于文件数）

优化优先级：
  提高缓存命中 > 减少文件数 > 缩短 RowKey > 调 RPC
```

---

## 10. HDFS 侧配合

```text
① 副本数
   热数据 3 副本；冷数据可降为 2（需评估可靠性）

② 短路读
   dfs.client.read.shortcircuit=true
   RS 与 DN 同机时生效，读延迟显著下降

③ DataNode 线程数
   dfs.datanode.max.transfer.threads = 16384

④ 磁盘
   多盘 JBOD 优于单一大 RAID（并行 IO）
   SSD/NVMe 提升 Compaction 与随机读

⑤ 网络
   10GbE 起步；Compaction 与写入共享带宽，需限流

⑥ 机架感知
   dfs.datanode.rack 配置正确，副本跨机架
```

---

## 11. 故障排查表

| 现象 | 可能原因 | 排查动作 | 处理 |
|------|---------|---------|------|
| 写延迟飙升，日志 `Blocking updates` | MemStore 满、文件数超限、Compaction 积压 | 看 memstoreSize、storeFileCount、compactionQueue | 扩容/调参/降低写入速率 |
| `RegionTooBusyException` | 单 Region 写入过载 | 看 Region 请求分布 | 打散 RowKey、分裂 Region |
| `NotServingRegionException` | Region 迁移中/RS 宕机 | 看 Master UI 的 RIT | 等待恢复，必要时 hbck2 |
| RIT 长时间不结束 | 分配卡住、HDFS 异常 | Master 日志、HDFS 健康 | 手动 assign/unassign，hbck2 |
| RS 进程消失 | OS OOM Killer（堆外过大） | dmesg、系统日志 | 降低 BucketCache，加内存 |
| Full GC 频繁 | 堆不足、泄漏、Humongous | GC 日志、jmap histo | 调堆、查泄漏、调 G1 |
| 读延迟高、CPU 低 | 磁盘 IO 瓶颈、缓存命中低 | iostat、blockCacheHitRatio | 加缓存、减文件数、换 SSD |
| 单 RS CPU 打满 | 热点 Region、大量 Scan | Top Region 请求 | 迁移 Region、优化查询 |
| 磁盘持续增长 | 版本过多、墓碑、快照 | 表属性、快照列表 | 调 VERSIONS/TTL、major compact、删快照 |
| 数据读不到但写入成功 | 时间戳未来、墓碑覆盖 | 检查 timestamp | 用服务端时间，修复数据 |
| `hbase:meta` 异常 | 异常关机、HDFS 故障 | hbck2 检查 | 修复或从备份恢复 |
| 复制延迟大 | 网络、目标集群慢 | replication 指标 | 限流、扩容目标集群 |

---

## 12. 排查工具与命令

### 12.1 HBase 自带

```bash
# 集群状态
hbase shell
status 'detailed'

# Region 分布
hbase hbck -details
hbase hbck2 app:user_profile -details

# 表信息
describe 'app:user_profile'

# 手动操作
flush 'app:user_profile'
compact 'app:user_profile'
major_compact 'app:user_profile'
split 'regionName', 'splitKey'
move 'encodedRegionName', 'targetServer'
balancer
```

### 12.2 JVM 排查

```bash
# 线程栈（找阻塞、死锁、RPC 队列）
jstack -l <pid> > /tmp/jstack.txt

# 堆直方图（找大对象）
jmap -histo:live <pid> | head -50

# 堆转储（OOM 后分析）
jmap -dump:format=b,file=/tmp/heap.hprof <pid>

# GC 实时观察
jstat -gcutil <pid> 1000 30

# 原生内存
pmap -x <pid> | sort -k3 -n | tail
```

### 12.3 系统层

```bash
# 磁盘 IO
iostat -x 1 10

# 网络
sar -n DEV 1 10

# 内存
free -h
vmstat 1

# 进程打开的句柄
lsof -p <pid> | wc -l
```

### 12.4 日志关键字

```text
Blocking updates for region
RegionTooBusyException
NotServingRegionException
Region was in transition
Failed to open region
Compaction failed
HDFS write pipeline
Full GC
OutOfMemoryError
```

---

## 13. 压测方法

```text
目标：在变更前量化容量，变更后验证效果

工具：
  YCSB（Yahoo Cloud Serving Benchmark）：HBase 标准压测工具
  自研压测程序：模拟真实读写比例

场景设计：
  ① 纯写：100% put，测写入上限
  ② 纯读：100% get（热点/随机），测读延迟
  ③ 混合：读写 7:3 或 5:5，测均衡表现
  ④ Scan：范围扫描，测缓存污染与 IO
  ⑤ 故障注入：杀 RS、断网，测恢复时间

指标：
  QPS、P50/P95/P99 延迟、错误率
  RS CPU/内存/磁盘、GC 停顿
  Compaction 队列、StoreFile 数
  恢复时间（RTO）

注意：
  压测数据量与生产同量级，否则 Region 分布不真实
  压测前预分区，避免单 Region 瓶颈
  压测时观察 GC 与 Compaction，不只看 QPS
```

---

## 14. 两个真实案例

### 14.1 案例一：写入全压一台机器

```text
现象：
  订单表上线一周，写入延迟从 5ms 涨到 500ms
  一台 RS 的 CPU 90%，其他 RS  CPU 10%
  Region 数：全表只有 3 个 Region

根因：
  RowKey = 订单号（时间前缀，单调递增）
  建表未预分区
  → 所有新订单写到最后一个 Region

处理：
  ① 建新表，RowKey 改为 hash(orderId) 前 2 位 + orderId
  ② 预分区 32 个
  ③ 双写迁移，切读，下线旧表
  ④ 写入延迟回到 8ms，RS CPU 均衡

教训：
  时间前缀 + 不预分区 = 必然热点
```

### 14.2 案例二：读延迟周期性抖动

```text
现象：
  每天凌晨 3 点读延迟从 10ms 涨到 300ms，持续 20 分钟
  白天正常

根因：
  自动 major compaction 在凌晨触发
  多个大 Region 同时全量合并，HDFS 带宽打满

处理：
  ① 关闭自动 major（hbase.hregion.majorcompaction=0）
  ② 按表分级，低峰期分批调度 major_compact
  ③ 开启 compaction 限流
  ④ 抖动消失，凌晨延迟稳定在 15ms

教训：
  major compaction 是 IO 大事件，必须可控调度
```

---

## 15. 小结

| 问题 | 答案 |
|------|------|
| 调优第一步 | 分层定位瓶颈，不盲目加参数 |
| 内存三块 | 堆内 MemStore、堆内 BlockCache、堆外 BucketCache |
| GC 选型 | G1 为主，超大堆评估 ZGC |
| MemStore 优化 | 减少列族、控制大小、错峰刷写 |
| BlockCache 优化 | 命中率优先，冷数据关缓存，Scan 不污染 |
| Compaction 优化 | 线程数、限流、关闭自动 major 改调度 |
| Region 优化 | 10-30GB、预分区、单 RS 20-200 个 |
| 写优化 | 批量、ASYNC_WAL、打散 RowKey |
| 读优化 | 缓存、Bloom、减少文件、批量 get |
| 排查三件套 | HBase UI + jstack/jstat + iostat |

**下一模块**：[08-HBase Java客户端与Spring集成](08-HBase Java客户端与Spring集成.md)　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
