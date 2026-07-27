# 02 - KV Cache 管理与优化

> 🎯 KV Cache 是 LLM 推理显存的头号杀手 — 长序列+高并发时，KV Cache 占用远超模型权重。PagedAttention 把利用率从 30% 拉到 90%

## KV Cache 原理

```text
自回归生成时，每个新 token 需要与所有历史 token 做 Attention

第1步：算 "Hello" → 存 K₁,V₁
第2步：算 "Hello world" → 复用 K₁,V₁ + 算 K₂,V₂
第3步：算 "Hello world !" → 复用 K₁₂,V₁₂ + 算 K₃,V₃

不缓存：每次 O(n²) 从头算 → 1000 个 token → 50万次计算
有缓存：O(n) 新 token → 1000 个 token → 1000 次计算

KV Cache 的大小：
  size = 2 × num_layers × hidden_dim × seq_len × bytes × batch_size
  2 × 32 × 4096 × 8192 × 2 × 1 = 4 GB（单个请求 8K 上下文）
```

## 传统 KV Cache 的问题

```text
问题：预分配连续显存 → 碎片化

  请求A 预分配 max_len=8192 → 实际只用 500 tokens → 浪费 93%
  请求B 预分配 max_len=4096 → 实际只用 2000 tokens → 浪费 50%

实际利用率 ~30% → 80GB 显存只用了 24GB → 浪费 56GB
```

## PagedAttention 解决方案

```text
将 KV Cache 分成固定大小的 Page（类似 OS 虚拟内存分页）

  Page Pool: [page₀][page₁][page₂]...[pageₙ]
  
  请求A：分配 page₀, page₃, page₇ ← 只分配需要的
  请求B：分配 page₁, page₄, page₅, page₈
  请求C：分配 page₂, page₆

  所有请求共享 Page Pool → 按需分配 → 零碎片 → 利用率 ~90%
```

## KV Cache 量化

```python
# INT8 量化 KV Cache → 显存减半
from vllm import LLM
llm = LLM(model="Qwen/Qwen2-7B",
          kv_cache_dtype="fp8",  # FP8 量化 KV Cache
          max_model_len=8192)
```

| 精度 | KV Cache 大小(8K/请求) | 精度损失 |
|------|:---:|:---:|
| FP16 | 4 GB | 基准 |
| FP8 | 2 GB | 几乎无损 |
| INT8 | 2 GB | 几乎无损 |
| INT4 | 1 GB | 轻微 |

## 显存优化效果对比

| 优化 | 7B模型+8K上下文+8并发 |
|------|:---:|
| 原始分配 | ~48 GB（30%利用率） |
| +PagedAttention | ~18 GB（90%利用率） |
| +FP8 KV Cache | ~12 GB |
| +FP8+INT4权重 | ~5 GB |
