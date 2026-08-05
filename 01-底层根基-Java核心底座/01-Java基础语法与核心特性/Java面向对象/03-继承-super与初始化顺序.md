# 03 继承：super 与初始化顺序

> 继承让子类复用父类能力并扩展——但"复用"是有代价的：单继承限制、重写规则、初始化顺序，每一处都是面试与踩坑的重灾区

---

## 📚 目录

1. [继承的本质与单继承限制](#1-继承的本质与单继承限制)
2. [子类继承了什么、不继承什么](#2-子类继承了什么不继承什么)
3. [super 关键字三种用法](#3-super-关键字三种用法)
4. [方法重写五条硬规则](#4-方法重写五条硬规则)
5. [final 的三重身份](#5-final-的三重身份)
6. [继承链中的初始化顺序](#6-继承链中的初始化顺序)
7. [继承的代价与组合替代](#7-继承的代价与组合替代)

---

## 1. 继承的本质与单继承限制

继承表达 **is-a** 关系：子类（Subclass）是父类（Superclass）的一种。`Manager is a Employee`，因此 Manager 可以出现在任何需要 Employee 的地方（多态的前提）。

```java
public class Employee {
    protected String name;
    protected double salary;

    public Employee(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }

    public double calculateBonus() { return salary * 0.1; }
}

public class Manager extends Employee {
    private int teamSize;

    public Manager(String name, double salary, int teamSize) {
        super(name, salary);       // 调用父类构造器，必须先于子类逻辑
        this.teamSize = teamSize;
    }

    @Override
    public double calculateBonus() {   // 重写：经理的奖金规则不同
        return salary * 0.3 + teamSize * 1000;
    }
}
```

**为什么 Java 类只能单继承？** 避免**菱形继承**（Diamond Problem）：若一个类同时继承 A、B，而 A、B 都有 `foo()`，子类到底继承谁的 `foo()` 无法判定。C++ 引入虚拟继承解决但复杂度极高；Java 选择"类单继承 + 接口多实现"，让"行为契约"可以多继承而"实现"只有一条链。

| 继承形式 | 是否支持 | 原因 |
|---------|:-------:|------|
| 类 extends 类 | 单继承 | 避免菱形歧义 |
| 类 implements 接口 | 多实现 | 接口只定义契约，冲突由默认方法规则解决（见 05 模块） |
| 接口 extends 接口 | 多继承 | 同上，无状态冲突 |

> 💡 单继承 + 多接口是 Java 的经典权衡：牺牲"实现的复用面"换取**语义确定性**。

---

## 2. 子类继承了什么、不继承什么

| 父类成员 | 子类是否可见/可用 | 说明 |
|---------|:----------------:|------|
| `public` / `protected` 字段与方法 | ✅ | 直接使用 |
| `default` 成员 | 同包 ✅ / 跨包 ❌ | 包私有是作用域边界 |
| `private` 字段与方法 | ❌ | 不可见（但**存在于**子类对象中） |
| **构造器** | ❌ | 不继承，需用 `super(...)` 显式调用 |
| `static` 方法 | 可见但**不是继承** | 重名同参叫"隐藏"，受引用类型影响（见 04 模块） |

```java
public class Parent {
    private int secret = 42;          // 子类对象内存中真实存在，但不可见
    protected int visible = 10;
    public static void staticMethod() { }
}

public class Child extends Parent {
    public void test() {
        System.out.println(visible);  // ✅ 可访问
        // System.out.println(secret); // ❌ 编译错误
    }
}
```

> 🎯 **核心要点**：private 成员"不被继承访问，但被继承存在"——`new Child()` 在堆中分配的内存包含父类所有字段（含 private），只是子类代码无法直接操作它们。

---

## 3. super 关键字三种用法

| 用法 | 语法 | 场景 |
|------|------|------|
| 访问父类字段 | `super.字段名` | 子类字段与父类字段重名遮蔽时 |
| 调用父类方法 | `super.方法名()` | 子类重写后仍需父类原逻辑（先 super 再扩展） |
| 调用父类构造器 | `super(参数)` | 必须位于子类构造器**第一条语句** |

```java
public class Manager extends Employee {
    private String name;   // 遮蔽父类的 name

    public Manager(String name, double salary, int teamSize) {
        super(name, salary);          // 1. 必须第一行
        this.name = "经理-" + name;   // 子类字段
    }

    @Override
    public double calculateBonus() {
        double base = super.calculateBonus();   // 复用父类 0.1 倍逻辑
        return base + teamSize * 1000;          // 再扩展
    }
}
```

> ⚠️ **易错点**：① 子类构造器不写 `super(...)` 时，编译器自动插入**无参** `super()`——若父类没有无参构造器则编译失败，此时必须显式调用带参父类构造器；② `this(...)` 与 `super(...)` 互斥且都必须在第一行。

---

## 4. 方法重写五条硬规则

1. **签名必须完全一致**：方法名、参数列表相同（重写不是重载）。
2. **返回类型**：相同，或为父类返回类型的**子类型**（协变返回，见 04 模块）。
3. **访问权限**：不能比父类更严格（`public` 父类方法子类不能降为 `private`）。
4. **异常声明**：不能抛出比父类**更宽泛**的受检异常（可更少或更具体，可新增非受检异常）。
5. **static / final / private 方法不可重写**：static 方法同名同参是"隐藏"；final 方法禁止重写；private 方法不是重写（是各自独立的方法，`@Override` 标注会编译报错）。

```java
public class Parent {
    protected Object make() throws IOException { return new Object(); }
}

public class Child extends Parent {
    @Override
    public String make() throws FileNotFoundException {   // ✅ 权限变宽、协变返回、异常变窄
        return "child result";
    }
}
```

> 💡 **@Override 注解为什么必须加**：它让编译器校验上述规则——写错签名时，不加注解会"静默地变成重载"，行为诡异且难以排查；加注解则直接编译报错。

---

## 5. final 的三重身份

| 修饰位置 | 含义 | 效果 |
|---------|------|------|
| `final class` | 类不可被继承 | 防止子类破坏设计（如 `String`、`Integer` 全部 final） |
| `final 方法` | 方法不可被重写 | 骨架算法锁定（模板方法中的不可变步骤） |
| `final 变量` | 值/引用不可重新赋值 | 常量、安全发布（JMM 保证 final 字段可见性） |

```java
public final class Config { ... }        // 禁止继承

public class Base {
    public final void lockStep() { }     // 子类不可重写，保证算法固定
    public void openStep() { }           // 子类可重写，扩展点
}
```

> ⚠️ **final 变量 ≠ 不可变对象**：`final List<String> list = new ArrayList<>()` 只是引用不能重新指向，`list.add()` 仍然可以修改集合内容。要真正不可变需配合不可变类/不可变集合（见 02 模块）。

---

## 6. 继承链中的初始化顺序

**完整顺序**（这是面试几乎必考的一题）：

```text
父类静态块/静态字段 → 子类静态块/静态字段   （类加载阶段，各一次）
→ 父类实例块/实例字段 → 父类构造器
→ 子类实例块/实例字段 → 子类构造器
```

```java
public class Parent {
    static { System.out.println("1. 父类静态块"); }
    private int p = initP();                 // 字段初始化与实例块按声明顺序
    { System.out.println("4. 父类实例块"); }

    Parent() { System.out.println("5. 父类构造器"); }
    private int initP() { System.out.println("3. 父类字段初始化"); return 1; }
}

public class Child extends Parent {
    static { System.out.println("2. 子类静态块"); }
    private int c = initC();
    { System.out.println("7. 子类实例块"); }

    Child() { System.out.println("8. 子类构造器"); }
    private int initC() { System.out.println("6. 子类字段初始化"); return 1; }
}

// new Child() 输出：1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
```

**规则提炼**：

1. **静态先行、自上而下**：静态初始化（含静态字段）在类加载时执行，父类先于子类；再次 `new` 时静态部分不再执行。
2. **实例初始化自上而下**：父类实例块/字段 → 父类构造器 → 子类实例块/字段 → 子类构造器。
3. **实例块与字段初始化按声明顺序**，且都在构造器体之前。

> ⚠️ **重写陷阱**：若父类构造器调用了**被子类重写的方法**，此时子类字段尚未初始化（值为默认值）——这是著名的"构造器调用可重写方法"反模式，导致读到 null/0。

```java
public class Parent {
    Parent() { print(); }               // 危险：构造器调用可重写方法
    protected void print() { System.out.println("Parent"); }
}

public class Child extends Parent {
    private String name = "child";      // 此时尚未执行！
    @Override protected void print() { System.out.println(name); }
}

new Child();   // 输出 null，而不是 "child" —— 因为父类构造器先执行，name 还是默认值
```

> 🎯 **核心要点**：父类构造器执行时，子类字段还是默认值。因此**构造器内只调用 private/final 方法**，绝不调用可重写方法。

---

## 7. 继承的代价与组合替代

**继承的优势**：代码复用、is-a 语义自然、为多态提供基础、增量扩展（重写 + super 复用父类逻辑）。

**继承的代价**：

| 问题 | 说明 |
|------|------|
| 脆弱基类 | 父类方法修改实现，所有子类行为随之改变，且常不可见 |
| 耦合加深 | 子类依赖父类内部细节，`protected` 字段破坏封装 |
| 层次过深 | 3 层以上难以维护（阿里规约：超过 3 层禁止） |
| 不恰当的 is-a | 继承"工具类"只是想要方法复用，语义错误 |
| 重写耦合 | 子类重写若漏调 `super`，父类不变量被破坏 |

**组合优于继承**（Favor Composition over Inheritance）：

```java
// 错误示范：为复用而继承
public class OrderExporter extends FileUtil {   // OrderExporter is-a FileUtil？语义不通
    public void export(Order order) { writeFile(order.toString()); }
}

// 正确示范：组合复用能力
public class OrderExporter {
    private final FileUtil fileUtil = new FileUtil();   // has-a 关系
    public void export(Order order) { fileUtil.writeFile(order.toString()); }
}
```

**决策准则**：只有"子类确实是父类的一种"（is-a 成立）且"需要多态替换"时才用继承；只为复用方法时用组合；需要"行为契约"时用接口。

---

**下一模块**：[04-多态-动态绑定与重载重写](./04-多态-动态绑定与重载重写.md) / **返回总览**：[00-Java面向对象知识体系总览](./00-Java面向对象知识体系总览.md)
