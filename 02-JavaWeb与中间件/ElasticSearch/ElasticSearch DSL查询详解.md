# ElasticSearch DSL 查询详解（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | DSL 查询语法速查大全
> **版本**：Elasticsearch 7.x/8.x
> **核心场景**：搜索、过滤、多条件组合、高亮、排序、分页

---

## 一、查询分类总览

```
ES 查询
├── 全文查询（Full Text）    → 分词后匹配
│   ├── match               → 匹配词
│   ├── match_phrase        → 短语匹配
│   ├── multi_match         → 多字段匹配
│   └── match_all           → 查询所有
├── 精确查询（Term Level）   → 不分词，精确匹配
│   ├── term                → 精确值匹配
│   ├── terms               → 多值匹配（in）
│   ├── range               → 范围查询
│   ├── exists              → 字段是否存在
│   ├── prefix              → 前缀匹配
│   └── wildcard            → 通配符
├── 复合查询（Compound）      → 组合查询
│   ├── bool                → 多条件组合
│   ├── constant_score      → 固定评分
│   └── boosting            → 加权查询
├── 地理位置查询
│   └── geo_distance        → 附近范围
└── 特殊查询
    └── ids                 → 按 ID 查
```

---

## 二、全文查询（Full Text）

### 2.1 match（最常用）

```json
GET /goods/_search
{
  "query": {
    "match": {
      "title": "华为手机"
    }
  }
}
// 先分词"华为手机" → ["华为", "手机"]
// 再查询包含"华为"或"手机"的文档，按评分排序
```

**match 不是精确匹配，是分词后的 OR 关系！**

### 2.2 match AND 逻辑

```json
{
  "query": {
    "match": {
      "title": {
        "query": "华为手机",
        "operator": "and"       // 必须同时包含"华为"和"手机"
      }
    }
  }
}
```

### 2.3 match_phrase（短语匹配）

```json
{
  "query": {
    "match_phrase": {
      "title": "华为手机"       // 词语必须连续出现，顺序一致
    }
  }
}
```

**slop 参数**：允许词语之间随多 N 个词：

```json
{
  "query": {
    "match_phrase": {
      "title": {
        "query": "华为手机",
        "slop": 2               // 允许中间有最多 2 个词
      }
    }
  }
}
```

### 2.4 multi_match（多字段搜索）

```json
{
  "query": {
    "multi_match": {
      "query": "华为手机",
      "fields": ["title^3", "description", "brand"]  // title 权重 3 倍
    }
  }
}
```

| 参数 | 值 | 说明 |
|---|---|---|
| `best_fields` | 默认 | 得分最高的字段 |
| `most_fields` | - | 所有字段分数累加 |
| `cross_fields` | - | 跨字段视为一个大字段 |
| `phrase` | - | 精确短语匹配 |

---

## 三、精确查询（Term Level）

### 3.1 term（精确匹配）

```json
{
  "query": {
    "term": {
      "category.keyword": "手机"     // 不分词，精确等于"手机"
    }
  }
}
```

**注意**：term 用于 keyword 类型，text 类型用 match。

### 3.2 terms（IN 查询）

```json
{
  "query": {
    "terms": {
      "category.keyword": ["手机", "电脑", "平板"]
    }
  }
}
```

### 3.3 range（范围查询）

```json
{
  "query": {
    "range": {
      "price": {
        "gte": 1000,
        "lte": 5000
      }
    }
  }
}
```

| 参数 | 含义 |
|---|---|
| `gte` | >= |
| `gt` | > |
| `lte` | <= |
| `lt` | < |

### 3.4 时间范围

```json
{
  "query": {
    "range": {
      "create_time": {
        "gte": "2024-01-01 00:00:00",
        "lt": "2024-12-31 23:59:59",
        "format": "yyyy-MM-dd HH:mm:ss",
        "time_zone": "+08:00"
      }
    }
  }
}
```

### 3.5 exists / prefix / wildcard

```json
// 字段存在
{ "query": { "exists": { "field": "discount" } } }

// 前缀匹配
{ "query": { "prefix": { "brand.keyword": "华为" } } }

// 通配符
{ "query": { "wildcard": { "brand.keyword": "华*" } } }
```

---

## 四、复合查询（Bool Query —— 核心中的核心）

### 4.1 四大子句

```json
{
  "query": {
    "bool": {
      "must": [ ... ],         // AND — 必须满足，参与评分
      "filter": [ ... ],       // AND — 必须满足，不评分，可缓存
      "should": [ ... ],       // OR — 满足越多分越高
      "must_not": [ ... ]      // NOT — 必须不满足
    }
  }
}
```

### 4.2 实战示例：电商商品搜索

```json
GET /goods/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "华为手机" } }
      ],
      "filter": [
        { "term": { "is_on_sale": true } },
        { "range": { "price": { "gte": 1000, "lte": 5000 } } },
        { "terms": { "brand.keyword": ["华为", "荣耀"] } }
      ],
      "must_not": [
        { "term": { "stock": 0 } }
      ],
      "should": [
        { "term": { "tags.keyword": "新品" } }
      ],
      "minimum_should_match": 0
    }
  }
}
```

### 4.3 minimum_should_match

控制 should 子句中至少满足几个：

```json
{
  "bool": {
    "should": [
      { "match": { "title": "手机" } },
      { "match": { "title": "5G" } },
      { "match": { "title": "新品" } }
    ],
    "minimum_should_match": 1      // 至少匹配 1 个
  }
}
```

### 4.4 Filter vs Must

| 维度 | filter | must |
|---|---|---|
| **评分** | 不影响评分 | 影响评分 |
| **缓存** | 自动缓存结果 | 不缓存 |
| **性能** | 更快 | 更慢 |
| **使用场景** | 精确过滤（范围、状态） | 全文匹配 |

**最佳实践**：精确条件用 filter，搜索匹配用 must。

---

## 五、高亮显示（Highlight）

```json
GET /goods/_search
{
  "query": {
    "match": { "title": "华为手机" }
  },
  "highlight": {
    "fields": {
      "title": {
        "pre_tags": ["<em>"],
        "post_tags": ["</em>"],
        "fragment_size": 60,
        "number_of_fragments": 1
      }
    }
  }
}
```

返回结果中 `highlight.title` 包含被 `<em>手机</em>` 包裹的关键词。

---

## 六、排序

```json
GET /goods/_search
{
  "query": { "match_all": {} },
  "sort": [
    { "price": { "order": "asc" } },
    { "create_time": { "order": "desc" } },
    "_score"                        // 按评分
  ]
}
```

**注意**：text 字段不能用于排序，需用 `.keyword` 子字段。

---

## 七、分页

### 7.1 from + size（浅分页）

```json
{
  "from": 0,
  "size": 20,
  "query": { "match_all": {} }
}
```

限制：`from + size <= max_result_window`（默认 10000）。

### 7.2 search_after（深分页 / 滚动）

```json
// 第 1 页
GET /goods/_search
{
  "size": 20,
  "sort": [
    { "create_time": "desc" },
    { "_id": "asc" }            // 必须有唯一排序字段
  ]
}
// 返回最后一个 sort 值

// 第 2 页
GET /goods/_search
{
  "size": 20,
  "sort": [
    { "create_time": "desc" },
    { "_id": "asc" }
  ],
  "search_after": [1700000000000, "doc_12345"]  // 上一页最后一条 sort 值
}
```

### 7.3 Scroll（数据导出）

```json
// 创建 Scroll
GET /goods/_search?scroll=2m
{
  "size": 1000,
  "query": { "match_all": {} }
}

// 翻页
GET /_search/scroll
{
  "scroll": "2m",
  "scroll_id": "返回的 scroll_id"
}

// 清理
DELETE /_search/scroll/_all
```

---

## 八、控制返回字段

```json
GET /goods/_search
{
  "_source": ["title", "price", "brand"],     // 只要这 3 个字段
  "query": { "match_all": {} }
}

// 排除某些字段
{
  "_source": {
    "excludes": ["description", "detail_info"]
  }
}
```

---

## 九、评分控制

### 9.1 查看评分详情

```json
GET /goods/_search
{
  "explain": true,           // 返回详细评分计算过程
  "query": {
    "match": { "title": "手机" }
  }
}
```

### 9.2 TF-IDF → BM25

ES 5.x+ 默认使用 **BM25** 算法评分：
- TF（Term Frequency）：词在文档中出现次数，次数越多得分越高（有上限）
- IDF（Inverse Document Frequency）：词在多少文档中出现过，越多得分越低
- Field Length：文档越长，TF 贡献越低

---

## 十、面试核心要点

1. **match vs term？** match 分词模糊匹配，term 精确匹配不分词
2. **filter vs must？** filter 不评分可缓存（更快），must 评分不可缓存
3. **深分页解决方案？** search_after（实时滚动）、scroll（数据导出）
4. **bool 四个子句？** must/filter/should/must_not
5. **text 字段不能排序怎么办？** 用 `.keyword` 子字段或多字段映射

---

## 十一、极简总结

```
match = 分词模糊搜（找"手机"能搜到"华为手机"）
term = 精确匹配（查状态、ID、分类）
bool + filter = 精确过滤 + 缓存（性能王者）
bool + should = 加权匹配（越多越靠前）
深分页 = search_after（不用 from + size 翻 100 页）
```
