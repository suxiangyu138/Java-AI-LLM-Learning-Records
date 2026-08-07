# 04 方法级安全：@EnableMethodSecurity 与表达式

> 请求级授权管"谁能进这个接口"，方法级安全管"谁能调这个方法"——@PreAuthorize 家族在 Service 层实现字段级、数据级细粒度控制，7.0 起 @EnableMethodSecurity 是唯一模型

---

## 📚 目录

1. [方法级安全的价值与触发原理](#1-方法级安全的价值与触发原理)
2. [@EnableMethodSecurity 开启与配置](#2-enablemethodsecurity-开启与配置)
3. [核心注解全解](#3-核心注解全解)
4. [SpEL 安全表达式](#4-spel-安全表达式)
5. [自定义权限校验与表达式](#5-自定义权限校验与表达式)
6. [异步与事务下的上下文传播](#6-异步与事务下的上下文传播)

---

## 1. 方法级安全的价值与触发原理

**为什么需要方法级：** 请求级授权只能判断"URL 权限"，业务层细粒度规则（"只有订单本人能看""只有管理员能改状态"）必须写在方法上——否则就得在业务代码里手写 `if(!hasPermission) throw`，散落且难审计。

**触发原理（AOP）：**

```text
调用 @PreAuthorize 方法
  → 代理拦截（CGLIB/JDK，与事务切面同族）
  → 读取方法注解的 SpEL 表达式
  → SecurityContextHolder 取当前认证
  → AuthorizationManager 评估 → 通过则 proceed，否则 AccessDeniedException
```

> 🎯 **要点**：方法级安全是 AOP 的典型应用——与 @Transactional 共用代理机制。**同类自调用失效**的坑在这里同样存在（`this.foo()` 绕过代理 → 注解不生效）。

## 2. @EnableMethodSecurity 开启与配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity                // 7.0 唯一模型（@EnableGlobalMethodSecurity 已删除）
public class SecurityConfig { }
```

| 属性 | 默认 | 说明 |
|------|:---:|------|
| `prePostEnabled` | true | 启用 `@PreAuthorize`/`@PostAuthorize` |
| `securedEnabled` | false | 启用 `@Secured` |
| `jsr250Enabled` | false | 启用 JSR-250 `@RolesAllowed` |

```java
// 同时兼容三种注解
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
```

> ⚠️ **7.0 注意**：`@EnableGlobalMethodSecurity` 已删除；`AuthorizationManager#check` 不再使用，方法级安全底层全部走 `authorize`。旧项目迁移只需换注解名，行为兼容。

## 3. 核心注解全解

### 3.1 四大注解对比

| 注解 | 时机 | 语义 | 典型场景 |
|------|:---:|------|---------|
| `@PreAuthorize` | 方法执行**前** | 满足条件才执行 | 权限校验（最常用） |
| `@PostAuthorize` | 方法执行**后** | 对返回值做二次校验 | 数据级过滤（返回 null 兜底） |
| `@PreFilter` | 方法执行前 | 过滤**入参集合** | 批量删除前剔除无权限项 |
| `@PostFilter` | 方法执行后 | 过滤**返回值集合** | 列表只返回本人数据 |

```java
// 执行前校验
@PreAuthorize("hasRole('ADMIN') or #order.owner == authentication.name")
public Order updateOrder(Order order) { ... }

// 执行后校验（数据级）：只能看自己的订单
@PostAuthorize("returnObject.owner == authentication.name")
public Order getOrder(Long id) { ... }

// 入参/返回值过滤
@PreFilter("filterObject.owner == authentication.name")
public void batchDelete(List<Order> orders) { ... }

@PostFilter("filterObject.owner == authentication.name")
public List<Order> listOrders() { ... }
```

> ⚠️ **@PostAuthorize 陷阱**：方法**已经执行完**才发现无权——副作用（写库）已发生。适合"读操作的数据级校验"；写操作请在方法内自校验或事务配合回滚。

### 3.2 @Secured / @RolesAllowed

```java
@Secured("ROLE_ADMIN")                          // 纯角色
@Secured({"ROLE_ADMIN", "ROLE_OPS"})            // 满足其一
@RolesAllowed("ADMIN")                          // JSR-250 标准（无 ROLE_ 前缀自动处理）
```

| 注解 | 表达式能力 | 标准 |
|------|:---:|------|
| @PreAuthorize/@PostAuthorize | ✅ 完整 SpEL | Spring 专属 |
| @Secured | ❌ 仅角色名 | Spring 遗留 |
| @RolesAllowed | ❌ 仅角色名 | JSR-250 标准 |

> 💡 新代码一律 `@PreAuthorize`（表达式能力完整）；@Secured/@RolesAllowed 用于兼容存量代码。

## 4. SpEL 安全表达式

### 4.1 内置对象

| 对象 | 含义 | 示例 |
|------|------|------|
| `authentication` | 当前认证 | `authentication.name` |
| `principal` | 主体（UserDetails） | `principal.username` |
| `#param` | 方法参数 | `#order.owner` |
| `returnObject` | 方法返回值 | `returnObject.status == 'DRAFT'` |
| `filterObject` | 当前遍历元素 | `filterObject.owner == ...` |

### 4.2 常用表达式速查

```java
// 角色与权限
@PreAuthorize("hasRole('ADMIN')")                          // 角色
@PreAuthorize("hasAnyRole('ADMIN','OPS')")                 // 任一角色
@PreAuthorize("hasAuthority('order:write')")               // 精确权限
@PreAuthorize("hasAnyAuthority('a','b')")
@PreAuthorize("hasAllRoles('ADMIN','AUDITOR')")            // 7.0：全部角色（AllAuthoritiesAuthorizationManager）

// 逻辑组合
@PreAuthorize("hasRole('ADMIN') and #order.owner == authentication.name")
@PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")

// 方法参数引用
@PreAuthorize("#userId == authentication.name")            // 只能操作自己
@PreAuthorize("#order.status.name() == 'DRAFT'")           // 状态校验
```

### 4.3 7.0 变化

- 新增 `AllAuthoritiesAuthorizationManager`（`hasAllRoles`/`hasAllAuthorities`：要求全部满足）；
- 复杂 SpEL 建议改写为自定义 AuthorizationManager Bean（易测试）；
- 方法级与请求级共用同一套 AuthorizationManager 抽象（可复用自定义授权器）。

## 5. 自定义权限校验与表达式

### 5.1 自定义校验 Bean（推荐）

```java
@Component("perm")                       // Bean 名：表达式里用
public class PermissionService {

    public boolean isOwner(String owner) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName().equals(owner);
    }

    public boolean canApprove(String orderType) {
        return hasRole("ADMIN") || "LOW".equals(orderType);
    }

    private boolean hasRole(String role) { /* 自行判断 */ }
}
```

```java
// 表达式引用 Bean：@perm.isOwner(...)
@PreAuthorize("@perm.isOwner(#order.owner)")
public Order updateOrder(Order order) { ... }

@PreAuthorize("@perm.canApprove(#order.type)")
public void approve(Order order) { ... }
```

> 💡 **最佳实践**：复杂规则（跨表查询、业务状态机校验）写进自定义 Bean，注解保持一行表达式——可单测、可复用、表达式不爆炸。

### 5.2 自定义表达式解析器（高级）

```java
@Bean
public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
    DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
    handler.setPermissionEvaluator(new MyPermissionEvaluator());   // 自定义 PermissionEvaluator
    return handler;
}
```

## 6. 异步与事务下的上下文传播

| 场景 | 问题 | 方案 |
|------|------|------|
| 同类自调用 | `this.method()` 绕过代理，注解失效 | 注入自身代理 / 拆类 |
| @Async 线程 | ThreadLocal 上下文丢失 | `DelegatingSecurityContextAsyncTaskExecutor` |
| 线程池 | 子线程无上下文 | `DelegatingSecurityContextExecutor` |
| 响应式 | 线程切换 | 响应式安全上下文（ReactorContext） |
| 虚拟线程（JDK 21+） | ThreadLocal 兼容但需显式传播 | 同线程池方案 |

```java
// @Async 上下文传播
@Bean
public AsyncTaskExecutor securityAwareExecutor() {
    return new DelegatingSecurityContextAsyncTaskExecutor(
            new ThreadPoolTaskExecutor() {{
                setCorePoolSize(4); setMaxPoolSize(8); setQueueCapacity(100);
                afterPropertiesSet();
            }});
}
```

> ⚠️ **事务注意**：@PreAuthorize 与 @Transactional 在同一代理上，**授权失败抛 AccessDeniedException 会触发事务回滚**（RuntimeException 语义）——这是合理的默认行为，但"先授权后写库"的时序要清楚。

> 🎯 **核心要点**：方法级安全 = AOP 代理 + SpEL 表达式 + AuthorizationManager。三句话：①`@PreAuthorize` 管"能不能调"，`@PostAuthorize` 管"返回值合不合规"，过滤注解管集合；②复杂规则进自定义 Bean（`@perm.xxx()`）；③异步/自调用是失效重灾区，代理语义与事务完全一致。

---

**上一模块**：[03-授权模型：AuthorizationManager与请求级授权](03-授权模型：AuthorizationManager与请求级授权.md)　**下一模块**：[05-Web防护：CSRF、CORS与安全头](05-Web防护：CSRF、CORS与安全头.md)
