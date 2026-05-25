从Java后端开发角度深度剖析《算法导论》：堆排序
一、堆排序核心定义（后端视角）
堆排序是基于二叉堆的高效排序算法，属于选择排序类，时间复杂度稳定O(nlogn)，空间复杂度O(1)
在Java后端开发中，堆排序是优先队列、TopK问题、海量数据处理的核心算法，是PriorityQueue底层实现基础
二、堆的核心概念（算法导论）
1. 堆的结构
    - 完全二叉树：除最后一层外，所有层节点填满，最后一层靠左排列
    - 大顶堆：父节点≥子节点，堆顶为最大值
    - 小顶堆：父节点≤子节点，堆顶为最小值
2. 堆的存储
    - 数组存储：下标i节点
    左子节点：2i+1
    右子节点：2i+2
    父节点：(i-1)/2
3. 核心操作
    - 堆化（heapify）：维护堆性质，时间复杂度O(logn)
    - 建堆（build-heap）：将数组转为堆，时间复杂度O(n)
    - 堆排序：循环取堆顶+堆化，O(nlogn)
    三、堆排序完整实现（Java后端标准）
    java
    /**
     * 堆排序（大顶堆实现）
     * 时间复杂度：O(nlogn)
     * 空间复杂度：O(1) 原地排序
     * 稳定性：不稳定
     */
    public class HeapSort {
    /**
     * 堆排序主方法
     */
    public void sort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        int n = arr.length;
        // 1. 建堆：从最后一个非叶子节点向上堆化
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }
        // 2. 排序：交换堆顶与末尾，堆化剩余元素
        for (int i = n - 1; i > 0; i--) {
            // 交换堆顶（最大值）与当前末尾
            swap(arr, 0, i);
            // 堆化剩余i个元素
            heapify(arr, i, 0);
        }
    }
    /**
     * 堆化：维护大顶堆性质
     * @param arr 数组
     * @param len 堆有效长度
     * @param i 当前节点下标
     */
    private void heapify(int[] arr, int len, int i) {
        int largest = i;        // 最大值下标
        int left = 2 * i + 1;   // 左子节点
        int right = 2 * i + 2;  // 右子节点
        // 左子节点更大
        if (left < len && arr[left] > arr[largest]) {
            largest = left;
        }
        // 右子节点更大
        if (right < len && arr[right] > arr[largest]) {
            largest = right;
        }
        // 最大值不是当前节点，交换并递归堆化
        if (largest != i) {
            swap(arr, i, largest);
            heapify(arr, len, largest);
        }
    }
    /**
     * 交换数组元素
     */
    private void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
    }
 
四、堆排序核心步骤（算法导论）
1. 建堆：将数组转为大顶堆，O(n)
2. 交换堆顶与末尾元素，最大值就位
3. 堆化剩余n-1元素，O(logn)
4. 重复2-3，直到数组有序
    五、Java后端核心应用场景
    1. 优先队列（PriorityQueue）
    - JDK PriorityQueue基于小顶堆实现
    - 应用：任务优先级调度、延迟队列、超时处理
    java
    // 小顶堆：默认自然排序
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    // 大顶堆：自定义比较器
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
 
2. TopK问题（高频面试）
    - 场景：求前K大/小元素、热点排行、热门搜索
    - 复杂度：O(nlogk)，远优于全排序O(nlogn)
    java
    /**
     * TopK问题：求数组中第K大元素
     */
    public int findKthLargest(int[] nums, int k) {
    // 小顶堆，保留K个最大元素
    PriorityQueue<Integer> heap = new PriorityQueue<>();
    for (int num : nums) {
        heap.add(num);
        if (heap.size() > k) {
            heap.poll();
        }
    }
    return heap.peek();
    }
 
3. 海量数据排序
    - 内存受限场景：堆排序原地排序，无需额外空间
    - 外部排序优化：多路归并+堆排序
    - 应用：日志排序、数据报表、离线计算
4. 流式数据处理
    - 动态数据排序：实时维护堆结构
    - 应用：实时排行榜、流式TopK、监控指标
    六、堆排序性能分析（算法导论）
    1. 时间复杂度
    - 建堆：O(n)
    - 堆化：O(logn)
    - 总复杂度：O(nlogn)，稳定不退化
    2. 空间复杂度
    - O(1)：原地排序，仅用常数额外空间
    3. 稳定性
    - 不稳定：交换可能改变相同元素相对位置
    七、堆排序与其他排序对比（后端选型）
    1. 快排 vs 堆排
    - 快排：平均O(nlogn)，最坏O(n²)，常数因子小，实际更快
    - 堆排：稳定O(nlogn)，原地排序，适合内存受限场景
    2. 归并 vs 堆排
    - 归并：稳定O(nlogn)，需O(n)空间，适合外部排序
    - 堆排：原地O(nlogn)，不稳定，适合内存紧张场景
    3. 后端选型建议
    - 追求速度：快排（JDK Arrays.sort基础类型）
    - 内存受限：堆排
    - 要求稳定：归并（JDK Arrays.sort对象类型）
    - TopK问题：堆排最优
    八、堆排序优化（后端进阶）
    1. 小顶堆优化TopK
    - 求前K大：小顶堆，O(nlogk)
    - 求前K小：大顶堆，O(nlogk)
    2. 堆化迭代实现
    - 避免递归栈溢出，适合大数据量
    java
    private void heapifyIterative(int[] arr, int len, int i) {
    while (true) {
        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        if (left < len && arr[left] > arr[largest]) largest = left;
        if (right < len && arr[right] > arr[largest]) largest = right;
        if (largest == i) break;
        swap(arr, i, largest);
        i = largest;
    }
    }
 
3. 并行堆排序
    - 多线程建堆+排序，提升大数据处理速度
    - 应用：ForkJoin框架、分布式计算
    九、总结（后端堆排序核心要点）
    1. 堆排序基于二叉堆，时间复杂度稳定O(nlogn)，空间O(1)
    2. 核心操作：建堆O(n)、堆化O(logn)、排序O(nlogn)
    3. Java后端核心应用：PriorityQueue、TopK问题、海量数据排序、流式处理
    4. 性能优于选择排序，适合内存受限场景
    5. 面试高频：堆排序实现、TopK问题、优先队列原理
    6. 与快排/归并互补，是后端必备排序算法
    suxiangyu
