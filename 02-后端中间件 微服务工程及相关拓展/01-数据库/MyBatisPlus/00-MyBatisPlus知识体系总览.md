# MyBatis-Plus 知识体系总览

> MyBatis-Plus 是 MyBatis 的增强工具——通用 CRUD、条件构造器、分页插件、逻辑删除、多租户，让单表开发效率提升数倍，是 Java 后端 CRUD 的标配

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透 MyBatis-Plus](#3-为什么必须学透-mybatis-plus)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
MyBatis-Plus 知识体系
│
├── 01 定位与快速上手
│   ├── MP 与 MyBatis 的关系（增强而非替代）
│   ├── 版本选型：3.5.x 时间线与 Spring Boot 2/3/4 starter
│   ├── 快速开始：依赖/配置/第一个 CRUD
│   ├── 3.5.9+ 插件拆分（jsqlparser 依赖）
│   └── 与 MyBatis 系统的交叉导航
│
├── 02 实体映射与注解体系
│   ├── @TableName / @TableId / @TableField 全解
│   ├── 主键策略：雪花 ID / 自增 / 自定义
│   ├── 驼峰映射与字段命名
│   ├── 逻辑删除 @TableLogic
│   ├── 乐观锁 @Version
│   ├── 自动填充 MetaObjectHandler
│   └── 枚举处理与类型处理器
│
├── 03 BaseMapper 通用 CRUD
│   ├── API 全景：select 家族 / insert / update / delete
│   ├── selectById / selectOne / selectList / selectPage
│   ├── 更新语义：updateById vs UpdateWrapper
│   ├── 删除语义：物理 vs 逻辑
│   └── 通用方法的内置 SQL 生成原理
│
├── 04 Wrapper 条件构造器
│   ├── QueryWrapper vs LambdaQueryWrapper
│   ├── 条件方法全景：eq/ne/gt/ge/lt/le/like/in/between...
│   ├── eq 传 null 的陷阱与条件重载
│   ├── 嵌套条件与子查询
│   ├── 动态条件构建范式
│   ├── customSqlSegment 融合自定义 SQL
│   └── last/apply 的危险操作与规范
│
├── 05 IService 与 Service 层封装
│   ├── IService / ServiceImpl 设计
│   ├── saveOrUpdate 家族与链式调用
│   ├── 批量操作：saveBatch / updateBatchById 原理
│   ├── 与事务的配合
│   └── 何时该写自己的 Service 方法
│
├── 06 插件体系与分页
│   ├── MybatisPlusInterceptor 机制
│   ├── 分页插件 PaginationInnerInterceptor（3.5.9+ 依赖）
│   ├── 分页优化：索引 / Keyset / 游标
│   ├── 乐观锁插件 OptimisticLockerInnerInterceptor
│   ├── 防全表更新 BlockAttackInnerInterceptor
│   ├── 多租户 TenantLineInnerInterceptor
│   └── 多数据源的分页失效坑
│
├── 07 代码生成器与扩展机制
│   ├── FastAutoGenerator 三行生成
│   ├── MetaObjectHandler 自动填充实战
│   ├── 自定义方法注入（SQL 注入器）
│   ├── 与 XML / 注解 SQL 的混合实践
│   └── 生成代码的企业级定制
│
├── 08 生产实践与性能优化
│   ├── 逻辑删除 × 唯一索引冲突（三大解法）
│   ├── 批量操作性能对比与最佳参数
│   ├── 千万级数据的 CRUD 优化
│   ├── 监控：性能插件 / 慢 SQL
│   ├── 常见坑清单（多数据源/事务/序列化）
│   └── 80/20 原则：单表 MP、复杂 SQL 原生
│
└── 09 面试高频考点与总结
    ├── 必背考点：MP 原理/CRUD/Wrapper/分页
    ├── 高频陷阱题 10 连问
    ├── 场景题：企业级 CRUD 架构设计
    └── 记忆口诀与进阶导航
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | 定位与快速上手 | 与 MyBatis 关系、版本选型、快速开始 | 初中级必须掌握 | [01-定位与快速上手](./01-定位与快速上手.md) |
| 02 | 实体映射与注解体系 | 表映射、主键、逻辑删除、乐观锁、自动填充 | 初中级必须掌握 | [02-实体映射与注解体系](./02-实体映射与注解体系.md) |
| 03 | BaseMapper 通用 CRUD | API 全景、select/update/delete 语义 | 初中级必须掌握 | [03-BaseMapper通用CRUD](./03-BaseMapper通用CRUD.md) |
| 04 | Wrapper 条件构造器 | 条件方法、null 陷阱、嵌套、customSqlSegment | 中高级 | [04-Wrapper条件构造器](./04-Wrapper条件构造器.md) |
| 05 | IService 与 Service 层 | 封装设计、saveOrUpdate、批量操作 | 中高级 | [05-IService与Service层封装](./05-IService与Service层封装.md) |
| 06 | 插件体系与分页 | 分页/乐观锁/防全表/多租户插件 | 中高级 | [06-插件体系与分页](./06-插件体系与分页.md) |
| 07 | 代码生成器与扩展 | FastAutoGenerator、自定义方法注入 | 中高级 | [07-代码生成器与扩展机制](./07-代码生成器与扩展机制.md) |
| 08 | 生产实践与性能 | 逻辑删除冲突、批量优化、坑清单 | 中高级 | [08-生产实践与性能优化](./08-生产实践与性能优化.md) |
| 09 | 面试高频考点与总结 | 必背考点、陷阱题、场景题 | 面试冲刺 | [09-面试高频考点与总结](./09-面试高频考点与总结.md) |

---

## 3. 为什么必须学透 MyBatis-Plus

1. **国内 Java 后端的 CRUD 标配**：绝大多数企业项目的持久层 = MyBatis + MyBatis-Plus——不掌握 MP，等于不会写 Java 业务代码。
2. **效率与规范的双重武器**：通用 CRUD 消灭样板代码（效率提升数倍），逻辑删除/乐观锁/自动填充让"数据规范"变成"框架约束"——**规范内建比人工约定可靠得多**。
3. **面试常考且区分度明显**：Wrapper 的 null 陷阱、分页插件依赖变化（3.5.9+）、逻辑删除与唯一索引冲突、乐观锁返回 0 行——这些"真踩过坑才知道"的细节是面试加分项。
4. **持续演进的活跃框架**：3.5.17（2026-07 发布）新增 GraalVM 原生镜像支持、Spring Boot 4 starter（3.5.13+）——**版本与生态状态必须追最新**。
5. **工程排障的高频区**：分页失效、逻辑删除不生效、乐观锁不生效、多租户漏 SQL——MP 的坑集中在"配置与插件"，本体系逐项覆盖。

---

## 4. 核心概念速查

### 4.1 核心组件速查

| 组件 | 作用 | 关键注解/类 |
|------|------|------------|
| 实体映射 | Java 类 ↔ 表 | `@TableName`/`@TableId`/`@TableField` |
| 通用 Mapper | 内置 CRUD 方法 | `BaseMapper<T>` |
| 条件构造器 | 动态 SQL 条件 | `LambdaQueryWrapper`/`QueryWrapper` |
| Service 封装 | 业务层基类 | `IService<T>`/`ServiceImpl` |
| 插件体系 | SQL 拦截增强 | `MybatisPlusInterceptor` |
| 分页 | 物理分页 | `PaginationInnerInterceptor` |
| 代码生成 | 一键生成分层代码 | `FastAutoGenerator` |

### 4.2 核心注解速查

| 注解 | 作用 |
|------|------|
| `@TableName("user")` | 实体 → 表名 |
| `@TableId(type = IdType.ASSIGN_ID)` | 主键策略（雪花 ID） |
| `@TableField(fill = FieldFill.INSERT)` | 字段映射/自动填充 |
| `@TableLogic` | 逻辑删除字段 |
| `@Version` | 乐观锁版本字段 |
| `@TableField(exist = false)` | 非表字段（如计算字段） |

### 4.3 版本时间线（时效性重点）

| 版本 | 时间 | 关键变化 |
|------|:----:|---------|
| 3.5.9 | 2024 | **分页等插件拆为可选依赖**（需 `mybatis-plus-jsqlparser`） |
| 3.5.13 | 2025 | **Spring Boot 4 支持**（`mybatis-plus-spring-boot4-starter`） |
| 3.5.17 | 2026-07 | GraalVM native-image 模块、eqOrIsNull、Lambda 增强 |
| 3.5.x | 持续 | 3 个月左右一个版本，追 Latest 需看 release notes |

---

## 5. 与周边知识的关系

```text
                    ┌── MyBatis/ 系统 —— 核心架构/XML/动态SQL/插件/源码（MP 的地基）
                    ├── Spring 框架核心/ —— 事务、AOP（MP 与 Spring 的整合点）
                    ├── MySQL/ —— 索引、分页、唯一约束（MP 优化的数据库侧）
                    ├── 多数据源（dynamic-datasource）—— 分页失效场景
                    └── 分布式ID —— 雪花算法（MP 主键默认策略）
```

**本体系与 MyBatis/ 系统的分工**：MyBatis/ 讲"MyBatis 本身"（架构、XML、动态 SQL、缓存、源码）；本体系讲"MyBatis-Plus 增强层"（通用 CRUD、Wrapper、插件、代码生成）——`MyBatis/07-MyBatis-Plus快速开发.md` 是快速入门，本体系是完整深度系统。

---

## 6. 学习路线推荐

**路线一：快速上手（1~2 天，对应模块 01-03）**
定位与版本 → 实体注解 → BaseMapper CRUD——一个实体类 + 一个 Mapper 跑通全部单表操作，重点理解主键策略与逻辑删除。

**路线二：进阶深化（1 周，对应模块 04-07）**
Wrapper（重点 null 陷阱与动态条件）→ IService 批量操作 → 插件体系（重点分页依赖与配置）→ 代码生成器；用 FastAutoGenerator 生成一个完整模块练手。

**路线三：生产实战（对应模块 08-09）**
生产坑清单（逻辑删除冲突/分页失效/乐观锁）→ 性能优化 → 面试冲刺；结合 `MyBatis/` 系统的 XML 与源码补齐底层认知。

> 🎯 **核心要点**：MP 的学习终点 = "**单表开发 10 分钟一个模块，复杂查询知道何时回归原生 SQL**"——会配置、懂语义、知边界，三件事缺一不可。

---

## 7. 快速自测 10 题

1. MP 和 MyBatis 是什么关系？分页插件从哪个版本开始需要额外依赖？
2. `@TableId(type = IdType.ASSIGN_ID)` 用什么生成主键？自增主键怎么配？
3. 逻辑删除后，`selectById` 还能查到已删除的数据吗？自定义 SQL 会自动拼逻辑删除条件吗？
4. `.eq(User::getStatus, null)` 会怎样？正确写法是什么？
5. `updateById` 会把字段设为 null 吗？怎么显式置 null？
6. `selectPage` 的页码从几开始？传 0 会怎样？
7. 乐观锁冲突时 MP 的行为是什么？为什么必须检查返回值？
8. 逻辑删除 + 唯一索引会有什么问题？怎么解决？
9. `saveBatch` 为什么比循环 insert 快？默认批量多大？
10. 多表联查应该用 Wrapper 还是原生 SQL？为什么？

> 💡 答不出的题目对应上方模块导航中的文件，按图索骥复习即可。

---

**下一模块**：[01-定位与快速上手](./01-定位与快速上手.md)
