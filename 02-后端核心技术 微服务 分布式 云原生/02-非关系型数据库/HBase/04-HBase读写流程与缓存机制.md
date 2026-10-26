# 04-HBase 读写流程与缓存机制

> 写路径是"WAL → MemStore → 返回"，读路径是"MemStore → BlockCache → HFile"。两条路径都短，但每一步都可能成为瓶颈：写卡在 WAL 同步或 MemStore 阻塞，读卡在文件数量与缓存命中率。把两条路径的每一步拆开，才知道该调哪个参数。

---

## 目录

1. [写流程全景](#1-写流程全景)
2. [写流程逐段拆解](#2-写流程逐段拆解)
3. [写一致性模型](#3-写一致性模型)
4. [读流程全景](#4-读流程全景)
5. [读流程逐段拆解](#5-读流程逐段拆解)
6. [BlockCache 深度](#6-blockcache-深度)
7. [短路读](#7-短路读)
8. [Scan 机制](#8-scan-机制)
9. [读写性能优化清单](#9-读写性能优化清单)
10. [Java 代码示例](#10-java-代码示例)
11. [小结](#11-小结)

---

## 1. 写流程全景

一次 `put` 从客户端到落盘的完整链路：

```text
Client
  │ ① 查路由缓存（未命中则查 meta）
  ▼
RegionServer（目标 Region 所在）
  │ ② RPC 反序列化，定位 Region 与列族
  ▼
Region
  │ ③ 获取行锁（row lock）
  ▼
WAL
  │ ④ 追加 HLog 条目
  │ ⑤ 按 Durability 决定是否 sync 到 HDFS
  ▼
MemStore
  │ ⑥ 插入跳表
  │ ⑦ 检查 MemStore 是否达到刷写阈值
  ▼
返回客户端成功（⑧）
```

这条链路有三个关键点：

1. **WAL 在 MemStore 之前**。先持久化日志，再写内存，保证崩溃可恢复。
2. **行锁保证单行原子**。同一行的并发写按到达顺序串行。
3. **返回成功时数据可能只在内存**。此时数据已进 WAL，不丢，但还没进 HFile。

---

## 2. 写流程逐段拆解

### 2.1 客户端路由

```text
① 客户端根据 RowKey 查本地路由缓存
   → 命中：直接发往对应 RegionServer
   → 未命中：查 hbase:meta，拿到 Region 区间与 RS 地址
② 构造 Mutate 请求（Put/Delete/Increment/Append）
③ 通过 RPC 发送（protobuf 编码）
```

### 2.2 RegionServer 接收

```text
① RPC 线程池接收请求
② 校验表是否启用、Region 是否在线
③ 定位 Region 与目标 Store
④ 获取行锁（hbase.rowlock.wait.duration，默认不等待，冲突则重试）
```

### 2.3 WAL 写入

```text
① 序列化 KeyValue
② 追加到当前 WAL 文件（HDFS 顺序写）
③ 根据 Durability 决定同步级别：
   SYNC_WAL（默认）：写入 HDFS pipeline 后返回
   ASYNC_WAL：放入异步队列，立即返回
   SKIP_WAL：不写
④ 记录 WAL 序列号（sequence id），用于副本复制与恢复
```

WAL 写入是写路径上最重的一步。它的延迟直接决定 put 的 P99：

```text
WAL 延迟来源：
  HDFS pipeline（3 副本 → 3 次网络写）
  DataNode 磁盘 fsync
  RegionServer 与 DataNode 同机时的短路写优化

优化手段：
  同机部署（数据本地化）
  ASYNC_WAL（牺牲少量可靠性）
  批量写（BufferedMutator 合并请求）
```

### 2.4 MemStore 写入

```text
① 插入 ConcurrentSkipListMap
② 更新 MemStore 大小计数
③ 检查是否触发刷写：
   单 Store 超过 flush.size
   Region 内总和超过 block.multiplier × flush.size
   全局超过 global.memstore.size
④ 触发则把当前 MemStore 标记为冻结，后台异步刷写
```

### 2.5 写入阻塞（Blocking）

当 MemStore 无法及时刷写时，RegionServer 会主动阻塞写入：

```text
阻塞条件：
  Region 内 MemStore 总和 > hbase.hregion.memstore.block.multiplier × flush.size
  RegionServer 全局 MemStore > global.memstore.size
  Store 文件数 > hbase.hstore.blockingStoreFiles（默认 16）

阻塞表现：
  put 请求挂起，日志出现 "Blocking updates for region ..."
  客户端超时后重试 → 重试风暴 → 雪崩

根因通常是三类：
  ① 写入速率超过磁盘/Compaction 能力
  ② 列族太多，MemStore 总量被放大
  ③ 单 Region 过热（RowKey 不散列）
```

---

## 3. 写一致性模型

HBase 的一致性要分三个层次讲清楚：

### 3.1 单行原子性

```text
一个 Put 操作包含同一行的多个列：
  put 't', 'row1', 'info:name', 'A'
  put 't', 'row1', 'info:age', '18'
  一次批量 Put 的多个 Cell → 原子生效

底层保证：
  行锁串行化同一行的写入
  WAL 条目按行组织，恢复时整行重放
```

### 3.2 单行强一致读

```text
写入返回成功后，任何客户端读同一行都能读到最新值
原因：
  读先查 MemStore（最新数据在内存）
  不需要等待刷盘或副本同步
```

### 3.3 跨行与跨 Region

```text
跨行：没有事务，可能读到部分成功
跨 Region：没有全局快照，Scan 期间可能有新写入
跨集群复制：异步，存在延迟（通常秒级）

因此：
  HBase 适合"单行状态机"，不适合"多行账本"
  需要跨行事务时用 Phoenix 事务表或外部事务框架
```

### 3.4 时间戳与时钟

```text
默认时间戳 = RegionServer 当前毫秒时间
  → 多台 RS 时钟不一致会导致版本顺序错乱
  → 生产必须开启 NTP 同步

也可以由客户端显式指定时间戳：
  put.addColumn(cf, qualifier, timestamp, value)
  → 用于数据回放、历史数据导入

风险：
  客户端指定错误的时间戳（如未来时间）会让新数据"永远最新"
  并阻止旧版本被清理
```

> **警告**：跨机房部署时 NTP 偏差可能到几十毫秒。若业务依赖版本顺序，务必显式传时间戳或保证同一行只由同一客户端写入。

---

## 4. 读流程全景

一次 `get` 的完整链路：

```text
Client
  │ ① 路由定位（同写路径）
  ▼
RegionServer
  │ ② 定位 Region 与列族
  ▼
MemStore
  │ ③ 查 MemStore（最新未刷写数据）
  │    命中且满足版本要求 → 返回
  ▼
BlockCache
  │ ④ 查缓存块（DATA/INDEX/BLOOM）
  │    命中 → 直接解析
  ▼
HFile（从新到旧）
  │ ⑤ Bloom Filter 判断"是否可能包含"
  │ ⑥ 块索引二分定位 Data Block
  │ ⑦ 读块（缺失则从 HDFS 读并回填缓存）
  │ ⑧ 块内二分查找 KeyValue
  ▼
合并
  │ ⑨ 多文件、多版本按时间戳合并
  ▼
返回最新版本（或指定版本数）
```

读路径的核心问题是"要查多少个 HFile"。假设一个 Store 有 20 个 HFile：

```text
无优化：20 次 Bloom 查询 + 最多 20 次块读
有 Bloom：Bloom 过滤掉 90%+ → 实际读 1-2 个文件
有 BlockCache：热点块直接在内存命中 → 0 次 HDFS 读
```

---

## 5. 读流程逐段拆解

### 5.1 MemStore 查找

```text
MemStore 是有序跳表：
  按 (row, family, qualifier, timestamp) 排序
  查找是 O(log n) 的跳表定位

注意：
  同一 key 可能同时存在于 MemStore 与 HFile
  MemStore 的版本更新，合并时优先返回
```

### 5.2 Bloom Filter 判定

```text
对每个 HFile：
  用 Bloom 判断 row（或 row+col）是否存在
  → 一定不存在：跳过整个文件
  → 可能存在：进入下一步

重要限制：
  Bloom 只在 get 时生效
  scan（前缀范围）无法使用，因为范围未知
```

### 5.3 块索引定位

```text
每个 HFile 的块索引记录：
  每个 Data Block 的首 key → 块偏移

查找：
  在索引中二分找到"首 key ≤ 目标 key"的最后一个块
  读取该块
```

### 5.4 块读取与缓存回填

```text
① 先查 BlockCache（块号 → 块内容）
② 未命中 → 从 HDFS 读（可能触发短路读）
③ 读到的块放入 BlockCache（按策略淘汰）
④ 在块内二分查找 KeyValue
```

### 5.5 多版本合并

```text
同一 key 在多个来源存在：
  MemStore（最新）
  HFile 1（较新）
  HFile 2（较旧）

合并规则：
  按 timestamp 降序
  取前 VERSIONS 个
  跳过墓碑及其覆盖的旧版本
```

---

## 6. BlockCache 深度

BlockCache 是读性能的核心，理解它的三种实现与四类缓存对象：

### 6.1 三种实现

| 实现 | 存储位置 | 特点 |
|------|---------|------|
| `LRUBlockCache` | JVM 堆内 | 简单，但大堆 GC 压力大 |
| `BucketCache` | 堆外内存 / SSD 文件 | 减少 GC，容量可超堆 |
| `CombinedBlockCache` | 堆内 LRU + 堆外 Bucket | 2.x/3.x 默认，兼顾两者 |

```text
CombinedBlockCache 的分工：
  堆内 LRU：INDEX、BLOOM、META 块（小、访问频繁）
  堆外 Bucket：DATA 块（大、占用多）
  → 索引常驻内存，数据块不挤压 GC
```

### 6.2 四类缓存对象

| 类型 | 内容 | 默认归属 |
|------|------|---------|
| `DATA` | 实际 KeyValue 数据块 | BucketCache（堆外） |
| `INDEX` | 块索引 | LRU（堆内） |
| `BLOOM` | 布隆过滤器位图 | LRU（堆内） |
| `META` | meta 表数据 | LRU（堆内） |

### 6.3 缓存淘汰与命中率

```text
LRU 的双队列：
  single-access queue：第一次访问的块
  multi-access queue：第二次访问后晋升
  → 防止一次性扫描污染缓存

命中率观察：
  RegionServer UI 的 blockCacheHitRatio
  低于 80% 说明缓存不足或访问模式随机

容量规划：
  BlockCache 占堆 40% 是常见起点
  读多写少可到 50-60%
  堆外 Bucket 可配置为堆的 1-2 倍（需预留系统内存）
```

> **提示**：BucketCache 用堆外内存，不受 GC 管理，但配置过大（超过物理内存）会导致 OS OOM Killer 杀进程。规划时把"堆 + 堆外 + 系统预留"三者相加，留 10-15% 余量。

---

## 7. 短路读

短路读（Short-Circuit Read）是 HBase 读性能的重要优化：

```text
普通读：
  RegionServer → HDFS Client → DataNode（网络）→ 磁盘

短路读：
  RegionServer 与 DataNode 同机
  → 直接读本地磁盘上的块文件
  → 绕过网络与 HDFS 协议栈

生效条件：
  RegionServer 与 DataNode 部署在同一台机器
  hbase.regionserver.hdfs.client.shortcircuit 相关配置开启
  HDFS 侧 dfs.client.read.shortcircuit=true

收益：
  延迟降低（无网络往返）
  带宽节省（不占集群网络）
```

> **要点**：短路读是"计算存储一体化部署"的最大收益之一。如果采用计算存储分离（RS 与 DN 不同机），短路读失效，读延迟会明显上升，架构选型时必须计入。

---

## 8. Scan 机制

Scan 是 HBase 的另一半查询能力，机制与 get 不同：

### 8.1 Scan 的执行模型

```text
Client 发起 Scan(startRow, stopRow)
  → 服务端在 Region 内顺序遍历
  → 每次返回一批结果（由 caching 控制）
  → 客户端调用 next() 拉下一批
  → 直到遍历完或达到 limit

关键：Scan 不是"一次返回全部"
  → 必须用 next() 分批拉取
  → 忘记关闭 ResultScanner 会泄漏连接
```

### 8.2 三个关键参数

| 参数 | 作用 | 建议 |
|------|------|------|
| `caching` | 每次 RPC 返回多少行 | 100-1000，过小则 RPC 次数多 |
| `batch` | 每次返回多少列（宽表） | 宽表必设，避免单行过大 |
| `limit` | 最多返回多少行 | 明确上限，防止全表扫 |

```java
Scan scan = new Scan()
    .withStartRow(Bytes.toBytes("u1001_"))
    .withStopRow(Bytes.toBytes("u1001`"))
    .setCaching(500)
    .setBatch(100)
    .setLimit(1000);
```

### 8.3 Filter 的工作方式

Filter 在服务端逐行判断，减少网络传输，但不减少磁盘读：

```text
FilterList（AND/OR）
  ├── PrefixFilter：RowKey 前缀
  ├── RowFilter：RowKey 正则/比较
  ├── FamilyFilter：列族
  ├── QualifierFilter：列名
  ├── ValueFilter：值
  ├── SingleColumnValueFilter：单列值（最常用，但要注意性能）
  └── PageFilter：按行数分页

重要认知：
  Filter 是"读出来再判断"，不是索引
  它对 RowKey 之外的字段没有加速作用
  能用 RowKey 表达的条件，优先用 startRow/stopRow
```

### 8.4 Scan 的性能陷阱

```text
① 全表 Scan：没有 startRow/stopRow 或前缀过短
   → 扫所有 Region，可能几小时

② caching 太小：每次 RPC 只取 10 行
   → 网络往返成为瓶颈

③ 单行超大：一行几 MB，batch 未设
   → 单次 RPC 超时

④ 未关闭 Scanner：
   → RegionServer 资源泄漏，最终拒绝服务

⑤ 在 Scan 中做复杂 Filter：
   → 服务端 CPU 打满，影响其他请求
```

---

## 9. 读写性能优化清单

### 9.1 写优化

```text
客户端侧：
  用 BufferedMutator 批量写（每批 100-1000 条）
  关闭自动刷写（setAutoFlush(false)）
  能接受少量丢失时用 ASYNC_WAL
  避免单条 put 循环

表设计侧：
  RowKey 散列，避免写热点
  预分区，避免初期单 Region
  列族控制在 1-3 个
  VERSIONS = 1（不需要历史时）

服务端侧：
  MemStore 与 BlockCache 配比按读写比调整
  Compaction 策略与线程数匹配写入量
  关闭自动 major compaction，改低峰调度
```

### 9.2 读优化

```text
客户端侧：
  能用 get 不用 scan
  scan 必设 caching/batch/limit
  批量 get（List<Get>）而不是循环 get
  使用 Filter 减少网络传输

表设计侧：
  RowKey 设计匹配查询前缀
  Bloom Filter 按需开启（点查开 ROWCOL）
  列族分离冷热数据
  压缩算法选 LZ4/Snappy 降低 IO

服务端侧：
  BlockCache 占比足够
  控制单 Store 文件数（< 10）
  避免频繁 major compaction 干扰读
```

---

## 10. Java 代码示例

### 10.1 连接与写入

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

public class HBaseWriter implements AutoCloseable {

    private final Connection connection;
    private final BufferedMutator mutator;

    public HBaseWriter(Configuration conf, TableName table) throws Exception {
        this.connection = ConnectionFactory.createConnection(conf);
        this.mutator = connection.getBufferedMutator(table);
    }

    public void put(String rowKey, String family, String qualifier, String value) throws Exception {
        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(value));
        mutator.mutate(put);
    }

    public void flush() throws Exception {
        mutator.flush();
    }

    @Override
    public void close() throws Exception {
        mutator.close();
        connection.close();
    }
}
```

### 10.2 点查与范围扫描

```java
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;

public class HBaseReader implements AutoCloseable {

    private final Connection connection;

    public HBaseReader(Configuration conf) throws Exception {
        this.connection = ConnectionFactory.createConnection(conf);
    }

    public Result get(TableName tableName, String rowKey) throws Exception {
        try (Table table = connection.getTable(tableName)) {
            return table.get(new Get(Bytes.toBytes(rowKey)));
        }
    }

    public void scanPrefix(TableName tableName, String prefix, int limit) throws Exception {
        Scan scan = new Scan()
                .withStartRow(Bytes.toBytes(prefix))
                .withStopRow(Bytes.toBytes(prefix + "￿"))
                .setCaching(500)
                .setLimit(limit);

        try (Table table = connection.getTable(tableName);
             ResultScanner scanner = table.getScanner(scan)) {
            for (Result result : scanner) {
                byte[] row = result.getRow();
                byte[] value = result.getValue(Bytes.toBytes("info"), Bytes.toBytes("name"));
            }
        }
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
```

### 10.3 批量 Get

```java
public List<Result> batchGet(TableName tableName, List<String> rowKeys) throws Exception {
    List<Get> gets = rowKeys.stream()
            .map(row -> new Get(Bytes.toBytes(row)))
            .collect(Collectors.toList());

    try (Table table = connection.getTable(tableName)) {
        return Arrays.asList(table.get(gets));
    }
}
```

> **提示**：批量 Get 会按 Region 自动分组并发发送，比循环单条 get 快一个数量级。但单批不宜过大（建议 ≤ 1000 行），否则单次 RPC 过大反而超时。

---

## 11. 小结

| 问题 | 答案 |
|------|------|
| 写路径顺序 | Client → Region → WAL → MemStore → 返回 |
| 为什么先 WAL | 内存数据宕机可恢复 |
| 写阻塞的根因 | MemStore 刷写跟不上、文件数超限、单 Region 过热 |
| 读路径顺序 | MemStore → BlockCache → Bloom → 块索引 → HFile → 合并 |
| Bloom 的作用边界 | 只对 get 有效，scan 无效 |
| BlockCache 三类 | LRU（堆内）、Bucket（堆外）、Combined（默认） |
| 短路读的前提 | RS 与 DataNode 同机 |
| Scan 三参数 | caching、batch、limit |
| Filter 的本质 | 服务端逐行判断，不是索引 |
| 单行一致性 | 强一致；跨行无事务 |

**下一模块**：[05-HBase RowKey与Schema设计](05-HBase RowKey与Schema设计.md)　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
