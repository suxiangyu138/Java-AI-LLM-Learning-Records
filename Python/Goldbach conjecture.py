# 判断一个数是否是素数
def is_prime(n):
    # 小于2的数不是素数
    if n < 2:
        return False
    # 优化：只需判断到平方根即可
    for i in range(2, int(n ** 0.5) + 1):
        if n % i == 0:
            return False
    return True

# 分解整数为两个素数之和
def prime_sum(num):
    result = []
    # 遍历所有可能的拆分方式
    for a in range(2, num // 2 + 1):
        b = num - a
        if is_prime(a) and is_prime(b):
            result.append((a, b))
    return result

# 主程序：输入 + 输出
if __name__ == "__main__":
    try:
        # 获取用户输入的整数
        number = int(input("请输入一个整数："))
        
        # 小于4的数无法拆成两个素数之和
        if number < 4:
            print("该数字不能分解为两个素数之和")
        else:
            res = prime_sum(number)
            if not res:
                print("该数字不能分解为两个素数之和")
            else:
                print(f"{number} 可以分解为以下素数之和：")
                for pair in res:
                    print(f"{pair[0]} + {pair[1]} = {number}")
    except ValueError:
        print("请输入有效的整数！")