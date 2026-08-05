# 04 - Milvus 生产级部署

> 🎯 Milvus = 云原生分布式向量数据库 — 支持 10 亿级向量、水平扩展、多租户。当 Chroma 不够用时，这就是答案

---

## 目录

1. [Milvus 架构](#1-milvus-架构)
2. [Docker 部署](#2-docker-部署)
3. [Collection 设计](#3-collection-设计)
4. [索引与检索](#4-索引与检索)
5. [分区与多租户](#5-分区与多租户)
6. [监控与运维](#6-监控与运维)

---

## 1. Milvus 架构

```text
Milvus 四层架构：

┌────────────────────────────────────────────┐
│  接入层 (Proxy) — 负载均衡、请求路由         │
├────────────────────────────────────────────┤
│  协调层 (Coordinator)                       │
│  ├── Root Coord (DDL)                      │
│  ├── Query Coord (查询)                     │
│  └── Data Coord (数据)                      │
├────────────────────────────────────────────┤
│  执行层 (Worker Node)                       │
│  ├── Query Node (查询执行)                  │
│  └── Data Node (数据写入)                   │
├────────────────────────────────────────────┤
│  存储层                                     │
│  ├── MinIO/S3 (向量/日志)                   │
│  ├── etcd (元数据)                          │
│  └── Pulsar/Kafka (消息)                    │
└────────────────────────────────────────────┘

关键：存算分离 → 各层可独立扩缩
```

---

## 2. Docker 部署

```yaml
# docker-compose.yml — Milvus Standalone
version: '3.5'
services:
  etcd:
    image: quay.io/coreos/etcd:v3.5.5
    environment:
      - ETCD_AUTO_COMPACTION_MODE=revision
    volumes:
      - etcd_data:/etcd

  minio:
    image: minio/minio:latest
    environment:
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin
    command: minio server /data
    volumes:
      - minio_data:/data

  standalone:
    image: milvusdb/milvus:v2.4.0
    command: milvus run standalone
    ports:
      - "19530:19530"
      - "9091:9091"
    depends_on:
      - etcd
      - minio

volumes:
  etcd_data:
  minio_data:
```

```bash
docker-compose up -d
# Milvus 运行在 localhost:19530
```

---

## 3. Collection 设计

```python
from pymilvus import MilvusClient, DataType

client = MilvusClient("milvus_demo.db")  # 或连接: uri="http://localhost:19530"

# 定义 Schema
schema = client.create_schema(
    auto_id=False,
    enable_dynamic_field=True,
)

schema.add_field("id", DataType.INT64, is_primary=True)
schema.add_field("text", DataType.VARCHAR, max_length=65535)
schema.add_field("vector", DataType.FLOAT_VECTOR, dim=1024)
schema.add_field("source", DataType.VARCHAR, max_length=256)
schema.add_field("chunk_index", DataType.INT64)

# 创建 Collection
client.create_collection(
    collection_name="rag_docs",
    schema=schema,
    index_params={
        "field_name": "vector",
        "index_type": "HNSW",
        "metric_type": "COSINE",
        "params": {"M": 16, "efConstruction": 200}
    }
)

# 插入数据
data = [{
    "id": i,
    "text": chunk,
    "vector": embedding,
    "source": source_file,
    "chunk_index": i
} for i, (chunk, embedding, source_file) in enumerate(chunks)]
client.insert("rag_docs", data)
```

### 索引参数调优

| 参数 | 含义 | 推荐值 |
|------|------|:---:|
| **M** | HNSW 节点连接数 | 16-32 |
| **efConstruction** | 构建时搜索宽度 | 100-200 |
| **ef** | 查询时搜索宽度 | 64-128 |
| **nlist** (IVF) | 聚类中心数 | sqrt(N) |

---

## 4. 索引与检索

```python
# 检索
results = client.search(
    collection_name="rag_docs",
    data=[query_vector],
    limit=5,
    search_params={"ef": 64},
    output_fields=["text", "source"],
    filter='source == "handbook.pdf"'  # 元数据过滤
)

for r in results[0]:
    print(f"[{r['distance']:.3f}] {r['entity']['text'][:50]}")
```

---

## 5. 分区与多租户

```python
# 按 tenant 分区
client.create_partition("rag_docs", "tenant_a")
client.create_partition("rag_docs", "tenant_b")

# 插入到指定分区
client.insert("rag_docs", data, partition_name="tenant_a")

# 按分区检索
results = client.search(..., partition_names=["tenant_a"])
```

---

## 6. 监控与运维

```text
Milvus 内置 Prometheus 指标：
  → 端口 9091 暴露 metrics

关键指标：
  □ 查询 QPS 和延迟 P99
  □ 索引构建进度
  □ 各组件内存/CPU
  □ Segment 数量和大小
  □ 磁盘使用率

备份：milvus-backup 工具全量/增量备份
滚动升级：先升级 Proxy → Query Node → Data Node → Index Node
```

---

## 核心要点回顾

- Milvus 存算分离 → 各层独立扩缩
- Collection = Table，Schema 定义字段类型 + 向量维度
- HNSW 索引：M=16-32, efConstruction=100-200
- 分区实现多租户隔离
