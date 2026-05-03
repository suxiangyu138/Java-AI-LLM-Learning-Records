import numpy as np
from sklearn.linear_model import LinearRegression

# 生成一维线性数据：y = 2x + 1 + 噪声
np.random.seed(42)
X = np.linspace(0, 10, 50).reshape(-1, 1)
noise = np.random.normal(0, 1, size=X.shape[0])
y = 2 * X.reshape(-1) + 1 + noise

# 使用sklearn的线性回归
model = LinearRegression()
model.fit(X, y)

print("w:", model.coef_[0])
print("b:", model.intercept_)
print("R^2:", model.score(X, y))

X_test = np.array([[0], [5], [10]], dtype=float)
print("predict:", model.predict(X_test))