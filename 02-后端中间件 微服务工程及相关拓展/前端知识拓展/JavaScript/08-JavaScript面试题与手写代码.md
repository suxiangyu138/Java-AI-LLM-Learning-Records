# 08 - JavaScript 面试题与手写代码

> 定位：前端面试 JS 高频题与手写代码全家桶——输出顺序、手写实现、原理问答、综合实战

## 📚 目录

1. [输出顺序题（高频）](#1-输出顺序题高频)
2. [手写实现大全](#2-手写实现大全)
3. [原理问答 TOP 10](#3-原理问答-top-10)
4. [综合实战：实现一个 EventEmitter](#4-综合实战实现一个-eventemitter)

---

## 1. 输出顺序题（高频）

### 1.1 事件循环类

```javascript
// 题 1：宏微任务顺序
console.log('1');
setTimeout(() => console.log('2'), 0);
Promise.resolve().then(() => console.log('3'));
console.log('4');
// 答案：1 4 3 2（同步 → 微任务 → 宏任务）

// 题 2：嵌套微任务
Promise.resolve().then(() => {
    console.log('a');
    Promise.resolve().then(() => console.log('b'));
});
setTimeout(() => console.log('c'), 0);
// 答案：a b c（微任务链全部先于宏任务）

// 题 3：async/await 展开
async function foo() {
    console.log('1');
    await bar();              // await 右侧同步执行
    console.log('3');         // 之后的代码是微任务
}
async function bar() {
    console.log('2');
}
foo();
console.log('4');
// 答案：1 2 4 3（await 后的代码 = then 回调 = 微任务）
```

### 1.2 作用域与闭包类

```javascript
// 题 4：var 循环陷阱
for (var i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 100);
}
// 答案：3 3 3（var 函数级作用域，闭包共享 i）
// 修复：let（块级）或 IIFE 捕获

// 题 5：let 块级
for (let i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 100);
}
// 答案：0 1 2（每次迭代独立绑定）

// 题 6：经典闭包
let x = 1;
function fn() { console.log(x); }
function outer() {
    let x = 2;
    fn();                     // ⚠️ 词法作用域：看定义位置
}
outer();
// 答案：1（fn 定义在全局，x 是全局的）
```

---

## 2. 手写实现大全

### 2.1 防抖与节流

```javascript
// 防抖：等停稳再执行
function debounce(fn, delay = 300) {
    let timer = null;
    return function (...args) {
        clearTimeout(timer);
        timer = setTimeout(() => fn.apply(this, args), delay);
    };
}

// 节流：固定间隔放行
function throttle(fn, interval = 300) {
    let last = 0;
    return function (...args) {
        const now = Date.now();
        if (now - last >= interval) {
            last = now;
            fn.apply(this, args);
        }
    };
}
```

### 2.2 深拷贝与浅拷贝

```javascript
// 浅拷贝（ES6 展开即可）
const shallow = { ...obj };

// 深拷贝（手写版：支持数组/循环引用）
function deepClone(obj, map = new WeakMap()) {
    if (obj === null || typeof obj !== 'object') return obj;
    if (map.has(obj)) return map.get(obj);      // 循环引用
    const clone = Array.isArray(obj) ? [] : {};
    map.set(obj, clone);
    for (const key of Object.keys(obj)) {
        clone[key] = deepClone(obj[key], map);
    }
    return clone;
}
// 生产环境：structuredClone(obj)（ES2022 原生）
```

### 2.3 手写 Promise 系列

```javascript
// Promise.all：保持顺序 + 任一失败短路
Promise.myAll = function (promises) {
    return new Promise((resolve, reject) => {
        const results = new Array(promises.length);
        let done = 0;
        promises.forEach((p, i) => {
            Promise.resolve(p).then(v => {
                results[i] = v;
                if (++done === promises.length) resolve(results);
            }, reject);
        });
    });
};

// Promise.race：首个结算
Promise.myRace = function (promises) {
    return new Promise((resolve, reject) => {
        for (const p of promises) {
            Promise.resolve(p).then(resolve, reject);
        }
    });
};

// Promise 版 sleep
const sleep = (ms) => new Promise(r => setTimeout(r, ms));
```

### 2.4 其他高频手写

```javascript
// 手写 call（核心：临时挂载 + 调用 + 清理）
Function.prototype.myCall = function (thisArg, ...args) {
    const fn = Symbol('fn');                    // 防冲突
    thisArg = thisArg ?? globalThis;
    thisArg[fn] = this;                         // 挂载
    const result = thisArg[fn](...args);        // 调用（this = thisArg）
    delete thisArg[fn];                         // 清理
    return result;
};

// 手写 instanceof
function myInstanceof(obj, Ctor) {
    let proto = Object.getPrototypeOf(obj);
    while (proto) {
        if (proto === Ctor.prototype) return true;
        proto = Object.getPrototypeOf(proto);
    }
    return false;
}

// 手写 new
function myNew(Ctor, ...args) {
    const obj = Object.create(Ctor.prototype);  // ① 原型连接
    const result = Ctor.apply(obj, args);       // ② 绑定 this
    return typeof result === 'object' && result !== null
        ? result : obj;                         // ③ 返回对象优先
}
```

---

## 3. 原理问答 TOP 10

**Q1: 闭包是什么？应用场景？**
```
闭包 = 函数 + 词法作用域（记住外层变量）
应用：私有变量、防抖节流、模块模式、柯里化
⚠️ 副作用：内存（变量不被回收）——注意释放
```

**Q2: this 指向怎么判断？**
```
调用方式决定：独立调用 → window（严格 undefined）
obj.method() → obj；call/apply/bind → 指定
new → 新对象。优先级：new > 显式 > 隐式 > 默认
箭头函数：定义时捕获（无自己的 this）
```

**Q3: 事件循环机制？**
```
单线程 + 任务队列：同步先执行，
微任务（Promise）每轮宏任务后清空，
宏任务（setTimeout）按序执行。
```

**Q4: 原型链是什么？**
```
实例.__proto__ → 构造.prototype → Object.prototype → null
属性查找沿链向上；方法共享（原型）、状态独立（实例）
```

**Q5: var / let / const 区别？**
```
作用域（函数 vs 块）、提升（TDZ vs undefined）、
重复声明、const 引用不可变。默认 const。
```

**Q6: == 和 === 的区别？**
```
== 隐式转换后比较（有坑：0==''、[]==false）
=== 严格比较（类型 + 值）。永远用 ===。
```

**Q7: 事件委托原理？**
```
冒泡：子元素事件冒泡到父 → 父统一处理（closest 定位）
好处：动态元素、事件数量 N→1、性能
```

**Q8: 深拷贝与浅拷贝？**
```
浅拷贝只复制第一层（...obj）；
深拷贝复制全部层级（structuredClone/手写 WeakMap 防循环）
```

**Q9: 防抖和节流区别？**
```
防抖：等停稳才执行（输入搜索）
节流：固定间隔放行（滚动）
```

**Q10: async/await 本质？**
```
Promise 的语法糖：await = then 的暂停版
async 函数返回 Promise；独立请求用 Promise.all 并发
```

---

## 4. 综合实战：实现一个 EventEmitter

```javascript
// ⚠️ 面试综合题：手写事件总线（Vue $on/$emit 的底层）

class EventEmitter {
    constructor() {
        this.events = new Map();          // 事件名 → 回调数组
    }

    // 订阅
    on(event, handler) {
        if (!this.events.has(event)) this.events.set(event, []);
        this.events.get(event).push(handler);
        return this;                      // 链式调用
    }

    // 一次性订阅
    once(event, handler) {
        const wrapper = (...args) => {
            this.off(event, wrapper);     // 先取消再执行
            handler(...args);
        };
        wrapper._original = handler;      // 保留原引用（off 可匹配）
        return this.on(event, wrapper);
    }

    // 发布
    emit(event, ...args) {
        const handlers = this.events.get(event);
        if (handlers) {
            [...handlers].forEach(h => h(...args));   // 副本（防修改遍历）
        }
        return this;
    }

    // 取消订阅
    off(event, handler) {
        if (!handler) {
            this.events.delete(event);    // 取消该事件全部
        } else {
            const handlers = this.events.get(event);
            if (handlers) {
                this.events.set(event, handlers.filter(
                    h => h !== handler && h._original !== handler
                ));
            }
        }
        return this;
    }

    // 订阅数
    listenerCount(event) {
        return (this.events.get(event) || []).length;
    }
}

// 使用
const bus = new EventEmitter();
const handler = (msg) => console.log('收到：' + msg);
bus.on('message', handler);
bus.once('init', () => console.log('初始化一次'));
bus.emit('message', '你好');        // 收到：你好
bus.emit('init');                   // 初始化一次
bus.emit('init');                   // （once 已移除，无输出）
bus.off('message', handler);
bus.emit('message', '不再收到');     // 无输出
```

> 🎯 **要点**：EventEmitter 覆盖**类设计 + Map + 闭包 + 数组操作**四大能力——是前端手写综合题的集大成者。注意细节：once 包装、off 匹配原始引用、emit 用副本遍历。

---

> 🎯 **核心要点**：JS 面试 = **输出顺序题**（事件循环 + 闭包作用域）+ **手写五件套**（防抖节流/深浅拷贝/Promise.all/call/new/instanceof）+ **原理十问** + **EventEmitter 综合**。先答定义、再讲原理、最后给工程建议是标准回答结构。

---

**返回总览**：[00-JavaScript总览与核心概念](00-JavaScript总览与核心概念.md) | **上一篇**：[07-JavaScriptES2025与ES2026新特性](07-JavaScriptES2025与ES2026新特性.md)
