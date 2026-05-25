# Spring 家族全体成员
Spring生态组件分类整理
本文按核心 → Web → 数据 → 消息 → 安全 → 云原生 → AI → 其他对Spring生态组件进行归类，方便面试学习快速查阅。

## 一、核心全家桶（地基）
Spring Framework：整个家族的根，包含 IOC、AOP、事务、JDBC 等核心模块。
Spring Boot：快速开发框架，自动配置、内嵌服务器、starter 开箱即用。
Spring Cloud：微服务全家桶，基于 Spring Boot 做分布式系统。

## 二、Web 相关
Spring MVC：传统 Web 开发框架，处理请求、控制器、视图。
Spring WebFlux：响应式编程 Web 框架，非阻塞、高并发。

## 三、数据访问层
Spring Data JPA：简化数据库操作，几乎不用写 SQL。
Spring Data Redis：Redis 操作封装。
Spring Data MongoDB：MongoDB 操作封装。
Spring Data Elasticsearch：ES 搜索引擎封装。
Spring JDBC：最基础的 JDBC 模板。
Spring MyBatis（整合包）：Spring 整合 MyBatis。

## 四、消息队列
Spring AMQP：对接 RabbitMQ 等消息队列。
Spring Kafka：对接 Kafka。
Spring JMS：传统 Java 消息服务。

## 五、安全权限
Spring Security：登录认证、权限控制、OAuth2、JWT 全套安全方案。
OAuth2 / OIDC：第三方登录、开放授权。

## 六、微服务 & 云原生（Spring Cloud 子项目）
Spring Cloud Netflix：Eureka、Ribbon、Feign、Hystrix（老一代微服务）
Spring Cloud Alibaba：Nacos、Sentinel、Seata、RocketMQ（国内主流）
Spring Cloud Gateway：网关路由、限流、过滤。
Spring Cloud Config：统一配置中心。
Spring Cloud OpenFeign：声明式 HTTP 客户端。

## 七、AI & 大模型
Spring AI：Spring 官方对接大模型：ChatGPT、通义千问、文心一言等，支持 RAG、Function Calling、Prompt 管理。

## 八、其他常用
Spring Batch：批处理、定时大量数据读写。
Spring Shell：命令行应用开发。
Spring HATEOAS：RESTful 风格增强。
极简总结
根：Spring Framework
快速开发：Spring Boot
Web：Spring MVC / WebFlux
数据：Spring Data 系列
微服务：Spring Cloud / Cloud Alibaba
安全：Spring Security
AI 大模型：Spring AI
