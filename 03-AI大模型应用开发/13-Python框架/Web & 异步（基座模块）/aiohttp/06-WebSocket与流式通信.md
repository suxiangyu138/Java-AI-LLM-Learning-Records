# 06-WebSocket与流式通信
> 定位：WebSocket 是「一次连接、双向实时」的协议——行情推送、聊天、Agent 任务流、大模型流式输出都靠它；aiohttp 是少数客户端服务端全支持的库。

## 📚 目录
1. [WebSocket 与 HTTP 的分工](#1-websocket-与-http-的分工)
2. [客户端连接与收发](#2-客户端连接与收发)
3. [心跳机制](#3-心跳机制)
4. [原始字节模式](#4-原始字节模式)
5. [服务端 WebSocket](#5-服务端-websocket)
6. [断线与重连](#6-断线与重连)
7. [流式上传与 SSE](#7-流式上传与-sse)
8. [常见坑](#8-常见坑)
9. [练习](#9-练习)

## 1. WebSocket 与 HTTP 的分工

HTTP 是请求-响应模型：客户端问、服务端答，服务端没法主动推。轮询（客户端定时问）简单但浪费，SSE（服务端单向推）解决了推送但只支持服务端到客户端。**WebSocket 是全双工长连接**：建立时是一次带升级头的 HTTP 握手（101 状态码），之后双向任意时刻互发消息，头部开销每帧仅 2 字节。

选型判断：服务端要主动推、需要双向对话（聊天室、协同编辑、行情）、消息频率高——WebSocket；只做服务端单向通知、想要 HTTP 生态的自动重连——SSE 更轻。大模型流式输出（token 流）本质是 SSE 场景，aiohttp 客户端用普通 GET + 流式读即可消费，不必上 WebSocket。

**典型应用场景**：金融行情推送（订阅-推送模型，消息频率每秒数十条，头部开销可忽略）；实时协作（协同编辑的变更同步、在线白板的坐标流）；物联网设备通道（设备长连接上报 + 服务端下发指令，双向天然契合）；聊天与客服（消息推送 + 在线状态）；以及 AI 应用里的任务流——Agent 把推理进度通过 WS 推给前端，前端把中断指令回传，双向实时性都比轮询或 SSE 干净。判断一个场景是否该用 WS 的标准只有一条：**是否双方都需要主动发消息**。只有服务端要推，用 SSE；只有客户端要问，用轮询或长轮询；双方都要发，才轮到 WebSocket。

## 2. 客户端连接与收发

```python
async with session.ws_connect("wss://stream.example.com/realtime") as ws:
    await ws.send_str(json.dumps({"action": "subscribe", "symbol": "BTCUSD"}))
    async for msg in ws:                      # 标准收消息循环
        if msg.type == aiohttp.WSMsgType.TEXT:
            data = json.loads(msg.data)
            print(data)
        elif msg.type == aiohttp.WSMsgType.ERROR:
            break
```

`ws_connect` 走 session 的复用与超时配置；握手失败抛 `aiohttp.ClientConnectorError` 或 `WSServerHandshakeError`（非 101 响应）。消息类型四件套：`TEXT`（字符串）、`BINARY`（bytes）、`PING`/`PONG`（控制帧）、`CLOSE`（关闭帧）。`async for msg in ws` 循环内部自动处理 PING/PONG——收到 PING 自动回 PONG，不需要你写。

发送三件套：`send_str(s)`、`send_bytes(b)`、`send_json(obj)`（内部 json 序列化）。服务端主动发起的关闭，`async for` 会在收到 CLOSE 后自然退出，此时检查 `ws.close_code` 获取关闭原因。

## 3. 心跳机制

长连接最大的风险是「半开连接」：网络设备静默断开，双方都不知道，消息互相等死。心跳（PING/PONG 探测）是标准解法。

```python
ws = await session.ws_connect(url, heartbeat=30)   # 每 30 秒自动发 PING
```

`heartbeat=30` 表示 30 秒无收发时自动发 PING，等不到 PONG 则判定连接死亡并关闭。3.13 之前有个知名 bug：服务端发来大帧数据时，客户端忙于读数据没发心跳，被对端误判死亡——3.14 修复为**收到数据即重置心跳计时器**，大帧场景不再误判。生产配置：心跳间隔取对端超时的一半，如对端 60 秒无活动断开，本地 heartbeat=30。

## 4. 原始字节模式

3.14 新增 `decode_text=False`：TEXT 消息不再解码为 str，直接给 bytes。用途是高性能 JSON 解析——字符串解码 + 再 json.loads 有两次拷贝，直接 `orjson.loads(msg.data)` 省一次，高频行情流（每秒几千帧）收益明显：

```python
ws = await session.ws_connect(url, decode_text=False)
async for msg in ws:
    if msg.type == aiohttp.WSMsgType.TEXT:
        data = orjson.loads(msg.data)     # bytes 直喂 orjson
```

服务端 `WebSocketResponse.send_str` 对应有 `send_json_bytes`（同样免中间解码）。数据量大到 2GB 的「二进制流」请用 `ws.set_max_msg_size()` 调大上限（默认 4MB，超限自动断连）。

## 5. 服务端 WebSocket

```python
async def ws_handler(request: web.Request) -> web.WebSocketResponse:
    ws = web.WebSocketResponse(heartbeat=30)
    await ws.prepare(request)              # 完成握手（101）
    async for msg in ws:
        if msg.type == aiohttp.WSMsgType.TEXT:
            await ws.send_str(f"echo: {msg.data}")
        elif msg.type == aiohttp.WSMsgType.CLOSE:
            break
    return ws

app.router.add_get("/ws", ws_handler)
```

服务端要点：`prepare(request)` 完成握手后才能收发；`async for` 循环结束（客户端关闭或异常）后 `return ws` 即返回关闭帧。连接集合管理是聊天室的基础：`connections = set()`，`on_connect` 加入、循环退出时移除，广播就是遍历集合 `send_str`。注意广播的异常处理——某客户端断线时 `send` 抛异常，要单独捕获并移出集合，不能让一个断线拖死整个广播循环。

## 6. 断线与重连

网络不可靠，WebSocket 客户端必须自带重连。核心设计：**心跳驱动诊断 + 指数退避重连 + 幂等订阅**。

```python
async def run(ws_url):
    backoff = 1
    while True:
        try:
            async with session.ws_connect(ws_url, heartbeat=30) as ws:
                backoff = 1                                  # 连接成功重置退避
                async for msg in ws: ...                     # 正常业务循环
        except (aiohttp.ClientError, asyncio.TimeoutError) as e:
            log.warning(f"WS 断开: {e}，{backoff}s 后重连")
        await asyncio.sleep(backoff)
        backoff = min(backoff * 2, 60)                       # 1→2→4→...→60 封顶
```

`while True` 外层管重连，内层管单次连接生命周期；连接成功后必须**重发订阅消息**——服务端不会记住断线前的订阅状态，这是最隐蔽的「重连成功但没数据」事故。

## 7. 流式上传与 SSE

**流式上传**：aiohttp 的 `data=` 支持任何 async 可迭代对象，配合 `StreamReader` 实现生成器式上传：

```python
async def gen():
    for i in range(1000000):
        yield f"line{i}\n".encode()

await session.post(url, data=gen())        # 边生成边发送，不占内存
```

对需要「读多少发多少」的场景（大文件、日志流、LLM 上下文流式发送）这是唯一不爆内存的姿势。

**消费 SSE**：SSE 是普通 HTTP 响应，`text/event-stream`，客户端直接流式读：

```python
async with session.get("https://llm.example.com/stream", 
                        headers={"Accept": "text/event-stream"}) as resp:
    async for line in resp.content:
        if line.startswith(b"data:"):
            yield json.loads(line[5:])     # 逐行解析事件数据
```

大模型流式输出的客户端消费就是这么简单——不必引入任何 SSE 库，aiohttp 原生支持。

## 8. 常见坑

**坑一：`async for msg in ws` 里发消息卡死**。收循环内 `await ws.send_str()` 长时间不返回，通常是对端不读数据导致发送缓冲区打满——发送频率要有背压意识，不能无限制狂发。

**坑二：心跳只设客户端不设服务端**。两端各自维护心跳，单端心跳防不了对端半开。

**坑三：重连后不重发订阅**。断线重连成功但消息全无，多半是订阅状态没恢复。

**坑四：广播循环被一个断线客户端拖死**。`send` 抛异常必须捕获，断线者移出集合。

**坑五：大消息被静默断连**。默认 4MB 上限，超过即断；大二进制先 `set_max_msg_size`。

**坑六：PING 帧没回也断**。`heartbeat` 期间对端没回 PONG 判定死亡——对端是代理时先确认代理支持转发控制帧。

**坑七：服务端 `prepare` 后忘了管理连接集合**。握手完成后连接即占资源，不做连接计数与超时回收，慢客户端会占满连接——服务端要有一套「连接注册、心跳巡检、超时踢出」的治理，而不是只写收发循环。

## 9. 练习

1. WebSocket 与 SSE 的选型标准是什么？
2. `heartbeat=30` 的作用机制是什么？3.14 修复了什么？
3. `decode_text=False` 的适用场景与收益。
4. 设计一个带指数退避的断线重连循环，注意哪些细节？
5. 服务端广播集合的健壮实现要注意什么？

> 🎯 **核心要点**：WebSocket = 双向长连接，`async for msg in ws` 收 + `send_str/send_bytes/send_json` 发；心跳两端都设（客户端 heartbeat 参数）；重连必须重发订阅；SSE 就是普通流式 GET；大消息先调 `set_max_msg_size`。

---

**下一模块**：[07-服务端Web框架](07-服务端Web框架.md)｜**返回总览**：[00-aiohttp总览](00-aiohttp总览.md)
