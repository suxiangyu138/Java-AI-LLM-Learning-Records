# 00 - Oracle 知识体系总览

> 🎯 Oracle 是企业级数据库的标杆——PL/SQL、RAC 集群、Data Guard 容灾、AWR 性能报告。Java 后端开发必须掌握 Oracle 与 MySQL 的核心差异

> 🎯 共 **12 篇**，从安装到性能、从 SQL 到高可用

---

## 1. 知识全景

```
Oracle 体系（12个文件）
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-Oracle架构与安装部署.md
│   ├── 02-SQL基础与核心语法.md
│   └── 03-PLSQL存储过程与函数.md
│
├── 🔧 进阶篇（04-06）
│   ├── 04-索引与执行计划.md
│   ├── 05-事务与锁机制.md
│   └── 06-表空间与数据文件管理.md
│
├── 🚀 高可用篇（07-09）
│   ├── 07-DataGuard容灾与备份恢复.md
│   ├── 08-RAC集群与负载均衡.md
│   └── 09-性能诊断AWR与ASH.md
│
├── 📋 整合篇（10）
│   └── 10-Java与Oracle整合实战.md
│
└── 📌 冲刺篇（11）
    └── 11-面试高频考点与总结.md
```

## 2. Oracle vs MySQL 速查

| 维度 | Oracle | MySQL |
|------|--------|-------|
| 事务隔离 | READ COMMITTED(默认) | REPEATABLE READ(默认) |
| 自增主键 | SEQUENCE | AUTO_INCREMENT |
| 分页 | `ROWNUM` / `OFFSET FETCH` | `LIMIT` |
| 字符串拼接 | `\|\|` | `CONCAT()` |
| 存储过程 | PL/SQL | 标准 SQL 存储过程 |
| 数据泵 | expdp / impdp | mysqldump |
| 高可用 | RAC + Data Guard | MGR / MHA |
| 授权模式 | License 按 CPU/用户 | 开源免费 |
