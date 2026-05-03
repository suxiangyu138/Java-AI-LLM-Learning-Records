def bucket_sort(arr):
    if not arr:
        return arr
    # 创建桶
    bucket_num = max(arr) // 10 + 1
    buckets = [[] for _ in range(bucket_num)]
    # 元素入桶
    for num in arr:
        buckets[num // 10].append(num)
    # 桶内排序+合并
    sorted_arr = []
    for bucket in buckets:
        sorted_arr.extend(sorted(bucket))
    return sorted_arr

bucket_arr = [12, 39, 4, 22, 5, 1, 50]
print("桶排序:", bucket_sort(bucket_arr.copy()))