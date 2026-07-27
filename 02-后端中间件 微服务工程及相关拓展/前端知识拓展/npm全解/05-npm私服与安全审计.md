# 05 - npm 私服与安全审计

> 🎯 企业内部需要私有 npm 仓库（如同 Nexus），安全审计防止依赖投毒 — 这部分和 Maven 私服 + OWASP 依赖检查的思路一脉相承

---

## 目录

1. [Verdaccio 私有 npm 仓库](#1-verdaccio-私有-npm-仓库)
2. [npm Registry 配置](#2-npm-registry-配置)
3. [安全审计（npm audit）](#3-安全审计npm-audit)
4. [依赖管理最佳实践](#4-依赖管理最佳实践)

---

## 1. Verdaccio 私有 npm 仓库

> Verdaccio 是轻量级 npm 私服，类似 Java 生态的 Nexus/Artifactory。

```bash
# Docker 启动 Verdaccio
docker run -d --name verdaccio -p 4873:4873 \
  -v /opt/verdaccio/storage:/verdaccio/storage \
  verdaccio/verdaccio:5

# 访问 http://localhost:4873
```

```bash
# 客户端配置
npm adduser --registry http://localhost:4873

# 发布私有包
npm publish --registry http://localhost:4873
```

### 上游代理（类似 Nexus proxy repository）

```yaml
# verdaccio config.yaml
uplinks:
  npmjs:
    url: https://registry.npmjs.org/
  npmmirror:
    url: https://registry.npmmirror.com/

packages:
  '@company/*':
    access: $authenticated        # 私有包仅认证用户
    publish: $authenticated
  '**':
    access: $all                 # 公共包走上游代理
    proxy: npmmirror npmjs       # 先淘宝镜像，后官方
```

| 概念 | npm | Java |
|------|-----|------|
| 私有 npm 仓库 | Verdaccio / Nexus npm | Nexus / Artifactory |
| 上游代理 | `uplinks` | proxy repository |
| 作用域包 | `@company/package` | `com.company:project` |
| 认证 | npm token / npm login | Maven settings.xml |

---

## 2. npm Registry 配置

```bash
# 查看当前 registry
npm config get registry

# 淘宝镜像（国内加速）
npm config set registry https://registry.npmmirror.com

# 临时使用
npm install --registry=https://registry.npmmirror.com
```

### .npmrc 文件（类似 Maven settings.xml）

```ini
# 项目级 .npmrc
registry=https://registry.npmmirror.com

# @company 作用域包走私有仓库
@company:registry=http://verdaccio.company.com:4873
//verdaccio.company.com:4873:_authToken=xxx
```

```ini
# 全局 ~/.npmrc
prefix=/usr/local
cache=/Users/xxx/.npm
registry=https://registry.npmmirror.com
```

| 文件 | 作用域 | Java 对标 |
|------|--------|----------|
| `~/.npmrc` | 用户级 | `~/.m2/settings.xml` |
| `.npmrc`（项目根目录） | 项目级 | 项目 `.mvn/maven.config` |
| `package.json` 不存 registry | 不污染项目文件 | pom.xml |

---

## 3. 安全审计（npm audit）

> 类似 Maven 的 OWASP Dependency Check。

```bash
# 扫描所有依赖的安全漏洞
npm audit

# 输出示例
# lodash  <4.17.21
# Severity: high
# Prototype Pollution - https://github.com/advisories/GHSA-xxx
# fix available via `npm audit fix --force`
# Will install lodash@4.17.21, which is a breaking change

# 自动修复（安全的小版本升级）
npm audit fix

# 强制修复（可能包含 breaking change）
npm audit fix --force

# JSON 格式输出（CI 集成）
npm audit --json
```

### 在 CI 中集成安全扫描

```yaml
# GitHub Actions
- run: npm ci
- run: npm audit --audit-level=high    # 高危漏洞导致 CI 失败
```

```json
// package.json 中忽略特定漏洞（已评估安全）
{
  "overrides": {
    "lodash": "4.17.21"   // 强制所有依赖用安全版本
  }
}
```

---

## 4. 依赖管理最佳实践

| ✅ DO | ❌ DON'T |
|-------|----------|
| 提交 `package-lock.json` 到 Git | `.gitignore` 中排除 lock 文件 |
| CI 环境用 `npm ci` | CI 环境用 `npm install` |
| 定期运行 `npm audit` | 忽略高危漏洞 |
| 生产依赖放 `dependencies` | 构建工具放 `dependencies` |
| 用 `^` 或 `~` 管理版本范围 | 用 `*` 或 `latest` |
| 搭建私有 npm 仓库管理内部包 | 把 node_modules 提交到 Git |

```dockerfile
# ⭐ Dockerfile 最佳实践 — 利用缓存层
FROM node:20-alpine AS build
WORKDIR /app

# 先 COPY 依赖文件（利用缓存，不因源码变更重复安装）
COPY package.json package-lock.json ./
RUN npm ci                              # CI 严格安装

# 再 COPY 源码（源码变更不影响依赖缓存层）
COPY . .
RUN npm run build
```

> 🎯 **后端必记**：npm 安全 = `npm audit`（扫描） + `package-lock.json`（版本锁定） + Verdaccio 私服（内部包管理）。和 Maven 的 OWASP + Nexus + settings.xml 镜像配置是同一套思路。
