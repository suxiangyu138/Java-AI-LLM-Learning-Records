# 07 - Data Guard 容灾与备份恢复

> 🎯 Oracle Data Guard 是企业级容灾方案——物理 Standby 实时同步，主库故障自动切换到备库（FSFO）。同时 RMAN 是 Oracle 的备份恢复利器，支持增量备份、块级恢复

---

## 目录

1. [Data Guard 架构](#1-data-guard-架构)
2. [RMAN 备份恢复](#2-rman-备份恢复)
3. [Flashback 闪回](#3-flashback-闪回)

---

## 1. Data Guard 架构

```text
Data Guard = Primary + Standby(s)

Primary (主库)                      Standby (备库)
    │                                    │
    ├── LGWR → Redo Log                  ├── RFS → 接收 Redo
    │                                    │
    └── ARCH → Archive Log ──→ 网络 ──→  ├── MRP → 应用 Redo
                                         └── 只读查询（Active DG）
```

### 三种保护模式

| 模式 | 数据丢失 | 性能影响 | 适用场景 |
|------|:---:|:---:|------|
| **Maximum Protection** | 零丢失 | 高 | 金融核心系统 |
| **Maximum Availability** | 零丢失(正常时) | 中 | **推荐** ✅ |
| **Maximum Performance** | 可能丢失 | 低 | 报表查询 |

```sql
-- 查看 Data Guard 状态
SELECT database_role, protection_mode, switchover_status FROM v$database;

-- 查看备库延迟
SELECT name, value FROM v$dataguard_stats WHERE name = 'apply lag';
```

## 2. RMAN 备份恢复

```bash
# RMAN 备份命令
rman target /

# 全库备份 + 归档日志
BACKUP DATABASE PLUS ARCHIVELOG DELETE INPUT;

# 增量备份（只备份变化的块）
BACKUP INCREMENTAL LEVEL 1 DATABASE;

# 备份压缩（节省空间 70%+）
BACKUP AS COMPRESSED BACKUPSET DATABASE;

# 验证备份完整性
RESTORE DATABASE VALIDATE;
```

```sql
-- 恢复到某个时间点（不完整恢复）
STARTUP MOUNT;
RUN {
    SET UNTIL TIME "TO_DATE('2026-07-28 10:00:00','YYYY-MM-DD HH24:MI:SS')";
    RESTORE DATABASE;
    RECOVER DATABASE;
}
ALTER DATABASE OPEN RESETLOGS;
```

## 3. Flashback 闪回

```text
Oracle 独有的"时间机器"——不需要恢复备份就能回到过去

Flashback 层次：
  Level 1: Flashback Query → 查看历史数据
  Level 2: Flashback Table → 恢复单表
  Level 3: Flashback Drop → 恢复误删表
  Level 4: Flashback Database → 恢复整个库（最快！）
```

```sql
-- 1. 查看 10 分钟前的数据
SELECT * FROM users AS OF TIMESTAMP SYSTIMESTAMP - INTERVAL '10' MINUTE;

-- 2. 恢复误删的表（回收站！）
FLASHBACK TABLE users TO BEFORE DROP;
-- 或指定还原名
FLASHBACK TABLE "BIN$xxx" TO BEFORE DROP RENAME TO users_restored;

-- 3. 恢复整表到指定时间
FLASHBACK TABLE users TO TIMESTAMP SYSTIMESTAMP - INTERVAL '1' HOUR;

-- 4. 开启/关闭回收站
ALTER SYSTEM SET recyclebin = ON;
PURGE RECYCLEBIN;  -- 清空回收站
```

## 核心要点回顾

- Data Guard = Primary → Redo 传输 → Standby（实时同步）
- Maximum Availability = 零丢失(正常时) + 性能可接受（推荐）
- RMAN 增量备份 = 只备份改变的块（大幅节省备份时间）
- Flashback = Oracle 的"撤销"按钮（误删数据/表/库都可以恢复）
- 回收站 `recyclebin` = `DROP TABLE` 不会真的删除（类似 Windows 回收站）

## 参考资料

1. Oracle Data Guard Concepts and Administration
2. Oracle RMAN 备份恢复指南
