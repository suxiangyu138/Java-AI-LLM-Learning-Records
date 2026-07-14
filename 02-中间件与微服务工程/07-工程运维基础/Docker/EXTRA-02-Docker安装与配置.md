# 02 - Docker 安装与配置

## 2.1 安装方式概述

| 平台 | 推荐方式 | 备注 |
|---|---|---|
| Windows 10/11 | Docker Desktop for Windows | 内置 WSL2 后端 |
| macOS | Docker Desktop for Mac | 支持 Apple Silicon (M1/M2/M3) |
| Linux (Ubuntu/Debian) | `apt` 直接安装 Docker CE | 服务器最常见 |
| Linux (CentOS/RHEL) | `yum` 安装 Docker CE | 企业环境常用 |

## 2.2 Windows 安装 (AMD64)

### 前置条件
- Windows 10/11 专业版/企业版（开启 Hyper-V）或任意版本（使用 WSL2）
- 启用虚拟化（BIOS 中开启 VT-x/AMD-V）

### 步骤
1. 下载 [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)
2. 双击安装包，选择 **Use WSL 2 instead of Hyper-V**（推荐）
3. 安装完成后重启
4. 执行 `wsl --install` 确保 WSL2 已安装
5. 验证安装：

```powershell
docker --version
docker run hello-world
```

### 配置镜像加速（国内必备）

打开 Docker Desktop → Settings → Docker Engine，在 JSON 中配置：

```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
```

点击 **Apply & Restart**。

## 2.3 Linux 安装 (Ubuntu 22.04)

```bash
# 1. 卸载旧版本
sudo apt remove docker docker-engine docker.io containerd runc

# 2. 安装依赖
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release

# 3. 添加 Docker 官方 GPG 密钥
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 4. 添加 Docker 仓库
echo \
  "deb [arch=$(dpkg --print-architecture) \
  signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 5. 安装 Docker CE
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin

# 6. 将当前用户加入 docker 组（避免每次 sudo）
sudo usermod -aG docker $USER
newgrp docker  # 或重新登录

# 7. 验证
docker --version
docker run hello-world
```

### 配置镜像加速（Linux）

```bash
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF

sudo systemctl daemon-reload
sudo systemctl restart docker
```

## 2.4 关键配置说明

### docker 组权限

为什么 `sudo usermod -aG docker $USER`？

- Docker daemon 通过 Unix Socket (`/var/run/docker.sock`) 通信
- 默认只有 root 和 docker 组成员能访问
- 添加用户到 docker 组，避免每次命令加 `sudo`

### Docker Daemon 配置 (`/etc/docker/daemon.json`)

```json
{
  "registry-mirrors": ["https://xxx.mirror.com"],  // 镜像加速
  "insecure-registries": ["192.168.1.100:5000"],    // 私有仓库（非 HTTPS）
  "data-root": "/data/docker",                       // 修改数据存储目录
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",                               // 单个容器最大日志大小
    "max-file": "3"                                  // 最多保留日志文件数
  },
  "storage-driver": "overlay2",                      // 存储驱动
  "default-address-pools": [
    {
      "base": "172.17.0.0/16",
      "size": 24
    }
  ]
}
```

## 2.5 验证安装

```bash
# 查看 Docker 版本信息
docker version

# 查看 Docker 系统信息（存储驱动、镜像数、容器数等）
docker info

# 运行测试容器
docker run hello-world
```

### hello-world 输出解读

```
Hello from Docker!
This message shows that your installation appears to be working correctly.
```

这个输出说明 Docker 成功完成了：连接到 daemon → 拉取镜像 → 创建容器 → 运行容器 → 输出结果。

## 2.6 Docker Compose 安装（按需）

Docker Desktop 自带 Docker Compose。Linux 手动安装：

```bash
# 下载最新版 Docker Compose
sudo curl -L \
  "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose

# 验证
docker-compose --version
# 或使用新版命令
docker compose version
```

---

> **下一步**：[03-Docker镜像管理](03-Docker镜像管理.md)
