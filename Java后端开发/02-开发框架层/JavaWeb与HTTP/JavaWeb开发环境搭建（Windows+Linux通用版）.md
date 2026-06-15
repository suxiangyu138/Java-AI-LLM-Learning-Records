# JavaWeb 开发环境搭建（Windows + Linux 通用版）

> **文档定位**：Java 后端环境搭建指南 | JavaWeb 开发环境  
> **核心依赖**：JDK（基础）+ Web 服务器（Tomcat）+ 开发工具（IDEA）  
> **版本适配**：推荐 JDK 8 / 17、Tomcat 9 / 10、IDEA 2023+  
> **覆盖平台**：Windows + Linux

---

## 目录

- [一、前置准备：版本选择与下载](#一前置准备版本选择与下载)
- [二、Windows 系统环境搭建](#二windows-系统环境搭建)
- [三、Linux 系统环境搭建](#三linux-系统环境搭建)
- [四、常见问题与解决方案](#四常见问题与解决方案)
- [五、补充说明与安全建议](#五补充说明与安全建议)

---

## 一、前置准备：版本选择与下载

> **核心原则**：优先选择 LTS（长期支持）版本，稳定性更强。

### JDK 选择

| 版本 | 说明 |
|------|------|
| **JDK 8** | 兼容性最强，适配所有主流框架 |
| **JDK 17** | 最新 LTS，推荐追求新特性的开发者 |

| 下载渠道 | 说明 |
|----------|------|
| [Adoptium](https://adoptium.net/) | 开源免费，无商业授权问题（推荐） |
| [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) | 个人学习免费，商业需授权 |

### Tomcat 版本适配

| JDK 版本 | Tomcat 版本 |
|----------|------------|
| JDK 8 | Tomcat 9 |
| JDK 11+ / 17 | **Tomcat 10**（推荐） |

> 下载：[Apache Tomcat](https://tomcat.apache.org/)，Windows 选 `.zip`，Linux 选 `.tar.gz`

### IDEA

- [JetBrains 官网](https://www.jetbrains.com/idea/)
- 社区版（免费）vs 旗舰版（付费，有试用）
- JavaWeb 开发建议旗舰版（完整 Servlet/JSP/Tomcat 集成）

---

## 二、Windows 系统环境搭建

### 步骤 1：安装并配置 JDK

| 操作 | 说明 |
|------|------|
| 安装 JDK | 双击 `.msi`，**路径不含中文和空格**（如 `C:\Program Files\Java\jdk-17`） |
| 配置 `JAVA_HOME` | 系统属性 → 环境变量 → 新建变量 `JAVA_HOME`=JDK 根路径 |
| 配置 `Path` | 编辑 Path → 添加 `%JAVA_HOME%\bin` |
| 验证 | `java -version` + `javac -version` |

### 步骤 2：安装并配置 Tomcat

| 操作 | 说明 |
|------|------|
| 解压 | 解压到无中文/空格目录（如 `D:\apache-tomcat-10`） |
| 配置（可选） | 环境变量 `CATALINA_HOME`=Tomcat 解压路径，Path 加 `%CATALINA_HOME%\bin` |
| 启动 | `bin\startup.bat` |
| 验证 | 访问 `http://localhost:8080`，出现欢迎页即成功 |
| 停止 | `bin\shutdown.bat` |

> **端口冲突**：修改 `conf\server.xml` 中 `<Connector port="8080">` 的值。

### 步骤 3：安装 IDEA 并配置

| 操作 | 路径 |
|------|------|
| 配置 JDK | File → Project Structure → Project SDK 选择 JDK |
| 创建 JavaWeb 项目 | New Project → Jakarta EE → 勾选 Web Application |
| 配置 Tomcat | Add Configuration → Tomcat Server → Local → 选择 Tomcat 路径 |
| 启动部署 | 点击启动，访问 `http://localhost:8080/项目名` |

---

## 三、Linux 系统环境搭建

### 步骤 1：安装 JDK

```bash
# 解压
tar -zxvf /opt/jdk-xxx.tar.gz -C /opt
mv /opt/jdk-xxx /opt/jdk

# 配置环境变量
vim ~/.bashrc
# 添加：
export JAVA_HOME=/opt/jdk
export PATH=$JAVA_HOME/bin:$PATH

# 生效
source ~/.bashrc

# 验证
java -version && javac -version
```

### 步骤 2：安装 Tomcat

```bash
# 解压
tar -zxvf /opt/tomcat-xxx.tar.gz -C /opt
mv /opt/tomcat-xxx /opt/tomcat

# 配置（可选）
vim ~/.bashrc
# 添加：
export CATALINA_HOME=/opt/tomcat
source ~/.bashrc

# 启动 / 停止
cd $CATALINA_HOME/bin
./startup.sh       # 启动
./shutdown.sh      # 停止
curl http://localhost:8080  # 验证
```

### Tomcat 管理员配置（可选）

```xml
<!-- conf/tomcat-users.xml -->
<role rolename="manager-gui"/>
<role rolename="admin-gui"/>
<user username="admin" password="admin123" roles="manager-gui,admin-gui"/>
```

---

## 四、常见问题与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `javac` 不是内部命令 | `JAVA_HOME` 或 `Path` 配置错误 | 检查路径是否正确，重启 cmd |
| Tomcat 黑窗口一闪而过 | JDK 未配置或版本不匹配 | `java -version` 验证，检查版本适配 |
| `localhost:8080` 无法访问 | Tomcat 未启动或端口冲突 | `netstat -ano \| findstr 8080` 排查 |
| IDEA 无法识别 JDK | SDK 未配置 | Project Structure → Add SDK 手动选择 |

---

## 五、补充说明与安全建议

| 项目 | 建议 |
|------|------|
| **版本适配优先级** | JDK 版本 → Tomcat 版本 → IDEA 版本（避免跨版本） |
| **生产环境安全** | 禁用 Tomcat 管理界面，限制 IP 白名单，定期更新版本 |
| **项目部署** | WAR 包放入 `webapps` 目录自动解压部署 |
