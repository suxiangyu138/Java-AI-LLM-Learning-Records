# MyBatis 与 MyBatis-Plus（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MyBatis vs MyBatis-Plus  
> **核心关系**：MP 是 MyBatis 的增强工具，非替代品，两者可共存  
> **前置基础**：MySQL 基础、SQL、Spring Boot 项目配置

---

## 一、核心概念

### 1.1 两者的关系

MyBatis-Plus 并非替代 MyBatis，而是 **基于 MyBatis 的增强工具**，遵循"不改变 MyBatis 原有功能、不侵入原有代码"原则，在 MyBatis 基础上增加了 CRUD 接口封装、条件构造器、分页插件等功能。

| 维度 | MyBatis | MyBatis-Plus |
|------|---------|-------------|
| **定位** | SQL 映射框架，灵活性高 | MyBatis 增强工具，简化开发 |
| **开发效率** | 需手写 XML + SQL | BaseMapper 提供默认 CRUD，无需写基础 SQL |
| **SQL 控制** | 灵活性极高，任意复杂 SQL | 兼容 MyBatis + 条件构造器 |
| **功能特性** | 基础：SQL 映射、参数绑定、结果映射 | 增强：条件构造器、分页插件、乐观锁、自动填充、逻辑删除 |
| **学习成本** | 中等 | 低（基于 MyBatis） |

> **实战选型**：简单 CRUD 用 MP 提升效率，复杂 SQL（多表关联、优化）用 MyBatis 手动编写，两者 **可共存于同一项目**。

---

## 二、底层原理

### 2.1 MyBatis 核心原理

```
Java Mapper 接口 → 动态代理（MapperProxy）→ 解析 XML/注解 → 参数绑定 #{} → 执行 SQL → ResultSet → 对象映射
```

### 2.2 MyBatis-Plus 增强机制

| 增强点 | 原理 |
|--------|------|
| **BaseMapper** | 通过泛型推断实体类 → 自动生成 CRUD SQL → 注入到 Mapper 代理 |
| **条件构造器** | 链式 API 构建 `Wrapper` 对象 → 解析为 SQL WHERE 子句 |
| **分页插件** | MyBatis 拦截器（Interceptor）→ 拦截 SQL → 追加 LIMIT 子句 + count 查询 |
| **乐观锁** | 拦截 UPDATE 语句 → 追加 `version = ?` 条件 → 更新 version + 1 |
| **自动填充** | `MetaObjectHandler` 在 INSERT/UPDATE 时自动设置 `create_time`/`update_time` |

---

## 三、代码实现

### 3.1 MyBatis 配置

```yaml
# application.yml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://192.168.1.100:3306/db_ecommerce?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: db_ecommerce_user
    password: Xx@123456

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.ecommerce.entity
  configuration:
    map-underscore-to-camel-case: true  # 下划线转驼峰
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 3.2 MyBatis XML 映射

```xml
<mapper namespace="com.example.ecommerce.mapper.UserOrderMapper">
    <resultMap id="userOrderMap" type="com.example.ecommerce.vo.UserOrderVO">
        <result column="user_id"   property="userId"/>
        <result column="username"  property="username"/>
        <result column="order_id"  property="orderId"/>
        <result column="order_no"  property="orderNo"/>
    </resultMap>

    <select id="selectUserOrder" resultMap="userOrderMap">
        SELECT u.id AS user_id, u.username, o.id AS order_id, o.order_no
        FROM t_user u
        INNER JOIN t_order o ON u.id = o.user_id
        WHERE u.id = #{userId}
    </select>
</mapper>
```

### 3.3 MyBatis-Plus 配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.3.1</version>
</dependency>
```

```yaml
# application.yml — MP 配置兼容 MyBatis 配置
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.ecommerce.entity
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: AUTO                    # 主键自增
      logic-delete-field: isDelete     # 逻辑删除字段名
      logic-delete-value: 1            # 已删除标识
      logic-not-delete-value: 0        # 未删除标识
```

### 3.4 MP BaseMapper（免写基础 CRUD）

```java
/**
 * 用户 Mapper —— 继承 BaseMapper 后自动获得 CRUD 方法。
 */
public interface UserMapper extends BaseMapper<User> {
    // 复杂查询可手动添加，兼容 MyBatis
    List<User> selectUserByUsername(@Param("username") String username);
}
```

```java
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean addUser(User user) {
        return userMapper.insert(user) > 0;  // BaseMapper 自带方法
    }

    @Override
    public User selectUserById(Long id) {
        return userMapper.selectById(id);    // BaseMapper 自带方法
    }

    @Override
    public List<User> selectUserByUsername(String username) {
        return userMapper.selectUserByUsername(username);  // 手动定义
    }
}
```

### 3.5 MP 条件构造器

```java
// QueryWrapper：链式 API 拼接查询条件
QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
queryWrapper.eq("user_id", 1)
            .eq("order_status", 1)
            .eq("is_delete", 0)
            .orderByDesc("create_time");
List<Order> orders = orderMapper.selectList(queryWrapper);

// LambdaQueryWrapper：类型安全，避免字段名拼写错误
LambdaQueryWrapper<Order> lambdaQuery = new LambdaQueryWrapper<>();
lambdaQuery.eq(Order::getUserId, 1)
           .eq(Order::getOrderStatus, 1)
           .orderByDesc(Order::getCreateTime);
List<Order> orders2 = orderMapper.selectList(lambdaQuery);
```

### 3.6 MP 分页插件

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

```java
// 分页查询
Page<Order> page = new Page<>(1, 10);  // 第 1 页，每页 10 条
Page<Order> result = orderMapper.selectPage(page, null);
```

### 3.7 乐观锁

```java
// 实体类版本字段
@Version
private Integer version;
```

```java
// 配置乐观锁拦截器
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
}
```

```java
// 更新时自动拼接 version = ? 条件
order.setOrderStatus(1);
orderMapper.updateById(order);  // SQL: UPDATE ... SET ... version=version+1 WHERE id=? AND version=?
```

---

## 四、实战要点

### 4.1 技术选型决策

| 业务场景 | 推荐 | 原因 |
|----------|------|------|
| 单表 CRUD | **MP** | BaseMapper 免写 SQL，效率高 |
| 多表关联查询 | **MyBatis** | 灵活编写 JOIN，比条件构造器更清晰 |
| 复杂统计 SQL | **MyBatis** | 手写 SQL 可控性最强 |
| 分页查询 | **MP 分页插件** | 一行代码实现物理分页 |
| 乐观锁 | **MP @Version** | 自动拼接 version 条件 |
| 逻辑删除 | **MP 自动填充** | `global-config.db-config` 统一配置 |

### 4.2 项目中共存的架构

```
src/main/java/com/example/
├── mapper/
│   ├── UserMapper.java        extends BaseMapper<User>  （MP：单表 CRUD）
│   └── UserOrderMapper.java   （MyBatis：复杂多表查询）
├── config/
│   └── MybatisPlusConfig.java （分页插件 + 乐观锁配置）
└── resources/mapper/
    └── UserOrderMapper.xml    （复杂 SQL 手动编写）
```

---

## 五、避坑总结

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| **MP 与 MyBatis 依赖冲突** | 同时引入 mybatis 和 mybatis-plus | 只用 `mybatis-plus-boot-starter`（已包含 MyBatis） |
| **分页插件不生效** | 未注册 `PaginationInnerInterceptor` | 必须配置 `@Bean MybatisPlusInterceptor` |
| **乐观锁更新失败** | 未加 `@Version` 注解 | 实体类 version 字段加 `@Version` |
| **BaseMapper 方法不存在** | Mapper 未继承 `BaseMapper<T>` | `extends BaseMapper<你的实体类>` |
| **实体类字段与数据库不匹配** | 未启用驼峰映射 | 配置 `map-underscore-to-camel-case: true` |

---

## 六、企业级最佳实践

### 6.1 项目结构规范

```
单表 CRUD → MP BaseMapper（无需 XML）
多表关联 / 复杂查询 → MyBatis XML（手动编写 SQL）
两者共用同一项目，互不冲突
```

### 6.2 从 MyBatis 迁移到 MP 步骤

1. 替换依赖：`mybatis-spring-boot-starter` → `mybatis-plus-boot-starter`
2. 修改配置前缀：`mybatis.*` → `mybatis-plus.*`
3. Mapper 继承 `BaseMapper<T>`
4. 删除已有基础 CRUD 的 XML（保留复杂查询 XML）
5. 添加分页/乐观锁插件配置

### 6.3 开发规范

- **简单单表** → 用 MP BaseMapper，禁止手写 XML
- **复杂查询** → 写 MyBatis XML，充分利用 SQL 灵活性
- **禁止混用**：同一查询不要同时用 MP 条件构造器和 XML
- **LambdaQueryWrapper 优先**：类型安全 + IDE 重构友好
