def bubble_sort(arr):
    n = len(arr)
    # 外层循环控制排序轮数
    for i in range(n):
        # 内层循环比较相邻元素
        for j in range(0, n-i-1):
            if arr[j] > arr[j+1]:
                # 交换元素
                arr[j], arr[j+1] = arr[j+1], arr[j]
    return arr

# 测试
test_arr = [64, 34, 25, 12, 22, 11, 90]
print("冒泡排序:", bubble_sort(test_arr.copy()))