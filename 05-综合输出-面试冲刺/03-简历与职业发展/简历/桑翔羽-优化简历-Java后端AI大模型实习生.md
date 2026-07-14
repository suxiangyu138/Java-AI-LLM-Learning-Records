# 桑翔羽

**Java 后端开发实习生（AI 大模型应用开发方向）**  
📍 安徽省合肥市 | 📞 19165863362 | ✉️ 1368614311@qq.com | 🎓 安徽大学 · 计算机科学与技术 · 本科 2028 届（211）

---

## 教育背景

**安徽大学** · 计算机科学与技术学院 · 计算机科学与技术 · 全日制本科（211）  
*2024.09 – 2028.06*  
GPA：前 30%（如有） | 英语：CET-4 / CET-6 已通过  
相关课程：数据结构 · 操作系统 · 计算机网络 · 数据库系统 · 面向对象程序设计

---

## 技术栈

**后端基础**  
Java 17/21 · Spring Boot 3.x · Spring Cloud Alibaba · Spring Security · MyBatis / MyBatis-Plus  
MySQL 8.0（索引优化 / 慢查询调优 / 事务隔离级别） · Redis（缓存策略 / 分布式锁 / Lua 脚本 / 多级缓存）  
RabbitMQ（异步削峰 / 死信延迟队列） · Elasticsearch（全文检索 / 聚合查询） · JWT · RBAC 权限控制

**AI 大模型应用**  
OpenAI / DeepSeek / 通义千问 API 调用 · SSE 流式输出 · Prompt Engineering · Function Calling  
RAG 检索增强生成 · LangChain4j · Agent 智能体（ReAct 编排） · MCP 协议 · NL2SQL  
Milvus 向量数据库 · BGE / CLIP Embedding · 混合检索（BM25 + 向量 + RRF 融合）

**工具与部署**  
Docker · Docker Compose · Git · Maven · Nginx · Linux · Prometheus + Grafana  
AI 编程工具：Claude Code · Cursor · GitHub Copilot

---

## 项目经历

### LingShu 灵枢 — 企业级医疗 AI 智能问答平台  
*Spring Boot 3.4 + RAG + Agent + MCP + Milvus* | *个人项目 · 2024.09 – 至今*

> 面向医疗场景的 AI 智能问答平台，通过 RAG + Agent 架构解决大模型幻觉问题，支持症状问诊、用药指导、健康科普、体检报告解读、智能分诊五大场景，满足医疗数据合规与私有化部署要求。

- 设计高可用**大模型网关**：封装 OpenAI 兼容 API 客户端，实现 SSE 流式 + 同步双模调用；引入指数退避重试（2s → 4s → 10s，最多 3 次）+ Sentinel 限流熔断 + 四级异常分类，API 调用失败率 **< 1%**
- 搭建 14 步 **RAG 检索增强管线**：PHI 脱敏 → 意图路由 → 查询改写 → 混合检索（BM25 + 向量 + RRF k=60 融合）→ 重排序（向量 70% + 关键词 30%）→ 上下文构建 → LLM 生成 → 输出护栏 → 来源溯源，Top-5 检索准确率 **~85%**，有效抑制医疗幻觉
- 实现 **Agent 智能体系统**：基于 ReAct（Think → Act → Observe）循环设计 AgentPlanner，Prompt 引导 Function Calling 输出 JSON 格式 tool_call；封装 5 个医疗 Skill（症状问诊 / 用药指导 / 智能分诊 / 报告解读 / 健康科普），支持双层会话存储（Caffeine L1 + Redis L2），20 轮历史 / 8000 字上下文窗口
- 设计多层**安全护栏**：输入端自残/停药/偏方 3 级关键词检测，输出端 6 条危险模式正则校验（禁止诊断/停药建议），PHI 敏感信息自动脱敏（身份证/手机号/住址）
- 实现**私有化部署**：Docker Compose 编排 9 个服务（Spring Boot ×2 + MySQL + Redis + Milvus + ES + Ollama + RabbitMQ + MinIO），单机 8GB 内存冷启动 **< 2 分钟**，编写 52 个单元测试全部通过

---

### SuGuangMall — AI-Native 高并发微服务智能电商平台  
*Spring Cloud Alibaba + AI Multi-Agent + CLIP 以图搜图* | *团队项目 · 2025.03 – 至今*

> 基于微服务架构的企业级 AI 电商平台，12 个业务微服务 + 7 个 AI 智能服务，覆盖完整交易闭环；融合 Multi-Agent 购物管家、CLIP 以图搜图、RAG 语义搜索、秒杀 10 万 QPS 等核心能力。

- 设计 **Multi-Agent 购物管家**：基于 LangChain4j 实现 ReAct 编排，6 个专业化 Agent（搜索/比较/推荐/客服/内容/分析）+ AgentRouter 意图路由 + YAML 技能热加载，支持自然语言多轮对话购物
- 实现 **CLIP 以图搜图**：CLIP 多模态 Embedding（512 维）→ Milvus ANN 向量检索（TopK=30）→ 属性标签匹配（颜色/款式/材质）→ 风格识别 + 穿搭建议，用户拍照即可找同款商品
- 搭建 **RAG 混合检索导购**：Query Rewrite（LLM 改写）→ 多路召回（Milvus 向量 + ES 关键词）→ RRF 融合排序 → MMR 重排 → LLM 生成推荐，搜索召回率提升 **40%**
- 设计**秒杀高并发链路**：Nginx 限流 → Sentinel QPS 控制 → Redis Lua 原子扣库存（预占 → 确认扣减 → 超时回滚）→ RabbitMQ 异步削峰 → 死信延迟队列 15min 关单 + 库存回滚，库存**零超卖**，JMeter 压测 QPS 2000+
- 引入 Seata AT 分布式事务、ShardingSphere 分库分表（订单按 user_id 分库 ×16 分表）、Caffeine + Redis 多级缓存、RBAC 权限体系、Prometheus + Grafana + SkyWalking + ELK 可观测体系

---

### Flavor Dash — 企业级 AI 智能外卖履约平台  
*Spring Boot 3.4 + LLM + RAG + Agent + Milvus* | *个人项目 · 2024.06 – 2024.12*

> 面向本地生活服务的企业级外卖平台，完整覆盖 C 端下单、商家运营、微信支付、实时通知等业务闭环；三期升级 AI 大模型能力，落地 6 大 AI 模块。

- 搭建**企业级交易平台**：设计 RESTful 接口规范与代码分层架构，完成 MySQL 表结构设计与索引优化；引入 Redis 缓存预热 + 分布式锁 + 布隆过滤器，解决缓存穿透/雪崩/击穿与并发超卖
- 落地 **6 大 AI 模块**：① AI 智能客服（SSE 流式 + Function Calling 自然语言点餐）② AI 智能推荐（RAG + 用户偏好向量）③ AI 智能调度（LLM 多目标优化）④ AI 菜单优化（Prompt 工程生成描述/标签）⑤ AI 评价分析（情感分析 + 投诉预警）⑥ AI 运营看板（NL2SQL 自然语言查数据）
- 构建 **LLM Client Factory**：统一抽象 OpenAI / DeepSeek / 智谱 / 通义千问多模型提供商，SSE 流式输出 + 指数退避重试 + Resilience4j 熔断降级 + Token 计数 + 上下文窗口管理 + 多 Provider 动态切换
- 实现 **Agent 框架 + RAG 管线**：Think → Act → Observe 自主循环 + Tool Registry 工具注册中心 + AgentMemory 对话记忆（滑动窗口 + 上下文压缩）+ MCP 协议（stdio / HTTP 双传输模式）；DocumentSplitter 文档分割 + Embedding 向量化 + Milvus 相似度检索
- 整合 RabbitMQ 异步通知 + WebSocket 实时推送（来单提醒/催单）+ Spring Task 超时自动处理 + 微信支付 API v3 + 阿里云 OSS + Elasticsearch 全文检索

---

## 个人优势

- 英语 CET-4/CET-6 通过，可流畅阅读英文技术文档（Spring 官方文档、OpenAI API Reference 等）
- 长期活跃于 GitHub、CSDN 等技术社区，持续跟进 Java 生态与大模型应用前沿，具备快速学习与技术调研能力
- 三个完整项目从架构设计到部署上线全流程实践，具备独立排查问题与工程化落地能力
- 熟练运用 Claude Code、Cursor、GitHub Copilot 等 AI 编程工具，结合自身判断安全高效地提升开发效率
- 规范编码习惯（命名/注释/分支管理），适配团队协作模式，可快速融入企业开发流程

---

> **简历排版说明**（导出 PDF 时参考）：
> - **中文字体**：微软雅黑 | **英文/数字字体**：Arial 或 Calibri | **代码/技术名词**：Consolas
> - **字号**：姓名 16pt 加粗 · 一级标题 13pt 加粗 · 正文 10-10.5pt（全文最多 3 档字号）
> - **对齐**：全文左对齐 · 行距 1.2–1.4 倍 · 页边距 1.5–2cm
> - **输出格式**：PDF（严禁 Word）· 一页纸最佳
