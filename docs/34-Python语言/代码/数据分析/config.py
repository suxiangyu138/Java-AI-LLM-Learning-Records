"""全局配置管理 — 集中控制所有分析参数，便于复现和调参。"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


@dataclass
class AnalysisConfig:
    """数据分析全局配置，所有子模块共用同一实例。

    Usage:
        cfg = AnalysisConfig(random_seed=42, output_dir="./output")
        loader = DataLoader(cfg)
    """

    # ---------- 通用 ----------
    random_seed: int = 42
    output_dir: str = "./data_output"
    log_level: int = logging.INFO

    # ---------- 数据加载 ----------
    default_test_size: float = 0.2
    csv_encoding: str = "utf-8"

    # ---------- 数据清洗 ----------
    missing_threshold_col: float = 0.5   # 缺失率 > 此值删除列
    missing_threshold_row: float = 0.3   # 缺失率 > 此值删除行
    outlier_std_threshold: float = 3.0   # Z-score 离群阈值
    impute_strategy: str = "median"      # median / mean / mode / constant

    # ---------- 特征工程 ----------
    correlation_threshold: float = 0.95  # 高相关特征去重阈值
    variance_threshold: float = 0.01     # 低方差特征过滤阈值

    # ---------- 文本处理 ----------
    max_features_tfidf: int = 5000
    ngram_range: tuple[int, int] = (1, 2)

    # ---------- 可视化 ----------
    figure_dpi: int = 120
    figure_figsize: tuple[int, int] = (12, 5)
    palette: str = "viridis"
    save_figures: bool = False

    # 运行时状态（非配置项，模块内部自动维护）
    _metadata: dict[str, Any] = field(default_factory=dict, repr=False)

    def __post_init__(self) -> None:
        Path(self.output_dir).mkdir(parents=True, exist_ok=True)
        logging.basicConfig(
            level=self.log_level,
            format="%(asctime)s | %(levelname)-7s | %(name)s | %(message)s",
            datefmt="%H:%M:%S",
        )

    @property
    def logger(self) -> logging.Logger:
        return logging.getLogger(self.__class__.__name__)
