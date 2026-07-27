# JMeter 变量、属性与函数

> 🔢 JMeter 变量 vs 属性、30+ 内置函数、JSR223 (Groovy) 脚本、正则/JSON 提取 —— 动态化压测的基石

---

## 📚 目录

1. [变量 vs 属性](#1-变量-vs-属性)
2. [内置函数速查](#2-内置函数速查)
3. [JSR223 脚本引擎](#3-jsr223-脚本引擎)
4. [变量作用域与生命周期](#4-变量作用域与生命周期)

---

## 1. 变量 vs 属性

### 1.1 核心区别

| 维度 | Variables (变量) | Properties (属性) |
|------|:--------------:|:---------------:|
| **定义方式** | `vars.put("key","value")` | `props.put("key","value")` |
| **引用方式** | `${key}` | `${__P(key)}` 或 `${__property(key)}` |
| **作用域** | 当前线程组 | 全局（所有线程组共享） |
| **生命周期** | 线程结束即销毁 | JMeter 进程结束才销毁 |
| **线程安全** | 是（线程私有） | 需注意（全进程共享） |
| **使用场景** | 提取 token、数据传递 | 全局配置、跨线程组通信 |
| **命令行传参** | ❌ | ✅ `jmeter -Jkey=value` |

### 1.2 使用示例

```groovy
// JSR223 Sampler / PreProcessor / PostProcessor

// ===== Variables =====
vars.put("myVar", "hello");           // 设置变量
def val = vars.get("myVar");          // 获取变量
vars.remove("myVar");                 // 删除变量

// ===== Properties =====
props.put("globalTimeout", "30000");  // 设置属性
def timeout = props.get("globalTimeout");
// 命令行覆盖：jmeter -JglobalTimeout=60000

// ===== 在请求中引用 =====
// ${myVar}           → 变量
// ${__P(globalTimeout)} → 属性
```

```bash
# 命令行传参（覆盖属性默认值）
jmeter -n -t test.jmx \
  -JbaseUrl=https://staging.example.com \
  -Jthreads=200 \
  -Jduration=600 \
  -l result.jtl
```

---

## 2. 内置函数速查

### 2.1 信息类

| 函数 | 说明 | 示例输出 |
|------|------|---------|
| `${__machineName}` | 本机机器名 | `worker-01` |
| `${__machineIP}` | 本机 IP | `192.168.1.100` |
| `${__threadNum}` | 当前线程编号 | `1, 2, 3...` |
| `${__threadGroupName}` | 线程组名称 | `Thread Group 1` |
| `${__TestPlanName}` | 测试计划名称 | `API Test Plan` |

### 2.2 时间类

| 函数 | 说明 | 示例输出 |
|------|------|---------|
| `${__time()}` | 当前时间戳（毫秒） | `1750000000000` |
| `${__time(yyyy-MM-dd HH:mm:ss)}` | 格式化时间 | `2026-07-28 14:30:00` |
| `${__timeShift(yyyy-MM-dd,,P1D,,)}` | 时间偏移（+1天） | `2026-07-29` |
| `${__RandomDate(2026-01-01,2026-12-31)}` | 随机日期 | `2026-03-15` |

### 2.3 随机/生成类

| 函数 | 说明 | 示例输出 |
|------|------|---------|
| `${__Random(1,100)}` | 随机整数 | `42` |
| `${__RandomString(8,abcdef123)}` | 随机字符串 | `ab3f1c2e` |
| `${__UUID()}` | UUID | `550e8400-e29b-...` |
| `${__counter(,)}` | 自增计数器 | `1, 2, 3...` |
| `${__RandomFromMultipleVars(val1\|val2\|val3)}` | 多选一 | `val2` |
| `${__groovy(new Date().format("yyyyMMddHHmmss"))}` | Groovy 执行 | `20260728143000` |

### 2.4 条件/逻辑类

| 函数 | 说明 |
|------|------|
| `${__if(${code}==200, success, fail)}` | 条件判断 |
| `${__jexl3("${status}"=="ok")}` | 表达式求值 |
| `${__groovy(vars.get("count").toInteger() > 10)}` | Groovy 条件 |
| `${__escapeHtml("a<b>c")}` | HTML 转义 |

### 2.5 属性/变量操作

| 函数 | 说明 |
|------|------|
| `${__P(baseUrl)}` | 读取属性 baseUrl |
| `${__P(baseUrl, http://localhost)}` | 读取属性 + 默认值 |
| `${__property(baseUrl)}` | 同 __P |
| `${__setProperty(key,value)}` | 设置属性 |
| `${__V(varName)}` | 嵌套变量引用 `${${prefix}_name}` |
| `${__eval(${expression})}` | 执行变量中的表达式 |

---

## 3. JSR223 脚本引擎

### 3.1 为什么选 Groovy

| 引擎 | 速度 | 功能 | 推荐 |
|------|:---:|------|:---:|
| **Groovy (JSR223)** | 快 | 语法≈Java，丰富 | ✅ 首选 |
| **BeanShell** | 慢 | 类 Java，老旧 | ❌ 不推荐 |
| **JavaScript (Nashorn)** | 中 | JDK11+ 已移除 | ❌ 过时 |
| **Java** | 最快 | 需编译打包 | 极复杂场景 |

### 3.2 JSR223 常用 API

```groovy
// ===== JSR223 PreProcessor / PostProcessor / Sampler =====

// 1. 变量操作
vars.put("token", "abc123");
def name = vars.get("username");

// 2. 属性操作
props.put("globalCounter", "42");

// 3. 日志
log.info("当前线程: " + ctx.getThreadNum());
log.warn("检测到异常值: " + errorCount);

// 4. SampleResult（仅 PostProcessor 和 Assertion 可用）
def responseCode = prev.getResponseCode();       // "200"
def responseBody = prev.getResponseDataAsString(); // 响应体
def latency = prev.getLatency();                 // 延迟 ms
prev.setSuccessful(false);                       // 标记失败

// 5. 当前 Sampler（仅 PreProcessor 可用）
sampler.addArgument("dynamicParam", "value");    // HTTP 请求动态加参
sampler.setProperty("HTTPSampler.path", "/new-path");

// 6. JSON 处理
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;

def json = new JsonSlurper().parseText(responseBody);
def userId = json.data.id;
log.info("提取到 userId: " + userId);

// 7. 文件操作（谨慎使用！影响性能）
new File("custom_data.txt").eachLine { line ->
    // 处理每行
}
```

### 3.3 实战：复杂签名生成

```groovy
// JSR223 PreProcessor — HMAC-SHA256 签名
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

def appId = "app123";
def timestamp = String.valueOf(System.currentTimeMillis());
def nonce = UUID.randomUUID().toString().replace("-", "");
def secret = vars.get("appSecret");

// step 1: 排序拼接
def params = [appId: appId, timestamp: timestamp, nonce: nonce];
def sortedKeys = params.keySet().sort();
def signStr = sortedKeys.collect { "${it}=${params[it]}" }.join("&") + secret;

// step 2: HMAC-SHA256
def mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
def sign = mac.doFinal(signStr.getBytes("UTF-8")).encodeHex().toString();

// step 3: 存入变量
vars.put("timestamp", timestamp);
vars.put("nonce", nonce);
vars.put("sign", sign);
```

---

## 4. 变量作用域与生命周期

```text
┌────────────────────────────────────────────────────────┐
│              变量/属性 作用域                           │
├────────────────────────────────────────────────────────┤
│                                                        │
│  Properties (props)                                    │
│  └── 全局共享，跨线程组                                 │
│      └── ${__P(key)} / ${__property(key)}             │
│                                                        │
│  Variables (vars)                                      │
│  ├── Test Plan 级别                                    │
│  │   └── User Defined Variables                        │
│  │       └── ${key}   所有线程组可用                    │
│  │                                                     │
│  ├── Thread Group 级别                                 │
│  │   └── CSV Data Set Config / User Defined Variables   │
│  │       └── ${key}   本线程组内可用                    │
│  │                                                     │
│  ├── 单线程内（动态产生）                               │
│  │   └── JSON Extractor / JSR223 vars.put()            │
│  │       └── 当前线程内可用，线程结束后销毁              │
│  │                                                     │
│  └── 跨线程传递                                        │
│      └── props.put() → 其他线程组可读                  │
│                                                        │
└────────────────────────────────────────────────────────┘
```

---

> 🎯 **核心要点**：变量 `${key}` 线程私有做传递，属性 `__P(key)` 全局共享做配置，JSR223 (Groovy) 做一切复杂逻辑，远离 BeanShell。

---

*创建于：2026年7月*
