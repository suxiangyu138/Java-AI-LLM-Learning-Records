# Redis 快速安装指南

> 覆盖 Linux（Ubuntu/CentOS）、macOS、Windows（WSL2）、Docker 全平台，5 分钟完成安装验证。

---

## 各平台安装

### Linux（Ubuntu/Debian）

```bash
sudo apt update && sudo apt install redis-server -y
sudo systemctl enable redis-server && sudo systemctl start redis-server
```

### Linux（CentOS/RHEL）

```bash
sudo yum install epel-release -y
sudo yum install redis -y
sudo systemctl start redis && sudo systemctl enable redis
```

### macOS（Homebrew）

```bash
brew install redis
brew services start redis
```

### Windows（WSL2 推荐）

```powershell
# 管理员 PowerShell 启用 WSL2
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
wsl --set-default-version 2
# Microsoft Store 安装 Ubuntu → 按 Linux 步骤安装
```

### Docker（跨平台，最便捷）

```bash
docker run --name redis -p 6379:6379 -d redis:latest
# 带密码
docker run --name redis -p 6379:6379 -d redis:latest --requirepass "密码"
```

---

## 验证

```bash
redis-cli          # Docker: docker exec -it redis redis-cli
> ping             # PONG → 正常
> set test "ok"    # 测试写入
> get test         # 测试读取
```

---

## 卸载

| 平台 | 命令 |
|------|------|
| Ubuntu | `sudo apt remove redis-server -y` |
| CentOS | `sudo yum remove redis -y` |
| macOS | `brew uninstall redis` |
| Docker | `docker stop redis && docker rm redis` |

---

## 注意事项

| 注意点 | 说明 |
|--------|------|
| Linux 默认绑定 `127.0.0.1` | 远程访问需修改 `redis.conf` |
| Windows 原生版已停维护 | 生产建议 Linux/Docker |
| Docker 容器停止即终止 | 加 `--restart=always` 自动重启 |
