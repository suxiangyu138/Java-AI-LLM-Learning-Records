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
| Pulsar | `02-.../03-消息队列/Pulsar/` | 9 | 三层架构→订阅模式→多租户→跨地域复制→分层存储→Functions→Java→选型 |
| 主流 Agent 范式 | `03-AI.../03-Agent与MCP协议/主流 Agent 范式/` | 10 | 工作流vs Agent→五工作流→ReAct→PnE→反思→Agentic Reasoning→多Agent→实践→框架 |
| 雪花算法 | `02-.../08-分布式系统与架构/雪花算法/` | 10 | 原理→时钟回拨→变体→ID方案全景→UUIDv7→基准→Java→选型→生产 |
| JUnit | `02-.../07-工程运维基础/JUnit/` | 10 | 演进史(JUnit 6.1.1)→三平台架构→生命周期→参数化→扩展→Mock→Spring/Boot 4→CI→迁移 |
| 装箱拆箱与泛型擦除 | `01-.../01-Java基础语法与核心特性/装箱拆箱 泛型擦除/` | 10 | 装箱字节码→缓存陷阱→擦除深潜→TypeToken→集合交汇→Valhalla(JEP 401) |
| SDK | `01-.../SDK/` | 8 | 概念辨析→JDK解剖→JPMS/jlink→集成→自研设计 |
| Spring框架核心 | `02-.../Spring框架核心/` | 11 | 容器→DI→生命周期→AOP→事务→事件→配置（7.0） |
| MyBatisPlus | `02-.../MyBatisPlus/` | 10 | 映射→CRUD→Wrapper→插件→生成器→生产（3.5.17） |
| Spring Data Elasticsearch | `02-.../Spring组件汇总/Spring Data 系列【数据访问层】/Spring Data Elasticsearch/` | 9 | 总览→连接配置→映射注解→Repository→Operations→聚合滚动→向量RAG→集成避坑（6.1/ES 9.4） |
| Spring Data JPA | `02-.../Spring组件汇总/Spring Data 系列【数据访问层】/Spring Data JPA/` | 10 | 总览→模块清单→连接配置→实体映射→Repository→Specification→事务锁→性能批量→审计多租户→集成避坑（4.1/Hibernate 7.4/Boot 4.1） |
| Spring Data MongoDB | `02-.../Spring组件汇总/Spring Data 系列【数据访问层】/Spring Data MongoDB/` | 10 | 总览→模块清单→连接配置→文档映射→Repository→模板聚合→批量事务写关注→索引性能→向量检索AI→集成避坑（4.1/MongoDB 8.3） |
| Spring Data R2DBC | `02-.../Spring组件汇总/Spring Data 系列【数据访问层】/Spring Data R2DBC/` | 10 | 总览→模块清单→连接配置→聚合根映射→Repository→DatabaseClient→响应式事务→性能批量→WebFlux集成→集成避坑（4.1/r2dbc-spi 1.0/驱动生态） |
| Spring Data Redis（家族版） | `02-.../Spring组件汇总/Spring Data 系列【数据访问层】/Spring Data Redis/` | 11 | 总览→模块清单(UnifiedJedis)→连接配置→序列化映射→操作API(8.4新命令)→缓存一致性(语义缓存)→锁与Lua→管道事务→响应式→消息Stream→集成避坑（4.1/Redis 8.4；顶层 06-Spring全家桶/Spring Data Redis 另有 11 篇旧版，保持不动） |
| Nacos | `02-.../Spring组件汇总/Spring Cloud 微服务全家桶（分布式）/Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Nacos：注册中心 + 配置中心/` | 10 | 总览→模块清单(版本矩阵)→部署→注册中心→配置中心→一致性协议(AP/CP)→高可用→安全→AI新能力(MCP/Agent)→集成避坑（3.2.3/SCA 2025.0.x） |
| Seata | `02-.../Spring组件汇总/Spring Cloud 微服务全家桶（分布式）/Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Seata：分布式事务/` | 10 | 总览→模块清单(版本矩阵)→部署速查→AT原理(undo_log/全局锁)→TCC→SAGA/XA→模式选型→高可用(存储模式/Raft)→配置注册集成→避坑排错（2.6.0/SCA 2025.1.0.0） |
| Sentinel | `02-.../Spring组件汇总/Spring Cloud 微服务全家桶（分布式）/Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Sentinel：流量控制熔断降级/` | 10 | 总览→模块清单(版本矩阵)→部署速查→限流原理(滑动窗口)→熔断降级→系统保护(BBR)→规则持久化→集群流控→SCA集成→避坑排错（1.8.9/SCA 2025.1.0.0） |
| Spring Cloud Bus | `02-.../Spring组件汇总/Spring Cloud 微服务全家桶（分布式）/Spring Cloud Bus/` | 6 | 总览→模块清单(版本矩阵)→配置刷新速查→事件广播原理→自定义事件→生产选型避坑（4.1.x/Boot 3.x；vs Nacos 推送对比） |
| Spring Cloud CircuitBreaker | `02-.../Spring组件汇总/Spring Cloud 微服务全家桶（分布式）/Spring Cloud CircuitBreaker/` | 9 | 总览→模块清单(版本矩阵)→编程式API→熔断原理(状态机/滑动窗口)→弹性能力(隔离超时重试限流)→注解与降级→配置属性→Framework Retry vs Spring Retry→生产选型避坑（5.0.2/2025.1 Oakwood/Boot 4；SCCB 无官方注解澄清） |
| Spring Cloud Config | `02-.../Spring组件汇总/Spring Cloud 微服务全家桶（分布式）/Spring Cloud Config/` | 9 | 总览→模块清单(版本矩阵)→快速开始(配置读取)→仓库后端全景(Git/Vault/复合)→客户端原理(Config Data两步加载)→动态刷新(Bus集成)→加密解密({cipher}密钥管理)→安全运维→生产选型避坑（5.0.4/2025.1 Oakwood/Boot 4；Jackson 3、vs Nacos 选型） |
| Spring Cloud Gateway | `02-.../Spring组件汇总/Spring Cloud 微服务全家桶（分布式）/Spring Cloud Gateway/` | 11 | 总览→模块清单(双栈拆分/版本矩阵)→快速开始(路由四要素)→谓词工厂→过滤器(上:改写)→过滤器(下:防护/全局)→核心原理(WebFlux链路)→网关限流熔断→高级特性→WebMVC/ProxyExchange(5.0新)→生产选型避坑（5.0.2/2025.1 Oakwood/Boot 4；WebFlux/WebMVC 双栈、starter 与属性前缀变更） |
| Spring Cloud LoadBalancer | `02-.../Spring组件汇总/Spring Cloud 微服务全家桶（分布式）/Spring Cloud LoadBalancer/` | 9 | 总览→模块清单(commons归属/版本矩阵)→快速开始(@LoadBalanced/核心API)→策略全解(9种)→核心原理(Supplier链/响应式)→缓存与健康检查→重试与自定义扩展→API Versioning/HTTP Service Clients(5.0新)→生产选型避坑（5.0.2/2025.1 Oakwood/Boot 4；Ribbon 继任者、vs Nacos 选型） |
| Spring Cloud OpenFeign | `02-.../Spring组件汇总/Spring Cloud 微服务全家桶（分布式）/Spring Cloud OpenFeign/` | 9 | 总览→模块清单(feature-complete/版本矩阵)→快速开始(@FeignClient全属性)→超时重试与HTTP客户端→熔断降级集成→拦截器日志压缩→核心原理(代理生成/调用链)→迁移到Spring HTTP Service Clients→生产选型避坑（5.0.2/2025.1 Oakwood/Boot 4；官方降级为兼容适配器、@HttpExchange 新方向） |
| Spring Cloud Stream | `02-.../Spring组件汇总/Spring Cloud 微服务全家桶（分布式）/Spring Cloud Stream/` | 11 | 总览→模块清单(版本矩阵)→核心模型(Binder抽象/Destination/Binding/Group/Partition)→函数式模型(Supplier/Function/Consumer)→快速开始(双Binder)→绑定配置全解→Kafka Binder深入→RabbitMQ Binder深入→重试死信(max-attempts陷阱/两策略/DLQ)→StreamBridge事件路由→生产避坑（5.0.2/2025.1 Oakwood/Boot 4；函数式模型唯一、@EnableBinding 已移除） |
| Spring AI | `02-.../06-Spring全家桶/Spring AI/` | 11 | 总览→模块清单(版本矩阵/2.0破坏性变更)→核心架构与ChatClient(Advisor链/递归)→模型接入与结构化输出(7大provider/entity校验)→工具调用与Agent(ToolCallingAdvisor/按需发现)→快速开始(三连Demo)→MCP集成(@McpTool/双角色/Streamable HTTP)→RAG与向量库(双Advisor/重排)→文档ETL(分块策略)→记忆与Advisor链→可观测评估与避坑（2.0.0 GA 2026-06-12/Boot 4.0-4.1/Jackson 3；ChatClient主API、工具循环统一、SSE弃用） |
| 高级进阶与源码学习 | `02-.../06-Spring全家桶/高级进阶 & 源码学习/` | 11 | 总览(定位:方法论+地图+跨框架,与深度剖析互补)→源码阅读方法论与工具链(五步法/IDEA断点/Arthas/JFR)→Spring容器源码架构地图(BeanFactory层级/Context体系/refresh十二步)→扩展机制与设计模式源码全景(@Import/SPI/.imports/模式落点)→并发与异步源码(@Async链路/线程池/WebFlux)→Spring Security 7源码导读(过滤器链/认证授权/MFA/Passkey)→数据访问源码(MyBatis MapperProxy/Spring Data代理)→Spring Cloud源码导读(Nacos/Feign/Gateway/Stream四入口)→动态代理与字节码深潜(JDK/CGLIB/FastClass/失效根因)→源码实战演练(三案例断点走读)→源码面试题集锦(30题+答题范式)（Framework 7/Boot 4/Security 7/Cloud 2025.1 基准；与Spring生态深度剖析机制层互补） |
| GitHub License | `02-.../05-开发流程工具/GitHub/GitHub License/` | 7 | 总览→许可证谱系与选型(宽松→强著佐权→非OSI/choosealicense三问)→核心条款深读(许可/条件/限制三要素/MIT/Apache/GPL/AGPL)→GitHub License机制全解(Licensee检测/命名规则/UI流程/三端点API/检测局限)→开源合规与依赖管理(Dependency Review门禁/Dependabot边界/SCA对比/SBOM)→兼容性与商用避坑(GPL传染边界/AGPL网络条款/SSPL/高频裁决表)→实操速查与FAQ(Licensee CLI/API/10问/面试题)（2026-08：Licensee 98%置信度、License API v3、GitHub OSPO合规产品2026-07推出、Dependabot不做许可合规） |
| Git 知识体系 | `02-.../05-开发流程工具/Git/` | 13（另有 `_archive-v1/` 存档旧版 31 篇） | 总览→核心原理与对象模型(四对象/内容寻址/三区/DAG/packfile)→基础操作与入门(安装/.gitignore/常用命令/SpringBoot实战)→分支模型与团队工作流(GitFlow/GitHub Flow/GitLab Flow/Trunk/命名规范/hotfix双合并/worktree)→合并与变基(三策略/rebase -i/cherry-pick/三方合并/冲突解决/rerere)→撤销与历史重写(reset三态/revert/reflog/原子提交)→远程协作与托管平台(remote/Fork工作流/PR/GitHub-GitLab-Gitee对比/gh CLI/LFS)→日常管理与工作区实践(每日节奏/add -p/stash进阶/log高级/bisect/GC)→钩子自动化与定制(Hooks/husky/pre-commit/CI/别名)→标签子模块与Monorepo(tag/SemVer/Releases/submodule/subtree/Maven多模块选型)→调试取证与排错(bisect深潜/blame/worktree/陷阱/应急预案)→配置安全与最佳实践(GPG/SSH签名/filter-repo/分支保护/团队规范模板)→工具链与IDE集成(客户端/VS Code/GitLens/JetBrains/终端工具)（2026-08 重构：31 篇两套重叠体系合并去重为 13 篇，内容零丢失） |
| RAG拓展优化深化 | `03-.../02-RAG检索增强生成/`(12-22) | 11 | 五代演进→GraphRAG→Agentic→长上下文→评估 |
| Multi-Agent/MCP深化 | `03-.../03-Multi-Agent与MCP协议/`(12-16) | 5 | 无状态MCP→A2A→安全攻防→评估→编程Agent |
| Milvus | `03-AI.../Milvus/` | 10 | 数据模型→部署→索引→混合检索→3.0→生态 |
| 向量数据库 | `03-.../04-向量数据库/` | 10 | 索引原理→Chroma/Milvus/FAISS→选型（2026） |
| 部署工具 | `03-.../部署工具/` | 12 | Docker/K8s→vLLM/Ollama/TRT→MLOps→灰度（2026） |
| Docker | `02-.../11-云原生/Docker/` | 12（另有 `_archive-v1/` 存档旧版 18 篇） | 总览→核心概念与架构(容器vsVM/镜像分层/Namespace/Cgroups/OCI)→容器生命周期与常用命令→Dockerfile与镜像构建(指令/多阶段/缓存/安全)→镜像发布与仓库分发(Registry/Harbor/HTTPS)→网络配置与通信(端口映射/自定义网络/容器名通信)→存储卷与数据持久化(绑定挂载/管理卷/tmpfs/共享)→Compose多服务编排(声明式/healthcheck/多环境/生产配置)→安全与资源限制(四大危险/最小权限/非root)→多主机与Swarm(Machine/Stack/高可用/Secrets)→Docker与Kubernetes衔接(三探针/蓝绿/金丝雀/Istio/CI-CD)→应用场景与学习索引(八大场景/实战清单)（2026-08 重构：18 篇两代重叠体系去重为 12 篇，55KB 巨文件 K8s 章节收敛为衔接篇并交叉引用同级 Kubernetes 体系） |
| Docker Compose（深化） | `02-.../11-云原生/Docker/Docker Compose/` | 7 | 总览(与上级07的入门/深化分工)→Compose Spec文件规范全解(顶层键/depends_on三条件/profiles/extends/include/多文件合并)→网络与卷高级配置(networks/volumes字段/external/三种挂载长语法)→服务生命周期与依赖管理(up-down语义/重建规则/healthcheck五参数/restart)→多环境与配置管理(.env插值/-f合并/configs-secrets/日志轮转)→开发工作流与Compose Watch(sync-rebuild-sync+restart三action/dev分离/Java热重载)→生产实践与故障排查(12项清单/Compose Bridge转K8s/三板斧/面试题)（2026-08：Compose v5、version字段废弃、watch v2.22+ GA、CVE-2025-62725 已修复） |
| 模型推理与部署 | `03-.../模型推理与部署/` | 11 | KV Cache→投机采样→量化→压测→成本 |
| 深度学习 | `03-.../深度学习/` | 11 | CNN/RNN/GAN→迁移→压缩→框架（LLM视角） |
| Agent开发 | `03-AI.../Agent开发/` | 12 | P-A-M-E→patterns→framework→MCP→eval→prod |
| Vibe Coding | `03-AI.../Vibe Coding/` | 10 | Paradigm→tools→workflow→context→risk |
| Harness Engineering | `03-AI.../Harness Engineering/` | 8 | Agent=Model+Harness→security→governance |
| Python虚拟环境 | `03-AI.../Python虚拟环境/` | 7 | PEP 405→venv/conda/uv→lock→CI/Docker |
| DeepSeek (V4 Pro) | `03-AI.../DeepSeek/` | 10 | Full evolution V2→V4 Pro |
| Java 后端名词剖析 | `02-.../Java后端开发名词剖析/` | 11 | 300+ concepts, 10 domains |
| 后端分布式名词通俗解释 | `02-.../09-Web开发全流程/后端 分布式常用名词通俗解释/` | 7 | 总览→解耦异步→限流削峰背压→熔断降级雪崩→幂等防抖→缓存分片→电商全链路串讲（大白话+面试对比） |
| Postman | `02-.../Postman/` | 8 | API testing full workflow |
| JMeter | `02-.../Jmeter/` | 8 | Performance testing: Sampler→CI/CD |
| Ubuntu | `02-.../Ubuntu/` | 7 | CLI→Java env→systemd→Shell |
| CentOS | `02-.../CentOS/` | 5 | dnf→SELinux→firewalld→Production |
| Linux | `02-.../运维/Linux 操作系统/Linux/` | 12 | 基础→命令→系统管理→网络安全→服务→速查→服务器→权限→JVM排查→日志（Rocky 10/Ubuntu 26.04） |
| Shell | `02-.../运维/Linux 操作系统/Shell/` | 6 | 入门→变量→流程→函数→文本处理（三剑客） |
| 官方文档精读清单 | `02-.../07-工程运维基础/开发/后端 & AI 方向必啃官方文档清单/` | 6 | 总览(方法论七条/校招在职双轨/原稿剖析)→Java生态与JVM(JDK API/JLS/JVMS/JEP/Spring 7/Boot 4/AI 2/JUnit)→中间件(MySQL 8.4 LTS/Redis 8/Kafka 4 KRaft/RocketMQ 5/Rabbit 4/ES 9/Nacos 3)→AI大模型(OpenAI兼容协议/DeepSeek/Qwen/LangChain v1/LangChain4j/Milvus/部署推理)→工具工程化(Maven 4/Git Book/Docker/K8s/Linux man/可观测性)→协议基础(MDN/RFC 9110-9114/TLS 1.3)（2026-08 检索校准） |
| 项目全流程 | `02-.../项目从开始开发到上线全流程/` | 6 | Requirements→Launch→Ops |
| Vim | `01-.../Vim/` | 4 | Basic→Advanced→Plugins |
| 文件后缀名 | `01-.../不同文件的后缀名/` | 5 | 100+ file formats |
| CPU | `01-.../计算机组成原理/CPU/` | 8 | 组成→流水线→超标量→实例→封装→评测→AI演进 |
| GPU | `01-.../计算机组成原理/GPU/` | 8 | SIMT→存储→调度→CUDA→NVIDIA演进→AMD→AI时代 |
| 缓存与Cache | `01-.../计算机组成原理/缓存与Cache/` | 8 | 原理→结构→替换→多级→一致性→安全→前沿 |
| 寄存器 | `01-.../计算机组成原理/寄存器/` | 9 | 本质→ISA→系统寄存器→ABI→重命名→切换→分配→前沿 |
| 离散数学 | `01-.../数学基础/离散数学/` | 9 | 逻辑→集合→图论→组合→代数→数论→AI应用→Java实战 |
| 初等数论 | `01-.../数学基础/初等数论/` | 9 | 整除素数→同余→欧几里得→CRT→原根→互反律→反演→后量子 |
| 数据结构与算法 | `01-.../06-数据结构与算法/` | 33 主题 | 四阶段路线：前置基础→基础思想→高频专题→中高级（73 目录归并） |
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
