# SpringMVC 文件上传与下载

> **上传三前提**：① 表单 `enctype="multipart/form-data"` ② 提交方式 `POST` ③ `<input type="file" name="xxx">`

---

## 目录

1. [文件上传](#一文件上传)
   - [1.1 必要前提](#11-必要前提)
   - [1.2 依赖导入](#12-依赖导入)
   - [1.3 方式一：CommonsMultipartResolver（兼容低版本）](#13-方式一commonsmultipartresolver兼容低版本)
   - [1.4 方式二：StandardServletMultipartResolver（Servlet 3.0+）](#14-方式二standardservletmultipartresolverservlet-30)
   - [1.5 多文件上传](#15-多文件上传)
2. [文件下载](#二文件下载)
   - [2.1 方式一：基于 HttpServletResponse（灵活可控）](#21-方式一基于-httpservletresponse灵活可控)
   - [2.2 方式二：基于 ResponseEntity（SpringMVC 封装，简洁）](#22-方式二基于-responseentityspringmvc-封装简洁)
3. [常见问题与避坑](#三常见问题与避坑)
4. [扩展说明](#四扩展说明)

---

## 一、文件上传

### 1.1 必要前提

| 序号 | 前提要求 | 说明 |
|:----:|----------|------|
| 1 | `<form>` 的 `enctype="multipart/form-data"` | 默认 `application/x-www-form-urlencoded` 无法识别文件流 |
| 2 | 提交方式必须是 `POST` | GET 请求有参数长度限制，无法承载文件 |
| 3 | `<input type="file" name="xxx">` | `name` 属性需与后端接收参数名一致 |

### 1.2 依赖导入

SpringMVC 文件上传依赖 Apache Commons FileUpload，在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>commons-fileupload</groupId>
    <artifactId>commons-fileupload</artifactId>
    <version>1.3.3</version>
</dependency>
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.5</version>
</dependency>
```

> 若使用 Servlet 3.0+ 的 `StandardServletMultipartResolver`，则无需导入上述依赖。

---

### 1.3 方式一：CommonsMultipartResolver（兼容低版本）

#### 第一步：配置文件解析器（springmvc.xml）

> ⚠️ `bean` 的 `id` 必须严格为 **`multipartResolver`**，否则 SpringMVC 无法识别。

```xml
<!-- 文件上传解析器，id 必须为 multipartResolver -->
<bean id="multipartResolver"
      class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
    <!-- 单个文件最大上传大小（10MB，单位：字节） -->
    <property name="maxUploadSize" value="10485760"/>
    <!-- 整个请求最大上传大小（20MB） -->
    <property name="maxUploadSizePerFile" value="20971520"/>
    <!-- 编码格式，与表单一致 -->
    <property name="defaultEncoding" value="UTF-8"/>
    <!-- 临时文件内存缓存大小（字节） -->
    <property name="maxInMemorySize" value="40960"/>
</bean>
```

#### 第二步：前端 JSP 表单（upload.jsp）

```html
<h3>SpringMVC 文件上传</h3>
<form action="${pageContext.request.contextPath}/file/upload"
      method="post" enctype="multipart/form-data">
    选择文件：<input type="file" name="uploadFile"/><br/>
    <input type="submit" value="上传文件"/>
</form>
```

#### 第三步：后端 Controller

```java
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("/file")
public class FileUploadController {

    /**
     * 处理文件上传请求
     * @param uploadFile SpringMVC 自动将上传文件流封装为 MultipartFile 对象
     * @param request    HttpServletRequest，用于获取服务器真实路径
     */
    @RequestMapping("/upload")
    public String upload(MultipartFile uploadFile,
                         HttpServletRequest request) throws IOException {

        // 1. 校验文件是否为空
        if (uploadFile.isEmpty()) {
            return "error";  // 可跳转至错误页面
        }

        // 2. 获取服务器端上传目录的真实路径（避免硬编码）
        String uploadPath = request.getSession()
                .getServletContext().getRealPath("/uploads");
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();  // 多级目录需用 mkdirs()
        }

        // 3. 处理文件名：UUID 生成唯一标识，防重名
        String originalFilename = uploadFile.getOriginalFilename();
        String suffix = originalFilename.substring(
                originalFilename.lastIndexOf("."));        // 获取后缀如 .jpg
        String newFilename = UUID.randomUUID().toString()
                .replace("-", "").toUpperCase() + suffix;

        // 4. 保存文件到服务器（transferTo 底层自动处理文件流）
        File destFile = new File(uploadDir, newFilename);
        uploadFile.transferTo(destFile);

        // 5. 跳转成功页面（携带文件名供前端展示）
        request.setAttribute("filename", newFilename);
        return "success";
    }
}
```

**核心步骤拆解**：

| 步骤 | 操作 | 关键方法 |
|:----:|------|----------|
| ① | 校验文件 | `uploadFile.isEmpty()` |
| ② | 获取上传路径 | `getServletContext().getRealPath("/uploads")` |
| ③ | 生成唯一文件名 | `UUID.randomUUID()` + 后缀 |
| ④ | 保存文件 | `MultipartFile.transferTo(File)` |
| ⑤ | 返回结果 | 携带文件名跳转成功页面 |

---

### 1.4 方式二：StandardServletMultipartResolver（Servlet 3.0+）

> 无需 `commons-fileupload` 依赖，使用 Servlet 3.0+ 原生标准方式。

#### 第一步：配置 web.xml 中的 DispatcherServlet

```xml
<servlet>
    <servlet-name>springmvc</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:springmvc.xml</param-value>
    </init-param>
    <!-- 文件上传相关配置 -->
    <multipart-config>
        <location>/tmp</location>              <!-- 临时文件存储目录 -->
        <max-file-size>10485760</max-file-size>   <!-- 单个文件最大 10MB -->
        <max-request-size>20971520</max-request-size> <!-- 请求总大小 20MB -->
    </multipart-config>
</servlet>
<servlet-mapping>
    <servlet-name>springmvc</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

#### 第二步：配置 springmvc.xml 中的解析器

```xml
<bean id="multipartResolver"
      class="org.springframework.web.multipart.support.StandardServletMultipartResolver"/>
```

#### 第三步：Controller 代码

> 与方式一完全一致，`MultipartFile` 接收文件的逻辑通用。

---

### 1.5 多文件上传

**前端**（`<input>` 添加 `multiple` 属性）：

```html
<input type="file" name="uploadFiles" multiple/>
```

**后端 Controller**：

```java
@RequestMapping("/uploadMulti")
public String uploadMulti(@RequestParam("uploadFiles") MultipartFile[] files,
                          HttpServletRequest request) throws IOException {
    String uploadPath = request.getSession()
            .getServletContext().getRealPath("/uploads");
    File uploadDir = new File(uploadPath);
    if (!uploadDir.exists()) {
        uploadDir.mkdirs();
    }
    for (MultipartFile file : files) {
        if (!file.isEmpty()) {
            String suffix = file.getOriginalFilename()
                    .substring(file.getOriginalFilename().lastIndexOf("."));
            String newName = UUID.randomUUID().toString()
                    .replace("-", "").toUpperCase() + suffix;
            file.transferTo(new File(uploadDir, newName));
        }
    }
    return "success";
}
```

---

## 二、文件下载

### 2.1 方式一：基于 HttpServletResponse（灵活可控）

> 支持权限控制、文件校验，适配各类场景。

```java
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

@Controller
@RequestMapping("/file")
public class FileDownloadController {

    @RequestMapping("/download")
    public void download(String filename,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // 1. 校验文件名（避免非法请求）
        if (filename == null || "".equals(filename.trim())) {
            response.getWriter().write("请指定要下载的文件");
            return;
        }

        // 2. 获取服务器端文件的真实路径
        String uploadPath = request.getSession()
                .getServletContext().getRealPath("/uploads");
        File file = new File(uploadPath, filename);

        // 3. 校验文件是否存在、是否为文件
        if (!file.exists() || !file.isFile()) {
            response.getWriter().write("文件不存在");
            return;
        }

        // 4. 设置响应头，告知浏览器以附件形式下载
        response.setContentType("application/octet-stream");
        // 中文文件名 URL 编码，防乱码
        String safeFilename = URLEncoder.encode(filename, "UTF-8")
                .replace("+", "%20");
        response.addHeader("Content-Disposition",
                "attachment;filename=" + safeFilename);

        // 5. 流式输出文件内容
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            os = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        } finally {
            // 按顺序关闭流（先关输出，再关输入）
            if (os != null) { try { os.close(); } catch (IOException e) { } }
            if (bis != null) { try { bis.close(); } catch (IOException e) { } }
            if (fis != null) { try { fis.close(); } catch (IOException e) { } }
        }
    }
}
```

**下载流程拆解**：

| 步骤 | 操作 | 说明 |
|:----:|------|------|
| ① | 校验文件名 | 防止空参和非法请求 |
| ② | 获取文件路径 | `getRealPath("/uploads")` |
| ③ | 校验文件存在性 | 文件不存在返回提示 |
| ④ | 设置响应头 | `Content-Disposition: attachment` |
| ⑤ | 流式输出 | `FileInputStream → BufferedInputStream → response.getOutputStream()` |

---

### 2.2 方式二：基于 ResponseEntity（SpringMVC 封装，简洁）

> 利用 `ResponseEntity` + `FileSystemResource`，SpringMVC 自动处理流关闭。

```java
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Controller
@RequestMapping("/file")
public class FileDownloadController2 {

    @RequestMapping("/download2")
    public ResponseEntity<FileSystemResource> download2(
            String filename, HttpServletRequest request)
            throws UnsupportedEncodingException {

        // 1. 获取文件并校验
        String uploadPath = request.getSession()
                .getServletContext().getRealPath("/uploads");
        File file = new File(uploadPath, filename);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();  // 404
        }

        // 2. 封装文件资源
        FileSystemResource fileResource = new FileSystemResource(file);

        // 3. 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String safeFilename = URLEncoder.encode(filename, "UTF-8")
                .replace("+", "%20");
        headers.add("Content-Disposition",
                "attachment;filename=" + safeFilename);

        // 4. 返回 ResponseEntity，SpringMVC 自动处理流关闭
        return new ResponseEntity<>(fileResource, headers, HttpStatus.OK);
    }
}
```

**两种下载方式对比**：

| 维度 | 方式一（HttpServletResponse） | 方式二（ResponseEntity） |
|------|--------------------------|------------------------|
| 流控制 | 手动 try-finally 关闭 | SpringMVC 自动关闭 |
| 代码量 | 较多 | 更简洁 |
| 灵活性 | ✅ 可精细控制 | 中等 |
| 权限校验 | ✅ 灵活 | ✅ 灵活 |
| 推荐场景 | 需要自定义流处理的场景 | 常规文件下载 |

---

### 前端触发下载

```html
<!-- a 标签直接触发下载 -->
<a href="${pageContext.request.contextPath}/file/download?filename=${filename}">
    下载文件
</a>

<!-- 或通过 JS 发起请求 -->
<script>
function downloadFile(filename) {
    window.location.href = '${pageContext.request.contextPath}/file/download?filename=' + filename;
}
</script>
```

---

## 三、常见问题与避坑

### 3.1 上传相关问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 文件上传失败，抛 `ClassCastException` | 解析器 `id` 不是 `multipartResolver` | 确保 `bean` 的 `id` 严格为 `multipartResolver` |
| 中文文件名乱码 | 前后端编码不一致 | 设置解析器 `defaultEncoding=UTF-8` + 表单 `accept-charset="UTF-8"` |
| 文件大小超限 | 超过 `maxUploadSize` | 调整解析器 `maxUploadSize` 和 `maxUploadSizePerFile`（单位：字节） |
| `Could not parse multipart` 异常 | 表单未设置 `enctype="multipart/form-data"` | 检查 `<form>` 标签 `enctype` 属性 |
| 服务器保存失败 `Permission denied` | 上传目录无写权限 | Linux 执行 `chmod -R 755 目录路径` |

### 3.2 下载相关问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 下载文件为空或损坏 | 流关闭顺序错误 / 未读取完就关闭 | 先关输出流再关输入流，确保 `while` 循环完整执行 |
| 浏览器直接打开而非下载 | `Content-Disposition` 未设为 `attachment` | 设置 `response.addHeader("Content-Disposition", "attachment;filename=...")` |
| 中文文件名下载乱码 | 未做 URL 编码 | `URLEncoder.encode(filename, "UTF-8").replace("+", "%20")` |
| 大文件下载超时 | 服务器/代理超时限制 | 调整 `request-timeout` + 配置 Nginx `proxy_read_timeout` |
| 下载提示"文件不存在" | 路径错误或文件已被删 | 检查 `getRealPath("/uploads")` 路径，确认文件存在 |

---

## 四、扩展说明

| 扩展方向 | 做法 |
|----------|------|
| **多文件上传** | 前端 `<input type="file" multiple/>` + 后端 `MultipartFile[]` 数组接收 |
| **文件类型校验** | 检查文件后缀名 + MIME 类型，拒绝 `.exe`、`.jsp` 等可执行文件 |
| **生产环境存储** | 推荐 OSS（阿里云/腾讯云），避免本地存储（重启丢失 / 容量不足） |
| **大文件分片上传** | 前端分片 + 后端合并，配合进度条 |
| **权限校验** | 下载时校验用户登录状态，防止未授权下载敏感文件 |
