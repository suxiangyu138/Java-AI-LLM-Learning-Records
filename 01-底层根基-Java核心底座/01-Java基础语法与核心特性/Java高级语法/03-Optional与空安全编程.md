# 03 - Optional 与空安全编程

> 🎯 NPE 是 Java 的头号运行时错误。Optional 不是银弹，但用对了能让空值处理从"到处 if-else"变成优雅的链式调用。在 AI 数据管道中，null 无处不在——掌握 Optional 是写出健壮代码的基础

---

## 目录

1. [创建 Optional](#1-创建-optional)
2. [安全消费与转换](#2-安全消费与转换)
3. [Optional 最佳实践](#3-optional-最佳实践)

---

## 1. 创建 Optional

```java
// 三种创建方式
Optional<String> opt1 = Optional.of("hello");          // 必不为 null
Optional<String> opt2 = Optional.ofNullable(maybeNull); // 可能为 null
Optional<String> opt3 = Optional.empty();              // 明确为空的容器

// 实战：从可能返回 null 的方法
public Optional<User> findById(Long id) {
    return Optional.ofNullable(db.query("SELECT * FROM users WHERE id = ?", id));
}
```

## 2. 安全消费与转换

```java
// 如果不为空 → 执行
userOpt.ifPresent(user -> System.out.println(user.getName()));

// 如果不为空 → 转换 → 否则给默认值
String name = userOpt
    .map(User::getName)
    .orElse("Unknown");

// 多步转换（类似 Stream.map）
String city = userOpt
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("Unknown");

// 抛出异常（如果确实不能为空）
User user = userOpt.orElseThrow(() -> new NotFoundException("用户不存在"));

// 惰性默认值（只有在 Optional 为空时才计算）
String result = opt.orElseGet(() -> expensiveDefault());

// ⚠️ orElse vs orElseGet
String x = opt.orElse(expensiveDefault());    // expensiveDefault() 总会被调用！
String y = opt.orElseGet(() -> expensiveDefault());  // 只在需要时调用 ✅

// 条件过滤
userOpt.filter(u -> u.getAge() >= 18)
        .ifPresent(u -> System.out.println("成年人"));

// 扁平化 (flatMap) — 避免 Optional<Optional<T>>
Optional<String> name = userOpt.flatMap(User::getOptionalName);
```

## 3. Optional 最佳实践

```java
// ✅ 好的用法
// 1. 方法的返回类型用 Optional 表示"可能无结果"
public Optional<Order> findLatestOrder(Long userId) { ... }

// 2. 链式处理空值
String email = userRepository.findById(id)
    .map(User::getEmail)
    .filter(e -> e.contains("@"))
    .orElse("no-email@example.com");

// 3. 配合 Stream 使用
List<String> names = users.stream()
    .map(User::getOptionalName)
    .flatMap(Optional::stream)      // Java 9+: Optional → Stream
    .toList();

// ❌ 坏的用法
// 1. 不要把 Optional 作为字段类型
class User { Optional<String> name; }  // ❌ 不可序列化，浪费内存

// 2. 不要把 Optional 作为方法参数
void process(Optional<String> maybe) { }  // ❌ 调用者还要包一层 Optional.of

// 3. 不要写 opt.get() 不检查（等于没解决 NPE！）
opt.get();  // ❌ 如果为空抛出 NoSuchElementException

// 4. 不要用 Optional 代替简单的 null 检查
if (opt.isPresent()) { opt.get().doSomething(); }  // ❌ 这和 if(x!=null) 一样
opt.ifPresent(Thing::doSomething);  // ✅ 这才是 Optional 的用法
```

## 核心要点回顾

- 创建：`Optional.of`(必非空) / `ofNullable`(可能空) / `empty`()
- 消费：`ifPresent` > `isPresent`+`get`
- 转换：`map`(1→1) / `flatMap`(1→Optional)
- 默认值：`orElseGet`(惰性) > `orElse`(总执行)
- Optional 只做返回值，不做字段/参数
- `Optional.stream()` (Java 9+) 桥接 Stream 世界

## 参考资料

1. Optional 官方文档
2. Effective Java 3rd — Item 55
