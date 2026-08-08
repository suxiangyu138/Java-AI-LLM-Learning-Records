# 07 Datasets 与数据流水线
> 数据工程的官方管道：加载、映射、过滤、流式与缓存——微调的数据底座

## 📚 目录
1. [Datasets 核心：Arrow 格式与内存映射](#1-datasets-核心arrow-格式与内存映射)
2. [数据加载：load_dataset 全形态](#2-数据加载load_dataset-全形态)
3. [map：并行转换的流水线核心](#3-map并行转换的流水线核心)
4. [过滤 / 去重 / 采样](#4-过滤--去重--采样)
5. [流式加载：超大数据集](#5-流式加载超大数据集)
6. [与 Trainer 集成](#6-与-trainer-集成)
7. [缓存管理](#7-缓存管理)
8. [常见坑](#8-常见坑)
9. [核心要点](#9-核心要点)

---

## 1. Datasets 核心：Arrow 格式与内存映射

```python
from datasets import load_dataset, Dataset, DatasetDict

# Datasets 底层 = Apache Arrow 格式
#  → 列式存储、零拷贝内存映射、并行读取
#  → 100GB 数据集不占满内存（按需映射到磁盘）

ds = load_dataset("json", data_files="train.jsonl")
print(ds)                  # DatasetDict({'train': Dataset({features: [...], num_rows: 10000})})
print(ds["train"][0])      # 按索引访问
print(ds["train"]["text"]) # 按列访问（快速）
```

| 特性 | 说明 | 价值 |
|------|------|------|
| Arrow 列式 | 按列读取零拷贝 | 大数据集内存友好 |
| 内存映射 | 数据在磁盘，按需读页 | 100GB 也能玩 |
| 并行 | 多进程处理 | map 加速 |
| 缓存 | 处理结果自动缓存 | 重复实验秒回 |

> 🎯 **核心要点**：Datasets 是为"**大数据集也能在内存玩**"设计的——10 万条微调数据轻松处理；处理结果自动缓存，改一行代码重跑也不全量重算。

## 2. 数据加载：load_dataset 全形态

```python
# ① 本地文件
ds = load_dataset("json", data_files={"train": "train.jsonl", "val": "val.jsonl"})
ds = load_dataset("csv", data_files="data.csv")
ds = load_dataset("parquet", data_files="data.parquet")     # 快 + 省空间

# ② HuggingFace Hub 数据集
ds = load_dataset("c-s-ale/medquad")                       # 公开数据集
ds = load_dataset("my-org/my-dataset", split="train")      # 自己上传的

# ③ 流式（大文件不落盘）→ §5
ds = load_dataset("json", data_files="big.jsonl", streaming=True)

# ④ 从列表直接建（小数据/测试）
data = Dataset.from_list([{"text": "a"}, {"text": "b"}])
```

| 加载源 | 用法 | 适用 |
|--------|------|------|
| JSONL | `load_dataset("json", ...)` | ⭐ 微调数据标准 |
| CSV | `load_dataset("csv", ...)` | 表格数据 |
| Parquet | 快/压缩 | 大文件推荐 |
| HF Hub | `load_dataset("owner/name")` | 公开数据集 |
| 列表 | `Dataset.from_list` | 小数据/原型 |

> 💡 上传自己的数据集到 Hub（01 章 §7 方式），团队共享数据仓库——与模型一样走 `owner/name` 拉取，版本可追溯。

## 3. map：并行转换的流水线核心

```python
from datasets import load_dataset
from transformers import AutoTokenizer

ds = load_dataset("json", data_files="train.jsonl")
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-7B-Instruct")

# 逐条转换：格式化 + 分词（微调数据准备的标准姿势）
def format_example(batch):                       # 接收一批（map 自动批处理）
    return {
        "input_ids": tokenizer.apply_chat_template(
            batch["messages"], tokenize=True, add_generation_prompt=False
        ).input_ids,
    }

ds = ds.map(
    format_example,
    batched=True,          # ⭐ 批处理：比逐条快 10 倍+
    num_proc=8,            # 多进程并行
    remove_columns=["messages"],   # 移除原列（省空间）
)
print(ds["train"].features)   # {'input_ids': Sequence(feature=Value(dtype='int32'))}
```

| map 参数 | 作用 | 建议 |
|---------|------|------|
| `batched=True` | 批处理（整批转换） | ⭐ 必须（性能差 10 倍） |
| `num_proc` | 多进程数 | CPU 核数 |
| `remove_columns` | 删原列 | 省内存/缓存 |
| `load_from_cache_file=False` | 跳过缓存 | 函数变化时强制重跑 |
| `desc` | 进度条说明 | 长任务友好 |

> 🎯 **核心要点**：**map = 数据流水线的"transform 站"**——格式化、分词、清洗全在这里做。生产流水线：`加载 → map(清洗) → map(格式化) → map(分词) → filter → 训练`。每个 map 结果自动缓存，改哪站只重算哪站。

## 4. 过滤 / 去重 / 采样

```python
# 过滤：长度/质量/语言
ds = ds.filter(lambda x: 100 < len(x["input_ids"]) < 2048, num_proc=8)

# 去重（微调数据必备：重复样本导致过拟合）
from datasets import Dataset

def dedup(ds, key_fn):
    seen, keep = set(), []
    for ex in ds:
        key = key_fn(ex)
        if key not in seen:
            seen.add(key)
            keep.append(ex)
    return Dataset.from_list(keep)

# 或按列去重（先排序保证稳定）
ds = ds.sort("text").deduplicate(column="text")

# 采样/切分
train_val = ds.train_test_split(test_size=0.05, seed=42)   # 5% 验证集
sample = ds.shuffle(seed=42).select(range(1000))           # 快速冒烟数据
```

| 操作 | API | 场景 |
|------|-----|------|
| 过滤 | `filter(lambda)` | 长度/质量/语言筛选 |
| 去重 | `deduplicate(column=...)` | ⭐ 微调必备（重复=过拟合） |
| 切分 | `train_test_split` | 训练/验证 |
| 采样 | `shuffle().select()` | 冒烟/调试 |
| 排序 | `sort(column)` | 稳定去重/长度排序 |

> ⚠️ 数据清洗三必做：**去重 + 长度过滤 + 样本抽查**。微调失败的第一大原因不是模型，是"训练集里有 30% 重复/垃圾样本"——清洗先行，训练后补。

## 5. 流式加载：超大数据集

```python
# streaming=True：不下载全量，边读边用（IterableDataset）
stream = load_dataset("json", data_files="huge.jsonl", streaming=True)
# 适用：数据 > 内存/磁盘预算；快速探索

for example in stream["train"]:      # 逐条迭代
    ...

# 流式 + 过滤 + 采样（迭代式操作）
from datasets import interleave_datasets

filtered = stream["train"].filter(lambda x: x["lang"] == "zh")
sampled = filtered.shuffle(seed=42, buffer_size=10000)   # 流式 shuffle（缓冲窗口）
```

| 加载方式 | 行为 | 适用 |
|---------|------|------|
| 默认 | 全量下载 + 内存映射 | 数据集 < 磁盘/内存预算 |
| **streaming** | 边读边用 | ⭐ 超大/远程数据/快速探索 |
| 缓存 | 处理结果落盘 | 重复实验 |

> 💡 流式的取舍：**探索用流式（快），训练用缓存全量（确定性 + 随机访问）**——训练器需要随机打乱/重复 epoch，流式迭代器做不到。流式验证数据质量 → 全量落地跑训练，是最佳流程。

## 6. 与 Trainer 集成

```python
from datasets import load_dataset
from transformers import (
    AutoTokenizer, AutoModelForCausalLM, Trainer, TrainingArguments,
    DataCollatorForLanguageModeling,
)

# 数据（含清洗与格式化）
ds = load_dataset("json", data_files="train.jsonl")
ds = ds.map(format_example, batched=True, num_proc=8)
ds = ds.filter(lambda x: len(x["input_ids"]) < 2048)
ds = ds.sort("text").deduplicate(column="text")
ds = ds.train_test_split(test_size=0.05, seed=42)

# 训练（05 章：Trainer 直接吃 Dataset）
trainer = Trainer(
    model=model,
    args=TrainingArguments(output_dir="./out", ...),
    train_dataset=ds["train"],
    eval_dataset=ds["val"],
    data_collator=DataCollatorForLanguageModeling(tokenizer=tokenizer, mlm=False),
)
trainer.train()
```

> 🎯 **核心要点**：**Trainer 直接吃 Dataset 对象**——数据流水线（本章）与训练（05/06 章）无缝衔接。生产流程：清洗 → 格式化 → 切分 → 缓存 → 训练，每一站可独立验证。

## 7. 缓存管理

```python
# 缓存位置与治理
# 默认：~/.cache/huggingface/datasets
# 环境变量：HF_DATASETS_CACHE（v5 统一归 HF_HOME 管）

# 查看缓存大小
du -sh ~/.cache/huggingface/datasets/*

# 程序内清理
from datasets import disable_caching        # 临时禁用缓存（调试）
# disable_caching()

# map 缓存冲突：函数改了但缓存没更新 → load_from_cache_file=False 强制重跑
ds = ds.map(fn, load_from_cache_file=False)
```

| 缓存策略 | 做法 |
|---------|------|
| 默认 | 自动缓存，改函数记得强制重跑 |
| 调试 | `disable_caching()` 或 load_from_cache_file=False |
| 团队 | 共享缓存目录（NAS） |
| 磁盘 | 定期清理旧缓存（大 map 结果动辄 GB） |

> ⚠️ **缓存"脏"是最隐蔽的坑**：改了 map 函数但忘强制重跑 → 训练用的还是旧数据。改函数 = 立刻 `load_from_cache_file=False`（或删缓存）。

## 8. 常见坑

| # | 坑 | 现象 | 解法 |
|:---:|-----|------|------|
| 1 | map 忘记 batched | 慢 10 倍 | batched=True |
| 2 | 缓存脏 | 改了函数没生效 | load_from_cache_file=False |
| 3 | 数据有重复 | 过拟合/训练不稳 | deduplicate |
| 4 | 长度超限 | 截断/浪费 | filter 长度范围 |
| 5 | 大文件内存炸 | OOM | streaming=True 或 Parquet |
| 6 | 中文编码 | 乱码 | utf-8 强制 |
| 7 | remove_columns 忘写 | 缓存巨大 | 转换后删原列 |
| 8 | 流式用于训练 | 打乱/重复 epoch 做不到 | 探索流式、训练全量 |

## 9. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | Arrow + 内存映射：100GB 数据集也玩得动 |
| 2 | 加载五形态：JSONL/CSV/Parquet/Hub/流式 |
| 3 | map 三件套：batched + num_proc + remove_columns |
| 4 | 清洗三必做：去重 + 长度过滤 + 抽查 |
| 5 | 探索用流式、训练用全量缓存 |
| 6 | Trainer 直接吃 Dataset，流水线无缝衔接 |
| 7 | 缓存纪律：改函数强制重跑、定期清理 |
| 8 | 数据是微调上限——清洗先行 |

---

**下一模块**：[08-Gradio 与 Spaces 应用](08-Gradio与Spaces应用.md) / **返回总览**：[00-HuggingFace知识体系总览](00-HuggingFace知识体系总览.md)

## 参考来源

- [Datasets 官方文档](https://huggingface.co/docs/datasets/index)
- [Datasets：map 指南](https://huggingface.co/docs/datasets/en/process)
- [Datasets：流式加载](https://huggingface.co/docs/datasets/en/stream)
- [HF 官方：数据处理教程](https://huggingface.co/docs/datasets/en/use_with_transformers)
