# Spring AI 实战项目清单

## 一、核心能力

Spring AI 是 Java 后端接入大模型的官方框架，必须掌握：

- 模型调用与配置
- Prompt 工程化管理
- RAG 检索增强生成
- Function Calling 函数调用
- Agent 智能体开发
- 向量数据库集成
- 服务化部署与监控

---

## 二、技术能力拆解

- 基础层：ChatClient / Prompt / 系统提示词 / 上下文管理
- 模板层：PromptTemplate / StructuredOutputConverter / 参数注入
- RAG 层：文档解析 / 向量化 / 检索增强 / 引用溯源
- 工具层：FunctionCallback / 工具注册 / 参数绑定 / 自动调用
- Agent 层：任务拆解 / 多工具编排 / 记忆管理 / 自主决策
- 工程层：微服务网关 / 限流熔断 / 鉴权 / 可观测性

---

## 三、必做项目（求职优先级）

### 1. Spring AI 基础对话应用
**技术栈**：SpringBoot + Spring AI

**实现**：
- 单轮对话
- 多轮上下文对话
- 参数配置
- 模型切换
- 系统提示词配置
- 超时设置

**核心知识点**：
- Spring AI Starter
- ChatClient
- Prompt 管理
- 上下文管理

**产出**：
- Spring AI 基础能力

**耗时**：2天

---

### 2. Prompt 模板工程化项目
**技术栈**：SpringBoot + Spring AI

**实现**：
- 动态 Prompt 模板
- 变量注入
- 结构化输出
- JSON 格式返回
- Prompt 优化

**核心知识点**：
- PromptTemplate
- StructuredOutputConverter
- 参数化 Prompt 设计

**产出**：
- Prompt 工程化能力

**耗时**：2天

---

### 3. 大模型文本总结 / 翻译工具
**技术栈**：SpringBoot + Spring AI

**实现**：
- 长文本总结
- 中英文互译
- 关键词提取
- 情感分析
- 批量文本处理

**核心知识点**：
- 文本处理 API
- 自定义指令
- 文本预处理

**产出**：
- 大模型文本处理能力

**耗时**：2~3天

---

### 4. Spring AI RAG 知识库问答系统
**技术栈**：SpringBoot + Spring AI + Milvus / Chroma

**实现**：
- 文档上传解析
- 文本向量化
- 向量库存储
- 相似度检索
- 检索增强问答
- 引用溯源
- 幻觉抑制

**核心知识点**：
- Spring AI RAG 框架
- 向量数据库集成
- 文档解析器
- 检索策略
- 文本分块
- 嵌入模型
- RAG 全链路

**产出**：
- 企业级 RAG 系统

**耗时**：7~10天

---

### 5. Spring AI 函数调用业务系统
**技术栈**：SpringBoot + Spring AI

**实现**：
- 大模型调用自定义 Java 接口
- 实时数据查询
- 业务工具调用
- 智能决策
- 工具注册
- 参数绑定
- 自动调用编排

**核心知识点**：
- FunctionCallback
- 工具调用原理
- 业务函数封装
- 权限校验
- 调用链路追踪

**产出**：
- Function Calling 能力

**耗时**：5~7天

---

### 6. Spring AI 自主决策智能体（Agent）
**技术栈**：SpringBoot + Spring AI

**实现**：
- 任务自主拆解
- 多工具链式调用
- 对话记忆持久化
- 复杂问题分步解决
- 短期 / 长期记忆
- 工具链编排
- 任务规划

**核心知识点**：
- Spring AI Agent 框架
- ChatMemory
- 工具链编排
- 任务规划器
- 智能体记忆管理
- 自主决策逻辑

**产出**：
- Agent 智能体能力

**耗时**：7~10天

---

## 四、进阶项目（提升上限）

### 7. 智能文本分类与实体抽取系统
**技术栈**：SpringBoot + Spring AI

**实现**：
- 新闻分类
- 意图识别
- 命名实体抽取
- 关键词提取
- 数据标注辅助

**产出**：
- 结构化输出能力

---

### 8. 多智能体协作系统
**技术栈**：SpringBoot + Spring AI

**实现**：
- 分工式智能体
- 角色定义
- 任务分发
- 结果汇总
- 多智能体对话

**产出**：
- 多 Agent 协作能力

---

### 9. 智能客服对话系统
**技术栈**：SpringBoot + Spring AI + WebSocket

**实现**：
- 多轮对话
- 意图识别
- 知识库问答
- 人工转接
- 对话日志管理

**产出**：
- 智能客服系统

---

### 10. Spring AI 微服务化大模型网关
**技术栈**：SpringCloud Alibaba + Gateway + Sentinel + Spring AI

**实现**：
- 多模型统一接入
- 负载均衡
- 限流熔断
- API 鉴权
- 调用统计

**产出**：
- 大模型服务网关

---

### 11. AI 文档智能处理平台
**技术栈**：SpringBoot + Spring AI + 文件分片上传

**实现**：
- PDF / Word / Markdown 文档解析
- 智能总结
- 问答
- 翻译
- 格式转换

**产出**：
- 文档智能处理能力

---

### 12. 前后端分离 AI 对话平台
**技术栈**：SpringBoot + Spring AI + Vue3 + WebSocket + JWT

**实现**：
- 用户登录
- 会话管理
- 流式对话
- 历史记录
- 知识库管理
- 暗黑模式

**产出**：
- 全栈 AI 对话平台

---

### 13. Spring AI 可观测与监控平台
**技术栈**：SpringBoot + Spring AI + Spring AOP + Prometheus

**实现**：
- 调用日志记录
- Token 统计
- 耗时监控
- 异常告警
- Prompt 优化分析

**产出**：
- 大模型可观测性能力

---

## 五、你的优先级

结合你的 **Java 后端 + AI 大模型应用** 路线，最值得优先完成的是：

1. Spring AI 基础对话应用
2. Prompt 模板工程化项目
3. Spring AI RAG 知识库问答系统
4. Spring AI 函数调用业务系统
5. Spring AI Agent 自主决策智能体
6. 前后端分离 AI 对话平台

---

## 六、练手路线

### 第一阶段：基础能力
先做：
- Spring AI 基础对话应用
- Prompt 模板工程化项目
- 大模型文本总结 / 翻译工具

目标：
- 熟悉 Spring AI 核心 API 与配置

### 第二阶段：核心能力
再做：
- Spring AI RAG 知识库问答系统
- Spring AI 函数调用业务系统

目标：
- 掌握 RAG 全链路与函数调用

### 第三阶段：高阶能力
再做：
- Spring AI Agent 自主决策智能体
- 智能客服对话系统

目标：
- 实现大模型自主调用 Java 业务接口

### 第四阶段：工程化落地
最后做：
- 前后端分离 AI 对话平台
- Spring AI 微服务化大模型网关
- Spring AI 可观测与监控平台

目标：
- 全链路工程化落地

---

## 七、项目架构标准

必须覆盖：

- Spring AI 核心配置
- Prompt 工程化管理
- 向量数据库集成
- 函数调用封装
- Agent 任务编排
- 统一异常处理
- 日志记录
- 性能监控

---

## 八、简历表达（核心关键词）

- Spring AI 大模型调用与配置
- Prompt 模板工程化管理
- RAG 检索增强生成全链路
- Function Calling 函数调用与业务集成
- Agent 智能体任务拆解与工具编排
- 向量数据库（Milvus / Chroma）集成
- 流式对话与 WebSocket 实时通信
- 微服务化大模型网关与限流熔断

---

## 九、技术栈推荐

- 框架：SpringBoot + Spring AI
- 大模型：OpenAI / 通义 / Ollama
- 向量库：Milvus / Chroma / PgVector
- 缓存：Redis
- 数据库：MySQL
- 消息队列：RabbitMQ
- 微服务：SpringCloud Alibaba
- 前端：Vue3 + WebSocket
- 监控：Prometheus + Grafana

---

## 十、适配你的技术栈优势

Spring AI 完全基于 Spring 生态，你无需学习全新框架，直接复用已掌握的：

- SpringBoot / SpringMVC / Spring AOP
- MySQL / Redis / Milvus 向量库
- WebSocket / JWT / 微服务组件

**Java 后端开发 + Spring AI 大模型应用** = 当前互联网、国企、AI 初创公司最稀缺的复合型人才

---

## 十一、项目成果要求

- GitHub 完整代码（结构清晰）
- README 写明：项目背景、技术栈、功能模块、部署步骤、核心难点
- 项目必须体现：Prompt 工程化、RAG 全链路、函数调用、Agent 编排、工程化部署
- 至少 1 个完整业务 Demo
- 能清晰展示 Spring AI 与 Java 后端的深度整合
- 项目亮点突出差异化竞争力

---
