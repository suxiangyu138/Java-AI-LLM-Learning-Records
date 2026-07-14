# Java后端开发知识点 模块化归类
核心遵循**「基础铺垫→核心开发→框架核心→项目支撑→进阶优化→工程规范」的顺序，每个模块标注核心掌握程度和学习/应用重点**，新手可按模块逐步攻坚，避免知识点混乱。

## 一、基础必备模块（★★★★★ 必考/必会，后端根基）
1. JavaSE 核心语法
    - 基础：数据类型、运算符、流程控制（分支/循环）、数组、字符串（String/StringBuilder/StringBuffer）
    - 面向对象：封装/继承/多态、抽象类/接口、内部类、枚举、注解
    - 核心类库：集合框架（List/Set/Map/Queue，底层原理+常用方法）、IO/NIO（文件操作/流处理）、日期时间（LocalDateTime等）、异常处理（自定义异常/异常链）
    - 进阶基础：多线程（创建方式/锁机制/线程池/并发集合）、反射/注解、泛型、Lambda表达式/Stream流
2. 计算机基础
    - 操作系统：进程/线程、内存管理、Linux常用命令（文件操作/权限/进程/服务部署）
    - 计算机网络：TCP/IP协议簇、HTTP/HTTPS、Socket、请求响应模型、Cookie/Session/Token
    - 数据结构与算法：数组/链表/栈/队列/树/哈希表、排序/查找算法、LeetCode简单/中等题（后端面试必问）
    - 设计模式：单例/工厂/代理/装饰器/观察者等常用设计模式（Spring底层大量使用）

## 二、数据库核心模块（★★★★★ 开发必备，高频使用）
1. 关系型数据库（MySQL 为主）
    - 基础：建库建表、数据类型、约束、增删改查（CRUD）、联表查询（join）、子查询、分组/排序/分页
    - 进阶：索引（分类/创建原则/失效场景）、事务（ACID/隔离级别/事务传播）、存储过程/函数、视图
    - 优化：慢查询分析、EXPLAIN执行计划、表结构优化（分库分表/分区）、索引优化
2. 非关系型数据库（Redis 为主，必学；MongoDB 可选）
    - Redis：数据类型（String/List/Hash/Set/ZSet）、常用命令、持久化（RDB/AOF）、缓存穿透/击穿/雪崩解决方案、分布式锁、Redis集群
    - MongoDB（可选）：文档型数据库基础、CRUD、索引，适用于非结构化数据存储场景

## 三、JavaWeb 基础模块（★★★★ 框架底层基础，理解即可）
- 核心：Servlet、Filter、Listener、JSP（现在极少用，了解即可）
- 核心概念：请求转发/重定向、请求参数解析、会话管理、MVC设计模式
- 技术栈：Tomcat服务器（配置/部署/优化）、Maven/Gradle（项目构建/依赖管理）
- 重点：无需深钻开发，理解底层原理即可（SpringMVC是对Servlet的封装）

## 四、主流框架核心模块（★★★★★ 企业级开发核心，重中之重）
1. 基础框架（SSM）→ 理解底层，为SpringBoot打基础
    - Spring Core：IOC容器（依赖注入/控制反转）、AOP（面向切面编程/动态代理）、Bean生命周期/作用域
    - MyBatis：核心配置、Mapper映射、动态SQL、分页插件（PageHelper）、MyBatis-Plus（必学，简化开发）
    - SpringMVC：核心流程、请求映射、参数绑定、返回值处理、拦截器、异常处理器
2. 主流开发框架（SpringBoot 必学，后端标配）
    - 核心：自动配置、起步依赖、核心注解、配置文件（application.yml/properties）、多环境配置
    - 开发：Web开发（接口编写/RESTful风格）、数据访问（整合MyBatis/Redis）、事务管理、异常统一处理
    - 常用组件：拦截器/过滤器、自定义注解、starter自定义、整合第三方工具（如EasyExcel/邮件）
3. 微服务框架（SpringCloud 主流，中高级开发必备）
    - 核心组件：Nacos（服务注册/配置中心）、OpenFeign（服务远程调用）、Gateway（网关/路由/限流）
    - 服务治理：Sentinel（限流/熔断/降级）、Seata（分布式事务）
    - 进阶：SpringCloud Alibaba 生态（主流企业使用，优先学）、Docker/K8s（容器化部署，微服务配套）

## 五、项目开发支撑模块（★★★★ 开发必备，提升效率/保证项目稳定性）
1. 接口开发与测试
    - 接口规范：RESTful API 设计、接口文档（Swagger/Knife4j）
    - 接口测试：Postman/JMeter、单元测试（JUnit5/Mockito）
2. 数据处理与交互
    - 数据传输：JSON（FastJSON/Jackson/Gson）、对象拷贝（BeanUtils/MapStruct）
    - 文件处理：文件上传/下载、大文件处理、EasyExcel（Excel导入导出）
3. 日志与监控
    - 日志框架：SLF4J + Logback/Log4j2（日志配置/分级/输出）
    - 监控：SpringBoot Actuator、Prometheus + Grafana（可选，企业级监控）
4. 安全框架
    - 基础：Shiro（入门简单，小型项目）
    - 主流：Spring Security（必学，整合SpringBoot，支持OAuth2.0/JWT）
    - 核心：认证/授权、密码加密（MD5/SHA256/Bcrypt）、JWT令牌（无状态认证）

## 六、进阶优化模块（★★★★ 中高级开发必备，提升竞争力）
1. JVM 深入理解与调优
    - 核心：JVM内存结构（堆/栈/方法区/元空间）、垃圾回收算法/收集器（CMS/G1/ZGC）
    - 调优：JVM参数配置、垃圾回收日志分析、内存泄漏/溢出排查（MAT/JProfiler）
2. 分布式开发核心
    - 分布式理论：CAP/BASE、最终一致性
    - 分布式技术：分布式ID（雪花算法/UUID）、分布式锁、分布式事务（Seata/TCC）
    - 服务通信：RPC框架（Dubbo 可选，了解原理）、HTTP远程调用
3. 性能优化与问题排查
    - 项目优化：接口优化、缓存优化、数据库优化、服务器优化
    - 问题排查：线上bug排查、日志分析、JVM问题排查、网络问题排查
4. 中间件进阶（企业级开发高频使用）
    - 消息队列：RabbitMQ/Kafka（核心概念/消息模型/可靠性投递/消费削峰）
    - 搜索引擎：Elasticsearch（基础/索引/查询，适用于全文检索场景）
    - 任务调度：Quartz/Spring Task/Xxl-Job（定时任务开发）

## 七、工程化与部署模块（★★★★ 企业级项目必备，落地关键）
1. 版本控制
    - Git：基础命令（拉取/提交/推送/分支）、分支管理（GitFlow）、Gitee/GitHub使用
2. 项目部署与运维
    - 容器化：Docker（镜像/容器/镜像仓库，必学）、Docker Compose（多容器编排）
    - 服务器：Linux服务器部署（SpringBoot项目打包/运行/守护进程）、Nginx（反向代理/负载均衡/静态资源部署）
    - 持续集成/持续部署（CI/CD）：Jenkins（可选，了解流程）
3. 工程规范
    - 代码规范：命名规范、注释规范、代码结构规范（阿里巴巴Java开发手册，必看）
    - 开发规范：接口规范、数据库设计规范、日志规范、异常处理规范
    - 团队协作：Git协作流程、需求分析/接口设计/开发提测流程

## 八、可选拓展模块（★★★ 按需学习，适配特定场景）
- 网络编程：Netty（高性能NIO框架，适用于高并发通信场景）
- 大数据相关：Flink/Spark（可选，适配大数据开发方向）
- 其他：RPC框架（Dubbo）、NoSQL（Redis Cluster/MongoDB）、服务网格（Istio，高级）
    新手学习模块优先级建议
    1. 第一阶段：JavaSE核心 + 计算机基础（Linux/网络/数据结构） + MySQL基础 → 打牢根基
    2. 第二阶段：JavaWeb基础 + Maven + SpringBoot核心 + MyBatis-Plus → 能做简单接口开发
    3. 第三阶段：Redis + SpringMVC深入 + 项目实战（如后台管理系统） → 积累项目经验
    4. 第四阶段：SpringCloud Alibaba + 消息队列 + JVM调优 → 向中高级开发进阶
    5. 第五阶段：分布式开发 + 容器化 + 性能优化 → 适配企业级高并发项目
