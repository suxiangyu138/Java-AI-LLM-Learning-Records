# 06 - Java 实现与面试实战

> B 树家族面试收尾：B+ 树简化实现（Node/查找/插入分裂）、8 道高频题、面试追问话术、易错点——从原理到手写再到 MySQL 面试的完整闭环。

## 📚 目录

1. [B+ 树简化实现](#1)
2. [高频考点速览](#2)
3. [面试追问与答题话术](#3)
4. [易错点清单](#4)
5. [面试速记卡](#5)

## 1. B+ 树简化实现

```java
// B+ 树简化实现（定阶 m=4，仅覆盖查找/插入核心逻辑）
// 完整工程实现（删除/链表维护）远超篇幅——这里掌握核心骨架
class BPlusTree {
    private static final int ORDER = 4;          // 阶（教学用小阶）
    private Node root = new LeafNode();

    // ===== 节点抽象 =====
    abstract static class Node {
        int[] keys = new int[ORDER - 1];         // 键（最多 m-1）
        int keyCount = 0;

        abstract int search(int key);            // 返回键位置
        abstract Node getChild(int i);
    }

    static class InternalNode extends Node {
        Node[] children = new Node[ORDER];       // 孩子（最多 m）

        @Override int search(int key) {          // 找第一个 ≥ key 的位置
            int i = 0;
            while (i < keyCount && key > keys[i]) i++;
            return i;
        }
        @Override Node getChild(int i) { return children[i]; }
    }

    static class LeafNode extends Node {
        LeafNode next;                           // 叶子链表（B+ 关键）

        @Override int search(int key) {
            int i = 0;
            while (i < keyCount && key > keys[i]) i++;
            return i;
        }
        @Override Node getChild(int i) { return null; }
    }

    // ===== 查找（总是到叶子）=====
    public boolean search(int key) {
        Node node = root;
        while (node instanceof InternalNode) {
            InternalNode internal = (InternalNode) node;
            node = internal.getChild(internal.search(key));  // 逐层下降
        }
        LeafNode leaf = (LeafNode) node;
        int i = leaf.search(key);
        return i < leaf.keyCount && leaf.keys[i] == key;
    }

    // ===== 插入（叶子插入 + 满则分裂）=====
    public void insert(int key) {
        Result result = insert(root, key);
        if (result != null) {                    // 根分裂 → 树长高
            InternalNode newRoot = new InternalNode();
            newRoot.keys[0] = result.upKey;
            newRoot.children[0] = root;
            newRoot.children[1] = result.newNode;
            newRoot.keyCount = 1;
            root = newRoot;
        }
    }

    // 返回分裂信息（upKey = 上提键，newNode = 新右节点）；null = 未分裂
    private Result insert(Node node, int key) {
        if (node instanceof LeafNode) {
            LeafNode leaf = (LeafNode) node;
            insertIntoNode(leaf, key);           // 叶子插入
            return leaf.keyCount == ORDER - 1 ? splitLeaf(leaf) : null;
        }
        InternalNode internal = (InternalNode) node;
        int i = internal.search(key);
        Result r = insert(internal.children[i], key);
        if (r == null) return null;
        // 孩子分裂：键上提 + 新节点挂接
        insertIntoNode(internal, r.upKey);
        shiftRight(internal, i + 1);
        internal.children[i + 1] = r.newNode;
        return internal.keyCount == ORDER - 1 ? splitInternal(internal) : null;
    }

    private void insertIntoNode(Node node, int key) {
        int i = node.keyCount - 1;
        while (i >= 0 && key < node.keys[i]) {
            node.keys[i + 1] = node.keys[i];
            i--;
        }
        node.keys[i + 1] = key;
        node.keyCount++;
    }

    private void shiftRight(InternalNode node, int from) {
        for (int i = node.keyCount; i > from; i--) {
            node.children[i] = node.children[i - 1];
        }
    }

    // 叶子分裂：中键上提 + 链表维护
    private Result splitLeaf(LeafNode leaf) {
        int mid = ORDER / 2;
        LeafNode right = new LeafNode();
        right.keyCount = leaf.keyCount - mid;
        System.arraycopy(leaf.keys, mid, right.keys, 0, right.keyCount);
        leaf.keyCount = mid;
        right.next = leaf.next;                  // 链表维护
        leaf.next = right;
        return new Result(right.keys[0], right);
    }

    private Result splitInternal(InternalNode node) {
        int mid = ORDER / 2;
        InternalNode right = new InternalNode();
        right.keyCount = node.keyCount - mid - 1;
        System.arraycopy(node.keys, mid + 1, right.keys, 0, right.keyCount);
        System.arraycopy(node.children, mid + 1, right.children, 0, right.keyCount + 1);
        node.keyCount = mid;
        return new Result(node.keys[mid], right);  // 中键上提
    }

    static class Result {
        int upKey;
        Node newNode;
        Result(int upKey, Node newNode) {
            this.upKey = upKey;
            this.newNode = newNode;
        }
    }
}
```

> ⚠️ **实现要点**：内部节点分裂时中键**上提**（留在父）、叶子分裂时中键**保留在右叶**（B+ 树语义：内部键是路由、叶子键是数据）；叶子链表在分裂时维护——这是 B+ 与 B 树的代码差异点。

## 2. 高频考点速览

| 考点 | 关键点 |
|------|--------|
| 树高公式 | log_B n；10 亿数据 3-4 层 |
| 键数范围 | ⌈m/2⌉-1 ~ m-1（非根） |
| I/O 次数 | = 树高（根常驻内存再 -1） |
| 分裂 | 中键上提（插入长高唯一方式） |
| 合并 | 父键下移（删除变矮唯一方式） |
| B+ 优势 | 数据只叶子 + 叶子链表 |
| 聚簇索引 | InnoDB 主键索引（叶子整行） |
| 回表/覆盖 | 二级索引取主键 → 聚簇；覆盖免回表 |
| 最左前缀 | 联合索引前缀连续 |
| 与红黑树 | 内存 vs 磁盘（I/O 视角） |

## 3. 面试追问与答题话术

| 追问 | 答题要点 |
|------|---------|
| 为什么 B 树不用二叉树？ | 树高 = I/O 次数；多路节点让树高从 30 降到 3-4 |
| B+ 树比 B 树好在哪？ | 数据只在叶子（页利用率翻倍）+ 叶子链表（范围查询顺序读） |
| 单点查询 B+ 一定比 B 快？ | 否——同阶；B+ 胜在范围与页利用率 |
| 为什么 InnoDB 用 B+ 不用 B？ | 聚簇索引 + 范围查询是 MySQL 核心场景 |
| 树高怎么算？ | 页 16KB ÷ (键+指针) ≈ 1170 → 3 层 2000 万行 |
| 为什么主键要自增？ | 单调插入避免页分裂（UUID 随机插入退化） |
| 索引为什么用最左前缀？ | B+ 树按联合列整体排序，缺前缀无法定位 |
| 覆盖索引为什么快？ | 叶子直接返回，免回表（少一次 B+ 树） |
| 删除为什么合并？ | 键数低于下限 ⌈m/2⌉-1 → 下溢 → 借/合并 |
| 与 LSM 树对比？ | B+ 读快写慢、LSM 写快读慢（日志场景） |

## 4. 易错点清单

| # | 易错点 | 后果 | 对策 |
|---|--------|------|------|
| 1 | 树高算错（忘减根） | I/O 次数多算 1 | 根常驻内存，实际 I/O = 树高 - 1 |
| 2 | 键数上下限记反 | 概念错误 | ⌈m/2⌉-1（下）~ m-1（上） |
| 3 | 叶子分裂中键也上提 | 数据重复丢失 | 叶子分裂中键留在右叶 |
| 4 | 忘维护叶子链表 | 范围查询断裂 | 分裂/合并时更新 next |
| 5 | 回表概念混淆 | 答错 | 二级 → 主键 → 聚簇 |
| 6 | 覆盖索引与联合索引混淆 | 答错 | 覆盖看「查询列 ⊆ 索引列」 |
| 7 | 最左前缀理解片面 | 优化失误 | 缺最左失效、中间断档 |
| 8 | 主键设计随意（UUID） | 页分裂性能退化 | 自增单调主键 |
| 9 | 合并节点数超上限 | 结构非法 | 两半 + 父键 ≤ m-1 天然成立 |
| 10 | 与红黑树场景混淆 | 选型错误 | 内存红黑树、磁盘 B+ |

## 5. 面试速记卡

```text
B 树家族 30 秒速记:

「树高 = I/O 次数」—— B 树把树高压到 3-4
「数据只在叶子 + 叶子链表」—— B+ 树的两大改动
「表即索引」—— InnoDB 聚簇索引存整行
「二级索引存主键」—— 回表 / 覆盖索引
「最左前缀」—— 联合索引的前缀连续原则
「内存红黑树、磁盘 B+ 树、日志 LSM」—— 选型三句
```

**树的体系收官**（回顾整个树结构目录）：

```text
二叉树（形态）→ BST（有序）→ AVL/红黑（内存平衡）
→ B 树/B+ 树（磁盘平衡）→ Trie（字符分叉）→ 堆（取最值）
→ 普通树（工程形态）→ 各种树（全景对比）
```

> 🎯 **核心要点**：B 树家族面试通关——简化 B+ 实现能讲清（Node 抽象/查找到叶子/分裂上提/链表维护）；「树高 = I/O 次数」贯穿一切回答；InnoDB 实战四连（聚簇/二级/回表/覆盖）+ 最左前缀；选型三句（内存红黑、磁盘 B+、日志 LSM）；至此树结构 9 大体系全部完成——树的知识版图闭环。

---

**上一模块**：[05-数据库索引实战](05-数据库索引实战.md) ｜ **返回总览**：[00-B 树 B+树知识体系总览](00-B 树 B+树知识体系总览.md)

**【参考来源】**
- 算法导论（CLRS）第 18 章
- MySQL 官方文档（InnoDB 索引）
