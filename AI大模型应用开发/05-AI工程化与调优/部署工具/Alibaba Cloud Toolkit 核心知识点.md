# Alibaba Cloud Toolkit 核心知识点

## 一、概述

Alibaba Cloud Toolkit 是阿里云推出的 IDE 插件，让开发者能在 IntelliJ IDEA / Eclipse / VS Code 内一键部署应用到云端服务器（ECS、EDAS、Kubernetes 等），同时支持文件上传、远程命令执行、日志查看等运维操作。

**核心定位：** 一站式云开发部署工具，**将部署流程从命令行迁移到 IDE 内完成**，大幅提升 Java 后端上云效率。

**官方文档：** https://help.aliyun.com/product/29966.html

## 二、核心能力

| 功能 | 说明 |
|------|------|
| **一键部署** | 从 IDE 直接部署本地应用到远程 ECS / EDAS / Kubernetes |
| **文件上传** | FTP/SFTP 上传文件到远程服务器 |
| **远程命令** | 在 IDE 终端执行服务器 Shell 命令 |
| **远程调试** | Java 应用远程 Debug |
| **日志查看** | 实时查看服务器应用日志 |
| **Docker 部署** | Dockerfile + Docker Compose 一键构建部署 |

## 三、快速上手

### 3.1 安装

- **IntelliJ IDEA**：Settings → Plugins → 搜索 "Alibaba Cloud Toolkit" → 安装
- **VS Code**：扩展商店搜索 "Alibaba Cloud Toolkit"
- **Eclipse**：Help → Eclipse Marketplace → 搜索安装

### 3.2 ECS 部署配置

```
1. 配置服务器连接
   Alibaba Cloud Toolkit → Deploy to Host
   Host: 192.168.1.100
   Port: 22
   Username: admin
   Authentication: 密码 / SSH 密钥

2. 配置部署参数
   ├── 上传文件：target/*.jar
   ├── 目标路径：/opt/app/
   ├── 部署后执行：systemctl restart myapp
   └── 部署前备份：mv /opt/app/*.jar /opt/backup/
```

### 3.3 一键部署

```
步骤：
1. Maven 打包：mvn clean package -DskipTests
2. 右键 *.jar → Deploy to Host
3. 选择目标服务器 → Deploy
4. 自动完成：上传 → 备份 → 重启应用
5. 打开日志终端，验证启动成功
```

## 四、核心功能详解

### 4.1 Deploy to Host（ECS 部署）

```yaml
部署配置（Upload File）:
  Name: 生产环境部署
  Target Host: 选择已配置的服务器
  File:
    Local File: target/myapp-1.0.0.jar
    Remote Path: /opt/myapp/
  Commands:
    Before Deploy:
      - cp /opt/myapp/*.jar /opt/backup/$(date +%Y%m%d%H%M%S).jar
    After Deploy:
      - systemctl restart myapp
      - sleep 5
      - curl -f http://localhost:8080/actuator/health || exit 1
```

### 4.2 远程终端

```
Alibaba Cloud Toolkit → 选择服务器 → Open Terminal
  → 在 IDE 内直接操作远程服务器 Shell
  → 无需切换终端或 Xshell
```

### 4.3 远程调试

```
1. 在远程服务器 JVM 启动参数添加：
   java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar myapp.jar

2. IDEA → Run → Edit Configurations → Remote JVM Debug
   Host: 远程服务器 IP
   Port: 5005

3. 点击 Debug 按钮，即可断点调试远程代码
```

### 4.4 日志查看

```
Alibaba Cloud Toolkit → Log Viewer
  → 选择服务器 → 指定日志路径
  → 实时 tail -f 日志流
  → 支持关键字过滤高亮
```

## 五、与 CI/CD 流水线的定位

```
日常开发调试（Cloud Toolkit）
  → IDE 内快速验证，适合开发/测试环境

正式发布（CI/CD 流水线）
  → GitHub Actions / Jenkins / 阿里云效
  → 审核 + 自动化测试 + 灰度发布
```

## 六、替代品

| 工具 | 特点 |
|------|------|
| **FinalShell** | 国产 SSH 客户端 + 文件管理 + 监控一体化 |
| **Xshell + Xftp** | 老牌 SSH 工具，运维人员常用 |
| **JetBrains Remote Development** | IDEA 原生远程开发，代码在远端 |
| **VS Code Remote SSH** | VS Code 远程开发方案 |

## 七、总结

Alibaba Cloud Toolkit 的核心价值在于 **开发到部署的零切换**——编码、打包、部署、排查全在 IDE 内完成。对阿里云生态用户尤为便利。非阿里云环境可使用 FinalShell 替代，或通过 CI/CD 流水线实现自动化部署。
