# 数据库编程（Java 后端实战版）

> **文档定位**：Java 后端企业级技术文档 | 数据库编程  
> **核心目标**：通过代码与数据库交互，实现业务 CRUD  
> **前置基础**：MySQL 操作、多表查询、视图、事务、JDBC/MyBatis

---

## 一、核心概念

### 1.1 数据库编程的定义

数据库编程核心是通过代码（Java）与数据库建立连接，执行 SQL 操作，实现业务数据的存储、读取与管理。对于 Java 后端而言，是将 SQL 操作与后端代码融合，确保数据一致性、安全性和高效性。

### 1.2 两种连接方式

| 方式 | 特点 | 适用场景 |
|------|------|----------|
| **JDBC** | 手动加载驱动 → 建立连接 → 执行 SQL → 关闭资源 | 简单场景、学习底层原理 |
| **MyBatis**（推荐） | 简化 SQL 编写，支持多表映射、参数绑定 | **企业级开发首选** |

---

## 二、底层原理

### 2.1 数据库连接流程

```
Java 代码 → 连接池获取连接 → 执行 SQL（#{}预编译）→ ResultSet → 映射为 Java 对象 → 归还连接
```

### 2.2 核心规范

| 规范 | 说明 |
|------|------|
| **存储引擎** | 所有业务表使用 InnoDB（支持事务和外键） |
| **账号权限** | 专用账号，最小权限（SELECT/INSERT/UPDATE） |
| **命名一致** | 表名/字段名与 Java 实体类对齐（下划线 ↔ 驼峰） |
| **自动提交** | 多表操作关闭自动提交，手动控制事务 |

---

## 三、代码实现

### 3.1 单表 CRUD

```sql
-- 新增
INSERT INTO t_category (category_name, sort, is_delete) VALUES ('手机', 1, 0);

-- 查询
SELECT * FROM t_category WHERE id = 1 AND is_delete = 0;

-- 修改
UPDATE t_category SET category_name = '智能手机' WHERE id = 1 AND is_delete = 0;

-- 逻辑删除
UPDATE t_category SET is_delete = 1 WHERE id = 1;
```

```java
// MyBatis Mapper
int addCategory(Category category);
Category selectCategoryById(Integer id);
```

### 3.2 多表关联编程

```sql
-- 用户 + 订单关联查询
SELECT u.id AS user_id, u.username, o.id AS order_id, o.order_no
FROM t_user u
INNER JOIN t_order o ON u.id = o.user_id
WHERE u.is_delete = 0 AND o.is_delete = 0 AND u.id = 1;
```

```java
/** 多表结果 VO */
public class UserOrderVO {
    private Long userId;
    private String username;
    private Long orderId;
    private String orderNo;
    // getter / setter
}

/** Mapper 接口 */
List<UserOrderVO> selectUserOrder(Integer userId);
```

### 3.3 视图编程（简化开发）

```sql
-- 创建视图封装多表关联
CREATE VIEW v_user_order AS
SELECT u.id AS user_id, u.username, o.order_no, o.order_status
FROM t_user u
INNER JOIN t_order o ON u.id = o.user_id
WHERE u.is_delete = 0 AND o.is_delete = 0;

-- 查询视图（与查普通表一致）
SELECT * FROM v_user_order WHERE user_id = 1;
```

### 3.4 事务编程

```sql
START TRANSACTION;
INSERT INTO t_order (order_no, user_id, total_price, order_status)
VALUES ('20241001006', 1, 5999.00, 0);
INSERT INTO t_order_detail (order_id, goods_id, goods_name, goods_price, buy_num)
VALUES (6, 2, '华为Mate60', 5999.00, 1);
COMMIT;  -- 或 ROLLBACK（异常时）
```

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder(OrderDTO dto) {
    orderMapper.insertOrder(order);
    detailMapper.insertDetail(detail);
}
```

---

## 四、实战要点

### 4.1 命名映射规范

| 数据库 | Java |
|--------|------|
| 表名 `t_user` | 实体类 `User` |
| 字段 `user_id` | 属性 `userId`（驼峰） |
| 视图 `v_user_permission` | Mapper 查询源 |
| 存储过程 `proc_xxx` | MyBatis 调用 |

### 4.2 必须关闭自动提交

多表操作、事务场景必须关闭自动提交，Spring/MyBatis 框架会自动管理。

---

## 五、避坑总结

| 坑点 | 正确做法 |
|------|----------|
| **字段类型不匹配** | `BIGINT` → Java `Long`，`DECIMAL` → `BigDecimal` |
| **忘记逻辑删除筛选** | 所有查询加 `AND is_delete = 0` |
| **驱动类错误** | MySQL 8.0+ 用 `com.mysql.cj.jdbc.Driver` |
| **密码明文存储** | 必须加密后存储，配置中避免明文密码 |

---

## 六、企业级最佳实践

- **连接池**：使用 HikariCP（Spring Boot 默认）
- **SQL 规范**：关键字大写，表名字段名小写，`#{}` 参数绑定
- **事务控制**：`@Transactional` 注解，最小范围原则
- **视图优先**：高频多表查询封装为视图，简化后端代码
