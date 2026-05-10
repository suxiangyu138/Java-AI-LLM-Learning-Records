"""文本处理器 — TF-IDF 向量化 / 文本清洗 / 词频分析 / 文本统计，面向 NLP 管道。"""

from __future__ import annotations

import logging
import re
from collections import Counter
from typing import Optional

import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer

from data_analysis.config import AnalysisConfig


class TextProcessor:
    """NLP 文本预处理和向量化。

    Usage:
        tp = TextProcessor(cfg)
        clean = tp.clean_text(df, "review")
        tfidf = tp.tfidf_vectorize(clean, "review_clean")
        freq = tp.word_frequency(clean["review_clean"], top_n=20)
    """

    _STOP_WORDS_CN = {"的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
                        "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己"}

    def __init__(self, config: AnalysisConfig) -> None:
        self.cfg = config
        self._log = logging.getLogger(self.__class__.__name__)
        self._vectorizer: Optional[TfidfVectorizer] = None
        self._vocabulary: dict[str, int] = {}

    # ------------------------------------------------------------------
    # 文本清洗
    # ------------------------------------------------------------------

    def clean_text(
        self,
        df: pd.DataFrame,
        text_col: str,
        new_col: str | None = None,
        *,
        lower: bool = True,
        remove_urls: bool = True,
        remove_digits: bool = False,
        remove_punct: bool = True,
        min_length: int = 2,
    ) -> pd.DataFrame:
        """清洗文本列：去 URL、标点、多余空格，可选去数字/小写化。

        Args:
            df: 输入 DataFrame
            text_col: 文本列名
            new_col: 输出列名（None 则覆盖原列）
            lower: 是否转小写
            remove_urls: 是否移除 URL
            remove_digits: 是否移除数字
            remove_punct: 是否移除标点
            min_length: 过滤太短的文本
        """
        out_col = new_col or text_col
        series = df[text_col].astype(str).copy()

        if lower:
            series = series.str.lower()
        if remove_urls:
            series = series.str.replace(r"https?://\S+", " ", regex=True)
        if remove_digits:
            series = series.str.replace(r"\d+", " ", regex=True)
        if remove_punct:
            series = series.str.replace(r"[^\w\s]", " ", regex=True)

        # 合并空白、去首尾空格
        series = series.str.replace(r"\s+", " ", regex=True).str.strip()

        df = df.copy()
        df[out_col] = series
        # 过滤太短的文本
        short_mask = df[out_col].str.len() < min_length
        if short_mask.any():
            self._log.info("过滤短文本: %d 行", short_mask.sum())
            df.loc[short_mask, out_col] = ""
        self._log.info("文本清洗完成: %s → %s", text_col, out_col)
        return df

    # ------------------------------------------------------------------
    # TF-IDF 向量化（LLM 特征提取核心）
    # ------------------------------------------------------------------

    def tfidf_vectorize(
        self,
        texts: pd.Series | list[str],
        return_features: bool = True,
    ) -> np.ndarray | tuple[np.ndarray, list[str]]:
        """TF-IDF 向量化 — 将文本转为数值特征矩阵，可直接喂给 ML/LLM 模型。

        Returns:
            X: shape (n_samples, n_features) 的稀疏/密集矩阵
            (X, feature_names): 若 return_features=True
        """
        self._vectorizer = TfidfVectorizer(
            max_features=self.cfg.max_features_tfidf,
            ngram_range=self.cfg.ngram_range,
            stop_words="english",
            sublinear_tf=True,
            max_df=0.9,
            min_df=2,
        )
        X = self._vectorizer.fit_transform(texts)
        self._vocabulary = self._vectorizer.vocabulary_
        self._log.info("TF-IDF: %d samples × %d features", X.shape[0], X.shape[1])
        if return_features:
            feature_names = self._vectorizer.get_feature_names_out().tolist()
            return X, feature_names
        return X

    def get_top_tfidf_terms(
        self, X: np.ndarray, feature_names: list[str], top_n: int = 20
    ) -> pd.DataFrame:
        """返回 TF-IDF 全局最高分词汇（用于理解文档集的关键词）。"""
        if hasattr(X, "toarray"):
            X = X.toarray()
        scores = np.asarray(X).mean(axis=0)
        top_idx = np.argsort(scores)[::-1][:top_n]
        return pd.DataFrame({
            "term": [feature_names[i] for i in top_idx],
            "tfidf_mean": scores[top_idx],
        })

    # ------------------------------------------------------------------
    # 词频 & N-gram 分析
    # ------------------------------------------------------------------

    def word_frequency(
        self, texts: pd.Series | list[str], top_n: int = 30, min_len: int = 2
    ) -> pd.DataFrame:
        """词频统计 — 快速了解文本集的词汇分布。"""
        counter: Counter[str] = Counter()
        for text in texts:
            tokens = str(text).lower().split()
            counter.update(t for t in tokens if len(t) >= min_len)
        items = counter.most_common(top_n)
        return pd.DataFrame(items, columns=["word", "count"]).assign(
            proportion=lambda d: d["count"] / d["count"].sum()
        )

    def text_statistics(self, texts: pd.Series) -> pd.DataFrame:
        """文本统计：长度分布、词数分布、平均词长。"""
        stats_df = pd.DataFrame({
            "char_length": texts.astype(str).str.len(),
            "word_count": texts.astype(str).str.split().str.len(),
        })
        return stats_df.describe()

    # ------------------------------------------------------------------
    # 中文分词辅助
    # ------------------------------------------------------------------

    def segment_cn(self, text: str) -> list[str]:
        """简单的中文分词（基于停用词和标点切分，生产环境请用 jieba）。"""
        text = re.sub(r"[^一-鿿\w]", " ", text)
        tokens = text.split()
        return [t for t in tokens if t not in self._STOP_WORDS_CN and len(t) >= 2]

    def clean_cn_text(
        self, df: pd.DataFrame, text_col: str, new_col: str | None = None
    ) -> pd.DataFrame:
        """中文文本清洗 + 分词。"""
        out_col = new_col or f"{text_col}_segmented"
        df = df.copy()
        df[out_col] = df[text_col].astype(str).apply(
            lambda x: " ".join(self.segment_cn(x))
        )
        # 过滤空结果
        empty = df[out_col].str.strip() == ""
        if empty.any():
            self._log.info("中文分词后空文本: %d 行", empty.sum())
        return df
