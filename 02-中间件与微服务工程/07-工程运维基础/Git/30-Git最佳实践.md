# 30-Git最佳实践
> 🎯 Git用得好不好，一行命令就能看出来 — 从分支策略、提交规范、协作流程、安全防护到多环境部署，Java后端团队的Git最佳实践全集

---

## 目录
1. [分支管理最佳实践](#1-分支管理最佳实践)
2. [提交规范最佳实践](#2-提交规范最佳实践)
3. [协作流程最佳实践](#3-协作流程最佳实践)
4. [版本发布最佳实践](#4-版本发布最佳实践)
5. [安全与防护最佳实践](#5-安全与防护最佳实践)
6. [多环境与微服务最佳实践](#6-多环境与微服务最佳实践)
7. [团队Git规范模板](#7-团队git规范模板)

---

## 1. 分支管理最佳实践

### 1.1 分支体系设计

```text
长期分支（永久存在）:
  main          → 生产环境代码，始终可部署
  develop       → 开发主线，所有feature的合并目标

临时分支（合并后删除）:
  feature/*     → 新功能开发，从develop拉，合并回develop
  hotfix/*      → 生产紧急修复，从main拉，合并回main+develop
  release/*     → 发布准备，从develop拉，合并回main+develop

Java后端多环境对应:
  main      → 生产环境
  develop   → 测试环境
  release/* → 预发布环境
```

### 1.2 分支命名

| 类型 | 格式 | 示例 |
|------|------|------|
| 新功能 | `feature/<模块>-<简述>` | `feature/user-login` |
| Bug修复 | `fix/<模块>-<简述>` | `fix/order-calculate` |
| 紧急修复 | `hotfix/<简述>` | `hotfix/payment-timeout` |
| 发布 | `release/v<版本号>` | `release/v2.3.0` |
| 重构 | `refactor/<模块>` | `refactor/extract-common` |

### 1.3 分支保护规则

```yaml
main / develop 保护:
  ✅ 禁止直接push
  ✅ 必须通过PR/MR合并
  ✅ 至少1人Approval
  ✅ CI所有检查通过
  ✅ 分支必须最新（禁止落后于目标分支）
  ✅ 禁止force push

feature/* 无保护（开发者自由操作）
```

---

## 2. 提交规范最佳实践

### 2.1 Conventional Commits

```text
<type>(<scope>): <subject>

type: feat | fix | docs | style | refactor | test | chore | perf | ci
scope: 影响模块（可选）
subject: ≤50字符，现在时，首字母小写
```

| type | 何时使用 | 示例 |
|------|----------|------|
| `feat` | 新功能 | `feat(user): 添加用户登录` |
| `fix` | Bug修复 | `fix(order): 修复金额计算精度` |
| `refactor` | 重构（无功能变更） | `refactor(common): 提取Token工具类` |
| `test` | 测试 | `test(user): 补充登录失败测试` |
| `docs` | 文档 | `docs: 更新API文档` |
| `chore` | 构建/依赖/工具 | `chore: 升级Spring Boot到3.2` |
| `perf` | 性能优化 | `perf(order): 优化查询索引` |
| `ci` | CI/CD | `ci: 添加代码覆盖率检查` |

### 2.2 Commit粒度

```text
✅ 好的粒度：
  feat(user): 添加用户注册接口
  feat(user): 添加用户名唯一性校验
  test(user): 补充注册功能单元测试

❌ 太大：
  feat: 完成用户模块（改了20个文件，无法review）

❌ 太小：
  fix: typo
  fix: another typo     ← 应该squash或amend
  fix: one more typo
```

### 2.3 提交前检查清单

```checkbox
□ 编译通过：mvn compile 成功
□ 测试通过：mvn test 全绿
□ 无调试代码：没有 System.out.println / console.log
□ 无敏感信息：没有密码/Token/密钥
□ 检查文件：没有 target/、.idea/ 等
□ commit message符合规范
```

---

## 3. 协作流程最佳实践

### 3.1 标准PR流程

```text
1. 从develop拉feature分支
   git switch develop && git pull && git switch -c feature/xxx

2. 开发 + 频繁commit（粒度适中）
   git add -p → git commit

3. 每天同步develop（避免落后太多）
   git fetch origin && git rebase origin/develop

4. 开发完成，整理历史
   git rebase -i HEAD~10  # squash多余的WIP

5. 推送并创建PR
   git push -u origin feature/xxx
   → GitHub/GitLab 创建PR，填写描述

6. Code Review → 修复意见 → push更新
   用 fixup! commit 回应review意见

7. 通过后合并
   → Merge Commit（推荐）或 Squash and Merge

8. 清理
   git switch develop && git pull
   git branch -d feature/xxx
```

### 3.2 PR描述模板

```markdown
## 📝 变更概述
新增用户邮箱验证功能

## 🔗 关联Issue
Closes #1234

## 🧪 测试
- [x] 单元测试：验证码生成与校验
- [x] 集成测试：注册→验证→登录完整流程
- [x] 异常测试：过期验证码、错误验证码

## 📸 截图
<如有UI变更>

## ⚠️ 风险提示
- 依赖邮件服务，需确认测试环境邮件配置
```

---

## 4. 版本发布最佳实践

### 4.1 发布流程

```bash
# 1. 从develop拉release分支
git switch develop && git pull
git switch -c release/v2.3.0

# 2. 在release上做发布准备
# 更新版本号、补充文档、最终测试

# 3. 合并到main
git switch main && git pull
git merge --no-ff release/v2.3.0

# 4. 打标签
git tag -a v2.3.0 -m "Release v2.3.0: 新增订单导出、修复支付超时"

# 5. 推送到远程
git push origin main --tags

# 6. 合并回develop（保证develop有release上的最终修复）
git switch develop
git merge --no-ff release/v2.3.0
git push origin develop

# 7. 清理
git branch -d release/v2.3.0
```

### 4.2 版本号与标签

```text
semver: MAJOR.MINOR.PATCH
  MAJOR: 不兼容的API变更
  MINOR: 向后兼容的新功能
  PATCH: 向后兼容的Bug修复

标签 = 版本的不可变标记
  git tag -a v2.3.0 → 附注标签（推荐，含发布说明）
  git tag -s v2.3.0 → GPG签名标签（安全要求高的企业）
```

---

## 5. 安全与防护最佳实践

| 实践 | 说明 |
|------|------|
| **不要提交密钥** | API Key/密码/Token一律用环境变量，`.env`加入`.gitignore` |
| **启用分支保护** | main/develop禁止直接push、强制PR、要求CI通过 |
| **GPG签名** | 确保commit不可伪造 |
| **定期清理** | 删除已合并分支、清理过期stash |
| **.gitignore** | 编译产物、IDE配置、本地配置一律忽略 |
| **敏感信息泄露补救** | `git filter-branch` 或 BFG Repo-Cleaner 清除历史中的密码 |

```bash
# 如果不小心提交了密码，从历史中彻底清除
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch config/secrets.yml" \
  --prune-empty --tag-name-filter cat -- --all
# 注意：这需要所有协作者重新clone！首选BFG工具
```

---

## 6. 多环境与微服务最佳实践

### 6.1 多环境配置管理

```text
配置文件分层（由通用到专用）：
  application.yml              → 通用配置，提交到Git
  application-dev.yml          → 开发环境，可提交
  application-local.yml        → 本地配置，.gitignore忽略
  application-prod.yml         → 生产配置，可提交（不含密钥）

密钥管理：
  ❌ 密钥写在application-prod.yml中
  ✅ 密钥存在环境变量/K8s Secret/Vault中
     通过 ${DB_PASSWORD} 引用
```

### 6.2 微服务Git策略

| 策略 | 适用 | Git操作 |
|------|------|---------|
| **多仓库(Multi-Repo)** | 大团队、服务独立 | 每个服务独立仓库 |
| **Monorepo** | 小团队、服务紧耦合 | Maven多模块单仓库 |
| **公共组件** | 跨服务复用 | 发布jar包到内部Maven仓库（推荐） |

---

## 7. 团队Git规范模板

```markdown
# 团队Git规范 (可直接复制到项目README/CONTRIBUTING.md)

## 分支命名
- feature/<模块>-<简述>  例: feature/user-auth
- fix/<模块>-<简述>      例: fix/order-total
- hotfix/<简述>         例: hotfix/pay-timeout

## Commit格式
<type>(<scope>): <subject>
type: feat|fix|refactor|test|docs|chore|perf|ci

## PR要求
- 必须关联Issue（Closes #N）
- 至少1人Approval
- CI全部通过
- PR描述包含：变更概述 + 测试说明 + 风险提示

## 禁止事项
- 禁止直接push到main/develop
- 禁止在公共分支rebase
- 禁止提交密钥/密码/Token
- 禁止提交编译产物(target/、.idea/)
- 禁止force push共享分支
```

---

> 🎯 **最佳实践的本质**：不是背诵规则，而是理解每条规则背后的"为什么"——规范是为了减少混乱、自动化是为了减少失误、安全是为了防范灾难。
