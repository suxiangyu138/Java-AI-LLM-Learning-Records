# 00 - JavaScript 总览与核心概念

> 定位：JS 知识体系入口——语言定位、执行模型、版本演进、类型体系、模块导航

## 📚 目录

1. [JavaScript 是什么](#1-javascript-是什么)
2. [执行模型：单线程与事件循环](#2-执行模型单线程与事件循环)
3. [版本演进：ES5 → ES2026](#3-版本演进es5--es2026)
4. [数据类型体系](#4-数据类型体系)
5. [模块导航](#5-模块导航)

---

## 1. JavaScript 是什么

```
JavaScript = 前端三件套的行为层（HTML 结构 / CSS 表现 / JS 行为）

三重身份：
  浏览器端：DOM/BOM 操作、交互、动画
  服务端：Node.js（后端、构建工具）
  全栈：前后端同构（Next.js 等）

⚠️ 易混淆概念：
  JavaScript vs ECMAScript：ECMAScript 是标准，JS 是它的实现
  JavaScript vs TypeScript：TS = JS + 类型系统（超集）

⚠️ 面试必答：
"JS 是 ECMAScript 标准的实现——
 语言核心（语法/类型/异步）由标准定义，
 宿主环境（浏览器/Node）提供 API（DOM/fs）。"
```

---

## 2. 执行模型：单线程与事件循环

### 2.1 单线程的本质

```
JS 是单线程语言（一个调用栈）
  → 不存在"多线程数据竞争"
  → 耗时操作必须异步（不阻塞主线程）

⚠️ 面试必答：
"JS 单线程 = 一个调用栈——
 异步靠事件循环 + 任务队列实现
 并发，而不是多线程。"
```

### 2.2 事件循环（Event Loop）

```
执行模型（浏览器）：
  ① 执行同步代码（调用栈）
  ② 遇到异步 → 交给 Web API（定时器/网络）
  ③ 回调进任务队列
  ④ 栈空 → 从队列取任务执行

任务优先级：
  微任务（Promise.then/MutationObserver）> 宏任务（setTimeout/事件）
  每轮宏任务后清空全部微任务

⚠️ 面试必答：
"事件循环核心：微任务先于宏任务、
 每轮宏任务后清空微任务队列——
 这是 async/await 与 setTimeout
 执行顺序的底层解释。"
```

```javascript
// 经典输出顺序题：
console.log('1');                          // 同步
setTimeout(() => console.log('2'), 0);     // 宏任务
Promise.resolve().then(() => console.log('3'));  // 微任务
console.log('4');                          // 同步
// 输出：1 4 3 2（微任务 3 先于宏任务 2）
```

---

## 3. 版本演进：ES5 → ES2026

| 版本 | 年份 | 核心特性 |
|------|:---:|---------|
| ES5 | 2009 | 严格模式、JSON、数组方法 |
| ES6 (ES2015) | 2015 | **箭头函数/类/模块/let-const/模板字符串/解构/Promise** |
| ES2016-2019 | 2016-19 | async/await、扩展运算符、可选链等 |
| ES2020 | 2020 | 可选链 ?.、空值合并 ??、BigInt、globalThis |
| ES2021-2024 | 2021-24 | 逻辑赋值、结构化克隆、数组 findLast |
| ES2025 | 2025 | Iterator Helpers、Set 方法、Promise.try、RegExp 增强 |
| ES2026 | 2026（6 月发布） | **Temporal、using 声明、Math.sumPrecise** |

```
⚠️ 面试必答：
"ES6 是分水岭（2015）——之后的特性
 按年演进；ES2025/2026 的最新特性
 见本体系 07 篇。"
```

---

## 4. 数据类型体系

### 4.1 七种原始类型 + 对象

```javascript
// 原始类型（7 种，不可变，按值传递）
undefined   // 未定义
null        // 空值
boolean     // true/false
number      // 双精度浮点（含 NaN/Infinity）
string      // 字符串（不可变）
symbol      // 唯一标识（ES6）
bigint      // 大整数（ES2020，1n）

// 引用类型
object      // 对象/数组/函数（按引用传递）

// ⚠️ typeof 的坑
typeof null        // "object"（历史遗留 bug）
typeof function(){} // "function"
typeof NaN         // "number"
```

### 4.2 类型转换

```javascript
// 隐式转换的坑（面试高频）
'1' + 1        // '11'（字符串拼接优先）
'1' - 1        // 0（算术运算转数字）
[] + []        // ''（空数组转空串）
[] + {}        // '[object Object]'
0 == ''        // true（宽松相等）
0 === ''       // false（严格相等 ✅ 永远用 ===）

// 显式转换
Number('42')   // 42
String(42)     // '42'
Boolean(0)     // false（0/''/null/undefined/NaN 为假）
parseInt('42px') // 42
```

> 🎯 **要点**：永远用 `===`（严格相等）；了解隐式转换规则（拼接 vs 算术）；`typeof null` 是历史 bug 要会解释。

---

## 5. 模块导航

| 序号 | 模块 | 核心内容 | 定位 |
|------|------|---------|------|
| 00 | [本总览](00-JavaScript总览与核心概念.md) | 执行模型、版本、类型 | 入口 |
| 01 | [基础语法与流程控制](01-JavaScript基础语法与流程控制.md) | 变量、运算符、条件、循环、解构 | 基础 |
| 02 | [函数与作用域](02-JavaScript函数与作用域.md) | 函数形态、闭包、this、作用域链 | 核心 |
| 03 | [对象与原型链](03-JavaScript对象与原型链.md) | 对象、原型链、class、继承 | 核心 |
| 04 | [异步编程](04-JavaScript异步编程.md) | 回调、Promise、async/await、事件循环 | 核心 |
| 05 | [ES6+ 进阶特性](05-JavaScriptES6进阶特性.md) | 模块、Symbol、迭代器、生成器、代理 | 进阶 |
| 06 | [DOM 与浏览器 API](06-JavaScriptDOM与浏览器API.md) | DOM 操作、事件、存储、Fetch、Web API | 实战 |
| 07 | [ES2025/2026 新特性](07-JavaScriptES2025与ES2026新特性.md) | Iterator Helpers、Temporal、using 等 | 前沿 |
| 08 | [面试题与手写代码](08-JavaScript面试题与手写代码.md) | 高频面试题、手写实现 | 面试 |

---

> 🎯 **本体系学习建议**：JS 五条主线——**类型**（00）、**作用域与闭包**（02）、**原型链**（03）、**异步**（04）、**ES6+**（05）。面试 80% 的问题集中在这五块，07 新特性是 2026 加分项。

---

**下一篇**：[01-JavaScript基础语法与流程控制](01-JavaScript基础语法与流程控制.md)
