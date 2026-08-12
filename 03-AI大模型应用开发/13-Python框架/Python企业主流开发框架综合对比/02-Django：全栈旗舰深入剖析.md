# Django：全栈旗舰深入剖析

> Django 是 Python 世界唯一"开箱即全栈"的框架：ORM、Admin、认证、迁移、表单、后台任务全部内置。2026 年的 Django 6.0/6.1 用内置后台任务与原生 CSP 补齐了最后两块短板——"运营后台 + 内容管理"类应用的第一选择没有悬念

---

## 📚 目录

1. [MTV 架构与电池内置哲学](#1-mtv-架构与电池内置哲学)
2. [核心组件：ORM / Admin / 迁移 / 认证](#2-核心组件orm--admin--迁移--认证)
3. [Django 6.0 与 6.1：2026 版本要点](#3-django-60-与-612026-版本要点)
4. [LTS 节奏与升级策略](#4-lts-节奏与升级策略)
5. [适用场景：全栈应用的甜区与边界](#5-适用场景全栈应用的甜区与边界)
6. [核心要点与思考题](#6-核心要点与思考题)

---

## 1. MTV 架构与电池内置哲学

Django 采用 **MTV（Model-Template-View）** 架构——与 MVC 同源，命名不同：Model 管数据、Template 管展示、View 管逻辑（扮演 Controller 角色）。与 Spring Boot 的"全家桶"并列，它是 Python 端"电池内置（batteries included）"哲学的唯一完整实现：

- **一个命令创建完整项目**：`django-admin startproject` 生成 settings/urls/wsgi 骨架，`startapp` 生成 models/views/admin——从零到"能跑"只需十分钟
- **约定强于配置**：项目结构、命名规范、配置项都有官方标准——团队无需开会讨论"目录怎么放"，新成员上手成本全生态最低
- **应用内建边界**：每个业务模块一个 app，Admin/迁移/测试按 app 自动挂载——单体阶段清晰，微服务化时成为拆分的负担（05 章详述）

```bash
# 从零到能跑：两条命令（2026 标准流程）
django-admin startproject mysite && cd mysite
python manage.py startapp orders        # app 自带 models/admin/views/migrations
python manage.py runserver
```

> 💡 **定位**：Django 不是"一个 Web 框架"，而是"一套应用平台"——它替你决定了 ORM 用什么、认证怎么做、后台长什么样。代价是"全栈全能但全栈都绑在 Django 上"，这决定了它的适用边界（第 5 节）。

## 2. 核心组件：ORM / Admin / 迁移 / 认证

四大内置组件构成 Django 的护城河：

- **ORM**：模型即表定义，`python manage.py makemigrations` 自动生成迁移、`migrate` 执行——**模型驱动开发**，改模型 → 生成迁移 → 更新库，全链路无 SQL 手写（复杂查询仍可原生 SQL）
- **Admin**：模型注册即后台——`admin.site.register(Order)` 一行代码得到可搜索、可过滤、可编辑的管理界面；**全生态没有等价的免费方案**，这是 Django 对"内部系统/运营后台"的决定性优势
- **认证系统**：用户/组/权限内置 + `django-allauth` 补齐 OAuth/社交登录，Session 与密码哈希开箱即用
- **后台任务（6.0 新增）**：`django.utils.task` 内置后台任务框架——替代 Celery 的官方轻量方案，适用"任务量中等、不想引第三方"的场景；重量级异步仍选 Celery/消息队列

```python
# 模型 + Admin + 迁移：三行代码获得完整 CRUD 后台
from django.db import models
from django.contrib import admin

class Order(models.Model):
    user_id = models.IntegerField()
    amount = models.DecimalField(max_digits=12, decimal_places=2)

admin.site.register(Order)   # 后台即刻可用
```

除四大组件外，Django 的"隐藏内置"也值得盘点，它们在内部系统场景频繁兑现：**缓存框架**（内存/Redis/数据库多后端，模板片段缓存）、**消息框架**（一次性 flash 提示）、**国际化**（i18n/l10n 完整管线）、**安全防护**（CSRF 内置、SQL 注入免疫于 ORM、XSS 模板转义）、**表单系统**（校验 + 渲染 + 文件上传）。这些"免费能力"在 FastAPI/Flask 生态中对应着五六个第三方包的组合——**"内置安全基线"是 Django 对内部系统项目的隐藏价值**：默认就防住了 CSRF 与模板 XSS，而 FastAPI 的安全中间件组合需要团队自己组装并保证不遗漏。

## 3. Django 6.0 与 6.1：2026 版本要点

**Django 6.0（2025-12-03 发布）**三大亮点：

1. **内置后台任务框架**——官方轻量任务方案（与 Celery 并存，中等任务量首选）
2. **原生 Content Security Policy（CSP）**——`ContentSecurityPolicyMiddleware` 开箱配置，Web 安全基线（对标 OWASP 要求）不用再靠第三方包
3. **模板 partials**——`partialdef`/`partial` 标签支持组件化模板开发，服务端渲染的复用性大幅提升

**Django 6.1（2026-08-05 发布）**：模型字段抓取模式（fetch mode）、`ForeignKey.on_delete` 数据库级删除选项、邮箱配置字典化；支持 Python 3.12-3.14，弃 PostgreSQL < 15、MySQL < 8.4。

版本信号解读：**Django 在 2026 年的更新方向是"全栈能力的现代补齐"**——后台任务、CSP、模板组件化，都是让"一体化的 Django"继续适用于现代应用的关键拼图，而不是追赶 FastAPI 的异步路线（Django 4.1+ 已有异步视图支持，但整体仍是同步为主、异步为辅的定位）。

对学习者还有一个**版本选择建议**：学 Django 直接上 6.x（或等 2026-12 的 6.2 LTS），5.x 只读迁移指南即可——6.x 的 Python 3.12+ 要求与 5.x 的差异不大，但新特性（后台任务/CSP）会出现在面试与招聘要求里，**用最新主线学习、按 LTS 部署**是 Django（也是 Python 生态）的标准姿势。面试被问"你用的哪个版本"时，能说出"6.1 + Python 3.14"比模糊的"Django"更有说服力——版本意识本身就是工程素养的体现。

## 4. LTS 节奏与升级策略

- Django 的 LTS 节奏：每两个版本一个 LTS（**5.2 LTS 支持至 2028-04**，6.x 的下一个 LTS 约在 2026-12）；非 LTS 支持 8 个月
- 企业实践：**生产钉 LTS，功能跟随非 LTS 尝鲜**——与 Java 的 LTS 策略同构（见「Java Python对比」体系 00 章）
- 升级路径：Django 官方提供发布说明与 deprecation 时间线，每个版本至少一年过渡期；配合 `python -W error::DeprecationWarning` 提前暴露废弃 API

## 5. 适用场景：全栈应用的甜区与边界

Django 的甜区（优势最大化的场景）：

| 场景 | 为什么 Django | 反面场景（别用 Django） |
|------|--------------|----------------------|
| 运营后台/管理平台 | Admin 开箱即用，开发成本骤降 | 纯 API 网关（框架太重） |
| 内容站/CMS/电商 | 模板 + ORM + 缓存生态成熟 | 高并发 WebSocket（同步模型吃力） |
| 内部系统/CRM | 认证/权限/Admin 全家桶 | 模型服务（异步流式非强项） |
| 全栈单体应用 | 一套代码端到端 | 微服务拆分（app 边界难拆） |

边界判断：**"要后台、要管理、要内容"→ Django；"只要接口、要并发、要模型服务"→ FastAPI**。2026 年最典型的组合是"Django 打底业务 + FastAPI sidecar 出异步接口"（08 章详述）——这不是妥协，而是两个框架各自甜区的并集。

两个容易踩的边界误区要提前澄清：**误区一"Django 不能做 API"**——错，DRF 与 Django Ninja 都成熟，Django 做 API 完全成立，只是"纯 API 项目用 Django"属于杀鸡用牛刀（框架重量与后台组件用不上）；**误区二"Django 不适合高并发"**——半对，Django 的同步模型单机吞吐低于 ASGI，但"缓存 + 多 worker + 读写分离 + 异步任务卸载"的标准架构下，Django 承载日千万级请求的案例普遍存在——**"高并发"首先靠架构，其次才轮到框架的并发模型**。两个误区都源于把"框架默认形态"当成"框架能力上限"，实际上限由团队架构水平决定。

**性能上还有一笔账要算平**：Django 的"慢"（相对 FastAPI）被低估的另一面是它的**缓存与数据库能力密度**——内置缓存框架（Redis 后端）、ORM 的查询优化（select_related/prefetch_related）、Admin 的惰性加载管理，这些"省下来的数据库往返"对真实吞吐的影响，往往大于框架本身的并发模型差异。一个日千万请求的 Django 站点的性能瓶颈实测通常落在"慢查询与缓存命中率"而不是"WSGI 模型"——**Django 的工程问题大多能用 Django 自己的工具解决**，这是它作为"平台"与"框架"的本质区别。

> 🎯 **核心要点**：Django = 电池内置的全栈平台，Admin/ORM/迁移/认证四大件构成护城河；6.0 的内置后台任务与原生 CSP 补齐现代短板；甜区在"后台 + 内容 + 管理"类应用，纯 API 与模型服务场景让位给 FastAPI。

## 6. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. MTV + 电池内置：一条命令建项目，ORM/Admin/认证/迁移全内置，约定强于配置；
> 2. 6.0（2025-12）内置后台任务 + 原生 CSP + 模板 partials；6.1（2026-08）补抓取模式与数据库级删除；
> 3. 甜区 = 后台/内容/管理类应用；纯 API 与模型服务让位 FastAPI，混合模式用 sidecar。

**思考题**：

1. Django 的 MTV 与 MVC 的对应关系？（→ 1 节）
2. Admin 一行注册获得什么能力？为什么说它无可替代？（→ 2 节）
3. Django 6.0 的内置任务与 Celery 是什么关系？（→ 3 节）
4. 一个"运营后台 + 异步推送"的项目怎么用 Django？（→ 5 节）

---

**下一模块**：[03-FastAPI：异步 API 事实标准](03-FastAPI：异步 API 事实标准.md)｜**返回总览**：[00-Python企业主流开发框架综合对比知识体系总览](00-Python企业主流开发框架综合对比知识体系总览.md)

---

## 参考来源

- [Django 6.0 release notes（官方）](https://django.readthedocs.io/en/stable/releases/6.0.html)——后台任务/CSP/partials
- [Django 6.1 released（2026-08-05）](https://www.djangoproject.com/weblog/2026/aug/05/django-61-released/)——6.1 要点与支持范围
- [Django Releases Version 6.0 with Built-In Background Tasks（InfoQ, 2026-01）](https://www.infoq.com/news/2026/01/django-6-release/)——6.0 解读
- [Django Latest Version & EOL（VersionLog）](https://versionlog.com/django/)——LTS 节奏
