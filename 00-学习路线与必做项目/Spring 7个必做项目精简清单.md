# Spring 七个必做项目精简清单

## 一、核心能力

Spring 是 Java 后端开发核心框架，必须掌握：

- IOC 容器与依赖注入
- AOP 面向切面编程
- SpringMVC 请求处理
- SpringBoot 自动配置
- 中间件整合（Redis / RabbitMQ / MySQL）
- 高并发场景解决方案
- AI 大模型应用整合

---

## 二、技术能力拆解

- 原理层：IOC / AOP / Bean 生命周期 / 动态代理
- Web 层：请求映射 / 参数绑定 / 拦截器 / 异常处理
- 持久层：MyBatis / MyBatis-Plus / 事务管理
- 缓存层：Redis 三大问题 / 分布式锁 / 缓存更新
- 消息层：RabbitMQ 交换机 / 死信队列 / 异步解耦
- 并发层：限流 / 削峰 / 防超卖 / 库存扣减
- AI 层：大模型封装 / RAG 检索 / 向量化 / 流式输出

---

## 三、必做项目（按顺序执行）

### 1. Spring 原生 IOC & AOP 手写实战
**定位**：吃透 Spring 底层原理，面试核心考点

**技术栈**：Java SE + 反射 + 注解

**实现**：
- IOC 容器：
  - Bean 定义与注册
  - 依赖注入
  - 单例池
  - Bean 生命周期
- AOP：
  - JDK 动态代理
  - CGLIB 动态代理
  - 切面 / 切点 / 通知

**核心知识点**：
- 反射机制
- 注解解析
- 动态代理
- 单例模式

**产出**：
- 极简版 Spring 容器

**耗时**：3~5天

---

### 2. SpringMVC 前后端分离基础项目
**定位**：掌握 Web 请求核心流程，夯实 MVC 架构

**技术栈**：SpringMVC + MySQL + Maven

**实现**：
- 请求映射
- 参数绑定
- JSON 序列化
- 全局异常处理
- 统一返回体
- 拦截器登录鉴权
- 文件上传

**核心知识点**：
- DispatcherServlet
- HandlerMapping
- ViewResolver
- 拦截器

**产出**：
- 用户管理系统（登录、注册、CRUD、权限拦截）

**耗时**：4~6天

---

### 3. SpringBoot 个人博客后端
**定位**：入门 SpringBoot，掌握现代后端开发范式

**技术栈**：SpringBoot + MyBatis-Plus + MySQL + Knife4j

**实现**：
- 文章 CRUD
- 评论功能
- 标签管理
- 分类管理
- 用户权限
- 分页查询
- 接口文档

**核心知识点**：
- 自动配置
- 约定大于配置
- ORM 持久层
- 分页 / 条件查询
- 全局异常 / 跨域处理

**产出**：
- 完整博客后端

**耗时**：5~7天

---

### 4. SpringBoot + Redis 缓存实战项目
**定位**：企业级缓存能力，解决性能问题

**技术栈**：SpringBoot + Redis + MySQL

**实现**：
- 商品详情缓存
- 缓存穿透：布隆过滤器 / 缓存空值
- 缓存击穿：互斥锁 / 逻辑过期
- 缓存雪崩：随机过期时间
- 分布式锁防超卖
- 热点数据缓存

**核心知识点**：
- 缓存三大问题
- Cache Aside 更新策略
- 分布式锁
- 热点数据处理

**产出**：
- 商品详情页系统（缓存优化、库存防超卖）

**耗时**：4~5天

---

### 5. SpringBoot + RabbitMQ 异步消息项目
**定位**：掌握异步解耦，对接企业业务场景

**技术栈**：SpringBoot + RabbitMQ + MySQL

**实现**：
- 订单下单后发送 MQ
- 异步短信通知
- 异步邮件通知
- 死信队列处理失败消息
- 延迟队列实现订单超时取消

**核心知识点**：
- Fanout / Direct / Topic 交换机
- 死信队列
- 消息可靠性
- 异步解耦

**产出**：
- 订单异步通知系统

**耗时**：4~5天

---

### 6. SpringBoot 高并发秒杀系统
**定位**：校招 / 实习简历加分核心，解决高并发场景

**技术栈**：SpringBoot + Redis + RabbitMQ + MySQL

**实现**：
- 商品缓存预热
- Redis 库存预扣
- 分布式锁防超卖
- 接口限流防刷
- 异步下单
- RabbitMQ 削峰
- 订单异步处理

**核心知识点**：
- 库存预扣
- 限流防刷
- 分布式锁
- 异步削峰
- 事务控制

**产出**：
- 完整秒杀业务

**耗时**：7~10天

---

### 7. SpringBoot + Ollama 本地大模型 RAG 问答系统
**定位**：差异化竞争力，贴合 Java + AI 职业规划

**技术栈**：SpringBoot + Spring AI + Ollama + Milvus + PDF 解析

**实现**：
- PDF 文档上传
- 文档解析与分块
- 向量化与存储
- 相似度检索
- 大模型流式输出
- 上下文对话
- 答案溯源

**核心知识点**：
- 大模型接口封装
- RAG 全链路
- 向量化与检索
- 流式响应
- 多轮对话

**产出**：
- 私有化知识库问答系统

**耗时**：7~10天

---

## 四、配套执行标准

### 1. 代码规范
- 严格遵循阿里巴巴 Java 开发手册
- 代码分层：Controller / Service / Dao / Entity
- 统一返回体
- 全局异常处理
- 参数校验

### 2. 项目管理
- 所有项目上传 GitHub
- 仓库名规范
- 提交记录清晰
- 标准 README.md

### 3. README 内容
- 项目背景
- 技术栈
- 核心功能
- 部署步骤
- 核心难点
- 接口文档

---

## 五、简历表达（核心关键词）

- Spring IOC / AOP 底层原理
- SpringMVC 请求处理与拦截器
- SpringBoot 自动配置与工程化开发
- Redis 缓存三大问题解决方案
- RabbitMQ 异步解耦与死信队列
- 高并发秒杀系统（库存预扣 / 限流 / 削峰）
- SpringBoot + Ollama RAG 知识库问答系统

---

## 六、技术栈总览

| 项目 | 核心技术 |
|------|---------|
| 1. IOC & AOP 手写 | Java SE / 反射 / 注解 / 动态代理 |
| 2. SpringMVC | SpringMVC / MySQL / 拦截器 |
| 3. 博客后端 | SpringBoot / MyBatis-Plus / Knife4j |
| 4. Redis 缓存 | SpringBoot / Redis / MySQL |
| 5. RabbitMQ 消息 | SpringBoot / RabbitMQ / MySQL |
| 6. 秒杀系统 | SpringBoot / Redis / RabbitMQ / MySQL |
| 7. RAG 问答 | SpringBoot / Ollama / Milvus / Spring AI |

---

## 七、练手路线

### 第一阶段：原理与基础
- 项目 1：Spring IOC & AOP 手写
- 项目 2：SpringMVC 基础项目

### 第二阶段：现代开发范式
- 项目 3：SpringBoot 博客后端

### 第三阶段：中间件整合
- 项目 4：Redis 缓存实战
- 项目 5：RabbitMQ 异步消息

### 第四阶段：高并发与 AI 融合
- 项目 6：秒杀系统
- 项目 7：RAG 问答系统

---

## 八、项目成果要求

- GitHub 完整代码（结构清晰）
- README 完整（技术栈 / 功能 / 部署 / 难点）
- 代码分层规范
- 统一返回体与异常处理
- 至少 1 个可运行 Demo
- 能清晰展示核心技术点与业务价值

---