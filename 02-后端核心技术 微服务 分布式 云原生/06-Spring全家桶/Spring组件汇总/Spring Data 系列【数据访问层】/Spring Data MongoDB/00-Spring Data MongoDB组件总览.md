# 00 Spring Data MongoDB 组件总览

> 组件卡片：Spring Data MongoDB 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，MongoDB 引擎本身的深挖见 [MongoDB 深度体系（01-06）](../../../../02-非关系型数据库/MongoDB/01-MongoDB核心概念与安装配置.md)

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

**Spring Data MongoDB 是 Spring Data 家族中面向 MongoDB 的数据访问组件**——用 Repository 派生方法、`MongoTemplate` 模板和注解映射（`@Document`/`@Field`）把 MongoDB 的 BSON 文档操作封装成"类 JPA 的编程体验"，是 Java 生态对接 MongoDB 8.x 的标准姿势。

```text
核心心智模型：
  实体类（@Document/@Id/@Field 注解）
    ├── Repository 接口：方法名派生查询 / @Query JSON / @Aggregation 管道
    ├── MongoTemplate：Query/Criteria 链式查询、Update、聚合、索引、GridFS
    └── MongoOperations 接口族：事务、批量、向量检索
            ↓
  MongoClient（mongodb-driver-sync 5.6.x，同步 / -reactive 响应式）
            ↓
  MongoDB 8.3 集群（文档存储 + 聚合管道 + 事务 + 全文/向量检索）
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Data 家族（spring-data-mongodb） |
| 版本线 | 2026.0 发行列车 → 4.1.x（2026-08 当前线） |
| 对接引擎 | MongoDB 8.3（2026-05 GA，AI-native）；兼容矩阵测试 6.x~8.x |
| 底层驱动 | mongodb-driver-sync / -reactive 5.6.x（Boot BOM 管理） |
| 配套 | Spring Framework 7.0.x / Spring Boot 4.1.x |
| 定位 | POJO 中心的文档数据访问层：映射、Repository、模板、聚合、事务、向量检索 |

### 1.1 Spring Data MongoDB 解决什么问题

直接使用 MongoDB 原生驱动写数据访问，开发者要面对三件事：**样板 CRUD 代码**（insert/find/update/delete 每集合写一遍）、**文档与 POJO 的转换手写**（Document ↔ 实体、ObjectId ↔ String、类型转换）、**查询/更新/聚合的 BSON 手拼**（易错、不可编译检查）。Spring Data MongoDB 把这三件事收编为声明式能力：注解定义映射、方法名生成查询、模板自动转换。

| 维度 | 原生 mongodb-driver | Spring Data MongoDB |
|------|---------------------|---------------------|
| 基础 CRUD | 手写 Document 操作 | 继承 `MongoRepository` 免费获得 |
| 查询表达 | BSON Document 手拼 | 方法名派生、@Query JSON、Criteria 链式 |
| 结果映射 | 手写 Document → POJO | 自动映射（MappingMongoConverter） |
| 聚合 | 手写管道 Document 数组 | @Aggregation 注解 / TypedAggregation |
| 与 Spring 生态 | 无集成 | Repository、审计、事件、Boot 自动配置 |

> 🎯 判断标准一句话：**"业务代码里有没有 POJO ↔ BSON 的转换和重复查询逻辑？"**——有，用 Spring Data MongoDB；没有（纯运维脚本/数据管道），直接用原生驱动更轻。

### 1.2 适用与不适用场景

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| 文档型业务数据（用户画像、订单、日志、内容） | ✅ | Repository + 聚合管道是主战场 |
| 模式灵活/快速迭代的业务 | ✅ | 无固定表结构，加字段不加列 |
| 高写入吞吐（日志、埋点、IOT） | ✅ | WiredTiger + 分片集群 |
| 向量检索 / AI 语义搜索（2026 主流） | ✅ | 4.x 起 $vectorSearch 全链路支持（[08 篇](08-向量检索与AI集成速查.md)） |
| 强事务、强一致、复杂关联 | ⚠️ 部分 | 事务仅副本集多文档；**关联用内嵌/引用，不支持 join**（$lookup 有限） |
| 复杂 SQL 报表、多表 join 查询 | ❌ | 关系型语义请用 [Spring Data JPA](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/00-Spring%20Data%20JPA组件总览.md) |
| 精确金额结算（DECIMAL 语义） | ⚠️ 部分 | 用 Decimal128 支持但不如关系库审计完备 |

> ⚠️ **最大认知误区**：把 MongoDB 当"关系型数据库"用——没有外键/join/强 schema，事务范围受限（副本集内多文档）。**数据模型要按"文档思维"设计**（内嵌优先、按查询建模），照搬 MySQL 表结构设计会在关联与一致性上处处碰壁（[07 篇](07-索引与性能优化速查.md) 设计模式专节）。

### 1.3 与其他 Spring Data 组件的定位差异

| 组件 | 存储 | 数据模型 | 事务 | 本系列的差异点 |
|------|------|---------|:---:|--------------|
| Spring Data JPA | 关系型数据库 | 表/实体/关联 | ✅ | 有外键、有事务、有强 schema |
| Spring Data Redis | Redis | K/V、集合 | ⚠️ 有限 | 无索引、无查询 DSL |
| **Spring Data MongoDB** | **MongoDB 8.3** | **文档 + 内嵌/引用 + 向量** | ✅（副本集多文档） | **文档模型、聚合管道、无 join、动态 schema** |
| Spring Data Elasticsearch | ES 9 | 文档 + 倒排索引 + 向量 | ❌ | 查询是 JSON DSL，映射动态 |
| Spring Data R2DBC | 关系型数据库 | 表/实体 | ✅ | 响应式非阻塞，同库可互转 |

> 💡 本系列定位"查得快"——组件速查；Repository 设计哲学（派生方法、Pageable、审计）与 Spring Data JPA 同源（同级目录），会 JPA 的开发者上手本组件几乎零门槛；差异集中在：**映射注解不同、查询是 JSON/Criteria 而非 JPQL、无关联懒加载、有聚合管道**。

## 2. 版本现状（2026-08）

| 发行列车 | Spring Data MongoDB | MongoDB 服务端 | Spring Framework | Spring Boot | 状态 |
|----------|--------------------|----------------|-----------------|-------------|------|
| **2026.0** | **4.1.x（4.1.0，2026 GA）** | **8.3（2026-05）** | 7.0.x | **4.1.x** | **Current（当前线）** |
| 2025.1 | 4.0.x | 8.0/8.1 | 7.0.x | 4.0.x | Stable（配 Boot 4.0.x） |
| 2025.0 | 3.5.x | 7.x/8.0 | 6.2.x | 3.5.x | 停止维护 |
| 2024.1 | 3.4.x | 7.x | 6.1.x | 3.4.x | 停止维护 |
| 2024.0 | 3.3.x | 7.x | 6.1.x | 3.3.x | 停止维护 |
| 2021.2 | 3.3.x（Raj 起改名） | 5.x/6.x | 5.3.x | 2.7.x | EOL |

> ⚠️ **版本策略（2026 起）**：MongoDB 8.x 是当前主线（8.3 为 AI-native 版本，性能较 8.0 提升 45% 读/35% 写）；**新项目直接 Boot 4.1 + SDM 4.1 + MongoDB 8.x**；存量 Boot 3.5 项目停留在 3.5.x 且已停止维护——升级需 Java 21（同 JPA 4.x 线的迁移约束，见 [09 篇](09-集成地图与常见问题.md)）。

### 2.1 4.x 线关键变化（3.5 → 4.0 迁移要点）

| 变化 | 说明 |
|------|------|
| Java 21 + Spring Framework 7 | 与全家族同步（同 [JPA 4.x 线](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/00-Spring%20Data%20JPA组件总览.md)） |
| **向量检索全链路** | 4.0 起：`@VectorSearch` 仓库注解（预览转正）、VectorIndex/SearchIndexOperations、VectorSearchOperation 聚合 API（[08 篇](08-向量检索与AI集成速查.md)） |
| 类型安全属性路径 | 4.1 新特性：`Query`/`Criteria`/`Update` 支持类型安全属性路径（告别字符串属性名） |
| 批量 API 修订 | 4.1：多集合写操作、`MongoOperations.bulkWrite()`（insert/update/delete 混合一条调用） |
| JSpecify 空值注解 | 全家族统一（影响 Kotlin/IDE 静态检查） |
| 空值处理 | 默认不写 null 字段（`@Field(nullValue = NullHandling.SKIP)`）语义保持 |

### 2.2 MongoDB 8.x 关键演进（服务端侧）

| 版本 | 里程碑 |
|------|--------|
| 6.x/7.x | 时间序列集合、可查询加密（QE）、Search 索引、列压缩 |
| 8.0（2024-10） | 性能大幅提升；`$vectorSearch` 增强；Atlas 统一体验 |
| 8.1/8.2（2025） | 索引与查询优化、安全增强 |
| **8.3（2026-05）** | **AI-native**：45% 读/35% 写吞吐提升；`$rankFusion`（RRF 混合检索）正式化；Atlas 自动 Voyage 嵌入（预览）；LangGraph 长期记忆集成 |

> 💡 服务端能力的窗口：**`$vectorSearch` 需要 Search/Vector 索引（Atlas 或 Enterprise 的 mongot）**——社区版自建需评估；`$rankFusion` 同理。开发/测试可用本地 mongot 或降级到普通 kNN 计算（[08 篇](08-向量检索与AI集成速查.md) 有对比）。

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| 实体映射 | 集合/字段/主键映射 | `@Document`、`@Id`、`@Field`、`@DBRef` |
| 索引管理 | 单字段/复合/TTL/地理/向量索引 | `@Indexed`、`@CompoundIndex`、SearchIndexOperations |
| 仓库抽象 | 声明式数据访问 | `MongoRepository`、`ReactiveMongoRepository` |
| 派生查询 | 方法名生成查询 | `findByXxxRegex`、`findByXxxExists`、`findTop5ByXxx` 等 |
| 自定义查询 | JSON 直写 / 聚合管道 | `@Query`（JSON）、`@Aggregation` |
| 模板操作 | 编程式查询入口 | `MongoTemplate`（Query/Criteria/Update/Aggregation） |
| 聚合分析 | 管道/分组/连接 | `Aggregation`（group/match/$lookup/$unwind） |
| 事务 | 副本集多文档事务 | `@Transactional`（MongoTransactionManager） |
| 写关注/读关注 | 数据安全级别 | `@WriteConcern`、`@ReadPreference` |
| 批量操作 | 多集合批量写 | 4.1 `bulkWrite()`、BulkOperations |
| 向量检索 | 语义搜索 / 混合检索 | `@VectorSearch`、VectorSearchOperation、`$rankFusion` |
| 辅助能力 | 审计、乐观锁、GridFS、验证 | `@CreatedDate`、`@Version`、`GridFsTemplate`、`@Valid` |

### 3.1 能力边界：Spring Data MongoDB 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| 存储引擎/副本集/分片 | MongoDB 集群 | 本组件只做数据访问，不参与集群管理 |
| join 与复杂关联 | 设计时规避 | 用内嵌/引用 + 聚合 $lookup（有限）；复杂关系数据选 JPA |
| 全文/向量索引内核 | mongot（Atlas/Enterprise） | 组件只负责建索引与查询翻译 |
| 嵌入向量生成 | Spring AI / LangChain4j / 自建 | MongoDB 只存向量并检索（Atlas 自动嵌入除外） |
| 分布式事务（跨库/跨副本集） | Seata / 本地消息表 | MongoDB 事务限于副本集内多文档 |

> ⚠️ **常见归因错误**：查询慢怪"Spring Data MongoDB 不好用"——MongoDB 查询性能由索引设计（$match 前置、索引覆盖）、文档模型（内嵌 vs 引用）决定，组件只负责翻译查询；排查路径应是"explain 走没走索引 → 查询模式对不对 → 数据模型要不要改"。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-快速开始与连接配置速查 | [MongoDB-01 核心概念与安装配置](../../../../02-非关系型数据库/MongoDB/01-MongoDB核心概念与安装配置.md) |
| 03-实体映射与文档注解速查 | [MongoDB-02 数据类型与CRUD操作](../../../../02-非关系型数据库/MongoDB/02-MongoDB数据类型与CRUD操作.md) |
| 04/05-查询与聚合速查 | [MongoDB-03 索引与聚合操作](../../../../02-非关系型数据库/MongoDB/03-MongoDB索引与聚合操作.md) |
| 06-批量、事务与写关注速查 | [MongoDB-04 副本集与分片集群](../../../../02-非关系型数据库/MongoDB/04-MongoDB副本集与分片集群.md) |
| 07-索引与性能优化速查 | [MongoDB-03 索引与聚合](../../../../02-非关系型数据库/MongoDB/03-MongoDB索引与聚合操作.md)、[MongoDB-04 副本集与分片](../../../../02-非关系型数据库/MongoDB/04-MongoDB副本集与分片集群.md) |
| 09-集成地图与常见问题 | [MongoDB-06 SpringBoot集成与实战场景](../../../../02-非关系型数据库/MongoDB/06-SpringBoot集成MongoDB与实战场景.md) |

> 💡 分工约定：**速查页回答"API 怎么写"，深度页回答"引擎怎么工作"**——文档模型、副本集/分片原理、备份恢复在 MongoDB 深度体系；本系列聚焦"POJO ↔ 文档 ↔ 查询的映射与编排"。

## 5. 快速上手 3 步

**① 引入依赖**（Boot 4.1 + MongoDB 8.x，BOM 管理版本）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

**② 配置连接**（application.yml）：

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/shop        # 标准连接串（含认证：mongodb://user:pass@host:port/db）
      auto-index-creation: true                  # 开发期自动按 @Indexed 建索引；生产建议关闭
```

**③ 声明实体 + Repository + 使用**：

```java
@Document(collection = "product")
public record Product(
        @Id String id,                           // String 自动映射 ObjectId
        String name,
        BigDecimal price,
        @Field("category") String category) {}   // @Field 指定字段名

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByNameContaining(String keyword);      // 派生查询 → {name: {$regex: ...}}
    Page<Product> findByCategoryAndPriceBetween(String category,
            BigDecimal lo, BigDecimal hi, Pageable pageable);
}

// 使用
Page<Product> page = productRepository.findByCategoryAndPriceBetween(
        "手机", BigDecimal.valueOf(1000), BigDecimal.valueOf(5000),
        PageRequest.of(0, 10, Sort.by("price").descending()));
```

### 5.1 快速上手补充：本地起一个 MongoDB 8

```bash
# Docker 单节点（开发足够；生产见 MongoDB 深度体系 04 篇 副本集/分片）
docker run -d --name mongo8 -p 27017:27017 mongo:8.0
```

> 💡 事务提示：**多文档事务需要副本集**（单节点需 `--replSet` 初始化）；开发期只想试 CRUD 用单节点即可，测事务再起副本集（[06 篇](06-批量操作与写关注速查.md)）。

### 5.2 三步验证集成真的通了

| 步骤 | 做法 | 预期 |
|------|------|------|
| ① 看启动日志 | 观察索引自动创建日志 | `created index ...` 无异常 |
| ② 写读验证 | save 一条 → findById | 返回同一实体，id 已回填 |
| ③ 控制台核对 | `mongosh shop` 查 `db.product.find()` | 文档存在，字段与实体一致 |

> 💡 排障起点：**先确认 MongoDB 通不通（mongosh 或 mongo shell），再查 Java 侧**——`Timed out after 30000 ms` 一般是集群没起或 uri 配错，不是组件问题。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | artifact 坐标、包结构、依赖边界与职责切割 |
| [02-快速开始与连接配置速查](02-快速开始与连接配置速查.md) | starter、连接串、MongoClientSettings、认证、Boot 属性、多集群 |
| [03-实体映射与文档注解速查](03-实体映射与文档注解速查.md) | @Document/@Id/@Field/@DBRef、ObjectId 映射、类型转换、验证 |
| [04-Repository 速查](04-Repository速查.md) | 仓库层次、派生方法关键词全表、@Query JSON、@Aggregation、分页 |
| [05-查询构造与模板速查](05-查询构造与模板速查.md) | MongoTemplate、Criteria 链式、Query/Update、聚合管道、投影 |
| [06-批量、事务与写关注速查](06-批量操作与写关注速查.md) | bulkWrite、写关注/读关注、多文档事务、@Version 乐观锁 |
| [07-索引与性能优化速查](07-索引与性能优化速查.md) | 索引类型、explain、慢查询、内嵌 vs 引用、分片、设计模式 |
| [08-向量检索与 AI 集成速查](08-向量检索与AI集成速查.md) | $vectorSearch、VectorIndex、@VectorSearch、混合检索 RRF、Spring AI 联动 |
| [09-集成地图与常见问题](09-集成地图与常见问题.md) | 与全家桶联动、3.5→4.1 迁移、高频坑与排错 |

### 6.1 阅读顺序建议

- **第一次接触**：03 → 04 → 05，先掌握"映射、仓库、模板"三件套；
- **项目实战**：02（连接）→ 03（映射）→ 04（Repository）→ 05（模板/聚合）→ 07（索引）→ 09（避坑）；
- **准备面试**：07（内嵌 vs 引用/索引）→ 05（聚合管道）→ 06（事务/写关注）→ 04（派生方法）；
- **AI 应用开发**：08 篇直接是向量检索落地章节，配合 [RAG 知识体系总览](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/00-RAG知识体系总览.md)；
- **升级迁移**：09 篇迁移清单 + 02 篇配置变化。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| MongoDB 核心概念（文档/集合/索引） | [MongoDB-01](../../../../02-非关系型数据库/MongoDB/01-MongoDB核心概念与安装配置.md) | 理解文档模型与查询语义的前提 |
| CRUD 与数据类型 | [MongoDB-02](../../../../02-非关系型数据库/MongoDB/02-MongoDB数据类型与CRUD操作.md) | @Query 的 JSON 语法基础 |
| 聚合管道 | [MongoDB-03](../../../../02-非关系型数据库/MongoDB/03-MongoDB索引与聚合操作.md) | @Aggregation 的管道语义 |
| Spring Data 通用模型 | Spring Data JPA 系列（同级目录） | Repository/Pageable/审计同源 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会写 CRUD | 00 总览 → 02 连接 → 03 映射 → 04 Repository |
| 项目实践 | 上生产做文档业务 | 03 映射 → 04 Repository → 05 模板 → 07 索引 → 09 避坑 |
| AI/向量检索 | RAG 落地 | 02 → 03 → 08 向量检索 → 09（配合 [RAG 知识体系总览](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/00-RAG知识体系总览.md)） |
| 面试冲刺 | 全考点 | 07 数据模型 → 05 聚合 → 06 事务 → 08 向量（2026 高频）→ 04 派生方法 |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| @Document | 实体 ↔ 集合绑定（collection 指定集合名） |
| @Id | 主键映射（String/ObjectId/UUID/自定义） |
| @Field | 字段名映射（BSON 字段 ↔ Java 属性） |
| @DBRef | 文档引用（**慎用**：不级联、无事务保证，优先内嵌） |
| MongoRepository | 声明式仓库接口（派生方法自动翻译查询） |
| MongoTemplate | 编程式模板（查询/更新/聚合/索引/批量全入口） |
| Criteria | 链式条件构造（is/gt/regex/in/exists） |
| @Query | Repository 方法上的 JSON 查询（?0 占位 / :name 命名） |
| @Aggregation | 方法级聚合管道（group/match/$lookup） |
| $vectorSearch | 向量检索聚合阶段（8.x 语义搜索核心） |
| 写关注 | 写入确认级别（W:1/majority，牺牲性能换安全） |
| 多文档事务 | 副本集内跨文档原子性（@Transactional 支持） |
| @Version | 乐观锁版本（CAS 更新防并发覆盖） |
| GridFS | 大文件存储（>16MB 文档上限） |
| ObjectId | 12 字节文档主键（时间+机器+进程+自增） |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Data 2026.0.0-M1 released（官方博客）](https://spring.io/blog/2026/02/13/spring-data-2026-0-0-m1-released)、[Spring Data 2026.0.0 generally available（thenote）](https://thenote.app/post/en/spring-data-2026-0-0-generally-available-w6vcearl6t)、[Maven Central: spring-data-mongodb 4.1.0](https://mvnrepository.com/artifact/org.springframework.data/spring-data-mongodb/4.1.0)、[MongoDB 8.3 新特性（官方）](https://www.mongodb.com/products/updates/mongodb-8-3/)、[Spring Data MongoDB: Vector Search 官方博客](https://www.mongodb.com/company/blog/product-release-announcements/spring-data-mongodb-now-with-vector-search-queryable-encryption)
