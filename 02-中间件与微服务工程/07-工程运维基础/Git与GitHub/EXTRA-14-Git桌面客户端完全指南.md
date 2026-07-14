# Git 桌面客户端完全指南

## Git 生态第三层：图形化操作

不是所有人都喜欢敲命令。桌面客户端让你通过可视化界面完成 Git 操作——看 diff 更直观，解决冲突更轻松，看历史更清晰。

---

## 1. 四大客户端速览

| | GitHub Desktop | GitKraken | Sourcetree | Fork |
|------|:---:|:---:|:---:|:---:|
| **价格** | 免费 | 免费(基础)/付费 | 免费 | 付费(一次买断) |
| **平台** | Win/Mac | Win/Mac/Linux | Win/Mac | Win/Mac |
| **学习曲线** | ⭐ 最简单 | ⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **历史可视化** | 基础 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **冲突解决** | 基础 | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **GitHub 集成** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| **性能(大仓库)** | ⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| **适合谁** | 新手、GitHub 用户 | 全面手 | 爱折腾的 | Mac 专业户 |

---

## 2. GitHub Desktop — 新手友好首选

### 为什么推荐它

- GitHub 官方出品，安装即用
- 界面极简，没有多余功能
- 与 GitHub.com 无缝集成（PR、Issues 一条龙）

### 安装

```bash
# Windows
winget install GitHub.GitHubDesktop
# 或直接下载: https://desktop.github.com

# Mac
brew install --cask github
```

### 核心操作流

```text
┌─ 左侧栏 ─────────────┬─ 右侧主区域 ────────────────┐
│                      │                            │
│  Current Repository   │  Changes (工作区修改)       │
│  Current Branch       │   ├─ 勾选文件 → 写 summary │
│                       │   └─ Commit to main        │
│  ┌─ 仓库列表 ──────┐  │                            │
│  │ repo-1          │  │  History (提交历史)         │
│  │ repo-2     ★    │  │   └─ 时间线视图            │
│  │ repo-3          │  │                            │
│  └─────────────────┘  │  Pull Request 面板         │
│                       │                            │
└───────────────────────┴────────────────────────────┘
```

### 六步完成日常操作

```text
1. Clone 仓库 → File → Clone Repository → 输入 URL
2. 创建分支 → 顶部 "Current Branch" → New Branch
3. 写代码 → 用任意编辑器
4. 查看变更 → 左侧 Changes tab，勾选要提交的文件
5. Commit → 写 summary → 点 Commit
6. Push → 点 Push origin
```

### 适用场景

- ✅ Git 新手入门（配合 [01-Git核心技能](01-Git核心技能.md) 学习）
- ✅ 日常简单的 commit/push/pull
- ✅ 查看 diff 和简单冲突解决
- ❌ 复杂的 rebase/cherry-pick（这些还是得命令）

---

## 3. GitKraken — 可视化王者

### 最大卖点：Commit 图

GitKraken 的历史视图是所有客户端中最出色的——清晰的 DAG 图，一眼看清全部分支结构。

```text
●  ●  ●  ● (main)
    \  /
     ●  ● (feature/login)
         \
          ● (feature/oauth)
```

### 安装

```bash
# Windows
winget install Axosoft.GitKraken

# Mac
brew install --cask gitkraken
```

### 特色功能

**拖拽式 Rebase / Cherry-Pick**

```text
直接把 commit 节点拖到目标分支上 → 自动 rebase 或 cherry-pick
不用记命令参数，拖就完事了
```

**内置 Merge Conflict 编辑器**

```text
三栏视图:
  左边 (当前分支) │ 中间 (输出) │ 右边 (被合并分支)
  ─────────────────┼────────────┼─────────────────
  const PORT=3000  │ 选一个 →  │ const PORT=8080
```

**GitKraken Boards / Timelines**

- 内置看板（类似 Jira/Trello），与 Git 分支联动
- 团队时间线：谁在做什么，一清二楚

**Interactive Rebase GUI**

```text
右键一个 commit → Interactive Rebase
→ 拖拽排序
→ 右键 Squash/Drop/Reword
→ 一键 Apply
```

### 适用场景

- ✅ 需要看清楚分支结构（复杂仓库的救星）
- ✅ 频繁 rebase / cherry-pick
- ✅ 解决复杂冲突
- ✅ 团队协作可视化
- ❌ 免费版不支持私有仓库

---

## 4. Sourcetree — 老牌经典

### 特点

Atlassian（Bitbucket 的公司）出品，功能最"硬核"的客户端。

```bash
# Windows
winget install Atlassian.Sourcetree

# Mac
brew install --cask sourcetree
```

### Sourcetree 独有功能

- **文件状态筛选**：只看 Modified / Staged / Untracked，快速定位
- **逐行 Stage (Hunk staging)**：可以选择性地只暂存文件中的某几行
- **Blame 视图**：侧边栏直接显示每行的作者和 commit
- **Git Flow 内置**：一键创建 feature/release/hotfix 分支
- **Bisect 向导**：图形化引导二分法定位 bug

### 适用场景

- ✅ 想学 Git 命令（每个操作都显示对应的命令行）
- ✅ 需要精细控制（逐行 stage）
- ✅ 使用 Git Flow 工作流
- ✅ Bitbucket 用户（原生集成）
- ❌ 界面略老旧，启动较慢

---

## 5. Fork — Mac 专业户

### 特点

- 快，非常快——大仓库性能秒杀同类
- 界面精美，符合 Mac 设计哲学
- 一次性付费（$59.99），没有订阅制

### 核心优势

```text
- 极速的 commit 图渲染（万级 commit 也不卡）
- 内置 GitFlow 支持
- 多 tab 仓库切换（像浏览器一样）
- Stash / Apply Stash 可视化
- 交互式 Rebase GUI
```

### 适用场景

- ✅ Mac 用户首选
- ✅ 大型仓库（几万 commits）
- ✅ 愿意一次性付费
- ❌ 没有 Windows/Linux 版...吗？（Fork 现在有 Windows 版了！）
- ❌ 付费才能长期用（有免费试用期）

---

## 6. 客户端 vs 命令行：什么时候用哪个？

| 场景 | 推荐 |
|------|------|
| 日常 commit/push/pull | **客户端** (更快，点点点即可) |
| 查看 commit 历史 | **客户端** (可视化 DAG 图无与伦比) |
| 解决冲突 | **客户端** (三栏对比看得清楚) |
| 查看 diff | **客户端** (颜色高亮，逐行对比) |
| Rebase / Cherry-pick | **客户端** (拖拽) 或 **命令行** (灵活) |
| 批量操作 / 脚本 | **命令行** |
| Bisect / Blame | **客户端** (图形化更直观) |
| CI/CD 自动化 | **命令行** |
| .gitignore / config | **命令行** 或直接编辑文件 |

### 混合使用的建议

```text
1. 用客户端查看历史和 diff（它的强项）
2. 用命令行做复杂操作（rebase -i、cherry-pick、bisect run）
3. 配置 git 别名简化常用命令（见 11-Git配置优化）
4. 不依赖客户端：万一哪天只有终端，你也会操作
```

---

## 7. 一句话选型

| 你是谁 | 用什么 |
|--------|--------|
| Git 新手 | **GitHub Desktop** — 简单，够用，免费 |
| 追求可视化 | **GitKraken** — Drag & Drop 让人上瘾 |
| 想边学边用 | **Sourcetree** — 每个操作都显示对应命令 |
| Mac 用户 + 大仓库 | **Fork** — 丝滑流畅 |
| 命令行老手 | 继续用命令行，偶尔开客户端看图 |

> **核心原则**：代码放 Gitee/GitHub，日常用 VS Code + GitLens，想看图用 GitKraken。

---

> 上一篇：[13-Git托管平台对比与实战](13-Git托管平台对比与实战.md)
> 下一篇：[15-IDE中的Git集成](15-IDE中的Git集成.md)
