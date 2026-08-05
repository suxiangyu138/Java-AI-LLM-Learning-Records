# 08 - MyBatis 性能优化

> 🎯 MyBatis 的性能问题通常不在框架本身，而在 SQL 写法和使用方式。批量操作、N+1 问题、延迟加载——这三个问题是 90% 的性能瓶颈所在

---

## 目录

1. [批量操作优化](#1-批量操作优化)
2. [N+1 问题解决](#2-n1-问题解决)
3. [延迟加载](#3-延迟加载)

---

## 1. 批量操作优化

```java
// ❌ 逐条插入：1000 条 = 1000 次 SQL
for (User user : users) {
    mapper.insert(user);
}

// ✅ 批量插入：一条 SQL 插入 1000 条
// XML:
// INSERT INTO users (name, email) VALUES
// <foreach collection="list" item="u" separator=",">
//     (#{u.name}, #{u.email})
// </foreach>

// MyBatis-Plus:
userService.saveBatch(users, 500);  // 每 500 条一个批次

// ✅ SqlSession Batch 模式（最高性能）
try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    for (User user : users) {
        mapper.insert(user);
    }
    session.commit();  // 一次性提交
}
```

```text
性能对比（10000 条数据）：
  逐条 insert：~30s
  foreach 批量：~2s
  Batch 模式：  ~0.5s
```

## 2. N+1 问题解决

```text
N+1 问题：查 1 次主表 → 查 N 次关联表

示例：
  1. SELECT * FROM orders                          ← 1 次（查出 100 个订单）
  2. SELECT * FROM users WHERE id = ?  ← 100 次（每个订单查一次用户）
  总计：101 次查询！

原因：嵌套查询（association 中用了 select 属性）
```

```xml
<!-- ❌ 嵌套查询：导致 N+1 -->
<resultMap id="orderMap" type="Order">
    <association property="user" column="user_id"
        select="com.example.UserMapper.findById"/>   <!-- N 次查询！ -->
</resultMap>

<!-- ✅ 嵌套结果：一条 JOIN SQL 搞定 -->
<resultMap id="orderMap" type="Order">
    <association property="user" javaType="User">
        <id property="id" column="user_id"/>
        <result property="name" column="user_name"/>
    </association>
</resultMap>
<select id="findAll" resultMap="orderMap">
    SELECT o.*, u.name as user_name
    FROM orders o LEFT JOIN users u ON o.user_id = u.id
</select>
```

## 3. 延迟加载

```xml
<!-- mybatis-config.xml -->
<settings>
    <setting name="lazyLoadingEnabled" value="true"/>   <!-- 全局延迟加载 -->
    <setting name="aggressiveLazyLoading" value="false"/>  <!-- 按需加载 -->
</settings>
```

```xml
<!-- 只在使用 order.getUser() 时才加载 User（单独发 SQL） -->
<resultMap id="orderMap" type="Order">
    <association property="user" column="user_id"
        select="com.example.UserMapper.findById"
        fetchType="lazy"/>     <!-- 延迟加载 -->
</resultMap>
```

```text
延迟加载 vs 嵌套结果：
  延迟加载：主表查询快，按需加载关联对象（适合关联对象不总使用）
  嵌套结果：一条 SQL 搞定，避免 N+1（适合关联对象总使用）

选择：
  → 关联对象大概率被用到 → 嵌套结果（JOIN）
  → 关联对象很少被用到 → 延迟加载（分离查询）
```

## 核心要点回顾

- 批量操作三选一：foreach XML > MyBatis-Plus saveBatch > Batch Executor
- N+1 根因 = 嵌套查询（association 中用了 select）
- 解决方案 = 嵌套结果（一条 JOIN SQL）替代嵌套查询
- 延迟加载 = 关联对象按需加载（`fetchType="lazy"`）
- 批量操作时事务内 `sqlSession.flushStatements()` 定期刷盘（防止 OOM）

## 参考资料

1. MyBatis 性能优化指南
2. MyBatis-Plus 批量操作文档
