# 21 - Optional与日期时间API
> 定位：掌握Optional的创建、转换、判空与安全取值链，告别NullPointerException；精通java.time（LocalDate/LocalTime/LocalDateTime）替代传统Date/Calendar

## 目录
1. [Optional：优雅的空值处理](#1-optional优雅的空值处理)
2. [Optional的创建方式](#2-optional的创建方式)
3. [Optional的转换与消费：map、flatMap、ifPresent](#3-optional的转换与消费mapflatmapifpresent)
4. [Optional的判空与默认值：isPresent、orElse、orElseGet、orElseThrow](#4-optional的判空与默认值ispresentorelseorelsegetorelsethrow)
5. [Optional 最佳实践与反模式](#5-optional-最佳实践与反模式)
6. [传统Date/Calendar的痛点](#6-传统datecalendar的痛点)
7. [java.time核心类：LocalDate、LocalTime、LocalDateTime](#7-javatime核心类localdatelocaltimelocaldatetime)
8. [Instant、Duration与Period](#8-instantduration与period)
9. [DateTimeFormatter格式化与解析](#9-datetimeformatter格式化与解析)
10. [ZonedDateTime与时区处理](#10-zoneddatetime与时区处理)
11. [传统API与java.time的互操作](#11-传统api与javatime的互操作)
12. [面试高频考点](#12-面试高频考点)

---

## 1. Optional：优雅的空值处理

传统防御式编程充斥着`if (x != null)`嵌套，冗长且易漏。`Optional<T>`是JDK 8引入的容器，用函数式风格安全处理可能为null的值。

```java
// 传统多层判空（冗长易错）
public String getCityFromUser(User user) {
    if (user != null) {
        Address address = user.getAddress();
        if (address != null) {
            City city = address.getCity();
            if (city != null) return city.getName();
        }
    }
    return "未知";
}

// Optional链式重构
public String getCityFromUserOpt(User user) {
    return Optional.ofNullable(user)
        .map(User::getAddress).map(Address::getCity).map(City::getName)
        .orElse("未知");
}
```

### 1.1 Optional API速查

| 类别 | 方法 | 说明 |
|------|------|------|
| **创建** | `empty()` / `of(T)` / `ofNullable(T)` | 创建（of传null抛NPE，ofNullable安全） |
| **判空** | `isPresent()` / `isEmpty()`(JDK 11+) | 判断是否有值 |
| **取值** | `get()`(不推荐) / `orElse(T)` / `orElseGet(Supplier)` / `orElseThrow(Supplier)` | 安全/默认/异常取值 |
| **消费** | `ifPresent(Consumer)` / `ifPresentOrElse(Consumer,Runnable)`(JDK 9+) | 有值消费，无值兜底 |
| **转换** | `map(Function)` / `flatMap(Function)` / `filter(Predicate)` / `stream()`(JDK 9+) | 链式转换过滤 |

---

## 2. Optional的创建方式

```java
Optional<String> empty   = Optional.empty();                // 空Optional
Optional<String> nonNull = Optional.of("Hello");            // 非空，传null抛NPE
Optional<String> nullable = Optional.ofNullable(getData());  // 最常用，安全处理可能null
```

> 💡 日常开发中`ofNullable`使用频率最高。

基本类型使用专门版本避免装箱开销：`OptionalInt`、`OptionalLong`、`OptionalDouble`。

```java
OptionalInt optInt = OptionalInt.of(42);
optInt.orElse(0);       // 默认值
```

> ⚠️ 不要用`Optional<Integer>`，应使用`OptionalInt`。

---

## 3. Optional的转换与消费：map、flatMap、ifPresent

### 3.1 map：值转换

```java
Optional<String> opt = Optional.of("hello");
Optional<Integer> len = opt.map(String::length); // Optional[5]

// 转换链
String result = Optional.ofNullable(getUser())
    .map(User::getName).map(String::toUpperCase).orElse("默认");
```

### 3.2 flatMap：扁平化映射

当方法本身返回`Optional`时，用`flatMap`避免嵌套。

```java
public static Optional<String> findNickName(String name) {
    return name.length() > 0 ? Optional.of(name + "_nick") : Optional.empty();
}

Optional<Optional<String>> nested = Optional.of("Alice").map(s -> findNickName(s));
Optional<String> flat = Optional.of("Alice").flatMap(s -> findNickName(s));
```

### 3.3 ifPresent / ifPresentOrElse：消费

```java
Optional.of("Hello").ifPresent(s -> System.out.println(s));

Optional.ofNullable(getData()).ifPresentOrElse(
    s  -> System.out.println("值: " + s),       // JDK 9+
    () -> System.out.println("值不存在")
);
```

### 3.4 filter：过滤

```java
Optional.of("Java 8").filter(s -> s.length() > 5); // Optional[Java 8]
Optional.ofNullable(getConfig()).filter(Config::isActive)
    .map(Config::getValue).orElse("默认");
```

---

## 4. Optional的判空与默认值：isPresent、orElse、orElseGet、orElseThrow

### 4.1 判空

```java
if (opt.isPresent()) { /* JDK 8 */ }
if (opt.isEmpty())   { /* JDK 11+ */ }
```

> ⚠️ 频繁用`isPresent`+`get()`说明未充分利用Optional API，优先用`ifPresent`或`orElse`。

### 4.2 orElse vs orElseGet：惰性求值（重要陷阱）

> 🎯 **核心考点**：`orElse`的默认值**始终计算**；`orElseGet`仅在Optional为空时计算。

```java
Optional<String> opt = Optional.of("有值");
String r1 = opt.orElse(computeExpensiveDefault()); // 即使有值也会执行！
String r2 = opt.orElseGet(() -> computeExpensiveDefault()); // 不会执行！
```

| 方法 | 执行时机 | 适用场景 |
|------|----------|----------|
| `orElse(T)` | 始终执行 | other是常量或简单字面量 |
| `orElseGet(Supplier)` | 仅Optional为空时 | other需复杂计算/数据库查询/RPC调用 |

### 4.3 orElseThrow

```java
// JDK 8+：抛自定义异常
Optional.ofNullable(getData()).orElseThrow(() -> new IllegalArgumentException("数据不存在"));
// JDK 10+：抛NoSuchElementException
Optional.ofNullable(getData()).orElseThrow();
```

### 4.4 完整取值对比

| 方法 | 有值 | 为空 |
|------|:---:|:---:|
| `get()` | 值 | NoSuchElementException |
| `orElse("缺省")` | 值 | `"缺省"` |
| `orElseGet(() -> f())` | 值 | 执行Lambda |
| `orElseThrow()` | 值 | NoSuchElementException |

---

## 5. Optional 最佳实践与反模式

### 5.1 正确用法

```java
// ✅ 作为返回值
public Optional<Customer> findById(Long id) {
    return Optional.ofNullable(db.query(id));
}

// ✅ 链式安全调用
return Optional.ofNullable(config).filter(Config::isActive)
    .map(Config::getValue).orElse("默认");

// ✅ 与Stream结合（JDK 9+）
List<String> vals = optionals.stream()
    .flatMap(Optional::stream).collect(Collectors.toList());
```

### 5.2 反模式

```java
// ❌ 不作为字段（Optional不可序列化）
// ❌ 不作为方法参数（调用者可能传null）
// ❌ 不调用get()而不判空
// ❌ 不用Optional包装集合（空集合本身就是"空"的语义）
// ❌ 不在集合中放Optional（直接filter null）
```

### 5.3 规范总结

| 场景 | 推荐做法 |
|------|----------|
| 返回值可能为空 | ✅ `Optional<T>` 作为返回值 |
| 方法参数可能为空 | ❌ 不用Optional参数；用重载或@Nullable |
| 字段可能为空 | ❌ 不用Optional字段；直接用null |
| 昂贵默认值 | ✅ `orElseGet` |
| 简单默认值 | ✅ `orElse` |
| 基本类型 | ✅ `OptionalInt`/`OptionalLong`/`OptionalDouble` |

> 💡 **设计哲学**：Optional是**返回值包装器**，用于告诉调用者"结果可能为空，请优雅处理"，不应侵入类内部设计。

---

## 6. 传统Date/Calendar的痛点

### 6.1 缺陷一览

| 缺陷 | 说明 |
|------|------|
| **可变性** | Date/Calendar可修改，非线程安全 |
| **月份偏移** | 从0开始（一月=0），需+1修正 |
| **年份偏移** | Date年份从1900偏移 |
| **设计混乱** | Date既含日期又含时间 |
| **线程不安全** | SimpleDateFormat不能static复用 |
| **时区复杂** | API晦涩难用 |

```java
Calendar cal = Calendar.getInstance();
cal.set(2024, Calendar.MARCH, 15);
int month = cal.get(Calendar.MONTH); // 2（不是3！）

SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // 线程不安全！

Date date = new Date();
date.setTime(0); // 违反不可变设计
```

### 6.2 传统 vs java.time

| 维度 | java.util.Date/Calendar | java.time |
|------|------------------------|-----------|
| 可变性 | 可变 | **不可变，线程安全** |
| 月份 | 0开始 | **1开始** |
| 职责 | Date含日期+时间 | **LocalDate/LocalTime/LocalDateTime分离** |
| 格式化 | SimpleDateFormat线程不安全 | **DateTimeFormatter线程安全** |
| 时区 | Calendar+TimeZone晦涩 | **ZoneId+ZonedDateTime清晰** |
| 间隔 | 无直接API | **Duration/Period** |

> ⚠️ **新项目强制使用java.time**，严禁Date/Calendar/SimpleDateFormat。

---

## 7. java.time核心类：LocalDate、LocalTime、LocalDateTime

### 7.1 类职责

| 类 | 表示 | 示例 | 场景 |
|----|------|------|------|
| `LocalDate` | 年-月-日 | `2024-03-15` | 生日、订单日期 |
| `LocalTime` | 时:分:秒.纳秒 | `14:30:00` | 打卡、时段 |
| `LocalDateTime` | 日期+时间 | `2024-03-15T14:30:00` | **最常用** |
| `ZonedDateTime` | 日期+时间+时区 | `2024-03-15T14:30:00+08:00` | 跨时区 |
| `Instant` | UTC时间戳 | `2024-03-15T06:30:00Z` | 机器时间 |

### 7.2 创建与操作

> 💡 所有操作返回**新对象**，原对象不可变，天然线程安全。

```java
// ---- 创建 ----
LocalDate today  = LocalDate.now();
LocalTime now    = LocalTime.now();
LocalDateTime dt = LocalDateTime.now();
LocalDate date   = LocalDate.of(2024, 3, 15);       // 月份1~12！
LocalTime time   = LocalTime.of(14, 30, 0);
LocalDateTime dt2 = LocalDateTime.of(2024, 3, 15, 14, 30);

// ---- 解析（默认ISO格式） ----
LocalDate.parse("2024-03-15");
LocalDateTime.parse("2024-03-15T14:30:00");

// ---- 加减 ----
today.plusDays(1).minusWeeks(1).plusMonths(1).minusYears(1);
dt.plusHours(2).plusMinutes(30);

// ---- 设置字段 ----
dt.withDayOfMonth(1).withHour(0).withMinute(0);

// ---- 获取字段 ----
today.getYear();        // 2024
today.getMonthValue();  // 1~12（无需修正）
today.getMonth();       // Month.MARCH
today.getDayOfMonth();  // 15
today.getDayOfWeek();   // DayOfWeek.FRIDAY
today.isLeapYear();     // true

// ---- 比较 ----
d1.isBefore(d2); d1.isAfter(d2); d1.isEqual(d2);

// ---- ChronoUnit间隔 ----
ChronoUnit.DAYS.between(d1, d2);   // 365
ChronoUnit.MONTHS.between(d1, d2); // 11
```

### 7.3 TemporalAdjuster

```java
LocalDate d = LocalDate.of(2024, 3, 15);
d.with(TemporalAdjusters.firstDayOfMonth());     // 2024-03-01
d.with(TemporalAdjusters.lastDayOfMonth());       // 2024-03-31
d.with(TemporalAdjusters.next(DayOfWeek.MONDAY)); // 下周一
d.with(TemporalAdjusters.firstDayOfYear());       // 2024-01-01
d.with(TemporalAdjusters.firstInMonth(DayOfWeek.TUESDAY)); // 本月第一个周二
```

---

## 8. Instant、Duration与Period

### 8.1 Instant：UTC时间戳

```java
Instant now = Instant.now();
now.getEpochSecond();   // 秒级时间戳
now.toEpochMilli();     // 毫秒级时间戳
Instant.ofEpochSecond(1_710_484_200L);
Instant.ofEpochMilli(1_710_484_200_000L);

// Instant <-> Date
java.util.Date.from(now);
date.toInstant();
```

### 8.2 Duration：时间差（时分秒纳秒）

```java
Duration d = Duration.between(LocalTime.of(9, 0), LocalTime.of(17, 30));
d.toHours();    // 8
d.toMinutes();  // 510
d.getSeconds(); // 30600

Duration.ofHours(2);
Duration.ofMinutes(10);
Duration.ofDays(1);
```

### 8.3 Period：日期差（年月日）

```java
Period p = Period.between(LocalDate.of(1990, 1, 1), LocalDate.of(2024, 3, 15));
p.getYears();  // 34
p.getMonths(); // 2
p.getDays();   // 14

Period.ofYears(1);
Period.ofMonths(3);
Period.ofWeeks(2);
Period.of(1, 6, 15); // 1年6个月15天
```

| 对比 | Duration | Period |
|------|----------|--------|
| 精度 | 秒/纳秒 | 年/月/日 |
| 适用类型 | LocalTime, Instant | LocalDate |
| 创建 | `Duration.ofHours(2)` | `Period.ofMonths(3)` |

---

## 9. DateTimeFormatter格式化与解析

> ✅ **线程安全**，可`static final`复用。`SimpleDateFormat`线程不安全，禁止static。

```java
private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

// 格式化
LocalDateTime.now().format(FMT); // 2024-03-15 14:30:00
// 解析
LocalDateTime.parse("2024-03-15 14:30:00", FMT);
```

### 格式模式

| 模式 | 示例 | 说明 |
|------|------|------|
| `yyyy-MM-dd` | `2024-03-15` | 日期 |
| `HH:mm:ss` | `14:30:00` | 24小时制 |
| `yyyy-MM-dd HH:mm:ss` | `2024-03-15 14:30:00` | 日期+时间 |
| `yyyy/MM/dd` | `2024/03/15` | 斜杠 |
| `yyyy年MM月dd日` | `2024年03月15日` | 中文 |

> ⚠️ 年份用`yyyy`（非`YY`），月份用`MM`（非`mm`），小时用`HH`（24h，非`hh`）。

### 预定义与本地化格式

```java
DateTimeFormatter.ISO_LOCAL_DATE;          // 2024-03-15
DateTimeFormatter.ISO_LOCAL_DATE_TIME;     // 2024-03-15T14:30:00
DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);   // 2024年3月15日 星期五
DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM); // 2024-3-15

// 非ISO格式解析
DateTimeFormatter.ofPattern("yyyy/MM/dd");
LocalDate.parse("2024/03/15", customFmt);

// 异常处理
try { LocalDateTime.parse("2024-13-01 14:30:00", FMT); }
catch (DateTimeException e) { /* 格式错误 */ }
```

---

## 10. ZonedDateTime与时区处理

```java
ZoneId shanghai = ZoneId.of("Asia/Shanghai");
ZoneId ny = ZoneId.of("America/New_York");
ZoneId utc = ZoneId.of("UTC");

// 创建ZonedDateTime
ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.of(2024, 3, 15, 14, 30), shanghai);
// 2024-03-15T14:30:00+08:00[Asia/Shanghai]

// 时区转换（同一瞬间，不同显示）
zdt.withZoneSameInstant(ny);  // 纽约时间
zdt.withZoneSameInstant(utc); // UTC时间
zdt.getOffset();              // +08:00
```

### 时区最佳实践

| 场景 | 推荐 |
|------|------|
| 服务端内部 | `Instant`或`LocalDateTime + ZoneId` |
| 数据库 | TIMESTAMP WITH TIME ZONE，存UTC |
| 跨时区通信 | ISO-8601格式字符串（含偏移） |

> 💡 **服务端间通信推荐Instant**，统一UTC，避免时区歧义。

---

## 11. 传统API与java.time的互操作

```java
// Date <-> Instant
java.util.Date.from(instant);                  // Instant -> Date
legacyDate.toInstant();                         // Date -> Instant

// Date <-> LocalDateTime
LocalDateTime.ofInstant(legacyDate.toInstant(), ZoneId.systemDefault());       // Date -> LDT
Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());      // LDT -> Date

// java.sql.Date <-> LocalDate
java.sql.Date.valueOf(LocalDate.now());         // LocalDate -> sql.Date
sqlDate.toLocalDate();                           // sql.Date -> LocalDate

// Calendar -> LocalDateTime
LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
```

### 互操作速查

| 转换方向 | 代码 |
|----------|------|
| `Date` -> `LocalDateTime` | `LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())` |
| `LocalDateTime` -> `Date` | `Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())` |
| `Date` -> `Instant` | `date.toInstant()` |
| `Instant` -> `Date` | `Date.from(instant)` |
| `sql.Date` -> `LocalDate` | `sqlDate.toLocalDate()` |
| `LocalDate` -> `sql.Date` | `java.sql.Date.valueOf(localDate)` |

---

## 12. 面试高频考点

### 12.1 Optional

| 问题 | 要点 |
|------|------|
| **创建方式？** | `empty()`/`of()`/`ofNullable()`，区别是否允许null |
| **orElse vs orElseGet？** | `orElse`始终执行；`orElseGet`惰性求值 |
| **map vs flatMap？** | map自动包装；flatMap用于方法返回Optional场景 |
| **正确使用场景？** | 仅返回值，不作字段/参数/集合元素 |
| **基本类型？** | `OptionalInt`/`OptionalLong`/`OptionalDouble` |

### 12.2 日期时间

| 问题 | 要点 |
|------|------|
| **为什么用LocalDateTime替代Date？** | 不可变/线程安全、月份1开始、职责分离、格式化器线程安全 |
| **LocalDate/LocalTime/LocalDateTime区别？** | 日期/时间/日期+时间职责分离 |
| **Duration vs Period？** | 时分秒精度 vs 年月日精度 |
| **DateTimeFormatter vs SimpleDateFormat？** | 前者线程安全可static；后者线程不安全 |
| **如何处理时区？** | ZoneId+ZonedDateTime，服务端用Instant/UTC |

### 12.3 代码题

```java
// 生日（Date）转年龄
public static int getAge(java.util.Date birthDate) {
    if (birthDate == null) return 0;
    return Period.between(
        birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
        LocalDate.now()
    ).getYears();
}

// 安全获取嵌套属性
public static String getCompanyName(Employee emp) {
    return Optional.ofNullable(emp)
        .map(Employee::getDepartment).map(Department::getCompany)
        .map(Company::getName).orElse("未知公司");
}
```

### 12.4 快速选型

| 场景 | 使用类 |
|------|--------|
| 纯日期 | `LocalDate` |
| 纯时间 | `LocalTime` |
| 本地日期+时间 | `LocalDateTime` |
| 跨时区 | `ZonedDateTime` |
| 时间戳/机器时间 | `Instant` |
| 格式化解析（线程安全） | `DateTimeFormatter` |
| 日期间隔（年月日） | `Period` |
| 时间间隔（时分秒） | `Duration` |
| 安全空值处理 | `Optional` / `OptionalInt` / `OptionalLong` / `OptionalDouble` |

---

> 🎯 **核心总结**：Optional通过函数式链式调用（map/flatMap/filter + orElse/orElseGet）优雅解决NPE问题。java.time API以不可变设计、职责分离、线程安全格式化器彻底取代了传统Date/Calendar。**新项目禁止使用Date/Calendar/SimpleDateFormat，优先用Optional作为返回值类型。**
