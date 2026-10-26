# 01-HBase 定位与数据模型

> HBase 是 Google BigTable 的开源实现：一张可以存 PB 级、数十亿行、数百万列的表，按 RowKey 字典序排列，支持单行强一致与范围扫描。理解它的第一件事不是架构，而是"它到底替你解决什么问题、又明确不解决什么"。

---

## 目录

1. [HBase 是什么](#1-hbase-是什么)
2. [它解决什么问题](#2-它解决什么问题)
3. [它明确不解决什么](#3-它明确不解决什么)
4. [五层数据模型](#4-五层数据模型)
5. [稀疏存储与多版本](#5-稀疏存储与多版本)
6. [排序规则与物理含义](#6-排序规则与物理含义)
7. [与关系模型的对照](#7-与关系模型的对照)
8. [逻辑模型与物理模型](#8-逻辑模型与物理模型)
9. [类型系统与序列化](#9-类型系统与序列化)
10. [典型场景与反例](#10-典型场景与反例)
11. [小结](#11-小结)

---

## 1. HBase 是什么

**HBase**：Apache 顶级的分布式、可扩展、面向列的 NoSQL 数据库，2008 年从 Hadoop 子项目起步，2010 年成为 Apache 顶级项目。它的论文原型是 Google 2006 年的 BigTable。

一句话定位：

```text
HBase = HDFS 之上的"分布式有序大表"

  存储底座：HDFS（数据三副本、机架感知）
  数据组织：LSM-Tree（写入先写日志与内存，再顺序落盘）
  访问模型：RowKey 点查 + 范围扫描
  一致性：单行强一致（Row 级原子性）
  扩展方式：Region 水平分裂 + RegionServer 横向加机器
```

它和 Hadoop 的关系需要说清楚：

- HDFS 解决"海量数据怎么存"，但不支持随机写、不支持按 key 快速定位。
- HBase 在 HDFS 之上补上"按 RowKey 随机读写 + 范围扫描"的能力。
- HBase 不是 HDFS 的替代品，而是 HDFS 的应用层封装；HBase 的 RegionServer 崩溃不会丢数据，因为 HFile 与 WAL 都在 HDFS 上有多副本。

> **要点**：HBase 的"分布式"不是靠一致性协议做多副本强一致，而是把副本交给 HDFS。RegionServer 之间不复制数据，故障恢复靠 WAL 回放与 Region 重新分配。

---

## 2. 它解决什么问题

HBase 的能力边界来自四个设计目标，这四点是判断"该不该用 HBase"的核心：

```text
① 海量数据：单表数十亿行、PB 级
   → 关系库单机到千万级就吃力，分库分表后运维复杂度爆炸
   → HBase 天然按 Region 分片，加机器即扩容

② 稀疏数据：几百万列里每行只填几列
   → 关系库的 NULL 也要占位（或依赖变长存储但仍有行开销）
   → HBase 空单元格完全不占空间

③ 随机读写：按 RowKey 毫秒级点查 + 前缀范围扫描
   → HDFS 只能全表扫描；HBase 用 LSM + 索引块 + Bloom Filter 做到点查

④ 写多读少 / 写吞吐优先：日志、轨迹、时序、消息明细
   → LSM 把随机写转成顺序写，写吞吐远高于 B+ 树
```

四个目标合起来，就是 HBase 最典型的场景画像：

| 场景 | 为什么适合 |
|------|-----------|
| 用户行为日志明细 | 每天数十亿条写入、按用户 + 时间范围查询 |
| 订单/账单流水存档 | 海量行、按订单号点查、按用户前缀扫描 |
| 物联网设备轨迹 | 设备 ID + 时间戳做 RowKey，天然时间有序 |
| 消息/Feed 存储 | 写多读少、按会话 ID 拉取最近 N 条 |
| 画像标签宽表 | 数百万标签列、每用户只填少量 |
| 推荐特征明细 | 按用户/物品 ID 批量点查，离线训练取数 |

---

## 3. 它明确不解决什么

这一节比上一节更重要。HBase 的很多生产事故，来自"把它当关系库用"。

```text
不擅长 ①：多行事务
  HBase 只保证单行原子性（一个 Row 的多个列族写入原子）
  跨行、跨表事务需要额外方案（Phoenix 事务表、或业务层补偿）
  → 转账、库存扣减这类强事务场景，用 MySQL

不擅长 ②：复杂查询
  没有 JOIN、没有子查询、没有任意条件过滤的索引优化
  Scan + Filter 是"服务端逐行判断"，不是索引下推
  → 多表关联分析用 Hive/Spark，检索用 ES

不擅长 ③：聚合与 OLAP
  COUNT/SUM/GROUP BY 都要全表扫
  → 报表与即席分析用 ClickHouse/Doris/Spark

不擅长 ④：二级索引
  原生只有 RowKey 一个索引；按其他字段查 = 全表扫描
  → 需要二级索引时用 Phoenix 全局索引，或自己建索引表

不擅长 ⑤：小数据量
  几十 GB 以下的数据，MySQL + 分库分表更简单
  HBase 的运维成本（ZK、HDFS、Region 管理）在小数据量下不划算

不擅长 ⑥：频繁更新同一行
  HBase 的"更新"是追加新版本，Compaction 时才清理
  高频更新少量行会造成版本堆积与写放大
```

> **警告**：把 HBase 当"无限大的 MySQL"是最常见的误用。它没有事务、没有 JOIN、没有二级索引、没有聚合。选型时先问一句："我的查询能不能只用 RowKey 表达？"不能，就不该选 HBase。

---

## 4. 五层数据模型

HBase 的逻辑模型有五层，从大到小：

```text
Namespace（命名空间）
  └── Table（表）
        └── Row（行，由 RowKey 唯一标识）
              └── Column Family（列族，物理存储单元）
                    └── Column Qualifier（列限定符，列名）
                          └── Cell（单元格：value + timestamp + type）
```

### 4.1 Namespace

命名空间是表的逻辑分组，类似数据库的 schema：

```text
默认命名空间：default
系统命名空间：hbase（存放 meta 表与 namespace 表）

作用：
  ① 权限隔离（按 namespace 授权）
  ② 配额管理（限制 namespace 的 Region 数、存储量、请求量）
  ③ 表名分组（生产/测试、业务线隔离）
```

### 4.2 Table

表由若干行组成，行按 RowKey 字典序全局有序。建表时必须指定至少一个列族：

```text
create_namespace 'app'
create 'app:user_profile', {NAME => 'info', VERSIONS => 3}, {NAME => 'tag', TTL => 2592000}
```

注意：建表时**只定义列族**，不定义列。列（Qualifier）是写入时动态产生的。

### 4.3 Row 与 RowKey

RowKey 是行的唯一标识，也是 HBase 唯一的原生索引：

```text
RowKey 的三条硬规则：
  ① 唯一：同一张表内不能重复
  ② 有序：全表按字节字典序排列（不是哈希）
  ③ 定长友好：越短越好，建议 10-100 字节

RowKey 决定的事：
  - 数据在 Region 中的分布（排序 → 切分）
  - 点查效率（直接定位 Region → HFile → Block）
  - 范围扫描的边界（前缀连续 → 顺序 IO）
```

### 4.4 Column Family 与 Column Qualifier

列族是 HBase 的物理存储单元，一个列族对应一组独立的 HFile、独立的 MemStore、独立的 BlockCache：

```text
表 app:user_profile
├── 列族 info
│     ├── name = "张三"
│     ├── age = "28"
│     └── city = "合肥"
└── 列族 tag
      ├── interest:sports = "1"
      └── interest:music = "1"
```

列族的设计约束（第 05 篇展开）：

- 列族数量要少，通常 1-3 个；每个列族都会带来独立的 MemStore 与刷写。
- 同一列族内的列共享存储与缓存，访问模式应相近。
- 列族名要短（会重复存储在每个 KeyValue 里），如 `info`、`d`、`f`。

### 4.5 Cell

Cell 是 HBase 的最小存储单元，由四元组定位：

```text
{ RowKey, ColumnFamily, ColumnQualifier, Timestamp } → Value

写入示例（HBase Shell）：
  put 'app:user_profile', 'u1001', 'info:name', '张三'
  put 'app:user_profile', 'u1001', 'info:name', '张三丰', 1735689600000

读取示例：
  get 'app:user_profile', 'u1001'
  get 'app:user_profile', 'u1001', {COLUMN => 'info:name', VERSIONS => 2}
```

---

## 5. 稀疏存储与多版本

### 5.1 稀疏：空单元格不占空间

关系库的行是"定宽的记录"，即使某列为 NULL，行头与偏移量仍然存在。HBase 的存储是 KeyValue 列表，只存实际写入的单元格：

```text
关系表（100 万列，每行填 5 列）：
  行存储 → 每行仍要维护 100 万个列的元信息 → 空间爆炸

HBase（100 万列，每行填 5 列）：
  只存 5 个 KeyValue → 空间与"填了几列"成正比，与"定义了几列"无关
```

这就是"宽表"能成立的原因：画像标签可以有几百万个 qualifier，每个用户只填几十个，总存储仍然可控。

### 5.2 多版本：按时间戳保留历史

每次写入都会生成一个新版本（默认取 RegionServer 当前时间戳，也可显式指定）：

```text
put 'app:user_profile', 'u1001', 'info:city', '合肥'          → ts=100
put 'app:user_profile', 'u1001', 'info:city', '上海'          → ts=200

get 默认返回最新版本（上海）
get VERSIONS => 2 返回两个版本：[上海@200, 合肥@100]
```

版本控制有三个旋钮：

| 配置 | 位置 | 含义 |
|------|------|------|
| `VERSIONS` | 列族属性 | 保留多少个版本，默认 1 |
| `MIN_VERSIONS` | 列族属性 | 至少保留多少个版本（TTL 过期时仍保留） |
| `TTL` | 列族属性 | 单元格存活秒数，过期后在 Compaction 时清除 |

> **提示**：版本清理不是实时的。超过 VERSIONS 或 TTL 的数据在 MemStore 刷写与 Compaction 时才真正删除，所以"删除后磁盘没降"是正常现象，需要等 major compaction。

---

## 6. 排序规则与物理含义

HBase 的三层排序决定了所有查询行为：

```text
全局：RowKey 按字节字典序升序
  → Region 按 RowKey 区间切分
  → 前缀相同的 RowKey 物理相邻

列族内：Column Qualifier 按字节字典序升序
  → 同一行的列按 qualifier 排列

单元格内：Timestamp 按降序（最新在前）
  → get 默认取第一个
```

字节序排序带来的两个直接后果：

1. **数字 ID 必须补零或编码**。字符串 `"10"` 排在 `"9"` 前面，因为 `'1' < '9'`。用户 ID 是自增数字时，要么补零到定长，要么用 `Bytes.toBytes(long)` 存 8 字节大端整数。
2. **时间戳要倒序编码**。如果 RowKey 是 `userId + 正序时间戳`，最新数据排在最后；想"最近 N 条优先"通常要 `userId + (Long.MAX_VALUE - ts)` 反转时间戳，让新数据排在前面。

```text
正序：u1001_1735689600 → u1001_1735689700 → u1001_1735689800
      读最近 N 条要 seek 到区间末尾再反向扫描

倒序：u1001_9223372035121262207 → ... → u1001_9223372035121262000
      读最近 N 条直接从区间头开始正向扫描
```

---

## 7. 与关系模型的对照

| 维度 | 关系数据库 | HBase |
|------|-----------|-------|
| 行结构 | 固定 schema，列在建表时定义 | 只有列族固定，列动态 |
| 主键 | 可复合主键，可更新 | RowKey，不可更新（更新 = 删旧行 + 写新行） |
| 索引 | 主键 + 二级索引 + 联合索引 | 仅 RowKey |
| 事务 | 多行 ACID | 单行原子性 |
| 查询 | SQL，优化器选索引 | get / scan / filter，无优化器 |
| 聚合 | GROUP BY、窗口函数 | 无，需客户端或计算引擎 |
| 排序 | ORDER BY 任意列 | 只能按 RowKey 顺序 |
| 扩展 | 垂直 + 分库分表 | Region 自动分裂 + 加 RegionServer |
| 更新 | 原地更新 | 追加新版本，Compaction 清理 |
| 删除 | 原地删除 | 写墓碑标记，Compaction 清理 |

一个具体例子说明差异。需求是"查某用户最近 30 天的订单"：

```text
关系库：
  SELECT * FROM orders WHERE user_id = ? AND created_at >= ? ORDER BY created_at DESC
  → 需要 (user_id, created_at) 联合索引

HBase：
  RowKey = userId + 反转时间戳，Scan 前缀 user_1001_ 取前 30 条
  → 天然按时间倒序，无需额外索引
```

反过来，"查某商品最近 30 天的订单"在 HBase 里就变成全表扫描，因为商品 ID 不在 RowKey 前缀里。这就是 RowKey 设计的全部权衡：**你只能为一个访问模式优化**。

---

## 8. 逻辑模型与物理模型

逻辑上是一张表，物理上被切成多个 Region：

```text
逻辑视图：
  Table app:user_profile
  ├── Row u0001
  ├── Row u0002
  ├── ...
  └── Row u9999

物理视图：
  Table
  ├── Region 1  [u0000, u3333)  → RegionServer A
  ├── Region 2  [u3333, u6666)  → RegionServer B
  └── Region 3  [u6666, u9999]  → RegionServer C

每个 Region 内部：
  ├── 列族 info → MemStore + 多个 HFile（HDFS）
  └── 列族 tag  → MemStore + 多个 HFile（HDFS）
```

关键结论：

- Region 是**按 RowKey 区间**切的，不是按哈希。所以 RowKey 连续写入会集中到一个 Region（写热点）。
- 一个 Region 只属于一个 RegionServer，但一个 RegionServer 可以托管几十到几百个 Region。
- Region 分裂由 RegionServer 自主触发，HMaster 负责迁移与负载均衡。
- 列族是物理隔离的：不同列族的数据在不同 HFile 里，Scan 指定列族可以少读文件。

---

## 9. 类型系统与序列化

HBase 没有类型系统，所有值都是 `byte[]`：

```text
RowKey、列族名、列名、值：全部是字节数组

这意味着：
  ① 数字、日期、对象都要应用层序列化
  ② 排序按字节比较，不是按数值比较
  ③ 同一个 qualifier 可以存不同类型（但应用要自己保证一致）
```

Java 侧的标准做法是用 `Bytes` 工具类：

```java
byte[] rowKey = Bytes.toBytes("u1001");          // 字符串
byte[] age = Bytes.toBytes(28);                  // int，4 字节大端
byte[] ts = Bytes.toBytes(1735689600000L);       // long，8 字节大端
byte[] amount = Bytes.toBytes(new BigDecimal("99.50").toPlainString());
```

序列化选型：

| 类型 | 推荐方式 | 原因 |
|------|---------|------|
| 整数/长整数 | `Bytes.toBytes(int/long)` | 大端定长，字节序即数值序 |
| 浮点 | 转整型（乘精度）或存字符串 | IEEE 754 字节序与数值序不一致 |
| 时间 | long 毫秒 + 反转编码 | 便于时间范围扫描 |
| 金额 | 字符串或 long 分 | 避免浮点误差 |
| 复杂对象 | Protobuf / Avro / JSON | 带 schema 演进能力 |

> **警告**：不要用 Java 原生序列化（`ObjectOutputStream`）存 HBase 值。它体积大、跨语言不可读、版本升级后反序列化失败风险高，且无法参与服务端过滤。

---

## 10. 典型场景与反例

### 10.1 适合的场景画像

```text
判据一：数据量 > 单机关系库舒适区（通常 > 1TB 或 > 10 亿行）
判据二：查询能用 RowKey 表达（点查或前缀范围扫描）
判据三：写入吞吐要求高，或写多读少
判据四：结构稀疏或列动态增长
判据五：能接受最终一致的二级索引与无跨行事务
```

五个判据同时满足，HBase 就是好选择；缺两个以上，先考虑 MySQL + 分库分表、MongoDB、ClickHouse 或 ES。

### 10.2 反例

| 反例 | 问题 | 替代方案 |
|------|------|---------|
| 用 HBase 做订单主库 | 跨行事务缺失，库存/状态流转无法保证 | MySQL + 分库分表 |
| 用 HBase 做后台报表 | 无聚合、无 JOIN，全表扫描慢 | ClickHouse / Doris / Spark SQL |
| 用 HBase 做全文搜索 | 无倒排索引，只能 Filter 逐行判断 | Elasticsearch |
| 用 HBase 存 10GB 配置数据 | 运维成本远高于收益 | MySQL / Redis |
| 高频更新同一批行 | 版本堆积、写放大、Compaction 压力 | Redis 或关系库 |
| 多维度随机查询 | 只能为一个 RowKey 前缀优化 | ES + HBase 双写，或宽表冗余 |

---

## 11. 小结

| 问题 | 答案 |
|------|------|
| HBase 是什么 | HDFS 之上的分布式有序大表，BigTable 开源实现 |
| 核心能力 | RowKey 点查、前缀范围扫描、海量稀疏数据、高写吞吐 |
| 核心限制 | 无跨行事务、无 JOIN、无二级索引、无聚合 |
| 数据模型 | Namespace → Table → Row → ColumnFamily → Qualifier → Cell |
| 排序列 | RowKey 字节序、Qualifier 字节序、Timestamp 降序 |
| 稀疏的含义 | 只存写过的单元格，列定义不占空间 |
| 多版本 | 每次写入追加版本，VERSIONS/TTL 控制保留 |
| 物理切分 | Region 按 RowKey 区间切分，列族物理隔离 |
| 类型系统 | 全部 byte[]，应用层序列化 |

**下一模块**：[02-HBase架构原理与组件协作](02-HBase架构原理与组件协作.md)　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
