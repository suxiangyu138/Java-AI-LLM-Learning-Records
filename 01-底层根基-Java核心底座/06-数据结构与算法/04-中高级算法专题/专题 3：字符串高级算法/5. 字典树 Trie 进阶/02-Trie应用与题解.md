# 02-Trie应用与题解
> 一句话：211 单词搜索 II（Trie + DFS）、421 最大异或（位 Trie）、自动补全——Trie 从"字典"到"算法引擎"的应用延伸

## 📚 目录
1. [211 添加与搜索单词（通配符）](#1-211-添加与搜索单词通配符)
2. [212 单词搜索 II（Trie + 网格 DFS）](#2-212-单词搜索-iitrie--网格-dfs)
3. [421 数组中两个数的最大异或（位 Trie）](#3-421-数组中两个数的最大异或位-trie)
4. [自动补全与词频统计](#4-自动补全与词频统计)
5. [Trie 家族总览](#5-trie-家族总览)
6. [易错点与面试要点](#6-易错点与面试要点)

---

## 1. 211 添加与搜索单词（通配符）

### 1.1 题目与思路

**题**：字典支持通配符 '.' 的搜索（. 匹配任意字符）。

**Trie + DFS（. 时遍历所有孩子）**：

```java
/**
 * LeetCode 211. 添加与搜索单词
 * 搜索含 '.'：遇到 '.' 递归所有孩子
 */
class WordDictionary {
    private static class Node {
        Node[] children = new Node[26];
        boolean isEnd;
    }

    private final Node root = new Node();

    public void addWord(String word) {
        Node cur = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (cur.children[idx] == null) cur.children[idx] = new Node();
            cur = cur.children[idx];
        }
        cur.isEnd = true;
    }

    public boolean search(String word) {
        return dfs(root, word, 0);               // ① DFS 匹配
    }

    private boolean dfs(Node node, String word, int index) {
        if (node == null) return false;
        if (index == word.length()) return node.isEnd;   // ② 完整词判定

        char c = word.charAt(index);
        if (c != '.') {                          // ③ 普通字符
            return dfs(node.children[c - 'a'], word, index + 1);
        }
        for (Node child : node.children) {       // ④ ⚠️ '.' → 遍历所有孩子
            if (child != null && dfs(child, word, index + 1)) return true;
        }
        return false;
    }
}
// 推演 addWord("bad") → search("b..") → 找到 "bad" ✓
//   search("b.a") → true；search("b") → false（isEnd 判定）✓
// ⚠️ '.' 的本质: 通配 → DFS 分支（把 Trie 查询变成搜索）
```

> 🎯 **211 的通配处理**："**'.' 把 Trie 查询从'沿路径走'升级为'DFS 分支搜索'**——普通字符走孩子、通配符遍历全部孩子。'isEnd 判定'在 DFS 结尾仍是关键。"

## 2. 212 单词搜索 II（Trie + 网格 DFS）

### 2.1 题目与思路

**题**：网格中找字典里的所有单词（相邻四方向）。

**Trie 剪枝 + 网格 DFS**——比"每个词单独 DFS"快一个数量级：

```java
/**
 * LeetCode 212. 单词搜索 II
 * Trie 剪枝: 沿网格 DFS 时同步走 Trie——
 *   字典中不存在的前缀直接剪掉
 */
public List<String> findWords(char[][] board, String[] words) {
    // ① 字典建 Trie（加速前缀判定）
    Node root = buildTrie(words);

    List<String> result = new ArrayList<>();
    int m = board.length, n = board[0].length;
    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            dfs(board, i, j, root, new StringBuilder(), result);
    return result;
}

private void dfs(char[][] board, int i, int j, Node node,
                 StringBuilder path, List<String> result) {
    if (i < 0 || i >= board.length || j < 0 || j >= board[0].length) return;
    char c = board[i][j];
    if (c == '#') return;                        // ② 已访问
    int idx = c - 'a';
    if (node.children[idx] == null) return;      // ③ ⚠️ Trie 剪枝: 前缀不存在

    Node next = node.children[idx];
    path.append(c);
    if (next.isEnd) {                            // ④ 命中字典词
        result.add(path.toString());
        next.isEnd = false;                      // ⑤ 防重复收集
    }

    board[i][j] = '#';                           // ⑥ 标记访问
    dfs(board, i + 1, j, next, path, result);    // 四方向
    dfs(board, i - 1, j, next, path, result);
    dfs(board, i, j + 1, next, path, result);
    dfs(board, i, j - 1, next, path, result);
    board[i][j] = c;                             // ⑦ 撤销标记

    path.deleteCharAt(path.length() - 1);
}
// 推演 board=[["o","a","a","n"],...], words=["oath","pea","eat","rain"] → ["oath","eat"] ✓
// ⚠️ 为什么 Trie 快: 朴素 = 每词独立 DFS（O(词数 × 网格)）；
//   Trie = 一次 DFS 同步剪枝（前缀不存在立即返回）——共享前缀剪枝
```

> 🎯 **212 的剪枝价值**："**'网格 DFS 同步走 Trie'——字典不存在的路径立即剪掉，比逐个词搜索快一个数量级**。212 是 Trie + DFS + 剪枝的组合题，也是 Trie 面试的高频 hard。"

## 3. 421 数组中两个数的最大异或（位 Trie）

### 3.1 题目与思路

**题**：数组中两个数异或的最大值（O(n) 位级）。

**位 Trie（二进制前缀树）**：逐位贪心——尽量走向与当前位相反的分支：

```java
/**
 * LeetCode 421. 数组中两个数的最大异或
 * 位 Trie: 每个数的 31..0 位建路径
 * 贪心: 每位尽量选相反位（异或 = 1 最大）
 */
public int findMaximumXOR(int[] nums) {
    // ① 建位 Trie（31 位二进制路径）
    Node root = new Node();
    for (int x : nums) {
        Node cur = root;
        for (int bit = 31; bit >= 0; bit--) {
            int b = (x >> bit) & 1;
            if (cur.children[b] == null) cur.children[b] = new Node();
            cur = cur.children[b];
        }
    }

    // ② 每个数找"最大异或搭档": 逐位走相反分支
    int best = 0;
    for (int x : nums) {
        Node cur = root;
        int xor = 0;
        for (int bit = 31; bit >= 0; bit--) {
            int b = (x >> bit) & 1;
            int want = 1 - b;                    // ③ 想要相反位
            if (cur.children[want] != null) {    // ④ 有 → 走（异或位 = 1）
                xor |= (1 << bit);
                cur = cur.children[want];
            } else {
                cur = cur.children[b];           // ⑤ 没有 → 走同向
            }
        }
        best = Math.max(best, xor);
    }
    return best;
}
// 推演 [3,10,5,25,2,8] → 28 ✓（5 ^ 25 = 28）
// ⚠️ 位 Trie 的本质: 二进制前缀树——"逐位贪心选相反"
//   复杂度 O(31n) ≈ O(n)（比 O(n²) 暴力快 n 倍）
```

> 💡 **位 Trie 的通用性**："**421 = 位 Trie + 逐位贪心——'尽量走相反位'让异或最大**。位 Trie 还用于最大/最小异或、子数组异或问题，是 Trie 的'二进制形态'。"

## 4. 自动补全与词频统计

### 4.1 自动补全（前缀 + 词频）

```java
/**
 * 自动补全: Trie 前缀定位 + 子树遍历收集
 * 工程: 搜索引擎联想、IDE 补全
 */
List<String> autocomplete(String prefix, int topK) {
    Node node = findPrefix(prefix);              // ① 定位前缀节点
    if (node == null) return Collections.emptyList();

    // ② 子树遍历收集所有词（带词频）
    List<Entry> candidates = collectWords(node, new StringBuilder(prefix));
    // ③ 按词频降序取 topK
    candidates.sort((a, b) -> b.freq - a.freq);
    return candidates.subList(0, Math.min(topK, candidates.size()))
                     .stream().map(e -> e.word).toList();
}
// ⚠️ 工程要点: 词频存储（节点 count）+ topK 选择——
//   自动补全 = Trie 前缀检索 + 词频排序
```

## 5. Trie 家族总览

| 题 | Trie 角色 | 额外技术 |
|:---:|:---:|:---:|
| 208 实现 Trie | 基础结构 | isEnd 语义 |
| 211 通配符搜索 | 查询 + DFS | '.' 分支 |
| 212 单词搜索 II | 剪枝引擎 | 网格 DFS |
| 421 最大异或 | 位 Trie | 逐位贪心 |
| 自动补全 | 前缀 + 词频 | topK 排序 |
| 词典系统 | 集合组织 | 变体组合 |

```text
⚠️ 家族规律:
  "Trie 是'组织层'——查询（208/211）、剪枝（212）、
   位运算（421）、排序（补全）都是它的应用层"
```

## 6. 易错点与面试要点

### 6.1 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 211 的 '.' 忘递归 | 遍历孩子 DFS | 通配失效 |
| 2 | 212 忘 Trie 剪枝 | 前缀不存在返回 | 退化 O(n²) |
| 3 | 212 忘撤销标记 | board 复原 | 状态污染 |
| 4 | 212 重复收集 | isEnd 置 false | 结果重复 |
| 5 | 421 位序错 | 从高位 31 开始 | 贪心失效 |
| 6 | 421 忘相反位优先 | want = 1−b | 异或非最大 |
| 7 | 补全 topK 边界 | 空前缀/超长 | 越界/空结果 |

### 6.2 面试要点

```text
① 开场: "Trie = 前缀共享的字符树——查询 O(L)"
② 高级应用: "212 Trie 剪枝、421 位 Trie 贪心"
③ 复杂度: "插入/查询 O(L)，与数据量无关"
④ 与哈希对比: "前缀检索是 Trie vs 哈希的分水岭"

⚠️ 高频追问: "Trie 和哈希表怎么选？"
  → "哈希 O(1) 平均查询但不支持前缀；
    Trie O(L) 查询 + 天然前缀检索——
    需要前缀/自动补全/共享前缀剪枝 → Trie"
```

> 🎯 **核心要点**：Trie 应用通关——**208（基础）+ 211（通配 DFS）+ 212（剪枝引擎）+ 421（位 Trie）**。四应用覆盖"查询/通配/剪枝/位运算"——"Trie 是组织层，应用层随意组合"，就是 Trie 家族的全部形态。

---

**上一篇**：[01-Trie核心原理与实现](01-Trie核心原理与实现.md)
**返回总览**：[00-字符串高级算法知识体系总览](../00-字符串高级算法知识体系总览.md)
