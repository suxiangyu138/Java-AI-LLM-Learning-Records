# MyBatis XML 方式的基本用法

> **文档定位**：Java 后端技术参考文档 | MyBatis XML 方式基本用法  
> **核心优势**：SQL 与 Java 代码分离，便于统一管理、优化和维护  
> **核心组成**：`mybatis-config.xml`（全局配置）+ Mapper XML（SQL 映射）

---

## 目录

- [一、前期准备](#一前期准备)
- [二、核心 XML 配置（mybatis-config.xml）](#二核心-xml-配置mybatis-configxml)
- [三、Mapper 映射 XML](#三mapper-映射-xml)
- [四、实操演示](#四实操演示)
- [五、注意事项](#五注意事项)
- [六、总结](#六总结)

---

## 一、前期准备

### 核心依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.10</version>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.30</version>
    </dependency>
</dependencies>
```

### 数据库与实体类

```sql
CREATE DATABASE IF NOT EXISTS mybatis_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mybatis_db;

CREATE TABLE IF NOT EXISTS user (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL,
    age      INT,
    email    VARCHAR(100)
);
```

---

## 二、核心 XML 配置（mybatis-config.xml）

> **标签顺序不可乱**：properties → settings → typeAliases → environments → mappers

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
    PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
    "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!-- 1. 加载外部数据库配置 -->
    <properties resource="db.properties"></properties>

    <!-- 2. 全局参数 -->
    <settings>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <setting name="logImpl" value="SLF4J"/>
    </settings>

    <!-- 3. 别名 -->
    <typeAliases>
        <package name="com.example.pojo"/>
    </typeAliases>

    <!-- 4. 数据库环境 -->
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="${jdbc.driver}"/>
                <property name="url" value="${jdbc.url}"/>
                <property name="username" value="${jdbc.username}"/>
                <property name="password" value="${jdbc.password}"/>
            </dataSource>
        </environment>
    </environments>

    <!-- 5. Mapper 映射 -->
    <mappers>
        <package name="com.example.mapper"/>
    </mappers>
</configuration>
```

### db.properties

```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/mybatis_db?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8
jdbc.username=root
jdbc.password=123456
```

---

## 三、Mapper 映射 XML

### 核心结构

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">
    <!-- SQL 标签 -->
</mapper>
```

> `namespace` 必须与 Mapper 接口全类名一致；SQL 标签 `id` 必须与接口方法名一致。

### CRUD 操作

```xml
<!-- 查询 -->
<select id="selectUserById" resultType="user">
    SELECT * FROM user WHERE id = #{id}
</select>
<select id="selectAllUser" resultType="user">
    SELECT * FROM user
</select>

<!-- 新增 -->
<insert id="insertUser" parameterType="user">
    INSERT INTO user (username, password, age, email)
    VALUES (#{username}, #{password}, #{age}, #{email})
</insert>

<!-- 修改 -->
<update id="updateUser" parameterType="user">
    UPDATE user
    SET username = #{username}, password = #{password}, age = #{age}, email = #{email}
    WHERE id = #{id}
</update>

<!-- 删除 -->
<delete id="deleteUserById" parameterType="java.lang.Integer">
    DELETE FROM user WHERE id = #{id}
</delete>
```

### Mapper 接口

```java
public interface UserMapper {
    User selectUserById(@Param("id") Integer id);
    List<User> selectAllUser();
    int insertUser(User user);
    int updateUser(User user);
    int deleteUserById(@Param("id") Integer id);
}
```

---

## 四、实操演示

### 工具类

```java
public class MyBatisUtils {
    private static SqlSessionFactory sqlSessionFactory;

    static {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    public static SqlSession getSqlSession() {
        return sqlSessionFactory.openSession(true);
    }
}
```

### 测试示例

```java
@Test
public void testSelectUserById() {
    try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        User user = userMapper.selectUserById(1);
        System.out.println(user);
    }
}
```

---

## 五、注意事项

| 关键点 | 说明 |
|--------|------|
| **配置文件标签顺序** | properties → settings → typeAliases → environments → mappers |
| **namespace 匹配** | 必须与 Mapper 接口全类名一致 |
| **id 匹配** | SQL 标签 id 必须与接口方法名一致 |
| **路径规范** | Mapper XML 路径需与接口包结构一致 |
| **`#{}` vs `${}`** | `#{}` 预编译防注入（推荐）；`${}` 拼接，用于表名/列名 |
| **SqlSession 线程安全** | 线程不安全，每次操作获取新的，用完关闭 |
| **特殊字符** | 使用 `&lt;` `&gt;` `&amp;` 或 `<![CDATA[ ]]>` 包裹 |

---

## 六、总结

```
全局配置 mybatis-config.xml → 配置数据源 → Mapper 接口 + XML → SqlSession 执行 SQL
```

XML 方式的核心价值是 **SQL 与 Java 代码分离**，兼顾灵活性和可维护性，适合复杂 SQL 场景。
