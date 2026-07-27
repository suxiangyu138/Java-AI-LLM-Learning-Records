# 08 - GitHub Copilot 与 AI 编程

> Copilot 是 AI 辅助编程的标杆——从代码补全到 Chat 对话，从 PR 摘要到代码审查。学会与 AI 高效协作，是新一代工程师的核心竞争力。

---

## 目录

1. [Copilot 产品矩阵](#1-copilot-产品矩阵)
2. [代码补全最佳实践](#2-代码补全最佳实践)
3. [Copilot Chat 对话技巧](#3-copilot-chat-对话技巧)
4. [Context 上下文管理](#4-context-上下文管理)
5. [Copilot 在 Java 开发中的实战](#5-copilot-在-java-开发中的实战)
6. [限制与注意事项](#6-限制与注意事项)
7. [常见面试题](#7-常见面试题)

---

## 1. Copilot 产品矩阵

### 1.1 三大产品

```
┌─────────────────────────────────────────────────────────┐
│                  GitHub Copilot 产品线                     │
├─────────────────────┬───────────────────────────────────┤
│ Copilot             │ IDE 中的代码补全                     │
│ (Code Completion)   │ - 行级补全 / 块级补全                │
│                     │ - 支持 VS Code, JetBrains, Neovim   │
├─────────────────────┼───────────────────────────────────┤
│ Copilot Chat        │ IDE 中的对话式 AI                    │
│                     │ - 解释代码 / 重构 / 生成测试          │
│                     │ - 对话上下文包含当前文件              │
├─────────────────────┼───────────────────────────────────┤
│ Copilot Code Review │ PR 审查辅助                         │
│                     │ - 自动发现潜在问题                   │
│                     │ - 仅在 GitHub.com 上可用             │
└─────────────────────┴───────────────────────────────────┘
```

### 1.2 安装与配置（JetBrains IDEA）

```
1. 安装 GitHub Copilot 插件
   → Settings → Plugins → 搜索 "GitHub Copilot"

2. 登录 GitHub 账号
   → Tools → GitHub Copilot → Login to GitHub
   → 浏览器中授权

3. 配置
   → Settings → Tools → GitHub Copilot
   → ✅ Enable GitHub Copilot

4. 快捷键
   → Tab：接受建议
   → Alt + \：触发内联补全
   → Ctrl + Enter：查看多个建议
   → Alt + [ / ]：上一条/下一条建议
```

---

## 2. 代码补全最佳实践

### 2.1 让 Copilot 理解你的意图

```java
// ═══ ✅ 好：提供清晰的上下文 ═══

// 方法名和参数清晰表达意图
public List<User> findActiveUsersByDepartment(Long departmentId) {
    // 注释描述复杂逻辑
    // Filter users by department, active status, and sort by last login descending
    return userRepository.findByDepartmentIdAndStatusOrderByLastLoginDesc(
        departmentId, UserStatus.ACTIVE);
}

// ═══ ❌ 差：模糊的上下文 ═══

// Copilot 不知道你想做什么
public List<User> getUsers(Long id) {
    // ...
}
```

### 2.2 通过注释引导补全

```java
// ═══ 注释驱动的代码生成 ═══

// Create a REST controller for user management with CRUD endpoints
// Endpoints:
//   GET /api/users — list all users with pagination
//   GET /api/users/{id} — get user by id, return 404 if not found
//   POST /api/users — create new user, validate email uniqueness
//   PUT /api/users/{id} — update user
//   DELETE /api/users/{id} — soft delete
@RestController
@RequestMapping("/api/users")
public class UserController {
    // Copilot 会根据上面的注释生成完整的 Controller
    // ...
}

// ═══ 算法注释 ═══

// Implement binary search on a sorted array
// Return index of target, or -1 if not found
public int binarySearch(int[] arr, int target) {
    // Copilot 补全：
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) return mid;
        if (arr[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}
```

### 2.3 Copilot 擅长的场景

| 场景 | 效果 | 示例 |
|------|------|------|
| **重复模式** | ⭐⭐⭐⭐⭐ | CRUD 接口、Builder 模式、DTO 转换 |
| **单元测试** | ⭐⭐⭐⭐⭐ | Given-When-Then 模板、边界用例 |
| **正则表达式** | ⭐⭐⭐⭐ | 邮箱校验、手机号匹配 |
| **SQL 查询** | ⭐⭐⭐⭐ | 复杂 JOIN、分页查询 |
| **YAML/XMl 配置** | ⭐⭐⭐⭐ | Spring 配置、CI 配置文件 |
| **JavaDoc 注释** | ⭐⭐⭐⭐ | 类/方法文档 |
| **复杂算法** | ⭐⭐⭐ | 需清晰的注释引导 |
| **业务逻辑** | ⭐⭐ | 需完整上下文 |

---

## 3. Copilot Chat 对话技巧

### 3.1 常用对话模板

```markdown
# ═══ 解释代码 ═══
"/explain 这段代码的作用"
"解释 UserService.authenticate() 方法的认证流程"

# ═══ 重构 ═══
"/fix 修复这段代码中的 potential NPE"
"/simplify 简化这个 if-else 逻辑，使用策略模式"
"将这段同步代码改写为 CompletableFuture 异步版本"

# ═══ 生成测试 ═══
"/tests 为 UserService 生成完整的单元测试"
"/tests 添加边界条件测试：空输入、null、超大值"

# ═══ 生成代码 ═══
"写一个方法：接收 List<User>，按 age 降序排列，返回前 N 个"
"生成 Spring Security 配置：JWT 认证 + 忽略 /api/public/**"

# ═══ 调试 ═══
"/fix 修复这个错误：NullPointerException at line 42"
"这段代码在高并发下可能有什么问题？"

# ═══ 文档 ═══
"/doc 为这个方法生成 JavaDoc"
```

### 3.2 Chat 上下文

```
Copilot Chat 的上下文来源：
  1. 当前打开的文件（自动包含）
  2. 选中的代码片段（Ctrl+Shift+I → Ask Copilot）
  3. 对话历史（同一会话中）
  4. 手动 @workspace 引用整个项目
  5. 手动 @file 引用特定文件

最佳实践：
  ✅ 打开相关文件再提问（让 Copilot 获得更多上下文）
  ✅ 选中代码 + 右键 → Copilot → Explain This
  ✅ 使用 @workspace 进行跨文件分析
```

---

## 4. Context 上下文管理

### 4.1 让 Copilot 更了解你的项目

```markdown
# ═══ 在 IDE 中打开相关文件 ═══
提问"如何创建新的 API 端点？"
  ✅ 打开已有的 Controller 类 → Copilot 会参考你的风格
  ❌ 不打开任何文件 → Copilot 生成通用模板（可能不符合项目规范）

# ═══ 使用 @workspace 引用 ═══
"@workspace 数据库连接配置在哪里？"
"@workspace 项目中如何处理异常？参考已有模式写一个新的异常类"

# ═══ 使用 @file 引用 ═══
"@file:UserService.java 参考这个类的风格，写一个 OrderService"
```

### 4.2 编写好的 Prompt

```
Prompt 质量的四个要素：

1. 角色（Role）：可选，但有助于调整风格
   "作为一个 Java 后端开发者..."

2. 任务（Task）：明确做什么
   "编写一个方法，根据用户ID查询订单列表"

3. 约束（Constraints）：格式、框架、模式
   "使用 Spring Data JPA，返回 Page<Order>，包含分页参数"
   "使用项目中已有的 BaseController 模式"

4. 示例（Examples）：一两个示例比长篇描述更有效
   "参考 UserController.findById() 的风格"

完整示例：
  "参考 UserController.findById() 的风格，
   写一个 OrderController.getByUserId() 方法，
   使用 Spring Data JPA Specification 实现多条件动态查询，
   包含分页和排序参数"
```

---

## 5. Copilot 在 Java 开发中的实战

### 5.1 快速生成 Entity + DTO 转换

```java
// 给 Copilot 一个 Entity 类，让它生成 DTO 和转换器

// Step 1: 定义 Entity
@Entity
public class User {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    // getter/setter ...
}

// Step 2: 写注释让 Copilot 生成 DTO
// Create a UserDTO with: id, username, email, createdAt formatted as "yyyy-MM-dd HH:mm:ss"

// Step 3: 生成 MapStruct Mapper
// Create a MapStruct mapper interface for converting User ↔ UserDTO
```

### 5.2 快速生成 Repository 查询

```java
// 写好注释，Copilot 自动补全 Spring Data JPA 方法名
public interface UserRepository extends JpaRepository<User, Long> {

    // Find active users by department ordered by name
    List<User> findByDepartmentAndStatusOrderByUsername(
        Department department, UserStatus status);

    // Find users with a custom @Query
    // Find all users who have placed at least one order in the last 30 days
    @Query("SELECT DISTINCT u FROM User u JOIN u.orders o " +
           "WHERE o.createdAt > :since")
    List<User> findActiveUsersWithRecentOrders(@Param("since") LocalDateTime since);
}
```

### 5.3 生成测试用例

```java
// 选中方法 → Ctrl+Shift+I → /tests
// Copilot 生成：

@Test
void shouldReturnUsers_whenValidDepartment() {
    // Given
    Department dept = DepartmentFixture.createIT();
    User user1 = UserFixture.create("张三", dept, UserStatus.ACTIVE);
    User user2 = UserFixture.create("李四", dept, UserStatus.ACTIVE);
    when(userRepository.findByDepartmentAndStatus(any(), any()))
        .thenReturn(List.of(user1, user2));

    // When
    List<User> result = userService.findActiveUsers(dept.getId());

    // Then
    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(u -> u.getStatus() == UserStatus.ACTIVE));
}

@Test
void shouldReturnEmptyList_whenNoActiveUsers() {
    // Given
    when(userRepository.findByDepartmentAndStatus(any(), any()))
        .thenReturn(Collections.emptyList());

    // When
    List<User> result = userService.findActiveUsers(1L);

    // Then
    assertTrue(result.isEmpty());
}
```

---

## 6. 限制与注意事项

### 6.1 Copilot 的局限性

```
⚠️ 不能做的：
1. 完全理解你的业务逻辑（只能基于公开代码和当前上下文）
2. 保证代码安全性（必须人工审查）
3. 理解项目历史决策（为什么这样设计）
4. 精确匹配你公司内部库的 API（私有代码不在训练数据中）

⚠️ 注意：
1. Copilot 生成的代码可能有版权问题（从公开代码学习）
2. 不要粘贴敏感数据（API Key、密码等）到 Chat 中
3. AI 可能产生幻觉——自信地输出错误代码
4. 过度依赖会降低独立编程能力
```

### 6.2 安全使用准则

```
✅ 所有 AI 生成代码必须 Review
✅ 不在 Chat 中粘贴公司私有代码
✅ 敏感配置使用环境变量，不让 AI 看到
✅ 生成的 SQL 必须检查是否安全（防注入）
✅ 生成的认证/授权代码必须安全审查
✅ 遵守公司 AI 使用政策
```

---

## 7. 常见面试题

### Q1：Copilot Code Completion 和 Chat 的区别？

> Completion 是实时代码补全（Tab 接受建议），集成在编码流中；Chat 是对话式 AI（解释、重构、生成测试、问答），可理解多文件上下文。详见第1-3节。

### Q2：如何提高 Copilot 的建议质量？

> 1) 给出清晰的方法名和参数名；2) 用注释描述意图；3) 打开相关文件提供上下文；4) 使用 @workspace 引用项目；5) 编写好的 Prompt（角色+任务+约束+示例）。详见第2-4节。

### Q3：使用 Copilot 有哪些风险？

> 代码安全性（需人工审核）、版权合规（可能产生与开源代码相似的输出）、隐私泄露（不要在 Chat 中输入敏感信息）、过度依赖降低技能。详见第6节。
