# 06 - ONNX Runtime 跨平台部署

> 🎯 ONNX = 深度学习模型的"通用格式" — PyTorch 训练 → ONNX 导出 → Java/C#/C++ 等多语言推理

### 导出流程

```python
# PyTorch → ONNX
torch.onnx.export(model, dummy_input, "model.onnx",
    input_names=['input'], output_names=['output'],
    dynamic_axes={'input': {0: 'batch'}, 'output': {0: 'batch'}},
    opset_version=17)

# 验证
import onnx
onnx.checker.check_model(onnx.load("model.onnx"))
```

### Java 端加载

```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.17.0</version>
</dependency>
```

```java
try (OrtEnvironment env = OrtEnvironment.getEnvironment();
     OrtSession session = env.createSession("model.onnx")) {
    
    OnnxTensor input = OnnxTensor.createTensor(env, floatBuffer, shape);
    OrtSession.Result result = session.run(Map.of("input", input));
    float[][] output = (float[][]) result.get("output").get().getValue();
}
```

### 使用 DJL (更友好的 Java API)

```java
Criteria<Image, Classifications> criteria = Criteria.builder()
    .setTypes(Image.class, Classifications.class)
    .optModelPath(Paths.get("model.onnx"))
    .optEngine("OnnxRuntime").build();

try (ZooModel<Image, Classifications> model = criteria.loadModel();
     Predictor<Image, Classifications> predictor = model.newPredictor()) {
    Classifications result = predictor.predict(img);
}
```

> 详细部署参见 [ML基础/10-PyTorch部署](../../01-大模型基础与Prompt工程/ML基础/10-PyTorch部署-量化与ONNX导出.md)

---

## 6. ONNX Runtime 的 2026 定位

**ONNX 在 LLM 时代的角色变化**：

```text
传统角色：CV/NLP 小模型的跨平台部署（Java/移动端/边缘）
LLM 时代：ONNX 不用于大模型在线推理（那是 vLLM/TRT-LLM 的领域）
  → ONNX 定位：小模型（Embedding/分类器/重排模型）+ 边缘部署

2026 引擎对比中的 ONNX 位置：
  大模型在线 → vLLM（GPU 生产默认）
  小模型 Java → ONNX Runtime（DJL 生态）
  边缘/嵌入式 → ONNX / llama.cpp / TFLite
```

**Java 生态的 ONNX 部署（本仓库重点）**：

```java
// DJL（Deep Java Library）加载 ONNX 模型
import ai.djl.repository.zoo.Criteria;
import ai.djl.modality.Input;

Criteria<Input, Output> criteria = Criteria.builder()
        .setTypes(Input.class, Output.class)
        .optModelPath(Paths.get("model.onnx"))
        .optEngine("OnnxRuntime")
        .build();
// 场景：Embedding 模型 / BGE-Reranker 重排模型（RAG 链路 Java 侧）
```

**ONNX 使用速查**：

```text
导出：PyTorch → torch.onnx.export（注意动态轴 dynamic_axes）
优化：onnxruntime-gpu（CUDA EP）/ onnxsim（图简化）
量化：ONNX 静态量化（INT8，精度损失需验证）
Java：DJL / onnxruntime-java 官方绑定
```

> 🎯 **核心要点**：ONNX 的 2026 定位 = "**小模型跨平台 + Java/边缘部署**"——大模型在线推理已属 vLLM 阵营；**Java 后端的 RAG 链路（Embedding/重排小模型）用 ONNX + DJL 是最佳实践**。

---

## 7. ONNX 导出与优化速查

**PyTorch → ONNX 导出的关键参数**：

```python
import torch

torch.onnx.export(
    model,
    (dummy_input,),
    "model.onnx",
    input_names=["input_ids", "attention_mask"],
    output_names=["output"],
    dynamic_axes={                       # 动态轴（变长输入必须！）
        "input_ids": {0: "batch", 1: "seq"},
        "attention_mask": {0: "batch", 1: "seq"},
    },
    opset_version=17,                    # 算子集版本（影响兼容）
)
```

**ONNX 优化三件套**：

```text
① onnxsim：图简化（常量折叠/去冗余）→ 体积与速度优化
② 精度模式：FP32 → FP16（GPU 加速，精度损失小）
③ 量化：INT8 静态量化（需校准数据）→ 边缘部署关键

Java 侧加载：
  onnxruntime-java（官方绑定，直接加载）
  DJL（高层抽象，自动管理 ORT 会话）
```

**ONNX 模型大小与加载参考**：

| 模型类型 | 大小（FP32） | 加载方式 |
|---------|:-----------:|---------|
| Embedding（BGE 小模型） | ~100-400MB | 内存加载 |
| 重排模型（BGE-Reranker） | ~500MB | 内存加载 |
| 分类器/意图识别 | <100MB | 内存加载 |

> 🎯 **核心要点**：ONNX 导出 = "**dynamic_axes（变长必配）+ opset 版本 + onnxsim 优化**"三件事——Java 侧用 `onnxruntime-java` 或 DJL；**小模型（Embedding/重排）是 ONNX 在 RAG 链路中的主战场**。

---

## 8. 引擎定位总结与速查

**2026 推理引擎全家谱定位**：

| 引擎 | 大模型在线 | 小模型 Java | 边缘 | 本地 |
|------|:----------:|:----------:|:----:|:----:|
| vLLM | ✅ 默认 | ❌ | ❌ | ❌ |
| TensorRT-LLM | ✅ 极致 | ❌ | ❌ | ❌ |
| SGLang | ✅ 高吞吐 | ❌ | ❌ | ❌ |
| **ONNX Runtime** | ❌ | ✅ | ✅ | ✅ |
| llama.cpp | ❌ | 部分 | ✅ | ✅ |
| Ollama | ❌ | ❌ | ❌ | ✅ 本地 |

**ONNX 选型速查**：

```text
小模型（Embedding/重排/分类）部署：
  Java 后端 → ONNX Runtime + DJL（本仓库推荐）
  移动端 → ONNX Mobile / TFLite
  边缘 → ONNX + TensorRT EP（NVIDIA 边缘卡）
大模型在线 → 别用 ONNX（vLLM 阵营）

一句话：ONNX = "小模型的跨平台标准"，大模型时代的专用工具
```

> 🎯 **核心要点**：引擎定位 = "**大模型在线 vLLM、小模型 Java/边缘 ONNX、本地 Ollama**"——ONNX 在 2026 是"小模型 + Java/边缘"的专属赛道，与 vLLM 互不竞争。

---

## 9. 部署流程速查（Java 侧完整链路）

**Java 部署 ONNX 模型的完整流程**：

```text
① 导出：Python 侧 torch.onnx.export（dynamic_axes 必配）
② 优化：onnxsim 简化 + 可选 INT8 量化
③ 放置：模型文件进 resources 或外部存储
④ Java 加载：DJL 或 onnxruntime-java
⑤ 推理：输入预处理 → session.run → 后处理
⑥ 服务化：Spring Boot 接口包装（Embedding/重排服务）
```

```java
// onnxruntime-java 直连示例
import ai.onnxruntime.*;

OrtEnvironment env = OrtEnvironment.getEnvironment();
OrtSession session = env.createSession("model.onnx", new OrtSession.SessionOptions());
// 推理：OnnxTensor 输入 → session.run → 输出

// 注意：session 线程安全可复用；输入 shape 需与 dynamic_axes 匹配
```

**部署检查清单**：

```text
□ dynamic_axes 已配置（变长输入）
□ 输入输出名与 Java 侧一致（input_names/output_names）
□ 精度验证（FP16/INT8 对比黄金集）
□ session 复用（勿每次新建）
□ 超时与降级（模型服务不可用时的兜底）
```

> 🎯 **核心要点**：Java ONNX 部署 = "**导出（dynamic_axes）→ 优化 → 加载（session 复用）→ 服务化**"四步——**input/output 名称对齐与 session 复用**是两个最常踩的坑。
