def counting_sort(arr):
    if not arr:
        return arr
    max_val = max(arr)
    min_val = min(arr)
    # 初始化计数数组
    count = [0] * (max_val - min_val + 1)
    # 统计元素次数
    for num in arr:
        count[num - min_val] += 1
    # 重构排序数组
    index = 0
    for i in range(len(count)):
        while count[i] > 0:
            arr[index] = i + min_val
            index += 1
            count[i] -= 1
    return arr

count_arr = [4, 2, 2, 8, 3, 3, 1]
print("计数排序:", counting_sort(count_arr.copy()))