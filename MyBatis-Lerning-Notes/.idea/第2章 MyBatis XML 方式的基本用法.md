# 第2章 MyBatis XML 方式的基本用法
## 2.1 本章学习目标
1. 掌握 XML 映射文件实现 **CRUD 完整操作**
2. 熟练使用 `#{}`、`${}` 参数取值
3. 理解返回值类型、事务提交规则
4. 掌握实体类与数据库字段映射处理

> 环境承接第一章：Maven 依赖、`mybatis-config.xml`、工具类、User 实体类、测试环境统一复用

---

## 2.2 前置基础
### 2.2.1 目录规范
```
src
├── main
│   ├── java
│   │   └── com
│   │       ├── pojo     # 实体类
│   │       ├── mapper   # Mapper接口
│   │       └── util     # MyBatis工具类
│   └── resources
│       ├── mybatis-config.xml  # 全局核心配置
│       └── mapper              # 存放XML映射文件
```

### 2.2.2 核心规则
1. Mapper 接口全类名 = XML 文件 `namespace`
2. 接口方法名 = XML 标签 `id`
3. 方法返回值 = XML `resultType`
4. 方法参数 = SQL 中 `#{参数名}` 接收

---

## 2.3 新增操作（Insert）
### 2.3.1 Mapper 接口方法
```java
public interface UserMapper {
    // 新增用户
    int addUser(User user);
}
```

### 2.3.2 UserMapper.xml 编写
```xml
<insert id="addUser">
    INSERT INTO user(username, password, age)
    VALUES(#{username}, #{password}, #{age})
</insert>
```

### 2.3.3 测试代码
```java
@Test
public void testAdd(){
    SqlSession session = MyBatisUtil.getSqlSession(false);
    UserMapper mapper = session.getMapper(UserMapper.class);
    
    User user = new User(null,"张三","123456",20);
    int rows = mapper.addUser(user);
    System.out.println("受影响行数：" + rows);
    
    // 手动提交事务
    session.commit();
    session.close();
}
```
> 关键：**增/删/改 需要手动提交事务**
> `openSession()` 不传参 = 关闭自动提交

---

## 2.4 删除操作（Delete）
### 2.4.1 接口方法
```java
int deleteUserById(Integer id);
```

### 2.4.2 XML 映射
```xml
<delete id="deleteUserById">
    DELETE FROM user WHERE id = #{id}
</delete>
```

### 2.4.3 测试
```java
@Test
public void testDelete(){
    SqlSession session = MyBatisUtil.getSqlSession(false);
    UserMapper mapper = session.getMapper(UserMapper.class);
    int rows = mapper.deleteUserById(1);
    session.commit();
    session.close();
}
```

---

## 2.5 修改操作（Update）
### 2.5.1 接口方法
```java
int updateUser(User user);
```

### 2.5.2 XML 映射
```xml
<update id="updateUser">
    UPDATE user
    SET username=#{username},password=#{password},age=#{age}
    WHERE id=#{id}
</update>
```

---

## 2.6 查询操作（Select）
### 2.6.1 根据ID查单个对象
接口：
```java
User findUserById(Integer id);
```

XML：
```xml
<select id="findUserById" resultType="com.pojo.User">
    SELECT id,username,password,age FROM user WHERE id = #{id}
</select>
```

### 2.6.2 查询全部（集合返回）
接口：
```java
List<User> findAllUser();
```

XML：
```xml
<select id="findAllUser" resultType="com.pojo.User">
    SELECT * FROM user
</select>
```
> 注意：返回集合时，`resultType` 写**泛型实体类**，不是 List

---

## 2.7 两种参数取值方式
### 2.7.1 `#{}` 预编译参数（推荐）
- 底层使用 `PreparedStatement`
- 防止 **SQL 注入**
- 自动拼接引号，安全高效
- 日常开发统一使用

### 2.7.2 `${}` 字符串直接拼接
- 底层直接拼接 SQL 语句
- 存在 SQL 注入风险
- 适用于：动态表名、排序字段等特殊场景

示例（了解即可，不推荐常规使用）：
```xml
<select id="findByTableName" resultType="com.pojo.User">
    SELECT * FROM ${tableName}
</select>
```

---

## 2.8 多参数传递处理
### 方式1：使用 @Param 注解（推荐）
接口：
```java
List<User> findByAgeAndName(@Param("age") Integer age, 
                            @Param("name") String name);
```

XML 直接通过注解名称取值：
```xml
<select id="findByAgeAndName" resultType="com.pojo.User">
    SELECT * FROM user WHERE age = #{age} AND username = #{name}
</select>
```

### 方式2：封装为实体类/Map
参数过多时，优先封装对象或 Map，可读性更强。

---

## 2.9 字段名与属性名不一致解决方案
场景：数据库字段 `user_name`，实体类属性 `userName`

### 方案1：SQL 起别名
```xml
<select id="findAll" resultType="com.pojo.User">
    SELECT id,user_name AS userName,age FROM user
</select>
```

### 方案2：全局配置驼峰命名
在 `mybatis-config.xml` 开启自动转换：
```xml
<settings>
    <setting name="mapUnderscoreToCamelCase" value="true"/>
</settings>
```

### 方案3：使用 ResultMap 自定义映射
```xml
<resultMap id="userMap" type="com.pojo.User">
    <id column="id" property="id"/>
    <result column="user_name" property="userName"/>
</resultMap>

<select id="findAll" resultMap="userMap">
    SELECT * FROM user
</select>
```

---

## 2.10 事务小结
1. `openSession(true)`：自动提交，**查询常用**
2. `openSession()` / `openSession(false)`：手动提交，**增删改必须 commit()**
3. 异常场景：需要 `rollback()` 回滚事务，保证数据一致性

---

## 2.11 本章核心总结
1. XML 四大标签：`<select>`、`<insert>`、`<update>`、`<delete>`
2. CRUD 核心绑定规则：namespace、id、resultType 一一对应
3. 参数优先使用 `#{}`，杜绝 SQL 注入
4. 多参数使用 `@Param` 注解绑定
5. 下划线字段与驼峰属性可通过全局配置快速适配
6. DML 操作必须手动提交事务

---

