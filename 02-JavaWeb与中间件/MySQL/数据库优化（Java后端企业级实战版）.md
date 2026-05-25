# 数据库优化（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MySQL 数据库优化体系  
> **前置基础**：MySQL 操作、多表查询、事务、视图、数据库编程  
> **衔接章节**：数据库编程 → 事务 → 视图 → 多表操作 → **本章**

---

## 一、核心概念

### 1.1 数据库优化的定义

数据库优化是一套 **系统性优化体系**，核心围绕 **减少数据库 IO 操作、缩短 SQL 执行时间、降低锁竞争** 三个方向，在不改变业务逻辑、不影响数据一致性的前提下，提升数据库整体性能。

### 1.2 核心原则

| 原则 | 说明 |
|------|------|
| **不影响业务逻辑** | 优化后保证数据一致性、完整性 |
| **性价比优先** | 先做低成本高收益优化（优化 SQL、加索引），再考虑分库分表 |
| **贴合业务场景** | 订单查询高频 → 重点优化订单表；权限查询高频 → 重点优化视图 |
| **避免过度优化** | 低频简单查询无需优化，过度优化增加维护成本 |

> **关键前提**：优化前先 **定位问题** — 通过慢查询日志、EXPLAIN 执行计划找到性能瓶颈，再针对性优化，避免盲目操作。

---

## 二、底层原理

### 2.1 B+ 树索引结构

MySQL 索引默认使用 **B+ 树** 数据结构：
- 非叶子节点只存索引键，叶子节点存完整数据行（或主键值）
- 叶子节点之间通过双向链表连接，支持高效范围查询
- 查询时间复杂度 O(log n)，vs 全表扫描 O(n)

### 2.2 索引加速查询的原理

```
无索引：全表扫描
  MySQL → 逐行扫描表中所有数据 → 过滤匹配行 → 返回结果（数据量越大越慢）

有索引：索引定位
  MySQL → 在 B+ 树中二分查找目标值 → 定位到数据页 → 直接读取目标行（效率呈指数级提升）
```

### 2.3 慢查询日志原理

MySQL 慢查询日志记录执行时间超过 `long_query_time` 阈值的 SQL，通过分析慢查询日志定位性能瓶颈。

### 2.4 EXPLAIN 执行计划核心指标

| 指标 | 含义 | 优化目标 |
|------|------|----------|
| `type` | 访问类型 | 避免 `ALL`（全表扫描），至少达到 `ref`（索引扫描） |
| `key` | 使用的索引 | 不为 `NULL` |
| `rows` | 扫描行数 | 越小越好 |
| `Extra` | 额外信息 | 避免 `Using filesort`、`Using temporary` |

---

## 三、代码实现

### 3.1 索引优化（最常用、高收益）

#### 3.1.1 索引设计实战

```sql
-- 1. 单字段索引：高频查询字段
CREATE INDEX idx_user_username ON t_user(username);      -- 登录查询高频
CREATE INDEX idx_order_orderno ON t_order(order_no);     -- 订单号查询高频
CREATE INDEX idx_order_userid ON t_order(user_id);       -- 关联用户查询高频

-- 2. 联合索引：多条件查询（遵循最左前缀原则）
CREATE INDEX idx_detail_order_goods ON t_order_detail(order_id, goods_id);

-- 3. 唯一索引：唯一字段，避免重复数据
CREATE UNIQUE INDEX idx_order_orderno_unique ON t_order(order_no);
```

> **避免冗余索引**：若已创建联合索引 `idx_detail_order_goods`（order_id + goods_id），无需再单独创建 `order_id` 的索引。

#### 3.1.2 必须加索引的字段类型

| 字段类型 | 示例 | 原因 |
|----------|------|------|
| **查询条件高频字段** | `order_no`、`username` | 避免全表扫描 |
| **多表关联字段** | `user_id`、`order_id` | JOIN 效率的核心依赖 |
| **排序/分组字段** | `create_time`、`total_sales` | GROUP BY / ORDER BY 加速 |

### 3.2 SQL 优化（低成本、易落地）

#### 3.2.1 高频 SQL 优化技巧

```sql
-- ❌ 错误：全表扫描
SELECT * FROM t_order;

-- ✅ 正确：加条件 + 索引 + 分页
SELECT id, order_no, total_price FROM t_order
WHERE is_delete = 0 ORDER BY create_time DESC LIMIT 0, 20;


-- ❌ 错误：SELECT * 冗余字段
SELECT * FROM v_user_permission WHERE user_id = 1;

-- ✅ 正确：仅查所需字段
SELECT perm_name FROM v_user_permission WHERE user_id = 1;


-- ❌ 错误：大偏移量分页
SELECT * FROM t_order ORDER BY create_time DESC LIMIT 100000, 10;

-- ✅ 正确：用主键 ID 过滤（游标分页）
SELECT * FROM t_order WHERE id > 100000 ORDER BY id DESC LIMIT 10;


-- ❌ 错误：多层子查询嵌套
SELECT * FROM t_user WHERE id IN (
    SELECT user_id FROM t_order WHERE id IN (
        SELECT order_id FROM t_order_detail WHERE goods_id = 1));

-- ✅ 正确：用 JOIN 替代深层子查询
SELECT DISTINCT u.* FROM t_user u
INNER JOIN t_order o ON u.id = o.user_id
INNER JOIN t_order_detail od ON o.id = od.order_id
WHERE od.goods_id = 1;
```

#### 3.2.2 实战：优化复杂 SQL

```sql
-- 优化前：查询用户拥有的所有权限（多表 JOIN，冗余 DISTINCT）
SELECT DISTINCT p.perm_name
FROM t_user u
INNER JOIN t_user_role ur ON u.id = ur.user_id
INNER JOIN t_role_permission rp ON ur.role_id = rp.role_id
INNER JOIN t_permission p ON rp.perm_id = p.id
WHERE u.id = 1 AND u.is_delete = 0;

-- 优化后：从权限表出发，利用 user_id 索引快速过滤
SELECT p.perm_name
FROM t_permission p
INNER JOIN t_role_permission rp ON p.id = rp.perm_id
INNER JOIN t_user_role ur ON rp.role_id = ur.role_id
WHERE ur.user_id = 1 AND p.id IS NOT NULL;
```

### 3.3 事务与锁优化

#### 3.3.1 缩小事务范围

```java
// ❌ 优化前：事务范围过大，包含日志记录等无关操作
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order, List<OrderDetail> detailList) {
    orderMapper.addOrder(order);
    orderDetailMapper.addDetailList(detailList);
    logService.recordLog("创建订单：" + order.getOrderNo()); // 无关，延长事务持有锁的时间
}

// ✅ 优化后：仅核心数据库操作在事务内
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order, List<OrderDetail> detailList) {
    orderMapper.addOrder(order);
    orderDetailMapper.addDetailList(detailList);
}
// 日志等操作在事务提交后执行
```

#### 3.3.2 乐观锁实现库存扣减

```sql
-- 1. 商品表新增乐观锁版本号字段
ALTER TABLE t_goods ADD COLUMN version INT DEFAULT 1 COMMENT '乐观锁版本号';

-- 2. 库存扣减 SQL（版本号匹配 + 库存 > 0 才执行）
UPDATE t_goods
SET stock = stock - 1, version = version + 1
WHERE id = 2 AND stock > 0 AND version = 1;
-- 若 affected rows = 0，说明并发冲突（版本号不匹配或库存不足），Java 端重试或提示
```

### 3.4 表结构优化

```sql
-- 合理选择字段类型，减少存储空间
-- age 用 TINYINT（0-255），无需 INT（4 字节）
ALTER TABLE t_user MODIFY COLUMN age TINYINT UNSIGNED;

-- 金额用 DECIMAL，避免 FLOAT 精度问题
ALTER TABLE t_order MODIFY COLUMN total_price DECIMAL(10, 2);

-- 拆分大表（按时间分区，数据量 100 万+ 时考虑）
-- t_order_2024、t_order_2025
```

### 3.5 慢查询日志配置

```sql
-- 开启慢查询日志（临时生效）
SET GLOBAL slow_query_log = 'ON';

-- 设置慢查询阈值（1 秒）
SET GLOBAL long_query_time = 1;

-- 查看慢查询日志存储路径
SHOW VARIABLES LIKE 'slow_query_log_file';
```

### 3.6 EXPLAIN 执行计划分析

```sql
-- 分析 SQL 执行计划
EXPLAIN SELECT perm_name FROM v_user_permission WHERE user_id = 1;
-- 关注 type（避免 ALL）、key（不为 NULL）、rows（越小越好）
```

---

## 四、实战要点

### 4.1 Java 后端高频场景与优化映射

| 业务接口 | 性能瓶颈 | 优化手段 |
|----------|----------|----------|
| 订单列表查询 | SQL 慢、全表扫描 | 添加 `order_no` + `create_time` 联合索引 |
| 用户权限查询 | 视图多层关联 | 简化为直接 JOIN + 索引 |
| 商品搜索 | 模糊查询 `%keyword%` | 引入 Elasticsearch 全文索引，MySQL 仅做精确查询 |
| 库存扣减 | 高并发行锁竞争 | 乐观锁（version 字段）+ 库存 Redis 预减 |
| 统计报表 | 大表聚合函数慢 | 离线计算 + 缓存，避免实时聚合 |

### 4.2 优化效果评估标准

| 指标 | 优化预期 |
|------|----------|
| 单条 SQL 执行时间 | 降低到优化前的 10% 以下 |
| 慢查询条目 | 大幅度减少，核心接口无慢查询 |
| Full GC 触发次数 | 控制在 1 次/天以内 |
| 接口响应时间（P99） | 降低到可接受范围内（如 < 500ms） |

---

## 五、避坑总结

### 5.1 索引相关

| 坑点 | 错误做法 | 正确做法 |
|------|----------|----------|
| **索引字段使用函数** | `WHERE DATE(create_time) = '2024-10-01'` | `WHERE create_time BETWEEN '2024-10-01 00:00:00' AND '2024-10-01 23:59:59'` |
| **模糊查询前缀 %** | `WHERE username LIKE '%zhangsan'` | `WHERE username LIKE 'zhangsan%'`（后缀 % 索引生效） |
| **联合索引不遵循最左前缀** | 联合索引 on (a,b) 但 WHERE 仅用 b | WHERE 条件必须包含 a（最左列）才能命中索引 |
| **盲目添加索引** | 给所有字段加索引 | 仅高频查询/关联/排序字段加索引（索引增加写操作开销） |

### 5.2 综合避坑

| 坑点 | 说明 |
|------|------|
| **未定位就优化** | 先通过慢查询日志和 EXPLAIN 找到瓶颈，再针对性优化 |
| **忽视写操作效率** | 索引虽加速查询但拖慢 INSERT/UPDATE/DELETE，需平衡 |
| **过度分库分表** | 单表不到 100 万行就分库分表，增加系统复杂度 |
| **视图嵌套过多** | 视图嵌套超过 2 层性能大幅下降，直接基于底层表优化 |
| **事务未提交导致锁表** | 手动事务必须在 try-catch 中确保 COMMIT/ROLLBACK |

---

## 六、企业级最佳实践

### 6.1 数据库优化四步法

```
1. 监控定位 → 慢查询日志 / EXPLAIN 找到慢 SQL
2. 快速优化 → 先优化 SQL 语句 + 添加必要索引（最快见效）
3. 事务锁优化 → 缩小事务范围 + 乐观锁/悲观锁策略选型
4. 架构优化 → 分库分表、读写分离、缓存层引入（大流量时）
```

### 6.2 GC 参数配置建议（以 G1 收集器为例，堆内存 8GB）

```bash
-Xms8g -Xmx8g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps
```

### 6.3 优化 Checklist

- [ ] 核心查询字段已添加索引
- [ ] 联合索引遵循最左前缀原则
- [ ] SQL 语句避免 SELECT *
- [ ] 分页查询使用游标分页（主键 ID 过滤）
- [ ] 子查询已替换为 JOIN
- [ ] 事务范围已最小化
- [ ] 高并发场景已使用乐观锁
- [ ] 慢查询日志已开启并定期分析
- [ ] EXPLAIN 验证索引命中

### 6.4 本章小结

1. 数据库优化核心：减少 IO、缩短 SQL 时间、降低锁竞争
2. 核心优化手段：索引优化（最常用）→ SQL 优化（低成本）→ 事务与锁优化 → 表结构优化
3. 避坑核心：先定位再优化、平衡读写、避免过度优化
4. 后续方向：分库分表 → 读写分离 → 缓存层（Redis）→ 搜索层（Elasticsearch）
