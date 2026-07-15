# Java 内部类知识点完整版

> **定位**：内部类是定义在外部类内部的类，是 Java 实现代码封装、简化开发、解决多继承局限性的核心手段。内部类可直接访问外部类所有成员（包括私有），同时隐藏自身实现细节。

---

## 目录

1. [核心定义与价值](#1-核心定义与价值)
2. [四大分类详解](#2-四大分类详解)
   - [成员内部类](#21-成员内部类)
   - [静态内部类](#22-静态内部类)
   - [局部内部类](#23-局部内部类)
   - [匿名内部类](#24-匿名内部类)
3. [核心注意事项](#3-核心注意事项)
4. [面试高频考点](#4-面试高频考点)
5. [总结](#5-总结)

---

## 1. 核心定义与价值

### 1.1 核心定义

> 内部类（Inner Class）本质是一个类嵌套在另一个类（外部类）内部，可看作外部类的"成员"。

| 特性 | 说明 |
|------|------|
| 访问外部类 | 内部类可直接访问外部类**所有**成员（包括 `private`） |
| 被外部类访问 | 外部类需通过内部类对象才能访问内部类成员 |
| 形象理解 | 内部类就像外部类的"内部助手"，专门为外部类服务 |

### 1.2 核心价值

| 价值 | 说明 |
|------|------|
| **高度封装** | 内部类隐藏在外部类内部，外部无法直接访问，保护实现细节 |
| **便捷访问** | 无需外部类对象就能访问外部类所有成员 |
| **解决多继承局限** | 一个外部类可有多个内部类，每个可继承不同类 → 间接多继承 |
| **简化代码结构** | 仅为另一个类服务时（如监听器、适配器），定义成内部类更紧凑 |

---

## 2. 四大分类详解

### 四大内部类速览

| 类型 | `static` | 定义位置 | 依赖外部类对象 | 访问外部类 | 可定义静态成员 |
|------|:--------:|----------|:------------:|------------|:------------:|
| **成员内部类** | ❌ 无 | 类体内 | ✅ 是 | 所有成员 | ❌ |
| **静态内部类** | ✅ 有 | 类体内 | ❌ 否 | 仅静态成员 | ✅ |
| **局部内部类** | ❌ 无 | 方法/代码块内 | ✅ 是 | 所有成员 | ❌ |
| **匿名内部类** | ❌ 无 | 方法/代码块内 | ✅ 是 | 所有成员 | ❌ |

---

### 2.1 成员内部类

> 无 `static` 修饰，属于外部类的非静态成员，**依赖外部类对象存在**。

```java
// 外部类
class Person {
    private String name = "张三";

    // 成员内部类
    class Heart {
        private String beat = "咚咚咚";

        public void beat() {
            // 直接访问外部类私有属性
            System.out.println(name + "的心脏在" + beat + "跳动");
        }
    }

    public void showHeartBeat() {
        Heart heart = new Heart();  // 外部类内部直接 new
        heart.beat();
    }
}

// 测试
public class Test {
    public static void main(String[] args) {
        Person person = new Person();
        person.showHeartBeat();                        // 方式 1：间接访问

        Person.Heart heart = person.new Heart();       // 方式 2：外部创建
        heart.beat();  // 输出：张三的心脏在咚咚咚跳动
    }
}
```

**核心特性**：

| 特性 | 说明 |
|------|------|
| 创建方式 | 先有外部类对象 → `外部类对象.new 内部类名()` |
| 访问外部类 | ✅ 可直接访问所有成员（包括 `private`） |
| 静态成员 | ❌ 不能定义 `static` 属性/方法/代码块 |
| 命名冲突 | 用 `外部类名.this.成员名` 区分 |

---

### 2.2 静态内部类

> 有 `static` 修饰，属于外部类的静态成员，**不依赖外部类对象**，独立性最强。

```java
class Computer {
    private static String brand = "华为";
    private String model = "MateBook";        // 非静态，静态内部类不可访问

    // 静态内部类
    static class CPU {
        private static String type = "麒麟9000";
        private int core = 8;

        public static void showBrand() {
            // ✅ 只能访问外部类静态成员
            System.out.println("品牌：" + brand + "，CPU：" + type);
            // System.out.println(model);    // ❌ 不能访问非静态成员
        }

        public void showCore() {
            System.out.println("CPU 核心数：" + core);
        }
    }
}

// 测试
public class Test {
    public static void main(String[] args) {
        // 方式 1：直接访问静态成员（无需任何对象）
        Computer.CPU.showBrand();

        // 方式 2：直接创建静态内部类对象（无需外部类对象）
        Computer.CPU cpu = new Computer.CPU();
        cpu.showCore();
    }
}
```

**核心特性**：

| 特性 | 说明 |
|------|------|
| 创建方式 | 直接 `new 外部类名.内部类名()`，无需外部类对象 |
| 访问外部类 | ⚠️ 只能访问外部类的**静态成员** |
| 静态成员 | ✅ 可以自由定义 `static` 属性/方法 |
| 外部访问 | `外部类名.内部类名.静态成员` |

---

### 2.3 局部内部类

> 定义在方法/代码块内部，**仅当前方法内有效**，生命周期与方法一致。

```java
class Calculator {
    private int num1 = 10;

    public int add(int num2) {
        final int sumTemp = 0;  // 被局部内部类访问，必须是 final

        // 局部内部类（仅在 add 方法内有效）
        class CalculateLogic {
            public int compute() {
                return num1 + num2 + sumTemp;  // 访问外部类成员 + 局部变量
            }
        }

        CalculateLogic logic = new CalculateLogic();
        return logic.compute();
    }
}

// 测试
Calculator calculator = new Calculator();
int result = calculator.add(20);  // 输出：30
```

**核心特性**：

| 特性 | 说明 |
|------|------|
| 作用域 | 仅定义它的方法/代码块内有效 |
| 修饰符 | ❌ 不能用 `public`/`private`/`protected`/`static` |
| 访问外部类 | ✅ 可直接访问所有成员 |
| 局部变量限制 | ⚠️ 访问的局部变量必须是 `final` 或 effectively final |

---

### 2.4 匿名内部类

> 没有类名的局部内部类，本质是**"继承了某类/实现了某接口的匿名子类对象"**，仅能使用一次。是开发中最常用的内部类。

**语法格式**：

```java
// 实现接口
new 接口名() {
    @Override
    接口方法() { ... }
};

// 继承类
new 类名() {
    @Override
    父类方法() { ... }
};
```

**实战示例**：

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
        dog.run();

        // 精简写法：创建后直接调用
        new Animal() {
            @Override
            public void eat() { System.out.println("小猫吃鱼"); }
            @Override
            public void run() { System.out.println("小猫慢慢地跑"); }
        }.eat();
    }
}
```

**常见应用场景**：

| 场景 | 示例 |
|------|------|
| 线程创建 | `new Thread(() -> { ... }).start()` |
| 集合排序 | `Collections.sort(list, new Comparator<>() { ... })` |
| 事件监听 | `button.addActionListener(new ActionListener() { ... })` |

**核心特性**：

| 特性 | 说明 |
|------|------|
| 类名 | ❌ 没有类名 |
| 构造方法 | ❌ 不能定义构造方法（可用构造代码块初始化） |
| 使用次数 | ⚠️ 仅能创建**一个**对象 |
| 前提 | 必须继承一个类或实现一个接口 |
| 静态成员 | ❌ 不能定义 |

---

## 3. 核心注意事项

### 3.1 访问修饰符限制

| 内部类类型 | 可用的访问修饰符 | `static` |
|-----------|:---------------:|:--------:|
| 成员内部类 | `public`/`protected`/`private`/默认 | ❌ |
| 静态内部类 | `public`/`protected`/`private`/默认 | ✅ |
| 局部内部类 | ❌ 无 | ❌ |
| 匿名内部类 | ❌ 无 | ❌ |

### 3.2 命名冲突解决

```java
class Outer {
    String name = "外部类";

    class Inner {
        String name = "内部类";

        public void showName() {
            String name = "局部变量";
            System.out.println(name);              // 局部变量（优先级最高）
            System.out.println(this.name);         // 内部类成员
            System.out.println(Outer.this.name);   // 外部类成员
        }
    }
}
```

**优先级**：`局部变量 > 内部类成员 > 外部类成员`

### 3.3 生命周期

| 类型 | 生命周期 |
|------|----------|
| 成员内部类 | 与外部类**对象**一致 |
| 静态内部类 | 与外部**类**一致（类加载时加载） |
| 局部内部类 | 与方法一致（方法结束即销毁） |
| 匿名内部类 | 与方法一致，且仅使用一次 |

### 3.4 序列化注意事项

> 若外部类实现了 `Serializable`，内部类需要单独实现 `Serializable`，否则无法序列化。静态内部类序列化不依赖外部类，可独立实现。

---

## 4. 面试高频考点

### 4.1 四大内部类的区别？

（详见 [四大内部类速览表](#四大内部类速览)）

### 4.2 成员内部类为什么不能定义静态成员？

> 成员内部类依赖外部类**对象**存在，而静态成员是类级别的，不依赖对象。二者生命周期和依赖关系冲突。

### 4.3 匿名内部类的本质是什么？

> 本质：**没有类名的局部内部类 + 继承某类/实现某接口的子类对象**。
>
> 特点：无类名、无构造方法、仅使用一次、必须依托父类/接口。

### 4.4 静态内部类 vs 成员内部类

| 对比维度 | 静态内部类 | 成员内部类 |
|----------|-----------|-----------|
| 依赖外部类对象 | ❌ 不依赖 | ✅ 依赖 |
| 访问外部类 | 仅静态成员 | 所有成员 |
| 定义静态成员 | ✅ 可以 | ❌ 不可以 |
| 创建方式 | `new 外部类.内部类()` | `外部类对象.new 内部类()` |

### 4.5 局部内部类访问局部变量为什么要 `final`？

> 局部变量的生命周期是"方法执行期间"，方法结束后局部变量销毁。而局部内部类对象可能在方法结束后仍存在（如被返回或赋值给外部引用）。为保证变量值不变，避免"变量已销毁但内部类还在用"的矛盾，必须 `final`。
>
> JDK 8+ 可省略 `final`，但变量值不能修改（effectively final）。

---

## 5. 总结

```text
Java 内部类
├── 成员内部类：类体内、无 static、依赖外部类对象
│   └── 场景：紧密关联的逻辑封装（Heart → Person）
├── 静态内部类：类体内、有 static、独立于外部类对象
│   └── 场景：独立于实例的逻辑（CPU → Computer）
├── 局部内部类：方法内、仅方法内有效
│   └── 场景：临时逻辑封装
└── 匿名内部类：无类名、仅一次、依托父类/接口
    └── 场景：监听器、线程、排序（最常用）
```

| 层次 | 要点 |
|------|------|
| **核心价值** | 封装 + 便捷访问 + 间接多继承 + 代码紧凑 |
| **重点掌握** | 四种内部类的定义位置、依赖关系、访问权限、创建方式 |
| **实战关键** | 命名冲突（`Outer.this.x`）、局部变量 final、匿名内部类场景 |
| **面试重点** | 四大区别、static 限制原因、匿名内部类本质、final 原因 |
