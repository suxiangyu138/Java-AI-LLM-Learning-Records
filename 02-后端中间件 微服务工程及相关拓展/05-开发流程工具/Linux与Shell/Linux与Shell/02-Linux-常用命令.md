# Linux 常用命令

> 面向 Java 后端开发的 Linux 命令速查手册，涵盖文件管理、文本处理、权限控制、进程管理、磁盘网络、打包压缩及实战场景。

---

## 目录

- [文件与目录操作](#文件与目录操作)
- [文件查看与搜索](#文件查看与搜索)
- [文本处理](#文本处理)
- [用户与权限](#用户与权限)
- [进程管理](#进程管理)
- [磁盘与存储](#磁盘与存储)
- [网络操作](#网络操作)
- [打包与压缩](#打包与压缩)
- [后端开发场景](#后端开发场景)
- [常见问题](#常见问题)

---

## 文件与目录操作

| 命令 | 说明 | 常用示例 |
|------|------|----------|
| `ls` | 列出目录内容 | `ls -lah` 显示隐藏文件及大小 |
| `cd` | 切换目录 | `cd ~` 回 home；`cd -` 回上次目录 |
| `pwd` | 当前绝对路径 | `pwd` |
| `mkdir` | 创建目录 | `mkdir -p a/b/c` 递归创建 |
| `touch` | 创建空文件 / 更新 mtime | `touch app.log` |
| `cp` | 复制 | `cp -r src/ dst/` 递归复制 |
| `mv` | 移动 / 重命名 | `mv old.txt new.txt` |
| `rm` | 删除 | `rm -rf dir/` 强制递归删除 |

### 目录树结构速览

```bash
# 以树状查看两层目录
tree -L 2 /app
```

---

## 文件查看与搜索

### 内容查看

```bash
cat app.log          # 全文输出
head -n 50 app.log   # 前 50 行
tail -n 100 app.log  # 后 100 行
tail -f app.log      # 实时追踪（Java 日志常用）
less app.log         # 分页浏览（/ 搜索，q 退出）
```

### 文件查找

```bash
find /app -name "*.log"                     # 按名字查找
find /app -mtime -1                         # 最近 1 天修改的文件
find /app -type f -size +100M               # 大于 100MB 的文件
find /app -name "*.class" -exec rm {} \;    # 找到后执行操作
```

### 内容搜索

```bash
grep "ERROR" app.log                        # 基本搜索
grep -r "NullPointerException" /app/src     # 递归搜索
grep -rn "Config" --include="*.java" .      # 仅搜 Java 文件
grep -v "^#" /etc/nginx/nginx.conf          # 排除注释行
```

**grep 常见参数**

| 参数 | 作用 |
|------|------|
| `-i` | 忽略大小写 |
| `-n` | 显示行号 |
| `-c` | 统计匹配行数 |
| `-A 5` | 匹配行后 5 行 |
| `-B 5` | 匹配行前 5 行 |
| `-C 5` | 匹配行前后各 5 行 |

---

## 用户与权限

### 权限位说明

```bash
# 权限示例：rwxr-xr--
# 所有者(u) 同组(g) 其他(o)
# r=4, w=2, x=1
```

### 常用命令

```bash
chmod 755 script.sh                 # rwxr-xr-x
chmod +x gradlew                    # 加执行权限
chown -R admin:admin /app/data/     # 递归改所有者:组
chmod -R g+w /app/logs/             # 组追加写权限
umask 022                           # 设置默认权限掩码
```

| 命令 | 说明 |
|------|------|
| `chmod` | 修改文件权限 |
| `chown` | 修改文件所有者 |
| `whoami` | 当前用户名 |
| `id` | 用户 ID 及所属组 |
| `sudo` | 以 root 执行 |
| `useradd` | 创建用户 |
| `passwd` | 修改密码 |

---

## 进程管理

```bash
ps -ef                 # 所有进程快照
ps aux                 # BSD 风格（CPU/内存）
ps -ef | grep java     # 查找 Java 进程

top                    # 实时进程监控
top -p 1234            # 监控指定 PID
htop                   # 增强版 top（需安装）

kill -9 1234           # 强制终止（SIGKILL）
kill -15 1234          # 正常终止（SIGTERM）
pkill -f "java -jar"   # 按名称模式杀进程

# 查看进程监听端口
lsof -i :8080
ss -tlnp | grep 8080
```

**进程状态**

| 状态 | 含义 |
|------|------|
| R | 运行 / 可运行 |
| S | 可中断休眠 |
| D | 不可中断休眠（磁盘 IO） |
| Z | 僵尸进程 |
| T | 已停止 |

---

## 磁盘与存储

```bash
df -h                 # 分区使用情况（人类可读）
du -sh /app/          # 目录总大小
du -h --max-depth=1   # 一级子目录大小
ls -lh                # 查看文件大小
```

| 命令 | 说明 |
|------|------|
| `df` | 文件系统磁盘空间 |
| `du` | 目录 / 文件占用空间 |
| `fdisk -l` | 分区表 |
| `mount` | 挂载设备 |
| `iostat` | IO 性能统计 |

---

## 网络操作

```bash
# 网络连通性
ping -c 4 8.8.8.8
curl -I https://api.example.com          # 查看响应头
curl -X POST -H "Content-Type: application/json" \
  -d '{"key":"value"}' http://localhost:8080/api

# 端口监听
ss -tlnp                                    # TCP 监听端口
netstat -tlnp                               # 传统方式（需安装）
lsof -i :8080                               # 谁在用 8080

# DNS 排查
nslookup example.com
dig example.com
```

**curl 常用参数**

| 参数 | 用途 |
|------|------|
| `-I` | 仅响应头 |
| `-v` | 详细输出（调试） |
| `-L` | 跟随重定向 |
| `-o file` | 输出到文件 |
| `-s` | 静默模式 |
| `-u user:pass` | 基本认证 |

---

## 打包与压缩

```bash
tar -czf app.tar.gz /app/                    # 创建 tar.gz
tar -xzf app.tar.gz                          # 解压
tar -cjf app.tar.bz2 /app/                   # bz2 压缩
tar -xvf app.tar -C /dest/                   # 解压到指定目录

gzip app.log                                 # 压缩为 .gz
gunzip app.log.gz                            # 解压
zip -r app.zip /app/                         # zip 压缩
unzip app.zip                                # 解压 zip
```

| 格式 | 创建 | 解压 |
|------|------|------|
| `.tar.gz` | `tar -czf` | `tar -xzf` |
| `.tar.bz2` | `tar -cjf` | `tar -xjf` |
| `.zip` | `zip -r` | `unzip` |
| `.gz` | `gzip` | `gunzip` |

---

## 后端开发场景

### 1. 查看 Java 进程 & JVM 状态

```bash
ps -ef | grep java
# 或
jps -l -v

# 查看进程 PID 后：
top -p $(pgrep -f "java" | head -1)
```

### 2. 实时追踪应用日志

```bash
tail -f /app/logs/application.log | grep --line-buffered "ERROR\|Exception"
```

### 3. 查找占用磁盘最多的文件

```bash
du -sh /app/* | sort -rh | head -10
```

### 4. 批量替换配置文件

```bash
find /app/config -name "*.yml" -exec sed -i 's/dev/prod/g' {} \;
```

### 5. 检查端口是否被占用

```bash
ss -tlnp | grep -E "8080|8443"
```

### 6. 快速备份与恢复

```bash
cp app.jar{,.bak}                    # 备份（app.jar.bak）
cp app.jar.bak app.jar               # 恢复
```

### 7. 查看系统资源限制

```bash
ulimit -a                            # 查看所有限制
ulimit -n 65535                      # 临时改最大文件句柄数
```

### 8. 启动 Spring Boot 应用（后台）

```bash
nohup java -jar app.jar --spring.profiles.active=prod > app.log 2>&1 &
```

---

## 常见问题

**Q: 文件删除后磁盘空间没释放？**
A: 有进程仍持有文件句柄。`lsof | grep deleted` 找到进程并重启。

**Q: `command not found` 但程序已安装？**
A: 检查 PATH：`echo $PATH`。如果是当前目录程序，用 `./program` 执行。

**Q: 如何让 `tail -f` 退出？**
A: 按 `Ctrl+C`。

**Q: 权限不够怎么办？**
A: 前置 `sudo`。确认是该用 `sudo` 还是需要切换到对应用户（`su - user`）。

**Q: 如何让进程在退出 SSH 后继续运行？**
A: 使用 `nohup`、`setsid` 或 `screen` / `tmux` 会话。

**Q: `ss` 和 `netstat` 有什么区别？**
A: `ss` 是现代替代品，性能更好、信息更全，推荐优先使用 `ss`。

**Q: `kill -9` 杀不掉进程？**
A: 进程可能是僵尸（Z）状态，需要其父进程回收；或处于 D 状态（不可中断休眠），需重启系统。
