# 第5步：HuggingFace与开源模型库

> **阶段目标：** 熟练使用HuggingFace生态（Model Hub, Datasets, Pipelines, Transformers），能够加载、使用和评估任何开源模型  
> **预计学时：** 1-2周（每天3-4小时）  
> **前置要求：** Transformer架构基础  

---

## 📚 目录

- [5.1 HuggingFace生态全景](#51-huggingface生态全景)
- [5.2 Transformers库核心](#52-transformers库核心)
- [5.3 Model Hub使用指南](#53-model-hub使用指南)
- [5.4 Datasets数据处理](#54-datasets数据处理)
- [5.5 Pipelines快速上手](#55-pipelines快速上手)
- [5.6 模型选型策略](#56-模型选型策略)
- [5.7 阶段练习](#57-阶段练习)
- [5.8 常见问题](#58-常见问题)

---

## 5.1 HuggingFace生态全景

```
HuggingFace 生态圈
═══════════════════════════════════════════════════════════
                    ┌──────────────────┐
                    │   🤗 Hub         │  ← 模型/数据集/应用的GitHub
                    │ huggingface.co   │
                    └──────┬───────────┘
        ┌──────────────────┼──────────────────┐
        ↓                  ↓                  ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ transformers │  │   datasets   │  │   evaluate   │
│ 模型加载/训练 │  │  数据加载/处理 │  │  评估指标    │
└──────────────┘  └──────────────┘  └──────────────┘
        ↓                  ↓                  ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  tokenizers  │  │  accelerate  │  │    peft      │
│ 快速Token化  │  │  分布式训练   │  │  参数高效微调 │
└──────────────┘  └──────────────┘  └──────────────┘
        ↓                  ↓                  ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   diffusers  │  │  gradio      │  │    trl       │
│  图像生成    │  │  Demo搭建    │  │  RLHF训练    │
└──────────────┘  └──────────────┘  └──────────────┘
```

---

## 5.2 Transformers库核心

### 5.2.1 Auto Classes — 通用加载接口

```python
from transformers import (
    AutoTokenizer,      # 自动匹配Tokenizer
    AutoModel,          # 自动匹配基础模型（无任务头）
    AutoModelForCausalLM,       # 自回归语言模型（GPT-like）
    AutoModelForSequenceClassification,  # 文本分类模型
    AutoModelForQuestionAnswering,       # 问答模型
)

# ========== AutoModel: 万能加载器 ==========
# 不需要知道具体模型类名，Auto前缀帮你自动选择
model_name = "Qwen/Qwen2-0.5B-Instruct"

tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(
    model_name,
    torch_dtype="auto",           # 自动选择精度
    device_map="auto",            # 自动分配设备（CPU/GPU）
)

print(f"模型类型: {type(model).__name__}")
print(f"参数量: {sum(p.numel() for p in model.parameters()):,}")
print(f"词汇表大小: {len(tokenizer)}")
```

### 5.2.2 Tokenizer深入

```python
# ========== Tokenizer的核心方法 ==========
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2-0.5B-Instruct")

# 1. 编码：文本 → Token IDs
text = "你好，请问怎么学习AI？"
encoded = tokenizer.encode(text)
print(f"编码: {encoded}")

# 2. 批量编码（推荐使用__call__）
batch = [
    "你好",
    "今天天气不错",
    "请帮我写一段代码",
]
encoded_batch = tokenizer(
    batch,
    padding=True,          # 自动填充到相同长度
    truncation=True,       # 自动截断超长文本
    max_length=512,
    return_tensors='pt',   # 返回PyTorch Tensor
)
print(f"input_ids shape: {encoded_batch['input_ids'].shape}")
print(f"attention_mask: {encoded_batch['attention_mask']}")

# 3. 解码：Token IDs → 文本
decoded = tokenizer.decode(encoded_batch['input_ids'][0])
print(f"解码: {decoded}")

# 4. Chat Template（对话模板）
messages = [
    {"role": "system", "content": "你是一个AI助手"},
    {"role": "user", "content": "什么是机器学习？"},
]
# 应用模型的Chat Template
prompt = tokenizer.apply_chat_template(
    messages,
    tokenize=False,        # True返回token_ids, False返回文本
    add_generation_prompt=True,
)
print(f"格式化后的Prompt:\n{prompt}")
```

### 5.2.3 模型推理基础

```python
import torch

def generate_text(
    model,
    tokenizer,
    prompt: str,
    max_new_tokens: int = 256,
    temperature: float = 0.7,
    top_p: float = 0.9,
) -> str:
    """标准文本生成函数"""
    
    # 编码输入
    inputs = tokenizer(prompt, return_tensors='pt').to(model.device)
    
    # 生成
    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=max_new_tokens,
            temperature=temperature,
            top_p=top_p,
            do_sample=True,            # 使用采样（非贪心）
            pad_token_id=tokenizer.pad_token_id,
            eos_token_id=tokenizer.eos_token_id,
        )
    
    # 解码输出（只取新生成的部分）
    generated_ids = outputs[0][inputs['input_ids'].shape[1]:]
    response = tokenizer.decode(generated_ids, skip_special_tokens=True)
    
    return response


# ========== 生成参数详解 ==========
"""
temperature (0-2):
  0: 确定性地选最可能的Token → 输出稳定但无聊
  0.7: 适度的随机性 → 大多数场景的推荐值
  1.5: 高随机性 → 创意写作
  2.0: 几乎随机 → 通常不建议

top_p (nucleus sampling):
  0.1: 只在最可能的10%积累Token中选择 → 非常保守
  0.5: 中等
  0.9: 推荐值，在质量和多样性间平衡

top_k:
  40: 只从最可能的40个Token中选 → 经典值

建议: 同时调temperature和top_p效果最好
      通常只调temperature，top_p保持0.9即可
"""
```

---

## 5.3 Model Hub使用指南

### 5.3.1 搜索模型

```python
from huggingface_hub import HfApi, list_models, ModelFilter

api = HfApi()

# 搜索模型（编程方式）
models = list(list_models(
    filter=ModelFilter(
        task="text-generation",  # 任务类型
        language="zh",           # 语言
        library="transformers",
    ),
    sort="downloads",           # 按下载量排序
    direction=-1,               # 降序
    limit=20,
))

for m in models:
    print(f"{m.modelId:40s} | ⬇ {m.downloads:>10,} | ❤ {m.likes:>5}")

# 常见中文大模型:
# - Qwen/Qwen2-7B-Instruct         ← 阿里通义千问（推荐）
# - 01-ai/Yi-6B-Chat               ← 零一万物
# - THUDM/chatglm3-6b              ← 清华智谱
# - baichuan-inc/Baichuan2-7B-Chat ← 百川
```

### 5.3.2 下载与管理

```python
from huggingface_hub import snapshot_download, hf_hub_download

# 下载整个模型仓库
snapshot_download(
    repo_id="sentence-transformers/all-MiniLM-L6-v2",
    local_dir="./models/all-MiniLM-L6-v2",
    local_dir_use_symlinks=False,  # 不使用符号链接（Win兼容）
    resume_download=True,          # 断点续传
)

# 下载单个文件
hf_hub_download(
    repo_id="google/gemma-2b",
    filename="config.json",
    local_dir="./models/gemma-2b",
)
```

### 5.3.3 模型卡(Model Card)阅读指南

```markdown
阅读一个HuggingFace Model Card时，重点关注：

1. 模型类型: Base vs Instruct/Chat → 决定是否需要套Chat Template
2. 上下文长度: Context Window → 决定能处理多长的输入
3. 支持语言: 是否支持中文 → 决定是否适合你的场景
4. 许可证(License): 
   - Apache 2.0 / MIT → 商用友好
   - Llama License → 有商用限制
   - CC BY-NC → 仅限非商用
5. 推荐参数: temperature, top_p 推荐值
6. 硬件要求: 最低显存 → 决定能不能本地跑
```

---

## 5.4 Datasets数据处理

### 5.4.1 数据集加载

```python
from datasets import load_dataset, Dataset, DatasetDict

# ========== 从HuggingFace Hub加载 ==========
# 加载SQuAD问答数据集
squad = load_dataset("squad", split="train[:1000]")
print(squad)  # Dataset({features: ['id', 'title', 'context', 'question', 'answers']})

# ========== 从本地加载 ==========
dataset = load_dataset("json", data_files="data/my_data.json")

# ========== 从字典创建 ==========
my_data = Dataset.from_dict({
    'prompt': ["你好", "今天天气怎么样", "写一个排序算法"],
    'completion': ["你好！有什么可以帮助你的？", "抱歉，我没有天气信息", "def sort(arr):\n    ..."],
})

# ========== 划分数据集 ==========
splits = my_data.train_test_split(test_size=0.2, seed=42)
print(splits)          # DatasetDict({train: Dataset(2 rows), test: Dataset(1 row)})
```

### 5.4.2 数据处理

```python
# ========== Map: 批量处理 ==========
def tokenize_function(examples):
    """Token化函数 — 应用于整个数据集"""
    return tokenizer(
        examples['prompt'],
        padding='max_length',
        truncation=True,
        max_length=128,
    )

# 并行处理
tokenized_dataset = my_data.map(
    tokenize_function,
    batched=True,           # 批量处理（更快）
    num_proc=4,             # 4进程并行
    remove_columns=['prompt', 'completion'],  # 移除原始列
)

# ========== Filter: 数据过滤 ==========
def is_long_enough(example):
    return len(example['prompt']) > 10

filtered = my_data.filter(is_long_enough)

# ========== 流式处理（超大数据集）==========
# 不一次性加载到内存，适合TB级别数据
stream_dataset = load_dataset("c4", "en", split="train", streaming=True)
first_batch = next(iter(stream_dataset.take(10)))
```

---

## 5.5 Pipelines快速上手

```python
from transformers import pipeline

# Pipeline = 预构建的模型+Tokenizer+预处理+后处理全套管线
# 适合快速验证和原型开发

# ========== 文本生成 ==========
generator = pipeline(
    "text-generation",
    model="Qwen/Qwen2-0.5B-Instruct",
    device=0,  # GPU设备ID，-1表示CPU
)
result = generator("请解释什么是人工智能", max_new_tokens=100)
print(result[0]['generated_text'])

# ========== 情感分析 ==========
classifier = pipeline("sentiment-analysis")
results = classifier(["这个产品很棒！", "质量太差了"])
for r in results:
    print(f"{r['label']}: {r['score']:.3f}")

# ========== 命名实体识别 ==========
ner = pipeline("ner", model="dslim/bert-base-NER")
text = "马云于1999年在杭州创立了阿里巴巴"
entities = ner(text)
for e in entities:
    print(f"{e['word']:10s} → {e['entity']}")

# ========== Embedding提取 ==========
extractor = pipeline("feature-extraction", model="bert-base-chinese")
embeddings = extractor("人工智能改变世界", return_tensors=True)

# ========== 翻译 ==========
translator = pipeline("translation", model="Helsinki-NLP/opus-mt-zh-en")
translated = translator("你好世界")
print(translated[0]['translation_text'])  # "Hello World"

# ========== 文本摘要 ==========
summarizer = pipeline("summarization", model="facebook/bart-large-cnn")
summary = summarizer("...(长文本)...", max_length=130, min_length=30)
```

---

## 5.6 模型选型策略

### 5.6.1 决策矩阵

```
                   需要生成文本？
                   ╱          ╲
                 是             否
                 ╱              ╲
          需要中文吗？       需要Embedding？
          ╱      ╲           ╱       ╲
        是       否         是        否
       ╱          ╲        ╱          ╲
   Qwen2/        LLaMA   BGE/        all-MiniLM/
   Yi/ChatGLM    Mistral文本嵌入  text-embedding
   ╱  ╲          ╱  ╲
 7B+  0.5-3B   7B+  1-3B
(强)  (弱但快)  (强) (弱但快)
```

### 5.6.2 2024年推荐模型

| 用途 | 推荐模型 | 大小 | 特点 |
|------|---------|------|------|
| **中文对话** | Qwen2-7B-Instruct | 7B | 中文最强开源 |
| **英文对话** | LLaMA-3-8B-Instruct | 8B | 综合能力强 |
| **代码** | DeepSeek-Coder-V2 | 16B | 代码能力SOTA |
| **中文Embedding** | BAAI/bge-large-zh | 326M | RAG首选 |
| **英文Embedding** | text-embedding-3-small | - | OpenAI API |
| **轻量对话** | Qwen2-0.5B-Instruct | 0.5B | 适合学习/调试 |
| **多模态** | Qwen-VL-Chat | 7B | 图文理解 |

---

## 5.7 阶段练习

### 练习1：模型对比实验
用相同的Prompt测试3个不同模型（Qwen2, LLaMA3, ChatGLM3），对比生成质量和风格差异。

### 练习2：数据处理Pipeline
从HuggingFace加载一个中文数据集，完成清洗→Token化→批处理→保存的全流程。

### 练习3：模型选择工具
写一个脚本，输入任务需求和硬件配置，自动推荐最合适的模型。

---

## 5.8 常见问题

### Q1: transformers库太大了，可以只装需要的吗？

```bash
# 最小安装
pip install transformers

# 按框架安装
pip install transformers[torch]    # PyTorch
pip install transformers[tf]      # TensorFlow

# 按任务安装额外依赖
pip install transformers[sentencepiece]  # Tokenizer后端
pip install transformers[vision]         # 视觉模型
```

### Q2: 模型下载太慢怎么办？

```python
# 方法1: 使用镜像
import os
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'

# 方法2: 使用ModelScope（国内源）
from modelscope import snapshot_download
snapshot_download('qwen/Qwen2-7B-Instruct', cache_dir='./models')
```

### Q3: Pipeline和手动加载有什么区别？

| | Pipeline | 手动加载 |
|---|---|---|
| 代码量 | 3行 | 15-30行 |
| 灵活性 | 低 | 高 |
| 生产环境 | 不推荐 | 推荐 |
| 调试 | 困难 | 容易 |
| 适用场景 | 快速实验 | 产品开发 |

---

> **✅ 阶段完成检查清单：**
> - [ ] 能用AutoModel+AutoTokenizer加载任意模型
> - [ ] 掌握tokenizer的encode/decode/apply_chat_template
> - [ ] 能从HuggingFace Hub搜索和下载模型
> - [ ] 能用datasets库加载和处理数据
> - [ ] 能用Pipeline快速完成5种以上NLP任务
> - [ ] 能根据需求选择合适的模型
>
> **下一步：** [第6步：大模型API应用基础](../06-大模型API应用基础/README.md)
