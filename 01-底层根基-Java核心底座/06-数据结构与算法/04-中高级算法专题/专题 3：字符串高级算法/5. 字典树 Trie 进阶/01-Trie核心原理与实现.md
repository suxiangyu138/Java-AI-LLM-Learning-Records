# 01-Trie核心原理与实现
> 一句话：字符路径组织字符串集合——前缀检索 O(L)、空间共享前缀，208/211/212 的底层结构

## 📚 目录
1. [Trie 模型：字符路径树](#1-trie-模型字符路径树)
2. [Trie 实现（208 模板）](#2-trie-实现208-模板)
3. [前缀共享：Trie 的空间本质](#3-前缀共享trie-的空间本质)
4. [Trie 变体：词频与删除](#4-trie-变体词频与删除)
5. [复杂度分析](#5-复杂度分析)
6. [识别信号与易错点](#6-识别信号与易错点)

---

## 1. Trie 模型：字符路径树

```text
Trie（前缀树/字典树）: 用字符路径组织字符串集合
  根 → 每条路径 = 一个字符串（前缀共享）

插入 "apple"、"app"、"april":
      根
      a
      p
      p
     / \
    l   r
    e   i
    (apple) l
        (april)

⚠️ 性质:
  ① 路径 = 字符串：从根到任意节点 = 一个前缀
  ② 前缀共享: 公共前缀只存一次（空间高效）
  ③ 查询 O(L)：按字符沿路径走（L = 串长）
  ④ 节点标记: isEnd 标记"是否有串在此结束"
```

> 🎯 **核心认知**："**Trie = '字符路径组织字符串'——公共前缀共享、查询 O(L)**。它是 AC 自动机的骨架、208/211/212 的底层，也是'前缀检索'类问题的标准答案。"

## 2. Trie 实现（208 模板）

### 2.1 完整实现（LeetCode 208）

```java
/**
 * LeetCode 208. 实现 Trie（前缀树）
 * 三种操作: insert / search / startsWith
 */
class Trie {
    private static class Node {
        Node[] children = new Node[26];        // ① 字符边（小写字母）
        boolean isEnd;                          // ② 结束标记
    }

    private final Node root = new Node();

    public void insert(String word) {
        Node cur = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (cur.children[idx] == null) cur.children[idx] = new Node();
            cur = cur.children[idx];            // ③ 沿路径走
        }
        cur.isEnd = true;                       // ④ 标记结束
    }

    public boolean search(String word) {
        Node cur = find(word);                  // ⑤ 走完路径
        return cur != null && cur.isEnd;        // ⑥ 完整字符串才 true
    }

    public boolean startsWith(String prefix) {
        return find(prefix) != null;            // ⑦ 前缀存在即 true
    }

    private Node find(String s) {
        Node cur = root;
        for (char c : s.toCharArray()) {
            int idx = c - 'a';
            if (cur.children[idx] == null) return null;   // ⑧ 断链 → 不存在
            cur = cur.children[idx];
        }
        return cur;
    }
}
// 推演 insert("apple") → search("app")=false → startsWith("app")=true ✓
// ⚠️ 核心差异: search 要 isEnd、startsWith 只要路径存在——
//   "完整词 vs 前缀"的判定就是 isEnd 的检查
```

> 🎯 **208 的模板地位**："**208 = Trie 的 'Hello World'——insert/search/startsWith 三操作 + isEnd 语义**。会它，211 单词搜索 II、词典系统全是变体。"

## 3. 前缀共享：Trie 的空间本质

### 3.1 空间对比

```text
朴素存储 K 个字符串: O(Σ|sᵢ|)（无共享）
Trie 存储: O(Σ|sᵢ|) 最坏（无公共前缀）、远小于（有共享）

例: "app", "apple", "application"——
  朴素: 3 + 5 + 11 = 19 字符
  Trie: "app" 共享 → 3 + 2 + 8 = 13 节点（省 32%）

⚠️ 共享收益: 公共前缀越多越省——
  英语词典场景（大量共享前缀）空间优势巨大

⚠️ 代价: 每个节点 26 个孩子指针（数组实现）——
  稀疏时空间浪费 → 变体用哈希表/压缩 Trie（Radix Tree）
```

### 3.2 数组 vs 哈希孩子

| 实现 | 孩子存储 | 查询 | 空间 |
|------|:---:|:---:|:---:|
| 数组（26） | 固定槽 | O(1) 最快 | 稀疏浪费 |
| 哈希表 | 动态映射 | O(1) 平均 | 按需分配 |
| 压缩（Radix） | 边存字符串 | O(L) | 最省（路径压缩） |

```text
⚠️ 选型: 字符集小且固定 → 数组；字符集大/稀疏 → 哈希；
  竞赛/工程大规模 → Radix Tree（IP 路由表标准）
```

> 💡 **Trie 的工程形态**："**Trie 家族 = 数组版（208）/哈希版（字符集大）/Radix Tree（路径压缩）**——Linux IP 路由表、自动补全、拼写检查都是它的工程形态。"

## 4. Trie 变体：词频与删除

### 4.1 词频统计

```java
// 词频 Trie: 节点存 count（经过次数）
// insert 时沿途 count++；查询时返回节点 count
class FreqNode {
    FreqNode[] children = new FreqNode[26];
    int count;                                   // ① 经过此节点的次数
}
// 应用: 统计"以某前缀开头的词数"——
//   count = 前缀路径末节点的值 ✓
// 例: 插入 "app"×2、"apple"×1 → 前缀 "app" 的 count = 3
```

### 4.2 删除

```java
// 删除: 递归删除——从叶子向上删无用的节点
boolean delete(Node node, String word, int depth) {
    if (node == null) return false;
    if (depth == word.length()) {
        if (!node.isEnd) return false;           // ① 不存在
        node.isEnd = false;                      // ② 清除结束标记
        return isEmpty(node);                    // ③ 是否可删（无孩子）
    }
    int idx = word.charAt(depth) - 'a';
    boolean shouldDelete = delete(node.children[idx], word, depth + 1);
    if (shouldDelete) node.children[idx] = null; // ④ 删无用子树
    return !node.isEnd && isEmpty(node);         // ⑤ 向上传播
}
// ⚠️ 删除要点: 只删"不再被其他词使用"的节点——
//   先清 isEnd、再判断是否可删（自底向上）
```

## 5. 复杂度分析

| 操作 | 数组实现 | 说明 |
|------|:---:|------|
| insert | O(L) | 沿路径 L 个节点 |
| search | O(L) | 同上 |
| startsWith | O(L) | 同上 |
| 空间 | O(Σ 字符数 × 26) | 节点 × 孩子槽 |

```text
⚠️ 与哈希表的对比:
  哈希: 插入/查询 O(L) 平均，但不支持"前缀检索"
  Trie: 插入/查询 O(L) + 天然支持前缀——
  "前缀查询"是 Trie vs 哈希的分水岭

⚠️ 面试表达:
  "Trie 的查询 O(L)（L = 串长），
   与数据量无关——'前缀共享 + 沿路径走'
   让大量字符串集合的前缀检索高效"
```

## 6. 识别信号与易错点

### 6.1 识别信号

```text
"前缀检索/自动补全/词典" → Trie
"多个字符串 + 前缀统计" → Trie
"单词搜索/字典匹配" → Trie（212）
"异或最大值（位 Trie）" → Trie 变体（421）

⚠️ 面试定位: Trie 是面试高频——
  208 手撕、211/212 变体、前缀统计是常见追问
```

### 6.2 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | search 忘 isEnd | 路径存在 ≠ 词存在 | "app" 误判存在 |
| 2 | startsWith 用 isEnd | 只要路径存在 | 前缀误判不存在 |
| 3 | 孩子数组忘初始化 | 默认 null ✓ | — |
| 4 | 删除忘清 isEnd | 先清标记再删节点 | 词残留 |
| 5 | 删除忘判空节点 | isEmpty 判断 | 误删共享路径 |
| 6 | 字符索引越界 | `c - 'a'` 校验 | 越界 |
| 7 | 递归深度（长串） | 迭代或注意深度 | 栈溢出 |

> 🎯 **核心要点**：Trie 通关四件事——**① 路径模型**（字符边 + isEnd）；**② 208 三操作**（search vs startsWith 的 isEnd 差异）；**③ 前缀共享**（空间本质）；**④ 复杂度**（O(L) 与数据量无关）。**Trie = '前缀共享的字符树'——'沿路径走、isEnd 判词'十个字，就是它的全部实现**。

---

**下一篇**：[02-Trie应用与题解](02-Trie应用与题解.md)
**返回总览**：[00-字符串高级算法知识体系总览](../00-字符串高级算法知识体系总览.md)
