# 04-Flowable 实战：嵌入式工作流引擎落地

> 定位：Flowable 8.0 是 2026 年中国式 OA/审批平台最均衡的开源底座。本篇走通"集成 → 部署 → 流转 → 网关 → 监听"最小闭环，并给出 8 个生产级坑位。以 Spring Boot 4 + Flowable 8 为基线。

## 1. Spring Boot 4 集成

```xml
<!-- 8.0 起基于 Boot 4 / Jackson 3，无需再手配 Bean -->
<dependency>
  <groupId>org.flowable</groupId>
  <artifactId>flowable-spring-boot-starter</artifactId>
  <version>8.0.0</version>
</dependency>
```

- 自动配置自动建库（`flowable.database-schema-update=true`）、自动部署 `classpath*:processes/*.bpmn20.xml`、注册七大 Service Bean。
- 常用配置：`flowable.async-executor-activate=true`（定时器/异步 Job 必需）、`flowable.history-level=audit`（默认，含变量；升级为 full 可追变量变化）、`flowable.idm.enabled=false`（不启用内置 IDM，用户体系对接自家表）。
- 表前缀 `flowable.table-prefix=ACT_` 可自定义，多租户可加 `tenant-id` 按租户隔离定义。
- 多环境配置：开发/测试用内存 H2 + `drop-create` 模式快速迭代，预发/生产用 MySQL 8.4 + 增量升级模式——环境差异（数据库、异步执行器开关、历史等级）用 Spring Profile 隔离，避免"测试没暴露、生产才炸"的配置漂移。

## 2. 部署与启动

```java
// 部署：同名流程新版本自动 +1，ACT_RE_PROCDEF 保留全版本
repositoryService.createDeployment()
    .addClasspathResource("processes/leave.bpmn20.xml")
    .name("请假流程")
    .deploy();

// 启动：按最新版本定义，携带业务变量与业务键
Map<String, Object> vars = Map.of("days", 3, "amount", 5000L);
ProcessInstance pi = runtimeService.startProcessInstanceByKey(
    "leave", "BIZ-20260812-001", vars);
```

**业务键（businessKey）是必须养成的习惯**：流程实例与业务单据的一对一关联靠它，查历史、查活跃实例、幂等防重复发起都依赖 `businessKey`。

## 3. 任务流转与网关

```java
// 查询我的待办 → 完成任务（引擎自动按变量推进到下一节点）
List<Task> tasks = taskService.createTaskQuery()
    .taskAssignee("zhangsan").active().list();

Map<String, Object> vars = new HashMap<>();
vars.put("approved", true);          // 排他网关条件：${approved}
vars.put("amount", 10000L);          // 多级网关条件：${amount > 5000}
taskService.complete(task.getId(), vars);
```

- 网关条件用 **UEL 表达式**，变量名拼错或缺失时表达式求值为 false，流程会静默走 default 流——务必在测试用例里覆盖每条分支。
- **排他网关必须配 default 流**，否则条件全 false 时引擎抛 `ActivitiException`，流程实例停在网关前。

## 4. 监听器：三类挂点

```java
// 全局事件监听：注册 Bean 即可，不侵入流程定义
@Component
public class GlobalEventListener implements ActivitiEventListener {
    @Override
    public void onEvent(ActivitiEvent event) {
        if (event.getType() == ActivitiEventType.TASK_COMPLETED) {
            // 审计落库、指标上报
        }
    }
}
```

- **全局监听器（ActivitiEventListener）**：按事件类型过滤，做审计/埋点最干净，不污染流程定义。
- **执行监听器（ExecutionListener）**：挂在节点上，监听 start/end/take，可写变量、发消息。
- **任务监听器（TaskListener）**：任务 create/assignment/complete 时触发，常用于"任务创建时动态设置处理人"。

> ⚠️ 监听器里禁止调用引擎 API 执行新的流程推进（complete/start）——事务未提交、锁未释放，轻则死锁重则数据不一致。需要联动就发领域事件，让事务外消费（参考同级《软件工程上的设计思想》幂等与事件篇）。

### 4.1 任务操作全家桶（高频 API 速查）

```java
// 认领：待办列表展示后用户点开即认领，防止多人操作同一任务
taskService.claim(taskId, "zhangsan");

// 转办：彻底换人，原处理人不再可见（注意先认领再转办的顺序）
taskService.setAssignee(taskId, "lisi");

// 委派：保留 owner，委派人完成后 resolve 回原人
taskService.delegateTask(taskId, "lisi");
taskService.resolveTask(taskId, vars);

// 挂起/激活：停掉整条流程实例（风险单冻结），或按定义挂起
runtimeService.suspendProcessInstanceById(piId);
runtimeService.activateProcessInstanceById(piId);

// 加评论：审批意见的推荐载体，审计可查
taskService.addComment(taskId, piId, "同意，请财务复核");
```

**任务查询三件套**：按处理人（`taskAssignee`）、按候选人/组（`taskCandidateUser`，配合认领语义）、按业务键（`processInstanceBusinessKey`）——待办列表、已办列表、单据详情三个高频页面各对应一种，查询走索引避免全表扫。

**操作权限与幂等设计**：任务操作（认领/完成/转办）前必须校验"操作人是否为该任务的当前处理人"，否则越权操作是数据事故级缺陷；而"重复提交"问题要靠业务侧幂等——启动流程前查 businessKey 是否已有活跃实例（唯一约束兜底），complete 前校验任务状态，配合数据库唯一键（businessKey + 流程定义 key）双保险。引擎本身不提供业务幂等，这层必须在流程服务层做（呼应《软件工程上的设计思想》的幂等专项）。

### 4.2 与业务系统的集成模式

- **businessKey 关联**：启动时写入业务单号，`runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(key)` 反查流程状态——单据详情页的"流程进度"就靠它，别用变量存单号再查（多一次表关联）。
- **表单数据归属**：审批表单数据落业务表，流程变量只存审批结论与控制字段（`approved`/`nextApprover`）——变量表是审计快照，不是业务数据库。
- **用户体系对接**：`flowable.idm.enabled=false`，处理人存业务用户 ID（字符串），权限判断走业务侧；多租户场景用流程变量携带租户上下文，在全局监听器里做数据隔离。
- **回调通知**：`PROCESS_COMPLETED` 事件监听器里发业务完成通知（钉钉/邮件），流程结束的"最后一公里"——注意同样遵守事务外发送原则。

### 4.3 部署资源与流程文件规范

- **目录约定**：流程文件统一放 `classpath*:processes/`，按业务域分子目录（`processes/leave/`、`processes/purchase/`）；同一流程的 BPMN 与关联 DMN 同目录，部署名与流程 key 命名一致，方便运维识别。
- **版本发布纪律**：流程文件进 Git，走评审合并；线上部署新版本前先在预发环境跑一轮"部署 → 启动 → 审批 → 退回"冒烟；灰度方式是按租户/业务域分批发布（引擎支持按租户隔离定义集）。
- **流程即代码的测试规范**：每个流程至少覆盖"主路径、每条网关分支、每个边界事件、异常路径"四类用例；用 Flowable 测试基类 + 内存 H2 跑单测，CI 门禁里跑集成用例；流程文件变更必须有对应测试变更，否则视为缺陷——这是"改流程不改测试"事故的唯一防线。

### 4.4 完整闭环时序（概念走读）

一次"员工提交请假 → 主管审批 → 完成"的完整链路：

1. 员工提交表单，业务层创建请假单并落库，携带业务单号调用 `startProcessInstanceByKey("leave", bizKey, vars)`——引擎写入实例、执行令牌推进到主管审批节点、创建用户任务。
2. 主管待办列表通过 `taskAssignee` 查询展示，点开认领（`claim`），填写意见与结论，`complete` 携带 `approved` 变量。
3. 引擎在**同一事务**内完成任务、求值网关条件（`${approved}`）、推进令牌到结束事件、写入历史记录——事务提交后流程结束。
4. 全局事件监听器收到 `PROCESS_COMPLETED`，事务外发通知、更新请假单状态、上报指标。

这个闭环解释了三条重要特性：业务写库与流程推进共享一个事务（要么全成要么全滚）、流程状态通过历史表随时可查、业务单号与流程实例通过 businessKey 双向可达。

## 5. 生产级 8 坑

1. **历史表无限膨胀**：高并发审批上线三个月 ACT_HI_ACTINST 可达千万行，未做归档清理会拖垮所有历史查询——上线即配历史清理（见 08 篇）。
2. **Jackson 3 变量兼容**：6/7 升级 8 后，历史表里旧的 JSON 变量读取可能失败，需要兼容层处理序列化差异。
3. **事务边界外操作**：complete 后立刻查任务列表查不到下一步——引擎同事务内还没提交，需要在事务提交后（事务同步/事件）再查询。
4. **多实例会签完成条件**：`nrOfCompletedInstances >= ${passCount}` 这类表达式要写对变量作用域，且并行多实例下并发减签有竞态。
5. **流程定义永不删除**：ACT_RE_PROCDEF 保留全版本，部署太多无用版本会撑大定义表、拖慢启动时的版本选择；用 `suspend` 挂起废弃版本而非反复重部署。
6. **异步执行器未开启**：定时器、异步继续、事件子流程全部依赖 JobExecutor，未激活 = 超时兜底静默失效。
7. **内置 IDM 误用**：默认引擎自带用户体系，与业务用户表脱节后会产生"两个用户世界"；直接 `idm.enabled=false` 对接业务用户，或用自定义 UserManager。
8. **表达式注入风险**：UEL 表达式解析服务任务/监听器里的 Bean 方法，外部可控输入拼进表达式等于 RCE——变量只放数据，方法调用白名单化。

> 🎯 核心要点：Flowable 上手容易、生产踩坑也多。最小闭环之外，先把"历史清理、异步执行器、事务边界、表达式安全"四条线拉直，再谈功能叠加。

---

**下一模块**：[05-中国式审批语义工程化](./05-中国式审批语义工程化.md) / **返回总览**：[00-工作流知识体系总览](./00-工作流知识体系总览.md)

**参考来源**：

- [Flowable 8.0 官方文档](https://flowable.com/open-source/docs/)
- [Flowable 8 升级说明](https://github.com/flowable/flowable-engine/releases/tag/flowable-8.0.0)
- [SpringBoot+Flowable 审批任务协作设计（博客园）](https://www.cnblogs.com/zhouzhongyan2020/articles/22121326)
