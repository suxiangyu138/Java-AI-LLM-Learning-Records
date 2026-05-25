"""数据加载器 — 统一入口加载 CSV / sklearn 数据集 / 自定义数据，输出标准化 DataFrame。"""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Optional

import numpy as np
import pandas as pd
from sklearn.datasets import (
    fetch_california_housing,
    load_breast_cancer,
    load_diabetes,
    load_iris,
    make_classification,
    make_regression,
)

from data_analysis.config import AnalysisConfig


class DataLoader:
    """统一数据加载接口。

    支持来源：
        - 本地 CSV / Excel
        - sklearn 内置数据集（iris, breast_cancer, diabetes, california_housing）
        - 合成数据集（分类 / 回归，用于快速原型验证）
    """

    _SKLEARN_LOADERS = {
        "iris": load_iris,
        "breast_cancer": load_breast_cancer,
        "diabetes": load_diabetes,
        "california_housing": fetch_california_housing,
    }

    def __init__(self, config: AnalysisConfig) -> None:
        self.cfg = config
        self._log = logging.getLogger(self.__class__.__name__)

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def load_csv(self, path: str | Path, **kwargs: object) -> pd.DataFrame:
        """加载 CSV 文件，自动处理常见编码问题。"""
        path = Path(path)
        if not path.exists():
            raise FileNotFoundError(f"文件不存在: {path}")
        try:
            df = pd.read_csv(path, encoding=self.cfg.csv_encoding, **kwargs)  # type: ignore[arg-type]
        except UnicodeDecodeError:
            self._log.warning("UTF-8 解码失败，尝试 gbk")
            df = pd.read_csv(path, encoding="gbk", **kwargs)  # type: ignore[arg-type]
        self._log.info("加载 CSV: %s, shape=%s", path.name, df.shape)
        return df

    def load_sklearn(self, name: str, as_frame: bool = True) -> tuple[pd.DataFrame, pd.Series]:
        """加载 sklearn 内置数据集。

        Args:
            name: iris / breast_cancer / diabetes / california_housing
            as_frame: 是否返回 DataFrame（True 便于后续分析）

        Returns:
            (X, y) — 特征矩阵和标签
        """
        loader = self._SKLEARN_LOADERS.get(name)
        if loader is None:
            raise ValueError(f"不支持的数据集: {name}，可选 {list(self._SKLEARN_LOADERS)}")
        bunch = loader(as_frame=as_frame)
        X = bunch.data if hasattr(bunch, "data") else bunch["data"]
        y = bunch.target if hasattr(bunch, "target") else bunch["target"]
        if isinstance(X, pd.DataFrame):
            X.columns = [str(c).replace(" ", "_").lower() for c in X.columns]
        self._log.info("加载 sklearn 数据集: %s, X=%s", name, X.shape)
        return X, pd.Series(y, name="target")

    def make_classification_data(
        self, n_samples: int = 1000, n_features: int = 10, n_classes: int = 2
    ) -> tuple[pd.DataFrame, pd.Series]:
        """生成合成二分类数据（用于快速测试 ML 管道）。"""
        X, y = make_classification(
            n_samples=n_samples,
            n_features=n_features,
            n_classes=n_classes,
            n_informative=max(3, n_features // 2),
            random_state=self.cfg.random_seed,
        )
        cols = [f"feature_{i}" for i in range(n_features)]
        self._log.info("生成分类数据: samples=%d, features=%d", n_samples, n_features)
        return pd.DataFrame(X, columns=cols), pd.Series(y, name="target")

    def make_regression_data(
        self, n_samples: int = 1000, n_features: int = 8
    ) -> tuple[pd.DataFrame, pd.Series]:
        """生成合成回归数据。"""
        X, y = make_regression(
            n_samples=n_samples,
            n_features=n_features,
            n_informative=max(2, n_features // 2),
            noise=0.1,
            random_state=self.cfg.random_seed,
        )
        cols = [f"feature_{i}" for i in range(n_features)]
        self._log.info("生成回归数据: samples=%d, targets=%d", n_samples, n_features)
        return pd.DataFrame(X, columns=cols), pd.Series(y, name="target")

    def info(self, df: pd.DataFrame) -> str:
        """简要摘要：shape, dtypes, 缺失值, 内存占用。"""
        buf = []
        buf.append(f"Shape: {df.shape}")
        buf.append(f"列: {list(df.columns)}")
        buf.append(f"类型分布:\n{df.dtypes.value_counts().to_string()}")
        missing = df.isnull().sum()
        missing = missing[missing > 0]
        if len(missing) > 0:
            buf.append(f"缺失列 ({len(missing)}):\n{missing.to_string()}")
        else:
            buf.append("缺失值: 0")
        buf.append(f"内存: {df.memory_usage(deep=True).sum() / 1024:.1f} KB")
        return "\n".join(buf)
