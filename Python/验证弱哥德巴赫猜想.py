def is_prime(n):
    if n < 2:
        return False
    for i in range(2, int(n ** 0.5) + 1):
        if n % i == 0:
            return False
    return True

def split_three_prime(num):
    res = []
    # 遍历第一个素数a
    for a in range(2, num):
        if not is_prime(a):
            continue
        # 遍历第二个素数b
        for b in range(a, num - a):
            c = num - a - b
            if c >= b and is_prime(b) and is_prime(c):
                res.append((a, b, c))
    return res

if __name__ == "__main__":
    try:
        n = int(input("请输入一个大于5的奇数："))
        if n <= 5 or n % 2 == 0:
            print("请输入严格大于5的奇数！")
        else:
            lst = split_three_prime(n)
            print(f"\n{n} 分解为三个素数之和的所有情况：")
            for item in lst:
                print(f"{item[0]} + {item[1]} + {item[2]} = {n}")
    except ValueError:
        print("请输入合法整数！")