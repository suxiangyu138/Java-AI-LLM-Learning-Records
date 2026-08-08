# Jenkins 总览
> 最老牌的开源 CI/CD 服务器：核心概念、Pipeline 语法、分布式构建、共享库与生产运维——"存量企业标配、新项目备选"的 2026 现实

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [Jenkins 定位（2026）](#4-jenkins-定位2026)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Jenkins 体系（8 篇，LTS 2.555.x / Java 21-25 基准，2026-08）
│
├─ 认知层 ─────────────────────────────
│   ├─ 00 总览（本文）
│   ├─ 01 安装与初始化（war/docker/插件/系统配置）
│   └─ 02 核心概念（Job/Agent/Executor/Workspace/触发器）
│
├─ 机制层 ─────────────────────────────
│   ├─ 03 Pipeline 语法（声明式/脚本式/stage/参数化）
│   ├─ 04 凭证与安全（Credentials/权限矩阵/加固）
│   └─ 05 构建工具集成（JDK/Maven/SonarQube/制品）
│
├─ 工程层 ─────────────────────────────
│   ├─ 06 Agent 与分布式构建（标签/Docker/K8s 动态）
│   └─ 07 共享库与工程化（Shared Library/Jenkinsfile 规范）
│
└─ 实战层 ─────────────────────────────
│   └─ 08 生产运维与故障排查（备份/升级/故障/避坑/面试）
```

> 🎯 定位一句话：Jenkins 是 **"开源 CI/CD 的事实老牌"**——1800+ 插件、Groovy 全编程能力、任意 SCM 兼容；2026 年仍占组织市场约 28%（JetBrains 2025 调查），是存量企业、内网隔离、复杂流水线场景的标配。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-Jenkins总览.md) | 导图、2026 定位、速查 | 所有人 |
| 01 | [安装与初始化](01-安装与初始化.md) | war/docker/初始化向导/插件/系统配置 | 入门必读 |
| 02 | [核心概念](02-核心概念.md) | Job 类型/Agent/Executor/Workspace/触发器 | 入门必读 |
| 03 | [Pipeline 语法](03-Pipeline语法.md) | 声明式/脚本式/stage/环境变量/参数化 | 重点 |
| 04 | [凭证与安全](04-凭证与安全.md) | Credentials 五类/权限矩阵/Agent 安全/加固 | 重点 |
| 05 | [构建工具集成](05-构建工具集成.md) | JDK/Maven/Node/SonarQube 门禁/制品推送 | 重点 |
| 06 | [Agent 与分布式构建](06-Agent与分布式构建.md) | Master/Agent/标签/Docker/K8s 动态/高可用 | 进阶 |
| 07 | [共享库与工程化](07-共享库与工程化.md) | Shared Library/Jenkinsfile 规范/多分支/Webhook | 进阶 |
| 08 | [生产运维与故障排查](08-生产运维与故障排查.md) | 备份/升级（Java 21 注意）/故障/避坑/面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速上手（1 天） | 后端开发者 | 00 → 01 → 02 → 03 → 05 |
| 完整学习（2 天） | 运维/DevOps | 00 → 01 → 03 → 04 → 06 → 08 |
| 面试冲刺（半天） | 备战后端面试 | 00 → 02 → 03 → 06 → 08 |

> 💡 与兄弟体系分工：**CI/CD 概念、Jenkins vs GitLab CI 选型** 在 [CI/CD 灰度发布体系](../CI%20CD灰度发布/00-CI-CD与灰度发布总览.md)（02 篇有对比）；**本体系专攻 Jenkins 深潜**（语法/凭证/分布式/运维）。发布策略与灰度见该体系 05/06 篇。

## 4. Jenkins 定位（2026）

### 4.1 市场现实

| 数据 | 说明 |
|------|------|
| 组织采用率 | **28%**（JetBrains 2025 调查：GitHub Actions 33% 领跑、GitLab CI 19%） |
| 当前 LTS | **2.555.x**（2.555.3，2026-06-08） |
| Java 要求 | **Java 21 或 25**（2.555 起不再支持 Java 17） |
| 插件数 | 1800+（生态覆盖一切工具） |
| 谁在用 | 存量企业/内网隔离/多 SCM/复杂流水线 |

### 4.2 2026 选型结论

```text
用 Jenkins 的理由（2026）：
  ✅ 已有大量 Jenkinsfile/插件/团队经验（迁移 200+ 流水线不便宜）
  ✅ 内网隔离/数据主权要求（唯一可行的自托管选项之一）
  ✅ 多 SCM 并存（GitHub+GitLab+SVN 同时管）
  ✅ 复杂流水线逻辑（Groovy 图灵完备，YAML 表达不了）
  ✅ 严格访问控制 + 水平扩展（配好之后很能打）

不选 Jenkins 的理由：
  ❌ 新项目（代码在 GitHub → GitHub Actions 零摩擦）
  ❌ 想要低维护（插件/安全补丁/扩容全自己扛）
  ❌ 云原生/AI Agent 工作流（容器化与快速反馈要返工适配）
```

> 🎯 一句话结论：**"新项目默认 GitHub Actions/GitLab CI；Jenkins 的甜区是存量企业与内网隔离"**——但面试与存量工作里 Jenkins 仍是高频考点，本体系把它的完整能力讲透。

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Job（任务） | 一次构建流程的配置单元 |
| Freestyle | 传统"步骤式"任务（UI 配置） |
| Pipeline | 代码化流水线（Jenkinsfile，推荐） |
| Master（控制器） | 调度中枢（UI/调度/队列） |
| Agent（节点） | 实际执行构建的机器 |
| Executor（执行器） | Agent 上的并发执行槽位 |
| Workspace | 构建工作目录（代码检出处） |
| 触发器 | 定时/SCM 变更/Webhook 触发构建 |
| Credentials | 凭证库（密码/密钥/令牌统一管理） |
| Shared Library | 共享流水线代码库（复用） |
| 多分支流水线 | 每分支自动建 Pipeline |
| 制品（Artifact） | 构建产物（jar/镜像/报告） |
| Jenkinsfile | 代码化的流水线定义（声明式） |

## 6. 参考来源

- [Jenkins 官方文档](https://www.jenkins.io/doc/)
- [Jenkins LTS 版本与生命周期](https://endoflife.date/jenkins)
- [Jenkins 2.555 LTS 发布信息](https://releaserun.com/versions/jenkins/2.555/)
- [2026 CI 工具对比（JetBrains 数据引用）](https://blog.jetbrains.com/teamcity/2026/03/best-ci-tools/)
- [Jenkins 替代方案评估（2026）](https://semaphore.io/blog/best-jenkins-alternatives-in-2026)
- [Jenkins vs GitHub Actions vs GitLab CI（2026 对比）](https://technologymatch.com/blog/jenkins-vs-gitlab-ci-vs-circleci-vs-github-actions-the-ci-cd-decision-guide-in-2026)

---

**下一模块**：[01-安装与初始化](01-安装与初始化.md)
