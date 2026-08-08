# IIS 与 Windows 生态
> Windows 世界的官方 Web 服务器：集成管道架构、web.config、ARR 反向代理、与 ASP.NET Core 的托管模式——以及它为何在 Linux 时代仍占一席

## 📚 目录
1. [IIS 现状与定位](#1-iis-现状与定位)
2. [集成管道架构](#2-集成管道架构)
3. [web.config：站点级配置](#3-webconfig站点级配置)
4. [URL Rewrite 与 ARR 反向代理](#4-url-rewrite-与-arr-反向代理)
5. [与 ASP.NET Core 的托管](#5-与-aspnet-core-的托管)
6. [安全要点](#6-安全要点)
7. [对比与适用场景](#7-对比与适用场景)

## 1. IIS 现状与定位

| 事实 | 数值 |
|------|------|
| 当前版本 | IIS 10（Windows Server 2016/2019/2022）、IIS 10.0 内置 Server 2025 |
| 份额 | Netcraft 全量约 5.8%（2026），Windows 环境垄断 |
| 定位 | Windows Server 内置、与 .NET 深度绑定 |
| 独特能力 | 与 AD 域认证（Windows 集成认证）、.NET 托管原生 |

> 🎯 一句话：IIS 不是"最强的 Web 服务器"，而是"**Windows 生态的标配**"——企业在 Windows 域环境（AD 认证、SharePoint、老 ASP.NET）里绕不开它；纯 Linux 团队基本不接触。

## 2. 集成管道架构

```text
IIS 7+ 的集成管道（Integrated Pipeline）：
请求 → 内核驱动（http.sys）→ 模块管道 → 应用
                │
                ├─ 原生模块（C++，如静态文件、认证、压缩）
                └─ 托管模块（.NET，如 ASP.NET 管道）
```

| 特性 | 说明 |
|------|------|
| **http.sys** | 内核态 HTTP 监听器（Windows 内核的一部分）——性能与稳定性根基 |
| **集成管道** | 原生 + 托管模块统一在一个管道（IIS 6 是两套管道） |
| **应用池（App Pool）** | 站点隔离单元：一个池一个 w3wp.exe 进程，崩溃隔离 + 回收策略 |
| **模块化** | 按需安装模块（类似 Apache LoadModule） |

> 🎯 核心设计：**http.sys 内核监听 + 应用池进程隔离**——这就是 IIS 的并发底座（内核处理连接，用户态进程只处理业务），也是"w3wp.exe 崩溃"故障的由来（一个池一个进程）。

## 3. web.config：站点级配置

```xml
<!-- IIS 的 .htaccess/web.xml 混合体：站点级配置 -->
<configuration>
  <system.webServer>
    <!-- 默认文档 -->
    <defaultDocument>
      <files><add value="index.html" /></files>
    </defaultDocument>

    <!-- HTTP 响应头 -->
    <httpProtocol>
      <customHeaders>
        <remove name="X-Powered-By" />
        <add name="X-Content-Type-Options" value="nosniff" />
      </customHeaders>
    </httpProtocol>

    <!-- 压缩 -->
    <urlCompression doStaticCompression="true" doDynamicCompression="true" />
  </system.webServer>
</configuration>
```

| 特性 | 说明 |
|------|------|
| 层级 | machine.config → applicationHost.config → web.config（逐级覆盖） |
| 部署 | 随应用走（开发可自管，运维可锁） |
| 相当于 | Apache 的 .htaccess + Nginx 的 server 块 |
| 与 .NET 融合 | ASP.NET 配置共用同一 web.config |

> 💡 运维友好点：**web.config 随应用目录部署**——发布即配置生效，这比"改 Nginx 配置要重启/reload"更适合企业发布流程。

## 4. URL Rewrite 与 ARR 反向代理

```xml
<!-- URL Rewrite（IIS 版 mod_rewrite）：规则化重写 -->
<rewrite>
  <rules>
    <rule name="SPA history 路由" stopProcessing="true">
      <match url="^(.*)$" />
      <conditions>
        <add input="{REQUEST_FILENAME}" matchType="IsFile" negate="true" />
        <add input="{REQUEST_FILENAME}" matchType="IsDirectory" negate="true" />
      </conditions>
      <action type="Rewrite" url="/index.html" />
    </rule>
  </rules>
</rewrite>

<!-- ARR（Application Request Routing）：官方反向代理模块 -->
<rewrite>
  <rules>
    <rule name="API 反代" stopProcessing="true">
      <match url="^api/(.*)" />
      <action type="Rewrite" url="http://backend:8080/api/{R:1}" />
    </rule>
  </rules>
</rewrite>
```

| 模块 | 能力 | 类比 |
|------|------|------|
| URL Rewrite | URL 重写/重定向 | mod_rewrite |
| **ARR** | 反向代理 + 负载均衡 + 缓存 | Nginx 反代 |
| WebSocket 代理 | 3.0+ 支持 | 原生反代 |
| 缓存 | 静态/动态缓存 | Nginx proxy_cache |

> ⚠️ 印象纠偏：**IIS 也能反代**——ARR 就是微软官方版 Nginx（负载均衡、健康检查、URL 重写一应俱全），只是配置依赖 GUI/XML、生态文档远不如 Nginx。Windows 存量架构用它，新架构直接上 Nginx（Windows 版）或云网关。

## 5. 与 ASP.NET Core 的托管

```text
ASP.NET Core 在 IIS 的两种托管模式：
① In-process（默认）：应用 DLL 直接加载进 w3wp.exe
   · 性能最好（无进程间开销）
   · 崩溃即池崩溃（应用池回收兜底）

② Out-of-process：Kestrel 独立进程 + IIS 反代（http.sys → Kestrel）
   · 隔离性更好
   · 多一层进程间通信（轻微性能损失）
```

| 托管模式 | 进程 | 性能 | 隔离 | 适用 |
|---------|------|:---:|:---:|------|
| In-process | 共享 w3wp.exe | 最佳 | 弱 | 默认推荐 |
| Out-of-process | Kestrel 独立 | 略低 | 强 | 特殊隔离需求 |

> 🎯 关联：这本质是 **"Web 服务器反代到应用服务器"** 的 Windows 版——Kestrel 是 ASP.NET Core 的"内置 Tomcat"，IIS 是"Windows 版 Nginx"。与 [05-Web服务器与应用服务器分工](05-Web服务器与应用服务器分工.md) 的 Java 版（Nginx → Tomcat）完全同构。

## 6. 安全要点

| 项 | 做法 |
|----|------|
| 历史漏洞 | IIS 是 Windows 攻击面重点（历届漏洞多）——**及时打补丁**是第一要务 |
| 移除冗余头 | 移除 `X-Powered-By` / `Server` 头（信息泄露） |
| 应用池权限 | 最小权限账户运行 w3wp.exe（勿用 LocalSystem） |
| 目录权限 | 上传目录禁脚本执行（.NET 处理程序映射隔离） |
| 方法限制 | 禁 PUT/DELETE 等方法（WebDAV 默认关闭） |
| 管理面 | 远程管理（RDP/Web Deploy）走 VPN/堡垒机 |

> ⚠️ 与 Linux 服务器对比的运维现实：**IIS 补丁节奏绑定 Windows 更新**（补丁星期二），CVE 暴露窗口管理更依赖企业补丁流程；Docker/Linux 团队的加固经验不完全适用。

## 7. 对比与适用场景

| 维度 | IIS | Nginx | Apache |
|------|:---:|:---:|:---:|
| 平台 | Windows | 跨平台 | 跨平台 |
| 配置 | GUI + XML | 文本 | 文本 |
| 内核监听 | ✅ http.sys | 用户态 | 用户态 |
| 反代 | ARR | ✅ 强 | mod_proxy |
| .NET 集成 | **原生** | 一般 | 一般 |
| 域认证 | **AD 集成原生** | 需扩展 | 需扩展 |
| 份额趋势 | 平稳 | 领先 | 下滑 |

| 场景 | 选型 |
|------|------|
| Windows 企业 + AD 域环境 | **IIS**（集成认证无可替代） |
| ASP.NET / SharePoint 存量 | **IIS** |
| ASP.NET Core 新项目 | Kestrel +（IIS/Nginx 反代） |
| Linux/容器环境 | Nginx / Caddy |
| 混合架构 | 网关统一入口，后端异构 |

> 🎯 结论：**选 IIS 的唯一强理由是"Windows + AD + .NET 存量"**；新项目在 Windows 上跑 .NET 时，标准姿势是 Kestrel 自托管 + Nginx（Windows）或 IIS 反代——IIS 的角色是"入口网关"而非"应用宿主机"。

---

**下一模块**：[05-Web服务器与应用服务器分工](05-Web服务器与应用服务器分工.md) / **返回总览**：[00-Web服务器总览](00-Web服务器总览.md)
