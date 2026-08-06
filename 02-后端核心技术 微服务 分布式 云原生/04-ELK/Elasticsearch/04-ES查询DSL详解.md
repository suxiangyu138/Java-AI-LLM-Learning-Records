# Elasticsearch 查询 DSL 详解
> ES 的查询语言是搜索的核心——从全文搜索到精确匹配，从 bool 组合到高亮排序，完整覆盖实战。

## 目录
1. [查询分类总览](#1-查询分类总览)
2. [全文查询](#2-全文查询)
3. [精确查询](#3-精确查询)
4. [复合查询 bool](#4-复合查询-bool)
5. [排序与分页](#5-排序与分页)
6. [高亮显示](#6-高亮显示)
7. [结果过滤与 source](#7-结果过滤与-source)
8. [分页方案对比](#8-分页方案对比)
9. [聚合查询](#9-聚合查询)
10. [查询优化](#10-查询优化)

---

## 1. 查询分类总览

### 1.1 查询类型树

```text
ES 查询
├── 全文查询（Full Text） → 分词后匹配
│   ├── match             → 标准全文匹配
│   ├── match_phrase      → 短语精确匹配
│   ├── multi_match       → 多字段同时匹配
│   ├── match_all         → 查询所有文档
│   └── query_string      → Lucene 语法查询
├── 精确查询（Term Level） → 不分词精确匹配
│   ├── term              → 单值精确匹配
│   ├── terms             → 多值 IN 查询
│   ├── range             → 范围查询
│   ├── exists            → 字段存在性查询
│   ├── prefix            → 前缀匹配
│   ├── wildcard          → 通配符匹配
│   └── ids               → 按 ID 查询
├── 复合查询（Compound）
│   ├── bool              → must/filter/should/must_not
│   ├── boosting          → 降权查询
│   ├── constant_score    → 固定评分
│   └── function_score    → 自定义评分函数
├── 地理位置
│   ├── geo_distance      → 距离范围
│   └── geo_bounding_box  → 矩形范围
└── 聚合分析（Aggregations）
    ├── Metric            → 指标聚合（avg/sum/min/max）
    └── Bucket            → 桶聚合（分组）
```

### 1.2 查询结构模板

```json
GET /goods/_search
{
  "query": {
    "bool": {
      "must":     [...],     // 必须匹配（贡献评分）
      "filter":   [...],     // 必须匹配（不贡献评分，可缓存）
      "should":   [...],     // 可选匹配（提升评分）
      "must_not": [...]      // 必须不匹配
    }
  },
  "sort":   [{ "price": "asc" }],
  "from":   0,
  "size":   20,
  "_source": ["id", "title"],
  "highlight": { "fields": { "title": {} } },
  "aggs": { "price_stats": { "stats": { "field": "price" } } },
  "timeout": "10s"
}
```

---

## 2. 全文查询

### 2.1 match（最常用）

先对查询字符串分词，再匹配包含任一词的文档，按相关性（BM25 评分）排序。

```json
GET /goods/_search
{
  "query": { "match": { "title": "华为手机" } }
}
// 执行："华为手机" → ["华为", "手机"] → 匹配倒排索引 → BM25 排序

// 进阶参数
GET /goods/_search
{
  "query": {
    "match": {
      "title": {
        "query": "华为手机",
        "operator": "and",                // 全部匹配（默认 or）
        "minimum_should_match": "75%",    // 至少匹配 75% 的词
        "fuzziness": "AUTO",              // 模糊匹配，容忍拼写错误
        "boost": 2.0                      // 权重提升
      }
    }
  }
}
```

### 2.2 match_phrase（短语匹配）

要求词的顺序和位置完全一致。

```json
GET /goods/_search
{
  "query": { "match_phrase": { "title": "华为手机" } }
}
// "华为手机P40"     → 匹配 ✓
// "华为新款手机"   → 不匹配 ✗（中间有间隔）

// 带 slop 允许间隔
GET /goods/_search
{
  "query": {
    "match_phrase": {
      "title": { "query": "华为手机", "slop": 2 }
    }
  }
}
// "华为新款手机"   → 匹配 ✓（slop=1）
```

### 2.3 multi_match（多字段匹配）

```json
GET /goods/_search
{
  "query": {
    "multi_match": {
      "query": "华为手机",
      "fields": [
        "title^3",           // ^3 表示 3 倍权重
        "description",
        "brand^2"
      ],
      "type": "best_fields"  // 取最佳字段的评分
    }
  }
}
```

| type | 说明 |
|------|------|
| `best_fields`（默认）| 取任意字段的最高分 |
| `most_fields` | 各字段分数相加 |
| `cross_fields` | 跨字段视为一个字段 |

### 2.4 match_all（查询全部）

```json
GET /goods/_search
{ "query": { "match_all": {} }, "size": 0 }
```

### 2.5 query_string（Lucene 语法）

```json
GET /goods/_search
{
  "query": {
    "query_string": {
      "query": "华为 AND (手机 OR 平板) NOT 二手",
      "default_field": "title"
    }
  }
}
```

> ⚠️ `query_string` 用户输入特殊字符会报错，生产环境建议用 `bool` + `match` 替代。安全替代：`simple_query_string`。

---

## 3. 精确查询

### 3.1 term（单值精确匹配）

**关键：字段必须是 keyword 类型，或使用 `.keyword` 子字段。**

```json
// 正确（keyword 字段）
GET /goods/_search
{ "query": { "term": { "category": "手机" } } }

// 正确（text 的 keyword 子字段）
GET /goods/_search
{ "query": { "term": { "title.keyword": "华为手机P40" } } }

// 错误（text 字段直接用 term — 找不到！）
GET /goods/_search
{ "query": { "term": { "title": "华为手机" } } }
// title 已分词为 ["华为", "手机", "P40"]，term 不分词查不到
```

> 💡 `term` 查询不走分词，匹配的是倒排索引中存的完整词条。

### 3.2 terms（多值 IN 查询）

```json
GET /goods/_search
{ "query": { "terms": { "brand": ["华为", "小米", "苹果"] } } }
// 等价 SQL: WHERE brand IN ('华为', '小米', '苹果')
```

### 3.3 range（范围查询）

```json
GET /goods/_search
{
  "query": {
    "range": {
      "price": { "gte": 1000, "lte": 5000, "boost": 2.0 }
    }
  }
}
// gt: >, gte: >=, lt: <, lte: <=

// 日期范围
GET /orders/_search
{
  "query": {
    "range": {
      "create_time": {
        "gte": "now-30d/d",
        "lte": "now/d"
      }
    }
  }
}
```

### 3.4 其他精确查询

```json
// exists — 字段存在性
GET /goods/_search
{ "query": { "exists": { "field": "description" } } }

// prefix — 前缀匹配
GET /goods/_search
{ "query": { "prefix": { "brand": { "value": "华" } } } }

// wildcard — 通配符（性能较差，慎用）
GET /goods/_search
{ "query": { "wildcard": { "brand": { "value": "华*" } } } }

// ids — 按 ID 查
GET /goods/_search
{ "query": { "ids": { "values": ["1", "3", "5"] } } }
```

---

## 4. 复合查询 bool

### 4.1 四种子句

| 子句 | 作用 | 类比 SQL | 贡献评分 | 结果缓存 |
|:----:|------|:---------:|:--------:|:--------:|
| `must` | 必须匹配 | AND | 是 | 否 |
| `filter` | 必须匹配 | AND | **否** | **是** |
| `should` | 可选匹配 | OR | 是 | 否 |
| `must_not` | 必须不匹配 | NOT | 否 | 是 |

### 4.2 完整搜索示例

```json
GET /goods/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "华为手机" } }
      ],
      "filter": [
        { "term":  { "category": "手机" } },
        { "range": { "price": { "gte": 1000, "lte": 5000 } } },
        { "terms": { "brand": ["华为", "荣耀"] } },
        { "term":  { "is_on_sale": true } }
      ],
      "should": [
        { "match": { "description": "5G" } },
        { "match": { "description": "旗舰" } }
      ],
      "must_not": [
        { "term": { "status": "二手" } }
      ],
      "minimum_should_match": 1
    }
  },
  "sort": [{ "price": { "order": "asc" } }],
  "from": 0, "size": 20
}

// 等价 SQL 逻辑：
-- WHERE title LIKE '%华为%' OR title LIKE '%手机%'
--   AND category = '手机'
--   AND price BETWEEN 1000 AND 5000
--   AND brand IN ('华为', '荣耀')
--   AND is_on_sale = true
--   AND status != '二手'
-- ORDER BY price ASC LIMIT 20
```

### 4.3 filter 性能优势

```json
// ✅ 推荐：filter 不评分 + 可缓存
GET /goods/_search
{
  "query": {
    "bool": {
      "must": [{ "match": { "title": "华为" } }],
      "filter": [
        { "term":  { "brand": "华为" } },
        { "range": { "price": { "gte": 3000 } } }
      ]
    }
  }
}

// ❌ 不推荐：不必要地参与评分
GET /goods/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "华为" } },
        { "term":  { "brand": "华为" } }
      ]
    }
  }
}
```

> 💡 filter 不计算相关性评分（跳过打分阶段），结果会被 ES 缓存，相同 filter 复用效率极高。**能放 filter 不要放 must**。

### 4.4 should 与评分

```json
// should 控制排序，匹配的文档排前面
GET /goods/_search
{
  "query": {
    "bool": {
      "must": [{ "match": { "title": "手机" } }],
      "should": [
        { "match": { "brand": "华为" } },
        { "range": { "rating": { "gte": 4.5 } } }
      ],
      "minimum_should_match": 1
    }
  }
}
```

### 4.5 boosting 与 constant_score

```json
// boosting：降权不排除
GET /goods/_search
{
  "query": {
    "boosting": {
      "positive":     { "match": { "title": "手机" } },
      "negative":     { "term": { "status": "二手" } },
      "negative_boost": 0.2
    }
  }
}

// constant_score：固定评分
GET /goods/_search
{
  "query": {
    "constant_score": {
      "filter": { "term": { "brand": "华为" } },
      "boost": 1.0
    }
  }
}
```

---

## 5. 排序与分页

### 5.1 排序

```json
GET /goods/_search
{
  "sort": [
    { "price": { "order": "asc" } },
    { "_score": { "order": "desc" } },
    { "title.keyword": { "order": "asc" } }
  ]
}
```

### 5.2 基本分页

```json
GET /goods/_search
{ "from": 0, "size": 20, "query": { "match_all": {} } }
// page 1: from=0, size=20
// page 2: from=20, size=20
```

> ⚠️ **深度分页问题**：from+size 在深度大时性能急剧下降，默认上限 `max_result_window=10000`。因为 ES 需从每个分片取 (from+size) 条数据到协调节点汇总排序后截取。

---

## 6. 高亮显示

```json
GET /goods/_search
{
  "query": { "match": { "title": "华为" } },
  "highlight": {
    "pre_tags": ["<em>"],
    "post_tags": ["</em>"],
    "fields": {
      "title": { "fragment_size": 50, "number_of_fragments": 3 },
      "description": { "fragment_size": 100, "number_of_fragments": 2 }
    }
  }
}

// 响应
{
  "hits": [{
    "_source": { "title": "华为手机P40" },
    "highlight": { "title": ["<em>华为</em>手机P40"] }
  }]
}
```

---

## 7. 结果过滤与 source

```json
// 只返回指定字段
GET /goods/_search
{
  "_source": ["id", "title", "price"],
  "query": { "match_all": {} }
}

// 包含/排除模式
GET /goods/_search
{
  "_source": {
    "includes": ["id", "title", "price"],
    "excludes": ["description"]
  },
  "query": { "match_all": {} }
}

// 不返回 source（节省带宽）
GET /goods/_search
{ "_source": false, "query": { "match_all": {} } }
```

---

## 8. 分页方案对比

### 8.1 三种方案

| 分页方式 | 原理 | 优点 | 缺点 | 适用场景 |
|----------|------|------|------|---------|
| **from + size** | 深度分页 | 简单，支持跳页 | 越深越慢，上限 10000 | 前 1000 条 |
| **search_after** | 游标 | 无限深度，性能稳定 | 不可跳页 | 无限滚动 |
| **scroll** | 快照上下文 | 海量数据分批，一致快照 | 有状态，耗内存 | 批量导出 |

### 8.2 search_after（推荐）

```json
// 第一次查询
GET /goods/_search
{
  "size": 20,
  "sort": [{ "id": "asc" }],
  "query": { "match_all": {} }
}
// 最后一条 sort 值: [120]

// 后续：取最后一条的 sort 值传入
GET /goods/_search
{
  "size": 20,
  "search_after": [120],
  "sort": [{ "id": "asc" }],
  "query": { "match_all": {} }
}
```

### 8.3 scroll（批量导出）

```json
// 创建 scroll 上下文
GET /goods/_search?scroll=1m
{ "size": 1000, "query": { "match_all": {} } }
// 返回 _scroll_id

// 后续用 scroll_id 获取
POST /_search/scroll
{ "scroll": "1m", "scroll_id": "DXF1ZXJ5QW5kRmV0Y2gBAA..." }

// 用完务必清除
DELETE /_search/scroll
{ "scroll_id": "DXF1ZXJ5QW5kRmV0Y2gBAA..." }
```

---

## 9. 聚合查询

### 9.1 Metric Aggregations（指标聚合）

```json
// 统计价格
GET /goods/_search
{
  "size": 0,
  "aggs": {
    "price_stats": { "stats": { "field": "price" } }
  }
}
// 返回: count, min, max, avg, sum

// 范围分桶
GET /goods/_search
{
  "size": 0,
  "aggs": {
    "price_range": {
      "range": {
        "field": "price",
        "ranges": [
          { "to": 1000 },
          { "from": 1000, "to": 3000 },
          { "from": 3000, "to": 5000 },
          { "from": 5000 }
        ]
      }
    }
  }
}
```

### 9.2 Bucket Aggregations（桶聚合）

```json
// 按品牌分组 + 统计平均价格
GET /goods/_search
{
  "size": 0,
  "aggs": {
    "brand_count": {
      "terms": {
        "field": "brand",
        "size": 10
      },
      "aggs": {
        "avg_price": { "avg": { "field": "price" } }
      }
    }
  }
}
```

### 9.3 聚合 + 查询

```json
GET /goods/_search
{
  "query": { "match": { "category": "手机" } },
  "size": 0,
  "aggs": {
    "brand_count": { "terms": { "field": "brand", "size": 20 } },
    "price_stats": { "stats": { "field": "price" } }
  }
}
```

---

## 10. 查询优化

### 10.1 优化建议

| 优化手段 | 说明 | 效果 |
|----------|------|:----:|
| **用 filter 代替 must** | filter 不评分 + 可缓存 | 高 |
| **限制返回字段 `_source`** | 减少网络传输 | 中 |
| **search_after 代替深分页** | 深度查询性能稳定 | 高 |
| **避免 script 查询** | 脚本无法缓存 | 高 |
| **设置查询超时** | `timeout: 10s` | 中 |
| **用 keyword 做精确查询** | term 比 match 快 | 中 |

### 10.2 查询性能对比（从快到慢）

```text
1. filter (term/range/terms)  最快，可缓存
2. term 精确查询               次快
3. match 全文查询              分词有开销
4. phrase 短语匹配             需要位置信息
5. wildcard 通配符             需扫描所有 term
6. regexp 正则查询             最慢，慎用
```

### 10.3 慢查询日志

```json
PUT /goods/_settings
{
  "index.search.slowlog.threshold.query.warn": "10s",
  "index.search.slowlog.threshold.query.info": "5s",
  "index.search.slowlog.threshold.query.trace": "500ms"
}
```

---

## 面试核心要点

| 问题 | 答案 |
|------|------|
| **match 和 term 的区别？** | match 分词后匹配（全文搜索）；term 不分词精确匹配（keyword 字段）|
| **filter 和 must 的区别？** | filter 不评分 + 可缓存，性能更高；must 计算评分 |
| **ES 为什么不适合深分页？** | from+size 在每个分片查询并排序后聚合，深度越大性能指数级下降。用 search_after 替代 |
| **match_phrase 和 slop 的关系？** | slop 允许词之间的间隔数，slop=0 要求完全连续 |
| **scroll 和 search_after 区别？** | scroll 创建快照上下文、耗内存、适合批量导出；search_after 无状态、适合实时滚动 |
| **bool 查询子句执行顺序？** | must/filter 同时执行，should 在 must 后，must_not 最后过滤 |

> 🎯 **极简总结**：全文搜索用 `match`，精确查询用 `term`，精确条件放 `filter`，评分条件放 `must`，深分页用 `search_after`，海量导出用 `scroll`。
