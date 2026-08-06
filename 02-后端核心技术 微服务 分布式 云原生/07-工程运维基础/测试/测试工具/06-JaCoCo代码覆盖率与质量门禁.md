# JaCoCo 代码覆盖率与质量门禁

> 代码覆盖率工具的事实标准，用于度量测试对代码的覆盖程度，并结合 CI/CD 流水线设置质量门禁，确保项目代码质量。

---

## 目录

1. [代码覆盖率概念](#1-代码覆盖率概念)
2. [JaCoCo 概述](#2-jacoco-概述)
3. [Maven 插件配置](#3-maven-插件配置)
4. [报告格式与解读](#4-报告格式与解读)
5. [核心 Goals](#5-核心-goals)
6. [覆盖率检查规则](#6-覆盖率检查规则)
7. [排除规则配置](#7-排除规则配置)
8. [SonarQube + JaCoCo 集成](#8-sonarqube--jacoco-集成)
9. [CI/CD 集成](#9-cicd-集成)
10. [JaCoCo 的局限性](#10-jacoco-的局限性)
11. [变异测试（PIT）简介](#11-变异测试pit简介)
12. [完整示例：SpringBoot 项目配置 80% 覆盖率门禁](#12-完整示例springboot-项目配置-80-覆盖率门禁)
13. [最佳实践](#13-最佳实践)

---

## 1. 代码覆盖率概念

### 1.1 覆盖率类型

代码覆盖率（Code Coverage）衡量测试代码对生产代码的覆盖程度。JaCoCo 支持以下五种覆盖率指标：

| 指标类型 | 说明 | 计算公式 | 度量粒度 |
|----------|------|----------|----------|
| **行覆盖率（Line）** | 代码行是否被执行 | 执行行数 / 总行数 | 单行 |
| **分支覆盖率（Branch）** | 条件语句（if/else/switch）的每个分支是否被覆盖 | 执行分支数 / 总分指数 | 分支 |
| **方法覆盖率（Method）** | 方法是否被调用 | 执行方法数 / 总方法数 | 方法 |
| **类覆盖率（Class）** | 类是否涉及（至少一个方法被执行） | 涉及类数 / 总类数 | 类 |
| **指令覆盖率（Instruction）** | Java 字节码指令是否被执行（最细粒度） | 执行指令数 / 总指令数 | 字节码指令 |

### 1.2 各指标对比

```java
public class CoverageExample {

    public String checkStatus(int score) {
        if (score >= 60) {             // ← 分支点：2个分支（true / false）
            return "pass";             // ← 1 条指令 / 1 行
        } else {
            return "fail";             // ← 1 条指令 / 1 行
        }
    }

    public void unusedMethod() {
        // 这个方法如果没被测试调用 → 方法覆盖率下降
    }
}
```

假设测试只传入了 `score = 80`：

| 指标 | 覆盖情况 | 覆盖率 |
|------|----------|--------|
| 行覆盖率 | `score >= 60` 和 `return "pass"` 执行了，`return "fail"` 未执行 | 2/3 = 66% |
| 分支覆盖率 | `true` 分支覆盖，`false` 分支未覆盖 | 1/2 = 50% |
| 方法覆盖率 | `checkStatus` 被调用，`unusedMethod` 未调用 | 1/2 = 50% |
| 类覆盖率 | 类被涉及 | 100% |
| 指令覆盖率 | 3 条指令被执行，1 条未执行 | 75% |

> ⚠️ **行覆盖率 ≠ 分支覆盖率**：即使执行到了 if 那一行，也不意味着 if 的两个分支都被执行了。行覆盖率 66% 的情况下，分支覆盖率可能只有 50%。

### 1.3 覆盖率的"谎言"

```
常见的覆盖率误解：

❌ "我的行覆盖率 90%，测试很充分"
   → 可能只测了正常路径，异常路径完全没覆盖

❌ "覆盖率 100% 就代表没有 Bug"
   → 覆盖率只测"代码被执行了"，不测"逻辑是否正确"

✅ 覆盖率是下限指标——告诉你有多少代码完全没测试
❌ 覆盖率不能作为上限指标——不能保证测试质量
```

---

## 2. JaCoCo 概述

### 2.1 什么是 JaCoCo

JaCoCo（Java Code Coverage）是一个开源的 Java 代码覆盖率工具，自 2009 年起成为 Java 生态中最广泛使用的覆盖率工具。它是 **EclEmma**（Eclipse 的覆盖率插件）的底层引擎。

**核心能力：**
- 字节码插桩（on-the-fly / offline 两种模式）
- 生成 HTML / XML / CSV 多种报告格式
- 覆盖率规则检查与质量门禁
- 与 Maven / Gradle / SonarQube / CI/CD 深度集成

### 2.2 JaCoCo 工作原理

```
┌─────────────────────────────────────────────────────────────┐
│ JaCoCo 工作流程                                              │
│                                                              │
│  编译阶段          测试执行阶段         报告生成阶段          │
│                                                              │
│  .java ──► .class ──► JaCoCo 插桩 ──► 测试运行 ──► .exec    │
│                          │                  │               │
│                     on-the-fly          记录执行            │
│                     (JVM agent)         数据文件            │
│                                                              │
│  .exec ──► Report Goal ──► HTML/XML/CSV                     │
│  .exec ──► Check Goal  ──► 覆盖率检查（门禁）               │
└─────────────────────────────────────────────────────────────┘
```

JaCoCo 支持两种插桩模式：

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| **On-the-fly** | JVM 启动时通过 Java Agent 动态插桩 | Maven `prepare-agent` goal，最常用 |
| **Offline** | 编译后对 class 文件进行静态插桩 | Android 项目、不支持 Java Agent 的环境 |

> 💡 绝大多数 Java 后端项目使用 On-the-fly 模式即可，无需配置 Offline。

---

## 3. Maven 插件配置

### 3.1 基础配置

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <!-- 1. 准备 Agent（在 test 阶段前启动） -->
                <execution>
                    <id>prepare-agent</id>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>

                <!-- 2. 生成报告（在 test 阶段后执行） -->
                <execution>
                    <id>report</id>
                    <phase>prepare-package</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>

                <!-- 3. 覆盖率检查（在 verify 阶段执行） -->
                <execution>
                    <id>check</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>...</rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 3.2 Gradle 配置

```groovy
plugins {
    id 'java'
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.12"
}

// 生成报告
test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}

// 覆盖率检查
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.80
            }
        }
    }
}

check {
    dependsOn jacocoTestCoverageVerification
}
```

### 3.3 Agent 配置参数详解

```xml
<execution>
    <id>prepare-agent</id>
    <goals>
        <goal>prepare-agent</goal>
    </goals>
    <configuration>
        <!-- 输出文件路径（默认为 target/jacoco.exec） -->
        <destFile>${project.build.directory}/jacoco.exec</destFile>

        <!-- 附加 JVM 参数 -->
        <propertyName>jacoco.agent.argLine</propertyName>

        <!-- 包含的类路径模式（默认包含所有） -->
        <includes>
            <include>com/example/**</include>
        </includes>

        <!-- 排除的类路径模式 -->
        <excludes>
            <exclude>**/*Config.*</exclude>
            <exclude>**/*DTO.*</exclude>
        </excludes>
    </configuration>
</execution>
```

---

## 4. 报告格式与解读

### 4.1 报告格式

| 格式 | 路径 | 用途 |
|------|------|------|
| **HTML** | `target/site/jacoco/index.html` | 开发人员人工查看覆盖率详情 |
| **XML** | `target/site/jacoco/jacoco.xml` | SonarQube 等工具解析 |
| **CSV** | `target/site/jacoco/jacoco.csv` | 电子表格处理 |

### 4.2 HTML 报告颜色编码

```
报告颜色编码规则：

┌───────────────────────────────────────────────────────────┐
│  元素        │  绿色（覆盖）  │  黄色（部分覆盖）│  红色（未覆盖）│
├───────────────┼───────────────┼─────────────────┼────────────────┤
│  行（背景色）  │  全部执行      │  部分分支未覆盖    │  完全未执行    │
│  菱形（分支）  │  所有分支覆盖  │  部分分支覆盖     │  无分支覆盖    │
│  圆圈（方法）  │  全部指令覆盖  │  部分指令覆盖     │  全部指令未覆盖│
└───────────────────────────────────────────────────────────┘

实际颜色示例：
  🟢 ████████ 100% 覆盖（绿色背景）
  🟡 ██████░░ 66%  覆盖（黄色背景）
  🔴 ██░░░░░░ 25%  覆盖（红色背景）
```

### 4.3 解读 JaCoCo HTML 报告

JaCoCo 的 HTML 报告页面结构如下：

```
index.html（概览页面）
├── com.example.service       包级别：80% 行覆盖，75% 分支覆盖
│   ├── OrderService.java     类级别
│   │   ├── placeOrder()      方法级别：100% 行覆盖，100% 分支覆盖 🟢
│   │   ├── refundOrder()     方法级别：90% 行覆盖，67% 分支覆盖 🟡
│   │   └── cancelOrder()     方法级别：0% 行覆盖，0% 分支覆盖 🔴
│   └── PaymentService.java
│       └── charge()          方法级别：0% 行覆盖，0% 分支覆盖 🔴
├── com.example.repository    包级别：95% 行覆盖
└── com.example.config        包级别：0% 行覆盖 🔴
```

进入具体页面时：

- **绿色行**：该行代码已被执行
- **红色行**：该行代码未被任何测试执行
- **黄色菱形**：该行的条件分支有部分未覆盖
- 点击行号可以看到该行被执行的具体次数

### 4.4 在 IDE 中查看覆盖率

IntelliJ IDEA 原生支持使用 JaCoCo 引擎运行测试覆盖率：

```
右键测试目录 → Run Tests with Coverage
    └── 选择 JaCoCo（IntelliJ 内置）
    └── 直接在编辑器侧边栏显示绿色/红色标记
    └── Coverage 面板展示：类/方法/行覆盖率数据

快捷键：
    Ctrl + Shift + F6 (Windows/Linux)
    ⌘ + Shift + F6 (macOS)
```

> 💡 IDE 的覆盖率显示比 HTML 报告更直观——在写代码时就能看到哪行没被覆盖，建议开发阶段使用 IDE 覆盖功能，CI 中使用 JaCoCo Maven 插件。

---

## 5. 核心 Goals

jacoco-maven-plugin 提供三个核心 goal 和一个辅助 goal：

### 5.1 prepare-agent

```xml
<execution>
    <id>prepare-agent</id>
    <goals>
        <goal>prepare-agent</goal>
    </goals>
</execution>
```

- **作用**：在测试执行前启动 JaCoCo Java Agent，对已加载的类进行 on-the-fly 插桩
- **阶段**：默认绑定到 `initialize` 阶段
- **产物**：生成 `jacoco.exec` 二进制执行数据文件
- **关键参数**：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `destFile` | `${project.build.directory}/jacoco.exec` | 执行数据输出路径 |
| `propertyName` | `jacoco.agent.argLine` | 存储 Agent JVM 参数属性名 |
| `includes` | `*` | 包含的类路径模式 |
| `excludes` | 无 | 排除的类路径模式 |

### 5.2 report

```xml
<execution>
    <id>report</id>
    <phase>prepare-package</phase>
    <goals>
        <goal>report</goal>
    </goals>
</execution>
```

- **作用**：从 `jacoco.exec` 读取执行数据，生成 HTML/XML/CSV 报告
- **阶段**：通常绑定到 `prepare-package` 或 `post-integration-test`
- **输出**：默认在 `target/site/jacoco/` 目录

### 5.3 check

```xml
<execution>
    <id>check</id>
    <phase>verify</phase>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <rules>...</rules>
    </configuration>
</execution>
```

- **作用**：根据配置的规则检查覆盖率是否达标，不达标则构建失败
- **阶段**：通常绑定到 `verify` 阶段
- **行为**：当覆盖率低于阈值时，Maven 构建抛出 `RuleViolatedException`，构建失败

### 5.4 dump（辅助）

```xml
<execution>
    <id>dump</id>
    <goals>
        <goal>dump</goal>
    </goals>
    <configuration>
        <address>localhost</address>
        <port>6300</port>
        <dump>true</dump>
    </configuration>
</execution>
```

- **作用**：从远程 JaCoCo Agent 中转储执行数据
- **适用场景**：针对已部署的服务进行集成测试覆盖率收集（较少使用）

---

## 6. 覆盖率检查规则

### 6.1 规则结构

```xml
<configuration>
    <rules>
        <!-- 为 BUNDLE（整个项目）设定规则 -->
        <rule>
            <element>BUNDLE</element>
            <limits>
                <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.80</minimum>
                </limit>
                <limit>
                    <counter>BRANCH</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.70</minimum>
                </limit>
            </limits>
        </rule>

        <!-- 为特定的 PACKAGE 设定更严格的规则 -->
        <rule>
            <element>PACKAGE</element>
            <includes>
                <include>com.example.service</include>
            </includes>
            <limits>
                <limit>
                    <counter>LINE</counter>
                    <minimum>0.90</minimum>
                </limit>
                <limit>
                    <counter>BRANCH</counter>
                    <minimum>0.85</minimum>
                </limit>
            </limits>
        </rule>
    </rules>
</configuration>
```

### 6.2 element 类型

| element 类型 | 作用范围 | 说明 |
|--------------|----------|------|
| `BUNDLE` | 整个项目 | 全局覆盖率门禁，最常用 |
| `PACKAGE` | 特定包 | 可为不同包设置不同阈值 |
| `CLASS` | 特定类 | 针对关键类单独设定 |
| `SOURCEFILE` | 特定源文件 | 细粒度的文件级别控制 |
| `METHOD` | 特定方法 | 最细粒度，通常不推荐 |

### 6.3 counter 与 value

**counter（计数器类型）：**

| counter | 含义 | 常用 |
|---------|------|------|
| `LINE` | 行覆盖率 | 最常用指标，直观易懂 |
| `BRANCH` | 分支覆盖率 | 业务逻辑复杂时重点关注 |
| `METHOD` | 方法覆盖率 | 辅助指标 |
| `CLASS` | 类覆盖率 | 辅助指标 |
| `INSTRUCTION` | 指令覆盖率 | 最精细但不易理解 |
| `COMPLEXITY` | 圈复杂度 | 度量代码复杂性 |

**value（度量值）：**

| value | 含义 | 示例 |
|-------|------|------|
| `TOTALCOUNT` | 总数 | 总行数 |
| `MISSEDCOUNT` | 未覆盖数 | 未覆盖行数 |
| `COVEREDCOUNT` | 覆盖数 | 覆盖行数 |
| `MISSEDRATIO` | 未覆盖率 | 0.20（20%未覆盖） |
| `COVEREDRATIO` | 覆盖率 | 0.80（80%覆盖） |

### 6.4 多规则组合示例

```xml
<configuration>
    <rules>
        <!-- 全局规则：80% 行覆盖，70% 分支覆盖 -->
        <rule>
            <element>BUNDLE</element>
            <limits>
                <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.80</minimum>
                </limit>
                <limit>
                    <counter>BRANCH</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.70</minimum>
                </limit>
            </limits>
        </rule>

        <!-- Service 层：90% 覆盖 -->
        <rule>
            <element>PACKAGE</element>
            <includes>
                <include>com.example.service</include>
            </includes>
            <limits>
                <limit>
                    <counter>LINE</counter>
                    <minimum>0.90</minimum>
                </limit>
            </limits>
        </rule>

        <!-- Controller 层：只要求 50% 覆盖（Web 层测试成本高） -->
        <rule>
            <element>PACKAGE</element>
            <includes>
                <include>com.example.controller</include>
            </includes>
            <limits>
                <limit>
                    <counter>LINE</counter>
                    <minimum>0.50</minimum>
                </limit>
            </limits>
        </rule>
    </rules>
</configuration>
```

---

## 7. 排除规则配置

### 7.1 为什么要排除某些类

```text
需要排除的常见类类型：

1. Lombok 生成的代码（@Data, @Builder, @AllArgsConstructor 等）
   → 自动生成的 getter/setter/equals/hashCode 无业务逻辑

2. 配置类（@Configuration, @SpringBootApplication）
   → 通常由框架初始化，不包含业务逻辑

3. DTO / VO / Request / Response
   → 纯数据载体，测试价值低

4. 代理类 / 增强类
   → CGLIB 代理等运行时生成的类

5. 第三方生成代码（MyBatis Generator, OpenAPI Generator 等）
   → 自动生成，无法也不应该由自己测试
```

### 7.2 Maven 配置排除

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <configuration>
        <excludes>
            <!-- Lombok 生成的类（根据生成的 class 名称模式） -->
            <exclude>**/entity/**</exclude>
            <exclude>**/dto/**</exclude>
            <exclude>**/vo/**</exclude>
            <exclude>**/model/*Request*</exclude>
            <exclude>**/model/*Response*</exclude>

            <!-- 配置类 -->
            <exclude>**/*Config.*</exclude>
            <exclude>**/*Application.*</exclude>

            <!-- 第三方生成代码 -->
            <exclude>**/generated/**</exclude>

            <!-- MyBatis Generator 生成的 Mapper -->
            <exclude>**/mapper/*Mapper.*</exclude>
        </excludes>
    </configuration>
    <executions>
        <!-- 应用到 prepare-agent -->
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
            <configuration>
                <!-- prepare-agent 的排除继承自全局配置 -->
            </configuration>
        </execution>

        <!-- 应用到 report（排除的类不会出现在报告中） -->
        <execution>
            <id>report</id>
            <goals>
                <goal>report</goal>
            </goals>
            <configuration>
                <excludes>
                    <exclude>**/entity/**</exclude>
                    <exclude>**/*Config.*</exclude>
                </excludes>
            </configuration>
        </execution>

        <!-- 应用到 check（检查时排除这些类） -->
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <excludes>
                    <exclude>**/entity/**</exclude>
                    <exclude>**/*Config.*</exclude>
                </excludes>
                <rules>...</rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 7.3 使用 @Generated 注解排除

对于无法通过路径模式匹配的类，可以使用 `@Generated` 注解：

```java
import javax.annotation.processing.Generated;

@Generated  // JaCoCo 默认排除所有 @Generated 注解的类
public class AutoGeneratedDto {
    private String name;
    // getter / setter
}
```

> ⚠️ 注意：JDK 9+ 中 `@Generated` 在 `javax.annotation.processing` 包下。如果使用 Lombok，在 `lombok.config` 中配置 `lombok.addLombokGeneratedAnnotation = true`，会让 Lombok 生成的代码自动添加 `@Generated` 注解，JaCoCo 会自动排除。

```properties
# lombok.config (放在项目根目录)
lombok.addLombokGeneratedAnnotation = true
```

---

## 8. SonarQube + JaCoCo 集成

### 8.1 集成原理

```
┌──────────────┐     ┌────────────────┐     ┌──────────────┐
│  Maven Test   │ ──► │  JaCoCo .exec   │ ──► │  SonarQube   │
│  (运行测试)    │     │  (执行数据)      │     │  (质量分析)   │
└──────────────┘     └────────────────┘     └──────────────┘
                            │                        │
                     sonar-project.properties     Quality Gate
                     jacoco.xml 路径对应配置      (覆盖率门禁)
```

### 8.2 Maven + SonarQube 配置

```xml
<!-- pom.xml - SonarQube 属性 -->
<properties>
    <sonar.host.url>https://sonarqube.example.com</sonar.host.url>
    <sonar.login>${env.SONAR_TOKEN}</sonar.login>
    <sonar.coverage.jacoco.xmlReportPaths>
        ${project.build.directory}/site/jacoco/jacoco.xml
    </sonar.coverage.jacoco.xmlReportPaths>
    <sonar.exclusions>
        **/entity/**,
        **/dto/**,
        **/config/**,
        **/*Application.*
    </sonar.exclusions>
</properties>
```

### 8.3 Quality Gate 配置

在 SonarQube 中配置覆盖率质量门：

| 指标 | 阈值 | 等级 |
|------|------|------|
| 整体行覆盖率 | >= 80% | Pass |
| 整体分支覆盖率 | >= 70% | Pass |
| 新代码行覆盖率 | >= 85% | Pass |
| 新代码分支覆盖率 | >= 75% | Pass |

```xml
<!-- sonar-project.properties -->
sonar.projectKey=com.example:my-service
sonar.projectName=My Service
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes

# JaCoCo 报告路径
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

# 排除不扫描的文件
sonar.exclusions=**/entity/**,**/dto/**,**/config/**,**/*Application.java

# 覆盖率门禁（通过 SonarQube UI 配置）
# 推荐设置：
#   新代码覆盖率 < 80% → Fail
#   全局覆盖率 < 60% → Fail
```

### 8.4 Docker Compose 运行 SonarQube 本地检测

```yaml
# docker-compose.yml
version: '3.8'
services:
  sonarqube:
    image: sonarqube:lts-community
    ports:
      - "9000:9000"
    environment:
      - SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_extensions:/opt/sonarqube/extensions

volumes:
  sonarqube_data:
  sonarqube_extensions:
```

```bash
# 完整 CI 命令链
mvn clean test              # 运行测试 + 生成 jacoco.exec
mvn jacoco:report           # 生成 XML 报告
mvn sonar:sonar             # 上传到 SonarQube 分析
```

> 💡 SonarQube 会在代码门禁仪表盘上展示覆盖率趋势图，帮助团队追踪覆盖率变化。新代码覆盖率比全局覆盖率更重要——它反映了最近改动是否被充分测试。

---

## 9. CI/CD 集成

### 9.1 GitHub Actions

```yaml
# .github/workflows/ci.yml
name: CI with Coverage

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

      - name: Build and test with coverage check
        run: mvn clean verify -B

      - name: Upload JaCoCo report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: jacoco-report
          path: target/site/jacoco/
          retention-days: 30

      - name: SonarQube Scan
        if: github.event_name != 'pull_request'
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar -B
```

### 9.2 Jenkins Pipeline

```groovy
// Jenkinsfile
pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    stages {
        stage('Build & Test') {
            steps {
                sh 'mvn clean verify -B'
            }
        }

        stage('Coverage Report') {
            steps {
                // 将 JaCoCo HTML 报告发布到 Jenkins
                publishHTML(target: [
                    allowMissing         : false,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : 'target/site/jacoco',
                    reportFiles          : 'index.html',
                    reportName           : 'JaCoCo Coverage Report'
                ])
            }
        }

        stage('Quality Gate') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar -B'
                }
            }
        }

        stage('Quality Gate Check') {
            steps {
                timeout(time: 1, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }

    post {
        always {
            // 保存 JaCoCo .exec 文件
            archiveArtifacts artifacts: 'target/*.exec', fingerprint: true
            junit 'target/surefire-reports/*.xml'
        }
    }
}
```

### 9.3 Coverage Badge

在 README 中添加覆盖率徽章：

```markdown
<!-- SonarQube 徽章 -->
[![Coverage](https://sonarqube.example.com/api/project_badges/measure?project=com.example:my-service&metric=coverage)](https://sonarqube.example.com/dashboard?id=com.example:my-service)

<!-- GitHub Actions + JaCoCo 徽章（需要第三方服务如 codecov.io） -->
[![codecov](https://codecov.io/gh/your-org/your-repo/branch/main/graph/badge.svg)](https://codecov.io/gh/your-org/your-repo)
```

### 9.4 覆盖率趋势追踪

```
推荐的可视化工具：

SonarQube           ─── 覆盖率历史趋势图、新代码覆盖率
JaCoCo HTML Report  ─── 单次构建的快照，无历史趋势
Codecov / Coveralls  ─── PR 级别的覆盖率对比 + 徽章
IntelliJ IDEA       ─── 本地开发实时覆盖率显示

最佳实践：Jenkins/GitHub Actions + SonarQube 覆盖全部
         ─── CI 门禁用 `mvn verify` 的 check goal
         ─── 趋势追踪用 SonarQube
```

---

## 10. JaCoCo 的局限性

### 10.1 核心局限

```
JaCoCo 能做什么：
✅ 统计哪些代码被执行了
✅ 统计哪些分支被覆盖了
✅ 阻止未经充分测试的代码合并

JaCoCo 不能做什么：
❌ 验证测试断言是否正确（测试可能是"false positive"）
❌ 检测缺失的测试场景（比如没测空指针场景）
❌ 评估测试质量（覆盖率 100% 的测试也可能是烂测试）
❌ 检测逻辑错误（测试覆盖了代码但 assert 可能写错了）
❌ 衡量集成测试覆盖（JaCoCo 只能度量测试过程中的代码执行）
```

### 10.2 常见陷阱

```java
public class CoverageTrap {

    // 陷阱 1：高覆盖率低质量的测试
    public String process(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("negative");
        }
        return "positive: " + value;
    }

    @Test
    void coverageTrap_ValueIsZero() {
        // 这个测试覆盖了 100% 的代码行
        // 但完全没有测试负数的异常路径吗？
        // 等等——这个测试传了 0，实际上只覆盖了一个分支！
        // 100% 行覆盖率 ≠ 100% 分支覆盖率
        assertNotNull(service.process(0));
    }

    // 陷阱 2：覆盖了但不验证
    @Test
    void coverageTrap_NoAssertion() {
        // 这行被执行了，覆盖率计数器会记录
        // 但没有 assertion，测试永远 pass
        service.executeSomeSideEffect();
        // 缺少验证！覆盖率 100% 但测试毫无意义
    }

    // 陷阱 3：高覆盖但断言错误
    @Test
    void coverageTrap_WrongAssert() {
        int result = service.add(2, 2);
        // 断言本身是错误的，但测试通过了？不，但...
        // 如果这里写错为 assertEquals(5, result)，测试会失败
        // 但如果断言的是错误的业务理解——测试通过但逻辑错误
        assertEquals(4, result);  // 如果业务需求其实是加法但应该是 5...
    }
}
```

### 10.3 如何弥补 JaCoCo 的不足

| 局限 | 弥补方案 |
|------|----------|
| 不能验证测试质量 | 代码审查 + 测试设计评审 |
| 不能检测缺失场景 | 边界值分析 + 等价类划分 |
| 不能检测逻辑错误 | Mutation Testing（PIT） |
| 不能检测集成覆盖 | 集成测试单独追踪 + End-to-End 测试 |

> ⚠️ **核心原则**：覆盖率是**必要非充分条件**。80% 覆盖率只能说明"80% 的代码被测试执行到了"，不能说明"测试是高质量的"。永远不要为了冲覆盖率而写无意义的测试。

---

## 11. 变异测试（PIT）简介

### 11.1 为什么覆盖率还不够

覆盖率回答的问题是"哪些代码被执行了"，而变异测试回答的问题是"如果代码逻辑变了，测试能发现吗"。

```
覆盖率的盲区：

代码：
    if (a > 0 && b > 0) {
        return true;
    }
    return false;

测试：
    @Test
    void test() {
        assertTrue(service.check(1, 1));  // 覆盖了 true 分支
    }

JaCoCo：行覆盖率 100%，分支覆盖率 50%（只有 true 分支）
PIT：   将 `&&` 变异为 `||` → 测试依然通过 ❌（测试不够健壮）
        将 `a > 0` 变异为 `a >= 0` → 测试依然通过 ❌
```

### 11.2 PIT 基本配置

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.16.1</version>
    <configuration>
        <targetClasses>
            <param>com.example.service.*</param>
        </targetClasses>
        <targetTests>
            <param>com.example.service.*</param>
        </targetTests>
        <mutators>
            <!-- 使用默认的突变算子集合 -->
            <mutator>DEFAULTS</mutator>
        </mutators>
        <coverageThreshold>80</coverageThreshold>
        <mutationThreshold>70</mutationThreshold>
        <outputFormats>
            <outputFormat>HTML</outputFormat>
            <outputFormat>XML</outputFormat>
        </outputFormats>
    </configuration>
</plugin>
```

### 11.3 JaCoCo 与 PIT 的对比

| 维度 | JaCoCo（覆盖率） | PIT（变异测试） |
|------|-----------------|----------------|
| **度量目标** | 代码是否被执行 | 测试能否发现代码变化 |
| **输出指标** | 行覆盖率 / 分支覆盖率 | 变异得分（Mutation Score） |
| **执行速度** | 快（一次测试运行） | 慢（每处变异都要跑一次测试） |
| **配置复杂度** | 低 | 中等 |
| **CI 适用性** | 每次构建都运行 | 建议定时运行（如 nightly） |
| **给出的信心** | 低（仅覆盖信息） | 高（验证测试有效性） |
| **团队接受度** | 高（广泛使用） | 低（执行慢，概念较新） |

### 11.4 实际应用建议

```text
推荐的使用方式：

日常开发（每次提交）：
    JaCoCo check goal → 80% 行覆盖率门禁

每日构建（Nightly Build）：
    PIT 变异测试 → 70% 变异得分门禁

发布前（Release Build）：
    JaCoCo + PIT 全部通过 → 双门禁通过才允许发布

渐进式采用路线：
    阶段 1：JaCoCo 报告（了解当前覆盖率）
    阶段 2：JaCoCo 门禁（80% 行覆盖，70% 分支覆盖）
    阶段 3：PIT 变异测试（每周运行，关注趋势）
    阶段 4：PIT 门禁（关键模块 70% 变异得分）
```

> 💡 **变异测试是覆盖率的"质检员"**：JaCoCo 告诉你覆盖率数字，PIT 告诉你这个数字是否值得信任。如果一个模块 JaCoCo 显示 90% 覆盖但 PIT 只有 30%，说明你的测试写了很多却没测试到真正关键的逻辑。

---

## 12. 完整示例：SpringBoot 项目配置 80% 覆盖率门禁

### 12.1 pom.xml 完整配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>order-service</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
        <jacoco.version>0.8.12</jacoco.version>
        <sonar.coverage.jacoco.xmlReportPaths>
            ${project.build.directory}/site/jacoco/jacoco.xml
        </sonar.coverage.jacoco.xmlReportPaths>
    </properties>

    <dependencies>
        <!-- Spring Boot 依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- JaCoCo 插件 -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>${jacoco.version}</version>
                <configuration>
                    <!-- 排除不需要覆盖的类 -->
                    <excludes>
                        <exclude>**/entity/**</exclude>
                        <exclude>**/dto/**</exclude>
                        <exclude>**/*Config.*</exclude>
                        <exclude>**/*Application.*</exclude>
                        <exclude>**/generated/**</exclude>
                    </excludes>
                </configuration>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>check</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <!-- 全局规则 -->
                                <rule>
                                    <element>BUNDLE</element>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.80</minimum>
                                        </limit>
                                        <limit>
                                            <counter>BRANCH</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.70</minimum>
                                        </limit>
                                        <limit>
                                            <counter>METHOD</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.80</minimum>
                                        </limit>
                                        <limit>
                                            <counter>CLASS</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.90</minimum>
                                        </limit>
                                    </limits>
                                </rule>

                                <!-- Service 层要求更高 -->
                                <rule>
                                    <element>PACKAGE</element>
                                    <includes>
                                        <include>com.example.service</include>
                                    </includes>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <minimum>0.90</minimum>
                                        </limit>
                                        <limit>
                                            <counter>BRANCH</counter>
                                            <minimum>0.85</minimum>
                                        </limit>
                                    </limits>
                                </rule>

                                <!-- Controller 层宽松要求 -->
                                <rule>
                                    <element>PACKAGE</element>
                                    <includes>
                                        <include>com.example.controller</include>
                                    </includes>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <minimum>0.50</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- Spring Boot Maven 插件 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 12.2 业务代码示例

```java
// OrderService.java - 被测业务逻辑
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public OrderService(OrderRepository orderRepository,
                        PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }

    public OrderResult createOrder(CreateOrderRequest request) {
        // 参数校验
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("订单项不能为空");
        }

        // 计算总价
        BigDecimal total = request.getItems().stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单金额必须大于 0");
        }

        // 创建订单
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);
        order = orderRepository.save(order);

        // 调用支付
        PaymentResponse payment = paymentGateway.charge(order.getId(), total);

        if (payment.isSuccess()) {
            order.setStatus(OrderStatus.PAID);
            order.setTransactionId(payment.getTransactionId());
        } else {
            order.setStatus(OrderStatus.FAILED);
            order.setFailReason(payment.getErrorMessage());
        }

        return orderRepository.save(order).toResult();
    }

    public OrderResult cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("订单不存在: " + orderId));

        // 只有 PENDING 状态的订单可以取消
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                "订单状态不允许取消，当前状态: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        return orderRepository.save(order).toResult();
    }

    public List<OrderResult> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(Order::toResult)
            .collect(Collectors.toList());
    }
}
```

### 12.3 测试代码

```java
// OrderServiceTest.java
@SpringBootTest
@Transactional
class OrderServiceTest {

    @MockBean
    private PaymentGateway paymentGateway;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    private CreateOrderRequest validRequest;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        validRequest = new CreateOrderRequest();
        validRequest.setUserId(1L);
        validRequest.setItems(List.of(
            new OrderItem(100L, "商品A", new BigDecimal("49.99"), 2),
            new OrderItem(101L, "商品B", new BigDecimal("29.99"), 1)
        ));
    }

    @Test
    @DisplayName("创建订单-支付成功-订单状态为PAID")
    void createOrder_PaymentSuccess_StatusPaid() {
        // Given
        when(paymentGateway.charge(anyLong(), any(BigDecimal.class)))
            .thenReturn(new PaymentResponse(true, "TXN-001", null));

        // When
        OrderResult result = orderService.createOrder(validRequest);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.PAID, result.getStatus());
        assertEquals("TXN-001", result.getTransactionId());

        verify(paymentGateway).charge(anyLong(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("创建订单-支付失败-订单状态为FAILED")
    void createOrder_PaymentFailed_StatusFailed() {
        // Given
        when(paymentGateway.charge(anyLong(), any(BigDecimal.class)))
            .thenReturn(new PaymentResponse(false, null, "余额不足"));

        // When
        OrderResult result = orderService.createOrder(validRequest);

        // Then
        assertEquals(OrderStatus.FAILED, result.getStatus());
        assertEquals("余额不足", result.getFailReason());
    }

    @Test
    @DisplayName("创建订单-空订单项-抛出异常")
    void createOrder_EmptyItems_ThrowsException() {
        // Given
        validRequest.setItems(List.of());

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(validRequest));
    }

    @Test
    @DisplayName("创建订单-金额为零-抛出异常")
    void createOrder_ZeroTotal_ThrowsException() {
        // Given：单价为零
        validRequest.setItems(List.of(
            new OrderItem(100L, "免费商品", BigDecimal.ZERO, 1)));

        // When & Then
        assertThrows(IllegalArgumentException.class,
            () -> orderService.createOrder(validRequest));
    }

    @Test
    @DisplayName("取消订单-PENDING状态-取消成功")
    void cancelOrder_PendingStatus_Cancelled() {
        // Given
        when(paymentGateway.charge(anyLong(), any(BigDecimal.class)))
            .thenReturn(new PaymentResponse(true, "TXN-001", null));
        OrderResult created = orderService.createOrder(validRequest);

        // When
        OrderResult cancelled = orderService.cancelOrder(
            created.getOrderId(), "测试取消");

        // Then
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals("测试取消", cancelled.getCancelReason());
    }

    @Test
    @DisplayName("取消订单-已支付订单-抛出异常")
    void cancelOrder_PaidStatus_ThrowsException() {
        // Given
        when(paymentGateway.charge(anyLong(), any(BigDecimal.class)))
            .thenReturn(new PaymentResponse(true, "TXN-001", null));
        OrderResult created = orderService.createOrder(validRequest);

        // 已支付状态下取消 → 应该抛出 IllegalStateException
        assertThrows(IllegalStateException.class,
            () -> orderService.cancelOrder(created.getOrderId(), "想退款"));
    }

    @Test
    @DisplayName("取消订单-不存在的订单-抛出异常")
    void cancelOrder_NotFound_ThrowsException() {
        assertThrows(OrderNotFoundException.class,
            () -> orderService.cancelOrder(99999L, "不存在"));
    }

    @Test
    @DisplayName("查询用户订单-返回排序后的订单列表")
    void getOrdersByUser_ReturnsUserOrders() {
        // Given
        when(paymentGateway.charge(anyLong(), any(BigDecimal.class)))
            .thenReturn(new PaymentResponse(true, "TXN-001", null));
        orderService.createOrder(validRequest);
        orderService.createOrder(validRequest);

        // When
        List<OrderResult> orders = orderService.getOrdersByUser(1L);

        // Then
        assertEquals(2, orders.size());
        // 验证按创建时间倒序
        assertTrue(orders.get(0).getCreatedAt()
            .isAfter(orders.get(1).getCreatedAt()));
    }
}
```

### 12.4 覆盖率验证

```bash
# 运行测试并检查覆盖率
mvn clean verify

# 预期输出：
# [INFO] --- jacoco:check ---
# [INFO] Rule violated for bundle order-service: 行覆盖率为 0.91，最低要求为 0.80
# [INFO] Rule violated for bundle order-service: 分支覆盖率为 0.86，最低要求为 0.70
# [INFO] Rule violated for bundle order-service: 方法覆盖率为 0.95，最低要求为 0.80
# [INFO] Rule violated for bundle order-service: 类覆盖率为 1.00，最低要求为 0.90
# [INFO] All coverage checks passed.
# [INFO] BUILD SUCCESS

# 如果覆盖率不足，构建失败：
# [ERROR] Failed to execute goal org.jacoco:jacoco-maven-plugin:0.8.12:check
# (check) on project order-service: Coverage checks have failed.
# See log for details.
```

---

## 13. 最佳实践

### 13.1 覆盖率的合理目标

| 项目阶段 | 行覆盖率目标 | 分支覆盖率目标 | 说明 |
|----------|-------------|---------------|------|
| 新项目（未接入） | - | - | 先跑起来看报告 |
| 新项目（初始阶段） | 70% | 60% | 从核心业务逻辑开始 |
| 新项目（稳定期） | 80% | 70% | 业界推荐的合理目标 |
| 核心金融/交易系统 | 90%+ | 85%+ | 支付、交易等核心链路 |
| 遗留项目（旧代码） | 60% | 50% | 新代码要求 80%，旧代码逐步提升 |

### 13.2 不同代码层的目标

```
┌────────────────────────────────────────────┐
│  代码层          │  推荐行覆盖率 │  测试重点  │
├──────────────────┼──────────────┼───────────┤
│  Service 层       │  90%+        │  业务逻辑  │
│  Controller 层    │  50-70%      │  请求处理  │
│  Repository 层    │  80%+        │  数据访问  │
│  Utility 工具类   │  90%+        │  工具方法  │
│  Config 配置类    │  排除         │  -         │
│  DTO/Entity       │  排除         │  -         │
│  Application 入口  │  排除         │  -         │
└────────────────────────────────────────────┘
```

### 13.3 反模式与建议

```
❌ 反模式：为冲覆盖率而写"假测试"

// 毫无意义的测试 —— 覆盖率达标了但毫无价值
@Test
void fakeTest_forCoverage() {
    orderService.createOrder(null);  // 覆盖了抛异常的行
    // 没有 assert，甚至没有验证异常被抛出
}

✅ 正确做法：基于行为写测试

• 写测试时关注"行为"而非"行"
• 每个测试验证一个明确的行为场景
• Given-When-Then 结构清晰
• 测试方法名称描述场景和预期结果


❌ 反模式：追求 100% 覆盖率

// 为了 100% 覆盖率加测试
// 这行是 getter/setter，测试成本 > 价值
@Data
public class OrderDTO {
    private Long id;           // 覆盖这个的 getter/setter 意义不大
    private String status;     // 同上
}

✅ 正确处理：排除 + 聚焦

• 排除无业务逻辑的类（DTO、Entity、Config）
• 将精力集中在：核心业务逻辑、复杂条件、边界场景
• 80/20 法则：80% 的缺陷存在于 20% 的代码中


❌ 反模式：覆盖率下降就调低门禁

// 当覆盖率不足时，不是降低门禁
// ✓ 而是补充测试
// ✓ 或是分析为什么覆盖率低（排除规则是否正确？）

✅ 正确做法：门禁是底线，只升不降

• 门禁只可以变得更严格（从 80% → 85%）
• 永远不应该因为"当前达不到"而降低门禁
• 如果确实需要例外，走例外审批流程
```

### 13.4 覆盖率工作的推荐流程

```
阶段 1：基线建立（第 1-2 周）
   1. 配置 JaCoCo 和 SonarQube
   2. 跑出当前覆盖率基线
   3. 确定排除规则
   4. 设定"新代码覆盖率 >= 80%"的门禁

阶段 2：增量提升（第 3-8 周）
   1. 每次提交新功能/修复时写测试
   2. SonarQube 监控新代码覆盖率趋势
   3. 每周回顾覆盖率变化
   4. 逐步提高全局门禁

阶段 3：持续改进（长期）
   1. 引入 PIT 变异测试（关键模块）
   2. 将测试设计纳入 Code Review
   3. 建立测试质量文化
   4. 每季度回顾测试策略
```

### 13.5 团队协作建议

| 角色 | 职责 |
|------|------|
| **开发者** | 写测试、关注 IDE 覆盖率、确保新代码达标 |
| **Code Reviewer** | 检查测试完整性、分支覆盖是否充分 |
| **Tech Lead** | 设定覆盖率门禁、优化排除规则 |
| **QA** | 补充集成测试、监控覆盖率趋势 |
| **DevOps** | 配置 CI/CD 门禁、SonarQube 集成 |

> 🎯 **核心总结**：
> 1. JaCoCo 是行业标准的代码覆盖率工具，推荐 80% 行覆盖 + 70% 分支覆盖作为基线目标
> 2. 正确配置排除规则（DTO、Entity、Config）才能得到有意义的覆盖率数字
> 3. 覆盖率门禁应纳入 CI/CD 流水线，新代码覆盖率比整体覆盖率更重要
> 4. 覆盖率是质量的下限指标——它告诉你哪些代码完全没被测试，但不能保证测试的质量
> 5. 进阶团队应引入变异测试（PIT）来验证测试的有效性，弥补纯覆盖率指标的盲区
