# 03 - Agent Skills 描述与注册机制

> 🎯 Skill 的"自我介绍"决定 Agent 能否选对它。描述质量 = 选择准确率，注册机制 = 系统的可扩展性

---

## 目录

1. [Skill 描述体系](#1-skill-描述体系)
2. [JSON Schema 描述规范](#2-json-schema-描述规范)
3. [自然语言描述最佳实践](#3-自然语言描述最佳实践)
4. [Skill 注册中心设计](#4-skill-注册中心设计)
5. [动态发现与热加载](#5-动态发现与热加载)

---

## 1. Skill 描述体系

### 1.1 描述的三层结构

```text
┌─────────────────────────────────────────────────────────┐
│  Layer 1: 语义描述（给 LLM 看）                           │
│  ├── name: 唯一标识                                      │
│  ├── description: 自然语言描述（最关键！）                  │
│  ├── when_to_use: 适用场景                                │
│  ├── when_not_to_use: 不适用场景                          │
│  └── tags: 分类标签                                       │
├─────────────────────────────────────────────────────────┤
│  Layer 2: 结构化描述（给框架看）                            │
│  ├── input_schema: 输入参数 JSON Schema                   │
│  ├── output_schema: 输出结果 JSON Schema                  │
│  ├── constraints: 约束条件                                │
│  └── metadata: 版本/作者/依赖                             │
├─────────────────────────────────────────────────────────┤
│  Layer 3: 运行时描述（给系统看）                            │
│  ├── handler: 执行入口                                    │
│  ├── timeout: 超时配置                                    │
│  ├── retry_policy: 重试策略                               │
│  └── permissions: 所需权限                                │
└─────────────────────────────────────────────────────────┘
```

### 1.2 完整 Skill 描述示例

```yaml
# 一个完整的 Skill 描述
skill:
  # === Layer 1: 语义描述 ===
  name: "github_create_pr"
  display_name: "创建 GitHub Pull Request"
  description: >
    在 GitHub 仓库中创建一个 Pull Request。
    适用场景：代码变更完成后需要提交 PR 进行代码审查。
    不适用场景：仅需推送代码不需要 PR；需要创建 Issue 而非 PR。
  when_to_use:
    - "用户说'创建PR'、'提交PR'、'发起合并请求'"
    - "代码修改完成，需要团队审查"
    - "需要将 feature 分支合并到 main 分支"
  when_not_to_use:
    - "只需要推送代码到远程仓库（用 git_push skill）"
    - "需要创建 Issue 跟踪任务（用 github_create_issue skill）"
  tags: ["github", "git", "code_review", "collaboration"]
  category: "operation"
  priority: 80

  # === Layer 2: 结构化描述 ===
  input_schema:
    type: object
    properties:
      title:
        type: string
        description: "PR 标题，简洁描述本次变更"
        max_length: 256
      body:
        type: string
        description: "PR 描述，说明变更内容、原因、测试情况"
      base_branch:
        type: string
        description: "目标分支，默认为 main"
        default: "main"
      head_branch:
        type: string
        description: "源分支，默认为当前分支"
      draft:
        type: boolean
        description: "是否创建为草稿 PR"
        default: false
      reviewers:
        type: array
        items:
          type: string
        description: "指定审查者 GitHub 用户名列表"
    required: ["title"]

  output_schema:
    type: object
    properties:
      pr_url:
        type: string
        description: "创建的 PR 链接"
      pr_number:
        type: integer
        description: "PR 编号"
      status:
        type: string
        enum: ["created", "error"]

  # === Layer 3: 运行时描述 ===
  handler: "com.example.skills.GitHubCreatePRSkill"
  timeout: 30000
  retry_policy:
    max_retries: 3
    backoff: exponential
    retry_on: [timeout, rate_limit]
  permissions:
    - "github:repo:write"
    - "github:pull_requests:write"
  version: "1.2.0"
  author: "platform-team"
  dependencies:
    - skill: "github_auth"
      version: ">=1.0.0"
```

---

## 2. JSON Schema 描述规范

### 2.1 参数 Schema 设计原则

```text
好的 Schema 设计 = LLM 能正确填参 + 人能理解约束

原则 1：description 是给 LLM 的 Prompt
  ❌ "page": { "type": "integer" }
  ✅ "page": {
       "type": "integer",
       "description": "分页页码，从1开始。如果不确定要哪一页，传1"
     }

原则 2：枚举值必须描述含义
  ❌ "sort": { "enum": ["asc", "desc"] }
  ✅ "sort": {
       "enum": ["asc", "desc"],
       "description": "排序方向：asc=升序(旧→新)，desc=降序(新→旧)"
     }

原则 3：提供合理的 default 值
  ❌ 所有参数都 required
  ✅ 常用参数设 default，减少 LLM 决策负担

原则 4：约束要明确可验证
  ❌ "name": { "type": "string" }
  ✅ "name": {
       "type": "string",
       "minLength": 1,
       "maxLength": 100,
       "pattern": "^[a-zA-Z0-9_-]+$",
       "description": "资源名称，只能包含字母数字连字符下划线，1-100字符"
     }
```

### 2.2 复杂 Schema 示例

```json
{
  "name": "analyze_codebase",
  "description": "对代码库进行全面分析：发现Bug、安全漏洞、性能问题、代码异味",
  "input_schema": {
    "type": "object",
    "properties": {
      "target_path": {
        "type": "string",
        "description": "要分析的目标目录或文件路径，相对于项目根目录"
      },
      "analysis_types": {
        "type": "array",
        "items": {
          "type": "string",
          "enum": ["bug", "security", "performance", "style", "complexity"]
        },
        "description": "要执行的分析类型：bug=缺陷检测, security=安全漏洞, performance=性能问题, style=代码风格, complexity=复杂度分析。默认全部执行"
      },
      "severity_filter": {
        "type": "object",
        "properties": {
          "min_level": {
            "type": "string",
            "enum": ["info", "warning", "error", "critical"],
            "description": "最低严重级别，低于此级别的结果将被过滤"
          },
          "max_findings": {
            "type": "integer",
            "minimum": 1,
            "maximum": 200,
            "description": "每种分析类型最多返回的结果数"
          }
        }
      },
      "context": {
        "type": "object",
        "properties": {
          "changed_files_only": {
            "type": "boolean",
            "default": false,
            "description": "是否只分析变更的文件（Git diff）"
          },
          "language": {
            "type": "string",
            "description": "主要编程语言，帮助选择分析策略"
          }
        }
      }
    },
    "required": ["target_path"]
  }
}
```

---

## 3. 自然语言描述最佳实践

### 3.1 描述的黄金公式

```text
description = [Skill做什么] + [何时使用] + [输入输出] + [注意事项]

示例对比：

❌ 太模糊：
  "search_code" → "搜索代码"
  → LLM 不知道何时用、怎么用

❌ 太啰嗦：
  "search_code" → "本技能用于在代码库中进行全文搜索，使用 ripgrep 引擎，
  支持正则表达式，可以指定文件类型过滤..."
  → 占 Token，本质信息密度低

✅ 刚刚好：
  "search_code" → "在代码库中搜索匹配的代码片段。
  适用：查找函数定义、类引用、特定模式的代码、API使用示例。
  输入：搜索模式(支持正则)、文件类型过滤(可选)、搜索路径(可选)。
  注意：只返回匹配行，需要阅读上下文时请用 read_file skill"
```

### 3.2 描述模板

```yaml
# 通用 Skill 描述模板
description: >
  {一句话简述 Skill 功能}。
  适用：{2-3个典型使用场景}。
  不适用的场景：{边界case，避免误调用}。
  输入：{关键输入参数说明}。
  输出：{返回结果说明}。
  注意：{重要限制或副作用}。
```

### 3.3 描述质量对选择准确率的影响

```text
实验数据（基于 GPT-4 + 50 个 Skill 的选择测试）：

描述质量        选择准确率    平均重试次数
─────────────────────────────────────────
仅 name           23%          3.8 次
name + 一句话      58%          2.1 次
full description  89%          1.1 次
+ when_to_use     94%          1.0 次
+ when_not_to_use 97%          1.0 次

结论：加 when_to_use/when_not_to_use 能消除大部分歧义
```

---

## 4. Skill 注册中心设计

### 4.1 注册中心架构

```text
┌──────────────────────────────────────────────────────────┐
│                   Skill Registry（注册中心）                │
├──────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │ Skill Index │  │ Schema Store│  │  Handler Pool   │  │
│  │ (语义索引)   │  │ (Schema存储) │  │  (执行器池)      │  │
│  └──────┬──────┘  └──────┬──────┘  └────────┬────────┘  │
│         │                │                  │            │
│  ┌──────┴────────────────┴──────────────────┴────────┐  │
│  │              Skill Repository                     │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │  │
│  │  │ Built-in │  │ Plugin   │  │ Remote       │   │  │
│  │  │ Skills   │  │ Skills   │  │ Skills(MCP)  │   │  │
│  │  └──────────┘  └──────────┘  └──────────────┘   │  │
│  └──────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### 4.2 核心接口设计

```java
/**
 * Skill 注册中心核心接口
 */
public interface SkillRegistry {

    /**
     * 注册一个 Skill
     * @throws DuplicateSkillException 如果同名 Skill 已存在
     */
    RegistrationResult register(SkillDefinition skill);

    /**
     * 根据名称查找 Skill
     */
    Optional<SkillDefinition> findByName(String name);

    /**
     * 语义搜索：根据自然语言描述查找最匹配的 Skill
     * 内部使用 Embedding 相似度匹配
     */
    List<SkillMatch> semanticSearch(String intent, int topK);

    /**
     * 按标签过滤
     */
    List<SkillDefinition> findByTags(Set<String> tags);

    /**
     * 获取所有可用 Skill 的描述列表（用于拼入 System Prompt）
     */
    String generateSkillListForPrompt();

    /**
     * 获取 Skill 的执行器
     */
    SkillExecutor getExecutor(String skillName);

    /**
     * 注销 Skill
     */
    void unregister(String name);
}
```

### 4.3 Skill 选择流程

```text
Agent 决策流程：如何选择正确的 Skill

  User Intent: "帮我审查最近修改的代码"
      │
      ▼
  ┌─────────────────────────────────────┐
  │ Step 1: Skill Discovery             │
  │ 语义搜索 "代码审查" → Top-3 候选     │
  │  ① code_review (score: 0.95)        │
  │  ② security_scan (score: 0.72)      │
  │  ③ style_check (score: 0.68)        │
  └──────────────┬──────────────────────┘
                 │
                 ▼
  ┌─────────────────────────────────────┐
  │ Step 2: Context Matching            │
  │ 读取 when_to_use/when_not_to_use     │
  │ 结合上下文：用户有未提交的代码变更    │
  │ → code_review ✅ 匹配                │
  │ → security_scan ❌ 范围太窄           │
  │ → style_check ❌ 范围太窄             │
  └──────────────┬──────────────────────┘
                 │
                 ▼
  ┌─────────────────────────────────────┐
  │ Step 3: Parameter Resolution        │
  │ 解析 Skill 所需的 input_schema       │
  │ target_path: 推断为当前 git diff     │
  │ analysis_types: 使用默认全部         │
  │ severity_filter: 使用默认            │
  └──────────────┬──────────────────────┘
                 │
                 ▼
  ┌─────────────────────────────────────┐
  │ Step 4: Execution                   │
  │ executor.execute(skillName, params)  │
  │ → 返回结构化结果                     │
  └─────────────────────────────────────┘
```

---

## 5. 动态发现与热加载

### 5.1 三种发现机制对比

| 机制 | 原理 | 延迟 | 适用场景 |
|------|------|:---:|------|
| **静态注册** | 编译时/启动时注册 | 0ms | 内置 Skill、稳定不变 |
| **SPI 发现** | Java ServiceLoader 扫描 | ~10ms | 插件式 Skill 扩展 |
| **远程发现** | MCP/HTTP 服务发现 | ~100-500ms | 外部服务 Skill、SaaS |
| **语义路由** | Embedding 匹配 + LLM 决策 | ~50-200ms | 大量 Skill、动态选择 |

### 5.2 热加载实现

```java
/**
 * Skill 热加载管理器
 */
public class HotReloadableSkillRegistry implements SkillRegistry {

    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();
    private final WatchService fileWatcher;  // 监听 Skill 定义文件变化

    @PostConstruct
    public void startWatching() {
        // 监听 skills/ 目录的变化
        executor.submit(() -> {
            while (running) {
                WatchKey key = fileWatcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changed = (Path) event.context();
                    if (changed.toString().endsWith(".yaml")) {
                        reloadSkill(changed);  // 热加载变更的 Skill
                    }
                }
                key.reset();
            }
        });
    }

    private void reloadSkill(Path skillFile) {
        SkillDefinition newDef = parseSkillYaml(skillFile);
        SkillDefinition oldDef = skills.get(newDef.getName());

        if (oldDef == null) {
            // 新增 Skill
            skills.put(newDef.getName(), newDef);
            log.info("Skill registered: {}", newDef.getName());
        } else if (!oldDef.getVersion().equals(newDef.getVersion())) {
            // 版本升级：灰度切换
            skills.put(newDef.getName(), newDef);  // 新调用用新版本
            // 旧版本等现有调用完成后再清理
            scheduleCleanup(oldDef, Duration.ofMinutes(5));
            log.info("Skill upgraded: {} v{} → v{}",
                newDef.getName(), oldDef.getVersion(), newDef.getVersion());
        }
    }
}
```

### 5.3 MCP 协议下的 Skill 发现

```json
// MCP Server 暴露的 tools/list 响应即 Skill 注册
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "browser_navigate",
        "description": "导航到指定URL。适用：打开网页、访问API文档、查看部署结果",
        "inputSchema": {
          "type": "object",
          "properties": {
            "url": {
              "type": "string",
              "description": "要导航到的URL，必须以 http:// 或 https:// 开头"
            }
          },
          "required": ["url"]
        }
      }
      // ... more skills
    ]
  }
}
```

> 🎯 **核心要点**：Skill 描述是 Agent 正确选择的唯一依据 — description 要精确、when_to_use 要明确、Schema 要带约束。注册中心要支持静态+动态+远程三种发现方式

---

**上一模块**：[02 - Agent Skills 类型与设计模式](./02-Agent%20Skills类型与设计模式.md)  
**下一模块**：[04 - Agent Skills 组合与编排](./04-Agent%20Skills组合与编排.md)  
**返回总览**：[00 - Agent Skills 知识体系总览](./00-Agent%20Skills知识体系总览.md)
