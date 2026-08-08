# 核心模式：Text-to-SQL 现状与演进

> Agent 让业务库"说人话"的关键技术。2026 年基准：学术基准已到 75%+（人类 93%），但企业真实 schema 上只有 ~39%——理解这道鸿沟，才知道自己的 Text-to-SQL Agent 该往哪里投入。

## 1. 什么是 Text-to-SQL（NL2SQL）

```text
用户："上月销量前十的商品是什么？"
   │  Text-to-SQL
   ▼
SQL: SELECT p.name, SUM(o.amount) FROM orders o
     JOIN products p ON o.product_id = p.id
     WHERE o.created_at >= date_trunc('month', now()) - interval '1 month'
     GROUP BY p.name ORDER BY SUM(o.amount) DESC LIMIT 10
   │  执行
   ▼
"1. 无线耳机 12,430 单 2. ..."
```

| 环节 | 内容 | 失败率来源 |
|---|---|---|
| 问题理解 | 实体、属性、过滤条件、聚合意图 | 歧义、口语 |
| Schema 链接 | 把词映射到真实表/列 | 同名列、缩写、脏命名 |
| SQL 生成 | 语法+方言+连接路径 | 多表连接错误 |
| 执行验证 | 跑出正确结果 | 脏数据、业务规则 |

> 🎯 核心要点：Text-to-SQL 不是"写 SQL"，是"从业务问题到正确查询的完整推理"——一半的失败发生在生成之前（理解与链接），一半在生成之后（业务规则不符）。

## 2. 基准现状：BIRD 是事实标准

| 基准 | 内容 | 现状（2026） |
|---|---|---|
| BIRD | 95 个真实库、12751 条问答，执行准确率 | 人类 92.96%；SOTA 75.63% |
| Spider | 学术合成库，无脏数据 | SOTA ~90%（已趋于饱和） |
| Spider 2.0 | 企业级复杂度（大 schema/真实脏数据） | SOTA <60% |
| BIRD-Ent | 4000+ 列、150 万 token 知识语料的企业版 | SOTA ~39.1% |

**BIRD 排行榜（执行准确率，2026-08 基准）**：

| 系统 | 准确率 | 特点 |
|---|---|---|
| XiYan-SQL（阿里云） | 75.63% | 多生成器集成 + 选择模型（IEEE TKDE 2026） |
| CYANSQL（腾讯云+复旦） | 73.47%（dev） | SQL 骨架聚类 + 测试时扩展（ICDE 2026） |
| Arctic-Text2SQL-R1（Snowflake 32B） | 71.83% | GRPO 强化学习 + 执行奖励 |
| Infly-RL-SQL-32B | 70.60% | 开源 RL 路线 |
| 本地开源 32B（复现基准） | ~51.76% | 即使带 schema 链接与自纠错 |

> 💡 数字口径警告：以上为 BIRD 执行准确率；有 2026 报告称端到端任务完成率仅 ~16%——那是更严格的"完整流程成功"口径，两者不矛盾，引用时务必说明口径。

## 3. 五大挑战：为什么企业落地远比基准难

| 挑战 | 表现 | 例子 |
|---|---|---|
| Schema 复杂度 | 几百张表、同义异名 | `orders` vs `order_info` vs `t_order` |
| 业务规则不在 schema | 口径藏在文档/人脑里 | "有效订单=已支付且未退款" |
| 脏数据 | 空值、类型错乱、历史遗留 | 金额存成字符串 |
| 多表连接歧义 | 连接路径不止一条 | 用户-订单-物流-支付 |
| 模型幻觉 | 编造列名/函数 | `SUM(price) OVER` 方言不存在 |

> 🎯 核心要点：基准考的"写对语法"，企业考的是"写对业务"——**context layers（指标口径、合法连接路径、业务规则）是 2026 年公认的破局杠杆**，比换更强的模型见效更快。

## 4. 2026 四大技术趋势

| 趋势 | 做法 | 代表 |
|---|---|---|
| 测试时扩展（Test-time Scaling） | 生成多个候选 SQL，按执行结果排序选优 | XiYan-SQL、CYANSQL |
| 强化学习 + 执行奖励 | 以"执行对不对"为奖励信号训练，小模型追平大模型 | Arctic-Text2SQL-R1（32B 追平 70B） |
| Schema 链接专项化 | 零样本/免训练链接器，先链接后生成 | EACL 2026 链接方法 |
| Context Layers 工程化 | 把口径/规则/样例注入上下文，按域拆分 | 企业落地主流 |

## 5. 工程化模式：三种落地姿势

### 5.1 直接生成（轻量，适合小库）

```text
system: 你是 SQL 专家，仅根据提供的 schema 生成 SELECT...
+ 完整 schema DDL + 少量 few-shot → 生成 SQL → 人工/规则校验 → 执行
```

- 适合：表少（<20）、schema 稳定、只读。
- 局限：schema 变了就翻车；上下文占 token 大。

### 5.2 多工具链式（主流，LangChain 模式）

```text
Agent 循环：list_tables → schema(相关表) → generate SQL
        → validate（AST 校验只读）→ execute → 报错则 rewrite（≤3 次）
```

- 即 [04 模块](04-工具封装实战：连接查询与返回.md) 的工具设计；LangChain `SQLDatabaseToolkit` 四工具即此模式。
- 优势：schema 按需加载、可自纠错、可审计。
- 关键：schema 一次别喂超过 ~20 张表（上下文溢出线）。

### 5.3 技能化渐进披露（2026 前沿）

```text
DeepAgents：AGENTS.md 声明 query-writing / schema-exploration 技能
Agent 需要时才 load_skill —— 上下文保持最小，任务才开始加载
```

- 把指标口径、join 路径、业务规则做成"技能"按需加载（[LangChain 官方 SQL 助手模式](https://docs.langchain.com/oss/python/langchain/multi-agent/skills-sql-assistant)）。
- 解决"全量上下文装不下企业知识"的根本矛盾。

## 6. 准确率提升清单（按 ROI 排序）

| 手段 | 提升效果 | 成本 |
|---|---|---|
| 喂 schema 样例行（每表 2-3 行） | 中-高 | 低 |
| 业务规则注入（口径文档化） | 高 | 中 |
| 执行反馈自纠错（错误回喂重写） | 中 | 低 |
| few-shot 检索（向量取相似历史成功 SQL） | 中 | 中 |
| 多候选生成+执行排序 | 高（+5-8 分） | 高（多倍模型调用） |
| schema 缓存+按域拆分 | 中（防上下文溢出） | 低 |
| 换更强推理模型 | 中 | 高（成本随模型档位） |

> 🎯 核心要点：先做"零模型成本"的工程项（样例行、业务规则、自纠错），再考虑多候选与换模型——工程杠杆在 2026 年比模型杠杆更划算。

---

**下一模块**：[03-安全第一：Agent 数据库访问防线](03-安全第一：Agent数据库访问防线.md)　**返回总览**：[00-数据库交互总览](00-数据库交互总览.md)

## 参考来源

- [XiYan-SQL: A Novel Multi-Generator Framework for Text-to-SQL（IEEE TKDE）](https://ieeexplore.ieee.org/abstract/document/11363445)
- [Smaller Models, Smarter SQL: Arctic-Text2SQL-R1 Tops BIRD（Snowflake）](https://www.snowflake.com/en/blog/engineering/arctic-text2sql-r1-sql-generation-benchmark/)
- [CYANSQL: Clustering-based Test-Time Scaling（Tencent Cloud）](https://intl.cloud.tencent.com/dynamic/blogs/sample-article/101165)
- [Talk to Your Data Tools in 2026（Promethium）](https://promethium.ai/guides/talk-to-your-data-tools-2026/)
- [What Is Text-to-SQL? Accuracy, Limitations, and How Context Layers Fix the Gaps（Kaelio）](https://www.kaelio.com/blog/what-is-text-to-sql-accuracy-limitations-context-layers)
- [Build a SQL assistant with on-demand skills（LangChain Docs）](https://docs.langchain.com/oss/python/langchain/multi-agent/skills-sql-assistant)
- [Text to SQL Agent with LangChain（GitHub langchain-ai）](https://github.com/langchain-ai/text-to-sql-agent/blob/main/agent.py)
