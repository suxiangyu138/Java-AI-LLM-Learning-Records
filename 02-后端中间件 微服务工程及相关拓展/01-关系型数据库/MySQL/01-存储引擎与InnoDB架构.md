# 01 - 存储引擎与 InnoDB 架构

> 定位：InnoDB 存储引擎的内存/磁盘架构、日志体系（undo/redo/binlog）、WAL 机制、MyISAM 对比与引擎选型

## 📚 目录

1. [存储引擎对比与选型](#1-存储引擎对比与选型)
2. [InnoDB 内存架构](#2-innodb-内存架构)
3. [InnoDB 磁盘架构](#3-innodb-磁盘架构)
4. [日志体系：undo / redo / binlog](#4-日志体系undo--redo--binlog)
5. [WAL 与刷盘策略](#5-wal-与刷盘策略)

---

## 1. 存储引擎对比与选型

### 1.1 引擎对比

| 维度 | InnoDB（默认） | MyISAM（历史） | MEMORY |
|------|:---:|:---:|:---:|
| 事务 | ✅ | ❌ | ❌ |
| 行级锁 | ✅ | ❌（表锁） | ❌（表锁） |
| 外键 | ✅ | ❌ | ❌ |
| 崩溃恢复 | ✅ redo log | ❌ | 重启即失 |
| 全文索引 | ✅（8.0+） | ✅ | ❌ |
| 数据存储 | 聚簇（主键组织） | 分离（.MYD/.MYI） | 内存 |
| 适用 | 业务主表 | 只读历史/报表（旧系统） | 临时表 |

```
⚠️ 面试必答：
"InnoDB 是默认与唯一正解——事务、行锁、
 崩溃恢复三件套；MyISAM 是历史遗留
 （读多写少且无事务要求的老场景）。"
```

### 1.2 引擎选择

```sql
-- 查看引擎
SHOW ENGINES;
SHOW CREATE TABLE t\G;

-- 建表指定（一般不需要，默认 InnoDB）
CREATE TABLE t (...) ENGINE = InnoDB;
```

---

## 2. InnoDB 内存架构

### 2.1 Buffer Pool（缓冲池）

```
Buffer Pool = InnoDB 的内存数据缓存（最核心组件）
  缓存：数据页 + 索引页 + 插入缓冲 + 锁信息
  作用：读写先在内存 → 减少磁盘 IO

⚠️ 关键参数：
  innodb_buffer_pool_size：默认 128MB，生产建议物理内存 60-70%
  innodb_buffer_pool_instances：多实例分片（8.0 自动）

⚠️ 面试必答：
"Buffer Pool 是 InnoDB 性能的根基——
 命中率高则读写都在内存；
 生产配置建议 60-70% 物理内存。"
```

### 2.2 内存组件总览

| 组件 | 作用 |
|------|------|
| Buffer Pool | 数据/索引页缓存 |
| Change Buffer | 二级索引变更缓冲（合并随机写） |
| Adaptive Hash Index | 自适应哈希（热数据自动建） |
| Redo Log Buffer | redo log 内存缓冲 |
| Data Dictionary | 数据字典缓存 |

```
⚠️ 面试必答：
"InnoDB 内存五件套——Buffer Pool 主缓存、
 Change Buffer 优化二级索引写、
 Adaptive Hash 加速热数据、
 Redo Log Buffer 缓冲日志。"
```

---

## 3. InnoDB 磁盘架构

### 3.1 磁盘文件

| 文件 | 作用 |
|------|------|
| 表空间（.ibd） | 数据 + 索引（每表独立，8.0 默认） |
| 系统表空间（ibdata1） | 数据字典、双写缓冲区 |
| redo log 文件（ib_logfile*） | 崩溃恢复日志 |
| undo 表空间 | 回滚段（8.0 独立于 ibdata1） |
| binlog（.000001） | 逻辑日志（Server 层） |

### 3.2 页与行

```
页（Page）：InnoDB 最小存储单位，默认 16KB
行（Row）：页内记录，聚集在聚簇索引叶子

⚠️ 面试必答：
"页 16KB 是 IO 的基本单位——
 B+Tree 一层一页，树高 3-4 层
 即 3-4 次磁盘 IO（见 04 索引量化分析）。"
```

---

## 4. 日志体系：undo / redo / binlog

### 4.1 三种日志的分工

| 日志 | 层级 | 作用 | 时机 |
|------|:---:|------|------|
| undo log | InnoDB | 原子性（回滚）+ MVCC 版本链 | 修改前记录旧值 |
| redo log | InnoDB | 持久性（崩溃恢复） | 修改时先写（WAL） |
| binlog | Server | 复制 + 点恢复 | 事务提交时 |

```
⚠️ 面试必答：
"三日志分工——undo 保原子（旧值回滚）、
 redo 保持久（WAL 崩溃恢复）、
 binlog 保复制（主从同步）。"
```

### 4.2 两阶段提交（redo 与 binlog 的一致性）

```
事务提交时：
  ① 写 redo log（prepare 状态）
  ② 写 binlog
  ③ redo log（commit 状态）
  → 崩溃恢复时以 binlog 为准协调两者

⚠️ 为什么两阶段：
  redo 与 binlog 分属两个组件，
  必须保证"要么都成功要么都失败"——
  两阶段提交 + 崩溃恢复检查。

⚠️ 面试必答：
"两阶段提交保证 redo 与 binlog 一致——
 prepare → binlog → commit；
 崩溃恢复用 binlog 判断事务是否应生效。"
```

### 4.3 刷盘参数（持久性与性能的权衡）

```
innodb_flush_log_at_trx_commit：
  =1：每次提交刷盘（最安全，默认）
  =0：每秒刷盘（最快，可能丢 1 秒）
  =2：写 OS cache 每秒刷（折中）

⚠️ 面试必答：
"'每次提交刷盘'最安全但慢；
 每秒刷盘快但崩溃丢 1 秒——
 金融场景 =1，日志场景可 =2。"
```

---

## 5. WAL 与刷盘策略

### 5.1 WAL 为什么快

```
WAL（Write-Ahead Logging）：
  修改数据 → 先写 redo log（顺序写，快）
            → 再异步刷数据页（随机写，慢）

⚠️ 顺序写 vs 随机写：
  顺序写磁盘 ≈ 内存速度的 1/10
  随机写磁盘 ≈ 内存速度的 1/1000
  → WAL 把"随机写"转成"顺序写日志"

⚠️ 面试必答：
"WAL = 先日志后数据——
 随机写转顺序写是性能关键；
 redo log 追加写、数据页随机刷。"
```

### 5.2 刷盘时机

```
数据页何时刷盘（从 Buffer Pool 到磁盘）：
  ① redo log 满（checkpoint 触发）
  ② 内存不足（LRU 淘汰脏页）
  ③ 后台线程定时刷
  ④ 正常关闭

⚠️ 面试必答：
"脏页刷盘由 checkpoint 与 LRU 驱动——
 不是'提交就刷数据页'（提交只刷 redo log）。"
```

---

> 🎯 **核心要点**：存储引擎 = **InnoDB 默认**（事务/行锁/崩溃恢复）+ **内存架构**（Buffer Pool 60-70% 内存）+ **日志三件套**（undo 原子/redo 持久/binlog 复制）+ **WAL**（随机写转顺序写）。两阶段提交保证 redo/binlog 一致性是事务提交的隐藏考点。

---

**返回总览**：[00-MySQL总览与技术术语](00-MySQL总览与技术术语.md) | **下一篇**：[02-SQL进阶与查询技巧](02-SQL进阶与查询技巧.md)
