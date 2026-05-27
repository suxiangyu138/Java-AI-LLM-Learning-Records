# Tomcat：部署器（理论+实战）

## 一、核心定位：Tomcat部署器的核心价值（后端必懂）

Tomcat部署器（Deployer）是Tomcat负责Web应用部署、卸载、更新的核心组件，衔接Tomcat容器（Engine/Host/Context）与文件系统，核心职责是将Web应用（WAR包、文件夹形式）解析为Tomcat可识别的Context容器，完成应用的初始化、启动、停止、卸载全生命周期管理。

对于Java后端开发而言，部署器直接关联：
1. 开发环境热部署（快速迭代代码）
2. 生产环境应用部署（WAR包部署、目录部署）
3. 应用更新与回滚（无停机更新、版本切换）
4. 多环境部署一致性（开发/测试/生产部署配置统一）

> **关键注意**：Tomcat部署器并非独立组件，而是集成在Host容器中，每个Host对应一个部署器，协同完成应用部署。

## 二、理论深度：部署器的架构原理、核心组件与生命周期

### 2.1 部署器的核心架构

| 组件 | 说明 |
|------|------|
| **顶层接口** | `org.apache.catalina.Deployer`，定义部署器的核心规范 |
| **核心实现类** | `org.apache.catalina.core.StandardHostDeployer`，集成在StandardHost中 |
| **ContextConfig** | 负责解析Web应用的配置文件（web.xml、context.xml） |
| **WarExtractor** | 负责解压WAR包，将WAR包解析为文件夹形式 |
| **DeployerListener** | 监听部署目录的文件变化，触发自动部署 |
| **WebAppClassLoader** | 为每个Web应用创建独立的类加载器，确保应用隔离 |

> 部署器的核心逻辑是「将应用资源转为Context容器」，所有部署操作本质上都是对Context容器生命周期的管理。

### 2.2 Tomcat部署方式详解

#### （1）自动部署（Auto Deploy）

- **核心特点**：部署器自动扫描appBase目录，检测到新增、修改、删除Web应用时自动执行部署/更新/卸载操作
- **配置方式**：`<Host ... autoDeploy="true">`
- **适用场景**：开发环境、测试环境

#### （2）手动部署（Manual Deploy）

- **核心特点**：手动将Web应用放入appBase目录，或通过Tomcat管理页面/命令行执行部署操作
- **操作方式**：
  - 将WAR包复制到appBase目录，重启Tomcat
  - 通过Tomcat Manager页面（`http://ip:port/manager`）上传WAR包
  - 通过命令行执行部署指令
- **适用场景**：**生产环境（推荐）**

#### （3）热部署（Hot Deploy）

- **核心特点**：应用运行时修改代码或配置文件，部署器自动检测变化并重新部署，无需重启Tomcat
- **配置方式**：需同时开启`Host的autoDeploy="true"`和`Context的reloadable="true"`
- **适用场景**：开发环境

> **关键注意**：热部署会销毁旧的Context容器和WebAppClassLoader，导致当前应用的会话丢失，**生产环境严禁开启热部署**。

#### （4）并行部署（Parallel Deploy）

- **核心特点**：同一应用部署多个版本，部署器同时启动多个Context容器，通过路径区分版本，实现无停机更新
- **配置方式**：在Context标签中设置version属性
- **适用场景**：生产环境无停机更新

### 2.3 部署器的生命周期（与Host、Context协同）

| 阶段 | 说明 |
|------|------|
| **初始化（init）** | Host启动时部署器初始化，加载autoDeploy、unpackWARs等配置 |
| **部署扫描（deployScan）** | 扫描Host的appBase目录，检测Web应用并执行部署 |
| **运行（running）** | 若autoDeploy为true，持续监控appBase目录变化 |
| **停止（stop）** | Host停止时，停止所有已部署的Context容器 |
| **销毁（destroy）** | Host销毁时，销毁自身及辅助组件 |

## 三、核心源码剖析

### 3.1 部署器核心实现

```java
public class StandardHostDeployer implements Deployer, Lifecycle {
    private Host host;
    private String appBase;
    private boolean autoDeploy;
    private boolean unpackWARs;

    @Override
    public void deploy(String path, String docBase) throws ServletException {
        // 1. 校验应用合法性
        if (!validateDocBase(docBase)) {
            throw new ServletException("应用部署路径无效：" + docBase);
        }
        // 2. 解压WAR包（若unpackWARs为true且是WAR包）
        if (unpackWARs && docBase.endsWith(".war")) {
            docBase = unpackWar(docBase);
        }
        // 3. 创建Context容器
        Context context = createContext(path, docBase);
        // 4. 初始化Context（解析web.xml等）
        initContext(context);
        // 5. 将Context添加到Host并启动
        host.addChild(context);
        context.start();
    }

    @Override
    public void undeploy(String path) throws ServletException {
        Context context = findDeployedApp(path);
        if (context == null) throw new ServletException("应用未部署：" + path);
        context.stop();
        host.removeChild(context);
        context.destroy();
    }

    @Override
    public void update(String path) throws ServletException {
        undeploy(path);                        // 卸载旧应用
        Context oldContext = findDeployedApp(path);
        deploy(path, oldContext.getDocBase()); // 重新部署
    }
}
```

### 3.2 核心源码关键点

- **deploy方法**：核心三部曲——创建Context → 初始化Context → 启动Context
- **update方法**：本质是"先卸载旧应用，再重新部署新应用"
- **应用隔离**：每个应用对应独立的Context和WebAppClassLoader，卸载时销毁

## 四、生产环境实战

### 4.1 生产环境标准配置

```xml
<Service name="Catalina">
    <Connector port="80" protocol="org.apache.coyote.http11.Http11NioProtocol"
               maxConnections="10000" maxThreads="500" connectionTimeout="20000"/>
    
    <Engine name="Catalina" defaultHost="www.xxx.com">
        <Host name="www.xxx.com" 
              appBase="webapps/www" 
              unpackWARs="true" 
              autoDeploy="false"           <!-- 生产环境关闭自动部署 -->
              deployOnStartup="true"       <!-- 启动时部署appBase下的应用 -->
              deployXML="true">            <!-- 解析应用的context.xml -->
            
            <Context path="" docBase="demo-web" reloadable="false"
                     allowLinking="false" privileged="false">
                <Resources cachingAllowed="true" cacheMaxSize="102400" />
                <Manager pathname="" />
            </Context>
        </Host>
    </Engine>
</Service>
```

### 4.2 生产环境核心参数

| 参数 | 生产建议 | 说明 |
|------|----------|------|
| `autoDeploy` | **false** | 关闭自动部署，避免误操作导致应用异常 |
| `unpackWARs` | **true** | 解压WAR包，提升启动速度和访问效率 |
| `deployOnStartup` | **true** | 启动时自动部署appBase下的应用 |
| `reloadable` | **false** | 关闭热部署，避免会话丢失和资源消耗 |
| `allowLinking` | **false** | 禁止符号链接，提升安全性 |
| `privileged` | **false** | 禁止应用访问Tomcat核心资源 |

### 4.3 生产环境部署流程（标准步骤）

1. **准备工作**：打包WAR包 → 备份旧应用 → 检查配置
2. **部署应用**：上传WAR包到appBase目录 → 重启Tomcat或通过Manager手动部署
3. **测试验证**：查看部署日志（catalina.out）→ 接口测试 → 性能测试
4. **上线与回滚**：测试通过后正式上线，异常时立即恢复旧应用备份

> **关键注意**：生产环境严禁直接覆盖旧应用文件，需先停止旧应用、备份，再部署新应用。

### 4.4 优化策略

| 优化维度 | 措施 |
|----------|------|
| **部署性能** | 开启WAR包解压、关闭autoDeploy和reloadable、配置资源缓存 |
| **安全性** | 禁止符号链接、限制应用权限、隐藏部署细节、校验应用合法性 |
| **可维护性** | 应用隔离（独立appBase）、日志优化、版本管理、自动化部署 |

### 4.5 Spring Boot整合

```yaml
# application.yml
server:
  port: 8080
  servlet:
    context-path: /api
  tomcat:
    auto-deploy: false
    unpack-wars: true
    basedir: /data/tomcat
    resources:
      caching-allowed: true
      cache-max-size: 102400
```

## 五、常见故障排查

| 故障 | 核心原因 | 解决方案 |
|------|----------|----------|
| **部署失败（docBase不存在）** | docBase路径错误或权限不足 | 检查路径和Tomcat运行用户权限 |
| **热部署失效** | autoDeploy或reloadable未开启 | 确认两个配置都开启，检查watchedResource范围 |
| **应用无法访问（404）** | Context的path配置错误 | 确认path与请求路径匹配 |
| **更新后内容未生效** | autoDeploy为false未触发更新 | 手动停止旧应用并重启Tomcat |
| **启动时部署缓慢** | unpackWARs为false或依赖过多 | 设为true，清理冗余依赖 |

## 六、总结

- Tomcat部署器是应用全生命周期管理的核心组件，集成在Host中
- 核心逻辑：将应用资源转为Context容器，实现部署、卸载、更新
- 生产环境核心：关闭自动部署和热部署、开启WAR包解压、应用隔离、安全优化
- **生产环境严禁开启热部署和自动部署**，应用部署前需备份，路径配置需准确

> 理解部署器的底层机制，能帮助快速定位部署相关异常，构建稳定、可维护的部署体系。
