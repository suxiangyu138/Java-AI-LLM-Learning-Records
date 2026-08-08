# Docker Compose 深化总览
> 从"会用 Compose 编排多服务"到"吃透 Compose Spec"：文件规范、网络卷、依赖管理、多环境、Watch 热重载、生产与故障排查

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [定位：与上级 Docker 体系的分工](#2-定位与上级-docker-体系的分工)
3. [模块导航](#3-模块导航)
4. [学习路线推荐](#4-学习路线推荐)
5. [版本窗口说明](#5-版本窗口说明)
6. [核心概念速查](#6-核心概念速查)
7. [参考来源](#7-参考来源)

## 1. 知识体系导图

```text
Docker Compose 深化体系（7 篇，Compose v5 / v2.40+ 基准）
│
├─ 规范层 ─────────────────────────────
│   ├─ 01 Compose Spec 文件规范全解（顶层键/depends_on 三条件/profiles/extends/include）
│   ├─ 02 网络与卷高级配置（networks/volumes 字段全解/共享模式）
│   └─ 03 服务生命周期与依赖管理（up/down 语义/healthcheck/restart/资源限制）
│
├─ 工程层 ─────────────────────────────
│   ├─ 04 多环境与配置管理（.env/多文件合并/profiles/configs/secrets/日志）
│   └─ 05 开发工作流与 Compose Watch（sync/rebuild/restart 热重载）
│
└─ 生产层 ─────────────────────────────
│   └─ 06 生产实践与故障排查（生产清单/Compose Bridge 转 K8s/排查/面试题）
│
└─ 00 总览（本文）
```

> 🎯 定位一句话：上级 `07-Docker-Compose多服务编排.md` 讲"怎么用"，本体系讲"为什么这么设计 + 怎么用到极致"。

## 2. 定位：与上级 Docker 体系的分工

| 层 | 文档 | 回答的问题 |
|----|------|-----------|
| 入门层 | `../07-Docker-Compose多服务编排.md` | Compose 是什么、怎么编排 Spring Boot+MySQL+Redis、核心命令 |
| **深化层（本体系）** | `Docker Compose/` 7 篇 | Compose Spec 每个字段的精确语义、多环境/多文件工程化、Watch 热重载、生产与排障 |
| 衔接层 | `../10-Docker与Kubernetes衔接.md` | Compose 与 K8s 的映射（Compose Bridge 转换） |

## 3. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-Docker%20Compose深化总览.md) | 导图、定位、版本窗口 | 所有人 |
| 01 | [Compose Spec 文件规范全解](01-Compose-Spec文件规范全解.md) | 顶层键、depends_on 三条件、profiles/extends/include、多文件合并 | 进阶必读 |
| 02 | [网络与卷高级配置](02-网络与卷高级配置.md) | networks/volumes 字段全解、external、共享模式 | 进阶 |
| 03 | [服务生命周期与依赖管理](03-服务生命周期与依赖管理.md) | up/down 语义、容器重建规则、healthcheck、restart、资源限制 | 进阶 |
| 04 | [多环境与配置管理](04-多环境与配置管理.md) | .env 插值、-f 合并规则、profiles、configs/secrets、日志 | 重点 |
| 05 | [开发工作流与 Compose Watch](05-开发工作流与Compose-Watch.md) | watch 三 action、initial_sync、dev 分离、调试 | 实战 |
| 06 | [生产实践与故障排查](06-生产实践与故障排查.md) | 生产清单、Compose Bridge 转 K8s、排查、面试题 | 收尾 |

## 4. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 深化入门（1 天） | 已会用 Compose 编排 | 01 → 03 → 04 |
| 工程实战（2 天） | 要落地多环境/热重载 | 01 → 04 → 05 → 02 |
| 生产上线（3 天） | 负责容器化部署 | 全部 + 06（生产清单与排障） |

## 5. 版本窗口说明

| 项 | 现状（2026-08） | 说明 |
|----|----------------|------|
| Compose 版本 | **v5**（2026 升级，v2 → v5） | Go 重写，集成于 Docker CLI：`docker compose`（无连字符） |
| `version:` 字段 | **已废弃，可省略** | 旧教程的 `version: '3.8'` 会被忽略 |
| Watch 热重载 | v2.22.0 GA，v2.32.0 起三 action | `develop.watch` 块（原 `x-develop`） |
| 文件命名 | `compose.yaml` 优先 | `compose.yml`、`docker-compose.yml` 兼容 |
| Compose Bridge | 可用 | `docker compose convert --format kubernetes` 转 K8s 清单 |
| 安全 | CVE-2025-62725 已修复（v2.40.2+） | `include` + OCI 制品场景需升级 |

> ⚠️ **时效性**：本文按 2026-08 官方 Compose Spec 与发布信息编写。字段细节以 [Compose Spec 官方规范](https://compose-spec.io/) 与 `docker compose config` 实际输出为准。

## 6. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Compose Spec | Compose 文件的跨实现规范（compose-spec.io），Docker/其他工具共同遵循 |
| `services` | 唯一必填顶层键，定义所有容器 |
| `depends_on` | 依赖控制；三条件：`service_started`（默认）/`service_healthy`/`service_completed_successfully` |
| `profiles` | 按需启用的可选服务（监控/调试/测试库） |
| `extends` | 服务配置复用（注意：与 depends_on 不兼容） |
| `include` | 引入外部 Compose 文件 |
| `configs` | 只读配置文件挂载（文件级） |
| `secrets` | 敏感数据挂载（只读，默认 `/run/secrets/<name>`） |
| `develop.watch` | 热重载配置：`sync`（同步）/`rebuild`（重建）/`sync+restart`（同步重启） |
| `.env` | 变量插值文件（`${VAR}` 引用） |
| 多文件合并 | `-f` 叠加 + 后文件覆盖前文件的合并规则 |
| Compose Bridge | 本地 Compose → Kubernetes 清单一键转换 |

## 7. 参考来源

- [Compose Specification 官方规范](https://compose-spec.io/)
- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [Compose Watch 官方文档](https://docs.docker.com/compose/file-watch/)
- [Docker Compose 变更记录（v2 → v5）](https://bdteo.com/docker-compose-major-changes-since-october-2023/)

---

**下一模块**：[01-Compose-Spec文件规范全解](01-Compose-Spec文件规范全解.md)
