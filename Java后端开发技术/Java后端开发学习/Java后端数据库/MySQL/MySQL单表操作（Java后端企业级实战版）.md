# MySQL 单表操作（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MySQL 单表 CRUD  
> **前置基础**：MySQL 数据库创建、数据类型与约束  
> **衔接章节**：MySQL 数据库设计 → **本章** → MySQL 多表操作

---

## 一、核心概念

### 1.1 单表操作的定义

单表操作是 Java 后端开发中 **最基础、最高频** 的数据库操作，所有业务接口（登录、注册、编辑、删除）的底层本质都是单表 CRUD。

| 操作 | SQL 关键字 | Java 后端业务场景 |
|------|-----------|------------------|
| **Create** | `INSERT` | 用户注册 |
| **Read** | `SELECT` | 用户登录、列表查询、详情查看 |
| **Update** | `UPDATE` | 编辑个人信息、重置密码 |
| **Delete** | `UPDATE ... SET is_delete=1` | 用户注销（逻辑删除） |

### 1.2 示例表结构（`t_user`）

```sql
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID，对应Java Long',
    username    VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名，对应Java String',
    password    VARCHAR(100) NOT NULL COMMENT '加密后密码',
    phone       VARCHAR(11) UNIQUE COMMENT '手机号',
    gender      TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    is_delete   TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

---

## 二、底层原理

### 2.1 CRUD 与 Java 类型映射

| MySQL 类型 | Java 类型 | 示例字段 |
|------------|----------|----------|
| `BIGINT` | `Long` | `id` |
| `VARCHAR` | `String` | `username` |
| `TINYINT` | `Integer` / `Boolean` | `gender` / `is_delete` |
| `DATETIME` | `LocalDateTime` | `create_time` |

### 2.2 主键查询 vs 全表扫描

- **主键查询（`WHERE id = ?`）**：MySQL 通过聚簇索引直接定位数据页，效率最高 O(1)
- **全表扫描（`SELECT * FROM t_user`）**：逐行扫描所有数据，数据量大时极慢 O(n)

### 2.3 逻辑删除 vs 物理删除

| 方式 | 操作 | 优点 | 缺点 |
|------|------|------|------|
| **逻辑删除**（推荐） | `UPDATE SET is_delete = 1` | 数据可恢复、可追溯 | 需所有查询加 `is_delete=0` |
| **物理删除** | `DELETE FROM` | 彻底释放空间 | 数据不可恢复，生产环境禁止 |

---

## 三、代码实现

### 3.1 新增操作（INSERT）

#### 指定字段插入（推荐）

```sql
INSERT INTO t_user (username, password, phone, gender)
VALUES ('lisi', 'e10adc3949ba59abbe56e057f20f883e', '13900139000', 1);
```

#### 批量新增（高频场景）

```sql
INSERT INTO t_user (username, password, phone, gender)
VALUES
    ('zhaoliu', 'e10adc3949ba59abbe56e057f20f883e', '13600136000', 1),
    ('qianqi',  'e10adc3949ba59abbe56e057f20f883e', '13500135000', 2);
```

#### 避免重复插入（注册场景必用）

```sql
INSERT IGNORE INTO t_user (username, password, phone, gender)
VALUES ('lisi', 'e10adc3949ba59abbe56e057f20f883e', '13900139000', 1);
-- IGNORE：若违反唯一约束则跳过该条，不报错
```

### 3.2 查询操作（SELECT）

```sql
-- 场景1：用户登录（精准查询）
SELECT id, username, password, phone
FROM t_user
WHERE username = 'lisi' AND is_delete = 0;

-- 场景2：用户列表（分页查询）
SELECT id, username, phone, gender
FROM t_user
WHERE is_delete = 0
ORDER BY create_time DESC
LIMIT 0, 10;  -- 第1页，每页10条

-- 场景3：用户详情（主键查询，效率最高）
SELECT * FROM t_user WHERE id = 1 AND is_delete = 0;

-- 场景4：条件筛选
SELECT id, username, phone FROM t_user
WHERE gender = 1 AND is_delete = 0;

-- 场景5：模糊搜索
SELECT id, username, phone FROM t_user
WHERE username LIKE '%li%' AND is_delete = 0;

-- 场景6：去重查询
SELECT DISTINCT phone FROM t_user WHERE is_delete = 0;
```

### 3.3 修改操作（UPDATE）

```sql
-- 编辑用户信息（按主键修改，最安全）
UPDATE t_user
SET phone = '13900000000', gender = 2, update_time = NOW()
WHERE id = 1 AND is_delete = 0;

-- 重置密码（管理员操作）
UPDATE t_user
SET password = 'e10adc3949ba59abbe56e057f20f883e'
WHERE username = 'lisi' AND is_delete = 0;

-- 批量修改
UPDATE t_user
SET is_enable = 0
WHERE id IN (1, 2, 3) AND is_delete = 0;
```

### 3.4 删除操作（逻辑删除）

```sql
-- 逻辑删除（企业级首选）
UPDATE t_user SET is_delete = 1, update_time = NOW()
WHERE id = 1 AND is_delete = 0;

-- 恢复逻辑删除
UPDATE t_user SET is_delete = 0, update_time = NOW()
WHERE id = 1;

-- 物理删除（仅限本地测试/清理无效数据）
DELETE FROM t_user WHERE id = 10 AND is_delete = 1;
```

### 3.5 排序查询

```sql
-- 单字段排序
SELECT id, username, phone FROM t_user
WHERE is_delete = 0
ORDER BY create_time DESC;  -- DESC 降序，ASC 升序（默认）

-- 多字段排序：先按性别升序，再按创建时间倒序
SELECT id, username, gender, create_time FROM t_user
WHERE is_delete = 0
ORDER BY gender ASC, create_time DESC;
```

---

## 四、实战要点

### 4.1 Java 后端关联规范

| 规范 | 说明 |
|------|------|
| **密码加密存储** | 使用 MD5/SHA256 加密，禁止明文存储，Java 端用 `DigestUtils.md5Hex()` |
| **指定字段列表** | 禁止 `INSERT INTO t_user VALUES (...)`，字段顺序变化会导致映射异常 |
| **避免 SELECT \*** | 指定需要的字段，减少数据传输，与实体类属性一一对应 |
| **分页查询** | `LIMIT offset, size`，Java 端：`offset = (pageNum - 1) * pageSize` |
| **MyBatis `#{}`** | 参数用 `#{}` 预编译，禁止 `${}` 拼接 |

### 4.2 逻辑删除体系

- 所有业务表设计必须包含 `is_delete` 字段（`TINYINT DEFAULT 0`）
- **所有查询、修改、删除 SQL 必须添加 `AND is_delete = 0`**
- 物理删除仅限本地测试环境

---

## 五、避坑总结

| 坑点 | 错误做法 | 正确做法 |
|------|----------|----------|
| **省略字段列表** | `INSERT INTO t_user VALUES (...)` | 必须写 `INSERT INTO t_user(col1, col2) VALUES (...)` |
| **密码明文存储** | 密码直接插入 | 必须加密后存储 |
| **WHERE 条件遗漏** | `UPDATE t_user SET phone = 'xxx'` | 必须加 `WHERE id = ? AND is_delete = 0` |
| **SELECT \*** | 生产查询返回所有字段 | 按需指定字段列表 |
| **忘记逻辑删除筛选** | `WHERE username = 'lisi'` | `WHERE username = 'lisi' AND is_delete = 0` |
| **模糊查询前缀 %** | `LIKE '%zhang'` 索引失效 | 尽量用 `LIKE 'zhang%'` |
| **非空字段未赋值** | INSERT 时 username/password 为 NULL | Java 代码前端参数校验 |

---

## 六、企业级最佳实践

### 6.1 SQL 编写规范

- 关键字大写（`SELECT`、`FROM`、`WHERE`），表名字段名小写
- 每条 SQL 末尾加 `;`
- 所有业务表操作必须包含 `is_delete` 筛选
- 分页查询统一使用 `LIMIT offset, pageSize`
- 密码字段统一加密（不可逆加密算法）

### 6.2 MyBatis 映射规范

```xml
<!-- 查询示例 -->
<select id="findByUsername" resultType="User">
    SELECT id, username, password, phone, gender
    FROM t_user
    WHERE username = #{username}
      AND is_delete = 0
</select>

<!-- 新增示例 -->
<insert id="insertUser" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO t_user (username, password, phone, gender)
    VALUES (#{username}, #{password}, #{phone}, #{gender})
</insert>
```

### 6.3 企业级开发 Checklist

- [ ] 所有表使用 InnoDB 引擎 + utf8mb4 字符集
- [ ] 所有表包含 `is_delete` 逻辑删除字段
- [ ] 密码字段加密存储（不可逆）
- [ ] CRUD SQL 使用 `#{}` 参数绑定
- [ ] 列表查询包含分页 `LIMIT`
- [ ] 所有查询包含 `is_delete = 0` 筛选
