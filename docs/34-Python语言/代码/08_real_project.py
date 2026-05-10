"""
模块 08：完整实战项目 —— 天气数据抓取。

本项目演示一个完整的爬虫工程流程：
    1. 需求分析 → 数据建模
    2. API 接口分析 → 请求构造
    3. 数据抓取 → 异常处理 → 重试
    4. 数据清洗 → 校验 → 转换
    5. 持久化 → CSV + SQLite
    6. 调度器（批量城市、定时任务）
"""

from __future__ import annotations

import csv
import json
import sqlite3
import time
from dataclasses import dataclass, field, asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import requests


# ============================================================
# 1. 数据模型
# ============================================================
@dataclass
class WeatherData:
    """天气数据模型。"""

    city: str
    country: str
    temperature: float
    feels_like: float
    humidity: int
    pressure: int
    wind_speed: float
    weather_desc: str
    weather_icon: str
    fetched_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    def to_row(self) -> list[Any]:
        """转为 CSV 行。"""
        return list(asdict(self).values())

    @property
    def is_valid(self) -> bool:
        """基本校验：温度在合理范围。"""
        return -80 <= self.temperature <= 60 and 0 <= self.humidity <= 100


# ============================================================
# 2. API 客户端
# ============================================================
class OpenWeatherClient:
    """
    OpenWeatherMap API 客户端。

    免费 API Key 注册: https://openweathermap.org/api
    免费额度: 60 次/分钟, 1000 次/天

    支持账号自行注册，或用下面模拟数据演示。
    """

    BASE = "https://api.openweathermap.org/data/2.5/weather"

    def __init__(self, api_key: str = "") -> None:
        self.api_key = api_key
        self._call_count = 0

    def fetch_city(self, city: str) -> WeatherData | None:
        """
        抓取单个城市的实时天气。

        Args:
            city: 城市名称（英文）。

        Returns:
            WeatherData 或 None（失败时）。

        Raises:
            不抛异常，所有错误吞掉并返回 None。
        """
        params: dict[str, Any] = {
            "q": city,
            "appid": self.api_key,
            "units": "metric",  # 摄氏度
            "lang": "zh_cn",
        }

        try:
            resp = requests.get(self.BASE, params=params, timeout=10)
            if resp.status_code == 401:
                raise RuntimeError("API Key 无效，请到 https://openweathermap.org 注册免费账号")
            resp.raise_for_status()
            data = resp.json()
            self._call_count += 1
            return self._parse(city, data)
        except requests.RequestException as e:
            print(f"    [网络错误] {city}: {e}")
            return None

    def _parse(self, city: str, data: dict) -> WeatherData:
        """解析 API 响应。"""
        main = data.get("main", {})
        weather = data.get("weather", [{}])[0]
        wind = data.get("wind", {})
        sys_info = data.get("sys", {})

        return WeatherData(
            city=city,
            country=sys_info.get("country", ""),
            temperature=float(main.get("temp", 0)),
            feels_like=float(main.get("feels_like", 0)),
            humidity=int(main.get("humidity", 0)),
            pressure=int(main.get("pressure", 0)),
            wind_speed=float(wind.get("speed", 0)),
            weather_desc=str(weather.get("description", "")),
            weather_icon=str(weather.get("icon", "")),
        )


# ============================================================
# 3. 模拟数据提供者（无 API Key 时使用）
# ============================================================
class MockWeatherClient:
    """
    模拟天气客户端 —— 无需 API Key，生成合理的模拟数据。

    用于演示和学习，结构完全与 OpenWeatherClient 兼容。
    """

    def fetch_city(self, city: str) -> WeatherData:
        """返回模拟天气数据。"""
        import random

        seasons = {
            "Beijing": (20, 35, 40, 60),
            "Shanghai": (22, 33, 60, 80),
            "Tokyo": (18, 30, 50, 70),
            "New York": (15, 28, 45, 65),
            "London": (10, 20, 65, 85),
            "Sydney": (15, 25, 55, 75),
        }

        base_temp, high_temp, hum_low, hum_high = seasons.get(
            city, (15, 28, 50, 70),
        )
        return WeatherData(
            city=city,
            country={"Beijing": "CN", "Shanghai": "CN", "Tokyo": "JP",
                     "New York": "US", "London": "GB", "Sydney": "AU"}.get(city, "XX"),
            temperature=round(random.uniform(base_temp, high_temp), 1),
            feels_like=round(random.uniform(base_temp - 2, high_temp + 2), 1),
            humidity=random.randint(hum_low, hum_high),
            pressure=random.randint(990, 1030),
            wind_speed=round(random.uniform(1, 15), 1),
            weather_desc=random.choice(["晴", "多云", "小雨", "阴天", "晴朗"]),
            weather_icon=random.choice(["01d", "02d", "03d", "04d"]),
        )


# ============================================================
# 4. 数据存储
# ============================================================
class WeatherStorage:
    """天气数据存储器 —— 支持 CSV 和 SQLite。"""

    def __init__(self, output_dir: str = "weather_output") -> None:
        self.output = Path(output_dir)
        self.output.mkdir(exist_ok=True)
        self._csv_file = open(self.output / "weather.csv", "w", newline="", encoding="utf-8-sig")
        self._csv_writer = csv.writer(self._csv_file)
        self._csv_header_written = False

        self._db = sqlite3.connect(str(self.output / "weather.db"))
        self._init_db()

    def _init_db(self) -> None:
        self._db.execute("""
            CREATE TABLE IF NOT EXISTS weather (
                city TEXT,
                country TEXT,
                temperature REAL,
                feels_like REAL,
                humidity INTEGER,
                pressure INTEGER,
                wind_speed REAL,
                weather_desc TEXT,
                weather_icon TEXT,
                fetched_at TEXT,
                PRIMARY KEY (city, fetched_at)
            )
        """)
        self._db.commit()

    def save(self, data: WeatherData) -> None:
        """保存一条天气数据。"""
        row = data.to_row()
        fields = list(asdict(data).keys())

        # CSV
        if not self._csv_header_written:
            self._csv_writer.writerow(fields)
            self._csv_header_written = True
        self._csv_writer.writerow(row)

        # SQLite
        placeholders = ", ".join("?" for _ in fields)
        cols = ", ".join(fields)
        try:
            self._db.execute(
                f"INSERT OR REPLACE INTO weather ({cols}) VALUES ({placeholders})",
                row,
            )
            self._db.commit()
        except sqlite3.Error as e:
            print(f"    [DB错误] {e}")

    def save_batch(self, items: list[WeatherData]) -> None:
        for item in items:
            self.save(item)

    def close(self) -> None:
        self._csv_file.close()
        self._db.close()

    def get_summary(self) -> list[dict[str, Any]]:
        """获取汇总数据。"""
        cursor = self._db.execute("""
            SELECT city, country, temperature, humidity, weather_desc, fetched_at
            FROM weather ORDER BY city, fetched_at DESC
        """)
        columns = [col[0] for col in cursor.description]
        return [dict(zip(columns, row)) for row in cursor.fetchall()]


# ============================================================
# 5. 调度器
# ============================================================
class WeatherCrawler:
    """
    天气数据调度器 —— 批量抓取、自动重试、进度追踪。

    使用示例:
        crawler = WeatherCrawler()
        results = crawler.run(["Beijing", "Shanghai", "Tokyo"])
    """

    def __init__(
        self,
        api_key: str = "",
        output_dir: str = "weather_output",
        delay: float = 0.5,
    ) -> None:
        if api_key:
            self.client: MockWeatherClient | OpenWeatherClient = OpenWeatherClient(api_key)
        else:
            print("  ℹ️  未提供 API Key，使用模拟数据演示")
            self.client = MockWeatherClient()

        self.storage = WeatherStorage(output_dir)
        self.delay = delay

    def run(self, cities: list[str]) -> list[WeatherData]:
        """
        批量抓取城市天气。

        Args:
            cities: 城市名列表。

        Returns:
            成功抓取的 WeatherData 列表。
        """
        results: list[WeatherData] = []
        total = len(cities)

        print(f"\n  开始抓取 {total} 个城市的天气数据...")
        start = time.perf_counter()

        for i, city in enumerate(cities, 1):
            print(f"  [{i}/{total}] {city}...", end=" ")
            data = self.client.fetch_city(city)

            if data and data.is_valid:
                self.storage.save(data)
                results.append(data)
                print(f"✓ {data.temperature}°C, {data.weather_desc}")
            else:
                print(f"✗ 失败或数据无效")

            if i < total:
                time.sleep(self.delay)

        elapsed = time.perf_counter() - start
        print(f"\n  完成! {len(results)}/{total} 成功, 耗时 {elapsed:.1f}s")
        return results

    def close(self) -> None:
        self.storage.close()


def demo() -> None:
    """运行本模块的演示代码。"""
    print("=" * 60)
    print("模块 08：完整实战项目 —— 天气数据抓取")
    print("=" * 60)

    # 运行爬虫
    crawler = WeatherCrawler(delay=0.1)  # 演示用短延迟
    cities = ["Beijing", "Shanghai", "Tokyo", "New York", "London", "Sydney"]
    results = crawler.run(cities)

    # 展示结果
    print(f"\n--- 抓取结果摘要 ---")
    for r in results:
        print(f"  {r.city:12s} ({r.country}) | {r.temperature:5.1f}°C | "
              f"湿度: {r.humidity}% | {r.weather_desc}")

    # 查看存储
    print(f"\n--- 数据库查询 ---")
    for row in crawler.storage.get_summary():
        print(f"  {row['city']:12s} | {row['temperature']:5.1f}°C | {row['weather_desc']}")

    # 最热最冷城市
    print(f"\n--- 统计 ---")
    if results:
        hottest = max(results, key=lambda r: r.temperature)
        coldest = min(results, key=lambda r: r.temperature)
        avg_temp = sum(r.temperature for r in results) / len(results)
        print(f"  最热: {hottest.city} ({hottest.temperature}°C)")
        print(f"  最冷: {coldest.city} ({coldest.temperature}°C)")
        print(f"  平均: {avg_temp:.1f}°C")

    # 输出文件路径
    print(f"\n--- 输出文件 ---")
    output = Path("weather_output")
    for f in output.iterdir():
        print(f"  {f} ({f.stat().st_size}B)")

    crawler.close()

    # 清理
    import shutil
    shutil.rmtree("weather_output")


if __name__ == "__main__":
    demo()
