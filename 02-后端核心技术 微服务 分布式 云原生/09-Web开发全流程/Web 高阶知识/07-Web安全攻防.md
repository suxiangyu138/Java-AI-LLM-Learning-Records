# Web 安全攻防
> OWASP Top 10 2025 全榜解读、XSS/CSRF/SQL 注入/SSRF/越权六大高频攻击的原理与防护——"攻击怎么打进来、防线怎么织起来"

## 📚 目录
1. [OWASP Top 10 2025：榜单与变化](#1-owasp-top-10-2025榜单与变化)
2. [XSS：跨站脚本](#2-xss跨站脚本)
3. [CSRF：跨站请求伪造](#3-csrf跨站请求伪造)
4. [注入类：SQL/命令/模板](#4-注入类sql命令模板)
5. [SSRF：服务端请求伪造](#5-ssrf服务端请求伪造)
6. [越权与访问控制](#6-越权与访问控制)
7. [安全日志与告警](#7-安全日志与告警)
8. [防护清单总表](#8-防护清单总表)

## 1. OWASP Top 10 2025：榜单与变化

| 排名 | 类别 | 2021 对比 | 变化解读 |
|:---:|------|---------|---------|
| A01 | **Broken Access Control**（访问控制失效） | #1 不变 | 含 SSRF（2021 独立类别并入） |
| A02 | **Security Misconfiguration**（安全配置错误） | #5 → **#2** | 云原生可配置性大增，配置错误暴露面扩大 |
| A03 | **Supply Chain Failures**（供应链失败） | #6（组件漏洞）→ **#3** | 范围扩到依赖生态/构建管线/仓库劫持 |
| A04 | Cryptographic Failures | #2 → #4 | 加密实践整体改善 |
| A05 | Injection | #3 → #5 | 框架参数化查询收窄了注入面 |
| A06 | Insecure Design | #4 → #6 | 威胁建模进步 |
| A07 | Authentication Failures | #7 | 名称微调 |
| A08 | Software/Data Integrity Failures | #8 | - |
| A09 | Security Logging & **Alerting** Failures | #9 | 强调告警 |
| A10 | **Mishandling of Exceptional Conditions**（异常处理不当） | **全新** | 错误处理/逻辑错误/fail-open |

> 🎯 2025 版本质变化：**风险从"代码漏洞"转向"架构与配置"**——注入靠框架已大幅缓解，访问控制、配置错误、供应链成为前三。2026 年面试聊安全，先讲这个趋势再讲具体攻击。

## 2. XSS：跨站脚本

### 2.1 三种形态

```text
反射型 XSS：恶意脚本在 URL 参数里，服务器回显 → 一次性
  https://search.example.com/?q=<script>alert(1)</script>

存储型 XSS：恶意脚本存进数据库，所有用户访问时执行（最危险）
  评论区提交 <script>...</script> → 每次渲染都执行

DOM 型 XSS：纯前端漏洞（不经过服务器）
  location.hash 内容直接 innerHTML
```

### 2.2 攻击链与防护

| 环节 | 防护 |
|------|------|
| 输入侧 | 输入校验（长度/白名单）；**富文本用白名单过滤库** |
| 存储侧 | 不信任任何用户输入 |
| 输出侧 | **上下文转义**：HTML 转义 `<>"&` / JS 转义 / URL 编码 |
| 全局兜底 | **CSP**（[05 §6](05-跨域与浏览器安全机制.md)）+ `HttpOnly` Cookie（脚本读不到会话） |
| 框架层 | Vue/React 默认转义；`v-html`/`dangerouslySetInnerHTML` 显式禁用 |

```text
后端输出转义铁律：
  <  → &lt;    >  → &gt;    &  → &amp;    "  → &quot;    '  → &#x27;
  前端框架已自动做；手拼 HTML/JSONP 时最容易漏
```

> ⚠️ **XSS 是"偷会话"的入口**：配合 Cookie 缺 HttpOnly → 拿到 JSESSIONID → 会话劫持。所以 XSS 防护与 Cookie 加固（[会话安全](../Cookie%20%26%20Session（会话技术）/05-会话安全与防护.md)）必须配套。

## 3. CSRF：跨站请求伪造

### 3.1 攻击模型

```text
前提：用户已登录目标站（Cookie 自动携带）
攻击：诱导用户访问恶意页面（图片/表单自动提交）
  恶意页 <img src="https://bank.com/transfer?to=attacker&amount=1000">
  浏览器自动带上 bank.com 的 Cookie → 转账成功！

本质：服务器无法区分"用户主动操作"与"跨站代发请求"
```

### 3.2 三层防线

| 防线 | 机制 | 现状 |
|------|------|------|
| **SameSite Cookie**（默认 Lax） | 跨站子请求不带 Cookie | **第一防线（2026 默认）** |
| CSRF Token | 表单带随机 Token，服务端校验 | 第二防线 |
| 双重提交/自定义头 | 校验自定义头必须跨域预检 | 附加 |

```java
// CSRF Token 防御（Spring Security 内置）
// 状态变更请求必须携带与 Session 绑定的随机 Token：
<form action="/transfer" method="post">
    <input type="hidden" name="_csrf" value="${csrfToken}">
</form>
// 服务器校验：Token 与 Session 绑定值一致才放行
```

> 🎯 2026 结论：**SameSite=Lax 默认化后 CSRF 攻击面大幅收窄**（表单/图片/iframe 的跨站自动请求都不带 Cookie）——但 Token 仍是纵深防御标配（尤其 SameSite 被显式设为 None 的登录态、子域共享 Cookie 场景）。完整方案见 [Cookie & Session 安全体系](../Cookie%20%26%20Session（会话技术）/05-会话安全与防护.md)。

## 4. 注入类：SQL/命令/模板

### 4.1 SQL 注入

```sql
-- 漏洞：字符串拼接
SELECT * FROM users WHERE name = 'admin' OR '1'='1';   -- 万能密码/越权

-- 防护：参数化查询（框架默认）
-- Java：PreparedStatement / MyBatis #{}（不是 ${}！）
SELECT * FROM users WHERE name = ?
-- MyBatis 陷阱：${} 直接拼接（排序字段/表名场景），必须白名单校验
```

| 注入面 | 防护 |
|--------|------|
| SQL | **参数化/ORM**（99% 场景）；动态表名/排序白名单 |
| 命令 | 不拼 shell；参数化或白名单 |
| 模板（SSTI） | 不渲染用户输入；模板引擎沙箱 |
| LDAP/XPath | 参数化 API |

> ⚠️ MyBatis 面试坑：**`#{}` 预编译安全、`${}` 拼接危险**——`ORDER BY ${column}` 需要排序字段时，必须白名单校验后再拼。

## 5. SSRF：服务端请求伪造

### 5.1 攻击模型（云时代高危）

```text
攻击：利用"服务器主动请求外部"的功能打内网
  漏洞代码：URL 参数直接给 HttpClient 请求
  GET /fetch?url=http://169.254.169.254/latest/meta-data/    ← 云元数据服务！
  服务器去请求内网地址 → 攻击者拿到云凭证/内网信息

为什么 2026 更危险：
  · 云元数据服务（169.254.169.254）是凭证窃取入口
  · 内网资产扫描/横向移动的跳板
```

### 5.2 防护

| 防护 | 说明 |
|------|------|
| URL 白名单 | 只允许协议/域名白名单（http/https + 已知域名） |
| DNS 二次解析校验 | 防"白名单域名解析到内网 IP"（重绑定攻击） |
| IP 黑名单 | 拦 169.254.0.0/16、10.0.0.0/8、127.0.0.0/8 等内网段 |
| 禁用重定向跟随 | 防间接跳转内网 |
| 网络层隔离 | 出口防火墙/独立网络命名空间 |
| Boot 4 新能力 | `InetAddressFilter`（SSRF 防护，2026-06 引入，见 [SpringBoot Web](../SpringBoot%20Web/08-生产实践与面试题.md)） |

> 🎯 面试新考点：**SSRF 从 2021 独立类别并入 2025 的 A01 访问控制**——云时代"服务器能访问内网"本身就是信任边界问题；出网请求一律白名单 + 内网段拦截。

## 6. 越权与访问控制

```text
水平越权（IDOR）：用户 A 访问用户 B 的资源
  GET /order/42    ← 42 是别人的订单？没校验就返回（A01 最常见）

垂直越权：普通用户调管理员接口
  POST /admin/deleteUser   ← 没角色校验？

防护核心：服务端每次资源访问都要"验身份 + 验权限"（不依赖前端隐藏）
```

```java
// 水平越权防护：资源属主校验
@GetMapping("/orders/{id}")
public OrderVO getOrder(@PathVariable Long id) {
    Long userId = currentUser().getId();
    return orderService.getOwnedOrder(id, userId);   // WHERE id=? AND user_id=?
    // 查不到 → 404（不暴露"存在但无权限"）
}
```

> ⚠️ 三大原则：**① 权限校验放服务端**（前端按钮隐藏不是安全）；**② 用"属主条件查询"而非"查出再判"**（防时间侧信道/信息泄露）；**③ 默认拒绝**（新接口先想权限）。

## 7. 安全日志与告警

```text
A09 的核心诉求：出了事要"看得见"
  · 认证失败/越权尝试 → 记录（谁、何时、何操作、来源 IP）
  · 敏感操作（转账/删除/改权限）→ 审计日志
  · 异常模式（批量失败、异常频率）→ 告警
  · 日志与 traceId 贯通 → 攻击溯源链路

反面教材（A10 联动）：异常被吞掉（catch 后不记录）→ 攻击无痕
```

> 💡 落地联动：日志体系见 [日志监控指标体系](../../07-工程运维基础/运维/日志监控指标/00-日志监控指标总览.md)；告警与 SLO 见同一体系。安全日志的保留策略按合规要求（等保/PCI）。

## 8. 防护清单总表

| 攻击 | 一句话防护 | 关键配置 |
|------|-----------|---------|
| XSS | 输出转义 + CSP + HttpOnly | CSP 头 / Cookie HttpOnly |
| CSRF | SameSite + Token 双保险 | Cookie SameSite=Lax |
| SQL 注入 | 参数化查询 + 白名单 | MyBatis `#{}` |
| SSRF | 出网白名单 + 内网段拦截 | Boot 4 InetAddressFilter |
| 越权 | 服务端属主校验 + 默认拒绝 | 查询带 user_id |
| 文件上传 | 重命名 + 白名单 + 隔离目录 | [Servlet 上传安全](../Servlet/07-文件上传下载与PartAPI.md) §7 |
| 认证 | 防爆破（限流）+ 登录轮换会话 | 429 限流 / changeSessionId |
| 配置错误 | 最小化暴露 + 定期扫描 | 关调试模式/默认口令 |
| 供应链 | 依赖扫描 + 锁版本 | Dependabot/SCA 工具 |
| 异常处理 | 不泄露堆栈 + 记录告警 | `include-stacktrace: never` |

> 🎯 **安全观总结**：安全不是"某个过滤器"，而是**纵深防御（Defense in Depth）**——每层防线独立生效：网络层（WAF/防火墙）→ 传输层（HTTPS/HSTS）→ 应用层（输入校验/权限）→ 数据层（加密/最小权限）→ 运营层（日志/告警）。单点防线可被绕过的前提是"只有一层"。

---

**下一模块**：[08-Web性能与浏览器机制](08-Web性能与浏览器机制.md) / **返回总览**：[00-Web高阶知识总览](00-Web高阶知识总览.md)
