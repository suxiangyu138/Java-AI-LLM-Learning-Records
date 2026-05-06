import numpy as np

X = np.array([
    [1, 80],
    [1, 85],
    [1, 90],
    [1, 95],
    [1, 100]
], dtype=float)

y = np.array([[70], [72], [75], [78], [80]], dtype=float)

m, n = X.shape
w = np.zeros((n, 1))
alpha = 0.01
epochs = 1000

for _ in range(epochs):
    y_pred = X.dot(w)
    error = y_pred - y

    dw = (1.0 / m) * X.T.dot(error)

    w = w - alpha * dw

print(w)
print(X.dot(w))