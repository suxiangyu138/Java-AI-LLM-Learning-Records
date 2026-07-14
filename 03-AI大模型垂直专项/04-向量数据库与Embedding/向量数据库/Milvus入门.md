# 🗄️ Milvus 完整安装与使用总结

> **核心摘要**：本文涵盖 Milvus 向量数据库从环境准备、安装部署到可视化管理的完整流程，提供 Windows 平台 WSL2 + Docker 部署的详细步骤、常用命令速查与避坑要点，方便快速回顾与操作。

---

## 一、环境准备与安装流程

| 步骤 | 操作说明 | 关键命令 / 要点 |
|---|---|---|
| 1. 前置依赖 | Windows 开启 WSL2 + 安装 Docker Desktop | `wsl --install`，Docker 内存分配 >= 4GB |
| 2. Milvus 安装 | 下载并运行官方单机版脚本 | `mkdir D:\milvus` → `Invoke-WebRequest` 下载脚本 → `.\standalone.bat start` |
| 3. Python 连接 | 安装 SDK 并验证服务连通性 | `pip install pymilvus`，执行连接测试脚本确认无报错 |
| 4. Attu 可视化 | 安装并配置 Web 管理工具 | `docker run -d -p 8000:3000 ... zilliz/attu:v2.5`，地址填 `host.docker.internal:19530` |

### 详细安装步骤

**步骤 1：安装 Docker Desktop**
- 确保 Windows 已开启 WSL2
- Docker Desktop 内存分配不少于 4GB（Settings → Resources → Memory）

**步骤 2：启动 Milvus**
```powershell
mkdir D:\milvus
cd D:\milvus
# 下载官方启动脚本
Invoke-WebRequest -Uri https://raw.githubusercontent.com/milvus-io/milvus/master/deployments/standalone/standalone.bat -OutFile standalone.bat
# 启动 Milvus
.\standalone.bat start
```

**步骤 3：安装 Python SDK**
```bash
pip install pymilvus
```

**步骤 4：部署 Attu 可视化管理工具**
```powershell
docker run -d -p 8000:3000 `
  -e MILVUS_URL=host.docker.internal:19530 `
  --add-host=host.docker.internal:host-gateway `
  zilliz/attu:v2.5
```

> **注意**：Attu 连接 Milvus 必须使用 `host.docker.internal:19530`，不能直接用 `localhost:19530`。

---

## 二、核心服务信息

| 服务 | 访问地址 / 端口 | 说明 |
|---|---|---|
| Milvus 服务 | `localhost:19530` | 向量数据库核心服务（gRPC 端口） |
| Attu 管理界面 | `http://localhost:8000` | 可视化管理，查看集合 / 数据 / 索引 |
| 数据存储目录 | `D:\milvus\volumes` | Milvus 数据持久化目录，备份需保留 |

---

## 三、常用命令速查

### 3.1 Milvus 服务管理

```powershell
cd D:\milvus
.\standalone.bat start   # 启动 Milvus
.\standalone.bat stop    # 停止 Milvus
.\standalone.bat restart # 重启 Milvus
```

### 3.2 Attu 容器管理

```powershell
docker start attu       # 启动 Attu
docker stop attu        # 停止 Attu
docker restart attu     # 重启 Attu
```

---

## 四、关键使用流程

### 4.1 Python 开发流程

```
连接 Milvus → 创建集合（定义向量字段+业务字段）
→ 插入数据 → 创建索引 → 加载集合 → 向量检索
```

### 4.2 可视化管理流程

```
打开 http://localhost:8000 → 连接 Milvus
→ 查看集合/数据/索引 → 执行查询、管理操作
```

> **重点**：向量检索前必须先创建索引并加载集合到内存，否则无法执行查询。

---

## 五、避坑要点

1. **Docker 内存不足**：启动 Milvus 前，Docker 需分配 >= 4GB 内存，否则会启动失败
2. **Attu 连接地址**：必须使用 `host.docker.internal:19530`，不能用 `localhost:19530`
3. **检索前加载集合**：执行搜索前必须调用 `collection.load()` 将集合加载到内存
4. **数据持久化**：`D:\milvus\volumes` 目录是持久化存储所在，备份和迁移需保留此目录

---

## 六、下一步推荐阅读

- [[快速上手Milvus]] — Milvus 完整代码实战
- [[快速学会 主流向量数据库「全覆盖」]] — 向量数据库全景对比
- [[向量数据库选型与实践：Chroma与FAISS]] — 深入选型实践

---

## 核心要点回顾

- Milvus 需在 WSL2 + Docker 环境下部署，Docker 需分配 >= 4GB 内存
- Attu 是 Milvus 的 Web 可视化管理工具，连接地址必须用 `host.docker.internal`
- 核心使用流程：连接 → 建集合 → 插数据 → 建索引 → 加载 → 检索
- 数据持久化目录 `volumes` 是备份和迁移的关键

## 参考资料

1. Milvus 官方安装指南：https://milvus.io/docs/install_standalone-docker.md
2. Attu 可视化工具：https://github.com/zilliztech/attu
3. pymilvus SDK 文档：https://pypi.org/project/pymilvus/
4. Milvus Java SDK：https://github.com/milvus-io/milvus-sdk-java
