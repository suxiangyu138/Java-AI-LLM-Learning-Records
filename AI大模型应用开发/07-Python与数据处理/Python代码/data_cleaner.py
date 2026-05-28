"""数据清洗管道 — 缺失值处理 / 离群值检测 / 类型转换 / 去重。"""

from __future__ import annotations

import logging
from typing import Callable, Optional

import numpy as np
import pandas as pd
from scipy import stats

from data_analysis.config import AnalysisConfig


class DataCleaner:
    """声明式数据清洗管道。

    Usage:
        cleaner = DataCleaner(cfg)
        df_clean = cleaner.pipe(df, [
            cleaner.drop_high_missing_columns,
            cleaner.impute_missing,
            cleaner.remove_outliers_zscore,
        ])
    """

    def __init__(self, config: AnalysisConfig) -> None:
        self.cfg = config
        self._log = logging.getLogger(self.__class__.__name__)
        self._report: dict[str, object] = {}

    # ------------------------------------------------------------------
    # 管道执行器
    # ------------------------------------------------------------------

    def pipe(self, df: pd.DataFrame, steps: list[Callable[[pd.DataFrame], pd.DataFrame]]) -> pd.DataFrame:
        """顺序执行清洗步骤，记录每步耗时和 shape 变化。"""
        result = df.copy()
        for step in steps:
            before = result.shape
            result = step(result)
            after = result.shape
            self._report[step.__name__] = {"before": before, "after": after}
            self._log.info("%s: %s → %s", step.__name__, before, after)
        return result

    @property
    def report(self) -> dict[str, object]:
        return self._report

    # ------------------------------------------------------------------
    # 清洗步骤（每个步骤是一等函数，可自由组合）
    # ------------------------------------------------------------------

    def drop_high_missing_columns(self, df: pd.DataFrame) -> pd.DataFrame:
        """删除缺失率超过阈值的列。"""
        threshold = self.cfg.missing_threshold_col
        to_drop = df.columns[df.isnull().mean() > threshold]
        if len(to_drop):
            self._log.info("删除高缺失列: %s", list(to_drop))
        return df.drop(columns=to_drop)

    def drop_high_missing_rows(self, df: pd.DataFrame) -> pd.DataFrame:
        """删除缺失率超过阈值的行。"""
        threshold = self.cfg.missing_threshold_row
        mask = df.isnull().mean(axis=1) <= threshold
        return df.loc[mask].copy()

    def impute_missing(self, df: pd.DataFrame) -> pd.DataFrame:
        """按列类型智能填充缺失值。"""
        df = df.copy()
        strategy = self.cfg.impute_strategy
        for col in df.columns:
            if df[col].isnull().sum() == 0:
                continue
            if pd.api.types.is_numeric_dtype(df[col]):
                fill = self._numeric_fill(df[col], strategy)
            else:
                fill = df[col].mode().iloc[0] if len(df[col].mode()) > 0 else "missing"
            df[col] = df[col].fillna(fill)
        return df

    def remove_outliers_zscore(self, df: pd.DataFrame) -> pd.DataFrame:
        """基于 Z-score 移除离群行（仅数值列）。"""
        numeric = df.select_dtypes(include=[np.number])
        z = np.abs(stats.zscore(numeric, nan_policy="omit"))
        mask = (z < self.cfg.outlier_std_threshold).all(axis=1)
        removed = (~mask).sum()
        if removed:
            self._log.info("移除离群行: %d 行", removed)
        return df.loc[mask].copy()

    def remove_outliers_iqr(self, df: pd.DataFrame, multiplier: float = 1.5) -> pd.DataFrame:
        """基于 IQR 移除离群行（更稳健）。"""
        numeric = df.select_dtypes(include=[np.number])
        mask = pd.Series(True, index=df.index)
        for col in numeric.columns:
            q1, q3 = df[col].quantile(0.25), df[col].quantile(0.75)
            iqr = q3 - q1
            mask &= (df[col] >= q1 - multiplier * iqr) & (df[col] <= q3 + multiplier * iqr)
        removed = (~mask).sum()
        if removed:
            self._log.info("IQR 移除离群行: %d 行", removed)
        return df.loc[mask].copy()

    def drop_duplicates(self, df: pd.DataFrame, subset: Optional[list[str]] = None) -> pd.DataFrame:
        """移除重复行。"""
        before = len(df)
        result = df.drop_duplicates(subset=subset)
        dupes = before - len(result)
        if dupes:
            self._log.info("移除重复: %d 行", dupes)
        return result

    def to_numeric_safe(self, df: pd.DataFrame, columns: list[str]) -> pd.DataFrame:
        """安全转换为数值类型，非法值变为 NaN。"""
        df = df.copy()
        for col in columns:
            if col in df.columns:
                df[col] = pd.to_numeric(df[col], errors="coerce")
        return df

    @staticmethod
    def _numeric_fill(series: pd.Series, strategy: str) -> float:
        if strategy == "median":
            return float(series.median())
        if strategy == "mean":
            return float(series.mean())
        if strategy == "mode":
            return float(series.mode().iloc[0]) if len(series.mode()) > 0 else 0.0
        return 0.0
