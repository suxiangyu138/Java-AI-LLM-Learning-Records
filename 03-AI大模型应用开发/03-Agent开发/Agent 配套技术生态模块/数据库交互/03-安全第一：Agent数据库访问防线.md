# 安全第一：Agent 数据库访问防线

> 核心铁律：**提示词约束不了权限，只有数据库能**。OWASP LLM06（过度代理）点名"给只读任务配写账号"是第一事故源——防线必须一层层建到数据库层。

## 1. 威胁模型：Agent 碰数据库的四种死法

| 威胁 | 攻击路径 | 后果 |
|---|---|---|
| 提示注入劫持 | 网页/文档/搜索结果里藏指令 → Agent 执行恶意查询 | 数据外泄（SELECT 敏感列） |
| 过度授权 | 只读任务配了写账号 → 注入落地可删库 | 数据破坏 |
| SQL 注入 | 用户输入拼进 SQL（模型生成的字符串里混入） | 越权读写 |
| 自毁式错误 | 模型生成 `DELETE FROM orders` 执行 | 事故性破坏 |
| 放大效应 | 出错后反复重试，每次都全表扫描 | 拖垮生产库 |

> 🎯 核心要点：**所有威胁都指向同一件事——Agent 拿着它不需要的权限**。把权限在数据库层削到最小，四个威胁同时消解大半。

## 2. 防线强度阶梯：从最弱到最强

| 层 | 机制 | 能否绕过 | 定位 |
|---|---|---|---|
| 提示词约束 | system prompt 写"只读" | ✅ 可绕过（注入即废） | 仅礼貌性引导 |
| 会话标志 | `SET default_transaction_read_only = on` | ✅ 任何用户可 SET 回 off | 仅防手滑 |
| 角色授权 | `GRANT SELECT ONLY`，不授 DML/DDL | ❌ 需超级用户才能改 | **第一道真防线** |
| 列级授权/视图 | 只暴露安全列（PII 列屏蔽） | ❌ | 敏感列防线 |
| 行级安全（RLS） | 存储层按策略过滤行 | ❌（除非 BYPASSRLS） | 多租户/权限域 |
| 读副本 | 物理备库，写入结构性不可能 | ❌ | **最强物理边界** |

> ⚠️ 会话标志（SET read_only）**不是安全边界**——任何会话用户自己就能 `SET default_transaction_read_only TO off` 恢复写。它只是防意外修改的"安全气囊"，别当成安全机制写进架构图。

## 3. 各数据库的只读落地

### PostgreSQL

```sql
-- PG 14+ 推荐：预置只读角色，覆盖现有与未来所有对象
CREATE ROLE agent_read LOGIN PASSWORD 'xxx';
GRANT pg_read_all_data TO agent_read;   -- 含 SELECT + USAGE + 未来表

-- 或显式授权（注意 ALTER DEFAULT PRIVILEGES 只作用于授权人创建的对象）
GRANT CONNECT ON DATABASE app TO agent_read;
GRANT USAGE ON SCHEMA public TO agent_read;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO agent_read;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO agent_read;
```

### MySQL

```sql
-- 专属用户 + 绑定来源 IP（不用 %）
CREATE USER 'agent_ro'@'10.0.0.%' IDENTIFIED BY 'xxx';
GRANT SELECT ON app_db.* TO 'agent_ro'@'10.0.0.%';   -- 绝不 *.* 与 FILE
-- 部署前必须验证：INSERT/UPDATE/DELETE 全部报错
```

### SQLite

```python
import sqlite3
# 方式一：URI 只读模式
conn = sqlite3.connect("file:data.db?mode=ro", uri=True)
# 方式二：authorizer 回调（编译期拒绝一切非 SELECT）
conn.set_authorizer(lambda action, *a: sqlite3.SQLITE_DENY if action != sqlite3.SQLITE_SELECT else sqlite3.SQLITE_OK)
```

## 4. PII 与敏感列屏蔽

| 手段 | 做法 | 注意 |
|---|---|---|
| 列级 GRANT | `GRANT SELECT (safe_cols) ON t` | **先 REVOKE 表级权限**——表级授权会覆盖列级限制 |
| 视图脱敏 | 视图返回掩码列（NULL/脱敏函数），撤销原表访问 | 跨库通用 |
| 应用层过滤 | 工具函数检测列名（password/ssn/email）拒绝 | 兜底，不是主防线 |
| 结果脱敏 | 查询结果回传前做脱敏处理 | 防注入把敏感列带进上下文 |

> 💡 原则：敏感列在**授权层**隐藏，而不是在应用层过滤——应用层过滤是"看见了再遮"，授权层是"根本看不见"。

## 5. 双层强制：应用层守卫 + 数据库层兜底

### 第一层：SQL 文本守卫（应用层，可被绕过）

| 规则 | 实现 |
|---|---|
| 白名单 | 仅允许 SELECT/WITH/EXPLAIN |
| 黑名单 | 拒绝 INSERT/UPDATE/DELETE/DROP/ALTER/TRUNCATE/CREATE |
| 单条语句 | 拒绝分号堆叠/多语句 |
| 强制 LIMIT | 自动钳制（默认 100，上限 1000） |
| 超时 | statement_timeout 强制（如 10s） |
| AST 解析 | JSqlParser/sqlparse 解析，识别表名过白名单、拦截副作用函数 |

> ⚠️ 字符串过滤**不是安全边界**——`SELECT side_effect_func()` 能绕开所有关键词过滤。守卫只是减负，真防线在数据库层。

### 第二层：数据库层只读（真正的边界）

```text
PostgreSQL: BEGIN READ ONLY; ... 执行; ROLLBACK;（或只读角色）
MySQL:     START TRANSACTION READ ONLY;
SQLite:    mode=ro / authorizer
```

只读事务能拦住"SELECT 里调用副作用函数"这类文本守卫抓不到的把戏。

## 6. SQL 注入防护：参数化是唯一正解

```python
# ❌ 禁止：字符串拼接
sql = f"SELECT * FROM users WHERE email = '{user_input}'"

# ✅ 正解：参数化 + 单条语句
sql = "SELECT * FROM users WHERE email = $1"
rows = cursor.execute(sql, [user_input])
```

| 规则 | 说明 |
|---|---|
| 占位符绑定 | `$1..$N` / `?` / `%s`，值由驱动绑定，永不被当作 SQL |
| 单条语句强制 | 扩展协议层拒绝多语句（PG 协议默认即如此） |
| 黑名单文件操作 | 拒绝 OUTFILE/DUMPFILE/LOAD_FILE/LOAD DATA |
| 错误脱敏 | 驱动报错信息清洗后回喂模型（不暴露表结构/连接串） |
| 凭据治理 | 连接串不进代码库；专用低权限账号；定期轮换 |

## 7. 审计与观测

| 项 | 做法 |
|---|---|
| 查询审计 | 每条 SQL 记录：会话 id、工具名、完整 SQL、耗时、行数、是否异常 |
| 审计日志 | 只记"查询尝试与元数据"，不记原始结果（防日志泄密） |
| 异常告警 | 全表扫描、无 LIMIT、连续失败重试、敏感表访问 → 告警 |
| 链路追踪 | 请求 id 贯穿 UI→Agent→工具→数据库（对接 [前端演示界面](..%2F前端演示界面%2F00-前端演示界面总览.md) 10 篇可观测性） |

## 8. 安全基线速查（上线前逐项确认）

```text
□ 数据库层：专用只读角色（GRANT SELECT ONLY），绝不 root/业务主账号
□ 能上读副本就上读副本（最强物理边界）
□ 只连接"Agent 该看的数据"——连接范围本身就是权限
□ PII 列在授权层隐藏（列级 GRANT 前先 REVOKE 表级）
□ 应用层：SELECT 白名单 + 强制 LIMIT + 超时 + 单条语句
□ 所有输入参数化绑定
□ 写操作（如确有）：独立更高权限 + HITL 审批（见 07 模块）
□ 审计日志开启，敏感表访问有告警
□ 优先使用本地/脱敏/预发数据，而非生产真实数据
```

> 🎯 核心要点：**安全纵深不是"多几道提示词"**——是 授权层（角色/读副本）打底、守卫层（文本校验）减负、交互层（写审批）把关、审计层（日志告警）兜底，四层缺一不可。

## 9. 三个常见误解

| 误解 | 真相 |
|---|---|
| "提示词写了只读，模型就不会写" | 提示词可被注入覆盖，且只约束"生成"，不约束"权限" |
| "会话级 read_only 就是只读安全" | 任何会话用户可自行 SET 回 off，仅防手滑 |
| "字符串过滤 DML 关键词就够了" | `SELECT evil_func()` 可绕过所有关键词过滤，必须 AST + 数据库层 |
| "MCP 服务器开了只读模式就安全" | 只读模式是应用层守卫，仍可绕过；数据库账号只读才是边界 |

四个误解的共同根源：**把应用层的"软件设置"当成了数据库层的"物理权限"**——安全边界的唯一判定标准是"绕过它需要多少权限"。

---

**下一模块**：[04-工具封装实战：连接查询与返回](04-工具封装实战：连接查询与返回.md)　**返回总览**：[00-数据库交互总览](00-数据库交互总览.md)

## 参考来源

- [Read-only enforcement and least-privilege roles（Laoujin/Atlas 2026）](https://github.com/Laoujin/Atlas/blob/main/research/2026-06-09-audit-and-harden-text-to-sql-pipelines-2026/read-only-enforcement-and-least-privilege-roles/index.md)
- [mcp-multi-db SECURITY.md（GitHub）](https://github.com/mahAnuj/mcp-multi-db/blob/main/SECURITY.md)
- [safedb-mcp：Secure read-only DB access with guardrails（GitHub）](https://github.com/narekmalk/safedb-mcp)
- [Model Context Protocol for AI-Assisted Database Query Generation（IEEE）](https://ieeexplore.ieee.org/abstract/document/11526454/keywords)
- [OWASP LLM Top 10 for LLM Applications（OWASP）](https://owasp.org/www-project-top-10-for-large-language-model-applications/)
