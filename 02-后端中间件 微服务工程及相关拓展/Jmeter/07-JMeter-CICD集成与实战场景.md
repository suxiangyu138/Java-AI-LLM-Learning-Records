# JMeter CI/CD 集成与实战场景

> 🚀 Maven Plugin 集成、Jenkins Pipeline、GitHub Actions、常见压测场景实战模板 —— 从脚本到流水线

---

## 📚 目录

1. [Maven JMeter Plugin](#1-maven-jmeter-plugin)
2. [Jenkins Pipeline 集成](#2-jenkins-pipeline-集成)
3. [GitHub Actions 集成](#3-github-actions-集成)
4. [实战场景模板](#4-实战场景模板)
5. [常见问题排查](#5-常见问题排查)

---

## 1. Maven JMeter Plugin

### 1.1 配置 pom.xml

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.lazerycode.jmeter</groupId>
            <artifactId>jmeter-maven-plugin</artifactId>
            <version>3.8.0</version>
            <configuration>
                <testFilesDirectory>${project.basedir}/src/test/jmeter</testFilesDirectory>
                <resultsFileName>results.jtl</resultsFileName>
                <jmeterDirectory>/opt/apache-jmeter-5.6.3</jmeterDirectory>
                <propertiesUser>
                    <baseUrl>${baseUrl}</baseUrl>
                    <threads>${threads}</threads>
                    <duration>${duration}</duration>
                </propertiesUser>
                <testResultsTimestamp>false</testResultsTimestamp>
                <generateReports>true</generateReports>
            </configuration>
            <executions>
                <execution>
                    <id>jmeter-tests</id>
                    <goals>
                        <goal>jmeter</goal>
                        <goal>results</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

```bash
# 执行 JMeter 测试
mvn jmeter:jmeter -DbaseUrl=https://staging.example.com -Dthreads=200 -Dduration=300

# 生成报告
mvn jmeter:results
```

### 1.2 目录结构

```text
src/test/jmeter/
├── performance-test.jmx       ← JMeter 脚本
├── test-data/
│   └── users.csv              ← 测试数据文件
└── properties/
    └── staging.properties     ← 环境配置
```

---

## 2. Jenkins Pipeline 集成

### 2.1 完整 Pipeline

```groovy
pipeline {
    agent any

    parameters {
        choice(name: 'ENV', choices: ['staging', 'production'], description: '目标环境')
        string(name: 'THREADS', defaultValue: '100', description: '并发线程数')
        string(name: 'DURATION', defaultValue: '300', description: '持续时间(秒)')
    }

    environment {
        JMETER_HOME = '/opt/apache-jmeter-5.6.3'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('JMeter Test') {
            steps {
                script {
                    def baseUrl = params.ENV == 'production'
                        ? 'https://api.example.com'
                        : 'https://staging-api.example.com'

                    sh """
                        ${JMETER_HOME}/bin/jmeter -n \
                          -t src/test/jmeter/performance-test.jmx \
                          -l results.jtl \
                          -e -o report/ \
                          -JbaseUrl=${baseUrl} \
                          -Jthreads=${params.THREADS} \
                          -Jduration=${params.DURATION} \
                          -j jmeter.log
                    """
                }
            }
        }

        stage('Check Results') {
            steps {
                script {
                    // 解析 JTL → 检查错误率和 P99
                    def errorRate = sh(
                        script: "awk -F',' 'NR>1{total++; if(\$8==\"false\") err++} END{printf \"%.2f\", err/total*100}' results.jtl",
                        returnStdout: true
                    ).trim().toFloat()

                    def p99 = sh(
                        script: "awk -F',' 'NR>1{print \$2}' results.jtl | sort -n | awk '{all[NR]=\$0} END{print all[int(NR*0.99)]}'",
                        returnStdout: true
                    ).trim().toInteger()

                    echo "错误率: ${errorRate}%, P99: ${p99}ms"

                    if (errorRate > 1.0) {
                        error("错误率过高: ${errorRate}%")
                    }
                    if (p99 > 2000) {
                        error("P99 超阈值: ${p99}ms")
                    }
                }
            }
        }

        stage('Publish Report') {
            steps {
                publishHTML(target: [
                    reportName: 'JMeter Performance Report',
                    reportDir: 'report',
                    reportFiles: 'index.html'
                ])
                archiveArtifacts artifacts: 'results.jtl, jmeter.log'
            }
        }
    }

    post {
        failure {
            // 压测失败 → 通知
            slackSend(
                channel: '#qa-alerts',
                color: 'danger',
                message: "性能测试失败！环境: ${params.ENV}, 线程: ${params.THREADS}"
            )
        }
    }
}
```

### 2.2 关键步骤解析

```text
Jenkins Pipeline 流程：

  1. Checkout → 拉取代码 + JMeter 脚本
  2. JMeter Test → CLI 模式执行压测
  3. Check Results → 解析 JTL → 判断 SLA 达标
     → 错误率 > 1% ？→ 构建失败
     → P99 > 2000ms？→ 构建失败
  4. Publish Report → 归档 HTML Dashboard + JTL
  5. post { failure } → 失败通知 Slack/钉钉/邮件
```

---

## 3. GitHub Actions 集成

```yaml
# .github/workflows/perf-test.yml
name: Performance Test

on:
  workflow_dispatch:          # 手动触发
    inputs:
      threads:
        description: '并发数'
        required: true
        default: '100'
      duration:
        description: '持续时间(秒)'
        required: true
        default: '300'

jobs:
  perf-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup JMeter
        run: |
          wget -q https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
          tar -xzf apache-jmeter-5.6.3.tgz
          echo "JMETER_HOME=$PWD/apache-jmeter-5.6.3" >> $GITHUB_ENV

      - name: Run JMeter
        run: |
          $JMETER_HOME/bin/jmeter -n \
            -t src/test/jmeter/perf-test.jmx \
            -l results.jtl \
            -e -o report/ \
            -Jthreads=${{ github.event.inputs.threads }} \
            -Jduration=${{ github.event.inputs.duration }}

      - name: Check SLA
        run: |
          ERROR_RATE=$(awk -F',' 'NR>1{t++; if($8=="false") e++} END{printf "%.2f", e/t*100}' results.jtl)
          echo "Error Rate: $ERROR_RATE%"
          if (( $(echo "$ERROR_RATE > 1.0" | bc -l) )); then
            echo "❌ 错误率超阈值！"
            exit 1
          fi

      - name: Upload Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: performance-report
          path: report/
```

---

## 4. 实战场景模板

### 4.1 REST API 性能测试

```text
场景：用户服务 API 性能测试

Thread Group 配置：
  Number of Threads: ${__P(threads, 100)}
  Ramp-up Period: 30
  Duration: ${__P(duration, 300)}

业务比例：
  Throughput Controller 分配：
    GET  /users       → 40% (查询为主)
    POST /users       → 20% (注册)
    GET  /users/{id}  → 30% (查看详情)
    PUT  /users/{id}  → 10% (修改)

关键断言：
  → 响应状态码 200/201
  → 响应时间 < 1000ms
  → JSON Schema 校验

结果目标：
  → 吞吐量 > 500 QPS
  → P99 < 500ms
  → 错误率 < 0.1%
```

### 4.2 数据库性能测试

```text
场景：MySQL 读/写性能基准

  JDBC Connection Configuration
    → Pool: 50 连接

  Thread Group 1：读测试
    100 线程 × 300 秒
    SELECT * FROM users WHERE id = ?
    随机 ID 参数化

  Thread Group 2：写测试
    50 线程 × 300 秒
    INSERT INTO orders (...) VALUES (...)
    随机数据

  关注指标：
    → 数据库 CPU/IO（PerfMon 监控）
    → 慢查询日志
    → 连接池使用率
```

### 4.3 秒杀场景压力测试

```text
场景：秒杀接口高并发

  Synchronizing Timer（集合点）：
    → Number of Simulated Users to Group: 500
    → 攒 500 个请求同时释放 → 模拟秒杀瞬间

  Stepping Thread Group：
    → 0 → 1000 线程
    → 快速 Ramp-up: 10s（不等间隔）
    → 观察系统瞬间压力承受能力

  预期行为验证：
    → 一些请求返回"已售罄"（正常）
    → 不能超卖！(断言库存正确)
    → 不能 500 错误（熔断/限流是预期行为）
```

---

## 5. 常见问题排查

| 问题 | 原因 | 解决 |
|------|------|------|
| **OutOfMemoryError** | JMeter 堆内存不足 | 增大 HEAP → `-Xmx8g` |
| **Connection Refused** | 服务器连接池满 | 检查服务器连接池配置 |
| **SocketTimeout** | 服务器处理超时 | 增大 Timeout 或优化服务 |
| **结果偏差大** | 压测机资源不足 | 降低并发或升级机器 |
| **分布式结果不聚合** | RMI 通信问题 | 检查端口和防火墙 |
| **CSV 数据不够** | 线程数 > 数据行数 | Recycle on EOF = True |
| **TPS 上不去** | 压测机带宽/CPU 瓶颈 | 增加 Agent 数量 |

---

> 🎯 **核心要点**：CI/CD 集成的关键是 **Maven Plugin 或 CLI + 结果解析 + SLA 门禁**。压测失败构建也应该失败（质量门禁）。实战中 90% 场景用 HTTP Request + CSV Data Set + JSON Extractor + Aggregate Report 即可覆盖。

---

**返回总览**：[00-JMeter知识体系总览](./00-JMeter知识体系总览.md)

---

*创建于：2026年7月*
