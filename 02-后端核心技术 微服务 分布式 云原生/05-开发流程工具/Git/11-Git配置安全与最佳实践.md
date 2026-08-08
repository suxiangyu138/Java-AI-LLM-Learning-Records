# 11-Git 配置安全与最佳实践
> 规范是为了减少混乱、自动化是为了减少失误、安全是为了防范灾难——GPG/SSH 签名、敏感信息清理、分支保护、团队规范模板

## 📚 目录
1. [安全全景：四道防线](#1-安全全景四道防线)
2. [GPG 签名提交](#2-gpg-签名提交)
3. [SSH 签名（Git 2.34+）](#3-ssh-签名git-234)
4. [敏感信息防泄露](#4-敏感信息防泄露)
5. [历史清理：filter-repo / BFG / git-secrets](#5-历史清理filter-repo--bfg--git-secrets)
6. [分支保护规则](#6-分支保护规则)
7. [推荐完整 .gitconfig](#7-推荐完整-gitconfig)
8. [分支管理最佳实践](#8-分支管理最佳实践)
9. [提交规范最佳实践](#9-提交规范最佳实践)
10. [协作流程最佳实践](#10-协作流程最佳实践)
11. [多环境与微服务最佳实践](#11-多环境与微服务最佳实践)
12. [团队 Git 规范模板](#12-团队-git-规范模板可直接复制)
13. [核心要点](#13-核心要点)
14. [参考来源](#14-参考来源)

## 1. 安全全景：四道防线

| 防线 | 手段 | 防什么 |
|------|------|--------|
| ① 提交前预防 | `.gitignore` + pre-commit hook（detect-private-key） | 密钥进仓库 |
| ② 提交身份 | GPG/SSH 签名（GitHub "Verified"） | 身份伪造 |
| ③ 仓库权限 | 分支保护 + 平台安全套件（Dependabot/Secret scanning） | 越权与漏洞 |
| ④ 泄露补救 | filter-repo / BFG + revoke 密钥 | 已泄露的历史 |

> 🎯 **核心要点**：安全策略必须在**服务端 enforce**——本地 hook 可被 `--no-verify` 跳过，分支保护与 Secret scanning 不能。

## 2. GPG 签名提交

```bash
# 1. 生成密钥
gpg --full-generate-key                          # RSA and RSA, 4096 bits, 设置过期时间
# 2. 列出密钥 → rsa4096/3AA5C34371567BD2 为 key ID
gpg --list-secret-keys --keyid-format LONG
# 3. 配置 Git
git config --global user.signingkey 3AA5C34371567BD2
git config --global commit.gpgsign true          # 所有 commit 自动签名
git config --global tag.gpgsign true             # 所有 tag 自动签名
# 4. 导出公钥 → GitHub → Settings → SSH and GPG keys → New GPG key
gpg --armor --export 3AA5C34371567BD2
# 5. 验证
git log --show-signature
git verify-commit HEAD
git verify-tag v1.0.0
```

> 💡 GitHub 显示 "Verified ✓"——即使用户名被伪造，GPG 签名无法伪造。企业高安全要求：平台开启 "Require signed commits"。

## 3. SSH 签名（Git 2.34+）

SSH 签名是 GPG 的轻量替代（复用已有 SSH key）：

```bash
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
echo "$(git config user.email) $(cat ~/.ssh/id_ed25519.pub)" >> ~/.ssh/allowed_signers
git config --global gpg.ssh.allowedSignersFile ~/.ssh/allowed_signers
```

## 4. 敏感信息防泄露

### 4.1 预防（.gitignore 先行）

```bash
echo ".env" >> .gitignore
echo "*.pem" >> .gitignore
echo "credentials.json" >> .gitignore
```

### 4.2 提交模板与自检清单

```text
提交前检查清单：
□ 编译通过：mvn compile 成功
□ 测试通过：mvn test 全绿
□ 无调试代码：没有 System.out.println / console.log
□ 无敏感信息：没有密码/Token/密钥
□ 检查文件：没有 target/、.idea/ 等
□ commit message 符合规范
```

### 4.3 密钥管理规范（多环境配置）

```text
配置文件分层（由通用到专用）：
  application.yml              → 通用配置，提交到 Git
  application-dev.yml          → 开发环境，可提交
  application-local.yml        → 本地配置，.gitignore 忽略
  application-prod.yml         → 生产配置，可提交（不含密钥）

密钥管理：
  ❌ 密钥写在 application-prod.yml 中
  ✅ 密钥存在环境变量/K8s Secret/Vault 中，通过 ${DB_PASSWORD} 引用
```

## 5. 历史清理：filter-repo / BFG / git-secrets

### 5.1 git-filter-repo（推荐，比 filter-branch 快 10-1000 倍）

```bash
pip install git-filter-repo

git filter-repo --path .env --invert-paths                  # 从完整历史删除文件
git filter-repo --path secrets/ --invert-paths              # 删除目录
git filter-repo --replace-text <(echo "old-password==>REDACTED")  # 替换所有历史中的密码
git filter-repo --strip-blobs-bigger-than 10M               # 删除大于 10MB 的文件
git push --force --all                                      # 清理后强制推送
git push --force --tags
```

> ⚠️ 改写历史！确保团队知晓——**所有协作者需要重新 clone**。

### 5.2 BFG Repo-Cleaner

```bash
# 下载: https://rtyley.github.io/bfg-repo-cleaner/
java -jar bfg.jar --strip-blobs-bigger-than 100M repo.git
java -jar bfg.jar --delete-files "*.zip" repo.git
java -jar bfg.jar --replace-text passwords.txt repo.git
```

### 5.3 git-secrets（持续扫描）

```bash
# 安装: https://github.com/awslabs/git-secrets
git secrets --scan                                  # 扫描
git secrets --register-aws --global                 # 全局注册 AWS 规则
git secrets --install ~/.git-templates/git-secrets
git config --global init.templateDir ~/.git-templates/git-secrets
git secrets --add 'password\s*=\s*.+'               # 自定义规则
git secrets --scan-history                          # 扫描历史
```

### 5.4 旧工具 filter-branch（仅遗留场景）

```bash
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch .env" \
  --prune-empty --tag-name-filter cat -- --all
```

## 6. 分支保护规则

```text
main / develop 保护（GitHub/GitLab 平台配置）：
  ✅ 禁止直接 push（必须通过 PR/MR 合并）
  ✅ Require approvals（至少 1 人，main 建议 2 人）
  ✅ Require status checks（CI 全部通过）
  ✅ Require conversation resolution
  ✅ 分支必须最新（禁止落后于目标分支）
  ✅ Require signed commits（高安全要求）
  ✅ 禁止 force push
  ✅ Include administrators（管理员也受约束）
  ⚠️ 可选：Require linear history / Require review from Code Owners
```

| 角色 | 可创建分支 | 可合并到 | 可删除分支 | 可修改保护规则 |
|------|:---:|:---:|:---:|:---:|
| Developer | feature/fix/* | — | 自己的 feature | — |
| Tech Lead | 全部 | develop | feature/fix | — |
| Maintainer | 全部 | main/release | 全部 | ✅ |

## 7. 推荐完整 .gitconfig

```ini
[user]
    name = Your Name
    email = your.email@example.com
    signingkey = 3AA5C34371567BD2

[init]
    defaultBranch = main

[core]
    editor = code --wait
    fscache = true
    autocrlf = input              # Windows 上设为 true
    untrackedCache = true

[commit]
    gpgsign = true

[tag]
    gpgsign = true

[pull]
    rebase = true                 # git pull 默认 rebase

[fetch]
    parallel = 8
    prune = true                  # fetch 时自动清理远程已删分支

[rebase]
    autoStash = true              # rebase 前自动 stash

[diff]
    algorithm = histogram
    colorMoved = zebra

[merge]
    conflictstyle = diff3         # 显示共同祖先版本

[rerere]
    enabled = true                # 记住冲突解决方案

[gc]
    writeCommitGraph = true

[alias]
    st = status
    lg = log --oneline --graph --all -20
    co = checkout
    sw = switch
    unstage = restore --staged .
    undo = reset --soft HEAD~1
    amend = commit --amend --no-edit
```

| 配置项 | 效果 |
|--------|------|
| `pull.rebase true` | `git pull` 默认用 rebase |
| `fetch.prune true` | fetch 时自动清理远程已删分支 |
| `rebase.autoStash true` | rebase 前自动 stash |
| `merge.conflictstyle diff3` | 冲突时显示三栏（当前/祖先/对方） |
| `rerere.enabled true` | 自动记住冲突解决 |
| `commit.gpgsign true` | 所有 commit 自动签名 |
| `core.ignorecase false` | 大小写敏感（Mac/Windows 默认不敏感！） |

## 8. 分支管理最佳实践

```text
长期分支（永久存在）:
  main          → 生产环境代码，始终可部署
  develop       → 开发主线，所有 feature 的合并目标

临时分支（合并后删除）:
  feature/*     → 新功能开发，从 develop 拉，合并回 develop
  hotfix/*      → 生产紧急修复，从 main 拉，合并回 main+develop
  release/*     → 发布准备，从 develop 拉，合并回 main+develop

Java 后端多环境对应:
  main      → 生产环境
  develop   → 测试环境
  release/* → 预发布环境
```

| 高频陷阱 | 后果 | 正确做法 |
|----------|------|----------|
| 在公共分支上 rebase | 团队历史混乱 | 仅 rebase 个人分支 |
| 直接 push 到 main | 绕过 CR 和 CI | 开启分支保护 |
| 长期不合并的分支 | 巨量冲突 | feature 分支存活 ≤ 3 天 |
| 忘记删除已合并分支 | 仓库臃肿 | CI 自动删除 / 定期清理 |
| detached HEAD 下提交 | 代码可能丢失 | 立即 `git switch -c temp` |
| hotfix 未合并回 develop | 下次发布重复踩坑 | hotfix 合并 main 和 develop |
| force push 公共分支 | 别人基于旧 commit 的工作全丢 | 仅 `--force-with-lease` 且仅自己的分支 |

## 9. 提交规范最佳实践

### 9.1 Conventional Commits

```text
<type>(<scope>): <subject>

type: feat | fix | docs | style | refactor | test | chore | perf | ci
scope: 影响模块（可选）
subject: ≤50 字符，现在时，首字母小写
```

| type | 何时使用 | 示例 |
|------|----------|------|
| `feat` | 新功能 | `feat(user): 添加用户登录` |
| `fix` | Bug 修复 | `fix(order): 修复金额计算精度` |
| `refactor` | 重构（无功能变更） | `refactor(common): 提取Token工具类` |
| `test` | 测试 | `test(user): 补充登录失败测试` |
| `docs` | 文档 | `docs: 更新API文档` |
| `chore` | 构建/依赖/工具 | `chore: 升级Spring Boot到3.2` |
| `perf` | 性能优化 | `perf(order): 优化查询索引` |
| `ci` | CI/CD | `ci: 添加代码覆盖率检查` |

```text
✅ 好的粒度：
  feat(user): 添加用户注册接口
  feat(user): 添加用户名唯一性校验
  test(user): 补充注册功能单元测试

❌ 太大：feat: 完成用户模块（改了20个文件，无法 review）
❌ 太小：fix: typo / fix: another typo（应该 squash 或 amend）
```

### 9.2 与 Issue 关联

```bash
git commit -m "feat(user): 实现用户登录接口

- 新增 LoginRequest/LoginResponse DTO
- 实现 JWT Token 签发与验证

Closes #1234
Related to #1230"
```

| 关键词 | 自动行为（GitHub/GitLab） |
|--------|--------------------------|
| `Closes #1234` | Issue 自动关闭 |
| `Fixes #1234` | Issue 自动关闭并标记已修复 |
| `Resolves #1234` | Issue 自动关闭 |
| `Related to #1234` | 关联但不关闭 |
| `Refs #1234` | 无自动行为 |

> 🎯 **黄金法则**：假设半年后的你在看 `git blame`——TA 能凭 commit message 理解"为什么有这个变更"。没有 Issue 关联的 commit 半年后就是"考古现场"。

## 10. 协作流程最佳实践

### 10.1 标准 PR 流程 8 步

```text
1. 从 develop 拉 feature 分支
   git switch develop && git pull && git switch -c feature/xxx
2. 开发 + 频繁 commit（粒度适中）git add -p → git commit
3. 每天同步 develop（避免落后太多）git fetch origin && git rebase origin/develop
4. 开发完成，整理历史 git rebase -i HEAD~10  # squash 多余的 WIP
5. 推送并创建 PR git push -u origin feature/xxx
6. Code Review → 修复意见 → push 更新（用 fixup! commit 回应）
7. 通过后合并（Merge Commit 推荐 / Squash）
8. 清理 git switch develop && git pull && git branch -d feature/xxx
```

### 10.2 PR 描述模板

```markdown
## 📝 变更概述
新增用户邮箱验证功能

## 🔗 关联Issue
Closes #1234

## 🧪 测试
- [x] 单元测试：验证码生成与校验
- [x] 集成测试：注册→验证→登录完整流程
- [x] 异常测试：过期验证码、错误验证码

## 📸 截图
<如有UI变更>

## ⚠️ 风险提示
- 依赖邮件服务，需确认测试环境邮件配置
```

### 10.3 Review 意见处理（fixup 策略）

```bash
# 收到 Review 意见后：
# ❌ 直接 amend 原 commit → reviewer 看不到你改了什么
# ✅ 新增 fixup commit → reviewer 只需看这个新 commit
git commit -m "fixup! feat(user): 添加用户登录逻辑"

# PR 最终合并前整理：
git rebase -i HEAD~5 --autosquash   # 自动把所有 fixup! 压入对应 commit
git push --force-with-lease
```

## 11. 多环境与微服务最佳实践

| 策略 | 适用 | Git 操作 |
|------|------|---------|
| **多仓库(Multi-Repo)** | 大团队、服务独立 | 每个服务独立仓库 |
| **Monorepo** | 小团队、服务紧耦合 | Maven 多模块单仓库 |
| **公共组件** | 跨服务复用 | 发布 jar 包到内部 Maven 仓库（推荐） |

```yaml
# GitHub Actions：按分支类型触发不同级别 CI
on:
  push:
    branches:
      - 'feature/**'    # 基础构建+单元测试
      - 'develop'       # 构建+测试+部署到测试环境
      - 'main'          # 完整流水线+部署到生产
      - 'hotfix/**'     # 快速通道（跳过非关键检查）
```

## 12. 团队 Git 规范模板（可直接复制）

```markdown
# 团队Git规范 (复制到项目 README/CONTRIBUTING.md)

## 分支命名
- feature/<模块>-<简述>  例: feature/user-auth
- fix/<模块>-<简述>      例: fix/order-total
- hotfix/<简述>         例: hotfix/pay-timeout

## Commit格式
<type>(<scope>): <subject>
type: feat|fix|refactor|test|docs|chore|perf|ci

## PR要求
- 必须关联Issue（Closes #N）
- 至少1人Approval
- CI全部通过
- PR描述包含：变更概述 + 测试说明 + 风险提示

## 禁止事项
- 禁止直接push到main/develop
- 禁止在公共分支rebase
- 禁止提交密钥/密码/Token
- 禁止提交编译产物(target/、.idea/)
- 禁止force push共享分支
```

## 13. 核心要点

> 🎯 **核心要点**：
> - 四道防线：预防（gitignore+hook）→ 身份（GPG/SSH 签名）→ 权限（分支保护）→ 补救（filter-repo + revoke）；
> - 密钥泄露处理顺序：**先 revoke 再清历史**（revoke 最高优先级）；
> - 分支保护：PR 强制 + Approvals + CI 通过 + 禁 force push 四件套；
> - 提交规范：Conventional Commits + Issue 关联 + 原子化（"and" 即拆分信号）；
> - 团队规范 = 可复制模板：分支命名/Commit 格式/PR 要求/禁止事项四段式。

## 14. 参考来源

- [Git 官方文档：GPG 签名](https://git-scm.com/book/zh/v2/Git-工具-签名工作)
- [GitHub Docs：关于提交签名验证](https://docs.github.com/zh/authentication/managing-commit-signature-verification)
- [git-secrets（AWS Labs）](https://github.com/awslabs/git-secrets)
- [git-filter-repo](https://github.com/newren/git-filter-repo)
- [Conventional Commits 规范](https://www.conventionalcommits.org/zh-hans/)

---

**下一模块**：[12-Git工具链与IDE集成](12-Git工具链与IDE集成.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
