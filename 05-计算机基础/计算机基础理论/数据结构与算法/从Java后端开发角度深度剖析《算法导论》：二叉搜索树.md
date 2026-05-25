从Java后端开发角度深度剖析《算法导论》：二叉搜索树
一、二叉搜索树（BST）核心定义（后端视角）
二叉搜索树是满足左小右大规则的二叉树，中序遍历可得到有序序列
平均时间复杂度O(logn)，最坏退化为链表O(n)
在Java后端中，是TreeMap/TreeSet底层结构，用于有序存储、范围查询、动态排序
二、BST核心性质（算法导论）
1. 左子树所有节点值 < 根节点值
2. 右子树所有节点值 > 根节点值
3. 左右子树均为二叉搜索树
4. 中序遍历结果严格递增
    三、BST基本操作（Java实现）
    java
    class TreeNode {
    int val;
    TreeNode left, right;
    TreeNode(int val) { this.val = val; }
    }
    public class BST {
    private TreeNode root;
    // 插入
    public void insert(int val) {
        root = insert(root, val);
    }
    private TreeNode insert(TreeNode node, int val) {
        if (node == null) return new TreeNode(val);
        if (val < node.val) node.left = insert(node.left, val);
        else node.right = insert(node.right, val);
        return node;
    }
    // 查找
    public boolean search(int val) {
        return search(root, val);
    }
    private boolean search(TreeNode node, int val) {
        if (node == null) return false;
        if (val == node.val) return true;
        return val < node.val ? search(node.left, val) : search(node.right, val);
    }
    // 删除
    public void delete(int val) {
        root = delete(root, val);
    }
    private TreeNode delete(TreeNode node, int val) {
        if (node == null) return null;
        if (val < node.val) node.left = delete(node.left, val);
        else if (val > node.val) node.right = delete(node.right, val);
        else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            TreeNode min = findMin(node.right);
            node.val = min.val;
            node.right = delete(node.right, min.val);
        }
        return node;
    }
    private TreeNode findMin(TreeNode node) {
        while (node.left != null) node = node.left;
        return node;
    }
    }
 
四、BST复杂度分析
- 平均：插入/查找/删除 O(logn)
- 最坏：退化为链表 O(n)
- 空间：O(n)
    五、平衡二叉树（后端工程必备）
    普通BST易退化，工程使用自平衡树
    1. AVL树：高度差≤1，旋转平衡
    2. 红黑树：JDK TreeMap/TreeSet底层，黑色高度平衡，性能稳定
    六、Java后端核心应用
    1. TreeMap/TreeSet：有序键值对，范围查询，O(logn)
    2. 排行榜：动态维护有序序列
    3. 时间序列数据：按时间范围快速检索
    4. 区间统计：范围查询、最值查询
    七、BST vs 散列表（后端选型）
- BST：有序、范围查询、O(logn)
- 散列表：无序、O(1)查找、无范围查询
- 有序场景选BST，快速查找选散列表
    八、总结（后端BST核心要点）
    1. BST左小右大，中序递增，平均O(logn)
    2. 工程使用红黑树（TreeMap）避免退化
    3. 核心应用：有序存储、范围查询、排行榜
    4. 是Java后端有序数据结构的基础
    suxiangyu
