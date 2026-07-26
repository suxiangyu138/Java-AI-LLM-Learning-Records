# 09 - GitHub 组织管理与开源协作

> 从个人的仓库到组织的团队协作，从闭源开发到开源社区贡献——GitHub 组织功能是规模化协作的基础。掌握组织管理和开源规范，是技术 Leader 的必经之路。

---

## 目录

1. [GitHub Organization 概述](#1-github-organization-概述)
2. [Teams 团队与权限管理](#2-teams-团队与权限管理)
3. [仓库权限模型](#3-仓库权限模型)
4. [开源项目治理规范](#4-开源项目治理规范)
5. [开源协作完整流程](#5-开源协作完整流程)
6. [InnerSource 内部开源实践](#6-innersource-内部开源实践)
7. [常见面试题](#7-常见面试题)

---

## 1. GitHub Organization 概述

### 1.1 Organization vs Personal Account

```
┌─────────────────────────────────────────────────────────┐
│                  Organization 的作用                       │
│                                                          │
│  Personal Account (个人)     Organization (组织)           │
│  ├── 单人所有                ├── 团队共有                   │
│  ├── 无层级结构              ├── Teams + Members 层级       │
│  ├── 无审计日志              ├── 审计日志（Enterprise）      │
│  ├── 单一计费                ├── 集中计费管理                │
│  └── 简单协作                └── 精细权限控制                │
└─────────────────────────────────────────────────────────┘
```

### 1.2 组织类型

| 类型 | 适用场景 | 仓库 | 权限模型 |
|------|---------|------|---------|
| **Free** | 开源组织 | 公开+私有 | 基础 Teams + 仓库权限 |
| **Team** | 小型团队 | 公开+私有 | + CODEOWNERS + Required Reviewers |
| **Enterprise** | 企业 | 公开+私有 | + SAML SSO + 审计日志 + IP 白名单 |

### 1.3 创建组织

```
1. GitHub → ⊕ → New organization
2. 选择计划（Free / Team / Enterprise）
3. 填写组织名称 + 联系邮箱
4. 邀请成员
5. 创建默认的 .github 仓库（组织级健康文件）
```

---

## 2. Teams 团队与权限管理

### 2.1 Team 组织结构

```
Organization: my-company
├── Team: platform-eng         (平台工程团队)
│   ├── @zhangsan (Maintainer)
│   └── @lisi (Member)
├── Team: backend              (后端团队)
│   ├── @wangwu (Maintainer)
│   ├── @zhaoliu (Member)
│   └── Team: backend-senior   (子团队 — 高级后端)
│       └── @wangwu
├── Team: frontend             (前端团队)
└── Team: devops               (DevOps 团队)
```

### 2.2 Team 角色与权限

```
Team 中的角色：
  Member      — 普通成员
  Maintainer  — 可以管理 Team（添加/移除成员、修改 Team 设置）

授予仓库权限：
  方式一：直接给用户仓库权限（不推荐 — 难管理）
  方式二：通过 Team 授予仓库权限（推荐 — 集中管理）

Team → Repository 权限设置：
  Read     — 只能查看代码
  Triage   — Read + 管理 Issues/PRs
  Write    — Triage + 推送代码
  Maintain — Write + 管理仓库设置
  Admin    — 完全控制
```

### 2.3 使用 gh CLI 管理团队

```bash
# 创建 Team
gh api /orgs/my-org/teams -f name='backend' -f privacy='closed'

# 添加成员
gh api /orgs/my-org/teams/backend/memberships/zhangsan \
  -X PUT -f role='maintainer'

# 授予仓库权限
gh api /orgs/my-org/teams/backend/repos/my-org/my-repo \
  -X PUT -f permission='write'

# 列出成员
gh api /orgs/my-org/teams/backend/members --jq '.[].login'
```

---

## 3. 仓库权限模型

### 3.1 五种权限级别

| 权限 | 能做什么 | 不能做什么 |
|------|---------|-----------|
| **Read** | 查看代码、Clone、Fork、查看 Issues | Push、创建 Issue |
| **Triage** | Read + 创建/管理 Issues 和 PRs | Push 代码 |
| **Write** | Triage + Push 代码、创建分支 | 删除仓库、修改保护规则 |
| **Maintain** | Write + 管理仓库设置 | 删除仓库、转移所有权 |
| **Admin** | 完全控制 | — |

### 3.2 权限授予方式

```yaml
# 权限来源（优先级从高到低）：

1. 直接仓库协作者（Repository Collaborator）
   → Settings → Collaborators → 直接添加个人

2. Team 权限（推荐 ⭐）
   → Team → Repositories → 添加仓库 + 选择权限级别

3. 组织 Base Permissions（兜底）
   → Organization Settings → Member privileges → Base permissions
   → 适用于所有成员在所有仓库的默认权限
   → 建议设为 "None" 或 "Read"（最小权限原则）
```

### 3.3 权限审计

```bash
# 查看某人跨越所有仓库的权限
gh api /orgs/my-org/members/zhangsan

# 查看某仓库的协作者列表
gh api /repos/my-org/my-repo/collaborators?permission=admin

# 查看 Team 在仓库上的权限
gh api /orgs/my-org/teams/backend/repos
```

---

## 4. 开源项目治理规范

### 4.1 开源项目必配文件

```markdown
开源项目根目录的必备文件：

README.md             # 项目介绍、快速开始、徽章
LICENSE               # 开源许可证（MIT/Apache 2.0/GPL...）
CONTRIBUTING.md       # 贡献指南（如何提 Issue、如何提 PR、代码风格）
CODE_OF_CONDUCT.md    # 行为准则（社区规范）
SECURITY.md           # 安全漏洞报告流程
CHANGELOG.md          # 版本变更记录
```

```markdown
<!-- CONTRIBUTING.md 典型内容 -->

## 🚀 快速开始
1. Fork 本仓库
2. git clone <你的Fork>
3. git checkout -b feat/your-feature
4. 开发 + 测试
5. git push origin feat/your-feature
6. 提交 Pull Request

## 📋 PR 规范
- 一个 PR 只做一件事
- PR 标题遵循 Conventional Commits
- 必须包含测试
- 必须通过 CI 检查

## 🔍 Code Review 流程
1. 至少 2 位 Maintainer 审批
2. CI 必须全绿
3. 所有讨论必须解决

## 🧪 开发环境搭建
...
```

### 4.2 语义化版本规范

```bash
# 版本号格式：MAJOR.MINOR.PATCH

v2.1.3
│ │ └── PATCH: 向后兼容的 Bug 修复
│ └──── MINOR: 向后兼容的新功能
└────── MAJOR: 不兼容的 API 变更

# Changelog 格式（Keep a Changelog 标准）
## [1.2.0] - 2024-01-15

### Added
- 新增用户导出 CSV 功能

### Changed
- 优化查询性能，分页接口响应时间降低 50%

### Deprecated
- /api/v1/users 已弃用，请迁移至 /api/v2/users

### Fixed
- 修复并发创建订单时偶发的死锁问题

### Security
- 修复 CVE-2024-1234：登录接口 SQL 注入漏洞
```

### 4.3 开源项目健康指标

```
OpenSSF Scorecard（开源安全基金会评分卡）检查项：
  ✅ Code-Review          — 代码审查是否强制执行
  ✅ Branch-Protection    — 分支保护
  ✅ Signed-Releases      — Release 产物是否签名
  ✅ Dependency-Update-Tool — 是否使用 Dependabot/Renovate
  ✅ Fuzzing              — 是否进行模糊测试
  ✅ SAST                 — 静态代码分析
  ✅ Token-Permissions    — Actions Token 权限最小化
  ✅ Vulnerabilities      — 已知漏洞是否修复
```

---

## 5. 开源协作完整流程

### 5.1 第一次贡献

```bash
# ═══ 给陌生项目贡献代码的完整路线 ═══

# 1. 找 Issue（good first issue 标签）
#    → Labels: "good first issue", "help wanted"

# 2. 在 Issue 下留言申请
#    "I'd like to work on this. Can you assign it to me?"

# 3. Fork + Clone
gh repo fork original/repo --clone
cd repo

# 4. 创建分支（包含 Issue 编号）
git checkout -b fix/42-typo-in-readme

# 5. 阅读 CONTRIBUTING.md
#    → 了解提交规范、代码风格、测试要求

# 6. 开发 + 测试
# ... 写代码 ...
# 确保所有测试通过

# 7. Push
git push origin fix/42-typo-in-readme

# 8. 创建 PR
gh pr create --base main --title "docs: fix typo in README" \
    --body "Closes #42\n\n修正了 README 中的拼写错误"

# 9. 配合 Review
#    → 及时回复评论
#    → 合理讨论设计决策
#    → 修改后标记 Resolved
#    → 保持礼貌和耐心

# 10. 🎉 合并！
```

### 5.2 开源礼仪

```
✅ DO：
  1. 阅读 CONTRIBUTING.md 和 CODE_OF_CONDUCT
  2. 先搜索是否已有相关 Issue/PR
  3. Issue 中提供完整信息（复现步骤、环境、日志）
  4. PR 小而专注（Small PR > Big PR）
  5. 保持礼貌和建设性（即使是批评）
  6. 接受"不修复"的决定（维护者有最终决定权）

❌ DON'T：
  1. 在 Issue 中催促进度（"为什么还没修？"）
  2. 提交大而全的 PR（没有事先讨论设计）
  3. 态度恶劣（开源维护者是志愿工作）
  4. 无视 Review 意见
  5. 要求合并到其他分支
```

---

## 6. InnerSource 内部开源实践

### 6.1 什么是 InnerSource

> InnerSource = 在企业内部应用开源协作模式。任何团队都可以向其他团队的仓库提 PR，打破部门壁垒，促进代码复用。

```
开源                        InnerSource
公开仓库                    私有仓库（企业账号）
全球社区                    公司内部开发者
GitHub.com                  GitHub Enterprise Server/Cloud
任何人都可贡献              企业内任何员工可贡献
公开讨论                   内部 Open 讨论
```

### 6.2 InnerSource 核心实践

```
1. Trusted Committer（可信任提交者）
   - 仓库的 Maintainer，负责 Code Review 和维护
   - 至少 1 名，建议 2-3 名避免单点

2. 文档优先
   - README + CONTRIBUTING.md 必须完善
   - 新人应能在 15 分钟内完成首次构建

3. 模块化设计
   - 清晰界定每个仓库的职责边界
   - 通过 API/库 暴露能力，而非要求修改源码

4. 自动化 CI/CD
   - 所有 PR 自动测试
   - 降低 Review 的认知负担

5. 奖励机制
   - 跨团队贡献应被纳入绩效考核
   - 公示 InnerSource 贡献排行榜
```

---

## 7. 常见面试题

### Q1：GitHub Organization 的权限体系是怎样的？

> 三级：仓库权限（Read/Triage/Write/Maintain/Admin）→ Team（通过 Team 批量授予仓库权限）→ 组织 Base Permissions（兜底默认权限）。推荐通过 Team 管理权限，遵循最小权限原则。详见第2-3节。

### Q2：一个好的开源项目需要哪些治理文件？

> README（项目介绍）、LICENSE（许可证）、CONTRIBUTING.md（贡献指南）、CODE_OF_CONDUCT.md（行为准则）、SECURITY.md（安全报告流程）、CHANGELOG.md（变更记录）。详见第4节。

### Q3：如何为开源项目做第一次贡献？

> 搜索 good first issue → 留言申请 → Fork → 创建分支 → 阅读 CONTRIBUTING.md → 开发+测试 → 提 PR → 配合 Review → 合并。详见第5节。

### Q4：什么是 InnerSource？与开源有什么异同？

> InnerSource 是企业内部的开源协作模式。相同点：Fork + PR + Code Review 流程；不同点：仓库私有、贡献者限于员工、企业安全合规要求。详见第6节。
