# Java网络编程：URL和URI详细知识点剖析

## 📑 目录

- [一、核心概念：URI与URL的定义及本质区别](#一核心概念uri与url的定义及本质区别)
- [二、URI详解（Java核心API：java.net.URI）](#二uri详解java核心apijavaneturi)
- [三、URL详解（Java核心API：java.net.URL）](#三url详解java核心apijavaneturl)
- [四、URI与URL的实战对比](#四uri与url的实战对比)
- [五、常见问题与解决方案](#五常见问题与解决方案)
- [六、总结](#六总结)

---

## 一、核心概念：URI与URL的定义及本质区别

### 1.1 核心定义

#### URI（Uniform Resource Identifier，统一资源标识符）

URI是用于唯一标识互联网上某一资源的字符串，核心作用是"识别"资源，不必然包含资源的访问方式。它是一个抽象的概念，范围更广，包含了URL和URN两种类型。

示例：

- `mailto:test@163.com`（标识一个邮箱地址）
- `urn:isbn:9787111641247`（标识一本书的ISBN）
- `https://www.baidu.com`（既能标识资源，也能访问）

#### URL（Uniform Resource Locator，统一资源定位符）

URL是URI的子集，不仅能唯一标识资源，还包含了访问该资源的具体方式和路径（即"定位"资源）。它必须指定协议（如HTTP、HTTPS、FTP），通过协议和路径能直接访问到目标资源。

示例：

- `http://www.java.com:80/docs/api`
- `ftp://ftp.example.com/file.txt`
- `file:///D:/test.txt`

### 1.2 URI与URL的核心区别

| 对比维度 | URI（统一资源标识符） | URL（统一资源定位符） |
|---------|---------------------|---------------------|
| 核心作用 | 唯一标识资源（不关心如何访问） | 唯一定位并访问资源（明确访问方式） |
| 范围 | 范围广，包含URL和URN | 范围窄，是URI的子集 |
| 是否包含协议 | 可选 | 必须包含协议 |
| 是否可访问 | 不一定 | 一定可以通过协议访问 |
| 示例 | `urn:isbn:9787111641247` | `https://www.baidu.com` |

> 所有URL都是URI，但并非所有URI都是URL。

---

## 二、URI详解（Java核心API：java.net.URI）

### 2.1 URI的结构

完整格式：

```
[scheme:][//authority][path][?query][#fragment]
```

各组件说明：

| 组件 | 说明 | 示例 |
|------|------|------|
| scheme（协议） | 可选，如http、https、ftp | `https` |
| authority（权限信息） | 包含主机名和端口 | `www.example.com:8080` |
| path（路径） | 资源在服务器上的路径 | `/docs/api` |
| query（查询参数） | 以 `?` 开头 | `name=java` |
| fragment（片段） | 以 `#` 开头，定位资源内部片段 | `chapter1` |

示例：`https://www.example.com:8080/docs/api?name=java#chapter1`

### 2.2 Java中URI类的核心用法

**实例创建**：

```java
URI uri1 = URI.create("https://www.example.com:8080/docs?name=java#chapter1");
URI uri2 = new URI("mailto:test@163.com");
```

**核心方法**：

| 方法 | 说明 |
|------|------|
| `getScheme()` | 获取协议 |
| `getAuthority()` | 获取权限信息 |
| `getHost()` | 获取主机名 |
| `getPort()` | 获取端口号（不存在返回-1） |
| `getPath()` | 获取路径 |
| `getQuery()` | 获取查询参数 |
| `getFragment()` | 获取片段 |
| `isAbsolute()` | 判断是否为绝对URI |
| `resolve(URI uri)` | 解析相对URI |

**常用方法示例**：

```java
URI uri = URI.create("https://www.example.com:8080/docs/api?name=java#chapter1");
System.out.println("协议：" + uri.getScheme());        // https
System.out.println("主机名：" + uri.getHost());         // www.example.com
System.out.println("端口号：" + uri.getPort());          // 8080
System.out.println("路径：" + uri.getPath());           // /docs/api
System.out.println("查询参数：" + uri.getQuery());      // name=java
System.out.println("片段：" + uri.getFragment());       // chapter1

// 解析相对URI
URI relativeUri = URI.create("test?age=18");
URI absoluteUri = uri.resolve(relativeUri);
// 结果：https://www.example.com:8080/docs/test?age=18
```

### 2.3 URI的编码与解码

URI中不能包含空格、中文、特殊符号，需要进行编码。

**编码与解码示例**：

```java
// 编码
String encodedPath = URLEncoder.encode("测试 页面", "UTF-8");
String encodedQuery = URLEncoder.encode("张三", "UTF-8");

// 解码
String decodedPath = URLDecoder.decode(encodedPath, "UTF-8");
String decodedQuery = URLDecoder.decode(encodedQuery, "UTF-8");
```

> **注意事项**：
> - 编码时，仅对"非标准字符"编码，协议、主机名、分隔符不编码。
> - 编码和解码的字符集必须一致（推荐UTF-8）。

---

## 三、URL详解（Java核心API：java.net.URL）

### 3.1 URL的结构

```
scheme://host:port/path?query#fragment
```

常见URL协议：

| 协议 | 说明 | 默认端口 |
|------|------|---------|
| http | 超文本传输协议 | 80 |
| https | 加密的HTTP协议 | 443 |
| ftp | 文件传输协议 | 21 |
| file | 本地文件协议 | - |
| mailto | 邮件协议 | - |

### 3.2 Java中URL类的核心用法

**实例创建**：

```java
URL url1 = new URL("https://www.baidu.com:80/index.html?name=test#section1");
URL url2 = new URL("http", "www.example.com", 8080, "/docs/api?name=java");
URL baseUrl = new URL("https://www.example.com/docs/");
URL relativeUrl = new URL(baseUrl, "test.html");
```

**核心方法**：

| 类别 | 方法 | 说明 |
|------|------|------|
| 组件获取 | `getProtocol()` | 获取协议 |
| 组件获取 | `getHost()` | 获取主机名 |
| 组件获取 | `getPort()` | 获取端口号 |
| 组件获取 | `getPath()` | 获取路径 |
| 组件获取 | `getQuery()` | 获取查询参数 |
| 组件获取 | `getRef()` | 获取片段 |
| 组件获取 | `getFile()` | 获取路径+查询参数 |
| 资源访问 | `openConnection()` | 打开与URL对应的连接 |
| 资源访问 | `openStream()` | 打开URL的输入流 |

**访问资源示例**：

```java
URL url = new URL("https://www.baidu.com");
try (InputStream is = url.openStream();
     BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
    String line;
    StringBuilder sb = new StringBuilder();
    while ((line = br.readLine()) != null) {
        sb.append(line);
    }
    System.out.println(sb.toString());
}
```

### 3.3 URL与URLConnection的关系

`URLConnection` 是URL的核心辅助类，用于建立与URL资源的连接。

```java
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setConnectTimeout(3000);
conn.setReadTimeout(3000);

if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
    try (BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
        // 读取响应...
    }
}
conn.disconnect();
```

---

## 四、URI与URL的实战对比

### 4.1 什么时候用URI？

- 仅需要"标识资源"，不需要访问资源时。
- 处理相对路径的解析（如 `URI.resolve()`）。
- 需要严格校验资源标识的语法。
- 涉及非URL类型的标识（如URN、邮箱地址）。

### 4.2 什么时候用URL？

- 需要"访问资源"（读取、发送数据）时。
- 发送HTTP/HTTPS请求。
- 访问FTP服务器、本地文件。
- 需要获取资源的访问信息并建立连接。

### 4.3 两者转换

```java
// URL → URI
URI uriFromUrl = url.toURI();

// URI → URL（仅当URI是URL时可转换）
URL urlFromUri = uri.toURL();
```

---

## 五、常见问题与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| URI/URL格式错误异常 | 字符串含非法字符、协议格式错误 | 对中文/空格部分编码；检查协议格式 |
| URL访问资源超时/失败 | 网络不通、防火墙拦截 | 设置超时时间；检查防火墙 |
| 编码/解码乱码问题 | 字符集不一致 | 统一使用UTF-8字符集 |

---

## 六、总结

- **范围关系**：URL是URI的子集，所有URL都是URI，但URI不一定是URL。
- **核心区别**：URI负责"标识"资源，无需协议；URL负责"定位+访问"资源，必须包含协议。
- **API用法**：
  - `URI` 类：用于解析、验证、转换URI，不支持访问。
  - `URL` 类：用于解析URL、建立连接、访问资源。
- **实战场景**：仅标识资源用URI，需访问资源用URL；编码解码需统一字符集。

---

## 📖 相关阅读

- [Java网络编程：URLConnection详细知识点剖析](./Java网络编程：URLConnection详细知识点剖析.md)
- [Java网络编程：HTTP详细知识点剖析](./Java网络编程：HTTP详细知识点剖析.md)
- [Java网络编程详细知识点剖析](./Java网络编程详细知识点剖析.md)
