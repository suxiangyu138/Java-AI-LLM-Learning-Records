03.20 13:04
MySQL用户与权限（Java后端企业级实战版）
第七章：MySQL用户与权限（Java后端核心实战）
核心说明：Java后端项目中，权限管理是保障系统安全的核心（如管理员、普通用户、游客的操作权限区分），而MySQL用户与权限是底层安全支撑——不仅要控制“谁能访问数据库”，还要限制“能执行哪些操作”（如普通后端账号只能查询、新增，禁止删除、修改表结构）。本章衔接前文多表操作、数据库设计知识点，先讲解MySQL原生用户与权限管理（底层基础），再重点讲解Java后端实战中“用户-角色-权限”体系的数据库设计与多表操作，全程贴合企业级开发规范，兼顾安全性与实操性。
前置基础：已掌握多表关联操作、数据库设计的实体关系（多对多），明确“用户-角色-权限”的核心逻辑：一个用户可拥有多个角色，一个角色可拥有多个权限（多对多关系），需通过中间表实现关联，这也是Java后端权限管理的通用架构。
7.1 基础：MySQL原生用户与权限管理（底层支撑）
Java后端开发中，我们通常不会直接操作MySQL原生用户，但需了解其核心逻辑——后端项目部署时，会创建专用的MySQL账号（如db_ecommerce_user），分配最小权限（仅操作项目对应的数据库），避免root账号泄露导致系统安全风险。以下操作均为企业级部署必备，无需频繁操作，但必须掌握。
7.1.1 MySQL用户管理（创建、修改、删除）
MySQL用户信息存储在系统数据库mysql的user表中，核心操作是“创建项目专用账号”，避免使用root账号直接连接项目。
-- 1. 创建MySQL用户（企业级实战：项目专用账号，仅允许本地/指定IP访问）
-- 语法：CREATE USER '用户名'@'访问IP' IDENTIFIED BY '密码';
CREATE USER 'db_ecommerce_user'@'localhost' IDENTIFIED BY 'Ecommerce@123';  -- 仅本地访问
CREATE USER 'db_ecommerce_user'@'%' IDENTIFIED BY 'Ecommerce@123';  -- 允许所有IP访问（生产环境谨慎使用）
-- 2. 修改用户密码（密码泄露时使用）
ALTER USER 'db_ecommerce_user'@'localhost' IDENTIFIED BY 'Ecommerce@456';
-- 3. 删除用户（项目下线时使用）
DROP USER 'db_ecommerce_user'@'localhost';
-- 4. 查看所有MySQL用户
SELECT user, host FROM mysql.user;
企业级规范
用户名规范：前缀+项目名（如db_ecommerce_user，db表示database，ecommerce表示电商项目）；
密码规范：包含大小写字母、数字、特殊符号（如Ecommerce@123），禁止简单密码（如123456）；
访问权限：生产环境优先设置“仅本地访问”或“仅项目服务器IP访问”，禁止%（所有IP）访问，降低泄露风险。
7.1.2 MySQL权限分配（核心：最小权限原则）
企业级开发中，遵循“最小权限原则”——给项目专用账号分配“仅能完成业务所需的权限”，禁止分配超级权限（如root的ALL PRIVILEGES），避免误操作或泄露导致的数据库风险。
-- 1. 分配权限（给电商项目账号分配db_ecommerce数据库的所有操作权限）
-- 语法：GRANT 权限列表 ON 数据库.表 TO '用户名'@'访问IP';
GRANT SELECT, INSERT, UPDATE ON db_ecommerce.* TO 'db_ecommerce_user'@'localhost';
-- 说明：db_ecommerce.* 表示db_ecommerce数据库的所有表；权限仅包含查询、新增、修改（禁止删除、建表）
-- 2. 分配全部权限（仅测试环境使用，生产环境禁止）
GRANT ALL PRIVILEGES ON db_ecommerce.* TO 'db_ecommerce_user'@'localhost';
-- 3. 撤销权限（权限分配错误时使用）
REVOKE DELETE ON db_ecommerce.* FROM 'db_ecommerce_user'@'localhost';
-- 4. 查看用户权限
SHOW GRANTS FOR 'db_ecommerce_user'@'localhost';
-- 5. 刷新权限（分配/撤销权限后必须执行，否则不生效）
FLUSH PRIVILEGES;
Java后端关联实战
项目配置文件（如application.yml）中，数据库连接参数使用上述创建的专用账号（db_ecommerce_user），而非root账号，确保即使账号泄露，也不会影响其他数据库；生产环境中，后端账号禁止拥有DELETE、DROP、ALTER等高危权限，避免误删数据、修改表结构。
7.2 实战：Java后端“用户-角色-权限”体系设计（核心）
MySQL原生用户权限是“数据库级”的控制，而Java后端的权限管理是“业务级”的控制（如管理员能删除商品、普通用户只能查看商品），核心是“用户-角色-权限”三层架构，通过数据库表设计实现，全程贴合多表关联操作知识点。
核心逻辑（多对多关系）：
用户（t_user）与角色（t_role）：多对多 → 中间表t_user_role关联；
角色（t_role）与权限（t_permission）：多对多 → 中间表t_role_permission关联。
以下基于db_ecommerce数据库，设计完整的权限体系表结构，适配Java后端权限接口开发（如用户角色分配、权限校验）。
7.2.1 权限体系表设计（4张核心表）
结合前文t_user表（用户表），新增3张表（角色表、权限表、用户角色中间表、角色权限中间表），遵循企业级规范，添加通用字段和约束。
-- 1. 角色表（t_role）：存储角色信息（如管理员、普通用户）
CREATE TABLE IF NOT EXISTS t_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色主键ID，对应Java Long',
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称（如admin、user），对应Java String',
    role_desc VARCHAR(255) COMMENT '角色描述（如系统管理员、普通用户），对应Java String',
    is_enable TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用，对应Java Boolean',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，对应Java LocalDateTime',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，对应Java LocalDateTime'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '角色表';
-- 2. 权限表（t_permission）：存储权限信息（如查询商品、删除商品）
CREATE TABLE IF NOT EXISTS t_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限主键ID，对应Java Long',
    perm_name VARCHAR(50) NOT NULL UNIQUE COMMENT '权限名称（如goods:select、goods:delete），对应Java String',
    perm_desc VARCHAR(255) COMMENT '权限描述（如查询商品、删除商品），对应Java String',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，对应Java LocalDateTime',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，对应Java LocalDateTime'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '权限表';
-- 3. 用户角色中间表（t_user_role）：关联用户与角色（多对多）
CREATE TABLE IF NOT EXISTS t_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '中间表主键ID，对应Java Long',
    user_id BIGINT NOT NULL COMMENT '用户ID，关联t_user.id，对应Java Long',
    role_id BIGINT NOT NULL COMMENT '角色ID，关联t_role.id，对应Java Long',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，对应Java LocalDateTime',
    -- 唯一约束：一个用户不能重复关联同一个角色
    UNIQUE KEY uk_user_role (user_id, role_id),
    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户角色中间表';
-- 4. 角色权限中间表（t_role_permission）：关联角色与权限（多对多）
CREATE TABLE IF NOT EXISTS t_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '中间表主键ID，对应Java Long',
    role_id BIGINT NOT NULL COMMENT '角色ID，关联t_role.id，对应Java Long',
    perm_id BIGINT NOT NULL COMMENT '权限ID，关联t_permission.id，对应Java Long',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，对应Java LocalDateTime',
    -- 唯一约束：一个角色不能重复关联同一个权限
    UNIQUE KEY uk_role_perm (role_id, perm_id),
    -- 外键约束
    FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (perm_id) REFERENCES t_permission(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '角色权限中间表';
7.2.2 插入基础测试数据（适配后续操作）
插入角色、权限及关联数据，模拟企业级权限分配场景（管理员拥有所有权限，普通用户仅拥有查询权限）。
-- 插入角色
INSERT INTO t_role (role_name, role_desc)
VALUES ('admin', '系统管理员，拥有所有权限'), ('user', '普通用户，仅拥有查询权限');
-- 插入权限
INSERT INTO t_permission (perm_name, perm_desc)
VALUES 
('goods:select', '查询商品'),
('goods:insert', '新增商品'),
('goods:update', '修改商品'),
('goods:delete', '删除商品'),
('order:select', '查询订单');
-- 关联角色与权限（admin拥有所有权限，user仅拥有查询权限）
INSERT INTO t_role_permission (role_id, perm_id)
VALUES 
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),  -- admin关联所有权限
(2, 1), (2, 5);  -- user关联查询商品、查询订单权限
-- 关联用户与角色（zhangsan（id=1）为admin，lisi（id=2）为user）
INSERT INTO t_user_role (user_id, role_id)
VALUES (1, 1), (2, 2);
7.3 权限体系多表关联操作（Java后端高频）
Java后端权限管理接口（如获取用户拥有的角色、获取用户拥有的权限、分配角色），本质都是多表关联查询、新增、修改操作，衔接前文多表操作知识点，重点掌握以下实战场景。
7.3.1 高频场景1：查询用户拥有的角色（用户详情接口核心）
业务场景：用户登录后，后端需查询该用户拥有的角色（如admin、user），用于后续权限判断，关联t_user、t_user_role、t_role三张表。
-- 查询id=1的用户（zhangsan）拥有的角色
SELECT 
    u.id AS user_id,
    u.username,
    r.id AS role_id,
    r.role_name,
    r.role_desc
FROM t_user u
INNER JOIN t_user_role ur ON u.id = ur.user_id
INNER JOIN t_role r ON ur.role_id = r.id
WHERE u.id = 1 AND u.is_delete = 0 AND r.is_enable = 1;
Java后端关联
通过MyBatis的resultMap，将用户信息和角色列表映射到UserVO实体类（包含List<Role> roles属性），用于用户详情接口返回，供前端展示用户角色。
7.3.2 高频场景2：查询用户拥有的所有权限（权限校验核心）
业务场景：用户执行操作（如删除商品）时，后端需校验该用户是否拥有对应权限（goods:delete），需关联4张表，查询用户的所有权限。
-- 查询id=1的用户（admin）拥有的所有权限
SELECT DISTINCT p.perm_name, p.perm_desc
FROM t_user u
INNER JOIN t_user_role ur ON u.id = ur.user_id
INNER JOIN t_role_permission rp ON ur.role_id = rp.role_id
INNER JOIN t_permission p ON rp.perm_id = p.id
WHERE u.id = 1 AND u.is_delete = 0 AND rp.role_id IS NOT NULL;
Java后端实战技巧
权限校验逻辑：后端接口拦截器中，获取当前登录用户的权限列表，判断请求的操作（如/goods/delete）是否在权限列表中，无权限则返回异常；
使用DISTINCT去重：避免用户拥有多个角色时，重复查询到相同的权限。
7.3.3 高频场景3：给用户分配角色（管理员操作接口）
业务场景：管理员给普通用户分配角色（如将lisi从user改为admin），本质是操作中间表t_user_role，新增/删除关联关系（多表关联新增/删除）。
-- 1. 给id=2的用户（lisi）分配admin角色（新增关联关系）
INSERT IGNORE INTO t_user_role (user_id, role_id)
VALUES (2, 1);  -- IGNORE避免重复分配（唯一约束）
-- 2. 撤销id=2的用户的user角色（删除关联关系）
DELETE FROM t_user_role
WHERE user_id = 2 AND role_id = 2;
避坑点
分配角色时，需先判断角色是否启用（is_enable=1），禁止给用户分配禁用角色；删除关联关系时，仅删除中间表数据，不删除用户、角色本身（避免误删核心数据）。
7.3.4 高频场景4：给角色分配权限（管理员操作接口）
业务场景：管理员给角色分配权限（如给user角色新增修改商品权限），操作中间表t_role_permission，新增关联关系。
-- 给user角色（id=2）分配修改商品权限（perm_id=3）
INSERT IGNORE INTO t_role_permission (role_id, perm_id)
VALUES (2, 3);
-- 撤销user角色的修改商品权限
DELETE FROM t_role_permission
WHERE role_id = 2 AND perm_id = 3;
7.4 权限体系企业级规范（Java后端必守）
权限管理是系统安全的核心，以下规范贯穿Java后端开发全流程，结合MySQL表设计和多表操作，避免权限泄露、权限混乱等问题。
表设计规范：
权限名称（perm_name）采用“模块:操作”格式（如goods:select、order:delete），便于Java后端权限校验（如拦截器匹配请求路径）；
中间表必须添加唯一约束（uk_user_role、uk_role_perm），避免重复关联（如一个用户重复关联同一个角色）；
角色表添加is_enable字段，支持角色禁用（禁用后，关联该角色的用户无法获得对应权限）。
权限分配规范：
遵循“最小权限原则”：给用户分配的角色，仅包含其业务所需的权限（如普通用户无需删除权限）；
管理员角色（admin）仅分配给系统管理员，禁止给普通用户分配；
权限分配操作（新增/删除关联），需记录操作日志（如谁分配的角色、分配时间），便于追溯。
多表操作规范：
查询用户权限时，必须关联所有相关表，加逻辑删除筛选（u.is_delete=0），避免查询到已删除用户的权限；
删除角色/权限时，通过外键ON DELETE CASCADE，自动删除中间表的关联数据（无需手动删除）；
禁止直接删除用户、角色、权限表的核心数据，优先用逻辑删除（可新增is_delete字段）。
安全规范：
MySQL原生账号仅分配最小权限，禁止root账号连接项目；
Java后端权限校验必须在后端实现（如拦截器），禁止仅依赖前端隐藏按钮（前端可绕过）；
敏感操作（如分配管理员角色），需二次校验管理员权限，避免越权操作。
7.5 本章实战练习（Java后端视角）
目标：基于权限体系4张核心表，完成以下操作，模拟Java后端权限管理业务场景，巩固多表关联操作和权限设计规范。
查询id=2的用户（lisi）拥有的角色和所有权限；
给id=3的新增用户（假设已插入）分配user角色，并查询该用户的权限；
给user角色（id=2）新增“新增商品”权限（perm_id=2），并验证权限分配结果；
撤销id=1的用户（zhangsan）的“删除商品”权限；
查询所有启用的角色，以及每个角色拥有的权限列表；
禁用admin角色（id=1），查询id=1的用户是否还能获取到admin角色的权限。
提示：练习时，结合Java后端权限接口开发思路，思考每条操作对应的业务场景（如权限分配对应“角色管理”接口），以及如何通过MyBatis实现多表字段映射、权限校验逻辑。
7.6 本章小结（Java后端重点）
MySQL原生用户与权限是底层安全支撑，核心是“创建项目专用账号、分配最小权限”，禁止使用root账号连接项目；
Java后端权限管理的核心是“用户-角色-权限”三层架构，通过4张表（用户表、角色表、权限表、两个中间表）实现多对多关联，贴合前文多表操作知识点；
高频操作：查询用户角色、查询用户权限、分配角色/权限，本质都是多表关联查询、新增、删除，需遵循“先主后从、逻辑删除”的原则；
牢记最小权限原则和企业级规范，避免权限泄露、越权操作，保障系统安全；
后续章节：讲解索引（提升权限查询效率）、事务（保证权限分配的原子性），进一步完善Java后端权限管理体系，适配高并发场景。

