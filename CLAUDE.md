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
| MinIO | `02-.../01-关系型数据库/MinIO/` | 8 | 总览(定位:自托管S3对象存储,文件归MinIO-元数据归DB-查询归应用)→对象存储核心概念与S3模型(桶与对象/键扁平命名空间/凭证/版本控制/生命周期/S3 API全景/思维转换)→架构原理纠删码与数据保护(副本vs纠删码1.4-2倍/Reed-Solomon/8+8容错/bit rot/healing/分布式无主/站点复制异步镜像非共识/小文件放大陷阱)→部署与运维(单机-分布式-Docker-Operator/mc命令速查/Prometheus三探针/三层备份)→权限与安全(IAM风格Policy/服务账号最小权限/预签名URL/TLS/SSE加密/2026安全基线)→Java集成(MinioClient/上传全形态/分片上传5MB-10000片/流式下载/预签名/版本操作/云OSS零改动切换)→后端架构配合(应用转发vs预签名直传/元数据分离file_meta表/下载链路私有默认公开显式/图片处理/CDN配合)→生产实践与故障排查(容量规划公式/小文件15倍放大/滚动升级/故障速查表/性能/恢复演练/Checklist)→选型对比与面试题(vs云OSS成本/vs Ceph/MinIO不是NoSQL红线/AGPL与维护模式风险/决策树/10面试)（2026-08：RELEASE.2026-04服务器与mc RELEASE.2026-04-21基准；企业版AIStor；CVE-2026-34204已修复） |
| Memcached | `02-.../02-非关系型数据库/Memcached/` | 9 | 定位→slab内存→协议→一致性哈希→多线程→高可用→Java→选型vs Redis |
| Neo4j | `02-.../02-非关系型数据库/Neo4j/` | 9 | 图模型→Cypher→索引→存储事务→图算法→向量GraphRAG→Java→部署选型 |
| Redis Lua脚本 | `02-.../02-非关系型数据库/Redis/Lua脚本/` | 8 | 总览(定位:Redis服务端编程体系,与Redis主体系08集成篇分工)→Lua语言基础(5.1速成/类型/table/函数/控制流/cjson/速查)→脚本机制与命令族(EVAL/EVALSHA/SCRIPT/KEYS与ARGV铁律/call vs pcall/缓存/NOSCRIPT/返回转换)→原子性与执行语义(单线程原子/脚本vs事务/无回滚/沙箱/2026加固:全局函数禁止-print移除-LRU500-内存限制/超时与SCRIPT KILL/复制确定性)→管道vs事务vs脚本vs函数(四机制对比/选型决策树/组合姿势)→Redis Function机制(7.0+官方推荐/库与注册/FCALL命令族/vs EVAL全面对比/版本化热更新/no-writes/迁移四步/局限)→经典脚本实战(分布式锁Redisson思路/固定窗口/滑动窗口/令牌桶/扣库存/防重幂等)→Spring Data Redis集成(DefaultRedisScript/ResultType/execute/序列化陷阱/NOSCRIPT/FCALL/生产规范)→性能调试与生产实践(性能真相/红线与优化/调试三板斧/慢脚本治理/12避坑/10面试)（2026-08：Redis 8.10/Lua 5.1基准；Functions为官方推荐、EVAL仅一次性场景；沙箱加固适用7.0+） |
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
| 爬虫阶段1：前置基础 | `03-AI.../12-Python爬虫/阶段 1：前置基础（必备）/` | 9 | 总览(定位:学习路径第一站,与12篇主体系课本vs字典分工)→Python环境与爬虫工具箱(3.14.6/venv-uv/requests-bs4-lxml三件套/curl探针)→HTTP协议基础爬虫视角(对话本质/请求响应/状态码判定字典403-429/Headers/UA/Cookie与Session/HTTPS证书CA信任库)→网页三件套与DOM(HTML骨架/CSS皮肤/JS肌肉/数据藏在哪层/内嵌JSON宝矿/稳定锚点)→选择器与定位技术(CSS全解/XPath谓词与轴/文本匹配独门/Chrome生成与泛化)→数据格式与编码(JSON主角/XML遗留/UTF-8与GBK/乱码四步法/HTML实体与xa0)→正则表达式基础(元字符/分组捕获/贪婪vs懒惰/re模块/高频模板/正则vs解析库分工)→浏览器开发者工具实战(F12四板斧/Network找接口/Console验证/Application看Cookie/定位接口五步法/curl转requests)→爬虫法律与伦理(robots.txt/个保法数据安全法反不正当竞争/技术合规四原则/礼貌爬虫三不/合规自检10问)→阶段实战与自测(综合实战/接口直取/20道自测题/毕业检查单)（2026-08：Python 3.14.6基准、3.15将于2026-10发布PEP 686 UTF-8默认、requests 2.34） |
| 爬虫阶段2：静态网页爬虫 | `03-AI.../12-Python爬虫/阶段 2：静态网页爬虫（入门）/` | 9 | 总览(定位:请求解析存储最小闭环,毕业=独立写完整爬虫)→Requests库实战(2.34.2/GET-POST-params-json-files/Session会话/响应三件套/流式下载/工程习惯)→请求进阶异常重试代理(异常体系/raise_for_status必须手动/超时(3,15)分离/指数退避重试规则/HTTPAdapter/代理/verify=CA信任库正确姿势/健壮封装模板)→BeautifulSoup解析实战(lxml后端/对象模型/find族class_坑/select选择器/判空三连/容器先行模式)→lxml与XPath解析(双重身份/etree.HTML/XPath文本定位轴/1开始索引/性能5-10倍/bs4协同)→数据提取与清洗(四步合一/清洗规则xa0全角换行/类型转换to_int-to_date/urljoin补全/去重去噪/结构化输出规范)→翻页与爬取策略(四种分页模式/终止四条件/防循环seen+max_pages/节奏随机间隔/429处理/失败清单与断点/列表详情两层架构)→数据存储入门(CSV utf-8-sig/JSON ensure_ascii/Excel交付/SQLite UNIQUE去重与参数化/存储规范)→综合实战完整爬虫项目(请求解析存储四模块/端到端代码/运行验证/五扩展任务/11项自评)→阶段自测与常见问题(报错排查表/三层定位方法论/20道自测/毕业检查单)（2026-08：requests 2.34.2(2026-05-14)弃3.9/内联类型/CVE-2026-25645已修复） |
| 爬虫阶段3：数据存储 | `03-AI.../12-Python爬虫/阶段 3：数据存储/` | 7 | 总览(定位:文件级升级为数据库级,毕业=数据管道五步)→MySQL连接与表设计(pymysql/utf8mb4必配/连接池DBUtils/爬虫表四件套/类型选型)→MySQL写入与去重(增删改查/executemany批量/三种去重UNIQUE-IGNORE-ON DUPLICATE选型/写入规范)→MongoDB存储(pymongo/文档嵌套零转换/unique+upsert去重/索引/三场景)→去重与增量策略(四层去重/内容指纹MD5/三种增量策略/任务状态机/增量爬虫骨架)→pandas清洗流水线(DataFrame/read_sql闭环/缺失值/向量化清洗/类型转换/groupby/to_sql回写)→存储选型与生产规范(选型矩阵/索引三原则/建表规范/保留归档分区/巡检清单)→阶段实战与自测(增量爬虫+库+pandas综合项目/20自测)（2026-08：MySQL 8.4/MongoDB 8.x/pandas 2.x基准；与MySQL-MongoDB主体系分工为爬虫视角子集） |
| 爬虫阶段4：反爬基础 | `03-AI.../12-Python爬虫/阶段 4：反爬基础（高频遇到）/` | 8 | 总览(定位:高频三件事403-429-登录+合规边界)→反爬全景与常见手段(五层防线/被哪层拦诊断五步/强度分级)→UA与Headers伪装(UA池/完整浏览器头Sec-Fetch/Referer实测/Cookie一致性/伪装工程规范)→频率控制与限流应对(随机延迟/令牌桶节流/429应对Retry-After/边界探测/频率哲学)→IP代理基础(代理原理/免费代理三坑/付费隧道/requests代理/验证出口IP/简单代理池)→Cookie与登录态(F12登录分析/三种方案/手动Cookie/代码登录三步/验证码应对/会话维护)→请求指纹与风控信号(设备指纹组合/JS检测机制/风控评分/requests短板/站点分级)→合规边界与对抗伦理(对抗升级代价/五个该停判断/法律映射/职业边界/合规替代API优先/10问自查)→阶段实战与自测(反爬工具箱/四场景演练/20自测)（2026-08：合规优先立场,UA-节奏-代理三件套应对中强以下反爬） |
| 爬虫阶段5：动态网页爬虫 | `03-AI.../12-Python爬虫/阶段 5：动态网页爬虫（难点）/` | 8 | 总览(定位:接口直取最优+渲染兜底,先直取后渲染)→动态页面原理与定位(SSR vs CSR/JS渲染链路/四步定位法/策略树)→接口直取最佳实践(F12五步/复刻请求/四种分页/签名加密坑/直取vs渲染切换)→Selenium基础(4.4x/WebDriver/八种定位/基础操作/局限)→Selenium进阶等待与渲染(显式等待WebDriverWait/EC条件/隐式等待/加载更多滚动/渲染模板)→Playwright现代方案(1.60+首选/自动等待/locator/route拦截/Context隔离/模板)→无头浏览器与反检测(headless=new/webdriver标志/stealth补丁/反检测态度)→数据提取与渲染结合(三路对比/渲染解析/响应捕获/混合架构/稳定性四件套)→阶段实战与自测(三路爬虫/20自测)（2026-08：Playwright 1.60+新项目首选/Selenium 4.4x存量维护/selenium-wire已归档/接口直取优先于渲染） |
| 爬虫阶段6：爬虫框架 | `03-AI.../12-Python爬虫/阶段 6：爬虫框架（工程化，写大型爬虫）/` | 10 | 总览(定位:requests脚本→Scrapy框架工程化,与主体系04速查版分工)→Scrapy架构与数据流(五组件/八步流/yield契约/DownloaderAwarePriorityQueue/2.14异步现代化AsyncCrawlerProcess/2.15实验httpx下载器/2.16 Python3.14)→项目结构与Spider开发(脚手架/四种Spider/CrawlSpider规则/回调链meta/errback铁律/async start_requests)→选择器与Item提取(Selector三合一/::text::attr/CSS vs XPath/Item三形态dict-Item-dataclass/ItemAdapter/FEEDS)→ItemPipeline与数据落地(三铁律/校验清洗UPSERT/MySQL连接生命周期/executemany批量/幂等唯一键/ImagesPipeline+2.14 EXIF旋转)→中间件与下载控制(三钩子/顺序规则/UA池/代理池dont_filter铁律/重试Cookie/AutoThrottle/2.15 httpx)→动态页面与反爬实战(scrapy-playwright 0.0.48/配置三件套/混合请求按需渲染/PageMethod/多上下文/storage_state/PLAYWRIGHT_BROWSER_PROVIDER camoufox/内存治理)→分布式爬虫架构(scrapy-redis换两组件/RedisSpider/断点续爬/布隆去重1000万URL 2GB+→200MB/brpop/停维护但稳定/替代Celery-Kafka选型)→部署监控与工程化(scrapyd 1.6.0低活跃/6800不公网/Docker镜像tag回滚/CronJob/StatsCollection指标/监控告警先止血/CI-CD门禁)→生产实战与自测(新闻站端到端项目/四验收/性能调优9项/12避坑/20自测/毕业检查单/面试6题)（2026-08：Scrapy 2.16.0(2026-05-19)/Python 3.10-3.14/Twisted 26.4/scrapy-playwright 0.0.48/scrapy-redis 0.7.3停维护/scrapyd 1.6.0） |
| Python 异步 + FastAPI | `03-AI.../01-Python语言/Python 异步 + FastAPI/` | 11 | 总览(定位:异步深潜+FastAPI全栈,与高级语法07语言层入门/生态05 AI视角分工)→asyncio事件循环与异步核心(单线程轮转模型/协程-Task-Future/await三步骤/协作式取消-shield/超时timeout-wait_for/TaskGroup结构化并发/run-Runner/get_event_loop 3.14 RuntimeError/自由线程一等公民/循环策略弃用/子进程watcher移除)→并发控制与异步设计模式(信号量-锁-Event/令牌桶限流/扇出三件套/背压队列上限/Queue-task_done-join/to_thread桥接/进程池/10大反模式)→异步IO生态实战(httpx首选vs aiohttp/AsyncClient连接池复用/Limits-TimeOut四段/fetch重试指数退避/asyncpg池参数/redis.asyncio nx锁/aiofiles/兼容矩阵/选型顺序)→FastAPI与ASGI架构(0.139基准/0.126-Pydantic v1移除/0.128 pydantic.v1彻底移除/0.130 Rust序列化2倍+/三层架构/ASGI 3.0 scope-receive-send/def线程池vs async def事件循环金科玉律/lifespan唯一推荐/Starlette 1.0 on_event移除)→路由请求与参数校验(参数五源/Field约束/field_validator-model_validator/response_model必写/HTTPException vs 422/Union含str v2陷阱/校验分层)→依赖注入与认证授权(Depends机制/依赖树解析顺序/yield依赖资源生命周期/请求级缓存use_cache/JWT完整实现-OAuth2PasswordBearer/角色-资源授权-IDOR防护/安全基线九条)→异步数据库集成(SQLAlchemy async三件套/create_async_engine五参数/pool_pre_ping/expire_on_commit=False/get_db会话依赖/事务与行锁/N+1-MissingGreenlet/lazy-raise+selectinload/连接池监控/PgBouncer/asyncpg直连选型)→中间件异常处理与可观测性(洋葱模型/CORS白名单-TrustedHost-GZip/追踪ID中间件/BizError统一错误契约/OTel三行接入/结构化JSON日志/健康检查双探针)→生产部署与性能优化(Uvicorn 0.51 SIGHUP重叠热重启/0.50 websockets-sansio/workers-uvloop-httptools/多worker进程模型/Nginx反代四件事/压测方法论wrk-locust-P99/调优12项收益排序/慢请求定位三步/优雅停机发布检查单)→实战项目与面试冲刺(异步商品API端到端/lifespan-依赖-response_model-中间件-探针五层验收/12避坑/20自测/毕业检查单/面试7题)（2026-08：FastAPI 0.139/Pydantic 2.13.4/Starlette 1.1.0(1.0=2026-03-22首稳定版)/Uvicorn 0.51.0(2026-07-08)/Python 3.14(2025-10-07)） |
| Function Calling 函数调用 | `03-AI.../02-大模型基础与Prompt工程/Function Calling 函数调用【Agent 基石】/` | 11 | 总览(定位:Agent基石-决策与执行分离,与LLM-API09速查版分工)→原理与演进(模型不执行函数/职责边界/提示词JSON vs 协议FC/Agent公式-模型+工具+循环/ReAct认知范式vs FC协议层/2026生态-Responses-工具发现-MCP)→API协议全解(tools声明完整形态/tool_calls数组-arguments JSON字符串/回传tool消息-tool_call_id配对/tool_choice四值/parallel_tool_calls三关场景/流式index增量拼接/Anthropic对照-input_schema-tool_use-tool_use_id/Responses API顶层function_call)→调用循环工程(五步循环-追加assistant-判空退出-执行-回传/终止条件-零调用自然出口-8-12轮上限/工具错误转JSON回传消除80%卡死/并行asyncio.gather-有依赖协议层关并行/三层超时重试/每轮留痕可观测)→工具定义与设计规范(JSON Schema要素-description必写/描述三原则-做什么-何时用-边界/参数即决策负担-可选参数独立成工具/命名动词开头-一工具一职责/工具100-200token-10内最优/降本三件套-缓存-拆分-发现/装饰器注册表)→参数校验与安全(模型输出不可信/三层校验-strict-Pydantic-业务/strict硬规则-additionalProperties-false-全required-null联合-并行下失效/权限最小化-角色过滤/确认-执行模式-一次性令牌/提示注入三防线/全量审计脱敏)→高级模式(并行白拿吞吐/强制调用-路由与提取/结构化输出-强制指定+strict+关并行/工具发现-tool_search-defer_loading/多角色子Agent三赢/上下文管理三件套/模式选型决策树)→流式与长会话(流式组装-index聚合-arguments增量-finish收尾/推理模型先思后调/DeepSeek thinking必回传reasoning_content否则400/工具结果4KB截断-摘要/压缩三方案-窗口-摘要-状态/SSE四阶段体验)→主流模型实现对比(OpenAI事实标准-strict-Responses-工具发现/Anthropic块结构-显式缓存断点/DeepSeek V4-OpenAI兼容-低价-thinking支持FC/国产兼容≠同能力-探针验证/统一适配层-抽象ToolCall/选型-协议认OpenAI-能力看需求-生产必封装)→生产实践与成本优化(成本=轮数×全量上下文×单价/prompt caching-工具前缀稳定-断点-50-90%降本/缓存铁律五条-不增删工具-固定排序-冻结system/监控三件事-钱-卡-错/测试三层-CI无模型-冒烟外围/12避坑)→实战项目与面试冲刺(智能客服Agent端到端-查询-两段式退款-双供应商/七项验收/20自测/毕业检查单/面试8题)（2026-08：OpenAI gpt-5.x tool_search/Claude 5系 defer_loading/DeepSeek V4(2026-07-24 chat-reasoner弃用)/Anthropic缓存读90%折扣/工具定义≈100-200token/轮） |
| HuggingFace 生态 | `03-AI.../02-大模型基础与Prompt工程/HuggingFace/` | 11 | 总览(定位:HF全链路-拿模型-加载-微调-部署-生产,与Python生态04速查版分工)→Hub与模型生态(150万+模型/三仓库model-dataset-space/safetensors安全格式/模型卡四步读/gated许可+token/HF_TOKEN环境变量/镜像hf-mirror-HF_ENDPOINT import前设置/hfd.sh多线程实测70B-4h→22min/HF_HOME缓存治理/上传版本管理commit-branch-tag)→Transformers v5核心机制(v5.0.0 2026-01-27五年首大版本/模块化乐高架构-AttentionInterface/统一Rust分词-Fast-Slow取消-tokenizer.json/WeightConverter动态权重加载-加载即融合-QKV-MoE/量化一等公民-bitsandbytes≥0.46.1/破坏性变更-TF-Flax停更-HF_HOME-apply_chat_template返回BatchEncoding-Trainer默认参数/互操作中枢定位-训练PEFT服务vLLM兜底serve)→分词器与模型加载实战(三件套Config-Tokenizer-Model全Auto类/对话必须AutoModelForCausalLM/device_map-auto+torch_dtype-auto/bf16大模型标准-显存公式参数×字节/4bit NF4单卡70B甜点/多卡分片vs训练accelerate/v5加载即优化fuse_qkv-fuse_moe-attention量化一体/10坑)→推理实战与serve部署(pipeline任务抽象/生成参数三件-max_new_tokens-temperature-top_p/pad-eos必设/apply_chat_template对话标准/v5 transformers serve一条命令OpenAI兼容服务/continuous_batching+paged_attention进标准库/vLLM分工-协议统一引擎切换无感/性能基线TTFT-吞吐)→微调与Trainer(微调金字塔-预训练-SFT-LoRA/数据质量上限-messages三件套/Trainer全流程-等效batch=per_device×累积×卡数/v5参数-eval_strategy-report_to默认none旧参数移除/训练显存6字节每参-大模型默认LoRA/检查点save_total_limit-3+resume/评估内嵌+推送Hub)→PEFT与LoRA微调实战(LoRA原理-W=W+BA冻结底座训补丁/成本全量42GB-2天vs LoRA 20GB-小时vs QLoRA 10GB/超参-r16-alpha32-lr1e-4/QLoRA-4bit NF4底座+bf16适配层单卡微调标准/适配器即产品-热切换-合并merge_and_unload部署/实战案例-验收前后对比+通用能力回测/灾难性遗忘混通用数据)→Datasets与数据流水线(Arrow列式+内存映射100GB可玩/加载五形态JSONL-CSV-Parquet-Hub-流式/map三件套-batched-num_proc-remove_columns/清洗三必做-去重-长度过滤-抽查/流式探索-全量训练/缓存纪律-load_from_cache_file=False)→Gradio与Spaces应用(定位-模型Demo事实标准vs FastAPI生产/Interface最快Demo/Blocks布局+事件+Chatbot对话/流式-生成器yield+TextIteratorStreamer+queue/模型全局加载一次/Spaces三件套-app-requirements-README即上线/FastAPI挂载mount-demo)→推理优化与生产实践(显存公式-推理参数×字节+KV-微调×3/优化顺序-量化-注意力-引擎逐步压测/生产不现下模型-预下载制品库/磁盘三杀手-模型-数据集-检查点/环境版本矩阵-uv+transformers5.x+bitsandbytes≥0.46.1/生产清单十项/12避坑)→面试冲刺(20自测三档/毕业检查单10项/面试8题-范式版本事实-原理-实战参数-选型权衡)（2026-08：Transformers 5.0.0(2026-01-27)/Hub 150万+模型/日安装300万+/hf-mirror镜像/QLoRA单卡微调标准/bitsandbytes≥0.46.1） |
| LiteLLM 多模型适配 | `03-AI.../02-大模型基础与Prompt工程/LiteLLM 多模型适配/` | 11 | 总览(定位:100+供应商一个接口-多模型时代适配层,与OpenAI兼容04网关全景分工)→定位与核心能力(多模型三大痛点-切换-key-成本/双形态SDK接口统一-Proxy治理统一/统一抽象层价值-一次编写到处运行/2026版本线-v1.94.0(2026-07-28)-Router Plugins-成本页-Python3.14/v1.83.14-Prompt compression-Memory API/Rust网关迁移-453→6782 RPS-359MB→32MB-sub1ms开销-配置全兼容)→SDK编程接入(模型名=路由键-供应商前缀省略默认OpenAI/key环境变量前缀大写/completion三形态-同步-异步acompletion-流式/统一参数映射-各家字段自动翻译/FC完整支持-OpenAI格式内部转各家/成本追踪-response_cost-回调/8坑)→模型管理(model_list注册表-逻辑名→物理模型映射/别名按能力命名-cheap-pro-fast-不按供应商/key三规范-环境变量-os.environ引用-零明文/供应商清单-DeepSeek-DashScope-Ollama/自定义模型-openai前缀+api_base接vLLM/SDK与Proxy配置同源-Router↔config.yaml)→路由与负载均衡(Router=模型层负载均衡/策略-simple-shuffle默认打散防限流/fallback三层级-同逻辑名-调用级-Proxy全局/生产标准双供应商/健康机制-num_retries-allowed_fails-cooldown/写操作重试先幂等/Router Plugins v1.94-信号流水线-只缩不扩/三层心智-逻辑名-策略-物理)→成本与上下文(成本失控主因-选贵-重复-黑盒非单价/记账-response_cost+metadata+回调落库/预算三级-团队-用户-key+TPM-RPM/缓存三类型-响应Redis-语义-供应商prompt caching/缓存三纪律-敏感不缓-时效不缓-阈值测试/压缩与Memory API/治理闭环-记账-路由-预算-复盘)→Proxy部署与配置(Proxy=OpenAI兼容端点-调用方改base_url+api_key/config.yaml五块-模型-运行时-安全-路由-guardrails/master_key必须-虚拟key权限核心/虚拟key能力-模型白名单-预算-TPM-RPM-可撤销/安全红线-内网-反代-虚拟key-密钥轮换/团队=预算权限容器-全局>团队>用户>key/管理API-key生命周期-模型健康-花费查询)→Proxy生产实践(生产架构-Nginx+多实例+PG+Redis/FastAPI体系同构/Docker-锁定版本-密钥环境变量-PG数据卷/key表必须备份/OTel一行接入-告警三件套-成本-失败率-缓存命中/数据面控制面分离2026-05-慢管理查询不拖请求/真正HA在供应商fallback/升级三步-release notes-测试-灰度/8坑)→生态集成(三层次-SDK-Python-Proxy-多语言-框架适配器/Proxy接入三配置-base_url-虚拟key-逻辑名/语言无关-OpenAI兼容客户端全接/Java-Node-Go接入模式/LangChain-ChatLiteLLM-LlamaIndex/FastAPI组合-lifespan Router+acompletion+SSE/Agent栈-MCP管工具-LiteLLM管模型-FC管编排)→生产避坑与选型(选型决策树-SDK-Proxy-SaaS-自研/vs One-API-New-API-Python栈LiteLLM-重UI管理One-API/vs OpenRouter-数据合规-数据过第三方/vs自研-100家现成vs3家手写/12避坑/落地清单12项/速查表-401-404-429-延迟-成本)→面试冲刺(20自测三档/毕业检查单10项/面试8题-范式痛点-方案-架构-治理-收益数据)（2026-08：v1.94.0(2026-07-28)/v1.83.14压缩+Memory/Rust迁移beta-15倍吞吐/每周多minor必须锁版/OpenAI兼容为事实协议） |
| DeepSeek (V4 Pro) | `03-AI.../DeepSeek/` | 10 | Full evolution V2→V4 Pro |
| Java 后端名词剖析 | `02-.../Java后端开发名词剖析/` | 11 | 300+ concepts, 10 domains |
| 后端分布式名词通俗解释 | `02-.../09-Web开发全流程/后端 分布式常用名词通俗解释/` | 7 | 总览→解耦异步→限流削峰背压→熔断降级雪崩→幂等防抖→缓存分片→电商全链路串讲（大白话+面试对比） |
| Postman | `02-.../Postman/` | 8 | API testing full workflow |
| JMeter | `02-.../Jmeter/` | 8 | Performance testing: Sampler→CI/CD |
| APIFox | `02-.../07-工程运维基础/测试/APIFox/` | 9 | 总览(定位:API全生命周期一体化,vs Postman)→核心概念与界面入门(项目/接口/环境三大模型/定义驱动)→接口调试与请求构造(变量插值/鉴权/OAuth2自动刷新/网络信息/SSE)→接口文档与Mock(定义即文档/智能Mock/期望引擎/自托管Runner)→自动化测试与场景编排(测试套件静态动态双模式/场景用例/断言库)→CLI与CI-CD集成(apifox run/命令家族/Jenkins/Actions)→团队协作与导入导出(分支/AI分支/Postman-OpenAPI迁移)→AI能力与MCP调试(文档自愈/CLI+Skill for Agent/cli-schema/MCP调试)→生产实践与选型避坑(vs JMeter/安全沙盒/面试题)（2026-08：文档自愈、CLI+Skill、MCP调试、测试套件、OAuth2自动刷新） |
| HTTPie | `02-.../07-工程运维基础/测试/HTTPie/` | 6 | 总览(定位:人类友好CLI,vs curl互补)→安装与基础语法(http [flags] [METHOD] URL [items]/方法推断/URL粘贴)→请求构造全解(items运算符三族/JSON自动序列化/表单文件上传/认证basic-digest-bearer/插件/TLS)→响应处理与输出(格式化/-p组合/下载/SSE自动流式/--check-status)→会话与脚本化(sessions持久化/认证一次处处免认证/--offline离线构建/CI脚本)→与curl对比与生产实践(逐场景对照/迁移互转/避坑/面试)（2026-08：HTTPie 3.2.3、Bearer认证3.0原生、可取消UA/Accept-Encoding、SSE自动流式） |
| Ubuntu | `02-.../Ubuntu/` | 7 | CLI→Java env→systemd→Shell |
| CentOS | `02-.../CentOS/` | 5 | dnf→SELinux→firewalld→Production |
| Linux | `02-.../运维/Linux 操作系统/Linux/` | 12 | 基础→命令→系统管理→网络安全→服务→速查→服务器→权限→JVM排查→日志（Rocky 10/Ubuntu 26.04） |
| 日志监控指标 | `02-.../07-工程运维基础/运维/日志监控指标/` | 8 | 总览(三支柱定位,与同级Prometheus体系分工)→Java日志框架与SLF4J(生态演进/门面机制/Logback vs Log4j2/桥接死循环陷阱)→Logback配置与生产实践(Pattern铁律/滚动三件套/异步四参数/MDC线程池传递/JSON结构化)→日志采集与集中式平台(采集架构/EFK vs Loki选型/LogQL/标签基数/脱敏)→JVM监控指标与Micrometer(内存GC线程类加载四族/泄漏三信号/Actuator暴露)→告警体系与SLO(告警分级/错误预算/燃烧率14x-2x双层/疲劳治理)→可观测性三支柱与OpenTelemetry(traceId关联/LGTM/OTel Java Agent/采样策略)→生产实践与面试题(落地清单/成本三黑洞/14避坑/面试)（2026-08：SLF4J 2.x/Logback 1.4-1.5/Log4j2 2.20+安全管控/Loki v3.2/OTel行业标准/Alloy取代Promtail） |
| 网络问题排查 | `02-.../07-工程运维基础/运维/网络问题排查/` | 9 | 总览(分层定位原则/五连最小工具集)→分层排查方法论(现象到层映射/由外到内/两端视角/抓包定论/决策树)→基础工具速查(ping/telnet/ss/curl耗时分解/traceroute-mtr/dig)→TCP连接问题排查(超时vs拒绝/RST/TIME_WAIT/CLOSE_WAIT泄漏/SYN队列溢出/抓包分析)→DNS与域名问题排查(解析链路/dig四状态/TTL变更规范/JVM DNS缓存双坑/容器DNS)→HTTP层问题排查(超时三兄弟/超时链设计/重试幂等/代理网关502-504-499/TLS四坑/LB逐实例对比)→Java应用网络问题排查(异常翻译表/连接池三件套/HikariCP参数/SSL truststore/jstack-Arthas)→容器与微服务网络排查(Docker三问/K8s endpoints第一问/CoreDNS/Ingress/调用链分水岭/Nacos容器IP坑/netshoot)→生产案例与面试题(六案例/八习惯/10避坑/11面试) |
| CI/CD与灰度发布 | `02-.../07-工程运维基础/运维/CI CD灰度发布/` | 9 | 总览(2026工具链全景/典型组合)→CI-CD概念与流水线设计(CI-CD-DevOps辨析/七阶段流水线/Pipeline as Code/门禁四层/环境分支联动)→Jenkins与GitLab-CI实践(对比/容器化Agent/凭证管理/发布按钮/选型)→GitHub-Actions与制品管理(事件驱动/needs门禁链/environment审批/矩阵/Harbor-Nexus/制品版本回滚)→质量门禁与安全左移(测试门禁/覆盖率红线/SonarQube/依赖扫描CVSS7阻断/Trivy/Sealed Secrets)→发布策略全景(滚动/蓝绿秒级回滚/金丝雀10-30-60-100/深红2026新理念/A-B/回滚三层)→灰度发布落地(四方案对比/Argo Rollouts AnalysisTemplate自动回滚/Istio权重Header路由/Nacos配置灰度/网关灰度局限)→GitOps与ArgoCD(Git唯一事实来源/CI不碰集群/App of Apps/Flux对比/回滚即Git回退)→生产实践与面试题(发布平台四维度/五阶段落地/10避坑/14面试) |
| Jenkins | `02-.../07-工程运维基础/运维/Jenkins/` | 8 | 总览(2026定位:组织市场28%与GA33%对比/LTS 2.555需Java21-25/存量企业内网隔离甜区)→安装与初始化(war-docker/Compose/初始化向导/插件最小化/系统配置三件事)→核心概念(Master-Agent架构/Freestyle vs Pipeline/Workspace与构建记录/触发器Webhook优先/参数化)→Pipeline语法(声明式vs脚本式/pipeline块结构/agent/stages-post/when-parallel/script转义Groovy)→凭证与安全(Credentials五类/withCredentials三不/权限矩阵/Script Console即RCE/加固清单)→构建工具集成(全局工具/Maven缓存提速/SonarQube质量门禁waitForQualityGate/Nexus-Harbor制品/Java标准流水线模板)→Agent与分布式构建(SSH-JNLP-Docker/K8s动态Agent/标签分组/环境一致性/HA现实单写者)→共享库与工程化(vars-src结构/多分支流水线/when-branch-tag/Webhook工程化/Jenkinsfile规范/三阶段演进)→生产运维与故障排查(备份含secrets目录/升级分步/故障速查表/磁盘治理三板斧/12避坑/10面试)（2026-08：LTS 2.555.3(2026-06-08)/2.541 EOL 2026-04-15/Java 21或25基准；与CI CD灰度发布体系分工:对比选型vs本体系深潜） |
| FinalShell | `02-.../07-工程运维基础/运维/FinalShell/` | 4 | 总览(定位:SSH+SFTP+监控+隧道四合一,vs Xshell-Xftp-Putty-WinSCP)→安装与连接管理(SSH会话/密码与ED25519密钥认证/多标签分屏/编码GBK-UTF8/Keep-Alive心跳)→文件管理与资源监控(SFTP拖拽断点续传打包上传/Java部署闭环/实时监控面板CPU内存网络IO/进程kill/网络工具)→高级功能与生产实践(端口转发三隧道/跳板机代理链/批量执行命令面板/免费vs专业版/10避坑)（2026-08：4.6.3；三栏界面、监控无需服务器装脚本） |
| Xshell | `02-.../07-工程运维基础/运维/Xshell/` | 4 | 总览(定位:Xshell管命令+Xftp管文件双工具组合,vs FinalShell)→安装与连接管理(会话管理核心竞争力/文件夹分组/凭据保存/主密码保护/公钥RSA-ED25519-PKCS11-GSSAPI/发送到所有会话)→Xftp与文件传输(SFTP-FTP/FXP服务器间直传/断点续传/文件夹同步/同步浏览/Xshell一键协同)→高级功能与生产实践(跳板机Jump Host/端口转发/脚本录制与触发器/快捷命令/对比选型/10避坑)（2026-08：Xshell 8/Xftp 8 Build 0095；NetSarang出品、Windows平台、个人学校免费） |
| Nginx | `02-.../07-工程运维基础/运维/Nginx 基础/` | 11 | 总览(定位:接入层统一入口,与Gateway分工)→核心概念与安装(事件驱动/epoll/master-worker/配置三层/面试30秒模板)→静态资源与location匹配(优先级口诀/root vs alias/gzip/缓存头/try_files SPA路由)→反向代理配置(proxy_pass路径规则带-不带斜杠/四头必配/超时分场景/流式三件套/WebSocket三要素)→负载均衡与高可用(四策略/weight-max-fails-backup/proxy_next_upstream/keepalive三件套)→HTTP模块与rewrite(last vs break/return优先/邪恶if/CORS)→HTTPS与证书(TLS终止/全站301跳转/TLS1.3+HTTP2/证书自动续期与告警)→缓存配置(三层缓存/proxy_cache四要素/缓存键纪律/版本化更新/X-Cache-Status)→限流与安全防护(漏桶算法/limit_req-burst/limit_conn/IP黑名单/安全头五件套/WAF定位)→日志与监控(log_format/rt-urt耗时对比/日志切割USR1/5xx-p99-QPS)→生产实践与面试题(调优三件套/502-504-499排障/10避坑/面试五模板+14题)（2026-08：稳定版1.28.2/主线1.29.x） |
| 中间件运维故障排查 | `02-.../07-工程运维基础/运维/MySQL Redis MQ 运维相关故障现象/` | 10 | 总览(现象到排查路径的故障地图)→故障排查方法论(五步法/监控先行/现象分级P1先止血/变更回溯/最小复现/复盘四段式)→MySQL连接与性能(连接数满-爬升即泄漏/慢查询EXPLAIN三列/CPU高processlist快照)→MySQL锁与复制(锁等待vs死锁/INNODB STATUS取证/主从延迟三查Last-SQL-Error/大事务元凶)→MySQL存储与数据(磁盘满binlog膨胀/误删恢复三要素-备份binlog时间点/全量+增量+演练)→Redis连接与内存(连接爬升泄漏/maxmemory淘汰-evicted_keys信号/OOM四查/内存规划60-70%)→Redis阻塞与数据(大key四害/生产禁KEYS-用SCAN/slowlog/fork隐藏阻塞/主从丢失防护min-replicas)→MQ堆积与消费(LAG拐点/分区=并行度扩容上限/重试风暴退避上限死信/幂等铁律/顺序边界)→MQ可靠性与死信(丢消息三环节/死信三步分类/延迟18等级限制/事务消息vs Outbox/生产配置基线)→生产实战与面试题(八案例八教训/10避坑/14面试)（MySQL 8.4/Redis 8/Kafka 4基准） |
| Cookie & Session（会话技术） | `02-.../09-Web开发全流程/Cookie & Session（会话技术）/` | 8 | 总览(HTTP无状态下的有状态方案/演进主线:单体Session到JWT混合)→Cookie机制与属性详解(六大属性/HttpOnly-Secure-SameSite黄金组合/SameSite三值/作用域/Java操作)→Session机制与Java实现(数据在服务器标识在客户端/getSession-vs-false/HttpSession API/Spring注入)→会话传递与生命周期(SessionID三传递/创建时机-JSP坑/失效四途径/关浏览器不等于登出/encodeURL)→分布式会话方案(复制-粘滞-集中式三对比/Spring Session零改动/Redis落地/序列化瘦身)→会话安全与防护(四威胁:偷ID送ID读Cookie借Cookie/登录轮换changeSessionId/CSRF双层SameSite+Token)→Token与JWT无状态方案(JWT结构-签名保完整不保密/双Token模式/可撤销vs不可撤销/安全基线)→生产实践与面试题(配置基线/混合方案/10避坑/14面试)（2026-08：Spring Session主流、Boot 4 SameSite显式配置防回归Issue#48830、JWT全面替代Session已非共识） |
| Filter 过滤器 | `02-.../09-Web开发全流程/Filter 过滤器/` | 7 | 总览(定位:Servlet请求链路统一拦截关卡)→Filter基础与生命周期(三方法init-doFilter-destroy/doFilter三位置-不调chain即拦截/单例线程安全/匹配范围)→FilterChain责任链(洋葱模型-前置顺序后置逆序/顺序三原则/请求包装Wrapper-Body单次读)→常用Filter场景(登录校验教科书/日志最外层/CORS预检204/限流429/XSS过滤)→SpringBoot注册Filter(三方式对比/FilterRegistrationBean推荐/顺序控制/OncePerRequestFilter标准基类)→Filter vs Interceptor vs AOP(容器层-MVC层-Bean层/HandlerMethod独有能力/执行顺序/选型决策树)→生产实践与面试题(四要素骨架/10避坑/12面试)（Servlet 6.x/Jakarta EE 11基准） |
| JSP（了解即可） | `02-.../09-Web开发全流程/JSP 了解即可/` | 4 | 总览(定位:历史知识-原理懂语法认识不深写)→基础语法与内置对象(脚本三元素/三大指令/九大内置对象/JSTL+EL现代正确用法)→JSP与Servlet的关系及MVC(本质是Servlet/翻译机制-首次访问慢/MVC分工/演进主线)→现状与替代方案(退场核心原因/Thymeleaf属性式语法/前后端分离主流/存量项目策略)（Jakarta Pages规范；新项目不用JSP） |
| Listener 监听器 | `02-.../09-Web开发全流程/Listener 监听器/` | 6 | 总览(定位:容器事件观察者,JavaWeb三大组件之一)→基础与八大分类(事件模型/3对象×2事件+2会话绑定=8/触发时机总表)→ServletContextListener与启动初始化(启动关闭回调/缓存预热定时任务/context全局共享/与ApplicationRunner对比)→Session与Request监听器(在线统计-浏览器关闭不触发销毁/访问日志/属性监听登录感知/在线用户管理ConcurrentHashMap)→SpringBoot注册Listener(@WebListener不可注入vs注册Bean/与Spring事件体系对比/Boot正确姿势)→生产实践与面试题(场景总表/现代替代/10避坑/12面试)（Servlet 6.x基准；Boot项目用ApplicationRunner替代初始化Listener） |
| Servlet | `02-.../09-Web开发全流程/Servlet/` | 9 | 总览(定位:JavaWeb基石-一切框架底层)→规范演进史与版本矩阵(2.3→6.1/6.2里程碑/javax→jakarta迁移/容器版本矩阵)→生命周期与线程模型(五阶段/load-on-startup/init重载陷阱/service分发/destroy边界/单例多线程/线程安全铁律/虚拟线程)→核心API体系详解(继承层次源码/Request全API/Response全API/Config与Context/四大域对象/6.0-6.1新增)→请求处理与路径映射(URL拆解/四种映射优先级/缺省Servlet与静态资源/转发vs重定向/编码三铁律/multipart参数行为)→配置方式演进与动态注册(web.xml/注解扫描/SCI编程式/web-fragment/SpringBoot注册三路径)→异步处理与非阻塞IO(AsyncContext/ReadListener-WriteListener/ByteBuffer/Tomcat NIO模型/异步vs虚拟线程)→文件上传下载与PartAPI(MultipartConfig四参数/Part全解/大文件流式/下载头/路径穿越安全)→生产实践与面试题(安全清单/性能调优/与SpringMVC本质关系/12避坑/12面试)（2026-08：Servlet 6.1/Jakarta EE 11/Tomcat 11基准；6.2里程碑开发中） |
| SpringBoot Web | `02-.../09-Web开发全流程/SpringBoot Web/` | 8 | 总览(定位:Servlet体系之上的框架化Web层,与06-Spring全家桶/SpringBoot全框架分工)→内嵌容器与启动时序(WebServerFactory/TomcatStarter/ServletContextInitializer/启动时序/server.*全表/容器切换/虚拟线程)→DispatcherServlet请求处理链路(doDispatch十步/四大件/HandlerMapping家族/参数解析/消息转换/拦截器链/异步返回类型/API版本管理)→WebMvcConfigurer定制全解(拦截器/CORS/静态资源/消息转换器/参数解析器/格式化器/路径匹配)→Filter与Interceptor注册与顺序(三路径/OncePerRequestFilter/顺序控制/三层分工)→错误处理机制深潜(/error/BasicErrorController/ErrorAttributes/Advice分工/内容协商/错误页/404排查)→静态资源与SPA路由(默认目录/缓存版本化/WebJars/SPA三方案/前后端分离)→部署形态与HTTPS配置(Jar vs War/SpringBootServletInitializer/SSL/HTTP2/压缩/线程池/优雅停机)→生产实践与面试题(上传速查/故障排障/安全速查/Boot3到4迁移/12避坑/12面试)（2026-08：Boot 4.1.0/Framework 7.0/Tomcat 11/Servlet 6.1/Jackson 3基准；starter更名webmvc、Undertow移除、尾斜杠匹配移除、SPA无内置fallback已源码验证） |
| Web 服务器 | `02-.../09-Web开发全流程/Web 服务器/` | 7 | 总览(定位:服务器家族全景,与Nginx/Tomcat独立体系分工)→分类与演进史(四类服务器/HTTP演进倒逼/进程模型fork到事件驱动/C10K/三十年简史)→Apache httpd全面解析(2.4.68现状/模块化架构/MPM三模型/虚拟主机/.htaccess生态/存量场景)→Caddy与自动HTTPS时代(v2.11.4/ACME三挑战/On-demand TLS/Caddyfile/HTTP3-ECH-PQC/vs Nginx/避坑)→IIS与Windows生态(集成管道/http.sys/应用池/web.config/URL Rewrite与ARR/ASP.NET Core托管/安全)→Web服务器与应用服务器分工(动静分离/组合架构/反代五件套/Java标准组合/云原生演变)→选型决策与生产实践(决策树/份额口径/性能对比/Apache迁移/组合选型表/10避坑/10面试)（2026-08：Apache httpd 2.4.68(2026-06-08)/Caddy 2.11.4(2026-07-02)/W3Techs份额Nginx31.5%-Cloudflare29%-Apache23.1%基准） |
| Web 高阶知识 | `02-.../09-Web开发全流程/Web 高阶知识/` | 9 | 总览(定位:协议与浏览器生态专项,服务器之上业务之下)→HTTP协议深入(报文三段/URL语义/方法幂等/状态码全语义/头字段体系/连接管理与队头阻塞/REST语义)→HTTP缓存体系(缓存位置全景/强缓存/协商缓存ETag/Cache-Control全参数/启发式缓存/策略设计/版本化是钥匙/排障)→HTTPS与TLS深入(2026现状94%加密/TLS1.3占70-73%/证书链与信任/握手1.2vs1.3/前向保密/双向TLS/HSTS/性能)→HTTP2与HTTP3(HTTP1.1三大痛点/二进制帧多路复用/HPACK/ALPN/Push已死/QUIC四能力/2026采用数据HTTP2流量51-55%/升级配置/对比选型)→跨域与浏览器安全机制(同源策略/CORS预检全流程/Credentials/Expose-Headers/SameSite边界/安全头六件套CSP/反代同域消灭跨域)→实时通信WebSocket与SSE(演进/握手101/帧/心跳/SSE自动重连Last-Event-ID/对比选型/集群粘滞vs广播/Boot落地)→Web安全攻防(OWASP Top 10 2025全榜解读/XSS三型/CSRF三层防线/SQL注入/SSRF云时代/越权IDOR/日志告警/防护清单总表)→Web性能与浏览器机制(请求生命周期/关键渲染路径/Core Web Vitals含INP替代FID/async-defer-preload/lazy/渲染阻塞治理/优化分层地图/度量监控)→生产实践与面试题(幂等三板斧/限流防重放/调试工具链/12避坑/12面试)（2026-08：OWASP Top 10 2025(2025-11)/HTTP3站点40%-流量21-34%/TLS1.3占加密流量70-73%/HTTPS占94%基准） |
| Shell | `02-.../运维/Linux 操作系统/Shell/` | 6 | 入门→变量→流程→函数→文本处理（三剑客） |
| 官方文档精读清单 | `02-.../07-工程运维基础/开发/后端 & AI 方向必啃官方文档清单/` | 6 | 总览(方法论七条/校招在职双轨/原稿剖析)→Java生态与JVM(JDK API/JLS/JVMS/JEP/Spring 7/Boot 4/AI 2/JUnit)→中间件(MySQL 8.4 LTS/Redis 8/Kafka 4 KRaft/RocketMQ 5/Rabbit 4/ES 9/Nacos 3)→AI大模型(OpenAI兼容协议/DeepSeek/Qwen/LangChain v1/LangChain4j/Milvus/部署推理)→工具工程化(Maven 4/Git Book/Docker/K8s/Linux man/可观测性)→协议基础(MDN/RFC 9110-9114/TLS 1.3)（2026-08 检索校准） |
| 项目全流程 | `02-.../项目从开始开发到上线全流程/` | 6 | Requirements→Launch→Ops |
| Vim | `01-.../Vim/` | 4 | Basic→Advanced→Plugins |
| 文件后缀名 | `01-.../不同文件的后缀名/` | 5 | 100+ file formats |
| CPU | `01-.../计算机组成原理/CPU/` | 8 | 组成→流水线→超标量→实例→封装→评测→AI演进 |
| GPU | `01-.../计算机组成原理/GPU/` | 8 | SIMT→存储→调度→CUDA→NVIDIA演进→AMD→AI时代 |
| 缓存与Cache | `01-.../计算机组成原理/缓存与Cache/` | 8 | 原理→结构→替换→多级→一致性→安全→前沿 |
| 寄存器 | `01-.../计算机组成原理/寄存器/` | 9 | 本质→ISA→系统寄存器→ABI→重命名→切换→分配→前沿 |
| 量化背后硬件约束 | `01-.../计算机硬件方向系统学习清单/量化背后硬件约束/` | 11 | 总览→量化为什么能提速(带宽受限/Roofline 1 FLOP/byte)→数值格式全景(FP8/INT4/FP4/MX微缩放)→Tensor Core与矩阵指令(反量化位置)→NVIDIA量化能力全谱(Turing INT8→Hopper FP8→Blackwell NVFP4 9-18PFLOPS实测~1200)→AMD/Intel/国产(MI355X MXFP4 MFMA/Gaudi3 FP8 INC/Xeon AMX INT8/Ascend)→内存系统约束(HBM带宽容量/KV Cache量化RDKV 4.5x/近内存反量化)→权重部署格式(W4A16 GPTQ-AWQ/反量化陷阱/组缩放)→精度与硬件效率权衡(异常值/SmoothQuant/MR-GPTQ/MicroMix/W4A6)→端侧硬件约束(GGUF Q4_K_M/Apple Metal/Qualcomm QNN)→生产实践与选型决策(决策树/引擎对比/成本token/避坑)（2026-08：B200 FP4 9PF-B300 15PF/GB300 NVL72 1.08EF/FlashInfer 1132 vs vLLM 968 TFLOPS/Kimi K2.5 MXFP4 5369 tok/s） |
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
- **Prose-first, tables for contrast**: 机制/取舍/演进逻辑用文字论述（配代码示例）；表格仅用于对比、API 速查、检查清单等真正适合枚举的场景，禁止用表格代替论述或凑字数（2026-08-09 用户反馈表格过度）
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
