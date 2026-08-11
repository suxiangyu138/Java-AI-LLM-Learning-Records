# 10-Agentic CI 与面试冲刺：AI 编码代理进流水线、20 自测与速记

> 定位：2026 年 GitHub Actions 的最前沿是"AI 编码代理被工作流编排"——gh-aw 把强化版工作流编译成运行 Copilot/Claude/Codex 的流水线。本篇讲清 Agentic CI 形态与趋势，并以 20 道自测 + 面试题 + 速记卡收尾整个体系。

## 1. Agentic CI：AI 编码代理进入流水线

**gh-aw（Agentic Workflows）**：2026-02-13 开源的技术预览 CLI 扩展（`gh extension install github/gh-aw`），用带 YAML frontmatter 的 Markdown 描述任务，`gh aw compile` 编译为运行 AI 编码代理的强化版 Actions 工作流。安全默认值值得关注：**默认只读、网络隔离、依赖 SHA 固定**——把"AI 代理在 CI 里跑代码"的供应链风险前置堵死。

```markdown
---
# gh-aw 任务文件（概念形态）
name: 修复测试失败
on:
  workflow_call:
    inputs:
      run-id:
        type: number
model: claude          # 支持的代理：Copilot、Claude、Codex
agent: true            # 代理模式：可读写分支、跑测试、提交修复
---
1. 检出指定 run 的失败测试
2. 定位根因并修复（可运行测试验证）
3. 提交修复 PR 并附失败分析
```

它的价值不是"AI 生成 YAML"（那是 Copilot 辅助），而是**把 AI 代理本身变成流水线的执行单元**——失败测试自动修复、PR 机器人自动处理、代码审查助理并发执行——流程仍由 Actions 的确定性机制调度（权限、门禁、审计都在），代理只在受约束的沙箱里行动。

**为代理设计约束（工程师的新职责）**：代理进流水线后，工程师的活儿从"写步骤"变成"画边界"——给代理的权限声明（只读还是可写分支）、网络策略（能否访问外网，默认隔离）、执行预算（超时与重试上限）、验收标准（代理提交的 PR 要过什么检查才合）。这套"约束设计"与工作流思想同构：确定性外壳（Actions 的权限/门禁/审计）包住非确定性内核（模型行为），正是《WorkFlow》体系 10 篇讲的 Agentic Workflow 在 CI 域的投影——两个体系在这一页交汇。

**AI 生成工作流的验收清单**：AI 写的 YAML 不等于可用的 YAML——合并前逐项过：① actionlint 零错误（语法与 schema）；② permissions 是否显式且最小（AI 默认给宽）；③ 密钥是否走 secrets 而非明文；④ 有没有 concurrency 与 timeout；⑤ 路径过滤与触发事件是否与需求一致；⑥ 表达式里的不可信输入是否经 env 中转。六项齐了才放行——AI 负责产出，人负责验收，这是 2026 年"AI 辅助工程化"的正确姿势。

**AI 写工作流的两层现实**：其一，Copilot/编辑器内联生成——2026-01 编辑器增强后（schema 校验、表达式补全、悬停文档），AI 生成的 YAML 被工具链即时校验，"生成即正确"的门槛大幅降低；其二，生产环境的 AI 生成物仍然必须过 actionlint + review——**AI 生成代码的审查责任在团队，不在模型**（呼应《Vibe Coding》体系的工程纪律）。

## 2. 2026-2027 趋势与工程师新技能

- **平台化深化**：2025-08 起所有 job 已运行在重构后的新后端上，企业级 job 启动吞吐提升 7 倍——Actions 的规模天花板被拉高，大规模矩阵与高速并行的门槛继续下降。
- **语法持续演进**：并行步骤（2026-06）、`$/` 自引用（2026-07）、case 函数（2026-01）——声明式能力在吸收"脚本里手搓的活"，工作流语言本身在成熟。
- **安全左移固化**：OIDC 自定义属性、供应链 pin SHA 常态化、org 级市场白名单——"CI 安全基线"从最佳实践变成平台默认。
- **AI 代理编排**：从"AI 帮你写 YAML"到"AI 在 YAML 里跑"——流水线工程师的新技能是**为代理设计约束**（权限边界、网络隔离、结果验收标准），本质还是"把过程变数据、把执行交平台"的工作流思想（呼应《WorkFlow》体系的 Agentic Workflow 结论：确定性基座 + 自主执行者）。

## 3. 20 道自测题

1. workflow、job、step、action、runner 五层的关系是什么？
2. 并行 job 与串行 job 分别用什么声明？
3. `paths` 过滤的价值与陷阱分别是什么？
4. `pull_request_target` 与 `pull_request` 的区别与风险是什么？
5. cron 触发如何指定非 UTC 时区？
6. workflow_dispatch 的输入上限是多少？外部系统触发用什么事件？
7. `${{ }}` 与 `$VAR` 的求值时机差异是什么？
8. if 条件的真值规则有哪些？"游离文本"陷阱是什么？
9. case 函数解决什么问题？
10. needs、steps、github 三个上下文分别装什么？
11. 2026 年标准 Runner 与 Larger Runner 的定价差异是什么？
12. arm64 Runner 为什么值得用？适用边界是什么？
13. 自托管 Runner 的核心安全风险是什么？怎么隔离？
14. 官方 actions 集里 Java 工程必用的五个是什么？
15. 第三方 Action 为什么建议 pin SHA？
16. `$/` 自引用解决什么问题？需要什么版本？
17. GITHUB_TOKEN 的 permissions 最小化怎么写？id-token 什么时候开？
18. secrets 与 vars 的区别是什么？secrets 的三个层级是什么？
19. OIDC 相比长效密钥的优势是什么？2026 年新增了什么能力？
20. 缓存 key 怎么设计？2026 年缓存上传限流是什么？

## 4. 面试题与答题范式（8 题）

- **介绍你的 CI 流水线**：范式 = 触发设计（事件 + 路径过滤）→ 阶段划分（lint/测试/部署）→ 门禁体系（分支保护 + 环境审批）→ 安全基线（权限/secrets/OIDC）→ 成本数据（分钟/命中率）。
- **OIDC 怎么免密钥**：原理（短期令牌 → 云厂商验证 → 临时凭据）→ 配置（id-token: write + configure-aws-credentials）→ 信任策略（sub claim 限定仓库/ref/环境）→ 2026 自定义属性（按组织分类细化）。
- **缓存 key 怎么设计**：内容哈希（hashFiles）→ restore-keys 回退 → 维度合并（os + 工具链）→ 2026 限流（200 次/分钟上传，防抖动）。
- **自托管 Runner 安全吗**：风险（不可信代码执行平台）→ 三条红线（受信分支限定/一次性环境/不留密钥）→ 与托管的成本权衡。
- **矩阵怎么控制成本**：区分验证矩阵与常规矩阵 → exclude/include 收敛 → fail-fast 策略 → 动态矩阵配置化。
- **GITHUB_TOKEN 权限怎么管**：默认只读（2023+）→ 显式 permissions → id-token 最小开 → 与环境保护组合。
- **CI 安全怎么防供应链**：pin SHA → Dependabot → 评估四问 → org 白名单 → SBOM 附签。
- **AI 会取代 CI 工程师吗**：不会——AI 代理成为执行单元（gh-aw），但约束设计、门禁体系、成本治理仍是人定义的；引用"确定性基座 + 自主执行者"。

### 4.1 面试高频追问链准备

面试官从"你的 CI 怎么做的"往下追的常见链：**"权限怎么配的"**（显式最小、id-token 最小开）→ **"密钥怎么管的"**（三层放置、季度轮换、OIDC 免长效密钥）→ **"矩阵怎么控制成本的"**（常规/验证矩阵分离、exclude/include、arm64）→ **"缓存怎么设计的"**（内容哈希 key、restore-keys 回退、2026 上传限流）→ **"安全怎么防供应链"**（pin SHA、Dependabot、评估四问、SBOM）。每问的标准答法是"原理一句话 + 我的实践 + 权衡"——背住这条链，面试中段的高频区就稳了。

## 5. 自测通过线

- **通过标准**：20 题中对 16 题可进入面试冲刺；错题集中在 14-19 题（Action/安全）属正常，回到 05/06 篇重读后重测。
- **错题归因**：1-6 题错 → 补 01/02 篇；7-10 题错 → 补 03 篇；11-13 题错 → 补 04 篇；14-16 题错 → 补 05 篇；17-19 题错 → 补 06 篇；20 题错 → 补 07 篇。
- **面试前 10 分钟**：只背速记卡十条 + 8 道范式的四段式骨架，细节靠体系理解现场组织。

## 6. 速记卡（背诵版）

1. Actions = 事件驱动的 YAML 流水线；五层：工作流 → 作业（并行）→ 步骤（顺序）→ 动作（复用）→ Runner。
2. 触发四主类：push/PR、schedule（可带时区）、workflow_dispatch（手动）、repository_dispatch（外部）。
3. 表达式 `${{ }}` 运行前求值，`$VAR` 运行时展开；if 真值：空串/0/null 为假。
4. 上下文九件套：github/needs/jobs/steps/runner/env/vars/secrets/inputs/matrix。
5. 2026 新语法：并行步骤（background/wait/cancel）、case 函数、`$/` 同仓库自引用、cron 时区、deployment: false。
6. Runner 定价：标准 2 核 $0.006/min（Linux），Larger 按核计费，arm64 省 20-39%，自托管免费（平台费无限期推迟）。
7. 安全基线：permissions 最小化、secrets 三层（环境 > org > repo）、OIDC 免密钥、第三方 pin SHA、不可信输入过 env。
8. 缓存 key = 内容哈希 + 回退前缀 + 维度合并；2026 起上传限 200 次/分钟；制品默认保留 90 天。
9. 成本三板斧：路径过滤、规格匹配、矩阵收敛；生产规范 = 模板仓库 + actionlint + review。
10. Agentic CI：gh-aw 让 AI 代理在受约束沙箱里跑（只读/网络隔离/SHA 固定）；确定性调度 + 自主执行是 2026 共识。

> 🎯 收尾要点：把 GitHub Actions 讲出体系感的三板斧——**触发与过滤（为什么跑）、权限与密钥（凭什么跑）、成本与复用（跑得起吗）**。答出这三点，面试官会认为你不只是用过，而是管过。

---

**返回总览**：[00-GitHub Actions 知识体系总览](./00-GitHub%20Actions%20知识体系总览.md)

**参考来源**：

- [gh-aw：Agentic Workflows 技术预览 | GitHub](https://github.com/github/gh-aw)
- [GitHub Actions 2026 更新：case 函数、条件日志和环境配置 | CSDN](https://coderfix.blog.csdn.net/article/details/160760255)
- [GitHub Actions 2026 架构升级与新功能实战指南 | CSDN](https://blog.csdn.net/diandianxiyu_geek/article/details/160561855)
- [Workflow limits and scaling | GitHub Docs](https://docs.github.com/en/actions/reference/usage-limits-billing-and-administration)
