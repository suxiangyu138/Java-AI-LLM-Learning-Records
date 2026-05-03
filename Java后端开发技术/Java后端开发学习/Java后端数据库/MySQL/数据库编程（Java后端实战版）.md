03.20 14:53
数据库编程（Java后端实战版）
第一章 数据库编程核心认知
数据库编程，核心是通过代码（如Java）与数据库建立连接，执行SQL操作（查询、新增、修改、删除），实现业务数据的存储、读取与管理。
对于Java后端开发而言，数据库编程并非单纯编写SQL，而是结合业务场景，将SQL操作与后端代码融合，确保数据一致性、安全性和高效性，同时衔接前文所学的多表操作、视图、事务等知识点，解决实际业务中的数据处理需求。
核心目标：通过数据库编程，实现业务数据的CRUD（增删改查），支撑后端接口开发（如订单管理、用户权限管理），同时保证数据安全、操作高效，避免数据混乱和业务异常。
前置基础：已掌握MySQL基础操作、多表关联查询、视图使用、事务ACID特性，了解Java与数据库的连接方式（如JDBC、MyBatis），这是数据库编程的核心前提。
第二章 数据库编程核心准备（Java后端必备）
数据库编程的前提的是“环境就绪+规范统一”，避免因配置、命名不规范导致的开发效率低下或线上故障，以下准备工作是后续编程的基础。
2.1 数据库环境规范
数据库选型：生产环境优先使用MySQL（开源、稳定、适配Java生态），与前文所有操作保持一致，统一使用db_ecommerce数据库（电商场景），便于业务统一管理。
存储引擎：所有核心业务表（用户、订单、权限等）均使用InnoDB引擎，支持事务和外键约束，贴合前文事务、外键的使用规范；避免使用MyISAM引擎（不支持事务）。
账号权限：使用专用数据库账号（如db_ecommerce_user），仅分配业务所需权限（SELECT、INSERT、UPDATE），禁止分配DROP、ALTER等高危权限，降低安全风险（参考前文MySQL用户权限管理）。
2.2 命名与字段规范（衔接前文）
数据库编程的核心是“代码与数据库字段对齐”，避免因命名不一致导致的映射错误，规范如下：
表名：前缀+业务场景（如t_user、t_order），全部小写，下划线分隔，与前文表命名保持一致；
字段名：全部小写，下划线分隔，与Java实体类属性名对应（如Java实体类属性userId，对应数据库字段user_id）；
视图/存储过程：视图前缀v_（如v_user_permission），存储过程前缀proc_，便于区分和维护。
2.3 Java与数据库的连接方式
Java后端数据库编程，核心是通过连接工具（JDBC、MyBatis）与MySQL建立连接，执行SQL操作，两种主流方式适配不同业务场景：
JDBC：基础连接方式，需手动加载驱动、建立连接、执行SQL、关闭资源，适合简单场景（如单表查询）；
MyBatis：主流框架，简化SQL编写，支持多表映射、参数绑定，适配复杂业务场景（如多表关联、视图查询），是企业级开发的首选，前文所有多表、视图、事务操作均适配MyBatis。
关键提醒：无论使用哪种方式，都需保证“关闭自动提交”（适配事务控制），避免单条SQL执行失败导致数据不一致。
第三章 核心数据库编程场景（Java后端高频）
结合前文电商场景，聚焦Java后端最常用的3类数据库编程场景，整合多表、视图、事务知识点，每类场景配套完整的代码示例和编程注意事项，直接适配实际开发。
3.1 场景1：单表CRUD编程（基础）
适用于简单业务表（如商品分类、基础配置表），无多表关联，核心是规范SQL编写，适配Java实体类映射。
-- 1. 新增（插入商品分类）
INSERT INTO t_category (category_name, sort, is_delete)
VALUES ('手机', 1, 0);
-- 2. 查询（根据分类ID查询）
SELECT * FROM t_category WHERE id = 1 AND is_delete = 0;
-- 3. 修改（更新分类名称）
UPDATE t_category SET category_name = '智能手机' WHERE id = 1 AND is_delete = 0;
-- 4. 删除（逻辑删除，避免物理删除）
UPDATE t_category SET is_delete = 1 WHERE id = 1;
Java编程适配（MyBatis示例）：
// 新增分类
int addCategory(Category category);
// 查询分类
Category selectCategoryById(Integer id);
注意事项：单表操作需添加逻辑删除筛选（is_delete=0），避免查询/操作已删除数据；字段类型与Java实体类保持一致（如数据库BIGINT对应Java Long）。
3.2 场景2：多表关联编程（核心）
适用于订单、用户权限等复杂场景，需关联多张表（如用户表、订单表、商品表），核心是“关联条件准确+逻辑清晰”，衔接前文多表操作知识点。
示例：查询用户及其关联的订单信息（多表关联+条件筛选）
-- 多表查询（用户表+订单表）
SELECT u.id AS user_id, u.username, o.id AS order_id, o.order_no
FROM t_user u
INNER JOIN t_order o ON u.id = o.user_id
WHERE u.is_delete = 0 AND o.is_delete = 0 AND u.id = 1;
Java编程适配（MyBatis）：
// 映射多表结果（用户+订单）
public class UserOrderVO {
    private Long userId;
    private String username;
    private Long orderId;
    private String orderNo;
    // getter/setter
}
// Mapper接口
List<UserOrderVO> selectUserOrder(Integer userId);
注意事项：多表关联时，需明确关联条件（如u.id = o.user_id），避免关联错误；字段别名与Java实体类属性一致，减少映射异常。
3.3 场景3：视图关联编程（简化开发）
针对前文复杂的多表查询，通过视图封装后，数据库编程可简化SQL编写，无需重复编写复杂关联逻辑，贴合Java后端高效开发需求。
-- 1. 先创建视图（封装多表关联逻辑）
CREATE VIEW v_user_order AS
SELECT u.id AS user_id, u.username, o.order_no, o.order_status
FROM t_user u
INNER JOIN t_order o ON u.id = o.user_id
WHERE u.is_delete = 0 AND o.is_delete = 0;
-- 2. 数据库编程查询视图（简化SQL）
SELECT * FROM v_user_order WHERE user_id = 1;
Java编程适配：查询视图与查询普通表一致，MyBatis无需编写复杂多表映射，直接查询视图即可，大幅提升开发效率。
3.4 场景4：事务相关编程（核心重点）
数据库编程中，事务是保证数据一致性的核心，尤其是多表操作、批量操作场景，需通过代码控制事务的开启、提交与回滚，衔接前文事务ACID特性。
-- 开启事务（手动控制）
START TRANSACTION;
-- 执行多表操作（新增订单+新增订单详情）
INSERT INTO t_order (order_no, user_id, total_price, order_status)
VALUES ('20241001006', 1, 5999.00, 0);
INSERT INTO t_order_detail (order_id, goods_id, goods_name, buy_num)
VALUES (6, 2, '华为Mate60', 1);
-- 提交事务（所有操作成功）
COMMIT;
-- 若出现异常，执行回滚
-- ROLLBACK;
Java编程适配（Spring+MyBatis）：
// 用@Transactional注解控制事务
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order, List<OrderDetail> detailList) {
    // 新增订单
    orderMapper.addOrder(order);
    // 新增订单详情
    orderDetailMapper.addDetailList(detailList);
}
注意事项：事务需包含所有相关SQL操作，异常时必须回滚；避免事务范围过大（如包含无关的日志操作），影响性能。
第四章 数据库编程避坑指南（Java后端必看）
数据库编程的核心风险是“数据不一致、代码冗余、性能低下、安全隐患”，结合前文知识点，总结6个高频坑点及解决方案，避免线上故障。
坑点1：SQL与Java实体类字段不匹配
❌ 错误：数据库字段user_id，Java实体类属性userId，MyBatis映射时未指定别名，导致映射失败；
✅ 解决方案：创建视图或编写SQL时，给字段取与Java属性一致的别名（如u.id AS userId）。
坑点2：事务未控制，导致数据不一致
❌ 错误：多表操作时未开启事务，新增订单成功但订单详情失败，导致“有订单无详情”；
✅ 解决方案：所有多表操作、批量操作必须纳入事务控制，指定rollbackFor=Exception.class。
坑点3：过度使用视图/存储过程
❌ 错误：每个简单查询都创建视图，或用存储过程封装简单SQL，增加维护成本；
✅ 解决方案：仅对“多次复用、复杂关联”的SQL创建视图，存储过程仅用于封装极复杂的业务逻辑（如批量数据处理）。
坑点4：忽略逻辑删除，导致数据混乱
❌ 错误：查询、修改时未添加is_delete = 0，查询到已删除数据；
✅ 解决方案：所有SQL操作（查询、新增、修改）均需添加逻辑删除筛选，与前文数据管理规范保持一致。
坑点5：数据库账号权限过高
❌ 错误：使用root账号连接项目，存在数据泄露风险；
✅ 解决方案：使用专用账号，仅分配业务所需权限（SELECT、INSERT、UPDATE），禁止分配DROP、ALTER权限。
坑点6：并发场景未处理，导致数据异常
❌ 错误：高并发下单时，未设置隔离级别，导致库存超卖；
✅ 解决方案：高并发场景设置隔离级别为“串行化”，或结合乐观锁（如添加version字段）控制并发。
第五章 数据库编程实战练习（Java后端视角）
基于前文电商场景，完成以下数据库编程练习，巩固核心知识点，贴合实际开发流程。
使用MyBatis编写代码，实现“新增用户+分配角色”的数据库编程：插入用户（t_user），同时关联角色（t_user_role），纳入事务控制，确保两个操作同时成功或同时失败；
编写SQL创建视图（v_goods_sales），统计每个商品的销售数量和总金额，再通过Java代码查询该视图，封装成VO返回；
实现“订单创建”的数据库编程：新增订单（t_order）、新增订单详情（t_order_detail），用事务控制，模拟异常场景（如详情插入失败），验证事务回滚功能；
编写代码查询用户权限（结合视图v_user_permission），将结果封装成Java实体类，返回给前端；
模拟高并发场景，设置事务隔离级别为串行化，编写代码实现商品库存扣减，避免库存负数。
提示：练习时，重点关注“SQL编写→Java映射→事务控制”的完整流程，同时规避上述坑点，确保代码可直接复用。
第六章 本章小结（数据库编程核心要点）
数据库编程的核心是“业务适配”：结合Java后端接口需求，编写规范SQL，实现数据的CRUD，同时保证数据一致性；
关键知识点衔接：多表关联、视图、事务是数据库编程的核心，需灵活运用，简化开发、保障数据安全；
避坑核心：规范命名、控制事务范围、设置合适的隔离级别、使用最小权限账号，避免数据不一致和安全隐患；
后续延伸：数据库编程可结合索引优化（提升查询性能）、存储过程封装（复杂逻辑），进一步适配高并发、复杂业务场景。

