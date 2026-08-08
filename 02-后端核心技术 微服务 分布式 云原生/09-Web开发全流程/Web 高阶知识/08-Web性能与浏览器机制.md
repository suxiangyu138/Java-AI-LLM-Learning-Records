# Web 性能与浏览器机制
> 请求生命周期、关键渲染路径、Core Web Vitals 指标体系、资源加载优化——从"用户点了链接"到"页面可以交互"的全链路性能地图

## 📚 目录
1. [请求生命周期：每一毫秒去哪了](#1-请求生命周期每一毫秒去哪了)
2. [关键渲染路径](#2-关键渲染路径)
3. [Core Web Vitals 指标体系](#3-core-web-vitals-指标体系)
4. [资源加载优化：async/preload/懒加载](#4-资源加载优化asyncpreload懒加载)
5. [渲染阻塞资源治理](#5-渲染阻塞资源治理)
6. [性能优化分层地图](#6-性能优化分层地图)
7. [度量与监控](#7-度量与监控)

## 1. 请求生命周期：每一毫秒去哪了

```text
用户点击 → DNS 解析 → TCP 握手 → TLS 握手 → 请求发送 → 服务器处理(TTFB 前)
→ 首包返回 → 资源下载 → 解析/渲染 → 可交互

时间线指标：
            DNS   TCP   TLS   请求   服务器处理   传输   解析渲染
            └─┘   └─┘   └─┘    └─┘     └───┘     └─┘    └────┘
            20ms  30ms  0-30ms  5ms     200ms     50ms    600ms
            （海外可达 200ms+ 每跳）
```

| 阶段 | 主导者 | 优化手段 |
|------|:---:|---------|
| DNS | 网络 | DNS 预解析（preconnect）、CDN 就近 |
| TCP/TLS | 网络 | HTTP/3（[04](04-HTTP2与HTTP3.md)）、TLS 1.3 会话恢复（[03](03-HTTPS与TLS深入.md)） |
| 请求发送 | 浏览器 | 多路复用（HTTP/2）、预连接 |
| 服务器处理（TTFB） | **后端** | 缓存/异步/慢查询治理 |
| 传输 | 网络/CDN | CDN 边缘、压缩 |
| 解析渲染 | 前端 | 关键渲染路径（§2） |

> 🎯 后端视角的结论：**你的性能责任是 TTFB 与下载体积**——TTFB 由服务端响应速度决定（缓存、DB、代码）；之后的时间是浏览器与网络的游戏。压测看 TTFB 分布，就是看后端性能。

## 2. 关键渲染路径

```text
HTML ──> DOM 树
          │           CSSOM 树 ──> 渲染树 ──> 布局(Layout) ──> 绘制(Paint) ──> 合成(Composite)
JS ────> 修改 DOM/CSSOM（解析被 JS 阻塞）
```

| 步骤 | 说明 | 阻塞因素 |
|------|------|---------|
| 解析 HTML → DOM | 边下载边构建 | **JS 同步执行阻塞解析** |
| 解析 CSS → CSSOM | CSS 全部下载完才构建 | **CSS 阻塞渲染**（render-blocking） |
| 渲染树 | DOM + CSSOM 合并 | - |
| 布局 | 计算几何位置 | 重排（Reflow）开销大 |
| 绘制/合成 | 像素上屏 | 合成层优化（GPU） |

> 🎯 核心规律：**CSS 阻塞渲染、JS 阻塞解析**——这就是"CSS 放 head、JS 放 body 末尾或 defer"的理论依据；也是首屏优化的第一课。

## 3. Core Web Vitals 指标体系

| 指标 | 度量什么 | 良好线 | 2026 说明 |
|------|---------|:---:|---------|
| **LCP**（Largest Contentful Paint） | 最大内容渲染（首屏主内容） | ≤ 2.5s | 加载性能 |
| **INP**（Interaction to Next Paint） | 交互响应（点击→画面变化） | ≤ 200ms | **替代 FID（2024 起）**，度量全周期交互 |
| **CLS**（Cumulative Layout Shift） | 布局稳定性（跳动） | ≤ 0.1 | 体验质量 |
| FCP | 首次内容渲染 | ≤ 1.8s | 辅助 |
| TTFB | 首字节 | ≤ 800ms | 后端责任 |
| TTI/TBT | 可交互时间 | - | 辅助 |

```text
2026 关键更新：INP 正式取代 FID（Interaction to Next Paint）
  FID 只度量"首次交互延迟"，INP 度量"整个会话所有交互的最差值"
  → 交互卡顿优化（长任务拆分）成为前端性能考核主项
```

> 🎯 后端联动：**LCP 主元素若是接口渲染的数据 → 后端 TTFB 直接决定 LCP**；INP 受慢接口拖累（点击等数据）→ 接口 ≤ 200ms 是硬指标。前后端性能是联动的，不是两摊事。

## 4. 资源加载优化：async/preload/懒加载

### 4.1 script 三形态

```html
<script src="app.js"></script>          <!-- 阻塞解析：下载+执行都阻塞 -->
<script async src="a.js"></script>      <!-- 下载不阻塞，下载完立即执行（乱序） -->
<script defer src="b.js"></script>      <!-- 下载不阻塞，解析完按序执行（推荐） -->
```

| 方式 | 执行时机 | 顺序 | 适用 |
|------|---------|:---:|------|
| 普通 | 遇即停 | - | 首屏关键（极少用） |
| async | 下载完即执行 | 乱序 | 独立脚本（统计/广告） |
| **defer** | HTML 解析完执行 | 保序 | **业务脚本默认** |

### 4.2 预加载家族

```html
<link rel="preconnect" href="https://api.example.com">   <!-- 提前建连（DNS+TCP+TLS） -->
<link rel="dns-prefetch" href="https://cdn.example.com"> <!-- 提前 DNS -->
<link rel="preload" href="/font.woff2" as="font">        <!-- 关键资源提前下载 -->
<link rel="prefetch" href="/next-page.js">               <!-- 空闲时预取下一页 -->
<link rel="modulepreload" href="/app.js">                <!-- ESM 模块预取 -->
```

| 指令 | 用途 | 滥用代价 |
|------|------|---------|
| preconnect | 首屏就要跨域请求 | 多开连接占资源（≤3 个） |
| preload | **当前页必须**的关键资源 | 下载不需要的 = 带宽浪费 |
| prefetch | 可能用到的未来资源 | 空闲带宽换延迟 |
| 懒加载 | 首屏外的图片/组件 | 需防"可视区没触发"的坑 |

> ⚠️ **preload vs prefetch 的语义差异**：preload = "现在就要，优先下载"；prefetch = "以后可能用，空闲下载"。用反了会拖慢首屏（prefetch 抢带宽）或首屏资源延迟（该 preload 的没标）。

### 4.3 图片与媒体

```html
<img src="photo.jpg" loading="lazy" decoding="async">   <!-- 懒加载 + 异步解码 -->
<picture>
    <source type="image/avif" srcset="photo.avif">      <!-- 现代格式优先 -->
    <source type="image/webp" srcset="photo.webp">
    <img src="photo.jpg" alt="...">
</picture>
<!-- 尺寸属性必填：width/height 防 CLS 跳动 -->
```

## 5. 渲染阻塞资源治理

| 资源 | 阻塞什么 | 治理 |
|------|---------|------|
| CSS | 渲染（必须构建完 CSSOM） | 首屏关键 CSS 内联 + 非关键异步加载 |
| 同步 JS | 解析（构建 DOM） | defer/async |
| 字体 | 文字渲染（FOIT 白屏） | `font-display: swap` + preload |
| 大图 | LCP 延迟 | 响应式图片 + 现代格式 + 懒加载 |

```css
/* 字体闪白治理：swap 先显示回退字体，字体到位后替换 */
@font-face {
    font-family: 'Icon';
    src: url('/fonts/icon.woff2') format('woff2');
    font-display: swap;
}
```

## 6. 性能优化分层地图

| 层 | 优化项 | 后端能做的 |
|----|--------|-----------|
| **网络层** | CDN、HTTP/3、预连接 | 静态资源上 CDN、开 HTTP/2/3 |
| **传输层** | 压缩、缓存头、版本化 | gzip/brotli、Cache-Control（[02](02-HTTP缓存体系.md)） |
| **后端层** | TTFB、吞吐 | 缓存（Redis）、异步化、慢查询治理、限流保护 |
| **前端层** | 渲染路径、体积 | 接口契约配合（首屏接口聚合/分页） |
| **体验层** | INP、CLS | 接口 ≤200ms、返回稳定结构 |

```text
一个典型优化案例（后端主导）：
  问题：LCP 3.5s（指标 2.5s）
  拆解：TTFB 1.2s（DB 慢查询 + 无缓存）→ 加缓存后 300ms
       资源下载 2.3s（无 CDN 无压缩）→ CDN + brotli 后 800ms
       总 LCP ≈ 1.6s ✅
```

> 🎯 方法论：**先度量再优化**——用指标定位瓶颈层，别盲猜。优化顺序永远是"最大的窟窿先补"（看瀑布图哪个阶段最长）。

## 7. 度量与监控

| 工具 | 用途 |
|------|------|
| **Lighthouse** | 实验室性能审计（性能/SEO/无障碍评分） |
| **Web Vitals**（JS 库） | 现场指标采集（上报 RUM） |
| Chrome DevTools Performance | 火焰图定位长任务 |
| Network 面板 | 瀑布图（缓存命中/TTFB/传输） |
| RUM（真实用户监控） | 线上持续度量（按地域/设备分桶） |
| 压测（JMeter/k6） | 容量与瓶颈验证（[JMeter 体系](../../07-工程运维基础/测试/Jmeter/00-JMeter知识体系总览.md)） |

> 💡 上线红线建议：LCP ≤ 2.5s、INP ≤ 200ms、CLS ≤ 0.1、TTFB ≤ 800ms——用 RUM 持续监控，性能回归像功能回归一样进 CI（Lighthouse CI）。

---

**下一模块**：[09-生产实践与面试题](09-生产实践与面试题.md) / **返回总览**：[00-Web高阶知识总览](00-Web高阶知识总览.md)
