# 09 - Human-in-the-Loop 循环

> 🎯 AI Agent 的终极安全网——在关键决策点插入人类确认。这不是"AI 不行才要人"，而是"风险控制的基本架构"。本章覆盖审批循环、纠正循环和反馈闭环的设计模式

---

## 目录

1. [HITL 的四种模式](#1-hitl-的四种模式)
2. [审批循环设计](#2-审批循环设计)
3. [纠正循环设计](#3-纠正循环设计)
4. [自由度梯度](#4-自由度梯度)

---

## 1. HITL 的四种模式

```text
AI 自主程度递增 →

模式 1: Human Only（纯人工）
  AI 不参与 → 全部由人完成

模式 2: AI Suggest（AI 建议）
  AI 生成建议 → 人类审核 → 人类执行

模式 3: AI Act, Human Approve（AI 执行，人审批）
  AI 生成+执行建议 → 但需人类确认后才生效

模式 4: AI Act, Human Monitor（AI 执行，人监控）
  AI 自主执行 → 人类在后台监控 → 异常时介入

模式 5: Full Auto（完全自动）
  AI 自主执行 → 无需人类干预
```

### 使用场景映射

```text
什么操作需要哪个模式？

模式 2 (AI Suggest):
  → 写邮件草稿、代码建议、翻译
  → 成本低、出错后果小

模式 3 (AI Act, Human Approve):
  → 发送邮件、提交 PR、修改生产配置
  → 不可逆操作、有外部影响

模式 4 (AI Act, Human Monitor):
  → 自动回复常见客服问题、监控告警处理
  → 高频操作、人工审核成本太高

模式 5 (Full Auto):
  → 天气预报查询、简单计算、格式转换
  → 确定性高、无风险的操作
```

## 2. 审批循环设计

### 2.1 审批工作流

```python
class ApprovalLoop:
    def __init__(self, risk_classifier):
        self.risk = risk_classifier

    def execute(self, action: dict) -> str:
        # 1. 风险评估
        risk_level = self.risk.classify(action)

        # 2. 低风险 → 自动执行
        if risk_level == "low":
            return self._auto_execute(action)

        # 3. 高风险 → 进入审批循环
        approval = self._request_approval(action, risk_level)

        # 4. 审批循环（最多 3 次驳回）
        for attempt in range(3):
            if approval.status == "approved":
                return self._execute(action)

            if approval.status == "rejected_with_feedback":
                # 根据反馈修正
                action = self._revise(action, approval.feedback)
                approval = self._request_approval(action, risk_level)

            if approval.status == "rejected_permanent":
                return "操作已被永久拒绝"

        return "审批循环超限"
```

### 2.2 审批消息设计

```text
好的审批消息：
┌─────────────────────────────────────┐
│ ⚠️ AI 请求执行以下操作：               │
│                                     │
│ 操作：删除 production 数据库的日志表  │
│ 影响：约 500 万条日志记录将被删除      │
│ 风险等级：🔴 HIGH                    │
│ AI 理由：日志表已超过 30 天保留期限    │
│                                     │
│ [ ✅ 批准 ]  [ ❌ 拒绝 ]  [ ✏️ 修改 ] │
└─────────────────────────────────────┘

→ 清晰的 What / Why / Risk / Action
```

## 3. 纠正循环设计

### 3.1 在线纠正

```text
用户看到 AI 输出 → 不满意 → 纠正 → AI 改进

纠正循环：
  AI 回答 → 用户指出问题 → AI 理解反馈 → 修正回答
      ↑                                          │
      └──────────── 仍不满意，继续修正 ←────────────┘

实现要点：
  → 保存纠正历史（不要犯同样的错）
  → 同一会话内立即生效
  → 跨会话需要写入长期记忆
```

### 3.2 后验纠正（反馈吸收）

```python
class FeedbackLoop:
    def __init__(self, memory_manager):
        self.memory = memory_manager

    def absorb_feedback(self, session_id, feedbacks: list[dict]):
        """吸收一批用户反馈，形成改进计划"""
        # 1. 聚类反馈
        clusters = self._cluster_feedbacks(feedbacks)

        # 2. 每个聚类 → 一条记忆
        for issue, items in clusters.items():
            if len(items) >= 3:  # 至少 3 人反馈才记录
                self.memory.write(
                    f"用户反馈：{issue}。共 {len(items)} 次反馈。"
                    f"改进：下次遇到类似场景时，应该..."
                )

        # 3. 生成改进报告
        return self._generate_improvement_report(clusters)
```

## 4. 自由度梯度

```text
根据用户信任度动态调整 AI 的自主程度：

新用户（信任度低）：
  → 所有写操作需要审批
  → 高风险操作禁止
  → 输出带置信度标注

老用户（信任度高）：
  → 常规操作自动执行
  → 小额支付不需要审批
  → 异常时才求助人类

实现：
  trust_score = f(历史审批通过率, 纠错次数, 使用时长)
  auto_threshold = scale(trust_score)
```

## 核心要点回顾

- HITL 五级：纯人工 → 建议 → 审批 → 监控 → 全自动
- 审批循环：风险评估 → 请求审批 → 驳回则修正 → 最多 3 轮
- 审批消息三要素：做什么 + 为什么 + 风险等级
- 纠正循环：在线纠正（立即生效）+ 后验纠正（聚类→记忆）
- 自由度梯度：新用户保守、老用户宽松、随信任动态调整

## 参考资料

1. Human-in-the-Loop Machine Learning (Robert Monarch, 2021)
2. Claude Code 审批机制
3. Snorkel AI 弱监督反馈循环
