# 02 - 执行计划 EXPLAIN 深度

> **核心摘要**：EXPLAIN 是调优的第一工具——**type（访问方式）金字塔、key（用哪个索引）、rows（估算行数）、Extra（关键提示）**四要素定生死。本文给出全字段解读、常见执行计划速查表、EXPLAIN ANALYZE（8.0.18+ 真实执行）实战。

> **前置阅读**：[[01-SQL执行原理深度]]

---

## 📚 目录

1. [EXPLAIN 基本用法](#1-explain-基本用法)
2. [四要素：type / key / rows / Extra](#2-四要素type--key--rows--extra)
3. [type 金字塔详解](#3-type-金字塔详解)
4. [Extra 关键值解读](#4-extra-关键值解读)
5. [完整案例分析](#5-完整案例分析)
6. [EXPLAIN ANALYZE：真实执行](#6-explain-analyze真实执行)
7. [核心要点](#7-核心要点)

---

## 1. EXPLAIN 基本用法

> **背景**：SQL 慢 → 第一反应 EXPLAIN → 看执行计划。
> **目的**：掌握 EXPLAIN 系列命令与输出结构。
> **适用范围**：所有 SQL 调优的第一现场。

```sql
-- 基本用法（MySQL 8.0+/9.x）
EXPLAIN SELECT * FROM t_order WHERE user_id = 10086;
-- 输出 12+ 列：id/select_type/table/partitions/type/possible_keys
--            /key/key_len/ref/rows/filtered/Extra

-- 查看格式化输出（JSON 更详细）
EXPLAIN FORMAT=JSON SELECT ...;

-- 查看树状输出（8.0.16+，更易读）
EXPLAIN FORMAT=TREE SELECT ...;

-- 真实执行 + 实际耗时（8.0.18+，最强诊断）
EXPLAIN ANALYZE SELECT ...;
-- 注意：EXPLAIN ANALYZE 会真实执行 SQL！（只读语句安全）
```

> ⚠️ **EXPLAIN 与 EXPLAIN ANALYZE 的区别**：EXPLAIN 是**估算**（来自统计信息，可能不准）；EXPLAIN ANALYZE 是**实测**（真实执行 + 实际行数 + 实际耗时）——调优验证用 ANALYZE，日常看估算用 EXPLAIN。

---

## 2. 四要素：type / key / rows / Extra

> 🎯 **看执行计划只看四列**（先看这四列就能判断 90% 问题）：

| 列 | 含义 | 好/坏判断 |
|----|------|-----------|
| **type** | 访问方式（怎么找数据） | 目标 ≥ range；ALL = 全表扫描（警惕） |
| **key** | 实际用的索引 | NULL = 没用索引 |
| **rows** | 估算扫描行数 | 越大越慢（对比实际行数判断估算准不准） |
| **Extra** | 附加信息（关键提示） | Using filesort/Using temporary 是红灯 |

```text
一分钟诊断流程
├── ① type 是什么？ALL → 看 key 为什么没用（下一节）
├── ② key 用了但 rows 很大？→ 选择性差/索引设计问题
├── ③ Extra 有 filesort/temporary？→ 排序/分组没走索引
├── ④ filtered 低？→ 过滤比例低（回表多）
└── ⑤ 多层 id？→ 子查询/派生表（看 select_type）
```

---

## 3. type 金字塔详解

> 🎯 **type 从好到坏的金字塔**——目标是让查询落在 `range` 及以上：

```text
访问方式金字塔（从最优到最差）
┌─────────────────────────────────────────┐
│ system  仅一行（系统表）——最优            │
│ const   主键/唯一键等值（最多 1 行）       │
│ eq_ref  联表时按主键/唯一键关联（每行匹配 1）│
│ ref     普通索引等值（可多行）             │
│ range   索引范围（BETWEEN/IN/></=）       │
│ index   全索引扫描（遍历整个索引）          │
│ ALL     全表扫描（最差，必须警惕）          │
└─────────────────────────────────────────┘
                         ↑ 目标区间
                         ↓ 警告区间
```

**type 详解与优化方向**：

| type | 含义 | 典型 SQL | 优化方向 |
|------|------|---------|---------|
| `const` | 主键/唯一等值 | `WHERE id = 100` | ✅ 理想（走了唯一索引） |
| `eq_ref` | 联表唯一匹配 | 主表 JOIN 从表 on 主键 | ✅ JOIN 正常形态 |
| `ref` | 普通索引等值 | `WHERE user_id = 10086` | ✅ 常规好状态 |
| `range` | 索引范围 | `WHERE create_time BETWEEN ...` | ✅ 可接受 |
| `index` | 全索引扫描 | 覆盖索引的 GROUP BY | ⚠️ 比 ALL 好但全遍历 |
| `ALL` | 全表扫描 | 无索引/选择性差 | ❌ 加索引/改 SQL |

> 💡 **ref 与 eq_ref 别混淆（面试常考）**：`eq_ref` 是**每行只匹配一个**（主键/唯一键关联）；`ref` 是**普通索引可能匹配多行**（非唯一索引）。eq_ref 出现在「主表 JOIN 副表走主键」——JOIN 性能最好的形态。

---

## 4. Extra 关键值解读

> 🎯 **Extra 列是「执行计划的红绿灯」**——出现红灯值必须处理：

```text
🚨 红灯（性能杀手，必须优化）
├── Using filesort：排序没走索引（文件排序）
│   ├── 场景：ORDER BY 不满足索引顺序
│   └── 对策：调整索引（(a,b) 索引配 ORDER BY a,b）或改查询
├── Using temporary：用了临时表（GROUP BY/DISTINCT/子查询）
│   ├── 场景：GROUP BY 无索引/多表聚合
│   └── 对策：索引覆盖分组列
└── Using join buffer：JOIN 用了缓存（块嵌套循环——小表先加载）

🟡 黄灯（了解含义）
├── Using index：覆盖索引（无需回表）——✅ 其实是好信号！
├── Using index condition：ICP 索引下推（8.0 特性）
│   └── 索引层过滤后回表（比直接回表好）
├── Using where：存储引擎层后过滤（索引条件之外）
├── Using index for group-by：GROUP BY 走索引（好）
└── Using MRR：多范围读优化（回表批量）

🟢 绿灯（好状态）
├── Using index（覆盖索引）——查询所需列全在索引里
└── NULL（无特殊操作）
```

**filesort 与索引排序的对比（必考）**：

```text
场景：WHERE a = 1 ORDER BY b
├── 索引 (a, b)：先按 a 定位，再按 b 顺序取——已有序，无 filesort ✅
├── 索引 (a) 单独：取回多行 b 无序 → filesort 排序 ❌
└── 金句：联合索引「查询条件 + 排序条件」组合设计——一石二鸟
```

---

## 5. 完整案例分析

> 🎯 **一个真实调优案例全流程**——从慢 SQL 到执行计划到修复：

```sql
-- 慢 SQL（业务反馈：订单列表 3 秒+）
SELECT id, order_no, amount, status
FROM t_order
WHERE user_id = 10086
  AND create_time > '2026-01-01'
ORDER BY create_time DESC
LIMIT 10;
```

```text
第一步：EXPLAIN 看现状
├── type: ALL            ← 全表扫描！
├── key: NULL            ← 没有可用索引
├── rows: 1,200,000      ← 扫描 120 万行
└── Extra: Using filesort ← 排序还走了文件排序
→ 问题确认：无索引导致全表 + 文件排序

第二步：分析条件
├── 等值条件：user_id（高选择性）
├── 范围条件：create_time
├── 排序：create_time DESC
└── 查询列：id/order_no/amount/status（需回表）

第三步：设计索引（联合索引 (user_id, create_time)）
├── user_id 等值定位 → create_time 范围过滤（已在索引内有序）
├── ORDER BY create_time 直接用索引顺序 → 无 filesort
└── 回表取 amount/status（无法避免——除非覆盖索引）

第四步：验证
ALTER TABLE t_order ADD INDEX idx_user_time (user_id, create_time);
EXPLAIN 复查：
├── type: range        ← 索引范围扫描
├── key: idx_user_time ← 用上新索引
├── rows: 120          ← 扫描从 120 万 → 120
└── Extra: (无 filesort) ← 排序走索引
→ 实际耗时：3s → 20ms
```

**复盘要点**：

```text
├── 索引设计顺序：等值条件 → 范围条件 → 排序 → 分组
├── 范围条件放最后：联合索引 (user_id, create_time) 中
│   create_time 范围用完后，后面列无法再用（最左前缀）
├── 排序与过滤共用索引：一石二鸟的设计
└── 回表不可避免时：看查询列能否并入索引（覆盖索引权衡）
```

---

## 6. EXPLAIN ANALYZE：真实执行

> 🎯 **EXPLAIN ANALYZE（8.0.18+）= 估算 + 实测的终极结合**——直接看真实执行路径和耗时：

```sql
EXPLAIN ANALYZE
SELECT o.id, o.amount
FROM t_order o
JOIN t_user u ON o.user_id = u.id
WHERE u.level = 'VIP'
LIMIT 100;
```

```text
输出示例（FORMAT=TREE 风格）
-> Limit: 100 row(s)  (actual time=1.234..5.678 rows=100 loops=1)
    -> Nested loop inner join  (cost=2834 rows=300) (actual time=... rows=100 loops=1)
        -> Filter: (u.level = 'VIP')  (actual time=... rows=500 loops=1)
            -> Table scan on u  (cost=... rows=5000) (actual time=...)
        -> Index lookup on o using idx_user_id (user_id=u.id)
           (actual time=... rows=1 loops=500)

解读要点
├── actual time：每步真实耗时（估算 vs 实测对照）
├── actual rows：真实行数（对比估算 rows——偏差大 = 统计信息问题）
├── loops：循环次数（JOIN 驱动次数——小表驱动大表验证）
└── 定位「慢在哪一步」：时间最大的节点就是瓶颈
```

> 💡 **实测 vs 估算对照的价值**：`rows=300（估算）` 但 `actual rows=30000` → 统计信息严重失真 → ANALYZE TABLE 先修数据，再谈优化。

---

## 7. 核心要点

> 🎯 **核心要点**：
> 1. **四要素诊断**：type（怎么找）→ key（用没用）→ rows（扫多少）→ Extra（有啥坑）
> 2. **type 金字塔**：目标是 range 及以上；ALL/index 是警告
> 3. **eq_ref vs ref**：eq_ref 主键联表（每行唯一匹配）性能最优形态
> 4. **Extra 红灯**：filesort（排序没走索引）/temporary（临时表）——联合索引一石二鸟
> 5. **索引设计顺序**：等值 → 范围 → 排序 → 分组（范围条件后不能再接力）
> 6. **EXPLAIN ANALYZE 实测**：估算 vs 实际对照——统计信息失真一眼看穿
> 7. **调优闭环**：EXPLAIN 找问题 → 建索引 → 复查 EXPLAIN → 实测验证

---

**下一模块**：[03-索引优化实战](03-索引优化实战.md) | **返回总览**：[00-SQL调优知识体系总览](00-SQL调优知识体系总览.md)
