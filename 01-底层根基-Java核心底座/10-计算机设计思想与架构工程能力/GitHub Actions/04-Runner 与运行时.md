# 04-Runner 与运行时：执行环境、定价与自托管

> 定位：Runner 是工作流的执行环境——"runs-on 写什么、花多少钱、跑在谁的机器上"三个问题直接决定 CI 的速度与账单。本篇基于 2026-01-01 新定价，讲透托管 Runner 规格、自托管路线与运行时机制。

## 1. GitHub 托管 Runner：标准规格与 2026 降价

标准 Runner 覆盖三操作系统，公共仓库免费，私有仓库消耗分钟配额：

- **ubuntu-latest**（Linux 2 核 7GB）：最常用，分钟价 $0.006（2026-01-01 起从 $0.008 降 25%）。
- **windows-latest**（Windows 2 核）：$0.010（降 38%）——Windows 构建成本高的典型。
- **macos-14/15**（macOS 3/4 核）：$0.062（降 23%）——iOS/macOS 生态才值得用。

2026-01-01 降价背景：GitHub 引入 $0.002/分钟的 "Actions cloud platform charge"，已包含在降价后的托管 Runner 计费价中；**自托管 Runner 的平台费原定 2026-03-01 生效，社区反对后无限期推迟**——自托管保持免费。

**分钟配额**（org 全局共享）：Free 2000 分钟/月、Pro 3000、Team 3000、Enterprise 50000。超出后按每分钟价格计费（或按消费额度 pay-as-you-go）。

## 2. Larger Runners：规格与定价表

标准 Runner 只有 2 核，编译型项目（Java 全量构建、Docker 多阶段）需要更大规格——Larger Runners 按核数分档：

| 规格 | Linux 价格（$/min） | 较旧价降幅 | 典型用途 |
|---|---|---|---|
| 4 核 | $0.012 | -25% | 中型编译 |
| 8 核 | $0.022 | -31% | Java/Maven 全量构建 |
| 16 核 | $0.042 | -34% | 大型单体编译 |
| 32 核 | $0.082 | -36% | 重型 CI |
| 64 核 | $0.162 | -37% | 极端构建 |
| arm64 8 核 | $0.014 | -30% | 性价比之王 |
| GPU 4 核 | $0.052 | -26% | 训练/推理测试 |

**三个定价真相**：其一，**Larger Runners 在公共仓库也计费**（不走免费分钟），且不支持分钟配额抵扣——用之前先看账单；其二，arm64 比同规格 x64 便宜 20-39%（Linux arm64 8 核 $0.014 vs x64 $0.022），Java 构建（JDK arm64 版齐全）直接换 `runs-on: ubuntu-24.04-arm` 即可；其三，规格要与负载匹配——lint/单元测试 2 核足够，全量编译 8 核封顶，64 核只对"构建即瓶颈"的巨型项目有意义。

### 2.1 规格选型方法论（先想清楚再掏钱）

- **按负载特征分档**：IO 密集（npm install、Maven 依赖下载）2-4 核足够——瓶颈在网络与磁盘，核多了白花钱；CPU 密集（全量编译、测试执行）8-16 核有明显加速；内存密集（JVM 大堆、容器构建）看内存规格而非核数。结论：**先看瓶颈再看核数**，用一次 8 核试跑对比 2 核耗时，加速比不明显就降回小规格。
- **按频率分档**：每次提交跑的（lint、快速测试）用小规格，每天跑一次的（夜间全量构建）可以上大规格——同一负载在"高频小规格 + 低频大规格"的分配下，总成本最低。
- **按平台分档**：Linux 是默认与最便宜；Windows 只给"必须 Windows 验证"的 job（PowerShell 脚本、.NET 平台）；macOS 只在 iOS/macOS 构建时用——跨平台验证矩阵用 `include` 控制平台成员数量，别三平台全量跑。
- **价格敏感团队的默认配置**：`ubuntu-latest`（2 核）跑 lint + 单测，`ubuntu-24.04-arm`（8 核 $0.014/min）跑全量编译，Windows/macOS 只进发版前验证矩阵——这是 2026 年中小团队"体验与账单平衡"的事实标准。

## 3. Self-Hosted Runners：免费但要会管

自托管 Runner 的价值：**无限免费分钟、内网资源、定制镜像**。代价是运维与安全责任全部自担：

- **注册与标签**：仓库/org 设置里获取注册 token，`./config.sh --url <repo> --token <token> --labels gpu-linux` 注册；`runs-on: [self-hosted, linux, gpu]` 按标签路由。标签即路由键，命名规范（`os-cpu-gpu`）直接决定调度体验。
- **Runner Groups**：org 级分组 + 环境级限制——生产环境的 job 只能跑指定组的 Runner，是"环境隔离"的基础设施层实现。配置链路：org 设置建组 → 把 Runner 移入组 → 环境/仓库限制"仅允许使用组 X"——三方对账后，生产 job 的 `runs-on` 标签必须显式指向生产组，否则报"无可用 Runner"（这是"环境隔离"的第一道硬约束，比靠自觉安全得多）。
- **注册与更新运维**：自托管 Runner 的注册 token 有效期短（org 级 1 小时），自动化注册用 GitHub App 或托管脚本；Runner 自动更新默认开启，内网/离线环境需配置镜像更新源并规划版本漂移（Runner 版本落后会导致新语法不可用，如 `$/` 需 2.336.0+）；健康监控用 `GET /orgs/{org}/actions/runners` API 拉在线状态，掉线告警接入值班。
- **安全红线**：自托管 Runner 是**不可信代码执行平台**——仓库内的 PR 可提交任意脚本，恶意代码能拿到 Runner 机器上的密钥与内网权限。对策：① 只对受信分支开自托管（`if: github.event_name == 'push' && github.ref == 'refs/heads/main'`）；② Runner 用一次性容器/VM 隔离（每次 job 全新环境）；③ 不在 Runner 上留存跨任务密钥。
- **平台费推迟的意义**：GitHub 官方明确自托管免费延续，混合部署（自托管跑重活 + 托管跑矩阵）成为 2026 年主流成本结构。

## 4. 运行时机制：一次 job 的完整旅程

1. **调度**：事件触发 → GitHub 平台校验权限/过滤条件 → 按 `runs-on` 分配 Runner（托管：起新 VM；自托管：等待空闲）。
2. **环境准备**：Runner 拉取最新 runner 版本与 Action 仓库引用，**checkout 前无工作目录内容**——所有依赖通过 checkout 与 setup 步骤显式获得（这是 CI 可复现性的根基）。
3. **执行**：job 内 steps 顺序执行，每步独立 shell（默认 bash -e）；失败即停（`continue-on-error` 例外）；`GITHUB_OUTPUT` 等文件在 step 间传递数据。
4. **结果上报**：日志流式上传、状态徽章更新、状态检查（status check）反馈到 PR。
5. **清理**：托管 Runner 销毁 VM（环境完全干净）；自托管 Runner 清理工作目录与临时文件。

**运行时版本要求**：新语法（如 `$/` 自引用）要求 runner 2.336.0+；自托管 Runner 需定期更新（自动更新默认开启，内网环境需镜像更新源）。

### 4.1 计费粒度与账单解读

计费的最小粒度是**分钟**，但账单明细的解读有三个易混点：其一，**并行 job 的分钟叠加**——同一运行里 3 个 job 并行各跑 5 分钟，账单是 15 分钟（不是 5 分钟），"运行次数 × 平均 job 数 × 单 job 时长"才是分钟消耗的正确算法；其二，**排队不计费**——job 等待 Runner 空闲的时间不产生费用，但拖慢交付节奏，排队积压是"并发配额不够"的信号而非账单问题；其三，**Larger Runner 不走分钟配额**——公共仓库里它照样计费（04 篇已强调），私有仓库里它优先从"额外分钟"扣而非配额——账单上看到"Larger 独立计费行"属正常，别当成计费 bug。学会读账单明细（Settings → Billing → Usage 按工作流展开），是成本治理（09 篇）的第一步。

## 5. 并发与配额现实

- 并发 job 上限按计划分档（Free 20、Pro 40、Team 60、Enterprise 180）；超出排队，不报错——大矩阵并行时注意队列积压不是故障。
- 定时工作流（schedule）在仓库 60 天无活动时**自动禁用**——长期闲置仓库的定时任务会静默消失，告警监控要有"任务消失"检测。
- 运行保留：工作流日志/制品默认保留 90 天（可配置 1-400 天）——审计合规与存储成本之间的旋钮（07 篇详述）。

**托管 Runner 的软件环境（写 run 命令前先确认）**：GitHub 托管 Runner 预装了大量工具（Node、Python、Java、Docker CLI、Azure/AWS/GCP CLI、gh 等，镜像每月更新），"装了什么"以官方 Runner Images 文档为准——**写 `run:` 命令前先查预装清单**，能省掉一半"工具不存在"的报错；特殊版本（如某特定 gcc）用 `setup-*` 系列 Action 安装，别赌镜像里有。镜像更新策略是"月度 + 紧急安全补丁"，锁定工具版本的工作流要用 `setup-*` 显式装（setup-java 装指定 JDK 而非依赖镜像默认），否则镜像一更新，CI 结果可能静默漂移——"同样的代码，上周绿这周红"的排查，先怀疑环境漂移再怀疑代码。

**运行时与工作流的依赖关系（再想深一层）**：Runner 版本与语法特性绑定（`$/` 需 2.336.0+），这是"托管环境永远领先、自托管环境版本漂移"的结构性差异——自托管团队要在 Runner 更新上投入固定节奏（自动更新 + 每月人工确认），否则"新语法为什么在我这跑不了"会反复出现；托管 Runner 没有这个问题，但换来的是"镜像更新不受控"（前述环境漂移）。两条路线各有结构性代价，选型时把它算进去——**运行时的运维承诺，是 Runner 选型的第一维度**。

> 🎯 核心要点：Runner 决策 = 成本决策。默认策略：常规矩阵用标准 ubuntu + arm64 混跑，编译重活上 8-16 核 Larger，内网/GPU/长任务来自托管；自托管永远记住一句——"Runner 上跑的是不可信代码，隔离比便利重要"。

---

**下一模块**：[05-市场与自定义 Action](./05-市场与自定义%20Action.md) / **返回总览**：[00-GitHub Actions 知识体系总览](./00-GitHub%20Actions%20知识体系总览.md)

**参考来源**：

- [GitHub Actions new pricing – Hosted runner price drop | GitHub Roadmap](https://github.com/github/roadmap/issues/1196)
- [GitHub Actions Pricing and Usage | GitDash 2026](https://gitdash.dev/blog/github-actions-pricing-usage)
- [About self-hosted runners | GitHub Docs](https://docs.github.com/en/actions/hosting-your-own-runners/managing-self-hosted-runners/about-self-hosted-runners)
- [About GitHub-hosted runners | GitHub Docs](https://docs.github.com/en/actions/using-github-hosted-runners/about-github-hosted-runners)
