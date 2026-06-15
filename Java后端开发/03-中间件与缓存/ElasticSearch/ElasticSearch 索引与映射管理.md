# ElasticSearch 索引与映射管理（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | 索引与 Mapping 全生命周期管理
> **版本**：Elasticsearch 7.x/8.x
> **核心场景**：创建索引、管理 Mapping、Reindex、索引模板

---

## 一、索引（Index）核心概念

Index 是 ES 中存储数据的**逻辑命名空间**，类比 MySQL 的 Database/Table：

```
MySQL：  Database > Table > Row > Column
ES：     Index > Type(废弃) > Document > Field
```

7.x 中一个 Index 只能有一个 `_doc` Type，8.x 中 Type 已完全移除。

---

## 二、创建索引 + Mapping

### 2.1 基础创建

```json
PUT /goods
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1
  },
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "title": { 
        "type": "text", 
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart" 
      },
      "price": { "type": "double" },
      "category": { "type": "keyword" },
      "brand": { "type": "keyword" },
      "stock": { "type": "integer" },
      "description": { "type": "text", "analyzer": "ik_max_word" },
      "create_time": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" },
      "is_on_sale": { "type": "boolean" },
      "tags": { "type": "keyword" }
    }
  }
}
```

### 2.2 Settings 配置项

| 配置项 | 说明 | 默认值 | 推荐值 |
|---|---|---|---|
| `number_of_shards` | 主分片数 | 1 | 按数据量计算 |
| `number_of_replicas` | 副本数 | 1 | 生产至少 1 |
| `refresh_interval` | 刷新间隔 | 1s | 写入优化时可调大（30s） |
| `max_result_window` | 分页最大窗口 | 10000 | 大数据量用 search_after |

```json
PUT /high_write_index
{
  "settings": {
    "number_of_shards": 5,
    "number_of_replicas": 1,
    "refresh_interval": "30s",          // 拉大 refresh，提升写入性能
    "translog.durability": "async",     // 异步 Translog
    "translog.sync_interval": "30s"
  }
}
```

---

## 三、Mapping 字段类型详解

### 3.1 常用类型

| 类型 | 用途 | 说明 |
|---|---|---|
| `text` | 全文搜索 | 分词，支持 match 查询 |
| `keyword` | 精确匹配 | 不分词，用于过滤/排序/聚合 |
| `long / integer / short / byte` | 整数 | 按范围选 |
| `float / double` | 浮点数 | |
| `boolean` | 布尔 | `true` / `false` |
| `date` | 日期 | 支持多种格式 |
| `binary` | 二进制 | Base64 编码 |
| `geo_point` | 地理坐标 | LBS 搜索 |
| `dense_vector` | 密集向量 | 8.x 语义搜索 |

### 3.2 关键参数

```json
{
  "field_name": {
    "type": "text",
    "analyzer": "ik_max_word",          // 索引时分词器
    "search_analyzer": "ik_smart",      // 搜索时分词器
    "fields": {                         // 多字段映射
      "keyword": { "type": "keyword" }  // 不分词的子字段
    },
    "index": true,                      // 是否建索引（false 则不可搜索）
    "store": false,                     // 是否单独存储（默认 _source 已存储）
    "copy_to": "all_fields"             // 复制到目标字段（统一搜索）
  }
}
```

### 3.3 多字段映射（重要）

同一个字段，同时支持全文搜索 + 精确匹配：

```json
{
  "title": {
    "type": "text",
    "analyzer": "ik_max_word",
    "fields": {
      "keyword": { "type": "keyword", "ignore_above": 256 }
    }
  }
}
```

使用：
```json
// 全文搜索用 title
{"match": {"title": "手机"}}
// 精确匹配/排序/聚合用 title.keyword
{"term": {"title.keyword": "华为Mate60"}}
```

---

## 四、Mapping 管理操作

### 4.1 查看 Mapping

```json
GET /goods/_mapping
```

### 4.2 新增字段（支持增量添加）

```json
PUT /goods/_mapping
{
  "properties": {
    "discount": { "type": "float" }
  }
}
```

### 4.3 修改字段（限制）

已有字段**不能修改类型**和**分词器**：
- 不能改 `text` → `keyword`
- 不能改分词器
- **只能新增字段**

需要修改 = **Reindex**

---

## 五、Dynamic Mapping（动态映射）

### 5.1 行为控制

```json
PUT /my_index
{
  "mappings": {
    "dynamic": "strict",  // true / false / strict
    "properties": { ... }
  }
}
```

| 值 | 行为 |
|---|---|
| `true`（默认） | 自动检测并添加新字段 |
| `false` | 新字段不建索引，不报错 |
| `strict` | 新字段直接报错（生产推荐） |

### 5.2 Dynamic Template

根据字段名/类型自动匹配 Mapping：

```json
PUT /my_index
{
  "mappings": {
    "dynamic_templates": [
      {
        "strings_as_keyword": {
          "match_mapping_type": "string",
          "mapping": { "type": "keyword" }
        }
      },
      {
        "longs_as_long": {
          "match_mapping_type": "long",
          "mapping": { "type": "long" }
        }
      }
    ]
  }
}
```

---

## 六、Reindex（索引迁移）

### 6.1 基础用法

当需要修改 Mapping 时，通过 Reindex 迁移数据：

```json
// 1. 创建新索引（正确 Mapping）
PUT /goods_v2
{
  "mappings": {
    "properties": {
      "title": { "type": "text", "analyzer": "ik_max_word" },
      "price": { "type": "double" }
    }
  }
}

// 2. 迁移数据
POST /_reindex
{
  "source": { "index": "goods" },
  "dest": { "index": "goods_v2" }
}

// 3. 确认数据完整性后切换
POST /_aliases
{
  "actions": [
    { "remove": { "index": "goods", "alias": "goods_alias" } },
    { "add": { "index": "goods_v2", "alias": "goods_alias" } }
  ]
}

// 4. 删除旧索引
DELETE /goods
```

### 6.2 异步 Reindex（大数据量）

```json
POST /_reindex?wait_for_completion=false
{
  "source": { "index": "goods", "size": 1000 },
  "dest": { "index": "goods_v2" }
}
// 返回 task_id
```

```json
// 查询进度
GET /_tasks/{task_id}
```

---

## 七、索引别名（Alias）

别名让你**无缝切换**索引，生产必备：

```json
// 给索引加别名
POST /_aliases
{
  "actions": [
    { "add": { "index": "goods", "alias": "goods_current" } }
  ]
}

// 业务代码中直接使用别名
GET /goods_current/_search

// 无缝切换新索引
POST /_aliases
{
  "actions": [
    { "remove": { "index": "goods", "alias": "goods_current" } },
    { "add": { "index": "goods_v2", "alias": "goods_current" } }
  ]
}
```

---

## 八、索引模板（Index Template）

自动为新创建的索引应用预设的 Settings + Mapping：

```json
PUT /_index_template/goods_template
{
  "index_patterns": ["goods*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1
    },
    "mappings": {
      "properties": {
        "title": { "type": "text", "analyzer": "ik_max_word" },
        "price": { "type": "double" },
        "create_time": { "type": "date" }
      }
    }
  }
}
```

之后创建 `goods_2024`、`goods_2025` 等索引，自动应用以上配置。

---

## 九、常用管理命令

```json
// 查看所有索引
GET _cat/indices?v

// 查看索引健康
GET _cat/indices?v&health=yellow

// 查看索引设置
GET /goods/_settings

// 关闭/打开索引（停用/恢复读写）
POST /goods/_close
POST /goods/_open

// 强制合并（优化查询）
POST /goods/_forcemerge?max_num_segments=1

// 清空索引（保留结构）
POST /goods/_delete_by_query?conflicts=proceed
{ "query": { "match_all": {} } }

// 删除索引
DELETE /goods
```

---

## 十、面试核心要点

1. **Mapping 字段能改吗？** 不能改类型和分词器，只能新增
2. **Reindex 是什么？** 创建新索引 + 迁移数据，用于修改 Mapping
3. **Alias 有啥用？** 无缝切换索引，生产必备
4. **Dynamic Template 做什么？** 自动为新字段匹配设定好的 Mapping
5. **text vs keyword？** text 分词+全文搜索，keyword 不分词+精确匹配/排序/聚合

---

## 十一、极简总结

```
创建索引 = Settings（分片/副本/刷新） + Mapping（字段类型/分词器）
改 Mapping = 只能新增字段 → 需要改 → Reindex
索引别名 = 业务代码只认别名 → 背后随意切换 → 不停机
```
