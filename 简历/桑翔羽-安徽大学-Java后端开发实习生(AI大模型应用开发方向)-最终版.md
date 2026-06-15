# Java 后端开发实习生（AI 大模型应用开发方向）

姓名：桑翔羽 | 电话：19165863362 | 邮箱：suxiangyu_dev@foxmail.com | 现居地：安徽省合肥市

---

## 教育背景

2024.09 – 2028.06&nbsp;&nbsp;&nbsp;&nbsp;安徽大学 / 计算机科学与技术学院&nbsp;&nbsp;&nbsp;&nbsp;全日制本科（211）&nbsp;&nbsp;&nbsp;&nbsp;计算机科学与技术专业

---

## 专业技能

- **Java**：掌握核心特性、JUC 并发编程、JVM 内存模型与类加载机制，具备高并发场景下的调优能力
- **Spring 生态**：熟悉 Spring Boot 3.x、Spring Cloud Alibaba（Nacos/Gateway/Sentinel/Seata）、Dubbo、MyBatis / MyBatis-Plus，可独立完成 RESTful API 设计、微服务拆分与治理
- **数据库与缓存**：掌握 MySQL 索引优化、SQL 调优、事务隔离级别；熟悉 Redis 缓存策略、分布式锁、Lua 脚本、多级缓存，解决缓存穿透/雪崩/击穿与并发超卖
- **消息与搜索**：熟悉 RabbitMQ 异步削峰、死信延迟队列；熟悉 Elasticsearch 全文检索、分词器配置与聚合查询
- **大模型应用**：熟悉 LangChain4j / Spring AI，掌握 Prompt Engineering、Function Calling、Agent 智能体（ReAct 编排）、MCP 协议、NL2SQL 等核心技术
- **RAG 检索增强**：掌握全链路 RAG：文档切片 → Embedding 向量化（BGE / CLIP）→ Milvus 向量存储 → 混合检索（BM25 + 向量 + RRF 融合）→ MMR 重排序 → LLM 生成，有效抑制模型幻觉
- **模型平台**：熟悉 DeepSeek / OpenAI / 通义千问 / 智谱 GLM 等主流 API，掌握 SSE 流式输出、多模型统一网关、指数退避重试、Resilience4j 熔断降级
- **工程运维**：熟悉 Docker / Docker Compose 容器化部署、Nginx 反向代理、Git / Maven / Linux 常用操作，可独立完成项目打包与上线部署；熟练运用 Claude Code、Cursor 等 AI 编程工具
- **Python**：具备脚本开发与数据分析能力，可完成网络爬虫支撑知识库构建与 AI 辅助开发

---

## 项目经历

### 项目一：Flavor Dash — 企业级 AI 智能外卖履约平台&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`2024.06 – 2025.01`

**项目描述**：采用 Spring Boot 单体架构搭建本地化餐饮外卖平台，覆盖 C 端用户、商家、配送员三方业务场景，实现用户注册登录、菜品展示、在线点餐、订单调度、微信支付、实时通知等核心功能。三期引入 AI 大模型能力，落地 AI 智能客服、智能推荐、智能调度、菜单优化、评价分析、运营看板 6 大 AI 模块，日均处理订单 **5000+**。

**负责内容**：

- 基于 Spring Boot 3.4 搭建项目基础架构，设计 RESTful 接口规范与代码分层架构，完成 MySQL 表结构设计与索引优化
- 结合 Redis 实现缓存预热、分布式锁、布隆过滤器，解决缓存穿透/雪崩/击穿与并发超卖，缓存命中率提升至 **95%**
- 整合 RabbitMQ 异步通知 + WebSocket 实时推送（延迟 **< 500ms**）+ Spring Task 超时自动处理
- 接入 Elasticsearch 实现商家、菜品全文检索，整合微信支付 API v3 + 阿里云 OSS
- 构建 **LLM Client Factory** 多模型统一网关，抽象 OpenAI / DeepSeek / 智谱 / 通义千问多提供商，实现 SSE 流式输出、指数退避重试、Resilience4j 熔断降级、Token 计数与上下文窗口管理，模型调用成功率 **99.5%+**
- 实现 **Agent 框架**（Think→Act→Observe 自主循环 + Tool Registry 工具注册中心 + AgentMemory 对话记忆）+ **RAG 管线**（DocumentSplitter 文档分割 → Embedding 向量化 → Milvus 相似度检索），TOP-5 召回率 **~88%**
- 落地 6 大 AI 模块：AI 智能客服（SSE 流式 + Function Calling，自动处理 **70%** 咨询）、AI 智能推荐（RAG + 偏好向量，点击率 **+25%**）、AI 评价分析（情感分析 + 投诉预警，准确率 **91%**）、AI 运营看板（NL2SQL）、AI 智能调度、AI 菜单优化

### 项目二：SuGuangMall — AI-Native 高并发微服务智能电商平台&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`2025.01 – 至今`

**项目描述**：基于 Spring Cloud Alibaba 微服务架构构建企业级 AI 电商平台，拆解为 12 个业务微服务 + 7 个 AI 智能服务，实现服务独立开发、注册发现、远程调用与流量治理。融合 Multi-Agent 购物管家、CLIP 以图搜图、RAG 语义搜索、秒杀高并发等核心能力，支撑电商业务规模化、高可用运营。

**负责内容**：

- 完成微服务拆分与架构设计，通过 Nacos 实现服务注册发现与配置统一管理，利用 OpenFeign + Dubbo 完成微服务间远程调用
- 搭建 Spring Cloud Gateway 网关，实现请求路由、JWT + RBAC 权限校验与跨域处理；接入 Sentinel 实现服务限流、熔断降级
- 设计 **Multi-Agent 购物管家**：基于 LangChain4j 实现 ReAct 编排，6 个专业化 Agent（搜索/比较/推荐/客服/内容/分析）+ AgentRouter 意图路由 + YAML 技能热加载，支持自然语言多轮对话购物
- 实现 **CLIP 以图搜图**：CLIP 多模态 Embedding（512 维）→ Milvus ANN 向量检索（TopK=30）→ 属性标签匹配（颜色/款式/材质）→ 风格识别 + 穿搭建议，相似度匹配准确率 **~90%**
- 搭建 **RAG 混合检索导购**：Query Rewrite（LLM 改写）→ 多路召回（Milvus 向量 + ES 关键词）→ RRF 融合排序 → MMR 重排 → LLM 生成推荐，搜索召回率提升 **40%**
- 设计**秒杀高并发链路**：Nginx 限流 → Sentinel QPS 控制 → Redis Lua 原子扣库存（预占→确认扣减→超时回滚）→ RabbitMQ 异步削峰 → 死信延迟队列 15min 关单 + 库存回滚，库存**零超卖**，JMeter 压测 QPS **2000+**
- 引入 Seata AT 分布式事务、ShardingSphere 分库分表（订单按 user_id 分库 ×16 分表）、Caffeine + Redis 多级缓存、Prometheus + Grafana 全链路监控

### 项目三：LingShu 灵枢 — 企业级医疗 AI 智能问答平台&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`2024.09 – 至今`

**项目描述**：依托大模型技术构建医疗领域 AI 智能问答平台，结合医疗专业知识库，通过 RAG + Agent 架构解决大模型幻觉问题，实现症状问诊、用药指导、健康科普、体检报告解读、智能分诊五大核心场景，满足医疗数据合规、高可用、私有化部署要求。

**负责内容**：

- 设计高可用**大模型网关**：封装 OpenAI 兼容 API 客户端，实现 SSE 流式 + 同步双模调用；引入指数退避重试（2s→4s→10s）×3 次 + Sentinel 限流熔断 + 四级异常分类，API 调用失败率 **< 1%**
- 搭建 14 步 **RAG 检索增强管线**：PHI 脱敏 → 意图路由 → 查询改写 → 混合检索（BM25 + 向量 + RRF k=60 融合）→ 重排序（向量 70% + 关键词 30%）→ 上下文构建 → LLM 生成 → 输出护栏 → 来源溯源，Top-5 检索准确率 **~85%**
- 实现 **Agent 智能体系统**：基于 ReAct（Think→Act→Observe）循环设计 AgentPlanner，Prompt 引导 Function Calling 输出 JSON 格式 tool_call；封装 5 个医疗 Skill，双层会话存储（Caffeine L1 + Redis L2），支持 20 轮历史 / 8000 字上下文窗口
- 设计多层**安全护栏**：输入端 3 级敏感关键词检测，输出端 6 条危险模式正则校验（禁止诊断/停药建议），PHI 敏感信息自动脱敏
- 基于 Python 完成网络爬虫与数据分析，采集处理医疗公开数据，构建医疗专业向量知识库
- 实现**私有化部署**：Docker Compose 编排 9 个服务（Spring Boot ×2 + MySQL + Redis + Milvus + ES + Ollama + RabbitMQ + MinIO），单机 8GB 内存冷启动 **< 2 分钟**，单元测试 + 集成测试全覆盖

---

## 个人优势

- 顺利通过英语 CET-6，具备流畅阅读英文技术文档、获取整合各类技术资源的能力
- 三个完整项目从架构设计到部署上线全流程实践，具备独立排查问题与工程化落地能力
- 长期活跃于 GitHub、CSDN 等技术社区，主动跟进 Java 生态与大模型应用前沿技术，乐于学习接受新事物
- 具备规范编码习惯与工程化思维，适配团队协作模式，可快速融入企业开发流程
- **Java 后端功底扎实 + AI 应用落地能力强**，能独立完成从 API 设计到 AI 模型集成的全链路开发
