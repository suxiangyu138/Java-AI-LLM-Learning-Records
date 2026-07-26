# Git 托管平台对比与实战

## Git 生态第二层：代码托管平台

写好的代码总要有个地方存放和协作——这就是托管平台的角色。它们不只是"存代码的网盘"，更提供了 Code Review、CI/CD、项目管理等全套协作能力。

---

## 1. 三大玩家速览

| | GitHub | GitLab | Gitee |
|------|--------|--------|-------|
| **归属** | 微软 | GitLab Inc. | OSChina |
| **定位** | 全球开源社区中心 | DevOps 一体化平台 | 国内开发者社区 |
| **免费私有仓库** | ✅ 无限 | ✅ 无限 | ✅ 无限 |
| **CI/CD 分钟数** | 2000 分钟/月 | 400 分钟/月 | 有限免费 |
| **国内访问** | ⚠️ 不稳定 | ⚠️ 偶尔慢 | ✅ 飞快 |
| **开源氛围** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **企业功能** | GitHub Enterprise | GitLab EE（功能最强） | Gitee Enterprise |

---

## 2. GitHub — 开源世界的"社交网络"

### 核心概念

```text
Repository (仓库)
  ├── Issues (问题跟踪)
  ├── Pull Requests (代码审查)
  ├── Actions (CI/CD)
  ├── Projects (看板)
  ├── Wiki (文档)
  ├── Discussions (社区讨论)
  └── Releases (版本发布)
```

### 特色功能

**GitHub Actions — 内置 CI/CD**

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - run: npm ci && npm test
```

**GitHub CLI (gh) — 命令行操作一切**

```bash
# 安装: winget install GitHub.cli  (Windows)
# 或: brew install gh              (Mac)

gh auth login                    # 登录
gh repo view owner/repo          # 查看仓库信息
gh issue list                    # 列出 Issues
gh pr create --title "feat: xxx" --body "## Summary..."
gh pr checkout 123               # 本地检出 PR 分支
gh pr review --approve           # 批准 PR
gh run watch                     # 实时查看 CI 运行
```

**GitHub Pages — 免费静态网站**

```bash
# 设置 → Pages → Source: Deploy from a branch → main → /docs
# 你的网站就在 https://username.github.io/repo/ 上线了
```

### Pull Request 生态

GitHub 最强的地方：

- **Draft PR** — 还没写完，但想提前展示给团队
- **Review required** — 必须 N 人 approve 才能 merge
- **CODEOWNERS** — 指定谁审查哪些文件
- **Suggested changes** — Reviewer 可以直接在 PR 中写代码建议，一键采纳
- **Auto-merge** — CI 通过后自动合并

```text
# .github/CODEOWNERS
# 指定目录的审查责任人
src/auth/*     @team-lead
*.yml          @devops-team
docs/*         @tech-writer
```

---

## 3. GitLab — DevOps 一体化

### 最大优势：一个平台搞定全部

```text
GitLab 覆盖的 DevOps 环节:
Plan → Code → Build → Test → Release → Deploy → Monitor
  ↑      ↑      ↑       ↑       ↑        ↑         ↑
 Issue  MR    CI/CD  Pipeline  Registry  K8s     Metrics
```

不需要 GitHub + Jenkins + Jira + Artifactory 拼凑，GitLab 一个就够了。

### 核心概念

```text
Group (组)
  └── Project (项目)
        ├── Issues
        ├── Merge Requests (MR)
        ├── CI/CD (.gitlab-ci.yml)
        ├── Container Registry
        ├── Package Registry
        ├── Pages
        └── Wiki
```

### GitLab CI 示例

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - deploy

variables:
  DOCKER_IMAGE: registry.gitlab.com/$CI_PROJECT_PATH

build:
  stage: build
  script:
    - docker build -t $DOCKER_IMAGE:$CI_COMMIT_SHORT_SHA .
    - docker push $DOCKER_IMAGE:$CI_COMMIT_SHORT_SHA

test:
  stage: test
  script:
    - npm ci
    - npm test
    - npm run lint

deploy:
  stage: deploy
  script:
    - kubectl set image deployment/app app=$DOCKER_IMAGE:$CI_COMMIT_SHORT_SHA
  only:
    - main
```

### GitLab Flow 环境追踪

```text
main → staging → production
  │       │          │
  │       └─ 自动部署  └─ 手动审批后部署
  └─ feature 分支合入
```

---

## 4. Gitee（码云）— 国内首选

### 为什么用 Gitee？

| 场景 | Gitee 的优势 |
|------|------------|
| 国内访问 | 速度快，不翻墙 |
| 高校教育 | 免费 Gitee 高校版，课程管理 |
| 企业合规 | 数据在国内服务器 |
| 开源项目 | Gitee GVP（优秀开源项目）扶持 |

### 特色功能

- **Gitee Go** — 国内的 CI/CD 服务
- **Gitee Pages** — 免费静态网站（需实名认证）
- **Gitee Scan** — 代码质量分析
- **高校版** — 专门的教学管理功能

---

## 5. 三平台核心操作对比

| 操作 | GitHub | GitLab | Gitee |
|------|--------|--------|-------|
| 克隆仓库 | `git clone` + URL | `git clone` + URL | `git clone` + URL |
| 创建代码审查 | **Pull Request** | **Merge Request** | **Pull Request** |
| CI/CD 配置 | `.github/workflows/` | `.gitlab-ci.yml` | `.gitee/` (Gitee Go) |
| Wiki | 独立 tab | 独立 tab | 独立 tab |
| 私有仓库 | ✅ | ✅ | ✅ |
| 项目看板 | Projects | Boards | 看板 |
| 命令行工具 | `gh` CLI | `glab` CLI | — |
| 包管理 | GitHub Packages | GitLab Registry | — |
| 免费 Pages | ✅ | ✅ | ✅ (需实名) |

---

## 6. 如何选择？

```text
┌────────────────────────────────────────────────────┐
│                Git 托管平台选择指南                  │
├──────────────────┬─────────────────────────────────┤
│ 个人开源项目      │ GitHub ⭐（全球开发者都在用）     │
│ 国内项目/稳定访问  │ Gitee（不用翻墙，速度飞快）      │
│ 企业私有部署      │ GitLab（自建或SaaS，功能最全）   │
│ 全栈 DevOps       │ GitLab（一个平台全链路）         │
│ 追求社区和生态    │ GitHub（Actions 市场、Copilot）  │
│ 学生/教学         │ Gitee 高校版                    │
│ 同时用两个        │ GitHub(开源) + Gitee(镜像同步)   │
└──────────────────┴─────────────────────────────────┘
```

### 常见策略：双平台同步

```bash
# 主仓库在 GitHub，Gitee 做镜像
git remote add github git@github.com:user/repo.git
git remote add gitee git@gitee.com:user/repo.git

# 推送时两个都推
git push github main
git push gitee main

# 或者配别名一次推两个
git remote add all git@github.com:user/repo.git
git remote set-url --add --push all git@github.com:user/repo.git
git remote set-url --add --push all git@gitee.com:user/repo.git
git push all main
```

---

## 7. 平台不可不知的配置

### 分支保护规则

无论哪个平台，`main`/`master` 分支都应该：

```text
✅ Require pull request before merging
✅ Require approvals (至少1人)
✅ Require status checks to pass (CI 通过)
✅ Require conversation resolution
❌ 禁止 force push
❌ 禁止直接 push
```

### 安全扫描

```text
GitHub: Settings → Code security → Dependabot / Code scanning
GitLab: Security & Compliance → Vulnerability Report
Gitee: 服务 → Gitee Scan
```

---

> 上一篇：[12-Git常见陷阱与排错指南](12-Git常见陷阱与排错指南.md)
> 下一篇：[14-Git桌面客户端完全指南](14-Git桌面客户端完全指南.md)
