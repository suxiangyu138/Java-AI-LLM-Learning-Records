# Milvus 完整安装与使用总结
以下是你从安装到使用 Milvus 的完整流程总结，方便你后续快速回顾与操作：

---

## 一、环境准备与安装流程
| 步骤 | 操作说明 | 关键命令/要点 |
| :--- | :--- | :--- |
| 1. 前置依赖 | Windows 开启 WSL2 + 安装 Docker Desktop | `wsl --install`，Docker 内存分配 ≥4GB |
| 2. Milvus 安装 | 下载并运行官方单机版脚本 | `mkdir D:\milvus` → `Invoke-WebRequest` 下载脚本 → `.\standalone.bat start` |
| 3. Python 连接 | 安装 SDK 并验证服务连通性 | `pip install pymilvus`，执行连接测试脚本，确认无报错 |
| 4. Attu 可视化 | 安装并配置 Web 管理工具 | `docker run -d -p 8000:3000 -e MILVUS_URL=host.docker.internal:19530 --add-host=host.docker.internal:host-gateway zilliz/attu:v2.5`，地址填 `host.docker.internal:19530` |

---

## 二、核心服务信息
| 服务 | 访问地址/端口 | 说明 |
| :--- | :--- | :--- |
| Milvus 服务 | `localhost:19530` | 向量数据库核心服务 |
| Attu 管理界面 | `http://localhost:8000` | 可视化管理、查看集合/数据/索引 |
| 数据存储目录 | `D:\milvus\volumes` | Milvus 数据持久化目录，备份需保留 |

---

## 三、常用命令速查
### 1. Milvus 服务管理
```powershell
cd D:\milvus
.\standalone.bat start   # 启动 Milvus
.\standalone.bat stop    # 停止 Milvus
.\standalone.bat restart # 重启 Milvus
```

### 2. Attu 容器管理
```powershell
docker start attu    # 启动 Attu
docker stop attu     # 停止 Attu
docker restart attu  # 重启 Attu
```

---

## 四、关键使用流程
1.  **Python 开发流程**：
    连接 Milvus → 创建集合（定义向量字段+业务字段）→ 插入数据 → 创建索引 → 加载集合 → 向量检索
2.  **可视化管理流程**：
    打开 `http://localhost:8000` → 连接 Milvus → 查看集合/数据/索引 → 执行查询、管理操作

---

## 五、避坑要点
- 启动 Milvus 前，Docker 内存需分配 ≥4GB，否则会因内存不足启动失败
- Attu 连接 Milvus 必须使用 `host.docker.internal:19530`，不能直接用 `localhost:19530`
- 向量检索前必须先创建索引并加载集合到内存，否则无法执行查询

---
