"""
单元 12：日志、配置与环境管理

覆盖：logging（层级/Formatter/Handler）、pydantic-settings、
python-dotenv、dataclass 配置模式.
"""

from __future__ import annotations

import json
import logging
import os
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

# ============================================================
# 1. logging 标准库（企业级日志）
# ============================================================

def setup_logger(name: str | None = None) -> logging.Logger:
    """配置标准 logger：同时输出到控制台和文件.

    企业实践：
    - 不直接使用 root logger
    - 每个模块用 __name__ 创建 logger
    - 统一格式：时间 | 级别 | 模块 | 消息
    """
    logger = logging.getLogger(name or __name__)
    logger.setLevel(logging.DEBUG)

    # 避免重复添加 handler（函数可能被多次调用）
    if logger.handlers:
        return logger

    fmt = logging.Formatter(
        "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    # 控制台 handler
    console = logging.StreamHandler(sys.stdout)
    console.setLevel(logging.INFO)
    console.setFormatter(fmt)
    logger.addHandler(console)

    # 文件 handler（生产环境推荐 json 格式，方便 ELK 采集）
    log_dir = Path(__file__).parent / "logs"
    log_dir.mkdir(exist_ok=True)
    file_handler = logging.FileHandler(
        log_dir / "app.log", encoding="utf-8"
    )
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(fmt)
    logger.addHandler(file_handler)

    return logger


_log = setup_logger("unit_12")


def demo_logging() -> None:
    """日志级别演示."""
    _log.debug("这是 DEBUG 信息（仅文件可见）")
    _log.info("这是 INFO 信息")
    _log.warning("这是 WARNING 信息")
    _log.error("这是 ERROR 信息")
    try:
        1 / 0
    except ZeroDivisionError:
        _log.exception("捕获异常并自动附加 traceback")  # 生产必备


# ============================================================
# 2. 结构化日志（JSON 格式）
# ============================================================

class JsonFormatter(logging.Formatter):
    """JSON 格式 log formatter — 适合日志采集系统（ELK、Splunk）."""

    def format(self, record: logging.LogRecord) -> str:
        log_entry: dict[str, Any] = {
            "timestamp": self.formatTime(record, self.datefmt),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "module": record.module,
            "line": record.lineno,
        }
        if record.exc_info and record.exc_info[1]:
            log_entry["exception"] = str(record.exc_info[1])
        return json.dumps(log_entry, ensure_ascii=False)


def demo_json_logging() -> None:
    """JSON 日志演示."""
    logger = logging.getLogger("json_test")
    logger.setLevel(logging.DEBUG)
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(JsonFormatter())
    logger.handlers.clear()
    logger.addHandler(handler)

    logger.info("用户登录成功", extra={"user_id": 42})
    try:
        raise RuntimeError("Something went wrong")
    except RuntimeError:
        logger.exception("捕获到运行时错误")


# ============================================================
# 3. 应用配置模式（dataclass + 环境变量）
# ============================================================

@dataclass
class AppConfig:
    """应用配置：集中管理所有配置项.

    优先级：环境变量 > dataclass 默认值
    """
    # 服务
    host: str = field(
        default_factory=lambda: os.getenv("APP_HOST", "0.0.0.0")
    )
    port: int = field(
        default_factory=lambda: int(os.getenv("APP_PORT", "8000"))
    )
    debug: bool = field(
        default_factory=lambda: os.getenv("APP_DEBUG", "false").lower() == "true"
    )

    # 数据库
    db_url: str = field(
        default_factory=lambda: os.getenv(
            "DATABASE_URL",
            "postgresql://localhost:5432/mydb",
        )
    )
    db_pool_size: int = field(
        default_factory=lambda: int(os.getenv("DB_POOL_SIZE", "10"))
    )

    # Redis
    redis_url: str = field(
        default_factory=lambda: os.getenv("REDIS_URL", "redis://localhost:6379/0")
    )

    # LLM
    llm_model: str = field(
        default_factory=lambda: os.getenv("LLM_MODEL", "claude-sonnet-4-6")
    )
    llm_max_tokens: int = field(
        default_factory=lambda: int(os.getenv("LLM_MAX_TOKENS", "4096"))
    )

    def to_dict(self) -> dict[str, object]:
        """导出为字典（日志/调试用，注意脱敏）."""
        return {
            "host": self.host,
            "port": self.port,
            "debug": self.debug,
            "db_url": self._mask_password(self.db_url),
            "db_pool_size": self.db_pool_size,
            "redis_url": self.redis_url,
            "llm_model": self.llm_model,
            "llm_max_tokens": self.llm_max_tokens,
        }

    @staticmethod
    def _mask_password(url: str) -> str:
        """简单脱敏：隐藏 URL 中的密码."""
        import re
        return re.sub(r"://([^:]+):([^@]+)@", r"://\1:***@", url)


# ============================================================
# 4. .env 文件加载（仅开发环境）
# ============================================================

def load_dotenv_simple(env_file: Path | None = None) -> None:
    """手动加载 .env 文件（无第三方依赖）.

    第三方库 python-dotenv 功能更全，此为基础实现.
    """
    if env_file is None:
        env_file = Path(__file__).parent / ".env"
    if not env_file.exists():
        return

    for line in env_file.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        key, _, value = line.partition("=")
        key, value = key.strip(), value.strip().strip("\"'")
        if key not in os.environ:  # 环境变量优先级更高
            os.environ[key] = value


# ============================================================
# 入口
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("单元 12：日志、配置与环境管理")
    print("=" * 50)

    # 加载 .env（如果存在）
    load_dotenv_simple()

    print("\n1. 标准日志:")
    demo_logging()

    print("\n2. JSON 结构化日志:")
    demo_json_logging()

    print("\n3. 应用配置:")
    config = AppConfig()
    print(f"   {json.dumps(config.to_dict(), indent=2, ensure_ascii=False)}")
    print(f"\n   可设置环境变量覆盖默认值，例如：")
    print(f"   set APP_PORT=9000 APP_DEBUG=true")
    print(f"   或在 .env 文件中写入 APP_PORT=9000")
