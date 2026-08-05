# 02 - Tomcat 配置文件详解

> 定位：server.xml / web.xml / context.xml 三大配置文件全解析——连接器、线程池、虚拟主机、Servlet 映射、数据源

## 📚 目录

1. [配置文件总览](#1-配置文件总览)
2. [server.xml：全局配置](#2-serverxml全局配置)
3. [web.xml：应用配置](#3-webxml应用配置)
4. [context.xml：上下文配置](#4-contextxml上下文配置)
5. [配置实战：常见场景](#5-配置实战常见场景)

---

## 1. 配置文件总览

| 文件 | 位置 | 作用域 | 内容 |
|------|------|:---:|------|
| server.xml | conf/ | 全局 | 连接器、线程池、虚拟主机 |
| web.xml | conf/ + WEB-INF/ | 应用 | Servlet/Filter 映射、欢迎页 |
| context.xml | conf/ + META-INF/ | 应用上下文 | 数据源、JNDI、会话配置 |

```
⚠️ 配置优先级（同名配置覆盖）：
  全局 conf/web.xml < 应用 WEB-INF/web.xml
  全局 conf/context.xml < 应用 META-INF/context.xml

⚠️ 面试必答：
"三个配置文件分工——server.xml 管服务器、
 web.xml 管应用映射、context.xml 管资源。
 应用级覆盖全局级。"
```

---

## 2. server.xml：全局配置

### 2.1 核心结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Server port="8005" shutdown="SHUTDOWN">
    <!-- ① 全局监听器/资源 -->
    <Listener className="org.apache.catalina.startup.VersionLoggerListener" />

    <!-- ② 全局线程池（Executor） -->
    <Executor name="tomcatThreadPool"
              namePrefix="catalina-exec-"
              maxThreads="200"
              minSpareThreads="10"
              maxQueueSize="100" />

    <!-- ③ 服务（Service = Connector + Engine） -->
    <Service name="Catalina">

        <!-- ④ 连接器：HTTP（8080） -->
        <Connector port="8080"
                   protocol="org.apache.coyote.http11.Http11NioProtocol"
                   executor="tomcatThreadPool"
                   connectionTimeout="20000"
                   maxParameterCount="10000" />

        <!-- ⑤ 连接器：HTTPS（8443，需 SSL 证书） -->
        <Connector port="8443"
                   protocol="org.apache.coyote.http11.Http11NioProtocol"
                   SSLEnabled="true">
            <SSLHostConfig>
                <Certificate certificateKeystoreFile="conf/keystore.p12"
                             type="PKCS12" />
            </SSLHostConfig>
        </Connector>

        <!-- ⑥ 引擎 -->
        <Engine name="Catalina" defaultHost="localhost">

            <!-- ⑦ 虚拟主机 -->
            <Host name="localhost" appBase="webapps"
                  unpackWARs="true" autoDeploy="true">

                <!-- ⑧ 阀门（Valve） -->
                <Valve className="org.apache.catalina.valves.AccessLogValve"
                       pattern="%h %l %u %t &quot;%r&quot; %s %b" />

                <!-- ⑨ 应用上下文 -->
                <Context path="" docBase="ROOT" />
            </Host>

            <!-- ⑩ 集群（可选） -->
            <Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster" />
        </Engine>
    </Service>
</Server>
```

### 2.2 关键配置项

| 配置 | 说明 | 易错点 |
|------|------|--------|
| Server port="8005" | 关闭端口 | 生产应改或禁用 |
| Executor maxThreads | 最大线程数 | 默认 200，高并发调大 |
| Connector port | 端口 | 冲突排查 |
| protocol Nio | IO 模型 | NIO 默认 |
| connectionTimeout | 连接超时 | 20 秒默认 |
| maxParameterCount | 参数数上限 | **安全配置（防 HashDoS）** |
| Host appBase | 应用目录 | webapps |
| autoDeploy | 热部署 | 生产建议关闭 |

> 🎯 **要点**：server.xml 三大件——Executor（线程池）、Connector（连接器）、Host（虚拟主机）。`maxParameterCount` 是 2011 HashDoS 事件后的安全标配。

---

## 3. web.xml：应用配置

### 3.1 Servlet 映射

```xml
<!-- WEB-INF/web.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         version="6.0">

    <!-- ① Servlet 声明 -->
    <servlet>
        <servlet-name>hello</servlet-name>
        <servlet-class>com.example.HelloServlet</servlet-class>
        <!-- 启动时加载（数值越小越先） -->
        <load-on-startup>1</load-on-startup>
        <!-- 初始化参数 -->
        <init-param>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
    </servlet>

    <!-- ② Servlet 映射（URL → Servlet） -->
    <servlet-mapping>
        <servlet-name>hello</servlet-name>
        <url-pattern>/hello</url-pattern>
    </servlet-mapping>

    <!-- ③ Filter 声明与映射（顺序 = 声明顺序） -->
    <filter>
        <filter-name>auth</filter-name>
        <filter-class>com.example.AuthFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>auth</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <!-- ④ 欢迎页 -->
    <welcome-file-list>
        <welcome-file>index.jsp</welcome-file>
    </welcome-file-list>

    <!-- ⑤ 会话超时（分钟） -->
    <session-config>
        <session-timeout>30</session-timeout>
    </session-config>
</web-app>
```

### 3.2 URL 匹配规则

| 模式 | 匹配 | 示例 |
|------|------|------|
| 精确 | `/hello` | 完全匹配 |
| 路径前缀 | `/api/*` | /api/users |
| 扩展名 | `*.do` | /user.do |
| 默认 | `/` | 兜底（默认 Servlet） |

```
⚠️ 匹配优先级：
  精确 > 最长路径前缀 > 扩展名 > 默认

⚠️ 面试必答：
"URL 匹配四级——精确最高、默认兜底；
 Spring MVC 的 DispatcherServlet 映射 '/'
 就是默认级别（静态资源交给默认 Servlet）。"
```

---

## 4. context.xml：上下文配置

### 4.1 数据源配置（JNDI）

```xml
<!-- META-INF/context.xml -->
<Context>

    <!-- ① 数据源（连接池，生产必备） -->
    <Resource name="jdbc/UserDB"
              auth="Container"
              type="javax.sql.DataSource"
              maxTotal="20"
              maxIdle="10"
              maxWaitMillis="10000"
              username="root"
              password="secret"
              driverClassName="com.mysql.cj.jdbc.Driver"
              url="jdbc:mysql://localhost:3306/demo" />

    <!-- ② 环境参数 -->
    <Environment name="app.env" value="production" type="java.lang.String" />

    <!-- ③ 会话配置（管理器） -->
    <Manager pathname="" />       <!-- 禁用会话持久化 -->

</Context>
```

```java
// Java 侧获取数据源
Context initCtx = new InitialContext();
Context envCtx = (Context) initCtx.lookup("java:comp/env");
DataSource ds = (DataSource) envCtx.lookup("jdbc/UserDB");
Connection conn = ds.getConnection();
```

> 🎯 **要点**：数据源放 context.xml（JNDI）让容器管连接池——**Spring Boot 场景直接用 application.yml 的 HikariCP 即可**（context.xml 是传统部署方式）。

---

## 5. 配置实战：常见场景

### 5.1 场景一：多虚拟主机

```xml
<Engine name="Catalina" defaultHost="a.com">
    <!-- 两个域名 → 两个应用目录 -->
    <Host name="a.com" appBase="webapps-a">
        <Context path="" docBase="app-a" />
    </Host>
    <Host name="b.com" appBase="webapps-b">
        <Context path="" docBase="app-b" />
    </Host>
</Engine>
```

### 5.2 场景二：HTTPS 启用

```xml
<!-- ① 生成证书 -->
<!-- keytool -genkeypair -alias tomcat -keyalg RSA
     -storetype PKCS12 -keystore keystore.p12 -->

<!-- ② 连接器配置（见 2.1 ⑤） -->
<!-- ③ HTTP 自动跳 HTTPS（rewrite 阀） -->
<Valve className="org.apache.catalina.valves.rewrite.RewriteValve" />
<!-- conf/Catalina/localhost/rewrite.config:
     RewriteCond %{HTTPS} !=on
     RewriteRule ^(.*)$ https://%{HTTP_HOST}$1 [R,L] -->
```

### 5.3 配置检查清单

```
✅ 生产关闭 autoDeploy（防意外热部署）
✅ 修改 shutdown 端口/密码
✅ maxParameterCount 设置合理值
✅ 数据源用连接池（maxTotal 调优）
✅ HTTPS 证书有效 + 强制跳转
✅ 访问日志开启（AccessLogValve）
✅ 线程池大小与实际流量匹配
✅ 修改配置后重启验证（catalina.sh run 看日志）
```

> 🎯 **核心要点**：配置体系 = **server.xml**（服务器骨架：Executor/Connector/Host）+ **web.xml**（应用映射：Servlet/Filter 四规则）+ **context.xml**（资源：数据源/环境）。三文件 + 安全清单 = 配置实战的全部。

---

**返回总览**：[00-Tomcat总览与核心概念](00-Tomcat总览与核心概念.md) | **上一篇**：[01-Tomcat架构与请求处理流程](01-Tomcat架构与请求处理流程.md) | **下一篇**：[03-Tomcat连接器与并发模型](03-Tomcat连接器与并发模型.md)
