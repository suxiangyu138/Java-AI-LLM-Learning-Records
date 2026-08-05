# FastAPI 面试宝典
> 基于课程大纲全面覆盖面试高频考点

## 目录
1. [一、基础概念速答](#一基础概念速答15-18题)
2. [二、深度原理剖析](#二深度原理剖析10-12题)
3. [三、实战场景题](#三实战场景题6-10题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（15-18题）

### 1.1 FastAPI 是什么？核心特点？

FastAPI 是一个**现代、高性能的 Python Web 框架**，基于 Starlette（Web 路由）和 Pydantic（数据校验）构建，专为构建 RESTful API 而生。

| 特性 | 说明 |
|------|------|
| **异步原生** | 基于 `asyncio`，支持 `async def` 路由，不阻塞事件循环 |
| **自动文档** | `/docs` (Swagger UI) 和 `/redoc` (ReDoc) 开箱即用 |
| **类型校验** | 基于 Pydantic 自动验证请求/响应，IDE 提示友好 |
| **高性能** | 与 Node.js / Go 吞吐量相当（得益于 Starlette + Uvicorn） |
| **依赖注入** | 内置 `Depends` 系统，管理共享逻辑（数据库、认证） |

```python
from fastapi import FastAPI

app = FastAPI(title="My API", version="1.0.0")

@app.get("/")
async def root():
    return {"message": "Hello FastAPI"}

# 启动：uvicorn main:app --reload
```

> 💡 FastAPI 由 Sebastian Ramirez（tiangolo）于 2018 年创建，当前是 GitHub 上星标最高的 Python Web 框架之一。

### 1.2 FastAPI 与 Flask / Django 的区别

| 维度 | FastAPI | Flask | Django |
|------|---------|-------|--------|
| **异步支持** | 原生 `async/await` | 需插件（Quart） | 3.0+ 支持异步，但生态不成熟 |
| **性能** | 最高（~50k req/s） | 中等（~15k req/s） | 较低（~10k req/s） |
| **类型校验** | Pydantic 自动校验 | 手动校验 / marshmallow | DRF Serializer |
| **API 文档** | 自动 OpenAPI + Swagger | flasgger 手动集成 | drf-yasg 手动集成 |
| **电池** | 轻量，按需扩展 | 极简，DIY 风格 | 全栈，内置 ORM/Admin |
| **学习曲线** | 中等（类型注解 + 异步） | 低（入门简单） | 高（框架庞大） |
| **适用场景** | AI 服务 / 微服务 / 高并发 API | 小型项目 / 原型 | 企业级全栈应用 |

> 🎯 面试亮点：新项目无历史包袱的优先选 FastAPI。AI 场景下 FastAPI + `httpx.AsyncClient` 是调用 LLM 的最佳组合。

### 1.3 路径参数 vs 查询参数 vs 请求体

```python
from fastapi import FastAPI, Query, Path, Body
from pydantic import BaseModel

app = FastAPI()

# 路径参数 — URL 路径的一部分
@app.get("/items/{item_id}")
def get_item(item_id: int = Path(ge=1, description="商品ID")):
    return {"item_id": item_id}

# 查询参数 — URL ?key=value
@app.get("/search")
def search(
    q: str = Query(min_length=1, max_length=50, description="搜索关键词"),
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100)
):
    return {"query": q, "page": page, "size": size}

# 请求体 — JSON Body
class Item(BaseModel):
    name: str
    price: float = Body(gt=0, description="价格必须大于0")
    tags: list[str] = []

@app.post("/items")
def create_item(item: Item):
    return {"name": item.name, "price": item.price}
```

| 参数来源 | 定义位置 | 示例 URL | 典型用途 |
|---------|---------|---------|---------|
| 路径参数 | `/{param}` | `/users/42` | 资源标识（ID） |
| 查询参数 | `?key=val` | `/users?page=1` | 筛选/分页/排序 |
| 请求体 | Body JSON | POST `/users` | 创建/更新资源数据 |

### 1.4 Pydantic 模型与校验

Pydantic 是 FastAPI 的**数据校验引擎**，利用 Python 类型注解进行运行时校验。

```python
from pydantic import BaseModel, Field, EmailStr, validator
from datetime import date
from typing import Optional

class User(BaseModel):
    id: int
    name: str = Field(..., min_length=2, max_length=50)
    email: EmailStr
    age: int = Field(ge=0, le=150, default=0)
    created_at: date = Field(default_factory=date.today)
    tags: list[str] = []

    # 自定义校验器
    @validator("name")
    def name_must_be_valid(cls, v):
        if not v.strip():
            raise ValueError("名称不能为空")
        return v.strip()

    # 模型配置
    class Config:
        from_attributes = True  # ORM 模式
        json_schema_extra = {
            "example": {
                "name": "Alice",
                "email": "alice@example.com",
                "age": 25
            }
        }
```

> 💡 `from_attributes=True` 允许从 ORM 模型（SQLAlchemy/Tortoise）直接创建 Pydantic 实例，替代早期版本的 `orm_mode=True`。

### 1.5 响应模型（response_model）

```python
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List

# 数据库模型（内部）
class UserInDB(BaseModel):
    id: int
    name: str
    password_hash: str
    email: str

# 响应模型（对外暴露，隐藏敏感字段）
class UserOut(BaseModel):
    id: int
    name: str
    email: str

app = FastAPI()

@app.get("/users/{user_id}", response_model=UserOut)
def get_user(user_id: int):
    # 内部返回 UserInDB，FastAPI 自动转换为 UserOut（password_hash 被过滤）
    db_user = UserInDB(id=1, name="Alice", password_hash="xxx", email="a@b.com")
    return db_user

@app.get("/users", response_model=List[UserOut])
def list_users():
    return [UserInDB(id=1, name="Alice", password_hash="xxx", email="a@b.com")]
```

| 参数 | 作用 |
|------|------|
| `response_model` | 声明返回类型，自动过滤字段 |
| `response_model_exclude_unset` | 只返回前端实际设置的字段 |
| `response_model_include` | 只包含指定字段 |
| `response_model_exclude` | 排除指定字段 |
| `response_model_by_alias` | 按别名返回 |

> ⚠️ `response_model` 是**类型过滤**而非数据转换。敏感字段在模型中不存在就不会被返回，比手动过滤更安全。

### 1.6 异步路由与同步路由

```python
import asyncio, time
from fastapi import FastAPI

app = FastAPI()

# 路由方式一：同步 def — 在线程池中运行
@app.get("/sync")
def read_sync():
    time.sleep(1)  # 同步阻塞，但不阻塞事件循环（运行在线程池）
    return {"mode": "sync"}

# 路由方式二：异步 async def — 在事件循环中运行
@app.get("/async")
async def read_async():
    await asyncio.sleep(1)  # 异步等待，不阻塞
    return {"mode": "async"}
```

| 函数类型 | 运行位置 | 适用场景 |
|---------|---------|---------|
| `def` | 线程池（ThreadPool） | CPU 密集型、同步 I/O |
| `async def` | 事件循环（Event Loop） | 异步 I/O、数据库查询、网络请求 |

> 💡 绝大多数场景用 `async def`。内部调用同步库时也可以用 `def`，FastAPI 会自动用线程池执行。

### 1.7 表单数据处理

```python
from fastapi import FastAPI, Form, File, UploadFile

app = FastAPI()

# 表单数据
@app.post("/login")
def login(
    username: str = Form(...),
    password: str = Form(min_length=6)
):
    return {"username": username}

# 文件上传
@app.post("/upload")
def upload_file(file: UploadFile = File(...)):
    content = file.file.read()
    return {
        "filename": file.filename,
        "content_type": file.content_type,
        "size": len(content)
    }

# 多文件上传
@app.post("/upload-multiple")
def upload_multiple(files: list[UploadFile] = File(...)):
    return {"count": len(files)}
```

> ⚠️ `UploadFile` 使用 SpooledTmpFile，小文件在内存，大文件在磁盘，比直接 `bytes` 更高效。

### 1.8 Request 对象直接访问

```python
from fastapi import FastAPI, Request

app = FastAPI()

@app.get("/request-info")
def get_request_info(request: Request):
    return {
        "method": request.method,
        "url": str(request.url),
        "headers": dict(request.headers),
        "query_params": dict(request.query_params),
        "path_params": request.path_params,
        "client_host": request.client.host if request.client else None,
        "cookies": request.cookies,
        "json": request.json() if request.method == "POST" else None,
    }
```

### 1.9 多种响应类型

```python
from fastapi import FastAPI, Response
from fastapi.responses import (
    JSONResponse, HTMLResponse, PlainTextResponse,
    FileResponse, RedirectResponse, StreamingResponse
)
from typing import Any

app = FastAPI()

@app.get("/json")     # 默认 JSON
def json_response():
    return {"key": "value"}

@app.get("/html", response_class=HTMLResponse)
def html_response():
    return "<h1>Hello</h1>"

@app.get("/text", response_class=PlainTextResponse)
def text_response():
    return "Plain text content"

@app.get("/file", response_class=FileResponse)
def file_response():
    return FileResponse("report.pdf", filename="report.pdf")

@app.get("/redirect", response_class=RedirectResponse)
def redirect():
    return RedirectResponse(url="/json")

@app.get("/stream")
def stream_response():
    def generate():
        for i in range(100):
            yield f"data: {i}\n\n"
    return StreamingResponse(generate(), media_type="text/event-stream")
```

| 响应类 | 用途 |
|--------|------|
| `JSONResponse` | 默认，返回 JSON 数据 |
| `HTMLResponse` | 返回 HTML 页面 |
| `PlainTextResponse` | 返回纯文本 |
| `FileResponse` | 文件下载（自动处理 Range 请求） |
| `RedirectResponse` | HTTP 重定向 |
| `StreamingResponse` | 流式输出（大文件/SSE） |

### 1.10 后台任务（BackgroundTasks）

```python
from fastapi import FastAPI, BackgroundTasks

app = FastAPI()

def send_email(email: str, body: str):
    """模拟发送邮件（耗时操作）"""
    import time
    time.sleep(3)
    print(f"Sent email to {email}: {body}")

def log_request(url: str):
    print(f"Request logged: {url}")

@app.post("/register")
def register(email: str, tasks: BackgroundTasks):
    # 后台任务：不阻塞响应返回
    tasks.add_task(send_email, email, "Welcome!")
    tasks.add_task(log_request, "/register")
    return {"message": "Registration successful, email sending in background"}
```

> 💡 `BackgroundTasks` 适合轻量级任务（发送邮件、写日志）。重量级任务应使用 Celery / RabbitMQ 等消息队列。

### 1.11 静态文件服务

```python
from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

app = FastAPI()

# 挂载静态文件目录
app.mount("/static", StaticFiles(directory="static"), name="static")
# 访问：http://localhost:8000/static/style.css
```

### 1.12 生命周期事件（Lifespan）

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI

# 新版：使用 lifespan 上下文管理器（替代 deprecated 的 on_event）
@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动时
    print("Starting up...")
    await init_db_pool()
    yield
    # 关闭时
    print("Shutting down...")
    await close_db_pool()

app = FastAPI(lifespan=lifespan)
```

> ⚠️ `@app.on_event("startup"/"shutdown")` 方式在 FastAPI 新版本中已标记为 deprecated，推荐使用 `lifespan` 上下文管理器。

### 1.13 WebSocket 支持

```python
from fastapi import FastAPI, WebSocket, WebSocketDisconnect

app = FastAPI()

class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def broadcast(self, message: str):
        for connection in self.active_connections:
            await connection.send_text(message)

manager = ConnectionManager()

@app.websocket("/ws/{client_id}")
async def websocket_endpoint(websocket: WebSocket, client_id: str):
    await manager.connect(websocket)
    try:
        while True:
            data = await websocket.receive_text()
            await manager.broadcast(f"Client {client_id}: {data}")
    except WebSocketDisconnect:
        manager.disconnect(websocket)
        await manager.broadcast(f"Client {client_id} left")
```

### 1.14 异常处理

```python
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

app = FastAPI()

# 自定义异常
class CustomException(Exception):
    def __init__(self, code: int, message: str):
        self.code = code
        self.message = message

# 注册全局异常处理器
@app.exception_handler(CustomException)
async def custom_exception_handler(request: Request, exc: CustomException):
    return JSONResponse(
        status_code=exc.code,
        content={"error": exc.message, "code": exc.code}
    )

# HTTP 异常
@app.get("/items/{item_id}")
def get_item(item_id: int):
    if item_id > 100:
        raise HTTPException(status_code=404, detail="Item not found")
    return {"item_id": item_id}
```

### 1.15 APIRouter 路由拆分

```python
# === app/main.py ===
from fastapi import FastAPI
from routers import users, items

app = FastAPI()
app.include_router(users.router, prefix="/users", tags=["users"])
app.include_router(items.router, prefix="/items", tags=["items"])

# === app/routers/users.py ===
from fastapi import APIRouter

router = APIRouter()

@router.get("/")
def list_users():
    return [{"id": 1, "name": "Alice"}]

@router.get("/{user_id}")
def get_user(user_id: int):
    return {"id": user_id, "name": "Alice"}
```

### 1.16 环境配置管理

```python
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    app_name: str = "FastAPI App"
    debug: bool = False
    database_url: str = "sqlite:///./test.db"
    secret_key: str = "change-me"
    redis_url: str = "redis://localhost:6379/0"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

settings = Settings()
# 自动从 .env 文件或环境变量加载配置
```

### 1.17 OpenAPI / Swagger 自动生成

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(
    title="My API",
    description="API description for Swagger docs",
    version="1.0.0",
    docs_url="/docs",           # Swagger UI
    redoc_url="/redoc",         # ReDoc
    openapi_url="/openapi.json", # OpenAPI schema
)

class Item(BaseModel):
    name: str
    price: float

@app.post("/items", summary="Create item", response_description="Created item")
def create_item(item: Item):
    """创建商品（这段 docstring 会出现在 Swagger 文档中）"""
    return item
```

> 🎯 FastAPI 自动从类型注解、docstring、`response_model` 生成完整的 OpenAPI 3.0 规范文档，无需手动编写 YAML/JSON。

### 1.18 分页查询实现

```python
from fastapi import FastAPI, Query
from pydantic import BaseModel
from typing import Generic, TypeVar, List

T = TypeVar("T")

class Page(BaseModel, Generic[T]):
    """通用分页响应模型"""
    items: List[T]
    total: int
    page: int
    size: int
    pages: int  # 总页数

class UserOut(BaseModel):
    id: int
    name: str
    email: str

app = FastAPI()

@app.get("/users", response_model=Page[UserOut])
def list_users(
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100)
):
    # 模拟分页查询
    total = 100
    items = [UserOut(id=i, name=f"User{i}", email=f"user{i}@test.com")
             for i in range((page-1)*size, min(page*size, total))]
    return Page(
        items=items,
        total=total,
        page=page,
        size=size,
        pages=(total + size - 1) // size
    )
```

---

## 二、深度原理剖析（10-12题）

### 2.1 FastAPI 依赖注入原理（Depends）

依赖注入（DI）是 FastAPI 的核心设计模式，允许声明式地声明路由需要哪些依赖，框架自动解析并注入。

```python
from fastapi import FastAPI, Depends, HTTPException, Header
from typing import Optional

app = FastAPI()

# 定义依赖：可以是函数、类、可调用对象
async def verify_token(authorization: Optional[str] = Header(None)):
    """验证 Token 依赖"""
    if not authorization:
        raise HTTPException(status_code=401, detail="Missing token")
    token = authorization.replace("Bearer ", "")
    if token != "valid-token":
        raise HTTPException(status_code=401, detail="Invalid token")
    return {"user": "alice", "token": token}

def get_db():
    """数据库会话依赖（生成器形式）"""
    db = {"connection": "fake-db"}
    try:
        yield db
    finally:
        print("Closing db connection")

# 使用依赖
@app.get("/profile")
def get_profile(
    user: dict = Depends(verify_token),  # 注入认证信息
    db: dict = Depends(get_db)           # 注入数据库会话
):
    return {"user": user, "db_connected": True}
```

**依赖注入原理：**

| 概念 | 说明 |
|------|------|
| **声明式** | 路由函数声明需要的参数，框架自动提供 |
| **可组合** | 依赖可以嵌套依赖，形成依赖树 |
| **缓存** | 同一请求中多次依赖同一函数，结果会被缓存（共享状态） |
| **生命周期** | 每个请求创建，请求结束销毁（生成器依赖的 finally 执行） |

```python
# 类作为依赖
class CommonQueryParams:
    def __init__(self, q: str = None, page: int = 1, size: int = 20):
        self.q = q
        self.page = page
        self.size = size

@app.get("/search")
def search(params: CommonQueryParams = Depends()):
    # FastAPI 自动从请求参数构造 CommonQueryParams 实例
    return {"query": params.q, "page": params.page}
```

> 🎯 面试亮点：FastAPI 的 DI 比 Spring Boot 的 `@Autowired` 更轻量、更透明。DI 系统还支持 `Global Dependencies`（在 `app` 或 `APIRouter` 级别添加依赖）。

### 2.2 Pydantic v2 核心特性

Pydantic v2 完全用 Rust（pydantic-core）重写了校验引擎，速度比 v1 快 5-50 倍。

```python
from pydantic import BaseModel, Field, field_validator, model_validator
from typing import Optional

class Product(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    price: float = Field(..., gt=0)
    discount: Optional[float] = Field(None, ge=0, le=1)

    # v2 语法：字段级校验器
    @field_validator("name")
    @classmethod
    def name_not_empty(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Name cannot be empty")
        return v.strip()

    # v2 语法：模型级校验器（可同时校验多个字段）
    @model_validator(mode="after")
    def check_discount_price(self):
        if self.discount and self.discount > self.price * 0.5:
            raise ValueError("Discount cannot exceed 50% of price")
        return self

# v2 新特性：类型窄化
from pydantic import TypeAdapter
from typing import List

int_list_validator = TypeAdapter(List[int])
int_list_validator.validate_python([1, 2, 3])  # OK
# int_list_validator.validate_python([1, "a", 3])  # ValidationError

# v2 新特性：序列化时使用别名
class Config(BaseModel):
    api_key: str = Field(alias="API_KEY")

    class Config:
        populate_by_name = True  # 同时支持别名和原始名称

config = Config(API_KEY="sk-xxx")  # 通过别名赋值
print(config.api_key)  # sk-xxx
```

| Pydantic v1 | Pydantic v2 |
|-------------|-------------|
| `@validator` | `@field_validator` |
| `@root_validator` | `@model_validator` |
| `orm_mode = True` | `from_attributes = True` |
| Rust 后端无 | 默认 Rust 后端（pydantic-core） |
| 校验慢 | 校验快 5-50x |
| `class Config` | 部分配置改为 `model_config` |

### 2.3 FastAPI 中间件执行流程

中间件是**请求/响应处理管道中的钩子**，在请求到达路由前和响应返回客户端前执行。

```python
import time
from fastapi import FastAPI, Request
from starlette.middleware.base import BaseHTTPMiddleware

app = FastAPI()

# 方式一：函数式中间件（推荐）
@app.middleware("http")
async def add_process_time_header(request: Request, call_next):
    start_time = time.perf_counter()
    response = await call_next(request)  # 调用下一个中间件或路由
    process_time = time.perf_counter() - start_time
    response.headers["X-Process-Time"] = str(process_time)
    return response

# 方式二：类式中间件
class LoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        print(f"Request: {request.method} {request.url.path}")
        response = await call_next(request)
        print(f"Response status: {response.status_code}")
        return response

app.add_middleware(LoggingMiddleware)
```

**中间件执行顺序：**

```text
Request → Middleware 1 (入) → Middleware 2 (入) → Route Handler → Middleware 2 (出) → Middleware 1 (出) → Response
```

> 💡 中间件中修改请求/响应是常见需求：请求日志记录、性能监控、请求ID注入、统一错误处理。

### 2.4 CORS 跨域配置

```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=[              # 允许的域名列表
        "http://localhost:3000",
        "https://myapp.com",
    ],
    allow_origin_regex="https?://.*\.example\.com",  # 正则匹配
    allow_credentials=True,      # 允许 Cookie 携带
    allow_methods=["*"],         # 允许的 HTTP 方法
    allow_headers=["*"],         # 允许的请求头
    expose_headers=["X-Total-Count"],  # 暴露给前端的响应头
    max_age=600,                 # 预检请求缓存时间（秒）
)
```

| CORS 参数 | 作用 | 安全提示 |
|-----------|------|---------|
| `allow_origins` | 允许的源 | 生产环境不要用 `["*"]` |
| `allow_credentials` | 允许 Cookie | `allow_origins` 不能为 `["*"]` 时才能为 True |
| `allow_methods` | 允许的 HTTP 方法 | 最小权限原则 |
| `allow_headers` | 允许的请求头 | 按需开放 |

### 2.5 SQLAlchemy 异步集成（async session）

```python
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy import Column, Integer, String, select

# === database.py ===
DATABASE_URL = "postgresql+asyncpg://user:pass@localhost/db"
engine = create_async_engine(DATABASE_URL, echo=True)
AsyncSessionLocal = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

class Base(DeclarativeBase):
    pass

# === models.py ===
class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True)
    name = Column(String(50))
    email = Column(String(100))

# === dependencies.py ===
async def get_db():
    async with AsyncSessionLocal() as session:
        yield session

# === main.py ===
from fastapi import FastAPI, Depends
from sqlalchemy import select

app = FastAPI()

@app.get("/users")
async def get_users(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User))
    users = result.scalars().all()
    return users

@app.post("/users")
async def create_user(name: str, email: str, db: AsyncSession = Depends(get_db)):
    user = User(name=name, email=email)
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return user
```

> 💡 SQLAlchemy 2.0 推荐 `select()` 语法替代旧版 `Query API`。异步会话必须用 `async with` 管理。
> ⚠️ `expire_on_commit=False` 避免提交后对象过期导致后续访问触发同步查询。

### 2.6 Tortoise ORM（异步 ORM）

Tortoise ORM 是 Python 最成熟的**异步 ORM**，语法类似 Django ORM。

```python
from tortoise import Tortoise, fields
from tortoise.models import Model
from pydantic import BaseModel
from typing import Optional

# === 模型定义 ===
class User(Model):
    id = fields.IntField(pk=True)
    name = fields.CharField(max_length=50)
    email = fields.CharField(max_length=100, unique=True)
    created_at = fields.DatetimeField(auto_now_add=True)

    class Meta:
        table = "users"

class Article(Model):
    id = fields.IntField(pk=True)
    title = fields.CharField(max_length=200)
    content = fields.TextField()
    author = fields.ForeignKeyField("models.User", related_name="articles")
    created_at = fields.DatetimeField(auto_now_add=True)

# === 初始化 ===
async def init():
    await Tortoise.init(
        db_url="sqlite://./test.db",
        modules={"models": ["__main__"]}
    )
    await Tortoise.generate_schemas()

# === CRUD 操作 ===
async def crud_examples():
    # Create
    user = await User.create(name="Alice", email="alice@test.com")

    # Read
    user = await User.get(id=1)
    users = await User.filter(name__contains="Ali").order_by("-created_at")

    # Update
    await User.filter(id=1).update(name="Bob")

    # Delete
    await User.filter(id=1).delete()

    # 关联查询
    articles = await user.articles.all()  # related_name
```

| Tortoise ORM | SQLAlchemy (async) | Django ORM |
|-------------|-------------------|------------|
| 原生异步 | 需 asyncpg/aiomysql | 同步 |
| Django 风格语法 | 复杂但灵活 | 简单但限制多 |
| 内置迁移（Aerich） | Alembic 配合 | 内置 migrate |
| 支持 SQLite/MySQL/PG | 同上 | 同上 |
| 社区较小 | 社区最大 | 最成熟 |

### 2.7 Aerich 迁移工具

```bash
# 安装
pip install aerich

# 初始化配置
aerich init -t config.TORTOISE_ORM

# 生成迁移（首次）
aerich init-db

# 修改模型后生成迁移
aerich migrate --name "add_user_age"

# 应用迁移
aerich upgrade

# 回滚
aerich downgrade
```

```python
# aerich 配置示例
TORTOISE_ORM = {
    "connections": {"default": "sqlite://./db.sqlite3"},
    "apps": {
        "models": {
            "models": ["app.models", "aerich.models"],
            "default_connection": "default",
        }
    },
}
```

### 2.8 OAuth2 + JWT 认证

```python
from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from jose import JWTError, jwt
from passlib.context import CryptContext
from pydantic import BaseModel
from datetime import datetime, timedelta
from typing import Optional

# === 配置 ===
SECRET_KEY = "your-secret-key"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/token")

app = FastAPI()

# === 数据模型 ===
class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    username: Optional[str] = None

class User(BaseModel):
    username: str
    email: Optional[str] = None

# === 认证工具函数 ===
def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)

def hash_password(password: str) -> str:
    return pwd_context.hash(password)

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=15))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

async def get_current_user(token: str = Depends(oauth2_scheme)):
    credentials_exception = HTTPException(
        status_code=401,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username: str = payload.get("sub")
        if username is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception
    # 实际从数据库查询用户
    return User(username=username)

# === 路由 ===
@app.post("/token", response_model=Token)
async def login(form_data: OAuth2PasswordRequestForm = Depends()):
    # 验证用户名密码（实际从数据库查询）
    if form_data.username != "admin" or form_data.password != "admin":
        raise HTTPException(status_code=400, detail="Incorrect username or password")
    access_token = create_access_token(
        data={"sub": form_data.username},
        expires_delta=timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    )
    return Token(access_token=access_token, token_type="bearer")

@app.get("/users/me", response_model=User)
async def read_users_me(current_user: User = Depends(get_current_user)):
    return current_user
```

> 💡 `OAuth2PasswordBearer` 自动从请求头提取 `Authorization: Bearer <token>`，失败返回 401。

### 2.9 FastAPI 性能优化策略

```python
# 1. 使用异步数据库驱动
# pip install asyncpg
DATABASE_URL = "postgresql+asyncpg://user:pass@localhost/db"

# 2. GZip 压缩响应
from fastapi.middleware.gzip import GZipMiddleware
app.add_middleware(GZipMiddleware, minimum_size=1000)

# 3. 缓存热点数据（全局缓存）
from functools import lru_cache

@lru_cache(maxsize=128)
def get_expensive_data(key: str):
    """热点数据缓存"""
    return {"key": key, "data": "expensive"}

# 4. 连接池复用（不重复创建）
from httpx import AsyncClient, Limits

_client: AsyncClient = None

async def get_http_client() -> AsyncClient:
    global _client
    if _client is None:
        _client = AsyncClient(
            limits=Limits(max_connections=100, max_keepalive_connections=20),
            timeout=30.0
        )
    return _client

# 5. ORM 优化
#  - 使用 selectinload 替代 joinedload 避免笛卡尔积
#  - 只查询需要的字段（不要 SELECT *）
#  - 使用 paginate 限制查询数量
```

| 优化手段 | 效果 | 说明 |
|---------|------|------|
| 异步驱动 | 高并发 | asyncpg 比 psycopg2 快 3-5 倍 |
| GZip 压缩 | 减少带宽 60-80% | 大 JSON 响应效果明显 |
| 连接池复用 | 减少 TCP 握手 | httpx 连接池复用 |
| 数据缓存 | 降低数据库负载 | lru_cache / Redis |
| Pydantic v2 | 校验快 5-50x | Rust 后端 |
| ORM 懒加载优化 | 减少 N+1 查询 | 使用 selectinload/eager loading |

### 2.10 FastAPI 与 httpx 异步 HTTP 调用

```python
import httpx
from fastapi import FastAPI

app = FastAPI()

# 全局客户端（连接池复用）
client = httpx.AsyncClient(base_url="https://api.example.com", timeout=30.0)

@app.on_event("shutdown")
async def shutdown():
    await client.aclose()

@app.get("/proxy/users")
async def proxy_users():
    """代理调用外部 API"""
    resp = await client.get("/users")
    resp.raise_for_status()
    return resp.json()

# 并发调用多个外部 API
import asyncio

@app.get("/dashboard")
async def dashboard():
    async def fetch_users():
        resp = await client.get("/users")
        return resp.json()

    async def fetch_stats():
        resp = await client.get("/stats")
        return resp.json()

    users, stats = await asyncio.gather(fetch_users(), fetch_stats())
    return {"users": users, "stats": stats}
```

> 💡 `httpx.AsyncClient` 是 FastAPI 生态中最常用的异步 HTTP 客户端，支持连接池复用。

### 2.11 FastAPI 测试（TestClient）

```python
from fastapi import FastAPI
from fastapi.testclient import TestClient

app = FastAPI()

@app.get("/")
async def root():
    return {"message": "Hello"}

@app.post("/items")
async def create_item(name: str, price: float):
    return {"name": name, "price": price}

# === 测试代码 ===
from fastapi.testclient import TestClient

client = TestClient(app)

def test_root():
    response = client.get("/")
    assert response.status_code == 200
    assert response.json() == {"message": "Hello"}

def test_create_item():
    response = client.post("/items", json={"name": "book", "price": 29.9})
    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "book"
    assert data["price"] == 29.9

def test_auth_required():
    response = client.get("/protected", headers={"Authorization": "Bearer invalid"})
    assert response.status_code == 401
```

> ⚠️ `TestClient` 基于 `httpx`，异步路由在测试中也可直接调用。使用 `pytest` 运行测试。

### 2.12 FastAPI 项目结构规范

```text
project/
├── app/
│   ├── __init__.py
│   ├── main.py               # 应用入口，创建 FastAPI 实例
│   ├── config.py              # 配置管理（Settings）
│   ├── dependencies.py        # 公共依赖（get_db, get_current_user）
│   ├── models/                # ORM 模型
│   │   ├── __init__.py
│   │   └── user.py
│   ├── schemas/               # Pydantic 模型（请求/响应）
│   │   ├── __init__.py
│   │   └── user.py
│   ├── routers/               # 路由模块
│   │   ├── __init__.py
│   │   ├── users.py
│   │   └── items.py
│   ├── services/              # 业务逻辑层
│   │   ├── __init__.py
│   │   └── user_service.py
│   ├── middleware/            # 中间件
│   │   └── logging.py
│   ├── utils/                 # 工具函数
│   │   └── security.py
│   └── tests/                 # 测试
│       └── test_users.py
├── migrations/                # 数据库迁移
├── static/                    # 静态文件
├── .env                       # 环境变量
└── requirements.txt
```

---

## 三、实战场景题（6-10题）

### 3.1 AI 对话 API（流式输出 SSE）

```python
import json
import httpx
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import List

app = FastAPI()

class ChatMessage(BaseModel):
    role: str  # "user" | "assistant" | "system"
    content: str

class ChatRequest(BaseModel):
    messages: List[ChatMessage]
    stream: bool = True

async def stream_llm_response(messages: list) -> str:
    """流式调用 LLM API"""
    async with httpx.AsyncClient() as client:
        async with client.stream(
            "POST",
            "https://api.deepseek.com/v1/chat/completions",
            headers={
                "Authorization": "Bearer sk-xxx",
                "Content-Type": "application/json"
            },
            json={
                "model": "deepseek-chat",
                "messages": [m.model_dump() for m in messages],
                "stream": True
            },
            timeout=60
        ) as resp:
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    chunk = line[6:]
                    if chunk == "[DONE]":
                        break
                    data = json.loads(chunk)
                    content = data["choices"][0]["delta"].get("content", "")
                    if content:
                        yield f"data: {json.dumps({'content': content})}\n\n"

@app.post("/chat")
async def chat(request: ChatRequest):
    """流式聊天接口"""
    return StreamingResponse(
        stream_llm_response(request.messages),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # 禁用 Nginx 缓冲
        }
    )
```

### 3.2 文件上传与下载服务

```python
import os
import uuid
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
from pathlib import Path

app = FastAPI()
UPLOAD_DIR = Path("./uploads")
UPLOAD_DIR.mkdir(exist_ok=True)

@app.post("/upload")
async def upload_file(file: UploadFile = File(...)):
    """上传文件"""
    # 生成唯一文件名
    ext = Path(file.filename).suffix
    unique_name = f"{uuid.uuid4()}{ext}"
    file_path = UPLOAD_DIR / unique_name

    # 分段写入（避免大文件占用内存）
    with open(file_path, "wb") as f:
        while chunk := await file.read(1024 * 1024):  # 每次 1MB
            f.write(chunk)

    return {
        "filename": file.filename,
        "stored_as": unique_name,
        "size": file_path.stat().st_size,
    }

@app.get("/download/{filename}")
async def download_file(filename: str):
    """下载文件"""
    file_path = UPLOAD_DIR / filename
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="File not found")
    return FileResponse(
        path=file_path,
        filename=filename,
        media_type="application/octet-stream"
    )

# 限制上传大小
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware

class MaxSizeMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        if request.method == "POST" and request.url.path == "/upload":
            content_length = int(request.headers.get("content-length", 0))
            if content_length > 100 * 1024 * 1024:  # 100MB
                raise HTTPException(status_code=413, detail="File too large")
        return await call_next(request)

app.add_middleware(MaxSizeMiddleware)
```

### 3.3 数据库关联关系 CRUD（Tortoise ORM）

```python
from tortoise import fields, Tortoise
from tortoise.models import Model
from pydantic import BaseModel
from typing import List, Optional

# ===== 模型定义 =====

class Category(Model):
    """商品分类"""
    id = fields.IntField(pk=True)
    name = fields.CharField(max_length=50, unique=True)
    description = fields.TextField(null=True)
    products = fields.ReverseRelation["Product"]

class Product(Model):
    """商品（多对一：一个分类下有多个商品）"""
    id = fields.IntField(pk=True)
    name = fields.CharField(max_length=100)
    price = fields.DecimalField(max_digits=10, decimal_places=2)
    category = fields.ForeignKeyField("models.Category", related_name="products")

class Tag(Model):
    """标签（多对多：商品可以有多个标签）"""
    id = fields.IntField(pk=True)
    name = fields.CharField(max_length=30, unique=True)
    products = fields.ManyToManyField("models.Product", related_name="tags")

class Profile(Model):
    """商品扩展信息（一对一）"""
    id = fields.IntField(pk=True)
    description = fields.TextField()
    stock = fields.IntField(default=0)
    product = fields.OneToOneField("models.Product", related_name="profile")

# ===== 业务操作 =====

async def create_complete_product():
    """创建商品（含关联关系）"""
    # 1. 创建或获取分类
    category, _ = await Category.get_or_create(name="电子产品")

    # 2. 创建商品
    product = await Product.create(
        name="iPhone 15",
        price=6999.00,
        category=category
    )

    # 3. 创建标签并关联
    tag1, _ = await Tag.get_or_create(name="手机")
    tag2, _ = await Tag.get_or_create(name="苹果")
    await product.tags.add(tag1, tag2)

    # 4. 创建扩展信息（一对一）
    await Profile.create(description="最新款 iPhone", stock=100, product=product)

    # 5. 预加载关联关系
    product = await Product.get(id=product.id).prefetch_related(
        "category", "tags", "profile"
    )
    return product

async def query_with_relations():
    """关联查询"""
    # 查询分类下的所有商品（1对多）
    category = await Category.get(id=1).prefetch_related("products")
    products = category.products  # 该分类下的所有商品

    # 查询商品详情（1对1）
    product = await Product.get(id=1).prefetch_related("profile")
    stock = product.profile.stock

    # 查询商品的所有标签（多对多）
    product = await Product.get(id=1).prefetch_related("tags")
    tag_names = [tag.name for tag in product.tags]

    # 反向查询：标签下的所有商品
    tag = await Tag.get(id=1).prefetch_related("products")
    tagged_products = tag.products
```

| 关联类型 | Tortoise 字段 | 示例 |
|---------|---------------|------|
| 1 对 1 | `OneToOneField` | Product ↔ Profile |
| 1 对多 | `ForeignKeyField` | Category → Product |
| 多对多 | `ManyToManyField` | Product ↔ Tag |

### 3.4 请求限流实现

```python
import time
from collections import defaultdict
from fastapi import FastAPI, Request, HTTPException
from starlette.middleware.base import BaseHTTPMiddleware

app = FastAPI()

class RateLimitMiddleware(BaseHTTPMiddleware):
    """简单的 IP 限流中间件"""
    def __init__(self, app, max_requests: int = 60, window: int = 60):
        super().__init__(app)
        self.max_requests = max_requests
        self.window = window
        self.requests: dict[str, list[float]] = defaultdict(list)

    async def dispatch(self, request: Request, call_next):
        if request.url.path.startswith("/api/"):
            client_ip = request.client.host
            now = time.time()
            # 清理过期记录
            self.requests[client_ip] = [
                t for t in self.requests[client_ip]
                if now - t < self.window
            ]
            if len(self.requests[client_ip]) >= self.max_requests:
                raise HTTPException(status_code=429, detail="Too Many Requests")
            self.requests[client_ip].append(now)

        return await call_next(request)

app.add_middleware(RateLimitMiddleware, max_requests=60, window=60)
```

### 3.5 日志中间件与请求追踪

```python
import uuid
import time
import logging
from fastapi import FastAPI, Request
from starlette.middleware.base import BaseHTTPMiddleware

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("api")

app = FastAPI()

class RequestLogMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        # 生成请求唯一 ID
        request_id = str(uuid.uuid4())[:8]
        request.state.request_id = request_id

        # 记录请求开始
        start_time = time.perf_counter()
        logger.info(f"[{request_id}] {request.method} {request.url.path} - Start")

        # 执行请求
        try:
            response = await call_next(request)
            elapsed = time.perf_counter() - start_time
            logger.info(
                f"[{request_id}] {request.method} {request.url.path} "
                f"- {response.status_code} ({elapsed:.3f}s)"
            )
            response.headers["X-Request-ID"] = request_id
            return response
        except Exception as e:
            elapsed = time.perf_counter() - start_time
            logger.error(
                f"[{request_id}] {request.method} {request.url.path} "
                f"- Error: {e} ({elapsed:.3f}s)"
            )
            raise

app.add_middleware(RequestLogMiddleware)

@app.get("/api/users")
async def get_users(request: Request):
    rid = request.state.request_id  # 获取请求 ID
    return {"request_id": rid, "users": ["Alice", "Bob"]}
```

### 3.6 使用 AI 生成 API 接口（FastAPI + LLM）

```python
# FastAPI + AI 自动生成 API — 只需定义数据模型，AI 补全业务逻辑
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional

app = FastAPI()

# 1. 定义数据模型（数据库表对应）
class Student(BaseModel):
    id: int
    name: str
    age: int
    grade: str
    email: Optional[str] = None

class StudentCreate(BaseModel):
    name: str
    age: int
    grade: str
    email: Optional[str] = None

# 2. AI 自动生成以下 CRUD 接口（实际由开发者编写）
# FastAPI + AI 工具（如 Cursor/GitHub Copilot）可自动补全

# === 手动实现的 CRUD ===
students_db: dict[int, Student] = {}
next_id = 1

@app.post("/students", response_model=Student, status_code=201)
def create_student(data: StudentCreate):
    global next_id
    student = Student(id=next_id, **data.model_dump())
    students_db[next_id] = student
    next_id += 1
    return student

@app.get("/students", response_model=List[Student])
def list_students(grade: Optional[str] = None):
    students = list(students_db.values())
    if grade:
        students = [s for s in students if s.grade == grade]
    return students

@app.get("/students/{student_id}", response_model=Student)
def get_student(student_id: int):
    student = students_db.get(student_id)
    if not student:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="Student not found")
    return student

@app.put("/students/{student_id}", response_model=Student)
def update_student(student_id: int, data: StudentCreate):
    if student_id not in students_db:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="Student not found")
    updated = Student(id=student_id, **data.model_dump())
    students_db[student_id] = updated
    return updated

@app.delete("/students/{student_id}")
def delete_student(student_id: int):
    if student_id not in students_db:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="Student not found")
    del students_db[student_id]
    return {"message": "Deleted"}
```

### 3.7 数据过滤与分页封装

```python
from fastapi import FastAPI, Query
from pydantic import BaseModel
from typing import Generic, TypeVar, List, Optional

T = TypeVar("T")

# === 通用分页模型 ===
class PaginationParams:
    """分页参数依赖"""
    def __init__(
        self,
        page: int = Query(1, ge=1, description="页码"),
        size: int = Query(20, ge=1, le=100, description="每页数量"),
        sort_by: Optional[str] = Query(None, description="排序字段"),
        sort_order: Optional[str] = Query("asc", regex="^(asc|desc)$"),
    ):
        self.page = page
        self.size = size
        self.sort_by = sort_by
        self.sort_order = sort_order

    @property
    def offset(self) -> int:
        return (self.page - 1) * self.size

class Page(BaseModel, Generic[T]):
    """通用分页响应"""
    items: List[T]
    total: int
    page: int
    size: int
    total_pages: int

# === 过滤条件模型 ===
class ProductFilter:
    """产品过滤参数依赖"""
    def __init__(
        self,
        name: Optional[str] = Query(None, min_length=1),
        min_price: Optional[float] = Query(None, ge=0),
        max_price: Optional[float] = Query(None, ge=0),
        category: Optional[str] = Query(None),
        in_stock: Optional[bool] = Query(None),
    ):
        self.name = name
        self.min_price = min_price
        self.max_price = max_price
        self.category = category
        self.in_stock = in_stock

# === 使用 ===
app = FastAPI()

@app.get("/products", response_model=Page[dict])
def list_products(
    pagination: PaginationParams = Depends(),
    filters: ProductFilter = Depends(),
):
    """商品列表（分页 + 过滤）
    GET /products?page=1&size=20&category=elec&min_price=100&sort_by=price&sort_order=desc
    """
    # 实际从数据库查询
    return Page(
        items=[{"id": 1, "name": "Product"}],
        total=100,
        page=pagination.page,
        size=pagination.size,
        total_pages=5,
    )
```

### 3.8 FastAPI + WebSocket 实时通知

```python
import asyncio
import json
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from typing import Set

app = FastAPI()

# 在线用户管理
class ConnectionManager:
    def __init__(self):
        self.connections: Set[WebSocket] = set()

    async def connect(self, ws: WebSocket):
        await ws.accept()
        self.connections.add(ws)

    def disconnect(self, ws: WebSocket):
        self.connections.discard(ws)

    async def broadcast(self, message: dict):
        dead = set()
        for ws in self.connections:
            try:
                await ws.send_json(message)
            except Exception:
                dead.add(ws)
        self.connections -= dead

manager = ConnectionManager()

@app.websocket("/ws/notifications")
async def notification_ws(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            data = await websocket.receive_text()
            # 处理客户端消息
            msg = json.loads(data)
            await manager.broadcast({"type": "notification", "data": msg})
    except WebSocketDisconnect:
        manager.disconnect(websocket)

# HTTP 触发 WebSocket 广播
@app.post("/notify")
async def send_notification(message: str):
    await manager.broadcast({"type": "broadcast", "content": message})
    return {"sent": True}
```

---

## 四、手写代码题（5-8题）

### 4.1 手写 FastAPI CRUD 路由（Pydantic + SQLAlchemy）

```python
from fastapi import FastAPI, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy import create_engine, Column, Integer, String, Boolean
from sqlalchemy.orm import Session, declarative_base, sessionmaker
from typing import List, Optional

# === 数据库配置 ===
SQLALCHEMY_DATABASE_URL = "sqlite:///./todos.db"
engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# === ORM 模型 ===
class TodoModel(Base):
    __tablename__ = "todos"
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    description = Column(String, default="")
    completed = Column(Boolean, default=False)

# === Pydantic 模型 ===
class TodoBase(BaseModel):
    title: str
    description: Optional[str] = ""
    completed: bool = False

class TodoCreate(TodoBase):
    pass

class TodoUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    completed: Optional[bool] = None

class Todo(TodoBase):
    id: int
    class Config:
        from_attributes = True

# === 依赖 ===
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# === FastAPI 应用 ===
app = FastAPI(title="Todo API")

# === CRUD 路由 ===

@app.post("/todos", response_model=Todo, status_code=status.HTTP_201_CREATED)
def create_todo(todo: TodoCreate, db: Session = Depends(get_db)):
    db_todo = TodoModel(**todo.model_dump())
    db.add(db_todo)
    db.commit()
    db.refresh(db_todo)
    return db_todo

@app.get("/todos", response_model=List[Todo])
def list_todos(
    skip: int = 0,
    limit: int = 100,
    completed: Optional[bool] = None,
    db: Session = Depends(get_db)
):
    query = db.query(TodoModel)
    if completed is not None:
        query = query.filter(TodoModel.completed == completed)
    return query.offset(skip).limit(limit).all()

@app.get("/todos/{todo_id}", response_model=Todo)
def get_todo(todo_id: int, db: Session = Depends(get_db)):
    todo = db.query(TodoModel).filter(TodoModel.id == todo_id).first()
    if not todo:
        raise HTTPException(status_code=404, detail="Todo not found")
    return todo

@app.put("/todos/{todo_id}", response_model=Todo)
def update_todo(todo_id: int, update: TodoUpdate, db: Session = Depends(get_db)):
    db_todo = db.query(TodoModel).filter(TodoModel.id == todo_id).first()
    if not db_todo:
        raise HTTPException(status_code=404, detail="Todo not found")
    update_data = update.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(db_todo, key, value)
    db.commit()
    db.refresh(db_todo)
    return db_todo

@app.delete("/todos/{todo_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_todo(todo_id: int, db: Session = Depends(get_db)):
    db_todo = db.query(TodoModel).filter(TodoModel.id == todo_id).first()
    if not db_todo:
        raise HTTPException(status_code=404, detail="Todo not found")
    db.delete(db_todo)
    db.commit()
    return None

# === 启动 ===
if __name__ == "__main__":
    Base.metadata.create_all(bind=engine)
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

### 4.2 手写依赖注入（认证 + 数据库）

```python
from fastapi import FastAPI, Depends, HTTPException, Header
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import Optional

app = FastAPI()
security = HTTPBearer()

# === 依赖 1：JWT 认证 ===
def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """验证 Bearer Token"""
    token = credentials.credentials
    # 实际用 jose 库解析 JWT
    if not token.startswith("valid_"):
        raise HTTPException(status_code=401, detail="Invalid token")
    return {"user_id": 1, "username": "alice"}

# === 依赖 2：数据库会话（模拟）===
class DatabaseSession:
    def __init__(self):
        self.connected = True

    def query(self, sql: str):
        return f"Result of: {sql}"

def get_db():
    db = DatabaseSession()
    try:
        yield db
    finally:
        print("Closing DB session")

# === 依赖 3：权限检查 ===
def require_admin(user: dict = Depends(verify_token)):
    if user.get("username") != "admin":
        raise HTTPException(status_code=403, detail="Admin only")
    return user

# === 路由：使用多重依赖 ===
@app.get("/profile")
def get_profile(
    user: dict = Depends(verify_token),
    db: DatabaseSession = Depends(get_db)
):
    return {"user": user, "db_status": db.connected}

@app.get("/admin/dashboard")
def admin_dashboard(
    admin: dict = Depends(require_admin),
    db: DatabaseSession = Depends(get_db)
):
    return {"admin": admin, "dashboard": "Secret data"}
```

### 4.3 手写中间件（请求日志 + 性能监控）

```python
import time
import logging
from fastapi import FastAPI, Request
from starlette.middleware.base import BaseHTTPMiddleware

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("api.monitor")

class PerformanceMiddleware(BaseHTTPMiddleware):
    """性能监控中间件：记录每个请求的耗时和状态码"""

    async def dispatch(self, request: Request, call_next):
        start = time.perfcounter()
        response = await call_next(request)
        elapsed = time.perf_counter() - start

        logger.info(
            f"[{request.method}] {request.url.path} "
            f"→ {response.status_code} | {elapsed:.4f}s"
        )

        # 添加自定义响应头
        response.headers["X-Response-Time"] = f"{elapsed:.4f}s"

        # 慢请求告警（超过 1 秒）
        if elapsed > 1.0:
            logger.warning(
                f"SLOW REQUEST: {request.method} {request.url.path} "
                f"took {elapsed:.4f}s"
            )

        return response

app = FastAPI()
app.add_middleware(PerformanceMiddleware)

@app.get("/fast")
async def fast_endpoint():
    return {"speed": "fast"}

@app.get("/slow")
async def slow_endpoint():
    time.sleep(1.5)
    return {"speed": "slow"}
```

### 4.4 手写 Pydantic 数据模型（含校验）

```python
from pydantic import BaseModel, Field, EmailStr, field_validator, model_validator
from datetime import datetime
from typing import Optional, List
from enum import Enum

class OrderStatus(str, Enum):
    PENDING = "pending"
    PAID = "paid"
    SHIPPED = "shipped"
    COMPLETED = "completed"
    CANCELLED = "cancelled"

class Address(BaseModel):
    """嵌套模型"""
    province: str = Field(..., min_length=2)
    city: str = Field(..., min_length=2)
    district: str = Field(..., min_length=2)
    detail: str = Field(..., min_length=5, max_length=200)
    zip_code: str = Field(..., pattern=r"^\d{6}$")

class OrderItem(BaseModel):
    product_id: int = Field(..., gt=0)
    product_name: str = Field(..., min_length=1)
    quantity: int = Field(..., ge=1, le=100)
    unit_price: float = Field(..., gt=0)

    @field_validator("unit_price")
    @classmethod
    def price_precision(cls, v: float) -> float:
        return round(v, 2)

class OrderCreate(BaseModel):
    user_id: int = Field(..., gt=0)
    email: EmailStr
    address: Address
    items: List[OrderItem] = Field(..., min_length=1)

    @model_validator(mode="after")
    def validate_total(self):
        """校验订单总额不超过 100 万"""
        total = sum(item.unit_price * item.quantity for item in self.items)
        if total > 1_000_000:
            raise ValueError("Order total exceeds 1,000,000 limit")
        return self

class OrderResponse(BaseModel):
    order_id: int
    status: OrderStatus = OrderStatus.PENDING
    total_amount: float
    created_at: datetime
    items: List[OrderItem]

    class Config:
        from_attributes = True
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }
```

### 4.5 手写异步批量导入接口

```python
import csv
import io
from fastapi import FastAPI, UploadFile, File, BackgroundTasks
from pydantic import BaseModel
from typing import List

app = FastAPI()

class ImportResult(BaseModel):
    total: int
    success: int
    failed: int
    errors: List[str]

@app.post("/import/users", response_model=ImportResult)
async def import_users(file: UploadFile = File(...)):
    """批量导入用户（CSV 格式）"""
    if not file.filename.endswith(".csv"):
        from fastapi import HTTPException
        raise HTTPException(status_code=400, detail="Only CSV files supported")

    content = await file.read()
    text = content.decode("utf-8-sig")  # 处理 BOM 头
    reader = csv.DictReader(io.StringIO(text))

    result = ImportResult(total=0, success=0, failed=0, errors=[])

    for row in reader:
        result.total += 1
        try:
            name = row.get("name", "").strip()
            email = row.get("email", "").strip()
            if not name or not email:
                raise ValueError("name and email are required")
            # 实际插入数据库
            # db.add(User(name=name, email=email))
            result.success += 1
        except Exception as e:
            result.failed += 1
            result.errors.append(f"Row {result.total}: {str(e)}")

    return result
```

### 4.6 手写 WebSocket 聊天室

```python
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from typing import Dict, Set
import json

app = FastAPI()

class ChatRoom:
    def __init__(self):
        self.rooms: Dict[str, Set[WebSocket]] = {}

    async def join(self, room: str, ws: WebSocket):
        await ws.accept()
        if room not in self.rooms:
            self.rooms[room] = set()
        self.rooms[room].add(ws)
        await self.broadcast(room, {"type": "system", "message": "A user joined"})

    def leave(self, room: str, ws: WebSocket):
        if room in self.rooms:
            self.rooms[room].discard(ws)
            if not self.rooms[room]:
                del self.rooms[room]

    async def broadcast(self, room: str, message: dict):
        if room not in self.rooms:
            return
        dead = set()
        for ws in self.rooms[room]:
            try:
                await ws.send_json(message)
            except Exception:
                dead.add(ws)
        self.rooms[room] -= dead

chat = ChatRoom()

@app.websocket("/ws/chat/{room_name}")
async def chat_websocket(websocket: WebSocket, room_name: str):
    await chat.join(room_name, websocket)
    try:
        while True:
            data = await websocket.receive_text()
            msg = json.loads(data)
            await chat.broadcast(room_name, {
                "type": "message",
                "sender": msg.get("sender", "anonymous"),
                "content": msg.get("content", ""),
                "room": room_name,
            })
    except WebSocketDisconnect:
        chat.leave(room_name, websocket)
        await chat.broadcast(room_name, {"type": "system", "message": "A user left"})
```

### 4.7 手写异步爬虫（FastAPI + httpx）

```python
import httpx
import asyncio
from fastapi import FastAPI
from typing import List, Dict

app = FastAPI()

async def fetch_single(url: str, client: httpx.AsyncClient) -> Dict:
    """爬取单个 URL"""
    try:
        resp = await client.get(url, timeout=10)
        resp.raise_for_status()
        return {"url": url, "status": resp.status_code, "length": len(resp.text)}
    except Exception as e:
        return {"url": url, "error": str(e)}

@app.post("/crawl")
async def crawl_urls(urls: List[str]):
    """批量爬取 URL（并发）"""
    async with httpx.AsyncClient(
        limits=httpx.Limits(max_connections=20),
        timeout=30.0
    ) as client:
        tasks = [fetch_single(url, client) for url in urls]
        results = await asyncio.gather(*tasks)
        return {"total": len(urls), "results": results}
```

### 4.8 手写分页中间件

```python
from fastapi import FastAPI, Query
from pydantic import BaseModel
from typing import Generic, TypeVar, List, Optional

T = TypeVar("T")

class Page(BaseModel, Generic[T]):
    """通用分页响应"""
    items: List[T]
    total: int
    page: int
    page_size: int
    total_pages: int
    has_next: bool
    has_prev: bool

def paginate(
    items: List[T],
    total: int,
    page: int,
    page_size: int
) -> Page[T]:
    total_pages = max(1, (total + page_size - 1) // page_size)
    return Page(
        items=items,
        total=total,
        page=page,
        page_size=page_size,
        total_pages=total_pages,
        has_next=page < total_pages,
        has_prev=page > 1,
    )

app = FastAPI()

class UserOut(BaseModel):
    id: int
    name: str

@app.get("/users/paginated", response_model=Page[UserOut])
def list_users_paginated(
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=100)
):
    # 模拟数据
    total = 100
    data = [
        UserOut(id=i, name=f"User{i}")
        for i in range((page-1)*page_size, min(page*page_size, total))
    ]
    return paginate(data, total, page, page_size)
```

---

## 五、系统设计题（3-5题）

### 5.1 设计电商平台订单 API

```text
┌──────────────────────────────────────────────┐
│              客户端 (Web/App)                   │
└──────────────────┬───────────────────────────┘
                   │ HTTP / WebSocket
┌──────────────────▼───────────────────────────┐
│           API 网关 (FastAPI)                    │
│  ┌─────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ 认证     │ │ 限流     │ │ 请求日志      │   │
│  │ (JWT)   │ │ (Redis)  │ │ (结构化日志)  │   │
│  └─────────┘ └──────────┘ └──────────────┘   │
└────┬────┬────┬────────────┬─────────┬────────┘
     │    │    │            │         │
┌────▼┐ ┌▼───┐┌▼────────┐ ┌▼───────┐┌▼────────┐
│商品  ││购物││订单      │ │支付     ││用户      │
│服务  ││车  ││服务      │ │服务     ││服务      │
└────┬┘ └┬───┘└────┬────┘ └───┬────┘└────┬────┘
     │   │         │          │           │
┌────▼───▼─────────▼──────────▼──────────▼────┐
│               数据存储层                        │
│  PostgreSQL (主) + Redis (缓存)               │
│  Elasticsearch (商品搜索)                      │
└─────────────────────────────────────────────┘
```

**核心 API 设计：**

| 端点 | 方法 | 说明 |
|------|------|------|
| `/products` | GET | 商品列表（分页 + 过滤） |
| `/products/{id}` | GET | 商品详情 |
| `/cart` | GET/POST/PUT/DELETE | 购物车 CRUD |
| `/orders` | POST | 创建订单 |
| `/orders/{id}` | GET | 订单详情 |
| `/orders/{id}/pay` | POST | 支付 |
| `/orders/{id}/cancel` | POST | 取消订单 |

**关键实现要点：**

```python
from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum
from datetime import datetime

class OrderStatus(str, Enum):
    PENDING = "pending"
    PAID = "paid"
    SHIPPED = "shipped"
    DELIVERED = "delivered"
    CANCELLED = "cancelled"

class OrderCreate(BaseModel):
    address_id: int
    items: List[dict] = Field(..., min_length=1)

class OrderResponse(BaseModel):
    id: int
    user_id: int
    status: OrderStatus
    total_amount: float
    created_at: datetime
    items: list

@app.post("/orders", status_code=201)
async def create_order(order: OrderCreate, user=Depends(get_current_user)):
    """创建订单（包含事务处理）"""
    # 1. 验证库存
    # 2. 锁定库存
    # 3. 计算价格
    # 4. 创建订单
    # 5. 清空购物车
    # 6. 异步发送通知
    pass
```

### 5.2 设计 SaaS AI 平台后端架构

```text
┌────────────────────────────────────────────┐
│           多租户 SaaS AI 平台                │
├────────────────────────────────────────────┤
│ 认证层: JWT + API Key (租户隔离)            │
├────────────────────────────────────────────┤
│ API 层: FastAPI + APIRouter (模块化)        │
│  - /api/v1/chat        对话接口              │
│  - /api/v1/completion  补全接口              │
│  - /api/v1/embeddings  向量化接口            │
│  - /api/v1/files       文件管理              │
│  - /api/v1/knowledge   知识库管理             │
├────────────────────────────────────────────┤
│ 业务层: Services + LLM Gateway              │
│  - 模型路由 (DeepSeek / GPT / Claude)       │
│  - 负载均衡 + 熔断降级                       │
│  - Token 用量统计                           │
├────────────────────────────────────────────┤
│ 存储层: PostgreSQL + Redis + Milvus         │
│  - 用户/租户/配置 → PostgreSQL              │
│  - 会话缓存/限流 → Redis                    │
│  - 知识库向量 → Milvus                      │
└────────────────────────────────────────────┘
```

**关键设计模式：**

```python
# 租户隔离中间件
@app.middleware("http")
async def tenant_middleware(request: Request, call_next):
    api_key = request.headers.get("X-API-Key", "")
    tenant = await get_tenant_by_api_key(api_key)
    request.state.tenant_id = tenant.id
    request.state.tenant_config = tenant.config
    return await call_next(request)

# 多模型路由
class LLMRouter:
    def __init__(self):
        self.models = {
            "deepseek-chat": DeepSeekClient(),
            "gpt-4": OpenAIClient(),
            "claude-3": AnthropicClient(),
        }

    async def chat(self, model: str, messages: list, config: dict):
        client = self.models.get(model)
        if not client:
            raise HTTPException(status_code=400, detail=f"Unknown model: {model}")
        return await client.chat(messages, **config)
```

### 5.3 设计高可用文件上传服务

```text
┌─────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ 客户端   │ → │ FastAPI  │ → │ 预处理   │ → │ 对象存储  │
│ (分片)  │   │ 上传接口 │   │ (校验/    │   │ (S3/MinIO)│
└─────────┘   └──────────┘   │ 缩略图/   │   └──────────┘
                             │ 病毒扫描) │
                             └──────────┘

1. 分片上传：大文件分成 5MB 切片，支持断点续传
2. 秒传：文件 MD5 哈希检查，已存在则直接返回
3. 预处理：Lambda 函数异步处理（缩略图、转码）
4. CDN 加速：上传完成后通过 CDN 提供下载
```

**分片上传实现：**

```python
import hashlib
from fastapi import FastAPI, UploadFile, File, HTTPException
from pydantic import BaseModel

app = FastAPI()

# 内存存储分片信息（生产用 Redis）
uploads: dict = {}

class InitUploadResponse(BaseModel):
    upload_id: str
    chunk_size: int = 5 * 1024 * 1024  # 5MB

class CompleteUploadRequest(BaseModel):
    upload_id: str
    filename: str
    total_chunks: int

@app.post("/upload/init")
def init_upload(filename: str, file_size: int):
    """初始化分片上传"""
    import uuid
    upload_id = str(uuid.uuid4())
    uploads[upload_id] = {
        "filename": filename,
        "file_size": file_size,
        "chunks": {},
        "created_at": datetime.now()
    }
    return InitUploadResponse(upload_id=upload_id)

@app.post("/upload/{upload_id}/chunk/{chunk_number}")
async def upload_chunk(upload_id: str, chunk_number: int, file: UploadFile = File(...)):
    """上传分片"""
    if upload_id not in uploads:
        raise HTTPException(status_code=404, detail="Upload not found")

    content = await file.read()
    md5 = hashlib.md5(content).hexdigest()
    uploads[upload_id]["chunks"][chunk_number] = {
        "data": content,
        "md5": md5
    }
    return {"chunk": chunk_number, "md5": md5}

@app.post("/upload/complete")
async def complete_upload(req: CompleteUploadRequest):
    """合并分片"""
    if req.upload_id not in uploads:
        raise HTTPException(status_code=404, detail="Upload not found")

    info = uploads[req.upload_id]
    if len(info["chunks"]) != req.total_chunks:
        raise HTTPException(status_code=400, detail="Missing chunks")

    # 按顺序合并
    with open(f"./uploads/{req.filename}", "wb") as f:
        for i in range(req.total_chunks):
            f.write(info["chunks"][i]["data"])

    del uploads[req.upload_id]
    return {"message": "Upload complete", "filename": req.filename}
```

### 5.4 设计 API 版本管理策略

```text
策略一：URL 路径版本（推荐）
GET /api/v1/users
GET /api/v2/users

策略二：请求头版本
Accept: application/vnd.api+json;version=1

策略三：查询参数版本
GET /api/users?version=1
```

```python
from fastapi import FastAPI, APIRouter

# === v1 路由 ===
router_v1 = APIRouter(prefix="/api/v1", tags=["v1"])

@router_v1.get("/users")
def list_users_v1():
    return [{"id": 1, "name": "Alice"}]  # 简单版本

# === v2 路由（增强版）===
router_v2 = APIRouter(prefix="/api/v2", tags=["v2"])

class UserV2(BaseModel):
    id: int
    name: str
    email: str
    created_at: datetime

@router_v2.get("/users", response_model=List[UserV2])
def list_users_v2(
    pagination: PaginationParams = Depends(),
    filters: UserFilter = Depends()
):
    """v2 版本：支持分页和过滤"""
    pass

# === 注册 ===
app = FastAPI()
app.include_router(router_v1)
app.include_router(router_v2)
```

> 💡 推荐使用 URL 路径版本（`/api/v1/`、`/api/v2/`），最直观、最容易缓存、无需自定义 Header。保持 v1 和 v2 共存，给客户端迁移时间。

---

## 六、常见坑点与最佳实践（表格：坑点|原因|解决方案）

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| **async def 中同步阻塞** | `async def` 路由调用 `time.sleep()` 阻塞事件循环 | 用 `asyncio.sleep()` 或改用 `def` 路由（自动在线程池执行） |
| **Pydantic orm_mode 未配置** | Pydantic v2 默认无法从 ORM 模型创建实例 | 配置 `from_attributes = True`（替代 v1 的 orm_mode） |
| **response_model 误用** | 返回 User 模型但密码字段被泄露给客户端 | 使用 `response_model=UserOut` 定义只包含安全字段的响应模型 |
| **Depends 调用带括号** | 在路由中写 `Depends(get_db())` 多写了括号 | 正确写法：`Depends(get_db)` 传函数引用而非调用结果 |
| **CORS 配置错误** | `allow_origins=["*"]` 导致浏览器阻止带凭据的请求 | 生产环境显式指定允许的域名列表，使用 `allow_origin_regex` 匹配 |
| **表单 File 和 UploadFile 混淆** | 大文件直接 `bytes = File()` 全量读入内存导致 OOM | 小文件用 `bytes`，大文件（>1MB）用 `UploadFile` 流式读取 |
| **数据库 N+1 查询** | 循环中逐个查询关联对象导致大量 SQL | 使用 `selectinload` / `prefetch_related` 预加载关联关系 |
| **中间件顺序错误** | CORS 中间件添加在其他中间件后面导致跨域失效 | CORS 中间件应最先添加（`app.add_middleware(CORSMiddleware, ...)` 排第一） |
| **同步 Session 阻塞事件循环** | 异步路由中使用 SQLAlchemy 同步 Session | 异步路由必须用 `AsyncSession` + `asyncpg` 驱动 |
| **TestClient 异步测试失败** | 异步路由直接用 `TestClient` 测试报错 | 使用 `pytest.mark.anyio` 装饰器或 httpx `AsyncClient` |
| **忽略 lifespan 事件** | 启动时创建连接池但关闭时不释放 | 使用 `lifespan` 上下文管理器管理资源生命周期，确保 finally 执行资源清理 |
| **Query 校验不生效** | `Query(min_length=1)` 参数名与函数参数名不匹配 | 确保参数名与路由函数参数名一致，FastAPI 按名称匹配校验器 |

---

## 七、面试回答模板（Top 5 高频题的结构化回答模板）

### 7.1 "FastAPI 和 Flask 有什么区别？"

**回答框架（4 层）：**

> **第一层：核心差异**
> FastAPI 是**异步原生**的框架，而 Flask 是**同步**的。FastAPI 基于 Starlette + Pydantic，天然支持 async/await；Flask 基于 Werkzeug，异步需要额外插件（Quart）。
>
> | 维度 | FastAPI | Flask |
> |------|---------|-------|
> | 异步支持 | 原生 async/await | 需 Quart |
> | 类型校验 | Pydantic 自动 | 手动 / marshmallow |
> | API 文档 | 自动 OpenAPI + Swagger | 需 flasgger |
> | 性能 | ~50k req/s | ~15k req/s |
> | 学习成本 | 中等（类型注解 + 异步） | 低 |
>
> **第二层：性能差异原因**
> FastAPI 的底层 Starlette 使用 asyncio 事件循环处理请求，单机吞吐量大约是 Flask 的 2-3 倍。与 Node.js/Go 在同一量级。
>
> **第三层：场景选型**
> - 新项目选 FastAPI，尤其是 AI/ML 服务、微服务架构
> - 老项目或团队同步编程经验丰富，可继续用 Flask
>
> **第四层：AI 场景优势**
> FastAPI + `httpx.AsyncClient` 调用 LLM API，可以充分利用异步非阻塞特性，实现高并发 AI 服务。流式输出 SSE 支持也原生更好。

### 7.2 "FastAPI 的依赖注入是怎么工作的？"

**回答框架（4 层）：**

> **第一层：概念理解**
> 依赖注入是一种设计模式，让框架负责创建和注入依赖对象，而不是在函数内部手动创建。FastAPI 的 DI 通过 `Depends()` 函数实现。
>
> **第二层：基本用法**
> ```python
> @app.get("/items")
> def list_items(db: Session = Depends(get_db)):
>     return db.query(Item).all()
> ```
> `Depends(get_db)` 声明路由需要 `get_db()` 的返回值，FastAPI 自动调用该函数并注入结果。
>
> **第三层：高级特性**
> - **可组合**：依赖可以嵌套，形成依赖树
> - **共享**：同一请求中多次调用同一依赖，结果缓存
> - **生命周期**：可声明请求级、应用级依赖
> - **可清理**：生成器函数中的 `finally` 在请求结束后执行
>
> **第四层：与传统框架对比**
> 相比 Spring Boot 的 `@Autowired`（通过反射实现），FastAPI 的 DI 更透明、类型安全，所有依赖都可以在 IDE 中追踪。

### 7.3 "FastAPI 如何处理异步？同步和异步路由有什么区别？"

**回答框架（4 层）：**

> **第一层：两种路由模式**
> FastAPI 同时支持 `def`（同步）和 `async def`（异步）路由。同步路由在**线程池**中运行，异步路由在**事件循环**中运行。
>
> **第二层：执行机制**
> ```python
> @app.get("/sync")
> def sync_endpoint():
>     time.sleep(1)  # 在线程池执行，不阻塞主循环
>     return {"mode": "sync"}
>
> @app.get("/async")
> async def async_endpoint():
>     await asyncio.sleep(1)  # 在事件循环执行
>     return {"mode": "async"}
> ```
>
> **第三层：选型原则**
> - I/O 密集型（数据库查询、HTTP 调用）→ `async def`
> - CPU 密集型（图像处理、计算）→ `def`（利用线程池多核）
> - 调用同步库（如 `requests`）→ `def`
>
> **第四层：性能考量**
> Uvicorn 默认使用单进程事件循环。对于 CPU 密集型任务，配合 `--workers 4` 使用多进程。但对于 I/O 密集型，异步优势巨大：单个 worker 可处理数千并发连接。

### 7.4 "FastAPI 的 Pydantic 模型校验是怎么工作的？"

**回答框架（4 层）：**

> **第一层：类型驱动校验**
> Pydantic 利用 Python 类型注解在**运行时**进行数据验证。当请求到达时，FastAPI 自动将 JSON 数据转换为 Pydantic 模型实例，并执行校验。
>
> **第二层：校验流程**
> ```text
> 请求 JSON → Pydantic 类型转换 → 字段校验器 (@field_validator)
> → 模型校验器 (@model_validator) → 返回校验后的模型对象
> ```
>
> **第三层：校验规则**
> - 类型自动转换（`"123"` → `123`）
> - `Field(gt=0, le=100)` 数值范围
> - `Field(min_length=1, max_length=50)` 字符串长度
> - `@field_validator` 自定义字段校验
> - `@model_validator` 跨字段校验
>
> **第四层：v2 性能优势**
> Pydantic v2 用 Rust（pydantic-core）重写了校验引擎，校验速度比 v1 快 5-50 倍。与 FastAPI 配合，即使百万次请求校验开销也很小。

### 7.5 "FastAPI 如何做认证和鉴权？"

**回答框架（4 层）：**

> **第一层：OAuth2 认证流程**
> FastAPI 内置 `OAuth2PasswordBearer`，自动提取请求头的 `Authorization: Bearer <token>`。
>
> **第二层：JWT 实现**
> ```python
> from jose import jwt
>
> # 生成 Token
> token = jwt.encode({"sub": username, "exp": expire}, SECRET_KEY, algorithm="HS256")
>
> # 验证 Token（作为依赖注入）
> user = Depends(get_current_user)
> ```
>
> **第三层：依赖注入实现权限控制**
> 通过 `Depends(get_current_user)` 注入到需要认证的路由。可以进一步定义 `Depends(require_admin)` 实现角色权限控制。
>
> **第四层：最佳实践**
> - 密码用 `passlib` 加 bcrypt 哈希
> - Token 设置过期时间，支持 refresh_token
> - 敏感操作二次验证
> - 使用 HTTPS 传输，防止 Token 被截获

---

## 八、快速查漏补缺 Checklist

> 面试前逐项确认已掌握，未掌握的先标记，优先补齐

### 基础概念
- [ ] FastAPI 核心特点（异步、Pydantic、Swagger）
- [ ] FastAPI vs Flask vs Django 对比
- [ ] 路径参数、查询参数、请求体的区别
- [ ] Pydantic 模型定义与校验（Field / validator）
- [ ] 响应模型（response_model）过滤
- [ ] 异步路由 `async def` vs 同步路由 `def`
- [ ] 表单数据和文件上传处理
- [ ] Request 对象直接访问
- [ ] 多种响应类型（JSON / HTML / File / Streaming）
- [ ] BackgroundTasks 后台任务
- [ ] 静态文件服务与生命周期事件
- [ ] WebSocket 支持
- [ ] 异常处理与自定义异常处理器
- [ ] APIRouter 路由拆分
- [ ] OpenAPI / Swagger 自动文档生成

### 核心进阶
- [ ] 依赖注入（Depends）原理与使用
- [ ] Pydantic v1 vs v2 差异
- [ ] 中间件执行流程与自定义
- [ ] CORS 跨域配置
- [ ] SQLAlchemy 异步集成（AsyncSession）
- [ ] Tortoise ORM 配置与 CRUD
- [ ] Aerich 迁移工具使用
- [ ] 模型关联关系（1对1、1对多、多对多）
- [ ] OAuth2 + JWT 认证流程
- [ ] 请求限流实现
- [ ] 日志中间件与请求追踪
- [ ] TestClient 测试

### 实战场景
- [ ] AI 对话 API 流式输出（SSE）
- [ ] 文件上传下载服务
- [ ] 数据库关联查询与预加载
- [ ] 数据过滤与分页封装（通用 Page 模型）
- [ ] WebSocket 实时聊天/通知
- [ ] 批量导入接口
- [ ] 异步爬虫

### 手写代码
- [ ] 手写完整 CRUD 路由（Pydantic + SQLAlchemy）
- [ ] 手写依赖注入（认证 + 数据库）
- [ ] 手写中间件（日志 + 性能监控）
- [ ] 手写 Pydantic 模型（含自定义校验）
- [ ] 手写 WebSocket 聊天室
- [ ] 手写异步批量导入接口
- [ ] 手写分页封装
- [ ] 手写分片上传

### 性能与部署
- [ ] 异步数据库驱动选择（asyncpg vs aiomysql）
- [ ] GZip 压缩配置
- [ ] 连接池复用（httpx / 数据库）
- [ ] 热点数据缓存（lru_cache / Redis）
- [ ] ORM N+1 查询优化
- [ ] Pydantic v2 Rust 后端优势
- [ ] Uvicorn 多 workers 部署
- [ ] Docker + Docker Compose 编排
- [ ] Nginx 反向代理 + SSL

### 整体评估
- [ ] FastAPI 路由定义与参数校验
- [ ] Pydantic 模型设计与校验
- [ ] 依赖注入系统
- [ ] 异步编程（async/await / asyncio）
- [ ] 数据库集成（SQLAlchemy / Tortoise ORM）
- [ ] 认证鉴权（OAuth2 / JWT）
- [ ] 中间件与跨域
- [ ] 错误处理
- [ ] 测试（TestClient）
- [ ] 部署与运维

---

> 🎯 **面试核心策略**：FastAPI 面试的核心亮点在于**异步性能 + Pydantic 类型安全 + 自动文档 + 依赖注入**四个关键词。准备 1-2 个完整项目（AI 对话 API、电商订单系统、文件上传服务），可以现场讲解架构设计，展现对 FastAPI 生态的深入理解和工程化实践经验。
