# 04 - npm Scripts 与前端工程化

> 🎯 npm scripts 是前端工程化的"粘合剂" — 开发、构建、测试、部署全流程通过 scripts 串起来。Java 后端在 CI/CD 中主要接触的就是 `npm run build`

---

## 目录

1. [npm Scripts 机制](#1-npm-scripts-机制)
2. [前端构建工具链](#2-前端构建工具链)
3. [与 Java 后端项目的集成](#3-与-java-后端项目的集成)
4. [CI/CD 中的 npm](#4-cicd-中的-npm)

---

## 1. npm Scripts 机制

### 1.1 内置钩子

```json
{
  "scripts": {
    "prebuild": "rimraf dist",        // ← build 之前执行清理
    "build": "vite build",
    "postbuild": "echo '构建完成'",    // ← build 之后执行
    "preview": "vite preview",
    "test": "vitest run",
    "dev": "vite --port 3000"
  }
}
```

```bash
npm run build
# 实际执行顺序：prebuild → build → postbuild
# 等价于：rimraf dist → vite build → echo '构建完成'
```

| npm 内置生命周期 | 触发时机 | Java 对标 |
|-----------------|----------|----------|
| `pre<script>` | 脚本执行前 | Maven phase 前置 |
| `<script>` | 脚本本身 | Maven Goal |
| `post<script>` | 脚本执行后 | Maven phase 后置 |
| `prepare` | `npm install` 后（本地） | 编译期准备 |
| `prepublishOnly` | `npm publish` 前 | deploy 前检查 |

### 1.2 串联与并行

```json
{
  "scripts": {
    "build": "vite build",
    "lint": "eslint src --fix",
    "test": "vitest run",
    "check": "npm run lint && npm run test && npm run build",
    "dev": "concurrently \"vite\" \"json-server db.json\""
  }
}
```

---

## 2. 前端构建工具链

```text
前端项目构建流程（Vue/React 通用）：

src/                        编写代码
  │
  ├── npm run lint          ① 代码检查（ESLint）
  ├── npm test              ② 单元测试（Vitest/Jest）
  ├── npm run build         ③ 构建打包
  │     ├── 编译（Vite/Webpack）：Vue SFC → JS/CSS/HTML
  │     ├── Tree Shaking：去除未使用的代码
  │     ├── 代码分割：按路由拆分 Chunk
  │     └── 压缩：Terser（JS）+ PostCSS（CSS）
  └── dist/                 ④ 构建产物 → Nginx 静态部署
```

| 阶段 | 说明 | Java 对标 |
|------|------|----------|
| 代码检查 | ESLint 语法+规范检查 | Checkstyle / SpotBugs |
| 单元测试 | Vitest/Jest 测试 | Surefire / JUnit |
| 构建打包 | Vite/Webpack 编译打包 | `mvn compile package` |
| 产物 | `dist/` 静态文件 | `target/*.jar` |

---

## 3. 与 Java 后端项目的集成

### 3.1 目录结构

```text
my-microservice/
├── pom.xml                          # Java 后端
├── src/main/java/...
├── src/main/resources/
│   └── static/                      # ⚠️ 前端构建产物放这里
└── frontend/                        # 前端项目（独立目录）
    ├── package.json
    ├── vite.config.ts
    └── src/
```

### 3.2 Maven 集成（frontend-maven-plugin）

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.0</version>
    <executions>
        <execution>
            <id>install node and npm</id>
            <goals><goal>install-node-and-npm</goal></goals>
            <configuration>
                <nodeVersion>v20.10.0</nodeVersion>
            </configuration>
        </execution>
        <execution>
            <id>npm install</id>
            <goals><goal>npm</goal></goals>
            <configuration>
                <arguments>ci</arguments>         <!-- CI 严格安装 -->
            </configuration>
        </execution>
        <execution>
            <id>npm build</id>
            <goals><goal>npm</goal></goals>
            <configuration>
                <arguments>run build</arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 3.3 Docker 多阶段构建

```dockerfile
# ═══ Stage 1：构建前端 ═══
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build                    # → dist/

# ═══ Stage 2：构建后端 ═══
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY src/ src/
COPY --from=frontend /app/frontend/dist/ src/main/resources/static/
RUN mvn package -DskipTests

# ═══ Stage 3：运行 ═══
FROM eclipse-temurin:21-jre
COPY --from=backend /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 4. CI/CD 中的 npm

```yaml
# GitHub Actions — 前端 CI
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci                    # ⭐ CI 用 ci 不用 install
      - run: npm run lint
      - run: npm test
      - run: npm run build
      - uses: actions/upload-artifact@v4
        with:
          name: dist
          path: dist/
```

```dockerfile
# Dockerfile — 前端项目
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci                              # ⭐ CI 严格安装（利用 Docker 缓存层）
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
```

> 🎯 **后端记住**：`npm ci` 是 CI 环境的标配（严格按 lock、速度快、确定性构建）。Docker 多阶段构建中先 COPY `package*.json` 再 `npm ci`，利用 Docker 缓存层避免每次重复安装依赖。
