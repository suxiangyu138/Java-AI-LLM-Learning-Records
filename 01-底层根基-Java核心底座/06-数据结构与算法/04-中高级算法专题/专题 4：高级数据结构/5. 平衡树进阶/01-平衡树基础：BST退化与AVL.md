# 01-平衡树基础：BST退化与AVL
> 一句话：BST 有序插入退化为链表——AVL 用平衡因子 + 旋转四型维持高度差 ≤ 1，查询最快但维护贵

## 📚 目录
1. [BST 退化：有序插入的灾难](#1-bst-退化有序插入的灾难)
2. [平衡目标与平衡因子](#2-平衡目标与平衡因子)
3. [旋转：LL/RR 单旋](#3-旋转llrr-单旋)
4. [旋转：LR/RL 双旋](#4-旋转lr-双旋)
5. [AVL 完整实现（插入 + 平衡）](#5-avl-完整实现插入--平衡)
6. [识别信号与易错点](#6-识别信号与易错点)

---

## 1. BST 退化：有序插入的灾难

```text
BST 性质: 左 < 根 < 右——查找 O(log n) 的前提是"平衡"

退化: 有序插入 1, 2, 3, 4, 5——
  1 → 2 挂右 → 3 挂右 → … → 单链表！
  查找 5: 从根一路走到尾 → O(n)（失去 BST 意义）

⚠️ 为什么退化:
  BST 的插入只比较大小——有序数据每次都挂同一边
  → 树高从 log n 恶化到 n

平衡树: 通过约束"树高"恢复 O(log n)
  AVL: 高度差 ≤ 1（严格）
  红黑树: 最长 ≤ 2× 最短（弱）
  Treap: 随机化期望平衡

⚠️ 面试必答:
  "BST 有序插入退化为链表 O(n)——
   平衡树用旋转/变色/随机化维持树高 O(log n)"
```

> 🎯 **核心认知**："**BST 的退化是'有序插入挂同边'——平衡树的一切设计都在'约束树高'**。理解退化机制，才能理解为什么需要旋转/变色/随机化三套手段。"

## 2. 平衡目标与平衡因子

### 2.1 平衡因子

```text
平衡因子 = 左子树高度 − 右子树高度
AVL 要求: |平衡因子| ≤ 1（任何节点）

  balance(node) = height(left) − height(right) ∈ {−1, 0, 1}

为什么 |h| ≤ 1 够: 高度差 ≤ 1 → 树高 ≈ 1.44·log₂n
  查询最坏 1.44·log n 层 → O(log n) ✓

⚠️ 与红黑树的差异:
  AVL 严格（|h|≤1）→ 树更矮 → 查询更快
  红黑树弱（2 倍）→ 树稍高但增删维护少
```

### 2.2 高度计算

```java
// 节点结构 + 高度维护
class AVLNode {
    int val, height;                             // ① 高度（非平衡因子）
    AVLNode left, right;
    AVLNode(int val) { this.val = val; this.height = 1; }
}

int height(AVLNode node) {
    return node == null ? 0 : node.height;
}
// 更新高度: height = max(左高, 右高) + 1
// 平衡因子: height(left) − height(right)
```

> 🎯 **平衡因子的定位**："**'|平衡因子| ≤ 1'把树高压在 1.44·log n 内——AVL 查询最快的根本**。高度存节点上（插入后更新），平衡因子由两子树高度差算出。"

## 3. 旋转：LL/RR 单旋

### 3.1 失衡四型与单旋

```text
插入后沿路径检查平衡因子——失衡四种:

LL 型（左孩子的左子树过高）: 右单旋
  y(失衡) 的 left = x；x 的 right 过继给 y 的 left

RR 型（右孩子的右子树过高）: 左单旋（对称）

LR 型（左孩子的右子树）: 双旋（先左后右）
RL 型（右孩子的左子树）: 双旋（先右后左）

⚠️ 判定: 沿"插入路径"看两层——
  第一层方向 + 第二层方向 = 型别
```

### 3.2 左旋/右旋实现

```java
/** 右旋（LL 修复）: y 是失衡节点、x = y.left */
AVLNode rotateRight(AVLNode y) {
    AVLNode x = y.left;
    AVLNode t2 = x.right;                        // ① 保存过继子树
    x.right = y;                                 // ② x 变根
    y.left = t2;                                 // ③ 过继
    y.height = Math.max(height(y.left), height(y.right)) + 1;  // ④ 更新高度
    x.height = Math.max(height(x.left), height(x.right)) + 1;
    return x;                                    // ⑤ 新根
}

/** 左旋（RR 修复）: 对称 */
AVLNode rotateLeft(AVLNode y) {
    AVLNode x = y.right;
    AVLNode t2 = x.left;
    x.left = y;
    y.right = t2;
    y.height = Math.max(height(y.left), height(y.right)) + 1;
    x.height = Math.max(height(x.left), height(x.right)) + 1;
    return x;
}
// ⚠️ 旋转保持中序遍历不变——只改形态不改有序性
```

> 🎯 **旋转的本质**："**'旋转 = 换根 + 过继子树'——中序遍历不变、树高下降**。LL 右旋、RR 左旋对称；更新高度从下到上，是旋转后必做的两件事。"

## 4. 旋转：LR/RL 双旋

### 4.1 双旋的原理

```text
LR 型: y 的左孩子 x 的右子树过高——
  单右旋 y 不够（x 的右子树还在中间位置）
  两步: ① 对 x 左旋（变成 LL）② 对 y 右旋

RL 型: 对称——先右旋再左旋

⚠️ 为什么必须双旋:
  单旋只解决"直线型"（LL/RR）
  LR/RL 是"折线型"——先转直再转正
```

### 4.2 实现

```java
/** LR 型: 先左旋 x、再右旋 y */
if (balance > 1 && balanceFactor(node.left) < 0) {   // ① LR 判定
    node.left = rotateLeft(node.left);               // ② 先左旋（变 LL）
    return rotateRight(node);                        // ③ 再右旋
}

/** RL 型: 先右旋 x、再左旋 y */
if (balance < -1 && balanceFactor(node.right) > 0) { // ④ RL 判定
    node.right = rotateRight(node.right);            // ⑤ 先右旋（变 RR）
    return rotateLeft(node);                         // ⑥ 再左旋
}
// ⚠️ 判定组合:
//   LL: balance > 1 && 左孩子平衡因子 ≥ 0 → 单右旋
//   LR: balance > 1 && 左孩子平衡因子 < 0 → 双旋
//   RR: balance < −1 && 右孩子平衡因子 ≤ 0 → 单左旋
//   RL: balance < −1 && 右孩子平衡因子 > 0 → 双旋
```

> 🎯 **四型的判定**："**'看两层方向定型别、直线单旋、折线双旋'——LL/RR 单旋、LR/RL 双旋（先转直再转正）**。判定组合四行 if 是 AVL 插入的全部逻辑。"

## 5. AVL 完整实现（插入 + 平衡）

### 5.1 插入并保持平衡

```java
/**
 * AVL 插入（递归 + 回溯平衡）
 * 时间 O(log n)，空间 O(log n) 栈
 */
AVLNode insert(AVLNode node, int val) {
    if (node == null) return new AVLNode(val);   // ① 插入叶子

    if (val < node.val) node.left = insert(node.left, val);
    else if (val > node.val) node.right = insert(node.right, val);
    else return node;                            // ② 重复值忽略

    node.height = 1 + Math.max(height(node.left), height(node.right));  // ③ 更新高度

    int balance = height(node.left) - height(node.right);  // ④ 平衡因子

    // ⑤ 四型修复（单旋/双旋）
    if (balance > 1 && val < node.left.val) return rotateRight(node);          // LL
    if (balance < -1 && val > node.right.val) return rotateLeft(node);         // RR
    if (balance > 1 && val > node.left.val) {                                   // LR
        node.left = rotateLeft(node.left);
        return rotateRight(node);
    }
    if (balance < -1 && val < node.right.val) {                                 // RL
        node.right = rotateRight(node.right);
        return rotateLeft(node);
    }
    return node;                               // ⑥ 未失衡 → 原样返回
}
// 推演 插入 1,2,3：
//   1 → 2（右）→ 3（右）: 节点 1 失衡（balance −2）→ RR 左旋
//   旋转后: 2 为根、1 左 3 右 → 树高 2 ✓（未退化）
// ⚠️ 四型判定用"新值方向"（val 与孩子比较）——
//   插入路径决定了失衡型别
```

### 5.2 删除（同思路，旋转修复）

```java
// 删除: BST 删除（找后继）+ 回溯平衡（同插入的旋转四型）
// ⚠️ 删除比插入多一种情形: 平衡因子为 0 时——
//   四型判定需要"≤/≥"（0 按单旋处理）
// 面试定位: 插入会写即可；删除讲思路（BST 删除 + 旋转修复）
```

> 🎯 **AVL 的面试定位**："**'插入 = BST 插入 + 回溯四型旋转'——会插入就懂 AVL 核心**。面试考 AVL 通常到'讲旋转四型'；完整手撕是竞赛要求。"

## 6. 识别信号与易错点

### 6.1 识别信号

```text
"需要严格 O(log n) 的动态集合" → 平衡树（AVL/红黑/Treap）
"面试考旋转/平衡因子" → AVL
"读多写少 + 查询优先" → AVL（对比红黑树）

⚠️ 面试定位: AVL 以"原理 + 旋转"为主——
  手撕概率低于 Treap（更简单）、红黑树（讲性质）
```

### 6.2 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 高度忘更新 | 旋转后更新两节点 | 平衡因子错 |
| 2 | 四型判定漏 | 全部四型覆盖 | 失衡未修复 |
| 3 | 双旋顺序反 | LR 先左后右 | 未修复 |
| 4 | 重复值处理 | 直接返回 | 死递归 |
| 5 | height(null) 忘 0 | 空节点高度 0 | 空指针 |
| 6 | 删除后忘平衡 | 回溯旋转 | 失衡残留 |
| 7 | 旋转丢子树 | t2 过继先保存 | 子树丢失 |

> 🎯 **核心要点**：AVL 通关四件事——**① 退化机制**（有序插入挂同边）；**② 平衡因子**（|h|≤1 → 1.44·log n）；**③ 旋转四型**（直线单旋/折线双旋）；**④ 插入流程**（BST 插入 + 回溯修复）。**AVL = '严格平衡的 BST'——'旋转保序、双旋转直'八个字，就是它的全部机制**。

---

**下一篇**：[02-红黑树与工程应用](02-红黑树与工程应用.md)
**返回总览**：[00-平衡树知识体系总览](00-平衡树知识体系总览.md)
