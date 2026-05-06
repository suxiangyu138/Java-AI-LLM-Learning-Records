# MySQL 用户与权限（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MySQL 用户与权限管理  
> **核心原则**：最小权限原则（Least Privilege）  
> **前置基础**：MySQL 多表操作、数据库设计（多对多关系）

---

## 一、核心概念

### 1.1 两层权限体系

| 层级 | 控制粒度 | 工具 | 核心目标 |
|------|----------|------|----------|
| **数据库层** | 谁能访问数据库、执行什么操作 | MySQL 原生用户管理 | 项目专用账号，禁止 root 直连 |
| **业务层** | 管理员/普通用户的操作权限 | "用户-角色-权限"表设计 | 接口级权限控制 |

### 1.2 核心逻辑（多对多关系）

```
用户（t_user） ←→ 角色（t_role）         ：多对多 → t_user_role 中间表
角色（t_role） ←→ 权限（t_permission）   ：多对多 → t_role_permission 中间表
```

---

## 二、底层原理

### 2.1 MySQL 用户存储机制

MySQL 用户信息存储在系统数据库 `mysql` 的 `user` 表中，通过 `host + user` 组合唯一标识一个用户。

### 2.2 最小权限原则

- 项目专用账号只分配业务所需权限（`SELECT`、`INSERT`、`UPDATE`）
- 禁止分配 `ALL PRIVILEGES`（超级权限）
- 生产环境禁止 `DELETE`、`DROP`、`ALTER` 等高危权限

---

## 三、代码实现

### 3.1 MySQL 原生用户管理

```sql
-- 1. 创建项目专用账号（仅本地访问）
CREATE USER 'db_ecommerce_user'@'localhost' IDENTIFIED BY 'Ecommerce@123';

-- 2. 创建允许远程访问的用户（生产环境谨慎使用 %）
CREATE USER 'db_ecommerce_user'@'%' IDENTIFIED BY 'Ecommerce@123';

-- 3. 修改密码
ALTER USER 'db_ecommerce_user'@'localhost' IDENTIFIED BY 'Ecommerce@456';

-- 4. 删除用户（项目下线时）
DROP USER 'db_ecommerce_user'@'localhost';

-- 5. 查看所有用户
SELECT user, host FROM mysql.user;
```

### 3.2 权限分配（GRANT / REVOKE）

```sql
-- 分配权限：只给查询、新增、修改
GRANT SELECT, INSERT, UPDATE ON db_ecommerce.* TO 'db_ecommerce_user'@'localhost';

-- 分配全部权限（仅测试环境）
GRANT ALL PRIVILEGES ON db_ecommerce.* TO 'db_ecommerce_user'@'localhost';

-- 撤销 DELETE 权限
REVOKE DELETE ON db_ecommerce.* FROM 'db_ecommerce_user'@'localhost';

-- 查看用户权限
SHOW GRANTS FOR 'db_ecommerce_user'@'localhost';

-- 刷新权限（修改后必须执行）
FLUSH PRIVILEGES;
```

### 3.3 权限体系表设计

#### 角色表（`t_role`）

```sql
CREATE TABLE IF NOT EXISTS t_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色主键ID',
    role_name   VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称（如admin、user）',
    role_desc   VARCHAR(255) COMMENT '角色描述',
    is_enable   TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
```

#### 权限表（`t_permission`）

```sql
CREATE TABLE IF NOT EXISTS t_permission (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限主键ID',
    perm_name   VARCHAR(50) NOT NULL UNIQUE COMMENT '权限名称（如goods:select、goods:delete）',
    perm_desc   VARCHAR(255) COMMENT '权限描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';
```

#### 用户角色中间表（`t_user_role`）

```sql
CREATE TABLE IF NOT EXISTS t_user_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    role_id     BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色中间表';
```

#### 角色权限中间表（`t_role_permission`）

```sql
CREATE TABLE IF NOT EXISTS t_role_permission (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id     BIGINT NOT NULL COMMENT '角色ID',
    perm_id     BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_perm (role_id, perm_id),
    FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE,
    FOREIGN KEY (perm_id) REFERENCES t_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限中间表';
```

### 3.4 权限校验多表查询

```sql
-- 查询用户拥有的所有权限（Java 后端权限校验核心）
SELECT p.perm_name
FROM t_user u
INNER JOIN t_user_role ur ON u.id = ur.user_id
INNER JOIN t_role_permission rp ON ur.role_id = rp.role_id
INNER JOIN t_permission p ON rp.perm_id = p.id
WHERE u.id = 1 AND u.is_delete = 0;
```

---

## 四、实战要点

### 4.1 账号命名与密码规范

| 规范 | 示例 | 说明 |
|------|------|------|
| **用户名** | `db_ecommerce_user` | 前缀 `db_` + 项目名 + `_user` |
| **密码** | `Ecommerce@123` | 大小写 + 数字 + 特殊符号，禁止弱密码 |
| **访问IP** | `localhost` 或指定 IP | 生产环境禁止 `%`（所有 IP 可访问） |
| **权限范围** | `db_ecommerce.*` | 仅项目数据库，不跨库 |

### 4.2 Java 后端配置文件

```yaml
spring:
  datasource:
    username: db_ecommerce_user  # 专用账号，非 root
    password: Ecommerce@123
```

---

## 五、避坑总结

| 坑点 | 错误做法 | 正确做法 |
|------|----------|----------|
| **用 root 直连项目** | 配置中直接用 root | 创建项目专用账号，最小权限 |
| **忘记 FLUSH PRIVILEGES** | 分配权限后未刷新 | 执行 `FLUSH PRIVILEGES;` |
| **生产环境用 % 访问** | `CREATE USER ... @'%'` | 限定为 `localhost` 或指定服务器 IP |
| **分配 ALL PRIVILEGES** | 给项目账号超级权限 | 仅分配 SELECT/INSERT/UPDATE |
| **密码弱口令** | `123456`、`password` | 大小写 + 数字 + 特殊符号 |

---

## 六、企业级最佳实践

### 6.1 环境账号分级

| 环境 | 账号 | 权限 |
|------|------|------|
| 开发 | `db_xxx_dev` | SELECT/INSERT/UPDATE/DELETE |
| 测试 | `db_xxx_test` | SELECT/INSERT/UPDATE/DELETE |
| 生产 | `db_xxx_user` | SELECT/INSERT/UPDATE（禁止 DELETE/DROP） |
| 运维 | `db_xxx_admin` | ALL（仅 DBA 持有，双人授权） |

### 6.2 权限管理 Checklist

- [ ] 项目配置文件使用专用账号，禁用 root
- [ ] 生产账号无 DELETE/DROP/ALTER 权限
- [ ] 密码符合复杂度要求
- [ ] 生产环境限制访问 IP
- [ ] 权限变更后执行 `FLUSH PRIVILEGES`
- [ ] 业务层权限使用"用户-角色-权限"中间表设计
