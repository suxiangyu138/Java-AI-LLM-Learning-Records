/**
 * Java 继承核心操作专项讲解
 * 覆盖：继承定义、super关键字、方法重写、继承限制、多层继承
 */
public class InheritanceExperience {
    public static void main(String[] args) {
        // ====================== 1. 基本继承：子类使用父类属性/方法 ======================
        System.out.println("===== 1. 基本继承使用 =====");
        // 创建子类对象
        Dog dog = new Dog();
        // 调用父类继承的属性
        dog.name = "旺财";
        dog.age = 3;
        // 调用父类继承的方法
        dog.eat(); // 父类方法
        // 调用子类特有方法
        dog.bark(); // 子类扩展方法
        System.out.println();

        // ====================== 2. super关键字：调用父类构造/属性/方法 ======================
        System.out.println("===== 2. super关键字使用 =====");
        // 子类有参构造 → 调用父类有参构造
        Cat cat = new Cat("咪咪", 2, "橘猫");
        cat.showInfo(); // 调用重写后的方法
        // 子类方法中调用父类方法
        cat.eat(); // 子类重写后，通过super调用父类原方法
        System.out.println();

        // ====================== 3. 方法重写：子类扩展父类功能 ======================
        System.out.println("===== 3. 方法重写 =====");
        Bird bird = new Bird("小白", 1, "白色");
        bird.move(); // 执行子类重写的move方法
        // 对比父类方法（如果没重写会执行Animal的move）
        Animal animal = new Animal();
        animal.move();
        System.out.println();

        // ====================== 4. 多层继承：子类的子类 ======================
        System.out.println("===== 4. 多层继承 =====");
        Husky husky = new Husky("二哈", 1, "黑白");
        husky.bark(); // 继承Dog的方法
        husky.eat(); // 继承Animal的方法
        husky.digHole(); // 自己的特有方法
        System.out.println();

        // ====================== 5. 继承限制：final类/方法 ======================
        System.out.println("===== 5. 继承限制（final） =====");
        // FinalAnimal 是final类，无法被继承（下面代码会报错）
        // class Test extends FinalAnimal {} 

        GoldenRetriever golden = new GoldenRetriever("大黄", 2);
        golden.eat(); // final方法无法被重写，只能用父类的实现
    }
}

// -------------------------- 父类：Animal（基类/超类） --------------------------
/**
 * 父类：动物（所有子类的基类）
 * 抽取所有动物的公共属性和方法
 */
class Animal {
    // 公共属性
    String name;
    int age;

    // 1. 无参构造
    public Animal() {
        System.out.println("Animal无参构造执行");
    }

    // 2. 有参构造
    public Animal(String name, int age) {
        this.name = name;
        this.age = age;
        System.out.println("Animal有参构造执行：" + name + "," + age);
    }

    // 公共方法：吃
    public void eat() {
        System.out.println(name + "正在吃食物");
    }

    // 公共方法：移动
    public void move() {
        System.out.println(name + "正在移动");
    }
}

// -------------------------- 子类1：Dog（继承Animal） --------------------------
/**
 * 子类：狗（继承Animal）
 * 扩展父类功能，新增特有属性/方法
 */
class Dog extends Animal {
    // 子类特有属性
    String color;

    // 1. 子类无参构造（默认调用父类无参构造）
    public Dog() {
        // super(); // 隐式调用父类无参构造，可省略
        System.out.println("Dog无参构造执行");
    }

    // 2. 子类有参构造（显式调用父类有参构造）
    public Dog(String name, int age, String color) {
        super(name, age); // 必须放在第一行，调用父类有参构造
        this.color = color;
        System.out.println("Dog有参构造执行：" + color);
    }

    // 子类特有方法：叫
    public void bark() {
        System.out.println(name + "（" + color + "）汪汪叫");
    }

    // 重写父类方法：扩展eat的功能
    @Override
    public void eat() {
        super.eat(); // 调用父类原方法
        System.out.println(name + "喜欢吃骨头"); // 子类扩展逻辑
    }
}

// -------------------------- 子类的子类：Husky（继承Dog） --------------------------
/**
 * 多层继承：哈士奇（继承Dog，间接继承Animal）
 */
class Husky extends Dog {
    public Husky(String name, int age, String color) {
        super(name, age, color); // 调用父类Dog的有参构造
    }

    // 特有方法：挖坑
    public void digHole() {
        System.out.println(name + "（哈士奇）正在刨坑");
    }
}

// -------------------------- 子类2：Cat（继承Animal） --------------------------
/**
 * 子类：猫（继承Animal）
 */
class Cat extends Animal {
    String breed; // 品种

    public Cat(String name, int age, String breed) {
        super(name, age); // 调用父类构造
        this.breed = breed;
    }

    // 重写父类move方法
    @Override
    public void move() {
        System.out.println(name + "（" + breed + "）轻盈地走猫步");
    }

    // 重写父类showInfo（自定义展示）
    public void showInfo() {
        System.out.println("猫的信息：姓名=" + super.name + "，年龄=" + super.age + "，品种=" + breed);
    }
}

// -------------------------- 子类3：Bird（继承Animal） --------------------------
/**
 * 子类：鸟（继承Animal）
 */
class Bird extends Animal {
    String featherColor;

    public Bird(String name, int age, String featherColor) {
        super(name, age);
        this.featherColor = featherColor;
    }

    // 重写move方法（完全替换父类逻辑）
    @Override
    public void move() {
        System.out.println(name + "（羽毛" + featherColor + "）正在飞翔");
    }
}

// -------------------------- 继承限制1：final类（无法被继承） --------------------------
/**
 * final类：无法被继承
 */
final class FinalAnimal {
    public void sleep() {
        System.out.println("动物睡觉");
    }
}

// -------------------------- 继承限制2：final方法（无法被重写） --------------------------
/**
 * 子类：金毛（继承Dog），演示final方法
 */
class GoldenRetriever extends Dog {
    public GoldenRetriever(String name, int age) {
        super(name, age, "金黄色");
    }

    // final方法无法重写（下面代码会报错）
    // @Override
    // public final void eat() {}
}