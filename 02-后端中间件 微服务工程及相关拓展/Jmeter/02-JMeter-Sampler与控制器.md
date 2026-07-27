# JMeter Sampler 与控制器

> 📡 HTTP/JDBC/TCP Sampler 请求发送 + 逻辑控制器控制执行流程 —— 请求采样与流程编排

---

## 📚 目录

1. [Sampler 采样器概述](#1-sampler-采样器概述)
2. [HTTP Request Sampler](#2-http-request-sampler)
3. [其他常用 Sampler](#3-其他常用-sampler)
4. [逻辑控制器](#4-逻辑控制器)
5. [参数化 HTTP 请求](#5-参数化-http-请求)

---

## 1. Sampler 采样器概述

```text
Sampler = JMeter 中最核心的元件，负责发送请求

常用 Sampler：
  ├── HTTP Request      → Web API 测试（最常用 90%+）
  ├── JDBC Request      → 数据库压测
  ├── TCP Sampler       → TCP 协议压测
  ├── FTP Request       → FTP 文件传输
  ├── JMS Publisher/Subscriber → 消息队列
  ├── Java Request      → 自定义 Java 代码
  ├── Debug Sampler     → 调试变量值
  └── Dummy Sampler     → 占位/模拟请求
```

---

## 2. HTTP Request Sampler

### 2.1 基础配置

```text
┌─────────────────────────────────────────┐
│ HTTP Request 配置面板                    │
├─────────────────────────────────────────┤
│                                         │
│  Protocol:  https                       │
│  Server:    api.example.com             │
│  Port:      443                         │
│  Method:    POST                        │
│  Path:      /api/v1/users               │
│                                         │
│  Content encoding: UTF-8                │
│                                         │
│  Parameters 标签（Query/Form 参数）：     │
│  ┌──────────┬──────────┬──────────┐    │
│  │ page     │ 1        │ ☑ Encode│    │
│  │ size     │ 20       │ ☑ Encode│    │
│  └──────────┴──────────┴──────────┘    │
│                                         │
│  Body Data 标签（JSON/XML/Text）：       │
│  ┌─────────────────────────────────┐    │
│  │ {                               │    │
│  │   "name": "${username}",        │    │
│  │   "email": "${email}"           │    │
│  │ }                               │    │
│  └─────────────────────────────────┘    │
│                                         │
│  Files Upload 标签（文件上传）：         │
│  ┌──────────┬──────────────┬──────┐    │
│  │ avatar   │ /path/img.png │ MIME │    │
│  └──────────┴──────────────┴──────┘    │
│                                         │
└─────────────────────────────────────────┘
```

### 2.2 高级 HTTP 配置

```text
HTTP Request 的 Advanced 标签：

  Client Implementation：
    ├── Java (默认)  → 仅支持 HTTP/1.1
    └── HttpClient4  → 需要额外配置（推荐）
       → 支持 HTTP/2, 连接池, Kerberos, 更好的 Keep-Alive

  Timeouts：
    ├── Connect: 5000  (ms)  连接超时
    └── Response: 30000 (ms) 响应超时

  Embedded Resources：
    → Retrieve All Embedded Resources：下载内嵌资源（JS/CSS/图片）
    → 模拟真实浏览器行为
    → ⚠️ 不要用来压静态资源 CDN

  Source Address：
    → 绑定特定 IP（多网卡场景）

  Redirect Automatically vs Follow Redirects：
    → Follow Redirects：跟踪重定向且每个重定向单独统计
    → 推荐 ✅
```

### 2.3 HTTP Request Defaults

```text
HTTP Request Defaults（配置元件）：
  → 设置全局默认的 HTTP 请求参数
  → 后续 HTTP Request 不填的字段自动继承
  → 减少了大量重复配置！

适用场景：
  → 所有请求同一 host/port/protocol
  → 统一 Content-Encoding
  → 统一自定义请求头

继承规则：
  HTTP Request Defaults（全局）
    └── HTTP Request 继承（可以覆盖）
        └── HTTP Header Manager（本地）追加/覆盖
```

---

## 3. 其他常用 Sampler

### 3.1 JDBC Request（数据库压测）

```text
前置：需加载 JDBC 驱动
  → Test Plan → Add directory/jar to classpath → mysql-connector.jar

配置步骤：
  1. JDBC Connection Configuration（Config Element）
     → Pool Variable Name: mySqlPool
     → Database URL: jdbc:mysql://localhost:3306/testdb
     → JDBC Driver: com.mysql.cj.jdbc.Driver
     → Username/Password: ...

  2. JDBC Request
     → Variable Name: mySqlPool (关联上面的连接池)
     → Query Type: Select Statement
     → SQL: SELECT * FROM users WHERE id = ?

     → Parameter values: ${userId}
     → Parameter types: INT

  Query Type 可选：
    Select / Update / Callable / Prepared Select / Commit / Rollback ...
```

### 3.2 Debug Sampler

```text
用途：调试时打印当前所有变量值
  → 添加到任何位置
  → 执行后在 View Results Tree 中查看响应
  → 显示 JMeter Variables + Properties + System Properties

  ⚠️ 正式压测时务必删除或禁用！影响性能
```

### 3.3 Java Request（自定义扩展）

```java
// 自定义 Sampler：实现 JavaSamplerClient 接口
public class CustomSampler extends AbstractJavaSamplerClient {

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();  // 开始计时

        try {
            // 自定义测试逻辑
            String param = context.getParameter("key");
            // ... do something ...

            result.setSuccessful(true);
            result.setResponseCode("200");
            result.setResponseMessage("OK");
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setResponseMessage(e.getMessage());
        }

        result.sampleEnd();  // 结束计时
        return result;
    }
}

// 打包成 .jar → 放 lib/ext/ → JMeter GUI 中出现
```

---

## 4. 逻辑控制器

### 4.1 控制器分类

| 控制器 | 功能 | 典型场景 |
|--------|------|---------|
| **Simple Controller** | 纯分组容器 | 组织 Sampler |
| **Loop Controller** | 循环执行子元件 | 多次执行某接口 |
| **Once Only Controller** | 仅执行一次 | 登录/初始化 |
| **If Controller** | 条件执行 | 根据变量判断 |
| **Transaction Controller** | 将多请求合并为一个事务 | 统计整体响应时间 |
| **While Controller** | 循环直到条件不满足 | 轮询直到完成 |
| **Switch Controller** | 按值选择执行路径 | 多分支场景 |
| **Module Controller** | 引用外部模块 | 复用测试片段 |
| **Random Controller** | 随机执行一个子元件 | 模拟随机行为 |
| **Throughput Controller** | 按比例分配流量 | 按比例模拟用户行为 |

### 4.2 常用控制器实战

```text
Transaction Controller（性能测试必备！）
  → 一组请求视为一个业务事务
  → 报告显示整体响应时间 = 所有子请求之和
  → Generate parent sample：主从关系清晰

  示例：用户注册流程
    Transaction Controller: "用户注册"
    ├── POST /api/auth/getVerifyCode   → 获取验证码
    ├── POST /api/auth/register         → 提交注册
    └── GET  /api/users/profile          → 获取个人信息

If Controller
  → Interpret Condition as Variable Expression
  → Condition: ${__jexl3("${status}" == "success")}
  → 仅当 status 变量值为 "success" 时执行
```

---

## 5. 参数化 HTTP 请求

### 5.1 请求参数传递方式

| 方式 | 适用场景 | 配置 |
|------|---------|------|
| **URL Query** | GET 请求 | Parameters 标签 |
| **Path Variable** | RESTful 路径 | Path: /users/${userId} |
| **Request Body (JSON)** | POST/PUT | Body Data 标签 |
| **Form Data** | 传统表单 | Parameters 标签 → Body 模式选 form-data |
| **File Upload** | 文件上传 | Files Upload 标签 |
| **Header** | Token / API Key | HTTP Header Manager |

### 5.2 Content-Type 与编码

```text
不同 Content-Type 对应的 Body 发送方式：

  application/json：
    → Body Data: {"key": "value"}  ← 直接写 JSON
    → JMeter 不作额外编码

  application/x-www-form-urlencoded：
    → Parameters 标签填写 key=value
    → JMeter 自动 URL 编码

  multipart/form-data：
    → Body Data → 选择 multipart/form-data
    → 文本框 + 文件上传混合
```

---

> 🎯 **核心要点**：HTTP Request Sampler 是绝对主力（90%+ 场景），HTTP Request Defaults 减少重复配置，Transaction Controller 合并多请求为一个业务事务统计整体耗时。

---

*创建于：2026年7月*
