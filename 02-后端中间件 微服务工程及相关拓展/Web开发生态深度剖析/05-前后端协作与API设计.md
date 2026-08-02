# 05 - 前后端协作与 API 设计

> 定位：RESTful 规范、参数校验、跨域、幂等、版本化、错误码体系、接口文档——前后端协作契约

## 📚 目录

1. [前后端协作模式](#1-前后端协作模式)
2. [RESTful API 规范](#2-restful-api-规范)
3. [统一响应与错误码](#3-统一响应与错误码)
4. [参数校验](#4-参数校验)
5. [跨域 CORS](#5-跨域-cors)
6. [幂等与版本化](#6-幂等与版本化)

---

## 1. 前后端协作模式

```
前后端分离（当前主流）：
  前端：Vue/React（独立部署）
  后端：REST API + JSON
  协作契约：接口文档（OpenAPI）

关键约定：
  ① 接口文档先行（API-first）
  ② 统一响应格式
  ③ 错误码语义化
  ④ 联调环境（Mock）

⚠️ 面试必答：
"前后端分离 = 契约驱动——
 接口文档是团队共识、JSON 是语言、
 统一响应是规范。"
```

---

## 2. RESTful API 规范

### 2.1 资源设计

```
RESTful 核心：
  资源用名词复数：/users /orders
  方法表动作：GET 查/POST 建/PUT 改/DELETE 删
  嵌套资源：/users/{id}/orders

示例：
  GET    /api/v1/users           查询列表
  GET    /api/v1/users/1001      查询单个
  POST   /api/v1/users           创建（201）
  PUT    /api/v1/users/1001      全量更新
  PATCH  /api/v1/users/1001      部分更新
  DELETE /api/v1/users/1001      删除（204）
```

### 2.2 REST 规范要点

```
✅ 状态码语义（200/201/204/400/401/403/404）
✅ 查询参数分页（?page=1&size=20 + 元信息）
✅ 版本化（/api/v1 或 Header）
✅ DTO 分层（请求 DTO/响应 VO）
❌ 动词化 URL（/getUser 非 REST）
❌ 大而全的万能接口

⚠️ 面试必答：
"REST 五规范——资源名词、方法语义、
 状态码、分页、版本化；
 DTO/VO 分离是工程标准。"
```

---

## 3. 统一响应与错误码

### 3.1 统一响应格式

```java
// ⚠️ 统一响应（前后端约定）
@Data
public class Result<T> {
    private int code;        // 业务码（0 成功）
    private String message;  // 提示信息
    private T data;          // 数据
    private long timestamp;  // 时间戳（可选）

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "success";
        r.data = data;
        return r;
    }
    public static <T> Result<T> error(int code, String message) { }
}

// 分页响应
@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
}
```

### 3.2 错误码设计

```
错误码设计原则：
  ① 分段定义（1xxxx 参数、2xxxx 认证、3xxxx 业务）
  ② 语义化（1001 参数缺失、2001 未登录、3001 余额不足）
  ③ 前后端枚举对齐（文档维护）
  ④ 全局异常 → 统一错误码（@RestControllerAdvice）

⚠️ 面试必答：
"错误码三原则——分段、语义化、文档对齐；
 全局异常处理器统一映射（业务/校验/系统三类）。"
```

---

## 4. 参数校验

### 4.1 分层校验

```
校验三层：
  ① 前端校验（体验：即时提示）
  ② 后端 DTO 校验（安全：Bean Validation）⚠️ 必须
  ③ 业务校验（规则：库存/余额/状态）

⚠️ 面试必答：
"前端校验是体验、后端校验是安全——
 后端必须校验（前端可绕过）；
 DTO 注解 + 业务规则双层。"
```

### 4.2 Bean Validation

```java
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "长度 2-20")
    private String name;

    @NotNull
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @Email(message = "邮箱格式错误")
    private String email;
}

// Controller 启用
@PostMapping
public Result<UserVO> create(@Valid @RequestBody UserCreateDTO dto) { }
// ⚠️ 校验失败 → MethodArgumentNotValidException → 全局异常处理
```

> 🎯 **要点**：DTO 校验（@Valid + 注解）是接口安全第一道门——参数错误在入口拦截，业务层只处理"合法输入"。

---

## 5. 跨域 CORS

### 5.1 跨域原理

```
同源策略：协议 + 域名 + 端口相同才可访问
跨域 = 不满足同源（前后端分离常见）

CORS 流程：
  浏览器预检（OPTIONS）→ 服务器返回允许头 → 正式请求

⚠️ 面试必答：
"跨域是浏览器安全策略——
 CORS 靠服务器响应头声明允许；
 生产方案：Nginx 代理（同源）或 CORS 配置。"
```

### 5.2 CORS 配置

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("https://admin.example.com");  // ⚠️ 生产限定域名
        config.addAllowedMethod("*");       // GET/POST/PUT/DELETE
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);   // 允许携带 Cookie
        config.setMaxAge(3600L);            // 预检缓存
        return new CorsFilter(source -> config);
    }
}
// ⚠️ 安全：生产不要用 *（允许所有来源 + 凭据不能并存）
```

---

## 6. 幂等与版本化

### 6.1 幂等设计

```
幂等 = 重复执行结果相同

需要幂等的场景：
  POST 创建（重复提交会重复创建！）
  支付/退款（重复扣款/退款）

方案：
  ① 幂等键（请求头 Idempotency-Key）
  ② 数据库唯一约束（订单号唯一）
  ③ 状态机（已处理 → 拒绝）

⚠️ 面试必答：
"幂等三方案——幂等键（Redis 去重）、
 唯一约束（DB 兜底）、状态机（业务校验）；
 资金类接口必须幂等。"
```

### 6.2 版本化

```
API 版本化三方式：
  ① URL 路径：/api/v1/users（最常用）
  ② Header：X-API-Version: 1
  ③ 媒体类型：Accept: application/vnd.app.v1+json

策略：
  主版本不兼容（v1 → v2）
  次版本兼容（新字段）
  旧版本维护期（双版本过渡）

⚠️ 面试必答：
"版本化 = 破坏性变更的护栏——
 URL 路径最直观；v1/v2 并存过渡、
 旧版本限期废弃。"
```

---

> 🎯 **核心要点**：协作体系 = **契约驱动**（文档先行 + 统一响应）+ **RESTful 五规范**（资源/方法/状态码/分页/版本）+ **错误码**（分段语义化 + 全局异常）+ **校验**（DTO 注解 + 业务规则双层）+ **CORS**（白名单 + 凭据安全）+ **幂等版本**（幂等键 + 版本过渡）。"接口是前后端契约、校验是安全底线"是协作两原则。

---

**返回总览**：[00-Web开发生态总览](00-Web开发生态总览.md) | **上一篇**：[04-Web安全与防护](04-Web安全与防护.md) | **下一篇**：[06-性能优化体系](06-性能优化体系.md)
