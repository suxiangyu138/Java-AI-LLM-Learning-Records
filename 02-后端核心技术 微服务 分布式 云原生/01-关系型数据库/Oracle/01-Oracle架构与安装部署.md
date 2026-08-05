# 01 - Oracle 架构与安装部署

> 🎯 Oracle 的架构和 MySQL 截然不同——实例(Instance) vs 数据库(DB)、SGA 共享内存、PGA 私有内存、表空间管理体系。理解架构是管理 Oracle 的基础

---

## 目录

1. [核心架构](#1-核心架构)
2. [内存结构 SGA + PGA](#2-内存结构-sga--pga)
3. [表空间与数据文件](#3-表空间与数据文件)

---

## 1. 核心架构

```text
Oracle 架构 = 实例 (Instance) + 数据库 (Database)

实例 (Instance) = 内存 + 后台进程：
  ├── SGA (System Global Area) — 共享内存
  ├── PGA (Program Global Area) — 每个会话的私有内存
  └── 后台进程（DBWn/LGWR/CKPT/SMON/PMON...）

数据库 (Database) = 物理文件：
  ├── 数据文件 (.dbf) → 属于某个表空间
  ├── 控制文件 (.ctl) → 数据库结构信息
  ├── 重做日志 (Redo Log) → 事务日志
  └── 归档日志 (Archive Log) → Redo Log 的备份

对比 MySQL：
  MySQL InnoDB = 缓冲池(Buffer Pool) + 共享表空间 + Redo Log + Undo
  Oracle 的 SGA ≈ MySQL 的 Buffer Pool（但 SGA 包含更多组件）
```

## 2. 内存结构 SGA + PGA

```text
SGA (System Global Area) — 所有会话共享：

  Database Buffer Cache    → 缓存数据块（最重要，类似 InnoDB Buffer Pool）
  Shared Pool               → 缓存 SQL + 数据字典
    ├── Library Cache       → 解析后的 SQL 执行计划
    └── Data Dictionary Cache → 表结构/权限信息
  Redo Log Buffer           → 事务修改记录（未写入磁盘的）
  Large Pool                → RMAN 备份 / 并行查询
  Java Pool                 → JVM 内存

PGA (Program Global Area) — 每个会话私有：
  Sort Area                 → 排序操作的临时空间
  Hash Area                 → Hash Join 的临时空间
  Session Memory            → 会话变量/游标
```

```sql
-- 查看 SGA 大小和组件
SHOW PARAMETER sga_target;
SELECT component, current_size/1024/1024 AS mb FROM v$sga_dynamic_components;
```

## 3. 表空间与数据文件

```text
逻辑存储层次（由上到下）：
  Database
    └── 表空间 (Tablespace)  ← 逻辑概念
          └── 段 (Segment)   ← 表/索引
                └── 区 (Extent)
                      └── 块 (Block) ← 最小 I/O 单位

物理存储：
  表空间 → 对应 1 个或多个数据文件 (.dbf)
```

```sql
-- 创建表空间
CREATE TABLESPACE app_data
    DATAFILE '/u01/oradata/app_data01.dbf' SIZE 1G
    AUTOEXTEND ON NEXT 100M MAXSIZE 10G;

-- 创建用户 + 指定默认表空间
CREATE USER app_user IDENTIFIED BY "password"
    DEFAULT TABLESPACE app_data
    TEMPORARY TABLESPACE temp;
GRANT CONNECT, RESOURCE TO app_user;
```

## 核心要点回顾

- Oracle = Instance(内存+进程) + Database(物理文件)
- SGA = Buffer Cache + Shared Pool + Redo Buffer
- 表空间是逻辑概念，对应物理 .dbf 文件
- 每个用户独立 Schema（默认 = 用户名）

## 参考资料

1. Oracle Database Concepts Guide
