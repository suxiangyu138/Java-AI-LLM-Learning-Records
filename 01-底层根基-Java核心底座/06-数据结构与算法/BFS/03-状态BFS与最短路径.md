# 状态BFS与最短路径
> 状态即节点、转移即边——把"最少操作"问题建模成状态图上的 BFS

## 📚 目录
1. [状态 BFS 的思想](#1-状态-bfs-的思想)
2. [127. 单词接龙](#2-127-单词接龙)
3. [752. 打开转盘锁](#3-752-打开转盘锁)
4. [909. 蛇梯棋](#4-909-蛇梯棋)
5. [状态 BFS 建模总结](#5-状态-bfs-建模总结)

---

## 1. 状态 BFS 的思想

### 1.1 什么是状态 BFS

```
普通 BFS：节点是图上的点（树节点/网格格子）
状态 BFS：节点是"问题的状态"，边是"一次操作"

建模三要素：
  状态：当前局面（单词/密码/位置+属性）
  转移：一次操作能到达的新状态
  目标：满足条件的状态

例：752 转盘锁
  状态：4 位密码（0000-9999）
  转移：任一数字 +1 或 -1（8 种）
  目标：打开锁的密码（避开 deadends）
```

### 1.2 状态 BFS 的特征

```
"最少操作次数/最少步数" + "状态可枚举/可编码" → 状态 BFS

常见状态编码：
  字符串（单词/密码）→ String 直接做状态
  位置 + 属性 → int[] 或打包成 int
  集合 → 位掩码（状态压缩）

状态数估计（面试必讲）：
  转盘锁：10⁴ 种状态 → BFS 可行
  单词接龙：单词数量 n → BFS 可行
  ⚠️ 状态数巨大时（如 2ⁿ）BFS 不可行 → 换思路
```

---

## 2. 127. 单词接龙

### 2.1 完整实现（标准 BFS）

```java
/**
 * LeetCode 127. 单词接龙（Hot 100 Hard）
 * 从 beginWord 到 endWord，每次只能改一个字母，
 * 且中间词必须在 wordList 中，求最短转换序列长度
 *
 * 状态：单词；转移：改一个字母且在字典中的词
 * BFS 逐层扩散 → 第一次到 endWord 即最短
 */
public int ladderLength(String beginWord, String endWord,
                        List<String> wordList) {
    Set<String> wordSet = new HashSet<>(wordList);
    if (!wordSet.contains(endWord)) return 0;    // 终点不在字典

    Queue<String> queue = new LinkedList<>();
    queue.offer(beginWord);
    wordSet.remove(beginWord);        // 入队即标记（从字典移除）

    int steps = 1;                    // 起点算第 1 层

    while (!queue.isEmpty()) {
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            String word = queue.poll();
            if (word.equals(endWord)) return steps;   // 到达终点

            // 生成所有"改一个字母"的邻居
            char[] chars = word.toCharArray();
            for (int j = 0; j < chars.length; j++) {
                char original = chars[j];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == original) continue;
                    chars[j] = c;
                    String next = new String(chars);
                    if (wordSet.contains(next)) {    // 在字典中
                        wordSet.remove(next);        // 入队即标记
                        queue.offer(next);
                    }
                }
                chars[j] = original;                // 还原
            }
        }
        steps++;
    }
    return 0;                          // 不可达
}
```

### 2.2 复杂度分析

```
每个单词：长度 L，生成 26L 个候选邻居
检查是否在字典：O(1)（HashSet）
总复杂度：O(n × 26 × L) = O(n×L)
空间：O(n)（队列+字典）

⚠️ 面试必答：
  为什么用 HashSet 而不遍历 wordList？
  → 26×L 次查询 × 每次 O(1) vs O(n) 遍历
```

### 2.3 优化：双向 BFS

```
单向 BFS：从 beginWord 扩散，层数可能很多
双向 BFS：同时从 beginWord 和 endWord 扩散
  → 两个方向的扩散半径减半 → 状态数平方级减少

实现要点：
  两个 Set（visited）交替扩散
  每次扩散较小的一侧
  两侧相遇 → 找到最短路径
（完整代码见 04 模块）
```

---

## 3. 752. 打开转盘锁

### 3.1 完整实现

```java
/**
 * LeetCode 752. 打开转盘锁
 * 4 位密码锁，每次拨动一个数字 +1 或 -1
 * deadends 是死亡密码（不能到达），求到 target 的最少拨动次数
 *
 * 状态：4 位密码字符串（0000-9999）
 * 转移：任一位置 +1 或 -1（8 种）
 * 目标：target；跳过 deadends
 */
public int openLock(String[] deadends, String target) {
    Set<String> dead = new HashSet<>(Arrays.asList(deadends));
    if (dead.contains("0000")) return -1;        // 起点就是死亡

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

            // 生成 8 种下一状态
            for (String next : getNeighbors(cur)) {
                if (dead.contains(next)) continue;    // 死亡密码跳过
                if (visited.contains(next)) continue;
                visited.add(next);
                queue.offer(next);
            }
        }
        steps++;
    }
    return -1;                       // 不可达
}

// 生成 8 种邻居：每个位置 +1 和 -1
private List<String> getNeighbors(String code) {
    List<String> neighbors = new ArrayList<>();
    char[] chars = code.toCharArray();
    for (int i = 0; i < 4; i++) {
        char original = chars[i];

        chars[i] = (char) ((original - '0' + 1) % 10 + '0');   // +1
        neighbors.add(new String(chars));

        chars[i] = (char) ((original - '0' + 9) % 10 + '0');   // -1（+9 等价 -1 mod 10）
        neighbors.add(new String(chars));

        chars[i] = original;        // 还原
    }
    return neighbors;
}
```

### 3.2 状态数分析（面试必讲）

```
状态空间：10⁴ = 10000 种密码
  → BFS 可行（每个状态入队一次 → O(10⁴)）
  → 对比：如果密码 10 位 → 10¹⁰ 状态 → BFS 爆炸

⚠️ 状态 BFS 面试第一句话：
"状态数有限且可枚举（10⁴），BFS 复杂度 O(状态数×转移数)
 = O(10⁴×8) = O(80000)，可行。"
```

---

## 4. 909. 蛇梯棋

### 4.1 完整实现（棋盘状态 BFS）

```java
/**
 * LeetCode 909. 蛇梯棋
 * n×n 棋盘，从 1 到 n²，掷骰子 1-6 前进
 * 遇到梯子/蛇传送到目标格，求最少掷骰次数
 *
 * 状态：格子编号（1 到 n²）
 * 转移：+1 到 +6（骰子），可能触发传送
 * 目标：n²
 */
public int snakesAndLadders(int[][] board) {
    int n = board.length;
    int target = n * n;
    // 预处理：编号 → 坐标映射（蛇形棋盘）
    // board[n-1-i][奇数列从左到右/偶数列从右到左]

    Queue<Integer> queue = new LinkedList<>();
    boolean[] visited = new boolean[target + 1];
    queue.offer(1);
    visited[1] = true;

    int steps = 0;
    while (!queue.isEmpty()) {
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            int cur = queue.poll();
            if (cur == target) return steps;

            // 掷骰子：1-6
            for (int dice = 1; dice <= 6; dice++) {
                int next = cur + dice;
                if (next > target) break;    // 超出棋盘

                // 有蛇/梯子 → 传送
                int[] pos = getPosition(board, next);
                if (board[pos[0]][pos[1]] != -1) {
                    next = board[pos[0]][pos[1]];   // 传送到目标格
                }

                if (!visited[next]) {
                    visited[next] = true;
                    queue.offer(next);
                }
            }
        }
        steps++;
    }
    return -1;
}
```

### 4.2 状态 BFS 题型对比

| 题号 | 状态 | 转移 | 状态数 |
|:---:|------|------|:---:|
| 127 单词接龙 | 单词 | 改一字母 | n |
| 752 转盘锁 | 4 位密码 | 8 种拨动 | 10⁴ |
| 909 蛇梯棋 | 格子编号 | 骰子 1-6 | n² |
| 1293 网格障碍 | (x,y,剩余k) | 四方向 | m×n×k |
| 863 二叉树距离 | 节点 | 父/左右 | n |

---

## 5. 状态 BFS 建模总结

### 5.1 建模四步

```
① 定义状态：当前局面是什么？
   （字符串/编号/坐标+属性）
② 枚举转移：一次操作能得到哪些新状态？
   （改一字母/拨一数字/走一步）
③ 确定目标：什么状态算到达终点？
④ 估计状态数：BFS 可行吗？
   （状态数 × 转移数 可控 → 可行）

⭐ 面试先说复杂度可行性：
"状态数 N，每状态最多 k 种转移，
 BFS 总复杂度 O(N×k)，可行/不可行因为..."
```

### 5.2 面试话术

```
"这题建模成状态 BFS：
 状态是【当前密码】，转移是【任一位置 ±1 共 8 种】，
 目标是【target】，deadends 是禁入状态。
 状态空间 10⁴，每状态 8 转移 → O(8×10⁴) 可行。
 BFS 逐层扩散，第一次到达 target 就是最少拨动次数。"
```

### 5.3 常见坑

| 坑 | 说明 |
|------|------|
| 状态数爆炸 | 10 位密码 BFS 不可行 → 先估计状态空间 |
| 入队未标记 | 状态可能被重复入队（同 01 模块铁律） |
| 转移还原 | 修改 char[] 后忘记还原 |
| 目标不可达 | BFS 结束返回 -1/0（根据题目语义） |
| 边界剪枝 | 越界/死亡状态/已访问跳过 |

> 🎯 **核心要点**：状态 BFS 把"最少操作"问题抽象为**状态图上的最短路径**——状态是节点、操作是边。127/752/909 三道题展示了字符串/密码/编号三种状态编码。**面试第一句话讲状态数和复杂度可行性**（状态 BFS 的成败关键），再套 BFS 模板（入队即标记 + size 分层）。

---

**下一模块**：[04-BFS进阶-双向搜索与拓扑排序](04-BFS进阶-双向搜索与拓扑排序.md) | **返回总览**：[00-BFS知识体系总览](00-BFS知识体系总览.md)
