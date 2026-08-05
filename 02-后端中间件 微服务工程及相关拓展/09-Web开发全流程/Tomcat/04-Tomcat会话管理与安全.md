# 04 - Tomcat 会话管理与安全

> 定位：Session 机制、SSL/TLS 配置、认证 Realm 体系、安全最佳实践——Tomcat 安全面试核心

## 📚 目录

1. [Session 会话机制](#1-session-会话机制)
2. [Session 的分布式问题](#2-session-的分布式问题)
3. [SSL/TLS 配置](#3-ssltls-配置)
4. [认证 Realm 体系](#4-认证-realm-体系)
5. [安全最佳实践](#5-安全最佳实践)

---

## 1. Session 会话机制

### 1.1 工作原理

```
Session 机制（无状态 HTTP 的会话方案）：
  ① 首次请求 → 服务端创建 Session → 生成 JSESSIONID
  ② 响应 Set-Cookie: JSESSIONID=xxx
  ③ 后续请求携带 Cookie → 服务端匹配 Session

⚠️ 面试必答：
"HTTP 无状态 → Session 用 Cookie 携带
 JSESSIONID 维持会话；服务端存
 Session 数据（内存/持久化）。"
```

### 1.2 Session 的生命周期

```
创建：request.getSession(true)（首次）
失效三时机：
  ① 超时（默认 30 分钟，web.xml session-timeout）
  ② 主动失效（invalidate()）
  ③ 应用重启（内存 Session 丢失）

⚠️ 面试必答：
"Session 失效三时机——超时、invalidate、
 重启丢失（内存型）；登出必须调
 invalidate() 防会话固定攻击。"
```

```java
// Session 使用
HttpSession session = request.getSession();
session.setAttribute("user", user);       // 存
User user = (User) session.getAttribute("user");  // 取
session.invalidate();                     // 登出销毁

// Cookie 配置（web.xml）
<session-config>
    <session-timeout>30</session-timeout>
    <cookie-config>
        <http-only>true</http-only>       <!-- ⚠️ JS 不可读（防 XSS 窃取） -->
        <secure>true</secure>             <!-- 仅 HTTPS 传输 -->
    </cookie-config>
</session-config>
```

---

## 2. Session 的分布式问题

### 2.1 多节点会话不一致

```
集群部署问题：
  用户请求到 Node A 登录 → Session 在 A
  下次请求负载均衡到 Node B → 找不到 Session！

⚠️ 面试必答：
"集群 Session 问题是'会话黏性 vs 共享'——
 负载均衡换节点就丢登录态。"
```

### 2.2 三种解决方案

| 方案 | 原理 | 适用 |
|------|------|------|
| Session 黏性 | LB 固定同一节点 | 简单但单点故障 |
| Session 复制 | 节点间同步（Tomcat 集群） | 小集群（同步开销） |
| **集中存储** | Session 放 Redis/DB | ✅ 现代标准 |

```
⚠️ 现代实践（面试必答）：
  Spring Session + Redis：Session 存 Redis
  → 任意节点可服务、节点增减无感
  → 对比：Tomcat 自带集群复制（SimpleTcpCluster）
    同步开销大，已被 Redis 方案替代
```

```xml
<!-- Tomcat 自带集群（传统方案，了解即可） -->
<Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster">
    <Manager className="org.apache.catalina.ha.session.DeltaManager" />
    <Channel className="org.apache.catalina.tribes.group.GroupChannel" />
</Cluster>
<!-- 现代推荐：Spring Session Redis（应用层透明） -->
```

---

## 3. SSL/TLS 配置

### 3.1 证书生成与配置

```bash
# ① 生成自签名证书（生产用 CA 签发的证书）
keytool -genkeypair -alias tomcat \
    -keyalg RSA -keysize 2048 \
    -storetype PKCS12 \
    -keystore conf/keystore.p12 \
    -validity 3650

# ② 查看证书
keytool -list -keystore conf/keystore.p12 -storetype PKCS12
```

```xml
<!-- ③ server.xml 配置 HTTPS 连接器 -->
<Connector port="8443"
           protocol="org.apache.coyote.http11.Http11NioProtocol"
           SSLEnabled="true"
           maxThreads="150">
    <SSLHostConfig>
        <Certificate certificateKeystoreFile="conf/keystore.p12"
                     certificateKeystorePassword="changeit"
                     type="PKCS12" />
    </SSLHostConfig>
</Connector>
```

### 3.2 TLS 最佳实践

```
✅ TLS 1.2+（禁用 1.0/1.1）
✅ 证书密码不硬编码（环境变量/密管）
✅ HTTP 强制跳转 HTTPS（RewriteValve）
✅ HSTS 头（Strict-Transport-Security）
✅ 生产用 CA 证书（非自签名）

⚠️ 面试必答：
"HTTPS 三件套——证书（PKCS12）、
 连接器 SSLEnabled、HTTP→HTTPS 跳转；
 TLS 1.2 起步、HSTS 增强。"
```

---

## 4. 认证 Realm 体系

### 4.1 Realm 是什么

```
Realm = Tomcat 的认证领域（用户/角色存储）
  配合 web.xml 安全约束实现登录鉴权

内置 Realm：
  MemoryRealm（内存，演示）
  JDBCRealm / DataSourceRealm（数据库）✅ 生产
  JAASRealm（对接 JAAS）
```

### 4.2 配置示例（数据库 Realm）

```xml
<!-- server.xml：数据源 + Realm -->
<Realm className="org.apache.catalina.realm.DataSourceRealm"
       dataSourceName="jdbc/UserDB"
       userTable="users" userNameCol="username"
       userCredCol="password"
       userRoleTable="user_roles" roleNameCol="role" />
```

```xml
<!-- web.xml：安全约束（URL + 角色） -->
<security-constraint>
    <web-resource-collection>
        <web-resource-name>admin</web-resource-name>
        <url-pattern>/admin/*</url-pattern>
    </web-resource-collection>
    <auth-constraint>
        <role-name>admin</role-name>
    </auth-constraint>
</security-constraint>

<login-config>
    <auth-method>FORM</auth-method>     <!-- 表单登录（vs BASIC/DIGEST） -->
</login-config>
```

```
⚠️ 面试必答：
"Realm 是 Tomcat 的认证后端——
 数据库 Realm + 表单登录 + 安全约束
 是传统 Servlet 应用的鉴权方案；
 现代 Spring Security 更常用（应用层）。"
```

---

## 5. 安全最佳实践

### 5.1 安全清单

| 类别 | 措施 |
|------|------|
| 部署 | 关闭 autoDeploy、修改 shutdown 端口/密码 |
| 端口 | 禁用/内网化 AJP（8009） |
| 会话 | http-only + secure Cookie、登出 invalidate |
| 参数 | maxParameterCount 限制（防 HashDoS） |
| 加密 | TLS 1.2+、证书管理 |
| 目录 | 禁用目录列表（DefaultServlet listings=false） |
| 权限 | 最小权限运行（非 root） |
| 更新 | 及时升级（CVE 修复，如 11.0.22 安全更新） |

### 5.2 常见攻击与防御

| 攻击 | 防御 |
|------|------|
| XSS | Cookie http-only + 输出转义 |
| 会话固定 | 登录后 invalidate + 新 Session |
| HashDoS | maxParameterCount + 树化（JDK 8+） |
| 目录遍历 | 禁用 listings + 路径校验 |
| 慢速攻击 | connectionTimeout + maxKeepAliveRequests |
| 反序列化 | 升级版本 + 限制 Manager 访问 |

> 🎯 **要点**：Tomcat 安全 = **会话安全**（http-only Cookie + invalidate）+ **传输安全**（TLS）+ **认证安全**（Realm/Spring Security）+ **部署安全**（最小权限/端口/更新）。安全清单是面试"Tomcat 安全怎么做"的标准答案。

---

> 🎯 **核心要点**：会话与安全体系 = **Session 机制**（JSESSIONID + 三失效时机）+ **分布式会话**（Spring Session Redis 是现代答案）+ **TLS**（证书 + 跳转 + HSTS）+ **Realm 认证**（数据库 Realm）+ **安全清单**（端口/参数/更新）。"集群会话怎么解决"与"HTTPS 怎么配"是两大必考题。

---

**返回总览**：[00-Tomcat总览与核心概念](00-Tomcat总览与核心概念.md) | **上一篇**：[03-Tomcat连接器与并发模型](03-Tomcat连接器与并发模型.md) | **下一篇**：[05-Tomcat性能优化与故障排查](05-Tomcat性能优化与故障排查.md)
