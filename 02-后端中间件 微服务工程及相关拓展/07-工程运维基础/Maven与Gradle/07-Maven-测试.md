# Maven 测试

## 一、测试概述

Maven 测试核心依托 **maven-surefire-plugin**（单元测试）和 **maven-failsafe-plugin**（集成测试），与 JUnit/TestNG 等框架协同工作。

### 默认约定

| 约定 | 说明 |
|------|------|
| 测试源码目录 | `src/test/java` |
| 测试资源目录 | `src/test/resources` |
| 测试类命名 | `**/Test*.java`、`**/*Test.java`、`**/*Tests.java` |
| 测试方法 | `@Test` 注解（JUnit）或 `test` 前缀 |
| 执行流程 | `mvn test` → 先编译主代码 → 编译测试代码 → 执行测试 |

## 二、maven-surefire-plugin（单元测试）

### 2.1 基础配置

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.1.2</version>
            <configuration>
                <encoding>UTF-8</encoding>
                <testFailureIgnore>false</testFailureIgnore>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/Test*.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IntegrationTest.java</exclude>
                </excludes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2.2 JUnit 5 专属配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.1.2</version>
    <dependencies>
        <dependency>
            <groupId>org.apache.maven.surefire</groupId>
            <artifactId>surefire-junit5-engine</artifactId>
            <version>3.1.2</version>
        </dependency>
    </dependencies>
</plugin>

<!-- 配合 JUnit 5 依赖 -->
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-api</artifactId>
        <version>5.9.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-engine</artifactId>
        <version>5.9.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2.3 跳过测试

```bash
# 跳过测试执行（仍编译测试代码）—— 推荐
mvn clean package -DskipTests

# 完全跳过（不编译也不执行）
mvn clean package -Dmaven.test.skip=true
```

```xml
<!-- pom.xml 永久跳过（不推荐） -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <skipTests>true</skipTests>
    </configuration>
</plugin>
```

## 三、maven-failsafe-plugin（集成测试）

集成测试使用独立插件，测试类命名约定为 `*IntegrationTest.java`：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.1.2</version>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
            <configuration>
                <encoding>UTF-8</encoding>
                <includes>
                    <include>**/*IntegrationTest.java</include>
                </includes>
                <testFailureIgnore>false</testFailureIgnore>
            </configuration>
        </execution>
    </executions>
</plugin>
```

```bash
# 执行单元测试 + 集成测试
mvn clean verify
```

## 四、测试类型划分

| 类型 | 命名约定 | 说明 | 执行命令 |
|------|---------|------|---------|
| 单元测试 | `*Test.java` | 测试单个类/方法，不依赖外部资源 | `mvn test` |
| 集成测试 | `*IntegrationTest.java` | 测试模块协作，需数据库/Redis 等 | `mvn verify` |
| 接口测试 | `*ApiTest.java` | 测试 Controller 层 API | `mvn test` |

### 单元测试示例（JUnit 5 + Mockito）

```java
package com.company.project.service;

import com.company.project.dao.UserDao;
import com.company.project.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserService userService;

    @Test
    void testSelectById_ValidId_ReturnsUser() {
        User mockUser = new User(1L, "test");
        when(userDao.selectById(1L)).thenReturn(mockUser);

        User result = userService.selectById(1L);

        assertNotNull(result);
        assertEquals("test", result.getUsername());
    }
}
```

## 五、测试报告

### 5.1 Surefire 测试报告

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-report-plugin</artifactId>
    <version>3.1.2</version>
    <configuration>
        <encoding>UTF-8</encoding>
        <showSuccess>true</showSuccess>
    </configuration>
</plugin>
```

```bash
mvn clean test
# 报告路径：target/site/surefire-report.html
```

### 5.2 JaCoCo 代码覆盖率

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

```bash
mvn clean test
# 报告路径：target/site/jacoco/index.html
```

**企业级覆盖要求**：核心业务代码行覆盖率 ≥ 80%，方法覆盖率 ≥ 70%。

### 5.3 多模块覆盖率汇总

```xml
<!-- 父 POM 添加聚合报告 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <id>report-aggregate</id>
            <phase>test</phase>
            <goals>
                <goal>report-aggregate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## 六、CI/CD 集成要点

```yaml
# Jenkins Pipeline 示例
stage('Test') {
    steps {
        sh 'mvn clean verify'
    }
    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            publishHTML(target: [
                reportDir: 'target/site/jacoco',
                reportFiles: 'index.html',
                reportName: 'JaCoCo Coverage'
            ])
        }
    }
}
```

## 七、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `Tests run: 0` | 测试类命名不符合约定 | 确保类名以 `Test` 开头或结尾 |
| `ClassNotFoundException` | 依赖范围错误 | 检查 scope 配置，先 `mvn compile` |
| 覆盖率 0% | JaCoCo agent 未启动 | 确认 `prepare-agent` 目标已配置 |
| 中文乱码 | 编码不一致 | 所有插件统一 `<encoding>UTF-8</encoding>` |
| 集成测试连不上 DB | 测试环境未就绪 | 检查 application-test.yml 配置 |
| 多模块测试依赖失败 | 模块未 install | 先 `mvn clean install` |
