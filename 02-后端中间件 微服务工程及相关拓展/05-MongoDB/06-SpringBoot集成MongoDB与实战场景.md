# Spring Boot 集成 MongoDB 与实战场景
> 从依赖配置到 MongoRepository，从聚合管道到 Schema 设计——MongoDB 在 Spring Boot 项目中的完整落地。

## 目录
1. [Spring Boot 基础集成](#1-spring-boot-基础集成)
2. [MongoRepository 极简 CRUD](#2-mongorepository-极简-crud)
3. [MongoTemplate 高级查询](#3-mongotemplate-高级查询)
4. [聚合操作 Java 实现](#4-聚合操作-java-实现)
5. [Schema 设计原则](#5-schema-设计原则)
6. [实战场景：用户画像](#6-实战场景用户画像)
7. [实战场景：内容管理](#7-实战场景内容管理)
8. [实战场景：IoT 时序数据](#8-实战场景iot-时序数据)
9. [实战场景：LBS 地理空间](#9-实战场景lbs-地理空间)
10. [MongoDB 使用时机](#10-mongodb-使用时机)

---

## 1. Spring Boot 基础集成

### Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### 配置文件

```yaml
# 单节点
spring:
  data:
    mongodb:
      uri: mongodb://admin:admin123@localhost:27017/mydb

# 副本集
spring:
  data:
    mongodb:
      uri: mongodb://admin:admin123@host1:27017,host2:27017,host3:27017/mydb?replicaSet=rs0&writeConcern=majority

# 分片集群（连接 Mongos）
spring:
  data:
    mongodb:
      uri: mongodb://mongos1:27017,mongos2:27017/mydb
```

连接池参数通过 URI 查询字符串配置：`?maxPoolSize=100&minPoolSize=10&maxIdleTimeMS=300000`，Spring Boot 2.x+ 已自动管理。

---

## 2. MongoRepository 极简 CRUD

### 2.1 实体类

```java
@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;               // MongoDB _id

    @Field("user_name")
    private String name;             // 字段映射

    private Integer age;
    private String email;
    private List<String> tags;       // 数组字段
    private Address address;         // 嵌套文档
    private LocalDateTime createTime;
}

@Data
public class Address {
    private String city;
    private String street;
    private String zipCode;
}
```

> 💡 `@Field` 可实现 Java 驼峰与 MongoDB 下划线字段名的映射。

### 2.2 Repository

```java
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // 方法名派生查询（无需写实现）
    List<User> findByName(String name);
    List<User> findByAgeBetween(Integer min, Integer max);
    List<User> findByTagsContaining(String tag);
    List<User> findByAddressCity(String city);

    // 排序 + 分页
    List<User> findByAgeGreaterThanOrderByCreateTimeDesc(Integer age);

    Page<User> findByAgeGreaterThan(Integer age, Pageable pageable);

    // 自定义 @Query
    @Query("{ 'address.city': ?0, 'age': { $gte: ?1 } }")
    List<User> findByCityAndMinAge(String city, Integer minAge);

    long countByName(String name);
}
```

方法命名规则：`findBy{Field}{Operator}`，如 `findByNameAndAge` → `{ name: ?, age: ? }`、`findByAgeBetween` → `{ age: { $gte: ?, $lte: ? } }`、`findByNameLike` → `{ name: { $regex: ? } }`。

### 2.4 Service 层调用

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User create(User user) {
        user.setCreateTime(LocalDateTime.now());
        return userRepository.save(user);     // 有 id 则更新（upsert）
    }

    public User findById(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public Page<User> pageQuery(int page, int size) {
        return userRepository.findByAgeGreaterThan(18,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime")));
    }

    public void deleteById(String id) {
        userRepository.deleteById(id);
    }
}
```

---

## 3. MongoTemplate 高级查询

### 3.1 复杂条件查询

```java
@Service
public class UserSearchService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<User> advancedSearch(String keyword, Integer minAge, String city) {
        Criteria criteria = new Criteria();
        if (StringUtils.hasText(keyword))
            criteria.and("name").regex(keyword, "i");
        if (minAge != null)
            criteria.and("age").gte(minAge);
        if (StringUtils.hasText(city))
            criteria.and("address.city").is(city);

        Query query = new Query(criteria)
            .with(Sort.by(Sort.Direction.DESC, "createTime"))
            .skip(0).limit(20);
        return mongoTemplate.find(query, User.class);
    }
}
```

### 3.2 更新操作

```java
@Service
public class UserUpdateService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void addTag(String userId, String tag) {
        mongoTemplate.updateFirst(queryById(userId),
            new Update().push("tags", tag), User.class);
    }

    public void removeTag(String userId, String tag) {
        mongoTemplate.updateFirst(queryById(userId),
            new Update().pull("tags", tag), User.class);
    }

    public void incrementLoginCount(String userId) {
        mongoTemplate.updateFirst(queryById(userId),
            new Update().inc("loginCount", 1), User.class);
    }

    private Query queryById(String id) {
        return new Query(Criteria.where("id").is(id));
    }
}
```

### 3.3 批量操作

```java
@Service
public class BatchService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void bulkInsert(List<User> users) {
        mongoTemplate.insertAll(users);
    }

    public void bulkUpdateAge(List<String> userIds, int newAge) {
        mongoTemplate.updateMulti(
            new Query(Criteria.where("id").in(userIds)),
            new Update().set("age", newAge), User.class);
    }

    public void bulkMixed(List<User> newUsers, List<String> deleteIds) {
        BulkOperations ops = mongoTemplate.bulkOps(BulkMode.ORDERED, User.class);
        newUsers.forEach(ops::insert);
        ops.remove(new Query(Criteria.where("id").in(deleteIds)));
        ops.execute();
    }

    public List<User> findNameAndAgeOnly() {
        Query query = new Query();
        query.fields().include("name", "age").exclude("id");
        return mongoTemplate.find(query, User.class);
    }
}
```

---

## 4. 聚合操作 Java 实现

### 4.1 基础聚合

```java
@Service
public class AggregationService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Map> aggregateByCity() {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("age").gte(18)),   // $match
            Aggregation.group("address.city")                    // $group
                .count().as("userCount")
                .avg("age").as("avgAge"),
            Aggregation.sort(Sort.Direction.DESC, "userCount"),  // $sort
            Aggregation.limit(10)                                // $limit
        );

        return mongoTemplate.aggregate(aggregation, "users", Map.class)
            .getMappedResults();
    }
}
```

### 4.2 $lookup 关联查询

```java
public List<Map> lookupExample() {
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.match(Criteria.where("userId").is("u001")),
        Aggregation.lookup("orders", "userId", "userId", "orders"),
        Aggregation.unwind("orders", false),
        Aggregation.project("name", "email")
            .and("orders.amount").as("orderAmount")
            .and("orders.createTime").as("orderTime"),
        Aggregation.sort(Sort.Direction.DESC, "orderTime"),
        Aggregation.limit(10)
    );

    return mongoTemplate.aggregate(aggregation, "users", Map.class)
        .getMappedResults();
}
```

> ⚠️ `$lookup` 性能远低于关系型 JOIN，尽量用嵌入文档替代。

### 4.3 聚合管道操作符对照

| MQL 操作符 | Spring Data 方法 | 说明 |
|:----------:|------------------|------|
| `$match` | `Aggregation.match()` | 过滤文档 |
| `$group` | `Aggregation.group()` | 分组聚合 |
| `$sort` | `Aggregation.sort()` | 排序 |
| `$limit` | `Aggregation.limit()` | 限制条数 |
| `$skip` | `Aggregation.skip()` | 跳过条数 |
| `$project` | `Aggregation.project()` | 字段投影/重命名 |
| `$unwind` | `Aggregation.unwind()` | 展开数组 |
| `$lookup` | `Aggregation.lookup()` | 左外连接 |
| `$bucket` | `Aggregation.bucket()` | 分桶统计 |
| `$facet` | `Aggregation.facet()` | 多维度聚合 |

---

## 5. Schema 设计原则

### 5.1 嵌入（Embedding）vs 引用（Referencing）

```text
嵌入：
  { name: "张三", address: { city: "北京", street: "长安街" } }

引用：
  User → addressId → Address 集合（$lookup 关联）
```

### 5.2 选择矩阵

| 关系类型 | 推荐策略 | 原因 |
|----------|----------|------|
| 一对一（地址、配置）| 嵌入 | 一次查询拿到全部数据 |
| 一对少（标签、订单项）| 嵌入 | 避免 JOIN 性能开销 |
| 一对多（文章/评论）| 引用 | 避免文档无限增长（16MB 限制）|
| 多对多（用户/角色）| 引用 | 灵活管理 |

### 5.3 反范式设计

```java
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private String userId;
    private String userName;            // 冗余：常用展示字段
    private List<OrderItem> items;
    private Double totalAmount;
    private String shippingAddress;     // 冗余：订单创建时的地址快照
    private LocalDateTime createTime;
}

@Data
public class OrderItem {
    private String productId;
    private String productName;         // 冗余
    private Double price;               // 冗余
    private Integer quantity;
}
```

反范式化的权衡：

| 维度 | 优势 | 劣势 |
|------|------|------|
| 读取性能 | 一次查询，无需关联 | — |
| 写入性能 | — | 冗余字段变更需多处更新 |
| 数据一致性 | — | 存在冗余不一致风险 |
| 复杂度 | 查询简单 | 更新需额外逻辑 |

### 5.4 文档大小控制

MongoDB 单个文档最大 **16MB**。控制策略：
- 使用引用分离大字段（如富文本内容）
- 限制数组长度（如只存最近 100 条行为记录）
- 历史数据定期归档

### 5.5 索引建议

| 查询模式 | 索引策略 |
|----------|----------|
| 等值查询 + 排序 | 复合索引，排序字段放最后 |
| 范围查询 | 范围字段建索引 |
| 全文搜索 | 文本索引 |
| Geo 查询 | 2dsphere 索引 |
| TTL 过期 | TTL 索引 |

---

## 6. 实战场景：用户画像

### 6.1 Schema 设计

用户画像需要存储大量且多变的用户标签——传统关系型数据库需要频繁 DDL，MongoDB 无 Schema 特性天然适合。

```java
@Document(collection = "user_profiles")
public class UserProfile {
    @Id
    private String userId;
    private UserBasics basics;
    private Map<String, Object> tags;            // 多变的标签字段
    private UserMetrics metrics;
    private List<UserBehavior> recentBehaviors;
    private LocalDateTime lastUpdated;
}

@Data
public class UserBasics {
    private String name; private Integer age; private String city;
}

@Data
public class UserMetrics {
    private Integer loginCount; private Double totalSpent;
}

@Data
public class UserBehavior {
    private String action;      // page_view, add_cart, purchase
    private String targetId;
    private LocalDateTime timestamp;
}
```

### 6.2 无 Schema 优势：用户标签各异

```javascript
// 用户 A：{ userId: "1001", tags: { 消费能力: "高", 兴趣: ["科技", "游戏"] } }
// 用户 B：{ userId: "1002", tags: { 职业: "教师", 通勤方式: "地铁" } }
// 用户 C：{ userId: "1003", tags: { 来源渠道: "抖音广告" } }
```

### 6.3 标签查询与更新

```java
@Service
public class ProfileService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<UserProfile> findByTag(String tagKey, Object tagValue) {
        return mongoTemplate.find(
            new Query(Criteria.where("tags." + tagKey).is(tagValue)), UserProfile.class);
    }

    public void setTag(String userId, String tagKey, Object tagValue) {
        mongoTemplate.upsert(new Query(Criteria.where("userId").is(userId)),
            new Update().set("tags." + tagKey, tagValue), UserProfile.class);
    }

    public void recordBehavior(String userId, String action, String targetId) {
        mongoTemplate.upsert(new Query(Criteria.where("userId").is(userId)),
            new Update().push("recentBehaviors").slice(-100)
                .each(new UserBehavior(action, targetId, LocalDateTime.now()))
                .inc("metrics.loginCount", 1), UserProfile.class);
    }
}
```

---

## 7. 实战场景：内容管理

### 7.1 Schema 设计

```java
@Document(collection = "articles")
public class Article {
    @Id
    private String id;
    private String title;
    private String content;
    private String authorId;
    private String authorName;              // 冗余
    private List<String> tags;
    private ArticleMeta meta;
    private List<Comment> recentComments;   // 冗余：最近几条评论
    private String status;                  // draft, published, archived
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}

@Data
public class ArticleMeta {
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
}
```

### 7.2 文本索引与全文搜索

```javascript
// mongosh 创建文本索引（标题权重 10，内容权重 1）
db.articles.createIndex(
  { title: "text", content: "text" },
  { weights: { title: 10, content: 1 } }
)
```

```java
@Repository
public interface ArticleRepository extends MongoRepository<Article, String> {

    @Query("{ $text: { $search: ?0 } }")
    List<Article> searchByText(String keyword);

    @Query(value = "{ $text: { $search: ?0 } }",
           sort = "{ score: { $meta: 'textScore' } }")
    List<Article> searchByTextSorted(String keyword);
}
```

### 7.3 阅读量与热点排行

```java
@Service
public class ArticleService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void incrementViewCount(String articleId) {
        mongoTemplate.updateFirst(
            new Query(Criteria.where("id").is(articleId)),
            new Update().inc("meta.viewCount", 1), Article.class);
    }

    public List<Map> getHotArticles(int limit) {
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("status").is("published")),
            Aggregation.sort(Sort.Direction.DESC, "meta.viewCount"),
            Aggregation.limit(limit),
            Aggregation.project("title", "authorName", "publishTime")
                .and("meta.viewCount").as("views"));
        return mongoTemplate.aggregate(agg, "articles", Map.class).getMappedResults();
    }

    public List<Map> countByTag() {
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.unwind("tags"),
            Aggregation.group("tags").count().as("count"),
            Aggregation.sort(Sort.Direction.DESC, "count"), Aggregation.limit(20));
        return mongoTemplate.aggregate(agg, "articles", Map.class).getMappedResults();
    }
}
```

---

## 8. 实战场景：IoT 时序数据

### 8.1 Schema 设计

```java
@Document(collection = "sensor_data")
@CompoundIndex(def = "{ 'deviceId': 1, 'timestamp': -1 }")
public class SensorData {
    @Id
    private String id;
    private String deviceId;
    private Double temperature;
    private Double humidity;
    private Double pressure;
    private LocalDateTime timestamp;
}
```

> 💡 MongoDB 5.0+ 提供 Time Series 集合，建表时指定 `timeseries: { timeField: "timestamp", metaField: "deviceId", granularity: "seconds" }` 即可自动优化存储。

### 8.2 批量写入与查询

```java
@Service
public class SensorService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void batchInsert(List<SensorData> dataList) {
        BulkOperations ops = mongoTemplate.bulkOps(BulkMode.UNORDERED, SensorData.class);
        ops.insert(dataList);
        ops.execute();
    }

    public SensorData getLatest(String deviceId) {
        Query query = new Query(Criteria.where("deviceId").is(deviceId))
            .with(Sort.by(Sort.Direction.DESC, "timestamp")).limit(1);
        return mongoTemplate.findOne(query, SensorData.class);
    }
}
```

---

## 9. 实战场景：LBS 地理空间

### 9.1 Schema 设计

```java
@Document(collection = "places")
public class Place {
    @Id
    private String id;
    private String name;
    private String category;       // restaurant, hotel, park

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;
}
```

### 9.2 附近查询

```java
@Service
public class LbsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    // 附近的地点
    public List<Place> findNearby(double lon, double lat, double maxKm) {
        NearQuery nearQuery = NearQuery.near(new Point(lon, lat))
            .maxDistance(new Distance(maxKm, Metrics.KILOMETERS))
            .limit(20);

        return mongoTemplate.geoNear(nearQuery, Place.class)
            .getContent().stream()
            .map(GeoResult::getContent)
            .collect(Collectors.toList());
    }

    // 矩形区域
    public List<Place> findWithinBox(double minLon, double minLat,
                                      double maxLon, double maxLat) {
        Query query = new Query(Criteria.where("location")
            .within(new Box(new Point(minLon, minLat), new Point(maxLon, maxLat))));
        return mongoTemplate.find(query, Place.class);
    }

    // 圆形区域
    public List<Place> findWithinCircle(double lon, double lat, double radiusKm) {
        Query query = new Query(Criteria.where("location")
            .within(new Circle(new Point(lon, lat),
                new Distance(radiusKm, Metrics.KILOMETERS))));
        return mongoTemplate.find(query, Place.class);
    }
}
```

---

## 10. MongoDB 使用时机

### 10.1 适合场景

| 场景 | 原因 | 推荐度 |
|------|------|:------:|
| 用户画像（标签多变） | 无 Schema，不同用户不同标签 | ⭐⭐⭐⭐⭐ |
| 内容管理（文章/商品） | 嵌套文档自然表达，字段灵活 | ⭐⭐⭐⭐⭐ |
| IoT 时序数据 | 高吞吐写入，分片集群扩展 | ⭐⭐⭐⭐ |
| LBS 附近的人 | 地理空间索引，原生支持 | ⭐⭐⭐⭐ |
| 日志存储 | JSON 格式，capped collection | ⭐⭐⭐⭐ |
| 实时分析 | 聚合管道 | ⭐⭐⭐ |
| 缓存/会话 | TTL 索引自动过期 | ⭐⭐⭐ |

### 10.2 不适合场景

| 场景 | 原因 | 替代方案 |
|------|------|----------|
| 金融交易（强事务） | 事务能力弱于关系型 | MySQL / PostgreSQL |
| 复杂多层 JOIN | `$lookup` 性能差 | 关系型数据库 |
| 简单 CRUD 后台 | MySQL 更成熟稳定 | MySQL |
| 高并发扣减库存 | 无行级锁 | Redis + MySQL |

### 10.3 技术选型决策树

```text
数据结构是否灵活（字段多变）？
  ├─ 是 → 是否需要 Geo/聚合/数组？
  │      ├─ 是 → MongoDB ✅
  │      └─ 否 → MySQL（简单 CRUD）
  └─ 否 → 数据量预判？
         ├─ 超大（TB 级）→ MongoDB 分片 ✅
         └─ 常规（GB 级）→ MySQL ✅
```

---

## 面试核心

**Q: Spring Boot 集成 MongoDB 的核心依赖？** `spring-boot-starter-data-mongodb`。

**Q: MongoRepository 和 MongoTemplate 的区别？** Repository 适合简单 CRUD（方法名派生查询）；Template 适合复杂查询、聚合和批量操作。

**Q: 嵌入 vs 引用？** 一对少用嵌入（性能好），一对多用引用（避免文档膨胀）。

**Q: MongoDB 适合什么？不适合什么？** 适合用户画像、内容管理、IoT、LBS、日志；不适合强事务、复杂 JOIN、扣减库存。

**Q: 全文搜索实现？** 文本索引（可设权重）+ `$text: { $search: ... }`。

**Q: Geo 查询实现？** Spring Data NearQuery / Box / Circle，底层 2dsphere 索引。

> 🎯 **极简总结**：CRUD 用 Repository，复杂查询用 Template，嵌入优于引用，反范式减少关联，索引加速查询。
