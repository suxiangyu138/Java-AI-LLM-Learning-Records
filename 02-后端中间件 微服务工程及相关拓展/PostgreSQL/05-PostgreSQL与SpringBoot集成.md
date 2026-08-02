# 05 - PostgreSQL 与 Spring Boot 集成

> 定位：Java 与 PG 的最佳实践——JDBC/HikariCP 配置、JPA 适配、JSONB 映射、pgvector 检索、Flyway 迁移、读写分离

## 📚 目录

1. [快速集成](#1-快速集成)
2. [JSONB 映射实战](#2-jsonb-映射实战)
3. [pgvector 集成](#3-pgvector-集成)
4. [Flyway 数据库迁移](#4-flyway-数据库迁移)
5. [读写分离](#5-读写分离)
6. [常见坑与调试速查](#6-常见坑与调试速查)

---

## 1. 快速集成

```xml
<!-- 依赖（与 MySQL 同接口，换驱动即可） -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp
    # ⚠️ PG 18 驱动 URL 常用参数：
    #   ?sslmode=require（生产强制 TLS）
    #   &currentSchema=public（指定 schema）
    #   &stringtype=unspecified（兼容空字符串插入）
    username: ${PG_USER}
    password: ${PG_PASSWORD}
    hikari:
      maximum-pool-size: 20          # 经验：4 × CPU 核数
      minimum-idle: 5
      connection-timeout: 30000
      max-lifetime: 1800000
      # ⚠️ PG 专用：不自动提交（以便使用事务）
      auto-commit: false
  jpa:
    hibernate:
      ddl-auto: validate              # 生产禁用 update（防误改表结构）
    properties:
      hibernate:
        jdbc:
          batch_size: 50              # 批量插入优化（配合 .generator()）
```

```
⚠️ 注意事项：
  ① 驱动类：org.postgresql.Driver（Spring Boot 自动检测）
  ② 默认端口 5432
  ③ Schema：默认 public（相当于 MySQL 的 database）
  ④ 大小写：PG 默认折叠为小写（表名/列名）
  ⑤ JPA 方言：spring.jpa.database-platform 自动检测
  ⑥ 时区：URL 加 serverTimezone=Asia/Shanghai 避免 8 小时偏差
```

```java
// 批量插入性能（PG 支持多值 VALUES，JPA 需开启批处理）
@PersistenceContext
EntityManager em;

// 手动分批 flush（10 万行 ≈ 秒级）
for (int i = 0; i < 100000; i++) {
    em.persist(new User("user-" + i));
    if (i % 500 == 0) { em.flush(); em.clear(); }   // 每 500 条刷一次
}
```

---

## 2. JSONB 映射实战

### 2.1 JSONB vs JSON

```sql
-- JSONB = 二进制 JSON（可索引、查询快）
-- JSON = 纯文本存储（原样保留，查询慢）
-- ⚠️ 生产一律用 JSONB
```

```java
// ① JPA 自动映射（需要 Hibernate Types 库或 PG 方言）
// 方案一：Hibernate Types（推荐）
@Entity
@Table(name = "products")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Product {
    @Id private Long id;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes;    // ⚠️ Map 自动序列化为 JSONB
}

// ② 原生 SQL + JSONB 查询
@Query(value = """
    SELECT * FROM products
    WHERE attributes @> :filter::jsonb   -- JSONB 包含操作符
    """, nativeQuery = true)
List<Product> findByAttributes(@Param("filter") String filter);
```

```java
// ③ 复杂对象映射（List/嵌套对象同样自动序列化）
@Type(type = "jsonb")
@Column(columnDefinition = "jsonb")
private List<SpecItem> specs;          // List<对象> 存为 JSON 数组

// ④ 插入 JSONB 的正确姿势（String → jsonb 转换）
product.setAttributes(Map.of("color", "red", "size", "L"));
// JPA 自动处理：String/Map → JSON 字符串 → jsonb 列
// ⚠️ 不要手动拼 JSON 字符串插入，容易出引号/类型错误

// ⑤ 指定提取某个字段（避免整行查询）
@Query(value = """
    SELECT attributes ->> 'color' AS color, COUNT(*)
    FROM products
    WHERE attributes @> '{"category":"shoes"}'::jsonb
    GROUP BY 1
    """, nativeQuery = true)
List<Object[]> countByColor(@Param("filter") String filter);
```

### 2.2 JSONB 操作符

```sql
-- 常用 JSONB 操作（Java 侧用 @Query native）
attributes -> 'key'         -- 取 JSON 对象字段（返回 jsonb）
attributes ->> 'key'        -- 取文本值（返回 text）
attributes @> '{"color":"red"}'   -- 包含判断（⚠️ 走 GIN 索引）
attributes ? 'key'          -- 键存在
attributes #> '{a,b}'       -- 路径取值（返回 jsonb）
attributes #>> '{a,b}'      -- 路径取值（返回 text）

-- ⚠️ 区分：@> 判断包含（GIN 可索引）；->> 提取值（无索引也可）
-- ⚠️ 中文查询：值须为合法 UTF-8，JSON 内中文无转义问题
```

> 🎯 **要点**：JSONB = 无模式的灵活性 + 索引能力——适合"属性多变"的产品/配置场景。GIN 索引 + `@>` 操作符是查询效率关键；Java 侧 Map/List 自动序列化是最大的开发效率点。

---

## 3. pgvector 集成

```java
// ① 依赖
// implementation 'io.github.jkrasnay:pgvector-spring-boot-starter:1.0+'

// ② 实体映射
@Entity
public class Document {
    @Id private Long id;

    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;                 // ⚠️ PGVector 类型 → float[]
}

// ③ 向量检索（原生 SQL）
@Query(value = """
    SELECT *, 1 - (embedding <=> :query::vector) AS similarity
    FROM documents
    WHERE category = :category
    ORDER BY embedding <=> :query::vector
    LIMIT :limit
    """, nativeQuery = true)
List<Document> searchSimilar(
    @Param("query") String queryVector,
    @Param("category") String category,
    @Param("limit") int limit);

// ④ Spring AI 集成（更简单）
// VectorStore store = new PgVectorStore(dataSource);
// store.similaritySearch(SearchRequest.query(query).withTopK(5));
```

> 🎯 **要点**：pgvector + Spring Boot = AI 应用的轻量向量方案——已有 PG 零额外部署、结构化过滤 + 向量检索一条 SQL。详见 Java AI 体系 03 篇（RAG）。

---

## 4. Flyway 数据库迁移

```yaml
# Flyway 版本管理（同时支持 PG 的 CREATE EXTENSION）
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    # ⚠️ PG 专用配置：CREATE EXTENSION 不能在事务内
    # pgvector/postgis 等扩展需手动创建或使用 Flyway callback
```

```sql
-- V1__init.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,       -- ⚠️ PG 自增（BIGSERIAL = BIGINT + SEQUENCE）
    name VARCHAR(50) NOT NULL,
    attributes JSONB                -- PG 独有类型
);

-- V2__add_vector.sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    embedding vector(1536)
);
CREATE INDEX idx_doc_embedding ON documents USING ivfflat(embedding vector_cosine_ops);
```

```bash
# Flyway 常用命令（Maven 插件 / CLI）
./mvnw flyway:migrate          # 执行未应用的迁移
./mvnw flyway:info             # 查看版本历史
./mvnw flyway:baseline         # 已有库基线化（跳过历史 SQL）
./mvnw flyway:repair           # 修复 checksum 不一致
```

```
⚠️ Flyway + PG 实战注意：
  ① CREATE EXTENSION 不能放事务内 → 单独建库执行 或
     用 flyway 事务模式：spring.flyway.transactional=false
     （迁移含 CREATE EXTENSION 时全局关闭事务包裹）
  ② 已有生产库首次接入 → 先 flyway baseline 再 migrate
  ③ 多环境（dev/test/prod）→ 同一迁移文件，严禁改已发布的 V 文件
  ④ 分区表/大表 DDL 用 BEFORE 钩子避免长锁（PG 15+ 分区 DDL 更快）
```

---

## 5. 读写分离

```java
// PG 读写分离（与 MySQL 相同模式）
// ① 多数据源配置（@Primary master + slave）
// ② dynamic-datasource 库 @DS("slave") 注解
// ③ 或 Pgpool-II（中间件层，透明读写分离）

// PG 的流复制（SR）作为从库延迟更低
// 对比 MySQL：PG 逻辑复制更灵活（可选择性复制）
```

```yaml
# 完整读写分离配置（dynamic-datasource）
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          url: jdbc:postgresql://pg-master:5432/myapp
          driver-class-name: org.postgresql.Driver
          username: ${PG_USER}
          password: ${PG_PASSWORD}
        slave:
          url: jdbc:postgresql://pg-standby:5432/myapp   # 流复制从库
          driver-class-name: org.postgresql.Driver
          username: ${PG_USER}
          password: ${PG_PASSWORD}
          hikari: { read-only: true }      # ⚠️ 从库强制只读
```

```java
// 使用：读方法 @DS("slave")，写方法默认主库
@Service
public class OrderService {
    @DS("slave")
    public List<Order> listRecent(int limit) { ... }   // 读 → 从库

    @Transactional
    public void createOrder(Order order) { ... }        // 写 → 主库
}
```

```sql
-- ⚠️ PG 特有：同步复制配置
-- 主库 postgresql.conf
synchronous_commit = on
synchronous_standby_names = 'slave1'

-- 事务提交需等从库确认（类似 MySQL 半同步）
```

---

## 6. 常见坑与调试速查

| 坑 | 现象 | 解决 |
|----|------|------|
| 时间差 8 小时 | LocalDateTime 读写偏移 | URL 加 `serverTimezone=Asia/Shanghai`；列用 `timestamptz` |
| 表名带引号报错 | `CREATE TABLE "User"` 后找不到 | PG 大小写敏感表名加引号；约定全小写下划线 |
| Boolean 映射异常 | tinyint(1) 习惯不适用 | PG `boolean` 原生类型，无需转换 |
| 空串插不进 | `''` 插入 text NOT NULL 报错 | URL 加 `stringtype=unspecified` |
| 序列跳号 | BIGSERIAL 被回滚消耗 | 正常现象；强连续可改用 `USING local`（PG 10+） |
| 大批量慢 | 逐条 INSERT | JPA 批处理（batch_size + flush 分批）/ COPY |

```
⚠️ 面试必答：
"PG + Spring Boot 集成成本极低——
 驱动一换、方言自动检测；
 JSONB 用 Hibernate Types 映射、
 Flyway 管迁移（BIGSERIAL 自增）、
 dynamic-datasource 做读写分离，
 从 MySQL 迁移基本无痛。"
```

---

> 🎯 **核心要点**：PG + SpringBoot 集成 = **换驱动即可**（与 MySQL 接口一致）+ **JSONB 无模式设计**（Map/List 自动映射 + GIN 索引加速）+ **pgvector 向量**（AI 场景）+ **Flyway 迁移**（BIGSERIAL 自增 + 扩展需免事务）+ **流复制读写分离**（dynamic-datasource 注解路由）。"PG 换 MySQL 成本极低"是技术选型时的重要考量。

---

**返回总览**：[00-PostgreSQL总览与核心概念](00-PostgreSQL总览与核心概念.md) | **上一篇**：[04-PostgreSQL扩展生态](04-PostgreSQL扩展生态.md) | **下一篇**：[06-PostgreSQL性能优化与运维](06-PostgreSQL性能优化与运维.md)
