# 10 - AI 伦理合规与负责任 AI

> 🎯 AI 产品不是"能力强就够了"——可解释性、公平性、隐私保护、法律法规合规是产品能否长期存活的基础。本文覆盖六大 AI 伦理维度、国内外核心法规、以及可落地的合规检查清单

---

## 目录

1. [AI 伦理六维度](#1-ai-伦理六维度)
2. [全球核心法规速览](#2-全球核心法规速览)
3. [可解释性实践](#3-可解释性实践)
4. [公平性与偏见检测](#4-公平性与偏见检测)
5. [隐私保护技术方案](#5-隐私保护技术方案)
6. [企业合规落地检查清单](#6-企业合规落地检查清单)

---

## 1. AI 伦理六维度

| 维度 | 核心问题 | 典型风险 | 产品影响 |
|------|---------|---------|---------|
| **公平性** | AI 是否区别对待不同群体？ | 招聘 AI 歧视性别/年龄 | 品牌危机 + 法律诉讼 |
| **可解释性** | 用户能否理解 AI 的决策逻辑？ | 贷款被拒但不知道原因 | 用户不信任 + 监管罚款 |
| **隐私性** | 训练数据是否侵犯用户隐私？ | 模型记忆了训练数据中的 PII | GDPR 罚款（全球营收 4%） |
| **安全性** | AI 能否被恶意利用？ | Prompt 注入、越狱攻击 | 安全事件 + 信任崩塌 |
| **问责性** | AI 出错时谁负责？ | 自动驾驶事故责任不清 | 法律真空 |
| **透明度** | 用户是否知道自己在和 AI 交互？ | Deepfake / AI 冒充人类 | 监管合规风险 |

> 🎯 **一句话总结 AI 伦理**：AI 产品要回答的不是"能不能做"，而是"该不该做"

## 2. 全球核心法规速览

### 2.1 法规对比

| 法规 | 地区 | 生效时间 | 核心要求 | 违规罚款 |
|------|------|:---:|------|------|
| **GDPR** | 欧盟 | 2018 | 数据保护+被遗忘权+可解释性 | 全球营收 4% / €2000万 |
| **AI Act** | 欧盟 | 2024-2026 分阶段 | 风险分级（不可接受/高/有限/低） | 全球营收 7% / €3500万 |
| **CCPA/CPRA** | 美国加州 | 2020/2023 | 消费者数据权利 | $7500/次故意违规 |
| **《生成式 AI 管理办法》** | 中国 | 2023 | 算法备案+内容标识+安全评估 | 最高 50 万元 |
| **《个人信息保护法》** | 中国 | 2021 | 个人信息处理规则 | 最高 5000 万元/营收 5% |

### 2.2 EU AI Act 风险分级

```text
EU AI Act 风险金字塔
│
├── 🔴 不可接受风险 — 禁止
│   ├── 社会信用评分
│   ├── 实时远程生物识别（公共场所）
│   └── 潜意识操纵
│
├── 🟠 高风险 — 严格监管
│   ├── 招聘/雇佣 AI
│   ├── 信贷/保险 AI
│   ├── 医疗诊断 AI
│   └── 司法辅助 AI
│   → 要求：风险评估 + 数据治理 + 人工监督 + 透明度
│
├── 🟡 有限风险 — 透明度义务
│   ├── 聊天机器人 → 需告知用户是 AI
│   ├── AI 生成内容 → 需标注 AI 生成
│   └── Deepfake → 需明确标注
│
└── 🟢 低/最小风险 — 无额外义务
    └── AI 推荐系统、垃圾邮件过滤器等
```

## 3. 可解释性实践

### 3.1 可解释性层次

```text
层次一：全局解释（这个模型整体怎么工作的）
  "本推荐系统基于你的浏览历史、购买记录和相似用户的偏好"

层次二：局部解释（这次为什么给出这个结果）
  "你被推荐此产品因为：① 你最近看过同类商品 ② 8 位相似用户购买了它"

层次三：反事实解释（什么条件改变会带来不同结果）
  "如果你的信用评分提高 50 分，贷款利率将降至 4.5%"
```

### 3.2 可解释性技术选型

| 技术 | 适用模型 | 原理 | 复杂度 |
|------|---------|------|:---:|
| **SHAP** | 任何模型 | 基于博弈论的 Shapley 值 | ⭐⭐⭐ |
| **LIME** | 任何模型 | 局部线性近似 | ⭐⭐ |
| **Attention Visualization** | Transformer | 可视化注意力权重 | ⭐⭐ |
| **Chain-of-Thought** | LLM | 展示推理步骤 | ⭐ |
| **Feature Importance** | 树模型 | 特征分裂增益 | ⭐ |
| **Rule Extraction** | 决策树 | 提取 if-then 规则 | ⭐ |

> 💡 **产品最佳实践**：对普通用户展示"局部解释"（这次为什么推荐这个）；对专家/监管展示"全局解释"（模型整体如何决策）

## 4. 公平性与偏见检测

### 4.1 常见偏见类型

| 偏见类型 | 示例 | 检测方法 |
|---------|------|---------|
| **历史偏见** | 招聘 AI 认为"护士=女性" | 分布检查（标签 vs 现实） |
| **代表性偏见** | 面部识别对深肤色准确率低 | 分群准确率对比 |
| **测量偏见** | 用邮政编码替代收入产生的偏差 | 代理变量审计 |
| **聚合偏见** | 通用模型在少数群体上表现差 | 分组评估 |

### 4.2 公平性指标

```python
class FairnessMetrics:
    """公平性度量工具"""

    @staticmethod
    def demographic_parity(predictions, sensitive_attr):
        """人口统计均等：不同群体获得正面结果的比例应相同"""
        groups = set(sensitive_attr)
        positive_rates = {}
        for g in groups:
            mask = [a == g for a in sensitive_attr]
            positive_rates[g] = sum(p for p, m in zip(predictions, mask) if m) / sum(mask)
        max_diff = max(positive_rates.values()) - min(positive_rates.values())
        return {
            "group_rates": positive_rates,
            "max_disparity": max_diff,
            "is_fair": max_diff < 0.10   # 差距 < 10% 视为可接受
        }

    @staticmethod
    def equal_opportunity(predictions, labels, sensitive_attr):
        """均等机会：真正例率（TPR）在不同群体间应相同"""
        groups = set(sensitive_attr)
        tprs = {}
        for g in groups:
            mask = [a == g for a in sensitive_attr]
            actual_pos = [l for l, m in zip(labels, mask) if m and l == 1]
            pred_pos = [p for p, l, m in zip(predictions, labels, mask)
                       if m and l == 1]
            tprs[g] = sum(pred_pos) / len(actual_pos) if actual_pos else 0
        return {
            "group_tpr": tprs,
            "max_disparity": max(tprs.values()) - min(tprs.values())
        }

    @staticmethod
    def bias_audit_report(model_name, predictions, labels, sensitive_attrs):
        """生成偏见审计报告"""
        report = {"model": model_name, "timestamp": datetime.now().isoformat()}
        for attr_name, attr_values in sensitive_attrs.items():
            report[attr_name] = {}
            report[attr_name]["demographic_parity"] = \
                FairnessMetrics.demographic_parity(predictions, attr_values)
            report[attr_name]["equal_opportunity"] = \
                FairnessMetrics.equal_opportunity(predictions, labels, attr_values)
        return report
```

## 5. 隐私保护技术方案

### 5.1 隐私保护技术栈

| 技术 | 原理 | 场景 | 成熟度 |
|------|------|------|:---:|
| **数据脱敏** | 替换/掩码 PII | 训练数据预处理 | ⭐⭐⭐⭐⭐ |
| **差分隐私** | 加噪使个体不可区分 | 模型训练 | ⭐⭐⭐⭐ |
| **联邦学习** | 数据不出本地，只传梯度 | 多方协作训练 | ⭐⭐⭐⭐ |
| **同态加密** | 加密数据上直接计算 | 高安全场景 | ⭐⭐⭐ |
| **安全飞地** | 硬件隔离环境（TEE） | 云端推理 | ⭐⭐⭐ |
| **K-匿名化** | 确保每组至少 K 个相似记录 | 数据发布 | ⭐⭐⭐⭐ |

### 5.2 最小权限数据收集原则

```text
问自己三个问题：
1. 这个数据是产品功能必需的吗？ → 不是就删
2. 用户明确知道我们在收集这个数据吗？ → 不知道就告知
3. 用户可以随时删除这个数据吗？ → 不能就做删除功能

隐私 = 信任 = 用户留存
调查显示：63% 的用户因为隐私问题放弃使用 AI 产品
```

## 6. 企业合规落地检查清单

### 6.1 上线前必检项

| # | 检查项 | 类别 | 通过标准 |
|:---:|------|------|------|
| 1 | 用户是否知晓在和 AI 交互？ | 透明度 | 首屏可见标注 |
| 2 | 输出是否标注为"AI 生成"？ | 透明度 | 所有 AI 输出 |
| 3 | 是否提供人工客服转接？ | 问责性 | 高风险场景必须有 |
| 4 | 训练数据是否完成 PII 扫描？ | 隐私性 | 0 个 PII 泄露 |
| 5 | 是否做了分群偏见测试？ | 公平性 | 差距 < 10% |
| 6 | 用户能否删除自己的数据？ | 隐私性 | 一键删除 |
| 7 | Prompt 注入是否有防御？ | 安全性 | 至少一层检测 |
| 8 | 是否完成算法备案？ | 合规性 | 按地区要求 |
| 9 | 是否记录了模型决策日志？ | 问责性 | 可追溯 90 天 |
| 10 | 是否有数据泄露应急预案？ | 安全性 | 文档化 + 演练 |

### 6.2 合规团队角色

| 角色 | 职责 | 建议配置 |
|------|------|:---:|
| AI 伦理官 | 制定伦理准则、监督合规 | 1 人 |
| 隐私工程师 | 数据脱敏、差分隐私实施 | 1 人 |
| 安全工程师 | Prompt 注入防御、红队测试 | 1-2 人 |
| 法务顾问 | 法规解读、合同审查 | 按需 |

## 核心要点回顾

- AI 伦理六维度：公平性、可解释性、隐私性、安全性、问责制、透明度
- EU AI Act 按风险四级分档：不可接受（禁）→ 高（严管）→ 有限（透明）→ 低（无义务）
- 可解释性三层：全局（模型咋工作）→ 局部（这次为啥）→ 反事实（怎样会不同）
- 偏见检测两指标：人口统计均等 + 均等机会
- 隐私五件套：脱敏 + 差分隐私 + 联邦学习 + 同态加密 + K-匿名
- 合规底线：告知 (AI交互) + 标注 (AI生成) + 可删除 (用户数据) + 可追溯 (决策日志)

## 参考资料

1. EU AI Act 全文 — https://artificialintelligenceact.eu
2. NIST AI Risk Management Framework
3. Google PAIR — Fairness Indicators
4. 中国《生成式人工智能服务管理暂行办法》
