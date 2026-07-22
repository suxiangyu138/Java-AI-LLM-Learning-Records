# Java在线支付 面试宝典
> 基于课程大纲全面覆盖微信支付/支付宝支付面试高频考点，从支付安全原理到实战接入全流程。

## 目录

- [一、基础概念速答（20题）](#一基础概念速答20题)
- [二、深度原理剖析（15题）](#二深度原理剖析15题)
- [三、实战场景题（12题）](#三实战场景题12题)
- [四、手写代码题（8题）](#四手写代码题8题)
- [五、系统设计题（5题）](#五系统设计题5题)
- [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
- [七、面试回答模板（Top 5高频题）](#七面试回答模板top-5高频题)
- [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（20题）

### 1.1 在线支付中什么是支付网关？

**支付网关**是商户系统和支付渠道（微信/支付宝/银行）之间的中间层，负责：

- **协议转换**：将商户的业务请求转换为支付渠道的协议格式。
- **安全隔离**：商户无需直接持有用户的银行卡信息，由网关代为处理敏感数据。
- **统一接入**：屏蔽各支付渠道的差异，提供统一的接口给商户端。

| 能力 | 说明 |
|------|------|
| **路由** | 根据金额、渠道费率、可用性等因素选择最佳支付渠道 |
| **签名/验签** | 对请求报文签名，对响应报文验签 |
| **对账** | 下载渠道账单，与商户内部订单逐笔比对 |
| **退款** | 通过原渠道将资金退还给用户 |

> 💡 面试中可以说："支付网关的核心价值在于安全与解耦——商户只需对接一个网关，就能接入多个支付渠道，同时用户的敏感金融信息不经过商户服务器。"

### 1.2 对称加密和非对称加密的区别？

| 特性 | 对称加密（AES） | 非对称加密（RSA） |
|------|----------------|------------------|
| **密钥** | 加密和解密使用**同一个密钥** | 一对密钥：公钥加密、私钥解密 |
| **速度** | 快（适合大数据量） | 慢（适合小数据量如密钥交换） |
| **安全性** | 密钥分发困难，容易被截获 | 公钥公开，私钥服务器保存，安全性高 |
| **典型算法** | AES、DES、3DES | RSA、ECC、SM2 |
| **应用场景** | 报文内容加密 | 数字签名、密钥协商、证书验证 |

> 💡 **Tip**：HTTPS 中实际是"混合加密"——用**非对称加密**安全地传输对称密钥，后续用**对称加密**传输业务数据，兼顾安全与性能。

### 1.3 什么是数字签名？支付中为什么需要？

数字签名是**私钥签名、公钥验签**的过程，用于保证数据的**完整性**和**不可否认性**。

```
发送方：原始数据 → Hash → 摘要 → 私钥加密 → 签名
接收方：原始数据 → Hash → 摘要A  ← 对比 ← 公钥解密签名 → 摘要B
```

在支付中的用途：
- **防止篡改**：订单金额、商户号等关键参数被签名保护，篡改后验签失败。
- **身份认证**：只有持有商户私钥的人才能生成有效签名，确认请求来自合法商户。
- **不可抵赖**：签名具有法律效力，商户不能否认已发起的支付请求。

### 1.4 什么是数字证书？和 CA 的关系？

**数字证书**是由**CA（Certificate Authority，证书颁发机构）**签发的电子认证文件，本质是将**公钥**与**持有者身份**绑定在一起。

```
证书结构：
┌─────────────────────────────┐
│  证书版本 / 序列号            │
│  签发者（CA 名称）            │
│  有效期（开始 ~ 结束）         │
│  主体（持有者名称、域名）       │
│  主体公钥（RSA 公钥）          │
│  CA 的数字签名（对以上内容签名） │
└─────────────────────────────┘
```

- **CA 的作用**：验证申请者的身份真实性，然后签发证书。浏览器/操作系统内置了可信 CA 根证书列表。
- **证书链**：`根 CA → 中间 CA → 服务器证书`，逐级验证信任传递。
- **支付场景**：微信支付平台证书用于商户验签（确认响应来自微信），商户证书用于微信验签（确认请求来自合法商户）。

### 1.5 HTTPS 中 SSL/TLS 握手如何保护支付数据？

```
Client Hello ────────→  Server Hello + 证书 ────────→
←── Client Key Exchange (用证书公钥加密预主密钥) ──→
←── 双方用预主密钥生成会话密钥（对称密钥） ──────→
←── Finished (加密) ──────────────────────────────→
```

1. **身份认证**：服务器出示 CA 签发的数字证书，客户端验证证书链。
2. **密钥协商**：客户端生成随机预主密钥，用服务器公钥加密后发送，只有服务器私钥能解密。
3. **会话加密**：双方用预主密钥派生出对称会话密钥，后续通信全部加密。

> 🎯 支付接口强制使用 HTTPS，确保订单金额、用户信息等在传输过程中不被窃听或篡改。

### 1.6 微信支付 V3 和 V2 的核心区别？

| 对比维度 | APIv2（旧版） | APIv3（新版） |
|---------|-------------|-------------|
| **证书模式** | 使用商户证书做双向认证 | 使用商户API v3密钥（HMAC-SHA256签名）+ 平台证书验签 |
| **签名算法** | MD5 / SHA-256 with RSA | HMAC-SHA256（请求签名）/ RSA-SHA256（响应签名） |
| **API风格** | XML 格式，参数复杂 | JSON 格式，RESTful 风格 |
| **通知方式** | 明文通知，需自行验签 | 加密通知，需用 v3 密钥解密 |
| **敏感信息** | 明文传输 | 自动加密（敏感字段自动加解密） |
| **平台证书** | 商户证书即可 | 需要下载并定期更新平台证书用于验签 |
| **回调解密** | 无需解密 | 通知体加密，需用 v3 密钥 AES-GCM 解密 |
| **推荐度** | 不再推荐新接入 | 官方推荐，新商户首选 |

> 💡 面试中高频问题："V3 最大的变化是安全性提升——引入 HMAC-SHA256 签名、AES-GCM 加密回调报文、平台证书独立于商户证书，从根本上杜绝了证书泄露导致的安全问题。"

### 1.7 微信支付 Native 支付的流程是怎样的？

```
用户 ──→ 商户网站 ──→ 商户后端 ──→ 微信支付 ──→ 返回 code_url
                                                 ↓
用户 ←─ 展示二维码 ←── 商户后端 ──→ 用户扫码 ──→ 微信支付
                                                 ↓
微信支付 ──异步通知──→ 商户后端 ──→ 更新订单状态
  ↑                        ↓
用户 ──→ 支付成功页 ←── 用户主动查询订单
```

**步骤说明**：
1. 商户后端调用微信支付 [Native下单API](https://api.mch.weixin.qq.com/v3/pay/transactions/native)，传入`金额`、`商品描述`、`商户订单号`等参数。
2. 微信返回 `code_url`（二维码链接），商户生成二维码展示给用户。
3. 用户使用微信扫描二维码完成支付。
4. 微信支付异步通知商户后端支付结果（需验签、解密、更新订单）。
5. 商户也可主动调用 [查询订单API](https://api.mch.weixin.qq.com/v3/pay/transactions/id/{id}) 兜底。

### 1.8 支付宝支付的流程是怎样的？

```
用户 ──→ 商户网站（点击"支付宝支付"）
         ↓
商户后端 ──→ 支付宝 [alipay.trade.page.pay] ──→ 返回 Form 表单（自动提交）
                                                     ↓
用户 ──→ 支付宝收银台 ──→ 用户输入密码完成支付
         ↓
同步通知：浏览器 GET 跳转回商户页面（仅通知支付完成，不可信任）
         ↓
异步通知：支付宝 POST 通知商户后端（核心，需验签+二次校验）
         ↓
商户后端 ──→ 校验签名、校验金额、校验 seller_id、校验 app_id
         ↓
         ──→ 更新订单状态
```

> ⚠️ **关键**：同步通知（return_url）仅作为用户体验展示，**绝对不能**以同步通知为准更新订单状态。必须以**异步通知（notify_url）**为准，且业务上还需配合主动查单兜底。

### 1.9 微信支付签名如何生成（V3）？

使用 **HMAC-SHA256** 算法，将请求的 `HTTP方法 + URL + 时间戳 + 随机串 + 请求体` 拼接后签名。

```
String buildMessage = httpMethod + "\n"
    + url + "\n"
    + timestamp + "\n"
    + nonceStr + "\n"
    + body + "\n";

String signature = HMAC_SHA256(buildMessage, apiV3Key);
```

请求头中携带：`Authorization: WECHATPAY2-SHA256-RSA2048 mchid="xxx",nonce_str="xxx",timestamp="xxx",serial_no="xxx",signature="xxx"`

### 1.10 支付宝签名如何生成（RSA2）？

使用 **SHA256withRSA** 算法，商户对请求参数按特定规则排序后拼接成待签名字符串，用商户私钥签名。

```java
// 1. 参数按 key 字典序排序
Map<String, String> sortedParams = new TreeMap<>(params);

// 2. 拼接成 key1=value1&key2=value2 格式（排除 sign 和 sign_type）
String content = AlipaySignature.getSignContent(sortedParams);

// 3. 使用商户私钥签名
String sign = AlipaySignature.rsaSign(content, merchantPrivateKey, "UTF-8", "RSA2");
```

### 1.11 微信支付验签流程（V3）？

```java
// 1. 从响应头获取 Wechatpay-Serial（平台证书序列号）
// 2. 根据序列号找到对应的平台证书（公钥）
// 3. 拼接验签原文：响应体\n + 时间戳\n + 随机串\n
String message = responseBody + "\n" + timestamp + "\n" + nonce + "\n";
// 4. 用平台证书公钥验证签名
boolean result = RSA.verify(message, signature, platformPublicKey);
```

> 💡 平台证书需定期从微信下载更新，证书失效会导致验签失败。建议使用定时任务每天检查更新。

### 1.12 支付宝验签流程？

```java
// 1. 获取支付宝异步通知的所有参数（不包含 sign、sign_type）
// 2. 按字典序排序拼接
// 3. 使用支付宝公钥 + RSA2 验签
boolean signVerified = AlipaySignature.rsaCheckV1(
    requestParams, alipayPublicKey, "UTF-8", "RSA2");
```

### 1.13 微信支付订单的状态流转？

```
         ┌──────────┐
         │  未支付   │
         └────┬─────┘
              │
    ┌─────────┼──────────┐
    ↓         │          ↓
┌──────┐      │    ┌────────┐
│ 已支付 │      │    │ 已关闭  │（超时/用户取消）
└──┬───┘      │    └────────┘
   │          │
   ↓          │
┌──────┐      │
│ 已退款 │←────┘
└──────┘
```

**数据库中订单状态枚举**：
```java
public enum OrderStatus {
    UNPAID(0, "未支付"),
    PAID(1, "已支付"),
    CLOSED(2, "已关闭"),
    REFUNDING(3, "退款中"),
    REFUNDED(4, "已退款");
}
```

### 1.14 什么是内网穿透？支付开发中为什么需要？

**内网穿透**将本地开发环境中运行的服务暴露到公网，使微信/支付宝的异步通知能够到达开发者的机器。

- **常用工具**：natapp、ngrok、frp
- **支付场景**：本地开发调试时，微信支付回调通知需要公网可访问的地址
- **配置方式**：启动内网穿透工具，获得公网 URL，将该 URL 配置为支付通知地址

> 💡 生产环境不需要内网穿透，回调地址直接配置为线上域名即可。

### 1.15 什么是沙箱环境？支付宝沙箱如何使用？

**沙箱环境**是支付宝提供的模拟测试环境，与正式环境隔离，免费用假账号测试。

使用步骤：
1. 登录 [支付宝开放平台](https://open.alipay.com) → 沙箱应用 → 获取 `app_id`、`商户私钥`、`支付宝公钥`
2. 下载沙箱版支付宝 APP（用于扫码/登录支付测试）
3. 获取沙箱买家账号（余额充足，无需真实扣款）
4. 商户请求指向沙箱网关：`https://openapi.alipaydev.com/gateway.do`

**和正式环境的唯一区别**：请求的网关地址不同，资金流向均为虚拟资金。

### 1.16 什么是商户订单号的唯一性？为什么重要？

微信和支付宝要求商户生成的订单号在**商户系统内全局唯一**。

- **重要性**：防止重复下单（幂等性），保证每一笔订单有唯一标识。
- **实现方式**：通常使用 `时间戳 + 随机数 + 业务前缀` 或使用数据库自增ID、雪花算法。
- **重复后果**：同一订单号多次支付可能导致资金异常（一单多付）。

```java
// 常用生成策略
String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    + RandomStringUtils.randomNumeric(6);
```

### 1.17 什么是支付对账？流程是怎样的？

**对账**是商户将内部订单记录与支付平台的账单明细进行逐笔比对，发现差异并处理。

```
商户每日定时任务：
1. 调用微信【申请账单API】获取账单下载 URL
2. 下载账单文件（CSV/压缩包）
3. 解析账单文件，逐笔与本地订单比对
4. 处理差异：
   - 长款（商户有→平台无）：平台丢单，补充发起查询
   - 短款（平台有→商户无）：商户漏记，补记订单
   - 金额不一致：发起人工核查
```

### 1.18 支付中什么是退款？退款的类型有哪些？

**退款**是指将已支付订单的资金原路退还给用户。

- **全额退款**：订单全款退回，退款后订单状态变为"已退款"。
- **部分退款**：只退订单的一部分金额（如只退其中一件商品），多次退款累计金额不超过订单总额。
- **退款查询**：调用退款查询API确认退款最终状态（成功/失败/处理中）。

> ⚠️ **注意**：退款不支持撤销，一旦发起退款请求，即使退款失败也要走退款查询确认最终结果。

### 1.19 微信支付的商户号和 APPID 分别是什么？

| 参数 | 说明 | 获取位置 |
|------|------|---------|
| **商户号（mch_id）** | 微信支付商户身份标识，用于结算、账单 | 微信支付商户平台 |
| **APPID** | 公众号/小程序的身份标识，用于标识用户归属 | 微信公众平台 |

**关系**：一个商户号可以绑定多个 APPID（公众号、小程序、APP等），一个 APPID 只能绑定一个商户号。

### 1.20 支付金额使用什么数据类型？为什么？

**使用 `int`（单位：分）或 `Long`，绝不使用 `float/double`。**

```java
// 正确：金额以分为单位，整型存储
Integer totalFee = 100; // 表示 1.00 元

// 错误：浮点数精度丢失
Double totalFee = 0.1 + 0.2; // 结果是 0.30000000000000004
```

- 微信支付金额单位是 **分**（整数）
- 支付宝金额单位是 **元**（接受字符串 "0.01" 避免精度问题）
- 数据库使用 `bigint` 或 `int` 存储分为单位的值

> 🎯 面试回答重点："金额计算必须规避浮点数精度问题。微信直接传分为单位整数，支付宝传字符串格式小数，数据库统一用分为单位的整型。"

---

## 二、深度原理剖析（15题）

### 2.1 微信支付 V3 签名原理详解（HMAC-SHA256）

V3 签名使用 **HMAC-SHA256** 算法，密钥为 **APIv3密钥**（商户平台设置的 32 位字符串）。

**签名步骤**：

```java
public static String buildSign(HttpMethod method, String url, String body,
                                String mchId, String serialNo, String apiV3Key) {
    // 1. 生成时间戳和随机串
    long timestamp = System.currentTimeMillis() / 1000;
    String nonceStr = UUID.randomUUID().toString().replaceAll("-", "");

    // 2. 构建签名原文
    String signStr = method.name() + "\n"
        + url + "\n"
        + timestamp + "\n"
        + nonceStr + "\n"
        + (body == null ? "" : body) + "\n";

    // 3. HMAC-SHA256 签名
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(keySpec);
    byte[] signBytes = mac.doFinal(signStr.getBytes(StandardCharsets.UTF_8));
    String signature = Base64.getEncoder().encodeToString(signBytes);

    // 4. 组装 Authorization 头
    String auth = "WECHATPAY2-SHA256-RSA2048 "
        + "mchid=\"" + mchId + "\","
        + "nonce_str=\"" + nonceStr + "\","
        + "timestamp=\"" + timestamp + "\","
        + "serial_no=\"" + serialNo + "\","
        + "signature=\"" + signature + "\"";
    return auth;
}
```

**V3 签名与 V2 的核心区别**：
- V2 使用**商户私钥**对 XML 报文整体做 RSA 签名（商户证书私钥）
- V3 使用 **APIv3 密钥**做 HMAC-SHA256，算法**对称**，不需要"私钥"概念，计算简单
- V3 在请求头中携带商户证书序列号，微信通过序列号找到对应商户证书验证请求身份

### 2.2 微信支付 V3 验签原理（平台证书验证响应）

V3 验签和签名使用不同的密钥体系——签名用 APIv3 密钥（对称），验签用平台证书（非对称）。

```java
public static boolean verifySign(String body, String timestamp, String nonce,
                                  String signature, String serialNo,
                                  Map<String, X509Certificate> platformCertMap) {
    // 1. 根据序列号找到对应平台证书
    X509Certificate cert = platformCertMap.get(serialNo);
    if (cert == null) {
        // 证书不存在 → 需要下载最新平台证书
        throw new RuntimeException("平台证书未找到，请更新证书");
    }

    // 2. 构建验签原文
    String message = body + "\n" + timestamp + "\n" + nonce + "\n";

    // 3. 用平台证书公钥验签
    try {
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initVerify(cert.getPublicKey());
        sign.update(message.getBytes(StandardCharsets.UTF_8));
        return sign.verify(Base64.getDecoder().decode(signature));
    } catch (Exception e) {
        return false;
    }
}
```

**为什么需要定期更新平台证书？**
- 微信的签名证书会定期轮换（通常不超过 5 年）
- 微信可能随时切换签名证书，旧证书过期后验签会失败
- 建议启动定时任务每周下载一次平台证书并缓存

### 2.3 支付宝 RSA2 签名与验签原理

支付宝采用 **RSA2（SHA256withRSA）**非对称签名体系。

**签名**（商户私钥签名请求参数）：
```java
// 1. 参数排序拼接
String content = "app_id=20210011...&biz_content={\"主体\":\"值\"}&charset=UTF-8&method=alipay.trade.page.pay&timestamp=2024-01-01 12:00:00&version=1.0";
// 2. 商户私钥签名
java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
sig.initSign(merchantPrivateKey);
sig.update(content.getBytes("UTF-8"));
byte[] signed = sig.sign();
String sign = Base64.encode(signed);
```

**验签**（支付宝公钥验证响应）：
```java
// 1. 获取通知参数（排除 sign、sign_type）
// 2. 按字典序拼接
// 3. 支付宝公钥验签
boolean pass = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");
```

> 💡 **RSA2（SHA256WithRSA）** 比老版 **RSA（SHA1WithRSA）** 更安全，因为 SHA-256 摘要长度 256 位，远大于 SHA-1 的 160 位，抗碰撞性更强。支付宝于 2019 年后强制使用 RSA2。

### 2.4 支付异步通知为什么比同步通知更可靠？

| 对比维度 | 同步通知（return_url） | 异步通知（notify_url） |
|---------|----------------------|----------------------|
| **触发方式** | 用户支付成功后浏览器 302 跳转 | 支付平台服务端 POST 通知 |
| **可靠性** | 用户关闭浏览器即丢失 | 支付平台会重试直到商户返回 success |
| **安全性** | HTTP 明文可伪造 | 包含签名，可验签 |
| **时效性** | 即时 | 通常 1-30 秒内到达 |
| **重试机制** | 无 | 有（最多 8 次重试） |
| **信任级别** | ❌ 不可信任 | ✅ 可信赖 |

**异步通知的重试策略**：
- 支付宝：首次通知后未收到 `success` 则按 2^N 间隔重试（2s, 4s, 8s, 16s...），最多 8 次
- 微信支付：首次通知失败后每隔 30 秒重试，最多重试 3 次

### 2.5 支付回调验签后的"业务内容二次校验"是什么？

验签只证明了**通知来自支付平台**（消息真实性），但没有验证**通知的业务数据是否正确**。二次校验就是对业务参数的再验证：

```java
// 支付宝异步通知处理完整流程
public String handleAlipayNotify(HttpServletRequest request) {
    Map<String, String> params = getAllParams(request);

    // Step 1: 签名验证（确保来自支付宝）
    boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");
    if (!signVerified) {
        return "failure";
    }

    // Step 2: 业务内容二次校验
    String appId = params.get("app_id");
    String outTradeNo = params.get("out_trade_no");
    String totalAmount = params.get("total_amount");
    String sellerId = params.get("seller_id");
    String tradeStatus = params.get("trade_status");

    // 2.1 校验 app_id 是否为本商户
    if (!myAppId.equals(appId)) {
        log.error("app_id 不匹配");
        return "failure";
    }

    // 2.2 校验订单是否存在
    Order order = orderService.getByOrderNo(outTradeNo);
    if (order == null) {
        log.error("订单不存在: {}", outTradeNo);
        return "failure";
    }

    // 2.3 校验金额是否一致（防止订单金额被篡改）
    // 注意：支付宝金额为元，本地订单金额为分
    BigDecimal alipayAmount = new BigDecimal(totalAmount);
    BigDecimal orderAmount = new BigDecimal(order.getTotalFee()).divide(new BigDecimal("100"));
    if (alipayAmount.compareTo(orderAmount) != 0) {
        log.error("金额不一致");
        return "failure";
    }

    // 2.4 校验 seller_id 是否为本商户（防止钓鱼通知）
    if (!mySellerId.equals(sellerId)) {
        log.error("seller_id 不匹配");
        return "failure";
    }

    // Step 3: 处理 TRADE_SUCCESS
    if ("TRADE_SUCCESS".equals(tradeStatus)) {
        // 重复通知处理 + 更新订单
    }

    return "success";
}
```

### 2.6 如何处理支付通知的重复通知问题？

支付通知**天然不是幂等的**，可能出现"一单多通知"（网络抖动、重试机制等）。处理方案：

**方案一：订单状态校验（最常用）**

```java
public synchronized String handleNotify(String orderNo, Integer paidAmount) {
    // 查询数据库订单
    Order order = orderMapper.selectByOrderNo(orderNo);

    // 关键：如果订单已支付，直接返回 success 不再处理
    if (order.getStatus() == OrderStatus.PAID.getCode()) {
        log.info("订单已支付，忽略重复通知: {}", orderNo);
        return "success"; // 告知支付平台不再重试
    }

    // 更新订单为已支付
    order.setStatus(OrderStatus.PAID.getCode());
    order.setPaymentTime(LocalDateTime.now());
    orderMapper.updateById(order);
    return "success";
}
```

**方案二：数据库唯一约束 + 幂等表**

```java
// 幂等表：id, order_no, notify_id, create_time
// order_no + notify_id 联合唯一索引
try {
    idempotentMapper.insert(new IdempotentRecord(orderNo, notifyId));
    // 插入成功 → 首次通知
    doProcessNotify(orderNo);
} catch (DuplicateKeyException e) {
    // 插入失败 → 重复通知，直接忽略
    log.info("重复通知，已忽略: {}", notifyId);
}
```

**方案三：Redis 分布式锁 + 状态双重校验（推荐）**

```java
public String handleNotify(String orderNo, Integer amount) {
    String lockKey = "pay:lock:" + orderNo;
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", Duration.ofSeconds(30));

    if (Boolean.FALSE.equals(locked)) {
        return "success"; // 其他线程在处理，直接返回
    }

    try {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order.getStatus() != OrderStatus.UNPAID.getCode()) {
            return "success"; // 已处理
        }
        // 执行业务更新
        order.setStatus(OrderStatus.PAID.getCode());
        orderMapper.updateById(order);
    } finally {
        redisTemplate.delete(lockKey);
    }
    return "success";
}
```

### 2.7 为什么支付回调中需要加"数据锁"？

**场景**：在同一时刻，可能同时收到**异步通知**和**用户的主动查询请求**（或定时任务触发的查单），两个请求并发处理同一订单，会出现：

```
时间线：
T1: 异步通知到达，判断订单未支付 → 准备更新为已支付
T2: 用户主动查单，判断订单未支付 → 准备更新为已支付（脏数据）
T3: 异步通知更新完成
T4: 主动查单更新完成（重复更新，不影响数据一致但浪费性能）

更严重的是：
T1: 退款请求到达 → 读取订单状态为已支付 → 执行退款
T2: 异步通知到达 → 读取订单状态为未支付（旧数据）→ 更新为已支付
结果：订单"已支付"但已退款，资金被冻结！
```

**解决方案**：
1. **数据库乐观锁**：`UPDATE order SET status = ?, version = version + 1 WHERE id = ? AND version = ?`
2. **数据库悲观锁**：`SELECT ... FOR UPDATE`（行级锁）
3. **Redis 分布式锁**：基于 `SETNX` 的互斥锁

> 💡 推荐策略：**Redis 锁 + 状态双重校验**，兼顾性能和安全性。

### 2.8 定时查单的作用是什么？如何实现？

**定时查单作为兜底手段**，解决以下问题：
1. **异步通知丢失**：网络异常导致服务器未收到通知
2. **异步通知延迟**：超过业务可接受的时间范围
3. **回调处理失败**：商户端代码异常导致未成功更新订单

**实现方案**：

```java
@Component
@Slf4j
public class OrderQueryTask {

    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    public void queryUnpaidOrders() {
        // 1. 查询超过一定时间（如5分钟）仍未支付的订单
        List<Order> unpaidOrders = orderMapper.selectUnpaidOrders(
            LocalDateTime.now().minusMinutes(5));

        for (Order order : unpaidOrders) {
            try {
                // 2. 调用微信查单API
                String tradeState = wechatPayClient.queryOrder(order.getOrderNo());

                if ("SUCCESS".equals(tradeState)) {
                    // 3. 已支付但本地未更新 → 补更新
                    order.setStatus(OrderStatus.PAID.getCode());
                    order.setPaymentTime(LocalDateTime.now());
                    orderMapper.updateById(order);
                    log.info("定时查单补单成功: {}", order.getOrderNo());
                } else if ("CLOSED".equals(tradeState)
                    || "REVOKED".equals(tradeState)
                    || "PAY_ERROR".equals(tradeState)) {
                    // 4. 异常状态 → 关闭订单
                    order.setStatus(OrderStatus.CLOSED.getCode());
                    orderMapper.updateById(order);
                }
                // NOTPAY → 继续等待
            } catch (Exception e) {
                log.error("查单异常: {}", order.getOrderNo(), e);
            }
        }
    }
}
```

### 2.9 微信支付退款流程是怎样的？

```
商户后端 ──→ 微信支付（申请退款API）
              ↓
微信支付 ──→ 审核退款请求
              ↓
         ┌────┴────┐
         ↓         ↓
      退款成功    退款失败（余额不足等）
         ↓
微信异步通知商户退款结果
         ↓
商户更新订单状态为已退款
```

**关键参数**：
```java
public void refund(String orderNo, Integer refundAmount, String reason) {
    RefundRequest request = new RefundRequest();
    request.setOutTradeNo(orderNo);           // 原商户订单号
    request.setOutRefundNo(generateRefundNo());// 商户退款单号
    request.setReason(reason);                // 退款原因
    request.setAmount(new RefundAmount()
        .setRefund(refundAmount)              // 退款金额（分）
        .setTotal(order.getTotalFee())        // 原订单金额（分）
        .setCurrency("CNY"));

    RefundResponse response = wechatPayClient.refund(request);
    // refundStatus: PROCESSING / SUCCESS / ABNORMAL / CLOSED
    log.info("退款结果: {}", response.getRefundStatus());
}
```

**注意**：
- 退款金额不能超过订单金额
- 支持多次部分退款，累计退款金额不超过原订单金额
- 退款结果以异步通知或查询为准，不要依赖同步返回

### 2.10 支付宝的同步通知和异步通知分别处理什么？

```java
// === 1. 同步通知（仅用于前端展示） ===
// 请求方式：GET
// URL：return_url?orderId=xxx&total_amount=xxx&timestamp=xxx&sign=xxx
// 处理：展示支付成功页面，不做业务更新
@GetMapping("/returnUrl")
public String returnUrl(HttpServletRequest request) {
    // 验签（可选，但不要以结果为准）
    boolean verified = AlipaySignature.rsaCheckV1(
        getAllParams(request), alipayPublicKey, "UTF-8", "RSA2");
    if (!verified) {
        return "支付失败页面";
    }
    // 只是展示给用户看，不更新订单
    return "支付成功页面";
}

// === 2. 异步通知（核心业务处理） ===
// 请求方式：POST
// URL：notify_url（商户配置的回调地址）
// 处理：验签→二次校验→更新订单→返回"success"
@PostMapping("/notifyUrl")
public String notifyUrl(HttpServletRequest request) {
    // 完整处理流程见 2.5 节
    // ...
    return "success"; // 关键：必须返回纯文本 success
}
```

### 2.11 微信支付 V3 通知如何解密？

V3 的通知体是 AES-GCM 加密的密文，需要使用 **APIv3 密钥** 解密。

```java
public String decryptNotifyBody(NotifyRequest notifyRequest, String apiV3Key) {
    // resource 结构：algorithm, ciphertext, associated_data, nonce
    Resource resource = notifyRequest.getResource();
    String ciphertext = resource.getCiphertext();
    String associatedData = resource.getAssociatedData();
    String nonce = resource.getNonce();

    // AES-256-GCM 解密
    try {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec key = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");

        GCMParameterSpec spec = new GCMParameterSpec(128,
            nonce.getBytes(StandardCharsets.UTF_8));

        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));

        byte[] plaintext = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(plaintext, StandardCharsets.UTF_8);
    } catch (Exception e) {
        throw new RuntimeException("解密失败", e);
    }
}
```

> 💡 解密后的明文包含 `event_type`、`summary`、`resource_type` 等字段，需判断 `event_type` 为 `TRANSACTION.SUCCESS` 后处理支付成功逻辑。

### 2.12 支付宝沙箱版和正式版的区别？

| 对比维度 | 沙箱环境 | 正式环境 |
|---------|---------|---------|
| **网关地址** | `openapi.alipaydev.com` | `openapi.alipay.com` |
| **资金** | 虚拟资金，不产生真实交易 | 真实资金 |
| **APP** | 沙箱版支付宝（独立 APP） | 正式支付宝 |
| **账号** | 沙箱买家/卖家账号（平台生成） | 真实商户账号 |
| **应用状态** | 沙箱应用（自动通过） | 需上线审核 |
| **签约** | 无需签约 | 需签约相关支付产品 |

### 2.13 支付金额精度问题为什么是高频考点？

**浮点数在计算机中无法精确表示所有十进制小数**。

```java
// 问题演示
System.out.println(0.1 + 0.2); // 0.30000000000000004

// 正确做法1：整型（分）
long amountInCents = 100; // 1.00元

// 正确做法2：BigDecimal（字符串构造）
BigDecimal amount = new BigDecimal("0.01");

// 错误：double 构造
BigDecimal wrong = new BigDecimal(0.01); // 0.010000000000000000208...
```

**各支付平台金额处理方式**：

| 平台 | 金额单位 | API 传输格式 | 数据库建议 |
|------|---------|-------------|-----------|
| 微信支付 | 分 | 整数（如 100 表示 1 元） | `int` / `bigint` |
| 支付宝 | 元 | 字符串（如 "0.01"） | `decimal(10,2)` 或 `bigint` 存分 |

> 🎯 **最佳实践**：**统一以分为单位存储**（int/bigint），对外展示时除以 100。与支付宝交互时转换为元（字符串）。

### 2.14 什么是支付平台的"账单API"？对账的目的是什么？

**账单API**是支付平台提供的交易流水下载接口，商户每日获取前一天的交易明细。

**微信账单API**：
```java
// 1. 申请账单（生成账单文件）
// GET /v3/bill/tradebill?bill_date=2024-01-01&bill_type=ALL
BillResponse bill = wechatPayClient.applyBill("2024-01-01", "ALL");
// 返回：download_url, hash_type, hash_value

// 2. 下载账单（获取文件流）
byte[] billData = httpClient.download(bill.getDownloadUrl());

// 3. 验证 hash
String actualHash = DigestUtils.sha256Hex(billData);
if (!actualHash.equals(bill.getHashValue())) {
    throw new RuntimeException("账单文件哈希校验失败");
}

// 4. 解析账单文件（CSV格式），逐笔对账
```

**对账的目的**：
1. **资金安全**：确保每一笔交易都有对应记录
2. **差异发现**：发现平台侧扣款但商户未记录、商户记录但平台未扣款等异常
3. **手续费核对**：确认支付平台收取的手续费是否正确

### 2.15 支付订单表的设计要点？

```sql
CREATE TABLE `t_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) NOT NULL COMMENT '商户订单号（唯一）',
  `payment_type` tinyint(4) NOT NULL COMMENT '支付类型 1-微信 2-支付宝',
  `product_id` bigint(20) DEFAULT NULL COMMENT '商品ID',
  `product_name` varchar(256) DEFAULT NULL COMMENT '商品名称',
  `total_fee` int(11) NOT NULL COMMENT '订单金额（分）',
  `refund_fee` int(11) DEFAULT '0' COMMENT '已退款金额（分）',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '订单状态 0-未支付 1-已支付 2-已关闭 3-退款中 4-已退款',
  `payment_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `transaction_id` varchar(64) DEFAULT NULL COMMENT '微信/支付宝交易号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';
```

**设计要点**：
- `order_no` 唯一索引：商户订单号全局唯一
- `status` 索引：方便按状态查询超时订单
- `payment_type` 字段：支持多支付渠道并存
- `refund_fee` 字段：记录已退款金额，支持多次部分退款
- `transaction_id` 字段：记录支付平台的交易流水号，用于对账和退款

---

## 三、实战场景题（12题）

### 3.1 用户支付成功后没有收到异步通知，怎么办？

**原因可能**：
1. 回调地址不可达（内网穿透失效、DNS 解析失败）
2. 防火墙拦截了支付平台的 IP
3. 商户端代码异常导致未返回 `success`，支付平台放弃重试

**解决方案**：
1. **定时查单兜底**（核心方案）：每 30 秒扫描超时未支付且已过期的订单，主动调用查单API
2. **手动查单按钮**：提供用户主动查询订单状态的功能
3. **检查日志**：排查是否收到了通知但处理异常
4. **回调地址配置**：检查是否配置为公网可达的 URL

### 3.2 支付宝通知验签通过，但金额不对怎么办？

**这是二次校验要解决的问题**。如果验签通过但金额不对，说明通知报文被篡改过（虽然签名被伪造的可能性极低，但需要防范）。

```java
// 场景：订单金额 100 元，通知中金额为 10000 元
// 立即拒绝，并且告警
if (alipayAmount.compareTo(orderAmount) != 0) {
    // 发送告警邮件/短信
    alertService.sendAlert("金额不一致告警", "订单 " + orderNo
        + " 本地金额:" + orderAmount + " 通知金额:" + alipayAmount);
    return "failure"; // 不返回 success，让支付宝继续重试
}
```

### 3.3 微信支付 V3 平台证书过期了怎么办？

**平台证书过期后，验签会失败**，导致无法确认支付通知的真实性。

**处理方案**：
```java
@Component
public class PlatformCertManager {

    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点更新
    public void updatePlatformCerts() {
        try {
            // 1. 获取平台证书列表
            List<PlatformCert> certs = wechatPayClient.queryCerts();

            // 2. 解析并缓存到本地
            for (PlatformCert cert : certs) {
                X509Certificate x509Cert = CertUtil.parseCertificate(cert.getPublicKey());
                String serialNo = x509Cert.getSerialNumber().toString(16).toUpperCase();

                // 3. 存入本地缓存（内存或数据库）
                certCache.put(serialNo, x509Cert);
                log.info("平台证书更新成功: {}", serialNo);
            }

            // 4. 清理过期证书
            certCache.entrySet().removeIf(entry -> {
                try {
                    entry.getValue().checkValidity();
                    return false;
                } catch (Exception e) {
                    return true;
                }
            });
        } catch (Exception e) {
            log.error("平台证书更新失败", e);
        }
    }
}
```

> 💡 **建议**：启动时首次拉取 + 每天定时更新。缓存中保留多个证书，验签时根据 serial_no 查找对应证书。

### 3.4 用户扫码后长时间未支付，订单如何处理？

**超时关单机制**：

```java
public void closeExpiredOrders() {
    // 1. 查询超过 30 分钟未支付的订单
    List<Order> expiredOrders = orderMapper.selectUnpaidOlderThan(
        LocalDateTime.now().minusMinutes(30));

    for (Order order : expiredOrders) {
        try {
            // 2. 调用微信关闭订单API
            wechatPayClient.closeOrder(order.getOrderNo());

            // 3. 更新本地订单状态为已关闭
            order.setStatus(OrderStatus.CLOSED.getCode());
            orderMapper.updateById(order);
            log.info("超时关单成功: {}", order.getOrderNo());
        } catch (WechatPayException e) {
            // 4. 如果订单已支付，则更新为已支付
            if ("ORDER_PAID".equals(e.getErrorCode())) {
                order.setStatus(OrderStatus.PAID.getCode());
                orderMapper.updateById(order);
            }
        }
    }
}
```

> ⚠️ 关闭订单前应先查询订单状态，避免"已支付"的订单被错误关闭。

### 3.5 部分退款如何实现？多次退款的限制？

```java
/**
 * 部分退款
 * @param orderNo 原订单号
 * @param refundAmount 退款金额（分）
 * @param reason 退款原因
 */
public void partialRefund(String orderNo, Integer refundAmount, String reason) {
    // 1. 查询订单
    Order order = orderMapper.selectByOrderNo(orderNo);

    // 2. 校验退款金额
    int maxRefundAmount = order.getTotalFee() - order.getRefundFee();
    if (refundAmount > maxRefundAmount) {
        throw new BusinessException("退款金额超过可退余额");
    }

    // 3. 发起退款
    RefundRequest request = new RefundRequest();
    request.setOutTradeNo(orderNo);
    request.setOutRefundNo(generateRefundNo());
    request.setReason(reason);
    request.setAmount(new RefundAmount()
        .setRefund(refundAmount)
        .setTotal(order.getTotalFee())
        .setCurrency("CNY"));

    RefundResponse response = wechatPayClient.refund(request);

    // 4. 更新本地退款金额记录
    order.setRefundFee(order.getRefundFee() + refundAmount);
    order.setStatus(OrderStatus.REFUNDING.getCode());
    orderMapper.updateById(order);
}
```

**限制**：
- 微信支付：同一订单最多支持 50 次部分退款
- 退款总额不能超过原订单金额（不含运费等不可退还部分）
- 部分退款后剩余金额需满足最低退款限额（支付宝：1 元起）

### 3.6 订单表为什么要加 payment_type 字段？

**场景**：同一个商户系统可能需要同时接入微信支付和支付宝支付。

```java
// 下单时指定支付类型
public Order createOrder(Integer productId, Integer paymentType) {
    String orderNo = generateOrderNo();
    Product product = productMapper.selectById(productId);

    Order order = new Order();
    order.setOrderNo(orderNo);
    order.setProductId(productId);
    order.setProductName(product.getName());
    order.setTotalFee(product.getPrice());
    order.setPaymentType(paymentType); // 1-微信 2-支付宝
    order.setStatus(OrderStatus.UNPAID.getCode());

    orderMapper.insert(order);
    return order;
}

// 支付时根据类型路由
public String pay(Order order) {
    if (order.getPaymentType() == 1) {
        // 微信支付 Native
        return wechatPayService.nativePay(order);
    } else if (order.getPaymentType() == 2) {
        // 支付宝支付
        return alipayService.pagePay(order);
    }
    throw new BusinessException("不支持的支付类型");
}
```

### 3.7 支付金额单位不统一（微信：分 / 支付宝：元）如何处理？

**统一抽象层处理**：

```java
/**
 * 金额转换工具
 */
public class MoneyUtil {
    // 元转分
    public static int yuanToFen(String yuan) {
        BigDecimal amount = new BigDecimal(yuan);
        return amount.multiply(new BigDecimal("100")).intValue();
    }

    // 分转元（支付宝需要）
    public static String fenToYuan(Integer fen) {
        BigDecimal amount = new BigDecimal(fen);
        return amount.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toString();
    }

    // 微信支付返回给前端的分转元
    public static String fenToYuanForDisplay(Integer fen) {
        if (fen == null) return "0.00";
        return fenToYuan(fen);
    }
}

// 使用示例
// 微信：直接传分
wechatPayRequest.setAmount(order.getTotalFee()); // 100 → 1元

// 支付宝：转元（字符串）
alipayRequest.setTotalAmount(MoneyUtil.fenToYuan(order.getTotalFee())); // "1.00"
```

### 3.8 Redis 分布式锁在支付回调中如何实现？

```java
@Service
public class PayNotifyService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "pay:lock:";
    private static final long LOCK_TTL_SECONDS = 30;

    public boolean tryLock(String orderNo) {
        String key = LOCK_PREFIX + orderNo;
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(LOCK_TTL_SECONDS));
        return Boolean.TRUE.equals(success);
    }

    public void unlock(String orderNo) {
        redisTemplate.delete(LOCK_PREFIX + orderNo);
    }

    public String processNotify(String orderNo, Integer amount) {
        // 1. 获取分布式锁
        if (!tryLock(orderNo)) {
            // 获取锁失败，说明其他线程正在处理，直接返回 success
            return "success";
        }

        try {
            // 2. 双重校验：锁内再次检查订单状态
            Order order = orderMapper.selectByOrderNo(orderNo);
            if (order.getStatus() != OrderStatus.UNPAID.getCode()) {
                log.info("订单已处理，忽略重复: {}", orderNo);
                return "success";
            }

            // 3. 执行业务更新（使用乐观锁防并发）
            int rows = orderMapper.updateStatusUnpaidToPaid(
                orderNo, OrderStatus.PAID.getCode(), LocalDateTime.now());
            if (rows == 0) {
                log.warn("订单状态并发更新失败: {}", orderNo);
                return "success"; // 已由其他线程更新
            }

            // 4. TODO: 后续业务处理（积分、通知等）

        } finally {
            // 5. 释放锁
            unlock(orderNo);
        }
        return "success";
    }
}
```

### 3.9 微信支付 V2 和 V3 混用场景如何处理？

**场景**：老项目使用 V2 接入，新功能需要升级 V3。

```java
/**
 * 支付服务适配层 - 隔离 V2/V3 差异
 */
@Service
public class UnifiedWechatPayService {

    // 根据商户配置决定使用 V2 还是 V3
    @Autowired(required = false)
    @Qualifier("wechatPayV3Client")
    private WechatPayClient v3Client;

    @Autowired(required = false)
    @Qualifier("wechatPayV2Client")
    private WechatPayV2Client v2Client;

    public NativePayResult nativePay(Order order) {
        if (useV3(order)) {
            return convertV3Result(v3Client.nativePay(buildV3Request(order)));
        } else {
            return convertV2Result(v2Client.nativePay(buildV2Request(order)));
        }
    }

    private boolean useV3(Order order) {
        // 按商户号、时间、商品类型等判断
        return order.getPaymentConfig().isV3Enabled();
    }
}
```

**注意点**：
- V2 和 V3 的**回调地址可以不同**，但建议统一
- V2 和 V3 的**商户密钥体系**不同，分别存储
- 迁移期可双轨运行，待 V2 订单处理完毕后关闭 V2 配置

### 3.10 用户投诉说"扣了款但订单显示未支付"如何处理？

**排查步骤**：

1. **查日志**：确认是否收到支付异步通知
2. **查数据库**：订单状态是否真的为"未支付"
3. **调用查单API**：向微信/支付宝查询该订单的真实状态
4. **根据查单结果处理**：
   - **支付平台显示已支付**：本地未更新 → 执行补单操作
   - **支付平台显示未支付**：可能是重复扣款预授权、红包抵扣等原因 → 联系支付平台客服
   - **支付平台显示已退款**：用户可能记错或退款未到账通知

```java
public String handleUserComplaint(String orderNo) {
    // 1. 查微信真实状态
    QueryOrderResponse response = wechatPayClient.queryOrder(orderNo);

    if ("SUCCESS".equals(response.getTradeState())) {
        // 2. 已支付但本地未更新 → 补单
        orderService.supplementOrder(orderNo, response.getTransactionId());
        return "已为您补单成功";
    } else if ("REFUND".equals(response.getTradeState())
        || "REFUNDING".equals(response.getTradeState())) {
        return "该订单已退款/退款中";
    } else {
        return "该订单在微信侧状态为：" + response.getTradeState();
    }
}
```

### 3.11 如何测试支付功能？测试用例设计？

| 测试类型 | 测试场景 | 预期结果 |
|---------|---------|---------|
| **正常流程** | 扫码→支付→收到异步通知 | 订单状态更新为已支付 |
| **金额边界** | 最小金额 0.01 元、最大金额 | 正确创建订单并支付 |
| **重复通知** | 支付后手动重发回调 | 订单状态不变，返回 success |
| **超时关单** | 创建订单后 30 分钟不支付 | 订单状态变为已关闭 |
| **退款** | 全额退款、部分退款 | 金额正确退回 |
| **退款超限** | 退款金额超过订单金额 | 接口返回错误 |
| **签名错误** | 篡改回调参数后发送 | 验签失败，返回 failure |
| **金额篡改** | 修改回调中的金额 | 二次校验失败，返回 failure |
| **并发** | 同时发起支付和退款 | 数据一致，不会出现状态异常 |
| **沙箱** | 支付宝沙箱全流程 | 与正式环境行为一致 |

### 3.12 支付回调中返回"success"和"failure"的区别？

| 返回值 | 支付平台处理行为 | 适用场景 |
|-------|----------------|---------|
| `success` | 停止重试，标记通知完成 | 正常处理、重复通知、订单不存在等无需继续重试的情况 |
| `failure` | 继续重试（按重试策略） | 临时异常（数据库连接失败）、验签失败希望重新通知的情况 |

```java
@PostMapping("/notify")
public String notify(HttpServletRequest request) {
    try {
        // 业务处理
        handleNotify(request);
        return "success"; // 处理成功，停止重试
    } catch (DataIntegrityViolationException e) {
        log.error("数据库异常，让支付宝重试", e);
        return "failure"; // 临时异常，让支付宝重试
    } catch (Exception e) {
        log.error("未知异常，让支付宝重试", e);
        return "failure";
    }
}
```

> ⚠️ 注意：重复通知场景下也要返回 `success`，否则会导致支付平台不断重试。

---

## 四、手写代码题（8题）

### 4.1 微信支付 Native 下单实现

```java
/**
 * 微信支付 Native 下单
 * @param order 订单信息
 * @return code_url（二维码链接）
 */
public String nativePay(Order order) {
    // 1. 构造请求参数
    String url = "https://api.mch.weixin.qq.com/v3/pay/transactions/native";
    String body = JSON.toJSONString(new HashMap<String, Object>() {{
        put("appid", wechatConfig.getAppId());
        put("mchid", wechatConfig.getMchId());
        put("description", order.getProductName());
        put("out_trade_no", order.getOrderNo());
        put("notify_url", wechatConfig.getNotifyUrl());
        put("amount", new HashMap<String, Object>() {{
            put("total", order.getTotalFee());   // 单位：分
            put("currency", "CNY");
        }});
    }});

    // 2. 生成签名并发送请求
    String authHeader = signUtil.buildAuthHeader("POST", url, body);
    HttpPost httpPost = new HttpPost(url);
    httpPost.setHeader("Authorization", authHeader);
    httpPost.setHeader("Content-Type", "application/json");
    httpPost.setEntity(new StringEntity(body, "UTF-8"));

    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
        String result = EntityUtils.toString(response.getEntity());

        if (response.getStatusLine().getStatusCode() == 200) {
            // 3. 解析 code_url
            JSONObject json = JSON.parseObject(result);
            return json.getString("code_url");
        } else {
            throw new WechatPayException("下单失败: " + result);
        }
    } catch (IOException e) {
        throw new WechatPayException("请求微信支付异常", e);
    }
}
```

### 4.2 微信支付异步通知处理（含验签&解密）

```java
/**
 * 微信支付V3 异步通知处理
 */
public String handleWechatNotify(HttpServletRequest request, String apiV3Key) {
    try {
        // 1. 获取请求头（用于验签）
        String wechatpaySerial = request.getHeader("Wechatpay-Serial");
        String wechatpaySignature = request.getHeader("Wechatpay-Signature");
        String wechatpayTimestamp = request.getHeader("Wechatpay-Timestamp");
        String wechatpayNonce = request.getHeader("Wechatpay-Nonce");

        // 2. 读取请求体
        String body = IOUtils.toString(request.getInputStream(), "UTF-8");

        // 3. 验签
        boolean verified = verifySign(body, wechatpayTimestamp,
            wechatpayNonce, wechatpaySignature, wechatpaySerial);
        if (!verified) {
            log.error("微信支付通知验签失败");
            return "failure";
        }

        // 4. 解密通知数据
        JSONObject notifyJson = JSON.parseObject(body);
        JSONObject resource = notifyJson.getJSONObject("resource");

        String ciphertext = resource.getString("ciphertext");
        String associatedData = resource.getString("associated_data");
        String nonce = resource.getString("nonce");

        String decrypted = aesGcmDecrypt(ciphertext, associatedData,
            nonce, apiV3Key);

        // 5. 解析解密后的数据
        JSONObject payData = JSON.parseObject(decrypted);
        String eventType = notifyJson.getString("event_type");

        if ("TRANSACTION.SUCCESS".equals(eventType)) {
            String orderNo = payData.getString("out_trade_no");
            String transactionId = payData.getString("transaction_id");
            String tradeState = payData.getString("trade_state");

            if ("SUCCESS".equals(tradeState)) {
                // 6. 更新订单（含防重复处理）
                orderService.processSuccessfulPay(orderNo, transactionId);
            }
        }

        return "success";
    } catch (Exception e) {
        log.error("处理通知异常", e);
        return "failure";
    }
}

private String aesGcmDecrypt(String ciphertext, String associatedData,
                              String nonce, String apiV3Key) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    SecretKeySpec key = new SecretKeySpec(
        apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
    GCMParameterSpec spec = new GCMParameterSpec(128,
        nonce.getBytes(StandardCharsets.UTF_8));
    cipher.init(Cipher.DECRYPT_MODE, key, spec);
    cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
    return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)),
        StandardCharsets.UTF_8);
}
```

### 4.3 支付宝统一收单下单并支付页面实现

```java
/**
 * 支付宝 统一收单下单并支付页面（电脑网站支付）
 */
public String alipayPagePay(Order order) {
    // 1. 构造业务参数
    AlipayTradePagePayModel model = new AlipayTradePagePayModel();
    model.setOutTradeNo(order.getOrderNo());
    model.setProductCode("FAST_INSTANT_TRADE_PAY");
    model.setTotalAmount(MoneyUtil.fenToYuan(order.getTotalFee())); // 元（字符串）
    model.setSubject(order.getProductName());
    model.setBody(order.getProductName());

    // 2. 构造请求
    AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
    request.setReturnUrl(alipayConfig.getReturnUrl());  // 同步通知地址
    request.setNotifyUrl(alipayConfig.getNotifyUrl());  // 异步通知地址
    request.setBizModel(model);

    // 3. 调用 sdk
    try {
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if (response.isSuccess()) {
            // 返回 form 表单 HTML（自动提交到支付宝）
            return response.getBody();
        }
        throw new BusinessException("支付宝下单失败");
    } catch (AlipayApiException e) {
        throw new BusinessException("支付宝接口异常", e);
    }
}
```

### 4.4 支付宝异步通知处理（含验签+二次校验）

```java
/**
 * 支付宝异步通知处理
 */
public String handleAlipayNotify(HttpServletRequest request) {
    try {
        // 1. 获取所有参数
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            params.put(entry.getKey(), entry.getValue()[0]);
        }

        // 2. 签名验证
        boolean signVerified = AlipaySignature.rsaCheckV1(
            params, alipayConfig.getAlipayPublicKey(), "UTF-8", "RSA2");
        if (!signVerified) {
            log.error("支付宝通知验签失败");
            return "failure";
        }

        // 3. 业务二次校验
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");       // 支付宝交易号
        String totalAmount = params.get("total_amount");
        String tradeStatus = params.get("trade_status");
        String sellerId = params.get("seller_id");
        String appId = params.get("app_id");

        // 3.1 校验 app_id
        if (!alipayConfig.getAppId().equals(appId)) {
            return "failure";
        }

        // 3.2 校验 seller_id
        if (!alipayConfig.getSellerId().equals(sellerId)) {
            return "failure";
        }

        // 3.3 校验订单是否存在
        Order order = orderMapper.selectByOrderNo(outTradeNo);
        if (order == null) {
            log.error("订单不存在: {}", outTradeNo);
            return "failure";
        }

        // 3.4 校验金额
        BigDecimal notifyAmount = new BigDecimal(totalAmount);
        BigDecimal orderAmount = new BigDecimal(
            MoneyUtil.fenToYuan(order.getTotalFee()));
        if (notifyAmount.compareTo(orderAmount) != 0) {
            log.error("金额不匹配");
            return "failure";
        }

        // 4. 处理 TRADE_SUCCESS
        if ("TRADE_SUCCESS".equals(tradeStatus)
            || "TRADE_FINISHED".equals(tradeStatus)) {

            // 5. 幂等处理（状态校验）
            if (order.getStatus() == OrderStatus.UNPAID.getCode()) {
                order.setStatus(OrderStatus.PAID.getCode());
                order.setTransactionId(tradeNo);
                order.setPaymentTime(LocalDateTime.now());
                orderMapper.updateById(order);
                log.info("订单支付成功: {}", outTradeNo);
            } else {
                log.info("重复通知，已忽略: {}", outTradeNo);
            }
        }

        return "success";
    } catch (Exception e) {
        log.error("处理支付宝通知异常", e);
        return "failure";
    }
}
```

### 4.5 微信支付申请退款实现

```java
/**
 * 微信支付退款
 */
public void refund(String orderNo, Integer refundAmount, String reason) {
    // 1. 查询订单
    Order order = orderMapper.selectByOrderNo(orderNo);
    if (order == null) {
        throw new BusinessException("订单不存在");
    }
    if (order.getStatus() != OrderStatus.PAID.getCode()) {
        throw new BusinessException("订单状态不支持退款");
    }

    // 2. 校验退款金额
    int remainingRefund = order.getTotalFee() - order.getRefundFee();
    if (refundAmount > remainingRefund) {
        throw new BusinessException("退款金额超过可退余额");
    }

    // 3. 生成退款单号
    String refundNo = "REFUND" + System.currentTimeMillis()
        + RandomStringUtils.randomNumeric(4);

    // 4. 构造退款请求
    String url = "https://api.mch.weixin.qq.com/v3/refund/domestic/refunds";
    JSONObject body = new JSONObject();
    body.put("out_trade_no", orderNo);
    body.put("out_refund_no", refundNo);
    body.put("reason", reason);
    body.put("notify_url", wechatConfig.getRefundNotifyUrl());

    JSONObject amount = new JSONObject();
    amount.put("refund", refundAmount);
    amount.put("total", order.getTotalFee());
    amount.put("currency", "CNY");
    body.put("amount", amount);

    // 5. 发送请求
    String authHeader = signUtil.buildAuthHeader("POST", url, body.toJSONString());
    HttpPost httpPost = new HttpPost(url);
    httpPost.setHeader("Authorization", authHeader);
    httpPost.setHeader("Content-Type", "application/json");
    httpPost.setEntity(new StringEntity(body.toJSONString(), "UTF-8"));

    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
        String result = EntityUtils.toString(response.getEntity());
        JSONObject resp = JSON.parseObject(result);

        String refundStatus = resp.getString("status");
        // 记录退款单
        refundMapper.insert(new RefundRecord(
            refundNo, orderNo, refundAmount, refundStatus, reason));
        log.info("退款申请结果: {} {}", refundNo, refundStatus);
    } catch (IOException e) {
        throw new RuntimeException("退款请求失败", e);
    }
}
```

### 4.6 定时任务查单+补单实现

```java
@Component
@Slf4j
public class ScheduledOrderQuery {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private WechatPayClient wechatPayClient;
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private OrderService orderService;

    /**
     * 每60秒执行一次：查询超时未支付且未关闭的订单
     */
    @Scheduled(fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void queryUnpaidOrders() {
        // 1. 查询5分钟前创建但未支付且未关闭的订单
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5);
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
            .eq(Order::getStatus, OrderStatus.UNPAID.getCode())
            .lt(Order::getCreateTime, deadline));

        if (orders.isEmpty()) {
            return;
        }

        for (Order order : orders) {
            try {
                if (order.getPaymentType() == 1) {
                    // 微信支付查单
                    processWechatQuery(order);
                } else if (order.getPaymentType() == 2) {
                    // 支付宝查单
                    processAlipayQuery(order);
                }
            } catch (Exception e) {
                log.error("查单失败 orderNo={}", order.getOrderNo(), e);
            }
        }
    }

    private void processWechatQuery(Order order) {
        QueryOrderResponse resp = wechatPayClient.queryOrder(order.getOrderNo());
        String tradeState = resp.getTradeState();

        switch (tradeState) {
            case "SUCCESS":
                log.info("查单发现已支付，补单: {}", order.getOrderNo());
                orderService.supplementOrder(order.getOrderNo(),
                    resp.getTransactionId());
                break;
            case "CLOSED":
            case "REVOKED":
                log.info("查单发现已关闭: {}", order.getOrderNo());
                order.setStatus(OrderStatus.CLOSED.getCode());
                orderMapper.updateById(order);
                break;
            case "NOTPAY":
                // 超过30分钟还未支付，关单
                if (order.getCreateTime()
                    .isBefore(LocalDateTime.now().minusMinutes(30))) {
                    wechatPayClient.closeOrder(order.getOrderNo());
                    order.setStatus(OrderStatus.CLOSED.getCode());
                    orderMapper.updateById(order);
                    log.info("超时关单: {}", order.getOrderNo());
                }
                break;
            default:
                log.warn("未知交易状态: {} {}", tradeState, order.getOrderNo());
        }
    }

    private void processAlipayQuery(Order order) {
        AlipayTradeQueryResponse resp = alipayClient.queryOrder(order.getOrderNo());
        if ("TRADE_SUCCESS".equals(resp.getTradeStatus())) {
            log.info("支付宝查单发现已支付，补单: {}", order.getOrderNo());
            orderService.supplementOrder(order.getOrderNo(), resp.getTradeNo());
        } else if ("TRADE_CLOSED".equals(resp.getTradeStatus())) {
            order.setStatus(OrderStatus.CLOSED.getCode());
            orderMapper.updateById(order);
        }
    }
}
```

### 4.7 微信支付 V3 获取并更新平台证书

```java
/**
 * 平台证书管理器 - 获取并缓存微信支付平台证书
 */
@Service
public class CertificateManager {

    private final Map<String, X509Certificate> certCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        downloadAndCacheCerts();
    }

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点更新
    public void scheduledUpdate() {
        downloadAndCacheCerts();
    }

    private void downloadAndCacheCerts() {
        try {
            String url = "https://api.mch.weixin.qq.com/v3/certificates";
            String authHeader = signUtil.buildAuthHeader("GET", url, null);

            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", authHeader);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String result = EntityUtils.toString(response.getEntity());
                JSONObject json = JSON.parseObject(result);
                JSONArray dataArray = json.getJSONArray("data");

                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject certItem = dataArray.getJSONObject(i);
                    String serialNo = certItem.getString("serial_no");
                    JSONObject encryptCert = certItem.getJSONObject("encrypt_certificate");

                    // 解密证书内容（使用 APIv3 密钥）
                    String certContent = decryptCertificate(
                        encryptCert.getString("ciphertext"),
                        encryptCert.getString("associated_data"),
                        encryptCert.getString("nonce"),
                        apiV3Key
                    );

                    // 解析 X509 证书
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    ByteArrayInputStream bais = new ByteArrayInputStream(
                        certContent.getBytes(StandardCharsets.UTF_8));
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(bais);

                    // 验证证书有效期
                    cert.checkValidity();

                    // 存入缓存
                    certCache.put(serialNo, cert);
                    log.info("平台证书更新成功: serialNo={}", serialNo);
                }
            }
        } catch (Exception e) {
            log.error("下载平台证书失败", e);
        }
    }

    /**
     * 根据证书序列号获取平台证书
     */
    public X509Certificate getCertificate(String serialNo) {
        X509Certificate cert = certCache.get(serialNo);
        if (cert == null) {
            throw new RuntimeException("平台证书未找到: " + serialNo);
        }
        return cert;
    }
}
```

### 4.8 数据库乐观锁更新订单状态（防并发）

```java
/**
 * 使用乐观锁更新订单状态
 * 解决并发场景下状态覆盖问题
 */
@Mapper
public interface OrderMapper {

    /**
     * 乐观锁更新：只有当前状态为 UNPAID 时才更新为 PAID
     */
    @Update("UPDATE t_order SET status = #{newStatus}, " +
        "payment_time = #{paymentTime}, " +
        "transaction_id = #{transactionId} " +
        "WHERE order_no = #{orderNo} AND status = #{oldStatus}")
    int updateStatusWithLock(
        @Param("orderNo") String orderNo,
        @Param("oldStatus") Integer oldStatus,
        @Param("newStatus") Integer newStatus,
        @Param("paymentTime") LocalDateTime paymentTime,
        @Param("transactionId") String transactionId
    );
}

// 使用示例
public boolean tryUpdatePaid(String orderNo, String transactionId) {
    int rows = orderMapper.updateStatusWithLock(
        orderNo,
        OrderStatus.UNPAID.getCode(),  // oldStatus：必须是未支付
        OrderStatus.PAID.getCode(),     // newStatus：修改为已支付
        LocalDateTime.now(),
        transactionId
    );
    return rows > 0;
    // rows == 0 表示 status 不是 UNPAID（已由其他线程更新）
}
```

---

## 五、系统设计题（5题）

### 5.1 设计一个支持多渠道（微信+支付宝）的支付系统

**架构设计**：

```
┌──────────────────────────────────────────┐
│             商户前端（H5/小程序/PC）        │
└────────────────┬─────────────────────────┘
                 │
┌────────────────┴─────────────────────────┐
│          统一支付网关（API Gateway）        │
│    - 路由分发 / 限流 / 鉴权 / 日志          │
└────────────────┬─────────────────────────┘
                 │
┌────────────────┴─────────────────────────┐
│          支付核心服务（Pay Core）           │
│                                           │
│  ┌─────────┐ ┌─────────┐ ┌───────────┐  │
│  │ 支付路由  │ │ 订单管理 │ │ 对账服务   │  │
│  ├─────────┤ ├─────────┤ ├───────────┤  │
│  │ 退款服务  │ │ 回调处理 │ │ 账单解析   │  │
│  └─────────┘ └─────────┘ └───────────┘  │
└──────┬──────────────────────┬────────────┘
       │                      │
┌──────┴──────┐    ┌─────────┴─────────┐
│ 微信支付渠道  │    │  支付宝支付渠道    │
│ 适配器 V3   │    │   适配器           │
│ 适配器 V2   │    │   沙箱适配器       │
└─────────────┘    └───────────────────┘
```

**关键设计**：

1. **渠道适配器模式**：定义 `PayChannel` 接口，微信和支付宝各自实现
   ```java
   public interface PayChannel {
       String pay(Order order);         // 支付
       void refund(RefundRequest req);  // 退款
       PayStatus query(String orderNo); // 查单
       String handleNotify(HttpServletRequest request); // 处理回调
   }
   ```

2. **统一订单模型**：屏蔽渠道差异，统一订单状态枚举

3. **回调幂等处理**：数据库唯一键 + 状态校验 + 分布式锁

4. **对账定时任务**：每日自动拉取账单并比对

5. **告警机制**：对账差异、回调异常、退款失败等自动告警

### 5.2 设计一个高可用的支付回调处理系统

**挑战**：
- 支付回调不能丢失（资金相关）
- 回调处理要幂等
- 回调处理不能阻塞

**设计要点**：

```java
// 1. 异步处理：接收回调后立即返回 success，异步处理业务逻辑
@PostMapping("/notify")
public String notify(HttpServletRequest request) {
    // 只有验签失败才同步返回 failure
    if (!signVerify(request)) {
        return "failure";
    }
    // 验签通过后，将原始参数发送到 MQ 异步处理
    notifyQueue.send(request.getAllParams());
    return "success"; // 立即返回，不阻塞支付平台
}

// 2. MQ 消费者处理业务
@Component
@RabbitListener(queues = "pay.notify.queue")
public void handleNotify(Map<String, String> params) {
    String orderNo = params.get("order_no");
    String lockKey = "pay:lock:" + orderNo;

    // 3. 分布式锁 + 状态校验
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", Duration.ofSeconds(30));
    if (Boolean.FALSE.equals(locked)) {
        log.info("订单 {} 正在处理中", orderNo);
        return;
    }
    try {
        processOrder(params); // 更新订单、发放积分、通知用户等
    } finally {
        redisTemplate.delete(lockKey);
    }
}
```

**高可用措施**：
- MQ 持久化：回调消息不丢失
- 死信队列：处理失败的消息进入死信队列，人工介入
- 定时任务兜底：即使 MQ 或 Redis 故障，定时查单也能补回状态
- 多活部署：回调地址配置多个 IP（如有需要）

### 5.3 设计支付对账系统

```
                    每日凌晨2:00 触发
                           │
                    ┌──────┴──────┐
                    │ 触发对账任务  │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              ↓            ↓            ↓
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ 拉取微信   │ │ 拉取支付宝 │ │ 导出商户   │
        │ 账单      │ │ 账单     │ │ 订单记录   │
        └────┬─────┘ └────┬─────┘ └────┬─────┘
             └────────────┼────────────┘
                          ↓
                   ┌──────────────┐
                   │ 对账引擎      │
                   │ 逐笔比对      │
                   └──────┬───────┘
                          ↓
              ┌────────────┼────────────┐
              ↓            ↓            ↓
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ 一致 ✅   │ │ 长款差异  │ │ 短款差异  │
        │ 跳过     │ │ 商户多→   │ │ 平台多→   │
        └──────────┘ │ 平台补单  │ │ 人工核查   │
                      └──────────┘ └──────────┘
```

**对账引擎核心逻辑**：

```java
public ReconciliationResult reconcile(String date) {
    // 1. 拉取平台账单
    List<PlatformBill> platformBills = billService.fetchPlatformBills(date);

    // 2. 查询商户本地订单
    List<Order> localOrders = orderMapper.selectByDate(date);

    // 3. 建立映射
    Map<String, PlatformBill> platformMap = platformBills.stream()
        .collect(Collectors.toMap(PlatformBill::getOrderNo, Function.identity()));
    Map<String, Order> localMap = localOrders.stream()
        .collect(Collectors.toMap(Order::getOrderNo, Function.identity()));

    List<ReconcileDiff> diffs = new ArrayList<>();

    // 4. 逐笔比对
    for (String orderNo : platformMap.keySet()) {
        Order local = localMap.get(orderNo);
        if (local == null) {
            diffs.add(new ReconcileDiff(orderNo, DiffType.SHORT, "平台有但本地无"));
        } else if (!platformMap.get(orderNo).getAmount().equals(local.getTotalFee())) {
            diffs.add(new ReconcileDiff(orderNo, DiffType.AMOUNT_MISMATCH,
                "金额不一致 平台:" + platformMap.get(orderNo).getAmount()
                    + " 本地:" + local.getTotalFee()));
        }
    }
    for (String orderNo : localMap.keySet()) {
        if (!platformMap.containsKey(orderNo)) {
            diffs.add(new ReconcileDiff(orderNo, DiffType.LONG, "本地有但平台无"));
        }
    }

    return new ReconciliationResult(date, diffs);
}
```

### 5.4 如何设计支付安全体系？

**多层安全防护**：

| 层级 | 安全措施 | 说明 |
|------|---------|------|
| **传输层** | HTTPS/TLS 1.2+ | 防止中间人攻击，加密传输数据 |
| **请求层** | 签名机制 | 商户私钥签名，保证请求完整性和不可否认性 |
| **响应层** | 验签机制 | 验证响应来自真实支付平台 |
| **回调层** | 签名验证 + 二次校验 | 验证通知真实性 + 业务参数一致性 |
| **存储层** | 敏感信息加密 | 商户密钥使用加密存储（非明文） |
| **接口层** | IP 白名单 | 回调通知校验来源 IP |
| **应用层** | 幂等控制 + 乐观锁 | 防止并发导致状态错乱 |
| **运维层** | 密钥定期轮换 | 商户证书、API 密钥定期更换 |

**密钥存储最佳实践**：
```yaml
# 错误：配置文件中硬编码
wechat:
  api-v3-key: abc123def456  # ❌ 明文暴露

# 正确：使用密钥管理服务
wechat:
  api-v3-key: ${WECHAT_API_V3_KEY}  # ✅ 环境变量
```

> 💡 生产环境建议使用配置中心（Nacos/Apollo）+ 密钥管理服务（KMS/HSM），结合密钥定期轮换策略。

### 5.5 设计一个支持高并发秒杀的支付接口

**挑战**：秒杀场景下，大量用户同时下单支付，需保证：
1. 库存不超卖
2. 订单不重复
3. 支付入口限流

**设计方案**：

```java
// 1. 前端 -> 后端：预扣库存（Redis Lua 脚本保证原子性）
public boolean tryDecrStock(Long productId, Integer quantity) {
    String script = "local stock = redis.call('get', KEYS[1]) " +
        "if not stock or tonumber(stock) < tonumber(ARGV[1]) then " +
        "return 0 end " +
        "redis.call('decrby', KEYS[1], ARGV[1]) " +
        "return 1";
    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Arrays.asList("stock:" + productId),
        String.valueOf(quantity));
    return result == 1;
}

// 2. 下单 -> 异步处理
public String createSeckillOrder(Long userId, Long productId) {
    // 限流：每个用户每秒一次
    String limitKey = "seckill:limit:" + userId;
    Boolean canAccess = redisTemplate.opsForValue()
        .setIfAbsent(limitKey, "1", Duration.ofSeconds(1));
    if (Boolean.FALSE.equals(canAccess)) {
        throw new BusinessException("操作太频繁");
    }

    // 预扣库存
    if (!tryDecrStock(productId, 1)) {
        throw new BusinessException("库存不足");
    }

    // 创建订单（状态：未支付）
    Order order = orderService.createOrder(userId, productId);
    redisTemplate.opsForValue().set("order:" + order.getOrderNo(),
        order.getTotalFee(), Duration.ofMinutes(30));

    // 返回支付二维码
    return paymentService.nativePay(order);
}

// 3. 支付回调 -> 释放库存或超时释放
// 超时未支付：定时任务释放库存（回补）
@Scheduled(fixedRate = 30000)
public void releaseExpiredStock() {
    // 查询超时未支付订单 → 释放 Redis 库存
}
```

---

## 六、常见坑点与最佳实践

### 6.1 常见坑点

| 坑点 | 错误做法 | 正确做法 |
|------|---------|---------|
| **金额用 double** | `double total = 0.1;` 精度丢失 | 分为单位 int/long，或 `BigDecimal(String)` |
| **同步通知处理业务** | 在 return_url 中更新订单状态 | 只做展示，核心业务在异步通知中处理 |
| **重复通知未处理** | 收到回调直接更新订单 | 先判断订单状态，已支付则跳过 |
| **回调没有幂等** | 不加锁直接更新 | 分布式锁 + 乐观锁 + 状态校验 |
| **证书管理缺失** | 平台证书写死，不更新 | 定时拉取+缓存+定期轮换 |
| **订单号不唯一** | 简单时间戳 + 并发重复 | 时间戳 + 随机数 / 雪花算法 |
| **忽略二次校验** | 只看签名通过就更新 | 校验金额、seller_id、app_id |
| **退款金额未累积** | 部分退款只减原始金额 | 使用 `refund_fee` 字段记录已退总额 |
| **定时查单缺失** | 完全依赖异步通知 | 定时任务查单做兜底 |
| **数据库未建索引** | 按 order_no/status 查询慢 | 建立唯一索引和查询索引 |

### 6.2 最佳实践清单

**订单相关**：

```java
// 1. 订单号生成（雪花算法或时间戳+随机数）
String orderNo = String.format("%s%s%d",
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
    RandomStringUtils.randomNumeric(6),
    userId % 1000);

// 2. 订单状态变更使用枚举，不使用魔法数字
public enum OrderStatus {
    UNPAID(0), PAID(1), CLOSED(2), REFUNDING(3), REFUNDED(4);
}

// 3. 金额统一用 int（分），前端展示时 ÷100 保留两位小数
```

**配置相关**：

```yaml
# API 密钥通过环境变量注入，不进代码仓库
wechat:
  app-id: ${WX_APP_ID}
  mch-id: ${WX_MCH_ID}
  api-v3-key: ${WX_API_V3_KEY}
  mch-serial-no: ${WX_MCH_SERIAL_NO}
  private-key-path: ${WX_PRIVATE_KEY_PATH}
  notify-url: https://your-domain.com/api/pay/wechat/notify
```

**代码架构**：

```java
// 1. 使用适配器模式屏蔽渠道差异
PayChannel channel = PayChannelFactory.getChannel(order.getPaymentType());
String qrCodeUrl = channel.pay(order);

// 2. 统一返回结果结构（方便前端处理）
public class R<T> {
    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) { ... }
    public static <T> R<T> error(int code, String msg) { ... }
}

// 3. 支付日志链路追踪
MDC.put("traceId", orderNo); // 方便查单时追踪日志
```

**测试相关**：

```java
// 1. 沙箱环境测试全覆盖后再上线
// 2. 金额边界测试：0.01元、999999.99元
// 3. 并发测试：同一订单多次回调
// 4. 退款测试：多次部分退款累积 = 订单金额
// 5. 超时测试：超过30分钟未支付订单自动关闭
```

---

## 七、面试回答模板（Top 5高频题）

### 7.1 "请说一下微信支付的支付流程"

> **回答要点**：Native 支付流程、异步通知、回调验签
>
> "微信支付 Native 模式是线下展示二维码、用户扫码支付的模式。整体流程分五步：
>
> 第一步，商户后端调用微信支付 Native 下单 API，传入商品描述、金额（以分为单位的整数）、商户订单号、回调地址等参数，请求头使用 APIv3 密钥做 HMAC-SHA256 签名。
>
> 第二步，微信返回 `code_url`，这是一个微信支付的二维码链接。商户后端返回给前端，前端使用二维码库（如 qrcode.js）生成二维码展示。
>
> 第三步，用户使用微信扫描二维码，看到支付页面并输入密码完成支付。
>
> 第四步，微信支付通过异步 POST 请求通知商户后端。商户后端需要：先从请求头获取签名信息和时间戳，对响应体进行**验签**（用平台证书的公钥做 RSA 验签），验签通过后再使用 APIv3 密钥对密文进行 AES-GCM **解密**，最后解析出支付结果。
>
> 第五步，商户后端处理支付结果：先判断订单当前状态（幂等校验，防止重复通知），未支付则更新为已支付，记录微信交易号，然后返回 `success` 告知微信停止重试。同时我们还有定时任务兜底，每 30 秒查一次超时未支付订单，主动调用微信查单 API 确保状态一致。"

### 7.2 "支付宝和微信支付的回调有什么异同？"

> **回答要点**：验签方式、通知结构、幂等处理
>
> **相同点**：
> - 都是异步通知机制，验证签名后才能信任
> - 都需要商户返回 `success` 或 `failure`
> - 都需要做幂等处理（重复通知防重）
>
> **不同点**：
> - **签名算法**：微信 V3 用 HMAC-SHA256（APIv3 密钥）签名请求，RSA-SHA256（平台证书）验签响应；支付宝用 RSA2（SHA256withRSA）同时对请求签名和响应验签。
> - **回调内容**：微信 V3 回调体加密（AES-GCM 解密后才能读取）；支付宝回调体明文（参数在 URL query 中）。
> - **验签方式**：微信需要从响应头取签名信息拼接验签；支付宝从请求参数中取签名。
> - **二次校验**：支付宝需要在签名验证后额外校验 `app_id`、`seller_id`、`total_amount` 是否与本地一致；微信 V3 解密后直接获取可信数据。
> - **通知重试**：支付宝最多 8 次，间隔递增；微信最多 3 次，每次间隔 30 秒。"

### 7.3 "支付回调中怎么保证幂等性和防止并发？"

> **回答要点**：状态校验、分布式锁、乐观锁
>
> "幂等性保证的核心是：**同一笔订单的多次回调，最终业务结果一致**。我分为三个层面来保证：
>
> **第一层：订单状态校验（最基础）**
> 处理回调时先查数据库订单状态，如果已经是 '已支付' 就直接返回 `success` 不再处理。这能过滤掉网络重试导致的重复通知。
>
> **第二层：Redis 分布式锁**
> 在更新订单前先获取分布式锁，key 为 `pay:lock:{order_no}`，TTL 设为 30 秒。获取不到锁说明有其他线程在处理，直接返回 `success`。获取到锁后进行业务处理，完成后释放锁。
>
> **第三层：数据库乐观锁（最终防线）**
> 更新订单时使用 `UPDATE t_order SET status = ? WHERE order_no = ? AND status = ?`，where 条件指定旧状态为 '未支付'。如果影响行数为 0，说明订单状态已被其他线程修改，放弃更新。
>
> 三层结合：**状态校验**过滤大部分重复，**分布式锁**防止并发冲突，**乐观锁**确保数据最终一致。这样即使网络抖动、MQ 重复消费、定时任务并发查单等极端情况，也不会出现状态错乱。"

### 7.4 "对称加密和非对称加密在支付中如何应用的？"

> **回答要点**：签名用非对称、数据加密用对称、HTTPS 混合加密
>
> "在支付系统中，两种加密算法各司其职：
>
> **非对称加密（RSA）——用于签名和验签**
> 商户用私钥对请求参数签名，支付宝/微信用商户公钥验签，保证请求来自合法商户。反过来，微信/支付宝用它们的私钥签名响应，商户用平台证书公钥验签，保证响应来自真实支付平台。
> 这是非对称加密的核心价值：**私钥签名、公钥验签，实现身份认证和防抵赖**。
>
> **对称加密（AES）——用于数据加密**
> 微信支付 V3 回调通知体使用 AES-256-GCM 加密。商户收到密文后用 APIv3 密钥（对称密钥）解密获取明文。对称加密的优势是速度快，适合对较大数据量加解密。
>
> **混合加密——HTTPS**
> 支付 API 全部基于 HTTPS。TLS 握手阶段使用非对称加密传输预主密钥，后续通信使用对称加密（如 AES）加密业务数据。兼顾了非对称加密的安全性（密钥交换）和对称加密的高效性（数据传输）。
>
> 简单总结：**RSA 做签名认证，AES 做数据加密，HTTPS 保证传输通道安全**。"

### 7.5 "如何处理支付中的超时订单？"

> **回答要点**：超时关单+定时查单+兜底
>
> "超时订单处理分两个维度：主动关单和定时查单。
>
> **主动关单（用户取消）**
> 用户在支付页点击取消时，前端调用关单接口，后端调用微信关闭订单 API，将本地订单状态更新为 '已关闭'。注意关单前需查单确认订单未支付，防止误关已支付订单。
>
> **定时关单（系统自动）**
> 使用 Spring @Scheduled 定时任务，每 30 秒扫描创建时间超过 30 分钟且状态为 '未支付' 的订单。对于这些订单：先调用微信查单 API 确认是否真的未支付，确实未支付则调用关单 API 并更新本地状态；如果查单发现已支付，则执行补单操作。
>
> **兜底机制**
> 定时查单本身也是为了兜底异步通知丢失的情况。对于超过 5 分钟（这个阈值可配）未收到回调的未支付订单，定时查单会主动查询支付平台的真实状态，发现已支付就补更新，发现已关闭就关单。
>
> 核心原则：**关单前必须查单确认**，因为用户可能在关单 API 调用前刚完成支付，如果不查单直接关单，会引发'已支付但订单已关闭'的资金异常。"

---

## 八、快速查漏补缺Checklist

### 基础概念
- [ ] 支付网关的作用
- [ ] 对称加密 vs 非对称加密（AES vs RSA）
- [ ] 数字签名原理（签名 + 验签）
- [ ] 数字证书与 CA 证书链
- [ ] HTTPS SSL/TLS 握手流程
- [ ] 微信支付 V3 vs V2 核心区别
- [ ] 支付宝 RSA2 签名算法

### 支付流程
- [ ] 微信 Native 支付完整流程
- [ ] 支付宝统一收单支付流程
- [ ] 同步通知 vs 异步通知区别
- [ ] 异步通知重试策略
- [ ] 通知中返回 "success" vs "failure"

### 安全验签
- [ ] 微信 V3 HMAC-SHA256 签名生成
- [ ] 微信 V3 平台证书验签流程
- [ ] 支付宝 RSA2 签名生成
- [ ] 支付宝验签 + 二次校验
- [ ] 微信 V3 通知 AES-GCM 解密
- [ ] 平台证书定时更新机制

### 幂等与并发
- [ ] 订单状态校验防重复通知
- [ ] Redis 分布式锁实现
- [ ] 数据库乐观锁更新状态
- [ ] 幂等表方案

### 查单与补单
- [ ] 定时查单实现（@Scheduled）
- [ ] 超时关单逻辑
- [ ] 补单操作流程

### 退款
- [ ] 全额退款实现
- [ ] 部分退款 + 退款金额累积
- [ ] 退款查询

### 对账
- [ ] 申请账单 API
- [ ] 下载账单 + 哈希校验
- [ ] 对账引擎逐笔比对逻辑

### 金额处理
- [ ] 整型存分（int / bigint）
- [ ] 微信：直接传分
- [ ] 支付宝：传元（字符串）
- [ ] BigDecimal 字符串构造

### 系统设计
- [ ] 多渠道支付统一架构（适配器模式）
- [ ] 高可用回调处理（MQ 异步）
- [ ] 支付安全体系（传输/请求/响应/存储/接口）
- [ ] 高并发秒杀支付方案

### 代码实现
- [ ] Native 下单
- [ ] 回调验签 + 解密
- [ ] 通知处理 + 幂等
- [ ] 退款
- [ ] 定时查单
- [ ] 平台证书更新

---

> 🎯 **面试核心要诀**：支付面试的核心是**安全**和**可靠性**。回答时要突出：1）签名验签保证安全；2）幂等处理+分布式锁+乐观锁保证可靠性；3）定时查单兜底防止通知丢失；4）金额用整型避免精度问题。能结合代码实例说明最佳实践，会让面试官对你印象深刻。

> 💡 **准备建议**：可将手写代码题（第四部分）中的 8 个代码片段背熟或理解透，面试时能口述或手写出核心逻辑即可。另外建议在实际项目中亲自对接一次微信和支付宝，理解每个参数的含义和处理逻辑，这是面试中最大的加分项。
