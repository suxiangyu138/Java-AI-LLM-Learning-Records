MySQL理论+企业级实战（Java后端入门版-第二章）
第二章：MySQL数据库基本操作（Java后端实战核心）
核心说明：数据库基本操作是Java后端开发的必备技能，所有业务接口（登录、注册、下单等）都依赖这些操作。本章以“用户管理”场景的db_user_manage数据库和t_user表为基础，讲解“数据库操作、表操作、数据操作”三大核心，全程贴合企业级开发规范，避免入门级冗余操作，重点标注Java后端实战注意点（注：db_user_manage数据库和t_user表可参考基础入门操作创建）。
2.1 数据库操作（核心：增、查、删、切换）
数据库操作主要针对“数据库本身”，常用操作包括：创建数据库、查询数据库、删除数据库、切换数据库，Java后端开发中，创建数据库通常由运维或开发负责人执行，后端工程师主要负责“切换数据库”和“查询数据库信息”（排查环境问题）。
2.1.1 核心操作语法（企业级规范版）
-- 1. 创建数据库（企业级必加字符集和排序规则，避免乱码）
-- IF NOT EXISTS：避免重复创建报错（生产环境必备）
CREATE DATABASE IF NOT EXISTS 数据库名 
CHARACTER SET utf8mb4  -- 字符集，企业级固定utf8mb4
COLLATE utf8mb4_general_ci;  -- 排序规则，适配中文查询
-- 示例：创建电商数据库
CREATE DATABASE IF NOT EXISTS db_ecommerce CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- 2. 查询所有数据库（排查环境，确认数据库是否存在）
SHOW DATABASES;
-- 3. 查询当前正在使用的数据库（Java后端排查连接异常常用）
SELECT DATABASE();
-- 4. 切换数据库（后续表操作、数据操作，必须先切换到目标数据库）
USE 数据库名;
-- 示例：切换到用户管理数据库
USE db_user_manage;
-- 5. 删除数据库（高危操作！生产环境禁止直接执行，需审批）
-- IF EXISTS：避免删除不存在的数据库报错
DROP DATABASE IF EXISTS 数据库名;
-- 示例：删除测试用的数据库（仅本地测试使用）
DROP DATABASE IF EXISTS db_test;
2.1.2 企业级实战注意事项（Java后端重点）
禁止直接删除数据库：生产环境中，数据库删除需走审批流程，且删除前必须备份数据（后续讲解备份操作），避免误删导致数据丢失（后端开发无删除数据库权限，通常由运维负责）。
字符集固定：无论什么项目，创建数据库时必须指定utf8mb4字符集，避免中文、表情符号乱码（Java后端接收前端传参时，也需统一编码，与数据库保持一致）。
切换数据库：Java代码中，通过连接池配置指定数据库（无需手动执行USE语句），但排查问题时（如Navicat连接、命令行调试），必须确认当前使用的数据库是否正确，否则会出现“表不存在”报错。
2.2 表操作（核心：增、查、改、删）
表操作是Java后端开发的高频操作（如需求迭代时新增字段、修改字段类型），核心围绕“表的结构”操作，常用操作包括：创建表、查询表结构、修改表、删除表，全程以t_user表为示例，贴合Java实体类设计。
2.2.1 1. 创建表（CREATE TABLE）
理论核心：创建表时，需指定“表名、字段名、字段类型、字段约束”，企业级表设计需遵循“字段精简、约束明确”原则，与Java实体类一一对应。
-- 基本语法（企业级规范）
CREATE TABLE IF NOT EXISTS 表名 (
    字段名1 数据类型 约束条件,
    字段名2 数据类型 约束条件,
    ...
    字段名n 数据类型 约束条件
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;  -- 引擎和字符集固定
-- 示例1：创建商品表（t_goods），适配Java实体类Goods
CREATE TABLE IF NOT EXISTS t_goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 主键自增，对应Java Long
    goods_name VARCHAR(100) NOT NULL,      -- 商品名称，非空，对应Java String
    price DECIMAL(10,2) NOT NULL,          -- 商品价格，保留2位小数，对应Java BigDecimal
    stock INT NOT NULL DEFAULT 0,          -- 库存，非空，默认0，对应Java Integer
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间，对应Java LocalDateTime
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  -- 更新时间，自动更新
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 示例2：创建用户表（t_user），适配Java实体类User（基础必备表）
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 主键，自增，对应Java的Long
    username VARCHAR(50) NOT NULL UNIQUE,  -- 用户名，非空，唯一（避免重复注册），对应Java的String
    password VARCHAR(100) NOT NULL,       -- 密码，非空，存储加密后的密码，对应Java的String
    phone VARCHAR(20),                    -- 手机号，可空，对应Java的String
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP  -- 创建时间，默认当前时间
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
Java后端关联：上述表结构与Java实体类字段名、数据类型完全对应，后续通过MyBatis框架映射时，无需额外配置，减少数据映射异常。
2.2.2 2. 查询表操作（SHOW / DESC）
核心用途：排查表结构是否正确（如字段是否存在、数据类型是否匹配），Java后端调试时高频使用。
-- 1. 查询当前数据库中所有的表
SHOW TABLES;
-- 2. 查询表的详细结构（最常用，查看字段、类型、约束）
DESC 表名;
-- 示例：查看t_user表结构
DESC t_user;
-- 3. 查询创建表的完整SQL（排查表引擎、字符集、约束细节）
SHOW CREATE TABLE 表名;
-- 示例：查看t_goods表的创建语句
SHOW CREATE TABLE t_goods;
2.2.3 3. 修改表（ALTER TABLE）（高频实战）
Java后端开发中，需求迭代时经常需要修改表结构（如新增字段、修改字段类型、删除字段），核心语法为ALTER TABLE，需注意避免影响现有数据。
-- 前提：先切换到目标数据库
USE db_user_manage;
-- 1. 新增字段（最常用，如给t_user表新增“邮箱”字段）
ALTER TABLE t_user ADD COLUMN email VARCHAR(100) UNIQUE COMMENT '用户邮箱';
-- 说明：COMMENT 用于添加字段注释，方便团队协作，企业级必加
-- UNIQUE：邮箱唯一，避免重复绑定
-- 2. 修改字段类型（谨慎使用，避免数据丢失）
-- 示例：将t_user表的phone字段长度从20改为11（适配手机号规范）
ALTER TABLE t_user MODIFY COLUMN phone VARCHAR(11);
-- 3. 修改字段名（极少用，会影响Java代码映射，需同步修改实体类）
ALTER TABLE t_user CHANGE COLUMN phone user_phone VARCHAR(11);
-- 4. 删除字段（高危，需确认字段无业务依赖）
-- 示例：删除t_user表的email字段（仅测试使用）
ALTER TABLE t_user DROP COLUMN email;
-- 5. 修改表名（极少用，需同步修改Java实体类和映射配置）
ALTER TABLE t_user RENAME TO t_sys_user;
企业级注意：修改表结构时，需先备份表数据（后续讲解）；生产环境中，修改表结构需在低峰期执行（避免锁表，影响业务）；新增字段时，尽量设置默认值或允许为空（避免插入数据时报错）。
2.2.4 4. 删除表（DROP TABLE）（高危）
删除表会彻底删除表结构和所有数据，生产环境严格禁止直接执行，仅本地测试时使用。
-- 语法：避免删除不存在的表报错
DROP TABLE IF EXISTS 表名;
-- 示例：删除测试用的表
DROP TABLE IF EXISTS t_test;
Java后端视角：后端开发通常无删除表权限，仅在本地搭建测试环境时，会删除测试表；生产环境中，表删除需运维配合，且必须先备份数据。
2.3 数据操作（CRUD，Java后端核心实战）
数据操作是Java后端最核心的操作，对应业务中的“新增数据（注册）、查询数据（登录/列表）、修改数据（编辑）、删除数据（注销）”，即CRUD（Create、Read、Update、Delete），全程以t_user表为示例，贴合企业级业务场景。
2.3.1 1. 新增数据（INSERT，创建/注册场景）
核心用途：用户注册、下单、新增商品等场景，Java后端通过代码执行INSERT语句，将前端传参存入数据库。
-- 前提：切换到目标数据库，确认表存在
USE db_user_manage;
-- 方式1：指定所有字段（推荐，避免字段顺序变化导致报错）
INSERT INTO t_user (username, password, phone) 
VALUES ('lisi', '654321', '13900139000');
-- 方式2：不指定字段（不推荐，字段顺序变化会报错）
INSERT INTO t_user VALUES (null, 'wangwu', '111111', '13700137000', NOW());
-- 说明：id是自增字段，可传null，MySQL会自动生成自增ID
-- 方式3：批量新增（企业级高频，如批量导入用户，提升效率）
INSERT INTO t_user (username, password, phone)
VALUES 
('zhaoliu', '222222', '13600136000'),
('qianqi', '333333', '13500135000');
-- 企业级优化：新增时避免重复（结合唯一约束）
INSERT IGNORE INTO t_user (username, password, phone) 
VALUES ('zhangsan', '123456', '13800138000');
-- 说明：IGNORE 表示如果用户名重复（唯一约束），则忽略这条新增，不报错
Java后端实战：后端代码中，通过MyBatis的insert标签执行新增操作，参数从前端接收（如username、password），密码需先加密（后续讲解），再传入数据库，避免明文存储。
2.3.2 2. 查询数据（SELECT，登录/列表场景）
查询是Java后端最高频的操作（如用户登录查询、商品列表查询），核心语法为SELECT，可搭配条件、排序、分页，贴合企业级业务场景。
-- 1. 查询表中所有数据（本地测试用，生产环境禁止，数据量大时会卡顿）
SELECT * FROM t_user;
-- 2. 查询指定字段（推荐，减少数据传输，提升效率）
-- 示例：查询用户的id、用户名、手机号（适配用户列表接口）
SELECT id, username, phone FROM t_user;
-- 3. 带条件查询（最常用，如用户登录：根据用户名查询）
-- 示例1：查询用户名为zhangsan的用户（登录场景）
SELECT * FROM t_user WHERE username = 'zhangsan';
-- 示例2：查询手机号以138开头的用户
SELECT id, username, phone FROM t_user WHERE phone LIKE '138%';
-- 4. 排序查询（如用户列表按创建时间倒序）
SELECT * FROM t_user ORDER BY create_time DESC;  -- DESC：倒序，ASC：正序（默认）
-- 5. 分页查询（企业级高频，避免一次性查询所有数据）
-- 语法：LIMIT 起始索引, 每页条数（起始索引从0开始）
-- 示例：查询第1页，每页2条数据
SELECT * FROM t_user LIMIT 0, 2;
-- 示例：查询第2页，每页2条数据（起始索引=（页码-1）*每页条数）
SELECT * FROM t_user LIMIT 2, 2;
-- 6. 去重查询（避免重复数据）
SELECT DISTINCT phone FROM t_user;  -- 查询所有不重复的手机号
Java后端实战：用户登录时，后端代码执行SELECT * FROM t_user WHERE username = ?，将前端传入的用户名作为参数，查询数据库中的用户信息，再对比密码（加密后）；列表查询时，必用分页（避免数据量大导致接口超时），分页参数（页码、每页条数）从前端接收。
2.3.3 3. 修改数据（UPDATE，编辑场景）
核心用途：用户编辑个人信息、修改商品库存、更新订单状态等场景，必须加WHERE条件，否则会修改表中所有数据（高危）。
-- 语法：UPDATE 表名 SET 字段=值 WHERE 条件（条件必须加！）
-- 示例1：修改用户名为zhangsan的密码（重置密码场景）
UPDATE t_user SET password = '888888' WHERE username = 'zhangsan';
-- 示例2：修改id为1的用户的手机号和邮箱（编辑个人信息场景）
-- 先给t_user表新增email字段（参考2.2.3）
ALTER TABLE t_user ADD COLUMN email VARCHAR(100) UNIQUE COMMENT '用户邮箱';
UPDATE t_user SET phone = '13800000000', email = 'zhangsan@163.com' WHERE id = 1;
-- 示例3：批量修改（谨慎使用，如批量修改商品状态）
UPDATE t_goods SET stock = 0 WHERE id IN (1,2,3);  -- 修改id为1、2、3的商品库存为0
-- 禁止操作（无WHERE条件，会修改表中所有数据）
UPDATE t_user SET password = '123456';  -- 生产环境绝对禁止！
企业级注意：修改数据时，WHERE条件必须精准（如用主键id），避免误改其他数据；生产环境中，修改操作需记录日志（后续讲解），便于追溯；修改敏感数据（如密码、手机号），需验证用户权限。
2.3.4 4. 删除数据（DELETE，注销/删除场景）
删除数据分为“物理删除”和“逻辑删除”，企业级开发中优先使用“逻辑删除”（避免数据丢失，便于恢复），仅特殊场景使用“物理删除”。
-- 1. 物理删除（彻底删除数据，高危，需加WHERE条件）
-- 示例：删除id为5的用户（物理删除，无法恢复）
DELETE FROM t_user WHERE id = 5;
-- 2. 逻辑删除（企业级首选，仅标记删除，不删除实际数据）
-- 步骤1：给表新增“删除标记”字段（is_delete，0=未删除，1=已删除）
ALTER TABLE t_user ADD COLUMN is_delete TINYINT DEFAULT 0 COMMENT '删除标记：0未删除，1已删除';
-- 步骤2：删除时，仅修改is_delete字段（不删除数据）
UPDATE t_user SET is_delete = 1 WHERE id = 4;
-- 步骤3：查询时，过滤已删除的数据（Java后端接口必做）
SELECT * FROM t_user WHERE is_delete = 0;
-- 批量删除（逻辑删除，推荐）
UPDATE t_user SET is_delete = 1 WHERE id IN (6,7);
Java后端实战：用户注销功能，采用逻辑删除（修改is_delete为1），后续用户列表查询、登录查询时，都会过滤掉is_delete=1的数据；物理删除仅用于测试数据、无效数据清理，且需审批。
2.4 本章实战练习（Java后端视角）
目标：结合本章知识点，完成“用户管理”相关的基本操作，模拟Java后端业务场景，为后续Java代码操作数据库铺垫。
创建数据库db_test_manage，指定字符集utf8mb4；
在该数据库中创建t_student表，字段如下（对应Java实体类Student）：
id：BIGINT，主键自增；
student_name：VARCHAR(50)，非空；
age：INT，非空，默认0；
gender：VARCHAR(10)，可空；
create_time：DATETIME，默认当前时间；
is_delete：TINYINT，默认0（逻辑删除标记）。
向t_student表批量插入3条测试数据；
查询年龄大于18的学生信息，按创建时间倒序排列；
修改id为1的学生的年龄为20；
对id为3的学生执行逻辑删除；
查询未删除的学生信息，分页显示（第1页，每页2条）。
提示：练习时可结合可视化工具（Navicat、DBeaver）和命令行，模拟Java后端开发中“调试SQL”的场景，确保每一步操作符合企业级规范。
