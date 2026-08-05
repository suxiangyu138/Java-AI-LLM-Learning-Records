# 04 EnumSet 与 EnumMap

> 枚举专属的集合与映射——位向量与数组实现让它们比 HashSet/HashMap 快一个数量级，是"有限状态集合"建模的性能武器

---

## 📚 目录

1. [EnumSet：位向量实现的集合](#1-enumset位向量实现的集合)
2. [EnumSet 的操作与组合](#2-enumset-的操作与组合)
3. [EnumMap：数组实现的映射](#3-enummap数组实现的映射)
4. [性能对比：Enum vs Hash 家族](#4-性能对比enum-vs-hash-家族)
5. [典型应用场景](#5-典型应用场景)
6. [边界与注意](#6-边界与注意)

---

## 1. EnumSet：位向量实现的集合

**EnumSet**：专为枚举设计的 Set——底层是**位向量（bit vector）**：

```text
枚举 ≤ 64 个常量 → 单个 long（64 位，每位一个常量）
枚举 > 64 个 → long[]（每 64 个常量一组）

示例（Day 7 个常量）：
  EnumSet.of(MON, FRI) → long 值 0b100010（位 1 和 5 置 1）
  contains(MON)        → (bits & 1L << ordinal(MON)) != 0  ← 一次位运算！
```

**为什么快**：所有集合操作 = 位运算（`&` `|` `^` `~`），**无哈希计算、无对象分配、无碰撞**——比 HashSet 快一个数量级且内存极小。

```java
// 创建方式（枚举集合只能静态工厂，不能 new）
EnumSet<Day> weekend = EnumSet.of(Day.SATURDAY, Day.SUNDAY);
EnumSet<Day> all = EnumSet.allOf(Day.class);
EnumSet<Day> none = EnumSet.noneOf(Day.class);
EnumSet<Day> workdays = EnumSet.range(Day.MONDAY, Day.FRIDAY);  // 连续范围
EnumSet<Day> complement = EnumSet.complementOf(weekend);         // 补集
```

> 🎯 **核心要点**：EnumSet = "**枚举集合的位向量优化**"——**枚举序（ordinal）即 bit 位**，所有操作降维为位运算；创建只能用静态工厂（无 public 构造器）。

---

## 2. EnumSet 的操作与组合

**集合运算全是位运算**（性能核心）：

```java
EnumSet<Day> workdays = EnumSet.range(Day.MONDAY, Day.FRIDAY);
EnumSet<Day> weekend = EnumSet.of(Day.SATURDAY, Day.SUNDAY);

// 并集：| 位或
EnumSet<Day> all = EnumSet.copyOf(workdays);
all.addAll(weekend);                    // 全部 7 天

// 交集：& 位与
EnumSet<Day> both = EnumSet.copyOf(workdays);
both.retainAll(weekend);                // 空集（工作日 ∩ 周末）

// 差集：& ~ 位与非
EnumSet<Day> diff = EnumSet.copyOf(all);
diff.removeAll(weekend);                // 工作日

// 判断
workdays.contains(Day.MONDAY);          // true（一次位运算）
workdays.containsAll(weekend);          // false
```

**EnumSet 与普通 Set 的接口一致**——`Set<Day>` 声明、EnumSet 实现，切换零成本：

```java
public Set<Day> getAvailableDays() {
    return EnumSet.of(Day.MONDAY, Day.TUESDAY);   // 返回 Set 接口，内部位向量
}
```

> 🎯 **核心要点**：EnumSet 的集合运算 = "**位运算级性能**"（并/交/差都是 `|` `&` `&~`）——**API 与 Set 完全一致**（零学习成本），性能与内存是白送的优化。

---

## 3. EnumMap：数组实现的映射

**EnumMap**：专为枚举键设计的 Map——底层是**数组**（按枚举序索引）：

```text
Map<Day, String> schedule = new EnumMap<>(Day.class);
→ 内部数组 Object[7]，schedule.get(Day.MONDAY) = array[0]  ← 数组下标！
→ 无哈希计算、无碰撞、无扩容 rehash
→ 迭代顺序 = 枚举声明顺序（天然有序）
```

```java
// 用法与 HashMap 一致，构造需传枚举类型
Map<Day, String> schedule = new EnumMap<>(Day.class);
schedule.put(Day.MONDAY, "开会");
schedule.put(Day.FRIDAY, "复盘");
schedule.get(Day.MONDAY);        // "开会"（数组下标访问）
schedule.size();                 // 2

// 或从已有 Map 构建
Map<Day, String> fromMap = new EnumMap<>(existingMap);
```

**EnumMap vs HashMap（枚举键场景）**：

| 维度 | EnumMap | HashMap |
|------|:-------:|:-------:|
| 底层 | **数组（按枚举序）** | 哈希表 |
| get/put | **O(1) 数组下标** | O(1) 哈希（有计算） |
| 迭代顺序 | 枚举声明序（确定） | 无保证 |
| 内存 | 极低（无 Node 对象） | 高（Node + 哈希结构） |
| null 键 | ❌ 禁止 | ✅ 允许 |

> 🎯 **核心要点**：EnumMap = "**枚举键的数组映射**"——**键的枚举序即数组下标**（O(1) 且无哈希开销）；迭代顺序天然是枚举声明序（确定性）；**枚举键场景无脑用 EnumMap**。

---

## 4. 性能对比：Enum vs Hash 家族

**性能与内存对比（枚举场景）**：

| 维度 | EnumSet | HashSet | EnumMap | HashMap |
|------|:-------:|:-------:|:-------:|:-------:|
| 底层 | long 位向量 | 哈希表 | 数组 | 哈希表 |
| contains/get | 1 次位运算 | 1 次哈希+equals | 数组下标 | 1 次哈希+equals |
| 内存/元素 | **~1 bit** | ~32B+ | **~1 引用** | ~48B+ |
| 迭代 | 位扫描 | 桶遍历 | 数组遍历 | 桶遍历 |
| 速度 | **快一个数量级** | 基准 | **快一个数量级** | 基准 |

**实测量级（社区基准参考）**：百万次 contains——EnumSet 比 HashSet 快 **5-10 倍**，且内存少 30 倍以上（位 vs 对象）。

**为什么差距这么大**：

```text
HashSet：hashCode 计算 → 桶定位 → equals 比较（对象图遍历）
EnumSet：bits & (1L << ordinal)（一条 CPU 指令）
HashMap：hash + 桶 + 节点对象访问
EnumMap：array[ordinal]（一次内存访问，无计算）
```

> 🎯 **核心要点**：性能对比结论 = "**枚举集合用 Enum 家族，别用 Hash 家族**"——**快 5-10 倍 + 内存少 30 倍**是"位运算 vs 哈希"的物理差距；选择零成本（API 相同），性能白赚。

---

## 5. 典型应用场景

**场景一：权限/能力集合**（位向量天然适合"多选标记"）：

```java
public enum Permission { READ, WRITE, DELETE, EXPORT }

// 角色权限：组合标记
Set<Permission> admin = EnumSet.allOf(Permission.class);
Set<Permission> editor = EnumSet.of(Permission.READ, Permission.WRITE);
Set<Permission> viewer = EnumSet.of(Permission.READ);

// 校验（位运算级）
if (rolePermissions.containsAll(required)) { grant(); }
```

**场景二：状态集合查询**：

```java
// 查询"可取消"的状态集合
Set<OrderStatus> cancellable = EnumSet.of(OrderStatus.PENDING, OrderStatus.PAID);
if (cancellable.contains(order.getStatus())) { allowCancel(); }
```

**场景三：配置映射（EnumMap）**：

```java
// 状态 → 处理器映射（替代 switch 的注册表形态）
Map<OrderStatus, StatusHandler> handlers = new EnumMap<>(OrderStatus.class);
handlers.put(OrderStatus.PENDING, new PendingHandler());
handlers.put(OrderStatus.PAID, new PaidHandler());
// 使用：handlers.get(status).handle(order)  ← 数组下标级查找
```

> 🎯 **核心要点**：典型场景 = "**权限组合（位标记）+ 状态集合（快速 contains）+ 处理器注册表（EnumMap）**"——**"有限状态 + 组合/查询/映射"三件事全是枚举集合的主场**。

---

## 6. 边界与注意

| 注意 | 说明 |
|------|------|
| 元素必须枚举 | EnumSet 只接受枚举类型（构造时声明） |
| null 元素/键 | EnumSet 禁 null；EnumMap 禁 null 键（值可 null） |
| 构造方式 | 只能静态工厂（`of/allOf/range/complementOf`）与 `copyOf`——不能 new |
| 迭代顺序 | EnumSet/EnumMap 都按**枚举声明序**（确定性，可依赖） |
| 线程安全 | 与普通集合一致（非安全）——并发用同步包装 |
| 序列化 | EnumSet 序列化用位向量表示（省空间） |

```java
// 序列化注意：EnumSet 反序列化后的类可能是 JumboEnumSet（>64 常量）
// 接口声明 Set<Day> 可避免对具体类的依赖
public Set<Day> getDays() { return EnumSet.noneOf(Day.class); }   // 返回接口
```

> 🎯 **核心要点**：边界 = "**枚举专属 + 静态工厂 + 禁 null 键/元素 + 声明序迭代**"——**声明返回类型用 Set/Map 接口**（内部实现可换 JumboEnumSet 等）；并发场景按需同步。

---

**下一模块**：[05-枚举最佳实践与反模式](./05-枚举最佳实践与反模式.md) / **返回总览**：[00-枚举知识体系总览](./00-枚举知识体系总览.md)
