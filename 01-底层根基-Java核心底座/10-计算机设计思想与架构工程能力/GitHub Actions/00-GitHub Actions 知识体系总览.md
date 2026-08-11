# 00-GitHub Actions 知识体系总览：从事件模型到生产级 CI/CD

> 定位：面向后端工程师的 GitHub Actions 专项知识体系——Actions 不是"照抄一个 yml 就能跑"的模板粘贴，而是事件模型、YAML 语法、表达式、Runner 运行时、安全工程、成本治理六层能力的系统工程。本体系讲透机制与取舍，配可复现的 Java 工程示例，与《CI/CD 与灰度发布》体系分工互补：那边讲"Actions 在 CI/CD 全景中的位置与制品链路"，这边讲"Actions 本身的语法、运行时、生态与生产工程"。

**版本窗口：2026-08 基准**（2026-01-29 case 函数与 if 改进、2026-06-25 并行步骤、2026-07-30 同仓库自引用 `$/`、2026-01-01 Runner 降价最高 39%、self-hosted 平台费无限期推迟、OIDC 自定义属性 GA、gh-aw Agentic Workflows 技术预览）

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心速查表](#4-核心速查表)
5. [体系联动与分工](#5-体系联动与分工)
6. [常见误区速避](#6-常见误区速避)

## 1. 知识体系导图

```text
GitHub Actions 知识体系（11 篇）
│
├── 认知层（是什么）
│   ├── 01-核心概念与事件模型：组件、触发事件、GITHUB 上下文
│   └── 02-Workflow 语法全解：on/jobs/needs/matrix/2026 新语法
│
├── 语言层（怎么写）
│   ├── 03-表达式与上下文：${{ }}、函数、九大上下文、if 陷阱
│   └── 04-Runner 与运行时：hosted/self-hosted、执行流程、时区
│
├── 生态层（用什么）
│   ├── 05-市场与自定义 Action：官方集、三种 Action 类型
│   └── 08-可复用工作流与矩阵：reusable/composite/matrix
│
├── 工程层（怎么扛）
│   ├── 06-安全工程：最小权限、secrets、OIDC、供应链
│   ├── 07-缓存与制品：cache、artifacts、输出传递
│   └── 09-成本管理与生产规范：定价、优化、团队规范
│
└── 前沿层（往哪走）
    └── 10-Agentic CI 与面试冲刺：gh-aw、AI 写工作流、面试
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 00 | 总览 | 全景、路线、速查、分工 | 所有人 |
| 01 | 核心概念与事件模型 | 组件、事件全解 | 入门 |
| 02 | Workflow 语法全解 | YAML 结构、2026 新语法 | 入门 |
| 03 | 表达式与上下文 | 函数、if 陷阱 | 进阶 |
| 04 | Runner 与运行时 | 定价、self-hosted | 进阶 |
| 05 | 市场与自定义 Action | 三种类型、供应链 | 实战 |
| 06 | 安全工程 | 权限、密钥、OIDC | 生产 |
| 07 | 缓存与制品 | cache/artifacts | 实战 |
| 08 | 可复用与矩阵 | reusable/matrix | 架构 |
| 09 | 成本与生产规范 | 定价优化、团队规范 | 生产 |
| 10 | Agentic CI 与面试 | gh-aw、冲刺 | 求职 |

## 3. 学习路线推荐

**路线 A（上手实战，2 天）**：01 → 02 → 05 → 07 → 08。以 Java 项目 CI 为主线：push 触发 → 多版本矩阵编译 → 缓存依赖 → 上传制品，跑通"照抄会跑、明白为什么"。

**路线 B（生产工程，3 天）**：A 全量 + 03 表达式 + 04 Runner + 06 安全 + 09 成本。覆盖权限最小化、secrets 分层、OIDC 云凭证、成本优化——企业级流水线的硬功夫。

**路线 C（架构与面试，2 天）**：速查表 → 06 安全 → 08 可复用 → 09 规范 → 10 面试冲刺（20 自测 + 面试题 + 速记）。GitHub Actions 是 DevOps 方向高频考点，能讲清"OIDC 怎么免密钥、缓存 key 怎么设计、矩阵怎么收敛成本"即拉开差距。

## 4. 核心速查表

| 主题 | 一句话本质 | 速记 |
|---|---|---|
| 工作流 | 事件驱动的 YAML 流水线声明 | on 触发 → jobs 并行 → steps 顺序 |
| 事件 | push/PR/定时/workflow_dispatch 四主类 | 手动触发用 workflow_dispatch |
| 上下文 | 运行时注入的数据对象 | github/needs/jobs/steps/runner/env |
| 表达式 | `${{ }}` 内联求值，非 shell 语法 | 函数 contains/startsWith/fromJSON |
| Runner | 执行环境，hosted 免费分钟 + larger 计费 | arm64 便宜 20-39% |
| 权限 | 最小权限 permissions 显式声明 | 默认写权限已改只读（2023+） |
| secrets | 加密变量，环境级 > org 级 > repo 级 | 日志中禁止 echo 密钥 |
| OIDC | 免长效密钥的云凭证方案 | token 换短期凭据，2026 支持自定义属性 |
| 缓存 | 依赖复用的键值存储 | 10GB 免费、200 次/分钟上传限流 |
| 制品 | 构建产物留存，跨 job 传递 | 存储池与 Packages 共享 |
| 可复用 | 工作流调工作流、Action 调 Action | reusable + composite 收敛重复 |
| 成本 | 分钟费 × 规格 × 频率 | 路径过滤 + arm64 + 合并矩阵 |

## 5. 体系联动与分工

**为什么本体系放在"设计思想与架构工程能力"目录**：GitHub Actions 表面是工具，实则是"事件驱动的声明式系统设计"——触发模型（事件/过滤/门禁）、资源模型（Runner/分钟/配额）、信任模型（权限/secrets/OIDC）三层设计思想贯穿全部篇章。它与《WorkFlow》工作流体系在 10 篇交汇：Agentic CI 是 Agentic Workflow 在 CI 域的投影（确定性外壳包非确定性内核）。读本体系时带着"这层设计解决什么问题"的视角，比记住语法更值钱——语法会演进（2026 一年就加了并行步骤与 `$/`），设计思想是常量。

- **与《CI/CD 与灰度发布》**（02 层）：该体系 03 篇《GitHub-Actions 与制品管理》是速览视角——核心概念、Java 实战、制品/Harbor/Nexus 链路；本体系是专项深潜——语法语言层（02/03 篇）、运行时（04 篇）、安全（06 篇）、成本（09 篇）均不与其重复，读本体系前建议先有该体系的"CI/CD 全景"概念（流水线七阶段、门禁四层、发布策略）。
- **与《Jenkins》体系**：Jenkins 与 Actions 是"自建 vs 托管"两条路线，09 篇对比视角可联动。
- **与《Git 知识体系》**：GitHub 的分支保护、PR 工作流、gh CLI 是 Actions 触发与协作的前提，01 篇事件模型默认读者已了解 PR 概念。
- **与《软件工程上的设计思想》**：幂等（CI 重跑）、韧性（重试/超时）、最小权限在 06/09 篇落地。

## 6. 常见误区速避

**学习心法**：这套体系最容易犯的错是"只记语法不记模型"——语法三个月一更新（2026 年就加了并行步骤、case、`$/`），而"事件 → 作业 → 步骤"的执行模型、"权限 → 密钥 → OIDC"的信任模型、"缓存 → 制品 → 输出"的数据模型二十年不会变。读每一篇时先问"这层解决什么问题"，把答案写进自己的心智模型，语法细节随查随用即可——这也是本体系把"机制论述"放在"语法清单"之前的编排原因。

**阅读顺序补充**：若只够时间读三篇，选 01（触发模型）+ 06（信任模型）+ 09（资源模型）——三个模型覆盖"为什么跑、凭什么跑、跑得起吗"的完整叙事，其余篇章按需补细节——三条路线任选其一即可入门，不必按序通读。

- 误区一：**工作流 = 模板粘贴**。不理解事件与表达式，改一个环境名就花半天——语法层两篇先过。
- 误区二：**secrets 可以 echo 出来调试**。密钥进日志即泄漏，调试用临时值或环境级密钥。
- 误区三：**权限默认安全**。老工作流依赖默认写权限的必须显式收紧，2026 新仓库默认只读。
- 误区四：**所有 job 都上 64 核**。规格匹配负载：lint 用 2 核，编译用 4-8 核，成本差 40 倍。
- 误区五：**缓存越多越好**。缓存抖动（thrash）触发 200 次/分钟上传限流，key 设计比缓存量大更重要。
- 误区六：**第三方 Action 随便用**。供应链攻击面在 Action 市场，pin SHA + Dependabot 是底线。
- 误区七：**缓存 key 按时间戳**。key 每次变 = 缓存永不命中 = 白装白存，还触发 2026 上传限流——key 只放内容哈希与版本维度。
- 误区八：**矩阵越大越全面**。全组合矩阵是成本放大器，日常跑代表组合、发版前跑全组合才是正确姿势。
- 误区九：**自托管 Runner 免费就是赚到**。免费的是分钟，不免费的是安全责任——不可信代码执行平台，隔离不到位会连累生产密钥。

**路线产出物**：路线 A 完成时能独立给一个 Java 仓库加上"路径过滤 + 矩阵测试 + 缓存 + 制品"四件套标准 CI（含 actionlint 校验）；路线 B 完成时能输出一份含权限基线、secrets 分层、OIDC 方案的流水线安全清单；路线 C 完成时能对"OIDC、缓存 key、成本治理、供应链"四个高频追问给出四段式回答。

**阅读约定**：示例以 Java 17/21 + Maven 工程为基线（与仓库《JUnit》《MyBatisPlus》体系版本对齐）；版本与定价信息以 2026-08 检索校准，落地前复核 GitHub 官方 Changelog；术语"工作流"特指 GitHub Actions workflow 文件，"Action"特指市场复用单元，二者不混用。

---

**下一模块**：[01-核心概念与事件模型](./01-核心概念与事件模型.md)

**同级联动**：《CI/CD 与灰度发布》（03 篇速览分工）·《Jenkins》（自建路线对比）·《Git 知识体系》（分支保护/PR/gh CLI）·《软件工程上的设计思想》（幂等/韧性/最小权限）

**参考来源**：

- [Actions steps can now be run in parallel | GitHub Changelog 2026-06-25](https://github.blog/changelog/2026-06-25-actions-steps-can-now-be-run-in-parallel/)
- [Smarter editing, clearer debugging, and a new case function | GitHub Changelog 2026-01-29](https://github.blog/changelog/2026-01-29-github-actions-smarter-editing-clearer-debugging-and-a-new-case-function/)
- [Reference same-repository actions with self-repository syntax | GitHub Changelog 2026-07-30](https://github.blog/changelog/2026-07-30-reference-same-repository-actions-with-self-repository-syntax/)
- [GitHub Actions: Late March 2026 updates | GitHub Changelog 2026-03-19](https://github.blog/changelog/2026-03-19-github-actions-late-march-2026-updates/)
- [GitHub Actions: Early April 2026 updates | GitHub Changelog 2026-04-02](https://github.blog/changelog/2026-04-02-github-actions-early-april-2026-updates/)
- [GitHub Actions new pricing – Hosted runner price drop | GitHub Roadmap](https://github.com/github/roadmap/issues/1196)
- [GitHub Actions Pricing and Usage | GitDash 2026](https://gitdash.dev/blog/github-actions-pricing-usage)
