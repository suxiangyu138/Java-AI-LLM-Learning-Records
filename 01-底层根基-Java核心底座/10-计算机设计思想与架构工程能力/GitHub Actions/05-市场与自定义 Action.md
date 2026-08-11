# 05-市场与自定义 Action：官方集、三种类型与版本策略

> 定位：Action 是 Actions 生态的"零件市场"——90% 的需求官方 actions 集已覆盖，剩下 10% 需要自建。本篇讲透官方集清单、三种 Action 实现类型、action.yml 契约与版本引用策略，以及"什么时候该自建"的判断标准。

## 1. 官方 actions 集（先查这里，再搜市场）

GitHub 官方维护的 `actions/*` 仓库是质量与安全基线，Java 工程高频使用清单：

- **actions/checkout@v4**：检出仓库代码，所有需要代码的 job 第一步；`fetch-depth: 0` 拉全量历史（语义版本计算、SonarQube 分析需要）。
- **actions/setup-java@v4**：安装 JDK——`distribution: temurin`（推荐）、`java-version: '21'`、`cache: maven`（自动缓存依赖，替代手写 cache 步骤）。
- **actions/upload-artifact@v4 / download-artifact@v4**：job 间/运行间传递制品（07 篇）。
- **actions/cache@v4**：通用依赖缓存（07 篇）。
- **actions/github-script@v9**（2026-05 发布）：用 JS 调 GitHub API 做"评论、标记、审批"等平台操作——v9 破坏性变更：**`require('@actions/github')` 不再可用（ESM-only）**，新增注入式 `getOctokit` 工厂函数（支持多 token、GitHub App token、跨组织访问）。升级老脚本前必读迁移说明。
- 其它常用：`actions/setup-python`、`actions/configure-pages`（Pages 部署）、`docker/build-push-action`、`softprops/action-gh-release`（发版，第三方但事实标准）。

**选 Action 的评估四问**：① 维护活跃度（最近提交/版本是否旧于一年）？② 下载量与 stars（市场页面可见，低下载量 = 高风险）？③ 源码可读（直接进仓库看 action.yml 与脚本，是否清晰）？④ 安全基线（README 是否建议 pin SHA、有无 security 披露流程）？四问过不去，宁可用 `run:` 手写 shell 也别引。

### 1.1 官方集使用节奏与升级纪律

官方 Action 的版本节奏与大版本语义值得单独掌握：`@v4` 是 major 标签，官方在发布 minor 时移动（`v4.5.1` → `@v4` 指向最新 minor）；major 升级（如 checkout v4 → v5）伴随破坏性变更（输入改名、默认行为变化），升级前必须读发布说明与迁移指南。工程实践：**新项目直接追最新 major**（2026-08 时点 checkout@v4、setup-java@v4、cache@v4、upload-artifact@v4、github-script@v9 是当前线）；存量项目按季度评估升级，Dependabot 对 Action 的自动 PR 是"升级不忘"的保障。官方集的一个隐藏价值是**文档即规范**——每个官方 Action 的 README 都演示了"输入输出设计、缓存约定、错误处理"的正确写法，自建 Action 前先抄官方结构，比自己发明更稳。

## 2. 三种 Action 类型

**JavaScript Action**（最主流）：`action.yml` 声明元数据 + `runs.using: node20` + 编译后的 `dist/` 入口。价值：跨平台（Linux/Windows/macOS 原生跑）、无 Docker 依赖、快。工程规范：源码与编译产物分离（`src/` 与 `dist/` 都要入库——`uses:` 引用的是仓库快照，现场不编译）；用 `@vercel/ncc` 打包避免运行时依赖安装。

**Docker 容器 Action**：`runs.using: docker` + `image: Dockerfile`。价值：环境完全自包含（特定工具链、系统库），适合"宿主环境凑不齐"的场景；代价：每次运行拉镜像（慢）、不支持 Windows Runner、无法访问宿主 shell 变量。适用信号：工具链特殊（如特定 gcc 版本）、需要网络隔离环境。

**复合 Action（Composite）**：`runs.using: composite` + 多个 `run` 步骤的封装——**不写代码，纯 YAML 编排**。价值：把"3 个步骤 + 5 个参数"的固定套路打包复用，跨仓库分享 shell 逻辑；是 2026 年自建 Action 的默认起点（轻量、无构建、易 review）。

## 3. action.yml 契约

三种类型共享同一元数据骨架：

```yaml
name: 'My Java Build'        # 市场展示名
description: '编译并缓存 Maven 工程'
inputs:                      # 输入契约（with 传参的合法键）
  java-version:
    description: 'JDK 版本'
    required: true
    default: '21'
outputs:                     # 输出契约（steps.id.outputs.x 的合法键）
  artifact-id:
    description: '构建制品 ID'
runs:
  using: composite
  steps:
    - run: mvn -B package
      shell: bash
```

**契约纪律**：inputs/outputs 是 Action 的"API 签名"——改输入名 = 破坏性变更（引用方全部要改）；新增可选输入带 default 保持兼容；description 写清"做什么、何时用、边界是什么"（呼应《软件工程上的设计思想》接口设计）。

**输入输出的设计原则（自建 Action 的质量分水岭）**：其一，**必填输入宁少勿多**——每个必填输入都是引用方的负担，能推断的不让传（如从仓库上下文自动取的就别设计成输入）；其二，**校验前置**——Action 入口处先校验输入合法性（版本号格式、枚举值范围），非法输入快速失败并给出明确错误信息，别把错误留到执行中途；其三，**默认值语义化**——default 不是随便填的兜底，而是"90% 场景的正确行为"，文档与默认值必须一致；其四，**输出可观测**——核心输出（产物路径、耗时、结果摘要）进 `GITHUB_STEP_SUMMARY` 让运行页可见，排障不必翻日志；其五，**向后兼容承诺**——新输入必须带 default 才能发 minor，输入改名要提供过渡期别名，major 升级才允许破坏——这五条写进自建 Action 的开发规范，产出的 Action 才有"官方质感"。

## 4. 版本引用策略（安全与兼容的平衡）

- **tag 引用（默认）**：`uses: actions/checkout@v4`——语义版本大版本 tag，稳定可升级；但 tag 可被仓库所有者移动（supply chain 风险）。
- **commit SHA 引用（安全基线）**：`uses: actions/checkout@a6a08d5...`——不可变，供应链攻击免疫；2026 年企业安全基线要求第三方 Action 必须 pin SHA（Dependabot 自动生成 bump 更新）。
- **`$/` 同仓库自引用（2026-07-30 新增）**：`uses: $/actions/my-build/action.yml`——引用**当前仓库**的 Action，自动解析到正在运行的精确 commit，无需 checkout；是仓库内部组合 Action 的推荐语法（runner 2.336.0+）。
- **版本升级节奏**：官方 Action 追 minor 版本（`@v4` → `@v5` 按发布说明评估）；第三方 Action 用 Dependabot 自动 PR + CI 门禁验证。

**版本引用的完整策略（生产仓库落地版）**：把"引什么版本"写成团队规范，避免每个仓库各写各的——常规写法是"官方集用 `@major`（如 `@v4`，跟随 minor 自动升级）+ 第三方用 commit SHA + Dependabot 统一更新"；严格场景（金融/政务合规）则**全部 pin SHA**（含官方集），升级走人工 PR + 发布说明评估。pin SHA 的代价是"升级要主动做"——Dependabot 的 PR 就是升级提醒，配合 CI 跑一遍验证即可合入；完全不升级的 pin 会变成"静态锁死"，安全漏洞跟进不上，比用 tag 更危险。策略的本质是"**版本是安全与便利的平衡点，团队要显式选择并保持**"。

**Action 生态的维护成本（自建前最后一问）**：市场 Action 的平均寿命与维护活跃度参差——选 Action 时把"它一年后还在不在"也算进去：看仓库星标趋势、最近 release 时间、issue 响应速度；依赖高风险 Action 的替代预案（同功能替代品、或自建兜底）要在选型文档里写明。反过来，自建 Action 一旦公开，就背负了同样的维护承诺——**引入与自建都是负债，选择的是负债的种类**：引第三方负债的是"不可控的更新节奏"，自建负债的是"自己的维护时间"，团队按自己的维护能力与风险偏好选。

## 5. 自建 Action 的判断标准

**该自建的信号**：① 同一套路在 3+ 仓库重复（如"构建 + 推送内部镜像"）；② 有私有逻辑不能暴露（内部工具链、许可证校验）；③ 需要参数化与版本化复用（别人要 `with` 传参）；④ 市场没有可信替代。

**不该自建的信号**：① 一次性的 shell 脚本（直接 `run:`）；② 需要共享"跨仓库机密"（用 org secrets 而非 Action）；③ 团队无人维护（Action 是会腐烂的代码，无人维护不如内联）。

**发布路径**：私有仓库组织内共享（`uses: org/repo@v1`）→ 公开市场发布（需 public repo + 发布说明）；发布前跑 actionlint + 单元测试（JS Action 用 jest 测核心逻辑）。

**自建 Action 的工程规范（企业标准）**：仓库结构固定为 `action.yml` + `src/`（源码）+ `dist/`（编译产物）+ `tests/` + README（用法、输入输出表、变更记录）；CI 门禁含 actionlint、单元测试（jest 测核心逻辑，mock 掉网络与文件系统）、集成测试（一个真实 workflow 引用本地路径跑通）；发布用 GitHub Release 自动打 tag（`@v1` 跟随 `v1.x.y` 移动）；README 必须写清"做什么、何时用、边界"三件事——别人能不能放心引用你的 Action，取决于文档与测试的完备度，这是 2026 年企业自建 Action 的验收标准。

**官方集补充（发布/容器场景）**：`docker/build-push-action`（多平台构建 + 推送 registry，配 `cache-from/to` 联动 Actions 缓存）、`docker/login-action`（registry 登录，密码走 secrets）、`softprops/action-gh-release`（打 Release + 传附件，第三方但事实标准）、`actions/configure-pages`（GitHub Pages 部署上下文）。发布类工作流的完整链条是"构建 → 扫描（trivy/安全）→ 签名（cosign）→ 推送 → 发 Release"，每环一个 Action，链条本身用可复用工作流封装（08 篇）。

> 🎯 核心要点：用 Action 的三层决策——**官方集 > 可信第三方 > 自建**；自建默认从复合 Action 开始（零构建、易维护）；引用一律"major tag 起步、第三方 pin SHA"；2026 新增的 `$/` 让仓库内部复用不再需要跨仓库发布。

---

**下一模块**：[06-安全工程：权限、密钥与 OIDC](./06-安全工程：权限、密钥与%20OIDC.md) / **返回总览**：[00-GitHub Actions 知识体系总览](./00-GitHub%20Actions%20知识体系总览.md)

**参考来源**：

- [github-script v9 发布说明 | GitHub](https://github.com/actions/github-script/releases)
- [Reference same-repository actions with self-repository syntax | GitHub Changelog 2026-07-30](https://github.blog/changelog/2026-07-30-reference-same-repository-actions-with-self-repository-syntax/)
- [Metadata syntax for GitHub Actions | GitHub Docs](https://docs.github.com/en/actions/reference/metadata-syntax-for-github-actions)
- [Using scripts to test your actions | GitHub Docs](https://docs.github.com/en/actions/creating-actions/creating-a-javascript-action)
