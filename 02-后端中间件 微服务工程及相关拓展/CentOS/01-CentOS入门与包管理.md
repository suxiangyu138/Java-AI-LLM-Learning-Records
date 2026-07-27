# CentOS 入门与包管理

> 🔴 RHEL 版图变迁、CentOS Stream/Rocky/Alma 选型、dnf 完整操作手册、rpm 底层、EPEL/第三方源、Java 工具链安装

---

## 📚 目录

1. [RHEL 生态与版本选择](#1-rhel-生态与版本选择)
2. [CentOS vs Ubuntu 对比](#2-centos-vs-ubuntu-对比)
3. [dnf 完整操作手册](#3-dnf-完整操作手册)
4. [rpm 底层命令](#4-rpm-底层命令)
5. [第三方仓库与镜像加速](#5-第三方仓库与镜像加速)

---

## 1. RHEL 生态与版本选择

```text
RHEL 家族演变（2020→2026）：

  Fedora（社区版，每 6 月）→ CentOS Stream（滚动版）→ RHEL（付费）
  
  兼容 RHEL 的免费复刻：
  ├── Rocky Linux → CentOS 创始人重建 ⭐ 生产首选
  └── AlmaLinux   → CloudLinux 赞助

  ⚠️ CentOS 7 (2024.06 EOL) / CentOS 8 (2021.12 EOL) → 已死，别用！
  推荐：Rocky Linux 9 / AlmaLinux 9 / CentOS Stream 9
```

```bash
# 查看版本
cat /etc/os-release && cat /etc/redhat-release
uname -r                 # 内核版本
hostnamectl              # 完整系统信息
```

### 1.1 选型建议

| 场景 | 推荐 |
|------|:----:|
| 传统企业/政务 | RHEL 9（官方支持+合规） |
| 互联网公司 | Rocky Linux 9 / AlmaLinux 9 |
| 开发测试 | CentOS Stream 9 |
| Docker 镜像 | Rocky 9-minimal / UBI（~100MB） |

---

## 2. CentOS vs Ubuntu 对比

| 维度 | CentOS/RHEL | Ubuntu |
|------|:---------:|:-----:|
| 包管理 | dnf + rpm | apt + dpkg |
| 包名 | `java-21-openjdk-devel` | `openjdk-21-jdk` |
| 防火墙 | firewalld (nftables) | ufw (iptables) |
| 安全 | SELinux（细粒度强制访问） | AppArmor（路径级） |
| 文件系统 | XFS（默认） | ext4（默认） |
| 更新策略 | 极度保守，稳定版 | 相对激进 |
| 市场 | 传统企业/金融/政务 | 互联网/云原生/初创 |

```bash
# 包管理命令对照
# CentOS: dnf search / install / remove / upgrade / info
# Ubuntu: apt search / install / remove / upgrade / show
```

---

## 3. dnf 完整操作手册

```bash
# ===== 搜索与信息 =====
dnf search keyword                     # 搜索
dnf info nginx                         # 详情
dnf deplist nginx                      # 依赖树
dnf repoquery --requires java-21       # 直接依赖

# ===== 安装与升级 =====
sudo dnf install nginx                 # 安装
sudo dnf install nginx vim git         # 批量
sudo dnf upgrade                       # 升级所有
sudo dnf upgrade --security            # 仅安全补丁
sudo dnf upgrade nginx                 # 仅升级指定

# ===== 删除 =====
sudo dnf remove nginx
sudo dnf autoremove nginx              # 删+清理孤立依赖

# ===== 历史回滚（神器！）=====
dnf history                            # 操作历史
sudo dnf history undo 42               # 撤销第42次操作
sudo dnf history rollback 40           # 回滚到第40次

# ===== 仓库管理 =====
dnf repolist                           # 已启用源
sudo dnf config-manager --enable epel  # 启用 EPEL

# ===== 包组 =====
dnf group list
sudo dnf groupinstall "Development Tools"
```

### 3.1 镜像加速

```bash
# 阿里云 CentOS Stream 9
sudo sed -i 's|^mirrorlist=|#mirrorlist=|g' /etc/yum.repos.d/centos*.repo
sudo sed -i 's|^#baseurl=http://mirror.centos.org|baseurl=https://mirrors.aliyun.com|g' \
  /etc/yum.repos.d/centos*.repo

# EPEL 阿里云
sudo sed -i 's|^metalink=|#metalink=|g' /etc/yum.repos.d/epel*.repo
sudo sed -i 's|^#baseurl=|baseurl=|g' /etc/yum.repos.d/epel*.repo
sudo sed -i 's|download.example/fedora/epel|mirrors.aliyun.com/epel|g' \
  /etc/yum.repos.d/epel*.repo

sudo dnf clean all && sudo dnf makecache
```

---

## 4. rpm 底层命令

```bash
# ===== 查询 =====
rpm -qa | grep java                    # 已安装列表
rpm -qi java-21-openjdk                # 包详情
rpm -ql java-21-openjdk                # 包内文件
rpm -qc nginx                          # 配置文件
rpm -qf /usr/bin/java                  # 文件属于哪个包
rpm -qR java-21-openjdk                # 依赖列表

# ===== 安装/卸载 =====
sudo rpm -ivh package.rpm              # 安装
sudo rpm -Uvh package.rpm              # 升级
sudo rpm -e package-name               # 卸载

# ===== 验证 =====
rpm -V java-21-openjdk                 # 检查文件完整性
rpm -Va                                 # 检查所有包
```

---

## 5. 第三方仓库与镜像加速

```bash
# ===== EPEL（必装）=====
sudo dnf install epel-release
sudo dnf install htop tmux ripgrep fzf  # EPEL 中的常用工具

# ===== Docker CE =====
sudo dnf config-manager --add-repo \
  https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install docker-ce docker-compose-plugin
sudo systemctl enable --now docker

# ===== Nginx 官方 =====
sudo tee /etc/yum.repos.d/nginx.repo << 'EOF'
[nginx-stable]
name=nginx stable repo
baseurl=http://nginx.org/packages/centos/$releasever/$basearch/
gpgcheck=1
enabled=1
gpgkey=https://nginx.org/keys/nginx_signing.key
EOF

# ===== JDK 开发环境 =====
sudo dnf install java-21-openjdk-devel maven git
sudo dnf groupinstall "Development Tools"

# ===== 多版本 JDK 切换 =====
sudo dnf install java-17-openjdk-devel java-21-openjdk-devel
sudo alternatives --config java
sudo alternatives --config javac
```

---

> 🎯 **速记**：`dnf search/info/install/remove/upgrade` 日常，`dnf history undo` 后悔药，`rpm -qa/-ql/-qf` 查包，`dnf autoremove` 清理。CentOS 7→8/9 最大变化：**yum→dnf、iptables→firewalld**。

---

*创建于：2026年7月*
