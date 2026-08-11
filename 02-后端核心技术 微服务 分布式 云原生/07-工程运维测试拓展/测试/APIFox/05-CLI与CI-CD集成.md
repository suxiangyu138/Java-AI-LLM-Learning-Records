# 05-CLI 与 CI/CD 集成
> `apifox run` 把 APIFox 测试接进流水线：CLI 全命令家族、Jenkins/GitHub Actions 集成、定时任务、测试报告与失败通知

## 📚 目录
1. [CLI 定位与安装](#1-cli-定位与安装)
2. [apifox run：测试执行](#2-apifox-run测试执行)
3. [CLI 命令家族全解](#3-cli-命令家族全解)
4. [环境与变量管理（CLI）](#4-环境与变量管理cli)
5. [Jenkins 集成](#5-jenkins-集成)
6. [GitHub Actions 集成](#6-github-actions-集成)
7. [测试报告与失败通知](#7-测试报告与失败通知)
8. [2026 CLI 新能力：AI Agent 工作流](#8-2026-cli-新能力ai-agent-工作流)
9. [核心要点](#9-核心要点)
10. [参考来源](#10-参考来源)

## 1. CLI 定位与安装

```text
APIFox CLI = APIFox 的命令行入口
  ├─ 执行自动化测试（apifox run）
  ├─ 管理测试资源（场景/套件/用例/报告/定时任务）
  ├─ 管理 Mock 与环境/变量
  └─ 供 CI/CD 与 AI Agent 调用（2026 升级为 Agent 工作流入口）
```

```bash
# 安装（npm）
npm install -g apifox-cli
apifox --version

# 认证（使用个人访问令牌）
apifox login --token <APIFOX_ACCESS_TOKEN>
# 或环境变量方式（CI 推荐）
export APIFOX_ACCESS_TOKEN=<token>
```

## 2. apifox run：测试执行

```bash
# 基本用法：运行测试套件
apifox run --project <项目ID> --test-suite <套件ID>

# 常用参数
apifox run \
  --project <项目ID> \
  --test-suite <套件ID> \
  --environment <环境ID> \        # 指定运行环境
  --concurrency 4 \                # 并发数
  --fail-on-error \                # 有失败即退出码非 0（CI 门禁）
  --report-format json \           # 报告格式（json/html/junit）
  --report-output ./report.json    # 报告输出路径

# 运行场景用例
apifox run --project <项目ID> --test-scenario <场景ID>
```

| 关键参数 | 说明 |
|----------|------|
| `--fail-on-error` | **CI 门禁核心**：失败时退出码非 0，流水线拦截 |
| `--environment` | 指定环境（dev/test/prod） |
| `--concurrency` | 并发执行数（加速回归） |
| `--report-format` | json / html / junit（junit 供 Jenkins 解析） |

> 🎯 **CI 集成公式**：`apifox run --fail-on-error` + junit 报告 = 测试门禁 + 报告可视化，与 Newman（Postman）定位一致但资源管理在 APIFox 侧。

## 3. CLI 命令家族全解

2026 版 CLI 覆盖测试全生命周期：

```bash
# === 测试执行 ===
apifox run                    # 运行测试（套件/场景/用例）

# === 测试资源管理 ===
apifox test-scenario <子命令>   # 场景用例管理（创建/更新/查询/删除）
apifox test-suite <子命令>      # 测试套件管理
apifox test-case <子命令>       # 接口用例管理
apifox test-report <子命令>     # 测试报告管理（查询/导出）
apifox scheduled-task <子命令>  # 定时任务管理

# === Mock 与环境管理 ===
apifox mock <子命令>            # Mock 期望管理
apifox environment <子命令>     # 环境管理
apifox variables <子命令>       # 全局/团队变量管理

# === 资源导入导出 ===
apifox import / apifox export   # 接口资源导入导出

# === 结构校验（2026 新增） ===
apifox cli-schema               # 写入前校验复杂 JSON 结构（字段名/枚举/嵌套）
```

> 💡 CLI 管理命令让"测试资源即代码"成为可能：套件/用例/期望可以用脚本批量创建与维护，配合 Git 版本管理。

## 4. 环境与变量管理（CLI）

```bash
# 环境管理
apifox environment list --project <项目ID>
apifox environment create --name prod --project <项目ID> ...
apifox environment update --id <环境ID> ...

# 变量管理
apifox variables list
apifox variables set --key baseUrl --value https://api.example.com

# Mock 期望管理
apifox mock list --project <项目ID>
apifox mock create --expectation '{"path":"/api/orders/999","response":{...}}'
```

| 场景 | CLI 用法 |
|------|---------|
| CI 动态环境 | 流水线中创建/更新环境变量（如动态 baseUrl） |
| 多环境回归 | 同一套件在不同环境执行（dev → test → prod） |
| Mock 批量配置 | 脚本批量创建期望 |
| 团队变量同步 | 从 Git 同步变量定义 |

## 5. Jenkins 集成

```groovy
// Jenkinsfile（API 回归测试阶段）
pipeline {
    agent any
    environment {
        APIFOX_ACCESS_TOKEN = credentials('apifox-token')   // Jenkins 凭证管理
        PROJECT_ID = 'your-project-id'
        SUITE_ID = 'your-suite-id'
    }
    stages {
        stage('Build & Test') {
            steps { sh 'mvn clean package' }
        }
        stage('API Regression') {
            steps {
                sh """
                    apifox run \
                      --project ${PROJECT_ID} \
                      --test-suite ${SUITE_ID} \
                      --environment prod \
                      --fail-on-error \
                      --report-format junit \
                      --report-output ./apifox-report.xml
                """
            }
            post {
                always {
                    junit 'apifox-report.xml'          // Jenkins 报告可视化
                }
            }
        }
        stage('Deploy') {
            steps { sh './deploy.sh' }
        }
    }
}
```

> 🎯 流程价值：**API 回归通过才部署**——把"接口契约"变成部署门禁的一部分。

## 6. GitHub Actions 集成

```yaml
# .github/workflows/api-test.yml
name: API Regression Test
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  api-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup Node
        uses: actions/setup-node@v4
        with: { node-version: 20 }
      - name: Install APIFox CLI
        run: npm install -g apifox-cli
      - name: Run API Tests
        env:
          APIFOX_ACCESS_TOKEN: ${{ secrets.APIFOX_ACCESS_TOKEN }}
        run: |
          apifox run \
            --project ${{ vars.APIFOX_PROJECT_ID }} \
            --test-suite ${{ vars.APIFOX_SUITE_ID }} \
            --environment prod \
            --fail-on-error \
            --report-format junit \
            --report-output ./apifox-report.xml
      - name: Upload Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: apifox-report
          path: ./apifox-report.xml
```

| 集成要点 | 说明 |
|----------|------|
| Token 安全 | `secrets.APIFOX_ACCESS_TOKEN`（不入仓库） |
| 门禁生效 | `--fail-on-error` 失败即 PR 阻塞 |
| 报告留存 | artifact 上传（always 保证失败也有报告） |
| 触发策略 | push main / PR 触发 |

## 7. 测试报告与失败通知

```bash
# 报告格式与解析
--report-format junit    # Jenkins/GitLab CI 原生解析
--report-format json     # 自定义解析（脚本断言）
--report-format html     # 人类可读报告
```

| 通知渠道 | 方式 |
|----------|------|
| 平台内 | 定时任务失败通知（企业微信/钉钉/邮件/Webhook） |
| CI 内 | 流水线失败即通知（Jenkins 邮件/Actions 通知） |
| 报告归档 | 每次运行报告留存，趋势对比 |

> 💡 **失败通知闭环**：CI 拦截（门禁）+ 通知（谁负责）+ 报告（失败现场）——三件套齐了才算完整接入。

## 8. 2026 CLI 新能力：AI Agent 工作流

2026 年 CLI 从"测试执行工具"升级为 **AI Agent 的命令行入口**（Cursor、Claude Code、Trae、Codex 等）：

```text
AI Agent 使用 APIFox 的自动化闭环：
读取接口（CLI 查询）→ 生成测试用例（AI 生成）→ 结构校验（cli-schema）
→ 写入 APIFox（CLI 创建）→ 回读确认（CLI 查询）→ 运行测试（apifox run）
```

| 能力 | 说明 |
|------|------|
| 结构化命令 | Agent 通过 CLI 完成接口查看/用例创建/场景编排/测试运行 |
| `cli-schema` | 写入前校验复杂 JSON 结构（字段名/枚举值/嵌套结构） |
| 8 个 SKILL | 帮助 Agent 理解 CLI 命令与任务流程 |
| AI 分支 | Agent 修改资源先在独立分支完成，确认差异后合入（防污染主分支） |

> 💡 完整用法见 [07-AI能力与MCP调试](07-AI能力与MCP调试.md)。

## 9. 核心要点

> 🎯 **核心要点**：
> - 安装认证：npm 全局安装 + Token（环境变量方式 CI 友好）；
> - CI 门禁公式：`apifox run --fail-on-error` + junit 报告；
> - CLI 命令家族：run（执行）+ 资源管理（场景/套件/用例/报告/定时）+ mock/environment/variables；
> - Jenkins：credentials 管 Token + junit 报告可视化 + 部署前门禁；
> - Actions：secrets 管 Token + artifact 留存报告 + PR 门禁；
> - 2026：CLI 成为 AI Agent 工作流入口（cli-schema 校验 + SKILL + AI 分支）。

## 10. 参考来源

- [APIFox CLI 官方文档](https://apifox.com/help/cli/)
- [Apifox CLI + Skill 发布说明（2026）](https://www.apifox.cn/blog/apifox-cli/)
- [APIFox 6 月更新（CLI 升级）](https://apifox.cn/blog/features-2026-6/)

---

**下一模块**：[06-团队协作与导入导出](06-团队协作与导入导出.md)　/　**返回总览**：[00-总览](00-APIFox知识体系总览.md)
