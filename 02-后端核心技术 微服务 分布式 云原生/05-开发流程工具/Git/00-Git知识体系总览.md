# Git 知识体系总览
> Git 是开发者最重要的日常工具——从核心技能到底层原理、从分支模型到自动化集成，13 篇文档覆盖 Git 精通所需的全部深度

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [主题速查索引](#4-主题速查索引)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Git 精通体系（13 篇）
│
├── 🔰 基础与原理 (00-02)
│   ├── 00-Git知识体系总览.md        # 导航与路线
│   ├── 01-Git核心原理与对象模型.md   # 内容寻址/四对象/三区/DAG
│   └── 02-Git基础操作与入门.md       # 安装配置/.gitignore/常用命令
│
├── 🌿 分支与协作 (03-06)
│   ├── 03-Git分支模型与团队工作流.md # 分支本质/四模型/命名规范
│   ├── 04-Git合并与变基.md           # merge/rebase/cherry-pick/冲突
│   ├── 05-Git撤销与历史重写.md       # reset/revert/reflog/原子提交
│   └── 06-Git远程协作与托管平台.md   # remote/PR/三平台/LFS
│
├── 🔧 进阶技能 (07-11)
│   ├── 07-Git日常管理与工作区实践.md # 每日节奏/stash/log/bisect/GC
│   ├── 08-Git钩子自动化与定制.md     # hooks/CI/配置/别名
│   ├── 09-Git标签子模块与Monorepo.md # tag/SemVer/submodule/Monorepo
│   ├── 10-Git调试取证与排错.md       # 取证/排错/应急预案
│   └── 11-Git配置安全与最佳实践.md   # 签名/防泄露/团队规范
│
└── 🖥️ 工具与生态 (12)
    └── 12-Git工具链与IDE集成.md     # 桌面客户端/VS Code/JetBrains/终端工具
```

> 🎯 精通标准：不是记住所有命令，而是理解 Git 的 DAG 对象模型 + 能在任何"搞砸了"的情况下找回代码。

## 2. 模块导航

| 序号 | 文件 | 内容 | 级别 |
|:---:|------|------|:---:|
| 00 | [总览（本文）](00-Git知识体系总览.md) | 导图、路线、速查索引 | - |
| 01 | [核心原理与对象模型](01-Git核心原理与对象模型.md) | blob/tree/commit/tag、SHA-1、.git 目录、三区模型、DAG、packfile/GC | ⭐⭐⭐⭐ |
| 02 | [基础操作与入门](02-Git基础操作与入门.md) | 安装配置、.gitignore、init/clone/status/add/commit/diff、分支基础 | ⭐ |
| 03 | [分支模型与团队工作流](03-Git分支模型与团队工作流.md) | 分支本质、GitFlow/GitHub Flow/GitLab Flow/Trunk、命名规范、发布流程 | ⭐⭐⭐ |
| 04 | [合并与变基](04-Git合并与变基.md) | merge 三策略、rebase -i、cherry-pick、三方合并、冲突解决 | ⭐⭐⭐ |
| 05 | [撤销与历史重写](05-Git撤销与历史重写.md) | reset 三态、revert、reflog、stash 基础、原子化提交 | ⭐⭐⭐ |
| 06 | [远程协作与托管平台](06-Git远程协作与托管平台.md) | remote、fetch/pull/push、Fork 工作流、PR、GitHub/GitLab/Gitee、LFS | ⭐⭐⭐ |
| 07 | [日常管理与工作区实践](07-Git日常管理与工作区实践.md) | 每日节奏、status/diff、add -p、stash 进阶、log 高级、bisect、GC | ⭐⭐ |
| 08 | [钩子自动化与定制](08-Git钩子自动化与定制.md) | Hooks 全表、husky/pre-commit、CI 集成、config 三层、别名 | ⭐⭐⭐⭐ |
| 09 | [标签子模块与Monorepo](09-Git标签子模块与Monorepo.md) | tag/SemVer/Releases、submodule/subtree、Monorepo 选型 | ⭐⭐⭐⭐ |
| 10 | [调试取证与排错](10-Git调试取证与排错.md) | bisect/blame 深潜、reflog 取证、陷阱清单、应急预案 | ⭐⭐⭐⭐ |
| 11 | [配置安全与最佳实践](11-Git配置安全与最佳实践.md) | GPG/SSH 签名、敏感信息清理、分支保护、团队规范模板 | ⭐⭐⭐⭐ |
| 12 | [工具链与IDE集成](12-Git工具链与IDE集成.md) | 四大客户端、VS Code/GitLens/JetBrains、终端工具、生态全景 | ⭐⭐ |

## 3. 学习路线推荐

```text
🟢 L1：日常够用（半天）
02-基础操作 → 05-撤销 → 07-日常管理
产出：clone/commit/push/pull/branch/merge/reset 无压力

🔵 L2：团队协作（1天）
03-分支模型 → 04-合并变基 → 06-远程协作
产出：熟练 GitFlow/GitHub Flow，能处理合并冲突和 PR Review

🟣 L3：精通（2天）
01-底层原理 → 10-调试取证 → 08-钩子自动化 → 11-安全最佳实践
产出：理解 Git 内部存储机制，能解决任何 Git 问题，能设计团队 Git 规范

🛠️ 工具与生态（按需）
12-工具链（桌面客户端/IDE）+ 09-标签与Monorepo（发布/多仓管理）
```

## 4. 主题速查索引

| 我想... | 看这个 |
|--------|--------|
| 撤销错误的 commit | 05-Git撤销与历史重写 |
| 解决合并冲突 | 04-Git合并与变基 |
| 理解 Git 内部原理 | 01-Git核心原理与对象模型 |
| 学习团队分支策略 | 03-Git分支模型与团队工作流 |
| 配置 Git 别名和工具 | 08-Git钩子自动化与定制 |
| 用 bisect 找 Bug | 10-Git调试取证与排错 |
| 管理子项目/大仓库 | 09-Git标签子模块与Monorepo |
| 自动化 Git 流程 | 08-Git钩子自动化与定制 |
| GitHub 协作全流程 | 06-Git远程协作与托管平台 |
| 定制 Git 显示效果 | 12-Git工具链与IDE集成 |
| 团队 Git 规范落地 | 11-Git配置安全与最佳实践 |

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| **Git 是什么** | 内容寻址的键值存储，键=SHA-1，值=对象 |
| **Blob** | 文件内容的快照，不含文件名 |
| **Tree** | 目录清单，指向 blob 和子树 |
| **Commit** | 一次快照 = tree 指针 + parent 指针 + 元信息 |
| **分支** | 指向 commit 的可移动指针（一个 41 字节文件） |
| **HEAD** | 当前所在位置的符号引用（指向分支或 commit） |
| **三区模型** | 工作区 → 暂存区（index）→ 版本库（objects） |
| **DAG** | commit 链构成的有向无环图，历史不可篡改的数学基础 |
| **Packfile** | 压缩存储（基准+增量），GC 产物 |
| **Remote** | 约定的同步点（origin/upstream） |
| **PR/MR** | 代码审查与合并的协作入口 |
| **Hooks** | 提交/推送等时机的钩子脚本 |
| **Rebase** | 重放提交使历史线性化（改写历史） |
| **Reflog** | HEAD 移动日志，90 天后悔药 |
| **LFS** | 大文件管理（指针文件 + 独立存储） |

## 6. 参考来源

- [Pro Git Book（官方免费书，中文）](https://git-scm.com/book/zh/v2)
- [Git 官方文档](https://git-scm.com/doc)
- [Conventional Commits 规范](https://www.conventionalcommits.org/zh-hans/)
- [SemVer 语义化版本](https://semver.org/lang/zh-CN/)

---

**下一模块**：[01-Git核心原理与对象模型](01-Git核心原理与对象模型.md)
