03.18 15:07
Java日期和时间API全解析（传统版+Java8+新版）
Java的日期时间处理分为两大体系：一是JDK 1.0就存在的传统旧API（Date、Calendar、SimpleDateFormat），二是Java 8正式推出的全新java.time包API（JSR-310规范）。旧API存在线程不安全、设计混乱、易用性差等诸多缺陷，新版API彻底解决了这些问题，成为当下企业开发的首选。本文将完整梳理两类API的用法、核心区别、实战技巧及避坑要点，适配日常开发全场景。
一、传统日期时间API（Java 8之前，不推荐新项目使用）
1. 核心类概述
传统API主要分布在java.util和java.text包下，核心类包括：
java.util.Date：表示特定的瞬间，精确到毫秒，同时承载日期和时间，设计存在歧义，年份从1900年开始计算，月份从0开始（0代表1月），极易出错。
java.util.Calendar：抽象类，用于日期时间的计算、字段获取与修改，解决了Date的部分缺陷，但依旧线程不安全，API调用繁琐。
java.text.SimpleDateFormat：用于日期字符串和Date对象的互相转换，核心缺陷：线程不安全，多线程环境下直接使用会导致数据错乱、解析异常，严禁定义为静态全局变量。
2. 基础用法示例
// 1. Date类基本用法
Date date = new Date(); // 获取当前系统时间
System.out.println(date); // 输出格式如Wed Mar 18 15:30:20 CST 2026
// 2. Calendar类用法（获取指定字段、日期计算）
Calendar calendar = Calendar.getInstance();
calendar.setTime(date); // 绑定Date对象
int year = calendar.get(Calendar.YEAR); // 获取年份
int month = calendar.get(Calendar.MONTH) + 1; // 月份+1修正
int day = calendar.get(Calendar.DAY_OF_MONTH); // 获取日期
calendar.add(Calendar.DAY_OF_MONTH, 7); // 日期加7天
// 3. SimpleDateFormat格式化与解析（单线程环境）
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
String dateStr = sdf.format(date); // Date转字符串
Date parseDate = sdf.parse("2026-03-18 15:30:00"); // 字符串转Date
3. 致命缺陷与避坑提醒
线程不安全：SimpleDateFormat和Calendar均未实现线程安全，多线程并发场景下直接共享使用，会引发解析异常、日期结果错误，必须每次使用新建实例，或用ThreadLocal封装。
设计不规范：Date月份从0开始、年份偏移1900，语义模糊；Calendar字段常量繁多，记忆和调用成本高。
功能薄弱：缺少对时区、日期间隔、ISO标准的完善支持，复杂日期计算代码冗余。
二、Java 8+全新日期时间API（java.time包，推荐首选）
Java 8引入的java.time包是由Joda-Time作者主导设计的规范API，所有类均为不可变类、线程安全，语义清晰、分工明确，完美解决了传统API的所有痛点，是企业级开发的标准选择。核心设计理念：按职责拆分不同类，分别处理日期、时间、日期时间、时区、时间间隔等场景。
1. 核心核心类及职责分工
核心类
包路径
核心作用
特点
LocalDate
java.time
仅表示日期（年月日），无时间、无时区
纯日期，适合生日、订单日期等场景
LocalTime
java.time
仅表示时间（时分秒毫秒），无日期、无时区
纯时间，适合打卡时间、时段判断等场景
LocalDateTime
java.time
表示日期+时间（年月日时分秒），无时区
最常用，替代传统Date，本地日期时间
ZonedDateTime
java.time
带时区的日期时间，完整时区信息
跨时区项目、国际业务专用
Instant
java.time
时间戳，表示UTC时间轴上的瞬间，精确到纳秒
替代Date的时间戳功能，机器时间
Duration
java.time
计算两个时间的间隔（时分秒毫秒）
时间差，适用于LocalTime、LocalDateTime
Period
java.time
计算两个日期的间隔（年月日）
日期差，适用于LocalDate
DateTimeFormatter
java.time.format
日期时间格式化与解析
线程安全，替代SimpleDateFormat
2. 常用基础操作（实战高频用法）
2.1 获取当前时间/指定时间
// 获取当前本地日期、时间、日期时间
LocalDate nowDate = LocalDate.now();
LocalTime nowTime = LocalTime.now();
LocalDateTime nowDateTime = LocalDateTime.now();
// 指定具体日期时间（月份直接用1-12，无需偏移）
LocalDate指定Date = LocalDate.of(2026, 3, 18);
LocalDateTime 指定DateTime = LocalDateTime.of(2026, 3, 18, 15, 30, 20);
// 获取时间戳（Instant）
Instant instant = Instant.now(); // UTC时间
long milli = instant.toEpochMilli(); // 转毫秒时间戳
2.2 日期时间格式化与解析（线程安全）
DateTimeFormatter是线程安全的，可定义为静态常量全局复用，彻底规避传统API的线程风险，支持自定义格式和内置标准格式。
// 定义格式化器（线程安全，可静态final）
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
// LocalDateTime转字符串
String dateStr = nowDateTime.format(formatter);
// 字符串转LocalDateTime
LocalDateTime parseDateTime = LocalDateTime.parse("2026-03-18 15:30:20", formatter);
// 内置标准格式（ISO格式）
String isoStr = nowDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
2.3 日期时间计算（加减、比较、字段获取）
LocalDateTime now = LocalDateTime.now();
// 日期时间加减（链式调用，返回新对象，原对象不变）
LocalDateTime plus7Day = now.plusDays(7); // 加7天
LocalDateTime minus1Month = now.minusMonths(1); // 减1个月
LocalDateTime plus2Hour = now.plusHours(2); // 加2小时
// 获取单独字段
int year = now.getYear();
int month = now.getMonthValue(); // 1-12，无需修正
int day = now.getDayOfMonth();
int hour = now.getHour();
// 日期比较
boolean isAfter = now.isAfter(LocalDateTime.of(2026, 1, 1, 0, 0)); // 是否之后
boolean isBefore = now.isBefore(plus7Day); // 是否之前
boolean isEqual = now.isEqual(now); // 是否相等
2.4 日期/时间间隔计算
// 1. Period计算日期差（年月日）
LocalDate startDate = LocalDate.of(2025, 12, 18);
LocalDate endDate = LocalDate.of(2026, 3, 18);
Period period = Period.between(startDate, endDate);
int years = period.getYears();
int months = period.getMonths();
int days = period.getDays();
// 2. Duration计算时间差（时分秒毫秒）
LocalDateTime start = LocalDateTime.of(2026, 3, 18, 10, 0, 0);
LocalDateTime end = LocalDateTime.of(2026, 3, 18, 15, 30, 0);
Duration duration = Duration.between(start, end);
long hours = duration.toHours(); // 总小时差
long minutes = duration.toMinutes(); // 总分钟差
long seconds = duration.getSeconds(); // 总秒数差
2.5 时区处理（ZonedDateTime）
// 获取指定时区当前时间
ZonedDateTime shanghaiTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
ZonedDateTime newYorkTime = ZonedDateTime.now(ZoneId.of("America/New_York"));
// LocalDateTime转带时区时间
ZonedDateTime localToZone = nowDateTime.atZone(ZoneId.of("Asia/Shanghai"));
3. 新旧API互转（兼容老项目）
老项目遗留代码用到传统Date、Calendar，可无缝转换为新版API，逐步重构优化：
// Date转Instant、LocalDateTime
Date date = new Date();
Instant instant = date.toInstant();
LocalDateTime dateToLocalDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
// LocalDateTime转Date
LocalDateTime localDateTime = LocalDateTime.now();
Instant instant2 = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
Date localToDate = Date.from(instant2);
// Calendar转LocalDateTime
Calendar calendar = Calendar.getInstance();
LocalDateTime calendarToLocal = LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
三、核心注意事项与实战规范
强制规范：新项目严禁使用SimpleDateFormat、Date、Calendar，一律改用java.time包下的线程安全类，DateTimeFormatter可直接定义为静态常量复用。
不可变性：新版API所有类都是不可变类，加减计算会返回新对象，原对象不会被修改，无需担心并发修改问题。
时区区分：LocalDateTime不带时区，仅适用于本地业务；跨时区、国际业务必须用ZonedDateTime，避免时间错乱。
格式规范：格式化pattern避免用易错写法，年份用yyyy（4位年），避免用YY；月份用MM（2位），小时用HH（24小时制），避免用hh（12小时制）。
异常处理：解析字符串时，格式不匹配会抛出DateTimeException，必须做异常捕获，防止程序崩溃。
四、快速选型总结
纯日期：LocalDate
纯时间：LocalTime
本地日期+时间（常用）：LocalDateTime
跨时区业务：ZonedDateTime
时间戳/机器时间：Instant
格式化解析：DateTimeFormatter
新版Java日期时间API彻底解决了传统API的设计缺陷和线程安全问题，代码更简洁、语义更清晰、运行更稳定，熟练掌握这套API是Java后端开发的必备技能，尤其在微服务、高并发场景下，优势更为明显。

