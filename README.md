# Java 后端 + AI 大模型 系统化学习仓库

> 面向 **Java 后端开发工程师** 与 **AI 大模型应用开发工程师** 的全栈知识体系。
> **2,664 篇标准化 Markdown 技术文档**，按 5 层金字塔组织，覆盖从底层基础到面试冲刺的完整学习路径。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📐 五层金字塔知识体系

```
Java-AI-LLM-Learning-Records/  (2,664 篇 .md 文件)
│
├── 🏗️ 01-底层根基-Java核心底座/        823 篇  地基：Java核心+JUC+JVM+DSA+计算机基础+工具(Vim/文件后缀/SDK)
│
├── 🔧 02-后端中间件&微服务工程/         639 篇  骨架：MySQL+Redis+MQ+ES+Spring全家桶+MyBatisPlus+DevOps
│
├── 🤖 03-AI大模型应用开发/             735 篇  差异：DeepSeek+RAG+Agent+MCP+Milvus+部署工程化+Python
│
├── 🎯 04-项目能力综合提升/             230 篇  证明：FlavorDash+SuGuangMall+LingShu 项目 + 84门课程索引
│
└── 📝 05-综合输出-面试冲刺/            232 篇  输出：152题题库+手写代码+简历模板+速查手册+能力图谱
```

---

## 📂 各层详解

### 🏗️ 01-底层根基 — Java 核心底座（823 篇）

| 子模块 | 内容 |
|--------|------|
| Java 基础语法与核心特性 | 数据类型、泛型、集合、IO/NIO、异常/日志 |
| **🆕 Java 面向对象** | 11 篇：类与对象→封装→继承→多态→抽象类接口→构造器→内部类→record/sealed/模式匹配→SOLID→面试 |
| **🆕 Java 集合框架** | 10 篇：Collection 体系→List/Set/Queue/Map→并发集合→比较器→不可变集合→面试 |
| **🆕 Java 异常体系** | 10 篇：Throwable 层级→受检/非受检→try-catch-finally→TWR→传播策略→错误码→并发异常→反模式 |
| **🆕 SDK** | 8 篇：API/SDK/Framework 辨析→JDK 解剖→JPMS/jlink→第三方集成→自研设计→SDKMAN |
| **🆕 Java 多线程** | 9 篇：进程线程本质→Thread API→线程安全三要素→synchronized/volatile→协作→死锁→性能 |
| JUC 高并发编程 | volatile、synchronized、AQS、线程池、CompletableFuture、虚拟线程 |
| JVM 完整底层 | 类加载、运行时数据区、GC(CMS/G1/ZGC)、调优工具 |
| 设计模式 | GoF 23种 + 六大原则 + DDD |
| 计算机基础 | OS、网络、组成原理、分布式理论、计算机安全 |
| 数据结构与算法 | LeetCode Hot100、算法导论、排序、图论 |
| 高并发与性能优化 | 高并发设计、池化技术、异步编程 |
| 工具 | Vim、文件后缀名、SSH、JSON、快捷键、命令行 |

### 🔧 02-后端中间件 & 微服务工程（639 篇）

| 子模块 | 内容 |
|--------|------|
| MySQL 与持久层 | InnoDB、B+树、MVCC、SQL 调优、MyBatis |
| **🆕 MyBatisPlus** | 10 篇：定位→实体映射→BaseMapper→Wrapper→IService→插件分页→代码生成→生产实践→面试 |
| Redis 缓存层 | 五大结构、持久化、缓存三问题、分布式锁、Lua |
| RabbitMQ/RocketMQ/Kafka | 消息可靠性、死信/延迟队列、事务消息 |
| Elasticsearch | 倒排索引、DSL 查询、聚合、深度分页 |
| Spring 全家桶与微服务 | IoC/AOP、SpringBoot、SpringMVC、Spring Cloud Alibaba |
| **🆕 Spring 框架核心** | 11 篇：容器体系→BeanDefinition→依赖注入→生命周期→AOP→事务→事件→配置→测试→面试（Spring 7.0/Boot 4） |
| 工程运维 | Docker、Nginx、Linux、Git、Maven、K8s、监控 |
| 分布式系统与架构 | CAP/BASE、分布式事务/锁/ID、服务治理 |
| 名词剖析 | 10 大模块 300+ 名词完整体系 |
| 工具链 | Postman、JMeter、Ubuntu、CentOS、项目全流程 |

### 🤖 03-AI 大模型应用开发（735 篇）

| 子模块 | 内容 |
|--------|------|
| 大模型基础与 Prompt 工程 | Token/上下文、CoT、Few-shot、Function Calling、NL2SQL |
| **🆕 深度学习** | 11 篇：概述→CNN→RNN/LSTM→GAN→迁移学习→强化学习→模型压缩→训练技巧→框架（LLM 视角） |
| **🆕 RAG 拓展优化深化** | 11 篇（12-22）：五代演进→查询优化→重排压缩→GraphRAG→Agentic RAG→长上下文之争→多模态→评估深化→成本 |
| **🆕 Multi-Agent 与 MCP 深化** | 5 篇（12-16）：无状态 MCP 规范→A2A 协议→安全攻防→评估可观测性→编程 Agent/端侧 |
| **🆕 Milvus** | 10 篇：定位版本（3.0）→数据模型→部署→Schema→索引→混合检索→生态集成→生产实践→面试 |
| **🆕 向量数据库** | 10 篇增强：索引原理→Chroma/Milvus/FAISS/Pinecone→性能→选型（2026 双轨格局） |
| **🆕 部署工具** | 12 篇增强：Docker/K8s→vLLM/Ollama/TensorRT/ONNX→服务化→MLOps→监控→灰度（2026 引擎格局） |
| **🆕 模型推理与部署** | 11 篇增强：KV Cache→投机采样→批处理→量化→压测→引擎选型→弹性→成本（推理原理） |
| RAG 检索增强生成 | 数据预处理→切片→Embedding→Milvus→多路召回→重排序（基础 11 篇） |
| Multi-Agent 与 MCP 协议 | ReAct 循环、Planner/Skill/Memory、MCP（基础 11 篇） |
| 模型部署与工程化 | LLM API、Ollama 本地部署、容灾降级、Sentinel 限流 |
| 开发框架 | LangChain4j、Spring AI、LlamaIndex |
| 前沿体系 | Agent 开发、Vibe Coding、Harness Engineering、DeepSeek、Python 虚拟环境、AI 职业发展 |

### 🎯 04-项目能力综合提升（230 篇）

| 分类 | 内容 |
|------|------|
| 实战项目（45 篇） | FlavorDash AI 外卖（SpringBoot+AI 六大模块）、SuGuangMall AI 微服务电商、LingShu 医疗 AI 问答 |
| 课程索引（176 篇） | 84+ 门 B 站课程标准化大纲：Java 后端 26 门、中间件 16 门、AI 大模型 23 门、工具运维 12 门、前端语言 11 门 |
| 项目场景题 | 高频项目场景与架构设计题 |

### 📝 05-综合输出 — 面试冲刺（232 篇）

| 子模块 | 内容 |
|--------|------|
| 面试题库 | 152 题全量题库（含标准答案）+ Java/AI 双题库 |
| 手写代码题库 | HashMap/线程池/分布式锁/SSE/RAG/Agent 手写 |
| 简历与职业发展 | 多版本简历 + 职业规划 + 面试软实力 |
| 参考速查 | 核心知识点速查手册 + AI/数学名词图谱 |
| 学习路线与能力图谱 | Java 后端分级能力图谱 + AI 大模型学习路线 |

---

## 📌 活跃知识体系（2026-08 最新）

| 系统 | 位置 | 篇数 | 亮点 |
|------|------|:---:|------|
| Java 面向对象 | 01-.../Java面向对象/ | 11 | record/sealed/模式匹配 + SOLID |
| Java 集合框架 | 01-.../Java集合框架/ | 10 | 复杂度总表 + 并发容器 + 选型决策 |
| Java 异常体系 | 01-.../Java异常体系/ | 10 | 受检之争（2025）+ FailedException（JDK 25） |
| Java 多线程 | 01-.../Java多线程/ | 9 | 偏向锁移除（JDK 18）+ pinning 修复（JDK 24） |
| SDK | 01-.../SDK/ | 8 | JDK 解剖 + jlink + 第三方集成 + 自研设计 |
| Spring 框架核心 | 02-.../Spring框架核心/ | 11 | Spring 7.0/Boot 4 + CGLIB 一致默认 |
| MyBatisPlus | 02-.../MyBatisPlus/ | 10 | 3.5.17 + Boot4 starter + 逻辑删除×唯一索引 |
| RAG 拓展优化深化 | 03-.../02-RAG检索增强生成/（12-22） | 11 | 五代演进 + GraphRAG + 长上下文之争 + Contextual Retrieval |
| Multi-Agent/MCP 深化 | 03-.../03-Multi-Agent与MCP协议/（12-16） | 5 | 无状态 MCP（2026-07）+ A2A + 工具投毒防御 |
| Milvus | 03-.../Milvus/ | 10 | 3.0 Lake-Native + 混合检索 + 多模态 |
| 向量数据库 | 03-.../04-向量数据库/ | 10 | 2026 双轨格局 + DiskANN 亿级标配 |
| 部署工具 | 03-.../部署工具/ | 12 | 2026 引擎格局（vLLM 默认/TGI 退役） |
| 模型推理与部署 | 03-.../模型推理与部署/ | 11 | KV Cache + 投机采样 + FP8 + 弹性 |
| 深度学习 | 03-.../深度学习/ | 11 | LLM 视角对照表 + 2026 框架格局 |

---

## 🆕 本次更新（2026-08-05）

```text
新增/增强 148 篇技术文档，覆盖 14 个知识体系（5 个新建 + 9 个增强）：

🆕 新建（77 篇）：
  01-.../Java面向对象/          11篇   三大特性+现代 Java 数据建模
  01-.../Java集合框架/          10篇   集合体系+选型+并发
  01-.../Java异常体系/          10篇   机制+设计+反模式
  01-.../SDK/                   8篇   概念+JDK 解剖+集成+设计
  01-.../Java多线程/            9篇   原理+API+锁+死锁
  02-.../Spring框架核心/        11篇   IoC/AOP/事务/事件/配置（Spring 7.0）
  02-.../MyBatisPlus/           10篇   CRUD/Wrapper/插件/生产（3.5.17）
  03-.../Milvus/                10篇   向量库深度（3.0 Lake-Native）

🔄 增强（71 篇）：
  03-.../RAG检索增强生成/        +11篇  拓展优化深化（五代演进/GraphRAG/长上下文）
  03-.../Multi-Agent与MCP协议/   +5篇   MCP 无状态规范/A2A/安全/评估
  03-.../向量数据库/             +10篇  2026 双轨格局/DiskANN/选型框架
  03-.../部署工具/               +12篇  2026 引擎格局（vLLM 默认/TGI 退役）
  03-.../模型推理与部署/         +11篇  KV/投机/量化/弹性
  03-.../深度学习/               +11篇  LLM 视角对照/2026 框架格局

关键时效性更新：
  Spring Framework 7.0 / Boot 4（2025-11）· MCP 无状态规范（2026-07）
  Milvus 3.0（2026-07）· JDK 25/26 · 引擎格局（vLLM 45%/TGI 退役）
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
- **时效性**：所有版本/特性标注来源与时间（如 Spring 7.0 GA 2025-11）
- **交叉引用**：系统间导航（**下一模块** / **返回总览** / 专题交叉）

---

## ⚡ 快速开始

### 学习路线

| 目标 | 路径 |
|------|------|
| **Java 后端面试** | 01-Java面向对象 → 01-Java集合框架 → 01-Java异常体系 → 01-Java多线程 → 02-Spring框架核心 → 02-MyBatisPlus → 05-题库 |
| **AI 大模型开发** | 03-大模型基础 → 03-RAG（基础+深化）→ 03-Multi-Agent/MCP → 03-Milvus → 03-部署工具 → 03-模型推理 |
| **AI 架构师** | 03-RAG 深化 → 03-Agent 深化 → 03-向量数据库 → 03-深度学习 → 04-项目 → 05-题库 |
| **全栈架构师** | 01→02→03→04→05 全路径 |

### 仓库导航

- [底层根基](./01-底层根基-Java核心底座/)
- [中间件与微服务](./02-后端中间件%20微服务工程及相关拓展/)
- [AI 大模型](./03-AI大模型应用开发/)
- [项目与课程](./04-项目能力综合提升/)
- [面试冲刺](./05-综合输出-面试冲刺/)

---

## 🤝 贡献指南

1. **新建知识体系**：`00-总览` + 按序模块（`01-`、`02-`...），每篇 200+ 行
2. **增强现有文档**：就地补充，保留原有结构与风格
3. **时效性**：写前检索最新官方信息（版本/特性必须标注来源）
4. **规范**：遵循上方文档规范（表格/代码块/引用/交叉导航）
5. **提交**：`docs: daily contribution #N` 风格，一次提交一个主题

---

*最后更新：2026-08-05 | 2,664 篇 Markdown 文档 | 5 层金字塔 + 14 个活跃知识体系*
