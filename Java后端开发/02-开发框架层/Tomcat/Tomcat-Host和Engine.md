# Tomcat：Host和Engine（理论+实战）

## 一、核心定位：Engine与Host在Tomcat架构中的角色（后端必懂）

Tomcat的核心架构分为「连接器（Connector）」和「容器（Catalina）」，而Engine和Host是Catalina容器的核心组件，属于「层级容器」（Engine > Host > Context > Wrapper），核心作用是实现请求的层级分发、多应用/多域名部署隔离。

对于Java后端开发而言，Engine和Host的理解直接关联：
1. 多域名部署（如`www.xxx.com`、`api.xxx.com`对应同一Tomcat下的不同应用）
2. 虚拟主机配置（生产环境多应用共享Tomcat实例）
3. 请求路由故障排查（如请求找不到对应应用、域名跳转异常）

**核心区别与关联**：

| 组件 | 角色 | 核心职责 |
|------|------|----------|
| **Engine（引擎）** | Tomcat容器的顶层容器，一个Tomcat实例只有一个Engine | 管理所有Host，接收连接器传递的请求，分发到对应的Host处理 |
| **Host（虚拟主机）** | Engine的子容器，一个Engine可以包含多个Host | 每个Host对应一个「虚拟主机」（通常对应一个域名），管理该虚拟主机下的所有Web应用 |

> **核心类比**：Engine相当于"服务器机房"，Host相当于"机房里的每台物理服务器"，Context相当于"服务器上部署的每个应用"。

## 二、理论深度：Engine与Host的架构原理、生命周期

### 2.1 Tomcat容器层级回顾

Tomcat容器采用「四层层级结构」：Engine → Host → Context → Wrapper

- **Engine**：顶层容器，管理所有Host，统一接收Connector的请求
- **Host**：虚拟主机容器，管理当前虚拟主机下的所有Context
- **Context**：Web应用容器，管理当前应用的所有Servlet（Wrapper）
- **Wrapper**：Servlet容器，每个Wrapper对应一个Servlet

### 2.2 Engine的核心原理与特性

**核心职责**：
- **请求分发**：根据请求的「主机名（Host）」，将请求转发到对应的Host容器；若未找到匹配的Host，则转发到Engine的「默认Host」
- **全局配置**：统一管理所有Host的公共配置（全局错误页面、安全约束、日志配置）
- **容器管理**：启动、停止所有子Host容器，监控Host的生命周期

**核心特性**：
- **单实例性**：一个Tomcat实例只有一个Engine容器
- **默认Host**：Engine必须指定一个默认Host（通过`defaultHost`属性配置）
- **生命周期与Catalina绑定**：Tomcat启动时Engine启动，Tomcat停止时Engine停止

### 2.3 Host的核心原理与特性

**核心职责**：
- **虚拟主机管理**：每个Host对应一个虚拟主机，通过「主机名（name属性）」标识
- **应用管理**：管理当前虚拟主机下的所有Web应用（Context），包括部署、启动、停止
- **本地配置**：可配置当前Host的专属配置，覆盖Engine的全局配置

**核心特性**：
- **多实例性**：一个Engine可以包含多个Host，每个Host独立运行
- **应用隔离**：不同Host下的Web应用完全隔离
- **部署灵活性**：支持多种部署方式（自动部署、手动部署、热部署）

### 2.4 请求分发流程（Engine与Host协同工作）

1. 客户端发送HTTP请求，Connector接收请求，封装为Tomcat的Request/Response对象
2. Connector将Request/Response对象传递给Engine
3. Engine解析Request中的「主机名」，匹配自身管理的所有Host
4. 若找到匹配的Host，将Request转发给该Host；若未找到，转发给Engine的默认Host
5. Host解析Request中的「上下文路径」，匹配自身管理的所有Context
6. Context将Request转发给对应的Wrapper（Servlet容器），调用Servlet的service方法

> **易错点**：请求的Host字段必须与Tomcat中配置的Host的name属性完全匹配（不区分大小写），否则会被默认Host处理，这是"域名访问不到应用"的常见原因。

## 三、核心源码剖析（Java后端实战视角）

### 3.1 Engineer核心实现类

```java
public class StandardEngine extends ContainerBase implements Engine {
    private String defaultHost = "localhost";
    private Service service;

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        String hostName = request.getRequest().getServerName();
        Host host = (Host) findChild(hostName);
        if (host == null) {
            host = (Host) findChild(defaultHost);
        }
        host.invoke(request, response);
    }
}
```

### 3.2 Host核心实现类

```java
public class StandardHost extends ContainerBase implements Host {
    private String name;
    private String appBase = "webapps";
    private boolean autoDeploy = true;

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        String contextPath = request.getContextPath();
        Context context = (Context) findChild(contextPath);
        if (context == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        context.invoke(request, response);
    }
}
```

## 四、生产环境实战：Engine与Host配置

### 4.1 核心配置（server.xml完整示例）

```xml
<Service name="Catalina">
    <Connector 
        port="80" 
        protocol="org.apache.coyote.http11.Http11NioProtocol"
        maxConnections="10000"
        maxThreads="500"
        connectionTimeout="20000"
    />
    
    <Engine name="Catalina" defaultHost="www.xxx.com">
        <Valve className="org.apache.catalina.valves.AccessLogValve"
               directory="logs"
               prefix="localhost_access_log"
               suffix=".txt"
               pattern="%h %l %u %t &quot;%r&quot; %s %b" />
        
        <!-- 第一个Host：默认Host -->
        <Host name="www.xxx.com" appBase="webapps/www" unpackWARs="true" autoDeploy="false">
            <Context path="" docBase="demo-web" reloadable="false" />
            <Context path="/api" docBase="demo-api" reloadable="false" />
        </Host>
        
        <!-- 第二个Host：api.xxx.com -->
        <Host name="api.xxx.com" appBase="webapps/api" unpackWARs="true" autoDeploy="false">
            <Context path="" docBase="demo-api" reloadable="false" />
            <Valve className="org.apache.catalina.valves.AccessLogValve"
                   directory="logs/api"
                   prefix="api_access_log"
                   suffix=".txt"
                   pattern="%h %l %u %t &quot;%r&quot; %s %b" />
        </Host>
    </Engine>
</Service>
```

### 4.2 核心参数详解

#### Engine核心参数

| 参数 | 说明 |
|------|------|
| `name` | Engine的名称，默认Catalina |
| `defaultHost` | 默认Host的名称，必须与某个Host的name属性一致 |

#### Host核心参数（后端高频配置）

| 参数 | 默认值 | 说明 | 生产建议 |
|------|--------|------|----------|
| `name` | - | 虚拟主机名，必须与客户端请求的Host字段一致 | 配置正确的域名 |
| `appBase` | webapps | 该Host的应用部署目录 | 为每个Host配置独立的appBase |
| `unpackWARs` | true | 是否自动解压WAR包 | 设为true |
| `autoDeploy` | true | 是否自动部署 | **生产环境设为false** |
| `reloadable` | - | 是否开启热部署（影响该Host下所有应用） | **生产环境设为false** |

### 4.3 生产环境优化策略

- **应用隔离**：每个Host使用独立的appBase目录
- **日志隔离**：为每个Host配置独立的访问日志
- **关闭自动部署和热部署**：生产环境将autoDeploy和reloadable设为false
- **隐藏服务器信息**：配置ErrorReportValve，设置`showServerInfo="false"`
- **禁用默认Host的空应用**：删除默认Host下的默认应用（ROOT、docs、examples）

### 4.4 实战部署场景

#### 场景1：多域名对应同一应用

```xml
<Engine name="Catalina" defaultHost="www.xxx.com">
    <Host name="www.xxx.com" appBase="webapps/www">
        <Context path="" docBase="demo-web" reloadable="false" />
    </Host>
    <Host name="xxx.com" appBase="webapps/www">
        <Context path="" docBase="demo-web" reloadable="false" />
    </Host>
</Engine>
```

#### 场景2：禁止某个Host访问

```xml
<Host name="admin.xxx.com" appBase="webapps/admin">
    <Valve className="org.apache.catalina.valves.RemoteAddrValve" deny="*" />
    <Context path="" docBase="demo-admin" reloadable="false" />
</Host>
```

## 五、Spring Boot整合Tomcat的Engine与Host

### 5.1 默认配置

- **Engine**：默认名称为Catalina，默认Host为localhost
- **Host**：默认名称为localhost，appBase为webapps，autoDeploy默认false

### 5.2 自定义Engine与Host配置

```java
@Configuration
public class TomcatEngineHostConfig {
    @Bean
    public TomcatServletWebServerFactory tomcatServletWebServerFactory() {
        return new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                context.setReloadable(false);
                
                Engine engine = (Engine) context.getParent().getParent();
                engine.setName("Catalina-Demo");
                engine.setDefaultHost("www.xxx.com");
                
                Host host1 = new StandardHost();
                host1.setName("www.xxx.com");
                host1.setAppBase("webapps/www");
                host1.setAutoDeploy(false);
                
                AccessLogValve accessLogValve = new AccessLogValve();
                accessLogValve.setDirectory("logs/www");
                host1.addValve(accessLogValve);
                engine.addChild(host1);
            }
        };
    }
}
```

## 六、常见故障排查

| 故障 | 核心原因 | 解决方案 |
|------|----------|----------|
| **域名访问不到应用** | Host的name属性与请求Host字段不匹配 | 确认请求的Host字段与Host的name一致，或配置通配符Host |
| **Host启动失败** | Host配置错误（appBase目录不存在、name为空） | 查看catalina.out日志，检查Host配置 |
| **应用部署后无法访问** | 请求匹配到了错误的Host | 确认应用部署在正确的Host下 |
| **自动部署失效** | autoDeploy设为false | 开发环境设为true |

## 七、总结与进阶方向

- Engine和Host是Tomcat容器的顶层组件，核心作用是「请求层级分发」和「多应用/多域名隔离」
- 后端开发重点关注：Host的name属性（匹配域名）、appBase属性（应用部署目录）、autoDeploy属性
- 生产环境配置核心：多Host隔离部署、日志隔离、关闭自动部署和热部署、安全优化
- Spring Boot整合：内置Tomcat的Engine/Host配置适合简单场景，多域名部署优先结合Nginx反向代理
