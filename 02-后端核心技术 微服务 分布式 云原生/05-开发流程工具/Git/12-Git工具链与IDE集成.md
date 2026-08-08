# 12-Git 工具链与 IDE 集成
> Git 生态五层结构：桌面客户端、VS Code/GitLens/JetBrains、终端工具（tig/delta/lazygit）、推荐的完整工具箱

## 📚 目录
1. [Git 生态五层结构](#1-git-生态五层结构)
2. [四大桌面客户端对比](#2-四大桌面客户端对比)
3. [GitHub Desktop / GitKraken](#3-github-desktop--gitkraken)
4. [Sourcetree / Fork](#4-sourcetree--fork)
5. [客户端 vs 命令行选型](#5-客户端-vs-命令行选型)
6. [VS Code 内置 Git](#6-vs-code-内置-git)
7. [GitLens 与 Git Graph](#7-gitlens-与-git-graph)
8. [JetBrains IDE 集成](#8-jetbrains-ide-集成)
9. [各 IDE Git 功能对比与推荐方案](#9-各-ide-git-功能对比与推荐方案)
10. [终端工具：tig / delta / lazygit](#10-终端工具tig--delta--lazygit)
11. [推荐的 Git 工具箱与学习路径](#11-推荐的-git-工具箱与学习路径)
12. [核心要点](#12-核心要点)
13. [参考来源](#13-参考来源)

## 1. Git 生态五层结构

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

## 2. 四大桌面客户端对比

| | GitHub Desktop | GitKraken | Sourcetree | Fork |
|------|:---:|:---:|:---:|:---:|
| 价格 | 免费 | 免费(基础)/付费 | 免费 | 付费(一次买断) |
| 平台 | Win/Mac | Win/Mac/Linux | Win/Mac | Win/Mac |
| 学习曲线 | ⭐ 最简单 | ⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| 历史可视化 | 基础 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| 冲突解决 | 基础 | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| GitHub 集成 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| 性能(大仓库) | ⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| 适合谁 | 新手、GitHub 用户 | 全面手 | 爱折腾的 | Mac 专业户 |

## 3. GitHub Desktop / GitKraken

### 3.1 GitHub Desktop — 新手友好首选

```bash
winget install GitHub.GitHubDesktop    # Windows
brew install --cask github             # Mac
```

六步日常操作：Clone（File → Clone Repository → URL）→ 建分支（Current Branch → New Branch）→ 写代码 → 查看变更（Changes tab 勾选）→ Commit（写 summary）→ Push。

| 适用 | 不适用 |
|------|--------|
| 新手入门、日常 commit/push/pull、简单 diff/冲突 | 复杂 rebase/cherry-pick |

### 3.2 GitKraken — 可视化王者

```bash
winget install Axosoft.GitKraken    # Windows
brew install --cask gitkraken       # Mac
```

| 特色功能 | 说明 |
|----------|------|
| Commit 图 | DAG 清晰可视化（最大卖点） |
| 拖拽式 Rebase/Cherry-Pick | commit 节点直接拖到目标分支 |
| 内置冲突编辑器 | 三栏：左当前分支│中输出│右被合并分支 |
| Interactive Rebase GUI | 右键 → 拖拽排序 → Squash/Drop/Reword → Apply |
| Boards/Timelines | 内置看板与团队时间线 |

> ⚠️ 免费版不支持私有仓库。

## 4. Sourcetree / Fork

### 4.1 Sourcetree — 老牌经典（Atlassian）

```bash
winget install Atlassian.Sourcetree    # Windows
brew install --cask sourcetree         # Mac
```

| 独有功能 | 说明 |
|----------|------|
| 文件状态筛选 | Modified/Staged/Untracked 快速过滤 |
| 逐行 Stage | Hunk staging |
| Blame 视图 | 侧边栏显示每行作者和 commit |
| Git Flow 内置 | 一键创建 feature/release/hotfix |
| Bisect 向导 | 图形化二分定位 bug |
| 命令回显 | 每个操作显示对应命令行（适合学命令） |

缺点：界面略老旧、启动较慢。

### 4.2 Fork — Mac 专业户

- 极速 commit 图渲染（万级 commit 不卡）、内置 GitFlow、多 tab 仓库切换、Stash/Apply 可视化、交互式 Rebase GUI；
- 一次性付费 $59.99；现在有 Windows 版。

## 5. 客户端 vs 命令行选型

| 场景 | 推荐 |
|------|------|
| 日常 commit/push/pull | **客户端** |
| 查看 commit 历史 | **客户端**（可视化 DAG 图） |
| 解决冲突 | **客户端**（三栏对比） |
| 查看 diff | **客户端**（颜色高亮逐行） |
| Rebase / Cherry-pick | **客户端**（拖拽）或**命令行**（灵活） |
| 批量操作 / 脚本 | **命令行** |
| Bisect / Blame | **客户端**（图形化更直观） |
| CI/CD 自动化 | **命令行** |
| .gitignore / config | **命令行**或直接编辑文件 |

> 🎯 混合使用建议：客户端看历史/diff；命令行做复杂操作（rebase -i、cherry-pick、bisect run）；配置别名简化常用命令；**不依赖客户端**（万一只有终端也会操作）。一句话选型：新手→GitHub Desktop；追求可视化→GitKraken；边学边用→Sourcetree；Mac+大仓库→Fork。

## 6. VS Code 内置 Git

Source Control 面板（`Ctrl+Shift+G`）：

| 操作 | 方式 |
|------|------|
| Stage / Unstage | 点 `+` / `-` |
| Commit | `Ctrl+Enter` |
| Push/Pull | 底部状态栏 |
| 切换分支 | 底部状态栏点分支名 |
| 查看 Diff | 点击文件 |
| 放弃修改 | ↶ 图标（等效 `git restore`） |
| 解决冲突 | 图形化冲突编辑器：Accept Current / Accept Incoming / Accept Both / Compare Changes |

```json
// .vscode/settings.json
{
  "git.autofetch": true,          // 自动 fetch（默认每 3 分钟）
  "git.enableSmartCommit": true,  // 提交前自动 stage（类似 git add -A）
  "git.decorations.enabled": true // 文件浏览器显示 Git 状态颜色
}
```

## 7. GitLens 与 Git Graph

### 7.1 GitLens（VS Code 终极进化）

```bash
code --install-extension eamodio.gitlens
```

| 核心功能 | 说明 |
|----------|------|
| 行内 Blame 注解 | 每行末尾显示作者和时间，悬停看更多 |
| File History 面板 | 文件所有变更历史，点击展开完整 diff |
| Commit 详情悬浮框 | Commit/Author/Date/Message/Changes |
| Line History | 选中行右键 → Show Line History（只看一行代码的历史） |

> 💡 GitLens 免费版够用；Commit Graph 是付费功能，用 Git Graph 免费替代。

### 7.2 Git Graph（免费的 Commit 图）

```bash
code --install-extension mhutchie.git-graph
```

VS Code 内查看完整 commit DAG 图；右键任意 commit → Cherry Pick / Revert / Reset / Create Branch。

## 8. JetBrains IDE 集成

Git 集成业界最强，无需额外插件（IntelliJ/WebStorm/PyCharm 通用）。

| 面板 | 说明 |
|------|------|
| Git 面板（`Alt+9`） | Local Changes（Default Changelist）、Log tab（完整 DAG 图）、Console（内置终端，图形操作自动翻译为 Git 命令显示） |
| VCS Operations（`` Alt+` ``） | 弹出所有 Git 操作：Commit/Push/Pull/Branches/Stash/Rollback |

| 独有功能 | 说明 |
|----------|------|
| Shelve/Unshelve | JetBrains 版 Stash（可 shelve 部分文件） |
| Annotate | = Git Blame，点击跳转完整 diff，比 GitLens 更流畅 |
| Show Diff in Editor | Log 面板选两个 commit → `Ctrl+D` 并排对比 |
| 冲突解决三栏 | 左(ours)/中(base)/右(theirs) + `>>` 接受整块 |
| GitHub/GitLab 集成 | IDE 内直接创建 PR/MR、回复 Review 评论、查看 CI 状态 |

**IDEA 冲突解决快捷键**：

| 操作 | 快捷键 |
|------|--------|
| 接受左侧（本地）变更 | `Ctrl + Shift + ←` |
| 接受右侧（远程）变更 | `Ctrl + Shift + →` |
| 跳转下一个冲突 | `Alt + ↓` |
| 全部应用左侧 | `Ctrl + Alt + Shift + ←` |
| 全部应用右侧 | `Ctrl + Alt + Shift + →` |

## 9. 各 IDE Git 功能对比与推荐方案

| 功能 | VS Code 内置 | VS Code + GitLens | JetBrains |
|------|:---:|:---:|:---:|
| Stage / Commit / Push | ✅ | ✅ | ✅ |
| Diff 视图 | ✅ | ✅ | ✅ |
| 冲突解决 | ✅ | ✅ | ✅ |
| 分支管理 | ✅ | ✅ | ✅ |
| Blame | ❌ | ✅⭐ | ✅⭐ |
| File History | ❌ | ✅ | ✅ |
| Commit Graph | ❌ | ✅(Git Graph) | ✅ |
| Line History | ❌ | ✅ | ✅ |
| Shelve (部分 Stash) | ❌ | ❌ | ✅ |
| PR/MR 集成 | 基础 | ✅(GitHub) | ✅ |

| 方案 | 组合 | 适合 |
|------|------|------|
| A 轻量级 | VS Code + GitLens(免费) + Git Graph | **大多数开发者** |
| B 重型武器 | JetBrains 内置 | 全家桶用户（零插件） |
| C 混合搭配 | VS Code 写代码 + GitKraken 看图 + 终端自动化 | 进阶玩家 |

## 10. 终端工具：tig / delta / lazygit

### 10.1 tig（终端历史浏览）

```bash
sudo apt install tig    # 或 brew install tig
tig                     # 方向键移动、Enter 展开 diff、q 返回
tig src/auth.ts         # 只看某文件历史
tig blame src/auth.ts   # 终端 blame
```

### 10.2 delta（更好看的 diff）

```bash
brew install git-delta    # 或 cargo install git-delta
```

```ini
# ~/.gitconfig
[core]
    pager = delta
[interactive]
    diffFilter = delta --color-only
[delta]
    navigate = true             # 用 n/N 在 diff 块间跳转
    line-numbers = true
    syntax-theme = OneHalfDark
```

### 10.3 lazygit（终端 Git GUI）

```bash
winget install lazygit    # 或 brew install lazygit
```

四面板终端 Git GUI（状态/Staging、分支/Commit、Stash、Diff），全快捷键操作——服务器环境必备。

## 11. 推荐的 Git 工具箱与学习路径

### 11.1 工具箱

```text
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
```

### 11.2 学习路径

```text
第1周: 02-基础操作 + GitHub Desktop → 会用 Git
第2周: 01-底层原理 + 04-合并变基 → 理解 Git
第3周: 06-远程协作 + 03-分支模型 → 协作 Git
第4周: 08-钩子自动化 + 12-IDE集成 → 自动化 Git
第5周: 10-高级工具 + 11-安全最佳实践 → 精通 Git
```

## 12. 核心要点

> 🎯 **核心要点**：
> - Git 生态五层：核心引擎 → 托管平台 → 桌面客户端 → IDE → 扩展工具；
> - 客户端选型：新手 GitHub Desktop / 可视化 GitKraken / 学命令 Sourcetree / 大仓库 Fork；
> - IDE 标配：VS Code + GitLens + Git Graph（免费零成本）；JetBrains 内置即最强；
> - 终端三件套：tig（历史）、delta（diff）、lazygit（全功能 GUI）——服务器环境救命；
> - 混合使用原则：客户端看图、命令行做复杂操作、不依赖任何单一工具。

## 13. 参考来源

- [GitHub Desktop 官网](https://desktop.github.com/)
- [GitKraken 官网](https://www.gitkraken.com/)
- [Sourcetree 官网](https://www.sourcetreeapp.com/)
- [GitLens 扩展市场页](https://marketplace.visualstudio.com/items?itemName=eamodio.gitlens)
- [Git Graph 扩展市场页](https://marketplace.visualstudio.com/items?itemName=mhutchie.git-graph)
- [delta（GitHub）](https://github.com/dandavison/delta)
- [lazygit（GitHub）](https://github.com/jesseduffield/lazygit)

---

**返回总览**：[00-总览](00-Git知识体系总览.md)
