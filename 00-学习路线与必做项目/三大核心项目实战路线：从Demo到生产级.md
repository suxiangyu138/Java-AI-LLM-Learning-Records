# 三大核心项目实战路线：从 Demo 到生产级

> **目标：** 补齐"知道很多但动手不够深"的短板，用 3 个项目覆盖 Java 后端深度 + AI 融合能力，每个项目都能写进简历、上线演示、面试深讲。

---

## 为什么是这 3 个项目？

| 项目 | 覆盖能力 | 简历卖点 | 周期 |
|------|----------|----------|------|
| **项目一：分布式电商秒杀** | Java 高并发 + 中间件深度 | 后端基本功的天花板证明 | 6-8 周 |
| **项目二：RAG 智能知识库系统** | AI 大模型落地 + Java 集成 | Java × AI 复合能力 | 4-6 周 |
| **项目三：AI 智能客服 Agent** | Agent 开发 + 多轮对话 + 多租户 | 架构设计 + 商业化思维 | 6-8 周 |

**三个项目的关系：** 项目一证明你的 Java 后端深度，项目二证明你懂 AI，项目三证明你能把 Java + AI 融合成商业产品。

---

## 项目一：分布式电商秒杀系统

### 1.1 项目定位

> **面试官看到这个项目会想：** 这个人真正理解高并发，不是只会写 CRUD。

### 1.2 技术栈

```
后端：Spring Boot 3.x + MyBatis-Plus + MySQL 8.0 + Redis 7.x + RabbitMQ
中间件：Redisson（分布式锁）+ Canal（数据同步）+ Sentinel（限流降级）
基础设施：Docker Compose（本地）+ Nginx + JMeter（压测）
```

### 1.3 分阶段实施

#### 阶段 1：基础功能（1-2 周）

| 功能 | 要点 |
|------|------|
| 商品列表 + 详情 | RESTful API，分页查询 |
| 用户登录注册 | JWT + 拦截器 |
| 下单接口 | `POST /order`，扣库存 + 生成订单 |

**关键代码：** 扣库存用 MySQL 行级锁 `UPDATE ... SET stock = stock - 1 WHERE id = ? AND stock > 0`

#### 阶段 2：秒杀核心（2-3 周）——从这里开始体现技术深度

```
用户请求 → Nginx 负载均衡
              ↓
         Redis 预减库存（lua 脚本原子操作）
              ↓
         判断库存 > 0？
         ├─ 否 → 直接返回"已售罄"（快速失败）
         └─ 是 → 发送消息到 RabbitMQ
                       ↓
                  消费者异步创建订单（削峰填谷）
                       ↓
                  MySQL 最终扣减
```

**这个阶段你要能讲清楚：**

1. **为什么用 Redis 预减库存而不是直接操作 MySQL？**
   - MySQL 单行写 QPS 约 500-1000，秒杀场景远超此值
   - Redis 单机写 QPS 约 8-10w，轻松扛住
   - Lua 脚本保证"判断 + 扣减"原子性

2. **为什么用消息队列？**
   - 削峰：瞬间 10w 请求 → MQ 缓冲 → 消费者按数据库承载能力消费
   - 解耦：下单和扣库存异步，返回"排队中"给用户

3. **超卖怎么防？**
   - Redis Lua 脚本 + MySQL 行级锁 + 唯一索引三重保障

#### 阶段 3：生产级打磨（2-3 周）——简历高亮部分

| 能力 | 实现 |
|------|------|
| **分布式锁** | Redisson 解决集群环境下的并发安全 |
| **限流** | Sentinel 网关层 QPS 限流 + 热点参数限流 |
| **缓存** | 商品详情多级缓存（Nginx → Redis → DB），热点探测 |
| **数据一致性** | Canal 监听 binlog → 同步 Redis/ES，保证最终一致 |
| **分布式事务** | 下单 + 扣库存 + 发优惠券：Seata AT 模式或基于 MQ 的最终一致性 |
| **压测** | JMeter 模拟 1000 并发，输出 TPS/QPS/RT 报表 |

#### 阶段 4：运维部署（1 周）

```yaml
# docker-compose.yml
services:
  nginx: ...
  app: ...   # Spring Boot × 3 实例
  redis: ...
  mysql: ...
  rabbitmq: ...
  canal: ...
```

```bash
# 一键部署
docker-compose up -d
# 压测
jmeter -n -t seckill.jmx -l result.jtl
```

### 1.4 简历怎么写

```
项目：分布式电商秒杀系统 | 个人项目 | 202x.x - 202x.x

技术栈：Spring Boot、MyBatis-Plus、Redis、RabbitMQ、Canal、Sentinel、Docker

核心工作：
- 设计 Redis + Lua 预减库存方案，单接口 QPS 从 500 提升至 8000+
- 基于 RabbitMQ 实现异步下单削峰，消息积压时动态扩容消费者
- 使用 Sentinel 实现网关层限流，热点商品参数限流，系统在 3 倍峰值流量下不崩溃
- 通过 Canal + binlog 实现 MySQL → Redis → ES 的数据同步链路
- 压测报告：1000 并发下 TPS 8500，P99 延迟 < 200ms，0 超卖

亮点（面试准备）：
- 分布式锁 vs Redis Lua 原子操作的选型分析
- 库存扣减的最终一致性问题怎么解决的
- 如果 Redis 宕机了怎么办？降级方案是什么？
```

---

## 项目二：RAG 智能知识库系统

### 2.1 项目定位

> **面试官看到这个项目会想：** 这个人不只是会调 API，而是真正理解了 RAG 的完整 pipeline。

### 2.2 技术栈

```
后端：Spring Boot 3.x + Spring AI / LangChain4j
向量库：Milvus / Chroma（轻量可用 Milvus Lite）
模型：DeepSeek API（云端）+ Ollama + Qwen2.5（本地备份）
文档处理：Apache Tika / LangChain4j Document Loader
前端：Vue 3 + Element Plus（管理后台 + 问答界面）
```

### 2.3 分阶段实施

#### 阶段 1：最小可用（1 周）

- 上传 PDF/Markdown 文件 → 文本提取
- 文本分块（500 字符 + 50 重叠）→ Embedding → Milvus
- 用户输入问题 → Embedding → 向量检索 Top 5 → 拼接 Prompt → 调用 LLM
- 返回答案 + 引用来源

#### 阶段 2：检索优化（1-2 周）——体现你理解 RAG 的深度

| 优化手段 | 实现 | 效果 |
|----------|------|------|
| **混合检索** | 向量检索 + BM25 关键词检索（Elasticsearch）| 提升召回率 |
| **Rerank** | 使用 BGE-Reranker 对召回的 Top 20 重排序 | 提升精确率 |
| **查询重写** | 用户问题先用 LLM 扩展/分解为多个子查询 | 解决短查询问题 |
| **HyDE** | 先让 LLM 生成假设性答案 → 用答案做向量检索 | 解决语义鸿沟 |
| **上下文压缩** | 检索结果过长时，用 LLM 提取与问题最相关的片段 | Token 成本降低 |

#### 阶段 3：工程化（1-2 周）

```
完整的 RAG Pipeline：

文档上传 → Apache Tika 解析
              ↓
        智能分块（按段落 + 语义边界）
              ↓
        Embedding（BGE-M3 / text-embedding-3-large）
              ↓
        存入 Milvus（collection 管理 + 元数据过滤）
              ↓
用户提问 → 查询重写（可选）
              ↓
        混合检索（向量 + BM25）
              ↓
        Rerank 重排序
              ↓
        构建 Prompt Template → 调用 LLM
              ↓
        流式返回 + 引用来源高亮
              ↓
        用户反馈（👍/👎）→ 存入反馈数据集 → 未来微调
```

#### 阶段 4：部署与演示（1 周）

- Docker Compose 部署全栈
- 准备演示数据：上传 50+ 份 Java 技术文档，现场演示问答效果
- 压测检索延迟：P99 < 500ms

### 2.4 简历怎么写

```
项目：RAG 智能知识库系统 | 个人项目 | 202x.x - 202x.x

技术栈：Spring Boot、LangChain4j（或 Spring AI）、Milvus、DeepSeek、Elasticsearch

核心工作：
- 设计并实现完整 RAG Pipeline：文档解析 → 智能分块 → Embedding → 混合检索 → Rerank → 生成
- 采用混合检索（向量 + BM25）+ BGE-Reranker 重排序，检索 Top-5 准确率 87%
- 基于 LangChain4j 实现查询重写和 HyDE，解决短查询/模糊查询准确率低的问题
- 支持 10+ 种文档格式解析，流式返回 + 引用溯源

亮点（面试准备）：
- 为什么混合检索比纯向量检索好？什么场景 BM25 会优于向量？
- Chunk size 怎么选？太大/太小的 tradeoff 是什么？
- 如果文档更新了，怎么增量更新向量库？
```

---

## 项目三：AI 智能客服 Agent 系统

### 3.1 项目定位

> **面试官看到这个项目会想：** 这个人能设计复杂的 AI 系统，有产品思维，不只是写代码。

### 3.2 技术栈

```
后端：Spring Boot 3.x + LangChain4j（Agent 核心）+ Spring Security（多租户）
数据库：MySQL + Redis + Milvus（知识库）
消息队列：RabbitMQ（异步任务）
模型：DeepSeek API + Ollama 本地（敏感客户数据不出网）
前端：Vue 3 + Element Plus
```

### 3.3 分阶段实施

#### 阶段 1：基础对话（1 周）

- 多轮对话：ChatMemory 管理上下文
- 意图识别：用户问题 → LLM 分类 → "售后"/"咨询"/"投诉"
- 根据意图路由到不同处理逻辑

#### 阶段 2：Agent 核心（2-3 周）——项目灵魂

```
Agent 架构：

用户：我的订单 ORD-001 什么时候发货？
              ↓
    Agent（基于 ReAct 模式）
              ↓
   Thought: 需要查询订单信息
   Action: 调用 OrderQueryTool
   Observation: 订单 ORD-001 状态="已发货"，快递单号=SF123456
              ↓
   Thought: 用户还想知道快递到哪了
   Action: 调用 LogisticsQueryTool(SF123456)
   Observation: 快递到达"北京分拣中心"，预计明天送达
              ↓
   Final Answer: 您的订单已发货，快递 SF123456 已到达北京分拣中心，预计明天送达。
```

**自定义工具（Tools）：**

| 工具 | 实现 |
|------|------|
| `OrderQueryTool` | 查询 MySQL 订单表 |
| `LogisticsQueryTool` | 调用快递 100 API |
| `ProductSearchTool` | Elasticsearch 商品搜索 |
| `FAQSearchTool` | Milvus 向量检索常见问题 |
| `HumanTransferTool` | 创建人工客服工单 |

#### 阶段 3：企业级功能（2-3 周）

| 功能 | 技术方案 |
|------|----------|
| **多租户** | 每个企业独立的 knowledge base + 对话隔离 + 数据权限 |
| **敏感数据本地处理** | 涉及客户手机号/地址的请求路由到本地 Ollama，不调用云端 API |
| **对话安全** | Prompt 注入检测 + 敏感词过滤 + 输出审核 |
| **人机协同** | Agent 无法解决时自动创建工单 → 人工客服接手 → 对话记录同步 |
| **对话分析** | 统计高频问题、用户情绪、解决率，生成周报 |
| **成本控制** | 简单 FAQ 用本地 Qwen2.5 → 复杂问题用云端 DeepSeek → 按模型路由 |

#### 阶段 4：上线标准（1 周）

- Docker Compose 完整部署
- 演示脚本：模拟 5 个真实客服场景对话
- 准备一个 10 页 PPT：系统架构图 + Agent 决策链展示 + 运营数据看板

### 3.4 简历怎么写

```
项目：AI 智能客服 Agent 系统 | 个人项目 | 202x.x - 202x.x

技术栈：Spring Boot、LangChain4j、DeepSeek、Ollama、Milvus、RabbitMQ

核心工作：
- 基于 LangChain4j Agent + ReAct 模式构建智能客服，集成 6 个自定义 Tool（订单/物流/FAQ等）
- 设计多租户架构，支持企业级数据隔离；敏感数据自动路由到本地模型，不外传云端
- 实现人机协同机制：Agent 无法解决自动创建工单 → 人工客服无缝接管
- 内置 Prompt 注入检测 + 输出内容审核，通过安全合规审查
- 模型路由策略将 60% 简单问题降级到本地模型处理，API 成本降低约 40%

亮点（面试准备）：
- Agent 调用多个 Tool 时的错误处理？某个 Tool 挂了怎么 fallback？
- 如何评估 Agent 的回复质量？用什么指标？
- 多租户模式下，不同客户的 knowledge base 怎么隔离？
```

---

## 执行建议

### 时间规划（在校生版，每天 3 小时）

```
Week  1-8:  项目一（分布式秒杀）
Week  9-14: 项目二（RAG 知识库）  
Week 15-22: 项目三（智能客服 Agent）

同时进行：
  ├── 每天刷 1 道 LeetCode（30-45 分钟）
  └── 每周产出 1 篇项目技术博客（输出倒逼输入）
```

### 每个项目的交付标准

- [ ] **GitHub 开源**，README 有架构图 + 快速启动指南
- [ ] **Docker 一键部署**：`docker-compose up -d` 后即可访问
- [ ] **在线 Demo**：部署到云服务器，提供可访问地址
- [ ] **压测报告**：JMeter 结果截图 + TPS/QPS/P99 延迟
- [ ] **技术博客**：3 篇，讲述核心难点和解决方案
- [ ] **10 分钟答辩 PPT**：架构图 + 核心链路 + 难点 + 成果数据

### 常见坑（提前预警）

1. **秒杀项目**：不要一开始就搞分布式锁和 MQ，先跑通基础下单流程，再逐步加复杂度
2. **RAG 项目**：Chunk size 不是拍脑袋定的，要在你的文档上做实验（试 256/512/1024，对比检索准确率）
3. **Agent 项目**：Tool 定义要精确（参数类型、异常返回），否则 Agent 会"迷失"在错误信息里
4. **所有项目**：别想一次写完美，先把 MVP 跑起来，再逐步优化

---

## 总结

这三个项目形成了一个完整的叙事弧线：

> "我能做高并发 Java 后端（项目一）→ 我懂 AI 大模型落地（项目二）→ 我能把 Java + AI 做成商业产品（项目三）"

面试时不需要全部展示，根据面试岗位侧重讲一个，另外两个作为"我还做过"的补充。这三个项目的代码 + 博客 + 演示链接，构成你简历上最硬的一行。
