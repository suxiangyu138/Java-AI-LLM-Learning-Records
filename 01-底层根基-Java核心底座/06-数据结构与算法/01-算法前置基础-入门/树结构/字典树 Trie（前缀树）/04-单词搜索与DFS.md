# 04 - 单词搜索与 DFS

> Trie 的第二大应用：DFS 剪枝——212 单词搜索 II 用 Trie 把「每个单词独立 DFS」的 O(n·4^L) 优化为「所有单词一次 DFS」——Trie 做剪枝器的经典范式。

## 📚 目录

1. [79 单词搜索（基础版）](#1)
2. [212 单词搜索 II（Trie 剪枝）](#2)
3. [Trie 剪枝的原理](#3)
4. [DFS 剪枝应用清单](#4)

## 1. 79 单词搜索（基础版）

**问题**：网格中是否存在目标单词（四方向相邻）。

```java
// LeetCode 79 单词搜索：单单词 DFS + 回溯
public boolean exist(char[][] board, String word) {
    int m = board.length, n = board[0].length;
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (dfs(board, word, 0, i, j)) return true;
        }
    }
    return false;
}

boolean dfs(char[][] board, String word, int idx, int x, int y) {
    if (idx == word.length()) return true;                 // 全匹配
    if (x < 0 || x >= board.length || y < 0 || y >= board[0].length
        || board[x][y] != word.charAt(idx)) return false;
    char tmp = board[x][y];
    board[x][y] = '#';                                     // 标记访问
    boolean found = dfs(board, word, idx + 1, x + 1, y)
                 || dfs(board, word, idx + 1, x - 1, y)
                 || dfs(board, word, idx + 1, x, y + 1)
                 || dfs(board, word, idx + 1, x, y - 1);
    board[x][y] = tmp;                                     // 回溯还原
    return found;
}
```

**复杂度**：O(m·n·4^L)（每个起点 4 方向 × 单词长度）——**单个单词可接受，多个单词不可接受**。

## 2. 212 单词搜索 II（Trie 剪枝）

**问题**：网格中找出所有存在于词典中的单词（多单词）——朴素做法对每个单词跑一次 79 → O(n·m·n·4^L) 爆炸。

**Trie 解法**：**把词典建成 Trie，一次 DFS 同时匹配所有单词**——Trie 是剪枝器。

```java
// LeetCode 212 单词搜索 II
class Solution {
    int m, n;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    Set<String> result = new HashSet<>();           // 去重（路径不同同单词）

    public List<String> findWords(char[][] board, String[] words) {
        m = board.length; n = board[0].length;
        Trie root = new Trie();
        for (String w : words) root.insert(w);      // ① 词典入 Trie

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                dfs(board, root, i, j, new StringBuilder());
            }
        }
        return new ArrayList<>(result);
    }

    void dfs(char[][] board, Trie node, int x, int y, StringBuilder sb) {
        if (x < 0 || x >= m || y < 0 || y >= n) return;
        char c = board[x][y];
        int idx = c - 'a';
        if (c == '#' || node.children[idx] == null) return;  // ② Trie 剪枝！
        node = node.children[idx];
        sb.append(c);
        if (node.isEnd) result.add(sb.toString());  // ③ 命中单词

        board[x][y] = '#';
        for (int[] d : dirs) dfs(board, node, x + d[0], y + d[1], sb);
        board[x][y] = c;                            // 回溯
        sb.deleteCharAt(sb.length() - 1);
    }
}
```

**三处关键**：
1. 词典入 Trie（一次建树）；
2. **Trie 剪枝**：`node.children[idx] == null` 直接返回——**当前路径不是任何单词的前缀就停止**（vs 79 题要走到头才知失败）；
3. isEnd 命中收集 + HashSet 去重。

## 3. Trie 剪枝的原理

```text
朴素（79 多跑）:            Trie 剪枝（212）:
单词 = [abc, abd, abe]      词典树:
对每个单词独立 DFS            root
  DFS(abc): 路径 a→b→c        ├─ a ─ b ─┬─ c
  DFS(abd): 路径 a→b→d        │          ├─ d
  DFS(abe): 路径 a→b→e        │          └─ e
  前缀 a→b 被重复探索 3 次    一次 DFS 到 a→b 后
                             三个分支同时继续 ✓

剪枝收益: 共享前缀只探索一次
  路径「不是任何单词前缀」→ 整棵子树剪掉
```

| 对比 | 79 单词搜索 | 212 单词搜索 II |
|------|:---:|:---:|
| 单词数 | 1 | n |
| 剪枝依据 | 无（逐字符比对） | **Trie 前缀** |
| 复杂度 | O(mn·4^L) | O(mn·4^L)（共享前缀大幅常数优化） |
| 关键 | 单路径回溯 | Trie + 回溯 + 剪枝 |

> 💡 **Trie 剪枝范式**：**「字典 → Trie → DFS 路径中查前缀」**——凡是「网格/路径中找字典单词」的问题（212、恢复 IP、词梯）都可以套用；Trie 在这里不是查询工具而是**剪枝加速器**。

## 4. DFS 剪枝应用清单

| 题号 | 题目 | Trie 角色 |
|------|------|-----------|
| 212 | 单词搜索 II | 词典 Trie 剪枝（范式题） |
| 79 | 单词搜索 | 无 Trie（单单词基础版） |
| 425 | 单词方块 | Trie + 前缀回溯 |
| 472 | 连接词 | Trie + 记忆化 |
| 140 | 单词拆分 II | Trie 前缀 + 回溯 |
| 1233 | 删除子文件夹 | Trie 路径标记 |

```java
// 472 连接词（Trie + 记忆化思想预览）
// 每个单词拆成两段查 Trie：word = prefix + suffix
// 用 Trie 查前缀 + 递归查后缀是否可由词典词拼接
```

### 3.1 剪枝的收益量化

```text
词典 n 个单词、平均长度 L、网格 m×n:

朴素（每个单词独立 79）:  O(n × m·n × 4^L)
Trie 剪枝（一次 DFS）:    O(m·n × 4^L × 前缀共享因子)

例: 词典 [abc, abd, abe, abf, abg]（共享前缀 ab）
  朴素: 5 次完整 DFS，ab 路径被探索 5 次
  Trie: 1 次 DFS，ab 之后分 5 叉继续 ✓ 剪枝 80%
```

### 3.2 回溯 vs 剪枝对比

| 概念 | 回溯（79） | Trie 剪枝（212） |
|------|-----------|----------------|
| 目的 | 恢复现场（找所有路径） | 提前终止（非前缀路径） |
| 操作 | 标记/还原字符 | 查 children 是否 null |
| 配合 | DFS 标配 | Trie 专属优化 |
| 面试问法 | 「为什么还原？」 | 「为什么快？」 |

> 🎯 **核心要点**：单词搜索验收——79 单单词回溯默写；212 三件套（词典入树、前缀剪枝、isEnd 收集）默写；理解「共享前缀只探索一次」的剪枝本质；识别「网格/字符串中找词典词」信号 → Trie + DFS；剪枝范式是 Trie 最优雅的应用，也是面试的进阶分水岭。

---

**上一模块**：[03-前缀匹配应用](03-前缀匹配应用.md) ｜ **下一模块**：[05-位运算与 01-Trie](05-位运算与01-Trie.md) ｜ **返回总览**：[00-字典树 Trie 知识体系总览](00-字典树 Trie 知识体系总览.md)

**【参考来源】**
- LeetCode 79 单词搜索 / 212 单词搜索 II
- 算法导论（CLRS）第 26 章
