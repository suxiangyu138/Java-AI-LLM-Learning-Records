def heapify(arr, n, i):
    largest = i  # 初始化根节点为最大值
    left = 2 * i + 1
    right = 2 * i + 2
    
    # 找到左右子节点中的最大值
    if left < n and arr[i] < arr[left]:
        largest = left
    if right < n and arr[largest] < arr[right]:
        largest = right
    
    # 交换并递归调整堆
    if largest != i:
        arr[i], arr[largest] = arr[largest], arr[i]
        heapify(arr, n, largest)

def heap_sort(arr):
    n = len(arr)
    # 构建最大堆
    for i in range(n//2 - 1, -1, -1):
        heapify(arr, n, i)
    # 逐个取出堆顶元素
    for i in range(n-1, 0, -1):
        arr[i], arr[0] = arr[0], arr[i]
        heapify(arr, i, 0)
    return arr
test_arr = [64, 34, 25, 12, 22, 11, 90]
print("堆排序:", heap_sort(test_arr.copy()))