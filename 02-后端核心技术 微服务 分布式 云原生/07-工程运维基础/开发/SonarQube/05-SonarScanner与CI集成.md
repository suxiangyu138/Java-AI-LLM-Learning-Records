# 05-SonarScanner与CI集成
> 定位：Scanner 是分析入口、CI 是门禁执行者——CLI/Maven/Gradle 三种形态、分析异步模型、流水线门禁三件套；Community Build 没有 PR 装饰，CI 脚本补偿是标配动作。

## 📚 目录
1. [Scanner 家族](#1-scanner-家族)
2. [sonar-project.properties](#2-sonar-projectproperties)
3. [分析的异步模型](#3-分析的异步模型)
4. [Jenkins 集成](#4-jenkins-集成)
5. [GitLab CI 与 GitHub Actions](#5-gitlab-ci-与-github-actions)
6. [Community Build 的 PR 补偿](#6-community-build-的-pr-补偿)
7. [常见坑](#7-常见坑)
8. [练习](#8-练习)

## 1. Scanner 家族

三种分析器形态，按项目类型选：

**SonarScanner CLI**——通用分析器（任何语言）：下载 `sonar-scanner-cli` 解压即用，配 `sonar-project.properties` + 命令参数——**适合非构建系统项目与脚本场景**。**Maven 插件**——`mvn sonar:sonar`：自动读取 Maven 项目结构（模块、源码目录、依赖）——**Java/Maven 项目首选**（零配置文件，`mvn clean verify sonar:sonar` 一条命令带测试与覆盖率）。**Gradle 插件**——`gradle sonar`（sonarqube 插件）——Gradle 项目同 Maven 的体验。

**Scanner 与服务器版本配套**：老 Scanner 连新服务器可能协议不兼容——**升级服务器时 Scanner 同步升级**（09 篇升级纪律）。**Token 认证**：所有 Scanner 用 `-Dsonar.token=<token>`（02 篇——Token 比密码安全，CI 里放环境变量/凭证库）。

## 2. sonar-project.properties

CLI 场景的配置中心（Maven/Gradle 场景自动生成，也可覆盖）：

```properties
sonar.projectKey=my-app            # 项目唯一键（服务器端创建时定义）
sonar.projectName=My Application   # 显示名
sonar.projectVersion=2.1.0         # 版本（可传 build 号）
sonar.sources=src/main/java        # 源码目录（逗号分隔）
sonar.tests=src/test/java          # 测试目录
sonar.java.binaries=target/classes # Java 编译产物（必配，02 篇坑四）
sonar.sourceEncoding=UTF-8         # 编码（中文项目必须）
sonar.exclusions=**/generated/**   # 排除生成代码
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml  # 覆盖率报告
```

要点：**`projectKey` 必须与服务器端创建的项目一致**（不一致会创建新项目）；**`sonar.java.binaries` 是 Java 的硬性要求**；**排除生成代码**（`**/generated/**`、`**/target/**`——生成的代码全是「问题」）；**编码 UTF-8**（默认平台编码会导致中文乱码或规则误判）。完整参数表见 [[../代码质量/02-SonarQube代码质量平台实战|代码质量体系实战篇]] 第 11 章。

## 3. 分析的异步模型

**分析是异步的**——理解这个模型，CI 集成才不会踩坑：

```text
Scanner 提交 → 服务器入队（Compute Engine 任务）→ 分析执行（1-2 分钟）→ 结果入库 → 门禁判定
```

Scanner 命令**只负责提交**——提交完命令可能就退出了（`sonar-scanner` 默认等待报告生成，但 CI 的「分析完成」要以**服务器 API** 为准）。**CI 里的标准姿势**：扫描后**轮询 `/api/qualitygates/project_status`**（或平台提供的等待工具），拿到门禁结果再决定流水线成败——**「扫完立即读门禁」会读到「还没分析完」的空状态**——这是 CI 集成最常见的坑。

## 4. Jenkins 集成

[[../运维/Jenkins/05-构建工具集成|Jenkins 体系]] 的 SonarQube 集成是经典组合：

```groovy
// Jenkinsfile（声明式）
stage('SonarQube Analysis') {
    steps {
        withSonarQubeEnv('SonarQube') {           // Jenkins 插件：注入服务器地址与 Token
            sh 'mvn clean verify sonar:sonar'
        }
    }
}
stage('Quality Gate') {
    steps {
        timeout(time: 5, unit: 'MINUTES') {
            waitForQualityGate abortPipeline: true  // 等待门禁结果，失败则流水线失败
        }
    }
}
```

要点：**`withSonarQubeEnv` 注入连接配置**（Jenkins 全局配置里配 SonarQube 服务器 + Token——凭证走 Jenkins 凭证库，不写死在 Jenkinsfile）；**`waitForQualityGate` 是门禁执行器**——轮询门禁结果 + `abortPipeline: true` 失败即停；**timeout 包住等待**（分析卡住时流水线不无限挂起）。「分析 + 等待门禁」两个 stage 的拆分让失败定位清晰。

## 5. GitLab CI 与 GitHub Actions

**GitLab CI**（配合 [[../运维/CI%20CD灰度发布/00-CI-CD与灰度发布总览|CI/CD 体系]]）：

```yaml
sonarqube-check:
  stage: test
  image: sonarsource/sonar-scanner-cli:latest
  variables:
    SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
  script:
    - sonar-scanner -Dsonar.token=$SONAR_TOKEN \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.projectKey=$SONAR_PROJECT_KEY
  after_script:
    - "curl -s --fail -u $SONAR_TOKEN: $SONAR_HOST_URL/api/qualitygates/project_status?projectKey=$SONAR_PROJECT_KEY | jq -e '.projectStatus.status == \"OK\"'"  # 门禁检查
  allow_failure: false
```

**GitHub Actions**：`SonarSource/sonarqube-scan-action@v5`（扫描）+ `SonarSource/sonarqube-quality-gate-action@v3`（门禁——自带等待重试）——官方 action 组合，配置最省。**通用要点**：**Token 放 CI 变量/Secret**（不写进仓库）；**门禁检查用官方 action 或 API 轮询**；**`allow_failure: false` 让门禁失败阻断发布**。

## 6. Community Build 的 PR 补偿

Community Build 无 PR 装饰（01 篇局限）——**PR 级反馈用 CI 脚本补偿**，两个姿势：

**姿势一：PR 注释**——流水线里分析后，把门禁结果写到 PR 评论（GitHub 的 `gh pr comment` 或 GitLab 的 discussion API）：

```bash
STATUS=$(curl -s "$SONAR_HOST_URL/api/qualitygates/project_status?projectKey=$KEY" | jq -r '.projectStatus.status')
gh pr comment $PR_NUMBER --body "SonarQube 门禁：$STATUS（$SONAR_HOST_URL/dashboard?id=$KEY）" --repo $CI_REPO
```

**姿势二：问题清单导出**——门禁失败时把「新代码的问题列表」打到 CI 日志/评论（`/api/issues/search?projectKey=...&sinceLeakPeriod=true`）——开发者不用开 Dashboard 就知道改哪里。**补偿的边界**：PR 注释不是「PR 内联装饰」——体验差一档，但「合入前知道门禁红没红」的目标达成——**这是 Community Build 可接受的工程妥协**（升级 Developer 版才有真 PR 体验）。

**CI 集成的完整姿势清单**（五种主流程对照）：**Jenkins**——`withSonarQubeEnv` + `waitForQualityGate abortPipeline: true`（第 4 节）；**GitLab CI**——sonar-scanner-cli 镜像 + 官方 quality-gate-check（或自写 curl 轮询，第 5 节）；**GitHub Actions**——`sonarqube-scan-action` + `sonarqube-quality-gate-action`（官方 action 自带等待）；**Azure DevOps**——SonarQube extension（Prepare/Execute/Check Quality Gate 三任务）；**命令行手动**——`sonar-scanner` + 脚本轮询 `project_status` API。**选型要点**：CI 平台有自己的官方集成路径就优先用（维护省心）；**「等待门禁」是共同刚需**（异步模型——第 3 节）；**多 CI 并存**（Jenkins 存量 + GitHub Actions 新仓库）时 Token 与项目键按环境隔离（09 篇权限）。

**API 驱动的自动化**（运维与 CI 的进阶能力，配合 [[../代码质量/02-SonarQube代码质量平台实战|实战篇]] 的 API 章节）：**`/api/qualitygates/project_status`**（门禁查询——CI 轮询的核心，本文多处使用）；**`/api/ce/activity`**（分析任务状态——排障）；**`/api/issues/search`**（问题导出——PR 注释的数据源、批量治理的清单源）；**`/api/projects/search`**（项目清单——治理报表）。**Token 认证调用**（`-u $TOKEN:`）——API 是「把 SonarQube 纳入自动化体系」的通道：门禁看板、质量报表、问题工单对接都从这里长出来——「平台不只是 Web UI，是一套 API」。

## 7. 常见坑

**坑一：扫完立即读门禁**。异步分析未完成——读到空状态——轮询等待（官方 action / waitForQualityGate）。

**坑二：Token 写进仓库**。泄漏即用——CI 变量/凭证库 + 定期轮换。

**坑三：projectKey 不一致**。CI 里配错键 → 每次扫描建新项目——从服务器端复制键。

**坑四：Java 无编译产物**。`sonar.java.binaries` 缺失 → 漏检——先 compile 再扫。

**坑五：allow_failure 永久开**。门禁失败流水线照过——门禁形同虚设——显式豁免而非默认放行。

**坑六：Scanner 版本滞后**。服务器升级后老 scanner 报错——配套升级。

**坑七：PR 补偿不做**。Community 无 PR 装饰就裸奔——CI 注释补偿。

**坑八：CI 里硬编码服务器地址**。环境切换（测试/生产 SonarQube）要改流水线——服务器地址与 Token 走 CI 变量（`$SONAR_HOST_URL`/`$SONAR_TOKEN`），按环境注入——「CI 配置即代码」的变量化纪律。

**坑九：扫描与构建分离在不同 stage 且无产物缓存**。`mvn sonar:sonar` 依赖 `target/classes`——若扫描 stage 没跑构建——编译产物缺失——扫描 stage 里先构建再扫（或声明依赖上一步 artifact）。

**CI 集成的排障顺序**：流水线门禁红/失败——**先分「三层」排查**：第一层「扫描层」（`ANALYSIS SUCCESS` 了吗——失败先看 Scanner 输出——Token/路径/编译产物是前三原因）；第二层「分析层」（服务器 CE 任务成功了吗——`/api/ce/activity` 看任务状态——源码解析错误、内存不足）；第三层「门禁层」（任务成功但门禁红——看 `project_status` 的失败条件——是真问题还是配置过严）。**「扫描成功 ≠ 门禁通过」**——三层独立排查，别在第三层的问题上重试第一层。

## 8. 练习

1. 三种 Scanner 的适用场景？
2. sonar-project.properties 的五个关键配置？
3. 分析异步模型对 CI 集成意味着什么？
4. Jenkins 的「分析 + 等待门禁」两 stage 怎么写？
5. Community Build 的 PR 补偿两种姿势？

> 🎯 **核心要点**：Maven 项目 `mvn sonar:sonar`、CLI 通用、Gradle 插件；`sonar.java.binaries` 与排除生成代码必配；分析异步——轮询门禁 API 再判定；Jenkins `waitForQualityGate`、GitHub 官方 action；Community 无 PR 装饰用 CI 注释补偿。

---

**下一模块**：[06-SonarLint与IDE集成](06-SonarLint与IDE集成.md)｜**返回总览**：[00-SonarQube总览](00-SonarQube总览.md)
