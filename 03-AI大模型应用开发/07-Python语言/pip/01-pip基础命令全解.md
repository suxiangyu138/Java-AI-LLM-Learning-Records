# 01 - pip 基础命令全解

> 🎯 pip 是每个 Python 开发者每天都要用的工具。掌握 install/uninstall/list/freeze/check 五大核心命令及其进阶用法，能解决 90% 的包管理需求

---

## 目录

1. [核心命令速查](#1-核心命令速查)
2. [install 进阶用法](#2-install-进阶用法)
3. [依赖管理与审计](#3-依赖管理与审计)
4. [pip config 配置](#4-pip-config-配置)

---

## 1. 核心命令速查

| 命令 | 用途 |
|------|------|
| `pip install <pkg>` | 安装包 |
| `pip install -r requirements.txt` | 批量安装 |
| `pip uninstall <pkg>` | 卸载包 |
| `pip list` | 列出已安装的包 |
| `pip show <pkg>` | 查看包详情 |
| `pip freeze > requirements.txt` | 导出依赖 |
| `pip check` | 检查依赖冲突 |
| `pip cache purge` | 清理缓存 |

## 2. install 进阶用法

```bash
# 指定版本
pip install numpy==1.26.4
pip install "numpy>=1.24,<2.0"

# 从各种源安装
pip install package.whl                  # 本地 wheel
pip install git+https://github.com/user/repo.git  # GitHub
pip install git+https://github.com/user/repo.git@v1.2.3  # 指定 tag
pip install -e .                         # 可编辑模式（开发用）

# 不安装依赖
pip install --no-deps <pkg>

# 升级
pip install --upgrade <pkg>
pip install --upgrade pip               # 升级 pip 自身

# 指定镜像源
pip install -i https://pypi.tuna.tsinghua.edu.cn/simple <pkg>
pip install --index-url https://mirrors.aliyun.com/pypi/simple/ <pkg>

# 超时与重试
pip install --timeout 120 --retries 5 <pkg>

# 用户安装（无需 sudo）
pip install --user <pkg>
```

## 3. 依赖管理与审计

```bash
# 查看依赖树
pip install pipdeptree
pipdeptree                    # 列出所有依赖树
pipdeptree -p <pkg>           # 查看某个包的依赖
pipdeptree --reverse -p <pkg> # 哪些包依赖了它

# 检查依赖冲突
pip check

# 安全审计
pip install pip-audit
pip-audit                     # 检查已知漏洞

# 查看包的文件位置
pip show -f numpy | grep Location

# 列出过期的包
pip list --outdated
```

## 4. pip config 配置

```bash
# 查看当前配置
pip config list

# 设置全局配置
pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple

# 文件位置（三选一，优先级：site > user > global）
# ~/.config/pip/pip.conf          (Linux)
# ~/Library/Application Support/pip/pip.conf (macOS)
# %APPDATA%\pip\pip.ini           (Windows)
```

```ini
# pip.conf 实例
[global]
index-url = https://mirrors.aliyun.com/pypi/simple/
timeout = 120
retries = 5

[install]
# 只允许二进制 wheel（避免编译问题）
only-binary = :all:
```

## 核心要点回顾

- `pip install -r` 批量安装，`-e .` 开发模式
- `pipdeptree` 查看依赖树，`pip-audit` 安全审计
- `pip-compile` (pip-tools) 锁定精确版本
- 国内开发必须配镜像源，否则等半天

## 参考资料

1. pip 官方文档 — pip.pypa.io
