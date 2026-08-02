# 07 - JavaScript ES2025 与 ES2026 新特性

> 定位：ES2025（2025-06 发布）与 ES2026（2026-06 预计）的最新标准特性——迭代器增强、Temporal、using、Set 集合运算等（2026-07 验证）

## 📚 目录

1. [版本背景](#1-版本背景)
2. [ES2025：Iterator Helpers](#2-es2025iterator-helpers)
3. [ES2025：Set 集合运算](#3-es2025set-集合运算)
4. [ES2025：Promise.try 与 RegExp 增强](#4-es2025promisetry-与-regexp-增强)
5. [ES2026：Temporal 日期时间 API](#5-es2026temporal-日期时间-api)
6. [ES2026：using 资源管理](#6-es2026using-资源管理)
7. [ES2026：其他稳定特性](#7-es2026其他稳定特性)

---

## 1. 版本背景

```
版本演进（2026-07 验证）：
  ES2025（ES16）：2025-06-25 正式批准（第 129 届 Ecma 大会）
  ES2026（ES17）：预计 2026-06 发布（3 月分支规范）

趋势：将常见工程模式原生化，减少样板代码与第三方依赖

⚠️ 面试必答：
"ES2025/2026 的主题是'工程模式原生化'——
 迭代器增强替代手写循环、
 using 替代 try/finally、
 Temporal 替代 dayjs/moment。"
```

---

## 2. ES2025：Iterator Helpers

### 2.1 核心方法

```javascript
// 迭代器终于支持链式操作（惰性求值！）
Iterator.from(iterable)        // 包装为增强迭代器

// 方法：map/filter/take/drop/flatMap/reduce/forEach/some/every/find/toArray

// ⚠️ 惰性：不调用 toArray 不执行——适合无限序列
function* naturalNumbers() {
    let n = 1;
    while (true) yield n++;
}

naturalNumbers()
    .filter(n => n % 2 === 0)
    .map(n => n * n)
    .take(5)
    .toArray();          // [4, 16, 36, 64, 100]
```

### 2.2 对比数组方法

| 维度 | 数组方法 | Iterator Helpers |
|------|:---:|:---:|
| 求值 | 立即（全量） | **惰性（按需）** |
| 无限序列 | ❌ | ✅ |
| 内存 | 全量占用 | 流式 |
| 返回值 | 数组 | 迭代器（可继续链） |

```javascript
// 惰性的价值：提前终止零浪费
const first3 = naturalNumbers().filter(isPrime).take(3).toArray();
// ⚠️ 只计算到第 3 个素数——后续永不执行

// ES2026 候选：Iterator.concat / zip（Stage 3）
// Iterator.concat(it1, it2)    串联迭代器
// Iterator.zip(it1, it2)       并行配对
```

> 🎯 **要点**：Iterator Helpers 是 ES2025 最大亮点——**惰性链式处理无限序列**。手写"取前 N 个素数/斐波那契"从笨拙循环变为优雅链。

---

## 3. ES2025：Set 集合运算

### 3.1 七大新方法

```javascript
const a = new Set([1, 2, 3]);
const b = new Set([2, 3, 4]);

// 集合运算（返回新 Set，非变异）
a.union(b);               // {1,2,3,4}   并集
a.intersection(b);        // {2,3}       交集
a.difference(b);          // {1}         差集
a.symmetricDifference(b); // {1,4}       对称差

// 关系判断（返回布尔）
a.isSubsetOf(b);          // false       子集
a.isSupersetOf(b);        // false       超集
a.isDisjointFrom(b);      // false       不相交
```

### 3.2 实战价值

```javascript
// 权限/标签过滤的标准写法（替代手写 filter）
const allowed = user.permissions.intersection(requiredPermissions);

// 差集 = 需要补充的权限
const missing = requiredPermissions.difference(user.permissions);

// ⚠️ 参数是"类集合"（有 size/has/keys 即可）——可传 Map/自定义结构
// ⚠️ 非变异：原 Set 不变
```

---

## 4. ES2025：Promise.try 与 RegExp 增强

### 4.1 Promise.try

```javascript
// 统一包装"可能同步抛错或返回 Promise"的函数
function load() {
    if (!cached) throw new Error('未缓存');    // 同步抛错
    return fetchData();                        // 或返回 Promise
}

// ❌ 旧写法：同步异常逃出 .catch
load().catch(err => handle(err));              // 同步 throw 不被捕获！

// ✅ Promise.try：同步异常也转为 rejected
Promise.try(load).catch(err => handle(err));   // 统一处理
```

### 4.2 RegExp 三项增强

```javascript
// ① RegExp.escape()：安全转义用户输入
const pattern = RegExp.escape(userInput);      // 输入特殊字符自动转义
new RegExp(pattern).test(text);                // 防注入

// ② 重复命名捕获组（不同分支允许同名）
const re = /(?<year>\d{4})|(?<year>\d{2})/;
// ⚠️ ES2025 前：不同分支同名组报错

// ③ RegExp 修饰符（内联标志）
/(?i:hello)\s+world/;      // 仅 hello 部分忽略大小写
```

---

## 5. ES2026：Temporal 日期时间 API

### 5.1 为什么需要 Temporal

```
Date 的三大问题：
  ① 不可变？——Date 是可变对象（setFullYear 原地改）
  ② 时区混乱——本地时区/UTC 语义含糊
  ③ 解析不可靠——new Date('2024-01-01') 各浏览器不一致

Temporal：不可变 + 时区感知 + ISO 8601 原生支持

⚠️ 面试必答：
"Temporal 是 Date 的继任者——
 不可变（所有操作返回新对象）、
 时区一等公民、ISO 字符串原生解析。"
```

### 5.2 核心类型

```javascript
// 主要类型（2026-03 达 Stage 4）：
Temporal.PlainDate      // 纯日期（无时间无时区）
Temporal.PlainTime      // 纯时间
Temporal.ZonedDateTime  // 带时区的时刻（最常用）
Temporal.Instant        // 绝对时刻（UTC）
Temporal.Duration       // 时长

// 创建
const today = Temporal.PlainDate.from('2026-08-02');
const now = Temporal.Now.zonedDateTimeISO();

// ⚠️ 不可变操作：返回新对象
const tomorrow = today.add({ days: 1 });
const nextWeek = today.add({ weeks: 1 });

// 差值计算
const diff = date2.since(date1, { largestUnit: 'days' });
diff.days;               // 天数差

// 时区感知
const meeting = Temporal.ZonedDateTime.from(
    '2026-08-02T10:00:00[Asia/Shanghai]'
);
```

| 特性 | 说明 |
|------|------|
| 不可变 | add/subtract 返回新实例 |
| ISO 解析 | 字符串带 `[时区]` 注解 |
| 单位明确 | largestUnit/smallestUnit 参数 |
| 精确比较 | `Temporal.Instant.compare(a, b)` |

> 🎯 **要点**：Temporal 是 ES2026 最重磅特性——替代 moment.js/dayjs 的日期场景。面试表达："不可变 + 时区感知 + ISO 原生"三点即可。

---

## 6. ES2026：using 资源管理

### 6.1 基本用法

```javascript
// using = 作用域退出自动清理资源（替代 try/finally）
class Database {
    [Symbol.dispose]() {           // ⚠️ 清理钩子（同步）
        console.log('关闭连接');
    }
}

{
    using db = new Database();     // 作用域结束时自动 dispose
    db.query('SELECT ...');
}   // ← 自动调用 db[Symbol.dispose]()

// 异步资源
class Stream {
    async [Symbol.asyncDispose]() {
        await this.close();
    }
}
{
    await using stream = new Stream();   // 退出自动 await close()
}
```

### 6.2 价值与细节

```javascript
// 替代 try/finally 模式（更简洁 + 防遗漏）
// ❌ 旧写法
const db = openDb();
try {
    db.query(...);
} finally {
    db.close();              // 容易忘/容易乱
}

// ✅ using（自动清理，LIFO 逆序）
{
    using a = openA();
    using b = openB();       // 退出时先 b 后 a（LIFO）
}

// ⚠️ 支持：Chrome 134+、Node 24+、TypeScript 5.2+
// 用途：文件句柄、数据库连接、锁、定时器
```

> 🎯 **要点**：`using` = "finally 的原生化"——资源类实现 `[Symbol.dispose]`，作用域退出自动清理，多个按 LIFO 逆序。面试手写资源管理类的标准答案。

---

## 7. ES2026：其他稳定特性

| 特性 | 说明 | 示例 |
|------|------|------|
| Math.sumPrecise() | 精确求和（Shewchuk 算法） | `Math.sumPrecise([0.1,0.2,0.3])` → 0.6 |
| Uint8Array Base64/Hex | 原生编解码 | `bytes.toBase64()`、`Uint8Array.fromHex()` |
| Error.isError() | 跨 Realm 安全判断 | `Error.isError(x)`（iframe 也正确） |
| Map.getOrInsert | 查找或插入 | `map.getOrInsert(key, '默认')` |
| JSON.rawJSON() | 原始 JSON 文本保留 | 大数字精度保护 |

```javascript
// Math.sumPrecise：解决浮点累加误差
Math.sumPrecise([0.1, 0.2, 0.3]);    // 0.6（精确）
[0.1, 0.2, 0.3].reduce((a, b) => a + b);  // 0.6000000000000001

// Map.getOrInsert：简化"查找或插入"
map.getOrInsert('user', { name: '默认' });
map.getOrInsertComputed('key', () => expensive());

// 候选特性（Stage 2-3，未定稿）：
//   Pipeline Operator |>       管道操作符
//   Pattern Matching           模式匹配
//   Import Defer               延迟模块求值
// ⚠️ 面试提醒：模式匹配/管道符仍为提案，勿当标准使用
```

---

> 🎯 **核心要点**：ES2025/2026 三大明星——**Iterator Helpers**（惰性链式迭代）、**Temporal**（日期时间革命）、**using**（资源管理原生化）；配套 Set 集合运算、Promise.try、RegExp 增强、Math.sumPrecise。2026 面试加分项：能说出"标准已批准/Stage 4/仍为提案"的状态区分。

---

**返回总览**：[00-JavaScript总览与核心概念](00-JavaScript总览与核心概念.md) | **上一篇**：[06-JavaScriptDOM与浏览器API](06-JavaScriptDOM与浏览器API.md) | **下一篇**：[08-JavaScript面试题与手写代码](08-JavaScript面试题与手写代码.md)
