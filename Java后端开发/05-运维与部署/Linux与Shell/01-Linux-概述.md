## Linux 概述

### 什么是 Linux

Linux 是一个开源的类 Unix 操作系统内核，由 Linus Torvalds 于 1991 年创建。搭配 GNU 工具集形成完整的操作系统，广泛应用于服务器、嵌入式系统、云计算等领域。对于 Java 后端开发者，Linux 是生产环境的核心载体——绝大多数 Java 应用部署在 Linux 服务器上。

### Linux 核心特性

| 特性 | 说明 | Java 后端关联 |
|------|------|---------------|
| 开源免费 | 内核及配套工具免费，可定制裁剪 | 降低服务器成本，可针对 JVM 调优内核参数 |
| 多用户多任务 | 进程隔离，权限分离，CFS 调度器 | 多 Java 实例共存，开发/生产环境隔离 |
| 高稳定性 | 长时间运行无需重启，内存管理优秀 | 7x24 小时 Java 服务稳定运行 |
| 强大网络支持 | epoll I/O 多路复用，TCP/IP 协议栈 | Netty / Tomcat 基于 epoll 实现高并发 |
| 命令行与脚本 | 丰富的 CLI 工具 + Shell 自动化 | 部署、日志、监控、CI/CD 全链路支撑 |
| 安全权限模型 | rwx 权限 + SELinux / AppArmor | 进程级安全隔离，防止越权操作 |

### Linux vs Windows（服务器场景）

| 对比维度 | Linux | Windows Server |
|----------|-------|----------------|
| 市场份额（服务器） | 90%+ | < 5% |
| 成本 | 免费 | 需授权费用 |
| 稳定性 | 极高，可数年不重启 | 需定期重启更新 |
| 资源占用 | 轻量，无 GUI 仅需 256MB 内存 | 较重，GUI 占用资源多 |
| Java 生态兼容 | JDK / Tomcat / MySQL 原生支持 | 支持但生产环境极少使用 |
| Docker / 容器 | 原生支持，性能最佳 | 支持有限，需 Hyper-V |
| 命令行效率 | 管道 + 脚本组合强大 | PowerShell 功能弱于 Bash |
| 远程管理 | SSH 轻量高效 | RDP 需图形界面 |

### Linux 发行版（Java 后端常用）

| 发行版 | 包管理器 | 默认 init | 推荐场景 |
|--------|----------|-----------|----------|
| CentOS 7 / Stream | yum / dnf | systemd | 生产环境首选，企业广泛使用 |
| Ubuntu Server | apt | systemd | 开发/测试环境，社区活跃 |
| RedHat Enterprise Linux | yum / dnf | systemd | 商业支持，大型企业生产环境 |
| Rocky Linux / AlmaLinux | yum / dnf | systemd | CentOS 替代品，兼容 RHEL |

**选型建议：** 生产环境优先 CentOS 或 Rocky Linux；个人学习 / 本地开发使用 Ubuntu Server 更便捷。

### Linux 文件系统层次结构（FHS）

```
/          -- 根目录，所有文件的起点
├── bin    -- 用户可执行命令（ls, cp, mv）
├── sbin   -- 系统管理命令（fdisk, iptables）
├── etc    -- 系统配置文件（/etc/ssh, /etc/nginx）
├── home   -- 普通用户家目录
├── root   -- root 用户家目录
├── var    -- 可变数据：日志（/var/log），缓存（/var/cache）
├── usr    -- 用户程序和数据（/usr/local, /usr/share）
├── opt    -- 第三方软件安装目录
├── tmp    -- 临时文件（重启后清空）
├── proc   -- 虚拟文件系统，进程与内核信息
├── dev    -- 设备文件（/dev/sda 磁盘）
├── mnt    -- 临时挂载点
└── boot   -- 内核与引导加载程序
```

**Java 后端关注的关键目录：**

| 目录 | 作用 |
|------|------|
| `/etc/profile` / `/etc/environment` | 系统级环境变量（JAVA_HOME） |
| `/var/log` | 应用日志存放处 |
| `/opt` | 第三方软件（JDK, Tomcat） |
| `/usr/local` | 编译安装的程序 |
| `/proc` | 查看进程、内存、CPU 信息 |

### Linux 在 Java 后端中的角色

1. **部署环境**：Spring Boot 打 jar 包后通过 `nohup java -jar app.jar &` 或 `systemctl` 运行。
2. **中间件载体**：MySQL、Redis、Nginx、Kafka、Elasticsearch 均优先运行在 Linux。
3. **日志与监控**：`tail -f` 实时跟踪日志，`top` / `jstat` 监控 JVM 与系统资源。
4. **自动化运维**：Shell 脚本 + crontab 实现定时备份、日志切割、健康检查。
5. **容器化基础**：Docker 底层依赖 Linux 内核特性（cgroups, namespace）。

### Java 后端开发者常用 Linux 命令速查

```bash
# 文件操作
ls -la                        # 查看目录详情
tail -f app.log               # 实时跟踪日志
grep 'ERROR' app.log          # 过滤异常日志
find /opt -name "*.jar"       # 查找 jar 包

# 进程管理
ps -ef | grep java            # 查看 Java 进程
top -p <PID>                  # 监控特定进程资源
kill -9 <PID>                 # 强制终止进程
nohup java -jar app.jar > app.log 2>&1 &   # 后台启动

# 网络诊断
netstat -tlnp | grep java     # 查看 Java 进程端口
ss -tuln                      # 查看监听端口
curl http://localhost:8080/health  # 接口探测
ping -c 4 <host>              # 网络连通性测试

# 系统检查
df -h                         # 磁盘空间
free -h                       # 内存使用
uname -a                      # 系统内核版本
cat /etc/os-release           # 发行版信息

# Java 专属
java -version                 # JDK 版本
jps -l                        # 列出 Java 进程
jstack <PID>                  # 导出线程栈
jstat -gc <PID> 1000 5       # GC 统计
```

### Linux 与 Shell 的关系

Shell 是 Linux 的命令行解释器，用户通过 Shell 与内核交互。Bash（Bourne Again Shell）是 Linux 默认 Shell。

```
用户 --> Shell（bash） --> Linux 内核 --> 硬件
```

Java 后端开发者使用 Shell 的场景：
- 编写启动/停止脚本（`deploy.sh`, `start.sh`）
- 管道组合命令处理日志（`grep | awk | sort`）
- cron 定时任务执行批处理
- CI/CD 流水线中的脚本步骤

### 常见问题（FAQ）

**Q：Windows 上开发 Java，必须学 Linux 吗？**
A：是。生产环境几乎全是 Linux，本地开发与生产环境的差异（路径分隔符、大小写敏感、权限模型）会导致线上问题。建议通过 WSL2 或虚拟机在本地熟悉 Linux。

**Q：CentOS 7 已停止维护，现在用什么？**
A：CentOS Stream、Rocky Linux 或 AlmaLinux 均可。Ubuntu Server 也是成熟选择。

**Q：Java 后端需要学到什么程度？**
A：掌握文件操作、进程管理、网络诊断、日志查看、Shell 脚本基础、JDK 安装配置即可应对 80% 的工作。非运维岗位无需深入内核编译或 SELinux 策略编写。

**Q：`nohup` 和 `systemctl` 有什么区别？**
A：`nohup` 简单后台运行，终端关闭后进程仍在；`systemctl` 是系统服务管理器，支持开机自启、异常重启、日志收集，适用于生产环境正式部署。

**Q：如何查看 Linux 上 Java 进程的 JVM 参数？**
A：`jcmd <PID> VM.flags` 或 `jinfo -flags <PID>`，也可通过 `ps aux | grep java` 查看启动命令中的显式参数。
