# 06 - Java 实现与面试实战

> BST 面试收尾篇：Java TreeMap/TreeSet 工程实践、五个必背模板、10 道高频题、面试追问话术与易错点——BST 从入门到面试的最后一公里。

## 📚 目录

1. [Java 工程实践：TreeMap/TreeSet](#1)
2. [五大必背模板](#2)
3. [高频题解速览](#3)
4. [面试追问与答题话术](#4)
5. [易错点清单](#5)

## 1. Java 工程实践：TreeMap/TreeSet

Java 的红黑树实现（TreeMap/TreeSet）提供 BST 的全部有序操作：

```java
TreeMap<Integer, String> map = new TreeMap<>();
map.put(5, "five"); map.put(2, "two"); map.put(8, "eight");

map.firstKey();                    // 最小键 2
map.lastKey();                     // 最大键 8
map.floorKey(4);                   // ≤ 4 的最大键 2
map.ceilingKey(4);                 // ≥ 4 的最小键 5
map.lowerKey(5);                   // < 5 的最大键 2
map.higherKey(5);                  // > 5 的最小键 8
map.subMap(2, 8);                  // 区间 [2, 8)
```

| 场景 | 用 TreeMap/TreeSet | 用 HashMap/HashSet |
|------|-------------------|-------------------|
| 需要有序遍历/极值/区间 | ✅ 红黑树 O(log n) | ❌ 无序 |
| 只需要 O(1) 查增删 | ❌ 慢（O(log n)） | ✅ |
| 「最近/最接近 x」 | ✅ floor/ceiling | ❌ 需全扫 |

> 💡 工程高频场景：动态维护有序集合取极值（数据流）、求最接近元素、区间统计——TreeMap 的 floorKey/ceilingKey 是 LeetCode 中「有序集合」题的核心（[有序集合](../../../04-中高级算法专题/专题 4：高级数据结构/高级数据结构/有序集合/)体系衔接）。

## 2. 五大必背模板

### 模板一：验证 BST（区间传递）

```java
boolean valid(TreeNode node, Integer min, Integer max) {
    if (node == null) return true;
    if (min != null && node.val <= min) return false;
    if (max != null && node.val >= max) return false;
    return valid(node.left, min, node.val)
        && valid(node.right, node.val, max);
}
```

### 模板二：递归返回挂接（插入/删除）

```java
TreeNode insert(TreeNode root, int val) {
    if (root == null) return new TreeNode(val);
    if (val < root.val) root.left = insert(root.left, val);
    else root.right = insert(root.right, val);
    return root;
}
```

### 模板三：删除三态

```java
TreeNode delete(TreeNode root, int key) {
    if (root == null) return null;
    if (key < root.val) root.left = delete(root.left, key);
    else if (key > root.val) root.right = delete(root.right, key);
    else {
        if (root.left == null) return root.right;
        if (root.right == null) return root.left;
        TreeNode min = root.right;
        while (min.left != null) min = min.left;
        root.val = min.val;
        root.right = delete(root.right, min.val);
    }
    return root;
}
```

### 模板四：中序迭代（一切有序操作的地基）

```java
void inorder(TreeNode root) {
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode cur = root;
    while (cur != null || !stack.isEmpty()) {
        while (cur != null) { stack.push(cur); cur = cur.left; }
        cur = stack.pop();
        // ★ 业务逻辑写在这里（第 K 小/相邻比较/prev 串接）
        cur = cur.right;
    }
}
```

### 模板五：有序数组 → 平衡 BST

```java
TreeNode build(int[] nums, int l, int r) {
    if (l > r) return null;
    int mid = l + (r - l) / 2;
    TreeNode root = new TreeNode(nums[mid]);
    root.left = build(nums, l, mid - 1);
    root.right = build(nums, mid + 1, r);
    return root;
}
```

> 🎯 五个模板约 60 行——闭眼默写是 BST 面试及格线。

## 3. 高频题解速览

| 题号 | 题目 | 考点 | 一句话思路 |
|------|------|------|-----------|
| 98 | 验证 BST | 区间传递 | (min, max) 收紧 |
| 700 | 搜索 | 二分查找 | 小左大右 |
| 701 | 插入 | 返回挂接 | 空位落点 |
| 450 | 删除 | 三态 | 双子右最小替代 |
| 230 | 第 K 小 | 中序计数 | 第 K 个访问 |
| 235 | LCA | 值比较 | 分岔点 |
| 108 | 有序数组→BST | 二分递归 | 中点做根 |
| 426 | BST→双向链表 | 中序+prev | 原地串接 |
| 669 | 修剪 | 有序剪枝 | 越界整片舍 |
| 653 | 两数之和 | 中序+双指针 | 有序双指针 |
| 501 | 众数 | 中序相邻 | 相同值连续 |
| 99 | 恢复 BST | 中序逆序对 | 交换值 |

## 4. 面试追问与答题话术

| 追问 | 答题要点 |
|------|---------|
| BST 与有序数组/链表的取舍？ | BST：查找 O(h)、插入 O(h)；数组查找 O(log n) 但插入 O(n)；链表插入 O(1) 但查找 O(n)——BST 是折中 |
| 为什么删除双子树用右最小？ | 保持 BST 性质（> 根、< 右子树其余）；实现简单（只删叶子/单子节点） |
| BST 一定会快吗？ | 不一定——有序插入退化斜树 O(n)；平衡树（AVL/红黑）保证 O(log n) |
| 验证 BST 为什么不能只比左右孩子？ | 跨层违规：右子树里可能藏比根小的节点；必须区间传递 |
| 中序遍历在 BST 题里的地位？ | 一切有序操作的地基：第 K 小/相邻比较/双向链表/累加树 |
| 有重复值的 BST 怎么处理？ | 左 ≤ 根 < 右（允许左重复）或加 count 字段；验证边界用 ≤/≥ |
| Java 的 TreeMap 底层？ | 红黑树：平衡 BST 的一种，保证 O(log n)，提供 floor/ceiling 等有序 API |
| BST 与堆的区别？ | BST 全局有序（中序升序）、查任意值 O(h)；堆只保证根最值、查任意值 O(n) |

## 5. 易错点清单

| # | 易错点 | 后果 | 对策 |
|---|--------|------|------|
| 1 | 验证只比直接孩子 | 跨层违规误判 | 区间传递 (min, max) |
| 2 | 删除双子直接删 | 丢子树 | 右最小替代再删 |
| 3 | 递归不挂接返回值 | 树断裂 | root.left = 递归(...) |
| 4 | 相等值走错方向 | 死循环/漏插 | 明确 < 与 ≥ 分支 |
| 5 | 中序 prev 忘更新 | 相邻比较失效 | 每节点访问后 prev = node |
| 6 | 插入重复值 | 违反唯一性 | 题目约定或改 count |
| 7 | 230 中序全收集 | 空间浪费 | 计数提前返回 |
| 8 | 426 忘首尾相连 | 非循环链表 | head.left = prev; prev.right = head |
| 9 | 逆序对 second 只取第一个 | 非相邻交换修错 | second 持续更新 |
| 10 | 忽略退化风险 | 误以为 O(log n) | 提平衡树（AVL/红黑） |

> 🎯 **核心要点**：BST 面试通关——五个模板 + 12 道高频题 + 八个追问；核心心法「**中序升序 × 有序剪枝 × 返回挂接**」；面试进阶方向：平衡树（AVL/红黑）为什么存在、TreeMap 底层、顺序统计树。衔接：[AVL 树](../AVL 树（平衡二叉搜索树）/) / [红黑树](../红黑树 Red‑Black Tree/) / [专题 4 BST 精要](../../../03-高频专题模块/专题 4：树（二叉树为主）/二叉搜索树/00-二叉搜索树专题精要.md)。

---

**上一模块**：[05-BST 的应用](05-BST的应用.md) ｜ **返回总览**：[00-二叉搜索树 BST 知识体系总览](00-二叉搜索树 BST 知识体系总览.md)

**【参考来源】**
- LeetCode BST 题单：https://leetcode.cn/studyplan/binary-search-tree/
- Java TreeMap 文档：https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html
