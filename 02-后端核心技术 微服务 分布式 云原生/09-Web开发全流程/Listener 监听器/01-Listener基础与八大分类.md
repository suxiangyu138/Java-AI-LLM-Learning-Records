# 01-Listener 基础与八大分类
> 事件模型、八大监听器分类、触发时机表——"监听器家族全家福"

## 📚 目录
1. [事件模型](#1-事件模型)
2. [八大监听器分类](#2-八大监听器分类)
3. [生命周期监听器（三个）](#3-生命周期监听器三个)
4. [属性监听器（三个）](#4-属性监听器三个)
5. [会话绑定监听器（两个）](#5-会话绑定监听器两个)
6. [触发时机总表](#6-触发时机总表)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. 事件模型

```text
事件驱动模型三要素：
  ① 事件源：容器（ServletContext/Session/Request）
  ② 事件对象：xxxEvent（携带信息）
  ③ 监听器：实现接口，事件发生时被回调

流程：
容器发生事件（如应用启动）
  → 创建事件对象
  → 通知所有注册的监听器
  → 调用对应回调方法
```

> 🎯 **理解方式**：与观察者模式完全一致——**监听器"订阅"容器事件，容器"发布"事件时自动通知**。

## 2. 八大监听器分类

| 分类 | 监听器 | 数量 |
|------|--------|:---:|
| 生命周期 | ServletContextListener / HttpSessionListener / ServletRequestListener | 3 |
| 属性变化 | ServletContextAttributeListener / HttpSessionAttributeListener / ServletRequestAttributeListener | 3 |
| 会话绑定 | HttpSessionBindingListener / HttpSessionActivationListener | 2 |

```text
记忆框架：
  三类对象（Context/Session/Request）× 两类事件（生命周期/属性）= 6
  + 2 个会话特殊绑定 = 8
```

> 🎯 **八大监听器框架**：**3 个对象 × 2 类事件（生命周期+属性）= 6 个** + 2 个会话绑定——记住这个乘法就记住了分类。

## 3. 生命周期监听器（三个）

```java
// ① 应用生命周期（最常用）
public class AppListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("应用启动");
    }
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("应用关闭");
    }
}

// ② 会话生命周期
public class SessionListener implements HttpSessionListener {
    public void sessionCreated(HttpSessionEvent se) {
        System.out.println("会话创建: " + se.getSession().getId());
    }
    public void sessionDestroyed(HttpSessionEvent se) {
        System.out.println("会话销毁");
    }
}

// ③ 请求生命周期
public class RequestListener implements ServletRequestListener {
    public void requestInitialized(ServletRequestEvent sre) {
        System.out.println("请求开始");
    }
    public void requestDestroyed(ServletRequestEvent sre) {
        System.out.println("请求结束");
    }
}
```

| 监听器 | 回调 | 触发 |
|--------|------|------|
| ServletContextListener | contextInitialized/Destroyed | 应用启动/关闭 |
| HttpSessionListener | sessionCreated/Destroyed | 会话创建/销毁 |
| ServletRequestListener | requestInitialized/Destroyed | 请求开始/结束 |

> 🎯 **最常用的是 ServletContextListener**：应用启动时做初始化（缓存预热/配置加载/定时任务启动）——见 [02](02-ServletContextListener与启动初始化.md)。

## 4. 属性监听器（三个）

```java
// 属性变化监听器（以 Session 属性为例）
public class SessionAttrListener implements HttpSessionAttributeListener {
    public void attributeAdded(HttpSessionBindingEvent se) {
        System.out.println("添加属性: " + se.getName() + "=" + se.getValue());
    }
    public void attributeRemoved(HttpSessionBindingEvent se) {
        System.out.println("删除属性: " + se.getName());
    }
    public void attributeReplaced(HttpSessionBindingEvent se) {
        System.out.println("替换属性: " + se.getName());
    }
}
```

| 属性监听器 | 回调三方法 |
|-----------|-----------|
| ServletContextAttributeListener | attributeAdded/Removed/Replaced |
| HttpSessionAttributeListener | attributeAdded/Removed/Replaced |
| ServletRequestAttributeListener | attributeAdded/Removed/Replaced |

> 💡 属性监听器三个接口结构相同（增/删/改三方法）——**区别只在于监听哪个对象的属性**（Context/Session/Request）。

## 5. 会话绑定监听器（两个）

```java
// ① 对象被放入/移出 Session 时通知对象本身
public class User implements HttpSessionBindingListener {
    public void valueBound(HttpSessionBindingEvent event) {
        System.out.println("用户对象被放入 Session");
    }
    public void valueUnbound(HttpSessionBindingEvent event) {
        System.out.println("用户对象被移出 Session");
    }
}

// ② 会话激活/钝化（序列化迁移）
public class Data implements HttpSessionActivationListener {
    public void sessionWillPassivate(HttpSessionEvent se) {
        System.out.println("会话即将钝化（序列化）");
    }
    public void sessionDidActivate(HttpSessionEvent se) {
        System.out.println("会话已激活（反序列化）");
    }
}
```

| 绑定监听器 | 时机 | 场景 |
|-----------|------|------|
| HttpSessionBindingListener | 对象放入/移出 Session | 对象自身感知（如释放资源） |
| HttpSessionActivationListener | 会话钝化/激活（序列化） | 分布式 Session 迁移（见会话体系） |

> 💡 **绑定监听器与普通监听器的区别**：普通监听器是"全局监听 Session"；绑定监听器是"对象自己感知被放入/移出"——**由对象实现接口，不是注册全局监听**。

## 6. 触发时机总表

| 事件 | 监听器 | 回调 |
|------|--------|------|
| 应用启动 | ServletContextListener | contextInitialized |
| 应用关闭 | ServletContextListener | contextDestroyed |
| 会话创建 | HttpSessionListener | sessionCreated |
| 会话销毁 | HttpSessionListener | sessionDestroyed |
| 请求开始/结束 | ServletRequestListener | requestInitialized/Destroyed |
| 属性增/删/改 | 三个属性监听器 | attributeAdded/Removed/Replaced |
| 对象入/出 Session | HttpSessionBindingListener | valueBound/Unbound |
| 会话钝化/激活 | HttpSessionActivationListener | willPassivate/DidActivate |

## 7. 核心要点

> 🎯 **核心要点**：
> - 事件模型：容器（事件源）→ 事件对象 → 监听器回调（观察者模式）；
> - 八大分类框架：**3 对象 × 2 事件 = 6 + 2 会话绑定**；
> - 生命周期三监听器：Context（应用）/Session（会话）/Request（请求）；
> - 属性三监听器：增/删/改三方法同构；
> - 绑定两监听器：对象自身感知（入/出 Session、钝化/激活）；
> - **最常用：ServletContextListener**（启动初始化，见 02）；
> - 面试：八大分类 + 各自回调方法 + 最常用哪个。

## 8. 参考来源

- [Jakarta Servlet 规范（Listener 接口）](https://jakarta.ee/specifications/servlet/)
- [Baeldung：Servlet Listeners](https://www.baeldung.com/java-servlet-listeners)

---

**下一模块**：[02-ServletContextListener与启动初始化](02-ServletContextListener与启动初始化.md)　/　**返回总览**：[00-总览](00-Listener监听器总览.md)
