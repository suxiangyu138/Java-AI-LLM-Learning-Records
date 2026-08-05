# 03 - 异步编程：Promise 与 async/await

> 🎯 Node.js 的灵魂是异步 I/O——理解事件循环、Promise、async/await，才能写出不阻塞、不卡死的代码。这是 Java 开发者最容易踩坑的地方

---

## 目录

1. [事件循环](#1-事件循环)
2. [Callback → Promise → async/await](#2-callback--promise--asyncawait)
3. [错误处理](#3-错误处理)

---

## 1. 事件循环

```text
Node.js 单线程怎么处理 1000 个并发请求？

答案：事件循环 (Event Loop)

┌─────────────────────────┐
│    Event Loop (主线程)   │
│                         │
│  1. 执行同步代码          │
│  2. 检查微任务队列        │  ← Promise.then/catch
│  3. 检查宏任务队列        │  ← setTimeout/setInterval/I/O
│  4. 重复 2-3              │
└─────────────────────────┘

关键规则：
  → 所有用户代码在主线程执行（单线程）
  → I/O 操作交给系统内核（libuv 线程池）异步执行
  → I/O 完成后，回调函数被放入任务队列
  → 事件循环从队列中取出任务执行
```

## 2. Callback → Promise → async/await

### 2.1 Callback（回调地狱）

```javascript
// ❌ 回调地狱 — 深层嵌套，不可维护
fs.readFile('a.txt', (err, dataA) => {
    if (err) throw err;
    fs.readFile('b.txt', (err, dataB) => {
        if (err) throw err;
        fs.readFile('c.txt', (err, dataC) => {
            if (err) throw err;
            console.log(dataA + dataB + dataC);
        });
    });
});
```

### 2.2 Promise

```javascript
// ✅ Promise — 链式调用，告别嵌套
function readFile(path) {
    return new Promise((resolve, reject) => {
        fs.readFile(path, (err, data) => {
            if (err) reject(err);
            else resolve(data);
        });
    });
}

readFile('a.txt')
    .then(dataA => readFile('b.txt').then(dataB => [dataA, dataB]))
    .then(([dataA, dataB]) => readFile('c.txt').then(dataC => [dataA, dataB, dataC]))
    .then(all => console.log(all.join('')))
    .catch(err => console.error('失败:', err));
```

### 2.3 async/await（推荐）

```javascript
// 🏆 async/await — 像写同步代码一样写异步
async function readAll() {
    try {
        const [dataA, dataB, dataC] = await Promise.all([
            fs.promises.readFile('a.txt', 'utf-8'),
            fs.promises.readFile('b.txt', 'utf-8'),
            fs.promises.readFile('c.txt', 'utf-8'),
        ]);
        console.log(dataA + dataB + dataC);
    } catch (err) {
        console.error('失败:', err.message);
    }
}
```

### 2.4 Java 对比

| 概念 | Java | Node.js |
|------|------|---------|
| 异步任务 | `CompletableFuture<T>` | `Promise<T>` |
| 等待结果 | `future.get()` | `await promise` |
| 组合 | `thenCompose()` / `thenCombine()` | `.then()` / `Promise.all()` |
| 异常 | `exceptionally()` | `.catch()` |
| 异步方法 | `@Async` | `async function` |
| 线程池 | `ForkJoinPool` | libuv 线程池 |

## 3. 错误处理

```javascript
// 规则 1：async 函数中的 try-catch 能捕获 await
async function safeCall() {
    try {
        const data = await fetch('https://api.example.com');
        return await data.json();
    } catch (err) {
        console.error('请求失败:', err.message);
        return null;  // 降级返回值
    }
}

// 规则 2：Promise 链末尾必须有 .catch()（防止未处理异常）
fetchData()
    .then(process)
    .catch(err => console.error('最终的兜底:', err));

// 规则 3：顶层 async 用 IIFE
(async () => {
    try {
        await main();
    } catch (err) {
        console.error(err);
        process.exit(1);
    }
})();

// 规则 4：绝不要混用 async/await 和 .then()
// ✅ 统一用 async/await
// ❌ const data = await fetch().then(r => r.json())  // 混乱
```

## 核心要点回顾

- 事件循环 = 同步代码 → 微任务(Promise) → 宏任务(I/O/timer)
- async/await > Promise > Callback（从好到差）
- `Promise.all()` 并行执行独立异步任务
- async 函数总是返回 Promise
- 每个 Promise 链末尾必须有 `.catch()`
- 永远不要阻塞事件循环（`while(true)`、大数组同步遍历）

## 参考资料

1. MDN async/await — developer.mozilla.org
2. Node.js Event Loop 文档 — nodejs.org/en/learn
