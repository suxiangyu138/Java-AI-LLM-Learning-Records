"""
机器学习练习 - 主入口
数据集: Iris 分类（经典入门案例）
"""
import numpy as np
import pandas as pd
from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix


def load_data():
    """加载 Iris 数据集"""
    iris = load_iris()
    X = pd.DataFrame(iris.data, columns=iris.feature_names)
    y = iris.target
    print(f"数据集大小: {X.shape[0]} 条记录, {X.shape[1]} 个特征")
    print(f"类别: {iris.target_names}")
    return X, y, iris.target_names


def train_model(X, y, class_names):
    """训练随机森林分类器"""
    # 数据划分
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    # 标准化
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    # 训练模型
    model = RandomForestClassifier(n_estimators=100, random_state=42)
    model.fit(X_train_scaled, y_train)

    # 评估
    y_pred = model.predict(X_test_scaled)
    accuracy = model.score(X_test_scaled, y_test)

    print(f"\n模型准确率: {accuracy:.4f}")
    print(f"\n分类报告:\n{classification_report(y_test, y_pred, target_names=class_names)}")
    print(f"混淆矩阵:\n{confusion_matrix(y_test, y_pred)}")

    return model, scaler


if __name__ == "__main__":
    print("=" * 50)
    print("  机器学习练习 - Iris 分类")
    print("=" * 50)
    X, y, class_names = load_data()
    model, scaler = train_model(X, y, class_names)
    print("\n训练完成！可以开始探索更多算法和数据集。")
