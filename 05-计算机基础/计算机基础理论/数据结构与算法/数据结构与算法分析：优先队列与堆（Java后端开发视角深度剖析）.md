数据结构与算法分析：优先队列与堆（Java后端开发视角深度剖析）
一、优先队列基础
1. 定义
    优先队列是一种特殊队列，元素出队顺序按优先级而非入队顺序
    优先级由比较规则决定（升序/降序、自定义规则）
2. 核心特性
    - 插入：O(logn)
    - 取最值：O(1)
    - 删除最值：O(logn)
3. 底层实现
    - 堆（完全二叉树）：最优实现，时间复杂度稳定
    - 有序数组：插入O(n)，取最值O(1)
    - 无序数组：插入O(1)，取最值O(n)
    二、堆的核心概念
    1. 定义
    堆是完全二叉树，分为大顶堆与小顶堆
    - 大顶堆：父节点≥子节点，堆顶为最大值
    - 小顶堆：父节点≤子节点，堆顶为最小值
    2. 存储结构
    数组顺序存储（完全二叉树特性）
    - 父节点索引：i
    - 左子节点：2i+1
    - 右子节点：2i+2
    3. 核心操作
    - 上浮（siftUp）：插入元素后调整
    - 下沉（siftDown）：删除堆顶后调整
    - 建堆（heapify）：数组转堆，O(n)
    三、Java实现（小顶堆）
    class MinHeap {
    private int[] heap;
    private int size;
    public MinHeap(int capacity) {
    heap = new int[capacity];
    size = 0;
    }
    // 插入元素
    public void offer(int val) {
    heap[size] = val;
    siftUp(size);
    size++;
    }
    // 删除堆顶
    public int poll() {
    int top = heap[0];
    heap[0] = heap[size - 1];
    size--;
    siftDown(0);
    return top;
    }
    // 上浮调整
    private void siftUp(int i) {
    while (i > 0) {
    int parent = (i - 1) / 2;
    if (heap[i] >= heap[parent]) break;
    swap(i, parent);
    i = parent;
    }
    }
    // 下沉调整
    private void siftDown(int i) {
    while (2 * i + 1 < size) {
    int left = 2 * i + 1;
    int right = 2 * i + 2;
    int min = left;
    if (right < size && heap[right] < heap[left]) {
    min = right;
    }
    if (heap[i] <= heap[min]) break;
    swap(i, min);
    i = min;
    }
    }
    private void swap(int i, int j) {
    int temp = heap[i];
    heap[i] = heap[j];
    heap[j] = temp;
    }
    }
    四、Java内置优先队列（PriorityQueue）
    1. 基础特性
    - 默认小顶堆，基于小顶堆实现
    - 初始容量11，自动扩容
    - 非线程安全，并发需PriorityBlockingQueue
    2. 自定义优先级
    // 大顶堆
    PriorityQueue maxHeap = new PriorityQueue<>((a, b) -> b - a);
    // 自定义对象优先级
    class Task {
    int priority;
    String name;
    }
    PriorityQueue taskQueue = new PriorityQueue<>((a, b) -> a.priority - b.priority);
3. 常用方法
    - offer(E e)：插入元素
    - poll()：删除并返回堆顶
    - peek()：获取堆顶不删除
    - size()：获取元素数量
    五、Java后端实战场景
    1. 任务调度
    - 优先级任务队列：高优先级任务优先执行
    - 延迟队列：基于小顶堆实现定时任务
    2. 数据处理
    - TopK问题：小顶堆求前K大元素
    - 合并K个有序链表：小顶堆高效合并
    3. 中间件应用
    - Redis有序集合（ZSet）：skiplist+堆实现
    - 消息队列优先级队列：堆结构保证顺序
4. 算法优化
    - 堆排序：O(nlogn)，不稳定排序
    - 最短路径（Dijkstra）：小顶堆优化时间复杂度
    六、复杂度分析
    1. 插入操作：O(logn)
    2. 删除堆顶：O(logn)
    3. 建堆：O(n)
    4. 堆排序：O(nlogn)
    七、总结
    堆是优先队列最优实现，核心为上浮下沉调整
    Java PriorityQueue基于小顶堆，支持自定义优先级
    后端应用覆盖任务调度、数据处理、中间件、算法优化
    掌握堆结构可高效解决TopK、最短路径等高频问题
