# AJAX 与前后端数据交互
> 后端开发者理解前端如何发送请求、处理响应，以及常见的跨域、文件上传、实时通信等问题

## 目录

1. [AJAX 概念与背景](#1-ajax-概念与背景)
2. [XMLHttpRequest（历史）](#2-xmlhttprequest历史)
3. [Fetch API（现代标准）](#3-fetch-api现代标准)
4. [Axios（企业首选）](#4-axios企业首选)
5. [Axios 拦截器详解](#5-axios-拦截器详解)
6. [数据格式详解](#6-数据格式详解)
7. [文件上传与进度追踪](#7-文件上传与进度追踪)
8. [Server-Sent Events（SSE）](#8-server-sent-eventssse)
9. [WebSocket 基础](#9-websocket-基础)
10. [通信方式对比表](#10-通信方式对比表)
11. [常见问题与排查](#11-常见问题与排查)
12. [综合示例：Axios 封装](#12-综合示例axios-封装)

---

## 1. AJAX 概念与背景

### 什么是 AJAX？

**AJAX** = Asynchronous JavaScript And XML（异步 JavaScript 和 XML）

它的核心价值是：**不刷新整个页面，只更新部分内容**。

### 为什么需要异步？

```
传统同步请求（form 提交）：
  [点击提交] → 浏览器卡死 → 等待服务器响应 → 加载全新页面 → 页面闪烁

AJAX 异步请求：
  [点击按钮] → JS 发起后台请求 → 用户继续操作页面 → 响应到达 → JS 更新局部 DOM
```

```javascript
// 同步请求的痛点：整个页面被阻塞
// ❌ form 提交会导致页面刷新

// ✅ AJAX 异步请求：局部更新
const response = await fetch('/api/data');
const data = await response.json();
document.getElementById('result').textContent = data.message;
// 页面其他部分不受影响，用户可继续操作
```

> 💡 后端开发者视角：AJAX 使得前端可以像后端调用 RPC 一样调用 API。每一次 fetch/axios 调用，对应后端一个 controller 方法的执行。理解 AJAX 有助于调试 API 调用和排查跨域问题。

---

## 2. XMLHttpRequest（历史）

> 了解即可，除非需要维护老项目，新代码不应使用。

```javascript
// XMLHttpRequest（XHR）—— 老式 API
const xhr = new XMLHttpRequest();

xhr.open('GET', '/api/users/1', true);  // true = 异步

xhr.onreadystatechange = function() {
    if (xhr.readyState === 4) {         // 请求完成
        if (xhr.status >= 200 && xhr.status < 300) {
            const data = JSON.parse(xhr.responseText);
            console.log('成功:', data);
        } else {
            console.error('失败:', xhr.status);
        }
    }
};

xhr.onerror = function() {
    console.error('网络错误');
};

xhr.setRequestHeader('Content-Type', 'application/json');
xhr.send(JSON.stringify({ name: '张三' }));

// 问题：回调地狱、不支持 Promise、代码冗长
```

| XHR 阶段 | readyState | 含义 |
|---------|-----------|------|
| UNSENT | 0 | 已创建但未 open |
| OPENED | 1 | 已 open |
| HEADERS_RECEIVED | 2 | 已收到响应头 |
| LOADING | 3 | 响应体下载中 |
| DONE | 4 | 请求完成 |

---

## 3. Fetch API（现代标准）

### 3.1 基本用法

```javascript
// 最基本的 GET 请求
const response = await fetch('/api/users/1');
const user = await response.json();
console.log(user);

// POST 请求
const response = await fetch('/api/users', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify({ name: '张三', age: 25 })
});

if (response.ok) {  // HTTP 状态码 200-299
    const createdUser = await response.json();
} else {
    console.error('请求失败:', response.status, response.statusText);
}
```

### 3.2 重要：Fetch 的错误处理陷阱

```javascript
// ❌ 常见错误：Fetch 只在网络故障时 reject，HTTP 4xx/5xx 不会！
try {
    const response = await fetch('/api/users/999');
    // response.ok === false（例如 404），但不会进入 catch！
    if (!response.ok) {
        throw new Error(`HTTP Error: ${response.status}`);
    }
    const data = await response.json();
} catch (error) {
    // catch 只在网络断开、DNS 解析失败等情况下执行
    console.error('网络错误:', error);
}

// ✅ 正确做法：手动检查 response.ok
async function safeFetch(url, options = {}) {
    const response = await fetch(url, options);
    if (!response.ok) {
        // 尝试获取错误体的详细信息
        let errorBody;
        try {
            errorBody = await response.json();
        } catch {
            errorBody = { message: response.statusText };
        }
        throw new Error(`[${response.status}] ${errorBody.message || '请求失败'}`);
    }
    return response.json();
}
```

### 3.3 Fetch 其他功能

```javascript
// 发送 FormData（文件上传）
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('description', '头像');

await fetch('/api/upload', {
    method: 'POST',
    body: formData  // 浏览器自动设置 Content-Type: multipart/form-data
});

// 携带 Cookie（跨域时）
await fetch('/api/data', {
    credentials: 'include'  // 同域默认发送 Cookie，跨域需显式设置
});

// 取消请求（AbortController）
const controller = new AbortController();
setTimeout(() => controller.abort(), 5000);  // 5 秒超时

try {
    const response = await fetch('/api/slow-query', {
        signal: controller.signal
    });
} catch (error) {
    if (error.name === 'AbortError') {
        console.log('请求已被取消（超时）');
    }
}
```

| Fetch 特性 | 是否支持 | 说明 |
|-----------|---------|------|
| 默认不携带 Cookie | ❌ | 需设置 `credentials: 'include'` |
| 4xx/5xx 不 reject | ⚠️ | 需手动检查 `response.ok` |
| 上传进度 | ❌ | Fetch 不原生支持进度回调 |
| 请求超时 | ❌ | 需用 AbortController 模拟 |
| JSON 自动解析 | ❌ | 需手动调用 `response.json()` |

> ⚠️ Fetch 的设计更接近底层 HTTP 操作，它不会对响应状态做任何假设——200 和 404 对 Fetch 都是"成功的 HTTP 请求"。这种设计导致了常见的 `response.ok` 遗漏问题。

---

## 4. Axios（企业首选）

> Axios 是目前最流行的 HTTP 客户端库，封装了 XHR 并提供更友好的 API。

### 4.1 为什么选择 Axios？

| 功能 | Fetch | Axios |
|------|-------|-------|
| 自动 JSON 解析 | 手动 `response.json()` | 自动解析 |
| 错误处理 | 4xx/5xx 不 reject | 自动 reject 非 2xx 响应 |
| 请求/响应拦截器 | 无（需手动封装） | 内置 |
| 上传进度 | 不支持 | 支持（`onUploadProgress`） |
| 请求超时 | 需 AbortController | 原生 `timeout` 配置 |
| 取消请求 | AbortController | `CancelToken` / `AbortController` |
| 浏览器兼容 | 现代浏览器 | 支持 IE11（基于 XHR） |
| 下载进度 | 不支持 | 支持（`onDownloadProgress`） |
| 请求体自动序列化 | 手动 `JSON.stringify` | 自动 |

### 4.2 基本用法

```javascript
import axios from 'axios';

// GET 请求
const { data } = await axios.get('/api/users', {
    params: { page: 1, size: 10 },         // 自动拼接 ?page=1&size=10
    timeout: 5000                           // 5 秒超时
});
console.log(data);  // 已经是解析后的 JSON 对象

// POST 请求
const { data } = await axios.post('/api/users', {
    name: '张三',
    age: 25
}, {
    headers: { 'Authorization': `Bearer ${token}` }
});

// PUT / DELETE
await axios.put(`/api/users/${id}`, updatedData);
await axios.delete(`/api/users/${id}`);

// 并发请求
const [users, orders] = await Promise.all([
    axios.get('/api/users'),
    axios.get('/api/orders')
]);
```

### 4.3 全局配置

```javascript
// 创建 Axios 实例（推荐：不同服务使用不同实例）
const apiClient = axios.create({
    baseURL: 'https://api.example.com/v2',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    }
});

// 使用实例
const { data } = await apiClient.get('/users');
```

> 🎯 **后端开发者推荐**：在项目中使用 Axios 而不是直接使用 Fetch。Axios 的错误处理方式（非 2xx 自动 reject）更接近后端 HTTP 客户端的直觉。

---

## 5. Axios 拦截器详解

拦截器是 Axios 最强大的特性。**请求拦截器**在请求发出前执行，**响应拦截器**在收到响应后执行。

### 5.1 请求拦截器——自动添加 Token

```javascript
const apiClient = axios.create({
    baseURL: 'https://api.example.com'
});

// 请求拦截器
apiClient.interceptors.request.use(
    (config) => {
        // 从存储中获取 token
        const token = localStorage.getItem('token');

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        // 添加请求追踪 ID
        config.headers['X-Request-Id'] = crypto.randomUUID();

        // 请求日志（开发环境）
        console.log(`[${config.method?.toUpperCase()}] ${config.url}`, config.params || '');

        return config;  // 必须返回 config
    },
    (error) => {
        // 请求配置错误（极少发生）
        return Promise.reject(error);
    }
);
```

### 5.2 响应拦截器——全局错误处理 + Token 刷新

```javascript
// 响应拦截器
apiClient.interceptors.response.use(
    (response) => {
        // 2xx 响应：直接返回 data
        console.log(`[响应] ${response.config.url} -> ${response.status}`);
        return response.data;  // 此后 .then(data) 直接拿到 data，不再有 response 包裹
    },
    async (error) => {
        // 非 2xx 响应 或 网络错误
        const originalRequest = error.config;

        if (error.response) {
            const { status, data } = error.response;

            switch (status) {
                case 401:  // 未认证 / Token 过期
                    // 尝试刷新 token
                    if (!originalRequest._retry) {
                        originalRequest._retry = true;
                        try {
                            const refreshToken = localStorage.getItem('refreshToken');
                            const { accessToken } = await axios.post('/api/auth/refresh', {
                                refreshToken
                            });
                            localStorage.setItem('token', accessToken);
                            // 重试原始请求
                            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                            return apiClient(originalRequest);
                        } catch (refreshError) {
                            // 刷新失败：跳转到登录页
                            localStorage.clear();
                            window.location.href = '/login';
                            return Promise.reject(refreshError);
                        }
                    }
                    break;

                case 403:
                    console.error('无权限访问:', originalRequest.url);
                    break;

                case 429:
                    // 限流：等待一段时间后重试
                    const retryAfter = error.response.headers['retry-after'] || 2;
                    await new Promise(resolve => setTimeout(resolve, retryAfter * 1000));
                    return apiClient(originalRequest);

                case 500:
                case 502:
                case 503:
                case 504:
                    console.error('服务器故障:', status);
                    // 可在此触发全局错误提示
                    showErrorNotification('服务暂时不可用，请稍后重试');
                    break;
            }
        } else if (error.request) {
            // 网络错误（浏览器无响应）
            console.error('网络连接失败');
            showErrorNotification('网络连接异常，请检查网络');
        }

        return Promise.reject(error);
    }
);
```

### 5.3 使用封装后的客户端

```javascript
// 使用拦截器后，代码非常简洁
try {
    const users = await apiClient.get('/users');       // 自动携带 token
    const created = await apiClient.post('/users', {    // 自动序列化 JSON
        name: '张三'
    });
} catch (error) {
    // 全局拦截器已处理 401/500 等，这里仅处理业务异常
    console.error('业务错误:', error.message);
}
```

> 💡 拦截器模式对后端开发者很熟悉——本质上就是 Servlet 的 Filter 或 Spring 的 HandlerInterceptor。一个典型的拦截器执行链：**请求拦截器** → **实际 HTTP 请求** → **响应拦截器**。

---

## 6. 数据格式详解

### 6.1 JSON（90% 场景）

```javascript
// 前端发送 JSON
await axios.post('/api/users', {
    name: '张三',
    age: 25,
    tags: ['vip', 'new'],
    address: {
        city: '北京',
        district: '海淀'
    }
});

// 后端接收（Spring Boot）
@PostMapping("/api/users")
public User createUser(@RequestBody @Valid UserCreateRequest request) {
    // request.name -> "张三"
    // request.address.city -> "北京"
}
```

### 6.2 application/x-www-form-urlencoded

```javascript
// 前端方式一：使用 URLSearchParams
const params = new URLSearchParams();
params.append('username', 'admin');
params.append('password', '123456');

await axios.post('/api/login', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
});

// 前端方式二：Axios 自动处理（传入字符串）
await axios.post('/api/login', 'username=admin&password=123456');

// 后端对应
@PostMapping("/api/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
public String login(@RequestParam String username, @RequestParam String password) { ... }
```

### 6.3 multipart/form-data（文件上传）

```javascript
// 文件上传必须使用 FormData
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('userId', '123');
formData.append('type', 'avatar');

await axios.post('/api/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },  // 可省略，Axios 自动设置
    onUploadProgress: (progressEvent) => {
        const percent = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
        );
        console.log(`上传进度: ${percent}%`);
    }
});
```

### 6.4 Blob（文件下载）

```javascript
// 下载文件
const response = await axios.get('/api/export/report', {
    responseType: 'blob'  // 关键：告诉 Axios 返回二进制数据
});

// 触发浏览器下载
const url = window.URL.createObjectURL(new Blob([response.data]));
const link = document.createElement('a');
link.href = url;
link.setAttribute('download', 'report.xlsx');
document.body.appendChild(link);
link.click();
link.remove();
window.URL.revokeObjectURL(url);
```

---

## 7. 文件上传与进度追踪

### 7.1 带进度条的完整上传示例

```html
<!-- 前端 HTML -->
<input type="file" id="fileInput" multiple>
<button onclick="uploadFiles()">上传</button>
<div id="progress">
    <div id="progressBar" style="width: 0%; height: 20px; background: #4CAF50;"></div>
</div>
<div id="status"></div>
```

```javascript
// 完整的文件上传逻辑
async function uploadFiles() {
    const files = document.getElementById('fileInput').files;
    if (files.length === 0) return;

    const formData = new FormData();
    for (const file of files) {
        formData.append('files', file);
    }

    try {
        const result = await axios.post('/api/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            },
            onUploadProgress: (event) => {
                // 计算上传进度百分比
                const percent = Math.round(
                    (event.loaded * 100) / event.total
                );
                document.getElementById('progressBar').style.width = percent + '%';
                document.getElementById('status').textContent =
                    `上传中: ${formatSize(event.loaded)} / ${formatSize(event.total)}`;
            },
            timeout: 300000  // 大型文件超时设为 5 分钟
        });

        document.getElementById('status').textContent = '上传完成！';
        console.log('上传结果:', result.data);

    } catch (error) {
        document.getElementById('status').textContent = '上传失败: ' + error.message;
    }
}

function formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}
```

### 7.2 后端接收文件（Spring Boot）

```java
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @PostMapping
    public List<String> uploadFiles(
            @RequestParam("files") List<MultipartFile> files) {

        List<String> savedPaths = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            long size = file.getSize();
            String contentType = file.getContentType();

            // 校验文件类型
            if (!isAllowedType(contentType)) {
                throw new IllegalArgumentException("不支持的文件类型: " + contentType);
            }

            // 校验文件大小
            if (size > 50 * 1024 * 1024) {  // 50MB
                throw new IllegalArgumentException("文件过大: " + originalName);
            }

            // 保存文件
            String path = saveToDisk(file);
            savedPaths.add(path);
        }

        return savedPaths;
    }
}
```

> ⚠️ 文件上传后端注意事项：1) 必须限制文件大小（防止内存溢出）；2) 校验文件类型（不能只靠扩展名）；3) 防重名（UUID 重命名）；4) 病毒扫描（重要场景）；5) 存储路径不能暴露到公网。

---

## 8. Server-Sent Events（SSE）

SSE 是一种**服务器单向推送**技术，适用于实时通知、日志流、状态更新。

### 8.1 前端接收 SSE

```javascript
// 浏览器原生 EventSource API
const eventSource = new EventSource('/api/notifications/stream');

// 接收命名事件
eventSource.addEventListener('notification', (event) => {
    const data = JSON.parse(event.data);
    console.log('收到通知:', data);
    showNotification(data);
});

// 接收未命名事件（默认 message 事件）
eventSource.onmessage = (event) => {
    console.log('消息:', event.data);
};

// 连接建立
eventSource.onopen = () => {
    console.log('SSE 连接已建立');
};

// 错误处理（会自动重连）
eventSource.onerror = (error) => {
    console.error('SSE 连接错误，将自动重连');
    // eventSource.readyState 会变为 CONNECTING
};
```

### 8.2 后端实现 SSE（Spring Boot）

```java
@RestController
public class NotificationController {

    @GetMapping(path = "/api/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications() {
        SseEmitter emitter = new SseEmitter(60_000L);  // 60 秒超时

        // 在独立线程中发送事件
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Map<String, Object> data = Map.of(
                        "id", i,
                        "message", "通知 #" + i,
                        "timestamp", System.currentTimeMillis()
                    );

                    emitter.send(SseEmitter.event()
                        .name("notification")          // 事件名称
                        .data(data, MediaType.APPLICATION_JSON)
                        .id(String.valueOf(i)));       // 事件 ID（断线重连用）

                    Thread.sleep(1000);  // 每秒发送一条
                }
                emitter.complete();  // 发送完成
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
```

### 8.3 SSE 格式说明

```
SSE 协议文本格式（服务端发送）：
event: notification
id: 1
data: {"message": "通知 #1", "timestamp": 1700000000000}

event: notification
id: 2
data: {"message": "通知 #2", "timestamp": 1700000001000}

event: error
data: {"code": "RATE_LIMIT", "message": "请求过频繁"}
```

| SSE 特性 | 说明 |
|---------|------|
| 传输方向 | 服务器 → 浏览器（单向） |
| 协议 | HTTP（兼容所有防火墙） |
| 自动重连 | 内置（EventSource 自动处理） |
| 适用场景 | 通知推送、日志流、实时状态 |
| 浏览器限制 | 同一域名最多 6 个 SSE 连接 |

> 💡 SSE 适用于后端推送数据到前端的场景，比 WebSocket 更轻量，且基于 HTTP 协议，不会被防火墙拦截。缺点：单向传输，不支持客户端向服务器发送数据。

---

## 9. WebSocket 基础

WebSocket 提供**全双工**通信通道，适用于实时性要求高的场景。

### 9.1 前端 WebSocket

```javascript
// 建立连接
const ws = new WebSocket('wss://api.example.com/ws/chat');

// 连接建立
ws.onopen = () => {
    console.log('WebSocket 连接已建立');

    // 发送消息
    ws.send(JSON.stringify({
        type: 'message',
        content: '你好！',
        roomId: 'room-123'
    }));
};

// 接收消息
ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('收到消息:', data);
    displayMessage(data);
};

// 错误处理
ws.onerror = (error) => {
    console.error('WebSocket 错误:', error);
};

// 连接关闭
ws.onclose = (event) => {
    console.log('WebSocket 已关闭, code:', event.code, 'reason:', event.reason);
    // 可在此重连
};
```

### 9.2 何时使用 WebSocket

| 场景 | 推荐方案 | 原因 |
|------|---------|------|
| 即时聊天 | WebSocket | 双向、低延迟 |
| 实时协作编辑 | WebSocket | 双向、需要频繁交互 |
| 股票行情 | WebSocket | 高频率、低延迟 |
| 通知推送 | SSE | 单向推送即可，更轻量 |
| 日志流 | SSE | 单向、自动重连 |
| 普通 API 请求 | HTTP | 请求-响应模式，不需要持久连接 |

> ⚠️ WebSocket 在生产环境中需要注意：1) 连接数限制（每个 WebSocket 都占用服务端资源）；2) 负载均衡的会话保持（sticky session）；3) 心跳检测（检测死连接）；4) 断线重连机制。

---

## 10. 通信方式对比表

| 特性 | Fetch | Axios | WebSocket | SSE | 普通表单 |
|------|-------|-------|-----------|-----|---------|
| 通信方向 | 双向（请求-响应） | 双向（请求-响应） | 双向（实时） | 服务器→客户端 | 双向（刷新页面） |
| 传输协议 | HTTP | HTTP | ws:// / wss:// | HTTP | HTTP |
| 实时性 | 轮询（非实时） | 轮询（非实时） | 实时 | 实时 | 页面刷新 |
| 请求头自定义 | 支持 | 支持 | 握手后不可变 | 受限 | 不支持 |
| 自动重连 | 需手动实现 | 需手动实现 | 需手动实现 | 内置 | N/A |
| 二进制传输 | 支持（Blob） | 支持（Blob/ArrayBuffer） | 原生支持 | 不支持 | 不支持 |
| 服务端推送 | 不支持 | 不支持 | 支持 | 支持 | 不支持 |
| 连接开销 | 每次请求建立/复用 | 每次请求建立/复用 | 持久连接 | 持久连接 | 页面刷新 |
| 浏览器兼容 | 现代浏览器 | 所有浏览器（含 IE） | 现代浏览器 | 现代浏览器 | 所有浏览器 |
| 库体积 | 浏览器内置 | ~14KB min | 浏览器内置 | 浏览器内置 | N/A |

> 🎯 **选型建议**：
> - 90% 的 API 调用 → Axios（或 Fetch）
> - 服务器推送通知 → SSE
> - 即时通信/游戏 → WebSocket
> - 文件上传 → Axios（有进度追踪）
> - 文件下载 → Axios（支持 Blob）

---

## 11. 常见问题与排查

### 11.1 CORS 跨域拦截

```
浏览器控制台错误：
Access to fetch at 'https://api.example.com/data' 
from origin 'https://frontend.example.com' has been blocked by CORS policy
```

**原因**：浏览器的同源策略——不同协议、域名、端口的请求被限制。

**后端解决方案**：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://frontend.example.com")  // 指定允许的域名，不要用 *
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)       // 允许携带 Cookie
            .maxAge(3600);                // 预检请求缓存时间
    }
}
```

### 11.2 Content-Type 不匹配

```
❌ 前端发 JSON，后端期待 form 格式：
  POST /api/login 415 Unsupported Media Type
  
❌ 前端发表单，后端期待 JSON：
  POST /api/users 400 Bad Request（字段解析失败）
```

```javascript
// 前端排查——检查实际发送的 Content-Type
axios.interceptors.request.use(config => {
    console.log('发送请求:', {
        url: config.url,
        method: config.method,
        contentType: config.headers['Content-Type'],
        data: config.data
    });
    return config;
});
```

### 11.3 CSRF Token 缺失

```javascript
// 后端开启 CSRF 保护时，前端需要在请求中包含 token

// 从 Cookie 中读取 CSRF token
const csrfToken = document.cookie
    .split('; ')
    .find(row => row.startsWith('XSRF-TOKEN='))
    ?.split('=')[1];

// 在请求头中回传
await axios.post('/api/transfer', { amount: 1000 }, {
    headers: { 'X-XSRF-TOKEN': csrfToken }
});
```

### 11.4 GET 请求携带 Body

```javascript
// ❌ 错误：GET 请求不能有 body
await fetch('/api/search', {
    method: 'GET',
    body: JSON.stringify({ keyword: 'test' })  // 浏览器会忽略 body
});

// ✅ 正确：GET 使用 URL 参数
await fetch('/api/search?keyword=test');
// 或 Axios
await axios.get('/api/search', { params: { keyword: 'test' } });
```

### 11.5 排查清单

```
前端请求异常排查步骤：
□ 打开浏览器开发者工具 → Network 面板
□ 检查请求 URL 是否正确
□ 检查请求方法（GET/POST/PUT/DELETE）
□ 检查请求头 Content-Type 是否匹配
□ 检查是否携带了正确的 Authorization 头
□ 查看响应状态码（4xx/5xx）
□ 查看响应体中的错误信息
□ 检查控制台是否有 CORS 错误
□ 检查是否有预检（OPTIONS）请求及其响应
```

---

## 12. 综合示例：Axios 封装

一个企业级 Axios 封装，包含完整的请求/响应拦截、错误处理、Token 刷新和日志记录：

```javascript
// 📁 httpClient.js
import axios from 'axios';
import { message } from 'antd';  // 假设使用 Ant Design

class HttpClient {
    constructor(baseURL) {
        this.client = axios.create({
            baseURL,
            timeout: 15000,
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });

        this.setupInterceptors();
    }

    setupInterceptors() {
        // 请求拦截器
        this.client.interceptors.request.use(
            config => {
                // 添加 Token
                const token = localStorage.getItem('accessToken');
                if (token) {
                    config.headers.Authorization = `Bearer ${token}`;
                }

                // 添加请求追踪 ID
                config.headers['X-Request-Id'] = this.generateUUID();

                // 开发环境日志
                if (process.env.NODE_ENV === 'development') {
                    console.group(`[请求] ${config.method?.toUpperCase()} ${config.url}`);
                    console.log('Headers:', config.headers);
                    console.log('Params:', config.params);
                    console.log('Body:', config.data);
                    console.groupEnd();
                }

                return config;
            },
            error => Promise.reject(error)
        );

        // 响应拦截器
        this.client.interceptors.response.use(
            response => {
                // 成功响应日志
                if (process.env.NODE_ENV === 'development') {
                    console.log(`[响应] ${response.config.url} -> ${response.status}`);
                }

                // 直接返回 data，调用方直接拿到业务数据
                return response.data;
            },
            async error => {
                const originalRequest = error.config;

                // 网络错误（无响应）
                if (!error.response) {
                    message.error('网络连接失败，请检查网络');
                    return Promise.reject(error);
                }

                const { status, data } = error.response;

                // Token 过期——尝试刷新
                if (status === 401 && !originalRequest._retry) {
                    originalRequest._retry = true;

                    try {
                        const refreshToken = localStorage.getItem('refreshToken');
                        if (!refreshToken) {
                            throw new Error('No refresh token');
                        }

                        const response = await axios.post(
                            `${this.client.defaults.baseURL}/auth/refresh`,
                            { refreshToken }
                        );

                        const { accessToken, refreshToken: newRefreshToken } = response.data;
                        localStorage.setItem('accessToken', accessToken);
                        localStorage.setItem('refreshToken', newRefreshToken);

                        // 重试原始请求
                        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                        return this.client(originalRequest);

                    } catch (refreshError) {
                        // 刷新失败——清除登录信息，跳转登录页
                        localStorage.clear();
                        message.error('登录已过期，请重新登录');
                        window.location.href = '/login';
                        return Promise.reject(refreshError);
                    }
                }

                // 统一错误提示
                const errorMessages = {
                    400: '请求参数错误',
                    403: '无权限访问',
                    404: '请求的资源不存在',
                    405: '请求方法不允许',
                    409: '资源冲突',
                    429: '请求过于频繁，请稍后重试',
                    500: '服务器内部错误',
                    502: '网关错误',
                    503: '服务暂不可用',
                    504: '网关超时'
                };

                const errorMsg = data?.message || errorMessages[status] || '未知错误';
                message.error(errorMsg);

                // 开发环境详细日志
                if (process.env.NODE_ENV === 'development') {
                    console.error(`[错误] ${originalRequest.method?.toUpperCase()} ${originalRequest.url}`);
                    console.error('状态码:', status);
                    console.error('错误信息:', data);
                }

                return Promise.reject(error);
            }
        );
    }

    // HTTP 方法封装
    get(url, config) {
        return this.client.get(url, config);
    }

    post(url, data, config) {
        return this.client.post(url, data, config);
    }

    put(url, data, config) {
        return this.client.put(url, data, config);
    }

    delete(url, config) {
        return this.client.delete(url, config);
    }

    upload(url, formData, onProgress) {
        return this.client.post(url, formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
            onUploadProgress: onProgress,
            timeout: 300000
        });
    }

    download(url, filename) {
        return this.client.get(url, {
            responseType: 'blob'
        }).then(blob => {
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            link.click();
            window.URL.revokeObjectURL(url);
        });
    }

    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0;
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
    }
}

// 导出单例
const httpClient = new HttpClient('https://api.example.com/v2');
export default httpClient;
```

### 使用示例

```javascript
// 📁 userService.js
import httpClient from './httpClient';

export const userService = {
    // 查询用户列表
    getUsers(page = 1, size = 10) {
        return httpClient.get('/users', { params: { page, size } });
    },

    // 创建用户
    createUser(userData) {
        return httpClient.post('/users', userData);
    },

    // 上传头像（带进度）
    uploadAvatar(file, onProgress) {
        const formData = new FormData();
        formData.append('avatar', file);
        return httpClient.upload('/users/avatar', formData, onProgress);
    },

    // 导出用户报表
    exportUsers() {
        return httpClient.download('/users/export', 'users-report.xlsx');
    }
};

// 📁 App.js —— 实际调用
async function handleCreateUser() {
    try {
        const newUser = await userService.createUser({
            name: '张三',
            email: 'zhangsan@example.com',
            role: 'admin'
        });
        message.success(`用户 ${newUser.name} 创建成功`);
    } catch (error) {
        // 全局拦截器已处理了 401/500/网络错误等
        // 此处只需处理业务逻辑异常
        console.error('创建用户失败:', error);
    }
}
```

> 🎯 **前后端交互的核心原则**：
> 1. **统一封装**：像后端封装 RestTemplate/Feign 一样，前端统一封装 HTTP 客户端
> 2. **Token 管理**：拦截器自动处理 token 注入和刷新，业务代码无需关心
> 3. **错误收敛**：全局拦截器处理通用错误，业务代码只处理业务异常
> 4. **日志可追溯**：X-Request-Id 贯穿前后端，方便问题排查
> 5. **数据格式对齐**：前后端约定统一的请求/响应格式，避免 Content-Type 不匹配
