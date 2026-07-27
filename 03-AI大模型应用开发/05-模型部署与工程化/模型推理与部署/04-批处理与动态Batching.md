# 04 - 批处理与动态 Batching

> 🎯 批处理是推理吞吐的倍增器 — 静态Batching吞吐×3、动态Batching×5、连续Batching×10

## 三种批处理对比

| 类型 | 做法 | 利用率 | 延迟 |
|------|------|:---:|:---:|
| **静态 Batching** | 固定 batch_size，等满再算 | 50% | 高 |
| **动态 Batching** | 不等满，到齐就处理 | 70% | 中 |
| **连续 Batching** | 来一个加一个，完一个丢一个 | 90%+ | **低** |

## 连续批处理 Continuous Batching

```text
传统批处理：
  Batch = [reqA(50t), reqB(200t), reqC(30t)]
  → 必须等 reqB 生成完 200 tokens 才结束整个 batch
  → reqA 和 reqC 早就完了 → 槽位空等 → GPU 浪费

连续批处理 (vLLM / TensorRT-LLM)：
  t=0:   Batch = [reqA, reqB, reqC]
  t=1:   reqC 生成结束 → 踢出
  t=1.1: reqD 到达 → 立即加入
  t=2:   Batch = [reqA(继续), reqB(继续), reqD(新)]
  
  → 槽位永不空等 → GPU 利用率 90%+
  → 原理：每个 KV Cache 独立管理，随时增删请求
```

## vLLM 并发配置

```python
llm = LLM(
    model="Qwen/Qwen2-7B",
    max_num_seqs=256,        # 最大并发请求数
    max_num_batched_tokens=8192,  # 单 batch 最大 token 数
    # Prefill 和 Decode 会自动混排 → 连续批处理
)
```

## Flight Batching (TensorRT-LLM)

```text
TensorRT-LLM 的更激进方案：

  将请求拆分为独立 Token → 按 Token 而非按请求调度
  → 比连续批处理更细粒度

效果：极端并发场景下仍有高吞吐
代价：实现复杂度极高
```
