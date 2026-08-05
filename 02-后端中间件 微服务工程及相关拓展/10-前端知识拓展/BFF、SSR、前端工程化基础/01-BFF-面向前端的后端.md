# 01 - BFF：Backend For Frontend

> 🎯 BFF 不是新语言，而是一种架构模式 — 为每种前端（Web/iOS/Android）定制的 API 聚合层，解决微服务下"前端调 N 个接口"的体验问题

---

## 目录

1. [BFF 是什么](#1-bff-是什么)
2. [BFF vs API Gateway](#2-bff-vs-api-gateway)
3. [BFF 典型职责](#3-bff-典型职责)
4. [Java 后端如何配合 BFF](#4-java-后端如何配合-bff)

---

## 1. BFF 是什么

```text
没有 BFF：
  Web → user-service + order-service + product-service（3 个请求）
  iOS → user-service + order-service + notification-service（3 个不同请求）
  → 每个前端自己拼数据、前端逻辑越来越重

有 BFF：
  Web  → BFF-Web  → 聚合 user/order/product → 1 个请求
  iOS  → BFF-iOS  → 聚合 user/order/notification → 1 个请求
  → BFF 层负责聚合裁剪，前端拿到即可渲染
```

| 概念 | 说明 |
|------|------|
| **BFF** | 为特定前端定制的 API 聚合层 |
| **本质** | 把前端需要的"数据拼装"从浏览器搬到服务端 |
| **不是** | API Gateway（Gateway 是通用路由，BFF 是定制聚合） |

---

## 2. BFF vs API Gateway

| 维度 | API Gateway | BFF |
|------|------------|-----|
| 服务对象 | 所有客户端通用 | 特定前端（Web/iOS/Android） |
| 职责 | 路由、鉴权、限流、跨域 | API 聚合、数据裁剪、格式转换 |
| 粒度 | 粗（按服务路由） | 细（按页面聚合） |
| 数量 | 1 个 | 每种前端 1 个 |
| 实现 | Kong / Spring Cloud Gateway | Node.js / Java / GraphQL |

```text
架构层级：

   Web  ──→  BFF-Web  ──┐
   iOS  ──→  BFF-iOS  ──┼──→  API Gateway  ──→  user-service
   小程序 ──→ BFF-Mini ──┘                       order-service
                                                   product-service
```

---

## 3. BFF 典型职责

```javascript
// ⭐ BFF 聚合示例（Node.js）
// 前端只需调 /page/order-detail → BFF 内部并行调 3 个服务

app.get('/page/order-detail/:id', async (req, res) => {
  const { id } = req.params;

  // 并行调用微服务
  const [order, user, logistics] = await Promise.all([
    api.get(`/orders/${id}`),
    api.get(`/users/${order.userId}`),     // 依赖 order
    api.get(`/logistics?orderId=${id}`)
  ]);

  // 聚合 + 裁剪 → 前端直接渲染
  res.json({
    order: { id: order.id, status: order.status, amount: order.amount },
    user: { name: user.name, phone: user.phone },  // ← 裁掉了 email/address
    logistics: { tracking: logistics.tracking, eta: logistics.eta }
  });
});
```

| BFF 职责 | 说明 | 示例 |
|----------|------|------|
| **API 聚合** | 合并多个微服务调用 | 订单页聚合 订单+用户+物流 |
| **数据裁剪** | 去掉前端不需要的字段 | 只返回 name/phone，不返回 email |
| **格式转换** | 字段重命名、单位转换 | `created_at` → `createTime` |
| **协议适配** | gRPC → JSON | 内部 gRPC → 前端 REST JSON |

---

## 4. Java 后端如何配合 BFF

```java
// ⭐ 策略1：BFF 直接调微服务 REST API（标准做法）
// BFF 是独立的 Node.js/Java 服务，通过 Feign 或 RestTemplate 调用

// ⭐ 策略2：GraphQL 充当 BFF（将聚合能力标准化）
// 后端只需暴露 GraphQL Schema，前端声明需要哪些字段

// ⭐ 策略3：后端提供"粗粒度"视图接口（没有独立 BFF 时的简化方案）
@RestController
public class PageController {

    @GetMapping("/page/order-detail/{id}")
    public Result<OrderDetailVO> getOrderPage(@PathVariable Long id) {
        Order order = orderService.getById(id);
        User user = userService.getById(order.getUserId());
        Logistics logistics = logisticsService.getByOrderId(id);
        return Result.ok(OrderDetailVO.of(order, user, logistics));
    }
    // 这是 BFF 逻辑下沉到微服务内部 — 不推荐，但快速
}
```

| 方案 | 适用 | 优点 | 缺点 |
|------|------|------|------|
| 独立 BFF（Node.js） | 中大型项目 | 前端可控、灵活聚合 | 额外服务运维 |
| GraphQL | 前端需求多变的项目 | 前端声明数据需求 | 后端复杂 |
| 微服务内聚合接口 | 小型项目 | 简单快速 | 服务职责不清晰 |

> 🎯 **BFF 核心理解**：它解决了"微服务太碎、前端调不过来"的矛盾。后端提供原子 API（单服务单职责），BFF 负责聚合（按页面拼装）。后端不需要为每个前端定制接口。
