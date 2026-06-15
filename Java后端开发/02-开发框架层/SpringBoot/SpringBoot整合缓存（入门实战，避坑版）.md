# Spring Boot 整合缓存（入门实战，避坑版）

> **文档定位**：Java 后端技术参考文档 | Spring Boot + Caffeine 本地缓存实战  
> **核心方案**：Spring Cache（缓存抽象）+ Caffeine（本地缓存，轻量高效）  
> **前置条件**：Spring Boot 2.7.x + MyBatis（或 JDBC）+ 基础 CRUD 能力  
> **学习路径**：先掌握本地缓存（Caffeine），再学分布式缓存（Redis）

---

## 目录

- [一、核心认知](#一核心认知)
- [二、引入缓存依赖](#二引入缓存依赖)
- [三、缓存核心配置](#三缓存核心配置)
- [四、核心注解实战](#四核心注解实战)
- [五、新手常见坑及解决方案](#五新手常见坑及解决方案)
- [六、进阶补充](#六进阶补充)

---

## 一、核心认知

| 概念 | 说明 |
|------|------|
| **Spring Cache** | Spring 提供的缓存抽象框架，统一缓存 API，通过注解实现缓存，可切换底层实现 |
| **Caffeine** | Spring Boot 2.7.x 默认本地缓存，轻量高效，适合单机部署 |
| **核心注解** | `@Cacheable` / `@CachePut` / `@CacheEvict` / `@Caching` / `@CacheConfig` |

> **避坑提醒**：新手优先使用本地缓存（Caffeine），无需急于整合 Redis（需额外部署服务，增加入门难度）。

---

## 二、引入缓存依赖

```xml
<!-- Spring Cache 核心（提供缓存抽象和注解） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Caffeine 缓存实现（Spring Boot 2.7.x 默认整合，必加） -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- 可选：MyBatis（实战测试用） -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.2.2</version>
</dependency>
```

> **注意**：无需手动指定 Caffeine 版本，Spring Boot 父依赖会自动匹配兼容版本。

---

## 三、缓存核心配置

### application.properties

```properties
# 1. 开启 Spring Cache 注解支持（核心！必须配置）
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=600s

# 2. 数据库连接（实战测试用）
spring.datasource.url=jdbc:mysql://localhost:3306/springboot_db?serverTimezone=Asia/Shanghai&useSSL=false&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 3. MyBatis 配置
mybatis.mapper-locations=classpath:mapper/*.xml
mybatis.type-aliases-package=com.example.springboot.data.entity
mybatis.configuration.map-underscore-to-camel-case=true
```

### Caffeine 参数说明

| 参数 | 说明 | 示例值 |
|------|------|--------|
| `maximumSize` | 缓存最大容量，超量后淘汰不常用数据 | `1000` |
| `expireAfterWrite` | 写入后过期时间 | `600s`（10 分钟） |
| `expireAfterAccess` | 最后访问后过期时间（可选） | `300s` |

> **避坑**：不要遗漏 `spring.cache.type=caffeine`，否则使用默认的 SimpleCacheManager，效果差且不支持过期时间。

### 启动类

```java
@SpringBootApplication
@EnableCaching  // 必须添加！否则所有缓存注解无效
public class SpringbootCacheApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringbootCacheApplication.class, args);
    }
}
```

---

## 四、核心注解实战

### 5 大注解速查

| 注解 | 作用 | 核心说明 |
|------|------|----------|
| **`@Cacheable`** | 查询缓存：先查缓存，有则返回，无则执行方法并缓存 | 必须指定 `value`（缓存名），`key` 可选 |
| **`@CachePut`** | 更新缓存：执行方法后将结果存入缓存 | 与 `@Cacheable` 的 value/key 保持一致 |
| **`@CacheEvict`** | 删除缓存：执行方法后删除指定缓存 | `allEntries=true` 可删除整个缓存 |
| **`@Caching`** | 组合多个缓存注解 | 复杂场景使用 |
| **`@CacheConfig`** | 类级注解，统一指定缓存名 | 简化代码，避免重复写 `value` |

### 实战：UserService 完整示例

```java
@Service
public class UserCacheService {
    @Autowired
    private UserXmlMapper userXmlMapper;

    // 1. 查询缓存：根据 ID 查询用户
    @Cacheable(value = "user", key = "#id")
    public User getUserById(Integer id) {
        System.out.println("查询数据库，id：" + id);  // 缓存生效时不打印
        return userXmlMapper.getUserById(id);
    }

    // 2. 查询所有用户（缓存的 key 自定义为 'allUser'，避免与单个用户冲突）
    @Cacheable(value = "user", key = "'allUser'")
    public List<User> getAllUser() {
        System.out.println("查询数据库，所有用户");
        return userXmlMapper.getAllUser();
    }

    // 3. 新增用户：新增后更新缓存
    @CachePut(value = "user", key = "#user.id")
    public User addUser(User user) {
        userXmlMapper.addUser(user);
        return user;  // 必须返回 user，否则缓存存入 null
    }

    // 4. 修改用户：修改后更新缓存
    @CachePut(value = "user", key = "#user.id")
    public User updateUser(User user) {
        userXmlMapper.updateUser(user);
        return user;
    }

    // 5. 删除用户：删除后清除对应缓存
    @CacheEvict(value = "user", key = "#id")
    public void deleteUser(Integer id) {
        userXmlMapper.deleteUser(id);
    }

    // 6. 删除所有用户：清空整个 user 缓存
    @CacheEvict(value = "user", allEntries = true)
    public void deleteAllUser() {
        userXmlMapper.deleteAllUser();
    }
}
```

### 测试验证

| 测试场景 | 预期效果 |
|----------|----------|
| 两次调用 `getUserById(1)` | 控制台只打印一次"查询数据库"，第二次走缓存 |
| 先 `addUser()` 再 `getUserById(新ID)` | 不查数据库，直接从缓存返回 |
| 先 `deleteUser(1)` 再 `getUserById(1)` | 重新查数据库（缓存已删除） |

---

## 五、新手常见坑及解决方案

| 坑 | 原因 | 解决方案 |
|----|------|----------|
| **缓存注解不生效** | 未加 `@EnableCaching`；依赖不完整；注解写在了 Controller 层 | 检查启动类注解 + 依赖 + 注解位置 |
| **缓存与数据库不一致** | 新增/修改未加 `@CachePut`，删除未加 `@CacheEvict` | 确保增删改操作同步更新缓存 |
| **缓存 null 值** | 查询方法返回了 null | 添加 `unless = "#result == null"` 不缓存 null |
| **缓存 key 冲突** | 不同方法使用相同 key | 不同方法手动指定不同 key（如 `'allUser'`） |
| **修改配置不生效** | 修改 `application.properties` 后未重启 | 重启 Spring Boot 项目 |

---

## 六、进阶补充

| 方向 | 说明 |
|------|------|
| **缓存切换** | 将 Caffeine 切换为 Redis，只需修改依赖和配置，注解代码无需改动 |
| **策略优化** | 根据业务调整 `maximumSize` 和 `expireAfterWrite` |
| **@Caching 组合** | 实现复杂缓存逻辑（如查询后删除指定缓存） |
| **事务与缓存顺序** | `@Transactional` 应在 `@Cacheable` 之前，避免事务未提交就缓存 |

> **核心原则**：缓存适合"读多写少"的数据（用户详情、字典）；修改频繁的数据（订单）需谨慎使用或缩短有效期。
