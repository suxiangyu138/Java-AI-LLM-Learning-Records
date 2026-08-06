# Git 底层原理深度剖析

## 为什么学底层原理？

理解了 Git 的数据模型，一切命令都变得透明：
- `git merge` 为什么有时会冲突？→ 理解了 commit DAG 就知道
- `git reset --hard` 数据还能找回吗？→ 理解了对象存储就知道
- 为什么分支切换这么快？→ 理解了指针机制就知道

---

## 1. Git 不是"版本控制系统"，是"内容寻址文件系统"

Git 本质上是一个 **键值对数据库**：写入内容 → 返回一个唯一的键（SHA-1 哈希），之后用这个键取出内容。

```bash
# 用底层命令体验：存入"hello world"
echo "hello world" | git hash-object -w --stdin
# 输出: 95d09f2b10159347eece71399a7e2e907ea3df4f

# 用这个哈希值查看内容
git cat-file -p 95d09f2b10159347eece71399a7e2e907ea3df4f
# 输出: hello world
```

核心设计：**一切皆对象，对象靠哈希寻址**。

---

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
               ┌────────┘     │     └─────────┐
               ▼              ▼               ▼
        ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
        │ Blob 对象   │ │ Blob 对象   │ │ Tree 对象   │
        │ 文件内容快照│ │ 文件内容快照│ │ 子目录      │
        └─────────────┘ └─────────────┘ └──────┬──────┘
                                               ▼
                                        ┌─────────────┐
                                        │ Blob 对象   │
                                        │ 文件内容快照│
                                        └─────────────┘
```

### 2.1 Blob（Binary Large Object）— 文件内容

- 只存储**文件内容**，不存储文件名、路径、权限
- 相同内容的文件只存一份（自动去重）

```bash
# 创建blob
echo "version 1" > file.txt && git add file.txt
# git add 时创建了一个blob对象，存入 .git/objects/

# 查看所有对象
find .git/objects -type f
# .git/objects/83/baae61804e65cc73a7201a7252750c76066a30
# .git/objects/95/d09f2b10159347eece71399a7e2e907ea3df4f

# 查看对象类型
git cat-file -t 83baae6
# 输出: blob
```

### 2.2 Tree — 目录结构

Tree 对象记录了**目录内容**：文件名、文件模式、指向 blob 或子树（subtree）的哈希指针。

```bash
# 查看最新commit的tree
git cat-file -p HEAD^{tree}
# 输出示例:
# 100644 blob e69de29bb2d1d6434b8b29ae775ad8c2e48c5391    README.md
# 040000 tree a1b2c3d4e5f6...                          src/
```

- `100644` 是文件模式（普通文件）
- `040000` 表示目录（指向另一个 tree 对象）

### 2.3 Commit — 一次提交的快照

每个 commit 对象包含：

```bash
git cat-file -p HEAD
# tree 4b825dc642cb6eb9a060e54bf899d9b0a82bb05e2
# parent a1b2c3d4e5f6...           # 父提交（首个提交没有这行）
# author suxiangyu <suxiangyu@example.com> 1716960000 +0800
# committer suxiangyu <suxiangyu@example.com> 1716960000 +0800
#
# feat: add user login module
```

| 字段 | 含义 |
|------|------|
| `tree` | 指向项目根目录的 tree 对象 |
| `parent` | 父提交的 SHA-1（可以有多个，merge commit 有2个） |
| `author` | 代码原作者 + 时间戳 |
| `committer` | 提交者 + 时间戳（cherry-pick/rebase 时可能与 author 不同） |
| `message` | 提交信息 |

### 2.4 Tag — 标签对象

分两种：

```bash
# 轻量标签：只是一个指向commit的引用（不是独立对象）
git tag v1.0.0

# 注解标签：创建独立的tag对象（推荐）
git tag -a v1.0.0 -m "Release v1.0.0"
```

注解标签是一个独立对象：

```bash
git cat-file -p v1.0.0
# object a1b2c3d4e5f6...    # 指向的commit
# type commit
# tag v1.0.0
# tagger suxiangyu <...> 1716960000 +0800
#
# Release v1.0.0
```

---

## 3. .git 目录结构

```text
.git/
├── HEAD              # 当前分支引用（ref: refs/heads/main）
├── config            # 仓库配置
├── description       # 仓库描述（GitWeb用）
├── index             # 暂存区（staging area）
├── hooks/            # 钩子脚本
├── objects/          # 所有对象存储（核心！）
│   ├── 00/           # 按哈希前2位分目录
│   ├── 01/
│   ├── ...
│   ├── info/
│   │   └── packs/    # packfile 索引
│   └── pack/         # 打包后的对象文件
└── refs/
    ├── heads/        # 分支引用（每个文件 = 一个分支，内容是commit SHA）
    ├── tags/         # 标签引用
    └── remotes/      # 远程分支引用
```

关键洞察：
- **refs/heads/main** 文件里就是一个 40 位的 SHA-1
- **分支 = 一个会移动的指针**
- **objects/** 是 Git 的"数据库"

---

## 4. SHA-1 哈希：Git 的信任基石

Git 使用 SHA-1 (160 bits, 40 hex chars) 对对象内容做哈希：

```text
SHA-1 = hash(对象类型 + 空格 + 内容长度 + 空字节 + 内容)
```

```bash
# 手动计算一个blob的SHA-1
echo -n "hello world" | git hash-object --stdin
# 95d09f2b10159347eece71399a7e2e907ea3df4f

# 等价于手动计算：
# SHA-1("blob 11\0hello world")
```

**重要特性：**
- 内容相同 → 哈希永远相同（去重的数学基础）
- 内容改变 → 哈希完全不同（雪崩效应）
- 哈希冲突概率极低，但 Git 已迁移到 SHA-256 作为可选方案

---

## 5. Commit 链：有向无环图（DAG）

```text
main 分支:    A ── B ──────── D ── E (merge commit) ── F
                                │
feature 分支:          C ──────┘
(从 B 分出，C 在 feature 上，D 在 main 上，E 合并两个分支)
```

- 每个 commit 指向前一个 parent，形成 **链表**
- merge commit 有**两个** parent，分支汇聚
- 整体是一个 **有向无环图（DAG）**，不可能出现循环

```bash
# 可视化DAG结构
git log --oneline --graph --all
# * e3f4g5h (HEAD -> main) feat: add search
# *   a1b2c3d Merge branch 'feature'
# |\
# | * d4e5f6g (feature) feat: login page
# * | h7i8j9k fix: header style
# |/
# * k1l2m3n init
```

---

## 6. Packfile 与垃圾回收（GC）

### 为什么需要 Packfile？

每次 `git add` 都创建一个**完整的 blob 快照**（不存增量）。时间久了 objects/ 目录会很大。

```bash
# 手动触发打包
git gc --auto

# 查看 pack 文件
ls .git/objects/pack/
# pack-xxx.idx   # 索引文件
# pack-xxx.pack  # 数据文件
```

Packfile 会：
1. 存储**基准版本 + 增量（delta）**
2. 大幅减少空间占用（可压缩到原来的 1/10）

### GC 的触发条件

Git 自动在以下情况触发 `git gc --auto`：
- objects/ 中松散对象超过 6700 个
- packfile 超过 50 个

### 悬空对象与清理

```bash
# 查看悬空对象（无引用的对象）
git fsck --lost-found

# 14天后未被引用的对象会被GC清理
# git reflog 默认保留90天，这是数据恢复的安全窗口
```

---

## 7. 内容追踪：Git 如何知道文件被修改了

```
工作区文件 ---- git add ----> 暂存区 (index) ---- git commit ----> 仓库 (objects)
                    |                    |
             计算SHA-1并创建blob    创建tree + commit对象
```

```bash
# index 文件存储了暂存区状态
git ls-files --stage
# 100644 83baae61804e65cc73a7201a7252750c76066a30 0       file.txt
# 模式  | SHA-1                               | stage | 文件名
```

`git status` 通过比较三个区域来工作：
1. **工作区 vs Index** → 红色（unstaged changes）
2. **Index vs HEAD** → 绿色（staged changes）

---

## 8. 底层命令 vs 上层命令

| 分类 | 命令示例 | 说明 |
|------|---------|------|
| **Porcelain**（上层） | `git add`, `git commit`, `git log` | 用户友好的高层命令 |
| **Plumbing**（底层） | `git hash-object`, `git cat-file`, `git update-index` | 底层原子操作 |

```bash
# 纯底层命令完成一次提交（相当于 git add + git commit）
# 1. 创建blob
HASH=$(echo "new content" | git hash-object -w --stdin)

# 2. 更新index
git update-index --add --cacheinfo 100644 $HASH file.txt

# 3. 创建tree
TREE=$(git write-tree)

# 4. 创建commit
git commit-tree $TREE -p HEAD -m "commit with plumbing"

# 5. 更新分支引用
git update-ref refs/heads/main <commit-hash>
```

---

## 9. 引用（Refs）的本质

引用就是指向 commit 的别名：

```bash
# 所有引用
cat .git/refs/heads/main     # 分支 → commit SHA
cat .git/refs/tags/v1.0.0    # 标签 → commit SHA (或tag对象)
cat .git/refs/remotes/origin/main  # 远程分支
cat .git/HEAD                 # HEAD → 分支引用或直接指向commit (detached)

# HEAD 的两种状态
# 正常状态: ref: refs/heads/main     (指向一个分支)
# Detached: a1b2c3d4...              (直接指向一个commit)
```

---

## 10. 核心总结

| 概念 | 一句话 |
|------|--------|
| **Git 是什么** | 内容寻址的键值存储，键=SHA-1，值=对象 |
| **Blob** | 文件内容的快照，不含文件名 |
| **Tree** | 目录清单，指向 blob 和 subtree |
| **Commit** | 一次快照 = tree指针 + parent指针 + 元信息 |
| **分支** | 指向 commit 的可移动指针 |
| **Index** | 下一次提交的"预演"区域 |
| **DAG** | commit 链构成的有向无环图 |
| **Packfile** | 压缩存储，基准+增量 |

## 常见问题

**Q: 两个文件内容相同，Git 存几份？**
→ 一份。blob 按内容哈希寻址，相同内容自动去重。

**Q: `git add` 后还能找回修改吗？**
→ 可以。只要创建了 blob，就存在 objects/ 中。用 `git fsck --lost-found` 找回。

**Q: 为什么 rebase 会改变 commit hash？**
→ commit hash 包含 parent 指针。rebase 改变了 parent，hash 必然变化。

**Q: Git 是不是存增量（diff）？**
→ 松散对象存的是**完整快照**，不是增量。但 packfile 会做增量压缩优化。

---

>下一篇：[03-Git分支模型与团队工作流](03-Git分支模型与团队工作流.md)
