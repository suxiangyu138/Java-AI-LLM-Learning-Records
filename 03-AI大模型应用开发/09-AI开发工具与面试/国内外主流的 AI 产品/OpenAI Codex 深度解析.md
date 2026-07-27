# OpenAI Codex 深度解析（2026最新）

> **定位**：ChatGPT内置编程Agent模式，GPT-5-Codex专用代码微调分支，能自主拆解大型需求→修改多个关联文件→运行测试→排查报错→提交PR的完整自主工程Agent。2026年7月独立桌面客户端已下架，能力全面整合进ChatGPT。

---

## 目录

1. [Codex两个阶段：初代 vs 新版](#1-codex两个阶段初代-vs-新版)
2. [为什么Codex不常被单独列出](#2-为什么codex不常被单独列出)
3. [Codex核心能力详解](#3-codex核心能力详解)
4. [Codex vs GitHub Copilot vs Cursor vs Claude Code](#4-codex-vs-github-copilot-vs-cursor-vs-claude-code)
5. [Codex的典型工作模式](#5-codex的典型工作模式)
6. [2026年7月重大变化](#6-2026年7月重大变化)
7. [适用场景与选型建议](#7-适用场景与选型建议)
8. [Java开发者使用指南](#8-java开发者使用指南)

---

## 1. Codex两个阶段：初代 vs 新版

> ⚠️ **关键认知**：网上大部分Codex教程讲的是2021年老模型，已在2023年停用。当前Codex是完全不同的产品。

### 1.1 两个阶段对比

| 维度 | 初代Codex（2021–2023） | 新版Codex（2025–2026） |
|------|----------------------|---------------------|
| **状态** | ❌ 2023年已停用 | ✅ 当前可用 |
| **产品定位** | 代码专用底层大模型 | 独立AI编程Agent产品 |
| **底层模型** | 基于GPT-3微调 | GPT-5-Codex专用代码微调分支 |
| **产品形态** | 仅开放API（`code-davinci-002`） | ChatGPT桌面端内置「Codex模式」 |
| **获取方式** | 第三方工具间接调用 | ChatGPT Plus/Pro/企业版内解锁 |
| **核心能力** | 单次代码生成 | 全项目自动化：多文件重构→跑测试→提交PR |
| **独立客户端** | ❌ 无 | 曾有，2026年7月已下架合并 |
| **典型用户** | API开发者 | 需要完整项目自动化的工程师 |

### 1.2 初代Codex的遗产

```
初代Codex做了什么（已成为历史）：
- 2021年发布，是GitHub Copilot的第一代核心引擎
- 开放API code-davinci-002，但普通开发者不能直接交互
- 2023年OpenAI停用该API，代码能力整合进GPT-4
- 现在调用code-davinci-002会直接报错

初代Codex的意义：
- 证明了"代码专用模型"的可行性
- 孵化了GitHub Copilot
- 为2025年新版Codex Agent积累了经验
```

---

## 2. 为什么Codex不常被单独列出

### 2.1 三个原因

**原因一：不再是独立产品**
```
2026年7月之前：Codex有独立桌面客户端，可单独下载安装
2026年7月之后：独立客户端下架，Codex变成ChatGPT内的功能模式
→ 不再和Cursor、Copilot、Claude Code等并列成独立工具
```

**原因二：普通开发者接触更多的是Copilot**
```
95%开发者的AI编程需求：
  IDE内实时代码补全 → Copilot（刚需，高频）

5%开发者的AI编程需求：
  完整项目自动化 → Codex（高阶，低频）
  
→ Codex受众远小于Copilot，自然讨论更少
```

**原因三：初代Codex已死，认知混淆**
```
搜索"OpenAI Codex教程"：
  → 90%是2021-2023年的过时内容
  → 讲的是code-davinci-002（已停用）
  → 很多教程仍然建议去调用这个不存在的API
  
→ OpenAI刻意淡化Codex品牌，避免和"已死的初代"混淆
```

### 2.2 Codex当前的"存在形式"

```
现在（2026.07）获取Codex的方式：

1. ChatGPT桌面客户端 → 切换到Codex模式
2. ChatGPT网页端 → 选择Codex模式（有限功能）
3. ChatGPT CLI → 命令行调用Codex
4. GitHub Copilot Chat → 选择Codex模型（微软集成）
5. API → 通过Assistants API使用code-interpreter

注意：没有任何一个入口叫"OpenAI Codex App"，
      Codex已经不是一个独立产品。
```

---

## 3. Codex核心能力详解

### 3.1 自主工程Agent

> 🎯 **Codex和普通AI编程的本质区别**：不是"帮我写一段代码"，而是"帮我完成这个需求"。

```
普通AI编程（Copilot/ChatGPT对话）：
  用户："帮我写一个登录接口"
  AI：输出一段AuthController代码
  用户：自己创建文件、配置依赖、运行测试、调试...

Codex模式（自主Agent）：
  用户："给这个项目加用户登录功能"
  Codex自主执行：
  ├── 1. 读取项目结构，理解现有架构
  ├── 2. 识别需要修改/新增的文件
  ├── 3. 逐个文件生成/修改代码
  ├── 4. 添加Maven/Gradle依赖
  ├── 5. 运行 mvn test 检查是否通过
  ├── 6. 如果测试失败 → 自动分析报错 → 修复
  ├── 7. 生成API文档
  └── 8. git add + git commit + 生成commit message
```

### 3.2 核心能力列表

| 能力 | 说明 | 评级 |
|------|------|------|
| **项目理解** | 读取完整仓库，理解架构和依赖 | ⭐⭐⭐⭐⭐ |
| **多文件修改** | 同时修改/创建多个关联文件 | ⭐⭐⭐⭐⭐ |
| **自动测试** | 生成测试→运行→失败→修复→通过 | ⭐⭐⭐⭐⭐ |
| **Git操作** | 自动add/commit/创建PR | ⭐⭐⭐⭐⭐ |
| **终端执行** | 运行命令、检查输出、处理错误 | ⭐⭐⭐⭐⭐ |
| **代码质量** | 基于GPT-5-Codex分支，质量高 | ⭐⭐⭐⭐ |
| **多语言** | 和GPT-5.6相同的语言覆盖 | ⭐⭐⭐⭐⭐ |
| **上下文长度** | 256K token（Codex版本） | ⭐⭐⭐⭐⭐ |

### 3.3 云端沙箱执行

| 特性 | 说明 |
|------|------|
| **沙箱环境** | Codex在云端Linux沙箱中执行代码 |
| **安全隔离** | 和本地环境隔离，不会搞坏电脑 |
| **环境预置** | Python/Node.js/Java/Git等常见工具预装 |
| **结果同步** | 沙箱执行结果同步反映到本地项目 |
| **适用场景** | 运行测试、安装依赖、构建项目 |

---

## 4. Codex vs GitHub Copilot vs Cursor vs Claude Code

### 4.1 核心理念区别

```
GitHub Copilot：
  "我帮你写当前这行代码"
  → 实时代码补全，人写代码AI辅助
  
Cursor：
  "我帮你修改/生成一批文件"
  → Composer批量重构，人在IDE中用AI

Claude Code：
  "我帮你完成整个开发任务"
  → 终端自主Agent，人在旁边监督

OpenAI Codex：
  "我帮你完成整个项目工程"
  → 桌面/云端Agent，工程项目全流程自动化
```

### 4.2 详细对比表

| 维度 | Codex | Copilot X | Cursor | Claude Code |
|------|-------|-----------|--------|-------------|
| **产品归属** | OpenAI | 微软+GitHub | Anysphere | Anthropic |
| **产品形态** | ChatGPT内置模式 | IDE插件 | 独立IDE | CLI终端 |
| **实时补全** | ❌ 无 | 🥇 最流畅 | ⭐⭐⭐⭐ | ❌ 无 |
| **多文件重构** | 🥇 完整Agent | ⭐⭐ Agent模式 | 🥇 Composer | 🥇 自主执行 |
| **自动运行测试** | ✅ 沙箱执行 | ❌ | ⚠️ 有限 | ✅ 终端执行 |
| **自动提交PR** | ✅ | ⭐⭐⭐ | ❌ | ✅ |
| **云端执行** | ✅ 沙箱 | ❌ | ❌ | ❌ 纯本地 |
| **上下文** | 256K token | 局部 | 全局项目 | 全局项目 |
| **模型** | GPT-5-Codex | GPT/Claude/Gemini | GPT+Claude | Claude系列 |
| **付费** | 绑定ChatGPT订阅 | $10/月 | $20/月 | API按量 |

### 4.3 选型快速判断

```
日常写代码，需要IDE里实时补全 → Copilot（$10/月）
需要AI原生IDE + Composer批量重构 → Cursor（$20/月）
需要终端自主开发 + 最强代码质量 → Claude Code（API按量）
需要完整项目工程自动化 + 云端沙箱 → Codex（ChatGPT订阅内含）

国内开发者推荐：
  日常补全 → MarsCode（免费）
  企业项目 → 通义灵码（代码评审）
  复杂重构 → MarsCode跨文件修改（免费）
  不考虑Codex（ChatGPT需翻墙+付费）
```

---

## 5. Codex的典型工作模式

### 5.1 工作流程图

```
Codex自主工程流程：

1. 接收需求
   ↓
2. 读取项目 → 分析现有代码 → 理解架构
   ↓
3. 制定计划 → 列出要修改/新增的文件清单
   ↓
4. 逐文件执行
   ├── 创建新文件 → 写代码
   ├── 修改现有文件 → 保持一致性
   └── 更新配置文件
   ↓
5. 验证阶段
   ├── 运行测试 → 通过 ✅
   ├── 运行测试 → 失败 ❌ → 分析报错 → 修复 → 重跑
   └── 运行linter → 通过
   ↓
6. 输出阶段
   ├── 生成API文档
   ├── 更新README
   └── Git提交 + 生成commit message
   ↓
7. 人工审查 → 确认/调整
```

### 5.2 典型使用示例

```
场景：为一个Spring Boot项目添加完整的REST API缓存层

用户输入：
"给这个项目加Redis缓存：
1. 所有GET接口加缓存（1小时过期）
2. POST/PUT/DELETE自动清除相关缓存
3. 使用Spring Cache注解+Redis
4. 配置Redis连接池
5. 添加缓存预热逻辑
6. 编写缓存集成测试"

Codex自主执行：
├── 读取项目→确认是Spring Boot+Spring Data JPA项目
├── 修改pom.xml→添加spring-boot-starter-cache+redis依赖
├── 创建CacheConfig.java→Redis配置+序列化+Caffeine本地缓存
├── 修改所有Service类→添加@Cacheable/@CacheEvict注解
├── 修改application.yml→Redis连接配置
├── 创建CacheWarmup.java→启动时预热热点数据
├── 创建CacheIntegrationTest.java→验证缓存生效
├── 运行mvn test→4个测试失败→分析原因→修复→全部通过
├── 生成CACHE.md→缓存策略说明文档
└── git commit -m "feat: Add Redis caching layer"

整个过程用户只需要审查最终代码，不需要手动写一行！
```

---

## 6. 2026年7月重大变化

### 6.1 变化内容

| 变化 | 之前 | 之后 |
|------|------|------|
| **独立客户端** | 可单独下载Codex桌面App | ❌ 已下架 |
| **入口** | Codex App + ChatGPT App | 统一为ChatGPT桌面端 |
| **品牌** | "OpenAI Codex"独立品牌 | "ChatGPT Codex模式" |
| **功能** | 编程专用 | 嵌入ChatGPT，和对话/Agent融合 |
| **模型选择** | 固定Codex模型 | 自动选择最佳模型 |

### 6.2 变化对用户的影响

```
✅ 好处：
├── 一个ChatGPT客户端搞定所有（对话+编程+Agent）
├── 不需要切换App
└── 和ChatGPT Pro/Plus订阅统一，不需要额外付费

❌ 坏处：
├── Codex品牌弱化，更难被发现
├── 编程功能藏在ChatGPT里，入口不够直接
└── 独立App的一些特有功能可能被简化
```

---

## 7. 适用场景与选型建议

### 7.1 Codex最适合的场景

| 场景 | 适合度 | 说明 |
|------|--------|------|
| **完整模块开发** | 🥇 最佳 | "加一个订单模块"→自主完成所有文件 |
| **大型项目重构** | 🥇 最佳 | "把所有Date换成LocalDateTime"→自动处理50+文件 |
| **自动化测试** | 🥇 最佳 | 生成→运行→失败→修复→通过的完整循环 |
| **项目脚手架** | 🥇 最佳 | "创建一个微服务项目"→完整项目骨架 |
| **CI/CD集成** | 🥇 最佳 | 自动PR、自动修复CI失败 |
| **日常补全** | ❌ 不适合 | 用Copilot |
| **IDE内编辑** | ❌ 不适合 | 用Cursor |
| **轻量问答** | ❌ 不适合 | 用ChatGPT普通模式 |

### 7.2 谁适合用Codex

```
✅ 适合：
├── 有ChatGPT Plus/Pro订阅的开发者
├── 需要处理大型、多文件编程任务的工程师
├── 希望AI能"跑起来"（不只是生成代码，还能跑测试验证）
├── 技术Leader需要快速搭建项目原型
└── 在GitHub上做开源项目的维护者

❌ 不适合：
├── 只需要IDE内实时补全（用Copilot/MarsCode就行）
├── 国内网络受限的开发者（需要翻墙+ChatGPT订阅）
├── 数据敏感的政企项目（代码上传海外服务器）
└── 轻度编程需求（直接用ChatGPT普通模式就够）
```

---

## 8. Java开发者使用指南

### 8.1 获取Codex能力

```
前提条件：
1. 有ChatGPT Plus（$20/月）或 Pro（$200/月）订阅
2. 下载ChatGPT桌面客户端
3. 在桌面端中切换到Codex模式
4. 打开你的Java项目文件夹 → 授权ChatGPT访问

使用方式：
- 桌面端：打开项目→切换到Codex模式→描述需求
- CLI：chatgpt codex "给UserService加缓存"（命令行方式）
- API：通过Assistants API + code_interpreter工具
```

### 8.2 Codex模式下高效Prompt

```
✅ 好的Codex需求描述（具体、有边界、有验收标准）：

"在com.example.demo.user包下完成以下任务：
1. 为UserService添加分页查询方法
   - 方法签名：Page<UserDTO> findUsers(Pageable pageable, UserFilter filter)
   - 使用Spring Data JPA的Specification动态查询
   - UserFilter包含：username模糊匹配、email精确匹配、enabled布尔过滤
   - 返回的Page使用MapStruct转换为UserDTO
2. 创建UserFilter类（包含上述三个字段）
3. 创建UserSpecification类（构建动态查询条件）
4. 在UserController添加 GET /api/users 分页查询端点
5. 编写UserService.findUsers的完整单元测试
6. 编写UserController.findUsers的MockMvc集成测试
7. 所有测试必须通过 mvn test -pl user-module"

❌ 差的Codex需求描述（太模糊，AI容易出错）：

"帮我加个分页功能"
```

### 8.3 API方式调用Codex

```java
// 通过OpenAI Assistants API使用Codex能力
// 这需要ChatGPT订阅和API Key

import com.openai.client.OpenAIClient;
import com.openai.models.beta.assistants.*;

// 创建Codex助手
Assistant assistant = client.beta().assistants().create(
    AssistantCreateParams.builder()
        .model("gpt-5-codex")
        .name("Java项目代码审查助手")
        .instructions("""
            你是一个Java项目代码审查专家，具备以下能力：
            1. 读取完整代码库并理解架构
            2. 多文件代码修改，保持一致性
            3. 运行Maven/Gradle测试验证
            4. 自动生成测试代码
            """)
        .tools(List.of(
            Tool.ofFileSearch(FileSearchTool.builder().build()),
            Tool.ofCodeInterpreter(CodeInterpreterTool.builder().build())
        ))
        .build()
);

// 创建Thread并发送任务
Thread thread = client.beta().threads().create();
client.beta().threads().messages().create(
    thread.id(),
    MessageCreateParams.builder()
        .content("审查com.example.demo.user包下的代码，给出改进建议")
        .build()
);

// 运行助手
Run run = client.beta().threads().runs().create(
    thread.id(),
    RunCreateParams.builder()
        .assistantId(assistant.id())
        .build()
);

// 轮询获取结果...
```

---

## 核心要点回顾

- **Codex ≠ 一个独立产品**：当前Codex是ChatGPT内置的编程Agent模式，不复存在独立客户端
- **初代Codex已死**：2021年的code-davinci-002在2023年已停用，网上老教程全部过时
- **新版Codex是什么**：GPT-5-Codex分支驱动的自主工程Agent，能读项目→写代码→跑测试→修复→提交PR
- **vs Copilot**：Copilot是IDE内实时补全（写代码辅助），Codex是全项目自主工程（完成需求代理）
- **vs Cursor**：Cursor是AI原生IDE（Composer批量重构），Codex是桌面/云端Agent（全流程自动化）
- **vs Claude Code**：都是自主Agent，Codex有云端沙箱执行，Claude Code是终端本地执行
- **获取方式**：ChatGPT Plus/Pro订阅 → 桌面端 → Codex模式（无单独客户端，无单独订阅）
- **国内开发者**：建议用MarsCode + 通义灵码替代，Codex需要翻墙+ChatGPT付费

---

## 参考资料

1. OpenAI Codex - https://openai.com/index/openai-codex/
2. OpenAI Assistants API - https://platform.openai.com/docs/assistants
3. ChatGPT桌面端 - https://openai.com/chatgpt/desktop/
4. OpenAI API文档 - https://platform.openai.com/docs
5. Codex vs GitHub Copilot - https://github.com/features/copilot
