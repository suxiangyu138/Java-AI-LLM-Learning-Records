03.20 15:12
MyBatis与MyBatis-Plus（Java后端企业级实战版）
核心说明：Java后端数据库编程中，MyBatis是主流的持久层框架，负责将Java代码与SQL语句映射，简化数据库操作；而MyBatis-Plus（简称MP）是MyBatis的增强工具，在MyBatis基础上封装了大量常用功能，无需编写重复SQL，大幅提升开发效率。
本章衔接前文数据库编程、SQL优化、数据库配置等知识点，重点讲解MyBatis与MyBatis-Plus的核心区别、实战用法、适配场景，以及如何从MyBatis无缝迁移到MyBatis-Plus，全程贴合电商业务场景，确保与前文知识点连贯，同时覆盖企业级开发中的高频用法和避坑点，帮助开发者快速掌握两者的使用技巧。
前置基础：已掌握MySQL基础操作、数据库编程、SQL优化、Spring Boot项目配置，了解MyBatis的基本用法（如Mapper接口、XML映射文件），这是学习MyBatis-Plus的核心前提，也是衔接前文数据库编程场景的关键。
12.1 核心认知：MyBatis与MyBatis-Plus的关系
MyBatis-Plus并非替代MyBatis，而是“基于MyBatis的增强工具”，遵循“不改变MyBatis原有功能、不侵入原有代码”的原则，在MyBatis基础上增加了CRUD接口封装、条件构造器、分页插件等功能，核心目标是“简化开发、减少重复代码”。
核心关系总结（贴合Java后端实战）：
MyBatis：核心是“SQL映射”，灵活性高，可手动编写复杂SQL，适配复杂业务场景（如多表关联、复杂统计），是后端数据库编程的基础；
MyBatis-Plus：核心是“增强便捷”，封装了常用CRUD操作，无需编写XML映射文件和基础SQL，适合简单CRUD场景，同时兼容MyBatis的所有功能，可无缝衔接；
实战选型：简单CRUD场景用MyBatis-Plus提升效率，复杂SQL场景（如多表关联、SQL优化）用MyBatis手动编写SQL，两者可共存于同一个项目中。
关键提醒：MyBatis-Plus完全兼容MyBatis，已使用MyBatis的项目可无缝迁移到MyBatis-Plus，无需修改原有代码，仅需添加依赖和简单配置即可。
12.2 核心对比：MyBatis与MyBatis-Plus（企业级实战视角）
结合前文电商场景（t_user、t_order等表），从开发效率、功能特性、适用场景等维度，对比两者的核心差异，帮助开发者快速选型，衔接前文数据库编程知识点。
对比维度
MyBatis
MyBatis-Plus
企业级实战选型建议
开发效率
较低：需手动编写XML映射文件、CRUD SQL语句，重复代码多
较高：封装了BaseMapper接口，提供默认CRUD方法，无需编写基础SQL
简单CRUD场景选MP，减少重复开发
SQL控制
灵活性极高：可手动编写任意复杂SQL（多表关联、子查询、优化SQL），适配前文SQL优化场景
兼容MyBatis：可手动编写复杂SQL，同时提供条件构造器，简化SQL拼接
复杂SQL场景用MyBatis手动编写，简单条件查询用MP条件构造器
功能特性
基础功能：SQL映射、参数绑定、结果映射，无额外增强功能
增强功能：条件构造器、分页插件、乐观锁、自动填充、逻辑删除等，适配前文事务、优化场景
需要乐观锁、分页等功能时，优先用MP，无需重复开发
学习成本
中等：需掌握XML映射规则、SQL编写、参数绑定等知识点
低：基于MyBatis，只需额外掌握BaseMapper、条件构造器等简单用法，上手快
新手优先学MP，快速落地开发；进阶需掌握MyBatis的复杂SQL编写
适用场景
复杂业务场景：多表关联、复杂统计、SQL优化、自定义SQL场景（贴合前文多表操作、优化知识点）
简单CRUD场景：单表查询、新增、修改、删除，以及需要分页、乐观锁的场景
项目中两者共存：单表CRUD用MP，复杂查询用MyBatis
12.3 MyBatis实战用法（回顾+衔接前文）
MyBatis是Java后端数据库编程的基础，前文数据库编程章节已简单提及，本节结合电商场景，回顾MyBatis的核心用法，衔接前文多表操作、SQL优化知识点，确保上下文连贯。
12.3.1 MyBatis核心配置（Spring Boot适配）
MyBatis的核心配置的是“连接数据库+映射配置”，与前文数据库配置章节衔接，Spring Boot项目中配置如下（application.yml）：
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://192.168.1.100:3306/db_ecommerce?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false
    username: db_ecommerce_user
    password: Xx@123456
# MyBatis核心配置
mybatis:
  mapper-locations: classpath:mapper/**/*.xml  # XML映射文件路径（存放SQL语句）
  type-aliases-package: com.example.ecommerce.entity  # 实体类包路径（简化别名）
  configuration:
    map-underscore-to-camel-case: true  # 下划线转驼峰（适配数据库字段与Java实体类）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 打印SQL日志（便于调试、优化SQL）
12.3.2 MyBatis核心实战（多表+优化）
结合前文多表操作、SQL优化知识点，以“查询用户及其订单信息”为例，讲解MyBatis的实战用法（XML映射+Mapper接口）。
实体类（User、Order）： // User实体类（对应t_user表） public class User { private Long id; private String username; private String password; // 省略getter/setter } // Order实体类（对应t_order表） public class Order { private Long id; private String orderNo; private Long userId; private BigDecimal totalPrice; // 省略getter/setter }
Mapper接口（定义方法）： // UserOrderMapper接口 public interface UserOrderMapper { // 查询用户及其订单信息（多表关联） List<UserOrderVO> selectUserOrder(@Param("userId") Long userId); }
XML映射文件（编写优化后的SQL）： <?xml version="1.0" encoding="UTF-8"?> <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd"> <mapper namespace="com.example.ecommerce.mapper.UserOrderMapper"> <!-- 结果映射（适配Java VO与数据库字段） --> <resultMap id="userOrderMap" type="com.example.ecommerce.vo.UserOrderVO"> <result column="user_id" property="userId"/> <result column="username" property="username"/><result column="order_id" property="orderId"/> <result column="order_no" property="orderNo"/> </resultMap> <!-- 多表关联查询（优化后，结合索引） --> </mapper>
关键提醒：MyBatis的核心是“SQL手动编写”，可灵活适配复杂多表关联、SQL优化场景，与前文数据库优化知识点完美衔接，适合复杂业务需求。
12.4 MyBatis-Plus实战用法（核心重点）
MyBatis-Plus在MyBatis基础上做了增强，核心优势是“简化CRUD操作”，无需编写XML映射文件和基础SQL，结合电商场景，讲解其核心用法，适配前文事务、乐观锁等知识点。
12.4.1 MyBatis-Plus环境配置（Spring Boot适配）
MyBatis-Plus的配置与MyBatis基本一致，只需添加MP依赖，修改少量配置，即可无缝适配前文数据库配置。
添加依赖（pom.xml）： <!-- MyBatis-Plus依赖（替代MyBatis依赖） --> <dependency> <groupId>com.baomidou</groupId> <artifactId>mybatis-plus-boot-starter</artifactId> <version>3.5.3.1</version> </dependency> <!-- 数据库驱动依赖（MySQL 8.0+） --> <dependency> <groupId>mysql</groupId> <artifactId>mysql-connector-java</artifactId> <scope>runtime</scope> </dependency>
配置application.yml（与MyBatis兼容，新增MP专属配置）： spring: datasource: driver-class-name: com.mysql.cj.jdbc.Driver url: jdbc:mysql://192.168.1.100:3306/db_ecommerce?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false username: db_ecommerce_user password: Xx@123456 # MyBatis-Plus配置（兼容MyBatis配置） mybatis-plus: mapper-locations: classpath:mapper/**/*.xml # 兼容MyBatis XML映射文件 type-aliases-package: com.example.ecommerce.entity configuration: map-underscore-to-camel-case: true log-impl: org.apache.ibatis.logging.stdout.StdOutImpl # MP专属配置 global-config: db-config: id-type: AUTO # 主键自增（适配前文表结构主键规范） logic-delete-field: isDelete # 逻辑删除字段（适配前文逻辑删除规范） logic-delete-value: 1 # 逻辑删除标识（1=删除） logic-not-delete-value: 0 # 未删除标识（0=未删除）
12.4.2 MyBatis-Plus核心功能（实战高频）
结合电商场景，讲解MyBatis-Plus的核心增强功能，衔接前文事务、乐观锁、逻辑删除等知识点，突出“简化开发”的优势。
1. BaseMapper接口（无需编写基础CRUD）
MyBatis-Plus提供BaseMapper接口，封装了常用CRUD方法（新增、查询、修改、删除），Mapper接口只需继承BaseMapper，即可直接使用，无需编写XML和SQL。
// UserMapper接口（继承BaseMapper，无需编写基础方法）
public interface UserMapper extends BaseMapper<User> {
    // 如需复杂查询，可手动添加方法（兼容MyBatis）
    List<User> selectUserByUsername(@Param("username") String username);
}
// 服务层调用（无需编写SQL，直接使用BaseMapper方法）
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    // 新增用户（BaseMapper自带方法）
    @Override
    public boolean addUser(User user) {
        return userMapper.insert(user) > 0;
    }
    // 根据ID查询用户（BaseMapper自带方法）
    @Override
    public User selectUserById(Long id) {
        return userMapper.selectById(id);
    }
    // 复杂查询（手动编写方法）
    @Override
    public List<User> selectUserByUsername(String username) {
        return userMapper.selectUserByUsername(username);
    }
}
2. 条件构造器（QueryWrapper/LambdaQueryWrapper）
无需编写SQL条件，通过条件构造器拼接查询条件，简化查询操作，适配前文多条件查询场景（如订单列表查询、商品筛选）。
// 订单列表查询（多条件：用户ID+订单状态+未删除）
@Override
public List<Order> selectOrderList(Long userId, Integer orderStatus) {
    // LambdaQueryWrapper：避免字段名写错，更安全
    LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<Order>();
    queryWrapper.eq(Order::getUserId, userId)  // 等于
                .eq(Order::getOrderStatus, orderStatus)
                .eq(Order::getIsDelete, 0)  // 逻辑删除筛选（衔接前文规范）
                .orderByDesc(Order::getCreateTime);  // 排序（衔接前文优化）
    return orderMapper.selectList(queryWrapper);
}
3. 乐观锁（适配高并发场景）
结合前文数据库优化中的乐观锁知识点，MyBatis-Plus可通过注解快速实现乐观锁，无需手动编写SQL，避免高并发库存超卖等问题。
// 商品实体类（添加@Version注解，标识乐观锁版本号）
public class Goods {
    private Long id;
    private String goodsName;
    private Integer stock;
    @Version  // MP乐观锁注解
    private Integer version;  // 版本号字段（前文优化章节新增）
    // 省略getter/setter
}
// 库存扣减（MP自动实现乐观锁逻辑，无需手动编写version判断）
@Override
public boolean deductStock(Long goodsId) {
    Goods goods = goodsMapper.selectById(goodsId);
    if (goods == null || goods.getStock() <= 0) {
        return false;
    }
    // 修改库存
    goods.setStock(goods.getStock() - 1);
    // MP自动拼接version条件：WHERE id = ? AND version = ?
    return goodsMapper.updateById(goods) > 0;
}
4. 分页插件（适配前文分页优化）
MyBatis-Plus提供分页插件，无需手动编写分页SQL（LIMIT），简化分页操作，适配前文分页优化场景（如订单列表分页）。
// 1. 配置分页插件（Spring Boot配置类）
@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
// 2. 分页查询（订单列表分页）
@Override
public IPage<Order> selectOrderPage(Integer pageNum, Integer pageSize, Long userId) {
    // 分页对象
    Page<Order> page = new Page<>(pageNum, pageSize);
    // 条件构造器
    LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<Order>()
            .eq(Order::getUserId, userId)
            .eq(Order::getIsDelete, 0);
    // 分页查询（MP自动拼接分页SQL）
    return orderMapper.selectPage(page, queryWrapper);
}
12.5 MyBatis与MyBatis-Plus无缝衔接（企业级实战）
实际开发中，通常采用“MyBatis-Plus为主，MyBatis为辅”的方式，简单CRUD用MP，复杂SQL用MyBatis，两者无缝衔接，不影响原有代码，衔接前文数据库编程、优化知识点。
12.5.1 衔接方式（核心）
Mapper接口：既可以继承BaseMapper（使用MP的CRUD方法），也可以手动添加方法（使用MyBatis的XML映射）；
XML映射文件：MP兼容MyBatis的XML映射文件，可在XML中编写复杂SQL，与MP的方法共存；
配置：MP的配置兼容MyBatis，无需修改原有MyBatis配置，只需新增MP专属配置（如逻辑删除、分页插件）。
12.5.2 实战示例（多表复杂查询+MP分页）
结合前文多表操作、分页优化知识点，实现“用户订单分页查询”，用MP的分页插件，MyBatis的XML编写复杂多表SQL，实现两者无缝衔接。
// 1. Mapper接口（继承BaseMapper，添加复杂查询方法）
public interface UserOrderMapper extends BaseMapper<UserOrderVO> {
    // 多表复杂查询（手动编写XML SQL）
    IPage<UserOrderVO> selectUserOrderPage(Page<UserOrderVO> page, @Param("userId") Long userId);
}
// 2. XML映射文件（编写多表关联SQL）
<select id="selectUserOrderPage" resultType="com.example.ecommerce.vo.UserOrderVO">
    SELECT u.id AS userId, u.username, o.id AS orderId, o.orderNo, o.totalPrice
    FROM t_user u
    INNER JOIN t_order o ON u.id = o.userId
    WHERE u.id = #{userId} AND u.isDelete = 0 AND o.isDelete = 0
    ORDER BY o.createTime DESC
</select>
// 3. 服务层调用（MP分页+MyBatis复杂SQL）
@Override
public IPage<UserOrderVO> selectUserOrderPage(Integer pageNum, Integer pageSize, Long userId) {
    Page<UserOrderVO> page = new Page<>(pageNum, pageSize);
    return userOrderMapper.selectUserOrderPage(page, userId);
}
12.6 避坑指南（Java后端必看）
使用MyBatis与MyBatis-Plus时，容易出现SQL失效、映射异常、功能冲突等问题，结合前文知识点，总结6个高频坑点，避免开发故障。
避坑1：MyBatis与MyBatis-Plus依赖冲突
❌ 错误：项目中同时引入MyBatis和MyBatis-Plus依赖，导致版本冲突、功能异常；
✅ 正确：仅引入MyBatis-Plus依赖（mybatis-plus-boot-starter），其内部已包含MyBatis依赖，无需额外引入。
避坑2：MP条件构造器字段名写错
❌ 错误：使用QueryWrapper时，字段名写数据库字段（如user_id），而非Java实体类属性（userId）；
✅ 正确：优先使用LambdaQueryWrapper，通过实体类方法引用（如User::getUserId），避免字段名写错。
避坑3：乐观锁未配置版本号
❌ 错误：添加@Version注解后，未在数据库表中添加version字段，导致乐观锁失效；
✅ 正确：实体类添加@Version注解，同时在数据库表中新增version字段（INT类型，默认1），与前文优化知识点衔接。
避坑4：MyBatis XML映射文件命名空间错误
❌ 错误：XML映射文件的namespace与Mapper接口全路径不一致，导致方法无法映射；
✅ 正确：namespace必须与Mapper接口全路径一致（如com.example.ecommerce.mapper.UserMapper）。
避坑5：MP逻辑删除与手动SQL冲突
❌ 错误：MP配置了逻辑删除，但手动编写SQL时未添加is_delete=0筛选，导致查询到已删除数据；
✅ 正确：手动编写SQL时，必须添加逻辑删除筛选，与MP逻辑删除配置保持一致（衔接前文数据规范）。
避坑6：过度依赖MP，忽视复杂SQL编写
❌ 错误：用MP的条件构造器拼接复杂多表SQL，导致SQL冗余、效率低下；
✅ 正确：复杂多表查询、SQL优化场景，优先用MyBatis手动编写XML SQL，MP仅用于简单CRUD和分页。
12.7 本章实战练习（Java后端视角）
基于前文电商场景，结合MyBatis与MyBatis-Plus知识点，完成以下实战练习，掌握两者的用法及无缝衔接技巧，适配企业级开发需求。
搭建Spring Boot项目，引入MyBatis-Plus依赖，配置数据库连接和MP核心参数（逻辑删除、分页插件）；
创建User、Order实体类，编写Mapper接口（继承BaseMapper），使用MP的BaseMapper方法实现单表CRUD；
使用MP条件构造器，实现“查询未删除用户、按创建时间排序”的多条件查询；
结合乐观锁，实现商品库存扣减功能，避免高并发库存超卖；
编写XML映射文件，实现“用户订单多表关联查询”，结合MP分页插件，实现分页功能；
模拟从MyBatis迁移到MyBatis-Plus，确保原有XML映射文件和SQL正常运行，实现两者无缝衔接。
提示：练习时，重点关注两者的衔接方式，区分简单CRUD与复杂SQL的场景选型，同时规避上述坑点，确保代码规范、高效，贴合前文数据库编程、优化知识点。
12.8 本章小结（MyBatis与MyBatis-Plus核心要点）
核心关系：MyBatis-Plus是MyBatis的增强工具，不替代MyBatis，兼容MyBatis所有功能，核心优势是简化开发、减少重复代码；
实战选型：简单CRUD用MyBatis-Plus（BaseMapper、条件构造器、分页插件），复杂SQL、多表关联、SQL优化用MyBatis手动编写；
关键衔接：两者可无缝衔接，Mapper接口可同时继承BaseMapper和手动添加方法，XML映射文件兼容，配置可复用，贴合前文数据库编程、优化、配置知识点；
核心功能：MyBatis重点是SQL灵活性，MyBatis-Plus重点是增强便捷（乐观锁、分页、逻辑删除等），适配不同业务场景；
避坑核心：避免依赖冲突、字段名写错、乐观锁配置缺失、逻辑删除冲突，合理选型，不盲目依赖MP的复杂条件构造器。

