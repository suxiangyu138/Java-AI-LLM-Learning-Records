# MySQL数据库设计（Java后端企业级实战版）

> **文档版本**：第四章：MySQL数据库设计（Java后端核心实战）
> **文档时间戳**：03.20 12:54
>
> **核心说明**：数据库设计是Java后端项目开发的基础，直接决定系统的性能、可扩展性和可维护性。好的数据库设计能减少冗余数据、避免业务异常，降低后续开发和维护成本；反之，不合理的设计会导致查询缓慢、数据混乱、接口报错等问题。本文基于前文"数据类型""约束""基本操作"知识点，讲解企业级数据库设计的原则、流程、实战技巧，结合电商、用户管理等常见业务场景，全程贴合Java后端开发需求，摒弃理论化冗余内容，聚焦实战落地。
>
> **前置基础**：需掌握前文"数据类型与约束"核心知识点（如主键、外键、字段类型选择）。数据库设计的核心是"贴合业务、规范合理、性能优先"，所有设计都要为Java后端接口开发、数据映射（如MyBatis）服务。

---

## 一、核心概念

### 1.1 数据库设计五大核心原则（企业级必守）

数据库设计需遵循 **5大核心原则**，贯穿设计全流程，是避免后期返工的关键，尤其适配Java后端"高可用、高并发、易维护"的需求。

#### 1.1.1 原子性原则（字段不可拆分）

**核心要求**：表中的每个字段都应是"不可拆分的最小单元"，避免一个字段存储多个含义的数据，否则会增加查询、修改的复杂度，也会导致Java实体类映射混乱。

**正确示例（用户表）**：拆分字段，每个字段含义单一。

```sql
CREATE TABLE t_user (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50)  NOT NULL UNIQUE,
    phone    VARCHAR(11)  UNIQUE,
    province VARCHAR(20), -- 省份（单独字段）
    city     VARCHAR(20)  -- 城市（单独字段）
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**错误示例（字段可拆分）**：address字段存储"省份-城市-详细地址"，无法单独查询某一地区。

```sql
CREATE TABLE t_user (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50)  NOT NULL,
    address  VARCHAR(255) -- 可拆分，不符合原子性
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Java后端关联**：原子性字段能直接对应Java实体类的单个属性，避免拆分字段的额外代码（如拆分address字符串），提升开发效率。

#### 1.1.2 规范性原则（命名、类型、约束统一）

**核心要求**：数据库、表、字段的命名、数据类型、约束需统一规范，便于团队协作和后期维护，这是企业级开发的基础（尤其多人协作项目）。

**企业级规范（Java后端适配版）：**

- **命名规范**：全部小写，下划线分隔，避免中文、拼音、特殊字符。
  - 数据库：`db_项目名`（如 `db_ecommerce`、`db_user_manage`）；
  - 表：`t_业务模块名`（如 `t_user`、`t_order`、`t_goods`），中间表用 `t_关联表1_关联表2`（如 `t_user_role`）；
  - 字段：含义清晰，避免缩写（如 `user_id` 为正确，`uid` 为错误），状态字段用 `is_` 前缀（如 `is_delete`、`is_enable`）。

- **数据类型规范**：按前文"数据类型"知识点，精准匹配Java类型（如主键用 `BIGINT` → Java `Long`，价格用 `DECIMAL(10,2)` → Java `BigDecimal`）。

- **约束规范**：每张表必加主键，核心字段（如用户名、商品名称）必加非空约束，去重字段（如手机号）必加唯一约束，关联表必加外键约束（高并发场景可灵活调整）。

#### 1.1.3 冗余最小原则（避免重复数据）

**核心要求**：尽量减少重复数据，相同的信息只存储一次，通过外键关联实现数据共享，避免数据冗余导致的修改异常（如修改一个数据，需同步修改多个地方）。

**正确示例（商品表+分类表）**：分类表存储分类信息只存一次，商品表通过 `category_id` 关联分类表，不重复存储分类名称。

```sql
-- 分类表（存储分类信息，只存一次）
CREATE TABLE t_category (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL UNIQUE
);

-- 商品表（通过 category_id 关联分类表，不重复存储分类名称）
CREATE TABLE t_goods (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_name  VARCHAR(100) NOT NULL,
    category_id BIGINT       NOT NULL,
    FOREIGN KEY (category_id) REFERENCES t_category(id)
);
```

**错误示例（冗余数据）**：商品表重复存储分类名称，修改分类名称时需同步修改所有商品记录。

```sql
CREATE TABLE t_goods (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_name    VARCHAR(100) NOT NULL,
    category_name VARCHAR(50)  NOT NULL -- 冗余，与分类表重复
);
```

> **注意**：冗余最小 ≠ 完全无冗余。企业级开发中，为提升查询效率，可适当保留少量冗余（如订单表存储用户名，避免关联查询），需权衡"冗余"与"性能"。

#### 1.1.4 可扩展性原则（适配需求迭代）

**核心要求**：数据库设计需预留扩展空间，适配后续需求迭代，避免频繁修改表结构（修改表结构会影响Java代码、接口，甚至导致线上故障）。

**企业级实战技巧：**

1. **预留备用字段**：核心表（如 `t_user`、`t_order`）可添加1-2个备用字段（如 `ext1 VARCHAR(100)`、`ext2 INT`），避免新增简单需求时修改表结构。
2. **使用JSON类型**：非固定结构的信息（如用户扩展信息、商品规格），用JSON类型存储（如前文的 `ext_info` 字段），无需新增字段。
3. **字段长度预留**：字符串字段长度适当预留（如用户名 `VARCHAR(50)`，避免后续需求调整长度），但不盲目设置过长（如 `VARCHAR(255)`）。

#### 1.1.5 性能优先原则（适配高并发）

**核心要求**：数据库设计需兼顾查询性能，尤其Java后端高频接口（如商品列表、订单查询），避免设计导致的查询缓慢。

**关键技巧：**

1. **主键用自增BIGINT**，避免用UUID（UUID无序，会降低插入和查询效率）。
2. **高频查询字段**（如 `username`、`order_no`）可添加索引（后续章节讲解）。
3. **大文本字段**（如商品描述）用 `TEXT` 类型，且不作为查询条件。
4. **高并发场景**（如电商订单），可拆分大表（如 `t_order` 拆分为 `t_order_main`（主信息）和 `t_order_detail`（详情））。

### 1.2 实体关系核心概念

将业务需求中的"实体"（如用户、商品、订单）、"属性"（如用户的用户名、商品的价格）、"关系"（如用户和订单是一对多关系）梳理清楚，用 **ER图**（实体关系图）呈现（企业级开发必做，便于团队沟通）。

**常见实体关系（Java后端高频）：**

| 关系类型 | 说明 | 示例 |
|----------|------|------|
| **一对多（1:N）** | 最常见的关系类型 | 一个用户可创建多个订单，一个订单只属于一个用户（用户 → 订单） |
| **多对多（N:M）** | 需创建中间表关联 | 一个用户可选择多门课程，一门课程可被多个用户选择（用户 → 课程），需创建中间表 `t_user_course` |
| **一对一（1:1）** | 通常用于拆分大表提升性能 | 一个用户对应一个用户详情（如 `t_user` 和 `t_user_detail`） |

---

## 二、底层原理

### 2.1 数据库设计核心流程（企业级方法论）

Java后端项目中，数据库设计需遵循 **"需求分析 → 概念设计 → 逻辑设计 → 物理设计 → 优化验证"** 5个步骤，避免盲目建表，确保设计贴合业务。

#### 2.1.1 步骤1：需求分析（核心前提）

**核心**：明确业务需求，梳理业务流程，确定需要存储的核心数据和业务规则，这是数据库设计的基础（设计前必须和产品、前端确认需求）。

**示例（电商用户管理需求）：**

- **核心数据**：用户基本信息（用户名、密码、手机号）、用户状态（是否删除、是否启用）、用户扩展信息（爱好、职业）；
- **业务规则**：用户名唯一、密码非空、手机号唯一、用户可注销（逻辑删除）；
- **关联业务**：用户可下单（关联订单表）、用户可收藏商品（关联收藏表）。

**Java后端视角**：需求分析时，需同步考虑Java实体类的设计，确保数据字段能覆盖实体类属性，避免后期实体类与表结构不匹配。

#### 2.1.2 步骤2：概念设计（梳理实体关系）

**核心**：将业务需求中的实体、属性、关系梳理清楚，用ER图呈现。实体关系类型（一对多、多对多、一对一）详见本章"核心概念"部分。

#### 2.1.3 步骤3：逻辑设计（设计表结构）

**核心**：将ER图转化为具体的表结构，确定每张表的字段、数据类型、约束、关联关系，这是数据库设计的核心步骤，也是Java后端实体类映射的直接依据。

**实战要点：**

1. 每个实体对应一张表，实体属性对应表的字段。
2. 根据实体关系，添加外键约束（一对多：从表加外键关联主表主键；多对多：中间表加两个外键，分别关联两个主表主键）。
3. 添加通用字段：所有表统一添加 `create_time`（创建时间）、`update_time`（更新时间），核心业务表添加 `is_delete`（逻辑删除），符合企业级规范。

#### 2.1.4 步骤4：物理设计（落地到MySQL）

**核心**：将逻辑设计的表结构转化为MySQL可执行的SQL语句，创建数据库、表，配置合适的存储引擎、字符集，完成物理落地。

**企业级物理设计规范：**

1. **存储引擎**：统一用 `InnoDB`（支持事务、外键，适配企业级业务）。
2. **字符集**：统一用 `utf8mb4`（支持中文、表情符号，避免乱码）。
3. **排序规则**：`utf8mb4_general_ci`（适配中文查询，性能较好）。
4. **执行SQL顺序**：先创建数据库，再创建主表，最后创建从表（避免外键关联报错）。

#### 2.1.5 步骤5：优化验证（避免问题）

**核心**：创建表后，验证表结构是否符合需求、约束是否齐全、关联是否正确，同时优化表结构（如调整字段类型、添加索引），避免后期出现问题。

**Java后端验证要点：**

1. 表结构与Java实体类是否一一对应（字段名、数据类型）；
2. 核心业务SQL（如查询、新增、修改）能否正常执行，无报错；
3. 高频查询场景（如用户登录、商品列表）是否有性能隐患。

### 2.2 底层技术原理

#### 2.2.1 存储引擎选择原理

**InnoDB** 是MySQL的默认存储引擎，其核心特性适配企业级业务场景：

- **事务支持**：保证ACID特性，确保数据一致性。
- **外键约束**：支持声明式外键，保证数据参照完整性。
- **行级锁**：高并发场景下减少锁冲突，提升吞吐量。
- **崩溃恢复**：支持崩溃安全恢复，保障数据不丢失。

#### 2.2.2 字符集与排序规则原理

- **utf8mb4**：支持完整的Unicode字符集，包括四字节字符（如表情符号），兼容所有语言的文字输入，避免乱码问题。
- **utf8mb4_general_ci**：基于通用Unicode排序规则，大小写不敏感，适配中文查询场景，性能较 `utf8mb4_unicode_ci` 更优。

#### 2.2.3 主键类型选择原理

| 主键类型 | 原理 | 企业级推荐 |
|----------|------|-----------|
| **自增BIGINT** | 顺序写入，B+树索引页分裂少，插入效率高；8字节整数，范围大，不易溢出 | 推荐 |
| **UUID** | 无序写入，B+树频繁页分裂，插入和查询效率低；占用存储空间大（32字节） | 不推荐 |

#### 2.2.4 价格字段选择原理

`DECIMAL(10,2)` 是精确数值类型，内部以字符串形式存储，不会出现浮点数精度丢失问题。Java后端对应 `BigDecimal` 类型，可精确表示货币金额。**切勿使用 `DOUBLE` 或 `FLOAT` 存储价格**，会产生精度丢失。

#### 2.2.5 外键约束机制原理

外键约束保证数据的参照完整性，其 `ON DELETE` 和 `ON UPDATE` 子句定义了引用动作的行为：

| 约束选项 | 行为说明 | 适用场景 |
|----------|---------|----------|
| `RESTRICT` | 禁止删除/更新被引用的父表记录（如果有子表记录引用） | 核心数据保护（如分类存在商品时禁止删除分类） |
| `CASCADE` | 父表记录删除/更新时，同步删除/更新子表中所有关联记录 | 级联操作（如用户删除时同步删除其订单） |
| `SET NULL` | 父表记录删除/更新时，子表关联字段设为NULL | 非核心关联（需确保字段允许NULL） |
| `NO ACTION` | 同RESTRICT，但检查时机略有不同（MySQL中等同RESTRICT） | — |

---

## 三、代码实现

### 3.1 数据库创建

```sql
-- 电商数据库，字符集 utf8mb4，排序规则适配中文
CREATE DATABASE IF NOT EXISTS db_ecommerce
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE db_ecommerce;
```

### 3.2 核心表设计（按主从关系创建）

#### 表1：用户表（t_user，主表）

```sql
CREATE TABLE IF NOT EXISTS t_user (
    -- 【主键字段】自增BIGINT，对应Java Long
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键ID（对应Java Long）',

    -- 【基础信息字段】非空、唯一，对应Java String
    username VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名（非空、唯一，对应Java String）',
    password VARCHAR(100) NOT NULL COMMENT '密码（加密存储，对应Java String）',
    phone    VARCHAR(11)  UNIQUE COMMENT '手机号（唯一，对应Java String）',
    email    VARCHAR(100) UNIQUE COMMENT '邮箱（唯一，对应Java String）',

    -- 【状态与扩展字段】
    gender   TINYINT  DEFAULT 0 COMMENT '性别：0=未知，1=男，2=女（对应Java Integer）',
    is_enable TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用（对应Java Boolean）',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除，1=已删除（对应Java Boolean）',
    ext_info JSON COMMENT '用户扩展信息（如爱好、职业，对应Java String/JSONObject）',

    -- 【通用时间字段】
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（对应Java LocalDateTime）',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（对应Java LocalDateTime）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户表';
```

#### 表2：商品分类表（t_category，主表）

```sql
CREATE TABLE IF NOT EXISTS t_category (
    -- 【主键字段】自增BIGINT，对应Java Long
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类主键ID（对应Java Long）',

    -- 【业务字段】
    category_name VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称（非空、唯一，对应Java String）',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID（0=一级分类，对应Java Long）',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序（对应Java Integer）',

    -- 【通用时间字段】
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（对应Java LocalDateTime）',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（对应Java LocalDateTime）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '商品分类表';
```

#### 表3：商品表（t_goods，从表，关联分类表）

```sql
CREATE TABLE IF NOT EXISTS t_goods (
    -- 【主键字段】自增BIGINT，对应Java Long
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品主键ID（对应Java Long）',

    -- 【基础信息字段】非空、唯一，对应Java String
    goods_name VARCHAR(100) NOT NULL COMMENT '商品名称（非空，对应Java String）',
    goods_code VARCHAR(50)  NOT NULL UNIQUE COMMENT '商品编码（唯一，对应Java String）',

    -- 【关联字段】外键关联分类表
    category_id BIGINT NOT NULL COMMENT '商品分类ID（关联分类表，对应Java Long）',

    -- 【核心业务字段】
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '商品价格（对应Java BigDecimal）',
    stock INT NOT NULL DEFAULT 0 COMMENT '商品库存（对应Java Integer）',

    -- 【状态与扩展字段】
    is_enable TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用，0=禁用（对应Java Boolean）',
    goods_desc TEXT COMMENT '商品描述（对应Java String）',
    ext_info JSON COMMENT '商品扩展信息（如规格、颜色，对应Java String/JSONObject）',

    -- 【通用时间字段】
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（对应Java LocalDateTime）',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（对应Java LocalDateTime）',

    -- 【外键约束】关联商品分类表
    CONSTRAINT fk_goods_category FOREIGN KEY (category_id) REFERENCES t_category(id)
        ON DELETE RESTRICT -- 分类存在商品时，禁止删除分类
        ON UPDATE CASCADE  -- 分类ID修改时，同步更新商品分类ID
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '商品表';
```

#### 表4：订单表（t_order，从表，关联用户表）

```sql
CREATE TABLE IF NOT EXISTS t_order (
    -- 【主键字段】自增BIGINT，对应Java Long
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单主键ID（对应Java Long）',

    -- 【业务标识字段】唯一，对应Java String
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号（唯一，对应Java String）',

    -- 【关联字段】外键关联用户表
    user_id BIGINT NOT NULL COMMENT '用户ID（关联用户表，对应Java Long）',

    -- 【核心业务字段】
    total_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额（对应Java BigDecimal）',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0=待支付，1=已支付，2=已取消（对应Java Integer）',
    pay_time DATETIME COMMENT '支付时间（对应Java LocalDateTime）',

    -- 【状态与扩展字段】
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除，1=已删除（对应Java Boolean）',

    -- 【通用时间字段】
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（对应Java LocalDateTime）',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（对应Java LocalDateTime）',

    -- 【外键约束】关联用户表
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES t_user(id)
        ON DELETE CASCADE -- 用户删除时，同步删除其订单
        ON UPDATE CASCADE -- 用户ID修改时，同步更新订单用户ID
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '订单表';
```

#### 表5：订单详情表（t_order_detail，从表，关联订单表、商品表）

```sql
CREATE TABLE IF NOT EXISTS t_order_detail (
    -- 【主键字段】自增BIGINT，对应Java Long
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '详情主键ID（对应Java Long）',

    -- 【关联字段】双外键，分别关联订单表和商品表
    order_id BIGINT NOT NULL COMMENT '订单ID（关联订单表，对应Java Long）',
    goods_id BIGINT NOT NULL COMMENT '商品ID（关联商品表，对应Java Long）',

    -- 【冗余字段】商品名称，提升查询效率（避免每次查询关联商品表）
    goods_name VARCHAR(100) NOT NULL COMMENT '商品名称（冗余，提升查询效率，对应Java String）',
    goods_price DECIMAL(10,2) NOT NULL COMMENT '商品单价（对应Java BigDecimal）',
    buy_num INT NOT NULL DEFAULT 1 COMMENT '购买数量（对应Java Integer）',

    -- 【通用时间字段】
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（对应Java LocalDateTime）',

    -- 【外键约束】关联订单表和商品表
    CONSTRAINT fk_detail_order FOREIGN KEY (order_id) REFERENCES t_order(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_detail_goods FOREIGN KEY (goods_id) REFERENCES t_goods(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '订单详情表';
```

---

## 四、实战要点

### 4.1 Java后端开发实战关联

**原子性字段与Java实体类映射**：原子性字段能直接对应Java实体类的单个属性，避免拆分字段的额外代码（如拆分address字符串），提升开发效率。

**需求分析的Java视角**：需求分析时，需同步考虑Java实体类的设计，确保数据字段能覆盖实体类属性，避免后期实体类与表结构不匹配。

**逻辑设计的Java映射**：将ER图转化为具体的表结构，确定每张表的字段、数据类型、约束、关联关系，这是数据库设计的核心步骤，也是Java后端实体类映射的直接依据。

**物理设计的Java适配**：创建SQL时严格遵循数据类型严格对应Java类型（如 `BIGINT` → `Long`、`DECIMAL` → `BigDecimal`），避免映射异常；每张表需包含合适的字段、数据类型、约束（主键、非空、唯一、外键）；确保表结构贴合Java实体类，关联关系正确，无冗余、无设计缺陷。

**优化验证的Java要点**：

1. 表结构与Java实体类是否一一对应（字段名、数据类型）。
2. 核心业务SQL（如查询、新增、修改）能否正常执行，无报错。
3. 高频查询场景（如用户登录、商品列表）是否有性能隐患。

### 4.2 扩展性实战技巧

1. **预留备用字段**：核心表（如 `t_user`、`t_order`）可添加1-2个备用字段（如 `ext1 VARCHAR(100)`、`ext2 INT`），避免新增简单需求时修改表结构。
2. **使用JSON类型**：非固定结构的信息（如用户扩展信息、商品规格），用JSON类型存储（如前文的 `ext_info` 字段），无需新增字段。
3. **字段长度预留**：字符串字段长度适当预留（如用户名 `VARCHAR(50)`，避免后续需求调整长度），但不盲目设置过长（如 `VARCHAR(255)`）。

### 4.3 性能实战技巧

1. **主键用自增BIGINT**，避免用UUID（UUID无序，会降低插入和查询效率）。
2. **高频查询字段**（如 `username`、`order_no`）可添加索引（后续章节讲解）。
3. **大文本字段**（如商品描述）用 `TEXT` 类型，且不作为查询条件。
4. **高并发场景**（如电商订单），可拆分大表（如 `t_order` 拆分为 `t_order_main`（主信息）和 `t_order_detail`（详情））。

### 4.4 冗余设计的实战权衡

> 冗余最小 ≠ 完全无冗余。企业级开发中，为提升查询效率，可适当保留少量冗余（如订单表存储用户名，避免关联查询），需权衡"冗余"与"性能"。

典型的冗余设计示例：订单详情表（`t_order_detail`）存储 `goods_name`（商品名称），避免查询订单详情时频繁关联商品表，显著提升查询效率。

### 4.5 物理设计实战规范

1. **存储引擎**：统一用 `InnoDB`（支持事务、外键，适配企业级业务）。
2. **字符集**：统一用 `utf8mb4`（支持中文、表情符号，避免乱码）。
3. **排序规则**：`utf8mb4_general_ci`（适配中文查询，性能较好）。
4. **执行SQL顺序**：先创建数据库，再创建主表，最后创建从表（避免外键关联报错）。

### 4.6 本章实战练习（Java后端视角）

**目标**：巩固数据库设计知识点，设计符合企业级规范的表结构，适配Java后端实体类和业务场景。

**需求说明**：基于"用户管理系统"需求，设计数据库 `db_user_system`（字符集utf8mb4）。

**业务实体梳理**：

- 用户（`t_user`）
- 角色（`t_role`）
- 权限（`t_permission`）

**实体关系分析**：

- 用户与角色：多对多（N:M）
- 角色与权限：多对多（N:M）

**表结构设计方案**：

- 共需设计5张表：`t_user`（用户表）、`t_role`（角色表）、`t_permission`（权限表）、`t_user_role`（用户角色中间表）、`t_role_permission`（角色权限中间表）；
- 每张表需包含合适的字段、数据类型、约束（主键、非空、唯一、外键）；
- 添加通用字段（`create_time`、`update_time`），核心表添加 `is_delete`；
- 确保表结构贴合Java实体类，关联关系正确，无冗余、无设计缺陷。

---

## 五、避坑总结

汇总企业级开发中数据库设计的常见错误，结合Java后端开发场景，给出避坑方案，避免后期返工。

### 5.1 字段类型选择错误

- **错误**：手机号用 `INT` 类型（导致以0开头的手机号被截断）、价格用 `DOUBLE` 类型（精度丢失）、主键用 `INT` 类型（数据量大会溢出）。
- **正确方案**：手机号用 `VARCHAR(11)`、价格用 `DECIMAL(10,2)`、主键用 `BIGINT` 自增，严格对应Java类型。

### 5.2 忽略逻辑删除，直接物理删除

- **错误**：删除用户、订单时，用 `DELETE` 语句物理删除，导致数据无法恢复，且无法追溯。
- **正确方案**：所有核心表添加 `is_delete` 字段（`TINYINT DEFAULT 0`），删除时用 `UPDATE` 语句修改 `is_delete=1`，Java接口查询时过滤 `is_delete=0` 的数据。

### 5.3 外键使用不当

- **错误**：外键关联非主键字段、外键未设置 `ON DELETE`/`ON UPDATE`、高并发场景滥用外键（导致锁表）。
- **正确方案**：外键仅关联主表主键，设置合理的 `ON DELETE`/`ON UPDATE`（如用户删除同步删除订单），高并发场景可取消外键，由Java代码校验关联完整性。

### 5.4 表结构过于冗余或过于拆分

- **错误**：一张表包含所有字段（如用户表包含订单信息），或过度拆分表（如用户表拆分为 `t_user`、`t_user_name`、`t_user_phone`）。
- **正确方案**：按实体拆分表，核心信息放在主表，非核心信息拆分到子表（如 `t_user` 和 `t_user_detail`），平衡冗余与拆分。

### 5.5 未预留扩展空间

- **错误**：字段长度刚好满足当前需求（如用户名 `VARCHAR(20)`）、无备用字段，需求迭代时频繁修改表结构。
- **正确方案**：字符串字段适当预留长度（如用户名 `VARCHAR(50)`），核心表添加备用字段或JSON字段，适配需求迭代。

---

## 六、企业级最佳实践

### 6.1 命名规范（企业级Java后端适配版）

| 分类 | 规范格式 | 示例 |
|------|---------|------|
| 数据库 | `db_项目名` | `db_ecommerce`、`db_user_manage` |
| 表 | `t_业务模块名` | `t_user`、`t_order`、`t_goods` |
| 中间表 | `t_关联表1_关联表2` | `t_user_role`、`t_user_course` |
| 字段 | 全小写+下划线，含义清晰 | `user_id`（正确）、`uid`（错误） |
| 状态字段 | `is_` 前缀 | `is_delete`、`is_enable` |

### 6.2 数据类型规范（Java类型映射）

| MySQL类型 | Java类型 | 应用场景 |
|-----------|---------|----------|
| `BIGINT` | `Long` | 主键ID、外键关联ID |
| `VARCHAR` | `String` | 用户名、手机号、邮箱等字符串 |
| `DECIMAL(10,2)` | `BigDecimal` | 价格、金额等精确数值 |
| `INT` | `Integer` | 数量、排序、状态码 |
| `TINYINT` | `Integer` / `Boolean` | 状态标记（0/1）、性别 |
| `DATETIME` | `LocalDateTime` | 创建时间、更新时间 |
| `JSON` | `String` / `JSONObject` | 扩展信息、非固定结构数据 |
| `TEXT` | `String` | 大文本描述（不作为查询条件） |

### 6.3 约束规范

| 约束类型 | 规范要求 | 示例场景 |
|---------|---------|----------|
| **主键约束** | 每张表必加主键，统一使用自增 `BIGINT` | `id BIGINT PRIMARY KEY AUTO_INCREMENT` |
| **非空约束** | 核心字段必加非空约束 | 用户名、商品名称等必填字段 |
| **唯一约束** | 去重字段必加唯一约束 | 手机号、邮箱、商品编码等 |
| **外键约束** | 关联表必加外键约束，设置合理的 `ON DELETE`/`ON UPDATE` | 高并发场景可灵活调整，由Java代码校验 |

### 6.4 通用字段规范（所有表统一添加）

```sql
-- 【所有业务表必须包含】创建时间与更新时间
create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（对应Java LocalDateTime）',
update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（对应Java LocalDateTime）'
```

```sql
-- 【核心业务表额外添加】逻辑删除标记
is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0=未删除，1=已删除（对应Java Boolean）'
```

### 6.5 电商核心表设计要点总结

以下是对电商核心表设计中关键实践点的总结：

| 设计维度 | 实践要点 |
|---------|---------|
| **关联关系** | 用户→订单（1:N）、分类→商品（1:N）、订单→订单详情（1:N）、商品→订单详情（1:N），所有关联都通过外键实现 |
| **数据类型** | 严格对应Java实体类（如 `BIGINT` → `Long`、`DECIMAL` → `BigDecimal`），避免映射异常 |
| **冗余设计** | 订单详情表存储 `goods_name`（商品名称），避免查询订单详情时关联商品表，提升查询效率 |
| **通用字段** | 所有表都有 `create_time`、`update_time`，核心表有 `is_delete`，符合企业级规范 |
| **约束齐全** | 主键、非空、唯一、外键约束齐全，保证数据完整性 |

### 6.6 数据库设计核心总结

- **核心原则**：牢记 **原子性、规范性、冗余最小、可扩展性、性能优先** 5大原则，是数据库设计的核心。
- **核心流程**：**需求分析 → 概念设计 → 逻辑设计 → 物理设计 → 优化验证**，避免盲目建表。
- **实战关键**：表结构需贴合Java实体类，关联关系清晰，约束齐全，预留扩展空间，避开常见坑。
- **后续衔接**：下一章讲解SQL高级查询（关联查询、聚合查询等），结合本章设计的表结构，实现复杂业务查询，适配Java后端接口开发。
