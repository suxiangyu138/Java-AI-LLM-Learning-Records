03.24 19:45
从Java后端开发角度深度剖析《算法导论》：红黑树
一、红黑树核心定义（后端视角）
红黑树是一种自平衡二叉搜索树，通过颜色规则与旋转操作维持黑色高度平衡
保证最坏 O(logn) 时间复杂度，是 Java TreeMap/TreeSet、HashMap 链表转树的底层实现
是后端有序存储、范围查询、动态排序的核心数据结构
二、红黑树五大性质（算法导论）
1. 每个节点非红即黑
2. 根节点为黑色
3. 所有叶子节点（空节点）为黑色
4. 如果节点为红色，则其子节点均为黑色
5. 从任意节点到其每个叶子节点的路径包含相同数量的黑色节点（黑色高度平衡）
三、红黑树核心操作（Java 实现）
java
class RBNode {
    int val;
    RBNode left, right, parent;
    boolean color; // true 红, false 黑
    RBNode(int val) {
        this.val = val;
        this.color = true; // 新节点默认红色
    }
}
public class RedBlackTree {
    private static final boolean RED = true;
    private static final boolean BLACK = false;
    private RBNode root;
    // 左旋
    private void leftRotate(RBNode x) {
        RBNode y = x.right;
        x.right = y.left;
        if (y.left != null) y.left.parent = x;
        y.parent = x.parent;
        if (x.parent == null) root = y;
        else if (x == x.parent.left) x.parent.left = y;
        else x.parent.right = y;
        y.left = x;
        x.parent = y;
    }
    // 右旋
    private void rightRotate(RBNode y) {
        RBNode x = y.left;
        y.left = x.right;
        if (x.right != null) x.right.parent = y;
        x.parent = y.parent;
        if (y.parent == null) root = x;
        else if (y == y.parent.right) y.parent.right = x;
        else y.parent.left = x;
        x.right = y;
        y.parent = x;
    }
    // 插入后修复
    private void insertFixup(RBNode z) {
        while (z.parent != null && z.parent.color == RED) {
            if (z.parent == z.parent.parent.left) {
                RBNode y = z.parent.parent.right;
                if (y != null && y.color == RED) {
                    z.parent.color = BLACK;
                    y.color = BLACK;
                    z.parent.parent.color = RED;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.right) {
                        z = z.parent;
                        leftRotate(z);
                    }
                    z.parent.color = BLACK;
                    z.parent.parent.color = RED;
                    rightRotate(z.parent.parent);
                }
            } else {
                RBNode y = z.parent.parent.left;
                if (y != null && y.color == RED) {
                    z.parent.color = BLACK;
                    y.color = BLACK;
                    z.parent.parent.color = RED;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.left) {
                        z = z.parent;
                        rightRotate(z);
                    }
                    z.parent.color = BLACK;
                    z.parent.parent.color = RED;
                    leftRotate(z.parent.parent);
                }
            }
        }
        root.color = BLACK;
    }
    // 插入
    public void insert(int val) {
        RBNode z = new RBNode(val);
        RBNode y = null;
        RBNode x = root;
        while (x != null) {
            y = x;
            if (z.val < x.val) x = x.left;
            else x = x.right;
        }
        z.parent = y;
        if (y == null) root = z;
        else if (z.val < y.val) y.left = z;
        else y.right = z;
        insertFixup(z);
    }
}
 
四、红黑树复杂度分析
- 插入/删除/查找：最坏 O(logn)
- 空间复杂度：O(n)
- 旋转次数：插入最多 2 次，删除最多 3 次
五、Java 后端核心应用场景
1. TreeMap / TreeSet
- 底层红黑树实现
- 有序键值对、范围查询、subMap、headMap、tailMap
- 应用：排行榜、时间序列、区间统计
2. HashMap（JDK 1.8+）
- 链表长度 ≥ 8 转为红黑树
- 避免哈希冲突退化为 O(n)
- 保证稳定 O(logn) 查找
3. 并发有序结构
- ConcurrentSkipListMap（跳表）
- 高并发下替代 TreeMap
- 应用：分布式有序存储
六、红黑树 vs AVL 树（后端选型）
- AVL：高度差 ≤ 1，旋转频繁，查询略快
- 红黑树：黑色高度平衡，旋转少，插入删除更高效
- 后端工程优先红黑树（TreeMap 选择）
七、红黑树 vs 跳表（后端选型）
- 红黑树：内存紧凑，范围查询高效
- 跳表：实现简单，并发友好
- 高并发选跳表（Redis、ConcurrentSkipListMap）
- 单机有序选红黑树
八、总结（后端红黑树核心要点）
1. 红黑树是自平衡 BST，保证最坏 O(logn)
2. 五大性质 + 旋转 + 变色维持平衡
3. Java 核心应用：TreeMap、HashMap 树化
4. 适合有序存储、范围查询、动态排序
5. 是后端高级数据结构的基础
suxiangyu

