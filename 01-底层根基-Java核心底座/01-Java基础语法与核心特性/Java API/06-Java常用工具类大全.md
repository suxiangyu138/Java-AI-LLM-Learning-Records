# 06 Java 常用工具类大全

> 学会"站在 JDK 的肩膀上"——Arrays、Collections、Objects 这些工具类每多用一个方法，就少写一段需要测试和维护的手写代码

---

## 📚 目录

1. [Objects 工具类](#1-objects-工具类)
2. [Arrays 工具类](#2-arrays-工具类)
3. [Collections 工具类](#3-collections-工具类)
4. [Comparator 进阶](#4-comparator-进阶)
5. [Random 与 ThreadLocalRandom](#5-random-与-threadlocalrandom)
6. [Scanner 与输入解析](#6-scanner-与输入解析)
7. [Properties 与配置](#7-properties-与配置)
8. [综合速查表](#8-综合速查表)
9. [练习](#9-练习)

---

## 1. Objects 工具类

**定位**：`java.util.Objects`（Java 7 引入，Java 9/17 持续增强）—— 提供 null 安全、异常预防、边界检查的静态工具方法。**能用 Objects 的地方就不要手写 null 判断**。

### 1.1 equals —— null 安全比较

> 最常用的方法：`Objects.equals(a, b)` 等价于 `a == b` 或 `a != null && a.equals(b)`，当 a 或 b 为 null 时不会抛 NPE。

```java
// 传统写法 —— 每次都要写
boolean eq = a != null && a.equals(b);

// Objects 写法 —— 一行搞定，null 安全
boolean eq = Objects.equals(a, b);   // 两个都为 null → true；一个为 null → false
```

### 1.2 deepEquals —— 数组深层比较

> `Objects.deepEquals(a, b)` —— 递归比较嵌套数组内容，等价于 `Arrays.deepEquals`。

```java
int[] a1 = {1, 2, 3};
int[] a2 = {1, 2, 3};
int[] a3 = {1, 2, 4};

Objects.equals(a1, a2);       // false —— equals 比较的是数组引用，不是内容
Objects.deepEquals(a1, a2);   // true  —— 递归比较每个元素
Objects.deepEquals(a1, a3);   // false

int[][] nested1 = {{1}, {2}};
int[][] nested2 = {{1}, {2}};
Objects.deepEquals(nested1, nested2);  // true —— 多层嵌套也能正确比较
```

> 🎯 **核心要点**：比较数组（尤其是多维数组）内容时，务必用 `deepEquals` 而非 `equals`。`equals` 对数组只比较引用地址。

### 1.3 hash / hashCode —— null 安全哈希

```java
// 计算单个对象的哈希码，null → 0
int h1 = Objects.hashCode(null);    // 0
int h2 = Objects.hashCode("abc");   // 与 "abc".hashCode() 相同

// 计算多个值的组合哈希码（等效于 Arrays.hashCode(values)）
int h3 = Objects.hash(name, age, salary);
// 等价于：
int h4 = Objects.hash(name) * 31 + Objects.hash(age) * 31 + Objects.hash(salary);
```

> 💡 **常见用法**：在自定义类的 `hashCode()` 中，用 `Objects.hash(field1, field2, field3)` 一行生成合规的哈希码，无需手动乘以 31。

### 1.4 requireNonNull —— 空检查，支持错误消息 / Supplier 延迟构造

| 重载 | 说明 | 适用场景 |
|------|------|---------|
| `requireNonNull(T obj)` | 为空抛 NPE，异常消息固定 | 快速断言 |
| `requireNonNull(T obj, String msg)` | 抛 NPE + 自定义消息 | 生产代码 |
| `requireNonNull(T obj, Supplier<String> msgSupplier)` | 异常消息延迟构造（性能优化） | 消息构造代价高的场景 |

```java
// 方法入口校验 —— 取代手写 if (x == null) throw ...
public void setConfig(Config config) {
    this.config = Objects.requireNonNull(config, "config must not be null");
}

// Supplier 延迟构造 —— 仅抛异常时才构造消息字符串
public void process(Data data) {
    this.data = Objects.requireNonNull(data, () -> "data is null at " + LocalDateTime.now());
}

// != 的区别：Objects.requireNonNull 返回非空对象，支持链式赋值
String name = Objects.requireNonNull(user.getName());    // 返回 name（非 null），否则抛 NPE
```

### 1.5 checkIndex / checkFromToIndex / checkFromIndexSize（Java 9+）

> 安全的数组 / 集合索引边界检查，取代手写 `if (index < 0 || index >= length) throw ...`。

```java
String[] arr = {"a", "b", "c"};

// 检查单索引：0 ≤ index < length
Objects.checkIndex(1, arr.length);   // 返回 1
Objects.checkIndex(5, arr.length);   // 抛 IndexOutOfBoundsException

// 检查范围 [fromIndex, toIndex)：0 ≤ fromIndex ≤ toIndex ≤ length
Objects.checkFromToIndex(0, 3, arr.length);   // 返回 0

// 检查 [fromIndex, fromIndex + size)：常用在批量读取
Objects.checkFromIndexSize(1, 2, arr.length);  // 返回 1 （1+2=3 ≤ 3）
Objects.checkFromIndexSize(1, 3, arr.length);  // 抛 IOOBE （1+3=4 > 3）
```

> 💡 `RandomAccess` 场景下，`checkIndex` 配合 `subList` 实现更优雅。

### 1.6 requireNonNullElse / requireNonNullElseGet（Java 9+）

> **当对象为 null 时提供默认值**，比三目运算符 `x != null ? x : default` 更简洁。

```java
// requireNonNullElse —— 直接提供默认对象
String name = Objects.requireNonNullElse(input, "defaultName");

// requireNonNullElseGet —— Supplier 延迟构造默认值（类似 orElseGet）
String name = Objects.requireNonNullElseGet(input, () -> loadDefaultName());

// 三目运算符的等价写法：
String name = (input != null) ? input : "defaultName";
// ✅ Objects 写法更语义化，"如果为 null 就用这个默认值"
```

> 🎯 **注意**：`requireNonNullElse` 的参数（默认值）会立即求值；如果默认值构造代价大，用 `requireNonNullElseGet` + Supplier。

### 1.7 toString —— 以及 Java 17+ toIdentityString

```java
// toString —— null 安全转字符串
Objects.toString(obj);                // obj == null → "null" 字符串
Objects.toString(obj, "default");     // obj == null → "default"

// Java 17+ —— toIdentityString：返回 "类名@哈希"（等同于 Object.toString 的默认实现）
String idStr = Objects.toIdentityString(obj);
// 输出类似：com.example.User@1a2b3c4d
```

> 💡 `Objects.toString(obj, "N/A")` 是打印日志时最常用的防御写法。

---

## 2. Arrays 工具类

**定位**：`java.util.Arrays` —— 数组操作的瑞士军刀：排序、查找、拷贝、填充、比较、流式转换。

### 2.1 sort —— 排序（Dual-Pivot QuickSort + TimSort + 并行排序）

```java
int[] arr = {5, 3, 1, 4, 2};

// 全排序 —— 基础类型用 Dual-Pivot QuickSort（O(n log n)）
Arrays.sort(arr);                           // → [1, 2, 3, 4, 5]

// 范围排序 [fromIndex, toIndex)
Arrays.sort(arr, 1, 4);                     // 只排序索引 1~3

// 对象排序 —— 用 TimSort（稳定排序）
String[] names = {"Bob", "Alice", "Eve"};
Arrays.sort(names);                         // → ["Alice", "Bob", "Eve"]

// 自定义 Comparator
Arrays.sort(names, Comparator.comparingInt(String::length));

// 并行排序 —— 多线程分治，大数据集优势明显（底层 ForkJoinPool）
int[] bigArr = new int[10_000_000];
// ... 填充数据
Arrays.parallelSort(bigArr);               // 充分利用多核 CPU
```

> 🎯 **选型建议**：数据量 < 10 万用 `sort`，更大用 `parallelSort`。对象排序默认用 TimSort（稳定、自适应），基础类型用 Dual-Pivot QuickSort（更快、不稳定）。

### 2.2 binarySearch —— 二分查找（前提：已排序数组）

```java
int[] sorted = {1, 3, 5, 7, 9};

int idx1 = Arrays.binarySearch(sorted, 5);      // → 2 （找到）
int idx2 = Arrays.binarySearch(sorted, 4);      // → -3 （没找到，插入点为索引 2，公式：-(insertionPoint) - 1）
int idx3 = Arrays.binarySearch(sorted, 1, 4, 7); // 范围查找 [1, 4)

// 对象数组 + Comparator
String[] names = {"Alice", "Bob", "Eve"};
int i = Arrays.binarySearch(names, "Bob", String::compareToIgnoreCase);
```

> ⚠️ **未排序数组上调用 binarySearch 结果是未定义的**。一定要先 `sort` 再 `binarySearch`。

### 2.3 copyOf / copyOfRange —— 数组拷贝

> 底层调用 `System.arraycopy`（native 方法，C 语言内存拷贝，性能极高）。

```java
int[] src = {1, 2, 3, 4, 5};

// 拷贝全部 —— 支持指定新长度
int[] copy1 = Arrays.copyOf(src, src.length);     // → [1, 2, 3, 4, 5]
int[] copy2 = Arrays.copyOf(src, 3);              // → [1, 2, 3] （截断）
int[] copy3 = Arrays.copyOf(src, 7);              // → [1, 2, 3, 4, 5, 0, 0] （填充默认值）

// 拷贝范围 [fromIndex, toIndex)
int[] copy4 = Arrays.copyOfRange(src, 1, 4);      // → [2, 3, 4]

// 二维数组浅拷贝 —— .clone() = copyOf 的简写
int[][] matrix = {{1}, {2}, {3}};
int[][] shallow = matrix.clone();                 // 行引用共享，修改内层会影响原数组
```

> 🎯 **性能对比**：`Arrays.copyOf` ≈ `System.arraycopy` (native) >> 手写 for 循环。数组扩容场景（如 ArrayList 内部）正是用 `Arrays.copyOf`。

### 2.4 equals / deepEquals / compare / mismatch（Java 9+）

```java
int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
int[] c = {1, 2, 4};

// equals —— 逐个元素比较（一对空数组相等）
Arrays.equals(a, b);        // true
Arrays.equals(a, c);        // false
Arrays.equals(null, null);  // true

// 范围 equals [aFrom, aTo) vs [bFrom, bTo)
Arrays.equals(a, 0, 2, c, 0, 2);   // true —— 只比较前两个元素

// deepEquals —— 多维数组深层比较
int[][] deep1 = {{1, 2}, {3, 4}};
int[][] deep2 = {{1, 2}, {3, 4}};
Arrays.deepEquals(deep1, deep2);   // true

// compare —— 字典序比较（Java 9+），替代手写循环比较
// 返回 0（相等）、负数（a < b）、正数（a > b）
int cmp = Arrays.compare(a, b);    // 0
int cmp2 = Arrays.compare(a, c);   // -1

// mismatch —— 返回第一个不同元素的索引（Java 9+）
int idx = Arrays.mismatch(a, c);   // 2 （索引 2 处不同）
```

### 2.5 fill / setAll —— 批量设置

```java
int[] arr = new int[5];

// fill —— 统一填充一个值
Arrays.fill(arr, 42);             // → [42, 42, 42, 42, 42]

// fill 范围
Arrays.fill(arr, 1, 4, 99);      // → [42, 99, 99, 99, 42]

// setAll —— 根据索引函数逐个计算（Java 8+）
Arrays.setAll(arr, i -> i * 10);  // → [0, 10, 20, 30, 40]

// parallelSetAll —— 并行版，大数据量更快
Arrays.parallelSetAll(arr, i -> i * 10);
```

### 2.6 asList —— 数组 ↔ 固定大小 List 视图

```java
String[] arr = {"A", "B", "C"};

// 返回 java.util.Arrays 内部 ArrayList（固定大小，不是 java.util.ArrayList）
List<String> list = Arrays.asList(arr);

list.set(1, "X");        // ✅ 可以修改元素
list.add("D");           // ❌ 抛 UnsupportedOperationException
list.remove(0);          // ❌ 抛 UnsupportedOperationException

// 💡 如果需要可变 List：
List<String> mutable = new ArrayList<>(Arrays.asList(arr));

// 💡 Java 9+ 创建不可变小列表：List.of() 更优
List<String> imm = List.of("A", "B", "C");
```

> ⚠️ **高频坑**：`Arrays.asList(arr)` 返回的 List 是数组的视图，修改数组会反映到 List，反之亦然。不能 add/remove。

### 2.7 stream —— 数组 → Stream

```java
int[] nums = {1, 2, 3, 4, 5};

// 基础类型数组 → XxxStream
IntStream intStream = Arrays.stream(nums);               // IntStream
LongStream longStream = Arrays.stream(longArr);
DoubleStream doubleStream = Arrays.stream(doubleArr);

// 范围流
IntStream rangeStream = Arrays.stream(nums, 1, 4);      // → [2, 3, 4]

// 对象数组 → Stream<T>
String[] strs = {"a", "b", "c"};
Stream<String> stream = Arrays.stream(strs);

// 配合 collect 一行完成数组 → List（可变）
List<String> list = Arrays.stream(strs)
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

### 2.8 toString / deepToString —— 打印数组一行搞定

```java
int[] arr = {1, 2, 3};
System.out.println(arr);                    // ❌ [I@1a2b3c4d （堆地址，毫无意义）
System.out.println(Arrays.toString(arr));   // ✅ [1, 2, 3]

int[][] matrix = {{1, 2}, {3, 4}};
System.out.println(Arrays.deepToString(matrix));  // ✅ [[1, 2], [3, 4]]
```

> 💡 **Debug 常用**：`Arrays.toString()` 和 `Arrays.deepToString()` 是调试数组最快的方式，无需自己写循环拼接。

### 2.9 spliterator —— 数组分割迭代器

```java
int[] arr = {1, 2, 3, 4, 5, 6, 7, 8};

// 获取数组的分割迭代器，用于并行流/自定义并行任务
Spliterator.OfInt spliterator = Arrays.spliterator(arr);

// 支持指定范围
Spliterator.OfInt rangeSpliterator = Arrays.spliterator(arr, 2, 6);
```

> 💡 `Spliterator` 是并行流的基础设施，日常编码中直接使用场景较少，但理解其存在有助于排查并行流性能问题。

---

## 3. Collections 工具类

**定位**：`java.util.Collections` —— 操作 Collection 和 Map 的静态方法工厂，提供排序、查找、同步包装、不可变包装等功能。

### 3.1 sort —— 排序（List 专用）

```java
List<String> list = new ArrayList<>(Arrays.asList("Bob", "Alice", "Eve"));

// sort —— 默认自然序（元素需实现 Comparable）
Collections.sort(list);                         // → [Alice, Bob, Eve]

// sort + Comparator
Collections.sort(list, Comparator.comparingInt(String::length));

// 💡 Java 8+ 更推荐的写法 —— 直接在 List 上调用
list.sort(Comparator.naturalOrder());            // 等价
```

> 🎯 **内部实现**：`Collections.sort` 底层调用 `List.sort`，JDK 8+ 中用的是 `TimSort`（稳定排序，O(n log n)）。

### 3.2 binarySearch —— 二分查找

```java
// 前提：List 已按自然序排序
List<String> sorted = Arrays.asList("Alice", "Bob", "Eve");
int idx = Collections.binarySearch(sorted, "Bob");        // → 1
int idx2 = Collections.binarySearch(sorted, "Dave");      // → -4 （未找到）

// 自定义 Comparator
int idx3 = Collections.binarySearch(sorted, "bob",
    String.CASE_INSENSITIVE_ORDER);                       // → 1
```

### 3.3 reverse / shuffle / rotate / swap —— 原地修改

```java
List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

// reverse —— 反转
Collections.reverse(list);          // → [5, 4, 3, 2, 1]

// shuffle —— 随机打乱（Fisher-Yates 算法）
Collections.shuffle(list);          // 每次结果不同
Collections.shuffle(list, new Random(42));  // 指定 Random，可复现

// rotate —— 旋转：将索引 distance 处的元素移到开头
Collections.rotate(list, 2);        // → [4, 5, 1, 2, 3] （向右旋转 2 步）

// swap —— 交换两个位置
Collections.swap(list, 0, 2);       // → [3, 2, 1, 4, 5]
```

> 💡 `rotate` 常用于轮播、队列轮转场景；`shuffle` 用于洗牌、抽奖。

### 3.4 min / max —— 找极值

```java
List<Integer> nums = Arrays.asList(3, 1, 4, 1, 5, 9);

int min = Collections.min(nums);                    // 1
int max = Collections.max(nums);                    // 9

// 自定义 Comparator
String longest = Collections.max(
    Arrays.asList("a", "bb", "ccc"),
    Comparator.comparingInt(String::length)
);                                                  // → "ccc"
```

### 3.5 frequency / disjoint —— 统计与集合关系

```java
List<String> list = Arrays.asList("A", "B", "A", "C", "A");

// frequency —— 元素出现次数
int cnt = Collections.frequency(list, "A");        // 3
int cnt2 = Collections.frequency(list, "D");       // 0

// disjoint —— 判断两个集合是否有交集（无交集 → true）
List<String> c1 = Arrays.asList("A", "B");
List<String> c2 = Arrays.asList("C", "D");
List<String> c3 = Arrays.asList("B", "E");

Collections.disjoint(c1, c2);   // true  （无交集）
Collections.disjoint(c1, c3);   // false （有交集 "B"）
```

### 3.6 fill / copy / replaceAll

```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));

// fill —— 全部替换为同一个值
Collections.fill(list, "X");                        // → [X, X, X]

// copy —— 将 src 拷贝到 dest（dest 长度必须 ≥ src）
List<String> dest = new ArrayList<>(Arrays.asList("", "", ""));
Collections.copy(dest, Arrays.asList("1", "2", "3")); // dest → [1, 2, 3]

// replaceAll —— 替换所有匹配的元素
List<String> items = new ArrayList<>(Arrays.asList("old", "new", "old"));
Collections.replaceAll(items, "old", "replaced");    // → [replaced, new, replaced]
```

### 3.7 unmodifiable* —— 不可变包装

```java
List<String> mutable = new ArrayList<>(Arrays.asList("A", "B", "C"));

// 包装为不可变视图
List<String> unmod = Collections.unmodifiableList(mutable);
unmod.add("D");          // ❌ UnsupportedOperationException
unmod.set(0, "X");       // ❌ UnsupportedOperationException

// ❌ 但 mutable 的修改仍然会反映到 unmod！
mutable.set(0, "Changed");
System.out.println(unmod.get(0));  // → "Changed" （"不可变"视图被穿透）

// ✅ 真正不可变：先 copy 再包装
List<String> trulyImmutable =
    Collections.unmodifiableList(new ArrayList<>(mutable));
```

> ⚠️ **安全漏洞**：`Collections.unmodifiableList(list)` 只阻止通过返回的引用来修改，原始 list 的修改仍会穿透。需要先复制再包装。

> 🎯 **Java 9+ 更优选择**：`List.of("A", "B", "C")`、`Set.of("A")`、`Map.of("k1", "v1", "k2", "v2")`，它们直接返回不可变集合，不存在穿透问题。

### 3.8 synchronized* —— 同步包装

```java
List<String> raw = new ArrayList<>();

// 包装为线程安全的 List（每个方法都加 synchronized）
List<String> syncList = Collections.synchronizedList(raw);

// ⚠️ 迭代仍需外部同步
synchronized (syncList) {
    Iterator<String> it = syncList.iterator();
    while (it.hasNext()) {
        System.out.println(it.next());
    }
}
```

> 🎯 **性能警告**：`synchronized*` 包装使用粗粒度锁，高并发下性能极差。**首选** `CopyOnWriteArrayList`、`ConcurrentHashMap` 等 JUC 容器。

### 3.9 checked* —— 类型安全包装

```java
// 以下代码能通过编译（泛型擦除），但运行时出错
List<String> raw = new ArrayList<>();
List untyped = raw;                       // 未检查的警告
untyped.add(123);                         // ❌ 编译不报错，运行不报错（但集合被破坏）

// checked 包装 —— 运行时丢 ClassCastException
List<String> safe = Collections.checkedList(new ArrayList<>(), String.class);
List untypedSafe = safe;
untypedSafe.add(123);                     // ❌ ClassCastException 立即抛出
```

> 💡 `checked*` 在遗留代码集成、泛型擦除导致类型不安全的场景下提供运行时防护。

### 3.10 empty* —— 空集合单例 / singleton* —— 单元素集合 / nCopies

```java
// empty* —— 返回空不可变集合（单例，不分配新对象）
List<String> emptyList = Collections.emptyList();
Set<String> emptySet = Collections.emptySet();
Map<String, String> emptyMap = Collections.emptyMap();

// 💡 Java 9+ 替代：List.of()、Set.of()、Map.of() 也返回不可变空集合

// singleton* —— 恰好包含一个元素的不可变集合（比 new ArrayList + add 省内存）
List<String> single = Collections.singletonList("onlyOne");
Set<Integer> singleSet = Collections.singleton(42);
Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 1);
Map<String, Integer> singleMap = Collections.singletonMap("key", 1);

// singletonList 常用于 stream flatMap 返回单元素
// nCopies —— n 个相同引用的不可变 List（节省内存，共享同一对象引用）
List<String> tenNulls = Collections.nCopies(10, (String) null);
List<String> tenDefaults = Collections.nCopies(10, "DEFAULT");

// 实用场景：初始化指定大小且元素相同的列表
List<Integer> zeros = Collections.nCopies(100, 0);   // 100 个 0
```

> 🎯 `nCopies` 返回的 List 共享同一个对象引用，不占用额外内存，适合初始化默认值列表。

---

## 4. Comparator 进阶

**定位**：`java.util.Comparator` —— 函数式接口，Java 8+ 的链式 API 让排序逻辑表达力大幅提升。

### 4.1 comparing —— 链式比较 API

```java
// 实体类
class Employee {
    String name;
    int age;
    double salary;
}

// comparing —— 提取键 + 按自然序比较
Comparator<Employee> byName = Comparator.comparing(e -> e.name);

// 方法引用更简洁
Comparator<Employee> byNameRef = Comparator.comparing(Employee::getName);

// comparingInt / comparingLong / comparingDouble —— 避免装箱
Comparator<Employee> byAge = Comparator.comparingInt(Employee::getAge);
Comparator<Employee> bySalary = Comparator.comparingDouble(Employee::getSalary);
```

### 4.2 thenComparing —— 多级排序

```java
// 先按年龄排，年龄相同按姓名排
Comparator<Employee> byAgeThenName =
    Comparator.comparingInt(Employee::getAge)
              .thenComparing(Employee::getName);

// 多级：部门 → 年龄降序 → 姓名
Comparator<Employee> deptAgeName =
    Comparator.comparing(Employee::getDepartment)
              .thenComparingInt(Employee::getAge)
              .thenComparing(Employee::getName);

// 使用时
List<Employee> employees = getEmployees();
employees.sort(deptAgeName);
```

### 4.3 nullsFirst / nullsLast —— 空值安全排序

```java
// null 值出现时，排序会抛 NPE
List<String> names = Arrays.asList("Bob", null, "Alice", "Eve");

// nullsFirst —— null 排在最前面
names.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
// → [null, Alice, Bob, Eve]

// nullsLast —— null 排在最后面
names.sort(Comparator.nullsLast(Comparator.naturalOrder()));
// → [Alice, Bob, Eve, null]

// 自定义比较器 + null 处理
Comparator<Employee> byNameNullSafe =
    Comparator.nullsLast(
        Comparator.comparing(Employee::getName)
    );

// 💡 方法引用中处理可能为 null 的属性：
Comparator<Employee> byNullableName =
    Comparator.nullsLast(
        Comparator.comparing(Employee::getName,
            Comparator.nullsLast(Comparator.naturalOrder()))
    );
```

> 🎯 **何时用 nullsFirst vs nullsLast**：如果是"可选字段"，通常 `nullsLast` 更符合直觉（null 表示"没有值"排在末尾）。如果是"异常场景"，应在数据层面把 null 替换为默认值后再排序。

### 4.4 reversed —— 反转排序

```java
Comparator<Employee> byAgeDesc =
    Comparator.comparingInt(Employee::getAge).reversed();

// ⚠️ reversed 的位置影响链式顺序：
// 正确：先按年龄升序，再按姓名升序，整体反转
Comparator<Employee> c1 =
    Comparator.comparingInt(Employee::getAge)
              .thenComparing(Employee::getName)
              .reversed();               // → 年龄降序 + 姓名降序

// 错误：只有最后一步反转了
Comparator<Employee> c2 =
    Comparator.comparingInt(Employee::getAge)
              .reversed()
              .thenComparing(Employee::getName);  // → 年龄降序 + 姓名升序
```

### 4.5 naturalOrder / reverseOrder

```java
// naturalOrder —— 默认自然序（要求元素实现 Comparable）
Comparator<String> asc = Comparator.naturalOrder();

// reverseOrder —— 自然序的逆序
Comparator<String> desc = Comparator.reverseOrder();

// 等价写法：
Comparator<String> desc2 = Comparator.naturalOrder().reversed();
```

### 4.6 自实现 Comparator 的注意事项

```java
// ✅ 推荐：使用 Comparator 链式 API
Comparator<Employee> good =
    Comparator.comparingInt(Employee::getAge);

// ❌ 不推荐：匿名类实现（啰嗦、易错）
Comparator<Employee> bad = new Comparator<Employee>() {
    @Override
    public int compare(Employee a, Employee b) {
        return Integer.compare(a.getAge(), b.getAge());  // 必须用 Integer.compare 而非减法
    }
};

// ⚠️ 当心减法溢出陷阱：
Comparator<Integer> dangerous = (a, b) -> a - b;  // 当 a=Integer.MAX_VALUE, b=-1 时溢出
Comparator<Integer> safe = Integer::compare;        // ✅ 用 Integer.compare
```

> 🎯 **equals 与 Comparator 的关系**：Comparator 的 `equals` 方法继承自 Object，通常不重写。Comparator 的契约要求 `compare(a,b) == 0` 时 `a.equals(b)` 最好也成立，但不是强制。使用时注意：**TreeSet / TreeMap 用 Comparator 判断相等性，而非 equals**。

> 🎯 **序列化注意**：若用 Lambda 表达式实现 Comparator 并放入可序列化的集合（如 TreeMap），建议确保 Comparator 是 Serializable 的：
> ```java
> Comparator<Employee> c =
>     (Comparator<Employee> & Serializable)
>     Comparator.comparingInt(Employee::getAge);
> ```

---

## 5. Random 与 ThreadLocalRandom

**定位**：`java.util.Random` / `java.util.concurrent.ThreadLocalRandom` / `java.security.SecureRandom` —— 不同场景选择不同的随机数生成器。

### 5.1 Random —— 线性同余算法与种子

```java
// 无参构造 —— 使用系统时间作为种子
Random rnd = new Random();

// 指定种子 —— 可复现的随机序列（测试/模拟场景）
Random rndSeeded = new Random(42);

// 常用方法
int i = rnd.nextInt();          // [-2^31, 2^31-1]
int limited = rnd.nextInt(100); // [0, 100) —— 推荐，避免取模偏置
long l = rnd.nextLong();
double d = rnd.nextDouble();   // [0.0, 1.0)
float f = rnd.nextFloat();     // [0.0, 1.0)
boolean b = rnd.nextBoolean();

// Java 17+ —— nextInt 支持 origin + bound
int ranged = rnd.nextInt(10, 100);  // [10, 100)
```

**线性同余算法**（LCG）：
```
nextseed = (oldseed * 2345678 + 11111) & ((1 << 48) - 1)
```
- 优点：速度快
- 缺点：周期有限（2^48），分布质量不高（低维均匀性差），**不适合密码学**

### 5.2 线程安全的代价 —— CAS 竞争

```java
// Random 是线程安全的 —— 通过 CAS 更新种子
// 但高并发下 CAS 重试会降低性能
public int nextInt() {
    // 伪代码：
    do {
        oldseed = seed;
        nextseed = (oldseed * multiplier + addend) & mask;
    } while (!UNSAFE.compareAndSwapLong(this, seedOffset, oldseed, nextseed));
    return (int)(nextseed >>> 16);
}
```

> 💡 **高并发场景**：多个线程争抢同一个 Random 实例的种子 CAS，退化为自旋等待。此时应使用 `ThreadLocalRandom`。

### 5.3 ThreadLocalRandom —— 无竞争高性能

```java
// ThreadLocalRandom 设计：每个线程独立维护种子，无需 CAS
// 获取实例 —— 静态方法，无需 new
ThreadLocalRandom rnd = ThreadLocalRandom.current();

// API 与 Random 兼容
int i = rnd.nextInt(100);
int ranged = rnd.nextInt(10, 100);

// 特有方法：nextLong(origin, bound) —— Random 没有
long l = rnd.nextLong(1, 100);

// 流式生成
rnd.ints(10, 0, 100).toArray();     // 10 个 [0, 100) 的随机 int
```

**性能对比**：

| 场景 | Random | ThreadLocalRandom |
|------|--------|-------------------|
| 单线程 | 约 50 ns/次 | 约 50 ns/次（相近） |
| 8 线程竞争 | 约 300 ns/次（CAS 重试） | 约 50 ns/次（无竞争） |
| 16 线程竞争 | 约 600+ ns/次 | 约 50 ns/次 |

> 🎯 **最佳实践**：除非有明确理由用 Random（如需要种子可复现），**并发场景一律用 `ThreadLocalRandom.current()`**。单线程场景两者性能相近，ThreadLocalRandom 也完全可用。

### 5.4 Java 17+ nextInt(origin, bound) 简洁写法

```java
Random rnd = new Random();

// Java 8 写法：
int result = rnd.nextInt(100 - 10) + 10;        // [10, 100)

// Java 17+ 原生支持（更清晰）：
int result = rnd.nextInt(10, 100);               // [10, 100)
```

### 5.5 SecureRandom —— 密码学安全随机数

```java
// 底层从操作系统获取熵源（/dev/random, CryptGenRandom 等）
// 适用于：令牌生成、密码盐值、会话 ID、密钥

// 默认构造 —— 选择最安全的算法
SecureRandom sr = new SecureRandom();

// 指定算法
SecureRandom srStrong = SecureRandom.getInstance("SHA1PRNG");  // 可用的算法之一

byte[] token = new byte[32];
sr.nextBytes(token);          // 填充 32 个随机字节

// 获取种子（阻塞，等待熵池积累）
byte[] seed = sr.generateSeed(16);
```

| 类型 | 安全性 | 速度 | 适用场景 |
|------|--------|------|---------|
| Random | 低（可预测） | 极快 | 游戏、测试、模拟 |
| ThreadLocalRandom | 低（可预测） | 极快（无竞争） | 高并发随机、并行流 |
| SecureRandom | 高（不可预测） | 慢（阻塞等待熵池） | Token、密钥、密码学 |

> ⚠️ **不要在安全场景用 Random**：给定连续的 Random 输出，攻击者可反推种子，预测后续输出。

---

## 6. Scanner 与输入解析

**定位**：`java.util.Scanner` —— 将输入（System.in、文件、字符串）解析为 token 的便捷工具。

### 6.1 从不同源构造

```java
// 从标准输入
Scanner sc = new Scanner(System.in);

// 从文件（需 try-catch 或声明抛出 FileNotFoundException）
Scanner scFile = new Scanner(new File("data.txt"));

// 从字符串
Scanner scStr = new Scanner("1 2 3\n4 5 6");
```

### 6.2 hasNext* / next* 模式

```java
Scanner sc = new Scanner(System.in);

// nextInt —— 读取一个 int
System.out.print("请输入年龄：");
int age = sc.nextInt();

// nextDouble —— 读取 double
double salary = sc.nextDouble();

// next —— 读取下一个 token（遇空白符分隔）
String name = sc.next();

// nextLine —— 读取整行（包括空格，直到换行符）
String line = sc.nextLine();

// nextBoolean —— 读取 boolean
boolean flag = sc.nextBoolean();

// hasNext* 用于判断后续是否有对应类型的 token
while (sc.hasNextInt()) {
    int val = sc.nextInt();
    // 处理整数序列
}
```

### 6.3 useDelimiter —— 自定义分隔符

```java
// 默认分隔符：空白符（空格、制表符、换行）

// 逗号分隔（CSV 行）
Scanner csv = new Scanner("A,B,C,D");
csv.useDelimiter(",");
while (csv.hasNext()) {
    System.out.println(csv.next());  // A → B → C → D
}

// 正则表达式分隔符
Scanner sc = new Scanner("a::b::c");
sc.useDelimiter("::");
System.out.println(sc.next());  // a
System.out.println(sc.next());  // b
System.out.println(sc.next());  // c
```

### 6.4 useLocale —— 数字格式

```java
// 默认 locale 决定小数点分隔符和组分隔符

// 德文 locale：逗号作为小数点
Scanner scDe = new Scanner("3,14 2,71");
scDe.useLocale(Locale.GERMANY);
while (scDe.hasNextDouble()) {
    System.out.println(scDe.nextDouble());  // 3.14, 2.71
}

// 英文 locale：点作为小数点（默认）
Scanner scEn = new Scanner("3.14 2.71");
scEn.useLocale(Locale.US);
```

### 6.5 常见陷阱 —— nextLine 与 nextInt 混用的换行符残留

```java
Scanner sc = new Scanner(System.in);

System.out.print("请输入年龄：");
int age = sc.nextInt();       // 读取 25，但缓冲区还留着 '\n'

System.out.print("请输入姓名：");
String name = sc.nextLine();  // ❌ 立即返回空字符串！（读取了残留的 '\n'）

// ✅ 解决办法 1：先用 nextLine 读一行，再解析
Scanner sc2 = new Scanner(System.in);
System.out.print("请输入年龄：");
int age2 = Integer.parseInt(sc2.nextLine());
System.out.print("请输入姓名：");
String name2 = sc2.nextLine();

// ✅ 解决办法 2：在 nextInt 后额外调一次 nextLine 消耗换行符
Scanner sc3 = new Scanner(System.in);
int age3 = sc3.nextInt();
sc3.nextLine();               // 消耗残留换行符
String name3 = sc3.nextLine();
```

> 🎯 **根本原因**：`nextInt()` 不读取换行符，后续 `nextLine()` 读到残留的 `\n` 立即返回。**建议全程用 `nextLine()` + 手动解析**，避免类型转换的陷阱。

---

## 7. Properties 与配置

**定位**：`java.util.Properties` —— 继承 Hashtable，用于处理 key=value 格式的配置文件。

### 7.1 load / store —— properties 文件操作

```java
// 加载配置文件
Properties props = new Properties();

// 从类路径加载
try (InputStream in = PropertiesDemo.class.getClassLoader()
        .getResourceAsStream("config.properties")) {
    props.load(in);          // 加载 key=value 格式
}

// 读取属性
String url = props.getProperty("db.url");
String timeout = props.getProperty("db.timeout", "3000");  // 带默认值

// 写入属性
props.setProperty("app.version", "2.0");
try (OutputStream out = new FileOutputStream("config.properties")) {
    props.store(out, "Updated config");  // 第二个参数是注释
}
```

**properties 文件格式示例**：
```properties
# Database configuration
db.url=jdbc:mysql://localhost:3306/app
db.username=root
db.password=secret
db.pool.size=10
```

### 7.2 getProperty 带默认值

```java
// 两种带默认值的方式

// 方式一：getProperty 重载
int timeout = Integer.parseInt(
    props.getProperty("timeout", "3000")   // 缺少 key 时返回 "3000"
);

// 方式二：构造时传入默认 Properties
Properties defaults = new Properties();
defaults.setProperty("timeout", "3000");
defaults.setProperty("pool.size", "10");

Properties props = new Properties(defaults);  // 所有 key 都有默认值
props.load(inputStream);

String val = props.getProperty("timeout");  // 如果文件没有，就返回 "3000"
```

### 7.3 与 System.getProperties 的关系

```java
// JVM 系统属性 —— 通过 -D 参数传入
// java -Dapp.env=production -jar app.jar

// 读取
String env = System.getProperty("app.env", "dev");

// 获取全部系统属性
Properties sysProps = System.getProperties();
sysProps.list(System.out);  // 打印所有系统属性

// 常用系统属性
String javaVersion = System.getProperty("java.version");
String userHome = System.getProperty("user.home");
String osName = System.getProperty("os.name");

// 动态设置
System.setProperty("my.key", "my.value");
```

| 来源 | 方法 | 优先级（高→低） |
|------|------|----------------|
| 命令行 | `-Dkey=value` | 最高（可覆盖一切） |
| 环境变量 | `System.getenv()` | 中 |
| 配置文件 | Properties.load() | 低 |
| 硬编码默认值 | getProperty(key, default) | 最低 |

### 7.4 XML 格式支持

```java
// Properties 支持 XML 格式的加载/存储
Properties props = new Properties();

// 加载 XML 配置
try (InputStream in = Files.newInputStream(Paths.get("config.xml"))) {
    props.loadFromXML(in);
}

// 存储为 XML
try (OutputStream out = Files.newOutputStream(Paths.get("config.xml"))) {
    props.storeToXML(out, "Config XML", "UTF-8");
}
```

**XML 格式示例**：
```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
    <comment>Config XML</comment>
    <entry key="db.url">jdbc:mysql://localhost:3306/app</entry>
    <entry key="db.username">root</entry>
</properties>
```

### 7.5 Spring 中的 properties 使用

```java
// Spring Boot 中推荐的用法 —— 不在业务代码中直接操作 Properties 对象

// application.yml
// db:
//   url: jdbc:mysql://localhost:3306/app
//   pool-size: 10

// @ConfigurationProperties 绑定到 POJO
@Component
@ConfigurationProperties(prefix = "db")
public class DbConfig {
    private String url;
    @Min(5) @Max(100)
    private int poolSize = 10;     // 有默认值
    // getter / setter
}

// 或者 @Value 注入单个属性
@Component
public class AppService {
    @Value("${app.env:dev}")         // ✅ 支持默认值
    private String env;
}
```

### 7.6 Java 偏好使用 YAML / 环境变量替代

| 方案 | 优势 | 劣势 |
|------|------|------|
| `.properties` | 简单，JDK 原生支持 | 不支持层级结构，需要前缀命名 |
| `.yaml` / `.yml` | 支持层级、列表，可读性好 | 需要第三方库（SnakeYAML） |
| 环境变量 | Docker/K8s 原生，无文件依赖 | 类型只有 String，层级不明显 |
| 配置中心 | 动态刷新、多环境管理 | 引入外部依赖 |

> 🎯 **现代 Java 项目配置建议**：基础配置用 YAML（Spring Boot 默认），敏感信息（密码、Token）从环境变量读取，多环境切换靠 Profile（`application-dev.yml` / `application-prod.yml`）。**抛弃在 Properties 中硬编码敏感信息**。

---

## 8. 综合速查表

### 8.1 Objects 方法速查

| 方法 | 返回 | 作用 | 使用频率 |
|------|------|------|:-------:|
| `equals(a, b)` | boolean | null 安全相等比较 | ★★★★★ |
| `deepEquals(a, b)` | boolean | 数组深层递归比较 | ★★★☆☆ |
| `hashCode(o)` | int | null 安全哈希码（单值） | ★★★☆☆ |
| `hash(values...)` | int | 多值组合哈希码 | ★★★★☆ |
| `requireNonNull(obj)` | T | 空检查并返回对象 | ★★★★★ |
| `requireNonNull(obj, msg)` | T | 空检查 + 自定义消息 | ★★★★★ |
| `requireNonNull(obj, supplier)` | T | 空检查 + 延迟消息 | ★★☆☆☆ |
| `checkIndex(idx, len)` | int | 单索引边界检查（9+） | ★★★☆☆ |
| `checkFromToIndex(f,t,len)` | int | 范围边界检查（9+） | ★★☆☆☆ |
| `requireNonNullElse(obj, def)` | T | null 时返回默认值（9+） | ★★★★☆ |
| `requireNonNullElseGet(obj, sup)` | T | null 时延迟默认值（9+） | ★★★☆☆ |
| `toString(o, nullDefault)` | String | null 安全转字符串 | ★★★★☆ |
| `toIdentityString(o)` | String | 返回类名@哈希（17+） | ★☆☆☆☆ |

### 8.2 Arrays 方法速查

| 方法 | 作用 | 使用频率 |
|------|------|:-------:|
| `sort(array)` | 排序（基础类型 Dual-Pivot / 对象 TimSort） | ★★★★★ |
| `parallelSort(array)` | 并行排序（大数据量） | ★★★☆☆ |
| `binarySearch(array, key)` | 二分查找（需先排序） | ★★★★☆ |
| `copyOf(array, newLength)` | 数组拷贝（支持扩容/截断） | ★★★★★ |
| `copyOfRange(array, from, to)` | 范围拷贝 | ★★★★☆ |
| `equals(a, b)` | 逐个元素比较 | ★★★★☆ |
| `deepEquals(a, b)` | 多维数组深度比较 | ★★★☆☆ |
| `compare(a, b)` | 字典序比较（9+） | ★★☆☆☆ |
| `mismatch(a, b)` | 首个不同元素索引（9+） | ★★☆☆☆ |
| `fill(array, val)` | 统一填充 | ★★★★☆ |
| `setAll(array, generator)` | 按索引函数设置（8+） | ★★★☆☆ |
| `parallelSetAll(array, gen)` | 并行版 setAll | ★★☆☆☆ |
| `asList(array)` | 数组转固定大小 List | ★★★★★ |
| `stream(array)` | 数组转 Stream（8+） | ★★★★☆ |
| `toString(array)` | 打印一维数组 | ★★★★★ |
| `deepToString(array)` | 打印多维数组 | ★★★★☆ |
| `spliterator(array)` | 获取分割迭代器 | ★☆☆☆☆ |

### 8.3 Collections 方法速查

| 方法 | 作用 | 使用频率 |
|------|------|:-------:|
| `sort(list)` | 排序 | ★★★★★ |
| `binarySearch(list, key)` | 二分查找 | ★★★☆☆ |
| `reverse(list)` | 反转 | ★★★☆☆ |
| `shuffle(list)` | 随机打乱 | ★★★☆☆ |
| `rotate(list, distance)` | 旋转 | ★★☆☆☆ |
| `swap(list, i, j)` | 交换元素 | ★★★☆☆ |
| `min(collection)` | 最小值 | ★★★☆☆ |
| `max(collection)` | 最大值 | ★★★☆☆ |
| `frequency(collection, obj)` | 元素出现次数 | ★★★★☆ |
| `disjoint(c1, c2)` | 判断无交集 | ★★☆☆☆ |
| `fill(list, obj)` | 全部替换为同一值 | ★★★☆☆ |
| `copy(dest, src)` | 拷贝列表 | ★★☆☆☆ |
| `replaceAll(list, old, new)` | 替换所有匹配 | ★★★☆☆ |
| `unmodifiableXxx(c)` | 不可变包装 | ★★★★☆ |
| `synchronizedXxx(c)` | 同步包装 | ★★☆☆☆ |
| `checkedXxx(c, type)` | 类型安全检查 | ★☆☆☆☆ |
| `emptyXxx()` | 空集合单例 | ★★★★☆ |
| `singletonXxx(o)` | 单元素集合 | ★★★☆☆ |
| `nCopies(n, o)` | n 个相同引用的不可变 List | ★★★☆☆ |

### 8.4 Comparator 方法速查

| 方法 | 作用 | 使用频率 |
|------|------|:-------:|
| `comparing(keyExtractor)` | 按提取键升序 | ★★★★★ |
| `comparingInt/ Long/Double(keyExtractor)` | 按基础类型键升序（避免装箱） | ★★★★★ |
| `thenComparing(c)` | 多级排序 | ★★★★☆ |
| `thenComparingInt/ Long/Double(keyExtractor)` | 多级基础类型键 | ★★★★☆ |
| `nullsFirst(c)` / `nullsLast(c)` | 空值安全排序 | ★★★★☆ |
| `reversed()` | 反转排序 | ★★★★★ |
| `naturalOrder()` / `reverseOrder()` | 自然序 / 逆自然序 | ★★★☆☆ |

### 8.5 随机数方法速查

| 类 | 方法 | 作用 |
|----|------|------|
| `Random` | `nextInt()` | [-2^31, 2^31-1) |
| | `nextInt(bound)` | [0, bound) |
| | `nextInt(origin, bound)` | [origin, bound) （17+） |
| | `nextLong()` | 随机 long |
| | `nextDouble()` | [0.0, 1.0) |
| | `nextBytes(bytes)` | 填充字节数组 |
| `ThreadLocalRandom` | `current()` | 获取当前线程实例 |
| | `nextInt(origin, bound)` | 原生支持范围（同 Java 17 Random） |
| | `ints(count, min, max)` | 生成流式随机数 |
| `SecureRandom` | `nextBytes(bytes)` | 密码学安全随机字节 |

### 8.6 Scanner / Properties 速查

| 方法 | 作用 |
|------|------|
| `new Scanner(System.in / File / String)` | 构造 |
| `nextInt() / nextDouble() / nextBoolean()` | 读取特定类型 token |
| `next()` | 读取下一个 token |
| `nextLine()` | 读取整行 |
| `hasNextInt() / hasNextDouble() / hasNext()` | 判断下一个 token 类型 |
| `useDelimiter(pattern)` | 自定义分隔符 |
| `useLocale(locale)` | 设置数字格式 locale |
| `close()` | 关闭 Scanner（不会关闭 System.in） |

| 方法 | 作用 |
|------|------|
| `load(InputStream)` | 加载 .properties 文件 |
| `store(OutputStream, comment)` | 写 .properties 文件 |
| `loadFromXML(InputStream)` | 加载 XML 格式配置 |
| `storeToXML(OutputStream, comment, encoding)` | 写 XML 格式配置 |
| `getProperty(key, defaultValue)` | 读取属性 + 默认值 |
| `setProperty(key, value)` | 写入属性 |
| `System.getProperties()` | 系统属性 |
| `System.getenv(key)` | 环境变量 |

---

## 9. 练习

### 练习 1：用 Objects 重构空安全代码

重构以下代码，用 `Objects` 工具类替换手写 null 判断：

```java
// 要求：至少用 5 个不同的 Objects 方法
public class UserService {
    public void createUser(User user, Config config) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null at " + System.currentTimeMillis());
        }
        String name = user.getName();
        String displayName = name != null ? name : "anonymous";
        String[] tags = user.getTags();
        String[] defaultTags = {"new"};
        boolean tagsSame = Arrays.equals(tags, defaultTags);
        List<String> list = new ArrayList<>();
        list.add("A");
    }
}
```

> 💡 **提示**：用 `requireNonNull`、`requireNonNull(..., Supplier)`、`requireNonNullElse`、`deepEquals`、`Objects.hash` 等方法。

### 练习 2：数组操作实战

```java
// 给定以下数组，完成操作：
int[] scores = {85, 92, 78, 90, 88, 76, 95, 89};

// 1. 排序并打印
// 2. 二分查找 90 的位置
// 3. 拷贝前 5 个元素到新数组
// 4. 用 parallelSort 排序
// 5. 将 scores 转为 IntStream 并计算总分
// 6. 判断 scores 和 {85, 92, 78, 90, 88, 76, 95, 89} 是否相等
// 7. 找出 scores 和 {85, 92, 78, 90, 88, 76, 95, 99} 的第一个不同索引
```

### 练习 3：集合操作实战

```java
// 给定员工列表，用 Collections 和 Comparator 完成：
List<Employee> employees = Arrays.asList(
    new Employee("Alice", 30, 50000),
    new Employee("Bob", 25, 60000),
    new Employee("Charlie", 30, 45000),
    new Employee("David", 35, 60000)
);

// 1. 按薪资降序排序
// 2. 先按年龄升序，年龄相同按姓名降序
// 3. 找出薪资最高的员工
// 4. 统计姓名为 "Alice" 的出现次数
// 5. 将 list 包装为不可变 List
// 6. 创建包含 "UNKNOWN" 的 10 个副本的 List
```

### 练习 4：随机数与输入

```java
// 1. 用 ThreadLocalRandom 生成 10 个 [1, 100] 的随机数，计算平均值
// 2. 用 SecureRandom 生成 16 字节的 Token，转为十六进制字符串
// 3. 写一段代码从控制台读取 "姓名 年龄 薪资"，用 Scanner + 手动 nextLine 解析
//    （避免 nextInt/nextLine 混用陷阱）
```

### 练习 5：配置文件加载

```java
// 1. 从类路径加载 config.properties，读取 db.url 和 db.pool.size（带默认值）
// 2. 读取系统属性 java.version 和 os.name
// 3. 用 Properties.store 将当前配置导出到文件
// 4. 用 loadFromXML / storeToXML 实现 XML 格式的配置持久化
```

---

**上一模块**：[05-Optional与防御性编程实战](./05-Optional与防御性编程实战.md)
**返回总览**：[00-Java API知识体系总览](./00-Java API知识体系总览.md)
