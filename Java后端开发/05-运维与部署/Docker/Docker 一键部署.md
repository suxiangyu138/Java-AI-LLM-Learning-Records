# Docker 一键部署

## MySQL 8.0

```bash
docker run -d \
  --name mysql8 \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  --restart always \
  mysql:8.0
```

- 账号：`root`
- 密码：`123456`

---

## Redis 最新版

```bash
docker run -d \
  --name redis \
  -p 6379:6379 \
  --restart always \
  redis
```

- 无密码，直接连接

---

## Milvus 向量数据库（单机开发版）

```bash
docker run -d \
  --name milvus \
  -p 19530:19530 \
  --restart always \
  milvusdb/milvus:latest
```

- 连接端口：`19530`
- 适合 RAG、向量检索开发

---

## 通用高频补充命令

```bash
# 停止容器
docker stop 容器名

# 启动容器
docker start 容器名

# 删除容器
docker rm 容器名

# 删除镜像
docker rmi 镜像名

# 进入容器内部
docker exec -it 容器名 bash
```
