# 07 - 文件上传下载与 AJAX 交互

> 文件操作是 Web 开发的必修课——从表单上传到分片断点续传；AJAX 则是前后端分离的通信基石——从 XMLHttpRequest 到 Fetch API，从 JSON 到跨域处理。这一讲打通数据交互的任督二脉。

---

## 目录

1. [文件上传原理](#1-文件上传原理)
2. [Servlet 文件上传实战](#2-servlet-文件上传实战)
3. [文件下载实现](#3-文件下载实现)
4. [大文件分片上传与断点续传](#4-大文件分片上传与断点续传)
5. [AJAX 基础与 XMLHttpRequest](#5-ajax-基础与-xmlhttprequest)
6. [Fetch API 现代方案](#6-fetch-api-现代方案)
7. [JSON 数据处理](#7-json-数据处理)
8. [跨域问题（CORS）与解决方案](#8-跨域问题cors与解决方案)
9. [前后端交互实战](#9-前后端交互实战)
10. [常见面试题](#10-常见面试题)

---

## 1. 文件上传原理

### 1.1 HTTP 文件上传协议

```
普通表单（application/x-www-form-urlencoded）:
  POST /upload HTTP/1.1
  Content-Type: application/x-www-form-urlencoded

  username=zhangsan&age=25
  → 参数在 Body 中，key=value 格式

文件上传表单（multipart/form-data）:
  POST /upload HTTP/1.1
  Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxk

  ------WebKitFormBoundary7MA4YWxk
  Content-Disposition: form-data; name="username"

  zhangsan
  ------WebKitFormBoundary7MA4YWxk
  Content-Disposition: form-data; name="file"; filename="photo.jpg"
  Content-Type: image/jpeg

  <二进制文件内容>
  ------WebKitFormBoundary7MA4YWxk--
```

> 💡 `boundary` 是随机分隔符，用于分隔多个表单字段和文件。每个 `part` 都有自己的 Content-Disposition 描述。

### 1.2 前端表单

```html
<!-- 文件上传表单三要素 -->
<form action="/app/upload"
      method="post"
      enctype="multipart/form-data">  <!-- ⭐ 必须设置 -->

    <!-- 普通表单字段 -->
    <input type="text" name="description" placeholder="文件描述"/>

    <!-- 文件选择 — multiple 允许选择多个文件 -->
    <input type="file" name="file" accept="image/*"/>

    <!-- accept 限制可上传的文件类型 -->
    <!-- accept="image/*"           → 所有图片 -->
    <!-- accept=".pdf,.doc,.docx"   → 指定扩展名 -->
    <!-- accept="image/png,image/jpeg" → 指定 MIME 类型 -->

    <button type="submit">上传</button>
</form>
```

---

## 2. Servlet 文件上传实战

### 2.1 Servlet 3.0+ @MultipartConfig

```java
@WebServlet("/upload")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,     // 1MB — 超过后写入临时文件而非内存
    maxFileSize = 1024 * 1024 * 10,      // 10MB — 单个文件最大
    maxRequestSize = 1024 * 1024 * 50,   // 50MB — 整个请求最大
    location = "/tmp/upload_tmp"         // 临时文件存储目录
)
public class FileUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // ⚠️ POST + multipart 下 getParameter() 可用（Servlet 3.0+ 自动处理）
        String description = req.getParameter("description");

        // ═══ 方式一：获取单个文件 ═══
        Part filePart = req.getPart("file");  // 参数为表单中 input 的 name
        if (filePart != null && filePart.getSize() > 0) {
            String fileName = extractFileName(filePart);
            String savePath = "/data/uploads/" + generateUniqueName(fileName);

            // 写入磁盘
            filePart.write(savePath);

            // 获取文件元信息
            String contentType = filePart.getContentType();    // image/jpeg
            long size = filePart.getSize();                    // 字节数
            String header = filePart.getHeader("Content-Disposition");
        }

        // ═══ 方式二：获取所有部件 ═══
        for (Part part : req.getParts()) {
            if (part.getSubmittedFileName() != null) {
                // 是文件
                String fileName = part.getSubmittedFileName();
                part.write("/uploads/" + fileName);
                System.out.println("上传文件: " + fileName
                    + " (" + part.getSize() + " bytes)");
            } else {
                // 是普通字段
                String fieldValue = req.getParameter(part.getName());
                System.out.println("字段: " + part.getName() + " = " + fieldValue);
            }
        }

        resp.getWriter().write("上传成功");
    }

    // 从 Content-Disposition 头中提取原始文件名
    private String extractFileName(Part part) {
        // Content-Disposition: form-data; name="file"; filename="photo.jpg"
        String header = part.getHeader("Content-Disposition");
        for (String token : header.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 2,
                                       token.length() - 1);
            }
        }
        return null;
    }

    // 或用 Part 的便捷方法（Servlet 3.0+）
    // filePart.getSubmittedFileName();  // 直接获取原始文件名

    // 生成唯一文件名（防止覆盖 + 防止路径遍历攻击）
    private String generateUniqueName(String originalName) {
        String ext = originalName.substring(originalName.lastIndexOf("."));
        return UUID.randomUUID().toString() + ext;
    }
}
```

### 2.2 文件上传安全要点

```java
public class FileUploadSecurity {

    // ⚠️ 安全校验清单
    public void validate(Part filePart, String fileName) {

        // 1. 文件名非空
        if (fileName == null || fileName.isEmpty()) {
            throw new BusinessException("文件名为空");
        }

        // 2. 后缀名白名单（⭐ 关键防御）
        String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif",
                                          ".pdf", ".doc", ".docx", ".xlsx");
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("不支持的文件类型: " + ext);
        }

        // 3. MIME 类型校验（辅助，但可伪造，不能只靠这个）
        String contentType = filePart.getContentType();
        Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/png", "image/gif",
            "application/pdf", "application/msword");
        if (!ALLOWED_MIME.contains(contentType)) {
            throw new BusinessException("MIME 类型非法: " + contentType);
        }

        // 4. 魔数校验（⭐ 最可靠——读取文件头几个字节判断真实类型）
        // 防 .exe 改名为 .jpg 上传
        // JPEG: FF D8 FF E0  PNG: 89 50 4E 47  GIF: 47 49 46 38
        InputStream is = filePart.getInputStream();
        byte[] magic = new byte[4];
        is.read(magic);
        String hex = bytesToHex(magic);

        // 5. 文件大小校验
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (filePart.getSize() > maxSize) {
            throw new BusinessException("文件大小超限，最大 " + maxSize / 1024 / 1024 + "MB");
        }

        // 6. 防止路径遍历攻击
        // 文件名可能是 ../../../etc/passwd → 只用文件名部分
        fileName = new File(fileName).getName();
        // 或直接生成新文件名（推荐）
        fileName = UUID.randomUUID().toString() + ext;
    }
}
```

---

## 3. 文件下载实现

### 3.1 基本下载

```java
@WebServlet("/download")
public class FileDownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String fileName = req.getParameter("file");
        String filePath = "/data/uploads/" + fileName;

        File file = new File(filePath);
        if (!file.exists()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        // ═══ 设置响应头 ═══
        // 1. MIME 类型
        String mimeType = getServletContext().getMimeType(fileName);
        if (mimeType == null) {
            mimeType = "application/octet-stream";  // 未知类型默认
        }
        resp.setContentType(mimeType);

        // 2. 文件大小（便于浏览器显示进度）
        resp.setContentLengthLong(file.length());

        // 3. Content-Disposition
        // inline  → 浏览器尝试直接打开（图片/PDF）
        // attachment → 强制弹出下载对话框
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8")
            .replace("+", "%20");  // 空格处理
        resp.setHeader("Content-Disposition",
            "attachment; filename=\"" + encodedFileName + "\"");
        // 处理中文文件名（兼容各种浏览器）:
        // filename*=UTF-8''%e6%96%87%e4%bb%b6.pdf  ← RFC 5987 标准

        // ═══ 写入文件数据 ═══
        try (FileInputStream fis = new FileInputStream(file);
             ServletOutputStream os = resp.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }
}
```

### 3.2 支持断点续传的下载

```java
@WebServlet("/download/resume")
public class ResumeDownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String fileName = req.getParameter("file");
        File file = new File("/data/uploads/" + fileName);
        long fileLength = file.length();

        // 解析 Range 头：Range: bytes=1024-2047
        String rangeHeader = req.getHeader("Range");
        long start = 0, end = fileLength - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String rangeValue = rangeHeader.substring(6);
            String[] parts = rangeValue.split("-");
            start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }
        }

        long contentLength = end - start + 1;

        // 设置 206 Partial Content 状态
        resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        resp.setContentType("application/octet-stream");
        resp.setContentLengthLong(contentLength);
        resp.setHeader("Content-Range",
            "bytes " + start + "-" + end + "/" + fileLength);
        resp.setHeader("Accept-Ranges", "bytes");

        // 跳过 start 之前的字节
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             ServletOutputStream os = resp.getOutputStream()) {
            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = raf.read(buffer, 0, toRead);
                if (read == -1) break;
                os.write(buffer, 0, read);
                remaining -= read;
            }
            os.flush();
        }
    }
}
```

### 3.3 文件预览（inline）

```java
// PDF/图片 直接在线预览而非下载
resp.setHeader("Content-Disposition",
    "inline; filename=\"" + encodedFileName + "\"");
// inline → 浏览器尝试直接打开（需浏览器支持该文件类型）
```

---

## 4. 大文件分片上传与断点续传

### 4.1 整体架构

```
┌─────────┐     ┌──────────────┐     ┌──────────────┐
│  前端     │────>│  上传接口     │────>│  存储层        │
│          │     │              │     │              │
│ 1. 切片   │     │ /upload/chunk│     │ /tmp/chunks/ │
│ 2. 并发   │     │ /upload/merge│     │ /uploads/    │
│ 3. 重试   │     │ /upload/check│     │              │
└─────────┘     └──────────────┘     └──────────────┘
```

### 4.2 前端切片上传（JavaScript）

```javascript
// 文件切片与上传
async function uploadFile(file) {
    const CHUNK_SIZE = 5 * 1024 * 1024;  // 5MB 每片
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
    const fileId = generateFileId(file);  // MD5 文件指纹

    // 1. 检查服务端已上传的分片
    const uploaded = await fetch(`/upload/check?fileId=${fileId}`)
        .then(r => r.json());
    // uploaded.chunks = [0, 1, 3] — 已上传的片索引

    // 2. 并发上传未完成的分片
    const promises = [];
    for (let i = 0; i < totalChunks; i++) {
        if (uploaded.chunks.includes(i)) continue;  // 跳过已上传的

        const start = i * CHUNK_SIZE;
        const end = Math.min(start + CHUNK_SIZE, file.size);
        const chunk = file.slice(start, end);  // Blob.slice()

        const formData = new FormData();
        formData.append('fileId', fileId);
        formData.append('chunkIndex', i);
        formData.append('totalChunks', totalChunks);
        formData.append('fileName', file.name);
        formData.append('chunk', chunk);

        promises.push(
            fetch('/upload/chunk', { method: 'POST', body: formData })
                .then(r => r.json())
        );
    }

    // 3. 等待所有分片上传完成
    await Promise.all(promises);

    // 4. 发起合并请求
    const result = await fetch('/upload/merge', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            fileId: fileId,
            fileName: file.name,
            totalChunks: totalChunks
        })
    }).then(r => r.json());

    console.log('上传完成:', result);
}

// 生成文件指纹（SparkMD5 库）
function generateFileId(file) {
    // 方式一：大文件用采样 MD5（取首尾+中间若干段）
    // 方式二：小文件全量 MD5
    // 方式三：fileName + size + lastModified 组合（不可靠，但简单）
    return file.name + '_' + file.size + '_' + file.lastModified;
}
```

### 4.3 服务端分片接收与合并

```java
@WebServlet("/upload/chunk")
@MultipartConfig(fileSizeThreshold = 1024 * 1024,
                 maxFileSize = 1024 * 1024 * 10)  // 单分片最大 10MB
public class ChunkUploadServlet extends HttpServlet {

    private static final String CHUNK_DIR = "/data/uploads/chunks/";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String fileId = req.getParameter("fileId");
        int chunkIndex = Integer.parseInt(req.getParameter("chunkIndex"));

        // 分片存储目录：/chunks/{fileId}/
        File chunkDir = new File(CHUNK_DIR + fileId);
        if (!chunkDir.exists()) chunkDir.mkdirs();

        // 保存分片
        Part chunk = req.getPart("chunk");
        File chunkFile = new File(chunkDir, String.valueOf(chunkIndex));
        chunk.write(chunkFile.getAbsolutePath());

        resp.setContentType("application/json");
        resp.getWriter().write("{\"status\":\"ok\",\"chunk\":" + chunkIndex + "}");
    }
}

@WebServlet("/upload/check")
public class ChunkCheckServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String fileId = req.getParameter("fileId");
        File chunkDir = new File(CHUNK_DIR + fileId);

        List<Integer> uploadedChunks = new ArrayList<>();
        if (chunkDir.exists()) {
            for (File f : chunkDir.listFiles()) {
                uploadedChunks.add(Integer.parseInt(f.getName()));
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(new Gson().toJson(
            Map.of("chunks", uploadedChunks)));
    }
}

@WebServlet("/upload/merge")
public class ChunkMergeServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // 解析 JSON 请求体
        JsonObject json = new Gson().fromJson(req.getReader(), JsonObject.class);
        String fileId = json.get("fileId").getAsString();
        String fileName = json.get("fileName").getAsString();
        int totalChunks = json.get("totalChunks").getAsInt();

        File chunkDir = new File(CHUNK_DIR + fileId);
        File outputFile = new File("/data/uploads/" + UUID.randomUUID() + "_" + fileName);

        // 按序号合并所有分片
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             FileChannel outChannel = fos.getChannel()) {

            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(chunkDir, String.valueOf(i));
                if (!chunkFile.exists()) {
                    resp.getWriter().write("{\"error\":\"分片 " + i + " 缺失\"}");
                    return;
                }
                try (FileInputStream fis = new FileInputStream(chunkFile);
                     FileChannel inChannel = fis.getChannel()) {
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                }
            }
        }

        // 清理分片目录
        deleteDirectory(chunkDir);

        resp.setContentType("application/json");
        resp.getWriter().write("{\"status\":\"ok\",\"path\":\""
            + outputFile.getAbsolutePath() + "\"}");
    }
}
```

---

## 5. AJAX 基础与 XMLHttpRequest

### 5.1 什么是 AJAX

> AJAX（Asynchronous JavaScript And XML）允许网页通过 JavaScript 异步发送 HTTP 请求，**不刷新页面**即可更新部分内容。名称含 XML 但现代实践中数据格式以 **JSON** 为主。

```
传统请求（同步）：                         AJAX（异步）：
┌─────────────────────────┐              ┌─────────────────────────┐
│  用户点击 → 整页刷新      │              │  用户点击 → JS发请求     │
│  → 等待 → 整页重新渲染    │              │  → 页面不刷新             │
│  → 页面闪烁、表单丢失     │              │  → 仅更新变化的部分       │
└─────────────────────────┘              └─────────────────────────┘
```

### 5.2 XMLHttpRequest 完整用法

```javascript
// ═══ 基础 GET 请求 ═══
const xhr = new XMLHttpRequest();
xhr.open('GET', '/api/users?page=1&size=20', true);  // true = 异步

// 设置请求头
xhr.setRequestHeader('Accept', 'application/json');

// 监听状态变化
xhr.onreadystatechange = function() {
    // readyState 值：
    // 0: UNSENT — open() 未调用
    // 1: OPENED — open() 已调用
    // 2: HEADERS_RECEIVED — 接收到响应头
    // 3: LOADING — 正在接收响应体
    // 4: DONE — 请求完成

    if (xhr.readyState === 4) {
        if (xhr.status >= 200 && xhr.status < 300) {
            const data = JSON.parse(xhr.responseText);
            console.log('成功:', data);
        } else {
            console.error('请求失败:', xhr.status, xhr.statusText);
        }
    }
};

// 发送请求
xhr.send();  // GET 无 body


// ═══ POST 请求（发送 JSON） ═══
const xhr2 = new XMLHttpRequest();
xhr2.open('POST', '/api/users', true);
xhr2.setRequestHeader('Content-Type', 'application/json');
xhr2.setRequestHeader('Authorization', 'Bearer ' + token);

xhr2.onload = function() {  // 简写：readyState === 4
    if (xhr2.status === 200 || xhr2.status === 201) {
        console.log('创建成功:', JSON.parse(xhr2.responseText));
    }
};
xhr2.onerror = function() {
    console.error('网络错误');
};

const body = JSON.stringify({ name: '张三', email: 'zhangsan@example.com' });
xhr2.send(body);


// ═══ 上传进度监听 ═══
xhr2.upload.onprogress = function(e) {
    if (e.lengthComputable) {
        const percent = Math.round((e.loaded / e.total) * 100);
        console.log(`上传进度: ${percent}%`);
    }
};

// ═══ 超时设置 ═══
xhr2.timeout = 10000;  // 10 秒
xhr2.ontimeout = function() {
    console.error('请求超时');
};
```

### 5.3 XMLHttpRequest Level 2 新特性

| 特性 | API | 说明 |
|------|-----|------|
| 超时 | `xhr.timeout` | 设置超时毫秒数 |
| 上传进度 | `xhr.upload.onprogress` | 文件上传进度条 |
| FormData | `xhr.send(formData)` | 直接发送表单（含文件） |
| 跨域 | `xhr.withCredentials` | 携带 Cookie |
| 二进制 | `xhr.responseType = 'blob'` | 下载文件 |

---

## 6. Fetch API 现代方案

### 6.1 Fetch 基础

```javascript
// Fetch 是 XMLHttpRequest 的现代替代，基于 Promise

// ═══ GET 请求 ═══
fetch('/api/users?page=1')
    .then(response => {
        if (!response.ok) {  // status 不在 200-299
            throw new Error(`HTTP ${response.status}`);
        }
        return response.json();  // 解析 JSON
    })
    .then(data => {
        console.log('用户列表:', data);
    })
    .catch(error => {
        console.error('请求失败:', error);
    });

// ═══ async/await 风格（推荐） ═══
async function getUsers() {
    try {
        const response = await fetch('/api/users');
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('请求失败:', error);
        return null;
    }
}
```

### 6.2 Fetch 各种请求模式

```javascript
// ═══ POST JSON ═══
const user = await fetch('/api/users', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify({ name: '张三', email: 'zhangsan@example.com' })
}).then(r => r.json());

// ═══ 上传文件（FormData） ═══
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('description', '测试文件');

const uploadResult = await fetch('/upload', {
    method: 'POST',
    body: formData
    // ⚠️ 不要设置 Content-Type！浏览器会自动设 multipart/form-data + boundary
}).then(r => r.json());

// ═══ 下载文件（Blob） ═══
const response = await fetch('/download?file=report.pdf');
const blob = await response.blob();
const url = URL.createObjectURL(blob);
const a = document.createElement('a');
a.href = url;
a.download = 'report.pdf';
a.click();
URL.revokeObjectURL(url);  // 释放内存

// ═══ 携带 Cookie（跨域时） ═══
fetch('/api/user', {
    credentials: 'include'  // same-origin | include | omit
});

// ═══ 中止请求 ═══
const controller = new AbortController();
const timeoutId = setTimeout(() => controller.abort(), 5000);

try {
    const response = await fetch('/api/slow', {
        signal: controller.signal
    });
    clearTimeout(timeoutId);
} catch (err) {
    if (err.name === 'AbortError') {
        console.log('请求被取消');
    }
}
```

### 6.3 Fetch vs XMLHttpRequest

| 维度 | XMLHttpRequest | Fetch |
|------|---------------|-------|
| **语法** | 回调（onreadystatechange） | Promise（支持 async/await） |
| **请求/响应** | 混在一起 | 分离的 Request / Response 对象 |
| **JSON 解析** | 手动 `JSON.parse()` | `response.json()` |
| **进度事件** | ✅ 支持上传/下载进度 | ❌ 不直接支持（需 ReadableStream） |
| **超时** | `xhr.timeout` | 需手动用 AbortController |
| **取消请求** | `xhr.abort()` | AbortController |
| **Cookie** | 默认携带 | 默认不携带（`credentials: 'include'`） |
| **错误处理** | `onerror` | 只有网络错误才 reject，HTTP 4xx/5xx 算成功 |
| **浏览器支持** | 所有浏览器 | IE 不支持（需 polyfill） |

---

## 7. JSON 数据处理

### 7.1 Java 端 JSON 处理

```java
// ═══ Jackson (Spring Boot 默认) ═══
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();

// Java → JSON（序列化）
User user = new User("张三", "zhangsan@example.com");
String json = mapper.writeValueAsString(user);
// → {"name":"张三","email":"zhangsan@example.com"}

// JSON → Java（反序列化）
User user2 = mapper.readValue(json, User.class);
List<User> users = mapper.readValue(jsonArray,
    new TypeReference<List<User>>() {});

// 忽略未知属性（容错）
mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

// 日期格式化
mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));


// ═══ Gson (Google) ═══
import com.google.gson.Gson;

Gson gson = new Gson();
String json = gson.toJson(user);                  // 序列化
User user2 = gson.fromJson(json, User.class);     // 反序列化
List<User> users = gson.fromJson(json,
    new TypeToken<List<User>>(){}.getType());


// ═══ Fastjson2 (阿里巴巴，高性能) ═══
import com.alibaba.fastjson2.JSON;

String json = JSON.toJSONString(user);
User user2 = JSON.parseObject(json, User.class);
List<User> users = JSON.parseArray(json, User.class);
```

### 7.2 读取请求中的 JSON Body

```java
// Servlet 中读取 JSON 请求体（不能通过 getParameter 获取）
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
    // 读取 Body
    String body = req.getReader().lines()
        .collect(Collectors.joining(System.lineSeparator()));

    // 解析 JSON
    ObjectMapper mapper = new ObjectMapper();
    User user = mapper.readValue(body, User.class);

    // 业务处理
    userService.save(user);

    // 返回 JSON
    resp.setContentType("application/json;charset=UTF-8");
    Result<User> result = Result.success(user);
    resp.getWriter().write(mapper.writeValueAsString(result));
}
```

### 7.3 日期时间格式化约定

```java
// 业界规范：API 传输使用 ISO 8601 格式
// "2024-01-15T10:30:00+08:00"  或  "2024-01-15T10:30:00Z"

// Jackson 配置
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());  // Java 8 时间支持
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 不用时间戳
mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));

// 或使用注解
public class User {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
```

---

## 8. 跨域问题（CORS）与解决方案

### 8.1 什么是跨域

```
同源策略（Same-Origin Policy）：
协议 + 域名 + 端口 → 三者完全一致才算同源

http://example.com:8080/app/index.html
  ├── http://example.com:8080/api/users     ✅ 同源
  ├── http://example.com:8081/api/users     ❌ 端口不同
  ├── https://example.com:8080/api/users    ❌ 协议不同
  └── http://api.example.com:8080/users     ❌ 域名不同
```

### 8.2 CORS 响应头

```java
// ═══ 简单请求（GET/POST + 特定 Content-Type）═══
resp.setHeader("Access-Control-Allow-Origin", "https://example.com");
// 或 "*" 允许所有源（不能与 credentials 同用）

// ═══ 携带 Cookie 的跨域 ═══
resp.setHeader("Access-Control-Allow-Origin", "https://example.com");
resp.setHeader("Access-Control-Allow-Credentials", "true");
// 前端需设：fetch(url, { credentials: 'include' })

// ═══ 预检请求（OPTIONS）— 非简单请求时浏览器自动发 ═══
resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
resp.setHeader("Access-Control-Max-Age", "3600");  // 预检缓存时间（秒）

// ═══ 暴露自定义响应头 ═══
resp.setHeader("Access-Control-Expose-Headers", "X-Total-Count, X-Custom-Header");
```

### 8.3 完整 CORS Filter

```java
@WebFilter("/*")
public class CorsFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 允许的源（生产环境应配置白名单，不使用 *）
        String origin = req.getHeader("Origin");
        if (isAllowedOrigin(origin)) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
        }
        resp.setHeader("Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers",
            "Content-Type, Authorization, X-Requested-With");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Max-Age", "3600");

        // OPTIONS 预检请求直接返回 200
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAllowedOrigin(String origin) {
        // 从配置文件或数据库读取白名单
        // return ALLOWED_ORIGINS.contains(origin);
        return origin != null;  // 简化示例
    }
}
```

### 8.4 JSONP（历史遗留方案）

```javascript
// JSONP 利用 <script> 标签不受同源策略限制
// ❌ 只支持 GET 请求，有 XSS 风险，已基本淘汰

// 前端：
function handleResponse(data) {
    console.log('获得数据:', data);
}
const script = document.createElement('script');
script.src = 'http://other-domain.com/api/data?callback=handleResponse';
document.body.appendChild(script);

// 后端返回：
// handleResponse({"name": "张三", "age": 25});
```

### 8.5 跨域方案总结

| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **CORS** | 服务器设置响应头允许跨域 | W3C 标准，安全可控 | 需要服务端配合 |
| **JSONP** | `<script>` 不受同源限制 | 兼容老浏览器 | 只支持 GET，有 XSS 风险 |
| **代理** | 同源服务器转发请求 | 前端无感 | 需要额外代理服务 |
| **Nginx 反向代理** | 统一域名 | 生产环境最佳实践 | 需要配置 Nginx |

```nginx
# Nginx 反向代理解决跨域（生产环境推荐）
server {
    listen 80;
    server_name www.example.com;

    location /api/ {
        proxy_pass http://backend-server:8080/api/;
    }
    location / {
        root /var/www/frontend;
    }
}
# 前端访问 /api/users → Nginx 转发到 backend-server:8080/api/users → 同源！
```

---

## 9. 前后端交互实战

### 9.1 登录页面完整交互

```javascript
// ═══ 前端：登录表单 AJAX 提交 ═══
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();  // 阻止表单默认提交

    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData);  // → {username: "zhang", password: "123"}

    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.status === 401) {
            showError('用户名或密码错误');
            return;
        }

        if (!response.ok) throw new Error('服务器错误');

        const result = await response.json();
        if (result.code === 200) {
            // 保存 token
            localStorage.setItem('token', result.data.token);
            // 跳转到首页
            window.location.href = '/index.html';
        }
    } catch (error) {
        showError('网络错误，请稍后重试');
    }
});
```

### 9.2 带 Token 的 API 调用封装

```javascript
// ═══ 封装统一请求函数 ═══
const api = {
    async request(url, options = {}) {
        const token = localStorage.getItem('token');

        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
                ...(token && { 'Authorization': `Bearer ${token}` })
            }
        };

        const response = await fetch(url, {
            ...defaultOptions,
            ...options,
            headers: {
                ...defaultOptions.headers,
                ...options.headers
            }
        });

        // Token 过期处理
        if (response.status === 401) {
            localStorage.removeItem('token');
            window.location.href = '/login.html';
            throw new Error('登录已过期');
        }

        const result = await response.json();
        if (result.code !== 200) {
            throw new Error(result.message || '请求失败');
        }

        return result.data;
    },

    get(url, params) {
        const query = params ? '?' + new URLSearchParams(params) : '';
        return this.request(url + query);
    },

    post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    upload(url, formData) {
        const token = localStorage.getItem('token');
        return fetch(url, {
            method: 'POST',
            headers: token ? { 'Authorization': `Bearer ${token}` } : {},
            body: formData  // 不设 Content-Type，让浏览器自动处理
        }).then(r => r.json());
    }
};

// ═══ 使用示例 ═══
const users = await api.get('/api/users', { page: 1, size: 20 });
await api.post('/api/users', { name: '张三', email: 'zhangsan@example.com' });
const result = await api.upload('/upload', formData);
```

### 9.3 分页数据加载

```javascript
// ═══ 无限滚动加载 ═══
async function loadUsers(page = 1) {
    const data = await api.get('/api/users', { page, size: 20 });
    renderUserList(data.list);  // 渲染用户列表
    renderPagination(data.total, data.page, data.size);  // 渲染分页
}

// ═══ 服务端分页 Servlet ═══
@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    int page = Integer.parseInt(req.getParameter("page") != null ?
        req.getParameter("page") : "1");
    int size = Integer.parseInt(req.getParameter("size") != null ?
        req.getParameter("size") : "20");

    // 分页查询（使用 LIMIT 和 COUNT）
    List<User> list = userService.findByPage(page, size);
    long total = userService.count();

    Map<String, Object> result = Map.of(
        "list", list,
        "total", total,
        "page", page,
        "size", size,
        "pages", (total + size - 1) / size  // 总页数
    );

    resp.setContentType("application/json;charset=UTF-8");
    resp.getWriter().write(new Gson().toJson(Result.success(result)));
}
```

---

## 10. 常见面试题

### Q1：文件上传时 enctype 为什么必须是 multipart/form-data？

> 默认的 `application/x-www-form-urlencoded` 只适合 key=value 文本参数。`multipart/form-data` 用 boundary 分隔符包裹二进制内容，支持文件流传输。详见第1节。

### Q2：如何防止文件上传漏洞？

> 后缀名白名单 + MIME 类型校验 + 文件魔数校验 + 文件大小限制 + 防止路径遍历（去掉路径部分、重命名）。详见第2.2节。

### Q3：AJAX 的原理是什么？如何实现异步？

> 浏览器提供 XMLHttpRequest / Fetch API，JavaScript 发送 HTTP 请求后不阻塞主线程（通过事件循环机制），响应到达后触发回调/Promise resolve。详见第5节。

### Q4：Fetch 和 XMLHttpRequest 的区别？

> Fetch 基于 Promise（语法优雅、async/await 友好），XMLHttpRequest 基于回调。XHR 支持上传进度监听（`xhr.upload.onprogress`），Fetch 不直接支持。详见第6.3节。

### Q5：跨域是什么？如何解决？

> 浏览器同源策略限制不同源（协议+域名+端口）之间的 AJAX 请求。解决方案：CORS（服务端设置响应头，推荐）、Nginx 反向代理（生产环境）、JSONP（历史方案，仅 GET）。详见第8节。

### Q6：大文件上传如何实现断点续传？

> 前端将文件切片 → 计算文件指纹 → 并发上传分片 → 后端保存分片 → 全部完成后合并。已上传的分片跳过（通过 check 接口确认）。详见第4节。
