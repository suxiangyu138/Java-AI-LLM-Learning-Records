# CentOS firewalld 深度实战

> 🔥 Zone 区域体系、service/port/rich-rule 三层规则、直接规则 Direct Rules、NAT 转发与端口映射、与 nftables/iptables 底层关系

---

## 📚 目录

1. [firewalld 架构原理](#1-firewalld-架构原理)
2. [Zone 区域精讲](#2-zone-区域精讲)
3. [规则体系：Service → Port → Rich Rule → Direct](#3-规则体系)
4. [端口转发与 NAT](#4-端口转发与-nat)
5. [生产环境典型配置](#5-生产环境典型配置)

---

## 1. firewalld 架构原理

```text
CentOS 防火墙演进：
  CentOS 6 → iptables-service（手动规则，繁琐）
  CentOS 7 → firewalld（封装 iptables，zone+sercice 抽象）
  CentOS 8/9 → firewalld（底层切换到 nftables）

firewalld 内核交互：
  firewalld 守护进程
      ↓
  nftables（CentOS 8/9 默认后端）
      ↓
  netfilter（Linux 内核网络过滤框架）

firewalld vs Ubuntu ufw：
  ufw → 简单，适合开发机（sudo ufw allow 80）
  firewalld → 强大，适合服务器（zone + rich-rule + direct）
```

```bash
# 基础命令
sudo systemctl status firewalld
sudo systemctl enable --now firewalld
sudo firewall-cmd --state
sudo firewall-cmd --reload                # 重载（不中断连接！）
sudo firewall-cmd --complete-reload       # 完全重载（会中断）
```

---

## 2. Zone 区域精讲

### 2.1 Zone 信任级别

```text
Zone 信任级别（从高到低）：
  trusted   → 所有连接允许
  home      → 家庭网络（信任局域网设备）
  work      → 工作网络（信任同事设备）
  internal  → 内部网络（信任内部服务）
  public    → 公共网络（默认，不信任任何连接）
  dmz       → 隔离区（仅允许指定入站）
  block     → 拒绝（返回 icmp-host-prohibited）
  drop      → 丢弃（无任何响应，最严格）

查看：firewall-cmd --get-default-zone
```

```bash
# Zone 管理
firewall-cmd --get-zones                       # 列出所有 zone
firewall-cmd --get-active-zones                # 当前活跃 zone + 网卡绑定
firewall-cmd --list-all-zones                  # 所有 zone 详细规则
firewall-cmd --zone=internal --list-all        # 查看 internal zone

# 修改默认 zone
sudo firewall-cmd --set-default-zone=dmz

# 绑定网卡到 zone
sudo firewall-cmd --zone=internal --change-interface=eth1 --permanent

# 新建自定义 zone
sudo firewall-cmd --permanent --new-zone=appserver
sudo firewall-cmd --reload
```

---

## 3. 规则体系：Service → Port → Rich Rule → Direct

### 3.1 四层规则对比

```text
规则复杂度与灵活性：

  service（最简单）→ 预定义服务（http = 80, https = 443）
    ↓
  port（简单）→ 开放端口/协议（8080/tcp）
    ↓
  rich-rule（灵活）→ 加条件：来源IP、限频、日志
    ↓
  direct rule（最灵活）→ 直写 nftables/iptables 规则
```

### 3.2 Service

```bash
firewall-cmd --get-services                  # 列出所有预定义服务
# ssh dhcpv6-client http https mysql postgresql ...

# 自定义服务
sudo firewall-cmd --permanent --new-service=myapp
sudo firewall-cmd --permanent --service=myapp --set-short="My App"
sudo firewall-cmd --permanent --service=myapp --add-port=8080-8090/tcp
sudo firewall-cmd --reload

sudo firewall-cmd --add-service=myapp --permanent
```

### 3.3 端口管理

```bash
# 开放/删除端口
sudo firewall-cmd --add-port=8080/tcp --permanent
sudo firewall-cmd --add-port=3306/tcp --permanent
sudo firewall-cmd --add-port=6379/tcp --permanent
sudo firewall-cmd --add-port=8000-8100/tcp --permanent   # 端口范围
sudo firewall-cmd --remove-port=8080/tcp --permanent

# ===== Java 应用常用端口 =====
# Spring Boot → 8080
# Jenkins     → 8081
# MySQL       → 3306
# Redis       → 6379
# Nacos       → 8848
# Sentinel    → 8719
# RocketMQ    → 9876, 10909, 10911
```

### 3.4 Rich Rules（最强）

```bash
# Rich Rule 格式：
# rule family="ipv4|ipv6" source address="IP[/mask]" destination address="IP" ...
#   [service|port] [log] [limit] [accept|reject|drop]

# 1. 限制来源 IP
sudo firewall-cmd --permanent --add-rich-rule='
  rule family="ipv4"
  source address="192.168.1.100"
  port port="8080" protocol="tcp"
  accept'

# 2. 仅允许负载均衡器 IP 段
sudo firewall-cmd --permanent --add-rich-rule='
  rule family="ipv4"
  source address="10.0.1.0/24"
  port port="8080" protocol="tcp"
  accept'

# 3. 防 SSH 暴力破解：每分钟最多 5 个连接
sudo firewall-cmd --permanent --add-rich-rule='
  rule service name="ssh"
  limit value="5/m"
  accept'

# 4. 禁止特定 IP（带日志）
sudo firewall-cmd --permanent --add-rich-rule='
  rule family="ipv4"
  source address="10.0.0.99"
  log prefix="BLOCKED: " level="info"
  drop'

# 5. 限速：每秒最多 10 个连接
sudo firewall-cmd --permanent --add-rich-rule='
  rule family="ipv4"
  port port="8080" protocol="tcp"
  limit value="10/s"
  accept'
```

### 3.5 Direct Rules（底层直连）

```bash
# direct rule → 直接操作 nftables/iptables
# 适合 firewalld 不支持的高级场景

# 例如：允许已建立连接的返回流量（通常 firewalld 已自动处理）
sudo firewall-cmd --direct --add-rule ipv4 filter INPUT 0 \
  -m state --state ESTABLISHED,RELATED -j ACCEPT

# 查看 direct rules
sudo firewall-cmd --direct --get-all-rules
```

---

## 4. 端口转发与 NAT

```bash
# 场景 1：8080 转发到 80（低端口必须 root，但应用不应以 root 运行）
sudo firewall-cmd --permanent \
  --add-forward-port=port=80:proto=tcp:toport=8080

# 场景 2：转发到其他机器
sudo firewall-cmd --permanent \
  --add-forward-port=port=80:proto=tcp:toport=8080:toaddr=192.168.1.50

# 场景 3：开启 IP 转发（多网卡 NAT）
sudo firewall-cmd --permanent --add-masquerade

# 重载
sudo firewall-cmd --reload
```

---

## 5. 生产环境典型配置

### 5.1 Java 微服务服务器

```bash
#!/bin/bash
# 初始化微服务服务器防火墙

# 设置默认 zone 为 public
sudo firewall-cmd --set-default-zone=public

# 基础服务
sudo firewall-cmd --add-service=ssh       --permanent
sudo firewall-cmd --add-service=http      --permanent
sudo firewall-cmd --add-service=https     --permanent

# 仅负载均衡器能访问应用端口
sudo firewall-cmd --add-rich-rule='rule family="ipv4" source address="10.0.1.10/32" port port="8080" protocol="tcp" accept' --permanent

# 仅内网能访问数据库
sudo firewall-cmd --add-rich-rule='rule family="ipv4" source address="10.0.0.0/16" port port="3306" protocol="tcp" accept' --permanent
sudo firewall-cmd --add-rich-rule='rule family="ipv4" source address="10.0.0.0/16" port port="6379" protocol="tcp" accept' --permanent

# 防 SSH 暴力破解
sudo firewall-cmd --add-rich-rule='rule service name="ssh" limit value="5/m" accept' --permanent

sudo firewall-cmd --reload
sudo firewall-cmd --list-all
```

### 5.2 禁止外部访问的所有端口

```bash
# 查询当前有哪些端口对外"裸奔"
sudo firewall-cmd --list-all

# 策略：仅开放必要端口，默认全拒
sudo firewall-cmd --set-default-zone=drop
# ⚠️ 别在远程服务器上直接设为 drop！会断开 SSH
# 正确做法：先 add-service=ssh --permanent，再改 default-zone
```

### 5.3 应急开关

```bash
# 紧急：关闭防火墙（仅限极端情况）
sudo systemctl stop firewalld

# 紧急：拒绝所有转发流量（防御内网渗透）
sudo firewall-cmd --panic-on
sudo firewall-cmd --panic-off          # 恢复

# 验证当前状态
sudo firewall-cmd --query-panic
```

---

> 🎯 **原则**：默认 zone=public，按需白名单放行。数据库不对外，仅负载均衡器 IP 能接触应用端口。**Rich Rule 加 source address 限 IP** = 最安全的端口开放方式。

---

*创建于：2026年7月*
