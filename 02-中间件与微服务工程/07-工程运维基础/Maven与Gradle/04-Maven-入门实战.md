# Maven 入门实战

## 一、第一个 Maven 项目

### 1.1 创建项目（IDEA）

1. `File → New → Project`，选择 **Maven**，取消勾选 `Create from archetype`
2. 配置坐标：

| 属性 | 值 |
|------|-----|
| GroupId | `com.example.backend` |
| ArtifactId | `demo-util` |
| Version | `1.0.0` |

3. 完成项目创建后，补充标准包结构：

```
demo-util/
├── src/main/java/com/example/backend/
│   ├── entity/        # 实体类
│   └── util/          # 工具类
├── src/test/java/com/example/backend/
│   └── util/          # 测试类
└── pom.xml
```

### 1.2 配置 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.backend</groupId>
    <artifactId>demo-util</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 1.3 编写代码

**User 实体类** (`src/main/java/.../entity/User.java`)：

```java
package com.example.backend.entity;

public class User {
    private Integer id;
    private String name;
    private Integer age;

    public User() {}
    public User(Integer id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    // getters & setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', age=" + age + "}";
    }
}
```

**UserUtil 工具类** (`src/main/java/.../util/UserUtil.java`)：

```java
package com.example.backend.util;

import com.example.backend.entity.User;

public class UserUtil {

    /** 校验用户信息合法性 */
    public static boolean checkUser(User user) {
        if (user == null) return false;
        return user.getId() != null
            && user.getName() != null && !user.getName().trim().isEmpty()
            && user.getAge() != null && user.getAge() > 0 && user.getAge() <= 120;
    }

    /** 格式化用户信息 */
    public static String formatUser(User user) {
        if (!checkUser(user)) return "用户信息不合法，无法格式化！";
        return "用户ID：" + user.getId()
            + "，用户姓名：" + user.getName()
            + "，用户年龄：" + user.getAge() + "岁";
    }
}
```

**测试类** (`src/test/java/.../util/UserUtilTest.java`)：

```java
package com.example.backend.util;

import com.example.backend.entity.User;
import org.junit.Test;

public class UserUtilTest {

    @Test
    public void testCheckUser() {
        // 合法用户
        User validUser = new User(1, "张三", 20);
        assert UserUtil.checkUser(validUser) : "合法用户校验失败";

        // 姓名为空
        User invalidUser1 = new User(2, "", 25);
        assert !UserUtil.checkUser(invalidUser1) : "不合法用户校验失败";

        // 年龄为 0
        User invalidUser2 = new User(3, "李四", 0);
        assert !UserUtil.checkUser(invalidUser2) : "年龄为 0 校验失败";
    }

    @Test
    public void testFormatUser() {
        User validUser = new User(1, "张三", 20);
        String result = UserUtil.formatUser(validUser);
        assert result.contains("张三") : "格式化合法用户失败";

        User invalidUser = new User(2, "", 25);
        String result2 = UserUtil.formatUser(invalidUser);
        assert result2.contains("不合法") : "格式化不合法用户失败";
    }
}
```

### 1.4 构建全流程

```bash
# 编译
mvn clean compile
# → 生成 target/classes

# 测试
mvn clean test
# → 执行测试用例，输出测试报告

# 打包
mvn clean package
# → 生成 target/demo-util-1.0.0.jar

# 安装到本地仓库（供其他项目引用）
mvn clean install
# → 安装到本地仓库 ~/.m2/repository/...

# 其他项目引用
# <dependency>
#     <groupId>com.example.backend</groupId>
#     <artifactId>demo-util</artifactId>
#     <version>1.0.0</version>
# </dependency>
```

## 二、常用 Maven 命令速查

| 命令 | 说明 |
|------|------|
| `mvn clean` | 删除 target 目录 |
| `mvn compile` | 编译源码 |
| `mvn test` | 运行测试 |
| `mvn package` | 打包为 jar/war |
| `mvn install` | 安装到本地仓库 |
| `mvn deploy` | 部署到远程仓库 |
| `mvn clean package -DskipTests` | 跳过测试直接打包 |
| `mvn dependency:tree` | 查看依赖树 |
| `mvn dependency:analyze` | 分析未使用/未声明的依赖 |

## 三、项目构建生命周期关系

```
mvn clean           → clean 生命周期
mvn compile         → default 生命周期阶段（c: compile）
mvn test            → default 生命周期阶段（c: compile → test）
mvn package         → default 生命周期阶段（c: compile → test → package）
mvn install         → default 生命周期阶段（c: compile → test → package → install）
mvn deploy          → default 生命周期阶段（c: compile → test → package → install → deploy）
```

## 四、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `@Test` 注解标红 | Junit 依赖未下载 | 点击 IDEA 的 `Load Maven Changes` |
| 编译找不到包 | 包路径错误或依赖缺失 | 检查 import 和依赖坐标 |
| 测试断言失败 | 代码逻辑与预期不符 | 调试代码逻辑 |
| 打包后无 jar | 未配置打包插件 | 添加 `maven-jar-plugin` |
| 其他项目无法引用 | 未 install 到本地仓库 | 先执行 `mvn clean install` |
