# Hibernate 教程

> **定位**：Hibernate 是全自动化 ORM 框架，实现对象-关系映射（ORM），封装 JDBC，支持延迟加载、多级缓存、HQL 查询。

---

## 目录

- [Unit 01 · 基础入门](#unit-01--基础入门)
- [Unit 02 · 进阶特性](#unit-02--进阶特性)
- [Unit 03 · 高级查询与缓存](#unit-03--高级查询与缓存)

---

## Unit 01 · 基础入门

### 1. Hibernate 简介

| 主题 | 内容 |
|------|------|
| 1.1 什么是 Hibernate | 概念、作用 |
| 1.2 为什么用 Hibernate | Hibernate vs JDBC 对比、Hibernate vs MyBatis 对比 |

### 2. 使用 Hibernate

| 主题 | 内容 |
|------|------|
| 2.1 设计原理 | ORM 思想（对象-关系映射） |
| 2.2 体系结构 | 主配置文件、实体类、映射文件（`.hbm.xml`）、底层 API |
| 2.3 如何使用 | 常用 API（`Session`/`Transaction`/`Query`）、使用步骤 |
| 2.4 映射类型 | Java 类型 ↔ Hibernate 预定义类型 |

### 3. 项目实战

| 主题 | 内容 |
|------|------|
| 3.1 引入 Hibernate | 导包、主配置文件（`hibernate.cfg.xml`）、`HibernateUtil` 工具类 |
| 3.2 重构 DAO | 创建映射文件、声明映射、重构 DAO 实现类 |

### 4. 主键生成方式

| 方式 | 说明 | 适用 |
|------|------|------|
| `sequence` | 序列 | Oracle |
| `identity` | 自增 | MySQL |
| `native` | 自动选择 | 通用 |
| `increment` | 自增（Hibernate 管理） | — |
| `assigned` | 手动赋值 | — |
| `uuid` / `hilo` | UUID / 高低位算法 | — |

**经典案例**：基本使用、映射类型、重构资费 DAO

---

## Unit 02 · 进阶特性

### 1. 一级缓存

| 主题 | 内容 |
|------|------|
| 1.1 介绍 | 什么是 Session 级别一级缓存、为什么使用 |
| 1.2 使用 | 如何使用、缓存规则、缓存管理（`evict`/`clear`） |

### 2. 对象持久性

| 状态 | 说明 | 特征 |
|------|------|------|
| **临时态**（Transient） | `new` 创建、无 OID、不在 Session 中 | 未与 DB 关联 |
| **持久态**（Persistent） | 有 OID、在 Session 中 | 变化自动同步到 DB |
| **游离态**（Detached） | 有 OID、Session 已关闭 | 需 `update`/`merge` 重新关联 |

### 3. 延迟加载

| 主题 | 内容 |
|------|------|
| 3.1 介绍 | 什么是延迟加载（Lazy Loading）、为什么使用 |
| 3.2 使用 | 延迟加载的方法、注意事项（`LazyInitializationException`）、Open Session in View 模式 |

### 4. 关联映射

| 映射类型 | 说明 |
|----------|------|
| 一对一 | `<one-to-one>` |
| 一对多 | `<set>` + `<one-to-many>` |
| 多对一 | `<many-to-one>` |
| 多对多 | `<set>` + `<many-to-many>` |

### 5. 一对多关联

| 步骤 | 操作 |
|:----:|------|
| 1 | 明确两张表的关系及关系字段 |
| 2 | 在"一"方实体类添加集合属性（`Set<T>`） |
| 3 | 在映射文件中配置 `<set>` + `<one-to-many>` |

**经典案例**：验证一级缓存、验证持久态特性、验证延迟加载、一对多关联映射

---

## Unit 03 · 高级查询与缓存

### 1. 多对一关联

| 步骤 | 操作 |
|:----:|------|
| 1 | 明确关系字段（外键） |
| 2 | 在"多"方实体类添加实体属性 |
| 3 | 在映射文件中配置 `<many-to-one>` |

### 2. 关联操作

| 主题 | 内容 |
|------|------|
| 2.1 关联查询 | 延迟加载、抓取策略（`fetch="join"/"select"`） |
| 2.2 级联操作 | `cascade="save-update"/"delete"/"all"`、控制反转（`inverse`） |

### 3. Hibernate 查询

| 查询方式 | 说明 |
|----------|------|
| **HQL** | 面向对象的查询语言（`from User where id=?`） |
| 按条件查询 | `where` + 参数绑定 |
| 查询部分字段 | `select name, age from User` |
| 分页查询 | `setFirstResult()` + `setMaxResults()` |
| 多表联合查询 | HQL join 语法 |
| **SQL 查询** | `createSQLQuery()` 原生 SQL |
| **Criteria 查询** | 面向对象的条件查询 |

### 4. Hibernate 高级特性

| 特性 | 说明 |
|------|------|
| **二级缓存** | SessionFactory 级别，跨 Session 共享（需集成 Ehcache/Redis） |
| **查询缓存** | 缓存 HQL/Criteria 查询结果 |

**经典案例**：多对一映射、级联操作、HQL 四种查询、SQL 查询、二级缓存、查询缓存

---

## 学习路线

```text
Unit 01（基础）→ Unit 02（进阶）→ Unit 03（高级）
      │                 │                  │
  ORM + CRUD      缓存 + 延迟加载     HQL + 多级缓存
  + 主键策略      + 关联映射          + 级联操作
```
