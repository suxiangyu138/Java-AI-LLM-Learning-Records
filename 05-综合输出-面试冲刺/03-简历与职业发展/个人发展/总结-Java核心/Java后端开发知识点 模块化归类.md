# Java 后端开发知识点模块化归类

## 概述

本文按 **基础铺垫 → 核心开发 → 框架核心 → 项目支撑 → 进阶优化 → 工程规范** 六大层次，将 Java 后端开发知识点模块化归类，每模块标注掌握程度（★）和学习重点，新手可按模块逐步攻坚。

---

## 知识体系全景

```
一、基础必备（地基）        二、数据库核心（数据层）      三、JavaWeb 基础（过渡层）
  JavaSE + 计算机基础          MySQL + Redis                  Servlet + Tomcat + Maven

四、主流框架核心（骨架层）    五、项目开发支撑（交付层）    六、进阶优化（能力层）
  SSM + SpringBoot + Cloud     接口/日志/安全/监控           JVM/分布式/性能/MQ/ES

七、工程化与部署（落地层）    八、可选拓展（扩展层）
  Git/Docker/CICD/Nginx          Netty/大数据/Dubbo
```

---

## 一、基础必备模块（★★★★★ 必考必会）

| 模块 | 核心知识点 | 重点要求 |
|------|---------|---------|
| **JavaSE 核心语法** | 数据类型、运算符、流程控制、数组、String/StringBuilder/StringBuffer；OOP（封装/继承/多态、抽象类/接口、内部类、枚举、注解） | 熟练掌握 |
| **JavaSE 核心类库** | 集合框架（List/Set/Map/Queue 底层原理）、IO/NIO、日期时间（LocalDateTime）、异常处理（自定义异常/异常链） | 理解底层原理 |
| **JavaSE 进阶** | 多线程（创建方式/锁机制/线程池/并发集合）、反射/注解、泛型、Lambda/Stream | 理解+面试口述 |
| **计算机基础** | 操作系统（进程/线程/内存管理/Linux 常用命令）、计算机网络（TCP/IP/HTTP/HTTPS/Socket/Cookie/Session/Token）、数据结构与算法（数组/链表/栈/队列/树/哈希表/排序/查找） | 面试必考 |
| **设计模式** | 单例/工厂/代理/装饰器/观察者等（Spring 底层大量使用） | 能说出应用场景 |

---

## 二、数据库核心模块（★★★★★ 开发必备）

| 模块 | 核心知识点 |
|------|---------|
| **MySQL 基础** | 建库建表、数据类型、约束、CRUD、联表查询（JOIN）、子查询、分组/排序/分页 |
| **MySQL 进阶** | 索引（分类/创建原则/失效场景）、事务（ACID/隔离级别/传播机制）、存储过程/函数、视图 |
| **MySQL 优化** | 慢查询分析、EXPLAIN 执行计划、表结构优化（分库分表/分区）、索引优化 |
| **Redis** | 数据类型（String/List/Hash/Set/ZSet）、持久化（RDB/AOF）、缓存穿透/击穿/雪崩、分布式锁、集群（主从/哨兵/Cluster） |
| **MongoDB**（可选） | 文档型 CRUD、索引，适用于非结构化数据 |

---

## 三、JavaWeb 基础模块（★★★★ 理解原理即可）

| 核心内容 | 说明 |
|---------|------|
| **Servlet / Filter / Listener** | Servlet 生命周期、请求转发与重定向、会话管理——**理解底层原理**（Spring MVC 是对 Servlet 的封装） |
| **JSP** | 现在极少用，了解即可 |
| **Tomcat** | 配置、部署、优化 |
| **Maven / Gradle** | 项目构建、依赖管理 |
| **MVC 设计模式** | Model-View-Controller 分层思想 |

---

## 四、主流框架核心模块（★★★★★ 重中之重）

### 4.1 SSM 基础框架

| 框架 | 核心知识点 |
|------|-----------|
| **Spring Core** | IoC 容器（DI/控制反转）、AOP（动态代理 JDK/CGLIB）、Bean 生命周期/作用域 |
| **Spring MVC** | 核心流程、请求映射、参数绑定、返回值处理、拦截器、异常处理器 |
| **MyBatis** | 核心配置、Mapper 映射、动态 SQL、分页插件（PageHelper）、MyBatis-Plus（必学） |

### 4.2 主流开发与微服务

| 框架 | 核心知识点 |
|------|-----------|
| **Spring Boot** | 自动配置、Starter、核心注解、配置文件（yml/properties）、多环境配置、Web 开发、数据访问整合、事务管理、异常统一处理 |
| **Spring Cloud Alibaba** | Nacos（注册+配置）、OpenFeign（远程调用）、Gateway（网关/路由/限流）、Sentinel（限流/熔断/降级）、Seata（分布式事务） |
| **Docker / K8s** | 容器化部署，微服务配套必学 |

---

## 五、项目开发支撑模块（★★★★）

| 领域 | 核心技术点 |
|------|-----------|
| **接口开发与测试** | RESTful API 设计规范、Swagger/Knife4j 接口文档、Postman/JMeter 测试、JUnit 5/Mockito 单元测试 |
| **数据处理** | JSON（FastJSON/Jackson/Gson）、对象拷贝（MapStruct）、文件上传/下载、EasyExcel（Excel 导入导出） |
| **日志与监控** | SLF4J + Logback/Log4j2、Spring Boot Actuator、Prometheus + Grafana（企业级监控） |
| **安全框架** | Spring Security（必学，OAuth2/JWT）、Shiro（入门简单）、认证/授权、密码加密（BCrypt）、JWT 无状态认证 |

---

## 六、进阶优化模块（★★★★ 中高级必备）

| 领域 | 核心知识点 |
|------|-----------|
| **JVM 深入** | 内存结构（堆/栈/方法区/元空间）、GC 算法与收集器（CMS/G1/ZGC）、JVM 参数调优、内存泄漏/溢出排查（MAT/JProfiler） |
| **分布式核心** | CAP/BASE 理论、分布式 ID（雪花算法/UUID）、分布式锁、分布式事务（Seata/TCC）、RPC（Dubbo） |
| **性能优化** | 接口优化、缓存优化、数据库优化、Linux 服务器优化、线上 Bug/日志/网络问题排查 |
| **消息队列** | RabbitMQ/Kafka（核心概念/消息模型/可靠性投递/削峰） |
| **搜索引擎** | Elasticsearch（倒排索引/Query DSL/全文检索） |
| **任务调度** | Quartz / Spring Task / XXL-Job |

---

## 七、工程化与部署模块（★★★★ 落地关键）

| 领域 | 核心技术点 |
|------|-----------|
| **版本控制** | Git（拉取/提交/推送/分支管理/GitFlow）、Gitee/GitHub |
| **容器化部署** | Docker（镜像/容器/仓库）、Docker Compose（多容器编排） |
| **Linux 部署** | Spring Boot 项目打包/运行/守护进程、Nginx（反向代理/负载均衡/静态资源） |
| **CI/CD** | Jenkins（自动化构建/测试/部署流程） |
| **工程规范** | 阿里巴巴 Java 开发手册、接口规范、数据库设计规范、日志规范、Git 协作流程 |

---

## 八、可选拓展模块（★★★ 按需学习）

| 方向 | 技术 |
|------|------|
| 网络编程 | Netty（高性能 NIO 框架） |
| 大数据 | Flink / Spark |
| 微服务进阶 | Dubbo、Redis Cluster、MongoDB、Istio 服务网格 |

---

## 学习优先级路线图

```
第一阶段              第二阶段               第三阶段
JavaSE + 计算机基础     JavaWeb + Maven        SpringBoot + MyBatis-Plus
MySQL 基础              + Redis                + 项目实战

第四阶段               第五阶段
SpringCloud Alibaba     分布式/容器化
+ MQ + JVM 调优         + 性能优化
```

---

*最后更新：2026-07-15*
