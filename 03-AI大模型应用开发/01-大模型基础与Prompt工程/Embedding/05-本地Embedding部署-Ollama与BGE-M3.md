# 05 - 本地 Embedding 部署：Ollama 与 BGE-M3

> 🎯 本地 Embedding 零成本、零延迟、零数据泄露风险 — Ollama 一行命令启动 BGE-M3，配合 LangChain 实现与云端 API 无缝切换

---

## 目录

1. [为什么需要本地 Embedding](#1-为什么需要本地-embedding)
2. [Ollama 部署 Embedding 模型](#2-ollama-部署-embedding-模型)
3. [BGE-M3 深度使用](#3-bge-m3-深度使用)
4. [LangChain 集成](#4-langchain-集成)
5. [本地模型性能调优](#5-本地模型性能调优)
6. [云端 API vs 本地部署抉择](#6-云端-api-vs-本地部署抉择)

---

## 1. 为什么需要本地 Embedding

```text
云端 API Embedding 的局限：
  ❌ 数据必须上传 → 隐私/合规风险
  ❌ 网络延迟 → 每条 50-200ms
  ❌ 按量计费 → 大规模场景费用可观
  ❌ API 不可用 → 整个 RAG 瘫痪

本地 Embedding 的优势：
  ✅ 完全离线 → 数据不外泄
  ✅ 零延迟 → 本地推理微秒级
  ✅ 零成本 → 一次性硬件投入
  ✅ 可控 → 模型版本、推理参数完全自主
```

---

## 2. Ollama 部署 Embedding 模型

### 2.1 安装与启动

```bash
# 安装 Ollama
curl -fsSL https://ollama.com/install.sh | sh

# 拉取 Embedding 模型
ollama pull bge-m3       # 智源 BGE-M3 (推荐)
ollama pull nomic-embed-text  # Nomic Embed (英文)
ollama pull mxbai-embed-large # MXBAI (英文)

# 验证
ollama list
# NAME              ID              SIZE      MODIFIED
# bge-m3:latest     7907646...      1.2 GB    2 days ago
```

### 2.2 API 调用

```python
import requests

def ollama_embed(text: str, model="bge-m3"):
    """调用 Ollama Embedding API"""
    resp = requests.post(
        "http://localhost:11434/api/embeddings",
        json={"model": model, "prompt": text}
    )
    return resp.json()["embedding"]

# 批量模式
def ollama_embed_batch(texts: list[str], model="bge-m3"):
    """批量处理 — 注意 Ollama 不支持原生 batch，需逐条调用"""
    return [ollama_embed(t, model) for t in texts]

# 使用
vec = ollama_embed("Spring Boot 自动配置原理")
print(f"维度: {len(vec)}")  # 1024
```

### 2.3 使用 OpenAI 兼容接口

```python
# Ollama 提供 OpenAI 兼容的 /v1/embeddings 接口
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:11434/v1",  # Ollama 本地地址
    api_key="ollama"                        # Ollama 不需要真实 key
)

resp = client.embeddings.create(
    model="bge-m3",
    input=["Hello World", "你好世界"]
)

for d in resp.data:
    print(f"维度: {len(d.embedding)}")  # 1024
```

---

## 3. BGE-M3 深度使用

### 3.1 Dense + Sparse 双模式

```python
import requests

# BGE-M3 特有：同时返回 Dense 和 Sparse 向量
# 注意：Ollama 默认只返回 Dense，需要自定义 Modelfile

# 使用 HuggingFace 直接加载以获得完整功能
from transformers import AutoModel, AutoTokenizer
import torch

model = AutoModel.from_pretrained("BAAI/bge-m3")
tokenizer = AutoTokenizer.from_pretrained("BAAI/bge-m3")

def bge_m3_full(text: str):
    """BGE-M3 完整输出：Dense + Sparse"""
    inputs = tokenizer(text, return_tensors="pt",
                       max_length=8192, truncation=True)
    
    with torch.no_grad():
        outputs = model(**inputs)
    
    # Dense 向量 (1024维)
    dense = outputs.last_hidden_state[:, 0, :].squeeze().numpy()
    
    # Sparse 向量 (词汇权重)
    sparse = outputs.sparse_embedding  # 如果有的话
    
    return {"dense": dense, "sparse": sparse}
```

### 3.2 Dense 与 Sparse 的使用场景

| 向量类型 | 特点 | 适用场景 |
|----------|------|----------|
| **Dense** | 1024 维浮点数，语义匹配 | 语义相近但字面不同（"轿车"→"汽车"） |
| **Sparse** | 词权重字典，关键词匹配 | 专有名词、精确匹配（"Spring Boot 3.2"） |
| **混合** | Dense + Sparse 组合 | **推荐！** 兼顾语义和精确 |

---

## 4. LangChain 集成

### 4.1 统一抽象

```python
from langchain_community.embeddings import OllamaEmbeddings

# LangChain 封装 Ollama Embedding
embeddings = OllamaEmbeddings(
    model="bge-m3",
    base_url="http://localhost:11434"
)

# 单条
vec = embeddings.embed_query("什么是 Java 多态")
print(f"维度: {len(vec)}")  # 1024

# 批量
docs = ["Java 多态指...", "Spring IoC 是...", "MySQL 索引优化..."]
vecs = embeddings.embed_documents(docs)
print(f"批量: {len(vecs)} 个向量，每个 {len(vecs[0])} 维")
```

### 4.2 云端/本地无缝切换

```python
from langchain_openai import OpenAIEmbeddings
from langchain_community.embeddings import OllamaEmbeddings

class EmbeddingFactory:
    """根据配置创建 Embedding 实例"""
    
    @staticmethod
    def create(provider="ollama", **kwargs):
        if provider == "ollama":
            return OllamaEmbeddings(
                model=kwargs.get("model", "bge-m3"),
                base_url=kwargs.get("base_url", "http://localhost:11434")
            )
        elif provider == "openai":
            return OpenAIEmbeddings(
                model=kwargs.get("model", "text-embedding-3-small"),
                dimensions=kwargs.get("dimensions", 512)
            )
        else:
            raise ValueError(f"Unknown provider: {provider}")

# 切换只需改一行配置
# embedding = EmbeddingFactory.create("ollama")      # 本地
embedding = EmbeddingFactory.create("openai")       # 云端
```

---

## 5. 本地模型性能调优

### 5.1 硬件需求

| 模型 | 大小 | 内存/显存 | CPU 推理速度 |
|------|:---:|------|:---:|
| bge-m3 | 1.2GB | ~2GB | ~50 条/秒 |
| m3e-base | 440MB | ~1GB | ~100 条/秒 |
| bge-large-en | 1.3GB | ~2GB | ~40 条/秒 |

### 5.2 推理加速

```python
import torch

# ① 使用 GPU 加速
model = AutoModel.from_pretrained(
    "BAAI/bge-m3",
    device_map="cuda",             # 放到 GPU
    torch_dtype=torch.float16      # FP16 加速
)

# ② 批处理（本地模型支持真正的 batch）
def batch_encode(model, tokenizer, texts, batch_size=64):
    """GPU 批处理大幅提升吞吐"""
    all_embeddings = []
    for i in range(0, len(texts), batch_size):
        batch = texts[i:i+batch_size]
        inputs = tokenizer(batch, return_tensors="pt",
                           padding=True, truncation=True,
                           max_length=512).to("cuda")
        with torch.no_grad():
            outputs = model(**inputs)
            embs = outputs.last_hidden_state[:, 0, :].cpu().numpy()
        all_embeddings.append(embs)
    return np.concatenate(all_embeddings)
```

---

## 6. 云端 API vs 本地部署抉择

| 维度 | 云端 API (OpenAI) | 本地部署 (Ollama+BGE-M3) |
|------|:---:|:---:|
| **精度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **成本** | 按量 $0.02/1M | 免费（硬件除外） |
| **延迟** | 50-200ms | <10ms |
| **隐私** | ❌ 数据上传 | ✅ 完全本地 |
| **运维** | 零 | 需维护模型服务 |
| **批量吞吐** | 受 Rate Limit | 取决于硬件 |
| **模型选择** | 仅 OpenAI 系列 | 任意 HuggingFace 模型 |
| **适用** | 原型/中小规模 | 隐私敏感/大规模 |

```text
推荐策略：
  ✅ 开发/测试 → OpenAI API（快速验证）
  ✅ 生产/隐私敏感 → Ollama 本地部署
  ✅ 高精度需求 → text-embedding-3-large
  ✅ 大规模 → 本地 GPU 批处理
```

---

## 核心要点回顾

- Ollama 一行命令 `ollama pull bge-m3` 即可本地部署 Embedding 模型
- BGE-M3 支持 Dense+Sparse 混合检索，精度最高
- LangChain 封装让云端/本地无缝切换
- GPU 批处理可大幅提升本地推理吞吐
- 隐私敏感场景务必本地部署，数据绝不能上传到第三方 API
