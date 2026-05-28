# Spring 完整项目清单

## 一、核心能力

Spring 是 Java 后端开发核心生态，必须掌握：

- IOC 容器与依赖注入
- AOP 面向切面编程
- 声明式事务管理
- SpringMVC Web 请求处理
- SpringBoot 自动配置
- 中间件整合（Redis / RabbitMQ / ES）
- 微服务架构（SpringCloud）
- AI 大模型应用整合

---

## 二、技术能力拆解

- 原理层：IOC / AOP / Bean 生命周期 / 循环依赖 / 动态代理
- 事务层：传播机制 / 隔离级别 / @Transactional / 失效场景
- Web 层：请求分发 / 参数绑定 / 拦截器 / 异常处理
- Boot 层：自动配置 / 约定优于配置 / Starter 机制
- 持久层：MyBatis / MyBatis-Plus / JdbcTemplate
- 缓存层：Redis 三大问题 / 多级缓存 / 分布式锁
- 消息层：RabbitMQ 交换机 / 死信队列 / 异步解耦
- 搜索层：Elasticsearch 分词 / 高亮 / 聚合
- 微服务层：Nacos / Gateway / Feign / Sentinel / Seata
- AI 层：大模型封装 / RAG / 向量检索 / 流式输出

---

## 三、Spring 核心基础项目

### 1. 手写简易 IOC 容器
**技术栈**：Java SE + 反射 + 注解

**实现**：
- BeanDefinition 定义
- 反射实例化
- 依赖注入
- 单例池
- Bean 生命周期
- 循环依赖处理
- 注解扫描

**产出**：
- 极简版 Spring IOC 容器

**耗时**：3~4天

---

### 2. 手写 AOP 动态代理框架
**技术栈**：Java SE + 动态代理

**实现**：
- JDK 动态代理
- CGLIB 动态代理
- 切面定义
- 切点匹配
- 通知类型
- 事务环绕增强

**产出**：
- 极简版 AOP 框架

**耗时**：3~4天

---

### 3. Spring 声明式事务实战
**技术栈**：Spring + MySQL

**实现**：
- 转账业务
- 库存扣减
- 事务传播机制
- 隔离级别
- @Transactional 配置
- 事务失效场景

**产出**：
- 事务管理完整案例

**耗时**：2~3天

---

### 4. Spring 整合 JDBC 用户管理系统
**技术栈**：Spring + JdbcTemplate + MySQL

**实现**：
- JdbcTemplate 配置
- DAO 层封装
- 用户 CRUD
- 事务控制

**产出**：
- Spring JDBC 基础项目

**耗时**：2天

---

## 四、SpringMVC 必做项目

### 5. SpringMVC 登录注册系统
**技术栈**：SpringMVC + MySQL

**实现**：
- 请求映射
- 参数绑定
- JSON 序列化
- @ResponseBody
- Session 管理

**产出**：
- MVC 基础能力

**耗时**：2~3天

---

### 6. 全局异常处理 + 统一返回体
**技术栈**：SpringMVC

**实现**：
- @RestControllerAdvice
- 自定义异常
- 统一响应格式
- 异常分类处理

**产出**：
- 统一异常处理方案

**耗时**：1~2天

---

### 7. 拦截器实现登录鉴权
**技术栈**：SpringMVC

**实现**：
- HandlerInterceptor
- Token 校验
- 未登录拦截
- 权限控制

**产出**：
- 登录鉴权拦截器

**耗时**：1~2天

---

### 8. 文件上传与下载
**技术栈**：SpringMVC

**实现**：
- MultipartFile
- 文件校验
- 文件存储
- 文件下载
- 断点续传基础

**产出**：
- 文件处理能力

**耗时**：2天

---

## 五、SpringBoot 入门必做

### 9. SpringBoot 个人博客后端
**技术栈**：SpringBoot + MyBatis-Plus + MySQL

**实现**：
- 文章 CRUD
- 分页查询
- 评论功能
- 标签管理
- 分类管理
- 用户权限

**产出**：
- 完整博客后端

**耗时**：5~7天

---

### 10. 前后端分离后台管理系统
**技术栈**：SpringBoot + Vue + JWT

**实现**：
- RBAC 权限模型
- 菜单管理
- 用户角色管理
- JWT 登录认证
- 动态路由

**产出**：
- 企业级后台管理系统

**耗时**：7~10天

---

### 11. 接口文档自动化
**技术栈**：SpringBoot + Knife4j / Swagger

**实现**：
- 接口注解
- 参数说明
- 在线调试
- 文档导出

**产出**：
- 自动化接口文档

**耗时**：1天

---

### 12. 定时任务调度系统
**技术栈**：SpringBoot

**实现**：
- @Scheduled
- Cron 表达式
- 动态任务配置
- 任务监控

**产出**：
- 定时任务管理能力

**耗时**：2天

---

## 六、Spring 全家桶整合项目

### 13. SSM 校园二手交易平台
**技术栈**：Spring + SpringMVC + MyBatis

**实现**：
- 商品发布
- 商品搜索
- 订单管理
- 多表联查
- 动态 SQL
- 事务控制
- 文件上传

**产出**：
- SSM 完整项目

**耗时**：7~10天

---

### 14. SpringBoot + Redis 缓存实战
**技术栈**：SpringBoot + Redis + MySQL

**实现**：
- 商品详情缓存
- 首页热点数据
- 缓存穿透 / 击穿 / 雪崩
- 分布式锁
- 热点 Key 优化

**产出**：
- 企业级缓存方案

**耗时**：4~5天

---

### 15. SpringBoot + RabbitMQ 异步消息系统
**技术栈**：SpringBoot + RabbitMQ + MySQL

**实现**：
- 订单异步通知
- 邮件异步发送
- 日志异步落库
- 交换机 / 队列配置
- 死信队列
- 消息可靠性

**产出**：
- 异步消息处理能力

**耗时**：4~5天

---

### 16. SpringBoot + Elasticsearch 搜索引擎
**技术栈**：SpringBoot + Elasticsearch

**实现**：
- 商品搜索
- 文章全文检索
- 分词配置
- 高亮显示
- 分页查询
- 聚合统计

**产出**：
- 全文搜索能力

**耗时**：4~5天

---

## 七、企业级高并发实战项目

### 17. SpringBoot 秒杀系统
**技术栈**：SpringBoot + Redis + RabbitMQ + MySQL

**实现**：
- 库存预扣
- 防超卖
- 接口限流
- 异步下单
- 削峰填谷
- 防刷机制

**产出**：
- 高并发秒杀核心能力

**耗时**：7~10天

---

### 18. 分布式会话共享系统
**技术栈**：SpringBoot + Redis

**实现**：
- Redis 存储 Session
- 集群登录
- 单点登录基础
- Session 自动刷新

**产出**：
- 分布式会话管理能力

**耗时**：2~3天

---

### 19. 多级缓存商品详情系统
**技术栈**：SpringBoot + Caffeine + Redis + MySQL

**实现**：
- Caffeine 本地缓存
- Redis 分布式缓存
- MySQL 数据库
- 缓存更新策略
- 缓存一致性

**产出**：
- 多级缓存架构能力

**耗时**：3~4天

---

### 20. 分布式日志追踪系统
**技术栈**：SpringBoot + MDC

**实现**：
- MDC 链路追踪
- 全局日志格式
- 异常告警
- 日志收集

**产出**：
- 日志追踪能力

**耗时**：2~3天

---

## 八、SpringCloud 微服务项目

### 21. 微服务电商平台
**技术栈**：SpringCloud Alibaba（Nacos + Gateway + Feign + Sentinel）

**实现**：
- 服务注册发现
- 网关路由
- 限流熔断
- 配置中心
- 远程调用

**产出**：
- 微服务架构能力

**耗时**：10~15天

---

### 22. 分布式订单系统
**技术栈**：SpringCloud + Seata

**实现**：
- 分布式事务
- 库存锁定
- 订单超时关闭
- 服务协调

**产出**：
- 分布式事务能力

**耗时**：7~10天

---

### 23. 统一认证授权中心
**技术栈**：SpringBoot + OAuth2.0 + JWT

**实现**：
- OAuth2.0 认证
- JWT 令牌
- 资源服务
- 客户端认证
- 单点登录

**产出**：
- 认证授权能力

**耗时**：5~7天

---

## 九、Spring + AI 大模型融合项目

### 24. SpringBoot + Ollama 本地大模型接口服务
**技术栈**：SpringBoot + Spring AI + Ollama

**实现**：
- 封装大模型调用接口
- 参数控制
- 流式输出
- 对话管理

**产出**：
- 大模型接口服务

**耗时**：3~4天

---

### 25. Java 版 RAG 知识库问答系统
**技术栈**：SpringBoot + Milvus + Ollama + PDF 解析

**实现**：
- 文档解析
- 文档向量化
- 向量检索
- 检索增强生成
- 上下文问答
- 会话管理

**产出**：
- 企业级 RAG 系统

**耗时**：7~10天

---

### 26. AI 智能客服后端
**技术栈**：SpringBoot + Ollama

**实现**：
- 多轮对话
- 意图识别
- 知识库匹配
- 接口限流
- 对话历史

**产出**：
- 智能客服后端

**耗时**：5~7天

---

### 27. 代码生成器后端
**技术栈**：SpringBoot + Ollama

**实现**：
- 调用大模型生成 Java 代码
- 格式化输出
- 语法校验
- 代码优化建议

**产出**：
- AI 代码生成能力

**耗时**：3~4天

---

## 十、Spring 进阶硬核项目

### 28. 手写 SpringBoot 自动配置
**技术栈**：SpringBoot

**实现**：
- SPI 机制
- @EnableAutoConfiguration
- 条件注解
- Starter 封装

**产出**：
- 自动配置原理掌握

**耗时**：3~4天

---

### 29. 基于 Spring 的 RPC 框架
**技术栈**：Spring + Netty

**实现**：
- Netty 通信
- 自定义协议
- 服务注册
- 序列化
- 负载均衡

**产出**：
- RPC 框架开发能力

**耗时**：10~15天

---

### 30. Spring AOP 实现日志埋点与监控
**技术栈**：Spring AOP

**实现**：
- 切面采集请求耗时
- 异常信息记录
- 监控告警
- 统计分析

**产出**：
- AOP 监控能力

**耗时**：2~3天

---

## 十一、学习优先级建议

### 第一阶段：Spring 基础
1. 手写 IOC 容器
2. 手写 AOP 框架
3. SpringBoot 博客后端

### 第二阶段：企业级能力
4. Redis 缓存实战
5. RabbitMQ 异步消息
6. 秒杀系统

### 第三阶段：差异化优势
7. SpringBoot + Ollama 大模型服务
8. RAG 知识库问答系统

### 第四阶段：微服务（有余力）
9. SpringCloud 微服务电商平台

---

## 十二、项目落地标准

必须做到：

- 代码规范（阿里巴巴 Java 开发手册）
- 统一异常处理
- 统一返回体
- 接口文档齐全
- 上传 GitHub
- 标准 README
- 核心难点：问题 + 方案 + 优化

---

## 十三、简历表达（核心关键词）

- Spring IOC / AOP 底层原理与手写实现
- SpringMVC 请求处理与拦截器
- SpringBoot 自动配置与快速开发
- Redis 缓存三大问题解决方案
- RabbitMQ 异步解耦与死信队列
- 高并发秒杀系统（库存预扣 / 限流 / 削峰）
- SpringCloud 微服务架构（Nacos / Gateway / Sentinel）
- SpringBoot + Ollama RAG 知识库问答系统

---
