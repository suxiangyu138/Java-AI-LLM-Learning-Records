# 更新日志（CHANGELOG）

> 本文件遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)（SemVer）。
> 变更历史原记录于 README，自 v1.0.0 起迁移至本文件独立维护。

---

## [Unreleased]

### 维护模式（2026-08-12 起）

- 批量扩建阶段落幕，进入维护模式：只接受内容修正（死链/错字/过时信息校准），新知识体系按需创建
- CI 门禁企业级升级：actionlint 工作流自检 + lockfile 锁定 markdownlint（PR 验证用临时条目，验证后回滚）

---

## [1.0.0] - 2026-08-12

> 🏁 **阶段性开发收官版本**：280 次提交 / 170+ 套知识体系 / 3,910 → **5,752 篇**（+1,842 篇）
> 知识库目录最终形态：5 层金字塔（01=1,826 / 02=1,266 / 03=2,044 / 04=340 / 05=276）

### 新增：思想体系收官（44 篇）

- `01-.../10-计算机设计思想与架构工程能力/`：软件工程上的设计思想（28 条思想）、MVC 架构模式、DDD 领域驱动设计
- `03-.../09-模型微调与多模态/LoRA深度剖析/`：LoRA 原理数学→QLoRA→PEFT 实战→RLVR 实证（DoRA 46.6%）

### 新增：硬件专题 + 项目能力建设（~30 套体系）

- `01-.../05-计算机基础/计算机硬件方向系统学习清单/`：GPU 硬件架构 / NPU 概念 / PCIe 带宽 / RISC-V / 关键硬件-软件映射 / 微机原理 x86 汇编（8086→x86-64）/ 内存墙 / 量化背后硬件约束 / 凸优化 / 最优化理论
- `04-项目能力综合提升/` 重建（~130 篇）：Java-AI 后端能力建设 L1-L3（33）+ Python-LLM 能力建设 L1-L3 与工程硬性要求（44）+ 面试考察点 / 双栈联合实战 / 项目避坑（各 11）
- `05-.../面试复习准备/`：阶段一~四（基础复盘→AI 专项攻坚→项目+简历打磨→模拟面试）
- 专题 7-9：分布式高并发与限流熔断降级 / 架构层面优化与全链路压测 / 故障复盘与真实线上案例

### 新增：Python 数据分析 + 微调多模态 + 工具库 + Web 工程（~90 套体系）

- `03-.../11-Python数据分析/`：阶段 1-5 全体系（前置基础/NumPy/Pandas/Matplotlib·Seaborn/数据来源/实战/业务分析/机器学习/高阶）+ 爬虫阶段 7 进阶高级内容
- `03-.../09-模型微调与多模态/`：微调体系（基础概念/数据集/工程流程/超参显存/常见问题/PEFT）+ 多模态体系（基础/数据集/微调/RAG/缺陷/推理部署/LLaVA）+ 五模块课程导航
- Python 工具库 18 套：loguru / PyYAML / unstructured / milvus-python / peft / chromadb / faiss-cpu / transformers / openai python-sdk / pydantic v2 / pypdf·python-docx / aiohttp / trl / bitsandbytes / uvicorn / vLLM
- 向量库：Chroma / PgVector / Qdrant / Redis Stack
- Web 与工程：RESTful API / Token / Swagger / Knife4j / WebSocket / 浏览器内核 / 小程序 UniAPP / 飞书 / Gitee / Gradle / SonarQube
- 性能专题：性能理论基础 / Java 单机并发编程 / JVM 性能调优 / 数据库性能优化 / 缓存体系专题 / 中间件 & IO 优化
- 软件架构能力：L1 初级→L2 中级→L3 高级→L4 架构师 + 横向通用能力

### 新增：Agent 内核 + RAG 阶段 + MCP 全体系（~90 套体系）

- Agent 内核：什么是 LLM Agent / 区分概念 / 经典 Agent 范式 / 四大核心组件（11 篇）+ 组件速记 4 套
- Agent 子组件专项 20+ 套：Memory / Planner / Output Parser / Reflection / Tool 开发注册 / 安全护栏 / 感知 / 观测可观测 / Feedback 评估 / Orchestrator Runtime / Multi-Agent 协作 / Prompt 角色配置 / 文档解析 / 数据库交互 / 联网搜索 / 代码执行沙盒 / 前端演示界面 / 新兴 SDK / 多 Agent 框架 / 低代码平台
- Agent 面试专项：四大核心组件（面试必背）/ Agent 的核心缺陷 / Function-call 完整理论流程
- Coding Agent：Reasonix（DeepSeek）/ ZCode（智谱）/ OpenAI Agents SDK
- RAG 阶段 1-5（45 篇）：基础概念→Basic RAG→进阶调优→高级 RAG→工程化部署
- RAG 框架：LangChain / LlamaIndex / Haystack / AutoGen / Pytorch AI 应用开发
- MCP 体系：阶段 1-4 / 核心架构（CS 客户端-服务器）/ 主流 MCP 开发框架 / Agent Skill 阶段 1-5
- 硬件基础：操作系统硬件关联 / 计算机网络硬件基础
- 规范升级：MarkdownDocGenerator 技能 v1.2→v1.4、篇幅统一 2000 字、表格策略修订（Prose-first）

### 关键时效性（全部检索校准至 2026-08）

- 2026 标志性实践：Idempotency-Key 幂等键（Stripe 风格 TTL 24h）、RFC 9457 Problem Details、OpenAPI 3.1 契约优先、URL 路径版本化
- MCP 无状态规范（2026-07-28）/ A2A 协议 / DeepSeek V4（2026-07-24）
- Transformers 5.0（2026-01-27）/ PEFT 0.19.1 / QLoRA 单卡微调标准
- RLVR 实证：DoRA 46.6% > 全参 44.9% > LoRA 42.5%
- 内存墙：HBM4 2.0-2.8TB/s、CXL 4.0 机架池化；量化：B300 FP4 15PF
- SOLID 2026 学术实证 / 模块化单体+右规模服务为推荐起点（DDD）

---

## 历史更新（v1.0.0 之前，记录于 README 时代）

### 2026-08-08 — 5 大知识体系 + 目录重组（54 篇）

- `12-Python爬虫/阶段 6：爬虫框架`（10 篇）：Scrapy 2.16.0 / scrapy-playwright 0.0.48 / scrapy-redis 分布式 / scrapyd 部署
- `01-Python语言/Python 异步 + FastAPI`（11 篇）：FastAPI 0.139 / Starlette 1.0 / Uvicorn 0.51 / Python 3.14 asyncio
- `Function Calling 函数调用【Agent 基石】`（11 篇）：协议对照 + 循环工程 + 工具安全 + prompt caching
- `HuggingFace`（11 篇）：Transformers 5.0 重构 + Hub 生态 + QLoRA + Gradio/Spaces
- `LiteLLM 多模型适配`（11 篇）：v1.94 + Rust 网关迁移 + 虚拟 key 治理
- 目录重组：Python 爬虫/数据分析 → 03 新编号体系（01-13），153 旧路径迁移

### 2026-08-06 — 18 个知识体系（153 篇）

- 计算机组成原理四大件：CPU（8）/ GPU（8）/ 缓存与Cache（8）/ 寄存器（9）
- 数学基础四套：离散数学（9）/ 初等数论（9）/ 信息论（9）/ 密码学（9）
- JMM（8）/ 消息队列理论与实战（9）/ SQLite（10）/ Memcached（9）/ Neo4j（9）/ Pulsar（9）
- 主流 Agent 范式（10）/ 雪花算法（10）/ JUnit（10）
- 修正：计算机数学基础 13 篇全面修正（编号/链接/公式）

### 2026-08-05 — 14 个知识体系（148 篇）

- 新建 77 篇：Java 面向对象（11）/ Java 集合框架（10）/ Java 异常体系（10）/ SDK（8）/ Java 多线程（9）/ Spring 框架核心（11）/ MyBatisPlus（10）/ Milvus（10）
- 增强 71 篇：RAG 拓展优化深化（+11）/ Multi-Agent 与 MCP 深化（+5）/ 向量数据库（+10）/ 部署工具（+12）/ 模型推理与部署（+11）/ 深度学习（+11）
- 关键时效：Spring Framework 7.0 / Boot 4（2025-11）、Milvus 3.0（2026-07）、JDK 25/26

---

## 版本约定

- **v1.0.0**（2026-08-12）：阶段性开发收官，5,752 篇全量沉淀
- 维护模式下的版本节奏：内容修正累积达到一定量或结构重大调整时，递增 minor 版本

[1.0.0]: https://github.com/suxiangyu138/Java-AI-LLM-Learning-Records/releases/tag/v1.0.0
