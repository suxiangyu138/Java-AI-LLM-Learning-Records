# ElasticSearch 完整知识体系（后端面试/学习完整版）

> 基于 Lucene 的分布式全文检索引擎，提供 RESTful API，广泛用于全文搜索、日志分析、商品检索与指标分析。

### 一、基础核心概念

- `001` 定位与底层：ES 基于 Lucene，Java 开发，RESTful API
- `002` 适用场景：全文搜索、日志存储（ELK）、商品检索、指标分析、多维筛选
- `003` 不适合场景：海量事务、强一致性数据库、高频更新小文档
- `004` 核心名词：集群 Cluster、节点 Node（Master / Data / Coordinating / Ingest）
- `005` 索引 Index、类型 Type（ES7+ 废弃）、文档 Document、字段 Field
- `006` 分片 Shard：主分片 Primary Shard、副本分片 Replica Shard
- `007` 倒排索引（底层核心）：正排与倒排对比、Term 词典、倒排表、FST / DocValues / FieldData

### 二、Mapping 字段映射

- `008` 核心字段类型：字符串（text / keyword）
- `009` 数值、日期、布尔类型
- `010` 复合类型：object 与 nested
- `011` 特殊类型：geo 地理、ip、range
- `012` 核心参数：analyzer、index、doc_values、store、dynamic
- `013` 动态映射 vs 手动 Mapping

### 三、分词器 Analyzer

- `014` 分词器三组件：Character Filter、Tokenizer、Token Filter
- `015` 常用分词器：standard、ik（ik_max_word / ik_smart）、whitespace、pattern

### 四、文档 CRUD（REST API）

- `016` 写入：单条 PUT/POST、批量 Bulk API
- `017` 写入流程：内存缓冲区 → refresh → segment → translog → flush → merge
- `018` 读取：GET 精准查询、_search 检索
- `019` 更新与删除：标记删除、segment 合并时物理清理

### 五、检索 Query DSL

- `020` 两大查询分类：查询上下文 query、过滤上下文 filter
- `021` 基础查询：match、match_phrase、term、terms、range、exists
- `022` bool 复合查询：must、filter、should、must_not
- `023` 聚合 Aggs：桶聚合（terms / date_histogram / range）
- `024` 指标聚合（avg / sum / max / min / cardinality）与管道聚合
- `025` 排序、分页（from+size / search_after / scroll）、高亮

### 六、写入底层原理

- `026` 写入流程：路由计算、主分片写入、副本同步、refresh、flush、merge
- `027` 关键机制：translog、refresh_interval、segment 不可变性

### 七、集群分布式机制

- `028` 分片路由：路由公式固定，主分片创建后不可修改
- `029` Master 选举：基于 Zen Discovery，过半节点投票当选
- `030` 分片分配策略：扩容缩容自动迁移、分配过滤与水位控制
- `031` 脑裂问题：旧版隐患与解决方案（minimum_master_nodes），ES7 自动优化

### 八、性能优化

- `032` 写入优化：批量 bulk、增大 refresh_interval、临时调整副本数
- `033` 禁用动态 mapping、使用 numeric 类型、堆内存配置
- `034` 查询优化：filter 前置、避免深分页、聚合用 keyword、合理分片数
- `035` 开启 doc_values、_source 过滤
- `036` 集群优化：节点角色分离、磁盘水位控制、定期清理与冷归档

### 九、高可用与容灾

- `037` 副本分片自动提升、translog 持久化
- `038` 快照 snapshot、CCR 跨集群复制

### 十、ELK 技术栈配套

- `039` ElasticSearch：存储检索
- `040` Logstash：数据清洗与管道转换
- `041` Filebeat：轻量日志采集
- `042` Kibana：可视化与监控面板
- `043` Ingest Pipeline：ES 内置预处理

### 十一、常见问题与面试考点

- `044` ES 为什么近实时而非实时（refresh 机制延迟）
- `045` 为什么 text 不能排序聚合（无 doc_values）
- `046` 副本作用（容灾、分担读压力）
- `047` 深度分页问题（from+size 累加导致 OOM）
- `048` scroll 与 search_after 区别
- `049` 倒排索引优缺点（检索快、写入慢）
- `050` nested 和 object 区别（object 数组扁平化导致关联错乱）
- `051` 索引删除数据不会立即消失（segment 不可变，merge 清理）
- `052` ES 事务支持（单文档原子性，多文档无分布式事务）

### 十二、Java 客户端体系

- `053` High Level REST Client（主流）
- `054` 核心操作：索引创建、Bulk 批量、Bool 查询、聚合、Scroll 分页
- `055` 集成 Spring Boot：spring-data-elasticsearch 封装简化 CRUD
