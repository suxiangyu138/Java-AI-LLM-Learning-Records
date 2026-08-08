# Jupyter 与 Code-Interpreter：数据类 Agent 的沙箱架构

> 两大参考系：ChatGPT Code Interpreter 的"gVisor + Jupyter kernel"生产架构（逆向实测）；JupyterHub 多租户的 kernel 级隔离教训（含 CVE-2026-42266）。

## 1. ChatGPT Code Interpreter 架构解剖（逆向实测）

| 组件 | 实现 |
|---|---|
| 执行载体 | **gVisor 沙箱化 Linux 容器 + Jupyter kernel**，跑在 K8s（Azure 基础设施） |
| 隔离粒度 | **每次代码执行请求创建全新 gVisor 实例**——环境纯净，请求间零污染 |
| 执行服务 | `user_machine` 服务处理执行请求与结果返回（supervisor + nginx 同栈） |
| 选型原因 | gVisor 无需 `/dev/kvm`，可跑在"本身是 VM 的 K8s 节点"上（Firecracker 做不到，见 [03](03-容器与gVisor.md)） |

### 网络策略：严格断网 + 内网代理

- 直连出网（ping/wget/apt-get）全部失败——**"fully offline" 严格策略**
- 装包走**内部代理**（`applied-caas-gateway1.internal`）：PyPI/npm/Go/Maven/Gradle/Cargo/容器仓库全覆盖
- `container.download` 工具取 URL 需过 `web.run` 过滤器：只放行"用户直接输入的链接"或"搜索结果链接"——**不可被提示注入污染的来源**

### 资源限制（逆向实测值）

```text
PROCESS_MEMORY_LIMIT: 4GB    # 内存上限（~4GB 报告值）
cpu_shares: 1024             # CPU 份额（共享+节流）
max_execution_time: 30s      # 单次执行超时
network_access: false        # 断网
rootfs: 只读                 # 只读文件系统
```

### 2026 演进：隐藏的容器升级

- 未进 release notes 的升级：扩展为完整容器化开发环境——**bash + 11 种语言**（Ruby/Perl/PHP/Go/Java/Swift/Kotlin/C/C++/JS）、pip/npm 装包、网页文件下载
- 逆向结论："未发现逃逸/提权/跨会话影响"；模型的拒绝是**策略决策而非安全边界**（改 prompt 即可触发执行）

## 2. JupyterHub 多租户隔离（自建路径）

### 2.1 三层隔离结构

| 层 | 方案 | 要点 |
|---|---|---|
| 用户进程 | SystemdSpawner / DockerSpawner / KubeSpawner | 每用户独立容器/Pod；`mem_limit`/`cpu_limit`；**非 root（uid 1000）**；seccomp profile；`allowPrivilegeEscalation: false`；丢能力；idle-culler 回收闲置 |
| kernel 环境 | conda-pack + KernelSpecManager | 用户级 kernel 注册（`--user` 而非 `--sys-prefix`）；**共享 conda 环境是反模式**；注入用户环境变量（`CONDA_DEFAULT_ENV`/`PATH`） |
| kernel 权限 | 重写 `KernelManager.start_kernel()` | **URL 层权限是"致命缺陷"**：绕过 UI 直接 POST `/api/sessions` 可启动恶意 kernel——必须加 `start_kernel` scope 检查（403）+ `pre_start_kernel` 钩子注入租户配额（CPU/白名单），在解释器加载前生效 |

### 2.2 数据隔离红线

- **Linux 用户映射是地基**：`create_system_users=True` + umask 077 + `chmod 700 /home/*`（否则 `os.listdir("/home")` 泄漏用户存在性）
- 存储配额：单机 `setquota`/`prjquota`（project ID=UID）；容器只 bind `/home/{username}`，**绝不挂宿主根**
- 敏感文件硬拦截：`.env`/`.ssh/`/凭据目录永远禁止（jupyter-interpreter-mcp 的 `ALLOWED_UPLOAD_DIRS` 模式，默认仅 CWD）

### 2.3 2026 必补漏洞

- **CVE-2026-42266**（CVSS 8.8，2026-05-05 披露）：JupyterLab 4.0.0-4.5.6 经 **PyPI 扩展管理器**绕过扩展 allow-list 装任意包执行代码；修复 4.5.7——受限/多租户环境**必须禁用 PyPI Extension Manager**，并确保 kernel/terminal 真正沙箱化

## 3. 自建 vs 托管（数据类 Agent）

| 维度 | 自建 JupyterHub | ChatGPT 式托管（gVisor+Jupyter） |
|---|---|---|
| 隔离 | 需自己配好三层 | 平台内建 |
| 可控性 | 完全可控（数据不出内网） | 依赖平台策略 |
| 成本 | 运维重 | 按量 |
| 适用 | 企业内网/合规 | 快速上线 |

## 4. MCP 化（LLM 接入模式）

- **jupyter-interpreter-mcp**：把 JupyterHub 暴露为 MCP 工具给 LLM 用（`--allowed-dir`/`ALLOWED_UPLOAD_DIRS` 限制上传目录；敏感文件硬拦截）
- 教训：mcp-run-python（Pyodide 方案）已归档——**MCP server 的沙箱属性决定上层安全**，见 [05-WASM 与语言级沙箱](05-WASM与语言级沙箱.md)

## 面试速记

1. ChatGPT Code Interpreter = gVisor + Jupyter kernel + K8s；**每次请求新实例**；断网+内网包代理+URL 来源过滤。
2. 选 gVisor 不选 Firecracker：K8s 节点本身是 VM，无 /dev/kvm。
3. JupyterHub URL 层权限是致命缺陷——kernel 启动必须 scope 检查 + 配额注入（`pre_start_kernel`）。
4. 数据隔离三件套：用户映射+umask 077+只 bind 用户目录。
5. CVE-2026-42266：PyPI 扩展管理器绕过 allow-list，多租户环境必须禁用。

---

**下一模块**：[07-托管沙箱平台对比选型](07-托管沙箱平台对比选型.md) ｜ **返回总览**：[00-代码执行沙盒总览](00-代码执行沙盒总览.md)

## 参考来源

- [Poking Around ChatGPT's Sandbox（mkarots 逆向分析）](https://mkarots.github.io/blog/chatgpt-sandbox-exploration/)
- [ChatGPT's Hidden Container Upgrade Changes AI Coding](https://algustionesa.com/chatgpts-hidden-container-upgrade-changes-ai-coding/)
- [JupyterHub 多租户 RBAC 架构（CSDN）](https://wenku.csdn.net/column/20he1c054a)
- [Jupyter Notebook Security（systemshardening）](https://www.systemshardening.com/articles/kubernetes/jupyter-notebook-security/)
- [CVE-2026-42266（feedly）](https://feedly.com/cve/CVE-2026-42266)
- [jupyter-interpreter-mcp（GitHub）](https://github.com/lmseidler/jupyter-interpreter-mcp)
