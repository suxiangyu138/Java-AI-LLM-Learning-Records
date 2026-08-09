# 05 Embedding 服务化与模型网关

> 检索链的两根"管道"：TEI 把 Embedding 变成独立 GPU 服务（可热切换模型），LiteLLM 统一路由所有模型调用（一个密钥管所有供应商）——部署细节决定线上稳定性。

## 📚 目录

1. [为什么要服务化 Embedding](#1-为什么要服务化-embedding)
2. [TEI 部署实践](#2-tei-部署实践)
3. [服务化 API 设计](#3-服务化-api-设计)
4. [LiteLLM 模型网关](#4-litellm-模型网关)
5. [维度一致性与参数匹配](#5-维度一致性与参数匹配)
6. [模型热切换](#6-模型热切换)
7. [面试高频问法](#7-面试高频问法)

## 1. 为什么要服务化 Embedding

### 进程内嵌入的问题

| 问题 | 说明 |
|---|---|
| 模型重复加载 | 编排服务多副本各加载一份（显存浪费） |
| 无法独立扩容 | 检索瓶颈被迫扩整个编排 |
| 无法热切换 | 换模型要重启服务 |
| 语言绑定 | Python 进程内，Java 服务调不了 |

### 服务化的收益

```
① 独立 GPU Pod：模型常驻，副本间共享
② 独立扩容：检索瓶颈只扩 Embedding
③ 热切换：环境变量换模型不重启
④ 语言无关：REST API 任意语言调用
```

## 2. TEI 部署实践

### TEI（Text Embeddings Inference）

HuggingFace 的 Embedding 推理服务，专为生产设计：

```bash
# Docker 启动（GPU）
docker run --gpus all -p 8080:8080 \
  ghcr.io/huggingface/text-embeddings-inference:latest \
  --model-id BAAI/bge-large-zh-v1.5 \
  --max-batch-tokens 16384 \
  --port 8080
```

### K8s 部署（GPU Pod）

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tei
  namespace: embeddings
spec:
  replicas: 2
  template:
    spec:
      containers:
        - name: tei
          image: ghcr.io/huggingface/text-embeddings-inference:1.5
          args: ["--model-id", "BAAI/bge-large-zh-v1.5", "--max-batch-tokens", "16384"]
          resources:
            limits:
              nvidia.com/gpu: "1"
          readinessProbe:
            httpGet: { path: /health, port: 8080 }
            initialDelaySeconds: 30   # 模型加载慢，启动探测要长
```

### 关键参数

| 参数 | 作用 | 建议 |
|---|---|---|
| --model-id | 模型 | 中文 BGE-large-zh-v1.5 等 |
| --max-batch-tokens | 最大批 token | 按显存调（16384 常见） |
| --max-batch-requests | 最大并发请求数 | 防单请求占满 |
| --max-client-batch-size | 单客户端批大小 | 限制客户端批 |

## 3. 服务化 API 设计

### 标准接口

```
POST /embed          # 文本 → 向量
POST /rerank         # 查询+候选 → 精排分数（若用 TEI-rerank）
GET  /health         # 健康检查
GET  /info           # 模型信息（维度/类型）
```

### 客户端调用示例

```python
import requests

def embed_batch(texts: list[str]) -> list[list[float]]:
    """调用 TEI 服务（批量）"""
    resp = requests.post(
        "http://tei:8080/embed",
        json={"inputs": texts, "normalize": True},
        timeout=10,
    )
    resp.raise_for_status()
    return resp.json()
```

### 客户端工程要点

| 要点 | 说明 |
|---|---|
| 连接池 | requests.Session / httpx 复用连接 |
| 批量 | 32/64/128 按显存调 |
| 超时 | 网络超时 + 重试（指数退避） |
| 降级 | TEI 挂了 → 缓存向量 → 明确报错 |

## 4. LiteLLM 模型网关

### 为什么需要网关

```
多模型统一入口：
业务代码只认一个 API 格式（OpenAI 兼容）
供应商切换/多供应商路由在网关层处理
```

### LiteLLM 核心能力

| 能力 | 说明 |
|---|---|
| 统一 API | OpenAI 兼容接口调所有供应商 |
| 多供应商 | DeepSeek/OpenAI/Anthropic/本地 vLLM |
| 密钥管理 | 虚拟 key + 配额 |
| 失败重试 | 供应商故障自动切换 |
| 成本追踪 | 每请求成本记录 |

### 配置示例

```yaml
# config.yaml
model_list:
  - model_name: deepseek-chat        # 逻辑名（业务代码用）
    litellm_params:
      model: deepseek/deepseek-chat
      api_key: os.environ/DEEPSEEK_API_KEY
  - model_name: embedding-bge
    litellm_params:
      model: openai/bge-large-zh
      api_base: http://tei:8080/v1   # 指向 TEI
      api_key: dummy
  - model_name: rerank-bge
    litellm_params:
      model: openai/bge-reranker
      api_base: http://tei:8080/v1
      api_key: dummy
```

### 网关部署要点

```
独立命名空间（llm-gateway）
高可用：多副本（无状态）
监控：请求成功率/延迟/成本
配额：虚拟 key 限流（防内部滥用）
```

## 5. 维度一致性与参数匹配

### 维度铁律（生产级）

```
Embedding 服务输出维度 必须 = 向量库索引维度
bge-large-zh-v1.5 = 1024
bge-small-zh-v1.5 = 512
bge-m3 = 1024
```

### 一致性检查清单

| 检查项 | 位置 |
|---|---|
| 模型输出维度 | TEI /info 查询 |
| 向量库维度 | Qdrant/Milvus 集合配置 |
| 业务代码假设 | 配置常量（防止硬编码） |
| 换模型后 | 全链路维度验证 + 重建索引 |

### 相似度算法匹配

```
TEI normalize=true → 余弦可用
向量库 distance=Cosine → 匹配
换算法（内积/欧氏）→ 全链路一致
```

### 批大小与显存

| 维度 | 批大小建议 | 显存 |
|---|---|---|
| 512 维小模型 | 128-256 | 小 |
| 1024 维大模型 | 32-64 | 中 |
| 长文本（bge-m3） | 16-32 | 大 |

```
公式：显存 ≈ 批大小 × 最大序列长度 × 维度 × 4B × 系数
实测校准：压测观察 OOM 前的最优批大小
```

## 6. 模型热切换

### 热切换机制

```
TEI 支持通过环境变量/挂载切换模型：
MODEL_REGISTRY_PATH 动态加载
或：新版本 Deployment + 滚动更新（推荐）
```

### 切换流程（零中断）

```
① 新模型部署为新版本（同 Service）
② 滚动更新：先切 1 副本验证
③ 验证维度一致 + 检索质量（测试集）
④ 全量切换
⑤ 旧模型保留（快速回滚）
```

### 切换必须验证

| 验证 | 方法 |
|---|---|
| 维度 | /info 与向量库一致 |
| 质量 | 测试集指标不降（阶段 3 评估） |
| 一致性 | 建库与查询同模型（阶段 2 铁律） |
| 性能 | 延迟/吞吐达标 |
| 兼容 | 业务代码无需改动（API 不变） |

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 为什么 Embedding 要服务化？ | 模型常驻/独立扩容/热切换/语言无关 |
| TEI 是什么？ | HuggingFace Embedding 推理服务，生产专用 |
| LiteLLM 网关的作用？ | 统一 API/多供应商/密钥管理/成本追踪 |
| 维度不一致会怎样？ | 检索失败或静默错误——检查清单防住 |
| 批大小怎么定？ | 按显存实测，OOM 前最优值 |
| 热切换怎么做？ | 新版本滚动更新 + 维度/质量/性能验证 |

### 面试加分表达

> "Embedding 服务化我选 TEI：独立 GPU Pod、模型常驻、REST API 语言无关。模型调用统一走 LiteLLM 网关——业务代码只认 OpenAI 兼容格式，换供应商只改网关配置。切换模型走滚动更新，必查维度一致性和测试集指标。"

## 8. 服务化常见故障与排查

| 故障 | 原因 | 排查 |
|---|---|---|
| Embedding 超时 | 批太大/显存不足 | 看 TEI 日志，调 max-batch-tokens |
| 返回 503 | 模型未就绪/副本全挂 | 探针状态 + 副本数 |
| 维度不符 | 模型切换未同步 | /info 与向量库核对 |
| 网关 429 | 供应商配额 | 看 LiteLLM 限流日志 |
| 向量乱码 | 编码不一致 | 统一 UTF-8 |

### 服务化验证脚本

```python
# 验证 Embedding 服务健康
import requests

def verify_tei(tei_url: str) -> dict:
    """三查：健康/维度/编码"""
    # 1. 健康
    r = requests.get(f"{tei_url}/health", timeout=5)
    assert r.status_code == 200
    # 2. 维度
    info = requests.get(f"{tei_url}/info", timeout=5).json()
    dim = info.get("max_embedding_dimensions") or len(
        requests.post(f"{tei_url}/embed", json={"inputs": ["测试"]}).json()[0])
    # 3. 编码质量（自检相似度）
    v = requests.post(f"{tei_url}/embed",
        json={"inputs": ["测试", "测试"], "normalize": True}).json()
    sim = sum(a * b for a, b in zip(v[0], v[1]))
    return {"healthy": True, "dim": dim, "self_sim": round(sim, 3)}
```

> 🎯 核心要点：Embedding 服务化（TEI）解决模型常驻/独立扩容/热切换/语言无关四个问题；LiteLLM 网关统一模型调用（一个接口管所有供应商）；维度一致性是生产铁律（检查清单防错）；模型热切换走滚动更新 + 四验证（维度/质量/一致性/性能）；服务化上线先跑三查验证脚本（健康/维度/自检相似度）。

---

**下一模块**：[06-监控可观测与评估闭环](06-监控可观测与评估闭环.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)