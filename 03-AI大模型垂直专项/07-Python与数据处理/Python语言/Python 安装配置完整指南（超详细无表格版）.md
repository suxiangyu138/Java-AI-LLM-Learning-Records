# Python 安装配置完整指南

> **核心摘要**：适配 Windows、macOS、Linux 三大系统，全程无复杂步骤，新手可按流程一步步操作，确保安装后环境稳定可用。推荐 Python 3.8~3.12 版本。

---

## 一、安装前的准备

### 1.1 确认系统位数

- **Windows**：右键「此电脑」→「属性」，查看「系统类型」
- **macOS/Linux**：无需额外确认，安装包自动适配

### 1.2 卸载旧版本（可选）

| 系统 | 操作 |
|------|------|
| Windows | 控制面板 → 程序和功能 → 卸载 Python |
| macOS | `sudo rm -rf /Library/Frameworks/Python.framework` |
| Linux | `sudo apt remove python3`（注意系统可能依赖 Python） |

## 二、Windows 系统安装配置

### 2.1 下载安装包

1. 打开 Python 官方下载页面，推荐 3.8~3.12 版本
2. 选择对应版本，下载「Windows Installer (64-bit)」

### 2.2 运行安装

1. 双击 `.exe` 文件，**务必勾选**「Add Python 3.x to PATH」
2. 推荐「Customize installation」自定义安装路径（如 `D:\Python310`）
3. 安装完成后，勾选「Disable path length limit」解除路径长度限制

### 2.3 验证安装

```bash
python --version
```

若提示「python 不是内部或外部命令」，需手动配置环境变量：添加 Python 安装路径和 Scripts 路径到系统 Path 变量。

### 2.4 配置 pip 国内镜像源

创建 `C:\Users\用户名\pip\pip.ini` 文件：

```ini
[global]
index-url = https://pypi.tuna.tsinghua.edu.cn/simple
[install]
trusted-host = pypi.tuna.tsinghua.edu.cn
```

## 三、macOS 系统安装配置

### 3.1 下载安装

1. 选择 3.8~3.12 版本，下载「macOS 64-bit universal2 installer」
2. 双击 `.pkg` 文件，按引导安装

### 3.2 验证安装

```bash
python3 --version
```

> **注意**：macOS 自带 Python 2.7，需用 `python3` 调用新版本，`pip` 对应 `pip3`。

### 3.3 配置国内镜像源

```bash
mkdir ~/.pip
nano ~/.pip/pip.conf
```

写入：

```ini
[global]
index-url = https://pypi.tuna.tsinghua.edu.cn/simple
[install]
trusted-host = pypi.tuna.tsinghua.edu.cn
```

## 四、Linux 系统安装配置（Ubuntu）

### 4.1 方式一：apt 安装（简单）

```bash
sudo apt update
sudo apt install python3 python3-pip
python3 --version
```

### 4.2 方式二：源码编译安装（版本最新）

```bash
# 安装编译依赖
sudo apt install build-essential zlib1g-dev libncurses5-dev libgdbm-dev libnss3-dev libssl-dev libreadline-dev libffi-dev wget

# 下载源码
wget https://www.python.org/ftp/python/3.10.11/Python-3.10.11.tgz

# 编译安装
tar -xf Python-3.10.11.tgz
cd Python-3.10.11
./configure --prefix=/usr/local/python3 --enable-optimizations
make -j4
sudo make install

# 创建软链接
sudo ln -s /usr/local/python3/bin/python3 /usr/bin/python3
sudo ln -s /usr/local/python3/bin/pip3 /usr/bin/pip3
```

### 4.3 配置国内镜像源

```bash
mkdir ~/.pip
nano ~/.pip/pip.conf
```

写入与 macOS 相同的镜像配置。

## 五、安装虚拟环境工具

```bash
pip install virtualenv

# 创建虚拟环境
cd D:\PythonProjects
virtualenv venv

# 激活
# Windows: venv\Scripts\activate
# macOS/Linux: source venv/bin/activate

# 退出
deactivate
```

## 六、验证安装

```bash
python -c "print('Hello Python!')"
```

## 七、常见问题

| 问题 | 解决方案 |
|------|---------|
| pip 命令找不到 | 检查 Scripts 路径是否在环境变量中，或使用 `python -m pip` |
| 安装包权限不足 | Windows 以管理员身份运行；macOS/Linux 加 `sudo` |
| 虚拟环境激活失败 | Windows PowerShell 执行 `Set-ExecutionPolicy RemoteSigned` |

## 核心要点回顾

- 安装务必勾选「Add Python to PATH」
- 配置国内镜像源可大幅提升 pip 下载速度
- 虚拟环境是项目隔离的必备工具
- Windows 用 `python`，macOS/Linux 用 `python3`
- 推荐 Python 3.8~3.12 版本，兼容主流 AI 库

## 参考资料

1. Python 官方下载页面 - python.org/downloads
2. 清华大学开源软件镜像站 - PyPI 镜像使用帮助
