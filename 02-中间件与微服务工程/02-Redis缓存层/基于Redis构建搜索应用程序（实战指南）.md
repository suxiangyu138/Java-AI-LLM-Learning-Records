# 基于 Redis 构建搜索应用程序（实战指南）

> **定位**：依托 Redis 高性能和 Sorted Set/Hash/Set 实现轻量级搜索引擎，覆盖分词→索引→查询→缓存全流程，适用于中小规模搜索场景。

---

## 目录

1. [核心架构](#1-核心架构)
2. [核心组件实现](#2-核心组件实现)
3. [集成与注意事项](#3-集成与注意事项)

---

## 1. 核心架构

```text
数据采集 → 分词 → 索引构建(Sorted Set) → 搜索查询(Set交集) → 结果排序 → 缓存加速
```

### Redis 数据结构分配

| 模块 | 数据结构 | Key 格式 | 说明 |
|------|----------|----------|------|
| 原始数据 | Hash | `search:data:product:id` | 商品详情 |
| 关键词索引 | ZSet | `search:keyword` | Value=商品ID, Score=权重 |
| 搜索缓存 | String | `search:cache:keyword:page` | JSON 序列化 |

---

## 2. 核心组件实现

### 2.1 分词组件

```java
// IK 分词器分词 + 停用词过滤
IKSegmenter ikSegmenter = new IKSegmenter(reader, true);
while ((lexeme = ikSegmenter.next()) != null) {
    String keyword = lexeme.getLexemeText();
    if (!isStopWord(keyword) && keyword.length() >= 2) {
        keywords.add(keyword);
    }
}
```

### 2.2 索引构建

```java
// Hash 存储原始数据
hashOperations.putAll("search:data:product:" + productId, fields);

// ZSet 构建关键词索引（标题权重 > 描述权重）
double score = title.contains(keyword) ? TITLE_WEIGHT : DESC_WEIGHT;
zSetOperations.add("search:" + keyword, productId.toString(), score);
```

### 2.3 搜索查询

```java
// 多关键词取交集（同时包含所有关键词的商品）
Set<String> productIdSet = null;
for (String keyword : keywords) {
    Set<String> ids = zSetOperations.reverseRange("search:" + keyword, 0, -1);
    productIdSet = (productIdSet == null) ? ids : retainAll(productIdSet, ids);
}
// 按总权重降序排序
productVOList.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));
```

### 2.4 搜索缓存

```java
// 查询缓存 → 未命中则查询索引 → 写入缓存（30 分钟过期）
String cacheKey = "search:cache:" + searchText + ":" + pageNum + ":" + pageSize;
String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
if (cacheValue != null) return JSON.parseObject(cacheValue, SearchResult.class);
// ... 查询并缓存
```

---

## 3. 集成与注意事项

| 注意点 | 说明 |
|--------|------|
| **KEYS 慎用** | 模糊搜索用 `SCAN` 替代 `KEYS`，避免阻塞 |
| **权重设计** | 标题权重 > 描述权重，影响排序 |
| **索引同步** | 数据增删改时同步更新索引和缓存 |
| **模糊搜索** | `search:keyword*` 匹配所有相关关键词索引 |
| **场景适配** | 数据量 10 万级以内 ✅，百万级以上用 Elasticsearch |
