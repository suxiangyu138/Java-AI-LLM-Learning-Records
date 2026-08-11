# DEVELOPMENT.md — 开发说明文档

> 🚀 本仓库（Java 后端 + AI 大模型知识库，5,752 篇文档）的 Git 分支策略与开发规范。基于 **Git Flow** 适配文档维护场景：常驻 `main` + `develop`，动态 `feature/release/hotfix`
>
> 📅 最后更新：2026-08-12

---

## 📚 目录

1. [分支体系总览](#1-分支体系总览)
2. [分支职责说明](#2-分支职责说明)
3. [日常工作流](#3-日常工作流)
4. [发布流程](#4-发布流程)
5. [紧急修复流程](#5-紧急修复流程)
6. [提交规范](#6-提交规范)
7. [PR 与评审规范](#7-pr-与评审规范)
8. [命令速查表](#8-命令速查表)
9. [GitHub 保护规则建议](#9-github-保护规则建议)

---

## 1. 分支体系总览

```text
main（生产/展示分支）────────────────────────────────────
   │      ▲            ▲              ▲
   │      │ 合并        │ 合并          │ 合并
   ├── release/1.x ────┘              │  ← 发布分支（动态）
   │      │                           │
   ├── hotfix/xxx ────────────────────┘  ← 热修复分支（动态）
   │
develop（开发集成分支）─────────────────────────────────
   │      ▲          ▲
   │      │ 合并      │ 合并
   ├── feature/xxx ──┘              ← 功能分支（动态）
   └── feature/yyy ──────────────── ← 功能分支（动态）
```

| 分支 | 类型 | 生命周期 | 职责 |
|------|:---:|:---:|------|
| **main** | 常驻 | 永久 | 生产/展示分支——始终可发布，每个提交有完整内容 |
| **develop** | 常驻 | 永久 | 开发集成分支——日常开发的主战场 |
| **feature/xxx** | 动态 | 临时（天/周） | 单个知识体系/主题的开发 |
| **release/1.x** | 动态 | 临时（天） | 发布准备（review + 修正） |
| **hotfix/xxx** | 动态 | 临时（小时/天） | 紧急修正（如文档链接断裂/错误内容） |

> 🎯 **本仓库约定**：文档仓库「发布」= 内容稳定可展示。`main` 是 GitHub 默认展示分支——**日常合并完成后，main 即为最新稳定版本**。
>
> 🏁 **阶段落幕（2026-08-12）**：批量扩建阶段结束（5,752 篇，280 次提交收官）。仓库转入**维护模式**——只接受内容修正（死链/错字/过时校准），不再批量新建知识体系；新体系按需以 `feature` 分支创建。

---

## 2. 分支职责说明

### 2.1 main（生产/展示分支）

```text
职责：GitHub 展示的稳定内容（用户看到的就是 main）
规则：
├── 禁止直接推送（只收 PR）
├── 每个提交必须内容完整（不提交半成品）
├── 合并来源：release 分支 / hotfix 分支
└── 对应环境：线上展示（GitHub 默认分支）

本仓库实践：
├── 历史提交：docs: daily contribution #N（内容已稳定）
└── main = 知识库的「已发布版本」
```

### 2.2 develop（开发集成分支）

```text
职责：所有日常开发的集散地
规则：
├── 禁止直接推送（只收 PR）
├── 合并来源：feature 分支
├── 内容可「进行中」（但每个 PR 独立完整）
└── 对应环境：开发/预览

本仓库实践：
├── 新知识体系在 feature 分支开发
├── 完成一个体系 → PR 合入 develop
└── develop 内容持续累积（不急于合 main）
```

### 2.3 feature/xxx（功能分支——动态）

```text
职责：单个知识体系/主题的开发
命名：feature/<体系名>（如 feature/正则表达式）
规则：
├── 从 develop 拉出
├── 独立开发（不互相干扰）
├── 完成后 PR → develop（squash 合并）
└── 合并后删除（不留垃圾分支）

本仓库实践：
├── 一个知识体系 = 一个 feature 分支
├── 体系内多篇文档分多次提交（遵循提交规范）
└── 全部完成 → 合并 develop → 删除分支
```

### 2.4 release/1.x（发布分支——动态）

```text
职责：发布前的「冻结期」——review + 修正
命名：release/<版本号>（如 release/1.0）
规则：
├── 从 develop 拉出
├── 只修 bug/错误，不加新功能
├── 完成后双向合并：→ main（发布）+ → develop（同步修正）
└── 合并后删除

本仓库实践：
├── 文档错误修正（死链/错字/过时内容）
├── 结构性问题（目录/索引）统一处理
└── 触发时机：阶段性里程碑（如 5,752 篇收官、知识库结构调整）

🏁 维护模式下的发布：
├── 批量扩建阶段已落幕（2026-08-12，5,752 篇收官）
├── 日常维护：修正类提交直接合并 main（保持展示分支稳定）
└── 新体系：按需创建（feature → develop → 里程碑合入 main）
```

### 2.5 hotfix/xxx（热修复分支——动态）

```text
职责：紧急修正（内容错误/链接断裂/安全隐患）
命名：hotfix/<描述>（如 hotfix/fix-broken-links）
规则：
├── 从 main 拉出（绕过 develop 直接修）
├── 完成双向合并：→ main（立即生效）+ → develop（同步）
└── 合并后删除

本仓库实践：
├── 严重内容错误（误导性技术信息）
├── 大规模链接失效
└── 文件被误删/损坏
```

---

## 3. 日常工作流

### 3.1 开发新知识体系（标准流程）

```bash
# ① 同步 develop
git checkout develop
git pull origin develop

# ② 创建 feature 分支
git checkout -b feature/正则表达式

# ③ 开发（小步提交——遵循提交规范）
git add "正则表达式/00-xxx.md"
git commit -m "docs: 新增 正则表达式 知识体系（8篇）"
# ... 多次提交（每个体系一个提交或分阶段提交）

# ④ 推送 + PR
git push origin feature/正则表达式
# GitHub 上创建 PR：feature/正则表达式 → develop

# ⑤ 评审通过 → squash 合并 → 删除分支
git checkout develop && git branch -d feature/正则表达式
```

### 3.2 日常小更新（个人仓库简化版）

```bash
# 本仓库个人维护场景（无团队评审时）：
# 方案 A（推荐）：直接提交 develop
git checkout develop
git add -A
git commit -m "docs: 新增 xxx 知识体系"
git push origin develop

# 方案 B（正式）：走 feature → PR → develop
# 定期把 develop 合入 main（里程碑时）：
git checkout main
git merge develop
git push origin main
```

---

## 4. 发布流程

```text
触发时机：阶段性里程碑（如新增 N 个知识体系后）
流程：
① 从 develop 拉 release 分支
   git checkout -b release/1.0 develop

② 冻结期（只修不改加）：
   ├── 检查死链（所有文件可访问）
   ├── 检查目录/索引一致性
   └── 检查格式规范（表格/代码块/锚点）

③ 合并发布：
   git checkout main && git merge release/1.0
   git push origin main

④ 同步回 develop：
   git checkout develop && git merge release/1.0
   git push origin develop

⑤ 删除 release 分支
```

---

## 5. 紧急修复流程

```bash
# ① 从 main 拉 hotfix（绕过 develop——立即生效）
git checkout main
git pull origin main
git checkout -b hotfix/fix-broken-links

# ② 修复 + 提交
git add "01-底层根基-Java核心底座/xx/xx.md"
git commit -m "docs: 修复 xx 文档链接失效"

# ③ 合并到 main（立即生效）
git checkout main && git merge hotfix/fix-broken-links
git push origin main

# ④ 同步到 develop（防止修复丢失）
git checkout develop && git merge hotfix/fix-broken-links
git push origin develop

# ⑤ 删除分支
git branch -d hotfix/fix-broken-links
```

---

## 6. 提交规范

### 6.1 提交信息格式

```text
<type>(<scope>): <subject>

<body>（可选——为什么/怎么验证）

格式遵循 Conventional Commits：
├── docs     文档变更（本仓库主类型）
├── feat     新功能（本仓库 = 新知识体系）
├── fix      修复（本仓库 = 内容修正/链接修复）
├── refactor 重构（本仓库 = 结构调整）
├── chore    杂项（依赖/配置）
└── build    构建相关
```

### 6.2 本仓库提交示例

```text
# 新增知识体系
docs: 新增 正则表达式 知识体系（8篇）
- 覆盖：基础语法/进阶/引擎原理/ReDoS防护/Java API/实战/面试
- 遵循企业文档标准（背景/边界/权衡/工程细节）

# 内容修正
fix: 修复 xx 文档中过时的端口号信息

# 日常更新（历史习惯保留）
docs: daily contribution #N
```

### 6.3 提交纪律

```text
✅ 必须：
├── 一个提交 = 一个逻辑单元（一个体系/一个修正）
├── 信息清晰（做了什么 + 为什么）
├── 可编译/可验证（文档可访问、无死链）
└── 中文内容 + 英文术语（遵循仓库规范）

❌ 禁止：
├── 提交半成品（未完成的体系）
├── 提交临时文件（.tmp/备份）
├── 提交敏感信息（密钥/密码）
└── 无信息提交（"update"/"修改"）
```

---

## 7. PR 与评审规范

### 7.1 PR 规范（本仓库）

```text
PR 模板：
## 变更内容
（新增/修改了哪些知识体系）

## 为什么
（背景/目的）

## 验证方式
（链接检查/格式检查/文档规范）

## 影响范围
（涉及哪些目录/是否影响索引）

PR 要求：
├── 小步（一个体系一个 PR）
├── 描述清晰（模板填写）
├── 无死链（检查通过）
└── 遵循文档规范（CLAUDE.md 标准）
```

### 7.2 评审检查清单

```text
评审六查（对应 CLAUDE.md 文档规范）：
[ ] 结构：00 总览 + 模块编号正确？
[ ] 格式：表格/代码块语言标注/引用块？
[ ] 深度：200-350 行/篇？含边界/权衡/工程细节？
[ ] 规范：锚点链接正确？交叉引用有效？
[ ] 一致：与仓库既有体系风格一致？
[ ] 无死链：所有相对链接可访问？
```

---

## 8. 命令速查表

```bash
# ═══════════ 分支管理 ═══════════
git branch -a                        # 查看全部分支（含远程）
git branch -r                        # 远程分支
git checkout develop                 # 切换分支
git checkout -b feature/xxx develop  # 从 develop 创建功能分支

# ═══════════ 日常开发 ═══════════
git pull origin develop              # 同步
git add -A                           # 暂存
git commit -m "docs: ..."            # 提交
git push origin feature/xxx          # 推送

# ═══════════ 合并 ═══════════
git checkout develop && git merge feature/xxx   # 合并
git branch -d feature/xxx                        # 删除本地
git push origin --delete feature/xxx             # 删除远程

# ═══════════ 发布/修复 ═══════════
git checkout -b release/1.0 develop  # 发布分支
git checkout -b hotfix/xxx main      # 热修复分支（从 main）

# ═══════════ 同步 main 与 develop ═══════════
git checkout main && git merge develop && git push origin main
```

---

## 9. GitHub 保护规则建议

> ⚠️ 在 GitHub 仓库 Settings → Branches → Add rule 配置：

```text
main 分支保护规则：
├── ✅ Require a pull request before merging
│   └── 禁止直接推送（必须 PR）
├── ✅ Require status checks（若配置 CI）
│   └── 链接检查/格式校验通过才可合并
├── ✅ Do not allow bypassing（管理员也遵守）
└── 可选：Require conversation resolution（评审完成）

develop 分支保护规则：
├── ✅ Require a pull request before merging
└── （个人仓库可放宽——直接推送）

个人维护建议：
├── 个人仓库：develop 可直推（无团队评审成本）
├── 团队仓库：main/develop 都走 PR
└── 里程碑：main 合入前过一遍「发布检查」
```

---

> 🎯 **一句话总结**：本仓库 Git 策略 = **main（展示）+ develop（集成）+ feature/release/hotfix（动态）**——个人维护时 develop 直推、main 走里程碑合并；团队协作时全部走 PR。核心是：**main 永远稳定、develop 永远前进、动态分支用完即删**。

---

**相关文档**：[README.md](./README.md) | [CLAUDE.md](./CLAUDE.md) | [DEV-ENVIRONMENT.md](./DEV-ENVIRONMENT.md)
