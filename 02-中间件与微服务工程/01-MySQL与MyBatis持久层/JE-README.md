# Java Stage 3: Web与数据库 (Web & Database)

> **阶段目标**: 掌握 Web 开发核心协议、Servlet 容器原理、关系型数据库深度应用，具备独立构建企业级 Web 后端的能力。
>
> **预估时间**: 2-3 周（每天 3-5 小时）
>
> **前置要求**: 完成 Stage 1（语言基础）和 Stage 2（核心机制），理解 Java 语法、面向对象、集合框架、I/O 及多线程基础。

---

## 目录结构

| 文件 | 核心主题 | 建议耗时 |
|------|---------|---------|
| [01-HTTP协议与Web基础](./01-HTTP协议与Web基础.md) | HTTP/HTTPS 协议、RESTful API、认证机制 | 3-4 天 |
| [02-Servlet与Tomcat](./02-Servlet与Tomcat.md) | Servlet 生命周期、Filter/Listener、Tomcat 架构、Thymeleaf | 4-5 天 |
| [03-MySQL核心](./03-MySQL核心.md) | 存储引擎、索引原理、事务 MVCC、锁机制、SQL 优化 | 5-7 天 |
| [04-JDBC与连接池](./04-JDBC与连接池.md) | JDBC API、PreparedStatement、HikariCP、Druid、企业实践 | 2-3 天 |

---

## 为什么需要学习 Web 与数据库？

### 现实中的"Hello World"已经不够了

在 Stage 1 和 Stage 2 中，你写的是**在 IDE 控制台里打印的文字**；在 Stage 3，你将要写的是**能通过浏览器访问、能存储用户数据、能处理并发请求的系统**。

绝大多数 Java 开发者的日常工作围绕以下公式展开：

```
Java 后端开发 = Web 服务器 (Tomcat) + 数据库 (MySQL) + 业务逻辑 (Java)
```

本阶段的目标是让你理解这个公式中每一环的**底层原理**，而不只是停留在"配置好就能跑"的层面。

### 为什么在 Spring Boot 时代还要学 Servlet？

这是一个值得深思的问题。Spring Boot 的 auto-configuration 让 Web 开发变得极其简单：

```java
// 你只需要写这个，Spring Boot 就能跑起来一个 Web 应用
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() { return "Hello, World!"; }
}
```

但**抽象层越厚，遇到问题时的排查成本就越高**。当出现以下问题时，理解底层是唯一解决路径：

| 场景 | 表面现象 | 底层根因 |
|------|---------|---------|
| 请求参数无法接收 | `request.getParameter()` 返回 null | HTTP 请求 body 格式、编码问题、Servlet 容器解析规则 |
| Session 频繁丢失 | 用户登录后不久被踢出 | Cookie JSESSIONID 的 Domain/Path 配置、分布式 Session 同步 |
| 过滤器不生效 | 跨域配置没起作用 | Filter 匹配规则、FilterChain 执行顺序、DispatcherServlet 关系 |
| 数据库连接池耗尽 | 应用假死、Tomcat 线程池满 | 连接泄漏、HikariCP 参数配置不当、SQL 慢查询 |
| 慢查询导致系统崩溃 | 页面加载慢、CPU 100% | 索引失效、全表扫描、锁等待 |

**SSH（Struts + Spring + Hibernate）时代到 Spring Boot 时代，底层原理从未改变**——只是框架帮你做了更多自动配置。

> **理解 Servlet、HTTP、JDBC 等底层技术，是为框架排查问题时的"最终手段"。**

### 本阶段与前后阶段的关联

```
Stage 1 (语言基础)                Stage 2 (核心机制)                   Stage 3 (Web与数据库)
  ├── Java 语法                  ├── JVM 内存模型                    ├── HTTP 协议
  ├── 面向对象                   ├── 多线程/并发                     ├── Servlet 容器
  ├── 集合框架                   ├── I/O 模型 / NIO                ├── MySQL 核心
  ├── 异常处理                   ├── 反射/注解                      ├── JDBC / 连接池
  └── 基础 I/O                   └── 泛型                           └── MyBatis / JPA
                                                                         ↓
                                                                  Stage 4 (Spring全家桶)
                                                                    ├── Spring IoC/AOP
                                                                    ├── Spring MVC (底层是 Servlet)
                                                                    ├── Spring Boot (内嵌 Tomcat)
                                                                    ├── Spring Data (封装 JDBC/JPA)
                                                                    └── Spring Security (基于 Filter)
```

**Stage 3 的每一分努力，都会在 Stage 4 的学习中获得超额回报。**

---

## 学习路径规划

### 第 1 周：HTTP + Servlet + Tomcat

| 天 | 学习内容 | 实践任务 |
|----|---------|---------|
| Day 1 | HTTP 协议：请求/响应格式、方法、状态码 | 用 curl 发送各种 HTTP 请求，观察请求/响应格式 |
| Day 2 | HTTP 高级：Cookie、Session、CORS、HTTPS | 在浏览器 DevTools 中观察 Cookie 和 CORS 行为 |
| Day 3 | RESTful API 设计原则 | 设计一个 REST API 文档（用户资源、文章资源） |
| Day 4 | Servlet 生命周期、API 核心方法 | 编写第一个 Servlet: 接收参数、返回 JSON |
| Day 5 | Filter + Listener + RequestDispatcher | 实现一个日志 Filter、一个编码 Filter |
| Day 6 | Tomcat 架构：Connector、Container、Pipeline | 配置 Tomcat 连接器参数、部署多个应用 |
| Day 7 | Thymeleaf 模板引擎 | 用 Servlet + Thymeleaf 渲染动态页面 |

### 第 2 周：MySQL 核心

| 天 | 学习内容 | 实践任务 |
|----|---------|---------|
| Day 1 | MySQL 架构、存储引擎（InnoDB vs MyISAM） | 创建不同引擎的表，对比文件结构 |
| Day 2 | 数据类型、DDL/DML | 设计规范的数据表结构 |
| Day 3 | 索引原理（B+Tree、聚簇索引、二级索引） | 建表时设计索引，用 EXPLAIN 验证 |
| Day 4 | 事务 + MVCC | 测试不同隔离级别的并发效果 |
| Day 5 | 锁机制（行锁、间隙锁、Next-Key 锁） | 模拟死锁场景，分析锁等待 |
| Day 6 | SQL 优化 + EXPLAIN 深入 | 优化慢查询，改写 SQL 执行计划 |
| Day 7 | 聚合查询、窗口函数、CTE | 编写复杂报表查询 |

### 第 3 周：JDBC + 连接池 + 综合项目

| 天 | 学习内容 | 实践任务 |
|----|---------|---------|
| Day 1 | JDBC 核心 API（Connection、Statement、ResultSet） | 原生 JDBC 完成 CRUD |
| Day 2 | PreparedStatement + 事务管理 + 批量操作 | 实现批量插入、事务回滚 |
| Day 3 | HikariCP 连接池原理 + 配置实战 | 配置 HikariCP，压测连接池性能 |
| Day 4 | Druid 连接池 + SQL 监控 | 配置 Druid，观察 SQL 监控页面 |
| Day 5 | 综合项目：搭建项目骨架、数据库设计 | 用户表设计、建表、JDBC 工具类封装 |
| Day 6 | 综合项目：用户注册/登录功能 | Servlet + JDBC + Thymeleaf 实现注册登录 |
| Day 7 | 综合项目：CRUD + 页面完善 | 完成用户管理、文章管理、分页功能 |

---

## 实践项目：用户注册登录 + CRUD Web 系统

### 项目概述

使用纯 Servlet + JDBC + Thymeleaf（不借助 Spring 框架）构建一个完整的 Web 应用，包含用户注册登录和企业级 CRUD 功能。

### 功能需求

```text
用户注册登录 Web 系统
├── 用户模块
│   ├── 注册（用户名、密码、邮箱、手机号）
│   ├── 登录（用户名/邮箱 + 密码）
│   ├── 退出登录
│   ├── 查看个人信息
│   └── 修改个人信息
├── 文章模块
│   ├── 发布文章
│   ├── 查看文章列表（分页）
│   ├── 查看文章详情
│   ├── 编辑文章（仅作者/管理员）
│   └── 删除文章（仅作者/管理员）
├── 后台管理
│   ├── 用户列表（管理员查看所有用户）
│   ├── 禁用/启用用户
│   └── 删除违规文章
└── 技术需求
    ├── 使用 Servlet 3.0+ 注解（无 web.xml）
    ├── 使用 Filter 处理编码和登录校验
    ├── 使用 Listener 初始化数据源
    ├── 使用 JDBC + HikariCP 连接池
    ├── 使用 Thymeleaf 模板渲染
    ├── 密码加密存储（BCrypt）
    ├── 防止 SQL 注入（PreparedStatement）
    ├── 防止 XSS 攻击（HTML 转义）
    └── RESTful 风格 URL 设计
```

### 项目结构建议

```
user-management/
├── pom.xml                               # Maven 项目配置
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── config/
│   │   │   │   ├── DataSourceConfig.java     # 数据源配置（HikariCP）
│   │   │   │   └── ThymeleafConfig.java      # Thymeleaf 模板引擎配置
│   │   │   ├── filter/
│   │   │   │   ├── EncodingFilter.java       # 字符编码过滤器
│   │   │   │   ├── LoginFilter.java          # 登录校验过滤器
│   │   │   │   └── XssFilter.java            # XSS 过滤
│   │   │   ├── listener/
│   │   │   │   └── AppContextListener.java   # 应用上下文监听器
│   │   │   ├── model/
│   │   │   │   ├── User.java                 # 用户实体
│   │   │   │   ├── Article.java             # 文章实体
│   │   │   │   └── PageResult.java          # 分页结果
│   │   │   ├── dao/
│   │   │   │   ├── UserDao.java             # 用户数据访问
│   │   │   │   └── ArticleDao.java          # 文章数据访问
│   │   │   ├── service/
│   │   │   │   ├── UserService.java         # 用户业务逻辑
│   │   │   │   └── ArticleService.java      # 文章业务逻辑
│   │   │   ├── servlet/
│   │   │   │   ├── user/
│   │   │   │   │   ├── RegisterServlet.java
│   │   │   │   │   ├── LoginServlet.java
│   │   │   │   │   ├── LogoutServlet.java
│   │   │   │   │   └── ProfileServlet.java
│   │   │   │   ├── article/
│   │   │   │   │   ├── ArticleListServlet.java
│   │   │   │   │   ├── ArticleDetailServlet.java
│   │   │   │   │   ├── ArticleCreateServlet.java
│   │   │   │   │   └── ArticleDeleteServlet.java
│   │   │   │   └── admin/
│   │   │   │       ├── UserManageServlet.java
│   │   │   │       └── DashboardServlet.java
│   │   │   └── util/
│   │   │       ├── PasswordUtil.java        # BCrypt 密码工具
│   │   │       ├── ValidationUtil.java      # 输入校验工具
│   │   │       └── HtmlUtil.java            # HTML 转义工具
│   │   ├── resources/
│   │   │   └── db.properties               # 数据库配置
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   └── templates/              # Thymeleaf 模板
│   │       │       ├── user/
│   │       │       │   ├── login.html
│   │       │       │   ├── register.html
│   │       │       │   └── profile.html
│   │       │       ├── article/
│   │       │       │   ├── list.html
│   │       │       │   ├── detail.html
│   │       │       │   └── form.html
│   │       │       ├── admin/
│   │       │       │   ├── dashboard.html
│   │       │       │   └── user-list.html
│   │       │       └── common/
│   │       │           ├── header.html
│   │       │           └── footer.html
│   │       └── static/
│   │           ├── css/
│   │           └── js/
```

### 数据库设计

```sql
-- 用户表
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色: 0-普通用户, 1-管理员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 文章表
CREATE TABLE `article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
    `content` TEXT NOT NULL COMMENT '文章内容',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
    `user_id` BIGINT NOT NULL COMMENT '作者ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-草稿, 1-已发布, 2-已删除',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_status_created` (`status`, `created_at`),
    FULLTEXT INDEX `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';
```

### 构建工具

使用 Maven 进行项目管理：

```xml
<!-- pom.xml 关键依赖 -->
<dependencies>
    <!-- Servlet API -->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>4.0.1</version>
        <scope>provided</scope>
    </dependency>

    <!-- Thymeleaf 模板引擎 -->
    <dependency>
        <groupId>org.thymeleaf</groupId>
        <artifactId>thymeleaf</artifactId>
        <version>3.1.2.RELEASE</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.0.33</version>
    </dependency>

    <!-- HikariCP 连接池 -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>5.0.1</version>
    </dependency>

    <!-- BCrypt 密码加密 -->
    <dependency>
        <groupId>org.mindrot</groupId>
        <artifactId>jbcrypt</artifactId>
        <version>0.4</version>
    </dependency>

    <!-- Logback 日志 -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.14</version>
    </dependency>
</dependencies>
```

---

## 学习资源推荐

### 书籍

| 书名 | 作者 | 适合主题 | 推荐指数 |
|------|------|---------|---------|
| 《图解 HTTP》 | 上野宣 | HTTP 协议基础 | ⭐⭐⭐⭐⭐ |
| 《HTTP 权威指南》 | David Gourley | HTTP 协议深度 | ⭐⭐⭐⭐ |
| 《深入分析 Java Web 技术内幕》 | 许令波 | Servlet/Tomcat 原理 | ⭐⭐⭐⭐⭐ |
| 《高性能 MySQL》（第3版） | Baron Schwartz | MySQL 深度优化 | ⭐⭐⭐⭐⭐ |
| 《MySQL 是怎样运行的》 | 小孩子4919 | MySQL 原理通俗讲解 | ⭐⭐⭐⭐⭐ |
| 《Redis 设计与实现》 | 黄健宏 | Redis 原理 | ⭐⭐⭐⭐⭐ |
| 《Maven 实战》 | 许晓斌 | 构建工具 | ⭐⭐⭐⭐ |

### 在线资源

- [MDN HTTP 文档](https://developer.mozilla.org/zh-CN/docs/Web/HTTP) -- HTTP 协议权威参考
- [MySQL 官方文档](https://dev.mysql.com/doc/) -- 数据库权威参考
- [Baeldung Servlet 教程](https://www.baeldung.com/tag/servlet) -- Servlet 实战教程
- [Thymeleaf 官方文档](https://www.thymeleaf.org/documentation.html) -- 模板引擎参考
- [HikariCP 配置指南](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby) -- 连接池配置详解
- [阿里 Druid 官方文档](https://github.com/alibaba/druid/wiki) -- 监控与 SQL 防火墙

### 视频资源

- B站：尚硅谷 Servlet 教程
- B站：MySQL 高级教程
- B站：图解 HTTP 协议

---

## 学习建议

### 核心原则

1. **抓大放小**：本阶段知识点非常多，优先掌握核心脉络（HTTP 协议 → Servlet 生命周期 → MySQL 索引/事务 → JDBC 连接池），细节在后续实战中逐步补充。

2. **动手抓包**：学习 HTTP 时打开浏览器 DevTools Network 面板、学习 MySQL 时多使用 EXPLAIN 和 SHOW PROCESSLIST——用工具"看"到底发生了什么。

3. **先会跑再起飞**：不要试图在一个项目中同时使用 MyBatis + JPA + Redis。先用 JDBC 完成功能，再考虑引入框架——**理解原理比会用框架重要 100 倍**。

4. **面向问题学习**：遇到"Session 为什么丢失了？"再去深入学习 Cookie/Session 机制；遇到"SQL 为什么这么慢？"再去深入学习索引优化——以问题驱动学习效率最高。

5. **数据库先行**：写代码前先设计好数据表，字段类型、索引、约束都考虑清楚。好的数据库设计能让后续开发事半功倍。

### 常见误区

| 误区 | 纠正 |
|------|------|
| "Spring Boot 都不用写 web.xml，Servlet 已经过时了" | Spring MVC 的核心 DispatcherServlet 就是一个 Servlet，Filter 也是 Servlet 规范的一部分。所有抽象都建立在 Servlet 之上。 |
| "MyBatis 比 JDBC 方便，所以不用学 JDBC" | MyBatis 的底层就是 JDBC。不理解 JDBC 就无法理解 MyBatis 的 SqlSession、事务管理、连接池配置。 |
| "索引建得越多越好" | 每个索引都会增加写入开销和存储空间。索引设计需要根据查询模式权衡。 |
| "MySQL 默认隔离级别就够了" | 默认的 REPEATABLE READ 在多数场景下够用，但在高并发下可能导致间隙锁竞争，需要根据场景调整为 READ COMMITTED。 |
| "连接池越大越好" | 数据库连接池不是越大越好，过多连接反而会导致数据库上下文切换开销增大。经验值：CPU 核心数 * 2 + 有效磁盘数。 |

### 调试技巧

```
HTTP 调试:
  curl -v http://localhost:8080/api/users    # 查看完整请求/响应
  curl -X POST -H "Content-Type: application/json" -d '{}' http://...
  chrome://net-export/                        # Chrome 网络日志导出

MySQL 调试:
  EXPLAIN SELECT * FROM users WHERE id = 1;   # 查看执行计划
  SHOW PROCESSLIST;                           # 查看当前连接状态
  SHOW ENGINE INNODB STATUS;                  # 查看 InnoDB 状态（含锁信息）
  SET profiling = 1; SHOW profiles;           # 查看 SQL 执行耗时

Tomcat 调试:
  catalina.bat jpda start                     # 远程调试模式启动
  Tomcat 日志目录: logs/catalina.out
  Tomcat 管理控制台: http://localhost:8080/manager/html
```

---

## 版本说明

本文档基于以下软件版本编写：

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | LTS 版本 |
| Servlet API | 6.0 | Jakarta EE 对应，兼容 javax.servlet.* |
| Tomcat | 10.x | 对应 Servlet 6.0 / Jakarta EE 9+ |
| MySQL | 8.0 | 生产环境主流版本 |
| MySQL Connector/J | 8.0.x | JDBC 驱动 |
| HikariCP | 5.x | 默认在 Spring Boot 2/3 中使用 |
| Thymeleaf | 3.1.x | 当代 Java 模板引擎标准 |
| Maven | 3.9+ | 项目构建工具 |

> **注意**：Jakarta EE 将 javax.* 包名迁移到了 jakarta.*（如 javax.servlet → jakarta.servlet）。如果使用 Tomcat 10+，请使用对应的 Jakarta Servlet API。本文示例以 Tomcat 9 + javax.servlet.* 为主，同时标注 Jakarta 版本的差异。

---

## 前置条件检查清单

在开始本阶段之前，请确保：

### 开发环境

- [ ] JDK 17+ 已安装
- [ ] Maven 3.9+ 已安装
- [ ] IntelliJ IDEA（推荐 Ultimate 版本，Community 也可）
- [ ] MySQL 8.0 已安装（或使用 Docker 版本）
- [ ] curl 工具已安装
- [ ] Postman 或类似的 API 调试工具
- [ ] Git 客户端

### 知识点

- [ ] Java 面向对象（类、继承、接口、多态）
- [ ] Java 集合框架（List、Set、Map 的使用）
- [ ] 异常处理（try-with-resources）
- [ ] 注解的基本概念和使用
- [ ] 多线程基础（Thread、Runnable）
- [ ] Maven 项目构建基础
- [ ] Git 版本控制基础

> 如果以上任何一项你还不熟悉，请先完成 Stage 1 和 Stage 2。

---

## 知识地图

```
                  ┌─────────────────────────────────────────────────────┐
                  │            Stage 3: Web 与数据库                    │
                  └─────────────────────────────────────────────────────┘
                                         │
        ┌────────────────────────────────┼────────────────────────────────┐
        │                                │                                │
   ┌────┴────┐                    ┌──────┴──────┐                  ┌──────┴──────┐
   │ Web 基础 │                    │   数据库     │                  │  Java 连接  │
   │         │                    │              │                  │             │
   │ HTTP    │                    │ MySQL 架构   │                  │ JDBC API    │
   │ HTTPS   │                    │ 存储引擎     │                  │ Statement   │
   │ Cookie  │                    │ 数据类型     │                  │ ResultSet   │
   │ Session │                    │ 索引 (B+Tree)│                  │ Transaction │
   │ CORS    │                    │ 事务 / MVCC  │                  │ 连接池      │
   │ RESTful │                    │ 锁机制       │                  │ HikariCP    │
   └────┬────┘                    │ SQL 优化     │                  │ Druid       │
        │                         └──────┬──────┘                  └──────┬──────┘
        │                                │                                │
   ┌────┴────────────────────────────────┴────────────────────────────────┴────┐
   │                              实践项目                                      │
   │                    Servlet + JDBC + Thymeleaf 完整 Web 系统                │
   │                    用户注册登录 + CRUD + 分页 + 权限控制                   │
   └────────────────────────────────────────────────────────────────────────────┘
                                         │
                                         ▼
                              ┌─────────────────────┐
                              │ Stage 4: Spring 全家桶│
                              │ (构建在 Stage 3 之上) │
                              └─────────────────────┘
```

---

## 下一步

完成本阶段后，你将进入 **Stage 4: Spring 全家桶**。届时你会发现：

- Spring MVC 的 `DispatcherServlet` 本质上就是一个 Servlet
- Spring Boot 的内嵌 Tomcat 就是 Embedded Tomcat
- Spring Security 的 Filter 链就是对 Servlet Filter 的封装和增强
- Spring Data JPA 的底层就是 JDBC
- Spring 声明式事务就是对 JDBC 事务的 AOP 封装

**本阶段所有的知识都会在 Stage 4 中以更高级的形式再次出现。** 基础越牢固，理解 Spring 的设计思想和源码就越轻松。

> **"计算机科学中只有两件难事：缓存失效和命名。" —— Phil Karlton**
>
> 而 Web 开发中只有两件核心事：**理解网络协议** 和 **管理数据状态**。

---

*最后更新: 2026-05-31*
