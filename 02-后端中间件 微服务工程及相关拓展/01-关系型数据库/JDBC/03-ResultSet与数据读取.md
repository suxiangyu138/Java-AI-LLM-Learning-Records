# 03 ResultSet 与数据读取

> ResultSet 是查询结果的游标视图——next() 遍历、getXxx 类型映射、列名 vs 索引、元数据反射，是手写 ORM 的基础

---

## 📚 目录

1. [ResultSet 的本质：游标](#1-resultset-的本质游标)
2. [遍历模式](#2-遍历模式)
3. [getXxx：类型映射](#3-getxxx类型映射)
4. [列名 vs 列索引](#4-列名-vs-列索引)
5. [ResultSetMetaData：结果元数据](#5-resultsetmetadata结果元数据)
6. [大字段与特殊类型](#6-大字段与特殊类型)
7. [数据读取速查](#7-数据读取速查)

---

## 1. ResultSet 的本质：游标

**ResultSet = 结果集的游标视图**（不是集合——是"指向一行"的指针）：

```text
ResultSet 的游标模型：
  rs 初始位置：第一行之前（beforeFirst）
  rs.next()：下移一行（有数据返回 true，无数据返回 false）
  rs 结束位置：最后一行之后（afterLast）

关键认知：
  ① 只能顺序前进（默认）（可滚动类型例外）
  ② 指向"当前行"，getXxx 取的是"当前行"的列
  ③ 用完必须关闭（TWR）（释放数据库资源）
```

```java
try (ResultSet rs = ps.executeQuery()) {
    while (rs.next()) {              // 游标下移 + 判空
        long id = rs.getLong("id");  // 取当前行的列
        String name = rs.getString("name");
    }
}
```

> 🎯 **核心要点**：ResultSet = "**游标（指向一行的指针）**"——**"next() 先下移再取值"是核心心智模型**；默认单向游标（大数据量流式处理的限制，见第 6 节）。

---

## 2. 遍历模式

```java
// 模式 1：while + next（标准）
while (rs.next()) {
    process(rs.getLong("id"), rs.getString("name"));
}

// 模式 2：isBeforeFirst 判断空结果（选装）
if (!rs.isBeforeFirst()) {
    return emptyList();     // 无数据（避免"有结果但循环不执行"的歧义）
}
while (rs.next()) { ... }

// 模式 3：绝对定位（可滚动结果集，少见）
// 需要可滚动：conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ...)
rs.absolute(5);        // 跳到第 5 行
rs.first();            // 第一行
rs.last();             // 最后一行（可得总行数）
rs.getRow();           // 当前行号
```

**遍历的性能注意**：

```text
① 默认类型：TYPE_FORWARD_ONLY（单向，性能最好）
② 可滚动类型：牺牲性能换定位（大数据量勿用）
③ 逐行处理：流式（setFetchSize 控制批量拉取）
④ 尽早处理：ResultSet 持有数据库游标（长时间不消费 = 连接占用）
```

> 🎯 **核心要点**：遍历 = "**while(next) 标准 + isBeforeFirst 判空 + 可滚动是特例**"——**"可滚动牺牲性能"与"ResultSet 持有游标要尽快消费"**两个注意点是生产细节。

---

## 3. getXxx：类型映射

**JDBC → Java 的类型映射**（getXxx 家族）：

| SQL 类型 | getXxx | 说明 |
|---------|--------|------|
| INT/BIGINT | getInt / getLong | 整数 |
| VARCHAR/TEXT | getString | 字符串 |
| DECIMAL/NUMERIC | **getBigDecimal** | 金额（配合列 scale） |
| DATE | getDate → toLocalDate | JDBC 4.2 可用 getObject |
| TIMESTAMP | getTimestamp / getObject(LocalDateTime) | **JDBC 4.2 直接拿 java.time** |
| BOOLEAN/BIT | getBoolean | |
| BLOB | getBytes / getBinaryStream | 二进制 |
| NULL | wasNull() 判断 | getXxx 返回 0/false 而非 null！ |

**null 的陷阱**（重要）：

```java
// ⚠️ getInt 对 NULL 返回 0（不是 null！）
int age = rs.getInt("age");       // 数据库 NULL → 0（无法区分）
boolean flag = rs.getBoolean("flag");  // NULL → false

// 正确：先用 wasNull 判断
int age = rs.getInt("age");
if (rs.wasNull()) {               // wasNull：检查上一列是否 NULL
    age = -1;                     // 显式处理
}

// 引用类型（getString/getBigDecimal）本身返回 null（无此问题）
String name = rs.getString("name");    // NULL → null ✅
```

> 🎯 **核心要点**：类型映射 = "**基本类型 getXxx 对 NULL 返回默认值（0/false）+ wasNull 判断**"——**"getInt 取到 0 不一定是 0"是经典坑**；金额用 getBigDecimal（配合 scale）、时间用 getObject（JDBC 4.2）。

---

## 4. 列名 vs 列索引

```java
// 两种取列方式
rs.getLong("id");              // ① 列名（可读 ✅ 推荐）
rs.getLong(1);                 // ② 列索引（从 1 开始！）

// 列名的优势：
//   可读性（SQL 变化时容错——SELECT 顺序无关）
// 列索引的优势：
//   性能微优（无名字解析）

// 别名与大小写：
rs.getString("user_name");     // 按列名（MySQL 默认不区分大小写）
// SELECT u.name AS user_name → getString("user_name")
```

**两个易错点**：

```text
① 索引从 1 开始（不是 0！）——rs.getXxx(0) 抛异常
② 重复列名歧义：SELECT a.id, b.id → getString("id") 取第一个
   → 用别名区分（AS user_id）
```

> 🎯 **核心要点**：取列 = "**列名可读（推荐）+ 索引从 1 开始**"——**"索引 0 是数组思维、JDBC 从 1"是高频小坑**；重复列名用别名（ORM 结果映射的常见来源）。

---

## 5. ResultSetMetaData：结果元数据

**ResultSetMetaData：结果集的"结构描述"**（手写 ORM/通用查询的关键）：

```java
// 获取元数据
ResultSetMetaData meta = rs.getMetaData();

int count = meta.getColumnCount();           // 列数
for (int i = 1; i <= count; i++) {
    String name = meta.getColumnName(i);     // 列名
    String label = meta.getColumnLabel(i);   // 别名（SELECT AS）
    int type = meta.getColumnType(i);        // 类型（java.sql.Types）
    int size = meta.getPrecision(i);         // 精度
}
```

**元数据的应用**（通用查询/导出）：

```java
// 通用结果转 Map（手写迷你 ORM 的核心）
List<Map<String, Object>> rows = new ArrayList<>();
ResultSetMetaData meta = rs.getMetaData();
int colCount = meta.getColumnCount();
while (rs.next()) {
    Map<String, Object> row = new LinkedHashMap<>();
    for (int i = 1; i <= colCount; i++) {
        row.put(meta.getColumnLabel(i), rs.getObject(i));
    }
    rows.add(row);
}
// → 这就是 MyBatis resultType="map" 的 JDBC 底层！
```

> 🎯 **核心要点**：元数据 = "**结果集的列结构（列名/类型/精度）**"——**"getColumnLabel（别名）+ getObject 通用取列 → 通用结果映射"**是手写 ORM 的入门钥匙（MyBatis 的 map 结果底层）。

---

## 6. 大字段与特殊类型

**大字段（BLOB/CLOB）**：

```java
// BLOB（二进制）：流式读取
Blob blob = rs.getBlob("data");
try (InputStream in = blob.getBinaryStream()) {
    byte[] bytes = in.readAllBytes();       // 小数据
    // 大数据：流式处理（in.transferTo(out)）
}

// CLOB（大文本）：
Clob clob = rs.getClob("content");
String text = clob.getSubString(1, (int) clob.length());
// 或：getString（驱动可能限制大小）

// 大数据量查询：流式/游标
// MySQL：setFetchSize(Integer.MIN_VALUE) 强制流式（防一次性加载 OOM）
ps.setFetchSize(Integer.MIN_VALUE);     // MySQL 流式读取
```

**时间类型（JDBC 4.2 现代姿势）**：

```java
// 现代：getObject 直接取 java.time
LocalDate date = rs.getObject("create_date", LocalDate.class);
LocalDateTime dt = rs.getObject("create_time", LocalDateTime.class);
Instant instant = rs.getObject("create_ts", Instant.class);   // TIMESTAMPTZ

// 写入：
ps.setObject(1, LocalDate.now());
ps.setObject(2, LocalDateTime.now());
// 替代旧：rs.getDate().toLocalDate() / ps.setTimestamp(Timestamp.valueOf(...))
```

> 🎯 **核心要点**：大字段与时间 = "**BLOB 流式 + setFetchSize 流式查询（防 OOM）**"——**"getObject(列, LocalDate.class) 是现代时间读取"**（JDBC 4.2 替代 Date/Timestamp 转换）；大结果集必须流式。

---

## 7. 数据读取速查

**ResultSet 高频函数速查**：

| 场景 | 函数 | 坑点 |
|------|------|------|
| 遍历 | next() | 先下移再取列 |
| 判空结果 | isBeforeFirst() | — |
| 整数列 | getInt/getLong | NULL → 0 |
| 金额 | getBigDecimal | 配合 scale |
| 字符串 | getString | NULL → null（OK） |
| 时间 | getObject(..., LocalDate.class) | JDBC 4.2 |
| 判 NULL | wasNull() | 基本类型必须 |
| 列名/索引 | getString("name") / getString(1) | 索引从 1 开始 |
| 列结构 | getMetaData() | 手写 ORM |
| 二进制 | getBytes/getBlob | 大字段流式 |
| 流式 | setFetchSize | 大结果集防 OOM |

**一句话总结**：

```text
ResultSet 核心认知：
  游标模型（next 先下移）
  基本类型 NULL → 默认值（wasNull 判断）
  列名优先、索引从 1
  元数据（MetaData）→ 手写 ORM 钥匙
  时间用 getObject（JDBC 4.2）
  大结果集流式（setFetchSize）
```

> 🎯 **核心要点**：读取速查 11 项 = "**ResultSet 的完整操作清单**"——**"wasNull 判断 + getObject 时间 + 流式防 OOM"三个现代细节**是框架时代也要掌握的 JDBC 底层能力。

---

**下一模块**：[04-事务管理与隔离](./04-事务管理与隔离.md) / **返回总览**：[00-JDBC知识体系总览](./00-JDBC知识体系总览.md)
