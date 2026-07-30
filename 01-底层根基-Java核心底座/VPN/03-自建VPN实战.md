# 自建 VPN 实战

> 🛠️ WireGuard 5 分钟上线、OpenVPN 完整部署、云平台一键搭建、Docker 容器化、性能调优 —— 从零到生产

---

## 📚 目录

1. [为什么自建 VPN](#1-为什么自建-vpn)
2. [WireGuard 快速搭建](#2-wireguard-快速搭建)
3. [OpenVPN 完整部署](#3-openvpn-完整部署)
4. [云平台部署](#4-云平台部署)
5. [Docker 容器化方案](#5-docker-容器化方案)
6. [多客户端配置](#6-多客户端配置)
7. [性能调优](#7-性能调优)
8. [监控与运维](#8-监控与运维)

---

## 1. 为什么自建 VPN

### 1.1 自建 vs 商业 VPN

| 维度 | 自建 VPN | 商业 VPN (Nord/Express 等) |
|------|---------|--------------------------|
| **费用** | 仅 VPS 费用 (~$5/月) | ~$3-12/月 |
| **可控性** | ✅ 完全控制 | ❌ 信任第三方 |
| **IP 纯净度** | ⭐⭐⭐⭐⭐ 独享 IP | ⭐⭐ IP 被大量用户共享，容易被标记 |
| **速度** | 取决于 VPS 带宽 | 共享带宽，高峰期慢 |
| **被封风险** | 低（个人使用，流量小） | 中高（IP 段被大量封锁） |
| **维护成本** | 需要自己维护 | 零维护 |
| **日志** | 你自己说了算 | 取决于服务商（"No Logs" 无法验证） |

### 1.2 推荐 VPS 供应商

| 供应商 | 最低价格 | 优点 | 推荐区域（低延迟） |
|--------|:------:|------|------------------|
| **BandwagonHost** | ~$50/年 | CN2 GIA 线路，对国内优化 | 洛杉矶 DC6/DC9 |
| **Vultr** | $6/月 | 按小时计费，随时销毁 | 东京/新加坡/洛杉矶 |
| **DigitalOcean** | $6/月 | UI 体验好，文档一流 | 新加坡/旧金山 |
| **AWS Lightsail** | $3.5/月 | 大厂稳定，全球节点 | 东京/新加坡 |
| **阿里云国际** | $4.5/月 | 国内访问快 | 香港/新加坡 |

---

## 2. WireGuard 快速搭建

### 2.1 服务器安装 (Ubuntu 22.04)

```bash
# 1. 安装 WireGuard
sudo apt update
sudo apt install wireguard -y

# 2. 生成服务器密钥对
cd /etc/wireguard
sudo wg genkey | sudo tee server_private.key | sudo wg pubkey | sudo tee server_public.key
sudo chmod 600 server_private.key

# 3. 查看密钥（后面会用到）
echo "服务器私钥: $(sudo cat server_private.key)"
echo "服务器公钥: $(sudo cat server_public.key)"
```

### 2.2 服务器配置

```bash
# 获取主网卡名称
ip route show default | awk '{print $5}'
# 假设是 eth0

sudo vim /etc/wireguard/wg0.conf
```

```ini
# /etc/wireguard/wg0.conf
[Interface]
# 服务器私钥
PrivateKey = <服务器私钥>
# VPN 内网地址
Address = 10.0.0.1/24
# 监听端口
ListenPort = 51820

# 开启 IP 转发 (需配合 sysctl)
PostUp = sysctl -w net.ipv4.ip_forward=1
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT
PostUp = iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE

PostDown = iptables -D FORWARD -i wg0 -j ACCEPT
PostDown = iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE

# 客户端配置将在下面添加
# [Peer]
# PublicKey = ...
# AllowedIPs = ...
```

```bash
# 4. 开启 IP 转发 (永久)
sudo sysctl -w net.ipv4.ip_forward=1
echo "net.ipv4.ip_forward=1" | sudo tee -a /etc/sysctl.conf

# 5. 防火墙开放端口
sudo ufw allow 51820/udp
sudo ufw allow OpenSSH

# 6. 启动 WireGuard
sudo systemctl enable wg-quick@wg0
sudo systemctl start wg-quick@wg0

# 7. 验证
sudo wg show
```

### 2.3 客户端配置

```bash
# 在客户端机器上
sudo apt install wireguard -y

# 生成客户端密钥对
cd /etc/wireguard
sudo wg genkey | sudo tee client_private.key | sudo wg pubkey | sudo tee client_public.key
sudo chmod 600 client_private.key

echo "客户端私钥: $(sudo cat client_private.key)"
echo "客户端公钥: $(sudo cat client_public.key)"
```

```ini
# 客户端 /etc/wireguard/wg0.conf
[Interface]
PrivateKey = <客户端私钥>
Address = 10.0.0.2/24
DNS = 1.1.1.1, 8.8.8.8

[Peer]
PublicKey = <服务器公钥>
Endpoint = <服务器公网IP>:51820
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
```

### 2.4 将客户端添加到服务器

在服务器 `/etc/wireguard/wg0.conf` 追加：

```ini
[Peer]
# 客户端名称
PublicKey = <客户端公钥>
AllowedIPs = 10.0.0.2/32
```

然后重启：

```bash
sudo systemctl restart wg-quick@wg0

# 验证客户端已添加
sudo wg show
# 应看到 peer 条目
```

### 2.5 一键脚本 (新机快速部署)

```bash
#!/bin/bash
# wireguard-quick-setup.sh
# 在全新的 Ubuntu 22.04 上跑这个脚本

set -e

# 安装
apt update && apt install wireguard qrencode -y

# 开启转发
sysctl -w net.ipv4.ip_forward=1
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf

# 获取主网卡
MAIN_IF=$(ip route show default | awk '{print $5}')

# 生成密钥
SERVER_PRIV=$(wg genkey)
SERVER_PUB=$(echo "$SERVER_PRIV" | wg pubkey)
CLIENT_PRIV=$(wg genkey)
CLIENT_PUB=$(echo "$CLIENT_PRIV" | wg pubkey)

# 写服务器配置
cat > /etc/wireguard/wg0.conf <<EOF
[Interface]
PrivateKey = $SERVER_PRIV
Address = 10.0.0.1/24
ListenPort = 51820
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o $MAIN_IF -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o $MAIN_IF -j MASQUERADE

[Peer]
PublicKey = $CLIENT_PUB
AllowedIPs = 10.0.0.2/32
EOF

# 生成客户端配置
SERVER_IP=$(curl -s ifconfig.me)
cat > ~/client-wg0.conf <<EOF
[Interface]
PrivateKey = $CLIENT_PRIV
Address = 10.0.0.2/24
DNS = 1.1.1.1

[Peer]
PublicKey = $SERVER_PUB
Endpoint = $SERVER_IP:51820
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
EOF

# 启动
systemctl enable --now wg-quick@wg0

# 输出结果
echo "=== 配置完成 ==="
echo "客户端配置文件: ~/client-wg0.conf"
echo ""
echo "二维码:"
qrencode -t ansiutf8 < ~/client-wg0.conf
```

---

## 3. OpenVPN 完整部署

### 3.1 使用 openvpn-install 脚本（推荐）

```bash
# Nyr 的 openvpn-install 是最成熟的社区脚本
# GitHub: https://github.com/Nyr/openvpn-install

# 下载并运行
curl -O https://raw.githubusercontent.com/Nyr/openvpn-install/master/openvpn-install.sh
chmod +x openvpn-install.sh
sudo ./openvpn-install.sh

# 交互式配置：
# 1. 确认服务器 IP
# 2. 选择协议 (UDP 推荐)
# 3. 选择端口 (1194 默认)
# 4. 选择 DNS (1.1.1.1)
# 5. 输入客户端名称
# → 自动生成 .ovpn 客户端配置文件

# 添加更多客户端：
sudo ./openvpn-install.sh
# 选择 "Add a new client"
```

### 3.2 手动安装

```bash
# Ubuntu 22.04 手动安装
sudo apt update
sudo apt install openvpn easy-rsa -y

# 1. 搭建 PKI (CA + 证书)
make-cadir ~/openvpn-ca
cd ~/openvpn-ca

# 编辑 vars (按需修改)
vim vars
# set_var EASYRSA_REQ_COUNTRY    "CN"
# set_var EASYRSA_REQ_PROVINCE   "Beijing"
# set_var EASYRSA_REQ_CITY       "Beijing"
# set_var EASYRSA_REQ_ORG        "MyOrg"
# set_var EASYRSA_REQ_EMAIL      "admin@example.com"
# set_var EASYRSA_REQ_OU         "MyOrgUnit"

./easyrsa init-pki
./easyrsa build-ca nopass           # 生成 CA

# 2. 生成服务器证书
./easyrsa gen-req server nopass
./easyrsa sign-req server server

# 3. 生成客户端证书
./easyrsa gen-req client1 nopass
./easyrsa sign-req client client1

# 4. 生成 DH 参数 (需要几分钟)
./easyrsa gen-dh

# 5. 生成 TLS 密钥 (可选的额外安全层)
openvpn --genkey secret ta.key

# 6. 复制文件
sudo cp pki/ca.crt pki/issued/server.crt pki/private/server.key pki/dh.pem ta.key /etc/openvpn/server/
```

### 3.3 服务器配置

```text
# /etc/openvpn/server/server.conf
port 1194
proto udp
dev tun

# 证书
ca ca.crt
cert server.crt
key server.key
dh dh.pem

# TLS 密钥 (可选)
tls-crypt ta.key

# 加密
cipher AES-256-GCM
auth SHA256

# 网络
server 10.8.0.0 255.255.255.0
push "redirect-gateway def1 bypass-dhcp"
push "dhcp-option DNS 1.1.1.1"
push "dhcp-option DNS 8.8.8.8"

# 持久化
persist-key
persist-tun
keepalive 10 120

# 日志
status /var/log/openvpn/openvpn-status.log
log-append /var/log/openvpn/openvpn.log
verb 3

# 性能
max-clients 50
```

```bash
# 启动
sudo systemctl enable openvpn-server@server
sudo systemctl start openvpn-server@server
```

---

## 4. 云平台部署

### 4.1 AWS

```bash
# AWS EC2 上部署 WireGuard 的注意事项

# 1. 安全组开放 UDP 51820
# AWS Console → EC2 → Security Groups → Inbound Rules:
# Type: Custom UDP, Port: 51820, Source: 0.0.0.0/0

# 2. 关闭源/目标检查 (启用 NAT/MASQUERADE 必需)
# AWS Console → EC2 → 实例 → Networking → 
# "Source/destination check" → Stop

# 3. 如果使用 AWS Site-to-Site VPN 功能
# 托管方案，无需自己搭建
# 但费用较高 (~$36/月/连接)
```

### 4.2 阿里云

```bash
# 阿里云 ECS 特别注意：

# 1. 安全组放行
# 阿里云控制台 → 安全组 → 入方向：
# 协议: UDP, 端口: 51820, 授权对象: 0.0.0.0/0

# 2. 检查 IPv4 转发
sysctl net.ipv4.ip_forward
# 如果没有开启
sysctl -w net.ipv4.ip_forward=1

# 3. 如果是国内 ECS 搭建跨境 VPN
# ⚠️ 注意合规要求，个人使用通常没问题
# 但不要用于商业 VPN 服务
```

### 4.3 Oracle Cloud 免费层

```bash
# Oracle Cloud Always Free Tier 包含：
# - 4 核 ARM Ampere A1 + 24GB 内存 (最多 4 个实例)
# - 10TB 出站流量/月
# 这是目前最好的免费 VPS 方案

# 注意：
# 1. 需要添加防火墙规则（OCI 默认屏蔽所有入站）
sudo iptables -I INPUT 6 -m state --state NEW -p udp --dport 51820 -j ACCEPT
sudo netfilter-persistent save

# 2. OCI 安全列表也要开放
# OCI Console → Networking → Virtual Cloud Networks → 
# Security Lists → 添加规则:
# Stateless: No, Source: 0.0.0.0/0, 
# IP Protocol: UDP, Destination Port: 51820
```

---

## 5. Docker 容器化方案

### 5.1 WireGuard Docker (wg-easy)

```bash
# wg-easy 是最流行的 WireGuard Docker 方案
# GitHub: https://github.com/wg-easy/wg-easy

docker run -d \
  --name=wg-easy \
  -e WG_HOST=<服务器公网IP或域名> \
  -e PASSWORD=<Web管理面板密码> \
  -e WG_DEFAULT_ADDRESS=10.8.0.x \
  -e WG_DEFAULT_DNS=1.1.1.1 \
  -e WG_ALLOWED_IPS=0.0.0.0/0 \
  -e WG_PERSISTENT_KEEPALIVE=25 \
  -v ~/.wg-easy:/etc/wireguard \
  -p 51820:51820/udp \
  -p 51821:51821/tcp \
  --cap-add=NET_ADMIN \
  --cap-add=SYS_MODULE \
  --sysctl="net.ipv4.conf.all.src_valid_mark=1" \
  --sysctl="net.ipv4.ip_forward=1" \
  --restart unless-stopped \
  ghcr.io/wg-easy/wg-easy

# 访问 http://<IP>:51821 进入 Web 管理面板
# 可以在这添加/管理客户端、生成配置、显示二维码
```

### 5.2 Docker Compose 完整配置

```yaml
# docker-compose.yml
version: '3.8'

services:
  wg-easy:
    image: ghcr.io/wg-easy/wg-easy:latest
    container_name: wg-easy
    restart: unless-stopped
    environment:
      - WG_HOST=vpn.example.com     # 改为你的域名/IP
      - PASSWORD=your-admin-pwd     # 管理面板密码
      - WG_PORT=51820
      - WG_DEFAULT_ADDRESS=10.8.0.x
      - WG_DEFAULT_DNS=1.1.1.1,8.8.8.8
      - WG_ALLOWED_IPS=0.0.0.0/0,::/0
      - WG_PERSISTENT_KEEPALIVE=25
      - WG_MTU=1420
      - UI_TRAFFIC_STATS=true       # 流量统计
      - UI_CHART_TYPE=1             # 图表类型
    ports:
      - "51820:51820/udp"
      - "51821:51821/tcp"
    volumes:
      - ./wg-data:/etc/wireguard
    cap_add:
      - NET_ADMIN
      - SYS_MODULE
    sysctls:
      - net.ipv4.conf.all.src_valid_mark=1
      - net.ipv4.ip_forward=1

# 启动
# docker compose up -d
```

---

## 6. 多客户端配置

### 6.1 WireGuard 多 Peers

```ini
# /etc/wireguard/wg0.conf
[Interface]
PrivateKey = <服务器私钥>
Address = 10.0.0.1/24
ListenPort = 51820

# === 客户端管理 ===

# 笔记本
[Peer]
# Name: mbp-work
PublicKey = <客户端A公钥>
AllowedIPs = 10.0.0.2/32

# 手机
[Peer]
# Name: iphone-personal
PublicKey = <客户端B公钥>
AllowedIPs = 10.0.0.3/32

# 家里路由器
[Peer]
# Name: home-router
PublicKey = <客户端C公钥>
AllowedIPs = 10.0.0.4/32, 192.168.1.0/24  # 可同时路由到家内网
```

### 6.2 管理脚本

```bash
#!/bin/bash
# wg-manage.sh - WireGuard 客户端管理

WG_CONF="/etc/wireguard/wg0.conf"
CLIENTS_DIR="/etc/wireguard/clients"

add_client() {
    local NAME=$1
    local CLIENT_PRIV=$(wg genkey)
    local CLIENT_PUB=$(echo "$CLIENT_PRIV" | wg pubkey)
    local CLIENT_IP="10.0.0.$(( $(grep -c '\[Peer\]' $WG_CONF) + 2 ))"
    
    # 追加 Peer 到服务器配置
    cat >> $WG_CONF <<EOF

# Client: $NAME
[Peer]
PublicKey = $CLIENT_PUB
AllowedIPs = $CLIENT_IP/32
EOF
    
    # 生成客户端配置文件
    local SERVER_PUB=$(grep -A1 '\[Interface\]' $WG_CONF | grep 'PrivateKey' | awk '{print $3}' | wg pubkey)
    local SERVER_IP=$(curl -s ifconfig.me)
    
    mkdir -p $CLIENTS_DIR
    cat > "$CLIENTS_DIR/$NAME.conf" <<EOF
[Interface]
PrivateKey = $CLIENT_PRIV
Address = $CLIENT_IP/24
DNS = 1.1.1.1

[Peer]
PublicKey = $SERVER_PUB
Endpoint = $SERVER_IP:51820
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
EOF
    
    wg syncconf wg0 <(wg-quick strip wg0)
    echo "客户端 $NAME 已添加: $CLIENTS_DIR/$NAME.conf"
    
    # 生成二维码
    qrencode -t ansiutf8 < "$CLIENTS_DIR/$NAME.conf"
}

remove_client() {
    local NAME=$1
    local KEY=$(grep -A1 "# Client: $NAME" $WG_CONF | grep 'PublicKey' | awk '{print $3}')
    
    # 从配置中删除
    sed -i "/# Client: $NAME/,/^$/d" $WG_CONF
    
    # 从 WireGuard 中移除
    wg set wg0 peer $KEY remove
    
    echo "客户端 $NAME 已移除"
}

list_clients() {
    echo "=== 已连接的客户端 ==="
    wg show wg0 | grep -A4 'peer:'
    
    echo ""
    echo "=== 配置中的客户端 ==="
    grep '# Client:' $WG_CONF
}

# 使用方式
case $1 in
    add)    add_client "$2" ;;
    remove) remove_client "$2" ;;
    list)   list_clients ;;
    *)      echo "Usage: $0 {add|remove|list} [name]" ;;
esac
```

---

## 7. 性能调优

### 7.1 影响 VPN 性能的关键因素

```
VPN 性能公式：
吞吐量 ≈ min(带宽, CPU单核频率 × 加密效率, 延迟 × TCP窗口)

瓶颈通常是：
1. CPU 加密/解密能力（高带宽场景）
2. 网络延迟（跨区域连接）
3. MTU 不当导致的分片
```

### 7.2 MTU 调优

```bash
# 测试最佳 MTU
# ping 测试（禁止分片）
ping -M do -s 1472 <VPN服务器IP>   # 1500(标准MTU) - 20(IP头) - 8(ICMP头) = 1472
ping -M do -s 1420 <VPN服务器IP>   # 尝试更小的值
# 找到不丢包的最大值 + 28 = 最佳 MTU

# WireGuard 推荐 MTU
echo "WireGuard 开销: 60 字节 (IPv4) / 80 字节 (IPv6)"
echo "推荐 MTU: 1420 (IPv4) / 1400 (IPv6)"

# 在客户端配置中设置
# WireGuard: MTU = 1420
# OpenVPN: tun-mtu 1420
```

### 7.3 内核参数优化

```bash
# /etc/sysctl.d/99-vpn-tuning.conf

# === 网络性能 ===

# 增大 TCP 缓冲区
net.core.rmem_max = 134217728
net.core.wmem_max = 134217728
net.ipv4.tcp_rmem = 4096 87380 134217728
net.ipv4.tcp_wmem = 4096 65536 134217728

# BBR 拥塞控制 (Linux 4.9+)
net.core.default_qdisc = fq
net.ipv4.tcp_congestion_control = bbr

# 开启 TCP Fast Open
net.ipv4.tcp_fastopen = 3

# === UDP 优化 (WireGuard/OpenVPN UDP 模式) ===
net.core.rmem_default = 262144
net.core.wmem_default = 262144

# === 路由转发 ===
net.ipv4.ip_forward = 1
net.ipv4.conf.all.forwarding = 1

# 应用
sysctl -p /etc/sysctl.d/99-vpn-tuning.conf
```

### 7.4 WireGuard 特定优化

```ini
# /etc/wireguard/wg0.conf 优化参数

[Interface]
# ... 基础配置 ...

# Table = auto 使用主路由表
# Table = off 可以使用策略路由实现更精细控制
Table = auto

# FwMark 用于策略路由标记
# 避免路由循环
FwMark = 0xca6c

# 保存配置到 wg0.conf (客户端使用 wg-quick 时)
SaveConfig = true
```

---

## 8. 监控与运维

### 8.1 基本状态检查

```bash
# WireGuard 状态
sudo wg show
# 输出示例：
# interface: wg0
#   public key: xxxx
#   private key: (hidden)
#   listening port: 51820
# 
# peer: yyyy
#   endpoint: 1.2.3.4:51820
#   allowed ips: 10.0.0.2/32
#   latest handshake: 15 seconds ago
#   transfer: 1.23 GiB received, 4.56 GiB sent

# 监控 last handshake 时间 —— 超过 2-3 分钟说明连接可能有问题
```

### 8.2 流量监控脚本

```bash
#!/bin/bash
# vpn-monitor.sh

watch -n 2 'echo "=== WireGuard Status ===" && \
  sudo wg show && \
  echo "" && \
  echo "=== VPN Connections ===" && \
  ss -tunp | grep -E "51820|1194" && \
  echo "" && \
  echo "=== Interface Traffic ===" && \
  ip -s link show wg0'
```

### 8.3 Prometheus + Grafana 监控

对于生产环境，推荐使用 Prometheus WireGuard Exporter：

```yaml
# docker-compose.yml (监控栈)
version: '3.8'

services:
  wireguard-exporter:
    image: mindFlavor/prometheus-wireguard-exporter:latest
    container_name: wireguard-exporter
    restart: unless-stopped
    network_mode: host
    cap_add:
      - NET_ADMIN
    volumes:
      - /etc/wireguard:/etc/wireguard:ro
    command:
      - "-n"
      - "wg0"
    # 暴露在 :9586 端口供 Prometheus 抓取
```

### 8.4 常见运维操作

```bash
# ===== WireGuard =====

# 重启 VPN (不影响已有连接)
sudo wg syncconf wg0 <(wg-quick strip wg0)

# 完全重启
sudo systemctl restart wg-quick@wg0

# 临时添加 Peer (重启后失效)
sudo wg set wg0 peer <PUBLIC_KEY> allowed-ips 10.0.0.5/32

# 删除 Peer
sudo wg set wg0 peer <PUBLIC_KEY> remove

# 查看详细日志
sudo dmesg | grep wireguard

# ===== OpenVPN =====

# 查看已连接客户端列表
sudo cat /var/log/openvpn/openvpn-status.log

# 踢掉特定客户端
echo "kill <CLIENT_COMMON_NAME>" | sudo tee /var/run/openvpn-server/status.sock

# 实时查看日志
sudo tail -f /var/log/openvpn/openvpn.log
```

> 🎯 **核心要点**：自建 VPN 投入运行后，最关键的三件事 —— 1) 定期 `wg show` 检查握手状态 2) 监控流量异常（是否有未授权的 client）3) 保持系统和 WireGuard/OpenVPN 软件更新。

---

**下一模块**：[04-VPN在开发中的应用](./04-VPN在开发中的应用.md) | **返回总览**：[00-VPN知识体系总览](./00-VPN知识体系总览.md)
