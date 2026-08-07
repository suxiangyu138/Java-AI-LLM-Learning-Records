# 07 Repository 模式与查询

> Spring Data Redis Repository 把"对象 ↔ Redis hash"映射、二级索引、TTL、查询方法派生全部封装成声明式能力——但它的底层是"每对象两个 key（hash + 索引集）"的数据模型，用错场景会付出惨痛代价。本模块讲透机制与适用边界

---

## 📚 目录

1. [Repository 是什么：对象化 Redis 的最后一公里](#1-repository-是什么对象化-redis-的最后一公里)
2. [数据模型：@RedisHash 与二级索引](#2-数据模型redishash-与二级索引)
3. [开启与基础 CRUD](#3-开启与基础-crud)
4. [查询方法派生](#4-查询方法派生)
5. [分页、排序与 TTL](#5-分页排序与-ttl)
6. [适用边界与性能警告](#6-适用边界与性能警告)
7. [与手动 RedisTemplate 的协同模式](#7-与手动-redistemplate-的协同模式)

---

## 1. Repository 是什么：对象化 Redis 的最后一公里

Spring Data Redis Repository 是"领域对象映射"：用注解描述对象与 Redis hash 的映射，用接口方法名声明查询，框架自动生成实现——**零手写序列化、零手写命令**。

```java
// 实体：映射到 Redis hash
@RedisHash("user")                      // hash key 前缀
public class User {
    @Id Long id;                        // 主键 → hash key 的尾缀
    @Indexed String name;               // 索引字段 → 生成二级索引
    @Indexed String email;
    String phone;                       // 普通字段：只存 hash 内
    @TimeToLive Long ttl;               // 过期时间（秒）
    // getters/setters...
}
```

```java
// Repository 接口：方法名即查询
public interface UserRepository extends CrudRepository<User, Long> {
    List<User> findByName(String name);
    List<User> findByNameAndEmail(String name, String email);
    Page<User> findByEmail(String email, Pageable pageable);
}
```

> 💡 注意：这里的 `@Indexed`、`@RedisHash` 与 JPA 的注解**完全不同**（Spring Data 各模块各自定义）。本质是"把 Redis 当文档型存储"使用，与关系型数据库建模思路有显著差异。

## 2. 数据模型：@RedisHash 与二级索引

### 2.1 物理存储结构（理解一切的关键）

```text
存储一个 User{id=1001, name="张三", email="z@x.com", phone="138..."} 后：

① 主数据（hash）：
   key: user:1001
   fields: id="1001"  name="张三"  email="z@x.com"  phone="138..."  _class="com.demo.User"

② 二级索引（set，每个 @Indexed 字段一个）：
   key: user:name:张三        → { "1001" }
   key: user:email:z@x.com    → { "1001" }
   作用：findByName("张三") = SISMEMBER user:name:张三 拿到 id 列表 → MGET user:1001...
```

> 🎯 **核心要点**：**每个对象在 Redis 里不是 1 个 key，而是 1 个 hash + N 个索引 set（N = @Indexed 字段数）**。这是理解 Repository 一切开销与陷阱的起点。`@RedisHash("user")` 只是前缀；`@Id` 只是主键；`@Indexed` 是"我要按这个字段查询"的声明——没标 @Indexed 的字段**无法**作为查询条件（会报错或全表扫描）。

### 2.2 注解清单

| 注解 | 作用于 | 语义 |
|------|--------|------|
| `@RedisHash("user")` | 类 | hash key 前缀 |
| `@Id` | 字段 | 主键；null 时保存自动生成 UUID |
| `@Indexed` | 字段 | 建二级索引（set） |
| `@TimeToLive` | Long/Integer 字段 | 过期秒数（TTL），每次写生效 |
| `@Reference` | 关联对象字段 | 只存关联 id，不内嵌对象 |

> ⚠️ **@TimeToLive 细节**：保存/更新对象时按字段值刷新 TTL；但**普通字段改动、索引字段删除**等场景的 TTL 行为差异大，测试期务必验证"更新后 TTL 是否符合预期"。

## 3. 开启与基础 CRUD

### 3.1 开启 Repository

```java
@Configuration
@EnableRedisRepositories                     // 扫描 @Repository 接口（等价配置）
public class RedisRepositoryConfig { }
```

```java
@Repository
public interface UserRepository extends CrudRepository<User, Long> { }

// 使用
User saved = userRepository.save(user);
Optional<User> found = userRepository.findById(1001L);
Iterable<User> all = userRepository.findAll();
userRepository.deleteById(1001L);
long count = userRepository.count();
```

### 3.2 继承体系

```text
CrudRepository<User, Long>          基础 CRUD（必须）
├── PagingAndSortingRepository      分页 + 排序
│   └── JpaRepository（JPA 专属，Redis 不用）
└── QueryByExampleExecutor          Example 查询（按对象样例匹配）
```

## 4. 查询方法派生

### 4.1 规则：方法名 → 命令

```java
List<User> findByName(String name);                       // SISMEMBER 索引 + MGET
List<User> findByNameContaining(String name);             // 模糊（索引 set 内匹配，慎用大索引）
List<User> findByEmailAndName(String email, String name); // 多条件 = 索引交集
List<User> findByAgeGreaterThan(int age);                 // 范围（Redis 无范围索引，慎用）
Page<User> findByEmail(String email, Pageable pageable);  // 分页
```

| 关键词 | 语义 | 实现 | 性能警告 |
|--------|------|------|:---:|
| `findByXxx` | 等值 | 索引 set → MGET | ✅ |
| `And`/`Or` | 组合 | 索引 set 交/并 | ✅ |
| `Containing` | 模糊 | 索引内扫描 | ⚠️ 大索引慎用 |
| `GreaterThan`/`Between` | 范围 | **全表扫描过滤** | ❌ 无索引支持 |
| `Top3`/`First` | 限量 | 结果截断 | ✅ |

> ⚠️ **性能认知**：范围查询（GreaterThan/Between）在 Redis 上**没有索引**——框架实现是全表遍历过滤，O(N) 且阻塞。Redis Repository 只适合"等值查询为主"的模型，重范围/统计的查询请用 ZSet 或换数据库。

### 4.2 排序与 Example

```java
// 排序
List<User> users = userRepository.findByName("张三",
        Sort.by(Sort.Direction.DESC, "age"));

// Example 查询：拿对象当模板
User probe = new User();
probe.setEmail("z@x.com");
List<User> result = userRepository.findAll(Example.of(probe));
```

## 5. 分页、排序与 TTL

```java
public interface UserRepository extends PagingAndSortingRepository<User, Long> {
    Page<User> findByEmail(String email, Pageable pageable);
}

// 使用
Pageable pageable = PageRequest.of(0, 20, Sort.by("id").descending());
Page<User> page = userRepository.findByEmail("z@x.com", pageable);
List<User> content = page.getContent();
long total = page.getTotalElements();
```

**分页实现机制（理解成本的关键）：**

```text
分页 = 先把匹配 id 全量查出来（索引 set）→ 内存做 offset/limit 切片
      → total 就是索引 size
```

> ⚠️ **陷阱**：Redis Repository 的分页**不是数据库式延迟分页**——底层还是"全量候选集 + 内存切片"。大索引集的分页照样 O(N)。排序同理：只对当前结果集排序，**全局排序需先全量取出**。

**TTL 维护：**

```java
@RedisHash("session")
public class UserSession {
    @Id String token;
    String userId;
    @TimeToLive Long ttlSeconds = 3600L;   // 每次 save 刷新过期
}
```

## 6. 适用边界与性能警告

### 6.1 适合用 Repository 的场景

| 场景 | 理由 |
|------|------|
| 简单对象缓存，按 id/少数字段查询 | 声明式开发，快 |
| 原型/内部工具/低并发管理台 | 开发效率优先 |
| 与其它 Spring Data 模块统一 API 风格 | 代码一致性 |

### 6.2 不适合的场景（生产红线）

| 场景 | 为什么不行 |
|------|-----------|
| 高并发核心链路缓存 | 每对象 1+N 个 key、MGET 开销、无管道聚合——QPS 上不去 |
| 范围/统计/模糊查询 | 无索引，全表 O(N) 阻塞 |
| 大对象列表（列表页等） | 分页是"全量+切片"假分页 |
| 需要精确控制 TTL/序列化 | 框架按固定模型存储，自定义能力弱 |
| 缓存与 DB 一致性治理 | 无旁路缓存语义，应走 06 篇 Spring Cache |

> 🎯 **核心结论**：Repository 适合"把 Redis 当文档库用的轻模型"，**不适合"把 Redis 当缓存用的高并发场景"**。生产主链路请用 RedisTemplate + Spring Cache；Repository 留给管理后台、原型验证、简单配置存储。

### 6.3 性能警告清单

- 每个 @Indexed 字段 = 一个 set key，**索引膨胀**：100 万对象 × 3 索引 = 300 万 set 成员；
- 保存 = 写 hash + 维护所有索引（**删除索引字段时框架会 SREM 旧值**，多余开销）；
- `findAll` = 全量 SCAN + 反序列化（禁止生产使用）；
- 对象大字段（Blob/大文本）塞 hash 会拖慢所有索引操作；
- 集群模式下 hash key 与索引 key 的 slot 分布（同前缀 user:* 一般同 slot，跨 slot 需验证）。

## 7. 与手动 RedisTemplate 的协同模式

**推荐的生产组合：**

```text
读链路（高并发）：RedisTemplate + 06 篇缓存配置（直接读 hash/JSON）
管理链路（低频）：Repository（复杂查询、管理后台）
写链路（一致性）：统一走 Service 层，先写 DB 再双写/删除缓存
```

```java
@Service
public class UserService {
    // 高并发读：RedisTemplate 直读（毫秒级）
    public User getFast(Long id) {
        Object v = redisTemplate.opsForHash().entries("user:" + id);
        return v.isEmpty() ? null : convert(v);
    }

    // 管理查询：Repository（低频复杂查询）
    @Resource
    private UserRepository userRepository;
    public List<User> search(String name) {
        return userRepository.findByName(name);
    }
}
```

> 💡 **决策口诀**：`@Cacheable` 管"热点缓存"，RedisTemplate 管"精确控制"，Repository 管"对象查询"——三者按场景分工，而非互相替代。

> 🎯 **核心要点**：Repository 的甜蜜点与死穴都在数据模型——**1 对象 = 1 hash + N 索引 set**。等值查询它好用（开发快、语义清晰），范围查询它致命（无索引全表扫），高并发它撑不住（key 爆炸 + 无管道）。面试问"什么时候用 Spring Data Redis Repository"，答"轻模型 + 等值查询 + 低频"就是满分答案。

---

**上一模块**：[06-Spring Cache抽象与RedisCacheManager](06-Spring Cache抽象与RedisCacheManager.md)　**下一模块**：[08-Pub/Sub与Stream消息集成](08-PubSub与Stream消息集成.md)
