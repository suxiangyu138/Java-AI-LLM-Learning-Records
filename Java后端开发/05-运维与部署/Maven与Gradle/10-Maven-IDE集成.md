# Maven IDE 集成

## 一、IntelliJ IDEA 集成（推荐）

### 1.1 关联本地 Maven

`File → Settings → Build, Execution, Deployment → Build Tools → Maven`

| 配置项 | 值 |
|-------|-----|
| **Maven home path** | 本地 Maven 安装目录 |
| **User settings file** | `%MAVEN_HOME%/conf/settings.xml` |
| **Local repository** | 本地仓库路径（自动识别 settings.xml 配置） |

### 1.2 推荐设置

| 设置 | 位置 | 说明 |
|------|------|------|
| 自动导入依赖 | `Settings → Maven → Importing` | 勾选 `Import Maven projects automatically` |
| Runner JRE | `Settings → Maven → Runner` | 选择 JDK 8+（与项目一致） |
| 源码目录标记 | 右键目录 → `Mark Directory as` | `src/main/java` → Sources Root |
| 资源目录标记 | 同上 | `src/main/resources` → Resources Root |
| 测试目录标记 | 同上 | `src/test/java` → Test Sources Root |

### 1.3 Maven 面板操作

打开右侧 `Maven` 面板：

| 操作 | 方式 |
|------|------|
| 执行生命周期 | 展开 `Lifecycle`，双击命令（clean、compile、package 等） |
| 运行插件 | 展开 `Plugins`，双击具体 goal |
| 刷新依赖 | 点击刷新图标 或 右键 pom.xml → `Reload Project` |
| 查看依赖图 | 右键项目 → `Maven` → `Show Diagram`（可视化依赖关系） |
| 跳过测试打包 | 按住 `Ctrl` 双击 `package`（等同于 `-DskipTests`） |

### 1.4 IDEA 创建 Maven 项目

1. `File → New → Project`
2. 选择 **Maven**，取消勾选 `Create from archetype`（灵活自定义结构）
3. 填写 GroupId、ArtifactId、Version
4. 完成创建后手动补充包结构

## 二、Eclipse 集成

### 2.1 安装 Maven 插件（M2E）

Eclipse 2020-06+ 已内置 M2Eclipse 插件。若需手动安装：

1. `Help → Install New Software`
2. 添加仓库：`https://download.eclipse.org/technology/m2e/releases/latest/`
3. 勾选：
   - `Maven Integration for Eclipse`（核心）
   - `Maven Integration for Eclipse WTP`（Web 项目）
4. 按向导完成安装，重启 Eclipse

### 2.2 关联本地 Maven

`Window → Preferences → Maven → Installations`

1. 点击 `Add`，选择本地 Maven 安装目录
2. 勾选新添加的 Maven 版本

`Window → Preferences → Maven → User Settings`

1. 指定 `settings.xml` 路径
2. 点击 `Update Settings`

### 2.3 Eclipse Maven 操作

| 操作 | 方式 |
|------|------|
| 创建 Maven 项目 | `File → New → Other → Maven → Maven Project` |
| 执行构建 | 右键项目 → `Run As → Maven build...` → 输入命令 |
| 刷新依赖 | 右键项目 → `Maven → Update Project` |
| 源码目录 | 右键 `src/main/java` → `Build Path → Use as Source Folder` |

## 三、VS Code 集成

通过 **Maven for Java** 扩展：

```bash
# 安装扩展
code --install-extension vscjava.vscode-maven
```

侧边栏 **MAVEN** 面板提供完整的生命周期和插件操作。

## 四、通用配置要点

### 4.1 编码统一配置

```xml
<!-- pom.xml -->
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
</properties>
```

### 4.2 JDK 版本统一

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>1.8</source>
        <target>1.8</target>
        <encoding>UTF-8</encoding>
    </configuration>
</plugin>
```

## 五、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `Cannot resolve plugin` | 插件版本不兼容或下载失败 | 更换插件版本，执行 `mvn clean install -U` |
| 依赖标红 | 未下载或本地仓库损坏 | 刷新依赖 / 删除本地仓库目录重新下载 |
| 项目结构不识别 | IDEA 未正确识别目录类型 | 手动 Mark Directory as |
| Maven 命令不可用 | IDE 关联的是内置 Maven | 改为手动安装的 Maven 版本 |
| 中文注释乱码 | 编码配置缺失 | 全局 + 项目编码统一为 UTF-8 |
| 编译报"找不到符号" | JDK 版本不匹配 | 检查 IDE 和 Maven 使用的 JDK 版本一致 |
