# 快速吃透 Elasticsearch

> **定位**：基于 Lucene 的分布式全文搜索引擎。核心能力：海量数据快速检索、模糊查询、分词、高亮、聚合统计。

---

## 目录

1. [核心概念](#1-核心概念)
2. [底层原理](#2-底层原理)
3. [Docker 部署](#3-docker-部署)
4. [DSL 基础操作](#4-dsl-基础操作)
5. [查询分类](#5-查询分类)
6. [Java 整合](#6-java-整合)
7. [适用场景](#7-适用场景)

---

## 1. 核心概念

| ES | MySQL | 说明 |
|----|-------|------|
| Index（索引） | 数据库/表 | 数据组织单元 |
| Document（文档） | 一行数据 | JSON 格式 |
| Field（字段） | 列 | 文档属性 |
| Mapping（映射） | 表结构/字段类型 | 定义字段类型 |
| Shard（分片） | — | 数据拆分、分布式存储 |
| Type | ❌ 已废弃 | ES 7+ 移除 |

> ES 存 JSON 文档，天然适合非结构化、文本类数据。

---

## 2. 底层原理

### 倒排索引（灵魂）

| 索引方式 | 方向 | 特点 |
|----------|------|------|
| MySQL 正排索引 | 行 → 字段 | 精确查询快，模糊慢 |
| **ES 倒排索引** | 分词 → 文档 ID | ⭐ 全文检索速度碾压数据库 |

### 分词器

```text
"计算机专业" → "计算机"、"专业"
```

> 中文检索必须安装 **IK 分词器**（`elasticsearch-analysis-ik`）。

---

## 3. Docker 部署

```bash
docker run -d \
  --name es \
  -p 9200:9200 -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  elasticsearch:7.17.0
```

| 端口 | 用途 |
|:----:|------|
| 9200 | HTTP 客户端 |
| 9300 | 集群通信 |

> 访问 `http://localhost:9200` 验证。

---

## 4. DSL 基础操作

### 创建索引 + 映射

```json
PUT /user_index
{
  "mappings": {
    "properties": {
      "id": { "type": "integer" },
      "name": { "type": "text" },
      "desc": { "type": "text" }
    }
  }
}
```

### 新增文档

```json
POST /user_index/_doc
{ "id": 1, "name": "张三", "desc": "计算机专业学生" }
```

### 全文模糊检索（最常用）

```json
GET /user_index/_search
{ "query": { "match": { "desc": "计算机" } } }
```

### 精确查询

```json
GET /user_index/_search
{ "query": { "term": { "id": 1 } } }
```

### 其他

```json
GET /user_index/_doc/1     # 按 ID 查询
DELETE /user_index          # 删除索引
```

---

## 5. 查询分类

| 类型 | 用途 | 示例 |
|------|------|------|
| **`match`** | 全文分词模糊查询 | 文本搜索 |
| **`term`** | 精确匹配 | 数字、关键字 |
| **`bool`** | 多条件组合 | `must`/`should`/`must_not` |
| **`aggregation`** | 分组、求和、统计 | 聚合分析 |
| **`highlight`** | 搜索结果高亮 | 关键词高亮 |

---

## 6. Java 整合

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

### 两种开发方式

| 方式 | 适用 |
|------|------|
| `RestHighLevelClient` 原生 DSL | 企业主流 |
| `ElasticsearchRepository` CRUD | 快速开发 |

---

## 7. 适用场景

| ✅ 适合 | ❌ 不适合 |
|--------|----------|
| 全文检索、模糊搜索、海量文本 | 强事务、强一致性 |
| 日志/订单/文章/商品大数据查询 | 金融交易 |
| 分词、语义检索、复杂聚合 | 小数据简单查询 |
| RAG：文本向量化 + 语义检索 | 直接用 MySQL 就行的 |

---

> 🎯 **极简总结**：ES 基于倒排索引，擅长全文检索。结构：索引→文档→字段。核心 = 分词 + DSL 查询。标配姿势：**MySQL 存业务数据，ES 负责搜索**。
