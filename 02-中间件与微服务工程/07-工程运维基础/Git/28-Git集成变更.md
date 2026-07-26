# 28-Git集成变更
> 🎯 合并是Git协作的心脏 — 掌握merge vs rebase的底层差异、三方合并机制、冲突解决策略、以及安全回滚合并的方法

---

## 目录
1. [集成变更的两种方式](#1-集成变更的两种方式)
2. [三方合并的底层原理](#2-三方合并的底层原理)
3. [冲突解决完整指南](#3-冲突解决完整指南)
4. [合并策略选择](#4-合并策略选择)
5. [撤销合并](#5-撤销合并)
6. [Java后端集成实战](#6-java后端集成实战)

---

## 1. 集成变更的两种方式

### 1.1 merge vs rebase 本质区别

```text
场景：main 有 B→C，feature 从 B 拉出后有 D→E

merge（三方合并）:
  main:    A---B---C
                \     
  feature:       D---E
  结果:     A---B---C---M   ← M是merge commit
                \     /
                 D---E
  历史保留真实分支拓扑

rebase（变基）:
  main:    A---B---C
                \
  feature:       D---E
  结果:     A---B---C---D'---E'   ← 线性历史
  D'和E'是全新的commit（SHA-1变了）
```

| 维度 | merge | rebase |
|------|-------|--------|
| 历史形状 | 保留分支拓扑 | 线性干净 |
| commit SHA | 不变 | 全新生成 |
| 冲突解决 | 一次性 | 逐commit解决 |
| 公共分支安全 | ✅ | ❌ 禁止 |
| 适用场景 | 公共分支合并 | 个人分支整理 |

### 1.2 快进合并

```bash
# 当feature分支可以直接接在main后面时（main没有新commit）
git merge feature
# Fast-forward: 直接移动main指针到feature → 无merge commit

# 企业级建议：禁止快进，保留合并记录
git merge --no-ff feature
# 即使能快进也生成merge commit → 历史可追溯
```

---

## 2. 三方合并的底层原理

### 2.1 三方是哪三方

```text
三方合并 (Three-Way Merge):

  Commit BASE (共同祖先)
      │
      ├──→ Commit LOCAL (当前分支，如main的HEAD)
      │
      └──→ Commit REMOTE (要合并进来的分支，如feature的HEAD)

Git自动合并逻辑：
  BASE和LOCAL相同，REMOTE改了 → 用REMOTE的（对方改了）
  BASE和REMOTE相同，LOCAL改了 → 用LOCAL的（我方改了）
  BASE和LOCAL和REMOTE都不一样 → 冲突（需要人工判断）
  BASE没这个文件，只有一方有 → 用有的一方
```

### 2.2 冲突标记解读

```text
合并冲突时Git在文件中插入标记：

<<<<<<< HEAD                  ← 当前分支(main)的内容
public void processOrder(Order order) {
    validate(order);           ← 当前分支的代码
    orderRepository.save(order);
=======                        ← 分隔线
public void processOrder(Order order) {
    validateOrder(order);      ← feature分支的代码
    paymentService.pay(order); ← feature新增了支付逻辑
    orderRepository.save(order);
>>>>>>> feature/payment        ← 来源分支名
```

---

## 3. 冲突解决完整指南

### 3.1 冲突产生原因

| 原因 | 场景 | 预防 |
|------|------|------|
| 两人改同一文件的同一行 | 并行开发同一模块 | 模块职责清晰、提前沟通 |
| 一人删文件另一人改文件 | 重构+功能开发同时进行 | 重构前通知团队 |
| 配置文件冲突 | 两人加了不同的配置项 | 配置按模块分区组织 |
| 依赖版本冲突(pom.xml) | 两人升级了同一个依赖 | 依赖管理集中化 |

### 3.2 解决流程

```bash
# 1. 发起合并
git merge feature/order-module
# 冲突！Git提示：CONFLICT in OrderService.java

# 2. 查看冲突文件
git status                  # 显示 both modified 的文件

# 3. 解决冲突（三选一）
# 方式A：手动编辑 → 删除 <<< === >>> 标记 → 保留正确的代码
# 方式B：IDEA可视化合并（推荐！） → 三栏对比 → 点击接受左侧/右侧/两者
# 方式C：选择一方完全接受
git checkout --ours OrderService.java    # 全部用当前分支的
git checkout --theirs OrderService.java  # 全部用对方分支的

# 4. 标记已解决
git add OrderService.java

# 5. 完成合并
git commit          # Git自动生成merge commit message
# 或 git merge --continue
```

### 3.3 合并工具配置

```bash
# VS Code 作为合并工具
git config --global merge.tool "code --wait"
git config --global mergetool.prompt false

# 启动合并工具
git mergetool

# IntelliJ IDEA（推荐，三栏可视化）
# VCS → Git → Resolve Conflicts → 左(ours)/中(base)/右(theirs) → 点击合并
```

---

## 4. 合并策略选择

### 4.1 merge 的合并策略

```bash
# 默认策略（ort，Git 2.34+）
git merge feature

# 明确指定策略
git merge -s ort feature           # 默认，快且准确
git merge -s recursive feature     # 老默认策略

# 策略选项
git merge -X theirs feature        # 冲突时自动选对方
git merge -X ours feature          # 冲突时自动选我方
git merge -X ignore-space-change   # 忽略空白差异

# 压缩合并（所有commit压成1个）
git merge --squash feature
git commit -m "feat: 合并订单模块（squash）"
```

### 4.2 PR/MR合并选项

| 选项 | 命令 | 历史效果 | 适用 |
|------|------|----------|------|
| **Merge Commit** | `git merge --no-ff` | 保留分支拓扑 + 合并节点 | ✅ 推荐，历史最清晰 |
| **Squash and Merge** | `git merge --squash` | 所有commit压为1个 | 小功能、清理杂乱提交 |
| **Rebase and Merge** | `git rebase` + `git merge --ff` | 线性历史 | 追求线性历史 |

```text
GitHub/GitLab PR合并按钮对应：
  "Create a merge commit"  → Merge Commit（保留完整历史）
  "Squash and merge"       → Squash（压成1个commit）
  "Rebase and merge"       → Rebase（线性历史）

推荐：默认用 Merge Commit
  理由：分支历史可追溯，每个commit的SHA-1不变
  例外：feature分支commit杂乱时用 Squash
```

---

## 5. 撤销合并

### 5.1 撤销未推送的合并

```bash
# merge后还没push，想撤销
git reset --hard HEAD~1        # 回退到merge前
# 或
git reset --hard ORIG_HEAD     # ORIG_HEAD记录危险操作前的状态
```

### 5.2 撤销已推送的合并（安全方式）

```bash
# merge commit已push → 必须用revert，不能用reset！
git revert -m 1 <merge-commit-hash>

# -m 1: 保留main分支（被合并进来的那一边是1号，被合并的那边是2号）
# 这会产生一个新的commit来"取消"merge的效果
# 历史完整保留，安全可审计
```

> ⚠️ 已push的merge commit绝对不能用`git reset`撤销——那会改写公共历史，导致其他人的仓库与你不同步。

---

## 6. Java后端集成实战

### 6.1 feature分支合并到develop

```bash
# 标准流程
git switch develop
git pull --rebase                        # 先同步最新的develop
git merge --no-ff feature/order-module   # 合并feature
# 解决冲突（如有）
mvn clean verify                          # 合并后务必编译验证
git push origin develop
git branch -d feature/order-module        # 清理已合并分支
```

### 6.2 hotfix合并到main和develop

```bash
# hotfix需要同时合并到main和develop
git switch main
git merge --no-ff hotfix/critical-fix
git push origin main

git switch develop
git merge --no-ff hotfix/critical-fix
# 如果有冲突（develop比main多了代码）→ 手动解决
git push origin develop

git branch -d hotfix/critical-fix
```

### 6.3 pom.xml冲突处理

```xml
<!-- pom.xml冲突常见模式

<<<<<<< HEAD
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>module-a</artifactId>
        <version>1.2.0</version>
    </dependency>
=======
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>module-b</artifactId>
        <version>2.0.0</version>
    </dependency>
>>>>>>> feature/module-b
-->

<!-- 解决：两者都需要保留，去除冲突标记 -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>module-a</artifactId>
        <version>1.2.0</version>
    </dependency>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>module-b</artifactId>
        <version>2.0.0</version>
    </dependency>
```

---

> 🎯 **集成的核心素养**：理解merge不是"自动合并命令"，而是"人工判断+自动辅助"。遇到冲突时，你的任务是理解两边代码的意图，做出正确的业务决策。
