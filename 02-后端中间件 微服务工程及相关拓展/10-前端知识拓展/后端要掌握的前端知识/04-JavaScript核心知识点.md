# JavaScript 核心知识点
> 面向后端开发者的 JavaScript 快速入门，聚焦与 Java 的差异以及现代 JS 必须掌握的核心概念

## 目录

1. [JS vs Java：核心差异](#1-js-vs-java核心差异)
2. [变量声明：var / let / const](#2-变量声明var--let--const)
3. [数据类型](#3-数据类型)
4. [== 与 ===](#4--与-)
5. [函数：声明、表达式、箭头函数](#5-函数声明表达式箭头函数)
6. [模板字面量](#6-模板字面量)
7. [解构赋值](#7-解构赋值)
8. [展开与剩余运算符](#8-展开与剩余运算符)
9. [数组方法（Java Stream 对照表）](#9-数组方法java-stream-对照表)
10. [对象操作](#10-对象操作)
11. [Promise 与 async/await](#11-promise-与-asyncawait)
12. [事件循环（Event Loop）](#12-事件循环event-loop)
13. [模块系统](#13-模块系统)
14. [错误处理](#14-错误处理)
15. [JSON 序列化](#15-json-序列化)
16. [浏览器存储](#16-浏览器存储)
17. [综合示例](#17-综合示例)

---

## 1. JS vs Java：核心差异

| 对比维度 | Java | JavaScript |
|---------|------|-----------|
| 类型系统 | 静态强类型 | 动态弱类型 |
| 编译方式 | 编译为字节码 | 解释执行（JIT 编译） |
| 面向对象 | 基于类（Class） | 基于原型（Prototype） |
| 函数 | 二等公民（方法必须属于类） | 一等公民（可赋值给变量、作为参数传递、作为返回值） |
| 并发模型 | 多线程（共享内存 + 锁） | 单线程 + 事件循环（非阻塞 I/O） |
| 运行环境 | JVM | 浏览器 / Node.js |
| 文件扩展 | `.java` → `.class` | `.js` / `.mjs` |
| null 处理 | `NullPointerException`（受检） | `null` 和 `undefined` 两种空值 |
| 继承方式 | 类继承（extends） | 原型链继承 |
| 访问修饰符 | `public` / `protected` / `private` | `#` 私有字段（ES2022）、闭包模拟 |

### 从 Java 迁移到 JS 的心理模型

```
Java:   类 → 对象 → 方法调用
JS:     对象 → 原型 → 函数调用

Java:   int x = 1;  String s = "hello";
JS:     let x = 1;  let s = "hello";  // 类型随时可变

Java:   多线程 + synchronized
JS:     单线程 + async/await + Promise
```

> 💡 对于后端开发者，**最重要的 JS 概念**按优先级排序：async/await > Promise > 箭头函数 > 解构 > 数组方法(map/filter/reduce) > 模块化。

---

## 2. 变量声明：var / let / const

| 关键字 | 作用域 | 提升(Hoisting) | 重复声明 | 重新赋值 | 建议 |
|--------|--------|----------------|----------|---------|------|
| `var` | 函数作用域 | 变量提升（初始化为 undefined） | 允许 | 允许 | ❌ 不要使用 |
| `let` | 块作用域 | 提升但不初始化（暂时性死区） | 不允许 | 允许 | ✅ 可变变量 |
| `const` | 块作用域 | 提升但不初始化（暂时性死区） | 不允许 | 不允许 | ✅ 首选（不变优先） |

### 变量提升（Hoisting）

```javascript
// var 的变量提升
console.log(a);  // undefined（不报错！因为 var a 被提升）
var a = 10;

// 等价于：
var a;
console.log(a);  // undefined
a = 10;

// let 的暂时性死区（Temporal Dead Zone）
console.log(b);  // ❌ ReferenceError: Cannot access 'b' before initialization
let b = 10;
```

### 作用域对比

```javascript
// 函数作用域 vs 块作用域
if (true) {
    var x = 1;   // 函数作用域——会泄漏到外面
    let y = 2;   // 块作用域——仅在 {} 内有效
    const z = 3; // 块作用域——仅在 {} 内有效
}
console.log(x);  // 1
console.log(y);  // ❌ ReferenceError

// const 对象的值可变
const obj = { name: '张三' };
obj.name = '李四';      // ✅ 属性可以修改
obj.age = 25;           // ✅ 可以添加新属性
// obj = { name: '王五' }  // ❌ 不能重新赋值
```

> 🎯 **后端开发者的原则**：总是用 `const` 声明变量，只有确实需要重新赋值时才用 `let`。永远不要用 `var`。

---

## 3. 数据类型

### 基本类型（Primitive Types）

| 类型 | 示例 | typeof 返回 | 说明 |
|------|------|------------|------|
| `Number` | `42`, `3.14`, `NaN`, `Infinity` | `"number"` | 没有 int/long/float 区分，都是 64 位浮点 |
| `String` | `"hello"`, `'hello'`, `` `hello` `` | `"string"` | 单引号/双引号/反引号均可 |
| `Boolean` | `true`, `false` | `"boolean"` | - |
| `null` | `null` | `"object"`（语言 bug） | 显式空值 |
| `undefined` | `undefined` | `"undefined"` | 声明未赋值 |
| `Symbol` | `Symbol('id')` | `"symbol"` | 唯一标识符 |
| `BigInt` | `9007199254740991n` | `"bigint"` | 超出 Number 安全范围的整数 |

### 引用类型（Object）

```javascript
const obj = { name: '张三' };     // 普通对象
const arr = [1, 2, 3];            // 数组
const fn = function() {};         // 函数
const date = new Date();          // 日期
const map = new Map();            // Map
const set = new Set();            // Set
```

### 类型转换常见陷阱

```javascript
// 加法运算符的陷阱
console.log(1 + '2');        // "12"（数字拼串）
console.log(1 - '2');        // -1（减号触发数值转换）
console.log('5' * 2);        // 10
console.log('a' * 1);        // NaN

// 虚假值（Falsy Values）
// 以下 6 个值在 if 判断中为 false：
false, 0, '' (空字符串), null, undefined, NaN

// 判空推荐写法
const name = user?.name ?? '默认用户';  // 可选链 + 空值合并
```

> ⚠️ `NaN` 不等于任何值，包括它自己：`NaN === NaN` → `false`。判断使用 `Number.isNaN(x)`。

---

## 4. == 与 ===

```javascript
// 绝对原则：始终使用 ===，永远不要用 ==

// == 会做类型转换（带来各种诡异结果）
console.log(1 == '1');           // true（自动转换类型）
console.log(0 == false);         // true
console.log('' == false);        // true
console.log(null == undefined);  // true
console.log([] == false);        // true

// === 不会做类型转换
console.log(1 === '1');          // false
console.log(0 === false);        // false
console.log(null === undefined); // false
```

> 💡 面试可能会问 `==` 的规则，但实际开发中**没有理由使用 `==`**。几乎所有现代 ESLint 配置都禁止使用 `==`。

---

## 5. 函数：声明、表达式、箭头函数

### 三种定义方式

```javascript
// 1. 函数声明（Function Declaration）—— 可提升
function add(a, b) {
    return a + b;
}

// 2. 函数表达式（Function Expression）—— 不可提升
const subtract = function(a, b) {
    return a - b;
};

// 3. 箭头函数（Arrow Function）—— ES6 首选
const multiply = (a, b) => a * b;
const square = x => x * x;         // 单个参数可省略括号
const noop = () => {};             // 无参数不能省略括号
```

### this 绑定差异

```javascript
// 这是 JS 中最容易让后端开发者困惑的点

// 普通函数：this 由调用方式决定
const obj1 = {
    name: '张三',
    greet: function() {
        console.log(this.name);  // this → obj1
    }
};
obj1.greet();  // "张三"

const greetFn = obj1.greet;
greetFn();     // undefined（this → global/window，严格模式为 undefined）

// 箭头函数：this 由定义位置决定（词法作用域）
const obj2 = {
    name: '李四',
    greet: () => {
        console.log(this.name);  // this 继承自外层作用域（此处为全局）
    }
};
obj2.greet();  // undefined（箭头函数的 this 不会指向 obj2）

// 箭头函数的典型正确用法——回调
class Timer {
    constructor() {
        this.seconds = 0;
        // ✅ 箭头函数捕获外部 this
        setInterval(() => {
            this.seconds++;  // this 指向 Timer 实例
        }, 1000);
        
        // ❌ 普通函数会丢失 this
        setInterval(function() {
            this.seconds++;  // this 指向全局对象
        }, 1000);
    }
}
```

> 🎯 **箭头函数使用原则**：
> - 需要 `this` 指向当前对象的方法 → 用 `function` 声明或类方法
> - 所有回调、闭包 → 用箭头函数
> - 不需要 `this` 的工具函数 → 用箭头函数

---

## 6. 模板字面量

```javascript
const name = '张三';
const age = 25;

// 旧方式（字符串拼接）
const oldWay = '我叫 ' + name + '，今年 ' + age + ' 岁';

// ES6 方式（模板字面量，反引号）
const newWay = `我叫 ${name}，今年 ${age} 岁`;

// 多行字符串
const html = `
<div>
    <h1>${name}</h1>
    <p>年龄：${age}</p>
</div>
`;

// 嵌入表达式
const message = `订单总额：${(price * quantity * 1.13).toFixed(2)} 元`;
```

> 💡 后端开发者使用模板字面量的场景：拼接 SQL（警惕注入）、拼接 URL、生成 HTML 片段。

---

## 7. 解构赋值

### 对象解构

```javascript
const user = { name: '张三', age: 25, email: 'zhangsan@example.com' };

// 基本解构
const { name, age } = user;
console.log(name);  // "张三"
console.log(age);   // 25

// 重命名
const { name: userName, email: userEmail } = user;

// 默认值
const { phone = '未填写' } = user;

// 嵌套解构
const response = { data: { id: 1, title: '文章' }, status: 200 };
const { data: { id, title }, status } = response;

// 函数参数解构（后端 API 常见）
function processUser({ name, age, email = '未提供' }) {
    console.log(`处理用户：${name}`);
}
processUser(user);
```

### 数组解构

```javascript
const colors = ['red', 'green', 'blue', 'yellow'];

const [first, second] = colors;         // first='red', second='green'
const [primary, , tertiary] = colors;   // 跳过第二个：primary='red', tertiary='blue'
const [head, ...tail] = colors;         // head='red', tail=['green','blue','yellow']

// 交换变量（经典用法）
let a = 1, b = 2;
[a, b] = [b, a];  // a=2, b=1
```

---

## 8. 展开与剩余运算符

### 展开运算符（Spread）`...`

```javascript
// 数组合并
const arr1 = [1, 2, 3];
const arr2 = [4, 5, 6];
const merged = [...arr1, ...arr2];  // [1,2,3,4,5,6]

// 对象合并（React/Vue 中最常见）
const base = { name: '张三', age: 25 };
const updated = { ...base, email: 'new@email.com' };  // 添加属性
const overridden = { ...base, age: 30 };               // 覆盖属性

// 复制（浅拷贝）
const copy = { ...base };

// 函数调用
const numbers = [10, 20, 30];
Math.max(...numbers);  // 30
```

### 剩余参数（Rest）`...`

```javascript
// 收集剩余参数
function sum(...numbers) {
    return numbers.reduce((total, n) => total + n, 0);
}
sum(1, 2, 3, 4);  // 10

// 解构中的剩余模式
const { name, ...rest } = { name: '张三', age: 25, email: 'z@example.com' };
console.log(rest);  // { age: 25, email: 'z@example.com' }
```

---

## 9. 数组方法（Java Stream 对照表）

```javascript
const numbers = [1, 2, 3, 4, 5, 6];
const users = [
    { name: '张三', age: 25, active: true },
    { name: '李四', age: 17, active: true },
    { name: '王五', age: 30, active: false }
];
```

| JavaScript 方法 | Java Stream 对应 | 用途 | 示例 |
|----------------|-----------------|------|------|
| `arr.map(fn)` | `.map()` | 转换每个元素 | `numbers.map(n => n * 2)` → `[2,4,6,8,10,12]` |
| `arr.filter(fn)` | `.filter()` | 筛选元素 | `numbers.filter(n => n > 3)` → `[4,5,6]` |
| `arr.reduce(fn, init)` | `.reduce()` | 归约为单一值 | `numbers.reduce((sum, n) => sum + n, 0)` → `21` |
| `arr.find(fn)` | `.findFirst()` | 第一个匹配元素 | `users.find(u => u.age > 18)` → `{ name:'张三', age:25 }` |
| `arr.some(fn)` | `.anyMatch()` | 任一匹配 | `numbers.some(n => n > 5)` → `true` |
| `arr.every(fn)` | `.allMatch()` | 全部匹配 | `numbers.every(n => n > 0)` → `true` |
| `arr.includes(v)` | `.anyMatch(x -> x.equals(v))` | 包含元素 | `numbers.includes(3)` → `true` |
| `arr.sort(fn)` | `.sorted()` | 排序 | `numbers.sort((a,b) => a - b)` |
| `arr.forEach(fn)` | `.forEach()` | 遍历（无返回值） | `users.forEach(u => console.log(u.name))` |
| `arr.flatMap(fn)` | `.flatMap()` | 展平映射 | `[[1,2],[3,4]].flatMap(x => x)` → `[1,2,3,4]` |

### 链式调用示例

```javascript
// 找出所有活跃用户中年龄大于 18 的用户名
const activeAdultNames = users
    .filter(u => u.active)                    // 先筛选活跃用户
    .filter(u => u.age >= 18)                 // 再筛选成年人
    .map(u => u.name);                        // 提取用户名

// 等价 Java Stream：
// users.stream()
//     .filter(User::isActive)
//     .filter(u -> u.getAge() >= 18)
//     .map(User::getName)
//     .collect(Collectors.toList());
```

> 💡 熟悉 Java Stream 的后端开发者可以很自然地过渡到 JS 数组方法。唯一差异：JS 的 `map` 不要求返回的类型一致，而 Java 的 `map` 是类型安全的。

---

## 10. 对象操作

### 常用 API

```javascript
const user = { name: '张三', age: 25, email: 'z@example.com' };

// 获取键 / 值 / 键值对数组
console.log(Object.keys(user));     // ["name", "age", "email"]
console.log(Object.values(user));   // ["张三", 25, "z@example.com"]
console.log(Object.entries(user));  // [["name","张三"], ["age",25], ["email","z@example.com"]]

// 遍历对象
for (const [key, value] of Object.entries(user)) {
    console.log(`${key}: ${value}`);
}

// 对象合并
const defaults = { theme: 'light', lang: 'zh-CN' };
const settings = { lang: 'en' };
const merged = Object.assign({}, defaults, settings);  // { theme: 'light', lang: 'en' }
// 推荐用展开运算符替代
const merged2 = { ...defaults, ...settings };
```

### 可选链与空值合并

```javascript
const user = {
    name: '张三',
    profile: {
        // address 不存在
    }
};

// 旧写法（层层判空）
const city = user && user.profile && user.profile.address && user.profile.address.city;

// ✅ 可选链（Optional Chaining）?.   —— ES2020
const city2 = user?.profile?.address?.city;  // undefined（不报错）

// 空值合并（Nullish Coalescing）??   —— ES2020
// 仅在左侧为 null/undefined 时取右侧默认值（与 || 不同：|| 对 '' 和 0 也会取默认值）
const displayName = user.name ?? '匿名用户';
const count = 0 ?? 100;     // 0（?? 只对 null/undefined 生效）
const count2 = 0 || 100;    // 100（|| 对所有 falsy 值生效）

// 方法调用
const result = user.save?.();  // 如果 save 不存在则不调用
```

> 🎯 `?.` 和 `??` 是后端开发者在 JS 中最需要的语法糖，能将判空代码减少 80%。

---

## 11. Promise 与 async/await

> 这是后端开发者最重要的 JS 概念——现代 JS 的异步操作全部基于 Promise。

### 11.1 Promise 基础

```javascript
// Promise 三种状态：pending → fulfilled / rejected

// 创建 Promise
const fetchUser = new Promise((resolve, reject) => {
    setTimeout(() => {
        const success = true;
        if (success) {
            resolve({ id: 1, name: '张三' });
        } else {
            reject(new Error('获取用户失败'));
        }
    }, 1000);
});

// 使用 Promise 链
fetchUser
    .then(user => {
        console.log('用户:', user);
        return fetchOrders(user.id);  // 返回另一个 Promise
    })
    .then(orders => {
        console.log('订单:', orders);
    })
    .catch(error => {
        console.error('错误:', error.message);
    })
    .finally(() => {
        console.log('请求完成（无论成功失败）');
    });
```

### 11.2 async/await（推荐写法）

```javascript
// async 函数总是返回一个 Promise
async function getUserOrders(userId) {
    try {
        const user = await fetchUser(userId);           // 等待 Promise 解决
        const orders = await fetchOrders(user.id);      // 串行
        console.log(`${user.name} 的订单:`, orders);
        return orders;
    } catch (error) {
        console.error('获取用户订单失败:', error);
        throw error;  // 继续向上抛出
    }
}

// 调用
getUserOrders(1)
    .then(orders => console.log('完成'))
    .catch(err => console.error('处理失败:', err));
```

### 11.3 并发执行

```javascript
// 串行 vs 并行

// ❌ 串行（不必要的等待）
async function serial() {
    const user = await fetchUser(1);
    const orders = await fetchOrders(1);     // 等 user 返回后才开始
    const profile = await fetchProfile(1);   // 等 orders 返回后才开始
}

// ✅ 并行
async function parallel() {
    const [user, orders, profile] = await Promise.all([
        fetchUser(1),          // 三个请求同时发出
        fetchOrders(1),
        fetchProfile(1)
    ]);
}

// Promise.allSettled —— 不关心某个失败，等待所有完成
const results = await Promise.allSettled([
    fetchUser(1),
    fetchOrders(1),
    fetchProfile(1)
]);
results.forEach(r => {
    if (r.status === 'fulfilled') {
        console.log('成功:', r.value);
    } else {
        console.log('失败:', r.reason);  // 不抛出异常
    }
});

// Promise.race —— 最快的一个返回即结束
const timeout = new Promise((_, reject) =>
    setTimeout(() => reject(new Error('请求超时')), 5000)
);
const result = await Promise.race([fetchUser(1), timeout]);
```

> ⚠️ 后端开发者常见错误：忘记 `await` 会导致 Promise 在后台静默执行，后续代码拿不到结果。未 catch 的 Promise 拒绝会触发 `unhandledRejection` 事件。

---

## 12. 事件循环（Event Loop）

> 理解事件循环是理解 JS 异步行为的关键。JS 是单线程的，靠事件循环实现非阻塞 I/O。

### 执行顺序

```
① Call Stack（调用栈）：同步代码依次执行
② 遇到异步操作（setTimeout、fetch、Promise）→ 交给 Web API 处理，不会阻塞栈
③ 异步操作完成后：
   - 宏任务（MacroTask）：setTimeout、setInterval、I/O → 进入 Task Queue
   - 微任务（MicroTask）：Promise.then/catch/finally、MutationObserver → 进入 Microtask Queue
④ 微任务队列优先级高于宏任务队列
```

### 执行顺序示例

```javascript
console.log('1');  // 同步

setTimeout(() => {
    console.log('2');  // 宏任务
}, 0);

Promise.resolve().then(() => {
    console.log('3');  // 微任务
});

console.log('4');  // 同步

// 输出顺序：1 → 4 → 3 → 2
// 为什么？同步先执行 → 微任务（Promise）优先于宏任务（setTimeout）
```

### 完整的 Event Loop 一次循环

```
① 执行 Call Stack 中所有同步代码
② 清空 Microtask Queue（微任务队列）
③ 从 Task Queue 中取出一个宏任务执行
④ 再次清空 Microtask Queue（微任务可以追加新的微任务）
⑤ UI 渲染（浏览器环境）
⑥ 回到 ③
```

```javascript
// 经典面试题
Promise.resolve().then(() => {
    console.log('微任务1');
    setTimeout(() => console.log('宏任务2'), 0);
});

setTimeout(() => {
    console.log('宏任务1');
    Promise.resolve().then(() => console.log('微任务2'));
}, 0);

// 输出：微任务1 → 宏任务1 → 微任务2 → 宏任务2
```

> 💡 对于后端开发者，事件循环的实用价值在于理解 `Promise` 的回调总是先于 `setTimeout` 执行，以及为什么 `await` 之后的代码会在微任务中执行。

---

## 13. 模块系统

### ES6 模块（ESM）—— 现代标准

```javascript
// 📁 math.js —— 导出
export const PI = 3.14159;
export function add(a, b) { return a + b; }
export default class Calculator { ... }

// 📁 app.js —— 导入
import Calculator, { PI, add as sum } from './math.js';
import * as MathUtils from './math.js';  // 全部导入
```

### CommonJS（Node.js 传统方式）

```javascript
// 📁 math.js —— 导出
const PI = 3.14159;
function add(a, b) { return a + b; }
module.exports = { PI, add };
module.exports.default = Calculator;

// 📁 app.js —— 导入
const { PI, add } = require('./math.js');
```

| 特性 | ES6 Module (ESM) | CommonJS (CJS) |
|------|------------------|----------------|
| 语法 | `import` / `export` | `require()` / `module.exports` |
| 加载时机 | 编译时（静态分析） | 运行时（动态加载） |
| 异步 | 支持（浏览器可用） | 同步（仅 Node.js） |
| Tree Shaking | 支持 | 不支持 |
| 文件扩展 | `.mjs` 或 `"type": "module"` | `.cjs` 或默认 |

> 💡 浏览器环境使用 ES6 模块，Node.js 环境两者并存，新项目建议使用 ESM。

---

## 14. 错误处理

```javascript
// 基本 try-catch
try {
    const data = JSON.parse(userInput);  // 可能抛出异常
    process(data);
} catch (error) {
    if (error instanceof SyntaxError) {
        console.error('JSON 格式错误:', error.message);
    } else if (error instanceof TypeError) {
        console.error('类型错误:', error.message);
    } else {
        console.error('未知错误:', error);
    }
} finally {
    cleanup();  // 无论是否异常都会执行
}

// 自定义错误
class ValidationError extends Error {
    constructor(message, field) {
        super(message);
        this.name = 'ValidationError';
        this.field = field;
    }
}

function validate(data) {
    if (!data.name) {
        throw new ValidationError('名称不能为空', 'name');
    }
}

// async 中的错误处理
async function fetchData() {
    try {
        const res = await fetch('/api/data');
        if (!res.ok) {
            throw new Error(`HTTP ${res.status}: ${res.statusText}`);
        }
        return await res.json();
    } catch (error) {
        console.error('请求失败:', error);
        throw error;  // 重新抛出
    }
}
```

> ⚠️ 注意 JSON.parse 包裹的是无效 JSON 字符串时会抛出异常，应始终放在 try-catch 中。另外，`fetch` 在遇到 404/500 时**不会** reject（见本系列后续章节）。

---

## 15. JSON 序列化

```javascript
const user = { name: '张三', age: 25, createdAt: new Date() };

// 对象 → JSON 字符串
const json = JSON.stringify(user);
console.log(json);  // {"name":"张三","age":25,"createdAt":"2024-01-15T..."}

// 格式化输出
console.log(JSON.stringify(user, null, 2));
// {
//   "name": "张三",
//   "age": 25,
//   "createdAt": "2024-01-15T..."
// }

// JSON 字符串 → 对象
const parsed = JSON.parse(json);
console.log(parsed.name);  // "张三"

// 安全解析（防止传入非法 JSON 导致页面崩溃）
function safeJsonParse(str, fallback = null) {
    try {
        return JSON.parse(str);
    } catch {
        return fallback;
    }
}

// 特殊类型处理
const data = {
    name: '张三',
    score: NaN,
    count: Infinity,
    nested: undefined,     // JSON.stringify 会删除 undefined
    symbol: Symbol('test') // JSON.stringify 会删除 Symbol
};
console.log(JSON.stringify(data));  // {"name":"张三","score":null,"count":null}
```

---

## 16. 浏览器存储

| 存储方式 | 容量 | 持久性 | 可访问 | 后端对应 |
|---------|------|--------|--------|---------|
| `localStorage` | ~5-10MB | 永久（需手动清除） | 同域名所有页面 | 类似本地文件 |
| `sessionStorage` | ~5-10MB | 标签页关闭即清除 | 当前标签页 | 类似内存缓存 |
| `Cookie` | ~4KB | 可设置过期时间 | 同域名请求自动携带 | HTTP 传输 |
| `IndexedDB` | 大量 | 永久 | 同域名 | 浏览器端数据库 |

```javascript
// localStorage（最常用的前端存储）
localStorage.setItem('token', 'eyJhbGciOi...');
const token = localStorage.getItem('token');
localStorage.removeItem('token');
localStorage.clear();

// 存储对象需要序列化
const user = { name: '张三', age: 25 };
localStorage.setItem('user', JSON.stringify(user));
const savedUser = JSON.parse(localStorage.getItem('user'));

// sessionStorage（API 相同，但仅当前标签页有效）
sessionStorage.setItem('tempData', '当前页面临时数据');
```

> 💡 后端开发者注意：JWT token 通常存在 `localStorage`（SPA 场景）或 `HttpOnly Cookie`（传统场景）。存在 `localStorage` 的 token 在每次请求时通过 JavaScript 读取并放至 `Authorization` 头。

---

## 17. 综合示例

一个完整的异步数据获取和处理函数，涵盖本文大多数知识点：

```javascript
// 📁 userService.js

import axios from 'axios';  // 或使用 fetch

const API_BASE = 'https://api.example.com';

/**
 * 获取用户及其订单信息
 * 展示：async/await、解构、可选链、数组方法、错误处理、Promise.all
 */
export async function getUserWithOrders(userId) {
    try {
        // 并行请求：同时获取用户信息和订单列表
        const [userResponse, ordersResponse] = await Promise.all([
            axios.get(`${API_BASE}/users/${userId}`),
            axios.get(`${API_BASE}/users/${userId}/orders`)
        ]);

        const user = userResponse.data;
        const orders = ordersResponse.data;

        // 对象解构 + 重命名
        const { name, email, ...otherInfo } = user;

        // 数组方法链式调用
        const activeOrders = orders
            .filter(order => order.status === 'completed')
            .map(order => ({
                id: order.id,
                total: order.totalAmount,
                // 可选链处理可能缺失的字段
                itemCount: order.items?.length ?? 0,
                date: new Date(order.createdAt).toLocaleDateString('zh-CN')
            }))
            .sort((a, b) => b.date - a.date);  // 按日期降序

        // 使用 reduce 计算总金额
        const totalSpent = activeOrders.reduce(
            (sum, order) => sum + order.total, 0
        );

        // 返回组合结果
        return {
            userId: user.id,
            name,
            email,
            otherInfo,
            orderCount: activeOrders.length,
            totalSpent: totalSpent.toFixed(2),
            recentOrders: activeOrders.slice(0, 5)  // 最近 5 条
        };

    } catch (error) {
        // 区分不同类型的错误
        if (error.response) {
            // HTTP 错误（4xx/5xx）
            console.error(`API 错误 [${error.response.status}]:`, error.response.data);
            throw new Error(`请求失败: ${error.response.status}`);
        } else if (error.request) {
            // 网络错误（无响应）
            console.error('网络错误: 无法连接到服务器');
            throw new Error('网络连接失败，请检查网络');
        } else {
            console.error('未知错误:', error);
            throw error;
        }
    }
}

// 📁 app.js —— 使用示例

(async () => {
    try {
        const result = await getUserWithOrders(1);

        // 模板字面量输出结果
        console.log(`用户: ${result.name}（${result.email}）`);
        console.log(`完成订单数: ${result.orderCount}`);
        console.log(`总消费: ¥${result.totalSpent}`);

        if (result.recentOrders.length > 0) {
            console.log('最近订单:');
            result.recentOrders.forEach((order, index) => {
                console.log(`  ${index + 1}. #${order.id} ¥${order.total} (${order.date})`);
            });
        }

    } catch (error) {
        console.error('程序执行失败:', error.message);
        // 可在此处展示错误 UI
    }
})();
```

> 🎯 **总结：后端开发者掌握 JS 的捷径**
> 1. 记住 JS 不同于 Java 的三大特性：动态类型、一等函数、事件循环
> 2. 熟练掌握 `async/await` + `Promise.all`（这是前后端交互的核心）
> 3. 用 `const` 代替 `var`，用 `===` 代替 `==`
> 4. 熟悉 `map/filter/reduce`（和 Java Stream 思维方式一致）
> 5. 善用 `?.` 和 `??` 简化判空逻辑
> 6. 理解模块化（`import/export`）和错误处理（`try-catch`）
