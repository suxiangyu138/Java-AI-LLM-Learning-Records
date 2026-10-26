# 01-重链剖分原理与 LCA 模板

> 树链剖分的核心是把"树上的路径"拆成"序列上的区间"。要做到这一点，关键是让重链在 dfn 序上连续。而"任何根到节点的路径最多经过 O(log n) 条轻边"这一性质，保证拆出的区间数量可控。

---

## 目录

1. [问题引入](#1-问题引入)
2. [剖分的定义](#2-剖分的定义)
3. [关键性质与证明](#3-关键性质与证明)
4. [两次 DFS](#4-两次-dfs)
5. [dfn 映射](#5-dfn-映射)
6. [LCA 模板](#6-lca-模板)
7. [完整代码](#7-完整代码)
8. [复杂度分析](#8-复杂度分析)
9. [易错点](#9-易错点)
10. [小结](#10-小结)

---

## 1. 问题引入

树上常见操作：

```text
① 查询 u 到 v 路径上的点权和
② 修改 u 到 v 路径上的所有点权
③ 查询以 u 为根的子树和
④ 求 LCA(u, v)
```

朴素做法：

```text
路径操作：沿着父指针一步步爬，O(路径长度) = O(n)
多次询问 → O(nq)，不可接受
```

树链剖分的思路：

```text
把树拆成若干条"重链"
每条重链在 dfn 序上是一段连续区间
→ 路径被拆成 O(log n) 段区间
→ 每段用线段树处理，单次 O(log n)
→ 总复杂度 O(log² n)
```

---

## 2. 剖分的定义

对每个节点 $u$：

```text
size[u]：以 u 为根的子树节点数
重儿子 son[u]：size 最大的儿子（有多个任选其一）
轻儿子：除重儿子外的儿子
重链：从某节点出发，连续走重儿子形成的链
链头 top[u]：u 所在重链上深度最小的节点
```

一个例子：

```text
        1
      / | \
     2  3  4
    / \    |
   5   6   7
  /
 8

size: 8=1, 5=2, 6=1, 2=4, 3=1, 7=1, 4=2, 1=8
son:  1→2（size 4 最大）, 2→5（size 2 最大）, 4→7

重链：
  1-2-5-8
  3
  4-7
  6

top: 1/2/5/8 的 top=1；3 的 top=3；4/7 的 top=4；6 的 top=6
```

---

## 3. 关键性质与证明

### 3.1 轻边数量上界

**命题**：从根到任意节点的路径上，轻边数量不超过 $\lfloor\log_2 n\rfloor$。

**证明**：

设 $u$ 是 $v$ 的父亲，且 $v$ 是轻儿子。因为 $v$ 不是重儿子，所以存在另一个儿子 $w$ 满足：

$$size[w]\ge size[v]$$

于是：

$$size[u]\ge 1+size[v]+size[w]\ge 1+2\cdot size[v]$$

即：

$$size[v]\le\frac{size[u]-1}{2}<\frac{size[u]}{2}$$

每经过一条轻边，子树大小至少减半。从 $n$ 减到 1 最多经过 $\lfloor\log_2 n\rfloor$ 条轻边。证毕。

### 3.2 路径分段数上界

**命题**：任意路径 $u\to v$ 被拆成不超过 $O(\log n)$ 段重链区间。

**证明**：

路径 $u\to v$ 可以拆成 $u\to lca$ 与 $v\to lca$ 两段。每一段上，每当从一个重链跳到另一条重链，必然经过一条轻边（因为重链内部是连续的，换链只能走轻边）。由 3.1，轻边数量 $O(\log n)$，所以换链次数 $O(\log n)$，区间段数 $O(\log n)$。证毕。

### 3.3 子树区间连续

**命题**：以 $u$ 为根的子树在 dfn 序上对应区间 $[dfn[u],dfn[u]+size[u]-1]$。

**证明**：

DFS 先序遍历时，访问 $u$ 后立即递归其所有儿子，直到子树遍历完才返回。因此 $u$ 的子树恰好占据从 $dfn[u]$ 开始、长度为 $size[u]$ 的连续区间。证毕。

---

## 4. 两次 DFS

### 4.1 第一次 DFS：算 size 与重儿子

```java
private void dfs1(int u, int p) {
    parent[u] = p;
    size[u] = 1;
    int maxSize = 0;
    for (int v : graph[u]) {
        if (v == p) {
            continue;
        }
        depth[v] = depth[u] + 1;
        dfs1(v, u);
        size[u] += size[v];
        if (size[v] > maxSize) {
            maxSize = size[v];
            heavy[u] = v;
        }
    }
}
```

### 4.2 第二次 DFS：分配 dfn 与链头

```java
private void dfs2(int u, int head) {
    top[u] = head;
    dfn[u] = timer;
    rev[timer++] = u;
    if (heavy[u] != -1) {
        dfs2(heavy[u], head);
    }
    for (int v : graph[u]) {
        if (v == parent[u] || v == heavy[u]) {
            continue;
        }
        dfs2(v, v);
    }
}
```

顺序很重要：

```text
必须先递归重儿子，再递归轻儿子
  → 重儿子的 dfn 紧接在 u 后面
  → 整条重链的 dfn 连续

如果先递归轻儿子，重链会被其他子树隔断
  → 路径无法映射成连续区间
```

---

## 5. dfn 映射

剖分后的 dfn 有三个性质：

```text
① 重链连续
   top[u] 到 u 的所有节点 dfn 连续
   → [dfn[top[u]], dfn[u]] 是该链上的一段区间

② 子树连续
   u 的子树占 [dfn[u], dfn[u]+size[u]-1]

③ 深度单调
   同一条重链上，dfn 越大深度越大
```

把树上的点权按 dfn 顺序放进数组：

```java
int[] value = new int[n];
for (int u = 0; u < n; u++) {
    value[dfn[u]] = originalWeight[u];
}
```

之后所有区间操作都在 `value` 上做。

---

## 6. LCA 模板

利用 top 数组求 LCA：

```java
public int lca(int u, int v) {
    while (top[u] != top[v]) {
        if (depth[top[u]] < depth[top[v]]) {
            int temp = u;
            u = v;
            v = temp;
        }
        u = parent[top[u]];
    }
    return depth[u] < depth[v] ? u : v;
}
```

思路：

```text
① 如果 u 与 v 不在同一条链，把"链头更深"的那个跳到链头的父亲
② 重复直到同链
③ 同链时，深度小的就是 LCA
```

复杂度：

```text
每次跳一条重链，最多 O(log n) 次
单次 LCA：O(log n)
```

与倍增 LCA 的对比：

| 方法 | 预处理 | 单次查询 | 代码量 |
|------|-------|---------|-------|
| 倍增 | O(n log n) | O(log n) | 中 |
| 树链剖分 | O(n) | O(log n) | 中（但可复用剖分） |
| Tarjan 离线 | O(n α(n)) | O(1)（离线） | 较高 |

---

## 7. 完整代码

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HeavyLightDecomposition {

    private final List<Integer>[] graph;
    private final int[] parent;
    private final int[] depth;
    private final int[] size;
    private final int[] heavy;
    private final int[] top;
    private final int[] dfn;
    private final int[] rev;
    private int timer;

    @SuppressWarnings("unchecked")
    public HeavyLightDecomposition(int n) {
        graph = new List[n];
        for (int i = 0; i < n; i++) {
            graph[i] = new ArrayList<>();
        }
        parent = new int[n];
        depth = new int[n];
        size = new int[n];
        heavy = new int[n];
        top = new int[n];
        dfn = new int[n];
        rev = new int[n];
        Arrays.fill(heavy, -1);
    }

    public void addEdge(int u, int v) {
        graph[u].add(v);
        graph[v].add(u);
    }

    public void build(int root) {
        dfs1(root, -1);
        dfs2(root, root);
    }

    private void dfs1(int u, int p) {
        parent[u] = p;
        size[u] = 1;
        int maxSize = 0;
        for (int v : graph[u]) {
            if (v == p) {
                continue;
            }
            depth[v] = depth[u] + 1;
            dfs1(v, u);
            size[u] += size[v];
            if (size[v] > maxSize) {
                maxSize = size[v];
                heavy[u] = v;
            }
        }
    }

    private void dfs2(int u, int head) {
        top[u] = head;
        dfn[u] = timer;
        rev[timer++] = u;
        if (heavy[u] != -1) {
            dfs2(heavy[u], head);
        }
        for (int v : graph[u]) {
            if (v == parent[u] || v == heavy[u]) {
                continue;
            }
            dfs2(v, v);
        }
    }

    public int lca(int u, int v) {
        while (top[u] != top[v]) {
            if (depth[top[u]] < depth[top[v]]) {
                int temp = u;
                u = v;
                v = temp;
            }
            u = parent[top[u]];
        }
        return depth[u] < depth[v] ? u : v;
    }

    public int distance(int u, int v) {
        int ancestor = lca(u, v);
        return depth[u] + depth[v] - 2 * depth[ancestor];
    }

    public int dfn(int u) {
        return dfn[u];
    }

    public int rev(int index) {
        return rev[index];
    }

    public int size(int u) {
        return size[u];
    }

    public int top(int u) {
        return top[u];
    }

    public int parent(int u) {
        return parent[u];
    }

    public int depth(int u) {
        return depth[u];
    }
}
```

使用示例：

```java
public static void main(String[] args) {
    HeavyLightDecomposition hld = new HeavyLightDecomposition(8);
    hld.addEdge(0, 1);
    hld.addEdge(0, 2);
    hld.addEdge(0, 3);
    hld.addEdge(1, 4);
    hld.addEdge(1, 5);
    hld.addEdge(3, 6);
    hld.addEdge(4, 7);
    hld.build(0);

    int ancestor = hld.lca(7, 5);
    int dist = hld.distance(7, 6);
}
```

---

## 8. 复杂度分析

| 阶段 | 复杂度 | 说明 |
|------|--------|------|
| 建图 | O(n) | 邻接表 |
| dfs1 | O(n) | 每个点访问一次 |
| dfs2 | O(n) | 每个点访问一次 |
| 单次 LCA | O(log n) | 跳链次数 |
| 单次路径操作 | O(log² n) | O(log n) 段 × 线段树 O(log n) |
| 单次子树操作 | O(log n) | 一段区间 |

---

## 9. 易错点

```text
① dfs2 先轻儿子后重儿子
   → 重链 dfn 不连续，路径查询结果错误

② 比较链头深度时写成比较节点深度
   → 跳错链，死循环或错误 LCA

③ 忘记判断 parent[u] == -1
   → 跳到根的父亲越界

④ 递归深度
   → 链状树 n=10^5 时 Java 默认栈会溢出
   → 改用迭代 DFS 或手动加栈（-Xss64m 只能缓解）

⑤ dfn 从 0 还是 1 开始
   → 与线段树下标保持一致，混用会错位

⑥ 多组数据忘记重置 timer
   → dfn 错乱
```

> **警告**：Java 递归深度受栈大小限制。树链剖分的两次 DFS 在最坏情况下深度为 $n$。$n\ge 10^5$ 时务必改成迭代实现，或在竞赛环境设置 `-Xss64m` 并接受风险。生产代码不要依赖递归深度。

---

## 10. 小结

| 问题 | 答案 |
|------|------|
| 剖分依据 | 子树大小最大的儿子为重儿子 |
| 关键性质 | 根到节点最多 O(log n) 条轻边 |
| dfn 性质 | 重链连续、子树连续 |
| LCA 做法 | 链头不同则跳深的一侧 |
| 复杂度 | 预处理 O(n)，路径操作 O(log² n) |
| 常见错误 | dfs2 顺序、链头深度比较、递归深度 |

**下一模块**：[02-路径查询与子树操作模板](02-路径查询与子树操作模板.md)　**返回总览**：[00-树链剖分知识体系总览](00-树链剖分知识体系总览.md)
