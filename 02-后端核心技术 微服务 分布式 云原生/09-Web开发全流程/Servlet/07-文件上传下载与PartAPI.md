# Servlet 文件上传下载与 Part API
> multipart 报文解析、@MultipartConfig 四参数、Part API 全解、大文件流式处理与下载头——以及路径穿越等上传安全必修课

## 📚 目录
1. [上传原理：multipart/form-data 报文](#1-上传原理multipartform-data-报文)
2. [@MultipartConfig 四参数](#2-multipartconfig-四参数)
3. [Part API 全解](#3-part-api-全解)
4. [多文件与表单字段混用](#4-多文件与表单字段混用)
5. [大文件流式处理](#5-大文件流式处理)
6. [文件下载与响应头](#6-文件下载与响应头)
7. [上传安全：路径穿越与消毒](#7-上传安全路径穿越与消毒)
8. [与 Spring MVC 的对比](#8-与-spring-mvc-的对比)

## 1. 上传原理：multipart/form-data 报文

```http
POST /upload HTTP/1.1
Host: localhost:8080
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxk

------WebKitFormBoundary7MA4YWxk
Content-Disposition: form-data; name="desc"          ← 普通字段
Content-Type: text/plain

用户备注
------WebKitFormBoundary7MA4YWxk
Content-Disposition: form-data; name="file"; filename="photo.jpg"  ← 文件字段
Content-Type: image/jpeg

<二进制文件内容>
------WebKitFormBoundary7MA4YWxk--
```

| 要点 | 说明 |
|------|------|
| boundary | 分隔符，随机生成，作为 Content-Type 参数下发 |
| 每段两个头 | `Content-Disposition`（name/filename）+ `Content-Type` |
| 普通字段 | 无 filename → 归入 `getParameter` |
| 文件字段 | 有 filename → 走 `Part` API（Servlet 3.0 起，`getParameter` 对文件返回 null） |

> 💡 Servlet 3.0 之前没有原生上传 API（只能解析 `getInputStream()` 手动拆报文，或用 Commons FileUpload）——Part API 是规范层对"手动解析 multipart"的终结。

## 2. @MultipartConfig 四参数

```java
@WebServlet("/upload")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,       // 1MB：超过则落盘（低于时驻内存）
    maxFileSize = 1024 * 1024 * 10,        // 单文件最大 10MB
    maxRequestSize = 1024 * 1024 * 50,     // 整个请求最大 50MB（防多文件组合超限）
    location = "/data/tmp"                 // 落盘临时目录（可省略）
)
public class UploadServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Part file = req.getPart("file");   // 未声明 @MultipartConfig 时返回 null
        // ...
    }
}
```

| 参数 | 默认值 | 语义 |
|------|:---:|------|
| `fileSizeThreshold` | 0（全驻内存） | 达到阈值前存内存，超过后写临时文件 |
| `maxFileSize` | -1（无限制） | 单文件上限，超限抛 `IllegalStateException` |
| `maxRequestSize` | -1（无限制） | 请求总大小上限（含所有文件与字段） |
| `location` | 空 | 临时文件目录（`Part.write()` 相对路径的基准） |

> ⚠️ **阈值设计**：小文件多 → 调大 `fileSizeThreshold` 减少磁盘 IO；大文件多 → 调小阈值防止大对象撑爆堆内存（尤其容器堆只有几百 MB 时，10MB 文件 × 100 并发 = 1GB 内存）。

## 3. Part API 全解

```java
// 获取方式
Part file = req.getPart("file");        // 按表单字段名取单个
Collection<Part> parts = req.getParts(); // 全部（字段 + 文件）

// 元数据
String fileName = file.getSubmittedFileName();  // 原始文件名（3.1+）
long size = file.getSize();                     // 字节数
String contentType = file.getContentType();     // image/jpeg
String fieldName = file.getName();              // 表单字段名（"file"）
InputStream in = file.getInputStream();         // 读取内容

// 落盘（最常用）
file.write("/uploads/" + fileName);   // 相对路径基于 location；绝对路径直接写
// ⚠️ write() 内部自动关闭流；重复调用报错

// 清理临时文件（大文件落盘后必须）
file.delete();
```

> ⚠️ **getSubmittedFileName() 返回的是客户端原始文件名**——这是路径穿越攻击的入口，见 §7。`getParts()` 返回的集合**包含普通字段**（无 filename 的 Part），判断文件：`part.getSubmittedFileName() != null`。

## 4. 多文件与表单字段混用

```html
<form action="/upload" method="post" enctype="multipart/form-data">
    <input type="text" name="title">                       <!-- 普通字段 -->
    <input type="file" name="images" multiple>             <!-- 多文件，同名 -->
    <input type="file" name="images">
    <input type="file" name="avatar">                      <!-- 单文件 -->
</form>
```

```java
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    String title = req.getParameter("title");              // 字段照常取

    // 多文件：同名 Part 是多个，逐个落盘
    for (Part part : req.getParts()) {
        if (part.getSubmittedFileName() == null) continue; // 跳过普通字段
        String safeName = sanitize(part.getSubmittedFileName());
        part.write("/uploads/" + safeName);
    }
}
```

## 5. 大文件流式处理

> 🎯 核心原则：**永远不要 `readAllBytes()` 或 `Files.copy(inputStream)` 到内存**——大文件上传是流式的，逐块搬移即可，内存占用恒定。

```java
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    Part part = req.getPart("file");
    String safe = sanitize(part.getSubmittedFileName());

    try (InputStream in = part.getInputStream();
         OutputStream out = Files.newOutputStream(
                 Path.of("/data/uploads", safe),
                 StandardOpenOption.CREATE_NEW)) {

        // 逐块搬移：8KB 缓冲，内存恒定
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }
    resp.getWriter().write("ok");
}
```

> 💡 进阶：大文件可配 `fileSizeThreshold` 让容器先落临时盘，再 `part.write()` 或直接 `Files.move` 到目标目录（避免一次 `getInputStream` 全读内存）。超大文件（GB 级）建议绕过 Servlet 走对象存储直传（前端直传 OSS/S3 预签名 URL），别让应用服务器成为带宽瓶颈。

## 6. 文件下载与响应头

```java
@WebServlet("/download")
public class DownloadServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // 1. 头部三件套
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + asciiName + "\"");

        // 2. 中文字符文件名：RFC 5987 编码（避免中文乱码/被截断）
        String encoded = URLEncoder.encode("报告.pdf", StandardCharsets.UTF_8);
        resp.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encoded);

        // 3. 内容长度（可缓存/进度条必要）
        resp.setContentLengthLong(file.length());

        // 4. 流式输出
        try (InputStream in = Files.newInputStream(file.toPath());
             ServletOutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        }
    }
}
```

| 头 | 作用 |
|----|------|
| `Content-Disposition: attachment` | 强制下载（浏览器不预览）；`inline` 则预览 |
| `filename` / `filename*` | ASCII 名 / RFC 5987 中文名（现代浏览器认后者） |
| `Content-Length` | 流式下载前设置，支持进度条与断点续传服务端判断 |
| `Accept-Ranges: bytes` | 声明支持 Range 分片（配合 206 实现断点续传） |

> ⚠️ 下载安全：**别用未校验的原始文件名直接拼 Content-Disposition**——`filename="../../etc/passwd"` 或注入 `";` 可被部分代理/杀软误判甚至注入响应头（CRLF 注入）。

## 7. 上传安全：路径穿越与消毒

### 7.1 路径穿越（最典型攻击）

```text
攻击者提交 filename="../../../etc/passwd"
如果直接 part.write("/uploads/" + fileName)
→ 实际写入路径可能逃逸上传目录，覆盖系统文件或写入可执行目录（配合 JSP 上传 = 远程代码执行）
```

```java
// 消毒规则（三件事）
private String sanitize(String fileName) {
    // 1. 只取最后一个分隔符后的部分，干掉 .. 与 / \
    String name = fileName.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1);
    // 2. 拒绝空名与特殊名
    if (name.isEmpty() || name.equals(".") || name.equals("..")) {
        throw new IllegalArgumentException("非法文件名");
    }
    // 3. 白名单扩展名（图片：jpg/png/gif/webp）或重命名
    return UUID.randomUUID() + "_" + name;
}
```

### 7.2 上传安全清单

| 风险 | 防护 |
|------|------|
| 路径穿越 | 文件名消毒 + **服务端重新命名**（UUID），不信任客户端名 |
| 超大文件 DoS | `maxFileSize`/`maxRequestSize` 严格限制 |
| 恶意类型（伪装 exe/jsp） | 校验魔数（Magic Number），不只看 Content-Type |
| 存储型 XSS | 上传目录**禁止脚本执行**（Nginx 配置 / 存对象存储） |
| 可执行文件上传 + 服务器可解析 | 上传目录与 Web 根目录**物理隔离**，改名+白名单扩展名 |
| 文件重名覆盖 | `CREATE_NEW` 或 UUID 重命名 |
| 临时文件残留 | `Part.delete()` / 定时清理 location 目录 |

> 🎯 一句话：**上传本质是把"不可信字节"放进"受控目录"**——客户端给的一切（文件名、Content-Type、大小）都不可信；服务端必须重新命名、白名单扩展名、隔离目录、限大小。

## 8. 与 Spring MVC 的对比

| 维度 | 原生 Servlet（Part） | Spring MVC（MultipartFile） |
|------|---------------------|---------------------------|
| 获取 | `req.getPart("file")` | `@RequestParam("file") MultipartFile` |
| 参数声明 | `@MultipartConfig` | Boot 自动配置（`spring.servlet.multipart.*`） |
| 大小限制 | 注解四参数 | `max-file-size` / `max-request-size` |
| 落盘 | `part.write(path)` | `file.transferTo(path)` |
| 流读取 | `part.getInputStream()` | `file.getInputStream()` |
| 内部机制 | 容器解析 | 底层仍是容器 Part（MultipartFile 是包装） |

```yaml
# Spring Boot 上传配置
spring:
  servlet:
    multipart:
      max-file-size: 10MB      # 对应 maxFileSize
      max-request-size: 50MB   # 对应 maxRequestSize
      file-size-threshold: 1MB # 对应 fileSizeThreshold
```

> 🎯 本质：Spring MVC 的 `MultipartFile` 就是 Part 的框架化包装——**学会 Part API，Spring 上传配置只需记三个 key**。两者共用同一套容器解析链路。

---

**下一模块**：[08-生产实践与面试题](08-生产实践与面试题.md) / **返回总览**：[00-Servlet总览](00-Servlet总览.md)
