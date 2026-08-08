# 01 Hub 与模型生态
> AI 时代的 GitHub：仓库机制、模型卡、gated 权限、国内镜像与缓存治理

## 📚 目录
1. [Hub 全景：150 万+ 模型的分发中心](#1-hub-全景150-万-模型的分发中心)
2. [仓库机制：模型 / 数据集 / Spaces 三种资产](#2-仓库机制模型--数据集--spaces-三种资产)
3. [模型卡：模型的说明书](#3-模型卡模型的说明书)
4. [gated 模型与 token 认证](#4-gated-模型与-token-认证)
5. [国内下载：镜像站与加速方案](#5-国内下载镜像站与加速方案)
6. [缓存治理：HF_HOME 与磁盘管理](#6-缓存治理hf_home-与磁盘管理)
7. [模型上传与版本管理](#7-模型上传与版本管理)
8. [核心要点](#8-核心要点)

---

## 1. Hub 全景：150 万+ 模型的分发中心

**2026 年 Hub 规模**：

| 资产 | 规模 | 说明 |
|------|------|------|
| 模型 | **150 万+** | NLP/视觉/音频/多模态全覆盖 |
| 数据集 | 数十万 | 社区与组织共享 |
| Spaces | 数十万 | 可运行的应用 Demo |

**生态角色**（v5 的"互操作中枢"定位）：

```text
        ┌──────────────────────────────────┐
        │          HuggingFace Hub          │  模型/数据/应用的唯一权威来源
        └───────┬──────────┬────────┬───────┘
                │          │        │
        ┌───────▼───┐ ┌────▼────┐ ┌─▼────────┐
        │Transformers│ │PEFT    │ │vLLM 等   │
        │加载与微调   │ │LoRA 训练│ │推理引擎   │
        └───────────┘ └─────────┘ └──────────┘
```

> 🎯 **核心要点**：**Hub 是"来源"，其他库是"消费方"**——模型权重只在 Hub 存一份，Transformers/PEFT/vLLM/llama.cpp 都从同一个 `model_id` 拉取。这就是"AI 时代的 GitHub"的含义。

## 2. 仓库机制：模型 / 数据集 / Spaces 三种资产

```text
https://huggingface.co/{owner}/{repo-name}
                │         │
                │         └─ 仓库名（模型名）
                └─ 组织/用户名

三种仓库类型（repo_type）：
  model    → 权重 + 配置文件 + 分词器 + 模型卡
  dataset  → 数据文件 + 数据卡
  space    → 应用代码（Gradio/Streamlit）+ 环境配置
```

**模型仓库的标准文件布局**：

```text
Qwen/Qwen2.5-7B-Instruct/
├── config.json              # 架构配置（层数/维度/激活函数）
├── model.safetensors        # 权重（safetensors 安全格式）
├── tokenizer.json           # v5 统一分词配置
├── tokenizer_config.json    # 分词器行为配置
├── generation_config.json   # 生成配置（温度/长度等默认值）
├── modelcard.md             # 模型卡（README）
├── LICENSE                  # 许可证
└── README.md                # 使用说明
```

| 文件 | 作用 | 关键点 |
|------|------|--------|
| config.json | 架构参数 | AutoConfig 读取 |
| *.safetensors | 权重 | 安全格式（防 pickle 注入），替代 .bin |
| tokenizer.json | v5 统一分词 | 一个文件全搞定（02 章） |
| generation_config.json | 生成默认值 | pipeline 自动读取 |
| LICENSE | 使用许可 | 商用前必读（GitHub License 体系） |

> 💡 **safetensors 是安全底线**：`.bin`（pickle）权重可能包含恶意代码，2026 年 HF 已默认禁用 pickle 反序列化风险面（`SAFETENSORS_FAST_GPU` 环境优化）。下载模型后检查有没有 `.bin` 格式的老仓库——有则优先换 safetensors 版本。

## 3. 模型卡：模型的说明书

模型卡（Model Card）是仓库 README，**选型与评估的第一手资料**：

| 模型卡内容 | 选型价值 |
|-----------|---------|
| 基准指标（MMLU/GSM8K 等） | 能力对比 |
| 支持的任务列表 | 是否覆盖你的场景 |
| 输入格式要求（模板/特殊 token） | 正确调用 |
| 训练数据与许可 | 合规评估 |
| 已知局限 | 防坑预警 |
| 使用示例代码 | 快速上手 |

```text
选型流程（读模型卡四步）：
  ① 看基准：你的任务是否有对应榜单
  ② 看许可：商用/衍生/数据合规（GitHub License 体系）
  ③ 看生态：HF 下载量/日下载、vLLM/llama.cpp 支持度
  ④ 看输入要求：对话模板/图像分辨率/上下文长度
```

> 🎯 **核心要点**：**模型卡是"先读文档再调 API"的 AI 版**——跳过模型卡直接跑代码，等于不读文档调接口。生产选型第一步永远是读模型卡（+ 许可）。

## 4. gated 模型与 token 认证

**gated（门控）模型**：需同意许可条款 + 携带 token 才能下载（Llama、Gemma、Mistral 部分、Qwen 部分）：

```python
# ① 网页同意许可（首次）：模型主页点 "Agree and access repository"
# ② 生成 token：hf.co/settings/tokens → New token（Read 权限即可）
# ③ 登录（本地持久化）
huggingface-cli login              # 输入 token
# 或代码登录
from huggingface_hub import login
login(token="hf_xxx")              # 生产用环境变量更安全

# ④ 使用（登录后自动带 token）
from transformers import AutoModel
model = AutoModel.from_pretrained("meta-llama/Llama-3.3-70B-Instruct")
```

```bash
# 环境变量方式（生产推荐，避免 token 进代码）
export HF_TOKEN=hf_xxx
# 镜像站透传（hf-mirror 支持 gated 模型 token 透传）
```

> ⚠️ token 安全：**HF_TOKEN 走环境变量/密钥系统，绝不写进代码与镜像**（token 泄漏 = 别人能用你的账号拉 gated 模型/消费你的配额）。git 仓库里出现过 token 会触发 HF 的自动告警（类 GitHub 密钥扫描）。

## 5. 国内下载：镜像站与加速方案

**2026 实测数据**（70B 模型 140GB，千兆网络）：

| 方式 | 耗时 | 成功率 |
|------|:---:|:---:|
| huggingface.co 直连 | 4 小时+（多次中断） | <30% |
| **hf-mirror.com**（单连接） | 38 分钟 | 100% |
| **hfd.sh + aria2c -x16** | 22 分钟 | 100% |
| ModelScope（阿里，有该模型时） | 26 分钟 | 100% |
| 内网 OSS/R2 缓存 | 3 分钟 | 100% |

### 方法一：环境变量（首选，代码零改动）

```bash
# Linux/macOS
export HF_ENDPOINT=https://hf-mirror.com
echo 'export HF_ENDPOINT=https://hf-mirror.com' >> ~/.bashrc
# Windows
setx HF_ENDPOINT "https://hf-mirror.com"
```

```python
# ⚠️ 必须在 import 之前设置！
import os
os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"
from transformers import AutoModel, AutoTokenizer
```

### 方法二：命令行下载

```bash
huggingface-cli download Qwen/Qwen2.5-7B-Instruct \
    --local-dir ./models/qwen2.5-7b \
    --local-dir-use-symlinks=False
```

### 方法三：多线程加速（大模型必用）

```bash
wget https://hf-mirror.com/hfd/hfd.sh && chmod a+x hfd.sh
./hfd.sh Qwen/Qwen2.5-72B-Instruct -x 16 --local-dir ./models/qwen-72b
# -x 16：aria2c 16 并发；万兆可开到 32-64
```

> 🎯 **核心要点**：**国内开发者的第一课 = 配置镜像**。`HF_ENDPOINT` 一行环境变量解决 90% 下载问题；70B+ 大模型用 hfd.sh 多线程；团队场景必须建**内网缓存**（R2/MinIO/OSS），避免全员重复公网拉取。

## 6. 缓存治理：HF_HOME 与磁盘管理

**v5 起缓存变量统一**：`HF_HOME`（替代 `TRANSFORMERS_CACHE`）。

```bash
# 设置缓存位置（大模型动辄几十 GB，必须规划磁盘）
export HF_HOME=/data/hf              # 大容量盘
```

```text
缓存目录结构（v5）：
HF_HOME/
├── hub/                  # 模型/数据集/Spaces 缓存
│   └── models--Qwen--Qwen2.5-7B-Instruct/
│       ├── snapshots/<commit-hash>/   # 实际文件（只读）
│       └── blobs/                     # 内容寻址存储
├── token/                 # 登录凭证
└── cache/                 # 其他缓存
```

| 治理动作 | 命令/做法 |
|---------|----------|
| 查看缓存占用 | `du -sh $HF_HOME/hub/*` |
| 清理未引用快照 | `huggingface-cli scan-cache` / `delete-cache` |
| 控制并发下载 | `HF_HUB_DOWNLOAD_TIMEOUT`、xet 并发参数 |
| 本地目录优先 | `--local-dir` 直接落盘（跳过缓存层） |
| 团队共享 | 缓存目录放 NAS，多机只读挂载 |

> ⚠️ 磁盘杀手：**每个模型版本一份快照**，反复更新 = 磁盘膨胀。定期 `scan-cache` 清理；生产服务器给 HF_HOME 独立分区并监控占用（09 章运维清单）。

## 7. 模型上传与版本管理

```python
# 上传自己的模型/微调结果
from huggingface_hub import HfApi, create_repo

api = HfApi()
create_repo("my-org/my-finetuned-model", repo_type="model")   # 建仓
api.upload_folder(
    folder_path="./model_output/",            # 本地权重目录
    repo_id="my-org/my-finetuned-model",
    commit_message="初版微调结果",
)
```

```bash
# 或命令行上传
huggingface-cli upload my-org/my-model ./model_output/
```

| 版本管理能力 | 说明 |
|-------------|------|
| 提交（commit） | 每次上传一个 commit，可回滚 |
| 分支（branch） | main + 实验分支 |
| tag | 稳定版本标记（`v1.0`） |
| revision 参数 | `from_pretrained(..., revision="v1.0")` 精确拉取 |
| 私有仓库 | 上传时 `private=True` |

> 💡 微调产物的标准归宿：**push 到 Hub（私有或公开）**——比存网盘正规：可版本化、可协作、`from_pretrained` 一行加载、CI 可复用。

## 8. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | Hub = 150 万+ 模型的唯一权威来源，各库都是"消费方" |
| 2 | 三种仓库：model/dataset/space；safetensors 是安全格式底线 |
| 3 | 模型卡 = 选型说明书（基准/许可/输入要求四步读） |
| 4 | gated 模型：同意许可 + token；HF_TOKEN 走环境变量 |
| 5 | 国内下载：HF_ENDPOINT 环境变量（import 前设置）+ hfd.sh 多线程 |
| 6 | 缓存：HF_HOME 统一管理（v5），大模型独立分区 + scan-cache 清理 |
| 7 | 上传走 HF Hub：commit/分支/tag/revision，微调产物标准归宿 |
| 8 | 团队必建内网缓存，避免全员公网重复拉取 |

---

**下一模块**：[02-Transformers v5 核心机制](02-Transformersv5核心机制.md) / **返回总览**：[00-HuggingFace知识体系总览](00-HuggingFace知识体系总览.md)

## 参考来源

- [HuggingFace Hub 官方文档](https://huggingface.co/docs/hub/index)
- [hf-mirror.com 全量镜像站](https://hf-mirror.com/)
- [国内镜像加速实战（2026-05）](https://docs.alayanew.com/en/docs/tech-blog/2026-05-05-hf-mirror-acceleration)
- [HF 缓存管理官方文档](https://huggingface.co/docs/huggingface_hub/en/guides/manage-cache)
- [HuggingFace Hub API 文档](https://huggingface.co/docs/huggingface_hub/index)
