# 02 - Promise 异步编程与 Java 对标

> 🎯 Promise 之于 JavaScript = CompletableFuture 之于 Java — 都是解决异步编程的方案。理解三者（回调→Promise→async/await）的演进，后端能读懂前端异步代码

---

## 目录

1. [回调地狱 → Promise → async/await](#1-回调地狱--promise--asyncawait)
2. [Promise 核心 API](#2-promise-核心-api)
3. [Promise 与 Java CompletableFuture 对标](#3-promise-与-java-completablefuture-对标)
4. [async/await 语法糖](#4-asyncawait-语法糖)

---

## 1. 回调地狱 → Promise → async/await

```javascript
// ❌ 回调地狱（Callback Hell）
getUser(1, user => {
  getOrders(user.id, orders => {
    getOrderDetail(orders[0].id, detail => {
      console.log(detail);    // 3 层嵌套！
    });
  });
});

// ✅ Promise 链式调用
getUser(1)
  .then(user => getOrders(user.id))
  .then(orders => getOrderDetail(orders[0].id))
  .then(detail => console.log(detail))
  .catch(err => console.error(err));

// ⭐ async/await — 像同步代码一样写异步
async function getOrderDetail(userId) {
  try {
    const user = await getUser(userId);
    const orders = await getOrders(user.id);
    const detail = await getOrderDetail(orders[0].id);
    console.log(detail);
  } catch (err) {
    console.error(err);
  }
}
```

---

## 2. Promise 核心 API

```javascript
// ═══ 创建 Promise ═══
const promise = new Promise((resolve, reject) => {
  // 异步操作
  setTimeout(() => {
    const success = true;
    if (success) resolve('成功');
    else reject('失败');
  }, 1000);
});

// ═══ Promise.all — 全部成功才返回 ═══
const [user, orders, products] = await Promise.all([
  axios.get('/api/users/1'),
  axios.get('/api/orders?userId=1'),
  axios.get('/api/products')
]);

// ═══ Promise.race — 最快返回（超时控制） ═══
const result = await Promise.race([
  axios.get('/api/users/1'),
  new Promise((_, reject) => setTimeout(() => reject('超时'), 5000))
]);

// ═══ Promise.allSettled — 全部完成（不管成败） ═══
const results = await Promise.allSettled([
  fetch('/api/users/1'),
  fetch('/api/users/2'),
  fetch('/api/users/999')       // 404 不阻塞其他
]);
results.forEach(r => {
  if (r.status === 'fulfilled') console.log(r.value);
  if (r.status === 'rejected') console.log(r.reason);
});
```

| API | 行为 | Java 对标 |
|-----|------|----------|
| `Promise.all` | 全成功才成功，一个失败全失败 | `CompletableFuture.allOf()` |
| `Promise.race` | 最快返回 | `CompletableFuture.anyOf()` |
| `Promise.allSettled` | 全完成，分别看结果 | — |
| `.then()` | 链式处理 | `.thenApply()` / `.thenAccept()` |
| `.catch()` | 统一错误 | `.exceptionally()` |

---

## 3. Promise 与 Java CompletableFuture 对标

```java
// ═══ Java 并行调用 ═══
CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> getUser(1L));
CompletableFuture<List<Order>> ordersFuture = CompletableFuture.supplyAsync(() -> getOrders(1L));

// Promise.all 对标 allOf
CompletableFuture.allOf(userFuture, ordersFuture).join();
User user = userFuture.get();
List<Order> orders = ordersFuture.get();
```

```text
Promise 状态转换 — 对标 Java Future：

          pending（进行中）
          /           \
    fulfilled         rejected
   （已成功）         （已失败）
       │                │
     .then()         .catch()

Java 对标：
  pending  = Future 未完成
  fulfilled = Future.get() 返回
  rejected = Future.get() 抛异常
```

---

## 4. async/await 语法糖

```javascript
// ⭐ async/await — 写起来像同步，实际是异步
async function loadUserPage() {
  try {
    const user = await axios.get('/api/users/1');
    const orders = await axios.get(`/api/users/${user.id}/orders`);

    // 并行调用（不依赖）
    const [products, coupons] = await Promise.all([
      axios.get('/api/products'),
      axios.get('/api/coupons')
    ]);

    return { user, orders, products, coupons };
  } catch (error) {
    console.error('加载失败:', error);
    throw error;
  }
}
```

```text
async/await 本质：
  async 函数 → 始终返回 Promise
  await → 等 Promise 完成后再继续（暂停函数执行，不阻塞主线程）

⚠️ await 是顺序执行！如果请求间无依赖，用 Promise.all 并行：
  ✅ await Promise.all([req1, req2])    ← 并行
  ❌ await req1; await req2;            ← 串行（除非 req2 依赖 req1）
```

### 前端实际请求模式

```javascript
// ⭐ 最常见的页面加载写法
async mounted() {
  try {
    const [user, config] = await Promise.all([
      api.get('/users/me'),
      api.get('/config')
    ]);
    this.user = user;
    this.config = config;
  } catch (err) {
    this.$message.error('加载失败');
  } finally {
    this.loading = false;    // 无论成败都关闭 loading
  }
}
```

> 🎯 **后端对标理解**：`await` = `future.get()`、`Promise.all` = `CompletableFuture.allOf()`、`try/catch` = `@ExceptionHandler`、`finally` = `try-finally`。前端异步的本质和后端一模一样，只是语法不同。
