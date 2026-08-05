# 04 Wrapper 条件构造器

> Wrapper 是 MP 的动态 SQL 心脏——条件方法、null 陷阱、嵌套与自定义片段，用错一个符号就是线上事故

---

## 📚 目录

1. [Wrapper 体系与选型](#1-wrapper-体系与选型)
2. [条件方法全景](#2-条件方法全景)
3. [eq 传 null 的陷阱与条件重载](#3-eq-传-null-的陷阱与条件重载)
4. [嵌套条件与子查询](#4-嵌套条件与子查询)
5. [动态条件构建范式](#5-动态条件构建范式)
6. [customSqlSegment：融合自定义 SQL](#6-customsqlsegment融合自定义-sql)
7. [last / apply 的危险操作与规范](#7-last--apply-的危险操作与规范)

---

## 1. Wrapper 体系与选型

```text
Wrapper（抽象基类）
├── QueryWrapper        —— 查询条件（字符串字段名）
│     └── LambdaQueryWrapper —— 查询条件（方法引用，类型安全）✅ 首选
├── UpdateWrapper       —— 更新条件 + set 列（字符串）
│     └── LambdaUpdateWrapper —— 同上（方法引用）
└── AbstractLambdaWrapper —— 两者的 Lambda 公共基类
```

| 维度 | QueryWrapper | LambdaQueryWrapper |
|------|:------------:|:------------------:|
| 字段写法 | 字符串 `"user_name"` | **方法引用 `User::getUserName`** |
| 编译期检查 | ❌ 写错静默（SQL 报错才知） | ✅ 类型安全 |
| 重构友好 | ❌ 字段改名要改字符串 | ✅ 编译器自动 | 
| 适用 | 多表联查（跨表字段用字符串） | **单表操作（默认首选）** |

```java
// 首选：Lambda —— 列名写错直接编译失败
List<User> users = userMapper.selectList(
        new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .like(User::getName, "张"));

// 多表/复杂：QueryWrapper —— 字符串列名灵活（但小心拼写错误）
List<Map<String, Object>> rows = userMapper.selectMaps(
        new QueryWrapper<User>()
                .eq("u.status", 1)          // 联表时可用别名
                .orderByDesc("u.create_time"));
```

> 🎯 **核心要点**：**单表无脑 Lambda**（编译期安全），多表联查用 QueryWrapper 字符串（跨表字段必须字符串）。3.5.17 起 Wrapper 支持 XML 别名配置与 typeHandler 自动识别——复杂映射场景更顺手。

---

## 2. 条件方法全景

| 分类 | 方法 | SQL 语义 |
|------|------|---------|
| 等值/比较 | `eq/ne`、`gt/ge/lt/le` | `=`、`!=`、`>`、`>=`、`<`、`<=` |
| 范围 | `between`、`notBetween` | `BETWEEN a AND b` |
| 集合 | `in`、`notIn` | `IN (...)`（集合/数组/子查询） |
| 模糊 | `like`、`likeLeft`、`likeRight`、`notLike` | `LIKE '%x%'`、`LIKE '%x'`、`LIKE 'x%'` |
| 空值 | `isNull`、`isNotNull` | `IS NULL`、`IS NOT NULL` |
| 排序 | `orderByAsc/Desc`、`orderBy` | `ORDER BY` |
| 聚合 | `groupBy`、`having` | `GROUP BY`、`HAVING` |
| 分页 | `last("limit 10")` | 追加片段（危险，见第 7 节） |
| 拼接 | `apply` | 原生 SQL 片段（危险） |
| 选择列 | `select` | 只查指定列（性能优化） |
| 存在 | `exists/notExists` | `EXISTS (子查询)` |
| 去重 | `distinct` | `SELECT DISTINCT` |

```java
// 典型组合：范围 + 模糊 + 排序 + 只查列
userMapper.selectList(new LambdaQueryWrapper<User>()
        .select(User::getId, User::getName)                    // 只查两列
        .between(User::getCreateTime, start, end)              // 时间范围
        .likeRight(User::getPhone, "138")                      // 前缀匹配（可用索引）
        .in(User::getStatus, 1, 2, 3)                          // IN
        .orderByDesc(User::getCreateTime)                      // 排序
        .last("limit 20"));                                    // 追加（慎用）
```

> 💡 **性能提示**：`like`（`%x%`）无法走索引（前导通配）；前缀匹配用 `likeRight`（`x%`）可走索引——数据量大时这是隐性性能差异。

---

## 3. eq 传 null 的陷阱与条件重载

**陷阱**：`.eq(User::getStatus, null)` 会生成 `WHERE status = null`——**SQL 三值逻辑下永远查不到数据**（不是"跳过条件"！）：

```java
// ❌ 危险：条件值为 null 时查询结果为空
List<User> list = userMapper.selectList(
        new LambdaQueryWrapper<User>()
                .eq(User::getStatus, status));      // status=null → WHERE status = null → 空结果

// ✅ 条件重载：第一个参数是 boolean condition
List<User> list = userMapper.selectList(
        new LambdaQueryWrapper<User>()
                .eq(status != null, User::getStatus, status));   // false → 整段条件跳过
```

**两个重载的语义差异**（面试必答）：

| 重载 | 写法 | null 时行为 |
|------|------|------------|
| 无条件参数 | `.eq(col, val)` | 拼 `col = null`（**坑**） |
| 条件重载 | `.eq(condition, col, val)` | condition=false 整段跳过（**安全**） |

```java
// 规范：所有动态条件统一"条件重载"写法
List<User> list = userMapper.selectList(new LambdaQueryWrapper<User>()
        .eq(name != null && !name.isEmpty(), User::getName, name)
        .ge(minAge != null, User::getAge, minAge)
        .like(keyword != null, User::getPhone, keyword));
// 或者封装工具：eqIfNotNull(wrapper, col, val)
```

> ⚠️ **同类陷阱**：`like(col, null)` 拼 `%null%`；`in(col, 空集合)` 拼 `IN ()` 语法错误——**动态条件一律用条件重载**（或判空后拼接）。

---

## 4. 嵌套条件与子查询

**嵌套括号条件**（and/or 的组合逻辑）：

```java
// 需求：age > 20 且（name 含"张" 或 status = 1）
wrapper.and(w -> w.like(User::getName, "张").or().eq(User::getStatus, 1))
       .ge(User::getAge, 20);
// SQL：WHERE age >= 20 AND (name LIKE '%张%' OR status = 1)
```

**or 的正确使用**（经典坑：or 裸用会扩大范围）：

```java
// ❌ 坑：eq(...).or().eq(...) —— OR 与前面的条件平级，可能破坏语义
// SQL：WHERE a = 1 OR b = 2 AND c = 3（AND 优先级更高，结果与直觉不符）

// ✅ 规范：or 包裹成组
wrapper.eq(User::getStatus, 1)
       .and(w -> w.like(User::getName, "张").or().eq(User::getPhone, "138"));
// SQL：WHERE status = 1 AND (name LIKE '%张%' OR phone = '138')
```

**子查询**（`inSql`/`apply` 或嵌套 Wrapper）：

```java
// 子查询：查"有订单的用户"
wrapper.inSql(User::getId, "SELECT user_id FROM order WHERE status = 1");
// SQL：WHERE id IN (SELECT user_id FROM order WHERE status = 1)
// ⚠️ inSql 是 SQL 拼接 —— 内容必须是可信常量，禁止拼接用户输入！
```

> 🎯 **核心要点**：嵌套条件的规则 = **"or 必须进括号"**——`and(w -> ...)` 包裹组条件，避免 OR 平级扩大语义；子查询用 `inSql` 时**内容必须可信**（拼接用户输入 = SQL 注入）。

---

## 5. 动态条件构建范式

**动态查询（查询条件可选）的三种写法**——生产最常用模式：

```java
// 写法 1：条件重载（推荐，一行一个条件）
public List<User> search(String name, Integer minAge, String phone) {
    return userMapper.selectList(new LambdaQueryWrapper<User>()
            .eq(name != null, User::getName, name)
            .ge(minAge != null, User::getAge, minAge)
            .like(phone != null, User::getPhone, phone));
}

// 写法 2：条件方法内部判断（适合复杂逻辑）
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
if (name != null && !name.isBlank()) {
    wrapper.like(User::getName, name);
}
if (minAge != null) {
    wrapper.ge(User::getAge, minAge);
}
return userMapper.selectList(wrapper);

// 写法 3：封装工具方法（团队级规范）
eqIfNotNull(wrapper, User::getStatus, status);
likeIfNotBlank(wrapper, User::getName, name);
```

**动态更新的对应范式**（UpdateWrapper 条件重载同样适用）：

```java
userMapper.update(null, new LambdaUpdateWrapper<User>()
        .eq(User::getId, id)                    // 定位行（必须有！）
        .set(name != null, User::getName, name)
        .set(age != null, User::getAge, age));
```

> 🎯 **核心要点**：动态条件 = **条件重载（condition 参数）是首选写法**——可读、安全、无 null 陷阱；"定位条件 + 动态 set"是 UpdateWrapper 的标配姿势。**UPDATE 永远先写定位条件**（配合防全表插件，见 06 模块）。

---

## 6. customSqlSegment：融合自定义 SQL

**痛点**：自定义 SQL（XML）想复用 Wrapper 的条件——`${ew.customSqlSegment}` 把 Wrapper 拼好的条件片段嵌入 SQL：

```xml
<!-- Mapper 接口 -->
<!-- 自定义查询：LEFT JOIN + Wrapper 条件复用 -->
<select id="selectUserWithOrders" resultType="com.example.dto.UserOrderDto">
    SELECT u.*, o.order_no
    FROM user u
    LEFT JOIN `order` o ON o.user_id = u.id
    ${ew.customSqlSegment}
</select>
```

```java
// 接口定义：参数名必须叫 ew
List<UserOrderDto> selectUserWithOrders(@Param(Constants.WRAPPER) Wrapper<User> ew);

// 调用：Wrapper 的条件自动拼接到 SQL 末尾
List<UserOrderDto> list = userMapper.selectUserWithOrders(
        new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByDesc(User::getCreateTime));
// SQL：... LEFT JOIN ... WHERE status = 1 ORDER BY create_time DESC
```

**三个关键点**：

| 要点 | 说明 |
|------|------|
| 必须用 `${}` | 直接拼接 SQL 片段（`#{}` 是预编译参数，无法拼条件） |
| 参数名固定 `ew` | `@Param(Constants.WRAPPER)`（常量值 `"ew"`，不能改） |
| 返回类型 | 可以是 DTO（`resultType` 指定），不限于实体 |

> ⚠️ **安全注意**：`${ew.customSqlSegment}` 拼接的是 **Wrapper 生成的 SQL 片段**——Wrapper 内部对值参数做预编译（`?`），但**手动 `.apply("...")` 拼的内容是裸 SQL**——生产禁止把用户输入拼进 apply/last（见下节）。

---

## 7. last / apply 的危险操作与规范

**两个"裸 SQL 追加"方法**——最强大也最危险：

| 方法 | 用途 | 风险 |
|------|------|------|
| `.last("limit 10")` | 追加任意 SQL 片段（分页/锁） | **注入风险 + 破坏语义**（拼到 WHERE 后） |
| `.apply("DATE(create_time) = {0}", date)` | 追加条件片段（可用占位符） | 占位符安全；字符串拼接危险 |

```java
// ✅ 安全用法：占位符方式（值参数预编译）
wrapper.apply("DATE(create_time) = {0}", LocalDate.now());   // 值走占位符 → 安全
wrapper.last("FOR UPDATE");                                   // 悲观锁（合法用途）

// ❌ 危险用法：拼接用户输入
wrapper.apply("status = " + status);        // ❌ SQL 注入！（status 是用户输入）
wrapper.last("and name like '%" + name + "%'");   // ❌ 双重危险
```

**使用规范**（生产红线）：

1. **值参数一律占位符**：`{0}`/`{1}`（预编译）；
2. **last 只用于"白名单常量"**：`limit`、`FOR UPDATE` 等固定片段；
3. **优先官方方法**：分页用 `Page`（不要 last 拼 limit）、锁行用 `selectForUpdate` 或专门的 wrapper 方式；
4. **代码评审拦截**：`.apply(` 带 `+` 拼接、`.last(` 非白名单——直接打回。

> 🎯 **核心要点**：apply/last 是 Wrapper 的"后门"——**用占位符保安全、用白名单保语义**。能不用就不用（官方方法覆盖 95% 场景），用了就按两条红线评审。

---

**下一模块**：[05-IService与Service层封装](./05-IService与Service层封装.md) / **返回总览**：[00-MyBatisPlus知识体系总览](./00-MyBatisPlus知识体系总览.md)
