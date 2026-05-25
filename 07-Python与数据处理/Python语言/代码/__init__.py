"""
数据科学 & AI/LLM 数据分析工具包

企业级 Python 数据分析模块，覆盖 ML 数据全生命周期：
  1. 数据加载      — data_loader
  2. 数据清洗      — data_cleaner
  3. 探索性分析    — eda
  4. 特征工程      — feature_engineering
  5. 统计分析      — statistics
  6. 文本处理(NLP) — text_processor
  7. 可视化        — visualizer

Usage:
    from data_analysis import DataCleaner, FeatureEngineer
    from data_analysis.config import AnalysisConfig
"""

from data_analysis.config import AnalysisConfig
from data_analysis.data_loader import DataLoader
from data_analysis.data_cleaner import DataCleaner
from data_analysis.eda import EDA
from data_analysis.feature_engineering import FeatureEngineer
from data_analysis.statistics import Statistics
from data_analysis.text_processor import TextProcessor
from data_analysis.visualizer import Visualizer

__version__ = "1.0.0"
__all__ = [
    "AnalysisConfig",
    "DataLoader",
    "DataCleaner",
    "EDA",
    "FeatureEngineer",
    "Statistics",
    "TextProcessor",
    "Visualizer",
]
