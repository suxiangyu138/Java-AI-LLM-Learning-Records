# PEFT 面试题

> 微调面试最高频区："LoRA 为什么有效、r/alpha 怎么配、QLoRA 原理、DoRA 区别、何时 PEFT 不够"——每题带 2026 研究实证，背熟即得分

## 1. 必问基础 8 题

### Q1：LoRA 为什么用很少的参数就能接近全量微调的效果？

**答案**（三层结构）：**经验层**——Baseten 2026 实测 LoRA 恢复全量微调收益的中位数 98%；**假说层**——intrinsic dimensionality（内在维度）：微调数十亿参数的模型并不需要有意义地更新所有权重，任务相关的权重更新聚集在 ~100-1000 维的低维子空间里（实证链：Aghajanyan 证明 ~200 维随机投影更新可在 GLUE 几乎无损微调）；**机制层**（ICLR 2026 深化）——LoRA 是隐式梯度压缩器，把全量梯度投影到低秩子空间；与全量的差距本质是"秩 vs 梯度内在维度（GID）"的错配，对齐后（RaLoRA 类）可逼近全量。

### Q2：LoRA 的 r 和 alpha 怎么配？r 越大越好吗？

**答案**：r 是**容量硬上限不是平滑质量旋钮**——任务的 intrinsic rank 是 k：r ≥ k 就够（超出是冗余容量，只增过拟合风险），r < k 欠拟合。配置：简单风格 4-8、常规 SFT 16、复杂任务 32-64（配 rsLoRA 稳定大秩）。alpha 与 r 的比值（α/r）决定有效缩放 = 该模块有效学习率——2026 保守共识 alpha==r（缩放 1.0）；rsLoRA 理论正确缩放是 α/√r（大秩不坍缩）；调 alpha 与调 lr 二选一别双调。

### Q3：QLoRA 的原理是什么？为什么 4bit 量化不损效果？

**答案**：QLoRA = 4bit 量化底座 + LoRA 适配器（bf16 全精度训练）。三个组件：NF4（NormalFloat4——针对正态分布权重设计的分位数量化，4bit 信息最优）、双量化（量化常数再量化，每参数再省 ~0.5bit，合计 ~4.5bit/参数）、Paged Optimizer（优化器状态分页管理防 OOM）。**效果不损的机制**：量化只作用于存储（4bit 存），计算时反量化回 bf16（高精度算）——"低精度存、高精度算"分离；适配器（真正要学的部分）全精度不受量化影响——量化只影响被冻结的底座。代价：训练慢 30-40%（反量化开销，速度换显存）。

### Q4：LoRA 和 QLoRA 怎么选？什么时候用哪个？

**答案**：显存账本——LoRA ≈ 1.1 倍权重（7B 单卡 24GB）、QLoRA ≈ 0.5 倍（7B ~6GB、13B ~18GB、65B 48GB 单卡）；速度——QLoRA 慢 30-40%（Unsloth 类内核可拉平）。**选型**：显存够用 → LoRA（bf16）更快更稳；显存紧张/模型更大 → QLoRA；单卡训 13B+ → QLoRA 几乎是唯一选择。2026 组合：QLoRA + DoRA（复杂任务）、QLoRA + FSDP（70B 多卡）。

### Q5：DoRA 和 LoRA 的区别？DoRA 一定更好吗？

**答案**：DoRA（Weight-Decomposed LoRA）把权重分解为**幅度（magnitude）与方向（direction）**——LoRA 只作用于方向分量，幅度单独训练——因为全量微调的更新里幅度与方向变化规律不同，单独建模幅度让表达空间更接近全量（低秩下稳定 1-4% 收益，显存基本持平）。**不一定更好**：2026 审计论文（Learning Rate Matters）发现 DoRA 在简单任务（如 GSM8K）上可能**退化**（幅度/方向分解在低复杂度场景引入优化不稳定）——复杂任务收益、简单任务谨慎——选变体前先调好 vanilla LoRA 对比。

### Q6：LoRA 训练完怎么部署？多 LoRA 服务的要点？

**答案**：两条路径——**合并**：float16 加载底座（4bit 合并损精度）→ merge_and_unload → 完整模型服务（推理零开销、不可逆、合并后必重验）；**多 LoRA 服务**：底座常驻 + 适配器热挂（开销 2-5%）——vLLM 硬约束：enable_lora=True + 适配器预注册 + **modules_to_save 必须为 null**（全秩张量不支持）。选型：单模型合并、多变体多 LoRA（白得秒级回滚：回滚 = 切适配器版本）。注意底座版本一致性（换底座 = 适配器需重训/重验证）。

### Q7：PEFT 和全量微调怎么选？什么时候 PEFT 不够？

**答案**：三问——**数据量**（1k-50k PEFT 默认；50k+ 全量可竞争；<1k Prompt 更优）；**任务复杂度**（格式/风格等低内在维度行为 PEFT 足够；复杂推理/大领域迁移全量或继续预训练）；**资源**（显存/时间/GPU——通常是第一决定因素）。**PEFT 不够的三个信号**：r=64 调好仍不达（容量约束）、知识类需求（PEFT 不更新知识截止——归 RAG）、领域整体迁移（继续预训练）。先诊断"方法问题还是需求问题"再换方案。

### Q8：还有其他 PEFT 方法吗？各自适合什么？

**答案**：五类——**Prompt Tuning**（输入层软提示，<0.01% 参数，NLU 分类 + 极端省参；生成任务与小模型效果差）；**Prefix Tuning**（每层 K/V 前缀，生成任务层级引导；占上下文窗口）；**Adapter**（子层后瓶颈 MLP，1-5% 参数，模块化但 +10-20% 延迟不可合并——模块化优势被 LoRA 覆盖）；**IA3**（激活缩放向量，~0.01%，极低资源）；**BitFit**（bias-only，小数据场景反超全量）。FLAN-T5-XL 基准结论：**无普适最优方法**，优势随任务/数据规模变化——低资源时 LoRA/BitFit 反超全量——选型靠基准不靠信仰。

## 2. 原理与选型场景题

**场景一**："LoRA 微调后任务指标不达预期，r 从 16 升到 64 也没用，怎么排查？"——先分层诊断（`../微调常见问题/07-效果不达预期问题.md`）：确认"该不该微调"（知识缺口归 RAG）→ 数据量是否支撑（小数据 + 大 r 更快过拟合——升 r 前先确认数据）→ 超参是否调好（lr 是最影响超参，`../训练关键超参 & 显存优化技术/02-学习率与调度.md`）→ 容量是否真不够（r=64 调好仍不达 = 容量信号，考虑全量或重新审视任务定义）——**"升 r"是最容易被滥用的调节，先查前两层再谈容量**。

**场景二**："一张 24GB 卡要微调 13B 模型做客服格式任务，方案怎么给？"——QLoRA NF4（13B ≈ 18GB）+ 梯度检查点 + 8bit 优化器 + batch 1-2 + 累积 4 + packing（客服短样本）；LoRA 配置 r=16、alpha==r、q/k/v/o；lr 2e-4、epoch 2-3 + 早停；数据 1k-5k 条（行为校准足够）；部署先合并（单模型）；客服的"知识"类问题归 RAG（PEFT 管格式、RAG 管事实）。

**场景三**："团队有 20 个业务线都要微调底座，怎么规划？"——**多 LoRA 架构**：一个底座 + 20 个适配器（每个 15MB 级）——比 20 份全量模型（140GB+）省三个数量级；适配器生命周期管理（staging→production→archived + 注册表：底座版本/数据版本/指标/灰度结果）；路由按业务线分发（热切换 + 秒级回滚）；**注意**：20 个适配器的显存裕量（10-15%）与 vLLM 多 LoRA 配置（modules_to_save null）；适配器训练流水线统一（20 个业务共享数据/训练/评估基础设施）。

## 3. 答题范式总结

PEFT 题标准答题结构（四步）：**讲原理**（低秩分解 + intrinsic dimensionality——先给数学再给假说）→ **给数字**（r=16、alpha==r、98%、4.5bit、0.1-1%）→ **做对比**（LoRA vs QLoRA vs DoRA vs 全量——各解决什么）→ **划边界**（何时 PEFT 不够 + 与 RAG 组合）。

**"原理 → 数字 → 对比 → 边界"四件套**是 PEFT 答案的满分结构；进阶追问自然导向边界判断（何时微调）与超参体系（lr 怎么调）——PEFT 答得稳，微调面试就稳了。

> 🎯 **核心要点**：PEFT 面试 = 8 题全覆盖（原理三层/配置/QLoRA 机制/选型/DoRA 谨慎/部署/边界/其他方法）；答题四步 = 原理→数字→对比→边界；2026 数字弹药——98%（Baseten）、intrinsic dim ~100-1000 维、α/√r（rsLoRA）、4.5bit/参数、慢 30-40%、↓89% 通信（混合分片）——PEFT 是面试与工程的交汇点：原理即配置、配置即原理。

---

**参考来源**：

- [Why low-rank works: intrinsic dimensionality hypothesis（The Neural Base）](https://theneuralbase.com/lora-qlora/learn/intermediate/why-low-rank-works-intrinsic-dimensionality-hypothesis/)
- [Gradient Intrinsic Dimensionality Alignment（ICLR 2026）](https://en.papernotes.org/ICLR2026/model_compression/gradient_intrinsic_dimensionalityalignmentnarrowing_the_gap_between_low-rank_ad/)
- [Learning Rate Matters: Vanilla LoRA May Suffice for LLM Fine-tuning（2026）](https://scite.ai/reports/learning-rate-matters-vanilla-lora-G3WVvPNx)
- [LoRA Fine-tuning Hyperparameters Guide（Unsloth）](https://unsloth.ai/docs/get-started/fine-tuning-llms-guide/lora-hyperparameters-guide)
- [Post-Training Science for Supervised Fine-Tuning（Baseten, 2026-06）](https://www.baseten.co/research/post-training-science-for-supervised-fine-tuning/)

---

**返回总览**：[00-PEFT参数高效微调总览](./00-PEFT参数高效微调总览.md)
