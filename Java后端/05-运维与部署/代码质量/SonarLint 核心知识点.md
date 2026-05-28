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

## 七、总结

SonarLint 的价值在于 **"左移"质量问题**——在 IDE 中实时发现并修复问题，而非等 CI/CD 阶段才发现。2026 年结合 AI 能力，SonarLint 的误报率持续降低，建议所有 Java 开发者安装。推荐配置 Connected Mode 对接团队 SonarQube，保持规则一致性。
