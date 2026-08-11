# 07-合并与多 LoRA 推理服务

> 定位：LoRA 产物的两种部署形态——静态合并（单任务、零开销）与动态多适配器（多任务、共享底座）。vLLM 的 Punica 内核让"一个底座挂千级适配器"成为 2026 年的标准架构。

## 形态一：静态合并（merge）

数学上 LoRA 的推理等价于把增量加回权重：W = W₀ + (α/r)·BA，合并后模型与"带 LoRA 前向"在数学上完全一致（差异仅来自计算精度）。PEFT 提供一条命令：

```python
model = PeftModel.from_pretrained(base, "adapter-dir")
merged = model.merge_and_unload()   # ΔW 写回 W₀，移除 LoRA 结构
merged.save_pretrained("merged-model")  # 存成普通 HF 模型
```

合并的坑集中在三处：**精度**——在 fp16/bf16 下执行合并，累积舍入误差虽小但长期存在，追求极致一致性时用 fp32 临时精度合并；**DoRA 类变体**——幅度/方向解耦需要 PEFT ≥ 0.10 才能正确合并，旧版合并结果是错的；**量化底座**——4-bit 底座合并需先反量化（内存瞬间膨胀 4 倍），量力而行。

**何时必须合并**：推理框架不支持 LoRA（部分嵌入式/TensorRT 管线）；模型要分发（合并产物是普通权重，任何框架可加载）；量化后微调的最终交付（合并再重新量化，避免部署端依赖 bnb）。

## 合并的工程细节

merge 前必查三件事：**版本**——变体（DoRA 等）合并依赖 PEFT ≥ 0.10，旧版本出的合并权重是错的，别省这一步；**精度**——默认在模型当前 dtype 下合并，若有后续量化/压缩计划，先升 fp32 合并再降回，避免舍入误差被二次量化放大；**目标目录**——merge_and_unload 后 save_pretrained 产物是普通 HF 模型（无 adapter 目录结构），可以再被任何框架加载。还有一个隐藏选项：**merge 而不 unload**（merge_and_unload(progressbar=True) 之外，PEFT 0.19 支持合并后保留 LoRA 结构，用于"想对比合并前后差异"的调试场景）。数值一致性验证方法：合并前对固定输入生成输出 A，合并后生成输出 B，两者在容差内一致即通过——这是发布前最便宜的回归测试。

## 形态二：动态多适配器

多租户场景（每个用户一个风格适配器）、多任务场景（50+ 下游任务共底座）下，逐任务复制 14GB 底座不可行。方案是单底座 + 请求级适配器挂载：请求到达时，内核把该请求 token 的矩阵乘路由到对应适配器上，**不同请求可并发使用不同适配器**。

vLLM 的实现：启动时 `--lora-modules name=path` 静态加载，或设 `VLLM_ALLOW_RUNTIME_LORA_UPDATING=True` 后通过 HTTP 接口动态增删；推理时每个请求带 `LoRARequest(lora_name, lora_int_id, lora_path)`，vLLM 为该请求应用对应权重。适配器生命周期由 **LoRAModelManager** 管理：扫描模型把支持层替换为 LoRA 能力版本 → 加载权重到 GPU 侧的 LoRA 权重缓冲 → 容量超限时用 **AdapterLRUCache** 逐出最久未用的适配器（类似操作系统页面置换）。

## Punica 内核：多适配器的计算核心

朴素实现是逐适配器做 GEMM（batch 内各请求不同适配器 → 无法合并乘），Punica 用分片批处理内核解决：**bgmv_shrink** 把同一请求组的 `x @ lora_a[i]` 按适配器分片堆叠执行，**bgmv_expand** 对称地做 `x[i] @ lora_b[i]` 并把结果按 offset 写回目标列。prefill 与 decode 使用不同的内核变体（decode 的 token 维度为 1，需要专用 kernel 才能喂满 GPU）；MoE 模型有 `add_lora_fused_moe` 融合内核，把专家选择与 LoRA 计算一次完成。token 与适配器的映射通过 `token_lora_mapping` 维护，并支持专家并行（EP+LoRA）下的按 rank 局部映射——vLLM 0.14/0.15 文档中 punica_wrapper 已覆盖 GPU（含 Triton 回退）/CPU/NPU 三平台。

这套机制源于学术界的 **S-LoRA** 与 Punica 工作：统一内存池（一个共享缓冲按需分配适配器）+ 分页 KV 缓存思路的迁移，实现"单底座同时服务数千适配器、显存随同时激活的适配器数而非总数增长"。

## vLLM 多适配器配置示例

启动与请求两侧的完整形态（vLLM 0.14/0.15）：

```bash
# 启动：静态预加载两个适配器 + 开启运行时更新
python -m vllm.entrypoints.openai.api_server \
  --model Qwen/Qwen3-8B \
  --lora-modules task-a=/adapters/task-a task-b=/adapters/task-b \
  --max-lora-rank 16 \
  --enable-lora \
  --max-cpu-loras 10
# 运行时更新需要环境变量
VLLM_ALLOW_RUNTIME_LORA_UPDATING=True
```

```python
# 请求侧：每个请求显式指定适配器
from openai import OpenAI
client = OpenAI(base_url="http://localhost:8000/v1", api_key="-")
resp = client.chat.completions.create(
    model="Qwen/Qwen3-8B",            # model 字段填底座名
    extra_body={
        "lora_request": {
            "lora_name": "task-a",    # 与 --lora-modules 名称对应
            "lora_int_id": 1,
            "lora_path": "/adapters/task-a",
        }
    },
    messages=[{"role": "user", "content": "..."}],
)
```

要点：`--max-lora-rank` 是 GPU 缓冲内预分配的秩上限，超过上限的适配器直接加载失败（不是降级），规划时要覆盖所有适配器的最大 r；动态加载的适配器受 `--max-cpu-loras` 上限约束，超出后走 LRU 逐出路径。

## S-LoRA 的设计思想拆解

S-LoRA 学术工作（vLLM 实现的直接前身）的三个设计决策值得单独理解：**统一内存池**——所有适配器权重按层切块（每层每适配器的 A/B 拆成固定块），动态分配给当前请求，池容量固定、按需驻留，这是"显存不随适配器总数增长"的结构保证；**分页化调度**——把 KV 缓存的 paged attention 思想移植到适配器权重上，请求级粒度分配与释放；**Punica 内核**——批处理内混合适配器的矩阵乘无法合并为单一 GEMM，bgmv（batched GEMM with variable inputs）系列内核按"适配器分组 × 分片堆叠"把计算打满 GPU。三者的关系：内核解决"算得快"，内存池解决"装得下"，调度解决"切换得顺"。理解这套设计后，看到"多 LoRA 服务"就应该自动映射到这三个问题，而不是把它当成一个黑盒功能。

## 容量规划

每个适配器的 GPU 常驻显存 = 2 × r × (挂载层参数量总和) × 2 字节（A+B，fp16）。7B 模型全线性层挂载、r=16 时约 200-400MB/适配器；4090 (24GB) 静态驻留 40-60 个适配器后触发 LRU 逐出，逐出涉及 CPU-GPU 拷贝，QPS 会抖动。**S-LoRA 式共享池的关键收益：物理驻留上限由池大小决定，可以显著小于适配器总数**。容量公式的核心变量是"同时激活的最大适配器数"，不是总数。

算一个具体例子：Qwen3-8B 全线性层挂载、r=16 的适配器驻留约 300MB；24GB 显卡给 LoRA 池留 12GB（其余留给 KV 缓存与激活）→ 静态驻留约 40 个适配器。若线上 200 个适配器但并发请求最多同时用 20 个，池大小按"峰值激活数 × 1.5 余量 = 30"规划即可，其余靠 LRU 逐出兜底——这就是共享池设计比"每适配器常驻"省 6 倍显存的原因。

## 多适配器服务的运维监控

动态多适配器的运维与单模型服务完全不同，监控面多一层"适配器维度"：**逐适配器质量监控**——同一底座下不同适配器的质量波动不能归因于底座，必须按适配器聚合（自动摘除连续质量下降的适配器，回滚到上一版本）；**LRU 逐出率**——逐出率长期高位说明池容量不足，应扩池或收缩适配器驻留；**适配器加载失败率**——`max-lora-rank` 上限被超是首因，报错要能定位到具体适配器；**混批副作用**——不同适配器请求混批时，逐出/加载造成的延迟毛刺会被放大，建议按适配器热点分桶（热适配器常驻、冷适配器共享池）。这些监控可以复用现成可观测性栈（Prometheus 打点 + 告警），但指标设计必须加上 lora_name 维度——缺了这个维度，多适配器服务出问题就是黑盒。

## 形态选型

| 维度 | 静态合并 | 动态多适配器 |
|---|---|---|
| 适配器数量 | 1（或多份模型副本） | 数十至数千 |
| 推理开销 | 零 | 内核调度微量 |
| 切换延迟 | 无切换概念 | 逐出/加载有抖动 |
| 适用 | 单任务交付、框架兼容 | 多租户、多任务在线服务 |
| 代表实现 | HF transformers | vLLM / S-LoRA / Punica |

> 🎯 **核心要点**：静态合并是"训练产物交付"，动态服务是"多任务在线架构"——前者利用 LoRA 的数学等价性，后者利用 LoRA 的轻量可插拔性。Punica 内核解决了多适配器批处理的算子级难题，这是 LoRA 能从"个人工具"升级为"平台能力"的关键一跳。落地时记住三件事：max-lora-rank 上限先规划、监控指标带 lora_name 维度、逐出率是容量健康度晴雨表。

---

**参考来源**：

- [vLLM LoRA Adapter Management | DeepWiki](https://deepwiki.com/vllm-project/vllm/6.4-lora-adapter-management)
- [vllm.lora.punica_wrapper API (v0.14.0)](https://docs.vllm.ai/en/v0.14.0/api/vllm/lora/punica_wrapper/)
- [punica_gpu.py | vllm-project/vllm](https://github.com/vllm-project/vllm/blob/469f3dcf/vllm/lora/punica_wrapper/punica_gpu.py)
- [Module 7: Production Deployment of Open Models | EngineersOfAI](https://engineersofai.com/docs/open-source-models/production-deployment/overview)
- [PEFT: Parameter-Efficient Fine-Tuning for LLMs & Diffusion | DEV.co](https://dev.co/ai/frameworks/peft)

**下一模块**：[08-多模态与扩散模型应用](08-多模态与扩散模型应用.md) / **返回总览**：[00-LoRA深度剖析总览](00-LoRA深度剖析总览.md)
