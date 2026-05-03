import java.util.Arrays;
import java.util.Random;

/**
 * Java 经典排序算法综合体验
 * 包含：冒泡/选择/插入/希尔/归并/快速/堆/基数排序
 * 附带：性能对比、复杂度分析、稳定性说明
 */
public class SortAlgorithmsExperience {
    // 生成随机数组（用于性能测试）
    private static int[] generateRandomArray(int size, int bound) {
        Random random = new Random();
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = random.nextInt(bound);
        }
        return arr;
    }

    // 数组拷贝（避免排序修改原数组）
    private static int[] copyArray(int[] arr) {
        return Arrays.copyOf(arr, arr.length);
    }

    // 打印数组（前20个元素，避免过长）
    private static void printArray(int[] arr) {
        int len = Math.min(arr.length, 20);
        System.out.print("[");
        for (int i = 0; i < len; i++) {
            System.out.print(arr[i]);
            if (i < len - 1) System.out.print(", ");
        }
        if (arr.length > 20) System.out.print("...");
        System.out.println("]");
    }

    // ====================== 1. 冒泡排序（Bubble Sort） ======================
    /**
     * 核心逻辑：相邻元素比较交换，每轮将最大元素"冒泡"到末尾
     * 时间复杂度：O(n²)  空间复杂度：O(1)  稳定性：稳定
     */
    public static void bubbleSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        int n = arr.length;
        // 外层循环：控制排序轮数
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false; // 优化：标记是否发生交换，无交换则提前退出
            // 内层循环：每轮比较到未排序的最后一个元素
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    // 交换相邻元素
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }
            if (!swapped) break; // 无交换，说明已排序完成
        }
    }

    // ====================== 2. 选择排序（Selection Sort） ======================
    /**
     * 核心逻辑：每轮选择未排序区间的最小元素，放到已排序区间末尾
     * 时间复杂度：O(n²)  空间复杂度：O(1)  稳定性：不稳定（如 [2, 2, 1]）
     */
    public static void selectionSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        int n = arr.length;
        // 外层循环：已排序区间[0, i)，未排序区间[i, n)
        for (int i = 0; i < n - 1; i++) {
            int minIndex = i; // 最小元素索引
            // 内层循环：找未排序区间的最小元素
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIndex]) {
                    minIndex = j;
                }
            }
            // 交换最小元素到已排序区间末尾
            if (minIndex != i) {
                int temp = arr[i];
                arr[i] = arr[minIndex];
                arr[minIndex] = temp;
            }
        }
    }

    // ====================== 3. 插入排序（Insertion Sort） ======================
    /**
     * 核心逻辑：将未排序元素插入到已排序区间的合适位置
     * 时间复杂度：O(n²)  空间复杂度：O(1)  稳定性：稳定
     * 优势：近乎有序的数组排序效率高（接近O(n)）
     */
    public static void insertionSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        int n = arr.length;
        // 外层循环：未排序元素从第2个开始
        for (int i = 1; i < n; i++) {
            int temp = arr[i]; // 待插入元素
            int j = i - 1;     // 已排序区间末尾索引
            // 内层循环：找到插入位置（已排序元素大于temp则后移）
            while (j >= 0 && arr[j] > temp) {
                arr[j + 1] = arr[j];
                j--;
            }
            // 插入元素
            arr[j + 1] = temp;
        }
    }

    // ====================== 4. 希尔排序（Shell Sort） ======================
    /**
     * 核心逻辑：插入排序的优化版，按步长分组排序，逐步缩小步长到1
     * 时间复杂度：O(n^1.3)  空间复杂度：O(1)  稳定性：不稳定
     */
    public static void shellSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        int n = arr.length;
        // 初始步长为n/2，逐步缩小为1
        for (int gap = n / 2; gap > 0; gap /= 2) {
            // 对每个步长组进行插入排序
            for (int i = gap; i < n; i++) {
                int temp = arr[i];
                int j = i;
                // 组内插入排序
                while (j >= gap && arr[j - gap] > temp) {
                    arr[j] = arr[j - gap];
                    j -= gap;
                }
                arr[j] = temp;
            }
        }
    }

    // ====================== 5. 归并排序（Merge Sort） ======================
    /**
     * 核心逻辑：分治思想，将数组拆分为子数组排序，再合并有序子数组
     * 时间复杂度：O(nlogn)  空间复杂度：O(n)  稳定性：稳定
     */
    public static void mergeSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        mergeSort(arr, 0, arr.length - 1);
    }

    // 递归拆分
    private static void mergeSort(int[] arr, int left, int right) {
        if (left >= right) return;
        int mid = left + (right - left) / 2; // 避免溢出
        mergeSort(arr, left, mid);          // 左半部分排序
        mergeSort(arr, mid + 1, right);     // 右半部分排序
        merge(arr, left, mid, right);       // 合并有序子数组
    }

    // 合并两个有序子数组
    private static void merge(int[] arr, int left, int mid, int right) {
        int[] temp = new int[right - left + 1]; // 临时数组
        int i = left, j = mid + 1, k = 0;

        // 合并两个有序数组到临时数组
        while (i <= mid && j <= right) {
            temp[k++] = arr[i] <= arr[j] ? arr[i++] : arr[j++];
        }

        // 拷贝剩余元素
        while (i <= mid) temp[k++] = arr[i++];
        while (j <= right) temp[k++] = arr[j++];

        // 临时数组拷贝回原数组
        System.arraycopy(temp, 0, arr, left, temp.length);
    }

    // ====================== 6. 快速排序（Quick Sort） ======================
    /**
     * 核心逻辑：分治思想，选基准元素，将数组分为小于/大于基准的两部分
     * 时间复杂度：O(nlogn)  空间复杂度：O(logn)（递归栈）  稳定性：不稳定
     * 优势：实际应用中最快的排序算法（原地排序，缓存友好）
     */
    public static void quickSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        quickSort(arr, 0, arr.length - 1);
    }

    // 递归排序
    private static void quickSort(int[] arr, int left, int right) {
        if (left >= right) return;
        int pivotIndex = partition(arr, left, right); // 分区，返回基准元素索引
        quickSort(arr, left, pivotIndex - 1);        // 左半部分排序
        quickSort(arr, pivotIndex + 1, right);       // 右半部分排序
    }

    // 分区操作（选最右元素为基准）
    private static int partition(int[] arr, int left, int right) {
        int pivot = arr[right]; // 基准元素
        int i = left - 1;       // 小于基准的区域末尾索引

        for (int j = left; j < right; j++) {
            if (arr[j] <= pivot) {
                i++;
                // 交换元素到小于基准的区域
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }

        // 基准元素放到正确位置（i+1）
        int temp = arr[i + 1];
        arr[i + 1] = arr[right];
        arr[right] = temp;

        return i + 1;
    }

    // ====================== 7. 堆排序（Heap Sort） ======================
    /**
     * 核心逻辑：构建大顶堆，每次取出堆顶元素放到数组末尾，调整堆结构
     * 时间复杂度：O(nlogn)  空间复杂度：O(1)  稳定性：不稳定
     */
    public static void heapSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        int n = arr.length;

        // 1. 构建大顶堆（从最后一个非叶子节点开始调整）
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }

        // 2. 逐个取出堆顶元素
        for (int i = n - 1; i > 0; i--) {
            // 堆顶（最大元素）交换到数组末尾
            int temp = arr[0];
            arr[0] = arr[i];
            arr[i] = temp;

            // 调整剩余元素为大顶堆
            heapify(arr, i, 0);
        }
    }

    // 堆调整（维护大顶堆性质）
    private static void heapify(int[] arr, int heapSize, int root) {
        int largest = root;    // 最大元素索引（初始为根节点）
        int left = 2 * root + 1; // 左子节点
        int right = 2 * root + 2; // 右子节点

        // 左子节点大于根节点
        if (left < heapSize && arr[left] > arr[largest]) {
            largest = left;
        }

        // 右子节点大于最大元素
        if (right < heapSize && arr[right] > arr[largest]) {
            largest = right;
        }

        // 最大元素不是根节点，交换并递归调整
        if (largest != root) {
            int temp = arr[root];
            arr[root] = arr[largest];
            arr[largest] = temp;
            heapify(arr, heapSize, largest);
        }
    }

    // ====================== 8. 基数排序（Radix Sort） ======================
    /**
     * 核心逻辑：按数字的每一位排序（从低位到高位），基于桶排序实现
     * 时间复杂度：O(d*(n+k))  空间复杂度：O(n+k)  稳定性：稳定
     * 适用：非负整数排序（可扩展到负数/小数）
     */
    public static void radixSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;

        // 1. 找到最大值，确定最大位数
        int max = Arrays.stream(arr).max().getAsInt();
        // 2. 按每一位排序（个位、十位、百位...）
        for (int exp = 1; max / exp > 0; exp *= 10) {
            countingSortByDigit(arr, exp);
        }
    }

    // 按指定位数进行计数排序
    private static void countingSortByDigit(int[] arr, int exp) {
        int n = arr.length;
        int[] output = new int[n]; // 输出数组
        int[] count = new int[10]; // 0-9的计数桶

        // 1. 统计每个位数的出现次数
        for (int i = 0; i < n; i++) {
            int digit = (arr[i] / exp) % 10;
            count[digit]++;
        }

        // 2. 计算前缀和，确定元素位置
        for (int i = 1; i < 10; i++) {
            count[i] += count[i - 1];
        }

        // 3. 构建输出数组（从后往前，保证稳定性）
        for (int i = n - 1; i >= 0; i--) {
            int digit = (arr[i] / exp) % 10;
            output[count[digit] - 1] = arr[i];
            count[digit]--;
        }

        // 4. 拷贝回原数组
        System.arraycopy(output, 0, arr, 0, n);
    }

    // ====================== 测试主方法 ======================
    public static void main(String[] args) {
        // 1. 小数据测试（验证算法正确性）
        int[] testArr = {5, 2, 9, 3, 7, 6, 1, 8, 4};
        System.out.println("===== 排序算法正确性测试 =====");
        System.out.println("原数组：");
        printArray(testArr);

        // 冒泡排序
        int[] bubbleArr = copyArray(testArr);
        bubbleSort(bubbleArr);
        System.out.println("冒泡排序后：");
        printArray(bubbleArr);

        // 快速排序
        int[] quickArr = copyArray(testArr);
        quickSort(quickArr);
        System.out.println("快速排序后：");
        printArray(quickArr);

        // 2. 大数据性能测试
        int[] sizes = {10000, 100000}; // 测试数据量
        int bound = 1000000;           // 随机数范围
        System.out.println("\n===== 排序算法性能测试 =====");

        for (int size : sizes) {
            System.out.println("\n测试数据量：" + size + " 个元素");
            int[] randomArr = generateRandomArray(size, bound);

            // 冒泡排序（数据量大时跳过，太慢）
            if (size <= 10000) {
                int[] bubbleTest = copyArray(randomArr);
                long start = System.currentTimeMillis();
                bubbleSort(bubbleTest);
                long time = System.currentTimeMillis() - start;
                System.out.println("冒泡排序耗时：" + time + "ms");
            } else {
                System.out.println("冒泡排序：数据量过大，跳过");
            }

            // 选择排序
            int[] selectionTest = copyArray(randomArr);
            long start = System.currentTimeMillis();
            selectionSort(selectionTest);
            long time = System.currentTimeMillis() - start;
            System.out.println("选择排序耗时：" + time + "ms");

            // 插入排序
            int[] insertionTest = copyArray(randomArr);
            start = System.currentTimeMillis();
            insertionSort(insertionTest);
            time = System.currentTimeMillis() - start;
            System.out.println("插入排序耗时：" + time + "ms");

            // 希尔排序
            int[] shellTest = copyArray(randomArr);
            start = System.currentTimeMillis();
            shellSort(shellTest);
            time = System.currentTimeMillis() - start;
            System.out.println("希尔排序耗时：" + time + "ms");

            // 归并排序
            int[] mergeTest = copyArray(randomArr);
            start = System.currentTimeMillis();
            mergeSort(mergeTest);
            time = System.currentTimeMillis() - start;
            System.out.println("归并排序耗时：" + time + "ms");

            // 快速排序
            int[] quickTest = copyArray(randomArr);
            start = System.currentTimeMillis();
            quickSort(quickTest);
            time = System.currentTimeMillis() - start;
            System.out.println("快速排序耗时：" + time + "ms");

            // 堆排序
            int[] heapTest = copyArray(randomArr);
            start = System.currentTimeMillis();
            heapSort(heapTest);
            time = System.currentTimeMillis() - start;
            System.out.println("堆排序耗时：" + time + "ms");

            // 基数排序
            int[] radixTest = copyArray(randomArr);
            start = System.currentTimeMillis();
            radixSort(radixTest);
            time = System.currentTimeMillis() - start;
            System.out.println("基数排序耗时：" + time + "ms");
        }

        // 3. 核心特性总结
        System.out.println("\n===== 排序算法核心特性总结 =====");
        System.out.println("1. 冒泡排序：O(n²) | O(1) | 稳定 | 适合小数据/近乎有序数据");
        System.out.println("2. 选择排序：O(n²) | O(1) | 不稳定 | 交换次数少");
        System.out.println("3. 插入排序：O(n²) | O(1) | 稳定 | 近乎有序数据效率高");
        System.out.println("4. 希尔排序：O(n^1.3) | O(1) | 不稳定 | 插入排序优化版");
        System.out.println("5. 归并排序：O(nlogn) | O(n) | 稳定 | 适合大数据/外部排序");
        System.out.println("6. 快速排序：O(nlogn) | O(logn) | 不稳定 | 实际应用最快");
        System.out.println("7. 堆排序：O(nlogn) | O(1) | 不稳定 | 适合内存受限场景");
        System.out.println("8. 基数排序：O(d*(n+k)) | O(n+k) | 稳定 | 非负整数/字符串排序");
    }
}