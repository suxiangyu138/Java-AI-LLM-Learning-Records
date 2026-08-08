# 部署形态与 HTTPS 配置
> Jar vs War、SpringBootServletInitializer、SSL 证书与 HTTPS/HTTP2、压缩与线程池调优、优雅停机——"写好代码"到"安全上线"的最后一段路

## 📚 目录
1. [部署形态：Jar vs War](#1-部署形态jar-vs-war)
2. [SpringBootServletInitializer 外置部署](#2-springbootservletinitializer-外置部署)
3. [HTTPS 与 SSL 配置](#3-https-与-ssl-配置)
4. [HTTP/2 与 h2c](#4-http2-与-h2c)
5. [压缩与响应优化](#5-压缩与响应优化)
6. [线程池调优与虚拟线程](#6-线程池调优与虚拟线程)
7. [优雅停机与平滑发布](#7-优雅停机与平滑发布)

## 1. 部署形态：Jar vs War

| 维度 | 可执行 Jar（Boot 默认） | War + 外置容器 |
|------|:---:|:---:|
| 打包 | `mvn package` → boot jar | `mvn package` → war |
| 启动 | `java -jar app.jar` | 放入 Tomcat webapps |
| 容器 | 内嵌（版本锁定） | 外部容器（版本环境相关） |
| 部署粒度 | 单文件，可版本化 | 目录/文件 |
| 环境一致性 | ✅ 完全一致 | 容器版本差异风险 |
| 云原生 | ✅ 天然适合 | ❌ 与容器/K8s 理念冲突 |
| 适用 | **现代默认（2026）** | 存量企业规范/特殊运维要求 |

> 🎯 结论：**除非有强制理由（如企业要求共用容器管理），一律用 Jar**——单文件部署 + 版本锁定 + 云原生就绪，War 已是遗留形态。

## 2. SpringBootServletInitializer 外置部署

```java
// 需要 War 部署时：启动类继承 SpringBootServletInitializer
@SpringBootApplication
public class App extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);    // Jar 方式入口（保留）
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(App.class);          // War 方式入口（容器调用）
    }
}
```

```xml
<!-- pom.xml：打包方式改 war + 内嵌容器标记 provided -->
<packaging>war</packaging>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>          <!-- 外置容器提供，避免类冲突 -->
</dependency>
```

| 机制 | 说明 |
|------|------|
| 双入口 | main（Jar）+ configure（War），同一启动逻辑 |
| 容器如何发现 | 外部容器按 Servlet 3.0 SCI 机制找 `SpringBootServletInitializer`（onStartup 回调） |
| 潜在坑 | 外置容器版本 ≠ 开发版本（Servlet 特性漂移）；`context-path` 由容器 webapps 名决定 |
| Boot 4 注意 | starter 已更名 `spring-boot-starter-web-server-tomcat`，provided 排除时用新名 |

## 3. HTTPS 与 SSL 配置

### 3.1 证书准备（自签/内部用）

```bash
# 生成 PKCS12 证书（生产用正式证书：Let's Encrypt/云证书，流程同，文件格式一致）
keytool -genkeypair -alias demo -keyalg RSA -keysize 2048 \
  -validity 365 -storetype PKCS12 -keystore keystore.p12 \
  -dname "CN=api.example.com" -ext SAN=dns:api.example.com,dns:localhost
```

### 3.2 配置

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    protocol: TLS
    key-store: classpath:keystore.p12
    key-store-type: PKCS12
    key-store-password: changeit
    # 证书链与私钥分离（正式部署常见）：
    # certificate: classpath:fullchain.pem
    # certificate-private-key: classpath:privkey.pem
  http2:
    enabled: true                    # TLS 开启后 h2（ALPN）自动生效
```

```bash
# 验证
curl -k https://localhost:8443/actuator/health
openssl s_client -connect localhost:8443 -alpn h2   # 看 ALPN 协商出 h2
```

> ⚠️ **生产 SSL 建议放网关层**（Nginx/TLS 终止 + 证书自动续期），应用层 SSL 仅适合直连/内网场景——证书续期、TLS 版本升级都集中在网关，应用只跑明文。见 [Nginx SSL 与安全加固](../Nginx/10-Nginx-SSL与安全加固.md)。

### 3.3 反代后的正确姿势

```yaml
server:
  forward-headers-strategy: framework   # 信任代理传递的 X-Forwarded-*
  # 效果：request.getScheme()/isSecure()/getServerName() 返回客户端视角
  # ⚠️ 只在信任的反代后开启，否则伪造 X-Forwarded-* 可绕过安全判断
```

| 策略 | 说明 |
|------|------|
| `framework` | 解析 X-Forwarded-* 头（标准代理） |
| `native` | 容器原生支持（Tomcat RemoteIpValve） |
| `none`（默认） | 不处理 |

## 4. HTTP/2 与 h2c

| 场景 | 配置 | 说明 |
|------|------|------|
| h2 over TLS（主流） | `server.http2.enabled=true` + SSL | ALPN 协商，浏览器标准 |
| h2c（明文升级） | `server.http2.enabled=true`（无 SSL） | 需客户端显式升级（内部 RPC 用） |
| 网关场景 | 网关开 HTTP/2，应用可保持 HTTP/1.1 | 应用侧收益被网关截留 |

> 🎯 HTTP/2 收益：多路复用（并发请求不排队）、头部压缩（HPACK）、服务端推送（已废弃，见 [Servlet 6.1 演进](../Servlet/01-规范演进史与版本矩阵.md)）。**内网高并发 API 场景收益有限，重点在前端聚合请求**。

## 5. 压缩与响应优化

```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,text/html,text/css,application/javascript,image/svg+xml
    min-response-size: 1024     # 低于 1KB 不压（小响应压缩收益为负）
```

| 优化项 | 做法 | 收益 |
|--------|------|------|
| Gzip/Brotli | 上述配置；网关层做更优 | JSON 可压缩 70-85% |
| 响应瘦身 | Jackson 排除 null（`NON_NULL`） | 大列表收益明显 |
| 分页 | 列表必须分页 | 响应体线性下降 |
| 流式 | 大结果用 StreamingResponseBody（见 02 §8） | 内存恒定 |
| 静态缓存 | 见 [06 §3](06-静态资源与SPA路由.md) | 命中零流量 |

> ⚠️ 压缩与缓存联动：带 `ETag` 的响应压缩后 ETag 必须基于**压缩后内容**计算（Tomcat 自动处理）；压缩对 SSE 流式响应无效（逐帧发，无整包可压）。

## 6. 线程池调优与虚拟线程

```yaml
# 平台线程时代（传统）
server:
  tomcat:
    threads:
      max: 300                 # 依据压测：IO 密集可大、CPU 密集≈核数×2
      min-spare: 20
    accept-count: 200          # 排队上限（过大 → 响应延迟累积）
    max-connections: 10000
    connection-timeout: 10s    # 防慢速连接拖住连接数

# 虚拟线程时代（JDK 21+，推荐新项目）
spring:
  threads:
    virtual:
      enabled: true            # maxThreads 等平台线程参数自动失效
```

| 决策 | 说明 |
|------|------|
| 何时调 maxThreads | 先压测：看线程水位（Actuator `/actuator/metrics/tomcat.threads.busy`） |
| 何时上虚拟线程 | IO 密集 + JDK 21+（Boot 3.2+ 支持） |
| 调参铁律 | 每次只改一个参数 + 压测验证；盲调 = 反优化 |

> ⚠️ 线程池参数只是容器层；业务侧还有数据库连接池、HTTP 客户端连接池——**瓶颈通常在最短的木板**，先看慢调用再调线程。

## 7. 优雅停机与平滑发布

```yaml
server:
  shutdown: graceful            # 优雅停机（默认 immediate 直接断）
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s   # 每阶段最长等待
```

```text
优雅停机时序（K8s 滚动发布场景）：
  ① K8s 发 SIGTERM → Boot 收到
  ② 停止接收新请求（Tomcat 停止 accept）
  ③ 在途请求继续处理（最多等 30s）
  ④ Spring 容器按顺序销毁（Bean destroy → 连接池关闭）
  ⑤ 进程退出 → K8s 摘除 Pod
```

| 配套 | 说明 |
|------|------|
| 健康检查 | `/actuator/health`（readiness/liveness 分离，K8s 探针） |
| 预热 | 启动后预热（ApplicationRunner 加载缓存/连接池） |
| 双端口 | 管理端口与业务端口分离（`management.server.port`） |
| 发布顺序 | 先停流量 → 再停应用（K8s preStop hook 可配合） |

> 🎯 关联：完整发布策略（滚动/蓝绿/金丝雀）见 [CI/CD 与灰度发布体系](../../07-工程运维基础/运维/CI%20CD灰度发布/00-CI-CD与灰度发布总览.md)；可观测配置见 [SpringBoot 生产运维体系](../../06-Spring全家桶/SpringBoot/07-SpringBoot生产运维与可观测.md)。

---

**下一模块**：[08-生产实践与面试题](08-生产实践与面试题.md) / **返回总览**：[00-SpringBootWeb总览](00-SpringBootWeb总览.md)
