# MySQL 视图（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MySQL 视图  
> **前置基础**：MySQL 多表操作、用户与权限体系  
> **衔接章节**：MySQL 多表操作 → 用户与权限 → **本章** → MySQL 事务

---

## 一、核心概念

### 1.1 什么是视图

视图（View）是 MySQL 中的 **虚拟表**，本身不存储数据，只存储 **查询语句**。查询视图时，MySQL 自动执行背后的查询语句并返回结果——相当于将常用的复杂查询"封装"起来。

### 1.2 Java 后端实战价值

| 优势 | 说明 |
|------|------|
| **简化开发** | 复杂多表查询封装为视图，MyBatis 只查视图，无需写冗长 JOIN |
| **提升可读性** | 视图名直观体现业务含义（如 `v_user_permission`） |
| **数据安全** | 隐藏底层表结构和敏感字段（密码、手机号），仅暴露所需字段 |
| **复用性强** | 同一复杂查询供多个后端接口复用 |

> **关键提醒**：视图是虚拟表，不存储实际数据。修改视图（INSERT/UPDATE）本质是修改底层表，企业级开发中视图 **多用于查询，极少用于修改**。

---

## 二、底层原理

### 2.1 视图的工作机制

```
Java 后端 → SELECT ... FROM v_xxx WHERE ... → MySQL 解析视图定义
    → 展开视图背后的 SELECT 查询 → 合并 WHERE 条件 → 执行优化后的 SQL → 返回结果
```

视图本质是 **SQL 层面的封装**，MySQL 在查询视图时会将视图定义展开为子查询，与外部 WHERE 条件合并后执行。

### 2.2 视图 vs 底层表

| 维度 | 视图 | 底层表 |
|------|------|--------|
| 数据存储 | 不存储数据（虚拟） | 存储实际数据 |
| 底层依赖 | 依赖底层表查询 | 独立存在 |
| 修改操作 | 有限制（非空、唯一约束等） | 无限制 |
| 性能 | 取决于底层查询和索引 | 直接访问 |

---

## 三、代码实现

### 3.1 创建视图

#### 命名规范

视图名统一前缀 `v_`，全部小写，下划线分隔：`v_user_permission`、`v_order_detail`

#### 场景 1：用户权限视图

```sql
-- 封装 4 表关联查询，供 Java 后端权限校验接口使用
CREATE VIEW v_user_permission AS
SELECT
    u.id          AS user_id,
    u.username,
    p.perm_name,
    p.perm_desc
FROM t_user u
INNER JOIN t_user_role ur ON u.id = ur.user_id
INNER JOIN t_role_permission rp ON ur.role_id = rp.role_id
INNER JOIN t_permission p ON rp.perm_id = p.id
WHERE u.is_delete = 0;
```

#### 场景 2：订单详情视图

```sql
-- 封装 4 表关联查询，供订单详情/列表接口复用
CREATE VIEW v_order_detail AS
SELECT
    od.id          AS detail_id,
    o.order_no,
    u.username,
    g.goods_name,
    od.goods_price,
    od.buy_num,
    o.total_price,
    o.order_status,
    o.create_time
FROM t_order_detail od
INNER JOIN t_order o  ON od.order_id = o.id
INNER JOIN t_user u   ON o.user_id   = u.id
INNER JOIN t_goods g  ON od.goods_id  = g.id
WHERE od.is_delete = 0 AND o.is_delete = 0
  AND u.is_delete  = 0 AND g.is_delete = 0;
```

#### 创建注意事项

- 查询语句必须合法（关联条件正确、字段名不冲突）
- 字段可起别名（`u.id AS user_id`）适配 Java 实体类
- 必须包含 `is_delete = 0` 逻辑删除筛选
- 禁止为简单单表查询创建视图（仅封装高频复用的复杂多表查询）

### 3.2 查询视图

```sql
-- 查询用户权限（Java 后端权限校验核心）
SELECT perm_name FROM v_user_permission WHERE user_id = 1;

-- 分页查询订单详情（Java 后端订单列表接口）
SELECT * FROM v_order_detail
ORDER BY create_time DESC
LIMIT 0, 10;

-- 条件筛选（已支付订单）
SELECT order_no, username, goods_name FROM v_order_detail
WHERE order_status = 1;
```

> MyBatis 中可将视图当作普通表处理：`SELECT perm_name FROM v_user_permission WHERE user_id = #{userId}`

### 3.3 修改视图

```sql
-- 需求变更：新增角色名称字段
ALTER VIEW v_user_permission AS
SELECT
    u.id          AS user_id,
    u.username,
    r.role_name,  -- 新增
    p.perm_name,
    p.perm_desc
FROM t_user u
INNER JOIN t_user_role ur ON u.id = ur.user_id
INNER JOIN t_role r ON ur.role_id = r.id                       -- 新增关联
INNER JOIN t_role_permission rp ON ur.role_id = rp.role_id
INNER JOIN t_permission p ON rp.perm_id = p.id
WHERE u.is_delete = 0;
```

### 3.4 删除视图

```sql
-- 删除废弃视图（不影响底层表数据）
DROP VIEW IF EXISTS v_old_user_permission;

-- 批量删除
DROP VIEW IF EXISTS v_old_order_detail, v_old_goods_info;
```

> 删除前确认无后端接口引用，建议先注释观察无异常后再删除。

### 3.5 视图筛选敏感字段

```sql
-- 隐藏密码、手机号等敏感字段，仅暴露安全字段
CREATE VIEW v_user_safe AS
SELECT id, username, gender, create_time
FROM t_user
WHERE is_delete = 0;
```

---

## 四、实战要点

### 4.1 视图使用原则

| 原则 | 说明 |
|------|------|
| **仅用于查询** | 视图的 INSERT/UPDATE 受底层表约束，易出问题 |
| **封装高频复杂查询** | 2 张表以上 + 多次复用 → 值得创建视图 |
| **必须加 is_delete** | 与底层表规范一致 |
| **字段别名规范化** | 与 Java 实体类字段名对应 |

---

## 五、避坑总结

| 坑点 | 问题 | 正确做法 |
|------|------|----------|
| **视图嵌套** | 基于视图创建新视图，超过 2 层性能大幅下降 | 直接基于底层表创建视图 |
| **视图修改操作** | 通过视图 INSERT/UPDATE 触发底层约束异常 | 视图仅用于查询 |
| **未加逻辑删除** | 视图查询出已删除数据 | 视图定义中必须包含 `is_delete=0` |
| **删除前未检查引用** | 后端接口报错 | 先注释→观察→确认后再删除 |
| **冗余字段** | 视图包含大量无用字段 | 仅包含接口需要的字段 |

---

## 六、企业级最佳实践

### 6.1 视图开发规范

| 规范 | 说明 |
|------|------|
| 命名 `v_` 前缀 | `v_user_permission`、`v_order_detail` |
| SQL 注释 | 每个视图头部注释用途和关联表 |
| 版本管理 | 视图 DDL 纳入 Git 管理 |
| 性能监控 | 定期 EXPLAIN 检查视图查询是否走索引 |
| 字段最小化 | 只包含接口需要的字段，避免 `SELECT *` |

### 6.2 本章小结

1. 视图是虚拟表，封装复杂查询，不存储数据
2. 核心操作：`CREATE VIEW` → `SELECT` → `ALTER VIEW` → `DROP VIEW`
3. 企业级开发中视图 **多用于查询，极少用于修改**
4. 视图中必须包含 `is_delete=0` 逻辑删除筛选
