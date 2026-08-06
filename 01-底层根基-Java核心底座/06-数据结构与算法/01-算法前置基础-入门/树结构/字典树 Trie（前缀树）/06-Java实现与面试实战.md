# 06 - Java 实现与面试实战

> Trie 面试收尾：四大模板（标准/计数/01-Trie/删除）、10 道高频题、面试追问话术、易错点——从 208 到 421 的完整武器库。

## 📚 目录

1. [四大必背模板](#1)
2. [高频题解速览](#2)
3. [面试追问与答题话术](#3)
4. [易错点清单](#4)

## 1. 四大必背模板

### 模板一：标准 Trie（208 题）

```java
class Trie {
    private Trie[] children = new Trie[26];
    private boolean isEnd;

    public void insert(String word) {
        Trie node = this;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) node.children[idx] = new Trie();
            node = node.children[idx];
        }
        node.isEnd = true;
    }

    public boolean search(String word) {
        Trie node = searchPrefix(word);
        return node != null && node.isEnd;
    }

    public boolean startsWith(String prefix) {
        return searchPrefix(prefix) != null;
    }

    private Trie searchPrefix(String prefix) {
        Trie node = this;
        for (char c : prefix.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) return null;
            node = node.children[idx];
        }
        return node;
    }
}
```

### 模板二：计数 Trie（前缀统计）

```java
class TrieCount {
    TrieCount[] children = new TrieCount[26];
    int count;                          // 经过次数（前缀统计）

    void insert(String word) {
        TrieCount node = this;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) node.children[idx] = new TrieCount();
            node = node.children[idx];
            node.count++;               // 每个节点记录经过次数
        }
    }

    int countPrefix(String prefix) {    // 前缀出现次数
        TrieCount node = this;
        for (char c : prefix.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) return 0;
            node = node.children[idx];
        }
        return node.count;
    }
}
```

### 模板三：01-Trie（421 题）

```java
class BinaryTrie {
    BinaryTrie[] children = new BinaryTrie[2];

    void insert(int num) {
        BinaryTrie node = this;
        for (int i = 30; i >= 0; i--) {
            int bit = (num >> i) & 1;
            if (node.children[bit] == null) node.children[bit] = new BinaryTrie();
            node = node.children[bit];
        }
    }

    int maxXor(int num) {               // 与 num 异或最大
        BinaryTrie node = this;
        int xor = 0;
        for (int i = 30; i >= 0; i--) {
            int bit = (num >> i) & 1;
            int opposite = bit ^ 1;
            if (node.children[opposite] != null) {
                xor |= (1 << i);
                node = node.children[opposite];
            } else {
                node = node.children[bit];
            }
        }
        return xor;
    }
}
```

### 模板四：删除（回溯剪枝）

```java
boolean delete(Trie node, String word, int depth) {
    if (depth == word.length()) {
        if (!node.isEnd) return false;
        node.isEnd = false;
        return isEmpty(node);
    }
    int idx = word.charAt(depth) - 'a';
    if (node.children[idx] == null) return false;
    boolean canDelete = delete(node.children[idx], word, depth + 1);
    if (canDelete) node.children[idx] = null;
    return isEmpty(node) && !node.isEnd;
}
```

> 🎯 四大模板约 80 行——208 + 421 是必背，计数/删除是扩展。

## 2. 高频题解速览

| 题号 | 题目 | 考点 | 一句话思路 |
|------|------|------|-----------|
| 208 | 实现 Trie | 基础模板 | 数组 + isEnd |
| 648 | 单词替换 | 最短前缀 | 第一个 isEnd |
| 1268 | 搜索推荐系统 | DFS 收集 | 下钻 + 子树 DFS 前 3 |
| 212 | 单词搜索 II | Trie 剪枝 | 词典入树 + 前缀剪枝 |
| 421 | 最大异或对 | 01-Trie | 贪心相反位 |
| 1707 | 与元素最大异或 | limit 剪枝 | 子树 min 约束 |
| 1803 | 异或值范围数对 | 计数 Trie | 前缀计数差 |
| 472 | 连接词 | Trie + 记忆化 | 前缀拆分 |
| 336 | 回文对 | Trie + 回文 | 后缀回文检测 |
| 1233 | 删除子文件夹 | 路径 Trie | 前缀标记 |

```java
// 336 回文对核心思想（Trie 后缀匹配）
// 每个单词反转插入 Trie，匹配时检查「剩余部分是否回文」
// 复杂度 O(n·L²) vs 朴素 O(n²·L)
```

## 3. 面试追问与答题话术

| 追问 | 答题要点 |
|------|---------|
| Trie 和 HashSet 怎么选？ | 需要**前缀操作**用 Trie；纯存在性查询 HashSet 更省 |
| 空间复杂度？ | O(字符总数 × 字母表)；哈希实现按需省空位 |
| search 和 startsWith 区别？ | search 需 isEnd；startsWith 下钻成功即可 |
| Trie 为什么适合单词搜索 II？ | 共享前缀只探索一次 + 非前缀路径剪枝 |
| 01-Trie 为什么能求最大异或？ | 异或由高位决定 → 逐位贪心相反位 |
| 01-Trie 位数怎么定？ | 非负 int 用 0..30；含负需处理符号位 |
| Trie 能排序吗？ | 能——DFS 从左到右即字典序 |
| 中文分词用什么？ | 双数组 Trie（空间优化） |
| Trie vs AC 自动机？ | 单模式 Trie；多模式 Trie + fail 指针 |
| Trie 删除的坑？ | 递归回溯 + 空分支剪除，注意 isEnd 维护 |

## 4. 易错点清单

| # | 易错点 | 后果 | 对策 |
|---|--------|------|------|
| 1 | search 忘查 isEnd | 「cat」匹配「cattle」误判 | 下钻后检查 isEnd |
| 2 | 字符映射忘减 'a' | 数组越界 | `c - 'a'` |
| 3 | 插入忘标记 isEnd | 单词查不到 | 末尾 isEnd = true |
| 4 | 212 回溯忘还原 | 路径污染 | 递归后还原字符 |
| 5 | 212 结果去重 | 重复单词 | HashSet 收集 |
| 6 | 01-Trie 位数错 | 高位移位丢失 | 从 30 位开始 |
| 7 | 421 先插入再查询 | 自异或 = 0 | 先查询后插入 |
| 8 | 01-Trie 无相反位分支 | NPE | else 走同向分支 |
| 9 | 计数 Trie 忘在路径上累加 | 计数为 0 | 每个节点 count++ |
| 10 | 删除后 isEnd 状态残留 | 幽灵单词 | 删除时清 isEnd |

> 🎯 **核心要点**：Trie 面试通关——四模板 + 10 道高频题 + 十个追问；识别信号「前缀/词根/推荐/词典中找」→ Trie，「异或最值」→ 01-Trie，「网格找词典词」→ Trie + DFS；进阶方向：AC 自动机（多模式）、双数组 Trie（工程）、[专题 4 精要](../../../03-高频专题模块/专题 4：树（二叉树为主）/字典树/01-字典树进阶：01Trie与搜索剪枝.md)。

---

**上一模块**：[05-位运算与 01-Trie](05-位运算与01-Trie.md) ｜ **返回总览**：[00-字典树 Trie 知识体系总览](00-字典树 Trie 知识体系总览.md)

**【参考来源】**
- LeetCode Trie 题单：https://leetcode.cn/tag/trie/
- 算法导论（CLRS）第 26 章
