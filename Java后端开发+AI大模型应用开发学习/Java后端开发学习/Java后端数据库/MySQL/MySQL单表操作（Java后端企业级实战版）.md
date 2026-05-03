03.20 12:57
MySQL单表操作（Java后端企业级实战版）
第五章：MySQL单表操作（Java后端核心实战）
核心说明：单表操作是Java后端开发中最基础、最高频的数据库操作，所有业务接口（登录、注册、编辑、删除）的底层，本质都是单表CRUD（创建Create、查询Read、修改Update、删除Delete）。本章基于前文“数据库设计”章节的电商核心表（t_user、t_goods等），全程贴合Java后端实战场景，讲解单表操作的企业级语法、实战技巧、避坑指南，摒弃入门级冗余内容，重点适配Java实体类映射、接口开发需求。
前置基础：已掌握数据库、表的创建，熟悉数据类型与约束（如主键、非空、唯一），明确MySQL字段与Java实体类的对应关系（如BIGINT→Long、VARCHAR→String）。本章所有操作均基于db_ecommerce数据库的t_user表（用户表）展开，统一表结构如下（后续操作均基于此表）：
-- 确认t_user表结构（企业级规范版）
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，对应Java Long',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名，对应Java String',
    password VARCHAR(100) NOT NULL COMMENT '加密后密码，对应Java String',
    phone VARCHAR(11) UNIQUE COMMENT '手机号，对应Java String',
    gender TINYINT DEFAULT 0 COMMENT '性别：0未知、1男、2女，对应Java Integer',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删、1已删，对应Java Boolean',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，对应Java LocalDateTime',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，对应Java LocalDateTime'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户表';
5.1 单表核心操作：CRUD（企业级实战）
单表CRUD是Java后端接口的核心底层操作，对应业务场景如下：新增用户（注册）→ Create、查询用户（登录/列表）→ Read、修改用户（编辑信息）→ Update、删除用户（注销）→ Delete。以下操作均遵循企业级规范，适配Java后端代码映射（如MyBatis）。
5.1.1 新增操作（INSERT）—— 对应Java后端“注册”场景
核心用途：用户注册时，后端接收前端传参（用户名、密码等），执行INSERT语句将数据存入t_user表，需避免重复注册、密码明文存储等问题。
企业级语法（推荐）
-- 1. 指定字段插入（推荐，避免字段顺序变化导致报错，适配Java实体类赋值）
INSERT INTO t_user (username, password, phone, gender)
VALUES ('lisi', 'e10adc3949ba59abbe56e057f20f883e', '13900139000', 1);
-- 2. 批量新增（高频，如批量导入用户，提升效率，减少Java代码循环插入）
INSERT INTO t_user (username, password, phone, gender)
VALUES 
('zhaoliu', 'e10adc3949ba59abbe56e057f20f883e', '13600136000', 1),
('qianqi', 'e10adc3949ba59abbe56e057f20f883e', '13500135000', 2);
-- 3. 避免重复插入（结合唯一约束，注册场景必用）
INSERT IGNORE INTO t_user (username, password, phone, gender)
VALUES ('lisi', 'e10adc3949ba59abbe56e057f20f883e', '13900139000', 1);
-- 说明：IGNORE表示若用户名/手机号重复（唯一约束），则忽略该条插入，不报错
Java后端关联实战
后端代码中，通过MyBatis的insert标签执行上述SQL，参数从前端接收（如username、phone），密码需先通过MD5/SHA256加密（禁止明文存储）；
实体类User的属性的顺序，无需与INSERT语句的字段顺序一致，但字段名必须完全匹配（如实体类的username对应表的username）；
批量新增比循环单条新增效率高，Java后端批量注册、批量导入场景优先使用批量INSERT。
避坑点
禁止省略字段列表（如INSERT INTO t_user VALUES (...)），字段顺序变化会导致数据插入错误，适配Java实体类时易出现映射异常；
密码必须加密存储，禁止明文插入（示例中密码为MD5加密后的值，Java后端可通过DigestUtils工具类实现加密）；
非空字段（username、password）必须赋值，否则会报错，可通过Java代码校验前端传参，避免无效插入。
5.1.2 查询操作（SELECT）—— 对应Java后端“登录/列表”场景
查询是Java后端最高频的单表操作（如用户登录、用户列表查询、详情查询），核心是“精准筛选、高效查询”，避免冗余数据传输，提升接口性能。
企业级高频语法（按使用场景分类）
-- 场景1：用户登录（精准查询，按用户名查询，Java后端登录接口核心）
SELECT id, username, password, phone FROM t_user 
WHERE username = 'lisi' AND is_delete = 0;  -- 必加逻辑删除筛选，避免查询已注销用户
-- 场景2：用户列表查询（分页，Java后端列表接口核心，避免查询所有数据）
SELECT id, username, phone, gender FROM t_user 
WHERE is_delete = 0 
ORDER BY create_time DESC  -- 按创建时间倒序，符合用户列表展示习惯
LIMIT 0, 10;  -- 分页：起始索引0，每页10条（页码=1时）
-- 场景3：查询单个用户详情（按主键ID，Java后端详情接口核心）
SELECT * FROM t_user WHERE id = 1 AND is_delete = 0;  -- 主键查询效率最高
-- 场景4：条件筛选查询（如查询性别为男的用户）
SELECT id, username, phone FROM t_user 
WHERE gender = 1 AND is_delete = 0;
-- 场景5：模糊查询（如根据用户名模糊搜索，Java后端搜索接口）
SELECT id, username, phone FROM t_user 
WHERE username LIKE '%li%' AND is_delete = 0;  -- %表示任意字符
-- 场景6：去重查询（如查询所有已注册的手机号，避免重复）
SELECT DISTINCT phone FROM t_user WHERE is_delete = 0;
Java后端关联实战
登录场景：后端接收前端传入的username，执行“按用户名查询”SQL，获取数据库中的password（加密后），与前端传入的密码（加密后）对比，验证登录；
列表查询：后端接收前端传入的页码、每页条数，计算起始索引（起始索引=（页码-1）*每页条数），执行带LIMIT的查询，避免一次性查询所有数据（数据量大时导致接口超时）；
禁止使用SELECT * 查询（冗余数据多，传输效率低），需指定查询字段，与Java实体类的属性一一对应（如查询id、username，对应User类的id、username属性）。
避坑点
所有查询操作必须加逻辑删除筛选（is_delete = 0），否则会查询到已注销/删除的用户，导致业务异常；
模糊查询（LIKE）尽量避免以%开头（如'%li'），会导致索引失效，查询效率降低（后续索引章节详解）；
主键查询（WHERE id = ?）效率最高，Java后端详情接口优先使用主键查询。
5.1.3 修改操作（UPDATE）—— 对应Java后端“编辑”场景
核心用途：用户编辑个人信息（修改手机号、性别）、管理员重置密码等场景，核心是“精准修改，避免误改”，必须加WHERE条件（否则会修改全表数据）。
企业级语法（精准修改）
-- 场景1：编辑用户信息（修改手机号、性别，按主键ID修改，最安全）
UPDATE t_user 
SET phone = '13900000000', gender = 2, update_time = NOW()
WHERE id = 1 AND is_delete = 0;  -- 加主键+逻辑删除，避免误改
-- 场景2：重置密码（管理员操作，按用户名修改）
UPDATE t_user 
SET password = 'e10adc3949ba59abbe56e057f20f883e'
WHERE username = 'lisi' AND is_delete = 0;
-- 场景3：批量修改（谨慎使用，如批量禁用用户）
UPDATE t_user 
SET is_enable = 0  -- 假设新增is_enable字段（是否启用）
WHERE id IN (1,2,3) AND is_delete = 0;
Java后端关联实战
修改操作的参数（如新手机号、新密码）从前端接收，Java代码中通过MyBatis的update标签执行SQL；
必须通过主键ID（id）定位要修改的用户，避免按用户名、手机号修改（可能存在重复，导致误改）；
修改敏感字段（如密码、手机号）时，需在Java代码中验证用户权限（如只能修改自己的信息）。
避坑点（高频错误）
禁止省略WHERE条件：UPDATE t_user SET phone = '13900000000' 会修改表中所有未删除用户的手机号，生产环境绝对禁止；
修改时需加逻辑删除筛选（is_delete = 0），避免修改已删除的用户数据；
禁止一次性修改过多字段，按需修改（如只改手机号，就只写phone = ?），减少数据传输和误改风险。
5.1.4 删除操作（DELETE/逻辑删除）—— 对应Java后端“注销”场景
企业级开发中，禁止使用物理删除（DELETE语句），优先使用逻辑删除（修改is_delete字段），避免数据丢失、无法追溯。仅本地测试、无效数据清理时可使用物理删除。
1. 逻辑删除（企业级首选，对应用户注销）
-- 逻辑删除：修改is_delete为1，不删除实际数据，Java接口查询时过滤
UPDATE t_user SET is_delete = 1, update_time = NOW()
WHERE id = 1 AND is_delete = 0;
-- 恢复逻辑删除（如用户误注销，管理员恢复）
UPDATE t_user SET is_delete = 0, update_time = NOW()
WHERE id = 1;
2. 物理删除（仅本地测试/清理无效数据）
-- 物理删除：彻底删除数据，无法恢复，生产环境禁止
DELETE FROM t_user WHERE id = 10 AND is_delete = 1;  -- 仅删除已逻辑删除的无效数据
-- 禁止操作（生产环境绝对禁止）
DELETE FROM t_user;  -- 删除表中所有数据，无法恢复
Java后端关联实战
用户注销接口：后端执行逻辑删除SQL（修改is_delete=1），而非物理删除；
Java代码中，所有查询、修改、删除操作，都需添加is_delete=0的筛选条件，避免操作已注销用户；
物理删除仅用于本地测试环境，清理无效测试数据，生产环境无物理删除权限（由运维负责）。
5.2 单表查询高级技巧（Java后端高频）
除基础查询外，Java后端列表接口、统计接口常需用到排序、分页、聚合函数等高级查询，以下均为企业级实战必备技巧，适配接口开发需求。
5.2.1 排序查询（ORDER BY）
核心用途：用户列表、商品列表按时间、价格等排序，Java后端接口需支持前端传入排序字段和排序方式（升序/降序）。
-- 1. 单字段排序（按创建时间倒序，最新注册的用户在前）
SELECT id, username, phone FROM t_user 
WHERE is_delete = 0 
ORDER BY create_time DESC;  -- DESC降序，ASC升序（默认）
-- 2. 多字段排序（先按性别升序，再按创建时间倒序）
SELECT id, username, gender, create_time FROM t_user 
WHERE is_delete = 0 
ORDER BY gender ASC, create_time DESC;
Java后端适配：前端传入sortField（排序字段，如create_time）、sortType（排序方式，如desc），后端拼接SQL，避免硬编码（如"ORDER BY " + sortField + " " + sortType）。
5.2.2 分页查询（LIMIT）—— 接口性能核心
Java后端列表接口必用分页，避免一次性查询大量数据导致接口超时、内存溢出，LIMIT语法是分页的核心。
-- 语法：LIMIT 起始索引, 每页条数（起始索引从0开始）
-- 场景：用户列表，第1页（每页10条）
SELECT id, username, phone FROM t_user 
WHERE is_delete = 0 
ORDER BY create_time DESC 
LIMIT 0, 10;
-- 第2页（起始索引=（2-1）*10=10）
SELECT id, username, phone FROM t_user 
WHERE is_delete = 0 
ORDER BY create_time DESC 
LIMIT 10, 10;
Java后端实战技巧
后端接收前端传入的pageNum（页码）、pageSize（每页条数），计算起始索引：startIndex = (pageNum - 1) * pageSize；
禁止使用LIMIT 10000, 10（起始索引过大），会导致查询效率极低，后续索引章节讲解优化方案；
分页查询需搭配ORDER BY，否则分页结果无序，用户体验差。
5.2.3 聚合函数（COUNT/SUM/AVG）—— 统计接口核心
Java后端统计接口（如用户总数、平均年龄），需用到MySQL聚合函数，常用3种函数如下（结合t_user表实战）：
-- 1. COUNT：统计数量（用户总数，Java后端统计接口核心）
SELECT COUNT(id) AS user_count FROM t_user WHERE is_delete = 0;
-- 说明：COUNT(id)比COUNT(*)效率高，id为主键（非空），避免统计NULL值
-- 2. SUM：求和（如统计某类用户的总积分，假设新增points字段）
SELECT SUM(points) AS total_points FROM t_user WHERE gender = 1 AND is_delete = 0;
-- 3. AVG：求平均值（如统计用户平均年龄，假设新增age字段）
SELECT AVG(age) AS avg_age FROM t_user WHERE is_delete = 0;
Java后端关联
通过MyBatis的select标签执行聚合查询，将结果映射为Java的Integer（COUNT/SUM）、BigDecimal（AVG）类型，用于统计接口返回（如{"userCount": 100, "avgAge": 25.5}）。
5.2.4 条件查询进阶（AND/OR/BETWEEN/IN）
适配Java后端多条件筛选场景（如用户列表多条件搜索），核心是精准拼接条件，避免逻辑错误。
-- 1. AND：多条件同时满足（如查询性别为男且手机号以139开头的用户）
SELECT id, username, phone FROM t_user 
WHERE gender = 1 AND phone LIKE '139%' AND is_delete = 0;
-- 2. OR：满足任一条件（如查询性别为男或手机号以138开头的用户）
SELECT id, username, phone FROM t_user 
WHERE (gender = 1 OR phone LIKE '138%') AND is_delete = 0;  -- 括号区分优先级
-- 3. BETWEEN：范围查询（如查询创建时间在2024-01-01到2024-12-31的用户）
SELECT id, username, create_time FROM t_user 
WHERE create_time BETWEEN '2024-01-01' AND '2024-12-31' AND is_delete = 0;
-- 4. IN：指定多个值（如查询id为1、3、5的用户）
SELECT id, username FROM t_user 
WHERE id IN (1,3,5) AND is_delete = 0;
-- 5. IS NULL/IS NOT NULL（查询手机号为空/不为空的用户）
SELECT id, username FROM t_user 
WHERE phone IS NULL AND is_delete = 0;
避坑点
AND优先级高于OR，多条件组合时需加括号，避免逻辑错误（如上述OR条件需加括号）；
BETWEEN包含边界值，查询时间范围时需注意结束时间（如查询2024年数据，结束时间设为2024-12-31 23:59:59）；
IN适合少量值（如10个以内），值过多时效率低，可改用JOIN查询（后续章节详解）。
5.3 单表操作企业级规范（Java后端必守）
规范是避免业务异常、提升可维护性的关键，以下规范贯穿Java后端开发全流程，必须严格遵守。
命名规范：SQL语句中，表名、字段名全部小写，下划线分隔，与Java实体类属性名保持一致（如Java的userId对应表的user_id，需注意MyBatis驼峰映射）；
SQL语句规范：关键字（SELECT、INSERT、UPDATE等）全部大写，便于区分；每条SQL结尾加分号；复杂SQL换行排版，提升可读性（如上述批量新增、多条件查询）；
逻辑删除规范：所有核心表必须加is_delete字段，所有CRUD操作必须包含is_delete=0的筛选（除逻辑删除本身）；
性能规范：
禁止SELECT * 查询，指定所需字段；
分页查询必用LIMIT，搭配ORDER BY；
主键查询优先，避免频繁模糊查询、大范围IN查询；
安全规范：
密码必须加密存储（MD5、SHA256等），禁止明文插入；
避免SQL注入（Java后端通过MyBatis参数绑定，禁止拼接SQL字符串）；
生产环境中，后端无DELETE、DROP权限，仅拥有SELECT、INSERT、UPDATE权限。
5.4 本章实战练习（Java后端视角）
目标：基于db_ecommerce数据库的t_user表，完成以下操作，模拟Java后端业务场景，巩固单表CRUD及高级技巧。
插入3条测试用户数据（密码用MD5加密，手机号不重复）；
查询所有未删除用户，按创建时间倒序，分页显示（第1页，每页2条）；
修改id为2的用户的手机号和性别；
逻辑删除id为3的用户；
统计未删除用户的总数，以及性别为男的用户数量；
查询手机号以139开头且未删除的用户信息。
提示：练习时，可模拟Java后端接口开发思路，思考每条SQL对应的业务场景（如插入对应注册、修改对应编辑），以及如何与Java实体类、MyBatis映射结合。
5.5 本章小结（Java后端重点）
单表操作核心是CRUD，对应Java后端的注册、登录、编辑、注销等核心接口，所有操作需贴合业务场景；
查询是高频操作，重点掌握分页、排序、多条件筛选，禁止SELECT *，必加逻辑删除筛选；
修改、删除操作必须加WHERE条件（优先用主键），避免误操作，生产环境禁止物理删除；
牢记企业级规范，避免语法错误、性能隐患，为后续MyBatis框架映射、接口开发铺垫；
后续章节：讲解SQL高级查询（多表关联）、索引，进一步提升Java后端接口性能。

