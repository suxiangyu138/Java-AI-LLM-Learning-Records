# Git 钩子与自动化集成

## 什么是 Git Hooks？

Git 钩子是在特定事件（commit、push、merge 等）发生时**自动执行**的脚本。它们不是 CI/CD 的替代品，而是前置防线。

---

## 1. 钩子全景图

### 1.1 客户端钩子（你的电脑上运行）

| 钩子 | 触发时机 | 典型用途 |
|------|---------|---------|
| `pre-commit` | `git commit` 之前 | lint、格式化、检查密钥泄露 |
| `prepare-commit-msg` | 默认 commit 消息生成后 | 自动填充 commit 模板 |
| `commit-msg` | commit 消息写入后 | 校验 commit message 格式 |
| `post-commit` | commit 完成后 | 通知、日志 |
| `pre-rebase` | rebase 之前 | 阻止危险 rebase |
| `post-checkout` | checkout 之后 | 更新依赖 |
| `post-merge` | merge 之后 | 自动安装新依赖 |

### 1.2 服务端钩子（远程仓库上运行）

| 钩子 | 触发时机 | 典型用途 |
|------|---------|---------|
| `pre-receive` | push 到达之后，更新 refs 之前 | 拒绝不符合规范的 push |
| `update` | 每个 ref 更新时 | 拒绝特定分支的 force push |
| `post-receive` | push 完成后 | CI 触发、部署、通知 |

---

## 2. 手动编写 Git Hook

```bash
# 进入 hooks 目录
ls .git/hooks/
# applypatch-msg.sample  pre-push.sample
# commit-msg.sample      pre-rebase.sample
# pre-commit.sample      ...

# Git 提供了示例模板，去掉 .sample 后缀即激活
```

### 2.1 编写 pre-commit hook

```bash
# .git/hooks/pre-commit
#!/bin/bash

# 禁止提交包含 console.log 的代码
if git diff --cached | grep -q "^+.*console\.log"; then
    echo "❌ 发现 console.log，请在提交前移除"
    exit 1
fi

# 检查 .env 文件是否被提交
if git diff --cached --name-only | grep -q "\.env$"; then
    echo "❌ 禁止提交 .env 文件！"
    exit 1
fi

exit 0
```

```bash
chmod +x .git/hooks/pre-commit
```

### 2.2 编写 commit-msg hook

```bash
# .git/hooks/commit-msg
#!/bin/bash

COMMIT_MSG=$(cat "$1")

# 强制 Conventional Commits 格式
PATTERN="^(feat|fix|docs|style|refactor|test|chore|perf|ci)(\(.+\))?: .{1,72}$"

if ! echo "$COMMIT_MSG" | grep -qE "$PATTERN"; then
    echo "❌ Commit message 不符合规范"
    echo "格式: <type>(<scope>): <subject>"
    echo "type: feat|fix|docs|style|refactor|test|chore|perf|ci"
    exit 1
fi
```

### 2.3 Hooks 的局限性

- `.git/hooks/` 不能被 Git 追踪（不在版本控制中）
- 每个开发者需要手动设置
- 跨平台兼容性差（bash 脚本在 Windows 上不能用）

**解决方案**：用 Node.js 的 **husky** 或 Python 的 **pre-commit** 框架。

---

## 3. Husky + lint-staged（Node.js 生态）

### 3.1 安装配置

```bash
# 安装
npm install --save-dev husky lint-staged

# 初始化 husky（Git 2.9+）
npx husky init
# 创建 .husky/ 目录，并且设置 prepare 脚本
```

### 3.2 配置 pre-commit

```bash
# .husky/pre-commit
npx lint-staged
```

```json
// package.json 或 .lintstagedrc.json
{
  "lint-staged": {
    "*.{js,ts,tsx}": [
      "eslint --fix",
      "prettier --write"
    ],
    "*.{json,md,yaml}": [
      "prettier --write"
    ],
    "*.py": [
      "black",
      "isort"
    ]
  }
}
```

### 3.3 配置 commit-msg

```bash
# .husky/commit-msg
npx --no -- commitlint --edit $1
```

```javascript
// commitlint.config.js
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', [
      'feat', 'fix', 'docs', 'style', 'refactor',
      'test', 'chore', 'perf', 'ci', 'build'
    ]],
    'subject-max-length': [2, 'always', 72],
  },
};
```

### 3.4 完整 husky 配置示例

```json
// package.json
{
  "scripts": {
    "prepare": "husky"   // npm install 时自动初始化 husky
  }
}
```

目录结构：

```text
project/
├── .husky/
│   ├── pre-commit       # npx lint-staged
│   ├── commit-msg       # npx commitlint --edit $1
│   └── pre-push         # npm test
├── .lintstagedrc.json
├── commitlint.config.js
└── package.json
```

---

## 4. Python pre-commit 框架

```bash
# 安装
pip install pre-commit

# 创建配置
cat > .pre-commit-config.yaml << 'EOF'
repos:
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.5.0
    hooks:
      - id: trailing-whitespace      # 移除行尾空格
      - id: end-of-file-fixer        # 文件末尾加空行
      - id: check-yaml               # YAML 语法检查
      - id: check-json               # JSON 语法检查
      - id: detect-private-key       # 检测私钥泄露
      - id: check-added-large-files  # 阻止大文件

  - repo: https://github.com/psf/black
    rev: 24.1.0
    hooks:
      - id: black                    # Python 格式化

  - repo: https://github.com/charliermarsh/ruff-pre-commit
    rev: v0.1.0
    hooks:
      - id: ruff                     # Python linting

  - repo: https://github.com/pre-commit/mirrors-prettier
    rev: v3.1.0
    hooks:
      - id: prettier                 # 通用格式化
        types_or: [javascript, typescript, json, yaml, markdown]
EOF

# 安装钩子
pre-commit install           # 安装 pre-commit hook
pre-commit install --hook-type commit-msg  # 安装 commit-msg hook

# 手动对所有文件运行
pre-commit run --all-files
```

---

## 5. CI/CD 集成

### 5.1 GitHub Actions 示例

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'npm'
      - run: npm ci
      - run: npm run lint
      - run: npm test

  # 检查 commit message 格式
  commitlint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: wagoid/commitlint-github-action@v5
```

### 5.2 GitLab CI 示例

```yaml
# .gitlab-ci.yml
stages:
  - test
  - deploy

lint:
  stage: test
  script:
    - npm ci
    - npm run lint

unit-test:
  stage: test
  script:
    - npm ci
    - npm test

deploy-staging:
  stage: deploy
  script:
    - echo "Deploying to staging..."
  only:
    - main
```

---

## 6. 常用自动化场景汇总

| 场景 | 工具/方案 | 说明 |
|------|---------|------|
| 代码格式化 | pre-commit + Prettier/Black | 提交前自动格式化 |
| 代码检查 | pre-commit + ESLint/Ruff | 阻止有问题的代码提交 |
| Commit 规范 | commit-msg + commitlint | 强制 Conventional Commits |
| 密钥检测 | pre-commit + detect-private-key | 防止密钥泄露到仓库 |
| 大文件检测 | pre-commit + check-added-large-files | 阻止大文件被提交 |
| 自动测试 | pre-push + npm test | push 前自动跑测试 |
| CI 检查 | GitHub Actions / GitLab CI | PR 时自动跑完整 CI |
| 自动部署 | post-receive / GitHub Actions | main 合并后自动部署 |
| 生成 Changelog | post-commit / CI | 自动根据commit生成 |

---

## 7. Hook 跳过（危险但有用）

```bash
# 跳过 pre-commit hook
git commit --no-verify -m "emergency fix"

# 跳过所有 hooks
git commit -n -m "..."        # -n = --no-verify

# 跳过 push hooks
git push --no-verify

# ⚠️ 只在明确知道为什么需要跳过时使用
# CI 应该作为第二道防线来兜底
```

---

## 8. 最佳实践

```text
1. 轻量前置 → 重量后置
   Hook 做轻量检查（格式、lint），CI 做重量检查（测试、构建）

2. 保持 Hook 快速
   pre-commit 应在 5 秒内完成，不然开发体验很差

3. 统一管理
   用 husky/pre-commit 框架让团队共享 hook 配置

4. 不依赖 Hook 做安全
   Hook 可以被跳过，安全策略必须在服务端 enforce

5. CI 是最终防线
   不要只在本地 hook 检查，CI 必须重复验证
```

---

> 上一篇：[06-Git远程协作与平台实战](06-Git远程协作与平台实战.md)
> 下一篇：[08-Git标签与语义化版本发布](08-Git标签与语义化版本发布.md)
