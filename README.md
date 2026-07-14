# Java后端开发 + AI大模型应用开发 学习笔记

> 面向 **Java后端开发工程师** 与 **AI大模型应用开发工程师** 的系统化学习仓库，按 **分层金字塔知识体系（5层）** 组织。
> **技术栈 = Java后端底座 + 中间件工程能力 + AI大模型垂直专项 + 实战项目落地 + 面试冲刺**

---

## 分层金字塔知识体系总览

```
Java-AI-LLM-Learning-Records/
│
├── 01-底层根基-Java核心底座/           # 地基：Java核心 + JUC + JVM + 计算机基础
│   ├── 01-Java基础语法与核心特性/       # 数据类型、泛型、集合源码、IO/NIO、异常/日志
│   ├── 02-JUC高并发编程/               # volatile、synchronized、AQS、线程池、CompletableFuture
│   ├── 03-JVM完整底层/                 # 类加载、内存模型、GC(CMS/G1/ZGC)、调优工具
│   ├── 04-设计模式/                    # GoF 23种 + 设计原则 + DDD
│   ├── 05-计算机基础/                  # OS、网络、组成原理、数学基础、分布式理论
│   ├── 06-数据结构与算法/              # LeetCode Hot100、算法导论、图论、贪心/回溯
│   ├── 07-Java后端技术全览/            # Java后端15篇系列文档 + 项目就业指南
│   └── 08-高并发与性能优化/            # 高并发设计、池化技术、异步编程
│
├── 02-中间件与微服务工程/              # 骨架：数据库 + 缓存 + MQ + ES + Spring + 运维
│   ├── 01-MySQL与MyBatis持久层/        # InnoDB、B+树、MVCC、SQL调优 + MyBatis/MyBatis-Plus
│   ├── 02-Redis缓存层/                 # 五大数据结构、持久化、缓存三问题、分布式锁、Lua
│   ├── 03-RabbitMQ消息队列/            # 交换机、ACK、死信/延迟队列、消息可靠性
│   ├── 04-Elasticsearch检索引擎/       # 倒排索引、DSL查询、聚合、深度分页
│   ├── 05-MongoDB/                     # CRUD、索引、聚合管道、副本集
│   ├── 06-Spring全家桶与微服务/        # IoC/AOP、SpringBoot、SpringMVC、Spring Cloud Alibaba
│   ├── 07-工程运维基础/                # Docker、Nginx、Linux、Git、Maven、K8s、监控
│   └── 08-分布式系统与架构/            # CAP/BASE、分布式事务/锁/ID、服务治理
│
├── 03-AI大模型垂直专项/                # 差异化竞争力：RAG + Agent + 网关 + 框架
│   ├── 01-大模型基础与Prompt工程/      # Token/上下文、CoT、Few-shot、Function Calling、NL2SQL
│   ├── 02-RAG检索增强生成/             # 数据预处理→切片→Embedding→Milvus→多路召回→重排序→生成
│   ├── 03-Multi-Agent与MCP协议/        # ReAct循环、Planner/Skill/Memory、YAML热加载、MCP
│   ├── 04-向量数据库与Embedding/       # Milvus、Chroma、FAISS、Embedding模型选型
│   ├── 05-模型部署与工程化/            # LLM API、Ollama本地部署、容灾降级、Sentinel限流
│   ├── 06-开发框架-LangChain4j-SpringAI/ # LangChain4j、Spring AI、LlamaIndex
│   ├── 07-Python与数据处理/            # Python基础、爬虫、数据分析（NumPy/Pandas）
│   ├── 08-模型微调与多模态/            # LoRA/QLoRA、PEFT、多模态大模型
│   ├── 09-AI开发工具与面试/            # Claude Code、Cursor、AI Coding、AI面试指南
│   └── 10-综合指南/                    # Transformer架构、模型选型、学习路径
│
├── 04-实战项目综合落地/                # 面试核心表达载体（3大项目独立复盘）
│   ├── 01-FlavorDash-AI外卖平台/       # 单体SpringBoot + AI六大模块 + 多模型网关 + SSE流式
│   ├── 02-SuGuangMall-AI微服务电商/    # SpringCloud Alibaba + Multi-Agent + 秒杀零超卖
│   ├── 03-LingShu-医疗AI问答平台/      # 14步RAG管线 + ReAct Agent + 多层安全护栏
│   └── 04-必做项目清单/                # 40+技术栈练习项目清单（入门→进阶→企业级）
│
└── 05-综合输出-面试冲刺/               # 面试冲刺：题库 + 手写代码 + 简历 + 速查
    ├── 01-面试题库/                    # 152题全量题库（含标准答案 + PDF版本）
    ├── 02-手写代码题库/                # HashMap/线程池/分布式锁/SSE/RAG/Agent手写
    ├── 03-简历与职业发展/              # 多版本简历 + 个人发展笔记 + 职业规划
    ├── 04-参考速查/                    # 核心知识点速查手册 + AI/数学名词图谱
    └── 05-学习路线与能力图谱/          # Java后端分级能力图谱 + AI大模型学习路线
```

---

## 知识体系设计理念

```
         ┌──────────────────────┐
         │  05-面试冲刺（输出）   │  ← 面试表达、手写代码、简历
         ├──────────────────────┤
         │  04-项目落地（证明）   │  ← 三大项目深度复盘
         ├──────────────────────┤
         │  03-AI专项（差异）    │  ← 简历核心竞争力
         ├──────────────────────┤
         │  02-中间件（骨架）    │  ← 业务通用工程能力
         ├──────────────────────┤
         │  01-Java根基（地基）  │  ← 所有技术的底层支撑
         └──────────────────────┘
```

**复习策略**：自底向上逐层攻克 → 底层Java是地基，中间件是业务骨架，AI是差异化优势，项目是落地证明，面试冲刺是最终输出。

---

## 三大核心项目

| 项目 | 架构 | 核心亮点 | 位置 |
|------|------|----------|------|
| **Flavor Dash** AI外卖平台 | 单体 SpringBoot | AI 6大模块、多模型网关、SSE流式、91%情感分析 | `04-实战项目综合落地/01-FlavorDash-AI外卖平台/` |
| **SuGuangMall** AI微服务电商 | Spring Cloud Alibaba | Multi-Agent购物管家、RAG导购(召回+40%)、秒杀零超卖 | `04-实战项目综合落地/02-SuGuangMall-AI微服务电商/` |
| **LingShu** 医疗AI问答平台 | RAG + Agent | 14步医疗RAG管线、ReAct医疗Agent、多层安全护栏 | `04-实战项目综合落地/03-LingShu-医疗AI问答平台/` |

---

## 关键技术栈速查

| 层级 | 技术栈 |
|------|--------|
| 语言 | Java 8/17/21、Python 3 |
| 框架 | Spring Boot、Spring Cloud Alibaba、MyBatis-Plus |
| 中间件 | MySQL、Redis、RabbitMQ、Elasticsearch、MongoDB |
| 运维 | Docker、Nginx、Kubernetes、Prometheus、Git |
| AI | LangChain4j、Spring AI、Milvus、Ollama、DeepSeek/通义千问 |
| 方法论 | RAG、ReAct Agent、Multi-Agent、Function Calling、MCP |

---

## 笔记规范

### 单篇笔记模板
```md
# 标题（Java 后端企业级实战版）
> 文档定位、版本、核心场景

## 一、核心概念（是什么）
## 二、原理分析（为什么）
## 三、实战操作（怎么用）
## 四、常见面试题（面试要点）
## 五、易错点/注意事项
## 六、极简总结（背诵版）
```

### 输出要求
- 以 Markdown 为主，保证可直接用于 GitHub 展示
- 每篇笔记回答"是什么、为什么、怎么用、有哪些坑"
- 一知识点一文件，保持粒度一致
- 表格优先（对比、速查），代码示例直接可运行

---

## 使用优势

1. **分层递进，由浅入深**：底层Java是地基，中间件是业务骨架，AI是差异化优势，项目是落地证明，完全贴合简历技术栈
2. **模块化拆分，可自由拆分复习单元**：每天只攻克一层下的1~2个小模块，任务清晰
3. 区分**通用后端能力**和**AI大模型特色能力**，面试时能清晰区分八股和加分亮点
4. 所有知识点完全对齐简历项目，复习过程可以随时绑定业务场景，不会纸上谈兵
5. 顶层结构简单，记忆成本低，能快速在脑海搭建完整知识图谱

---

## 致自己

保持长期主义，拒绝零散学习。

把每一篇笔记都写成未来面试、简历、项目复盘时可以直接复用的资产。
