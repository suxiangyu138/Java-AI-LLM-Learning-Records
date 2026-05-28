"""统计分析 — t 检验 / ANOVA / 置信区间 / 效应量 / 分布拟合，面向模型评估。"""

from __future__ import annotations

import logging
from typing import Any

import numpy as np
import pandas as pd
from scipy import stats

from data_analysis.config import AnalysisConfig


class Statistics:
    """统计工具箱 — 用于数据理解和模型评估。"""

    def __init__(self, config: AnalysisConfig) -> None:
        self.cfg = config
        self._log = logging.getLogger(self.__class__.__name__)

    # ------------------------------------------------------------------
    # 基础推断
    # ------------------------------------------------------------------

    def confidence_interval(
        self, data: np.ndarray | pd.Series, confidence: float = 0.95
    ) -> dict[str, float]:
        """计算均值的置信区间。"""
        arr = np.asarray(data, dtype=float)
        arr = arr[~np.isnan(arr)]
        n = len(arr)
        mean = float(np.mean(arr))
        sem = float(stats.sem(arr))
        ci = stats.t.interval(confidence, df=n - 1, loc=mean, scale=sem)
        return {"mean": mean, "ci_lower": ci[0], "ci_upper": ci[1], "n": n, "confidence": confidence}

    def t_test_independent(
        self, group_a: np.ndarray, group_b: np.ndarray
    ) -> dict[str, float]:
        """独立样本 t 检验（比较两组均值差异是否显著）。"""
        a = np.asarray(group_a, dtype=float)
        b = np.asarray(group_b, dtype=float)
        t_stat, p_val = stats.ttest_ind(a[~np.isnan(a)], b[~np.isnan(b)])
        effect = self._cohens_d(a, b)
        return {"t_statistic": t_stat, "p_value": p_val, "cohens_d": effect, "significant": p_val < 0.05}

    def anova(self, *groups: np.ndarray) -> dict[str, float]:
        """单因素 ANOVA。"""
        cleaned = [np.asarray(g, dtype=float) for g in groups]
        cleaned = [g[~np.isnan(g)] for g in cleaned if len(g) > 0]
        f_stat, p_val = stats.f_oneway(*cleaned)
        return {"f_statistic": f_stat, "p_value": p_val, "groups": len(cleaned)}

    def chi2_test(self, observed: np.ndarray) -> dict[str, float]:
        """卡方检验（适合类别变量的独立性检验）。"""
        chi2, p, dof, expected = stats.chi2_contingency(observed)
        return {"chi2": chi2, "p_value": p, "dof": dof}

    # ------------------------------------------------------------------
    # 分布分析
    # ------------------------------------------------------------------

    def normality_test(self, data: np.ndarray, alpha: float = 0.05) -> dict[str, Any]:
        """Shapiro-Wilk 正态性检验。"""
        arr = np.asarray(data, dtype=float)
        arr = arr[~np.isnan(arr)]
        if len(arr) > 5000:
            arr = np.random.RandomState(self.cfg.random_seed).choice(arr, size=5000, replace=False)
        stat, p = stats.shapiro(arr)
        return {"statistic": stat, "p_value": p, "normal": p > alpha, "sample_size": len(arr)}

    def distribution_fit(self, data: np.ndarray) -> pd.DataFrame:
        """比较数据对常见分布的拟合优度。"""
        arr = np.asarray(data, dtype=float)
        arr = arr[~np.isnan(arr)]
        results = []
        for name in ["norm", "lognorm", "expon", "gamma"]:
            dist = getattr(stats, name)
            params = dist.fit(arr)
            _, p = stats.kstest(arr, name, params)
            results.append({"distribution": name, "p_value": p, "params": str(params)})
        return pd.DataFrame(results)

    def correlation_significance(
        self, df: pd.DataFrame, method: str = "pearson"
    ) -> pd.DataFrame:
        """计算相关性矩阵的 p 值矩阵。"""
        numeric = df.select_dtypes(include=[np.number])
        cols = numeric.columns
        n = len(cols)
        r_matrix = numeric.corr(method=method)
        p_matrix = pd.DataFrame(np.eye(n), index=cols, columns=cols)
        for i in range(n):
            for j in range(i + 1, n):
                a = numeric.iloc[:, i].dropna()
                b = numeric.iloc[:, j].dropna()
                common_idx = a.index.intersection(b.index)
                if len(common_idx) < 3:
                    continue
                if method == "pearson":
                    r, p = stats.pearsonr(a[common_idx], b[common_idx])
                else:
                    r, p = stats.spearmanr(a[common_idx], b[common_idx])
                p_matrix.iloc[i, j] = p_matrix.iloc[j, i] = p
        return p_matrix

    def summary_stats(self, df: pd.DataFrame) -> pd.DataFrame:
        """数值列综合统计：均值/中位数/标准差/CV/IQR/偏度/峰度/缺失率。"""
        numeric = df.select_dtypes(include=[np.number])
        rows = []
        for col in numeric.columns:
            s = numeric[col].dropna()
            rows.append({
                "column": col,
                "count": len(s),
                "missing_pct": round(100 - len(s) / len(df) * 100, 2),
                "mean": s.mean(),
                "median": s.median(),
                "std": s.std(),
                "cv": s.std() / s.mean() if s.mean() != 0 else np.nan,
                "iqr": s.quantile(0.75) - s.quantile(0.25),
                "skew": s.skew(),
                "kurtosis": s.kurtosis(),
                "min": s.min(),
                "p25": s.quantile(0.25),
                "p50": s.quantile(0.50),
                "p75": s.quantile(0.75),
                "max": s.max(),
            })
        return pd.DataFrame(rows).set_index("column")

    @staticmethod
    def _cohens_d(a: np.ndarray, b: np.ndarray) -> float:
        a, b = np.asarray(a), np.asarray(b)
        a, b = a[~np.isnan(a)], b[~np.isnan(b)]
        n1, n2 = len(a), len(b)
        if n1 < 2 or n2 < 2:
            return float("nan")
        pooled_std = np.sqrt(((n1 - 1) * a.var(ddof=1) + (n2 - 1) * b.var(ddof=1)) / (n1 + n2 - 2))
        return float(abs(a.mean() - b.mean()) / pooled_std) if pooled_std > 0 else 0.0
