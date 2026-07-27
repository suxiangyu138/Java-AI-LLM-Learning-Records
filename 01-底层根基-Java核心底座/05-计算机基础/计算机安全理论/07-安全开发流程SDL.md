# 07 - 安全开发流程 SDL

> 🎯 在代码写完后再加安全 = 补漏洞。SDL（Security Development Lifecycle）的核心思想是"安全左移"——从需求阶段就开始考虑安全，每阶段都有安全检查

---

## 目录

1. [SDL 框架](#1-sdl-框架)
2. [威胁建模](#2-威胁建模)
3. [安全测试工具链](#3-安全测试工具链)
4. [DevSecOps](#4-devsecops)

---

## 1. SDL 框架

```text
Microsoft SDL（最经典的框架）：

需求 → 设计 → 实现 → 验证 → 发布 → 响应
  │      │      │      │      │      │
  ├──安全需求分析
         ├──威胁建模
                ├──静态分析+安全编码
                       ├──渗透测试+模糊测试
                              ├──安全评审
                                     ├──应急响应
```

### 各阶段安全活动

| 阶段 | 安全活动 | 工具/方法 |
|------|---------|---------|
| **需求** | 安全需求调研、隐私影响评估 | 需求文档安全章节 |
| **设计** | **威胁建模**、攻击面分析 | STRIDE 模型 |
| **实现** | 安全编码规范、静态分析 | SonarQube / SpotBugs |
| **验证** | DAST、渗透测试、模糊测试 | OWASP ZAP / Burp Suite |
| **发布** | 安全评审、应急预案 | 上线检查清单 |
| **响应** | 漏洞披露、补丁管理 | 安全响应中心(SRC) |

## 2. 威胁建模

```text
STRIDE 模型（Microsoft 提出，最常用）：

Spoofing (欺骗)         → 身份伪造       → 强认证
Tampering (篡改)        → 数据被修改      → 完整性校验
Repudiation (否认)      → 不承认操作      → 审计日志
Information Disclosure  → 信息泄露       → 加密+访问控制
Denial of Service       → 拒绝服务       → 限流+高可用
Elevation of Privilege  → 权限提升       → 最小权限+沙箱
```

```text
威胁建模流程：
1. 画数据流图（DFD）：数据从哪来、经过哪、到哪去
2. 对每个数据流/存储/处理节点 → STRIDE 逐个分析
3. 识别威胁 → 评级（DREAD）→ 决定处理策略
4. 处理策略：消除 > 缓解 > 转移 > 接受
```

## 3. 安全测试工具链

| 类型 | 工具 | 检测什么 | 阶段 |
|------|------|---------|:---:|
| **SAST** 白盒 | SonarQube, SpotBugs, CodeQL | 代码中的安全漏洞 | 编码时 |
| **DAST** 黑盒 | OWASP ZAP, Burp Suite, Acunetix | 运行中的安全漏洞 | 测试时 |
| **SCA** 组件分析 | Snyk, OWASP Dependency-Check | 第三方依赖漏洞(CVE) | 构建时 |
| **IAST** 交互式 | Contrast, Seeker | 运行时上下文+代码分析 | 测试时 |
| **容器扫描** | Trivy, Clair, Docker Scout | 容器镜像漏洞 | 构建时 |
| **密钥扫描** | TruffleHog, GitLeaks | 代码中的硬编码密钥 | 提交前 |

```bash
# CI/CD 中集成安全扫描（GitHub Actions）
- name: SAST Scan
  run: mvn sonar:sonar

- name: Container Scan
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: ${{ secrets.DOCKER_USER }}/app:${{ github.sha }}

- name: Dependency Check
  run: snyk test --severity-threshold=high
```

## 4. DevSecOps

```text
DevSecOps = DevOps + Security（安全融入 DevOps 全流程）

核心原则：
  1. 安全左移 (Shift Left)：安全测试越早越好
  2. 自动化：安全测试融入 CI/CD 流水线
  3. 持续监控：生产环境持续安全扫描
  4. 安全即代码：安全策略代码化（OPA/Rego）
```

### 安全流水线检查点

```text
代码提交 → Pre-commit Hook(密钥扫描)
  → CI Build(SAST+SCA+容器扫描)
  → 测试环境(DAST+渗透测试)
  → 部署前(安全审批+合规检查)
  → 生产(持续监控+容器运行时安全)
```

## 核心要点回顾

- SDL = 安全融入开发全流程，不是事后补丁
- STRIDE = 欺骗/篡改/否认/泄露/拒绝/提权 六维威胁建模
- SAST(白盒) + DAST(黑盒) + SCA(依赖) = 安全测试铁三角
- DevSecOps = 安全左移 + 自动化 + 持续监控
- 密钥扫描必须放在 pre-commit hook 中

## 参考资料

1. Microsoft SDL 文档
2. OWASP Threat Modeling
3. NIST SSDF 安全开发框架
