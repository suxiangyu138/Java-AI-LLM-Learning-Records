# 06 数据库与 SQL 规范

> SQL 是后端开发的"第二语言"——一条 SELECT * 可能拖垮整个数据库，一个缺失的索引可能让查询从毫秒变秒级

---

## 📚 目录

1. [表设计规约](#1-表设计规约)
2. [索引设计规约](#2-索引设计规约)
3. [SQL 编写规范](#3-sql-编写规范)
4. [ORM 映射规范](#4-orm-映射规范)
5. [分页查询规范](#5-分页查询规范)
6. [事务规范](#6-事务规范)
7. [连接池规范](#7-连接池规范)
8. [SQL 注入防御](#8-sql-注入防御)
9. [常见反模式](#9-常见反模式)
10. [动手练习](#10-动手练习)

---

## 1. 表设计规约

### 1.1 必备字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED AUTO_INCREMENT | 主键，自增 |
| create_time | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| is_deleted | TINYINT NOT NULL DEFAULT 0 | 逻辑删除（0:未删 1:已删） |

```sql
CREATE TABLE `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `email` VARCHAR(128) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 1.2 命名规范

| 对象 | 规范 | 示例 |
|------|------|------|
| 表名 | 小写下划线，单数 | `user` / `order_item` |
| 字段名 | 小写下划线 | `user_name` / `create_time` |
| 布尔字段 | is_ 前缀 | `is_deleted` / `is_active` |
| 索引 | idx_ 前缀 | `idx_user_name` |
| 唯一索引 | uk_ 前缀 | `uk_email` |

### 1.3 字段类型选型

| 场景 | 推荐类型 | 避免 |
|------|---------|------|
| 短字符串 | VARCHAR(n) | CHAR（浪费空间） |
| 金额 | DECIMAL(10,2) | FLOAT/DOUBLE（精度丢失） |
| 状态码 | TINYINT UNSIGNED | INT（浪费空间） |
| 时间戳 | DATETIME | TIMESTAMP（2038 问题） |
| 主键 | BIGINT UNSIGNED | INT（容量不够） |

---

## 2. 索引设计规约

### 2.1 最左前缀原则

```sql
-- 联合索引：idx_user_id_order_status (user_id, order_status)

-- ✅ 命中索引
SELECT * FROM `order` WHERE user_id = 1;
SELECT * FROM `order` WHERE user_id = 1 AND order_status = 0;

-- ❌ 不命中索引
SELECT * FROM `order` WHERE order_status = 0;  -- 缺少最左列
```

### 2.2 索引失效场景

```sql
-- ❌ 函数操作
SELECT * FROM `user` WHERE DATE(create_time) = '2024-01-01';

-- ✅ 改范围查询
SELECT * FROM `user` WHERE create_time >= '2024-01-01' AND create_time < '2024-01-02';

-- ❌ 隐式类型转换
SELECT * FROM `user` WHERE phone = 13800138000;  -- phone 是 VARCHAR，传入数字

-- ✅ 类型一致
SELECT * FROM `user` WHERE phone = '13800138000';

-- ❌ 前导模糊查询
SELECT * FROM `user` WHERE name LIKE '%张';

-- ✅ 后缀模糊查询
SELECT * FROM `user` WHERE name LIKE '张%';
```

### 2.3 覆盖索引

```sql
-- 索引：idx_user_name_email (user_name, email)

-- ✅ 覆盖索引（只查索引列，不回表）
SELECT user_name, email FROM `user` WHERE user_name = '张三';

-- ❌ 需要回表（查了不在索引里的列）
SELECT * FROM `user` WHERE user_name = '张三';
```

---

## 3. SQL 编写规范

### 3.1 禁止 SELECT *

```sql
-- ❌ 问题：网络 IO 大、无法覆盖索引、字段变更风险
SELECT * FROM `user`;

-- ✅ 只查需要的字段
SELECT id, user_name, email FROM `user`;
```

### 3.2 避免在 WHERE 中对字段做函数操作

```sql
-- ❌ 索引失效
SELECT * FROM `user` WHERE YEAR(create_time) = 2024;

-- ✅ 改范围查询
SELECT * FROM `user` WHERE create_time >= '2024-01-01' AND create_time < '2025-01-01';
```

### 3.3 避免 OR 条件

```sql
-- ❌ 索引失效
SELECT * FROM `user` WHERE user_name = '张三' OR email = 'test@test.com';

-- ✅ 用 UNION
SELECT * FROM `user` WHERE user_name = '张三'
UNION
SELECT * FROM `user` WHERE email = 'test@test.com';
```

### 3.4 大批量数据分批处理

```sql
-- ❌ 一次性处理大量数据
UPDATE `user` SET is_deleted = 1 WHERE last_login < '2023-01-01';

-- ✅ 分批处理
UPDATE `user` SET is_deleted = 1 WHERE last_login < '2023-01-01' LIMIT 1000;
-- 循环执行直到影响行数为 0
```

---

## 4. ORM 映射规范

### 4.1 MyBatis #{} vs ${}

```xml
<!-- ✅ 参数化查询，防注入 -->
<select id="findById">
    SELECT * FROM `user` WHERE id = #{id}
</select>

<!-- ❌ 字符串拼接，有注入风险 -->
<select id="findByTable">
    SELECT * FROM ${tableName}
</select>

<!-- ✅ 白名单校验后使用 ${} -->
<select id="findByTable">
    <if test="tableName == 'user' or tableName == 'order'">
        SELECT * FROM ${tableName}
    </if>
</select>
```

### 4.2 避免 N+1 查询

```java
// ❌ N+1 问题
List<Order> orders = orderMapper.findAll();
for (Order order : orders) {
    User user = userMapper.findById(order.getUserId());  // 每次循环查一次
}

// ✅ JOIN 查询
<select id="findAllWithUser">
    SELECT o.*, u.user_name
    FROM `order` o
    LEFT JOIN `user` u ON o.user_id = u.id
</select>
```

### 4.3 批量插入优化

```xml
<!-- ✅ 批量插入 -->
<insert id="batchInsert">
    INSERT INTO `user` (user_name, email) VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.userName}, #{user.email})
    </foreach>
</insert>
```

---

## 5. 分页查询规范

### 5.1 分页必须带排序

```sql
-- ❌ 分页结果不稳定
SELECT * FROM `order` LIMIT 10, 10;

-- ✅ 带排序
SELECT * FROM `order` ORDER BY id DESC LIMIT 10, 10;
```

### 5.2 深分页优化

```sql
-- ❌ 深分页性能差（扫描 10010 条，丢弃前 10000 条）
SELECT * FROM `order` ORDER BY id DESC LIMIT 10000, 10;

-- ✅ 游标分页
SELECT * FROM `order` WHERE id > #{lastId} ORDER BY id DESC LIMIT 10;
```

---

## 6. 事务规范

### 6.1 @Transactional 失效场景

```java
// ❌ 自调用：同一个类内方法调用，事务失效
@Service
public class OrderService {
    @Transactional
    public void createOrder() {
        doCreate();
    }
    
    public void doCreate() {
        createOrder();  // ❌ 事务失效
    }
}

// ✅ 拆分到不同类
@Service
public class OrderService {
    @Autowired
    private OrderManager orderManager;
    
    public void doCreate() {
        orderManager.createOrder();  // ✅ 事务生效
    }
}
```

### 6.2 长事务的危害

```java
// ❌ 长事务：锁持有时间长、连接占用
@Transactional
public void processOrder(Order order) {
    doSomething();        // 100ms
    callRemoteService();  // 2s（RPC 调用）
    doSomethingElse();    // 100ms
}
// 事务耗时 2.2s，连接被占用 2.2s

// ✅ 拆分事务
public void processOrder(Order order) {
    doSomething();
    doInTransaction(order);  // 只把 DB 操作包在事务里
    callRemoteService();
}

@Transactional
public void doInTransaction(Order order) {
    // DB 操作
}
```

---

## 7. 连接池规范

### 7.1 HikariCP 参数配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20          # 最大连接数
      minimum-idle: 5                # 最小空闲
      connection-timeout: 30000      # 连接超时 30s
      idle-timeout: 600000           # 空闲超时 10min
      max-lifetime: 1800000          # 连接最大生命周期 30min
      leak-detection-threshold: 60000  # 泄漏检测 60s
```

### 7.2 连接池大小计算

```text
核心公式：connections = ((core_count * 2) + effective_spindle_count)

示例：4 核 CPU，SSD（spindle=0）
connections = (4 * 2) + 0 = 8

实际推荐：10-20，根据压测调整
```

---

## 8. SQL 注入防御

### 8.1 参数化查询

```java
// ✅ PreparedStatement
String sql = "SELECT * FROM user WHERE id = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setLong(1, userId);
ResultSet rs = ps.executeQuery();

// ❌ 字符串拼接
String sql = "SELECT * FROM user WHERE id = " + userId;  // 危险！
```

### 8.2 动态表名白名单

```java
// ❌ 危险
String sql = "SELECT * FROM " + tableName;

// ✅ 白名单校验
Set<String> allowedTables = Set.of("user", "order", "product");
if (!allowedTables.contains(tableName)) {
    throw new IllegalArgumentException("非法表名");
}
String sql = "SELECT * FROM " + tableName;
```

---

## 9. 常见反模式

| 反模式 | 问题 | 正确做法 |
|--------|------|---------|
| SELECT * | 网络 IO 大、索引失效 | 指定字段 |
| 在循环里执行 SQL | N+1 问题 | JOIN 或批量查询 |
| 大事务包小事务 | 连接占用时间长 | 拆分事务 |
| 事务里调用 RPC | 外部调用慢导致连接占用 | RPC 放事务外 |
| 索引字段做函数操作 | 索引失效 | 改范围查询 |
| 深分页直接用 LIMIT offset | 性能差 | 游标分页 |
| SQL 拼接用户输入 | SQL 注入 | 参数化查询 |
| 一次性处理大量数据 | 锁时间长、内存大 | 分批处理 |

---

## 10. 动手练习

### 练习 1：优化 SQL

```sql
-- 问题：这条 SQL 有什么问题？如何优化？
SELECT * FROM `order` WHERE DATE_FORMAT(create_time, '%Y-%m-%d') = '2024-01-01' ORDER BY id LIMIT 100000, 10;
```

<details>
<summary>参考答案</summary>

问题：
1. SELECT * 查所有字段
2. DATE_FORMAT 函数操作导致索引失效
3. 深分页性能差

优化：
```sql
SELECT id, order_no, user_id, amount
FROM `order`
WHERE create_time >= '2024-01-01' AND create_time < '2024-01-02'
ORDER BY id DESC
LIMIT 10;

-- 或使用游标分页
SELECT id, order_no, user_id, amount
FROM `order`
WHERE id > #{lastId} AND create_time >= '2024-01-01' AND create_time < '2024-01-02'
ORDER BY id DESC
LIMIT 10;
```

</details>

---

**上一模块**：[05-异常处理与日志规范](./05-异常处理与日志规范.md)  
**下一模块**：[07-安全与性能规范](./07-安全与性能规范.md)  
**返回总览**：[00-Java代码规范知识体系总览](./00-Java代码规范知识体系总览.md)
