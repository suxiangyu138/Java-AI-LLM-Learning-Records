# 04 - 高级 CI/CD

> 定位：流水线的编排艺术——"needs 让 DAG 并行（不等同阶段全完）、parallel matrix 让矩阵测试一键展开、trigger 让父子流水线分层、CI/CD Components 让模板版本化——2026 年流水线从'直线'走向'编排'"

---

## 📚 目录

1. [Needs：DAG 并行依赖](#1-needsdag-并行依赖)
2. [Parallel：并行与矩阵](#2-parallel并行与矩阵)
3. [Trigger：父子流水线](#3-trigger父子流水线)
4. [Scheduled Pipelines：定时流水线](#4-scheduled-pipelines定时流水线)
5. [Workflow Rules：MR-First 三件套](#5-workflow-rulesmr-first-三件套)
6. [CI/CD Components：版本化模板](#6-cicd-components版本化模板)
7. [动态生成流水线](#7-动态生成流水线)
8. [五个常见坑](#8-五个常见坑)
9. [练习 5 题](#9-练习-5-题)

---

## 1. Needs：DAG 并行依赖

**默认的 stage 模型是"顺序流水线"**：test 必须等 build **全部** job 完成——**build 有 3 个 job 但 test 只依赖其中 1 个，也要白等另外 2 个**。**needs 关键字打破这个限制**：**job 声明"我只等谁"**——依赖完成立即启动，不等同阶段全完——**流水线从"线性流水线"变成"DAG（有向无环图）"**：

```yaml
test-api:
  stage: test
  needs: [build-api]        # 只等 build-api 完成
  script: run api tests

test-web:
  stage: test
  needs: [build-web]
  script: run web tests
# build-api 完成 → test-api 立即跑；build-web 完成 → test-web 立即跑（并行）
```

**needs 的三个要点**：**needs: [] 表示"立即开始"**（不依赖任何 job——**资源准备类 job 立刻跑**）；**跨 stage 的 needs 允许**（test 直接依赖 build 的产物）；**needs 的 job 名必须精确**（**写错名字就是断链，流水线直接报错**）——"**needs 是流水线并行度的第一杠杆：大流水线从'分阶段排队'变成'按依赖并行'**"（墙钟时间大幅缩短）。

## 2. Parallel：并行与矩阵

**parallel 解决"同一 job 要跑多份"**：**parallel: 4**——同一 job 并行 4 份（**测试分片**：`CI_NODE_INDEX`/`CI_NODE_TOTAL` 环境变量告诉每份"我是第几片"——**测试用例按片拆分**）：

```yaml
test-suite:
  stage: test
  parallel: 4                       # 4 份并行
  script: run tests $CI_NODE_INDEX  # 每份跑 1/4 测试
```

**parallel: matrix**——**矩阵展开**（多维度组合自动生成 job）：跨版本/跨浏览器测试（**笛卡尔积**）：

```yaml
test-java:
  parallel:
    matrix:
      - JDK_VERSION: [17, 21]
        FRAMEWORK: [spring-boot-3, spring-boot-4]
# 自动生成 2×2=4 个 job：17+spring3 / 17+spring4 / 21+spring3 / 21+spring4
```

——"**parallel 管'同构分片'，matrix 管'异构组合'——测试矩阵一页配置全展开**"。**矩阵的产物数量**：job 数 = 各维度乘积——**维度别太多（5×5=25 个 job 资源爆炸）；用 rules 限制矩阵子集**。

## 3. Trigger：父子流水线

**trigger 让流水线分层**——**父流水线触发子流水线**（跨项目/同项目多组件）：

```yaml
# 父流水线：组装阶段
trigger-service-a:
  stage: deploy
  trigger:
    project: backend/service-a
    branch: main

# 子流水线结果聚合到父流水线（子失败 → 父失败）
```

**父子流水线的价值**：**复杂系统的流水线分层**——**上层"编排"（先 A 后 B）、下层"自管"（每个服务自己的流水线）**——"**父流水线管顺序，子流水线管细节**"；**跨项目触发**（前端项目触发后端部署）是微服务 CI 的常见姿势。**注意**：trigger 触发的是**子项目的流水线**（在子项目里有自己的 Runner/变量上下文）；**传参**：子流水线可接收 `variables:` 传值——"父传上下文，子管细节"。

## 4. Scheduled Pipelines：定时流水线

**定时流水线（Schedule）解决"周期性任务"**：**夜间构建、每日数据刷新、定期回归**——Settings → CI/CD → Schedules 配置 cron 表达式（或 .gitlab-ci.yml 的 `rules: - if: '$CI_PIPELINE_SOURCE == "schedule"'`）。**三个要点**：其一，**定时触发的 job 用 rules 区分**（`schedule` 来源的流水线只跑特定 job——**别让定时任务触发部署 job**）；其二，**定时任务与测试隔离**（夜间构建跑全量回归，MR 流水线跑增量——**分层省钱**）；其三，**定时流水线也占 Runner 资源**（**排期错峰**；cron 按 UTC 配置，注意与业务时区换算）——"**Schedule 是流水线的'时钟'：周期任务进平台，别放服务器 cron**"。

## 5. Workflow Rules：MR-First 三件套

**GitLab 的 MR-First 交付哲学（03 篇第 7 节）的完整实现**——三件套：

**① merge request pipelines**——**开 MR 触发流水线**（`workflow:rules: - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'`——**MR 里跑测试，省得 push 一次跑一次**）；**② merged results pipelines**——**测试"合并后的结果"**（临时合并 commit 跑测试——**比测源分支更准**：能发现"分支与 main 合并后才有"的冲突问题，代价是多一次合并计算——**大仓库可用 MR 流水线 + 主分支定时全量回归的折中**）；**③ workflow rules**——**流水线级的路由**（`workflow: rules:` 控制"什么时候创建流水线"——MR 事件/主分支推送/定时任务各走各的路）。

**实践组合**：**MR 流水线跑测试 + 扫描**（`merge_request_event`）、**main 推送跑部署**（`$CI_COMMIT_BRANCH == "main"`）、**Schedule 跑回归**——"**workflow rules 是流水线的总路由：什么事件跑什么活，一条规则表说清**"；**workflow rules 与 job rules 的分工**：workflow 管"建不建流水线"，job rules 管"跑不跑这个 job"——**两层路由**。

## 6. CI/CD Components：版本化模板

**CI/CD Components 是 2026 年 include 的演进**——**把可复用的流水线逻辑发布成"组件"**（版本化、带文档、可发现）：**发布到 CI/CD Catalog**（项目内的组件目录），**消费方一行 include**：

```yaml
include:
  - component: gitlab.com/ops/ci-components/java-build@1.2
    # 组件 + 版本（@1.2）——版本化引用，模板更新不波及
```

**组件的三个价值**：**版本化**（@tag 引用，升级可控）；**可发现**（Catalog 浏览器找现成组件——**不用从零写**）；**去复制粘贴**（团队统一的构建/部署逻辑一处维护）——"**include 是'复制公共文件'，Components 是'引用公共库'——2026 年模板工程化的标准答案**"；**19.0 的 Components Analytics**：组织级看板看组件使用与版本分布（01 篇）。**选型**：简单复用 include 够用；**需要版本管理与分发面广用 Components**——"include 是复制，Components 是引用"；**组件可定义 inputs 传参**——"组件不是黑盒，参数化复用"。

## 7. 动态生成流水线

**动态生成（Dynamic Pipelines）让流水线"程序化生成"**——**脚本在 pipeline 运行前生成 .gitlab-ci.yml**（按环境/服务动态展开）：

```yaml
generate-config:
  stage: build
  script: ./gen-pipeline.sh > generated.yml    # 脚本生成配置
  artifacts: { paths: [generated.yml] }

deploy-all:
  stage: deploy
  trigger:
    include: generated.yml                     # 用生成的配置触发子流水线
```

**适用场景**：**多环境部署**（几十个环境的配置由模板生成——**一个脚本生成 N 个环境流水线**）、**动态服务列表**（按仓库目录动态生成各服务 job）。**代价**：**可读性下降**（流水线不再是静态 YAML，排障要看生成逻辑——**生成逻辑留注释与日志**）——"**动态生成是'配置即代码'的极致：静态 YAML 管 90% 场景，动态留给'配置量大到手写会疯'的场景**"。

## 8. 五个常见坑

- **坑一**：大流水线不用 needs——**test 白等无关 build；needs 是第一并行杠杆**（第 1 节）；
- **坑二**：parallel 分片但测试不按片拆分——**4 份并行跑同样的测试 = 4 倍浪费；用 CI_NODE_INDEX 拆分**（第 2 节）；
- **坑三**：trigger 子流水线不锁分支——**子项目 main 变动即触发；trigger 指定 branch**（第 3 节）；
- **坑四**：定时流水线不设 rules——**定时触发把部署 job 也跑了；schedule 来源限定 job**（第 4 节）；
- **坑五**：Components 不锁版本——**组件升级全局漂移；@版本号引用**（第 6 节）。

## 9. 练习 5 题

1. needs 解决什么问题？needs: [] 的语义？
2. parallel 与 matrix 的差异？分片测试怎么做？
3. 父子流水线的价值？trigger 的注意点？
4. MR-First 三件套是什么？各自解决什么？
5. Components 与 include 的差异？动态生成的适用场景？

> 🎯 **核心要点**：高级 CI/CD = **"needs 并行化（DAG）+ parallel/matrix 展开 + trigger 分层 + Schedule 定时 + workflow rules 路由 + Components 版本化"**——"流水线从'直线'走向'编排'，2026 的模板工程化答案是 Components"。

---

**下一模块**：[05-Runner体系.md](05-Runner体系.md) / **返回总览**：[00-GitLab总览.md](00-GitLab总览.md)
