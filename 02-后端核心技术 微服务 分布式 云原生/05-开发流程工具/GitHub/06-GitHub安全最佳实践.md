# 06 - GitHub 安全最佳实践

> 代码安全从仓库配置开始——分支保护、CODEOWNERS、密钥扫描、Dependabot、安全公告。GitHub 内置了完整的安全能力，关键在于配置和使用。

---

## 目录

1. [仓库级别安全配置](#1-仓库级别安全配置)
2. [分支保护规则](#2-分支保护规则)
3. [CODEOWNERS 与强制审查](#3-codeowners-与强制审查)
4. [依赖安全管理（Dependabot）](#4-依赖安全管理dependabot)
5. [密钥扫描（Secret Scanning）](#5-密钥扫描secret-scanning)
6. [代码扫描（Code Scanning / CodeQL）](#6-代码扫描code-scanning--codeql)
7. [安全公告（Security Advisory）](#7-安全公告security-advisory)
8. [GitHub 账户安全](#8-github-账户安全)
9. [企业级安全功能](#9-企业级安全功能)
10. [常见面试题](#10-常见面试题)

---

## 1. 仓库级别安全配置

### 1.1 Security 概览页

```
Settings → Security → Security overview
┌─────────────────────────────────────────────────────────┐
│ Security Coverage                                       │
│                                                          │
│ Code scanning     ✅ Enabled (CodeQL)                    │
│ Secret scanning   ✅ Enabled (Push protection + Alerts)  │
│ Dependabot        ✅ Enabled (Alerts + Security updates) │
│ Branch protection ✅ Enabled (main branch)               │
│                                                          │
│ Risk Summary:                                            │
│   🔴 3 Critical alerts                                  │
│   🟡 12 High alerts                                     │
│   🔵 45 Medium alerts                                   │
└─────────────────────────────────────────────────────────┘
```

### 1.2 安全配置检查清单

| 配置项 | 路径 | 推荐值 |
|--------|------|--------|
| 分支保护 | Settings → Branches | main 分支强制保护 |
| CODEOWNERS | `.github/CODEOWNERS` | 核心目录需专人审批 |
| Dependabot alerts | Settings → Security | ✅ 开启 |
| Dependabot security updates | Settings → Security | ✅ 开启（自动 PR） |
| Secret scanning | Settings → Security | ✅ 开启 + Push 保护 |
| Code scanning | Settings → Security | ✅ 开启（CodeQL） |
| 删除分支限制 | Branch protection | ✅ 禁止删除 |
| Force Push | Branch protection | ✅ 禁止 |
| Signed commits | Branch protection | 推荐开启 |
| 私有仓库 | Settings → General | 按需选择 |

---

## 2. 分支保护规则

### 2.1 完整保护配置

```yaml
# Settings → Branches → Add branch protection rule

Branch name pattern: main

# ═══ 保护规则 ═══
☑ Require a pull request before merging
  ☑ Require approvals: 2              # 至少 2 人审批
  ☑ Dismiss stale pull request approvals  # 新 commit 后旧审批失效
  ☑ Require review from Code Owners     # CODEOWNERS 必须审批

☑ Require status checks to pass before merging
  ☑ Require branches to be up to date  # PR 分支需与 main 同步
  Status checks:                        # 必须通过的 CI Job
    - "build (ubuntu-latest)"
    - "test (ubuntu-latest)"
    - "SonarCloud Code Analysis"

☑ Require conversation resolution before merging  # 所有讨论解决

☑ Do not allow bypassing the above settings  # 管理员也不例外

# ═══ 禁止操作 ═══
☑ Lock branch                          # 只读，禁止任何推送
☑ Do not allow bypassing
☐ Allow force pushes                   # ❌ 禁止
☐ Allow deletions                      # ❌ 禁止
```

### 2.2 保护规则的层级

```
分支保护规则的优先级：

1. 组织级规则（Organization-wide ruleset）
   → 对所有仓库生效，最小粒度（如 "所有仓库的 main 分支至少 1 个审批"）

2. 仓库级规则（Repository rule）
   → 对本仓库生效，可细化

3. 规则集（Ruleset）
   → 可组合的规则集合，可应用到多个仓库/分支

特殊角色豁免：
  - Repository Admin 默认可以绕过分支保护
  - 勾选 "Do not allow bypassing" 后连 Admin 也受限
```

---

## 3. CODEOWNERS 与强制审查

### 3.1 CODEOWNERS 文件

```markdown
# .github/CODEOWNERS

# 语法：<文件/目录模式> @用户名或团队

# ═══ 全局所有者 ═══
*                       @team-leads

# ═══ 目录级别 ═══
/frontend/              @frontend-team
/backend/api/           @backend-senior @backend-lead
/infrastructure/        @devops-team
/docs/                  @tech-writer

# ═══ 文件类型 ═══
*.java                  @java-experts
*.js                    @frontend-team
*.sql                   @dba

# ═══ 精确文件 ═══
/Dockerfile             @devops-team
/.github/workflows/     @devops-team @security-team
/pom.xml                @backend-lead

# ═══ 安全敏感文件 ═══
/src/main/resources/application*.yml  @security-team
```

### 3.2 审批规则示例

```
场景：PR 修改了 /backend/api/OrderService.java 和 /pom.xml

CODEOWNERS 解析：
  /backend/api/ → 需要 @backend-senior 或 @backend-lead 审批
  *.java       → 需要 @java-experts 审批
  /pom.xml     → 需要 @backend-lead 审批

Branch Protection "Require review from Code Owners" →
  这三组中每组至少 1 人审批，PR 才能合并
```

---

## 4. 依赖安全管理（Dependabot）

### 4.1 Dependabot 三大功能

```
1. Dependabot Alerts（告警）
   扫描依赖文件 → 发现已知漏洞（CVE） → 生成告警
   支持的包管理：Maven, Gradle, npm, pip, Docker, Go modules...

2. Dependabot Security Updates（安全更新）
   告警生成后 → 自动创建 PR 升级到修复版本

3. Dependabot Version Updates（版本更新）
   定期（每周）检查依赖是否有新版本 → 自动创建 PR 升级
```

### 4.2 Dependabot 配置

```yaml
# .github/dependabot.yml
version: 2
updates:

  # ═══ Maven 依赖更新 ═══
  - package-ecosystem: "maven"
    directory: "/"                    # pom.xml 所在目录
    schedule:
      interval: "weekly"              # daily | weekly | monthly
      day: "monday"
      time: "09:00"
      timezone: "Asia/Shanghai"
    open-pull-requests-limit: 10      # 同时开启的 PR 上限
    reviewers:
      - "backend-team"
    assignees:
      - "backend-lead"
    labels:
      - "dependencies"
      - "java"
    # 忽略某些依赖
    ignore:
      - dependency-name: "com.example:legacy-lib"
        versions: [">=1.0"]

  # ═══ Docker 镜像更新 ═══
  - package-ecosystem: "docker"
    directory: "/"
    schedule:
      interval: "weekly"

  # ═══ GitHub Actions 更新 ═══
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
```

### 4.3 Dependabot 告警处理流程

```
1. 收到 Dependabot Alert（邮件/仓库 Security 页面）
   ↓
2. 评估影响：是否影响生产环境？攻击复杂度如何？
   ↓
3. 处理：
   - 有安全更新 PR → Review → 合并
   - 无自动 PR → 手动升级依赖版本
   - 误报/不影响 → Dismiss（写明原因）
   ↓
4. 部署 + 验证
```

---

## 5. 密钥扫描（Secret Scanning）

### 5.1 两种扫描模式

```
1. Secret scanning alerts（告警模式）
   - 扫描已推送到仓库的代码
   - 发现密钥/Token 后告警
   - GitHub 自动吊销对应服务的 Token

2. Push protection（推送保护，⭐ 推荐）
   - 在 push 之前拦截
   - 检测到密钥 → 阻止推送
   - 支持 200+ 服务商（AWS, Azure, Google Cloud, OpenAI...）
```

```bash
# Push 被拦截时的提示：
git push origin main
# remote: error: GH013: Repository rule violations found for refs/heads/main.
# remote: - GITHUB PUSH PROTECTION
# remote:   — GitHub Personal Access Token found:
# remote:     src/main/resources/application.yml:15
# remote:     (token: ghp_xxxxxxxxxxxxxxxxxxxx)
# remote:
# remote:   ! Push blocked: Cannot push secrets to public repositories.

# 解决方案：
# 1. 撤销 commit → 删除密钥
# 2. 如果密钥已泄露 → 立即吊销 + 重新生成
# 3. 使用环境变量或 GitHub Secrets 替代硬编码
```

### 5.2 防止密钥泄露的最佳实践

```java
// ❌ 绝对不要在代码中硬编码
String apiKey = "sk-abc123xyz456";  // 危险！
String dbPassword = "admin123";     // 危险！

// ✅ 使用环境变量
String apiKey = System.getenv("OPENAI_API_KEY");

// ✅ 使用配置文件 + .gitignore
// application-local.yml (加入 .gitignore)
String apiKey = config.getApiKey();

// ✅ GitHub Actions 中使用 Secrets
// ${{ secrets.OPENAI_API_KEY }}

// ✅ 使用密钥管理服务
// AWS Secrets Manager / Azure Key Vault / HashiCorp Vault
```

---

## 6. 代码扫描（Code Scanning / CodeQL）

### 6.1 CodeQL 是什么

> CodeQL 是 GitHub 的语义代码分析引擎——将代码视为数据，编写查询发现漏洞模式。支持 Java, C/C++, C#, Python, JavaScript/TypeScript, Go, Ruby 等。

```yaml
# .github/workflows/codeql.yml
name: "CodeQL"

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  schedule:
    - cron: '30 1 * * 0'   # 每周日 UTC 1:30

jobs:
  analyze:
    name: Analyze (${{ matrix.language }})
    runs-on: ubuntu-latest
    permissions:
      security-events: write
      actions: read
      contents: read

    strategy:
      fail-fast: false
      matrix:
        language: ['java', 'javascript']

    steps:
      - uses: actions/checkout@v4

      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: ${{ matrix.language }}

      - name: Autobuild  # 自动检测构建系统
        uses: github/codeql-action/autobuild@v3

      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v3
```

### 6.2 CodeQL 能发现什么

```
Java 常见漏洞检测：
├── SQL 注入（动态拼接 SQL）
├── 路径遍历（../ 未过滤）
├── XSS（未转义的用户输入输出到页面）
├── 不安全的反序列化
├── 硬编码密钥
├── 资源泄漏（未关闭的 Stream/Connection）
├── 空指针解引用
└── 线程安全问题
```

---

## 7. 安全公告（Security Advisory）

### 7.1 安全漏洞披露流程

```
Security Advisory 完整流程：

1. 发现者 → 通过 "Report a vulnerability" 私密报告
2. 维护者 → 创建 Draft Security Advisory（私密）
3. 维护者 → 在私密 Fork 中开发修复
4. 维护者 → 请求 CVE 编号（GitHub 自动分配）
5. 修复完成 → 发布 Advisory
6. 发布同时：
   - 创建公开的 Advisory
   - CVE 编号公开
   - Dependabot 通知所有依赖此包的仓库
   - GitHub 自动创建 Release
```

```bash
# 通过 gh CLI 管理 Advisory
gh advisory list --repo user/repo

# 创建私密 Advisory
gh advisory create \
  --repo user/repo \
  --severity high \
  --cve-id "CVE-2024-1234" \
  --summary "SQL injection in search endpoint"
```

---

## 8. GitHub 账户安全

### 8.1 必做安全设置

```
✅ 1. 启用双因素认证（2FA）
   Settings → Password and authentication → Enable 2FA
   - Authenticator App（推荐，TOTP）
   - Security Key（最安全，硬件密钥如 YubiKey）
   - SMS（不推荐，SIM Swap 风险）

✅ 2. 使用 SSH Key 替代密码
   ssh-keygen -t ed25519 -C "email@example.com"
   gh ssh-key add ~/.ssh/id_ed25519.pub

✅ 3. 创建 Personal Access Token（代替密码用于 CLI/API）
   Settings → Developer settings → Personal access tokens → Fine-grained
   ✅ 精确权限（只给需要的仓库和权限）
   ✅ 设置过期时间（7天/30天/自定义）
   ❌ 不要用 Classic Token（权限太大）

✅ 4. 开启 GPG 签名（防 commit 伪造）
   gpg --gen-key
   git config --global user.signingkey YOUR_KEY_ID
   git config --global commit.gpgsign true

✅ 5. 审查授权的 OAuth Apps 和 GitHub Apps
   Settings → Applications → Authorized OAuth Apps
   定期清理不再需要的授权
```

### 8.2 GPG Commit 签名

```bash
# GPG 签名后，GitHub 上显示 "Verified" 绿标

# 1. 生成 GPG 密钥
gpg --full-generate-key
# 选择：RSA and RSA / 4096 bits
# 输入：姓名 + 邮箱（必须与 GitHub 邮箱一致）

# 2. 导出公钥
gpg --list-secret-keys --keyid-format LONG
gpg --armor --export YOUR_KEY_ID

# 3. 添加到 GitHub
# Settings → SSH and GPG keys → New GPG key → 粘贴公钥

# 4. 配置 Git
git config --global user.signingkey YOUR_KEY_ID
git config --global commit.gpgsign true

# 5. 签名 commit
git commit -S -m "feat: add login"
# -S 表示签名（配置 gpgsign=true 后自动签名）
```

---

## 9. 企业级安全功能

| 功能 | Free | Team | Enterprise | 说明 |
|------|------|------|------------|------|
| Dependabot Alerts | ✅ | ✅ | ✅ | 依赖漏洞告警 |
| Dependabot Security Updates | ✅ | ✅ | ✅ | 自动修复 PR |
| Secret Scanning Alerts | ✅ | ✅ | ✅ | 已推送代码扫描 |
| Push Protection | ✅ | ✅ | ✅ | 推送前拦截 |
| CodeQL (Code Scanning) | ✅ | ✅ | ✅ | 代码漏洞扫描 |
| Branch Protection | ✅ | ✅ | ✅ | 分支保护规则 |
| CODEOWNERS | ❌ | ✅ | ✅ | 代码所有权 |
| Required Reviewers | ❌ | ✅ | ✅ | 强制审批 |
| SAML SSO | ❌ | ❌ | ✅ | 单点登录 |
| Audit Log | ❌ | ❌ | ✅ | 审计日志 |
| IP Allow List | ❌ | ❌ | ✅ | IP 白名单 |

---

## 10. 常见面试题

### Q1：GitHub 分支保护规则通常包含哪些配置？

> 必须通过 PR 合并、需要 N 人 Approve、CI 状态检查通过、CODEOWNERS 审批、禁止 Force Push、禁止删除分支。详见第2节。

### Q2：Dependabot 的三种功能分别是什么？

> Alerts（告警：发现已知漏洞）、Security Updates（自动 PR 升级修复版本）、Version Updates（定期 PR 升级到最新版）。详见第4节。

### Q3：Secret Scanning Push Protection 的原理？

> 在 Git Push 阶段实时扫描推送内容，匹配 200+ 服务商的密钥模式（Token 前缀+格式），匹配成功则阻止推送并告警。详见第5节。

### Q4：如何防止 commit 伪造？

> 使用 GPG 密钥签名 commit，GitHub 上显示 Verified 标签。配合分支保护规则可以要求所有 commit 必须签名。详见第8.2节。

### Q5：Security Advisory 的发布流程是什么？

> 私密报告 → 创建 Draft Advisory → 私密 Fork 修复 → 请求 CVE → 修复完成 → 公开发布 → Dependabot 自动通知依赖方。详见第7节。
