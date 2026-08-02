# 10 - 内部类、枚举与Record

> 定位：掌握Java内部类四种类型、枚举的高级用法、JDK 14+ Record类与JDK 17+密封类，理解它们各自解决的问题与应用场景

## 目录

1. [内部类概述与分类](#1-内部类概述与分类)
2. [成员内部类](#2-成员内部类)
3. [静态内部类（静态嵌套类）](#3-静态内部类静态嵌套类)
4. [局部内部类](#4-局部内部类)
5. [匿名内部类](#5-匿名内部类)
6. [内部类选择指南](#6-内部类选择指南)
7. [枚举（Enum）基础](#7-枚举enum基础)
8. [枚举高级用法](#8-枚举高级用法)
9. [Record类（JDK 14+）](#9-record类jdk-14)
10. [密封类 Sealed Class（JDK 17+）](#10-密封类-sealed-classjdk-17)
11. [各类型对比总结](#11-各类型对比总结)
12. [面试高频考点](#12-面试高频考点)

---

## 1. 内部类概述与分类

### 1.1 核心定义

> 内部类（Inner Class）本质是一个类嵌套在另一个类（外部类）内部，可看作外部类的"成员"。内部类可直接访问外部类所有成员（包括私有），同时隐藏自身实现细节。

| 特性 | 说明 |
|------|------|
| 访问外部类 | 内部类可直接访问外部类**所有**成员（包括 `private`） |
| 被外部类访问 | 外部类需通过内部类对象才能访问内部类成员 |

### 1.2 核心价值

| 价值 | 说明 |
|------|------|
| **高度封装** | 内部类隐藏在外部类内部，保护实现细节 |
| **便捷访问** | 无需 getter/setter 即可访问外部类所有成员 |
| **解决多继承局限** | 多个内部类可各自继承不同类，间接实现多继承 |
| **简化代码结构** | 仅为另一个类服务时，定义成内部类更紧凑 |

### 1.3 四大内部类速览

| 类型 | `static` | 定义位置 | 依赖外部类对象 | 访问外部类 | 可定义静态成员 |
|------|:--------:|----------|:------------:|------------|:------------:|
| **成员内部类** | 无 | 类体内 | 是 | 所有成员 | 否 |
| **静态内部类** | 有 | 类体内 | 否 | 仅静态成员 | 是 |
| **局部内部类** | 无 | 方法/代码块内 | 是 | 所有成员 | 否 |
| **匿名内部类** | 无 | 方法/代码块内 | 是 | 所有成员 | 否 |

### 1.4 编译后 class 文件命名

| 类型 | 文件名 |
|------|--------|
| 成员/静态内部类 | `Outer$Inner.class` |
| 匿名内部类 | `Outer$1.class`（数字编号） |

---

## 2. 成员内部类

> 无 `static` 修饰，属于外部类的非静态成员，**依赖外部类对象存在**。

```java
class Person {
    private String name = "张三";

    class Heart {  // 成员内部类
        public void beat() {
            System.out.println(name + "的心脏跳动");  // 直接访问外部私有属性
        }
    }
}

// 外部创建方式
Person person = new Person();
Person.Heart heart = person.new Heart();  // 依赖外部类对象
heart.beat();
```

| 特性 | 说明 |
|------|------|
| 创建方式 | `外部类对象.new 内部类名()` |
| 静态成员 | 不能定义 `static` 属性/方法 |
| 修饰符 | `public`/`protected`/`private`/默认 |
| 生命周期 | 与外部类对象一致 |

> ⚠️ **为什么不能定义静态成员？** 成员内部类依赖外部类对象存在，而静态成员是类级别的，不依赖对象，二者生命周期冲突。

### 命名冲突解决

```java
class Outer {
    String name = "外部";
    class Inner {
        String name = "内部";
        public void show() {
            String name = "局部";
            System.out.println(name);              // 局部
            System.out.println(this.name);         // 内部
            System.out.println(Outer.this.name);   // 外部
        }
    }
}
```

---

## 3. 静态内部类（静态嵌套类）

> 有 `static` 修饰，属于外部类的静态成员，**不依赖外部类对象**，独立性最强。

```java
class Computer {
    private static String brand = "华为";
    private String model = "MateBook";  // 非静态，静态内部类不可访问

    static class CPU {
        private static String type = "麒麟9000";

        public static void showBrand() {
            System.out.println("品牌：" + brand);  // 只能访问外部静态成员
            // System.out.println(model);          // 编译错误
        }
    }
}

// 无需外部类对象，直接创建
Computer.CPU.showBrand();
Computer.CPU cpu = new Computer.CPU();
```

| 特性 | 说明 |
|------|------|
| 创建方式 | `new 外部类名.内部类名()`，无需外部类对象 |
| 访问外部类 | **只能**访问外部类的静态成员 |
| 静态成员 | 可以自由定义 |
| 生命周期 | 与外部类一致（类加载时加载） |

> 💡 **典型应用**：Builder模式、`Map.Entry`、与外部类逻辑相关但独立的辅助类。

---

## 4. 局部内部类

> 定义在方法/代码块内部，**仅当前方法内有效**，生命周期与方法一致。

```java
class Calculator {
    private int num1 = 10;

    public int add(int num2) {
        int sumTemp = 0;  // 必须是 effectively final

        class CalculateLogic {  // 局部内部类，仅在 add 方法内有效
            public int compute() {
                return num1 + num2 + sumTemp;
            }
        }
        return new CalculateLogic().compute();
    }
}
```

| 特性 | 说明 |
|------|------|
| 作用域 | 仅定义它的方法内有效 |
| 修饰符 | 不能用 `public`/`private`/`protected`/`static` |
| 局部变量限制 | 访问的局部变量必须 `final` 或 effectively final |

> ⚠️ **为什么局部变量必须 effectively final？** 方法结束后局部变量销毁，但内部类对象可能仍在（如被返回）。为保证变量值不变，需禁止修改。JDK 8+ 可省略 `final`，但值不能变。

---

## 5. 匿名内部类

> 没有类名的局部内部类，本质是**继承了某类/实现了某接口的匿名子类对象**，仅能使用一次。

```java
interface Animal {
    void eat();
    void run();
}

class Zoo {
    private String food = "青草";

    public void showAnimal() {
        // 匿名内部类：实现 Animal 接口
        Animal dog = new Animal() {
            @Override
            public void eat() {
                System.out.println("小狗吃" + food);  // 访问外部类私有属性
            }
            @Override
            public void run() {
                System.out.println("小狗飞快地跑");
            }
        };
        dog.eat();
    }
}
```

### 常见应用场景

| 场景 | 示例 |
|------|------|
| 线程创建 | `new Thread(() -> { ... }).start()` |
| 集合排序 | `Collections.sort(list, new Comparator<>() { ... })` |
| 事件监听 | `button.addActionListener(new ActionListener() { ... })` |

| 特性 | 说明 |
|------|------|
| 类名 | 没有类名 |
| 构造方法 | 不能定义（可用构造代码块初始化） |
| 使用次数 | 仅能创建**一个**对象 |
| 前提 | 必须继承一个类或实现一个接口 |
| 静态成员 | 不能定义 |

> 💡 **Lambda 替代**：对于函数式接口（唯一抽象方法），JDK 8+ 优先使用 Lambda。匿名内部类仍适用于抽象类或多方法接口。

---

## 6. 内部类选择指南

### 访问修饰符限制

| 类型 | 可用修饰符 | `static` |
|------|:---------:|:--------:|
| 成员内部类 | `public`/`protected`/`private`/默认 | 否 |
| 静态内部类 | `public`/`protected`/`private`/默认 | 是 |
| 局部内部类 | 无 | 否 |
| 匿名内部类 | 无 | 否 |

### 选型决策

```
需要独立于外部类对象？──是──→ 静态内部类（Builder、辅助类）
否
仅使用一次、需快速实现？──是──→ 匿名内部类（回调、Lambda）
否
仅在方法内复用逻辑？──是──→ 局部内部类（极少用）
否
紧密绑定外部类实例？──→ 成员内部类（迭代器、事件适配器）
```

> ⚠️ **内存泄漏**：成员内部类持有外部类引用，易导致内存泄漏。Android 开发中常用静态内部类 + WeakReference 替代。

---

## 7. 枚举（Enum）基础

> Java 5 引入的 `enum` 关键字，用于定义固定数量的常量集合。枚举类隐式继承 `java.lang.Enum`，不能手动继承。

```java
public enum Status {
    PENDING, APPROVED, REJECTED, CANCELLED
}

// 使用
Status status = Status.APPROVED;
System.out.println(status.name());        // "APPROVED"
System.out.println(status.ordinal());     // 1（从 0 开始）
Status s = Status.valueOf("PENDING");     // 字符串转枚举
Status[] all = Status.values();           // 所有枚举值
```

### 重要方法

| 方法 | 说明 |
|------|------|
| `name()` | 返回枚举常量名称 |
| `ordinal()` | 返回序号（从 0 开始） |
| `values()` | 返回所有枚举常量数组（静态方法） |
| `valueOf(String)` | 名称字符串转枚举常量 |

> 💡 枚举常量比较用 `==` 而非 `equals()`：二者结果一致，但 `==` 更快且无空指针问题。

---

## 8. 枚举高级用法

### 8.1 带字段和方法的枚举

```java
public enum OrderStatus {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    DELIVERED(3, "已签收"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {  // 构造方法隐式 private
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    // 状态机转换
    public OrderStatus next() {
        return switch (this) {
            case PENDING -> PAID;
            case PAID -> SHIPPED;
            case SHIPPED -> DELIVERED;
            case DELIVERED, CANCELLED -> throw new IllegalStateException("终态");
        };
    }
}
```

### 8.2 枚举实现策略模式

每个枚举常量可各自实现抽象方法，是策略模式最优雅的实现方式：

```java
public enum PayStrategy {
    ALIPAY("支付宝") {
        @Override
        public void pay(BigDecimal amount) {
            System.out.println("支付宝支付: " + amount);
        }
    },
    WECHAT("微信支付") {
        @Override
        public void pay(BigDecimal amount) {
            System.out.println("微信支付: " + amount);
        }
    };

    private final String displayName;
    PayStrategy(String displayName) { this.displayName = displayName; }

    public abstract void pay(BigDecimal amount);  // 每个常量必须实现
}

// 使用
PayStrategy strategy = PayStrategy.ALIPAY;
strategy.pay(new BigDecimal("100.00"));
```

> 🎯 **枚举策略模式优势**：类型安全、单例保证、策略与定义在同一处，无需独立类层次。

### 8.3 枚举要点

| 要点 | 说明 |
|------|------|
| 构造方法 | 隐式 `private`，不能手动 `new` |
| 继承限制 | 不能继承其他类（已隐式继承 `Enum`） |
| 接口实现 | 可以实现接口 |
| 单例保证 | 每个枚举常量是全局唯一的单例 |

---

## 9. Record类（JDK 14+）

> Record 是一种透明的数据载体，自动生成构造方法、getter（命名 `x()` 而非 `getX()`）、`equals()`、`hashCode()`、`toString()`。JDK 14 预览，JDK 16 正式稳定。

### 9.1 基本用法

```java
// 传统 POJO：需手动编写大量样板代码
public final class Point {
    private final int x;
    private final int y;
    public Point(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    // equals(), hashCode(), toString() 均需手动实现
}

// Record：一行搞定，编译器自动生成所有上述方法
public record PointRecord(int x, int y) {
    // 紧凑构造：参数验证（不需要 this.x = x; 编译器自动处理）
    public PointRecord {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("坐标不能为负数");
        }
    }

    // 可添加静态方法和实例方法
    public double distanceFromOrigin() {
        return Math.sqrt(x * x + y * y);
    }
}
```

### 9.2 Record 对比

| 特性 | Record | 传统 POJO |
|------|--------|-----------|
| 构造/Getter | 自动生成 | 手动编写 |
| `equals`/`hashCode`/`toString` | 基于所有组件自动生成 | 手动编写 |
| 可变性 | 所有字段 `private final`，不可变 | 可设 setter |
| 继承 | 隐式 `final`，不能继承其他类 | 可自由继承 |
| 实例字段 | 只能声明组件，不能额外添加 | 自由添加 |
| 接口实现 | 可以实现接口 | 可以 |

> 💡 **最佳实践**：适用于 DTO、VO、请求参数、返回值等不可变数据载体。不适合有业务逻辑或可变状态的场景。

---

## 10. 密封类 Sealed Class（JDK 17+）

> 密封类用于限制哪些类可以继承/实现它，提供精确的继承控制。JDK 17 正式稳定。

```java
// 密封类：只允许 Circle、Rectangle 继承
public sealed class Shape
    permits Circle, Rectangle {

    public abstract double area();
}

// 子类必须用 final、sealed 或 non-sealed 修饰
final class Circle extends Shape {              // 不再允许继续继承
    private final double radius;
    public Circle(double radius) { this.radius = radius; }
    @Override
    public double area() { return Math.PI * radius * radius; }
}

non-sealed class Rectangle extends Shape {      // 恢复为普通类，可任意继承
    private final double width, height;
    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }
    @Override
    public double area() { return width * height; }
}

// 密封接口
sealed interface Service permits UserService, OrderService {
    void execute();
}

final class UserService implements Service {
    public void execute() { System.out.println("用户服务"); }
}
```

### 与 switch 模式匹配配合

密封类与 `switch` 配合，编译器可检查穷举性，无需 `default`：

```java
public String process(Shape shape) {
    return switch (shape) {
        case Circle c -> "圆形, 面积: " + c.area();
        case Rectangle r -> "矩形, 面积: " + r.area();
        // 不需要 default，密封类已穷举
    };
}
```

| 子类修饰符 | 含义 | 允许继续继承 |
|-----------|------|:-----------:|
| `final` | 不再允许继承 | 否 |
| `sealed` | 继续密封，需列出 permits | 是（受限制） |
| `non-sealed` | 恢复为普通类 | 是（不受限） |

---

## 11. 各类型对比总结

### 内部类 vs 枚举 vs Record 全面对比

| 维度 | 成员内部类 | 静态内部类 | 局部内部类 | 匿名内部类 | 枚举 | Record |
|------|-----------|-----------|-----------|-----------|------|--------|
| 版本 | 1.0 | 1.0 | 1.0 | 1.1 | 5 | 16 |
| 类名 | 有 | 有 | 有 | 无 | 有 | 有 |
| 可实例化 | 是 | 是 | 是 | 是（一次） | 否（常量） | 是 |
| 依赖外部实例 | 是 | 否 | 是 | 是 | 不适用 | 不适用 |
| 可变性 | 任意 | 任意 | 任意 | 任意 | 不可变 | 不可变 |
| 可继承 | 是 | 是 | 是 | 是（隐含） | 否 | 否 |
| 主要用途 | 紧密关联逻辑 | 独立辅助类 | 临时逻辑 | 一次性回调 | 固定常量 | 数据载体 |

### 知识体系总览

```text
Java 类定义方式
├── 常规类（class）：通用代码组织
├── 内部类
│   ├── 成员内部类：绑定外部实例（迭代器）
│   ├── 静态内部类：逻辑归类（Builder）
│   ├── 局部内部类：方法内临时结构
│   └── 匿名内部类：一次性实现（优先用 Lambda）
├── 枚举（enum）：固定常量 + 策略模式
├── Record：不可变数据载体
└── 密封类（sealed）：限制继承范围
```

---

## 12. 面试高频考点

### 12.1 四种内部类的核心区别

| 维度 | 关键区别 |
|------|---------|
| 静态性 | 仅有静态内部类不依赖外部类对象 |
| 作用域 | 成员/静态内部类在类体内，局部/匿名在方法体内 |
| 创建方式 | 成员: `outer.new Inner()`，静态: `new Outer.Inner()` |
| 访问外部类 | 静态内部类仅可访问外部静态成员，其他三者可访问所有 |

### 12.2 匿名内部类本质

> 本质是没有类名的局部内部类，创建时就地生成一个继承某类/实现某接口的子类对象。特点：无类名、无构造方法、仅使用一次、必须依托父类/接口。

### 12.3 静态内部类 vs 成员内部类

| 对比 | 静态内部类 | 成员内部类 |
|------|-----------|-----------|
| 依赖外部对象 | 不依赖 | 依赖 |
| 访问外部类 | 仅静态成员 | 所有成员 |
| 定义静态成员 | 可以 | 不可以 |
| 创建方式 | `new Outer.Inner()` | `outer.new Inner()` |
| 内存泄漏风险 | 无 | 有（持有外部引用） |

### 12.4 枚举 `==` vs `equals()`

> 行为完全一致（枚举单例保证），但 `==` 更快且无空指针风险。推荐用 `==`。

### 12.5 Record vs POJO

| 对比 | Record | POJO |
|------|--------|------|
| 代码量 | 一行定义 | 大量样板代码 |
| 可变性 | 不可变（所有 `final`） | 可设 setter |
| 自动生成 | 构造、getter、equals、hashCode、toString | 手动生成 |
| 继承 | 不能继承 | 可继承 |

### 12.6 密封类用途

1. **精确控制继承范围**：通过 `permits` 列出允许的子类
2. **穷举性检查**：与 switch 模式匹配配合，无需 `default` 分支
3. **API 设计**：向调用者明确子类集合，防止外部随意扩展

---

> 🎯 **总结**：内部类解决封装与多继承问题，枚举解决固定常量与策略模式，Record 简化不可变数据载体，密封类精确控制继承层次。掌握这四类特性的适用场景与最佳实践，是 Java 高级开发的基础要求。
