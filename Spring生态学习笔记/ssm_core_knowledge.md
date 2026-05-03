# SSM 核心知识点详解

## 概述
SSM 通常指 Spring、Spring MVC 和 MyBatis 三个框架的组合，它们分别负责应用的业务管理、Web 请求处理和数据库持久化，是传统 Java Web 项目中非常经典的一套分层开发方案。[cite:4][cite:5] 这套组合强调职责拆分与模块解耦，常见于中小型管理系统、教学项目和面试高频项目场景中。[cite:5][cite:10]

## 整体架构
SSM 项目一般采用分层架构，常见划分为 Controller、Service、Dao/Mapper 和 View 四层。[cite:5] Controller 层负责接收请求和返回响应，Service 层负责组织业务逻辑，Dao 或 Mapper 层负责与数据库交互，View 层负责页面展示或前后端分离场景下的数据呈现。[cite:4][cite:5]

| 层次 | 核心职责 | 常见技术 |
|------|----------|----------|
| Controller 层 | 接收 HTTP 请求、参数绑定、返回页面或 JSON | Spring MVC [cite:4] |
| Service 层 | 业务逻辑编排、事务控制 | Spring [cite:4][cite:5] |
| Dao/Mapper 层 | 执行 SQL、数据映射 | MyBatis [cite:4][cite:8] |
| View 层 | 页面渲染或前端展示 | JSP 等 [cite:5] |

## Spring 核心
Spring 在 SSM 中主要承担容器管理、依赖注入、AOP 和事务管理等职责。[cite:2][cite:4] 它的核心价值是降低对象之间的耦合度，让业务类专注于业务逻辑，而不是对象创建和依赖维护。[cite:2][cite:5]

### IoC 与 DI
IoC 即控制反转，表示对象的创建权从程序员手中交给 Spring 容器管理。[cite:2][cite:4] DI 即依赖注入，表示容器在运行时将一个对象依赖的其他对象自动装配进去，常见方式包括构造器注入、setter 注入和字段注入。[cite:2]

### Bean 管理
Spring 容器负责 Bean 的实例化、初始化和销毁，常见作用域包括 singleton 和 prototype。[cite:4][cite:5] 在实际开发中，大多数 Service 和 Dao Bean 会使用单例模式，这样可以减少对象创建开销并便于统一管理。[cite:4]

### AOP
AOP 是面向切面编程，适合将日志、权限、事务等横切关注点从业务代码中分离出来。[cite:2][cite:4] 其核心术语包括切面 Aspect、通知 Advice、切点 Pointcut 和连接点 JoinPoint，底层通常通过动态代理完成方法增强。[cite:4]

### 事务管理
Spring 支持声明式事务管理，通常配合 `@Transactional` 使用，通过 AOP 代理机制控制事务提交与回滚。[cite:4][cite:5] 当一个业务方法包含多次数据库写操作时，事务可以保证这些操作要么全部成功，要么全部失败，从而保证数据一致性。[cite:4]

## Spring MVC 核心
Spring MVC 是 SSM 中的 Web 层框架，核心思想是基于 MVC 模式对请求处理流程进行统一调度。[cite:4][cite:6] 它采用前端控制器模式，通过 DispatcherServlet 统一接收请求，再分发到对应的处理器方法。[cite:4]

### MVC 模式
MVC 即 Model、View、Controller 的分离思想，其中 Model 负责数据，View 负责展示，Controller 负责调度与控制。[cite:4][cite:6] 这种分层方式有助于前后职责清晰，也方便后期维护和功能扩展。[cite:5]

### 核心组件
Spring MVC 的核心组件包括 DispatcherServlet、HandlerMapping、Controller、ModelAndView 和 ViewResolver。[cite:4] DispatcherServlet 是前端控制器，HandlerMapping 负责根据请求路径查找处理器，ViewResolver 负责把逻辑视图名解析为真实视图资源。[cite:4]

### 请求流程
浏览器发起请求后，请求先进入 DispatcherServlet，再由 HandlerMapping 找到对应 Controller 方法，随后调用 Service 和 Dao 完成业务与数据访问，最后将结果返回给视图解析器或直接返回 JSON 数据。[cite:4][cite:5] 这是 SSM 面试中非常高频的一条主线，通常需要能口头完整描述一遍。[cite:4][cite:10]

## MyBatis 核心
MyBatis 是一个半自动 ORM 持久层框架，能够在简化 JDBC 操作的同时保留开发者对 SQL 的直接控制能力。[cite:4][cite:5] 相比全自动 ORM，MyBatis 更适合对 SQL 性能和复杂查询有较强要求的后端项目。[cite:5][cite:10]

### 核心作用
MyBatis 负责将数据库中的表记录映射为 Java 对象，同时将 Java 参数传递给 SQL 语句执行。[cite:4][cite:8] 它通过 Mapper 接口和 XML 映射文件或注解方式，将方法调用与 SQL 执行关联起来。[cite:4][cite:8]

### 关键对象
SqlSessionFactory 用于创建 SqlSession，SqlSession 是执行增删改查操作的核心会话对象。[cite:4] 在 Spring 整合 MyBatis 后，SqlSession 的创建和管理通常由 Spring 负责，从而减少手工编码成本。[cite:8]

### resultType 与 resultMap
`resultType` 适合简单查询结果直接映射到单个 JavaBean 的场景。[cite:8] `resultMap` 更适合复杂字段映射、字段名不一致映射以及一对多、多对一等关联查询场景，是 MyBatis 中需要重点掌握的高级配置点。[cite:8]

### 动态 SQL
MyBatis 支持动态 SQL，常用标签包括 `if`、`where`、`set` 和 `foreach`，适合处理条件查询、动态更新和批量操作等需求。[cite:8][cite:10] 这是企业开发中非常实用的能力，因为很多查询条件并不是固定不变的。[cite:10]

## SSM 整合思路
SSM 整合的核心是让 Spring 成为统一管理中心，由 Spring 管理 Service、事务、数据源以及 MyBatis 相关对象，再由 Spring MVC 专注处理 Web 请求。[cite:4][cite:8] 这种整合方式的目标不是把三个框架简单堆在一起，而是让每一层都只做自己最擅长的事情。[cite:5]

### 典型整合点
- Spring 配置：组件扫描、数据源、事务管理器、AOP 支持。[cite:4][cite:8]
- Spring MVC 配置：DispatcherServlet、Controller 扫描、静态资源处理、视图解析器。[cite:4]
- MyBatis 配置：SqlSessionFactory、Mapper 扫描、别名配置、映射文件加载。[cite:8]
- 数据访问流程：Controller 调用 Service，Service 调用 Mapper，Mapper 执行 SQL 后返回结果对象。[cite:4][cite:5]

## 面试高频点
SSM 在校招和 Java 后端初中级面试中，常考点并不复杂，但非常看重表达是否完整、是否分层清晰。[cite:5][cite:10] 下面这些问题通常需要做到脱口而出。[cite:10]

### 高频问题清单
- 什么是 IoC 和 DI，它们解决了什么问题。[cite:2][cite:4]
- AOP 的底层原理是什么，适合解决哪些场景。[cite:2][cite:4]
- Spring 事务什么时候会失效，事务传播行为大致有哪些用途。[cite:4][cite:5]
- Spring MVC 的请求执行流程是怎样的。[cite:4]
- `@Controller` 和 `@RestController` 的区别是什么。[cite:4]
- MyBatis 中 `#{}` 和 `${}` 的区别是什么，为什么推荐优先使用 `#{}`。[cite:8][cite:10]
- `resultType` 和 `resultMap` 的区别是什么。[cite:8]
- SSM 三大框架各自负责什么，为什么这样分层更合理。[cite:4][cite:5]

## 学习重点建议
对于 Java 后端学习者，SSM 的重点不是死记配置，而是理解每层职责、掌握一次请求的完整流转路径，并能独立完成基础 CRUD 项目开发。[cite:4][cite:5] 如果目标是求职，最好做到既能说清八股概念，也能从零搭建一个包含登录、分页查询、增删改查和事务控制的 SSM 项目。[cite:5][cite:10]

### 建议优先掌握的内容
1. Spring 的 IoC、DI、AOP、事务。[cite:2][cite:4]
2. Spring MVC 的执行流程、参数绑定、返回值处理。[cite:4]
3. MyBatis 的 Mapper、动态 SQL、结果映射、分页基础。[cite:8][cite:10]
4. SSM 的分层设计思想和整合配置逻辑。[cite:4][cite:5]
5. 一个完整的学生管理或后台管理系统实战项目。[cite:5][cite:10]
