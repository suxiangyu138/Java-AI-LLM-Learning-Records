# 06-Spring Data数据访问层
> 🎯 Spring Data是Spring生态的统一数据访问抽象层 — 屏蔽不同存储（JPA/Redis/MongoDB）的底层差异，提供一致的Repository编程模型，是从单体到微服务数据层的基石

---

## 目录
1. [本章总览](#1-本章总览)
2. [分层理论讲解](#2-分层理论讲解)
3. [高频踩坑与误区](#3-高频踩坑与误区)
4. [随堂基础练习](#4-随堂基础练习)
5. [章节综合实操案例](#5-章节综合实操案例)
6. [分层综合习题](#6-分层综合习题)
7. [本章复盘速记清单](#7-本章复盘速记清单)
8. [精通拓展补充-P2](#8-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位
- **归属**：Spring全家桶数据访问层 → 层级2 P0核心必学
- **前置依赖**：Spring Boot自动装配 + Spring事务管理 + MySQL/Redis基础
- **重要性**：⭐⭐⭐⭐⭐（几乎所有后端项目都涉及数据访问，Spring Data是官方推荐实践）

### 1.2 三层学习目标

| 级别 | 目标 |
|------|------|
| **基础** | 使用Spring Data JPA完成CRUD，掌握Repository接口命名规则 |
| **熟练** | 掌握JPQL/原生SQL分页排序、@Cacheable缓存、RedisTemplate操作 |
| **精通** | 理解JPA懒加载与N+1问题、自定义Repository实现、多数据源配置 |

### 1.3 本章知识图谱

```
Spring Data 数据访问层
├── Spring Data JPA（核心，占50%）
│   ├── Repository 接口体系（CrudRepository / PagingAndSortingRepository / JpaRepository / JpaSpecificationExecutor）
│   ├── 实体映射（@Entity / @Table / @Column / @Id / @GeneratedValue）
│   ├── 关联关系（@OneToMany / @ManyToOne / @ManyToMany / @JoinColumn / @JoinTable）
│   ├── 查询方式
│   │   ├── 方法命名查询（findByName、findByAgeBetween...）
│   │   ├── @Query JPQL / 原生SQL
│   │   ├── @Modifying 更新/删除
│   │   └── Specification 动态查询
│   ├── 分页排序（Pageable / Sort / Page）
│   └── 事务与懒加载
├── Spring Data Redis（占30%）
│   ├── RedisTemplate / StringRedisTemplate
│   ├── 序列化策略（Jackson2JsonRedisSerializer / StringRedisSerializer）
│   ├── 缓存注解（@Cacheable / @CacheEvict / @CachePut / @Caching）
│   └── 缓存管理器配置
├── Spring Data MongoDB（占10%）
│   ├── MongoRepository
│   └── 基本CRUD
└── 多框架对比（占10%）
    ├── JPA vs MyBatis vs JDBC Template
    └── 选型策略
```

---

## 2. 分层理论讲解

### 2.1 Spring Data统一抽象层

#### 2.1.1 设计理念

Spring Data 的核心设计理念是 **Repository 抽象** — 对任意存储后端提供统一的 CRUD + 分页 + 排序编程模型。

> 💡 Spring Data 不是 ORM 框架，而是一个抽象层。JPA 是底层 ORM 实现（默认 Hibernate），Redis 和 MongoDB 则是通过各自的模板 API 实现。

#### 2.1.2 Repository 接口体系

```
Repository (标记接口)
    └── CrudRepository<T, ID>          → 基本的 CRUD 操作
            └── PagingAndSortingRepository<T, ID>  → 分页 + 排序
                    └── JpaRepository<T, ID>       → JPA 特有方法（flush、批量操作）
                    └── MongoRepository<T, ID>     → MongoDB 特有方法
```

| 接口 | 提供方法 | 适用场景 |
|------|----------|----------|
| `Repository` | 空标记接口 | 需要完全自定义方法时 |
| `CrudRepository` | `save`、`findById`、`findAll`、`count`、`delete`、`existsById` | 仅有基本CRUD需求 |
| `PagingAndSortingRepository` | + `findAll(Pageable)`、`findAll(Sort)` | 需要分页和排序 |
| `JpaRepository` | + `flush`、`saveAndFlush`、`deleteInBatch`、`getAllById` | JPA完整功能 |
| `MongoRepository` | + MongoDB 特有查询 | MongoDB 数据访问 |

#### 2.1.3 核心依赖

```xml
<!-- Spring Boot Starter Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Boot Starter Data Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Spring Boot Starter Data MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

---

### 2.2 Spring Data JPA 深度解析

#### 2.2.1 实体映射

```java
// 基础实体
@Entity                         // 标记为 JPA 实体
@Table(name = "t_user")         // 映射到表 t_user
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder                       // Builder 模式构建对象
public class User {

    @Id                         // 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 自增主键
    private Long id;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(unique = true, nullable = false)
    private String email;

    private Integer age;        // 无 @Column 则默认字段名 = 列名

    @Enumerated(EnumType.STRING)  // 枚举存为字符串而不是数字下标
    private UserStatus status;

    @Temporal(TemporalType.TIMESTAMP)  // 日期精度
    private Date createTime;

    @Transient                  // 不映射到数据库
    private String tempField;
}

public enum UserStatus {
    ACTIVE, INACTIVE, BANNED
}
```

```yaml
# application.yml 配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/spring_data?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update          # create | update | validate | none
    show-sql: true              # 控制台打印 SQL
    properties:
      hibernate:
        format_sql: true        # 格式化 SQL
        use_sql_comments: true  # 显示 JPQL 注释
        dialect: org.hibernate.dialect.MySQL8Dialect
```

> ⚠️ `ddl-auto` 在生产环境建议用 `validate` 或 `none`，禁止使用 `create`（会删表）或 `update`（有风险）。开发阶段可以使用 `update`。

#### 2.2.2 Repository 基础 CRUD

```java
// 1. 定义 Repository
@Repository                                   // 声明为 Spring Bean
public interface UserRepository extends JpaRepository<User, Long> {
    // 继承 JpaRepository 后自动拥有以下方法：
    // save(User)           → 保存/更新
    // findById(Long)       → 按主键查询
    // findAll()            → 查询全部
    // findAll(Pageable)    → 分页查询
    // count()              → 统计
    // delete(User)         → 删除
    // existsById(Long)     → 判断存在
}

// 2. 使用 Repository
@Service
@Transactional(readOnly = true)  // 查询方法统一只读事务
public class UserService {

    private final UserRepository userRepository;

    // 构造器注入（Spring 推荐）
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ========== 新增 ==========
    @Transactional      // 写操作需要读写事务
    public User createUser(User user) {
        // save 方法：id 为 null 时执行 INSERT，id 不为 null 且存在时执行 UPDATE
        return userRepository.save(user);
    }

    @Transactional
    public List<User> batchCreate(List<User> users) {
        return userRepository.saveAll(users);
    }

    // ========== 查询 ==========
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
        // Optional 避免 NPE，推荐使用 orElseThrow / orElse
    }

    public User findByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在，id=" + id));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public boolean exists(Long id) {
        return userRepository.existsById(id);
    }

    public long count() {
        return userRepository.count();
    }

    // ========== 更新 ==========
    @Transactional
    public User updateUser(Long id, String newName) {
        User user = findByIdOrThrow(id);
        user.setUserName(newName);       // 直接修改实体对象
        // 无需调用 save()！事务提交时 JPA 会自动检测脏数据并 UPDATE
        return user;
    }

    // ========== 删除 ==========
    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void deleteUser(User user) {
        userRepository.delete(user);
    }
}
```

> 💡 **JPA 更新机制**：在 `@Transactional` 范围内，JPA 通过 **脏检查（Dirty Checking）** 自动对比实体快照和当前状态，提交时自动生成 UPDATE 语句，无需手动调用 `save()`。

#### 2.2.3 方法命名查询

Spring Data JPA 最重要的特性之一：**根据方法名自动生成查询**。

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // ========== 精确匹配 ==========
    Optional<User> findByUserName(String userName);

    Optional<User> findByEmail(String email);

    List<User> findByAge(Integer age);

    // ========== 条件查询 ==========
    List<User> findByAgeGreaterThan(Integer age);            // age > ?
    List<User> findByAgeLessThanEqual(Integer age);          // age <= ?
    List<User> findByAgeBetween(Integer start, Integer end); // age BETWEEN ? AND ?

    List<User> findByUserNameLike(String pattern);           // name LIKE '%?%'
    List<User> findByUserNameStartingWith(String prefix);    // name LIKE '?%'
    List<User> findByUserNameEndingWith(String suffix);      // name LIKE '%?'

    List<User> findByAgeIn(Collection<Integer> ages);        // age IN (?)
    List<User> findByAgeNotIn(Collection<Integer> ages);     // age NOT IN (?)

    // ========== 空值判断 ==========
    List<User> findByEmailIsNull();
    List<User> findByEmailIsNotNull();

    // ========== 逻辑运算 ==========
    List<User> findByUserNameAndEmail(String name, String email);   // AND
    List<User> findByUserNameOrEmail(String name, String email);    // OR

    // ========== 排序 ==========
    List<User> findByAgeGreaterThanOrderByAgeAsc(Integer age);        // ORDER BY age ASC
    List<User> findByAgeGreaterThanOrderByAgeDesc(Integer age);       // ORDER BY age DESC

    // ========== 去重 / 限制 ==========
    List<User> findDistinctByAge(Integer age);                        // SELECT DISTINCT
    Optional<User> findFirstByOrderByAgeDesc();                       // 年龄最大的
    List<User> findTop5ByOrderByCreateTimeDesc();                     // 最近5条
    Page<User> findTop10By(Pageable pageable);                        // 配合分页
}
```

**方法命名关键字对照表**：

| 关键字 | SQL 片段 | 示例 |
|--------|----------|------|
| `And` | `AND` | `findByAgeAndName` |
| `Or` | `OR` | `findByAgeOrName` |
| `Between` | `BETWEEN` | `findByAgeBetween` |
| `LessThan` | `<` | `findByAgeLessThan` |
| `LessThanEqual` | `<=` | `findByAgeLessThanEqual` |
| `GreaterThan` | `>` | `findByAgeGreaterThan` |
| `GreaterThanEqual` | `>=` | `findByAgeGreaterThanEqual` |
| `After` | `>` (Date) | `findByCreateTimeAfter` |
| `Before` | `<` (Date) | `findByCreateTimeBefore` |
| `IsNull` | `IS NULL` | `findByEmailIsNull` |
| `IsNotNull` / `NotNull` | `IS NOT NULL` | `findByEmailNotNull` |
| `Like` | `LIKE` | `findByNameLike` |
| `NotLike` | `NOT LIKE` | `findByNameNotLike` |
| `StartingWith` | `LIKE 'prefix%'` | `findByNameStartingWith` |
| `EndingWith` | `LIKE '%suffix'` | `findByNameEndingWith` |
| `Containing` | `LIKE '%value%'` | `findByNameContaining` |
| `OrderBy` | `ORDER BY` | `findByAgeOrderByNameDesc` |
| `Not` | `<>` | `findByNameNot` |
| `In` | `IN` | `findByAgeIn` |
| `NotIn` | `NOT IN` | `findByAgeNotIn` |
| `True` / `False` | `= true` / `= false` | `findByActiveTrue` |
| `IgnoreCase` | `UPPER(x)=UPPER(y)` | `findByNameIgnoreCase` |

#### 2.2.4 分页与排序

```java
// ========== 分页查询 ==========
@Service
public class UserPageService {

    private final UserRepository userRepository;

    // 1. 基础分页
    public Page<User> findUsersByPage(int pageNum, int pageSize) {
        // PageRequest 从 0 开始计数！
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<User> page = userRepository.findAll(pageable);

        // Page 包含丰富的信息
        System.out.println(page.getContent());          // 当前页数据
        System.out.println(page.getTotalElements());    // 总记录数
        System.out.println(page.getTotalPages());       // 总页数
        System.out.println(page.getNumber());           // 当前页码
        System.out.println(page.getSize());             // 每页大小
        System.out.println(page.isFirst());             // 是否为第一页
        System.out.println(page.isLast());              // 是否为最后一页
        System.out.println(page.hasNext());             // 是否有下一页

        return page;
    }

    // 2. 分页 + 排序
    public Page<User> findUsersSortedByAge(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("age").ascending());
        return userRepository.findAll(pageable);
    }

    // 3. 多字段排序
    public Page<User> findUsersMultiSort(int page, int size) {
        Sort sort = Sort.by(
                Sort.Order.desc("createTime"),  // 先按创建时间降序
                Sort.Order.asc("age")           // 再按年龄升序
        );
        return userRepository.findAll(PageRequest.of(page, size, sort));
    }

    // 4. 方法命名 + 分页
    public Page<User> findByAgeGreaterThan(Integer age, int page, int size) {
        return userRepository.findByAgeGreaterThan(age, PageRequest.of(page, size));
    }

    // 5. List 方式（不返回 Page 元数据）
    public List<User> findUsersSorted(Sort sort) {
        return userRepository.findAll(sort);
    }

    // 6. 自定义 Pageable 实现
    public Page<User> findCustomPage() {
        // 从第 0 页开始，每页 10 条，按 id 降序
        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "id");
        // 等效写法：
        // Pageable pageable = PageRequest.ofSize(10).withPage(0)
        //         .withSort(Sort.by(Sort.Direction.DESC, "id"));
        return userRepository.findAll(pageable);
    }
}
```

> 💡 `Page` 对象在返回给前端时，通常通过 `PageDTO` 转换，避免直接暴露 JPA 内部结构。可以使用 `Page.map()` 方法：
> ```java
> Page<UserDTO> dtoPage = userPage.map(user -> new UserDTO(user.getId(), user.getUserName()));
> ```

#### 2.2.5 @Query 自定义查询（JPQL + 原生SQL）

当方法命名查询无法满足需求（多表关联、复杂聚合、子查询）时，使用 `@Query` 注解。

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // ========== JPQL 查询（面向实体对象） ==========

    @Query("SELECT u FROM User u WHERE u.email = ?1")
    Optional<User> findByEmailJPQL(String email);

    @Query("SELECT u FROM User u WHERE u.userName LIKE %:keyword% AND u.age > :minAge")
    List<User> searchUsers(@Param("keyword") String keyword,
                           @Param("minAge") Integer minAge);

    @Query("SELECT u.userName, u.age FROM User u WHERE u.age > ?1")
    List<Object[]> findUserNamesAndAges(Integer minAge);

    // JPQL 聚合查询
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") UserStatus status);

    @Query("SELECT AVG(u.age) FROM User u WHERE u.status = 'ACTIVE'")
    Double findAverageAgeByActive();

    // ========== JPQL 更新/删除（需要 @Modifying + @Transactional） ==========

    @Modifying          // 标识这是一个更新/删除操作
    @Transactional      // 必须配合事务
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    int updateUserStatus(@Param("id") Long id, @Param("status") UserStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.email = ?1")
    int deleteByEmail(String email);

    // ========== 原生 SQL 查询（nativeQuery = true） ==========

    @Query(value = "SELECT * FROM t_user WHERE age > ?1", nativeQuery = true)
    List<User> findUsersByNativeSQL(Integer minAge);

    @Query(value = "SELECT u.id, u.user_name, COUNT(o.id) as order_count " +
                   "FROM t_user u LEFT JOIN t_order o ON u.id = o.user_id " +
                   "GROUP BY u.id HAVING COUNT(o.id) > ?1",
           nativeQuery = true)
    List<Object[]> findUsersWithOrderCount(int minOrderCount);

    // 原生 SQL 分页（必须写 countQuery）
    @Query(value = "SELECT * FROM t_user WHERE age > ?1",
           countQuery = "SELECT COUNT(*) FROM t_user WHERE age > ?1",
           nativeQuery = true)
    Page<User> findUsersByAgeNative(Integer age, Pageable pageable);
}
```

**JPQL vs 原生 SQL 选择指南**：

| 维度 | JPQL | 原生 SQL |
|------|------|----------|
| 对象性 | 直接返回实体/DTO映射 | 返回 `Object[]`，需手动转换 |
| 跨数据库 | 自动适配方言 | 写死数据库语法，不可移植 |
| 复杂查询 | 多表关联语法受限 | 支持窗口函数、子查询等所有 SQL 特性 |
| 性能优化 | 难以使用索引提示 | 可以写 `FORCE INDEX`、`USE INDEX` |
| 推荐场景 | 简单 CRUD、单表 | 复杂报表、统计分析 |

#### 2.2.6 Specification 动态查询

当查询条件不确定（动态拼接 WHERE 子句）时，使用 `Specification`。

```java
// 1. Repository 继承 JpaSpecificationExecutor
public interface UserRepository extends
        JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {  // 加上这个接口获得 Specification 支持
}

// 2. 创建动态查询
@Service
public class UserDynamicQueryService {

    private final UserRepository userRepository;

    // 方式一：匿名内部类
    public Page<User> searchUsers(String name, Integer minAge, Integer maxAge,
                                   UserStatus status, Pageable pageable) {

        Specification<User> spec = Specification.where(null);  // 起始条件

        if (StringUtils.hasText(name)) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("userName"), "%" + name + "%")
            );
        }

        if (minAge != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("age"), minAge)
            );
        }

        if (maxAge != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("age"), maxAge)
            );
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status)
            );
        }

        return userRepository.findAll(spec, pageable);
    }

    // 方式二：工具类封装
    public Page<User> searchWithTools(UserQuery query, Pageable pageable) {
        Specification<User> spec = UserSpecifications.buildQuery(query);
        return userRepository.findAll(spec, pageable);
    }
}

// 3. 封装为工具类
public class UserSpecifications {

    public static Specification<User> buildQuery(UserQuery query) {
        return Specification
                .where(hasName(query.getUserName()))
                .and(hasAgeBetween(query.getMinAge(), query.getMaxAge()))
                .and(hasStatus(query.getStatus()))
                .and(createdAfter(query.getStartDate()));
    }

    private static Specification<User> hasName(String name) {
        return (root, query, cb) -> name == null ? cb.conjunction()
                : cb.like(root.get("userName"), "%" + name + "%");
    }

    private static Specification<User> hasAgeBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return cb.conjunction();
            if (min == null) return cb.lessThanOrEqualTo(root.get("age"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("age"), min);
            return cb.between(root.get("age"), min, max);
        };
    }

    private static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    private static Specification<User> createdAfter(LocalDate date) {
        return (root, query, cb) -> date == null ? cb.conjunction()
                : cb.greaterThan(root.get("createTime"), date.atStartOfDay());
    }
}

// 4. 查询条件 DTO
@Data
public class UserQuery {
    private String userName;
    private Integer minAge;
    private Integer maxAge;
    private UserStatus status;
    private LocalDate startDate;
}
```

> 💡 `cb.conjunction()` 表示 `1=1`（恒真条件），当参数为 null 时不添加该条件，这是 Specification 常用的"短路"写法。

#### 2.2.7 实体关联关系

```java
// ========== 1. 用户与订单：一对多 ==========
// User.java（一方）
@Entity
@Table(name = "t_user")
@Data
@ToString(exclude = "orders")   // 避免 toString 循环
@EqualsAndHashCode(exclude = "orders")  // 避免循环
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;

    private Integer age;

    @OneToMany(mappedBy = "user",              // 由 Order 的 user 字段维护关系
               cascade = CascadeType.ALL,      // 级联操作
               fetch = FetchType.LAZY,         // 懒加载（重要！）
               orphanRemoval = true)           // 删除订单时自动清理
    private List<Order> orders = new ArrayList<>();

    // 双向关联维护的辅助方法
    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }

    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);
    }
}

// Order.java（多方）
@Entity
@Table(name = "t_order")
@Data
@ToString(exclude = "user")
@EqualsAndHashCode(exclude = "user")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;

    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)         // 多对一，懒加载
    @JoinColumn(name = "user_id")              // 外键列名
    private User user;
}

// ========== 2. 学生与课程：多对多 ==========
// Student.java
@Entity
@Data
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(name = "t_student_course",          // 中间表名
               joinColumns = @JoinColumn(name = "student_id"),
               inverseJoinColumns = @JoinColumn(name = "course_id"))
    private List<Course> courses = new ArrayList<>();
}

// Course.java
@Entity
@Data
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "courses")             // 由 Student 维护关系
    private List<Student> students = new ArrayList<>();
}
```

> ⚠️ **最关键的 JPA 性能陷阱**：`@OneToMany` 默认 `FetchType.LAZY` 是必须的！如果使用 `EAGER`，一个 `findAll()` 可能产生 N+1 条 SQL。`@ManyToOne` 默认是 `EAGER`，建议显式改为 `LAZY`。

#### 2.2.8 CascadeType 详解

| CascadeType | 含义 | 说明 |
|-------------|------|------|
| `PERSIST` | 级联保存 | 保存一方时同时保存多方 |
| `MERGE` | 级联更新 | 更新一方时同时更新多方 |
| `REMOVE` | 级联删除 | 删除一方时同时删除多方 |
| `REFRESH` | 级联刷新 | 刷新一方时同时刷新多方 |
| `DETACH` | 级联脱管 | 脱管一方时同时脱管多方 |
| `ALL` | 全部级联 | 包含以上所有 |

> ⚠️ **级联删除需谨慎**：`CascadeType.REMOVE` 或 `orphanRemoval = true` 在删除父实体时会连带删除子实体。如果子实体还被其他业务引用，会导致数据完整性错误。生产环境通常只使用 `PERSIST` 和 `MERGE`。

---

### 2.3 Spring Data Redis 深度解析

#### 2.3.1 RedisTemplate 与序列化

```java
@Configuration
@EnableCaching        // 开启缓存注解支持
public class RedisConfig {

    // ========== 1. RedisTemplate 自定义配置 ==========

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // JSON 序列化器（用于 Value）
        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);  // 不序列化 null
        om.setDateFormat("yyyy-MM-dd HH:mm:ss");
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        jsonSerializer.setObjectMapper(om);

        // String 序列化器（用于 Key）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 设置 Key 和 HashKey 使用 String 序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        // 设置 Value 和 HashValue 使用 JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // ========== 2. StringRedisTemplate ==========
    // Spring Boot 自动配置已提供 StringRedisTemplate Bean，直接注入即可
    // 专注于 String-String 操作，key 和 value 都用 StringRedisSerializer

    // ========== 3. 缓存管理器（用于 @Cacheable 注解） ==========

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))                    // 默认过期时间 30 分钟
                .serializeKeysWith(                                  // Key 序列化
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(                                // Value 序列化
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();                         // 不缓存 null

        // 支持针对每个缓存单独设置 TTL
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        configMap.put("users", config.entryTtl(Duration.ofMinutes(10)));
        configMap.put("products", config.entryTtl(Duration.ofHours(1)));
        configMap.put("config", config.entryTtl(Duration.ofDays(7)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(configMap)
                .build();
    }
}
```

#### 2.3.2 RedisTemplate 操作实战

```java
@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    // 常用操作类型
    private final ValueOperations<String, Object> valueOps;
    private final HashOperations<String, String, Object> hashOps;
    private final ListOperations<String, Object> listOps;
    private final SetOperations<String, Object> setOps;
    private final ZSetOperations<String, Object> zSetOps;

    public RedisService(RedisTemplate<String, Object> redisTemplate,
                        StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        // 获取各类操作接口（更高效）
        this.valueOps = redisTemplate.opsForValue();
        this.hashOps = redisTemplate.opsForHash();
        this.listOps = redisTemplate.opsForList();
        this.setOps = redisTemplate.opsForSet();
        this.zSetOps = redisTemplate.opsForZSet();
    }

    // ========== String 操作 ==========

    public void setString(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public String getString(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    // ========== 对象操作（JSON 序列化） ==========

    public void setObject(String key, Object value) {
        valueOps.set(key, value);
    }

    public void setObjectWithExpire(String key, Object value, long timeout, TimeUnit unit) {
        valueOps.set(key, value, timeout, unit);
    }

    public <T> T getObject(String key, Class<T> clazz) {
        Object value = valueOps.get(key);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        // 如果泛型擦除导致类型不匹配，通过 ObjectMapper 手动反序列化
        return null;
    }

    // ========== 过期时间 ==========

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key);
        return expire != null ? expire : -1;
    }

    // ========== Hash 操作 ==========

    public void putHash(String key, String hashKey, Object value) {
        hashOps.put(key, hashKey, value);
    }

    public Object getHash(String key, String hashKey) {
        return hashOps.get(key, hashKey);
    }

    public Map<String, Object> getAllHash(String key) {
        return hashOps.entries(key);
    }

    // ========== List 操作 ==========

    public void leftPush(String key, Object value) {
        listOps.leftPush(key, value);
    }

    public Object rightPop(String key) {
        return listOps.rightPop(key);
    }

    public List<Object> range(String key, long start, long end) {
        return listOps.range(key, start, end);
    }

    // ========== Set 操作 ==========

    public void addToSet(String key, Object... values) {
        setOps.add(key, values);
    }

    public boolean isMember(String key, Object value) {
        return Boolean.TRUE.equals(setOps.isMember(key, value));
    }

    // ========== 布隆过滤器（Redisson 实现，略） ==========

    // ========== 分布式锁 ==========

    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        // 方式一：RedisTemplate 原生 SETNX
        Boolean result = valueOps.setIfAbsent(key, "locked", timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    // ========== 原子操作 ==========

    public long increment(String key) {
        Long count = valueOps.increment(key);
        return count != null ? count : 0;
    }

    public long incrementBy(String key, long delta) {
        Long count = valueOps.increment(key, delta);
        return count != null ? count : 0;
    }

    // ========== 批量操作（Pipeline） ==========

    public List<Object> executePipeline(List<RedisCallback<?>> callbacks) {
        return redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            callbacks.forEach(callback -> callback.doInRedis(connection));
            return null;
        });
    }

    // ========== 删除 ==========

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void deleteBatch(Collection<String> keys) {
        redisTemplate.delete(keys);
    }
}
```

#### 2.3.3 序列化策略对比

| 序列化器 | 适用位置 | 特点 | 可读性 | 空间 | 推荐度 |
|----------|----------|------|--------|------|--------|
| `StringRedisSerializer` | **Key** 和 HashKey | UTF-8 字符串，简洁 | 高 | 小 | ⭐⭐⭐⭐⭐ |
| `Jackson2JsonRedisSerializer` | Value | JSON格式，支持复杂对象 | 高 | 中 | ⭐⭐⭐⭐⭐ |
| `GenericJackson2JsonRedisSerializer` | Value | 带 class 信息的 JSON | 高 | 中 | ⭐⭐⭐⭐ |
| `JdkSerializationRedisSerializer` | Value | Java 原生序列化（默认） | 乱码 | 大 | ❌ 禁止 |
| `OxmSerializer` | Value | XML 格式 | 中 | 大 | ⭐ |
| `GenericToStringSerializer` | Value | 基于转换器 | 中 | 小 | ⭐⭐ |

> ⚠️ **Spring Boot 默认的 RedisTemplate 使用 JdkSerializationRedisSerializer**，存到 Redis 中是二进制乱码，且可读性极差。**务必自定义序列化器！**

#### 2.3.4 缓存注解详解

```java
@Service
@CacheConfig(cacheNames = "users")  // 类级别统一缓存名称
public class CacheUserService {

    @Autowired
    private UserRepository userRepository;

    // ========== @Cacheable：查询时缓存，缓存存在则直接返回 ==========

    @Override
    @Cacheable(
            cacheNames = "users",                // 缓存名称（对应 Redis 中的 key 前缀）
            key = "#id",                          // 缓存的 key（SpEL 表达式）
            unless = "#result == null",           // 条件：结果为空时不缓存
            condition = "#id > 0"                 // 条件：id > 0 才缓存
    )
    // 生成的 Redis key = "users::1"
    public User getUserById(Long id) {
        // 第一次调用：执行方法，缓存结果
        // 第二次调用（相同 key）：直接返回缓存，不执行方法
        return userRepository.findById(id).orElse(null);
    }

    // 复杂 SpEL key
    @Cacheable(key = "#user.userName + '_' + #user.age")
    public User searchUser(User user) {
        return userRepository.findByUserName(user.getUserName()).orElse(null);
    }

    // ========== @CachePut：总是执行方法，并更新缓存 ==========

    @CachePut(key = "#result.id")
    // 或 @CachePut(key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
        // 先执行方法，再将返回值写入缓存
        // 与 @Cacheable 不同：不会拦截方法执行
    }

    // ========== @CacheEvict：清除缓存 ==========

    @CacheEvict(key = "#id")                      // 删除单个缓存
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @CacheEvict(allEntries = true)               // 清空 users 下的所有缓存
    public void clearAllUserCache() {
        // 方法体可以不执行任何操作，但必须有
    }

    @CacheEvict(key = "#id", beforeInvocation = true)  // 在方法执行前删除缓存
    public void deleteUserBefore(Long id) {
        // beforeInvocation=true：即使方法抛出异常，缓存也已清除
        // beforeInvocation=false（默认）：方法异常则不清除
        userRepository.deleteById(id);
    }

    // ========== @Caching：组合多个缓存注解 ==========

    @Caching(
            cacheable = @Cacheable(key = "#id"),
            put = {
                    @CachePut(key = "#result.userName", condition = "#result != null"),
                    @CachePut(key = "'email_' + #result.email")
            },
            evict = @CacheEvict(key = "'all_users'")
    )
    public User getAndCacheUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // ========== 多个缓存名称 ==========

    @Cacheable(cacheNames = {"users", "profiles"}, key = "#id")
    public User getUserWithProfiles(Long id) {
        return userRepository.findById(id).orElse(null);
        // 同时存储到 users::1 和 profiles::1 两个缓存中
    }
}
```

> 💡 **SpEL 上下文变量**：
> - `#root.methodName`：方法名
> - `#root.targetClass`：目标类
> - `#root.args`：方法参数数组
> - `#result`：方法返回值（仅用于 `@CachePut` 和 `@CacheEvict` 的 condition/unless）
> - `#参数名` 或 `#a0`、`#p0`：方法参数

#### 2.3.5 缓存封装工具类

```java
// ========== 缓存数据封装 ==========

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheResult<T> {
    private T data;
    private boolean fromCache;  // 是否来自缓存
    private long cacheTime;     // 缓存时间戳
}

// ========== 穿透保护的缓存工具 ==========

@Component
public class CacheTemplate<K, V> {

    private final RedisTemplate<String, Object> redisTemplate;

    // 缓存穿透保护：缓存空值（短TTL）
    private static final long NULL_VALUE_TTL = 30;  // 空值缓存 30 秒
    private static final String NULL_PLACEHOLDER = "__NULL__";

    /**
     * 缓存模板方法：查缓存 → 查数据库 → 回填缓存
     * 包含缓存穿透、缓存击穿保护
     */
    @SuppressWarnings("unchecked")
    public V queryWithCache(
            String cacheKey,                        // Redis key
            long ttl,                               // 缓存过期时间（秒）
            TimeUnit unit,                          // 时间单位
            Supplier<V> dbQuery,                    // 数据库查询回调
            boolean enableNullCache                 // 是否缓存空值防穿透
    ) {
        // 1. 查缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (NULL_PLACEHOLDER.equals(cached)) {
                return null;  // 空值缓存命中，直接返回 null
            }
            return (V) cached;
        }

        // 2. 缓存未命中，查数据库（加锁防击穿）
        synchronized (this) {
            // 双重检查
            cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return NULL_PLACEHOLDER.equals(cached) ? null : (V) cached;
            }

            V result = dbQuery.get();

            // 3. 回填缓存
            if (result == null && enableNullCache) {
                // 缓存空值防止穿透（短TTL）
                redisTemplate.opsForValue()
                        .set(cacheKey, NULL_PLACEHOLDER, NULL_VALUE_TTL, TimeUnit.SECONDS);
            } else if (result != null) {
                redisTemplate.opsForValue().set(cacheKey, result, ttl, unit);
            }

            return result;
        }
    }

    /**
     * 批量查询缓存
     */
    public Map<String, V> batchQuery(List<String> keys, Class<V> clazz) {
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) return Collections.emptyMap();

        Map<String, V> result = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            Object val = values.get(i);
            if (val != null && clazz.isInstance(val)) {
                result.put(keys.get(i), clazz.cast(val));
            }
        }
        return result;
    }
}
```

---

### 2.4 Spring Data MongoDB 基础

#### 2.4.1 配置与实体映射

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/spring_data_db
      # 或分项配置：
      # host: localhost
      # port: 27017
      # database: spring_data_db
      # username: admin
      # password: admin
```

```java
// 实体映射
@Document(collection = "user_profile")  // 映射到 MongoDB 的 user_profile 集合
@Data
public class UserProfile {

    @Id                                 // MongoDB 的 _id 字段
    private String id;                  // MongoDB 默认使用 ObjectId 字符串

    @Field("nickname")                  // 指定文档字段名
    private String nickName;

    private Integer age;

    @Field("tags")
    private List<String> tags;          // 数组字段

    private Address address;            // 嵌套文档

    @CreatedDate                        // 自动填充创建时间
    private LocalDateTime createdTime;

    @LastModifiedDate                   // 自动填充修改时间
    private LocalDateTime updatedTime;
}

// 嵌套文档（不需要 @Document）
@Data
public class Address {
    private String province;
    private String city;
    private String detail;
}
```

#### 2.4.2 MongoRepository

```java
// Repository 定义
public interface UserProfileRepository
        extends MongoRepository<UserProfile, String> {

    // 方法命名查询
    List<UserProfile> findByNickName(String nickName);

    List<UserProfile> findByAgeGreaterThan(Integer age);

    List<UserProfile> findByTagsContaining(String tag);

    // @Query 查询（MongoDB JSON Query 语法）
    @Query("{ 'age': { '$gte': ?0, '$lte': ?1 } }")
    List<UserProfile> findByAgeBetween(Integer min, Integer max);

    // 排序
    List<UserProfile> findByAgeGreaterThanOrderByAgeDesc(Integer age);
}

// 使用 MongoTemplate 高级查询
@Service
public class UserProfileService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserProfileRepository repository;

    // 基础 CRUD
    public UserProfile save(UserProfile profile) {
        return repository.save(profile);
    }

    public Optional<UserProfile> findById(String id) {
        return repository.findById(id);
    }

    // MongoTemplate 聚合查询
    public List<UserProfile> complexQuery(String tag, Integer minAge) {
        Query query = new Query();
        query.addCriteria(Criteria.where("tags").in(tag)
                .and("age").gte(minAge));
        query.with(Sort.by(Sort.Direction.DESC, "age"));
        query.skip(0).limit(10);
        return mongoTemplate.find(query, UserProfile.class);
    }
}
```

#### 2.4.3 JPA vs MongoDB Repository 对比

| 特性 | JPA Repository | MongoRepository |
|------|---------------|-----------------|
| 底层存储 | 关系型数据库（MySQL/PostgreSQL） | 文档型数据库（MongoDB） |
| 主键类型 | `Long`/`Integer`/`UUID` | `String`（ObjectId） |
| 关联关系 | `@OneToMany`、`@JoinColumn` | 嵌入式文档（`@DBRef` 可选） |
| 查询语法 | JPQL / SQL / 方法命名 | JSON Query / 方法命名 |
| 事务 | `@Transactional` 强事务 | 单文档原子性 / `@Transactional` 仅副本集 |
| Schema变更 | `ddl-auto` 或 Migration | 无 Schema，动态字段 |

---

### 2.5 JPA vs MyBatis vs JDBC Template 对比

#### 2.5.1 综合对比表

| 维度 | Spring Data JPA (Hibernate) | MyBatis | JDBC Template |
|------|---------------------------|---------|---------------|
| **抽象层次** | 全自动 ORM（对象 ↔ 表） | 半自动 ORM（SQL ↔ 对象） | 数据访问工具 |
| **SQL 控制力** | 弱（自动生成，难以调优） | 强（手写 SQL，完全掌控） | 最强（原生 SQL） |
| **开发效率** | ⭐⭐⭐⭐⭐ 极高 | ⭐⭐⭐ 中等 | ⭐⭐ 低 |
| **学习成本** | 中高（JPA 规范 + Hibernate 特性） | 低（会 SQL 即可） | 低 |
| **复杂查询** | NP困难（Specification 晦涩） | ⭐⭐⭐⭐⭐ 擅长 | ⭐⭐⭐⭐ |
| **动态 SQL** | Specification / @Query 拼凑 | `<if>` `<choose>` 标签强大 | 手拼字符串 |
| **缓存** | 一级/二级缓存自动管理 | 需手动配置 | 无 |
| **懒加载** | 原生支持 | 需插件或手动 | 无 |
| **N+1 问题** | 容易触发，需警惕 | 不会自动产生（SQL 写死） | 不会 |
| **分页** | Pageable 开箱即用 | 需插件或手写 limit | 手写 |
| **批量操作** | saveAll 自动批处理 | BatchExecutor | Batch 手动 |
| **事务控制** | JPA 事务管理器 | DataSource 事务管理器 | DataSource 事务管理器 |
| **迁移成本** | 切换数据库几乎无感 | SQL 方言需调整 | SQL 方言需调整 |
| **主流版本** | Spring Data JPA 3.x | MyBatis 3.5.x | Spring 6.x |

#### 2.5.2 选型决策树

```
项目数据层选型
├── 团队 SQL 能力强，对性能要求极致？
│   └── ✅ MyBatis（互联网大厂常用）
├── 团队 Java 能力强，追求开发效率？
│   └── ✅ JPA（创业公司、内部系统、快速迭代）
├── 复杂关联报表、多表 Join？
│   └── ✅ MyBatis + 手写 SQL
├── 简单 CRUD、单表操作为主？
│   └── ✅ JPA（80% 场景都能覆盖）
├── 不希望引入任何 ORM？
│   └── ✅ JDBC Template 或 JOOQ
└── 要两者优点？
    └── ✅ JPA + MyBatis 混合（JPA 管 CRUD，MyBatis 管复杂查询）
```

#### 2.5.3 相同功能代码对比

```java
// ========== 场景：分页查询年龄 > 18 的用户，按名称模糊匹配 ==========

// ---------- JPA 方式 ----------
public interface JpaUserRepo extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.age > :minAge AND u.userName LIKE %:name%")
    Page<User> searchUsers(@Param("minAge") int minAge,
                           @Param("name") String name,
                           Pageable pageable);
}
// 调用：repo.searchUsers(18, "张", PageRequest.of(0, 10));
// 生成 SQL：SELECT * FROM t_user WHERE age > ? AND user_name LIKE ? LIMIT ?, ?

// ---------- MyBatis 方式 ----------
// UserMapper.xml
// <select id="searchUsers" resultType="User">
//   SELECT * FROM t_user
//   <where>
//     <if test="minAge != null">AND age > #{minAge}</if>
//     <if test="name != null and name != ''">AND user_name LIKE CONCAT('%', #{name}, '%')</if>
//   </where>
//   ORDER BY ${pageable.sort}
//   LIMIT #{pageable.offset}, #{pageable.pageSize}
// </select>
// 调用：mapper.searchUsers(18, "张", pageable);

// ---------- JDBC Template 方式 ----------
public Page<User> searchUsers(int minAge, String name, Pageable pageable) {
    StringBuilder sql = new StringBuilder("SELECT * FROM t_user WHERE age > ?");
    if (name != null && !name.isEmpty()) {
        sql.append(" AND user_name LIKE ?");
    }
    sql.append(" LIMIT ?, ?");

    List<Object> params = new ArrayList<>();
    params.add(minAge);
    if (name != null && !name.isEmpty()) {
        params.add("%" + name + "%");
    }
    params.add(pageable.getOffset());
    params.add(pageable.getPageSize());

    List<User> users = jdbcTemplate.query(sql.toString(), params.toArray(),
            new BeanPropertyRowMapper<>(User.class));
    return new PageImpl<>(users, pageable, countUsers(minAge, name));
}
```

#### 2.5.4 混合使用策略（生产推荐）

生产环境中，**JPA + MyBatis 混合** 是最常见的方案：

```java
// JPA 负责：单表 CRUD、分页、事务管理
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 80% 的单表操作，一行代码不用写
}

// MyBatis 负责：复杂多表联查、报表统计
@Mapper
public interface UserReportMapper {
    List<UserOrderVO> queryUserOrderReport(@Param("userId") Long userId,
                                            @Param("startDate") String startDate);
}
```

> 🎯 **一句话总结**：JPA 做 80% 的简单 CRUD，MyBatis 做 20% 的复杂查询，各自用最擅长的领域。

---

## 3. 高频踩坑与误区

### 3.1 JPA 常见陷阱

| 序号 | 问题现象 | 根因 | 解决方案 |
|------|----------|------|----------|
| 1 | **N+1 查询**：查 10 条 User，发了 1 + 10 = 11 条 SQL | `@OneToMany` 默认 Lazy 但循环遍历时会逐个查询 | 使用 `@EntityGraph` 或 `join fetch` 一次性关联查询 |
| 2 | **懒加载异常**：`LazyInitializationException` | 在事务外访问未加载的关联对象 | 使用 `@Transactional` 或在查询时提前加载 |
| 3 | **save() 返回空**：`save()` 后实体 id 为 null | 主键策略配置错误或没有 `@GeneratedValue` | 检查实体主键注解 |
| 4 | **修改不生效**：调用了 `setName()` 但没有调 `save()` | 脏检查仅在 `@Transactional` 事务内有效 | 确认方法在事务中 |
| 5 | **方法命名查询找不到**：`findByLast_Name` 报错 | 驼峰转下划线规则不符实际列名 | 用 `@Query` 明确指定 SQL |
| 6 | **分页返回全部数据**：Page 内容为所有记录 | 没有正确传递 Pageable 参数 | 确认 DAO 方法接收 Pageable 参数 |
| 7 | **死循环 StackOverflow**：`toString()` 双向引用 | 实体间 A→B→A 循环引用 | 使用 `@ToString(exclude = "...")` |
| 8 | **级联删除删了整个库**：删除 User 连带删除了所有关联数据 | CascadeType.ALL 包含 REMOVE | 只使用 `CascadeType.PERSIST` + `MERGE` |
| 9 | **@Query 更新未生效**：明明写了 UPDATE 语句，数据库没变 | 缺少 `@Modifying` 注解 | 更新/删除操作必须加 `@Modifying` |
| 10 | **save() 执行了 INSERT 而非 UPDATE** | 实体 id 不为 null 但数据库中不存在这条记录 | `save()` 是 merge 语义，如果是 detached 实体会 INSERT |

### 3.2 N+1 问题的根治

```java
// ========== 问题复现 ==========
// User 有 @OneToMany(mappedBy = "user", fetch = FetchType.LAZY) List<Order> orders

@GetMapping("/users")
public List<User> getUsers() {
    List<User> users = userRepository.findAll();  // 1 条 SQL: SELECT * FROM t_user
    for (User user : users) {
        System.out.println(user.getOrders().size());  // N 条 SQL: SELECT * FROM t_order WHERE user_id = ?
    }
    // 总共 1 + N 条 SQL！
    return users;
}

// ========== 方案一：@EntityGraph（推荐） ==========
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"orders"})   // 指定要关联查询的字段
    @Query("SELECT u FROM User u")
    List<User> findAllWithOrders();

    @EntityGraph(attributePaths = {"orders", "profile"})  // 多个关联
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithAll(@Param("id") Long id);
}

// ========== 方案二：JPQL JOIN FETCH ==========
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.orders")
    List<User> findAllWithOrdersFetch();

    // DISTINCT 避免笛卡尔积重复
    // JOIN FETCH 会忽略懒加载设置，强制一次性加载
}

// ========== 方案三：@NamedEntityGraph（实体类上声明） ==========
@Entity
@NamedEntityGraph(name = "User.withOrders",
                  attributeNodes = @NamedAttributeNode("orders"))
public class User {
    // ...
}

// Repository 使用
@EntityGraph("User.withOrders")
List<User> findAll();
```

### 3.3 Redis 缓存陷阱

| 序号 | 问题现象 | 根因 | 解决方案 |
|------|----------|------|----------|
| 1 | **缓存穿透**：请求大量不存在的 key，直接打穿到 DB | 恶意攻击或查询不存在的 ID | 缓存空值 + 布隆过滤器 |
| 2 | **缓存击穿**：热点 key 过期瞬间高并发打到 DB | 一个 key 过期 + 大量并发 | 互斥锁 + 后台异步更新 |
| 3 | **缓存雪崩**：大量 key 同时过期，DB 被打爆 | 同一时间大量缓存失效 | 过期时间加随机偏移 + 多级缓存 |
| 4 | **Redis 存了乱码**：`\xac\xed\x00\x05t...` | 未自定义序列化器，使用默认 JDK 序列化 | 配置 Jackson2JsonRedisSerializer |
| 5 | **@Cacheable 不生效** | 未加 `@EnableCaching` | 在配置类上加 `@EnableCaching` |
| 6 | **同一个 key 缓存了不同类型** | 多个方法用相同 cacheNames + key | 确保每个缓存有唯一的 key 组合 |
| 7 | **缓存与数据库不一致** | 删缓存和更新数据库非原子操作 | 延迟双删 + Canal 监听 binlog |
| 8 | **@CacheEvict 未清除干净** | 只删了单条未删列表缓存 | `allEntries = true` 或维护缓存 key 列表 |

### 3.4 缓存穿透解决方案

```java
@Component
public class CachePenetrationGuard {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ========== 方案一：布隆过滤器 ==========
    // 提前将所有可能存在的 key 哈希到 bitmap 中
    // 查询时先过布隆过滤器，不存在则直接返回

    // ========== 方案二：缓存空值（已在 CacheTemplate 中实现） ==========

    // ========== 方案三：互斥锁防击穿 ==========

    public User getUserWithMutexLock(Long id) {
        String cacheKey = "user::" + id;
        String lockKey = "lock::" + cacheKey;

        // 1. 查缓存
        User user = (User) redisTemplate.opsForValue().get(cacheKey);
        if (user != null) return user;

        // 2. 缓存未命中，尝试获取分布式锁
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 3, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 3. 再次检查缓存（双重检查）
                user = (User) redisTemplate.opsForValue().get(cacheKey);
                if (user != null) return user;

                // 4. 查数据库
                user = userRepository.findById(id).orElse(null);

                // 5. 回填缓存
                if (user != null) {
                    redisTemplate.opsForValue()
                            .set(cacheKey, user, 30, TimeUnit.MINUTES);
                }
                return user;
            } finally {
                redisTemplate.delete(lockKey);  // 释放锁
            }
        } else {
            // 6. 未获取到锁，等待重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getUserWithMutexLock(id);  // 递归重试
        }
    }
}
```

---

## 4. 随堂基础练习

### 4.1 JPA 基础练习

1. 创建一个 `City` 实体，包含 `id`、`name`、`province`、`population` 字段，使用 `JpaRepository` 完成 CRUD
2. 在 `CityRepository` 中定义方法：按省份查询、人口大于指定值查询、名称模糊查询
3. 使用 `Pageable` 实现城市列表分页，按人口降序排列
4. 使用 `@Query` 写一个 JPQL 统计每个省份的城市数量

### 4.2 Redis 基础练习

1. 配置自定义 `RedisTemplate`，使用 `Jackson2JsonRedisSerializer` 序列化 Value
2. 使用 `ValueOperations` 缓存一个用户对象，设置过期时间 5 分钟
3. 使用 `HashOperations` 存储用户信息（name / age / email）到 Hash 中
4. 使用 `@Cacheable` 注解实现对数据库查询的缓存

### 4.3 综合对比练习

1. 用 JPA、MyBatis、JDBC Template 三种方式分别实现查询年龄范围在 20~30 之间的用户列表
2. 对比三种方式的代码量和执行效率

---

## 5. 章节综合实操案例

### 5.1 案例需求：用户订单管理系统

实现一个用户订单管理系统的数据访问层，包含以下功能：

1. **用户管理**：注册、信息修改、状态管理
2. **订单管理**：下单、支付、取消、分页查询
3. **商品浏览**：热门商品缓存、商品详情
4. **数据报表**：用户消费排行

### 5.2 实体定义

```java
// ========== 用户实体 ==========
@Entity
@Table(name = "t_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String nickname;

    private Integer age;

    @Enumerated(EnumType.STRING)
    private UserLevel level;     // BRONZE, SILVER, GOLD, PLATINUM

    @Enumerated(EnumType.STRING)
    private UserStatus status;   // NORMAL, FROZEN, DELETED

    private LocalDateTime registerTime;

    private LocalDateTime lastLoginTime;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();
}

// ========== 订单实体 ==========
@Entity
@Table(name = "t_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String orderNo;

    private BigDecimal totalAmount;

    private Integer productCount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;  // PENDING, PAID, SHIPPED, COMPLETED, CANCELLED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private LocalDateTime createTime;

    private LocalDateTime payTime;
}

// ========== 订单项 ==========
@Entity
@Table(name = "t_order_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;

    private BigDecimal price;   // 购买时的单价
}

// ========== 商品实体 ==========
@Entity
@Table(name = "t_product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private Integer stock;

    private String category;

    private Boolean isHot;      // 是否热门商品

    private LocalDateTime createTime;
}
```

### 5.3 Repository 层

```java
// ========== UserRepository ==========
public interface UserRepository extends
        JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :id")
    Optional<User> findByIdWithOrders(@Param("id") Long id);

    @EntityGraph(attributePaths = {"orders"})
    @Query("SELECT u FROM User u WHERE u.level = :level")
    List<User> findByLevelWithOrders(@Param("level") UserLevel level);

    long countByStatus(UserStatus status);
}

// ========== OrderRepository ==========
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createTime DESC")
    List<Order> findRecentOrdersByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createTime BETWEEN :start AND :end")
    List<Order> findOrdersByStatusAndDateRange(
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :status, o.payTime = :payTime WHERE o.id = :id")
    int updateOrderStatus(@Param("id") Long id,
                          @Param("status") OrderStatus status,
                          @Param("payTime") LocalDateTime payTime);

    @Query(value = "SELECT u.id, u.nickname, SUM(o.total_amount) as totalSpent " +
                   "FROM t_user u JOIN t_order o ON u.id = o.user_id " +
                   "WHERE o.status = 'COMPLETED' " +
                   "GROUP BY u.id ORDER BY totalSpent DESC LIMIT ?1",
           nativeQuery = true)
    List<Object[]> findTopSpenders(int limit);
}

// ========== ProductRepository ==========
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);

    Page<Product> findByCategory(String category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isHot = true ORDER BY p.createTime DESC")
    List<Product> findHotProducts();
}

// ========== OrderItemRepository ==========
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
}
```

### 5.4 Service 层（含缓存）

```java
@Service
@Transactional
public class OrderService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ========== 用户注册 ==========
    public User register(String username, String password, String nickname) {
        // 检查用户名唯一性
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        User user = User.builder()
                .username(username)
                .password(password)
                .nickname(nickname)
                .level(UserLevel.BRONZE)
                .status(UserStatus.NORMAL)
                .registerTime(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    // ========== 用户下单 ==========
    public Order createOrder(Long userId, List<OrderRequest> items) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Order order = Order.builder()
                .orderNo(generateOrderNo())
                .user(user)
                .status(OrderStatus.PENDING)
                .createTime(LocalDateTime.now())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest req : items) {
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new RuntimeException("商品不存在"));

            if (product.getStock() < req.getQuantity()) {
                throw new RuntimeException("商品库存不足：" + product.getName());
            }

            // 扣减库存
            product.setStock(product.getStock() - req.getQuantity());
            productRepository.save(product);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(req.getQuantity())
                    .price(product.getPrice())
                    .build();

            orderItems.add(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(req.getQuantity())));
        }

        order.setTotalAmount(total);
        order.setProductCount(orderItems.size());
        order.setItems(orderItems);
        return orderRepository.save(order);
    }

    // ========== 订单支付（含缓存更新） ==========
    @CacheEvict(cacheNames = "orders", key = "#orderNo")
    public Order payOrder(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("订单状态不正确，无法支付");
        }

        order.setStatus(OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());

        return orderRepository.save(order);
    }

    // ========== 带缓存的商品查询 ==========
    @Cacheable(cacheNames = "products", key = "#id", unless = "#result == null")
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    // ========== 热门商品缓存 ==========
    public List<Product> getHotProducts() {
        String cacheKey = "products:hot";

        // 1. 查缓存
        List<Product> cached = (List<Product>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        // 2. 查数据库
        List<Product> products = productRepository.findHotProducts();

        // 3. 回填缓存（热门商品缓存 5 分钟）
        redisTemplate.opsForValue().set(cacheKey, products, 5, TimeUnit.MINUTES);
        return products;
    }

    // ========== 用户订单分页查询（含缓存） ==========
    @Cacheable(cacheNames = "orders", key = "'user:' + #userId + ':page:' + #page")
    public Page<Order> getUserOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
        return orderRepository.findByUserId(userId, pageable);
    }

    // ========== 消费排行（纯 SQL 实现） ==========
    public List<UserSpendVO> getTopSpenders(int limit) {
        List<Object[]> results = orderRepository.findTopSpenders(limit);
        List<UserSpendVO> voList = new ArrayList<>();
        for (Object[] row : results) {
            voList.add(new UserSpendVO(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (BigDecimal) row[2]
            ));
        }
        return voList;
    }

    // ========== 工具方法 ==========
    private String generateOrderNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
```

### 5.5 配置与启动

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/spring_data_demo?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 30000
      connection-timeout: 10000

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 3000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

---

## 6. 分层综合习题

### 基础题

1. Spring Data JPA 中 `CrudRepository`、`PagingAndSortingRepository`、`JpaRepository` 三者的继承关系是什么？
2. 写出使用 `@Query` 进行 JPQL 分页查询的完整代码片段
3. `@OneToMany` 中 `mappedBy` 属性有什么作用？
4. `@Cacheable`、`@CachePut`、`@CacheEvict` 三个注解分别代表什么语义？
5. Spring Data Redis 中 `StringRedisTemplate` 和 `RedisTemplate<String, Object>` 有何区别？

### 进阶应用题

6. JPA 的 N+1 问题是如何产生的？分别写出 `@EntityGraph` 和 `JOIN FETCH` 的解决方案
7. 设计一个通用的缓存工具类，支持缓存穿透防护（空值缓存）、过期时间可配置
8. 描述 `@Modifying` 注解的作用场景，为什么更新操作必须配合 `@Transactional`？
9. 在 JPA 双向关联中如何避免 `toString()` 死循环和 JSON 序列化死循环？
10. 使用 `Specification<User>` 实现一个支持以下条件的动态查询 API：用户名模糊匹配、年龄范围、注册时间范围、状态过滤、按注册时间排序分页

### 精通拔高题

11. JPA 的 `save()` 方法底层是 `persist()` 还是 `merge()`？什么条件下执行 INSERT，什么条件下执行 UPDATE？分析 `SimpleJpaRepository.save()` 源码
12. 设计一个多级缓存方案（本地 Caffeine + 远程 Redis），并说明缓存一致性如何保证
13. JPA 懒加载代理的实现原理是什么？`HibernateProxy` 如何工作？LazyInitializationException 产生的底层原因是什么？
14. 对比 Spring Data JPA 和 MyBatis 在大规模数据批量插入（10万+记录）场景下的性能差异，说明应如何优化
15. Redis 分布式缓存与本地缓存组成的两级缓存中，如果数据库发生写操作，如何保证两级缓存的一致性？给出伪代码实现

---

## 7. 本章复盘速记清单

| 类别 | 要点 |
|------|------|
| **Repository 体系** | `Repository` → `CrudRepository` → `PagingAndSortingRepository` → `JpaRepository` / `MongoRepository` |
| **实体映射核心** | `@Entity` `@Id` `@GeneratedValue` `@Column` `@Table` `@Transient` `@Enumerated` |
| **方法命名查询** | `findBy` + 字段名 + `And`/`Or`/`Between`/`Like`/`OrderBy` / `GreaterThan` / `In` / `Null` / `Containing` |
| **@Query** | JPQL 面向实体对象，`nativeQuery=true` 用原生 SQL；更新操作必须加 `@Modifying` + `@Transactional` |
| **分页排序** | `Pageable` → `PageRequest.of(page, size, Sort.by(...))`；`Page` 包含 content/totalElements/totalPages 等 |
| **Specification** | 动态查询三要素：`Specification` + `JpaSpecificationExecutor` + `CriteriaBuilder`；短路用 `cb.conjunction()` |
| **关联关系** | `@OneToMany`(LAZY) + `@ManyToOne`(LAZY) + `@ManyToMany` + `@JoinColumn` + `@JoinTable` |
| **N+1 规避** | `@EntityGraph(attributePaths=...)` 或 `JOIN FETCH` 或 `@NamedEntityGraph` |
| **RedisTemplate** | **Key 用 StringRedisSerializer**，**Value 用 Jackson2JsonRedisSerializer**，禁用默认 JDK 序列化 |
| **缓存注解** | `@Cacheable`(查缓存→执行→存缓存)、`@CachePut`(执行→存缓存)、`@CacheEvict`(删缓存)、`@Caching`(组合) |
| **缓存异常** | 穿透（空值缓存+布隆过滤器）、击穿（互斥锁）、雪崩（随机TTL+多级缓存） |
| **JPA vs MyBatis** | JPA = 开发效率高、80% CRUD 自动；MyBatis = SQL 可控、复杂查询强；可混合使用 |
| **MongoDB** | `@Document` `@Field` `MongoRepository` `MongoTemplate`；无 Schema、适合文档模型 |
| **序列化器** | `StringRedisSerializer`(Key)、`Jackson2JsonRedisSerializer`(Value)、`GenericJackson2JsonRedisSerializer`(带class信息) |

---

## 8. 精通拓展补充-P2

### 8.1 JPA 一级缓存与二级缓存

```java
// ========== 一级缓存（PersistenceContext） ==========
// 作用范围：同一个 EntityManager / 同一个事务内
// 自动开启，不可关闭

@Service
public class FirstLevelCacheDemo {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void demo() {
        // 第一次查询：发送 SQL 到数据库
        User user1 = userRepository.findById(1L).orElse(null);

        // 第二次查询：同一事务内，直接返回一级缓存中的对象（不发送 SQL）
        User user2 = userRepository.findById(1L).orElse(null);

        System.out.println(user1 == user2);  // true — 同一对象引用

        // 一级缓存在事务提交时清空
    }
}

// ========== 二级缓存（Second-Level Cache） ==========
// 作用范围：SessionFactory 级别，跨事务共享
// 需要显式开启，使用第三方缓存实现（Ehcache / Redis）

// application.yml 配置
// spring.jpa.properties.hibernate.cache.use_second_level_cache=true
// spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory

// 实体上标记
@Entity
@Cacheable                            // 允许二级缓存
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User {
    // ...
}
```

### 8.2 多数据源配置

```java
// ========== 当项目需要同时连接多个数据库 ==========

// 1. application.yml
// spring:
//   datasource:
//     primary:
//       url: jdbc:mysql://localhost:3306/db1
//       username: root
//       password: root
//     secondary:
//       url: jdbc:mysql://localhost:3306/db2
//       username: root
//       password: root

// 2. 配置类
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "secondaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }
}

// 3. 主数据源配置
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.example.primary.repository",
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager"
)
public class PrimaryJpaConfig {

    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("primaryDataSource") DataSource dataSource) {
        // 配置 Hibernate 属性和扫描路径
        return new LocalContainerEntityManagerFactoryBean();
    }
}
```

### 8.3 Auditing 自动填充时间

```java
// ========== 自动填充创建时间、修改时间 ==========

// 1. 启动类开启审计
@SpringBootApplication
@EnableJpaAuditing         // 开启 JPA 审计
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// 2. 实体类使用审计注解
@Entity
@EntityListeners(AuditingEntityListener.class)   // 必须添加监听器
public class BaseEntity {

    @CreatedDate
    @Column(updatable = false)                    // 创建后不允许修改
    private LocalDateTime createdTime;

    @LastModifiedDate
    private LocalDateTime updatedTime;

    @CreatedBy                                     // 自动填充创建人
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy                                // 自动填充修改人
    private String updatedBy;
}

// 3. 实现 AuditorAware（填充创建人/修改人）
@Component
public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        // 从 SecurityContext 获取当前登录用户
        // return Optional.ofNullable(SecurityContextHolder.getContext()
        //         .getAuthentication().getName());
        return Optional.of("system");  // 默认
    }
}
```

### 8.4 Spring Data REST

```java
// ========== 零代码暴露 REST 接口 ==========

// 添加依赖：spring-boot-starter-data-rest

// 一个 Repository 自动生成 REST 端点
@RepositoryRestResource(
        path = "users",              // 访问路径：/users
        collectionResourceRel = "users",
        itemResourceRel = "user"
)
public interface UserRepository extends JpaRepository<User, Long> {
    // 自动生成：
    // GET    /users           → 查询所有
    // GET    /users/{id}      → 查询单个
    // POST   /users           → 新增
    // PUT    /users/{id}      → 全量更新
    // PATCH  /users/{id}      → 部分更新
    // DELETE /users/{id}      → 删除
}

// 可选：配置基础路径
// spring.data.rest.base-path=/api/v1
// 访问 /api/v1/users
```

### 8.5 事务与锁策略

```java
// ========== 悲观锁 ==========
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)   // SELECT ... FOR UPDATE
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithPessimisticLock(@Param("id") Long id);
}

// ========== 乐观锁 ==========
@Entity
public class Product {

    @Id
    private Long id;

    private String name;

    private Integer stock;

    @Version                                        // 乐观锁版本号
    private Integer version;

    // 更新时自动检查 version：
    // UPDATE product SET stock = ?, version = version + 1
    // WHERE id = ? AND version = ?
    // 如果 version 不匹配则抛出 OptimisticLockException
}

// ========== 事务传播行为 ==========
@Service
public class PropagationDemo {

    @Transactional(propagation = Propagation.REQUIRED)   // 默认：支持当前事务，没有则新建
    public void required() { }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 永远新建事务，挂起当前事务
    public void requiresNew() { }

    @Transactional(propagation = Propagation.NESTED)      // 嵌套事务（Savepoint 实现）
    public void nested() { }

    @Transactional(propagation = Propagation.SUPPORTS)    // 支持当前事务，没有则不开启
    public void supports() { }

    @Transactional(propagation = Propagation.MANDATORY)   // 必须在事务中，否则抛异常
    public void mandatory() { }

    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 不支持事务，挂起当前事务
    public void notSupported() { }

    @Transactional(propagation = Propagation.NEVER)       // 不能在事务中，否则抛异常
    public void never() { }
}
```

### 8.6 Redis 高级特性

```java
// ========== Redis Pipeline 批量操作（提升吞吐量） ==========
public void pipelineBatch(List<User> users) {
    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
        for (User user : users) {
            byte[] key = ("user:" + user.getId()).getBytes();
            byte[] value = JSON.toJSONBytes(user);
            connection.stringCommands().set(key, value);
        }
        return null;
    });
}

// ========== Redis Lua 脚本（原子操作） ==========
// 场景：限流器 — 单位时间内最多允许 N 次访问
public boolean rateLimit(String key, int maxCount, long windowSeconds) {
    String luaScript =
            "local key = KEYS[1] " +
            "local limit = tonumber(ARGV[1]) " +
            "local now = redis.call('TIME')[1] " +
            "local window = tonumber(ARGV[2]) " +
            "redis.call('ZREMRANGEBYSCORE', key, 0, now - window) " +
            "local count = redis.call('ZCARD', key) " +
            "if count < limit then " +
            "   redis.call('ZADD', key, now, now) " +
            "   redis.call('EXPIRE', key, window) " +
            "   return 1 " +
            "else " +
            "   return 0 " +
            "end";

    RedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
    Long result = redisTemplate.execute(script, List.of(key), maxCount, windowSeconds);
    return Long.valueOf(1).equals(result);
}

// ========== Redis 发布订阅 ==========
// 配置监听器
@Bean
public MessageListenerAdapter messageListener(RedisReceiver receiver) {
    return new MessageListenerAdapter(receiver, "receiveMessage");
}

@Bean
public RedisMessageListenerContainer container(
        RedisConnectionFactory factory,
        MessageListenerAdapter listener) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(factory);
    container.addMessageListener(listener, new PatternTopic("channel:orders"));
    return container;
}

// 发布消息
public void publishOrderEvent(Order order) {
    redisTemplate.convertAndSend("channel:orders", JSON.toJSONString(order));
}
```

### 8.7 性能优化清单

```yaml
# JPA 性能优化
spring.jpa.properties:
  hibernate:
    jdbc.batch_size: 50                # 批量提交，减少网络往返
    jdbc.fetch_size: 200               # 一次从数据库读取的行数
    order_inserts: true                # 按类型排序插入，利于批量
    order_updates: true                # 按类型排序更新，利于批量
    generate_statistics: true          # 开启统计（性能调优时用，生产关掉）
    jdbc.batch_versioned_data: true    # 支持版本号的批量操作
```

| 优化项 | 手段 | 效果 |
|--------|------|------|
| **批量操作** | `hibernate.jdbc.batch_size=50` | INSERT/UPDATE 合并为 batch，提升 5~10 倍 |
| **只读优化** | `@Transactional(readOnly = true)` | 关闭脏检查，Hibernate 不维护快照 |
| **关联查询** | `@EntityGraph` 替代循环查询 | 避免 N+1，减少 90%+ SQL 语句 |
| **分页优化** | `Pageable` + `countQuery` | 避免全表扫描 |
| **查询优化** | `Select new DTO` 替代实体 | 只查需要的字段，减少网络传输 |
| **缓存优化** | Redis 缓存热点数据 | 降低 DB 负载 90%+ |
| **连接池** | HikariCP 调优 | 避免连接等待 |

> 🎯 **Spring Data 学习心法**：JPA 适合 80% 的简单 CRUD，用约定大于配置的思路理解它；对于复杂 SQL 不要硬用 JPA 的 Specification 去拼，大胆引入 MyBatis 做混合架构。Redis 层重点关注缓存策略（穿透/击穿/雪崩）和序列化配置，这是生产环境出问题最多的地方。牢记"缓存是缓存，数据库是数据库"，两者的一致性是分布式系统最难的挑战之一。
