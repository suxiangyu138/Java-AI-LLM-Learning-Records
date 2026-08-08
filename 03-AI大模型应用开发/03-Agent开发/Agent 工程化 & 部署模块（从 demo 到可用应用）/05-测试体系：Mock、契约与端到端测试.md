# 05 测试体系：Mock、契约与端到端测试

> 定位：让"代码逻辑"可以被快速、便宜、稳定地验证——LLM 不确定性之下，测试策略的分层是关键（2026-08 基准）

## 📚 目录

1. [Agent 测试的独特挑战](#1-agent-测试的独特挑战)
2. [测试金字塔：四层策略](#2-测试金字塔四层策略)
3. [Mock LLM 的三种模式](#3-mock-llm-的三种模式)
4. [工具与编排逻辑测试](#4-工具与编排逻辑测试)
5. [契约测试：锁定输出格式](#5-契约测试锁定输出格式)
6. [故障注入与边界测试](#6-故障注入与边界测试)
7. [CI 集成与测试数据](#7-ci-集成与测试数据)
8. [落地检查单](#8-落地检查单)

## 1. Agent 测试的独特挑战

| 挑战 | 普通软件 | Agent |
|------|---------|-------|
| 输出不确定 | 输入→输出确定 | 同一输入可能不同输出 |
| 副作用难控 | 依赖可 mock | 工具调用链长，状态耦合 |
| 测试成本 | 免费/便宜 | 真实 API 调用烧钱、限流、不稳定 |
| 行为空间 | 有限分支 | 工具选择组合爆炸 |

> 🎯 **核心策略**：把"不确定性"隔离在 LLM 适配层之外——**业务逻辑用确定性测试**，**LLM 行为用评估体系测**（06 篇）。测试体系管"代码没写错"，评估体系管"Agent 没变笨"，两者分工不重叠。

## 2. 测试金字塔：四层策略

```text
        ┌──────────────┐
        │  E2E（少量）   │  真实 API + 真实工具，冒烟/发布前跑，烧钱
        ├──────────────┤
        │  集成（中量）   │  Mock LLM + 真实工具实现（本地），核心场景
        ├──────────────┤
        │  组件（多量）   │  Mock LLM + Fake 工具，编排逻辑全覆盖
        ├──────────────┤
        │  单元（大量）   │  纯函数：工具执行、校验、状态机、预算逻辑
        └──────────────┘
        ↑ 越往下越便宜、越快、越稳定，跑得越频繁
```

| 层 | 成本/次 | 稳定性 | 跑的位置 |
|----|--------|--------|---------|
| 单元 | ~0（无网络） | 100% 稳定 | 每次 commit |
| 组件 | ~0（无网络） | 100% 稳定 | 每次 PR |
| 集成 | 低（本地） | 高 | PR + 发布前 |
| E2E | 高（API 费用） | 中（依赖供应商） | 发布前 + 定期冒烟 |

> ⚠️ **红线**：CI 里禁止真实 API 调用——既烧钱又不稳定（限流会让测试 flaky）。CI 只跑前两层 + 少量集成；E2E 走专门的夜间/发布任务。

## 3. Mock LLM 的三种模式

| 模式 | 做法 | 适用 | 优点 | 缺点 |
|------|------|------|------|------|
| 回放（VCR） | 录制真实对话存 fixture，测试时按输入匹配回放 | 回归、复现 bug | 最接近真实 | fixture 过期（prompt 变更后需重录） |
| 契约 stub | 按 JSON Schema 返回"合法但预设"的输出 | 测试编排逻辑 | 快、可控 | 与真实模型行为有差距 |
| 确定性 fake | 规则引擎模拟（含工具调用决策） | 循环/终止条件测试 | 完全确定 | 需维护 |

**回放模式示例**：

```python
class ReplayProvider(LLMProvider):
    """按 (prompt_hash, 轮次) 匹配 fixture 回放，未命中抛错（提示需重录）"""
    async def chat(self, messages, **kwargs):
        key = (hashlib.sha256(serialize(messages)).hexdigest(), self.counter)
        if key not in self.fixtures:
            raise FixtureMissing(key)   # 明确失败而非静默变弱
        return self.fixtures[key]

# 重录方式：真实调用一次并保存（集成测试里做，结果落 fixture 文件）
```

> 💡 生产项目里 `FixtureMissing` 直接导致测试失败——这是特性不是 bug：prompt 一改，所有相关测试立刻告诉你"fixture 过期了"。

## 4. 工具与编排逻辑测试

### 4.1 工具函数测试（纯函数化）

```python
def test_refund_amount_cap():
    assert refund.calculate({"amount": 500, "max": 300}).blocked
    assert refund.calculate({"amount": 100, "max": 300}).ok

def test_tool_idempotency():
    # 相同 idempotency_key 第二次执行返回相同结果，无副作用
```

**工具实现要求**（呼应 02 篇契约）：纯函数核心 + 注入的外部依赖（DB/HTTP 接口），单测注入 fake 即可全覆盖。

### 4.2 编排逻辑测试

| 测试点 | 手法 | 断言 |
|--------|------|------|
| 终止条件 | 确定性 fake 返回固定工具调用序列 | 循环在 max_steps 内停止 |
| 死循环防护 | fake 一直请求同一工具 | 达到 `max_tool_calls` 上限即终止 |
| 错误恢复 | fake 返回 `retry: true` / `retry: false` | 重试 vs 放弃路径正确 |
| 预算超限 | fake 返回高 token 用量 | 触发预算中断逻辑 |
| 工具顺序 | fake 按脚本编排多次调用 | 调用序列符合状态机 |

**轨迹断言**：Agent 测试的断言对象不是最终答案，而是**调用轨迹**（工具选择序列、参数、恢复动作）——这比"输出字符串匹配"可靠得多。

## 5. 契约测试：锁定输出格式

```text
问题：模型输出 JSON 结构变了 → 下游解析崩溃（生产事故高发区）
方案：契约测试把"输出结构"锁死：
  1. LLM 调用声明输出 Schema（structured output / strict 模式）
  2. 契约测试：Mock 返回合法/非法样本，验证解析层行为
  3. 解析失败路径必须有显式行为（重试/降级/报错），不允许静默丢弃
```

| 契约测试项 | 用例 |
|-----------|------|
| 合法样本 | 各字段齐全、类型正确 → 解析成功 |
| 缺字段 | 缺少必填字段 → 走降级路径 |
| 类型错误 | `amount` 是字符串 → 拒绝 + 重试 |
| 超长字段 | 工具结果 4KB 截断逻辑正确 |
| 非法工具名 | 模型返回不存在的工具 → 安全跳过并记录 |

> 联动：结构化输出的完整机制（strict、Pydantic 校验）见 [Function Calling 体系](../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/05-参数校验与安全.md)。

## 6. 故障注入与边界测试

```text
目标：验证 Agent 在"供应商/工具/网络"全链路故障下的行为
手法：对注入层（Provider/Tool/HTTP）注入故障，断言降级行为
```

| 注入故障 | 预期行为 |
|---------|---------|
| Provider 超时 | 重试（指数退避）→ 超上限后降级到备用模型 |
| Provider 5xx/429 | 退避重试 → 熔断 → 返回友好错误 |
| 工具超时 | 放弃该工具继续流程（或转人工） |
| 工具返回业务错误 | 错误回传 LLM，观察能否自救（结构化错误码） |
| 流式中断 | 已收 chunk 能组装、连接恢复重连 |
| 并发冲击 | 限流/排队生效，不雪崩 |

> 💡 故障注入测试用 `FaultyProvider`（包装真实 Provider 按概率注入故障）跑集成层，不需要真实 API。

## 7. CI 集成与测试数据

### 7.1 CI 测试策略

```text
每个 PR：
  ├─ 单元 + 组件测试（全量，无 API）
  ├─ 集成测试（Mock LLM + 真实本地工具实现）
  └─ 契约测试（输出 schema）
发布前：
  ├─ E2E 冒烟（真实 API，小数据集）
  └─ 评估门禁（06 篇：golden set + 统计回归）

禁止：CI 里直接调真实 API（费用 + 限流 flaky）
```

### 7.2 测试数据治理

| 项 | 要求 |
|----|------|
| 数据来源 | 真实会话脱敏后入库（PII 打码） |
| 黄金样本 | 与评估集复用但分工明确（测试=代码行为，eval=质量） |
| fixture 版本 | fixture 与 prompt 版本绑定，变更 prompt 触发重录提示 |
| 数据量 | 编排测试几十例足够；评估集 100-200 例/路由（06 篇） |
| 保密 | 含业务数据的 fixture 不进公共仓库 |

## 8. 落地检查单

```text
□ 单元测试覆盖：工具纯函数、校验、预算、状态机
□ Mock LLM 三模式就位（回放/契约/fake），CI 零 API 调用
□ 编排测试覆盖：终止条件、死循环、错误恢复、预算中断
□ 契约测试锁定输出 schema，解析降级路径有测试
□ 故障注入覆盖 Provider/工具/流式三类故障
□ 测试数据脱敏，fixture 与 prompt 版本绑定
□ 评估门禁已与测试分层（测试管代码，eval 管质量）
```

---

## 【参考来源】

- [Atlas research（2026-06）: Evals in CI/CD（三层级联门禁中的确定性检查层）](https://github.com/Laoujin/Atlas/blob/main/research/2026-06-04-evals-how-do-you-know-your-ai-works-session-blueprint/evals-in-ci-cd/index.md)
- [futureagi.com: CI/CD for AI Agents in 2026（测试与门禁分层）](https://futureagi.com/blog/ci-cd-for-ai-agents-best-practices-2026/)
- [Mindflow: 9 Engineering Practices That Actually Work（工具测试与契约）](https://mindflow.io/blog/the-production-ai-agent-reality-check-9-engineering-practices-that-actually-work)
- [MLflow: Building Production-Ready AI Agents in 2026（可测试性）](https://mlflow.org/articles/building-production-ready-ai-agents-in-2026/)
- 结构化输出/参数校验细节：见 [Function Calling 体系 05 篇](../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/05-参数校验与安全.md)

---

**返回总览**：[00-总览：Agent 工程化与部署模块（从 demo 到可用应用）](00-总览：Agent%20工程化与部署模块（从%20demo%20到可用应用）.md)

**下一模块**：[06-评估体系：Eval、回归门禁与 LLM-as-Judge](06-评估体系：Eval、回归门禁与%20LLM-as-Judge.md)
