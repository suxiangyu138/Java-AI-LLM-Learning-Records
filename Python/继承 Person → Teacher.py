class Person:
    def __init__(self, name, age):
        self.name = name
        self.age = age

    def eat(self):
        print(f"{self.name} 在吃饭")


class Teacher(Person):
    def __init__(self, name, age, subject):
        super().__init__(name, age)
        self.subject = subject

    # 重写父类方法
    def eat(self):
        print(f"老师 {self.name} 悠闲地吃饭")

    # 新增自有方法
    def teach(self):
        print(f"{self.name} 讲授 {self.subject} 课程")

# 测试
t1 = Teacher("王老师", 35, "Python编程")
t1.eat()
t1.teach()