# 07 - MyBatis-Plus 快速开发

> 🎯 MyBatis-Plus 是 MyBatis 的增强工具——自动生成 CRUD、条件构造器、分页插件、逻辑删除，让单表开发效率提升 5 倍

---

## 目录

1. [核心特性](#1-核心特性)
2. [条件构造器 Wrapper](#2-条件构造器-wrapper)
3. [分页与代码生成](#3-分页与代码生成)

---

## 1. 核心特性

```java
// Entity 定义
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)     // 自增主键
    private Long id;

    @TableField("user_name")         // 列名映射
    private String name;

    @TableField(select = false)      // 查询时不返回（隐私字段）
    private String password;

    @TableLogic                      // 逻辑删除
    private Integer deleted;         // 0=正常, 1=删除

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

```java
// Mapper 接口 — 继承了 BaseMapper 就有全套 CRUD
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 不需要写任何方法！BaseMapper 自带：
    // insert / deleteById / updateById / selectById / selectList ...
}

// Service 接口 — 继承 IService 有全套业务方法
@Service
public class UserServiceImpl
    extends ServiceImpl<UserMapper, User>
    implements IUserService {
    // save / remove / update / getOne / list / page ...
}
```

```java
// 使用 — 原来需要写 50 行的 CRUD，现在一行搞定
List<User> users = userMapper.selectList(null);             // 查全部
User user = userMapper.selectById(1L);                      // 查单个
userMapper.insert(new User("Alice"));                        // 插入
userMapper.updateById(user);                                 // 更新
userMapper.deleteById(1L);                                   // 逻辑删除（@TableLogic）
```

## 2. 条件构造器 Wrapper

```java
// QueryWrapper: 链式查询条件
List<User> users = userMapper.selectList(
    new QueryWrapper<User>()
        .eq("status", "ACTIVE")                    // status = 'ACTIVE'
        .like("name", "张")                        // name LIKE '%张%'
        .ge("age", 18)                              // age >= 18
        .orderByDesc("create_time")                 // 降序
        .last("LIMIT 10")                           // 限制 10 条
);

// Lambda 写法（类型安全，避免字符串写错列名！）✅
List<User> users = userMapper.selectList(
    new LambdaQueryWrapper<User>()
        .eq(User::getStatus, "ACTIVE")
        .like(User::getName, "张")
        .ge(User::getAge, 18)
);

// UpdateWrapper: 条件更新
userMapper.update(null,
    new UpdateWrapper<User>()
        .set("status", "INACTIVE")
        .eq("email", "old@example.com")
);
```

### 条件构造器速查

| 方法 | SQL | 说明 |
|------|-----|------|
| `eq("name", "A")` | name = 'A' | 相等 |
| `ne("name", "A")` | name != 'A' | 不等 |
| `like("name", "A")` | name LIKE '%A%' | 模糊 |
| `gt/ge/lt/le` | > / >= / < / <= | 比较 |
| `between("age", 18, 30)` | age BETWEEN 18 AND 30 | 区间 |
| `in("id", ids)` | id IN (1,2,3) | 包含 |
| `isNull("email")` | email IS NULL | 空值 |
| `orderByAsc/Desc` | ORDER BY | 排序 |
| `groupBy` | GROUP BY | 分组 |

## 3. 分页与代码生成

```java
// 1. 配置分页插件
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}

// 2. 分页查询
Page<User> page = new Page<>(1, 10);  // 第 1 页, 每页 10 条
Page<User> result = userMapper.selectPage(page,
    new LambdaQueryWrapper<User>().eq(User::getStatus, "ACTIVE"));
// result.getRecords() → 数据列表
// result.getTotal()   → 总记录数
// result.getPages()   → 总页数
```

```java
// 代码生成器 — 写完 Entity 运行 → 自动生成 Mapper/Service/Controller
public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create(
            "jdbc:mysql://localhost:3306/mydb", "root", "root")
            .globalConfig(builder -> builder
                .author("dev").outputDir("src/main/java"))
            .packageConfig(builder -> builder
                .parent("com.example"))
            .strategyConfig(builder -> builder
                .addInclude("users", "orders"))  // 要生成的表
            .execute();
    }
}
```

## 核心要点回顾

- `BaseMapper<T>` = 自带全套单表 CRUD（不用写任何方法）
- LambdaQueryWrapper > QueryWrapper（类型安全，Refactor 友好）
- 逻辑删除 = `@TableLogic` 注解 + 全局配置
- 分页 = `PaginationInnerInterceptor` + `Page<T>`
- 自动填充 = `@TableField(fill = ...)` + `MetaObjectHandler`
- 代码生成器 = 一张表 → 5 分钟生成全套 CRUD 代码

## 参考资料

1. MyBatis-Plus 官方文档 — baomidou.com
