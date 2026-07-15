# SpringBoot 教程

> 从零搭建 Spring Boot 项目，覆盖自动配置、Web 开发、数据访问、安全管理、任务调度、邮件发送、项目实战。

---

## 1. Spring Boot 基础

- Spring Boot 概述与核心特性
- 环境搭建（IDEA + Maven + JDK）
- 第一个 Spring Boot 项目
- 自动配置原理
- Starter 依赖管理
- 配置文件（properties / yml）

## 2. Web 应用开发

- `@RestController` 与 `@RequestMapping`
- 请求参数接收（`@PathVariable` / `@RequestParam` / `@RequestBody`）
- 统一异常处理
- 跨域配置
- 静态资源访问
- 内嵌 Tomcat 配置与切换（Jetty/Undertow）

## 3. 数据访问

- Spring Boot 整合 JDBC / JdbcTemplate
- Spring Boot 整合 MyBatis（注解 + XML）
- Spring Boot 整合 MyBatis-Plus
- 数据库连接池 HikariCP 配置
- 事务管理 @Transactional

## 4. 安全管理

- Spring Security 整合
- 认证（Authentication）与授权（Authorization）
- BCrypt 密码加密
- 数据库用户认证
- JWT Token 认证

## 5. 消息服务

- RabbitMQ 整合（发送 + 接收）
- 消息确认机制（生产者 Confirm / 消费者 ACK）

## 6. 任务调度与邮件

- `@Scheduled` 定时任务
- Quartz 整合
- JavaMail 邮件发送（文本/HTML/附件/图片）

## 7. 综合项目实战

- 瑞吉外卖：Spring Boot + MyBatis-Plus + Redis + Spring Security
- 员工管理、菜品管理、订单管理
- Docker 容器化部署
