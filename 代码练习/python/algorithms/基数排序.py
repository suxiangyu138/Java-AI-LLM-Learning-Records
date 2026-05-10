def radix_sort(arr):
    if not arr:
        return arr
    max_num = max(arr)
    digit = 1  # 从个位开始
    # 按每一位排序
    while max_num // digit > 0:
        buckets = [[] for _ in range(10)]
        for num in arr:
            buckets[num // digit % 10].append(num)
        # 合并桶
        arr = []
        for bucket in buckets:
            arr.extend(bucket)
        digit *= 10
    return arr

radix_arr = [170, 45, 75, 90, 802, 24, 2, 66]
print("基数排序:", radix_sort(radix_arr.copy()))