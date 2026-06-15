# Linux 服务管理

## Crontab 定时任务

格式：`分 时 日 月 星期 command`。特殊符号：`,` 枚举, `-` 范围, `*/` 步长。

| 命令 | 说明 |
|------|------|
| `crontab -e` | 编辑任务 |
| `crontab -l` | 列出任务 |
| `systemctl status crond` | 查看 crond 状态 |

```bash
# 每天 2:30 重启 Java 应用
30 2 * * * /opt/app/restart.sh
# 每 10 分钟检查进程
*/10 * * * * pgrep java || /opt/app/start.sh
# 工作日 9 点备份
0 9 * * 1-5 /opt/scripts/backup.sh
# 每月 1 号 3 点清理日志
0 3 1 * * find /var/log/app -mtime +30 -delete
```

脚本使用绝对路径；重定向日志：`>> /var/log/cron.log 2>&1`；`%` 需转义。

---

## FTP / SFTP

```bash
yum install -y vsftpd
systemctl enable --now vsftpd
```

`/etc/vsftpd/vsftpd.conf` 关键参数：

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `anonymous_enable` | `NO` | 禁止匿名 |
| `local_enable` | `YES` | 本地用户 |
| `chroot_local_user` | `YES` | 禁锢家目录 |
| `pasv_min_port`/`pasv_max_port` | `30000 31000` | 被动端口 |

```bash
useradd -d /var/ftp/share -s /sbin/nologin ftpuser
passwd ftpuser
firewall-cmd --add-service=ftp --permanent && firewall-cmd --reload
```

| 特性 | vsftpd | SFTP |
|------|--------|------|
| 加密 | 需 TLS | 默认 SSH |
| 端口 | 21+数据 | 22 |
| 配置 | 较复杂 | 零配置 |

SFTP 基于 SSH：`sftp user@host`。

---

## NFS 文件共享

```bash
# 服务端
yum install -y nfs-utils rpcbind
systemctl enable --now rpcbind nfs-server
```

`/etc/exports`：`/data/share 192.168.1.0/24(rw,sync,no_root_squash)`

```bash
exportfs -arv   # 重载
# 客户端
yum install -y nfs-utils
showmount -e 192.168.1.100
mount -t nfs 192.168.1.100:/data/share /mnt/nfs
```

`/etc/fstab`：`192.168.1.100:/data/share /mnt/nfs nfs defaults,_netdev 0 0`

---

## Samba 文件共享

```bash
yum install -y samba samba-client
systemctl enable --now smb nmb
```

```ini
[global]
workgroup = WORKGROUP
security = user

[shared]
path = /srv/samba/shared
writable = yes
valid users = @smbgroup
create mask = 0644
```

```bash
useradd smbuser && smbpasswd -a smbuser
firewall-cmd --add-service=samba --permanent && firewall-cmd --reload
```

Windows 访问：`\\IP\shared`；Linux 挂载：`mount -t cifs //IP/shared /mnt/smb -o user=smbuser`。

---

## 邮件服务

| 协议 | 端口 | 用途 |
|------|------|------|
| SMTP | 25/465/587 | 发送 |
| POP3 | 110/995 | 接收(下载) |
| IMAP | 143/993 | 接收(保留) |

```bash
yum install -y mailx
echo "正文" | mail -s "主题" user@example.com
mail -s "日志" -a /var/log/app.log user@example.com < body.txt
```

外部 SMTP (`/etc/mail.rc`)：`set smtp=smtps://smtp.example.com:465 smtp-auth=login smtp-auth-user=u@example.com smtp-auth-password=xxx ssl-verify=ignore`

---

## systemd 服务管理

| 命令 | 说明 |
|------|------|
| `systemctl start/stop/restart NAME` | 启停 |
| `systemctl enable/disable NAME` | 开机自启 |
| `systemctl status NAME` | 状态 |
| `journalctl -u NAME -f` | 实时日志 |
| `systemctl daemon-reload` | 重载单元 |

Java 应用自启动 (`/etc/systemd/system/myapp.service`)：

```ini
[Unit]
Description=My Java App
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java -jar /opt/myapp/app.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable --now myapp
```

---

## Apache HTTP 服务器

```bash
yum install -y httpd
systemctl enable --now httpd
```

虚拟主机代理 Java 后端：

```apache
<VirtualHost *:80>
    ServerName api.example.com
    ProxyPass / http://localhost:8080/
    ProxyPassReverse / http://localhost:8080/
</VirtualHost>
```

| 特性 | Apache | Nginx |
|------|--------|-------|
| 架构 | 进程/模块 | 事件驱动 |
| 动态内容 | 原生模块 | 需反向代理 |
| 场景 | LAMP 传统栈 | 反向代理/静态资源 |

---

## 常见问题

**Q: crontab 未执行？** 检查 crond 状态、脚本权限、绝对路径。查看 `/var/log/cron`。

**Q: vsftpd 报 500 OOPS？** chroot 目录权限问题。设置 `allow_writeable_chroot=YES`。

**Q: NFS 挂载卡住？** `umount -l` 强制卸载，改用 `soft,intr` 选项。

**Q: Samba 无法访问？** 检查防火墙、SELinux (`setsebool -P samba_enable_home_dirs on`)、`valid users`。

**Q: Apache ProxyPass 503？** 确认后端 Java 进程存活、`mod_proxy` 已加载。
