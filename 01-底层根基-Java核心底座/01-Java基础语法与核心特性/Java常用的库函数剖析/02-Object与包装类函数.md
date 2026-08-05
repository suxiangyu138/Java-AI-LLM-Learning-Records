# 02 Object 与包装类函数

> equals/hashCode 契约、Integer 缓存池、装箱与 == 陷阱、parse 家族——Object 与包装类是 Java 细节题的富矿

---

## 📚 目录

1. [Object 三大方法契约](#1-object-三大方法契约)
2. [equals 重写的黄金模板](#2-equals-重写的黄金模板)
3. [Integer 缓存池与 valueOf](#3-integer-缓存池与-valueof)
4. [自动装箱与 == 陷阱](#4-自动装箱与--陷阱)
5. [parse 家族：parseInt / valueOf / toString](#5-parse-家族parseint--valueof--tostring)
6. [其他包装类实用函数](#6-其他包装类实用函数)
7. [函数速查表](#7-函数速查表)

---

## 1. Object 三大方法契约

**Object 是所有类的根**——三大方法的重写契约（与 `Java面向对象/01` 联动）：

| 方法 | 默认实现 | 重写契约 |
|------|---------|---------|
| equals | 引用相等（==） | 自反/对称/传递/一致 + null 返回 false |
| hashCode | 基于地址 | **equals 相等 → hashCode 必相等**（反之不必） |
| toString | `类名@哈希` | 返回有意义描述 |

**hashCode 契约的后果**（Set/Map 正确性基础）：

```java
// 只重写 equals 不重写 hashCode → HashSet 失效
public class User {
    private String name;
    @Override public boolean equals(Object o) { /* 值比较 */ }
    // hashCode 未重写 → 基于地址 → 相同值 hash 不同 → 不同桶 → contains 失败！
}
// 与 Java集合框架/03 的"去重正确性"联动
```

> 🎯 **核心要点**：三大契约 = "**equals 值相等 + hashCode 同步 + toString 可读**"——**"equals 相等必须 hashCode 相等"是 HashMap/HashSet 正确性的前提**；record（JDK 16+）自动实现三件套。

---

## 2. equals 重写的黄金模板

**equals 重写的标准五步**（面试手写题标准答案）：

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;                          // ① 同一对象
    if (o == null || getClass() != o.getClass()) return false;  // ② 类型检查
    Money money = (Money) o;                             // ③ 强转
    return amount == money.amount                        // ④ 字段比较
            && Objects.equals(currency, money.currency); // ⑤ 引用字段用 Objects.equals
}

@Override
public int hashCode() {
    return Objects.hash(amount, currency);               // 字段参与哈希
}
```

**equals 的两个设计决策**：

```text
① getClass() vs instanceof：
   getClass()：严格（子类不等）—— 集合规范推荐
   instanceof：宽松（子类可等）—— 但可能违反对称性
② 参与比较的字段必须稳定（可变字段入 equals = 集合丢元素）

现代替代：record 自动实现（组件全参与 + 值语义）
```

> 🎯 **核心要点**：equals 模板 = "**五步（自身/类型/null/强转/逐字段）+ 同步 hashCode**"——**"Objects.equals 处理引用字段（防 NPE）"**是模板的关键细节；能手写模板 + 说清 getClass vs instanceof = 面试满分。

---

## 3. Integer 缓存池与 valueOf

**Integer 缓存池（-128 ~ 127）**——valueOf 与装箱的核心：

```java
Integer a = Integer.valueOf(127);   // 缓存命中 → 返回池中对象
Integer b = Integer.valueOf(127);   // 同一对象
a == b;                              // true（缓存池）

Integer c = Integer.valueOf(128);   // 超出缓存 → new 新对象
Integer d = Integer.valueOf(128);
c == d;                              // false！

// 缓存池范围：-128 ~ 127（可用 -XX:AutoBoxCacheMax 调大）
// 来源：Byte/Short/Integer/Long（≤127）+ Character（≤127）
```

**装箱的底层**：

```text
Integer x = 100;          // 等价于 Integer.valueOf(100) → 走缓存池
Integer y = new Integer(100);  // 强制新对象（构造器已废弃 JDK 9+）
int z = x;                // 拆箱：intValue()

缓存池意义：
  高频小整数复用（省对象）→ 但带来 == 陷阱（见第 4 节）
```

> 🎯 **核心要点**：Integer 缓存池 = "**-128~127 共享对象（valueOf/装箱走池）**"——**"127 == 128 的差别"是 Java 最著名的面试题**；`new Integer()` 已废弃（JDK 9+），统一用 valueOf/装箱。

---

## 4. 自动装箱与 == 陷阱

**装箱/拆箱与 == 的完整规则**（面试必背）：

```java
Integer a = 127, b = 127;     // 装箱 → 缓存池 → a == b ✅ true
Integer c = 128, d = 128;     // 装箱 → 超缓存 → c == d ❌ false
Integer e = new Integer(128); // 显式 new → 永不相等

// 包装类 vs 基本类型比较：自动拆箱
Integer x = 100;
int y = 100;
x == y;                       // ✅ true（x 拆箱后比）
// ⚠️ 但：包装类 == 包装类 是引用比较（拆箱只在基本类型参与时）

// NPE 陷阱：自动拆箱 null
Integer n = null;
int m = n;                    // ❌ NullPointerException（拆箱 null）
```

**== 的完整决策树**：

```text
两侧都是基本类型 → 值比较
一侧基本类型一侧包装类 → 拆箱后值比较（null 会 NPE！）
两侧都是包装类 → 引用比较（缓存池内相等、池外不等）
→ 结论：包装类比较一律用 equals（或 intValue）
```

> 🎯 **核心要点**：装箱陷阱 = "**包装类 == 是引用比较（缓存池例外）+ 拆箱 null 抛 NPE**"——**"包装类比较用 equals"是铁律**；`Integer == int` 自动拆箱的 NPE 是生产常见事故。

---

## 5. parse 家族：parseInt / valueOf / toString

**字符串 ↔ 数字的三组函数**：

```java
// ① 转基本类型：parseInt（返回 int）
int i = Integer.parseInt("123");        // 123
// parseInt("12a") → NumberFormatException

// ② 转包装类：valueOf（返回 Integer，走缓存池）
Integer v = Integer.valueOf("123");     // 等价 parseInt + 装箱

// ③ 数字转字符串
String s1 = Integer.toString(123);      // "123"（最直接）
String s2 = String.valueOf(123);        // "123"（通用，null 安全）
String s3 = 123 + "";                   // "123"（拼接，编译器优化）

// 进制与格式化
Integer.parseInt("ff", 16);             // 255（按进制解析）
Integer.toHexString(255);               // "ff"
String.format("%05d", 123);             // "00123"（补零）
```

**parse 家族的选择速查**：

| 函数 | 返回 | null/异常 | 场景 |
|------|------|----------|------|
| parseInt | int | NumberFormatException | 直接要基本类型 |
| valueOf | 包装类 | NumberFormatException | 要包装类/装箱 |
| String.valueOf | String | **"null"**（不抛） | 任意对象转字符串 |
| toString | String | NPE（null 调用） | 已知非 null |

> 🎯 **核心要点**：parse 家族 = "**parseInt（基本类型）+ valueOf（包装类）+ String.valueOf（null 安全）**"——**"String.valueOf(null) 返回 'null' 而非 NPE"是细节考点**；进制与 format 是扩展能力。

---

## 6. 其他包装类实用函数

**各包装类的特色函数**：

```java
// Character
Character.isDigit('5');        // true
Character.isLetter('a');       // true
Character.isWhitespace(' ');   // true（含 Unicode）
Character.toUpperCase('a');    // 'A'

// Boolean
Boolean.parseBoolean("true");  // true（非 "true" 一律 false）
Boolean.TRUE;                  // 仅两个实例（TRUE/FALSE）
Boolean.valueOf("yes");        // false（只认 "true"）

// Double/Float
Double.isNaN(0.0 / 0.0);       // true（NaN 判断必须用 isNaN！）
Double.isInfinite(1.0 / 0.0);  // true
Double.parseDouble("1.5");     // 1.5
// ⚠️ 坑：Double.NaN == Double.NaN 为 false（NaN 不等于自身）
//    → 比较必须 isNaN()

// Long
Long.parseLong("12345678901"); // 大数
Long.toBinaryString(10);       // "1010"
```

**包装类选择速查**：

```text
数字场景：Integer/Long（常用）/ Double（浮点）
金额：BigDecimal（见 05 模块，勿用 Double）
布尔：Boolean（仅 TRUE/FALSE 两实例）
字符：Character（isDigit/isLetter 判断）
```

> 🎯 **核心要点**：包装类特色 = "**Character 判断、Boolean 两实例、Double 的 NaN 陷阱**"——**"NaN != NaN，必须 isNaN()"是最隐蔽的坑**；金额场景直接跳到 BigDecimal（05 模块）。

---

## 7. 函数速查表

**Object 与包装类速查**（面试快查）：

| 场景 | 函数 | 坑点 |
|------|------|------|
| 相等判断 | Objects.equals(a, b) | null 安全（勿用 a.equals(b)） |
| 哈希 | Objects.hash(...) | 与 equals 同步 |
| 缓存池 | Integer.valueOf | 127 == 128 陷阱 |
| 包装类比较 | equals / intValue | == 是引用比较 |
| 拆箱 | 自动 | null 拆箱 NPE |
| 解析 | parseInt / valueOf | 格式错抛异常 |
| 转字符串 | String.valueOf | null 安全（"null"） |
| NaN | Double.isNaN | == 永远 false |
| 判空参数 | Objects.requireNonNull | 见 03 模块 |

**一句话总结**：

```text
Object 与包装类的核心认知：
  equals 五步模板 + hashCode 同步（record 自动）
  Integer 缓存池（-128~127）→ 包装类比较用 equals
  拆箱 null 抛 NPE + NaN 不等于自身
  parse 家族三选（parseInt/valueOf/String.valueOf）
```

> 🎯 **核心要点**：速查九项 = "**Object 与包装类的完整考点清单**"——**缓存池、==、拆箱 NPE、NaN 四大陷阱**是细节题富矿；黄金模板 + 同步 hashCode 是手写题标准答案。

---

**下一模块**：[03-Objects与Arrays工具函数](./03-Objects与Arrays工具函数.md) / **返回总览**：[00-库函数剖析知识体系总览](./00-库函数剖析知识体系总览.md)
