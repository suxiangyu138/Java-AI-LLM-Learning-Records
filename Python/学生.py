class Student:
    # 构造方法：初始化对象属性
    def __init__(self, name, age, grade):
        self.name = name
        self.age = age
        self.grade = grade

    # 实例方法：自我介绍
    def introduce(self):
        print(f"我叫{self.name}，今年{self.age}岁，读{self.grade}年级")

    # 实例方法：学习行为
    def study(self, subject):
        print(f"{self.name} 正在学习 {subject}")


# 创建对象（实例化）
stu1 = Student("小明", 12, 6)
stu2 = Student("小红", 11, 5)

# 调用方法
stu1.introduce()
stu1.study("Python")

stu2.introduce()
stu2.study("数学")