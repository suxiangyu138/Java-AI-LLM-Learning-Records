# 02 Statement 三兄弟

> Statement、PreparedStatement、CallableStatement——预编译为什么防注入、怎么选型，是 JDBC 面试的第一道深度题

---

## 📚 目录

1. [三兄弟总览](#1-三兄弟总览)
2. [Statement：静态 SQL（基本不用）](#2-statement静态-sql基本不用)
3. [PreparedStatement：预编译 + 防注入（首选）](#3-preparedstatement预编译--防注入首选)
4. [SQL 注入原理与预编译机制](#4-sql-注入原理与预编译机制)
5. [CallableStatement：存储过程](#5-callablestatement存储过程)
6. [三兄弟选型速查](#6-三兄弟选型速查)

---

## 1. 三兄弟总览

| 类型 | 用途 | SQL 形式 | 是否预编译 | 场景 |
|------|------|---------|:---------:|------|
| Statement | 静态 SQL | 拼接字符串 | ❌ | **基本不用**（注入风险） |
| **PreparedStatement** | 参数化 SQL | `?` 占位符 | ✅ | **首选（99%）** |
| CallableStatement | 存储过程 | `{call proc(?,?)}` | ✅ | 存储过程调用 |

**三兄弟的继承关系**：`Statement ← PreparedStatement ← CallableStatement`（子类继承父类能力）。

> 🎯 **核心要点**：三兄弟 = "**Statement 静态 + PreparedStatement 预编译 + CallableStatement 存储过程**"——**现代代码 99% 用 PreparedStatement**（Statement 是历史，Callable 是存储过程专用）。

---

## 2. Statement：静态 SQL（基本不用）

```java
// Statement：SQL 字符串直接拼接（⚠️ 注入风险）
try (Connection conn = ...;
     Statement stmt = conn.createStatement()) {

    // 查询
    ResultSet rs = stmt.executeQuery("SELECT * FROM user WHERE name = '" + name + "'");
    // ⚠️ name 为 "x' OR '1'='1" → 全表返回！

    // 增删改
    int rows = stmt.executeUpdate("UPDATE user SET age = " + age + " WHERE id = " + id);
}
```

**Statement 的三大问题**：

| 问题 | 说明 |
|------|------|
| **SQL 注入** | 字符串拼接 → 恶意输入可改写 SQL |
| 无预编译 | 每次执行都解析 SQL（性能差） |
| 拼接易错 | 引号/转义手动处理（SQL 语法错误高发） |

```text
Statement 的存在意义（仅两个场景）：
  ① 无参数的 DDL（CREATE TABLE 等）
  ② 动态列名/表名（无法用占位符）
→ 其余一律 PreparedStatement
```

> 🎯 **核心要点**：Statement = "**静态拼接 SQL（注入 + 性能双输）**"——**"除无参数 DDL 与动态表名外不用 Statement"**是铁律；面试答"Statement 什么时候用"给这两个例外 = 深度分。

---

## 3. PreparedStatement：预编译 + 防注入（首选）

```java
// PreparedStatement：参数化 SQL（? 占位符）
String sql = "SELECT * FROM user WHERE name = ? AND age > ?";

try (Connection conn = ...;
     PreparedStatement ps = conn.prepareStatement(sql)) {   // ① 预编译 SQL

    ps.setString(1, name);        // ② 参数按序绑定（类型安全）
    ps.setInt(2, minAge);

    try (ResultSet rs = ps.executeQuery()) {   // ③ 执行（无参数）
        while (rs.next()) { ... }
    }
}

// 插入并取回自增主键
String insertSql = "INSERT INTO user (name) VALUES (?)";
try (PreparedStatement ps = conn.prepareStatement(insertSql,
        Statement.RETURN_GENERATED_KEYS)) {
    ps.setString(1, "张三");
    ps.executeUpdate();
    try (ResultSet keys = ps.getGeneratedKeys()) {   // ④ 自增主键
        if (keys.next()) {
            long id = keys.getLong(1);
        }
    }
}
```

**PreparedStatement 的优势**：

| 优势 | 机制 |
|------|------|
| 防注入 | 参数走预编译（值不参与 SQL 解析） |
| 预编译性能 | SQL 只解析一次（同 SQL 复用执行计划） |
| 类型安全 | setString/setInt 类型绑定（SQL 语法错误前置） |
| 可读性 | SQL 与参数分离（? 占位符） |

> 🎯 **核心要点**：PreparedStatement = "**预编译 + 参数化（防注入/性能/类型安全）**"——**"一切有参数的 SQL 用它"是 JDBC 第一铁律**；`getGeneratedKeys` 取自增主键是高频配套。

---

## 4. SQL 注入原理与预编译机制

**注入的原理**（面试必画）：

```text
攻击输入：name = "x' OR '1'='1"

拼接后 SQL：
  SELECT * FROM user WHERE name = 'x' OR '1'='1'
                                    ↑ 恒真 → 全表返回！

拼接后（删除场景）：
  DELETE FROM user WHERE id = 1 OR '1'='1'   → 全表删除！

预编译的防护：
  SELECT * FROM user WHERE name = ?
  参数 "x' OR '1'='1" 作为【值】传给数据库
  → 数据库只把它当作字符串值（'x'' OR ''1''=''1'）
  → 引号被转义/参数化 → 无法改变 SQL 结构
```

**预编译的两层含义**：

```text
① 安全层：参数是"值"不是"代码"（SQL 结构在执行前已固定）
② 性能层：SQL 解析一次 → 执行计划复用（同结构 SQL）
   → 与 MySQL 的 prepare 语句（二进制协议预编译）配合
   → 注意：JDBC 预编译发生在驱动/服务器（useServerPrepStmts 参数）
```

> 🎯 **核心要点**：注入原理 = "**拼接让用户输入成为 SQL 代码**"——**预编译让参数永远是"值"**（结构固定）；面试答"为什么防注入"用"SQL 结构在执行前已确定"一句话 + 恒真示例。

---

## 5. CallableStatement：存储过程

```java
// CallableStatement：调用存储过程
// 存储过程：CREATE PROCEDURE get_user_count(IN min_age INT, OUT cnt INT)
String call = "{call get_user_count(?, ?)}";

try (Connection conn = ...;
     CallableStatement cs = conn.prepareCall(call)) {

    cs.setInt(1, 18);              // IN 参数
    cs.registerOutParameter(2, Types.INTEGER);   // OUT 参数注册

    cs.execute();

    int count = cs.getInt(2);      // 读取 OUT 参数
}
```

**CallableStatement 的要点**：

```text
① 语法：{call 过程名(?, ?)}（占位符同 PreparedStatement）
② 参数三态：IN（setXxx）/ OUT（registerOutParameter + getXxx）/ INOUT
③ 返回结果集：executeQuery / 多个结果集（getMoreResults）
④ 何时用：存储过程场景（报表/批量复杂逻辑/历史系统）
⑤ 2026 现状：新项目少用（逻辑放应用层更易维护），遗留系统仍见
```

> 🎯 **核心要点**：CallableStatement = "**存储过程调用（IN/OUT 参数）**"——**"新项目少用存储过程（逻辑上移应用层）"是 2026 趋势**；面试答"Callable 何时用"给"遗留系统/数据库强逻辑"场景。

---

## 6. 三兄弟选型速查

**选型决策树**：

```text
要调用存储过程？
├── 是 → CallableStatement
└── 否 → SQL 有参数吗？
    ├── 是 → PreparedStatement（首选，99%）
    └── 否 → 动态表名/列名或 DDL？
        ├── 是 → Statement（罕见例外）
        └── 否 → PreparedStatement（无参数也用它——统一风格）
```

**参数绑定速查**（setXxx 家族）：

| 类型 | 方法 |
|------|------|
| String | setString |
| int/long | setInt / setLong |
| BigDecimal | setBigDecimal |
| 时间 | setObject(LocalDate)（JDBC 4.2 ✅）/ setTimestamp |
| 二进制 | setBytes |
| null | setNull(i, Types.X) |
| 通用 | setObject（JDBC 4.2 支持 java.time） |

**一句话总结**：

```text
Statement 三兄弟：
  PreparedStatement 首选（预编译 + 防注入 + 性能）
  Statement 只用于无参 DDL/动态表名（注入风险）
  CallableStatement 存储过程（IN/OUT 参数）
  参数绑定 setXxx + 时间用 setObject（JDBC 4.2）
```

> 🎯 **核心要点**：选型 = "**默认 PreparedStatement、存储过程 Callable、例外 Statement**"——**"99% 场景 PreparedStatement"是面试结论**；setObject 支持 java.time（JDBC 4.2）是 2026 的时间参数标准姿势。

---

**下一模块**：[03-ResultSet与数据读取](./03-ResultSet与数据读取.md) / **返回总览**：[00-JDBC知识体系总览](./00-JDBC知识体系总览.md)
