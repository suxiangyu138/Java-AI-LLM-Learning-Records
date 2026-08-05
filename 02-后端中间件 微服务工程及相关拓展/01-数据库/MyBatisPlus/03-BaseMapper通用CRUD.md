# 03 BaseMapper 通用 CRUD

> 继承 BaseMapper 即获得全套单表 CRUD——理解每个方法的 SQL 语义与边界，是零手写 SQL 的正确前提

---

## 📚 目录

1. [API 全景：四大方法族](#1-api-全景四大方法族)
2. [select 家族详解](#2-select-家族详解)
3. [insert 语义](#3-insert-语义)
4. [update 语义：updateById vs UpdateWrapper](#4-update-语义updatebyid-vs-updatewrapper)
5. [delete 语义：物理与逻辑](#5-delete-语义物理与逻辑)
6. [通用 SQL 的生成原理](#6-通用-sql-的生成原理)

---

## 1. API 全景：四大方法族

```java
public interface BaseMapper<T> extends Mapper<T> {
    // select 家族
    T selectById(Serializable id);
    T selectOne(Wrapper<T> queryWrapper);
    List<T> selectList(Wrapper<T> queryWrapper);
    List<T> selectBatchIds(Collection<? extends Serializable> idList);
    List<Map<String, Object>> selectMaps(Wrapper<T> queryWrapper);
    Long selectCount(Wrapper<T> queryWrapper);
    <E extends IPage<T>> E selectPage(E page, Wrapper<T> queryWrapper);
    List<T> selectByMap(Map<String, Object> columnMap);   // 等值条件 Map

    // insert
    int insert(T entity);

    // update
    int updateById(T entity);
    int update(T entity, Wrapper<T> updateWrapper);

    // delete
    int deleteById(Serializable id);
    int delete(Wrapper<T> queryWrapper);
    int deleteBatchIds(Collection<? extends Serializable> idList);
    int deleteByMap(Map<String, Object> columnMap);
}
```

**统一返回值语义**：所有写方法返回 **int（影响行数）**——**必须检查**（尤其乐观锁场景，见 02 模块第 5 节）。

> 🎯 **核心要点**：BaseMapper = "**单表操作全家桶**"——查询四种形态（按 id/按条件/按 Map/分页）+ 写操作三件套。80% 的单表需求它直接覆盖，剩下的交给 Wrapper 与自定义 SQL。

---

## 2. select 家族详解

| 方法 | SQL 语义 | 边界/坑 |
|------|---------|---------|
| `selectById` | `WHERE id=?` | 逻辑删除后查不到（自动拼 deleted=0） |
| `selectOne` | `WHERE 条件 LIMIT 1` | **多个结果抛异常**（TooManyResults）——条件必须唯一 |
| `selectList` | `WHERE 条件` | 无结果返回**空列表**（不是 null） |
| `selectBatchIds` | `WHERE id IN (...)` | 空集合返回空列表 |
| `selectMaps` | 结果转 Map | 列名作 key（下划线原样） |
| `selectCount` | `SELECT COUNT(*)` | 返回 Long |
| `selectByMap` | `WHERE col=val AND ...` | 等值条件；Map 的 key 是**列名** |
| `selectPage` | `LIMIT offset, size` | 页码从 **1** 开始（传 0 查不到！） |

```java
// 使用要点
User u = userMapper.selectById(1L);                          // 单查
User one = userMapper.selectOne(                             // 唯一条件：手机号
        new LambdaQueryWrapper<User>().eq(User::getPhone, "138..."));
List<User> users = userMapper.selectList(                    // 条件列表
        new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByDesc(User::getCreateTime));
Long count = userMapper.selectCount(                         // 计数
        new LambdaQueryWrapper<User>().ge(User::getAge, 18));
```

> ⚠️ **selectOne 的两个坑**：① 条件不唯一（查到多条）抛 `TooManyResultsException`；② 配合逻辑删除时"已删数据"不参与——**判断"手机号是否被占用"前要想清楚逻辑删除语义**（见 08 模块唯一索引冲突）。

---

## 3. insert 语义

```java
int rows = userMapper.insert(user);
// SQL：INSERT INTO user (id, name, age, ...) VALUES (?, ?, ...)
```

**insert 的四个细节**：

| 细节 | 说明 |
|------|------|
| 主键回填 | `ASSIGN_ID`/`AUTO` 生成的 id **写回实体**（insert 后可直接 `user.getId()`） |
| null 字段 | **默认不插入**（`FieldStrategy.NOT_NULL`）——`INSERT` 只含非 null 列 |
| 逻辑删除字段 | 插入时**自动带 deleted=0**（默认值） |
| 自动填充 | `fill = INSERT` 的字段自动填（见 02 模块第 6 节） |

```java
User u = new User();
u.setName("张三");
userMapper.insert(u);
System.out.println(u.getId());    // ✅ 雪花 ID 已回填 —— 无需再次查询
```

> 🎯 **核心要点**：insert 的智能点 = **null 字段不插入 + 主键回填 + 自动填充**——实体只需设置非默认字段；"insert 后马上用 id"是标配姿势。

---

## 4. update 语义：updateById vs UpdateWrapper

| 方法 | 语义 | 适用 |
|------|------|------|
| `updateById(entity)` | 按主键更新，**null 字段跳过**（NOT_NULL 策略） | 更新已知实体（表单提交） |
| `update(entity, wrapper)` | 按 Wrapper 条件更新，null 字段跳过 | 条件更新（批量/动态条件） |

```java
// updateById：null 字段不更新（静默跳过！）
User u = new User();
u.setId(1L);
u.setName("新名字");
userMapper.updateById(u);
// SQL：UPDATE user SET name=? WHERE id=1
// age 字段为 null → 不更新（保持原值）—— 这是"部分更新"的便利，也是"想置 null 却置不了"的坑

// 显式置 null：用 UpdateWrapper.set
userMapper.update(null, new LambdaUpdateWrapper<User>()
        .eq(User::getId, 1L)
        .set(User::getRemark, null));     // ✅ 显式 SET remark = null
```

**三个高频坑**：

1. **updateById 置 null 失败**：默认策略跳过 null——需要 `UpdateWrapper.set(col, null)` 或字段 `@TableField(updateStrategy = FieldStrategy.IGNORED)`；
2. **无 WHERE 条件更新**：`update(entity, wrapper)` 传空 Wrapper = 全表更新（**生产禁止**——用 `BlockAttackInnerInterceptor` 拦截，见 06 模块）；
3. **乐观锁字段**：`@Version` 字段自动参与 `WHERE version=?`（插件注册后，见 02 模块第 5 节）。

> 🎯 **核心要点**：**"updateById 更新非 null 字段"是 MP 的核心更新语义**——做"部分更新"（表单只传修改字段）极其方便；但"置 null""全表更新"需要显式手段。记一句：**想跳过 null 用 updateById，想操作 null 用 UpdateWrapper**。

---

## 5. delete 语义：物理与逻辑

```java
// 物理删除（无 @TableLogic 时）
int rows = userMapper.deleteById(1L);                 // DELETE FROM user WHERE id=1
int rows2 = userMapper.delete(new LambdaQueryWrapper<User>()
        .eq(User::getAge, 18));                       // DELETE FROM user WHERE age=18

// 逻辑删除（字段有 @TableLogic 时）—— 自动改写！
// deleteById(1L) → UPDATE user SET deleted=1 WHERE id=1 AND deleted=0
// delete(wrapper) → UPDATE user SET deleted=1 WHERE deleted=0 AND ...
```

**delete 的边界**：

| 场景 | 行为 |
|------|------|
| 物理删除 | 数据永久消失，**不可恢复** |
| 逻辑删除 | 数据保留（deleted=1），查询自动过滤 |
| 删除条件为空 | `delete(null)` 语义 = 全表删除（生产禁止，防全表拦截） |
| 逻辑删除 + 物理删除混用 | 同表要么全部逻辑要么全部物理——混用语义混乱 |

**选择建议**：**有审计/恢复需求的表用逻辑删除**（用户、订单）；纯临时数据（日志明细）用物理删除。逻辑删除的代价 = 所有查询自动加条件 + 唯一索引冲突风险（08 模块）。

> 🎯 **核心要点**：delete 的语义由 **@TableLogic 决定**——删除操作"看起来一样"，实际执行可能完全不同。**"delete 前先想清楚物理还是逻辑"是设计决策，不是代码决策**。

---

## 6. 通用 SQL 的生成原理

**MP 如何"凭空"提供 CRUD 方法**（理解原理 = 排障基础）：

```text
① 启动时：扫描继承 BaseMapper 的 Mapper 接口
② 为每个方法生成对应的 SQL 与 MappedStatement：
   - insert → INSERT INTO 表 (列...) VALUES (...)
   - selectById → SELECT 列... FROM 表 WHERE id=? （按主键）
   - selectList → SELECT 列... FROM 表 [WHERE wrapper]
   - deleteById → DELETE/UPDATE ... WHERE id=?
   ③ 注册进 MyBatis 的 Configuration（与手写 XML 的 MappedStatement 同机制）
④ 运行时：与普通 MyBatis 方法完全一致（参数处理 → SQL 执行 → 结果映射）
```

**"列名从哪来"**：MP 通过解析实体的 `TableInfo`（注解 + 反射）构建列清单——所以：

- 实体字段 → 列名映射错误时，生成的 SQL 列名就错（排查先看 `log-impl` 打印的 SQL）；
- `@TableField(exist = false)` 的字段**不参与**生成 SQL（否则报"列不存在"）；
- 手写自定义方法（XML/`@Select`）**不经过** SQL 注入器——逻辑删除条件等不会自动拼（02 模块第 4 节）。

> 🎯 **核心要点**：MP 通用方法的本质 = **启动期用 SQL 注入器为接口方法生成 SQL**（与手写 XML 走同一条 MappedStatement 通道）。排障口诀：**"SQL 不对先看生成的 SQL 长什么样"**（开启 `log-impl: StdOutImpl`），再回到实体映射找原因（源码见 `MyBatis/09-MyBatis源码导读.md`）。

**CRUD 场景 → 方法对照速查**（面试/开发速记）：

| 需求 | 方法 |
|------|------|
| 按主键查一条 | `selectById(id)` |
| 按唯一条件查一条（手机号） | `selectOne(wrapper)` |
| 条件查列表 | `selectList(wrapper)` |
| 按主键集合查 | `selectBatchIds(ids)` |
| 统计数量 | `selectCount(wrapper)` |
| 分页查询 | `selectPage(page, wrapper)` |
| 新增 | `insert(entity)`（主键回填） |
| 按主键部分更新 | `updateById(entity)`（null 跳过） |
| 条件更新/置 null | `update(entity, updateWrapper)` |
| 按主键删除 | `deleteById(id)`（逻辑删除自动改写） |
| 条件删除 | `delete(wrapper)` |

> 💡 速记逻辑：**查询四形态（id/one/list/page）+ 写入三件套（insert/updateById/条件 update）+ 删除按主键或条件**——BaseMapper 的核心方法覆盖全部单表需求。

---

**下一模块**：[04-Wrapper条件构造器](./04-Wrapper条件构造器.md) / **返回总览**：[00-MyBatisPlus知识体系总览](./00-MyBatisPlus知识体系总览.md)
