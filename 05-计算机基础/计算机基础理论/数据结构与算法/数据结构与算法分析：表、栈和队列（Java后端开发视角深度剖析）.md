数据结构与算法分析：表、栈和队列（Java后端开发视角深度剖析）
一、表（线性表）
1. 定义
    线性表是n个数据元素的有限序列，元素间为一对一关系
2. 分类
    顺序表（数组实现）：连续内存，随机访问O(1)
    链表（指针实现）：离散内存，插入删除O(1)
3. Java实现
    ArrayList：动态扩容顺序表，默认容量10，扩容1.5倍
    LinkedList：双向链表，实现List与Deque接口
    Vector：线程安全顺序表，性能低，已淘汰
4. 适用场景
    ArrayList：频繁随机访问、少量插入删除
    LinkedList：频繁头尾操作、大量插入删除
    二、栈（Stack）
    1. 定义
    后进先出（LIFO）线性表，仅在栈顶操作
2. 核心操作
    push：入栈O(1)
    pop：出栈O(1)
    peek：查看栈顶O(1)
3. Java实现
    Stack类：继承Vector，线程安全但性能差
    Deque接口：ArrayDeque（推荐）、LinkedList
    示例：Deque stack = new ArrayDeque<>();
4. 后端应用
    函数调用栈：方法递归、JVM栈帧
    表达式求值：计算器、语法解析
    括号匹配：代码校验、HTML标签检查
    单调栈：解决Next Greater Element问题
    三、队列（Queue）
    1. 定义
    先进先出（FIFO）线性表，队尾入队、队头出队
2. 核心操作
    offer：入队O(1)
    poll：出队O(1)
    peek：查看队头O(1)
3. Java实现
    ArrayDeque：循环数组实现，性能最优
    LinkedList：链表实现，支持双端操作
    PriorityQueue：优先队列（堆实现）
    ConcurrentLinkedQueue：无锁并发队列
4. 后端应用
    任务队列：线程池、消息队列
    广度优先搜索（BFS）：二叉树层序遍历、最短路径
    阻塞队列：生产者-消费者模型（ArrayBlockingQueue）
    双端队列：滑动窗口、缓存设计
    四、性能对比
    1. 随机访问
    ArrayList O(1) > LinkedList O(n)
2. 插入删除
    头部：LinkedList O(1) > ArrayList O(n)
    中间：均为O(n)
    尾部：ArrayList O(1)（均摊）、LinkedList O(1)
3. 内存占用
    ArrayList：连续内存，紧凑高效
    LinkedList：节点含前后指针，内存开销大
    五、Java后端实战场景
    1. 数据存储
    列表数据用ArrayList，频繁修改用LinkedList
2. 算法实现
    栈：括号匹配、逆波兰表达式
    队列：BFS、任务调度
3. 并发场景
    线程安全队列：ConcurrentLinkedQueue
    阻塞队列：LinkedBlockingQueue（线程池核心）
    六、复杂度总结
    1. 表
    访问：ArrayList O(1)，LinkedList O(n)
    插入/删除：ArrayList O(n)，LinkedList O(1)
2. 栈/队列
    基本操作均为O(1)
    七、最佳实践
    1. 列表优先选ArrayList，尾部操作高效
    2. 栈/队列优先用ArrayDeque，性能优于Stack/LinkedList
    3. 并发场景用对应并发容器，避免手动同步
    4. 根据操作类型选择数据结构，优化系统性能
