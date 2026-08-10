# 03 - GitLab CI/CD 核心

> 定位：流水线的基础语法——"pipeline 是执行的'流水线'、job 是'工序'、stage 是'工段'——.gitlab-ci.yml 声明一次，每次 push/MR 自动执行；2026 年 rules 取代 only/except，artifacts 与 cache 分工明确"

---

## 📚 目录

1. [核心概念：pipeline / job / stage](#1-核心概念pipeline--job--stage)
2. [第一个 .gitlab-ci.yml](#2-第一个-gitlabciyml)
3. [Artifacts 与 Cache：产物与缓存的分工](#3-artifacts-与-cache产物与缓存的分工)
4. [变量体系](#4-变量体系)
5. [Rules：条件控制（取代 only/except）](#5-rules条件控制取代-onlyexcept)
6. [Include：复用](#6-includereuse复用)
7. [CI/CD 在 MR 中的位置](#7-cicd-在-mr-中的位置)
8. [五个常见坑](#8-五个常见坑)
9. [练习 5 题](#9-练习-5-题)

---

## 1. 核心概念：pipeline / job / stage

**三个概念的类比**：**pipeline（流水线）** = 一次 CI/CD 执行的完整流程（**每次 push/MR 触发一次**）；**stage（工段）** = 流程的阶段（build → test → deploy，**同阶段 job 并行执行**）；**job（工序）** = 流水线的最小执行单元（**一个 job = 一段脚本 + 运行环境**）。**执行规则**：**stage 顺序执行**（test 等 build 全完），**同 stage 的 job 并行执行**（测试多 job 同时跑）——"**pipeline 是纵向的时间线，stage 是横向的并行线**"。**job 的四个必知属性**：`script`（执行的命令）、`image`（运行镜像）、`stage`（所属阶段）、`tags`（路由到哪个 Runner，05 篇）——**最少只要 script + stage 就能跑**。**job 的其他常用属性**：`before_script`（前置脚本——登录/准备）、`after_script`（后置——清理）、`timeout`（job 超时——**防死循环 job 挂死 Runner，必配**）、`when`（on_success/on_failure/always——失败也跑清理）。

## 2. 第一个 .gitlab-ci.yml

```yaml
stages:            # 阶段定义（顺序执行）
  - build
  - test
  - deploy

build-job:         # job 名（自定义）
  stage: build
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn clean package -DskipTests
  artifacts:                        # 构建产物传给后续 job
    paths: [ target/*.jar ]
    expire_in: 1 week

test-job:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test
  rules:                            # 条件：MR 流水线才跑测试
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'

deploy-job:
  stage: deploy
  image: alpine:3
  script:
    - echo "部署到测试环境"
  environment: test                 # 环境标记（06 篇环境分支）
  only:
    - main                          # 老写法（新代码用 rules，第 5 节）
```

**写流水线的三个要点**：其一，**先定义 stages 再写 job**（stage 顺序 = 流水线骨架）；其二，**image 指定运行环境**（每个 job 在独立容器里跑——**Runner 的 Docker executor，05 篇**）；其三，**job 之间用 artifacts 传产物、用 cache 传依赖**（第 3 节）——"**.gitlab-ci.yml 是声明式配置：'跑什么、在哪跑、什么时候跑'三件事说清即可**"。**跑通路径**：push 到 main → GitLab 检测 .gitlab-ci.yml → 创建 pipeline → Runner 拉 job 执行——"**第一次跑通的感觉：改一行代码，全自动验证**"（示例用 Maven 是 Java 团队主流——**换成 npm/pip 同理，script 换成对应命令即可**）。

## 3. Artifacts 与 Cache：产物与缓存的分工

**Artifacts（产物）与 Cache（缓存）是最常混淆的一对**：

| 维度 | Artifacts | Cache |
|------|-----------|-------|
| 目的 | **job 之间的产物传递**（jar/报告） | **依赖加速**（Maven 仓库/node_modules） |
| 生命周期 | 流水线内传递 + 可下载 | 跨流水线复用（按 key 存） |
| 必须性 | 下一个 job 要文件就必配 | 可选优化 |
| 典型用法 | build 产 jar → test 用 | maven 依赖 → 各 job 复用 |

**实践要点**：**artifacts 配 expire_in**（过期自动清理，防仓库膨胀）；**artifacts 可配 `download: false`**（大产物下游按需拉取——"默认自动传，大件按需拉"）；**cache 用 key 绑定锁文件**（`cache:key: $CI_COMMIT_REF_SLUG` 或依赖锁文件哈希——**依赖没变就命中缓存**）：

```yaml
cache:
  key: maven-$CI_COMMIT_REF_SLUG
  paths: [ .m2/repository ]
```

——"**Artifacts 是接力棒（job 间传递），Cache 是工具箱（跨流水线复用）**"。

## 4. 变量体系

**GitLab CI 的变量分三层**：**预定义变量**（`$CI_COMMIT_SHA`/`$CI_PIPELINE_SOURCE`/`$CI_COMMIT_REF_NAME` 等几十个——**不用声明直接用**）；**项目/组变量**（Settings → CI/CD → Variables——**跨流水线配置**：数据库地址/密钥）；**job 内变量**（`variables:` 关键字定义）。**两个关键纪律**：**密钥类变量必须 Masked**（打码防泄露）+ **Protected**（只在保护分支生效——**密钥不暴露给普通 MR 流水线**，07 篇）；**环境差异用变量不用改 YAML**（测试/生产地址通过变量注入）——"**变量是流水线的'配置中心'：密钥走变量，环境走变量，代码不动**"。**常用预定义变量**：`$CI_COMMIT_SHA`（当前提交）、`$CI_COMMIT_BRANCH`（分支）、`$CI_PROJECT_PATH`（项目路径）、`$CI_REGISTRY_IMAGE`（镜像地址，08 篇）——**CI 里拼路径/标签全靠它们**；**变量作用域**：项目变量 > 组变量 > 实例变量——"就近覆盖"；**变量调试**：`echo $CI_COMMIT_SHA` 打日志看值——"CI 变量看不见就打印出来"（**变量名拼错不报错，打印验证是唯一手段**）。

## 5. Rules：条件控制（取代 only/except）

**rules 是 2026 的条件控制标准写法**（only/except 是旧语法，新代码不用）：

```yaml
test-job:
  script: mvn test
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'   # 仅 MR 流水线
    - if: '$CI_COMMIT_BRANCH == "main"'                     # 或 main 分支
    - when: never                                           # 其他情况不跑
```

**rules 与 only/except 的差异**：**rules 支持"多条件 + 顺序匹配"**（从上到下第一条匹配生效）、**可用预定义变量做复杂判断**（流水线来源/分支/变更路径）、**支持 changed paths**（`changes: [src/**]`——只改文档就不跑测试）——"**only/except 是'单条件开关'，rules 是'多条件路由'——新代码一律 rules**"。**rules 的完整条件族**：`if`（变量条件）、`changes`（文件变更）、`exists`（文件存在）、`when`（执行与否）——"**rules 是条件引擎，不是开关**"。**2026 的 CI Expert Agent 生成的就是 rules 风格**（01 篇）。

## 6. Include：复用

**include 是流水线的"复用与分层"机制**——**把公共配置抽到公共文件/公共项目**：

```yaml
# .gitlab-ci.yml 入口文件只做组装
include:
  - local: '/ci/.common.yml'           # 本仓库公共配置
  - project: 'ops/ci-templates'        # 公共项目模板（04 篇 Components）
    file: '/templates/java-build.yml'
    ref: 'v1.2'                        # 锁版本！
```

**include 的四个来源**：local（本仓库）、project（其他项目——**公共模板仓库**）、remote（URL）、template（GitLab 官方模板库）。**两个纪律**：**引用公共模板要锁 ref 版本**（不锁 = 模板更新即全局漂移）；**include 有深度限制**（最多 100 层防循环引用）——"**include 让流水线从'每个仓库复制一份'变成'公共模板 + 本地覆盖'**"（04 篇 Components 是它的演进）。**覆盖顺序**：后 include 覆盖先 include、job 内定义覆盖 include——"**就近优先：公共默认 + 项目覆盖的分层结构**"。

## 7. CI/CD 在 MR 中的位置

**GitLab CI 与 MR 深度绑定——"MR 即流水线"**：**开 MR 自动触发 MR 流水线**（跑测试/扫描）；**流水线结果直接显示在 MR 页面**（红色 ✖/绿色 ✔）；**流水线失败可阻止合并**（`Pipeline must succeed` 合并规则，06 篇）；**JUnit 测试报告/覆盖率直接内嵌 MR**（`artifacts:reports:junit`）。**2026 的 MR-First 三件套**（04 篇详讲）：**merge request pipelines**（MR 触发）、**merged results pipelines**（测试"合并后的结果"，更准）、**workflow rules**（按事件路由）——"**GitLab 的哲学：测试与评审在同一时刻同一处——MR 是质量的关卡，CI 是关卡的守卫**"。**覆盖率门禁**：Cobertura 报告进 MR（覆盖率变化一目了然）——"**覆盖率是 MR 的第二个红绿灯**"；**失败体验**：MR 页面红叉 + 失败 job 日志可直接点开看——"排障从 MR 页面开始"。

## 8. 五个常见坑

- **坑一**：only/except 继续用——**新代码用 rules；only/except 条件表达能力不够**（第 5 节）；
- **坑二**：job 之间用文件系统传东西不配 artifacts——**Docker executor 每个 job 独立容器；必须 artifacts**（第 3 节）；
- **坑三**：密钥变量不 Masked——**流水线日志泄露；Masked + Protected**（第 4 节）；
- **坑四**：cache key 不绑定依赖锁文件——**依赖没变也重新下载；key 绑定锁文件哈希**（第 3 节）；
- **坑五**：include 不锁版本——**模板更新全局漂移；ref 锁版本**（第 6 节）；
- **坑六**：job 没有 timeout——**死循环 job 占住 Runner；timeout 必配**（第 1 节）。

## 9. 练习 5 题

1. pipeline/job/stage 的类比与执行规则？
2. 写出三阶段（build/test/deploy）最小流水线？
3. Artifacts 与 Cache 的分工？各自要点？
4. 变量三层与密钥纪律？
5. rules 与 only/except 的差异？MR 与 CI 的绑定关系？

> 🎯 **核心要点**：CI/CD 核心 = **"stages 骨架 + job 工序 + artifacts 接力 + cache 复用 + rules 路由 + include 分层"**——"MR 即流水线"是 GitLab 的哲学；**2026 新代码一律 rules、密钥必 Masked、include 必锁版本**。

---

**下一模块**：[04-高级CI-CD.md](04-高级CI-CD.md) / **返回总览**：[00-GitLab总览.md](00-GitLab总览.md)
