# Java 继承知识点完整版（面试 + 实战）

> **定位**：继承是 Java 面向对象三大核心特性（封装、继承、多态）之一，核心作用是**代码复用、简化开发、实现多态**。子类站在父类的基础上开发，无需重复编写已有代码，专注于自身特有的功能扩展。

---

## 目录

1. [核心定义](#1-核心定义)
2. [语法规则](#2-语法规则)
3. [核心特性](#3-核心特性)
4. [核心注意事项](#4-核心注意事项)
5. [面试高频考点](#5-面试高频考点)
6. [总结](#6-总结)

---

## 1. 核心定义

| 概念 | 说明 |
|------|------|
| **继承（Inheritance）** | 子类直接获取父类的非私有属性（成员变量）和方法（成员方法） |
| **子类（Subclass）** | 继承父类的类，可在父类基础上扩展 |
| **父类（Superclass）** | 被继承的类，提供公共属性和方法 |

**三大核心价值**：

| 价值 | 说明 |
|------|------|
| **代码复用** | 减少重复代码，降低开发成本 |
| **代码扩展** | 子类新增属性和方法，实现功能升级 |
| **奠定多态基础** | 子类对象可以赋值给父类引用，程序更灵活 |

---

## 2. 语法规则

> 通过 `extends` 关键字实现继承。

### 2.1 基本语法

```java
// 父类（基类）：定义公共的属性和方法
class 父类名 {
    // 父类的属性
    访问修饰符 数据类型 属性名;

    // 父类的方法
    访问修饰符 返回值类型 方法名(参数列表) {
        // 方法体
    }
}

// 子类（派生类）：继承父类，扩展自身功能
class 子类名 extends 父类名 {
    // 子类特有属性
    访问修饰符 数据类型 子类特有属性;

    // 子类特有方法
    访问修饰符 返回值类型 子类特有方法(参数列表) { ... }

    // 重写父类方法
    @Override
    访问修饰符 返回值类型 父类方法名(参数列表) {
        // 子类重写后的逻辑
    }
}
```

### 2.2 完整实战案例

```java
// 父类：Person（人）
class Person {
    String name;
    int age;

    public void eat() {
        System.out.println(name + "在吃饭");
    }

    public void sleep() {
        System.out.println(name + "在睡觉");
    }
}

// 子类：Student（学生）
class Student extends Person {
    String studentId;           // 子类特有属性：学号

    public void study() {       // 子类特有方法
        System.out.println(name + "（学号：" + studentId + "）在学习Java");
    }

    @Override
    public void eat() {         // 重写父类方法
        System.out.println(name + "在学校食堂吃饭，注重营养均衡");
    }
}

// 测试
public class TestInheritance {
    public static void main(String[] args) {
        Student student = new Student();
        student.name = "张三";          // 继承父类属性
        student.age = 18;               // 继承父类属性
        student.studentId = "2024001";  // 子类特有属性

        student.eat();     // 输出：张三在学校食堂吃饭，注重营养均衡（重写后）
        student.sleep();   // 输出：张三在睡觉（继承父类）
        student.study();   // 输出：张三（学号：2024001）在学习Java（子类特有）
    }
}
```

---

## 3. 核心特性

### 3.1 单继承特性

> ⚠️ Java 一个子类只能继承一个父类，不支持多继承。

```java
// ❌ 错误：不能同时继承多个父类
class Student extends Person, Animal { }  // 编译报错
```

**为什么不支持多继承**：避免"菱形继承"问题——多个父类有同名方法/属性时，子类无法确定调用哪个。

**解决方案**：通过"接口多实现"弥补单继承的局限性：

```java
class Student extends Person implements Studyable, Sportable { }
```

### 3.2 传递性

> 子类 → 父类 → 祖父类，层层传递。

```java
class Grandfather {
    public void run() { System.out.println("祖父会跑步"); }
}

class Father extends Grandfather {
    public void work() { System.out.println("父亲会工作"); }
}

class Son extends Father {
    public void play() { System.out.println("儿子会玩耍"); }
}

// 测试
Son son = new Son();
son.run();   // 继承祖父类的方法
son.work();  // 继承父类的方法
son.play();  // 自身的方法
```

### 3.3 子类不能继承父类的私有成员

> `private` 修饰的属性和方法，子类无法直接访问。

```java
class Person {
    private String idCard;                // 私有属性

    public String getIdCard() {           // 公共 getter
        return idCard;
    }
    public void setIdCard(String idCard) { // 公共 setter
        this.idCard = idCard;
    }
}

class Student extends Person {
    public void showIdCard() {
        // System.out.println(idCard);    // ❌ 不能直接访问
        System.out.println("身份证号：" + getIdCard());  // ✅ 通过 getter 间接访问
    }
}
```

### 3.4 方法重写（Override）

> 子类定义与父类"方法名、参数列表、返回值类型"完全一致的方法，覆盖父类实现。

**重写规则**：

| 规则 | 要求 |
|------|------|
| 方法签名 | 方法名、参数列表必须与父类完全一致 |
| 返回值类型 | 必须是父类返回值的子类或本身（不能是无关类型） |
| 访问修饰符 | 子类权限**不能低于**父类（父 `public` → 子不能 `protected`） |
| 不可重写 | `final` 方法、`static` 方法 |
| 注解 | 建议加 `@Override`，编译校验规则 |

```java
class Parent {
    public Number getValue() { return 100; }     // 返回 Number
}

class Child extends Parent {
    @Override
    public Integer getValue() { return 200; }    // ✅ Integer 是 Number 的子类
}
```

---

## 4. 核心注意事项

### 4.1 构造方法的继承规则

> ⚠️ 子类**不能继承**父类的构造方法。

| 场景 | 规则 |
|------|------|
| 父类有无参构造 | 子类构造方法默认调用 `super()`（隐式） |
| 父类只有有参构造 | 子类必须在构造方法中手动 `super(参数)` |
| `super()` 位置 | 必须放在子类构造方法**第一行** |

```java
class Person {
    String name;
    // 父类只有有参构造（无无参构造）
    public Person(String name) {
        this.name = name;
    }
}

class Student extends Person {
    String studentId;

    // 子类必须手动调用父类有参构造
    public Student(String name, String studentId) {
        super(name);          // ⚠️ 必须放在第一行
        this.studentId = studentId;
    }
}
```

### 4.2 super 关键字的三种用法

| 用法 | 语法 | 场景 |
|------|------|------|
| 调用父类属性 | `super.属性名` | 子类与父类有同名属性，区分父类属性 |
| 调用父类方法 | `super.方法名(参数)` | 子类重写了方法，仍想调用父类原方法 |
| 调用父类构造 | `super()` / `super(参数)` | 仅子类构造方法中使用，必须第一行 |

### 4.3 final 关键字与继承

| 修饰目标 | 效果 | 示例 |
|----------|------|------|
| 类 | 不能被继承 | `final class String {}` |
| 方法 | 不能被重写 | `public final void init() {}` |
| 属性 | 常量不可修改，但不影响继承 | `final int MAX = 100;`（子类可继承使用） |

### 4.4 继承的使用场景（is-a 原则）

| 判断标准 | ✅ 正确 | ❌ 错误 |
|----------|--------|--------|
| **is-a 关系** | `Student is a Person` | `Student is a Teacher`（平级关系） |
| **做法** | Student 继承 Person | Student 和 Teacher 共同继承 Person |

> ⚠️ 非 is-a 关系会导致代码耦合度高、难以维护。

---

## 5. 面试高频考点

### 5.1 Java 为什么不支持多继承？

> 核心原因：**避免菱形继承（歧义）**问题。

假设子类同时继承两个父类，两个父类有同名方法，子类无法确定调用哪个。Java 通过**"单继承 + 接口多实现"**解决——既满足多维度的功能扩展，又避免歧义。

### 5.2 方法重写（Override）vs 方法重载（Overload）

| 对比维度 | 重写（Override） | 重载（Overload） |
|----------|-----------------|------------------|
| **定义** | 子类重写父类方法，方法签名完全一致 | 同一类中，方法名相同，参数列表不同 |
| **所属范围** | 子类与父类之间 | 同一类中 |
| **参数列表** | 必须一致 | 必须不同（个数/类型/顺序） |
| **返回值** | 需是父类返回值的子类或本身 | 无要求 |
| **访问修饰符** | 不能低于父类 | 无要求 |
| **核心作用** | 扩展父类方法，实现子类特有逻辑 | 同一方法名处理不同参数，简化调用 |

### 5.3 super vs this

| 关键字 | 指代 | 用途 |
|--------|------|------|
| `this` | 当前对象 | 访问当前类的属性、方法、构造方法 |
| `super` | 父类对象 | 访问父类的属性、方法、构造方法 |

```java
class Child extends Parent {
    private String name;

    public Child(String name, String parentName) {
        super(parentName);      // 调用父类构造
        this.name = name;       // this.name = 当前类的成员变量
    }
}
```

### 5.4 父类的私有方法，子类能继承吗？

> ❌ **不能**。`private` 修饰的方法仅父类内部可见，子类无法继承、访问、重写。

但子类可通过父类的 `public`/`protected` getter/setter **间接访问**私有属性。

---

## 6. 总结

| 层次 | 要点 |
|------|------|
| **核心定义** | 子类继承父类非私有属性和方法，实现代码复用 |
| **语法** | `class 子类 extends 父类`，`super` 调用父类构造 |
| **三大特性** | 单继承 + 传递性 + 方法重写 |
| **三大限制** | 不继承私有成员 + 不继承构造方法 + `final` 禁止继承/重写 |
| **面试重点** | 不支持多继承的原因、重写 vs 重载、super vs this、is-a 原则 |

```text
继承知识体系
├── 核心价值：代码复用 → 代码扩展 → 奠定多态基础
├── 语法：extends + super + @Override
├── 特性：单继承、传递性、不可继承私有、方法重写
├── 限制：构造方法不继承、final 不可重写/继承、必须 is-a
└── 面试：菱形继承、Override vs Overload、super vs this
```
