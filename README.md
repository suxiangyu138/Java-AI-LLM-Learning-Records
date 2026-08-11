# Java 后端 + AI 大模型 系统化学习仓库

> 面向 **Java 后端开发工程师** 与 **AI 大模型应用开发工程师** 的全栈知识体系。
> **5,752 篇标准化 Markdown 技术文档**，按 5 层金字塔组织，覆盖从底层基础到面试冲刺的完整学习路径。
> 🏁 **阶段性开发已于 2026-08-12 落幕**，仓库进入维护模式。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📐 五层金字塔知识体系

```
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

## 🏁 阶段收官：本次更新（2026-08-08 → 08-12）

```text
🏁 阶段性开发落幕（280 次提交 / 170+ 套知识体系 / 3,910 → 5,752 篇，+1,842 篇）：

2026-08-12 思想体系收官（44 篇）：
  01-.../10-计算机设计思想与架构工程能力/  软件工程上的设计思想（28 条思想）+ MVC 架构模式 + DDD 领域驱动设计
  03-.../09-模型微调与多模态/LoRA深度剖析/  LoRA 原理数学→QLoRA→PEFT 实战→RLVR 实证（DoRA 46.6%）

2026-08-11 硬件专题 + 项目能力建设（~30 套体系）：
  01-.../05-计算机基础/计算机硬件方向系统学习清单/  GPU 硬件架构/NPU 概念/PCIe 带宽/RISC-V/关键硬件-软件映射/
    微机原理 x86 汇编（8086→x86-64）/内存墙/量化背后硬件约束/凸优化/最优化理论
  04-项目能力综合提升重建（~130 篇）：Java-AI 后端能力建设 L1-L3（33）+ Python-LLM 能力建设 L1-L3
    与工程硬性要求（44）+ 面试考察点/双栈联合实战/项目避坑（各 11）
  05-.../面试复习准备/  阶段一~四（基础复盘→AI 专项攻坚→项目+简历打磨→模拟面试）
  专题 7-9：分布式高并发与限流熔断降级 / 架构层面优化与全链路压测 / 故障复盘与真实线上案例

2026-08-10 Python 数据分析 + 微调多模态 + 工具库 + Web 工程（~90 套体系）：
  03-.../11-Python数据分析/  阶段 1-5 全体系（前置基础/NumPy/Pandas/Matplotlib·Seaborn/数据来源/实战/
    业务分析/机器学习/高阶）+ 爬虫阶段 7 进阶高级内容
  03-.../09-模型微调与多模态/  微调体系（基础概念/数据集/工程流程/超参显存/常见问题/PEFT）+
    多模态体系（基础/数据集/微调/RAG/缺陷/推理部署/LLaVA）+ 五模块课程导航
  Python 工具库 18 套：loguru/PyYAML/unstructured/milvus-python/peft/chromadb/faiss-cpu/
    transformers/openai python-sdk/pydantic v2/pypdf·python-docx/aiohttp/trl/bitsandbytes/uvicorn/vLLM
  向量库：Chroma/PgVector/Qdrant/Redis Stack
  Web 与工程：RESTful API/Token/Swagger/Knife4j/WebSocket/浏览器内核/小程序 UniAPP/飞书/Gitee/Gradle/SonarQube
  性能专题：性能理论基础/Java 单机并发编程/JVM 性能调优/数据库性能优化/缓存体系专题/中间件 & IO 优化
  软件架构能力：L1 初级→L2 中级→L3 高级→L4 架构师 + 横向通用能力

2026-08-09 Agent 内核 + RAG 阶段 + MCP 全体系（~90 套体系）：
  Agent 内核：什么是 LLM Agent/区分概念/经典 Agent 范式/四大核心组件（11 篇）+ 组件速记 4 套
  Agent 子组件专项 20+ 套：Memory/Planner/Output Parser/Reflection/Tool 开发注册/安全护栏/感知/
    观测可观测/Feedback 评估/Orchestrator Runtime/Multi-Agent 协作/Prompt 角色配置/文档解析/
    数据库交互/联网搜索/代码执行沙盒/前端演示界面/新兴 SDK/多 Agent 框架/低代码平台
  Agent 面试专项：四大核心组件（面试必背）/Agent 的核心缺陷/Function-call 完整理论流程
  Coding Agent：Reasonix（DeepSeek）/ZCode（智谱）/OpenAI Agents SDK
  RAG 阶段 1-5（45 篇）：基础概念→Basic RAG→进阶调优→高级 RAG→工程化部署
  RAG 框架：LangChain/LlamaIndex/Haystack/AutoGen/Pytorch AI 应用开发
  MCP 体系：阶段 1-4/核心架构（CS 客户端-服务器）/主流 MCP 开发框架/Agent Skill 阶段 1-5
  硬件基础：操作系统硬件关联/计算机网络硬件基础
  规范升级：MarkdownDocGenerator 技能 v1.2→v1.4、篇幅统一 2000 字、表格策略修订（Prose-first）

关键时效性（全部检索校准至 2026-08）：
  2026 标志性实践：Idempotency-Key 幂等键（Stripe 风格 TTL 24h）、RFC 9457 Problem Details、
    OpenAPI 3.1 契约优先、URL 路径版本化
  MCP 无状态规范（2026-07-28）/A2A 协议/DeepSeek V4（2026-07-24）
  Transformers 5.0（2026-01-27）/PEFT 0.19.1/QLoRA 单卡微调标准
  RLVR 实证：DoRA 46.6% > 全参 44.9% > LoRA 42.5%
  内存墙：HBM4 2.0-2.8TB/s、CXL 4.0 机架池化；量化：B300 FP4 15PF
  SOLID 2026 学术实证/模块化单体+右规模服务为推荐起点（DDD）
```

---

## 🆕 历史更新（2026-08-08）

```text
新增 54 篇技术文档（5 大知识体系，全部检索校准至 2026-08）+ 知识库目录重组：

🆕 新建（54 篇）：
  03-AI.../12-Python爬虫/阶段 6：爬虫框架（工程化，写大型爬虫）/  10篇
    Scrapy 2.16.0（2026-05-19）/2.14 异步现代化/scrapy-playwright 0.0.48/
    scrapy-redis 分布式（布隆去重）/scrapyd 1.6.0 部署/生产实战
  03-AI.../01-Python语言/Python 异步 + FastAPI/                   11篇
    FastAPI 0.139（0.130 Rust 序列化 2 倍）/Starlette 1.0（on_event 移除）/
    Uvicorn 0.51（SIGHUP 重叠重启）/Python 3.14 asyncio（get_event_loop 变更）/
    SQLAlchemy async 连接池/N+1 治理/压测方法论
  03-AI.../02-大模型基础与Prompt工程/Function Calling 函数调用【Agent 基石】/ 11篇
    OpenAI strict + 并行调用/Anthropic 显式缓存/DeepSeek V4 thinking 回传坑/
    调用循环工程/工具安全（确认-执行/提示注入）/prompt caching 降本 50-90%
  03-AI.../02-大模型基础与Prompt工程/HuggingFace/                  11篇
    Transformers 5.0.0（2026-01-27 模块化重构/统一分词/动态权重加载）/
    Hub 150 万+ 模型/hf-mirror 镜像/QLoRA 单卡微调/transformers serve/Gradio Spaces
  03-AI.../02-大模型基础与Prompt工程/LiteLLM 多模型适配/            11篇
    v1.94.0（2026-07-28 Router Plugins）/Rust 网关迁移（453→6782 RPS）/
    SDK 统一接口/Proxy 虚拟 key/预算三级治理/数据面控制面分离

🔄 目录重组（153 旧路径文件迁移，Git 自动识别为 rename）：
  Python爬虫 → 03-AI.../12-Python爬虫（主体系 12 篇 + 阶段 1-7）
  Python数据分析 → 03-AI.../11-Python数据分析
  旧 FastAPI/深度学习/ML基础/NLP/Transformer 等 → 新编号体系（01-13）
  URL 体系 → 01-底层根基/计算机基础/计算机网络/URL

关键时效性：
  Scrapy 2.16.0（2026-05-19）· scrapy-playwright 0.0.48（camoufox 反检测内核）
  FastAPI 0.139 · Starlette 1.0（2026-03-22）· Uvicorn 0.51.0（2026-07-08）
  Python 3.14（2025-10-07，asyncio 自由线程一等公民）
  DeepSeek V4（2026-07-24 chat/reasoner 弃用，thinking 模式 FC 回传铁律）
  Transformers 5.0.0（2026-01-27，五年首个大版本）
  LiteLLM v1.94.0（2026-07-28）· Rust 网关 beta（sub-1ms 开销）
```

---

## 🆕 历史更新（2026-08-06）

```text
新增 153 篇技术文档，覆盖 18 个知识体系（计算机组成原理四大件 + 数学基础四套 + JMM + MQ 理论 + SQLite + Memcached + Neo4j + Pulsar + Agent 范式 + 雪花算法 + JUnit + 存量修正）：

🆕 新建（153 篇）：
  01-.../计算机组成原理/CPU/         8篇   组成/流水线/超标量/实例/封装/评测/AI演进
  01-.../计算机组成原理/GPU/         8篇   SIMT/存储/调度/CUDA/NVIDIA-AMD/AI时代
  01-.../计算机组成原理/缓存与Cache/  8篇   结构/替换算法/一致性/侧信道/前沿
  01-.../计算机组成原理/寄存器/       9篇   ISA/ABI/重命名/上下文切换/前沿
  01-.../数学基础/离散数学/          9篇   逻辑/集合/图论/组合/代数/Java实战
  01-.../数学基础/初等数论/          9篇   整除/同余/CRT/原根/互反律/后量子
  01-.../数学基础/信息论/            9篇   熵/KL/信道容量/压缩/纠错/率失真/6G语义
  01-.../数学基础/密码学/            9篇   AES/哈希/RSA-ECC/PKI/协议/PQC迁移
  01-.../03-JVM完整底层/JMM/         8篇   三大保证/Happens-Before/volatile-锁语义/CAS/JEP 491
  02-.../03-消息队列/消息队列理论与实战/ 9篇  可靠性/顺序/消费/延迟死信/高可用/事务/选型
  02-.../01-关系型数据库/SQLite/     10篇  架构/类型/WAL事务/JSONB/FTS5/向量搜索/Java集成
  02-.../02-非关系型数据库/Memcached/ 9篇  slab内存/协议/一致性哈希/多线程/三问题/选型
  02-.../02-非关系型数据库/Neo4j/      9篇  图模型/Cypher/图算法/向量GraphRAG/Java集成
  02-.../03-消息队列/Pulsar/          9篇  三层架构/多租户/跨地域复制/分层存储/Functions
  03-.../03-Agent与MCP协议/主流 Agent 范式/ 10篇 工作流vs Agent/ReAct/PnE/反思/Agentic Reasoning/框架
  02-.../08-分布式系统与架构/雪花算法/  10篇 原理/时钟回拨/变体/UUIDv7/选型/生产
  02-.../07-工程运维基础/JUnit/      10篇  演进史/JUnit 6.1.1/三平台架构/生命周期/参数化/扩展/Mock/Boot 4/CI/迁移

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
  JUnit 6.1.1（2026-06）：Java 17 基线 · Vintage 弃用 · 统一版本号 · FastCSV · JFR 内置
  Spring Boot 4：JUnit 6 默认 · @MockBean→@MockitoBean · Vintage 移除 · @ServiceConnection
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

---

## 🤝 贡献指南

1. **新建知识体系**：`00-总览` + 按序模块（`01-`、`02-`...），每篇 2000-2500 中文字符
2. **增强现有文档**：就地补充，保留原有结构与风格
3. **时效性**：写前检索最新官方信息（版本/特性必须标注来源）
4. **规范**：遵循上方文档规范（论述优先/代码块/引用/交叉导航）
5. **提交**：`docs: daily contribution #N` 风格，一次提交一个主题
6. **🏁 维护模式**：阶段性开发已于 2026-08-12 落幕（280 次提交收官）。当前只接受内容修正（死链/错字/过时信息校准），新知识体系按需创建，不再批量扩建

---

*最后更新：2026-08-12 | 5,752 篇 Markdown 文档 | 5 层金字塔 + 70+ 知识体系 | 🏁 阶段性开发落幕，进入维护模式*
