# Spring Data 核心知识点
---

# 一、Spring Data 是什么？
**统一的数据访问标准 + 简化 CRUD**
- 它不是 ORM 框架，而是**对 JPA、MyBatis、Redis、MongoDB 等的统一封装**
- 目标：**不用写 SQL，不用写重复代码**
- 支持几乎所有数据库：MySQL、Redis、Mongo、ES、Neo4j...

你最常用的就是：
**Spring Data JPA（操作关系型数据库）**

---

# 二、核心思想（最重要）
1. **约定大于配置**
2. **Repository 模式**
3. **方法名自动生成查询**
4. **统一分页、排序、CRUD 接口**

---

# 三、四大核心接口（必须背）
所有 DAO 都继承这几个接口，**自带 CRUD**

1. **Repository**
   顶层接口，空的，只做标识

2. **CrudRepository**
   提供基础增删改查
   - save
   - findById
   - findAll
   - deleteById

3. **PagingAndSortingRepository**
   继承 CrudRepository，**增加分页、排序**

4. **JpaRepository**（最常用）
   继承上面所有，增强：
   - 批量操作
   - 刷新缓存
   - 更多查询

---

# 四、最牛功能：方法名自动生成查询（不用写 SQL）
直接按规则写方法名，Spring 自动实现 SQL

### 示例：
```java
public interface UserRepository extends JpaRepository<User, Long> {

    // 根据用户名查询
    User findByUsername(String username);

    // 根据用户名和密码查询
    User findByUsernameAndPassword(String username, String password);

    // 模糊查询
    List<User> findByUsernameLike(String username);

    // 排序
    List<User> findByAgeGreaterThanOrderByCreateTimeDesc(Integer age);
}
```

### 常用关键词（背会就能写 90% 查询）
- `And`
- `Or`
- `Between`
- `LessThan` / `GreaterThan`
- `Like`
- `IsNull`
- `NotNull`
- `OrderBy`
- `Asc/Desc`

---

# 五、自定义 SQL（复杂查询用）
使用 `@Query` 注解

### JPQL（面向对象）
```java
@Query("select u from User u where u.username = ?1")
User findByUsername(String username);
```

### 原生 SQL
```java
@Query(value = "select * from user where username = ?1", nativeQuery = true)
User findByUsername(String username);
```

### 修改/删除必须加：
```java
@Modifying
@Transactional
@Query("update User set username=?1 where id=?2")
int updateUsername(String username, Long id);
```

---

# 六、分页 & 排序（超级简单）
```java
// 分页
Page<User> page = userRepository.findAll(PageRequest.of(0, 10));

// 排序
List<User> list = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createTime"));
```

返回对象：
- `Page`：包含总条数、总页数、当前页数据
- `Slice`：轻量分页，不查总数

---

# 七、JPA 关系映射（高频）
```java
@Entity // 实体类
@Table(name = "user") // 表名
public class User {

    @Id // 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自增
    private Long id;

    private String username;

    // 一对一
    @OneToOne

    // 一对多
    @OneToMany

    // 多对一
    @ManyToOne

    // 多对多
    @ManyToMany
}
```

---

# 八、Spring Data 通用规范（所有数据库都一样）
不管是 MySQL、Mongo、Redis、ES，用法几乎一样：
1. 写实体
2. 继承 Repository
3. 写方法名 / 写注解
4. 直接调用

**一套语法，操作所有数据库**，这就是 Spring Data 的强大之处。

---

# 九、Spring Data JPA 执行流程（面试）
1. 项目启动 → 扫描 Repository 接口
2. 动态代理生成实现类
3. 根据方法名生成 SQL
4. 执行 JDBC / Hibernate
5. 返回结果

---

# 十、最精简总结
1. **Spring Data = 统一数据访问 + 简化CRUD**
2. **核心接口：Repository → CrudRepository → PagingAndSortingRepository → JpaRepository**
3. **方法名自动生成 SQL**，不用写 XML
4. **@Query 自定义 SQL**
5. **分页、排序、批量操作一行搞定**
6. **实体 + 注解映射表结构**

---

