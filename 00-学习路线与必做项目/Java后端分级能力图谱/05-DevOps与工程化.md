# DevOps 与工程化 —— 工程能力体现

> **原则：能写代码是入门，能交付上线是合格，能自动化是优秀。**
> Git + Docker + CI/CD = 现代软件工程的三根支柱。

---

## 一、Git 版本控制（★★★★★）

### 1.1 Git Flow 工作流

```
分支模型：
main / master    → 生产环境代码（只接受 merge，不直接 commit）
develop          → 开发主线
feature/xxx      → 功能分支（从 develop 分出，完成后 merge 回 develop）
release/x.x.x    → 发布分支（从 develop 分出，bugfix 后合入 main + develop）
hotfix/xxx       → 紧急修复（从 main 分出，修复后合入 main + develop）
```

### 1.2 merge vs rebase

```
merge：
  main ← feature
  保留完整分支历史，产生一个 merge commit
  历史图：分叉 → 合并点

rebase：
  feature 的提交"嫁接"到 main 最新提交之后
  历史变成一条直线，更整洁
  ⚠️ 不要 rebase 已推送的公共分支（黄金法则）

场景选型：
- 合并公共分支到当前分支 → rebase（保持历史干净）
- 合并 feature 到公共分支 → merge（保留上下文，方便追溯）
```

### 1.3 高级操作

```
git cherry-pick <commit-hash>
  挑选特定提交应用到当前分支（修了一个 Bug 要同步到多个版本分支）
  
git stash
  暂存当前修改，切换分支处理紧急任务，回来 git stash pop 恢复

git rebase -i HEAD~3
  交互式合并最近 3 个提交（squash/fixup：合并小提交为大提交）

git reflog
  查看所有 HEAD 操作记录（找回误删的分支/commit）

git bisect
  二分查找引入 Bug 的提交（自动化定位问题 commit）
```

### 1.4 团队协作规范

```
Commit Message 规范（Conventional Commits）：
<type>(<scope>): <subject>

类型：
- feat: 新功能
- fix: Bug 修复
- refactor: 重构（不改变功能）
- docs: 文档变更
- test: 测试相关
- chore: 构建/工具变更

示例：
feat(order): 添加订单超时取消功能
fix(auth): 修复 JWT Token 刷新时的并发问题
refactor(user): 提取用户校验逻辑到 UserValidator

Code Review 要点：
- 每个 PR 保持 < 400 行变更
- 关注逻辑正确性 > 代码风格（风格交给 linter）
- 提问代替指责："这里考虑过 XX 情况吗？"
```

---

## 二、Docker 容器化（★★★★☆）

### 2.1 Dockerfile 最佳实践

```dockerfile
# 多阶段构建：编译阶段 + 运行阶段分离
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline    # 利用缓存，只下载依赖
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER 1000                        # 非 root 用户运行
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**优化要点**：
1. 多阶段构建：编译阶段用大镜像，运行阶段用最小镜像（alpine/slim）
2. 分层缓存：先 COPY 不常变的文件（如 pom.xml），再 COPY 源码
3. `.dockerignore`：排除 target/、.git/ 等无关文件
4. 非 root 用户运行：减少安全风险

### 2.2 Docker Compose

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: mydb
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/mydb
      SPRING_REDIS_HOST: redis

volumes:
  mysql_data:
```

### 2.3 Kubernetes 核心概念

```
Pod：最小部署单元（1 个或多个容器共享网络/存储）
Deployment：管理 Pod 副本数 + 滚动更新 + 回滚
Service：为 Pod 提供稳定的网络入口（ClusterIP/NodePort/LoadBalancer）
ConfigMap：非敏感配置的外部化
Secret：敏感信息（密码/Token）的外部化
Ingress：HTTP/HTTPS 路由到 Service

滚动更新：新版本 Pod 逐步替换旧版本，保证服务不中断
  maxSurge=25%：最多额外创建 25% 新 Pod
  maxUnavailable=25%：最多 25% Pod 不可用
```

---

## 三、CI/CD 持续集成（★★★☆☆）

### 3.1 Jenkins Pipeline（声明式）

```groovy
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps { git url: 'https://github.com/xxx/project.git' }
        }
        
        stage('Build') {
            steps { sh 'mvn clean compile' }
        }
        
        stage('Test') {
            steps { sh 'mvn test' }
        }
        
        stage('Static Analysis') {
            steps { sh 'mvn sonar:sonar' }
        }
        
        stage('Package') {
            steps { sh 'mvn package -DskipTests' }
        }
        
        stage('Docker Build & Push') {
            steps {
                sh 'docker build -t registry.example.com/app:${BUILD_NUMBER} .'
                sh 'docker push registry.example.com/app:${BUILD_NUMBER}'
            }
        }
        
        stage('Deploy') {
            steps {
                sh 'kubectl set image deployment/app app=registry.example.com/app:${BUILD_NUMBER}'
            }
        }
    }
    
    post {
        failure { emailext to: 'team@example.com', subject: 'Pipeline Failed' }
    }
}
```

### 3.2 Maven 依赖管理

```
依赖冲突解决：
mvn dependency:tree    查看完整依赖树
<exclusions>           排除传递依赖中的冲突包

版本管理：
<dependencyManagement> 统一声明版本，子模块不写版本号
BOM（Bill of Materials）Spring Boot 的 spring-boot-dependencies

多模块项目：
parent-module/
├── pom.xml（packaging: pom）
├── common-module/
├── service-module/
└── web-module/
```

---

## 四、代码质量

### 4.1 SonarQube 质量门禁

```
默认质量门禁（Quality Gate）：
- 新增代码覆盖率 < 80% → 不通过
- 新增代码重复率 > 3% → 不通过
- 阻断级别的 Bug > 0 → 不通过
- 安全漏洞评级 A 以下 → 不通过

本地检查插件：SonarLint（IDE 内实时提示）
```

### 4.2 单元测试策略

```
测试金字塔：
        /\
       /E2E\        少量端到端测试（关键流程）
      /------\
     /集成测试\      中等数量（API + 数据库）
    /----------\
   /  单元测试  \    大量（每个方法/分支）
  /--------------\

JUnit 5 + Mockito 核心用法：
- @Mock：创建 Mock 对象
- @InjectMocks：将 Mock 注入到被测试对象
- when().thenReturn()：定义 Mock 行为
- verify()：验证 Mock 方法被调用
- assertThrows()：验证异常抛出

Mock 原则：
- 不 Mock 值对象（DTO/Entity）
- 不 Mock 不属于自己的类
- Mock 外部依赖（数据库、RPC、MQ）
```

---

## 面试自查清单

```
□ Git merge vs rebase 的区别 + rebase 黄金法则
□ Docker 镜像分层缓存原理（为什么先 COPY pom.xml）
□ K8s Deployment 滚动更新 maxSurge/maxUnavailable 的含义
□ 多阶段构建的价值（编译镜像 vs 运行镜像大小对比）
□ SonarQube 质量门禁包含哪几项指标
□ 单元测试金字塔各层的比例和关注点
□ Maven dependencyManagement 和 dependencies 的区别
```
