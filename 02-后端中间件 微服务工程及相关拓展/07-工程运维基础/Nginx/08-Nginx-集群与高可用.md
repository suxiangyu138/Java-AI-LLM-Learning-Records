# 08 - Nginx 集群与高可用

> 🎯 单台 Nginx 是单点故障的定时炸弹 — Keepalived + VRRP 实现主备自动切换、Ansible 批量管理多节点配置

---

## 目录

1. [为什么需要 Nginx 集群](#一为什么需要-nginx-集群)
2. [集群架构](#二集群架构)
3. [Keepalived 安装与配置](#三keepalived-安装与配置)
4. [主备切换验证](#四主备切换验证)
5. [Ansible 批量配置管理](#五ansible-批量配置管理)

---

## 一、为什么需要 Nginx 集群

单台 Nginx 存在两大风险：

| 风险 | 说明 |
|------|------|
| **性能瓶颈** | 单机并发有上限，无法承载海量请求 |
| **单点故障** | Nginx 宕机 → 整个服务中断 |

集群方案：**多台 Nginx + Keepalived（高可用）+ Ansible（配置管理）**。

## 二、集群架构

```
客户端 → 虚拟 IP（VIP）→ Keepalived
           ├── Nginx 主节点（active）
           └── Nginx 备用节点（standby）
                    ↓
              后端应用集群
```

## 三、Keepalived 高可用配置

### 3.1 安装

```bash
yum install -y keepalived
```

### 3.2 主节点配置

```nginx
# /etc/keepalived/keepalived.conf
! Configuration File for keepalived

global_defs {
   router_id NGINX_MASTER
}

# Nginx 存活检测脚本
vrrp_script check_nginx {
    script "/etc/keepalived/check_nginx.sh"
    interval 2           # 每 2 秒检测
    weight -20           # 失败则优先级 -20
}

vrrp_instance VI_1 {
    state MASTER                     # 主节点
    interface eth0                  # 网卡名称
    virtual_router_id 51            # 主备一致
    priority 100                    # 优先级（高于备节点）
    advert_int 1                    # 心跳间隔

    authentication {
        auth_type PASS
        auth_pass 1111              # 主备一致
    }

    virtual_ipaddress {
        192.168.1.200               # 虚拟 IP（VIP）
    }

    track_script {
        check_nginx
    }
}
```

### 3.3 备用节点配置

与主节点基本相同，仅修改：

```nginx
global_defs {
   router_id NGINX_BACKUP
}

vrrp_instance VI_1 {
    state BACKUP         # 备用节点
    priority 90          # 优先级低于主节点
    # 其余与主节点一致
}
```

### 3.4 Nginx 检测脚本

```bash
#!/bin/bash
# /etc/keepalived/check_nginx.sh

if [ $(ps -ef | grep nginx | grep -v grep | wc -l) -eq 0 ]; then
    systemctl restart nginx
    sleep 3
    if [ $(ps -ef | grep nginx | grep -v grep | wc -l) -eq 0 ]; then
        systemctl stop keepalived
    fi
fi
```

```bash
chmod +x /etc/keepalived/check_nginx.sh
systemctl start keepalived
systemctl enable keepalived
```

### 3.5 验证主备切换

```bash
# 查看 VIP 绑定（主节点应显示 VIP）
ip addr | grep 192.168.1.200

# 模拟主节点故障
systemctl stop nginx
# 3 秒后 VIP 自动漂移到备用节点

# 恢复主节点
systemctl start nginx && systemctl start keepalived
# VIP 自动切回主节点
```

## 四、Ansible 统一配置管理

### 4.1 安装 Ansible（配置管理节点）

```bash
yum install -y ansible

# 配置免密登录
ssh-keygen -t rsa
ssh-copy-id root@192.168.1.100    # 主节点
ssh-copy-id root@192.168.1.101    # 备节点
```

### 4.2 主机清单

```ini
# /etc/ansible/hosts
[nginx_cluster]
192.168.1.100 ansible_ssh_user=root
192.168.1.101 ansible_ssh_user=root
```

### 4.3 配置推送 Playbook

```yaml
# /etc/ansible/nginx_playbook/nginx_config.yml
- name: 推送 Nginx 集群配置
  hosts: nginx_cluster
  remote_user: root
  tasks:
    - name: 推送 nginx.conf
      template:
        src: templates/nginx.conf
        dest: /etc/nginx/nginx.conf
        mode: 0644
        backup: yes             # 自动备份，便于回滚

    - name: 检查配置语法
      command: nginx -t
      register: check_result

    - name: 重载 Nginx
      systemctl:
        name: nginx
        state: reloaded
      when: check_result.rc == 0

    - name: 重启 Keepalived
      systemctl:
        name: keepalived
        state: restarted
```

```bash
# 一键推送
ansible-playbook /etc/ansible/nginx_playbook/nginx_config.yml
```

## 五、集群日常运维

### 5.1 状态检查

```bash
# 批量查看服务状态
ansible nginx_cluster -m systemctl -a "status nginx"
ansible nginx_cluster -m systemctl -a "status keepalived"

# 查看 VIP 绑定
ansible nginx_cluster -m command -a "ip addr | grep 192.168.1.200"

# 查看请求分发
ansible nginx_cluster -m command -a "grep -o '192.168.1.[0-9]*:8080' /var/log/nginx/access.log | sort | uniq -c | sort -nr"
```

### 5.2 集群扩容

```bash
# 1. 新节点安装 Nginx + Keepalived
# 2. 加入 Ansible 主机清单
echo "192.168.1.105 ansible_ssh_user=root" >> /etc/ansible/hosts

# 3. 推送配置
ansible-playbook nginx_config.yml --limit 192.168.1.105

# 4. Keepalived 设为备用节点（优先级低于现备节点）
```

### 5.3 集群缩容

```bash
# 1. 从 Keepalived 中移除节点
# 2. 停止服务
systemctl stop nginx keepalived
# 3. 从 Ansible 主机清单移除
```

### 5.4 配置回滚

```bash
# Playbook 自动备份，恢复备份即可
ansible nginx_cluster -m command -a "cp /etc/nginx/nginx.conf.bak /etc/nginx/nginx.conf"
ansible nginx_cluster -m systemctl -a "name=nginx state=reloaded"
```

## 六、安全要点

| 措施 | 配置 |
|------|------|
| 防火墙 | 仅开放 80/443 + 监控端口 |
| 监控接口 | `allow` 限制内网 IP |
| 隐藏版本 | `server_tokens off;` |
| 日志权限 | `chmod 600` 仅 nginx 用户可读 |
| 定期更新 | 及时升级修复安全漏洞 |

## 七、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 主备未切换 | Keepalived 未启动 / 配置不一致 | 检查状态 + 对比主备配置 |
| 负载不均 | 策略配置问题 | 检查 upstream 策略和权重 |
| 配置推送失败 | 免密登录/网络不通 | 检查 SSH 和网络连通性 |
| 后端节点未自动剔除 | 健康检查未配置 | 启用主动健康检查 |
| VIP 无法访问 | VIP 配置/网卡绑定错误 | `ip addr` 查看 VIP 状态 |
