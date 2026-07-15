# MyBatis 高级查询

> **定位**：覆盖 MyBatis 高级结果映射（一对一/一对多/鉴别器）、存储过程调用和自定义类型处理器，解决复杂业务场景下的查询需求。

---

## 目录

1. [高级结果映射概述](#1-高级结果映射概述)
2. [一对一映射（association）](#2-一对一映射association)
3. [一对多映射（collection）](#3-一对多映射collection)
4. [鉴别器映射（discriminator）](#4-鉴别器映射discriminator)
5. [存储过程调用](#5-存储过程调用)
6. [类型处理器（TypeHandler）](#6-类型处理器typehandler)

---

## 1. 高级结果映射概述

| 映射类型 | 标签（XML） | 注解 | 场景 |
|----------|-----------|------|------|
| **一对一** | `<association>` | `@One` | 用户 ↔ 身份证 |
| **一对多** | `<collection>` | `@Many` | 用户 → 多个订单 |
| **鉴别器** | `<discriminator>` + `<case>` | `@Discriminator` + `@Case` | 同表数据，按字段值映射不同实体 |

---

## 2. 一对一映射（association）

### 2.1 环境准备

```sql
-- 身份证表，与 user 一对一（user_id 外键）
CREATE TABLE id_card (
    id INT PRIMARY KEY AUTO_INCREMENT,
    card_no VARCHAR(20) NOT NULL UNIQUE,
    address VARCHAR(100),
    user_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

```java
// User 实体中关联 IDCard
public class User {
    private Integer id;
    private String username;
    private IDCard idCard;  // 一对一关联
}
```

### 2.2 XML 方式

**方式 1：嵌套结果（一次查询，推荐）**

```xml
<resultMap id="userWithIDCardMap" type="user">
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    <association property="idCard" javaType="com.example.pojo.IDCard">
        <id column="card_id" property="id"/>
        <result column="card_no" property="cardNo"/>
        <result column="address" property="address"/>
    </association>
</resultMap>

<select id="selectUserWithIDCard" resultMap="userWithIDCardMap">
    SELECT u.*, ic.id AS card_id, ic.card_no, ic.address
    FROM user u LEFT JOIN id_card ic ON u.id = ic.user_id
    WHERE u.id = #{id}
</select>
```

**方式 2：嵌套查询（延迟加载）**

```xml
<resultMap id="userWithIDCardLazyMap" type="user">
    <id column="id" property="id"/>
    <association property="idCard" javaType="com.example.pojo.IDCard"
        select="com.example.mapper.IDCardMapper.selectIDCardByUserId"
        column="id"/>
</resultMap>

<select id="selectUserWithIDCardLazy" resultMap="userWithIDCardLazyMap">
    SELECT * FROM user WHERE id = #{id}
</select>
```

### 2.3 注解方式

```java
// 嵌套结果
@Select("SELECT u.*, ic.id AS card_id, ic.card_no, ic.address " +
        "FROM user u LEFT JOIN id_card ic ON u.id = ic.user_id WHERE u.id = #{id}")
@Results({
    @Result(column = "id", property = "id"),
    @Result(column = "card_no", property = "idCard.cardNo"),
    @Result(column = "address", property = "idCard.address")
})
User selectUserWithIDCard(@Param("id") Integer id);

// 嵌套查询（延迟加载）
@Select("SELECT * FROM user WHERE id = #{id}")
@Results({
    @Result(column = "id", property = "idCard",
        one = @One(select = "com.example.mapper.IDCardMapper.selectIDCardByUserId",
                   fetchType = FetchType.LAZY))
})
User selectUserWithIDCardLazy(@Param("id") Integer id);
```

---

## 3. 一对多映射（collection）

### 3.1 环境准备

```sql
CREATE TABLE `order` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(20) NOT NULL UNIQUE,
    total_price DECIMAL(10,2) NOT NULL,
    user_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

```java
public class User {
    private List<Order> orders;  // 一对多关联
}
```

### 3.2 XML 方式

```xml
<resultMap id="userWithOrdersMap" type="user">
    <id column="id" property="id"/>
    <collection property="orders" ofType="com.example.pojo.Order">
        <id column="order_id" property="id"/>
        <result column="order_no" property="orderNo"/>
        <result column="total_price" property="totalPrice"/>
    </collection>
</resultMap>

<select id="selectUserWithOrders" resultMap="userWithOrdersMap">
    SELECT u.*, o.id AS order_id, o.order_no, o.total_price
    FROM user u LEFT JOIN `order` o ON u.id = o.user_id
    WHERE u.id = #{id}
</select>
```

### 3.3 association vs collection

| 维度 | `<association>` | `<collection>` |
|------|:---------------:|:--------------:|
| 映射关系 | 一对一 | 一对多 |
| Java 类型 | 单个实体（`javaType`） | 集合（`ofType` 指定元素类型） |
| 注解 | `@One` | `@Many` |

---

## 4. 鉴别器映射（discriminator）

> 根据字段值动态选择不同映射规则，类似 `switch-case`。

### 场景

`user` 表新增 `type` 字段：`1` = 普通用户（关联身份证），`2` = 管理员（关联角色）。

```xml
<resultMap id="userWithDiscriminatorMap" type="user">
    <id column="id" property="id"/>
    <discriminator column="type" javaType="java.lang.Integer">
        <case value="1">  <!-- 普通用户 → 身份证 -->
            <association property="idCard" javaType="com.example.pojo.IDCard">
                <id column="card_id" property="id"/>
                <result column="card_no" property="cardNo"/>
            </association>
        </case>
        <case value="2">  <!-- 管理员 → 角色 -->
            <association property="role" javaType="com.example.pojo.Role">
                <id column="role_id" property="id"/>
                <result column="role_name" property="roleName"/>
            </association>
        </case>
    </discriminator>
</resultMap>
```

---

## 5. 存储过程调用

### 5.1 MySQL 存储过程

```sql
DELIMITER //
CREATE PROCEDURE select_user_by_id(
    IN p_user_id INT,
    OUT p_username VARCHAR(50),
    OUT p_age INT
)
BEGIN
    SELECT username, age INTO p_username, p_age FROM user WHERE id = p_user_id;
END //
DELIMITER ;
```

### 5.2 MyBatis 调用

```xml
<select id="callSelectUserById" statementType="CALLABLE">
    {CALL select_user_by_id(
        #{userId, mode=IN, jdbcType=INTEGER},
        #{username, mode=OUT, jdbcType=VARCHAR},
        #{age, mode=OUT, jdbcType=INTEGER}
    )}
</select>
```

```java
// 输出参数通过 Map 接收
void callSelectUserById(Map<String, Object> paramMap);
```

| 关键点 | 说明 |
|--------|------|
| `statementType="CALLABLE"` | 标识为存储过程调用 |
| `mode=IN` | 输入参数 |
| `mode=OUT` | 输出参数（需用 Map/数组接收） |

---

## 6. 类型处理器（TypeHandler）

> 解决数据库字段类型与 Java 属性类型不匹配。

### 6.1 内置处理器

| Java 类型 | 数据库类型 | 处理器 |
|-----------|-----------|--------|
| `String` | VARCHAR/CHAR | `StringTypeHandler` |
| `Integer` | INT | `IntegerTypeHandler` |
| `Date` | DATETIME | `DateTypeHandler` |
| `Boolean` | TINYINT | `BooleanTypeHandler` |

### 6.2 自定义处理器（VARCHAR ↔ 枚举）

```java
// 枚举定义
public enum GenderEnum {
    MALE("男"), FEMALE("女");
    private String value;
    GenderEnum(String value) { this.value = value; }
    public String getValue() { return value; }
    public static GenderEnum getByValue(String value) {
        for (GenderEnum g : values()) {
            if (g.value.equals(value)) return g;
        }
        return null;
    }
}

// 自定义 TypeHandler
public class GenderTypeHandler extends BaseTypeHandler<GenderEnum> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
            GenderEnum parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getValue());  // 枚举 → VARCHAR
    }

    @Override
    public GenderEnum getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        return GenderEnum.getByValue(rs.getString(columnName));  // VARCHAR → 枚举
    }
    // ... 其他 getNullableResult 重载
}
```

```xml
<!-- mybatis-config.xml 注册 -->
<typeHandlers>
    <typeHandler handler="com.example.typehandler.GenderTypeHandler"
        javaType="com.example.enums.GenderEnum" jdbcType="VARCHAR"/>
</typeHandlers>
```

---

> 🎯 **核心总结**：一对一用 `<association>` + `@One`，一对多用 `<collection>` + `@Many`，鉴别器用 `<discriminator>`。存储过程用 `CALLABLE`，类型不匹配用自定义 `TypeHandler`。
