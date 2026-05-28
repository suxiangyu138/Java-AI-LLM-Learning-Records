"""
数据分析练习 - 主入口
使用生成的销售数据演示数据清洗、分析和可视化流程
"""
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False


def create_sample_data() -> pd.DataFrame:
    """生成示例销售数据"""
    np.random.seed(42)
    n = 200
    data = {
        'date': pd.date_range('2024-01-01', periods=n, freq='D'),
        'product': np.random.choice(['产品A', '产品B', '产品C', '产品D'], n),
        'category': np.random.choice(['电子', '家居', '服装'], n),
        'quantity': np.random.randint(1, 20, n),
        'unit_price': np.random.uniform(10, 500, n).round(2),
    }
    df = pd.DataFrame(data)
    df['revenue'] = df['quantity'] * df['unit_price']
    # 模拟缺失值
    df.loc[np.random.choice(n, 5), 'quantity'] = np.nan
    return df


def clean_data(df: pd.DataFrame) -> pd.DataFrame:
    """数据清洗"""
    print(f"原始数据: {len(df)} 行")
    print(f"缺失值数量:\n{df.isnull().sum()}")
    # 填充缺失值
    df['quantity'] = df['quantity'].fillna(df['quantity'].median())
    # 删除重复行
    df = df.drop_duplicates()
    print(f"清洗后: {len(df)} 行")
    return df


def analyze(df: pd.DataFrame):
    """数据分析"""
    print("\n=== 基本统计 ===")
    print(df[['quantity', 'unit_price', 'revenue']].describe())

    print("\n=== 按品类汇总 ===")
    category_stats = df.groupby('category').agg(
        总营收=('revenue', 'sum'),
        平均单价=('unit_price', 'mean'),
        订单数=('quantity', 'sum')
    ).round(2)
    print(category_stats)

    print("\n=== 按产品汇总 Top3 ===")
    product_top = df.groupby('product')['revenue'].sum().sort_values(ascending=False).head(3)
    print(product_top)


def visualize(df: pd.DataFrame):
    """数据可视化"""
    fig, axes = plt.subplots(1, 3, figsize=(15, 5))

    # 1. 各品类营收柱状图
    category_revenue = df.groupby('category')['revenue'].sum()
    axes[0].bar(category_revenue.index, category_revenue.values, color=['#4C72B0', '#55A868', '#C44E52'])
    axes[0].set_title('各品类总营收')
    axes[0].set_ylabel('营收 (元)')

    # 2. 每日营收趋势
    daily = df.groupby('date')['revenue'].sum()
    axes[1].plot(daily.index, daily.values, color='#4C72B0', linewidth=1)
    axes[1].set_title('每日营收趋势')
    axes[1].tick_params(axis='x', rotation=45)

    # 3. 产品数量分布
    product_counts = df['product'].value_counts()
    axes[2].pie(product_counts.values, labels=product_counts.index, autopct='%1.1f%%',
                colors=sns.color_palette('pastel'))
    axes[2].set_title('产品销量占比')

    plt.tight_layout()
    plt.savefig('output/sales_analysis.png', dpi=150, bbox_inches='tight')
    print("\n图表已保存至: output/sales_analysis.png")


if __name__ == "__main__":
    import os
    os.makedirs('output', exist_ok=True)

    print("=" * 50)
    print("  数据分析练习 - 销售数据 EDA")
    print("=" * 50)

    df = create_sample_data()
    df = clean_data(df)
    analyze(df)
    visualize(df)

    print("\n分析完成！可以开始探索真实数据集了。")
