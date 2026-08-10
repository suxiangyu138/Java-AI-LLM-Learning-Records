# 04-ClientSession与连接管理
> 定位：`ClientSession` 是 aiohttp 客户端的心脏——连接池、超时、Cookie、代理全部由它管理；不懂连接管理，异步请求就是「看起来并发、实际连不上」。

## 📚 目录
1. [Session：连接池的持有者](#1-session连接池的持有者)
2. [生命周期与关闭](#2-生命周期与关闭)
3. [TCPConnector 参数全解](#3-tcpconnector-参数全解)
4. [四段式超时](#4-四段式超时)
5. [Cookie 与安全红线](#5-cookie-与安全红线)
6. [代理配置](#6-代理配置)
7. [连接管理五大坑](#7-连接管理五大坑)
8. [练习](#8-练习)

## 1. Session：连接池的持有者

HTTP 每次请求都要三次握手（HTTPS 还要 TLS 四次），这个成本远高于请求本身。连接复用（keep-alive）就是「建立后不关，下个请求继续用」——而连接的容器就是 `ClientSession`。它管理三样东西：到每个主机的连接池、全局 Cookie、请求默认参数（headers、timeout、默认的 `json` 序列化器）。

```python
async with aiohttp.ClientSession() as session:
    # 连接池建立；多次请求复用同一批 TCP 连接
    async with session.get("https://a.com/1") as r1: ...
    async with session.get("https://a.com/2") as r2: ...
```

注意两个 `async with` 的层级：外层管 session 生命周期，内层管单次请求——请求结束连接**归还连接池**而不是关闭，这是 aiohttp 高性能的秘密。session 没有上下文管理器包裹时会收到 RuntimeError 提示，这是设计好的强制规范。

## 2. 生命周期与关闭

session 是重量级对象：创建即初始化连接池，关闭即断开所有 keep-alive 连接。三条纪律：

**建一次，全局复用**。模块级 `session = aiohttp.ClientSession()` 一次创建，所有协程共用——并发 1000 也是同一批池连接复用，这是性能根基。

**用完必须关**。`async with` 语法保证退出时调用 `close()`（或 `__aexit__`）——不关则连接池持有 TCP 连接不释放，长跑进程文件描述符耗尽，崩溃无声无息。

**`asyncio.run` 内的 session 不手动关**。`asyncio.run` 退出时会关闭事件循环并清理其管理的资源，但连接池的释放依赖引用计数，规范做法仍是显式 `async with`。长期进程（服务端）则必须确保每个 session 有明确的关闭时机。

## 3. TCPConnector 参数全解

`ClientSession(connector=...)` 传入自定义连接器，两个参数是并发调优的核心旋钮：

```python
connector = aiohttp.TCPConnector(
    limit=100,             # 连接池全局上限（默认 100）
    limit_per_host=10,     # 到同一主机的连接上限（默认 0=不限制）
    ttl_dns_cache=300,     # DNS 缓存时长（默认 10 秒）
    ssl=False,             # 仅开发环境关证书验证
)
async with aiohttp.ClientSession(connector=connector) as session: ...
```

`limit` 管全局并发连接数：1000 个并发请求但 limit=100 时，多余的请求排队等待池里空出连接——这是内置背压，比裸 Semaphore 更贴近 TCP 层。`limit_per_host` 管单主机：爬同一个站时用它防打爆对端，配合 `limit` 双保险。生产调优口诀：**limit 按本机资源定，limit_per_host 按对端承受力定**。

`ttl_dns_cache` 值得单独强调：默认 10 秒的 DNS 缓存适合动态环境；对 IP 极少变动的内部服务可调到 600+，减少 DNS 往返。装了 aiodns 后解析走异步事件，两者配合更佳。

## 4. 四段式超时

`ClientTimeout` 把一次请求拆成四个独立的超时维度，全部独立生效：

```python
timeout = aiohttp.ClientTimeout(
    total=30,          # 整个请求的总时长上限（含重定向）
    connect=5,         # 建立 TCP 连接的时间
    sock_connect=5,    # 连接池取连接时的等待（排队时间）
    sock_read=10,      # 两次读操作之间的间隔——防「吊死连接」
)
async with aiohttp.ClientSession(timeout=timeout) as session: ...
```

最容易犯的错是只设 `total` 不设 `sock_read`：服务器接受了连接但迟迟不回数据，`total` 到点前你只能干等——而 `sock_read` 是「读操作最长沉默期」，能立刻发现吊死连接并释放它去处理下一个请求。`total` 默认 5 分钟太长，生产统一收紧：默认推荐 `total=30, connect=5, sock_read=10`，对端慢的接口单独放宽。

注意 `sock_connect` 与 `connect` 的区别：前者是「池里等连接」的时间（连接被占满时排队），后者是「建新连接」的时间。并发高时两者都可能命中，都要设。

## 5. Cookie 与安全红线

**默认行为**：session 级 CookieJar 自动存储响应 Set-Cookie，后续请求自动带上——但**只发给设置它的域**（同域规则）。爬虫登录态的保持就靠它，`session.get(url)` 无需手动传 Cookie。

**跨域需要 `unsafe=True`**：`CookieJar(unsafe=True)` 允许 Cookie 跨域发送，IP 直连地址也适用。默认关是有意的安全设计。

**两条 2026 安全红线**：

`CookieJar.load()` 反序列化漏洞（CVE-2026-34993）——从文件加载 Cookie 会 pickle 反序列化，不可信文件可致任意代码执行。3.14 起 `save()` 改为 JSON 格式，`load()` 自动兼容。红线：**绝不加载来源不明的 Cookie 文件**，自己存的用 `save()` JSON 写回。

`cookies` 参数跨域重定向泄漏（CVE-2026-47265）——请求级 `session.get(url, cookies={...})` 在跟随跨域重定向后会继续携带这些 Cookie，可被攻击者控制的重定向目标窃取。3.14 已修复为跨域丢弃。红线：**敏感 Cookie 用 headers 传而不是 cookies 参数**（headers 里的 Cookie 不受此问题影响）。

## 6. 代理配置

```python
# 单请求代理（proxy_auth 已弃用，用 URL 里带凭据）
async with session.get(url, proxy="http://user:pass@proxy.example.com:8080") as resp: ...
```

**连接复用需警惕**：HTTP 代理本身做 keep-alive，3.13.3 修复了代理连接复用导致的 Authorization 头泄漏——代理连接属于全局复用资源，请求间可能串凭据。生产建议：代理带凭据时每个代理独立 session，或改用隧道代理（HTTPS 代理走 CONNECT，无此问题）。

**环境变量代理**：`ClientSession(trust_env=True)` 时自动读取 `HTTP_PROXY`/`HTTPS_PROXY`/`NO_PROXY` 环境变量（如 `http_proxy` 全局设置）。CI、内网、开发机三环境切换时用环境变量而非改代码——但显式 `proxy=` 参数优先级更高。

## 7. 连接管理五大坑

**坑一：每请求新建 session**。连接池频繁建拆，复用机制归零——性能退化到同步库水平还白费异步。

**坑二：并发超限被静默排队**。limit=100 而并发 1000，多出的 900 个在池里排队，`sock_connect` 会先于业务暴露问题——报错信息里出现大量 `TimeoutError: sock_connect` 就是信号。

**坑三：keep-alive 连接失效不感知**。服务器断开空闲连接后客户端不知情，首次请求报 `ConnectionResetError`——这是正常现象，aiohttp 会自动重试一次；自己别手写重试导致双重请求。

**坑四：TLS-in-TLS 误报警告**。HTTPS 走 HTTP 代理会触发警告，3.14 起只在代理本身是 HTTPS 时才警告——看到警告先确认代理协议。

**坑五：服务端（服务端场景）session 无关闭时机**。每个请求新建 session 且无清理逻辑，连接池泄漏——服务端场景把 session 挂到 app 上，`on_cleanup` 里统一关闭。

## 8. 练习

1. 解释为什么「session 建一次全局复用」是性能根基。
2. `limit` 与 `limit_per_host` 分别按什么原则定值？
3. 只设 `total` 不设 `sock_read` 的风险是什么？
4. CVE-2026-47265 的正确规避姿势是什么？
5. 代理连接复用与 Authorization 泄漏是什么关系？

> 🎯 **核心要点**：session = 连接池 + Cookie + 默认参数，建一次全局复用；TCPConnector 的 limit 是内置背压；超时四段独立设，sock_read 防吊死；Cookie 走 jar 同域自动、敏感信息走 headers；2026 两条 CVE 都与 Cookie 相关，升级 3.14+ 是第一防线。

---

**下一模块**：[05-请求与响应处理](05-请求与响应处理.md)｜**返回总览**：[00-aiohttp总览](00-aiohttp总览.md)
