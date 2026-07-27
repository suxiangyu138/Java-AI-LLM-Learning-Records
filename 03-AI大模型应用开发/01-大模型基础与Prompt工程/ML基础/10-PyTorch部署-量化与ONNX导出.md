# 10 - PyTorch 部署：量化与 ONNX 导出

> 🎯 模型训好了不代表能上线 — 量化压缩、ONNX 导出、Java 端加载，这是模型从 Python 走向生产的关键三步

---

## 目录

1. [模型部署方案概览](#1-模型部署方案概览)
2. [模型量化](#2-模型量化)
3. [TorchScript 导出](#3-torchscript-导出)
4. [ONNX 导出与 Java 推理](#4-onnx-导出与-java-推理)
5. [部署方案对比](#5-部署方案对比)

---

## 1. 模型部署方案概览

```text
PyTorch 模型 → 生产环境的路径：

  Python 训练 → TorchScript → C++ 推理（LibTorch）
  Python 训练 → ONNX → ONNX Runtime（Java/C#/C++）
  Python 训练 → 量化 → GGUF → llama.cpp / Ollama
  Python 训练 → 量化 → TensorRT → NVIDIA GPU 推理

对 Java 后端开发者最实用的路径：
  PyTorch → ONNX → ONNX Runtime Java API
```

---

## 2. 模型量化

### 2.1 量化原理

```text
量化 = 将 FP32 权重转换为 INT8/INT4，减少模型大小和推理时间

  FP32 权重：32 bit/参数 → 7B 模型 ≈ 28 GB
  INT8 权重： 8 bit/参数 → 7B 模型 ≈ 7 GB （缩小 75%）
  INT4 权重： 4 bit/参数 → 7B 模型 ≈ 3.5 GB（缩小 87%）

精度损失：
  INT8：几乎无损（< 0.5%）
  INT4：轻微损失（< 2%）
  INT2：明显损失，不推荐
```

### 2.2 静态量化

```python
import torch.quantization as quant

# ① 设置量化配置
model.qconfig = quant.get_default_qconfig('fbgemm')
# 或使用 x86 优化的配置

# ② 插入量化观测器
model_prepared = quant.prepare(model, inplace=False)

# ③ 用校准数据校准（确定激活值的范围）
with torch.no_grad():
    for batch in calibration_loader:
        model_prepared(batch)

# ④ 转换为量化模型
model_quantized = quant.convert(model_prepared, inplace=False)

# ⑤ 保存
torch.save(model_quantized.state_dict(), "model_int8.pt")
```

### 2.3 动态量化（推荐，更简单）

```python
# 动态量化 — 只量化权重，激活值在推理时动态量化
# 适合 LLM（大部分时间花在矩阵乘法上）

model_int8 = torch.quantization.quantize_dynamic(
    model,                          # 原始 FP32 模型
    {nn.Linear, nn.LSTM},          # 要量化的层类型
    dtype=torch.qint8               # 目标类型
)

# 保存和加载与普通模型一样
torch.save(model_int8.state_dict(), "model_int8.pt")

# 对比大小
import os
torch.save(model.state_dict(), "model_fp32.pt")
print(f"FP32: {os.path.getsize('model_fp32.pt')/1e6:.1f} MB")
print(f"INT8: {os.path.getsize('model_int8.pt')/1e6:.1f} MB")
```

---

## 3. TorchScript 导出

### 3.1 Trace vs Script

```python
# 方式 1：Trace（跟踪 — 适合固定输入形状的网络）
example_input = torch.randn(1, 3, 224, 224)
traced_model = torch.jit.trace(model, example_input)
traced_model.save("model_traced.pt")

# 方式 2：Script（脚本 — 编译完整 Python 代码）
scripted_model = torch.jit.script(model)
scripted_model.save("model_scripted.pt")

# 对比
# Trace:  快、简单，但控制流（if/for）会被固定
# Script: 支持控制流，但编写更复杂
# 推荐：  先用 Trace，如有控制流问题再转 Script
```

### 3.2 加载与推理（C++ 端可用）

```python
# Python 加载
model = torch.jit.load("model_traced.pt")
output = model(input_tensor)

# 优化推理
model = torch.jit.optimize_for_inference(model)
model.save("model_optimized.pt")
```

---

## 4. ONNX 导出与 Java 推理

### 4.1 PyTorch → ONNX

```python
import torch.onnx

# ① 导出 ONNX
dummy_input = torch.randn(1, 3, 224, 224)

torch.onnx.export(
    model,                          # PyTorch 模型
    dummy_input,                    # 示例输入（用于 trace 形状）
    "model.onnx",                   # 输出路径
    input_names=['input'],          # 输入节点名
    output_names=['output'],        # 输出节点名
    dynamic_axes={                  # 动态轴（支持可变 batch）
        'input': {0: 'batch_size'},
        'output': {0: 'batch_size'}
    },
    opset_version=17                # ONNX opset 版本
)

print("ONNX 导出成功")
```

### 4.2 验证 ONNX 模型

```python
import onnx
import onnxruntime as ort
import numpy as np

# 检查模型结构
onnx_model = onnx.load("model.onnx")
onnx.checker.check_model(onnx_model)
print("ONNX 模型验证通过")

# ONNX Runtime 推理（验证一致性）
session = ort.InferenceSession("model.onnx")

# PyTorch 推理
with torch.no_grad():
    pt_output = model(dummy_input).numpy()

# ONNX Runtime 推理
ort_output = session.run(None, {'input': dummy_input.numpy()})[0]

# 对比
diff = np.abs(pt_output - ort_output).max()
print(f"PyTorch vs ONNX 最大差异: {diff:.6f}")
assert diff < 1e-4, "输出不一致！"
```

### 4.3 Java 端加载 ONNX（关键！）

```xml
<!-- Maven 依赖 -->
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.17.0</version>
</dependency>
```

```java
import ai.onnxruntime.*;

public class ONNXInference {
    public static void main(String[] args) throws OrtException {
        // ① 创建 ONNX Runtime 环境
        try (OrtEnvironment env = OrtEnvironment.getEnvironment();
             OrtSession session = env.createSession("model.onnx")) {
            
            // ② 准备输入
            float[] inputData = new float[1 * 3 * 224 * 224]; // 填充输入数据
            long[] inputShape = {1, 3, 224, 224};
            OnnxTensor inputTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(inputData), inputShape
            );
            
            // ③ 推理
            OrtSession.Result result = session.run(
                Collections.singletonMap("input", inputTensor)
            );
            
            // ④ 获取输出
            OnnxTensor outputTensor = (OnnxTensor) result.get("output").get();
            float[][] outputData = (float[][]) outputTensor.getValue();
            
            System.out.println("推理完成，输出维度: " + outputData.length);
        }
    }
}
```

### 4.4 DJL (Deep Java Library) — 更友好的 Java API

```xml
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>djl-pytorch-engine</artifactId>
    <version>0.27.0</version>
</dependency>
```

```java
import ai.djl.*;
import ai.djl.inference.Predictor;
import ai.djl.translate.Translator;

// DJL 比 ONNX Runtime 更 Java 友好
Criteria<Image, Classifications> criteria = Criteria.builder()
    .setTypes(Image.class, Classifications.class)
    .optModelPath(Paths.get("model.onnx"))
    .optEngine("OnnxRuntime")
    .build();

try (ZooModel<Image, Classifications> model = criteria.loadModel();
     Predictor<Image, Classifications> predictor = model.newPredictor()) {
    
    Image img = ImageFactory.getInstance().fromFile(Paths.get("cat.jpg"));
    Classifications result = predictor.predict(img);
    System.out.println(result);
}
```

---

## 5. 部署方案对比

| 方案 | 接口语言 | 性能 | LLM 适用 | 复杂度 |
|------|:---:|:---:|:---:|:---:|
| **ONNX Runtime Java** | Java | ⭐⭐⭐⭐ | ✅ 传统模型 | ⭐⭐ |
| **DJL** | Java | ⭐⭐⭐⭐ | ✅ 多引擎 | ⭐ |
| **TorchScript + LibTorch** | C++ | ⭐⭐⭐⭐⭐ | ✅ | ⭐⭐⭐⭐ |
| **TensorRT** | C++/Python | ⭐⭐⭐⭐⭐ | ✅ GPU极致 | ⭐⭐⭐⭐ |
| **llama.cpp / GGUF** | C++ | ⭐⭐⭐ | ✅ LLM专属 | ⭐⭐ |
| **Ollama API** | HTTP | ⭐⭐⭐ | ✅ 最简单 | ⭐ |

```text
对 Java 后端开发者的推荐：

  小型传统模型 → ONNX + ONNX Runtime Java
  LLM 推理 → Ollama HTTP API（最简单）
  高性能 LLM 推理 → vLLM 独立服务 + Java HTTP 调用
```

---

## 核心要点回顾

- INT8 量化几乎无损、显存减 75%；推荐动态量化
- ONNX 是 PyTorch → Java 的桥梁（最成熟的跨语言方案）
- Java 端 ONNX Runtime API 或 DJL 都可以加载 ONNX 模型
- `torch.load()` 需设置 `weights_only=True` 防止安全风险
- LLM 推理推荐 Ollama/vLLM 独立服务 + HTTP 调用，而非直接嵌入 Java
