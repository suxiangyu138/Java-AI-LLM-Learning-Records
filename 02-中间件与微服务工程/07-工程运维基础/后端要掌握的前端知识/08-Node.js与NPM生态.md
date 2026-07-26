# 08-Node.js与NPM生态
> 🎯 前端工程化的基石 — 理解Node.js运行时、NPM包管理、构建工具和脚手架，为什么前端项目需要"编译"才能运行

---

## 目录
1. [Node.js是什么](#1-nodejs是什么)
2. [NPM包管理 — 前端的Maven](#2-npm包管理--前端的maven)
3. [前端构建工具](#3-前端构建工具)
4. [前端项目结构解读](#4-前端项目结构解读)
5. [后端需要知道的NPM命令](#5-后端需要知道的npm命令)

---

## 1. Node.js是什么

> Node.js = Chrome V8引擎 + 文件系统和网络API → 让JavaScript可以脱离浏览器运行在服务器端。

### 1.1 后端怎么理解Node.js

```text
后端类比：
  Node.js   ≈  JVM（运行环境）
  JavaScript ≈ Java（语言）
  NPM       ≈ Maven（包管理+构建）
  Vite      ≈ Maven/Gradle插件（构建工具）

前端需要Node.js不是因为"前端用Node写服务"，而是因为：
  ✅ 前端构建工具(Vite/Webpack)运行在Node.js上
  ✅ 前端依赖管理通过NPM
  ✅ 前端开发服务器(Vite dev server)基于Node.js
```

### 1.2 Node.js版本管理

```bash
# 建议使用 nvm 管理Node版本
nvm install 20              # 安装Node 20 LTS
nvm use 20                  # 切换到20
node -v                     # 查看版本
npm -v                      # 查看NPM版本
```

---

## 2. NPM包管理 — 前端的Maven

### 2.1 核心概念对照

| NPM概念 | Maven/Gradle类比 | 说明 |
|---------|-----------------|------|
| `package.json` | `pom.xml` / `build.gradle` | 项目配置文件 |
| `node_modules/` | `~/.m2/repository/` | 依赖存放目录 |
| `npm install` | `mvn install` | 安装依赖 |
| `npm run dev` | `mvn spring-boot:run` | 启动开发服务器 |
| `npm run build` | `mvn package` | 构建生产包 |
| `dependencies` | `<dependencies>` | 运行时依赖 |
| `devDependencies` | `<scope>test</scope>` | 仅开发时依赖 |

### 2.2 package.json 解读

```json
{
  "name": "my-vue-app",
  "version": "1.0.0",
  "scripts": {
    // ← 类似Maven的<executions>
    "dev": "vite",              // npm run dev
    "build": "vite build",      // npm run build
    "preview": "vite preview"   // npm run preview
  },
  "dependencies": {
    // ← 运行时依赖（打包进最终产物）
    "vue": "^3.4.0",            // ^3.4.0 ≈ >=3.4.0且<4.0.0
    "axios": "^1.6.0"
  },
  "devDependencies": {
    // ← 仅开发时依赖（不打包进最终产物）
    "vite": "^5.0.0",           // 构建工具
    "@vitejs/plugin-vue": "^5.0.0"
  }
}
```

### 2.3 版本号语义

```text
"vue": "^3.4.0"  → 允许 3.4.0 ~ 3.x.x（不破坏兼容性的更新）
"vue": "~3.4.0"  → 允许 3.4.0 ~ 3.4.x（仅补丁更新）
"vue": "3.4.0"   → 锁定 3.4.0（精确版本）

package-lock.json ≈ Maven的依赖树锁定文件
  → 确保团队成员安装的依赖版本完全一致
  → 类似 Maven 的 dependencyManagement
```

---

## 3. 前端构建工具

### 3.1 为什么需要构建

```text
后端开发：.java → javac → .class → jar → 部署
前端开发：.vue/.js/.css → 构建工具(Vite) → .html/.js/.css → 放到CDN

构建过程做了什么：
  ✅ JS压缩(uglify)：变量名缩短、删除空格注释 → 文件更小
  ✅ CSS处理：Sass/Less → CSS、自动加浏览器前缀
  ✅ Tree Shaking：删除未使用的代码（类似ProGuard）
  ✅ 代码分割：按路由拆分JS文件（按需加载）
  ✅ Hot Module Replacement(HMR)：改代码浏览器自动刷新
```

### 3.2 Vite vs Webpack

| 维度 | Vite | Webpack |
|------|------|---------|
| 开发启动速度 | ⚡ 秒级 | 🐢 分钟级（大项目） |
| HMR速度 | ⚡ 即时 | 🐢 较慢 |
| 构建速度 | ⚡ 快（Rollup） | 🐢 较慢 |
| 生态 | 新，快速成长 | 成熟，插件丰富 |
| 推荐 | ✅ 新项目首选 | 兼容老项目 |

```bash
# 创建Vite + Vue项目（前端脚手架 ≈ Spring Initializr）
npm create vite@latest my-app -- --template vue
cd my-app && npm install && npm run dev
```

---

## 4. 前端项目结构解读

```text
my-vue-app/
├── package.json           # 项目配置（= pom.xml）
├── package-lock.json      # 依赖锁定（= dependency tree）
├── vite.config.js         # Vite构建配置（= pom.xml中的plugin配置）
├── index.html             # 入口HTML（= src/main/resources/static/index.html）
├── node_modules/          # 依赖包（= target/lib/）
├── public/                # 静态资源（= src/main/resources/static/）
└── src/                   # 源代码
    ├── main.js            # 入口JS（= SpringBoot的main方法）
    ├── App.vue            # 根组件
    ├── components/        # 公共组件
    ├── views/             # 页面组件（= Controller）
    ├── router/            # 路由配置（= @RequestMapping）
    ├── api/               # API调用（= Feign Client接口）
    └── assets/            # 图片/字体等资源
```

```text
后端一看就懂的结构映射：
  src/api/userApi.js      ≈ @FeignClient(name = "user-service")
  src/views/UserList.vue  ≈ @Controller + Thymeleaf模板
  src/router/index.js     ≈ @RequestMapping 路径映射
  src/components/Modal.vue ≈ 可复用的JSP include / Thymeleaf fragment
```

---

## 5. 后端需要知道的NPM命令

```bash
# 项目初始化
npm init                    # 创建package.json
npm install                 # 安装所有依赖（= mvn install）
npm install axios           # 安装axios并加到dependencies
npm install -D vite         # 安装vite加到devDependencies

# 开发
npm run dev                 # 启动开发服务器（= mvn spring-boot:run）
npm run build               # 构建生产包（= mvn package）
npm run preview             # 预览生产构建

# 依赖管理
npm list                    # 查看依赖树
npm outdated                # 查看可更新的依赖
npm update                  # 更新依赖

# 清理
rm -rf node_modules && npm install  # 清理重装（类似 mvn clean install）
```

> 🎯 **后端学Node.js/NPM的最小集合**：理解Node.js是前端工具链的运行时 → 能读懂`package.json` → 知道`npm install`和`npm run dev`是做什么的 → 能自己启动前端项目。
