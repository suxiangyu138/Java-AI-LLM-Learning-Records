# 02-双向BFS高频题解
> 一句话：127 单词接龙、752 开锁、773 滑动谜题——双向 BFS 三题全解，状态建模 + 可逆性判断实战

## 📚 目录
1. [127 单词接龙（双向 BFS 经典）](#1-127-单词接龙双向-bfs-经典)
2. [752 打开转盘锁（双向模板）](#2-752-打开转盘锁双向模板)
3. [773 滑动谜题（状态编码）](#3-773-滑动谜题状态编码)
4. [双向 BFS vs 单向对比](#4-双向-bfs-vs-单向对比)
5. [家族总览](#5-家族总览)
6. [易错点与面试要点](#6-易错点与面试要点)

---

## 1. 127 单词接龙（双向 BFS 经典）

### 1.1 题目与思路

**题**：从 beginWord 每次变换一个字母到 endWord（中间词必须在 wordList），求最短变换次数。

**双向 BFS + 单词邻居生成**：

```java
/**
 * LeetCode 127. 单词接龙
 * 双向 BFS: 从两端各扩一层，相遇即最短
 */
public int ladderLength(String beginWord, String endWord, List<String> wordList) {
    Set<String> words = new HashSet<>(wordList);
    if (!words.contains(endWord)) return 0;

    Set<String> forward = new HashSet<>();
    Set<String> backward = new HashSet<>();
    forward.add(beginWord);
    backward.add(endWord);
    Set<String> visited = new HashSet<>();

    int steps = 1;                               // ① 起点算 1 步
    while (!forward.isEmpty() && !backward.isEmpty()) {
        if (forward.size() > backward.size()) {  // ② 扩展较小者
            Set<String> tmp = forward; forward = backward; backward = tmp;
        }

        Set<String> next = new HashSet<>();
        for (String word : forward) {
            char[] chars = word.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char original = chars[i];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == original) continue;
                    chars[i] = c;
                    String neighbor = new String(chars);
                    if (backward.contains(neighbor)) return steps + 1;  // ③ 相遇
                    if (words.contains(neighbor) && !visited.contains(neighbor)) {
                        visited.add(neighbor);
                        next.add(neighbor);
                    }
                }
                chars[i] = original;             // ④ 还原（重要！）
            }
        }
        forward = next;
        steps++;
    }
    return 0;
}
// 推演 begin="hit", end="cog", list=[hot,dot,dog,lot,log,cog] → 5 ✓
//   hit→hot→dot→dog→cog（长度 5）
// ⚠️ 邻居生成: 逐位替换 26 字母——O(26·L) 而非遍历 wordList
//   还原 chars[i] 防污染（每位的替换要独立）
```

> 🎯 **127 的必考点**："**'逐位换 26 字母'生成邻居（O(26L)）+ '扩展较小者' + '相遇即返回'**——三个细节是 127 的完整答案。还原 chars[i] 是最高频 bug（漏还原 → 邻居全错）。"

## 2. 752 打开转盘锁（双向模板）

### 2.1 完整实现（01 模板回顾）

```java
/**
 * LeetCode 752. 打开转盘锁
 * 双向 BFS + 死锁过滤
 */
public int openLock(String[] deadends, String target) {
    Set<String> dead = new HashSet<>(Arrays.asList(deadends));
    if (dead.contains("0000")) return -1;
    if (target.equals("0000")) return 0;

    Set<String> forward = new HashSet<>(), backward = new HashSet<>();
    forward.add("0000");
    backward.add(target);
    Set<String> visited = new HashSet<>();

    int steps = 0;
    while (!forward.isEmpty() && !backward.isEmpty()) {
        if (forward.size() > backward.size()) {
            Set<String> tmp = forward; forward = backward; backward = tmp;
        }
        Set<String> next = new HashSet<>();
        for (String cur : forward) {
            if (backward.contains(cur)) return steps;   // ① 相遇
            if (visited.contains(cur)) continue;
            visited.add(cur);
            for (String nb : neighbors(cur)) {          // ② 八个邻居（±1 × 4 位）
                if (!dead.contains(nb) && !visited.contains(nb)) next.add(nb);
            }
        }
        forward = next;
        steps++;
    }
    return -1;
}

private List<String> neighbors(String s) {
    List<String> result = new ArrayList<>();
    char[] chars = s.toCharArray();
    for (int i = 0; i < 4; i++) {
        char original = chars[i];
        chars[i] = (char) ((original - '0' + 1) % 10 + '0');  // ③ 加一
        result.add(new String(chars));
        chars[i] = (char) ((original - '0' + 9) % 10 + '0');  // ④ 减一（+9 mod 10）
        result.add(new String(chars));
        chars[i] = original;
    }
    return result;
}
// 推演 deadends=["0201","0101","0102","1212","2002"], target="0202" → 6 ✓
// ⚠️ 减一技巧: (d − 1 + 10) % 10——环形的 ±1 运算
```

> 🎯 **752 的模板价值**："**752 = 双向 BFS 的干净模板——'四轮 ±1 邻居 + 死锁过滤 + 相遇判定'**。会 752 就能套 127/773（邻居生成换实现）。"

## 3. 773 滑动谜题（状态编码）

### 3.1 题目与思路

**题**：2×3 滑动拼图归位的最少步数。

**状态编码**：棋盘序列化成一维字符串（可逆移动 → 双向 BFS 适用）：

```java
/**
 * LeetCode 773. 滑动谜题
 * 状态: 棋盘序列化为字符串（如 "123450"）
 * 移动: 0 与相邻位置交换（预计算邻接表）
 */
public int slidingPuzzle(int[][] board) {
    StringBuilder sb = new StringBuilder();
    for (int[] row : board)
        for (int x : row) sb.append(x);
    String start = sb.toString();
    String target = "123450";

    if (start.equals(target)) return 0;
    // ① 0 的邻居位置（2×3 棋盘）
    int[][] neighbors = {
        {1, 3}, {0, 2, 4}, {1, 5}, {0, 4}, {1, 3, 5}, {2, 4}
    };

    Set<String> forward = new HashSet<>(), backward = new HashSet<>();
    forward.add(start);
    backward.add(target);
    Set<String> visited = new HashSet<>();

    int steps = 0;
    while (!forward.isEmpty() && !backward.isEmpty()) {
        if (forward.size() > backward.size()) {
            Set<String> tmp = forward; forward = backward; backward = tmp;
        }
        Set<String> next = new HashSet<>();
        for (String cur : forward) {
            if (backward.contains(cur)) return steps;
            if (visited.contains(cur)) continue;
            visited.add(cur);
            int zero = cur.indexOf('0');
            for (int nb : neighbors[zero]) {     // ② 0 与邻居交换
                char[] chars = cur.toCharArray();
                chars[zero] = chars[nb];
                chars[nb] = '0';
                String nextState = new String(chars);
                if (!visited.contains(nextState)) next.add(nextState);
            }
        }
        forward = next;
        steps++;
    }
    return -1;
}
// 推演 [[1,2,3],[4,0,5]] → 1 ✓（0 与 5 交换）
// ⚠️ 状态编码: 棋盘 → 字符串（哈希友好）——
//   滑动移动可逆 → 双向 BFS 适用 ✓
```

> 💡 **状态编码的通用性**："**'棋盘 → 字符串/整数编码'让状态可哈希、可入集合——双向 BFS 的前提**。773 的邻接表预计算是 2×3 棋盘的特化，大棋盘用坐标映射。"

## 4. 双向 BFS vs 单向对比

| 维度 | 单向 BFS | 双向 BFS |
|------|:---:|:---:|
| 时间 | O(b^d) | O(b^(d/2)) |
| 空间 | O(b^d) | O(b^(d/2)) |
| 队列 | 1 个 | 2 个（集合） |
| 前提 | 无 | 状态可逆 |
| 实现 | 简单 | 扩展较小者 + 相遇判定 |
| 适用 | 一切最短路 | 可逆 + 两端明确 |

```text
⚠️ 面试表达:
  "单向简单通用；双向减半但要求可逆。
   状态可逆 + 深度大 → 双向（127/752）；
   不可逆或实现优先 → 单向。"
```

## 5. 家族总览

| 题 | 状态 | 邻居生成 | 可逆性 |
|:---:|:---:|:---:|:---:|
| 127 单词接龙 | 单词字符串 | 逐位换 26 字母 | ✔ |
| 752 开锁 | 4 位数字串 | 四轮 ±1 | ✔ |
| 773 滑动谜题 | 棋盘序列化 | 0 与邻居交换 | ✔ |
| 433 最小基因变化 | 基因串 | 单点换字符 | ✔ |

```text
⚠️ 家族规律:
  "状态编码（字符串/整数）→ 邻居生成（按规则）→ 双向模板套用——
   三题共享完全相同的双向 BFS 骨架"
```

## 6. 易错点与面试要点

### 6.1 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 127 漏还原 chars[i] | 每轮还原 | 邻居全错 |
| 2 | 127 遍历 wordList 找邻居 | 逐位换 26 字母 | O(nL) 超时 |
| 3 | 752 减一写错 | `(d + 9) % 10` | 负数/错误 |
| 4 | 773 忘预计算邻接 | neighbors 表 | 重复计算 |
| 5 | 相遇判定漏 | 扩展当前层检查 | 步数错 |
| 6 | 起点=终点忘判 | 先判相等返回 0 | 多算步数 |
| 7 | 死锁忘过滤 | dead.contains 检查 | 走入死锁 |

### 6.2 面试要点

```text
① 复杂度开场: "O(b^(d/2))——深度减半"
② 可逆性论证: "滑动可逆/变换可逆 → 反向可搜"
③ 状态编码: "棋盘/数字 → 字符串（哈希友好）"
④ 扩展较小者: "让两队均衡前进，总探索最小"

⚠️ 高频追问: "为什么相遇即最短？"
  → "双向按层同步推进——前向 k 层 + 后向 (d−k) 层，
    相遇时路径长度固定 d；更短的路径会让两前沿更早相遇"
```

> 🎯 **核心要点**：双向 BFS 题解通关——**127（逐位邻居 + 还原）+ 752（四轮 ±1）+ 773（状态编码 + 邻接表）**。三题共享同一骨架——"状态编码 + 邻居生成 + 双向模板"，识别可逆性就能套用。

---

**上一篇**：[01-双向BFS核心原理](01-双向BFS核心原理.md)
**返回总览**：[00-高级搜索知识体系总览](../00-高级搜索知识体系总览.md)
