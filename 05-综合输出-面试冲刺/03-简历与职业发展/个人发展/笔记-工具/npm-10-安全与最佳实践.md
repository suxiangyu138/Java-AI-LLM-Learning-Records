# npm 安全与最佳实践

> 依赖安全是前端安全最容易被忽略的环节。一个恶意包可以拿走你的环境变量、SSH 密钥、甚至源代码。

---

## 一、依赖安全风险全景

### 1.1 风险类型

```
依赖安全风险
├── 已知漏洞（CVE）
│   某个版本的包有公开的安全漏洞
│   例：lodash 原型污染 CVE-2020-8203
│
├── 供应链攻击
│   恶意代码被注入到依赖链中
│   例：event-stream 事件（2018 年，攻击者获取了发布权限，注入比特币窃取代码）
│
├── 名称抢注（Typosquatting）
│   注册与流行包名相似的恶意包
│   例：electorn（假冒 electron）、colorss（假冒 colors）
│
├── 依赖混淆（Dependency Confusion）
│   公开源注册与私有包同名的包
│   包管理器优先从公开源拉取，导致安装了恶意版本
│   例：2021 年安全研究员通过此方法入侵 Apple/Microsoft/Netflix
│
├── 过期依赖
│   长时间不升级，漏洞积累
│
└── 恶意 install 脚本
    npm install 时自动执行 preinstall/postinstall 脚本
    恶意脚本可读取环境变量、SSH 密钥、源代码
```

### 1.2 真实案例

| 事件 | 年份 | 影响 |
|------|------|------|
| **left-pad 事件** | 2016 | 作者 unpublish 了 11 行代码的包，导致 React/Babel 等数千项目构建失败 |
| **event-stream 攻击** | 2018 | 攻击者获取发布权限，注入比特币钱包窃取代码 |
| **eslint-scope 攻击** | 2018 | npm 账号被盗，发布恶意版本窃取 `.npmrc` 中的 token |
| **ua-parser-js 攻击** | 2021 | 开发者账号被钓鱼，发布带挖矿脚本的版本 |
| **coa/rc 攻击** | 2021 | 同 ua-parser-js，多个包同时被劫持 |
| **colors.js 破坏性更新** | 2022 | 作者故意在库中引入无限循环，作为"开源抗议" |

---

## 二、npm audit 安全审计

### 2.1 基础使用

```bash
# 扫描安全漏洞
npm audit

# JSON 格式输出（便于集成到 CI）
npm audit --json

# 只扫描生产依赖
npm audit --production

# 指定严重等级
npm audit --audit-level=high     # 只显示 high 和 critical
npm audit --audit-level=critical

# 自动修复（不引入破坏性变更）
npm audit fix

# 强制修复（可能引入破坏性变更）
npm audit fix --force

# 模拟修复（看会改什么，不真的改）
npm audit fix --dry-run
```

### 2.2 输出解读

```
# npm audit 输出示例
Moderate    Prototype Pollution
Package     lodash
Dependency  of  express
Path        express > body-parser > lodash
More info   https://github.com/advisories/GHSA-xxxx

found 3 vulnerabilities (1 low, 1 moderate, 1 high)
  run `npm audit fix` to fix them
```

### 2.3 CI/CD 集成

```json
{
  "scripts": {
    "audit": "npm audit --audit-level=high",
    "audit:prod": "npm audit --production --audit-level=moderate"
  }
}
```

```yaml
# GitHub Actions
- run: npm audit --audit-level=high
  continue-on-error: false
```

---

## 三、防范供应链攻击

### 3.1 锁定依赖版本

```bash
# 永远提交 package-lock.json！
git add package-lock.json

# CI 中使用 ci 而非 install
npm ci            # ✅ 严格按 lock 文件安装
npm install       # ❌ CI 中不用，可能漂移版本
```

### 3.2 禁用 install 脚本

```bash
# 全局禁止自动执行 install 脚本
npm config set ignore-scripts true

# 单次执行
npm install --ignore-scripts

# 手动运行受信任包的 install 脚本
npm rebuild <package-name>
```

`.npmrc` 配置：
```
ignore-scripts=true
```

### 3.3 审查包的可信度

```bash
# 查看包的详细信息
npm view <package-name>

# 查看维护者数量
npm view <package-name> maintainers

# 查看每周下载量（下载量越高相对越可信）
npm view <package-name> --json | grep downloads

# 查看是否有已知漏洞
npm audit
```

**选包 Checklist**：
- [ ] 每周下载量高（百万级很安全，几十个要小心）
- [ ] 最近有更新（超过 1 年未更新的非稳定包值得怀疑）
- [ ] 维护者数量 > 1
- [ ] GitHub ⭐ 数量合理
- [ ] 有 README 和 CHANGELOG
- [ ] 名称不像是拼写错误（typosquatting）
- [ ] 依赖数量合理（依赖数百个其他包的要谨慎）
- [ ] install 脚本行为透明

### 3.4 Socket.dev / Snyk 安全检测

```bash
# Socket.dev CLI 扫描依赖
npx socket npm install

# Snyk 扫描
npx snyk test
npm install -g snyk
snyk auth
snyk test
snyk monitor
```

---

## 四、私密信息保护

### 4.1 .npmrc 中的 Token

```bash
# ❌ 永远不要在 .npmrc 中硬编码 token
//registry.npmjs.org/:_authToken=npm_abcdefg123456

# ✅ 使用环境变量
//registry.npmjs.org/:_authToken=${NPM_TOKEN}

# ✅ 或在用户级 ~/.npmrc（不会被提交到 Git）
```

### 4.2 .npmignore 防止泄露

```
# .npmignore（如果用了 npm publish）
.env
.env.local
.npmrc（含 token 的）
.git
node_modules
tests
src（如果发布了 dist）
```

```bash
# 发布前模拟预览
npm publish --dry-run
```

### 4.3 Git 泄露防范

```bash
# .gitignore 必须包含
node_modules/
.env
.env.local
*.log
.npmrc（如果含 token）
```

---

## 五、最佳实践清单

### 5.1 依赖管理

```
□ 始终提交 package-lock.json 到 Git
□ CI 环境使用 npm ci 而非 npm install
□ 定期运行 npm outdated 检查过期依赖
□ 定期运行 npm audit 检查安全漏洞
□ 不要使用 "*" 或 "latest" 这种浮动版本号
□ 使用 --save-exact(-E) 安装关键依赖
□ CD 管道中加入 npm audit --audit-level=high
□ 使用 dependabot / renovate 自动更新依赖
```

### 5.2 包选型

```
□ 优先选择流行度高、维护活跃的包
□ 检查包的依赖树（npm ls <pkg>），避免引入巨大依赖链
□ 对于小功能（如 left-pad），自己写而不是依赖第三方
□ 固定包的主版本（^），避免破坏性更新
□ 作用域包（@org/pkg）比非作用域更可信（有组织背书）
```

### 5.3 安全配置

```
□ 配置 .npmrc 启用 ignore-scripts=true（生产环境）
□ Token 用环境变量注入，不写死在文件
□ 启用双因素认证（npm profile enable-2fa auth-and-writes）
□ 使用 .npmrc 的 registry 限定作用域
□ 配置 engine-strict=true 强制 Node 版本
```

### 5.4 自动化审计

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "npm"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    versioning-strategy: increase
    labels:
      - "dependencies"
      - "automerge"
```

---

## 六、lock 文件校验

### 6.1 为什么需要校验

`package-lock.json` 中的 `integrity` 字段记录了每个包的哈希值。npm 安装时会校验下载的包是否与哈希匹配，防篡改。

```json
{
  "node_modules/vue": {
    "version": "3.4.0",
    "integrity": "sha512-xxx..."        ← SHA-512 哈希
  }
}
```

### 6.2 手动校验

```bash
# 验证本地 node_modules 与 lock 文件是否一致
npm ci --dry-run

# 查看某个包的完整性哈希
npm view vue dist.integrity
```

---

> **核心守则**：提交 lock 文件、用 npm ci、禁 install 脚本、定期 audit、Token 不硬编码。对于小功能能自己写的就自己写——left-pad 的教训永不过时。
