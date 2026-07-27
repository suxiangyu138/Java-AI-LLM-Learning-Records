# MyBatis 持久层面试问答清单
> 🎯 基于 MyBatis 实战项目清单，涵盖从基础 CRUD 到企业级缓存优化的面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：MyBatis #{} 和 ${} 有什么区别？各自的使用场景是什么？

**面试官意图：** 考察对 SQL 预编译和安全性的理解，以及实际项目中是否遇到过 SQL 注入问题。

**完美解答：**

`#{}` 和 `${}` 是 MyBatis 中两种参数占位方式，它们的核心区别在于 **是否使用预编译**。

**核心区别对比：**

| 对比维度 | `#{}`（预编译） | `${}`（字符串拼接） |
|---------|----------------|-------------------|
| 处理方式 | 占位符 `?` 替换，参数预编译 | 直接字符串替换到 SQL 中 |
| SQL 注入 | 安全 | 存在注入风险 |
| 性能 | 高（预编译，可复用执行计划） | 低（每次拼接成新 SQL） |
| 类型转换 | 自动类型转换（字符串加引号） | 原样替换 |
| 适用场景 | 绝大多数传参场景 | 动态表名、列名、排序字段 |
| 推荐程度 | 优先使用 | 慎用，必须做白名单校验 |

```java
// #{} 的使用（推荐）
@Select("SELECT * FROM user WHERE id = #{id} AND name = #{name}")
User findByIdAndName(@Param("id") Long id, @Param("name") String name);
// 生成的 SQL：SELECT * FROM user WHERE id = ? AND name = ?

// ${} 的使用（动态表名/列名，需要白名单校验）
@Select("SELECT ${columns} FROM ${tableName} WHERE id = #{id}")
Map<String, Object> findDynamic(@Param("tableName") String tableName,
                                @Param("columns") String columns);
// 注意：必须要对 tableName 和 columns 做白名单校验
```

**白名单校验示例：**

```java
public String validateTableName(String tableName) {
    // 只允许指定的表名
    Set<String> allowedTables = Set.of("user", "order", "product");
    if (!allowedTables.contains(tableName)) {
        throw new SecurityException("非法表名：" + tableName);
    }
    return tableName;
}
```

> ⚠️ **安全警示**：`${}` 如果没有白名单校验，用户传入恶意参数会导致 SQL 注入。比如 `${tableName}` 传入 `"user; DROP TABLE user; --"`，可以直接删除表。

**延伸追问应对：** 面试官可能追问"MyBatis 是如何防止 SQL 注入的"，从预编译机制回答：MyBatis 底层使用 JDBC 的 PreparedStatement，参数通过 `setXxx()` 方法设置，数据库驱动会自动对特殊字符进行转义，从根本上避免 SQL 注入。

---

### Q2：MyBatis 的动态 SQL 有哪些标签？它们的执行原理是什么？

**面试官意图：** 考察对动态 SQL 的掌握程度，这是 MyBatis 最核心也是最常用的功能。

**完美解答：**

MyBatis 提供了丰富的动态 SQL 标签，用于解决复杂的 SQL 拼接场景。

**核心标签一览：**

```xml
<!-- 1. <if>：条件判断 -->
<select id="findByCondition" resultType="User">
    SELECT * FROM user WHERE 1=1
    <if test="name != null and name != ''">
        AND name LIKE CONCAT('%', #{name}, '%')
    </if>
    <if test="status != null">
        AND status = #{status}
    </if>
    <if test="age != null">
        AND age >= #{age}
    </if>
</select>

<!-- 2. <where>：自动处理 WHERE 关键字和多余的 AND/OR -->
<select id="findByCondition" resultType="User">
    SELECT * FROM user
    <where>
        <if test="name != null">AND name = #{name}</if>
        <if test="status != null">AND status = #{status}</if>
    </where>
    <!-- 如果条件都不满足，不会加 WHERE；自动去掉领先的 AND/OR -->
</select>

<!-- 3. <choose> <when> <otherwise>：类似 switch-case -->
<select id="findByRole" resultType="User">
    SELECT * FROM user
    <where>
        <choose>
            <when test="role == 'admin'">
                AND role = 'admin'
            </when>
            <when test="role == 'user'">
                AND role = 'user'
            </when>
            <otherwise>
                AND role IN ('admin', 'user')
            </otherwise>
        </choose>
    </where>
</select>

<!-- 4. <foreach>：集合遍历（批量操作最常用） -->
<insert id="batchInsert">
    INSERT INTO user (name, email, status) VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.name}, #{user.email}, #{user.status})
    </foreach>
</insert>

<select id="findByIds" resultType="User">
    SELECT * FROM user WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>

<!-- 5. <set>：自动处理 UPDATE 中的 SET 和多余的逗号 -->
<update id="updateUser">
    UPDATE user
    <set>
        <if test="name != null">name = #{name},</if>
        <if test="email != null">email = #{email},</if>
        <if test="status != null">status = #{status}</if>
    </set>
    WHERE id = #{id}
</update>

<!-- 6. <trim>：更灵活的前后缀处理 -->
<trim prefix="WHERE" prefixOverrides="AND |OR ">
    <!-- 自定义前缀和后缀覆盖规则 -->
</trim>
```

**执行原理：** MyBatis 在解析 Mapper XML 时，会将动态 SQL 标签解析为一个个的 SqlNode 对象，形成一棵 SqlNode 树。在执行时，遍历这棵树，根据传入的参数值动态拼接 SQL 语句。核心类是 `DynamicSqlSource` 和 `SqlNode`。

> 💡 **面试亮点**：如果能说出动态 SQL 的底层实现是基于 OGNL 表达式引擎进行条件判断，面试官会觉得你对 MyBatis 的理解更深一层。

---

### Q3：MyBatis 的结果映射中，association 和 collection 的区别是什么？怎么处理 N+1 问题？

**面试官意图：** 考察对 MyBatis 复杂查询结果映射的理解，以及是否能写出高性能的关联查询。

**完美解答：**

**association 和 collection 的区别：**

| 特性 | association | collection |
|------|-------------|------------|
| 关联关系 | 一对一 | 一对多 |
| Java 类型 | 单个对象 | 列表（List/Set） |
| 示例 | 用户 -> 用户详情 | 用户 -> 角色列表 |
| SQL 方式 | JOIN 连表或嵌套查询 | JOIN 连表或嵌套查询 |

```xml
<!-- association：一对一（用户 -> 用户详情） -->
<resultMap id="UserDetailMap" type="User">
    <id property="id" column="user_id"/>
    <result property="username" column="username"/>
    <!-- 用户详情，一对一关系 -->
    <association property="profile" javaType="Profile">
        <id property="id" column="profile_id"/>
        <result property="phone" column="phone"/>
        <result property="avatar" column="avatar"/>
    </association>
</resultMap>

<!-- collection：一对多（用户 -> 订单列表） -->
<resultMap id="UserOrderMap" type="User">
    <id property="id" column="user_id"/>
    <result property="username" column="username"/>
    <!-- 订单列表，一对多关系 -->
    <collection property="orders" ofType="Order" column="user_id"
                select="com.example.mapper.OrderMapper.findByUserId" 
                fetchType="lazy">
        <id property="id" column="order_id"/>
        <result property="amount" column="amount"/>
    </collection>
</resultMap>
```

**N+1 查询问题及解决方案：**

**什么是 N+1：** 查询 1 个用户需要 N 次额外查询（N 是该用户的订单数）。比如查 100 个用户，每个用户又有 10 个订单，总共需要执行 1 + 100 = 101 次 SQL。

```xml
<!-- 问题写法：嵌套查询导致 N+1 -->
<resultMap id="UserOrderMap" type="User">
    <collection property="orders" ofType="Order"
                select="findOrdersByUserId"  <!-- 每个用户都执行一次！ -->
                column="id"/>
</resultMap>

<!-- 解决方案 1：JOIN 连表一次性查询 -->
<resultMap id="UserOrderMap" type="User">
    <id property="id" column="id"/>
    <result property="username" column="username"/>
    <collection property="orders" ofType="Order">
        <id property="id" column="order_id"/>
        <result property="amount" column="amount"/>
    </collection>
</resultMap>

<select id="findUserWithOrders" resultMap="UserOrderMap">
    SELECT u.*, o.id as order_id, o.amount
    FROM user u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.id IN
    <foreach collection="ids" item="id" separator="," open="(" close=")">
        #{id}
    </foreach>
</select>

<!-- 解决方案 2：使用 @NestedSelect 但开启懒加载 -->
<!-- application.yml 配置 -->
mybatis-plus:
  configuration:
    lazy-loading-enabled: true    # 开启懒加载
    aggressive-lazy-loading: false # 按需加载，不一次性加载全部
    lazy-load-trigger-methods: ""  # 任何方法调用都会触发懒加载
```

> ⚠️ **性能关键**：大数据量场景下尽量不要使用嵌套查询（`select` 属性），因为它会执行 N+1 次 SQL。优先使用 JOIN 连表查询加适当的分页。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你们项目中的多表关联权限管理系统是怎么实现的？用到了哪些 MyBatis 特性？

**面试官意图：** 考察 MyBatis 复杂关联查询的实战能力，以及数据库设计水平。

**完美解答：**

这是一个"用户 -> 角色 -> 权限"三层的 RBAC 权限系统，核心是多对多关系的查询优化。

**数据库设计：**

```sql
-- 核心表
user(id, username, password)
role(id, name, code)          -- 如：ROLE_ADMIN, ROLE_USER
permission(id, name, code)    -- 如：user:create, order:query

-- 关联表
user_role(user_id, role_id)
role_permission(role_id, permission_id)
```

**完整的结果映射：**

```xml
<!-- 用户 -> 角色列表 -> 权限列表（三层嵌套） -->
<resultMap id="UserWithRolesAndPermissions" type="User">
    <id property="id" column="id"/>
    <result property="username" column="username"/>
    
    <!-- 用户的角色列表 -->
    <collection property="roles" ofType="Role" column="id">
        <id property="id" column="role_id"/>
        <result property="name" column="role_name"/>
        <result property="code" column="role_code"/>
        
        <!-- 角色的权限列表 -->
        <collection property="permissions" ofType="Permission">
            <id property="id" column="perm_id"/>
            <result property="name" column="perm_name"/>
            <result property="code" column="perm_code"/>
        </collection>
    </collection>
</resultMap>

<!-- 一次性 JOIN 查询，避免 N+1 -->
<select id="selectUserWithPermissions" resultMap="UserWithRolesAndPermissions">
    SELECT 
        u.id, u.username,
        r.id as role_id, r.name as role_name, r.code as role_code,
        p.id as perm_id, p.name as perm_name, p.code as perm_code
    FROM user u
    LEFT JOIN user_role ur ON u.id = ur.user_id
    LEFT JOIN role r ON ur.role_id = r.id
    LEFT JOIN role_permission rp ON r.id = rp.role_id
    LEFT JOIN permission p ON rp.permission_id = p.id
    WHERE u.id = #{userId}
</select>
```

**MyBatis-Plus 实现方式（更简洁）：**

```java
// 实体类注解
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    
    @TableField(exist = false)  // 非数据库字段
    private List<Role> roles;
    
    @TableField(exist = false)
    private List<Permission> permissions;
}

// Service 层组合查询
@Service
public class PermissionService {
    
    public User getUserWithPermissions(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;
        
        // 查询角色
        List<Role> roles = roleMapper.selectRolesByUserId(userId);
        user.setRoles(roles);
        
        // 查询权限
        List<Permission> permissions = permissionMapper.selectPermissionsByUserId(userId);
        user.setPermissions(permissions);
        
        return user;
    }
}
```

> 💡 **面试加分**：除了实现查询，我们还在 Redis 中缓存了用户的权限数据，避免每次请求都访问数据库。当管理员修改角色权限时，通过 `@CacheEvict` 清除相关用户的缓存。

---

### Q5：你们项目中用 MyBatis-Plus 做了哪些事情？手写 MyBatis 和用 MP 怎么选择？

**面试官意图：** 考察对 MyBatis-Plus 的理解和工程化能力，以及技术选型的判断力。

**完美解答：**

我们在项目中大量使用 MyBatis-Plus 来提升开发效率，同时在复杂查询场景保留手写 MyBatis 的灵活性。

**项目中的 MP 实践：**

```java
// 1. 继承 BaseMapper，零 SQL 实现基础 CRUD
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 基础 CRUD 方法直接继承，无需手写
    // insert(T), deleteById(Serializable), updateById(T), selectById(Serializable)
    // selectList(Wrapper<T>), selectPage(Page<T>, Wrapper<T>)
}

// 2. Lambda 条件构造器（类型安全，不写错字段名）
@Service
public class UserService {
    
    public List<User> searchUsers(String keyword, Integer status, String sortField) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.like(StringUtils.isNotBlank(keyword), User::getName, keyword)  // 动态条件
               .eq(status != null, User::getStatus, status)                    // 动态条件
               .orderByDesc(StringUtils.isNotBlank(sortField), User::getCreateTime)  // 排序
               .last("LIMIT 100");  // 追加 SQL
               
        return userMapper.selectList(wrapper);
    }
}

// 3. 自动填充（创建时间和更新时间自动赋值）
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
}

// 4. 逻辑删除（数据不真的删除，而是标记状态）
@Data
public class User {
    @TableLogic  // 逻辑删除字段
    @TableField(select = false)  // 查询时不返回此字段
    private Integer deleted;
}
// MP 会自动将 delete 操作转为 UPDATE deleted=1，将查询自动加上 WHERE deleted=0
```

**MyBatis vs MyBatis-Plus 选择策略：**

| 场景 | 推荐 | 原因 |
|------|------|------|
| 简单 CRUD | MyBatis-Plus | 几乎零代码，效率高 |
| 复杂 JOIN 查询 | MyBatis 手写 XML | SQL 更可控，优化更灵活 |
| 分页查询 | MyBatis-Plus Page | 开箱即用，自动统计总数 |
| 多表关联 | MyBatis 手写 | 避免 MP 的封装导致的性能问题 |
| 动态 SQL 复杂 | MyBatis-Plus Wrapper + 手写混用 | 简单条件用 Wrapper，复杂逻辑手写 |
| 新项目快速开发 | MyBatis-Plus | 开发效率高，代码量减少 70% |

> 🎯 **最佳实践**：实际项目中，我们采用 **MyBatis-Plus 做 80% 的常规操作 + 手写 MyBatis 做 20% 的复杂查询** 的策略，兼顾开发效率和灵活性。

---

### Q6：你们项目中用 PageHelper 做分页时遇到过什么问题？怎么解决的？

**面试官意图：** 考察对分页插件的理解深度，以及是否踩过坑。

**完美解答：**

PageHelper 使用非常广泛，但如果使用不当，有很多隐藏的坑。

**正确用法：**

```java
// 正确的使用方式
public PageInfo<User> listUsers(int pageNum, int pageSize) {
    // 分页设置（必须在 Mapper 方法调用之前）
    PageHelper.startPage(pageNum, pageSize);
    
    // Mapper 查询
    List<User> users = userMapper.selectList(condition);
    
    // 包装成分页对象
    return new PageInfo<>(users);
}
```

**常见问题及解决方案：**

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 分页不生效 | `startPage` 后跟了不是 Mapper 调用（如查询集合再循环） | 确保 `startPage` 后紧跟着第一个 Mapper 调用 |
| 分页无效但统计了总数 | 查询了多条 SQL，PageHelper 拦截了非目标 SQL | 使用 `PageHelper.startPage(pageNum, pageSize, true)` 只拦截最近的一条 SQL |
| 线程安全问题 | 多线程环境下 `startPage` 和 Mapper 调用之间被干扰 | 使用 `try-finally` 块确保线程清理 |
| 统计 SQL 性能差 | PageHelper 自动生成的 `COUNT` 查询很慢 | 使用手写优化 `COUNT` 的方法 |

```java
// 线程安全问题：使用 try-finally 保证线程变量清理
public PageInfo<User> safePageQuery(int pageNum, int pageSize) {
    try {
        PageHelper.startPage(pageNum, pageSize);
        List<User> users = userMapper.selectList(condition);
        return new PageInfo<>(users);
    } finally {
        // 清理 ThreadLocal，防止线程池复用导致数据错乱
        PageHelper.clearPage();
    }
}

// 大表分页优化：使用手写 COUNT 避免性能问题
@Select("SELECT COUNT(*) FROM user WHERE status = #{status} AND name LIKE #{name}")
Long countUsers(@Param("status") Integer status, @Param("name") String name);

@Select("SELECT * FROM user WHERE status = #{status} AND name LIKE #{name} ORDER BY id LIMIT #{offset}, #{limit}")
List<User> pageUsers(@Param("status") Integer status, @Param("name") String name,
                     @Param("offset") Integer offset, @Param("limit") Integer limit);
```

> ⚠️ **关键经验**：PageHelper 原理是使用 ThreadLocal 存储分页参数，在拦截器中拦截 Mapper 调用并拼接分页 SQL。但由于 ThreadLocal 在线程池场景下需要手动清理，否则会出现"分页参数错乱"的诡异 Bug。**每次使用一定要在 finally 块中调用 clearPage()**。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：MyBatis 的一级缓存和二级缓存在什么场景下会出现问题？

**面试官意图：** 考察对 MyBatis 缓存机制的理解深度，以及是否有过因为缓存导致数据不一致的事故经验。

**完美解答：**

MyBatis 的缓存机制分为两级，理解它们的原理和局限是写出正确代码的基础。

**一级缓存（SqlSession 级别）：**

```java
// 一级缓存默认开启，同一个 SqlSession 内共享
// 问题场景：事务中多次查询，数据被其他事务修改了
@Transactional
public void testL1Cache() {
    // 第一次查询 -> 查数据库
    User user1 = userMapper.selectById(1L);
    
    // 假设此时另一个事务修改了这条数据并提交
    
    // 第二次查询 -> 返回一级缓存结果（脏数据！）
    User user2 = userMapper.selectById(1L);
    
    // user1 == user2 (true) - 但数据库中的数据已经变了
}
```

**二级缓存（Mapper 级别，跨 SqlSession）：**

```xml
<!-- 开启二级缓存 -->
<mapper namespace="com.example.mapper.UserMapper">
    <cache eviction="LRU" 
           flushInterval="60000" 
           size="512" 
           readOnly="true"/>
</mapper>
```

```java
// 二级缓存问题场景
// 场景 1：关联查询数据不一致
@Autowired
private ProductMapper productMapper;

public void testL2CacheIssue() {
    // 用户 A 查询商品列表
    List<Product> products1 = productMapper.selectWithCategory();  
    // 二级缓存了结果
    
    // 此时商品分类被修改
    categoryMapper.updateName(1L, "新分类名");
    
    // 用户 B 查询商品列表
    List<Product> products2 = productMapper.selectWithCategory();
    // 返回缓存的旧结果！分类名称还是旧的
}
```

**缓存问题对照表：**

| 缓存级别 | 作用域 | 默认开启 | 问题 | 解决方案 |
|---------|--------|---------|------|---------|
| 一级缓存 | SqlSession | 是 | 事务内数据旧 | 批量操作后 `clearCache()` |
| 二级缓存 | Mapper Namespace | 否 | 跨表缓存不一致 | 开启 `flushCache="true"`，或不用二级缓存 |

```java
// 最佳实践：批量操作后手动清空缓存
public void batchUpdateUserStatus(List<Long> ids, Integer status) {
    // 批量更新
    userMapper.batchUpdateStatus(ids, status);
    
    // 清空一级缓存
    sqlSession.clearCache();
    
    // 如果开启了二级缓存：清空指定 namespace 的缓存
    // (无法手动清空，只能通过执行 UPDATE 操作自动刷新)
}
```

> 🎯 **我的建议**：在分布式微服务架构下，**不建议开启 MyBatis 二级缓存**。因为多个微服务实例共享的缓存一致性无法保证，且可能和 Redis 缓存冲突。Redis 做分布式缓存就够了，MyBatis 的二级缓存更适合单体应用。

---

### Q8：在一个订单系统中，如何用 MyBatis 实现乐观锁防超卖？配合 Redis 可以怎么优化？

**面试官意图：** 考察 MyBatis 在高并发场景下的应用，以及 MySQL 乐观锁技术的实际使用。

**完美解答：**

**MyBatis 乐观锁实现：**

```xml
<!-- 核心 SQL：版本号 + 库存条件 -->
<update id="deductStockOptimistic">
    UPDATE product 
    SET 
        stock = stock - #{quantity},
        version = version + 1
    WHERE 
        id = #{productId}
        AND stock >= #{quantity}
        AND version = #{version}  <!-- 版本号匹配才更新成功 -->
</update>
```

```java
// Service 层的重试逻辑
@Service
public class ProductService {
    
    @Retryable(value = OptimisticLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public boolean deductStock(Long productId, Integer quantity) {
        // 1. 获取当前版本号
        Product product = productMapper.selectById(productId);
        
        // 2. 更新库存（带上版本号条件）
        int affected = productMapper.deductStockOptimistic(productId, quantity, product.getVersion());
        
        if (affected == 0) {
            // 更新失败：版本号变更或被修改
            throw new OptimisticLockException("库存扣减失败，请重试");
        }
        
        return true;
    }
}
```

**配合 Redis 的优化方案：**

```java
@Service
public class OptimizedStockService {
    
    public boolean deductStockWithCache(Long productId, Integer quantity) {
        // Step 1: Redis 预扣（高性能，扛大流量）
        String key = "stock:" + productId;
        Long remain = redisTemplate.opsForValue().decrement(key, quantity);
        
        if (remain < 0) {
            // 扣超了，回滚 Redis
            redisTemplate.opsForValue().increment(key, quantity);
            return false;
        }
        
        // Step 2: 异步落库（MQ + 乐观锁）
        rabbitTemplate.convertAndSend("stock.deduct", new StockMessage(productId, quantity));
        
        return true;
    }
}

// 异步消费者：数据库最终扣减
@RabbitListener(queues = "stock.deduct.queue")
public void handleStockDeduct(StockMessage message) {
    try {
        // 乐观锁扣减，重试 3 次
        retryTemplate.execute(context -> {
            int affected = productMapper.deductStockOptimistic(message.getProductId(), 
                message.getQuantity(), getVersion(message.getProductId()));
            if (affected == 0) {
                throw new RetryableException("乐观锁冲突，重试");
            }
            return null;
        });
    } catch (Exception e) {
        log.error("库存扣减失败，已回滚 Redis 库存", e);
        // 回滚 Redis
        redisTemplate.opsForValue().increment("stock:" + message.getProductId(), message.getQuantity());
    }
}
```

> 💡 **架构经验**：在高并发场景下，数据库乐观锁的写入能力有限（单表 1000 TPS 左右）。所以先通过 Redis 预扣扛流量（单机 10 万 QPS），MQ 做缓冲，数据库通过乐观锁做最终写入。这样的"缓存层 + MQ 层 + 数据库层"三层方案，可以支撑秒杀级别的并发量。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：线上突然发现一条慢 SQL 导致接口超时，你怎么定位和优化？

**面试官意图：** 考察 SQL 性能优化的实战能力。

**完美解答：**

**排查步骤：**

```sql
-- Step 1: 开启 MySQL 慢查询日志，找到慢 SQL
-- 配置 my.cnf
slow_query_log = ON
long_query_time = 1  -- 超过 1 秒的 SQL 记录
slow_query_log_file = /var/log/mysql/slow.log

-- Step 2: 使用 EXPLAIN 分析执行计划
EXPLAIN SELECT * FROM orders 
WHERE user_id = 12345 
  AND status = 'PAID' 
  AND create_time > '2025-01-01'
ORDER BY create_time DESC 
LIMIT 10;
```

**EXPLAIN 结果分析：**

| 字段 | 值 | 含义 | 是否正常 |
|------|-----|------|---------|
| type | ALL | 全表扫描 | 异常，应该至少到 `ref` |
| possible_keys | idx_user_id | 有可用的索引 | 好 |
| key | NULL | 没有实际使用索引 | 异常！ |
| rows | 500000 | 扫描了 50 万行 | 必须优化 |
| Extra | Using where; Using filesort | 使用了文件排序 | 性能差 |

**优化方案：**

```sql
-- 问题分析：SQL 中有 user_id + status + create_time 三个条件 + 排序
-- 但只有 user_id 的独立索引，导致 MySQL 选择了全表扫描

-- 解决方案 1：建立复合索引
CREATE INDEX idx_user_status_time ON orders(user_id, status, create_time);
-- 复合索引最左前缀原则：user_id -> status -> create_time
-- 该索引完整覆盖了 WHERE 条件和 ORDER BY 排序

-- 解决方案 2：覆盖索引（索引里已经有了需要的数据，不用回表）
CREATE INDEX idx_covering ON orders(user_id, status, create_time, id, amount);

-- 优化后的执行计划：
-- type: ref（命中索引）
-- key: idx_user_status_time
-- rows: 50（从 50 万降到了 50 行）
-- Extra: Using index condition（使用了索引下推，不再 filesort）
```

**MyBatis XML 中的优化：**

```xml
<!-- 优化后的 Mapper -->
<select id="findPaidOrders" resultType="Order">
    SELECT id, user_id, status, create_time, amount
    FROM orders
    WHERE user_id = #{userId}
      AND status = 'PAID'
      AND create_time > #{startTime}
    ORDER BY create_time DESC
    LIMIT #{limit}
</select>
```

> 💡 **优化原则**：一条查询的性能 80% 由索引决定，20% 由 SQL 写法决定。优化的顺序是：先看 EXPLAIN -> 加合适的索引 -> 改写 SQL。不要一上来就想着改代码，改索引往往是性价比最高的方案。

---

### Q10：MyBatis 项目中常见的性能问题有哪些？你遇到过最坑的问题是什么？

**面试官意图：** 考察候选人的 MyBatis 实战经验，以及排查解决复杂问题的能力。

**完美解答：**

**MyBatis 常见性能问题 TOP 5：**

| 问题 | 现象 | 原因 | 解决方案 |
|------|------|------|---------|
| N+1 查询 | 查询列表时重复执行大量 SQL | 嵌套查询 `select` 属性 | 换 JOIN 查询 |
| 循环内查询 | 接口执行极慢 | Service 层 `for` 循环内调 Mapper | 改用批量查询 |
| 大结果集溢内存 | OOM | 未分页查询全量数据 | 强制分页 + 流式查询 |
| 缓存数据不一致 | 读到旧数据 | 一级/二级缓存未清理 | 更新操作后清除缓存 |
| 索引未命中 | 接口响应慢 | SQL 未走索引或索引不合适 | 优化索引 |

**我遇到的最坑的问题：MyBatis 批量插入性能杀手**

```java
// 错误写法：逐条插入（每秒只能插几十条）
public void batchInsertWrong(List<User> users) {
    for (User user : users) {
        userMapper.insert(user);  // 循环内逐条插入，每次事务提交
    }
}

// 正确写法 1：MyBatis 批量插入（每秒可以插几千条）
public void batchInsertCorrect(List<User> users) {
    userMapper.batchInsert(users);  // 单条 SQL 批量插入
}

// XML
<insert id="batchInsert">
    INSERT INTO user (name, email) VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.name}, #{user.email})
    </foreach>
</insert>

// 正确写法 2：MyBatis BATCH 执行器模式（适合超大批量）
@Bean
public SqlSessionTemplate batchSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
}

@Service
public class BatchService {
    
    @Autowired
    @Qualifier("batchSqlSessionTemplate")
    private SqlSessionTemplate batchSqlSession;
    
    public void batchInsertLarge(List<User> users) {
        UserMapper batchMapper = batchSqlSession.getMapper(UserMapper.class);
        for (int i = 0; i < users.size(); i++) {
            batchMapper.insert(users.get(i));  // 不会立即提交，而是攒在批处理中
            if (i % 1000 == 0) {
                batchSqlSession.flushStatements();  // 每 1000 条提交一次
            }
        }
        batchSqlSession.flushStatements();
    }
}
```

> ⚠️ **经验教训**：我曾经接手过一个项目，用户导出功能跑几十万行数据时直接 OOM。排查发现查询没有分页，全量数据加载到内存中，然后用 Java 逐条处理。优化后改用 MyBatis 游标查询 + 流式写入，内存占用从 2GB 降到 20MB。

---

### Q11：如果公司让你从零开始选择持久层框架，MyBatis 和 JPA 你怎么选？为什么？

**面试官意图：** 考察技术选型能力和对两种持久层方案的深度理解。

**完美解答：**

这不是一个"谁更好"的问题，而是一个"谁更适合"的问题。我从团队、项目、运维三个维度给出选型建议。

**全面对比：**

| 维度 | MyBatis | JPA/Hibernate |
|------|---------|---------------|
| SQL 控制 | 完全手写，100% 可控 | 自动生成，复杂查询需 JPQL/原生 SQL |
| 学习曲线 | 低，懂 SQL 就能上手 | 高，需要理解持久化上下文、懒加载、缓存 |
| 复杂查询 | 优秀，手写 SQL 可精细优化 | 困难，N+1 问题需要特别注意 |
| 字段映射 | 手写 ResultMap | 字段自动映射，实体类管理 |
| 性能优化 | 可控性强，DBA 友好 | 依赖框架优化，黑盒 |
| 动态 SQL | XML 标签，功能强大 | Specifications / QueryDSL |
| 批处理 | 支持 BATCH 执行器 | 支持，但需要手动 Flush |
| 代码生成 | MyBatis-Plus Generator | JHipster |
| 缓存 | 二级缓存，适合单体 | 二级缓存 + 查询缓存 |

**我的选型决策树：**

```
项目是否复杂查询居多？
  └── 是 -> MyBatis（复杂 SQL 手写可控）
  └── 否 ->
      团队对 JPA 是否熟练？
        └── 是 -> JPA（标准 CRUD 开发快）
        └── 否 -> MyBatis-Plus（学习成本低）
项目 DBA 是否参与 SQL Review？
  └── 是 -> MyBatis（DBA 可以直接看 XML）
  └── 否 -> MyBatis-Plus（ORM 自动生成 SQL）
```

> 🎯 **我的结论**：如果项目以复杂联表查询为主，或者有 DBA 团队主导 SQL 审核，**选 MyBatis**；如果是标准 CRUD 为主，团队有 JPA 经验，**选 JPA**。国内企业级项目 MyBatis 占比超过 70%，所以从就业角度看，MyBatis 是必选项。

---

## 💎 面试加分金句
- "MyBatis 不是 ORM 框架，它是一个'半自动化'的数据映射框架——它不做对象关联映射，而是把 SQL 的控制权完全交给开发者。"
- "我在项目中始终坚持的原则是：能用 `#{}` 绝不用 `${}`，必须用 `${}` 的时候一定有白名单校验，这是数据安全的底线。"
- "`association` 和 `collection` 的选择标准很简单：返回一个对象用前者，返回列表用后者。"
- "N+1 查询是 MyBatis 最高频的性能问题，解决思路也很简单：能用 JOIN 别用嵌套查询。"
- "MyBatis-Plus 解决的是 80% 的常规 CRUD，剩下的 20% 复杂查询还是要靠手写 SQL——高手懂得在框架和灵活性之间找到平衡。"

## 📋 高频追问清单
| 追问方向 | 应对策略 |
|----------|----------|
| MyBatis 中 `resultType` 和 `resultMap` 的区别？ | resultType 自动映射，resultMap 自定义映射 |
| MyBatis 分页和 MySQL 分页的区别？ | MyBatis 分页是逻辑拦截，MySQL 分页是 LIMIT 语法 |
| MyBatis 如何处理枚举类型？ | 使用 `TypeHandler` 或 MP 的通用枚举处理 |
| `ExecutorType.SIMPLE/REUSE/BATCH` 的区别？ | SIMPLE 每次新建 Statement，REUSE 重用，BATCH 批处理 |
| MyBatis 如何防止 SQL 注入？ | 预编译 + `#{}` 参数替换 + 输入校验 |
| MyBatis-Plus 的 `selectOne` 如果查到多条会怎样？ | 抛出异常 `TooManyResultsException` |

## 🔗 关联知识点
- [Spring 7个必做项目面试问答](./Spring7个必做项目-面试问答.md)
- [Maven面试问答](./Maven必做项目清单-面试问答.md)
- [SpringCloudAlibaba面试问答](./SpringCloudAlibaba必做项目-面试问答.md)
