# Java 后端 + AI 大模型 系统化学习仓库

> 面向 **Java 后端开发工程师** 与 **AI 大模型应用开发工程师** 的全栈知识体系。
> **2,952 篇标准化 Markdown 技术文档**，按 5 层金字塔组织，覆盖从底层基础到面试冲刺的完整学习路径。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📐 五层金字塔知识体系

```
Java-AI-LLM-Learning-Records/  (2,952 篇 .md 文件)
│
├── 🏗️ 01-底层根基-Java核心底座/        1023 篇  地基：Java核心+JUC+JVM+DSA+计算机基础(四大件+数学体系)+工具(Vim/文件后缀/SDK)
│
├── 🔧 02-后端核心技术 微服务 分布式 云原生/ 717 篇  骨架：MySQL+Redis+MQ+ES+Spring全家桶+MyBatisPlus+DevOps
│
├── 🤖 03-AI大模型应用开发/             745 篇  差异：DeepSeek+RAG+Agent+MCP+Milvus+部署工程化+Python
│
├── 🎯 04-项目能力综合提升/             230 篇  证明：FlavorDash+SuGuangMall+LingShu 项目 + 84门课程索引
│
└── 📝 05-综合输出-面试冲刺/            232 篇  输出：152题题库+手写代码+简历模板+速查手册+能力图谱
```

---

## 📂 各层详解

### 🏗️ 01-底层根基 — Java 核心底座（1023 篇）

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
| 计算机基础 | OS、网络、组成原理（**🆕 CPU 8篇/GPU 8篇/缓存与Cache 8篇/寄存器 9篇**）、数学基础（**🆕 离散数学 9篇/初等数论 9篇/信息论 9篇/密码学 9篇**）、分布式理论、计算机安全 |
| 数据结构与算法 | LeetCode Hot100、算法导论、排序、图论 |
| 高并发与性能优化 | 高并发设计、池化技术、异步编程 |
| 工具 | Vim、文件后缀名、SSH、JSON、快捷键、命令行 |

### 🔧 02-后端核心技术 微服务 分布式 云原生（717 篇）

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

### 🤖 03-AI 大模型应用开发（745 篇）

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
| **🆕 CPU** | 01-.../计算机组成原理/CPU/ | 8 | 流水线→超标量→现代核心→封装→AI 演进（2025-2026 实例） |
| **🆕 GPU** | 01-.../计算机组成原理/GPU/ | 8 | SIMT→存储→调度→CUDA→Blackwell/Rubin→AI 时代 |
| **🆕 缓存与Cache** | 01-.../计算机组成原理/缓存与Cache/ | 8 | 结构→替换→一致性→侧信道安全→3D V-Cache |
| **🆕 寄存器** | 01-.../计算机组成原理/寄存器/ | 9 | ISA→ABI→重命名→上下文切换→APX（32 GPR） |
| **🆕 离散数学** | 01-.../数学基础/离散数学/ | 9 | 逻辑→集合→图论→组合→数论→AI/Java 应用 |
| **🆕 初等数论** | 01-.../数学基础/初等数论/ | 9 | 整除→同余→CRT→原根→二次互反律→后量子 |
| **🆕 信息论** | 01-.../数学基础/信息论/ | 9 | 熵→KL→信道容量→压缩纠错→率失真→6G 语义 |
| **🆕 密码学** | 01-.../数学基础/密码学/ | 9 | AES→哈希→RSA/ECC→PKI→协议→PQC 迁移（2026） |
| **🆕 计算机数学基础** | 01-.../数学基础/计算机数学基础/ | 13 | 线代/微积分/概率/离散/数值/数论速查体系（修正版） |
| **🆕 JMM** | 01-.../03-JVM完整底层/JMM/ | 8 | 三大保证→Happens-Before→volatile/锁语义→CAS→JEP 491（JDK 24） |
| **🆕 消息队列理论与实战** | 02-.../03-消息队列/消息队列理论与实战/ | 9 | 可靠性→顺序→消费→延迟死信→高可用→分布式事务→选型（Kafka 4.0/RocketMQ 5.x） |
| **🆕 SQLite** | 02-.../01-关系型数据库/SQLite/ | 10 | JSONB→FTS5→向量搜索（sqlite-vec）→WAL→Java 集成（3.53） |
| **🆕 Memcached** | 02-.../02-非关系型数据库/Memcached/ | 9 | slab 内存→一致性哈希→多线程→缓存三问题→Java→vs Redis 选型 |
| **🆕 Neo4j** | 02-.../02-非关系型数据库/Neo4j/ | 9 | Cypher→图算法→原生 Vector→SEARCH→GraphRAG→Spring Data Neo4j |
| **🆕 Pulsar** | 02-.../03-消息队列/Pulsar/ | 9 | 三层架构→多租户→跨地域复制→分层存储→Functions→vs Kafka 选型 |
| **🆕 主流 Agent 范式** | 03-AI.../03-Agent与MCP协议/主流 Agent 范式/ | 10 | 工作流vs Agent→ReAct→PnE→反思→Agentic Reasoning→多Agent→框架 |
| **🆕 雪花算法** | 02-.../08-分布式系统与架构/雪花算法/ | 10 | 原理→时钟回拨→变体→ID方案→UUIDv7（RFC 9562）→选型 |

---

## 🆕 本次更新（2026-08-06）

```text
新增 143 篇技术文档，覆盖 17 个知识体系（计算机组成原理四大件 + 数学基础四套 + JMM + MQ 理论 + SQLite + Memcached + Neo4j + Pulsar + Agent 范式 + 雪花算法 + 存量修正）：

🆕 新建（143 篇）：
  01-.../计算机组成原理/CPU/         8篇   组成/流水线/超标量/实例/封装/评测/AI演进
  01-.../计算机组成原理/GPU/         8篇   SIMT/存储/调度/CUDA/NVIDIA-AMD/AI时代
  01-.../计算机组成原理/缓存与Cache/  8篇   结构/替换算法/一致性/侧信道/前沿
  01-.../计算机组成原理/寄存器/       9篇   ISA/ABI/重命名/上下文切换/前沿
  01-.../数学基础/离散数学/          9篇   逻辑/集合/图论/组合/代数/Java实战
  01-.../数学基础/初等数论/          9篇   整除/同余/CRT/原根/互反律/后量子
  01-.../数学基础/信息论/            9篇   熵/KL/信道容量/压缩/率失真/6G语义
  01-.../数学基础/密码学/            9篇   AES/哈希/RSA-ECC/PKI/协议/PQC迁移
  01-.../03-JVM完整底层/JMM/         8篇   三大保证/Happens-Before/volatile-锁语义/CAS/JEP 491
  02-.../03-消息队列/消息队列理论与实战/ 9篇  可靠性/顺序/消费/延迟死信/高可用/事务/选型
  02-.../01-关系型数据库/SQLite/     10篇  架构/类型/WAL事务/JSONB/FTS5/向量搜索/Java集成
  02-.../02-非关系型数据库/Memcached/ 9篇  slab内存/协议/一致性哈希/多线程/三问题/选型
  02-.../02-非关系型数据库/Neo4j/      9篇  图模型/Cypher/图算法/向量GraphRAG/Java集成
  02-.../03-消息队列/Pulsar/          9篇  三层架构/多租户/跨地域复制/分层存储/Functions
  03-.../03-Agent与MCP协议/主流 Agent 范式/ 10篇 工作流vs Agent/ReAct/PnE/反思/Agentic Reasoning/框架
  02-.../08-分布式系统与架构/雪花算法/  10篇 原理/时钟回拨/变体/UUIDv7/选型/生产

🔄 修正（13 篇存量）：
  01-.../数学基础/计算机数学基础/    13篇  编号错乱/失效链接/数学公式错误全面修正

关键时效性：
  EPYC Turin 192核 · Panther Lake 18A · Rubin HBM4（2026）· Diamond Rapids 弃 SMT
  后量子混合 TLS（RFC 9794，Chrome 30%+）· 语义信息论（张平团队）· APX 32 GPR
  JEP 491（JDK 24）synchronized 不再 pinning 虚拟线程
  Kafka 4.0 KRaft/KIP-848/KIP-932 · RocketMQ 5.x 存算分离 · RabbitMQ 4.0 Quorum
  SQLite 3.53（JSONB/ALTER 约束）· sqlite-vec 向量搜索 · FTS5 混合检索
  Memcached 1.6.x（多线程 150 万 ops/s）· Redis 双许可与 Valkey
  Neo4j 2025.10+（原生 Vector/SEARCH 语法/索引内过滤/GRAPH TYPE）
  Pulsar v4.2（V2 命名/Oxia 元数据）· 腾讯 TDMQ 百万 QPS 验证
  Agent 范式：Model-Native Harness（2026）· Anthropic 五工作流 · 推理模型内化循环
  UUIDv7（RFC 9562）：PG 18/Java 26 原生 · 雪花 64 位场景保留
```

---

## 🆕 历史更新（2026-08-05）

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
- [中间件与微服务](./02-后端核心技术%20微服务%20分布式%20云原生/)
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

*最后更新：2026-08-06 | 2,952 篇 Markdown 文档 | 5 层金字塔 + 31 个活跃知识体系*
