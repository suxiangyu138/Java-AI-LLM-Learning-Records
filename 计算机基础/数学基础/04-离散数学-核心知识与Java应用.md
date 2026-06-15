# 04 — 离散数学：核心知识与 Java 应用

> **文档版本：** v2.0 | **最后更新：** 2026-06-13 | **优先级：** ★★★★☆（后端拓展）
>
> **一句话总结：** 离散数学是计算机科学的数学语言——数据库关系模型、编译器的语法分析、微服务的 DAG 调度，其背后都是数理逻辑、集合论和图论。

---

## 目录

1. [课程定位与资源](#1-课程定位与资源)
2. [数理逻辑](#2-数理逻辑)
3. [集合论](#3-集合论)
4. [关系与函数](#4-关系与函数)
5. [图论](#5-图论)
6. [组合数学精要](#6-组合数学精要)
7. [Java 工程中的离散数学](#7-java-工程中的离散数学)

---

## 1. 课程定位与资源

### 1.1 为什么 Java 后端开发者需要离散数学

| 工程场景 | 离散数学对应 | 实际收益 |
|----------|------------|---------|
| **数据库查询优化** | 谓词逻辑、关系代数 | 写出更高效的 SQL |
| **Spring Bean 依赖** | DAG + 拓扑排序 | 理解启动顺序和循环依赖检测 |
| **垃圾回收算法** | 图的可达性分析 | 理解 GC Roots 追踪原理 |
| **微服务调用链** | 最短路径、最小生成树 | 优化服务拓扑 |
| **算法设计与证明** | 数学归纳法、递推关系 | 证明递归算法的正确性 |
| **权限系统设计** | 偏序关系 | RBAC 权限继承模型 |

### 1.2 推荐资源

| 类型 | 资源 | 说明 |
|------|------|------|
| **系统** | MIT 6.042J 《Mathematics for Computer Science》 | 计算机科学中的数学，强烈推荐 |
| **教材** | 《离散数学及其应用》(Rosen, 第 8 版) | 经典教材，习题丰富 |
| **进阶** | 《具体数学》(Graham, Knuth, Patashnik) | 算法分析中的数学 |

---

## 2. 数理逻辑

### 2.1 命题逻辑

| 联结词 | 符号 | 含义 | 真值条件 | Java 对应 |
|--------|------|------|----------|-----------|
| 否定 | ¬ | 非 | ¬P 为真 ↔ P 为假 | `!p` |
| 合取 | ∧ | 且 | P∧Q 为真 ↔ P,Q 同时为真 | `p && q` |
| 析取 | ∨ | 或 | P∨Q 为真 ↔ P 或 Q 为真 | `p \|\| q` |
| 蕴含 | → | 若...则... | P→Q 为假 ↔ P=1,Q=0 | `!p \|\| q` |
| 等价 | ↔ | 当且仅当 | P↔Q 为真 ↔ P,Q 同值 | `p == q` |

### 2.2 谓词逻辑

```text
命题逻辑的限制：不能表达"所有"和"存在"

谓词逻辑的量化：
  ∀x P(x)  — 对于所有 x，P(x) 成立
  ∃x P(x)  — 存在某个 x，使 P(x) 成立

量词否定规则：
  ¬∀x P(x) ≡ ∃x ¬P(x)
  ¬∃x P(x) ≡ ∀x ¬P(x)
```

### 2.3 数据库查询中的谓词逻辑

```sql
-- "所有年龄>20 的用户" → ∀x(用户(x) ∧ 年龄(x)>20)
SELECT * FROM users WHERE age > 20;

-- "存在一笔金额>1000 的订单" → ∃x(订单(x) ∧ 金额(x)>1000)
SELECT * FROM users u WHERE EXISTS (
    SELECT 1 FROM orders o WHERE o.user_id = u.id AND amount > 1000
);

-- "不存在任何金额>10000 的订单的用户" → 对应 NOT EXISTS
SELECT * FROM users u WHERE NOT EXISTS (
    SELECT 1 FROM orders o WHERE o.user_id = u.id AND amount > 10000
);
```

### 2.4 推理规则

| 规则 | 形式 | 示例 |
|------|------|------|
| **假言推理** (Modus Ponens) | `P→Q, P ∴ Q` | 下雨→地湿，下雨了 → 地湿 |
| **拒取式** (Modus Tollens) | `P→Q, ¬Q ∴ ¬P` | 下雨→地湿，地不湿 → 没下雨 |
| **析取三段论** | `P∨Q, ¬P ∴ Q` | 咖啡或茶，不要咖啡 → 茶 |
| **假言三段论** | `P→Q, Q→R ∴ P→R` | A→B, B→C ∴ A→C |

---

## 3. 集合论

### 3.1 基本运算与 Java 映射

| 运算 | 符号 | 定义 | Java `Set` API |
|------|------|------|---------------|
| 并集 | `A ∪ B` | `{x \| x∈A ∨ x∈B}` | `set.addAll(other)` |
| 交集 | `A ∩ B` | `{x \| x∈A ∧ x∈B}` | `set.retainAll(other)` |
| 差集 | `A - B` | `{x \| x∈A ∧ x∉B}` | `set.removeAll(other)` |
| 补集 | `A̅` | `{x \| x∉A}`（相对 U） | 需构造全集 |

```java
Set<Integer> a = new HashSet<>(Set.of(1, 2, 3));
Set<Integer> b = new HashSet<>(Set.of(2, 3, 4));

// 并集
Set<Integer> union = new HashSet<>(a); union.addAll(b);     // {1,2,3,4}
// 交集
Set<Integer> inter = new HashSet<>(a); inter.retainAll(b);  // {2,3}
// 差集
Set<Integer> diff = new HashSet<>(a); diff.removeAll(b);    // {1}
```

### 3.2 集合恒等式

```text
德摩根律：  (A ∪ B)̅ = A̅ ∩ B̅      (A ∩ B)̅ = A̅ ∪ B̅
分配律：    A ∩ (B ∪ C) = (A ∩ B) ∪ (A ∩ C)
           A ∪ (B ∩ C) = (A ∪ B) ∩ (A ∪ C)
```

### 3.3 容斥原理

| 规模 | 公式 |
|------|------|
| 两集合 | `|A ∪ B| = |A| + |B| - |A ∩ B|` |
| 三集合 | `|A∪B∪C| = |A|+|B|+|C| - |A∩B|-|B∩C|-|A∩C| + |A∩B∩C|` |
| n 集合 | 交错加减，符号为 `(-1)^{k+1}` |

---

## 4. 关系与函数

### 4.1 二元关系的性质

| 性质 | 定义 | 示例 |
|------|------|------|
| **自反性** | `∀a, aRa` | `=`, `≤` |
| **对称性** | `aRb → bRa` | `=`, 朋友关系 |
| **反对称性** | `aRb ∧ bRa → a=b` | `≤`, `⊆` |
| **传递性** | `aRb ∧ bRc → aRc` | `=`, `≤`, 祖先关系 |

```text
等价关系 = 自反 + 对称 + 传递    → 分类（如 hashCode 相等分区）
偏序关系 = 自反 + 反对称 + 传递  → 排序（如 Comparable 接口）
```

### 4.2 关系与数据库的对应

```text
数据库表  = 关系 R 的实例        ⊆ D₁ × D₂ × ... × Dₙ（笛卡尔积子集）
SQL JOIN  = 关系代数中的连接 (⋈)
WHERE     = 选择运算 (σ)
外键      = 参照完整性约束
```

### 4.3 函数的分类

| 性质 | 定义 | Java 视角 |
|------|------|----------|
| **单射** | `f(a₁)=f(a₂) → a₁=a₂` | 不同输入必有不同输出 |
| **满射** | `∀b, ∃a: f(a)=b` | 值域 = 陪域 |
| **双射** | 单射+满射 | 可逆映射（encode ↔ decode） |

---

## 5. 图论

### 5.1 基本概念速查

| 术语 | 定义 |
|------|------|
| 简单图 | 无自环、无多重边 |
| 完全图 Kₙ | 每对不同顶点间都有边 |
| 二分图 | 顶点可分两个不相交集，所有边跨集合 |
| 握手定理 | `Σ deg(v) = 2E` |

### 5.2 核心算法

| 问题 | 算法 | 复杂度 | 工程场景 |
|------|------|--------|---------|
| 单源最短路径 | Dijkstra | `O((V+E)logV)` | 微服务路由 |
| 全源最短路径 | Floyd-Warshall | `O(V³)` | 网络拓扑分析 |
| 最小生成树 | Kruskal | `O(E log E)` | 网络布线优化 |
| 最小生成树 | Prim | `O((V+E)logV)` | 稠密图 |
| 拓扑排序 | Kahn | `O(V+E)` | **依赖解析** |

### 5.3 DAG 与拓扑排序（最高频的工程应用）

```text
拓扑排序 = 将 DAG 中的顶点排成线性序列，使每条边 (u→v) 中 u 在 v 之前

算法（Kahn）：
  1. 计算所有顶点入度
  2. 入度为 0 的入队
  3. 每次出队 v → 加入结果 → v 的所有后继入度减 1
  4. 若结果数 < 顶点数 → 存在环（不是 DAG）
```

```java
List<String> topologicalSort(Map<String, Set<String>> graph) {
    Map<String, Integer> inDegree = new HashMap<>();
    for (String node : graph.keySet()) inDegree.putIfAbsent(node, 0);
    for (var e : graph.entrySet())
        for (String nb : e.getValue())
            inDegree.merge(nb, 1, Integer::sum);

    Queue<String> q = new ArrayDeque<>();
    inDegree.forEach((k, v) -> { if (v == 0) q.offer(k); });

    List<String> result = new ArrayList<>();
    while (!q.isEmpty()) {
        String node = q.poll();
        result.add(node);
        for (String nb : graph.getOrDefault(node, Set.of()))
            if (inDegree.merge(nb, -1, Integer::sum) == 0)
                q.offer(nb);
    }
    if (result.size() != inDegree.size())
        throw new RuntimeException("Cycle detected!");
    return result;
}
```

### 5.4 图论工程应用对照表

| 图论概念 | Java 工程应用 |
|----------|-------------|
| **DAG + 拓扑排序** | Maven 依赖、Spring Bean 初始化、Spark 执行计划 |
| **最短路径** | 微服务调用链优化、API 网关路由 |
| **最小生成树** | 网络拓扑设计 |
| **二分图匹配** | 任务-资源分配 |
| **图着色** | 寄存器分配（编译器） |
| **强连通分量** | 循环依赖检测 |
| **可达性分析** | GC Roots 追踪（JVM GC 核心） |

---

## 6. 组合数学精要

### 6.1 排列与组合

| 概念 | 公式 | 说明 |
|------|------|------|
| 排列 P(n,r) | `n!/(n-r)!` | 有序选取 r 个 |
| 组合 C(n,r) | `n!/(r!(n-r)!)` | 无序选取 r 个 |
| 可重复排列 | `nʳ` | 允许重复的有序选取 |
| 可重复组合 | `C(n+r-1, r)` | 允许重复的无序选取 |

```java
// 组合数递推（杨辉三角）
long[][] C = new long[n+1][n+1];
for (int i = 0; i <= n; i++) {
    C[i][0] = C[i][i] = 1;
    for (int j = 1; j < i; j++)
        C[i][j] = C[i-1][j-1] + C[i-1][j];  // 帕斯卡公式
}
```

### 6.2 鸽巢原理

```text
基本：n+1 只鸽子 → n 个巢 → 至少一个巢有 ≥2 只
推广：k 只鸽子 → n 个巢 → 至少一个巢有 ≥⌈k/n⌉ 只

工程应用：哈希冲突必然存在（无限输入 → 有限哈希空间）
```

### 6.3 经典递推关系

| 问题 | 递推式 | 通解 |
|------|--------|------|
| 斐波那契 | `Fₙ = Fₙ₋₁ + Fₙ₋₂` | `Fₙ = (φⁿ - ψⁿ)/√5` |
| 汉诺塔 | `Tₙ = 2Tₙ₋₁ + 1` | `Tₙ = 2ⁿ - 1` |
| 卡特兰数 | `Cₙ = Σ CᵢC_{n-1-i}` | `Cₙ = C(2n,n)/(n+1)` |

---

## 7. Java 工程中的离散数学

### 7.1 集合运算：流失用户分析

```java
Set<Long> yesterdayUsers = getActiveUsers(yesterday);
Set<Long> todayUsers     = getActiveUsers(today);

// 差集 = 流失用户
Set<Long> churned = new HashSet<>(yesterdayUsers);
churned.removeAll(todayUsers);

// 交集 = 连续活跃
Set<Long> retained = new HashSet<>(yesterdayUsers);
retained.retainAll(todayUsers);

double churnRate = (double) churned.size() / yesterdayUsers.size();
```

### 7.2 BFS：服务下游依赖发现

```java
Set<String> findDownstream(String start, Map<String, Set<String>> deps) {
    Set<String> visited = new HashSet<>();
    Queue<String> q = new ArrayDeque<>();
    q.offer(start);
    visited.add(start);
    while (!q.isEmpty()) {
        for (String ds : deps.getOrDefault(q.poll(), Set.of()))
            if (visited.add(ds)) q.offer(ds);
    }
    visited.remove(start);
    return visited;
}
```

---

## 关键公式速记卡

| 公式 | 用途 |
|------|------|
| `C(n,k) = C(n-1,k-1) + C(n-1,k)` | 杨辉三角递推 |
| `Σ deg(v) = 2E` | 握手定理 |
| `|A ∪ B| = |A| + |B| - |A ∩ B|` | 容斥原理 |
| `Tₙ = 2Tₙ₋₁ + 1 → 2ⁿ - 1` | 汉诺塔通解 |

---

## 关联文档

- [[01-线性代数-核心知识与AI应用]] —— 图的邻接矩阵表示
- [[08-卡特兰数核心知识点]] —— 卡特兰数专题
- [[09-组合数学核心知识点]] —— 排列组合速查
- [[00-数学基础总览]] —— 返回总览
