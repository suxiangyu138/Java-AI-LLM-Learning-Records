# 09 - Git 与 IDE 集成

> 🎯 命令行 Git 是基础，IDE Git 是效率。IDEA/Cursor/VS Code 的 Git 集成让你在不离开编辑器的情况下完成 90% 的 Git 操作

---

## 目录

1. [IDEA Git 操作](#1-idea-git-操作)
2. [代码审查利器](#2-代码审查利器)
3. [冲突解决](#3-冲突解决)

---

## 1. IDEA Git 操作

```text
IDEA Git 工具窗（Alt+9）：
├── Local Changes → 查看暂未提交的修改 (git status)
├── Log → 查看提交历史 (git log)
├── Console → 查看 Git 命令输出
└── Branches → 分支管理 (git branch)

常用快捷操作：
  Ctrl+K → Commit 窗口（选择文件 + 写消息 + 提交）
  Ctrl+Shift+K → Push 推送
  Ctrl+T → Pull 拉取
  Ctrl+Alt+Z → 撤销当前文件的修改
  Ctrl+Shift+` → 切换分支

差异对比（最好用的功能）：
  → 选中文件 → Ctrl+D → 查看修改了什么
  → 左 = 旧版本，右 = 当前版本
  → 可以直接在对比视图中编辑
  → 可以部分回退（选中某几行 → 还原）
```

## 2. 代码审查利器

```text
IDEA 审查流程：

1. 查看 PR/Commit 的修改
   → VCS → Git → Show History
   → 选中一个 commit → Show Diff

2. Annotate (Git Blame)
   → 右键代码 → Git → Annotate
   → 左侧显示每行的作者 + 提交时间
   → 点击可看完整 commit 信息

3. 审查模式
   → View → Tool Windows → Commit
   → 逐个文件查看修改 → 添加审查意见

GitHub/GitLab 集成：
   → IDEA 内置 PR 管理（Plugins → GitHub/GitLab）
   → 直接在 IDE 中查看/评论/合并 PR
   → 不需要打开浏览器
```

## 3. 冲突解决

```text
IDE 冲突解决（比命令行直观 10 倍）：

冲突文件显示为红色 → 右键 → Merge

三栏界面：
  左：你的修改  中：最终结果  右：远程的修改

操作：
  → << (接受左边的) / >> (接受右边的)
  → 手动编辑中间栏
  → 右键 → "Accept Left" → "Accept Right"
  → 完成后 → Apply

冲突预防：
  → 频繁 pull（每天至少一次）
  → 小 PR（< 200 行，冲突概率低）
  → 先 rebase 再 merge（IDEA: Git → Rebase）
```

### AI 辅助冲突解决

```text
Cursor/VS Code AI 冲突解决：
  → 选中冲突标记 (<<< === >>>)
  → Ctrl+K → "resolve this merge conflict"
  → AI 理解两边修改 → 自动合并冲突
```

## 核心要点回顾

- IDEA Git: Ctrl+K(Commit) / Ctrl+Shift+K(Push) / Ctrl+D(Diff)
- Annotate = 代码考古（谁写的、什么时候写的）
- 冲突解决三栏视图 = 比命令行直观 10 倍
- IDE 内置 PR 管理 = 不离开编辑器完成 Code Review
- AI 冲突解决 = 选中→一句话→自动合并

## 参考资料

1. IntelliJ IDEA Git 集成文档
2. Git 官方文档 — git-scm.com
