# CodeGeeX 核心知识点

> **核心摘要**：CodeGeeX 是智谱 AI 推出的免费开源 AI 编程助手，支持超过 100 种编程语言。最大特点是完全免费、支持代码翻译、模型开源可私有化部署。

---

## 一、概述

CodeGeeX 是智谱 AI（GLM 大模型团队）推出的免费开源 AI 编程助手，支持超过 100 种编程语言。其最大特点是**完全免费**且支持**代码翻译**功能。

**核心定位**：免费、多语言、跨 IDE 的 AI 编程助手。

## 二、核心能力

| 功能 | 说明 |
|------|------|
| 代码自动补全 | 基于上下文的行内和多行代码续写 |
| 代码生成 | 根据注释或自然语言描述生成代码 |
| **代码翻译** | 将代码从一种语言翻译为另一种（核心差异功能） |
| 代码解释 | 用自然语言解释代码逻辑 |
| 注释生成 | 自动为代码块生成规范注释 |
| 智能问答 | 对话式解决编程问题 |

**支持语言**：主要语言 Java、Python、JavaScript/TypeScript、Go、C/C++、Rust、Kotlin；总计 100+ 种。

**支持 IDE**：VS Code、IntelliJ IDEA、PyCharm/GoLand、Jupyter Notebook。

## 三、核心功能详解

### 3.1 代码翻译（核心差异功能）

```java
// 输入 Java 代码
public List<String> filterUsers(List<User> users) {
    return users.stream()
        .filter(u -> u.getAge() > 18)
        .map(User::getName)
        .sorted()
        .limit(10)
        .collect(Collectors.toList());
}

// 翻译为 Python
def filter_users(users):
    return sorted(
        [user.name for user in users if user.age > 18]
    )[:10]
```

### 3.2 操作方式

| 操作 | VS Code | IntelliJ IDEA |
|------|---------|---------------|
| 触发补全 | Tab 接受建议 | Tab 接受建议 |
| 代码翻译 | 选中 → Ctrl+Shift+T | 选中 → 右键 → CodeGeeX 翻译 |
| 代码解释 | 选中 → 右键 → 解释代码 | 同上 |
| 智能问答 | Ctrl+Shift+G 唤起聊天 | 侧边栏面板 |

### 3.3 开源模型私有化部署

```bash
# 使用 Hugging Face 拉取模型
git clone https://huggingface.co/THUDM/codegeex4-all-9b

# 使用 vLLM 部署推理服务
python -m vllm.entrypoints.openai.api_server \
    --model codegeex4-all-9b --port 8000
```

## 四、与其他工具对比

| 维度 | CodeGeeX | GitHub Copilot | 通义灵码 |
|------|----------|----------------|----------|
| 价格 | 完全免费 | 付费订阅 | 免费 |
| 代码补全 | 中等偏上 | 最强 | 高 |
| 代码翻译 | 支持 | 不支持 | 部分支持 |
| 开源 | 模型开源 | 闭源 | 闭源 |
| 语言支持 | 100+ | ~20 种主力语言 | ~20 种 |
| 私有化部署 | 支持 | 不支持 | 不支持 |
| 国内访问 | 低延迟 | 需稳定网络 | 低延迟 |

## 五、适用场景

1. **预算敏感的个人开发者**：完全免费，功能满足日常需求
2. **多语言项目维护**：前后端分离项目统一使用一个助手
3. **代码翻译需求**：技术栈迁移时提供参考
4. **私有化部署**：企业内网环境需要本地化 AI 编程服务的场景

## 核心要点回顾

- CodeGeeX 的核心竞争力是完全免费 + 代码翻译 + 模型开源
- 代码补全质量略低于 Copilot，但在免费工具中处于领先水平
- 代码翻译功能是区别于其他工具的独特优势
- 支持私有化部署，适合有数据安全需求的企业

## 参考资料

1. CodeGeeX 官方文档 - codegeex.cn
2. Hugging Face 模型库 - THUDM/codegeex4-all-9b
3. vLLM 官方文档 - 模型推理部署
