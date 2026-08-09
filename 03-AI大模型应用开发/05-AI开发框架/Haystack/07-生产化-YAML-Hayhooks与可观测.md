# 07 生产化：YAML、Hayhooks 与可观测

> Haystack 的"生产就绪"落地：管道 YAML 序列化进 CI/CD、Hayhooks 一键部署 K8s、OpenTelemetry 原生追踪、内置评估框架——从"代码能跑"到"系统可运维"。

## 📚 目录

1. [生产化全景](#1-生产化全景)
2. [YAML 序列化](#2-yaml-序列化)
3. [Hayhooks 部署](#3-hayhooks-部署)
4. [OpenTelemetry 可观测](#4-opentelemetry-可观测)
5. [内置评估框架](#5-内置评估框架)
6. [生产实践清单](#6-生产实践清单)
7. [面试高频问法](#7-面试高频问法)
8. [CI/CD 集成示例](#8-cicd-集成示例)
9. [常见误区](#9-常见误区)

## 1. 生产化全景

```
Haystack 生产四件套：
① 序列化：管道 → YAML（版本控制）
② 部署：Hayhooks（HTTP 服务/K8s）
③ 观测：OpenTelemetry（追踪）
④ 评估：内置指标（质量门禁）
```

| 件 | 解决什么 |
|---|---|
| YAML | 管道可版本化、可审查 |
| Hayhooks | 管道变 HTTP 服务（一键部署） |
| OTel | 请求级追踪（慢在哪一层） |
| 评估 | 质量可量化（退化可发现） |

## 2. YAML 序列化

### 导出与加载

```python
# 导出管道为 YAML
pipeline_yaml = qa_pipeline.dumps()
with open("qa_pipeline.yaml", "w") as f:
    f.write(pipeline_yaml)

# 加载
from haystack import Pipeline
pipeline = Pipeline.loads(open("qa_pipeline.yaml").read())
```

### YAML 的价值

| 价值 | 说明 |
|---|---|
| 版本控制 | 管道配置进 git（review/回滚） |
| 环境复用 | dev/prod 同 YAML 不同参数 |
| 文档化 | YAML 即管道文档 |
| CI/CD | 测试中加载 YAML 跑管道 |

### YAML 示例（简化）

```yaml
components:
  embedder:
    type: haystack_integrations.components.embeddings.sentence_transformers.SentenceTransformersTextEmbedder
    init_parameters:
      model: BAAI/bge-small-zh-v1.5
  retriever:
    type: haystack_integrations.components.retrievers.chroma.ChromaEmbeddingRetriever
    init_parameters:
      top_k: 5
connections:
  - receiver: retriever.query_embedding
    sender: embedder.embedding
metadata:
  version: 1.0
```

## 3. Hayhooks 部署

### 是什么

```
Hayhooks = 把 Haystack 管道部署为 HTTP 服务的工具
单 Docker 镜像 + Helm chart → K8s 部署
```

### 部署流程

```
① 管道导出 YAML
② Hayhooks 加载（管道注册为 HTTP 端点）
③ Docker 打包（单镜像）
④ K8s 部署（Helm）
⑤ 调用：POST /pipeline-name {"query": "..."}
```

### 架构形态

```
请求 → Hayhooks（HTTP 端点）→ 管道（YAML 定义）→ 响应
```

### 与 RAG 阶段 5 的衔接

```
Hayhooks = 应用层的"管道服务化"（阶段 5 的 05 篇思想）
管道 YAML = 服务配置（版本化/可回滚）
K8s 部署 = 探针/HPA/命名空间（阶段 5 的 03 篇）
```

## 4. OpenTelemetry 可观测

### 原生支持

```
Haystack 2.x 内置 OTel 追踪：
管道执行自动生成 spans（组件级）
可对接：Jaeger/Tempo/Datadog/Honeycomb
```

### 能追踪到什么

| 追踪内容 | 说明 |
|---|---|
| 管道级 | 整次请求耗时 |
| 组件级 | 每个组件耗时（定位慢在哪） |
| 检索 | 检索耗时/结果数 |
| LLM | 生成耗时/token |

### 接入方式

```python
# 配置 OTel exporter（环境变量或代码）
from opentelemetry import trace
# 管道运行自动产生 spans
result = pipeline.run({"embedder": {"text": query}})
```

### 观测价值

```
"管道即图" → "追踪即图"：
架构图 = 追踪链路，哪层慢一目了然
（对应阶段 5 的 06 篇链路追踪思想）
```

## 5. 内置评估框架

### 两类指标

| 类别 | 指标 | 用途 |
|---|---|---|
| 检索质量 | MRR / MAP / NDCG | 检索排序质量 |
| 生成质量 | faithfulness / answer relevance | 生成忠实度 |

### 评估方式

```
① 基于参考答案（有 ground truth）
② LLM judge（无参考答案）
```

### 评估接入（RAG 阶段 3 的落地）

```
① 建立测试集（问题 + 参考答案）
② 管道跑测试集
③ 计算指标（检索 MRR/NDCG + 生成 faithfulness）
④ CI 门禁（指标低于阈值 → 阻止合并）
⑤ 生产持续（每日评估防退化）
```

### 与 Ragas 的关系

```
Haystack 内置评估 = 快速集成的第一道门禁
Ragas = 更全面的评估框架（可独立使用）
生产建议：内置评估进 CI + Ragas 定期深评
```

## 6. 生产实践清单

### 上线检查清单

| 类别 | 检查项 | ✅ |
|---|---|---|
| 序列化 | 管道 YAML 入库（版本化） | ☐ |
| 部署 | Hayhooks 端点可调用 | ☐ |
| 观测 | OTel 追踪已接入（Jaeger/Tempo） | ☐ |
| 评估 | 测试集 + CI 门禁已配置 | ☐ |
| 存储 | DocumentStore 生产选型（非 InMemory） | ☐ |
| 模型 | Key 走环境变量/Secret | ☐ |
| 性能 | 压测 P95 达标 | ☐ |
| 回滚 | YAML 可回滚（git revert） | ☐ |

### 与 RAG 阶段 5 的完整对照

```
Haystack 生产化 = 阶段 5 的思想 + 框架内建：
部署（阶段 5 的 01-04 篇）→ Hayhooks/K8s
观测（阶段 5 的 06 篇）→ OTel 原生
评估（阶段 3/5）→ 内置指标 + CI 门禁
安全（阶段 5 的 08 篇）→ 密钥/白名单/HITL
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 管道怎么版本化？ | YAML 序列化（dumps/loads）+ git |
| Hayhooks 是什么？ | 管道部署为 HTTP 服务（Docker/K8s） |
| 可观测怎么做？ | OTel 原生（组件级 spans） |
| 内置评估有哪些？ | MRR/MAP/NDCG + faithfulness/relevance |
| 评估怎么进 CI？ | 测试集 + 阈值门禁 |
| 生产四件套？ | YAML/Hayhooks/OTel/评估 |

### 面试加分表达

> "Haystack 生产化是'框架内建'的：管道 dumps 成 YAML 进 git（可审查可回滚），Hayhooks 把管道变 HTTP 服务部署 K8s，OTel 原生追踪组件级延迟，内置评估进 CI 门禁——四件套都是开箱即用，这正是它'生产就绪'的含金量。"

## 8. CI/CD 集成示例

```yaml
# .github/workflows/rag-ci.yml（简化）
name: RAG Pipeline CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: "3.11" }
      - run: pip install haystack-ai haystack-integrations-*
      - name: 管道结构校验（构建期校验在测试中触发）
        run: python -c "
          from haystack import Pipeline
          p = Pipeline.loads(open('qa_pipeline.yaml').read())
          print('管道加载成功')
          "
      - name: 评估门禁（测试集指标）
        run: python eval_rag.py --yaml qa_pipeline.yaml --threshold 0.8
```

### CI 门禁的意义

```
① 管道 YAML 变更 → 结构校验（构建期错误拦截）
② 检索/生成指标 → 阈值门禁（质量回归拦截）
③ 发布 = 合并 YAML 变更 → Hayhooks 滚动更新
（对应 RAG 阶段 3 的 CI 门禁思想，Haystack 让 YAML 可测）
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "YAML 只是配置" | 是管道本体（可加载可测试可回滚） |
| "评估上生产后再说" | 评估先行（RAG 阶段 3 纪律） |
| "OTel 要自己接入" | 原生支持（配置 exporter 即可） |
| "Hayhooks 必须 K8s" | 单 Docker 也可（小规模） |
| "生产四件套可选" | 缺一件 = 运维黑洞（部署/观测/评估） |

> 🎯 核心要点：生产四件套（YAML 序列化/Hayhooks 部署/OTel 观测/内置评估）；YAML 让管道可版本化可回滚（CI 可加载测试）；Hayhooks = 管道服务化（K8s 一键）；OTel 原生"追踪即图"；内置评估（检索 MRR/NDCG + 生成 faithfulness）进 CI 门禁（YAML 变更即质量回归测试）；与 RAG 阶段 5 的思想完全打通，Haystack 只是框架内建。

---

**下一模块**：[08-对比选型与面试](08-对比选型与面试.md) / **返回总览**：[00-Haystack知识体系总览](00-Haystack知识体系总览.md)
