# Cypher查询语言
> 模式匹配、MATCH/CREATE/WHERE、路径查询、聚合与 Cypher 25 新语法：图查询的声明式语言。

---

## 📚 目录

1. [Cypher 概览](#1-cypher-概览)
2. [读写基础](#2-读写基础)
3. [模式匹配](#3-模式匹配)
4. [路径查询](#4-路径查询)
5. [聚合与排序](#5-聚合与排序)
6. [Cypher 25 新特性](#6-cypher-25-新特性)

---

## 1. Cypher 概览

### 1.1 定位

```text
Cypher：Neo4j 的声明式图查询语言（2011 年发布）
  类似 SQL 之于关系型——"描述要什么，不描述怎么做"

特点：
  ① 模式匹配（Pattern）：用图的形状描述查询
  ② 图模式语法：(节点)-[关系]->(节点)
  ③ 声明式：优化器决定执行计划

标准演进：Cypher 25 对齐 GQL（ISO 图查询标准）
  → Cypher 是 GQL 标准的重要来源
```

### 1.2 语法元素

| 元素 | 语法 | 含义 |
|------|------|------|
| 节点 | (n) / (:Person) / (p:Person {name:'张三'}) | 匹配/创建节点 |
| 关系 | --> / -[r:FRIEND]-> / -[r {since:2020}]-> | 有向关系 |
| 任意关系 | --（无方向）/ -[*1..3]->（1-3 跳） | 变长路径 |
| 变量 | n、r、p | 绑定结果引用 |
| Label | :Person | 类型过滤 |
| 属性 | {key: value} | 匹配/写入 |

```text
模式示例（找张三的朋友）：
  (张三)-[:FRIEND]->(朋友)
  → 声明式描述"张三的 FRIEND 关系指向的节点"

变长路径：
  (张三)-[:FRIEND*1..3]->(朋友)   // 1-3 跳好友（好友的好友...）
  → 图查询的核心能力（多跳）
```

---

## 2. 读写基础

### 2.1 创建数据

```cypher
// 创建节点
CREATE (p:Person {name: '张三', age: 30})
CREATE (m:Movie {title: '流浪地球', year: 2019})

// 创建关系（带属性）
MATCH (p:Person {name: '张三'}), (m:Movie {title: '流浪地球'})
CREATE (p)-[:ACTED_IN {role: '主角'}]->(m)

// 合并（幂等：存在则匹配，不存在则创建）
MERGE (p:Person {name: '张三'})
MERGE (p)-[:FRIEND]->(q:Person {name: '李四'})
// MERGE = 图数据库的 UPSERT（推荐用于幂等写入）
```

### 2.2 查询与过滤

```cypher
// 基本查询
MATCH (p:Person) WHERE p.age > 25
RETURN p.name, p.age ORDER BY p.age DESC LIMIT 10;

// 关系过滤
MATCH (p:Person)-[r:FRIEND]->(q:Person)
WHERE r.since > 2020
RETURN p.name, q.name, r.since;

// 属性条件（多种写法）
WHERE p.age > 25 AND p.name STARTS WITH '张'
WHERE p.name IN ['张三', '李四']
WHERE NOT EXISTS (p.email)
```

### 2.3 更新与删除

```cypher
// 更新属性
MATCH (p:Person {name: '张三'})
SET p.age = 31, p.updated = datetime();

// 删除关系
MATCH (p)-[r:FRIEND]->(q) WHERE ...
DELETE r;

// 删除节点（先删关系，或级联）
MATCH (p:Person {name: '张三'})
DETACH DELETE p;        // 级联删除关系+节点
```

---

## 3. 模式匹配

### 3.1 核心机制

```text
模式匹配 = 在图中"找形状"：
  MATCH (a)-[r]->(b)     // 任意有向边
  MATCH (a)-[:X]->(b)-[:Y]->(c)  // 链式模式
  MATCH (a)--(b)         // 无方向
  MATCH (a)-[*1..3]->(b) // 变长路径

执行原理：
  从已知锚点（有属性的节点）出发
  沿模式遍历（免索引邻接加速）
  返回匹配的模式实例

性能关键：
  锚点（起点）的选择 → 索引命中
  模式的约束（Label/属性/方向）→ 缩小遍历
```

### 3.2 常见模式

```cypher
// 三角模式（共同好友）
MATCH (a:Person)-[:FRIEND]->(b:Person)-[:FRIEND]->(c:Person),
      (a)-[:FRIEND]->(c)
RETURN a.name, c.name;      // a 和 c 是"互相关注的共同好友"

// 可选匹配（LEFT JOIN 语义）
MATCH (p:Person)
OPTIONAL MATCH (p)-[:FRIEND]->(f)
RETURN p.name, count(f);    // 无好友的人 count=0

// 路径变量
MATCH path = (a:Person {name:'张三'})-[:FRIEND*1..3]->(b)
RETURN path, length(path);  // 获取完整路径
```

### 3.3 模式中的变量绑定

```text
模式中的变量：
  (a)-[r]->(b)  → a、r、b 均可引用
  MATCH (a)-[r]->(b) RETURN a, type(r), b
  → 关系类型函数：type(r)

聚合中的模式：
  MATCH (p)-[:FRIEND]->(f) RETURN p.name, count(f) AS friends
  → 按 p 分组统计（类似 GROUP BY）

注意：
  模式中重复变量 = 等值约束
  MATCH (a)-[:X]->(b), (a)-[:Y]->(b)  → 同一个 a、b
```

---

## 4. 路径查询

### 4.1 最短路径

```cypher
// 最短路径（BFS 实现）
MATCH (a:Person {name:'张三'}), (b:Person {name:'王五'})
MATCH path = shortestPath((a)-[*..6]->(b))
RETURN path;
// 限制跳数（性能保护）

// 所有最短路径
MATCH path = allShortestPaths((a)-[*..6]->(b))
RETURN path;
```

### 4.2 可达性查询

```cypher
// 可达性（是否存在路径）
MATCH (a {id:1})-[:DEPENDS_ON*1..]->(b {id:100})
RETURN count(*) > 0 AS reachable;
// 适用：依赖链、权限继承、影响分析

// 路径上的节点/关系
MATCH path = (a)-[:X*1..5]->(b)
RETURN [n IN nodes(path) | n.name] AS nodeNames,
       [r IN relationships(path) | type(r)] AS relTypes;
// nodes()/relationships() 提取路径元素
```

### 4.3 路径查询的性能

```text
变长路径的性能特征：
  跳数爆炸：每跳分支因子 b → b^k 条路径
  防护：限制跳数上限（*1..3 而非 *）
       加方向/类型约束
       用 shortestPath（提前终止）

工程经验：
  多跳查询设上限（6 跳内常见）
  高分支图（社交）更要严格限制
  分析型长路径 → 图算法（05 模块）
```

---

## 5. 聚合与排序

### 5.1 聚合函数

| 函数 | 作用 | 示例 |
|------|------|------|
| count() | 计数 | count(f)、count(DISTINCT type) |
| sum/avg/min/max | 数值聚合 | sum(r.weight) |
| collect() | 收集为列表 | collect(f.name) |
| count { } | 子查询计数 | count { MATCH ... } |

```cypher
// 聚合示例：每个人按关系类型分组统计
MATCH (p:Person)-[r]->(m)
RETURN p.name, type(r) AS rel, count(*) AS cnt
ORDER BY cnt DESC;

// collect：聚合为列表（常见于响应组装）
MATCH (p:Person)-[:ACTED_IN]->(m:Movie)
RETURN p.name, collect(m.title) AS movies;
```

### 5.2 子查询与组合

```cypher
// CALL 子查询（Cypher 25 增强）
CALL {
    MATCH (p:Person) RETURN count(p) AS personCount
}
RETURN personCount;

// 并发子查询（2026.06 支持 DISJOINT BY 防死锁）
CALL {
    MATCH (p:Person)
    SET p.processed = true
} IN CONCURRENT TRANSACTIONS
```

### 5.3 条件逻辑（Cypher 25）

```cypher
// WHEN 条件（Cypher 25 新增）
MATCH (p:Person)
RETURN p.name,
  WHEN p.age >= 60 THEN 'senior'
  WHEN p.age >= 18 THEN 'adult'
  ELSE 'minor' END AS ageGroup;
// 替代复杂的 CASE WHEN（更简洁）
```

---

## 6. Cypher 25 新特性

### 6.1 版本演进

```text
Cypher 25（2025 年）的关键变化：
  ① GQL 标准对齐（ISO 图查询语言）
  ② 原生向量搜索语法（SEARCH 语句）
  ③ WHEN 条件逻辑
  ④ 查询组合改进（CALL 子查询）

2026 状态：
  Aura 新组织默认 Cypher 25（2026-03 起）
  Community Edition 同样支持
  → Cypher 25 是当前标准
```

### 6.2 SEARCH 语句（向量搜索）

```cypher
// 向量搜索（原生语法，替代过程调用）
SEARCH docs
WHERE docs.vector_embedding <-> $queryVector <= 0.5
RETURN docs.title, docs.vector_embedding <-> $queryVector AS distance;

// 索引内过滤（2026.02 GA）——见 06 模块
SEARCH docs
WHERE docs.vector_embedding <-> $q <= 0.5
  AND docs.author = '张三'          // 索引内过滤（声明过的属性）
RETURN docs.title;
```

### 6.3 迁移注意

```text
从旧版 Cypher 迁移：
  SEARCH 语句替代 db.index.vector.queryNodes（过程）
  WHEN 替代部分 CASE WHEN
  GQL 别名（新语法兼容层）

兼容性：
  旧查询仍可运行（向后兼容）
  新项目直接用 Cypher 25
  → 学习重点：SEARCH + WHEN + 子查询
```

> 🎯 **核心要点**：Cypher = 用"图的形状"描述查询的声明式语言——模式匹配（MATCH 形状）+ 路径遍历（变长/最短路径）+ 聚合。性能三原则：**锚点要命中索引、模式要加约束（Label/方向/跳数上限）、多跳设上限**。Cypher 25 的标志性新特性是 SEARCH 向量语法与 GQL 对齐——图查询正在标准化。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| Cypher 是什么？ | 声明式图查询语言（模式匹配） |
| 核心语法？ | (节点)-[关系]->(节点) 模式 |
| 多跳怎么查？ | 变长路径 [*1..3] / shortestPath |
| 聚合？ | count/sum/collect + 分组 |
| Cypher 25 新特性？ | SEARCH 向量语法、WHEN、GQL 对齐 |
| 性能三原则？ | 锚点索引、模式约束、跳数上限 |

**下一模块**：[03-索引与性能优化](03-索引与性能优化.md)　**返回总览**：[00-Neo4j知识体系总览](00-Neo4j知识体系总览.md)
