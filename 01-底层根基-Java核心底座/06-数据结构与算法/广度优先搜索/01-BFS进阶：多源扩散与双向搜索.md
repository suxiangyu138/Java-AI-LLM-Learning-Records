# BFS进阶：多源扩散与双向搜索
> 从"单源遍历"到"多源扩散"——BFS 五种进阶形态的深度剖析

## 📚 目录
1. [BFS 的进阶形态总览](#1-bfs-的进阶形态总览)
2. [多源 BFS 详解](#2-多源-bfs-详解)
3. [双向 BFS 详解](#3-双向-bfs-详解)
4. [状态 BFS 与隐式图](#4-状态-bfs-与隐式图)
5. [BFS 与其他算法的结合](#5-bfs-与其他算法的结合)
6. [进阶总结](#6-进阶总结)

---

## 1. BFS 的进阶形态总览

### 1.1 五种形态

```
① 分层 BFS：二叉树层序（size 分层）
② 网格 BFS：方向数组 + 沉岛
③ 多源 BFS：所有起点同时入队（超级源点）
④ 状态 BFS：状态 = 节点，操作 = 边（隐式图）
⑤ 双向 BFS：两端同时扩散（平方级加速）

⚠️ 识别信号：
  "最短/最少/扩散" → BFS
  "多起点" → 多源
  "状态可枚举" → 状态 BFS
  "分支因子大" → 双向 BFS
```

### 1.2 形态选型表

| 形态 | 特征 | 复杂度 | 代表题 |
|------|------|:---:|:---:|
| 分层 | 树/层序 | O(n) | 102 |
| 网格 | 方向数组 | O(mn) | 994/1091 |
| 多源 | 多起点 | O(mn) | 994/542 |
| 状态 | 状态图 | O(N×K) | 127/752 |
| 双向 | 两端扩散 | O(2×b^(d/2)) | 127 优化 |

```
⚠️ 面试第一问："这是什么形态的 BFS？"
  （识别形态 → 套对应模板）
```

### 1.3 为什么 BFS 找最短

```
BFS 按层扩散：第 k 层节点 = 距离起点 k 步
  → 第一次到达目标 = 最短距离

⚠️ 前提：无权图（每条边代价相同）
  带权 → Dijkstra（见最短路专题）

⚠️ 面试必答：
"BFS 按层扩散，第一次到达目标时
 不可能有更短路径——更短的会在
 更早的层被访问。"
```

---

## 2. 多源 BFS 详解

### 2.1 原理：超级源点

```
多源 BFS = 添加一个"虚拟超级源点"：
  超级源点 → 所有初始源点（距离 1）
  从超级源点 BFS → 距离 = 到达最近源点的距离

好处：
  一次 BFS 解决多起点问题
  （逐个 BFS 是 O(k×mn)，多源是 O(mn)）

⚠️ 实现上不需要真的建超级源点：
  所有源点同时入队（距离 0）即可
```

### 2.2 994. 腐烂的橘子（多源计时）

```java
/**
 * LeetCode 994. 腐烂的橘子（多源 BFS 经典）
 * 所有初始烂橘子同时入队 → 逐层扩散计时
 */
public int orangesRotting(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    Queue<int[]> queue = new LinkedList<>();
    int fresh = 0;

    // ① 所有腐烂橘子入队 + 统计新鲜数
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == 2) queue.offer(new int[]{i, j});
            else if (grid[i][j] == 1) fresh++;
        }
    }

    // ② 多源 BFS 扩散
    int minutes = 0;
    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    while (!queue.isEmpty() && fresh > 0) {      // ⚠️ fresh > 0 剪枝
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            int[] cur = queue.poll();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                if (nx < 0 || nx >= m || ny < 0 || ny >= n) continue;
                if (grid[nx][ny] != 1) continue;   // 只感染新鲜

                grid[nx][ny] = 2;                // 腐烂（原地标记）
                fresh--;                         // 新鲜数-1
                queue.offer(new int[]{nx, ny});
            }
        }
        minutes++;                               // 1 分钟 = 1 层
    }
    return fresh == 0 ? minutes : -1;            // 有剩余新鲜 → -1
}
// 推演 [[2,1,1],[1,1,0],[0,1,1]] → 4 分钟 ✓
// ⚠️ 面试必答：
//   "多源 BFS = 虚拟超级源点连所有源——
//    一次 BFS 得到所有位置到最近源的距离。
//    分层计时：每层 = 1 分钟。"
```

### 2.3 多源 BFS 家族

| 题号 | 题目 | 源点 | 结果 |
|:---:|------|:---:|------|
| 994 | 腐烂橘子 | 所有烂橘子 | 扩散时间 |
| 542 | 01 矩阵 | 所有 0 | 最近 0 距离 |
| 1162 | 地图分析 | 所有陆地 | 最远海洋 |
| 286 | 墙与门 | 所有门 | 最近门距离 |

```
⚠️ 统一模板：
  所有源入队（dist 0）→ 逐层扩散 → 距离数组
```

---

## 3. 双向 BFS 详解

### 3.1 为什么双向更快

```
单向 BFS：从起点扩散
  层数 d → 状态数 b^d（b = 分支因子）

双向 BFS：同时从起点和终点扩散
  每侧扩散 d/2 层 → 状态数 2 × b^(d/2)

对比（b=8, d=10）：
  单向：8¹⁰ ≈ 10⁹
  双向：2 × 8⁵ ≈ 2×32768 ≈ 65536
  → 平方级减少！（开根号效果）

⚠️ 面试必答：
"双向 BFS 把状态数从 b^d 降到 2×b^(d/2)——
 平方级减少，适合分支因子大的场景。"
```

### 3.2 127. 单词接龙（双向版）

```java
/**
 * LeetCode 127. 单词接龙（双向 BFS 优化版）
 * 从 beginWord 和 endWord 同时扩散
 */
public int ladderLength(String beginWord, String endWord,
                        List<String> wordList) {
    Set<String> wordSet = new HashSet<>(wordList);
    if (!wordSet.contains(endWord)) return 0;

    Set<String> beginSet = new HashSet<>();
    Set<String> endSet = new HashSet<>();
    Set<String> visited = new HashSet<>();
    beginSet.add(beginWord);
    endSet.add(endWord);

    int steps = 1;
    while (!beginSet.isEmpty() && !endSet.isEmpty()) {
        // ⚠️ 扩散较小的一侧（性能优化）
        if (beginSet.size() > endSet.size()) {
            Set<String> temp = beginSet;
            beginSet = endSet;
            endSet = temp;
        }

        Set<String> nextLevel = new HashSet<>();
        for (String word : beginSet) {
            char[] chars = word.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char original = chars[i];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == original) continue;
                    chars[i] = c;
                    String next = new String(chars);

                    if (endSet.contains(next)) return steps + 1;  // 相遇！

                    if (wordSet.contains(next) && !visited.contains(next)) {
                        visited.add(next);
                        nextLevel.add(next);
                    }
                }
                chars[i] = original;
            }
        }
        beginSet = nextLevel;
        steps++;
    }
    return 0;
}
// ⚠️ 双向 BFS 三要点：
//   ① 两个 Set 分别记录两端当前层
//   ② 每次扩散"较小"的一侧
//   ③ 两侧集合相交 → 找到最短路径
```

### 3.3 双向 BFS 的适用条件

```
✅ 适用：
  起点和终点都明确
  分支因子大（层数深）

❌ 不适用：
  终点不明确（如"访问所有节点"）
  状态空间小（单向即可）

⚠️ 识别：
  "从 A 到 B 的最少步数" + 状态多
  → 双向 BFS 候选
```

---

## 4. 状态 BFS 与隐式图

### 4.1 状态 BFS 的本质

```
状态 BFS：状态 = 节点，操作 = 边
  建模三要素：
    状态：当前局面（单词/密码/位置）
    转移：一次操作能到达的新状态
    目标：满足条件的状态

⚠️ 隐式图：
  边"按需生成"而非预先建图
  例：752 转盘锁 → 每状态生成 8 个邻居
  （显式建图 10⁴ 节点 × 8 边可行，但生成即可）
```

### 4.2 752. 打开转盘锁（状态 BFS）

```java
/**
 * LeetCode 752. 打开转盘锁
 * 状态 = 4 位密码（10⁴ 种）
 * 转移 = 任一位置 +1 或 -1（8 种）
 */
public int openLock(String[] deadends, String target) {
    Set<String> dead = new HashSet<>(Arrays.asList(deadends));
    if (dead.contains("0000")) return -1;

    Queue<String> queue = new LinkedList<>();
    Set<String> visited = new HashSet<>();
    queue.offer("0000");
    visited.add("0000");

    int steps = 0;
    while (!queue.isEmpty()) {
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            String cur = queue.poll();
            if (cur.equals(target)) return steps;

            for (String next : getNeighbors(cur)) {
                if (dead.contains(next)) continue;    // 死亡密码跳过
                if (visited.contains(next)) continue;
                visited.add(next);
                queue.offer(next);
            }
        }
        steps++;
    }
    return -1;
}

private List<String> getNeighbors(String code) {
    List<String> neighbors = new ArrayList<>();
    char[] chars = code.toCharArray();
    for (int i = 0; i < 4; i++) {
        char original = chars[i];
        chars[i] = (char) ((original - '0' + 1) % 10 + '0');   // +1
        neighbors.add(new String(chars));
        chars[i] = (char) ((original - '0' + 9) % 10 + '0');   // -1
        neighbors.add(new String(chars));
        chars[i] = original;        // ⚠️ 还原
    }
    return neighbors;
}
// ⚠️ 面试第一句：
//   "状态空间 10⁴，每状态 8 转移 →
//    O(8×10⁴) 可行（先评估再写！）"
```

### 4.3 状态 BFS 的可行性评估

```
状态数 × 转移数 可控 → BFS 可行
  752 转盘锁：10⁴ × 8 ✓
  127 单词接龙：n × 26L ✓
  10 位密码：10¹⁰ ✗（爆炸！）

⚠️ 面试必答：
"状态数 N，每状态 k 转移 →
 总复杂度 O(N×k)。
 先评估可行性再写代码。"
```

---

## 5. BFS 与其他算法的结合

### 5.1 BFS + 拓扑（Kahn）

```
Kahn 拓扑排序 = BFS + 入度：
  入度 0 入队 → 出队减入度 → 减到 0 再入队
  处理数 == n → 无环

⚠️ 关系：
  拓扑排序的队列操作 = BFS 模板
  （详见拓扑排序专题）
```

### 5.2 BFS + 二分（答案二分变体）

```
网格"最大最小"类问题：
  二分答案 + BFS 验证连通性
  例：778 水位上升的泳池
  （二分水位 → BFS 能否从起点到终点）

⚠️ 识别：
  "最小化最大值" + 网格 → 二分 + BFS
```

### 5.3 BFS + DP（状态扩展）

```
网格 + 附加状态：
  1293 网格最短路径（最多消除 k 个障碍）
  状态 = (x, y, 剩余消除次数)
  → visited 三维数组

⚠️ 识别：
  "网格 + 限制条件" → 状态扩展 BFS
```

---

## 6. 进阶总结

### 6.1 进阶要点速查

```
① 五种形态：分层/网格/多源/状态/双向
② 多源：所有源入队 = 超级源点（994/542）
③ 双向：b^d → 2×b^(d/2)（127）
④ 状态：状态=节点 + 可行性评估（10⁴×8）
⑤ 结合：拓扑（入度）/二分（答案）/DP（状态扩展）
```

### 6.2 面试话术模板

```
"这是【形态】BFS：
 ① 识别【多源/状态/双向】特征；
 ② 模板【所有源入队/状态枚举/两端扩散】；
 ③ 可行性【状态数×转移数】评估；
 ④ 复杂度【O(mn)/O(N×K)】。
 关键细节：【入队即标记/size 分层】。"
```

### 6.3 易错点清单

| 错误 | 后果 | 修正 |
|------|------|------|
| visited 出队标记 | 重复入队死循环 | 入队即标记 |
| 多源忘全入队 | 结果错误 | 所有源入队 |
| 双向忘换小侧 | 性能差 | 扩散较小一侧 |
| 状态数爆炸 | 超时 | 先评估 |
| 状态还原漏 | 邻居生成错 | 还原 char[] |

> 🎯 **核心要点**：BFS 进阶 = **多源**（超级源点）+ **双向**（平方级加速）+ **状态**（隐式图 + 可行性评估）。"入队即标记"是铁律；"先评估状态空间"是状态 BFS 第一句；双向 BFS 的 b^d → 2×b^(d/2) 论证是面试必答。

---

**返回总览**：[00-算法汇总知识体系总览](../00-算法汇总知识体系总览.md) | **上一篇**：[00-广度优先搜索专题精要](00-广度优先搜索专题精要.md) | **下一篇**：[02-广度优先搜索高频题解](02-广度优先搜索高频题解.md)
