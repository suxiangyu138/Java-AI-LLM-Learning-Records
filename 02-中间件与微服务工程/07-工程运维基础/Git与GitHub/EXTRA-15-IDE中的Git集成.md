# IDE 中的 Git 集成

## Git 生态第四层：IDE 集成

写代码时频繁切换到终端或客户端很打断思路。IDE 集成让你在编辑器内完成 90% 的 Git 操作——这才是最高效的方式。

---

## 1. VS Code Git 内置功能

VS Code 自带的 Git 已经够覆盖日常操作：

### 1.1 Source Control 面板 (`Ctrl+Shift+G`)

```text
┌─ SOURCE CONTROL ──────────────────────────────────┐
│                                                    │
│  Changes (3)                     [+] Commit All    │
│  ──────────────────────────────────────────────     │
│  M  src/auth.ts          [文件图标] [撤销图标]      │
│  M  src/login.ts         [文件图标] [撤销图标]      │
│  U  src/new-file.ts      [文件图标] [撤销图标]      │
│                                                    │
│  Staged Changes (2)                               │
│  ──────────────────────────────────────────────     │
│  A  src/oauth.ts                                  │
│  M  README.md                                     │
│                                                    │
│  Message (press Ctrl+Enter to commit)              │
│  ┌──────────────────────────────────────────┐      │
│  │ feat: add OAuth login                    │      │
│  └──────────────────────────────────────────┘      │
│                                                    │
└────────────────────────────────────────────────────┘
```

### 1.2 日常操作映射

| 操作 | 快捷键 | 说明 |
|------|--------|------|
| 打开 Source Control | `Ctrl+Shift+G` | 源码管理面板 |
| Stage 文件 | 点 `+` 号 | 单个文件暂存 |
| Unstage | 点 `-` 号 | 撤销暂存 |
| Commit | `Ctrl+Enter` | 提交暂存的修改 |
| Push/Pull | 底部状态栏点击 | 同步图标 |
| 切换分支 | 底部状态栏点击分支名 | 弹出分支列表 |
| 查看 Diff | 点击修改的文件 | 内联 diff 视图 |
| 放弃修改 | 点文件旁的 ↶ 图标 | 等效 `git restore` |
| 解决冲突 | 点击冲突文件 | 图形化冲突编辑器 |

### 1.3 内置 Diff 编辑器

```text
左侧 (旧版本)          │  右侧 (新版本)
───────────────────────│───────────────────────
  function login() {   │  function login(user) {
    // old code        │    if (!user) throw ...
    ...                │    ...
  }                    │  }
```

### 1.4 冲突编辑器

```text
Current Change (ours)  │  Incoming (theirs)  │  Result
───────────────────────│─────────────────────│────────
const PORT = 3000;     │  const PORT = 8080; │  选择
                       │                     │
 [Accept Current] │ [Accept Incoming] │ [Accept Both] │ [Compare Changes]
```

### 1.5 三个常用 VS Code 设置

```json
// .vscode/settings.json
{
  // 自动 fetch 远程更新（默认每 3 分钟）
  "git.autofetch": true,
  "git.autofetchPeriod": 180,

  // 提交前自动 stage 所有文件（类似 git add -A）
  "git.enableSmartCommit": true,

  // 在文件浏览器中显示 Git 状态颜色
  // 绿色 = 新增, 黄色 = 修改, 红色 = 删除
  "git.decorations.enabled": true
}
```

---

## 2. GitLens — VS Code Git 的终极进化

### 为什么需要 GitLens

VS Code 内置 Git 满足基本操作，但缺三样东西：
1. **Blame 信息** — 这行代码是谁写的？什么时候写的？
2. **历史可视化** — 这个文件经历了哪些修改？
3. **代码导航** — 从这个 commit 跳出去，看完整的变更上下文

GitLens 补齐了这些，而且**免费版就够用**。

### 安装

```bash
# VS Code 内:
# Extensions (Ctrl+Shift+X) → 搜索 "GitLens" → Install
code --install-extension eamodio.gitlens
```

### 核心功能

**① 行内 Blame 注解（GitLens 的灵魂）**

```text
// 每行末尾自动显示作者和时间
function login(user, pass) {   // zhangsan, 3 weeks ago
  if (!user) throw ...         // zhangsan, 3 weeks ago
  const res = await fetch(...) // lisi, 2 days ago  ← 悬停看更多
}
```

**② File History 面板**

```text
点击文件右上角的 GitLens 图标 →
显示这个文件的所有变更历史：
  ● zhangsan 3 weeks ago "feat: add login module"
  ● zhangsan 2 months ago "refactor: extract auth utils"
  ● lisi 4 months ago "init project"
  ...
点击任意一条 → 展开该 commit 的完整 diff
```

**③ Commit 详情悬浮框**

```text
悬停在任意一行代码的 blame 信息上：
┌─────────────────────────────────────────┐
│ Commit: a1b2c3d                         │
│ Author: zhangsan <zhangsan@example.com>  │
│ Date:   2025-01-10 10:30:00             │
│ Message: feat: add login module         │
│                                         │
│ Changes in this commit:                 │
│  src/auth.ts       +42 -8               │
│  src/login.ts      +120 new             │
│                                         │
│ [Open Changes] [Open File at Revision]   │
└─────────────────────────────────────────┘
```

**④ 行历史 (Line History)**

```text
选中任意一行 → 右键 → GitLens: Show Line History
→ 只看这"一行代码"的修改历史
→ 什么时候引入的、谁改过、每次改了什么
```

**⑤ Commit Graph（付费功能，有替代品）**

GitLens 的 Commit Graph 是付费功能，用下面这个免费插件替代。

---

## 3. Git Graph — 免费的 Commit 图

### 安装

```bash
code --install-extension mhutchie.git-graph
```

### 功能

```text
在 VS Code 内直接查看完整的 commit DAG 图：

* e3f4g5h (HEAD -> main) feat: add search
*   a1b2c3d Merge branch 'feature/login'
|\
| * d4e5f6g feat: login form
| * c7d8e9f refactor: auth utils
|/
* b1c2d3e fix: header style
* a1b2c3d init

右键任意 commit → Cherry Pick / Revert / Reset / Create Branch / ...
```

---

## 4. JetBrains IDE (IntelliJ / WebStorm / PyCharm)

JetBrains 的 Git 集成是业界最强之一，无需额外插件：

### 核心面板

```text
┌─ Git 面板 (Alt+9) ─────────────────────────────────┐
│                                                      │
│  Local Changes                                       │
│   ├─ Default Changelist (3 files)                    │
│   │   ├─ src/auth.ts                                 │
│   │   ├─ src/login.ts                                │
│   │   └─ test/auth.test.ts                           │
│                                                      │
│  Log (Alt+9 → Log tab)                               │
│   └─ 完整的 DAG 图 + 搜索 + 过滤                     │
│                                                      │
│  Console                                             │
│   └─ 内置终端，图形操作自动翻译为 Git 命令显示         │
└──────────────────────────────────────────────────────┘
```

### JetBrains 独有功能

**① VCS Operations 弹窗 (`Alt+``)**

```text
一个快捷键弹出所有 Git 操作：
→ Commit
→ Push
→ Pull
→ Branches
→ Stash / Unstash
→ Rollback
→ ...
```

**② Shelve / Unshelve（JetBrains 版的 Stash）**

比 `git stash` 更好用——可以 shelve 部分文件的修改，而 stash 默认是全量的。

**③ Annotate（= Git Blame）**

```text
右键 → Git → Annotate
→ 编辑器左边栏显示每一行的 commit 信息
→ 点击 annotation 跳转到完整 diff
→ 比 GitLens 更流畅，不被干扰
```

**④ Show Diff in Editor**

```text
Log 面板 → 选中两个 commit → Ctrl+D
→ 并排显示两个版本的所有差异文件
→ 点击任意文件查看完整 diff
```

### JetBrains + GitHub/GitLab 集成

```text
Settings → Version Control → GitHub/GitLab
→ 登录账号后：
  - IDE 内直接创建 PR/MR
  - 查看和回复 PR Review 评论
  - 查看 CI/CD 状态
```

---

## 5. 各 IDE Git 功能对比

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
| 内置终端 | ✅ | ✅ | ✅ |

---

## 6. 推荐配置方案

### 方案 A：轻量级（推荐大多数开发者）

```text
VS Code
  + GitLens (免费版)          → Blame + File History
  + Git Graph                 → Commit DAG 可视化
  = 完整的 Git 工作体验，零成本
```

### 方案 B：重型武器（JetBrains 用户）

```text
JetBrains IDE
  内置 Git 就已经完全够用
  不需要额外装任何插件
```

### 方案 C：混合搭配

```text
VS Code 写代码（看 Blame、做 Diff）
GitKraken 看图（解决复杂冲突、Rebase 拖拽）
终端 做自动化（脚本、CI/CD）
```

---

> 上一篇：[14-Git桌面客户端完全指南](14-Git桌面客户端完全指南.md)
> 下一篇：[16-Git生态扩展与自动化工具](16-Git生态扩展与自动化工具.md)
