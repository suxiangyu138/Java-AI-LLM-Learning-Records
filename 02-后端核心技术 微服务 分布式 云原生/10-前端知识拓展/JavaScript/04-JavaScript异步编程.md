# 04 - JavaScript 异步编程

> 定位：从回调地狱到 Promise 到 async/await 的演进——事件循环、微任务宏任务、并发控制、手写实现

## 📚 目录

1. [异步的本质](#1-异步的本质)
2. [Promise 全解](#2-promise-全解)
3. [async/await](#3-asyncawait)
4. [事件循环与任务队列](#4-事件循环与任务队列)
5. [并发控制](#5-并发控制)
6. [手写实现（面试）](#6-手写实现面试)

---

## 1. 异步的本质

### 1.1 为什么需要异步

```
JS 单线程 → 同步阻塞 = 页面卡死
异步 = 不等待结果，先做别的，结果到了再处理

演进路线：
  回调函数 → Promise → async/await（语法糖）
  ⚠️ 底层都是事件循环 + 任务队列

⚠️ 面试必答：
"异步 = 主线程不阻塞——
 耗时操作交给宿主（定时器/网络），
 完成回调进入任务队列，栈空时执行。"
```

### 1.2 回调与回调地狱

```javascript
// 回调地狱（多层嵌套，难以维护）
getUser(id, (user) => {
    getOrders(user.id, (orders) => {
        getDetails(orders[0].id, (detail) => {
            console.log(detail);
        });
    });
});

// Promise 解决：链式扁平化
getUser(id)
    .then(user => getOrders(user.id))
    .then(orders => getDetails(orders[0].id))
    .then(detail => console.log(detail))
    .catch(err => console.error(err));
```

---

## 2. Promise 全解

### 2.1 三种状态

```
Promise 三态（不可逆）：
  pending（等待）→ fulfilled（成功）/ rejected（失败）

状态不可逆：pending → fulfilled 后不能再 rejected

⚠️ 面试必答：
"Promise 状态机三态单向流转——
 pending 是唯一可转移状态；
 状态由 resolve/reject 触发且只触发一次。"
```

### 2.2 基础用法

```javascript
// 创建
const promise = new Promise((resolve, reject) => {
    // 异步操作
    setTimeout(() => {
        Math.random() > 0.5 ? resolve('成功') : reject(new Error('失败'));
    }, 1000);
});

// 消费
promise
    .then(value => console.log(value))      // 成功
    .catch(err => console.error(err))       // 失败
    .finally(() => console.log('完成'));    // 总是执行（ES2018）

// 链式传递
fetchUser(id)
    .then(user => fetchOrders(user.id))     // 返回新 Promise → 继续链
    .then(orders => orders.length)
    .catch(err => handleError(err));
```

### 2.3 静态方法（重点）

```javascript
// 并发：全部成功才成功（一个失败全失败）
Promise.all([p1, p2, p3])
    .then(([r1, r2, r3]) => { });

// 并发：首个成功或失败
Promise.race([p1, p2]);

// 全部结算（不因失败短路）ES2020
Promise.allSettled([p1, p2])
    .then(results => results.forEach(r => {
        r.status === 'fulfilled' ? r.value : r.reason;
    }));

// 首个成功（忽略失败，ES2021）
Promise.any([p1, p2]);

// 立即值
Promise.resolve(42);
Promise.reject(new Error('x'));

// 统一包装（ES2025）
Promise.try(() => maybeThrows());    // 同步异常也变 rejected
```

---

## 3. async/await

### 3.1 语法糖本质

```javascript
// async 函数 = 返回 Promise 的函数
async function fetchData() {
    // await = 暂停等待（内部等价 then）
    const user = await fetchUser(id);
    const orders = await fetchOrders(user.id);
    return orders;                    // 自动包成 Promise
}

// 等价 Promise 链
function fetchData() {
    return fetchUser(id)
        .then(user => fetchOrders(user.id));
}

// 错误处理：try/catch 替代 .catch
async function safeFetch() {
    try {
        const data = await fetchData();
        return data;
    } catch (err) {
        console.error(err);           // ⚠️ await 的 rejected 抛到这里
        return fallback;
    }
}
```

### 3.2 await 的细节

```javascript
// ① 并发优化：不要串行等待独立请求
// ❌ 串行（慢）
const a = await fetchA();
const b = await fetchB();     // 等 a 完成才开始

// ✅ 并发（快）
const [a, b] = await Promise.all([fetchA(), fetchB()]);

// ② await 只能在 async 内（顶层 await 仅模块支持）
// ③ await 一个非 Promise 值 → 直接返回
const x = await 42;           // 42

// ④ 循环中的 await（注意串行）
for (const id of ids) {
    await process(id);        // 串行处理
}
// 并发处理：Promise.all(ids.map(process))
```

> 🎯 **要点**：async/await 是 Promise 的语法糖（await = then 的暂停版）。独立请求必须用 `Promise.all` 并发——串行 await 是常见性能错误。

---

## 4. 事件循环与任务队列

### 4.1 宏任务 vs 微任务

```
宏任务（macrotask）：setTimeout、setInterval、I/O、事件回调、requestAnimationFrame
微任务（microtask）：Promise.then/catch/finally、queueMicrotask、MutationObserver

执行顺序（每一轮）：
  ① 执行一个宏任务
  ② 清空全部微任务
  ③ 渲染（浏览器）
  ④ 下一轮宏任务

⚠️ 面试必答：
"微任务先于宏任务、每轮宏任务后
 清空微任务——Promise 总是先于
 setTimeout 执行（即使 setTimeout 0）。"
```

### 4.2 输出顺序题（必考）

```javascript
console.log('1');                              // 同步
setTimeout(() => console.log('2'), 0);         // 宏任务
Promise.resolve().then(() => console.log('3')); // 微任务
queueMicrotask(() => console.log('4'));        // 微任务
console.log('5');                              // 同步

// 输出：1 5 3 4 2
// 同步 → 微任务（3,4）→ 宏任务（2）

// 嵌套微任务
Promise.resolve().then(() => {
    console.log('a');
    Promise.resolve().then(() => console.log('b'));
});
setTimeout(() => console.log('c'), 0);
// 输出：a b c（微任务链全部先执行）
```

---

## 5. 并发控制

### 5.1 并发限制（面试手写高频）

```javascript
// 并发限制：同时最多 n 个请求
async function limitConcurrency(tasks, n) {
    const results = [];
    let index = 0;

    async function worker() {
        while (index < tasks.length) {
            const current = index++;           // 取任务（原子）
            results[current] = await tasks[current]();
        }
    }
    // 启动 n 个 worker
    const workers = Array.from({ length: Math.min(n, tasks.length) },
                               () => worker());
    await Promise.all(workers);
    return results;
}
```

### 5.2 重试与超时

```javascript
// 超时控制
async function withTimeout(promise, ms) {
    let timer;
    const timeout = new Promise((_, reject) => {
        timer = setTimeout(() => reject(new Error('超时')), ms);
    });
    try {
        return await Promise.race([promise, timeout]);
    } finally {
        clearTimeout(timer);
    }
}

// 重试（指数退避）
async function retry(fn, times = 3, delay = 500) {
    for (let i = 0; i < times; i++) {
        try { return await fn(); }
        catch (err) {
            if (i === times - 1) throw err;
            await sleep(delay * 2 ** i);       // 指数退避
        }
    }
}
const sleep = (ms) => new Promise(r => setTimeout(r, ms));
```

---

## 6. 手写实现（面试）

### 6.1 手写 Promise.all

```javascript
Promise.myAll = function (promises) {
    return new Promise((resolve, reject) => {
        const results = new Array(promises.length);
        let completed = 0;

        promises.forEach((p, i) => {
            Promise.resolve(p).then(value => {
                results[i] = value;            // 保持顺序
                if (++completed === promises.length) resolve(results);
            }, reject);                        // ⚠️ 一个失败全失败
        });
    });
};
```

### 6.2 手写防抖与节流

```javascript
// 防抖：停止触发后 delay 才执行（输入搜索）
function debounce(fn, delay = 300) {
    let timer = null;
    return function (...args) {
        clearTimeout(timer);
        timer = setTimeout(() => fn.apply(this, args), delay);
    };
}

// 节流：固定间隔最多执行一次（滚动/拖拽）
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
// 防抖 vs 节流：防抖"等停稳"、节流"定时放行"
```

### 6.3 手写 sleep / 深拷贝

```javascript
// sleep
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

// 深拷贝（支持循环引用）
function deepClone(obj, map = new WeakMap()) {
    if (obj === null || typeof obj !== 'object') return obj;
    if (map.has(obj)) return map.get(obj);     // 循环引用
    const clone = Array.isArray(obj) ? [] : {};
    map.set(obj, clone);
    for (const key of Object.keys(obj)) {
        clone[key] = deepClone(obj[key], map);
    }
    return clone;
}
```

> 🎯 **要点**：手写四件套——Promise.all（保持顺序 + 任一失败短路）、防抖（clearTimeout 重置）、节流（时间戳判断）、深拷贝（WeakMap 防循环）——面试最高频。

---

> 🎯 **核心要点**：异步体系 = **事件循环**（宏/微任务顺序）+ **Promise**（三态 + 链式 + 静态方法）+ **async/await**（语法糖 + 并发优化）+ **并发控制**（limit/超时/重试）。输出顺序题、手写 Promise.all、防抖节流是三大必考。

---

**返回总览**：[00-JavaScript总览与核心概念](00-JavaScript总览与核心概念.md) | **上一篇**：[03-JavaScript对象与原型链](03-JavaScript对象与原型链.md) | **下一篇**：[05-JavaScriptES6进阶特性](05-JavaScriptES6进阶特性.md)
