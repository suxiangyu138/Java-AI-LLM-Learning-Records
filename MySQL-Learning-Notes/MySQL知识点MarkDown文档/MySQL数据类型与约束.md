# MySQL 数据类型 + 约束 完整核心笔记
## 一、MySQL 常用数据类型
### 1. 数值类型
#### （1）整型
| 类型 | 范围特点 | 适用场景 |
|------|----------|----------|
| `TINYINT` | 1字节 | 状态、性别、开关 |
| `SMALLINT` | 2字节 | 小规模数字 |
| `INT` | 4字节 | 常规ID、年龄、数量（最常用） |
| `BIGINT` | 8字节 | 订单号、雪花ID、大数值 |

#### （2）浮点/定点型
- `FLOAT`：单精度浮点数，精度低，不推荐存金额
- `DOUBLE`：双精度浮点数
- `DECIMAL(m, n)`：**定点数**，m总长度，n小数位数
  > 金融、金额、账单**必须使用 DECIMAL**，避免精度丢失

---

### 2. 字符串类型
| 类型 | 特点 | 适用 |
|------|------|------|
| `CHAR(n)` | 固定长度，存取快 | 手机号、身份证、固定编码 |
| `VARCHAR(n)` | 可变长度，节省空间 | 用户名、地址、文本描述（最常用） |
| `TEXT` | 大文本，无固定限制 | 文章内容、长备注 |

> 区别：
> `CHAR` 长度不足自动补空格；`VARCHAR` 按需占用空间。

---

### 3. 日期时间类型
- `DATE`：`yyyy-MM-dd` 仅日期
- `TIME`：`HH:mm:ss` 仅时间
- `DATETIME`：`yyyy-MM-dd HH:mm:ss` 常用时间字段
- `TIMESTAMP`：时间戳，范围较小，受时区影响

---

### 4. 枚举与集合
- `ENUM('值1','值2')`：单选，如性别、状态
- `SET('值1','值2')`：多选

---

## 二、六大完整性约束
作用：**保证数据表数据合法、完整、有效**

### 1. 主键约束 `PRIMARY KEY`
- 特点：**非空 + 唯一**
- 一张表只能有一个主键
- 常用搭配：`AUTO_INCREMENT` 自增
```sql
id INT PRIMARY KEY AUTO_INCREMENT
```

### 2. 非空约束 `NOT NULL`
- 字段**不能为空**，必须赋值
- 例：用户名、密码、手机号

### 3. 唯一约束 `UNIQUE`
- 字段值**不能重复**，允许为空
- 适用：手机号、邮箱、账号

### 4. 默认约束 `DEFAULT`
- 字段不赋值时，自动使用默认值
```sql
age INT DEFAULT 18
```

### 5. 外键约束 `FOREIGN KEY`
- 用于**多表关联**，保证参照完整性
- 单表开发极少使用，企业开发常逻辑外键替代

### 6. 检查约束 `CHECK`
- 限制字段取值范围
- 例：年龄必须大于0
```sql
age INT CHECK (age > 0)
```

---

## 三、约束组合示例（可直接运行）
```sql
CREATE TABLE student (
    id INT PRIMARY KEY AUTO_INCREMENT,
    stu_name VARCHAR(20) NOT NULL,
    phone VARCHAR(11) UNIQUE,
    age INT DEFAULT 18 CHECK(age >= 0),
    create_time DATETIME
);
```

---

## 四、高频面试区分点
1. **主键 vs 唯一**
    - 主键：非空+唯一，整张表唯一
    - 唯一：仅唯一，可以为空，一张表可多个
2. **CHAR vs VARCHAR**
    - 固定长度 / 可变长度
3. **DOUBLE vs DECIMAL**
    - 小数精度：DECIMAL 无精度丢失，金额专用
4. **WHERE 与 CHECK**
    - WHERE 过滤查询结果；CHECK 限制字段存入数据

