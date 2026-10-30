# Java 后端 + AI 大模型 系统化学习仓库

> 面向 **Java 后端开发工程师** 与 **AI 大模型应用开发工程师** 的全栈知识体系。
> **2,912 篇标准化 Markdown 技术文档**，按 5 层金字塔组织，覆盖从底层基础到面试冲刺的完整学习路径。
> 2026-10-30 按求职简历收敛，并按「秋招是否真会考」三轮裁剪：2,973 篇迁入本地归档区 `前方的路 以后再来探索吧/`，该目录已 git 忽略，只留在本机，不推送。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Docs](https://img.shields.io/badge/docs-3%2C540%20%E7%AF%87%20Markdown-2ea44f?style=flat)](.)

---

## 五层金字塔知识体系

```text
Java-AI-LLM-Learning-Records/  (2,912 篇 .md 文件)
│
├── 01-底层根基-Java核心底座/        1166 篇  地基：Java核心+JUC+JVM+DSA+OS/计网+设计模式+高并发专题
│
├── 02-后端核心技术 微服务 分布式 云原生/ 651 篇  骨架：MySQL+Redis+RabbitMQ+ES+Spring全家桶+SpringCloudAlibaba+Docker/Nginx
│
├── 03-AI大模型应用开发/             847 篇  差异：Prompt/Function Calling+Agent+MCP+RAG+Milvus+Python爬虫与数据分析
│
├── 04-项目能力综合提升/             109 篇  证明：Java-AI 能力建设 L1-L3+FlavorDash/SuGuangMall/LingShu
│
└── 05-综合输出-面试冲刺/            139 篇  输出：题库+手写代码+简历+速查+面试四阶段
```

---

## 各层详解

### 01-底层根基 — Java 核心底座（1,443 篇）

| 子模块 | 内容 |
|--------|------|
| Java 基础语法与核心特性 | 数据类型、泛型、反射、集合、IO/NIO、异常、Stream、枚举、正则、语法糖 |
| **Java 面向对象** | 11 篇：类与对象→封装→继承→多态→抽象类接口→构造器→内部类→record/sealed/模式匹配→SOLID→面试 |
| **Java 集合框架** | 10 篇：Collection 体系→List/Set/Queue/Map→并发集合→比较器→不可变集合→面试 |
| **Java 异常体系** | 10 篇：Throwable 层级→受检/非受检→try-catch-finally→TWR→传播策略→错误码→并发异常→反模式 |
| **Java 多线程** | 9 篇：进程线程本质→Thread API→线程安全三要素→synchronized/volatile→协作→死锁→性能 |
| JUC 高并发编程 | volatile、synchronized、AQS、线程池、CompletableFuture、虚拟线程、ThreadLocal |
| JVM 完整底层 | 类加载、运行时数据区、GC(CMS/G1/ZGC)、调优工具、JMM、OOM 排查 |
| 设计模式 | GoF 23 种 + 六大原则 + 源码中的落地 |
| 计算机基础 | 操作系统、计算机网络（TCP/UDP/HTTP/HTTPS/DNS）、数据库理论 |
| 数据结构与算法 | 四阶段路线（前置基础→核心思想→高频专题→中高级）、LeetCode 高频题型、图论、动态规划、字符串 |
| 高并发与性能优化 | 9 个专题：性能理论→单机并发→JVM 调优→数据库→缓存→中间件与 IO→限流熔断降级→架构与压测→故障复盘 |
| 设计思想与架构工程能力 | 软件工程上的设计思想（SOLID/幂等/状态机/容错）、软件架构能力体系、企业级标准、日志、异步编程 |
| 工具 | SSH、JSON、Swagger/Knife4j、命令行终端、包管理器、脚本、配置文件、云厂商控制台 |

### 02-后端核心技术 微服务 分布式 云原生（786 篇）

| 子模块 | 内容 |
|--------|------|
| MySQL 与持久层 | InnoDB、B+树、MVCC、事务隔离、索引优化、SQL 调优、JDBC、连接池、MyBatis |
| **MyBatisPlus** | 10 篇：定位→实体映射→BaseMapper→Wrapper→IService→插件分页→代码生成→生产实践→面试 |
| Redis 缓存层 | 五大结构、持久化、缓存穿透/击穿/雪崩、分布式锁、Lua 脚本、多级缓存 |
| RabbitMQ | 可靠性投递、死信/延迟队列、削峰、幂等消费；消息队列理论与选型 |
| Elasticsearch | 倒排索引、分词器、DSL 查询、聚合、深度分页 |
| Spring 全家桶 | IoC/AOP、SpringBoot 3.x、SpringMVC、Spring Security、Spring Task、Spring Data Redis |
| **Spring 框架核心** | 11 篇：容器体系→BeanDefinition→依赖注入→生命周期→AOP→事务→事件→配置→测试→面试（Spring 7.0/Boot 4） |
| Spring Cloud Alibaba | Nacos、Sentinel、Seata、Gateway、OpenFeign、LoadBalancer、CircuitBreaker |
| 工程运维 | Docker、Nginx、Linux、Git、Maven、日志监控、网络排查、MySQL/Redis/MQ 故障现象 |
| 分布式系统与架构 | CAP/BASE、分布式事务/锁/ID、服务治理、雪花算法 |
| 名词剖析 | 10 大模块 300+ 名词完整体系 |
| **Web 开发全流程** | RESTful API（RFC 9457+幂等键+OpenAPI 3.1）、Cookie & Session、Servlet/Filter、Dubbo、SpringBoot Web、Tomcat、Web 高阶知识 |
| 前端基础 | WebSocket 实时通信、Token/JWT、HTTP 规范与跨域、RESTful 接口设计 |

### 03-AI 大模型应用开发（1,287 篇）

| 子模块 | 内容 |
|--------|------|
| 大模型基础与 Prompt 工程 | Token/上下文、CoT、Few-shot、结构化输出、LLM API、OpenAI/Anthropic 兼容接口、Embedding |
| **Function Calling 函数调用** | 11 篇（Agent 基石）：原理演进→协议全解（OpenAI/Anthropic/DeepSeek 对照）→循环工程→工具设计→安全校验→流式→生产成本→面试 |
| **LiteLLM 多模型适配** | 11 篇：v1.94（Router Plugins/Rust 网关迁移）→SDK 统一接口→Proxy 虚拟 key→预算治理→生态集成 |
| **Agent 内核与组件体系** | 内核理论模块（11）、四大核心组件（11）、配套工具生态（代码沙盒/联网搜索/文档解析/数据库交互）、调优排错与安全、幻觉治理、阶段 1-5 实战路径 |
| **RAG 阶段 1-5** | 45 篇：基础概念→动手实现 Basic RAG→进阶调优（工程核心）→高级 RAG（GraphRAG/Agentic）→工程化部署 |
| **MCP 与 Agent Skill 体系** | 50+ 套：MCP 阶段 1-4、MCP 核心架构（CS 客户端-服务器）、主流 MCP 开发框架、Agent Skill 阶段 1-5 |
| AI 开发框架 | Spring AI、LangChain4j、LangChain（Java AI 生态为核心） |
| **Python 语言体系** | 爬虫阶段 1-6（前置→静态→存储→反爬→动态→框架）+ 高级语法/生态/虚拟环境 |
| **Python 数据分析全体系** | 60+ 套：阶段 1-5（NumPy/Pandas/Matplotlib·Seaborn/数据来源/实战项目/业务分析/机器学习/高阶） |
| Milvus 与向量数据库 | Milvus 10 篇 + 向量索引原理 + 2026 选型格局 |
| 模型推理与部署 | KV Cache→投机采样→量化→压测→成本；vLLM/Ollama、私有化部署、MLOps |
| AI 开发工具 | Claude Code、主流大模型 API 调用（OpenAI / Claude / DeepSeek / Qwen，同一套兼容协议） |

### 04-项目能力综合提升（109 篇）

| 分类 | 内容 |
|------|------|
| **Java-AI 后端项目能力建设（33 篇）** | Level1 基础 Demo → Level2 工程化完整项目（简历核心）→ Level3 进阶加分 |
| 面试考察点（11 篇） | 项目会被深挖的考察点：高并发/分布式/业务设计 |
| 项目避坑（11 篇） | 校招高频踩坑点全梳理 |
| 实战项目（45 篇） | FlavorDash AI 外卖（SpringBoot+AI 六大模块）、SuGuangMall AI 微服务电商、LingShu 医疗 AI 问答 |
| 项目场景题 | 高频项目场景与架构设计题 |

### 05-综合输出 — 面试冲刺（208 篇）

| 子模块 | 内容 |
|--------|------|
| 面试题库 | 152 题全量题库（含标准答案）+ Java/AI 双题库 |
| **面试复习准备（阶段一~四，44 篇）** | 阶段一基础复盘（后端基本功）→ 阶段二 AI 大模型专项攻坚（技术差异化）→ 阶段三项目打磨+简历打磨 → 阶段四模拟面试+查漏补缺 |
| 手写代码题库 | HashMap/线程池/分布式锁/SSE/RAG/Agent 手写 |
| 简历与职业发展 | 多版本简历 + 职业规划 |
| 参考速查 | 核心知识点速查手册 + AI/数学名词图谱 |
| 技术栈深度剖析 | Java-AI 大模型企业级开发中用到的技术栈全剖析 |
| 名词剖析 | Java 后端开发名词剖析与知识体系搭建 |

### 本地归档区（2,973 篇，不入库）

> 迁出的内容只留在本机 `前方的路 以后再来探索吧/`，该目录已写入 `.gitignore`，不提交、不推送。需要时按目录移回即可。

| 迁出类别 | 举例 |
|------|------|
| 简历未写的同类替代品 | Oracle、PostgreSQL、SQL Server、SQLite、MongoDB、HBase、Memcached、Neo4j、Kafka、RocketMQ、Pulsar、ActiveMQ、Chroma、Qdrant、PgVector、MinIO |
| 简历未写的相邻方向 | Kubernetes/Prometheus、前端全套（HTML/CSS/JS/Vue/React）、模型微调与多模态、强化学习、深度学习、组成原理与数学、测试工具（JUnit/Postman/JMeter） |
| 旧版与汇总版重复体系 | Spring Core、Spring 全家桶、旧版 Spring Data Redis、JavaWeb 汇总、Agent 子组件专项、LLM Agent 理论课、Java 后端技术全览、Spring 底层核心组件手册、Agent 进阶工程化组件 |
| 秋招不考的高阶专题 | 算法竞赛（树链剖分/虚树/LCT/莫队/CDQ/AC自动机/Manacher/后缀数组/网络流类图论/博弈/随机算法）、Harness/Hook/Loop Engineering、Agent 开发框架层与 Skill、架构师 L3/L4、模型部署容灾运维、6 家重复供应商 API |
| 学习过程产物 | 84 门课程大纲、软件工程与 MVC/DDD 方法论、职业规划与软技能 |
| 工具类百科与个人环境文档 | Vim、Markdown、文件后缀名、键盘符号、快捷键、VPN、飞书、本机环境说明书 |

迁回方式：`git mv "前方的路 以后再来探索吧/<原路径>" "<原路径>"`，归档区保留了迁移前的完整相对路径。

---

## 活跃知识体系（2026-10-30 简历收敛后）

| 系统 | 位置 | 篇数 | 亮点 |
|------|------|:---:|------|
| Java 面向对象 | 01-.../Java面向对象/ | 11 | record/sealed/模式匹配 + SOLID |
| Java 集合框架 | 01-.../Java集合框架/ | 10 | 复杂度总表 + 并发容器 + 选型决策 |
| Java 异常体系 | 01-.../Java异常体系/ | 10 | 受检之争（2025）+ FailedException（JDK 25） |
| Java 多线程 | 01-.../Java多线程/ | 9 | 偏向锁移除（JDK 18）+ pinning 修复（JDK 24） |
| JMM | 01-.../03-JVM完整底层/JMM/ | 8 | 三大保证→Happens-Before→volatile/锁语义→CAS→JEP 491（JDK 24） |
| **软件工程上的设计思想** | 01-.../10-计算机设计思想与架构工程能力/软件工程上的设计思想/ | 11 | 28 条工程思想：SOLID/幂等/状态机/容错专项（2026-08 收官） |
| 数据结构与算法 | 01-.../06-数据结构与算法/ | 33 主题 | 四阶段路线（前置基础→核心思想→高频专题→中高级），73 目录归并编号 |
| Spring 框架核心 | 02-.../Spring框架核心/ | 11 | Spring 7.0/Boot 4 + CGLIB 一致默认 |
| MyBatisPlus | 02-.../MyBatisPlus/ | 10 | 3.5.17 + Boot4 starter + 逻辑删除×唯一索引 |
| **RESTful API 设计体系** | 02-.../09-Web开发全流程/RESTful API/ | 11 | RFC 9457 错误格式 + 幂等键（2026 标志性实践）+ OpenAPI 3.1 契约优先 |
| 消息队列理论与实战 | 02-.../03-消息队列/消息队列理论与实战/ | 9 | 可靠性→顺序→消费→延迟死信→高可用→分布式事务→选型 |
| 雪花算法 | 02-.../08-分布式系统与架构/雪花算法/ | 10 | 原理→时钟回拨→变体→ID方案→UUIDv7（RFC 9562）→选型 |
| Linux | 02-.../07-工程运维测试拓展/运维/Linux 操作系统/ | 12 | 基础→命令→系统管理→网络安全→服务→速查→权限深度→JVM 排查→日志（Rocky 10/Ubuntu 26.04） |
| Shell | 02-.../07-工程运维测试拓展/运维/Linux 操作系统/Shell/ | 6 | 入门→变量→流程控制→函数实战→grep/sed/awk 三剑客 |
| **Agent 内核与组件体系** | 03-.../03-Agent开发/ | 200+ | 内核理论 + 四大核心组件 + 工具生态 + 阶段 1-5 + 面试专项 |
| 主流 Agent 范式 | 03-.../03-Agent开发/主流 Agent 范式/ | 10 | 工作流vs Agent→ReAct→PnE→反思→Agentic Reasoning→多Agent→框架 |
| Multi-Agent/MCP 深化 | 03-.../03-Agent开发/Agent与MCP协议/（12-16） | 5 | 无状态 MCP（2026-07）+ A2A + 工具投毒防御 |
| **RAG 阶段 1-5** | 03-.../04-RAG检索增强生成/ | 45 | 基础概念 → Basic RAG → 进阶调优 → 高级 RAG → 工程化部署 |
| **MCP 与 Agent Skill 体系** | 03-.../06-MCP协议与Agent Skill/ | 50+ | MCP 阶段 1-4 + 核心架构 + 主流框架 + Skill 阶段 1-5 |
| **Python 数据分析全体系** | 03-.../11-Python数据分析/ | 60+ | NumPy/Pandas/可视化/数据来源/实战/业务分析/机器学习/高阶 |
| 爬虫阶段6：爬虫框架 | 03-.../12-Python爬虫/阶段 6：爬虫框架（工程化，写大型爬虫）/ | 10 | Scrapy 2.16 异步现代化 + scrapy-playwright + scrapy-redis 分布式 + 部署监控 |
| Function Calling 函数调用 | 03-.../02-大模型基础与Prompt工程/Function Calling 函数调用【Agent 基石】/ | 11 | Agent 基石：协议对照 + 循环工程 + 工具安全 + prompt caching（2026-08） |
| LiteLLM 多模型适配 | 03-.../02-大模型基础与Prompt工程/LiteLLM 多模型适配/ | 11 | v1.94 + SDK/Proxy 双形态 + 虚拟 key 治理 + Rust 网关迁移（15 倍吞吐） |
| Milvus | 03-.../08-向量数据库/Milvus/ | 10 | 3.0 Lake-Native + 混合检索 + 多模态 |
| 向量数据库 | 03-.../08-向量数据库/向量数据库/ | 10 | 2026 双轨格局 + DiskANN 亿级标配 |
| 部署工具 | 03-.../07-模型部署与工程化/部署工具/ | 12 | 2026 引擎格局（vLLM 默认/TGI 退役） |
| 模型推理与部署 | 03-.../07-模型部署与工程化/模型推理与部署/ | 11 | KV Cache + 投机采样 + FP8 + 弹性 |
| **项目能力建设** | 04-项目能力综合提升/ | 109 | Java-AI L1-L3 + 面试考察点 + 项目避坑 + 实战项目 + 场景题 |

---

## 变更历史

> 完整变更日志见 **[CHANGELOG.md](./CHANGELOG.md)**（Keep a Changelog 格式，自 v1.0.0 独立维护）。

| 版本 / 日期 | 内容 |
|------|------|
| **2026-10-30** | 按简历 + 秋招范围三轮裁剪：主线 2,912 篇，2,979 篇迁入本地归档区（git 忽略，不推送） |
| **v1.0.0（2026-08-12）** | 阶段性开发收官：280 次提交 / 170+ 套知识体系 / 3,910 → 5,752 篇（+1,842），思想体系收官（设计思想/MVC/DDD/LoRA） |
| 2026-08-08 | 5 大知识体系 54 篇 + 目录重组（爬虫阶段 6 / FastAPI / Function Calling / HuggingFace / LiteLLM） |
| 2026-08-06 | 18 个知识体系 153 篇（组成原理四大件 / 数学四套 / JMM / MQ / SQLite / Memcached / Neo4j / Pulsar / 雪花 / JUnit） |
| 2026-08-05 | 14 个知识体系 148 篇（Java 面向对象 / 集合 / 异常 / SDK / 多线程 / Spring 核心 / MyBatisPlus / Milvus + RAG 深化） |

---

## 文档规范

所有 `.md` 文件遵循统一规范：

```md
# 标题
> 一句话定位

## 目录
1. [锚点链接](#section)

## 1. 章节
### 1.1 小节
| 表格 | 对比 |
|------|------|
```

- **论述优先**：机制/取舍/演进逻辑用文字论述（配代码示例）；表格仅用于对比、API 速查、检查清单
- **Code Block 标注语言**：代码围栏必须带语言标识（java / yaml / bash / json / text）
- **Blockquote 提示**：`> **警告**：` `> **提示**：` `> **要点**：`
- **篇幅标准**：知识点讲完即止，不设字数门禁（2026-10 废除原有 2000-2500 门禁）
- **时效性**：所有版本/特性标注来源与时间（如 Spring 7.0 GA 2025-11）
- **交叉引用**：系统间导航（**下一模块** / **返回总览** / 专题交叉）

---

## 快速开始

### 学习路线

| 目标 | 路径 |
|------|------|
| **Java 后端面试** | 01-Java面向对象 → 01-Java集合框架 → 01-Java异常体系 → 01-Java多线程 → 02-Spring框架核心 → 02-MyBatisPlus → 05-题库 |
| **AI 大模型开发** | 03-大模型基础 → 03-Function Calling → 03-Agent 内核与组件 → 03-RAG（阶段 1-5）→ 03-MCP 体系 → 03-部署工具 → 03-模型推理 |
| **RAG 工程** | 03-Prompt 工程 → 03-Embedding → 03-RAG 阶段 1-5 → 03-Milvus → 03-模型推理与部署 |
| **Python 数据链路** | 03-Python 爬虫阶段 1-6 → 03-Python数据分析 阶段 1-5（NumPy/Pandas/可视化/ML）→ 知识库构建 |
| **项目实战** | 04-Java-AI 能力建设 L1-L3 → 04-项目避坑 → 04-项目场景题 |
| **面试冲刺** | 05-面试复习准备 阶段一~四 → 05-题库（152 题）→ 05-参考速查 → 05-简历 |
| **全栈架构师** | 01→02→03→04→05 全路径 |

### 仓库导航

- [底层根基](./01-底层根基-Java核心底座/)
- [中间件与微服务](./02-后端核心技术%20微服务%20分布式%20云原生/)
- [AI 大模型](./03-AI大模型应用开发/)
- [项目能力](./04-项目能力综合提升/)
- [面试冲刺](./05-综合输出-面试冲刺/)

### 项目文档

- [更新日志（CHANGELOG）](./CHANGELOG.md)
- [贡献指南（CONTRIBUTING）](./CONTRIBUTING.md)
- [开发流程（DEVELOPMENT）](./DEVELOPMENT.md)
- [安全策略（SECURITY）](./SECURITY.md)
- [行为准则（CODE OF CONDUCT）](./CODE_OF_CONDUCT.md)

---

## 贡献指南

> 完整流程见 **[CONTRIBUTING.md](./CONTRIBUTING.md)**；安全相关见 **[SECURITY.md](./SECURITY.md)**。

1. **新建知识体系**：`00-总览` + 按序模块（`01-`、`02-`...），篇幅按知识点讲完即止
2. **增强现有文档**：就地补充，保留原有结构与风格
3. **时效性**：写前检索最新官方信息（版本/特性必须标注来源）
4. **规范**：遵循上方文档规范（论述优先/代码块/引用/交叉导航）
5. **提交**：`docs:` / `fix:` 前缀（Conventional Commits），一次提交一个主题
6. **维护模式**：阶段性开发已于 2026-08-12 落幕（280 次提交收官）。当前只接受内容修正（死链/错字/过时信息校准），新知识体系按需创建，不再批量扩建

---

*最后更新：2026-10-30 | 2,912 篇 Markdown 文档 | 5 层金字塔 + 30+ 知识体系 | 维护模式，按需增补*
