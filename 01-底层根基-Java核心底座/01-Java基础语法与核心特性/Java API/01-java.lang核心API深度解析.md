# 01 java.lang 核心 API 深度解析

> java.lang 是唯一自动导入的包——Object 是所有类的根，String 是使用频率最高的类，理解它们的契约与内存模型是 Java 程序正确性的基石

---

## 📚 目录

1. [Object 类深度剖析](#1-object-类深度剖析)
2. [String 不可变性与内存模型](#2-string-不可变性与内存模型)
3. [包装类与自动装箱](#3-包装类与自动装箱)
4. [System 与 Runtime](#4-system-与-runtime)
5. [Math 与 StrictMath](#5-math-与-strictmath)
6. [动手练习](#6-动手练习)

---

## 1. Object 类深度剖析

> Object 是一切类的根。`equals`、`hashCode`、`toString`、`clone` 这四大方法是所有 Java 对象的通用契约，面试中出现频率最高的就是 `equals/hashCode` 的约定规则。

### 1.1 Object 类全景

Object 是 Java 类层次结构的顶层父类，在 `java.lang` 包中定义。所有类（包括数组）都直接或间接继承 Object。

| 方法 | 用途 | 是否 native | 关键点 |
|------|------|-------------|--------|
| `equals(Object obj)` | 判断对象逻辑相等 | 否 | 默认 `==`，需重写 |
| `hashCode()` | 返回对象的哈希码 | 是（默认） | 与 `equals` 一致 |
| `toString()` | 返回字符串表示 | 否 | 默认 `类名@哈希十六进制` |
| `clone()` | 创建并返回对象的副本 | 是（protected） | 需实现 `Cloneable` |
| `finalize()` | GC 前调用（已废弃） | 否 | Java 9 起 deprecated |
| `getClass()` | 返回运行时 Class 对象 | 是（final） | 不可重写 |
| `notify()` / `notifyAll()` / `wait()` | 线程间协作 | 是（final） | 需在同步块中调用 |

### 1.2 equals 方法 —— 正确重写的六大纪律

由 Java 语言规范（JLS）定义的 `equals` 契约包含 **自反性、对称性、传递性、一致性** 四个特性，外加 **非空性与参数类型检查**。

```java
// 一个符合规范的标准 equals 实现模板
@Override
public boolean equals(Object o) {
    // 1. 自反性：同一个对象直接返回 true
    if (this == o) return true;
    // 2. 非空性 + 类型检查：null 返回 false；类型不匹配返回 false
    if (o == null || getClass() != o.getClass()) return false;
    // 3. 转型后逐字段比较
    Person person = (Person) o;
    return age == person.age
            && Objects.equals(name, person.name)
            && Objects.equals(email, person.email);
}
```

#### 四大特性详解

| 特性 | 规则说明 | 违反后果 |
|------|---------|---------|
| **自反性** | `x.equals(x)` 必须为 `true` | 集合 `contains` 判断自身时出错 |
| **对称性** | `x.equals(y)` 与 `y.equals(x)` 结果一致 | **最常见 bug**：子类用 `instanceof` 而父类用 `getClass` 时恶性循环 |
| **传递性** | 若 `x.equals(y)` 且 `y.equals(z)` 则 `x.equals(z)` | 集合中元素行为不一致 |
| **一致性** | 未修改的对象的多次 `equals` 调用结果不变 | 依赖可变字段会导致集合查找不可预测 |

> 💡 **关于 instanceof 还是 getClass**
>
> - `getClass()` 校验：更严格，子类对象与父类对象永不等价 —— 符合 **对称性**
> - `instanceof` 校验：允许子类与父类比较，但容易触发 **对称性** 违规
> - **推荐做法**：若类被设计为可被继承（如抽象类中的逻辑相等），父类用 `instanceof` 并允许子类扩展；若类被设计为 `final`（如 `Integer`、`String`），用 `getClass()` 更安全

> 🎯 **核心要点**：重写 `equals` 必须同时重写 `hashCode`，否则该类在 `HashMap` / `HashSet` / `HashTable` 中行为异常。

### 1.3 hashCode 方法 —— 哈希碰撞与分布

哈希码是对象的"数字指纹"，主要用在哈希表集合（HashMap、HashSet、ConcurrentHashMap）中。

#### 与 equals 的契约

```text
1. 若 x.equals(y) 为 true, 则 x.hashCode() == y.hashCode()   → 必须遵守
2. 若 x.equals(y) 为 false, 则 x.hashCode() 与 y.hashCode() 可以相同（但应尽量避免）
3. 同一对象多次调用 hashCode() 返回相同整数（前提是 equals 所用字段未变）
```

#### JDK Objects.hash() 工具方法

```java
@Override
public int hashCode() {
    return Objects.hash(name, age, email);
}
```

`Objects.hash(...)` 内部调用 `Arrays.hashCode(Object[])`，使用 **31 作为乘数**（质数 + 可被 JIT 优化为 `(i << 5) - i`）。

#### 自定义 hashCode 的选型

| 方案 | 碰撞概率 | 性能 | 适用场景 |
|------|---------|------|---------|
| `Objects.hash(f1, f2, ...)` | 低 | 中等（创建数组） | 大多数场景，推荐 |
| `return Objects.hashCode(name) * 31 + age` | 低 | 高（无数组开销） | 热路径优化 |
| **Lombok `@EqualsAndHashCode`** | 低 | 中等 | 项目已用 Lombok |

> ⚠️ **不要在 hashCode 中使用随机值或线程敏感字段**，否则违反一致性。

### 1.4 toString 最佳实践

`toString()` 的通用约定是："返回对象的文本表示，应简洁但信息丰富"。

```java
// 不理想的默认形式
getClass().getName() + "@" + Integer.toHexString(hashCode())
// 输出: com.example.User@7f31245a

// 推荐格式：类名 + 关键字段
@Override
public String toString() {
    return "User{name='" + name + "', age=" + age + "}";
}
```

#### 工具方法推荐

| 工具 | 示例 | 说明 |
|------|------|------|
| `Objects.toString(obj, "null")` | `Objects.toString(name, "N/A")` | null 安全的字段转字符串 |
| `String.format()` | `String.format("User{name=%s}", name)` | 格式化风格 |
| Lombok `@ToString` | `@ToString(exclude = "password")` | 自动生成，排除敏感字段 |

#### 实际调试效果

```text
// 有 toString 时的日志输出
User{name='Alice', age=28}
// 没有 toString
com.example.User@7f31245a     ← 毫无调试价值
```

### 1.5 clone 方法 —— 浅拷贝 vs 深拷贝

`clone()` 是 Object 中声明为 `protected` 的 native 方法。要使用它，类必须实现 `Cloneable` 接口（标记接口），否则调用时抛出 `CloneNotSupportedException`。

#### 浅拷贝

```java
public class Address implements Cloneable {
    private String city;
    // getter / setter ...
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();  // 默认浅拷贝
    }
}

public class Person implements Cloneable {
    private String name;
    private Address address;  // 引用类型字段

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();  // address 的引用被拷贝，指向同一个 Address 对象
    }
}
```

#### 深拷贝实现

```java
@Override
protected Object clone() throws CloneNotSupportedException {
    Person cloned = (Person) super.clone();
    cloned.address = (Address) address.clone();  // 递归 clone 引用类型
    return cloned;
}
```

#### 深浅拷贝对比

| 特性 | 浅拷贝 | 深拷贝 |
|------|-------|-------|
| 基本类型字段 | 复制值 | 复制值 |
| 引用类型字段 | 复制引用（共享对象） | 创建新对象 |
| 性能 | 高 | 低（递归复制） |
| 默认实现 | `super.clone()` | 需要手动实现 |
| 修改子对象是否影响原对象 | 影响 | 不影响 |

> 💡 **生产实践中推荐用拷贝工厂或序列化代替 clone()**
>
> ```java
> // 推荐：拷贝构造函数
> public Person(Person other) {
>     this.name = other.name;
>     this.address = new Address(other.address);
> }
>
> // 或使用序列化实现深拷贝（需所有对象 Serializable）
> public Person deepCopyViaSerialization() {
>     try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
>          ObjectOutputStream oos = new ObjectOutputStream(bos)) {
>         oos.writeObject(this);
>         try (ObjectInputStream ois = new ObjectInputStream(
>                 new ByteArrayInputStream(bos.toByteArray()))) {
>             return (Person) ois.readObject();
>         }
>     } catch (IOException | ClassNotFoundException e) {
>         throw new RuntimeException(e);
>     }
> }
> ```

### 1.6 getClass 方法

`getClass()` 是 `final` 方法，返回运行时对象的 `Class<?>` 对象。

```java
Object obj = "Hello";
System.out.println(obj.getClass());                // class java.lang.String
System.out.println(obj.getClass().getName());      // java.lang.String
System.out.println(obj.getClass().getSimpleName());// String

// 反射创建实例
Class<?> clazz = Class.forName("java.lang.StringBuilder");
StringBuilder sb = (StringBuilder) clazz.getDeclaredConstructor().newInstance();
```

### 1.7 finalize 方法 —— 为什么被废弃

`finalize()` 是 Object 的 protected 方法，在 GC 回收对象前被调用，最初设计用于释放非 Java 资源（如 native 句柄）。但它在 Java 9 被标记为 `@Deprecated`，Java 18 已标记为 `forRemoval=true`。

#### 废弃原因

| 问题 | 说明 |
|------|------|
| **执行时机不可预测** | 无法保证 finalize 何时执行，甚至可能永不执行 |
| **性能开销巨大** | 对象需要至少两次 GC 循环才能回收（finalizable 队列处理） |
| **异常被忽视** | finalize 抛出的异常被忽略，不中断线程 |
| **线程安全问题** | 任何线程都可访问 finalizable 对象 |
| **存在更好的替代** | `try-with-resources`、`Cleaner`（Java 9+）、`AutoCloseable` |

```java
// ❌ 已废弃的方式
@Override
protected void finalize() throws Throwable {
    try {
        resource.close();  // 不保证执行
    } finally {
        super.finalize();
    }
}

// ✅ 推荐方式：try-with-resources
try (FileInputStream in = new FileInputStream("data.txt")) {
    // 处理文件 ...
} // 自动关闭，确定及时

// ✅ 或使用 Cleaner（Java 9+）
public class Resource implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();
    private final Cleaner.Cleanable cleanable;

    public Resource() {
        this.cleanable = CLEANER.register(this, () -> {
            // native 资源清理逻辑
            System.out.println("Cleaner 已清理资源");
        });
    }

    @Override
    public void close() {
        cleanable.clean();
    }
}
```

> 🎯 **核心要点**：永远不要依赖 `finalize()` 来关闭资源。使用 `try-with-resources`（`AutoCloseable`）或 `Cleaner`（Java 9+）来保证资源确定性释放。

---

## 2. String 不可变性与内存模型

> String 是 Java 中使用频率最高的类。它的不可变性设计深刻影响了安全性、哈希缓存与字符串池机制。理解 String 的内存模型是区分"会用"和"懂原理"的分水岭。

### 2.1 String 不可变性的设计原因

String 在 Java 中被设计为 `final` 且其字符数组也被声明为 `private final`，一旦创建就不可改变。

#### 六大设计动机

| 动机 | 说明 | 实际影响 |
|------|------|---------|
| **字符串池 (String Pool)** | 常量池复用共享对象，不可变才能安全共享 | 节省大量内存 |
| **哈希缓存** | String 的 `hashCode` 只在首次计算后缓存，若可变则哈希失效 | HashMap 的 String 键性能极佳 |
| **安全性** | 类加载器、网络连接、文件路径等场景大量使用 String 参数 | 防篡改，防止绕过安全检查 |
| **线程安全** | 不可变对象天然线程安全，无需同步 | 多线程下无需额外锁 |
| **Class 对象** | 类名、方法名以 String 表示，不可变保证反射安全 | JVM 内部稳定可靠 |
| **Set 元素 / Map 键** | 不可变对象放入集合后不会因意外修改而破坏集合 | 集合行为可预测 |

#### "修改" String 的真实行为

```java
String s = "hello";
String t = s.toUpperCase();  // 产生新对象 "HELLO"
System.out.println(s);       // 输出 "hello" —— 原对象不变

// 所有看似"修改"的方法都返回新字符串
// concat(), replace(), substring(), trim(), toLowerCase(), toUpperCase() ...
```

### 2.2 String Pool 与 intern() 机制

#### 字符串常量池（String Pool）

String Pool 是 JVM 在 **堆（Heap）的元空间（Java 7 起在堆中；Java 6 及之前在方法区/永久代）** 中维护的一个字符串实例池。

```java
String a = "hello";                // 字面量 → 池中创建
String b = "hello";                // 从池中直接引用同一对象
System.out.println(a == b);        // true —— 同一引用

String c = new String("hello");    // 堆中新对象（即使池中已有）
System.out.println(a == c);        // false —— 堆对象 vs 池对象
```

#### intern() 方法的工作机制

```java
String s1 = new String("hello");
String s2 = s1.intern();  // 将 s1 入池（若池中已有则返回池中对象）
String s3 = "hello";

System.out.println(s1 == s2);  // false
System.out.println(s2 == s3);  // true  —— intern 返回池中对象
```

| JDK 版本 | String Pool 位置 | 影响 |
|----------|-----------------|------|
| Java 6 | 永久代（方法区），固定大小 | -XX:MaxPermSize 限制；池容量受限 |
| Java 7 | **堆**，可动态伸缩 | 字符串可被 GC 回收；极大降低 OOM 风险 |
| Java 8+ | 堆，元空间替代永久代 | 元空间使用本地内存，池仍在堆中 |

#### intern() 的使用建议

> ⚠️ **大量使用 intern() 需要仔细评估**。对重复字符串做 intern 可以节省大量内存，但字符串池的存储和维护本身消耗一定性能。

```java
// 场景：亿级日志中的用户 ID 去重
// 如果大量重复的 userID 每次都 new String()，内存会爆炸
String userId = rawLine.substring(0, 8).intern();
// 但副作用：String Pool 需要维护这些引用，且 intern 操作涉及 native 调用
```

### 2.3 new String("abc") 创建了几个对象

这是经典面试题，答案取决于常量池中是否已存在 "abc"。

#### 情况一：常量池中已有 "abc"

```java
// 场景：之前有代码执行过 String s = "abc";
String s = new String("abc");

// 创建的对象：1 个
// - 堆中：new String() 对象（引用了池中的 char[]）
// - 池中：已存在，不重复创建
// 所以总共创建了 1 个堆对象
```

#### 情况二：常量池中没有 "abc"

```java
String s = new String("abc");

// 创建的对象：2 个
// 1. 编译期常量折叠或类加载时在 String Pool 中放入 "abc"
// 2. 运行时 new String() 在堆中创建新对象
// 所以总共创建了 2 个对象（池中 + 堆中）
```

> 💡 **注意**：如果字符串由 `new String("ab" + "c")` 在编译期常量折叠后生成的，行为同上。如果 `new String("a" + new String("b"))` 则涉及 StringBuilder，情况更复杂。

### 2.4 JDK 9+ 紧凑字符串（Compact Strings）

Java 9 引入 **Compact Strings**（JEP 254），将 String 的底层存储从 `char[]`（每个 char 2 字节，UTF-16）改为 **`byte[]` + 编码标记**。

```java
// Java 8 及之前
private final char value[];     // 每个字符 2 字节

// Java 9+
private final byte[] value;     // 每个字符 1 字节（Latin-1）或 2 字节（UTF-16）
private final byte coder;       // 编码标记：0 = LATIN1, 1 = UTF16
```

| 特性 | Java 8 (char[]) | Java 9+ (byte[] + coder) |
|------|----------------|--------------------------|
| 拉丁字符（英文、数字） | 每个字符 2 字节 | 每个字符 **1 字节** |
| CJK 字符（中文、日文） | 每个字符 2 字节 | 每个字符 **2 字节**（同前） |
| 内存占用 | 基础 | 纯英文场景降低 **50%** |
| 兼容性 | 全部使用 char | 需要编码检测，API 100% 向上兼容 |

```java
// Java 9+ 中 String 长度计算方式
public int length() {
    return value.length >> coder();  // coder=1 时，byte[] 长度除以 2
}
```

> 🎯 **核心要点**：Java 9+ 的 Compact Strings 让英文为主的字符串内存占用降低一半，且对开发者完全透明。日常开发无需主动适配，但面试和性能分析场景要知道底层变化。

### 2.5 == vs equals — 彻底理解

| 比较方式 | 比较内容 | 何时用 |
|----------|---------|--------|
| `==` | **引用地址**（是否同一对象） | 判断两个引用是否指向堆中的同一个实例 |
| `equals()` | **逻辑内容**（由覆盖实现决定） | 判断两个字符串字符序列是否相同 |

```java
String a = "hello";
String b = "hello";
String c = new String("hello");

System.out.println(a == b);          // true  (池中同一对象)
System.out.println(a == c);          // false (池 vs 堆)
System.out.println(a.equals(c));     // true  (内容相同)

// 常用模式：常量在前，避免 NPE
if ("hello".equals(inputString)) {   // 推荐
    // ...
}
if (inputString.equals("hello")) {   // 若 inputString 为 null 则 NPE
    // ...
}
```

### 2.6 StringBuilder vs StringBuffer

#### 性能对比

```java
// 循环拼接字符串时性能差异巨大
String s = "";
for (int i = 0; i < 10000; i++) {
    s += i;        // ❌ 每次循环创建多个临时对象：Integer → String → StringBuilder → String
}

// 推荐方式
StringBuilder sb = new StringBuilder(10000 * 5);  // 预分配容量
for (int i = 0; i < 10000; i++) {
    sb.append(i);
}
String result = sb.toString();
```

#### 全景对比

| 特性 | StringBuilder | StringBuffer |
|------|--------------|-------------|
| **线程安全** | ❌ 非线程安全 | ✅ 方法使用 `synchronized` |
| **性能** | ⭐ 最高（无锁开销） | ⭐⭐ 次之（有锁开销） |
| **引入版本** | JDK 1.5 | JDK 1.0（原始） |
| **适用场景** | **单线程**字符串拼接 | **多线程**共享可变字符串 |
| **默认容量** | 16 | 16 |
| **扩容策略** | `原容量 * 2 + 2` | `原容量 * 2 + 2` |

#### 实际性能数据（Java 11, 10 万次追加操作）

| 操作 | 耗时 | 说明 |
|------|------|------|
| `String +=` | ~8500 ms | 大量临时对象，GC 压力极大 |
| `String.concat()` | ~4200 ms | 有改进但依旧慢 |
| `StringBuilder.append()` | ~8 ms | 最快 |
| `StringBuffer.append()` | ~15 ms | 有 synchronized 开销 |

> ⚠️ **StringBuffer 的使用已经很少**。现代 Java 多线程场景中，StringBuilder + 局部变量（每个线程有自己的 StringBuilder）远比 StringBuffer 更高效。StringBuffer 仅在确实需要跨线程共享同一个可变字符串时使用。

#### 常见面试题

```java
String str1 = "hello";
String str2 = "hel" + "lo";          // 编译期常量折叠，实际是 "hello"
System.out.println(str1 == str2);     // true

String hel = "hel";
String str3 = hel + "lo";            // 运行时拼接，通过 StringBuilder
System.out.println(str1 == str3);     // false

String str4 = (hel + "lo").intern(); // 强制入池
System.out.println(str1 == str4);     // true
```

### 2.7 String 常用方法全景

| 分类 | 方法 | 说明 |
|------|------|------|
| **判空** | `isEmpty()` / `isBlank()` (JDK 11+) | `isEmpty` 判断长度；`isBlank` 判断空白字符 |
| **比较** | `equals()` / `equalsIgnoreCase()` / `compareTo()` | 内容比较与字典序 |
| **查找** | `indexOf()` / `lastIndexOf()` / `contains()` / `startsWith()` / `endsWith()` | 子串定位与前缀后缀判断 |
| **截取** | `substring(begin, end)` / `split(regex)` / `chars()` | 子串提取与分割 |
| **转换** | `toCharArray()` / `getBytes()` / `toLowerCase()` / `toUpperCase()` | 类型转换 |
| **替换** | `replace(old, new)` / `replaceAll(regex, str)` / `replaceFirst(regex, str)` | 字符/字符串替换 |
| **修剪** | `trim()` / `strip()` (JDK 11+) / `stripLeading()` / `stripTrailing()` | **strip 能去除全角空格，trim 不能** |
| **格式化** | `format(format, args)` / `formatted(args)` (JDK 15) | 格式化输出 |
| **拼接** | `join(delimiter, elements)` | 静态方法，无需循环 |
| **重复** | `repeat(n)` (JDK 11+) | 字符串重复 n 次 |
| **代码点** | `codePointAt()` / `codePointCount()` | 处理 Emoji 等补充字符 |

```java
// JDK 11+ 实用方法
"  ".isBlank();              // true
"  ".isEmpty();              // false
"  Hello  ".strip();         // "Hello"（去首尾空白，包括全角）
"Hello".repeat(3);           // "HelloHelloHello"

// JDK 15+ formatted
String msg = "Hello, %s!".formatted("World");  // "Hello, World!"

// 静态 join 替代循环拼接
String joined = String.join(", ", "A", "B", "C");  // "A, B, C"
```

> 🎯 **核心要点**：String 不可变性是 Java 安全模型和性能优化的基石；非线程安全场景始终用 StringBuilder；`equals` 才是内容比较，`==` 只比引用；JDK 9+ 紧凑字符串对英文场景节省 50% 内存。

---

## 3. 包装类与自动装箱

> 8 种基本类型各有对应的包装类，自动装箱拆箱在编译期通过 `valueOf`/`xxxValue` 完成。理解包装类的缓存机制和性能陷阱，能避免写出"看起来对但很慢"的代码。

### 3.1 8 种包装类全景

| 基本类型 | 包装类 | 字节数 | 默认值 | 最小值 | 最大值 |
|---------|--------|:-----:|:-----:|:-------:|:-------:|
| `boolean` | `Boolean` | N/A | `false` | — | — |
| `byte` | `Byte` | 1 | 0 | -128 | 127 |
| `short` | `Short` | 2 | 0 | -32,768 | 32,767 |
| `int` | `Integer` | 4 | 0 | -2³¹ | 2³¹-1 |
| `long` | `Long` | 8 | 0L | -2⁶³ | 2⁶³-1 |
| `float` | `Float` | 4 | 0.0f | 1.4e-45f | 3.4e+38f |
| `double` | `Double` | 8 | 0.0d | 4.9e-324d | 1.8e+308d |
| `char` | `Character` | 2 | '\u0000' | 0 | 65,535 |

#### 继承关系

```text
Object
 ├── Number (抽象类)
 │    ├── Byte
 │    ├── Short
 │    ├── Integer
 │    ├── Long
 │    ├── Float
 │    ├── Double
 │    └── AtomicInteger / AtomicLong / BigDecimal / BigInteger（不在本模块）
 ├── Boolean
 └── Character
```

### 3.2 自动装箱拆箱的编译器行为

#### 语法糖的真相

```java
// 源代码 —— 开发者写的样子
Integer a = 127;          // 自动装箱
int b = a;                // 自动拆箱

// 编译后 —— javac 翻译的结果
Integer a = Integer.valueOf(127);  // 装箱
int b = a.intValue();              // 拆箱
```

```java
// 泛型时代的典型模式
List<Integer> list = new ArrayList<>();
list.add(42);                           // 自动装箱：list.add(Integer.valueOf(42));
int value = list.get(0);                // 自动拆箱：list.get(0).intValue();

// 复合表达式
Integer result = a + b;                 // 拆箱 → 运算 → 装箱
// 编译后：Integer.valueOf(a.intValue() + b.intValue())
```

### 3.3 Integer 缓存机制

Integer 内部维护了一个缓存池，默认缓存 **-128 到 127** 之间的所有 Integer 对象。

```java
Integer a = 127;
Integer b = 127;
System.out.println(a == b);       // true  —— 两个都是缓存中的同一对象

Integer c = 128;
Integer d = 128;
System.out.println(c == d);       // false —— 128 超出缓存范围，创建了两个不同对象

// 逻辑比较始终用 equals
System.out.println(c.equals(d));  // true  —— 内容相同
```

#### 缓存覆盖范围

| 类型 | 缓存范围 | 可配置？ |
|------|---------|:-------:|
| `Byte` | **全部**（-128 ~ 127） | ❌ |
| `Short` | -128 ~ 127 | ❌ |
| `Integer` | -128 ~ 127 | ✅ `-XX:AutoBoxCacheMax=2000` |
| `Long` | -128 ~ 127 | ❌ |
| `Character` | 0 ~ 127（ASCII 全量） | ❌ |
| `Boolean` | `true` / `false` 两个实例 | ❌ |
| `Float` / `Double` | **不缓存** | ❌ |

```java
// 修改 Integer 缓存上限（JVM 参数）
// -Djava.lang.Integer.IntegerCache.high=2000

// 通过代码查看缓存上限
Integer[] cache = Integer.class.getDeclaredClasses()[0]
        .getDeclaredField("cache");
cache.setAccessible(true);
Integer[] cacheData = (Integer[]) cache.get(null);
System.out.println("Cache size: " + cacheData.length);
```

### 3.4 valueOf vs new Integer()

```java
Integer a = Integer.valueOf(42);   // ✅ 优先使用 —— 使用缓存
Integer b = new Integer(42);       // ❌ 避免使用 —— 始终创建新对象（JDK 9 起 marked @Deprecated）
```

| 方式 | 对象创建策略 | 适用场景 |
|------|-------------|---------|
| `Integer.valueOf(n)` | 使用缓存（范围内返回缓存对象） | **始终推荐** |
| `new Integer(n)` | 无论值大小都创建新对象 | 几乎不使用（已废弃） |

> 💡 `new Integer()` / `new Long()` / `new Short()` 在 JDK 9 中已被标记为 `@Deprecated`，建议统一使用 `valueOf()` 或自动装箱。

### 3.5 性能陷阱：循环中装箱的 GC 压力

```java
// ❌ 问题代码 —— 大量自动装箱导致 GC 风暴
long sum = 0L;
for (long i = 0; i < Integer.MAX_VALUE; i++) {
    sum += i;     // Long 类型的 sum 被反复装箱拆箱
}
// 每次 sum += i 实际是：
// sum = Long.valueOf(sum.longValue() + i);
// 导致产生大量 Long 临时对象

// ✅ 正确做法 —— 使用基本类型
long sum = 0L;
for (long i = 0; i < Integer.MAX_VALUE; i++) {
    sum += i;     // 基本类型运算，无装箱
}
```

#### 性能对比

```java
// 测试：1000 万次累加
// 基本类型 long：       ~15  ms
// 包装类型 Long：       ~350 ms（增加 23 倍）
// 根本原因：每次运算产生临时 Long 对象 → GC 频繁触发
```

> ⚠️ **包装类在集合中是合理的**（泛型不支持基本类型），但在算法计算、循环累加等场景，使用基本类型可避免不必要的 GC 压力。

### 3.6 parseInt vs valueOf

```java
int n1 = Integer.parseInt("42");     // 返回基本类型 int
Integer n2 = Integer.valueOf("42");  // 返回包装类 Integer
Integer n3 = Integer.parseInt("42"); // ❌ 编译错误，parseInt 返回 int
```

| 方法 | 返回类型 | 内部实现 | 适用场景 |
|------|---------|---------|---------|
| `Integer.parseInt(s)` | `int` | 直接解析字符串为 int | 需要基本类型的后续运算 |
| `Integer.valueOf(s)` | `Integer` | 内部调用 `parseInt` 后装箱 | 需要包装对象（如放入集合） |

```java
// 常见错误
String s = "123";
int sum = Integer.parseInt(s) + 456;    // ✅ 直接计算

List<Integer> list = new ArrayList<>();
list.add(Integer.valueOf(s));            // ✅ 显式装箱
list.add(Integer.parseInt(s));           // ❌ 编译错误
```

### 3.7 比较规则总结

```java
Integer a = 100;
Integer b = 100;
Integer c = 200;
Integer d = 200;

// == 比较
a == b;              // true  （缓存范围内）
c == d;              // false （缓存范围外，不同对象）

// 混合类型比较（自动拆箱）
Integer e = 100;
int f = 100;
e == f;              // true  —— Integer 自动拆箱为 int 后比较值

// 推荐做法：统一用 equals
c.equals(d);         // true  —— 逻辑相等
```

> 🎯 **核心要点**：包装类优先使用 `valueOf()`；缓存在 -128~127 范围内；循环计算用基本类型防 GC 压力；比较始终用 `equals` 而非 `==`（除非你知道两侧是否在缓存范围内）。

---

## 4. System 与 Runtime

> System 是 JVM 与操作系统交互的核心门面——获取系统属性、环境变量、时间戳、数组拷贝等日常操作都靠它。Runtime 则提供 JVM 运行时资源的全局视图。

### 4.1 System 类全景

System 类是一个 `final` 类，构造方法为 `private`，所有成员都是 **静态** 的。它主要提供三个部分：

```text
System
 ├── 标准 I/O 流：out / in / err
 ├── 系统属性与环境：getProperty() / getenv() / setProperty()
 ├── 时间函数：currentTimeMillis() / nanoTime()
 ├── 数组操作：arraycopy()
 ├── 系统控制：gc() / exit() / runFinalization()
 └── 安全管理：SecurityManager（Java 17 已标记 deprecated）
```

### 4.2 System.out / in / err

```java
// 标准输出流（自动刷新 PrintStream）
System.out.println("Hello");            // 输出到标准输出
System.err.println("Error occurred");   // 输出到标准错误

// 重定向输出
PrintStream fileOut = new PrintStream(new FileOutputStream("log.txt"));
System.setOut(fileOut);                 // 将 System.out 重定向到文件

// 标准输入（通常被 Scanner 包装使用）
Scanner scanner = new Scanner(System.in);
String line = scanner.nextLine();
```

#### 三者的关系

| 流 | 类型 | 用途 | 常用包装 |
|----|------|------|---------|
| `System.out` | `PrintStream` | 标准输出 | `System.out.println()` |
| `System.in` | `InputStream` | 标准输入 | `new Scanner(System.in)` |
| `System.err` | `PrintStream` | 标准错误 | `System.err.println()` |

### 4.3 System.currentTimeMillis() vs nanoTime()

```java
long currentMs = System.currentTimeMillis();  // 当前时间戳（毫秒），从 1970-01-01 UTC 开始
long currentNs = System.nanoTime();           // 相对时间（纳秒），用于测量时间差
```

| 对比维度 | `currentTimeMillis()` | `nanoTime()` |
|----------|----------------------|-------------|
| **含义** | 当前**绝对时间**（wall clock） | 当前 JVM 运行以来的相对**纳秒数** |
| **精度/分辨率** | 毫秒（实际精度取决于 OS，约 1~15ms） | 纳秒（实际精度取决于硬件，约微秒级） |
| **单调性** | ❌ 可被系统时间修改、NTP 同步调整 | ✅ 单调递增（同一 JVM 实例内） |
| **用途** | 日志时间戳、数据库记录、业务时间 | 性能测试、代码耗时分析 |
| **JDK 18+ 改进** | `currentTimeMillis()` 精度提升 | — |

```java
// ✅ 正确用法
long start = System.nanoTime();
// ... 执行代码 ...
long elapsed = System.nanoTime() - start;
System.out.printf("耗时: %.2f ms%n", elapsed / 1_000_000.0);

// ❌ 错误用法：用 nanoTime 取"当前时间"
System.out.println(new Date(System.nanoTime() / 1_000_000));  // 无意义

// ✅ 记录时间戳应用 currentTimeMillis
logger.info("用户登录: userId={}, time={}",
        userId, Instant.now());  // JDK 8+
```

> 💡 性能基准测试请使用 JMH（Java Microbenchmark Harness）而非 `nanoTime`，因为 JIT 编译、代码预热等会严重影响单点测量结果。

### 4.4 System.arraycopy() vs Arrays.copyOf()

```java
int[] src = {1, 2, 3, 4, 5};
int[] dest = new int[5];

// System.arraycopy —— native 方法，直接内存拷贝
System.arraycopy(src, 0, dest, 0, src.length);

// Arrays.copyOf —— 内部调用 arraycopy，自动创建目标数组
int[] copy = Arrays.copyOf(src, src.length);

// Arrays.copyOfRange —— 拷贝部分范围
int[] part = Arrays.copyOfRange(src, 1, 4);  // {2, 3, 4}
```

| 对比 | `System.arraycopy()` | `Arrays.copyOf()` |
|------|---------------------|-------------------|
| 方法类型 | native 方法 | Java 方法（内部调 `arraycopy`） |
| 目标数组 | 需预先创建 | 自动创建 |
| 返回 | `void` | 返回新数组 |
| 灵活性 | 指定起始位置 | 仅能从头拷贝或指定长度 |
| 性能 | **最高** | 略低（有新建数组开销） |
| 适用场景 | 自管理内存的底层操作 | 日常数组拷贝 |

```java
// Arrays.copyOf 底层源码（JDK 8+）
public static <T> T[] copyOf(T[] original, int newLength) {
    return (T[]) Arrays.copyOf(original, newLength, original.getClass());
}

// Arrays.copyOf(U[], int, Class<? extends T[]>) 内部：
// System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
```

### 4.5 System.getProperty() / getenv()

```java
// 获取 JVM 系统属性（-D 参数注入）
System.getProperty("java.version");            // 17.0.1
System.getProperty("user.home");               // C:\Users\username
System.getProperty("user.dir");                // 当前项目路径
System.getProperty("file.separator");          // \ 或 /
System.getProperty("line.separator");          // 换行符

// 设置自定义属性
System.setProperty("app.config.path", "/etc/myapp/config.yml");

// 不存在的属性返回 null（可提供默认值）
String debug = System.getProperty("debug", "false");

// 获取环境变量（操作系统级别）
System.getenv("JAVA_HOME");                    // C:\Program Files\Java\jdk-17
System.getenv("PATH");                         // 系统 PATH 变量

// 获取所有环境变量
Map<String, String> envMap = System.getenv();
envMap.forEach((key, value) ->
        System.out.println(key + " = " + value));
```

#### getProperty 与 getenv 的区别

| 对比 | `getProperty()` | `getenv()` |
|------|----------------|------------|
| 来源 | JVM 参数 `-Dkey=value` 或 `setProperty()` | 操作系统环境变量 |
| 运行时修改 | ✅ 可动态修改 | ❌ 只读 |
| 作用域 | JVM 实例级别 | 进程级别 |
| 敏感信息 | 不推荐存密码 | 不推荐存密码 |

### 4.6 System.gc() — 建议性

```java
System.gc();  // 只是"建议"JVM 执行 GC，不保证立即执行
```

| 问题 | 说明 |
|------|------|
| **不可靠** | 调用 `System.gc()` 等价于 `Runtime.getRuntime().gc()`，JVM 可能忽略 |
| **性能影响** | 强制触发 Full GC 会导致 STW（Stop-The-World）停顿 |
| **是否忽略** | 可通过 `-XX:+DisableExplicitGC` 禁用 |
| **生产建议** | **永远不要在生产代码中显式调用** |

> ⚠️ **例外**：某些性能基准测试框架（如 JMH）会在 `@TearDown` 阶段调用 `System.gc()` 以减少测试间的 GC 干扰。除此以外，请交给 JVM 自行决定。

### 4.7 Runtime 类深度

Runtime 是每个 JVM 进程唯一的运行时实例，通过 `Runtime.getRuntime()` 获取。

```java
Runtime rt = Runtime.getRuntime();

// 内存信息
long maxMemory = rt.maxMemory();          // JVM 能使用的最大内存（-Xmx）
long totalMemory = rt.totalMemory();      // 当前已申请的堆大小
long freeMemory = rt.freeMemory();        // 已申请中的空闲部分
long usedMemory = totalMemory - freeMemory;

System.out.printf("Max: %d MB%n", maxMemory / 1024 / 1024);
System.out.printf("Total: %d MB%n", totalMemory / 1024 / 1024);
System.out.printf("Used: %d MB%n", usedMemory / 1024 / 1024);

// 处理器核心数
int processors = rt.availableProcessors();
System.out.println("CPU cores: " + processors);
// 在设置线程池大小时常用：int poolSize = availableProcessors * 2;

// JVM 退出
rt.exit(0);   // 等价于 System.exit(0)
rt.halt(1);   // 强制退出，不执行 shutdown hook
```

#### Runtime.exec() — 谨慎使用

```java
// 执行外部命令 —— 存在安全隐患
Process process = Runtime.getRuntime().exec("ping google.com");
```

> ⚠️ **Runtime.exec() 的安全注意事项**

| 风险类别 | 说明 | 建议 |
|---------|------|------|
| **命令注入** | 参数中包含用户输入时，可能被拼接恶意命令 | 使用 `ProcessBuilder` + 参数列表而非字符串 |
| **子进程挂起** | 子进程输出缓冲区满时阻塞，若不及时读取流会死锁 | 务必消费 stdout/stderr 流 |
| **平台依赖** | 命令格式因 OS 不同而异 | 使用 `ProcessBuilder` 提供更好的跨平台支持 |
| **资源泄露** | 未调用 `process.destroy()` 可能导致进程残留 | 在 `finally` 中确保销毁 |

```java
// ✅ 推荐使用 ProcessBuilder
ProcessBuilder pb = new ProcessBuilder(
        "ping", "-c", "4", "google.com"  // 参数分离，避免注入
);
pb.redirectErrorStream(true);  // 合并错误流到标准输出
Process process = pb.start();

// 务必消费输出流
try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
}

int exitCode = process.waitFor();
System.out.println("Exit code: " + exitCode);
```

> 🎯 **核心要点**：`System.out` / `err` 是 PrintStream，可重定向；`nanoTime` 用于测耗时，`currentTimeMillis` 用于取时间戳；`arraycopy` 是 native 最高效；`System.gc()` 不可靠，生产环境绝不用；`Runtime.exec()` 注意安全。

---

## 5. Math 与 StrictMath

> Math 类提供基本的数学运算（三角函数、指数、对数、取整等），性能优先。StrictMath 保证在所有平台上可重现的结果，但可能牺牲性能。

### 5.1 常用方法全景

#### 取整操作

```java
Math.ceil(2.3);     // 3.0  向上取整
Math.floor(2.7);    // 2.0  向下取整
Math.round(2.5);    // 3    四舍五入（返回 long 或 int）
Math.round(2.4);    // 2
Math.rint(2.5);     // 2.0  向最近的整数取整（遇 .5 取偶数方向）
Math.rint(3.5);     // 4.0
```

#### 极值、绝对值与符号

```java
Math.max(a, b);          // 最大值
Math.min(a, b);          // 最小值
Math.abs(-10);           // 10
Math.abs(-2147483648);   // -2147483648 （溢出！int 最小值绝对值 > int 最大值）

// 安全取绝对值
Math.absExact(-10);      // Java 15+，溢出时抛 ArithmeticException
Math.abs(-2147483648);   // 若 Java 15 以下，需自行检查
```

> ⚠️ `Math.abs(Integer.MIN_VALUE)` 返回负数。因为 Integer.MIN_VALUE = -2,147,483,648，其绝对值为 2,147,483,648，但 int 最大值为 2,147,483,647，正好越界 1。使用 `Math.absExact()`（Java 15+）抛出异常代替静默错误。

#### 幂、平方根与指数

```java
Math.pow(2, 10);           // 1024.0   幂运算（返回 double）
Math.sqrt(16);             // 4.0      平方根
Math.cbrt(27);             // 3.0      立方根
Math.exp(1);               // e¹ ≈ 2.718
Math.log(10);              // ln(10)   自然对数
Math.log10(1000);          // 3.0      以 10 为底的对数
```

#### 三角函数

```java
Math.sin(Math.toRadians(30));    // 0.5
Math.cos(Math.PI / 3);           // 0.5
Math.tan(Math.PI / 4);           // 1.0

Math.toDegrees(Math.PI);         // 180.0
Math.toRadians(180);             // π ≈ 3.14159
```

#### 随机数

```java
// Math.random() 返回 [0.0, 1.0) 的 double
double r = Math.random();
int dice = (int) (Math.random() * 6) + 1;     // 1~6

// 多线程下推荐 ThreadLocalRandom
int dice2 = ThreadLocalRandom.current()
        .nextInt(1, 7);   // [1, 7)
```

### 5.2 StrictMath 的可重现性保证

Math 和 StrictMath 的核心区别在于：

```java
// Math —— 性能优先，可能在不同平台上产生不同结果
double a = Math.sin(1.0);
double b = Math.cos(1.0);
// 在不同硬件上，最后几位可能不同

// StrictMath —— 保证在所有平台上结果完全一致
double c = StrictMath.sin(1.0);
double d = StrictMath.cos(1.0);
// 无论 x86、ARM、MIPS，结果严格一致
```

| 特性 | `Math` | `StrictMath` |
|------|--------|-------------|
| **平台一致性** | ❌ 不同平台结果最后几位可能不同 | ✅ 结果完全确定、可重现 |
| **性能** | ⭐ 较快（使用 CPU 平台的 native 实现，如 x86 的 `fsin`） | ⭐ 稍慢（使用 FDMLib 参考实现） |
| **实现方式** | 部分委托给 StrictMath，部分用平台相关代码 | 纯 Java 实现（严格遵循 IEEE 754） |
| **适用场景** | 绝大多数场景（游戏、图形、日常计算） | **科学计算**、加密校验、需要精确可重现结果的场景 |

```java
// Math 底层实现策略
public static double sin(double a) {
    // 在 StrictMath 中有相同方法时，直接调用
    // 但 Math 在某些平台使用 CPU 原生指令
    return StrictMath.sin(a);  // 某些平台上实际如此
}
```

> 💡 **实际上**：在 x86/x64 平台上，`Math.sin()` 通过 native 方法使用 CPU 的 `fsin` 指令，性能比 StrictMath 的 Java 实现快；但在某些平台上结果精度有差异。对绝大多数业务应用，**完全不需要关注这个差异**，用 Math 即可。

### 5.3 Java 9+ Math 新增方法（clamp 等）

从 Java 8 到 Java 17，Math 和 StrictMath 持续新增实用方法。

#### Java 8 新增

```java
// 无符号整数运算
Integer.divideUnsigned(-1, 2);        // 2147483647
Integer.remainderUnsigned(-1, 2);     // 1
Long.divideUnsigned(-1L, 2L);         // 9223372036854775807

// 精确运算（溢出时抛 ArithmeticException）
Math.addExact(2_000_000_000, 1_000_000_000);  // 抛异常
Math.subtractExact(100, 200);                   // 正常
Math.multiplyExact(1_000_000, 1_000_000);       // 抛异常
Math.negateExact(Integer.MIN_VALUE);             // 抛异常
Math.toIntExact(100_000_000_000L);               // long → int 溢出检查
```

#### Java 11 新增

```java
// 精准取反范围检查
Math.negateExact(Integer.MIN_VALUE);  // Java 8 已有，Java 11 增强了 Long 版本
```

#### Java 15 新增

```java
// 安全绝对值
Math.absExact(Integer.MIN_VALUE);  // 抛 ArithmeticException
Math.absExact(-100);               // 100
```

#### Java 17 新增

```java
// clamp —— 将值限制在指定范围内（三个重载）
Math.clamp(5, 0, 10);               // 5   （在范围内）
Math.clamp(-5, 0, 10);              // 0   （低于下限）
Math.clamp(15, 0, 10);              // 10  （超出上限）

// 兼容基本类型
Math.clamp(3.14, 0.0, 1.0);        // 1.0
Math.clamp(5L, 0L, 10L);           // 5L
```

```java
// clamp 的等价实现（JDK 17 之前需手动写）
public static int clamp(int value, int min, int max) {
    if (min > max) throw new IllegalArgumentException(
            "min (" + min + ") > max (" + max + ")");
    return Math.max(min, Math.min(max, value));
}
```

#### Java 21 新增

```java
// Math.clamp 扩展了 long/double/float 版本
// 以及对 Character 类的进一步完善
```

### 5.4 Math 常用常量

```java
System.out.println(Math.PI);       // 3.141592653589793
System.out.println(Math.E);        // 2.718281828459045

// 浮点数边界
System.out.println(Double.MAX_VALUE);         // 1.7976931348623157e+308
System.out.println(Double.MIN_VALUE);         // 4.9e-324（正最小）
System.out.println(Double.POSITIVE_INFINITY); // Infinity
System.out.println(Double.NEGATIVE_INFINITY); // -Infinity
System.out.println(Double.NaN);               // NaN
```

### 5.5 浮点数精度说明

```java
// 浮点数运算不精确
0.1 + 0.2 == 0.3;  // false

// 精确比较方式
double sum = 0.1 + 0.2;
double epsilon = 1e-10;
Math.abs(sum - 0.3) < epsilon;  // true

// 金融计算使用 BigDecimal
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");
BigDecimal c = a.add(b);          // 0.3 精确
// 永远不要用 BigDecimal(double) 构造
new BigDecimal(0.1);              // ❌ 0.100000000000000005551115...
new BigDecimal("0.1");            // ✅ 0.1
```

> 🎯 **核心要点**：绝大多数场景用 `Math` 即可，无需关心与 `StrictMath` 的差异；精确运算（`addExact`、`multiplyExact`）可避免整数溢出引入的 bug；浮点数比较使用差值法；金融计算用 `BigDecimal`。

---

## 6. 动手练习

> 以下练习覆盖 Object 契约、String 内存模型、包装类缓存、System 常用操作四大主题。建议先手写实现，再对照参考答案。

### 练习 1：实现符合契约的 equals + hashCode

**题目**：有一个 `Book` 类，包含 `isbn`（String）、`title`（String）、`year`（int）、`price`（double）四个字段。请正确重写 `equals` 和 `hashCode` 方法，满足以下条件：

- 两本书当 `isbn` 相同时视为同一本书（其他字段不影响逻辑相等）
- 确保放入 HashSet 后行为正确
- 使用 `Objects` 工具类

<details>
<summary>💡 点击查看参考答案</summary>

```java
public class Book {
    private String isbn;      // ISBN 号，唯一标识
    private String title;
    private int year;
    private double price;

    // 构造方法、getter/setter 省略...

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(isbn, book.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }

    @Override
    public String toString() {
        return "Book{isbn='" + isbn + "', title='" + title + "'}";
    }

    // 测试
    public static void main(String[] args) {
        Set<Book> books = new HashSet<>();
        books.add(new Book("978-7-111-12345-6", "Java 核心技术", 2020, 128.0));
        books.add(new Book("978-7-111-12345-6", "Java 核心技术卷 I", 2020, 128.0));

        System.out.println("Set size: " + books.size());  // 1 —— isbn 相同，视为同一本书

        Book b1 = new Book("978-7-111-12345-6", "Java 核心技术", 2020, 128.0);
        Book b2 = new Book("978-7-111-12345-6", "Java 核心技术卷 I", 2020, 128.0);
        System.out.println(b1.equals(b2));  // true
        System.out.println(b1.hashCode() == b2.hashCode());  // true
    }
}
```

> 🎯 **练习题核心**：`equals` 只依赖 `isbn` 字段，同时 `hashCode` 也只依赖 `isbn`。这是"以业务标识进行逻辑相等"的典型模式。
</details>

---

### 练习 2：String intern 理解测试

**题目**：写出以下代码的输出结果，并解释原因。

```java
public class StringInternQuiz {
    public static void main(String[] args) {
        String s1 = "hello";
        String s2 = new String("hello");
        String s3 = s2.intern();
        String s4 = "he" + "llo";
        String s5 = "he";
        String s6 = s5 + "llo";
        String s7 = s6.intern();

        System.out.println(s1 == s2);    // ?
        System.out.println(s1 == s3);    // ?
        System.out.println(s1 == s4);    // ?
        System.out.println(s1 == s5);    // ?
        System.out.println(s1 == s6);    // ?
        System.out.println(s1 == s7);    // ?
        System.out.println(s3 == s7);    // ?
    }
}
```

<details>
<summary>💡 点击查看参考答案</summary>

```java
s1 == s2;    // false — s1 是池中对象，s2 是堆中对象
s1 == s3;    // true  — s3 是 s2.intern() 返回值，池中有则返回池中对象
s1 == s4;    // true  — "he"+"llo" 编译期常量折叠优化为 "hello"，池中同一对象
s1 == s5;    // false — s5 是 "he"，不同字符串引用
s1 == s6;    // false — s5 + "llo" 是运行时 StringBuilder 操作，产生新 String 对象
s1 == s7;    // true  — s6.intern() 将新字符串入池，池中有则返回池中对象 s1
s3 == s7;    // true  — 两者都是从池中获取到的 "hello" 对象引用
```

> 🎯 **练习题核心**：编译期常量折叠、运行时字符串拼接、`intern()` 方法的行为。
</details>

---

### 练习 3：包装类缓存与比较陷阱

**题目**：阅读以下代码并解释每一个 `System.out.println` 的结果，特别说明为什么。

```java
public class IntegerCacheQuiz {
    public static void main(String[] args) {
        Integer a = 100;
        Integer b = 100;
        Integer c = 200;
        Integer d = 200;

        System.out.println(a == b);           // ?
        System.out.println(c == d);           // ?

        Integer e = new Integer(100);
        System.out.println(a == e);           // ?

        int f = 100;
        System.out.println(a == f);           // ?

        Integer g = Integer.valueOf(200);
        Integer h = Integer.valueOf(200);
        System.out.println(g == h);           // ?

        // 进阶：以下表达式的结果是什么？
        Integer x = new Integer(100);
        Integer y = new Integer(100);
        System.out.println(x == y);           // ?
        System.out.println(x.equals(y));      // ?
    }
}
```

<details>
<summary>💡 点击查看参考答案</summary>

```java
a == b;     // true  — 自动装箱调用 valueOf(100)，100 在 [-128,127] 缓存内，返回同一对象
c == d;     // false — 200 超出默认缓存范围，valueOf(200) 创建了不同的 Integer 对象
a == e;     // false — a 是缓存中的 Integer 对象，e 是 new Integer(100) 显式新对象
a == f;     // true  — 混合类型比较：Integer 自动拆箱为 int，比较原始值 100 == 100
g == h;     // false — 显式 valueOf(200) 但超出缓存范围，与 c == d 道理相同，每个调用返回新对象
// 注意：如果设置了 -XX:AutoBoxCacheMax=500，则 200 也在缓存内，结果为 true

new Integer(100) == new Integer(100);    // false — 两个不同的堆对象
new Integer(100).equals(new Integer(100)); // true — equals 比较的是值
```

> 🎯 **练习题核心**：Integer 缓存范围、`==` vs `equals`、混合类型比较时的自动拆箱。
</details>

---

### 练习 4：性能分析 —— StringBuilder 扩容

**题目**：请对比以下三种字符串拼接方式的性能差异，并说明为什么。如果数据量从 1 万扩大到 100 万，预期趋势如何？

```java
// 方式 A：直接 +=
String a = "";
for (int i = 0; i < 10000; i++) {
    a += i;
}

// 方式 B：无预分配 StringBuilder
StringBuilder b = new StringBuilder();
for (int i = 0; i < 10000; i++) {
    b.append(i);
}

// 方式 C：预分配 StringBuilder
StringBuilder c = new StringBuilder(50000);
for (int i = 0; i < 10000; i++) {
    c.append(i);
}
```

<details>
<summary>💡 点击查看参考答案</summary>

性能排序：C > B >> A（差异可达数千倍）。

**方式 A 的代价**：
每次 `a += i` 等价于：
```java
a = new StringBuilder().append(a).append(i).toString();
```
每次循环产生 2~3 个临时对象（StringBuilder + String + 字符数组），10000 次循环产生约 2~3 万个短命对象，触发大量 GC。

**方式 B vs C 的区别**：
StringBuilder 默认初始容量为 16，当追加的内容超出当前容量时自动扩容：
```java
// 扩容策略
int newCapacity = (oldCapacity << 1) + 2;  // 原容量 * 2 + 2
```
append 10000 个数字字符串，总长度约 38890 个字符，扩容次数大约为：
- 16 → 34 → 70 → 142 → 286 → 574 → 1150 → 2302 → 4606 → 9214 → 18430 → 36862 → 73726
- 约 12 次扩容，每次涉及数组拷贝（`Arrays.copyOf`）

方式 C 直接预分配 50000 字符，零扩容，无数组拷贝开销。

**100 万数据量趋势**：
- A 方式：呈 **O(n²)** 增长，每次 += 都复制整个已构建字符串，100 万时不可用
- B 方式：约 20 次扩容，线性增长，可控
- C 方式：无扩容，线性增长，最快

> 🎯 **练习题核心**：预分配 StringBuilder 容量避免多次扩容，是高频字符串操作的关键优化。
</details>

---

> 💡 以上练习建议全部手写一遍，理解 `==` vs `equals`、`intern()` 行为、缓存范围、`StringBuilder` 效率差异，这些是面试和日常开发中最容易出错的点。

---

**上一模块**：[00-Java API知识体系总览](./00-Java%20API知识体系总览.md)
**下一模块**：[02-Java集合框架原理与实战](./02-Java集合框架原理与实战.md)
**返回总览**：[00-Java API知识体系总览](./00-Java%20API知识体系总览.md)
