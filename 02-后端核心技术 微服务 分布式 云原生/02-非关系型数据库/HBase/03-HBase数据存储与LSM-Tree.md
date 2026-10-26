# 03-HBase 数据存储与 LSM-Tree

> HBase 把"随机写"改造成"顺序写"：先追加 WAL，再写内存 MemStore，攒够了一批顺序刷成不可变的 HFile，后台再合并。这套 LSM-Tree 结构换来了极高的写吞吐，代价是读放大与 Compaction 开销。理解三种放大的来源，才能理解后面所有调优参数为什么存在。

---

## 目录

1. [为什么不是 B+ 树](#1-为什么不是-b-树)
2. [LSM-Tree 核心思想](#2-lsm-tree-核心思想)
3. [三种放大](#3-三种放大)
4. [HBase 的存储层次](#4-hbase-的存储层次)
5. [WAL：预写日志](#5-wal预写日志)
6. [MemStore：内存写缓冲](#6-memstore内存写缓冲)
7. [HFile：磁盘文件结构](#7-hfile磁盘文件结构)
8. [KeyValue 内部格式](#8-keyvalue-内部格式)
9. [Compaction：合并与清理](#9-compaction合并与清理)
10. [删除与墓碑](#10-删除与墓碑)
11. [Bloom Filter 与块索引](#11-bloom-filter-与块索引)
12. [编码与压缩](#12-编码与压缩)
13. [配置速查](#13-配置速查)
14. [小结](#14-小结)

---

## 1. 为什么不是 B+ 树

关系数据库用 B+ 树，HBase 用 LSM-Tree，根源是两种存储介质的写特性不同：

```text
B+ 树（MySQL InnoDB）：
  写入 = 找到页 → 修改页 → 可能分裂 → 随机写磁盘
  随机写对机械盘是灾难，对 SSD 也有写放大
  优势：读路径短（3-4 层）、范围扫描有序、原地更新

LSM-Tree（HBase / RocksDB / Cassandra）：
  写入 = 追加日志 → 写内存 → 批量顺序落盘
  顺序写吞吐接近磁盘带宽上限
  代价：读要查多层、后台合并消耗 IO、空间放大
```

一句话对比：

| 维度 | B+ 树 | LSM-Tree |
|------|-------|----------|
| 写路径 | 原地随机写 | 追加顺序写 |
| 写吞吐 | 受随机 IO 限制 | 接近顺序带宽 |
| 读路径 | 一次定位 | 多层查找 + 合并 |
| 更新 | 原地覆盖 | 追加新版本 |
| 删除 | 原地删除 | 写墓碑，延迟清理 |
| 空间 | 紧凑 | 短时间放大（多版本/多文件） |
| 适合 | 读多写少、事务 | 写多读少、海量数据 |

> **要点**：LSM 不是"更先进的 B+ 树"，而是为"写吞吐优先 + 顺序存储"做的取舍。读放大与 Compaction 是它的固有成本，不是 bug。

---

## 2. LSM-Tree 核心思想

LSM（Log-Structured Merge-Tree）由 Patrick O'Neil 等人 1996 年提出，核心是三层：

```text
① 内存层（C0）：可变的有序结构（MemStore，跳表）
     所有写入先落这里，天然有序

② 磁盘层（C1..Cn）：不可变的有序文件（HFile）
     MemStore 满了 → 冻结 → 刷成新文件
     文件按代际分层，越新的文件越小

③ 合并（Compaction）：
     后台把多个文件合并成一个更大的有序文件
     合并时顺带清理过期版本、墓碑、重复 key
```

写入的完整路径（概念级）：

```text
put(row, cf, qualifier, value)
  → 追加到 WAL（顺序写，保证持久性）
  → 插入 MemStore 的跳表（内存有序）
  → 返回成功

MemStore 达到阈值
  → 冻结当前 MemStore（不再接受写）
  → 新开一个 MemStore 继续服务
  → 后台把冻结的 MemStore 排序刷成 HFile（顺序写 HDFS）
  → 多个 HFile 达到条件后触发 Compaction
```

读取的路径（概念级）：

```text
get(row)
  → 查 MemStore（最新数据）
  → 查 BlockCache（热点块）
  → 依次查 HFile（从新到旧），用 Bloom Filter 跳过不含该 row 的文件
  → 合并多版本，返回最新
```

---

## 3. 三种放大

LSM 的所有性能问题都可以归到三种放大上，这也是调优的坐标系：

### 3.1 写放大（Write Amplification）

```text
定义：实际写入磁盘的字节数 / 用户写入的字节数

来源：
  ① WAL：每次写都追加一份
  ② MemStore 刷写：数据从内存写到 HFile（1 次）
  ③ Compaction：同一份数据被反复合并（N 次）

估算：
  分层合并（size-tiered，每层放大 10 倍）
  写放大 ≈ 层数 × 每层倍数
  3 层 → 约 10-30 倍

影响：
  磁盘带宽成为瓶颈，Compaction 跟不上写入 → 写阻塞
```

### 3.2 读放大（Read Amplification）

```text
定义：一次读实际读取的块数 / 理论上需要的块数

来源：
  ① 多个 HFile 都要查（新到旧）
  ② 每个 HFile 要查索引 + 数据块
  ③ 多版本需要合并

缓解：
  Bloom Filter（跳过不含目标 row 的文件）
  块索引（二分定位到块）
  BlockCache（热点块常驻内存）
  Compaction（减少文件数量）
```

### 3.3 空间放大（Space Amplification）

```text
定义：磁盘占用 / 逻辑数据大小

来源：
  ① 多版本（VERSIONS > 1）
  ② 墓碑未清理（删除标记占空间）
  ③ 同一 key 在多个 HFile 中重复

缓解：
  VERSIONS = 1（不需要历史时）
  TTL 自动过期
  Major Compaction 真正清理
```

> **提示**：三种放大互相制约。增加 Compaction 频率能降读放大与空间放大，但抬高写放大；增大 MemStore 能降写放大，但增加恢复时间与内存压力。调优就是在这三者之间找平衡。

---

## 4. HBase 的存储层次

把 LSM 映射到 HBase 的具体组件：

```text
用户写入
   │
   ▼
┌─────────────────────────────────────────────┐
│ WAL（HLog）                                  │
│   RegionServer 级别共享，顺序追加              │
│   保证 RegionServer 宕机后内存数据可恢复        │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│ MemStore（每个 Region 的每个列族一个）          │
│   ConcurrentSkipListMap<KeyValue>            │
│   有序、可并发读写、达到阈值后冻结               │
└──────────────────┬──────────────────────────┘
                   │ flush
                   ▼
┌─────────────────────────────────────────────┐
│ StoreFile / HFile（HDFS 上的不可变文件）        │
│   多个文件按代际存在                           │
└──────────────────┬──────────────────────────┘
                   │ compaction
                   ▼
┌─────────────────────────────────────────────┐
│ 合并后的大 HFile（数量减少、有序、无冗余）        │
└─────────────────────────────────────────────┘
```

几个容易混淆的术语：

| 术语 | 含义 |
|------|------|
| Store | 一个 Region 内的一个列族，对应一个 MemStore + 一组 StoreFile |
| StoreFile | HFile 在 HBase 中的逻辑称呼，一个 HFile 就是一个 StoreFile |
| HFile | 磁盘文件格式，HBase 3.0 使用 HFile v3 |
| HLog | WAL 的旧称，代码与配置中仍大量使用 |

---

## 5. WAL：预写日志

WAL（Write-Ahead Log）解决"内存数据在宕机时丢失"的问题：

```text
写入顺序（严格）：
  ① 追加 WAL（HDFS 顺序写）
  ② 写入 MemStore
  ③ 返回客户端成功

为什么先 WAL 后 MemStore：
  MemStore 在内存，进程崩溃即丢
  WAL 在 HDFS，多副本持久化
  恢复时重放 WAL → 重建 MemStore
```

### 5.1 WAL 的粒度

```text
一个 RegionServer 一个 WAL（默认）：
  所有 Region 的写入共用一个 WAL 文件
  优点：文件少、顺序写好
  缺点：故障时 WAL 包含所有 Region 的数据，需要 split

可选：一个 Region 一个 WAL（hbase.regionserver.wal.provider）
  优点：故障恢复只需重放相关 Region
  缺点：文件数量多，小文件压力大
```

### 5.2 WAL 同步策略

| 策略 | 行为 | 适用 |
|------|------|------|
| `SYNC_WAL` | 每次写同步到 HDFS（默认） | 不丢数据，延迟最高 |
| `FSYNC_WAL` | 每次写 fsync | 最严格，性能最差 |
| `ASYNC_WAL` | 异步刷盘，可能丢最近若干条 | 可接受少量丢失的高吞吐场景 |
| `SKIP_WAL` | 不写 WAL | 仅批量导入等可重放场景 |

```xml
<property>
  <name>hbase.regionserver.hlog.syncdfs.verbose</name>
  <value>false</value>
</property>
```

写入时可在 Put 上单独指定：

```java
Put put = new Put(rowKey);
put.setDurability(Durability.SKIP_WAL);
```

> **警告**：`SKIP_WAL` 只适合"数据可重新生成"的场景（如从 Kafka 重放）。用它存业务数据，RS 宕机会直接丢数据。

### 5.3 WAL 的滚动与清理

```text
滚动（roll）时机：
  WAL 文件达到 hbase.regionserver.hlog.blocksize * rollMultiplier
  （默认 95% 的 HDFS 块大小）
  或达到时间上限

清理：
  WAL 中的数据刷进 HFile 后，旧 WAL 进入 oldWALs
  Master 定期清理（hbase.master.logcleaner.ttl，默认 10 分钟）
  复制（replication）未完成的 WAL 不会被提前删除
```

---

## 6. MemStore：内存写缓冲

MemStore 是每个 Store（Region × 列族）独立的内存有序结构：

```text
数据结构：ConcurrentSkipListMap<Cell, Cell>
  有序：按 RowKey → ColumnFamily → Qualifier → Timestamp 排序
  并发：跳表支持无锁读、细粒度写

为什么用跳表而不是红黑树：
  跳表实现简单、并发友好、范围查询天然有序
```

### 6.1 刷写（Flush）触发条件

```text
① 单个 MemStore 达到 hbase.hregion.memstore.flush.size（默认 128MB）
② Region 内所有 MemStore 总和达到
   hbase.hregion.memstore.block.multiplier × flush.size（默认 4 倍 = 512MB）
   → 触发后阻塞写入，强制刷写
③ RegionServer 所有 MemStore 总和达到
   hbase.regionserver.global.memstore.size（默认堆的 40%）
   → 触发全局刷写，可能阻塞
④ WAL 文件数量达到 hbase.regionserver.max.logs（默认 32）
⑤ 手动 flush（shell 命令 flush）
```

### 6.2 刷写过程

```text
① 冻结当前 MemStore（新的写入进入新 MemStore）
② 冻结的 MemStore 按 key 排序
③ 顺序写成 HFile（HDFS）
④ 记录 WAL 的刷写位置（可清理旧 WAL）
⑤ 释放冻结 MemStore 内存

关键：刷写是后台异步的，不阻塞用户写入（除非触发 blocking）
```

### 6.3 MemStore 与写入阻塞

写阻塞是 HBase 最典型的性能事故：

```text
现象：put 延迟突然飙升，日志出现 "Blocking updates"
原因：
  MemStore 太多 / 刷写跟不上 / Compaction 积压
  → 内存达到上限 → 写入被阻塞等待刷写

排查：
  看 RegionServer 的 memstoreSize、flushQueueLength
  看 HDFS 写入带宽是否打满
  看是否大量小文件导致 Compaction 追不上

缓解：
  增大 MemStore 上限（有内存风险）
  减少列族数量（每列族独立 MemStore）
  预分区（避免单 Region 过热）
  控制写入速率或批量写
```

---

## 7. HFile：磁盘文件结构

HFile 是 HBase 的磁盘存储格式，v3 自 0.98 起使用。结构从尾到头：

```text
┌─────────────────────────────┐
│ Trailer（固定长度）           │  ← 文件末尾，记录各块偏移
├─────────────────────────────┤
│ Meta Index（元数据索引）       │
├─────────────────────────────┤
│ Bloom Filter（可选）          │
├─────────────────────────────┤
│ Bloom Index（布隆索引）        │
├─────────────────────────────┤
│ Root Index（根索引）           │  ← 顶层索引，指向中间索引
├─────────────────────────────┤
│ Intermediate Index（中间索引） │  ← 大文件才有
├─────────────────────────────┤
│ Data Blocks（数据块）          │  ← 实际 KeyValue，默认 64KB 一块
├─────────────────────────────┤
│ ...                          │
├─────────────────────────────┤
│ Data Block Index（块索引）     │  ← 每个数据块的首 key + 偏移
├─────────────────────────────┤
│ Scanned Block Section        │
├─────────────────────────────┤
│ Load-on-open Section          │  ← 打开文件时加载到内存
└─────────────────────────────┘
```

读取一个 key 的过程：

```text
① 读 Trailer（定位各区域）
② 查 Root Index → 定位到目标 Data Block
③ 读 Data Block（先查 BlockCache，未命中读 HDFS）
④ 在块内二分查找 KeyValue
⑤ 若有多版本，继续读相邻块
```

关键参数：

| 参数 | 默认 | 作用 |
|------|------|------|
| `hbase.hfile.blocksize` / `BLOCKSIZE` | 64KB | 数据块大小，影响索引粒度与缓存命中 |
| `hbase.hregion.max.filesize` | 10GB（2.x 默认策略下动态） | Region 分裂阈值 |
| `hbase.hstore.compaction.min` | 3 | 触发 minor compaction 的最少文件数 |
| `hbase.hstore.compaction.max` | 10 | 单次 compaction 最多合并文件数 |

块大小的权衡：

- 块小 → 索引更细、随机读更精准，但索引占用内存更多。
- 块大 → 顺序扫描效率高，但随机读会多读无用数据。

---

## 8. KeyValue 内部格式

HFile 里存的是 KeyValue（Cell）序列，每条的布局：

```text
┌──────────────┬─────────────────────────────────────────┐
│ Key Length   │ 4 字节                                    │
├──────────────┼─────────────────────────────────────────┤
│ Value Length │ 4 字节                                    │
├──────────────┼─────────────────────────────────────────┤
│ RowKey       │ 变长（带 2 字节长度前缀）                    │
├──────────────┼─────────────────────────────────────────┤
│ Family       │ 变长（带 1 字节长度前缀）                    │
├──────────────┼─────────────────────────────────────────┤
│ Qualifier    │ 变长（带 2 字节长度前缀）                    │
├──────────────┼─────────────────────────────────────────┤
│ Timestamp    │ 8 字节（毫秒）                             │
├──────────────┼─────────────────────────────────────────┤
│ Type         │ 1 字节（Put/Delete/DeleteColumn/DeleteFamily）│
├──────────────┼─────────────────────────────────────────┤
│ Value        │ 变长                                      │
└──────────────┴─────────────────────────────────────────┘
```

由此得出几个设计约束：

1. **RowKey 越短越好**：它会在每个 Cell 里重复出现一次。1000 万行、每行 5 列，RowKey 从 100 字节降到 20 字节，直接省下 4GB。
2. **列族名越短越好**：每个 Cell 都带列族名，`info` 比 `user_information` 更省。
3. **Qualifier 也是成本**：列名同样逐条存储，长列名在宽表里放大明显。

---

## 9. Compaction：合并与清理

Compaction 是 LSM 的后台整理过程，把多个 HFile 合并成更少、更大的文件。

### 9.1 Minor Compaction

```text
范围：选取部分小文件（通常是相邻的、大小相近的）
行为：合并成一个文件
不清理：过期版本、墓碑（除非满足条件）
目的：控制文件数量，降低读放大

触发：
  文件数 ≥ hbase.hstore.compaction.min（默认 3）
  由压缩策略选择合并哪些文件
```

### 9.2 Major Compaction

```text
范围：一个 Store 下的所有 HFile
行为：合并成一个文件
清理：所有过期版本、TTL 过期数据、墓碑
目的：真正回收空间、消除删除标记

触发：
  默认每 7 天（hbase.hregion.majorcompaction，604800000ms）
  或手动 major_compact 命令
  或 Region 分裂/合并后
```

> **警告**：Major Compaction 会重写整个 Store，IO 与 CPU 开销巨大。生产上通常关闭自动 major（`hbase.hregion.majorcompaction=0`），改在业务低峰期用 cron 调度 `major_compact`，避免随机时间点撞上写入高峰。

### 9.3 Compaction 策略

| 策略 | 思路 | 适用 |
|------|------|------|
| `RatioBasedCompactionPolicy` | 按文件大小比例选（老策略） | 兼容 |
| `ExploringCompactionPolicy` | 枚举组合，选"性价比最高"的一组 | 2.x 默认 |
| `StripeCompactionPolicy` | 大 Region 切成多个 stripe 分别合并 | Region 很大、写倾斜 |
| `DateTieredCompactionPolicy` | 按时间分层（类似时序库） | 时间序列数据 |

### 9.4 Compaction 与写入的关系

```text
Compaction 消耗：
  磁盘读 + 磁盘写 + CPU（解压/压缩）
  → 与用户写入争抢 HDFS 带宽

写阻塞链条：
  写入快 → HFile 多 → Compaction 追不上 → 文件数超限
  → 触发 flush 阻塞 → put 延迟上升 → 雪崩

监控指标：
  compactionQueueLength（排队数）
  compactionTime（耗时）
  storeFileCount（文件数，单 Store 超过 10 需关注）
```

---

## 10. 删除与墓碑

HBase 的删除不是物理删除，而是写一个"墓碑"（tombstone）：

```text
delete 'app:user', 'u1001', 'info:name'

实际写入：
  KeyValue(row=u1001, family=info, qualifier=name,
           timestamp=now, type=DeleteColumn, value=空)

读取时：
  从新到旧扫描，遇到墓碑 → 丢弃该列/该行更早的版本

物理删除：
  只有 Major Compaction 才会真正移除墓碑与被覆盖的数据
```

四种删除类型：

| 类型 | 作用范围 | Shell 对应 |
|------|---------|-----------|
| `Delete` | 指定版本的单个单元格 | `delete`（带时间戳） |
| `DeleteColumn` | 某列的所有版本（≤ 指定时间戳） | `delete`（不带时间戳） |
| `DeleteFamily` | 某列族的所有列 | `deleteall`（指定列族） |
| `DeleteFamilyVersion` | 某列族指定版本 | 客户端 API |

墓碑带来的三个工程后果：

1. **删除不释放空间**，直到 major compaction。
2. **删除后立刻读不到**，但底层数据还在，符合语义。
3. **大量删除会造成读放大**：每次读都要跳过墓碑。

> **提示**：批量清理历史数据优先用 TTL（自动过期），而不是逐行 delete。TTL 在 Compaction 时统一清理，不会产生海量墓碑。

---

## 11. Bloom Filter 与块索引

Bloom Filter 解决"这个 HFile 里到底有没有这个 key"的问题：

```text
没有 Bloom Filter：
  一次 get 要查所有 HFile（假设 20 个）→ 20 次磁盘 IO

有 Bloom Filter：
  先用内存中的位图判断"可能存在/一定不存在"
  → 跳过 90% 以上的文件 → 只查 1-2 个
```

两级 Bloom：

| 类型 | 判断粒度 | 适用 |
|------|---------|------|
| `ROW` | 行是否存在 | 按 RowKey 点查 |
| `ROWCOL` | 行 + 列是否存在 | 按行 + 列点查（宽表常用） |
| `NONE` | 不启用 | 纯 Scan 场景（无收益） |

建表时指定：

```text
create 'app:user', {NAME => 'info', BLOOMFILTER => 'ROWCOL'}
```

误判率与内存：

```text
hbase.hregion.bloom.block.size（默认 128KB）
误判率越低 → 位图越大 → 堆内存占用越高
默认配置在 1% 左右，通常够用
```

> **警告**：Bloom Filter 只对 `get`（点查）有效，对 `scan` 无效。Scan 是按区间顺序读，无法用位图跳过。如果业务只有 Scan，把 BLOOMFILTER 设为 NONE，省内存。

---

## 12. 编码与压缩

### 12.1 数据块编码

HFile 的数据块可以按前缀压缩，减少存储与 IO：

| 编码 | 原理 | 压缩率 | CPU 开销 |
|------|------|--------|---------|
| `NONE` | 不编码 | 1x | 无 |
| `PREFIX` | 相邻 key 共享前缀 | 中 | 低 |
| `DIFF` | 编码与上一个 key 的差异 | 较高 | 中 |
| `FAST_DIFF` | DIFF 的优化，支持随机定位 | 高 | 中 |
| `ROW_INDEX_V1` | 行级索引编码 | 高 | 中 |

建表指定：

```text
create 'app:log', {NAME => 'd', DATA_BLOCK_ENCODING => 'FAST_DIFF'}
```

### 12.2 压缩算法

| 算法 | 压缩率 | 速度 | 适用 |
|------|--------|------|------|
| `NONE` | 1x | 最快 | 已压缩数据（图片、视频） |
| `SNAPPY` | 2-3x | 快 | 通用默认 |
| `LZ4` | 2-3x | 极快 | 延迟敏感 |
| `ZSTD` | 3-5x | 中 | 存储敏感（HBase 2.0+ 支持） |
| `GZ` | 5-8x | 慢 | 冷数据归档 |

建表指定：

```text
create 'app:log', {NAME => 'd', COMPRESSION => 'ZSTD'}
```

> **提示**：压缩发生在 HFile 块级别，读时需要解压。CPU 与存储的权衡：写入量大、CPU 富余时选 ZSTD；延迟敏感选 LZ4。压缩算法可在表在线时修改，但只对新写入的文件生效，旧文件要等 Compaction 重写。

---

## 13. 配置速查

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `hbase.hregion.memstore.flush.size` | 128MB | 单 MemStore 刷写阈值 |
| `hbase.hregion.memstore.block.multiplier` | 4 | Region 内 MemStore 总上限倍数 |
| `hbase.regionserver.global.memstore.size` | 0.4 | RS 堆中 MemStore 总占比 |
| `hbase.regionserver.global.memstore.size.lower.limit` | 0.95 | 全局刷写触发比例 |
| `hfile.block.cache.size` | 0.4 | RS 堆中 BlockCache 占比 |
| `hbase.hstore.compaction.min` | 3 | minor compaction 最少文件数 |
| `hbase.hstore.compaction.max` | 10 | 单次最多合并文件数 |
| `hbase.hregion.majorcompaction` | 604800000（7 天） | major compaction 周期，0 为关闭 |
| `hbase.hregion.max.filesize` | 10GB | Region 分裂阈值上限 |
| `hbase.regionserver.max.logs` | 32 | WAL 文件数上限，超限阻塞写入 |
| `hbase.hregion.memstore.mslab.enabled` | true | MemStore 本地分配缓冲，减少 GC |
| `hbase.hstore.blockingStoreFiles` | 16 | Store 文件数超过则阻塞写入 |

---

## 14. 小结

| 问题 | 答案 |
|------|------|
| 为什么用 LSM | 把随机写转成顺序写，换高写吞吐 |
| LSM 三层 | MemStore（内存）→ HFile（磁盘）→ Compaction（合并） |
| 三种放大 | 写放大、读放大、空间放大，互相制约 |
| WAL 的作用 | 保证 RS 宕机后 MemStore 可恢复 |
| HFile 的结构 | 数据块 + 块索引 + Bloom + Meta + Trailer |
| 删除的本质 | 写墓碑，major compaction 才真正清理 |
| Compaction 两类 | minor 合并部分文件；major 全量合并并清理 |
| Bloom 的作用 | 点查时跳过不含目标 key 的文件，对 Scan 无效 |
| 调优的坐标系 | 在三种放大之间找平衡，没有全局最优 |

**下一模块**：[04-HBase读写流程与缓存机制](04-HBase读写流程与缓存机制.md)　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
