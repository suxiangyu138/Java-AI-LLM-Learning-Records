# Java 8 新特性 核心知识点

## 一、核心大盘点
Java 8 是里程碑版本，核心升级：
**Lambda 表达式、函数式接口、Stream 流、Optional、新日期时间、接口默认方法、重复注解、HashMap 底层优化、CompletableFuture**

---

## 二、Lambda 表达式（重中之重）

### 1. 作用

简化**匿名内部类**写法，支持**函数式编程**，让代码更简洁。

### 2. 语法
```java
(参数列表) -> {方法体}
```
- 无参：`() -> System.out.println()`
- 单个参数可省略括号：`x -> x * 2`
- 单行方法体可省略 `{}` 与 `return`

### 3. 使用前提
**必须配合函数式接口**使用。

---

## 三、函数式接口

### 1. 定义
有且仅有**一个抽象方法**的接口。
标识注解：`@FunctionalInterface`

### 2. 四大核心内置函数式接口
1. **Consumer<T> 消费型**：入参无返回
    ```java
    void accept(T t);
    ```
2. **Supplier<T> 供给型**：无入参有返回
    ```java
    T get();
    ```
3. **Function<T,R> 函数型**：入参+返回
    ```java
    R apply(T t);
    ```
4. **Predicate<T> 断定型**：入参+布尔返回（条件判断）
    ```java
    boolean test(T t);
    ```

### 3. 扩展接口
`UnaryOperator`、`BiFunction`、`BiConsumer` 等。

---

## 四、方法引用
简化 Lambda 进一步写法，4 种类型：
1. 对象::实例方法
2. 类::静态方法
3. 类::实例方法
4. 构造器引用：类::new

---

## 五、Stream 流式操作（开发高频）

### 1. 作用
简化**集合、数组**的遍历、过滤、排序、聚合、转换，链式编程。

### 2. 三大阶段
1. **创建流**：集合/数组转 Stream
2. **中间操作**：延迟执行、可链式（filter、map、sorted、limit）
3. **终端操作**：触发执行（forEach、collect、count、max）

### 3. 常用中间操作
- `filter()`：过滤
- `map()`：类型转换/字段映射
- `sorted()`：排序
- `distinct()`：去重
- `limit()`/`skip()`：分页

### 4. 常用终端操作
- `collect()`：收集为集合（最常用）
- `forEach()`：遍历
- `count()`：统计数量
- `reduce()`：归约聚合

### 5. 并行流
`list.parallelStream()`
利用多线程并行处理，大数据量提升效率。

---

## 六、接口增强
1. **默认方法**：`default` 修饰，接口可带实现，解决接口升级兼容问题
2. **静态方法**：`static` 修饰，直接 接口名.方法 调用

```java
interface A {
    default void hello(){}
    static void test(){}
}
```

---

## 七、Optional 空指针解决方案

### 1. 目的
杜绝 `NullPointerException`，优雅处理空对象。

### 2. 核心方法
- `Optional.ofNullable(obj)`：构建可空 Optional
- `orElse()` / `orElseGet()`：为空给默认值
- `isPresent()`：判断非空
- `ifPresent()`：非空执行逻辑
- `orElseThrow()`：为空抛异常

---

## 八、全新日期时间 API（java.time 包）
替代老旧、线程不安全的 `Date、SimpleDateFormat`

### 1. 核心类
- `LocalDate`：只日期
- `LocalTime`：只时间
- `LocalDateTime`：日期+时间
- `Instant`：时间戳
- `DateTimeFormatter`：线程安全时间格式化

### 2. 特点
- 不可变、线程安全
- 丰富的加减、比较、格式化方法

---

## 九、CompletableFuture
Java8 全新异步编程类，完善 Future 缺陷：
- 支持回调、链式调用、多任务组合
- 非阻塞异步处理，JUC 异步核心

---

## 十、集合底层优化

### 1. HashMap（JDK1.8 重大优化）
1. 底层：**数组 + 链表 + 红黑树**
2. 链表长度 ≥8 且 数组长度≥64 → 转为红黑树
3. 红黑树节点数 ≤6 → 退化为链表
4. 插入头插改尾插，**解决循环链表死循环**

### 2. 其他集合
- ConcurrentHashMap：JDK8 改用 CAS+synchronized，替代分段锁

---

## 十一、其他小特性
1. **重复注解**：`@Repeatable`，同一位置可多次注解
2. **类型注解**：注解可作用在泛型、变量类型上
3. **Base64**：内置 `Base64` 工具类
4. **Nashorn 引擎**：支持 JS 执行（后期废弃）

---

## 十二、面试高频必背
1. Lambda 底层原理：生成**匿名内部类**、 invokedynamic 指令
2. 函数式接口定义 & 四大核心接口
3. Stream 中间操作与终端操作区别
4. HashMap JDK1.7 与 1.8 对比
5. 为什么要用 LocalDateTime 替代 Date
6. Optional 如何优雅避免空指针
7. 接口 default 方法使用场景

---
