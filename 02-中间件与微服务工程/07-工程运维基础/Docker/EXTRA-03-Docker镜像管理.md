# 03 - Docker 镜像管理

## 3.1 镜像是什么？

镜像是一个轻量级、可执行的独立软件包，包含运行某个软件所需的所有内容：代码、运行时、系统工具、系统库和设置。

### 联合文件系统 (UnionFS)

Docker 镜像采用**分层存储**架构，每一层都是只读的：

```
┌─────────────────────┐
│  可写容器层 (R/W)    │  ← 容器运行时修改都在这层
├─────────────────────┤
│  应用层              │  ← COPY / ADD 的文件
├─────────────────────┤
│  依赖层              │  ← RUN pip install -r requirements.txt
├─────────────────────┤
│  基础镜像层          │  ← FROM python:3.11-slim
└─────────────────────┘

每层只存储与下层的差异（增量），节省磁盘空间。
多个镜像可以共享相同的底层。
```

### Copy-on-Write (写时复制)

- 读取文件时：从最上层往下找，找到即返回
- 写入文件时：先把文件从只读层**复制**到可写层，然后在可写层修改
- 删除文件时：在可写层创建一个"白名单"标记，隐藏下层文件

## 3.2 常用镜像命令

### 搜索镜像

```bash
# 在 Docker Hub 搜索镜像
docker search nginx

# 限制搜索结果数
docker search --limit 5 nginx

# 过滤官方镜像
docker search --filter is-official=true nginx

# 按 stars 过滤
docker search --filter stars=100 nginx
```

### 拉取镜像

```bash
# 拉取最新版
docker pull nginx

# 拉取指定版本（tag）
docker pull nginx:1.25-alpine
docker pull python:3.11-slim
docker pull node:20-bookworm

# 等价于完整写法
docker pull docker.io/library/nginx:latest
#         ↑ registry  ↑ namespace   ↑ name ↑ tag
```

### 查看本地镜像

```bash
# 列出所有镜像
docker images
# 或等价的
docker image ls

# 输出列解读：
# REPOSITORY  TAG       IMAGE ID       CREATED        SIZE
# nginx       latest    abc123def456   2 weeks ago    187MB

# 只显示镜像 ID
docker images -q

# 过滤虚悬镜像（dangling images，tag 为 <none>）
docker images -f dangling=true

# 显示完整摘要（digest）
docker images --digests
```

### 查看镜像详情

```bash
# 查看镜像元数据（JSON 格式）
docker inspect nginx:latest

# 查看镜像构建历史（每一层的大小和命令）
docker history nginx:latest
```

### 删除镜像

```bash
# 删除指定镜像
docker rmi nginx:latest
docker image rm nginx:latest

# 强制删除（即使有容器在使用）
docker rmi -f nginx:latest

# 删除所有未使用的镜像
docker image prune
docker image prune -a    # 删除所有未被任何容器使用的镜像

# 批量删除所有镜像
docker rmi $(docker images -q)
```

### 导入/导出镜像

```bash
# 将镜像保存为 tar 文件
docker save -o nginx.tar nginx:latest
docker save nginx:latest | gzip > nginx.tar.gz

# 从 tar 文件加载镜像（适用于离线环境）
docker load -i nginx.tar
docker load < nginx.tar.gz
```

### 镜像标签

```bash
# 给镜像打标签（不复制，只是添加一个引用）
docker tag nginx:latest myrepo/nginx:1.0

# 用于上传到 Docker Hub 或私有仓库
docker tag myapp:latest username/myapp:v1.0
docker push username/myapp:v1.0
```

## 3.3 创建自定义镜像

### 方法一：docker commit（不推荐）

```bash
# 在容器中做了修改后，提交为新镜像
docker run -it ubuntu:22.04 bash
# 在容器内：apt update && apt install -y vim
# exit 退出

docker commit <container-id> my-ubuntu-with-vim:v1

# 缺点：不可复现，不知道镜像里装了什么，像黑盒
```

### 方法二：Dockerfile（标准方式）

```dockerfile
# 创建文件 Dockerfile
FROM ubuntu:22.04

RUN apt update && apt install -y vim && \
    rm -rf /var/lib/apt/lists/*

CMD ["bash"]
```

```bash
docker build -t my-ubuntu-with-vim:v1 .
```

## 3.4 Docker Hub 交互

```bash
# 登录
docker login

# 推送镜像
docker tag myapp:latest suxiangyu138/myapp:v1.0
docker push suxiangyu138/myapp:v1.0

# 拉取
docker pull suxiangyu138/myapp:v1.0

# 登出
docker logout
```

## 3.5 镜像命名规范

```
[registry/][namespace/]name[:tag]

docker.io/library/nginx:latest
gcr.io/my-project/myapp:v2.3
harbor.company.com/team/service:dev
```

- **registry**：镜像仓库地址（省略则默认 Docker Hub）
- **namespace**：命名空间（Docker Hub 是用户名，省略则官方镜像）
- **name**：镜像名称
- **tag**：版本标签（省略则默认为 `latest`）

**最佳实践**：永远不要在生产环境使用 `:latest` 标签，始终指定明确的版本号。

## 3.6 虚悬镜像 (Dangling Images)

```bash
# 什么是虚悬镜像？
# 当多个镜像使用同一个 tag 时，旧的镜像 tag 变为 <none>:<none>
# 表现为 REPOSITORY 和 TAG 都是 <none>

# 查看虚悬镜像
docker images -f dangling=true

# 批量删除虚悬镜像
docker image prune
```

虚悬镜像的产生场景：
1. 重复 `docker build -t same-tag .` 后，旧镜像失去 tag
2. `docker pull` 新版本后，旧版本失去 tag
3. 删除依赖镜像后，中间层失去引用

---

> **下一步**：[04-Docker容器管理](04-Docker容器管理.md)
