import numpy as np
from sklearn.linear_model import LinearRegression

X = np.array([
    [50, 10],
    [60, 8],
    [80, 5],
    [100, 2],
    [120, 1],
    [70, 6],
    [90, 3]
], dtype=float)

true_w = np.array([3000, -5000], dtype=float)
true_b = 200000.0

np.random.seed(0)
noise = np.random.normal(0, 20000, size=X.shape[0])
y = X @ true_w + true_b + noise

model = LinearRegression()
model.fit(X, y)

print("learned w:", model.coef_)
print("learned b:", model.intercept_)

X_new = np.array([
    [85, 4],
    [110, 2]
], dtype=float)

print("predict:", model.predict(X_new))