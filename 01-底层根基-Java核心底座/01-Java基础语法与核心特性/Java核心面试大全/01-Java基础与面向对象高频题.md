# 01 - Java 基础与面向对象高频题

> 🎯 面试第一关 — 占权重最高、最不容易拉开差距也最不能丢分的模块。重点是"概念准确 + 对比清晰 + 能举反例"

---

## 目录

1. [面向对象三大特性](#1-面向对象三大特性)
2. [== 与 equals、hashCode](#2--与-equalshashcode)
3. [String 专题](#3-string-专题)
4. [值传递、装箱、异常、泛型](#4-值传递装箱异常泛型)
5. [反射与注解](#5-反射与注解)

---

## 1. 面向对象三大特性

### 1.1 封装、继承、多态分别是什么？

**参考答案：**

| 特性 | 定义 | 实现手段 | 好处 |
|------|------|----------|------|
| **封装** | 隐藏内部实现，暴露受控的访问接口 | private 字段 + getter/setter，访问修饰符 | 保护数据完整性、隔离变化 |
| **继承** | 子类复用父类属性和方法 | extends，单继承 | 代码复用、is-a 关系建模 |
| **多态** | 同一方法调用在不同对象上有不同表现 | 方法重写（动态绑定）+ 接口/父类引用 | 面向抽象编程、可扩展 |

```java
// 多态三要素：继承（或实现）、重写、父类引用指向子类对象
Animal a = new Dog();   // 编译看左边，运行看右边
a.sound();              // 输出 "汪汪"（动态绑定）
```

**追问：**
- 多态的实现原理？→ 方法表（vtable）动态分派；invokevirtual 指令按实际类型查方法表
- 重载（Overload）是多态吗？→ 编译期静态绑定，属于"编译时多态"，与方法重写的"运行时多态"区分
- 继承 vs 组合？→ 组合优于继承：继承破坏封装、层级过深难维护；组合灵活（has-a）
- 接口 vs 抽象类？→ 接口：行为契约、多实现、默认方法；抽象类：模板骨架、单继承、可有状态字段

**记忆点：**
> 🎯 封装藏数据、继承传血脉、多态靠重写。抽象类管"是什么"（is-a），接口管"能做什么"（can-do）。

### 1.2 接口 vs 抽象类的本质区别？

**参考答案：**

| 对比项 | 抽象类 | 接口 |
|--------|--------|------|
| 关键字 | abstract class | interface（JDK8+ 可以有默认/静态方法） |
| 继承方式 | 单继承 | 多实现 |
| 字段 | 可以有实例字段 | 只能有常量（public static final） |
| 构造器 | 有 | 无 |
| 语义 | is-a（模板骨架） | can-do（能力契约） |
| 典型场景 | 模板方法模式（如 Spring 的 AbstractTemplate） | 策略/观察者等契约（如 Runnable、Comparator） |

**追问：** JDK8 默认方法解决什么？→ 接口演进兼容（List.sort 直接加默认方法，无需破坏 ArrayList 等实现类）。JDK9 私有方法？→ 接口内代码复用。

---

## 2. == 与 equals、hashCode

### 2.1 == 和 equals() 的区别？

**参考答案：**

| 对比 | == | equals() |
|------|-----|----------|
| 基本类型 | 比较**值** | 不可用 |
| 引用类型 | 比较**地址**（是否同一对象） | Object 默认同 ==；String 等重写为比较**内容** |
| 是否可重写 | 不可 | 可以 |

```java
String a = new String("abc");
String b = new String("abc");
a == b          // false：不同对象地址
a.equals(b)     // true：String 重写了 equals，比较内容
```

**追问：** 重写 equals 为什么必须重写 hashCode？→ 违反约定会导致 HashMap/HashSet 中对象"找不到"（相同对象 hashCode 不同 → 分到不同桶，get 返回 null）。equals 相等的对象 hashCode 必须相等。

**记忆点：**
> 🎯 基本类型比值，引用类型比地址；equals 默认比地址，重写后才比内容。重写 equals 必须重写 hashCode — 这是集合类的地基。

### 2.2 hashCode() 在 HashMap 中如何工作？

**参考答案：**
1. `put(key, value)` 先调 `key.hashCode()`，高 16 位异或低 16 位（扰动函数，降低碰撞）
2. `(n - 1) & hash`（n 为 2 的幂）得到桶下标
3. 若桶内有对象，用 `equals` 比较：相等则覆盖 value，否则链式挂载
4. 所以 **hashCode 决定"找哪个桶"，equals 决定"是不是同一个 key"**

**追问：** 用可变对象做 HashMap 的 key 会怎样？→ key 的 hashCode 变化后桶位置错乱，数据"丢失"（还在，但 get 不到）。**key 必须不可变**（String、Integer 都是）。String 为什么高效？→ 不可变 + 缓存 hashCode 字段。

### 2.3 String 的 equals 为什么要先比较地址再比较内容？

```java
// String.equals 源码逻辑（简化）：
if (this == anObject) return true;              // 同一对象直接 true（性能优化）
if (anObject instanceof String) {               // 类型检查
    // 逐字符比较 char[]
}
```

**追问：** 为什么用 instanceof 而不是 getClass()？→ 兼容性考虑；且 String 是 final 类，两者等价，但 instanceof 对 null 安全。

---

## 3. String 专题

### 3.1 String 为什么设计成不可变？

**参考答案（四个角度）：**

| 角度 | 原因 |
|------|------|
| 常量池复用 | 不可变才能安全地共享引用（字符串常量池缓存） |
| 线程安全 | 不可变天然并发安全，无需加锁 |
| hashCode 缓存 | 不可变 → hashCode 稳定 → 缓存一次，HashMap 高效 |
| 安全 | 类加载、网络参数等敏感场景防止被篡改 |

**追问：** 真的完全不可变吗？→ 反射可以改 char[]（绕过 final），但破坏安全性，不是设计意图。"不可变"是约定 + 常规手段保证。

### 3.2 String / StringBuilder / StringBuffer 的区别？

| 对比 | String | StringBuilder | StringBuffer |
|------|--------|---------------|--------------|
| 可变性 | 不可变 | 可变 | 可变 |
| 线程安全 | 安全（不可变） | 不安全 | 安全（方法 synchronized） |
| 性能 | 拼接慢（频繁建新对象） | 最快 | 比 StringBuilder 慢 |
| 场景 | 常量、少量拼接 | **单线程拼接（推荐）** | 多线程共享拼接（极少用） |

```java
// 拼接性能对比：100 万次拼接
String s = ""; for(...) { s += "a"; }          // ❌ 每次创建新对象，O(n²)
StringBuilder sb = new StringBuilder(); ...     // ✅ 底层 char[] 扩容，O(n)
```

**追问：** JDK 为什么对 `+` 做了优化？→ 编译期常量折叠 + 字节码转 StringBuilder.append（但循环内仍低效）。concat 和 + 的区别？→ + 会 new StringBuilder，concat 是 char[] 拷贝，小串用 + 即可。

**记忆点：**
> 🎯 单线程拼接 → StringBuilder；多线程共享 → StringBuffer；大量字符串拼接一定不用 String 的 `+`（O(n²) 陷阱）。

### 3.3 字符串常量池与 intern()

**参考答案：**
- 字面量 `"abc"` 编译期进入常量池；运行时首次用到时在堆中创建并缓存到**字符串常量池**（JDK7+ 在堆中）
- `new String("abc")` 创建两个对象：堆对象 + 常量池对象（若池中没有）
- `intern()`：把字符串引用加入常量池，若已有则返回池中引用

```java
String s1 = "abc";                          // 池中创建
String s2 = new String("abc");              // 堆中新对象
s1 == s2.intern()   // true：intern 返回池中引用
```

**追问：** JDK6 vs JDK7 intern 的区别？→ JDK6 常量池在方法区（永久代），intern 是拷贝；JDK7+ 常量池在堆，intern 是引用。用 intern 做缓存有什么风险？→ 池中对象无法被 GC（池持引用），高并发场景 OOM 风险。

---

## 4. 值传递、装箱、异常、泛型

### 4.1 Java 是值传递还是引用传递？

**参考答案：** **只有值传递**。基本类型传值副本；引用类型传**引用（地址）的副本**，方法内对引用的重新赋值不影响外部，但通过引用修改对象内容会生效。

```java
public static void main(String[] args) {
    int a = 1; User u = new User("张三");
    change(a, u);
    System.out.println(a);     // 1（值传递，外部不变）
    System.out.println(u.name);// 李四（引用指向的对象内容被修改）
    // 但 u 的引用本身没有变
}
private static void change(int x, User u2) {
    x = 100; u2.name = "李四"; u2 = new User("王五");
}
```

**追问：** 那为什么"交换两个对象"写不出来？→ 因为形参引用重新赋值不影响实参（想要交换需包装容器或返回新值）。

### 4.2 自动装箱的陷阱？

**参考答案：**
- 基本类型 ↔ 包装类型自动转换（编译期调 valueOf/xxxValue）
- **Integer 缓存池**：-128~127 缓存，`Integer a = 127, b = 127; a == b` 为 true；128 以上为 false（new 出不同对象）

```java
Integer a = 100, b = 100;      a == b   // true（缓存池命中）
Integer c = 200, d = 200;      c == d   // false（各自 new）
Integer e = null; int f = e;            // ❌ 拆箱 NPE
```

**追问：** 拆箱 NPE 场景？→ 包装类型入参 + 自动拆箱时值为 null → NullPointerException（线上高频故障）。阿里规范为什么说包装类型做 POJO 属性？→ 区分"值为 0"与"值为空"。

### 4.3 受检异常 vs 非受检异常？

| 对比 | 受检异常（checked） | 非受检异常（unchecked） |
|------|---------------------|------------------------|
| 父类 | Exception | RuntimeException |
| 编译约束 | 必须捕获或声明 throws | 不强制 |
| 场景 | 可恢复的外部故障（IO、SQL） | 程序 bug（NPE、下标越界） |

**追问：** 什么异常适合自定义？→ 业务异常（如订单不存在）自定义非受检异常 + 全局处理器；Spring 中 @ControllerAdvice 统一处理。try-with-resources？→ JDK7+ 自动关闭 AutoCloseable 资源，finally 中手动 close 易遗漏。

### 4.4 泛型擦除与通配符？

**参考答案：**
- 泛型是编译期机制：类型检查后**擦除**（T → Object 或上界），运行时无泛型信息
- 泛型不支持基本类型：`List<int>` 非法（编译后是 Object[]，需要装箱）
- 通配符：`? extends T`（上界，读安全）、`? super T`（下界，写安全）— **PECS 原则**（Producer extends, Consumer super）

```java
List<? extends Number> list = new ArrayList<Integer>();
// list.add(1);  // ❌ 不能写：编译器不知道具体是 Integer 还是 Double

List<? super Integer> list2 = new ArrayList<Number>();
list2.add(1);     // ✅ 能写
// Number n = list2.get(0);  // ❌ 读出来是 Object
```

**追问：** 为什么擦除后还有类型信息？→ 字节码 Signature 属性保留签名（反射 getGenericType 可读）。桥方法？→ 继承泛型父类时编译器生成桥方法保持多态。

---

## 5. 反射与注解

### 5.1 反射的原理与性能问题？

**参考答案：**
- 运行时通过 Class 对象获取类的结构（字段/方法/构造器）并动态调用
- 获取 Class 三种方式：`类名.class`、`对象.getClass()`、`Class.forName("全限定名")`
- **性能问题**：动态解析方法、无内联优化，反射调用比直接调用慢 1~2 个数量级

```java
Class<?> clazz = Class.forName("com.example.User");
Method m = clazz.getDeclaredMethod("getName");
m.setAccessible(true);                       // 绕过 private
String name = (String) m.invoke(user);
```

**追问：** Spring 为什么能用反射？→ IOC 容器扫描注解 + 反射创建 Bean；Spring 框架自身缓存 Method 对象、JDK9+ 有方法句柄（MethodHandle）优化。什么场景会拒绝反射？→ 模块系统（模块未 open 时 IllegalAccessException）。

### 5.2 注解的原理？

**参考答案：**
- 注解本质是特殊接口，继承 `java.lang.annotation.Annotation`
- 元注解：@Target（使用范围）、@Retention（生命周期：SOURCE/CLASS/RUNTIME）
- **反射只读得到 @Retention(RUNTIME) 的注解**；SOURCE 的（如 @Override）编译期处理后被丢弃

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelColumn {
    String name();
    int order() default 0;
}
```

**追问：** 注解怎么生效？→ 三类处理者：编译期（APT/Lombok）、类加载期（字节码增强，AspectJ）、运行期（反射 + 框架，Spring 常见）。Spring 的 @Component 是如何被发现的？→ ClassPathBeanDefinitionScanner 扫描 + 反射读注解。

---

> 🎯 **核心要点**：基础模块的面试哲学是"每个概念准备两层" — 第一层定义准确（是什么 + 对比表），第二层机制清楚（为什么 + 反例/陷阱）。String、equals/hashCode、值传递、装箱缓存是追问率最高的四个点，务必讲透。

**下一模块**：[02-集合框架高频题与源码深挖](02-集合框架高频题与源码深挖.md) / **返回总览**：[00-Java核心面试大全总览](00-Java核心面试大全总览.md)
