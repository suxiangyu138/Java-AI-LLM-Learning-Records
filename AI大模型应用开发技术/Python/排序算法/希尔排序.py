def shell_sort(arr):
    n = len(arr)
    gap = n // 2  # 初始步长
    while gap > 0:
        # 按步长分组，进行插入排序
        for i in range(gap, n):
            temp = arr[i]
            j = i
            while j >= gap and arr[j - gap] > temp:
                arr[j] = arr[j - gap]
                j -= gap
            arr[j] = temp
        gap //= 2  # 缩小步长
    return arr

test_arr = [64, 34, 25, 12, 22, 11, 90]
print("希尔排序:", shell_sort(test_arr.copy()))