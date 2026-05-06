# 数据库配置与部署（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MySQL 配置与部署  
> **核心原则**：环境隔离、安全部署、数据备份  
> **前置基础**：MySQL 操作、数据库优化、权限管理

---

## 一、核心概念

### 1.1 核心目标

| 目标 | 说明 |
|------|------|
| **环境适配** | 开发/测试/生产环境配置独立，避免不一致导致连接失败 |
| **稳定运行** | 合理配置连接池、缓存、日志，保障 7×24 小时可用 |
| **安全高效** | 最小权限账号 + 参数优化，兼顾安全与性能 |

> **环境隔离铁律**：开发、测试、生产数据库必须分开部署、独立配置，禁止混用。

---

## 二、底层原理

### 2.1 数据库连接管理

```
Java 应用 → HikariCP 连接池 → TCP → MySQL Server
                                    ├── 连接线程（max_connections）
                                    ├── InnoDB Buffer Pool（缓存热数据）
                                    └── 慢查询日志（记录慢 SQL）
```

### 2.2 关键配置项的作用链路

| 配置 | 作用 |
|------|------|
| `innodb_buffer_pool_size` | 缓存热数据页，减少磁盘 IO |
| `max_connections` | 限制并发连接数，防止 MySQL 被打爆 |
| `slow_query_log` | 记录执行超时 SQL，便于优化 |
| HikariCP `maximum-pool-size` | 连接池最大连接数（≤ `max_connections`） |

---

## 三、代码实现

### 3.1 MySQL 核心参数配置（my.cnf / my.ini）

```ini
[mysqld]
# 1. 基础配置
datadir=/var/lib/mysql
character-set-server=utf8mb4
collation-server=utf8mb4_general_ci

# 2. 连接配置（高并发适配）
max_connections=1000                # 最大连接数（默认151，需提升）
wait_timeout=600                    # 连接超时（10分钟）
interactive_timeout=600

# 3. 性能优化
innodb_buffer_pool_size=4G          # 缓存大小（建议内存的50%-70%）
innodb_log_file_size=1G             # 事务日志大小
innodb_flush_log_at_trx_commit=1    # 每次提交刷盘（保证持久性）

# 4. 慢查询日志
slow_query_log=ON
long_query_time=1                   # 阈值1秒
slow_query_log_file=/var/log/mysql/slow.log

# 5. 安全配置
skip-grant-tables=0                 # 禁止跳过权限验证
sql_mode=STRICT_TRANS_TABLES        # 严格模式
```

### 3.2 Java 后端连接配置（Spring Boot + HikariCP）

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://192.168.1.100:3306/db_ecommerce?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false
    username: db_ecommerce_user       # 专用账号（非root）
    password: Xx@123456
    hikari:
      maximum-pool-size: 50           # ≤ MySQL max_connections
      minimum-idle: 10
      connection-timeout: 3000        # 3秒
      idle-timeout: 600000            # 10分钟

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.ecommerce.entity
  configuration:
    map-underscore-to-camel-case: true
```

### 3.3 部署流程

| 环境 | 流程 | 要点 |
|------|------|------|
| **本地开发** | 安装 MySQL → 创建 `db_ecommerce_dev` → 创建开发账号 → 配置 `application-dev.yml` | 快速搭建，无需复杂配置 |
| **测试环境** | 独立服务器 → 与生产配置接近 → 模拟数据 → 性能测试 | 模拟生产配置，数据隔离 |
| **生产环境** | 独立服务器（SSD 磁盘） → 优化配置 → SSL 加密 → 监控部署 | 严格安全配置，备份策略 |

### 3.4 数据备份与恢复

```bash
# 全量备份
mysqldump -u root -p db_ecommerce > /backup/db_ecommerce_$(date +%Y%m%d).sql

# 恢复
mysql -u root -p db_ecommerce < /backup/db_ecommerce_20241001.sql

# 定时备份（crontab）
0 3 * * * mysqldump -u root -p'xxx' db_ecommerce > /backup/db_$(date +\%Y\%m\%d).sql
```

---

## 四、实战要点

### 4.1 连接池配置原则

- `maximum-pool-size` ≤ MySQL `max_connections` - 预留 20%
- 连接超时时间不宜过长（3-5 秒）
- 空闲连接定期回收（`idle-timeout`）

### 4.2 环境隔离标准

```
开发环境     → db_xxx_dev    + dev 账号
测试环境     → db_xxx_test   + test 账号
生产环境     → db_xxx        + 最小权限账号
```

---

## 五、避坑总结

| 坑点 | 正确做法 |
|------|----------|
| **驱动类不匹配** | MySQL 8.0+ 用 `com.mysql.cj.jdbc.Driver` |
| **连接池超配** | `pool-size` 不超过 `max_connections` |
| **配置文件明文密码** | 生产环境使用配置中心加密（Jasypt/Config加密） |
| **环境数据混用** | 开发/测试/生产数据库物理隔离 |
| **忘记备份** | 每日自动全量备份 + binlog 增量备份 |
| **buffer_pool 过大** | 不超过服务器内存的 70%，预留 OS 内存 |

---

## 六、企业级最佳实践

### 6.1 部署 Checklist

- [ ] MySQL 版本与生产一致
- [ ] 配置文件已优化（buffer_pool / max_connections / slow_log）
- [ ] 专用账号已创建，root 禁止远程连接
- [ ] 连接池参数已合理配置
- [ ] SSL 加密连接已开启（生产）
- [ ] 定时备份任务已配置
- [ ] 监控告警已部署（Prometheus + Grafana）
- [ ] 数据库访问白名单已配置（仅应用服务器 IP）

### 6.2 运维监控指标

| 指标 | 告警阈值 |
|------|----------|
| 连接数使用率 | > 80% |
| 慢查询数量 | > 10 条/小时 |
| 磁盘使用率 | > 85% |
| QPS 异常波动 | 偏离基线 50% |
