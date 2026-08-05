# MyBatis 面试宝典
> 基于课程大纲全面覆盖 MyBatis 面试高频考点，从 ORM 原理到源码级分析

## 目录
1. [一、基础概念速答](#一基础概念速答15-20题)
2. [二、深度原理剖析](#二深度原理剖析10-15题)
3. [三、实战场景题](#三实战场景题8-12题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（15-20题）

### 1.1 什么是 MyBatis？
MyBatis 是一个优秀的**持久层框架**，它封装了 JDBC 的重复性操作，通过 XML 或注解方式配置 SQL 语句，实现对象与数据库记录的映射（ORM）。它与 Hibernate 不同，MyBatis 是**半自动 ORM**，开发者需要手写 SQL，但获得更高的灵活性和性能优化空间。

### 1.2 MyBatis 与 Hibernate 的区别是什么？
| 特性 | MyBatis | Hibernate |
|------|---------|-----------|
| SQL 控制 | 手写 SQL，精细控制 | 自动生成 SQL |
| 学习曲线 | 低，只需熟悉 SQL | 高，需掌握 HQL 和映射策略 |
| 性能调优 | 直接优化 SQL | 需调整缓存、抓取策略等 |
| 适用场景 | 复杂查询、大型项目、需 SQL 优化的场景 | CRUD 密集型、无需复杂 SQL 的场景 |

> 💡 很多大厂面试喜欢问 MyBatis vs Hibernate 的选择依据。核心观点：MyBatis 在手写 SQL 的灵活性上更胜一筹，适合需要 DBA 深度参与优化的场景。

### 1.3 什么是 ORM？
ORM（Object Relational Mapping）即**对象关系映射**，是一种将 Java 对象与数据库表记录建立映射关系的技术。MyBatis 通过 `resultMap` 或自动映射将查询结果集转换为 Java 实体对象，从而让开发者用面向对象的方式操作数据库。

### 1.4 #{} 和 ${} 的区别是什么？
这是 MyBatis 面试的**必考题**。

| 特性 | `${}` | `#{}` |
|------|-------|-------|
| 处理方式 | 字符串直接替换（拼接到 SQL） | 预编译参数替换（占位符 `?`） |
| SQL 注入 | 有风险 | 安全（预编译） |
| 使用场景 | 表名、列名等动态 SQL 结构 | 参数值传递 |
| 性能 | 每次编译 | 可复用执行计划 |

```java
// 安全用法
@Select("SELECT * FROM user WHERE id = #{id}")
User findById(Long id);

// 表名动态传入必须用 ${}（注意防止注入）
@Select("SELECT * FROM ${tableName} WHERE id = #{id}")
User findByTable(@Param("tableName") String tableName, @Param("id") Long id);
```

> ⚠️ 实测面试陷阱：`${}` 在 ORDER BY 排序字段、表名等场景不可避免，此时一定要对传入参数做白名单校验。

### 1.5 MyBatis 的一级缓存和二级缓存是什么？
| 缓存级别 | 作用域 | 生命周期 | 默认开启 |
|---------|--------|---------|---------|
| 一级缓存 | SqlSession | SqlSession 生命周期内 | 是 |
| 二级缓存 | Mapper（Namespace） | 跨 SqlSession 共享 | 否（需手动开启）|

**一级缓存**：同一个 SqlSession 中执行相同 SQL，第二次直接从缓存取。执行 `update/delete/insert` 或调用 `clearCache()` 会清空。

**二级缓存**：多个 SqlSession 共享同一个 Mapper 的缓存数据。需在 XML 中添加 `<cache/>` 标签，且实体类实现 `Serializable`。

> 🎯 面试亮点点拨：一级缓存是 SqlSession 级别的 Map 结构；二级缓存是 Mapper 级别的，实际存储底层依赖第三方缓存如 Redis。

### 1.6 MyBatis 的 DAO 接口是如何工作的？
MyBatis 使用 **JDK 动态代理**为每个 Mapper 接口创建代理对象。当调用接口方法时，代理对象根据方法的全限定名（`namespace.methodId`）找到对应的 SQL 语句并执行。关键是 `MapperProxy` 类实现了 `InvocationHandler` 接口。

### 1.7 MyBatis 的核心组件有哪些？
| 组件 | 作用 |
|------|------|
| `SqlSessionFactoryBuilder` | 解析配置文件，构建工厂 |
| `SqlSessionFactory` | 创建 SqlSession 的工厂类 |
| `SqlSession` | 数据库会话，提供增删改查 API |
| `Executor` | SQL 执行器，负责缓存维护和 JDBC 操作 |
| `StatementHandler` | 处理 JDBC Statement 的创建和参数设置 |
| `ParameterHandler` | 参数绑定处理 |
| `ResultSetHandler` | 结果集映射为 Java 对象 |
| `MappedStatement` | 存储 SQL 节点信息的封装对象 |

### 1.8 什么是 MyBatis 的 TypeHandler？
TypeHandler 用于实现 Java 类型和 JDBC 类型之间的相互转换。当实体类属性类型与数据库字段类型不一致时，可以自定义 TypeHandler。

```java
public class JsonTypeHandler extends BaseTypeHandler<List<String>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, JSON.toJSONString(parameter));
    }
    
    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return JSON.parseArray(rs.getString(columnName), String.class);
    }
    // ... getNullableResult 其他重载方法
}
```

### 1.9 MyBatis 的 resultMap 和 resultType 有什么区别？
| resultType | resultMap |
|------------|-----------|
| 自动映射到指定类的同名属性 | 手动配置字段与属性的映射关系 |
| 不支持复杂的联合查询映射 | 支持 `association`、`collection` 等高级映射 |
| 列名需与属性名一致（或用下划线驼峰转换） | 可完全自主定制映射规则 |

### 1.10 MyBatis 中动态 SQL 有哪些标签？
MyBatis 提供了丰富的动态 SQL 标签：

| 标签 | 用途 |
|------|------|
| `<if>` | 条件判断 |
| `<where>` | 自动处理 WHERE 子句中的 AND/OR 前缀 |
| `<set>` | 自动处理 UPDATE 语句中的 SET 逗号 |
| `<trim>` | 自定义前缀/后缀处理规则 |
| `<choose>/<when>/<otherwise>` | 类似 switch-case |
| `<foreach>` | 集合遍历（IN 查询、批量插入）|
| `<sql>/<include>` | SQL 片段复用 |

```xml
<select id="findByCondition" resultType="User">
    SELECT * FROM user
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
    </where>
</select>
```

### 1.11 MyBatis 支持哪些分页方式？
1. **内存分页**：使用 `RowBounds` 对象，查询全部后在内存中截取（不推荐，大数据量 OOM）
2. **SQL 分页**：手写 `LIMIT #{offset}, #{pageSize}` 或数据库方言分页
3. **插件分页**：使用 PageHelper 等分页插件，自动生成分页 SQL

### 1.12 MyBatis 的逆向工程是什么？
MyBatis Generator（MBG）是代码生成器，根据数据库表结构自动生成：
- 实体类（POJO）
- Mapper 接口（DAO）
- Mapper XML 文件（SQL 映射）
- Example 类（条件查询辅助）

> 💡 大厂面试不会重点考逆向工程用法，但需要知道它的存在以及**代码生成可能导致过度依赖、维护困难**的缺点。

### 1.13 MyBatis 延迟加载的原理是什么？
MyBatis 通过**动态代理**实现懒加载。当配置 `lazyLoadingEnabled=true` 时，关联对象的属性被设置成代理对象。真正调用该属性方法时，代理对象才会执行 SQL 去数据库查询。实现原理是 CGLIB 或 Javassist 创建代理类，触发 `EnhancedDeserializationProxy` 执行后续 SQL。

### 1.14 MyBatis 的插件机制原理是什么？
MyBatis 允许拦截四大核心对象的方法：
- `Executor`（update, query, commit, rollback）
- `StatementHandler`（prepare, parameterize, batch, update, query）
- `ParameterHandler`（getParameterObject, setParameters）
- `ResultSetHandler`（handleResultSets, handleOutputParameters）

通过 `@Intercepts` 注解声明拦截方法，用 `Plugin.wrap()` 生成代理。PageHelper、MyBatis-Plus 的分页插件都是基于此实现。

```java
@Intercepts({
    @Signature(type = Executor.class, method = "query",
              args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class MyPlugin implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 前置处理
        Object result = invocation.proceed();
        // 后置处理
        return result;
    }
}
```

### 1.15 MyBatis 事务管理有几种方式？
1. **JDBC 事务**：`<transactionManager type="JDBC"/>`，使用 Connection 的 commit/rollback
2. **MANAGED 事务**：`<transactionManager type="MANAGED"/>`，由外部容器（如 Spring）管理
3. **Spring 整合事务**：通过 `@Transactional` 注解声明式管理

---

## 二、深度原理剖析（10-15题）

### 2.1 MyBatis 完整的 SQL 执行流程是什么？
```
调用 Mapper 接口方法
    → MapperProxy.invoke()
        → MapperMethod.execute()
            → SqlSession.selectList/insert/update()
                → Executor.query()
                    → 一级缓存检查
                    → 二级缓存检查
                    → StatementHandler.prepare()
                        → 数据库连接准备
                    → ParameterHandler.setParameters()
                        → 参数绑定到 PreparedStatement
                    → StatementHandler.query()
                        → JDBC 执行 SQL
                    → ResultSetHandler.handleResultSets()
                        → 结果集映射为 Java 对象
                    → 存入一级缓存
                → 返回结果
```

### 2.2 一级缓存的源码实现是怎样的？
`BaseExecutor` 内部维护一个 `PerpetualCache`（本质是 `HashMap`）作为一级缓存。执行查询时，以 `CacheKey` 为键查找缓存。`CacheKey` 由 `MappedStatement ID`、`RowBounds`、`SQL` 语句、参数值等构成。

```java
// BaseExecutor 源码核心逻辑
public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, 
                         ResultHandler resultHandler, CacheKey key, BoundSql boundSql) {
    Cache cache = ms.getCache(); // 二级缓存
    // ...
    List<E> list = localCache.getObject(key); // 一级缓存查询
    if (list == null) {
        list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
    }
    return list;
}
```

执行 `update/delete/insert` 时，`BaseExecutor.flushCacheIfRequired()` 会清空本地缓存。

### 2.3 二级缓存的实现机制？
二级缓存在 `CachingExecutor` 中实现，底层采用**装饰器模式**，通过 `TransactionalCacheManager` 管理。每个 `MappedStatement` 持有自己的 `Cache` 对象（PerpetualCache）。使用 `CacheBuilder` 构建，支持 FIFO、LRU、Soft/Weak Reference 等策略。

```xml
<cache eviction="LRU" flushInterval="60000" size="512" readOnly="false"/>
```

> 🎯 面试亮点：二级缓存存在脏读问题（跨 namespace 操作关联表），实际生产建议用 Redis 替代 MyBatis 二级缓存。

### 2.4 MyBatis 如何解决 JDBC 的重复性问题？
1. **SQL 语句配置化**：SQL 从 Java 代码解耦到 XML/注解
2. **参数自动绑定**：`ParameterHandler` 处理参数设置
3. **结果自动映射**：`ResultSetHandler` 处理结果到对象的转换
4. **连接池管理**：集成 HikariCP、Druid 等
5. **事务统一管理**：声明式事务替代手写 commit/rollback

### 2.5 DAO 接口动态代理的实现原理是什么？
`MapperRegistry` 在初始化时，通过 `MapperProxyFactory` 为每个 Mapper 接口创建代理工厂。调用 `getMapper()` 时，返回 `Proxy.newProxyInstance()` 生成的代理对象。`MapperProxy` 内部持有 `SqlSession` 和 mapper 接口的 `Class` 对象，调用时构造 `MapperMethod` 去执行 SQL。

```java
// MapperProxy 核心代码
public class MapperProxy<T> implements InvocationHandler, Serializable {
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MapperMethod mapperMethod = cachedMapperMethod(method);
        return mapperMethod.execute(sqlSession, args);
    }
}
```

### 2.6 什么是 MyBatis 的 BoundSql？
`BoundSql` 是 MyBatis 中封装**解析后的 SQL 语句**及其参数的对象。它包含三部分：
- **SQL 语句**：`#{id}` 被替换为 `?` 后的完整 SQL
- **参数映射列表**：每个参数的名字、类型、JdbcType
- **附加参数**：额外传递给 SQL 的上下文参数

### 2.7 MyBatis 的 SqlSource 接口的作用是什么？
`SqlSource` 表示一条 SQL 的来源，负责解析动态 SQL 生成 `BoundSql`。主要有两个实现：

| 实现类 | 作用 |
|--------|------|
| `DynamicSqlSource` | 处理包含 `<if>`、`<where>` 等动态标签的 SQL |
| `RawSqlSource` | 处理只有 `#{}` 占位符的静态 SQL（性能更好）|

### 2.8 为什么 MyBatis 的 Mapper 接口不需要实现类？
这是在面试中展示深度的好问题。Mapper 接口的**全限定名**作为 `namespace`，**方法名**作为 `statementId`，两者唯一确定一个 `MappedStatement`。MyBatis 通过 `MapperProxy` 动态代理，将接口调用路由到对应的 SQL 执行。本质上是**组合模式 + 动态代理**的实现。

### 2.9 MyBatis 的参数处理机制是什么？
MyBatis 使用 `ParamNameResolver` 解析方法参数：
- **单个简单类型**：`#{任意名}` 都可以
- **多个参数**：默认包装为 Map（key: arg0, arg1... 或 param1, param2...）
- **`@Param` 注解**：指定参数名，推荐使用

```java
// MyBatis 多参数处理原理
public Object getNamedParams(Object[] args) {
    // 如果参数数组只有一个且没有@Param注解 -> 直接返回
    // 否则包装为 Map<String, Object>
    Map<String, Object> param = new ParamMap<>();
    for (int i = 0; i < names.size(); i++) {
        param.put(names.get(i), args[i]);  // @Param值 或 arg0/arg1
        param.put("param" + (i + 1), args[i]); // 兼容 param1/param2
    }
    return param;
}
```

### 2.10 MyBatis 的嵌套查询（association/collection）如何工作？
- **`association`**：多对一关系映射。在结果映射中嵌入另一个查询
- **`collection`**：一对多关系映射。映射集合属性
- **分步查询**：通过 `select` 属性指向另一个 MappedStatement，支持延迟加载

```xml
<resultMap id="userWithOrders" type="User">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <!-- 一对多分步查询（延迟加载） -->
    <collection property="orders" javaType="List" ofType="Order"
                select="com.example.mapper.OrderMapper.findByUserId"
                column="id" fetchType="lazy"/>
</resultMap>
```

### 2.11 MyBatis 的 Transaction 机制是怎样的？
MyBatis 提供 `Transaction` 接口，有 `JdbcTransaction` 和 `ManagedTransaction` 两种实现。前者直接管理 Connection 的 autoCommit 和事务边界；后者将控制权交给容器。Spring 集成时使用 `SpringManagedTransaction`，通过 `DataSourceUtils` 获取绑定在事务管理器上的 Connection，保证事务统一。

### 2.12 MyBatis 如何实现日志输出？
MyBatis 通过 `LogFactory` 适配各种日志框架，按优先级查找：SLF4J → Apache Commons Logging → Log4j2 → Log4j → StdOut。配置 `<setting name="logImpl" value="STDOUT_LOGGING"/>` 开启日志，`<setting name="logPrefix" value="..."/>` 设置前缀过滤。

---

## 三、实战场景题（8-12题）

### 3.1 批量插入 10 万条数据，如何优化？
```xml
<!-- 方式一：MyBatis foreach 批量插入 -->
<insert id="batchInsert">
    INSERT INTO user(name, age, email) VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.name}, #{item.age}, #{item.email})
    </foreach>
</insert>
```

> ⚠️ 每次 batch 不宜超过 1000 条，否则 SQL 过长会导致数据库解析压力大。建议分批次提交，每批 500-1000 条。

**优化方案**：
1. 设置 `rewriteBatchedStatements=true`（MySQL JDBC 参数）
2. 关闭自动提交，手动分批 commit
3. 使用 MyBatis 的 `BATCH` 执行器类型
4. 先删除非聚簇索引，插完再重建

### 3.2 模糊查询的几种写法及其安全性？
```xml
<!-- ✅ 安全且正确（推荐） -->
<if test="name != null">
    AND name LIKE CONCAT('%', #{name}, '%')
</if>

<!-- ✅ 安全但需要注意 -->
<if test="name != null">
    AND name LIKE "%"#{name}""
</if>

<!-- ❌ SQL 注入风险 -->
<if test="name != null">
    AND name LIKE '%${name}%'
</if>
```

### 3.3 复杂多表关联查询如何用 MyBatis 实现？
```xml
<resultMap id="orderDetailMap" type="OrderDetailVO" extends="BaseResultMap">
    <association property="user" javaType="User"
                 columnPrefix="u_">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
    </association>
    <collection property="items" ofType="OrderItem"
                columnPrefix="oi_">
        <id property="id" column="id"/>
        <result property="productName" column="product_name"/>
        <result property="quantity" column="quantity"/>
    </collection>
</resultMap>

<select id="queryOrderDetail" resultMap="orderDetailMap">
    SELECT o.id, o.order_no,
           u.id AS u_id, u.name AS u_name,
           oi.id AS oi_id, oi.product_name AS oi_product_name, oi.quantity AS oi_quantity
    FROM order o
    LEFT JOIN user u ON o.user_id = u.id
    LEFT JOIN order_item oi ON o.id = oi.order_id
    WHERE o.id = #{id}
</select>
```

### 3.4 如果查询结果字段很多，如何处理部分字段查询？
```xml
<!-- 方案一：创建专门的 VO 类并配置 resultMap -->
<resultMap id="simpleUserMap" type="SimpleUserVO">
    <result property="name" column="name"/>
    <result property="email" column="email"/>
</resultMap>

<!-- 方案二：用 Map 接收 -->
<select id="findPartial" resultType="java.util.Map">
    SELECT name, email FROM user WHERE id = #{id}
</select>

<!-- 方案三：动态指定查询列 -->
<select id="findCustom" resultType="java.util.Map">
    SELECT
    <foreach collection="columns" item="col" separator=",">
        ${col}
    </foreach>
    FROM user
</select>
```

### 3.5 如何处理数据库中的枚举类型？
```java
// 方式一：TypeHandler
public class GenderTypeHandler extends BaseTypeHandler<Gender> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Gender param, JdbcType jt) {
        ps.setInt(i, param.getCode());
    }
    @Override
    public Gender getNullableResult(ResultSet rs, String col) throws SQLException {
        return Gender.of(rs.getInt(col));
    }
    // ... 其他方法
}

// 方式二：MyBatis 3.6+ 默认枚举支持
public enum Gender {
    MALE(1), FEMALE(2);
    @EnumValue // 指定存储到数据库的值
    private final int code;
}
```

### 3.6 在使用 MyBatis 时，如何进行 SQL 性能监控？
1. **开启慢 SQL 日志**：配置 `mybatis.configuration.log-impl` 为 `log4jdbc` 或 `p6spy`
2. **MyBatis 插件拦截**：自定义 Interceptor 统计每条 SQL 执行时间
3. **集成 Druid 监控**：通过 Druid 的 SQL 监控面板
4. **Actuator + 指标收集**：Spring Boot 环境下通过 `management.endpoints.web.exposure.include=*`

### 3.7 项目中使用 MyBatis 时，如何处理大文本/Blob 字段？
```java
// 自定义 TypeHandler 处理 Blob -> String
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.BLOB)
public class BlobTypeHandler extends BaseTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String param, JdbcType jt) {
        ByteArrayInputStream bis = new ByteArrayInputStream(param.getBytes(StandardCharsets.UTF_8));
        ps.setBinaryStream(i, bis, param.length());
    }
    
    @Override
    public String getNullableResult(ResultSet rs, String col) throws SQLException {
        Blob blob = rs.getBlob(col);
        return blob == null ? null : new String(blob.getBytes(1, (int) blob.length()), StandardCharsets.UTF_8);
    }
}
```

### 3.8 使用 MyBatis 分页插件 PageHelper 时有哪些注意事项？
```java
// 正确用法
PageHelper.startPage(pageNum, pageSize);
List<User> list = userMapper.findAll();
PageInfo<User> pageInfo = new PageInfo<>(list);

// ⚠️ 注意事项
// 1. startPage 后必须紧跟查询方法，不能隔行
// 2. 线程安全问题：PageHelper 使用 ThreadLocal，查询结束后自动清理
// 3. 嵌套查询时，PageHelper 只对主查询生效，不对 association/collection 分页
// 4. 分页插件会修改 SQL 语句（追加 LIMIT/ROWNUM），影响执行计划缓存
```

---

## 四、手写代码题（5-8题）

### 4.1 手写 MyBatis 工具类 SqlSessionUtil
```java
public class SqlSessionUtil {
    private static final SqlSessionFactory sqlSessionFactory;
    
    static {
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        } catch (IOException e) {
            throw new RuntimeException("初始化 SqlSessionFactory 失败", e);
        }
    }
    
    // ThreadLocal 保证线程安全
    private static final ThreadLocal<SqlSession> tl = new ThreadLocal<>();
    
    public static SqlSession getSqlSession() {
        SqlSession sqlSession = tl.get();
        if (sqlSession == null) {
            sqlSession = sqlSessionFactory.openSession();
            tl.set(sqlSession);
        }
        return sqlSession;
    }
    
    public static void closeSqlSession() {
        SqlSession sqlSession = tl.get();
        if (sqlSession != null) {
            sqlSession.close();
            tl.remove();
        }
    }
    
    // 提交事务
    public static void commit() {
        SqlSession sqlSession = getSqlSession();
        sqlSession.commit();
        closeSqlSession();
    }
}
```

### 4.2 手写 MyBatis 分页查询（不使用插件）
```java
public interface UserMapper {
    List<User> findPage(@Param("offset") int offset, @Param("limit") int limit);
    Long countTotal();
}
```

```xml
<select id="findPage" resultType="User">
    SELECT * FROM user ORDER BY id DESC
    LIMIT #{offset}, #{limit}
</select>
<select id="countTotal" resultType="long">
    SELECT COUNT(*) FROM user
</select>
```

### 4.3 手写实现简单的 MyBatis 插件（统计 SQL 执行时间）
```java
@Intercepts({
    @Signature(type = Executor.class, method = "query",
              args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update",
              args = {MappedStatement.class, Object.class})
})
public class SqlCostInterceptor implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = invocation.proceed();
        long cost = System.currentTimeMillis() - start;
        
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        String sqlId = ms.getId();
        System.out.println("SQL [" + sqlId + "] 执行耗时: " + cost + "ms");
        
        if (cost > 1000) {
            // 记录慢 SQL
            System.err.println("⚠️ 慢 SQL 预警: " + sqlId + " 耗时 " + cost + "ms");
        }
        return result;
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // 可读取配置参数
    }
}
```

### 4.4 手写 MyBatis 延迟加载中的 association 配置
```xml
<mapper namespace="com.example.mapper.UserMapper">
    <resultMap id="userWithDeptLazy" type="User">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <!-- 延迟加载部门信息 -->
        <association property="dept" javaType="Department"
                     select="com.example.mapper.DeptMapper.findById"
                     column="dept_id" fetchType="lazy">
            <id property="id" column="id"/>
            <result property="name" column="name"/>
        </association>
    </resultMap>
    
    <select id="findByIdWithDept" resultMap="userWithDeptLazy">
        SELECT id, name, dept_id FROM user WHERE id = #{id}
    </select>
</mapper>
```

### 4.5 手写批量插入优化（分批次）
```java
public void batchInsert(List<User> userList) {
    int batchSize = 500;
    for (int i = 0; i < userList.size(); i += batchSize) {
        int end = Math.min(i + batchSize, userList.size());
        List<User> batch = userList.subList(i, end);
        userMapper.batchInsert(batch);
        sqlSession.commit(); // 每 500 条提交一次
    }
}
// 同时设置 SqlSession 的 ExecutorType 为 BATCH
SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
```

### 4.6 手写 MyBatis 动态 SQL 的 choose 示例
```xml
<select id="findByConditionDynamic" resultType="User">
    SELECT * FROM user
    <where>
        <choose>
            <when test="queryType == 'name'">
                AND name LIKE CONCAT('%', #{keyword}, '%')
            </when>
            <when test="queryType == 'email'">
                AND email = #{keyword}
            </when>
            <when test="queryType == 'phone'">
                AND phone = #{keyword}
            </when>
            <otherwise>
                AND 1=0
            </otherwise>
        </choose>
    </where>
</select>
```

---

## 五、系统设计题（3-5题）

### 5.1 设计一个通用的 MyBatis 多租户（SaaS）方案
```java
// 1. 通过 MyBatis 拦截器自动追加租户条件
@Intercepts({
    @Signature(type = Executor.class, method = "query",
              args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class TenantInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        // 获取当前租户 ID（从上下文中获取）
        String tenantId = TenantContext.getCurrentTenant();
        
        // 修改 BoundSql，追加 tenant_id = #{tenantId}
        BoundSql boundSql = ms.getBoundSql(args[1]);
        String sql = boundSql.getSql();
        String newSql = addTenantCondition(sql, tenantId);
        
        // 通过反射替换 SQL
        Field field = BoundSql.class.getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, newSql);
        
        return invocation.proceed();
    }
}
```

> 💡 多租户方案的核心挑战是：如何在不改造所有业务 SQL 的情况下自动路由租户数据。AOP + MyBatis 拦截器是最优雅的方案。

### 5.2 如何设计一个支持动态表名（分表分库）的 MyBatis 框架？
```java
// 方案：自定义动态表名插件
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DynamicTableInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();
        
        // 根据路由键替换表名
        String tableSuffix = ShardingContext.getTableSuffix();
        String newSql = sql.replaceAll("user_table", "user_table_" + tableSuffix);
        
        // 反射替换
        Field field = BoundSql.class.getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, newSql);
        
        return invocation.proceed();
    }
}
```

### 5.3 如何设计一个 MyBatis 二级缓存的 Redis 实现？
```java
public class RedisCache implements Cache {
    private final String id;
    
    public RedisCache(String id) {
        this.id = id;
    }
    
    @Override
    public void putObject(Object key, Object value) {
        // 使用 Redis Hash 存储
        redisTemplate.opsForHash().put(id, key.toString(), value);
        // 设置过期时间防止内存泄漏
        redisTemplate.expire(id, 1, TimeUnit.HOURS);
    }
    
    @Override
    public Object getObject(Object key) {
        return redisTemplate.opsForHash().get(id, key.toString());
    }
    
    @Override
    public Object removeObject(Object key) {
        return redisTemplate.opsForHash().delete(id, key.toString());
    }
    
    @Override
    public void clear() {
        redisTemplate.delete(id);
    }
    
    @Override
    public int getSize() {
        return redisTemplate.opsForHash().size(id).intValue();
    }
}

// MyBatis 配置中使用自定义缓存
// <cache type="com.example.cache.RedisCache" />
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点 | 问题描述 | 解决方案 | 最佳实践 |
|------|---------|---------|---------|
| `${}` SQL 注入 | 字符串拼接导致 SQL 注入 | 能用 `#{}` 的地方不用 `${}` | `${}` 仅用于表名/列名动态，且做白名单校验 |
| 一级缓存脏数据 | 同一 SqlSession 多表操作产生脏读 | 在关键查询后手动 `clearCache()` | 合理规划 SqlSession 生命周期 |
| 二级缓存跨 namespace 失效 | 关联表更新后其他缓存未清除 | 引用 `cache-ref` 或使用 Redis 统一缓存 | 生产环境建议关闭二级缓存，改用 Redis |
| N+1 查询问题 | 延迟加载导致频繁 SQL 查询 | 使用 `@One/@Many(fetch=FetchType.EAGER)` 或 left join | 在 association/collection 上合理设置 fetchType |
| 分页插件内存溢出 | PageHelper 拦截没有紧跟在查询后 | 确保 `startPage` 后紧接查询方法 | 封装通用分页工具类 |
| Like 拼接错误 | CONCAT 或 % 拼接出错 | 使用 `CONCAT('%', #{val}, '%')` | 统一写法，避免不同数据库方言问题 |
| 批量插入效率低 | foreach 拼接过长 SQL | 分批次批量插入（每批 500） | 设置 `allowMultiQueries=true` 或使用 BATCH 模式 |
| 驼峰映射不生效 | 数据库下划线字段未映射到驼峰属性 | 开启 `mapUnderscoreToCamelCase=true` | 统一在配置文件中全局开启 |

---

## 七、面试回答模板（Top 5高频题的结构化回答模板）

### 7.1 #{} 和 ${} 的区别
**总分结构**：
1. **核心区别**：`#{}` 是预编译占位符，使用 `PreparedStatement` 参数化；`${}` 是字符串直接替换
2. **安全性**：`#{}` 防 SQL 注入；`${}` 有注入风险
3. **性能**：`#{}` 可复用执行计划；`${}` 每次重新编译
4. **使用场景**：参数值用 `#{}`；表名、列名、排序字段动态必须用 `${}` 但需校验
5. **扩展回答**：可以引出 MyBatis 的 `SqlSource` 如何解析参数、`ParameterHandler` 如何处理类型

### 7.2 MyBatis 一级缓存和二级缓存
**三层结构**：
1. **一级缓存**：SqlSession 级别 HashMap，默认开启，作用域为一个 SqlSession
2. **二级缓存**：Mapper/Namespace 级别，需手动开启，跨 SqlSession 共享
3. **源码层面**：`BaseExecutor` 维护 `localCache`，`CachingExecutor` 装饰处理二级缓存
4. **坑点**：二级缓存脏读、分布式问题，大型项目建议用 Redis 替代

### 7.3 MyBatis 插件机制
**机制 + 举例**：
1. **四大对象**：Executor、StatementHandler、ParameterHandler、ResultSetHandler
2. **实现方式**：`@Intercepts` + `Plugin.wrap()` 生成代理
3. **分页插件原理**：PageHelper 拦截 `Executor.query()`，修改 SQL 追加分页语句
4. **源码细节**：`Plugin.invoke()` 匹配 `@Signature` 决定是否拦截

### 7.4 MyBatis 的 DAO 接口如何工作
**代理链分析**：
1. `MapperRegistry` 在初始化时注册 Mapper 接口
2. 调用 `getMapper()` → `MapperProxyFactory.newInstance()` → JDK 动态代理
3. `MapperProxy.invoke()` → `MapperMethod.execute()` → `SqlSession` 执行 SQL
4. `namespace + methodId` 定位 `MappedStatement`
5. 最终由 `Executor` → `StatementHandler` → `PreparedStatement` 执行

### 7.5 MyBatis 和 Hibernate 怎么选
**三点对比**：
1. **灵活性**：MyBatis 手写 SQL 适合复杂查询和 DBA 优化；Hibernate 全自动适合简单 CRUD
2. **性能控制**：MyBatis 完全可控 SQL 执行计划；Hibernate 需要了解缓存、抓取策略等
3. **团队要求**：MyBatis 要求 SQL 能力；Hibernate 要求 ORM 深度理解
4. **结论**：互联网大厂更倾向 MyBatis（SQL 可优化、DBA 友好）；传统企业项目可能选 Hibernate

---

## 八、快速查漏补缺 Checklist

- [ ] #{} vs ${} 区别及源码理解
- [ ] 一级缓存原理、作用域、失效条件
- [ ] 二级缓存原理、配置方式、与 Redis 集成
- [ ] DAO 接口动态代理原理（MapperProxy、MapperMethod）
- [ ] 类型转换 TypeHandler（内置类型 + 自定义）
- [ ] 延迟加载配置及原理（CGLIB 代理）
- [ ] 插件机制 Interceptor 四大对象
- [ ] 分页插件 PageHelper 原理
- [ ] 动态 SQL 标签（if/where/foreach/choose/trim/set）
- [ ] 高级映射（association/collection 分步查询）
- [ ] 参数处理机制（ParamNameResolver）
- [ ] MyBatis 执行流程（调用链顺序）
- [ ] 事务管理器类型（JDBC/MANAGED/Spring）
- [ ] MyBatis 核心配置文件参数
- [ ] SqlSource、BoundSql、MappedStatement 的关系
- [ ] 注解式开发（@Select/@Insert/@Update/@Delete）
- [ ] 逆向工程 MyBatis Generator
- [ ] MyBatis + Spring 集成原理（SqlSessionTemplate）
- [ ] SQL 注入防范（$ 的安全使用方案）
- [ ] 批量插入优化策略
