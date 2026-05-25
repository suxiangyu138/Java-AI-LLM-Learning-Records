# npm scripts 工程化实战

> `npm scripts` 是前端工程化的调度中心——用脚本串联开发、构建、测试、部署全流程。

---

## 一、scripts 基础回顾

### 1.1 定义

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "test": "vitest"
  }
}
```

```bash
npm run dev       # 执行命令
npm run build     # 执行命令
npm test          # 内置简写（不用 run）
```

### 1.2 为什么用 npm scripts 而不是直接敲命令

```
直接敲命令：
  → 记不住参数（vite build --mode production --base /app/ --outDir dist）
  → 每人敲的不一样 → 新同事文档找半天 → 错误排查困难

npm scripts：
  → 一条 npm run build 解决
  → 全团队统一
  → 新人看 package.json 就知道怎么启动项目
  → CI/CD 直接用 npm run xxx，与本地一致
```

---

## 二、pre / post 钩子

### 2.1 自动执行规则

```json
{
  "scripts": {
    "prebuild": "npm run lint",
    "build": "vite build",
    "postbuild": "node scripts/upload-cdn.js"
  }
}
```

```bash
npm run build

# 实际执行顺序：
# 1. npm run prebuild   → 自动触发
# 2. npm run build
# 3. npm run postbuild  → 自动触发
```

### 2.2 实用钩子组合

```json
{
  "scripts": {
    "predev": "node scripts/check-env.js",
    "dev": "vite",
    
    "prebuild": "npm run type-check && npm run lint",
    "build": "vite build",
    "postbuild": "node scripts/generate-sitemap.js",
    
    "pretest": "npm run type-check",
    "test": "vitest run",
    "posttest": "npm run coverage:report",
    
    "predeploy": "npm run build",
    "deploy": "scp -r dist/* user@server:/var/www/",
    "postdeploy": "curl -X POST https://api.example.com/deploy-notify"
  }
}
```

### 2.3 环境变量传递

```json
{
  "scripts": {
    "build:dev": "cross-env NODE_ENV=development vite build",
    "build:prod": "cross-env NODE_ENV=production vite build",
    "build:staging": "cross-env NODE_ENV=staging vite build"
  }
}
```

**cross-env**：跨平台设置环境变量（Windows 的 `SET` 和 Unix 的 `export` 语法不同）。

```bash
npm install -D cross-env
```

---

## 三、组合与并行执行

### 3.1 顺序执行（&&）

```json
{
  "scripts": {
    "build": "npm run clean && npm run compile && npm run bundle",
    "clean": "rimraf dist",
    "compile": "tsc",
    "bundle": "vite build"
  }
}
```

`&&` 表示前一个命令**成功**后才执行下一个（exit code 0）。

### 3.2 并行执行（& / concurrently）

```bash
# Linux/macOS 用 &，Windows 不行
"dev": "vite & node mock-server.js"

# 推荐：concurrently（跨平台并行）
npm install -D concurrently
```

```json
{
  "scripts": {
    "dev": "concurrently \"vite\" \"node mock-server.js\" \"sass --watch src:dist\"",
    "dev:names": "concurrently -n vite,mock,sass \"vite\" \"node mock-server.js\""
  }
}
```

### 3.3 条件执行

```json
{
  "scripts": {
    "build": "npm run lint && npm run bundle",
    "lint": "eslint src/",
    "bundle": "vite build",
    
    "build:force": "npm run lint || true && npm run bundle",
    "build:win": "npm run lint || ver>nul && npm run bundle"
  }
}
```

| 操作符 | 含义 |
|--------|------|
| `cmd1 && cmd2` | cmd1 成功后才执行 cmd2 |
| `cmd1 \|\| cmd2` | cmd1 失败才执行 cmd2 |
| `cmd1 & cmd2` | 并行执行 |
| `cmd1 \| cmd2` | cmd1 的输出作为 cmd2 的输入 |

---

## 四、实战工程化配置

### 4.1 Vue 3 + TypeScript 项目

```json
{
  "scripts": {
    "dev": "vite --host 0.0.0.0 --port 3000",
    "build": "run-s type-check build-only",
    "build-only": "vite build",
    "preview": "vite preview",
    "type-check": "vue-tsc --noEmit",
    "lint": "eslint . --ext .vue,.js,.ts,.jsx,.tsx",
    "lint:fix": "eslint . --ext .vue,.js,.ts --fix",
    "format": "prettier --write .",
    "test": "vitest",
    "test:ui": "vitest --ui",
    "test:coverage": "vitest run --coverage",
    "prepare": "husky install",
    "release": "standard-version",
    "analyze": "vite build --mode analyze"
  }
}
```

### 4.2 React + TypeScript 项目

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext .ts,.tsx",
    "lint:fix": "eslint src --ext .ts,.tsx --fix",
    "format": "prettier --write \"src/**/*.{ts,tsx,css,scss}\"",
    "test": "vitest",
    "test:coverage": "vitest run --coverage",
    "e2e": "playwright test",
    "e2e:ui": "playwright test --ui",
    "storybook": "storybook dev -p 6006",
    "build-storybook": "storybook build",
    "prepare": "husky install"
  }
}
```

### 4.3 Node.js 后端项目

```json
{
  "scripts": {
    "dev": "tsx watch src/index.ts",
    "build": "tsc",
    "start": "node dist/index.js",
    "lint": "eslint src/ --ext .ts",
    "test": "vitest run",
    "test:watch": "vitest",
    "db:migrate": "prisma migrate dev",
    "db:seed": "prisma db seed",
    "db:studio": "prisma studio",
    "docker:build": "docker build -t my-app .",
    "docker:run": "docker run -p 3000:3000 my-app",
    "deploy": "npm run build && npm run docker:build && npm run docker:run"
  }
}
```

### 4.4 Monorepo 项目（npm workspaces）

```json
{
  "private": true,
  "workspaces": ["packages/*"],
  "scripts": {
    "dev": "npm run dev --workspaces",
    "build": "npm run build --workspaces",
    "test": "npm run test --workspaces --if-present",
    "lint": "npm run lint --workspaces --if-present",
    "dev:web": "npm run dev -w packages/web",
    "dev:api": "npm run dev -w packages/api",
    "build:shared": "npm run build -w packages/shared",
    "clean": "npm run clean --workspaces --if-present"
  }
}
```

### 4.5 使用 npm-run-all 管理复杂流水线

```bash
npm install -D npm-run-all
```

```json
{
  "scripts": {
    "clean": "rimraf dist",
    "type-check": "vue-tsc --noEmit",
    "lint": "eslint src/",
    "build:css": "sass src/:dist/",
    "build:js": "vite build",
    
    "build": "run-s clean type-check build:*",
    "build:parallel": "run-p build:css build:js",
    "dev": "run-p dev:*",
    "dev:server": "vite",
    "dev:mock": "node mock/index.js"
  }
}
```

| 命令 | 等价于 |
|------|--------|
| `run-s a b c` | `npm run a && npm run b && npm run c` |
| `run-p a b c` | `concurrently "npm run a" "npm run b" "npm run c"` |

---

## 五、Git Hooks 集成

### 5.1 husky + lint-staged

```bash
npm install -D husky lint-staged
npx husky init
```

```json
// package.json
{
  "scripts": {
    "prepare": "husky install"
  },
  "lint-staged": {
    "*.{js,ts,vue}": ["eslint --fix", "prettier --write"],
    "*.{css,scss,md}": ["prettier --write"]
  }
}
```

```bash
# .husky/pre-commit
npx lint-staged

# .husky/commit-msg
npx --no -- commitlint --edit $1

# .husky/pre-push
npm run test
```

### 5.2 commitlint 配置

```bash
npm install -D @commitlint/cli @commitlint/config-conventional
```

```js
// commitlint.config.js
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', [
      'feat',      // 新功能
      'fix',       // bug 修复
      'docs',      // 文档
      'style',     // 格式（不影响代码运行）
      'refactor',  // 重构
      'perf',      // 性能优化
      'test',      // 测试
      'chore',     // 构建/工具变更
      'ci',        // CI 配置
      'revert'     // 回退
    ]]
  }
};
```

---

## 六、常用 npm scripts 工具包

| 包 | 用途 | 命令示例 |
|----|------|----------|
| **cross-env** | 跨平台环境变量 | `cross-env NODE_ENV=production` |
| **concurrently** | 并行执行命令 | `concurrently "vite" "mock"` |
| **npm-run-all** | 串行 + 并行管理 | `run-s a b c` / `run-p a b c` |
| **rimraf** | 跨平台删除目录 | `rimraf dist` |
| **nodemon** | 文件变化自动重启 | `nodemon src/index.ts` |
| **tsx** | 直接运行 TypeScript | `tsx src/index.ts` |
| **tsc** | TypeScript 编译 | `tsc --noEmit` |
| **husky** | Git hooks 管理 | 自动执行 pre-commit 等钩子 |
| **lint-staged** | 只检查暂存文件 | 快速 lint + format |
| **commitlint** | commit 信息校验 | 保证 commit message 规范 |
| **standard-version** | 自动生成 CHANGELOG | `standard-version` |
| **http-server** | 零配置静态服务 | `http-server dist -p 8080` |
| **serve** | 更好看的静态服务 | `serve dist` |
| **wait-on** | 等待服务启动 | `wait-on http://localhost:3000` |

---

## 七、环境区分与部署

### 7.1 多环境构建

```json
{
  "scripts": {
    "build:dev": "cross-env APP_ENV=development vite build --mode development",
    "build:test": "cross-env APP_ENV=test vite build --mode test",
    "build:staging": "cross-env APP_ENV=staging vite build --mode staging",
    "build:prod": "cross-env APP_ENV=production vite build --mode production",
    "deploy:staging": "npm run build:staging && scp -r dist/* deploy@staging:/app/",
    "deploy:prod": "npm run build:prod && scp -r dist/* deploy@prod:/app/",
    "docker:build": "docker build --build-arg APP_ENV=production -t app:latest .",
    "docker:push": "docker push registry.example.com/app:latest"
  }
}
```

### 7.2 CI/CD 脚本

```yaml
# .github/workflows/deploy.yml
jobs:
  deploy:
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'npm'
      - run: npm ci
      - run: npm run lint
      - run: npm run type-check
      - run: npm run test
      - run: npm run build
      - run: npm run deploy
```

---

> **核心思想**：npm scripts 是项目的操作手册——每个开发者看一眼 scripts 就知道项目怎么跑。pre/post 钩子做前置/后置处理，concurrently/run-p 并行提效，husky+lint-staged 保证代码质量。好的 scripts 配置 = 新人 5 分钟上手。
