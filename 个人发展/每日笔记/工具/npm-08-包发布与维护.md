# npm 包发布与维护

> 从写代码到被别人 `npm install` 你的包，完整流程和最佳实践。

---

## 一、发布前准备

### 1.1 检查清单

```
□ package.json 必须字段：name（唯一）、version、main
□ package.json 建议字段：description、keywords、license、repository
□ 已配置 files 字段（或 .npmignore 排除不需要发布的文件）
□ 包名在 npm Registry 上不重复（npm search 或 npm view <name> 验证）
□ 代码已通过测试
□ README.md 已写好
□ .gitignore 排除 node_modules
□ LICENSE 文件已创建
```

### 1.2 name 命名规则

```json
{
  "name": "my-awesome-utils",              // ✅ 短横线分隔
  "name": "@suxiangyu/my-awesome-utils",   // ✅ 作用域（推荐）
  "name": "MyAwesomeUtils",                // ❌ 不能大写（会转成小写）
  "name": "my awesome utils"              // ❌ 不能有空格
}
```

**作用域包**（Scoped Package）：
```bash
# 作用域包相当于名字空间
npm publish --access public   # 第一次发布作用域包需要

# 私有的作用域包（需要 npm Pro 或企业版）
npm publish                   # 默认是 --access restricted
```

### 1.3 files 白名单 vs .npmignore

```
优先级：files 字段 > .npmignore > .gitignore

如果配置了 files 字段 → .npmignore 无效
如果没配置 files → .npmignore 过滤
如果都没有 → 默认发布根目录下所有非忽略文件
```

```json
{
  "files": [
    "dist",           // 编译后的代码
    "README.md",
    "LICENSE"
  ]
}
```

这些文件**始终包含**（不管 files 怎么配）：`package.json`、`README`、`LICENSE` / `LICENCE`、`CHANGELOG`、主入口文件（main 字段指定的）。

---

## 二、登录与认证

### 2.1 npm login

```bash
npm login

# Username: suxiangyu138
# Password: ********
# Email: email@example.com
# 输入 OTP（如果开了双因素认证）

# 验证是否登录成功
npm whoami   # → suxiangyu138

# 登出
npm logout
```

### 2.2 Token 认证（CI/CD 推荐）

```bash
# 在 npmjs.com 的 Access Tokens 页面创建
# 类型：Publish（可发布）/ Read Only（只读）

# 设置 Token
npm config set //registry.npmjs.org/:_authToken=npm_xxxxx

# 或通过 .npmrc（注意不要提交到 Git！）
//registry.npmjs.org/:_authToken=${NPM_TOKEN}

# CI/CD 中设置环境变量
# GitHub Actions: Settings → Secrets → NPM_TOKEN
```

### 2.3 双因素认证 2FA

```bash
npm profile enable-2fa auth-only    # 登录需要 2FA
npm profile enable-2fa auth-and-writes  # 登录 + 发布都需要 2FA（推荐）
```

---

## 三、发布命令

### 3.1 基础发布

```bash
# 标准发布
npm publish

# 首次发布作用域包（公开）
npm publish --access public

# 发布到特定标签（非 latest）
npm publish --tag beta
npm publish --tag next

# 模拟发布（不真正上传，用于验证哪些文件会被发布）
npm publish --dry-run

# 指定源发布
npm publish --registry https://registry.npmmirror.com
```

### 3.2 版本号管理

```bash
npm version patch        # 1.0.0 → 1.0.1（bug 修复）
npm version minor        # 1.0.0 → 1.1.0（新功能）
npm version major        # 1.0.0 → 2.0.0（破坏性变更）

npm version prepatch     # 1.0.0 → 1.0.1-0
npm version preminor     # 1.0.0 → 1.1.0-0
npm version premajor     # 1.0.0 → 2.0.0-0
npm version prerelease   # 1.0.1-0 → 1.0.1-1
```

`npm version` 会自动：
1. 更新 `package.json` 的 `version` 字段
2. 创建 Git tag（如 `v1.0.1`）
3. 创建 Git commit（如 `1.0.1`）

### 3.3 标准发布流程

```bash
# 1. 确保所有改动已提交
git status

# 2. 运行测试
npm test

# 3. 构建
npm run build

# 4. 更新版本号
npm version patch

# 5. 发布
npm publish

# 6. 推送 tag
git push --follow-tags
```

### 3.4 自动化发布脚本

```json
{
  "scripts": {
    "prepublishOnly": "npm test && npm run build",
    "release": "npm version patch && npm publish && git push --follow-tags",
    "release:minor": "npm version minor && npm publish && git push --follow-tags",
    "release:major": "npm version major && npm publish && git push --follow-tags",
    "prerelease": "npm version prerelease --preid=beta && npm publish --tag beta"
  }
}
```

---

## 四、版本标签管理

```bash
# 查看所有标签
npm dist-tag ls my-package
# latest: 1.0.0
# beta: 1.1.0-beta.1
# next: 2.0.0-rc.1

# 添加标签
npm dist-tag add my-package@1.0.0 latest

# 删除标签
npm dist-tag rm my-package@beta
```

---

## 五、废弃与删除

### 5.1 deprecate（推荐）

```bash
# 废弃整个包
npm deprecate my-package "no longer maintained, use other-package instead"

# 废弃特定版本
npm deprecate my-package@"<2.0.0" "version <2.0.0 has security issues, please upgrade"

# 废弃指定标签
npm deprecate my-package@beta "beta is no longer active"
```

### 5.2 unpublish（谨慎使用）

```bash
# 删除指定版本
npm unpublish my-package@1.0.1

# 删除整个包（24 小时内可恢复，且不能再发同名包到相同版本）
npm unpublish my-package --force

# left-pad 事件后的策略变化：
# 发布超过 72 小时的包不能被 unpublish
# 发布后 72 小时内可以 unpublish
```

### 5.3 deprecate vs unpublish

| 操作 | 影响 | 推荐度 |
|------|------|--------|
| `deprecate` | 包还在，安装时有警告 | ✅ 推荐，不破坏已有项目 |
| `unpublish` | 包被删除，已依赖的项目炸了 | ❌ 极其不推荐 |

---

## 六、包的最佳实践

### 6.1 支持双模块（CJS + ESM）

```json
{
  "name": "my-lib",
  "main": "./dist/index.cjs.js",
  "module": "./dist/index.esm.js",
  "types": "./dist/index.d.ts",
  "exports": {
    ".": {
      "import": "./dist/index.esm.js",
      "require": "./dist/index.cjs.js",
      "types": "./dist/index.d.ts"
    }
  },
  "files": ["dist", "README.md"]
}
```

### 6.2 关键词优化

```json
{
  "keywords": [
    "vue",
    "component",
    "ui",
    "typescript",
    "vue3"
  ]
}
```

**搜索排名权重**：
- 包名是否精确匹配 > 描述权重 > keywords 权重
- GitHub ⭐ 数量、每周下载量也会影响排名

### 6.3 包大小优化

```json
{
  "files": ["dist"],           // 只发编译后的文件
  "sideEffects": false,        // Webpack Tree Shaking
  "peerDependencies": {        // 不打包 React/Vue
    "react": ">=16.8.0"
  },
  "devDependencies": {         // 构建工具放这里，不安装给用户
    "rollup": "^4.0.0"
  }
}
```

---

## 七、私服发布

### 7.1 Verdaccio 发布

```bash
# 配置私服源
npm config set registry http://localhost:4873

# 添加用户
npm adduser --registry http://localhost:4873

# 发布
npm publish --registry http://localhost:4873
```

### 7.2 GitHub Packages 发布

```yaml
# .github/workflows/npm-publish.yml
name: Publish to GitHub Packages

on:
  release:
    types: [created]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          registry-url: 'https://npm.pkg.github.com'
          scope: '@your-org'
      - run: npm ci
      - run: npm publish
        env:
          NODE_AUTH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

> **核心流程**：`npm version patch` → `npm publish` → `git push --follow-tags`。用 `files` 字段控制发布内容，用 `deprecate` 而非 `unpublish` 废弃包。支持 ESM + CJS 双模块是现代包的基本素养。
