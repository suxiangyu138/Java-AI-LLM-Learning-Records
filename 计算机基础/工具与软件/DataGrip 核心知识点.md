# DataGrip 核心知识点

## 一、概述

DataGrip 是 JetBrains 公司推出的数据库 IDE，与 IntelliJ IDEA、PyCharm 等共享代码底座。它支持几乎所有主流数据库，以强大的 SQL 智能补全和数据分析能力著称。

**核心定位：** 数据库领域的 IDE，SQL 智能辅助最强，JetBrains 生态用户首选。

**官网：** https://www.jetbrains.com/datagrip/

## 二、核心功能

### 2.1 支持的数据库

| 类型 | 具体数据库 |
|------|-----------|
| **关系型** | MySQL、PostgreSQL、Oracle、SQL Server、SQLite、MariaDB、DB2 |
| **NoSQL** | MongoDB、Cassandra、Redis、ClickHouse |
| **云数据库** | AWS RDS、Azure SQL、Google Cloud SQL |

### 2.2 智能 SQL 辅助

| 功能 | 说明 |
|------|------|
| **上下文感知补全** | 表名、列名、JOIN 条件自动补全，基于 Schema 实时分析 |
| **SQL 重构** | 重命名表/列/别名，安全跨文件修改 |
| **SQL 格式化** | 内置多种风格模板，支持自定义规则 |
| **SQL 注入检测** | 自动高亮潜在的 SQL 注入风险代码 |
| **快速文档** | Ctrl+Q 查看表结构、索引、外键 |
| **SQL → Java 代码** | 查询结果一键生成 Java Entity / DTO 类 |

### 2.3 数据编辑器

- **行内编辑**：类似 Excel 的单元格直接编辑
- **批量操作**：多行选中 + 批量修改
- **数据对比**：两张表数据 Diff
- **大文本查看**：JSON / XML 格式化展示

## 三、核心操作

### 3.1 连接配置

```
Data Source → MySQL
  Host: localhost
  Port: 3306
  Database: mydb
  User: root
  URL: jdbc:mysql://localhost:3306/mydb

Advanced 选项卡：
  useSSL=false
  serverTimezone=Asia/Shanghai
  characterEncoding=utf8mb4
```

### 3.2 数据库浏览器

```
Project 视图
├── mydb (MySQL - 8.0)
│   ├── schemas
│   │   └── mydb
│   │       ├── tables
│   │       │   ├── users
│   │       │   │   ├── columns
│   │       │   │   │   ├── id (bigint, PK, AUTO_INCREMENT)
│   │       │   │   │   ├── username (varchar)
│   │       │   │   │   └── email (varchar)
│   │       │   │   ├── indexes
│   │       │   │   └── foreign keys
│   │       │   └── orders
│   │       ├── views
│   │       └── routines (存储过程/函数)
```

### 3.3 查询控制台

```sql
-- DataGrip 特有能力
-- 1. 每个 SQL 语句独立执行
SELECT * FROM users WHERE age > 18;
SELECT COUNT(*) FROM orders;

-- 2. 参数化查询（? 自动弹出参数输入框）
SELECT * FROM users WHERE id = ? AND status = ?;

-- 3. Ctrl+Click 表名 → 跳转到表定义
-- 4. JOIN 自动推断 ON 条件
SELECT u.name, o.total
FROM users u
JOIN orders o ON ...  -- Ctrl+Space → 自动建议 u.id = o.user_id
```

## 四、高级功能

### 4.1 数据库图表

```
选中多张表 → 右键 → Diagrams → Show Visualization
自动生成 ER 关系图，可导出 PNG/SVG
```

### 4.2 数据导入导出

| 导入格式 | 导出格式 |
|----------|----------|
| CSV、TSV | CSV、TSV |
| SQL INSERT | SQL INSERT / UPDATE |
| JSON | JSON |
| Excel (.xlsx) | Excel (.xlsx) |
| 剪贴板粘贴 | HTML / Markdown 表格 |

### 4.3 版本控制集成

- SQL 脚本文件纳入 Git 管理
- 查询历史自动保存，支持搜索回溯
- 与 JetBrains Space 集成云端同步

### 4.4 IntelliJ IDEA 内置版

IntelliJ IDEA Ultimate 版已内置 DataGrip 核心功能：

```
IDEA 右侧工具栏 → Database → 添加数据源
  → 直接使用 DataGrip 的全部能力，无需切换窗口
```

## 五、DataGrip vs Navicat

| 维度 | DataGrip | Navicat |
|------|----------|---------|
| SQL 智能补全 | **极强** | 基础 |
| 代码重构 | **支持** | 不支持 |
| ER 图设计 | 只读可视化 | **可视化编辑** |
| 数据同步 | 需脚本 | **内置** |
| 备份恢复 | 基础 | **完整** |
| DBA 高级任务 | 偏开发 | **偏运维** |
| 团队协作 | Space 集成 | Navicat Cloud |
| 与 IDE 联动 | **无缝** | 独立 |
| 价格 | JetBrains 订阅 | 单独付费 |

## 六、快捷键速查

| 操作 | 快捷键 |
|------|--------|
| 执行当前语句 | `Ctrl + Enter` |
| 格式化 SQL | `Ctrl + Alt + L` |
| 跳转到表/列定义 | `Ctrl + B` |
| 查找表/列/函数 | `Double Shift` |
| 查看表结构 | `Ctrl + Q` |
| 重命名（重构） | `Shift + F6` |
| 打开查询控制台 | `Ctrl + Shift + F10` |

## 七、总结

DataGrip 是 **IntelliJ IDEA 用户的天然选择**——同样的快捷键和工作流操作数据库。核心优势在于 SQL 智能补全和代码重构能力，更适合开发阶段的数据库操作。专门的 DBA 运维任务（备份、同步）建议搭配 Navicat 使用。
