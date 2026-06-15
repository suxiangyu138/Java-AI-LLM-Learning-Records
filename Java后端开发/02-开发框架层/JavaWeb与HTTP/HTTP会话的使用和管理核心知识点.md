# HTTP 会话的使用和管理核心知识点

> **文档定位**：Java 后端技术参考文档 | HTTP 会话管理  
> **核心问题**：HTTP 是无状态协议，服务器无法识别多次请求是否来自同一个用户  
> **解决方案**：会话（Session）用于在服务器端保存用户状态，实现多次请求之间的数据共享  
> **典型场景**：用户登录状态、购物车、验证码、个人信息

---

## 目录

- [一、会话核心概念](#一会话核心概念)
- [二、JavaWeb 中会话常用操作](#二javaweb-中会话常用操作)
- [三、会话超时配置](#三会话超时配置)
- [四、SessionID 传递方式](#四sessionid-传递方式)
- [五、四大作用域](#五四-大作用域)
- [六、会话安全要点](#六会话安全要点)
- [七、集群环境会话共享](#七集群环境会话共享)
- [八、Session 与 Cookie 区别](#八session-与-cookie-区别)
- [九、企业开发常用总结](#九企业开发常用总结)

---

## 一、会话核心概念

| 概念 | 说明 |
|------|------|
| **Session** | 服务器为每个用户单独开辟的存储空间，用于保存该用户的数据 |
| **SessionID** | 服务器为每个会话生成的唯一标识，用于区分不同用户 |
| **传递方式** | 默认通过 Cookie 传递 SessionID（`JSESSIONID`），浏览器每次请求自动携带 |
| **生命周期** | 第一次请求时创建，超时或主动注销时销毁 |

---

## 二、JavaWeb 中会话常用操作

```java
import javax.servlet.http.HttpSession;

// 1. 获取会话（无则自动创建）
HttpSession session = request.getSession();

// 2. 只获取现有会话（不创建新的）
HttpSession session = request.getSession(false);

// 3. 存储数据
session.setAttribute("key", value);

// 4. 获取数据
Object obj = session.getAttribute("key");

// 5. 删除数据
session.removeAttribute("key");

// 6. 销毁会话（退出登录）
session.invalidate();

// 7. 设置超时时间（秒）
session.setMaxInactiveInterval(1800);  // 30 分钟
```

---

## 三、会话超时配置

| 方式 | 配置 |
|------|------|
| **代码配置** | `session.setMaxInactiveInterval(1800);` |
| **web.xml 全局配置** | `<session-config><session-timeout>30</session-timeout></session-config>` |
| **Tomcat 全局默认** | `conf/web.xml` 中设置 `session-timeout` |

---

## 四、SessionID 传递方式

| 方式 | 说明 |
|------|------|
| **Cookie 传递（默认）** | 服务器通过 `Set-Cookie` 响应头返回 `JSESSIONID`，浏览器自动在请求头中携带 |
| **URL 重写（禁用 Cookie 时）** | 将 SessionID 拼接在 URL 后面：`String url = response.encodeURL("cart.jsp")` |

---

## 五、四大作用域

| 作用域（从小到大） | 有效范围 |
|-------------------|----------|
| **pageContext** | 当前页面有效 |
| **request** | 一次请求有效 |
| **session** | 一次会话有效（多次请求） |
| **application** | 整个应用运行期间有效 |

---

## 六、会话安全要点

| 安全措施 | 说明 |
|----------|------|
| **Cookie 设为 HttpOnly** | 防止 JS 读取 Cookie，防范 XSS 攻击 |
| **登录后重置 SessionID** | 避免会话固定攻击（Session Fixation） |
| **敏感操作校验登录** | 支付、修改密码、注销等操作必须验证登录状态 |
| **退出时销毁会话** | 必须调用 `session.invalidate()` |

---

## 七、集群环境会话共享

| 场景 | 问题 | 解决方案 |
|------|------|----------|
| 单机 Tomcat | Session 保存在本地内存，无问题 | — |
| 多台 Tomcat 集群 | Session 不互通，用户反复登录 | **Redis 统一存储 Session** |

> **企业常用方案**：Spring Session + Redis 实现跨服务器 Session 共享。

---

## 八、Session 与 Cookie 区别

| 对比维度 | Session | Cookie |
|----------|---------|--------|
| **存储位置** | 服务器 | 浏览器（客户端） |
| **安全性** | 高 | 低（可篡改） |
| **数据类型** | 可存对象 | 只能存字符串 |
| **容量** | 无限制（受服务器内存影响） | 约 4KB |
| **有效期** | 默认会话级 | 可长期存储 |

> **关系**：Cookie 保存 SessionID，Session 保存真实业务数据。

---

## 九、企业开发常用总结

| 规范 | 说明 |
|------|------|
| 登录信息/购物车/验证码 | 存入 Session |
| 登录 → 创建会话 | 退出 → 销毁会话 |
| 未登录拦截 | 使用 Filter 统一拦截未登录请求 |
| 集群项目 | 必须使用 Redis 实现 Session 共享 |
| 安全控制 | HttpOnly + 重置 SessionID + 超时控制 |
