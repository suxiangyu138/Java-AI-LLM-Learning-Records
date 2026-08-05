# 03 Objects 与 Arrays 工具函数

> Objects 是判空与比较的现代工具，Arrays 是数组操作的总管——requireNonNull、sort、binarySearch、asList 的坑，一次讲透

---

## 📚 目录

1. [Objects：判空与比较的现代工具](#1-objects判空与比较的现代工具)
2. [Arrays：数组操作总管](#2-arrays数组操作总管)
3. [数组转集合的三个坑](#3-数组转集合的三个坑)
4. [排序与二分：sort / binarySearch](#4-排序与二分sort--binarysearch)
5. [拷贝与填充：copyOf / fill](#5-拷贝与填充copyof--fill)
6. [函数速查表](#6-函数速查表)

---

## 1. Objects：判空与比较的现代工具

**Objects（JDK 7）**：null 安全工具——**消灭手写判空**：

```java
// 判空参数（方法入口的标准姿势）
public void setUser(User user) {
    this.user = Objects.requireNonNull(user, "user 不能为 null");
    // 比手写 if (user == null) throw NPE 更简洁，且可定制消息
}

// null 安全比较（替代 a.equals(b) 的 NPE 风险）
Objects.equals(null, null);      // true（两个 null 相等）
Objects.equals("a", null);       // false
Objects.equals("a", "a");        // true

// null 安全哈希（equals 的黄金搭档）
Objects.hash(amount, currency);  // 任意数量字段 → hash

// null 安全 toString
Objects.toString(obj);           // obj 为 null 返回 "null"
Objects.toString(obj, "默认值"); // 可指定默认

// 比较器
Objects.compare(a, b, comparator);   // null 安全比较（null 排前）
```

**Objects 的使用规范**：

```text
① 方法入参校验 → requireNonNull（替代手写 if）
② 任意 equals → Objects.equals（替代 a.equals(b)）
③ equals 重写 → Objects.equals + Objects.hash（黄金模板）
④ 日志/调试 → Objects.toString（null 安全）
```

> 🎯 **核心要点**：Objects = "**null 安全的四个工具（requireNonNull/equals/hash/toString）**"——**"用 Objects.equals 替代 a.equals(b)"消灭 NPE 是 2026 代码规范共识**；requireNonNull 是方法入参校验的标准姿势。

---

## 2. Arrays：数组操作总管

**Arrays（一直存在）**：数组的排序/查找/拷贝/转换：

```java
// 排序
int[] arr = {3, 1, 2};
Arrays.sort(arr);                       // 原地升序 [1,2,3]
Arrays.sort(arr, 1, 3);                 // 区间排序
Integer[] objArr = {3, 1, 2};
Arrays.sort(objArr, Comparator.reverseOrder());   // 对象数组自定义排序

// 二分查找（必须先排序！）
Arrays.binarySearch(arr, 2);            // 1（索引）
Arrays.binarySearch(arr, 5);            // -4（未找到：-(插入点)-1）

// 拷贝
int[] copy = Arrays.copyOf(arr, 5);     // 扩容拷贝（尾部补 0）
int[] part = Arrays.copyOfRange(arr, 1, 3);  // 区间拷贝

// 填充
Arrays.fill(arr, 0);                    // 全部置 0

// 比较与字符串
Arrays.equals(arr, arr2);               // 一维比较
Arrays.deepEquals(arr2d, arr2d2);       // 多维比较（deep* 用于嵌套）
Arrays.toString(arr);                   // [1, 2, 3]（打印数组必须用它！）
Arrays.deepToString(arr2d);             // 多维字符串
```

**数组打印的坑**（最高频）：

```java
System.out.println(arr);        // [I@1b6d3586（类型+地址！）
System.out.println(Arrays.toString(arr));   // [1, 2, 3] ✅
// 二维数组：Arrays.deepToString（toString 只打印引用）
```

> 🎯 **核心要点**：Arrays = "**sort/binarySearch（先排序）/copyOf/toString**"四核心——**"数组直接打印是地址，必须 Arrays.toString"是最常见的坑**；binarySearch 未命中的负返回值（-(插入点)-1）是细节考点。

---

## 3. 数组转集合的三个坑

**Arrays.asList 的三大陷阱**（面试必答）：

```java
// 坑 1：返回"固定大小"的视图（不是 ArrayList！）
List<String> list = Arrays.asList("a", "b");
list.add("c");       // ❌ UnsupportedOperationException（固定大小）
list.set(0, "x");    // ✅ 可以修改元素

// 坑 2：修改写穿原数组（是视图不是副本）
String[] arr = {"a", "b"};
List<String> list = Arrays.asList(arr);
list.set(0, "z");
arr[0];              // "z"（写穿！）

// 坑 3：基本类型数组是"单元素"列表！
int[] nums = {1, 2, 3};
List<int[]> list = Arrays.asList(nums);   // 1 个元素（int[] 本身）！
// ✅ 正确：Integer[] 或用 Stream：
List<Integer> ok = Arrays.stream(nums).boxed().toList();

// 想要真正的可变列表：
List<String> mutable = new ArrayList<>(Arrays.asList("a", "b"));   // ✅ 包装一层
```

**数组 → 集合的现代选择**：

```java
// JDK 9+ 不可变：List.of("a","b")
// JDK 16+ 流式：Arrays.stream(arr).toList()
// 可变：new ArrayList<>(Arrays.asList(...))
// 基本类型：Arrays.stream(ints).boxed().toList()
```

> 🎯 **核心要点**：asList 三坑 = "**固定大小（不可增删）+ 写穿数组 + 基本类型单元素**"——**"要可变就包 ArrayList、要不可变用 List.of"**是标准解法；基本类型数组必须 boxed。

---

## 4. 排序与二分：sort / binarySearch

**sort 的算法细节**（与 `Java集合框架/07-排序与比较器` 联动）：

```java
// 基本类型数组：双轴快排（不稳定，O(n log n)，原地）
Arrays.sort(intArr);

// 对象数组：TimSort 稳定归并（需要 Comparable 或 Comparator）
Arrays.sort(strArr);
Arrays.sort(userArr, Comparator.comparing(User::getAge));

// 大数据量：并行排序（多核利用）
Arrays.parallelSort(bigArr);    // JDK 8+，分段排序后合并
```

**binarySearch 的规则**：

```text
前提：必须先排序（否则结果未定义）
返回：
  命中 → 索引（≥0）
  未命中 → -(插入点) - 1（负值 → 可用 ~ret 得插入点）
  示例：binarySearch([1,3,5], 4) → -3（插入点 2 → -(2)-1 = -3）

应用：插入点 = -ret - 1 → insert(插入点, 元素) 保持有序
```

> 🎯 **核心要点**：排序 = "**基本类型双轴快排（不稳定）+ 对象 TimSort（稳定）+ parallelSort（大数据）**"——**"二分必须先排序 + 负返回值转插入点"**是细节考点（与集合排序的 TimSort 一致）。

---

## 5. 拷贝与填充：copyOf / fill

```java
// copyOf：扩容/截断拷贝（ArrayList 扩容的底层！）
int[] a = {1, 2, 3};
int[] b = Arrays.copyOf(a, 5);      // [1,2,3,0,0]（扩到 5）
int[] c = Arrays.copyOf(a, 2);      // [1,2]（截断）
// 底层：System.arraycopy（native 快速拷贝）
// 与 ArrayList 扩容：elementData = Arrays.copyOf(old, newCapacity)

// copyOfRange：区间拷贝
Arrays.copyOfRange(a, 1, 3);        // [2,3]

// fill：填充
Arrays.fill(a, 9);                  // [9,9,9]
Arrays.fill(matrix, new int[3]);    // 注意：引用填充（同一对象！）

// 二维数组深拷贝的坑：
int[][] copy = matrix.clone();      // 浅拷贝（只复制外层引用！）
// 深拷贝需逐行：for + Arrays.copyOf
```

**拷贝的两个坑**：

```text
① clone() 是浅拷贝（引用数组只复制引用层）
② fill 引用类型 = 填充同一对象（所有元素指向同一实例）
→ 二维/对象数组的"拷贝"要确认深度
```

> 🎯 **核心要点**：拷贝 = "**copyOf（扩容/截断，ArrayList 底层）+ 浅拷贝陷阱（clone/fill 引用）**"——**"二维数组 clone 是浅拷贝"是隐蔽坑**；copyOf 是 ArrayList 扩容的实现细节（`Java集合框架/02` 联动）。

---

## 6. 函数速查表

**Objects/Arrays 速查**（开发/面试快查）：

| 函数 | 作用 | 坑点 |
|------|------|------|
| Objects.requireNonNull | 判空参数 | 消息定制 |
| Objects.equals | null 安全比较 | 替代 a.equals(b) |
| Objects.hash | null 安全哈希 | equals 搭档 |
| Arrays.sort | 排序（快排/TimSort） | 对象需 Comparator |
| Arrays.binarySearch | 二分 | **必须先排序** |
| Arrays.copyOf | 扩容/截断 | ArrayList 底层 |
| Arrays.asList | 数组转视图 | 三坑（固定/写穿/基本类型） |
| Arrays.toString | 打印数组 | 直接 println 是地址 |
| Arrays.deepEquals | 多维比较 | 一维用 equals |
| Arrays.fill | 填充 | 引用填充同对象 |
| Arrays.parallelSort | 并行排序 | 大数据量 |

**一句话总结**：

```text
Objects 消灭手写判空（requireNonNull/equals/hash）
Arrays 覆盖数组全操作（sort/二分/copyOf/toString）
asList 三坑（固定大小/写穿/基本类型）
拷贝记住浅拷贝陷阱（clone/fill）
```

> 🎯 **核心要点**：速查 11 项 = "**Objects 四工具 + Arrays 七操作 + 三坑**"——**"数组用 Arrays.*、对象用 Objects.*"是记忆主线**；asList 三坑与 toString 是面试细节题富矿。

---

**下一模块**：[04-集合工具函数剖析](./04-集合工具函数剖析.md) / **返回总览**：[00-库函数剖析知识体系总览](./00-库函数剖析知识体系总览.md)
