# MyBatis 实战项目清单

## 一、核心能力

MyBatis 是 Java 持久层框架，核心应用场景：

- SQL 与业务解耦
- 灵活动态 SQL
- 复杂多表关联查询
- 结果映射与缓存优化

---

## 二、技术能力拆解

- 配置层：全局配置 / Mapper 映射文件
- 查询层：参数绑定（#{}、${}）/ 结果映射
- 动态 SQL：if / where / choose / foreach
- 关联查询：association / collection / 嵌套查询
- 优化层：分页 / 缓存 / 批处理
- 工程层：MyBatis-Plus / 代码生成 / 乐观锁

---

## 三、必做项目（求职优先级）

### 1. 单表用户 CRUD 系统
**技术栈**：MyBatis + MySQL + Maven

**实现**：
- 全局配置文件 + Mapper 映射文件
- Mapper 接口绑定
- #{} 预编译 vs ${} 字符串拼接
- 用户注册 / 登录 / 查询 / 修改 / 删除

**产出**：
- MyBatis 基础操作能力

**耗时**：2~3天

---

### 2. 动态 SQL 条件查询系统
**技术栈**：MyBatis + MySQL

**实现**：
- `<if>` / `<where>` / `<choose>` / `<foreach>`
- 多条件组合筛选
- 批量删除 / 批量插入

**核心语法**：
```xml
<where>
  <if test="name != null">
    AND name LIKE CONCAT('%', #{name}, '%')
  </if>
  <if test="status != null">
    AND status = #{status}
  </if>
</where>

<foreach collection="ids" item="id" open="(" separator="," close=")">
  #{id}
</foreach>
```

**产出**：
- 动态 SQL 构建能力

**耗时**：2~3天

---

### 3. 多表关联权限管理系统
**技术栈**：MyBatis + MySQL

**实现**：
- 一对一：用户 - 详情
- 一对多：用户 - 角色
- 多对多：角色 - 权限
- association / collection / 嵌套查询

**核心映射**：
```xml
<resultMap id="UserRoleMap" type="User">
  <id property="id" column="user_id"/>
  <result property="username" column="username"/>
  <collection property="roles" ofType="Role">
    <id property="id" column="role_id"/>
    <result property="roleName" column="role_name"/>
  </collection>
</resultMap>
```

**产出**：
- 多表关联查询能力

**耗时**：3~4天

---

### 4. 分页 + 模糊查询项目
**技术栈**：MyBatis + PageHelper + MySQL

**实现**：
- PageHelper 插件配置
- 分页参数封装（PageInfo）
- 模糊查询 + 排序 + 总条数统计

**产出**：
- 分页查询能力

**耗时**：2~3天

---

### 5. MyBatis-Plus 快速 CRUD 后台
**技术栈**：MyBatis-Plus + SpringBoot + MySQL

**实现**：
- BaseMapper / IService 继承
- Lambda 条件构造器
- 主键策略（@TableId）
- 自动填充（@TableField(fill = FieldFill.INSERT)）
- 逻辑删除（@TableLogic）

**核心代码**：
```java
// Lambda 查询
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.like(User::getName, "张")
       .eq(User::getStatus, 1)
       .orderByDesc(User::getCreateTime);
List<User> users = userMapper.selectList(wrapper);
```

**产出**：
- MyBatis-Plus 现代开发能力

**耗时**：2~3天

---

### 6. MyBatis-Plus 复杂查询 + 联表
**技术栈**：MyBatis-Plus + SpringBoot + MySQL

**实现**：
- QueryWrapper / UpdateWrapper
- 自定义 XML 联表查询
- MP 混合原生 SQL
- 分组统计 / 复杂筛选

**产出**：
- MP 与原生 SQL 混合使用能力

**耗时**：3~4天

---

## 四、进阶项目（提升上限）

### 7. 订单系统（事务 + 关联查询）
**技术栈**：MyBatis + Spring + MySQL

**实现**：
- 订单 - 订单项一对多查询
- 事务控制（@Transactional）
- 订单状态流转
- 避免 N+1 查询

**产出**：
- 事务控制 + 性能优化能力

**耗时**：3~4天

---

### 8. 商品库存系统（乐观锁 + 缓存）
**技术栈**：MyBatis + SpringBoot + Redis

**实现**：
- 库存扣减
- 乐观锁（版本号控制）
- 防超卖
- Redis 缓存库存

**核心 SQL**：
```xml
<update id="deductStock">
  UPDATE product 
  SET stock = stock - #{quantity}, version = version + 1
  WHERE id = #{id} AND stock >= #{quantity} AND version = #{version}
</update>
```

**产出**：
- 并发控制 + 缓存整合能力

**耗时**：4~5天

---

### 9. MyBatis 缓存机制实战
**技术栈**：MyBatis + SpringBoot

**实现**：
- 一级缓存验证（同 SqlSession）
- 二级缓存配置（跨 SqlSession）
- 缓存失效场景测试
- 性能对比分析

**配置**：
```xml
<!-- 开启二级缓存 -->
<cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>
```

**产出**：
- 缓存机制理解 + 性能调优能力

**耗时**：2~3天

---

## 五、必练核心知识点

### 1. #{} vs ${}
- **#{}**：预编译，防 SQL 注入，推荐使用
- **${}**：字符串拼接，动态表名/字段名场景

---

### 2. 动态 SQL 标签
```xml
<if test="condition">...</if>
<where>...</where>
<choose><when><otherwise>
<foreach collection="list" item="item" separator=",">
<trim prefix="WHERE" prefixOverrides="AND |OR">
<set>...</set>
```

---

### 3. 结果映射
```xml
<resultMap id="BaseResultMap" type="User">
  <id property="id" column="user_id"/>
  <result property="name" column="user_name"/>
  <association property="profile" javaType="Profile">
    <id property="id" column="profile_id"/>
  </association>
  <collection property="roles" ofType="Role">
    <id property="id" column="role_id"/>
  </collection>
</resultMap>
```

---

### 4. MyBatis-Plus 常用注解
```java
@TableName("sys_user")       // 表名映射
@TableId(type = IdType.AUTO) // 主键策略
@TableField(fill = FieldFill.INSERT) // 自动填充
@TableLogic                  // 逻辑删除
@Version                     // 乐观锁
```

---

## 六、项目架构标准

必须覆盖：

- 规范分层：Controller / Service / Mapper / XML
- 动态 SQL 构建
- 多表关联查询优化
- 分页插件使用
- 事务控制
- 缓存策略
- 异常处理与日志

---

## 七、简历表达（核心关键词）

- MyBatis 持久层框架
- 动态 SQL 构建
- 多表关联查询优化
- MyBatis-Plus Lambda 条件构造器
- 乐观锁防并发
- 二级缓存性能优化
- PageHelper 分页插件

---

## 八、技术栈推荐

- 框架：MyBatis / MyBatis-Plus
- 数据库：MySQL
- 分页：PageHelper
- 代码生成：MyBatis-Plus Generator
- 缓存：Redis
- 连接池：Druid / HikariCP

---

## 九、MyBatis vs JPA 选型

| 特性 | MyBatis | JPA/Hibernate |
|------|---------|---------------|
| SQL 控制 | 灵活（手写 SQL） | 自动生成 |
| 学习曲线 | 平缓 | 陡峭 |
| 复杂查询 | 优秀 | 复杂场景需原生 SQL |
| 性能调优 | 可控性强 | 依赖框架优化 |
| 适用场景 | 复杂业务 / 遗留系统 | 标准 CRUD / 快速开发 |

---

## 十、项目成果要求

- GitHub 完整代码（结构清晰）
- README（技术栈 + 功能清单 + 核心难点）
- 规范分层架构
- XML 映射文件格式化（SQL 缩进）
- 至少 1 个完整业务 Demo
- 包含性能优化说明（索引 / 避免 N+1 查询）

---
