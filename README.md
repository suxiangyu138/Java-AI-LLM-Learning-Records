# Java后端开发 + AI大模型应用开发学习笔记

> 面向 **Java后端开发工程师** 与 **AI大模型应用开发工程师** 的系统化学习仓库，聚焦核心知识、实战项目、源码沉淀、面试准备与工程化能力建设。

## 仓库定位

这个仓库用于沉淀 Java 后端开发与 AI 大模型应用开发的学习笔记、项目实践、问题复盘和面试总结，强调“知识体系化 + 项目实战化 + 输出工程化”。

适合以下目标：

- 系统梳理 Java 后端核心知识体系
- 进阶 Spring 生态、数据库、中间件、分布式与性能优化
- 衔接 AI 大模型应用开发能力，构建 RAG、Agent、工作流、提示工程等项目经验
- 将学习内容沉淀为可复用的 Markdown 文档与可展示的 GitHub 仓库
- 服务于实习、校招、社招简历项目包装与技术面试准备

## 学习路线

### 1. Java后端基础

- JavaSE
- 集合
- 异常
- 泛型
- IO
- 反射
- 并发编程
- JVM

### 2. JavaWeb与主流框架

- HTTP / HTTPS
- Servlet / Filter / Listener
- Maven / Gradle
- Spring
- SpringMVC
- SpringBoot
- MyBatis / MyBatis-Plus
- Spring Security

### 3. 数据库与中间件

- MySQL
- Redis
- 消息队列
- Elasticsearch
- Nginx
- Docker

### 4. 分布式与高并发

- 微服务基础
- Spring Cloud Alibaba
- 注册中心 / 配置中心
- 分布式事务
- 限流 / 熔断 / 降级
- 缓存设计
- 性能调优

### 5. AI大模型应用开发

- 大模型基础概念
- Prompt Engineering
- Embedding
- 向量数据库
- RAG
- Function Calling
- Agent
- 工作流编排
- 本地模型部署（Ollama）
- 模型应用后端集成

### 6. 全栈协同能力

- Vue / React 基础对接
- 前后端分离项目开发
- RESTful API 设计
- 接口调试与文档管理

## 仓库结构建议

```text
.
├─ README.md
├─ docs
│  ├─ 01-JavaSE
│  ├─ 02-JavaWeb
│  ├─ 03-Spring
│  ├─ 04-SpringBoot
│  ├─ 05-MySQL
│  ├─ 06-Redis
│  ├─ 07-JVM
│  ├─ 08-Concurrent
│  ├─ 09-MQ
│  ├─ 10-Microservice
│  ├─ 11-Docker
│  ├─ 12-LLM-Basics
│  ├─ 13-Prompt
│  ├─ 14-RAG
│  ├─ 15-Agent
│  ├─ 16-Ollama
│  ├─ 17-Project-Notes
│  └─ 18-Interview
├─ projects
│  ├─ admin-system
│  ├─ file-service
│  ├─ rag-chatbot
│  ├─ agent-workflow
│  └─ ai-assistant-backend
├─ images
├─ resources
└─ templates
```

## 笔记规范

### 文件命名


```text
序号-主题.md
```


```text
01-Java集合详解.md
02-SpringBoot自动配置原理.md
03-Redis缓存穿透雪崩击穿.md
04-RAG完整流程.md
```

### 单篇笔记模板

```md
# 标题

## 1. 核心概念

## 2. 原理分析

## 3. 常见面试题

## 4. 实战应用

## 5. 易错点

## 6. 总结
```

### 输出要求

- 以 Markdown 为主，保证可直接用于 GitHub 展示
- 重点内容尽量配图、流程图、表格
- 每篇笔记至少回答“是什么、为什么、怎么用、有哪些坑”
- 理论笔记和项目实战笔记分开整理
- 每学完一个专题输出一份总结文档

## 推荐实战项目

### Java后端项目

- 后台管理系统：用户、角色、权限、JWT、RBAC、日志审计
- 文件上传下载系统：本地存储 / OSS、断点续传、文件校验
- 秒杀系统：Redis + MQ + 限流 + 异步削峰
- 订单系统：分库分表、分布式事务、幂等控制

### AI大模型项目

- RAG知识库问答系统
- 企业文档智能助手
- 基于 Ollama 的本地 AI 助手
- 多 Agent 协作任务系统
- Java + Python 混合架构的大模型应用平台

## 面试导向整理

- 八股文高频题
- 项目亮点拆解
- 场景题复盘
- 性能优化案例
- 故障排查案例
- AI 应用项目架构设计题

## 致自己

保持长期主义，拒绝零散学习。

把每一篇笔记都写成未来面试、简历、项目复盘时可以直接复用的资产。
