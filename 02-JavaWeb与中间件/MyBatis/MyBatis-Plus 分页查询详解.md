# MyBatis-Plus 分页查询详解

## 基础配置

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

## 基本分页

```java
// Controller
@GetMapping("/users")
public Result<IPage<UserVO>> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
    Page<User> pageParam = new Page<>(page, size);
    IPage<UserVO> result = userService.pageUsers(pageParam);
    return Result.ok(result);
}

// Service
public IPage<UserVO> pageUsers(Page<User> page) {
    return userMapper.selectPage(page, new LambdaQueryWrapper<User>()
            .eq(User::getStatus, "ACTIVE")
            .orderByDesc(User::getCreatedAt));
}
```

## 常用分页 API

```java
// 基础分页
Page<User> page = new Page<>(1, 10);
userMapper.selectPage(page, null);

// 带条件分页
userMapper.selectPage(page,
    new LambdaQueryWrapper<User>()
        .like(User::getUsername, "张")
        .between(User::getAge, 18, 30)
        .orderByDesc(User::getCreatedAt));

// 分页 + 关联查询（自定义 SQL）
IPage<UserVO> page = userMapper.selectUserPage(new Page<>(1, 10), "张");
```

```java
// Mapper 中自定义分页 SQL
@Mapper
public interface UserMapper extends BaseMapper<User> {
    IPage<UserVO> selectUserPage(Page<?> page, @Param("keyword") String keyword);
}
```

```xml
<select id="selectUserPage" resultType="com.example.vo.UserVO">
    SELECT u.*, d.name AS dept_name
    FROM user u LEFT JOIN department d ON u.dept_id = d.id
    <where>
        <if test="keyword != null and keyword != ''">
            AND u.username LIKE CONCAT('%', #{keyword}, '%')
        </if>
    </where>
    ORDER BY u.created_at DESC
</select>
```

返回的 Page 对象包含：

| 字段 | 说明 |
|------|------|
| `records` | 当前页数据列表 |
| `total` | 总记录数 |
| `size` | 每页条数 |
| `current` | 当前页码 |
| `pages` | 总页数 |

## 分页插件原理

PaginationInnerInterceptor 拦截 SQL 执行：
1. 先执行 `SELECT COUNT(*)` 获取总数
2. 再执行 `SELECT ... LIMIT offset, size` 获取当前页

关闭 count 查询（如果不需要总数）：
```java
page.setSearchCount(false);
```

## 性能优化

**大 offset 问题：**
`LIMIT 1000000, 20` 需要扫描 100 万行再抛弃。MySQL 实际需要回表读取这 100 万行。

**解决方案 — 延迟关联：**
```sql
-- 优化前
SELECT * FROM user ORDER BY id LIMIT 1000000, 20;

-- 优化后（先用覆盖索引找到 ID，再关联取完整数据）
SELECT u.* FROM user u
INNER JOIN (SELECT id FROM user ORDER BY id LIMIT 1000000, 20) t
ON u.id = t.id;
```

**解决方案 — 游标分页**（推荐替代深分页）：
```java
// 用上一页最后一条的 ID 作为起点
SELECT * FROM user WHERE id > #{lastId} ORDER BY id LIMIT 20;
```
不再需要 OFFSET，索引直接定位，性能稳定。

## 分页与缓存

首页等常用页可以加 Redis 缓存，但要注意：
- 数据变化时及时失效缓存
- 仅缓存前几页（通常用户只看前几页）
- 缓存 key 包含查询条件 hash，防止条件混淆
