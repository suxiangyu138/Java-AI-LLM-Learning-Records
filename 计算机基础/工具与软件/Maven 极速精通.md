Maven 极速精通
一、Maven 核心3个作用
1. 依赖管理：告别手动导 Jar，一行配置引依赖
2. 项目构建：编译、测试、打包、一键部署
3. 项目标准化：所有Java项目目录统一，协作无壁垒
    二、核心结构：坐标（唯一标识）
    每个项目/依赖，靠3个值定位
    xml
    <!-- 组织/公司域名反写 -->
    <groupId>com.example</groupId>
    <!-- 项目/模块名 -->
    <artifactId>demo</artifactId>
    <!-- 版本号 -->
    <version>1.0-SNAPSHOT</version>
 
-  SNAPSHOT ：快照版（开发中，频繁更新）
-  RELEASE ：正式稳定版
 
三、核心配置：pom.xml 必写标签
1. 引入依赖（最常用）
    xml
    <dependencies>
    <!-- 示例：MySQL驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.0.33</version>
        <!-- 可选：作用域 -->
        <scope>runtime</scope>
    </dependency>
    </dependencies>
 
2. 依赖作用域 scope（必考）
    -  compile ：默认，全程有效（编译+运行）
    -  runtime ：只运行有效，编译不参与（数据库驱动常用）
    -  test ：仅测试代码生效（junit）
    -  provided ：容器/服务器提供，打包不带入（servlet-api）
3. 版本统一管理（多模块项目必备）
    xml
    <dependencyManagement>
    <dependencies>
        <!-- 统一锁定版本，子模块无需写version -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.7.15</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
 
 
四、Maven 三大仓库
1. 本地仓库：本机文件夹，缓存所有下载的Jar
2. 中央仓库：Maven官方，外网慢
3. 镜像仓库（必配）：阿里云镜像，加速下载
    阿里云镜像配置（直接复制）
    修改  settings.xml 
    xml
    <mirrors>
    <mirror>
        <id>aliyunmaven</id>
        <name>阿里云公共仓库</name>
        <url>https://maven.aliyun.com/repository/public</url>
        <mirrorOf>central</mirrorOf>
    </mirror>
    </mirrors>
 
 
五、Maven 生命周期 & 常用命令
执行顺序： clean → compile → test → package → install → deploy 
bash

# 清空target编译产物
mvn clean

# 编译main目录代码
mvn compile

# 执行测试用例
mvn test

# 打包：生成jar/war包
mvn package

# 安装到本地仓库，给本地其他项目引用
mvn install

# 发布到公司私服（企业用）
mvn deploy
 
 
六、依赖核心机制
1. 依赖传递
    引入A依赖，A依赖的B会自动导入，不用重复写
2. 依赖冲突
    多个依赖引入不同版本Jar，Maven默认：
    - 就近原则
    - 路径短优先
3. 排除冲突依赖
    xml
    <dependency>
    <groupId>xxx</groupId>
    <artifactId>xxx</artifactId>
    <exclusions>
        <exclusion>
            <groupId>冲突包groupId</groupId>
            <artifactId>冲突包artifactId</artifactId>
        </exclusion>
    </exclusions>
 
 
七、标准目录结构（固定死）
plaintext
项目根目录
├── src
│   ├── main
│   │   ├── java        # 业务代码
│   │   └── resources   # 配置文件(yaml/yml/properties)
│   └── test
│       └── java        # 测试代码
├── pom.xml             # Maven核心配置文件
└── target              # 编译、打包输出目录(自动生成)
 
 
八、IDEA / VSCode 实操要点
1. 改完  pom.xml  → 点击刷新Maven，自动下载依赖
2. 右侧Maven面板：快捷点击命令（clean、package）
3. 可配置本地Maven路径+ settings.xml ，不用默认内置
 
九、你后端必存常用依赖（直接CV）
xml
<!-- Spring Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- MyBatis -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
</dependency>
<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
 
 
十、极简总结
1. Maven = 依赖管理 + 项目构建
2. 核心： pom.xml  + 坐标 + 阿里云镜像
3. 日常只用： clean、package、install  三个命令
4. 微服务、SpringCloud Alibaba 完全基于Maven，必须熟练
