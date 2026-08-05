# 04 - PostgreSQL 扩展生态

> 定位：PG 最大的差异化优势——pgvector 向量检索、PostGIS 地理空间、PL 语言扩展、FDW 外部数据源、时序与全文——PG 不止是关系库

## 📚 目录

1. [pgvector 向量检索](#1-pgvector-向量检索)
2. [PostGIS 地理空间](#2-postgis-地理空间)
3. [PL 语言扩展](#3-pl-语言扩展)
4. [FDW 外部数据源](#4-fdw-外部数据源)
5. [全文搜索与常用扩展](#5-全文搜索与常用扩展)
6. [场景选型：一个 PG 替代多个专用库](#6-场景选型一个-pg-替代多个专用库)

---

## 1. pgvector 向量检索

### 1.1 AI 时代的 PG 杀手级扩展

```sql
-- ① pgvector：向量相似检索（PG 的 AI 场景入口）
CREATE EXTENSION vector;

CREATE TABLE documents (
    id serial PRIMARY KEY,
    content text,
    embedding vector(1536)         -- OpenAI ada-002 维度
);

-- ② 插入向量
INSERT INTO documents(content, embedding)
VALUES ('PostgreSQL 教程', '[0.1, 0.3, ...]'::vector);

-- ③ 余弦相似度检索（Top 5）
SELECT content, 1 - (embedding <=> '[0.1,...]'::vector) AS similarity
FROM documents
ORDER BY embedding <=> '[0.1,...]'::vector
LIMIT 5;

-- ④ 精确检索 + 向量混合（一条 SQL 完成）
--    结构化过滤 + 向量相似 → RAG 场景的标准查询
SELECT content
FROM documents
WHERE category = '技术文档'        -- 结构化过滤
ORDER BY embedding <=> query_vector -- 向量排序
LIMIT 10;
```

> 🎯 **要点**：pgvector = **数据库内混合查询**（结构化过滤 + 向量排序一条 SQL）——对比独立向量库（Milvus）需要两步查询。是 Spring AI RAG 场景的默认向量库选择之一。

### 1.2 pgvector 0.8 实战（HNSW 参数 + 迭代扫描）

```sql
-- ① HNSW 索引（0.5+ 引入，推荐替代 IVFFlat）
CREATE INDEX idx_doc_embed ON documents
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
-- 参数：
--   m = 16（默认）：每个节点连接数，越大召回越高、内存越多
--   ef_construction = 64（默认）：建索引质量，生产 128-200
-- 查询时：SET hnsw.ef_search = 40（默认）→ 调高提召回、调低省延迟

-- ② 迭代扫描（0.8.0+）：解决"过滤后结果太少"问题
SET hnsw.iterative_scan = relaxed_order;   -- 过滤查询自动重扫候选
SET hnsw.max_scan_tuples = 20000;          -- 单次最多扫描元组数

-- ③ 二进制量化（0.8.0+）：存储压缩 32 倍
SELECT binary_quantize('[0.1, 0.2, ...]'::vector);   -- → bit 类型

-- ④ halfvec（FP16 半精度）：存储减半、召回损失 ~1-2%
CREATE TABLE docs (id int, embedding halfvec(1536));
```

| 能力 | pgvector 0.7 | pgvector 0.8（2026 验证） |
|------|:---:|:---:|
| 过滤后重扫 | ❌ 结果可能不足 | ✅ iterative scan |
| 二进制量化 | 手动 | ✅ 内置 binary_quantize |
| halfvec | ✅ | ✅（0.7 引入，0.8 优化） |
| PG 18 支持 | ❌ | ✅ 0.8.1+ |
| 并行建索引 | — | ✅ 0.8 快 40%（需 REINDEX 生效） |

> 💡 **选型经验**：生产常用组合 = `halfvec` 存储 + HNSW 索引 + `ef_search` 动态调参；百万级向量 + 90%+ 召回诉求再考虑量化两阶段（bit 粗排 + 原精度精排）。

### 1.3 pgvector vs 独立向量库

| 维度 | pgvector | Milvus |
|------|:---:|:---:|
| 部署 | ✅ 已有 PG 零额外 | 独立服务 |
| 混合查询 | ✅ SQL 一体化 | 两步 |
| 规模 | 百万级 | 十亿级 |
| 索引 | IVFFlat/HNSW | 多种 ANN |
| 一致性 | ACID | 最终 |

```
⚠️ 面试必答：
"pgvector = 关系库原生向量检索——
 中小规模（百万）零额外部署、
 结构化+向量一条 SQL；
 大规模用 Milvus 等专用库。"
```

---

## 2. PostGIS 地理空间

```sql
-- PostGIS = 地理空间扩展（PG 最著名的扩展）
CREATE EXTENSION postgis;

-- ① 几何计算
SELECT ST_Distance(
    ST_SetSRID(ST_MakePoint(116.38, 39.9), 4326),
    ST_SetSRID(ST_MakePoint(121.47, 31.23), 4326)
) AS distance;   -- 北京到上海距离

-- ② 范围查询（缓冲区）
SELECT name FROM stores
WHERE ST_DWithin(
    location::geography,
    ST_SetSRID(ST_MakePoint(116.38, 39.9), 4326)::geography,
    5000                               -- 5km 内
);

-- ⚠️ PostGIS 让 PG 成为地理信息系统的事实标准
--   (MongoDB/MySQL 均无法匹敌)
```

---

## 3. PL 语言扩展

```sql
-- PG 支持多种存储过程语言（PL 家族）
-- ① PL/pgSQL（默认，最常用）
CREATE FUNCTION greet(name text) RETURNS text AS $$
BEGIN
    RETURN '你好，' || name;
END;
$$ LANGUAGE plpgsql;

-- ② PL/Python（Python 函数）
CREATE EXTENSION plpython3u;
CREATE FUNCTION py_greet(name text) RETURNS text AS $$
    return f'Hello, {name}!'
$$ LANGUAGE plpython3u;

-- ③ PL/Rust（PG 18 热扩展，编译为原生代码）
-- ④ PL/V8（JavaScript）、PL/Java、PL/Perl ...
```

| PL 语言 | 适用 |
|---------|------|
| PL/pgSQL | 默认（数据密集型逻辑） |
| PL/Python | 机器学习/数据处理 |
| PL/Rust | 高性能函数（PG 18 亮点） |
| PL/Java | Java 生态集成 |

---

## 4. FDW 外部数据源

```sql
-- FDW = 外部数据包装器（Foreign Data Wrapper）
-- 把外部数据源（MySQL/Mongo/文件）当作 PG 表查询

-- ① MySQL FDW
CREATE EXTENSION mysql_fdw;
CREATE SERVER mysql_server FOREIGN DATA WRAPPER mysql_fdw
    OPTIONS (host '...', port '3306');
CREATE FOREIGN TABLE remote_users (id int, name text)
    SERVER mysql_server OPTIONS (dbname 'app', table_name 'users');

-- ② 查询外部表（像本地表一样 JOIN）
SELECT u.name, o.total
FROM remote_users u
JOIN local_orders o ON u.id = o.user_id;

-- ⚠️ FDW = 统一查询层（多数据源联邦查询）
```

> 🎯 **要点**：FDW = "PG 作为数据中间层"——统一查询 MySQL/Mongo/文件/API，免应用层聚合。PG 就是数据联邦引擎。

---

## 5. 全文搜索与常用扩展

### 5.1 自带全文搜索

```sql
-- PG 自带 tsvector 全文搜索（免 ES 小场景）
-- 见 02 篇 GIN 索引的全文示例
SELECT * FROM articles
WHERE to_tsvector('english', content) @@ to_tsquery('postgres & tutorial');
```

### 5.2 常用扩展速查

| 扩展 | 功能 |
|------|------|
| pgvector | 向量检索 |
| PostGIS | 地理空间 |
| pg_stat_statements | SQL 性能统计 |
| pgcrypto | 加密函数 |
| uuid-ossp | UUID 生成 |
| pg_partman | 自动分区管理 |
| pg_cron | 定时任务 |
| timescaledb | 时序数据 |
| Citus | 分布式 PG（水平扩展） |

### 5.3 常用扩展实战（pg_partman / pg_cron / pg_stat_statements）

```sql
-- ① pg_partman：自动分区（时间分区免手工 CREATE TABLE）
CREATE EXTENSION pg_partman;
SELECT partman.create_parent(
    p_parent_table => 'public.events',
    p_control      => 'created_at',
    p_interval     => '1 day',
    p_premake      => 7                     -- 预建未来 7 天分区
);
-- 定时调用 run_maintenance() 生成新分区（配合 pg_cron）

-- ② pg_cron：数据库内定时任务（免外部 crontab）
CREATE EXTENSION pg_cron;
SELECT cron.schedule('daily-vacuum', '0 2 * * *',
    'VACUUM (ANALYZE) public.orders');
SELECT cron.schedule('hourly-snapshot', '0 * * * *',
    'CALL public.snapshot_metrics()');
-- ⚠️ pg_cron 适合库内维护任务；应用级调度仍推荐 XXL-Job

-- ③ pg_stat_statements：SQL 性能统计（调优第一扩展）
CREATE EXTENSION pg_stat_statements;
SELECT query, calls, mean_exec_time, rows
FROM pg_stat_statements
ORDER BY mean_exec_time DESC LIMIT 10;      -- 慢 SQL Top10
```

---

## 6. 场景选型：一个 PG 替代多个专用库

| 业务需求 | 专用方案 | PG 方案 | 选型建议 |
|---------|---------|---------|---------|
| 向量检索 | Milvus/Weaviate | pgvector | ≤ 千万级用 PG，更大上专用 |
| 地理空间 | MongoDB 2dsphere | PostGIS | ✅ PG 完胜（精度/函数全） |
| 全文搜索 | Elasticsearch | tsvector + GIN | 小场景（<10GB 文本）PG 够用 |
| 时序数据 | InfluxDB | TimescaleDB | 百万级指标用 PG |
| 分布式 | Citus 集群 | — | 10 亿+ 行再考虑 |
| 多库联邦 | 应用层聚合 | FDW | ✅ 免中间件 |

```
⚠️ 面试必答：
"PG 扩展生态是最大护城河——
 pgvector（AI）、PostGIS（地理）、
 TimescaleDB（时序）、Citus（分布式）、
 FDW（联邦查询）；
 一个 PG 替代多个专用库，
 但'替代'有规模边界（千万向量、PB 级时序仍要专用库）。"
```

---

> 🎯 **核心要点**：PG 扩展 = **pgvector 0.8**（HNSW + 量化 + 迭代扫描）+ **PostGIS**（地理）+ **PL 多语言** + **FDW**（联邦查询）+ **pg_partman/pg_cron**（自动化）+ **pg_stat_statements**（性能）。PG 的定位早已超出"关系数据库"——是数据平台（关系/向量/地理/时序/全文一体化），但有规模边界。

---

**返回总览**：[00-PostgreSQL总览与核心概念](00-PostgreSQL总览与核心概念.md) | **上一篇**：[03-PostgreSQL MVCC与并发控制](03-PostgreSQLMVCC与并发控制.md) | **下一篇**：[05-PostgreSQL与SpringBoot集成](05-PostgreSQL与SpringBoot集成.md)
