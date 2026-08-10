# 03-模型加载与Auto类
> 定位：`from_pretrained` 是 transformers 的门面——模型类选型、加载参数、设备分发、缓存治理四个维度决定「模型能不能加载、加载到哪、花多少显存」。

## 📚 目录
1. [Auto 类家族选型](#1-auto-类家族选型)
2. [from_pretrained 参数全解](#2-from_pretrained-参数全解)
3. [device_map：设备分发](#3-device_map设备分发)
4. [加载的幕后](#4-加载的幕后)
5. [缓存与离线](#5-缓存与离线)
6. [五个坑](#6-五个坑)
7. [练习](#7-练习)

## 1. Auto 类家族选型

Auto 类按「任务头」区分——同一个基座模型，加载成不同子类获得不同能力。选错类是最常见的「模型加载成功但用不了」事故：

| Auto 子类 | 任务 | 输出形态 |
|-----------|------|---------|
| `AutoModel` | 基座（无头） | 隐藏状态，做嵌入/特征 |
| `AutoModelForCausalLM` | 因果语言模型 | 续写/对话，有 generate() |
| `AutoModelForSeq2SeqLM` | 编码器-解码器 | 翻译/摘要，有 generate() |
| `AutoModelForSequenceClassification` | 分类 | logits，情感/评分 |
| `AutoModelForTokenClassification` | 序列标注 | 每 token 标签，NER |
| `AutoModelForQuestionAnswering` | 抽取式问答 | start/end 位置 |
| `AutoModelForImageClassification` | 图像分类 | logits |

**对话与文本生成一律 `AutoModelForCausalLM`**——它带语言建模头与完整的 generate 支持。`AutoModel` 加载同一模型只给基座输出，没有头，`generate` 直接报错。判断口诀：**要生成——CausalLM/Seq2SeqLM；要判别——Classification 系列；只要向量——AutoModel**。

## 2. from_pretrained 参数全解

```python
model = AutoModelForCausalLM.from_pretrained(
    model_id,                              # 仓库 ID 或本地路径
    revision="main",                       # 分支/commit/tag
    torch_dtype=torch.bfloat16,            # 加载精度：显存减半的关键
    device_map="auto",                     # 设备自动分发（需 accelerate）
    attn_implementation="sdpa",            # 注意力后端：sdpa/flash_attention_2
    quantization_config=quant_config,      # 量化配置（06 篇）
    trust_remote_code=False,               # 远程代码：只对可信仓库开
    low_cpu_mem_usage=True,                # 大模型 CPU 内存治理
    local_files_only=False,                # 只用本地缓存，不联网
    cache_dir=None,                        # 缓存目录覆盖
)
```

按重要性排序的决策点：**torch_dtype**——`float32`（4 字节/参数）→ `float16`/`bfloat16`（2 字节）→ 量化（1 字节以下），内存线性缩放，7B 模型 fp32 28GB 起步，bf16 14GB；**device_map**——单卡/多卡/offload 的分配策略（下节）；**attn_implementation**——`sdpa`（PyTorch 原生，默认推荐）或 `flash_attention_2`（长序列更快，08 篇）；**revision**——要复现实验或避开有问题的分支时指定 commit。

## 3. device_map：设备分发

`device_map="auto"` 是 accelerate 提供的设备自动分配：把模型各层按显存容量铺到可用设备上——**优先 GPU，不够则分层放多卡，再不够放 CPU，最后落磁盘**。这是大模型加载的救命参数：

- 单卡 24GB 跑 7B bf16（14GB 权重 + KV cache）——auto 全部上 GPU
- 单卡 16GB 跑 7B——部分层 offload 到 CPU，推理变慢但能跑
- 双卡 24GB×2 跑 70B 4bit（约 40GB）——按层分摊两卡

```python
model = AutoModelForCausalLM.from_pretrained(
    model_id, torch_dtype=torch.bfloat16, device_map="auto")
print(model.hf_device_map)   # 看每层去了哪：{'model.embed_tokens': 0, 'model.layers.0': 0, ..., 'model.layers.31': 'cpu'}
```

两条纪律：**CPU/磁盘 offload 会显著拖慢推理**（每层跨设备传输），只作为「显存不足时先跑通」的手段，生产用量化代替 offload（06 篇）；**device_map="auto" 时不要把模型再手动 .to("cuda")**——auto 已经管好设备，二次移动破坏分配。显存极小场景还有 `max_memory` 参数手动指定每设备上限。

## 4. 加载的幕后

`from_pretrained` 不是简单读文件，而是五步流水线（5.x 动态化后有所简化）：读取 config.json 确定架构 → 实例化空模型骨架 → 按 safetensors 分片清单下载/读取权重 → 逐张量加载并做**权重复合**（5.x WeightConverter：把分片按目标格式融合，如 QKV 合并、MoE 专家重排——加载即优化，无需手动 merge）→ 挂载分词器与配置。理解这条流水线，三个现象都有了解释：**首次加载慢**（下载+融合）、**HF 缓存重复占用**（同模型多版本并存）、**`OOM` 发生在加载而非推理**（融合过程峰值内存——`low_cpu_mem_usage=True` 降低峰值）。

**加载性能与时机**：加载是「按需触发」的——`from_pretrained` 执行一次才加载一次，之后模型常驻内存。三个工程结论：**服务端在启动时加载，不要请求时加载**（启动 `on_startup` 里 from_pretrained，请求时直接 generate——首次请求不背加载的锅，这也是 09 篇 serve 的 `/load_model` 预热的原理）；**多轮推理复用同一模型实例**（`generate` 是纯函数式调用，同一模型并发/串行调用都安全，无需每次新建）；**模型切换是重量级操作**（换模型 = 卸载旧 + 下载/读盘 + 加载新，几十秒级——多模型服务要按需加载 + 空闲卸载，即 serve 的 `--model-timeout`）。理解「加载是一次性成本、推理是重复成本」，服务化架构就顺了：加载挪到后台、模型实例常驻、切换走调度。

**加载的并发安全**：同一模型实例被多个请求同时 `generate` 是安全的（generate 内部无共享可变状态，batch 维度由调用方控制）；但**同一进程里并发 `from_pretrained` 两个不同模型**可能触发下载锁竞争与显存瞬时双峰——服务端多模型场景按序加载（或预下载 + 串行加载）。并发调用 `generate` 时注意：显存预算按「单请求 KV × 并发数」算（06 篇公式），不是单请求预算——并发 8 的 7B 服务，KV 部分按 8 倍算，这是「能加载却推理 OOM」的常见盲区。

## 5. 缓存与离线

**缓存目录**：默认 `~/.cache/huggingface/hub`（Windows 为 `C:\Users\<名>\.cache\huggingface`），由 `HF_HOME` 环境变量整体迁移。模型按「仓库 ID + revision」哈希分目录，safetensors 分片以 blob 存储 + symlink 引用——**不要手动删缓存目录内的散文件**，要用 `huggingface-cli scan-cache`/`delete-cache` 管理。

**离线三件套**：

```python
# 1. 有网机器上全量拉取
from huggingface_hub import snapshot_download
snapshot_download("Qwen/Qwen2.5-7B-Instruct")
# 2. 拷贝到离线机（保留目录结构）
# 3. 离线机设环境变量 + local_files_only
os.environ["HF_HUB_OFFLINE"] = "1"
model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-7B-Instruct", local_files_only=True)
```

离线场景必须 `local_files_only=True`，否则 `from_pretrained` 会尝试联网并卡死等待。`HF_HUB_OFFLINE=1` 是全局开关，`local_files_only` 是单次调用开关——生产代码两者都用，双保险。

## 6. 五个坑

**坑一：选错 Auto 类**。`AutoModel` 加载对话模型没有 generate——任务头决定能力，先查模型卡的「pipeline tag」。

**坑二：fp32 硬扛 7B**。28GB 权重 + KV 缓存必然 OOM——`torch_dtype=bf16` 是第一步，量化是第二步。

**坑三：device_map 与 .to() 混用**。auto 分发后手动 `.to("cuda")` 破坏分配，报设备不匹配错误——选一种姿势走到底。

**坑四：离线环境忘设 local_files_only**。联网卡死数分钟——离线部署环境变量 + 参数双保险。

**坑五：`trust_remote_code=True` 无脑开**。社区仓库的远程代码在加载时执行——只对可信来源开，先审计再信任（官方模型仓绝大多数不需要）。

**坑六：加载版本不锁定（revision）**。仓库 main 分支持续更新，昨天能加载今天报错——复现实验用 `revision` 锁 commit 或 tag；生产快照仓库或锁定 revision。

**坑七：多卡 device_map 依赖均衡配置**。`max_memory` 不指定时 auto 按显存比例分配，卡规格不一（24G 混 48G）时小卡可能过载——显存不均环境显式传 `max_memory={0: "20GiB", 1: "40GiB"}`。

## 7. 练习

1. `AutoModel` 与 `AutoModelForCausalLM` 加载同一模型，能力差异是什么？
2. 7B 模型在 fp32、bf16、4bit 下各需多少权重显存？
3. `device_map="auto"` 在显存不足时依次做什么？
4. 权重复合（WeightConverter）解决了什么问题？
5. 离线部署三件套的完整命令与参数是什么？

> 🎯 **核心要点**：任务头决定 Auto 类选型，生成必选 CausalLM；torch_dtype + device_map + attn_implementation 三参数决定「多快多省」；加载是五步流水线，峰值内存要治理；离线 = snapshot_download + HF_HUB_OFFLINE + local_files_only。

---

**下一模块**：[04-分词器与输入构造](04-分词器与输入构造.md)｜**返回总览**：[00-transformers总览](00-transformers总览.md)
