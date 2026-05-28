# npm 核心命令大全

> 按使用频率和场景分类，覆盖日常开发 95% 的命令需求。

---

## 一、项目初始化

```bash
npm init              # 交互式创建 package.json
npm init -y           # 跳过问答，直接生成默认 package.json
npm init vue@latest   # 使用 create-vue 脚手架创建项目
```

`npm init <initializer>` 本质是执行 `npx create-<initializer>`，具体看下一节 npx 详解。

---

## 二、依赖安装（install / i）

### 2.1 基础安装命令

```bash
# 安装所有依赖（根据 package.json 和 package-lock.json）
npm install
npm i

# 安装指定包（生产依赖）
npm install axios
npm i lodash

# 安装指定包（开发依赖）
npm install eslint -D
npm i typescript --save-dev

# 安装全局包
npm install -g typescript
npm i -g @vue/cli

# 安装指定版本
npm i vue@3.4.0
npm i react@^18.0.0
npm i lodash@latest
```

### 2.2 安装标记速查

| 简写 | 全写 | 含义 | 影响 |
|------|------|------|------|
| `-P` / 无 | `--save-prod` | 生产依赖 | 写入 `dependencies` |
| `-D` | `--save-dev` | 开发依赖 | 写入 `devDependencies` |
| `-O` | `--save-optional` | 可选依赖 | 写入 `optionalDependencies` |
| `--no-save` | — | 不记录到 package.json | 仅下载到 node_modules |
| `-g` | `--global` | 全局安装 | 安装到全局目录 |
| `-E` | `--save-exact` | 锁定精确版本 | 去掉 `^` / `~` 前缀 |

### 2.3 生产环境安装

```bash
# CI/CD 或 Docker 构建中，只安装 dependencies
npm install --production
npm ci --production
```

### 2.4 安装原理简述

```
npm install axios
      │
      ▼
1. 解析 axios 的最新版本号
2. 下载 axios 及其依赖到 node_modules（扁平化）
3. 更新 package.json 的 dependencies
4. 更新 package-lock.json 锁定精确版本
```

---

## 三、依赖卸载（uninstall / un）

```bash
npm uninstall axios         # 卸载 + 从 dependencies 移除
npm un axios -D             # 卸载 + 从 devDependencies 移除
npm un -g typescript        # 卸载全局包
npm rm lodash               # rm 是 uninstall 的别名
```

---

## 四、依赖更新（update / outdated）

```bash
# 查看哪些包有可用的更新
npm outdated

# 输出示例：
# Package   Current  Wanted  Latest  Location
# vue       3.3.0    3.4.5   4.0.0   my-project

# 按语义版本更新
npm update                 # 更新所有包到 Wanted 版本
npm update axios           # 更新单个包

# 安装最新版本（无视 SemVer 限制）
npm i axios@latest
```

### outdated 列含义

| 列 | 含义 |
|----|------|
| Current | `node_modules` 中实际安装的版本 |
| Wanted | `package.json` 版本范围允许的最高版本 |
| Latest | Registry 上的最新版本 |

---

## 五、npm ci（CI/CD 专属安装）

```bash
npm ci
```

### npm install vs npm ci

| 对比项 | `npm install` | `npm ci` |
|--------|--------------|----------|
| 读取文件 | package.json（版本范围） | package-lock.json（精确版本）|
| 速度 | 较慢 | **更快** |
| 修改 lock 文件 | 会更新 | **不修改**（lock 不一致直接报错）|
| node_modules | 增量安装 | **先删除再重装**（干净环境）|
| 适用场景 | 本地开发 | **CI/CD / Docker 构建** |
| 前提条件 | 无 | 必须有 package-lock.json |

```dockerfile
# Dockerfile 中的最佳实践
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .
```

---

## 六、全局包管理

```bash
npm ls -g --depth=0      # 查看全局安装的包
npm root -g               # 查看全局 node_modules 路径
npm outdated -g           # 查看全局包更新
```

### 哪些包适合全局安装

```
✅ 适合全局安装：
   - CLI 工具：@vue/cli, create-react-app, typescript, ts-node
   - 代码格式化：prettier
   - 包管理器：pnpm, yarn
   - 进程管理：pm2, nodemon
   - HTTP 服务：http-server, serve

❌ 不适合全局安装：
   - 项目运行时的依赖（vue, react, axios）
   - 打包构建工具（webpack, vite）—— 应该作为 devDependencies
```

---

## 七、信息查看

```bash
# 版本查看
npm -v                  # npm 自身版本
npm view vue version    # 查看 Vue 最新版本
npm view vue versions   # 查看 Vue 所有历史版本
npm view vue versions --json  # JSON 格式输出

# 包信息
npm view axios                         # 查看 axios 的全部元数据
npm view axios description             # 只看描述
npm view axios dependencies            # 只看它的依赖
npm view axios repository.url          # 只看仓库地址
npm view axios license                 # 只看许可证
npm search axios                       # 搜索相关包

# 项目依赖树
npm list                     # 查看项目的依赖树
npm list --depth=0           # 只看顶层（你直接装的包）
npm list --depth=1           # 向下看一层子依赖
npm list axios               # 查看 axios 的依赖关系（在哪被依赖的）

# 包信息本地查询
npm ls axios                 # 查看本地是否安装了 axios，什么版本

# 文档
npm docs axios               # 在浏览器打开 axios 的文档/主页
npm repo vue                 # 在浏览器打开 vue 的仓库地址
```

---

## 八、缓存管理

```bash
npm cache verify            # 验证缓存完整性（推荐代替 clean）
npm cache clean --force     # 强制清空缓存（少用）
npm config get cache        # 查看缓存目录位置
```

npm 缓存目录结构：
```
~/.npm/_cacache/
├── content-v2/    # 包的实际文件内容（按哈希存储）
├── index-v5/      # 元数据索引（包名→版本→哈希的映射）
└── tmp/           # 临时文件
```

---

## 九、安全审计

```bash
npm audit                     # 扫描项目依赖的安全漏洞
npm audit fix                 # 自动修复（不会引入破坏性变更的包）
npm audit fix --force         # 强制修复（可能引入破坏性变更）
npm audit --json              # JSON 格式输出漏洞报告
npm audit --production        # 只检查生产依赖
```

### 漏洞严重等级

| 等级 | 含义 |
|------|------|
| Low | 低风险 |
| Moderate | 中等风险 |
| High | 高风险，可能被利用 |
| Critical | 严重，**需要立即修复** |

---

## 十、npx — 临时执行包的命令

### 10.1 npx 是什么

npx（npm package executer）从 npm 5.2 起内置，作用是**临时下载并执行一个包**，用完即删。

### 10.2 核心用法

```bash
# 执行本地安装的包（和 npm run 等价但不用写到 scripts）
npx vite

# 临时下载并执行（不污染 node_modules）
npx create-react-app my-app        # 创建完就删除 create-react-app
npx http-server -p 8080            # 临时起一个静态服务器
npx cowsay "hello"                 # 临时下载 cowsay 并执行

# 执行指定版本的包
npx create-react-app@5 my-app

# 从 GitHub 执行
npx github:user/repo

# 从 Gist 执行
npx https://gist.github.com/user/xxxxx.js
```

### 10.3 npx 执行逻辑

```
npx <command>
      │
      ▼
1. 本地 node_modules/.bin/ 有吗？
   ├─ 有 → 直接用本地的
   └─ 没有 ↓
2. 临时下载到缓存目录
3. 执行完毕
4. 清理（临时包不保留）
```

### 10.4 实用场景

```bash
# 1. 脚手架创建项目（不用先全局安装）
npx create-vite my-app --template vue-ts

# 2. 临时工具（用完就走）
npx kill-port 3000              # 杀掉占用 3000 端口的进程
npx npm-check-updates           # 检查依赖更新
npx tsc --noEmit                # TypeScript 类型检查
npx prettier --write .          # 一次性代码格式化

# 3. 环境隔离（不同项目用不同版本的脚手架）
npx create-react-app@4 legacy-project
npx create-react-app@5 new-project
```

---

## 十一、其他实用命令

```bash
# 查看全局包
npm ls -g --depth=0

# 查看包的作者/贡献者
npm view axios contributors

# 弃用某个版本
npm deprecate my-package@"<1.0.0" "不再维护，请升级到 2.x"

# 标记版本为废弃
npm deprecate my-package "这个包已废弃"

# 查看当前登录用户
npm whoami

# 登出
npm logout

# 发布（需要先 npm login）
npm publish

# 发布到指定标签
npm publish --tag beta

# 删除已发布的包（72 小时内可恢复）
npm unpublish my-package --force

# 废弃包（推荐，比 unpublish 更文明）
npm deprecate my-package "no longer maintained"
```

---

## 十二、命令别名速查表

| 命令 | 别名 |
|------|------|
| `npm install` | `npm i` |
| `npm install --save-dev` | `npm i -D` |
| `npm uninstall` | `npm un` / `npm rm` / `npm r` |
| `npm update` | `npm up` |
| `npm list` | `npm ls` / `npm la` / `npm ll` |
| `npm test` | `npm t` / `npm tst` |
| `npm start` | `npm run start` 的简写 |
| `npm run-script` | `npm run` |
| `npm search` | `npm s` / `npm se` / `npm find` |
| `npm version` | （无别名） |

---

> **高频命令记忆口诀**：`i` 装、`un` 卸、`up` 更、`ls` 查、`run` 执行、`ci` 部署、`audit` 安全。
