# 05-HBase RowKey 与 Schema 设计

> HBase 只有 RowKey 一个索引。表设计不是"建字段"，而是"把查询模式编码进 RowKey"。RowKey 设计错，后面所有调优都是补救；设计对，集群可以线性扩展到 PB 级。这一篇是 HBase 面试与实际项目最核心的部分。

---

## 目录

1. [RowKey 的四条设计原则](#1-rowkey-的四条设计原则)
2. [写热点：最常见的生产事故](#2-写热点最常见的生产事故)
3. [四种打散手段](#3-四种打散手段)
4. [时间序列场景的 RowKey](#4-时间序列场景的-rowkey)
5. [预分区](#5-预分区)
6. [列族设计](#6-列族设计)
7. [宽表与高表](#7-宽表与高表)
8. [二级索引方案](#8-二级索引方案)
9. [典型建模案例](#9-典型建模案例)
10. [反模式清单](#10-反模式清单)
11. [建表与设计代码](#11-建表与设计代码)
12. [面试高频问法](#12-面试高频问法)
13. [小结](#13-小结)

---

## 1. RowKey 的四条设计原则

RowKey 同时承担"唯一标识""排序索引""分片依据"三个角色，设计时必须同时满足：

```text
原则一：唯一
  同一张表内不能重复
  重复写入 = 覆盖（追加新版本）

原则二：长度短
  建议 10-100 字节，最长 64KB
  RowKey 在每个 KeyValue 中重复存储
  → 长度翻倍，存储与索引同步翻倍

原则三：分布均匀
  写入不能集中到少数 Region
  读请求不能集中到少数 Region
  → 否则集群加了机器也没用

原则四：匹配查询
  最高频的查询条件必须能作为 RowKey 前缀
  点查 → 完整 RowKey
  范围查 → 前缀 + 有序后缀
```

四条原则天然冲突：短 RowKey 可能不够散列，散列可能破坏查询顺序，匹配查询又限制前缀。设计过程就是排优先级：

```text
先确定"最高频查询是什么"
  → 决定 RowKey 前缀
再确定"数据量级与写入速率"
  → 决定是否需要散列
最后确定"是否需要时间范围扫描"
  → 决定时间戳正序还是倒序
```

---

## 2. 写热点：最常见的生产事故

### 2.1 热点怎么产生

```text
场景：订单表，RowKey = 订单号（自增或时间戳前缀）

  20261026000001
  20261026000002
  20261026000003
  ...

RowKey 字典序连续 → 全部落在最后一个 Region
→ 一台 RegionServer 承担全部写入
→ 其他机器空闲
→ 该 RS 的 MemStore 打满 → 阻塞写入 → 业务超时
```

热点不只在写入端，也会在读端：

```text
读热点：
  所有请求都查同一批 RowKey（如某个爆款商品、某个大 V）
  → 单个 Region 的 BlockCache 命中率再高，也会被 QPS 打爆
```

### 2.2 判断是否存在热点

```text
① RegionServer UI 看 Request 分布
② HBase Shell 的 status 'detailed' 看各 RS 的 Region 数
③ 看单 RS 的 CPU、磁盘 IO 是否显著高于其他
④ 看 Region 的写请求 QPS 分布（需要监控系统）
```

### 2.3 热点的三个来源

| 来源 | 例子 | 解决方向 |
|------|------|---------|
| RowKey 单调递增 | 时间戳、自增 ID、雪花 ID 直接做前缀 | 打散前缀 |
| 预分区缺失 | 新表只有一个 Region | 建表时预分区 |
| 业务天然倾斜 | 大客户、爆款商品、头部用户 | 拆分热点行、加缓存 |

---

## 3. 四种打散手段

### 3.1 加盐（Salt）

在 RowKey 前加固定长度的随机前缀：

```text
原 RowKey：order_20261026_000001
加盐（4 桶）：0_order_20261026_000001
             1_order_20261026_000001
             2_order_20261026_000001
             3_order_20261026_000001

写入：前缀 = hash(orderId) % 4
读取：需要知道前缀 → 查 4 次（或先查索引表）

优点：写入均匀
缺点：读取放大（前缀未知时要扫所有桶）
```

### 3.2 哈希（Hash）

用 MD5/SHA 或 MurmurHash 取前几位做前缀：

```text
原 RowKey：user_1001
哈希前缀：a3f2_user_1001
          （取 hash(user_1001) 的前 4 位十六进制）

优点：分布更均匀，不依赖桶数
缺点：完全失去顺序性，只能点查
```

### 3.3 反转（Reverse）

把 RowKey 的某些部分倒序：

```text
手机号：13812345678 → 87654321831
订单号：20261026000001 → 10000062016202

效果：原本递增的 ID 变成"看似随机"的分布
适用：ID 长度固定、且不需要范围扫描
缺点：范围查询失效
```

### 3.4 分桶 + 时间后缀

保留业务前缀，把时间维度放到后面：

```text
原设计（热点）：
  ts_20261026120000_user1001

改为（打散）：
  user1001_20261026120000

效果：
  同一用户的数据连续（可范围扫描）
  不同用户分散到不同 Region（写入均匀）
```

### 3.5 四种手段对比

| 手段 | 写入均匀 | 范围扫描 | 读取放大 | 适用 |
|------|:-------:|:-------:|:-------:|------|
| 加盐 | 好 | 破坏 | 有（查 N 桶） | 写入极高、读以点查为主 |
| 哈希 | 最好 | 破坏 | 无（点查） | 纯点查场景 |
| 反转 | 较好 | 破坏 | 无 | 定长 ID、点查 |
| 分桶+时间后缀 | 好 | 保留 | 无 | 用户维度时间序列（最常用） |

> **要点**：优先选"业务前缀 + 时间后缀"，它同时满足散列与范围扫描。只有在业务前缀本身高度集中（如单一大客户占 80% 写入）时，才需要加盐或哈希。

---

## 4. 时间序列场景的 RowKey

时间序列是 HBase 最典型的场景，RowKey 设计有一个标准答案：

### 4.1 正序 vs 倒序

```text
需求：查某用户最近 30 条记录

正序 RowKey：user1001_1735689600000
  → 最新数据在区间末尾
  → 需要 seek 到末尾再反向 Scan（或先算时间范围）

倒序 RowKey：user1001_9223372035121262207
  → 最新数据在区间开头
  → 直接 Scan 前 30 条，天然倒序
```

倒序时间戳的计算：

```java
long reversed = Long.MAX_VALUE - timestamp;
```

### 4.2 标准时间序列模型

```text
RowKey = 实体ID + 反转时间戳 + 可选序列号

例：设备轨迹
  device001_9223372035121262207_0001

查询：
  最近 N 条：Scan prefix=device001_ limit=N
  时间段：  Scan start=device001_(MAX-结束时间) stop=device001_(MAX-开始时间)
```

### 4.3 分页与去重

```text
同一毫秒多条记录：
  加序列号/随机后缀避免覆盖
  device001_<reverseTs>_<seq>

分页：
  记住上一页最后一条 RowKey
  下一页 startRow = 上一页最后 RowKey + \0（不含）
```

---

## 5. 预分区

预分区解决"新表只有一个 Region，写入全压一台机器"的问题：

### 5.1 手动指定切分点

```text
create 'app:order', 'info', SPLITS => ['1', '2', '3', '4', '5', '6', '7', '8', '9']
```

这会创建 10 个 Region，边界由切分点决定。

### 5.2 十六进制切分

```text
create 'app:order', 'info', {NUMREGIONS => 16, SPLITALGO => 'HexStringSplit'}
```

适合 RowKey 是十六进制哈希前缀的场景。

### 5.3 按业务前缀切分

```text
如果 RowKey 是 salt_xxx（4 个桶）：
  SPLITS => ['1', '2', '3']

如果 RowKey 是 user_xxx 且用户 ID 均匀：
  按 ID 范围切：['user_1000000', 'user_2000000', ...]
```

### 5.4 预分区数量怎么定

```text
经验公式：
  Region 数 = 预期数据量 / 单 Region 目标大小（10-50GB）

或者按写入能力：
  Region 数 = 峰值写入 QPS / 单 Region 可持续写入 QPS

注意：
  分区过多 → 小文件多、Compaction 压力大、管理开销高
  分区过少 → 后期频繁分裂，分裂期间抖动
  宁可初期略多，也不要全表一个 Region
```

> **提示**：预分区后如果发现分布仍不均（某几个 Region 特别大），说明 RowKey 前缀分布与切分点不匹配，需要重新设计前缀或做 Region 合并/迁移。

---

## 6. 列族设计

列族是物理存储单元，设计错误会直接放大 IO 与内存：

### 6.1 数量：越少越好

```text
每个列族独立拥有：
  一个 MemStore（占内存）
  一组 HFile（占文件句柄）
  一份 BlockCache 份额

3 个列族 → 3 倍 MemStore、3 组文件、3 倍刷写
生产建议：1-3 个，最多不超过 5 个
```

### 6.2 命名：越短越好

```text
列族名会写入每个 KeyValue
  info → 4 字节
  user_information → 16 字节
  10 亿个 Cell → 差 12GB

惯例：d（data）、f（family）、i（info）、m（meta）
```

### 6.3 划分依据：访问模式

```text
同一列族的列应该"总是一起读"或"总是写多读少"

例：用户表
  列族 info：姓名、年龄、城市（每次查询都读）
  列族 tag：兴趣标签（几十万列，很少读）
  → 分成两个列族，读 info 时不加载 tag 的文件

反例：
  按业务模块分 10 个列族
  → 每次读要打开 10 组文件，内存与句柄爆炸
```

### 6.4 列族属性

| 属性 | 作用 | 建议 |
|------|------|------|
| `VERSIONS` | 保留版本数 | 不需要历史设 1 |
| `TTL` | 数据存活时间 | 日志/会话必设 |
| `COMPRESSION` | 压缩算法 | SNAPPY/LZ4/ZSTD |
| `BLOOMFILTER` | 布隆过滤器 | 点查设 ROWCOL |
| `DATA_BLOCK_ENCODING` | 块内编码 | FAST_DIFF |
| `IN_MEMORY` | 优先驻留内存 | 仅小表热点用，慎用 |
| `BLOCKCACHE` | 是否进读缓存 | 冷数据可关 |

---

## 7. 宽表与高表

同一个业务可以有兩種建模方式：

```text
宽表（Wide）：
  RowKey = 用户ID
  列 = 每个标签一列（tag:sports、tag:music ...）
  → 一行包含该用户所有标签
  → 读一个用户 = 一次 get

高表（Tall）：
  RowKey = 用户ID + 标签名
  列 = value
  → 一个标签一行
  → 读一个用户 = 一次 Scan 前缀
```

| 维度 | 宽表 | 高表 |
|------|------|------|
| 单行大小 | 可能很大（百万列） | 小 |
| 点查 | 一次 get | 一次 scan |
| 列动态性 | qualifier 动态 | 天然动态 |
| 部分列读取 | 支持（指定 qualifier） | 按行过滤 |
| 事务 | 单行原子（多列一起） | 跨行无事务 |
| 适合 | 画像、配置、稀疏属性 | 明细、事件、日志 |

> **提示**：单行不要超过几十 MB。HBase 对超大行不友好：一次 RPC 传输大、Region 分裂粒度粗、内存压力大。超过阈值时拆成高表。

---

## 8. 二级索引方案

HBase 原生只有 RowKey 索引，按其他字段查需要自己建索引：

### 8.1 索引表（最常见）

```text
主表：RowKey = orderId，列含 userId、status、createTime
索引表：RowKey = userId_status_createTime_orderId
       列 = 主表 RowKey（orderId）

查询"某用户某状态的订单"：
  ① 查索引表 Scan prefix=userId_status_
  ② 拿到一批 orderId
  ③ 批量 get 主表
```

代价：

- 写放大 2 倍（主表 + 索引表）
- 一致性需要业务保证（双写或异步同步）
- 删除/更新要同步维护索引

### 8.2 Phoenix 全局索引

```text
Phoenix 在 HBase 上提供 SQL 与二级索引：
  CREATE INDEX idx_user_status ON orders(user_id, status);

全局索引：独立索引表，读走索引，写同步维护
本地索引：索引与数据同 Region，适合写多读少

代价：
  全局索引写放大明显
  需要额外部署 Phoenix Query Server
```

### 8.3 外部索引

```text
HBase 存明细，ES 存索引：
  写入 → HBase + ES 双写（或 CDC 同步）
  查询 → ES 找到 RowKey → 回查 HBase

优点：检索能力强（全文、聚合、多条件）
缺点：双写一致性、运维两套系统
```

### 8.4 选型

| 方案 | 写放大 | 查询能力 | 一致性 | 适用 |
|------|:-----:|---------|--------|------|
| 索引表 | 2x | 固定前缀 | 业务保证 | 查询模式固定 |
| Phoenix 全局索引 | 2-3x | SQL 多条件 | 较强 | 需要 SQL |
| ES 外部索引 | 2x + 同步 | 最强 | 最终一致 | 复杂检索 |

---

## 9. 典型建模案例

### 9.1 用户画像

```text
RowKey：userId（定长或哈希打散）
列族：
  info：姓名、年龄、城市、注册时间
  tag：兴趣标签（qualifier 动态，几万到几百万）
  score：评分模型输出

设计要点：
  tag 用宽表，单用户列多但稀疏
  读画像 = 一次 get（指定列族）
  标签更新 = put 单列，不影响其他列
```

### 9.2 消息 / Feed

```text
RowKey：receiverId + 反转时间戳 + messageId
列族：m（消息内容、发送者、类型）
TTL：90 天

查询：
  拉最近 20 条：Scan prefix=receiverId_ limit=20
  翻页：从上一页最后 RowKey 继续
```

### 9.3 订单明细

```text
主表 RowKey：orderId
索引表 RowKey：userId + 反转时间戳 + orderId
列族：info（金额、状态、商品数）

查询：
  按订单号：get 主表
  按用户查订单：Scan 索引表 → 批量 get 主表
```

### 9.4 设备时序数据

```text
RowKey：deviceId + 反转时间戳
列族：d（温度、湿度、电压等）
TTL：30 天
压缩：ZSTD
预分区：按 deviceId 哈希前缀

查询：
  最新值：Scan limit=1
  时间范围：start/stop 用反转时间计算
```

### 9.5 日志明细

```text
RowKey：appId + 日期 + 反转时间 + 随机后缀
列族：l（原始日志、解析字段）
TTL：7-30 天

注意：
  日志写入量极大 → 必须预分区 + 批量写
  查询多为按时间范围 → 日期放前缀便于过期清理
```

---

## 10. 反模式清单

| 反模式 | 后果 | 正确做法 |
|--------|------|---------|
| 时间戳直接做 RowKey 前缀 | 写入全压最后一个 Region | 反转或加实体前缀 |
| 自增 ID 直接做 RowKey | 同上 | 哈希/加盐/反转 |
| 建表不预分区 | 初期单 Region 热点 | 预分区 16-64 个 |
| 列族数量 > 5 | MemStore/文件句柄/刷写放大 | 合并列族，按访问模式分 |
| 列族名很长 | 存储放大 | 用 1-2 字符 |
| RowKey 很长（> 200 字节） | 存储与索引放大 | 缩短，用哈希替代长字符串 |
| 单行几十 MB | RPC 超时、分裂粒度粗 | 拆高表 |
| 用 scan 做全表统计 | 扫全集群，拖垮业务 | 用 Spark/ClickHouse |
| 用 Filter 代替索引 | 全表读 + 服务端 CPU 打满 | 建索引表或调整 RowKey |
| 频繁 delete 清理历史 | 墓碑堆积、读放大 | 用 TTL |
| 每行都指定未来时间戳 | 版本永远最新、无法清理 | 用服务端时间或严格校验 |

---

## 11. 建表与设计代码

### 11.1 Shell 建表

```text
create_namespace 'app'

create 'app:user_profile',
  {NAME => 'info', VERSIONS => 1, COMPRESSION => 'ZSTD', BLOOMFILTER => 'ROWCOL'},
  {NAME => 'tag', VERSIONS => 1, TTL => 31536000, COMPRESSION => 'ZSTD'},
  SPLITS => ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f']
```

### 11.2 Java 建表

```java
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;

public void createTable(Admin admin) throws Exception {
    TableName tableName = TableName.valueOf("app", "user_profile");

    ColumnFamilyDescriptorBuilder infoBuilder = ColumnFamilyDescriptorBuilder
            .newBuilder(Bytes.toBytes("info"))
            .setMaxVersions(1)
            .setCompressionType(Compression.Algorithm.ZSTD)
            .setBloomFilterType(BloomType.ROWCOL);

    ColumnFamilyDescriptorBuilder tagBuilder = ColumnFamilyDescriptorBuilder
            .newBuilder(Bytes.toBytes("tag"))
            .setMaxVersions(1)
            .setTimeToLive(365 * 24 * 3600)
            .setCompressionType(Compression.Algorithm.ZSTD);

    byte[][] splits = new byte[][]{
            Bytes.toBytes("1"), Bytes.toBytes("2"), Bytes.toBytes("3"),
            Bytes.toBytes("4"), Bytes.toBytes("5"), Bytes.toBytes("6"),
            Bytes.toBytes("7"), Bytes.toBytes("8"), Bytes.toBytes("9"),
            Bytes.toBytes("a"), Bytes.toBytes("b"), Bytes.toBytes("c"),
            Bytes.toBytes("d"), Bytes.toBytes("e"), Bytes.toBytes("f")
    };

    TableDescriptor descriptor = TableDescriptorBuilder.newBuilder(tableName)
            .setColumnFamily(infoBuilder.build())
            .setColumnFamily(tagBuilder.build())
            .build();

    admin.createTable(descriptor, splits);
}
```

### 11.3 RowKey 构造工具

```java
public final class RowKeys {

    private RowKeys() {
    }

    public static byte[] userTimeSeries(String userId, long timestamp) {
        long reversed = Long.MAX_VALUE - timestamp;
        return Bytes.toBytes(userId + "_" + reversed);
    }

    public static byte[] salted(String salt, String businessKey) {
        return Bytes.toBytes(salt + "_" + businessKey);
    }

    public static String saltOf(String key, int buckets) {
        int index = Math.floorMod(key.hashCode(), buckets);
        return String.format("%02d", index);
    }
}
```

---

## 12. 面试高频问法

**问：HBase 的 RowKey 怎么设计？**

答：四步。第一步定查询模式，把最高频的查询条件放前缀；第二步控长度，10-100 字节，因为 RowKey 在每个 Cell 里重复存；第三步防热点，用业务前缀 + 时间后缀，必要时加盐或哈希；第四步配合预分区，建表时按前缀分布切 16-64 个 Region。

**问：什么是写热点？怎么解决？**

答：RowKey 单调递增或高度集中，导致写入全压在一个 Region。表现为单台 RegionServer CPU/IO 打满、MemStore 阻塞、其他机器空闲。解决三层：RowKey 打散（加盐/哈希/反转/时间后缀）、建表预分区、业务倾斜严重时拆热点行或加缓存。

**问：为什么 RowKey 要短？**

答：HBase 每个 KeyValue 都完整存储 RowKey、列族、列名。RowKey 长度翻倍，存储、索引、缓存占用同步翻倍。10 亿行 × 每行 5 列 × 多 80 字节，就是 400GB 的额外开销。

**问：HBase 怎么实现二级索引？**

答：原生没有。三种方案：索引表（RowKey 为查询条件 + 主键，业务双写保证一致）、Phoenix 全局/本地索引、外部 ES 索引（HBase 存明细、ES 存检索字段）。选型看查询复杂度与一致性要求。

**问：宽表和高表怎么选？**

答：宽表一行包含实体的所有属性，点查一次 get，适合画像、配置；高表一行一条明细，Scan 前缀读取，适合日志、事件、时序。单行不要超过几十 MB，超过就拆高表。

---

## 13. 小结

| 问题 | 答案 |
|------|------|
| RowKey 四原则 | 唯一、短、均匀、匹配查询 |
| 热点根因 | RowKey 单调或集中，写入压单个 Region |
| 打散四手段 | 加盐、哈希、反转、业务前缀+时间后缀 |
| 时间序列标准解 | 实体 ID + 反转时间戳 |
| 预分区 | 建表时按前缀切 16-64 个 Region |
| 列族数量 | 1-3 个，按访问模式分，名字要短 |
| 宽表 vs 高表 | 画像用宽表，明细用时序高表 |
| 单行大小 | 不超过几十 MB |
| 二级索引 | 索引表 / Phoenix / ES |
| TTL | 清理历史优先用 TTL，不用 delete |

**下一模块**：[06-HBase集群部署与运维](06-HBase集群部署与运维.md)　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
