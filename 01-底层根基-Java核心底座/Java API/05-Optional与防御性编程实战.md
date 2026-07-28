# 05 Optional 与防御性编程实战

> Optional 不是一个"更安全的 null"，而是 API 设计者向调用方明确传达"这个返回值可能为空"的类型签名——用对地方消除 NPE，用错地方增加复杂度

---

## 📚 目录

1. [为什么需要 Optional](#1-为什么需要-optional)
2. [创建 Optional](#2-创建-optional)
3. [消费与转换](#3-消费与转换)
4. [兜底策略深度对比](#4-兜底策略深度对比)
5. [常见反模式](#5-常见反模式)
6. [Optional 与其他 API 的配合](#6-optional-与其他-api-的配合)
7. [Optional 的序列化问题](#7-optional-的序列化问题)
8. [防御性编程工具箱](#8-防御性编程工具箱)
9. [练习：用 Optional 重构 5 个场景](#9-练习用-optional-重构-5-个场景)
10. [总结](#10-总结)

---

## 1. 为什么需要 Optional

### 1.1 "Billion-Dollar Mistake"——null 引用的原罪

> I call it my billion-dollar mistake. It was the invention of the null reference in 1965. — **Tony Hoare**

null 引用是 Tony Hoare 在 1965 年设计 ALGOL W 时引入的，半个世纪以来，它导致了无数的崩溃、漏洞和安全问题。Java 从诞生起就继承了这一设计，而 NPE（`NullPointerException`）是 Java 开发者遇到最频繁的异常。

| 统计项 | 数据 |
|--------|------|
| Java 生产故障中由 NPE 引起 | 约 20%-30% |
| FindBugs/SpotBugs 最常见检测结果 | NPE 相关 |
| 防御性 null 检查代码在 Java 项目中的占比 | 5%-15% |
| 一次 NPE 定位成本 | 取决于 null 传播层数，有时需要回溯 5-10 个调用栈 |

### 1.2 NPE 是运行时异常——编译器不帮你

```java
// 编译器毫无怨言，运行到第三行就炸
User user = findUser(42);
String name = user.getName();          // 如果 user == null → NPE
String upper = name.toUpperCase();     // 如果 name == null → NPE
```

`NullPointerException` 继承自 `RuntimeException`，编译器不会强制你处理它。这与 checked exception 不同——后者至少提醒你"这里有风险"。

> 💡 如果一个错误编译器能提前发现，它就不叫"运行时异常"——但 Java 没有非空类型系统（不像 Kotlin 的 `String?` vs `String`），所以我们需要其他手段。

### 1.3 传统防御性手法与局限性

#### 手法一：if-else 嵌套（防御式编程里最笨的写法）

```java
// 反例：多层嵌套 → 代码复杂度指数级上升
public String getCityName(Long userId) {
    if (userId != null) {
        User user = userRepo.findById(userId);
        if (user != null) {
            Address address = user.getAddress();
            if (address != null) {
                return address.getCity();
            }
        }
    }
    return "未知城市";
}
```

**问题**：
- 逻辑和业务混在一起，可读性极差
- 漏掉一个 null 检查就炸
- 每多一层缩进，心智负担翻倍

#### 手法二：`@Nullable` / `@NonNull` 注解

```java
public @Nullable String getCityName(@NonNull Long userId) {
    // 告诉 IDE 和静态分析工具：参数不能 null，返回值可能 null
    // 但编译期仍不会强制你处理 null
}
```

**问题**：
- 注解仅仅是一种"约定"，不改变运行时行为
- 需要额外依赖（`javax.annotation` / `org.jetbrains.annotations`）
- 团队不遵守约定时形同虚设
- 反射、序列化等场景注解不再生效

#### 手法三：`Objects.requireNonNull`

```java
public User findUser(Long id) {
    Objects.requireNonNull(id, "id must not be null");
    // ... 正常逻辑
}
```

**问题**：
- 只能快速失败（fail-fast），不能做默认值或空安全链式操作
- "检测"和"处理"没有分开

#### 手法四：空对象模式（Null Object Pattern）

```java
public class NullUser extends User {
    @Override
    public String getName() { return "Guest"; }
}
```

**问题**：
- 需要为每个可能为空的类创建一个 Null 子类
- 无法区分"用户不存在"还是"默认游客"
- 不适合值对象（如金额、地址）

### 1.4 Optional 的定位

```text
Optional 不是什么
├── 不是 "更安全的 null"    → 因为 Optional 自己也可能为空（Optional.empty()）
├── 不是 "万能空安全方案"   → 序列化、字段、方法参数都不该用
└── 不是 "性能最优方案"     → 多了一层装箱，频繁调用路径不适合

Optional 是什么
├── 是 API 签名的一部分     → 看到 Optional<String> 就知道可能为空
├── 是函数式空安全组合子    → map / flatMap / orElse 链式组合
└── 是领域驱动设计中返回值的手段 → "可能没有值"
```

> 🎯 **核心要点**：Optional 的核心价值是 **类型层面的沟通**——`Optional<User>` 比 `User` 更能告诉 API 使用者"做好空值准备"。它不是解决 null 的银弹，而是改善 API 设计语义的工具。

---

## 2. 创建 Optional

### 2.1 三种创建方式

| 方法 | 语法 | 参数可以为 null 吗 | 行为 |
|------|------|-------------------|------|
| `Optional.empty()` | `Optional.empty()` | — | 创建一个空的 Optional 单例 |
| `Optional.of(value)` | `Optional.of(obj)` | **不可以** | 立即 NPE，适合明确非空的值 |
| `Optional.ofNullable(value)` | `Optional.ofNullable(obj)` | 可以 | 最常用，null 自动变 `Optional.empty()` |

```java
// 1. Optional.empty() — 单例，系统中只有一个空 Optional 实例
Optional<String> empty = Optional.empty();

// 2. Optional.of() — 参数为 null 立即抛 NPE
Optional<String> nonNull = Optional.of("hello");   // ✅
Optional<String> danger = Optional.of(null);       // ❌ 立即 NullPointerException

// 3. Optional.ofNullable() — 参数 null 自动转为 empty，最常用
Optional<String> maybe = Optional.ofNullable(getConfig("key")); // null → empty
```

### 2.2 Optional.of() 的典型场景——标记不可为空

```java
// 场景：参数经过前一层的防御性检查，确保非空后才构造 Optional
public Optional<String> parseResponse(@NonNull String raw) {
    // raw 已经过 Objects.requireNonNull 或框架校验，不可能为 null
    // 用 Optional.of 表达"此处绝不可能为空"的自证
    return Optional.of(doParse(raw));
}

// 对比：如果本该非空的值传进了 null，你想让它"尽快爆炸"而不是静默消失
public Optional<String> loadConfig(String path) {
    // path 传入 null 是个 bug，赶紧炸
    String content = Files.readString(Paths.get(path)); // 这里会炸
    return Optional.of(content);  // 逻辑上 content 一定非空
}
```

### 2.3 选型决策

```
值可能为 null 吗？
├── 否 → Optional.of(value)     ← 明确契约，快速失败
└── 是 → Optional.ofNullable(value)  ← 最常见的选择
    └── 值来源是一个调用结果，且你能接受空 → Optional.ofNullable()
        └── 但注意：如果调用结果本该非空却返回了 null → 这是上游 bug，用 ofNullable 会掩盖它

需要空 Optional 字面量？
└── Optional.empty()            ← 直接返回 empty，语义清晰
```

> ⚠️ **警告**：不要用 `Optional.ofNullable(null)` 来"手动"创建 empty——语义上告诉读者"这可能不空"，实际上却传入了 null，不如直接 `Optional.empty()`。

```java
// ❌ 反例：语义欺骗
return Optional.ofNullable(null);  // 读者以为"可能非空",实际上永远是空

// ✅ 正例：明确意图
return Optional.empty();           // 我就是空的
```

---

## 3. 消费与转换

### 3.1 map——值存在时转换

```java
// map 签名：<U> Optional<U> map(Function<? super T, ? extends U> mapper)
// 如果值存在则应用 mapper，并自动包装成 Optional；否则返回 empty

Optional<String> name = Optional.of("alice");

// 转换后自动包装
Optional<Integer> length = name.map(String::length);  // Optional.of(5)
// 等价于：
// if (name.isPresent()) { return Optional.of(name.get().length()); }
// else { return Optional.empty(); }

// 链式转换
String result = Optional.of("  hello  ")
    .map(String::trim)           // "hello"
    .map(String::toUpperCase)    // "HELLO"
    .orElse("DEFAULT");
```

**map 与普通 get 的区别**：

```java
// ❌ 反例：手动检查，手动 get，手动判断
if (userOpt.isPresent()) {
    User user = userOpt.get();
    String name = user.getName();
    if (name != null) {
        return name.toUpperCase();  // 仍可能空
    }
}
return "DEFAULT";

// ✅ 正例：map 将 null 安全地映射
return userOpt
    .map(User::getName)        // 自动处理 name 为 null 的情况
    .map(String::toUpperCase)  // 若上一步为 empty，这一层自动跳过
    .orElse("DEFAULT");
```

### 3.2 flatMap——展平嵌套 Optional

```java
// flatMap 签名：<U> Optional<U> flatMap(Function<? super T, ? extends Optional<? extends U>> mapper)
// 用于 mapper 本身返回 Optional 的情况——避免产生 Optional<Optional<T>>

// 场景：连续查询，每步都可能为空
// userRepo.findById → Optional<User>
// user.getAddress()  → Optional<Address>
// address.getCity()  → Optional<String>

// ❌ 用 map 会嵌套
Optional<Optional<String>> city = userRepo.findById(42L)
    .map(User::getAddressOpt);   // 返回 Optional<Optional<Address>>

// ✅ 用 flatMap 展平
Optional<String> city = userRepo.findById(42L)
    .flatMap(User::getAddressOpt)   // 返回 Optional<Address>
    .flatMap(Address::getCityOpt);  // 返回 Optional<String>

// 链式 flatMap 的等价命令式写法（你不想写的对吧？）
User user = userRepo.findById(42L);
if (user != null) {
    Optional<Address> addr = user.getAddressOpt();
    if (addr.isPresent()) {
        Optional<String> c = addr.get().getCityOpt();
        // ... 还没完
    }
}
```

### 3.3 filter——条件过滤

```java
// 值存在且满足 predicate → 保留当前 Optional
// 值不存在或不满足 predicate → 返回 empty

Optional<String> name = Optional.of("Alice");

// 只保留大写字母开头的名字
name.filter(n -> Character.isUpperCase(n.charAt(0)))
    .ifPresent(System.out::println);  // 输出 Alice

Optional<String> empty = Optional.empty();
empty.filter(n -> true)              // 即使 predicate 为 true，empty 还是 empty
      .ifPresent(System.out::println); // 无输出

// 实用场景：结合正则校验
Optional<String> email = Optional.ofNullable(user.getEmail());
email.filter(e -> e.matches("^[A-Za-z0-9+_.-]+@(.+)$"))
     .ifPresent(this::sendNotification);
```

### 3.4 ifPresent / ifPresentOrElse——值存在时消费

```java
// Java 8: ifPresent —— 值存在时执行 Consumer
Optional<String> name = Optional.of("Alice");
name.ifPresent(n -> System.out.println("Hello, " + n));

// Java 9+: ifPresentOrElse —— 值存在时执行 Consumer，否则执行 Runnable
name.ifPresentOrElse(
    n -> System.out.println("Hello, " + n),
    () -> System.out.println("No user found")
);

// 实际应用：缓存与数据库的 read-through
public String getUserDisplayName(Long id) {
    return cache.get(id)                  // Optional<String>
        .ifPresentOrElse(
            this::recordCacheHit,
            () -> loadFromDbAndCache(id)
        );
}
```

### 3.5 map vs flatMap 选型决策树

```
mapper 的返回值类型是什么？
├── 普通值（String, Integer, DTO） → map
│   └── 例子：user.map(User::getName)     ← getName() 返回 String
└── Optional 类型 → flatMap
    └── 例子：user.flatMap(User::getEmailOpt) ← getEmailOpt() 返回 Optional<String>
```

---

## 4. 兜底策略深度对比

### 4.1 四大兜底方法一览

| 方法 | 引入版本 | 参数类型 | 求值时机 | 返回值 |
|------|---------|---------|---------|--------|
| `orElse(defaultValue)` | Java 8 | `T` | **总是求值** | `T` |
| `orElseGet(Supplier)` | Java 8 | `Supplier<? extends T>` | 仅 empty 时求值 | `T` |
| `orElseThrow(exception)` | Java 8 | `Supplier<? extends X>` | 仅 empty 时求值 | `T` 或抛异常 |
| `or(supplier)` | Java 9 | `Supplier<? extends Optional<? extends T>>` | 仅 empty 时求值 | `Optional<T>` |

### 4.2 `orElse` vs `orElseGet`——最常见的性能陷阱

```java
// 陷阱：orElse 的参数总是求值，无论 Optional 是否有值

// 场景一：计算默认值开销大
Optional<String> cached = getFromCache("config");

// ❌ 无论 cache hit 与否，expensiveDefault() 都执行了
String result = cached.orElse(expensiveDefault());  // expensiveDefault() 总是被调用

// ✅ 只有 cache miss 时才执行 expensiveDefault()
String result = cached.orElseGet(() -> expensiveDefault());

// 场景二：方法的副作用
// ❌ createDefaultUser() 每次都被调用，可能创建了无用的对象或写入日志
User user = optionalUser.orElse(createDefaultUser());
```

**反编译验证**：

```java
// 源码
optional.orElse(fallback());
optional.orElseGet(() -> fallback());

// 反编译后的字节码（简化）
// orElse: fallback() 被编译为方法调用，作为参数传递（立即求值）
LDC "optional"
INVOKESTATIC Optional.of
ALOAD fallbackResult      ← fallback() 先调用了
INVOKEVIRTUAL Optional.orElse

// orElseGet: Supplier 被编译为 lambda 对象，方法体推迟执行
LDC "optional"
INVOKESTATIC Optional.of
INVOKEDYNAMIC get()       ← lambda 表达式
INVOKEVIRTUAL Optional.orElseGet
```

**基准测试（JMH 简化数据）**：

| 场景 | 方法 | 吞吐量（ops/ms） | 说明 |
|------|------|:--------------:|------|
| Optional 有值，默认值简单（"abc"） | orElse | ~320 | 无差异 |
| Optional 有值，默认值简单（"abc"） | orElseGet | ~315 | 无差异 |
| Optional 有值，默认值昂贵（DB查询） | orElse | ~45 | **每次执行昂贵操作** |
| Optional 有值，默认值昂贵（DB查询） | orElseGet | ~310 | 仅 empty 时执行 |

> 🎯 **规则**：当默认值是**简单的字面量或常量**（字符串、数字、枚举），用 `orElse`；当默认值需要**方法调用、计算、IO 操作**才能得到，用 `orElseGet`。

```java
// ✅ 或Else：常数，用 orElse
String name = optName.orElse("Guest");

// ✅ orElseGet：需要计算或方法调用
User user = optUser.orElseGet(() -> userService.createGuestUser());

// ✅ orElseGet：超长字符串拼接
String msg = optMsg.orElseGet(() -> "Fallback message with " + data.size() + " items");
```

### 4.3 `orElseThrow`——空时抛异常

```java
// Java 8 写法
String value = optional.orElseThrow(() -> new IllegalArgumentException("value is required"));

// 常用场景：从 Optional 中取出值，因为业务逻辑上此处不应为空
// 如果为空，说明上游有 bug，快速失败
public Order getOrderOrThrow(Long orderId) {
    return orderRepo.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException("Order " + orderId + " not found"));
}

// 使用场景：反序列化/解析时，必须要有值
public Currency parseCurrency(String code) {
    return Optional.ofNullable(Currency.getInstance(code))
        .orElseThrow(() -> new IllegalArgumentException("Unknown currency: " + code));
}

// Java 10+: 可以用 NoSuchElementException 的简写（不推荐，不够具象）
String value = optional.orElseThrow();  // 等效于 optional.get()
```

### 4.4 `or`——Java 9+ 链式兜底

```java
// or 签名：Optional<T> or(Supplier<? extends Optional<? extends T>> supplier)
// 当前值为 empty 时，执行 supplier 产生另一个 Optional
// 当前值非空时，返回当前 Optional（与 orElseGet 不同，它返回 Optional 而非裸值）

// 场景：多级缓存
public Optional<String> getConfig(String key) {
    return localCache.get(key)          // 一级缓存
        .or(() -> redisCache.get(key))  // 二级缓存
        .or(() -> dbCache.get(key));    // 数据库
}

// 对比：没有 or 时，你不得不这样写
public String getConfigOld(String key) {
    Optional<String> val = localCache.get(key);
    if (val.isPresent()) return val.get();
    val = redisCache.get(key);
    if (val.isPresent()) return val.get();
    return dbCache.get(key).orElse("default");
}

// or 与 orElseGet 的区别
// orElseGet: Optional<T> → T （拆箱）
// or: Optional<T> → Optional<T> （不拆箱，链式）
Optional<String> a = Optional.of("A");
Optional<String> b = Optional.of("B");

// or：a 有值，b 不会执行
Optional<String> result = a.or(() -> b);  // Optional.of("A")

// orElseGet：a 有值，b 不执行，但返回 T
String result2 = a.orElseGet(() -> b.get());  // "A"
```

### 4.5 兜底策略选型

```text
当前 Optional empty 时你的意图是？
├── 给一个默认值
│   ├── 默认值是字面量/常量 → orElse(value)
│   ├── 默认值需要计算/IO   → orElseGet(Supplier)
│   └── 默认值是另一个 Optional → or(Supplier)  (Java 9+)
├── 抛异常
│   └── 异常类型明确        → orElseThrow(Supplier)
└── 什么都不做（保持 Optional）→ 链式调用继续传递
```

---

## 5. 常见反模式

### 5.1 反模式一：`optional.get()` 不检查 isPresent

```java
// ❌ 反模式：get() 不检查，跟直接调用 obj.method() 一样危险
Optional<User> user = findUser(42);
User u = user.get();           // 如果 empty → NoSuchElementException
String name = u.getName();     // NPE 换了个马甲

// 本质上这跟老式的 null 检查没区别，只是异常类型变了
User oldUser = findUserOld(42);
String oldName = oldUser.getName();  // NPE
```

> 💡 `Optional.get()` 的设计意图是：**在你已经通过 `isPresent()`、`orElse`、`orElseThrow` 等手段确保值存在之后**，从 Optional 中取出值。不要把它当成 getter 直用。

### 5.2 反模式二：`isPresent` + `get` → 应改为 ifPresent / 链式调用

```java
// ❌ 反模式：ifPresent 检查 + get + 再处理
if (optional.isPresent()) {
    String val = optional.get();
    // 对 val 做各种操作
    System.out.println("Value: " + val);
    sendToQueue(val);
    metrics.record(val.length());
}

// ✅ 正例：map 链式处理，最后 ifPresent 消费
optional
    .map(val -> {
        System.out.println("Value: " + val);
        return val;
    })
    .map(val -> {
        sendToQueue(val);
        return val;
    })
    .ifPresent(val -> metrics.record(val.length()));

// 如果只是为了消费值，直接用 ifPresent（更简洁）
optional.ifPresent(val -> {
    System.out.println("Value: " + val);
    sendToQueue(val);
    metrics.record(val.length());
});
```

```java
// 更极端的例子：多层 isPresent+get
// ❌ 反模式
if (userOpt.isPresent()) {
    User user = userOpt.get();
    Optional<String> email = user.getEmail();
    if (email.isPresent()) {
        String e = email.get();
        if (e.endsWith("@company.com")) {
            sendInternalNotice(e);
        }
    }
}

// ✅ 正例：flatMap + filter + ifPresent
userOpt
    .flatMap(User::getEmail)
    .filter(e -> e.endsWith("@company.com"))
    .ifPresent(this::sendInternalNotice);
```

### 5.3 反模式三：Optional 作为字段 / 方法参数

**Brian Goetz（Java 语言架构师）明确说**：Optional 的设计意图是返回值类型，不是字段类型，也不是参数类型。

```java
// ❌ 反模式：Optional 作为字段
public class User {
    private Optional<String> email = Optional.empty();  // ❌ 不推荐
    private Optional<Address> address;                  // ❌ 不推荐
}

// 问题：
// 1. Optional 没实现 Serializable → 序列化会炸（见第 7 节）
// 2. 字段默认值语义不明确：null Optional 和 Optional.empty() 分别代表什么？
// 3. 性能：多了一层对象包装，每个字段都变成 Optional 增加 JVM 内存压力

// ✅ 正例：用 @Nullable + 空判断，或者直接用 null
public class User {
    private String email;       // null 表示未设置
    private Address address;   // null 表示无地址
}
```

```java
// ❌ 反模式：Optional 作为方法参数
public void saveUser(String name, Optional<Integer> age, Optional<String> email) {
    // 调用方必须构造 Optional 对象：saveUser("Alice", Optional.of(30), Optional.empty())
    // 调用方痛苦，代码难看
}

// ✅ 正例：方法重载 + 清晰语义
public void saveUser(String name) {
    saveUser(name, null, null);
}
public void saveUser(String name, Integer age) {
    saveUser(name, age, null);
}
public void saveUser(String name, Integer age, String email) {
    // 内部用 null 检查
}

// ✅ 正例：用 @Nullable 注解（工具类或内部 API 可以这样）
public void saveUser(String name, @Nullable Integer age, @Nullable String email) {
    // 方法内对 Optional 参数做处理
}
```

### 5.4 反模式四：`List<Optional<T>>`

```java
// ❌ 反模式：Optional 的集合
List<Optional<String>> names = getNames(); // List 中有些元素是 empty

// 处理时变得异常复杂
for (Optional<String> name : names) {
    name.ifPresent(System.out::println);
}

// ✅ 正例：用空的集合表示"无"，或者用 filter 过滤
List<String> names = getNonNullNames(); // 不含 null，不含 Optional

// 如果上游确实可能返回 null 值，过滤掉就是
List<String> cleanNames = rawList.stream()
    .filter(Objects::nonNull)
    .collect(Collectors.toList());

// 如果要区分"值不存在"和"值存在但为空字符串"
// 用哨兵值或自定义类型，不要用 Optional 装箱
public enum Presence { ABSENT, EMPTY_STRING, PRESENT }
```

### 5.5 反模式五：`optional.orElse(null)` 自废武功

```java
// ❌ 反模式：orElse(null) 完全违背了使用 Optional 的初衷
String value = optional.orElse(null);
// 你又回到了 null 的世界——需要再做 null 检查
if (value != null) { ... }

// 等价于：努力半天回到原点

// ✅ 正例：如果真的需要 null 去传递给遗留 API
String value = optional.orElse(null);
// 这种场景仅限于：调用方是第三方库/遗留代码，它的 API 必须传 null 表示"未设置"
// 而且这个 null 只在一个局部范围内，不传播

// ✅ 更好的做法：在边界处转换
// 在 Service 层保留 Optional，在 Controller 层转成 null 给 JSON 序列化
// 但尽量少用
```

### 5.6 反模式汇总表

| 反模式 | 危害 | 正确做法 |
|--------|------|---------|
| `opt.get()` 不检查 | 跟 NPE 一样容易炸 | `orElse` / `orElseGet` / `orElseThrow` |
| `isPresent` + `get` | 繁琐，放弃链式 | `ifPresent` / `map` / `flatMap` |
| Optional 做字段 | 序列化问题，性能开销 | `@Nullable` + null |
| Optional 做参数 | 调用方痛苦，违反设计意图 | 方法重载 / `@Nullable` |
| `List<Optional<T>>` | 集合语义冲突 | `filter(Objects::nonNull)` 或自定义枚举 |
| `orElse(null)` | 自废武功 | 用正确的兜底方法 |

---

## 6. Optional 与其他 API 的配合

### 6.1 Stream + Optional::stream（Java 9+）

```java
// Java 8 时代：从 Optional 集合中过滤出非空值，需要手动 filter + map
List<Optional<String>> items = List.of(
    Optional.of("A"),
    Optional.empty(),
    Optional.of("B"),
    Optional.empty()
);

// Java 8 做法：两步
List<String> result = items.stream()
    .filter(Optional::isPresent)
    .map(Optional::get)
    .collect(Collectors.toList());  // [A, B]

// Java 9+ 做法：一步，用 Optional::stream
List<String> result = items.stream()
    .flatMap(Optional::stream)      // 如果 empty 返回空流，否则返回单元素流
    .collect(Collectors.toList());  // [A, B]
```

```java
// 进阶场景：查数据库，过滤不存在的用户
List<Long> userIds = List.of(1L, 2L, 3L);
// 批量查询
List<User> existingUsers = userIds.stream()
    .map(userRepo::findById)          // Stream<Optional<User>>
    .flatMap(Optional::stream)        // 过滤掉不存在的
    .collect(Collectors.toList());
```

### 6.2 与 Map.computeIfAbsent 配合

```java
// 场景：缓存模式，Optional 作为缓存值类型
// 注意：用 null 表示"未缓存"，Optional.empty() 表示"已缓存但不存在"

// ❌ 问题：直接用 Optional 作为 Map 值，遇到 Value is null 的问题
Map<String, Optional<User>> cache = new HashMap<>();

// computeIfAbsent 不接受 null 作为 value，但 Optional.empty() 是可以的
// 这里有个细节：如果 Optional 是字段值，map.computeIfAbsent 没问题

// ✅ 正确用法
public Optional<User> getUserFromCache(String id) {
    return cache.computeIfAbsent(id, this::loadUser);
}

private Optional<User> loadUser(String id) {
    User user = userRepo.findById(id);
    return Optional.ofNullable(user);  // null → empty
}
```

### 6.3 与 Spring Data JPA 配合

Spring Data JPA 从 2.x 版本开始，Repository 方法返回 `Optional` 是天生的：

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);  // ✅ 原生返回 Optional
    // 注意：findById 返回 empty 表示数据库中无记录
}

// Service 层的配合
@Service
public class UserService {

    public Optional<User> findUser(Long id) {
        return userRepository.findById(id);
    }

    public User getUserOrThrow(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User getUserWithDefault(Long id) {
        return userRepository.findById(id)
            .orElseGet(User::createGuest);
    }

    // Spring Data 生成的查询方法
    // 如果不清楚是否会返回 null，用 Optional 包装
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
}
```

### 6.4 Guava Optional vs Java Optional

```java
// Google Guava 在 Java 8 之前就提供了 com.google.common.base.Optional

// 不同点对比
// ┌─────────────────────┬──────────────────────────┬────────────────────────────┐
// │ 特性                │ Java Optional (java.util) │ Guava Optional             │
// ├─────────────────────┼──────────────────────────┼────────────────────────────┤
// │ 引入版本            │ Java 8                   │ Guava 1.0                  │
// │ 实现 Serializable  │ ❌ 否                     │ ✅ 是 (序列化安全)          │
// │ 转换方法            │ map / flatMap / filter   │ transform (仅一个)          │
// │ Stream 互转         │ Optional::stream (Java9) │ asSet() → Set<T>           │
// │ 兜底                │ orElse / orElseGet / or  │ or / orNull                │
// │ 是否为 final 类     │ ✅ final                 │ abstract class              │
// │ 对 null 的容忍度    │ 明确不接受 null in of()  │ 部分方法接受  (fromNullable)│
// └─────────────────────┴──────────────────────────┴────────────────────────────┘

// 如果你是 Java 8+ 项目，优先用 java.util.Optional（标准库，无额外依赖）
// 项目已经用了 Guava，不冲突，在 Guava 返回值用 Guava Optional，自己 API 用标准库
// RPC/序列化场景下 Guava Optional 有优势（它实现了 Serializable）
```

---

## 7. Optional 的序列化问题

### 7.1 Optional 未实现 Serializable

```java
// 查看 JDK 源码：Optional 的定义根本没有实现 Serializable
public final class Optional<T> {
    // ...
    // 没有 implements Serializable
}

// 这意味着：
// ❌ 不能作为 RPC 调用的返回值（Dubbo、gRPC 序列化会失败）
// ❌ 不能放进 Redis / Memcached 缓存
// ❌ 不能作为消息队列的消息体字段
// ❌ 不能直接存在 Session 中（Tomcat Session 序列化）
```

### 7.2 序列化场景下的替代方案

```java
// 方案一：DTO 中不用 Optional，用普通的 null 字段
public class UserDTO implements Serializable {
    private String name;
    private String email;  // null 表示可选

    // getter 在 Service 层可以用 Optional 包装
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }
}

// 方案二：用 Guava Optional（实现了 Serializable）
// 但注意跨系统互操作时 Guava 是私有依赖

// 方案三：自定 Wrapper（极少用，不推荐）
public class Nullable<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final T value;

    private Nullable(T value) { this.value = value; }
    public static <T> Nullable<T> of(T value) { return new Nullable<>(value); }
    public static <T> Nullable<T> empty() { return new Nullable<>(null); }
    public T orElse(T other) { return value != null ? value : other; }
}
```

### 7.3 最佳实践

```text
序列化/跨进程通信 → 用 null 或 Guava Optional
数据库实体         → 用 null（JPA 字段）
DTO               → 用 null（JSON 反序列化时 Optional 找不到合适的构造器）
内部 Service 层   → 用 Optional（改善 API 语义）
Controller 返回值 → 用 Optional（JSON 序列化时 empty → null）
```

```java
// Controller 层的 Optional → JSON
@RestController
public class UserController {
    @GetMapping("/user/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return userService.findUser(id)
            .map(UserDTO::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

// UserDTO 中避免 Optional
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private Integer age;       // null → JSON 中不出现或缺省
}
```

---

## 8. 防御性编程工具箱

### 8.1 武器库总览

| 工具 | 使用场景 | 引入方式 |
|------|---------|---------|
| `Optional<T>` | **方法返回值**：可能为空，需调用方感知 | Java 8 标准库 |
| `Objects.requireNonNull` | **方法参数校验**：参数不可为空 | Java 7 标准库 |
| `@Nullable` / `@NonNull` | **注解契约**：辅助 IDE 检测和静态分析 | `javax.annotation` / `org.jetbrains.annotations` |
| 不可变对象（Immutable） | **避免对象中途变为 null** | 构造函数全参 + final 字段 |
| 空对象模式（Null Object） | **替代 "不存在" 语义** | 实现一个什么都不做的子类 |
| `Objects.isNull` / `nonNull` | **Stream 过滤** | Java 8 标准库 |
| `requireNonNullElse` / `requireNonNullElseGet` | **参数兜底**（Java 9+） | Java 9 标准库 |

### 8.2 Objects.requireNonNull —— 快速失败

```java
// 使用场景：构造器、setter 中校验参数
public class Order {
    private final String orderId;
    private final BigDecimal amount;

    public Order(String orderId, BigDecimal amount) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
    }
}

// Java 9+ 额外加了两个便捷方法
String result = Objects.requireNonNullElse(str, "default");        // 类似 orElse
String result = Objects.requireNonNullElseGet(str, () -> loadDefault()); // 类似 orElseGet
```

### 8.3 `@Nullable` / `@NonNull` 注解

```java
// 不同的包提供类似功能
import javax.annotation.Nullable;
import javax.annotation.Nonnull;

// 或者（JetBrains/IntelliJ 常用）
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

public class UserService {
    private static final User GUEST = new User("Guest");

    // 告诉调用方：参数不能 null，返回值一定非 null
    public @NotNull User findOrCreate(@NotNull Long id) {
        return repo.findById(id)
            .orElse(GUEST);
    }

    // 告诉调用方：返回值可能 null
    public @Nullable User findUser(@NotNull Long id) {
        return repo.findById(id).orElse(null);
    }
}

// 缺点：不改变运行时行为，依赖工具链
// 优点：零运行时开销，IDE 实时提示
```

### 8.4 不可变对象

```java
// 不可变对象天然防御 null——构造函数拒绝 null，之后字段永不改变
public final class User {
    private final String name;  // final 保证不会在生命周期内变为 null
    private final String email;

    public User(String name, String email) {
        this.name = Objects.requireNonNull(name, "name");
        this.email = email;  // email 可以是 null（可选字段）
    }

    // 不给 setter，只有 getter
    public String getName() { return name; }
    public Optional<String> getEmail() { return Optional.ofNullable(email); }
}
```

### 8.5 空对象模式（Null Object Pattern）

```java
// 场景：某些业务逻辑中，"不存在"本身是一个有效的业务状态
// 比如：未登录用户 = Guest 用户

// 定义空对象
public class NullOrder extends Order {
    public NullOrder() {
        super("N/A", BigDecimal.ZERO);
    }

    @Override
    public boolean isNull() { return true; }

    @Override
    public String getStatus() { return "NONE"; }
}

// 使用时
public Order findOrderOrNullObj(Long id) {
    return repo.findById(id).orElse(new NullOrder());
}

// 优点：避免处处检验 null
// 缺点：需要额外创建 Null 子类；无法区分"真的不存在"和"刚刚新建"；
// 不适合复杂业务场景
```

### 8.6 防御性编程最佳实践清单

```text
编写新方法时自问：
├── 这个方法的返回值可能为空吗？
│   ├── 是 → 返回 Optional<T>
│   └── 否 → 返回 T，并在方法内确保非空
├── 参数可以为空吗？
│   ├── 是 → @Nullable + 文档 + 内部空处理
│   └── 否 → Objects.requireNonNull + @NonNull
└── 字段可以为空吗？
    ├── 是 → 用 @Nullable 注解，设计时明确 null 语义
    └── 否 → 构造函数强制 non-null 参数 + final 字段

调用别人方法时：
├── 看到 Optional<T> 返回值 → 准备好 empty 的处理
├── 看到 @Nullable → 做好 null 检查
└── 看到 T（无注释）→ 不要假设非空，重要路径仍做防御
```

---

## 9. 练习：用 Optional 重构 5 个场景

### 场景 1：传统 if-else 嵌套 → Optional 链式

```java
// ---------- 原始代码 ----------
// 题目：获取用户所属城市的显示名称
public String getCityDisplayName(Long userId) {
    if (userId == null) {
        return "Unknown";
    }
    User user = userRepo.findById(userId);
    if (user == null) {
        return "Unknown";
    }
    Address address = user.getAddress();
    if (address == null) {
        return "Unknown";
    }
    String city = address.getCity();
    if (city == null || city.isBlank()) {
        return "Unknown";
    }
    return city;
}

// ---------- 重构后的参考答案 ----------
public String getCityDisplayName(Long userId) {
    return Optional.ofNullable(userId)
        .flatMap(id -> userRepo.findById(id))           // findById 返回 Optional<User>
        .map(User::getAddress)                           // Address（可能 null）
        .map(Address::getCity)                           // String（可能 null/blank）
        .filter(city -> !city.isBlank())
        .orElse("Unknown");
}

// 注意：如果 userRepo.findById 返回 User 而非 Optional<User>，改写：
public String getCityDisplayName(Long userId) {
    return Optional.ofNullable(userId)
        .map(userRepo::findById)                        // 返回 User（可能 null）
        .map(User::getAddress)
        .map(Address::getCity)
        .filter(city -> !city.isBlank())
        .orElse("Unknown");
}
```

### 场景 2：保险报价——多步验证

```java
// ---------- 原始代码 ----------
// 计算保险费：获取用户年龄、驾驶证类型、驾驶记录后给出报价
public BigDecimal calculatePremium(Long userId) {
    if (userId == null) return BigDecimal.ZERO;

    User user = userRepo.findById(userId);
    if (user == null) return BigDecimal.ZERO;

    Integer age = user.getAge();
    if (age == null || age < 18) return BigDecimal.ZERO;

    License license = user.getLicense();
    if (license == null || !"FULL".equals(license.getType())) return BigDecimal.ZERO;

    Record drivingRecord = license.getDrivingRecord();
    if (drivingRecord == null) return BigDecimal.ZERO;

    int violations = drivingRecord.getViolations();
    if (violations > 3) return BigDecimal.valueOf(2000);
    if (violations > 1) return BigDecimal.valueOf(1500);
    return BigDecimal.valueOf(1000);
}

// ---------- 重构后的参考答案 ----------
public BigDecimal calculatePremium(Long userId) {
    return Optional.ofNullable(userId)
        .flatMap(id -> findUserById(id))            // 返回 Optional<User>
        .filter(user -> {
            Integer age = user.getAge();
            return age != null && age >= 18;
        })
        .map(User::getLicense)
        .filter(license -> "FULL".equals(license.getType()))
        .map(License::getDrivingRecord)
        .map(record -> {
            int violations = record.getViolations();
            if (violations > 3) return BigDecimal.valueOf(2000);
            if (violations > 1) return BigDecimal.valueOf(1500);
            return BigDecimal.valueOf(1000);
        })
        .orElse(BigDecimal.ZERO);
}
```

### 场景 3：配置加载——多级兜底

```java
// ---------- 原始代码 ----------
// 加载配置：环境变量 → 系统属性 → 配置文件 → 默认值
public String loadConfig(String key) {
    // 从环境变量读取
    String value = System.getenv(key);
    if (value != null && !value.isEmpty()) {
        return value;
    }

    // 从系统属性读取
    value = System.getProperty(key);
    if (value != null && !value.isEmpty()) {
        return value;
    }

    // 从配置文件读取
    value = configFile.getProperty(key);
    if (value != null && !value.isEmpty()) {
        return value;
    }

    // 返回默认值
    return "default-" + key;
}

// ---------- 重构后的参考答案（Java 9+） ----------
public String loadConfig(String key) {
    return Optional.ofNullable(System.getenv(key))
        .filter(v -> !v.isEmpty())
        .or(() -> Optional.ofNullable(System.getProperty(key))
            .filter(v -> !v.isEmpty()))
        .or(() -> Optional.ofNullable(configFile.getProperty(key))
            .filter(v -> !v.isEmpty()))
        .orElse("default-" + key);
}

// 或者更简洁的写法（Java 9+）
public String loadConfig(String key) {
    return Stream.<Supplier<Optional<String>>>of(
                () -> Optional.ofNullable(System.getenv(key)),
                () -> Optional.ofNullable(System.getProperty(key)),
                () -> Optional.ofNullable(configFile.getProperty(key))
            )
            .map(Supplier::get)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElse("default-" + key);
}
```

### 场景 4：批量处理——过滤有效实体

```java
// ---------- 原始代码 ----------
// 批量查询并处理用户，只处理有效用户
public void processUsers(List<Long> userIds) {
    for (Long id : userIds) {
        User user = userRepo.findById(id);
        if (user == null) continue;
        if (user.getStatus() == null) continue;

        String email = user.getEmail();
        if (email == null) continue;

        if (!email.endsWith("@company.com")) continue;

        sendNotification(user);
    }
}

// ---------- 重构后的参考答案 ----------
public void processUsers(List<Long> userIds) {
    userIds.stream()
        .map(userRepo::findById)          // Stream<User>（可能 null）
        .filter(Objects::nonNull)
        .filter(u -> u.getStatus() != null)
        .map(User::getEmail)
        .filter(Objects::nonNull)
        .filter(email -> email.endsWith("@company.com"))
        .forEach(this::sendNotification);
}

// 或者如果 userRepo.findById 返回 Optional<User>
public void processUsers(List<Long> userIds) {
    userIds.stream()
        .map(userRepo::findById)          // Stream<Optional<User>>
        .flatMap(Optional::stream)        // Java 9+，过滤 empty
        .filter(u -> u.getStatus() != null)
        .map(User::getEmail)
        .filter(Objects::nonNull)
        .filter(email -> email.endsWith("@company.com"))
        .forEach(this::sendNotification);
}
```

### 场景 5：异常转换——Optional 统一异常类型

```java
// ---------- 原始代码 ----------
// 支付接口：不同来源的错误用不同异常
public void processPayment(Long orderId, String paymentMethod) {
    if (orderId == null) {
        throw new IllegalArgumentException("orderId cannot be null");
    }

    Order order = orderRepo.findById(orderId);
    if (order == null) {
        throw new OrderNotFoundException("Order not found: " + orderId);
    }

    Payment payment = paymentService.create(paymentMethod);
    if (payment == null) {
        throw new PaymentException("Failed to create payment: " + paymentMethod);
    }

    boolean success = paymentService.charge(order, payment);
    if (!success) {
        throw new PaymentException("Payment failed for order: " + orderId);
    }
}

// ---------- 重构后的参考答案 ----------
public void processPayment(Long orderId, String paymentMethod) {
    // 用 Objects.requireNonNull 统一参数校验
    Objects.requireNonNull(orderId, "orderId cannot be null");

    Order order = Optional.ofNullable(orderRepo.findById(orderId))
        .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

    Payment payment = Optional.ofNullable(paymentService.create(paymentMethod))
        .orElseThrow(() -> new PaymentException("Failed to create payment: " + paymentMethod));

    if (!paymentService.charge(order, payment)) {
        throw new PaymentException("Payment failed for order: " + orderId);
    }
}

// 或者将所有异常链式组合（用 map 做 side-effect 处理不推荐，所以这种场景偏好用 orElseThrow）
```

> 💡 **练习建议**：先自己尝试用 Optional 重写以上 5 个场景，再对比参考答案。重点不是"答案唯一"，而是体会 Optional 如何使用链式声明式的方式替代命令式的 null 检查。

---

## 10. 总结

### 10.1 Optional 使用决策树

```text
看到 null 或可能为空的值时：
├── 这是方法返回值吗？
│   ├── 是 → 返回 Optional<T>
│   │   ├── 需要默认值 → orElse / orElseGet / or (Java 9+)
│   │   ├── 需要抛异常 → orElseThrow
│   │   └── 需要转换 → map / flatMap / filter
│   └── 否 → 见下面
├── 这是方法参数吗？
│   └── 不用 Optional，用 @Nullable + Objects.requireNonNull
├── 这是类字段吗？
│   └── 不用 Optional，用 @Nullable + 设计 null 语义
├── 需要在集合中存储空值吗？
│   └── 不用 Optional，用 filter 过滤 null 或用空集合
└── 需要序列化/RPC 传输吗？
    └── 不用 Optional，用 null 字段 + DTO + Guava Optional（可选）
```

### 10.2 一句话法则

```
用 Optional 改善 API 契约，不用 Optional 治理内部 null——内部 null 治理靠 Objects.requireNonNull + @Nullable
```

### 10.3 各版本特性小结

| 版本 | Optional 相关特性 |
|:----:|-----------------|
| Java 8 | 引入 `Optional`、`orElse`、`orElseGet`、`orElseThrow`、`map`、`flatMap`、`filter`、`ifPresent` |
| Java 9 | 引入 `or(Supplier)`、`ifPresentOrElse`、`Optional::stream`、`stream()` |
| Java 10 | `orElseThrow()` 无参版本（等价于 `get()` 但有更好的语义） |
| Java 11 | 无新增 Optional API |
| Java 16 | `Stream.toList()` 配合 Optional::stream 更简洁 |

---

**上一模块**：[04-Java时间日期API深度解析](./04-Java时间日期API深度解析.md)

**下一模块**：[06-Java常用工具类大全](./06-Java常用工具类大全.md)

**返回总览**：[00-Java API知识体系总览](./00-Java API知识体系总览.md)
