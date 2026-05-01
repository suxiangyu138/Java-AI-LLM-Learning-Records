
## 总体学习路线（先速成，再进阶）

- 第 1 天：快速跑通一个 MyBatis 示例（普通 Java 项目或 Spring Boot 均可）。  
- 第 2～3 天：熟悉 Mapper 写法、增删改查、参数和结果映射。  
- 第 4～7 天：掌握动态 SQL、关联查询、分页、常见踩坑。  
- 后续一周：结合 Spring/Spring Boot 做一个小项目，顺带看一眼缓存、插件机制等进阶内容。 [dunwu.github](https://dunwu.github.io/java-tutorial/pages/d4e6ee/)

下面按“快速上手 → 日常必会 → 进阶原理”拆开。

***

## 第 1 步：快速跑通 MyBatis Demo

目标：1～2 小时内，跑通一个最简单的查询，把“套路”过一遍。 [developer.aliyun](https://developer.aliyun.com/article/1000910)

核心步骤（普通 Java + Maven）：

1. 准备数据库和表  
   - 建一个 `db_mybatis` 数据库，建一张 `user` 表，插入几条测试数据。 [cnblogs](https://www.cnblogs.com/itcq1024/p/18577106)
2. 创建 Maven 项目并添加依赖  
   - 至少要有：`mybatis`、数据库驱动（如 `mysql-connector-j`）、日志依赖。 [mybatis](https://mybatis.org/mybatis-3/zh_CN/getting-started.html)
3. 写 MyBatis 核心配置 `mybatis-config.xml`  
   - 配置数据源（或交给外部），配置环境 `environments`，注册 `mapper` 文件。 [developer.aliyun](https://developer.aliyun.com/article/1000910)
4. 写实体类 + Mapper.xml  
   - 实体 `User` 对应表字段；  
   - `UserMapper.xml` 中写一个 `select * from user` 的 `select` 语句，配置 `resultType` 或 `resultMap`。 [cnblogs](https://www.cnblogs.com/itcq1024/p/18577106)
5. 写一段测试代码  
   - 用 `Resources.getResourceAsStream("mybatis-config.xml")` 加载配置，构建 `SqlSessionFactory`，  
   - 再通过 `openSession()` 拿 `SqlSession`，执行 `selectList`，打印结果。 [dunwu.github](https://dunwu.github.io/java-tutorial/pages/d4e6ee/)

你可以参考一篇“从 0 开始搭环境 + 完整示例”的中文博文，按步骤抄一遍即可。 [cloud.tencent](https://cloud.tencent.com/developer/article/2037944)

***

## 第 2 步：日常开发必会功能（CRUD + 常见写法）

跑通 HelloWorld 后，重点是把常用功能练熟。 [edu.51cto](https://edu.51cto.com/article/note/15309.html)

1. Mapper 接口 + XML 映射  
   - 使用“接口 + XML”的经典方式：定义 `UserMapper` 接口，方法如 `selectById(Long id)`；  
   - 在 `UserMapper.xml` 中用 `<mapper namespace="全限定接口名">`，每个 SQL 用一个 `<select>/<insert>/<update>/<delete>` 映射方法。 [dunwu.github](https://dunwu.github.io/java-tutorial/pages/d4e6ee/)

2. 参数传递和返回值  
   - 单个简单参数：`Long id` 可以直接用 `#{id}`；  
   - 多参数：用 `@Param("name")` 或封装成 DTO/实体；  
   - 返回单对象、列表、受影响行数等多种形式。 [edu.51cto](https://edu.51cto.com/article/note/15309.html)

3. 增删改查套路  
   - 查询：精确查、模糊查、条件查询；  
   - 新增：返回自增主键（`useGeneratedKeys="true" keyProperty="id"`）；  
   - 更新：只更新部分字段 vs 全量更新；  
   - 删除：逻辑删除字段配合更新。 [developer.aliyun](https://developer.aliyun.com/article/1000910)

4. 基本配置与日志  
   - 在 `mybatis-config.xml` 配日志实现（如 logback），确认 SQL 和参数都打印出来，便于调试。 [edu.51cto](https://edu.51cto.com/article/note/15309.html)

这一块建议你自己做一个“小用户管理”模块：手写完整的 `UserMapper` 增删改查接口，用 JUnit 或 main 方法做调用测试。 [cnblogs](https://www.cnblogs.com/itcq1024/p/18577106)

***

## 第 3 步：动态 SQL、分页和关联映射

这是项目里最常见、也是“写得好不好”差距最大的部分。 [cloud.tencent](https://cloud.tencent.com/developer/article/2037944)

1. 动态 SQL 标签  
   - `<if>`：根据参数是否为空拼条件；  
   - `<where>`：自动处理前导 AND/WHERE；  
   - `<set>`：更新时自动处理逗号；  
   - `<foreach>`：批量操作（`IN` 查询、批量插入）。 [cloud.tencent](https://cloud.tencent.com/developer/article/2037944)

   示例：多条件组合查询用户列表，练习不同字段可选的情况。

2. 关联映射（一对一 / 一对多）  
   - 使用 `resultMap` 配置嵌套对象；  
   - 一对一：比如 User 中包含 Dept 信息；  
   - 一对多：用户与订单列表，用 `<collection>` 配置。 [c.biancheng](https://c.biancheng.net/mybatis/)

3. 分页  
   - 直接写 `limit` 语句，配合前端传分页参数；  
   - 或者使用分页插件（PageHelper / MyBatis-Plus），常见项目会这么做。 [developer.aliyun](https://developer.aliyun.com/article/1000910)

建议：写一个“订单列表查询”场景，包含条件过滤 + 分页 + 一对多关系，自己实现一次。

***

## 第 4 步：与 Spring / Spring Boot 集成（推荐你直接这样用）

实际工作基本都是 Spring Boot + MyBatis 或 MyBatis-Plus 组合。 [blog.csdn](https://blog.csdn.net/qq_44161833/article/details/150352216)

1. Spring Boot 方式快速集成  
   - 新建 Spring Boot 项目时选择 `MyBatis` 或 `MyBatis-Plus` 依赖即可；  
   - 在 `application.yml` 中配置数据源和 MyBatis 的 mapper 扫描路径；  
   - 用 `@Mapper` 注解标记 Mapper 接口，注入到 Service 使用。 [liaoxuefeng](https://liaoxuefeng.com/books/java/spring/database/mybatis/index.html)

2. 常见实践  
   - 统一放置 `mapper` 包和 `mapper.xml`，避免路径混乱；  
   - 事务由 Spring 管理，在 Service 层使用 `@Transactional`；  
   - 和 Spring MVC Controller 配合，构成完整的 REST 接口。 [blog.csdn](https://blog.csdn.net/qq_44161833/article/details/150352216)

你已经在用 Spring，建议优先把“MyBatis 在 Spring Boot 里怎么写一条 SQL”的闭环走一遍，而不是从裸 MyBatis 开始纠结各种配置细节。 [liaoxuefeng](https://liaoxuefeng.com/books/java/spring/database/mybatis/index.html)

***

## 第 5 步：进阶理解和优化（1～2 周内逐步补）

当你能写大部分 SQL 后，可以开始补一些“底层和最佳实践”。 [dunwu.github](https://dunwu.github.io/java-tutorial/pages/d4e6ee/)

- MyBatis 核心概念  
  - `SqlSessionFactory`、`SqlSession` 生命周期；  
  - Executor 执行器、一级/二级缓存的大致机制。 [cloud.tencent](https://cloud.tencent.com/developer/article/2037944)
- 性能与规范  
  - 避免 N+1 查询、合理使用关联 vs 分两次查；  
  - 动态 SQL 避免拼接错误导致错误索引走向；  
  - 善用 `resultMap` 和列别名，降低维护成本。 [c.biancheng](https://c.biancheng.net/mybatis/)
- 适用场景  
  - 什么时候适合 MyBatis（SQL 可控、复杂查询多）；  
  - 什么时候用 JPA 或其他 ORM；  
  - 团队内 SQL 管理和审查习惯。 [c.biancheng](https://c.biancheng.net/mybatis/)

可以找一篇“从零入门 + 核心概念讲解 + 适用场景总结”的文章细看一遍，把你平时遇到的问题对上号。 [edu.51cto](https://edu.51cto.com/article/note/15309.html)

***