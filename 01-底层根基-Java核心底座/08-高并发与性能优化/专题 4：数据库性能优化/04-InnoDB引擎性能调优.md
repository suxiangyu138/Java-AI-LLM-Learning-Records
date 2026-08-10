# 04 InnoDB 引擎性能调优

> InnoDB 是性能参数的主战场——Buffer Pool 决定读多快，脏页刷盘决定写多稳，redo log 决定崩溃恢复多快；2026 年参数语义与 5.7 时代已大不相同，先理解机制再动参数

---

## 📚 目录

1. [Buffer Pool：读性能的地基](#1-buffer-pool读性能的地基)
2. [脏页刷盘：写稳定性的核心](#2-脏页刷盘写稳定性的核心)
3. [redo log 与组提交](#3-redo-log-与组提交)
4. [并行查询参数与其他](#4-并行查询参数与其他)

---

## 1. Buffer Pool：读性能的地基

Buffer Pool 缓存数据页与索引页，命中率高意味着查询几乎不碰磁盘。三个核心决策：

- **尺寸**：专用实例 70-80% 物理内存，共享实例 50-60%，永远给 OS 留 1-2GB——堆满后 OS 会 swap，比命中率低更可怕。命中率公式 `1 - Innodb_buffer_pool_reads / Innodb_buffer_pool_read_requests`，长期低于 95-99% 说明池太小或数据量超内存。
- **实例数**：超过 1GB 拆多实例减少 mutex 竞争，大致 1-2GB 一个，上限 64，且**与 CPU 核数对齐**（16 核配 16 个）。
- **预热**：`innodb_buffer_pool_dump_at_shutdown=ON` + `innodb_buffer_pool_load_at_startup=ON` + `innodb_buffer_pool_dump_pct=25`（只热存最热的 25%），重启后分钟级恢复命中率，大促重启的保命配置。

**不要盲目套百分比**：混合负载下用 `Innodb_buffer_pool_wait_free`（非零 = 频繁缺空闲页）与 `Innodb_buffer_pool_pages_dirty` 佐证，而不是只看规则。命中率骤降多半是刷盘太激进把热页挤出去了（见下节），先查刷盘再考虑扩容。还有一个容易被误读的点：**数据总量超过内存时命中率必然低于 100%**，此时盯着命中率调参不如确认"热点数据是否都在池内"——分析型大查询会周期性把冷数据带进池、挤走热页，这正是混合负载场景的常态。把"命中率 + 脏页 + 等待"三指标一起看，比单看命中率更能反映真实水位。

## 2. 脏页刷盘：写稳定性的核心

写请求先改内存中的页（脏页），由 page cleaner 线程异步刷回磁盘。刷太快抢业务 IO、刷太慢脏页堆积触发"狂暴刷盘"（emergency flushing）导致延迟尖刺。核心参数 `innodb_io_capacity` 按**存储真实 IOPS** 取值，这是 2026 年最有据可依的对照：

| 存储类型 | innodb_io_capacity 建议 | 说明 |
|---|---|---|
| SATA HDD | 100-200 | 超过 200 磁盘响应直接崩溃 |
| SATA SSD | 1000-2000 起步 | 可提到 4000-6000，边提边看 iostat %util |
| NVMe SSD | 20000-80000 | 8.4 官方提示：超 20000 罕见有益 |
| 云盘（gp3/ESSD） | 额定随机写 IOPS × 70% | 如额定 16000 → 约 11200 |

配套规则：

- `innodb_io_capacity_max` = 基础值的 **2-3 倍**，作为突发欠账时的弹性上限；设成等于基础值等于关闭弹性。
- `innodb_flush_sync=ON`（默认）时，检查点紧急时 InnoDB 可能**无视 io_capacity 全力刷盘**——想要严格限速就关掉它。
- `innodb_max_dirty_pages_pct`：默认 75，压测确认过高的可降到 75 以下留缓冲；`innodb_max_dirty_pages_pct_lwm` 设 0.001 左右让刷盘更平滑。
- `innodb_lru_scan_depth`：默认 1024 对 SSD 偏浅，提到 2560-4096 防止热页被过早逐出。
- `innodb_adaptive_flushing` 默认 ON；有团队在**批量高密度更新**场景关掉它换固定速率更稳，两种声音都存在——用 24 小时脏页曲线自己验证，别盲从。

监控三件套：`Innodb_buffer_pool_pages_dirty` 持续超过总页数 10% 告警、`Innodb_buffer_pool_wait_free` 非零、`Innodb_log_waits` 非零——三者分别对应刷盘慢、缺页、redo 落后。

云盘实例有个特殊点：云盘的 IOPS 是"信用积分"模型（突发额度用完回落），io_capacity 取额定值的 70% 就是给突发留额度；同时监控要看 iostat 的 await 而非只看 %util——云盘 await 抖动比物理盘更频繁，是刷盘参数调整的第一反馈信号。

## 3. redo log 与组提交

redo log 记录页的物理变更，是崩溃恢复与刷盘节奏的枢纽：

- **8.4 起 `innodb_redo_log_capacity` 单参数取代旧的双参数**（log_file_size + log_files_in_group），直接指定总容量（如 4G）。升级时该参数必须清理后设置。
- **组提交**：多个事务的 fsync 合并为一次，`innodb_flush_log_at_trx_commit=1` 时每条 commit 都 fsync，靠组提交把磁盘 IO 摊薄——连接并发越高、组提交收益越大，这也是"连接多反而整体吞吐高"的机制之一。
- 容量与恢复时长正相关：redo 越大，崩溃后重放越久。`Innodb_log_waits` 持续非零说明 redo 写入落后，先看磁盘延迟，再考虑扩容。
- 刷盘节奏与 redo 联动：redo 快满会触发前述狂暴刷盘——`io_capacity`、redo 容量、脏页上限三者是一套系统，调一个必须看另两个。
- redo 与 binlog 的写入路径不同：redo 是 InnoDB 层、循环覆盖、崩溃恢复用；binlog 是服务层、追加、复制与回放用。两者都走组提交，`sync_binlog=1` 与 `innodb_flush_log_at_trx_commit=1` 双 1 配置下，一次 commit 的 fsync 数量被组提交摊薄——这也是为什么压测标准配置强制双 1（[08 篇](08-数据库压测与容量规划.md) 第二节），非双 1 的配置会高估吞吐。

## 4. 并行查询参数与其他

8.4 并行查询（默认开）的参数归属引擎层：`parallel_query` 总开关、`parallel_threads_limit`（默认 4，**16 是多数场景甜点**，上限 64）、`innodb_parallel_read_threads`（全局并行扫描线程池，自适应）。它们只对"注定全扫"的查询（大表 COUNT/聚合/DDL）起作用，与索引决策的关系见 [02 篇](02-索引与查询性能设计.md) 第四节。

其余两点补充：

- `innodb_change_buffering` 8.4 默认 `none`：批量写 + 多二级索引的表可能微降，属预期行为；确认 SSD 随机写能力足够即可，不必改回 `all`。
- **doublewrite 与页撕裂**：InnoDB 崩溃恢复必须保证"页要么全新要么全旧"，8.0 起 doublewrite 默认启用，把页先整页写入缓冲再落盘，防半页写入。它让每次刷盘多写一份，是写路径的固定税——NVMe 上收益大于开销，不建议关闭；只有写放大敏感且存储自带原子写保障的特殊场景才评估。页撕裂只在崩溃恢复后暴露，平时不可见，属于保险型开销，别为了省税拆保险。
- **一次只改一个参数，压测或 24 小时监控验证后再动下一个**；生产改动用 `SET PERSIST` 热生效并留回滚快照。参数是全套配合的，单独调 `io_capacity` 而不看脏页与 redo，等于盲调。

**调参后的验证闭环**：参数改完不等于调完，观察 24 小时的四个信号——脏页曲线是否平稳（无周期性尖峰）、Innodb_log_waits 是否归零、命中率是否稳定、RT 的 P95 是否如预期回落。四个信号任何一个恶化都要回滚参数；刷盘参数尤其如此，它的效果要隔一个完整的业务周期才能看清（白天高峰刷盘压力大、凌晨低峰不明显），当天看可能"没变化"。

> 🎯 **核心要点**：Buffer Pool 尺寸/实例/预热三件套决定读性能，70-80% 封顶且实例数与核数对齐；innodb_io_capacity 按存储真实 IOPS 取值、io_capacity_max 取 2-3 倍，flush_sync 狂暴模式与脏页/redo 三件联动；8.4 用 innodb_redo_log_capacity 单参数管容量，组提交让高并发写收益递增；并行查询 16 线程是甜点；所有参数一次一变量、压测验证、SET PERSIST 可回滚。

---

**下一模块**：[05 事务 MVCC 与锁竞争调优](05-事务MVCC与锁竞争调优.md)

**返回总览**：[00-总览](00-总览.md)
