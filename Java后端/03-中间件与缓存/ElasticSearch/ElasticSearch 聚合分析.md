# ElasticSearch 聚合分析（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | ES 聚合分析速查大全
> **版本**：Elasticsearch 7.x/8.x
> **核心场景**：分组统计、数据分析、可视化报表、商品聚合筛选

---

## 一、聚合分类

```
ES Aggregation
├── Metric（指标聚合）          → 计算统计值
│   ├── avg / sum / min / max
│   ├── stats / extended_stats
│   ├── cardinality            → 去重计数
│   └── value_count            → 计数
├── Bucket（桶聚合）            → 分组
│   ├── terms                  → 按字段值分组
│   ├── range                  → 按数值范围分组
│   ├── date_histogram         → 按时间分组
│   ├── histogram              → 按直方图分组
│   └── filter / filters       → 按条件分组
├── Pipeline（管道聚合）        → 对聚合结果再计算
│   ├── avg_bucket
│   ├── max_bucket
│   ├── cumulative_sum
│   └── derivative
└── Matrix（矩阵聚合）          → 多字段交叉分析（较少用）
```

---

## 二、Metric（指标聚合）

### 2.1 基础统计

```json
GET /orders/_search
{
  "size": 0,             // 不要文档，只要聚合结果
  "aggs": {
    "total_amount": { "sum": { "field": "amount" } },
    "avg_amount": { "avg": { "field": "amount" } },
    "max_amount": { "max": { "field": "amount" } },
    "min_amount": { "min": { "field": "amount" } }
  }
}
```

返回：
```json
{
  "aggregations": {
    "total_amount": { "value": 5000000.0 },
    "avg_amount": { "value": 250.5 },
    "max_amount": { "value": 9999.0 },
    "min_amount": { "value": 1.0 }
  }
}
```

### 2.2 stats（一次全拿）

```json
{
  "aggs": {
    "amount_stats": {
      "stats": { "field": "amount" }
    }
  }
}
// 返回 count、min、max、avg、sum
```

### 2.3 cardinality（去重计数）

```json
{
  "aggs": {
    "unique_users": {
      "cardinality": { "field": "user_id" }
    }
  }
}
// 类似 SQL: SELECT COUNT(DISTINCT user_id) FROM orders
```

**注意**：cardinality 是近似值（HyperLogLog 算法），误差约 1-5%。精确去重在百万级数据下极耗内存。

---

## 三、Bucket（桶聚合）

### 3.1 terms（分组统计）

```json
GET /orders/_search
{
  "size": 0,
  "aggs": {
    "by_category": {
      "terms": {
        "field": "category.keyword",
        "size": 10,                    // 返回前 10 个分组
        "order": { "_count": "desc" }  // 按数量倒序
      },
      "aggs": {                        // 子聚合：每个分组内再统计
        "total_amount": { "sum": { "field": "amount" } },
        "avg_price": { "avg": { "field": "amount" } }
      }
    }
  }
}
```

返回：
```json
{
  "aggregations": {
    "by_category": {
      "buckets": [
        {
          "key": "手机",
          "doc_count": 1500,
          "total_amount": { "value": 3500000.0 },
          "avg_price": { "value": 2333.33 }
        },
        { "key": "电脑", "doc_count": 800, ... }
      ]
    }
  }
}
```

### 3.2 range（范围分组）

```json
{
  "aggs": {
    "price_range": {
      "range": {
        "field": "price",
        "ranges": [
          { "to": 1000, "key": "千元以下" },
          { "from": 1000, "to": 3000, "key": "1K-3K" },
          { "from": 3000, "to": 5000, "key": "3K-5K" },
          { "from": 5000, "key": "5K以上" }
        ]
      }
    }
  }
}
```

### 3.3 date_histogram（按时间分组 — 报表必备）

```json
{
  "query": {
    "range": {
      "create_time": {
        "gte": "2024-01-01",
        "lt": "2024-02-01"
      }
    }
  },
  "aggs": {
    "daily_orders": {
      "date_histogram": {
        "field": "create_time",
        "fixed_interval": "1d",          // 按天
        "format": "yyyy-MM-dd",
        "min_doc_count": 0,              // 无数据的也显示
        "extended_bounds": {             // 固定区间
          "min": "2024-01-01",
          "max": "2024-01-31"
        }
      },
      "aggs": {
        "revenue": { "sum": { "field": "amount" } }
      }
    }
  }
}
```

间隔选项：`1s` / `1m` / `1h` / `1d` / `1w` / `1M` / `1y`

### 3.4 histogram（数值直方图）

```json
{
  "aggs": {
    "price_histogram": {
      "histogram": {
        "field": "price",
        "interval": 500          // 每 500 元一个区间
      }
    }
  }
}
```

### 3.5 filter / filters（条件分组）

```json
{
  "aggs": {
    "active_users": {
      "filter": { "term": { "status": "active" } },
      "aggs": { "total": { "sum": { "field": "amount" } } }
    },
    "inactive_users": {
      "filter": { "term": { "status": "inactive" } },
      "aggs": { "total": { "sum": { "field": "amount" } } }
    }
  }
}
```

---

## 四、Pipeline（管道聚合）

对聚合结果再做聚合：

```json
{
  "size": 0,
  "aggs": {
    "daily_sales": {
      "date_histogram": {
        "field": "create_time",
        "fixed_interval": "1d"
      },
      "aggs": {
        "daily_revenue": { "sum": { "field": "amount" } },
        "cumulative_revenue": {          // 累计和
          "cumulative_sum": {
            "buckets_path": "daily_revenue"
          }
        },
        "revenue_derivative": {          // 日环比
          "derivative": {
            "buckets_path": "daily_revenue"
          }
        }
      }
    }
  }
}
```

---

## 五、电商聚合实战：Search 联名过滤

搜索结果页常见的"品牌/价格/分类"聚合筛选：

```json
GET /goods/_search
{
  "size": 20,
  "query": {
    "match": { "title": "手机" }
  },
  "aggs": {
    "brand_agg": {
      "terms": { "field": "brand.keyword", "size": 20 }
    },
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
    },
    "category_agg": {
      "terms": { "field": "category.keyword", "size": 10 }
    }
  }
}
```

---

## 六、性能注意事项

| 问题 | 建议 |
|---|---|
| **terms 数据不准** | terms 是 ES 协调节点层面聚合，分布式环境下是近似值。调大 `shard_size`（如 size * 2）可提高精度 |
| **文本字段不能聚合** | 用 `field.keyword` 而非 `field` |
| **cardinality 是近似值** | 需要精确自己去重，用 `scripted_metric` 或外部计算 |
| **大范围 date_histogram** | 时间跨度大且粒度过细时极度耗内存 |

---

## 七、面试核心要点

1. **桶聚合 vs 指标聚合？** 桶=分组（terms/range/date_histogram），指标=算值（sum/avg/max/min/cardinality）
2. **cardinality 精确吗？** 不精确，HyperLogLog 算法，误差 1-5%
3. **size=0 有啥用？** 只返回聚合结果不返回文档，省带宽
4. **terms 分组不准怎么办？** 调大 `shard_size`，扩大每个分片的采样数

---

## 八、极简总结

```
sum/avg/cardinality = 指标聚合（算一个值出来）
terms/range/date_histogram = 桶聚合（把数据分组）
聚合 + query = 先过滤再统计（类似 SQL WHERE + GROUP BY）
子聚合 = 桶里面再聚合（类似 SQL GROUP BY 后算 SUM/AVG）
size=0 = 只要聚合，不要文档
```
