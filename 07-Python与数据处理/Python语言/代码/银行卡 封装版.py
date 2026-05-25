class BankCard:
    def __init__(self, name, balance):
        self.name = name
        self.__balance = balance

    def get_balance(self):
        return self.__balance

    def deposit(self, money):
        if money > 0:
            self.__balance += money
            print(f"存款成功，当前余额：{self.__balance}")
        else:
            print("金额必须大于0")

    def withdraw(self, money):
        if money <= 0:
            print("取款金额必须大于0")
        elif money > self.__balance:
            print("余额不足")
        else:
            self.__balance -= money
            print(f"取款成功，当前余额：{self.__balance}")

# 测试
card = BankCard("李四", 2000)
print("查询余额：", card.get_balance())
card.deposit(500)
card.withdraw(800)