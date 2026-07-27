# 09 - JavaWeb 性能优化

> Web 性能优化是用户体验和服务器成本的交叉点。本章从连接池、缓存、压缩、静态资源四个维度，系统覆盖 JavaWeb 性能优化的完整方案。

---

## 目录

1. [连接池优化](#1-连接池优化)
2. [HTTP 缓存策略](#2-http-缓存策略)
3. [响应压缩与静态资源](#3-响应压缩与静态资源)
4. [Tomcat 线程池调优](#4-tomcat-线程池调优)

---

## 1. 连接池优化

```java
// HikariCP 配置（Spring Boot 默认，也是最快的连接池）
// application.yml
spring.datasource.hikari:
  maximum-pool-size: 20        # 最大连接数
  minimum-idle: 5              # 最小空闲连接
  idle-timeout: 300000         # 空闲超时（5分钟）
  max-lifetime: 1800000        # 连接最大存活（30分钟，需 < 数据库 wait_timeout）
  connection-timeout: 30000    # 等待连接超时
  pool-name: AppPool
```

```text
池大小计算公式：
  connections = ((core_count * 2) + effective_spindle_count)

  实际建议（经验值）：
    开发环境: 5-10
    小流量服务: 10-20
    中等流量: 20-50
    大流量: 使用连接池监控动态调整

监控指标：
  → 活跃连接数 / 最大连接数 持续 > 80% → 增大池
  → 连接等待时间 P95 > 100ms → 增大池或优化 SQL
```

## 2. HTTP 缓存策略

```java
// 服务端控制缓存（Servlet）
response.setHeader("Cache-Control", "public, max-age=3600");  // 缓存 1 小时
response.setHeader("ETag", "\"" + md5(content) + "\"");       // 内容指纹
response.setDateHeader("Last-Modified", file.lastModified());

// 条件请求处理（304 Not Modified）
long lastModified = request.getDateHeader("If-Modified-Since");
if (lastModified != -1 && lastModified >= file.lastModified()) {
    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);  // 304
    return;  // 不传输 body，节省带宽
}
```

```text
缓存层次（由近到远）：
┌────────────┐
│ 浏览器缓存  │  Cache-Control: max-age → 命中直接跳过网络
├────────────┤
│ CDN 缓存    │  静态资源（JS/CSS/图片）CDN 边缘节点缓存
├────────────┤
│ 反向代理缓存 │  Nginx proxy_cache → 缓存动态页面
├────────────┤
│ 应用层缓存  │  Redis / Caffeine → 缓存数据库查询结果
└────────────┘
```

## 3. 响应压缩与静态资源

```java
// Gzip 压缩 Filter（减少传输量 70-80%）
@WebFilter("/*")
public class GzipFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String acceptEncoding = request.getHeader("Accept-Encoding");
        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
            GzipResponseWrapper wrapper = new GzipResponseWrapper(response);
            response.setHeader("Content-Encoding", "gzip");
            chain.doFilter(req, wrapper);
            wrapper.finish();  // GZIPOutputStream 写尾
        } else {
            chain.doFilter(req, res);
        }
    }
}
```

```text
静态资源优化：
  → 版本号 + 强缓存:  style.v2.css → Cache-Control: max-age=31536000
  → 资源合并: 多个 JS → 一个 bundle.js（减少 HTTP 请求数）
  → 图片优化: WebP 格式（比 JPG 小 30%）+ 懒加载
  → CDN: 静态资源全部走 CDN（用户就近访问）
```

## 4. Tomcat 线程池调优

```xml
<!-- server.xml -->
<Connector port="8080" protocol="HTTP/1.1"
    maxThreads="200"              <!-- 最大工作线程数（默认 200） -->
    minSpareThreads="25"          <!-- 最小空闲线程 -->
    acceptCount="100"             <!-- 等待队列长度 -->
    connectionTimeout="20000"     <!-- 连接超时（ms） -->
    maxConnections="10000"        <!-- 最大连接数 -->
    enableLookups="false"         <!-- 禁用 DNS 查询 -->
    compression="on"              <!-- 开启压缩 -->
    compressionMinSize="2048"     <!-- 最小压缩阈值 -->
/>
```

```text
监控指标：
  → 当前线程数 / maxThreads > 80% → 增加 maxThreads
  → acceptCount 持续有值 → 处理不过来，需要扩容
  → 平均响应时间 > 目标值 → 优化业务逻辑 / 加缓存

Tomcat vs Nginx vs Spring Boot：
  Spring Boot 内嵌 Tomcat → 直接调优 yml 参数
  Nginx 前置 → 静态资源/SSL/Gzip 在 Nginx 层处理
  大流量 → Nginx(反向代理) + Tomcat(应用服务器) 分离
```

## 核心要点回顾

- 连接池：HikariCP 默认最佳，20-50 连接覆盖大部分场景
- 缓存链：浏览器 → CDN → Nginx → Redis → DB
- Gzip 压缩：文本类响应减少 70-80%，CPU 开销可忽略
- 静态资源：版本号 + 强缓存 + CDN = 秒开
- Tomcat：200 线程 + 100 队列适合大多数中小服务

## 参考资料

1. HikariCP 官方文档 — github.com/brettwooldridge/HikariCP
2. HTTP 缓存规范 — MDN
3. Tomcat 性能调优指南
