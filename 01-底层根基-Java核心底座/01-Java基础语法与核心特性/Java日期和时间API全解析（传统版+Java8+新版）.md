# Java 日期和时间 API 全解析（传统版 + Java 8+ 新版）

> **定位**：Java 日期时间处理分为两大体系——JDK 1.0 传统 API（`Date`/`Calendar`/`SimpleDateFormat`）和 Java 8 `java.time` 包（JSR-310 规范）。新版 API 所有类均为**不可变类、线程安全**，彻底解决了传统 API 的缺陷，是企业开发首选。

---

## 目录

1. [传统 API](#1-传统-api)
2. [Java 8+ 新版 API](#2-java-8-新版-api)
3. [新旧互转](#3-新旧互转)
4. [核心注意事项](#4-核心注意事项)
5. [快速选型](#5-快速选型)

---

## 1. 传统 API

> ⚠️ 不推荐新项目使用，仅用于维护老代码。

### 1.1 核心类

| 类 | 包 | 用途 | 致命缺陷 |
|----|-----|------|----------|
| `Date` | `java.util` | 表示瞬间（日期+时间），精确到毫秒 | 年份从 1900 偏移，月份从 0 开始 |
| `Calendar` | `java.util` | 日期计算、字段获取 | ⚠️ 线程不安全，API 繁琐 |
| `SimpleDateFormat` | `java.text` | 日期字符串 ↔ `Date` | ⚠️ 线程不安全，严禁静态全局变量 |

### 1.2 基础用法

```java
// Date
Date date = new Date();
System.out.println(date);  // Wed Mar 18 15:30:20 CST 2026

// Calendar
Calendar calendar = Calendar.getInstance();
calendar.setTime(date);
int year = calendar.get(Calendar.YEAR);
int month = calendar.get(Calendar.MONTH) + 1;  // ⚠️ 月份 +1 修正
int day = calendar.get(Calendar.DAY_OF_MONTH);
calendar.add(Calendar.DAY_OF_MONTH, 7);         // 日期加 7 天

// SimpleDateFormat（单线程环境）
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
String dateStr = sdf.format(date);              // Date → 字符串
Date parseDate = sdf.parse("2026-03-18 15:30:00");  // 字符串 → Date
```

### 1.3 缺陷总结

| 缺陷 | 说明 |
|------|------|
| **线程不安全** | `SimpleDateFormat` 和 `Calendar` 多线程共享会数据错乱 |
| **设计混乱** | `Date` 月份从 0 开始、年份偏移 1900 |
| **功能薄弱** | 缺少时区、日期间隔、ISO 标准支持 |

---

## 2. Java 8+ 新版 API

> 由 Joda-Time 作者主导设计，线程安全、语义清晰。核心设计理念：**按职责拆分，分别处理日期、时间、时区、间隔。**

### 2.1 核心类速查

| 类 | 用途 | 特点 |
|----|------|------|
| `LocalDate` | 纯日期（年月日） | 生日、订单日期 |
| `LocalTime` | 纯时间（时分秒毫秒） | 打卡时间、时段判断 |
| `LocalDateTime` | 日期+时间（无时区） | **最常用**，替代 `Date` |
| `ZonedDateTime` | 带时区的日期时间 | 跨时区业务、国际项目 |
| `Instant` | UTC 时间戳（纳秒精度） | 机器时间，替代 `Date` 时间戳 |
| `Duration` | 时间差（时分秒） | 适用于 `LocalTime`、`LocalDateTime` |
| `Period` | 日期差（年月日） | 适用于 `LocalDate` |
| `DateTimeFormatter` | 格式化与解析 | ✅ 线程安全，可 static final 复用 |

### 2.2 创建与获取

```java
// 获取当前时间
LocalDate nowDate = LocalDate.now();
LocalTime nowTime = LocalTime.now();
LocalDateTime nowDateTime = LocalDateTime.now();

// 指定日期时间（月份直接用 1-12，无需偏移 ✅）
LocalDate date = LocalDate.of(2026, 3, 18);
LocalDateTime dateTime = LocalDateTime.of(2026, 3, 18, 15, 30, 20);

// 时间戳
Instant instant = Instant.now();
long milli = instant.toEpochMilli();  // 毫秒时间戳
```

### 2.3 格式化与解析

```java
// DateTimeFormatter 线程安全，可 static final 复用
private static final DateTimeFormatter FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

// LocalDateTime → 字符串
String str = nowDateTime.format(FMT);

// 字符串 → LocalDateTime
LocalDateTime parsed = LocalDateTime.parse("2026-03-18 15:30:20", FMT);

// 内置 ISO 格式
String iso = nowDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
```

### 2.4 日期计算（不可变，返回新对象）

```java
LocalDateTime now = LocalDateTime.now();

LocalDateTime plus7Day = now.plusDays(7);        // +7 天
LocalDateTime minus1Month = now.minusMonths(1);  // -1 月
LocalDateTime plus2Hour = now.plusHours(2);      // +2 小时

// 获取字段（1-12，无需修正 ✅）
int year = now.getYear();
int month = now.getMonthValue();  // 1-12
int day = now.getDayOfMonth();

// 比较
boolean after = now.isAfter(LocalDateTime.of(2026, 1, 1, 0, 0));
boolean before = now.isBefore(plus7Day);
```

### 2.5 日期/时间间隔

```java
// Period：日期差（年月日）
LocalDate start = LocalDate.of(2025, 12, 18);
LocalDate end = LocalDate.of(2026, 3, 18);
Period period = Period.between(start, end);
period.getYears();   // 0
period.getMonths();  // 3
period.getDays();    // 0

// Duration：时间差（时分秒）
LocalDateTime t1 = LocalDateTime.of(2026, 3, 18, 10, 0, 0);
LocalDateTime t2 = LocalDateTime.of(2026, 3, 18, 15, 30, 0);
Duration duration = Duration.between(t1, t2);
duration.toHours();    // 5
duration.toMinutes();  // 330
```

### 2.6 时区处理

```java
ZonedDateTime shanghai = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
ZonedDateTime newYork = ZonedDateTime.now(ZoneId.of("America/New_York"));

// LocalDateTime → ZonedDateTime
ZonedDateTime zoned = nowDateTime.atZone(ZoneId.of("Asia/Shanghai"));
```

---

## 3. 新旧互转

| 方向 | 代码 |
|------|------|
| `Date` → `LocalDateTime` | `LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())` |
| `LocalDateTime` → `Date` | `Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())` |
| `Calendar` → `LocalDateTime` | `LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault())` |

---

## 4. 核心注意事项

| 规范 | 说明 |
|------|------|
| **强制用新版** | 新项目严禁 `SimpleDateFormat`/`Date`/`Calendar` |
| **不可变性** | 新版 API 所有类不可变，加减返回新对象，无并发问题 |
| **时区区分** | `LocalDateTime` 无时区 → 本地业务；`ZonedDateTime` → 跨时区 |
| **格式规范** | 年份用 `yyyy`（非 `YY`），月份用 `MM`，小时用 `HH`（24h，非 `hh`） |
| **异常处理** | 解析格式不匹配抛 `DateTimeException`，需捕获 |

---

## 5. 快速选型

| 场景 | 使用类 |
|------|--------|
| 纯日期 | `LocalDate` |
| 纯时间 | `LocalTime` |
| 本地日期+时间（最常用） | `LocalDateTime` |
| 跨时区业务 | `ZonedDateTime` |
| 时间戳/机器时间 | `Instant` |
| 格式化解析 | `DateTimeFormatter` |

---

> 🎯 **核心总结**：新版 `java.time` API 彻底解决了传统 API 的线程安全和设计缺陷。所有类不可变、线程安全，`DateTimeFormatter` 可直接 `static final` 复用。代码更简洁、语义更清晰，是 Java 后端开发的必备技能。
