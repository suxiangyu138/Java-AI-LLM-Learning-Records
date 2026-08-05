# 06 - Node Exporter 主机监控

> 🎯 Node Exporter 是 Prometheus 的"主机体检仪" — 从 CPU/内存/磁盘/网络到 systemd 服务状态，覆盖 Linux 主机监控的全部维度

---

## 目录

1. [Node Exporter 安装](#1-node-exporter-安装)
2. [CPU 监控](#2-cpu-监控)
3. [内存监控](#3-内存监控)
4. [磁盘监控](#4-磁盘监控)
5. [网络监控](#5-网络监控)
6. [进程与系统负载](#6-进程与系统负载)
7. [Textfile Collector（自定义指标）](#7-textfile-collector自定义指标)
8. [关键告警规则](#8-关键告警规则)

---

## 1. Node Exporter 安装

```bash
# Docker
docker run -d --name node-exporter --net=host \
  -v /proc:/host/proc:ro -v /sys:/host/sys:ro \
  prom/node-exporter:v1.7.0 --path.procfs=/host/proc --path.sysfs=/host/sys

# 二进制
wget https://github.com/prometheus/node_exporter/releases/download/v1.7.0/node_exporter-1.7.0.linux-amd64.tar.gz
tar -xzf node_exporter-1.7.0.linux-amd64.tar.gz
sudo cp node_exporter-1.7.0.linux-amd64/node_exporter /usr/local/bin/

# systemd
sudo systemctl enable --now node_exporter
curl http://localhost:9100/metrics | head -20
```

### Prometheus 采集配置

```yaml
scrape_configs:
  - job_name: 'node-exporter'
    scrape_interval: 30s
    static_configs:
      - targets:
          - '10.0.1.1:9100'
          - '10.0.1.2:9100'
          - '10.0.1.3:9100'
        labels:
          env: 'prod'
```

---

## 2. CPU 监控

### 核心指标

| 指标 | 说明 |
|------|------|
| `node_cpu_seconds_total{cpu, mode}` | CPU 各模式累计时间 |
| `node_load1 / 5 / 15` | 系统负载（1/5/15 分钟） |

```promql
# CPU 总使用率
100 - avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) by (instance) * 100

# 按模式分解
sum(rate(node_cpu_seconds_total[5m])) by (mode)

# 单核心使用率
100 - rate(node_cpu_seconds_total{mode="idle"}[5m]) * 100

# 负载 vs 核心数
node_load1 / count(node_cpu_seconds_total{mode="idle"})
# > 1.0 → 过载
```

---

## 3. 内存监控

```promql
# ═══ 使用率 ═══
# 可用内存百分比
node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes * 100

# 已用内存百分比（含缓存）
(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100

# ═══ 具体值 ═══
node_memory_MemTotal_bytes          # 总内存
node_memory_MemAvailable_bytes      # 可用（含可回收缓存）
node_memory_MemFree_bytes           # 空闲（不含缓存）
node_memory_Cached_bytes            # 页缓存
node_memory_Buffers_bytes           # 缓冲区

# ═══ Swap ═══
node_memory_SwapTotal_bytes         # Swap 总量
node_memory_SwapFree_bytes          # Swap 空闲

# Swap 使用率（接近 100% = 内存不足在大量换页）
(1 - node_memory_SwapFree_bytes / node_memory_SwapTotal_bytes) * 100
```

> ⚠️ Java 应用场景：Swap 使用率是重要的 OOM 预警信号。JVM 堆内存被 swap 出去会导致 GC 时间暴涨。

---

## 4. 磁盘监控

```promql
# ═══ 空间 ═══
# 磁盘使用率
(1 - node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}) * 100

# 过滤掉 tmpfs/overlay 等虚拟文件系统
(1 - node_filesystem_avail_bytes{fstype!~"tmpfs|fuse.*|overlay"} 
   / node_filesystem_size_bytes{fstype!~"tmpfs|fuse.*|overlay"}) * 100

# ═══ IO ═══
# 磁盘 IO 使用率（%util）
rate(node_disk_io_time_seconds_total{device=~"sd.|nvme.*"}[5m]) * 100

# 读写速率（bytes/s）
rate(node_disk_read_bytes_total{device="sda"}[5m])
rate(node_disk_written_bytes_total{device="sda"}[5m])

# 读写延迟
rate(node_disk_read_time_seconds_total[5m]) / rate(node_disk_reads_completed_total[5m])
rate(node_disk_write_time_seconds_total[5m]) / rate(node_disk_writes_completed_total[5m])

# ═══ Inode ═══
(1 - node_filesystem_files_free{mountpoint="/"} / node_filesystem_files{mountpoint="/"}) * 100

# ═══ 预测磁盘满 ═══
predict_linear(node_filesystem_avail_bytes{mountpoint="/"}[4h], 24 * 3600) <= 0
# → 24 小时内磁盘将满
```

---

## 5. 网络监控

```promql
# ═══ 流量 ═══
# 入站速率
rate(node_network_receive_bytes_total{device="eth0"}[5m])

# 出站速率
rate(node_network_transmit_bytes_total{device="eth0"}[5m])

# ═══ 错误 ═══
# 入站丢包
rate(node_network_receive_drop_total{device="eth0"}[5m])

# 错误包
rate(node_network_receive_errs_total{device="eth0"}[5m])

# ═══ TCP 连接状态 ═══
node_netstat_Tcp_CurrEstab              # 当前 ESTABLISHED 连接数
node_netstat_Tcp_ActiveOpens            # 主动打开数
node_sockstat_TCP_tw                    # TIME_WAIT 数量
```

---

## 6. 进程与系统负载

```promql
# ═══ 进程数 ═══
node_processes_total                   # 总进程数

# ═══ 文件句柄 ═══
node_filefd_allocated / node_filefd_maximum * 100   # 文件句柄使用率

# ═══ 上下文切换 ═══
rate(node_context_switches_total[5m])   # 上下文切换速率

# ═══ 系统启动时间 ═══
time() - node_boot_time_seconds         # 运行时长（秒）
# > 600 → 10 分钟内重启过（可能异常重启）
```

---

## 7. Textfile Collector（自定义指标）

> 💡 用于暴露 Prometheus 无法直接采集的自定义指标（如 cron 任务结果、备份状态）。

```bash
# 启用 textfile collector
node_exporter --collector.textfile.directory=/var/lib/node_exporter/textfile_collector

# 写入自定义指标
cat > /var/lib/node_exporter/textfile_collector/backup.prom << 'EOF'
# HELP backup_last_success_timestamp 最后备份成功时间戳
# TYPE backup_last_success_timestamp gauge
backup_last_success_timestamp{type="mysql"} 1705315200
backup_last_success_timestamp{type="redis"} 1705315000
# HELP backup_status 备份状态 1=成功 0=失败
# TYPE backup_status gauge
backup_status{type="mysql"} 1
backup_status{type="redis"} 0
EOF
```

---

## 8. 关键告警规则

```yaml
# rules/node-exporter.yml
groups:
  - name: node-alerts
    rules:
      # ═══ 致命 — 主机宕机 ═══
      - alert: NodeDown
        expr: up{job="node-exporter"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "主机 {{ $labels.instance }} 宕机"

      # ═══ CPU ═══
      - alert: HighCpuUsage
        expr: 100 - avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) by (instance) * 100 > 90
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.instance }} CPU 使用率 > 90%，持续 10 分钟"

      # ═══ 内存 ═══
      - alert: HighMemoryUsage
        expr: (1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100 > 90
        for: 5m
        labels:
          severity: warning

      # ═══ 磁盘 ═══
      - alert: DiskWillFillIn24h
        expr: predict_linear(node_filesystem_avail_bytes{mountpoint="/"}[4h], 24*3600) <= 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "{{ $labels.instance }} 磁盘 / 预计 24 小时内满"

      - alert: HighDiskUsage
        expr: (1 - node_filesystem_avail_bytes{fstype!~"tmpfs|fuse.*"} / node_filesystem_files{fstype!~"tmpfs|fuse.*"}) * 100 > 85
        for: 5m
        labels:
          severity: warning

      # ═══ Swap ═══
      - alert: SwapUsage
        expr: (1 - node_memory_SwapFree_bytes / node_memory_SwapTotal_bytes) * 100 > 50
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.instance }} Swap 使用率 > 50%，检查内存是否不足"
```

> 🎯 **Node Exporter 是监控体系的基石** — 主机不行，应用再健康也没用。先配好主机告警，再往上构建应用层监控。
