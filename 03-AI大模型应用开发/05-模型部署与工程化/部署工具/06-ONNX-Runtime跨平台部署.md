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
