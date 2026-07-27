# Ubuntu 包管理与软件安装

> 📦 apt/dpkg/snap/AppImage 四种包管理方式 + JDK/Maven/Git/Docker/Nginx 安装实战

---

## 📚 目录

1. [包管理方式对比](#1-包管理方式对比)
2. [apt 最常用操作](#2-apt-最常用操作)
3. [Java 开发工具安装](#3-java-开发工具安装)
4. [Docker 安装与配置](#4-docker-安装与配置)
5. [PPA 与第三方源](#5-ppa-与第三方源)

---

## 1. 包管理方式对比

| 方式 | 依赖处理 | 软件来源 | 更新 | 适合 |
|------|:------:|------|:---:|------|
| **apt** | ✅ 自动 | Ubuntu 官方源 | `apt upgrade` | 首选 90% 场景 |
| **dpkg** | ❌ 手动 | 下载的 .deb 文件 | 手动 | 离线安装 |
| **snap** | ✅ 自带 | Snap Store | 自动后台 | 沙盒应用 |
| **AppImage** | ✅ 打包 | 官网下载 | 手动 | 免安装运行 |
| **源码编译** | ❌ 手动 | GitHub/官网 | 手动 | 特定版本/自定义 |
| **pip/npm** | ✅ 各自生态 | 各自仓库 | 各自命令 | 开发依赖 |

---

## 2. apt 最常用操作

```bash
# ==== 更新与升级 ====
sudo apt update                  # 更新软件包列表
sudo apt upgrade                 # 升级所有已安装的包
sudo apt full-upgrade            # 智能升级（处理依赖变化）
sudo apt autoremove              # 删除不再需要的依赖

# ==== 搜索与安装 ====
apt search nginx                 # 搜索软件包
apt show nginx                   # 查看软件包详情
sudo apt install nginx           # 安装
sudo apt install -y nginx        # 安装（跳过确认）

# ==== 删除 ====
sudo apt remove nginx            # 删除软件（保留配置文件）
sudo apt purge nginx             # 完全删除（含配置文件）

# ==== 查看已安装 ====
apt list --installed             # 列出所有已安装
apt list --installed | grep nginx
dpkg -l | grep nginx             # 同上

# ==== PPA 源管理 ====
sudo add-apt-repository ppa:xxx/yyy
sudo apt update                  # 添加 PPA 后必须 update
```

### 2.1 镜像源加速

```bash
# 国内推荐：清华 / 阿里云 / 中科大镜像
# 编辑 /etc/apt/sources.list 或用 GUI："Software & Updates"

# 查看当前 Ubuntu 版本
lsb_release -a

# 快速换源（Ubuntu 24.04 示例）
sudo sed -i 's@//.*archive.ubuntu.com@//mirrors.tuna.tsinghua.edu.cn@g' /etc/apt/sources.list
sudo apt update
```

---

## 3. Java 开发工具安装

### 3.1 JDK

```bash
# ==== 方式 1：apt 安装 OpenJDK（最简单）====
sudo apt update
sudo apt install openjdk-21-jdk    # JDK 21 LTS
sudo apt install openjdk-17-jdk    # JDK 17 LTS

# 验证
java -version
javac -version

# ==== 方式 2：多版本管理（update-alternatives）====
sudo apt install openjdk-17-jdk openjdk-21-jdk

# 查看已安装的 Java
update-java-alternatives -l

# 切换默认 JDK
sudo update-alternatives --config java
sudo update-alternatives --config javac

# ==== 方式 3：SDKMAN（推荐！多版本灵活切换）====
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh

sdk list java                    # 查看可用版本
sdk install java 21.0.5-tem     # 安装 JDK 21
sdk install java 17.0.12-tem    # 安装 JDK 17
sdk use java 21.0.5-tem         # 临时切换到 21
sdk default java 21.0.5-tem     # 永久设置默认

# ==== JAVA_HOME 配置（/etc/environment 或 ~/.bashrc）====
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### 3.2 其他开发工具

```bash
# Maven
sudo apt install maven             # 版本可能较旧
# 或手动下载最新版 → 解压到 /opt/maven → 配置 PATH

# Gradle
# 推荐 SDKMAN：
sdk install gradle 8.10
# 或手动：gradle.org 下载 → /opt/gradle → 配置 PATH

# Git（Ubuntu 自带，更新即可）
sudo apt install git
git --version

# VS Code（推荐用 snap）
sudo snap install code --classic

# IntelliJ IDEA
sudo snap install intellij-idea-community --classic
sudo snap install intellij-idea-ultimate --classic

# Node.js（推荐用 nvm 管理多版本）
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
nvm install 20        # 安装 Node.js 20 LTS
nvm use 20
```

---

## 4. Docker 安装与配置

### 4.1 官方脚本安装

```bash
# 官方安装脚本（推荐）
curl -fsSL https://get.docker.com | sudo sh

# 将当前用户加入 docker 组（免 sudo）
sudo usermod -aG docker $USER
# 重新登录生效
newgrp docker

# 验证
docker --version
docker run hello-world

# Docker Compose
sudo apt install docker-compose-plugin
docker compose version
```

### 4.2 镜像加速

```bash
# /etc/docker/daemon.json
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.mirrors.ustc.edu.cn"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}

sudo systemctl restart docker
docker info | grep -A 5 "Registry Mirrors"  # 验证
```

### 4.3 MySQL / Redis（Docker 快速启动）

```bash
# MySQL 8
docker run -d --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -v ~/data/mysql:/var/lib/mysql \
  mysql:8.0

# Redis
docker run -d --name redis \
  -p 6379:6379 \
  redis:7-alpine

# 用 Docker Compose 管理更优雅
```

---

## 5. PPA 与第三方源

```bash
# PPA = Personal Package Archive（个人软件源）
# 用于安装官方源中没有的软件

# 添加 PPA
sudo add-apt-repository ppa:ondrej/php    # PHP 最新版
sudo add-apt-repository ppa:deadsnakes/ppa  # Python 多版本
sudo apt update

# 删除 PPA
sudo add-apt-repository --remove ppa:xxx/yyy

# 列出所有 PPA
ls /etc/apt/sources.list.d/
```

```text
apt install 找不到的软件？按顺序尝试：

  1. snap find <name> && sudo snap install <name>
  2. 官方下载 .deb → sudo dpkg -i xxx.deb
  3. 官方下载 AppImage → chmod +x → 直接运行
  4. 官方下载 tar.gz → 解压到 /opt → 加 PATH
  5. GitHub Releases → 下载对应平台的二进制
```

---

> 🎯 **核心要点**：**apt install** 能搞定 90%，**SDKMAN** 管 Java 全家桶（JDK/Gradle/Maven），**Docker** 管中间件（MySQL/Redis/ES），**snap** 装桌面应用。

---

*创建于：2026年7月*
