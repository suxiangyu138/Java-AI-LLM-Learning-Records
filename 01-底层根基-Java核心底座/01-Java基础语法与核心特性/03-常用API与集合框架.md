# 03 - 常用 API 与集合框架 (Common APIs & Collections Framework)

## 学习目标

- 掌握 String/StringBuilder/StringBuffer 的使用与性能比较
- 理解包装类的自动装箱/拆箱机制和缓存策略
- 熟练使用 Java 日期时间 API（java.time）
- 深入理解集合框架体系及各实现类的底层原理
- 掌握 Stream API 的函数式编程风格
- 熟练使用 Optional 进行优雅的空值处理

---

## 1. String、StringBuilder、StringBuffer

### 1.1 String 的特性

```java
public class StringFeatures {

    public static void main(String[] args) {
        // 1. 不可变性（Immutable）
        String s1 = "Hello";
        String s2 = s1.concat(" World");
        System.out.println(s1);  // "Hello"（原字符串不变）
        System.out.println(s2);  // "Hello World"（创建新字符串）

        // 2. 字符串池（String Pool）
        String a = "Java";               // 字符串字面量 → 入池
        String b = "Java";               // 复用池中的对象
        System.out.println(a == b);      // true（同一引用）

        String c = new String("Java");   // 强制创建新对象
        System.out.println(a == c);      // false
        System.out.println(a.equals(c)); // true（比较内容）

        // 3. intern() 方法：手动入池
        String d = c.intern();           // 返回池中的引用
        System.out.println(a == d);      // true

        // 4. 拼接优化（编译期常量折叠）
        String e = "Hello" + " " + "World";  // 编译期优化为 "Hello World"
        String f = "Hello World";
        System.out.println(e == f);           // true（编译期常量）

        String hello = "Hello";
        String world = "World";
        String g = hello + " " + world;  // 运行时拼接（底层用 StringBuilder）
        System.out.println(g == f);      // false

        // 5. 常用方法
        String str = "  Hello Java World  ";
        System.out.println(str.length());           // 19
        System.out.println(str.trim());             // "Hello Java World"
        System.out.println(str.charAt(2));          // 'H'
        System.out.println(str.indexOf("Java"));    // 8
        System.out.println(str.substring(2, 7));    // "Hello"
        System.out.println(str.replace("Java", "Python")); // "  Hello Python World  "
        System.out.println(str.split(" ").length);  // 5
        System.out.println("".isEmpty());           // true
        System.out.println("  ".isBlank());         // true（JDK 11+）
        System.out.println("abc\n".strip());        // "abc"（JDK 11+）
    }
}
```

**String 常量池（JDK 7+）**:

```
JDK 6 及之前: String Pool 在方法区（永久代）
JDK 7+:        String Pool 移到堆中
              - 池中存的是引用而非实际字符串对象
              - 字符串对象本身在堆中
```

### 1.2 StringBuilder 与 StringBuffer

```java
public class StringComparison {
    public static void main(String[] args) {
        // StringBuilder：线程不安全，性能最好（单线程推荐）
        StringBuilder sb = new StringBuilder();
        sb.append("Hello")
          .append(" ")
          .append("World")
          .insert(5, " Java");
        System.out.println(sb.toString());  // "Hello Java World"

        // StringBuffer：线程安全（方法有 synchronized），性能略差
        StringBuffer sbf = new StringBuffer();
        sbf.append("Hello");
        sbf.append(" World");
        System.out.println(sbf.toString());

        // 指定初始容量（避免频繁扩容）
        StringBuilder sb2 = new StringBuilder(1024);
        sb2.append("Large text...");
    }
}
```

### 1.3 性能对比

| 操作 | String | StringBuilder | StringBuffer |
|------|--------|--------------|-------------|
| 拼接少量字符串 | O(n) 但会创建很多临时对象 | O(1) 追加 | O(1) 追加 |
| 拼接大量字符串（循环中） | O(n^2) 极度缓慢 | O(n) 快速 | O(n) 稍慢 |
| 线程安全 | 不可变（天然安全） | 不安全 | 安全 |
| 推荐场景 | 少量固定拼接、多线程共享 | 单线程大量拼接 | 多线程大量拼接 |

```java
// 极力避免：循环中拼接 String
String result = "";
for (int i = 0; i < 10000; i++) {
    result += i;         // 每次创建新对象，O(n^2)
}

// 正确做法
StringBuilder result = new StringBuilder(100000);
for (int i = 0; i < 10000; i++) {
    result.append(i);    // O(n)，推荐指定初始容量
}
```

> **阿里巴巴规范**: 循环体内字符串拼接使用 StringBuilder 的 `append()` 方法进行扩展，不要使用 `+` 号。

---

## 2. Math 与 Random 类

```java
public class MathAndRandom {

    public static void main(String[] args) {
        // Math 类：所有方法都是 static
        System.out.println(Math.abs(-10));         // 10
        System.out.println(Math.max(5, 8));        // 8
        System.out.println(Math.min(5, 8));        // 5
        System.out.println(Math.pow(2, 10));       // 1024.0
        System.out.println(Math.sqrt(16));         // 4.0
        System.out.println(Math.round(3.6));       // 4（四舍五入）
        System.out.println(Math.floor(3.6));       // 3.0
        System.out.println(Math.ceil(3.1));        // 4.0
        System.out.println(Math.random());         // [0.0, 1.0) 随机数
        System.out.println(Math.PI);               // 3.141592653589793
        System.out.println(Math.E);                // 2.718281828459045

        // 生成 [0, 99] 随机整数
        int rand = (int) (Math.random() * 100);

        // Random 类（推荐：功能更丰富）
        Random random = new Random();              // 基于当前时间戳种子
        // Random random = new Random(42L);        // 固定种子（可复现）
        System.out.println(random.nextInt());      // 任意整数
        System.out.println(random.nextInt(100));   // [0, 99]
        System.out.println(random.nextLong());
        System.out.println(random.nextDouble());    // [0.0, 1.0)
        System.out.println(random.nextBoolean());
        System.out.println(random.nextFloat());

        // JDK 17+ 新增 SplittableRandom（更快，适用于并行计算）
        SplittableRandom sr = new SplittableRandom();
        System.out.println(sr.nextInt(100));

        // ThreadLocalRandom：线程安全的随机数（并发场景）
        int threadRand = ThreadLocalRandom.current().nextInt(100);
    }
}
```

---

## 3. 包装类（Wrapper Classes）

### 3.1 基本类型与包装类对照

| 基本类型 | 包装类 | 默认值 | 大小 |
|----------|--------|--------|------|
| `boolean` | `Boolean` | false | / |
| `byte` | `Byte` | 0 | 1 byte |
| `short` | `Short` | 0 | 2 bytes |
| `int` | `Integer` | 0 | 4 bytes |
| `long` | `Long` | 0L | 8 bytes |
| `float` | `Float` | 0.0f | 4 bytes |
| `double` | `Double` | 0.0d | 8 bytes |
| `char` | `Character` | 空字符 | 2 bytes |

### 3.2 自动装箱与拆箱

```java
public class BoxingDemo {

    public static void main(String[] args) {
        // 自动装箱（Autoboxing）
        Integer i1 = 100;                    // 等效于 Integer.valueOf(100)

        // 自动拆箱（Unboxing）
        int i2 = i1;                         // 等效于 i1.intValue()

        // 运算中的自动拆箱
        Integer a = 10;
        Integer b = 20;
        Integer c = a + b;                   // a 和 b 拆箱，运算后装箱
        System.out.println(c);               // 30

        // 陷阱：null 拆箱 -> NullPointerException
        Integer n = null;
        // int m = n;                        // NullPointerException!

        // 陷阱：性能问题（循环中大量装箱）
        long start = System.nanoTime();
        Integer sum1 = 0;
        for (int i = 0; i < 10000; i++) {
            sum1 += i;                       // 装箱拆箱 10000 次！
        }
        System.out.println("包装类: " + (System.nanoTime() - start));

        long start2 = System.nanoTime();
        int sum2 = 0;
        for (int i = 0; i < 10000; i++) {
            sum2 += i;                       // 无装箱拆箱
        }
        System.out.println("基本类型: " + (System.nanoTime() - start2));
    }
}
```

### 3.3 Integer 缓存机制（享元模式）

```java
public class IntegerCacheDemo {

    public static void main(String[] args) {
        // Integer 缓存范围：-128 ~ 127（默认）
        Integer i1 = 127;
        Integer i2 = 127;
        System.out.println(i1 == i2);        // true（同一缓存对象）

        Integer i3 = 128;
        Integer i4 = 128;
        System.out.println(i3 == i4);        // false（超过缓存范围，新建对象）

        // 通过 JVM 参数扩展缓存上限
        // -XX:AutoBoxCacheMax=2000

        // 使用 valueOf 自动使用缓存（推荐）
        Integer i5 = Integer.valueOf(127);   // 使用缓存
        Integer i6 = new Integer(127);       // 强制创建新对象（不推荐）

        // Long 也有缓存：-128 ~ 127
        Long l1 = 127L;
        Long l2 = 127L;
        System.out.println(l1 == l2);        // true

        // Double/Float 没有缓存
        Double d1 = 1.0;
        Double d2 = 1.0;
        System.out.println(d1 == d2);        // false
    }
}
```

**规则**: 包装类对象比较始终使用 `equals()`，不要使用 `==`！

### 3.4 包装类工具方法

```java
public class WrapperUtils {
    public static void main(String[] args) {
        // 字符串转数字
        int i = Integer.parseInt("123");          // "123" -> 123
        long l = Long.parseLong("123456");        // "123456" -> 123456L
        double d = Double.parseDouble("3.14");    // "3.14" -> 3.14
        boolean b = Boolean.parseBoolean("true"); // "true" -> true

        // 数字转字符串
        String s1 = Integer.toString(123);        // 123 -> "123"
        String s2 = Integer.toHexString(255);     // 255 -> "ff"（十六进制）
        String s3 = Integer.toBinaryString(10);   // 10 -> "1010"（二进制）

        // 进制转换
        Integer.valueOf("ff", 16);                // 255
        Integer.valueOf("1010", 2);               // 10

        // 常量
        System.out.println(Integer.MAX_VALUE);    // 2147483647
        System.out.println(Integer.MIN_VALUE);    // -2147483648
        System.out.println(Integer.SIZE);         // 32（位）
        System.out.println(Integer.BYTES);        // 4
    }
}
```

---

## 4. 日期时间 API（java.time，JDK 8+）

### 4.1 为什么替代旧的 Date/Calendar

| 问题 | java.util.Date | java.time |
|------|---------------|-----------|
| 可变性 | Date 是可变的 | 所有类不可变，线程安全 |
| 可读性 | `Calendar.MONTH` 从 0 开始 | 月份从 1 开始 |
| 设计混乱 | 日期和时间在同一类中 | 分离为 LocalDate/LocalTime/LocalDateTime |
| 时区处理 | 复杂且易出错 | ZoneId/ZonedDateTime 清晰 |
| 格式化 | SimpleDateFormat 线程不安全 | DateTimeFormatter 线程安全 |

### 4.2 LocalDate / LocalTime / LocalDateTime

```java
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateTimeDemo {

    public static void main(String[] args) {
        // ---- 创建 ----
        LocalDate today = LocalDate.now();                    // 当前日期
        LocalDate specificDate = LocalDate.of(2024, 3, 15);  // 指定日期
        LocalDate parsed = LocalDate.parse("2024-03-15");    // 解析字符串

        LocalTime now = LocalTime.now();                      // 当前时间
        LocalTime specificTime = LocalTime.of(14, 30, 0);    // 14:30:00
        LocalTime parsedTime = LocalTime.parse("14:30:00");

        LocalDateTime dt = LocalDateTime.now();               // 当前日期+时间
        LocalDateTime dt2 = LocalDateTime.of(2024, 3, 15, 14, 30);
        LocalDateTime dt3 = LocalDateTime.parse("2024-03-15T14:30:00");

        // ---- 操作 ----
        LocalDate tomorrow = today.plusDays(1);
        LocalDate lastWeek = today.minusWeeks(1);
        LocalDate nextMonth = today.plusMonths(1);

        LocalDateTime dtPlus = dt.plusHours(2)
                                 .plusMinutes(30)
                                 .plusSeconds(15);

        // ---- 获取字段 ----
        int year = today.getYear();
        int month = today.getMonthValue();        // 1~12
        Month monthEnum = today.getMonth();       // Month.JANUARY
        int day = today.getDayOfMonth();
        DayOfWeek dow = today.getDayOfWeek();     // DayOfWeek.MONDAY
        int dayOfYear = today.getDayOfYear();

        System.out.println(today.isLeapYear());   // 是否闰年

        // ---- 比较 ----
        LocalDate d1 = LocalDate.of(2024, 1, 1);
        LocalDate d2 = LocalDate.of(2024, 12, 31);
        System.out.println(d1.isBefore(d2));      // true
        System.out.println(d1.isAfter(d2));       // false
        System.out.println(d1.isEqual(d2));       // false
        System.out.println(d1.compareTo(d2));     // 负数

        // ---- 计算间隔 ----
        long daysBetween = ChronoUnit.DAYS.between(d1, d2);      // 365
        long monthsBetween = ChronoUnit.MONTHS.between(d1, d2);  // 11
        long yearsBetween = ChronoUnit.YEARS.between(d1, d2);    // 0

        // ---- 格式化 ----
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatted = dt.format(formatter);                  // "2024-03-15 14:30:00"
        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        // 指定格式解析
        LocalDate parsed2 = LocalDate.parse("2024/03/15",
            DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        // ---- 时间调整器 TemporalAdjuster ----
        LocalDate firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate lastDayOfYear = today.with(TemporalAdjusters.lastDayOfYear());
    }
}
```

### 4.3 Duration 与 Period

```java
public class DurationPeriodDemo {
    public static void main(String[] args) {
        // Period：日期之间的差（年、月、日）
        Period period = Period.between(
            LocalDate.of(1990, 1, 1),
            LocalDate.of(2024, 3, 15)
        );
        System.out.println(period.getYears());    // 34
        System.out.println(period.getMonths());   // 2
        System.out.println(period.getDays());     // 14

        // Duration：时间之间的差（时、分、秒、纳秒）
        Duration duration = Duration.between(
            LocalTime.of(9, 0),
            LocalTime.of(17, 30)
        );
        System.out.println(duration.toHours());        // 8
        System.out.println(duration.toMinutes());      // 510
        System.out.println(duration.getSeconds());     // 30600

        // 创建 Duration
        Duration twoHours = Duration.ofHours(2);
        Duration tenMinutes = Duration.ofMinutes(10);
        Duration fiveSeconds = Duration.ofSeconds(5);
    }
}
```

### 4.4 ZonedDateTime 与时区

```java
public class ZoneDemo {
    public static void main(String[] args) {
        // 所有可用时区
        // ZoneId.getAvailableZoneIds().forEach(System.out::println);

        // 创建带时区的日期时间
        ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        System.out.println(zdt);  // "2024-03-15T14:30:00+08:00[Asia/Shanghai]"

        // 时区转换
        ZonedDateTime nyTime = zdt.withZoneSameInstant(ZoneId.of("America/New_York"));
        System.out.println(nyTime);

        // UTC 时间
        Instant instant = Instant.now();                 // UTC 时间戳
        System.out.println(instant);                     // "2024-03-15T06:30:00Z"
        System.out.println(instant.toEpochMilli());      // 时间戳毫秒数

        // Instant 与 Date 互转
        java.util.Date date = java.util.Date.from(instant);
        Instant back = date.toInstant();
    }
}
```

### 4.5 DateTimeFormatter 线程安全

```java
public class FormatterDemo {
    // 线程安全，可以声明为 static（SimpleDateFormat 不能！）
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String format(LocalDateTime dt) {
        return dt.format(FORMATTER);
    }

    public static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text, FORMATTER);
    }

    // 常用预定义格式
    // DateTimeFormatter.ISO_LOCAL_DATE       -> "2024-03-15"
    // DateTimeFormatter.ISO_LOCAL_TIME       -> "14:30:00"
    // DateTimeFormatter.ISO_LOCAL_DATE_TIME  -> "2024-03-15T14:30:00"
    // DateTimeFormatter.ISO_INSTANT          -> "2024-03-15T06:30:00Z"
}
```

---

## 5. 集合框架架构

### 5.1 集合体系总览

```
                    +----------------+
                    |   Iterable     |
                    +-------+--------+
                            |
                    +-------v--------+
                    |  Collection    |
                    +-------+--------+
                   /        |         \
                  /         |          \
         +------v--+  +----v---+  +----v------+
         |   List   |  |  Set   |  |  Queue    |
         +----------+  +--------+  +-----------+
            |  |           |            |
        ArrayList  LinkedList  HashSet   LinkedList
        Vector                 TreeSet   ArrayDeque
        Stack                  LinkedHashSet  PriorityQueue


        +-----------------+
        |     Map         |   (不是 Collection 的子接口)
        +-----------------+
            |       |
        HashMap   TreeMap
        LinkedHashMap
        Hashtable
        ConcurrentHashMap
```

### 5.2 Collection 接口 vs Map 接口

| 特性 | Collection | Map |
|------|-----------|-----|
| 存储类型 | 单个元素 | 键值对 (K-V) |
| 核心子接口 | List, Set, Queue | （无子接口） |
| 通用实现 | ArrayList, HashSet | HashMap, TreeMap |
| 遍历 | 增强 for, Iterator | keySet, entrySet, values |

### 5.3 集合核心接口对比

| 接口 | 有序性 | 允许重复 | 允许 null | 实现类 |
|------|--------|---------|-----------|--------|
| List | 有（按插入顺序） | 允许 | 允许 | ArrayList, LinkedList, Vector |
| Set | 无（部分有） | 不允许 | 允许（部分） | HashSet, TreeSet, LinkedHashSet |
| Queue | 有（FIFO/优先级） | 允许 | 不允许（部分） | LinkedList, ArrayDeque, PriorityQueue |
| Map | 无（部分有） | 键不重复 | 允许（部分） | HashMap, TreeMap, LinkedHashMap |

---

## 6. List 接口

### 6.1 ArrayList

```java
public class ArrayListDemo {
    public static void main(String[] args) {
        // 创建
        List<String> list = new ArrayList<>();           // 默认容量 10
        List<String> listWithSize = new ArrayList<>(100); // 指定初始容量
        List<String> fromCollection = new ArrayList<>(List.of("a", "b", "c"));

        // 增删改查
        list.add("Java");                                // 尾部添加
        list.add(0, "Python");                           // 指定位置插入
        list.addAll(List.of("Go", "Rust"));              // 批量添加
        list.set(1, "C++");                              // 替换指定位置元素
        list.remove(0);                                  // 按索引删除
        list.remove("Go");                               // 按对象删除
        String elem = list.get(0);                       // 按索引获取
        int index = list.indexOf("Java");                // 查找索引
        boolean has = list.contains("Java");             // 是否包含
        int size = list.size();                          // 大小
        boolean empty = list.isEmpty();                  // 是否为空

        // 遍历方式
        // 方式 1：增强 for
        for (String s : list) {
            System.out.println(s);
        }
        // 方式 2：Iterator
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
        // 方式 3：ListIterator（双向遍历）
        ListIterator<String> li = list.listIterator();
        while (li.hasNext()) { li.next(); }
        while (li.hasPrevious()) { System.out.println(li.previous()); }
        // 方式 4：Stream
        list.stream().filter(s -> s.startsWith("J")).forEach(System.out::println);

        // 子列表（视图，修改会影响原列表）
        List<String> sub = list.subList(0, 2);
        sub.set(0, "Modified");  // also changes list.get(0)

        // 转换为数组
        String[] arr = list.toArray(new String[0]);
    }
}
```

**ArrayList 扩容机制**:
```
初始容量 = 10（JDK 8+ 懒加载，第一次 add 时创建）
扩容因子 = 1.5 倍（newCapacity = oldCapacity + oldCapacity >> 1）
最大容量 = Integer.MAX_VALUE - 8

扩容时创建一个新数组，将旧数组复制过去（O(n)）
```

### 6.2 LinkedList

```java
public class LinkedListDemo {
    public static void main(String[] args) {
        // LinkedList 同时实现了 List 和 Deque
        LinkedList<String> list = new LinkedList<>();

        // List 方法
        list.add("a");
        list.addFirst("first");     // 头部插入
        list.addLast("last");       // 尾部插入
        String first = list.getFirst();
        String last = list.getLast();
        list.removeFirst();
        list.removeLast();

        // Deque 方法（双端队列）
        list.offer("x");            // 尾部添加（队列）
        list.push("y");             // 头部添加（栈）
        String pop = list.pop();    // 头部移除（栈）
        String peek = list.peek();  // 查看不移除
    }
}
```

### 6.3 ArrayList vs LinkedList

| 对比项 | ArrayList | LinkedList |
|--------|-----------|------------|
| 底层结构 | 动态数组（Object[]） | 双向链表（Node） |
| 随机访问 get(i) | O(1) | O(n) |
| 尾部插入 add(e) | O(1) 均摊 | O(1) |
| 指定位置插入 add(i, e) | O(n) 元素移动 | O(n) 遍历查找 |
| 头部插入 | O(n) | O(1) |
| 删除 | O(n) | O(n) |
| 内存消耗 | 较少（只需存储数据） | 较多（存储前驱/后继指针） |
| 适用场景 | 随机访问多，尾部增删多 | 头部增删多，频繁插入/删除 |

**选择建议**:
- 绝大多数场景使用 `ArrayList`
- 只有频繁在头部插入/删除时才用 `LinkedList`
- `LinkedList` 还可用于实现队列/栈（`Deque` 接口）

---

## 7. Set 接口

### 7.1 HashSet、LinkedHashSet、TreeSet 对比

| 特性 | HashSet | LinkedHashSet | TreeSet |
|------|---------|--------------|---------|
| 底层结构 | HashMap | LinkedHashMap | TreeMap（红黑树） |
| 顺序 | 无序 | 插入顺序 | 自然顺序/比较器顺序 |
| 时间复杂度 | O(1) | O(1) | O(log n) |
| null | 允许一个 null | 允许一个 null | 不允许 null（有比较器时可） |
| 比较方式 | hashCode + equals | hashCode + equals | compareTo / Comparator |
| 适用场景 | 通用去重 | 需要保持插入顺序 | 需要自动排序 |

```java
public class SetDemo {
    public static void main(String[] args) {
        // HashSet：基于哈希表，O(1) 操作
        Set<String> hashSet = new HashSet<>();
        hashSet.add("banana");
        hashSet.add("apple");
        hashSet.add("cherry");
        hashSet.add("banana");         // 重复，不会添加
        System.out.println(hashSet);   // 无序: [banana, apple, cherry]

        // LinkedHashSet：保持插入顺序
        Set<String> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add("banana");
        linkedHashSet.add("apple");
        linkedHashSet.add("cherry");
        System.out.println(linkedHashSet); // 有序: [banana, apple, cherry]

        // TreeSet：按自然顺序排序
        Set<String> treeSet = new TreeSet<>();
        treeSet.add("banana");
        treeSet.add("apple");
        treeSet.add("cherry");
        System.out.println(treeSet);       // 排序: [apple, banana, cherry]

        // 自定义排序
        Set<String> reverseSet = new TreeSet<>(Comparator.reverseOrder());
        reverseSet.add("banana");
        reverseSet.add("apple");
        reverseSet.add("cherry");
        System.out.println(reverseSet);    // [cherry, banana, apple]

        // Set 去重的条件：equals/hashCode 协定
        Set<Student> students = new HashSet<>();
        students.add(new Student(1L, "张三"));
        students.add(new Student(1L, "张三"));  // 需要正确实现 equals/hashCode
        System.out.println(students.size());     // 1（如果实现了 equals/hashCode）
    }
}
```

### 7.2 Comparable 与 Comparator

```java
// Comparable：自然排序，定义在类内部
public class Student implements Comparable<Student> {
    private Long id;
    private String name;
    private int score;

    // 构造、getter/setter 省略

    @Override
    public int compareTo(Student other) {
        // 先按分数降序，再按姓名升序
        int scoreCmp = Integer.compare(other.score, this.score);
        if (scoreCmp != 0) return scoreCmp;
        return this.name.compareTo(other.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        Student student = (Student) o;
        return Objects.equals(id, student.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

// Comparator：定制排序，独立于类
public class StudentComparators {
    public static final Comparator<Student> BY_NAME = Comparator.comparing(Student::getName);
    public static final Comparator<Student> BY_SCORE_DESC =
        Comparator.comparingInt(Student::getScore).reversed();
    public static final Comparator<Student> BY_NAME_THEN_SCORE =
        Comparator.comparing(Student::getName)
                  .thenComparingInt(Student::getScore);
}

// 使用
Set<Student> treeSet = new TreeSet<>(BY_SCORE_DESC);
List<Student> list = new ArrayList<>();
list.sort(BY_NAME);                    // List.sort
Collections.sort(list, BY_NAME_THEN_SCORE);
```

---

## 8. Map 接口

### 8.1 HashMap 核心原理

**数据结构**：数组 + 链表 + 红黑树（JDK 8+）

```
初始容量：16（必须是 2 的幂）
加载因子：0.75
扩容阈值：capacity * loadFactor = 16 * 0.75 = 12
树化阈值：链表长度 >= 8 且数组长度 >= 64 时，链表转为红黑树
反树化阈值：红黑树节点 <= 6 时，转为链表

哈希函数：h = key.hashCode() ^ (h >>> 16)    // 高 16 位参与运算
索引计算：i = (n - 1) & hash                  // 取模优化（要求容量为 2 的幂）
```

```java
public class HashMapDemo {
    public static void main(String[] args) {
        // 创建
        Map<String, Integer> map = new HashMap<>();             // 默认
        Map<String, Integer> map2 = new HashMap<>(64, 0.75f);   // 指定容量和加载因子

        // 增删改查
        map.put("Java", 100);
        map.put("Python", 95);
        map.put("Java", 98);                   // 覆盖旧值
        map.putIfAbsent("Go", 90);             // 不存在才插入
        map.putAll(Map.of("C++", 85, "Rust", 88));

        int val = map.get("Java");             // 98（不存在返回 null）
        int defaultVal = map.getOrDefault("Kotlin", 0);  // 0
        boolean exists = map.containsKey("Python");
        boolean valExists = map.containsValue(100);
        map.remove("Go");                      // 删除键
        map.remove("C++", 85);                 // 键值对同时匹配才删除
        map.replace("Java", 100);              // 替换（已存在才生效）

        int size = map.size();
        boolean empty = map.isEmpty();

        // 遍历方式
        // 方式 1：entrySet
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }
        // 方式 2：keySet + get
        for (String key : map.keySet()) {
            System.out.println(key + "=" + map.get(key));
        }
        // 方式 3：values（只遍历值）
        for (Integer value : map.values()) {
            System.out.println(value);
        }
        // 方式 4：forEach (JDK 8+)
        map.forEach((k, v) -> System.out.println(k + "=" + v));

        // JDK 8+ Map 增强方法
        map.compute("Java", (k, v) -> v == null ? 0 : v + 1);
        map.computeIfAbsent("Kotlin", k -> 90);
        map.computeIfPresent("Java", (k, v) -> v * 2);
        map.merge("Java", 10, Integer::sum);   // 存在则合并，不存在则插入

        // 不可变 Map
        Map<String, Integer> immutable = Map.of("a", 1, "b", 2);
        // immutable.put("c", 3);  // UnsupportedOperationException
    }
}
```

### 8.2 LinkedHashMap

```java
public class LinkedHashMapDemo {
    public static void main(String[] args) {
        // 保持插入顺序
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        System.out.println(map);  // {one=1, two=2, three=3}

        // 访问顺序（LRU Cache 基础）
        LinkedHashMap<String, Integer> lru = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return size() > 3;  // 超过 3 个就淘汰最久未访问的
            }
        };
        lru.put("a", 1);
        lru.put("b", 2);
        lru.put("c", 3);
        lru.get("a");               // 访问 a，a 变成最近使用的
        lru.put("d", 4);            // 触发淘汰，移除最久未访问的 "b"
        System.out.println(lru.keySet());  // [a, c, d]（b 被淘汰）
    }
}
```

### 8.3 TreeMap

```java
public class TreeMapDemo {
    public static void main(String[] args) {
        // 红黑树实现，键自动排序（O(log n)）
        TreeMap<String, Integer> map = new TreeMap<>();
        map.put("dog", 4);
        map.put("cat", 2);
        map.put("bird", 1);
        map.put("elephant", 8);

        // 排序后的遍历
        map.forEach((k, v) -> System.out.println(k + "=" + v));
        // bird=1, cat=2, dog=4, elephant=8

        // 导航方法
        System.out.println(map.firstKey());         // "bird"
        System.out.println(map.lastKey());          // "elephant"
        System.out.println(map.ceilingKey("d"));    // "dog"（>= "d" 的最小键）
        System.out.println(map.floorKey("e"));      // "elephant"（<= "e" 的最大键）
        System.out.println(map.lowerKey("dog"));    // "cat"（< "dog" 的最大键）
        System.out.println(map.higherKey("dog"));   // "elephant"（> "dog" 的最小键）

        // 子图
        SortedMap<String, Integer> sub = map.subMap("bird", "elephant"); // [bird, elephant)
        SortedMap<String, Integer> head = map.headMap("dog");            // < "dog"
        SortedMap<String, Integer> tail = map.tailMap("cat");            // >= "cat"
    }
}
```

### 8.4 HashMap（JDK 7 vs JDK 8）

| 特性 | JDK 7 | JDK 8+ |
|------|-------|--------|
| 数据结构 | 数组 + 链表 | 数组 + 链表 + 红黑树 |
| 节点插入 | 头插法 | 尾插法 |
| 扩容死链 | 有（头插法导致） | 无（尾插法） |
| 哈希函数 | 复杂（多次异或） | 简单（一次异或） |
| 树化 | 无 | 链表 >= 8 且数组 >= 64 转红黑树 |
| 性能 | 哈希冲突严重时 O(n) | 最坏 O(log n) |

---

## 9. Queue / Deque 接口

```java
public class QueueDemo {
    public static void main(String[] args) {
        // ---- Queue（队列，FIFO）----
        Queue<String> queue = new LinkedList<>();

        // 添加
        queue.offer("a");          // 添加成功返回 true，失败返回 false（容量限制）
        queue.add("b");            // 添加失败抛异常 IllegalStateException

        // 移除
        String head = queue.poll(); // 移除并返回头部，空队列返回 null
        // queue.remove();          // 移除并返回头部，空队列抛异常 NoSuchElementException

        // 查看不移除
        String peek = queue.peek(); // 查看头部，空队列返回 null
        // queue.element();         // 查看头部，空队列抛异常

        // ---- Deque（双端队列）----
        Deque<String> deque = new ArrayDeque<>();

        // 作为队列（FIFO）
        deque.offerLast("a");       // 尾部入队
        deque.offerLast("b");
        String first = deque.pollFirst();  // 头部出队 -> "a"

        // 作为栈（LIFO）
        deque.push("x");            // 头部压栈（等效 addFirst）
        deque.push("y");
        String top = deque.pop();   // 头部出栈（等效 removeFirst）
        String top2 = deque.peek(); // 查看栈顶（等效 peekFirst）

        // ---- ArrayDeque vs LinkedList ----
        // ArrayDeque：循环数组实现，无容量限制，比 LinkedList 快
        // LinkedList：双向链表实现，可作为队列/栈，但性能略差

        // ---- PriorityQueue（优先级队列）----
        Queue<Integer> pq = new PriorityQueue<>();  // 最小堆
        pq.offer(5);
        pq.offer(1);
        pq.offer(3);
        System.out.println(pq.poll());  // 1（最小优先）
        System.out.println(pq.poll());  // 3
        System.out.println(pq.poll());  // 5

        // 最大堆
        Queue<Integer> maxPq = new PriorityQueue<>(Comparator.reverseOrder());
        maxPq.offer(5);
        maxPq.offer(1);
        maxPq.offer(3);
        System.out.println(maxPq.poll());  // 5（最大优先）
    }
}
```

---

## 10. Collections 工具类

```java
public class CollectionsDemo {
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6));

        // 排序
        Collections.sort(list);                          // 自然排序
        Collections.sort(list, Comparator.reverseOrder()); // 逆序
        Collections.shuffle(list);                       // 随机打乱
        Collections.reverse(list);                       // 反转

        // 查找
        Collections.sort(list);
        int index = Collections.binarySearch(list, 5);   // 二分查找（必须先排序）

        // 极值
        int max = Collections.max(list);
        int min = Collections.min(list);

        // 批量操作
        Collections.fill(list, 0);                       // 全部填充为 0
        Collections.addAll(list, 7, 8, 9);                // 批量添加
        Collections.replaceAll(list, 3, 33);              // 替换
        Collections.rotate(list, 2);                     // 旋转

        // 不可变集合
        List<Integer> unmodifiable = Collections.unmodifiableList(list);
        // unmodifiable.add(10);  // UnsupportedOperationException

        Set<Integer> singleton = Collections.singleton(1);  // 单元素集合
        List<Integer> emptyList = Collections.emptyList();  // 空列表

        // 同步包装器（线程安全，性能不如 ConcurrentHashMap）
        Collection<String> syncCollection = Collections.synchronizedCollection(new ArrayList<>());
        Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());

        // 检查型包装器（类型安全检查）
        List<String> safeList = Collections.checkedList(new ArrayList<>(), String.class);
        // safeList.add(123);  // ClassCastException（但只能在运行时检查）
    }
}
```

---

## 11. Stream API（JDK 8+）

### 11.1 Stream 创建

```java
public class StreamCreateDemo {
    public static void main(String[] args) {
        // 从集合创建
        List<String> list = List.of("a", "b", "c");
        Stream<String> streamFromList = list.stream();
        Stream<String> parallelStream = list.parallelStream();

        // 从数组创建
        int[] arr = {1, 2, 3};
        IntStream intStream = Arrays.stream(arr);

        // 从值创建
        Stream<String> ofStream = Stream.of("a", "b", "c");

        // 无限流
        Stream<Integer> iterate = Stream.iterate(0, n -> n + 1);                // 0, 1, 2, ...
        Stream<Double> generate = Stream.generate(Math::random);                // 随机数
        Stream<Integer> iterateWithLimit = Stream.iterate(0, n -> n < 100, n -> n + 1); // JDK 9+

        // 从其他
        IntStream range = IntStream.range(0, 10);      // [0, 10) 0~9
        IntStream rangeClosed = IntStream.rangeClosed(0, 10);  // [0, 10] 0~10
        Stream<String> lines = Files.lines(Paths.get("file.txt"));  // 文件行
        Stream<String> splitStream = Pattern.compile(",").splitAsStream("a,b,c");
    }
}
```

### 11.2 中间操作

```java
public class StreamIntermediateDemo {
    public static void main(String[] args) {
        List<String> words = List.of("Java", "Python", "JavaScript", "Go", "Rust", "Java");

        // filter：过滤
        words.stream()
            .filter(s -> s.length() > 3)
            .forEach(System.out::println);  // Java, Python, JavaScript, Rust

        // map：映射
        words.stream()
            .map(String::toUpperCase)
            .forEach(System.out::println);  // JAVA, PYTHON, JAVASCRIPT, GO, RUST, JAVA

        // flatMap：扁平化
        List<List<String>> nested = List.of(
            List.of("a", "b"), List.of("c", "d")
        );
        nested.stream()
            .flatMap(Collection::stream)
            .forEach(System.out::println);  // a, b, c, d

        // distinct：去重
        words.stream()
            .distinct()
            .forEach(System.out::println);  // Java, Python, JavaScript, Go, Rust

        // sorted：排序
        words.stream()
            .sorted()
            .forEach(System.out::println);  // Go, Java, Java, JavaScript, Python, Rust

        // limit / skip：截取 / 跳过
        words.stream()
            .sorted()
            .limit(3)
            .skip(1)
            .forEach(System.out::println);  // Java, Java

        // peek：调试（中间操作，不是终端操作！）
        long count = words.stream()
            .peek(s -> System.out.println("Processing: " + s))
            .filter(s -> s.length() > 3)
            .count();  // 如果没有终端操作，peek 不会执行
    }
}
```

### 11.3 终端操作

```java
public class StreamTerminalDemo {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<String> words = List.of("Java", "Python", "JavaScript", "Go", "Rust");

        // forEach：遍历
        words.stream().forEach(System.out::println);

        // collect：收集
        List<String> longWords = words.stream()
            .filter(s -> s.length() > 3)
            .collect(Collectors.toList());

        Set<String> set = words.stream().collect(Collectors.toSet());

        String joined = words.stream()
            .collect(Collectors.joining(", ", "[", "]"));
        System.out.println(joined);  // [Java, Python, JavaScript, Go, Rust]

        Map<Integer, List<String>> grouped = words.stream()
            .collect(Collectors.groupingBy(String::length));
        System.out.println(grouped);  // {2=[Go], 4=[Java, Rust], 6=[Python], 10=[JavaScript]}

        // reduce：归约
        int sum = numbers.stream().reduce(0, Integer::sum);    // 55
        int product = numbers.stream().reduce(1, (a, b) -> a * b); // 3628800
        Optional<Integer> max = numbers.stream().reduce(Integer::max);

        // count / min / max
        long count = numbers.stream().count();
        Optional<Integer> minV = numbers.stream().min(Integer::compareTo);
        Optional<Integer> maxV = numbers.stream().max(Integer::compareTo);

        // anyMatch / allMatch / noneMatch
        boolean hasEven = numbers.stream().anyMatch(n -> n % 2 == 0);   // true
        boolean allPositive = numbers.stream().allMatch(n -> n > 0);    // true
        boolean noneNegative = numbers.stream().noneMatch(n -> n < 0);  // true

        // findFirst / findAny
        Optional<Integer> first = numbers.stream().filter(n -> n > 5).findFirst(); // 6
        Optional<Integer> any = numbers.parallelStream().filter(n -> n > 5).findAny();

        // toArray
        String[] arr = words.stream().toArray(String[]::new);
    }
}
```

### 11.4 并行流

```java
public class ParallelStreamDemo {
    public static void main(String[] args) {
        // 自动并行处理（使用 ForkJoinPool.commonPool()）
        long sum = LongStream.rangeClosed(1, 10_000_000)
            .parallel()              // 转为并行流
            .sum();

        // 并行流的注意事项
        List<Integer> list = new ArrayList<>();
        // ❌ 错误：并行 + 非线程安全的集合
        IntStream.range(0, 1000).parallel()
            .forEach(list::add);      // list 不是线程安全的！
        System.out.println(list.size());  // 可能 < 1000

        // ✅ 正确：使用线程安全的集合或 reduce
        List<Integer> safeList = IntStream.range(0, 1000).parallel()
            .boxed()
            .collect(Collectors.toList());
        System.out.println(safeList.size());  // 1000

        // 并行流适用的场景：CPU 密集型、大量数据、无共享状态
        // 不适用的场景：IO 密集型、数据量很小、有状态操作
    }
}
```

### 11.5 复杂 Stream 示例

```java
public class StreamAdvancedDemo {
    public static void main(String[] args) {
        // 示例：交易统计
        List<Transaction> transactions = List.of(
            new Transaction("A001", "USD", 1000),
            new Transaction("A002", "CNY", 5000),
            new Transaction("A003", "USD", 2000),
            new Transaction("A004", "JPY", 10000),
            new Transaction("A005", "USD", 300)
        );

        // 统计：按货币分组，计算每组的交易总额，按总额降序
        Map<String, DoubleSummaryStatistics> stats = transactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::currency,
                Collectors.summarizingDouble(Transaction::amount)
            ));

        // 找出美元交易中金额大于 500 的，按金额降序取前 3
        List<Transaction> topUsd = transactions.stream()
            .filter(t -> "USD".equals(t.currency()))
            .filter(t -> t.amount() > 500)
            .sorted(Comparator.comparingDouble(Transaction::amount).reversed())
            .limit(3)
            .collect(Collectors.toList());

        // 计算总金额
        double totalAmount = transactions.stream()
            .mapToDouble(Transaction::amount)
            .sum();

        // 分区：大额交易（>= 1000）和小额交易
        Map<Boolean, List<Transaction>> partitioned = transactions.stream()
            .collect(Collectors.partitioningBy(t -> t.amount() >= 1000));
    }
}

record Transaction(String id, String currency, double amount) {}
```

---

## 12. Optional 类（JDK 8+）

### 12.1 为什么需要 Optional

避免 `NullPointerException`，明确表示"值可能缺失"的语义。

```java
public class OptionalDemo {
    public static void main(String[] args) {
        // ---- 创建 Optional ----
        Optional<String> empty = Optional.empty();               // 空 Optional
        Optional<String> nonNull = Optional.of("Hello");         // 非空，传 null 抛 NPE
        Optional<String> nullable = Optional.ofNullable(null);   // 可为 null

        // ---- 判空 ----
        System.out.println(nonNull.isPresent());  // true
        System.out.println(nullable.isPresent()); // false
        // JDK 11+
        System.out.println(nonNull.isEmpty());    // false

        // ---- 获取值 ----
        String val1 = nonNull.get();                      // "Hello"，为空抛 NoSuchElementException
        String val2 = nonNull.orElse("Default");          // "Hello"
        String val3 = nullable.orElse("Default");         // "Default"
        String val4 = nullable.orElseGet(() -> computeDefault());  // 懒加载
        String val5 = nullable.orElseThrow();             // JDK 10+，空则抛 NoSuchElementException
        String val6 = nullable.orElseThrow(() -> new RuntimeException("Value missing"));

        // ---- 消费 ----
        nonNull.ifPresent(s -> System.out.println(s));    // 存在时执行
        nullable.ifPresentOrElse(
            s -> System.out.println(s),                   // 存在时
            () -> System.out.println("Missing")           // 不存在时（JDK 9+）
        );

        // ---- 转换 ----
        Optional<Integer> length = nonNull.map(String::length);        // Optional[5]
        Optional<String> upper = nonNull.filter(s -> s.length() > 3)
                                        .map(String::toUpperCase);
        // flatMap：避免 Optional<Optional<...>> 嵌套
        Optional<String> result = nonNull.flatMap(s -> getOptionalString(s));

        // ---- Stream 结合 ----
        List<Optional<String>> optionals = List.of(
            Optional.of("a"), Optional.empty(), Optional.of("b")
        );
        List<String> values = optionals.stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        // JDK 9+ 更简洁
        List<String> values2 = optionals.stream()
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    private static String computeDefault() {
        return "Default Value";
    }

    private static Optional<String> getOptionalString(String input) {
        return Optional.of(input + " processed");
    }
}
```

### 12.2 Optional 使用规范

```java
// 正确使用 Optional

// ✅ 作为返回值：明确表示可能没有结果
public Optional<Customer> findById(Long id) {
    // ...
    return Optional.ofNullable(customer);
}

// ❌ 不要作为字段
public class Order {
    // private Optional<String> note;  // 不推荐，Optional 不可序列化
    private String note;  // null 表示没有备注
}

// ❌ 不要作为方法参数
// public void process(Optional<String> opt) { }  // 不推荐

// ❌ 不要作为集合元素（除非有特殊理由）
// List<Optional<String>>  // 不推荐

// ❌ 不要用于基本类型包装
// Optional<Integer>  // 用 OptionalInt、OptionalLong、OptionalDouble
```

---

## 13. 复杂度速查表

| 操作 | ArrayList | LinkedList | HashSet | TreeSet | HashMap | TreeMap | ArrayDeque | PriorityQueue |
|------|-----------|------------|---------|---------|---------|---------|-----------|--------------|
| get(i) | O(1) | O(n) | - | - | - | - | O(1) | - |
| add(e) 尾部 | O(1) 均摊 | O(1) | O(1) | O(log n) | O(1) | O(log n) | O(1) | O(log n) |
| add(i, e) | O(n) | O(n) | - | - | - | - | - | - |
| remove(e) | O(n) | O(n) | O(1) | O(log n) | O(1) | O(log n) | O(n) | O(n) |
| contains(e) | O(n) | O(n) | O(1) | O(log n) | O(1) | O(log n) | O(n) | O(n) |
| put/get(key) | - | - | - | - | O(1) | O(log n) | - | - |

---

## 14. 常见面试题

1. **ArrayList 和 LinkedList 的区别及适用场景？**
2. **HashMap 的工作原理？JDK 8 中做了哪些优化？**
   - 数组 + 链表 + 红黑树
   - 哈希函数：高 16 位参与运算
   - 扩容机制：2 倍扩容
   - 树化条件：链表 >= 8 且数组 >= 64

3. **HashMap 和 Hashtable / ConcurrentHashMap 的区别？**
   - Hashtable：全表锁，线程安全但性能差
   - ConcurrentHashMap：分段锁/桶锁，高性能并发
   - HashMap：非线程安全

4. **HashSet 如何保证元素不重复？**
   - 底层是 HashMap，元素作为 key，用 PRESENT 常量作为 value
   - 通过 hashCode() 和 equals() 判断重复

5. **Comparable 和 Comparator 的区别？**
   - Comparable：自然排序，`compareTo(T o)`，内部比较器
   - Comparator：定制排序，`compare(T o1, T o2)`，外部比较器

6. **String 的不可变性有什么好处？**
   - 字符串池、安全性（类加载、网络连接参数）、线程安全、hashCode 缓存

7. **为什么重写 equals 必须重写 hashCode？**
   - 违反协定：`a.equals(b) -> a.hashCode() == b.hashCode()`

8. **Integer 的缓存范围是多少？如何调整？**
   - -128~127，通过 `-XX:AutoBoxCacheMax=size` 调整

9. **Stream 和集合的区别？**
   - 集合存储数据，Stream 计算数据
   - Stream 不修改源数据
   - Stream 是惰性的（中间操作延迟执行）
   - Stream 只能消费一次

10. **Optional 的正确使用方式？**
    - 用于返回值，不用于字段/参数/集合元素
    - 用 orElse/orElseGet 替代 get()

---

## 15. 练习建议

### 入门练习
1. 使用 String/StringBuilder 实现字符串反转、回文判断
2. 使用集合实现学生成绩统计（List + Map）
3. 使用 HashSet 对大量随机数去重

### 进阶练习
4. 实现 LRU 缓存（使用 LinkedHashMap）
5. 使用 Stream API 处理日志文件：筛选、分组、聚合
6. 实现一个简单的单词统计工具（使用 Map + Stream）

### 综合练习
7. 实现员工管理系统（CRUD + 按条件查询 + 排序）
8. 分析 Java 源码：自行阅读 `HashMap.put()` 和 `ArrayList.grow()` 源码
9. 实现自定义集合框架（如固定容量的 ArrayList）

---

**下一步**: 学完集合与 API 后，进入 [异常机制](./04-异常机制.md)，学习 Java 的错误处理体系。
