# Tomcat 与 JavaWeb 开发技术详解

> **文档定位**：Java 后端技术参考文档 | Tomcat + JavaWeb 开发全览  
> **核心涵盖**：Web 运作模型 → HTTP 协议 → Tomcat 详解 → 项目实战（手动 + IDEA 创建）→ Servlet 详解 → Bookstore 项目架构  
> **前置基础**：JDK 8+、Tomcat 8/9/10、HTML/CSS/JS 基础

---

## 目录

- [第一部分：Web 基础与 HTTP 协议](#第一部分web-基础与-http-协议)
- [第二部分：Tomcat 详解](#第二部分tomcat-详解)
- [第三部分：创建第一个 JavaWeb 应用](#第三部分创建第一个-javaweb-应用)
- [第四部分：Servlet 详解](#第四部分servlet-详解)
- [第五部分：Bookstore 应用架构](#第五部分bookstore-应用架构)

---

## 第一部分：Web 基础与 HTTP 协议

### 一、Web 核心运作模型（C/S 架构）

| 角色 | 说明 |
|------|------|
| **客户端** | 发起请求的终端（浏览器、APP、Postman） |
| **服务器** | 接收并处理请求，返回响应（Tomcat、Nginx） |
| **网络** | 通信链路（HTTP/HTTPS 协议） |

### 二、一次完整 Web 请求流程

```
1. DNS 解析 → 域名 → IP 地址
2. TCP 三次握手 → 建立连接
3. 发送 HTTP 请求 → 请求行 + 请求头 + 请求体
4. 服务器处理 → Nginx 转发 → Tomcat 执行业务 → 生成响应
5. 返回 HTTP 响应 → 状态行 + 响应头 + 响应体
6. TCP 四次挥手 → 关闭连接（keep-alive 时复用）
7. 浏览器渲染 → HTML → DOM + CSS → CSSOM → 渲染树 → 布局 → 绘制
```

### 三、HTTP 协议核心

| 特点 | 说明 |
|------|------|
| **无状态** | 每次请求独立，需 Cookie/Session 维持状态 |
| **无连接** | 默认一次请求对应一次连接（HTTP/1.1 支持 keep-alive） |
| **基于文本** | 请求和响应均为文本格式 |
| **灵活** | 支持多种请求方法、数据格式 |

### 请求方法

| 方法 | 用途 | 幂等性 |
|------|------|--------|
| `GET` | 获取资源 | ✅ |
| `POST` | 提交数据 | ❌ |
| `PUT` | 更新资源 | ✅ |
| `DELETE` | 删除资源 | ✅ |

### 状态码速查

| 类别 | 常见状态码 |
|------|-----------|
| 1xx 信息 | — |
| 2xx 成功 | `200 OK`、`201 Created` |
| 3xx 重定向 | `301` 永久、`302` 临时、`304` 缓存 |
| 4xx 客户端错误 | `400`、`401`、`403`、`404` |
| 5xx 服务端错误 | `500`、`502`、`503` |

### HTTPS vs HTTP

```
HTTPS = HTTP + SSL/TLS（加密层）
核心优势：数据加密 + 身份认证 + 防篡改
```

### Web 服务器 vs 应用服务器

| 类型 | 代表 | 功能 |
|------|------|------|
| **Web 服务器** | Nginx、Apache | 静态资源、反向代理、负载均衡 |
| **应用服务器** | Tomcat、Jetty | 运行动态代码、处理业务逻辑 |

```
典型架构：客户端 → Nginx（静态资源/反向代理） → Tomcat（动态请求） → 数据库
```

### Cookie 与 Session

| 对比 | Cookie | Session |
|------|--------|---------|
| 存储位置 | 客户端 | 服务器 |
| 安全性 | 低 | 高 |
| 容量 | ~4KB | 无限制 |

### AJAX 与前后端分离

```
前端（Vue/React） → AJAX/Fetch → RESTful API → 后端（Spring Boot） → JSON
```

---

## 第二部分：Tomcat 详解

### 一、核心定位

Apache Tomcat 是开源 Java Servlet 容器，同时支持 JSP、EL 表达式和 WebSocket。

| 特性 | 说明 |
|------|------|
| **轻量级** | 体积小（核心包几十 MB）、启动快、资源占用低 |
| **兼容性** | 完全兼容 Servlet 和 JSP 最新规范 |
| **易部署** | WAR 包部署、热部署（无需重启更新应用） |
| **内置功能** | HTTP 服务器、管理控制台、日志系统 |

### 二、核心组件

| 模块 | 职责 |
|------|------|
| **Catalina** | 核心引擎，Servlet 容器管理 |
| **Coyote** | HTTP/1.1 和 TCP 连接处理 |
| **Jasper** | JSP 解析引擎（JSP → Servlet 类） |
| **Connector** | 连接器，监听端口接收请求（默认 8080） |

### 容器结构（从大到小）

```
Server → Service（Connector + Engine）→ Engine → Host（虚拟主机）→ Context（Web 应用）
```

### 三、部署方式

| 方式 | 说明 |
|------|------|
| **WAR 包** | 放入 `webapps`，自动解压部署 |
| **目录部署** | 直接复制到 `webapps` |
| **配置文件** | `conf/Catalina/localhost/XXX.xml` 指定路径 |
| **热部署** | `context.xml` 中 `reloadable="true"` |

### 四、核心配置文件

| 文件 | 作用 |
|------|------|
| `conf/server.xml` | 核心配置（端口、Connector、Host） |
| `conf/web.xml` | 全局 Web 应用配置 |
| `conf/context.xml` | 全局上下文配置 |
| `conf/tomcat-users.xml` | 管理控制台用户认证 |
| `WEB-INF/web.xml` | 单个应用配置 |

### 五、运行模式

| 模式 | 说明 | 适用 |
|------|------|------|
| **独立运行** | 直接启动，Tomcat 自带 HTTP 服务器 | 开发环境 |
| **反向代理** | Nginx 处理静态资源，动态请求转发 Tomcat | 生产环境 ✅ |

### 六、常用端口

| 端口 | 用途 |
|------|------|
| `8080` | 默认 HTTP 端口 |
| `8005` | 关闭端口 |
| `8009` | AJP 端口（与 Apache HTTP Server 通信） |
| `8443` | HTTPS 端口 |

### 七、启动与停止

| 系统 | 启动 | 停止 |
|------|------|------|
| Windows | `bin/startup.bat` | `bin/shutdown.bat` |
| Linux/Mac | `bin/startup.sh` | `bin/shutdown.sh` |

### 八、与其他服务器对比

| 对比 | Tomcat | Nginx | JBoss/WebLogic |
|------|--------|-------|---------------|
| **类型** | 应用服务器 | Web 服务器 | 重量级应用服务器 |
| **职责** | 运行动态 Java 请求 | 静态资源 + 反向代理 | 完整 Java EE 规范 |
| **重量** | 轻量 | 轻量 | 重量 |

---

## 第三部分：创建第一个 JavaWeb 应用

### 方式 1：手动创建（理解底层结构）

#### 目录结构

```
HelloWeb/
├── index.html
└── WEB-INF/
    ├── classes/        （编译后的 class 文件）
    ├── lib/            （第三方 jar）
    └── web.xml         （核心配置）
```

#### Servlet 类

```java
package com.example;

import javax.servlet.http.*;
import java.io.*;

public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<h1>Hello JavaWeb!</h1>");
        out.println("<p>路径：" + request.getContextPath() + "</p>");
    }
}
```

#### 编译

```bash
# 进入 classes 目录
cd HelloWeb/WEB-INF/classes
javac -cp "Tomcat路径/lib/servlet-api.jar" com/example/HelloServlet.java
```

#### web.xml

```xml
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="4.0">
    <servlet>
        <servlet-name>HelloServlet</servlet-name>
        <servlet-class>com.example.HelloServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>HelloServlet</servlet-name>
        <url-pattern>/hello</url-pattern>
    </servlet-mapping>
</web-app>
```

#### 部署访问

```
http://localhost:8080/HelloWeb/hello → "Hello JavaWeb!"
```

### 方式 2：IDEA 创建（推荐）

| 操作 | 说明 |
|------|------|
| 新建项目 | New Project → Java → Web Application |
| 配置 Tomcat | Add Configuration → Tomcat Server → Local |
| 注解配置 | `@WebServlet("/hello")` 替代 web.xml |

### 常见问题排查

| 问题 | 原因 | 解决 |
|------|------|------|
| 404 | 路径错误 | 检查上下文路径 + Servlet 映射 |
| 500 | 代码/配置错误 | 查看 `logs/catalina.out` |
| 编译错误 | 缺少 servlet-api.jar | 引入 Tomcat lib 目录的 jar |

---

## 第四部分：Servlet 详解

### 一、核心定义

Servlet 是运行在服务器端的 Java 程序，处理 HTTP 请求并返回动态响应——JavaWeb 的核心技术。

### 二、核心接口与类

| 类/接口 | 说明 |
|----------|------|
| `Servlet` 接口 | 根接口，定义生命周期方法 |
| `GenericServlet` | 通用实现类 |
| **`HttpServlet`** | 最常用，专门处理 HTTP 请求 |
| `HttpServletRequest` | 封装客户端请求信息 |
| `HttpServletResponse` | 封装服务器响应信息 |
| `ServletConfig` | Servlet 配置信息 |
| `ServletContext` | Web 应用上下文，全局共享 |

### 三、生命周期（核心）

```
1. 加载 → 2. 实例化 → 3. init()（一次）→ 4. service/doGet/doPost（每次请求）→ 5. destroy()（一次）
```

> **注意**：Servlet 默认单例，需注意线程安全，避免定义成员变量。

### 四、HttpServlet 核心用法

```java
@WebServlet("/httpServletDemo")
public class HttpServletDemo extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        String name = request.getParameter("name");
        if (name == null) name = "默认用户";
        response.getWriter().write("<h1>Hello " + name + "</h1>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        doGet(request, response);  // 复用 GET 逻辑
    }
}
```

### HttpServletRequest 常用方法

| 方法 | 说明 |
|------|------|
| `getParameter(name)` | 获取单个参数 |
| `getParameterValues(name)` | 获取多值参数 |
| `getMethod()` | 获取请求方法 |
| `getRequestURI()` | 获取请求 URI |
| `getContextPath()` | 获取应用上下文路径 |
| `getRemoteAddr()` | 获取客户端 IP |

### HttpServletResponse 常用方法

| 方法 | 说明 |
|------|------|
| `setContentType(type)` | 设置响应类型 |
| `getWriter()` | 获取字符输出流（文本） |
| `getOutputStream()` | 获取字节输出流（图片/文件） |
| `sendRedirect(url)` | 重定向（客户端跳转） |

### 请求转发 vs 重定向

| 对比 | 转发（forward） | 重定向（sendRedirect） |
|------|----------------|----------------------|
| 请求次数 | 1 次 | 2 次 |
| 地址栏 | 不变 | 变化 |
| 共享数据 | ✅ | ❌ |

### Servlet 配置方式

```java
// 注解配置（推荐）
@WebServlet(
    name = "DemoServlet",
    urlPatterns = "/demo",
    loadOnStartup = 1,
    initParams = {
        @WebInitParam(name = "key", value = "value")
    }
)
```

### ServletContext（应用上下文）

```java
ServletContext context = getServletContext();
context.setAttribute("appName", "第一个JavaWeb应用");  // 全局共享
String name = (String) context.getAttribute("appName");
String path = context.getRealPath("/WEB-INF/config.properties");  // 获取真实路径
```

---

## 第五部分：Bookstore 应用架构

### 一、核心定位

Bookstore（网上书店）是 JavaWeb 经典实战项目，属于中小型企业级电商类应用，常作为 SSM / Spring Boot 入门实战标杆。

### 二、核心功能模块

| 模块 | 功能 |
|------|------|
| **用户模块** | 注册、登录（Session 保存状态）、个人中心 |
| **书籍模块** | 列表（分页 + 分类筛选）、详情、搜索 |
| **购物车** | 添加、修改数量、删除、价格计算 |
| **订单模块** | 生成订单（扣库存）、订单列表、状态管理 |
| **后台管理** | 书籍/订单/用户/分类管理 |

### 三、技术架构演进

| 阶段 | 架构 | 技术栈 |
|------|------|--------|
| **入门级** | 传统 JavaWeb | HTML + CSS + JS + JSP + EL + JSTL + Servlet + JDBC + MySQL + Tomcat |
| **进阶级** | 前后端分离 | Vue + Element UI + Spring Boot + Spring MVC + MyBatis + MySQL + Redis |

### 四、核心业务流程（书籍购买）

```
浏览 → 加购（校验登录）→ 购物车管理 → 生成订单（扣库存）→ 
支付 → 订单履约（发货/收货）→ 完成
```

### 五、核心数据表设计

| 表 | 核心字段 |
|----|----------|
| **用户表** | 用户 ID、用户名（唯一）、密码（加密）、邮箱、手机号 |
| **书籍表** | 书籍 ID、名称、作者、定价、库存、分类 ID、封面路径 |
| **购物车表** | 购物车 ID、用户 ID、书籍 ID、数量（联合唯一索引） |
| **订单表** | 订单编号、用户 ID、总价、地址、状态（枚举） |
| **订单项表** | 订单项 ID、订单编号、书籍 ID、数量、购买时单价 |

### 六、学习价值

| 维度 | 收获 |
|------|------|
| **技术整合** | Servlet + JSP + Filter + JDBC 的综合运用 |
| **MVC 理解** | Servlet（控制器）+ JavaBean（模型）+ JSP（视图） |
| **数据库设计** | 电商场景表结构设计、外键关联、索引 |
| **进阶准备** | 从传统模式过渡到前后端分离 + Spring Boot |
