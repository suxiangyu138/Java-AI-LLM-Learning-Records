# 08 - 视觉语言模型 VLM

> 🎯 VLM = Vision Language Model — 能看图回答问题。GPT-4V 最强但闭源，Qwen-VL 是最好的中文开源选择

## VLM 训练两阶段

```text
阶段一：预训练（图文对齐）
  目标：让投影层学会将视觉特征映射到 LLM 语义空间
  数据：亿万级图文对（图片+描述）
  冻结：LLM + 视觉编码器 → 只训练投影层

阶段二：指令微调（SFT）
  目标：让模型学会看图回答问题
  数据：高质量图文问答对
  解冻：LLM（全量或 LoRA）+ 投影层
```

## Qwen-VL 推理

```python
from transformers import Qwen2VLForConditionalGeneration, AutoProcessor

model = Qwen2VLForConditionalGeneration.from_pretrained(
    "Qwen/Qwen2-VL-7B-Instruct",
    torch_dtype="auto", device_map="auto"
)
processor = AutoProcessor.from_pretrained("Qwen/Qwen2-VL-7B-Instruct")

# 图片问答
messages = [{
    "role": "user",
    "content": [
        {"type": "image", "image": "architecture.png"},
        {"type": "text", "text": "分析这个架构图的瓶颈"}
    ]
}]

text = processor.apply_chat_template(messages, tokenize=False)
inputs = processor(text=[text], images=[image], return_tensors="pt").to(model.device)
outputs = model.generate(**inputs, max_new_tokens=256)
print(processor.decode(outputs[0], skip_special_tokens=True))
```

## LLaVA 推理

```python
from llava.model.builder import load_pretrained_model
from llava.mm_utils import process_images, tokenizer_image_token

# LLaVA = ViT + MLP投影 + LLaMA
model, tokenizer = load_pretrained_model("liuhaotian/llava-v1.6-vicuna-7b")

prompt = "<image>\n描述这张图片的内容"
input_ids = tokenizer_image_token(prompt, tokenizer)
output = model.generate(input_ids, images=[image_tensor])
```

## VLM 性能对比

| 模型 | MMBench | OCRBench | MME | 中文 |
|------|:---:|:---:|:---:|:---:|
| GPT-4o | 83.4 | — | — | ⭐⭐⭐⭐ |
| Qwen2-VL-72B | 82.3 | 89.5 | — | ⭐⭐⭐⭐⭐ |
| LLaVA-1.6-34B | 80.2 | — | 2010 | ⭐⭐⭐ |
| GLM-4V | 78.5 | — | — | ⭐⭐⭐⭐ |
| InternVL2-8B | 79.2 | — | — | ⭐⭐⭐ |

## VLM 应用场景

```text
后端开发场景：
  ✅ 代码截图 → 提取代码 + 找 Bug
  ✅ 架构图 → 分析瓶颈 + 改进建议
  ✅ UI 截图 → 生成前端代码
  ✅ 错误堆栈截图 → 分析根因
  ✅ 数据库 ER 图 → 生成建表 SQL
```
