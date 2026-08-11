# SonarQube 知识体系总览
> 一句话定位：代码质量平台的事实标准——静态分析、质量门禁、技术债治理三位一体；2026 年免费版改名 Community Build 并采用日历版本（26.7.0），每月一发。

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [常见误区](#5-常见误区)
6. [一周计划](#6-一周计划)
7. [自测题](#7-自测题)
8. [参考来源](#8-参考来源)

## 1. 知识体系导图

```text
SonarQube（代码质量平台）
├── 01 是什么（定位/2026版本格局/与静态分析工具族关系）
├── 02 安装与快速开始（Docker部署/首次扫描/验证）
├── 03 静态分析原理（七类问题/Clean as You Code/增量）
├── 04 质量门禁与指标（Quality Gates/指标家族/门禁设计）
├── 05 Scanner 与 CI 集成（CLI-Maven-Gradle/流水线门禁）
├── 06 SonarLint 与 IDE 集成（实时反馈/connected mode）
├── 07 规则体系与自定义（Profiles/severity/误报治理）
├── 08 多语言与项目配置（Java-JS-Python/多模块/覆盖率）
├── 09 生产部署与运维（Docker-K8s/备份/升级/权限）
└── 10 生产实战与自测（质量门禁体系搭建/面试/20题）
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | 是什么 | 定位、2026 版本格局、工具族关系 | 所有人 |
| 02 | 安装与快速开始 | Docker 部署、首次扫描 | 新手 |
| 03 | 分析原理 | 七类问题、Clean as You Code | 必须 |
| 04 | 质量门禁 | Gate 条件、指标家族、设计 | 必须 |
| 05 | Scanner 与 CI | 三种 Scanner、流水线门禁 | 必须 |
| 06 | SonarLint | IDE 实时反馈、connected mode | 进阶 |
| 07 | 规则体系 | Profiles、severity、误报治理 | 进阶 |
| 08 | 多语言配置 | 语言分析、多模块、覆盖率 | 必须 |
| 09 | 生产运维 | 部署、备份、升级、权限 | 进阶 |
| 10 | 实战与自测 | 门禁体系搭建、面试、20 题 | 毕业 |

## 3. 学习路线推荐

**路线一：快速上手（1-2 天）**——01 → 02 → 05 → 10。目标：Docker 起一个 SonarQube，扫描一个项目，看懂问题报告。

**路线二：质量工程（3-5 天）**——路线一 + 03 → 04 → 08。目标：质量门禁设计 + CI 流水线集成，让「不合格代码合不进去」。

**路线三：平台运营（一周）**——路线二 + 06 → 07 → 09。目标：规则治理、误报管理、多语言支持、生产级运维。

**先厘清分工**：本体系是 SonarQube 独立深潜版（11 篇）；同级 [[../代码质量/00-代码质量管理体系总览|代码质量体系]] 有完整的工具族全景（SpotBugs/PMD/Checkstyle/代码审查/度量治理），其中 [[../代码质量/02-SonarQube代码质量平台实战|SonarQube 实战篇]] 是 1615 行的单篇超深参考——本体系的各篇与其互补：那边查参数表与 API，这边建立知识骨架与决策逻辑。CI 侧衔接 [[../运维/CI%20CD灰度发布/04-质量门禁与安全左移|CI/CD 质量门禁]] 与 [[../运维/Jenkins/05-构建工具集成|Jenkins 集成]]。

## 4. 核心概念速查

| 概念 | 一句话 |
|------|-------|
| Community Build | 免费版（原 Community Edition，2024 底改名），日历版本每月一发 |
| SonarQube Server | 商业线（2026.1/2026.4），有 LTA 长期支持 |
| 静态分析 | 不运行代码的代码检查——7 类问题、6500+ 规则 |
| Clean as You Code | 只治理新代码的理念——增量质量而非历史债 |
| Quality Gate | 质量门禁：一组条件，不满足则流水线失败 |
| Quality Profile | 规则集：按语言配置启用哪些规则与严重级 |
| severity | 问题严重级五档（Blocker/Critical/Major/Minor/Info） |
| 技术债 | 问题修复的估计工作量（时间单位） |
| 覆盖率 | 测试覆盖（JaCoCo 等报告集成） |
| SonarScanner | 分析器：CLI/Maven/Gradle/CI 各形态 |
| SonarLint | IDE 插件：写代码时的实时反馈 |
| New Code Period | 「新代码」的定义窗口（Clean as You Code 的边界） |
| Security Hotspot | 安全热点：需要人工审查的可疑点 |
| 误报 | 规则误判——治理重点是「少而准」的规则集 |

## 5. 常见误区

**误区一：SonarQube 只是「检查代码风格」**。它是质量平台——bug、漏洞、代码坏味道、安全热点、覆盖率、技术债全维度；Checkstyle 那种风格检查只是它的一个小子集。

**误区二：规则越多越好**。全量规则开满 → 误报淹死团队 → 门禁被「suppress」架空——规则集要「少而准」，按团队场景裁剪。

**误区三：一次修完所有历史问题**。存量代码问题动辄上千——Clean as You Code：只卡新代码，历史债专项治理，别让门禁变成「不可能通过」的摆设。

**误区四：质量门禁只在发布前跑**。合入前卡住（PR 门禁）比发布前卡住便宜 100 倍——门禁进 CI 流水线，不合格代码进不了 main。

**误区五：SonarQube 结果不用人工看**。静态分析有误报——安全热点需要人工审查，门禁失败要人看报告定夺，全自动「失败即禁」会制造绕路。

**误区六：免费版功能够用就够**。Community Build 无分支分析、无 PR 装饰——只在 main 上分析——PR 级反馈要 Developer 版或 CI 脚本补偿。

**误区七：装了 SonarQube 质量就好了**。工具只发现问题，治理靠流程——门禁 + 例会 + 修债节奏，工具是其中一环。

**误区八：免费版与商业版功能一样**。Community Build 无分支分析、无 PR 装饰、无数据流安全——局限要认、补偿要做，别假装不存在。

**误区九：升级不是事**。Community 无 LTA 每月一发——「升级是运维常态」，备份 + Release Notes + 规则评审缺一不可。

**误区十：SonarQube 能测出所有问题**。静态分析查不到业务正确性与运行时问题——配合单测、集成测试与代码审查，它是体系一环不是全部。

九个误区的共同根源只有一个：**把 SonarQube 当成「一个查 bug 的工具」**。它是「质量平台」——规则引擎 + 门禁机制 + 度量积累 + CI 集成四位一体，价值在「机制自动执行」而非「报告生成」；同时它有自己的边界（查不到语义正确性）与版本现实（免费版缺 PR 级能力、月度升级节奏）。带着「装个工具」的心态，门禁、治理、运维必然漏；换成「质量机制平台」的心智——部署即配套、门禁即流程、升级即常态——整套体系才立得住。

## 6. 一周计划

| 天 | 内容 | 产出 |
|:---:|------|------|
| Day1 | 01 + 02：定位与部署 | Docker 起服务 + 首次扫描 |
| Day2 | 03：分析原理 | 看懂问题报告与指标 |
| Day3 | 04：质量门禁 | 设计并启用第一个 Gate |
| Day4 | 05：CI 集成 | 流水线门禁跑通 |
| Day5 | 08：多语言与覆盖率 | 覆盖率集成 + 多模块 |
| Day6 | 07 + 06：规则与 IDE | 规则治理 + SonarLint |
| Day7 | 09 + 10：运维与实战 | 备份升级 + 门禁体系 + 20 题 |

## 7. 自测题

1. Community Build 与 SonarQube Server 的区别与版本规则？
2. 静态分析七类问题是什么？
3. Clean as You Code 为什么是 2026 主流理念？
4. Quality Gate 的一组典型条件是什么？
5. 三种 SonarScanner 的适用场景？
6. SonarLint 的 connected mode 是什么？
7. severity 五档是什么？
8. Community Build 的三大局限？
9. 误报治理的正确姿势？
10. 质量门禁为什么必须进 CI？

## 8. 参考来源

- [SonarQube Community Build 官方文档（质量门禁）](https://docs.sonarsource.com/sonarqube-community-build/quality-standards-administration/managing-quality-gates/introduction-to-quality-gates)
- [SonarQube Community Build Release Notes](https://docs.sonarsource.com/sonarqube-community-build/server-update-and-maintenance/release-notes)
- [docker-sonarqube Releases（2026.4.0）](https://github.com/SonarSource/docker-sonarqube/releases)
- [endoflife.date：SonarQube Community 版本跟踪](https://endoflife.date/sonarqube-community)
- [SonarQube 2026 Review（功能与局限）](https://dev.to/rahulxsingh/sonarqube-review-2026-pros-cons-and-real-user-feedback-235n)
- [SonarQube：Pricing, Capabilities & Alternatives](https://www.cybersectool.com/tools/sonarqube)

---

**下一模块**：[01-SonarQube是什么](01-SonarQube是什么.md) → 从定位与 2026 版本格局开始。
