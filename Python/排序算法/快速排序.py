def quick_sort(arr):
    if len(arr) <= 1:
        return arr
    pivot = arr[len(arr) // 2]  # 选中间元素为基准
    left = [x for x in arr if x < pivot]
    middle = [x for x in arr if x == pivot]
    right = [x for x in arr if x > pivot]
    # 递归拼接
    return quick_sort(left) + middle + quick_sort(right)

test_arr = [64, 34, 25, 12, 22, 11, 90]
print("快速排序:", quick_sort(test_arr.copy()))