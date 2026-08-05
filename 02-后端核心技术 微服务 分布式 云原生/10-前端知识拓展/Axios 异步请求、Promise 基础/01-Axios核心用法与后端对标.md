# 01 - Axios 核心用法与后端对标

> 🎯 Axios 之于前端 = RestTemplate 之于 Java 后端 — 都是 HTTP 客户端，理解对标关系后端也能读懂前端请求代码

---

## 目录

1. [Axios 是什么](#1-axios-是什么)
2. [基本 CRUD + Java 对标](#2-基本-crud--java-对标)
3. [请求配置与响应结构](#3-请求配置与响应结构)
4. [拦截器（Interceptor）](#4-拦截器interceptor)

---

## 1. Axios 是什么

| 特性 | Axios | Java RestTemplate |
|------|-------|-------------------|
| 类型 | Promise-based HTTP 库 | 同步/异步 HTTP 客户端 |
| 运行环境 | 浏览器 + Node.js | JVM |
| 请求拦截 | ✅ interceptor | ✅ ClientHttpRequestInterceptor |
| 响应拦截 | ✅ interceptor | ✅ ResponseErrorHandler |
| 自动 JSON 解析 | ✅ 内置 | ✅ Jackson 集成 |
| 取消请求 | ✅ AbortController | — |
| 超时设置 | `timeout: 5000` | `setConnectTimeout` / `setReadTimeout` |

---

## 2. 基本 CRUD + Java 对标

```javascript
import axios from 'axios';

// ═══ GET — 查询 ═══
// Java: restTemplate.getForObject("/users/1", User.class)
const { data } = await axios.get('/api/users/1');
console.log(data);  // { code: 200, data: { id: 1, name: "张三" } }

// ═══ GET + 参数 ═══
// Java: restTemplate.getForObject("/users?page=1&size=20", ...)
const res = await axios.get('/api/users', {
  params: { page: 1, size: 20, keyword: '张三' }
});

// ═══ POST — 创建 ═══
// Java: restTemplate.postForObject("/users", user, User.class)
const newUser = await axios.post('/api/users', {
  name: '新用户',
  email: 'new@example.com'
});

// ═══ PUT — 全量更新 ═══
await axios.put('/api/users/1', { name: '张四', age: 30, email: 'new@example.com' });

// ═══ PATCH — 部分更新 ═══
await axios.patch('/api/users/1', { name: '张四' });   // 只改名字

// ═══ DELETE ═══
await axios.delete('/api/users/1');
```

---

## 3. 请求配置与响应结构

```javascript
// ⭐ 全局默认配置（类似 RestTemplate 的全局设置）
axios.defaults.baseURL = 'http://localhost:8080';
axios.defaults.timeout = 10000;
axios.defaults.headers.common['Authorization'] = 'Bearer ' + token;
axios.defaults.headers.post['Content-Type'] = 'application/json';

// 单次请求自定义配置
const res = await axios({
  method: 'post',
  url: '/api/users',
  data: { name: '张三' },
  timeout: 5000,
  headers: { 'X-Custom-Header': 'value' }
});

// 响应结构
console.log(res.status);     // 200
console.log(res.data);       // { code: 200, data: { ... } }  ← 后端返回的 JSON
console.log(res.headers);    // 响应头
```

---

## 4. 拦截器（Interceptor）

> 类似 Spring 的 `ClientHttpRequestInterceptor`

```javascript
// ⭐ 请求拦截器 — 自动加 Token（类似 Feign RequestInterceptor）
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.headers['X-Trace-Id'] = generateTraceId();
  return config;
}, error => Promise.reject(error));

// ⭐ 响应拦截器 — 统一处理错误（类似 @RestControllerAdvice）
axios.interceptors.response.use(
  response => {
    const { code, msg, data } = response.data;
    if (code === 200) return data;         // 直接返回 data，简化调用
    if (code === 401) { router.push('/login'); return Promise.reject(msg); }
    return Promise.reject(msg);
  },
  error => {
    if (error.response?.status === 401) router.push('/login');
    if (error.response?.status === 500) message.error('服务器错误');
    return Promise.reject(error);
  }
);
```

| Axios 拦截器 | Java 对标 |
|-------------|----------|
| `request interceptor` | `ClientHttpRequestInterceptor` / Feign `RequestInterceptor` |
| `response interceptor` | `ResponseErrorHandler` / `@RestControllerAdvice` |

### 封装实例（⭐ 推荐）

```javascript
// api.js — 统一封装
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
});

// 拦截器只在这个实例生效
api.interceptors.request.use(/* ... */);
api.interceptors.response.use(/* ... */);

export default api;

// 使用时
import api from './api';
const user = await api.get('/users/1');
```

> 🎯 **后端对标理解**：Axios = RestTemplate、拦截器 = RequestInterceptor + @RestControllerAdvice、`axios.create()` = 定制 RestTemplate Bean、全局 `defaults` = `application.yml` 配置。
