# 02 注解驱动开发：@Controller 与请求映射

> @RequestMapping 家族是"URL 与方法的契约"：路径、方法、参数、头、媒体类型五维匹配规则，组合注解的语义差异，以及 produces/consumes 的前置协商——本模块把请求映射写对、写规范

---

## 📚 目录

1. [@Controller 与 @RestController](#1-controller-与-restcontroller)
2. [@RequestMapping 家族：组合注解](#2-requestmapping-家族组合注解)
3. [五维匹配：路径、方法、参数、头、媒体类型](#3-五维匹配路径方法参数头媒体类型)
4. [路径变量与通配](#4-路径变量与通配)
5. [produces / consumes：内容协商前置](#5-produces--consumes内容协商前置)
6. [匹配规则优先级与冲突](#6-匹配规则优先级与冲突)
7. [接口设计规范](#7-接口设计规范)

---

## 1. @Controller 与 @RestController

```java
@RestController                     // = @Controller + @ResponseBody
public class UserController { ... }

// 等价写法
@Controller
public class UserController {
    @ResponseBody
    @GetMapping("/api/users")
    public List<User> list() { ... }
}
```

| 注解 | 语义 | 返回值处理 |
|------|------|-----------|
| `@Controller` | 控制器 + 视图渲染（配合模板） | 返回 String → 视图名 |
| `@RestController` | 控制器 + 消息转换 | 返回对象 → JSON/XML |
| `@RestControllerAdvice` | 全局异常 + 响应增强（05 篇） | — |

> 🎯 **要点**：@RestController 只做一件事——给类上所有方法加 @ResponseBody。**前后端分离场景一律 @RestController**；@Controller 只留给服务端渲染（Thymeleaf/JSP）。

## 2. @RequestMapping 家族：组合注解

| 注解 | 等价 | 语义 |
|------|------|------|
| `@GetMapping` | `@RequestMapping(method=GET)` | 查询 |
| `@PostMapping` | method=POST | 创建 |
| `@PutMapping` | method=PUT | 全量更新 |
| `@PatchMapping` | method=PATCH | 部分更新 |
| `@DeleteMapping` | method=DELETE | 删除 |

```java
@RestController
@RequestMapping("/api/users")          // 类级前缀：统一路径
public class UserController {

    @GetMapping("/{id}")                // GET /api/users/1001
    public User get(@PathVariable Long id) { ... }

    @PostMapping                        // POST /api/users
    public User create(@RequestBody User user) { ... }

    @PutMapping("/{id}")                // PUT /api/users/1001
    public User update(@PathVariable Long id, @RequestBody User user) { ... }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { ... }
}
```

> 💡 **注意**：类级 @RequestMapping 不限定方法（可写 method 限定），组合注解只能用于方法级。

## 3. 五维匹配：路径、方法、参数、头、媒体类型

```java
@RequestMapping(
    value = "/api/search",            // ① 路径
    method = RequestMethod.GET,       // ② HTTP 方法
    params = "mode=advanced",         // ③ 请求参数条件
    headers = "X-API-Version=2",      // ④ 请求头条件
    produces = "application/json",    // ⑤ 可产生的媒体类型
    consumes = "application/json"     //   可消费的媒体类型
)
```

| 维度 | 匹配语义 | 不满足时 |
|------|---------|---------|
| 路径 | URL + 变量/通配 | 404 |
| 方法 | GET/POST/PUT/DELETE... | 405 Method Not Allowed |
| 参数 | `params` 条件（有/无/值） | 404（无候选） |
| 头 | `headers` 条件 | 404 |
| produces | Accept 头是否含声明类型 | 406 Not Acceptable |
| consumes | Content-Type 是否匹配 | 415 Unsupported Media Type |

> 🎯 **要点**：这五维是"候选方法筛选器"——同时满足才命中。**406/415/405 是配置不匹配的信号，不是服务端错误**。

## 4. 路径变量与通配

```java
@GetMapping("/api/users/{id}")
public User get(@PathVariable Long id) { ... }          // 变量 → 方法参数

@GetMapping("/api/files/{dir}/{name}")
public void file(@PathVariable String dir, @PathVariable("name") String fileName) { ... }

@GetMapping("/api/users/{id}/orders/{orderId}")
public Order order(@PathVariable Long id, @PathVariable Long orderId) { ... }
```

**通配规则（PathPattern，7.x 默认）：**

| 模式 | 匹配 | 说明 |
|------|------|------|
| `/api/users/*` | `/api/users/1` | 单段 |
| `/api/**` | `/api/a/b/c` | 多段（开头/结尾） |
| `/api/{id:[0-9]+}` | `/api/1001` | 正则约束（不匹配 1001a） |
| `/api/{*path}` | 剩余所有段 | 尾部捕获 |

> ⚠️ **PathPattern 纪律**（与 Security 体系一致）：`**` 只能出现在开头或结尾、单段；尾斜杠不再匹配——`/api/users/` ≠ `/api/users`。

## 5. produces / consumes：内容协商前置

```java
// 只产生 JSON（声明后其它格式不会尝试）
@GetMapping(value = "/api/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
public User get(@PathVariable Long id) { ... }

// 只消费 JSON（请求体必须是 JSON）
@PostMapping(value = "/api/users", consumes = MediaType.APPLICATION_JSON_VALUE)
public User create(@RequestBody User user) { ... }

// 多格式：按 Accept 返回 JSON 或 XML
@GetMapping(value = "/api/report", produces = {"application/json", "application/xml"})
public Report report() { ... }
```

| 声明 | 影响 |
|------|------|
| `produces` | 内容协商的候选；Accept 不匹配 → 406 |
| `consumes` | Content-Type 校验；不匹配 → 415 |
| 不声明 | 默认按可注册转换器协商（JSON 优先） |

> 💡 **实战建议**：REST API 固定 JSON 时**显式声明 produces**——避免"请求 Accept: text/html 时框架尝试 HTML 渲染"的边界行为；也给接口文档（OpenAPI）提供准确语义。

## 6. 匹配规则优先级与冲突

### 6.1 命中同一请求的多个方法

```text
GET /api/users/1001 命中两个方法时：
  /api/users/{id}       （变量）
  /api/users/special    （字面量）
→ 字面量优先（模板特异性打分：精确段 > 变量段 > 通配段）

同样的变量个数与位置 → 无法区分 → 启动报错（Ambiguous mapping）
```

### 6.2 常见冲突与解决

| 冲突 | 现象 | 解法 |
|------|------|------|
| 两个方法同路径同方法 | 启动失败 Ambiguous | 改路径/加 produces 区分 |
| 变量与字面量撞车 | 字面量恒赢 | 避免同层变量与固定路径混用 |
| 泛型端点重复 | `/api/{type}` 与 `/api/users` | 重构路径设计 |

> ⚠️ **启动报错提示**：`Ambiguous mapping. Cannot map 'xxx' method ...`——排查两个映射的条件是否完全可区分（加 method/params/produces 维度）。

## 7. 接口设计规范

### 7.1 REST 资源化设计

```text
资源名词 + HTTP 方法语义：
  GET    /api/users         列表
  GET    /api/users/{id}    详情
  POST   /api/users         创建
  PUT    /api/users/{id}    全量更新
  PATCH  /api/users/{id}    部分更新
  DELETE /api/users/{id}    删除
子资源：GET /api/users/{id}/orders
操作化接口：POST /api/users/{id}/activate（动词只用于动作）
```

### 7.2 版本化

| 方案 | 方式 | 适用 |
|------|------|------|
| URL 版本 | `/api/v2/users` | 简单直观（常用） |
| 请求头版本 | `X-API-Version: 2` | 语义化（配合 headers 匹配） |
| 内容类型版本 | `application/vnd.company.v2+json` | 媒体类型协商 |

```java
// 头版本化：同路径不同版本不同方法
@GetMapping(value = "/api/users", headers = "X-API-Version=1")
public List<UserV1> listV1() { ... }

@GetMapping(value = "/api/users", headers = "X-API-Version=2")
public List<UserV2> listV2() { ... }
```

> 🎯 **核心要点**：请求映射 = 五维匹配（路径/方法/参数/头/媒体类型）的声明式筛选。写接口的纪律：组合注解表达语义、资源化路径 + 方法语义、显式 produces（JSON）、变量用正则约束、避免路径冲突。**REST 接口的"长相"全由这一层决定**。

---

**上一模块**：[01-DispatcherServlet请求处理全链路](01-DispatcherServlet请求处理全链路.md)　**下一模块**：[03-参数绑定与数据校验](03-参数绑定与数据校验.md)
