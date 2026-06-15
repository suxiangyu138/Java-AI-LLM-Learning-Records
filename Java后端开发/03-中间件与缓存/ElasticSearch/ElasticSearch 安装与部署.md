# ElasticSearch 安装与部署（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | ES 环境搭建速查
> **版本**：Elasticsearch 7.17.x / 8.x
> **环境**：Docker / Linux / Windows

---

## 一、Docker 部署（推荐，一键盘好）

### 1.1 单节点（开发环境）

```bash
docker run -d \
  --name es \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  -e "xpack.security.enabled=false" \
  elasticsearch:7.17.0
```

验证：
```bash
curl http://localhost:9200
# 返回 JSON 含 cluster_name、version 等信息即为成功
```

### 1.2 完整版：ES + Kibana + IK（开发一键启动）

`docker-compose.yml`：

```yaml
version: '3'
services:
  elasticsearch:
    image: elasticsearch:7.17.0
    container_name: es
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - es_data:/usr/share/elasticsearch/data
      - ./es-plugins:/usr/share/elasticsearch/plugins
    networks:
      - es_net

  kibana:
    image: kibana:7.17.0
    container_name: kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - es_net

volumes:
  es_data:

networks:
  es_net:
```

启动：
```bash
docker-compose up -d
# Kibana: http://localhost:5601
```

---

## 二、ElasticSearch 8.x 部署（安全开启）

8.x 默认启用安全（SSL + 密码），开发环境可关闭：

```yaml
# docker-compose.yml for ES 8.x
services:
  es8:
    image: elasticsearch:8.6.0
    container_name: es8
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms1g -Xmx1g
      - xpack.security.enabled=false          # 关闭安全
      - xpack.security.enrollment.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - es8_data:/usr/share/elasticsearch/data

volumes:
  es8_data:
```

若启用安全，首次启动时会输出 `elastic` 用户的密码和 enrollment token，需妥善保存。

---

## 三、Linux 部署

### 3.1 安装步骤

```bash
# 1. ES 不能以 root 运行，创建专用用户
useradd es
echo "your_password" | passwd --stdin es

# 2. 下载解压
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.17.0-linux-x86_64.tar.gz
tar -zxvf elasticsearch-7.17.0-linux-x86_64.tar.gz -C /usr/local/
chown -R es:es /usr/local/elasticsearch-7.17.0

# 3. 配置
su - es
cd /usr/local/elasticsearch-7.17.0/config
```

### 3.2 核心配置 `elasticsearch.yml`

```yaml
# ------------------- 集群 -------------------
cluster.name: my-es-cluster
node.name: node-1

# ------------------- 路径 -------------------
path.data: /data/es/data
path.logs: /data/es/logs

# ------------------- 网络 -------------------
network.host: 0.0.0.0
http.port: 9200
transport.port: 9300

# ------------------- 发现 -------------------
discovery.seed_hosts: ["192.168.1.10", "192.168.1.11", "192.168.1.12"]
cluster.initial_master_nodes: ["node-1", "node-2", "node-3"]

# ------------------- 内存锁定（生产必须）-------------------
bootstrap.memory_lock: true

# ------------------- 跨域（Kibana 连接）-------------------
http.cors.enabled: true
http.cors.allow-origin: "*"
```

### 3.3 JVM 配置 `jvm.options`

```
# 堆内存：不超过物理内存 50%，最大 32GB（受限于 JVM 指针压缩）
-Xms4g
-Xmx4g

# GC：生产推荐 G1GC（8.x 已是默认）
-XX:+UseG1GC
```

### 3.4 系统配置

```bash
# /etc/security/limits.conf  -- 打开文件数限制
es soft nofile 65536
es hard nofile 65536

# /etc/sysctl.conf  -- 虚拟内存
vm.max_map_count=262144

# 生效
sysctl -p
```

### 3.5 启动

```bash
# 前台
./bin/elasticsearch

# 后台
./bin/elasticsearch -d -p pid
```

---

## 四、Windows 部署

```powershell
# 1. 下载 zip 解压
# 2. 进入目录
cd elasticsearch-7.17.0

# 3. 启动
./bin/elasticsearch.bat

# 4. 验证
# 浏览器访问 http://localhost:9200
```

**注意事项**：
- Windows 不支持 `bootstrap.memory_lock`
- 中文路径可能导致问题，建议放在英文目录下
- 用 PowerShell 而非 CMD 避免编码问题

---

## 五、Kibana 安装（可视化管理）

### Docker 版
```bash
docker run -d \
  --name kibana \
  -p 5601:5601 \
  -e ELASTICSEARCH_HOSTS=http://192.168.1.100:9200 \
  kibana:7.17.0
```

### Dev Tools 常用操作

Kibana 的 `Dev Tools` 是日常操作 ES 的首选：

```json
// 查看集群健康
GET _cluster/health

// 查看所有索引
GET _cat/indices?v

// 查看节点
GET _cat/nodes?v

// 监控索引大小
GET _cat/shards?v
```

---

## 六、生产部署 Checklist

| 检查项 | 要求 |
|---|---|
| **JVM 堆内存** | 不超过物理内存 50%，最大 32GB |
| **内存锁定** | `bootstrap.memory_lock: true` |
| **文件描述符** | >= 65536 |
| **虚拟内存** | `vm.max_map_count >= 262144` |
| **线程数限制** | `ulimit -u >= 4096` |
| **磁盘** | SSD，预留 20% 以上空间 |
| **节点分离** | Master/Data/Ingest 角色分离 |
| **安全** | 8.x 默认 SSL，7.x 建议配 x-pack |
| **备份** | 定期 Snapshot 到远程仓库 |

---

## 七、集群部署要点

```yaml
# 三节点集群配置示例
# node-1
cluster.name: my-es
node.name: node-1
node.master: true
node.data: true
discovery.seed_hosts: ["node-1-ip", "node-2-ip", "node-3-ip"]
cluster.initial_master_nodes: ["node-1", "node-2", "node-3"]

# node-2 / node-3 同理，只需改 node.name 和 IP
```

---

## 八、核心端口说明

| 端口 | 协议 | 用途 |
|---|---|---|
| `9200` | HTTP | REST API（客户端、Kibana） |
| `9300` | TCP | 节点间通信（Transport） |
| `5601` | HTTP | Kibana 管理界面 |

---

## 九、极简总结

```
Docker 一把梭 = elasticsearch:7.17.0 + ES_JAVA_OPTS="-Xms512m -Xmx512m"
生产上线必调 = JVM 堆内存 + 内存锁定 + mmap + 文件描述符
Kibana Dev Tools = 日常操作首选
```
