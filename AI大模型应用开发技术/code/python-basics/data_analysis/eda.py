"""探索性数据分析 (EDA) — 描述性统计 / 分布分析 / 相关性矩阵 / 分组对比。"""

from __future__ import annotations

import logging
from typing import Any

import numpy as np
import pandas as pd

from data_analysis.config import AnalysisConfig


class EDA:
    """快速 EDA 报告生成器。

    Usage:
        eda = EDA(cfg)
        summary = eda.describe(df)
        corr = eda.correlation_matrix(df, target="price")
    """

    def __init__(self, config: AnalysisConfig) -> None:
        self.cfg = config
        self._log = logging.getLogger(self.__class__.__name__)

    # ------------------------------------------------------------------
    # 描述性统计
    # ------------------------------------------------------------------

    def describe(self, df: pd.DataFrame) -> pd.DataFrame:
        """增强版 describe：增加偏度、峰度、缺失率、唯一值数。"""
        base = df.describe(include="all").T
        extras = pd.DataFrame({
            "missing": df.isnull().sum(),
            "missing_pct": (df.isnull().sum() / len(df) * 100).round(2),
            "unique": df.nunique(),
            "dtype": df.dtypes.astype(str),
        })
        result = pd.concat([extras, base], axis=1)
        numeric = df.select_dtypes(include=[np.number])
        if not numeric.empty:
            skew = numeric.skew().to_dict()
            kurt = numeric.kurtosis().to_dict()
            result["skew"] = result.index.map(skew)
            result["kurtosis"] = result.index.map(kurt)
        self._log.info("EDA describe: %d 列", len(result))
        return result

    def correlation_matrix(
        self, df: pd.DataFrame, method: str = "pearson", target: str | None = None
    ) -> pd.DataFrame:
        """计算数值列相关矩阵，若指定 target 则按相关性降序排列。"""
        numeric = df.select_dtypes(include=[np.number])
        corr = numeric.corr(method=method, numeric_only=True)
        if target is not None and target in corr.columns:
            corr = corr.sort_values(target, ascending=False)
        self._log.info("相关性矩阵: %d×%d, method=%s", *corr.shape, method)
        return corr

    # ------------------------------------------------------------------
    # 分组分析
    # ------------------------------------------------------------------

    def group_stats(
        self, df: pd.DataFrame, group_col: str, agg_cols: list[str] | None = None
    ) -> pd.DataFrame:
        """按分组列计算 mean / std / count / median。"""
        targets = agg_cols or df.select_dtypes(include=[np.number]).columns.tolist()
        result = df.groupby(group_col)[targets].agg(["mean", "std", "count", "median"])
        self._log.info("分组统计: group=%s, targets=%d", group_col, len(targets))
        return result

    def value_distribution(self, df: pd.DataFrame, col: str, normalize: bool = True) -> pd.DataFrame:
        """单列值分布（频率表）。"""
        dist = df[col].value_counts(normalize=normalize, dropna=False).reset_index()
        dist.columns = [col, "proportion" if normalize else "count"]
        return dist

    # ------------------------------------------------------------------
    # 数据质量报告
    # ------------------------------------------------------------------

    def quality_report(self, df: pd.DataFrame) -> dict[str, Any]:
        """一键数据质量报告：缺失率、重复行、常量列、低方差列。"""
        report: dict[str, Any] = {
            "shape": df.shape,
            "duplicated_rows": int(df.duplicated().sum()),
            "total_missing": int(df.isnull().sum().sum()),
            "missing_pct": round(df.isnull().sum().sum() / (df.shape[0] * df.shape[1]) * 100, 2),
        }
        # 常量列
        constant = [c for c in df.columns if df[c].nunique(dropna=False) <= 1]
        report["constant_columns"] = constant
        # 低方差数值列
        numeric = df.select_dtypes(include=[np.number])
        low_var = [
            c for c in numeric.columns
            if numeric[c].std(ddof=0) < self.cfg.variance_threshold
        ]
        report["low_variance_columns"] = low_var
        # 高缺失列
        high_miss = df.columns[df.isnull().mean() > self.cfg.missing_threshold_col].tolist()
        report["high_missing_columns"] = high_miss
        self._log.info("质量报告完成: 问题列 %d 个", len(constant) + len(low_var) + len(high_miss))
        return report

    def compare_groups(
        self, df: pd.DataFrame, group_col: str, value_col: str
    ) -> pd.DataFrame:
        """两组/多组对比分析：均值差异、Cohen's d、ANOVA F。"""
        groups = df.groupby(group_col)[value_col]
        agg = groups.agg(["mean", "std", "count"]).reset_index()
        # ANOVA
        group_list = [g.dropna().values for _, g in groups]
        if len(group_list) >= 2:
            from scipy.stats import f_oneway
            f_stat, p_val = f_oneway(*group_list)
            agg["anova_f"] = f_stat
            agg["anova_p"] = p_val
        self._log.info("组间对比: %s by %s, groups=%d", value_col, group_col, len(group_list))
        return agg
