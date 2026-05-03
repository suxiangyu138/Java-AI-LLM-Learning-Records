def insertion_sort(arr):
    n = len(arr)
    # 从第二个元素开始插入
    for i in range(1, n):
        key = arr[i]
        j = i - 1
        # 比key大的元素后移
        while j >= 0 and key < arr[j]:
            arr[j + 1] = arr[j]
            j -= 1
        arr[j + 1] = key
    return arr
test_arr = [64, 34, 25, 12, 22, 11, 90]
print("插入排序:", insertion_sort(test_arr.copy()))