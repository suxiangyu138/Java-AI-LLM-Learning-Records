# Tomcat：关闭流程（俗称"关闭狗子"，理论+实战）

对于Java后端开发者而言，Tomcat的"关闭流程"是保障应用稳定性、避免资源泄漏、应对线上部署迭代的核心知识点。不同于Tomcat启动流程的"自上而下初始化"，关闭流程遵循"自下而上销毁"的核心逻辑，涉及Server、Service、Connector、Container等所有核心组件的生命周期终止。

## 一、核心认知：Tomcat"关闭狗子"的本质与后端关联价值

### 1.1 本质定义

Tomcat的关闭流程核心本质是：**反向触发所有组件的生命周期终止**（`stop()` → `destroy()`），与启动流程（`init()` → `start()`）的顺序完全相反，确保组件间的依赖关系被正确处理（如先停止接收请求，再处理剩余请求，最后释放资源）。

### 1.2 关闭流程与启动流程的核心区别

| 维度 | 启动流程 | 关闭流程 |
|------|----------|----------|
| **执行顺序** | 自上而下（Server → Service → Connector → Container → Wrapper） | 自下而上（Wrapper → Container → Connector → Service → Server） |
| **核心目的** | 初始化组件、启动服务 | 停止服务、释放资源、终止进程 |
| **关键操作** | 加载配置、实例化组件、启动线程池、监听端口 | 停止端口监听、处理剩余请求、销毁组件、释放线程/连接 |

## 二、关闭流程理论剖析：触发机制与核心流程

### 2.1 关闭流程的触发机制（3种核心方式）

#### 方式1：脚本触发（最常用）

通过`shutdown.sh`（Linux）/`shutdown.bat`（Windows）脚本触发，向Tomcat的关闭端口（默认8005）发送"SHUTDOWN"指令。

```xml
<Server port="8005" shutdown="SHUTDOWN">
    <!-- 其他组件配置 -->
</Server>
```

#### 方式2：API触发（自定义关闭）

```java
org.apache.catalina.Server server = tomcat.getServer();
server.stop();   // 触发关闭流程
server.destroy(); // 彻底销毁Server，释放所有资源
```

#### 方式3：JVM钩子触发

Tomcat启动时向JVM注册了关闭钩子（CatalinaShutdownHook），当JVM退出时自动执行关闭流程。

### 2.2 关闭流程的完整链路（自下而上）

```
JVM退出 / Kill信号 / Shutdown命令
  → CatalinaShutdownHook（钩子线程触发）
    → Catalina.stop()
      → Server.stop()
        → Service[n..0].stop()        // 自下而上反向遍历
          → Connector[n..0].stop()     // 先停止接收请求
          → Engine.stop()              // 再处理剩余请求
            → Host[n..0].stop()
              → Context[n..0].stop()
                → Wrapper[n..0].stop()
                  → Servlet.destroy()
      → Server.destroy()
        → 所有组件释放资源
  → JVM退出
```

### 2.3 组件停止的核心逻辑

#### Server组件停止

```java
@Override
protected void stopInternal() throws LifecycleException {
    setState(LifecycleState.STOPPING_PREP);
    // 自下而上，反向遍历
    for (int i = services.length - 1; i >= 0; i--) {
        services[i].stop();
    }
    stopAwait();
    setState(LifecycleState.STOPPED);
}
```

#### Service组件停止（先Connector，再Engine）

```java
@Override
protected void stopInternal() throws LifecycleException {
    setState(LifecycleState.STOPPING_PREP);
    // 1. 先停止所有Connector，停止接收新请求
    for (Connector connector : connectors) {
        connector.stop();
    }
    // 2. 再停止Engine，处理剩余请求后停止
    if (engine != null) {
        engine.stop();
    }
    setState(LifecycleState.STOPPED);
}
```

## 三、实战操作：关闭流程的使用、优化与异常排查

### 3.1 实战1：多种关闭方式

#### 方式1：脚本安全关闭（推荐生产环境）

```bash
# Linux
./shutdown.sh
ps -ef | grep tomcat  # 确认进程已退出

# Windows
shutdown.bat
```

#### 方式2：kill -15优雅关闭（等同脚本关闭）

```bash
ps -ef | grep tomcat
kill -15 <tomcat_pid>  # 发送SIGTERM信号，触发JVM钩子
```

#### 方式3：Spring Boot内置Tomcat关闭

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### 3.2 实战2：关闭优化配置

```xml
<!-- server.xml -->
<Server port="8005" shutdown="MY_SHUTDOWN_CMD">  <!-- 自定义关闭指令 -->
    <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener"/>
    <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener"/>
    
    <Service name="Catalina">
        <Connector port="8080" 
                   protocol="org.apache.coyote.http11.Http11NioProtocol"
                   shutdownTimeout="30000"/>  <!-- 等待剩余请求完成 -->
    </Service>
</Server>
```

### 3.3 实战3：常见关闭异常排查

| 问题 | 现象 | 核心原因 | 解决方案 |
|------|------|----------|----------|
| **进程残留** | 关闭脚本执行后进程未退出 | 存在非守护线程未停止 | jstack查看线程栈，优化关闭逻辑 |
| **关闭缓慢** | 关闭耗时超过2分钟 | 资源销毁逻辑复杂 | 增加shutdownTimeout，优化destroy方法 |
| **端口未释放** | 关闭后端口仍被占用 | 钩子执行不完整 | 优雅关闭后等待30秒再重启 |
| **关闭报错** | 日志中出现异常堆栈 | 某组件stop/destroy异常 | 检查catalina.out定位异常组件 |

#### 进程残留排查

```bash
ps -ef | grep tomcat
jstack <pid> | grep -A 20 "BLOCKED|WAITING|TIMED_WAITING"
tail -100 catalina.out | grep -i "stop|destroy|shutdown"
```

### 3.4 实战4：自定义关闭钩子

```java
@Component
public class GracefulShutdownListener implements LifecycleListener {
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getLifecycle().getState() == Lifecycle.State.STOPPING) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                backupCacheData();
                notifyMonitorSystem();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void backupCacheData() { /* 缓存数据备份 */ }
    private void notifyMonitorSystem() { /* 通知监控系统 */ }
}
```

## 四、总结：Java后端视角下的核心要点

- Tomcat关闭流程遵循"自下而上"的销毁原则，确保组件依赖关系被正确处理
- 关闭流程本质是生命周期管理的反向执行：`stop()` → `destroy()` → 释放所有资源
- **生产环境严禁使用`kill -9`强制关闭**，应使用`shutdown.sh`或`kill -15`优雅关闭
- 合理配置shutdownTimeout参数，给足时间处理剩余请求
- 核心排查点：进程残留（非守护线程）、关闭缓慢（长耗时操作）、端口未释放（资源泄漏）
- 自定义关闭逻辑推荐使用Tomcat生命周期监听器，在STOPPING状态执行清理逻辑

> 优雅关闭是生产环境的基本要求，保障部署迭代无感知、资源无泄漏、数据无丢失。
