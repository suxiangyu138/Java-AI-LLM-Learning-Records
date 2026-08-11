# 02-Workflow 语法全解：YAML 结构与 2026 新语法

> 定位：GitHub Actions 的语法是"声明式 YAML"，掌握顶层键、job 配置与 step 配置三个层次的完整字段，再叠加 2026 年新语法（并行步骤、同仓库自引用、服务容器覆盖），才能写出"一次写对、长期可维护"的工作流。

## 1. 顶层键全景

一个 workflow 文件的顶层键按职责分四组：

- **元信息**：`name`（展示名）、`run-name`（运行记录标题，可含表达式，如 `Deploy ${{ github.ref }}`）。
- **触发与权限**：`on`（事件，见 01 篇）、`permissions`（job/工作流级最小权限，见 06 篇）、`concurrency`（并发控制）。
- **执行声明**：`jobs`（必填，作业定义）、`env`（全局环境变量，job/step 级可覆盖）、`defaults`（默认 shell 与工作目录）。
- **策略**：`strategy`（在 job 级配置，矩阵与失败策略）。

**concurrency 是生产必备键**：同一分支的连续 push 应取消旧运行，避免"测试跑一半被覆盖"与重复部署：

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

注意：`cancel-in-progress: true` 会取消同组旧运行——对"部署类"工作流要慎用（部署到一半被取消是事故），部署场景常用 `false` 或按环境分组。

## 2. job 配置全解

job 级字段按用途分五组：

**环境与资源**：`runs-on`（Runner 标签，`ubuntu-latest`/`windows-latest`/`macos-14`/自托管标签）、`environment`（环境级 secrets 与保护规则，见 06 篇）、`services`（服务容器，如 MySQL 测试库）。

**依赖与并发**：`needs`（前置 job 列表，数组即"全部完成才跑"；`needs: [a, b]` 的语义是 AND）、`timeout-minutes`（job 超时，**生产必须设置**，防死循环烧分钟）。

**控制流**：`if`（job 级条件，如 `if: github.event_name == 'push'`）、`continue-on-error`（失败不阻塞，用于参考性检查）。

**矩阵与策略**：

```yaml
strategy:
  fail-fast: true          # 任一 job 失败即取消其余矩阵成员
  matrix:
    os: [ubuntu-latest, windows-latest]
    java: ['17', '21']
    exclude:               # 排除组合
      - os: windows-latest
        java: '17'
    include:               # 附加维度
      - os: ubuntu-latest
        java: '21'
        profile: release   # include 可注入额外字段
```

矩阵是"多版本 × 多平台"验证的标准武器，也是成本放大器（组合数 = 笛卡尔积，见 09 篇收敛策略）。

**defaults 与环境变量的细节**：`defaults.run` 有两个常用子键——`shell`（全局默认 shell，如 `bash --noprofile --norc -eo pipefail`）与 `working-directory`（默认工作目录，多模块仓库指定模块目录省去每步前缀）。环境变量的注入顺序是"平台注入（GITHUB_*）→ 工作流级 env → job 级 env → step 级 env"，后注入覆盖先注入——想在测试里覆盖平台变量（如改 PATH）在 step 级写；`env` 值支持表达式（`env: { REF: ${{ github.ref }} }`），但注意表达式求值后才是字符串，别指望 env 里做类型操作。**job 内多步骤共享环境变量是天然特性**（同一 shell 会话体系），跨 job 才需要 outputs——这两个机制的分工别搞混。

**输出**：`outputs`（job 级输出，供下游 `needs` 读取——`${{ needs.build.outputs.artifact }}`）。job 输出的定义方式是：step 里写 `echo "key=value" >> $GITHUB_OUTPUT` 后，job 级用 `outputs: { key: ${{ steps.step-id.outputs.key }} }` 显式暴露——**job 输出不是自动透传**，step 输出了但 job 没声明，下游就读不到，这是"明明写了输出却读不到"的经典原因。

## 3. step 配置与执行细节

step 级字段：`id`（引用锚点，下游用 `${{ steps.id.outputs.x }}`）、`uses`（Action）、`run`（shell 命令）、`with`（Action 输入）、`env`（step 局部环境变量）、`if`、`continue-on-error`、`working-directory`。

**两个高频陷阱**：

- `run` 里的多行命令用 `|` 块，但**块内的 `${{ }}` 与 `$VAR` 语义不同**——`${{ }}` 在运行前被平台求值替换（可注入恶意内容，见 06 篇脚本注入），`$VAR` 在运行时由 shell 展开。混用导致"表达式没被求值/被提前求值"是最常见的调试地狱。
- 步骤间传值用 `GITHUB_OUTPUT` 文件（`echo "key=value" >> $GITHUB_OUTPUT`），**不要用环境变量传值**——环境变量不跨 step 持久，且会泄漏进日志。

## 4. 2026 新语法（重点掌握）

**并行步骤（2026-06-25 GA）**：step 不再只能顺序执行，四个新关键字让"并行 + 聚合"成为语法原生能力：

```yaml
- run: mvn -B package
  background: true          # 后台异步执行，立即进入下一步
- run: docker build .       # 与上面并行跑
- wait: [步骤 id]           # 等待指定后台步骤完成（wait-all 等全部）
- run: echo "cleanup"
  cancel: [后台步骤 id]     # 不再需要时优雅终止后台步骤
```

`parallel` 是语法糖：`parallel: [a, b, c]` 等价于"全部转后台 + 自动 wait"。适用场景：并行构建多模块、后台起服务跑依赖测试再清理、打包与遥测并行。

**同仓库自引用 `$/`（2026-07-30）**：`uses: $/actions/...` 引用当前仓库内的 Action 或可复用工作流，自动解析到正在运行的精确 commit，**无需 checkout**——仓库内部组合的推荐语法（替代之前 `uses: ./` 的相对路径写法），需要 runner 2.336.0+。

**服务容器覆盖（2026-04）**：`services` 支持 `entrypoint` 与 `command` 键，覆盖镜像默认入口，命名与行为对齐 Docker Compose——不用再为测试库自定义 Dockerfile。

**环境免部署记录（2026-03）**：`environment: {name: staging, deployment: false}`——只用环境的 secrets/变量管理而不自动创建 deployment 记录，适合 Terraform plan、预检查等场景；自定义部署保护规则下不可用。

**case 函数（2026-01）**：表达式新增 `case`（SQL 风格多分支），替代一长串 `&&`/`||` 短路表达式（详见 03 篇）。

**新语法演进的意义**：把 2025-2026 的语法新增放在一起看，能读出 GitHub 的产品意图——并行步骤吸收"脚本里手搓的 `&` 后台进程"，`$/` 吸收"相对路径引用 + checkout 前置"的样板，case 吸收"if-else 长链"，时区吸收"cron 换算"：**声明式语言在持续吸收脚本化样板，工作流正在从"能跑"走向"好写"**。对工程师的启示是双重的：其一，持续跟进 Changelog（本体系 00 篇的版本窗口就是追踪锚点），新语法往往比老技巧更省事；其二，模板与团队规范要随语法演进更新——2025 年的模板可能还在教人用老语法绕路。

## 5. YAML 高级特性与错误处理语义

**YAML 锚点与扩展（2025 GA）**：工作流 YAML 支持 `&anchor` 定义复用块与 `<<:` 合并——"多 job 共享 runs-on 与缓存配置"的场景不再复制粘贴：

```yaml
x-base: &base
  runs-on: ubuntu-latest
  timeout-minutes: 15
jobs:
  lint: { <<: *base, steps: [...] }
  test: { <<: *base, needs: lint, steps: [...] }
```

注意：锚点必须在顶层 `x-` 键（自定义键）下定义，且**合并发生在 YAML 解析层，不是表达式层**——锚点内不能引用表达式求值结果以外的动态内容；过度使用锚点会降低可读性，两个以上 job 复用才值得。

**错误处理语义（job/step 级）**：`continue-on-error` 与 `if: failure()` 的配合是错误处理的核心——`continue-on-error: true` 的步骤失败后**后续步骤继续跑**（`success()` 仍为真），适合"上报但不阻塞"；真正要"失败后执行补救"用 `if: failure()` 步骤（如发通知、上传诊断日志）。**默认 shell 语义**：Linux/macOS 用 `bash -e`（命令失败即退出），Windows 用 PowerShell；`defaults.run.shell` 可全局覆盖，`run: |` 块内要容忍失败的命令显式加 `|| true` 或 `set +e`——理解"shell 退出码 → 步骤失败"的传导链，是调试"明明报错还继续跑/明明没报错却失败"两类的钥匙。

## 6. 校验与调试工具链

- **actionlint**：静态校验 YAML 语法、字段拼写、表达式错误——CI 里跑 `actionlint` 是"工作流本身的质量门禁"，推荐进 pre-commit 钩子。
- **VS Code / Web 编辑器**：2026-01 起官方编辑器增强已覆盖全 IDE——表达式自动补全、schema 校验、悬停文档、inline cron 提示；跳过的 job 日志会显示原始表达式与展开后的运行时值，调试 if 条件不再靠猜。
- **调试三板斧**：`ACTIONS_STEP_DEBUG=true`（step 级调试日志）、`ACTIONS_RUNNER_DEBUG=true`（runner 级）、手动 `workflow_dispatch` + 最小复现——从大到小定位。

**"工作流即代码"的工程位置**：工作流文件与业务代码同仓、同 review、同测试——它是仓库的"可执行文档"：新人看 `ci.yml` 就懂"这个项目怎么验证、怎么发布"。因此语法层还有一个隐性要求——**可读性优先**：文件名按触发域命名（`ci.yml`、`release.yml`），job 名体现阶段语义（`lint`、`test`、`deploy-prod`），step 名写清"做什么"（`run: mvn test` 的 name 应写明"运行单元测试"）；YAML 超过 200 行就要拆（抽 reusable 或拆文件），这是"工作流会被长期维护"的前提。记住：流水线是团队公共资产，不是个人脚本。

**语法学习的边界**：本文覆盖的字段覆盖了 95% 生产场景，其余字段（`secrets` 在 job 级的继承、`permissions` 的细粒度 scope、`strategy` 的失败策略变体）用到时查官方文档即可——语法是"查得到的东西"，不必背全；真正需要内化的只有三条：**顶层四键（on/jobs/permissions/concurrency）是每个工作流的骨架，job 三要素（runs-on/needs/timeout）是每个 job 的底线，step 两件事（uses 还是 run、id 要不要）是每步的决策**——把这三条写进自己的模板，语法细节随用随查、自然沉淀。

> 🎯 核心要点：语法层的价值在于"少踩坑"——concurrency 防重复、timeout 防烧钱、`${{ }}` vs `$VAR` 防混淆、GITHUB_OUTPUT 传值。2026 新语法里并行步骤与 `$/` 自引用是生产力提升最明显的两个，优先用起来。

---

**下一模块**：[03-表达式与上下文](./03-表达式与上下文.md) / **返回总览**：[00-GitHub Actions 知识体系总览](./00-GitHub%20Actions%20知识体系总览.md)

**参考来源**：

- [Actions steps can now be run in parallel | GitHub Changelog 2026-06-25](https://github.blog/changelog/2026-06-25-actions-steps-can-now-be-run-in-parallel/)
- [Reference same-repository actions with self-repository syntax | GitHub Changelog 2026-07-30](https://github.blog/changelog/2026-07-30-reference-same-repository-actions-with-self-repository-syntax/)
- [Workflow syntax for GitHub Actions | GitHub Docs](https://docs.github.com/en/actions/reference/workflow-syntax-for-github-actions)
- [Actions runner docs（GITHUB_OUTPUT 等）| GitHub Docs](https://docs.github.com/en/actions/reference/environment-variables)
