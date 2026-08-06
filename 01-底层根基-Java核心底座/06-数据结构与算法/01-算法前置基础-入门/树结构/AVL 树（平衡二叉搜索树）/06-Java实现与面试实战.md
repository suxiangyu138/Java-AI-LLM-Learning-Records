# 06 - Java 实现与面试实战

> AVL 面试收尾：完整 AVL 类（插入/删除/旋转全实现）、高频考点、面试话术、易错点——手写 AVL 的「通关代码」全在这篇。

## 📚 目录

1. [完整 AVL 实现](#1)
2. [高频考点速览](#2)
3. [面试追问与答题话术](#3)
4. [易错点清单](#4)

## 1. 完整 AVL 实现

```java
// AVL 树完整实现（插入/删除/查找/遍历）
class AVLTree {
    private AVLNode root;

    // ===== 对外 API =====
    public void insert(int val) { root = insert(root, val); }
    public void delete(int val) { root = delete(root, val); }
    public boolean search(int val) {
        AVLNode node = root;
        while (node != null) {
            if (val == node.val) return true;
            node = val < node.val ? node.left : node.right;
        }
        return false;
    }
    public int height() { return height(root); }

    // ===== 插入（BST + 回溯平衡）=====
    private AVLNode insert(AVLNode node, int val) {
        if (node == null) return new AVLNode(val);
        if (val < node.val) node.left = insert(node.left, val);
        else if (val > node.val) node.right = insert(node.right, val);
        else return node;
        updateHeight(node);
        return balance(node);
    }

    // ===== 删除（三态 + 回溯平衡）=====
    private AVLNode delete(AVLNode node, int val) {
        if (node == null) return null;
        if (val < node.val) {
            node.left = delete(node.left, val);
        } else if (val > node.val) {
            node.right = delete(node.right, val);
        } else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            AVLNode min = node.right;
            while (min.left != null) min = min.left;
            node.val = min.val;
            node.right = delete(node.right, min.val);
        }
        if (node == null) return null;
        updateHeight(node);
        return balance(node);
    }

    // ===== 平衡分发 =====
    private AVLNode balance(AVLNode node) {
        int bf = balanceFactor(node);
        if (bf > 1) {
            if (balanceFactor(node.left) >= 0) return rotateRight(node);
            node.left = rotateLeft(node.left);
            return rotateRight(node);
        } else if (bf < -1) {
            if (balanceFactor(node.right) <= 0) return rotateLeft(node);
            node.right = rotateRight(node.right);
            return rotateLeft(node);
        }
        return node;
    }

    // ===== 旋转 =====
    private AVLNode rotateRight(AVLNode a) {
        AVLNode b = a.left;
        a.left = b.right;
        b.right = a;
        updateHeight(a);
        updateHeight(b);
        return b;
    }

    private AVLNode rotateLeft(AVLNode a) {
        AVLNode b = a.right;
        a.right = b.left;
        b.left = a;
        updateHeight(a);
        updateHeight(b);
        return b;
    }

    // ===== 工具 =====
    private int height(AVLNode n) { return n == null ? 0 : n.height; }
    private int balanceFactor(AVLNode n) { return height(n.left) - height(n.right); }
    private void updateHeight(AVLNode n) {
        n.height = Math.max(height(n.left), height(n.right)) + 1;
    }

    static class AVLNode {
        int val;
        AVLNode left, right;
        int height = 1;                // 新节点高度 1
        AVLNode(int val) { this.val = val; }
    }
}
```

> 🎯 **手写验收**：20 分钟内默写完整类——insert/delete/balance/两个旋转/工具方法，这是平衡树面试的满分卷。

## 2. 高频考点速览

| 考点 | 关键点 |
|------|--------|
| 平衡因子 | 左高 - 右高；±2 失衡 |
| 四种失衡 | LL/RR 单旋、LR/RL 双旋（同向单异向双） |
| 插入修复 | 最多一次旋转（高度复原） |
| 删除修复 | 回溯到根，最多 O(log n) 次 |
| 树高上界 | 1.44·log₂(n+1)（最矮平衡树） |
| 与红黑树 | 读多 AVL、写多红黑 |
| 中序性质 | 中序升序（BST 性质保持） |
| 复杂度 | 增删查全部 O(log n) |

**与 BST 体系的呼应**：AVL = BST + 平衡约束——[BST 增删查](../二叉搜索树 BST（二叉排序树）/02-BST的增删查.md)模板完全复用，只多 balance 一步。

## 3. 面试追问与答题话术

| 追问 | 答题要点 |
|------|---------|
| 为什么 AVL 查询最快？ | 高差 ≤ 1 严格约束 → 树高最小（1.44 log n） |
| 插入为什么一次旋转就够？ | 旋转后子树高度复原 → 上层自动平衡 |
| 删除为什么可能多次旋转？ | 节点真的减少 → 高度损失无法弥补 → 逐层回溯 |
| LL/LR 怎么区分？ | 看失衡根的孩子侧 + 孙侧（同向单旋异向双旋） |
| 旋转为什么保持 BST？ | 中序序列不变（T1 B T2 A T3 平移） |
| 树高上界怎么证明？ | 高度 h 的 AVL 最少节点 F(h)=F(h-1)+F(h-2)+1 |
| 递归 vs 迭代实现？ | 递归天然回溯；迭代需显式 parent 栈 |
| 高度字段存什么？ | 子树高度（int）；更新顺序先下后上 |
| AVL 能用于工程容器吗？ | 能但不是默认（读写均衡时红黑树更优） |
| 与 2-3-4 树的关系？ | 都是平衡 BST 的不同约束（高度 vs 颜色） |

## 4. 易错点清单

| # | 易错点 | 后果 | 对策 |
|---|--------|------|------|
| 1 | 更新高度顺序反了 | 高度错乱 | 先下沉后上提（先 A 后 B） |
| 2 | LR/RL 忘记先旋孩子 | 旋转无效 | 双旋 = 先掰直再修 |
| 3 | 删除后不回溯到根 | 上层失衡未修 | 每层递归都 balance |
| 4 | 平衡因子用「左右孩子高度差」而非子树高 | 计算错误 | 左高 - 右高（子树高） |
| 5 | 新节点 height 忘记初始化 1 | 高度全 0 | `height = 1` |
| 6 | 旋转返回的根没挂回父节点 | 树断裂 | 递归返回挂接模式 |
| 7 | 删除双子替代后不递归删替代者 | 重复节点 | `node.right = delete(node.right, min.val)` |
| 8 | balance 只在插入时调用 | 删除后失衡 | 插入删除都调用 |
| 9 | 误以为 AVL 删除也是「一次旋转」 | 漏修 | 回溯到根（最多 O(log n)） |
| 10 | 与红黑树混淆旋转次数 | 面试答错 | 插入 ≤1、删除 ≤O(log n) |

> 🎯 **核心要点**：AVL 面试通关——完整类默写（20 分钟）；「同向单旋异向双旋」+「插入一次、删除回溯」两个口诀；树高 1.44 log n 证明与斐波那契递推；选型一句话（读多 AVL、写多红黑）；姊妹篇[红黑树](../红黑树 Red‑Black Tree/00-红黑树知识体系总览.md)与[搜索树家族对比](../计算机中的各种树/01-搜索树家族对比.md)交叉复习效果最佳。

---

**上一模块**：[05-AVL vs 红黑树](05-AVLvs红黑树.md) ｜ **返回总览**：[00-AVL 树知识体系总览](00-AVL 树知识体系总览.md)

**【参考来源】**
- LeetCode 平衡树相关题（验证平衡：110）
- 维基百科 AVL tree（C++ 实现参考）
