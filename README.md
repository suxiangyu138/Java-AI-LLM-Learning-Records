# Java 后端 + AI 大模型 系统化学习仓库

> 面向 **Java 后端开发工程师** 与 **AI 大模型应用开发工程师** 的全栈知识体系。
> **2,370+ 篇标准化 Markdown 技术文档**，按 6 层金字塔组织，覆盖从底层基础到面试冲刺的完整学习路径。

---

## 📐 六层金字塔知识体系

```
Java-AI-LLM-Learning-Records/  (2,370+ .md 文件)
│
├── 🏗️ 01-底层根基-Java核心底座/       669 篇  地基：Java核心+JUC+JVM+DSA+计算机基础+工具(Vim/文件后缀)
│
├── 🔧 02-后端中间件&微服务工程/        604 篇  骨架：MySQL+Redis+MQ+ES+Spring全家桶+DevOps+知识体系搭建
│
├── 🤖 03-AI大模型应用开发/            696 篇  差异：DeepSeek+RAG+Agent+Harness+Vibe Coding+Python虚拟环境
│
├── 🎯 04-实战项目综合落地/             45 篇  证明：FlavorDash+SuGuangMall+LingShu 三大项目深度复盘
│
├── 📝 05-综合输出-面试冲刺/            185 篇  输出：152题题库+手写代码+简历模板+速查手册+能力图谱
│
└── 📚 06-课程思路体系搭建/            176 篇  索引：84+门B站课程标准化大纲，五大分类完整索引
```

---

## 📂 各层详解

### 🏗️ 01-底层根基 — Java 核心底座（669 篇）

| 子模块 | 内容 |
|--------|------|
| Java 基础语法与核心特性 | 数据类型、泛型、集合源码、IO/NIO、异常/日志 |
| JUC 高并发编程 | volatile、synchronized、AQS、线程池、CompletableFuture |
| JVM 完整底层 | 类加载、运行时数据区、GC(CMS/G1/ZGC)、调优工具 |
| 设计模式 | GoF 23种 + 六大原则 + DDD |
| 计算机基础 | OS、网络、组成原理、分布式理论、计算机安全 |
| 数据结构与算法 | LeetCode Hot100、算法导论、排序、图论 |
| 高并发与性能优化 | 高并发设计、池化技术、异步编程 |
| **🆕 Vim** | 基础编辑 → 进阶文本对象/宏/寄存器 → 插件与 IDE 化 |
| **🆕 不同文件的后缀名** | 文本/源码/配置/归档/系统/多媒体格式全解析 |

### 🔧 02-后端中间件 & 微服务工程（604 篇）

| 子模块 | 内容 |
|--------|------|
| MySQL 与 MyBatis 持久层 | InnoDB、B+树、MVCC、SQL 调优、MyBatis/MyBatis-Plus |
| Redis 缓存层 | 五大结构、持久化、缓存三问题、分布式锁、Lua |
| RabbitMQ/RocketMQ/Kafka | 消息可靠性、死信/延迟队列、事务消息 |
| Elasticsearch | 倒排索引、DSL 查询、聚合、深度分页 |
| Spring 全家桶与微服务 | IoC/AOP、SpringBoot、SpringMVC、Spring Cloud Alibaba |
| 工程运维 | Docker、Nginx、Linux、Git、Maven、K8s、监控 |
| 分布式系统与架构 | CAP/BASE、分布式事务/锁/ID、服务治理 |
| **🆕 Java 后端名词剖析** | 10 大模块 300+ 名词完整体系 |
| **🆕 Postman** | API 测试全流程：请求→脚本→Runner→Newman CLI |
| **🆕 JMeter** | 性能测试：Sampler→断言→分布式→CI/CD 集成 |
| **🆕 Ubuntu** | 命令行→包管理→权限→Java 环境→systemd 部署→Shell 脚本 |
| **🆕 CentOS** | dnf/rpm→SELinux 排障→firewalld 防火墙→生产部署实战 |
| **🆕 项目全流程** | 需求→方案→编码→测试→CI/CD→上线→运维监控 |

### 🤖 03-AI 大模型应用开发（696 篇）

| 子模块 | 内容 |
|--------|------|
| **🆕 Agent 开发** | 12 篇完整体系：P-A-M-E 架构→设计模式(ReAct/H-V-R)→框架选型→记忆/工具/MCP→多Agent→评估/安全/生产→实战案例 |
| **🆕 Vibe Coding** | 10 篇完整体系：范式革命→工具选型→五步工作流→提示词与上下文→工程规范→多Agent协作→风险与趋势 |
| **🆕 Harness Engineering** | 8 篇完整体系：Agent=Model+Harness→运行管控体系→安全纵深防御→治理合规→测试评估→企业落地 |
| **🆕 AI时代职业发展** | 9 篇双主线：AI 赋能产业全景+行业模式，程序员冲击数据+转型路线+新岗位+个人策略 |
| **🆕 Python 虚拟环境** | 7 篇完整体系：PEP 405 原理→venv/conda/uv 选型→锁文件→CI/Docker→迁移路径 |
| **🆕 DeepSeek (V4 Pro)** | 10 篇完整体系：概览→MoE+MLA→V3训练→R1推理→Coder→多模态→API→生态对比→V4/V4-Pro |
| 大模型基础与 Prompt 工程 | Token/上下文、CoT、Few-shot、Function Calling、NL2SQL |
| RAG 检索增强生成 | 数据预处理→切片→Embedding→Milvus→多路召回→重排序 |
| Multi-Agent 与 MCP 协议 | ReAct 循环、Planner/Skill/Memory、YAML 热加载、MCP |
| 向量数据库与 Embedding | Milvus、Chroma、FAISS、Embedding 模型选型 |
| 模型部署与工程化 | LLM API、Ollama 本地部署、容灾降级、Sentinel 限流 |
| 开发框架 | LangChain4j、Spring AI、LlamaIndex |
| Python 与数据处理 | Python 基础、爬虫、NumPy/Pandas 数据分析、Python 虚拟环境 |
| 模型微调与多模态 | LoRA/QLoRA、PEFT、多模态大模型 |
| AI 开发工具与面试 | Claude Code、Cursor、AI Coding、AI 面试指南 |

### 🎯 04-实战项目综合落地（45 篇）

| 项目 | 技术栈 | 亮点 |
|------|--------|------|
| FlavorDash AI 外卖平台 | SpringBoot + AI 六大模块 + 多模型网关 + SSE 流式 | 单体架构 + AI 深度集成 |
| SuGuangMall AI 微服务电商 | SpringCloud Alibaba + Multi-Agent + 秒杀零超卖 | 微服务架构 + AI Agent |
| LingShu 医疗 AI 问答平台 | 14步RAG管线 + ReAct Agent + 多层安全护栏 | 医疗垂直场景 |

### 📝 05-综合输出 — 面试冲刺（185 篇）

| 子模块 | 内容 |
|--------|------|
| 面试题库 | 152 题全量题库（含标准答案 + PDF 版本） |
| 手写代码题库 | HashMap/线程池/分布式锁/SSE/RAG/Agent 手写 |
| 简历与职业发展 | 多版本简历 + 个人发展笔记 + 职业规划 |
| 参考速查 | 核心知识点速查手册 + AI/数学名词图谱 |
| 学习路线与能力图谱 | Java 后端分级能力图谱 + AI 大模型学习路线 |

### 📚 06-课程思路体系搭建（176 篇）

| 分类 | 课程数 | 覆盖 |
|------|:-----:|------|
| Java 后端体系 | 26 门 | JavaWeb→JDBC→MyBatis→Spring→SpringBoot→SpringCloud→JVM→并发 |
| 中间件 | 16 门 | Redis→RabbitMQ→RocketMQ→ES→Nginx→MongoDB→ZK→Dubbo→Netty |
| AI 大模型 | 23 门 | LLM→Prompt→RAG→Agent→LangChain→SpringAI→MCP→Milvus→Ollama |
| 工具与运维 | 12 门 | Git→Maven→Linux→Docker→Jenkins→ClaudeCode→Cursor |
| 前端与语言 | 11 门 | Vue3→React→微信小程序→Python→FastAPI→Node.js |

---

## 🆕 本次更新（2026年8月）

```text
新增 46 篇高质量技术文档，覆盖 5 个新知识体系（AI 前沿方向）：

  03-AI大模型应用开发/
    ├── Agent开发               12篇   P-A-M-E架构→设计模式→框架→记忆/工具/MCP→多Agent→评估/安全/生产→实战
    ├── Vibe Coding             10篇   范式革命→工具选型→五步工作流→上下文工程→工程规范→风险与趋势
    ├── AI时代如何用AI赋能各行各业  9篇   AI赋能产业全景+程序员冲击/转型/新岗位+个人策略+社会影响
    ├── Harness Engineering      8篇   Agent=Model+Harness→七层管控→安全纵深→治理合规→评估→落地
    └── Python虚拟环境            7篇   PEP 405原理→venv/conda/uv选型→锁文件→CI/Docker→迁移

结构性调整：Spring 全家桶文档整理归入 06-Spring全家桶/Spring全家桶/ 子目录；
新增 ActiveMQ、PostgreSQL、SQL Server 中间件知识目录。
```

---

## 📋 文档规范

所有 `.md` 文件遵循统一规范：

```md
# 标题
> 一句话定位

## 📚 目录
1. [锚点链接](#section)

## 1. 章节
### 1.1 小节
| 表格 | 对比 |
|------|------|
```

- **Table 优先**：对比、API、特性列表
- **Code Block 标注语言**：\```java, \```yaml, \```bash
- **Blockquote 提示**：`> ⚠️` 警告 `> 💡` 技巧 `> 🎯` 总结

---

## ⚡ 快速开始

### 学习路线

| 目标 | 路径 |
|------|------|
| **Java 后端面试** | 01-Java核心 → 01-JUC → 01-JVM → 02-MySQL → 02-Redis → 02-Spring → 05-题库 |
| **AI 大模型开发** | 03-大模型基础 → 03-RAG → 03-Agent → 03-LangChain4j → 03-DeepSeek |
| **全栈架构师** | 01→02→03→04→05→06 全路径 |

### 仓库导航

- [底层根基](./01-底层根基-Java核心底座/)
- [中间件与微服务](./02-后端中间件%20微服务工程及相关拓展/)
- [AI 大模型](./03-AI大模型应用开发/)
- [实战项目](./04-实战项目综合落地/)
- [面试冲刺](./05-综合输出-面试冲刺/)
- [课程索引](./06-课程思路体系搭建/)

---

*最后更新：2026年8月 | 2,370+ 篇 Markdown 文档*
