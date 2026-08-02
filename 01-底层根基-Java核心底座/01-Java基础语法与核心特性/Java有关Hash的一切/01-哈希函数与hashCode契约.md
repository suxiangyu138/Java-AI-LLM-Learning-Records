# 01 - 哈希函数与 hashCode 契约

> 定位：理解哈希函数的本质特性，吃透 equals/hashCode 契约及其背后原理，掌握 String 的 31 之谜与自定义类的哈希最佳实践

## 📚 目录

1. [哈希函数基础](#1-哈希函数基础)
2. [equals 与 hashCode 契约](#2-equals-与-hashcode-契约)
3. [String.hashCode：31 之谜](#3-stringhashcode31-之谜)
4. [Objects.hash 与 record 自动哈希](#4-objectshash-与-record-自动哈希)
5. [自定义类 hashCode 最佳实践](#5-自定义类-hashcode-最佳实践)
6. [常见坑与排查](#6-常见坑与排查)
7. [面试高频考点](#7-面试高频考点)

---

## 1. 哈希函数基础

### 1.1 定义与特性

**哈希函数（Hash Function）**：把任意长度的输入映射为固定长度输出的函数。

```
f(x) = h     x 任意长度，h 固定长度（Java 中 hashCode 返回 int，32 位）
```

| 特性 | 说明 | 反例 |
|------|------|------|
| 确定性 | 同一输入必须同一输出 | 用 `new Random()` 生成的 hashCode |
| 高效性 | 计算开销必须小 | 把整个文件内容循环加密 |
| 均匀性 | 输出在值域上尽量均匀分布 | 只取低位 8 位做哈希 |
| 雪崩效应 | 输入微变 → 输出剧烈变化 | 直接返回 `key.length()` |

> 💡 **注意**：Java 对象 `hashCode()` 是**非加密哈希**（不要求不可逆、不要求抗碰撞），和 MD5/SHA 这类**加密哈希**是两回事，详见 [07-密码学哈希与安全哈希](07-密码学哈希与安全哈希.md)。

### 1.2 哈希表为什么是 O(1)

```
哈希表核心思想：空间换时间
put:  index = hash(key) % capacity → 直接定位桶
get:  index = hash(key) % capacity → 直接定位桶
```

关键结论：**哈希表的时间复杂度取决于哈希函数把 key 均匀打散到桶的能力**，而不是数据量 n。理想情况下每个桶至多一个元素，读写都是 O(1)。

### 1.3 碰撞的必然性

32 位 int 只有 2^32 ≈ 42.9 亿种取值，但 key 的可能取值是无限的（如任意长度的 String）。根据**鸽巢原理**，碰撞不可避免——所以哈希表必须解决碰撞，Java 采用**链地址法**（数组 + 链表/红黑树）。

---

## 2. equals 与 hashCode 契约

### 2.1 官方契约（Object.hashCode 的 javadoc）

| 条款 | 内容 |
|------|------|
| 一致性 | 同一对象多次调用 hashCode() 必须返回同一值（equals 未改变时） |
| 对称性 | **equals 相等 → hashCode 必须相等**（核心契约） |
| 反向不要求 | hashCode 相等 → equals 不一定相等（允许碰撞） |

### 2.2 为什么必须"先 equals 后 hashCode"

HashMap 的查找是两段式：

```java
// HashMap.getNode 中的判断
first.hash == hash && ((k = first.key) == key || (key != null && key.equals(k)))
//         ↑ 快速过滤（hash 比较）          ↑ 精确匹配（equals）
```

若两个 equals 相等的对象 hash 不同 → 它们落在不同桶 → **HashMap 里永远找不到**。后果：

```java
class BadHash {                          // ❌ 重写 equals 不重写 hashCode
    private final String id;
    BadHash(String id) { this.id = id; }
    @Override public boolean equals(Object o) {
        return o instanceof BadHash && ((BadHash) o).id.equals(this.id);
    }
}

Map<BadHash, String> map = new HashMap<>();
map.put(new BadHash("a"), "v1");
map.get(new BadHash("a"));   // null ！equals 相同但 hash 不同，找不到
```

> 🎯 **契约的本质**：equals 定义"逻辑相等"，hashCode 定义"去哪个桶找"。两者必须协作，否则集合操作行为不可预测。

### 2.3 常见理解误区

| 误区 | 正解 |
|------|------|
| hashCode 必须唯一 | 错，允许碰撞，均匀即可 |
| equals 不重写就不用管 hashCode | 错，两个不同实例 equals 为 false 但 hash 相同没问题；关键是不能"该相等时不相等" |
| 用 == 判断即可 | 值对象（String/Integer/自定义 VO）必须用 equals |

---

## 3. String.hashCode：31 之谜

### 3.1 公式与源码

```java
// JDK String.hashCode() 源码（长期未变，JDK 8 → 26 一致）
public int hashCode() {
    int h = hash;                       // 缓存字段（String 不可变，可缓存）
    if (h == 0 && value.length > 0) {
        char val[] = value;
        for (int i = 0; i < value.length; i++)
            h = 31 * h + val[i];        // ⭐ 核心：加权多项式
        hash = h;
    }
    return h;
}
```

展开即：`s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]`

### 3.2 为什么偏偏是 31？

| 原因 | 解释 |
|------|------|
| 奇素数 | 乘法哈希理论认为**奇素数**做乘数能减少碰撞。偶数会导致低位信息丢失（乘 2 左移一位，末尾补 0） |
| 乘法优化 | JVM 把 `31 * h` 优化为 `(h << 5) - h`（移位+减法），比乘法快一个数量级 |
| 经验数据 | Effective Java 作者 Joshua Bloch 实测：用 31、33、37、39、41 做乘数碰撞率差异极小，31 性能最好 |
| 分布折中 | 乘数太小人值类冲突多，太大易溢出；31 是"不大不小"的质数 |

```java
// 推导验证：31 * h ≡ (h << 5) - h
h = 5
31 * 5  = 155
(5 << 5) - 5 = 160 - 5 = 155 ✅
```

> ⚠️ **注意溢出**：int 计算会溢出（`31 * h` 溢出是预期的），溢出反而增加了低位的随机性，属于"有意的缺陷"。两个不同字符串有极小概率碰撞（如 `"Aa"` 与 `"BB"`：`65*31+97 = 2112`，`66*31+66 = 2112`）。

### 3.3 哈希缓存（cached hash）

String 不可变 → hash 一定不变 → 用字段 `hash` 缓存，**首次计算后永久复用**。这就是为什么 String 作为 HashMap key 性能极好。自定义不可变类也应采用同样模式。

---

## 4. Objects.hash 与 record 自动哈希

### 4.1 Objects.hash（JDK 7+）

```java
// Objects.hash 源码本质：把参数装箱后交给 Arrays.hashCode
public static int hash(Object... values) {
    return Arrays.hashCode(values);
}

// Arrays.hashCode 的乘子同样是 31
public static int hashCode(Object a[]) {
    if (a == null) return 0;
    int result = 1;
    for (Object element : a)
        result = 31 * result + (element == null ? 0 : element.hashCode());
    return result;
}
```

> 💡 注意：`Objects.hash(a, b)` 会**装箱**（Integer 等），性能略低于手写 `31 * x + y`。热点代码可手写，普通代码用 Objects.hash 更清晰。

### 4.2 record 自动生成 hashCode（JDK 16+）

```java
// record 自动生成 equals/hashCode/toString，禁止手写
public record Point(int x, int y) { }

Point p1 = new Point(1, 2);
Point p2 = new Point(1, 2);
p1.equals(p2);   // true（按组件比较）
p1.hashCode();   // 内部用 ObjectMethods bootstrap 按组件组合，乘子同样为 31
```

| 方式 | 适用场景 | 注意 |
|------|---------|------|
| record | 纯数据载体（DTO/VO） | 自动生成，组件必须是不可变 |
| Objects.hash | 快速实现 | 有装箱开销 |
| 手写 31*h+... | 性能敏感 | 需逐字段处理 null |
| Lombok @Data | 老项目习惯 | 与 record 二选一 |

---

## 5. 自定义类 hashCode 最佳实践

### 5.1 Effective Java 标准流程

```java
public final class User {
    private final String name;      // 参与哈希的字段必须是不可变或不被修改
    private final int age;

    @Override
    public int hashCode() {
        int result = 17;            // 起始非零值（任意非零常数）
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + age;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User u = (User) o;
        return age == u.age && Objects.equals(name, u.name);
    }
}
```

**步骤口诀**：选不可变字段 → 非零初始值 → 每字段 `31 * result + fieldHash` → 与 equals 使用**完全相同的字段集**。

### 5.2 等价简化与缓存

```java
// 简化版（功能等价）
@Override public int hashCode() { return Objects.hash(name, age); }

// 不可变类 + 高频使用 → 缓存哈希
private int cachedHash;                       // 默认 0
@Override public int hashCode() {
    if (cachedHash == 0) cachedHash = Objects.hash(name, age);
    return cachedHash;
}
```

> ⚠️ 缓存陷阱：只有字段**不可能变化**（final / 不可变类）才能缓存。字段可变时缓存会导致哈希过期 → 元素在 HashMap 中"找不回"。

---

## 6. 常见坑与排查

| 坑 | 现象 | 解决 |
|----|------|------|
| 重写 equals 忘了 hashCode | Map 找不到已放入的元素 | 两个必须同时重写（IDEA 有检查） |
| 可变字段参与哈希 | 放入 Map 后改字段 → 永远取不到 | 用不可变字段，或设计成不可变类 |
| 业务 key 高碰撞 | HashMap 桶严重不均衡，性能退化 | 用 `Objects.hash` 组合多个维度 |
| hashCode 返回固定值 | 全部挤到一个桶 → O(n) | 见 [05-哈希碰撞攻击与HashDoS](05-哈希碰撞攻击与HashDoS.md) |
| 手写哈希漏掉 null 处理 | NPE 或字段不一致 | null 一律返回 0 |

**排查工具**：`System.identityHashCode(obj)` 可拿"引用哈希"与重写后的 hashCode 对比；监控桶分布可用 `HashMap` 的迭代统计链表长度。

---

## 7. 面试高频考点

**Q1: hashCode 和 equals 的关系？** equals 相等 → hashCode 必相等；hashCode 相等 → equals 不一定相等。重写 equals 必须重写 hashCode，否则 HashMap/HashSet 行为异常。

**Q2: 为什么 String 的乘数是 31？** 奇素数（避免偶数导致低位信息丢失）、JVM 可优化为 `(h<<5)-h`、经验上分布与性能均衡。

**Q3: hashCode 可以随机吗？** 不可以，违反一致性条款。但 HashDoS 防御中 JVM 会对某些内置类型的哈希加随机种子（见 05 模块）。

**Q4: record 需要手写 hashCode 吗？** 不需要，编译器自动生成基于组件的实现（ObjectMethods bootstrap）。

**Q5: 两个不同字符串可能 hashCode 相同吗？** 可能（碰撞不可避免，如 `"Aa"`/`"BB"`），Java 用链表/红黑树解决。

---

> 🎯 **核心要点**：hashCode 契约是 Java 集合的基石——equals 决定"是否同一个"，hashCode 决定"去哪个桶找"。31 的选取兼顾了素数理论、JVM 优化与工程经验。

---

**下一模块**：[02-HashMap源码深度剖析](02-HashMap源码深度剖析.md)（哈希在集合框架中的核心应用） / **返回总览**：[00-Hash总览](00-Hash总览.md)
