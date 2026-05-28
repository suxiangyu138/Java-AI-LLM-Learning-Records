MyBatis 高级查询
本章介绍了 MyBatis 中的高级结果映射，包括一对一映射、一对多映射和鉴别器映射。通过循序渐进的代码示例让读者轻松地学会使用 MyBatis 中最高级的结果映射。本章还通过全面的示例讲解了存储过程的用法和类型处理器的用法，帮助读者应对复杂业务场景下的查询需求，进一步提升 MyBatis 的使用能力。
11.1 高级结果映射概述
在基础查询中，我们通常将数据库表的字段直接映射到实体类的属性，这种映射方式适用于单表查询、实体类与表结构完全对应的场景。但在实际开发中，业务逻辑往往更复杂：例如，一个用户对应一个身份证信息（一对一）、一个用户对应多个订单（一对多）、不同类型的用户对应不同的信息（鉴别器映射）。
MyBatis 的高级结果映射（ResultMap 高级用法）正是为了解决这类实体类与表结构不直接对应、多表关联查询的场景，通过手动配置 ResultMap，明确数据库字段与实体类属性的映射关系，甚至支持嵌套映射、动态鉴别映射，让查询结果更精准地匹配业务实体。
高级结果映射的核心是 <resultMap> 标签，通过嵌套 <association>（一对一）、<collection>（一对多）、<discriminator>（鉴别器）等子标签，实现复杂的映射逻辑，无需手动拼接结果集，提升开发效率和代码可读性。
11.2 一对一映射（association）
一对一映射是指两个实体类之间存在一对一的关联关系，例如：用户（User）和身份证（IDCard），一个用户只能有一个身份证，一个身份证只属于一个用户。这种场景下，我们需要通过多表关联查询，将两个表的结果映射到一个包含关联实体的主实体中。
11.2.1 准备环境（表与实体类）
1. 数据库表设计
    沿用前文的 user 表，新增 id_card 表（与 user 表一对一关联，通过 user_id 外键关联）：
    -- 身份证表（与user表一对一）
    CREATE TABLE IF NOT EXISTS id_card (
    id INT PRIMARY KEY AUTO_INCREMENT,
    card_no VARCHAR(20) NOT NULL UNIQUE, -- 身份证号
    address VARCHAR(100), -- 户籍地址
    user_id INT NOT NULL, -- 外键，关联user表的id
    FOREIGN KEY (user_id) REFERENCES user(id) -- 外键约束
    );
    -- 插入测试数据
    INSERT INTO id_card (card_no, address, user_id) VALUES
    ('110101199001011234', '北京市东城区', 1),
    ('310101199202024567', '上海市黄浦区', 2);
2. 实体类设计
    创建 IDCard 实体类，在 User 实体类中添加 IDCard 类型的属性（体现一对一关联）：
    // 身份证实体类（com.example.pojo.IDCard）
    public class IDCard {
    private Integer id;
    private String cardNo; // 对应数据库card_no字段
    private String address;
    private Integer userId; // 外键，关联user表id
    // getter、setter、toString方法（省略）
    }
    // 用户实体类（com.example.pojo.User），新增idCard属性
    public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;
    private IDCard idCard; // 一对一关联：一个用户对应一个身份证
    // getter、setter、toString方法（省略）
    }
    11.2.2 一对一映射实现（XML方式）
    一对一映射通过 <resultMap> 中的 <association> 标签实现，<association> 用于映射关联的单个实体（一对一），核心属性如下：
    property：主实体中关联属性的名称（如 User 中的 idCard）；
    javaType：关联属性的实体类全类名（如 com.example.pojo.IDCard）；
    column：主表中用于关联的外键字段（如 user 表的 id，对应 id_card 表的 user_id）；
    select：可选，用于延迟加载（后续讲解），指定查询关联实体的SQL语句ID；
    resultMap：可选，引用已定义的 ResultMap，用于映射关联实体的字段。
    方式1：嵌套结果映射（推荐，一次查询完成）
    通过多表连接查询（LEFT JOIN），将 user 表和 id_card 表的结果一次性查询出来，再通过 <association> 嵌套映射关联实体，无需多次查询数据库。
    <!-- UserMapper.xml 中定义一对一映射的ResultMap -->
    <resultMap id="userWithIDCardMap" type="user"&gt;
    <!-- 映射User自身的字段 -->
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    <result column="password" property="password"/>
    <result column="age" property="age"/>
    <result column="email" property="email"/><!-- 一对一映射：关联IDCard实体 -->
    <association property="idCard" javaType="com.example.pojo.IDCard">
        <id column="card_id" property="id"/&gt; <!-- 别名card_id，避免与user表id冲突 -->
        <result column="card_no" property="cardNo"/>
        <result column="address" property="address"/>
        <result column="user_id" property="userId"/&gt;
    &lt;/association&gt;
    &lt;/resultMap&gt;
    <!-- 多表连接查询，查询用户及其关联的身份证信息 -->
    <select id="selectUserWithIDCard" resultMap="userWithIDCardMap">
    SELECT 
        u.*, 
        ic.id AS card_id, 
        ic.card_no, 
        ic.address, 
        ic.user_id 
    FROM user u
    LEFT JOIN id_card ic ON u.id = ic.user_id
    WHERE u.id = #{id}
    </select>
    方式2：嵌套查询（延迟加载，按需查询）
    先查询主实体（User），当需要使用关联实体（IDCard）时，再通过关联字段查询关联实体，适用于关联实体不常使用的场景，可减少不必要的查询开销。
    <!-- 1. 定义IDCard的ResultMap和查询SQL（IDCardMapper.xml） -->
    <resultMap id="idCardMap" type="com.example.pojo.IDCard">
    <id column="id" property="id"/>
    <result column="card_no" property="cardNo"/>
    <result column="address" property="address"/>
    <result column="user_id" property="userId"/>
    </resultMap>
    <select id="selectIDCardByUserId" resultMap="idCardMap">
    SELECT * FROM id_card WHERE user_id = #{userId}
    </select&gt;
    <!-- 2. UserMapper.xml中定义一对一映射，使用select属性实现延迟加载 -->
    <resultMap id="userWithIDCardLazyMap" type="user">
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    <!-- 嵌套查询：通过select属性指定查询IDCard的SQL，column传递参数（user表的id） -->
    <association 
        property="idCard" 
        javaType="com.example.pojo.IDCard"
        select="com.example.mapper.IDCardMapper.selectIDCardByUserId"
        column="id">
    </association>
    </resultMap>
    <select id="selectUserWithIDCardLazy" resultMap="userWithIDCardLazyMap">
    SELECT * FROM user WHERE id = #{id}
    </select>
    说明：延迟加载需在 mybatis-config.xml 中开启全局配置（可选）：
    <settings>
    <!-- 开启延迟加载 -->
    <setting name="lazyLoadingEnabled" value="true"/&gt;
    <!-- 关闭立即加载（按需加载） -->
    <setting name="aggressiveLazyLoading" value="false"/>
    </settings>
    11.2.3 一对一映射实现（注解方式）
    注解方式通过 @Result 和 @One 注解实现一对一映射，@One 对应 XML 中的 <association> 标签，用于关联单个实体。
    // UserMapper接口（注解方式）
    public interface UserMapper {
    // 嵌套结果映射（一次查询）
    @Select("SELECT u.*, ic.id AS card_id, ic.card_no, ic.address, ic.user_id " +
            "FROM user u LEFT JOIN id_card ic ON u.id = ic.user_id WHERE u.id = #{id}")
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "username", property = "username"),
        @Result(column = "password", property = "password"),
        // 一对一映射：关联IDCard实体
        @Result(column = "card_id", property = "idCard.id"),
        @Result(column = "card_no", property = "idCard.cardNo"),
        @Result(column = "address", property = "idCard.address"),
        @Result(column = "user_id", property = "idCard.userId")
    })
    User selectUserWithIDCard(@Param("id") Integer id);
    // 嵌套查询（延迟加载）
    @Select("SELECT * FROM user WHERE id = #{id}")
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "username", property = "username"),
        // @One对应<association>，select指定查询关联实体的方法，fetchType指定延迟加载
        @Result(column = "id", property = "idCard", 
                one = @One(select = "com.example.mapper.IDCardMapper.selectIDCardByUserId", 
                          fetchType = FetchType.LAZY))
    })
    User selectUserWithIDCardLazy(@Param("id") Integer id);
    }
    // IDCardMapper接口
    public interface IDCardMapper {
    @Select("SELECT * FROM id_card WHERE user_id = #{userId}")
    IDCard selectIDCardByUserId(@Param("userId") Integer userId);
    }
    11.3 一对多映射（collection）
    一对多映射是指两个实体类之间存在一对多的关联关系，例如：用户（User）和订单（Order），一个用户可以有多个订单，一个订单只属于一个用户。这种场景下，需要将主实体（User）和关联的多个子实体（Order）一起查询，并映射到主实体的集合属性中。
    MyBatis 中通过 <resultMap> 中的 <collection> 标签实现一对多映射，<collection> 用于映射关联的多个实体（集合），核心属性与 <association> 类似，区别在于：javaType 通常为 List 等集合类型，ofType 指定集合中元素的实体类类型。
    11.3.1 准备环境（表与实体类）
    1. 数据库表设计
    新增 order 表（与 user 表一对多关联，通过 user_id 外键关联）：
    -- 订单表（与user表一对多）
    CREATE TABLE IF NOT EXISTS `order` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(20) NOT NULL UNIQUE, -- 订单号
    total_price DECIMAL(10,2) NOT NULL, -- 订单总价
    create_time DATETIME NOT NULL, -- 创建时间
    user_id INT NOT NULL, -- 外键，关联user表的id
    FOREIGN KEY (user_id) REFERENCES user(id)
    );
    -- 插入测试数据
    INSERT INTO `order` (order_no, total_price, create_time, user_id) VALUES
    ('ORDER2024001', 199.99, '2024-01-01 10:00:00', 1),
    ('ORDER2024002', 299.99, '2024-01-02 14:30:00', 1),
    ('ORDER2024003', 99.99, '2024-01-03 09:15:00', 2);
    注意：order 是 MySQL 关键字，表名需用反引号（`）包裹。
2. 实体类设计
    创建 Order 实体类，在 User 实体类中添加 List<Order> 类型的属性（体现一对多关联）：
    // 订单实体类（com.example.pojo.Order）
    public class Order {
    private Integer id;
    private String orderNo; // 对应order_no字段
    private BigDecimal totalPrice; // 对应total_price字段
    private Date createTime; // 对应create_time字段
    private Integer userId; // 外键，关联user表id
    // getter、setter、toString方法（省略）
    }
    // 用户实体类（com.example.pojo.User），新增orders属性
    public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;
    private IDCard idCard; // 一对一关联
    private List<Order> orders; // 一对多关联：一个用户对应多个订单
    // getter、setter、toString方法（省略）
    }
    11.3.2 一对多映射实现（XML方式）
    方式1：嵌套结果映射（一次查询完成）
    通过多表连接查询（LEFT JOIN），将 user 表和 order 表的结果一次性查询出来，通过 <collection> 标签将多个订单映射到 User 的 orders 集合中。
    <!-- UserMapper.xml 中定义一对多映射的ResultMap -->
    <resultMap id="userWithOrdersMap" type="user"&gt;
    <!-- 映射User自身的字段 -->
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    <result column="password" property="password"/>
    <result column="age" property="age"/>
    &lt;result column="email" property="email"/&gt;
    <!-- 一对多映射：关联Order集合 -->
    <collection 
        property="orders" 
        ofType="com.example.pojo.Order"&gt; <!-- ofType：集合中元素的类型 -->
        &lt;id column="order_id" property="id"/&gt; <!-- 别名order_id，避免冲突 -->
        <result column="order_no" property="orderNo"/>
        <result column="total_price" property="totalPrice"/>
        <result column="create_time" property="createTime"/>
        <result column="user_id" property="userId"/>
    </collection>
    &lt;/resultMap&gt;
    <!-- 多表连接查询，查询用户及其关联的所有订单 -->
    <select id="selectUserWithOrders" resultMap="userWithOrdersMap">
    SELECT 
        u.*, 
        o.id AS order_id, 
        o.order_no, 
        o.total_price, 
        o.create_time, 
        o.user_id 
    FROM user u
    LEFT JOIN `order` o ON u.id = o.user_id
    WHERE u.id = #{id}
    </select>
    方式2：嵌套查询（延迟加载）
    先查询主实体（User），当需要使用关联的订单集合时，再通过 user_id 查询所有关联的订单，适用于订单集合不常使用的场景。
    <!-- 1. 定义Order的ResultMap和查询SQL（OrderMapper.xml） -->
    <resultMap id="orderMap" type="com.example.pojo.Order">
    <id column="id" property="id"/>
    <result column="order_no" property="orderNo"/>
    <result column="total_price" property="totalPrice"/>
    <result column="create_time" property="createTime"/>
    <result column="user_id" property="userId"/>
    </resultMap>
    <select id="selectOrdersByUserId" resultMap="orderMap">
    SELECT * FROM `order` WHERE user_id = #{userId}
    &lt;/select&gt;
    <!-- 2. UserMapper.xml中定义一对多映射，使用select属性实现延迟加载 -->
    <resultMap id="userWithOrdersLazyMap" type="user">
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    <!-- 一对多映射：通过select属性指定查询订单的SQL，column传递参数（user表的id） -->
    <collection 
        property="orders" 
        ofType="com.example.pojo.Order"
        select="com.example.mapper.OrderMapper.selectOrdersByUserId"
        column="id">
    </collection>
    </resultMap>
    <select id="selectUserWithOrdersLazy" resultMap="userWithOrdersLazyMap">
    SELECT * FROM user WHERE id = #{id}
    </select>
    11.3.3 一对多映射实现（注解方式）
    注解方式通过 @Result 和 @Many 注解实现一对多映射，@Many 对应 XML 中的 <collection> 标签，用于关联多个实体（集合）。
    // UserMapper接口（注解方式）
    public interface UserMapper {
    // 嵌套结果映射（一次查询）
    @Select("SELECT u.*, o.id AS order_id, o.order_no, o.total_price, o.create_time, o.user_id " +
            "FROM user u LEFT JOIN `order` o ON u.id = o.user_id WHERE u.id = #{id}")
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "username", property = "username"),
        // 一对多映射：关联Order集合
        @Result(column = "order_id", property = "orders.id"),
        @Result(column = "order_no", property = "orders.orderNo"),
        @Result(column = "total_price", property = "orders.totalPrice"),
        @Result(column = "create_time", property = "orders.createTime"),
        @Result(column = "user_id", property = "orders.userId")
    })
    User selectUserWithOrders(@Param("id") Integer id);
    // 嵌套查询（延迟加载）
    @Select("SELECT * FROM user WHERE id = #{id}")
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "username", property = "username"),
        // @Many对应<collection>，select指定查询订单的方法，fetchType指定延迟加载
        @Result(column = "id", property = "orders", 
                many = @Many(select = "com.example.mapper.OrderMapper.selectOrdersByUserId", 
                           fetchType = FetchType.LAZY))
    })
    User selectUserWithOrdersLazy(@Param("id") Integer id);
    }
    // OrderMapper接口
    public interface OrderMapper {
    @Select("SELECT * FROM `order` WHERE user_id = #{userId}")
    List<Order> selectOrdersByUserId(@Param("userId") Integer userId);
    }
    11.4 鉴别器映射（discriminator）
    鉴别器映射（Discriminator）是 MyBatis 中最复杂的结果映射，核心作用是根据一个字段的值，动态选择不同的映射规则，类似 Java 中的 switch-case 语句。适用于：同一表中的数据，根据某个字段的不同值，对应不同的实体类（或不同的映射逻辑）。
    例如：用户表（user）中新增 type 字段（1-普通用户，2-管理员），普通用户关联身份证信息，管理员关联角色信息，此时可通过鉴别器根据 type 字段的值，动态选择映射身份证或角色。
    11.4.1 准备环境（表与实体类）
    1. 数据库表修改与新增
    -- 给user表新增type字段（1-普通用户，2-管理员）
    ALTER TABLE user ADD COLUMN type INT NOT NULL DEFAULT 1;
    -- 新增role表（管理员关联角色）
    CREATE TABLE IF NOT EXISTS role (
    id INT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL, -- 角色名称（如管理员、超级管理员）
    user_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id)
    );
    -- 更新测试数据
    UPDATE user SET type = 1 WHERE id = 1; -- 普通用户
    UPDATE user SET type = 2 WHERE id = 2; -- 管理员
    -- 插入管理员角色数据
    INSERT INTO role (role_name, user_id) VALUES ('管理员', 2);
2. 实体类设计
    创建 Role 实体类，修改 User 实体类，新增 type 字段和 role 属性（管理员关联角色）：
    // 角色实体类（com.example.pojo.Role）
    public class Role {
    private Integer id;
    private String roleName; // 对应role_name字段
    private Integer userId;
    // getter、setter、toString方法（省略）
    }
    // 修改后的User实体类
    public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;
    private Integer type; // 1-普通用户，2-管理员
    private IDCard idCard; // 普通用户关联身份证
    private Role role; // 管理员关联角色
    // getter、setter、toString方法（省略）
    }
    11.4.2 鉴别器映射实现（XML方式）
    鉴别器映射通过 <resultMap> 中的 <discriminator> 标签实现，核心属性：
    column：用于判断的数据库字段（如 user 表的 type）；
    javaType：判断字段的数据类型（如 Integer）；
    <case>：对应 switch-case 中的 case，value 是字段的具体值，内部可嵌套 <association>、<collection> 等标签，指定该值对应的映射规则。
    <!-- UserMapper.xml 中定义鉴别器映射的ResultMap -->
    <resultMap id="userWithDiscriminatorMap" type="user">
    <id column="id" property="id"/>
    <result column="username" property="username"/&gt;
    &lt;result column="type" property="type"/&gt; <!-- 鉴别器判断字段 -->
    <!-- 鉴别器：根据type字段的值，动态选择映射规则 -->
    <discriminator column="type" javaType="java.lang.Integer">
    <!-- case 1：type=1（普通用户），映射身份证信息 -->
        <case value="1">
            <association property="idCard" javaType="com.example.pojo.IDCard">
                <id column="card_id" property="id"/>
                <result column="card_no" property="cardNo"/>
                <result column="address" property="address"/>
            </association>
        </case>
        <!-- case 2：type=2（管理员），映射角色信息 -->
        <case value="2">
            <association property="role" javaType="com.example.pojo.Role">
                <id column="role_id" property="id"/>
                <result column="role_name" property="roleName"/>
            </association>
        </case>
    &lt;/discriminator&gt;
    &lt;/resultMap&gt;
    <!-- 查询用户，根据type动态映射不同的关联信息 -->
    <select id="selectUserWithDiscriminator" resultMap="userWithDiscriminatorMap">
    SELECT 
        u.*, 
        ic.id AS card_id, ic.card_no, ic.address, -- 普通用户关联的身份证字段
        r.id AS role_id, r.role_name -- 管理员关联的角色字段
    FROM user u
    LEFT JOIN id_card ic ON u.id = ic.user_id AND u.type = 1 -- 仅普通用户关联身份证
    LEFT JOIN role r ON u.id = r.user_id AND u.type = 2 -- 仅管理员关联角色
    WHERE u.id = #{id}
    </select>
    说明：查询时通过 AND 条件限制关联表的匹配，避免不必要的关联查询，当 type=1 时，仅关联 id_card 表；type=2 时，仅关联 role 表。
    11.4.3 鉴别器映射实现（注解方式）
    注解方式通过 @Discriminator 和 @Case 注解实现鉴别器映射，对应 XML 中的 <discriminator> 和 <case> 标签。
    // UserMapper接口（注解方式）
    public interface UserMapper {
    @Select("SELECT u.*, ic.id AS card_id, ic.card_no, ic.address, r.id AS role_id, r.role_name " +
            "FROM user u " +
            "LEFT JOIN id_card ic ON u.id = ic.user_id AND u.type = 1 " +
            "LEFT JOIN role r ON u.id = r.user_id AND u.type = 2 " +
            "WHERE u.id = #{id}")
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "username", property = "username"),
        @Result(column = "type", property = "type"),
        // 鉴别器映射
        @Discriminator(column = "type", javaType = Integer.class, cases = {
            // case 1：普通用户，映射身份证
            @Case(value = "1", results = {
                @Result(column = "card_id", property = "idCard.id"),
                @Result(column = "card_no", property = "idCard.cardNo"),
                @Result(column = "address", property = "idCard.address")
            }),
            // case 2：管理员，映射角色
            @Case(value = "2", results = {
                @Result(column = "role_id", property = "role.id"),
                @Result(column = "role_name", property = "role.roleName")
            })
        })
    })
    User selectUserWithDiscriminator(@Param("id") Integer id);
    }
    11.5 存储过程的用法
    存储过程是数据库中预编译的一组SQL语句的集合，具有封装性、安全性、高效性等特点，适用于复杂的业务逻辑（如多表关联操作、批量处理、数据统计等）。MyBatis 支持调用数据库中的存储过程，通过 <select>、<insert> 等标签的 statementType="CALLABLE" 属性实现。
    本节以 MySQL 为例，讲解 MyBatis 调用存储过程的完整流程，包括存储过程创建、MyBatis 配置、调用实操。
    11.5.1 准备：创建MySQL存储过程
    创建两个常用存储过程，分别实现“根据用户ID查询用户信息”和“批量插入用户”，演示不同类型存储过程的调用。
    存储过程1：根据用户ID查询用户信息（有输入参数、输出参数）
    -- 存储过程：根据user_id查询用户的用户名和年龄，通过输出参数返回
    DELIMITER // -- 临时修改语句结束符，避免与存储过程中的分号冲突
    CREATE PROCEDURE select_user_by_id(
    IN p_user_id INT, -- 输入参数：用户ID
    OUT p_username VARCHAR(50), -- 输出参数：用户名
    OUT p_age INT -- 输出参数：年龄
    )
    BEGIN
    SELECT username, age INTO p_username, p_age FROM user WHERE id = p_user_id;
    END //
    DELIMITER ; -- 恢复语句结束符
    存储过程2：批量插入用户（有输入参数，无输出参数）
    -- 存储过程：批量插入用户，接收用户名、密码、年龄、邮箱参数（可扩展为集合，此处简化）
    DELIMITER //
    CREATE PROCEDURE batch_insert_user(
    IN p_username VARCHAR(50),
    IN p_password VARCHAR(50),
    IN p_age INT,
    IN p_email VARCHAR(100)
    )
    BEGIN
    INSERT INTO user (username, password, age, email) VALUES (p_username, p_password, p_age, p_email);
    END //
    DELIMITER ;
    11.5.2 MyBatis调用存储过程（XML方式）
    MyBatis 调用存储过程时，需指定 statementType="CALLABLE"，并通过 #{ } 占位符传递输入/输出参数，输出参数需指定 mode="OUT"，输入参数可省略 mode（默认 mode="IN"）。
    调用存储过程1：有输入、输出参数
    <!-- UserMapper.xml 调用存储过程：根据ID查询用户信息 -->
    <select id="callSelectUserById" statementType="CALLABLE">
    {CALL select_user_by_id(

        #{userId, mode=IN, jdbcType=INTEGER}, -- 输入参数

        #{username, mode=OUT, jdbcType=VARCHAR}, -- 输出参数

        #{age, mode=OUT, jdbcType=INTEGER} -- 输出参数
    )}
</select>
对应 Mapper 接口方法（输出参数通过Map接收，或通过实体类接收）：
// 调用存储过程，输出参数通过Map接收
void callSelectUserById(Map<String, Object> paramMap);
调用测试：
@Test
public void testCallSelectUserById() {
    try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", 1); // 输入参数：用户ID
        // 调用存储过程
        userMapper.callSelectUserById(paramMap);
        // 获取输出参数
        String username = (String) paramMap.get("username");
        Integer age = (Integer) paramMap.get("age");
        System.out.println("用户名：" + username + "，年龄：" + age);
    }
}
调用存储过程2：仅输入参数
<!-- UserMapper.xml 调用存储过程：批量插入用户（简化版） -->
<insert id="callBatchInsertUser" statementType="CALLABLE">
    {CALL batch_insert_user(

        #{username, jdbcType=VARCHAR},

        #{password, jdbcType=VARCHAR},

        #{age, jdbcType=INTEGER},

        #{email, jdbcType=VARCHAR}
    )}
</insert>
对应 Mapper 接口方法：
// 调用存储过程，仅输入参数
int callBatchInsertUser(User user);
11.5.3 MyBatis调用存储过程（注解方式）
注解方式通过 @Select、@Insert 等注解，指定 statementType = StatementType.CALLABLE，同时通过 @Param 注解指定参数的输入/输出模式。
// UserMapper接口（注解方式调用存储过程）
public interface UserMapper {
    // 调用存储过程1：有输入、输出参数
    @Select(value = "{CALL select_user_by_id(#{userId, mode=IN, jdbcType=INTEGER}, " +
            "#{username, mode=OUT, jdbcType=VARCHAR}, #{age, mode=OUT, jdbcType=INTEGER})}")
    @Options(statementType = StatementType.CALLABLE)
    void callSelectUserById(@Param("userId") Integer userId,
                           @Param("username") String[] username, // 输出参数用数组接收（引用传递）
                           @Param("age") Integer[] age);
    // 调用存储过程2：仅输入参数
    @Insert(value = "{CALL batch_insert_user(#{username, jdbcType=VARCHAR}, " +
            "#{password, jdbcType=VARCHAR}, #{age, jdbcType=INTEGER}, #{email, jdbcType=VARCHAR})}")
    @Options(statementType = StatementType.CALLABLE)
    int callBatchInsertUser(User user);
}
说明：注解方式中，输出参数需用数组或对象接收（Java 中基本类型是值传递，无法接收输出参数）。
11.6 类型处理器的用法
MyBatis 的类型处理器（TypeHandler）用于解决数据库字段类型与 Java 实体类属性类型不匹配的问题，MyBatis 内置了大量常用的类型处理器（如 String 对应 VARCHAR、Integer 对应 INT、Date 对应 DATETIME 等），可满足大部分场景需求。
当内置类型处理器无法满足需求时（如：数据库字段是 VARCHAR，Java 属性是 Enum 枚举、JSON 对象，或自定义类型），我们可以自定义类型处理器，实现数据库类型与 Java 类型的双向转换。
11.6.1 内置类型处理器（默认使用）
MyBatis 内置了超过 30 种类型处理器，无需手动配置，自动匹配数据库类型与 Java 类型，常见示例：
Java 类型
数据库类型
内置类型处理器
String
VARCHAR、CHAR
StringTypeHandler
Integer、int
INT
IntegerTypeHandler
Date
DATETIME、DATE
DateTypeHandler
Boolean、boolean
TINYINT、BIT
BooleanTypeHandler
示例：数据库字段是 TINYINT(1)（存储 0/1），Java 属性是 Boolean（true/false），MyBatis 会通过 BooleanTypeHandler 自动转换，无需手动处理。
11.6.2 自定义类型处理器（重点）
以“数据库 VARCHAR 类型 → Java 枚举类型”为例，讲解自定义类型处理器的实现步骤，场景：用户表的 gender 字段（VARCHAR，存储“男/女”），Java 实体类中用 GenderEnum 枚举表示。
步骤1：定义枚举类和实体类
// 性别枚举类（com.example.enums.GenderEnum）
public enum GenderEnum {
    MALE("男"), FEMALE("女");
    private String value;
    GenderEnum(String value) {
        this.value = value;
    }
    // 根据value获取枚举（用于数据库值→Java枚举）
    public static GenderEnum getByValue(String value) {
        for (GenderEnum gender : GenderEnum.values()) {
            if (gender.value.equals(value)) {
                return gender;
            }
        }
        return null;
    }
    // 获取枚举的value（用于Java枚举→数据库值）
    public String getValue() {
        return value;
    }
}
// 修改User实体类，新增gender属性（枚举类型）
public class User {
    // 其他属性省略
    private GenderEnum gender; // 对应数据库gender字段（VARCHAR）
    // getter、setter、toString方法（省略）
}
步骤2：自定义类型处理器
自定义类型处理器需继承 BaseTypeHandler<T>（T 是 Java 类型，此处为 GenderEnum），重写 4 个抽象方法，实现数据库类型与 Java 类型的双向转换：
package com.example.typehandler;
import com.example.enums.GenderEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
/**
 * 自定义类型处理器：VARCHAR（数据库）→ GenderEnum（Java）
     */
    public class GenderTypeHandler extends BaseTypeHandler<GenderEnum> {
    // 1. Java类型 → 数据库类型（设置参数时调用）
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, GenderEnum parameter, JdbcType jdbcType) throws SQLException {
        // 将枚举的value（String）设置到SQL参数中
        ps.setString(i, parameter.getValue());
    }
    // 2. 数据库类型 → Java类型（从结果集获取值时调用，根据列名）
    @Override
    public GenderEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return GenderEnum.getByValue(value); // 将String转换为枚举
    }
    // 3. 数据库类型 → Java类型（从结果集获取值时调用，根据列索引）
    @Override
    public GenderEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return GenderEnum.getByValue(value);
    }
    // 4. 数据库类型 → Java类型（从存储过程获取值时调用）
    @Override
    public GenderEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return GenderEnum.getByValue(value);
    }
    }
    步骤3：配置自定义类型处理器
    需在 mybatis-config.xml 中配置自定义类型处理器，让 MyBatis 识别并使用：
    &lt;typeHandlers&gt;
    <!-- 配置自定义类型处理器 -->
    <typeHandler handler="com.example.typehandler.GenderTypeHandler" 
                 javaType="com.example.enums.GenderEnum" 
                 jdbcType="VARCHAR"/>
    </typeHandlers>
    说明：
    handler：自定义类型处理器的全类名；
    javaType：对应的 Java 类型（枚举类）；
    jdbcType：对应的数据库类型（VARCHAR）。
    步骤4：使用自定义类型处理器
    配置完成后，MyBatis 会自动使用自定义类型处理器，实现枚举与数据库字符串的双向转换，无需手动处理。
    <!-- UserMapper.xml 中使用枚举类型 -->
    <resultMap id="userMap" type="user">
    <id column="id" property="id"/>
    <result column="username" property="username"/>
    <result column="gender" property="gender"/&gt; <!-- 自动转换：VARCHAR→GenderEnum -->
    </resultMap>
    <select id="selectUserById" resultMap="userMap">
    SELECT * FROM user WHERE id = #{id}
    </select>
    <insert id="insertUser" parameterType="user">
    INSERT INTO user (username, password, gender) VALUES (#{username}, #{password}, #{gender})
    <!-- 自动转换：GenderEnum→VARCHAR -->
    </insert>
    注解方式同样无需额外配置，直接使用枚举类型即可，MyBatis 会自动调用自定义类型处理器。
    11.7 本章小结
    本章重点讲解了 MyBatis 高级查询的核心内容，包括高级结果映射、存储过程和类型处理器，核心要点如下：
    高级结果映射是解决复杂关联查询的核心，一对一映射用 <association>（@One），一对多映射用 <collection>（@Many），鉴别器映射用 <discriminator>（@Discriminator），可根据业务场景选择嵌套结果或嵌套查询（延迟加载）。
    存储过程通过 statementType="CALLABLE" 调用，输入/输出参数需明确指定 mode，适用于复杂业务逻辑的封装。
    类型处理器用于解决数据库类型与 Java 类型的不匹配问题，内置处理器可满足大部分场景，自定义处理器可适配枚举、JSON 等特殊类型，实现双向转换。
    掌握本章内容后，可轻松应对 MyBatis 中的复杂查询场景，结合前文的基础用法和动态 SQL，能够完整实现 MyBatis 从基础到高级的全部核心功能，为实际开发中的复杂业务需求提供解决方案。
