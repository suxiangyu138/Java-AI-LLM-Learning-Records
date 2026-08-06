# SonarLint 核心知识点

## 一、概述

SonarLint 是 SonarSource 推出的 IDE 实时代码质量检查插件，相当于在你写代码时有一位"代码评审专家"持续检查代码异味、Bug 隐患和安全漏洞。它可看作本地版 SonarQube，不需要 SonarQube 服务器即可独立运行。

**核心定位：** IDE 内的实时代码质量守卫——边写边检查，问题消灭在编码阶段。

**官网：** https://www.sonarsource.com/products/sonarlint/

## 二、核心能力

### 2.1 检查维度

| 分类 | 检查内容 |
|------|----------|
| **Bug 隐患** | 空指针、资源泄露、死代码、无限循环 |
| **代码异味** | 复杂方法、重复代码、命名不规范、过长参数列表 |
| **安全漏洞** | SQL 注入、XSS、路径遍历、硬编码密码 |
| **性能问题** | 不合理的集合操作、字符串拼接、N+1 查询 |

### 2.2 问题严重等级

| 等级 | 说明 | 处理建议 |
|------|------|----------|
| **Blocker（阻塞）** | 极可能在生产环境导致严重问题 | 必须立即修复 |
| **Critical（严重）** | 可能影响程序行为或安全 | 强烈建议修复 |
| **Major（主要）** | 显著降低代码可维护性 | 建议修复 |
| **Minor（次要）** | 轻微质量缺陷 | 可择机修复 |
| **Info（信息）** | 仅供参考 | 了解即可 |

### 2.3 支持的 IDE

| IDE | 安装方式 |
|-----|----------|
| **IntelliJ IDEA** | 插件市场搜索 "SonarLint" |
| **VS Code** | 扩展商店搜索 "SonarLint" |
| **Eclipse** | Eclipse Marketplace 安装 |
| **Visual Studio** | VS Marketplace（.NET 语言） |

## 三、功能详解

### 3.1 实时检查

```java
// SonarLint 会在以下代码上标记问题：
public class BadExample {

    // ⚠️ Hard-coded credentials are security-sensitive
    private static final String DB_PASSWORD = "admin123";

    // ⚠️ A "NullPointerException" could be thrown
    public String getUpperName(User user) {
        return user.getName().toUpperCase();
    }

    // ⚠️ Use isEmpty() instead of size() == 0
    public boolean isEmpty(List<String> list) {
        return list.size() == 0;
    }

    // ⚠️ Remove useless curly braces around statement
    public void doWork() {
        if (true) {
            process();
        }
    }
}
```

### 3.2 问题查看与修复

- 代码行右侧 **高亮标记**（黄色=异味，红色=严重）
- 点击灯泡 → 查看问题描述 + 规则详情
- SonarLint 面板：按文件/规则/严重等级查看所有问题
- 快速修复：部分问题提供一键修复

### 3.3 连接 SonarQube / SonarCloud

```
SonarLint (IDE) ←→ SonarQube Server（团队统一规则）
                     ├── 质量阀（Quality Gate）
                     ├── 历史趋势
                     └── 新代码检测
```

**连接方式：** SonarLint 面板 → Connected Mode → 输入 SonarQube URL + Token

**Connected Mode 优势：**
- 远端禁用的规则本地同步禁用
- 查看项目级问题的历史数据
- 新代码周期（New Code Period）内的问题重点标记

### 3.4 规则配置

```
Settings → SonarLint → Rules
  ├── 按语言（Java / Python / JavaScript...）
  ├── 按标签（security / performance / bug...）
  ├── 启用 / 禁用特定规则
  └── 导出项目规则文件（sonarlint.json）
```

## 四、Java 常见规则示例

| 规则 ID | 说明 |
|---------|------|
| `S1141` | try-catch 块不应为空 |
| `S2077` | SQL 查询应使用参数化（防注入） |
| `S2111` | BigDecimal 不要用 new BigDecimal(double) |
| `S2221` | 不应捕获 Exception 基类 |
| `S3516` | 方法在可能时应返回 Optional 替代 null |
| `S4248` | 不要在资源关闭后使用 |
| `S5852` | 正则表达式存在 ReDoS 风险 |

## 五、实战工作流

```
写代码 → SonarLint 实时标黄/标红
   ↓
编码完成前 → 打开 SonarLint 面板 → 逐一修复
   ↓
Commit 前 → 确保 0 Blocker + 0 Critical
   ↓
CI/CD → SonarQube 扫描 → 质量阀判定
```

## 六、SonarLint vs 其他工具

| 工具 | 定位 | 集成方式 |
|------|------|----------|
| **SonarLint** | IDE 内实时检查 | 插件 |
| **SonarQube** | 服务端全局质量管控 | CI/CD 流水线 |
| **Checkstyle** | 代码风格规范 | Maven/Gradle 插件 |
| **SpotBugs** | 字节码级别的缺陷检测 | Maven/Gradle 插件 |
| **PMD** | 源码静态分析 | Maven/Gradle 插件 |

## 七、IntelliJ IDEA 深度集成

### 7.1 安装与配置

```
File → Settings → Plugins → 搜索 "SonarLint" → Install → Restart IDE

首次使用：
1. View → Tool Windows → SonarLint → 打开面板
2. 自动扫描当前打开的文件
3. 可在 Project Errors 标签查看全项目所有问题
```

### 7.2 关键操作

| 操作 | 快捷键/方式 | 说明 |
|------|-----------|------|
| 查看问题详情 | 点击行号右侧标记 | 弹出规则说明+修复建议 |
| 分析当前文件 | SonarLint面板 → Analyze Current File | 手动触发分析 |
| 分析所有文件 | SonarLint面板 → Analyze All Files | 全项目扫描 |
| 清理已修复问题 | SonarLint面板 → Clean Console | 清理已处理的问题 |
| 规则配置 | File → Settings → Tools → SonarLint → Rules | 启用/禁用特定规则 |

### 7.3 Connected Mode 配置详解

```text
步骤：
1. SonarQube管理员 → My Account → Security → Generate Token
2. IDE → Settings → Tools → SonarLint → Connected Mode
3. Add Connection → SonarQube → 输入URL和Token
4. Bind Project → 选择SonarQube上的对应项目

同步效果：
✅ 本地IDE自动使用SonarQube的Quality Profile（规则集）
✅ 本地排除的规则与服务器一致
✅ 可看到服务器标记为"Won't Fix"/"False Positive"的问题
✅ 新代码周期内的问题着重高亮
```

## 八、常见问题与排查

| 问题 | 原因 | 解决 |
|------|------|------|
| 安装后没有扫描 | 文件未被分析 | 手动Analyze Current File |
| 误报太多 | 默认规则集不适合项目 | 进入Rules设置禁用不适用的规则 |
| Connected Mode连接失败 | Token过期/URL错误 | 重新生成Token检查URL |
| 分析速度慢 | 大项目全量扫描 | 使用Analyze All Files仅在需要时 |
| 与本地Checkstyle规则冲突 | 两套规则不一致 | 以SonarQube服务器规则为准 |

## 九、SonarLint 规则深度解读

### 9.1 Java关键规则Top 10

| 规则ID | 规则名 | 严重度 | 说明 | 修复示例 |
|--------|--------|--------|------|----------|
| `S1141` | try-catch不以为空 | Blocker | 空的catch块隐藏异常 | 至少加日志记录 |
| `S2077` | SQL查询硬拼接 | Blocker | 格式化字符串拼SQL → 注入风险 | 使用PreparedStatement |
| `S2111` | `new BigDecimal(double)` | Critical | double精度丢失 | 用`new BigDecimal("0.1")` |
| `S2221` | catch Exception | Critical | 捕获过于宽泛 | 捕获具体异常类型 |
| `S3516` | 返回null不返回Optional | Major | null增加NPE风险 | `return Optional.ofNullable(x)` |
| `S1181` | catch Throwable | Blocker | 捕获Throwable捕获了Error | 只catch Exception |
| `S1319` | 用ArrayList声明而非List | Minor | 违背面向接口编程 | `List<String> list = new ArrayList<>()` |
| `S1068` | 未使用的private字段 | Major | 死代码 | 删除未使用的字段 |
| `S1854` | 无用的赋值 | Major | 赋值后未使用 | 删除无用赋值 |
| `S3457` | `String.format`用于日志 | Minor | 性能+可读性问题 | 用`log.info("{}", value)` |

### 9.2 自定义规则抑制

```java
// 单行抑制（不推荐滥用）
@SuppressWarnings("java:S1068")
private String legacyField; // 遗留字段，历史原因保留

// 方法级抑制
@SuppressWarnings({"java:S1141", "java:S2221"})
public void legacyMethod() { ... }

// 连接SonarQube后，在服务器端标记 "Won't Fix" 或 "False Positive"
// 这些标记会同步到本地SonarLint，更推荐这种方式
```

## 十、总结

> 🎯 SonarLint 的价值在于 **"左移"质量问题**——在 IDE 中实时发现并修复问题，而非等 CI/CD 阶段才发现。

**使用建议：**
- **必装**：所有Java开发者必备IDE插件
- **必连**：Connected Mode对接团队SonarQube，保持规则一致性
- **纪律**：提交代码前确保0 Blocker + 0 Critical
- **不盲目**：理解每条规则的含义，不为了消除告警而乱改代码
- **长期主义**：SonarLint + SonarQube + CI/CD 构成完整的代码质量保障体系
