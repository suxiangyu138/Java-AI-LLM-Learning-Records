# Git 生态扩展与自动化工具

## Git 生态第五层：扩展工具链

Git 本身 + 托管平台 + 客户端 + IDE，构成了日常使用的基础。但还有一些周边工具，能在特定场景下大幅提升效率——自动生成 Changelog、修复误提交的密码、本地模拟 CI、多仓库管理。

---

## 1. Git 生态全景图

```text
┌──────────────────────────────────────────────────────────┐
│                    Git 生态五层结构                       │
├────────────┬─────────────────────────────────────────────┤
│ 第五层      │ 扩展工具：gh CLI · act · BFG · repo 工具    │
│ → 效率加成  │  changelog生成 · 本地CI · 敏感信息清理       │
├────────────┼─────────────────────────────────────────────┤
│ 第四层      │ IDE 集成：VS Code · GitLens · JetBrains     │
│ → 开发顺手  │  Git Graph · Annotate · File History        │
├────────────┼─────────────────────────────────────────────┤
│ 第三层      │ 桌面客户端：GitHub Desktop · GitKraken       │
│ → 图形化    │  Sourcetree · Fork                          │
├────────────┼─────────────────────────────────────────────┤
│ 第二层      │ 托管平台：GitHub · GitLab · Gitee           │
│ → 云端协作  │  Issue · PR/MR · CI/CD · Pages              │
├────────────┼─────────────────────────────────────────────┤
│ 第一层      │ Git 核心引擎：命令行 · 数据模型 · 协议       │
│ → 基础      │  blob · tree · commit · DAG · refs          │
└────────────┴─────────────────────────────────────────────┘
```

---

## 2. GitHub CLI (gh) — 在终端操作一切

```bash
# 安装
winget install GitHub.cli        # Windows
brew install gh                  # Mac
sudo apt install gh              # Linux
```

### 完整工作流

```bash
# 登录
gh auth login

# === 仓库操作 ===
gh repo clone owner/repo         # 克隆
gh repo create my-new-repo --public --push  # 创建并推送
gh repo view owner/repo --web    # 在浏览器打开

# === Issue 操作 ===
gh issue list                    # 列出 Issues
gh issue create --title "Bug: login fails" --body "## Steps..."
gh issue view 42                 # 查看 #42
gh issue status                  # 我的 Issues

# === PR 操作（最常用）===
gh pr create --title "feat: add OAuth" --body "## Summary..."
gh pr list                       # 列出 PR
gh pr checkout 123               # 本地检出 PR
gh pr view 123                   # 查看详情
gh pr review --approve           # 批准
gh pr review --comment -b "LGTM" # 评论
gh pr merge 123 --squash         # 合并

# === CI 监控 ===
gh run list                      # 最近的 CI 运行
gh run watch                     # 实时查看当前 CI 输出
gh pr checks 123                 # 查看 PR 的 CI 状态

# === Release ===
gh release create v1.0.0 --title "v1.0.0" --notes "## Changelog..."
gh release list

# === Gist ===
gh gist create file.ts           # 分享代码片段
```

### 高级用法：gh + jq

```bash
# 列出所有需要我 review 的 PR（跨所有仓库）
gh search prs --review-requested=@me --state=open \
  --json title,url,repository \
  --jq '.[] | "\(.repository.name): \(.title) → \(.url)"'

# 统计项目的贡献者
gh api repos/owner/repo/contributors \
  --jq '.[] | "\(.login) — \(.contributions) commits"'
```

---

## 3. act — 本地运行 GitHub Actions

不用 push 到 GitHub 就能在本地跑 CI，调试 Actions 的利器。

```bash
# 安装
winget install nektos.act      # Windows
brew install act               # Mac

# 本地运行默认 workflow
act

# 运行特定 job
act -j test

# 运行特定事件
act pull_request

# 运行 push 到 main 的 workflow
act push

# 查看 workflow 而不运行
act -n

# 指定 runner 镜像（中号镜像包含常用工具）
act -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

### 典型场景

```bash
# 场景：写了一个 GitHub Action，想在本地调试
# 不用 push → 看 CI 失败 → 改 → push → 看 CI → 循环
# 直接在本地反复运行，10 分钟调通
act -j build-and-test --verbose
```

---

## 4. 敏感信息清理

### git-filter-repo（取代 filter-branch）

```bash
# 安装
pip install git-filter-repo

# 从完整历史中删除某个文件（如 .env）
git filter-repo --path .env --invert-paths

# 删除某个目录
git filter-repo --path secrets/ --invert-paths

# 替换文本（如替换所有历史中的密码）
git filter-repo --replace-text <(echo "old-password==>REDACTED")

# 删除大于 10MB 的文件
git filter-repo --strip-blobs-bigger-than 10M

# 清理后强制推送
git push --force --all
git push --force --tags
```

### BFG Repo-Cleaner（更快的大文件清理）

```bash
# 比 filter-branch 快 10-1000 倍
# 下载: https://rtyley.github.io/bfg-repo-cleaner/

# 删除大于 100M 的文件
java -jar bfg.jar --strip-blobs-bigger-than 100M repo.git

# 删除匹配的文件
java -jar bfg.jar --delete-files "*.zip" repo.git

# 替换密码文本
java -jar bfg.jar --replace-text passwords.txt repo.git
```

### git-secrets — 预防胜于治疗

```bash
# 安装
# git clone https://github.com/awslabs/git-secrets
# cd git-secrets && sudo make install

# 全局注册（对所有仓库生效）
git secrets --register-aws --global
git secrets --install ~/.git-templates/git-secrets
git config --global init.templateDir ~/.git-templates/git-secrets

# 添加自定义规则
git secrets --add 'password\s*=\s*.+'
git secrets --add 'API[_-]?KEY\s*=\s*.+'

# 扫描历史
git secrets --scan-history
```

---

## 5. Changelog 自动生成

### standard-version

```bash
npm install --save-dev standard-version

# 自动 bump 版本 + 生成 CHANGELOG + 打 tag
npx standard-version

# 首次发布
npx standard-version --first-release

# 指定版本类型
npx standard-version --release-as minor    # 0.1.0 → 0.2.0
npx standard-version --release-as major    # 0.1.0 → 1.0.0

# 预览（不实际执行）
npx standard-version --dry-run
```

### git-cliff（Rust 编写，极快）

```bash
# 安装
cargo install git-cliff

# 生成 CHANGELOG
git cliff -o CHANGELOG.md

# 配合 tag 使用
git cliff --latest --prepend CHANGELOG.md
```

### 配合 release 工作流

```bash
# 一个命令完成：版本 bump + changelog + tag
npm run release
# 内部做了什么：
# 1. standard-version bump 版本号
# 2. 从 conventional commits 生成 CHANGELOG.md
# 3. git commit -m "chore(release): 1.2.0"
# 4. git tag v1.2.0

# 然后推送
git push --follow-tags origin main
```

---

## 6. 多仓库管理

### git-repo（Google 的 Android 管理工具）

多个 Git 仓库协同工作时（如 monorepo 的相反面），统一管理：

```bash
# 安装
# 下载 repo 脚本: curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo

# 初始化（从 manifest 仓库获取所有子仓库定义）
repo init -u https://github.com/org/manifest.git

# 同步所有仓库
repo sync

# 在所有仓库创建分支
repo start feature/upgrade --all

# 查看所有仓库状态
repo status
```

### myrepos (mr)

```bash
# 适合管理多个独立仓库（如 ~/projects/ 下几十个项目）
sudo apt install myrepos    # Debian/Ubuntu
brew install myrepos        # Mac

# 注册仓库
mr register ~/projects/repo1
mr register ~/projects/repo2

# 在所有仓库执行命令
mr fetch       # 所有仓库 git fetch
mr status      # 所有仓库 git status
mr push        # 所有仓库 git push
```

---

## 7. 其他实用工具

### tig — 终端中的 Git 浏览器

```bash
# 安装
sudo apt install tig            # Debian/Ubuntu
brew install tig                # Mac

# 浏览 commit 历史（交互式）
tig
# 方向键移动，Enter 展开 diff，q 返回

# 只看某个文件的历史
tig src/auth.ts

# 查看 blame
tig blame src/auth.ts
```

### delta — 更好的 diff 输出

```bash
# 安装
brew install git-delta          # Mac
cargo install git-delta         # 或通过 cargo

# 配置
# ~/.gitconfig
[core]
    pager = delta
[interactive]
    diffFilter = delta --color-only
[delta]
    navigate = true             # 用 n/N 在 diff 块间跳转
    side-by-side = false        # 统一视图（非并排）
    line-numbers = true
    syntax-theme = OneHalfDark
```

### lazygit — 终端的 Git 客户端

```bash
# 安装
winget install lazygit          # Windows
brew install lazygit            # Mac

# 运行
lazygit
# 一个完整的终端 Git GUI：
# 1. 面板1: 状态/Staging
# 2. 面板2: 分支/Commit
# 3. 面板3: Stash
# 4. 面板4: Diff
# 所有操作通过快捷键完成，非常高效
```

---

## 8. 构建你的 Git 工具箱

```text
┌─────────────────────────────────────────────────┐
│            推荐的 Git 工具箱                     │
├──────────────┬──────────────────────────────────┤
│ 代码托管      │ GitHub (开源) + Gitee (国内镜像)  │
│ 桌面看图      │ GitKraken (复杂分支一目了然)       │
│ 日常开发      │ VS Code + GitLens + Git Graph     │
│ 命令行操作     │ git 原生命令 + gh CLI             │
│ 本地 CI 调试  │ act                               │
│ Changelog    │ standard-version / git-cliff      │
│ 敏感信息防护  │ git-secrets + pre-commit hook     │
│ 历史清理      │ git-filter-repo / BFG            │
│ 更好看的 diff │ delta                             │
│ 终端 Git GUI  │ lazygit                          │
└──────────────┴──────────────────────────────────┘
```

---

## 9. Git 生态学习路径总结

```text
第1周: 01核心技能 + GitHub Desktop → 会用 Git
第2周: 02底层原理 + 04合并变基 → 理解 Git
第3周: 06远程协作 + 13托管平台 → 协作 Git
第4周: 07钩子 + 15 IDE集成 → 自动化 Git
第5周: 10高级工具 + 16扩展工具 → 精通 Git
```

---

> 上一篇：[15-IDE中的Git集成](15-IDE中的Git集成.md)
> 🎉 这是 Git 生态系列最后一篇！全部 16 篇文档已完结，返回 [01-Git核心技能](01-Git核心技能.md) 快速复习。
