# 量化为什么能提速：带宽约束与 Roofline 模型
> 先建立最核心的认知：大模型推理快不快，主要取决于"从显存搬多少字节"，而不是"算多少次乘法"。

## 1. Decode 阶段是内存带宽受限，不是计算受限

### 1.1 一次 token 生成的数学账

自回归解码（decode）每一步只生成一个 token，但必须把**全部权重从 HBM 搬到计算单元**。以 7B 参数 FP16 模型为例：

- 权重体积：7B × 2 字节 ≈ 14 GB
- 生成 1 个 token 的数学量：约 14 GFLOP（每个参数做 1 次乘加）
- 算术强度（Arithmetic Intensity）= 14 GFLOP ÷ 14 GB ≈ **1 FLOP/byte**

而现代 Tensor Core 需要约 200 FLOP/byte 才能饱和。1 比 200，意味着计算单元 99% 的时间在等数据——**HBM 总线是唯一瓶颈**。

### 1.2 用单层验证直觉

一个 8192×8192 的线性层，前向计算约 1.1 万亿次 FLOP，但 BF16 权重只有约 134 MB。在 TPU v5e（197 TFLOPS 算力、819 GB/s 带宽）上，矩阵乘单元会因等权重搬运而长期空转。**权重体积小到读一次只要 0.16ms，计算却要 5.6ms**——时间全耗在等数据。

> 💡 判断一段负载是否内存受限，看算术强度与硬件 Ridge Point 的关系：低于 Ridge Point（算力÷带宽）就是内存受限。LLM decode 的 ~1 FLOP/byte 远低于任何加速卡的 Ridge Point，属于教科书级的带宽受限负载。

### 1.3 Prefill 与 Decode：两种负载，两种瓶颈

推理的两个阶段负载性质完全不同，这是"量化收益到底有多大"的第一个分水岭：

- **Prefill（预填充）**：一次性消化整个输入提示词，数学量 ≈ 序列长度 × 权重体积的矩阵乘，**批量大、计算密集**，靠近计算受限区，量化收益主要来自矩阵乘效率而非带宽
- **Decode（解码）**：逐 token 生成，每一步只做"1 个向量 × 全部权重"的矩阵乘，**批量 1、极度带宽受限**，量化收益接近线性

因此同一个量化模型，短提示长生成的聊天场景（decode 主导）延迟收益最明显；长提示短生成的文档分析场景（prefill 占比高）收益被稀释。**评估量化收益前，先算清你的流量里 prefill 与 decode 的占比**。

## 2. 差距在逐年拉大：算力 4×，带宽 2×

硬件演进有一个被低估的事实：**每代 GPU 算力约翻 4 倍，而 HBM 带宽只翻约 2 倍**。一次 DRAM 访问的机会成本约等于 500 次乘法运算。计算/带宽比值逐代升高，意味着：

- 相同的模型，新一代硬件上"内存受限"的属性更强
- 单纯堆算力对 decode 延迟几乎无效，省字节才是正路
- 量化、KV Cache 优化、推理引擎的一切带宽优化，重要性只会越来越高

这也解释了 2026 年行业为何集中攻坚 FP4：精度位宽每降一半，带宽压力就降一半，而无需等硬件换代。

### 2.1 用 H100 算一笔 Ridge Point 的账

以 H100 SXM（约 989 TFLOPS FP16 稠密、3.35 TB/s HBM3）为例，Ridge Point = 989 TFLOPS ÷ 3.35 TB/s ≈ 295 FLOP/byte——即算术强度超过 295 的负载才能喂饱 Tensor Core。对比 LLM decode 的 ~1 FLOP/byte，差距近 300 倍。这意味着即使把 Tensor Core 利用率做到 100%，decode 的理论峰值也只有带宽能支撑的 1/295——**纯算力对 decode 几乎无意义**，这就是"带宽即性能"的硬依据。

### 2.2 批量的隐藏作用：为什么批量可以掩盖带宽墙

把 batch 从 1 升到 N，每个 token 可以共享权重读取：权重只读一次、喂给 N 个不同激活向量。算术强度近似提升 N 倍——batch 64 时 decode 的算术强度从 ~1 升到 ~64，开始进入吞吐优化区。这解释了推理系统的两个核心事实：

- **为什么推理引擎都在卷 continuous batching**：批量大 = 算术强度高 = 带宽压力摊薄
- **为什么量化与批量的收益可叠加**：量化把单 token 的字节成本降 4 倍，批量把读取次数摊薄 N 倍，两者独立乘算

交互式场景（batch 接近 1）吃不到批量红利，量化几乎是唯一的延迟杠杆——这也解释了为什么聊天 API 的成本结构对量化位宽如此敏感。

## 3. 量化如何直接命中瓶颈

量化把每个权重占用的字节数降低，带宽流量线性下降：

- BF16/FP16（2 字节/数）：基线
- INT8（1 字节）：带宽流量减半
- INT4（0.5 字节）：带宽效率翻 4 倍

由于 decode 时间由权重搬运主导，搬运量降 4 倍直接转化为延迟改善：INT4 在实际部署中普遍带来 **2-3 倍延迟下降**，而 INT8 通常只有 <0.1% 的困惑度劣化——性价比极高。

注意量化带来的收益全部来自"少搬数据"，不是"少算数"。数学量没变（反量化后仍按原精度算），变的是数据搬运成本。因此**凡是 HBM 流量主导的阶段（decode、长上下文 attention），量化收益最大；计算密集的 prefill 阶段收益相对小**。

### 3.1 延迟下限公式：量化收益可以提前算出来

单 token 延迟的下限可粗算为：

```text
decode 延迟下限 ≈ 权重体积 ÷ 可用带宽
```

70B 模型在 H100（4.8 TB/s）上：FP16 约 140 GB ÷ 4.8 TB/s ≈ 29 ms/token；INT4 约 35 GB ≈ 7 ms/token——**公式给出的是理论下限**，实际延迟还要加反量化、kernel 调度、attention 等开销，通常达到理论值的 1.5-2 倍。这个公式在生产中非常有用：上线前先算"位宽从 2 字节降到 0.5 字节，延迟能省多少"，再决定是否值得付出精度与工程成本。

## 4. Roofline 视角：量化把负载推向计算受限区

Roofline 模型把性能画成两段折线：低算术强度段受带宽限制，高算术强度段受算力限制，转折点即 Ridge Point。量化做的事是把分子（数学量）不变、分母（搬运字节）变小，即**把负载的算术强度整体右移**，从带宽受限段推向 Ridge Point 方向。当权重从 2 字节降到 0.5 字节，算术强度从 ~1 升到 ~4 FLOP/byte，仍低于 Ridge Point——所以量化不是把 decode 变成计算受限，而是"在带宽受限区内加速"。

值得用 Roofline 视角重新审视前文两个结论：

- **为什么量化不追求算力**：decode 的算术强度即使在 INT4 下也远低于 Ridge Point，算力提升对带宽受限段毫无作用——量化省字节的收益是"实打实"的，而堆算力的收益是"纸面"的
- **为什么批量与量化叠加收益更高**：批量把算术强度按 N 倍提升，从 ~4 推到 64+，开始触及吞吐区；此时算力（而非带宽）才成为下一瓶颈——所以推理引擎先卷 continuous batching（把算术强度抬起来），再卷量化（把每次搬运的字节压下去），两者顺序有因果

一句话总结：**量化在 Roofline 图上做的事，是把"同一个带宽预算里能跑多少 token"变多，而不是把"一个 token 算多快"变快**——前者正是部署成本的全部。

> 🎯 **核心要点**：decode 是"搬权重"的负载，算术强度仅 ~1 FLOP/byte；算力每代 4× 而带宽仅 2×，瓶颈越来越硬。量化通过降低每权重字节数（2→1→0.5）线性削减 HBM 流量，换来 2-3 倍延迟收益——收益的本质是"少搬数据"，前提是反量化在片上完成而非 HBM 往返（详见 07 篇）。

---

**下一模块**：[02-数值格式全景](02-数值格式全景.md)　**返回总览**：[00-量化背后硬件约束知识体系总览](00-量化背后硬件约束知识体系总览.md)

## 参考来源

- [3-Part Series: LLM Latency in Production (Part 1) — Towards AI](https://pub.towardsai.net/3-part-series-llm-latency-in-production-part-1-babc85a2128a)
- [Accelerating LLM Inference: Fused INT8 Weight-Only Quantization in Pallas — Hugging Face](https://huggingface.co/blog/rishiraj/fused-int8-weight-only-quantization-in-pallas)
- [Efficiency in LLMs — Alex Smola, MLSS 2026](http://alex.smola.org/posts/45-mlss-efficiency/index.html)
- [The Memory Wall and Roofline Diagnosis — MLSys Book](https://mlsysbook.ai/vol2/performance_engineering/performance_engineering.html)
