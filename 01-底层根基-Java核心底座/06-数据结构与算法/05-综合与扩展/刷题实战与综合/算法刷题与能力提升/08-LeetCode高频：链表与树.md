# 08 - LeetCode 高频：链表与树

> 🎯 链表和树是大厂面试的"基本功测试"——链表考指针操作，树考遍历和递归。这两类题写不出来基本等于"不合格"

---

## 目录

1. [链表必刷题](#1-链表必刷题)
2. [树必刷题](#2-树必刷题)

---

## 1. 链表必刷题

```java
// LRU 缓存（HashMap + 双向链表）⭐⭐⭐⭐⭐
class LRUCache {
    private final int capacity;
    private final Map<Integer, Node> map;
    private final Node head, tail;  // 哑头哑尾

    public LRUCache(int capacity) {
        this.capacity = capacity;
        map = new HashMap<>();
        head = new Node(0, 0); tail = new Node(0, 0);
        head.next = tail; tail.prev = head;
    }
    public int get(int key) {
        Node node = map.get(key);
        if (node == null) return -1;
        moveToHead(node);
        return node.value;
    }
    public void put(int key, int value) {
        Node node = map.get(key);
        if (node != null) { node.value = value; moveToHead(node); }
        else {
            node = new Node(key, value);
            map.put(key, node);
            addToHead(node);
            if (map.size() > capacity) {
                Node removed = removeTail();
                map.remove(removed.key);
            }
        }
    }
    private void moveToHead(Node node) { removeNode(node); addToHead(node); }
    private void addToHead(Node node) { node.next = head.next; head.next.prev = node; head.next = node; node.prev = head; }
    private void removeNode(Node node) { node.prev.next = node.next; node.next.prev = node.prev; }
    private Node removeTail() { Node node = tail.prev; removeNode(node); return node; }

    static class Node { int key, value; Node prev, next; Node(int k, int v) { key = k; value = v; } }
}

// 环形链表检测 + 找入环点（快慢指针）
public ListNode detectCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next; fast = fast.next.next;
        if (slow == fast) {           // 相遇 → 有环
            ListNode ptr = head;
            while (ptr != slow) { ptr = ptr.next; slow = slow.next; }
            return ptr;               // 入环点
        }
    }
    return null;
}
```

### 链表高频题型

| 题目 | 核心技巧 | 难度 |
|------|---------|:---:|
| 反转链表 | prev/curr/next 三指针 | E |
| 环形链表检测 | 快慢指针 | M |
| 合并 K 个升序链表 | 优先队列 | H |
| LRU 缓存 | HashMap + 双向链表 | M |
| 删除倒数第 N 个 | 快指针先走 N 步 | M |
| 链表相交 | 双指针走对方路径 | E |

## 2. 树必刷题

```java
// 二叉树的最近公共祖先 LCA（后序遍历）
public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;
    TreeNode left = lowestCommonAncestor(root.left, p, q);
    TreeNode right = lowestCommonAncestor(root.right, p, q);
    if (left != null && right != null) return root;  // p和q在两边 → root是LCA
    return left != null ? left : right;                // 在一侧 → 找到了谁返回谁
}

// 路径总和 III（前缀和 + HashMap）
public int pathSum(TreeNode root, int targetSum) {
    Map<Long, Integer> prefix = new HashMap<>();
    prefix.put(0L, 1);
    return dfs(root, 0L, targetSum, prefix);
}
private int dfs(TreeNode node, long curr, int target, Map<Long, Integer> prefix) {
    if (node == null) return 0;
    curr += node.val;
    int count = prefix.getOrDefault(curr - target, 0);
    prefix.merge(curr, 1, Integer::sum);
    count += dfs(node.left, curr, target, prefix) + dfs(node.right, curr, target, prefix);
    prefix.merge(curr, -1, Integer::sum);  // 回溯
    return count;
}
```

### 树高频题型

| 题目 | 核心技巧 | 难度 |
|------|---------|:---:|
| 最大深度 | 递归 | E |
| 验证 BST | 中序遍历检查递增 | M |
| 层序遍历 | BFS + 队列 | M |
| LCA | 后序遍历 | M |
| 路径总和 III | 前缀和 + 回溯 | M |
| 序列化反序列化 | 前序遍历 + 队列 | H |

## 核心要点回顾

- LRU = HashMap(O(1)查找) + 双向链表(O(1)移动)，面试必写
- 链表环检测 = 快慢指针相遇，找入环点 = 慢指针+头指针同步走
- LCA = 后序遍历，左右都有 → root 是 LCA
- 树的路径问题 = 前缀和 + HashMap + 回溯

## 参考资料

1. LeetCode 链表/树 标签
2. 《剑指 Offer》树章节
