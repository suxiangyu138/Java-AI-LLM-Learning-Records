03.20 12:51
MySQL数据类型与约束（Java后端企业级实战版）
第三章：MySQL数据类型与约束（Java后端核心基础）
核心说明：数据类型和约束是MySQL表设计的灵魂，直接决定数据存储的合理性、安全性和查询效率，更是Java后端实体类与数据库表映射的关键（避免数据类型不匹配导致的映射异常）。本章聚焦企业级开发中最常用的数据类型、核心约束，结合Java后端业务场景（如用户管理、商品管理）讲解，摒弃冗余的冷门知识点，重点标注实战注意点和避坑指南。
3.1 核心数据类型（企业级常用，对应Java类型）
MySQL支持多种数据类型，Java后端开发中无需记忆所有类型，重点掌握以下4大类常用类型，核心原则：按需选择、精准匹配（既不浪费存储空间，也不导致数据溢出或精度丢失），所有类型均适配MySQL 8.0版本。
关键关联：以下所有数据类型均对应Java实体类的具体类型，是后续MyBatis映射、接口开发的基础，必须牢记对应关系。
3.1.1 数值类型（存储数字：ID、年龄、价格、库存等）
核心用途：存储整数、小数，企业级开发中优先选择“最小适配类型”（如年龄用TINYINT，无需用INT），减少存储空间。
MySQL数据类型
取值范围
Java对应类型
企业级使用场景
实战注意点
TINYINT
0-255（无符号）；-128~127（有符号）
Integer
状态标记（如is_delete：0=未删，1=已删）、性别（1=男，2=女）、开关
企业级常用无符号（UNSIGNED），避免负数干扰；无需指定长度
INT
-2147483648~2147483647
Integer
年龄、库存、普通ID（数据量<2000万）
最常用的整数类型，无需指定长度（默认长度足够）
BIGINT
-9223372036854775808~9223372036854775807
Long
主键ID（自增，数据量≥2000万）、订单号、时间戳
Java后端主键首选类型，避免INT溢出（如电商订单号、用户ID）
DECIMAL(M,D)
M为总长度，D为小数位数（M≤65，D≤30）
BigDecimal
商品价格、金额、手续费（需精准计算，无精度丢失）
企业级固定写法：DECIMAL(10,2)（适配元角分），禁止用FLOAT/DOUBLE（精度丢失）
Java后端实战示例：商品价格字段（price）设计为DECIMAL(10,2)，对应Java实体类的BigDecimal类型，避免用Double导致的价格计算误差（如0.1+0.2≠0.3的问题）。
3.1.2 字符串类型（存储文本：用户名、手机号、邮箱等）
核心用途：存储文本信息，企业级开发中需精准控制长度（避免过长浪费空间，过短导致数据截断）。
MySQL数据类型
长度限制
Java对应类型
企业级使用场景
实战注意点
VARCHAR(M)
M为字符数（1≤M≤65535）
String
用户名（50位内）、手机号（11位）、邮箱（100位内）、地址（255位内）
最常用，可变长度（存储多少占多少空间），必须指定长度；手机号固定11位写VARCHAR(11)，不写INT（避免以0开头的手机号被截断）
CHAR(M)
M为字符数（1≤M≤255）
String
固定长度文本（如身份证号18位、手机号11位、性别2位）
固定长度（不足补空格），查询效率略高，仅用于长度固定的场景
TEXT
最大65535字符
String
长文本（如商品描述、用户备注、文章内容）
不适合作为查询条件（效率低），禁止用于主键；长文本（超过65535字符）用LONGTEXT（极少用）
企业级避坑：用户名用VARCHAR(50)（足够满足大多数场景），邮箱用VARCHAR(100)（适配长邮箱），禁止用TEXT存储短文本；手机号用VARCHAR(11)，不用INT（如手机号13800138000，INT无法存储，会溢出）。
3.1.3 日期时间类型（存储时间：创建时间、更新时间等）
核心用途：存储时间相关数据，企业级开发中统一规范，避免时间格式混乱，便于排序和查询。
MySQL数据类型
格式
Java对应类型
企业级使用场景
实战注意点
DATETIME
YYYY-MM-DD HH:MM:SS（范围1000-01-01~9999-12-31）
LocalDateTime
创建时间（create_time）、更新时间（update_time）、订单时间
最常用，无时区限制，企业级表必加字段；可设置默认值CURRENT_TIMESTAMP
DATE
YYYY-MM-DD
LocalDate
出生日期、商品生产日期、日期筛选
仅存储日期，不存储时间，适合只需要日期的场景
TIMESTAMP
YYYY-MM-DD HH:MM:SS（范围1970-01-01~2038-01-19）
LocalDateTime
时间戳记录（如登录时间）
有时间范围限制，企业级很少用（避免2038年溢出），优先用DATETIME
Java后端实战规范：所有表统一添加create_time（创建时间）和update_time（更新时间）字段，设置默认值：
-- 创建时间：默认当前时间，插入时自动填充
create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
-- 更新时间：默认当前时间，修改数据时自动更新
update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
3.1.4 特殊类型（企业级高频）
仅讲解Java后端开发中高频使用的2种特殊类型，其余冷门类型（如ENUM、SET）了解即可。
BOOLEAN：等价于TINYINT(1)，取值0（false）、1（true），对应Java的Boolean类型，用于存储开关、状态（如is_enable：是否启用）。
JSON：MySQL 8.0新增类型，用于存储JSON格式数据（如用户扩展信息、商品规格），对应Java的String或JSONObject类型，适合存储非固定结构的文本（避免创建过多字段）。
-- 示例：JSON类型字段（存储用户扩展信息）
ALTER TABLE t_user ADD COLUMN ext_info JSON COMMENT '用户扩展信息（如爱好、职业）';
-- 插入JSON数据
INSERT INTO t_user (username, password, ext_info) 
VALUES ('zhangsan', '123456', '{"hobby":"game","job":"developer"}');
3.2 核心约束（企业级表设计必备，保证数据完整性）
约束是对表中字段的限制，用于保证数据的正确性、唯一性和完整性，避免无效数据、重复数据进入数据库，是Java后端业务逻辑的基础（如避免重复注册、避免空用户名）。企业级开发中，重点掌握5种核心约束，无需记忆冷门约束。
3.2.1 主键约束（PRIMARY KEY，PK）
核心作用：唯一标识表中的每一条记录，相当于“身份证号”，保证记录不重复、不缺失，每张表必须有且只有一个主键。
-- 方式1：创建表时指定主键（自增主键，企业级首选）
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 主键自增，Java对应Long
    username VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 方式2：联合主键（极少用，不推荐，Java后端映射复杂）
CREATE TABLE t_student_course (
    student_id BIGINT,
    course_id BIGINT,
    PRIMARY KEY (student_id, course_id)  -- 两个字段联合作为主键
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
企业级实战规范：
主键首选BIGINT类型+自增（AUTO_INCREMENT），避免用INT（数据量大会溢出）、UUID（查询效率低）。
主键字段名统一为id，禁止用其他名称（如user_id、goods_id），便于团队协作和框架映射。
禁止用业务字段作为主键（如手机号、用户名），避免业务字段修改导致主键失效。
3.2.2 非空约束（NOT NULL，NN）
核心作用：限制字段的值不能为NULL（空），避免无效数据（如用户名、密码不能为空），是企业级表设计的基础约束。
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,  -- 非空，用户名不能为空
    password VARCHAR(100) NOT NULL, -- 非空，密码不能为空
    phone VARCHAR(11)  -- 可空，手机号非必填
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
企业级注意：
核心业务字段（用户名、密码、商品名称、价格）必须加非空约束，非核心字段（手机号、邮箱）可空。
如果字段非空但可能暂时无值，可设置默认值（如age INT NOT NULL DEFAULT 0），避免插入数据时报错。
3.2.3 唯一约束（UNIQUE，UK）
核心作用：限制字段的值在表中唯一，避免重复数据（如用户名、手机号、邮箱不能重复，避免重复注册）。
-- 方式1：创建表时指定唯一约束
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,  -- 唯一+非空，用户名不能重复
    phone VARCHAR(11) UNIQUE,  -- 唯一，手机号不能重复（可空，但不为空时必须唯一）
    email VARCHAR(100) UNIQUE COMMENT '邮箱唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 方式2：给已有表添加唯一约束（需求迭代时用）
ALTER TABLE t_user ADD UNIQUE (email);
企业级避坑：
唯一约束允许字段值为NULL，且可以有多个NULL（NULL不等于任何值，包括自身）。
联合唯一约束：多个字段组合起来唯一（如student_id和course_id联合唯一，避免重复选同一门课）。
新增数据时，可搭配INSERT IGNORE语句，避免唯一约束冲突报错（如重复注册时忽略操作）。
3.2.4 外键约束（FOREIGN KEY，FK）
核心作用：建立两个表之间的关联关系，保证关联数据的完整性（如订单表的user_id必须对应用户表的id，避免出现不存在的用户订单），是关系型数据库的核心特性。
示例：用户表（t_user）和订单表（t_order），订单表的user_id关联用户表的id：
-- 1. 先创建主表（被关联的表，用户表）
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 2. 创建从表（关联主表的表，订单表），添加外键约束
CREATE TABLE t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,  -- 订单号
    user_id BIGINT NOT NULL,  -- 关联用户表的id
    total_price DECIMAL(10,2) NOT NULL,
    -- 外键约束：user_id关联t_user表的id
    FOREIGN KEY (user_id) REFERENCES t_user(id)
    -- 可选：删除/修改主表数据时，从表的处理方式（企业级常用）
    ON DELETE CASCADE  -- 主表用户删除，从表关联的订单也删除
    ON UPDATE CASCADE  -- 主表用户id修改，从表关联的user_id也同步修改
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
企业级实战注意：
外键必须关联主表的主键（或唯一约束字段），且数据类型必须完全一致（如主表id是BIGINT，从表user_id也必须是BIGINT）。
ON DELETE / ON UPDATE 配置（避免关联数据异常）：
CASCADE：主表操作，从表同步操作（推荐，如删除用户，同步删除其订单）；
RESTRICT：禁止操作（如主表有关联数据，禁止删除主表记录）；
SET NULL：主表操作，从表关联字段设为NULL（需保证从表关联字段可空）。
高并发场景（如电商订单），可暂时不建外键（减少数据库压力），由Java后端代码保证关联完整性（手动校验user_id是否存在）。
3.2.5 默认值约束（DEFAULT）
核心作用：字段未插入值时，自动填充默认值，避免字段为空，减少Java后端代码的赋值操作。
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    age INT NOT NULL DEFAULT 0,  -- 默认值0，未插入时自动填充
    is_delete TINYINT NOT NULL DEFAULT 0,  -- 默认未删除
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP  -- 默认当前时间
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
企业级规范：
状态字段（is_delete、is_enable）默认值设为0（未删除、未启用）；
数值字段（age、stock）默认值设为0；
时间字段（create_time）默认值设为CURRENT_TIMESTAMP（插入时自动填充当前时间）。
3.3 企业级表设计实战（综合数据类型与约束）
结合本章知识点，设计一个电商项目的“商品表（t_goods）”，贴合Java后端实体类，满足企业级规范：
CREATE TABLE IF NOT EXISTS t_goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品主键ID',
    goods_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    goods_code VARCHAR(50) NOT NULL UNIQUE COMMENT '商品编码（唯一）',
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '商品库存',
    category_id BIGINT NOT NULL COMMENT '商品分类ID（关联分类表）',
    is_enable TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用',
    goods_desc TEXT COMMENT '商品描述',
    ext_info JSON COMMENT '商品扩展信息（如规格、颜色）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 外键约束：关联商品分类表（t_category）的id
    FOREIGN KEY (category_id) REFERENCES t_category(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '商品表';
设计说明：
数据类型：id用BIGINT，price用DECIMAL(10,2)，goods_name用VARCHAR(100)，贴合Java实体类；
约束：主键、非空、唯一、外键、默认值齐全，保证数据完整性；
注释：每个字段、每张表都加COMMENT，便于团队协作（企业级必做）。
3.4 本章实战练习（Java后端视角）
目标：巩固数据类型与约束的使用，设计符合企业级规范的表，为后续Java代码映射铺垫。
创建数据库db_ecommerce，字符集utf8mb4；
创建t_category（商品分类表），字段如下，添加对应约束：
id：BIGINT，主键自增；
category_name：VARCHAR(50)，非空、唯一；
sort：INT，非空，默认0（排序）；
create_time：DATETIME，默认当前时间。
创建t_user（用户表），结合本章知识点，添加必要的约束和合适的数据类型，包含用户名、密码、手机号、邮箱、删除标记、创建/更新时间；
给t_user表新增“性别”字段（gender），数据类型选合适的类型，设置默认值；
创建t_order（订单表），关联t_user表的id，添加外键约束，包含订单号、用户ID、订单金额、创建时间。
3.5 本章小结（Java后端重点）
数据类型：牢记4大类常用类型与Java的对应关系，核心是“按需选择、精准匹配”，避免精度丢失和空间浪费；
核心约束：主键（必加）、非空（核心字段必加）、唯一（去重）、外键（关联表）、默认值（减少代码操作），缺一不可；
企业级规范：字段命名小写下划线、加注释、数据类型适配Java实体类、约束齐全，避免冷门用法；
后续衔接：下一章讲解SQL高级查询，结合本章表设计，实现复杂业务查询（如关联查询、聚合查询）。

