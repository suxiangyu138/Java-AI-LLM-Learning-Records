# 00 Spring Data Elasticsearch 组件总览

> 组件卡片：Spring Data Elasticsearch 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，ES 引擎本身的深挖见 [04-ELK/Elasticsearch 系列](../../../../04-ELK/Elasticsearch/01-ES核心概念与安装配置.md)

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring Data Elasticsearch 是 Spring Data 家族中面向 Elasticsearch 的数据访问组件**——用 Repository 派生方法、`ElasticsearchOperations` 模板和注解映射（`@Document`/`@Field`）把 ES 的 JSON DSL 封装成"类 JPA 的编程体验"，是 Java 生态对接 ES 8/9 的标准姿势。

```text
核心心智模型：
  实体类（@Document/@Field 注解）
    ├── Repository 接口：方法名派生查询 / @Query JSON / @SearchTemplateQuery
    ├── ElasticsearchOperations：CriteriaQuery / StringQuery / NativeQuery
    └── IndexOperations：索引创建、映射管理、别名
            ↓
  ElasticsearchClient（elasticsearch-java 9.x，底层 Rest5Client）
            ↓
  Elasticsearch 集群（倒排索引 + 向量检索 + 聚合）
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Data 家族（spring-data-elasticsearch） |
| 版本线 | 2026.0 发行列车 → 6.1.x（2026-08 当前线） |
| 对接引擎 | Elasticsearch 9.x（6.x 线）；5.5.x 及更早对接 ES 8.x |
| 底层客户端 | elasticsearch-java 9.x（ElasticsearchClient + Rest5Client） |
| 配套 | Spring Framework 7.0.x / Spring Boot 4.x |
| 定位 | POJO 中心的数据访问层：索引映射、Repository、模板查询、聚合、向量检索 |

### 1.1 Spring Data Elasticsearch 解决什么问题

直接使用 ES 原生 Java 客户端写查询，开发者要面对三件事：**JSON 查询手写与拼接**（易错、不可编译检查）、**结果反序列化手写**（GetSource → Gson/Jackson 模板代码）、**索引映射与实体脱节**（mapping JSON 与 POJO 各改各的）。Spring Data Elasticsearch 把这三件事收编为声明式能力：注解定义映射、方法名生成查询、模板自动反序列化。

| 维度 | 原生 elasticsearch-java 客户端 | Spring Data Elasticsearch |
|------|-------------------------------|--------------------------|
| 查询表达 | JSON 字符串 / 链式 builder | 方法名派生、@Query、Criteria、NativeQuery |
| 结果映射 | 手写反序列化 | 自动映射 POJO（ElasticsearchEntityMapper） |
| 索引管理 | 手写 mapping/settings JSON | @Document/@Field/@Mapping/@Setting 注解 |
| 分页排序 | 手写 from/size/sort | Pageable / Sort 与 Spring Data 统一 |
| 与 Spring 生态 | 无集成 | Repository、审计、事件、Boot 自动配置 |
| 适合 | 复杂运维脚本、API 全量透传 | 业务 CRUD + 搜索 + 聚合的 Java 服务 |

> 🎯 判断标准一句话：**"业务代码里有没有 POJO ↔ ES 文档的转换和重复查询逻辑？"**——有，用 Spring Data Elasticsearch；没有（纯脚本/运维），直接用原生客户端更轻。

### 1.2 适用与不适用场景

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| 商品/内容搜索、全文检索 | ✅ | 派生方法 + @Query 是主战场 |
| 日志/指标聚合分析 | ✅ | SearchOperations + 聚合 API |
| 向量检索 / RAG 知识库（2026 主流） | ✅ | dense_vector 字段 + NativeQuery kNN（5.3.1+） |
| 需要强事务/强一致性的核心数据 | ❌ | ES 无事务、近实时（refresh 延迟），事务性数据用 MySQL |
| 高度定制的 ES 高级 API | ⚠️ 部分 | NativeQuery 可透传，但不如原生客户端直接 |
| 复杂地理/嵌套递归查询 | ⚠️ 部分 | Criteria 支持有限，走 NativeQuery/自定义实现 |

> ⚠️ **最大认知误区**：把 ES 当"主数据库"用。ES 是搜索/分析引擎，不是事务数据库——写入近实时（默认 refresh 1s）、无 ACID、更新是"删除+重建"，业务主数据存 MySQL/PostgreSQL，ES 只做索引副本。

### 1.3 与其他 Spring Data 组件的定位差异

| 组件 | 存储 | 数据模型 | 事务 | 本系列的差异点 |
|------|------|---------|:---:|--------------|
| Spring Data JPA | 关系型数据库 | 表/实体/关联 | ✅ | 有外键、有事务，ES 全无 |
| Spring Data Redis | Redis | K/V、集合 | ⚠️ 有限 | 无索引、无查询 DSL |
| Spring Data MongoDB | MongoDB | 文档 | ⚠️ | 有聚合管道，无倒排索引/向量检索（Mongo 8 起有部分） |
| **Spring Data Elasticsearch** | **ES 9** | **文档 + 倒排索引 + 向量** | ❌ | 查询表达式是 JSON DSL，映射是动态的 |

> 💡 本系列定位"查得快"——组件速查；Repository 设计哲学（派生方法、Pageable、审计）与 Spring Data JPA 同源（同级目录另有 Spring Data JPA 系列），会 JPA 的开发者上手本组件几乎零门槛。

## 2. 版本现状（2026-08）

| 发行列车 | Spring Data ES | Elasticsearch | Spring Framework | 状态 |
|----------|---------------|---------------|-----------------|------|
| **2026.0** | **6.1.x** | **9.4.2** | 7.0.x | **Current（当前线，配 Boot 4.1.x）** |
| 2025.1 | 6.0.x（6.0.6） | 9.2.2 | 7.0.x | Stable（配 Boot 4.0.x） |
| 2025.0 | 5.5.x | 8.18.1 | 6.2.x | 停止维护 |
| 2024.1 | 5.4.x | 8.15.5 | 6.1.x | 停止维护 |
| 2024.0 | 5.3.x | 8.13.4 | 6.1.x | 停止维护 |
| 2021.2（Raj） | 4.4.x | 7.17.3 | 5.3.x | 停止维护 |

> ⚠️ **版本策略（2026 起）**：ES 官方已全面转向 9.x，Spring Data 6.x 线只对接 ES 9；**存量 ES 8.x 项目停留在 5.5.x 且已停止维护**——若必须用 ES 8，考虑升级集群到 9.x 或评估 [SQLite 向量搜索](../../../../01-关系型数据库/SQLite/00-SQLite知识体系总览.md) 等替代，别在死线上再投新功能。

### 2.1 6.x 线关键变化（5.5.x → 6.0.x 迁移要点）

| 变化 | 说明 |
|------|------|
| 默认客户端换成 Rest5Client | ES Java client 9+ 的底层传输客户端；旧 RestClient 弃用，移入 `org.springframework.data.elasticsearch.client.elc.rest_client` 包，配置回调改用 `org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients` |
| 空值注解换 JSpecify | 编译期空值检查从 JetBrains 注解迁到 JSpecify（影响 Kotlin/IDE 静态检查工具） |
| @Setting 支持 SpEL | `settingPath` 参数可用 SpEL 表达式动态取路径 |
| UpdateQuery 类型修正 | `ifSeqNo` / `ifPrimaryTerm` 从 `Integer` 改为 `Long`（对齐 ES 客户端） |
| ScriptType 枚举移除 | 改用 `ScriptData` record 区分内联/存储脚本 |
| unfreeze() 移除 | ES 9 不再支持索引解冻，`ReactiveElasticsearchIndicesClient.unfreeze()` 删除 |

### 2.2 关键演进时间线

| 版本 | 里程碑 |
|------|--------|
| 3.2.x（Moore） | TransportClient 时代，Jackson 映射 |
| 4.0.x（Neumann） | 移除 Jackson ObjectMapper → ElasticsearchEntityMapper；Operations 拆分（Document/Search/Index） |
| 4.4.x（Raj） | 引入新 elasticsearch-java 客户端 + NativeQuery；删除 TransportClient 模板 |
| 5.0.x（Turing） | 全面转向 ELC（Elasticsearch Java Client） |
| 5.3.x（2024.0） | NativeQuery 支持 kNN 搜索（`withKnnSearches`） |
| 5.4.x（2024.1） | dense_vector 增强：elementType、knnSimilarity、knnIndexOptions |
| 6.0.x（2025.1） | 默认 Rest5Client；JSpecify；对接 ES 9.2 |
| 6.1.x（2026.0） | 对接 ES 9.4；当前主线 |

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| 实体映射 | 索引文档映射 | `@Document`、`@Id`、`@Field`、`@Mapping`、`@Setting` |
| 索引管理 | 索引/映射/别名生命周期 | `IndexOperations` |
| 仓库抽象 | 声明式数据访问 | `ElasticsearchRepository`、`ReactiveElasticsearchRepository` |
| 派生查询 | 方法名生成 DSL | `findByXxxContaining`、`findByPriceBetween` 等 24 个关键词 |
| 自定义查询 | JSON DSL 直写 | `@Query`（?0 占位 / SpEL）、`@SearchTemplateQuery` |
| 模板操作 | 编程式查询入口 | `ElasticsearchOperations`（CriteriaQuery/StringQuery/NativeQuery） |
| 聚合分析 | 桶/指标聚合 | NativeQuery + `Aggregation` |
| 向量检索 | kNN / 混合检索 | dense_vector 字段 + `withKnnSearches` |
| 响应式 | 非阻塞全链路 | `ReactiveElasticsearchOperations` |
| 批量/滚动 | 大数据量吞吐 | bulk、scroll、PointInTime、search_after |
| 辅助能力 | 审计、实体回调、路由 | `@CreatedDate`、EntityCallbacks、`@Document(routing)` |

### 3.1 能力边界：Spring Data Elasticsearch 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| 倒排索引/分词/评分 | Elasticsearch 引擎 | 本组件只"翻译"查询和映射，不参与索引结构 |
| 集群管理（分片/副本/滚动升级） | ES 集群 / 运维 | IndexOperations 只做单索引生命周期 |
| 中文分词 | IK / 官方 ICU 插件 | 需在 ES 侧安装插件（[见 03 篇映射](03-实体映射与索引注解速查.md)） |
| 向量嵌入生成 | Spring AI / LangChain4j / 自建 | ES 只存向量并检索，不做嵌入 |
| 事务 / 分布式一致性 | MySQL/Seata | ES 无事务语义，本组件也不假装有 |

> ⚠️ **常见归因错误**：搜索不准怪"Spring Data 不好用"——搜索质量由分词器、mapping、评分模型决定，组件只负责把查询送到引擎；排查路径应是"DSL 对不对 → 分词对不对 → 索引结构对不对"。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-快速开始与连接配置速查 | [04-ELK-01 ES核心概念与安装配置](../../../../04-ELK/Elasticsearch/01-ES核心概念与安装配置.md) |
| 03-实体映射与索引注解速查 | [04-ELK-03 ES索引与映射管理](../../../../04-ELK/Elasticsearch/03-ES索引与映射管理.md) |
| 04-Repository 速查 | [04-ELK-04 ES查询DSL详解](../../../../04-ELK/Elasticsearch/04-ES查询DSL详解.md) |
| 05-查询构造与 Operations 速查 | [04-ELK-04 ES查询DSL详解](../../../../04-ELK/Elasticsearch/04-ES查询DSL详解.md) |
| 06-聚合、滚动与批量速查 | [04-ELK-05 ES聚合分析](../../../../04-ELK/Elasticsearch/05-ES聚合分析.md) |
| 08-集成地图与常见问题 | [04-ELK-06 ES集群高可用与性能优化](../../../../04-ELK/Elasticsearch/06-ES集群高可用与性能优化.md) |

> 💡 分工约定：**速查页回答"API 怎么写"，深度页回答"引擎怎么工作"**——分词器选型、评分原理、集群参数在 04-ELK 系列；本系列所有示例的 DSL 语义都可以去 [04-ES查询DSL详解](../../../../04-ELK/Elasticsearch/04-ES查询DSL详解.md) 溯源。

## 5. 快速上手 3 步

**① 引入依赖**（Boot 4.x + ES 9，`spring-boot-dependencies` BOM 管理版本）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

**② 配置连接**（application.yml）：

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200        # 集群地址，逗号分隔
    username: elastic                  # 本地开发若开启安全，填认证
    password: changeme
```

**③ 声明实体 + Repository + 使用**：

```java
@Document(indexName = "product")
public record Product(@Id String id,
                      @Field(type = FieldType.Text, analyzer = "ik_max_word") String name,
                      @Field(type = FieldType.Keyword) String category,
                      @Field(type = FieldType.Double) double price) {}

public interface ProductRepository extends ElasticsearchRepository<Product, String> {
    List<Product> findByNameContaining(String keyword);      // 派生查询 → match
    Page<Product> findByCategoryAndPriceBetween(String category, double lo, double hi, Pageable pageable);
}

// 使用
Page<Product> page = productRepository.findByCategoryAndPriceBetween(
        "手机", 1000, 5000, PageRequest.of(0, 10, Sort.by("price").descending()));
```

### 5.1 快速上手补充：本地起一个 ES 9 集群

```bash
# Docker 单节点（开发足够；生产见 04-ELK 集群篇）
docker run -d --name es9 -p 9200:9200 -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" docker.elastic.co/elasticsearch/elasticsearch:9.4.2
```

> 💡 ES 9 镜像默认启用安全（xpack），开发环境显式关掉安全或配置 API key 均可；生产必须启用 TLS + 认证（[08 篇常见问题](08-集成地图与常见问题.md) 有认证排错清单）。

### 5.2 三步验证集成真的通了

| 步骤 | 做法 | 预期 |
|------|------|------|
| ① 看启动日志 | 观察启动时索引初始化 | `index ... created` 或 mappings 写入无异常 |
| ② 写读验证 | save 一条 → findById | 返回同一文档（注意 1s refresh 延迟） |
| ③ 控制台核对 | 用 Kibana/curl 查 `_cat/indices` | 索引存在，文档数 +1 |

> 💡 排障起点：**先确认 ES 通不通（curl 9200），再查 Java 侧**——`Connection refused` 一般是集群没起或 uris 配错，不是组件问题。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | artifact 坐标、包结构、依赖边界与职责切割 |
| [02-快速开始与连接配置速查](02-快速开始与连接配置速查.md) | starter、ClientConfiguration、Rest5Client、SSL/认证、Boot 属性 |
| [03-实体映射与索引注解速查](03-实体映射与索引注解速查.md) | @Document/@Field/@Mapping/@Setting、字段类型、索引生命周期 |
| [04-Repository 速查](04-Repository速查.md) | 派生方法关键词全表、@Query、@SearchTemplateQuery、分页 |
| [05-查询构造与 Operations 速查](05-查询构造与Operations速查.md) | ElasticsearchOperations、Criteria/String/Native 三类查询、高亮 |
| [06-聚合、滚动与批量速查](06-聚合滚动与批量速查.md) | 聚合、滚动、PIT、search_after、bulk 批量 |
| [07-向量检索与 RAG 集成速查](07-向量检索与RAG集成速查.md) | dense_vector、kNN、混合检索、与 Spring AI/RAG 联动 |
| [08-集成地图与常见问题](08-集成地图与常见问题.md) | 与全家桶联动、IK 分词、高频坑与排错 |

### 6.1 阅读顺序建议

- **第一次接触**：03 → 04 → 05，先掌握"映射、仓库、模板"三件套；
- **项目实战**：02（连接）→ 03（映射）→ 04（Repository）→ 05（模板）→ 08（避坑）；
- **准备面试**：05（三类查询区别）→ 04（派生方法）→ 07（向量检索，2026 高频）→ 03（映射原理）；
- **AI 应用开发**：07 篇直接是 RAG 落地章节，配合 [RAG 知识体系总览](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/00-RAG知识体系总览.md)；
- **源码学习**：01 篇的包结构 + 05 篇的查询执行链路，两条线贯通全流程。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| ES 核心概念（倒排索引/分片/分词） | [04-ELK-01](../../../../04-ELK/Elasticsearch/01-ES核心概念与安装配置.md) | 理解索引与映射语义的前提 |
| 查询 DSL（match/term/bool/range） | [04-ELK-04](../../../../04-ELK/Elasticsearch/04-ES查询DSL详解.md) | @Query/NativeQuery 的 JSON 语法基础 |
| Spring Data 通用模型 | Spring Data JPA 系列（同级目录） | Repository/Pageable/审计同源 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会写 CRUD | 00 总览 → 02 连接 → 03 映射 → 04 Repository |
| 项目实践 | 上生产做搜索 | 03 映射 → 04 Repository → 05 模板 → 06 聚合 → 08 避坑 |
| AI/向量检索 | RAG 落地 | 02 → 03 → 07 向量检索 → 08（配合 [RAG 知识体系总览](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/00-RAG知识体系总览.md)） |
| 面试冲刺 | 全考点 | 05 三类查询 → 04 派生方法 → 03 映射 → 07 向量 → 01 版本线 |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| @Document | 实体 ↔ 索引绑定（indexName、createIndex、aliases） |
| @Field | 字段 ↔ mapping 类型映射（Text/Keyword/Dense_Vector/Date...） |
| ElasticsearchRepository | 声明式仓库接口（派生方法自动翻译 DSL） |
| ElasticsearchOperations | 编程式模板（Document/Search/Index 三操作域） |
| CriteriaQuery | 链式条件查询（is/contains/between，类型安全） |
| NativeQuery | 直传 ELC 原生 Query 对象（可透传聚合/kNN/高亮） |
| @Query | Repository 方法上的 JSON DSL（?0 占位 / SpEL） |
| IndexOperations | 索引生命周期管理（创建/映射/别名/刷新） |
| dense_vector | 向量字段类型（kNN 检索，RAG 的地基） |
| scroll / PIT / search_after | 深分页三种方案（深分页禁止用 from+size） |
| refresh | 写入可见延迟（默认 1s，近实时语义的根源） |
| ElasticsearchEntityMapper | 实体映射器（4.0 起替代 Jackson） |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Data Elasticsearch 官方版本矩阵（6.1.0）](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/versions.html)、[6.0 What's new](https://docs.spring.io/spring-data/elasticsearch/reference/6.0/elasticsearch/elasticsearch-new.html)、[5.5 → 6.0 迁移指南](https://docs.spring.io/spring-data/elasticsearch/reference/6.0/migration-guides/migration-guide-5.5-6.0.html)、[Spring Data Elasticsearch 6.0.3 Release（GitHub）](https://github.com/spring-projects/spring-data-elasticsearch/releases/tag/6.0.3)、[Spring Data 2026.0.0 GA（thenote）](https://thenote.app/post/en/spring-data-2026-0-0-generally-available-w6vcearl6t)
