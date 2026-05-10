"""完整演示管道 — 加载 → 清洗 → EDA → 特征工程 → 统计 → 文本处理 → 可视化。

运行方式:
    python -m data_analysis.run_demo
    # 或
    python run_demo.py
"""

from __future__ import annotations

import sys
from pathlib import Path

# 确保包内模块可导入
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import numpy as np
import pandas as pd

from data_analysis.config import AnalysisConfig
from data_analysis.data_loader import DataLoader
from data_analysis.data_cleaner import DataCleaner
from data_analysis.eda import EDA
from data_analysis.feature_engineering import FeatureEngineer
from data_analysis.statistics import Statistics
from data_analysis.text_processor import TextProcessor
from data_analysis.visualizer import Visualizer


def demo_basic_pipeline(cfg: AnalysisConfig) -> None:
    """演示 1：标准 ML 数据管道 — iris → 清洗 → EDA → 特征工程。"""
    print("\n" + "=" * 60)
    print("演示 1: 标准 ML 数据管道（Iris 数据集）")
    print("=" * 60)

    loader = DataLoader(cfg)
    cleaner = DataCleaner(cfg)
    eda = EDA(cfg)
    fe = FeatureEngineer(cfg)
    stats = Statistics(cfg)

    # 1. 加载
    X, y = loader.load_sklearn("iris")
    df = pd.concat([X, y], axis=1)
    print("\n>>> 数据概览")
    print(loader.info(df))

    # 2. 清洗管道（声明式步骤组合）
    df_clean = cleaner.pipe(df, [
        cleaner.drop_high_missing_columns,
        cleaner.impute_missing,
        cleaner.drop_duplicates,
    ])
    print(f"\n>>> 清洗报告: {cleaner.report}")

    # 3. EDA
    print("\n>>> 描述性统计（前 5 列）")
    desc = eda.describe(df_clean)
    print(desc.head(10).to_string())

    print("\n>>> 数据质量报告")
    quality = eda.quality_report(df_clean)
    for k, v in quality.items():
        print(f"  {k}: {v}")

    print("\n>>> 相关性矩阵（与 target 排序）")
    corr = eda.correlation_matrix(df_clean, target="target")
    print(corr["target"].to_string())

    # 4. 特征工程
    df_fe = fe.scale(df_clean, X.columns.tolist(), method="standard")
    df_fe = fe.add_polynomial_features(df_fe, X.columns.tolist()[:2], degree=2)
    df_fe = fe.add_interaction_features(df_fe, [
        (X.columns[0], X.columns[1]),
        (X.columns[2], X.columns[3]),
    ])
    print(f"\n>>> 特征工程后: {df_fe.shape} (原始 {df.shape})")

    # 5. 统计分析
    print("\n>>> 综合统计")
    summary = stats.summary_stats(df_clean)
    print(summary.to_string())

    print("\n>>> 组间对比（target=0 vs target=1 vs target=2）")
    comparison = eda.compare_groups(df_clean, "target", X.columns[0])
    print(comparison.to_string())


def demo_synthetic_ml(cfg: AnalysisConfig) -> None:
    """演示 2：合成数据 → 特征工程 → 特征选择 — 模拟真实 ML 数据准备。"""
    print("\n" + "=" * 60)
    print("演示 2: 合成数据 ML 特征准备")
    print("=" * 60)

    loader = DataLoader(cfg)
    cleaner = DataCleaner(cfg)
    fe = FeatureEngineer(cfg)
    stats = Statistics(cfg)

    # 合成分类数据
    X, y = loader.make_classification_data(n_samples=500, n_features=8, n_classes=2)
    df = pd.concat([X, y], axis=1)

    # 注入一些缺失值和异常值（模拟真实数据）
    rng = np.random.default_rng(cfg.random_seed)
    mask = rng.random(df.shape) < 0.05
    df[mask] = np.nan
    # 人工注入离群值
    df.loc[0:4, "feature_0"] = 99.0

    print(f"\n>>> 原始数据: shape={df.shape}, 缺失值={df.isnull().sum().sum()}")

    # 清洗
    df_clean = cleaner.pipe(df, [
        cleaner.drop_high_missing_columns,
        cleaner.impute_missing,
        cleaner.remove_outliers_zscore,
        cleaner.drop_duplicates,
    ])

    # 特征工程
    df_fe = fe.scale(df_clean, X.columns.tolist(), method="standard")
    df_fe = fe.add_polynomial_features(df_fe, ["feature_0", "feature_1"], degree=2)
    # 高相关性过滤
    df_selected = fe.select_by_correlation(df_fe, threshold=0.9)
    # 低方差过滤
    df_final = fe.select_by_variance(df_selected)

    print(f"\n>>> 特征选择后: {df_final.shape} (原始 {df_fe.shape})")
    print(f"保留列: {list(df_final.columns)}")

    # 显著性检验
    print("\n>>> 回归系数相关性显著性")
    p_matrix = stats.correlation_significance(df_final)
    print(p_matrix.to_string())


def demo_text_nlp(cfg: AnalysisConfig) -> None:
    """演示 3：NLP 文本处理 → TF-IDF → 词频分析 — 面向 LLM 数据准备。"""
    print("\n" + "=" * 60)
    print("演示 3: NLP 文本处理（面向 LLM 数据准备）")
    print("=" * 60)

    tp = TextProcessor(cfg)

    # 模拟用户评论数据集
    reviews = [
        "The product is amazing and works perfectly well",
        "Terrible quality, broke after one week of use",
        "Good value for the price, but shipping was slow",
        "Excellent customer service and fast delivery",
        "Not worth the money, very disappointed with the quality",
        "Love this product! Highly recommended for everyone",
        "Decent quality but nothing special, average purchase",
        "Amazing experience, will buy again from this seller",
        "Poor packaging, the product arrived damaged",
        "Five stars! Best purchase I have made this year",
        "Shipping was fast but the product quality is terrible",
        "Really good product, exactly as described online",
        "Would not recommend, the quality is getting worse",
        "Pretty good overall, happy with my purchase decision",
        "Excellent quality and great customer support team",
        "The worst product ever, complete waste of money",
        "Solid build quality, works as expected so far",
        "Great value for money, highly recommend this product",
        "Disappointed with the color, looks different from photos",
        "Perfect fit and finish, very satisfied with this item",
    ] * 10  # 200 条数据

    df = pd.DataFrame({"review": reviews, "rating": np.random.default_rng(cfg.random_seed).integers(1, 6, len(reviews))})

    print(f"\n>>> 原始数据: {df.shape[0]} 条评论")

    # 文本清洗
    df = tp.clean_text(df, "review", "review_clean")

    # TF-IDF
    X_tfidf, features = tp.tfidf_vectorize(df["review_clean"], return_features=True)
    print(f"\n>>> TF-IDF 矩阵: {X_tfidf.shape}")

    # 最高 TF-IDF 词
    top_terms = tp.get_top_tfidf_terms(X_tfidf, features, top_n=15)
    print("\n>>> 最高 TF-IDF 词汇")
    print(top_terms.to_string(index=False))

    # 词频
    freq = tp.word_frequency(df["review_clean"], top_n=15)
    print("\n>>> 词频统计 Top 15")
    print(freq.to_string(index=False))

    # 文本统计
    print("\n>>> 文本长度统计")
    print(tp.text_statistics(df["review_clean"]).to_string())


def demo_visualization(cfg: AnalysisConfig) -> None:
    """演示 4：可视化 — 直方图 / 热力图 / 特征重要性。"""
    print("\n" + "=" * 60)
    print("演示 4: 可视化")
    print("=" * 60)

    loader = DataLoader(cfg)

    X, y = loader.load_sklearn("breast_cancer")
    df = pd.concat([X, y], axis=1)
    # 只取前 8 列方便展示
    subset = df.iloc[:, :8].copy()
    subset["target"] = y

    viz = Visualizer(cfg)

    # KDE 分布图
    print(f"\n>>> 生成分布图: {cfg.output_dir}/distribution_mean_radius.png")
    viz.distribution(subset["mean_radius"], title="Mean Radius Distribution", save_as="distribution_mean_radius.png")

    # 相关性热力图
    print(f">>> 生成热力图: {cfg.output_dir}/correlation_breast_cancer.png")
    viz.correlation_heatmap(subset, title="Breast Cancer Correlation", save_as="correlation_breast_cancer.png")

    # 特征重要性（用相关系数模拟）
    corr = subset.corr()["target"].drop("target").abs().sort_values(ascending=False)
    print(f">>> 生成特征重要性图: {cfg.output_dir}/feature_importance.png")
    viz.feature_importance_bar(corr, top_n=10, title="Feature Correlation with Target", save_as="feature_importance.png")

    viz.close_all()
    print("\n>>> 所有图表已生成完毕")


def demo_statistical_inference(cfg: AnalysisConfig) -> None:
    """演示 5：统计推断 — t 检验 / ANOVA / 置信区间 / 分布拟合。"""
    print("\n" + "=" * 60)
    print("演示 5: 统计推断")
    print("=" * 60)

    stats_tool = Statistics(cfg)
    rng = np.random.default_rng(cfg.random_seed)

    # 模拟 A/B 测试数据
    group_a = rng.normal(100, 15, 200)   # 对照组
    group_b = rng.normal(108, 15, 200)   # 实验组（提升了 8 分）

    # t 检验
    t_result = stats_tool.t_test_independent(group_a, group_b)
    print(f"\n>>> A/B t-test: t={t_result['t_statistic']:.3f}, p={t_result['p_value']:.4f}, "
          f"Cohen's d={t_result['cohens_d']:.3f}, significant={t_result['significant']}")

    # 置信区间
    ci = stats_tool.confidence_interval(group_b)
    print(f">>> 实验组 95% CI: [{ci['ci_lower']:.1f}, {ci['ci_upper']:.1f}], mean={ci['mean']:.1f}")

    # 正态性检验
    norm_test = stats_tool.normality_test(group_a)
    print(f">>> 正态性检验: stat={norm_test['statistic']:.4f}, p={norm_test['p_value']:.4f}, "
          f"normal={norm_test['normal']}")

    # 分布拟合
    fit = stats_tool.distribution_fit(group_a)
    print(f"\n>>> 分布拟合优度:\n{fit.to_string(index=False)}")

    # ANOVA（三组对比）
    group_c = rng.normal(95, 15, 200)
    anova = stats_tool.anova(group_a, group_b, group_c)
    print(f"\n>>> ANOVA (三组): F={anova['f_statistic']:.2f}, p={anova['p_value']:.4f}")


def main() -> None:
    cfg = AnalysisConfig(random_seed=42, output_dir="./data_output", log_level=20)
    print(f"配置: seed={cfg.random_seed}, output={cfg.output_dir}")

    try:
        demo_basic_pipeline(cfg)
    except Exception as e:
        print(f"[ERROR] 演示 1 失败: {e}")

    try:
        demo_synthetic_ml(cfg)
    except Exception as e:
        print(f"[ERROR] 演示 2 失败: {e}")

    try:
        demo_text_nlp(cfg)
    except Exception as e:
        print(f"[ERROR] 演示 3 失败: {e}")

    try:
        demo_statistical_inference(cfg)
    except Exception as e:
        print(f"[ERROR] 演示 5 失败: {e}")

    try:
        demo_visualization(cfg)
    except Exception as e:
        print(f"[ERROR] 演示 4 失败: {e}")

    print("\n" + "=" * 60)
    print("全部演示完成！")
    print(f"图表输出目录: {cfg.output_dir}")
    print("=" * 60)


if __name__ == "__main__":
    main()
