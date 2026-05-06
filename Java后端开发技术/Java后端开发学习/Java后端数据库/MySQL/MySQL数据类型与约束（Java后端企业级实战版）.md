# MySQL 数据类型与约束（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MySQL 数据类型与约束  
> **核心原则**：按需选择、精准匹配 — 不浪费存储，不导致溢出  
> **前置基础**：MySQL 数据库与表的创建

---

## 一、核心概念

### 1.1 数据类型与约束的作用

数据类型和约束是 MySQL 表设计的 **灵魂**，直接决定数据存储的合理性、安全性和查询效率，更是 Java 后端实体类与数据库表映射的关键。

### 1.2 与 Java 后端的关联

所有 MySQL 数据类型均对应 Java 实体类的具体类型，是后续 MyBatis 映射、接口开发的基础。

---

## 二、底层原理

### 2.1 数据类型选择原则

```
TINYINT（1 字节） → INT（4 字节） → BIGINT（8 字节）
     年龄/状态          普通 ID          主键/订单号

VARCHAR（可变长） → CHAR（定长） → TEXT（大文本）
   用户名/手机号       身份证号          文章内容

DATETIME（8 字节） → DATE（3 字节） → TIMESTAMP（4 字节）
  首选通用时间           仅日期           有 2038 溢出风险
```

---

## 三、代码实现

### 3.1 数值类型

| MySQL 类型 | Java 类型 | 使用场景 | 注意 |
|------------|----------|----------|------|
| `TINYINT` | `Integer` | 状态标记（`is_delete`）、性别 | 常用 `UNSIGNED`（0-255） |
| `INT` | `Integer` | 年龄、库存、普通 ID（< 2000 万） | 最常用整数类型 |
| `BIGINT` | `Long` | 主键 ID、订单号、时间戳 | 后端主键首选，避免 INT 溢出 |
| `DECIMAL(M,D)` | `BigDecimal` | 商品价格、金额、手续费 | **禁止用 FLOAT/DOUBLE**（精度丢失） |

```sql
-- 商品价格：DECIMAL(10,2) 适配元角分
price DECIMAL(10,2) NOT NULL COMMENT '商品价格（元）'

-- 主键：BIGINT 自增
id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID'
```

> Java 金额计算必须用 `BigDecimal`，避免 `Double` 导致 `0.1 + 0.2 ≠ 0.3` 的问题。

### 3.2 字符串类型

| MySQL 类型 | Java 类型 | 长度限制 | 使用场景 | 避坑 |
|------------|----------|----------|----------|------|
| `VARCHAR(M)` | `String` | M ≤ 65535 | **用户名(50)**、**手机号(11)**、邮箱(100)、地址(255) | 必须指定长度；手机号用 VARCHAR(11) 不用 INT |
| `CHAR(M)` | `String` | M ≤ 255 | 身份证号(18)、固定长度编码 | 仅固定长度场景 |
| `TEXT` | `String` | ≤ 65535 | 商品描述、文章内容 | 不适合做查询条件，禁止做主键 |

```sql
username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
phone    VARCHAR(11) UNIQUE COMMENT '手机号（固定11位）',
email    VARCHAR(100) COMMENT '邮箱',
address  VARCHAR(255) COMMENT '地址',
description TEXT COMMENT '商品描述（长文本）'
```

### 3.3 日期时间类型

| MySQL 类型 | Java 类型 | 格式 | 使用场景 |
|------------|----------|------|----------|
| **`DATETIME`** | `LocalDateTime` | `YYYY-MM-DD HH:MM:SS` | **创建时间、更新时间（首选）** |
| `DATE` | `LocalDate` | `YYYY-MM-DD` | 出生日期、生产日期 |
| `TIMESTAMP` | `LocalDateTime` | `YYYY-MM-DD HH:MM:SS` | 企业级很少用（2038 年溢出风险） |

```sql
-- 企业级表必加字段
create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
```

### 3.4 特殊类型

| 类型 | 对应 Java | 场景 |
|------|----------|------|
| `BOOLEAN`（= TINYINT(1)） | `Boolean` | 开关、状态 |
| `JSON`（MySQL 8.0+） | `String` / `JSONObject` | 非固定结构扩展信息 |

```sql
-- JSON 类型示例
ALTER TABLE t_user ADD COLUMN ext_info JSON COMMENT '用户扩展信息';
INSERT INTO t_user (username, password, ext_info)
VALUES ('zhangsan', '123456', '{"hobby":"game","job":"developer"}');
```

### 3.5 五大核心约束

#### 主键约束（PRIMARY KEY）

```sql
-- 自增主键（企业级首选）
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    ...
);
```

#### 非空约束（NOT NULL）

```sql
username VARCHAR(50) NOT NULL COMMENT '用户名（必填）',
password VARCHAR(100) NOT NULL COMMENT '密码（必填）'
```

#### 唯一约束（UNIQUE）

```sql
username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（唯一）',
phone    VARCHAR(11) UNIQUE COMMENT '手机号（唯一）'
```

#### 默认值约束（DEFAULT）

```sql
gender    TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女',
is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删'
```

#### 外键约束（FOREIGN KEY）

```sql
-- 可选：企业级开发中部分团队禁用物理外键，改用应用层维护关联关系
FOREIGN KEY (user_id) REFERENCES t_user(id)
```

| 约束 | 作用 | 企业级用法 |
|------|------|-----------|
| `PRIMARY KEY` | 唯一标识每条记录 | 每表必须有，推荐 `BIGINT AUTO_INCREMENT` |
| `NOT NULL` | 禁止空值 | 核心字段（username、password）必加 |
| `UNIQUE` | 禁止重复 | 用户名、手机号、订单号 |
| `DEFAULT` | 默认值 | 状态字段、逻辑删除、时间字段 |
| `FOREIGN KEY` | 关联其他表 | 可选（部分团队禁用物理外键） |

---

## 四、实战要点

### 4.1 Java 类型映射速查

| MySQL | Java | MyBatis 映射 |
|-------|------|-------------|
| `BIGINT` | `Long` | 自动映射 |
| `INT` / `TINYINT` | `Integer` | 自动映射 |
| `VARCHAR` / `TEXT` | `String` | 自动映射 |
| `DECIMAL` | `BigDecimal` | 自动映射 |
| `DATETIME` | `LocalDateTime` | 自动映射（JDK 8+） |
| `DATE` | `LocalDate` | 自动映射 |
| `BOOLEAN` | `Boolean` | `TINYINT(1)` 自动转 |
| `JSON` | `String` | 手动序列化 |

### 4.2 关键设计规范

- 主键统一用 `BIGINT AUTO_INCREMENT`
- 金额统一用 `DECIMAL(10,2)`
- 手机号用 `VARCHAR(11)`，不用 INT
- 所有表加 `create_time` + `update_time`
- 所有表加 `is_delete`（`TINYINT DEFAULT 0`）
- 字符集统一 `utf8mb4`，引擎统一 `InnoDB`

---

## 五、避坑总结

| 坑点 | 错误 | 正确 |
|------|------|------|
| **价格用 FLOAT/DOUBLE** | 精度丢失 | `DECIMAL(10,2)` + Java `BigDecimal` |
| **手机号用 INT** | 溢出 / 0 开头截断 | `VARCHAR(11)` |
| **主键用 INT** | 2000 万+ 溢出 | `BIGINT AUTO_INCREMENT` |
| **短文本用 TEXT** | 查询效率低 | `VARCHAR(M)` 指定长度 |
| **时间用 TIMESTAMP** | 2038 年溢出 | `DATETIME` |
| **缺 is_delete 字段** | 无法逻辑删除 | 所有表必须加 |

---

## 六、企业级最佳实践

### 6.1 表设计模板

```sql
CREATE TABLE IF NOT EXISTS t_xxx (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    -- 业务字段
    is_delete   TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表说明';
```

### 6.2 设计 Checklist

- [ ] 主键使用 `BIGINT AUTO_INCREMENT`
- [ ] 金额字段使用 `DECIMAL`（非 FLOAT）
- [ ] 手机号/固定长度编号使用 `VARCHAR`
- [ ] 所有表含 `is_delete` + `create_time` + `update_time`
- [ ] 字符集 `utf8mb4` + 引擎 `InnoDB`
- [ ] 所有字段有中文 `COMMENT`
- [ ] 数据类型与 Java 实体类类型一致
