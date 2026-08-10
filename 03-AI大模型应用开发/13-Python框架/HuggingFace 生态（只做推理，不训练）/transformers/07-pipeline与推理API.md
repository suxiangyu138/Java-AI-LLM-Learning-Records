# 07-pipeline与推理API
> 定位：pipeline 是 transformers 的任务级推理入口——一个函数跑通「加载→分词→推理→后处理」全流程；与 Auto 家族的分工是「快速验证 vs 精细控制」。

## 📚 目录
1. [pipeline 是什么](#1-pipeline-是什么)
2. [任务家族全景](#2-任务家族全景)
3. [三参数与返回结构](#3-三参数与返回结构)
4. [text-generation 详解](#4-text-generation-详解)
5. [与 Auto 家族的选型](#5-与-auto-家族的选型)
6. [多模态与扩展](#6-多模态与扩展)
7. [五个坑](#7-五个坑)
8. [练习](#8-练习)

## 1. pipeline 是什么

pipeline 把「加载模型 → 加载分词器/处理器 → 预处理 → 前向推理 → 后处理」封装成一个可调用对象。以文本分类为例，你只需要一句话，它内部完成 tokenize、模型前向、softmax、标签映射：

```python
from transformers import pipeline
classifier = pipeline("text-classification")     # 默认模型（distilbert-base）
classifier("今天天气真好")                        # [{'label': 'POSITIVE', 'score': 0.99}]
```

三个价值：**零配置**（task 即一切，默认模型自动下载）；**任务语义清晰**（返回 label/score 而不是张量）；**跨任务一致**（问答、摘要、图像分类同一个调用姿势）。它是「验证模型能用、快速跑通流程」的第一选择——也是 02 篇快速开始的入口。

## 2. 任务家族全景

| 任务名 | 做什么 | 典型模型 |
|--------|--------|---------|
| `text-generation` | 文本续写/对话 | Qwen/Llama 系 |
| `text-classification` | 情感/主题分类 | bert-base 系 |
| `token-classification` | 命名实体/词性标注 | bert-ner |
| `question-answering` | 抽取式问答（给文章） | bert-large-qa |
| `summarization` | 摘要 | pegasus/bart |
| `translation` | 翻译 | mbart/m2m100 |
| `feature-extraction` | 向量嵌入（RAG 用） | bge/e5 系 |
| `fill-mask` | 完形填空 | bert |
| `zero-shot-classification` | 零样本分类 | bart 系 |
| `image-classification` | 图像分类 | vit/convnext |
| `image-to-text` | 图像理解（VLM） | qwen-vl/llava |

**feature-extraction 是 RAG 场景的关键**：它输出句子向量，直接对接 [[../../向量%20&%20文档处理|向量库]]（milvus/chroma）。zero-shot 分类用自然语言标签替代训练——没有标注数据时的分类神器。

## 3. 三参数与返回结构

```python
pipe = pipeline(
    task="text-generation",            # 任务名（必填）
    model="Qwen/Qwen2.5-0.5B-Instruct",# 模型 ID（可选，缺省用默认）
    device=0,                          # 0=GPU，-1=CPU
    torch_dtype=torch.bfloat16,        # 精度（传 model 时可用）
    model_kwargs={"quantization_config": quant_config},  # 透传加载参数
)
result = pipe("你好", max_new_tokens=50)
# [{'generated_text': '你好！有什么可以帮您？'}]
```

返回结构随任务而变：生成返回 `generated_text` 列表、分类返回 `label`/`score`、问答返回 `answer`/`start`/`end`。**生成的返回包含完整文本**（prompt + 生成部分，`return_full_text=False` 可只取生成段）。`model_kwargs` 是 v5 的透传通道：量化、device_map、attn_implementation 都能从 pipeline 传入——pipeline 也能玩量化，不是只能 Auto 家族。

## 4. text-generation 详解

**pipeline 内部发生了什么**（理解黑盒的钥匙）：调用 `pipe(text)` 时，内部依次执行——`tokenizer(text, return_tensors="pt", padding=..., truncation=...)` 构造输入 → `model.generate(**inputs, **生成参数)` 自回归生成 → `tokenizer.batch_decode(..., skip_special_tokens=True)` 解码 → 组装返回结构（`generated_text`/`label`/`score`）。**它与你手写的 Auto 家族三段式是同一套流程**——pipeline 是 Auto 家族的标准封装，不是另一套机制。这意味着 pipeline 的 bug 排查可以下钻到 Auto 家族：pipeline 报错时，用 Auto 家族重写一遍就能定位「是加载、输入、还是生成环节的问题」。

对话场景的标准配置（pipeline 与 Auto 家族参数互通）：

```python
pipe = pipeline("text-generation", model=model_id, device=0,
                model_kwargs={"torch_dtype": torch.bfloat16})

out = pipe(
    "用户消息",                 # 或 messages 列表（5.x 支持 chat 消息直传）
    max_new_tokens=200,
    do_sample=True, temperature=0.7, top_p=0.9,
    return_full_text=False,     # 只取新生成部分
    batch_size=4,               # 批量推理（多请求复用，吞吐关键）
)
```

要点：**批量推理用 `batch_size`**（多个输入同时进模型，吞吐数倍，pipeline 自动 padding——注意 04 篇的 padding_side 左对齐）；**对话直传 messages**（5.x 起 `pipe(messages, ...)` 内部自动走 chat template，无需手动 apply）；**pipeline 不支持流式 streamer 参数**（流式必须 Auto 家族，05 篇线程 + TextIteratorStreamer）——这是选型时的重要边界。

## 5. 与 Auto 家族的选型

| 维度 | pipeline | Auto 家族 |
|------|----------|-----------|
| 上手 | 一句话跑通 | 三段式样板 |
| 控制 | 有限（生成参数可传，流式不行） | 全部（KV 操作、cache、钩子） |
| 调试 | 黑盒 | 每步可见 |
| 生产 | 简单任务够用 | 复杂/定制场景必需 |
| 性能 | 自带批量 | 自管批量（等价） |

选型决策：**验证/演示/标准任务 → pipeline**；**流式输出、自定义生成循环、多模态拼接、性能调优、故障排查 → Auto 家族**。一个项目里两者可以共存：调研期 pipeline 快跑，上线期 Auto 家族精控。pipeline 的隐藏成本是「黑盒」——出问题时你不知道卡在加载还是推理，Auto 家族每步可打印。

**pipeline 的设备与精度管理**：`device` 参数在 5.x 支持 `"auto"`（自动选 GPU）与整数索引（0/1/-1）；精度与量化经 `model_kwargs` 透传（`{"torch_dtype": torch.bfloat16, "quantization_config": ...}`）——pipeline 同样能玩 06 篇的全部手段，只是语法绕一层。**多模型切换**：同一个 pipeline 对象换 `pipe.model = AutoModelForCausalLM.from_pretrained(...)` 可行但不推荐（切换成本与状态残留难控），多模型场景建多个 pipeline 实例更清晰——每个实例持有自己的模型与缓存，互不污染。

## 6. 多模态与扩展

pipeline 家族覆盖多模态：`image-to-text`（图像理解）、`image-classification`、`audio-classification`、`automatic-speech-recognition`（语音转文字）。多模态 pipeline 的输入是 PIL 图像/音频路径，内部自动用对应 processor（分词器 + 图像处理器的组合）预处理：

```python
pipe = pipeline("image-to-text", model="Qwen/Qwen2.5-VL-3B-Instruct")
pipe("图片路径或PIL对象", max_new_tokens=100)   # [{'generated_text': '描述...'}]
```

**多模态模型与纯文本模型的加载差异**：多模态 pipeline 内部走 `AutoProcessor`——它是「分词器 + 图像/音频处理器」的组合体，`from_pretrained` 加载时需要额外的图像处理配置（尺寸、归一化参数）；输入不再是纯文本，而是「文本 + 图像」的复合结构。显存按 1.5 倍估算（视觉编码器 0.5-2GB 额外占用）；多模态生成同样支持 chat template（消息里带 `image` 字段）。踩坑重点：**图像输入格式必须匹配模型训练格式**（分辨率、通道顺序、是否缩放）——processor 自动处理，但手写预处理时最容易在这里出错。

## 7. 五个坑

**坑一：pipeline 里传 streamer 参数**。不支持流式——流式需求回 Auto 家族。

**坑二：忘 `return_full_text=False`**。输出重复整段 prompt——文本越长浪费越多。

**坑三：批量推理默认右对齐**。pipeline 批量时 padding 对齐方向由分词器决定——LLM 先设 `padding_side="left"`（04 篇）。

**坑四：`model` 与 `model_kwargs` 混传冲突**。传了已加载模型对象就别再传加载参数——二选一，传 ID + model_kwargs 或传模型对象。

**坑五：任务名与模型不匹配**。`text-generation` 加载分类模型报错或行为异常——任务名与模型族必须配套，看模型卡的 pipeline tag。

**坑六：pipeline 对象反复重建**。`pipeline()` 每次调用都完整加载模型（首次下载、之后走缓存但仍是完整加载）——循环里建 pipeline 等于循环加载模型，慢且浪费显存；**pipeline 建一次全局复用**（与 03 篇模型常驻同一纪律）。

**坑七：默认模型不设防**。`pipeline("text-generation")` 不传模型会用默认小模型（gpt2 级）——验证语义可以，业务必须显式指定模型，否则行为随默认值漂移。

## 8. 练习

1. pipeline 的「三个价值」是什么？
2. feature-extraction 在 RAG 里的角色？
3. pipeline 与 Auto 家族的选型边界（尤其流式场景）？
4. `return_full_text=False` 的作用？
5. 多模态模型与纯文本模型在加载上的差异？

> 🎯 **核心要点**：pipeline = 任务级零配置推理，验证与标准任务首选；生成注意 return_full_text 与 batch_size；流式/精细控制回 Auto 家族；model_kwargs 透传量化与精度；多模态走 AutoProcessor。

---

**下一模块**：[08-推理优化](08-推理优化.md)｜**返回总览**：[00-transformers总览](00-transformers总览.md)
