"""特征工程 — 编码 / 缩放 / 特征选择 / 交互特征 / 分箱，面向 ML 管道。"""

from __future__ import annotations

import logging
from typing import Optional

import numpy as np
import pandas as pd
from sklearn.preprocessing import LabelEncoder, MinMaxScaler, OneHotEncoder, StandardScaler

from data_analysis.config import AnalysisConfig


class FeatureEngineer:
    """ML-ready 特征工程。

    Usage:
        fe = FeatureEngineer(cfg)
        df = fe.onehot_encode(df, ["category_col"])
        df = fe.scale(df, ["age", "income"], method="standard")
        selected = fe.select_by_correlation(df, threshold=0.9)
    """

    def __init__(self, config: AnalysisConfig) -> None:
        self.cfg = config
        self._log = logging.getLogger(self.__class__.__name__)
        self._encoders: dict[str, object] = {}

    # ------------------------------------------------------------------
    # 编码
    # ------------------------------------------------------------------

    def label_encode(self, df: pd.DataFrame, columns: list[str]) -> pd.DataFrame:
        """标签编码（适用于有序类别或二分类目标）。"""
        df = df.copy()
        for col in columns:
            if col not in df.columns:
                continue
            le = LabelEncoder()
            df[col] = le.fit_transform(df[col].astype(str))
            self._encoders[col] = le
        self._log.info("Label 编码完成: %s", columns)
        return df

    def onehot_encode(
        self, df: pd.DataFrame, columns: list[str], drop_first: bool = True, max_categories: int = 20
    ) -> pd.DataFrame:
        """独热编码（自动跳过超过 max_categories 的高基数列）。"""
        df = df.copy()
        valid_cols = []
        for col in columns:
            if col not in df.columns:
                continue
            if df[col].nunique() > max_categories:
                self._log.warning("跳过高基数列 %s (nunique=%d)", col, df[col].nunique())
                continue
            valid_cols.append(col)
        if not valid_cols:
            return df
        ohe = OneHotEncoder(sparse_output=False, drop="first" if drop_first else None)
        encoded = ohe.fit_transform(df[valid_cols].astype(str))
        feat_names = ohe.get_feature_names_out(valid_cols)
        encoded_df = pd.DataFrame(encoded, columns=feat_names, index=df.index)
        result = pd.concat([df.drop(columns=valid_cols), encoded_df], axis=1)
        self._encoders["_onehot"] = ohe
        self._log.info("OneHot 编码完成: %s → %d 新特征", valid_cols, len(feat_names))
        return result

    # ------------------------------------------------------------------
    # 缩放
    # ------------------------------------------------------------------

    def scale(
        self, df: pd.DataFrame, columns: list[str], method: str = "standard"
    ) -> pd.DataFrame:
        """标准化 / 归一化。"""
        df = df.copy()
        scaler = StandardScaler() if method == "standard" else MinMaxScaler()
        existing = [c for c in columns if c in df.columns]
        if not existing:
            return df
        df[existing] = scaler.fit_transform(df[existing])
        self._encoders[f"_scaler_{method}"] = scaler
        self._log.info("缩放完成: %s (%s)", existing, method)
        return df

    # ------------------------------------------------------------------
    # 特征选择
    # ------------------------------------------------------------------

    def select_by_correlation(self, df: pd.DataFrame, threshold: float | None = None) -> pd.DataFrame:
        """移除高相关特征（保留每对中的第一个）。"""
        threshold = threshold or self.cfg.correlation_threshold
        numeric = df.select_dtypes(include=[np.number])
        corr = numeric.corr().abs()
        upper = corr.where(np.triu(np.ones(corr.shape), k=1).astype(bool))
        to_drop = [c for c in upper.columns if any(upper[c] > threshold)]
        if to_drop:
            self._log.info("高相关移除: %s", to_drop)
        return df.drop(columns=to_drop)

    def select_by_variance(self, df: pd.DataFrame, threshold: float | None = None) -> pd.DataFrame:
        """移除低方差特征。"""
        threshold = threshold or self.cfg.variance_threshold
        numeric = df.select_dtypes(include=[np.number])
        variances = numeric.var()
        low_var = variances[variances < threshold].index.tolist()
        if low_var:
            self._log.info("低方差移除: %s", low_var)
        return df.drop(columns=low_var)

    # ------------------------------------------------------------------
    # 特征构造
    # ------------------------------------------------------------------

    def add_polynomial_features(
        self, df: pd.DataFrame, columns: list[str], degree: int = 2
    ) -> pd.DataFrame:
        """添加多项式交互特征（仅平方项，避免维度爆炸）。"""
        df = df.copy()
        for col in columns:
            if col not in df.columns:
                continue
            for d in range(2, degree + 1):
                df[f"{col}_pow{d}"] = df[col] ** d
        self._log.info("多项式特征: %d 列, degree=%d", len(columns), degree)
        return df

    def add_interaction_features(
        self, df: pd.DataFrame, pairs: list[tuple[str, str]]
    ) -> pd.DataFrame:
        """添加两两列交互特征（乘积 + 比值）。"""
        df = df.copy()
        for a, b in pairs:
            if a in df.columns and b in df.columns:
                df[f"{a}_mul_{b}"] = df[a] * df[b]
                # 安全比值：避免除零
                denom = df[b].replace(0, np.nan)
                df[f"{a}_div_{b}"] = df[a] / denom
        self._log.info("交互特征: %d 对", len(pairs))
        return df

    def bin_numeric(
        self, df: pd.DataFrame, column: str, bins: int = 5, labels: Optional[list[str]] = None
    ) -> pd.DataFrame:
        """等频分箱（qcut），生成类别标签。"""
        df = df.copy()
        df[f"{column}_binned"] = pd.qcut(df[column], q=bins, labels=labels, duplicates="drop")
        self._log.info("分箱: %s → %d bins", column, bins)
        return df

    def add_aggregation_features(
        self, df: pd.DataFrame, group_col: str, agg_col: str, funcs: list[str] | None = None
    ) -> pd.DataFrame:
        """添加组聚合特征（如：按城市计算人均收入）。"""
        funcs = funcs or ["mean", "std", "max", "min"]
        grouped = df.groupby(group_col)[agg_col].agg(funcs)
        grouped.columns = [f"{group_col}_{agg_col}_{f}" for f in funcs]
        result = df.join(grouped, on=group_col)
        self._log.info("聚合特征: group=%s, agg=%s", group_col, agg_col)
        return result
