# Linux 必做项目清单 面试问答清单
> 🎯 基于 Linux 实战项目清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Linux 的文件权限模型是怎样的？rwx 的含义和常见权限值你都清楚吗？

**面试官意图：** 考察对 Linux 权限体系的底层理解，这是服务器安全的基础。

**完美解答：**

Linux 权限模型基于 **用户（User）**、**用户组（Group）**、**其他用户（Others）** 三个维度，每个维度有三个权限位：**r（读）**、**w（写）**、**x（执行）**。

以 `-rwxr-xr--` 为例：
- 第 1 位 `-` 表示文件类型（`d` 目录，`l` 软链接）
- 第 2-4 位 `rwx` 属主权限（4+2+1=7）
- 第 5-7 位 `r-x` 属组权限（4+0+1=5）
- 第 8-10 位 `r--` 其他用户权限（4+0+0=4）

常见的权限值：
| 值 | 权限 | 说明 |
|----|------|------|
| 755 | rwxr-xr-x | 常见可执行文件/目录 |
| 644 | rw-r--r-- | 常见普通文件 |
| 600 | rw------- | 密钥文件（SSH） |
| 700 | rwx------ | 私密脚本目录 |

**特殊权限**：
- **SUID（4000）**：执行时以文件属主身份运行（如 `/usr/bin/passwd`）
- **SGID（2000）**：目录下新建文件继承所属组
- **Sticky Bit（1000）**：/tmp 目录，只有文件所有者能删除

**延伸追问应对：** 如果面试官问"如何查找所有 SUID 文件"，用 `find / -perm -4000`，并解释这是安全审计的常见操作。

---

### Q2：nohup 和 systemd 在管理 Java 后台进程上有什么区别？

**面试官意图：** 考察对 Java 生产部署核心工具的理解深度。

**完美解答：**

| 特性 | nohup | systemd |
|------|-------|---------|
| 使用方式 | `nohup java -jar app.jar &` | 编写 `.service` 文件 |
| 开机自启 | 不支持 | `systemctl enable` |
| 失败重启 | 不支持 | `Restart=always` |
| 日志管理 | 重定向到文件 | `journalctl` 统一管理 |
| 进程控制 | kill + 重新运行 | `start/stop/restart/status` |
| 资源限制 | 不支持 | `LimitNOFILE`、`CPUQuota` 等 |

**生产环境核心建议**：使用 systemd 管理 Java 服务。一个标准的 Spring Boot 服务单元文件示例：

```ini
[Unit]
Description=My Spring Boot Application
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/app
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

**延伸追问应对：** 如果面试官问"nohup 有什么坑"，回答：关闭终端时 SIGHUP 信号仍可能终止进程，要配合 `disown` 或 `setsid` 使用；且无法自动重启。

---

### Q3：Linux 的网络排查工具有哪些？排查"端口连不上"一般是什么流程？

**面试官意图：** 考察线上排障的实战经验。

**完美解答：**

核心网络排查工具：

| 工具 | 用途 | 示例 |
|------|------|------|
| `ping` | 检测网络连通性 | `ping -c 4 target.com` |
| `telnet` | 测试端口连通性 | `telnet 192.168.1.1 8080` |
| `ss` | 查看端口监听状态 | `ss -tlnp` |
| `curl` | 测试 HTTP 接口 | `curl -v http://localhost:8080/health` |
| `tcpdump` | 抓包分析 | `tcpdump -i eth0 port 80` |
| `nslookup/dig` | DNS 解析 | `dig example.com` |

**"端口连不上"排查流程**：

1. **先确认进程是否在监听**：`ss -tlnp | grep 8080`，看 Java 进程是否正常
2. **确认防火墙是否拦截**：`systemctl status firewalld`，检查 `firewall-cmd --list-all`
3. **确认服务本身是否正常**：`curl localhost:8080/actuator/health`
4. **确认监听地址**：`ss -tlnp` 看是 `0.0.0.0:8080` 还是 `127.0.0.1:8080`（如果是环回地址则外部无法访问）
5. **确认云平台安全组**：在云厂商控制台检查入站规则是否放通该端口

**延伸追问应对：** 如果面试官问"线上突然大量 TIME_WAIT 怎么办"，回答：用 `ss -s` 看状态统计，结合 `net.ipv4.tcp_tw_reuse` 和 `net.ipv4.tcp_fin_timeout` 等内核参数调优。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你在生产环境是如何部署 Spring Boot 项目的？请详细描述从打包到上线的完整流程。

**面试官意图：** 考察你是否有真正的"上线"经验，而非只在 IDE 里运行过。

**完美解答：**

我的标准部署流程分为 6 个步骤：

**第一步：打包与准备**
```bash
# Maven 打包，跳过测试
mvn clean package -DskipTests -Pprod

# 检查 Jar 包是否正常
java -jar target/app.jar --dry-run
```

**第二步：环境初始化**
```bash
# 创建专用用户，减少权限风险
useradd -m -s /bin/bash appuser

# 创建目录结构
mkdir -p /opt/app/{bin,logs,config,backup}

# 上传 Jar 包到 /opt/app/
```

**第三步：配置 systemd 服务**
```ini
[Unit]
Description=App Server
After=network.target

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/app
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/app/app.jar --spring.config.location=/opt/app/config/application-prod.yml
Restart=always
RestartSec=10
StandardOutput=append:/opt/app/logs/app.log
StandardError=append:/opt/app/logs/error.log

[Install]
WantedBy=multi-user.target
```

**第四步：日志切割**
```bash
# /etc/logrotate.d/app
/opt/app/logs/*.log {
    daily
    rotate 30
    compress
    missingok
    copytruncate
    postrotate
        systemctl reload app || true
    endscript
}
```

**第五步：启动与验证**
```bash
systemctl daemon-reload
systemctl enable app
systemctl start app

# 健康检查
curl -s http://localhost:8080/actuator/health | grep '"status":"UP"'
```

**第六步：配置 Nginx 反向代理并验证 HTTPS 访问**

> ⚠️ 我在这踩过一个坑：忘记配置 logrotate 导致日志把磁盘写满。后来加上了 `copytruncate` 配置，重启策略也改成了 `systemctl reload` 而不是 `restart`，避免进程中断。

---

### Q5：你在 Nginx 上做过哪些配置？反向代理、负载均衡、SSL 分别是如何实现的？

**面试官意图：** 考察 Nginx 在生产环境中的工程化配置能力。

**完美解答：**

**1. 反向代理配置：**
```nginx
upstream backend {
    server 127.0.0.1:8081;
    server 127.0.0.1:8082;
    server 127.0.0.1:8083;
    
    # 健康检查
    check interval=3000 rise=2 fall=3;
}

server {
    listen 80;
    server_name api.example.com;
    
    # 反向代理 Java 后端
    location /api/ {
        proxy_pass http://backend/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时配置
        proxy_connect_timeout 5s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }
    
    # 前端静态资源
    location / {
        root /opt/app/web;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

**2. 负载均衡策略对比：**

| 策略 | 指令 | 适用场景 |
|------|------|----------|
| 轮询 | 默认 | 各节点配置均匀 |
| 加权 | `server 127.0.0.1:8081 weight=3;` | 节点性能不均 |
| IP Hash | `ip_hash;` | 保持 Session 粘性 |
| 最小连接 | `least_conn;` | 请求处理时间差异大 |

**3. SSL 配置：**
```nginx
server {
    listen 443 ssl http2;
    server_name api.example.com;
    
    ssl_certificate     /etc/nginx/ssl/example.com.pem;
    ssl_certificate_key /etc/nginx/ssl/example.com.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    
    # 强制 HTTPS 跳转
    location / {
        return 301 https://$host$request_uri;
    }
}
```

---

### Q6：服务器 CPU 飚到 100% 了，你是怎么排查的？

**面试官意图：** 考察线上故障排查的方法论和工具链熟练度。

**完美解答：**

我会按照"系统级 → 进程级 → 线程级 → 代码级"的四层排查法：

**第一层：系统级确认**
```bash
# 整体负载
top -c          # 按 P 按 CPU 排序
htop            # 更直观

# 平均负载
uptime
cat /proc/loadavg

# 各核利用率
mpstat -P ALL 1
```

**第二层：定位进程**
从 top 输出找到 CPU 最高的进程 PID，确认是 Java 进程（PID=1234）。

**第三层：定位线程**
```bash
top -H -p 1234            # 查看进程内各线程 CPU
printf "%x\n" 5678         # 将高 CPU 线程号转十六进制
```

**第四层：定位代码**
```bash
# 抓取线程堆栈
jstack 1234 > /tmp/thread_dump.txt

# 搜索对应线程（nid=0x162e）
grep -A 20 "0x162e" /tmp/thread_dump.txt
```

常见原因及对策：
| 问题 | 堆栈特征 | 解决方案 |
|------|----------|----------|
| 死循环 | 持续在某个业务方法 | 检查 SQL/循环条件 |
| GC 频繁 | GC 线程占比高 | 调整 JVM 参数、堆大小 |
| 锁竞争 | 大量线程 BLOCKED | 优化同步块、减小锁粒度 |
| 异步失误 | 线程池满 | 调整线程池参数、限流 |

> 💡 **进阶技巧**：`show-busy-java-threads` 脚本能一键输出最繁忙的线程堆栈，这是阿里开源的工具，面试中提一下很加分。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：如何在一个 Linux 服务器上设计一套高可用的 Java 服务部署方案？

**面试官意图：** 考察从"能跑"到"稳定运行"的架构设计能力。

**完美解答：**

高可用部署方案应该是"预防 + 自动恢复 + 可观测"三位一体：

**1. 进程级高可用：systemd 守护**
```ini
Restart=always
RestartSec=5
# 健康检查
ExecStartPre=/usr/bin/curl -sf http://localhost:8080/actuator/health
```

**2. 多实例部署 + Nginx 负载均衡**
```
          Nginx (端口 80/443)
         /         |         \
    App:8081   App:8082   App:8083
         \         |         /
           Redis + MySQL (共享存储)
```

**3. 优雅上下线机制**
- 服务上线：先注册到 Nginx，再开启流量
- 服务下线：先摘除流量，等待存量请求处理完，再关闭
- Spring Boot Actuator 的 `/actuator/shutdown` 配合 preStop 钩子

**4. 故障自动恢复链**
```
Nginx 健康检查检测到节点宕机
  → 摘除该节点
  → systemd 自动重启进程
  → 进程启动后健康检查通过
  → Nginx 自动将节点加回池子
```

**5. 可观测体系**
- 系统层：Prometheus + Node Exporter + Grafana
- JVM 层：Micrometer + Actuator 暴露指标
- 应用层：自定义业务指标（QPS、错误率、延迟）

> ⚠️ 需要注意：单机高可用是伪高可用，真正生产环境需要至少两台服务器配合 Keepalived + VIP 实现跨节点故障转移。

---

### Q8：Docker 和传统虚拟机有什么区别？你在 AI 项目中用 Docker 部署了什么？

**面试官意图：** 考察对容器化技术的理解，以及 AI 工程化落地的经验。

**完美解答：**

**与虚拟机的核心区别：**

| 维度 | 虚拟机 | Docker 容器 |
|------|--------|-------------|
| 内核 | 每个 VM 有独立 Guest OS | 共享宿主机内核 |
| 启动时间 | 分钟级 | 秒级 |
| 镜像大小 | GB~TB | MB~GB |
| 资源开销 | 高（完整 OS） | 低（仅进程级隔离） |
| 隔离性 | 强（完全隔离） | 中等（Namespaces） |

**在 AI 项目中的 Docker 部署实践：**

我使用 Docker Compose 一键启动了 AI 全套环境：

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  milvus:
    image: milvusdb/milvus:latest
    ports:
      - "19530:19530"
    environment:
      ETCD_ENDPOINTS: etcd:2379
    depends_on:
      - etcd

  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_models:/root/.ollama
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]

  app:
    build: ./app
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - milvus
      - ollama

volumes:
  mysql_data:
  ollama_models:
```

> 🎯 **关键点**：Docker 让"环境一致性"不再是问题，开发、测试、生产用同一套配置，尤其是 GPU 驱动的场景，Docker 的 `nvidia-docker` 大大简化了部署复杂度。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：服务器磁盘满了怎么办？你有哪些应对手段？

**面试官意图：** 考察运维基本功和应急处理经验。

**完美解答：**

**第一步：确认问题**
```bash
# 查看整体磁盘使用
df -h

# 查看各目录占用大小（从根目录往下查）
du -sh /* | sort -hr | head -10

# 查找超大文件（>500MB）
find / -type f -size +500M -exec ls -lh {} \; | sort -k5 -hr
```

**第二步：常见元凶和清理方案**

| 元凶 | 定位方法 | 解决 |
|------|----------|------|
| 应用日志 | `du -sh /opt/app/logs/` | 配置 logrotate，压缩归档 |
| 容器日志 | `du -sh /var/lib/docker/containers/` | 限制 Docker 日志大小 |
| 系统日志 | `journalctl --disk-usage` | `journalctl --vacuum-size=500M` |
| Core dump | `find / -name "core.*"` | 关闭 core dump 或限制大小 |
| 备份文件 | `find / -name "*.sql.gz" -mtime +30` | 删除或转移到冷存储 |

**第三步：扩容方案（如果清理后仍不够）**
```bash
# 新增磁盘挂载
fdisk /dev/sdb
mkfs.ext4 /dev/sdb1
mount /dev/sdb1 /data

# LVM 在线扩容
lvextend -L +100G /dev/mapper/rootvg-applv
resize2fs /dev/mapper/rootvg-applv
```

> ⚠️ **经验之谈**：我遇到过最坑的一次是 Docker 的 overlay2 目录占满整个磁盘，原因是容器日志没有限制。后来在所有部署脚本中强制加入容器的 `--log-opt max-size=10m --log-opt max-file=3`。

---

### Q10：如果要部署一个私有化大模型服务到 Linux 服务器上，需要考虑哪些方面？

**面试官意图：** 考察 AI 工程化部署的综合能力，贴合当前 AI 趋势。

**完美解答：**

私有化大模型部署需要考虑以下 5 个关键方面：

**1. 硬件检查**
```bash
# GPU 可用性
nvidia-smi

# 显存检查（Qwen2.5-7B 需要 ~16GB 显存）
nvidia-smi --query-gpu=memory.free --format=csv

# CPU 和内存
lscpu | grep "Model name"
free -h
```

**2. 驱动和 CUDA 环境**
```bash
# 检查驱动版本
nvidia-smi | grep "Driver Version"

# CUDA 版本兼容性
nvcc --version
```

**3. 部署方式选择**
| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| Ollama | 一键部署，开箱即用 | 灵活度低 | 快速验证 |
| vLLM | 高吞吐，支持 PagedAttention | 配置复杂 | 生产高并发 |
| llama.cpp | CPU 友好，量化支持好 | 功能少 | 无 GPU 环境 |

**4. 显存优化策略**
```bash
# Ollama 启动时限制并行数
OLLAMA_NUM_PARALLEL=4 ollama serve

# 使用量化模型（4bit 量化减少 60% 显存）
ollama pull qwen2.5:7b-q4_K_M
```

**5. 服务稳定性**
```bash
# 后台运行
nohup ollama serve > /var/log/ollama.log 2>&1 &

# 或使用 systemd 管理
[Service]
ExecStart=/usr/local/bin/ollama serve
Restart=always
User=ollama

# 端口安全（避免暴露到公网）
iptables -A INPUT -p tcp --dport 11434 -s 127.0.0.1 -j ACCEPT
iptables -A INPUT -p tcp --dport 11434 -j DROP
```

> 💡 **面试加分**：提到"模型路由策略"，简单问题用本地量化模型，复杂问题用云端 API，可以控制成本的同时保证质量。

---

### Q11：如何从日志中快速定位线上 Bug？请用实际案例说明。

**面试官意图：** 考察日志分析和问题定位的实战经验。

**完美解答：**

我有一套"日志定位三板斧"方法论：

**场景：线上某个接口突然大量 500 错误**

**第一板斧：定位时间和上下文**
```bash
# 查看错误集中在什么时间段
grep "ERROR" /opt/app/logs/app.log | head -20

# 提取关键错误堆栈（聚合相同错误）
grep "ERROR" app.log | awk '{print $5,$6,$7}' | sort | uniq -c | sort -rn

# 看这个时间段前后的上下文
grep -n "2026-07-22 14:3[0-5]" app.log | head -100
```

**第二板斧：提取关键链路**
```bash
# 根据 traceId 追踪完整请求链路
grep "traceId=abc123" app.log

# 或根据订单号查询
grep "ORD-20260722-001" app.log
```

**第三板斧：配合工具分析**
```bash
# 统计接口错误率
grep "/api/order/create" app.log | grep -c "ERROR"

# 慢查询日志分析
grep "Slow SQL" app.log | awk '{print $NF}' | sort -rn | head -10

# 找出最耗时接口
awk '/POST \/api\//{print $7,$NF}' app.log | sort -k2 -rn | head -5
```

**真实案例**：某次线上用户反馈"提交订单无响应"，通过日志发现大量 `Connection pool timeout`，Druid 连接池被耗尽。进一步追踪发现有个定时任务在批量插入数据后未释放连接，最终解决办法是加 `try-with-resources` 确保连接归还到池中。

> 💡 **加分技巧**：线上环境建议统一日志格式为 `JSON`，配合 ELK 或 Loki 可以实现全链路可观测，排查效率提高 10 倍以上。

---

### Q12：线上 Redis 突然变慢了，你怎么排查？

**面试官意图：** 考察中间件故障排查能力，Redis 是 Java 开发最高频使用的缓存组件。

**完美解答：**

**第一层：确认"慢"的表现**
```bash
# 延迟测试
redis-cli --latency -h 127.0.0.1 -p 6379
# 正常延迟 < 1ms，如果延迟 > 10ms 则有问题

# 查看慢查询日志
redis-cli SLOWLOG GET 10
redis-cli SLOWLOG LEN
```

**第二层：排查常见原因**

| 原因 | 诊断命令 | 解决方案 |
|------|----------|----------|
| 大 Key | `redis-cli --bigkeys` | 拆分大 Key，使用 hash 或 list |
| 内存不足 | `info memory` → `used_memory` | 加内存，或设置淘汰策略 `allkeys-lru` |
| Fork 耗时 | `info persistence` → `latest_fork_usec` | 减少持久化频率，用子进程 |
| 热 Key | `MONITOR` 命令（慎用） | 本地缓存 + 热点分散 |

**第三层：结合系统层排查**
```bash
# 确认不是网络问题
ping redis-server

# 确认不是带宽问题
sar -n DEV 1

# 确认不是 swap（Redis 最怕被换出）
redis-cli info | grep process_swaps
```

**实战经验**：我有一次遇到 Redis 突发延迟飙到 200ms，最终定位到是因为 AOF 重写时触发 `fork`，在 4G 内存的 Redis 实例上，fork 耗时 1.2 秒。解决方案是将 `auto-aof-rewrite-min-size` 调大，并安排在业务低峰期手动触发重写。

> ⚠️ **关键警告**：线上绝对不要在生产环境运行 `KEYS *` 命令，会阻塞 Redis 所有操作。用 `SCAN` 替代。

---

### Q13：服务器被暴力破解 SSH，你有哪些安全加固措施？

**面试官意图：** 考察安全意识和服务器安全加固工程经验。

**完美解答：**

SSH 安全加固应该从"进不去 + 发现早"两个维度入手：

**第一道防线：SSH 配置加固**
```bash
# /etc/ssh/sshd_config
Port 2222                               # 改端口，避开扫描默认端口
PermitRootLogin no                       # 禁止 root 直接登录
PasswordAuthentication no                # 关闭密码认证
PubkeyAuthentication yes                 # 启用密钥认证
AllowUsers appuser deployer              # 限制可登录用户
MaxAuthTries 3                           # 最大认证尝试次数
ClientAliveInterval 300                  # 空闲超时（秒）
ClientAliveCountMax 0                    # 超时自动断开
```

**第二道防线：防火墙和 Fail2Ban**
```bash
# firewalld 限制 SSH 访问来源
firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="192.168.1.0/24" service name="ssh" accept'
firewall-cmd --permanent --remove-service=ssh
firewall-cmd --reload

# Fail2Ban 自动封禁
# /etc/fail2ban/jail.local
[sshd]
enabled = true
port = 2222
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
bantime = 86400
```

**第三道防线：监控和告警**
```bash
# 查看登录失败记录
lastb | head -20

# 使用脚本监控异常登录
grep "Failed password" /var/log/auth.log | awk '{print $11}' | sort | uniq -c | sort -rn
```

> 🎯 **总结**：密钥登录 + 改端口 + Fail2Ban 三件套可以挡住 99% 的暴力破解。面试中如果能具体说明实施细节和配置参数，面试官会认为你真的"做过"而不是"背过"。

---

## 💎 面试加分金句

1. "Linux 运维不是背命令，而是建立一套'现象 → 分析 → 定位 → 解决 → 预防'的闭环思维。我在每个项目中都强制自己做复盘文档。"
2. "我习惯把常用的排查流程写成 Shell 脚本，比如一键收集 CPU/内存/磁盘/JVM 堆栈信息，排障效率提升 3 倍。"
3. "Docker 不只是部署工具，它最大的价值是让团队实现'环境一致'——开发环境是什么样，测试和生产就是什么样。"
4. "安全不是一次性配置，而是持续运营的过程。我每周都会检查一次服务器安全状态，包括审计日志和异常登录。"
5. "裸机部署和容器化部署我都做过，对于小团队来说，Docker Compose 是性价比最高的方案；但上百个服务就需要 K8s 了。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| 怎么优化 Linux 内核参数？ | 从 `sysctl -a` 入手，重点讲 `net.ipv4.tcp_tw_reuse`、`vm.swappiness`、`fs.file-max` |
| 你用过哪些监控工具？ | 从命令行维度（top/htop/iostat）讲到系统维度（Prometheus+Grafana），展现全面覆盖 |
| Docker 和 Podman 的区别？ | Podman 无守护进程、rootless 模式、兼容 Dockerfile，面试时客观对比 |
| 有没有遇到过 OOM？怎么处理的？ | 讲一次真实案例：JVM OOM → 分析堆转储 → 找到内存泄漏对象 → 修复并加监控 |
| 你做过安全审计吗？ | 检查 SUID 文件、开放端口、弱密码、内核版本 CVE、定期做基准合规扫描 |

## 🔗 关联知识点

- [Shell 必做项目清单-面试问答](./Shell必做项目清单-面试问答.md) — 自动化运维脚本面试问答
- [三大核心项目-面试问答](./三大核心项目-面试问答.md) — 秒杀系统/RAG/Agent 部署环境基于 Linux
- 计算机网络 — TCP/IP、HTTP 协议是排查网络问题的基础
- JVM 调优 — 服务器性能排查时离不开 JVM 工具链
