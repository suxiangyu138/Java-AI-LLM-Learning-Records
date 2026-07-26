# 02 - Tomcat 容器架构与部署

> 深入 Tomcat 内部：分层架构、连接器模型、类加载机制、调优参数与嵌入式部署——只有理解 Servlet 容器，才能真正驾驭 Java Web 应用。

---

## 目录

1. [Tomcat 概述与目录结构](#1-tomcat-概述与目录结构)
2. [Tomcat 顶层架构](#2-tomcat-顶层架构)
3. [Connector 连接器详解](#3-connector-连接器详解)
4. [Container 容器层次结构](#4-container-容器层次结构)
5. [请求处理完整流程](#5-请求处理完整流程)
6. [Tomcat 类加载机制](#6-tomcat-类加载机制)
7. [Tomcat 核心配置](#7-tomcat-核心配置)
8. [Tomcat 性能调优](#8-tomcat-性能调优)
9. [嵌入式 Tomcat 与 Spring Boot](#9-嵌入式-tomcat-与-spring-boot)
10. [Tomcat 集群与会话共享](#10-tomcat-集群与会话共享)
11. [常见面试题](#11-常见面试题)

---

## 1. Tomcat 概述与目录结构

### 1.1 Tomcat 定位

```
┌─────────────────────────────────────────────┐
│              Web 服务器层级                     │
│  ┌───────────────────────────────────┐       │
│  │   Web 服务器 (Apache/Nginx)        │ 静态资源 │
│  │   └─ 反向代理、负载均衡、SSL 终结     │       │
│  ├───────────────────────────────────┤       │
│  │   Servlet 容器 (Tomcat/Jetty)     │ 动态内容 │
│  │   └─ Servlet/JSP 运行、会话管理      │       │
│  ├───────────────────────────────────┤       │
│  │   Java 应用 (WAR 包)               │ 业务逻辑 │
│  │   └─ Spring MVC Controller、Service │       │
│  └───────────────────────────────────┘       │
└─────────────────────────────────────────────┘
```

> 🎯 Tomcat = Servlet 容器 + JSP 引擎 + Web 服务器（轻量）。生产环境通常 Nginx 前置 + Tomcat 后置。

### 1.2 安装目录结构

```
tomcat/
├── bin/              # 启停脚本
│   ├── startup.sh    # 启动（Linux）
│   ├── shutdown.sh   # 停止（Linux）
│   ├── catalina.sh   # 核心脚本（设置 JVM 参数等）
│   └── startup.bat   # Windows
├── conf/             # 配置文件
│   ├── server.xml    # ⭐ 核心配置（Connector、Host、Valve）
│   ├── web.xml       # 全局默认 web.xml（所有应用继承）
│   ├── tomcat-users.xml  # 管理用户
│   └── catalina.properties  # 系统属性
├── lib/              # 全局共享库（所有应用可见）
├── logs/             # 日志
│   ├── catalina.out  # 标准输出日志
│   ├── localhost.log # 单个虚拟主机日志
│   └── access_log    # 访问日志（需配置）
├── webapps/          # ⭐ 应用部署目录
│   ├── ROOT/         # 根应用（/）
│   ├── manager/      # 管理应用
│   └── docs/         # 文档
├── temp/             # 临时文件
└── work/             # JSP 编译后的 .java 和 .class 文件
```

---

## 2. Tomcat 顶层架构

### 2.1 组件层次图

```
                         Server (最顶层)
                           │
                    ┌──────┴──────┐
                    │    Service   │  (可多个，监听不同端口)
                    │   (Catalina) │
                    └──────┬──────┘
                ┌──────────┴──────────┐
                │                     │
           Connector               Container
         (HTTP/1.1 8080)          (Engine)
         (AJP   8009)                │
                              ┌──────┴──────┐
                              │    Host      │  (虚拟主机 localhost)
                              └──────┬──────┘
                              ┌──────┴──────┐
                              │   Context    │  (Web 应用 /app)
                              └──────┬──────┘
                              ┌──────┴──────┐
                              │   Wrapper    │  (单个 Servlet)
                              └─────────────┘
```

### 2.2 各级组件职责

| 组件 | 角色 | 数量 | 说明 |
|------|------|------|------|
| **Server** | 顶层容器 | 1个 | 代表整个 Tomcat 实例，管理 Service 集合 |
| **Service** | 服务层 | 1~N个 | 组合 Connector + Engine，一个 Service = 一组端口 |
| **Connector** | 连接器 | 1~N个 | 监听端口，解析协议，封装请求/响应 |
| **Engine** | 引擎 | 1个/Service | 处理所有请求，分发到对应 Host |
| **Host** | 虚拟主机 | 1~N个 | 按域名区分不同站点 |
| **Context** | 应用上下文 | 1~N个/Host | 一个 Web 应用（一个 war 包） |
| **Wrapper** | Servlet 包装器 | 1~N个/Context | 最底层，封装单个 Servlet |

> 💡 类比：Server = 大楼，Service = 楼层，Connector = 大门，Engine = 前台，Host = 公司，Context = 部门，Wrapper = 员工工位。

### 2.3 server.xml 配置示例

```xml
<Server port="8005" shutdown="SHUTDOWN">    <!-- shutdown 指令端口 -->

  <Service name="Catalina">

    <!-- HTTP/1.1 Connector -->
    <Connector port="8080"
               protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443"
               maxThreads="200"
               URIEncoding="UTF-8"/>

    <!-- AJP Connector (通常配合 Apache 使用) -->
    <Connector port="8009" protocol="AJP/1.3" redirectPort="8443"/>

    <Engine name="Catalina" defaultHost="localhost">

      <Host name="localhost" appBase="webapps"
            unpackWARs="true" autoDeploy="true">

        <!-- Context 可在此配置或通过 META-INF/context.xml -->
        <Context path="/app" docBase="myapp" reloadable="true"/>

        <!-- Access Log Valve -->
        <Valve className="org.apache.catalina.valves.AccessLogValve"
               directory="logs" prefix="localhost_access_log"
               suffix=".txt" pattern="%h %l %u %t &quot;%r&quot; %s %b"/>

      </Host>
    </Engine>
  </Service>
</Server>
```

---

## 3. Connector 连接器详解

### 3.1 协议与 I/O 模型

| 协议 | I/O 模型 | 适用场景 | 配置 |
|------|---------|---------|------|
| **HTTP/1.1** | NIO（默认） | 通用 HTTP | `protocol="HTTP/1.1"` |
| **HTTP/1.1** | NIO2 | 需要异步 I/O | `protocol="org.apache.coyote.http11.Http11Nio2Protocol"` |
| **HTTP/1.1** | APR | 高并发（需要本地库） | `protocol="org.apache.coyote.http11.Http11AprProtocol"` |
| **AJP/1.3** | NIO | Nginx/Apache 反向代理 | `protocol="AJP/1.3"` |

### 3.2 Connector 内部结构

```
Connector 内部：
┌──────────────────────────────────────────────────────┐
│  ProtocolHandler                                     │
│  ┌──────────────┐     ┌───────────────────┐         │
│  │   Endpoint    │────>│    Processor       │         │
│  │  (接收连接)    │     │ (解析协议→Request)  │         │
│  │              │     │                   │         │
│  │ Acceptor     │     │ Http11Processor   │         │
│  │ Poller       │     │                   │         │
│  │ SocketProcessor│   │                   │         │
│  └──────────────┘     └────────┬──────────┘         │
│                                │                     │
│                                ▼                     │
│                        ┌──────────────┐              │
│                        │   Adapter    │              │
│                        │ (Coyote→Catalina)│          │
│                        │ Request/Response │         │
│                        │ → HttpServletReq │         │
│                        └──────┬───────┘              │
└───────────────────────────────┼──────────────────────┘
                                │
                                ▼
                          Container (Engine → ... → Wrapper)
```

### 3.3 NIO Endpoint 三大组件

```java
// 1. Acceptor — 接收新连接
//    - 单线程，循环 accept() Socket 连接
//    - 将 SocketChannel 注册到 Poller 的队列

// 2. Poller — 轮询 I/O 事件
//    - 基于 Selector 的 NIO 轮询
//    - 检测到可读事件 → 交给 SocketProcessor

// 3. SocketProcessor — 处理具体请求
//    - 由线程池（Executor）执行
//    - 调用 Processor 解析 HTTP → 交给 Adapter → Container
```

### 3.4 连接器关键参数

```xml
<Connector port="8080" protocol="HTTP/1.1"
    maxThreads="200"           <!-- 最大工作线程数（默认200） -->
    minSpareThreads="10"       <!-- 最小空闲线程数 -->
    acceptCount="100"          <!-- 请求队列长度（默认100） -->
    connectionTimeout="20000"  <!-- 连接超时（毫秒，默认20s） -->
    maxConnections="10000"     <!-- 最大连接数（NIO默认10000） -->
    keepAliveTimeout="5000"    <!-- Keep-Alive 超时 -->
    maxKeepAliveRequests="100" <!-- Keep-Alive 最大请求数 -->

    <!-- 压缩 -->
    compression="on"
    compressionMinSize="2048"  <!-- 最小压缩字节数 -->
    compressibleMimeType="text/html,text/xml,text/plain,text/css,application/json"

    <!-- 异步 I/O -->
    useSendfile="true"         <!-- 大文件零拷贝传输 -->
/>
```

> 💡 `maxThreads` 的推荐公式：CPU 核心数 × 2 ~ 4。过高会导致线程切换开销大于业务处理开销。

---

## 4. Container 容器层次结构

### 4.1 责任链管道模式（Pipeline-Valve）

```
Engine Pipeline
  ├── Valve 1  (例如 AccessLogValve)
  ├── Valve 2
  └── Valve N
       │
       ▼ (Basic Valve 将请求传递到 Host Pipeline)
Host Pipeline
  ├── Valve 1  (例如 ErrorReportValve)
  └── Valve N
       │
       ▼ (Basic Valve → Context Pipeline)
Context Pipeline
  └── Valve →
       │
       ▼ (Basic Valve → Wrapper Pipeline)
Wrapper Pipeline
  └── Valve → 最终调用 Servlet.service()
```

```java
// 自定义 Valve — 记录每个请求耗时
public class TimingValve extends ValveBase {
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        long start = System.currentTimeMillis();
        try {
            getNext().invoke(request, response);  // 调用下一个 Valve
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            request.getContext().getLogger().info(
                request.getRequestURI() + " took " + elapsed + "ms");
        }
    }
}
```

> 💡 Valve 机制类似于 Servlet Filter 链，但工作在 Container 级别而非应用级别。Filter 在 Context 内部，Valve 可影响整个 Engine/Host。

### 4.2 Host — 虚拟主机

```xml
<!-- 多个域名指向不同应用 -->
<Engine name="Catalina" defaultHost="localhost">
    <Host name="www.mysite.com"  appBase="webapps/mysite">
        <Alias>mysite.com</Alias>
        <Context path="" docBase="ROOT"/>
    </Host>
    <Host name="admin.mysite.com" appBase="webapps/admin">
        <Context path="" docBase="ROOT"/>
    </Host>
</Engine>
```

### 4.3 Context — Web 应用

```xml
<!-- 三种 Context 配置方式 -->
<!-- 1. server.xml（不推荐：修改需重启） -->
<Context path="/app" docBase="/data/myapp" reloadable="true"/>

<!-- 2. META-INF/context.xml（打包在 war 中，推荐） -->
<Context reloadable="false" sessionCookieName="MYSESSIONID">
    <Resource name="jdbc/mydb" auth="Container"
              type="javax.sql.DataSource"
              maxTotal="100" maxIdle="30" maxWaitMillis="10000"
              username="root" password="secret"
              driverClassName="com.mysql.cj.jdbc.Driver"
              url="jdbc:mysql://localhost:3306/mydb"/>
</Context>

<!-- 3. 编程式（ServletContainerInitializer） -->
```

---

## 5. 请求处理完整流程

### 5.1 一次请求的完整路径

```
 1. 客户端发送 HTTP 请求
      │
      ▼
 2. Acceptor 接收 Socket 连接，注册到 Poller
      │
      ▼
 3. Poller 检测到 I/O 事件，创建 SocketProcessor
      │
      ▼
 4. 工作线程执行 SocketProcessor
      │
      ▼
 5. Http11Processor 解析 HTTP 协议
      → 构建 Coyote Request/Response 对象
      │
      ▼
 6. CoyoteAdapter 将 Coyote Request → HttpServletRequest
      → Coyote Response → HttpServletResponse
      │
      ▼
 7. Engine Valve Pipeline（记录日志等）
      │
      ▼
 8. Engine → Host(按域名匹配)
      │
      ▼
 9. Host → Context(按路径匹配)
      │
      ▼
10. Context → Wrapper(按 Servlet 映射匹配)
      │
      ▼
11. Filter Chain → Servlet.service() → doGet/doPost
      │
      ▼
12. 响应逐层返回 → Connector → Socket → 客户端
```

### 5.2 Mapper 路由组件

```java
// Mapper 是路由核心，维护 URL → Host → Context → Wrapper 的映射表
// 数据结构：Map<Host, Map<Context, Map<Path, Wrapper>>>

// 路由规则（按优先级）：
// 1. 精确匹配    /user/login   → Wrapper(userLoginServlet)
// 2. 前缀匹配    /user/*       → Wrapper(userServlet)
// 3. 扩展名匹配  *.do          → Wrapper(actionServlet)
// 4. 默认匹配    /             → Wrapper(defaultServlet)
// 5. 欢迎文件    /             → welcome-file-list
```

---

## 6. Tomcat 类加载机制

### 6.1 破坏双亲委派

```
Bootstrap ClassLoader (JVM 核心类)
         │
Extension ClassLoader (jre/lib/ext)
         │
Application ClassLoader (CLASSPATH)
         │
    ┌────┴────┐
    │ Common  │  ← $CATALINA_HOME/lib（所有应用 + Tomcat 共用）
    └────┬────┘
    ┌────┴────────────────────────┐
    │                             │
Catalina                       Shared
(Tomcat 内部类)                 ($CATALINA_BASE/shared/lib)
                                ┌────┴────┐
                                │         │
                            WebApp1   WebApp2
                          (/WEB-INF/lib /WEB-INF/classes)
```

### 6.2 关键原则

```
1. 可见性：下层可以访问上层 → WebApp 可以访问 Common
2. 不可见性：上层不能访问下层 → Common 不能访问 WebApp
3. 隔离性：WebApp 之间互相不可见 → WebApp1 看不到 WebApp2

好处：
  - Web 应用互相隔离（不同版本的 jar 不冲突）
  - 共享库放在 Common（如 JDBC 驱动放在 lib/）
  - 应用专属库放在 WEB-INF/lib（不会被其他应用影响）

⚠️ Spring Boot 嵌入式 Tomcat 的类加载不同！
  - 所有类由 Application ClassLoader 统一加载
  - 没有 WebApp 隔离，需要注意 jar 冲突
```

---

## 7. Tomcat 核心配置

### 7.1 JVM 参数配置

```bash
# setenv.sh (放在 $CATALINA_BASE/bin/ 下，Tomcat 启动时自动加载)

# 堆内存
CATALINA_OPTS="-Xms2048m -Xmx2048m"

# 新生代
CATALINA_OPTS="$CATALINA_OPTS -Xmn512m"

# GC 日志（JDK 8 用法）
CATALINA_OPTS="$CATALINA_OPTS -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
CATALINA_OPTS="$CATALINA_OPTS -Xloggc:$CATALINA_BASE/logs/gc.log"

# GC 选择
CATALINA_OPTS="$CATALINA_OPTS -XX:+UseG1GC"

# 内存溢出时 Dump
CATALINA_OPTS="$CATALINA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
CATALINA_OPTS="$CATALINA_OPTS -XX:HeapDumpPath=$CATALINA_BASE/logs/"

# MetaSpace
CATALINA_OPTS="$CATALINA_OPTS -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
```

### 7.2 web.xml 默认配置

```xml
<!-- $CATALINA_HOME/conf/web.xml 中的关键默认值 -->
<servlet>
    <servlet-name>default</servlet-name>   <!-- 处理静态资源 -->
    <servlet-class>org.apache.catalina.servlets.DefaultServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet>
    <servlet-name>jsp</servlet-name>       <!-- JSP 引擎 -->
    <servlet-class>org.apache.jasper.servlet.JspServlet</servlet-class>
</servlet>

<!-- Session 默认超时 -->
<session-config>
    <session-timeout>30</session-timeout>  <!-- 分钟 -->
</session-config>

<!-- MIME 类型映射 -->
<mime-mapping>
    <extension>json</extension>
    <mime-type>application/json</mime-type>
</mime-mapping>
```

---

## 8. Tomcat 性能调优

### 8.1 调优检查清单

| 调优维度 | 参数/措施 | 说明 |
|---------|----------|------|
| **JVM** | `-Xms -Xmx` 设为相同值 | 避免堆扩容带来的停顿 |
| **GC** | 使用 G1GC | JDK 9+ 默认，适合大堆 |
| **线程池** | `maxThreads` 200~500 | 取决于业务耗时 |
| **连接队列** | `acceptCount` 100~200 | 瞬时高峰缓冲 |
| **连接数** | `maxConnections` 10000 | NIO 连接不占线程 |
| **压缩** | `compression="on"` | 减少网络传输，CPU 换带宽 |
| **静态资源** | Nginx 处理静态资源 | Tomcat 只处理动态请求 |
| **APR** | 安装 native 库 | 高并发场景性能提升显著 |
| **超时** | `connectionTimeout` | 快速释放无效连接 |
| **关闭 AJP** | 注释 AJP Connector | 不配合 Apache 时不需要 |

### 8.2 Tomcat 8.5+ NIO vs NIO2 vs APR

```bash
# 性能对比（大致参考）
# NIO：    默认，成熟稳定
# NIO2：   异步 I/O，适合大量长连接
# APR：    本地代码，高并发下 TPS 可提升 20-30%，但需要安装 tomcat-native

# 推荐：
# - 通用场景 → NIO（默认即可）
# - 大量 WebSocket/长轮询 → NIO2
# - 极致性能 + 可运维 → APR（配合 Nginx）
```

### 8.3 线程池自定义

```xml
<!-- 使用自定义线程池替代默认 -->
<Executor name="tomcatThreadPool"
          namePrefix="catalina-exec-"
          maxThreads="400"
          minSpareThreads="25"
          maxIdleTime="60000"
          prestartminSpareThreads="true"/>

<Connector executor="tomcatThreadPool"
           port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"/>
```

---

## 9. 嵌入式 Tomcat 与 Spring Boot

### 9.1 Spring Boot 自动配置

```java
// Spring Boot 默认嵌入式 Tomcat 配置
// 在 application.yml 中覆盖：

server:
  port: 8080
  tomcat:
    uri-encoding: UTF-8
    max-threads: 200              # 工作线程池大小
    min-spare-threads: 10         # 最小空闲线程
    max-connections: 10000        # 最大连接数
    accept-count: 100             # 等待队列长度
    connection-timeout: 20000     # 连接超时(ms)
    # 基于 NIO，不需要 protocol 配置
  compression:
    enabled: true
    min-response-size: 2048
    mime-types: text/html,text/xml,text/plain,application/json
```

### 9.2 编程式定制

```java
@Bean
public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
    return factory -> {
        // 自定义 Connector
        factory.addConnectorCustomizers(connector -> {
            connector.setProperty("maxKeepAliveRequests", "100");
            connector.setProperty("keepAliveTimeout", "5000");
        });

        // 自定义 Context
        factory.addContextCustomizers(context -> {
            context.setSessionTimeout(30);
        });

        // 注册额外的 Servlet（替代 web.xml）
        factory.addServletRegistrations(
            new ServletRegistrationBean<>(new MyServlet(), "/custom/*"));
    };
}

// 切换容器（排除 Tomcat，使用 Jetty）
// <dependency>
//     <groupId>org.springframework.boot</groupId>
//     <artifactId>spring-boot-starter-jetty</artifactId>
// </dependency>
```

---

## 10. Tomcat 集群与会话共享

### 10.1 Session 复制 vs 集中存储

| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **Session 复制** | 节点间互相广播 Session | 无外部依赖 | 网络开销大，内存浪费 |
| **Session Sticky** | Nginx ip_hash 绑定节点 | 简单 | 节点宕机 Session 丢失 |
| **集中存储** | Redis/Memcached 存 Session | 高可用、可扩展 | 多一次网络调用 |
| **无状态 JWT** | 客户端持有 Token | 零服务端存储 | Token 不可撤销（需黑名单）|

```xml
<!-- Tomcat Session 复制配置（server.xml Cluster 部分） -->
<Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster">
    <Manager className="org.apache.catalina.ha.session.DeltaManager"
             expireSessionsOnShutdown="false"
             notifyListenersOnReplication="true"/>
    <Channel className="org.apache.catalina.tribes.group.GroupChannel">
        <Membership className="org.apache.catalina.tribes.membership.McastService"
                    address="228.0.0.4" port="45564"
                    frequency="500" dropTime="3000"/>
        <Receiver className="org.apache.catalina.tribes.transport.nio.NioReceiver"
                  address="auto" port="4000" autoBind="100"/>
        <Sender className="org.apache.catalina.tribes.transport.ReplicationTransmitter">
            <Transport className="org.apache.catalina.tribes.transport.nio.PooledParallelSender"/>
        </Sender>
    </Channel>
</Cluster>
```

> 🎯 现代化推荐：**Redis 集中存储 Session** 或 **JWT 无状态方案**。Tomcat 集群 Session 复制在生产中问题多（脑裂、网络抖动、内存浪费），已基本被淘汰。

---

## 11. 常见面试题

### Q1：Tomcat 的架构是怎样的？

> 顶层 Server → Service（Connector + Engine）→ Host → Context → Wrapper。Connector 处理网络连接和协议解析，Container 处理 Servlet 调用。详见第2节。

### Q2：Tomcat 如何处理一个 HTTP 请求？

> Acceptor 接收连接 → Poller 检测 I/O → 工作线程处理 → Processor 解析 HTTP → Adapter 封装 → Engine → Host → Context → Wrapper → Filter Chain → Servlet。详见第5节。

### Q3：Tomcat 的类加载机制有什么特点？

> 破坏双亲委派：先尝试 WebApp 自己加载 → 再委托父加载器。实现 Web 应用隔离和热部署。详见第6节。

### Q4：Tomcat 如何调优？

> JVM 参数调优（堆、GC）→ Connector 参数（线程数、队列）→ 静态资源分离（Nginx）→ Session 集中存储。详见第8节。

### Q5：Spring Boot 与独立 Tomcat 部署的区别？

> Spring Boot 内嵌 Tomcat，以 jar 包运行，类加载扁平化；独立 Tomcat 部署 war 包，多层类加载器隔离。Embedded Tomcat 更简单，适合微服务。
