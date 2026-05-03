class BankAccount:
    def __init__(self, name, balance):
        self.name = name
        # 私有属性（外部不能直接访问）
        self.__balance = balance

    # 对外提供获取余额的方法
    def get_balance(self):
        return self.__balance

    # 对外提供存款方法
    def deposit(self, money):
        if money > 0:
            self.__balance += money
            print(f"存款成功，当前余额：{self.__balance}")
        else:
            print("存款金额必须大于0")

    # 对外提供取款方法
    def withdraw(self, money):
        if 0 < money <= self.__balance:
            self.__balance -= money
            print(f"取款成功，当前余额：{self.__balance}")
        else:
            print("取款失败，余额不足或金额无效")


# 使用
account = BankAccount("张三", 1000)
print("初始余额：", account.get_balance())

account.deposit(500)
account.withdraw(30)
print("最终余额：", account.get_balance())

# 直接访问私有属性会报错
# print(account.__balance)  # 报错！