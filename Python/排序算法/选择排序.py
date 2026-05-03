def selection_sort(arr):
    n = len(arr)
    for i in range(n):
        # 寻找最小值索引
        min_idx = i
        for j in range(i+1, n):
            if arr[j] < arr[min_idx]:
                min_idx = j
        # 交换最小值到已排序位置
        arr[i], arr[min_idx] = arr[min_idx], arr[i]
    return arr

test_arr = [64, 34, 25, 12, 22, 11, 90]
print("选择排序:", selection_sort(test_arr.copy()))