class Tool:
    count = 0  # 类属性（所有对象共享）

    def __init__(self, name):
        self.name = name
        Tool.count += 1

    # 实例方法
    def use(self):
        print(f"使用工具：{self.name}")

    # 类方法（用@classmethod装饰）
    @classmethod
    def show_tool_count(cls):
        print(f"工具总数：{cls.count}")

    # 静态方法（用@staticmethod装饰）
    @staticmethod
    def desc():
        print("工具类：用于实现各种工具的通用功能")


# 子类1
class Hammer(Tool):
    def use(self):
        print(f"用 {self.name} 敲钉子")


# 子类2
class Screwdriver(Tool):
    def use(self):
        print(f"用 {self.name} 拧螺丝")


# 多态函数
def use_tool(tool: Tool):
    tool.use()


# 使用
hammer = Hammer("铁锤")
screwdriver = Screwdriver("螺丝刀")

# 多态：不同子类对象调用同一个方法，表现不同
use_tool(hammer)
use_tool(screwdriver)

# 调用类方法和静态方法
Tool.show_tool_count()
Tool.desc()
