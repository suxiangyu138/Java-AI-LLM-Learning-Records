# 第11章 MyBatis 开源项目

## 11.1 MyBatis 官方开源项目

### 11.1.1 核心项目：mybatis-3
- **仓库地址**：https://github.com/mybatis/mybatis-3
- **定位**：MyBatis 核心框架，SQL 映射与持久层基础实现
- **许可证**：Apache License 2.0
- **核心功能**：
    - 支持 XML/注解配置 SQL
    - 结果映射、动态 SQL、一级/二级缓存
    - 插件机制、事务管理、多数据库兼容

### 11.1.2 官方生态项目（GitHub 组织）
MyBatis 官方 GitHub 组织（https://github.com/mybatis）包含 37+ 开源仓库，核心如下：

| 项目 | 说明 |
|---|---|
| **spring-boot-starter** | SpringBoot 集成启动器，自动装配 MyBatis |
| **generator** | MyBatis 代码生成器，生成 Mapper/XML/实体类 |
| **mybatis-dynamic-sql** | 动态 SQL 构建器（Java/Kotlin DSL） |
| **jpetstore-6** | MyBatis+Spring 官方示例项目（电商） |
| **migrations** | 数据库版本迁移工具（类似 Flyway） |

---

## 11.2 国内主流 MyBatis 增强开源项目

### 11.2.1 MyBatis-Plus（最流行）
- **仓库地址**：https://github.com/baomidou/mybatis-plus
- **定位**：MyBatis 增强工具，**只增强不修改**，简化开发
- **核心特性**：
    - **BaseMapper**：内置 CRUD，无需手写 XML
    - **条件构造器（QueryWrapper）**：Java 链式编程，避免硬编码 SQL
    - **代码生成器**：一键生成 Entity/Mapper/Service/Controller
    - **内置分页插件**：支持多数据库分页
    - **逻辑删除、自动填充、乐观锁、多租户**
- **生态**：配套 IDEA 插件 **MybatisX**（跳转/补全/生成代码）


### 11.2.2 TkMyBatis
- **仓库地址**：https://github.com/abel533/Mapper
- **定位**：轻量级通用 Mapper，提供单表 CRUD
- **核心特性**：
    - **通用 Mapper 接口**：`BaseMapper` 提供 20+ 常用方法
    - **分页插件**：物理分页，支持多数据库
    - **简单配置**：依赖少、侵入低、易集成
- **对比 MP**：功能轻量、配置简单；MP 功能更全、生态更强

### 11.2.3 PageHelper（分页插件）
- **仓库地址**：https://github.com/pagehelper/Mybatis-PageHelper
- **定位**：MyBatis 通用分页插件，国内最常用
- **核心特性**：
    - **自动分页**：拦截 SQL，自动添加 `limit`
    - **多数据库支持**：MySQL/Oracle/SQL Server 等
    - **简单易用**：`PageHelper.startPage(pageNum, pageSize)`
    - **无侵入**：不修改原有 SQL，仅拦截增强

---

## 11.3 其他相关开源项目

### 11.3.1 MyBatis-Plus 生态
- **Mybatis-Mate**：企业级增强，分库分表、数据权限、字段加密
- **Dynamic-Datasource**：多数据源路由，支持 Seata 分布式事务

### 11.3.2 代码生成工具
- **MyBatis Generator（MBG）**：官方代码生成器，配置灵活
- **AutoGenerator（MP）**：MP 内置生成器，一键生成全层代码

### 11.3.3 监控与可视化
- **MyBatis-Plus-Visual**：MP 可视化控制台，监控 SQL 执行、性能分析

---

## 11.4 项目选型对比
| 项目 | 优点 | 缺点 | 适用场景 |
|---|---|---|---|
| **原生 MyBatis** | 灵活、可控、无侵入 | 重复 CRUD、XML 繁琐 | 复杂 SQL、高性能需求 |
| **MyBatis-Plus** | 功能全、生态强、开发快 | 学习成本、部分功能冗余 | 中大型企业、快速开发 |
| **TkMyBatis** | 轻量、简单、易集成 | 功能少、社区活跃度低 | 小型项目、简单 CRUD |
| **PageHelper** | 简单、无侵入、兼容性好 | 仅分页、无其他增强 | 需分页的原生 MyBatis 项目 |

---

## 11.5 本章总结
1. **官方核心**：mybatis-3 是基础，官方生态提供集成、生成、迁移工具。
2. **国内主流**：**MyBatis-Plus** 企业首选，功能全面；TkMyBatis 轻量替代。
3. **工具类**：PageHelper 解决分页痛点；MBG/MP 生成器提升开发效率。
4. **选型建议**：新项目优先 **SpringBoot + MyBatis-Plus**；旧项目可集成 PageHelper 或 TkMyBatis。
