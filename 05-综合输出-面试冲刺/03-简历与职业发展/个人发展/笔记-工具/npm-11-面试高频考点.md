# npm 面试高频考点

> 从基础到原理，常见 npm 面试题的深度解答。

---

## 一、基础知识题

### Q1：dependencies 和 devDependencies 的区别？

**标准回答**：

- `dependencies`：**生产依赖**，项目上线运行必需的包。用户使用你的项目时也需要安装。如 Vue、React、Axios、Express。
- `devDependencies`：**开发依赖**，仅在开发、构建、测试阶段使用的包。用户不需要。如 Webpack、Vite、ESLint、Jest、TypeScript。

**安装行为**：
```bash
npm install             # 两者都装
npm install --production   # 只装 dependencies
npm ci --production        # CI 中只装生产依赖
```

**追问：框架作者应该把 Vue 放在 dependencies 还是 peerDependencies？**
- 如果是**应用项目**：放在 `dependencies`
- 如果是**组件库/插件**：放在 `peerDependencies` + `devDependencies`（peerDependencies 声明兼容版本，devDependencies 本地开发用）

---

### Q2：package.json 和 package-lock.json 的关系？

```
package.json：声明"我需要什么"（版本范围 ^2.1.3）
package-lock.json：锁定"实际装了什么"（精确版本 2.1.5）

为什么需要两个文件？
- package.json 表达开发者意图（我要 2.x 最新版）
- package-lock.json 保证环境一致性（谁装都是 2.1.5 这个确定版本）

类比：
package.json     = 菜单（我要一份麻婆豆腐）
package-lock.json = 后厨记录（今天用的是XX牌豆腐、XX克辣椒）
                   下次再做时保证味道一致
```

**追问：只用 package.json 不用 lock 文件会怎样？**
不同时间/环境 `npm install` 可能安装不同版本，导致"我这能跑，你那炸了"。

---

### Q3：npm install 和 npm ci 的区别？

| 对比维 | npm install | npm ci |
|--------|-------------|--------|
| 读取 | package.json（版本范围）| package-lock.json（精确版本）|
| 写 lock | 会更新 | 不修改 |
| node_modules | 增量安装 | 先删除再重装 |
| 速度 | 较慢 | 更快 |
| lock 不一致时 | 更新 lock | 直接报错退出 |
| 适用场景 | 本地开发 | **CI/CD / Docker** |

```dockerfile
# Docker 最佳实践
COPY package.json package-lock.json ./
RUN npm ci --production
```

---

### Q4：^ 和 ~ 有什么区别？

| 符号 | 含义 | 示例 | 允许范围 |
|------|------|------|----------|
| `^` | 锁主版本 | `^2.1.3` | `>=2.1.3 <3.0.0` |
| `~` | 锁主+次版本 | `~2.1.3` | `>=2.1.3 <2.2.0` |
| 无 | 精确 | `2.1.3` | `=2.1.3` |

**特殊规则**：
```
^0.2.3  →  行为退化为 ~0.2.3（0.x 版本视为不稳定）
^0.0.3  →  行为等同于 0.0.3（精确匹配）
```

**常用策略**：日常开发用 `^`（享受新功能和 bug 修复），关键依赖用 `~` 或 `-E` 精确锁定。

---

## 二、原理深度题

### Q5：npm install 的完整流程是怎样的？

```
npm install 的 7 个步骤：

1. 读取配置：.npmrc、全局配置、环境变量
2. 依赖解析：解析 package.json → 构建依赖树 → 版本范围 → 具体版本
3. 检查缓存：在 ~/.npm/_cacache 中查找包，有则跳过下载
4. 下载包体：从 Registry 下载 .tgz 压缩包
5. 解压：将包解压到 node_modules → 执行扁平化算法
6. 执行生命周期：preinstall → install → postinstall → prepare
7. 写入 lock 文件：生成/更新 package-lock.json
```

**追问：npm ci 的区别？**
跳过步骤 2 和 7，直接用 lock 文件中的精确版本，先清空 node_modules 再重装，不修改 lock 文件。

---

### Q6：什么是"幽灵依赖"？

```
幽灵依赖（Phantom Dependency）：

你的 package.json 只声明了 express
但 express 依赖 lodash
→ npm 扁平化安装时，lodash 被提升到顶层 node_modules
→ 你的代码可以直接 require('lodash')，即使你没声明它！

问题：
- 某天 express 不依赖 lodash 了 → lodash 从顶层消失 → 你的代码 💥
- 本地能跑，CI 炸了（版本差异）
- 迁移到 pnpm 直接报错（pnpm 禁止幽灵依赖）
```

**解决方案**：
```bash
# 1. 把缺失的依赖明确加到 package.json
npm install lodash

# 2. 使用 pnpm（天然杜绝）
pnpm install

# 3. ESLint 检查
npm install -D eslint-plugin-import
# .eslintrc: 'import/no-extraneous-dependencies': 'error'
```

---

### Q7：peerDependencies 是什么？什么时候用？

```
peerDependencies（对等依赖）：

一般依赖：npm install 时自动安装 → 每个包自带一份依赖
对等依赖：声明"宿主项目需要自行安装" → 多个插件共享同一个依赖

使用场景：开发插件/组件库时

例：你写了一个 React 组件库
{
  "peerDependencies": {
    "react": ">=16.8.0"
  },
}

含义：
- "我的库兼容 React 16.8+"
- "但我不会自带一份 React，请使用此库的项目自行安装 React"
- 版本不匹配时会警告

版本行为：
- npm 6 及以前：仅警告，不自动安装
- npm 7+：自动安装 peerDependencies
```

---

### Q8：npm 的缓存机制是怎样的？

```
缓存位置：~/.npm/_cacache/

目录结构：
├── content-v2/      # 包的实际内容（按 SHA-512 哈希存储为文件）
└── index-v5/        # 元数据索引（包名 → 版本 → 哈希 → 文件路径映射）

查找流程：
npm install axios@1.6.0
  → 查 index-v5：axios@1.6.0 的哈希是什么？
  → 在 content-v2 中找对应哈希的文件
  → 有 → 解压到 node_modules（跳过下载）
  → 没 → 下载 → 存入缓存 → 解压

命令：
npm cache verify          # 验证缓存完整性
npm cache clean --force   # 强制清空（少用）
npm install --prefer-offline  # 优先用缓存
```

---

### Q9：npx 的原理是什么？

```
npx 的执行逻辑：

npx <command>
   ↓
1. 在本地 node_modules/.bin/ 中查找
   → 有 → 直接执行
   → 没有 ↓
2. 临时下载到缓存
3. 执行命令
4. 清理临时文件

与 npm run 的区别：
├── npm run：执行 package.json scripts 中定义的命令
└── npx：可以执行任何 npm 包的命令（包括未安装的）

实用场景：
npx create-vite my-app     # 创建项目（不用全局安装脚手架）
npx http-server -p 8080    # 临时起静态服务器
npx kill-port 3000         # 临时工具
```

---

### Q10：npm vs yarn vs pnpm 有什么区别？

| 维度 | npm | yarn | pnpm |
|------|-----|------|------|
| 安装速度 | 基准 | 较快 | **最快** |
| 磁盘效率 | 低（每项目复制一份）| 低 | **极高**（全局存储+硬链接）|
| node_modules | 扁平化 | 扁平化 | **非扁平**（软链接）|
| 幽灵依赖 | 存在 | 存在 | **不存在** |
| lock 文件 | package-lock.json | yarn.lock | pnpm-lock.yaml |
| monorepo | 基础支持 | 支持 | **最成熟** |
| 推荐场景 | 个人/入门 | 遗留项目 | **团队/企业** |

**pnpm 为什么快**：
- 全局硬链接存储，不重复下载
- 非扁平化 node_modules，解析快
- 并行安装效率高

**pnpm 为什么省磁盘**：
- 所有项目的同一个包都硬链接到全局 store 的同一个文件
- 10 个项目用 React = 1 份 React 文件 + 10 组软链接

---

## 三、实战场景题

### Q11：线上出了个包的安全漏洞，是深层依赖导致的，怎么处理？

```
场景：你项目用了 package-a，package-a 依赖了 package-b，
     package-b@1.0.0 有严重漏洞

排查：
npm ls package-b    # 查看为什么装了 package-b

处理方案（优先级从高到低）：

1. 升级上游依赖
   npm update package-a   # 希望它升级了 package-b 的版本

2. 强制覆写（npm 8.3+）
   {
     "overrides": {
       "package-b": "^2.0.0"
     }
   }
   # npm install 后生效

3. 临时 fork
   # fork package-a → 修改它的依赖 → 装自己的 fork
   npm install github:yourname/package-a

4. npm audit fix --force
   # 可能引入破坏性变更，谨慎使用
```

---

### Q12：如何解决 npm install 时的 node-gyp 报错？

```
node-gyp 用来编译 C++ 原生模块（如 node-sass、bcrypt）

Windows 解决方案：
npm install --global windows-build-tools   # 安装 Python + C++ 编译工具
# 或安装 Visual Studio Build Tools + Python 3.x

macOS/Linux：
xcode-select --install   (macOS)
apt install build-essential python3  (Ubuntu)

更好的方案：
换掉需要编译的包！
node-sass → sass (Dart Sass，纯 JS)
node-gyp 的原生模块 → 用 WASM 版本的替代品
```

---

### Q13：Monorepo 中多个子包如何互相依赖？

```json
// packages/shared/package.json
{
  "name": "@my-org/shared",
  "version": "1.0.0"
}

// packages/web/package.json
{
  "dependencies": {
    "@my-org/shared": "workspace:*"
  }
}
```

```bash
# npm
npm install @my-org/shared -w packages/web

# pnpm（更推荐）
pnpm --filter @my-org/web add @my-org/shared
```

---

### Q14：npm install 报 ERESOLVE unable to resolve dependency tree 怎么办？

```
原因：peerDependencies 版本冲突（npm 7 起更严格）

方案 1（推荐）：理解冲突并修复
npm ls <冲突的包>     # 找出冲突链
# 升级相关依赖到兼容版本

方案 2：临时绕过（不推荐，但项目能跑了）
npm install --legacy-peer-deps   # 用 npm 6 的行为，忽略冲突

方案 3：强行安装
npm install --force

根本解决：切换到 pnpm（它对 peerDependencies 处理更合理）
```

---

## 四、快速模拟面试

```
Q: npm 和 npx 有什么区别？
A: npm 是包管理器（下载、安装、管理依赖），npx 是包执行器（临时下载并执行命令）。
   npm run dev 执行 scripts 定义的命令，npx 可以执行任何 npm 包的 CLI，包括未安装的。

Q: 为什么 node_modules 这么大？
A: 因为 npm 用扁平化安装，所有依赖都展开在顶层。每个包又依赖了很多其他包。
   pnpm 通过硬链接 + 全局存储解决了这个问题。

Q: CI/CD 用 npm install 还是 npm ci？
A: npm ci。它按 lock 文件的精确版本安装，速度更快且环境可复现。

Q: 怎么发布一个 npm 包？
A: npm login → 确保 package.json 字段正确 → npm version patch → npm publish → git push --follow-tags

Q: 项目中同时用了 npm 和 yarn 怎么办？
A: 选一个统一。删掉其中一个的 lock 文件和 node_modules，全团队同意后迁移。
```

---

> **面试核心**：基础知识（dep/devDep/^/~）→ 安装原理（扁平化/幽灵依赖）→ 工具对比（npm/yarn/pnpm）→ 实战问题（漏洞修复/版本冲突）。把上面 14 题吃透，npm 相关的面试问题基本全覆盖。
