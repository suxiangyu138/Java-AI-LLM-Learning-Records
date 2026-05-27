# MongoDB Java 整合 SpringBoot（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | SpringBoot 集成 MongoDB 完整方案
> **版本**：SpringBoot 2.x/3.x | MongoDB 6.x/7.x
> **核心场景**：用户画像存储、日志收集、内容管理、IoT 数据

---

## 一、依赖与配置

### 1.1 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

无需额外依赖，Spring Data MongoDB 已封装全部能力。

### 1.2 配置文件

```yaml
# application.yml — 单节点
spring:
  data:
    mongodb:
      uri: mongodb://admin:admin123@localhost:27017/mydb

# 副本集
spring:
  data:
    mongodb:
      uri: mongodb://admin:admin123@host1:27017,host2:27017,host3:27017/mydb?replicaSet=rs0&w=majority

# 传统写法
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: mydb
      username: admin
      password: admin123
```

---

## 二、方式一：MongoRepository（极简 CRUD）

### 2.1 实体类

```java
@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Field("name")
    private String name;

    @Indexed(unique = true)     // 建唯一索引
    private String email;

    private Integer age;

    private String city;

    private List<String> tags;

    @CreatedDate                   // 自动填充创建时间
    private LocalDateTime createTime;

    @LastModifiedDate              // 自动填充更新时间
    private LocalDateTime updateTime;
}
```

### 2.2 启用审计

```java
@Configuration
@EnableMongoAuditing               // 开启自动填充 @CreatedDate/@LastModifiedDate
public class MongoConfig {
}
```

### 2.3 Repository 接口

```java
public interface UserRepository extends MongoRepository<User, String> {

    // 方法命名查询
    User findByName(String name);

    List<User> findByAgeGreaterThan(int age);

    List<User> findByCityAndAgeBetween(String city, int minAge, int maxAge);

    Page<User> findByCity(String city, Pageable pageable);

    // 数组查询
    List<User> findByTagsContaining(String tag);

    // 模糊搜索（正则）
    List<User> findByNameRegex(String regex);

    // 自定义 Mongo 查询
    @Query("{ 'age': { $gte: ?0, $lte: ?1 } }")
    List<User> findByAgeRange(int min, int max);

    @Query(value = "{ 'city': ?0 }", fields = "{ 'name': 1, 'email': 1 }")
    List<User> findByCityProjected(String city);

    // 更新操作
    @Update("{ '$set': { 'city': ?1 } }")
    @Query("{ 'name': ?0 }")
    long updateCityByName(String name, String city);
}
```

### 2.4 Service 使用

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 保存（有 id 更新，无则插入）
    public User save(User user) {
        return userRepository.save(user);
    }

    // 批量保存
    public List<User> batchSave(List<User> users) {
        return userRepository.saveAll(users);
    }

    // ID 查询
    public User findById(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    // 分页查询
    public Page<User> findByPage(String city, int page, int size) {
        return userRepository.findByCity(
            city,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "age"))
        );
    }

    // 删除
    public void delete(String id) {
        userRepository.deleteById(id);
    }
}
```

---

## 三、方式二：MongoTemplate（灵活操作 — 企业主力）

### 3.1 基础 CRUD

```java
@Service
@RequiredArgsConstructor
public class UserMongoService {

    private final MongoTemplate mongoTemplate;

    // 插入
    public User insert(User user) {
        return mongoTemplate.insert(user);
    }

    // 批量插入
    public Collection<User> batchInsert(List<User> users) {
        return mongoTemplate.insertAll(users);
    }

    // 保存（有 _id 则更新，无则插入）
    public User save(User user) {
        return mongoTemplate.save(user);
    }

    // 查询
    public List<User> findByAgeGte(int age) {
        Query query = new Query(Criteria.where("age").gte(age));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        return mongoTemplate.find(query, User.class);
    }

    // 分页查询
    public Page<User> findByPage(String city, int page, int size) {
        Query query = new Query(Criteria.where("city").is(city));
        long total = mongoTemplate.count(query, User.class);
        query.with(PageRequest.of(page, size));
        return new PageImpl<>(mongoTemplate.find(query, User.class),
            PageRequest.of(page, size), total);
    }

    // 返回指定字段
    public List<UserDTO> findNamesByCity(String city) {
        Query query = new Query(Criteria.where("city").is(city));
        query.fields().include("name").include("email").exclude("_id");
        return mongoTemplate.find(query, UserDTO.class);
    }

    // 更新
    public UpdateResult updateCityByName(String name, String city) {
        Query query = new Query(Criteria.where("name").is(name));
        Update update = new Update().set("city", city);
        return mongoTemplate.updateFirst(query, update, User.class);
    }

    // 批量更新
    public UpdateResult incAgeForCity(String city, int inc) {
        Query query = new Query(Criteria.where("city").is(city));
        Update update = new Update().inc("age", inc);
        return mongoTemplate.updateMulti(query, update, User.class);
    }

    // upsert
    public void upsert(User user) {
        Query query = new Query(Criteria.where("email").is(user.getEmail()));
        Update update = new Update()
            .set("name", user.getName())
            .set("age", user.getAge())
            .set("city", user.getCity());
        mongoTemplate.upsert(query, update, User.class);
    }

    // 删除
    public DeleteResult removeByCity(String city) {
        return mongoTemplate.remove(
            new Query(Criteria.where("city").is(city)), User.class
        );
    }
}
```

### 3.2 复杂条件查询

```java
public List<User> complexSearch(SearchParam param) {
    Criteria criteria = new Criteria();

    // 多条件 AND
    if (StrUtil.isNotBlank(param.getCity())) {
        criteria.and("city").is(param.getCity());
    }
    if (param.getMinAge() != null) {
        criteria.and("age").gte(param.getMinAge());
    }
    if (param.getMaxAge() != null) {
        criteria.and("age").lte(param.getMaxAge());
    }

    // OR 条件
    if (CollUtil.isNotEmpty(param.getTags())) {
        criteria.orOperator(
            Criteria.where("tags").in(param.getTags()),
            Criteria.where("specialTag").in(param.getTags())
        );
    }

    // 正则模糊搜索
    if (StrUtil.isNotBlank(param.getKeyword())) {
        criteria.and("name").regex(".*" + param.getKeyword() + ".*");
    }

    Query query = new Query(criteria);

    // 排序
    query.with(Sort.by(
        Sort.Order.desc("age"),
        Sort.Order.asc("name")
    ));

    // 分页
    query.with(PageRequest.of(param.getPage(), param.getSize()));

    return mongoTemplate.find(query, User.class);
}
```

### 3.3 聚合操作

```java
public List<CityStats> getCityStats() {
    // 等价 Shell:
    // db.users.aggregate([{$group: {_id: "$city", count: {$sum: 1}, avgAge: {$avg: "$age"}}}])

    Aggregation agg = Aggregation.newAggregation(
        Aggregation.group("city")
            .count().as("count")
            .avg("age").as("avgAge"),
        Aggregation.sort(Sort.Direction.DESC, "count")
    );

    AggregationResults<CityStats> results = mongoTemplate.aggregate(
        agg, "users", CityStats.class
    );
    return results.getMappedResults();
}
```

```java
// Spring Data 3.x 聚合新写法（Projection）
interface CityStatsProjection {
    String getId();
    int getCount();
    double getAvgAge();
}

public List<CityStatsProjection> getCityStatsV2() {
    Aggregation agg = Aggregation.newAggregation(
        Aggregation.group("city")
            .count().as("count")
            .avg("age").as("avgAge")
    );
    return mongoTemplate.aggregate(agg, "users", CityStatsProjection.class)
        .getMappedResults();
}
```

---

## 四、连接池配置

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mydb
      # 连接池配置（基于 MongoClient Settings）
      # Spring Data MongoDB 默认使用 MongoClients 自动管理

# 或通过 Java Config 自定义：
```

```java
@Configuration
public class MongoClientConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(mongoUri))
            .applyToConnectionPoolSettings(builder -> builder
                .maxSize(100)         // 最大连接数
                .minSize(10)          // 最小连接数
                .maxWaitTime(2, TimeUnit.SECONDS)
                .maxConnectionIdleTime(10, TimeUnit.MINUTES)
            )
            .applyToSocketSettings(builder -> builder
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
            )
            .build());
    }
}
```

---

## 五、测试环境 — 内嵌 MongoDB

```xml
<!-- 用于单元测试，无需安装 MongoDB -->
<dependency>
    <groupId>de.flapdoodle.embed</groupId>
    <artifactId>de.flapdoodle.embed.mongo</artifactId>
    <version>4.10.0</version>
    <scope>test</scope>
</dependency>
```

---

## 六、MongoDB Compass — 可视化管理

官方 GUI 工具，相当于 Navicat for MongoDB：
- 查看数据、索引
- 聚合管道可视化构建
- 查询性能分析
- 免费下载

---

## 七、面试核心要点

1. **SpringBoot 有几种操作 MongoDB 的方式？** MongoRepository（命名查询）+ MongoTemplate（灵活操作）
2. **MongoTemplate 怎么实现 upsert？** `mongoTemplate.upsert(query, update, class)`
3. **聚合在 Java 中怎么做？** `Aggregation` + `AggregationResults`
4. **怎么建唯一索引？** `@Indexed(unique = true)` 注解
5. **连接串中 `w=majority` 什么含义？** Write Concern，多数节点确认

---

## 八、极简总结

```
Repository = 简单 CRUD + 方法命名查询
MongoTemplate = 复杂条件 + upsert + 聚合（企业主力）
Query + Criteria = 构建查询条件
Update = $set / $inc / $push / $unset
聚合 = Aggregation + group / match / sort
生产连接 = mongodb://user:pass@host1,host2/db?replicaSet=rs0&w=majority
```
