# 04 Java 时间日期 API 深度解析

> Date 和 Calendar 被诟病了 20 年——可变、月份从 0 开始、线程不安全、API 反直觉。java.time (JSR-310) 用不可变对象 + 清晰的类职责划分彻底解决了这些问题

---

## 📚 目录

1. [旧 API 之痛](#1-旧-api-之痛)
2. [java.time 设计哲学](#2-javatime-设计哲学)
3. [LocalDate / LocalTime / LocalDateTime](#3-localdate--localtime--localdatetime)
4. [Instant 与时间戳](#4-instant-与时间戳)
5. [时区处理](#5-时区处理)
6. [Duration 与 Period](#6-duration-与-period)
7. [DateTimeFormatter 格式与解析](#7-datetimeformatter-格式与解析)
8. [TemporalAdjuster](#8-temporaladjuster)
9. [新旧 API 互转](#9-新旧-api-互转)
10. [练习](#10-练习)

---

## 1. 旧 API 之痛

在 Java 8 之前，Java 的时间日期处理主要依赖三个类：`java.util.Date`、`java.util.Calendar` 和 `java.text.SimpleDateFormat`。它们的设计缺陷之多，堪称 Java 标准库中最大的"遗留债"。

### 1.1 Date 的可变性

`java.util.Date` 是一个可变对象——它的内部时间值可以被 `setTime()` 等方法随意修改。这意味着你把一个 Date 传给某个方法后，无法保证它在方法执行过程中不被改变。

```java
// 🔴 旧 API：Date 可变，破坏封装
public class OrderService {
    private Date deliveryDate;

    public void setDeliveryDate(Date date) {
        this.deliveryDate = date;   // 引用传进来了
    }

    public Date getDeliveryDate() {
        return deliveryDate;        // 外部拿到引用后可以修改
    }
}

// 调用方
OrderService service = new OrderService();
service.setDeliveryDate(new Date());

Date d = service.getDeliveryDate();
d.setTime(d.getTime() + 100_000_000L); // ⚠️ 直接改内部状态！
// service.getDeliveryDate() 的值被无声无息地篡改了
```

```java
// 🟢 java.time：不可变，返回新实例
public class OrderService {
    private LocalDateTime deliveryDate;

    public void setDeliveryDate(LocalDateTime date) {
        this.deliveryDate = date;
    }

    public LocalDateTime getDeliveryDate() {
        return deliveryDate; // 安全——调用方无法修改原对象
    }
}

// 调用方
LocalDateTime d = service.getDeliveryDate();
LocalDateTime modified = d.plusHours(1);  // 返回新对象，原对象不变
System.out.println(d == modified);        // false
```

### 1.2 月份从 0 开始的著名 Bug

`java.util.Date` 的月份从 0 开始（January = 0），而日期从 1 开始。这种不一致是所有 Java 新手必经的"坑"。

```java
// 🔴 旧 API：月份从 0 开始
Date date = new Date(121, 0, 1);  // 2021年1月1日? 参数含义不直观
// 实际：year = 1900 + 121 = 2021, month = 0 = January

Date date2 = new Date(2021, 0, 1); // ⚠️ 你以为的 2021年0月1日（荒谬！）
// 事实上 year 参数表示 1900 + 2021 = 3921 年 😱

// 最常见的 Bug
int month = date.getMonth();       // 返回 0 ~ 11
System.out.println(month + 1);     // 几乎每次都要 +1，且经常忘记
```

```java
// 🟢 java.time：月份从 1 开始，符合直觉
LocalDate date = LocalDate.of(2021, 1, 1);  // 2021年1月1日
int month = date.getMonthValue();           // 1 ~ 12
Month m = date.getMonth();                  // Month.JANUARY（枚举，不会混淆）
```

### 1.3 SimpleDateFormat 线程不安全

`SimpleDateFormat` 是线程不安全的——它的内部 `Calendar` 对象在 `format()` 和 `parse()` 过程中会修改共享状态。生产环境中，静态共享 `SimpleDateFormat` 在高并发下会导致解析结果错乱、数值溢出，甚至 JVM crash。

```java
// 🔴 旧 API：SimpleDateFormat 线程不安全
public class DateUtils {
    // 静态共享 —— 多线程下灾难！
    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd");

    public static Date parse(String source) throws ParseException {
        return sdf.parse(source);  // ⚠️ 并发调用时内部 Calendar 被多线程篡改
    }
}

// 高并发时的表现
// 线程A调用 parse("2024-01-15") → 期望 2024-01-15
// 线程B同时调用 parse("2023-06-20") → 可能返回 null 或完全错误的值
// 甚至产生 NumberFormatException：对于 "2024-01-15"，可能读到部分被覆盖的 buffer
```

```java
// ❌ "修正"方案 1：每次创建新实例（性能差）
public static Date parse(String source) throws ParseException {
    return new SimpleDateFormat("yyyy-MM-dd").parse(source); // GC 压力大
}

// ❌ "修正"方案 2：加 synchronized（吞吐量暴跌）
public static synchronized Date parse(String source) throws ParseException {
    return sdf.parse(source); // 多线程排队，相当于单线程
}

// ❌ "修正"方案 3：ThreadLocal（代码繁琐）
private static final ThreadLocal<SimpleDateFormat> TL =
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
```

```java
// 🟢 java.time：DateTimeFormatter 是线程安全的
private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd"); // 不可变，线程安全

// 任意多线程同时调用都安全
public static LocalDate parse(String source) {
    return LocalDate.parse(source, FORMATTER);
}
```

### 1.4 Calendar 的 API 反直觉

`Calendar` 的设计充满了误导。最典型的陷阱是 `set()` 不会立即重新计算——它只会标记字段为"被修改"，直到你调用 `get()`、`getTime()` 或 `add()` 时才触发重新计算。

```java
// 🔴 旧 API：Calendar 的反直觉行为
Calendar cal = Calendar.getInstance();
cal.set(Calendar.MONTH, Calendar.DECEMBER); // 设置到 12 月
cal.set(Calendar.DAY_OF_MONTH, 31);          // 12 月 31 日
cal.set(Calendar.YEAR, 2024);                 // 2024 年
// 到现在一切正常：2024-12-31

cal.set(Calendar.DAY_OF_MONTH, 32);           // ⚠️ 没有立即报错！
// 内部 lenient 模式自动溢出，变成了 2025-01-01
// 但如果你没调用 get()，程序毫无察觉

// 另一种坑：set 的顺序依赖
cal.set(2024, Calendar.OCTOBER, 31);          // 2024-10-31
cal.set(Calendar.MONTH, Calendar.NOVEMBER);   // 设置月份为 11 月
// 直观想法：2024-11-31 → 但 11 月只有 30 天
// 自动修正为 2024-12-01 😱
```

```java
// 🟢 java.time：清晰的 API，没有意外
LocalDate date = LocalDate.of(2024, 12, 31);
// LocalDate.of(2024, 12, 32) → 抛出 DateTimeException: Invalid value
// 不会默默溢出，立即失败

// 月份调整同样安全
LocalDate oct31 = LocalDate.of(2024, 10, 31);
LocalDate nov30 = oct31.withMonth(11); // 自动取 11 月 30 日（有效值）
// withMonth 会智能调整，但比 Calendar 可预测得多
```

### 1.5 Date 和 Calendar 互转的繁琐

```java
// 🔴 旧 API：互转需要大量样板代码
// Date → Calendar
Date date = new Date();
Calendar cal = Calendar.getInstance();
cal.setTime(date);

// Calendar → Date
Date dateFromCal = cal.getTime();

// 获取"年-月-日 时:分:秒"
Calendar c = Calendar.getInstance();
int year = c.get(Calendar.YEAR);
int month = c.get(Calendar.MONTH) + 1;     // 别忘了 +1！
int day = c.get(Calendar.DAY_OF_MONTH);
int hour = c.get(Calendar.HOUR_OF_DAY);
int minute = c.get(Calendar.MINUTE);
int second = c.get(Calendar.SECOND);
// 7 行代码，还容易掉进月份 +1 的坑
```

```java
// 🟢 java.time：一行获取所需
LocalDateTime now = LocalDateTime.now();
int year = now.getYear();           // 直接
int month = now.getMonthValue();    // 1 ~ 12，不需要 +1
int day = now.getDayOfMonth();
// 3 行搞定，语义清晰
```

### 1.6 Date 和 Calendar 的命名混乱

| 旧 API | 问题 |
|--------|------|
| `Date` | 既表示日期又表示时间，名字叫 Date 却包含时分秒 |
| `Calendar.getInstance()` | 返回的是 `GregorianCalendar`，不是抽象 `Calendar` |
| `Calendar.YEAR` | 用整型常量表示字段，不是枚举，不类型安全 |
| `Date.getMonth()` | 返回 0~11，名字看不出从 0 开始 |
| `Date.getYear()` | 返回 year - 1900，完全反直觉 |

```java
// 🟢 java.time 命名一览
// 类名即含义：
// LocalDate     = 只有日期（年月日）
// LocalTime     = 只有时间（时分秒纳秒）
// LocalDateTime = 日期 + 时间
// Instant       = 时间戳（UTC 纳秒）
// ZonedDateTime = 带时区的完整日期时间
// Duration      = 时间量（纳秒精度）
// Period        = 日期量（年月日）
```

> 🎯 **核心要点**：旧 API 的根源问题：可变性、月份从 0、线程不安全、API 反直觉。java.time 用不可变对象 + 职责单一 + 工厂方法三个原则一次性解决了所有问题。

---

## 2. java.time 设计哲学

JSR-310（Java Specification Request 310）由 Stephen Colebourne 领导设计，他同时也是 Joda-Time 的作者。java.time 包吸收 Joda-Time 的经验教训，在 Java 8 正式引入。

### 2.1 不可变对象（Immutable）

所有 java.time 核心类都是不可变的（immutable）和线程安全的。

```java
// 不可变性：修改操作返回新实例
LocalDate d1 = LocalDate.of(2024, 1, 1);
LocalDate d2 = d1.plusDays(1);
LocalDate d3 = d2.withMonth(12);

System.out.println(d1);               // 2024-01-01（原对象不变）
System.out.println(d2);               // 2024-01-02（新对象）
System.out.println(d3);               // 2024-12-02（另一个新对象）
System.out.println(d1 == d2);         // false

// 所有"修改"方法都返回 Temporal 子类型的新实例：
// plusDays / plusWeeks / plusMonths / plusYears
// minusDays / minusWeeks / minusMonths / minusYears
// withDayOfMonth / withMonth / withYear
// plus / minus (接受 TemporalAmount)
```

不可变性的好处：
- **线程安全**：无需同步即可在多线程间共享
- **无副作用**：方法参数不会被修改
- **可缓存**：可以安全地用作 Map 的 key
- **无防御性拷贝**：getter 可以直接返回内部引用

```java
// 不可变对象的线程安全优势
public class DateRange {
    private final LocalDate start;
    private final LocalDate end;

    public DateRange(LocalDate start, LocalDate end) {
        this.start = start;  // 无需防御性拷贝！
        this.end = end;      // LocalDate 不可变，引用不会被篡改
    }

    public LocalDate getStart() { return start; }  // 直接返回，安全
    public LocalDate getEnd()   { return end; }
}
```

### 2.2 类职责单一（Single Responsibility）

每个类只负责一个概念，不再有 Date 那种"既表示日期又表示时间"的混乱。

| 类 | 职责 | 精度 | 典型场景 |
|----|------|------|---------|
| `LocalDate` | 日期（年-月-日） | 天 | 生日、账单日、排期 |
| `LocalTime` | 时间（时:分:秒.纳秒） | 纳秒 | 营业时间、定时任务触发时刻 |
| `LocalDateTime` | 日期 + 时间（无时区） | 纳秒 | 本地事务记录、日志时间戳 |
| `Instant` | UTC 时间线上的瞬时点 | 纳秒 | 系统级时间戳、跨时区交换 |
| `ZonedDateTime` | 完整日期时间 + 时区 | 纳秒 | 国际会议、航班起降 |
| `OffsetDateTime` | 日期时间 + UTC 偏移量 | 纳秒 | REST API 传输、数据库存储 |
| `OffsetTime` | 时间 + UTC 偏移量 | 纳秒 | 特殊场景 |
| `Year` | 年份 | 年 | 年度报告 |
| `YearMonth` | 年-月 | 月 | 信用卡有效期 |
| `MonthDay` | 月-日 | 天 | 每年重复的纪念日 |

```java
// 职责单一 —— 不会"混搭"
// 生日只需要日期，不需要时间和时区
LocalDate birthday = LocalDate.of(1990, 5, 20);

// 营业时间只需要时间，不需要日期
LocalTime openingTime = LocalTime.of(9, 0);
LocalTime closingTime = LocalTime.of(18, 0);

// 系统日志需要精确到纳秒，无时区
LocalDateTime eventTime = LocalDateTime.now();

// 全球时间戳需要 UTC 瞬时
Instant now = Instant.now();
```

### 2.3 工厂方法取代构造器

java.time 类大量使用静态工厂方法（static factory method），而非 public 构造器。这是 Effective Java 第 1 条建议的最佳实践。

```java
// 构造器（旧 API，已废弃）
// Date date = new Date(2024, 1, 1); // deprecated

// 工厂方法（java.time 推荐）
// — now()：取当前时间
LocalDate today    = LocalDate.now();
LocalTime nowTime  = LocalTime.now();
LocalDateTime now  = LocalDateTime.now();
Instant instant    = Instant.now();

// — of()：从指定值创建
LocalDate  date1  = LocalDate.of(2024, 12, 25);
LocalTime  time1  = LocalTime.of(14, 30, 0);
LocalDateTime dt1  = LocalDateTime.of(2024, 12, 25, 14, 30);
YearMonth  ym     = YearMonth.of(2024, 12);
MonthDay   md     = MonthDay.of(12, 25);

// — parse()：从字符串解析
LocalDate dateParsed    = LocalDate.parse("2024-12-25");
LocalTime timeParsed    = LocalTime.parse("14:30:00");
LocalDateTime dtParsed  = LocalDateTime.parse("2024-12-25T14:30:00");
Instant instantParsed   = Instant.parse("2024-12-25T06:30:00Z");

// — from()：从其他 Temporal 对象转换
LocalDate fromDateTime  = LocalDate.from(LocalDateTime.now());
LocalTime fromDateTime2 = LocalTime.from(LocalDateTime.now());
```

### 2.4 ISO 8601 标准遵循

java.time 全面遵循 **ISO 8601** 国际标准。

| 概念 | ISO 8601 格式 | java.time 示例 |
|------|--------------|----------------|
| 日期 | `yyyy-MM-dd` | `2024-12-25` |
| 时间 | `HH:mm:ss.SSS` | `14:30:00.123` |
| 日期+时间 | `yyyy-MM-dd'T'HH:mm:ss` | `2024-12-25T14:30:00` |
| UTC 时间 | 末尾加 `Z` | `2024-12-25T06:30:00Z` |
| 时区偏移 | `+08:00` / `-05:00` | `2024-12-25T14:30:00+08:00` |
| 持续时间 | `PnYnMnDTnHnMnS` | `P1Y2M3DT4H5M6S` |
| 周期 | `Rn/PnYnMnD` | `R3/P1Y`（每 1 年重复 3 次） |

```java
// java.time 的 toString() 默认输出 ISO 8601 格式
System.out.println(LocalDate.of(2024, 12, 25));
// 输出: 2024-12-25

System.out.println(LocalDateTime.of(2024, 12, 25, 14, 30));
// 输出: 2024-12-25T14:30:00

System.out.println(Instant.now());
// 输出: 2024-12-25T06:30:00.123Z

System.out.println(ZoneId.of("Asia/Shanghai"));
// 输出: Asia/Shanghai

System.out.println(Duration.ofHours(1).plusMinutes(30));
// 输出: PT1H30M

System.out.println(Period.of(1, 2, 3));
// 输出: P1Y2M3D
```

### 2.5 空安全与异常设计

```java
// java.time 方法参数不接受 null
LocalDate date = LocalDate.now();
// date.plusDays(null) → NullPointerException，立即失败

// 无效值抛出 DateTimeException 或其子类
// LocalDate.of(2024, 2, 30) → DateTimeException:
//   Invalid date 'February 30'
// LocalTime.of(25, 0)       → DateTimeException:
//   Invalid value for HourOfDay (valid values 0 - 23)

// TemporalAccessor 接口 —— 统一访问协议
// 所有日期时间类都实现 TemporalAccessor
// 可以通过 TemporalQuery 统一查询
```

> 🎯 **核心要点**：java.time 的设计哲学概括为三点——不可变对象保证线程安全，职责单一避免概念混淆，工厂方法提供灵活创建。理解这三点，就能自然而然地理解整个包的 API 设计。

---

## 3. LocalDate / LocalTime / LocalDateTime

这是 java.time 包中使用频率最高的三个类，涵盖了大多数"不带时区"的场景。

### 3.1 创建

```java
// === LocalDate 创建 ===

// 当前日期（系统时钟，默认时区）
LocalDate today = LocalDate.now();

// 指定年月日
LocalDate christmas = LocalDate.of(2024, 12, 25);
LocalDate newYear   = LocalDate.of(2025, Month.JANUARY, 1); // Month 枚举

// 从字符串解析（ISO 8601 格式 yyyy-MM-dd）
LocalDate parsed = LocalDate.parse("2024-12-25");

// 其他工厂
LocalDate epochDay   = LocalDate.ofEpochDay(0);    // 1970-01-01
LocalDate yearDay    = LocalDate.ofYearDay(2024, 1); // 2024-01-01
LocalDate fromInstant = LocalDate.from(
        Instant.now().atZone(ZoneId.systemDefault()));

// === LocalTime 创建 ===

// 当前时间
LocalTime now = LocalTime.now();

// 指定时分秒纳秒
LocalTime noon    = LocalTime.of(12, 0);
LocalTime lunch   = LocalTime.of(12, 30, 0);
LocalTime precise = LocalTime.of(12, 30, 15, 123_456_789); // 纳秒

// 从字符串解析
LocalTime parsedTime = LocalTime.parse("14:30:00");
LocalTime withNanos  = LocalTime.parse("14:30:00.123456789");

// 其他工厂
LocalTime midNight = LocalTime.MIDNIGHT;  // 00:00
LocalTime minTime  = LocalTime.MIN;       // 00:00
LocalTime maxTime  = LocalTime.MAX;       // 23:59:59.999999999
LocalTime noon2    = LocalTime.NOON;      // 12:00

// === LocalDateTime 创建 ===

// 当前日期时间
LocalDateTime nowDt = LocalDateTime.now();

// 组合 LocalDate + LocalTime
LocalDateTime dt1 = LocalDateTime.of(LocalDate.now(), LocalTime.NOON);
LocalDateTime dt2 = LocalDateTime.of(2024, 12, 25, 14, 30);
LocalDateTime dt3 = LocalDateTime.of(2024, 12, 25, 14, 30, 15);
LocalDateTime dt4 = LocalDateTime.of(2024, 12, 25, 14, 30, 15, 123_456_789);

// 从字符串解析
LocalDateTime parsedDt = LocalDateTime.parse("2024-12-25T14:30:00");
```

### 3.2 获取字段

```java
LocalDate date = LocalDate.of(2024, 12, 25);

// 基本字段
int year          = date.getYear();           // 2024
int month         = date.getMonthValue();     // 12  （1 ~ 12）
Month monthEnum  = date.getMonth();           // Month.DECEMBER（类型安全）
int day           = date.getDayOfMonth();     // 25
int dayOfYear     = date.getDayOfYear();      // 360
DayOfWeek dow     = date.getDayOfWeek();      // DayOfWeek.WEDNESDAY
int dowValue      = dow.getValue();           // 3（Monday=1 ~ Sunday=7）

// 检查
boolean leap      = date.isLeapYear();         // true（2024 是闰年）
int lengthOfYear  = date.lengthOfYear();       // 366
int lengthOfMonth = date.lengthOfMonth();      // 31
Era era           = date.getEra();             // IsoEra.CE

LocalTime time = LocalTime.of(14, 30, 15, 123_456_789);

int hour        = time.getHour();              // 14
int minute      = time.getMinute();            // 30
int second      = time.getSecond();            // 15
int nano        = time.getNano();              // 123456789

// 通过 TemporalField 通用获取
int year2   = date.get(ChronoField.YEAR);
int month2  = date.get(ChronoField.MONTH_OF_YEAR);
int dow2    = date.get(ChronoField.DAY_OF_WEEK);
```

### 3.3 修改（with / plus / minus）

```java
// === withXXX：设置指定字段（返回新对象） ===
LocalDate date = LocalDate.of(2024, 6, 15);

LocalDate withYear       = date.withYear(2025);        // 2025-06-15
LocalDate withMonth      = date.withMonth(12);          // 2024-12-15
LocalDate withDayOfMonth = date.withDayOfMonth(1);      // 2024-06-01
LocalDate withDayOfYear  = date.withDayOfYear(1);       // 2024-01-01

// 通用 TemporalField 版本
LocalDate withChrono = date.with(ChronoField.MONTH_OF_YEAR, 12);

// === plusXXX：增加 ===
LocalDate plusDays  = date.plusDays(10);    // 2024-06-25
LocalDate plusWeeks = date.plusWeeks(2);    // 2024-06-29
LocalDate plusMonths = date.plusMonths(1);  // 2024-07-15
LocalDate plusYears  = date.plusYears(1);   // 2025-06-15
LocalDate plusDaysNegative = date.plusDays(-5); // 2024-06-10

// 通用 period 版本
LocalDate plus = date.plus(Period.ofDays(10));

// === minusXXX：减少 ===
LocalDate minusDays    = date.minusDays(10);    // 2024-06-05
LocalDate minusMonths  = date.minusMonths(6);   // 2023-12-15
LocalDate minusYears   = date.minusYears(30);   // 1994-06-15

// LocalTime 同理
LocalTime time = LocalTime.of(14, 30, 0);

LocalTime plusHours   = time.plusHours(2);    // 16:30
LocalTime plusMinutes = time.plusMinutes(30);  // 15:00
LocalTime plusSeconds = time.plusSeconds(45);  // 14:30:45
LocalTime plusNanos   = time.plusNanos(1000);  // 14:30:00.000001

// LocalDateTime
LocalDateTime dt = LocalDateTime.of(2024, 12, 25, 14, 30);
LocalDateTime modified = dt.plusDays(1)
                           .minusHours(2)
                           .withMinute(0);
// 2024-12-26T12:30:00  → 链式调用
```

### 3.4 比较

```java
LocalDate d1 = LocalDate.of(2024, 1, 1);
LocalDate d2 = LocalDate.of(2024, 12, 31);

// 布尔比较
boolean before = d1.isBefore(d2);          // true
boolean after  = d1.isAfter(d2);           // false
boolean equal  = d1.isEqual(d2);           // false
boolean leap   = d1.isLeapYear();          // true（2024）

// 与 equals / compareTo
boolean eq     = d1.equals(d2);            // false
int comparison = d1.compareTo(d2);         // 负数（d1 < d2）
int diffDays   = (int) ChronoUnit.DAYS.between(d1, d2);    // 365
long diffMonths = ChronoUnit.MONTHS.between(d1, d2);       // 11
long diffYears  = ChronoUnit.YEARS.between(d1, d2);        // 0

// until —— 更为灵活的间隔计算
long untilDays   = d1.until(d2, ChronoUnit.DAYS);    // 365
long untilMonths = d1.until(d2, ChronoUnit.MONTHS);  // 11
long untilYears  = d1.until(d2, ChronoUnit.YEARS);   // 0

// Period 版（同时获取年月日）
Period period = Period.between(d1, d2);
int pYears  = period.getYears();     // 0
int pMonths = period.getMonths();    // 11
int pDays   = period.getDays();      // 30
```

### 3.5 格式化与解析

```java
// 格式化为字符串
LocalDateTime dt = LocalDateTime.of(2024, 12, 25, 14, 30, 0);

// 预定义格式
String isoLocal     = dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
// "2024-12-25T14:30:00"
String isoDate      = dt.format(DateTimeFormatter.ISO_LOCAL_DATE);
// "2024-12-25"
String isoTime      = dt.format(DateTimeFormatter.ISO_LOCAL_TIME);
// "14:30:00"

// 自定义格式
DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
String formatted = dt.format(formatter);
// "2024年12月25日 14:30:00"

// 从字符串解析
String text = "2024-12-25 14:30:00";
DateTimeFormatter parser =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
LocalDateTime parsed = LocalDateTime.parse(text, parser);

// 预定义格式解析
LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse("2024-12-25"));
// equals: LocalDate.parse("2024-12-25") // 更简洁
```

### 3.6 LocalDateTime ↔ LocalDate / LocalTime 互转

```java
// LocalDateTime → LocalDate / LocalTime
LocalDateTime dt = LocalDateTime.now();

LocalDate datePart  = dt.toLocalDate();   // 只取日期部分
LocalTime timePart  = dt.toLocalTime();   // 只取时间部分

// LocalDate / LocalTime → LocalDateTime
LocalDate date = LocalDate.of(2024, 12, 25);
LocalTime time = LocalTime.of(14, 30, 0);

LocalDateTime dt1 = date.atTime(time);        // 2024-12-25T14:30
LocalDateTime dt2 = date.atTime(14, 30);      // 2024-12-25T14:30
LocalDateTime dt3 = date.atStartOfDay();      // 2024-12-25T00:00
LocalDateTime dt4 = time.atDate(date);        // 2024-12-25T14:30
```

> 🎯 **核心要点**：LocalDate/LocalTime/LocalDateTime 是日常最常用的三个类。记住创建三剑客——`now()`、`of()`、`parse()`，修改三兄弟——`with`、`plus`、`minus`，以及比较工具——`isBefore`/`isAfter`/`until`，就能覆盖 80% 的日常需求。

---

## 4. Instant 与时间戳

### 4.1 Instant 概念

`Instant` 表示 UTC 时间线上的一个瞬时点，精度为纳秒。它是机器时间（machine time），而非人类时间（human time）。

```java
// Instant 的本质：
// 内部存储两个 long 值：seconds（从 1970-01-01T00:00:00Z 起的秒数）
//                      nanos（当前秒内的纳秒偏移，0 ~ 999,999,999）
System.out.println(Instant.now());
// 示例输出: 2024-12-25T06:30:00.123456789Z
//           ↑ UTC 时间                    ↑ 表示 UTC
```

### 4.2 创建 Instant

```java
// 当前 UTC 时间戳
Instant now = Instant.now();

// 从毫秒/秒/纳秒创建
Instant fromEpochMilli = Instant.ofEpochMilli(1_704_067_200_000L);
// 2024-01-01T00:00:00Z

Instant fromEpochSecond = Instant.ofEpochSecond(1_704_067_200L);
// 同上的秒级版本

Instant fromEpochSecondNano = Instant.ofEpochSecond(1_704_067_200L, 123_456_789L);
// 带纳秒偏移

// 从字符串解析（ISO 8601，末尾必须带 Z）
Instant parsed = Instant.parse("2024-12-25T06:30:00Z");
// Instant.parse("2024-12-25T06:30:00") → ❌ DateTimeException（缺少 Z）

// 从 Date 转换（旧 API 互转）
Instant fromDate = new Date().toInstant();
```

### 4.3 获取时间戳

```java
Instant now = Instant.now();

// 获取秒级/毫秒级/纳秒级时间戳
long epochSecond = now.getEpochSecond();     // 自 1970-01-01T00:00:00Z 起的秒数
long epochMilli  = now.toEpochMilli();       // 毫秒（Java 标准）
int  nano        = now.getNano();            // 当前秒内的纳秒部分

// 与 System.currentTimeMillis() 对比
long sysMilli = System.currentTimeMillis();
long instMilli = Instant.now().toEpochMilli();
// 两者等价，但 Instant 还提供了纳秒精度
```

### 4.4 Instant 与 Date 互转

```java
// Date → Instant（推荐方式）
Date date = new Date();
Instant instant = date.toInstant();   // Date 新增的方法

// Instant → Date
Date fromInstant = Date.from(instant); // Date 新增的静态方法

// 对比：旧 API 互转
// 旧：Date ↔ Calendar 需要 3 行样板代码
// 新：Instant ↔ Date 仅需一行
```

### 4.5 Clock 抽象

`Clock` 是 java.time 的可插拔时钟抽象层，是依赖注入和测试的利器。

```java
// 默认时钟
Clock systemClock = Clock.systemUTC();
Clock defaultZone = Clock.systemDefaultZone();
Clock tickClock   = Clock.tickSeconds(ZoneId.systemDefault()); // 精度到秒

// 获取时间
Instant now1    = systemClock.instant();
long millis     = systemClock.millis();
LocalDate today = LocalDate.now(systemClock);

// === 测试利器：固定时钟 ===

// 固定到特定时刻，测试时不会"随时间漂移"
Clock fixed = Clock.fixed(
        Instant.parse("2024-12-25T10:00:00Z"),
        ZoneId.of("Asia/Shanghai"));

// 测试中的使用
public class OrderValidator {
    private final Clock clock;

    // 依赖注入 Clock
    public OrderValidator(Clock clock) {
        this.clock = clock;
    }

    public boolean isExpired(LocalDateTime expiryDate) {
        return LocalDateTime.now(clock).isAfter(expiryDate);
    }
}

// 测试代码
@Test
void testExpired() {
    Clock fixedClock = Clock.fixed(
            Instant.parse("2024-12-25T10:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    OrderValidator validator = new OrderValidator(fixedClock);

    LocalDateTime notExpired = LocalDateTime.of(2024, 12, 26, 0, 0);
    assertFalse(validator.isExpired(notExpired));
    // 无论真实时间如何，固定时钟保证结果可预测 ✅
}

// 生产代码
// OrderValidator validator = new OrderValidator(Clock.systemDefaultZone());
```

### 4.6 偏移量与截断

```java
Instant now = Instant.now();

// 偏移
Instant later = now.plusSeconds(3600);            // 加 1 小时
Instant earlier = now.minusMillis(5000);          // 减 5 秒

// 截断到指定精度
Instant truncatedToMillis = now.truncatedTo(ChronoUnit.MILLIS);
Instant truncatedToSeconds = now.truncatedTo(ChronoUnit.SECONDS);
Instant truncatedToMinutes = now.truncatedTo(ChronoUnit.MINUTES);

// 比较
boolean before = instant1.isBefore(instant2);
boolean after  = instant1.isAfter(instant2);

// 时间间隔
Duration duration = Duration.between(earlier, later);
long millisBetween = ChronoUnit.MILLIS.between(earlier, later);
```

> 🎯 **核心要点**：Instant 是 UTC 时间线上的纳秒瞬时点，适合跨时区传输和时间戳存储。Clock 抽象让时间相关代码可测试——通过固定时钟消除时间不确定性。

---

## 5. 时区处理

### 5.1 ZoneId

`ZoneId` 表示时区标识符，遵循 IANA Time Zone Database（tzdata）标准。

```java
// === 获取 ZoneId ===

// 系统默认时区
ZoneId defaultZone = ZoneId.systemDefault();
// 示例输出: Asia/Shanghai

// 所有可用时区
Set<String> allZones = ZoneId.getAvailableZoneIds();
System.out.println(allZones.size()); // 600+ 个
// 常用: Asia/Shanghai, America/New_York, Europe/London, Asia/Tokyo

// 指定时区
ZoneId shanghai = ZoneId.of("Asia/Shanghai");
ZoneId newYork  = ZoneId.of("America/New_York");
ZoneId utc      = ZoneId.of("UTC");
ZoneId gmt      = ZoneId.of("GMT+08:00");

// ZoneId 缩写说明
// "Asia/Shanghai" → 完整 IANA ID（推荐）
// "CST" → 有歧义（中国标准时间 / 美国中部标准时间），不推荐
// "UTC" / "GMT" → 无偏移

// ZoneId 与 ZoneOffset 的关系
// ZoneId 包含 ZoneOffset，但 ZoneId 还包含夏令时规则
// ZoneOffset 只是 UTC+X 的固定偏移
ZoneOffset offset = ZoneOffset.of("+08:00");
ZoneId fromOffset = ZoneId.ofOffset("UTC", offset); // UTC+08:00
```

### 5.2 ZonedDateTime

`ZonedDateTime` = `LocalDateTime` + `ZoneId`。它包含了完整的日期时间信息以及时区规则。

```java
// === 创建 ZonedDateTime ===

// 当前时刻在指定时区
ZonedDateTime nowInShanghai =
        ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));

ZonedDateTime nowInNewYork =
        ZonedDateTime.now(ZoneId.of("America/New_York"));
// 上述两个对象表示同一时刻，但显示的本地时间不同

// 从 LocalDateTime + ZoneId
LocalDateTime dt = LocalDateTime.of(2024, 12, 25, 10, 0);
ZonedDateTime zdt = ZonedDateTime.of(dt, ZoneId.of("Asia/Shanghai"));
// 2024-12-25T10:00+08:00[Asia/Shanghai]

// 从 Instant + ZoneId
Instant instant = Instant.now();
ZonedDateTime fromInstant = instant.atZone(ZoneId.of("Asia/Shanghai"));

// 直接 of（所有参数）
ZonedDateTime full = ZonedDateTime.of(
        2024, 12, 25, 10, 0, 0, 0,
        ZoneId.of("Asia/Shanghai"));

// 从字符串解析（ISO 8601 扩展格式）
ZonedDateTime parsed =
        ZonedDateTime.parse("2024-12-25T10:00:00+08:00[Asia/Shanghai]");
```

### 5.3 同一时刻不同时区转换

```java
// 定义北京时间的会议时间
ZonedDateTime meetingInBeijing =
        ZonedDateTime.of(
                LocalDateTime.of(2024, 12, 25, 14, 0),
                ZoneId.of("Asia/Shanghai"));

// 同一时刻在纽约是什么时间？
ZonedDateTime meetingInNewYork =
        meetingInBeijing.withZoneSameInstant(ZoneId.of("America/New_York"));
// 2024-12-25T01:00-05:00[America/New_York]
// 北京 14:00 → 纽约 01:00（时差 13 小时）

// 同一时刻在伦敦是什么时间？
ZonedDateTime meetingInLondon =
        meetingInBeijing.withZoneSameInstant(ZoneId.of("Europe/London"));
// 2024-12-25T06:00Z[Europe/London]

// 对比：只改时区不改本地时间（不同时刻）
ZonedDateTime wrong = meetingInBeijing.withZoneSameLocal(
        ZoneId.of("America/New_York"));
// 2024-12-25T14:00-05:00[America/New_York]
// ⚠️ 这表示"纽约 14:00"，和北京 14:00 不是同一时刻！
```

### 5.4 夏令时（DST）边界行为

夏令时（Daylight Saving Time, DST）是时区处理中最容易出错的场景。java.time 对 DST 边界的处理准确且可预测。

```java
// === 美国东部时区 2024 年夏令时 ===
// 开始：2024-03-10 02:00 → 跳到 03:00（时钟拨快 1 小时）
// 结束：2024-11-03 02:00 → 回到 01:00（时钟拨慢 1 小时）

ZoneId ny = ZoneId.of("America/New_York");

// === 春季"跳跃"：不存在的时间 ===
LocalDateTime springForward =
        LocalDateTime.of(2024, 3, 10, 2, 30);
// 这个时间在纽约时区不存在（02:00 直接跳到 03:00）

ZonedDateTime resolved = springForward.atZone(ny);
System.out.println(resolved);
// 输出: 2024-03-10T03:30-04:00[America/New_York]
// java.time 自动将 02:30 调整到 03:30（夏令时偏移 -04:00）

// 如果希望严格行为，使用 withEarlierOffsetAtOverlap()
ZonedDateTime strict = springForward.atZone(ny)
        .withEarlierOffsetAtOverlap();
// 和上面相同，因为不存在的时间会自动前推

// === 秋季"回拨"：存在两次的时间 ===
LocalDateTime fallBack =
        LocalDateTime.of(2024, 11, 3, 1, 30);
// 这个时间在纽约时区存在两次：
// 第一次 01:30 EDT（夏令时）-04:00
// 第二次 01:30 EST（标准时）-05:00

ZonedDateTime zdt1 = fallBack.atZone(ny);
System.out.println(zdt1);
// 输出: 2024-11-03T01:30-04:00[America/New_York]
// 默认取第一个（夏令时偏移，更早的偏移）

ZonedDateTime zdt2 = zdt1.withLaterOffsetAtOverlap();
System.out.println(zdt2);
// 输出: 2024-11-03T01:30-05:00[America/New_York]
// 使用 later offset（标准时偏移）

// 检查重叠
boolean isOverlap = zdt1.getOffset().equals(zdt2.getOffset()); // false

// 同一个本地时间，但代表不同的 UTC 时刻！
long utcMillis1 = zdt1.toInstant().toEpochMilli();
long utcMillis2 = zdt2.toInstant().toEpochMilli();
// utcMillis1 != utcMillis2（相差 1 小时）
```

### 5.5 OffsetDateTime 与 ZoneOffset

`OffsetDateTime` 只保存 UTC 偏移量（如 `+08:00`），不保存完整的时区规则（如夏令时）。适用于 REST API 传输和数据库存储。

```java
// === ZoneOffset ===
// 固定偏移量，不包含 DST 规则

// 创建
ZoneOffset utc8     = ZoneOffset.of("+08:00");
ZoneOffset utc      = ZoneOffset.UTC;             // +00:00
ZoneOffset negative = ZoneOffset.of("-05:00");
ZoneOffset hours    = ZoneOffset.ofHours(8);      // +08:00
ZoneOffset fromInstant = ZoneOffset.from(
        ZonedDateTime.now(ZoneId.of("Asia/Shanghai")));

// === OffsetDateTime ===
// LocalDateTime + ZoneOffset，适用于数据交换

OffsetDateTime now = OffsetDateTime.now();
// 2024-12-25T14:30:00+08:00

OffsetDateTime odt = OffsetDateTime.of(
        LocalDateTime.of(2024, 12, 25, 14, 30),
        ZoneOffset.of("+08:00"));

// 解析
OffsetDateTime parsed = OffsetDateTime.parse("2024-12-25T14:30:00+08:00");

// 互转
ZonedDateTime zdt = odt.atZoneSameInstant(ZoneId.of("America/New_York"));
OffsetDateTime changed = zdt.toOffsetDateTime();

// === ZonedDateTime vs OffsetDateTime 选型 ===

// ZonedDateTime —— 需要完整时区信息（推荐业务逻辑使用）
// 适用场景：用户时区转换、日程安排、涉及 DST 的业务
ZonedDateTime z = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
// 2024-12-25T14:30:00+08:00[Asia/Shanghai]
// ↑ 保留 Asia/Shanghai 时区信息

// OffsetDateTime —— 只需偏移量（推荐 API/数据库 使用）
// 适用场景：REST 响应、数据库 TIMESTAMP WITH TIME ZONE 列
OffsetDateTime o = OffsetDateTime.now();
// 2024-12-25T14:30:00+08:00
// ↑ 不保留时区信息，只有偏移量
```

### 5.6 常用时区速查表

| IANA ID | 城市 | UTC 偏移（标准时） | DST |
|---------|------|-------------------|-----|
| `Asia/Shanghai` | 上海/北京 | +08:00 | 无 |
| `Asia/Tokyo` | 东京 | +09:00 | 无 |
| `Asia/Seoul` | 首尔 | +09:00 | 无 |
| `Asia/Singapore` | 新加坡 | +08:00 | 无 |
| `Asia/Hong_Kong` | 香港 | +08:00 | 无 |
| `Asia/Kolkata` | 印度 | +05:30 | 无 |
| `Europe/London` | 伦敦 | +00:00 | 有 |
| `Europe/Paris` | 巴黎 | +01:00 | 有 |
| `Europe/Berlin` | 柏林 | +01:00 | 有 |
| `US/Eastern` | 纽约 | -05:00 | 有 |
| `US/Pacific` | 洛杉矶 | -08:00 | 有 |
| `Australia/Sydney` | 悉尼 | +10:00 | 有 |
| `Pacific/Auckland` | 奥克兰 | +12:00 | 有 |
| `UTC` | UTC | +00:00 | 无 |

> 🎯 **核心要点**：业务逻辑用 ZonedDateTime 保留完整时区规则，API/DB 传输用 OffsetDateTime 只保留偏移量。时区转换用 `withZoneSameInstant()`（改时区不改时刻），不要用 `withZoneSameLocal()`（本地时间不变但含义变了）。

---

## 6. Duration 与 Period

### 6.1 Duration：时间量（纳秒精度）

`Duration` 表示基于时间的时间量，精度为纳秒。适用于 `Instant`、`LocalTime`、`LocalDateTime` 之间的时间计算。

```java
// === 创建 Duration ===

// 从数量创建
Duration threeHours   = Duration.ofHours(3);         // PT3H
Duration ninetyMin    = Duration.ofMinutes(90);      // PT1H30M
Duration tenSeconds   = Duration.ofSeconds(10);      // PT10S
Duration precise      = Duration.ofSeconds(10, 500_000_000); // PT10.5S
Duration oneDayNanos  = Duration.ofDays(1);          // PT24H（注意：不是 P1D）
Duration fiveMillis   = Duration.ofMillis(5);        // PT0.005S

// 从字符串解析（ISO 8601 格式）
Duration parsed = Duration.parse("PT1H30M");
// PT1H30M = 1 小时 30 分

Duration moreParsed = Duration.parse("P2DT3H4M5S");
// 2 天 3 小时 4 分 5 秒

// 从时间间隔计算
Duration betweenTimes = Duration.between(
        LocalTime.of(10, 0),
        LocalTime.of(14, 30)
); // PT4H30M

Duration betweenInstants = Duration.between(
        Instant.parse("2024-01-01T00:00:00Z"),
        Instant.parse("2024-01-02T00:00:00Z")
); // PT24H
```

```java
// === 操作 Duration ===

Duration d = Duration.ofHours(2).plusMinutes(30); // PT2H30M

// 获取分量
long days         = d.toDays();                // 0
long hours        = d.toHours();               // 2
long minutes      = d.toMinutes();             // 150
long seconds      = d.toSeconds();             // 9000
long millis       = d.toMillis();              // 9000000
long nanos        = d.toNanos();               // 9000000000000

// 分解为时分秒
long hoursPart   = d.toHoursPart();            // 2（小时部分）
long minutesPart = d.toMinutesPart();          // 30（分钟部分）
long secondsPart = d.toSecondsPart();          // 0（秒部分）
long nanosPart   = d.toNanosPart();            // 0（纳秒部分）

// 加减
Duration longer  = d.plusHours(1);             // PT3H30M
Duration shorter = d.minusMinutes(30);         // PT2H

// 比较
boolean isNegative = d.isNegative();
boolean isZero     = d.isZero();
Duration abs       = d.abs();

// 乘以/除以
Duration doubled = d.multipliedBy(2);           // PT5H
Duration half    = d.dividedBy(2);              // PT1H15M
```

```java
// === 实用案例 ===

// 计算两个时间点之间的差
LocalTime start = LocalTime.of(9, 0);
LocalTime end   = LocalTime.of(17, 30);

Duration workingHours = Duration.between(start, end);
System.out.println(workingHours);               // PT8H30M
System.out.println(workingHours.toMinutes());   // 510

// Instant 版本
Instant startInst = Instant.now();
// ... 执行操作 ...
Instant endInst = Instant.now();
Duration elapsed = Duration.between(startInst, endInst);
System.out.println("耗时: " + elapsed.toMillis() + "ms");

// Duration 作为时间偏移
LocalTime newTime = start.plus(workingHours);  // 17:30
```

### 6.2 Period：日期量（年月日）

`Period` 表示基于日期的时间量，精度到天。适用于 `LocalDate` 之间的日期计算。

```java
// === 创建 Period ===

// 从数量创建
Period twoYears   = Period.ofYears(2);          // P2Y
Period threeMonths = Period.ofMonths(3);        // P3M
Period tenDays    = Period.ofDays(10);          // P10D
Period full       = Period.of(1, 6, 15);        // P1Y6M15D

// 从字符串解析（ISO 8601 格式）
Period parsed = Period.parse("P1Y2M3D");
// P1Y2M3D = 1 年 2 个月 3 天

// 计算两个日期之间的 Period
Period between = Period.between(
        LocalDate.of(2022, 1, 10),
        LocalDate.of(2024, 12, 25)
);
```

```java
// === 操作 Period ===

Period p = Period.of(1, 6, 15);

// 获取分量
int years  = p.getYears();     // 1
int months = p.getMonths();    // 6
int days   = p.getDays();      // 15

// 加减
Period plusMonths  = p.plusMonths(6);    // P1Y12M15D
Period minusYears  = p.minusYears(1);    // P6M15D

// 规范化（将 months > 12 转为年）
Period normalized = p.plusMonths(6).normalized(); // P2Y15D

// 比较
boolean isZero   = p.isZero();
boolean isNegative = p.isNegative();

// Period 作为日期偏移
LocalDate start = LocalDate.of(2024, 1, 1);
LocalDate end   = start.plus(p);           // 2025-07-16
LocalDate back  = end.minus(Period.ofYears(1)); // 2024-07-16
```

```java
// === 实用案例：年龄计算 ===

LocalDate birthday = LocalDate.of(1990, 5, 20);
LocalDate today    = LocalDate.now();

Period age = Period.between(birthday, today);
System.out.printf("年龄: %d岁%d个月%d天%n",
        age.getYears(), age.getMonths(), age.getDays());

// 精确周期计算
LocalDate startWork = LocalDate.of(2015, 7, 1);
LocalDate now       = LocalDate.now();
Period workPeriod = Period.between(startWork, now);
System.out.printf("工作: %d年%d个月%n",
        workPeriod.getYears(), workPeriod.getMonths());
```

### 6.3 Duration vs Period 对比

| 维度 | Duration | Period |
|------|----------|--------|
| 精度 | 纳秒级 | 天级 |
| 适合类 | Instant, LocalTime, LocalDateTime | LocalDate |
| 内部存储 | 秒 + 纳秒 (long + int) | 年 + 月 + 日 (int) |
| ISO 8601 | `PTnHnMnS` (P 开头 + T 分隔) | `PnYnMnD` (P 开头，无 T) |
| 示例 | `PT1H30M` (1.5 小时) | `P1Y2M3D` (1年2月3天) |
| toXxx | toMillis, toNanos, toMinutes | getYears, getMonths, getDays |
| 标准化 | 无（纳秒永远正确） | normalized() 将 12 月转 1 年 |
| 负值 | 支持 | 支持 |

> 🎯 **核心要点**：处理时间量（Instant/LocalTime）用 Duration，处理日期量（LocalDate）用 Period。Duration 精度到纳秒适合计时，Period 适合年龄、租期等人文日期计算。

---

## 7. DateTimeFormatter 格式与解析

### 7.1 预定义格式

`DateTimeFormatter` 提供了多个预定义格式常量，覆盖了 ISO 8601 标准格式。

```java
LocalDateTime dt = LocalDateTime.of(2024, 12, 25, 14, 30, 45, 123_456_789);
LocalDate d = dt.toLocalDate();
LocalTime t = dt.toLocalTime();

// === 预定义日期格式 ===
System.out.println(d.format(DateTimeFormatter.ISO_LOCAL_DATE));
// 2024-12-25

System.out.println(d.format(DateTimeFormatter.ISO_DATE));
// 2024-12-25（也可以输出 +08:00）

System.out.println(d.format(DateTimeFormatter.ISO_ORDINAL_DATE));
// 2024-360（当年的第 360 天）

System.out.println(d.format(DateTimeFormatter.ISO_WEEK_DATE));
// 2024-W52-3（2024年第52周，周三）

// === 预定义时间格式 ===
System.out.println(t.format(DateTimeFormatter.ISO_LOCAL_TIME));
// 14:30:45.123456789

System.out.println(t.format(DateTimeFormatter.ISO_TIME));
// 14:30:45.123456789

// === 预定义日期时间格式 ===
System.out.println(dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
// 2024-12-25T14:30:45.123456789

// === 带时区的格式 ===
ZonedDateTime zdt = dt.atZone(ZoneId.of("Asia/Shanghai"));

System.out.println(zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
// 2024-12-25T14:30:45.123456789+08:00[Asia/Shanghai]

System.out.println(zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
// 2024-12-25T14:30:45.123456789+08:00

System.out.println(zdt.format(DateTimeFormatter.ISO_INSTANT));
// 2024-12-25T06:30:45.123456789Z（转 UTC）

// === 常用国际化格式 ===
System.out.println(dt.format(DateTimeFormatter.ISO_DATE_TIME));
// 2024-12-25T14:30:45.123456789

System.out.println(dt.format(DateTimeFormatter.BASIC_ISO_DATE));
// 20241225（没有分隔符）
```

### 7.2 自定义格式（ofPattern）

使用 `DateTimeFormatter.ofPattern()` 可以定义任意格式的模式字符串。

```java
// === 模式字母速查 ===

// y = year, M = month, d = day
// H = hour (0-23), h = hour (1-12, 需配合 a), m = minute, s = second
// S = fraction of second, n = nano-of-second
// a = AM/PM, E = day name, D = day of year
// z = time zone name, Z = offset (RFC 822), X = offset (ISO 8601)

// === 常用模式 ===

// 标准日期时间
DateTimeFormatter f1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
System.out.println(dt.format(f1));
// 2024-12-25 14:30:45

// 中文格式
DateTimeFormatter f2 = DateTimeFormatter.ofPattern("yyyy年M月d日");
System.out.println(dt.format(f2));
// 2024年12月25日

// 带星期
DateTimeFormatter f3 = DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE");
System.out.println(dt.format(f3));
// 2024-12-25 星期三

// 12小时制 + 上下午
DateTimeFormatter f4 = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
System.out.println(dt.format(f4));
// 2024-12-25 02:30:45 下午

// 带毫秒
DateTimeFormatter f5 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
System.out.println(dt.format(f5));
// 2024-12-25 14:30:45.123

// 紧凑格式
DateTimeFormatter f6 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
System.out.println(dt.format(f6));
// 20241225143045

// ISO 8601 日期+时区
DateTimeFormatter f7 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
System.out.println(zdt.format(f7));
// 2024-12-25T14:30:45+08:00

// 时区名称
DateTimeFormatter f8 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
System.out.println(zdt.format(f8));
// 2024-12-25 14:30:45 CST

// 季度信息（Q = quarter-of-year, q = standalone quarter）
DateTimeFormatter f9 = DateTimeFormatter.ofPattern("yyyy-QQQ");
System.out.println(dt.format(f9));
// 2024-Q4
```

### 7.3 模式字母详解

| 字母 | 字段 | 示例（2024-12-25 14:30:45） |
|------|------|------------------------------|
| `y` | year | `yy` → 24, `yyyy` → 2024 |
| `M` | month-of-year | `M` → 12, `MM` → 12, `MMM` → 12月, `MMMM` → 十二月 |
| `d` | day-of-month | `d` → 25, `dd` → 25 |
| `D` | day-of-year | `D` → 360 |
| `E` | day-of-week | `E` → 周三, `EEEE` → 星期三 |
| `H` | hour-of-day (0-23) | `H` → 14, `HH` → 14 |
| `h` | clock-hour-of-am-pm (1-12) | `h` → 2, `hh` → 02 |
| `m` | minute-of-hour | `m` → 30, `mm` → 30 |
| `s` | second-of-minute | `s` → 45, `ss` → 45 |
| `S` | fraction-of-second | `S` → 1, `SSS` → 123 |
| `n` | nano-of-second | `n` → 123456789 |
| `a` | am-pm-of-day | 上午/下午 |
| `Q` | quarter-of-year | `Q` → 4, `QQQ` → Q4 |
| `z` | time-zone name | CST / 中国标准时间 |
| `V` | time-zone ID | Asia/Shanghai |
| `Z` | zone-offset (RFC 822) | +0800 |
| `X` | zone-offset (ISO 8601) | `X` → +08, `XX` → +0800, `XXX` → +08:00 |
| `O` | localized zone-offset | GMT+8 |
| `'` | 文本转义 | `'T'` → 输出 T |

### 7.4 解析（parse）

```java
// === 使用预定义格式解析 ===
LocalDate date1 = LocalDate.parse("2024-12-25");
// ISO_LOCAL_DATE 是默认格式

LocalDateTime dt1 = LocalDateTime.parse("2024-12-25T14:30:00");
// ISO_LOCAL_DATE_TIME 是默认格式

Instant instant = Instant.parse("2024-12-25T06:30:00Z");
// 默认需要末尾的 Z

// === 使用 ofPattern 解析 ===
DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy/MM/dd");
LocalDate date2 = LocalDate.parse("2024/12/25", parser);

DateTimeFormatter parser2 = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss");
LocalDateTime dt2 = LocalDateTime.parse(
        "2024-12-25 14:30:45", parser2);

DateTimeFormatter parser3 = DateTimeFormatter.ofPattern(
        "yyyy年M月d日");
LocalDate date3 = LocalDate.parse("2024年12月25日", parser3);

// === 宽松解析（lenient） ===
DateTimeFormatter lenientParser = new DateTimeFormatterBuilder()
        .parseLenient()  // 允许 2 月 30 日等非法日期？不，只对部分字段
        .appendPattern("yyyy-MM-dd")
        .toFormatter();
// 注意：java.time 的 lenient 比 Calendar 严格得多
// 即使 lenient，仍然不会接受 2024-02-30

// === 解析时携带默认值 ===
DateTimeFormatter withDefaults = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 30)
        .toFormatter();
// 解析日期字符串时，自动填充时间字段默认值
```

### 7.5 SimpleDateFormat vs DateTimeFormatter 安全性对比

```java
// === SimpleDateFormat 线程不安全（演示） ===
private static final SimpleDateFormat SDF =
        new SimpleDateFormat("yyyy-MM-dd");

// 在多线程环境下同时调用：
// 线程 A: SDF.parse("2024-01-01")
// 线程 B: SDF.parse("2023-06-15")
// 结果：可能返回错误值、null、或抛出异常

// 原因：SimpleDateFormat 内部维护了一个 Calendar 对象
// format() 和 parse() 都会修改这个共享 Calendar 的状态
// 没有任何同步保护

// === DateTimeFormatter 线程安全（演示） ===
private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

// 任意线程同时调用都安全：
// 线程 A: LocalDate.parse("2024-01-01", FORMATTER)
// 线程 B: LocalDate.parse("2023-06-15", FORMATTER)
// 结果：永远正确

// 原因：DateTimeFormatter 是不可变（immutable）的
// 所有字段都是 final，parse 过程不修改自身状态
// 不需要任何同步
```

### 7.6 DateTimeFormatterBuilder 复杂格式

对于非常复杂的格式，可以使用 `DateTimeFormatterBuilder` 逐段构建。

```java
// === 复杂格式构建 ===
DateTimeFormatter complex = new DateTimeFormatterBuilder()
        // 可选部分：年-月-日
        .appendPattern("yyyy[-MM[-dd")
        // 可选结束：支持 "2024", "2024-12", "2024-12-25"
        .optionalEnd()
        .appendLiteral(" ")
        .appendPattern("HH:mm:ss")
        .toFormatter();

LocalDateTime dt1 = LocalDateTime.parse("2024 14:30:00", complex);
// 注意：上面只是演示结构，实际需要处理可选部分逻辑

// === 区分大小写与解析样式 ===
DateTimeFormatter caseInsensitive = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()   // 解析时不区分大小写
        .appendPattern("dd-MMM-yyyy")
        .toFormatter();
LocalDate parsed = LocalDate.parse("25-DEC-2024", caseInsensitive);
// 即使写 "dec" 或 "Dec" 也能解析

// === 可选部分的实际用法 ===
DateTimeFormatter flexible = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .optionalStart()
        .appendLiteral("T")
        .append(DateTimeFormatter.ISO_LOCAL_TIME)
        .optionalEnd()
        .toFormatter();
// 可以解析 "2024-12-25" 和 "2024-12-25T14:30:00"

// === 本地化格式 ===
DateTimeFormatter localized = new DateTimeFormatterBuilder()
        .appendLocalized(
                FormatStyle.FULL,   // 日期样式
                FormatStyle.MEDIUM  // 时间样式
        )
        .toFormatter(Locale.CHINA);
System.out.println(dt.format(localized));
// 2024年12月25日 星期三 14:30:45
```

> 🎯 **核心要点**：DateTimeFormatter 是不可变且线程安全的，可以放心声明为 `static final`。`ofPattern` 覆盖 90% 的需求，`DateTimeFormatterBuilder` 处理剩余 10% 的复杂场景。不再需要 `ThreadLocal<SimpleDateFormat>` 这种丑陋的模式。

---

## 8. TemporalAdjuster

### 8.1 什么是 TemporalAdjuster

`TemporalAdjuster` 是一个函数式接口，用于时间调整策略。它接收一个 `Temporal` 对象，返回调整后的 `Temporal` 对象。

```java
@FunctionalInterface
public interface TemporalAdjuster {
    Temporal adjustInto(Temporal temporal);
}
```

### 8.2 TemporalAdjusters 工厂方法

`TemporalAdjusters` 工具类提供了大量预定义的调整器。

```java
LocalDate today = LocalDate.of(2024, 12, 25); // 周三

// === 下周/月/年相关 ===

// 下一个周一（不含当天）
LocalDate nextMonday = today.with(
        TemporalAdjusters.next(DayOfWeek.MONDAY));
// 2024-12-30（下一个周一）

// 下一个或同一天（含当天）
LocalDate nextOrSameMonday = today.with(
        TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
// 2024-12-25（当天就是周三）

// 上一个周一
LocalDate previousMonday = today.with(
        TemporalAdjusters.previous(DayOfWeek.MONDAY));
// 2024-12-23

// 上一个或同一天
LocalDate previousOrSame = today.with(
        TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY));
// 2024-12-25

// 本月第一个周一
LocalDate firstInMonth = today.with(
        TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
// 2024-12-02

// 本月最后一个周三
LocalDate lastInMonth = today.with(
        TemporalAdjusters.lastInMonth(DayOfWeek.WEDNESDAY));
// 2024-12-25

// === 月边界 ===

// 本月最后一天
LocalDate lastDayOfMonth = today.with(
        TemporalAdjusters.lastDayOfMonth());
// 2024-12-31

// 本月第一天
LocalDate firstDayOfMonth = today.with(
        TemporalAdjusters.firstDayOfMonth());
// 2024-12-01

// 下个月第一天
LocalDate firstDayOfNextMonth = today.with(
        TemporalAdjusters.firstDayOfNextMonth());
// 2025-01-01

// 下一年第一天
LocalDate firstDayOfNextYear = today.with(
        TemporalAdjusters.firstDayOfNextYear());
// 2025-01-01

// === 年边界 ===

// 当年第一天
LocalDate firstDayOfYear = today.with(
        TemporalAdjusters.firstDayOfYear());
// 2024-01-01

// 当年最后一天
LocalDate lastDayOfYear = today.with(
        TemporalAdjusters.lastDayOfYear());
// 2024-12-31
```

### 8.3 自定义 TemporalAdjuster

```java
// === 方式 1：Lambda 表达式 ===

// 下一个工作日（跳过周末）
TemporalAdjuster nextWorkingDay = temporal -> {
    LocalDate date = LocalDate.from(temporal);
    DayOfWeek dow = date.getDayOfWeek();
    int daysToAdd = switch (dow) {
        case FRIDAY    -> 3; // 周五 → 周一
        case SATURDAY  -> 2; // 周六 → 周一
        default        -> 1; // 其他 → 第二天
    };
    return date.plusDays(daysToAdd);
};

LocalDate today       = LocalDate.of(2024, 12, 27); // 周五
LocalDate nextWorkDay = today.with(nextWorkingDay);
// 2024-12-30（周一）

// === 方式 2：实现 TemporalAdjuster 接口 ===

public class BirthdayAdjuster implements TemporalAdjuster {
    private final MonthDay birthday;

    public BirthdayAdjuster(MonthDay birthday) {
        this.birthday = birthday;
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        LocalDate current = LocalDate.from(temporal);
        int currentYear = current.getYear();

        // 今年的生日
        LocalDate thisYearBirthday = LocalDate.of(
                currentYear,
                birthday.getMonth(),
                birthday.getDayOfMonth());

        // 如果已经过了，找明年的
        if (thisYearBirthday.isBefore(current)
                || thisYearBirthday.isEqual(current)) {
            return LocalDate.of(
                    currentYear + 1,
                    birthday.getMonth(),
                    birthday.getDayOfMonth());
        }
        return thisYearBirthday;
    }
}

LocalDate today2 = LocalDate.of(2024, 12, 25);
LocalDate nextBirthday = today2.with(
        new BirthdayAdjuster(MonthDay.of(5, 20))); // 生日 5月20日
// 2025-05-20（今年已过）

// === 方式 3：静态方法返回 TemporalAdjuster ===

public class MyAdjusters {
    public static TemporalAdjuster firstDayOfQuarter() {
        return temporal -> {
            LocalDate date = LocalDate.from(temporal);
            int quarter = (date.getMonthValue() - 1) / 3 + 1;
            int firstMonthOfQuarter = (quarter - 1) * 3 + 1;
            return LocalDate.of(date.getYear(), firstMonthOfQuarter, 1);
        };
    }
}

LocalDate someDate = LocalDate.of(2024, 11, 15);
LocalDate quarterStart = someDate.with(MyAdjusters.firstDayOfQuarter());
// 2024-10-01（Q4 第一天）
```

> 🎯 **核心要点**：TemporalAdjusters 提供了"下周一""本月最后一天"等常用调整器。内置工厂方法不够用时，用 Lambda 或实现接口创建自定义调整器。这是策略模式在 Java API 中的经典应用。

---

## 9. 新旧 API 互转

在迁移到 java.time 的过程中，不可避免地要与旧代码（使用 Date、Calendar）交互。这一节总结了所有互转方式。

### 9.1 Date ↔ Instant

这是最基本、最常用的互转。

```java
// === Date → Instant (Java 8+) ===
Date date = new Date();
Instant instant = date.toInstant();
// 等效于 Instant.ofEpochMilli(date.getTime())
// 但 toInstant() 更简洁、语义更清晰

// === Instant → Date ===
Date dateFromInstant = Date.from(instant);

// 实际应用：旧 API 返回值转 java.time
public static LocalDateTime dateToLocalDateTime(Date date) {
    return date.toInstant()
               .atZone(ZoneId.systemDefault())
               .toLocalDateTime();
}

// 反过来：java.time 转 Date
public static Date localDateTimeToDate(LocalDateTime ldt) {
    return Date.from(
            ldt.atZone(ZoneId.systemDefault())
               .toInstant());
}
```

### 9.2 Date ↔ LocalDate / LocalTime / LocalDateTime

```java
// === Date → LocalDate ===
Date date = new Date();

LocalDate localDate = date.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate();

// === Date → LocalTime ===
LocalTime localTime = date.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalTime();

// === Date → LocalDateTime ===
LocalDateTime localDateTime = date.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();

// === LocalDate → Date ===
LocalDate ld = LocalDate.now();
Date dateFromLocalDate = Date.from(
        ld.atStartOfDay(ZoneId.systemDefault())
          .toInstant());

// === LocalDateTime → Date ===
LocalDateTime ldt = LocalDateTime.now();
Date dateFromLDT = Date.from(
        ldt.atZone(ZoneId.systemDefault())
           .toInstant());
```

### 9.3 Calendar ↔ Instant / ZonedDateTime

```java
// === Calendar → Instant ===
Calendar cal = Calendar.getInstance();
Instant instant = cal.toInstant();  // Java 8+

// === Calendar → ZonedDateTime ===
// 方式 1：通过 Instant 中转
ZonedDateTime zdt1 = cal.toInstant()
        .atZone(cal.getTimeZone().toZoneId());

// 方式 2：直接转换（GregorianCalendar 才有）
ZonedDateTime zdt2 = ((GregorianCalendar) cal)
        .toZonedDateTime();

// === ZonedDateTime → Calendar ===
ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
Calendar calendar = GregorianCalendar.from(zdt);
```

### 9.4 SimpleDateFormat ↔ DateTimeFormatter

```java
// === SimpleDateFormat 模式 → DateTimeFormatter 模式 ===

// SimpleDateFormat 模式：  yyyy-MM-dd HH:mm:ss
// DateTimeFormatter 模式：yyyy-MM-dd HH:mm:ss
// 两者模式字母大部分兼容，但要注意：
// - SimpleDateFormat 的 Y 表示 Week year（与 y 不同）
// - SimpleDateFormat 的 D 表示 Day in year
// - DateTimeFormatter 更严格，不符合模式的会直接报错

// === 转换工具方法示例 ===
public static DateTimeFormatter toDateTimeFormatter(
        SimpleDateFormat sdf) {
    // 获取 SimpleDateFormat 的模式字符串
    String pattern = sdf.toPattern();
    Locale locale = sdf.getLocale();
    return new DateTimeFormatterBuilder()
            .appendPattern(pattern)
            .toFormatter(locale);
}
```

### 9.5 TimeZone ↔ ZoneId

```java
// === TimeZone → ZoneId ===
TimeZone timeZone = TimeZone.getDefault();
ZoneId zoneId = timeZone.toZoneId();  // Java 8+

// === ZoneId → TimeZone ===
ZoneId zid = ZoneId.of("Asia/Shanghai");
TimeZone tz = TimeZone.getTimeZone(zid);
```

### 9.6 Timestamp ↔ LocalDateTime

`java.sql.Timestamp` 是 `java.util.Date` 的子类，但它的主要使用场景是和数据库交互。

```java
// === Timestamp → LocalDateTime ===
Timestamp timestamp = new Timestamp(System.currentTimeMillis());
LocalDateTime ldt = timestamp.toLocalDateTime();  // 直接方法

// === LocalDateTime → Timestamp ===
LocalDateTime localDt = LocalDateTime.now();
Timestamp ts = Timestamp.valueOf(localDt);  // 静态方法

// === 精度处理 ===
// Timestamp 支持纳秒，LocalDateTime 也支持纳秒
// 两者转换不会损失精度
Timestamp ts2 = Timestamp.valueOf(LocalDateTime.now());
System.out.println(ts2.getNanos());               // 纳秒部分
System.out.println(ts2.toLocalDateTime().getNano()); // 一致

// Timestamp → Instant
Instant tsInstant = timestamp.toInstant();

// Instant → Timestamp
Timestamp tsFromInstant = Timestamp.from(Instant.now());
```

### 9.7 互转总结表

| 源类型 | 目标类型 | 方法 | 示例 |
|--------|---------|------|------|
| `Date` | `Instant` | `date.toInstant()` | 最简转换 |
| `Instant` | `Date` | `Date.from(instant)` | 静态工厂 |
| `Date` | `LocalDate` | `date.toInstant().atZone(zone).toLocalDate()` | 需时区 |
| `LocalDate` | `Date` | `Date.from(ld.atStartOfDay(zone).toInstant())` | 需时区 |
| `Calendar` | `Instant` | `calendar.toInstant()` | Java 8+ |
| `Calendar` | `ZonedDateTime` | `calendar.toInstant().atZone(zone)` | 通用 |
| `GregorianCalendar` | `ZonedDateTime` | `gc.toZonedDateTime()` | 直接转换 |
| `ZonedDateTime` | `Calendar` | `GregorianCalendar.from(zdt)` | 需 GregorianCalendar |
| `Timestamp` | `LocalDateTime` | `ts.toLocalDateTime()` | 直接转换 |
| `LocalDateTime` | `Timestamp` | `Timestamp.valueOf(ldt)` | 直接转换 |
| `TimeZone` | `ZoneId` | `timeZone.toZoneId()` | Java 8+ |
| `ZoneId` | `TimeZone` | `TimeZone.getTimeZone(zoneId)` | 静态方法 |
| `SimpleDateFormat` | `DateTimeFormatter` | `ofPattern(sdf.toPattern())` | 模式字符串 |

### 9.8 全功能工具类

```java
/**
 * 日期转换工具类 —— 新旧 API 互转的最佳实践
 * 所有方法都使用系统默认时区，需要指定时区时使用重载版本
 */
public final class DateConvertUtils {

    private DateConvertUtils() { /* 工具类禁止实例化 */ }

    // ===== Date <-> Instant =====

    public static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    public static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    // ===== Date <-> LocalDate =====

    public static LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static Date toDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());
    }

    // ===== Date <-> LocalDateTime =====

    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return Date.from(localDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    // ===== Calendar <-> ZonedDateTime =====

    public static ZonedDateTime toZonedDateTime(Calendar calendar) {
        if (calendar == null) return null;
        if (calendar instanceof GregorianCalendar) {
            return ((GregorianCalendar) calendar).toZonedDateTime();
        }
        return calendar.toInstant()
                .atZone(calendar.getTimeZone().toZoneId());
    }

    public static GregorianCalendar toCalendar(ZonedDateTime zdt) {
        if (zdt == null) return null;
        return GregorianCalendar.from(zdt);
    }

    // ===== Timestamp <-> LocalDateTime =====

    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public static Timestamp toTimestamp(LocalDateTime localDateTime) {
        return localDateTime == null ? null
                : Timestamp.valueOf(localDateTime);
    }

    // ===== Timestamp <-> Instant =====

    public static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
```

> 🎯 **核心要点**：所有新旧 API 互转的路径都是"先转 Instant，再转目标类型"。理解了这个思路，任何组合都能推导出来。对于 GregorianCalendar 可以使用 `toZonedDateTime()` 捷径。

---

## 10. 练习

### 10.1 基础题

**题目 1：生日计算**
给定一个生日 `1995-08-23`，计算到今天（`LocalDate.now()`）的年龄（精确到年、月、日）。

```java
// 参考实现
LocalDate birthday = LocalDate.of(1995, 8, 23);
LocalDate today = LocalDate.now();
Period age = Period.between(birthday, today);
System.out.printf("年龄: %d岁%d个月%d天%n",
        age.getYears(), age.getMonths(), age.getDays());
```

**题目 2：日期格式化转换**
将字符串 `"2024/12/25 14:30:00"` 解析为 `LocalDateTime`，然后格式化为 `"2024年12月25日 14时30分00秒"`。

```java
// 参考实现
DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
LocalDateTime dt = LocalDateTime.parse("2024/12/25 14:30:00", inputFmt);

DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("yyyy年M月d日 H时m分s秒");
System.out.println(dt.format(outputFmt));
// 2024年12月25日 14时30分00秒
```

**题目 3：当月日历**
编写一个方法，输入年份和月份，输出当月的日历（类似 Linux 的 `cal` 命令）。

```java
// 参考思路
public static void printCalendar(int year, int month) {
    LocalDate firstDay = LocalDate.of(year, month, 1);
    LocalDate lastDay = firstDay.with(TemporalAdjusters.lastDayOfMonth());

    System.out.println("  日  一  二  三  四  五  六");

    // 第一天是星期几
    int dowValue = firstDay.getDayOfWeek().getValue() % 7;
    // Java 的 DayOfWeek: Monday=1 ... Sunday=7
    // 我们需要: Sunday=0 ... Saturday=6

    for (int i = 0; i < dowValue; i++) {
        System.out.print("    ");
    }

    for (int day = 1; day <= lastDay.getDayOfMonth(); day++) {
        System.out.printf("%3d ", day);
        LocalDate current = LocalDate.of(year, month, day);
        if (current.getDayOfWeek() == DayOfWeek.SATURDAY) {
            System.out.println();
        }
    }
    System.out.println();
}
```

### 10.2 进阶题

**题目 4：时区转换工具**
实现一个工具方法，输入"源时间 源时区 目标时区"，输出转换后的时间。

```java
// 参考实现
public static ZonedDateTime convertTimezone(
        LocalDateTime localDateTime,
        ZoneId sourceZone,
        ZoneId targetZone) {

    ZonedDateTime sourceZdt = localDateTime.atZone(sourceZone);
    return sourceZdt.withZoneSameInstant(targetZone);
}

// 使用示例
ZonedDateTime result = convertTimezone(
        LocalDateTime.of(2024, 12, 25, 14, 0),
        ZoneId.of("Asia/Shanghai"),
        ZoneId.of("America/New_York")
);
System.out.println(result);
// 2024-12-25T01:00-05:00[America/New_York]
```

**题目 5：工作日计算器**
给定一个开始日期和天数（工作日），计算结束日期（跳过周末）。

```java
// 参考实现
public static LocalDate addWorkingDays(LocalDate start, int workingDays) {
    if (workingDays == 0) return start;
    int direction = workingDays > 0 ? 1 : -1;
    int daysToAdd = Math.abs(workingDays);

    LocalDate result = start;
    while (daysToAdd > 0) {
        result = result.plusDays(direction);
        if (result.getDayOfWeek().getValue() <= 5) { // 周一到周五
            daysToAdd--;
        }
    }
    return result;
}

// 使用示例
LocalDate start = LocalDate.of(2024, 12, 27); // 周五
System.out.println(addWorkingDays(start, 1));   // 2024-12-30（周一）
System.out.println(addWorkingDays(start, 3));   // 2024-12-31（周二）
```

**题目 6：订单过期判断**
一个订单创建于 `2024-12-20T10:30:00`，有效期为 72 小时。判断当前时订单是否已过期。

```java
// 参考实现
LocalDateTime orderCreateTime = LocalDateTime.of(2024, 12, 20, 10, 30);
Duration validDuration = Duration.ofHours(72);

LocalDateTime expirationTime = orderCreateTime.plus(validDuration);
boolean isExpired = LocalDateTime.now().isAfter(expirationTime);
System.out.println(isExpired ? "已过期" : "未过期");
```

### 10.3 面试题

**题目 7：SimpleDateFormat 线程不安全复现**
编写一个程序，演示多线程下 `SimpleDateFormat` 线程不安全的现象。

```java
// 参考思路
public static void demoSimpleDateFormatUnsafe() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    ExecutorService pool = Executors.newFixedThreadPool(10);
    List<Future<?>> futures = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
        final int idx = i;
        futures.add(pool.submit(() -> {
            try {
                // 多线程同时解析，可能得到错误结果
                Date parsed = sdf.parse("2024-01-15");
                // 或抛异常 NumberFormatException / NullPointerException
                System.out.printf("Thread %d: %s%n", idx, parsed);
            } catch (Exception e) {
                System.out.printf("Thread %d ERROR: %s%n", idx, e);
            }
        }));
    }

    pool.shutdown();
}

// 对比：DateTimeFormatter 线程安全
private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

// 100 个线程同时解析也不会出错
// 因为 DateTimeFormatter 是不可变的
```

**题目 8：判断闰年**
用 java.time 判断一个年份是否是闰年。

```java
// 方法 1：LocalDate 的 isLeapYear()
boolean leap = LocalDate.of(2024, 1, 1).isLeapYear(); // true

// 方法 2：Year 类
boolean leap2 = Year.of(2024).isLeap(); // true

// 方法 3：IsoChronology
boolean leap3 = IsoChronology.INSTANCE.isLeapYear(2024); // true

// 传统算法（了解即可）
public static boolean isLeapYear(int year) {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
}
```

**题目 9：获取某月所有周一的日期**
给定 2024 年 12 月，获取该月所有周一的日期。

```java
// 参考实现
YearMonth yearMonth = YearMonth.of(2024, 12);
LocalDate firstMonday = yearMonth.atDay(1)
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

List<LocalDate> mondays = new ArrayList<>();
LocalDate current = firstMonday;
while (current.getMonth() == yearMonth.getMonth()) {
    mondays.add(current);
    current = current.plusWeeks(1);
}
System.out.println(mondays);
// [2024-12-02, 2024-12-09, 2024-12-16, 2024-12-23, 2024-12-30]
```

**题目 10：全球会议时间查询**
某会议系统允许用户设置"每周一 10:00（Asia/Shanghai）"，请查询在给定的一周内，该会议在 New York、London、Tokyo 三个时区的对应时间。

```java
// 参考实现
public static Map<String, ZonedDateTime> meetingTimesForWeek(
        LocalDate weekStartMonday,
        LocalTime meetingTime,
        ZoneId sourceZone) {

    LocalDateTime weekMeeting = LocalDateTime.of(weekStartMonday, meetingTime);
    ZonedDateTime sourceZdt = weekMeeting.atZone(sourceZone);

    Map<String, ZonedDateTime> result = new LinkedHashMap<>();
    result.put("New York", sourceZdt.withZoneSameInstant(
            ZoneId.of("America/New_York")));
    result.put("London", sourceZdt.withZoneSameInstant(
            ZoneId.of("Europe/London")));
    result.put("Tokyo", sourceZdt.withZoneSameInstant(
            ZoneId.of("Asia/Tokyo")));
    return result;
}

// 调用
Map<String, ZonedDateTime> times = meetingTimesForWeek(
        LocalDate.of(2024, 12, 30), // 周一
        LocalTime.of(10, 0),
        ZoneId.of("Asia/Shanghai")
);
times.forEach((city, zdt) ->
        System.out.printf("%s: %s%n", city,
                zdt.format(DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd HH:mm z"))));
// New York: 2024-12-29 21:00 EST
// London:  2024-12-30 02:00 GMT
// Tokyo:   2024-12-30 11:00 JST
```

### 10.4 总结与避坑清单

| # | 常见错误 | 正确做法 | 说明 |
|:--:|---------|---------|------|
| 1 | `new Date()` 用于日期运算 | 使用 `LocalDate` / `LocalDateTime` | 旧 API 可变且 API 混乱 |
| 2 | 静态共享 `SimpleDateFormat` | 使用 `DateTimeFormatter`（不可变） | 线程安全，可 `static final` |
| 3 | `Calendar.MONTH` 从 0 计算 | `LocalDate.getMonthValue()` 从 1 开始 | 避免臭名昭著的 +1/-1 |
| 4 | 用 `Date` 表示"日期"却包含时间 | 使用 `LocalDate` / `YearMonth` / `MonthDay` | 职责单一原则 |
| 5 | 用 `long` 毫秒值做日期运算 | 使用 `Duration` / `Period` | 类型明确，不易出错 |
| 6 | `ZonedDateTime` 转 `LocalDate` 不含时区丢失信息 | 不丢失——`toLocalDate()` 基于本地日期 | 正确的设计 |
| 7 | 时区转换用 `withZoneSameLocal` | 使用 `withZoneSameInstant` | 前者是改时区不改数字（时刻变了），后者是数字跟着时区变（时刻不变） |
| 8 | 用字符串拼接格式化日期 | 使用 `DateTimeFormatter` | 国际化、类型安全 |
| 9 | 忽略 DST 直接算时差 | 使用 `ZonedDateTime` + `Duration.between` | DST 切换时偏移量会变化 |
| 10 | `getMonth()` 返回 `Month` 枚举却被当成数字 | 使用 `getMonthValue()` 获取 1~12 | 类型安全与转型需求分离 |

> 🎯 **核心要点**：最佳实践一句话总结——**新代码全用 java.time，旧代码通过 `Date.toInstant()` 桥接**。当有日期运算需求时，先想想"这是日期还是时间？用 Period 还是 Duration？需不需要时区？"

---

**上一模块**：[03-Java IO与NIO体系全解](./03-Java%20IO与NIO体系全解.md)
**下一模块**：[05-Optional与防御性编程实战](./05-Optional与防御性编程实战.md)
**返回总览**：[00-Java API知识体系总览](./00-Java API知识体系总览.md)
