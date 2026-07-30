# VPN 安全与排错

> 🔒 DNS 泄露防护、Kill Switch 机制、Split Tunneling 策略、MTU 问题诊断、日志分析 —— 让 VPN 真正安全可靠

---

## 📚 目录

1. [DNS 泄露](#1-dns-泄露)
2. [Kill Switch](#2-kill-switch)
3. [Split Tunneling](#3-split-tunneling)
4. [MTU 与性能问题](#4-mtu-与性能问题)
5. [安全加固清单](#5-安全加固清单)
6. [常见故障排查](#6-常见故障排查)
7. [日志与诊断](#7-日志与诊断)

---

## 1. DNS 泄露

### 1.1 什么是 DNS 泄露

```text
你期望的状态：
  所有 DNS 查询 → WireGuard 隧道 → VPN 服务器 DNS (1.1.1.1)
  
实际上可能发生：
  部分 DNS 查询 → 本地网络 → ISP DNS (192.168.1.1)
  ↑ 这就是 DNS 泄露

泄露意味着：
  - ISP 能看到你访问了哪些网站
  - 你本应隐藏的浏览记录被暴露
  - 你所处区域的 DNS 污染/劫持仍然生效
```

### 1.2 DNS 泄露检测

```bash
# 方法 1: 在线检测工具
# 浏览器访问: https://dnsleaktest.com
# 运行 Extended Test，查看显示的 DNS 服务器

# 方法 2: 命令行检测
# 测试你的 DNS 从哪个服务器解析
nslookup whoami.akamai.net
# 如果返回的是本地 ISP 的 DNS → 泄露

# 方法 3: 使用 dig 检查
dig +short myip.opendns.com @resolver1.opendns.com
# 对比 VPN 出口 IP 和 DNS 查询来源是否一致

# 方法 4: 检查系统 DNS 配置
# Linux/Mac
cat /etc/resolv.conf

# 可能看到多个 nameserver，排在 VPN DNS 前面的会被先使用
# 这就是泄露的根源
```

### 1.3 防止 DNS 泄露

#### WireGuard 端

```ini
# 客户端配置中强制使用 VPN DNS
[Interface]
PrivateKey = ...
Address = 10.0.0.2/24
DNS = 1.1.1.1, 8.8.8.8    # WireGuard 自动配置 DNS

# 关键：wg-quick 会通过 systemd-resolved 或 resolvconf
# 确保只有这些 DNS 被使用
```

#### 系统级防护

```bash
# Linux: 锁定 /etc/resolv.conf
# 方法 1: 使用 systemd-resolved
sudo systemctl enable systemd-resolved
sudo systemctl start systemd-resolved

# 配置 WireGuard 使用 systemd-resolved
# 在 wg0.conf 中：
# DNS = 1.1.1.1
# PostUp = resolvectl dns %i 1.1.1.1
# PostUp = resolvectl domain %i ~.

# 方法 2: 防止 DHCP 覆盖
sudo chattr +i /etc/resolv.conf  # 锁定文件
# (连接 VPN 前设置好 DNS，然后锁定)

# 方法 3: 防火墙规则强制所有 DNS 走 VPN
# 阻止 UDP 53 端口从物理网卡出去
sudo iptables -A OUTPUT -o eth0 -p udp --dport 53 -j DROP
sudo iptables -A OUTPUT -o eth0 -p tcp --dport 53 -j DROP
# 只允许通过 wg0 的 DNS
sudo iptables -A OUTPUT -o wg0 -p udp --dport 53 -j ACCEPT
```

#### 验证脚本

```bash
#!/bin/bash
# dns-leak-check.sh

echo "=== DNS 泄露检测 ==="
echo ""

echo "当前系统 DNS 配置："
cat /etc/resolv.conf | grep -v '^#' | grep -v '^$'
echo ""

echo "DNS 查询来源检测："
echo "OpenDNS 看到的 DNS 来源："
dig +short myip.opendns.com @resolver1.opendns.com

echo "Google DNS 看到的来源："
dig +short whoami.akamai.net @8.8.8.8

echo ""
echo "检测 DNS 服务器是否统一："
DNS_SERVERS=$(dig +short google.com | head -1)
echo "google.com 解析到: $DNS_SERVERS"
echo ""

# 实际发出 DNS 查询的出口
echo "当前默认 DNS 查询路径："
dig +short resolver.dnscrypt.info | head -5
echo ""
echo "如果看到本地 ISP 的 DNS 服务器 = DNS 泄露"
```

---

## 2. Kill Switch

### 2.1 什么是 Kill Switch

```text
正常状态：
  你的流量 → WireGuard 隧道 → VPN 服务器 → 互联网

VPN 断开时：
  ❌ 没有 Kill Switch → 你的流量 → 直接走本地网络 → ISP 看到一切
  ✅ 有 Kill Switch   → 你的流量 → 被防火墙拦截 → 无网络 = 无泄露
```

### 2.2 WireGuard Kill Switch 实现

#### 方法 1：iptables 防火墙规则

```bash
#!/bin/bash
# wireguard-killswitch.sh

# 只在 wg0 存在时允许流量出去
# VPN 断开时所有出站流量被 DROP

setup_killswitch() {
    # 允许本地回环
    sudo iptables -A OUTPUT -o lo -j ACCEPT
    
    # 允许 WireGuard 本身的流量到服务器
    sudo iptables -A OUTPUT -o eth0 -p udp --dport 51820 \
        -d <VPN服务器IP> -j ACCEPT
    
    # 允许 VPN 内网流量
    sudo iptables -A OUTPUT -o wg0 -j ACCEPT
    
    # 拒绝所有其他出站流量
    sudo iptables -A OUTPUT -j REJECT
}

remove_killswitch() {
    sudo iptables -F OUTPUT
}

# 使用
# VPN 连接时启用
setup_killswitch
sudo wg-quick up wg0

# VPN 断开时先移除规则
remove_killswitch
```

#### 方法 2：PostUp/PostDown 集成

```ini
# WireGuard 配置文件中直接集成 Kill Switch
[Interface]
PrivateKey = ...
Address = 10.0.0.2/24
DNS = 1.1.1.1

# Kill Switch: 启动时添加规则
PostUp = iptables -I OUTPUT ! -o wg0 -m mark ! --mark $(wg show %i fwmark) -m addrtype ! --dst-type LOCAL -j REJECT

# Kill Switch: 停止时移除规则
PostDown = iptables -D OUTPUT ! -o wg0 -m mark ! --mark $(wg show %i fwmark) -m addrtype ! --dst-type LOCAL -j REJECT
```

#### 方法 3：网络命名空间隔离（最安全）

```bash
# 创建独立的网络命名空间，只有 wg0 接口
# 需要 VPN 的应用在这个命名空间中运行

# 创建命名空间
sudo ip netns add vpn-ns

# 把 wg0 移入命名空间
sudo ip link set wg0 netns vpn-ns

# 在命名空间中配置
sudo ip netns exec vpn-ns ip addr add 10.0.0.2/24 dev wg0
sudo ip netns exec vpn-ns ip link set wg0 up
sudo ip netns exec vpn-ns ip route add default dev wg0

# 在命名空间中运行应用（这个应用完全被隔离）
sudo ip netns exec vpn-ns sudo -u $USER firefox
# VPN 断开 = Firefox 完全无法联网 = 100% 防泄露
```

### 2.3 Kill Switch 测试

```bash
# 1. 连接 VPN 并启用 Kill Switch
# 2. 打开网站确认网络正常
# 3. 手动断开 VPN: sudo wg-quick down wg0
# 4. 尝试访问任何网站 → 应该全部失败
# 5. 关闭 Kill Switch → 网络恢复

# 验证脚本
#!/bin/bash
echo "VPN 连接时 IP: $(curl -s ifconfig.me)"
echo "手动断开 VPN..."
sudo wg-quick down wg0
echo "断开后 IP: $(timeout 5 curl -s ifconfig.me || echo '无法连接 ✅ Kill Switch 生效')"
```

---

## 3. Split Tunneling

### 3.1 什么是 Split Tunneling

```text
Full Tunnel (全隧道):                Split Tunnel (分割隧道):
所有流量 → VPN                       内网流量 → VPN
                                    │
┌──────────┐   ┌──────────┐         ┌──────────┐   ┌──────────┐
│ YouTube  ├──→│   VPN    │         │ YouTube  ├──→│  本地ISP  │ ← 直接访问，延迟低
│ GitHub   │   │  Server  │         │ GitHub   │   │          │
│ 公司内网  │   │          │         └──────────┘   └──────────┘
└──────────┘   └────┬─────┘         
                    │                 ┌──────────┐   ┌──────────┐
                    ▼                 │ 公司内网  ├──→│   VPN    │ ← 安全隧道
               互联网                 │ 数据库    │   │  Server  │
                                    └──────────┘   └──────────┘
```

### 3.2 WireGuard Split Tunneling 配置

```ini
# 只将特定网段路由到 VPN，其余走本地
# 客户端 wg0.conf

[Interface]
PrivateKey = ...
Address = 10.0.0.2/24
# 不配置全局 DNS，使用系统默认

[Peer]
PublicKey = <服务器公钥>
Endpoint = <VPN服务器IP>:51820

# 关键：只路由公司内网网段
AllowedIPs = 10.1.0.0/16, 10.2.0.0/16, 172.16.0.0/12
# 而不是 0.0.0.0/0 (全隧道)

# 这意味着：
# ✅ 10.1.x.x, 10.2.x.x, 172.16.x.x → 走 VPN
# ✅ 其余所有 IP → 走本地网络
```

### 3.3 高级 Split Tunneling（策略路由）

```bash
# 场景：普通上网走本地，特定应用走 VPN

# 1. 创建自定义路由表
echo "200 vpn" | sudo tee -a /etc/iproute2/rt_tables

# 2. 为 wg0 配置策略路由
sudo ip rule add fwmark 0xca6c table vpn
sudo ip route add default dev wg0 table vpn

# 3. 用 cgroup 或 iptables 标记特定应用的流量
# 方法 A: 使用 iptables 按 UID 标记
sudo iptables -t mangle -A OUTPUT -m owner --uid-owner vpnuser \
    -j MARK --set-mark 0xca6c

# 方法 B: 使用网络命名空间
# 把特定应用放入 vpn-ns 命名空间运行

# 方法 C: 使用代理 + 路由
# 特定应用配置 SOCKS5 代理，代理出口选 VPN 网卡
```

### 3.4 Split Tunneling 适用场景

| 全隧道 (0.0.0.0/0) | Split Tunneling |
|---------------------|-----------------|
| 隐私优先（所有流量加密） | 性能优先（内网走 VPN，外网直连） |
| 公共 WiFi 使用 | 家庭/办公室使用 |
| 绕过地区限制 | 只访问公司内网 |
| 防止 ISP 监控 | 减少 VPN 服务器带宽压力 |
| VPN 出口带宽充足 | VPN 出口带宽有限 |

---

## 4. MTU 与性能问题

### 4.1 VPN 对 MTU 的影响

```text
标准以太网 MTU: 1500 字节

WireGuard 封装开销：
  外层 IP 头:   20 字节 (IPv4) / 40 字节 (IPv6)
  外层 UDP 头:   8 字节
  WireGuard 头: 16 字节
  Auth Tag:     16 字节
  ──────────────────────────
  总开销:       60 字节 (IPv4) / 80 字节 (IPv6)

有效 MTU = 1500 - 60 = 1440 (IPv4)
         1500 - 80 = 1420 (IPv6)

问题：
  如果你的路径中某段 MTU 更小（如 PPPoE 1492）
  → 1440 的包可能还是超过 1492 - 60 = 1432
  → 产生分片或丢包
```

### 4.2 MTU 问题症状

| 症状 | 可能原因 |
|------|---------|
| 某些网站打不开/加载一半 | MTU 过大导致丢包 |
| SSH 连接卡死（能连但输入不了） | MTU 分片导致部分 TCP 包丢失 |
| HTTP 正常但 HTTPS 超时 | TLS 握手包较大，被分片后丢包 |
| 大文件下载失败 | 大数据包被丢弃但小包正常 |

### 4.3 MTU 诊断与修复

```bash
# 1. 找到路径 MTU (Path MTU Discovery)
ping -M do -s 1472 8.8.8.8          # 1500 - 28 (ICMP头) = 1472
# 如果丢包，逐步减小
ping -M do -s 1452 8.8.8.8
ping -M do -s 1432 8.8.8.8
ping -M do -s 1412 8.8.8.8
# 找到不丢包的最大值

# 2. 计算最佳 MTU: ping 值 + 28
# 例如 1412 + 28 = 1440

# 3. 在 VPN 配置中设置
# WireGuard
# wg0.conf [Interface] 下添加：
# MTU = 1420   (保守值，适合大多数场景)

# OpenVPN
# server.conf:
# tun-mtu 1400
# mssfix 1360
```

### 4.4 MSS Clamping（TCP 层面的修复）

```bash
# MTU 问题通常只影响 TCP，可以用 MSS Clamping 解决
# MSS (Maximum Segment Size) = MTU - 40 (IP + TCP 头)

# 在 VPN 服务器上加 iptables 规则
# 自动将 TCP SYN 中的 MSS 调整为合适值
sudo iptables -t mangle -A FORWARD -p tcp \
    --tcp-flags SYN,RST SYN \
    -j TCPMSS --clamp-mss-to-pmtu

# 或指定固定 MSS
sudo iptables -t mangle -A FORWARD -p tcp \
    --tcp-flags SYN,RST SYN \
    -j TCPMSS --set-mss 1360

# WireGuard 可以在 PostUp 中添加
PostUp = iptables -t mangle -A FORWARD -p tcp --tcp-flags SYN,RST SYN -j TCPMSS --clamp-mss-to-pmtu
PostDown = iptables -t mangle -D FORWARD -p tcp --tcp-flags SYN,RST SYN -j TCPMSS --clamp-mss-to-pmtu
```

---

## 5. 安全加固清单

### 5.1 WireGuard 安全清单

```text
✅ 密钥管理
   □ 服务器私钥权限：chmod 600 /etc/wireguard/server_private.key
   □ 客户端私钥绝不外传
   □ 定期轮换密钥（建议 3-6 个月）
   □ 已失效的设备密钥及时删除

✅ 访问控制
   □ AllowedIPs 精确控制（不用 0.0.0.0/0 当 AllowedIPs 用于对端限制）
   □ 每个客户端独立 Peer（不共享密钥）
   □ 使用最小权限：客户端只能访问必要的 IP

✅ 网络层安全
   □ DNS 泄露已防护
   □ Kill Switch 已配置
   □ 防火墙只开放必要端口
   □ UFW/iptables 限制非 VPN 流量

✅ 系统安全
   □ 系统定期更新（unattended-upgrades）
   □ SSH 禁用密码登录，仅用密钥
   □ Fail2ban 配置 SSH 和 VPN 端口
   □ 关闭不必要的服务

✅ 监控
   □ 定期检查 wg show 输出（自动化脚本）
   □ 流量异常告警（突然增大或从未知 Peer 收发包）
   □ 日志定期检查
```

### 5.2 Fail2ban 保护 VPN

```ini
# /etc/fail2ban/jail.d/wireguard.conf
[wireguard]
enabled  = true
port     = 51820
protocol = udp
filter   = wireguard
logpath  = /var/log/syslog
maxretry = 3
bantime  = 3600
findtime = 600
```

```ini
# /etc/fail2ban/filter.d/wireguard.conf
[Definition]
failregex = ^.*wg0:.*Invalid handshake initiation from <HOST>.*$
ignoreregex =
```

### 5.3 OpenVPN 安全加固

```text
# server.conf 安全相关配置

# 1. 降权运行
user nobody
group nogroup

# 2. TLS 安全
tls-version-min 1.2
tls-cipher TLS-ECDHE-ECDSA-WITH-AES-256-GCM-SHA384:TLS-ECDHE-RSA-WITH-AES-256-GCM-SHA384

# 3. 加密
cipher AES-256-GCM
auth SHA256

# 4. TLS 密钥 (防 DoS 握手攻击)
tls-crypt ta.key   # 比 tls-auth 更好，同时加密控制通道

# 5. 限制重协商
reneg-sec 3600     # 至少 1 小时才重协商

# 6. 不推荐压缩 (CRIME 攻击)
# 不要开启 compression

# 7. 持久化
persist-key
persist-tun
```

---

## 6. 常见故障排查

### 6.1 WireGuard 故障速查表

| 症状 | 诊断命令 | 常见原因 | 解决方法 |
|------|---------|---------|---------|
| `wg show` 显示不了 peer | `sudo wg show` | 配置未加载 | `sudo wg-quick up wg0` |
| `latest handshake` 不更新 | `sudo dmesg \| grep wireguard` | 端口未开放/被墙 | 检查防火墙、换端口 |
| 握手成功但 ping 不通 | `ping 10.0.0.1` | AllowedIPs 不匹配 | 两端检查 AllowedIPs |
| 能 ping 但不能上网 | `curl -v ifconfig.me` | IP 转发未开启 | `sysctl net.ipv4.ip_forward=1` |
| 部分网站打不开 | `ping -M do -s 1472 google.com` | MTU 问题 | 降低 MTU 到 1420 |
| DNS 不工作 | `nslookup google.com` | DNS 未配置/被覆盖 | 检查 DNS 设置 |

### 6.2 系统性排查流程

```text
第 1 层: 接口层
  □ wg0 是否存在？       ip link show wg0
  □ 配置是否正确加载？    sudo wg show
  ↓

第 2 层: 连接层
  □ peer 是否在线？      sudo wg show | grep handshake
  □ 握手是否完成？        handshake 时间 < 2 分钟
  □ 数据传输是否正常？    transfer 值在增长
  ↓

第 3 层: 路由层
  □ VPN IP 可通？        ping <VPN Server 内网 IP>
  □ 目标 IP 可通？       ping <目标 IP>
  □ 路由表正确？         ip route | grep wg0
  ↓

第 4 层: 转发层 (服务端)
  □ IP 转发开启？        sysctl net.ipv4.ip_forward (应 =1)
  □ NAT 规则正确？       sudo iptables -t nat -L POSTROUTING
  □ 防火墙放行？         sudo iptables -L FORWARD
  ↓

第 5 层: DNS 层
  □ DNS 配置正确？       cat /etc/resolv.conf
  □ DNS 查询走 VPN？     检查是否有 DNS 泄露
  ↓

第 6 层: MTU 层
  □ 大包能通过？         ping -M do -s 1400 <目标>
  □ MSS Clamping 生效？  检查 iptables mangle 表
```

### 6.3 排查脚本

```bash
#!/bin/bash
# vpn-debug.sh - WireGuard 系统性诊断

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

check() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $1${NC}"
        return 0
    else
        echo -e "${RED}❌ $1${NC}"
        return 1
    fi
}

echo "=== WireGuard 诊断报告 ==="
echo "时间: $(date)"
echo ""

# L1: 接口
echo "--- 接口层 ---"
ip link show wg0 &>/dev/null
check "wg0 接口存在"

# L2: 配置
echo "--- 配置层 ---"
sudo wg show wg0 &>/dev/null
check "WireGuard 配置已加载"

PEER_COUNT=$(sudo wg show wg0 | grep -c 'peer:')
echo "  Peers 数量: $PEER_COUNT"

LAST_HANDSHAKE=$(sudo wg show wg0 | grep 'latest handshake' | awk '{print $3, $4, $5, $6}')
if [ -n "$LAST_HANDSHAKE" ]; then
    echo -e "${GREEN}  最后握手: $LAST_HANDSHAKE${NC}"
else
    echo -e "${RED}  无握手记录${NC}"
fi

# L3: 路由
echo "--- 路由层 ---"
ping -c 1 -W 2 10.0.0.1 &>/dev/null
check "VPN 网关可达 (10.0.0.1)"

# 默认路由
DEFAULT_ROUTE=$(ip route show default)
echo "  默认路由: $DEFAULT_ROUTE"

# L4: 转发
echo "--- 转发层 ---"
IP_FORWARD=$(sysctl -n net.ipv4.ip_forward)
if [ "$IP_FORWARD" = "1" ]; then
    echo -e "${GREEN}✅ IP 转发已开启${NC}"
else
    echo -e "${YELLOW}⚠️  IP 转发未开启 (如果是客户端则正常)${NC}"
fi

# L5: DNS
echo "--- DNS 层 ---"
echo "  DNS 配置:"
grep 'nameserver' /etc/resolv.conf | while read line; do
    echo "    $line"
done

# 外网连通性测试
echo "--- 连通性测试 ---"
curl -s -o /dev/null -w "  HTTP 状态: %{http_code}, 时间: %{time_total}s\n" --connect-timeout 5 https://www.google.com 2>/dev/null
check "HTTPS 访问正常"

# MTU 测试
echo "--- MTU 测试 ---"
ping -c 2 -M do -s 1400 -W 2 10.0.0.1 &>/dev/null
check "1400 字节大包通过"
```

---

## 7. 日志与诊断

### 7.1 WireGuard 日志

```bash
# WireGuard 使用内核日志
# 实时查看
sudo dmesg -w | grep wireguard

# WireGuard 的日志级别可通过内核模块参数控制
# 默认情况下错误才记日志，正常操作不记
# 开启调试日志 (不推荐长期开启)：
echo module wireguard +p | sudo tee /sys/kernel/debug/dynamic_debug/control

# 查看系统日志中的 WireGuard
sudo journalctl -u wg-quick@wg0 -f
sudo journalctl -u wg-quick@wg0 --since "10 minutes ago"
```

### 7.2 OpenVPN 日志

```bash
# OpenVPN 日志配置
# /etc/openvpn/server/server.conf
# verb 3  (0=静默, 3=正常, 6=调试)

# 连接日志
sudo tail -f /var/log/openvpn/openvpn.log

# 状态文件 (实时连接的客户端)
sudo cat /var/log/openvpn/openvpn-status.log
# 输出示例:
# OpenVPN CLIENT LIST
# Updated,Thu Jul 30 10:15:00 2026
# Common Name,Real Address,Bytes Received,Bytes Sent,Connected Since
# alice-laptop,123.45.67.89:54321,12345678,98765432,Thu Jul 30 08:00:00 2026
```

### 7.3 tcpdump 抓包分析

```bash
# WireGuard 协议抓包
sudo tcpdump -i eth0 udp port 51820 -n -X

# 验证 VPN 流量确实加密了
sudo tcpdump -i eth0 udp port 51820 -A
# 应该看到全是乱码

# 在 wg0 接口抓包（可以看到明文，因为已经解密了）
sudo tcpdump -i wg0 -n

# 分析握手
sudo tcpdump -i eth0 'udp port 51820 and (udp[8:4] = 0x00000001 or udp[8:4] = 0x00000002)'
# WireGuard 消息类型: 1=Handshake Initiation, 2=Handshake Response
```

### 7.4 性能诊断

```bash
# 带宽测试 (需要服务端运行 iperf3)
# 服务端：
iperf3 -s

# 客户端 (通过 VPN 内网 IP 测试):
iperf3 -c 10.0.0.1 -t 30 -P 4
# -t 30: 测试 30 秒
# -P 4: 4 个并行流

# 对比测试：VPN 内网 vs 公网直连
# 1. 通过 VPN
iperf3 -c 10.0.0.1 -t 10

# 2. 直连公网（绕过 VPN）
iperf3 -c <VPN服务器公网IP> -t 10

# 两者的差距 = VPN 加密开销
# WireGuard 通常只损失 5-10%
# OpenVPN 可能损失 15-30%
```

> 🎯 **核心要点**：VPN 安全的三个基石 —— **DNS 防泄露**（别人不知道你去哪）、**Kill Switch**（断开也不暴露）、**MTU 正确**（不丢包性能好）。排查问题时从上到下（接口→连接→路由→转发→DNS→MTU）层层排除，不跳步。

---

**返回总览**：[00-VPN知识体系总览](./00-VPN知识体系总览.md) | **上一篇**：[04-VPN在开发中的应用](./04-VPN在开发中的应用.md)
