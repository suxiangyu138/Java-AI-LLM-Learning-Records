# 05 - Runner 体系

> 定位：流水线的"执行工人"——"job 声明要什么（image/tags），Runner 提供什么环境——Runner 的类型、executor、tags 路由、并发与资源是 CI 基础设施的四件事；2026 年 Docker executor 是默认，K8s executor 是规模化答案"

---

## 📚 目录

1. [Runner 是什么](#1-runner-是什么)
2. [三种类型与 Tags 路由](#2-三种类型与-tags-路由)
3. [Executor：执行环境家族](#3-executor执行环境家族)
4. [Docker Executor 深潜](#4-docker-executor-深潜)
5. [Kubernetes Executor](#5-kubernetes-executor)
6. [注册与并发配置](#6-注册与并发配置)
7. [GitOps 与部署集成](#7-gitops-与部署集成)
8. [五个常见坑](#8-五个常见坑)
9. [练习 5 题](#9-练习-5-题)

---

## 1. Runner 是什么

**Runner 是执行 CI job 的代理程序**——"**GitLab 服务器负责'编排'（解析 .gitlab-ci.yml、分配 job），Runner 负责'干活'（拉取代码、跑脚本、回传结果）**"。**Runner 与 GitLab 的通信**：**注册时拿 token 建立连接**，然后**轮询取 job**（Runner 主动拉取，GitLab 不需要能访问 Runner——**内网 Runner 也能用**）。**Runner 的形态**：**可装在任意机器**（个人电脑/服务器/容器/K8s pod）——"**Runner 是分布式工人：GitLab 是工厂，Runner 是生产线**"。**版本配套**：Runner 与 GitLab 有版本兼容矩阵——**Runner 版本别太旧（18.x Runner 配 19.x GitLab 可能不兼容）；定期升级 Runner**；**数量规划**：按并发需求算——"10 个并发 job = 至少 10 个并发配额"。

## 2. 三种类型与 Tags 路由

**Runner 的三个作用域**：**Shared（共享）**——实例级所有项目可用（GitLab.com 的托管 Runner）；**Group（组级）**——组内项目可用；**Specific（项目级）**——单项目专用（**完全控制，高配/特殊环境**）。**Tags 路由是 Runner 的"标签系统"**：**Runner 注册时打 tags**（`docker`/`gpu`/`macos`/`large-memory`），**job 声明 tags 匹配**——"**没有 tags 的 job 可能被任何 Runner 捡走，有 tags 的 job 精确路由**"：

```yaml
gpu-test:
  tags: [gpu]          # 只被带 gpu 标签的 Runner 执行
  script: run gpu tests
```

**Tags 的三个纪律**：**tags 是"能力声明"不是"命名"**（`gpu` 表示有能力，`my-runner` 是命名）；**job 不写 tags 默认任何 Runner**（**特定环境 job 必须写 tags**）；**Runner 的 tag 列表与并发是容量规划的输入**（08 篇 Runner 容量）。**tags 与 image 的关系**：**tags 选 Runner（硬件/环境），image 选容器（软件环境）**——"tags 管在哪跑，image 管用什么跑"；**匹配顺序**：tags 精确匹配优先——"**想要'只有高配 Runner 跑大构建'，tags 是唯一手段**"；**在线状态**：GitLab 管理页面看 Runner 心跳——"**注册了但没心跳 = 白注册**"。

## 3. Executor：执行环境家族

**Executor 决定"job 在什么环境里跑"**，四个主流：

| Executor | 环境 | 适用 |
|----------|------|------|
| Shell | Runner 机器直接执行 | 简单/开发机 |
| Docker | 每 job 一个容器（image 指定） | ⭐ 默认（隔离 + 可复现） |
| Kubernetes | 每 job 一个 pod | 规模化（K8s 集群已有） |
| SSH | 连远程机器执行 | 远程部署 |

**Shell executor 的代价**：直接跑在宿主机——**job 间不隔离、宿主机被污染**；只适合开发机/简单环境——"**隔离是 CI 的默认要求，Shell 是例外**"。

**选型主线**：**"默认 Docker（image 声明环境、job 间隔离）、规模化 Kubernetes（pod 动态扩缩）、特殊场景 Shell/SSH"**——**Docker executor 的三大优势**：**环境可复现**（image 锁版本 = 构建环境一致）、**job 隔离**（并行 job 互不污染）、**无状态**（每个 job 全新容器——**这也意味着 job 间文件传递必须 artifacts，03 篇**）。**executor 与 job 的匹配**：job 的 image 只在 Docker/K8s executor 下有意义——**Shell executor 忽略 image，环境靠宿主机**。

## 4. Docker Executor 深潜

```bash
# 注册一个 Docker executor Runner
gitlab-runner register \
  --url https://gitlab.example.com \
  --token <registration-token> \
  --executor docker \
  --docker-image alpine:3 \
  --tag-list docker,linux \
  --docker-volumes /var/run/docker.sock:/var/run/docker.sock
```

**Docker executor 的三个进阶**：**Docker-in-Docker**（job 里要 build/push 镜像——**挂载 docker.sock（DinD 或 socket 直通）**——"**CI 里构建镜像 = CI 里跑 Docker**"）；**services 关键字**（**job 的辅助服务**：`services: [postgres:16]` 起测试数据库——**集成测试的标配**）；**私有镜像拉取**（Runner 配置 registry 凭据——**GitLab Container Registry 自动关联**，08 篇——"**私有镜像进 CI，凭据要么自动要么显式配**"）。**资源限制**：**concurrent + limit** 控制并发（第 6 节）——"**Docker executor 是 90% 团队的默认：image 锁版本、services 起依赖、artifacts 传产物**"。

## 5. Kubernetes Executor

**K8s executor：每个 job 动态创建 pod 运行**——**规模化 CI 的标准答案**：**不用常驻 Runner 集群**（job 来了起 pod，跑完销毁——**按需计费资源**）；**K8s 集群已有**（复用基础设施）；**配合 GitLab Agent（KAS）**——**部署到 K8s 的 KUBECONFIG 作用域化**（不用把集群 token 存 CI 变量——**安全性的提升**，07 篇）。**部署姿势**：**Helm chart 安装 GitLab Runner**（`helm install gitlab-runner gitlab/gitlab-runner` + values 配 executor: kubernetes）——"**K8s executor 是'Runner 即服务'：CI 基础设施跟着集群走**"（云原生栈的 CI 答案，Docker 体系交叉：`../../11-云原生/Docker/`）。**与常驻 Runner 的对比**：K8s 按需起 pod（资源省）、常驻 Runner 复用镜像层（启动快）——"规模化用 K8s，速度敏感用常驻"（**KAS = GitLab Agent Server，是 K8s 集成的桥梁组件**）。

## 6. 注册与并发配置

**Runner 配置的核心在 config.toml**（`gitlab-runner` 的配置文件），三个关键参数：

```toml
concurrent = 10              # 本 Runner 全局最大并发 job 数
[[runners]]
  limit = 5                  # 本 Runner 的并发上限（多 Runner 时分配）
  executor = "docker"
  [runners.docker]
    image = "alpine:3"
    volumes = ["/cache"]     # 共享缓存卷（cache 的落点，03 篇）
```

**并发的三个认知**：**concurrent 是"这台机器同时跑几个 job"**——**不是越大越好**（每个 job 吃 CPU/内存——**并发 10 个 Maven 构建 = 机器卡死**）；**共享缓存卷**（Docker executor 的 /cache 卷让 cache 跨 job 生效——**不配 = cache 白配**）；**Runner 健康**（`gitlab-runner verify` 检查注册状态、`gitlab-runner status` 看运行）——"**并发是容量决策：先看机器配置，再定 concurrent 数字**"（09 篇容量规划）。**注册流程**：`gitlab-runner register` 交互式完成（url/token/executor/tags）——"**注册一次，长期服务**"；**版本升级**：`gitlab-runner update` 或重装——与 GitLab 版本配套（第 1 节）。**注册 token 两种**：项目级（注册特定 Runner）与实例级（注册 shared Runner）——**token 是 Runner 的身份证，泄露可注册恶意 Runner 窃取 job 上下文——定期轮换**；**多 Runner 共享缓存**：配外部缓存（S3/MinIO）的 `[runners.cache]`——"缓存是共享的，不是每台 Runner 各存各的"。

## 7. GitOps 与部署集成

**Runner 与部署的三个结合姿势**：**直接部署**——job 里 `kubectl apply`/SSH 部署（简单但**权限大、审计弱**）；**GitOps 模式（2026 推荐）**——**CI 只更新部署仓库的镜像版本（image bump）**，**Flux/ArgoCD 监听部署仓库自动同步到集群**——"**CI 写 Git，GitOps 管集群——审计链完整（谁改了镜像版本一查 Git 历史）**"（CI/CD 灰度发布体系交叉：`../../07-工程运维基础/运维/CI CD灰度发布/`）；**Review Apps**——**每个 MR 自动部署一个预览环境**（`environment: review/...`，06 篇环境，**自动清理过期环境**）——"**MR 里点开链接看效果**是 Review 体验的巅峰。**2026 新增**：**macOS 托管 Runner（M2 Pro 6 核 16GB）**——iOS/macOS 构建的托管答案；GitLab.com 托管 Runner 免费额度（共享）——"SaaS 团队先白拿托管 Runner，再按需自建"；**调试**：`gitlab-runner run --debug` 看 job 分配——"排障最后一招"。

## 8. 五个常见坑

- **坑一**：job 不写 tags 依赖特定 Runner——**被不合适的 Runner 捡走（没 Docker 环境跑崩）；特定环境必写 tags**（第 2 节）；
- **坑二**：Docker executor 不挂 /cache 卷——**cache 配置了也不生效；volumes 配 /cache**（第 6 节）；
- **坑三**：CI 里 build 镜像没配 Docker——**DinD/socket 直通二选一**（第 4 节）；
- **坑四**：concurrent 拍脑袋——**并发 10 个构建把机器打爆；按机器配置定**（第 6 节）；
- **坑五**：部署 job 直接 kubectl apply 集群权限——**审计缺失；GitOps 模式（CI 改仓库，Flux 同步）**（第 7 节）。

## 9. 练习 5 题

1. Runner 与 GitLab 的职责分工？轮询模型的意义？
2. 三种类型与 tags 路由？tags 是能力声明怎么理解？
3. 四种 executor 的选型？Docker 的三大优势？
4. DinD 与 services 是什么？K8s executor 的价值？
5. config.toml 三个关键参数？GitOps 部署模式？

> 🎯 **核心要点**：Runner = **"GitLab 编排、Runner 干活"**——"**默认 Docker executor（隔离 + 可复现）、规模化 K8s executor、tags 精确路由、concurrent 按容量定**"；**GitOps 模式（CI 改仓库、Flux 同步）是 2026 部署审计链的标准答案**。

---

**下一模块**：[06-分支策略与MR工作流.md](06-分支策略与MR工作流.md) / **返回总览**：[00-GitLab总览.md](00-GitLab总览.md)
