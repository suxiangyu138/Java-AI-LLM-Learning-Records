# AMD / Intel / 国产加速卡的量化支持
> NVIDIA 之外的世界：AMD MI355X 原生 MXFP4、Intel Gaudi 3 走 FP8、Xeon CPU 靠 AMX、Ascend 支持 MX 格式——各自量化链路与成熟度差异巨大。

## 1. AMD Instinct MI355X：FP4 时代唯一的正面竞争

MI355X（CDNA 4 / gfx950）是 2026 年唯一与 Blackwell 正面竞争的 FP4 平台：单卡 288 GB HBM3E，1T 参数 MoE 模型 MXFP4 后 4 卡（TP=4）即可单节点装下。硬件核心是 **CDNA 4 原生微缩放支持**——FP4 GEMM 执行块缩放的 MFMA 指令 `v_mfma_scale_f32_16x16x128_f8f6f4`，4 位载荷连同每 32 元素一个的 E8M0 缩放直接进矩阵核，不经 BF16 扩展。

实测数据（2026 上半年）：

- Kimi K2.5/K2.6/K2.7-Code（1T MoE，MXFP4，TP=4）：峰值 5369.6 tok/s/GPU，Pareto 前沿在多个并发档位压过单节点 B200（NVFP4）
- GLM-5.2（MXFP4）：节点级 2626 tok/s，单流 213 tok/s，每百万 token 成本约 $0.22，不到 B200 的 3/4

软件栈依赖 ROCm 7.x + AMD-Quark v0.11（量化器）+ ATOM/AITER（推理内核），vLLM 需 `VLLM_ROCM_USE_AITER=1` 开关。**工程成熟度仍有短板**：MLA 稀疏解码在 TP=8 会触发内核崩溃（num_heads<16 路径），跨节点专家并行尚未规模化验证，且引擎差异大——同一 GLM-5.2 模型，vLLM 会丢掉 MXFP4 收益，sglang 却完整保留。

### 1.1 AMD 的差异化武器：FP4 集合通信

AMD 在 FP4 生态有一个 NVIDIA 没有的独门：**量化数据直接做集合通信**。QuickReduce 方案在 1 GB 消息量级下，FP4 all-reduce 较 RCCL 基线快 4.14×（TP=2）、3.43×（TP=4）、1.52×（TP=8）；端到端 Qwen3-30B-A3B + vLLM 实测，FP4 让 TTFT 降约 1.36×、TPOT 降约 1.26×，同时 GSM8K 精度恢复 99.38%。

思路的深层含义：**4-bit 化的不只是存储与计算，还包括跨卡通信**。MoE 模型的 all-to-all 专家分发、张量并行的权重规约，在传统栈里都要先把数据恢复成 BF16 再传——AMD 让 FP4 载荷在互联上直接走，把量化的收益延伸到了带宽瓶颈的第二战场（NVLink/Infinity Fabric）。NVIDIA 生态尚未公开同等的 FP4 集合通信优化，这是 AMD 在 4-bit 时代差异化竞争的代表性落点。

## 2. Intel Gaudi 3：FP8 路线，无 FP4

Gaudi 3 的量化能力集中在 **FP8**（Linear/KVCache/Matmul/Softmax 均可量化），走 vLLM + Intel Neural Compressor（INC）集成路径：先加载权重到 CPU 量化、再传 HPU，规避 OOM。实测（软件 1.24，8 卡 HLS-Gaudi3）：LLaMA 3.1 70B FP8 约 21k tok/s。**局限明显**：验证范围仅限 Llama 系模型；校准数据设备相关（Gaudi 2 与 Gaudi 3 的 scale 不能互用）；无原生 4-bit 支持，INT4 只能软件路径。

### 2.1 Gaudi 2 vs Gaudi 3：代际差距与量化相关

- Gaudi 2（2022）：FP8 原生支持有限，主要走 BF16/INT8 路径，vLLM-gaudi 的早期版本以 INT8 为主
- Gaudi 3（2024-2026 主力）：FP8 全面原生化（Linear/KVCache/Matmul/Softmax），软件 1.24 起 Llama 3.1 70B FP8 约 21k tok/s（8 卡）；2026 年陆续新增 ModelOpt FP8、动态量化（MatMul/KV-cache）等实验能力

代际教训：**Gaudi 的 FP8 能力绑定软件栈版本**——同一块卡，vllm-gaudi 0.13（2026-01）与 0.16（2026-03）的量化能力差异显著。买卡时要把软件路线图（而非纸面规格）纳入选型维度。

### 2.2 Gaudi 的 FP8 接入流程：INC 路径要点

vLLM-gaudi 的 FP8 量化走 Intel Neural Compressor（INC），流程与 CUDA 生态明显不同：

1. 校准模型生成量化配置 JSON（INC 测量）
2. 设置 `QUANT_CONFIG` 环境变量指向配置
3. 推理参数启用 `--quantization inc` 与 `--kv-cache-dtype fp8_inc`
4. 权重先在 CPU 上量化再传 HPU（vllm-gaudi 0.16 起支持强制 CPU 加载防 OOM）

三个易错点：**校准测量设备相关**（Gaudi 2 与 Gaudi 3 的 scale 不能互用）；**验证范围仅 Llama 系**（INC 官方验证列表）；**软件栈版本敏感**（0.13 动态量化是实验特性，0.15 才支持 ModelOpt FP8）——Gaudi 上线的量化能力本质上跟随 vllm-gaudi 发布节奏，需对照版本矩阵。

## 3. Intel Xeon CPU：AMX 与 INT8

CPU 侧的核心指令是 **AMX（Advanced Matrix Extensions）**——x86 的矩阵乘扩展，原生支持 INT8/BF16 矩阵运算。vLLM 兼容矩阵显示：**INT8（W8A8）在 x86 CPU 上支持**（Turing 之后的 GPU 同样支持），但 FP8 在 x86 CPU 上不支持——CPU 的量化甜点是 W8A8-INT8，不是 FP8。Intel Neural Compressor 覆盖 Xeon Scalable、Xeon CPU Max、Core Ultra，提供 INT8/FP8 静态/动态量化与 SmoothQuant。CPU 推理的量化收益同样来自带宽：DDR 带宽远低于 HBM，INT8 减半搬运对 CPU 场景是刚需。

### 3.1 CPU 量化部署的两条现实路径

- **vLLM CPU 后端**：W8A8 INT8 走 oneDNN/AMX，适合小模型（≤8B）的 CPU 生产部署；INT4 无 AMX 指令，走 avx512 软件解包，吞吐有限——CPU 上的 4-bit 只有内存收益
- **llama.cpp**：GGUF Q4_K_M 在 Xeon 上靠 AVX-512 + 部分 AMX 支持跑出可用吞吐，是 CPU 侧最成熟的 4-bit 路径

CPU 场景的量化和 GPU 有一个本质差异：**CPU 没有原生 4-bit 矩阵指令，所以 4-bit 的唯一收益是内存**——当模型从"装不进内存"变成"装得下"（如 32GB 内存的服务器跑 70B INT4），这一步的收益是"能不能跑"级别的，与吞吐无关。

### 3.2 国产平台的现实评估：Ascend 的 MX 生态站位

昇腾 NPU 对 MXFP8/MXFP4 的支持使其在格式生态上与全球主流对齐（这是好事），但生产视角有三个现实差距：**软件栈独立**（MindIE 生态与 vLLM/SGLang 的主线发展不完全同步，新格式支持存在版本滞后）；**内核工程密度**（FP4 的打包/解包内核深度依赖社区打磨，而社区投入集中在 CUDA/ROCm）；**生态文档**（跨平台迁移的坑位知识积累不足）。

务实建议：**在国产平台上先跑 FP8**（格式公共子集、引擎支持最稳），等业务验证通过再评估 MXFP4 的增量收益——把"格式对齐"与"工程成熟度"分开决策。

## 4. 国产与新兴平台：Ascend 与软件派

- **Ascend NPU**：支持 MXFP8 与 MXFP4 格式，与 MX 生态对齐，但成熟度与生态文档远不如 CUDA/ROCm
- **LMDeploy TurboMind**：把 MXFP4 的**软件**支持扩展到 H100 甚至 V100——没有 FP4 Tensor Core 也能跑 4-bit 检查点，定位与 NVIDIA 老卡一致：省内存不加速
- **Intel Neural Compressor**：MXFP8/MXFP4 实验支持（2025-10），NVFP4 实验支持（2025-12），追平格式生态的速度很快

## 5. 跨平台选型要点

- **格式生态分层**：FP8 是 2026 年跨平台最安全的公共子集（Hopper/Blackwell/Gaudi/部分 NPU 都原生），MXFP4/NVFP4 只有 Blackwell 与 MI355X 原生
- **软件栈决定成败**：NVIDIA 的成熟度（TensorRT-LLM/vLLM/SGLang/FlashInfer）与 AMD 的差异化（ATOM 的 MFMA 内核、QuickReduce FP4 集合通信 4.14× 加速）形成两种哲学：前者生态广度，后者单点深度
- **验证口径**：宣传数字多为特定模型 + 特定引擎 + 特定并发的峰值，选型前必须用目标模型实测，警惕"框架丢弃量化收益"的隐性陷阱

### 5.1 平台能力速查总表

| 平台 | 原生量化精度 | 4-bit 加速 | 推理引擎 | 成熟度 |
|------|------------|:---:|---------|-------|
| NVIDIA Blackwell | FP8/NVFP4/MXFP4 | ✅ 原生 | TRT-LLM/vLLM/SGLang | 生产级 |
| AMD MI355X | FP8/MXFP4/MXFP6 | ✅ 原生 | ATOM/vLLM(AITER)/sglang | 生产级（有坑） |
| NVIDIA Hopper/Ampere | FP8/INT8 | ❌ 软件回退 | vLLM 等 | 生产级 |
| Intel Gaudi 3 | FP8（INC） | ❌ | vLLM-gaudi | 生产级（Llama 系限定） |
| Xeon CPU | INT8（AMX） | ❌ | vLLM CPU/INC | 生产级 |
| Ascend NPU | MXFP8/MXFP4 | 部分 | MindIE | 快速演进 |
| 手机 NPU | INT8/INT16 | ❌ | ONNX Runtime/QNN/CoreML | 生产级（格式受限） |

选型的核心动作是：**找到你的目标模型在目标平台上"原生精度上限"在哪一行**——上限以下随便选，上限以上的格式全部是"省内存不加速"。

> 🎯 **核心要点**：AMD MI355X 是唯一原生 FP4 竞品（MFMA 块缩放指令直吃 MXFP4，1T MoE 单节点可部署，成本约为 B200 的 3/4 但软件成熟度有短板）；Intel Gaudi 3 走 FP8 无 FP4；Xeon CPU 的量化甜点是 AMX INT8；Ascend 对齐 MX 生态。FP8 是 2026 年跨平台公共子集，FP4 是两强（Blackwell/MI355X）之争。

---

**下一模块**：[06-内存系统约束：HBM带宽容量与KV Cache量化](06-内存系统约束：HBM带宽容量与KV Cache量化.md)　**返回总览**：[00-量化背后硬件约束知识体系总览](00-量化背后硬件约束知识体系总览.md)

## 参考来源

- [Kimi K2.5/K2.6/K2.7-Code in MXFP4 on AMD Instinct MI355X — AMD](https://www.amd.com/en/developer/resources/technical-articles/2026/kimi-code-in-mxfp4-on-amd-gpus.html)
- [MXFP6/MXFP4 Mixed Precision on MI355X — AMD ROCm Blog](https://rocm.blogs.amd.com/artificial-intelligence/w4a6-quant-mm/README.html)
- [Serve Kimi-K2.5-MXFP4 on MI355X with ATOM — AMD ROCm Blog](https://rocm.blogs.amd.com/software-tools-optimization/kimi-k25-mxfp4-atom/README.html)
- [Running GLM-5.2 on AMD: 2,626 Tokens/s at Half NVIDIA's Cost](https://www.besthub.dev/articles/running-glm-5-2-on-amd-2-626-tokens-s-at-half-nvidia-s-cost-a4c2ee0fd18c)
- [vLLM Quantization Supported Hardware](https://docs.vllm.ai/en/v0.10.0/features/quantization/supported_hardware.html)
- [Intel Neural Compressor — vLLM Gaudi docs](https://docs.vllm.ai/projects/gaudi/en/latest/configuration/quantization/inc.html)
- [Intel Gaudi 3 Model Performance](https://www.intel.com/content/www/us/en/developer/platform/gaudi/model-performance.html)
