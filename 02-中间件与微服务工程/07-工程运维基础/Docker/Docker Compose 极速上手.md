# Docker Compose 极速上手

## 一、核心认知
1. `Docker Compose`：**单机多容器编排**
   用一份 `docker-compose.yml` 配置，一键启停「MySQL、Redis、Milvus、Nginx、AI服务」等多个容器。
2. 核心优势
   - 不用记超长 `docker run` 命令
   - 容器之间自动互通网络
   - 配置固化，换电脑直接一键重启
3. 前置条件
   安装 Docker Desktop（Windows 默认自带 Compose，无需额外安装）
4. 校验
```bash
docker compose version
```

---

## 二、核心规则
1. 固定配置文件名：`docker-compose.yml`
2. 语法：**YAML 严格缩进**（用空格，不能用 Tab）
3. 核心三层结构：
   - `services`：定义每个容器
   - `volumes`：数据持久化
   - `networks`：容器互联（默认自动创建）

---

## 三、5分钟最简示例（Nginx）
1. 新建文件夹，新建文件 `docker-compose.yml`
```yaml
version: "3.8"

services:
  nginx:
    image: nginx:latest
    container_name: my-nginx
    ports:
      - "8080:80"
    restart: always
```

2. 核心操作命令（必背）
```bash
# 后台启动所有服务
docker compose up -d

# 查看运行状态
docker compose ps

# 查看日志
docker compose logs -f

# 停止容器，保留数据
docker compose stop

# 停止+删除容器、网络（镜像、数据卷保留）
docker compose down
```
浏览器访问 `http://localhost:8080` 直接生效。

---

## 四、关键字段详解（全覆盖）
```yaml
services:
  服务名:
    image: 镜像名:版本        # 依赖镜像
    container_name: 容器名    # 自定义容器名
    ports:                    # 端口映射 宿主机:容器
      - "3306:3306"
    volumes:                  # 数据挂载持久化
      - ./data:/容器路径
    environment:              # 环境变量（密码、配置）
      KEY: VALUE
    restart: always           # 开机/Docker重启自动拉起
    depends_on:               # 依赖顺序（先启动依赖服务）
      - redis
```

---

## 五、生产常用组合模板（直接复制即用）

### 模板1：MySQL8 + Redis（Java后端标配）
```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: mysql8
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: 123456
    volumes:
      - ./mysql-data:/var/lib/mysql
    restart: always

  redis:
    image: redis:latest
    container_name: redis
    ports:
      - "6379:6379"
    volumes:
      - ./redis-data:/data
    restart: always
```

### 模板2：Milvus 向量库（你的AI RAG 刚需）
```yaml
version: "3.8"

services:
  milvus:
    image: milvusdb/milvus:latest
    container_name: milvus
    ports:
      - "19530:19530"
    volumes:
      - ./milvus-data:/var/lib/milvus
    restart: always
```

---

## 六、日常高频命令（精简速查）
```bash
# 重建容器（改完yml配置必用）
docker compose up -d --build

# 只启动单个服务
docker compose up -d mysql

# 查看所有容器日志
docker compose logs -f

# 删除所有容器+网络+卷（清空数据，谨慎）
docker compose down -v
```

---

## 七、容器网络核心（必懂）
1. Compose 会**自动创建独立网桥网络**
2. 同一 yml 下的服务，可直接用**服务名互相访问**
   - 例：Java 连接 MySQL 地址直接填 `mysql:3306`
   - 不用写 `localhost`，容器内部互通

---

## 八、最简学习路线（适配你方向）
1. 熟记：`up -d` / `down` / `ps` / `logs` 四条命令
2. 会写：`ports`、`volumes`、`environment` 三大配置
3. 落地：
   - 后端：MySQL + Redis 一键编排
   - AI：Milvus + Ollama 组合部署
4. 结合 BAT 脚本：一键启动/关闭所有服务

---
