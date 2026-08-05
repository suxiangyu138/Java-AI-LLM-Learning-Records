# 02 - JavaScript 函数与作用域

> 定位：函数形态、作用域与闭包、this 指向、箭头函数、高阶函数——JS 面试第一核心域

## 📚 目录

1. [函数形态](#1-函数形态)
2. [作用域链与提升](#2-作用域链与提升)
3. [闭包](#3-闭包)
4. [this 指向全解](#4-this-指向全解)
5. [箭头函数](#5-箭头函数)
6. [高阶函数与柯里化](#6-高阶函数与柯里化)

---

## 1. 函数形态

### 1.1 五种定义方式

```javascript
// ① 函数声明（提升 ✅）
function add(a, b) { return a + b; }

// ② 函数表达式（不提升）
const add = function (a, b) { return a + b; };

// ③ 箭头函数
const add = (a, b) => a + b;

// ④ 立即执行 IIFE
(function () { console.log('立即执行'); })();

// ⑤ 构造函数（⚠️ 基本不用，用 class 替代）
const fn = new Function('a', 'b', 'return a + b');
```

### 1.2 参数进阶

```javascript
// 默认参数
function greet(name = '匿名', times = 1) { }

// 剩余参数（...rest，真数组）
function sum(...nums) { return nums.reduce((a, b) => a + b, 0); }

// 展开调用
Math.max(...[1, 3, 2]);

// arguments（⚠️ 箭头函数没有，用 rest 替代）
function log() { console.log(arguments); }
```

---

## 2. 作用域链与提升

### 2.1 作用域类型

```
JS 作用域（ES6+）：
  全局作用域
  函数作用域（function 内）
  块级作用域（let/const 的 {} 内）

作用域链：内层找不到 → 向外层找 → 全局
  变量查找是"词法作用域"（写代码的位置决定，不是调用位置！）

⚠️ 面试必答：
"作用域链按'定义位置'而非'调用位置'查找——
 词法作用域是闭包与 this 理解的基础。"
```

### 2.2 变量提升与 TDZ

```javascript
// 函数声明提升（整个函数可用）
console.log(fn());        // ✅ 函数已提升
function fn() { return 1; }

// var 提升（值为 undefined）
console.log(x);           // undefined
var x = 1;

// let/const 暂时性死区（TDZ）
console.log(y);           // ❌ ReferenceError
let y = 1;

// 函数与变量的提升优先级：函数声明 > var 声明
```

---

## 3. 闭包

### 3.1 什么是闭包

```javascript
// 闭包 = 函数 + 其词法作用域（记住外层变量）
function createCounter() {
    let count = 0;                    // 外层变量被"记住"
    return function () {
        return ++count;               // 访问外层作用域
    };
}
const counter = createCounter();
counter();    // 1
counter();    // 2
// count 不会被回收——闭包持有它的引用
```

### 3.2 闭包的应用

```javascript
// ① 私有变量（模块模式）
const module = (function () {
    let privateVar = 0;               // 外部不可访问
    return {
        increment: () => ++privateVar,
        get: () => privateVar,
    };
})();

// ② 防抖/节流（面试手写高频）
function debounce(fn, delay) {
    let timer = null;
    return function (...args) {       // ⚠️ 闭包持有 timer
        clearTimeout(timer);
        timer = setTimeout(() => fn.apply(this, args), delay);
    };
}

// ③ 经典循环陷阱（var + 闭包）
for (var i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 100);   // 3,3,3（闭包共享 i）
}
// 解决：let（块级）或 IIFE 捕获
```

> 🎯 **要点**：闭包 = "函数记住外层变量"。三大应用——私有变量、防抖节流、模块模式。循环陷阱（var + setTimeout 输出 3,3,3）是必考。

---

## 4. this 指向全解

### 4.1 四条规则

```
this 指向（调用时决定，不是定义时！）：

① 默认绑定：独立调用 → window（严格模式 undefined）
② 隐式绑定：obj.method() → obj
③ 显式绑定：call/apply/bind → 指定对象
④ new 绑定：new fn() → 新对象

优先级：new > 显式 > 隐式 > 默认

⚠️ 面试必答：
"this 由'调用方式'决定——
 谁调用指向谁（隐式）、
 显式指定（call/apply/bind）、
 new 最高优先级。"
```

### 4.2 显式绑定三兄弟

```javascript
function greet(a, b) {
    return `${this.name}: ${a + b}`;
}
const obj = { name: '张三' };

greet.call(obj, 1, 2);      // 立即执行，参数逐个
greet.apply(obj, [1, 2]);   // 立即执行，参数数组
const bound = greet.bind(obj);  // 返回新函数（不执行）
bound(1, 2);                // 之后调用

// bind 的经典应用：固定 this
const getUser = api.get.bind(api);
```

---

## 5. 箭头函数

### 5.1 箭头 vs 普通函数

| 维度 | 普通函数 | 箭头函数 |
|------|:---:|:---:|
| this | 调用时决定 | **定义时捕获（词法）** |
| arguments | ✅ | ❌（用 rest） |
| 构造函数 | ✅ new | ❌ |
| 提升 | 声明提升 | 不提升 |
| 简写 | — | 单表达式省略 return |

### 5.2 箭头函数的核心价值

```javascript
// 箭头函数没有自己的 this → 捕获外层（解决回调 this 丢失）

// ❌ 普通函数回调中 this 丢失
const obj = {
    items: [1, 2, 3],
    process() {
        this.items.forEach(function (x) {   // this = window ✗
            console.log(this);              // undefined/global
        });
    },
};

// ✅ 箭头函数捕获外层 this
const obj2 = {
    items: [1, 2, 3],
    process() {
        this.items.forEach(x => {
            console.log(this);              // ✅ this = obj2
        });
    },
};
```

> 🎯 **要点**：箭头函数无自己的 this/arguments/原型——它"捕获定义时的 this"。回调、事件、定时器里用箭头函数是标配。

---

## 6. 高阶函数与柯里化

### 6.1 高阶函数

```javascript
// 高阶函数 = 接收函数作为参数 或 返回函数

// ① 函数作为参数（回调）
[1, 2, 3].map(x => x * 2);

// ② 返回函数（工厂）
function createMultiplier(factor) {
    return (x) => x * factor;    // 闭包！
}
const double = createMultiplier(2);
double(5);    // 10
```

### 6.2 柯里化

```javascript
// 柯里化 = 多参数函数转单参数链
function curry(fn) {
    return function curried(...args) {
        if (args.length >= fn.length) {
            return fn(...args);            // 参数够了 → 执行
        }
        return (...more) => curried(...args, ...more);  // 不够 → 继续收集
    };
}

const add3 = curry((a, b, c) => a + b + c);
add3(1)(2)(3);    // 6
add3(1, 2)(3);    // 6

// 偏函数（固定部分参数）
const bind = (fn, ...fixed) => (...rest) => fn(...fixed, ...rest);
const addTen = bind(add3, 10);
addTen(1)(2);     // 13? add3(10,1,2)
```

> 🎯 **要点**：高阶函数（map/filter 都是）+ 柯里化（参数收集链）是函数式编程基础——面试手写 curry 是常见题。

---

> 🎯 **核心要点**：函数体系 = **闭包**（词法作用域 + 记住变量）+ **this 四规则**（调用决定 + new 最高）+ **箭头函数**（捕获 this）+ **高阶/柯里化**（函数是一等公民）。这四块是 JS 面试的第一核心域——闭包、this、防抖节流是手写题常客。

---

**返回总览**：[00-JavaScript总览与核心概念](00-JavaScript总览与核心概念.md) | **上一篇**：[01-JavaScript基础语法与流程控制](01-JavaScript基础语法与流程控制.md) | **下一篇**：[03-JavaScript对象与原型链](03-JavaScript对象与原型链.md)
