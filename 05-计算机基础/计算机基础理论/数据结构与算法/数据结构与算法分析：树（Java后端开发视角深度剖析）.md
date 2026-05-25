数据结构与算法分析：树（Java后端开发视角深度剖析）
一、树的基础概念
1. 定义
    树是n(n≥0)个节点的有限集合
    n=0为空树，n>0时存在唯一根节点
    子树互不相交，层级结构
2. 核心术语
    根节点：顶层节点
    父节点/子节点：直接关联节点
    叶子节点：无子女节点
    深度：根到节点的路径长度
    高度：节点到叶子的最长路径
    度：节点的子节点数量
3. 存储结构
    顺序存储：数组（完全二叉树）
    链式存储：左右孩子指针（通用）
    二、二叉树（核心）
    1. 定义
    每个节点最多两个子节点（左、右）
2. 特殊二叉树
    满二叉树：每层节点全满
    完全二叉树：除最后一层外全满，最后一层靠左
    平衡二叉树：左右子树高度差≤1
    二叉搜索树(BST)：左<根<右
3. 遍历方式（高频）
    前序：根→左→右
    中序：左→根→右（BST可排序）
    后序：左→右→根
    层序：按层级遍历（队列实现）
    三、Java实现（二叉树）
    class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    TreeNode(int val) {
    this.val = val;
    }
    }
    // 前序遍历
    public void preOrder(TreeNode root) {
    if (root == null) return;
    System.out.print(root.val + " ");
    preOrder(root.left);
    preOrder(root.right);
    }
    // 层序遍历
    public void levelOrder(TreeNode root) {
    if (root == null) return;
    Queue queue = new LinkedList<>();
    queue.offer(root);
    while (!queue.isEmpty()) {
    TreeNode node = queue.poll();
    System.out.print(node.val + " ");
    if (node.left != null) queue.offer(node.left);
    if (node.right != null) queue.offer(node.right);
    }
    }
    四、高级树结构（Java后端重点）
    1. 二叉搜索树(BST)
    特性：左<根<右，中序有序
    操作：插入、查找、删除（O(h)）
    缺点：退化为链表（O(n)）
2. 平衡二叉树(AVL)
    特性：左右高度差≤1，自动旋转平衡
    操作：旋转（左旋、右旋）维持平衡
    复杂度：O(logn)
3. 红黑树（Java核心）
    特性：自平衡二叉搜索树，5条规则
    应用：TreeMap、TreeSet、HashMap（JDK8）
    优势：插入删除高效，避免极端情况
4. B树/B+树（数据库核心）
    B树：多路平衡搜索树，减少IO
    B+树：叶子节点链表，范围查询高效
    应用：MySQL InnoDB索引
    五、Java后端实战场景
    1. 数据存储
    TreeMap/TreeSet：有序存储
    HashMap：红黑树优化冲突
2. 数据库索引
    InnoDB：B+树索引
    MyISAM：B树索引
3. 算法应用
    堆排序（完全二叉树）
    哈夫曼编码（最优二叉树）
    最近公共祖先(LCA)
    六、高频算法题（LeetCode）
    1. 二叉树遍历（前/中/后/层序）
    2. 二叉树的最大深度
    3. 对称二叉树
    4. 二叉搜索树的验证
    5. 路径总和
    6. 重建二叉树（前+中序）
    七、复杂度分析
    1. 二叉树遍历：O(n)
    2. BST操作：O(h)（h为高度）
    3. 平衡树/红黑树：O(logn)
    4. B+树查询：O(logn)（磁盘IO少）
    八、总结
    树是层级数据结构核心，二叉树为基础
    红黑树、B+树是Java后端关键技术
    应用于集合、数据库、算法优化
    掌握树结构提升后端性能与面试能力
