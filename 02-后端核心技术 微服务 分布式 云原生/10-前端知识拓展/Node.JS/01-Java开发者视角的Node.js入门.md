# 01 - Java 开发者视角的 Node.js 入门

> 🎯 你已经会 Java 了，学 Node.js 不需要从零开始。本章把 Node.js 的核心概念逐一映射到 Java 等价物，帮你用已有的知识框架快速上手 Node.js

---

## 目录

1. [运行时对比](#1-运行时对比)
2. [语法快速映射](#2-语法快速映射)
3. [关键差异](#3-关键差异)

---

## 1. 运行时对比

| 维度 | Java (JVM) | Node.js (V8) |
|------|:---:|:---:|
| 编译方式 | 编译到字节码(.class) | **JIT 编译**（即时） |
| 类型系统 | **静态强类型** | 动态弱类型 → TypeScript |
| 并发模型 | 多线程 (Thread) | **单线程 + 事件循环** |
| 包管理 | Maven/Gradle | **npm** |
| 入口文件 | `public static void main` | `node script.js` |
| 标准库 | `java.util`, `java.io`... | `fs`, `http`, `path`... |
| 框架 | Spring Boot | Express / Fastify |

```text
Node.js = V8 引擎 + 事件循环 + 非阻塞 I/O

Java:  "为每个请求分配一个线程"
Node:  "一个线程处理所有请求，异步非阻塞"
```

## 2. 语法快速映射

### 2.1 基础语法

```javascript
// === 变量声明 ===
// Java: String name = "hello"; final int PORT = 8080;
let name = "hello";        // 可变
const PORT = 8080;         // 不可变（final）

// === 函数 ===
// Java: public String greet(String name) { return "Hi " + name; }
function greet(name) {
    return `Hi ${name}`;   // 模板字符串（Java 的 String.format）
}
const greet = (name) => `Hi ${name}`;  // 箭头函数（Lambda）

// === 数组 ===
// Java: List<String> list = Arrays.asList("a", "b", "c");
const list = ["a", "b", "c"];
list.push("d");                    // 添加
list.filter(x => x !== "b");       // 过滤（Java Stream.filter）
list.map(x => x.toUpperCase());    // 映射（Java Stream.map）

// === 对象 ===
// Java: class User { String name; int age; }
const user = {
    name: "Alice",
    age: 25,
    greet() { return `Hi, I'm ${this.name}`; }
};
console.log(user.name);              // "Alice"
const { name, age } = user;         // 解构赋值

// === 条件与循环 ===
// 几乎和 Java 一样
if (x > 0) { ... }
for (let i = 0; i < 10; i++) { ... }
for (const item of array) { ... }   // Java for-each
while (condition) { ... }
```

### 2.2 关键差异速查

| Java | Node.js | 注意 |
|------|---------|------|
| `==` 比较值（基本类型） | `===` **严格相等**（类型+值） | `5 == "5"` → true (JS) vs 编译错误(Java) |
| `equals()` 比较对象 | `===` 或 `_.isEqual()` | JS 的 `==` 会自动类型转换！ |
| `null` | `null` + `undefined` | `undefined` ≠ `null`，`undefined` = "还没赋值" |
| `ArrayList` / `HashMap` | `[]` / `{}` 字面量 | JS 的原生对象就是 map |
| `this` 指向当前实例 | `this` 取决于**调用方式** | JS 的 this 是"动态绑定" |
| `public/private` | `#` 前缀或 TypeScript | 原生 JS 没有访问控制 |

## 3. 关键差异

### 3.1 单线程事件循环

```javascript
// Node.js 的核心模型：单线程 + 非阻塞 I/O

// Java 思维：为每个请求开一个线程
// Node 思维：全部在一个线程里，但要"异步"处理

console.log("1. 开始");

// 异步操作：不阻塞后续代码
setTimeout(() => {
    console.log("3. 异步回调执行");
}, 1000);

console.log("2. 继续执行");  // 不会等 setTimeout 完成！

// 输出顺序：1 → 2 → 3（不是 1 → 3 → 2！）
```

### 3.2 模块系统

```javascript
// Java: import com.example.User;
// Node (ESM): import { User } from './user.js';
// Node (CommonJS): const { User } = require('./user');

// 导出
// user.js
export class User {}           // ESM
// 或
module.exports = { User };     // CommonJS
```

### 3.3 错误处理

```javascript
// Java: try-catch-finally (几乎相同)
try {
    const data = JSON.parse(maybeJson);
} catch (error) {
    console.error("解析失败:", error.message);
}

// 异步错误处理（Node 特有）
async function fetchData() {
    try {
        const response = await fetch(url);
        return await response.json();
    } catch (error) {
        // 网络错误 / JSON 解析错误都在这里
    }
}
```

### 3.4 JSON 一等公民

```javascript
// Java: 需要 Jackson/Gson 库
// Node.js: 原生支持 JSON（JavaScript Object Notation！）

const obj = { name: "Alice", age: 25 };

// 对象 → JSON 字符串
const json = JSON.stringify(obj);         // '{"name":"Alice","age":25}'

// JSON 字符串 → 对象
const parsed = JSON.parse(json);          // { name: "Alice", age: 25 }

// 格式化输出
console.log(JSON.stringify(obj, null, 2));
```

## 核心要点回顾

- Node.js = V8 + 事件循环 + 非阻塞 I/O
- 单线程 ≠ 慢（非阻塞 I/O 让 CPU 不空等）
- `const`/`let` 代替 `var`；`===` 代替 `==`
- 异步是核心：`async/await` > `Promise` > 回调
- JSON 是原生能力，不像 Java 需要三方库
- 动态类型 → 用 TypeScript 找回类型安全

## 参考资料

1. Node.js 官方文档 — nodejs.org
2. MDN JavaScript 教程 — developer.mozilla.org
