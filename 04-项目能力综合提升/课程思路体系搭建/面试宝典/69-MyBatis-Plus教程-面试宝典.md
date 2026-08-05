# MyBatis-Plus 面试宝典
> 基于课程大纲全面覆盖 MyBatis-Plus 面试高频考点，从快速入门到插件机制与源码级分析

## 目录
1. [一、基础概念速答](#一基础概念速答12-18题)
2. [二、深度原理剖析](#二深度原理剖析8-12题)
3. [三、实战场景题](#三实战场景题6-10题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（12-18题）

### 1.1 MyBatis-Plus 与 MyBatis 有什么关系？
MyBatis-Plus（简称 MP）是 MyBatis 的**增强工具**，在 MyBatis 基础上**只做增强不做修改**。它不会改变 MyBatis 原有的行为，而是提供了 BaseMapper、IService、条件构造器等工具类，将单表 CRUD 的重复代码从开发者的日常工作中消除。引入 MP 不会影响现有 MyBatis 配置和 XML 映射文件，两者可以无缝共存。

### 1.2 MyBatis-Plus vs MyBatis vs JPA 有何区别？
| 特性 | MyBatis-Plus | MyBatis | JPA/Hibernate |
|------|-------------|---------|---------------|
| SQL 控制 | 单表自动 + 复杂查询手写 | 全手写 SQL | 自动生成 SQL（JPQL） |
| 单表 CRUD 效率 | 极高（零 XML） | 需手写全部 SQL | 极高（自动） |
| 动态 SQL | 条件构造器 + XML | XML/literal 拼接 | Criteria API |
| 分页 | 分页插件（自动 COUNT） | 手动拼接 LIMIT | 自带分页 |
| 代码生成 | AutoGenerator | 需第三方 | 逆向工程 |
| 学习成本 | 低（熟悉 MyBatis 即可） | 中 | 高 |
| 复杂 SQL 优化 | 灵活（完全可控） | 灵活（完全可控） | 困难（黑盒） |

> 💡 **面试高频话术**：MP 在保留 MyBatis 手写 SQL 灵活性的基础上，解决了 80% 单表 CRUD 的重复劳动，适合国内互联网公司追求开发效率的场景。

### 1.3 BaseMapper 提供了哪些 CRUD 方法？
BaseMapper 是 MP 的核心接口，泛型参数为实体类，提供以下方法：

| 分类 | 方法 | 说明 |
|------|------|------|
| 插入 | `int insert(T entity)` | 插入一条记录 |
| 删除 | `int deleteById(Serializable id)` | 按主键删除 |
| | `int deleteByMap(Map< String,Object > map)` | 按 columnMap 条件删除 |
| | `int delete(Wrapper<T> wrapper)` | 按条件构造器删除 |
| | `int deleteBatchIds(Collection<?> ids)` | 按主键集合批量删除 |
| 更新 | `int updateById(T entity)` | 按主键更新 |
| | `int update(T entity, Wrapper<T> wrapper)` | 按条件构造器更新 |
| 查询 | `T selectById(Serializable id)` | 按主键查询 |
| | `List<T> selectBatchIds(Collection<?> ids)` | 按主键集合查询 |
| | `T selectOne(Wrapper<T> wrapper)` | 查询一条记录 |
| | `Integer selectCount(Wrapper<T> wrapper)` | 查询总记录数 |
| | `List<T> selectList(Wrapper<T> wrapper)` | 查询列表 |
| | `List<T> selectByMap(Map< String,Object > map)` | 按 columnMap 查询 |
| | `<E> List<E> selectObjs(Wrapper<T> wrapper)` | 查询第一个字段 |
| | `<P extends IPage<T>> P selectPage(P page, Wrapper<T> wrapper)` | 分页查询 |

```java
// 使用示例：继承 BaseMapper 即可
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 无需任何方法定义
}

// 调用
User user = userMapper.selectById(1L);
List<User> users = userMapper.selectList(Wrappers.lambdaQuery(User.class).eq(User::getAge, 18));
```

### 1.4 IService 和 BaseMapper 有什么区别？
IService 是 MP 提供的**业务层接口**，进一步封装了 BaseMapper，提供更丰富的批量操作和链式调用。

| 对比维度 | BaseMapper | IService |
|---------|------------|----------|
| 层次 | DAO 层（Mapper） | Service 层 |
| 批量操作 | 无原生批量新增 | `saveBatch()`、`saveOrUpdateBatch()` |
| 链式调用 | 需手动构建 Wrapper | `lambdaQuery().eq(...).list()` |
| 事务 | 不处理 | 批量操作自带事务 |
| 典型方法 | `insert`、`updateById` | `save`、`saveOrUpdate`、`list`、`page` |

```java
// IService 使用
public interface UserService extends IService<User> {
}

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    // 直接拥有 saveBatch、list、page 等方法
    public void batchSave() {
        saveBatch(userList); // 批量插入，自带事务
        saveOrUpdateBatch(userList); // 批量新增或更新
    }
}
```

### 1.5 @TableName 的自动映射规则是什么？
MP 通过 `@TableName` 注解实现实体类与数据库表的映射。若不指定 `value`，MP 会将实体类名作为表名（默认启用驼峰转下划线）。例如 `UserInfo` 实体默认映射到 `user_info` 表。

```java
// 显式指定表名
@Data
@TableName("tb_user")
public class User {
    @TableId
    private Long id;
    private String name;
}

// schema 属性：多 schema 场景
@Data
@TableName(value = "user", schema = "my_db")
public class User {
    // ...
}

// autoResultMap = true：启用自动 resultMap（配合 JSON 处理器等场景）
@Data
@TableName(value = "user", autoResultMap = true)
public class User {
    private String name;
    private String contacts; // JSON 类型字段
}
```

### 1.6 @TableId 主键策略有哪些？
MP 通过 `@TableId` 的 `type` 属性指定主键生成策略，默认为 `IdType.ASSIGN_ID`（雪花算法）。

| 策略 | 值 | 说明 |
|------|-----|------|
| `ASSIGN_ID` | 3 | **默认**，使用雪花算法生成 64 位 Long 型 ID |
| `ASSIGN_UUID` | 4 | 生成 32 位 UUID 字符串 |
| `AUTO` | 0 | 数据库自增（依赖 DB auto_increment） |
| `NONE` | 1 | 不设置，由数据库或其他方式处理 |
| `INPUT` | 2 | 用户手动输入 |

```java
// 雪花算法 ID（默认）
@Data
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
}

// 数据库自增（MySQL）
@Data
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
}
```

> 🎯 **面试重点**：雪花算法（Snowflake ID）生成的 ID 是 **全局唯一、趋势递增** 的 64 位 Long，由「1 bit 符号位 + 41 bit 时间戳 + 10 bit 工作机器 ID + 12 bit 序列号」组成，适合分布式环境。

### 1.7 @TableField 有哪些常用配置？
`@TableField` 用于配置非主键字段的映射行为：

| 属性 | 作用 | 示例 |
|------|------|------|
| `value` | 指定数据库列名 | `@TableField("user_name")` |
| `exist` | 字段是否存在（false 则忽略） | `@TableField(exist = false)` |
| `fill` | 自动填充策略 | `@TableField(fill = FieldFill.INSERT_UPDATE)` |
| `select` | 是否参与查询（false 则排除） | `@TableField(select = false)` |
| `update` | 更新时指定 set 语句 | `@TableField(update = "%s+1")` |
| `condition` | 查询条件拼接 | `@TableField(condition = SqlCondition.LIKE)` |

```java
@Data
@TableName("user")
public class User {
    @TableId
    private Long id;
    
    @TableField("nick_name") // 指定列名
    private String nickname;
    
    @TableField(exist = false) // 非数据库字段
    private String extraInfo;
    
    @TableField(fill = FieldFill.INSERT_UPDATE) // 自动填充
    private LocalDateTime updateTime;
    
    @TableField(select = false) // 默认不查询（如密码）
    private String password;
}
```

### 1.8 条件构造器有哪些？各自的适用场景？
MP 提供四大条件构造器，均继承自 `AbstractWrapper`：

| 构造器 | 适用场景 | 特点 |
|--------|---------|------|
| `QueryWrapper<T>` | 查询/删除 | 字段名用字符串，简单直接 |
| `UpdateWrapper<T>` | 更新 | 支持 SET 语句自定义 |
| `LambdaQueryWrapper<T>` | 查询/删除 | 方法引用（`User::getName`），编译期类型安全 |
| `LambdaUpdateWrapper<T>` | 更新 | 方法引用，类型安全 + SET 自定义 |

```java
// QueryWrapper（字段名写死，重构有风险）
QueryWrapper<User> qw = new QueryWrapper<>();
qw.eq("name", "张三").ge("age", 18).orderByDesc("create_time");
List<User> list = userMapper.selectList(qw);

// LambdaQueryWrapper（推荐，类型安全）
LambdaQueryWrapper<User> lqw = Wrappers.lambdaQuery(User.class);
lqw.eq(User::getName, "张三").ge(User::getAge, 18);
List<User> list = userMapper.selectList(lqw);

// UpdateWrapper（可自定义 SET）
UpdateWrapper<User> uw = new UpdateWrapper<>();
uw.set("email", "new@email.com").eq("id", 1L);
userMapper.update(null, uw);

// LambdaUpdateWrapper
LambdaUpdateWrapper<User> luw = Wrappers.lambdaUpdate(User.class);
luw.set(User::getEmail, "new@email.com").eq(User::getId, 1L);
userMapper.update(null, luw);
```

> 💡 **最佳实践**：日常开发优先使用 `LambdaQueryWrapper` / `LambdaUpdateWrapper`，避免字段名硬编码导致的运行时异常。推荐通过 `Wrappers.lambdaQuery()` 静态方法创建。

### 1.9 @TableLogic 逻辑删除如何工作？
`@TableLogic` 注解标识逻辑删除字段，MP 在执行 `deleteById` 等删除操作时会自动将 SQL 转换为 UPDATE 语句：

```java
@Data
@TableName("user")
public class User {
    @TableId
    private Long id;
    
    @TableLogic
    private Integer deleted; // 0-未删除，1-已删除
}
```

```sql
-- 实际执行的 SQL
UPDATE user SET deleted = 1 WHERE id = ? AND deleted = 0;

-- 查询时自动追加条件
SELECT * FROM user WHERE deleted = 0;
```

配置项（`application.yml`）：
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted   # 全局逻辑删除字段名
      logic-delete-value: 1         # 逻辑已删除值（默认 1）
      logic-not-delete-value: 0     # 逻辑未删除值（默认 0）
```

> ⚠️ **注意**：逻辑删除后，唯一索引可能冲突。建议使用唯一索引时考虑包含逻辑删除字段，或用 `delete_time` 代替 `deleted` 标记。

### 1.10 MP 如何防止 SQL 注入？
MP 的条件构造器对传入的参数值使用 **预编译占位符（#{}）**，而非字符串拼接（${}），从根本上防止 SQL 注入。对于表名等无法预编译的场景，MP 在 Wrapper 内部对参数进行转义处理。

```java
// 即使参数传入了恶意 SQL 也会被安全处理
LambdaQueryWrapper<User> qw = Wrappers.lambdaQuery();
qw.eq(User::getName, "' OR 1=1 --"); // 被当作普通字符串值
// 最终 SQL: WHERE name = ?  （参数值为 "' OR 1=1 --"）
```

> 💡 **源码级理解**：Wrapper 底层使用 MyBatis 的 `ParameterMapping` 机制，所有 value 参数都通过 `#{ew.paramNameValuePairs.xxx}` 占位符传递，不参与 SQL 拼接。

### 1.11 主键 ASSIGN_ID（雪花算法）的工作原理？
MP 默认使用 `IdentifierGenerator` 接口的实现 `DefaultIdentifierGenerator` 生成主键，内部封装了雪花算法（Snowflake）：

- 生成 64 位 Long 型 ID，由 4 部分组成：**1 bit 符号位（0）+ 41 bit 时间戳（毫秒级，69 年）+ 10 bit 工作机器 ID（1024 台）+ 12 bit 序列号（4096/ms）**
- 特点：全局唯一、趋势递增、高性能（无网络依赖）
- 分布式友好：每台机器分配唯一 `workerId`，无需中心化 ID 生成服务

```java
// 自定义 ID 生成器
@Component
public class CustomIdGenerator implements IdentifierGenerator {
    @Override
    public Long nextId(Object entity) {
        // 可以用 Redis incr、Leaf、美团 Leaf 等方案
        return IdUtil.getSnowflakeNextId();
    }
}
```

### 1.12 ActiveRecord 模式在 MP 中如何使用？
MP 的 ActiveRecord（AR）模式让实体类直接继承 `Model<T>`，使实体类具备 CRUD 能力。AR 模式的核心是实体即 DAO：

```java
@Data
@TableName("user")
@EqualsAndHashCode(callSuper = true) // 注意必须调用父类
public class User extends Model<User> {
    @TableId
    private Long id;
    private String name;
    private Integer age;
}

// 使用 AR 模式
User user = new User();
user.setId(1L);
user.selectById();     // 直接查询
user.setName("新名字");
user.updateById();     // 直接更新
user.deleteById();     // 直接删除

User newUser = new User();
newUser.setName("test");
newUser.insert();      // 直接插入
```

> ⚠️ AR 模式需要实体类重写 `pkVal()` 方法（或使用 `@TableId`），并确保 `serialVersionUID`。实际项目中 IService 模式更常见，AR 模式适合小型项目或原型开发。

### 1.13 saveBatch 批量操作的原理？
IService 提供的 `saveBatch` 方法用于批量插入数据，默认每批 1000 条：

```java
// IService 批量插入
List<User> list = new ArrayList<>();
for (int i = 0; i < 10000; i++) {
    list.add(new User("user_" + i));
}
userService.saveBatch(list);        // 默认每批 1000
userService.saveBatch(list, 500);   // 自定义每批 500
```

**实现原理**：
1. `saveBatch` 内部调用 `save` 方法逐条执行 `baseMapper.insert()`
2. **关键点**：虽然源码中是 for 循环逐条 insert，但如果 MP 配置了 `rewriteBatchedStatements=true`，JDBC 驱动会在底层自动批处理
3. 注意：MP 本身不生成批量 SQL（不是 `INSERT INTO ... VALUES (...), (...)`），而是逐条 `INSERT`。如果需要真正的批量插入，需要在 JDBC URL 加 `rewriteBatchedStatements=true`

### 1.14 枚举处理器是什么？
MP 的枚举处理器（`MybatisEnumTypeHandler`）自动将 Java 枚举与数据库整型/字符串互转：

```java
// 定义枚举
public enum GenderEnum implements IEnum<Integer> {
    MALE(1, "男"),
    FEMALE(0, "女");
    
    private final int value;
    GenderEnum(int value, String desc) { this.value = value; }
    
    @Override
    public Integer getValue() { return value; }
}

// 实体中使用
@Data
public class User {
    private GenderEnum gender; // 数据库存 1/0，Java 用枚举
}
```

配置：
```yaml
mybatis-plus:
  configuration:
    default-enum-type-handler: com.baomidou.mybatisplus.extension.handlers.MybatisEnumTypeHandler
```

### 1.15 JSON 处理器是什么？
MP 的 JSON 处理器自动将 Java 对象（或 List/Map）与数据库 JSON 字段互转：

```java
@Data
@TableName(value = "user", autoResultMap = true)
public class User {
    @TableId
    private Long id;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags; // 数据库字段为 JSON 数组类型
}
```

> 💡 使用 JSON 处理器时必须设置 `@TableName(autoResultMap = true)`，否则查询结果中 JSON 字段不会被正确映射。

---

## 二、深度原理剖析（8-12题）

### 2.1 MP 条件构造器的实现原理是什么？
条件构造器（Wrapper）本质是**组合模式 + 责任链模式**的实现：

1. `AbstractWrapper` 定义了 `eq()`、`ge()` 等条件方法，每次调用将条件封装为 `QueryCondition` 对象存入 `expression` 链
2. `QueryCondition` 记录列名、操作符、值、拼接逻辑（AND/OR）
3. 最终生成 SQL 时，`getSqlSegment()` 遍历 `expression` 链，按优先级拼接为 WHERE 子句
4. 所有参数值通过 `Map<String, Object> paramNameValuePairs` 存储，在 MyBatis 层面映射为 `#{ew.paramNameValuePairs.xxx}` 占位符

```java
// 核心源码路径
// com.baomidou.mybatisplus.core.conditions.AbstractWrapper
// com.baomidou.mybatisplus.core.conditions.segments.MergeSegments
```

### 2.2 分页插件（PaginationInnerInterceptor）的工作流程？
分页插件基于 MyBatis 的**拦截器机制**（`Interceptor`），在 `Executor.query()` 执行前拦截 SQL：

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

**执行流程**：
1. 拦截到 `Page` 类型的参数
2. 先执行 `SELECT COUNT(1) FROM (原SQL) total` 查询总记录数
3. 再改写原 SQL 为 `原SQL LIMIT offset, size`（不同数据库方言不同）
4. 将结果封装为 `Page<T>` 对象（含 records、total、current、size）
5. 分页查询支持多数据库（MySQL、PostgreSQL、Oracle 等），通过 `DialectFactory` 适配

### 2.3 @Version 乐观锁的工作原理？
`@Version` 注解实现乐观锁，解决并发更新丢失问题：

```java
@Data
public class User {
    @Version
    private Integer version; // 初始值 0 或 1，每次更新 +1
}
```

配置插件：
```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
}
```

**原理**：
1. 更新时自动将 WHERE 条件追加 `version = oldVersion`
2. SET 中 version 改为 `version = version + 1`
3. 若 `updateCount == 0` 说明数据已被其他线程修改，抛出 `OptimisticLockException`

```sql
-- 实际生成的 SQL
UPDATE user SET name = ?, version = version + 1 WHERE id = ? AND version = ?;
-- 参数: [新名字, 1, 0]  -- version=0 只影响旧版本
```

> 🎯 **面试回答**：乐观锁适合**读多写少**场景，利用版本号避免长事务锁。如果更新失败（影响行数为 0），业务层应重试或告知用户。

### 2.4 MetaObjectHandler 自动填充的原理？
`MetaObjectHandler` 接口配合 `@TableField(fill = ...)` 实现插入/更新时自动填充字段：

```java
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
```

**原理**：
1. MP 在 `BaseMapper.insert()` 执行前，通过 `TableInfo` 解析实体中标注了 `fill` 策略的字段
2. 若策略为 `INSERT` 或 `INSERT_UPDATE`，调用 `MetaObjectHandler.insertFill()`
3. 通过 `MetaObject` 反射设置字段值
4. 更新时同理，调用 `updateFill()`
5. `strictXxxFill` 方法会先检查字段值是否为 null（若已赋值则不覆盖）

### 2.5 AutoGenerator 代码生成器的架构？
AutoGenerator 是 MP 的代码生成引擎，基于**模板模式**构建：

```java
// 快速配置
AutoGenerator generator = new AutoGenerator(new DataSourceConfig
    .Builder("jdbc:mysql://localhost:3306/db", "root", "pass")
    .build());
generator.global(new GlobalConfig.Builder().outputDir("src/main/java").build());
generator.packageInfo(new PackageConfig.Builder().parent("com.example.demo").build());
generator.execute();
```

**生成组件**：
| 组件 | 配置类 | 生成内容 |
|------|--------|---------|
| 全局配置 | `GlobalConfig` | 输出路径、作者、Swagger、日期格式 |
| 数据源 | `DataSourceConfig` | JDBC URL、驱动、用户名密码 |
| 包配置 | `PackageConfig` | 父包名、模块名、分层包名（entity/mapper/service/controller） |
| 策略配置 | `StrategyConfig` | 表名过滤、字段过滤、Lombok、@Accessors |
| 模板配置 | `TemplateConfig` | 自定义模板路径（可替换为微服务风格） |

### 2.6 多租户插件（TenantLineInnerInterceptor）的实现？
多租户插件在查询/新增/更新/删除时**自动追加租户 ID 条件**，实现数据隔离：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
        @Override
        public String getTenantId() {
            return TenantContextHolder.getTenantId(); // 从请求上下文获取
        }
        
        @Override
        public String getTableName() {
            return "user"; // 不影响该表
        }
    }));
    return interceptor;
}
```

**拦截效果**：
```sql
-- 原 SQL
SELECT * FROM order WHERE status = 1;
-- 自动追加
SELECT * FROM order WHERE status = 1 AND tenant_id = 'tenant_001';

-- 更新/删除同理
UPDATE order SET status = 2 WHERE tenant_id = 'tenant_001';
DELETE FROM order WHERE id = ? AND tenant_id = 'tenant_001';
```

> 🎯 **面试重点**：多租户插件通过 MyBatis 拦截器在 MappedStatement 构建后修改 SQL 的 Statement。需注意：某些表不需要租户隔离（如字典表），通过 `getTableName()` 排除。

### 2.7 动态表名插件（DynamicTableNameInnerInterceptor）？
动态表名插件在运行时**按策略替换表名**，适用于分表场景：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    DynamicTableNameInnerInterceptor dynamic = new DynamicTableNameInnerInterceptor();
    dynamic.setTableNameHandlerMap(new HashMap<String, TableNameHandler>() {{
        put("user", (sql, tableName) -> {
            Long userId = UserIdHolder.getUserId(); // 从上下文获取
            return "user_" + (userId % 16); // user_0 ~ user_15
        });
    }});
    interceptor.addInnerInterceptor(dynamic);
    return interceptor;
}
```

```sql
-- 原 SQL
SELECT * FROM user WHERE id = ?;
-- 替换为（假设 userId = 100）
SELECT * FROM user_4 WHERE id = ?;
```

### 2.8 IService 的批量 saveBatch 为什么性能不高及如何优化？
`IService.saveBatch()` 默认是逐条 `INSERT`，而非真正的批量插入。其内部实现为 `for` 循环调用 `baseMapper.insert(entity)`，并非一条 SQL 插入多行。

**优化方案**：
```yaml
# JDBC 开启批量提交（关键）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db?rewriteBatchedStatements=true&useServerPrepStmts=false
```

```java
// 或者使用 MP 的批量插入 SQL 注入器（自定义批量注入）
public class InsertBatchSqlInjector extends DefaultSqlInjector {
    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> list = super.getMethodList(mapperClass, tableInfo);
        list.add(new InsertBatchSomeColumn()); // 添加批量插入方法
        return list;
    }
}
```

---

## 三、实战场景题（6-10题）

### 3.1 实现分页查询并返回前端需要的分页结果
```java
// 后端 Service
public IPage<UserVO> queryUserPage(UserPageReq req) {
    Page<User> page = new Page<>(req.getCurrent(), req.getSize());
    LambdaQueryWrapper<User> qw = Wrappers.lambdaQuery(User.class);
    qw.like(StringUtils.isNotBlank(req.getName()), User::getName, req.getName());
    qw.ge(req.getMinAge() != null, User::getAge, req.getMinAge());
    qw.orderByDesc(User::getCreateTime);
    IPage<User> userPage = userMapper.selectPage(page, qw);
    
    // 如果需返回 VO，用 MP 的 convert 或手动转换
    IPage<UserVO> voPage = userPage.convert(u -> {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(u, vo);
        return vo;
    });
    return voPage;
}

// 前端入参（推荐封装）
@Data
public class UserPageReq {
    private long current = 1;
    private long size = 10;
    private String name;
    private Integer minAge;
}
```

### 3.2 自定义 SQL 配合条件构造器
在复杂业务中，手写 SQL + 条件构造器是最佳组合：

```java
// Mapper 接口
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 使用 ${ew.customSqlSegment} 引用条件构造器的 WHERE 片段
    @Select("SELECT * FROM user ${ew.customSqlSegment}")
    List<User> selectByMyWrapper(@Param(Constants.WRAPPER) Wrapper<User> wrapper);
}

// Mapper XML
<!-- <select id="selectByMyWrapper" resultType="com.example.User"> -->
<!--   SELECT * FROM user ${ew.customSqlSegment} -->
<!-- </select> -->

// 调用
List<User> list = userMapper.selectByMyWrapper(
    Wrappers.lambdaQuery(User.class).eq(User::getAge, 18).like(User::getName, "张")
);
// 生成的 SQL: SELECT * FROM user WHERE age = ? AND name LIKE ?
```

### 3.3 逻辑删除 + 唯一索引冲突解决
逻辑删除后若还保留原唯一字段值，再次插入相同值时唯一索引会冲突：

```java
// 方案一：联合唯一索引（推荐）
// ALTER TABLE user ADD UNIQUE KEY uk_name (name, deleted);

// 方案二：删除标记使用时间戳
@TableLogic
private LocalDateTime deleteTime; // null-未删除，非null-已删除
// ALTER TABLE user ADD UNIQUE KEY uk_name (name, delete_time);

// 方案三：使用业务层手动处理
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> {
    public boolean saveWithUniqueCheck(User user) {
        // 先查询是否已删除同名用户
        LambdaQueryWrapper<User> qw = Wrappers.lambdaQuery();
        qw.eq(User::getName, user.getName());
        User exist = getOne(qw);
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        return save(user);
    }
}
```

### 3.4 全局配置自动填充创建时间、更新时间
```java
// 1. 实体类注解
@Data
public class BaseEntity {
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(fill = FieldFill.INSERT)
    @TableLogic
    private Integer deleted;
}

// 2. 元对象处理器
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "deleted", () -> 0, Integer.class);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
}
```

### 3.5 使用 DB 静态工具类跨表查询
MP 提供 `Db` 静态工具类，无需注入 Service 即可操作数据库：

```java
// 在非 Spring 管理的工具类中使用
public class ReportUtil {
    public static List<User> getActiveUsers() {
        return Db.lambdaQuery(User.class)
            .eq(User::getStatus, 1)
            .list();
    }
    
    public static boolean updateUser(User user) {
        return Db.updateById(user);
    }
    
    public static void batchSaveUsers(List<User> list) {
        Db.saveBatch(list);
    }
}
```

### 3.6 结合 Spring 事务使用 MP
MP 的 `ServiceImpl` 自带 `@Transactional` 支持，批量方法默认开启事务：

```java
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order, List<OrderItem> items) {
        // 保存订单
        save(order);
        // 保存订单项
        orderItemService.saveBatch(items);
        // 扣减库存...
    }
}
```

> ⚠️ **注意**：`saveBatch()` 内部是一个大事务，数据量大时可能锁时间过长。建议按业务拆分为多个小事务，或使用 `BATCH` ExecutorType。

---

## 四、手写代码题（5-8题）

### 4.1 完整配置 MyBatis-Plus 分页插件
```java
@Configuration
@MapperScan("com.example.demo.mapper")
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件（必须，限制最大页大小防止内存溢出）
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
```

### 4.2 手写条件构造器：复杂 AND/OR 嵌套
```java
// 需求：WHERE (age > 18 AND name LIKE '张%') OR (age < 10 AND status = 1)
LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class);
wrapper.and(w -> w.gt(User::getAge, 18).like(User::getName, "张"))
       .or(w -> w.lt(User::getAge, 10).eq(User::getStatus, 1));

// 更复杂的嵌套
// WHERE (age > 18 OR name IS NULL) AND (status = 1 OR deleted = 0)
wrapper.and(w -> w.gt(User::getAge, 18).or().isNull(User::getName))
       .and(w -> w.eq(User::getStatus, 1).or().eq(User::getDeleted, 0));

// 使用 apply 添加原生 SQL 片段（谨慎使用）
wrapper.apply("date_format(create_time,'%Y-%m-%d') = {0}", "2026-07-22");
```

### 4.3 通用分页实体与 MP 转换
```java
// 自定义通用分页请求
@Data
public class PageRequest {
    private long page = 1;
    private long size = 10;
    
    public <T> Page<T> toPage() {
        return new Page<>(page, size);
    }
}

// 自定义通用分页响应
@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long page;
    private long size;
    private long pages;
    
    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }
}

// Service 中使用
public PageResult<UserVO> queryUsers(PageRequest req) {
    Page<User> page = req.toPage();
    LambdaQueryWrapper<User> qw = Wrappers.lambdaQuery(User.class).eq(User::getStatus, 1);
    return PageResult.of(userMapper.selectPage(page, qw).convert(UserConvert.INSTANCE::toVO));
}
```

### 4.4 Service 层复杂业务接口实现
```java
// 批量新增或更新，返回带 ID 的实体列表
public List<User> batchSaveOrUpdate(List<User> userList) {
    List<User> result = new ArrayList<>();
    
    // 分批处理
    for (User user : userList) {
        if (user.getId() == null) {
            save(user);      // 新增，MP 自动回填 ID
            result.add(user);
        } else {
            User exist = getById(user.getId());
            if (exist == null) {
                save(user);
            } else {
                updateById(user);
            }
            result.add(user);
        }
    }
    // 或直接使用 MP 的 saveOrUpdateBatch
    saveOrUpdateBatch(userList);
    return userList;
}
```

### 4.5 手写 LambdaUpdateWrapper 实现条件更新
```java
// 批量锁定用户：将 status=0 且 lastLoginTime 超过 90 天的用户状态改为 2（冻结）
LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate(User.class);
wrapper.eq(User::getStatus, 0)
       .lt(User::getLastLoginTime, LocalDateTime.now().minusDays(90))
       .set(User::getStatus, 2)
       .set(User::getFreezeTime, LocalDateTime.now());
       
boolean updated = update(wrapper);
// 对应 SQL: UPDATE user SET status=2, freeze_time=? WHERE status=0 AND last_login_time < ?
```

### 4.6 自定义批量插入（高性能）
```java
// 方式一：注入批量 SQL
@Bean
public SqlInjector sqlInjector() {
    return new DefaultSqlInjector() {
        @Override
        public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
            List<AbstractMethod> list = super.getMethodList(mapperClass, tableInfo);
            list.add(new InsertBatchSomeColumn()); // 添加批量插入方法
            return list;
        }
    };
}

// Mapper 中直接使用
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // InsertBatchSomeColumn 注入的方法
    int insertBatchSomeColumn(List<User> list);
}

// 方式二：JDBC 配置批量提交
// jdbc:mysql://localhost:3306/db?rewriteBatchedStatements=true
```

---

## 五、系统设计题（3-5题）

### 5.1 设计一个支持多租户的应用架构（MP 多租户方案）
**需求**：SaaS 平台，每个租户的数据完全隔离，但使用同一套代码和数据库。

**方案**：
1. **数据隔离方案**：共享数据库 + 租户 ID 字段（成本最低，MP 多租户插件正好解决）
2. **插件配置**：
```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
        @Override
        public String getTenantId() {
            // 从 JWT Token 或 ThreadLocal 获取当前租户
            return SecurityContextHolder.getTenantId();
        }
        
        @Override
        public boolean ignoreTable(String tableName) {
            // 忽略字典表、配置表等不需要租户隔离的表
            return Arrays.asList("sys_dict", "sys_config").contains(tableName);
        }
    }));
    return interceptor;
}
```
3. **注意事项**：索引需包含租户 ID；跨租户查询通过忽略特定表实现；租户上下文通过过滤器在请求入口设置

### 5.2 设计一个百万级数据分页查询方案
**问题**：传统 `LIMIT offset, size` 在深分页（offset 大）时性能急剧下降。

**方案一：游标分页（推荐）**
```java
// 前端传 lastId（上一页最后一条的 ID），后端用 ID > lastId 查询
public IPage<User> cursorQuery(Long lastId, long size) {
    LambdaQueryWrapper<User> qw = Wrappers.lambdaQuery(User.class);
    qw.gt(lastId != null, User::getId, lastId) // ID 游标
      .orderByAsc(User::getId)
      .last("LIMIT " + size);
    List<User> list = userMapper.selectList(qw);
    return new Page<>(1, size, -1).setRecords(list);
}
```

**方案二：覆盖索引 + 延迟关联**
```java
// 第一步：只查主键（走覆盖索引）
Page<Long> idPage = new Page<>(current, size);
LambdaQueryWrapper<User> qw = Wrappers.lambdaQuery(User.class).select(User::getId);
page(idPage, qw);

// 第二步：用主键回表查完整数据
List<User> records = userMapper.selectBatchIds(idPage.getRecords());
```

**方案三：子查询优化（MySQL 5.6+）**
```sql
SELECT * FROM user 
WHERE id >= (SELECT id FROM user ORDER BY id LIMIT 1000000, 1) 
LIMIT 10;
```

### 5.3 百万级数据导出时如何防止 OOM？
```java
// 使用 MP 分页流式查询 + 游标
public void exportLargeData(HttpServletResponse response) {
    // 方式一：MP 分页逐页查询
    Page<User> page = new Page<>(1, 1000);
    do {
        IPage<User> result = userMapper.selectPage(page, queryWrapper);
        writeToFile(result.getRecords()); // 写入文件
        page = (Page<User>) result.nextPage();
    } while (page.getCurrent() <= page.getPages());
    
    // 方式二：MyBatis 游标（Cursor）
    // Mapper: Cursor<User> scanAll(@Param(Constants.WRAPPER) Wrapper<User> wrapper);
    try (Cursor<User> cursor = userMapper.scanAll(wrapper)) {
        Iterator<User> it = cursor.iterator();
        while (it.hasNext()) {
            writeToFile(it.next());
        }
    }
}
```

> 💡 **大厂面试加分点**：大数据场景避免一次性加载到内存，用游标或分页流式处理。MySQL 游标需要 `@Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = Integer.MIN_VALUE)` 配合。

---

## 六、常见坑点与最佳实践

| 坑点 | 原因 | 解决方案 |
|------|------|---------|
| 分页查询总记录数不对 | `selectPage` 传入 `Page` 对象后 MP 自动执行 COUNT，COUNT SQL 可能复杂 | 手动优化 COUNT 查询，或设置 `page.setOptimizeCountSql(false)` 后自定义 |
| LambdaQueryWrapper 泛型丢失 | 链式调用时泛型擦除 | 始终使用 `Wrappers.lambdaQuery()` 静态方法，不用 `new LambdaQueryWrapper<>()` |
| @TableField(exist=false) 字段被查询 | MP 默认查询所有非 `exist=false` 字段 | 检查实体类中是否漏配 `exist=false` |
| saveBatch 性能差 | MP 的 saveBatch 是逐条 INSERT，不是真正批量 INSERT | JDBC 连接加 `rewriteBatchedStatements=true`，或注入自定义批量方法 |
| 逻辑删除 + 唯一索引冲突 | 逻辑删除后记录仍存在 DB，唯一索引仍生效 | 联合唯一索引（加 deleted 字段）、或用时间戳替代逻辑删除标记 |
| @Version 乐观锁更新失败 | 多线程并发时 version 不匹配 | 业务层捕获 `OptimisticLockException` 后重试 |
| MetaObjectHandler 不生效 | `strictInsertFill` 只有在字段值为 null 时才填充 | 确认字段类型匹配，或使用 `setFieldValByName()` 强制填充 |
| 多租户插件影响非租户表 | 插件对所有 SQL 生效 | 在 `TenantLineHandler.ignoreTable()` 中排除不需要的表 |
| 自定义 SQL 中条件构造器不生效 | 忘记在 Mapper 参数中加 `@Param(Constants.WRAPPER)` | Mapper 方法参数用 `@Param(Constants.WRAPPER)` 注解 |
| 枚举处理器映射失败 | 枚举未实现 `IEnum` 接口或未配置全局处理器 | 枚举实现 `IEnum<T>`，并在 yml 中配置 `default-enum-type-handler` |

---

## 七、面试回答模板（Top 5 高频题的结构化回答模板）

### 7.1 MyBatis-Plus 相比 MyBatis 有哪些优势？
**话术结构**：核心增强 + 三个关键能力 + 一句话总结。

> 核心是「只做增强不做修改」。第一，**BaseMapper 和 IService 接口**提供了 15+ 个通用 CRUD 方法，单表操作无需写 SQL，消除了大量重复劳动。第二，**条件构造器（Wrapper）** 支持动态 SQL 拼接，Lambda 形式保证了类型安全。第三，**插件机制**非常强大，分页、乐观锁、多租户、逻辑删除等功能无需手写拦截器。一句话总结：MP 让 MyBatis 在单表 CRUD 上像 JPA 一样高效，同时保留了手写复杂 SQL 的灵活性，非常适合国内互联网开发节奏。

### 7.2 MP 分页插件的原理是什么？
**话术结构**：基于拦截器 + 两步执行（COUNT + SELECT）+ 数据库方言适配。

> MP 的 `PaginationInnerInterceptor` 基于 MyBatis 的 `Interceptor` 接口，拦截 `Executor.query()` 方法。当检测到参数包含 `Page` 对象时，分两步执行：先自动生成 `SELECT COUNT(1) FROM (原SQL) total` 查询总记录数，再将原 SQL 改写为带 `LIMIT offset, size` 的分页语句。不同的数据库（MySQL、Oracle、PostgreSQL）通过方言工厂适配。此外，通过 `maxLimit` 可以限制最大分页大小，防止恶意请求导致内存溢出。

### 7.3 如何实现乐观锁？原理是什么？
**话术结构**：@Version 注解 + 插件注册 + 版本号自增 + 失败重试。

> 在实体类中添加 `@Version` 注解的字段（如 `Integer version`），然后在配置类注册 `OptimisticLockerInnerInterceptor`。更新时会自动在 SQL 的 WHERE 条件追加 `version = oldVersion`，SET 中变为 `version = version + 1`。如果影响行数为 0，说明数据已被其他线程修改，抛出 `OptimisticLockException`。业务层需要捕获这个异常并重试。适用场景是 **读多写少** 的并发场景，避免了数据库行锁的开销。

### 7.4 @TableLogic 逻辑删除有哪些注意事项？
**话术结构**：原理 + 唯一索引 + 查询过滤 + 推荐实践。

> `@TableLogic` 注解将删除操作转换为 UPDATE 语句，查询时自动过滤已删除记录。主要注意事项有三点：**第一**，逻辑删除后的记录仍保留在表中，如果字段有唯一索引，再次插入相同值会冲突，解决方案是联合唯一索引或使用 `delete_time` 标记代替整型标记。**第二**，全局配置中统一 `logic-delete-value` 和 `logic-not-delete-value`。**第三**，某些统计 SQL 需要包含已删除数据时，可以在 Wrapper 中手动调用 `notLogicDelete()` 或自定义 SQL 忽略逻辑删除条件。

### 7.5 条件构造器如何防止 SQL 注入？
**话术结构**：预编译占位符 + Wrapper 内部机制 + 对比 ${}。

> Wrapper 内部所有用户输入的值都通过 `Map<String, Object> paramNameValuePairs` 存储，在 SQL 中以 `#{ew.paramNameValuePairs.xxx}` 预编译占位符形式呈现，不会参与 SQL 拼接。这与 MyBatis 的 `#{}` 语法原理一致——参数值被预编译引擎转义，用户输入的任何恶意 SQL 片段都会被当作普通字符串处理。需要注意的是，如果开发者手动在 Wrapper 中使用了 `apply("列名 = {0}", value)` 的 `{0}` 语法是安全的，但如果直接在 `apply` 中拼接字符串则仍有风险。

---

## 八、快速查漏补缺 Checklist

**基础层**：
- [ ] 理解 MyBatis-Plus 的「只做增强不做修改」原则
- [ ] 熟悉 BaseMapper 的 15+ 个 CRUD 方法及方法签名
- [ ] 理解 IService 与 BaseMapper 的结构差异
- [ ] 掌握 @TableName、@TableId、@TableField、@TableLogic 四大注解
- [ ] 熟悉主键策略的 5 种类型及选择场景

**条件构造器**：
- [ ] 能区分 QueryWrapper、UpdateWrapper、LambdaQueryWrapper、LambdaUpdateWrapper
- [ ] 掌握 AND/OR 嵌套写法（lambda 嵌套写法）
- [ ] 理解 `eq`、`ne`、`gt`、`ge`、`lt`、`le`、`like`、`in`、`isNull` 等 20+ 条件方法
- [ ] 知道 `apply` 用于原生 SQL 片段及注入风险

**插件机制**：
- [ ] MybatisPlusInterceptor 注册方式
- [ ] PaginationInnerInterceptor 分页插件原理
- [ ] OptimisticLockerInnerInterceptor 乐观锁原理
- [ ] TenantLineInnerInterceptor 多租户
- [ ] DynamicTableNameInnerInterceptor 动态表名

**扩展功能**：
- [ ] AutoGenerator 代码生成器配置（4 大配置类）
- [ ] Db 静态工具类使用
- [ ] MybatisEnumTypeHandler 枚举处理器
- [ ] JacksonTypeHandler JSON 处理器
- [ ] MetaObjectHandler 自动填充接口

**实战能力**：
- [ ] 通用分页实体封装（PageRequest + PageResult）
- [ ] 自定义 SQL + 条件构造器整合
- [ ] saveBatch 性能优化（rewriteBatchedStatements）
- [ ] 深分页优化（游标分页/覆盖索引）
- [ ] 逻辑删除 + 唯一索引冲突解决方案

**面试高频题**（阿里/美团/字节）：
- [ ] MP vs MyBatis vs JPA 三选一场景题
- [ ] 分页插件深分页优化方案
- [ ] 乐观锁失败后如何处理
- [ ] 多租户数据隔离架构设计
- [ ] 雪花算法的 ID 结构及时钟回拨问题
- [ ] 百万级数据导出设计方案
- [ ] 条件构造器源码级原理
