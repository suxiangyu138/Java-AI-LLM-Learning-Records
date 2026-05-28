# DJL (Deep Java Library) 核心知识点

## 一、概述

Deep Java Library（DJL）是亚马逊 AWS 开源的 Java 原生深度学习框架，由 Java 引擎团队开发维护。它让 Java 开发者无需学习 Python 即可加载、运行和微调深度学习模型。

**核心定位：** Java 原生深度学习推理引擎，直接加载 PyTorch/TensorFlow 模型，适合 **模型推理工程师**。

**官网：** https://djl.ai

## 二、核心特性

### 2.1 引擎抽象层

```
┌─────────────────────────────────────────────┐
│              DJL API (Java 接口)             │
├─────────────────────────────────────────────┤
│  ┌──────────┬──────────┬──────────────────┐ │
│  │ PyTorch  │TensorFlow│  ONNX Runtime    │ │
│  │ Engine   │ Engine   │  Engine          │ │
│  └──────────┴──────────┴──────────────────┘ │
├─────────────────────────────────────────────┤
│        底层 C++ 推理库                       │
│  LibTorch / TensorFlow C / ONNX Runtime     │
└─────────────────────────────────────────────┘
```

### 2.2 核心优势

| 特性 | 说明 |
|------|------|
| **Java 原生** | 纯 Java API，无需 JNI 编程或 Python 进程间通信 |
| **多引擎** | 同一套 API 切换 PyTorch、TensorFlow、ONNX 引擎 |
| **模型动物园** | 预训练模型库，一行代码加载 ResNet、BERT 等 |
| **GPU 加速** | 自动检测 CUDA，利用 GPU 推理 |
| **Spark 集成** | 大规模分布式推理，海量数据批量处理 |
| **Android 支持** | 移动端推理，模型可部署到 Android 设备 |

## 三、快速上手

### 3.1 依赖

```xml
<dependency>
    <groupId>ai.djl</groupId>
    <artifactId>api</artifactId>
    <version>0.28.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-engine</artifactId>
    <version>0.28.0</version>
</dependency>
```

### 3.2 加载预训练模型

```java
// 从 ModelZoo 加载预训练 ResNet50
Criteria<Image, Classifications> criteria = Criteria.builder()
    .setTypes(Image.class, Classifications.class)
    .optArtifactId("ai.djl.pytorch:resnet")
    .optTranslator(ImageClassificationTranslator.builder()
        .addTransform(new Resize(224, 224))
        .addTransform(new ToTensor())
        .build())
    .build();

try (ZooModel<Image, Classifications> model = criteria.loadModel();
     Predictor<Image, Classifications> predictor = model.newPredictor()) {

    Image img = ImageFactory.getInstance()
        .fromFile(Paths.get("cat.jpg"));
    Classifications result = predictor.predict(img);
    System.out.println(result);
}
```

### 3.3 训练模型

```java
// 定义模型结构
Model model = Model.newInstance("my-mlp");
SequentialBlock block = new SequentialBlock()
    .add(Blocks.batchFlattenBlock(28 * 28))
    .add(Linear.builder().setUnits(128).build())
    .add(Activation.reluBlock())
    .add(Linear.builder().setUnits(10).build());
model.setBlock(block);

// 训练配置
DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
    .addEvaluator(new Accuracy())
    .optDevices(Device.getDevices(1))  // 单 GPU
    .addTrainingListeners(TrainingListener.Defaults.logging());

// 开始训练
try (Trainer trainer = model.newTrainer(config)) {
    trainer.initialize(new Shape(1, 28 * 28));
    EasyTrain.fit(trainer, numEpochs, trainingDataset, validationDataset);
}
```

### 3.4 加载 Python 训练的模型

```java
// 直接加载 PyTorch 保存的 .pt 文件
Path modelPath = Paths.get("bert-finetuned.pt");
Model model = Model.newInstance("bert");
model.load(modelPath);

// 加载 TensorFlow 模型
Criteria<NDList, NDList> criteria = Criteria.builder()
    .optEngine("TensorFlow")
    .optModelPath(Paths.get("saved_model/"))
    .build();
```

## 四、与 LLM 集成（2026 年新特性）

DJL 在 2024 年底推出了 `djl-serving` 和 LLM 支持：

```java
// DJL Serving：将大模型部署为 HTTP 服务
// 支持 HuggingFace 模型格式

// Java 侧调用
HuggingFaceModel model = HuggingFaceModel.newInstance(
    "s3://my-models/llama3-8b/",
    Engine.getEngine("PyTorch"));

// 或使用 OpenAI 兼容 API
ChatClient client = ChatClient.builder()
    .endpoint("http://localhost:8080/v1")
    .build();
```

## 五、典型应用场景

| 场景 | 说明 |
|------|------|
| **NLP 文本处理** | 命名实体识别、情感分析、文本分类 |
| **计算机视觉** | 图像分类、目标检测、OCR |
| **推荐系统** | 大规模 Embedding 召回、排序 |
| **LLM 本地推理** | 将 Python 训练的大模型直接 Java 加载推理 |
| **Spark 批处理** | 在数据湖上对亿级数据进行分布式推理 |

## 六、DJL vs Python ML 生态

| 维度 | DJL | Python (PyTorch/TF) |
|------|-----|---------------------|
| 开发语言 | **Java 原生** | Python |
| 模型训练 | 支持，但生态较小 | **主力生态** |
| 生产推理 | **优秀**（Java 部署成熟） | 需额外 Serving 方案 |
| 与 Java 后端集成 | **无缝** | 需 HTTP/gRPC 中间层 |
| GPU 支持 | CUDA / MPS | CUDA / MPS / ROCm |
| 社区规模 | 小 | **巨大** |

## 七、总结

DJL 不是要替代 Python 做模型训练，它的主战场是 **Java 生产环境的模型推理**。核心价值在于：让 Python 训练好的模型直接在 Java 后端中加载运行，省去模型 Serving 中间层的复杂性和延迟。
