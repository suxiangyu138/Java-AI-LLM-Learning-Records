# MyBatis 缓存详解（一级缓存 + 二级缓存）

> **文档定位**：Java 后端企业级技术文档 | MyBatis 缓存机制  
> **前置基础**：MyBatis 基础用法、SqlSession、Mapper 接口  
> **关联章节**：MyBatis 入门 → MyBatis 动态 SQL → **本章** → Spring 集成 MyBatis

---

## 一、核心概念

### 1.1 什么是 MyBatis 缓存

MyBatis 缓存是 MyBatis 为提升数据库查询性能设计的核心机制，本质是将频繁查询且数据不常变化的结果临时存储在内存中。当再次执行相同查询时，直接从内存获取结果，**无需重复访问数据库**，减少数据库 IO 操作。

> **类比**：缓存就像手机相册的"最近照片"，频繁查看的照片临时存在手机内存中，无需每次都从相册文件中读取，节省时间和资源。

### 1.2 缓存的核心作用

| 作用 | 说明 |
|------|------|
| **提升查询性能** | 减少数据库连接和 SQL 执行次数，尤其适合频繁查询、数据稳定的场景（如字典数据、用户基础信息） |
| **降低数据库压力** | 避免高频查询对数据库造成的负载，减少 IO 开销 |
| **简化开发** | 一级缓存默认开启，无需额外编码，开箱即用 |

### 1.3 MyBatis 缓存分类

MyBatis 提供两级缓存，查询顺序为：**先查二级缓存 → 再查一级缓存 → 最后查数据库**，两级缓存协同工作：

| 缓存级别 | 核心特点 | 生命周期 | 默认状态 |
|----------|----------|----------|----------|
| **一级缓存**（本地缓存） | SqlSession 级别，仅当前会话有效，不可跨会话共享 | 与 SqlSession 一致，关闭会话则缓存失效 | **默认开启**，无需配置 |
| **二级缓存**（全局缓存） | SqlSessionFactory 级别，可跨所有 SqlSession 共享 | 与应用一致，启动创建，关闭销毁 | **默认关闭**，需手动配置 |

> MyBatis 还支持集成第三方缓存（Redis、Ehcache），替代默认二级缓存，适用于分布式项目。

---

## 二、底层原理

### 2.1 一级缓存原理

一级缓存是 SqlSession 级别的缓存，MyBatis 在创建 SqlSession 时，内部维护一个 **HashMap** 作为缓存容器。

**核心流程**：

```
第一次查询 findById(1)：
  SqlSession → 检查一级缓存（无数据）→ 执行 SQL 查数据库 → 结果存入缓存 → 返回结果

第二次查询 findById(1)（同一 SqlSession）：
  SqlSession → 检查一级缓存（有数据）→ 直接返回缓存结果（不查数据库）

执行增删改操作 或 close() / clearCache()：
  → 清空一级缓存（避免数据不一致）
```

### 2.2 二级缓存原理

二级缓存是 SqlSessionFactory 级别的缓存，所有通过该工厂创建的 SqlSession 都能共享。

**核心流程**：

```
SqlSession1 查询 → 查二级缓存（无）→ 查一级缓存（无）→ 查数据库 → 结果存入两级缓存
SqlSession1.close() → 一级缓存数据同步到二级缓存
SqlSession2 查询 → 查二级缓存（有）→ 直接返回（不查数据库）

任意 SqlSession 执行增删改 → 清空当前 Mapper 的二级缓存
```

### 2.3 两级缓存协同机制

```
查询请求
    ↓
检查二级缓存（SqlSessionFactory 级别）
    ├── 命中 → 返回结果（结束）
    └── 未命中 ↓
检查一级缓存（SqlSession 级别）
    ├── 命中 → 返回结果（结束）
    └── 未命中 ↓
执行 SQL 查询数据库
    ↓
结果存入一级缓存 + 二级缓存
    ↓
返回结果
```

---

## 三、代码实现

### 3.1 一级缓存验证

```java
/**
 * 一级缓存验证 —— 同一 SqlSession 内相同查询从缓存获取。
 */
@Test
public void testFirstLevelCache() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

    // 同一个 SqlSession，验证一级缓存
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

        // 第一次查询：访问数据库，存入一级缓存
        User user1 = userMapper.findById(1);
        System.out.println("第一次查询结果：" + user1);

        // 第二次查询：从一级缓存获取，不访问数据库
        User user2 = userMapper.findById(1);
        System.out.println("第二次查询结果：" + user2);

        // 验证：两个对象是同一个引用（缓存复用）
        System.out.println("两次查询结果是否相同：" + (user1 == user2)); // true
    }
}
```

### 3.2 二级缓存配置（三步开启）

#### 步骤 1：开启全局二级缓存（mybatis-config.xml）

```xml
<configuration>
    <!-- 开启全局二级缓存（全局开关） -->
    <settings>
        <setting name="cacheEnabled" value="true"/> <!-- true=开启，false=关闭 -->
    </settings>

    <mappers>
        <package name="com.example.mapper"/>
    </mappers>
</configuration>
```

#### 步骤 2：为当前 Mapper 开启二级缓存

**方式 1：注解配置**

```java
package com.example.mapper;

import com.example.pojo.User;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper 接口 —— 开启二级缓存。
 */
@CacheNamespace
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Integer id);

    // 其他方法（addUser、updateUser 等）
}
```

**方式 2：XML 配置**

```xml
<mapper namespace="com.example.mapper.UserMapper">
    <!-- 开启当前 Mapper 的二级缓存 -->
    <cache/>
    <!-- 其他 SQL 标签 -->
</mapper>
```

#### 步骤 3：实体类实现序列化接口

```java
package com.example.pojo;

import java.io.Serializable;

/**
 * 用户实体类 —— 实现 Serializable 支持二级缓存序列化存储。
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;

    // 无参构造、getter/setter、toString 方法
}
```

### 3.3 二级缓存验证

```java
/**
 * 二级缓存验证 —— 不同 SqlSession 之间共享缓存数据。
 */
@Test
public void testSecondLevelCache() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

    User user1;
    // 第一个 SqlSession：查询并关闭，数据同步到二级缓存
    try (SqlSession sqlSession1 = sqlSessionFactory.openSession()) {
        UserMapper mapper1 = sqlSession1.getMapper(UserMapper.class);
        user1 = mapper1.findById(1);
        System.out.println("SqlSession1 查询结果：" + user1);
    } // 关闭 SqlSession1，一级缓存 → 二级缓存

    // 第二个 SqlSession：从二级缓存获取，不访问数据库
    try (SqlSession sqlSession2 = sqlSessionFactory.openSession()) {
        UserMapper mapper2 = sqlSession2.getMapper(UserMapper.class);
        User user2 = mapper2.findById(1);
        System.out.println("SqlSession2 查询结果：" + user2);
        System.out.println("跨会话缓存是否共享：" + user1.equals(user2)); // true
    }
}
```

### 3.4 禁止单个方法使用缓存

```java
public interface UserMapper {

    /** 禁止当前方法使用缓存（不存也不读缓存） */
    @Options(useCache = false)
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findByIdNoCache(Integer id);

    /** 执行增删改后清空当前 Mapper 的二级缓存 */
    @CacheEvict(allEntries = true)
    @Insert("INSERT INTO user (username, password) VALUES (#{username}, #{password})")
    int addUser(User user);
}
```

### 3.5 二级缓存详细配置

```xml
<cache
    eviction="LRU"          <!-- 缓存回收策略，默认 LRU（最近最少使用） -->
    flushInterval="60000"   <!-- 缓存刷新间隔（毫秒），60 秒自动刷新 -->
    readOnly="false"        <!-- false 返回对象副本（安全），true 返回引用（高性能但不安全） -->
    size="1024"             <!-- 缓存最多存储的对象数量，默认 1024 -->
/>
```

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `eviction` | 回收策略：`LRU`（最近最少使用）、`FIFO`（先进先出） | `LRU` |
| `flushInterval` | 自动刷新间隔（ms） | 根据业务数据变化频率设置 |
| `readOnly` | `false` 返回副本（安全），`true` 返回引用（快但不安全） | `false` |
| `size` | 最大缓存对象数 | 根据内存和业务量评估 |

---

## 四、实战要点

### 4.1 一级缓存注意事项

| 注意点 | 说明 |
|--------|------|
| **不可跨 SqlSession** | 不同 SqlSession 即使相同查询也各自查数据库 |
| **增删改清空缓存** | `add`/`update`/`delete` 操作自动清空当前 SqlSession 缓存 |
| **默认开启** | 无需配置，但无法关闭（只能 `clearCache()` 手动清空） |

### 4.2 二级缓存开启条件 Checklist

- [ ] `mybatis-config.xml` 中 `cacheEnabled = true`
- [ ] Mapper 接口添加 `@CacheNamespace` 或 XML 中添加 `<cache/>`
- [ ] 实体类实现 `Serializable` 接口

### 4.3 第三方缓存集成（分布式场景）

MyBatis 默认二级缓存是本地内存缓存，分布式项目需集成第三方缓存：

1. 导入 Redis/Ehcache 相关依赖（`pom.xml`）
2. 创建缓存实现类，实现 MyBatis 的 `Cache` 接口
3. 在 Mapper 接口上配置 `@CacheNamespace(implementation = RedisCache.class)`

---

## 五、避坑总结

### 5.1 常见问题与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| **二级缓存不生效** | 全局缓存未开启 / Mapper 未配置 / 实体类未序列化 | 核对三步开启条件，逐项检查 |
| **缓存数据与数据库不一致** | 增删改操作未清空缓存 | 添加 `@CacheEvict(allEntries = true)` |
| **分布式缓存无法共享** | 默认二级缓存是本地内存缓存 | 集成 Redis/Ehcache 实现分布式缓存 |
| **一级缓存导致数据重复** | 同一 SqlSession 多次查询返回同一对象引用 | 查询后使用对象副本（`BeanUtils.copyProperties`） |

### 5.2 缓存使用禁忌

- 实时性要求高的查询（如库存查询）**不适合缓存**
- 数据频繁变化的表 **不适合缓存**
- 不要同时对同一 Mapper 使用注解和 XML 配置（选一种）

---

## 六、企业级最佳实践

### 6.1 缓存选型策略

| 场景 | 推荐方案 |
|------|----------|
| 单应用、数据变化少 | 默认二级缓存（内存） |
| 字典数据、配置数据 | 二级缓存 + 较长刷新间隔 |
| 分布式多实例 | 集成 Redis 分布式缓存 |
| 高并发查询、实时要求高 | Redis + 本地 Caffeine 多级缓存 |

### 6.2 缓存使用规范

- **选择性开启**：不是所有 Mapper 都需要二级缓存，只对查询频繁、数据稳定的 Mapper 开启
- **及时清空**：所有增删改方法必须添加 `@CacheEvict`
- **设置合理过期**：通过 `flushInterval` 控制缓存有效期
- **避免缓存穿透**：对于不存在的 key，缓存空值（短过期时间）

### 6.3 本章小结

1. 一级缓存：SqlSession 级别，默认开启，无需配置，增删改会清空
2. 二级缓存：SqlSessionFactory 级别，需手动开启，核心三步：全局开关 + Mapper 配置 + 实体类序列化
3. 缓存选型：单应用用默认二级缓存，分布式项目集成 Redis 等第三方缓存
