# Kibana 安全与索引管理
> Spaces 多空间隔离、角色权限矩阵、索引生命周期管理（ILM）、Dev Tools 调试——Kibana 管理面完整指南，覆盖安全模型与索引运维

## 目录
1. [Kibana 安全模型](#1-kibana-安全模型)
2. [用户与角色权限](#2-用户与角色权限)
3. [Spaces：空间隔离](#3-spaces空间隔离)
4. [Index Management：索引管理](#4-index-management索引管理)
5. [索引生命周期管理（ILM）](#5-索引生命周期管理ilm)
6. [Dev Tools：调试 ES 的利器](#6-dev-tools调试-es-的利器)
7. [Saved Objects 管理](#7-saved-objects-管理)
8. [安全最佳实践](#8-安全最佳实践)

---

## 1. Kibana 安全模型

### 1.1 两层权限模型

```text
第一层：ES 权限（索引级）
  → 能查哪些索引、写哪些索引
  → 由 ES 角色控制（cluster / indices / run_as）

第二层：Kibana 权限（功能级）
  → 能看到哪些功能模块（Discover / Dashboard / Management）
  → 由 Kibana Space + 权限角色控制
```

| 权限层面 | 控制什么 | 配置位置 |
|------|------|------|
| ES 索引权限 | 数据可见性 | ES Role（Stack Management → Roles） |
| Kibana 功能权限 | 界面功能可见性 | Kibana Space + 角色内 Kibana 权限 |
| 空间（Spaces） | 资源隔离（看板/搜索/规则） | Stack Management → Spaces |

### 1.2 权限判定流程

```text
用户登录 → 认证（ES Security）→ 加载角色
→ 校验 Kibana 功能权限（空间内）
→ 数据请求时校验 ES 索引权限（服务端）
→ 双重通过才放行
```

> ⚠️ 常见误解：Kibana 的「只读」不等于 ES 只读——Discover 能看数据是因为 ES 角色给了 `read` 索引权限；要给用户「只看 Discover」需同时配两层的只读。

---

## 2. 用户与角色权限

### 2.1 内置角色速查

| 角色 | 权限 | 典型用途 |
|------|------|------|
| `superuser` | 全部权限 | 管理员（elastic 默认拥有） |
| `kibana_admin` | Kibana 全部管理功能 | 平台管理员 |
| `kibana_editor` | 创建/编辑保存对象 | 分析师 |
| `kibana_viewer` | 只读查看 | 只读用户 |
| `viewer`（ES） | 所有索引只读 | 数据只读 |
| `logstash_system` | Logstash 监控上报 | 服务账号 |

### 2.2 自定义角色的推荐配置

```yaml
# 场景：给「订单团队」分配 order-logs-* 的只读 + 看板只读
角色：order_team_viewer
  indices:
    - names: ["order-logs-*"]
      privileges: ["read", "view_index_metadata"]
  kibana:
    - spaces: ["order-team"]           # 空间限定
      privileges:
        - feature:
            discover: ["read"]
            dashboard: ["read"]
        - base: ["read"]               # 只读兜底
```

### 2.3 权限矩阵（推荐分级）

| 用户类型 | ES 权限 | Kibana 权限 | 空间 |
|------|------|------|------|
| 平台管理员 | superuser | 全部 | 全部 |
| 团队管理员 | 本团队索引全权 | editor | 本团队 |
| 分析师 | 本团队索引只读 | viewer | 本团队 |
| 只读领导 | 只读 | viewer | 只读空间 |
| 服务账号 | 最小权限（写特定索引） | 无 | — |

---

## 3. Spaces：空间隔离

### 3.1 Spaces 能隔离什么

| 资源 | 隔离方式 |
|------|------|
| 数据视图 | 每个空间独立 |
| 仪表盘/搜索/可视化 | 空间内共享，跨空间不互见 |
| 告警规则 | 空间内创建与查看 |
| 用户可见性 | 角色指定可访问的空间列表 |

### 3.2 创建与使用

```text
Stack Management → Spaces → Create space
  · 名称：order-team / platform / dev
  · 功能启用：可按需关闭（如 dev 空间不开告警）
用户侧：角色里分配 spaces 列表 → 登录后空间切换器选择
```

> 💡 多租户实践：**一个 ES 集群 + 多个 Space** = 团队资源隔离的标准姿势；数据隔离靠索引前缀（`order-logs-*`）+ ES 角色。

---

## 4. Index Management：索引管理

### 4.1 索引管理界面

| 功能 | 说明 |
|------|------|
| 索引列表 | 查看索引状态、大小、文档数、分片 |
| 索引详情 | 映射、设置、别名、状态（open/close） |
| 操作 | 关闭/打开、刷新、清理缓存、删除 |
| Data Streams | 数据流管理（日志场景推荐） |
| 模板管理 | Index Template 查看与维护 |

### 4.2 索引状态速查

| 状态 | 含义 | 处理 |
|------|------|------|
| Green | 主分片 + 副本正常 | 健康 |
| Yellow | 副本未分配 | 单节点正常，集群加节点恢复 |
| Red | 主分片缺失 | 紧急：检查节点/磁盘，重建或恢复 |
| Close | 关闭（不可读写） | 释放资源，需 reopen 才能用 |

---

## 5. 索引生命周期管理（ILM）

### 5.1 ILM 阶段

```text
Hot（热） → Warm（温） → Cold（冷） → Frozen（冻结） → Delete（删除）
写入频繁      只读优化     可搜索但慢    查询更慢（9.x 可选）   定期清理
```

| 阶段 | 典型配置 |
|------|------|
| Hot | rollover：按大小 50GB 或时间 1 天滚动 |
| Warm | shrink（合并分片）、forcemerge（段合并） |
| Cold | searchable snapshot（可搜索快照，降成本） |
| Delete | 保留期到期（如 30 天）删除 |

### 5.2 日志索引 ILM 策略示例

```yaml
# 日志保留 30 天：热 1 天 → 冷 30 天 → 删除
policy: logs-policy
  phases:
    hot:
      actions:
        rollover:
          max_size: 50gb
          max_age: 1d
    cold:
      min_age: 7d
      actions:
        searchable_snapshot:
          snapshot_repository: my-backup
    delete:
      min_age: 30d
      actions:
        delete: {}
```

### 5.3 索引模板 + ILM 组合（日志接入标准姿势）

```text
Index Template（logs-*）→ 指定 ILM policy + 分片副本数 + 别名
Logstash/Filebeat 写入 logs-xxx-2026.08.07
→ rollover 自动滚动新索引 → ILM 按策略降温清理
```

> 🎯 面试要点：**ILM = 日志数据的「冰箱」**——热数据快、冷数据省、到期自动删，是控制 ES 磁盘成本的核心机制。

---

## 6. Dev Tools：调试 ES 的利器

### 6.1 Console 功能

| 能力 | 示例 |
|------|------|
| 执行任意 REST API | `GET /logs-*/_search` |
| 自动补全 | 索引名、字段名、DSL 自动提示 |
| 历史记录 | 常用查询复用 |
| 多请求 | 一次粘贴多条（Ctrl+Enter 执行） |

### 6.2 常用调试命令

```json
// 查看索引健康
GET _cluster/health

// 查看索引信息
GET /logs-2026.08.07

// 测试查询（带 explain 看匹配原因）
GET /logs-*/_search
{
  "explain": true,
  "query": { "match": { "message": "支付失败" } }
}

// 查看字段映射
GET /logs-*/_mapping/field/message

// 查看 ILM 策略
GET /_ilm/policy/logs-policy

// 删除索引（谨慎！）
DELETE /logs-2026.08.01
```

> ⚠️ 生产禁忌：Dev Tools 对具备写入权限的用户是「裸奔的枪」——`DELETE`、`_forcemerge`、`_reindex` 都直接生效，权限给到 team 时需培训规范。

---

## 7. Saved Objects 管理

### 7.1 Saved Objects 类型

| 对象 | 说明 |
|------|------|
| Data View | 数据视图 |
| Dashboard | 看板 |
| Visualization | 可视化（Lens/老式） |
| Saved Search | 保存的搜索 |
| Rules | 告警规则 |

### 7.2 导入导出

| 方式 | 说明 |
|------|------|
| UI 导入导出 | Stack Management → Saved Objects → 导出 JSON |
| 跨集群迁移 | 导出 → 新环境导入（注意数据视图索引模式要匹配） |
| 版本兼容 | 跨大版本建议逐版本迁移（对象格式有差异） |
| API | `POST /api/saved_objects/_export` 等 |

---

## 8. 安全最佳实践

| # | 实践 | 说明 |
|:---:|------|------|
| 1 | 最小权限 | 服务账号/团队账号只给所需索引与功能 |
| 2 | 空间隔离 | 团队资源进独立 Space |
| 3 | HTTPS 全覆盖 | 浏览器 → Kibana、Kibana → ES 全部加密 |
| 4 | 密钥管理 | connector 密钥、加密密钥集中管理 |
| 5 | 审计日志 | 启用安全审计（9.x 内置 audit） |
| 6 | 定期轮换 | 服务账号密码定期更换 |
| 7 | 外网隔离 | Kibana 不直接暴露公网，走 VPN/网关 |

> 🎯 **核心要点**：Kibana 安全 = **ES 索引权限（数据层）+ Kibana 功能权限（功能层）+ Spaces（资源层）** 三层叠加；索引管理 = **模板 + ILM + Data Stream** 三件套管住日志生命周期；Dev Tools 是调试利器也是权限放大器。生产落地的三个「必须」：ILM 必须配（否则磁盘爆炸）、权限必须最小化、HTTPS 必须开。

---

**下一模块**：[06-Kibana运维实战与性能调优](06-Kibana运维实战与性能调优.md) | **返回总览**：[00-Kibana专题总览](00-Kibana专题总览.md)
