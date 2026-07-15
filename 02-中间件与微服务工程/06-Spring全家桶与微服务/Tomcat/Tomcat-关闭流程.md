# Tomcat：关闭流程（俗称"关闭狗子"，理论 + 实战）

> **定位**：Tomcat 从接收关闭指令到彻底停止、释放所有资源的完整过程。核心逻辑：**自下而上终止组件生命周期（stop → destroy）**，与启动流程反向对称。

---

## 目录

1. [核心认知](#一核心认知)
2. [理论剖析：触发机制与核心流程](#二理论剖析触发机制与核心流程)
3. [实战操作](#三实战操作)
4. [总结](#四总结)

---

## 一、核心认知

### 1.1 本质定义

> "关闭狗子" = Tomcat 完整关闭流程：接收关闭指令 → 所有组件停止 → 所有资源释放 → 进程终止。

核心本质：**反向触发所有组件的生命周期终止**（`stop()` → `destroy()`），与启动流程（`init()` → `start()`）顺序完全相反。

### 1.2 关闭 vs 启动

| 维度 | 启动流程（init → start） | 关闭流程（stop → destroy） |
|------|------------------------|--------------------------|
| **执行顺序** | 自上而下（Server → Service → Connector → Container → Wrapper） | 自下而上（Wrapper → Container → Connector → Service → Server） |
| **核心目的** | 初始化组件、启动服务、准备接收请求 | 停止服务、释放资源、终止进程 |
| **关键操作** | 加载配置、实例化组件、启动线程池、监听端口 | 停止端口监听、处理剩余请求、销毁组件、释放线程/连接 |
| **异常影响** | 启动失败，应用无法部署 | 关闭异常 → 进程残留、资源泄漏、影响下次部署 |

### 1.3 后端开发核心关联

| 场景 | 说明 |
|------|------|
| **线上部署迭代** | 安全关闭 Tomcat，避免新旧进程冲突、端口占用 |
| **资源泄漏防控** | 关闭不完整 → 内存泄漏、线程泄漏（进程无法退出） |
| **异常应急处理** | 强制关闭/优雅关闭快速止损 |
| **自定义扩展** | 监听关闭流程，执行数据备份/资源清理 |

---

## 二、理论剖析：触发机制与核心流程

### 2.1 三种触发方式

| 方式 | 适用场景 | 说明 |
|------|----------|------|
| **脚本触发** ⭐ | 线下/线上部署最常用 | `shutdown.sh` / `shutdown.bat` → 向关闭端口（默认 8005）发送 `SHUTDOWN` 指令 |
| **API 触发** | 自定义关闭逻辑 | `server.stop()` → `server.destroy()`（Spring Boot 嵌入式场景） |
| **JVM 钩子触发** | 系统 `kill` 信号 | `CatalinaShutdownHook` 在 JVM 退出前自动执行关闭流程 |

```xml
<!-- server.xml：关闭端口和指令配置 -->
<Server port="8005" shutdown="SHUTDOWN">
```

### 2.2 完整关闭链路

```text
JVM 退出 / Kill 信号 / Shutdown 命令
  → CatalinaShutdownHook（钩子线程触发）
    → Catalina.stop()
      → Server.stop()
        → Service[n..0].stop()           // 自下而上，反向遍历
          → Connector[n..0].stop()       // ① 先停止接收新请求
          → Engine.stop()                // ② 再处理剩余请求
            → Host[n..0].stop()
              → Context[n..0].stop()
                → Wrapper[n..0].stop()   // 调用 Servlet.destroy()
      → Server.destroy()
        → 所有组件释放资源
  → JVM 退出
```

### 2.3 组件停止的核心逻辑

#### Server 层（总调度者）

```java
@Override
protected void stopInternal() throws LifecycleException {
    setState(LifecycleState.STOPPING_PREP);
    // 自下而上，反向遍历所有 Service
    for (int i = services.length - 1; i >= 0; i--) {
        services[i].stop();
    }
    stopAwait();  // 停止监听关闭端口（8005）
    setState(LifecycleState.STOPPED);
}
```

#### Service 层（先 Connector，后 Engine）

```java
@Override
protected void stopInternal() throws LifecycleException {
    setState(LifecycleState.STOPPING_PREP);
    // ① 先停止所有 Connector（停止监听端口，不再接收新请求）
    for (Connector connector : connectors) {
        connector.stop();
    }
    // ② 再停止 Engine（处理剩余请求后停止）
    if (engine != null) {
        engine.stop();
    }
    setState(LifecycleState.STOPPED);
}
```

#### Connector 层（停止请求入口）

| 操作 | 说明 |
|------|------|
| `endpoint.stop()` | 停止监听端口，关闭所有 TCP 连接 |
| `executor.shutdown()` | 停止线程池，等待工作线程处理完当前请求后销毁 |
| `adapter.destroy()` | 停止 Adapter，不再适配请求到 Container |

> ⚠️ 若线程池中有大量阻塞请求（如 DB 查询超时），Connector 关闭会阻塞 → Tomcat 关闭缓慢。

#### Container 层（自下而上销毁）

| 层级 | 关闭操作 |
|------|----------|
| **StandardWrapper** | 停止 Valve 管道 → 调用 `unloadServlet()` → `Servlet.destroy()` |
| **Context** | 递归停止所有子 Wrapper → 释放 Web 应用资源 → 触发 `contextDestroyed()` |
| **Host** | 停止所有子 Context |
| **Engine** | 停止所有子 Host |

### 2.4 两个核心机制

| 机制 | 说明 |
|------|------|
| **优雅关闭**（默认开启） | 先停止接收新请求 → 等待当前请求处理完成 → 最后销毁组件。**生产环境严禁 `kill -9`** |
| **资源释放** | 每个组件 `destroy()` 释放自身资源 + Server 释放全局资源 + 监听器辅助释放 |

---

## 三、实战操作

### 3.1 多种关闭方式

| 方式 | 命令 | 场景 |
|------|------|------|
| **脚本关闭** ⭐ | `./shutdown.sh` / `shutdown.bat` | 生产环境推荐 |
| **kill -15 优雅关闭** | `kill -15 <pid>`（发送 SIGTERM） | 等同脚本关闭，触发 JVM 钩子 |
| **Spring Boot 关闭** | 配置 `server.shutdown: graceful` | 嵌入式 Tomcat |
| **紧急强制关闭** | `kill -9 <pid>` | ⚠️ 应急使用，会丢请求 |

```yaml
# Spring Boot 优雅关闭配置
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### 3.2 关闭优化配置

```xml
<!-- server.xml -->
<Server port="8005" shutdown="MY_SHUTDOWN_CMD">  <!-- 自定义关闭指令 -->
    <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener"/>
    <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener"/>
    <Service name="Catalina">
        <Connector port="8080"
                   protocol="org.apache.coyote.http11.Http11NioProtocol"
                   shutdownTimeout="30000"/>  <!-- 等待剩余请求完成（ms）-->
    </Service>
</Server>
```

### 3.3 常见关闭异常排查

| 问题 | 现象 | 核心原因 | 解决方案 |
|------|------|----------|----------|
| **进程残留** | 脚本执行后进程未退出 | 存在非守护线程未停止 | `jstack` 查看线程栈，`destroy()` 中停止线程 |
| **关闭缓慢** | 关闭耗时 >2 分钟 | 线程池有阻塞请求 / 资源销毁复杂 | 增加 `shutdownTimeout`，优化 `destroy()` 方法 |
| **端口未释放** | 关闭后端口仍被占用 | 钩子执行不完整 / 连接未关闭 | 优雅关闭后等待 30 秒再重启 |
| **关闭失败** | 脚本无响应 | 关闭端口被占用 / 指令不匹配 | 检查 8005 端口 + `shutdown` 属性一致 |

#### 排查命令

```bash
# 进程残留排查
ps -ef | grep tomcat
jstack <pid> | grep -A 20 "BLOCKED|WAITING|TIMED_WAITING"
tail -100 catalina.out | grep -i "stop|destroy|shutdown"

# 端口占用排查
netstat -anp | grep 8005
```

### 3.4 自定义关闭钩子

```java
@Component
public class GracefulShutdownListener implements LifecycleListener {
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getLifecycle().getState() == Lifecycle.State.STOPPING) {
            try {
                // ① 关闭自定义线程池
                executor.shutdown();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                // ② 缓存数据备份
                backupCacheData();
                // ③ 通知监控系统
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

### 3.5 禁止非守护线程（防进程残留）

```java
// ❌ 错误：非守护线程，Tomcat 关闭后进程残留
Thread thread = new Thread(() -> {
    while (true) { /* 定时任务 */ }
});
thread.start();

// ✅ 正确 1：设为守护线程
thread.setDaemon(true);
thread.start();

// ✅ 正确 2：手动停止（在 destroy() 中）
@Override
public void destroy() {
    if (myThread != null && myThread.isAlive()) {
        myThread.interrupt();
    }
}
```

---

## 四、总结

| 要点 | 说明 |
|------|------|
| **本质** | 自下而上终止所有组件的 `stop()` → `destroy()`，释放所有资源 |
| **三种触发** | 脚本（最常用）/ API（自定义）/ JVM 钩子（kill -15） |
| **核心链路** | CatalinaShutdownHook → Server → Service → Connector → Engine → Host → Context → Wrapper |
| **生产铁律** | **严禁 `kill -9` 强制关闭**，应使用 `shutdown.sh` 或 `kill -15` 优雅关闭 |
| **避坑重点** | 非守护线程 → 进程残留；阻塞请求 → 关闭缓慢；资源未关闭 → 端口占用 |
| **优化方向** | 配置 `shutdownTimeout` + 自定义 LifecycleListener + 守护线程 |

> 优雅关闭是生产环境的基本要求——保障部署迭代无感知、资源无泄漏、数据无丢失。
