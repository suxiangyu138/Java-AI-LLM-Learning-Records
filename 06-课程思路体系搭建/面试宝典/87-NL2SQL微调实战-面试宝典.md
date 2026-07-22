# NL2SQL微调实战 面试宝典
> 基于课程大纲全面覆盖面试高频考点

## 目录
1. [一、基础概念速答（18题）](#一基础概念速答18题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码题（8题）](#四手写代码题8题)
5. [五、系统设计题（5题）](#五系统设计题5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（18题）

### Q1：什么是 NL2SQL？
> **一句话定义：** NL2SQL（Natural Language to SQL）是将自然语言查询自动转化为可执行 SQL 语句的技术，是 Text-to-SQL 在企业界的工业落地形态。

| 维度 | 说明 |
|------|------|
| 输入 | 自然语言（如"上个月销售额最高的产品"） |
| 输出 | 可执行的 SQL 查询语句 |
| 核心目标 | 降低数据库使用门槛，让非技术人员自助获取数据 |
| 所属领域 | NLU（自然语言理解）与结构化数据查询的交叉领域 |

### Q2：NL2SQL 的核心流程是什么？
```text
用户输入（自然语言）
    │
    ▼
┌─────────────────┐
│  Schema Linking  │  ← 将 NL 中的实体映射到数据库表名/字段名/关系
└────────┬────────┘
         │
┌────────▼────────┐
│  SQL Generation  │  ← 模型基于映射结果生成 SQL 语句
└────────┬────────┘
         │
┌────────▼────────┐
│  SQL Execution   │  ← 在目标数据库上执行 SQL 并返回结果
└─────────────────┘
```

### Q3：NL2SQL 在企业中有哪些应用场景？
| 场景 | 说明 | 典型问题 |
|------|------|----------|
| BI 自助分析 | 业务人员直接提问获取报表 | "Q3 华东区各品类销售额占比" |
| 数据治理 | 自动探查数据质量 | "哪些字段空值率超过 5%" |
| 报表自动化 | 替代手工编写固定报表 | "按部门统计本月考勤异常数" |
| 智能客服 | 结合后台数据回答用户 | "订单 OD2024001 当前物流状态" |
| 运营分析 | 快速验证数据假设 | "最近 7 天日活跃用户趋势" |

### Q4：为什么要微调 NL2SQL 而不是直接用 Prompt 调用大模型？
> 💡 **核心原因：** 通用大模型缺乏对特定数据库 Schema 和业务术语的深入理解，微调可以实现知识内化。

| 对比维度 | Prompt 直接调用 | Fine-Tuning 微调 |
|----------|----------------|------------------|
| Schema 理解 | 依赖上下文注入，受限于 Token 长度 | 内化到模型参数，无长度限制 |
| 推理成本 | 每次调用 Token 消耗大 | 短 Prompt，成本低 |
| 时延 | 长 Prompt 导致首 Token 延迟高 | 短 Prompt，首 Token 延迟低 |
| 稳定性 | 对 Prompt 措辞敏感，波动大 | 输出稳定，可控性强 |
| 私有化部署 | 依赖云端 API，数据有外泄风险 | 完全私有化部署 |
| 领域术语 | 无法理解企业缩写和业务黑话 | 通过训练数据学习领域术语 |

### Q5：微调 NL2SQL 的前期准备有哪些？
| 准备项 | 说明 | 优先级 |
|--------|------|--------|
| 高质量数据集 | 自然语言-SQL 对，至少 1000+ 条 | ⭐⭐⭐⭐⭐ |
| 数据库 Schema | 表结构、字段注释、外键关系、值域信息 | ⭐⭐⭐⭐⭐ |
| 评估数据集 | 独立的验证集和测试集 | ⭐⭐⭐⭐ |
| 基座模型 | 如 Qwen2.5-Coder、DeepSeek-Coder、CodeLlama | ⭐⭐⭐⭐ |
| 计算资源 | GPU 显存至少 16GB（单卡 4090 可训 7B） | ⭐⭐⭐ |
| 微调框架 | LLaMA Factory（推荐）、FastChat、Axolotl | ⭐⭐⭐ |

### Q6：什么是 Schema Linking？为什么重要？
**Schema Linking（模式链接）** 是将自然语言中的实体映射到数据库 Schema（表名、字段名、关系）的过程。它是 NL2SQL 准确性的**最大瓶颈**——表选错或字段映射错误，后续生成不可能正确。

**三个子任务：**
1. **表选择**：从多表数据库中选出最相关的表
2. **列链接**：将 NL 关键词映射到具体字段
3. **值识别**：识别 NL 中的具体数值和条件（如"上个月" → `DATE_TRUNC('month', NOW()) - INTERVAL '1 month'`）

### Q7：什么是 LoRA 微调？
> **LoRA（Low-Rank Adaptation）** 是一种参数高效的微调方法，通过在预训练权重旁添加低秩分解矩阵来适配下游任务。

**数学原理：** `h = W₀·x + B·A·x`
- `W₀∈R^(d×k)` 原始权重（冻结）
- `A∈R^(r×k)`, `B∈R^(d×r)`, `r<<min(d,k)`
- 通常 r=8~64，训练参数仅为总参数的 0.1%~1%

| 优势 | 说明 |
|------|------|
| 显存低 | 单卡 4090 可微调 7B 模型 |
| 训练快 | 比全参数微调快 5~10 倍 |
| 切换灵活 | 多个 LoRA 权重可在同一基座模型上动态切换 |
| 过拟合风险低 | 参数量少，不易过拟合 |

### Q8：NL2SQL 数据集通常包含哪些字段？
```json
{
  "question": "查询2024年销售额超过100万的客户名称",
  "sql": "SELECT c.name FROM customers c JOIN orders o ON c.id = o.customer_id WHERE o.total > 1000000 AND YEAR(o.order_date) = 2024",
  "db_id": "sales_db",
  "tables_used": ["customers", "orders"],
  "difficulty": "hard",
  "evidence": "条件为2024年且销售额超过100万"
}
```

### Q9：常用的 NL2SQL 公开数据集有哪些？
| 数据集 | 规模 | 特点 | 适用场景 |
|--------|------|------|----------|
| **Spider** | 10,181 条 / 200+ 库 | 多表复杂查询，学术界标准基准 | 模型选型与横向对比 |
| **WikiSQL** | 80,654 条 | 单表简单查询 | 入门训练、基线建立 |
| **Bird-SQL** | 12,751 条 / 95 库 | 真实业务场景，含外部知识 | 企业级评测 |
| **CSpider** | 8,659 条 | Spider 的中文翻译版 | 中文 NL2SQL 评测 |
| **DuSQL** | 23,931 条 | 中文复杂查询，覆盖 200+ 库 | 中文业务系统训练 |

### Q10：LLaMA Factory 是什么？为什么适合 NL2SQL？
> **LLaMA Factory** 是一个开源的大模型微调框架，支持 LoRA、QLoRA、全参数微调。

**为什么适合 NL2SQL：**
1. **开箱即用**：支持 Qwen、LLaMA、ChatGLM 等主流模型
2. **配置化**：YAML 配置文件管理所有超参数
3. **数据格式灵活**：支持 Alpaca、ShareGPT、Belle 等多种格式
4. **内置评估**：训练中自动计算 Loss 并支持验证集评估
5. **模型导出**：支持合并 LoRA 权重，导出 HuggingFace 格式

### Q11：NL2SQL 的主要技术路线有哪些？
| 路线 | 方法 | 优势 | 劣势 |
|------|------|------|------|
| **Prompt-based** | Few-shot + ICL | 快速部署，无需训练成本 | 效果受限于上下文窗口 |
| **Fine-tuning** | LoRA / QLoRA 微调 | 领域适配好，输出稳定 | 需要训练数据和 GPU |
| **Agent-based** | 多步推理 + 工具调用 | 适合复杂多步查询 | 延迟高，成本高 |
| **Hybrid** | 微调模型 + Prompt 兜底 | 效果和泛化兼顾 | 系统复杂，维护成本高 |

### Q12：NL2SQL 的评估指标有哪些？
| 指标 | 全称 | 含义 | 说明 |
|------|------|------|------|
| **EX** | Execution Accuracy | 预测 SQL 执行结果与标准结果一致 | 工业最常用，容忍写法差异 |
| **EM** | Exact Set Match | 预测结果集合与标准结果完全一致 | 学术常用，顺序不敏感 |
| **VES** | Exact Value Match | 预测结果中每个字段值都与标准一致 | 粒度最细的匹配 |

> ⚠️ **面试重点：** EX 是工业界最看重的指标，关注"结果对不对"，不纠结"写法是否一致"。

### Q13：什么是 DuckDB？在 NL2SQL 中为什么常用？
**DuckDB** 是嵌入式列式 OLAP 数据库，无需安装服务端。用于：
- **SQL 验证沙箱**：快速验证生成 SQL 的语法和语义
- **训练环境**：内存中创建数据库，加载 CSV/Parquet 数据
- **自动化评估**：批量执行预测 SQL 并与标准结果对比

### Q14：Few-shot 学习在 NL2SQL 中如何应用？
1. **静态示例**：Prompt 中固定放 2~5 个（问题, SQL）对
2. **动态检索**：基于向量相似度检索最相关的示例
3. **难度适配**：简单问题给 2 个示例，复杂问题给 5 个

### Q15：什么是一键生成 NL2SQL 数据集？
> 利用大模型（GPT-4、Claude、Qwen-Max）根据 Schema 自动生成批量 NL-SQL 训练对。

**流程：** `Schema 定义 → LLM 批量生成 → SQL 自动验证 → 人工抽样校验 → 最终数据集`

可将数据集构建时间从数周缩短到数天。

### Q16：如何处理 NL2SQL 中的模糊列名？
| 策略 | 方法 | 效果 |
|------|------|------|
| 注释增强 | Schema 中添加详细业务含义注释 | 高 |
| 同义词词典 | 建立口语到字段名的映射表 | 中高 |
| 训练覆盖 | 训练数据中覆盖同一字段的不同问法 | 中 |
| Schema Linking 模块 | 开发独立的列名对齐模块 | 高（成本也高） |

### Q17：NL2SQL 如何处理复杂 JOIN 查询？
1. **关系图构建**：基于外键构建数据库表关系图
2. **路径推理**：从目标字段出发推理最短 JOIN 路径
3. **显式标注**：Schema 中标注 JOIN 条件和关系说明
4. **训练覆盖**：训练数据中按比例加入多表 JOIN 样本

### Q18：什么是嵌套查询？NL2SQL 如何处理？
嵌套查询是 SQL 中包含子查询（Subquery）的复杂语句。挑战在于：
1. **语义边界**：理解子查询的语义边界和作用域
2. **内外关联**：子查询与外部查询的关联条件
3. **操作符选择**：正确选择 EXISTS / IN / ANY / ALL

> 🎯 **基础概念小结：** Q1~Q4 是必答基础，Q6（Schema Linking）、Q7（LoRA）、Q12（评估指标）是高频追问点。

---

## 二、深度原理剖析（12题）

### Q19：NL2SQL 微调的技术路线如何选择？
> 🎯 **三大核心选择：基座模型、参数量、微调策略**

**基座模型推荐：**
| 模型 | 优势 | 推荐场景 |
|------|------|----------|
| **Qwen2.5-Coder** | 代码强、中文好、7B~32B | 中文企业场景首选 |
| **DeepSeek-Coder** | 竞赛级代码生成 | 英文/代码优先场景 |
| **CodeLlama** | Meta 出品，社区成熟 | 英文场景 |
| **SQLCoder** | 专为 SQL 优化 | 纯 SQL 生成任务 |

**参数量选择：**
- **7B**：单卡 4090 可训，适合单表简单过滤查询
- **14B**：两卡 4090 或单卡 A100，适合多表 JOIN + GROUP BY
- **32B~34B**：多卡 A100，适合复杂嵌套查询

**微调策略：**
- **LoRA**：性价比最高，推荐 rank=16~32
- **QLoRA**：4-bit 量化训练，显存再降 60%，精度损失约 1~2%
- **Full Fine-tuning**：效果最优但成本高，需多卡 A100

### Q20：如何构建 NL2SQL 训练数据集？
| 构建方式 | 成本 | 质量 | 适用阶段 | 说明 |
|----------|------|------|----------|------|
| 模板生成 | 低 | 中 | 冷启动 | 定义 SQL 模板 + 随机参数填充 |
| LLM 生成 | 中 | 中高 | 快速扩展 | 用 GPT-4/Qwen 生成，需人工校验 |
| 人工标注 | 高 | 高 | 精调阶段 | DBA 或分析师手工编写 |
| 日志清洗 | 低 | 高 | 运营阶段 | 从历史查询日志中提取 |
| 对抗生成 | 中 | 高 | 效果优化 | 识别薄弱点定向生成 |

### Q21：LoRA 微调的超参数配置
```yaml
# 推荐的 NL2SQL LoRA 配置
lora:
  r: 32                     # rank=32 比默认 8 更适合 NL2SQL
  lora_alpha: 64            # alpha = 2 * r
  lora_dropout: 0.05
  target_modules:
    - q_proj
    - k_proj
    - v_proj
    - o_proj

training:
  learning_rate: 2e-4       # LoRA 常用学习率
  num_train_epochs: 3
  per_device_batch_size: 4
  gradient_accumulation_steps: 4  # 等效 batch_size = 16
  warmup_ratio: 0.05
  lr_scheduler: cosine
  bf16: true
```

### Q22：Schema Linking 的实现策略对比
| 策略 | 准确率 | 延迟 | 实现难度 |
|------|--------|------|----------|
| **基于规则**：关键词+同义词词典 | 中 | 低 | 低 |
| **基于 Embedding**：字段描述语义检索 | 较高 | 中 | 中 |
| **基于 LLM**：Schema 输入 LLM 选择 | 高 | 高 | 低 |
| **混合策略**：规则粗筛→Embedding精排→LLM兜底 | 最高 | 中 | 高 |

### Q23：NL2SQL 微调的损失函数设计
**标准方案：** Causal LM Loss（自回归交叉熵损失）。

**优化方案：**
1. **SQL 加权 Loss**：SQL 部分 Token 更高权重（α=0.3 NL + β=0.7 SQL）
2. **Schema Loss Masking**：对 Prompt 中 Schema 部分的 Loss 做掩码
3. **对比学习增强**：拉近正确 SQL 与预测结果的 Embedding 距离

### Q24：如何处理复杂 JOIN 场景？
**核心挑战：** 多表关系推断、最短 JOIN 路径选择、多对多关系处理。

**解决方案：**
1. **关系图显式注入**：Schema 中加入外键关系描述
2. **BFS 路径搜索**：从目标字段出发搜索最短 JOIN 路径
3. **训练覆盖**：训练数据中按比例加入 2 表、3 表、4 表 JOIN

### Q25：如何处理字段名歧义？
| 歧义类型 | 示例 | 解决策略 |
|----------|------|----------|
| 同名不同义 | `status` 在订单表和库存表含义不同 | 字段名前加表名前缀 |
| 同义不同名 | `amount` vs `total` vs `sum` | 建立同义词词典 |
| 缩写歧义 | `amt`、`qty`、`cnt` | Schema 注释中保留全称 |
| 业务术语 | 不同部门对"收入"定义不同 | 注释中标注具体含义 |

### Q26：什么是 Value Mismatch？如何解决？
**Value Mismatch** 指自然语言中的条件值与数据库实际存储值不一致。

**常见原因：** 时间表达不一致、单位不一致、别名不一致、格式不一致。

**解决方案：**
1. **值标准化**：训练值归一化模块
2. **值域注入**：Schema 中枚举字段值域
3. **模糊匹配**：LIKE、相似度函数替代精确 `=` 匹配

### Q27：如何处理 Out-of-Distribution（OOD）查询？
**OOD** 指模型在训练阶段未见过的查询类型。

**处理策略：**
1. **拒绝机制**：置信度低于阈值时主动拒绝
2. **降级兜底**：OOD 查询降级到 Few-shot Prompt 方案
3. **在线学习**：采集 OOD 查询，人工标注后加入下一轮训练
4. **数据增强**：训练阶段通过模板组合增加边缘案例

### Q28：嵌套查询如何处理？
**训练策略：**
1. 训练数据中按 20%~30% 比例加入嵌套查询
2. 使用中间表示（SemQL、IRNet）降低生成难度
3. 分步生成：先生成主查询，再生成子查询

### Q29：如何处理灾难性遗忘（Catastrophic Forgetting）？
**缓解方案：**
1. **混合训练**：加入 10%~20% 通用指令数据
2. **EWC（Elastic Weight Consolidation）**：对重要参数施加约束
3. **LoRA 低 rank**：rank 越低，对原始权重破坏越小
4. **多任务学习**：同时训练 NL2SQL + 通用对话

### Q30：LLaMA Factory 微调 Pipeline 内部原理
```text
数据加载 → Tokenization → Padding/Truncation
    ↓
模型加载 → QLoRA 量化（可选）
    ↓
训练循环：
┌──────────────────────────────────────────┐
│  1. Forward Pass                          │
│     - Token Embedding                     │
│     - Transformer Layers + LoRA Adapter   │
│     - LM Head → Logits                   │
│  2. Loss 计算 (Cross-Entropy)             │
│  3. Backward Pass (仅更新 LoRA 参数)      │
│  4. Optimizer Step (AdamW)               │
│  5. Gradient Clipping                     │
└──────────────────────────────────────────┘
    ↓
Checkpoint 保存 → 评估 → 模型导出
```

> 🎯 **深度原理小结：** 面试中 Q19（技术路线）、Q21（LoRA配置）、Q22（Schema Linking）、Q29（灾难性遗忘）是高频追问方向。

---

## 三、实战场景题（10题）

### Q31：一个月内交付 NL2SQL 问数系统，如何规划？
| 阶段 | 时间 | 任务 | 产出 |
|------|------|------|------|
| 第 1 周 | 7 天 | Schema 梳理 + 数据集构建（2000 条） | 可用训练数据 |
| 第 2 周 | 7 天 | LoRA 微调 + 基础评估 | 初版模型（EX ≥ 70%） |
| 第 3 周 | 7 天 | API 封装 + 前端 Demo | 可演示原型 |
| 第 4 周 | 7 天 | Bad Case 修复 + 部署 | 生产级模型（EX ≥ 85%） |

**关键决策：** Qwen2.5-Coder-7B + LLaMA Factory LoRA + vLLM 部署，聚焦 5~10 张核心表。

### Q32：金融风控百张大表场景如何设计？
**挑战：** 表多（100+）、精度要求高、安全合规严格。

**解决方案：**
1. **分层 Schema Linking**：领域路由 → 子领域 Top-5 → 字段级精细链接
2. **安全约束层**：预置 WHERE 权限过滤、敏感字段白名单、SQL 注入检测
3. **执行沙箱**：只读副本、查询超时 30s、结果上限 10000 行

### Q33：电商多轮对话 NL2SQL 如何实现？
**核心挑战：** 维护上下文状态，跨轮引用前文。

**实现方案：**
1. **上下文压缩**：每轮后摘要为结构化上下文
2. **指代消解**：识别本轮对前文实体的指代
3. **SQL 改写**：基于历史 SQL 和当前问题直接改写

### Q34：字段命名不规范（a001、b_name_cn）如何处理？
| 方法 | 描述 | 维护成本 |
|------|------|----------|
| 字段注释增强 | Comment 中补充业务含义 | 低 |
| 元数据词典 | 字段名到业务术语的映射表 | 中 |
| Schema 别名 | SQL 中用 AS 映射 | 低 |
| Name Normalizer | 训练翻译模型规范化字段名 | 高 |

### Q35：如何评估 NL2SQL 系统上线效果？
**技术指标：** EX ≥ 90%、语法正确率 ≥ 95%、P99 延迟 < 2s、首轮准确率 ≥ 80%。

**业务指标：** 用户采纳率、人工介入率（目标 < 10%）、平均解决时间、NPS。

### Q36：BI 系统（Tableau、Metabase）如何集成 NL2SQL？
```text
用户提问 → NL2SQL API → SQL 生成 → BI 系统 REST API → 数据返回 → 可视化
```
NL2SQL 作为 NLP Query Layer 独立部署，通过 BI 系统 API 执行 SQL，复用已有图表组件。

### Q37：如何保证 NL2SQL 生成的 SQL 安全？
| 层级 | 措施 | 说明 |
|------|------|------|
| 输入层 | NL 输入过滤 | 检测 SQL 注入尝试 |
| 模型层 | 安全微调 | 训练数据中加入拒绝不安全查询的样本 |
| 输出层 | 语法树校验 | 检测 DDL/DML 等危险操作 |
| 执行层 | 只读沙箱 | 只读副本执行，设置超时和结果上限 |
| 审计层 | 全量日志 | 记录所有用户查询和生成 SQL |

### Q38：NL2SQL 如何在数据库方言间迁移？
| 操作 | MySQL | PostgreSQL | DuckDB |
|------|-------|------------|--------|
| 字符串连接 | `CONCAT(a,b)` | `a\|\|b` | `a\|\|b` |
| 日期格式化 | `DATE_FORMAT` | `TO_CHAR` | `STRFTIME` |
| 分页 | `LIMIT x OFFSET y` | `LIMIT x OFFSET y` | `LIMIT x OFFSET y` |

**迁移方案：** 微调时方言注入、中间表示转换、Post-processing 规则替换。

### Q39：如何用 DuckDB 搭建本地验证环境？
```python
import duckdb
conn = duckdb.connect(':memory:')
conn.execute("CREATE TABLE orders AS SELECT * FROM 'orders.csv'")
# 验证 SQL 正确性
result = conn.execute("SELECT COUNT(*) FROM orders WHERE amount > 1000").fetchone()
# 批量评估 EX
def compute_ex(preds, golds, conn):
    correct = 0
    for p, g in zip(preds, golds):
        try:
            if conn.execute(p).fetchall() == conn.execute(g).fetchall():
                correct += 1
        except Exception:
            pass
    return correct / len(preds)
```

### Q40：微调效果不佳如何系统性排查？
```text
Step 1: 整体指标 → EX < 80%？需要优化
Step 2: 按难度分层 → 简单 OK？复杂不 OK？
Step 3: 按操作类型 → JOIN 差？GROUP BY 差？
Step 4: 按表维度 → 哪些表表现差？
Step 5: Bad Case 聚类 → 人工看 50 条，分类统计
Step 6: 针对性优化
```

**常见方向：** 数据不足→扩充、质量差→清洗、Schema 缺失→增强、基座不对→换模型。

> 🎯 **实战场景核心：** 企业落地关键在于"数据 + 安全 + 迭代"三位一体，模型只是中间环节。

---

## 四、手写代码题（8题）

### Q41：LLaMA Factory 训练 YAML 配置
```yaml
# qwen_nl2sql_lora.yaml
model_name_or_path: /models/Qwen2.5-Coder-7B-Instruct
template: qwen
trust_remote_code: true

method: lora
lora_rank: 32
lora_alpha: 64
lora_dropout: 0.05
lora_target:
  - q_proj
  - k_proj
  - v_proj
  - o_proj

dataset: nl2sql_train
dataset_dir: ./data/nl2sql
val_size: 0.1
max_samples: 10000

output_dir: ./output/qwen7b-nl2sql-lora
per_device_train_batch_size: 4
per_device_eval_batch_size: 8
gradient_accumulation_steps: 4
learning_rate: 2e-4
num_train_epochs: 3
logging_steps: 10
save_steps: 200
warmup_ratio: 0.05
lr_scheduler_type: cosine
bf16: true
gradient_checkpointing: true
save_total_limit: 3
```

### Q42：训练数据格式（ShareGPT 风格 JSON）
```json
[
  {
    "conversations": [
      {
        "from": "system",
        "value": "你是一个 SQL 专家。请根据数据库 Schema 将自然语言转换为 SQL 查询。"
      },
      {
        "from": "human",
        "value": "数据库有 orders(id, customer_name, amount, order_date) 和 customers(id, name, city)。\n查询在北京的客户中订单金额超过 1000 元的客户名和订单金额。"
      },
      {
        "from": "gpt",
        "value": "```sql\nSELECT c.name, o.amount\nFROM customers c\nJOIN orders o ON c.id = o.customer_id\nWHERE c.city = '北京' AND o.amount > 1000;\n```"
      }
    ]
  }
]
```

### Q43：NL2SQL 评估指标计算器（Python）
```python
import sqlite3
from typing import List, Dict, Tuple

class NL2SQLEvaluator:
    """支持 EX、EM、VES 三项指标的评估器"""

    def __init__(self, db_path: str):
        self.conn = sqlite3.connect(db_path)

    def ex(self, preds: List[str], golds: List[str]) -> float:
        """Execution Accuracy：比较执行结果"""
        correct = 0
        for p, g in zip(preds, golds):
            try:
                if self.conn.execute(p).fetchall() == self.conn.execute(g).fetchall():
                    correct += 1
            except Exception:
                pass
        return correct / len(preds)

    def em(self, preds: List[str], golds: List[str]) -> float:
        """Exact Set Match：比较结果集合"""
        correct = 0
        for p, g in zip(preds, golds):
            try:
                p_set = sorted(self.conn.execute(p).fetchall())
                g_set = sorted(self.conn.execute(g).fetchall())
                if p_set == g_set:
                    correct += 1
            except Exception:
                pass
        return correct / len(preds)

    def ves(self, preds: List[str], golds: List[str]) -> float:
        """Exact Value Match：比较每个字段值"""
        correct = 0
        for p, g in zip(preds, golds):
            try:
                if self.conn.execute(p).fetchall() == self.conn.execute(g).fetchall():
                    correct += 1
            except Exception:
                pass
        return correct / len(preds)

    def full_report(self, preds: List[str], golds: List[str]) -> Dict:
        return {
            "EX": round(self.ex(preds, golds), 4),
            "EM": round(self.em(preds, golds), 4),
            "VES": round(self.ves(preds, golds), 4),
            "samples": len(preds),
        }
```

### Q44：Few-shot Prompt 模板设计
```python
def build_few_shot_prompt(schema_text: str, examples: List[Dict], query: str) -> str:
    """动态构建 Few-shot Prompt"""
    prompt = """根据数据库 Schema 和参考示例，将用户问题转换为 SQL 查询。

## 数据库 Schema
{schema_tables}

## 参考示例
{few_shot_examples}

## 用户问题
{user_query}

## SQL 输出
```sql
"""
    formatted_examples = []
    for i, ex in enumerate(examples, 1):
        formatted_examples.append(
            f"### 示例{i}\n问题：{ex['question']}\nSQL：{ex['sql']}"
        )
    return prompt.format(
        schema_tables=schema_text,
        few_shot_examples="\n\n".join(formatted_examples),
        user_query=query,
    )
```

### Q45：vLLM 推理服务部署
```python
from vllm import LLM, SamplingParams

class NL2SQLInference:
    """基于 vLLM 的 NL2SQL 推理引擎"""

    def __init__(self, model_path: str):
        self.llm = LLM(
            model=model_path,
            gpu_memory_utilization=0.9,
            max_model_len=4096,
            trust_remote_code=True,
        )
        self.params = SamplingParams(temperature=0.1, top_p=0.9, max_tokens=512)

    def predict(self, query: str, schema: Dict) -> str:
        schema_text = "\n".join(
            f"- {t['name']}({t.get('comment', '')})"
            for t in schema.get("tables", [])
        )
        prompt = f"数据库 Schema：\n{schema_text}\n\n用户查询：{query}\n\nSQL："
        outputs = self.llm.generate([prompt], self.params)
        return outputs[0].outputs[0].text.strip()
```

### Q46：NL2SQL 数据增强器
```python
import random
from typing import List, Dict

class NL2SQLAugmenter:
    """NL2SQL 数据增强：改写 + 等价变换"""

    @staticmethod
    def paraphrase(question: str) -> str:
        templates = [
            lambda q: f"请查询{q}", lambda q: f"我想知道{q}",
            lambda q: q, lambda q: f"统计一下{q}",
        ]
        return random.choice(templates)(question)

    def augment(self, dataset: List[Dict], multiplier=3) -> List[Dict]:
        augmented = []
        for sample in dataset:
            augmented.append(sample)
            for _ in range(multiplier - 1):
                augmented.append({
                    "question": self.paraphrase(sample["question"]),
                    "sql": sample["sql"],
                })
        return augmented
```

### Q47：SQL 安全检查器
```python
import sqlparse

class SQLSafetyChecker:
    """NL2SQL 输出 SQL 的安全校验"""

    FORBIDDEN = {"DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "TRUNCATE", "CREATE", "GRANT"}

    def check(self, sql: str) -> tuple[bool, str]:
        parsed = sqlparse.parse(sql)[0]
        for token in parsed.flatten():
            if token.value.upper() in self.FORBIDDEN:
                return False, f"禁止操作: {token.value}"
        if parsed.get_type().upper() != "SELECT":
            return False, f"只允许 SELECT，当前: {parsed.get_type()}"
        return True, "安全"

    def sanitize(self, sql: str, max_rows=1000) -> str:
        sql = sql.rstrip().rstrip(";")
        if "LIMIT" not in sql.upper():
            sql += f" LIMIT {max_rows}"
        return sql
```

### Q48：NL2SQL 实验管理
```python
import json, datetime
from pathlib import Path
from dataclasses import dataclass, asdict

@dataclass
class NL2SQLExperiment:
    """微调实验记录"""
    base_model: str
    method: str           # lora / qlora / full
    lora_rank: int = 32
    learning_rate: float = 2e-4
    epochs: int = 3
    dataset_size: int = 0
    metrics: dict = None
    notes: str = ""

    def save(self, path="./experiments/"):
        Path(path).mkdir(exist_ok=True)
        exp_id = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        with open(Path(path) / f"{exp_id}.json", "w") as f:
            json.dump(asdict(self), f, indent=2)
```

> 🎯 **代码题核心：** Q41（LLaMA Factory 配置）、Q43（评估实现）、Q44（Few-shot Prompt）是最高频面试手写题。

---

## 五、系统设计题（5题）

### Q49：设计企业级 NL2SQL 微调平台
```text
┌──────────────────────────────────────────────────┐
│                   Web UI                           │
│  [数据集管理] [模型训练] [模型评估] [模型部署] [监控] │
└────────────────────┬─────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────┐
│               Core Services                        │
│ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────────┐  │
│ │数据集管理│ │训练引擎 │ │评估引擎 │ │  推理服务   │  │
│ │数据校验 │ │LLaMA   │ │EX/EM   │ │  vLLM     │  │
│ │数据增强 │ │Factory  │ │VES/Bad │ │  Ollama   │  │
│ │版本管理 │ │实验跟踪 │ │Case分析│ │  负载均衡  │  │
│ └────────┘ └────────┘ └────────┘ └────────────┘  │
└────────────────────┬─────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────┐
│             Infrastructure                         │
│   [GPU Cluster] [对象存储] [MySQL] [Redis] [监控]   │
└──────────────────────────────────────────────────┘
```

| 模块 | 职责 | 技术选型 |
|------|------|----------|
| 数据集管理 | Schema 管理、数据版本控制 | S3/MinIO + Git LFS |
| 训练引擎 | 任务调度、超参搜索、实验管理 | LLaMA Factory + Optuna |
| 评估引擎 | 多指标评估、Bad Case 聚类分析 | DuckDB + 标注平台 |
| 推理服务 | 模型部署、自动扩缩容、A/B测试 | vLLM + K8s |

### Q50：设计高可用 NL2SQL 推理服务
```text
负载均衡 (Nginx/ALB) → 推理集群 (vLLM+K8s) → 结果缓存 (Redis) → SQL 执行引擎 → 监控
```

**关键设计：**
1. **预热机制**：新模型部署前发送预定义查询预热
2. **降级策略**：高负载切换到轻量模型或 Prompt 方案兜底
3. **缓存策略**：相同查询缓存，按数据更新频率设 TTL
4. **分片推理**：vLLM Tensor Parallel 支持多卡推理

### Q51：设计 NL2SQL 持续优化反馈闭环
```text
用户查询 → NL2SQL推理 → SQL执行 → 结果返回
                                   │
                             用户反馈 (点赞/点踩)
                                   ▼
                             日志采集 → Bad Case检测
                                   ▼
                         入库 + 自动标注 → 增量训练 → A/B评估 → 发布
```

**反馈策略：** 执行正确的 SQL 自动标注、低置信度查询 Self-training、Active Learning 优先标注最困惑查询、回归测试防止退化。

### Q52：设计跨业务线统一 NL2SQL 方案
**多租户架构：** 共享基座模型 + 按租户隔离 LoRA Adapter + 动态切换。

1. **Base Model 共享**：所有租户共享一个基座模型
2. **LoRA Adapter 隔离**：每个租户独立权重
3. **动态切换**：根据请求中租户 ID 切换 Adapter
4. **Schema 注册中心**：每个租户独立元数据

### Q53：设计 NL2SQL 分布式训练系统
| 模型大小 | 训练策略 | GPU 需求 | 预计时间（万条数据） |
|----------|----------|----------|---------------------|
| 7B | LoRA 单卡 | 1x A100/4090 | 2~4 小时 |
| 14B | LoRA 双卡 | 2x A100 | 4~6 小时 |
| 32B | QLoRA 单卡 | 1x A100-80G | 6~8 小时 |
| 34B+ | FSDP 全参 | 8x A100 | 24~48 小时 |

**关键组件：** Data Pipeline（WebDataset）、Orchestrator（Ray/Slurm）、Checkpoint Manager、Model Registry。

> 🎯 **系统设计核心：** 重点阐述"数据飞轮"理念——模型越好用户越爱用，反馈越多模型越好。

---

## 六、常见坑点与最佳实践

### 6.1 常见坑点与解决方案

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| Loss 下降但评估指标不升 | 过拟合训练数据 | 增加数据多样性，早停，增大 Dropout |
| SQL 语法正确但业务语义错误 | 模型未理解业务规则 | Prompt 中注入业务规则，增加约束样本 |
| 字段名过度敏感 | 字段名在训练集中频次不均衡 | 字段名脱敏处理，使用注释替代字段名 |
| 数据集越大效果反而下降 | 低质量样本污染模型 | 严格数据清洗，只保留高质量 NL-SQL 对 |
| LoRA 后灾难性遗忘 | 微调数据与预训练分布差异大 | 加入 10%~20% 通用数据混合训练 |
| 多表 JOIN 准确率低 | 模型不理解表间关系 | Schema 加入外键描述 |
| 推理延迟高 | Attention 计算开销大 | 使用 vLLM PagedAttention，模型量化 |
| OOD 查询无法处理 | 训练数据未覆盖 | 建立降级机制，Prompt 方案兜底 |
| 微调后通用能力退化 | 单一任务训练 | LoRA 低 rank + EWC 正则化 |
| DB 方言不兼容 | 训练数据只用一种方言 | 混入多种方言，Post-processing 规则转换 |
| 中文编码错误 | 分词器对中文支持不足 | 使用中文预训练模型 |
| NULL 值处理遗漏 | 训练数据缺少 NULL 样本 | 增加 IS NULL / COALESCE 样本 |
| GROUP BY 遗漏非聚合列 | SQL 语法规则常见错误 | 规则后处理校验 |
| 温度过高输出不稳定 | Temperature 设置不合理 | 生产环境设为 0.1~0.2 |
| 训练/评估数据泄露 | 同数据同时出现在训练和测试集 | 严格按时间/数据源划分 |
| 业务术语未覆盖 | 企业特有缩写未在训练数据中 | 业务术语词典 + 术语全覆盖 |

### 6.2 最佳实践清单

**数据层面：**
- 质量优先于数量：3000 条高质量 > 10 万条有噪声
- Schema 注释详细度直接影响模型表现
- 覆盖 80% 常见查询 + 20% 复杂边界
- 定期从生产环境抽取真实查询补充训练数据

**模型层面：**
- 优先选择代码类基座模型（Qwen2.5-Coder、DeepSeek-Coder）
- LoRA rank=16~32, alpha=rank×2, lr=2e-4
- 目标模块选 Attention 全投影层（q_proj, k_proj, v_proj, o_proj）

**评估层面：**
- 三个指标都要关注：EX（结果正确性）、EM（集合匹配）、VES（值精度）
- 按查询难度分层评估，定位薄弱环节
- 建立回归测试集，防止新版本退化

**部署层面：**
- 使用 vLLM 提供生产级推理，结果缓存减少重复
- 只读沙箱执行 SQL，设超时和结果限制
- 监控 EX 指标漂移，触发自动告警

---

## 七、面试回答模板（Top 5）

### 模板1：介绍 NL2SQL 项目的技术实现

> "我主导的 NL2SQL 项目采用'基座模型 + LoRA 微调 + 领域适配'路线。首先基于企业数据库 Schema，利用 GPT-4 自动生成 5000+ 条 NL-SQL 训练对，并用 DuckDB 逐一验证 SQL 可执行性。然后使用 LLaMA Factory 对 Qwen2.5-Coder-7B 进行 LoRA 微调，训练 3 个 epoch，EX 达到 88%。部署层面封装为 FastAPI 服务，集成权限控制、结果脱敏和 vLLM 推理加速，对 Bad Case 持续回放迭代。关键收获：数据质量和 SQL 验证远比调参重要。"

### 模板2：通用大模型直接 Prompt 和微调的差异

> "通用大模型（GPT-4）直接做 NL2SQL 在简单查询上表现不错，但企业场景有三大不足：一是术语不匹配，业务字段如 `crt_dt` 对应'创建日期'，通用模型无法理解；二是 SQL 方言差异，企业特定函数不同；三是一致性差，同问题不同轮次可能生成不同 SQL。微调后模型在 EX 上通常提升 20~35%，输出也更稳定。但微调需要持续维护数据集和模型版本。"

### 模板3：如何处理用户表述模糊的问题？

> "我采用三层策略：第一层——Schema 映射，构建同义词表将口语表达映射到标准字段。第二层——追问消歧，当模型检测到模糊（如'最近'未指明天数）时主动向用户确认。第三层——默认假设，对常见模糊做合理默认（如'上个月'默认最近一个完整自然月）。三层结合，兼顾体验流畅性和准确性。"

### 模板4：NL2SQL 评估指标的选取和解读

> "核心看三个指标：EX 是业务最关心的——SQL 执行结果是否正确；EM 偏技术——SQL 语句本身是否精确；VES 介于两者之间。企业场景 EX 最重要，结果对就行。但监控要分层：简单查询 EX 目标 > 95%，中等 > 85%，复杂 > 75%。EX 达标但 EM 偏低时，说明模型有'正确但写法不同'的问题，可通过 SQL 规范化后处理改善。"

### 模板5：NL2SQL 未来发展方向

> "三个方向最值得关注：一是多模态 NL2SQL——支持图表、截图等交互方式。二是 Agent 化——不仅能查数据，还能根据结果自动做数据分析。三是动态 Schema 自适应——模型自动适应 Schema 变更，无需重新微调。此外，流式 NL2SQL（边输入边预测）也会是重要方向。"

---

## 八、快速查漏补缺Checklist

- [ ] 能用一句话说出 NL2SQL 的定义和价值
- [ ] 能说出 3 个以上企业应用场景
- [ ] 能区分 Prompt-based 和 Fine-tuning 的优劣
- [ ] 理解 Schema Linking 的概念和三个子任务
- [ ] 掌握 EX、EM、VES 三项评估指标及区别
- [ ] 能写出 LoRA 的数学公式并解释原理
- [ ] 知道 Qwen2.5-Coder、DeepSeek-Coder、SQLCoder 等基座模型
- [ ] 能说出模板生成、LLM 生成、人工标注的优劣
- [ ] 会设计 NL2SQL 数据集的 JSON 格式
- [ ] 能写出 LLaMA Factory 的 YAML 配置文件
- [ ] 会设置 LoRA 超参数（rank、alpha、lr、target_modules）
- [ ] 知道 QLoRA 和全参数微调的适用场景
- [ ] 能用 Python 实现 EX/EM 评估代码
- [ ] 会按难度分层评估模型
- [ ] 能进行 Bad Case 分析和分类
- [ ] 知道如何排查和定位效果瓶颈
- [ ] 能画出 NL2SQL 微调平台架构图
- [ ] 了解 vLLM 推理服务部署方案
- [ ] 知道多租户 NL2SQL 的设计思路
- [ ] 理解持续优化的反馈闭环设计
- [ ] 掌握 SQL 安全检查和防护策略
- [ ] 了解 DuckDB/SQLite 在 NL2SQL 中的作用
- [ ] 理解灾难性遗忘问题和缓解方法
- [ ] 了解领域自适应（Domain Adaptation）的核心方法
- [ ] 掌握 Few-shot Prompt 模板设计
- [ ] 了解 NL2SQL Agent 路线和 Fine-tuning 路线的差异
- [ ] 理解 Value Mismatch 问题及处理策略
- [ ] 了解 OOD 查询的处理方法
- [ ] 掌握 SQL 方言适配的常见策略

---

> 🎯 **最后建议：** NL2SQL 面试考察对整体技术栈的深度理解和解决实际问题的工程思维。一面侧重基础概念，二面侧重实践细节，终面侧重系统设计。建议对 Checklist 中每一项都能用自己的话展开讲 3~5 分钟，配合具体项目案例效果最佳。
