# 00 - GitLab 总览

> 定位：一体化 DevOps 平台的事实标准——"代码托管 + CI/CD + 安全 + 制品库 + AI 助手一个平台全包——2026 年 19.2 月度迭代，MR-First 工作流与智能体自动化是主线；GitLab 是'从写代码到上线'的唯一平台，不是又一个 GitHub"

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)
9. [参考来源](#9-参考来源)

---

## 1. 知识体系导图

```text
GitLab（本体系 11 篇——05-开发流程工具）
├── 平台层：01 GitLab 是什么（DevOps 平台/2026 基线/版本节奏）
│          02 安装与项目基础（部署/权限模型/MR 基础）
├── CI/CD 层：03 CI/CD 核心（pipeline/job/stage/artifacts）
│          04 高级 CI/CD（DAG/矩阵/动态 pipeline/Components）
│          05 Runner 体系（类型/tags/executor/K8s/GitOps）
├── 协作层：06 分支策略与 MR 工作流（GitLab Flow/approval）
├── 安全层：07 安全与 DevSecOps（SAST/DAST/Secrets Manager）
├── 资产层：08 制品库与仓库管理（Registry/Package/Catalog）
├── 运维层：09 运维与高可用（升级/备份/Geo）
└── 验收层：10 生产实战与自测（Flow+CI+安全三合一 + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | 是什么 | DevOps 平台/2026 基线/版本节奏 | 认知 |
| 02 | 安装与基础 | 部署/权限模型/MR 基础流程 | 会建仓 |
| 03 | CI/CD 核心 | pipeline/job/stage/artifacts | 会写流水线 |
| 04 | 高级 CI/CD | DAG/矩阵/动态/Components | 会编排 |
| 05 | Runner 体系 | 类型/tags/executor/K8s/GitOps | 会调度 |
| 06 | 分支与 MR | GitLab Flow/approval/合并策略 | 会协作 |
| 07 | 安全 | SAST/DAST/Secrets Manager | 会加固 |
| 08 | 制品库 | Registry/Package/Catalog | 会管理 |
| 09 | 运维高可用 | 升级/备份/Geo/监控 | 会运维 |
| 10 | 实战与自测 | 三合一项目 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 Git 体系（`../Git/00-Git知识体系总览.md`）的分工**：Git 体系讲"Git 本身"（对象模型/分支/远程/工作流），**本体系讲"GitLab 平台"**——在 Git 之上的托管、CI/CD、MR、安全——"**Git 是引擎，GitLab 是整车**"（06 篇分支策略直接引用 Git 体系的 GitFlow/GitHub Flow 概念）。

**与 GitHub（`../GitHub/01-GitHub平台完全指南.md`）、Gitee（`../Gitee/00-Gitee知识体系总览.md`）的分工**：三平台对比在 01 篇；**GitHub 强在开源生态与 Actions，GitLab 强在一体化 DevOps 与私有化部署**——"**开源项目 GitHub，企业内网 GitLab，国内合规 Gitee**"（**CI 语法不互通——换平台 = 重写流水线**）。

**与 CI/CD 灰度发布（`../../07-工程运维基础/运维/CI CD灰度发布/00-CI-CD与灰度发布总览.md`）、Jenkins（`../../07-工程运维基础/运维/Jenkins/00-Jenkins总览.md`）的分工**：CI/CD 体系讲"流水线设计与发布策略方法论"，**本体系讲"GitLab CI/CD 这一个具体实现"**（该体系 02 篇就是"Jenkins 与 GitLab-CI 实践"）——"**方法论看 CI/CD 体系，GitLab 实现看本体系**"。

**与 Docker（`../../11-云原生/Docker/`）的分工**：Runner 的 Docker executor 与 Container Registry 都依赖 Docker 生态——"**GitLab 管编排，Docker 管运行环境**"（05/08 篇）。

**2026-08 基线**：**GitLab 19.2（2026-07-16 发布，月度节奏每月 22 号）**——主线是"**受治理的智能体自动化**"（governed agentic automation）：依赖扫描自动修复（beta，自动开 MR 修漏洞包）、Security Review Flow（beta，逻辑缺陷扫描）、GitLab Duo CLI（GA）、Custom Flows（GA，事件触发智能体工作流）、AI 审计事件报告；**19.0（2026-05-21）**：**Secrets Manager**（beta，平台内凭证管理 + Vault/AWS/Azure/GCP 集成）、Developer Flow（一键 rebase-and-merge）、CI/CD Catalog 组件分析；**18.11（2026-04）**：**CI Expert Agent**（beta，自动生成 .gitlab-ci.yml）、**Data Analyst Agent**（GA，自然语言查流水线指标）——"**2026 的 GitLab：平台 + AI 双主线，CI/CD 从'手写 YAML'走向'智能体生成'，但 YAML 语法仍是地基**"。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇动手——**毕业标准：独立完成"GitLab Flow 分支 + CI/CD 全链路 + 安全门禁"三合一项目**。**路线一的配套**：每篇的练习 5 题动手写代码——**学 GitLab 必须真跑一遍流水线**（Docker 起一个实例或注册 GitLab.com）。

**路线二：CI/CD 冲刺路线（2-3 天）**——01 → 03 → 04 → 05 → 10——**把流水线吃透**——"**GitLab 的核心价值是 CI/CD，03/04/05 三篇是必读**"。

**路线三：团队落地路线**——01 → 02 → 06 → 07 精读——团队管理员视角：权限、MR 规范、安全配置——"**先定协作规范，再谈流水线**"（管理员落地前先读 09 篇备份纪律——**平台上线第一天就配备份**）。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| DevOps 平台 | 代码/CI/CD/安全/制品一个平台 | 01 |
| Pipeline | 一次 CI/CD 执行（stages + jobs 组成） | 03 |
| Job | 流水线的最小执行单元 | 03 |
| Stage | 阶段（build/test/deploy，同阶段并行） | 03 |
| Artifact | 构建产物（跨 job 传递/下载） | 03 |
| Rules | 条件控制（取代 only/except） | 03/04 |
| Needs | DAG 依赖（不等同阶段全完成） | 04 |
| CI/CD Components | 版本化可复用流水线组件 | 04 |
| Runner | 执行 job 的代理（shared/group/specific） | 05 |
| MR | Merge Request（代码评审 + 合并关卡） | 06 |
| Merge Checks | 合并前关卡（流水线绿/批准满足） | 06 |
| Geo | 高可用/容灾（主从复制） | 09 |
| SAST/DAST | 静态/动态安全扫描 | 07 |
| Secrets Manager | 19.0 平台内凭证管理 | 07 |

## 6. 常见误区

**误区一：GitLab 只是代码托管**——它是**一体化 DevOps 平台**：CI/CD + 安全 + 制品库 + AI 一体——"**GitHub 是仓库，GitLab 是流水线工厂**"（01 篇）。

**误区二：GitLab CI 与 Jenkins 二选一必争**——**GitLab CI 强在"与代码同仓库"（MR 即流水线），Jenkins 强在插件生态与存量**——"**新项目 GitLab CI，存量 Jenkins 迁移是渐进**"（03 篇）。

**误区三：only/except 够用**——**rules 才是 2026 写法**（条件更丰富：pipeline source/变更路径）；only/except 是新代码别用的老语法（03 篇）。

**误区四：流水线靠复制粘贴**——**include 复用 + CI/CD Components 版本化**是 2026 标准——"**复制粘贴的流水线 = 配置漂移**"（04 篇）。

**误区五：安全扫描是上线前的事**——**DevSecOps 把扫描塞进流水线**（每次 MR 都扫）——"**安全左移：扫描在合并前，不在上线后**"（07 篇）。

**误区六：密钥放 CI 变量明文**——**masked/protected 变量 + 19.0 Secrets Manager** 是正确姿势（07 篇）；**密钥进仓库/代码 = 泄露即事故**（Secrets Manager 就是为它而生）。

**误区七：GitLab 必须自建**——**GitLab.com（SaaS）+ Self-Managed + Dedicated 三种形态**——"**小团队用 SaaS，合规要求私有化，大企业 Dedicated**"（02 篇）。

**误区八：CI 只跑测试就行**——**构建、制品、安全扫描、部署都是流水线的职责**——"**流水线是'从提交到上线'的自动化，不是'测试自动化'**"（03/07 篇；构建产物直接进制品库，08 篇）。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 + 02 | 起 GitLab.com 账号或 Docker 实例，建项目 |
| 2 | 03 | 写第一个 .gitlab-ci.yml（build/test/deploy 三阶段） |
| 3 | 04 | needs DAG + parallel matrix + include 复用 |
| 4 | 05 | 注册一个 Docker executor runner + tags 路由 |
| 5 | 06 | GitLab Flow 环境分支 + MR approval 配置 |
| 6 | 07 + 08 | 加 SAST 扫描 + 推送容器镜像到 Registry |
| 7 | 10 自测 + 面试 | 三合一项目 + 20 题（全程用 Docker 单机实例练习，备份纪律从第一天做起） |

## 8. 快速自测 10 题

1. GitLab 与 GitHub 的本质差异？三种形态？
2. 2026-08 的版本基线？19.0/19.2 的主线是什么？
3. pipeline/job/stage 的关系？同阶段 job 怎么执行？
4. artifacts 与 cache 的区别？
5. rules 与 only/except 的差异？
6. needs 解决什么问题？parallel matrix 呢？
7. Runner 的三种类型与 tags 的作用？
8. GitLab Flow 与 GitHub Flow 的差异？
9. SAST 与 DAST 的时机与分工？
10. Secrets Manager 是什么？masked 变量呢？

## 9. 参考来源

- [GitLab 官方文档（CI/CD/管理员/用户指南）](https://docs.gitlab.com/)
- [GitLab 19.0 发布公告（Secrets Manager/AI 特性）](https://about.gitlab.com/whats-new/19-0/)
- [GitLab 19.2 发布解读（受治理的智能体自动化）](https://softprom.com/gitlab-19-2-agentic-automation-devsecops)
- [GitLab CI/CD 官方文档（.gitlab-ci.yml 完整参考）](https://docs.gitlab.com/ci/)
- [GitLab Runner 官方文档（安装/executor/配置）](https://docs.gitlab.com/runner/)
- [GitLab CI Test Automation Guide 2026（流水线与缓存实战）](https://qaskills.sh/blog/gitlab-ci-test-automation-guide-2026)

---

**下一模块**：[01-GitLab是什么.md](01-GitLab是什么.md)
