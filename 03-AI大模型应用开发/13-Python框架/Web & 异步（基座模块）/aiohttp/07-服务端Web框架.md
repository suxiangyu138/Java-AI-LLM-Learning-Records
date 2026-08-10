# 07-服务端Web框架
> 定位：`web.Application` 是轻量级异步 Web 框架——路由、中间件、静态资源、WebSocket 服务端一应俱全；适合内部 API、代理网关、推送服务，与 FastAPI 分层不竞争。

## 📚 目录
1. [Application 骨架](#1-application-骨架)
2. [路由与路径参数](#2-路由与路径参数)
3. [请求对象全解](#3-请求对象全解)
4. [响应类型家族](#4-响应类型家族)
5. [中间件洋葱模型](#5-中间件洋葱模型)
6. [静态资源与文件下载](#6-静态资源与文件下载)
7. [运行与部署](#7-运行与部署)
8. [与 FastAPI 的分工](#8-与-fastapi-的分工)
9. [常见坑](#9-常见坑)
10. [练习](#10-练习)

## 1. Application 骨架

```python
from aiohttp import web

app = web.Application(middlewares=[error_middleware])   # 全局中间件
app.router.add_get("/", index)                          # 路由注册
app.on_startup.append(init_db)                          # 启动钩子
app.on_cleanup.append(close_db)                         # 清理钩子
web.run_app(app, host="0.0.0.0", port=8080)
```

`Application` 是配置容器：路由、中间件、生命周期钩子都挂在它身上。`on_startup`/`on_cleanup` 是资源生命周期的标准落点——连数据库、建 aiohttp 客户端 session、加载模型都放这里，避免「请求时懒初始化」导致的竞态。一个资源一条钩子函数，顺序执行。

`web.run_app` 只适合开发；生产用 Gunicorn worker（见第 7 节）或直接 `web.AppRunner` + `TCPSite` 自己控制（此时端口在 `TCPSite` 上，3.14 新增 `site.port` 可读）。

## 2. 路由与路径参数

两种注册方式等价。生产推荐 `RouteTableDef` 装饰器式——路由与处理函数同处，可读性好：

```python
routes = web.RouteTableDef()

@routes.get("/items/{item_id}")            # 路径参数 {item_id}
async def get_item(request: web.Request):
    item_id = request.match_info["item_id"]   # 字符串！需要时自行 int()
    return web.json_response({"id": item_id})

@routes.post("/items")
async def create_item(request):
    data = await request.json()
    return web.json_response(data, status=201)

app = web.Application()
app.add_routes(routes)                     # 或 app.router.add_routes(routes)
```

方法语义与客户端对称：`@routes.get/post/put/patch/delete`。同一路径不同方法注册多次，自动按方法分发。路径参数从 `request.match_info` 取，注意**永远是字符串**，`int()` 转换要自己处理异常（非数字路径会抛 ValueError——用中间件统一兜住，见第 5 节）。路径结尾的 `/` 默认不区分：`/items/` 与 `/items` 视为同一路由。

## 3. 请求对象全解

`web.Request` 在 aiohttp 里是**可丢弃对象**——响应返回后即销毁，不要跨请求保存（与 aiohttp 早期版本不同，3.x 里它是 HTTPMessage 不是 mutable）。常用字段与方法：

```python
method = request.method          # "GET"/"POST" 等
url = request.url                # yarl.URL 对象：query 用 url.query.get("page")
headers = request.headers        # 字典式访问，大小写不敏感
cookies = request.cookies        # 请求 Cookie（只读 dict）

await request.json()             # JSON 体 → dict（Content-Type 不符抛 400）
await request.post()             # 表单 → dict（multipart 表单也支持）
await request.read()             # 原始 bytes
data = await request.multipart() # 大文件 multipart 流式读取（边读边写盘）
```

`request.json()` 解析失败（非法 JSON）抛 `json.JSONDecodeError` 包成 400 响应——严格校验的业务建议自行 try 并返回友好错误。大文件上传务必走 `request.multipart()` 流式，`await request.post()` 会把整个请求体读进内存。

## 4. 响应类型家族

| 响应类型 | 用途 | 示例 |
|---------|------|------|
| `web.Response` | 通用响应 | `web.Response(text="ok", status=200)` |
| `web.json_response` | JSON API | `web.json_response(data, status=201)` |
| `web.text` | 纯文本/HTML | `web.Response(text=html, content_type="text/html")` |
| `web.FileResponse` | 文件下载 | `return web.FileResponse("model.bin")` |
| `web.StreamResponse` | 流式/SSE/大响应 | 手动 prepare+write+EOF |
| `web.json_bytes_response` | bytes 直出 JSON（3.14） | 免 str 中间层，配 orjson |

StreamResponse 是流式核心：`resp = web.StreamResponse()` → `await resp.prepare(request)` → 循环 `await resp.write(chunk)` → `await resp.write_eof()`。大文件、SSE 推送、实时数据都用它。FileResponse 处理静态文件最省事——自动设置 Content-Type、Content-Length、支持 Range（视频拖拽、断点续传直接支持）。

## 5. 中间件洋葱模型

中间件是「包裹处理函数」的协程：请求进入时从外到内，响应返回时从内到外（洋葱）。适合做统一横切：异常兜底、统一头、CORS、日志、限流。

```python
@web.middleware
async def error_middleware(request, handler):
    try:
        response = await handler(request)
        response.headers["X-Powered-By"] = "aiohttp"
        return response
    except web.HTTPException as ex:          # 路由内 raise web.HTTPNotFound() 等
        return web.json_response({"error": ex.reason}, status=ex.status)
    except Exception:
        log.exception("未处理异常")           # 500 兜底，绝不让异常裸奔
        return web.json_response({"error": "internal"}, status=500)

app = web.Application(middlewares=[error_middleware])
```

中间件里的 `await handler(request)` 是「进入下一层」；`try/except` 包住它即可捕获业务层一切异常。**这是 500 兜底的标准落点**——业务代码里 `raise web.HTTPNotFound()` 会走异常中间件统一转 JSON。CORS 中间件同理：检查 `Origin` 头、OPTIONS 预检直接回 204。多个中间件按注册顺序从外到内执行。

## 6. 静态资源与文件下载

```python
app.router.add_static("/static", path="static_dir")     # 静态目录映射
# /static/css/main.css → static_dir/css/main.css
```

`add_static` 一个调用搞定静态资源：自动设 Content-Type、缓存头、支持目录浏览与 Range。生产注意：静态资源走 Nginx 更高效，aiohttp 的 add_static 适合开发期与内网小服务。下载场景用 FileResponse，它会按文件大小自动选择流式还是 sendfile 传输。

## 7. 运行与部署

**开发**：`web.run_app(app, host, port)`。

**生产**：Gunicorn + aiohttp worker。aiohttp 自带两个 worker：`aiohttp.GunicornWebWorker` 与 `aiohttp.GunicornUVLoopWebWorker`（后者自动启用 uvloop）。注意 3.13.4 修复了 Python 3.14 下 Gunicorn worker 的 `RuntimeError: An event loop is running`——Python 3.14 生产部署必须 ≥3.13.4。

```bash
gunicorn app:app --worker-class aiohttp.GunicornWebWorker --workers 4 --bind 0.0.0.0:8080
```

worker 数按 CPU 核数定（异步服务多 worker 提升有限，通常 2-4 个足够，更多的收益来自多进程吞吐而非并发上限）。前面挂 Nginx 做 TLS 终止与静态资源分流，见 [[../../强烈推荐（做项目必用，简历加分）/loguru/00-loguru总览|loguru]] 篇的接入层链路。

## 8. 与 FastAPI 的分工

FastAPI 基于 Starlette，面向「对外业务 API」：Pydantic 请求校验、自动 OpenAPI 文档、依赖注入、类型提示全自动——写业务接口的效率和体验远胜 aiohttp 手写校验。aiohttp 服务端面向「内部基础件」：代理与转发（双件套同进程）、WebSocket 长连接服务、推送网关、嵌入式 HTTP 服务（你的库/工具自带的 API 面）。

选型判断：**对外业务 API 用 FastAPI，内部基础件用 aiohttp**。两者同属 [[../FastAPI/00-FastAPI知识体系总览|FastAPI 体系]] 的异步家族，但层次不同：FastAPI 是「框架」，aiohttp 是「HTTP 层库」。一个项目里两者共存也常见——FastAPI 暴露业务，aiohttp 做转发与推送。

**典型的 aiohttp 服务端画像**：内网监控面板（几十个只读端点，一个中间件统一鉴权与 CORS）；AI 应用的中转网关（收前端请求、转发多家模型供应商、流式回传——双件套同进程天然合适）；长连接推送服务（WebSocket 为主，HTTP 只做握手与鉴权）；以及库与工具自带的嵌入式管理面（`pip install` 进你项目的库，用 aiohttp 起一个 127.0.0.1 端口的管理 API，不引入 FastAPI 全家桶）。这几个画像的共同点是：**端点少、性能要求高、不需要业务框架的文档与校验**——正是「HTTP 层库」的舒适区。

## 9. 常见坑

**坑一：路由处理函数里写阻塞代码**。`time.sleep`、同步读大文件——事件循环冻结，所有连接一起卡。处理函数必须全异步。

**坑二：`match_info` 里的参数当数字用**。`int(request.match_info["id"])` 遇非数字抛 ValueError 变 500——要么参数校验中间件，要么 `try/except ValueError` 转 400。

**坑三：`request.post()` 读大表单爆内存**。multipart 大文件必须 `request.multipart()` 流式。

**坑四：中间件忘记 `await handler`**。不调用 handler 请求直接短路——有时是故意的（限流/拦截），忘写则是事故，响应永远空。

**坑五：`on_startup` 里同步阻塞**。初始化数据库连接池、加载模型都是 I/O——钩子函数也必须是协程，同步版本会卡启动。

**坑六：响应头跨域配置不全**。CORS 需要 `Access-Control-Allow-Origin` + 预检 OPTIONS 处理，只设 Origin 头浏览器照样拦。

## 10. 练习

1. `on_startup` 与 `on_cleanup` 各放什么资源？
2. `request.match_info` 取出的路径参数是什么类型？
3. 中间件 500 兜底的代码骨架是什么？
4. `web.json_response` 与 `web.json_bytes_response` 的差异（3.14）？
5. aiohttp 服务端与 FastAPI 的选型标准？

> 🎯 **核心要点**：服务端 = Application（路由+中间件+生命周期钩子）；RouteTableDef 装饰器式注册；异常兜底放中间件；大文件上传用 multipart 流式、下载用 FileResponse；生产 Gunicorn worker ≥3.13.4；对外 API 交给 FastAPI，aiohttp 做内部基础件。

---

**下一模块**：[08-并发限流与性能](08-并发限流与性能.md)｜**返回总览**：[00-aiohttp总览](00-aiohttp总览.md)
