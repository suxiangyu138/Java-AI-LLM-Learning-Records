class Student:
    def __init__(self, name, age, major):
        self.name = name
        self.age = age
        self.major = major

    def intro(self):
        print(f"姓名：{self.name}，年龄：{self.age}，专业：{self.major}")

    def study(self):
        print(f"{self.name} 正在学习 {self.major} 专业")

# 测试
s1 = Student("张三", 19, "计算机科学与技术")
s1.intro()
s1.study()