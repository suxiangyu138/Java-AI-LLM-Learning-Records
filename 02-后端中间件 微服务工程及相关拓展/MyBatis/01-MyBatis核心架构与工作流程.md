# 01 - MyBatis 核心架构与工作流程

> 🎯 MyBatis 的工作流程 = 加载配置 → 创建 SqlSessionFactory → 获取 SqlSession → 执行 SQL → 映射结果。理解这条链路上的每个组件，是阅读源码和排查问题的基础

---

## 目录

1. [核心组件](#1-核心组件)
2. [完整工作流程](#2-完整工作流程)
3. [JDBC vs MyBatis](#3-jdbc-vs-mybatis)

---

## 1. 核心组件

```text
MyBatis 核心架构：

┌─────────────────────────────────────────────┐
│               mybatis-config.xml             │  ← 全局配置
│          (数据源/事务/映射文件/插件)            │
└───────────────────┬─────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│         SqlSessionFactoryBuilder              │  ← 建造者
│              .build(config)                  │
└───────────────────┬─────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│          SqlSessionFactory                    │  ← 单例（全局一个）
│              .openSession()                  │
└───────────────────┬─────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│            SqlSession                         │  ← 每次请求一个
│   getMapper() / selectOne() / insert()       │
└───────────────────┬─────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│              Executor                         │  ← 执行引擎
│   Simple / Reuse / Batch / CachingExecutor   │
└───────────────────┬─────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│          MappedStatement                      │  ← SQL + 映射规则
│   (SQL / parameterMap / resultMap)           │
└───────────────────┬─────────────────────────┘
                    ▼
┌──────────┐  ┌──────────┐  ┌───────────────┐
│Parameter │  │Statement │  │ ResultSet     │
│ Handler  │→ │ Handler  │→ │ Handler       │
└──────────┘  └──────────┘  └───────────────┘
```

| 组件 | 职责 | 生命周期 |
|------|------|:---:|
| **SqlSessionFactory** | 创建 SqlSession | 应用级（全局单例） |
| **SqlSession** | 执行 SQL + 获取 Mapper | 请求级（用完关闭） |
| **Executor** | 真正执行 SQL 的核心引擎 | SqlSession 内 |
| **MappedStatement** | SQL + 参数/结果映射规则的封装 | 应用级 |
| **TypeHandler** | Java 类型 ↔ JDBC 类型 | 应用级 |

## 2. 完整工作流程

```java
// 1. 加载配置
String resource = "mybatis-config.xml";
InputStream inputStream = Resources.getResourceAsStream(resource);
SqlSessionFactory sqlSessionFactory =
    new SqlSessionFactoryBuilder().build(inputStream);

// 2. 获取 SqlSession
try (SqlSession session = sqlSessionFactory.openSession()) {

    // 3a. 方式一：直接执行 SQL
    User user = session.selectOne("com.example.UserMapper.findById", 1L);

    // 3b. 方式二：获取 Mapper 代理（推荐）
    UserMapper mapper = session.getMapper(UserMapper.class);
    User user2 = mapper.findById(1L);

    // 4. 提交事务
    session.commit();
}
// 5. SqlSession 自动关闭（try-with-resources）
```

```text
Mapper 代理的工作原理：

UserMapper mapper = session.getMapper(UserMapper.class);
  → JDK 动态代理 → MapperProxy
  → 拦截 mapper.findById() 调用
  → 从 MappedStatement 获取 SQL
  → SqlSession.selectOne() 执行
  → 结果映射为 User 对象
```

## 3. JDBC vs MyBatis

```java
// JDBC 原生：样板代码地狱
Connection conn = null; PreparedStatement ps = null; ResultSet rs = null;
try {
    conn = DriverManager.getConnection(url, user, password);
    ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
    ps.setLong(1, 1L);
    rs = ps.executeQuery();
    if (rs.next()) {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        // ... 10 行映射代码
    }
} finally {
    if (rs != null) rs.close();
    if (ps != null) ps.close();
    if (conn != null) conn.close();
}

// MyBatis：一行搞定
User user = mapper.findById(1L);
```

## 核心要点回顾

- SqlSessionFactory 全局唯一（创建开销大）
- SqlSession 用完即关（每次请求一个）
- `getMapper()` 本质是 JDK 动态代理
- Executor 有三种：Simple(默认)/Reuse(复用Statement)/Batch(批量)
- 所有 SQL 都在 MappedStatement 中（XML namespace+id 定位）

## 参考资料

1. MyBatis 官方文档 — mybatis.org
2. MyBatis 3 源码 — github.com/mybatis/mybatis-3
