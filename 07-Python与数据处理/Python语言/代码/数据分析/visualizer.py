"""可视化 — 统一绘图接口，面向数据探索和报告生成。"""

from __future__ import annotations

import logging
from pathlib import Path
from typing import Optional

import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

matplotlib.use("Agg")  # 无头模式，不弹窗，文件输出

from data_analysis.config import AnalysisConfig


class Visualizer:
    """数据可视化工具箱。

    Usage:
        viz = Visualizer(cfg)
        viz.correlation_heatmap(df, save_as="corr.png")
        viz.distribution(df, "age")
    """

    def __init__(self, config: AnalysisConfig) -> None:
        self.cfg = config
        self._log = logging.getLogger(self.__class__.__name__)
        plt.rcParams.update({
            "figure.dpi": config.figure_dpi,
            "savefig.dpi": config.figure_dpi,
            "axes.titlesize": 14,
            "axes.labelsize": 12,
        })

    # ------------------------------------------------------------------
    # 单变量
    # ------------------------------------------------------------------

    def distribution(
        self,
        data: pd.Series | np.ndarray,
        title: str = "Distribution",
        bins: int = 50,
        save_as: str | None = None,
    ) -> plt.Figure:
        """直方图 + KDE 密度曲线。"""
        fig, ax = plt.subplots(figsize=self.cfg.figure_figsize)
        arr = np.asarray(data, dtype=float)
        arr = arr[~np.isnan(arr)]
        ax.hist(arr, bins=bins, density=True, alpha=0.6, color="#3b82f6", edgecolor="white")
        # KDE
        from scipy.stats import gaussian_kde
        kde = gaussian_kde(arr)
        xs = np.linspace(arr.min(), arr.max(), 300)
        ax.plot(xs, kde(xs), color="#ef4444", linewidth=2, label="KDE")
        ax.set_title(title)
        ax.legend()
        if save_as or self.cfg.save_figures:
            self._save(fig, save_as or f"{title}.png")
        return fig

    def boxplot(
        self,
        df: pd.DataFrame,
        column: str,
        group_col: str | None = None,
        title: str | None = None,
        save_as: str | None = None,
    ) -> plt.Figure:
        """箱线图，可选分组。"""
        fig, ax = plt.subplots(figsize=self.cfg.figure_figsize)
        if group_col and group_col in df.columns:
            df.boxplot(column=column, by=group_col, ax=ax, grid=False)
            ax.set_title(title or f"{column} by {group_col}")
            fig.suptitle("")
        else:
            ax.boxplot(df[column].dropna(), vert=True)
            ax.set_xticklabels([column])
            ax.set_title(title or f"Boxplot of {column}")
        if save_as or self.cfg.save_figures:
            self._save(fig, save_as or f"boxplot_{column}.png")
        return fig

    # ------------------------------------------------------------------
    # 多变量
    # ------------------------------------------------------------------

    def correlation_heatmap(
        self,
        df: pd.DataFrame,
        method: str = "pearson",
        title: str = "Correlation Heatmap",
        save_as: str | None = None,
    ) -> plt.Figure:
        """相关性热力图 — 常用于特征选择前的快速扫描。"""
        numeric = df.select_dtypes(include=[np.number])
        corr = numeric.corr(method=method)
        mask = np.triu(np.ones_like(corr, dtype=bool), k=1)
        fig, ax = plt.subplots(figsize=(10, 8))
        cmap = matplotlib.colormaps.get_cmap(self.cfg.palette)
        im = ax.matshow(corr, cmap=cmap, vmin=-1, vmax=1)
        # 标注数值
        for i in range(len(corr)):
            for j in range(len(corr)):
                ax.text(j, i, f"{corr.iloc[i, j]:.2f}", ha="center", va="center",
                        fontsize=8, color="white" if abs(corr.iloc[i, j]) > 0.5 else "black")
        ax.set_xticks(range(len(corr.columns)))
        ax.set_yticks(range(len(corr.columns)))
        ax.set_xticklabels(corr.columns, rotation=45, ha="left")
        ax.set_yticklabels(corr.columns)
        ax.set_title(title)
        fig.colorbar(im, ax=ax, shrink=0.8)
        fig.tight_layout()
        if save_as or self.cfg.save_figures:
            self._save(fig, save_as or "correlation_heatmap.png")
        return fig

    def scatter_matrix(
        self, df: pd.DataFrame, columns: list[str], save_as: str | None = None
    ) -> plt.Figure:
        """散点矩阵 — 快速发现变量间关系。"""
        n = len(columns)
        fig, axes = plt.subplots(n, n, figsize=(12, 10))
        for i, col_i in enumerate(columns):
            for j, col_j in enumerate(columns):
                ax = axes[i][j] if n > 1 else axes
                if i == j:
                    ax.hist(df[col_i].dropna(), bins=30, color="#3b82f6", alpha=0.7)
                else:
                    ax.scatter(df[col_j], df[col_i], alpha=0.3, s=5, color="#3b82f6")
                if i == n - 1:
                    ax.set_xlabel(col_j[:12], fontsize=8)
                if j == 0:
                    ax.set_ylabel(col_i[:12], fontsize=8)
        fig.suptitle("Scatter Matrix", fontsize=14)
        fig.tight_layout()
        if save_as or self.cfg.save_figures:
            self._save(fig, save_as or "scatter_matrix.png")
        return fig

    def feature_importance_bar(
        self,
        importances: dict[str, float] | pd.Series,
        top_n: int = 15,
        title: str = "Feature Importance",
        save_as: str | None = None,
    ) -> plt.Figure:
        """特征重要性条形图（ML 模型解释）。"""
        if isinstance(importances, dict):
            importances = pd.Series(importances)
        top = importances.nlargest(top_n)
        fig, ax = plt.subplots(figsize=(10, 6))
        colors = plt.cm.viridis(np.linspace(0, 1, len(top)))
        ax.barh(range(len(top)), top.values, color=colors)
        ax.set_yticks(range(len(top)))
        ax.set_yticklabels(top.index)
        ax.invert_yaxis()
        ax.set_xlabel("Importance")
        ax.set_title(title)
        fig.tight_layout()
        if save_as or self.cfg.save_figures:
            self._save(fig, save_as or "feature_importance.png")
        return fig

    # ------------------------------------------------------------------
    # 时间趋势
    # ------------------------------------------------------------------

    def time_series(
        self,
        df: pd.DataFrame,
        date_col: str,
        value_col: str,
        title: str = "Time Series",
        save_as: str | None = None,
    ) -> plt.Figure:
        """时间序列折线图。"""
        fig, ax = plt.subplots(figsize=(14, 5))
        df_sorted = df.sort_values(date_col)
        ax.plot(
            df_sorted[date_col], df_sorted[value_col],
            color="#3b82f6", linewidth=1.5, alpha=0.9
        )
        ax.fill_between(
            df_sorted[date_col], df_sorted[value_col], alpha=0.15, color="#3b82f6"
        )
        ax.set_title(title)
        ax.set_xlabel(date_col)
        ax.set_ylabel(value_col)
        fig.autofmt_xdate()
        fig.tight_layout()
        if save_as or self.cfg.save_figures:
            self._save(fig, save_as or "time_series.png")
        return fig

    # ------------------------------------------------------------------
    # 辅助方法
    # ------------------------------------------------------------------

    def _save(self, fig: plt.Figure, filename: str) -> None:
        out = Path(self.cfg.output_dir) / filename
        fig.savefig(out, bbox_inches="tight", dpi=self.cfg.figure_dpi)
        self._log.info("图表已保存: %s", out)
        plt.close(fig)

    def close_all(self) -> None:
        plt.close("all")
