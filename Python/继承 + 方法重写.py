# 父类
class Animal:
    def __init__(self, name):
        self.name = name

    def eat(self):
        print(f"{self.name} 正在吃东西")

    def sleep(self):
        print(f"{self.name} 正在睡觉")


# 子类：Dog 继承 Animal
class Dog(Animal):
    def __init__(self, name, breed):
        # 调用父类构造方法
        super().__init__(name)
        self.breed = breed

    # 方法重写（覆盖父类的方法）
    def eat(self):
        print(f"{self.name}（{self.breed}）正在啃骨头")

    # 子类新增方法
    def bark(self):
        print(f"{self.name} 汪汪叫！")


# 子类：Cat 继承 Animal
class Cat(Animal):
    def __init__(self, name, color):
        super().__init__(name)
        self.color = color

    def eat(self):
        print(f"{self.name}（{self.color}猫）正在吃鱼")

    def meow(self):
        print(f"{self.name} 喵喵叫！")


# 使用
dog = Dog("旺财", "金毛")
dog.eat()
dog.bark()

cat = Cat("咪咪", "橘色")
cat.eat()
cat.meow()