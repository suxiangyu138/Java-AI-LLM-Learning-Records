快速吃透 Elasticsearch
一、核心定位
- ElasticSearch：分布式全文搜索引擎，基于 Lucene
- 核心能力：海量数据快速检索、模糊查询、分词、高亮、聚合统计
- 日常用途：
    1. 商品搜索、博客文章检索、日志检索
    2. 大数据量复杂条件模糊查询（MySQL 搞不定的）
    3. 搭配 Kibana 做日志分析
    4. RAG 场景：文本向量化+语义检索（轻量替代向量库）
 
二、核心概念（对标 MySQL 一秒理解）
Elasticsearch MySQL 对应概念 
Index（索引） 数据库/表 
Type（废弃） 行（旧版本） 
Document（文档） 一行数据 
Field（字段） 列 
Mapping（映射） 表结构/字段类型 
Shard 分片 数据拆分、分布式存储 
核心：
ES 存的是 JSON 格式文档，天然适合非结构化、文本类数据。
 
三、核心底层原理
1. 倒排索引（灵魂）
    - MySQL：正排索引 → 行→字段
    - ES：倒排索引 → 分词词 → 对应文档ID
    - 优势：模糊搜索、全文检索速度碾压数据库
2. 分词器
    把一句话拆成关键词，例如：
     计算机专业  →  计算机、专业 
    中文检索必须配置中文分词器 ik
 
四、Docker 一键部署（直接复制即用）
bash

# ES 单节点 开发版
docker run -d \
--name es \
-p 9200:9200 \
-p 9300:9300 \
-e "discovery.type=single-node" \
-e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
elasticsearch:7.17.0
 
- 访问地址： http://localhost:9200 
- 9200：HTTP 客户端端口
- 9300：集群通信端口
 
五、必掌握 6 大基础操作（DSL语句）
1. 创建索引 + 映射
    json
    PUT /user_index
    {
  "mappings": {
    "properties": {
      "id":{"type":"integer"},
      "name":{"type":"text"},
      "desc":{"type":"text"}
    }
  }
    }
 
2. 新增文档
    json
    POST /user_index/_doc
    {
  "id":1,
  "name":"张三",
  "desc":"计算机专业学生"
    }
 
3. 根据ID查询
    json
    GET /user_index/_doc/1
 
4. 全文模糊检索（最常用）
    json
    GET /user_index/_search
    {
  "query":{
    "match":{
      "desc":"计算机"
    }
  }
    }
 
5. 条件精确查询
    json
    GET /user_index/_search
    {
  "query":{
    "term":{
      "id":1
    }
  }
    }
 
6. 删除索引
    json
    DELETE /user_index
 
 
六、核心查询分类（面试+开发必背）
1. match：全文分词模糊查询（文本搜索）
2. term：精确匹配（数字、关键字）
3. bool：多条件组合  must/should/must_not 
4. aggregation：分组、求和、统计
5. highlight：搜索结果关键词高亮
 
七、Java 整合（SpringBoot 日常用法）
1. 核心依赖
    xml
    <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
    </dependency>
 
2. 配置地址
    yaml
    spring:
  elasticsearch:
    uris: http://localhost:9200
 
3. 两种开发方式
    - 方式1：RestHighLevelClient 原生 DSL（企业主流）
    - 方式2：ElasticsearchRepository 极简CRUD（快速开发）
 
八、ES 什么时候用？什么时候不用？
✅ 适合
- 全文检索、模糊搜索、海量文本
- 日志、订单、文章、商品大数据量查询
- 分词、语义检索、复杂聚合
    ❌ 不适合
- 强事务、强一致性、金融交易
- 小数据量简单查（直接 MySQL 就行）
 
九、极简背诵总结
1. ES 基于倒排索引，擅长全文检索
2. 结构：索引→文档→字段
3. 核心：分词 + DSL 查询
4. 业务场景：搜索、日志、大数据量模糊查询
5. Java 项目标配：MySQL 存业务数据，ES 负责搜索
