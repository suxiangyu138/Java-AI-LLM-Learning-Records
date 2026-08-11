# Java 后端 + AI 大模型 系统化学习仓库

> 面向 **Java 后端开发工程师** 与 **AI 大模型应用开发工程师** 的全栈知识体系。
> **5,752 篇标准化 Markdown 技术文档**，按 5 层金字塔组织，覆盖从底层基础到面试冲刺的完整学习路径。
> 🏁 **阶段性开发已于 2026-08-12 落幕**，仓库进入维护模式。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Stars](https://img.shields.io/github/stars/suxiangyu138/Java-AI-LLM-Learning-Records?logo=github&label=Stars)](https://github.com/suxiangyu138/Java-AI-LLM-Learning-Records)
[![Docs](https://img.shields.io/badge/docs-5%2C752%20%E7%AF%87%20Markdown-2ea44f?style=flat)](.)
[![Markdown Quality](https://img.shields.io/github/actions/workflow/status/suxiangyu138/Java-AI-LLM-Learning-Records/markdown-quality.yml?label=Markdown%20Quality&logo=githubactions)](https://github.com/suxiangyu138/Java-AI-LLM-Learning-Records/actions)
[![Last Commit](https://img.shields.io/github/last-commit/suxiangyu138/Java-AI-LLM-Learning-Records?label=Last%20Commit)](https://github.com/suxiangyu138/Java-AI-LLM-Learning-Records/commits/main)

---

## 📐 五层金字塔知识体系

```text
Java-AI-LLM-Learning-Records/  (5,752 篇 .md 文件)
│
├── 🏗️ 01-底层根基-Java核心底座/        1826 篇  地基：Java核心+JUC+JVM+DSA+计算机基础(四大件+数学体系+硬件专题)+工具+设计思想(DDD/MVC/SOLID)
│
├── 🔧 02-后端核心技术 微服务 分布式 云原生/ 1266 篇  骨架：MySQL+Redis+MQ+ES+Spring全家桶+MyBatisPlus+DevOps+Web开发全流程
│
├── 🤖 03-AI大模型应用开发/             2044 篇  差异：DeepSeek+RAG(阶段1-5)+Agent内核+MCP+微调多模态+Python数据分析+部署工程化
│
├── 🎯 04-项目能力综合提升/             340 篇  证明：Java-AI/Python-LLM 能力建设 L1-L3+FlavorDash/SuGuangMall/LingShu+84门课程索引
│
└── 📝 05-综合输出-面试冲刺/            276 篇  输出：152题题库+手写代码+简历模板+速查手册+面试四阶段（基础复盘→专项攻坚→项目打磨→模拟面试）
```

---

## 📂 各层详解

### 🏗️ 01-底层根基 — Java 核心底座（1,826 篇）

| 子模块 | 内容 |
|--------|------|
| Java 基础语法与核心特性 | 数据类型、泛型、集合、IO/NIO、异常/日志 |
| **Java 面向对象** | 11 篇：类与对象→封装→继承→多态→抽象类接口→构造器→内部类→record/sealed/模式匹配→SOLID→面试 |
| **Java 集合框架** | 10 篇：Collection 体系→List/Set/Queue/Map→并发集合→比较器→不可变集合→面试 |
| **Java 异常体系** | 10 篇：Throwable 层级→受检/非受检→try-catch-finally→TWR→传播策略→错误码→并发异常→反模式 |
| **SDK** | 8 篇：API/SDK/Framework 辨析→JDK 解剖→JPMS/jlink→第三方集成→自研设计→SDKMAN |
| **Java 多线程** | 9 篇：进程线程本质→Thread API→线程安全三要素→synchronized/volatile→协作→死锁→性能 |
| JUC 高并发编程 | volatile、synchronized、AQS、线程池、CompletableFuture、虚拟线程 |
| JVM 完整底层 | 类加载、运行时数据区、GC(CMS/G1/ZGC)、调优工具 |
| 设计模式 | GoF 23种 + 六大原则 + DDD |
| 计算机基础 | OS、网络、组成原理（CPU/GPU/缓存与Cache/寄存器）、数学基础（离散/数论/信息论/密码学）、分布式理论、计算机安全 |
| **计算机硬件方向系统学习清单** | 10+ 专题：内存墙、量化背后硬件约束、GPU 硬件架构、NPU 概念、PCIe 带宽、RISC-V、微机原理 x86 汇编（8086→x86-64）、关键硬件-软件映射、凸优化、最优化理论 |
| **设计思想与架构工程能力** | 软件工程上的设计思想（11 篇，28 条思想）、MVC 架构模式（11 篇）、DDD 领域驱动设计（11 篇） |
| 数据结构与算法 | LeetCode Hot100、算法导论、排序、图论 |
| 高并发与性能优化 | 高并发设计、池化技术、异步编程 |
| 工具 | Vim、文件后缀名、SSH、JSON、快捷键、命令行 |

### 🔧 02-后端核心技术 微服务 分布式 云原生（1,266 篇）

| 子模块 | 内容 |
|--------|------|
| MySQL 与持久层 | InnoDB、B+树、MVCC、SQL 调优、MyBatis |
| **MyBatisPlus** | 10 篇：定位→实体映射→BaseMapper→Wrapper→IService→插件分页→代码生成→生产实践→面试 |
| Redis 缓存层 | 五大结构、持久化、缓存三问题、分布式锁、Lua |
| RabbitMQ/RocketMQ/Kafka | 消息可靠性、死信/延迟队列、事务消息 |
| Elasticsearch | 倒排索引、DSL 查询、聚合、深度分页 |
| Spring 全家桶与微服务 | IoC/AOP、SpringBoot、SpringMVC、Spring Cloud Alibaba |
| **Spring 框架核心** | 11 篇：容器体系→BeanDefinition→依赖注入→生命周期→AOP→事务→事件→配置→测试→面试（Spring 7.0/Boot 4） |
| 工程运维 | Docker、Nginx、Linux、Git、Maven、K8s、监控 |
| 分布式系统与架构 | CAP/BASE、分布式事务/锁/ID、服务治理 |
| 名词剖析 | 10 大模块 300+ 名词完整体系 |
| 工具链 | Postman、JMeter、Ubuntu、CentOS、项目全流程 |
| **Web 开发全流程** | RESTful API（RFC 9457+幂等键+OpenAPI 3.1）、Cookie & Session、Servlet/Filter/Listener/JSP、SpringBoot Web、Web 服务器、Web 高阶知识 |

### 🤖 03-AI 大模型应用开发（2,044 篇）

| 子模块 | 内容 |
|--------|------|
| 大模型基础与 Prompt 工程 | Token/上下文、CoT、Few-shot、Function Calling、NL2SQL |
| **Function Calling 函数调用** | 11 篇（Agent 基石）：原理演进→协议全解（OpenAI/Anthropic/DeepSeek 对照）→循环工程→工具设计→安全校验→流式→生产成本→面试 |
| **HuggingFace 体系** | 11 篇：Transformers 5.0（模块化/统一分词/动态加载）→Hub 生态→QLoRA 微调→Datasets→Gradio/Spaces→生产实践 |
| **LiteLLM 多模型适配** | 11 篇：v1.94（Router Plugins/Rust 网关迁移）→SDK 统一接口→Proxy 虚拟 key→预算治理→生态集成 |
| **Python 语言体系** | 爬虫阶段 1-7（前置→静态→存储→反爬→动态→框架→进阶，~70 篇）+ Python 异步+FastAPI（11 篇）+ 高级语法/生态/虚拟环境 |
| **Agent 内核与组件体系** | 40+ 套：内核理论模块、四大核心组件、子组件专项（Memory/Planner/Output Parser/Reflection/Tool/Guardrails/感知/观测/编排/Multi-Agent 等）、面试专项（核心缺陷/Function-call 理论） |
| **RAG 阶段 1-5** | 45 篇：基础概念→动手实现 Basic RAG→进阶调优（工程核心）→高级 RAG（GraphRAG/Agentic）→工程化部署 |
| **MCP 与 Agent Skill 体系** | 50+ 套：MCP 阶段 1-4、MCP 核心架构（CS 客户端-服务器）、主流 MCP 开发框架、Agent Skill 阶段 1-5 |
| **RAG 框架** | LangChain、LlamaIndex、Haystack、AutoGen、OpenAI Agents SDK、Reasonix、ZCode、Pytorch AI 应用开发 |
| **大模型微调与多模态** | 90+ 套：微调全流程（基础概念/数据集/工程流程/超参显存/常见问题/PEFT）、多模态体系（基础/数据集/微调/RAG/缺陷/推理部署/LLaVA）、LoRA 深度剖析、五模块课程 |
| **Python 数据分析全体系** | 60+ 套：阶段 1-5（NumPy/Pandas/Matplotlib·Seaborn/数据来源/实战项目/业务分析/机器学习/高阶） |
| **Python 工具库** | 18 套：loguru/PyYAML/unstructured/milvus-python/peft/chromadb/faiss-cpu/transformers/openai python-sdk/pydantic v2/pypdf·python-docx/aiohttp/trl/bitsandbytes/uvicorn/vLLM |
| **向量数据库** | Chroma、PgVector、Qdrant、Redis Stack、Milvus、FAISS + 选型（2026 双轨格局） |
| Milvus | 10 篇：定位版本（3.0）→数据模型→部署→Schema→索引→混合检索→生态集成→生产实践→面试 |
| 深度学习 | 11 篇：概述→CNN→RNN/LSTM→GAN→迁移学习→强化学习→模型压缩→训练技巧→框架（LLM 视角） |
| 模型推理与部署 | KV Cache→投机采样→量化→压测→成本 |
| 部署工具 | Docker/K8s→vLLM/Ollama/TensorRT/ONNX→服务化→MLOps→监控→灰度（2026 引擎格局） |
| 多模态与微调前沿 | DeepSeek、Doubao 豆包、模型微调（PEFT/trl/bitsandbytes） |
| 前沿体系 | Agent 开发、Vibe Coding、Harness Engineering、DeepSeek、Python 虚拟环境、AI 职业发展 |

### 🎯 04-项目能力综合提升（340 篇）

| 分类 | 内容 |
|------|------|
| **Java-AI 后端项目能力建设（33 篇）** | Level1 基础 Demo → Level2 工程化完整项目（简历核心）→ Level3 进阶加分 |
| **Python-LLM 应用开发项目能力建设（44 篇）** | Level1 基础 Demo → Level2 工程化完整项目 → Level3 进阶加分项目 + Python 项目工程硬性要求 |
| 面试考察点（11 篇） | 项目会被深挖的考察点：高并发/分布式/业务设计 |
| 双栈联合实战（11 篇） | Java 后端 + Python LLM 双栈完整项目 |
| 项目避坑（11 篇） | 校招高频踩坑点全梳理 |
| 实战项目（45 篇） | FlavorDash AI 外卖（SpringBoot+AI 六大模块）、SuGuangMall AI 微服务电商、LingShu 医疗 AI 问答 |
| 课程索引（176 篇） | 84+ 门 B 站课程标准化大纲：Java 后端 26 门、中间件 16 门、AI 大模型 23 门、工具运维 12 门、前端语言 11 门 |
| 项目场景题 | 高频项目场景与架构设计题 |

### 📝 05-综合输出 — 面试冲刺（276 篇）

| 子模块 | 内容 |
|--------|------|
| 面试题库 | 152 题全量题库（含标准答案）+ Java/AI 双题库 |
| **面试复习准备（阶段一~四，44 篇）** | 阶段一基础复盘（后端基本功）→ 阶段二 AI 大模型专项攻坚（技术差异化）→ 阶段三项目打磨+简历打磨 → 阶段四模拟面试+查漏补缺 |
| 手写代码题库 | HashMap/线程池/分布式锁/SSE/RAG/Agent 手写 |
| 简历与职业发展 | 多版本简历 + 职业规划 + 面试软实力 |
| 参考速查 | 核心知识点速查手册 + AI/数学名词图谱 |
| 技术栈深度剖析 | Java 后端/Java-AI 大模型企业级开发中用到的技术栈全剖析 |
| 学习路线与能力图谱 | Java 后端分级能力图谱 + AI 大模型学习路线 |

---

## 📌 活跃知识体系（2026-08-12 阶段收官）

| 系统 | 位置 | 篇数 | 亮点 |
|------|------|:---:|------|
| Java 面向对象 | 01-.../Java面向对象/ | 11 | record/sealed/模式匹配 + SOLID |
| Java 集合框架 | 01-.../Java集合框架/ | 10 | 复杂度总表 + 并发容器 + 选型决策 |
| Java 异常体系 | 01-.../Java异常体系/ | 10 | 受检之争（2025）+ FailedException（JDK 25） |
| Java 多线程 | 01-.../Java多线程/ | 9 | 偏向锁移除（JDK 18）+ pinning 修复（JDK 24） |
| SDK | 01-.../SDK/ | 8 | JDK 解剖 + jlink + 第三方集成 + 自研设计 |
| Spring 框架核心 | 02-.../Spring框架核心/ | 11 | Spring 7.0/Boot 4 + CGLIB 一致默认 |
| MyBatisPlus | 02-.../MyBatisPlus/ | 10 | 3.5.17 + Boot4 starter + 逻辑删除×唯一索引 |
| **软件工程上的设计思想** | 01-.../10-计算机设计思想与架构工程能力/软件工程上的设计思想/ | 11 | 28 条工程思想：SOLID/幂等/状态机/容错专项（2026-08 收官） |
| **MVC 架构模式** | 01-.../10-计算机设计思想与架构工程能力/MVC/ | 11 | 1979-2026 全景 + Spring 7 新特性 + 前端 MVVM 双线对照 |
| **DDD 领域驱动设计** | 01-.../10-计算机设计思想与架构工程能力/DDD领域驱动设计/ | 11 | 战略+战术建模 + 事件风暴 + 微服务落地 + DDD×AI |
| **LoRA 深度剖析** | 03-.../09-模型微调与多模态/LoRA深度剖析/ | 11 | ΔW=BA + QLoRA + PEFT 实战 + RLVR 实证（DoRA 46.6%） |
| **RESTful API 设计体系** | 02-.../09-Web开发全流程/RESTful API/ | 11 | RFC 9457 错误格式 + 幂等键（2026 标志性实践）+ OpenAPI 3.1 契约优先 |
| **Agent 内核与组件体系** | 03-.../03-Agent开发/ | 40+ | 内核理论 + 四大核心组件 + 子组件专项 15 套 + 面试专项 |
| **RAG 阶段 1-5** | 03-.../04-RAG检索增强生成/ | 45 | 基础概念 → Basic RAG → 进阶调优 → 高级 RAG → 工程化部署 |
| **MCP 与 Agent Skill 体系** | 03-.../06-MCP协议与Agent Skill/ | 50+ | MCP 阶段 1-4 + 核心架构 + 主流框架 + Skill 阶段 1-5 |
| **Python 数据分析全体系** | 03-.../11-Python数据分析/ | 60+ | NumPy/Pandas/可视化/数据来源/实战/业务分析/机器学习/高阶 |
| **大模型微调与多模态** | 03-.../09-模型微调与多模态/ | 90+ | 微调全流程 + 多模态体系 + LLaVA + 五模块课程 |
| **硬件专题** | 01-.../05-计算机基础/计算机硬件方向系统学习清单/ | 10+ 专题 | 内存墙/量化背后硬件约束/GPU 架构/NPU/PCIe/RISC-V/x86 汇编/凸优化 |
| **项目能力建设** | 04-项目能力综合提升/ | 130 | Java-AI/Python-LLM L1-L3 + 面试考察点 + 双栈联合 + 项目避坑 |
| 主流 Agent 范式 | 03-.../03-Agent开发/主流 Agent 范式/ | 10 | 工作流vs Agent→ReAct→PnE→反思→Agentic Reasoning→多Agent→框架 |
| Multi-Agent/MCP 深化 | 03-.../03-Agent开发/Agent与MCP协议/（12-16） | 5 | 无状态 MCP（2026-07）+ A2A + 工具投毒防御 |
| Milvus | 03-.../Milvus/ | 10 | 3.0 Lake-Native + 混合检索 + 多模态 |
| 向量数据库 | 03-.../04-向量数据库/ | 10 | 2026 双轨格局 + DiskANN 亿级标配 |
| 部署工具 | 03-.../部署工具/ | 12 | 2026 引擎格局（vLLM 默认/TGI 退役） |
| 模型推理与部署 | 03-.../模型推理与部署/ | 11 | KV Cache + 投机采样 + FP8 + 弹性 |
| 深度学习 | 03-.../深度学习/ | 11 | LLM 视角对照表 + 2026 框架格局 |
| CPU | 01-.../计算机组成原理/CPU/ | 8 | 流水线→超标量→现代核心→封装→AI 演进（2025-2026 实例） |
| GPU | 01-.../计算机组成原理/GPU/ | 8 | SIMT→存储→调度→CUDA→Blackwell/Rubin→AI 时代 |
| 缓存与Cache | 01-.../计算机组成原理/缓存与Cache/ | 8 | 结构→替换→一致性→侧信道安全→3D V-Cache |
| 寄存器 | 01-.../计算机组成原理/寄存器/ | 9 | ISA→ABI→重命名→上下文切换→APX（32 GPR） |
| 离散数学 | 01-.../数学基础/离散数学/ | 9 | 逻辑→集合→图论→组合→数论→AI/Java 应用 |
| 初等数论 | 01-.../数学基础/初等数论/ | 9 | 整除→同余→CRT→原根→二次互反律→后量子 |
| 信息论 | 01-.../数学基础/信息论/ | 9 | 熵→KL→信道容量→压缩纠错→率失真→6G 语义 |
| 密码学 | 01-.../数学基础/密码学/ | 9 | AES→哈希→RSA/ECC→PKI→协议→PQC 迁移（2026） |
| 计算机数学基础 | 01-.../数学基础/计算机数学基础/ | 13 | 线代/微积分/概率/离散/数值/数论速查体系（修正版） |
| JMM | 01-.../03-JVM完整底层/JMM/ | 8 | 三大保证→Happens-Before→volatile/锁语义→CAS→JEP 491（JDK 24） |
| 消息队列理论与实战 | 02-.../03-消息队列/消息队列理论与实战/ | 9 | 可靠性→顺序→消费→延迟死信→高可用→分布式事务→选型（Kafka 4.0/RocketMQ 5.x） |
| SQLite | 02-.../01-关系型数据库/SQLite/ | 10 | JSONB→FTS5→向量搜索（sqlite-vec）→WAL→Java 集成（3.53） |
| Memcached | 02-.../02-非关系型数据库/Memcached/ | 9 | slab 内存→一致性哈希→多线程→缓存三问题→Java→vs Redis 选型 |
| Neo4j | 02-.../02-非关系型数据库/Neo4j/ | 9 | Cypher→图算法→原生 Vector→SEARCH→GraphRAG→Spring Data Neo4j |
| Pulsar | 02-.../03-消息队列/Pulsar/ | 9 | 三层架构→多租户→跨地域复制→分层存储→Functions→vs Kafka 选型 |
| 雪花算法 | 02-.../08-分布式系统与架构/雪花算法/ | 10 | 原理→时钟回拨→变体→ID方案→UUIDv7（RFC 9562）→选型 |
| JUnit | 02-.../07-工程运维基础/JUnit/ | 10 | JUnit 6.1.1（Java 17 基线）→三平台架构→生命周期→参数化→扩展→Mock→Boot 4 集成→CI→迁移 |
| Linux | 02-.../运维/Linux 操作系统/Linux/ | 12 | 基础→命令→系统管理→网络安全→服务→速查→权限深度→JVM 排查→日志（Rocky 10/Ubuntu 26.04） |
| Shell | 02-.../运维/Linux 操作系统/Shell/ | 6 | 入门→变量→流程控制→函数实战→grep/sed/awk 三剑客 |
| 数据结构与算法 | 01-.../06-数据结构与算法/ | 33 主题 | 四阶段路线（前置基础→核心思想→高频专题→中高级），73 目录归并编号 |
| 爬虫阶段6：爬虫框架 | 03-AI.../12-Python爬虫/阶段 6：爬虫框架（工程化，写大型爬虫）/ | 10 | Scrapy 2.16 异步现代化 + scrapy-playwright + scrapy-redis 分布式 + 部署监控 |
| Python 异步 + FastAPI | 03-AI.../01-Python语言/Python 异步 + FastAPI/ | 11 | asyncio 事件循环（3.14）+ FastAPI 0.139 全栈（Starlette 1.0/Uvicorn 0.51） |
| Function Calling 函数调用 | 03-AI.../02-大模型基础与Prompt工程/Function Calling 函数调用【Agent 基石】/ | 11 | Agent 基石：协议对照 + 循环工程 + 工具安全 + prompt caching（2026-08） |
| HuggingFace 体系 | 03-AI.../02-大模型基础与Prompt工程/HuggingFace/ | 11 | Transformers 5.0 重构 + Hub 生态 + QLoRA 单卡微调 + Gradio/Spaces |
| LiteLLM 多模型适配 | 03-AI.../02-大模型基础与Prompt工程/LiteLLM 多模型适配/ | 11 | v1.94 + SDK/Proxy 双形态 + 虚拟 key 治理 + Rust 网关迁移（15 倍吞吐） |

---

## 📋 变更历史

> 📄 完整变更日志见 **[CHANGELOG.md](./CHANGELOG.md)**（Keep a Changelog 格式，自 v1.0.0 独立维护）。

| 版本 / 日期 | 内容 |
|------|------|
| **v1.0.0（2026-08-12）** | 🏁 阶段性开发收官：280 次提交 / 170+ 套知识体系 / 3,910 → 5,752 篇（+1,842），思想体系收官（设计思想/MVC/DDD/LoRA） |
| 2026-08-08 | 5 大知识体系 54 篇 + 目录重组（爬虫阶段 6 / FastAPI / Function Calling / HuggingFace / LiteLLM） |
| 2026-08-06 | 18 个知识体系 153 篇（组成原理四大件 / 数学四套 / JMM / MQ / SQLite / Memcached / Neo4j / Pulsar / 雪花 / JUnit） |
| 2026-08-05 | 14 个知识体系 148 篇（Java 面向对象 / 集合 / 异常 / SDK / 多线程 / Spring 核心 / MyBatisPlus / Milvus + RAG 深化） |

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

- **论述优先**：机制/取舍/演进逻辑用文字论述（配代码示例）；表格仅用于对比、API 速查、检查清单
- **Code Block 标注语言**：\```java, \```yaml, \```bash
- **Blockquote 提示**：`> ⚠️` 警告 `> 💡` 技巧 `> 🎯` 总结
- **篇幅标准**：单篇中文字数 2000-2500（2026-08 统一门禁）
- **时效性**：所有版本/特性标注来源与时间（如 Spring 7.0 GA 2025-11）
- **交叉引用**：系统间导航（**下一模块** / **返回总览** / 专题交叉）

---

## ⚡ 快速开始

### 学习路线

| 目标 | 路径 |
|------|------|
| **Java 后端面试** | 01-Java面向对象 → 01-Java集合框架 → 01-Java异常体系 → 01-Java多线程 → 02-Spring框架核心 → 02-MyBatisPlus → 05-题库 |
| **AI 大模型开发** | 03-大模型基础 → 03-RAG（阶段 1-5）→ 03-Function Calling → 03-Agent 内核与组件 → 03-MCP 体系 → 03-部署工具 → 03-模型推理 |
| **Python + AI 工程** | 03-Python 爬虫阶段 1-7 → 03-Python 异步+FastAPI → 03-Function Calling → 03-HuggingFace（QLoRA 微调）→ 03-LiteLLM（多模型接入）→ 部署 |
| **Python 数据分析** | 03-Python数据分析 阶段 1-5（NumPy/Pandas/可视化/ML）→ 爬虫（数据采集）→ 数据来源与实战项目 |
| **AI 微调工程师** | 03-大模型微调体系 → 03-多模态体系 → 03-LoRA 深度剖析 → 03-多模态微调 → 03-推理部署工程 → 09-部署 |
| **AI 架构师** | 03-RAG 阶段 3-5 → 03-Agent 专项 → 03-向量数据库 → 03-深度学习 → 04-项目 → 05-题库 |
| **项目实战** | 04-Java-AI 能力建设 L1-L3 → 04-Python-LLM 能力建设 L1-L3 → 04-双栈联合实战 → 04-项目避坑 |
| **面试冲刺** | 05-面试复习准备 阶段一~四 → 05-题库（152 题）→ 05-手写代码题库 → 05-简历 |
| **全栈架构师** | 01→02→03→04→05 全路径 |

### 仓库导航

- [底层根基](./01-底层根基-Java核心底座/)
- [中间件与微服务](./02-后端核心技术%20微服务%20分布式%20云原生/)
- [AI 大模型](./03-AI大模型应用开发/)
- [项目与课程](./04-项目能力综合提升/)
- [面试冲刺](./05-综合输出-面试冲刺/)

### 项目文档

- [更新日志（CHANGELOG）](./CHANGELOG.md)
- [贡献指南（CONTRIBUTING）](./CONTRIBUTING.md)
- [开发流程（DEVELOPMENT）](./DEVELOPMENT.md)
- [安全策略（SECURITY）](./SECURITY.md)
- [行为准则（CODE OF CONDUCT）](./CODE_OF_CONDUCT.md)

---

## 🤝 贡献指南

> 📄 完整流程见 **[CONTRIBUTING.md](./CONTRIBUTING.md)**；安全相关见 **[SECURITY.md](./SECURITY.md)**。

1. **新建知识体系**：`00-总览` + 按序模块（`01-`、`02-`...），每篇 2000-2500 中文字符
2. **增强现有文档**：就地补充，保留原有结构与风格
3. **时效性**：写前检索最新官方信息（版本/特性必须标注来源）
4. **规范**：遵循上方文档规范（论述优先/代码块/引用/交叉导航）
5. **提交**：`docs:` / `fix:` 前缀（Conventional Commits），一次提交一个主题
6. **🏁 维护模式**：阶段性开发已于 2026-08-12 落幕（280 次提交收官）。当前只接受内容修正（死链/错字/过时信息校准），新知识体系按需创建，不再批量扩建

---

*最后更新：2026-08-12 | 5,752 篇 Markdown 文档 | 5 层金字塔 + 70+ 知识体系 | 🏁 阶段性开发落幕，进入维护模式*
