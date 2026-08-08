# 01-Git 核心原理与对象模型
> Git 不是"版本控制系统"，是"内容寻址文件系统"——理解四种对象、三区模型与 DAG，才能在任何"搞砸了"的情况下找回代码

## 📚 目录
1. [Git 本质：内容寻址文件系统](#1-git-本质内容寻址文件系统)
2. [四种 Git 对象](#2-四种-git-对象)
3. [.git 目录结构](#3-git-目录结构)
4. [三区模型与文件状态](#4-三区模型与文件状态)
5. [Commit 链：有向无环图（DAG）](#5-commit-链有向无环图dag)
6. [HEAD / 索引 / refs](#6-head--索引--refs)
7. [Packfile 与垃圾回收](#7-packfile-与垃圾回收)
8. [底层命令 vs 上层命令](#8-底层命令-vs-上层命令)
9. [核心概念速查与 FAQ](#9-核心概念速查与-faq)
10. [参考来源](#10-参考来源)

## 1. Git 本质：内容寻址文件系统

> 🎯 **核心论断**：Git 本质是**键值对数据库**——写入内容 → 返回唯一键（SHA-1 哈希）→ 用键取出内容。"一切皆对象，对象靠哈希寻址"。

**开篇动机三问**（理解原理的价值）：

| 问题 | 原理答案 |
|------|---------|
| `git merge` 为什么有时冲突？ | commit 构成 DAG，合并要找共同祖先 |
| `git reset --hard` 数据还能找回吗？ | 对象仍在 objects/，reflog 可找回 |
| 为什么分支切换这么快？ | 分支只是指针，切换 = 移动指针 + 更新工作区 |

### 1.1 哈希命令验证

```bash
# 用底层命令体验：存入"hello world"
echo "hello world" | git hash-object -w --stdin
# 输出: 95d09f2b10159347eece71399a7e2e907ea3df4f

# 用这个哈希值查看内容
git cat-file -p 95d09f2b10159347eece71399a7e2e907ea3df4f
# 输出: hello world
```

### 1.2 SHA-1 计算规则

```text
SHA-1 = hash(对象类型 + 空格 + 内容长度 + 空字节 + 内容)
示例：blob 对象的哈希 = SHA-1("blob 11\0hello world")
```

| SHA-1 特性 | 意义 |
|-----------|------|
| 内容相同 → 哈希相同 | 自动去重的数学基础 |
| 内容改变 → 哈希完全不同 | 雪崩效应，保证完整性 |
| 冲突概率极低 | Git 已支持迁移 SHA-256 |

## 2. 四种 Git 对象

```text
                     ┌─────────────────┐
                     │   Tag 对象       │
                     │ (annotated tag)  │
                     └────────┬────────┘
                              ▼
                    ┌───────────────────┐
                    │  Commit 对象       │
                    │ author / message   │
                    │ tree 指针          │
                    └────────┬──────────┘
                             ▼
                    ┌───────────────────┐
                    │  Tree 对象         │
                    │ 文件名 + 指针列表  │
                    └───┬─────┬─────┬───┘
                        │     │     │
                        ▼     ▼     ▼
                 ┌─────────────┐ ┌─────────────┐
                 │ Blob 对象   │ │ Tree 对象   │
                 │ 文件内容快照│ │ 子目录      │
                 └─────────────┘ └──────┬──────┘
                                        ▼
                                 ┌─────────────┐
                                 │ Blob 对象   │
                                 └─────────────┘
```

### 2.1 Blob — 文件内容

- 只存储文件内容，**不存文件名、路径、权限**；
- 相同内容只存一份（自动去重）。

```bash
echo "version 1" > file.txt && git add file.txt   # add 时创建 blob

find .git/objects -type f                          # 查看所有对象
git cat-file -t 83baae6                            # 查看对象类型 → blob
```

### 2.2 Tree — 目录结构

记录目录内容：文件名、文件模式、指向 blob 或子树的哈希指针。

```bash
git cat-file -p HEAD^{tree}
# 100644 blob e69de29bb2d1d6434b8b29ae775ad8c2e48c5391    README.md
# 040000 tree a1b2c3d4e5f6...                          src/
```

> 💡 `100644` = 普通文件模式；`040000` = 目录（指向另一个 tree 对象）。

### 2.3 Commit — 一次提交的快照

```bash
git cat-file -p HEAD
# tree 4b825dc642cb6eb9a060e54bf899d9b0a82bb05e2
# parent a1b2c3d4e5f6...
# author suxiangyu <suxiangyu@example.com> 1716960000 +0800
# committer suxiangyu <suxiangyu@example.com> 1716960000 +0800
#
# feat: add user login module
```

| 字段 | 含义 |
|------|------|
| `tree` | 指向项目根目录的 tree 对象（文件目录快照） |
| `parent` | 父提交 SHA-1（merge commit 有 2 个；首个提交无） |
| `author` | 代码原作者 + 时间戳（**谁写的**） |
| `committer` | 提交者 + 时间戳（**谁提交的**；cherry-pick/rebase 时两者不同） |
| `message` | 提交信息 |

### 2.4 Tag — 标签对象

```bash
git tag v1.0.0                          # 轻量标签：仅一个指向 commit 的引用
git tag -a v1.0.0 -m "Release v1.0.0"   # 注解标签：创建独立 tag 对象（推荐）

git cat-file -p v1.0.0                  # 注解标签内部结构
# object a1b2c3d4e5f6...
# type commit
# tag v1.0.0
# tagger suxiangyu <...> 1716960000 +0800
# Release v1.0.0
```

## 3. .git 目录结构

```text
.git/
├── HEAD              # 当前分支引用（ref: refs/heads/main）
├── config            # 仓库配置
├── description       # 仓库描述（GitWeb 用）
├── index             # 暂存区（staging area，二进制）
├── hooks/            # 钩子脚本（.sample 模板）
├── objects/          # 所有对象存储（核心！）
│   ├── 00/           # 按哈希前 2 位分目录
│   ├── ...
│   ├── info/
│   └── pack/         # 打包后的对象文件（packfile）
└── refs/
    ├── heads/        # 分支引用（每个文件 = 一个分支，内容是 commit SHA）
    ├── tags/         # 标签引用
    └── remotes/      # 远程分支引用
```

> 💡 关键洞察：`refs/heads/main` 文件里就是一个 40 位 SHA-1；**分支 = 一个会移动的指针**；`objects/` 是 Git 的"数据库"。

## 4. 三区模型与文件状态

### 4.1 三个区域

| 区域 | 英文 | 说明 | 存储位置 |
|------|------|------|---------|
| **工作区** | Working Directory | 你实际编辑文件的地方 | 文件系统 |
| **暂存区** | Staging Area (Index) | 下次提交的快照清单 | `.git/index` |
| **版本库** | Repository (.git) | 所有提交历史的数据库 | `.git/objects/` |

### 4.2 三种文件状态与流转

| 文件状态 | 说明 | 所在区域 |
|----------|------|---------|
| **Modified** | 已修改但尚未加入暂存区 | 工作区 |
| **Staged** | 已加入暂存区，等待提交 | 暂存区 |
| **Committed** | 已提交到版本库，安全存储 | 版本库 |

```text
        git add              git commit
  ┌────────────┐  ────────►  ┌────────────┐  ────────►  ┌────────────┐
  │  Working   │  staging    │  Staging   │  snapshot   │ Repository │
  │ Directory  │  file       │   Area     │  commit     │  (.git)    │
  └────────────┘  ◄────────  └────────────┘             └────────────┘
       │           git         ▲
       │        restore        │
       │        --staged       │ git checkout
       │                       │ (switch branch)
       ▼                       │
  (Modified)                   │
       │                       │
       └───────────────────────┘
         git restore (丢弃修改)
```

### 4.3 设计哲学与内容追踪

- **暂存区是 Git 最巧妙的设计之一**：一次提交只包含部分修改，实现"原子化提交"（改了 A、B 两文件，只想先提交 A：`git add A` 后 `git commit`，B 留待下次）。
- 内容追踪链路：`工作区 --git add--> 暂存区(index) --git commit--> 仓库(objects)`，add 时计算 SHA-1 创建 blob，commit 时创建 tree + commit 对象。

```bash
git ls-files --stage
# 100644 83baae61804e65cc73a7201a7252750c76066a30 0       file.txt
# 模式  | SHA-1                               | stage | 文件名

git status   # 通过比较三个区域工作：
             # 工作区 vs Index → 红色（unstaged）；Index vs HEAD → 绿色（staged）
```

## 5. Commit 链：有向无环图（DAG）

```text
main 分支:    A ── B ──────── D ── E (merge commit) ── F
                                │
feature 分支:          C ──────┘
```

- 每个 commit 指向前一个 parent，形成链表；merge commit 有**两个** parent，分支汇聚；整体是**有向无环图（DAG）**。
- **不可篡改的数学基础**：修改任何 commit → SHA-1 改变 → 所有后代 commit 的 SHA-1 **级联改变**。

```bash
git log --oneline --graph --all
# * e3f4g5h (HEAD -> main) feat: add search
# *   a1b2c3d Merge branch 'feature'
# |\
# | * d4e5f6g (feature) feat: login page
# * | h7i8j9k fix: header style
# |/
# * k1l2m3n init
```

## 6. HEAD / 索引 / refs

### 6.1 HEAD 符号引用

```bash
cat .git/HEAD
# ref: refs/heads/master        ← 正常状态：指向分支

git checkout 8a2f8c9
cat .git/HEAD
# 8a2f8c9b3d1e5f6a7b8c9d0e1f2a3b4c5d6e7f8a   ← detached：直接指向 commit
```

> ⚠️ **分离头指针风险**：HEAD 直接指向 commit 时，新提交不属于任何分支；切换分支后这些提交将"悬空"，最终被 GC 清理。保留方式：`git checkout -b new-branch-name`。

### 6.2 refs 本质

```bash
cat .git/refs/heads/main             # 分支 → commit SHA
cat .git/refs/tags/v1.0.0            # 标签 → commit SHA（或 tag 对象）
cat .git/refs/remotes/origin/main    # 远程分支

git branch -v                        # 查看所有分支及其指向的 commit
git branch -vv                       # 查看分支追踪关系
git branch --contains 8a2f8c9        # 查看 commit 所在的分支
```

> 💡 **分支成本对比**：Git 创建分支 = 创建指针 = 毫秒级（一个 41 字节文件）；SVN 创建分支 = 拷贝目录 = 秒到分级。正因分支成本趋近于零，Git Flow、GitHub Flow 等工作流才成为可能。

## 7. Packfile 与垃圾回收

### 7.1 为什么需要 Packfile

每次 `git add` 都创建**完整的 blob 快照**（不存增量），时间久了 `objects/` 会很大。Packfile 通过"基准版本 + 增量（delta）"压缩，可压缩到原来的约 1/10。

```bash
git gc --auto                 # 手动触发打包（自动触发条件见下）
ls .git/objects/pack/
# pack-xxx.idx   # 索引文件
# pack-xxx.pack  # 数据文件
```

### 7.2 GC 自动触发条件与悬空对象

| 触发条件 | 阈值 |
|----------|------|
| 松散对象数量 | > 6700 个 |
| packfile 数量 | > 50 个 |

```bash
git fsck --lost-found         # 查看悬空对象（无引用的对象）
```

> 💡 **数据恢复窗口**：悬空对象 14 天后被 GC 清理；`git reflog` 默认保留 **90 天**——这是所有"误操作找回"的安全窗口。

## 8. 底层命令 vs 上层命令

| 分类 | 命令示例 | 说明 |
|------|---------|------|
| **Porcelain**（上层） | `git add`、`git commit`、`git log` | 用户友好的高层命令 |
| **Plumbing**（底层） | `git hash-object`、`git cat-file`、`git update-index` | 底层原子操作 |

用纯底层命令完成一次提交（等价 `git add` + `git commit`）：

```bash
# 1. 创建 blob
HASH=$(echo "new content" | git hash-object -w --stdin)
# 2. 更新 index
git update-index --add --cacheinfo 100644 $HASH file.txt
# 3. 创建 tree
TREE=$(git write-tree)
# 4. 创建 commit
git commit-tree $TREE -p HEAD -m "commit with plumbing"
# 5. 更新分支引用
git update-ref refs/heads/main <commit-hash>
```

## 9. 核心概念速查与 FAQ

| 概念 | 一句话 |
|------|--------|
| **Git 是什么** | 内容寻址的键值存储，键=SHA-1，值=对象 |
| **Blob** | 文件内容的快照，不含文件名 |
| **Tree** | 目录清单，指向 blob 和子树 |
| **Commit** | 一次快照 = tree 指针 + parent 指针 + 元信息 |
| **分支** | 指向 commit 的可移动指针 |
| **Index** | 下一次提交的"预演"区域 |
| **DAG** | commit 链构成的有向无环图 |
| **Packfile** | 压缩存储，基准+增量 |

| FAQ | 答案 |
|-----|------|
| 两个文件内容相同，Git 存几份？ | 一份。blob 按内容哈希寻址，自动去重 |
| `git add` 后还能找回修改吗？ | 可以。只要创建了 blob 就在 objects/ 中，`git fsck --lost-found` 找回 |
| 为什么 rebase 会改变 commit hash？ | commit hash 包含 parent 指针；rebase 改变 parent，hash 必然变化 |
| Git 是不是存增量（diff）？ | 松散对象存**完整快照**；packfile 才做增量压缩优化 |

## 10. 参考来源

- [Pro Git Book（第 10 章 Git 内部原理）](https://git-scm.com/book/zh/v2)
- [Git 官方文档（hash-object/cat-file 等）](https://git-scm.com/docs)
- [git-scm.com：Git 对象](https://git-scm.com/book/zh/v2/Git-内部原理-Git-对象)

---

**下一模块**：[02-Git基础操作与入门](02-Git基础操作与入门.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
