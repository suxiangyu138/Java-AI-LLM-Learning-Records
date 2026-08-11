# 06 数据升级：PostgreSQL 与 Milvus

> 第六个加分项：把 Level2 的"演示级存储"升级为"生产级存储"——SQLite 换 PostgreSQL 18（并发与规模）、Chroma 换 Milvus 3.0（湖原生向量库、10 亿级向量毫秒检索）。升级的每一步都是选型决策课，迁移报告本身是简历素材。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [升级决策：何时值得动存储](#2-升级决策何时值得动存储)
3. [PostgreSQL 18：迁移与收益](#3-postgresql-18迁移与收益)
4. [Milvus 3.0：向量库升级](#4-milvus-30向量库升级)
5. [迁移工程：数据迁移与双跑](#5-迁移工程数据迁移与双跑)
6. [升级后的容量与规模](#6-升级后的容量与规模)
7. [常见坑](#7-常见坑)

---

## 1. 目标与验收

本模块的产出：PostgreSQL 18 + Milvus 3.0 替换 SQLite + Chroma，数据迁移脚本，迁移前后对比（功能等价 + 规模能力验证：10 万级向量检索延迟）。验收标准：**全功能在 PostgreSQL 上回归通过**（Level2 07 的测试库换引擎重跑）；**10 万向量下检索 P95 达标**（Milvus 特性验证）；**迁移报告完整**（为什么迁、怎么迁、迁后验证、回滚预案）。版本基线（2026-08）：PostgreSQL 18.4（2026-05 最新稳定版）、Milvus 3.0.0（2026-07-29 发布，湖原生架构）。

## 2. 升级决策：何时值得动存储

存储升级是"收益 vs 成本"的决策，不是技术时髦：**SQLite 的真实上限**——单写者（并发写锁）、单机（无法水平扩展）、数据量（百万行级 OK，更大吃力）——**项目什么时候到这条线**：并发写出现 "database is locked" 频率上升（05 篇多 worker 后）、对话数据量上百万行、需要多人远程访问（SQLite 不适合网络共享）。**升级的三个触发信号**：并发（多 worker 写锁）、规模（数据量级变化）、形态（需要分布式部署/K8s 多副本）。**诚实的决策**：如果项目还在单机演示规模，SQLite + Chroma 完全够用——"**评估后不升级**"同样是合格决策（记录在案）；本模块的升级理由是"为简历与真实规模验证"——把"为什么升级"和"为什么可以升级"都写清楚，面试官要的就是这个决策过程。

## 3. PostgreSQL 18：迁移与收益

PostgreSQL 18 的迁移路径（Level2 04 埋的伏笔兑现——SQLModel 只改 db_url）：**连接串**——`sqlite+aiosqlite:///./data/kbqa.db` → `postgresql+asyncpg://user:pass@localhost:5432/kbqa`（驱动换 asyncpg）；**表结构**——SQLModel 的 metadata 在 PostgreSQL 上重新 create_all（类型自动映射：SQLite 的 TEXT/INTEGER 变 PG 的 VARCHAR/BIGSERIAL——**注意类型差异的隐性 bug**：自增主键、布尔值、时区字段的行为差异）；**迁移工具**——正式引入 Alembic（Level2 04 预告过"数据量上来后引入"——现在数据量到了）：建 alembic.ini、初始 migration（基线表结构）、后续变更走 revision——**"没有 Alembic 的库演进 = 手工改表 + 祈祷"**，从这一刻开始规范化。

```python
# 配置只改一行（Level2 02 的配置管理收益）
db_url: str = "postgresql+asyncpg://kbqa:password@localhost:5432/kbqa"
```

PostgreSQL 的实战收益（相对 SQLite）：**并发写**（MVCC 多版本并发控制——多 worker 写不再锁死）；**全文检索**（tsvector 原生支持——**BM25 关键词检索可以换 PG 的全文检索或 PG 向量扩展，检索架构的一个可选演进**）；**运维成熟度**（备份 pg_dump、监控生态、角色权限）。**迁移验证**：Level2 07 的测试套件换引擎全量重跑（conftest 的引擎 fixture 指向 PG 测试库）——**测试是迁移的保险丝**，没有全绿测试的迁移是盲迁。

## 4. Milvus 3.0：向量库升级

Chroma 换 Milvus 的理由与边界：**Chroma 定位**——单机、嵌入式、Demo/小规模（Level2 的选择正确）；**Milvus 定位**——分布式、生产级、亿级向量：**规模**（10 亿级向量毫秒级 P99 < 10ms 的生产实证）、**高可用**（副本、滚动升级）、**生态**（与 K8s 无缝、prometheus 监控内建）。**Milvus 3.0 的新形态**（2026-07-29 发布）：**湖原生架构**——直接从对象存储（Lance/Iceberg/Parquet）做检索（零拷贝、增量刷新），外部集合（External Collection）直查数据湖；Storage V3、快照、在线加列等。**对项目的影响**：向量检索从"嵌入式依赖"变成"独立基础设施服务"（Docker Compose 加一个服务，生产 K8s 托管——组件化与部署复杂度同步上升，这就是升级的"成本"侧）。

```python
from pymilvus import MilvusClient, DataType

client = MilvusClient(uri=settings.milvus_uri)   # http://localhost:19530
# 建集合（含向量索引 HNSW）
client.create_schema("kbqa_chunks", auto_id=True)
# 插入：复用 Level2 06 的 embed 输出
client.insert("kbqa_chunks", [{"document_id": doc_id, "text": chunk,
                               "vector": embed(chunk)}])
# 检索：与 Chroma query 同构（Level2 06 的接口习惯迁移成本低）
hits = client.search("kbqa_chunks", data=[embed(question)], limit=50)
```

**索引选型**（2026 基线）：HNSW（高召回、内存开销大——默认甜点）与 IVF 系（显存省、调参多）——数据量 10 万级 HNSW 直接够；**混合检索的改造**（Level2 06 的 BM25 + RRF 保留——Milvus 负责向量路，BM25 路独立维护（可换 PG 全文检索），RRF 融合逻辑不动——**检索架构的组件化改造面最小化**）。**Milvus 检索的租户/文档过滤**（08 篇的隔离需求在这里准备）：Milvus 用 partition 或 metadata 过滤实现——建集合时按租户建 partition，检索时 `partition_names=["tenant_3"]`——**"检索层隔离"是 Level3 08 篇的前置设计**，本模块建集合时就把 partition 规划进去，避免二次迁移。

## 5. 迁移工程：数据迁移与双跑

存储升级的工程方法——**迁移不是"切过去"，是"并行走过去"**：**三步迁移法**：**① 双写**（新库与旧库同时写入——新增数据两边都落，期间观察一致性）；**② 双读比对**（读请求走旧库，定时任务比对新旧库结果——随机抽 N 条对话与文档对比，一致率 100% 才继续）；**③ 切换**（读切到新库，保留旧库 N 天回滚窗口）。**向量数据迁移**：Chroma 全量导出 → 重新向量化（或直接迁移向量——**embedding 模型没换就可以直接导向量**，换了必须重新向量化，这是 06 篇的教训）→ Milvus 批量插入（分批 + 幂等（按 document_id 去重——Level2 04 的一致性流程在新库重做一遍））。**回滚预案**：切换后出问题 → 配置改回旧连接串（配置管理三态的好处）→ 旧库数据完整（双写期的数据补同步）。**迁移报告模板**（本模块的交付物）：背景与触发信号、选型对比表（旧/新/备选）、迁移步骤与耗时、验证结果（测试全绿、数据一致率、检索延迟）、回滚预案——**这份报告本身就是简历素材与面试叙事**。

## 6. 升级后的容量与规模

升级后要做"规模验证"（不然升级只是换了名字）：**数据灌入实验**——造 10 万-100 万条文档块（真实数据 + 合成数据混合），验证：**检索延迟**（P95 随数据量增长曲线——10 万级应仍 < 100ms）；**索引构建时间**（全量索引耗时——后续入库策略按此规划）；**吞吐**（并发检索 QPS——与 04 篇压测衔接）。**规模验证的结论写进调优记录**："10 万向量 P95 检索 45ms，QPS 500 无错误"——**"我的项目能处理 10 万级向量"是有数字的**。**升级后的运维**（07 篇衔接）：Milvus 与 PostgreSQL 的监控指标（连接数、延迟、磁盘）进 Prometheus——存储组件从"项目的一部分"变成"基础设施"，运维视角同步升级。

## 7. 常见坑

**为了升级而升级**：数据量 1 万条也上 Milvus——先算账（第 2 节触发信号），"不升级的决策记录"同样是专业。

**类型映射踩雷**：SQLite 的布尔/时间在 PG 行为不同——迁移后全量回归测试（第 3 节），别只测主流程。

**向量迁移换 embedding**：迁移时换了 embedding 模型没重新向量化——新旧向量空间不一致，检索全部失效——迁移与模型升级永远分开做，一次只变一个变量。

**Milvus 部署不当**：单机 Milvus 也需要 etcd/MinIO 等依赖（Docker 起全套）——部署复杂度是升级成本的一部分，compose 文件与服务化（09 篇）统一托管。

**只切不验证**：切到新库不双跑比对——三步迁移法（第 5 节）的第二步不能跳，数据一致性验证是迁移的底线。

**性能没对比**：升级后没记录延迟/吞吐数字——规模验证（第 6 节）必做，升级的价值是数据对比出来的。

> 🎯 **核心要点**：数据升级的本质是**"规模与并发驱动的架构演进，用工程方法完成"**——触发信号判断值不值得动、三步迁移法保证不停机平滑切、规模验证证明升级真实有效。PostgreSQL + Milvus 的组合让项目从"Demo 存储"变成"生产存储"，简历上的含金量是"能处理真实规模数据"。

---

**下一模块**：[07 可观测性全家桶](./07-可观测性全家桶.md) | **返回总览**：[Level3 总览](./00-Level3%20进阶加分项目%20总览.md)

【参考来源】
- [Milvus 3.0.0 Release（2026-07-29）](https://github.com/milvus-io/milvus/releases/tag/v3.0.0)
- [Milvus 3.0 Brings Lake-Native AI Retrieval](https://www.opensourceforu.com/2026/08/milvus-3-0-brings-lake-native-ai-retrieval-to-open-data/)
- [PostgreSQL 18 Version Status](https://pgpedia.info/postgresql-versions/index.html)
- [Milvus 体系（本仓库）](https://github.com/suxiangyu138/Java-AI-LLM-Learning-Records)
