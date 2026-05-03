# 第3章 MyBatis 注解方式的基本用法
这一章专门带你学习 **MyBatis 纯注解开发**（不用写 XML 映射文件），代码更简洁、开发更快，也是现在企业最常用的方式！

---

## 3.1 本章学习目标
1. 掌握 MyBatis **增删改查 CRUD 注解**
2. 学会注解方式处理 **多参数、字段映射**
3. 完全抛弃 Mapper XML 文件
4. 学会注解 + 配置文件搭配使用

> 环境：继续沿用第1、2章的项目结构（实体类、工具类、数据库表不变）

---

## 3.2 什么是 MyBatis 注解开发？
- **不用写 UserMapper.xml**
- 直接在 **Mapper 接口方法上写注解 + SQL**
- 简洁、轻量、适合简单 SQL
- 复杂 SQL（多表、动态 SQL）还是推荐 XML

### 核心注解（必须背）
| 注解 | 作用 |
|------|------|
| `@Select` | 查询 |
| `@Insert` | 新增 |
| `@Update` | 修改 |
| `@Delete` | 删除 |
| `@Results` | 字段映射（字段名不一致时用） |
| `@Result` | 单个字段映射 |
| `@Param` | 多参数命名 |

---

## 3.3 第一步：修改核心配置文件
注解开发必须在 `mybatis-config.xml` 中**注册 Mapper 接口**，而不是注册 XML：

```xml
<mappers>
    <!-- 注解开发：注册接口类 -->
    <mapper class="com.mapper.UserMapper"/>
</mappers>
```

---

## 3.4 注解 CRUD 实战（完整代码）
直接在 **UserMapper 接口** 上写注解，**删除所有 XML 文件**！

### 3.4.1 查询单个（@Select）
```java
public interface UserMapper {

    // 根据ID查询
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Integer id);
}
```

---

### 3.4.2 查询全部（@Select）
```java
// 查询所有用户
@Select("SELECT * FROM user")
List<User> findAll();
```

---

### 3.4.3 新增（@Insert）
```java
// 新增用户
@Insert("INSERT INTO user(username,password,age) " +
        "VALUES(#{username},#{password},#{age})")
int addUser(User user);
```

---

### 3.4.4 修改（@Update）
```java
// 修改用户
@Update("UPDATE user SET username=#{username},password=#{password},age=#{age} WHERE id=#{id}")
int updateUser(User user);
```

---

### 3.4.5 删除（@Delete）
```java
// 根据ID删除
@Delete("DELETE FROM user WHERE id = #{id}")
int deleteById(Integer id);
```

---

## 3.5 测试类（和之前完全一样）
```java
@Test
public void testFindById(){
    try(SqlSession session = MyBatisUtil.getSqlSession(true)){
        UserMapper mapper = session.getMapper(UserMapper.class);
        User user = mapper.findById(1);
        System.out.println(user);
    }
}
```

> 注意：**增删改必须提交事务**！
> `getSqlSession(true)` = 自动提交

---

## 3.6 多参数处理（@Param）
和 XML 用法一样，**多参数必须加 @Param 注解**

接口：
```java
@Select("SELECT * FROM user WHERE username = #{name} AND age > #{age}")
List<User> findByCondition(
    @Param("name") String username,
    @Param("age") Integer age
);
```

---

## 3.7 字段名不一致映射（@Results）
场景：
- 数据库字段：`user_name`
- 实体类属性：`userName`

### 注解映射写法：
```java
@Select("SELECT * FROM user WHERE id = #{id}")
@Results({
    @Result(column = "user_name", property = "userName"),
    @Result(column = "user_age",  property = "age")
})
User findById(Integer id);
```

- `column`：数据库列名
- `property`：实体类属性名

---

## 3.8 注解开发 vs XML 开发
| 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **注解** | 代码少、无XML、快 | 复杂SQL难写、不灵活 | 简单单表CRUD |
| **XML** | 支持动态SQL、复杂查询 | 配置文件多 | 企业复杂项目 |

### 企业最佳实践
- **简单单表操作：用注解**
- **复杂多表/动态SQL：用XML**

---

## 3.9 本章核心总结（必背）
1. 注解开发 **完全抛弃 XML 映射文件**
2. 四大核心注解：`@Select` `@Insert` `@Update` `@Delete`
3. 多参数必须加 `@Param`
4. 字段名不一致用 `@Results + @Result` 映射
5. 配置文件注册用 `<mapper class="接口全类名">`
6. 增删改必须 **提交事务**

---


