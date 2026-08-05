# 04 - pip install 原理与缓存机制

> 🎯 理解 pip install 的内部流程——解析依赖图、下载 wheel、构建 sdist、安装到 site-packages，能帮你快速诊断安装问题

---

## 目录

1. [install 全流程](#1-install-全流程)
2. [wheel vs sdist](#2-wheel-vs-sdist)
3. [缓存机制](#3-缓存机制)
4. [离线安装技巧](#4-离线安装技巧)

---

## 1. install 全流程

```text
pip install transformers
   │
   ├── 1. 解析 (Resolution)
   │   ├── 查找 PyPI 上的 transformers 包
   │   ├── 读取其依赖：numpy, huggingface-hub, ...
   │   ├── 递归解析所有依赖的依赖
   │   └── 生成最终安装列表（满足所有约束）
   │
   ├── 2. 下载 (Download)
   │   ├── 优先找 wheel (.whl) → 预编译，直接装
   │   ├── 没有 wheel → 下载 sdist (.tar.gz) → 需编译
   │   └── 检查缓存 → 命中则跳过下载
   │
   ├── 3. 构建 (Build)
   │   ├── wheel → 跳过（直接解压）
   │   └── sdist → setup.py/pyproject.toml → 编译 C 扩展
   │
   └── 4. 安装 (Install)
       └── 复制到 site-packages/
```

## 2. wheel vs sdist

| 维度 | wheel (.whl) | sdist (.tar.gz) |
|------|:---:|:---:|
| 编译 | ❌ 预编译 | ✅ 需要本地编译 |
| 安装速度 | 秒级 | 分钟级 |
| 依赖 | 无 | gcc/cmake/pip-build |
| 平台 | 特定平台 | 跨平台 |
| AI 包 | PyTorch 官方 wheel | 自己编译 PyTorch(噩梦) |

```bash
# 只安装 wheel（避免编译问题）
pip install --only-binary :all: <pkg>

# 检查包是否有 wheel
pip install --dry-run <pkg> | grep "wheel"
```

> ⚠️ AI 开发中，尽量用官方 wheel（如 PyTorch 官网的 CUDA 版本），**不要自己编译 PyTorch**

## 3. 缓存机制

```bash
# 查看缓存位置
pip cache dir    # ~/.cache/pip (Linux), ~/Library/Caches/pip (macOS)

# 查看缓存大小
pip cache info

# 清理缓存
pip cache purge                          # 全部清除
pip cache remove <pkg>                   # 只清某个包

# 禁用缓存（排查问题用）
pip install --no-cache-dir <pkg>

# 离线安装（使用缓存）
pip install --find-links=file:///path/to/wheels/ <pkg>
```

## 4. 离线安装技巧

```bash
# 方案一：下载到本地 → 拷贝到离线环境
mkdir wheels
pip download -r requirements.txt -d wheels/
# → 拷贝 wheels/ 到离线机器
pip install --no-index --find-links=wheels/ -r requirements.txt

# 方案二：pip wheel 预构建
pip wheel -r requirements.txt -w wheels/

# 方案三：导出已缓存的 wheel（无需重下载）
pip cache list --format abspath         # 查看缓存的 wheel 路径
```

## 核心要点回顾

- install = 解析依赖图 → 下载 wheel/sdist → 编译(如需要) → 复制到 site-packages
- wheel > sdist：优先 wheel，AI 包尽量用官方预编译
- `--no-cache-dir` 排查安装问题时很有用
- 离线部署：`pip download` 到本地 → 拷文件夹 → `pip install --no-index`

## 参考资料

1. pip 内部原理 — pip.pypa.io/en/stable/topics
