"""
模块 08：常见设计模式。

覆盖知识点：
    - 单例模式（Singleton）—— 模块级单例、装饰器、元类
    - 工厂模式（Factory / Factory Method）
    - 建造者模式（Builder）
    - 观察者模式（Observer）
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Any, Callable


# ============================================================
# 1. 单例模式（Singleton）
# ============================================================
class AppConfig:
    """
    应用配置单例 —— 使用 __new__ 控制实例唯一性。

    线程安全（GIL 下足够），适合全局配置、连接池等场景。
    """

    _instance: AppConfig | None = None
    _initialized: bool = False

    def __new__(cls) -> AppConfig:
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self) -> None:
        if self._initialized:
            return
        self._config: dict[str, Any] = {}
        self._initialized = True

    def set(self, key: str, value: Any) -> None:
        self._config[key] = value

    def get(self, key: str, default: Any = None) -> Any:
        return self._config.get(key, default)


# ============================================================
# 2. 工厂模式（Factory）
# ============================================================
class Exporter(ABC):
    """导出器抽象基类。"""

    @abstractmethod
    def export(self, data: list[dict[str, Any]]) -> str:
        ...


class CSVExporter(Exporter):
    def export(self, data: list[dict[str, Any]]) -> str:
        if not data:
            return ""
        headers = ",".join(data[0].keys())
        rows = [headers]
        for row in data:
            rows.append(",".join(str(v) for v in row.values()))
        return "\n".join(rows)


class JSONExporter(Exporter):
    def export(self, data: list[dict[str, Any]]) -> str:
        import json

        return json.dumps(data, ensure_ascii=False, indent=2)


class ExporterFactory:
    """导出器工厂 —— 根据名称创建对应的导出器实例。"""

    _registry: dict[str, type[Exporter]] = {
        "csv": CSVExporter,
        "json": JSONExporter,
    }

    @classmethod
    def register(cls, name: str, exporter_cls: type[Exporter]) -> None:
        """注册新的导出器类型（开闭原则：对扩展开放）。"""
        cls._registry[name] = exporter_cls

    @classmethod
    def create(cls, format_name: str) -> Exporter:
        """根据名称创建导出器。"""
        exporter_cls = cls._registry.get(format_name)
        if exporter_cls is None:
            raise ValueError(f"不支持的导出格式: {format_name}")
        return exporter_cls()


# ============================================================
# 3. 建造者模式（Builder）
# ============================================================
@dataclass
class HTTPRequest:
    """HTTP 请求对象（不可变，通过 Builder 构建）。"""

    method: str
    url: str
    headers: dict[str, str] = field(default_factory=dict)
    body: str | None = None
    timeout: int = 30

    def execute(self) -> str:
        """模拟发送请求。"""
        return (
            f"HTTP {self.method} {self.url}\n"
            f"Headers: {self.headers}\n"
            f"Body: {self.body}\n"
            f"Timeout: {self.timeout}s"
        )


class HTTPRequestBuilder:
    """HTTP 请求建造者 —— 链式调用逐步构建复杂对象。"""

    def __init__(self, method: str, url: str) -> None:
        self._method = method
        self._url = url
        self._headers: dict[str, str] = {}
        self._body: str | None = None
        self._timeout: int = 30

    def add_header(self, key: str, value: str) -> HTTPRequestBuilder:
        self._headers[key] = value
        return self

    def with_body(self, body: str) -> HTTPRequestBuilder:
        self._body = body
        return self

    def with_timeout(self, seconds: int) -> HTTPRequestBuilder:
        self._timeout = seconds
        return self

    def build(self) -> HTTPRequest:
        return HTTPRequest(
            method=self._method,
            url=self._url,
            headers=self._headers,
            body=self._body,
            timeout=self._timeout,
        )


# ============================================================
# 4. 观察者模式（Observer）
# ============================================================
type Listener = Callable[[str, Any], None]


class EventBus:
    """
    事件总线 —— 发布-订阅模式的轻量实现。

    用于解耦模块间通信，UI 框架和微服务中广泛使用。
    """

    def __init__(self) -> None:
        self._listeners: dict[str, list[Listener]] = {}

    def on(self, event: str, callback: Listener) -> None:
        """订阅事件。"""
        self._listeners.setdefault(event, []).append(callback)

    def off(self, event: str, callback: Listener) -> None:
        """取消订阅。"""
        if event in self._listeners:
            self._listeners[event].remove(callback)

    def emit(self, event: str, data: Any = None) -> None:
        """发布事件。"""
        for callback in self._listeners.get(event, []):
            callback(event, data)


# ============================================================
# 演示
# ============================================================
def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 08：常见设计模式")
    print("=" * 60)

    # 1. 单例
    print("--- 单例模式 ---")
    cfg1 = AppConfig()
    cfg1.set("db.host", "localhost")
    cfg2 = AppConfig()
    print(f"cfg1 is cfg2: {cfg1 is cfg2}")  # True
    print(f"cfg2.get('db.host'): {cfg2.get('db.host')}")

    # 2. 工厂
    print("\n--- 工厂模式 ---")
    data = [{"name": "Alice", "age": 30}, {"name": "Bob", "age": 25}]
    for fmt in ("csv", "json"):
        exporter = ExporterFactory.create(fmt)
        print(f"[{fmt.upper()}]\n{exporter.export(data)}\n")

    # 3. 建造者
    print("--- 建造者模式 ---")
    request = (
        HTTPRequestBuilder("POST", "https://api.example.com/users")
        .add_header("Authorization", "Bearer token123")
        .add_header("Content-Type", "application/json")
        .with_body('{"name": "Carol"}')
        .with_timeout(10)
        .build()
    )
    print(request.execute())

    # 4. 观察者
    print("\n--- 观察者模式 ---")
    bus = EventBus()

    def log_handler(event: str, data: Any) -> None:
        print(f"  [Logger] 收到事件 '{event}': {data}")

    def alert_handler(event: str, data: Any) -> None:
        if event == "error":
            print(f"  [Alert] 告警: {data}")

    bus.on("user_login", log_handler)
    bus.on("error", log_handler)
    bus.on("error", alert_handler)

    bus.emit("user_login", {"user": "alice", "time": "10:30"})
    bus.emit("error", "数据库连接超时")


if __name__ == "__main__":
    demo()
