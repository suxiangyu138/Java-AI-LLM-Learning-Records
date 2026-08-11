# 03 Spring Boot 入门 Demo

> 第一个 Web 项目：用 start.spring.io 生成 Spring Boot 4.1 工程，写 REST 接口、理解自动配置与依赖注入，跑通"浏览器请求 → Controller → 响应"的完整链路。这是后续所有 Demo 的骨架。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [项目创建：start.spring.io](#2-项目创建startspringio)
3. [第一个 REST 接口](#3-第一个-rest-接口)
4. [参数绑定与返回](#4-参数绑定与返回)
5. [配置文件与热部署](#5-配置文件与热部署)
6. [理解自动配置与依赖注入](#6-理解自动配置与依赖注入)
7. [常见坑](#7-常见坑)
8. [核心要点](#8-核心要点)

---

## 1. 目标与验收

本 Demo 的产出：一个能启动的 Spring Boot Web 工程，提供三个接口（GET 问候、GET 带参数、POST 接收 JSON），返回正确结果。验收标准：**能解释项目目录结构每一层的职责**（controller/service/entity/application 配置文件）；**能讲清"启动时发生了什么"**（内嵌 Tomcat 启动、自动配置生效）；**独立重写一个接口**（不加任何框架代码，只写 Controller 与配置）。版本基线：Spring Boot 4.1.x（2026-08，Framework 7.0，要求 Java 21+，需用 JDK 21 或 25）。

## 2. 项目创建：start.spring.io

浏览器打开 start.spring.io（国内可访问，也可用阿里云镜像版 start.aliyun.com），按如下配置生成：**Project** 选 Maven；**Language** 选 Java；**Spring Boot** 选 4.1.x；**Group** 填 `com.example`、**Artifact** 填 `demo-web`；**Dependencies** 选 Spring Web、Lombok（可选）、Spring Boot DevTools（热部署）。生成后下载解压，用 IDEA 打开（选"Open"而不是"New"，IDEA 会识别 Maven 工程），等待依赖下载完成后点运行按钮。

首次启动的预期：控制台出现 Spring Boot 启动日志（`Tomcat started on port 8080`），浏览器访问 `http://localhost:8080` 返回 404 是正常的（还没有接口）——**不要被 404 吓到，这是没有根路径接口的正常现象**。启动失败先查 01 篇的环境自检清单（端口占用是第一嫌疑）。

## 3. 第一个 REST 接口

在 `com.example.demo` 包下新建 controller 包与第一个 Controller——这是你写下的第一个 Web 接口：

```java
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring Boot 4.1";
    }
}
```

```text
GET http://localhost:8080/hello  →  200 Hello Spring Boot 4.1
```

三个注解是理解重点：`@RestController`（= @Controller + @ResponseBody，返回内容直接写进响应体而非视图名）；`@GetMapping`（路径与方法绑定）；`@RequestMapping` 族（类级前缀 + 方法级路径组合成完整路由）。写完启动再访问，体会"改了代码 → 重启 → 看到结果"的节奏。修改后**不用手动重启**：DevTools 生效时 IDEA 会自动重启（控制台能看到 `Restarting` 日志），没生效就检查依赖是否包含 spring-boot-devtools 与 IDEA 的 Build → Build project automatically 是否开启。

## 4. 参数绑定与返回

扩展第二个接口，覆盖三种参数来源——这是面试与后续 Demo 的基础功：**路径参数**（`@PathVariable`）、**查询参数**（`@RequestParam`）、**JSON 请求体**（`@RequestBody`）。同时引入"实体类 + 返回对象"的分层雏形：

```java
@PostMapping("/orders")
public OrderVO createOrder(@RequestBody OrderRequest req) {
    return new OrderVO(req.orderId(), "SUCCESS", "订单创建成功");
}
```

```java
record OrderRequest(Long orderId, BigDecimal amount) {}
record OrderVO(Long orderId, String status, String message) {}
```

三个要点：**record 直接用作 DTO**（JDK 21+ 现代写法，替代 Lombok 的 @Data，序列化由 Jackson 自动完成）；**参数校验留到 Level2**（Demo 阶段不引入 validation 依赖）；**返回结构统一**（`OrderVO` 里 status/message 的雏形，就是 Level2 统一响应体的起点）。JSON 序列化注意：Jackson 默认驼峰命名，前端要下划线风格时配置 `spring.jackson.property-naming-strategy`，Demo 阶段用默认即可。

**REST 设计的两条纪律**顺手建立：**HTTP 方法语义**——GET 查（幂等）、POST 建（不幂等）、PUT 全量更新（幂等）、DELETE 删（幂等）；**状态码语义**——200 成功、201 创建成功、400 参数错误、404 资源不存在、500 服务端错误。Demo 阶段接口少，但每个接口的返回状态码按语义选——这是 Level2 统一响应体设计的前置习惯。**IDEA HTTP Client 是接口调试利器**：在工程里建 `requests.http` 文件，写 `GET http://localhost:8080/hello`，点行首箭头直接发送——比 Postman 少一个工具、比浏览器能带请求体，Level1 全程用它调试接口。

## 5. 配置文件与热部署

`application.properties` 是工程的"开关面板"，本 Demo 配置三样：**端口**（`server.port=8081` 避免与别的程序冲突，改完重启生效）、**应用名**（`spring.application.name=demo-web`）、**日志级别**（`logging.level.com.example=DEBUG` 观察调试日志）。配置修改的生效规则：**properties 修改必须重启**（Spring Boot 4 的配置热更新需要 spring-boot-devtools 的 restart 或 actuator refresh，Demo 阶段重启即可）。

热部署的完整配置：pom 加 `spring-boot-devtools` 依赖 → IDEA 勾选 Settings → Build → Compiler → Build project automatically → 注册表 `registry` 里打开 `compiler.automake.allow.when.app.running`。配好后改 Java 代码自动重启，改 properties 自动重启，前端静态资源不重启——热部署是 Demo 期迭代速度的保证，值得一次配对。

## 6. 理解自动配置与依赖注入

本 Demo 的技术含量不在代码量，而在两个机制的理解——这是 Spring 面试的必考，也是 Level2 排查问题的前提。**自动配置**：`spring-boot-autoconfigure` 按 classpath 上的依赖自动装配 Bean（加了 spring-boot-starter-web 就有内嵌 Tomcat 与 DispatcherServlet，不用写任何配置）。理解它的抓手：启动日志里的 `Auto-configuration Report` 与源码 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件——打开看一眼，比背十篇文章都直观。**依赖注入**：`@Service`/`@Repository` 标记 Bean，`@Autowired`（或构造器注入）让容器装配依赖。本 Demo 加一个极简 service 层示范：

```java
@Service
public class GreetingService {
    public String greet(String name) {
        return "Hello, " + (name == null ? "Stranger" : name);
    }
}
```

Controller 构造器注入该 service，接口返回 service 的结果。动手验证"注入"的含义：把 `@Service` 注释掉，启动会报"找不到 GreetingService 类型的 Bean"——这个报错看懂后，依赖注入机制就通了。

**注入方式的演进与选择**是面试常问：**构造器注入**（当前推荐——final 字段、不可变、显式依赖，Spring 官方与 IDEA 检查都推荐）、**字段注入**（@Autowired 直接标字段——写起来最短，但依赖隐藏、难以测试，老教程大量使用）、**setter 注入**（可选依赖场景）。Demo 用构造器注入：`private final GreetingService service;` + 构造器，Lombok 的 `@RequiredArgsConstructor` 可以自动生成构造器（final 字段自动注入）——Level2 的代码风格从这一刻定型。

**多环境配置**的概念也顺手建立：生产与开发环境配置不同（数据库地址、日志级别），Spring Boot 用**profile**解决——`application-dev.properties`（开发）+ `application-prod.properties`（生产）+ 主配置里 `spring.profiles.active=dev` 选择激活哪个。Demo 阶段只建一个 `application.properties` 即可，但**理解"配置要能切换环境"**这个意识，是 Level2 工程化的前提之一。

## 7. 常见坑

**启动报"端口占用"**：先 `netstat -ano | findstr 8080` 找占用进程，或直接改 `server.port`。

**接口 404/405**：404 查路径拼写与类级前缀；405 查方法类型（GET 请求打到了 POST 接口）。URL 大小写敏感，`/Hello` 与 `/hello` 不同。

**JSON 解析报错**：请求体与 record 字段名不一致（大小写、拼写），或 Content-Type 不是 application/json。报错信息会指出哪个字段，对照修正。

**"Consider defining a bean of type"**：某个接口的实现类没加 @Service/@Repository，或类不在主类子包下（组件扫描范围）。主类在 `com.example.demo`，所有组件必须在这个包及其子包内。

**中文乱码**：响应中文变问号——确认 IDEA 文件编码 UTF-8，且无响应头编码问题（Spring Boot 默认 UTF-8，多数乱码来自控制台显示设置而非代码）。

## 8. 核心要点

1. start.spring.io 生成工程：Boot 4.1.x + Spring Web + DevTools，404 是正常现象。
2. 三个注解入门：@RestController、@GetMapping、@RequestBody；record 直接当 DTO。
3. 配置三样：端口、应用名、日志级别；DevTools 热部署一次配对。
4. 自动配置看 `AutoConfiguration.imports` 文件，依赖注入用"删 @Service 看报错"验证。
5. 报错排查顺序：环境（端口/编码）→ 注解（扫描范围）→ 请求（URL/方法/JSON）。

> 🎯 **核心要点**：本 Demo 的完成标志不是"接口能跑"，而是**"启动发生了什么"与"Bean 从哪来"两个问题能讲清**。这两个机制是理解后续一切 Spring 生态组件（包括 Spring AI）的钥匙——它们都是"自动配置 + 依赖注入"的产物。

---

**下一模块**：[04 数据访问 Demo](./04-数据访问%20Demo%EF%BC%9AMySQL%20与%20MyBatis-Plus.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)
