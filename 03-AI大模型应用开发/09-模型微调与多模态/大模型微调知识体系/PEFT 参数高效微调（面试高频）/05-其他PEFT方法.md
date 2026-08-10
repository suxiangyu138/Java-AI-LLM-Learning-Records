# 其他 PEFT 方法

> LoRA 之外的家族成员：Prompt/Prefix Tuning、Adapter、IA3、BitFit——各自机制、基准实测（FLAN-T5-XL）、选型判据——"LoRA 之外"不再是冷知识

## 1. 方法速览

| 方法 | 改什么 | 可训练参数 | 推理开销 |
|------|--------|:---:|:---:|
| Prompt Tuning | 输入层软提示向量 | <0.01%（千级） | 无 |
| Prefix Tuning | 每层 K/V 前软前缀 | <0.1% | 微增（占上下文） |
| Adapter | 注意力/FFN 后瓶颈 MLP | 1-5% | +10-20% 延迟（不可合并） |
| IA3 | 激活逐元素缩放向量 | ~0.01% | 极小 |
| BitFit | 仅 bias 项 | 极小 | 无 |

**共同思想**：冻结底座、只训"附件"——差异在"附件"的形态与位置（输入层/每层/子层后/激活上/参数子集），容量与表达能力的取舍不同。

## 2. Prompt / Prefix Tuning：软提示

**Prompt Tuning**：只在输入层训练一组连续向量（soft prompt）拼在真实 prompt 前——参数最少（千级）、单模型多任务切换最便宜（每任务一组向量）；局限：**生成任务与小模型上效果差**（文本生成需要更深层的引导），100B+ 规模才接近全量——适合分类/情感类 NLU 任务的极端省参场景。

**Prefix Tuning**：在**每一层**的 K/V 前插入可训练前缀（不是只在输入层）——层级的引导让生成任务（翻译/摘要）表现更好；代价：**前缀长度消耗上下文窗口**（4096 窗口塞 100 token 前缀 = 浪费）、优化困难（常需 MLP 重参数化）、复杂推理任务效果差。

**软提示的通用局限**：初始化敏感（不同初始化效果差异大）；与 LoRA 相比容量更受限（`../训练关键超参 & 显存优化技术/05-正则化与防过拟合.md` 的容量论述类比）——**"软提示"是 PEFT 里最"提示词"的方法**（介于 Prompt 工程与微调之间，`../边界判断（非常关键，面试必问）/02-微调vsPrompt工程.md` 的边界在此有交集）。

**软提示与硬提示的连续谱**：Prompt 工程（硬文本）→ Prompt Tuning（输入层软向量）→ Prefix Tuning（每层软向量）→ LoRA（权重级）——**沿着这个谱，干预深度递增、可训练参数递增、效果上限递增**——选型时先定位"需要多深的干预"：只差提示格式用硬 Prompt、差一点泛化用软提示、真正改行为才上 LoRA——**"软提示"卡在中间**：它的定位是"比提示词强、比权重微调轻"。

## 3. Adapter：模块化的代价

**Adapter**：在注意力/FFN 子层后插入瓶颈 MLP（降维-升维）——参数 1-5%、模块化（每任务一组适配器可热切换）、GLUE 上接近全量。

**两个代价**：**推理延迟 +10-20%**（串行结构无法合并进底座——与 LoRA 的关键区别：LoRA 可合并、Adapter 不能）；参数占比高（多任务多适配器时存储开销累积）。

**适用**：多任务频繁切换且能接受延迟的场景（模块隔离好、互不干扰）；**LoRA 可合并 + 多变体共存之后，Adapter 的"模块化"优势被 LoRA 覆盖**——2026 年 Adapter 的使用率显著低于 LoRA（面试答"Adapter vs LoRA"时点出这一点是加分项）。

**Adapter vs LoRA 的完整对比**（面试高频）：参数（1-5% vs 0.1-1%）；可合并（否 vs 是——Adapter 串行结构无法写回底座）；延迟（+10-20% vs 合并后为零）；多任务（都支持模块隔离）；效果（GLUE 接近、复杂任务 LoRA 占优）——**结论：Adapter 的三个核心卖点（模块化/多任务/近全量效果）LoRA 全部覆盖且成本更低**——Adapter 的存量价值在于"历史项目与特定架构"（某些非 Transformer 架构没有低秩分解的天然位置）。

**IA3 与 BitFit 的"零结构"定位**：两者都不改网络结构（只缩放激活/只训 bias）——**与 LoRA（插入低秩结构）的本质区别是"不动结构"**——优势是推理零改动（无新增计算路径）、劣势是容量最低——**"结构干预深度"是 PEFT 方法的连续谱**：bias（BitFit）→ 激活缩放（IA3）→ 软提示（Prompt/Prefix）→ 低秩补丁（LoRA）→ 新模块（Adapter）——干预越深容量越大、成本越高，选型先定位"需要多深"。

## 4. IA3：极端的参数效率

**IA3（Infused Adapter by Inhibiting and Amplifying Inner Activations）**：不插入新模块，只训练**激活值的逐元素缩放向量**（keys、values、FFN 激活各一组）——参数 ~0.01%、推理开销极小、低资源（few-shot）场景表现好。

**局限**：容量低——不适合需要高表达能力的任务（复杂格式/长输出）；**定位**：极端省参场景（参数预算极紧、推理开销敏感）的选择——与 LoRA 的取舍是"参数 vs 容量"。

## 5. BitFit：bias-only 的惊喜

**BitFit（Bias-Term Fine-Tuning）**：只训练所有 bias 项（其余冻结）——参数极小、过拟合风险最低、推理零开销——**低资源/小数据场景的惊喜选手**（FLAN-T5-XL 基准在 AG News/E2E/SAMSum 上甚至领先全量）。

**局限**：容量天花板最低——只在任务"简单到 bias 校准就够"时有效；**定位**：小数据 + 简单任务的快速实验选项（比 Prompt 工程更进一步、比 LoRA 更省）。

## 6. 基准实测：FLAN-T5-XL 研究

受控基准（FLAN-T5-XL，分类 AG News/CoLA + 生成 E2E/SAMSum，100/1K/10K 样本三档）的核心发现：

**低资源（100-1K 样本）**：LoRA 与 BitFit 经常**反超全量微调**（LoRA 在 CoLA 显著强、BitFit 在 AG News/E2E/SAMSum 领先）——全量在小数据上过拟合；Prompt Tuning 在生成任务上表现**明显差**。

**中资源（1K-10K）**：IA3/LoRA/BitFit 保持竞争力；**高数据量**：全量在一些任务上收复失地（LoRA/IA3 仍能赢特定任务）。

**核心结论**：**没有普适最优的 PEFT 方法**——优势随任务类型、数据规模、配方变化——**选型靠基准而非信仰**（`../训练关键超参 & 显存优化技术/01-超参全景与调参方法论.md` 的"经验问题"纪律在此同样适用）。

**基准的方法论提醒**：FLAN-T5-XL 是 3B 级模型——**结论向 7B+ 模型迁移要谨慎**（小模型上 Prompt Tuning 差、BitFit 好的规律在大模型上可能反转——大模型对软提示更敏感）；**"在你的模型上跑你自己的小基准"**（20-50 条样本对比候选方法）比引用任何公开基准都可靠——选型成本极低、结论直接可用（`../完整微调工程流程/03-评估先行基线建立.md` 的基线方法在此复用）。

## 7. 选型判据汇总

**默认**：LoRA（绝大多数场景，效果/成本/生产友好度综合最优）；**单卡大模型**：QLoRA（06 篇）；**极端省参 + NLU 分类**：Prompt Tuning；**生成任务 + 层级引导**：Prefix Tuning；**模块隔离优先且接受延迟**：Adapter；**极低资源 + 简单任务**：BitFit/IA3；**小数据快速实验**：BitFit——**"其他方法"的定位是 LoRA 不合适的特定场景**，不是与 LoRA 竞争通用地位。

> 🎯 **核心要点**：五方法各解决一个场景——Prompt Tuning（输入层软提示，NLU 分类 + 极端省参）、Prefix Tuning（每层前缀，生成任务但占上下文）、Adapter（瓶颈 MLP，模块化但 +10-20% 延迟不可合并——模块化优势被 LoRA 覆盖）、IA3（激活缩放，极低资源）、BitFit（bias-only，小数据惊喜）；FLAN-T5-XL 基准——低资源时 LoRA/BitFit 反超全量、无普适最优；选型 = LoRA 默认 + 特定场景换方法——"其他方法"不是 LoRA 的竞争者，是补充者。

---

**参考来源**：

- [Benchmarking PEFT Techniques for Large Language Models（播客）](https://podcast.do-not-panic.com/episodes/benchmarking-peft-techniques-for-large-language-models/)
- [PEFT Explained: LoRA, QLoRA, Prefix Tuning, Adapters（LumiChats）](https://lumichats.com/glossary/peft)
- [What Is PEFT? A Guide to Parameter-Efficient Fine-Tuning（dev.to）](https://dev.to/bahadir_kusat_7df590dc9cd/what-is-peft-a-guide-to-parameter-efficient-fine-tuning-273b)
- [PEFT 全家桶：从 LoRA 到 IA³（CSDN）](https://blog.csdn.net/2402_84764726/article/details/158178737)

---

**下一模块**：[06-QLoRA深潜.md](./06-QLoRA深潜.md) / **返回总览**：[00-PEFT参数高效微调总览](./00-PEFT参数高效微调总览.md)
