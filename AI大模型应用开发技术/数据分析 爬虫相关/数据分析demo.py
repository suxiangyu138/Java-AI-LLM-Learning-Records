import pandas as pd
import matplotlib.pyplot as plt

# 解决中文乱码和负号显示问题
plt.rcParams["font.sans-serif"] = ["SimHei"]
plt.rcParams["axes.unicode_minus"] = False

# 1. 读取/构造数据
df = pd.DataFrame({
    "姓名": ["张三", "李四", "王五", "赵六"],
    "性别": ["男", "女", "男", "女"],
    "成绩": [85, 92, 78, 90]
})

# 2. 查看基础信息
print("=" * 50)
print("📊 数据基础信息")
print("=" * 50)
print(f"数据形状（行, 列）: {df.shape}")
print("\n数据前2行预览:")
print(df.head(2))
print("\n数值列描述性统计:")
print(df.describe())

# 3. 缺失值检查
print("\n" + "=" * 50)
print("🧹 缺失值统计")
print("=" * 50)
print(df.isnull().sum())

# 4. 分组统计
print("\n" + "=" * 50)
print("📈 按性别平均成绩")
print("=" * 50)
gender_score = df.groupby("性别")["成绩"].mean()
print(gender_score)

# 5. 可视化
plt.figure(figsize=(6, 4))  # 设置图表大小
gender_score.plot(kind="bar", color=["#1f77b4", "#ff7f0e"])
plt.title("男女平均成绩对比", fontsize=14)
plt.ylabel("平均成绩", fontsize=12)
plt.xlabel("性别", fontsize=12)
plt.ylim(0, 100)  # 设置Y轴范围，让图表更直观
plt.grid(axis="y", linestyle="--", alpha=0.7)
plt.tight_layout()  # 自动调整布局，防止标题被截断
plt.show()