# 06 - GiteeGo 流水线实战

> 🎯 Gitee Go 是 Gitee 内置的 CI/CD 工具：YAML 语法与 GitHub Actions 高度兼容（迁移成本低）、国内节点构建稳定、免费赠送时长。从"提交到部署"一条流水线串联 Issue、PR 门禁、制品与多云发布

---

## 目录

1. [Gitee Go 是什么：国产 CI/CD](#1-gitee-go-是什么国产-cicd)
2. [核心概念：流水线/阶段/任务/插件](#2-核心概念流水线阶段任务插件)
3. [YAML 语法：与 Actions 的兼容与差异](#3-yaml-语法与-actions-的兼容与差异)
4. [触发器与运行机制](#4-触发器与运行机制)
5. [制品与部署：多云发布](#5-制品与部署多云发布)
6. [智能构建与成本控制](#6-智能构建与成本控制)
7. [实战：Java 项目完整流水线](#7-实战java-项目完整流水线)
8. [练习](#8-练习)

---

## 1. Gitee Go 是什么：国产 CI/CD

Gitee Go 是 Gitee 推出的持续集成/交付工具，解决"代码提交到应用交付"的自动化：

- **一站式**：串联 Issue 创建、代码提交、PR 审查、部署上线全链路，研发过程可度量可观测
- **国内加速**：依托国内构建节点，依赖下载稳定（原生支持国内镜像源），无需代理——这是对比 GitHub Actions 的核心优势
- **兼容生态**：YAML 语法与 GitHub Actions 高度兼容，Actions 写的流水线稍作修改即可迁移

免费额度（2026-08）：开通赠送 200 分钟永久时长 + 每月额外赠送；macOS 运行器消耗较快。企业版按核分计费（8000 核分/年起）。

## 2. 核心概念：流水线/阶段/任务/插件

```text
流水线（Pipeline）→ 阶段（Stage）→ 任务（Job）→ 步骤（Step）
                ↖ 插件（Plugin）：官方 + 自定义
```

- **流水线**：一次完整的 CI/CD 编排，可视化展示
- **阶段**：串行分组（构建 → 测试 → 部署），阶段内任务可并行
- **任务**：最小调度单元，运行在指定环境的独立容器/机器
- **插件**：官方插件（拉代码/构建 Maven/打包 Docker/部署）与自定义插件（脚本/镜像）

设计原则：**阶段串行、任务并行**——构建与静态扫描并行、测试通过后再部署，失败即停（fail-fast）。

## 3. YAML 语法：与 Actions 的兼容与差异

```yaml
# .workflow/gitee.yml（Gitee Go 配置文件）
version: 1.0.0
stages:
  - name: build
    jobs:
      - name: maven-build
        runs-on: ubuntu-latest
        steps:
          - name: 拉取代码
            uses: checkout@v1
          - name: Maven 构建
            uses: maven@v3
            with:
              command: mvn clean package -DskipTests
          - name: 上传制品
            uses: artifact-upload@v1
            with:
              name: app.jar
              path: target/app.jar
```

与 GitHub Actions 的对应关系：`stages` ≈ `jobs` 的依赖链、`runs-on` 相同、`uses` 同构、`with` 传参一致。差异点：配置文件名（`.workflow/gitee.yml` vs `.github/workflows/*.yml`）、插件生态（Gitee 官方插件 vs Actions 市场）、网络（国内节点）。**迁移成本主要在插件替换**：Actions 第三方 Action 需找 Gitee 等价插件或改自定义脚本。

## 4. 触发器与运行机制

| 触发方式 | 场景 |
|----------|------|
| 代码变更自动触发 | push/PR 自动跑流水线（最常用） |
| PR 门禁 | PR 合并前必须跑完且通过 |
| 手工触发 | 手动执行（部署类流水线） |
| 定时触发 | 每日构建/夜间测试 |

**PR 门禁是与协作体系的结合点**（04 篇）：保护分支配置"合并前要求流水线通过"，GiteeScan/GiteeTest 全绿才能合并——CI 从"建议"变成"强制"。触发范围可配置（仅特定分支/路径），避免文档改动触发全量构建。

## 5. 制品与部署：多云发布

- **制品（Artifact）**：流水线产物，涵盖可执行程序、Docker 镜像、Helm Chart——上传后可在部署阶段消费，也可手动下载
- **多云管理**：一站式管理多云环境，支持一键发布多云、平滑迁移、分级发布和回滚
- **部署阶段**：容器化部署（Docker/K8s）+ 回滚控制

部署设计要点：**构建产物不可变**（同一构建产物部署到任何环境）、**环境隔离**（dev/test/prod 分阶段）、**回滚是发布的一部分**（金丝雀发布、版本回退）。云原生部署细节见 Docker/K8s 体系。一个反直觉的常见问题：**"构建成功"不等于"可部署"**——测试与扫描必须在前置阶段全绿，构建产物才允许进部署阶段；跳过测试直接发布是流水线设计最常见的偷懒，一次回滚事故的代价远超省下的几分钟。

## 6. 智能构建与成本控制

- **智能构建**：根据任务量动态调整资源，可视化观察排队任务与资源占用；基于企业历史流水线数据训练**预扩容模型**，最快秒级执行
- **成本控制**：免费额度有限（200 分钟起），三招省钱——**依赖缓存**（构建时间缩短 50% 以上）、**条件触发**（仅关键分支跑全量，其他分支跑快速检查）、**阶段跳过**（文档改动跳过构建）

时长消耗观察：macOS 运行器（iOS 打包）消耗快，Java 构建中等，纯脚本最省。企业版核分制下同样按这三招优化。

流水线调试三件套：**本地验证**（先在本地跑通命令，再写进 YAML——把"环境问题"挡在流水线外）、**日志看板**（Gitee Go 可视化日志按步骤查看，失败步骤的退出码与输出是主要线索）、**最小化复现**（流水线失败时抽出单条命令在相同环境手工执行——确定是环境差异还是脚本错误）。流水线问题 80% 是环境差异（镜像版本、JDK 版本、依赖源），先把环境固定（锁版本、固定镜像 tag），再谈脚本优化。

流水线设计的一个原则：**部署是受保护的操作**——构建与测试可以在任意 push 上跑，部署必须限制（指定分支/手工触发/审批），否则一次误 push 就触发线上部署。Gitee Go 的分支条件 + 手工触发 + 保护分支（04 篇）组合使用：部署阶段只在 release 分支、必须手工确认，发布回滚也走流水线而不是人工 ssh 改。

## 7. 实战：Java 项目完整流水线

```yaml
version: 1.0.0
stages:
  - name: quality           # 阶段一：质量门禁
    jobs:
      - name: scan
        runs-on: ubuntu-latest
        steps:
          - uses: checkout@v1
          - name: 代码扫描
            uses: gitee-scan@v1        # GiteeScan 安全/规范扫描
      - name: test
        runs-on: ubuntu-latest
        steps:
          - uses: checkout@v1
          - name: 单元测试
            uses: maven@v3
            with:
              command: mvn test
  - name: build             # 阶段二：构建打包
    jobs:
      - name: package
        runs-on: ubuntu-latest
        steps:
          - uses: checkout@v1
          - name: Maven 打包
            uses: maven@v3
            with:
              command: mvn clean package
          - uses: artifact-upload@v1
            with:
              name: app.jar
              path: target/app.jar
  - name: deploy            # 阶段三：部署（手工触发）
    jobs:
      - name: docker-deploy
        runs-on: ubuntu-latest
        steps:
          - name: 构建镜像并推送
            uses: docker-build@v1
            with:
              image: registry.example.com/app
          - name: 部署到 K8s
            uses: k8s-deploy@v1
            with:
              namespace: prod
```

这个三段式（质量 → 构建 → 部署）是团队流水线的标准模板：质量阶段并行扫描+测试、构建阶段产出不可变制品、部署阶段受保护（分支限制 + 手工触发）。

## 8. 练习

1. Gitee Go 的核心概念层级（流水线/阶段/任务/插件）？
2. 与 GitHub Actions 的兼容点与迁移差异？
3. 四种触发方式各用于什么场景？
4. 智能构建的预扩容模型是什么？免费额度怎么省？
5. 画出 Java 项目"质量→构建→部署"三段式流水线

---

> 🎯 **核心要点**：Gitee Go = 国产 CI/CD（国内节点 + Actions 兼容 + 免费额度）。核心概念：流水线→阶段→任务→插件；PR 门禁把 CI 变强制；制品不可变 + 多云发布 + 回滚是部署三原则。省钱三招：依赖缓存（省 50%）、条件触发、阶段跳过。三段式模板：质量 → 构建 → 部署。

**下一模块**：[07-代码质量与安全扫描](07-代码质量与安全扫描.md) / **返回总览**：[00-总览](00-Gitee知识体系总览.md)
