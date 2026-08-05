# Java与Spring Data Neo4j
> 官方驱动、Spring Data Neo4j 映射、Spring Boot 集成、向量搜索与 AI 应用：Java 生态使用 Neo4j 的完整指南。

---

## 📚 目录

1. [官方驱动](#1-官方驱动)
2. [Spring Data Neo4j](#2-spring-data-neo4j)
3. [实体映射](#3-实体映射)
4. [Repository 模式](#4-repository-模式)
5. [事务与批量](#5-事务与批量)
6. [AI 与向量集成](#6-ai-与向量集成)

---

## 1. 官方驱动

### 1.1 依赖与连接

```xml
<!-- Maven：官方驱动（neo4j-java-driver） -->
<dependency>
    <groupId>org.neo4j.driver</groupId>
    <artifactId>neo4j-java-driver</artifactId>
    <version>6.x.x</version>   <!-- v6.0+ 支持原生 Vector 类型 -->
</dependency>
```

```java
// 基础连接
Driver driver = GraphDatabase.driver(
    "bolt://localhost:7687",
    AuthTokens.basic("neo4j", "password"));

// 连接配置（连接池/超时）
Config config = Config.builder()
    .withMaxConnectionPoolSize(50)
    .withConnectionTimeout(5, TimeUnit.SECONDS)
    .withMaxTransactionRetryTime(10, TimeUnit.SECONDS)
    .build();
Driver driver = GraphDatabase.driver(uri, authToken, config);
```

### 1.2 驱动 API

| API | 用途 |
|-----|------|
| driver.session() | 会话（事务载体） |
| session.run() | 自动提交查询 |
| session.executeRead/Write | 函数事务（推荐） |
| session.beginTransaction() | 手动事务 |
| Result | 结果迭代（Record） |
| BookmarkManager | 因果一致性（书签） |

```java
// 标准查询模式（参数化 + 函数事务）
try (Session session = driver.session()) {
    List<String> names = session.executeRead(tx -> {
        Result result = tx.run(
            "MATCH (p:Person {name: $name})-[:FRIEND]->(f) " +
            "RETURN f.name ORDER BY f.name",
            Map.of("name", "张三"));
        return result.list(r -> r.get("f.name").asString());
    });
}
// 参数化：$name（防注入）
// executeRead/Write：自动事务管理 + 重试
```

---

## 2. Spring Data Neo4j

### 2.1 依赖与配置

```xml
<!-- Spring Boot + Spring Data Neo4j -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-neo4j</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: password
    pool:
      max-connection-pool-size: 50
```

```text
Spring Data Neo4j（SDN）：
  实体映射（@Node/@Relationship）
  Repository 接口（方法命名查询）
  事务集成（@Transactional）
  版本：SDN 7.x（Spring Boot 3.x）

特点：
  Cypher 生成（方法名 → 查询）
  自定义查询（@Query 注解）
  与 Spring 生态无缝集成
```

### 2.2 配置类

```java
@Configuration
public class Neo4jConfig {
    // 自动配置已处理大部分（application.yml）
    // 可选：自定义转换器/审计
    @Bean
    public Neo4jMappingContext neo4jMappingContext() {
        return new Neo4jMappingContext();
    }
}
```

---

## 3. 实体映射

### 3.1 节点映射

```java
@Node("Person")
public class Person {
    @Id @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Property("age")
    private Integer age;

    // 关系映射
    @Relationship(type = "FRIEND", direction = Direction.OUTGOING)
    private List<Person> friends;

    @Relationship(type = "ACTED_IN", direction = Direction.OUTGOING)
    private List<Movie> actedIn;
}
```

| 注解 | 作用 |
|------|------|
| @Node | 节点（Label 名） |
| @Relationship | 关系映射（类型/方向） |
| @Property | 属性映射（属性名） |
| @Id / @GeneratedValue | 主键（内部 ID 生成） |
| @RelationshipProperties | 关系实体（带属性） |

### 3.2 关系属性映射

```java
// 关系实体（关系带属性的场景）
@RelationshipProperties
public class ActedIn {
    @RelationshipId
    private Long id;

    @Property("role")
    private String role;

    @TargetNode
    private Movie movie;    // 目标节点
}

// 使用
@Relationship(type = "ACTED_IN", direction = Direction.OUTGOING)
private List<ActedIn> actedIn;
```

### 3.3 映射注意

```text
映射要点：
  双向关系：一端声明（避免循环序列化）
  懒加载：关系默认懒加载（@Lazy 可选）
  深度控制：避免过深的关系加载（性能）
  ID 策略：@GeneratedValue（内部）或业务 ID

常见问题：
  循环引用（A↔B）→ @JsonIgnore 或 DTO
  大关系集合 → 分页/限制加载
  → 生产用 DTO 而非实体直接返回
```

---

## 4. Repository 模式

### 4.1 方法命名查询

```java
public interface PersonRepository extends Neo4jRepository<Person, Long> {

    // 方法命名 → Cypher 自动生成
    Optional<Person> findByName(String name);

    List<Person> findByAgeGreaterThan(int age);

    List<Person> findByFriendsName(String friendName);
    // 关系路径查询：friends.name

    // 自定义 Cypher（@Query）
    @Query("MATCH (p:Person {name: $name})-[:FRIEND*1..3]->(f) " +
           "RETURN DISTINCT f")
    List<Person> findFriendsWithin(String name, int maxDepth);
}
```

### 4.2 自定义查询

```java
@Repository
public interface GraphQueries extends Neo4jRepository<Person, Long> {

    // 聚合查询
    @Query("MATCH (p:Person)-[:FRIEND]->(f) " +
           "RETURN p.name AS name, count(f) AS friendCount " +
           "ORDER BY friendCount DESC LIMIT 10")
    List<FriendCount> findTopFriends();

    // 路径查询（返回自定义 DTO）
    @Query("MATCH path = shortestPath((a:Person {id: $a})-[:FRIEND*..5]->(b:Person {id: $b})) " +
           "RETURN [n IN nodes(path) | n.name] AS path")
    List<String> findShortestPath(Long a, Long b);
}
```

### 4.3 Repository 注意

```text
方法命名规则：
  findByName（属性）
  findByFriendsName（关系路径：friends.name）
  findByAgeGreaterThan（比较）
  countBy.../existsBy...

性能注意：
  方法命名查询生成 Cypher（简单场景）
  复杂查询用 @Query（精确控制）
  深关系查询谨慎（加载深度）
```

---

## 5. 事务与批量

### 5.1 事务管理

```java
@Service
public class PersonService {

    @Transactional
    public void createWithFriends(Person p, List<String> friendNames) {
        personRepo.save(p);                    // 写节点
        for (String name : friendNames) {
            Person f = personRepo.findByName(name).orElseGet(() -> {
                Person np = new Person();
                np.setName(name);
                return personRepo.save(np);
            });
            p.getFriends().add(f);             // 关系写入（同事务）
        }
        personRepo.save(p);
    }
    // @Transactional：整个方法一个事务（原子性）
}
```

### 5.2 批量写入

```java
// 批量导入（大数据量）
// ① 原生驱动（最快）
try (Session session = driver.session()) {
    session.executeWrite(tx -> {
        for (BatchItem item : items) {
            tx.run("MERGE (n:Node {id: $id}) SET n.value = $value",
                   Map.of("id", item.id, "value", item.value));
        }
        return null;
    });
    // 注意：单事务批量 vs 分批事务（内存控制）
}

// ② 分批事务（推荐大导入）
//    每批 1000 条一个事务（CALL ... IN TRANSACTIONS 或应用层分批）
```

### 5.3 并发与冲突

```text
并发写：
  驱动自动重试（事务冲突重试）
  冲突热点：减少并发写同一节点

工程建议：
  写密集场景分批/队列
  冲突重试参数（maxTransactionRetryTime）
  读多写少（图数据库常见模式）无压力
```

---

## 6. AI 与向量集成

### 6.1 向量存储（LangChain4j / Spring AI）

```xml
<!-- LangChain4j Neo4j 集成 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-neo4j</artifactId>
</dependency>
```

```java
// LangChain4j：Neo4j 作为向量存储
Neo4jEmbeddingStore store = Neo4jEmbeddingStore.builder()
    .basicAuth("neo4j", "password")
    .dimension(1024)
    .build();

// 写入（文档 embedding）
store.add(Embedding.from(floatArray), "doc-1");

// 检索（相似度 Top-K）
List<EmbeddingMatch<Embedded>> matches =
    store.findRelevant(Embedding.from(queryVector), 5);
```

### 6.2 Cypher 向量操作（Java）

```java
// 原生驱动执行向量查询（SEARCH 语法）
try (Session session = driver.session()) {
    List<Map<String, Object>> results = session.executeRead(tx -> {
        Result r = tx.run(
            "SEARCH docs " +
            "WHERE docs.embedding <=> $query > 0.7 " +
            "AND docs.author = $author " +          // 索引内过滤
            "RETURN docs.title, docs.embedding <=> $query AS score " +
            "ORDER BY score DESC LIMIT 10",
            Map.of("query", queryVector, "author", "张三"));
        return r.list(record -> record.asMap());
    });
}
// 注意：SEARCH 需要 Cypher 25 + 驱动 v6.0+
```

### 6.3 GraphRAG 应用模式（Java）

```text
Java 侧 GraphRAG 的组件：
  ① embedding：调用模型 API（OpenAI/本地）→ float[]
  ② 写入：实体 MERGE + 切片 embedding（批量）
  ③ 检索：SEARCH（向量）+ MATCH（图扩展）
  ④ 生成：LLM 调用（Spring AI / LangChain4j）

典型流程（Java 服务）：
  ① 用户问题 → embedding
  ② SEARCH 向量召回（Top-N 切片）
  ③ MATCH 图扩展（实体/关系/社区）
  ④ 上下文组装 → LLM 生成回答

工具链：
  Spring AI（LLM 调用）
  LangChain4j（RAG 抽象 + Neo4j 向量存储）
  自定义 Cypher（图检索工具）
```

> 🎯 **核心要点**：Java 集成 = 官方驱动（Bolt + 参数化 + 函数事务）+ Spring Data Neo4j（@Node 映射 + Repository）+ 事务/批量管理。向量与 AI：驱动 v6.0+ 支持原生 Vector 与 SEARCH 语法；LangChain4j/Spring AI 提供向量存储与 RAG 抽象。工程纪律：**参数化查询防注入、DTO 而非实体返回、批量分批事务、SEARCH + MATCH 混合检索**。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| 官方驱动？ | neo4j-java-driver（v6+ 支持 Vector） |
| 实体映射？ | @Node/@Relationship/@Property |
| Repository？ | 方法命名查询 + @Query 自定义 |
| 事务？ | @Transactional + executeRead/Write |
| 批量导入？ | 分批事务（IN TRANSACTIONS） |
| AI 集成？ | LangChain4j 向量存储 + SEARCH 混合检索 |

**下一模块**：[08-部署运维与选型](08-部署运维与选型.md)　**返回总览**：[00-Neo4j知识体系总览](00-Neo4j知识体系总览.md)
