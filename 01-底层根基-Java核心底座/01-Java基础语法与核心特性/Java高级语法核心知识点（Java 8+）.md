# Java 高级语法核心知识点（Java 8+）

> **定位**：在基础语法之上，解决复杂开发场景、提升代码效率和可维护性的核心内容。重点围绕面向对象进阶、Lambda 表达式、Stream 流、反射、注解、泛型等模块展开。

---

## 目录

1. [面向对象进阶](#1-面向对象进阶)
2. [Java 8+ 新特性](#2-java-8-新特性)
3. [反射](#3-反射)
4. [注解](#4-注解)
5. [泛型](#5-泛型)
6. [异常处理进阶](#6-异常处理进阶)
7. [其他核心知识点](#7-其他核心知识点)

---

## 1. 面向对象进阶

### 1.1 抽象类（`abstract class`）

> 无法实例化的类，用于定义共性模板。包含抽象方法（无方法体）和非抽象方法，子类必须重写所有抽象方法（除非子类也是抽象类）。

```java
// 抽象类
abstract class Animal {
    // 非抽象方法（有方法体）
    public void eat() {
        System.out.println("动物需要进食");
    }

    // 抽象方法（无方法体，必须用 abstract 修饰）
    public abstract void move();
}

// 子类继承抽象类，必须重写抽象方法
class Dog extends Animal {
    @Override
    public void move() {
        System.out.println("狗用四肢奔跑");
    }
}

// ❌ 错误：抽象类无法实例化
// Animal animal = new Animal();
```

**核心要点**：

| 要点 | 说明 |
|------|------|
| 不能实例化 | `new Animal()` 编译报错 |
| 可有构造方法 | 供子类通过 `super()` 调用 |
| 抽象方法 | 必须在子类中重写 |
| 普通成员 | 可以有普通属性和方法 |

---

### 1.2 接口（`interface`）

> 一种特殊的"抽象类"，仅定义方法规范。Java 8 后允许定义 **默认方法**（`default`）和 **静态方法**（`static`）。

```java
// 接口（用 interface 修饰）
interface Flyable {
    // 抽象方法（默认 public abstract，可省略）
    void fly();

    // Java 8+ 默认方法（有方法体，子类可重写，也可直接使用）
    default void stop() {
        System.out.println("停止飞行");
    }

    // Java 8+ 静态方法（有方法体，只能通过接口名调用）
    static void showRule() {
        System.out.println("飞行需遵守安全规则");
    }
}

// 类实现接口（implements），必须重写抽象方法
class Bird implements Flyable {
    @Override
    public void fly() {
        System.out.println("鸟扇动翅膀飞行");
    }
}

// 调用静态方法
Flyable.showRule();
```

**核心要点**：

| 要点 | 说明 |
|------|------|
| 不能实例化 | 接口没有构造方法 |
| 多实现 | 类可以实现多个接口，解决 Java 单继承限制 |
| 成员变量 | 默认 `public static final`（常量） |
| 默认方法/静态方法 | Java 8 重要更新，提升接口灵活性 |

### 抽象类 vs 接口

| 维度 | 抽象类 | 接口 |
|------|--------|------|
| 关键字 | `abstract class` | `interface` |
| 构造方法 | ✅ 有 | ❌ 无 |
| 多继承/实现 | 单继承 | 多实现 |
| 成员变量 | 无限制 | 仅 `public static final` |
| 方法 | 可有方法体 | Java 8+ 支持 `default`/`static` |
| 设计意图 | "是什么"（is-a） | "能做什么"（can-do） |

---

### 1.3 多态（Polymorphism）

> 面向对象三大特性之一：**同一方法调用，不同对象有不同实现**。核心是"父类引用指向子类对象"。

**三个必要条件**：继承 → 重写 → 父类引用指向子类对象

```java
// 父类
class Animal {
    public void shout() {
        System.out.println("动物发出叫声");
    }
}

// 子类 1
class Cat extends Animal {
    @Override
    public void shout() {
        System.out.println("猫喵喵叫");
    }
}

// 子类 2
class Dog extends Animal {
    @Override
    public void shout() {
        System.out.println("狗汪汪叫");
    }
}

// 测试多态
public class Test {
    public static void main(String[] args) {
        Animal animal1 = new Cat();   // 父类引用指向 Cat 对象
        Animal animal2 = new Dog();   // 父类引用指向 Dog 对象

        animal1.shout();  // 输出：猫喵喵叫
        animal2.shout();  // 输出：狗汪汪叫
    }
}
```

**核心要点**：

| 要点 | 说明 |
|------|------|
| 实现机制 | 通过"重写"实现 |
| 访问限制 | 父类引用只能调用父类中定义的方法 |
| 子类特有方法 | 需要强制类型转换 |
| 核心价值 | 提升代码扩展性和灵活性 |

---

### 1.4 封装进阶

#### 访问修饰符

| 修饰符 | 本类 | 同包 | 子类（不同包） | 其他包 |
|--------|:----:|:----:|:------------:|:------:|
| `public` | ✅ | ✅ | ✅ | ✅ |
| `protected` | ✅ | ✅ | ✅ | ❌ |
| 默认（无修饰） | ✅ | ✅ | ❌ | ❌ |
| `private` | ✅ | ❌ | ❌ | ❌ |

#### 单例模式（封装的典型应用）

> 确保一个类只有一个实例，提供全局唯一的访问方式。

**饿汉式**（线程安全，类加载时初始化）：

```java
class SingletonHungry {
    // 私有静态实例（类加载时创建）
    private static final SingletonHungry instance = new SingletonHungry();

    // 私有构造方法（禁止外部实例化）
    private SingletonHungry() {}

    // 公共静态方法，返回实例
    public static SingletonHungry getInstance() {
        return instance;
    }
}
```

**懒汉式**（线程安全，延迟初始化，双重检查锁）：

```java
class SingletonLazy {
    // volatile 防止指令重排
    private static volatile SingletonLazy instance;

    // 私有构造方法
    private SingletonLazy() {}

    // 双重检查锁，确保线程安全且高效
    public static SingletonLazy getInstance() {
        if (instance == null) {
            synchronized (SingletonLazy.class) {
                if (instance == null) {
                    instance = new SingletonLazy();
                }
            }
        }
        return instance;
    }
}
```

| 方式 | 线程安全 | 延迟加载 | 适用场景 |
|------|:--------:|:--------:|----------|
| 饿汉式 | ✅ | ❌ | 实例创建开销小 |
| 懒汉式（DCL） | ✅ | ✅ | 实例创建开销大 |

---

## 2. Java 8+ 新特性

### 2.1 Lambda 表达式

> 函数式编程的核心，用于简化匿名内部类写法。适用于**函数式接口**（只有一个抽象方法的接口）。

**语法格式**：`(参数列表) -> { 方法体 }`

| 省略规则 | 说明 |
|----------|------|
| 参数无括号 | 单参数可省略 `()` |
| 方法体无括号 | 单条语句可省略 `{}` 和 `return` |

```java
// 函数式接口
interface Calculator {
    int calculate(int a, int b);
}

public class TestLambda {
    public static void main(String[] args) {
        // 传统匿名内部类
        Calculator add1 = new Calculator() {
            @Override
            public int calculate(int a, int b) {
                return a + b;
            }
        };

        // Lambda 简化（单条语句，省略 {} 和 return）
        Calculator add2 = (a, b) -> a + b;

        // Lambda（多条语句，需要 {} 和 return）
        Calculator sub = (a, b) -> {
            System.out.println("执行减法");
            return a - b;
        };

        System.out.println(add2.calculate(3, 5));   // 输出：8
        System.out.println(sub.calculate(10, 4));    // 输出：6
    }
}
```

**常用函数式接口**（`java.util.function` 包）：

| 接口 | 方法 | 用途 |
|------|------|------|
| `Consumer<T>` | `void accept(T t)` | 消费数据 |
| `Supplier<T>` | `T get()` | 提供数据 |
| `Predicate<T>` | `boolean test(T t)` | 判断/过滤 |
| `Function<T, R>` | `R apply(T t)` | 转换/映射 |

---

### 2.2 Stream 流

> 处理集合/数组的高效工具，通过"流式操作"实现过滤、映射、排序、聚合等功能。

**核心机制**：中间操作（返回 `Stream`）+ 终止操作（返回最终结果），惰性求值。

```java
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestStream {
    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);

        // 链式操作：过滤偶数 → 乘以 2 → 排序 → 收集为 List
        List<Integer> result = list.stream()
                .filter(num -> num % 2 == 0)   // 中间操作：过滤
                .map(num -> num * 2)           // 中间操作：映射
                .sorted()                      // 中间操作：排序
                .collect(Collectors.toList()); // 终止操作：收集

        System.out.println(result);  // 输出：[4, 8, 12, 16]

        // 统计与求和
        long count = list.stream().filter(num -> num > 5).count();
        int sum = list.stream()
                .filter(num -> num % 2 == 1)
                .mapToInt(Integer::intValue)
                .sum();

        System.out.println(count);  // 输出：3
        System.out.println(sum);    // 输出：16
    }
}
```

**常用操作速查**：

| 类型 | 方法 | 说明 |
|------|------|------|
| 中间操作 | `filter` | 过滤 |
| 中间操作 | `map` | 映射转换 |
| 中间操作 | `sorted` | 排序 |
| 中间操作 | `distinct` | 去重 |
| 中间操作 | `limit` / `skip` | 限制/跳过 |
| 终止操作 | `collect` | 收集结果 |
| 终止操作 | `forEach` | 遍历 |
| 终止操作 | `count` | 计数 |
| 终止操作 | `reduce` | 归约聚合 |

---

### 2.3 方法引用

> Lambda 表达式的进一步简化，当 Lambda 方法体只是调用已存在的方法时使用。

**格式**：`类名::方法名` 或 `对象::方法名`

```java
List<String> list = Arrays.asList("apple", "banana", "orange");

// Lambda 写法
List<String> upper1 = list.stream()
        .map(s -> s.toUpperCase())
        .collect(Collectors.toList());

// 方法引用（简化）
List<String> upper2 = list.stream()
        .map(String::toUpperCase)
        .collect(Collectors.toList());
```

**常用引用类型**：

| 类型 | 格式 | 示例 |
|------|------|------|
| 对象实例方法 | `对象::实例方法` | `System.out::println` |
| 类静态方法 | `类名::静态方法` | `Math::max` |
| 类实例方法 | `类名::实例方法` | `String::equals` |
| 构造器引用 | `类名::new` | `ArrayList::new` |

---

### 2.4 Optional（解决空指针异常）

> 用于包装可能为 `null` 的对象，避免 `NullPointerException`，提供一系列安全操作方法。

```java
import java.util.Optional;

public class TestOptional {
    public static void main(String[] args) {
        // 1. 创建 Optional 对象
        Optional<String> optional1 = Optional.of("hello");       // 不允许 null
        Optional<String> optional2 = Optional.ofNullable(null);  // 允许 null
        Optional<String> optional3 = Optional.empty();           // 空 Optional

        // 2. 安全获取值（推荐）
        String value1 = optional1.orElse("默认值");
        String value2 = optional2.orElseGet(() -> "默认值2");

        // 3. 过滤和映射
        optional1.filter(s -> s.length() > 3)
                .ifPresent(s -> System.out.println(s));

        // 4. 链式安全调用
        String str = null;
        Optional.ofNullable(str)
                .ifPresent(s -> System.out.println(s.length()));
    }
}
```

**常用方法**：

| 方法 | 说明 |
|------|------|
| `orElse(T other)` | 有值返回值，无值返回默认值 |
| `orElseGet(Supplier)` | 无值时执行 Lambda 获取默认值 |
| `ifPresent(Consumer)` | 有值则执行操作 |
| `filter(Predicate)` | 过滤，返回过滤后的 Optional |
| `map(Function)` | 映射转换 |

> ⚠️ 不推荐用 `get()` 方法（无值时抛异常），优先使用 `orElse` / `ifPresent`。

---

## 3. 反射

### 3.1 反射概述

> Java 核心特性之一：允许程序在**运行时**动态获取类信息，并动态调用属性和方法。是 Spring、MyBatis 等框架的底层实现原理。

**获取 Class 对象的 3 种方式**：

```java
// 方式 1：类名.class（编译期确定，最安全）
Class<?> clazz1 = User.class;

// 方式 2：对象.getClass()（运行时确定）
User user = new User();
Class<?> clazz2 = user.getClass();

// 方式 3：Class.forName("全类名")（动态加载，最灵活）
Class<?> clazz3 = Class.forName("com.demo.entity.User");
```

| 方式 | 时机 | 特点 |
|------|------|------|
| `类名.class` | 编译期 | 最安全 |
| `对象.getClass()` | 运行时 | 需实例 |
| `Class.forName()` | 运行时 | 最灵活，动态加载 |

### 3.2 反射核心操作

```java
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class User {
    private String username;
    public Integer age;

    public User() {}
    public User(String username, Integer age) {
        this.username = username;
        this.age = age;
    }

    public void show() {
        System.out.println("username: " + username + ", age: " + age);
    }

    private void sayHello(String name) {
        System.out.println("Hello, " + name);
    }
}

public class TestReflection {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = User.class;

        // 1. 获取构造器，创建对象
        Constructor<?> constructor1 = clazz.getConstructor();
        User user1 = (User) constructor1.newInstance();

        Constructor<?> constructor2 = clazz.getConstructor(String.class, Integer.class);
        User user2 = (User) constructor2.newInstance("zhangsan", 20);

        // 2. 获取属性并操作
        Field ageField = clazz.getField("age");     // 公共属性
        ageField.set(user2, 22);
        System.out.println(ageField.get(user2));    // 输出：22

        Field usernameField = clazz.getDeclaredField("username");  // 私有属性
        usernameField.setAccessible(true);           // 打破封装
        usernameField.set(user2, "lisi");
        System.out.println(usernameField.get(user2));  // 输出：lisi

        // 3. 获取方法并调用
        Method showMethod = clazz.getMethod("show");           // 公共方法
        showMethod.invoke(user2);  // 输出：username: lisi, age: 22

        Method sayHelloMethod = clazz.getDeclaredMethod("sayHello", String.class);  // 私有方法
        sayHelloMethod.setAccessible(true);
        sayHelloMethod.invoke(user2, "Java");  // 输出：Hello, Java
    }
}
```

**核心要点**：

| 要点 | 说明 |
|------|------|
| 打破封装 | `setAccessible(true)` 可访问私有成员 |
| 性能损耗 | 反射操作比直接调用慢，避免频繁使用 |
| 框架核心 | 理解反射才能掌握 Spring/MyBatis 原理 |

---

## 4. 注解（Annotation）

### 4.1 注解概述

> Java 5 引入的特性，用于给类/方法/属性添加"元数据"。本身不影响代码执行，但可通过反射获取注解信息实现自定义逻辑。

**常用内置注解**：

| 注解 | 作用 |
|------|------|
| `@Override` | 标识方法重写，编译器校验 |
| `@Deprecated` | 标识已过时，编译警告 |
| `@SuppressWarnings("all")` | 抑制编译器警告 |
| `@FunctionalInterface` | 标识函数式接口 |

### 4.2 自定义注解

> 通过 `@interface` 定义，搭配元注解控制使用范围和生命周期。

**元注解**：

| 元注解 | 作用 | 常用值 |
|--------|------|--------|
| `@Target` | 控制使用范围 | `TYPE`、`METHOD`、`FIELD` 等 |
| `@Retention` | 控制生命周期 | `SOURCE`、`CLASS`、`RUNTIME` |

```java
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// 自定义注解
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface MyAnnotation {
    String value() default "默认值";
    int age() default 18;
}

// 使用自定义注解
class Student {
    @MyAnnotation(value = "张三", age = 20)
    private String name;

    @MyAnnotation("学习Java")
    public void study() {
        System.out.println("正在学习Java");
    }
}

// 通过反射获取注解信息
public class TestAnnotation {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Student.class;

        // 获取属性上的注解
        Field nameField = clazz.getDeclaredField("name");
        MyAnnotation a1 = nameField.getAnnotation(MyAnnotation.class);
        System.out.println(a1.value());  // 输出：张三
        System.out.println(a1.age());    // 输出：20

        // 获取方法上的注解
        Method studyMethod = clazz.getMethod("study");
        MyAnnotation a2 = studyMethod.getAnnotation(MyAnnotation.class);
        System.out.println(a2.value());  // 输出：学习Java
    }
}
```

**核心要点**：

| 要点 | 说明 |
|------|------|
| 元注解必备 | `@Target` 和 `@Retention` 是自定义注解的基础 |
| 属性类型 | 只能是基本类型、String、枚举、注解、数组 |
| 获取方式 | 通过反射获取注解，实现参数校验、日志等功能 |

---

## 5. 泛型（Generic）

> 详见 [Java泛型核心知识点（新手易懂版）.md](./Java泛型核心知识点（新手易懂版）.md)

### 快速概览

```java
// 1. 泛型类
class GenericClass<T> {
    private T data;
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

// 2. 泛型方法
class GenericMethod {
    public <T> T getValue(T value) { return value; }
}

// 3. 泛型接口
interface GenericInterface<T> {
    T getResult();
}

// 使用
GenericClass<String> strClass = new GenericClass<>();
strClass.setData("Java");
String str = strClass.getData();  // 无需类型转换
```

### 泛型通配符

| 通配符 | 含义 | 读写规则 |
|--------|------|----------|
| `?` | 任意类型 | 只读不可写 |
| `? extends T` | T 或 T 的子类 | 只读不写 |
| `? super T` | T 或 T 的父类 | 可写不可读 |

```java
// 无界通配符
public static void printList(List<?> list) { ... }

// 上界通配符：只接收 Number 及其子类
public static void printNumberList(List<? extends Number> list) { ... }

// 下界通配符：只接收 Integer 及其父类
public static void addNumber(List<? super Integer> list) {
    list.add(200);  // 可添加 Integer 及其子类
}
```

---

## 6. 异常处理进阶

### 6.1 异常体系

```text
Throwable
├── Error（系统级错误，不可恢复）
└── Exception
    ├── RuntimeException（Unchecked，运行时异常）
    │   ├── NullPointerException
    │   ├── ArrayIndexOutOfBoundsException
    │   └── IllegalArgumentException
    └── 其他 Exception（Checked，编译期异常）
        ├── IOException
        ├── SQLException
        └── FileNotFoundException
```

| 类型 | 继承关系 | 处理要求 | 常见例子 |
|------|----------|----------|----------|
| Checked | `Exception`（非 `RuntimeException`） | 编译期必须 `try-catch` 或 `throws` | `IOException`、`SQLException` |
| Unchecked | `RuntimeException` | 可选择处理 | `NullPointerException`、`ArrayIndexOutOfBoundsException` |

### 6.2 try-catch-finally + try-with-resources

```java
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TestException {
    // throws：抛出异常，由调用者处理
    public static void readFile(String path) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            // 读取文件操作
        } catch (FileNotFoundException e) {
            System.out.println("文件不存在：" + e.getMessage());
            throw e;  // 重新抛出，让调用者进一步处理
        } finally {
            // finally：无论是否异常，都会执行（用于释放资源）
            if (fis != null) {
                try { fis.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    // Java 7+ try-with-resources（自动释放资源，无需手动 close）
    public static void readFile2(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path)) {
            // 读取文件操作
        }  // fis 自动 close()
    }
}
```

### 6.3 自定义异常

```java
// 自定义运行时异常
class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {}
    public UserNotFoundException(String message) {
        super(message);
    }
}

// 使用自定义异常
public class TestCustomException {
    public static void findUser(Long id) {
        if (id == null || id <= 0) {
            throw new UserNotFoundException("用户ID非法：" + id);
        }
        boolean userExists = false;
        if (!userExists) {
            throw new UserNotFoundException("用户不存在，ID：" + id);
        }
    }

    public static void main(String[] args) {
        try {
            findUser(0);
        } catch (UserNotFoundException e) {
            System.out.println("异常信息：" + e.getMessage());
        }
    }
}
```

**核心要点**：

| 要点 | 说明 |
|------|------|
| 继承选择 | Checked → `Exception`；Unchecked → `RuntimeException` |
| 异常信息 | 要清晰明确，便于定位问题 |
| 避免吞噬 | 不要空 `catch` 块，要么处理，要么抛出 |

---

## 7. 其他核心知识点

### 7.1 多线程基础

```java
// 方式 1：继承 Thread 类
class MyThread extends Thread {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Thread1: " + i);
            try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}

// 方式 2：实现 Runnable 接口（推荐，避免单继承限制）
class MyRunnable implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Thread2: " + i);
            try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}

// 启动线程
new MyThread().start();
new Thread(new MyRunnable()).start();

// Lambda 简化（Runnable 是函数式接口）
new Thread(() -> {
    for (int i = 0; i < 5; i++) {
        System.out.println("Thread3: " + i);
        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
    }
}).start();
```

### 7.2 枚举（Enum）

> Java 5 引入，用于定义固定数量的常量。枚举类是 `final` 类，不能继承，可包含属性和方法。

```java
enum Gender {
    // 枚举常量（必须放在最前面）
    MALE("男"), FEMALE("女");

    // 枚举属性
    private String desc;

    // 枚举构造方法（必须是 private）
    private Gender(String desc) {
        this.desc = desc;
    }

    // 枚举方法
    public String getDesc() {
        return desc;
    }
}

// 使用枚举
Gender gender = Gender.MALE;
System.out.println(gender);            // 输出：MALE
System.out.println(gender.getDesc());  // 输出：男

// 遍历所有枚举常量
for (Gender g : Gender.values()) {
    System.out.println(g + ": " + g.getDesc());
}
```

---

## 知识点体系总览

```text
Java 高级语法
├── 面向对象进阶
│   ├── 抽象类 vs 接口
│   ├── 多态（继承 + 重写 + 父类引用）
│   ├── 封装（访问修饰符 + 单例模式）
├── Java 8+ 新特性
│   ├── Lambda 表达式
│   ├── Stream 流（中间操作 + 终止操作）
│   ├── 方法引用（:: 语法）
│   └── Optional（防空指针）
├── 反射（运行时动态操作类）
├── 注解（元数据 + 反射获取）
├── 泛型（类型参数化 + 通配符）
├── 异常处理（Checked/Unchecked + try-with-resources）
└── 其他
    ├── 多线程基础（Thread / Runnable）
    └── 枚举（固定常量集合）
```
