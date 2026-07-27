# 05 - TensorRT 推理加速

> 🎯 TensorRT = NVIDIA 官方的推理加速引擎 — 图优化 + INT8 量化 + Kernel 融合，GPU 推理速度提升 3-5×

### 核心优化

```text
TensorRT 的优化技术栈：

① 层融合 (Layer Fusion)：Conv+BN+ReLU → 一个 Kernel
② 精度校准 (INT8 Calibration)：找到最优量化参数
③ Kernel 自动调优：为 GPU 架构选择最优 Kernel
④ 显存优化：复用中间结果内存
⑤ 动态形状支持：处理可变 batch_size/序列长度
```

### 使用流程

```python
# ① PyTorch → ONNX
torch.onnx.export(model, dummy_input, "model.onnx")

# ② ONNX → TensorRT Engine
trtexec --onnx=model.onnx --saveEngine=model.engine \
        --fp16 --int8 --calib=calibration.cache

# ③ Python 推理
import tensorrt as trt
runtime = trt.Runtime(trt.Logger())
with open("model.engine", "rb") as f:
    engine = runtime.deserialize_cuda_engine(f.read())
```

### 性能对比

| 引擎 | Llama-7B QPS | 延迟 P50 | 显存 |
|------|:---:|:---:|------|
| HF Transformers | 15 | 500ms | 16GB |
| vLLM | 150 | 80ms | 14GB |
| TensorRT-LLM | 200 | 50ms | 12GB |

> TensorRT-LLM 是 NVIDIA 专为 LLM 优化的版本，集成了 FlashAttention、PagedAttention 等最新技术。
