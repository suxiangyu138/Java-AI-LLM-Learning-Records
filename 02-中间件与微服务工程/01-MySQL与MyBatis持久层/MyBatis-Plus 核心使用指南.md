# MyBatis-Plus 核心使用指南

## 定位

MyBatis-Plus 是 MyBatis 的增强工具，**只做增强不做修改**。你仍然可以用 MyBatis 的一切能力，MP 帮你省掉重复的 CRUD SQL。

## Mapper 继承 BaseMapper

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承后自带 CRUD，不需要写任何 SQL
}
```

自带方法一览：
```java
userMapper.insert(user);                     // 插入
userMapper.deleteById(1L);                   // 按 ID 删除
userMapper.delete(new LambdaQueryWrapper<User>()
        .eq(User::getStatus, "INACTIVE"));   // 条件删除
userMapper.updateById(user);                 // 按 ID 更新
userMapper.update(user, new LambdaUpdateWrapper<User>()
        .set(User::getStatus, "ACTIVE")
        .eq(User::getId, 1L));               // 条件更新（只更新指定字段）
userMapper.selectById(1L);                   // 按 ID 查询
userMapper.selectList(null);                 // 查询全部
userMapper.selectCount(null);                // 计数
```

## LambdaQueryWrapper（类型安全）

```java
// 传统方式：字段名写死（容易出错）
new QueryWrapper<User>().eq("username", "zhangsan");

// Lambda 方式：编译器检查（推荐）
new LambdaQueryWrapper<User>().eq(User::getUsername, "zhangsan");

// 常用条件
new LambdaQueryWrapper<User>()
    .eq(User::getStatus, "ACTIVE")           // 等于
    .ne(User::getDeleted, 1)                 // 不等于
    .gt(User::getAge, 18)                    // 大于
    .ge(User::getAge, 18)                    // 大于等于
    .lt(User::getAge, 60)                    // 小于
    .le(User::getAge, 60)                    // 小于等于
    .like(User::getUsername, "张")           // 模糊
    .in(User::getId, Arrays.asList(1, 2, 3)) // IN
    .between(User::getAge, 18, 30)           // 范围
    .isNull(User::getEmail)                  // IS NULL
    .orderByDesc(User::getCreatedAt)         // 降序
    .last("LIMIT 10");                       // 尾部追加 SQL
```

## LambdaUpdateWrapper

```java
new LambdaUpdateWrapper<User>()
    .set(User::getStatus, "INACTIVE")        // SET status = 'INACTIVE'
    .set(User::getUpdatedAt, LocalDateTime.now())
    .eq(User::getId, 1L);                    // WHERE id = 1

userMapper.update(null, wrapper);            // 第一个参数 null，只更新 set 的字段
```

## 主键策略

```java
public class User {
    @TableId(type = IdType.AUTO)         // 数据库自增
    private Long id;                     // → 默认雪花算法
}

@TableId(type = IdType.ASSIGN_ID)        // 雪花算法（默认）推荐
@TableId(type = IdType.AUTO)             // 数据库自增
@TableId(type = IdType.INPUT)            // 手动输入
@TableId(type = IdType.ASSIGN_UUID)      // UUID（去横线）
```

## 逻辑删除

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted        # 逻辑删除字段
      logic-delete-value: 1              # 已删除
      logic-not-delete-value: 0          # 未删除
```

```java
public class User {
    @TableLogic
    private Integer deleted;
}
```

之后 `deleteById(1L)` 自动变成 `UPDATE user SET deleted=1 WHERE id=1`，查询自动加 `deleted=0`。

## 自动填充

```java
@Component
public class MetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

```java
public class User {
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

## Service 层封装

```java
public interface UserService extends IService<User> { }

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService { }
```

IService 自带的方法：
```java
userService.save(user);                    // 单个保存
userService.saveBatch(list);               // 批量保存
userService.saveOrUpdate(user);            // 有 ID 则更新，无则插入
userService.removeById(1L);                // 删除
userService.updateById(user);              // 更新
userService.getById(1L);                   // 查询
userService.listByIds(ids);                // 批量查
userService.page(page, wrapper);           // 分页
```

## 常用配置

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志
    map-underscore-to-camel-case: true   # 下划线转驼峰（user_name → userName）
  global-config:
    db-config:
      id-type: assign_id                 # 主键策略
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath:/mapper/**/*.xml
```
