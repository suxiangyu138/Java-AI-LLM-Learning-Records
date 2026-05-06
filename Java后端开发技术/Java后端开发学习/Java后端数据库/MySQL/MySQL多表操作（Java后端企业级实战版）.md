# MySQL 多表操作（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MySQL 多表关联操作  
> **前置基础**：MySQL 单表 CRUD、数据库设计（表关联关系）  
> **衔接章节**：MySQL 单表操作 → **本章** → MySQL 事务

---

## 一、核心概念

### 1.1 多表操作的定义

Java 后端开发中，单一业务场景往往需要关联多张表的数据。多表操作（Multi-Table Operation）是指通过 SQL 的 `JOIN` 语法将两张或多张表按关联条件组合查询、新增、修改或删除的技术，核心目标是 **精准关联、高效查询**。

### 1.2 核心表关联关系（基于电商场景）

以下基于 `db_ecommerce` 数据库的核心表，明确表间关联逻辑：

| 主表 | 从表 | 关联关系 | 关联字段 |
|------|------|----------|----------|
| `t_user`（用户表） | `t_order`（订单表） | 一对多（1:N） | `t_order.user_id → t_user.id` |
| `t_goods`（商品表） | `t_order_detail`（订单详情表） | 一对多（1:N） | `t_order_detail.goods_id → t_goods.id` |
| `t_order`（订单表） | `t_order_detail`（订单详情表） | 一对多（1:N） | `t_order_detail.order_id → t_order.id` |

### 1.3 基础测试数据

```sql
-- 插入基础测试数据，确保多表操作可正常执行
INSERT INTO t_user (username, password, phone, gender)
VALUES ('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', 1);

INSERT INTO t_goods (goods_name, goods_code, category_id, price, stock)
VALUES ('小米14', 'XM14001', 1, 4999.00, 100),
       ('华为Mate60', 'HW60001', 1, 5999.00, 80);

INSERT INTO t_order (order_no, user_id, total_price, order_status)
VALUES ('20241001001', 1, 4999.00, 1);

INSERT INTO t_order_detail (order_id, goods_id, goods_name, goods_price, buy_num)
VALUES (1, 1, '小米14', 4999.00, 1);
```

### 1.4 三种核心 JOIN 类型速览

| JOIN 类型 | 核心逻辑 | 使用频率 | 典型场景 |
|-----------|----------|----------|----------|
| `INNER JOIN` | 取两表交集（匹配才返回） | ★★★★★ 最高 | 查询已下单用户及订单 |
| `LEFT JOIN` | 保留左表全部，右表不匹配为 NULL | ★★★★ 高 | 用户列表（含可选订单信息） |
| `RIGHT JOIN` | 保留右表全部，左表不匹配为 NULL | ★ 极少 | 可用 LEFT JOIN 反转替代 |

---

## 二、底层原理

### 2.1 JOIN 查询的内部机制

```
INNER JOIN 逻辑示意：
  t_user (左表)          t_order (右表)
  ┌──────┬──────┐       ┌──────┬──────────┐
  │ u.id │ name │       │ o.id │ user_id  │
  ├──────┼──────┤       ├──────┼──────────┤
  │  1   │ 张三 │───→   │ 101  │    1     │  ✓ 匹配
  │  2   │ 李四 │───→   │ 102  │    1     │  ✓ 匹配
  │  3   │ 王五 │───→   │      │          │  ✗ 不匹配（无订单）
  └──────┴──────┘       └──────┴──────────┘
  结果：只返回 id=1,2 的行（交集）

LEFT JOIN 逻辑示意：
  结果：返回 id=1,2,3 的行（王五的订单字段为 NULL）
```

### 2.2 关联条件的底层原则

- **必须是主表主键 = 从表外键**：如 `u.id = o.user_id`，确保关联的准确性
- **禁止使用非关联字段** 进行 JOIN（如 `u.username = o.order_no`），会导致笛卡尔积或数据混乱
- **索引要求**：关联字段（外键）必须建立索引，否则多表 JOIN 会退化为全表扫描

### 2.3 多表操作顺序的约束原理

| 操作类型 | 执行顺序 | 原因 |
|----------|----------|------|
| **新增** | 先主表 → 再从表 | 从表的外键依赖主表主键，必须先存在主表记录 |
| **删除** | 先从表 → 再主表 | 主表被从表引用时无法直接删除（外键约束） |
| **修改（关联字段变更）** | 先从表 → 再主表 | 确保引用完整性 |

### 2.4 分页 + JOIN 的执行顺序

- **正确顺序**：先 JOIN 全部关联表 → 再 LIMIT 分页
- **错误顺序**：先对主表分页 → 再 JOIN 其他表（会导致每页数据量不足或数据错乱）
- **原因**：分页后再 JOIN 可能因关联条件过滤掉部分行，导致返回行数 < 预期 pageSize

### 2.5 子查询 vs JOIN 性能对比

| 维度 | JOIN 查询 | 子查询 |
|------|-----------|--------|
| **执行计划** | 单次扫描 + Hash Join | 可能多次扫描（嵌套循环） |
| **适用场景** | 复杂多表关联（3 张及以上） | 简单筛选、单条件查询 |
| **可读性** | 高（关联关系清晰） | 中（嵌套层级多时复杂） |
| **优化器友好度** | 高 | 低（子查询可能无法使用索引） |

---

## 三、代码实现

### 3.1 内连接（INNER JOIN）—— 最常用

**核心逻辑**：只查询两张表中关联条件匹配的数据，不匹配的数据不会显示。

**业务场景**：查询已下单的用户信息及对应订单信息。

```sql
-- INNER JOIN：取两表交集
SELECT
    u.id          AS user_id,   -- 别名区分同名字段
    u.username,
    u.phone,
    o.id          AS order_id,
    o.order_no,
    o.total_price,
    o.order_status
FROM t_user u                    -- 表别名 u（简化书写）
INNER JOIN t_order o             -- 表别名 o
    ON u.id = o.user_id          -- 关联条件：用户主键 = 订单外键
WHERE u.is_delete = 0
  AND o.is_delete = 0;           -- 必加逻辑删除筛选
```

**Java 后端关联实战要点**：
- 对应业务接口："已下单用户列表"接口，后端返回用户信息 + 订单信息
- 通过 MyBatis 的 `<resultMap>` 映射多表字段到 `UserOrderVO` 实体类
- 表别名规范：每张表取简短别名（`t_user` → `u`、`t_order` → `o`），避免字段名冲突

### 3.2 左连接（LEFT JOIN）—— 保留左表全部

**核心逻辑**：保留左表的全部数据，右表不匹配的字段为 NULL。

**业务场景**：查询所有未删除用户，包含其订单信息（无订单的用户也显示）。

```sql
-- LEFT JOIN：保留左表全部数据
SELECT
    u.id          AS user_id,
    u.username,
    o.order_no,                 -- 无订单时，该字段为 NULL
    o.total_price,
    o.order_status
FROM t_user u
LEFT JOIN t_order o
    ON u.id = o.user_id
WHERE u.is_delete = 0;          -- 左表筛选；右表筛选可加在 ON 后或 WHERE 后
```

**关键区别（INNER JOIN vs LEFT JOIN）**：
- `INNER JOIN`：只显示有订单的用户
- `LEFT JOIN`：显示所有用户（无订单的用户也会显示，订单相关字段为 NULL）
- Java 后端"用户列表 + 关联订单"接口 **优先用 LEFT JOIN**

### 3.3 右连接（RIGHT JOIN）—— 极少使用

**核心逻辑**：保留右表全部数据，左表不匹配的字段为 NULL。企业级开发中极少用，可通过 LEFT JOIN 反转表顺序替代。

**业务场景**：查询所有未删除订单，包含对应用户信息（无用户的订单也显示）。

```sql
-- RIGHT JOIN：保留右表全部数据
SELECT
    o.id          AS order_id,
    o.order_no,
    u.username,                 -- 无用户时，该字段为 NULL
    u.phone
FROM t_user u
RIGHT JOIN t_order o
    ON u.id = o.user_id
WHERE o.is_delete = 0;
```

### 3.4 多表关联（3 张及以上表）—— 订单详情实战

**业务场景**：查询订单详情，关联订单表、用户表、商品表。

```sql
-- 关联 4 张表：t_order_detail → t_order → t_user → t_goods
SELECT
    od.id          AS detail_id,
    o.order_no,                 -- 订单表字段
    u.username,                 -- 用户表字段
    g.goods_name,               -- 商品表字段
    od.goods_price,
    od.buy_num,
    o.total_price               -- 订单总金额
FROM t_order_detail od
INNER JOIN t_order o  ON od.order_id  = o.id   -- 详情 → 订单
INNER JOIN t_user u   ON o.user_id    = u.id   -- 订单 → 用户
INNER JOIN t_goods g  ON od.goods_id  = g.id   -- 详情 → 商品
WHERE od.is_delete = 0
  AND o.is_delete  = 0
  AND u.is_delete  = 0
  AND g.is_delete  = 0;         -- 所有表加逻辑删除筛选
```

**Java 后端实战技巧**：
- 按 **业务逻辑顺序** 关联（详情 → 订单 → 用户），提升 SQL 可读性
- 所有关联表都需加 `is_delete` 筛选
- 通过 MyBatis `<resultMap>` 将多表字段映射到一个 VO（如 `OrderDetailVO`）

### 3.5 多表关联新增 —— 创建订单

**Java 后端"创建订单"接口核心流程**：先新增主表 → 获取自增 ID → 新增从表。

```sql
-- 步骤 1：新增订单主表（t_order）
INSERT INTO t_order (order_no, user_id, total_price, order_status)
VALUES ('20241001002', 1, 5999.00, 0);

-- 步骤 2：新增订单详情表（t_order_detail），关联步骤 1 的订单 ID
INSERT INTO t_order_detail (order_id, goods_id, goods_name, goods_price, buy_num)
VALUES (2, 2, '华为Mate60', 5999.00, 1);
```

**MyBatis 关联获取自增主键**：

```xml
<!-- MyBatis Mapper XML：新增订单并获取自增 ID -->
<insert id="insertOrder" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO t_order (order_no, user_id, total_price, order_status)
    VALUES (#{orderNo}, #{userId}, #{totalPrice}, #{orderStatus})
</insert>
```

### 3.6 多表关联修改 —— 修改订单状态

**核心原则**：只修改需要变更的表，无需修改所有关联表。

```sql
-- 仅修改主表（t_order），关联字段（user_id）未变更，无需修改其他表
UPDATE t_order
SET order_status = 1,
    pay_time     = NOW()
WHERE id = 2
  AND is_delete = 0;

-- 若需修改详情表字段（如购买数量），单独修改从表
UPDATE t_order_detail
SET buy_num = 2
WHERE order_id = 2
  AND goods_id = 2
  AND is_delete = 0;
```

### 3.7 多表关联删除 —— 逻辑删除订单

**核心原则**：企业级开发中均为逻辑删除，先删从表再删主表。

```sql
-- 步骤 1：先逻辑删除从表（订单详情），解除与主表的关联
UPDATE t_order_detail
SET is_delete = 1
WHERE order_id = 2
  AND is_delete = 0;

-- 步骤 2：再逻辑删除主表（订单）
UPDATE t_order
SET is_delete = 1
WHERE id = 2
  AND is_delete = 0;
```

### 3.8 关联查询 + 分页 —— 订单列表接口核心

```sql
-- 关联多表 + 分页（第 1 页，每页 10 条）
SELECT
    o.id          AS order_id,
    o.order_no,
    u.username,
    g.goods_name,
    o.total_price,
    o.order_status,
    o.create_time
FROM t_order o
INNER JOIN t_user u          ON o.user_id = u.id
INNER JOIN t_order_detail od ON o.id = od.order_id
INNER JOIN t_goods g         ON od.goods_id = g.id
WHERE o.is_delete = 0
  AND u.is_delete = 0
  AND g.is_delete = 0
ORDER BY o.create_time DESC
LIMIT 0, 10;
```

### 3.9 关联查询 + 聚合函数 —— 统计接口核心

```sql
-- 场景 1：统计每个用户的订单总数
SELECT
    u.id          AS user_id,
    u.username,
    COUNT(o.id)   AS order_count
FROM t_user u
LEFT JOIN t_order o ON u.id = o.user_id
WHERE u.is_delete = 0
  AND o.is_delete = 0
GROUP BY u.id, u.username;

-- 场景 2：统计每个商品的销售总量
SELECT
    g.id            AS goods_id,
    g.goods_name,
    SUM(od.buy_num) AS total_sales
FROM t_goods g
LEFT JOIN t_order_detail od ON g.id = od.goods_id
WHERE g.is_delete = 0
  AND od.is_delete = 0
GROUP BY g.id, g.goods_name;
```

### 3.10 子查询 —— 替代简单关联

```sql
-- 子查询：查询购买过"小米14"的用户信息
SELECT id, username, phone
FROM t_user
WHERE id IN (
    SELECT DISTINCT o.user_id
    FROM t_order o
    INNER JOIN t_order_detail od ON o.id = od.order_id
    WHERE od.goods_name = '小米14'
      AND o.is_delete  = 0
      AND od.is_delete = 0
)
AND is_delete = 0;

-- 等价的多表 JOIN 查询（复杂场景优先用 JOIN）
SELECT DISTINCT u.id, u.username, u.phone
FROM t_user u
INNER JOIN t_order o        ON u.id = o.user_id
INNER JOIN t_order_detail od ON o.id = od.order_id
WHERE od.goods_name = '小米14'
  AND u.is_delete  = 0
  AND o.is_delete  = 0
  AND od.is_delete = 0;
```

---

## 四、实战要点

### 4.1 Java 后端接口与 SQL 场景映射

| 业务接口 | SQL 操作类型 | 核心技巧 |
|----------|-------------|----------|
| 订单列表 | JOIN + 分页 + 排序 | 先 JOIN 再分页 |
| 订单详情 | 4 表 JOIN | INNER JOIN 按业务顺序串联 |
| 用户列表（含订单） | LEFT JOIN | 保留无订单用户 |
| 创建订单 | 主表 INSERT → 从表 INSERT | `useGeneratedKeys` 获取自增 ID |
| 删除订单 | 从表 UPDATE → 主表 UPDATE | 逻辑删除，先从后主 |
| 销售统计 | JOIN + GROUP BY + 聚合函数 | LEFT JOIN 保留无销售商品 |
| 商品筛选用户 | 子查询 或 JOIN | 简单场景子查询，复杂场景 JOIN |

### 4.2 MyBatis 多表结果映射

多表查询结果需要通过 `<resultMap>` 映射到 VO 对象：

```xml
<resultMap id="orderDetailMap" type="com.example.ecommerce.vo.OrderDetailVO">
    <id     column="detail_id"    property="detailId"/>
    <result column="order_no"     property="orderNo"/>
    <result column="username"     property="username"/>
    <result column="goods_name"   property="goodsName"/>
    <result column="goods_price"  property="goodsPrice"/>
    <result column="buy_num"      property="buyNum"/>
    <result column="total_price"  property="totalPrice"/>
</resultMap>
```

### 4.3 性能优化要点

- **外键索引**：所有 JOIN 的关联字段（外键）必须建立索引
- **减少关联表数量**：单次查询关联表不超过 4-5 张，复杂场景拆分查询
- **冗余字段**：高频查询字段（如 `goods_name`）可冗余存储到从表，避免每次 JOIN
- **避免 SELECT ***：只查询接口需要的字段，减少数据传输

---

## 五、避坑总结

### 5.1 关联条件错误

| 错误做法 | 后果 | 正确做法 |
|----------|------|----------|
| 用非关联字段 JOIN（如 `u.username = o.order_no`） | 数据混乱、笛卡尔积 | 必须使用主键 = 外键 |
| 遗漏关联条件 | 笛卡尔积，结果集爆炸 | 每个 JOIN 必须配 ON |
| JOIN 条件写反（左表外键 = 右表主键） | 数据错误 | 主表主键 = 从表外键 |

### 5.2 分页与 JOIN 顺序错误

| 错误做法 | 后果 | 正确做法 |
|----------|------|----------|
| 先 `LIMIT` 再 JOIN | 每页数据不足 | 先 JOIN 全部表，再 LIMIT |

### 5.3 主键/外键修改陷阱

| 错误做法 | 后果 | 正确做法 |
|----------|------|----------|
| 直接修改主表主键 | 从表外键关联失效，业务异常 | 禁止修改主键；若必须修改，先从表再主表 |

### 5.4 删除顺序错误

| 错误做法 | 后果 | 正确做法 |
|----------|------|----------|
| 先删主表再删从表 | 外键约束报错，删除失败 | 先从表，再主表 |
| 物理删除多表关联数据 | 数据丢失，无法追溯 | 使用逻辑删除（`is_delete = 1`） |

### 5.5 逻辑删除遗漏

- 多表 JOIN 时，**每一张关联表** 都必须添加 `is_delete = 0` 条件，缺一不可
- 遗漏任何一张表的逻辑删除条件，都可能查询到已删除的数据

### 5.6 SQL 注入风险

- **禁止** 使用字符串拼接构建 SQL（如 `"SELECT * FROM t_order WHERE id = " + orderId`）
- **必须** 通过 MyBatis 参数绑定 `#{}` 传递参数

---

## 六、企业级最佳实践

### 6.1 关联条件规范

- 关联条件必须是 **主表主键 = 从表外键**
- 禁止用非关联字段关联，避免数据混乱

### 6.2 表别名规范

- 每张表必须取 **简短、有意义** 的别名：`t_user` → `u`、`t_goods` → `g`、`t_order_detail` → `od`
- 避免字段名冲突（如多表都有 `id` 字段，用 `u.id`、`o.id` 区分）

### 6.3 逻辑删除规范

- 所有关联表必须添加 `is_delete = 0` 筛选条件，缺一不可
- 避免查询到已删除的数据，造成业务逻辑错误

### 6.4 性能规范

- 多表关联 **优先用 JOIN**，避免多层子查询（效率低）
- 分页查询必须 **先关联，再分页**，禁止先分页再关联
- **禁止关联无关表**（如查询订单列表，无需关联商品分类表），减少关联开销
- 单次 JOIN 表数量控制在 4-5 张以内

### 6.5 操作顺序规范（强制执行）

| 操作 | 执行顺序 | 原因 |
|------|----------|------|
| 新增 | 先主表 → 再从表 | 从表外键依赖主表主键 |
| 删除 | 先从表 → 再主表 | 解除外键引用后才能删除主表 |
| 修改（关联字段变更） | 先从表 → 再主表 | 保持引用完整性 |

### 6.6 安全规范

- 禁止拼接 SQL 字符串（避免 SQL 注入），Java 后端通过 MyBatis `#{}` 参数绑定传递参数
- 生产环境 **禁止物理删除** 多表关联数据，一律使用逻辑删除
- 可通过 MySQL 外键 `ON DELETE CASCADE` 配置实现级联逻辑删除

### 6.7 代码审查 Checklist

- [ ] 所有 JOIN 是否都有 ON 条件？
- [ ] 关联条件是否为主键 = 外键？
- [ ] 所有表是否都加了 `is_delete = 0`？
- [ ] 是否有不必要的表被 JOIN？
- [ ] 分页是否在 JOIN 之后？
- [ ] 参数是否使用 `#{}` 而非 `${}`？
- [ ] 是否使用了表别名避免字段冲突？

### 6.8 本章实战练习

基于 `db_ecommerce` 数据库核心表，完成以下操作：

1. 关联 `t_user`、`t_order`、`t_order_detail`、`t_goods` 四张表，查询所有未删除订单的详情，包含用户名、订单号、商品名称、购买数量、订单总金额
2. 新增一条订单（主表 `t_order`）和对应的订单详情（从表 `t_order_detail`），关联已存在的用户和商品
3. 修改上述新增订单的状态为"已支付"，同步更新支付时间
4. 逻辑删除上述新增的订单，同步逻辑删除其关联的订单详情
5. 用子查询查询购买过"华为Mate60"的用户信息
6. 关联 `t_goods` 和 `t_order_detail`，统计每个商品的销售总量和销售总金额

### 6.9 本章小结

- 多表操作核心是关联查询，重点掌握 `INNER JOIN`（交集）和 `LEFT JOIN`（保留左表）
- 多表关联按 **主表 → 从表** 顺序，确保关联条件精准
- 多表新增/修改/删除，遵循 **"先主后从（新增）、先从后主（删除）"** 的顺序
- 企业级开发中，多表操作优先用 `JOIN` 查询，子查询适合简单场景
- 所有多表操作必须加逻辑删除筛选，牢记表别名、关联条件等规范
- 后续章节：MySQL 索引（提升多表查询效率）、MySQL 事务（保证多表操作的原子性）
