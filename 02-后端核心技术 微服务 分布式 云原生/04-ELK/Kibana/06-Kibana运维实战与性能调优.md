# Kibana 运维实战与性能调优
> Kibana 慢、Kibana 挂、Kibana 升级失败——运维三座大山。本文件覆盖资源规划、性能调优、常见故障排查、升级迁移与生产架构

## 目录
1. [资源规划与架构部署](#1-资源规划与架构部署)
2. [Kibana 性能调优](#2-kibana-性能调优)
3. [常见故障排查](#3-常见故障排查)
4. [升级与迁移](#4-升级与迁移)
5. [Kibana 高可用架构](#5-kibana-高可用架构)
6. [运维自动化](#6-运维自动化)

---

## 1. 资源规划与架构部署

### 1.1 资源需求估算

| 集群规模 | 内存（Kibana 进程） | CPU | 磁盘（自身日志） |
|------|:---:|:---:|:---:|
| 开发/测试（<100GB 数据） | 2GB | 2 核 | 10GB |
| 生产小集群（<1TB） | 4GB | 4 核 | 50GB |
| 生产大集群（1TB+） | 8GB+ | 8 核 | 100GB+ |

### 1.2 部署形态

| 形态 | 适用 | 说明 |
|------|------|------|
| 单机 Docker | 开发测试 | ES + Kibana 同机 |
| 独立节点 | 生产小集群 | Kibana 单独一台（2-4GB） |
| 多实例 + 反代 | 生产大集群 | Nginx 负载均衡，Kibana 无状态可水平扩展 |

> 💡 Kibana 是无状态应用（状态都在 ES 的 `.kibana_*` 索引），**水平扩展只需加实例 + 负载均衡**——这是它和 Logstash（有状态队列）的重要区别。

---

## 2. Kibana 性能调优

### 2.1 调优维度

| 维度 | 手段 |
|------|------|
| Node.js 堆 | `NODE_OPTIONS=--max-old-space-size=4096` |
| 并发上限 | `server.maxPayloadBytes`（大请求体限制） |
| 缓存 | 服务端响应缓存、浏览器缓存头 |
| 查询侧 | 面板瘦身、ES|QL 预聚合（见 03 篇 §8） |
| 资源隔离 | Kibana 与 ES 分机部署，避免争抢 |

### 2.2 慢 Kibana 的排查顺序

```text
页面慢？
 ├─ ES 查询慢 → Dev Tools 复测该查询（看耗时与命中量）
 ├─ 面板太多 → 减少面板、加时间粒度
 ├─ 资源不足 → top 看 CPU/内存，扩容
 └─ 网络问题 → 浏览器与 Kibana 之间延迟（反代位置）
```

### 2.3 常见性能指标

| 指标 | 正常范围 | 异常处理 |
|------|------|------|
| Kibana CPU | < 60% | 减少面板请求频率 |
| Node 堆占用 | < 70% | 调大 max-old-space-size |
| 页面加载 | < 3s | 检查 ES 查询耗时 |
| ES 查询 QPS | 按集群规格 | 加索引过滤/缩小时间窗 |

---

## 3. 常见故障排查

### 3.1 故障速查表

| 症状 | 可能原因 | 排查动作 |
|------|------|------|
| 页面报「Kibana server is not ready yet」 | ES 未就绪 / 版本不匹配 / ES 地址错 | `docker logs kibana` 看日志 |
| 登录后空白 / 502 | 内存不足 OOM | 查看进程、调大堆内存 |
| Discover 无数据 | 数据视图时间字段错 / 索引权限不足 | 检查 Data View + 角色权限 |
| 保存看板失败 | 空间权限不足 | 检查 kibana 功能权限 |
| 告警不触发 | 规则执行失败 / ES 侧查询超时 | Stack Management → 规则执行历史 |
| 升级后功能缺失 | 大版本跳级 / 未迁移 | 逐版本升级，检查兼容矩阵 |
| 页面极慢 | ES 慢查询 / 面板过多 | 按 2.2 排查顺序 |

### 3.2 关键日志位置

| 场景 | 日志 |
|------|------|
| Kibana 自身日志 | `logs/kibana.log`（Docker: stdout） |
| 请求审计 | `logs/kibana-access.log`（启用后） |
| ES 慢查询 | ES 侧 `slowlog`（需开启） |
| 规则执行 | `.kibana_alerting_*` 索引或 API 查询 |

### 3.3 调试常用 API

```bash
# 健康状态
curl http://localhost:5601/api/status

# 服务端信息（版本、内存）
curl http://localhost:5601/api/status | jq .status

# 空间/对象导出
curl -X POST http://localhost:5601/api/saved_objects/_export \
  -H "kbn-xsrf: true" -d '{"type": "dashboard"}'
```

---

## 4. 升级与迁移

### 4.1 升级原则

```text
① 版本对齐：ES 与 Kibana 同步升级，先 ES 后 Kibana
② 逐版本跳：8.17 → 9.4（跨大版本先到 9.0/9.1 再往上升）
③ 备份先行：ES 快照（含 .kibana_* 索引）
④ 灰度验证：先在测试环境升级，验证看板/规则/权限
```

### 4.2 升级检查清单

| # | 检查项 |
|:---:|------|
| 1 | 备份：`_snapshot` 全量快照（含系统索引） |
| 2 | 兼容：阅读 release notes 的 breaking changes |
| 3 | 验证：测试环境升级后看板可打开、查询可执行 |
| 4 | 回滚：保留旧版本目录/镜像，出问题可回退 |
| 5 | 规则：告警规则格式在新版本是否兼容（Saved Object 迁移自动） |

### 4.3 迁移场景

| 场景 | 方案 |
|------|------|
| 换环境 | Saved Objects 导出导入 + 数据视图重建 |
| 跨集群迁移 | 数据走 ES 跨集群复制（CCR）或 reindex；对象走导出导入 |
| 跨大版本 | 逐版本迁移，避免对象格式不兼容 |

---

## 5. Kibana 高可用架构

### 5.1 生产高可用拓扑

```text
                  ┌──→ Kibana A (5601)
用户 → Nginx(HTTPS) ──→ Kibana B (5601)  ──→ ES 集群（3 节点）
   （负载均衡+健康检查） ──→ Kibana C (5601)        ↑
                                            多节点 + 副本
```

| 组件 | 高可用手段 |
|------|------|
| Kibana | 多实例无状态 + Nginx upstream + health check |
| ES | 多节点 + 分片副本（至少 3 节点） |
| 证书 | Nginx 终结 TLS，内部 HTTP |

### 5.2 Nginx 反代配置示例

```nginx
upstream kibana_backend {
    server kibana-a:5601;
    server kibana-b:5601;
    server kibana-c:5601;
}

server {
    listen 443 ssl;
    server_name kibana.example.com;
    ssl_certificate     /etc/nginx/certs/kibana.crt;
    ssl_certificate_key /etc/nginx/certs/kibana.key;

    location / {
        proxy_pass http://kibana_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 120s;          # 慢查询保护
        client_max_body_size 10m;
    }
}
```

> ⚠️ Nginx 反代三坑：**① X-Forwarded-For 不配 → Kibana 审计记录不到真实 IP；② proxy_read_timeout 过短 → 长查询面板报 504；③ WebSocket 场景需额外配置（Kibana 9.x 部分实时功能）**。

---

## 6. 运维自动化

### 6.1 常用自动化手段

| 手段 | 用途 |
|------|------|
| Docker Compose | 开发/测试环境一键起 |
| systemd | 生产单实例守护 |
| 配置文件管理（Ansible 等） | 多环境配置一致 |
| 健康检查脚本 | `curl /api/status` 探活 |
| 快照定时任务 | ES 快照（含 `.kibana_*`）定期备份 |
| 监控对接 | Stack Monitoring + 外部告警 |

### 6.2 健康检查脚本示例

```bash
#!/bin/bash
# kibana_healthcheck.sh
status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:5601/api/status)
if [ "$status" != "200" ]; then
    echo "Kibana health check failed: HTTP $status"
    exit 1
fi
echo "Kibana OK"
```

> 🎯 **核心要点**：Kibana 运维四件事——**① 资源**（4GB 起、Node 堆调优）、**② 高可用**（无状态多实例 + Nginx 反代）、**③ 升级**（同版本对齐、逐版本跳、快照备份）、**④ 排错**（看日志三处：Kibana 自身 / ES / 规则执行历史）。记住一个本质：**Kibana 慢 90% 是 ES 慢**——调优先查查询，别先动 Kibana。

---

**返回总览**：[00-Kibana专题总览](00-Kibana专题总览.md) | **上一篇**：[05-Kibana安全与索引管理](05-Kibana安全与索引管理.md)
