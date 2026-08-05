# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Repository Purpose

Personal Java backend + AI learning knowledge base. **2,370+ markdown technical documents** organized as a **6-layer pyramid**. All content is documentation (not runnable code). The primary task is creating and maintaining high-quality modular knowledge systems.

---

## Directory Structure (6-Layer Pyramid)

```
01-底层根基-Java核心底座/               # Java core, JUC, JVM, CS fundamentals, Vim
02-后端核心技术 微服务 分布式 云原生/     # Middleware: MySQL, Redis, MQ, ES, Spring, DevOps, tool systems
03-AI大模型应用开发/                    # AI: DeepSeek, RAG, Agent, Embedding, LangChain4j, Spring AI
04-实战项目综合落地/                    # Projects: FlavorDash, SuGuangMall, LingShu
05-综合输出-面试冲刺/                    # Interview: 152 Q&A, hand-written code, resume, cheat sheets
06-课程思路体系搭建/                    # Curriculum: 84+ B站 course outlines organized by topic
```

### Active Knowledge Systems (2026.08)

| System | Location | Files | Focus |
|--------|---------|:---:|------|
| Java面向对象 | `01-.../Java面向对象/` | 11 | 三大特性→接口→内部类→record/sealed→SOLID |
| Java集合框架 | `01-.../Java集合框架/` | 10 | Collection→List/Set/Queue/Map→并发集合→选型 |
| Java异常体系 | `01-.../Java异常体系/` | 10 | 层级→受检之争→try-finally→TWR→错误码→并发 |
| Java多线程 | `01-.../Java多线程/` | 9 | 线程本质→API→三要素→synchronized→死锁→性能 |
| 虚拟线程 | `01-.../02-JUC高并发编程/虚拟线程/` | 10 | 线程模型演进→调度器→pinning(JEP 491)→池迁移→结构化并发(JEP 525)→ScopedValue(JEP 506)→生产实践 |
| JMM | `01-.../03-JVM完整底层/JMM/` | 8 | 三大保证→硬件模型→重排序→Happens-Before→volatile/锁语义→CAS→并发工具→JEP 491 |
| 消息队列理论与实战 | `02-.../03-消息队列/消息队列理论与实战/` | 9 | 核心模型→可靠性→顺序→消费性能→延迟死信→高可用→分布式事务→选型（2025-2026） |
| SQLite | `02-.../01-关系型数据库/SQLite/` | 10 | 架构→类型→SQL优化→WAL事务→JSONB→FTS5→向量搜索→生产实践→Java集成 |
| Memcached | `02-.../02-非关系型数据库/Memcached/` | 9 | 定位→slab内存→协议→一致性哈希→多线程→高可用→Java→选型vs Redis |
| Neo4j | `02-.../02-非关系型数据库/Neo4j/` | 9 | 图模型→Cypher→索引→存储事务→图算法→向量GraphRAG→Java→部署选型 |
| 装箱拆箱与泛型擦除 | `01-.../01-Java基础语法与核心特性/装箱拆箱 泛型擦除/` | 10 | 装箱字节码→缓存陷阱→擦除深潜→TypeToken→集合交汇→Valhalla(JEP 401) |
| SDK | `01-.../SDK/` | 8 | 概念辨析→JDK解剖→JPMS/jlink→集成→自研设计 |
| Spring框架核心 | `02-.../Spring框架核心/` | 11 | 容器→DI→生命周期→AOP→事务→事件→配置（7.0） |
| MyBatisPlus | `02-.../MyBatisPlus/` | 10 | 映射→CRUD→Wrapper→插件→生成器→生产（3.5.17） |
| RAG拓展优化深化 | `03-.../02-RAG检索增强生成/`(12-22) | 11 | 五代演进→GraphRAG→Agentic→长上下文→评估 |
| Multi-Agent/MCP深化 | `03-.../03-Multi-Agent与MCP协议/`(12-16) | 5 | 无状态MCP→A2A→安全攻防→评估→编程Agent |
| Milvus | `03-AI.../Milvus/` | 10 | 数据模型→部署→索引→混合检索→3.0→生态 |
| 向量数据库 | `03-.../04-向量数据库/` | 10 | 索引原理→Chroma/Milvus/FAISS→选型（2026） |
| 部署工具 | `03-.../部署工具/` | 12 | Docker/K8s→vLLM/Ollama/TRT→MLOps→灰度（2026） |
| 模型推理与部署 | `03-.../模型推理与部署/` | 11 | KV Cache→投机采样→量化→压测→成本 |
| 深度学习 | `03-.../深度学习/` | 11 | CNN/RNN/GAN→迁移→压缩→框架（LLM视角） |
| Agent开发 | `03-AI.../Agent开发/` | 12 | P-A-M-E→patterns→framework→MCP→eval→prod |
| Vibe Coding | `03-AI.../Vibe Coding/` | 10 | Paradigm→tools→workflow→context→risk |
| Harness Engineering | `03-AI.../Harness Engineering/` | 8 | Agent=Model+Harness→security→governance |
| Python虚拟环境 | `03-AI.../Python虚拟环境/` | 7 | PEP 405→venv/conda/uv→lock→CI/Docker |
| DeepSeek (V4 Pro) | `03-AI.../DeepSeek/` | 10 | Full evolution V2→V4 Pro |
| Java 后端名词剖析 | `02-.../Java后端开发名词剖析/` | 11 | 300+ concepts, 10 domains |
| Postman | `02-.../Postman/` | 8 | API testing full workflow |
| JMeter | `02-.../Jmeter/` | 8 | Performance testing: Sampler→CI/CD |
| Ubuntu | `02-.../Ubuntu/` | 7 | CLI→Java env→systemd→Shell |
| CentOS | `02-.../CentOS/` | 5 | dnf→SELinux→firewalld→Production |
| 项目全流程 | `02-.../项目从开始开发到上线全流程/` | 6 | Requirements→Launch→Ops |
| Vim | `01-.../Vim/` | 4 | Basic→Advanced→Plugins |
| 文件后缀名 | `01-.../不同文件的后缀名/` | 5 | 100+ file formats |
| CPU | `01-.../计算机组成原理/CPU/` | 8 | 组成→流水线→超标量→实例→封装→评测→AI演进 |
| GPU | `01-.../计算机组成原理/GPU/` | 8 | SIMT→存储→调度→CUDA→NVIDIA演进→AMD→AI时代 |
| 缓存与Cache | `01-.../计算机组成原理/缓存与Cache/` | 8 | 原理→结构→替换→多级→一致性→安全→前沿 |
| 寄存器 | `01-.../计算机组成原理/寄存器/` | 9 | 本质→ISA→系统寄存器→ABI→重命名→切换→分配→前沿 |
| 离散数学 | `01-.../数学基础/离散数学/` | 9 | 逻辑→集合→图论→组合→代数→数论→AI应用→Java实战 |
| 初等数论 | `01-.../数学基础/初等数论/` | 9 | 整除素数→同余→欧几里得→CRT→原根→互反律→反演→后量子 |
| 信息论 | `01-.../数学基础/信息论/` | 9 | 熵→互信息KL→信道容量→压缩→纠错→率失真→ML→6G语义 |
| 密码学 | `01-.../数学基础/密码学/` | 9 | 基础→AES→流密码→哈希→RSA/ECC→PKI→协议→后量子迁移 |
| 计算机数学基础 | `01-.../数学基础/计算机数学基础/` | 13 | 线代/微积分/概率/离散/数值/数论/信息论/优化速查体系 |

---

## Core Task: Creating Knowledge Systems

When user points to an empty directory (via `& 'path'`), build a comprehensive multi-file knowledge system.

### Overview File Requirements (`00-xxx总览.md`)

```md
# Title
> One-line positioning

## 📚 目录
1. [知识体系导图](#1)
2. [模块导航](#2)
3. [学习路线推荐](#3)
4. [核心概念速查](#4)

## 1. 知识体系导图
(ASCII tree showing module hierarchy)

## 2. 模块导航
| 序号 | 模块 | 核心内容 | 适合人群 |
## 3. 学习路线推荐
(2-3 paths: beginner/intermediate/advanced)
## 4. 核心概念速查
(quick reference table)
```

### Sub-module Requirements (`01-xxx.md`)

```md
# Title
> Positioning

## 📚 目录
1. [Section Name](#anchor)

## 1. Section Name
### 1.1 Subsection
(content with tables, code blocks)

> 🎯 **核心要点**：(key takeaway)

---

**下一模块**：[link] / **返回总览**：[link]
```

---

## Documentation Standards

- `> 一句话定位` blockquote under `# Title`
- **Tables preferred** for comparisons, API references, feature lists
- **Code blocks** MUST specify language: ` ```java ` ` ```yaml ` ` ```bash ` ` ```text ` ` ```json `
- Blockquotes: `> ⚠️` warning, `> 💡` tip, `> 🎯` summary
- Chinese content with English technical terms
- Anchor links in TOC must match section headers exactly
- Cross-references at file end: `**下一模块：**` or `**返回总览：**`

---

## File Naming

- `00-` prefix for overview files
- `01-`, `02-`... for sequential modules
- Chinese for topic names, English for well-known terms (JVM, Redis, Spring)
- Course outlines: `NN-EnglishName.md`

---

## Key Patterns

- **Documentation repo** — no build, test, or lint commands
- **Chinese technical content** with English code/API names
- `.txt` files are raw source materials to convert to `.md`
- `06-课程思路体系搭建/` is a curriculum indexing system (84+ course syllabi)
- Existing `.md` files are canonical; `.txt` files in same directory are source drafts
- When building new systems, prefer **fewer but richer files** over many thin ones
- When user says "太少了"/"不对", it means deepen the content or fix structure
