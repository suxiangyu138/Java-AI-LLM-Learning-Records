# Stage 4: Spring全家桶 (Spring Ecosystem)

> **"Spring is not just a framework — it is the operating system of the Java enterprise world."**  
> Time Estimate: **6-8 weeks** (the single most important stage of your Java backend career)

---

## Table of Contents

1. [Why Spring is the Absolute Core of Java Enterprise Development](#1-why-spring-is-the-absolute-core-of-java-enterprise-development)
2. [Spring Ecosystem Overview](#2-spring-ecosystem-overview)
3. [Prerequisites](#3-prerequisites)
4. [Learning Roadmap & Time Estimate](#4-learning-roadmap--time-estimate)
5. [Practice Projects](#5-practice-projects)
6. [Career Relevance: What Interviewers Expect](#6-career-relevance-what-interviewers-expect)
7. [Document Map](#7-document-map)
8. [Resource List](#8-resource-list)

---

## 1. Why Spring is the Absolute Core of Java Enterprise Development

### 1.1 Market Reality

In the Java ecosystem, Spring is not optional — it is the de facto standard. Consider these statistics:

| Metric | Value |
|--------|-------|
| **Enterprise Java applications using Spring** | > 70% globally |
| **Java backend job postings requiring Spring** | > 90% |
| **Spring Boot downloads** | > 100 million / month |
| **Fortune 500 companies using Spring** | > 90% |
| **Stack Overflow questions tagged [spring]** | > 1 million |
| **GitHub stars (spring-projects/spring-boot)** | > 75,000 |

### 1.2 What Spring Solves

Before Spring, Java enterprise development (J2EE/EJB 2.x) was notorious for:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    J2EE Pain Points (Pre-2004)                      │
├─────────────────────────────────────────────────────────────────────┤
│  ● Heavyweight EJBs: complex deployment descriptors, remote        │
│    interfaces, home interfaces, component interfaces                │
│  ● Vendor lock-in: WebLogic, WebSphere specific APIs               │
│  ● Boilerplate everywhere: JNDI lookups, connection pooling,       │
│    transaction management written manually again and again          │
│  ● No IoC/DI: developers manually managed object dependencies       │
│  ● POJO-unfriendly: had to implement/extends container interfaces  │
│  ● Testing nightmare: required container for even unit tests       │
└─────────────────────────────────────────────────────────────────────┘
```

Spring transformed this landscape by introducing:

| Spring Innovation | Business Value |
|-------------------|----------------|
| **IoC Container / DI** | Objects are wired declaratively — no more manual dependency management |
| **AOP (Aspect Oriented Programming)** | Cross-cutting concerns (logging, transactions, security) separated from business logic |
| **Declarative Transaction Management** | `@Transactional` replaces boilerplate transaction code |
| **POJO-based Development** | No container interface required — plain Java objects are first-class citizens |
| **Testability** | Lightweight containers enable unit testing without an application server |
| **Portable Abstractions** | DataSource, JTA, JMS — code against Spring abstractions, swap implementations |

### 1.3 The Spring Mindset

To master Spring, you must internalize three core philosophies:

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Spring Core Philosophies                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. Inversion of Control (IoC)                                      │
│     "Don't call us, we'll call you" (Hollywood Principle)           │
│     The container manages object lifecycle & wiring                 │
│                                                                     │
│  2. Convention over Configuration                                   │
│     Sensible defaults reduce decision fatigue                       │
│     Only configure what differs from the default                    │
│                                                                     │
│  3. Opinionated but Pluggable                                       │
│     Spring Boot makes strong default choices                        │
│     But every decision can be overridden when needed                │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.4 The Cost of NOT Learning Spring

If you skip or half-learn Spring, prepare for:

- **90% of job applications rejected** — Spring is the #1 requirement for Java backend roles
- **Cannot contribute to enterprise codebases** — virtually all modern Java backends are Spring/Spring Boot
- **Reinventing the wheel** — you'll hand-roll DI, transaction management, security, REST endpoints
- **Career ceiling** — senior/architect roles demand deep Spring understanding
- **Cloud-native blind spot** — Spring Cloud builds on Spring Boot; you can't learn cloud without it

---

## 2. Spring Ecosystem Overview

### 2.1 The Spring Landscape

```
╔═════════════════════════════════════════════════════════════════════╗
║                    SPRING ECOSYSTEM MAP                            ║
╠═════════════════════════════════════════════════════════════════════╣
║                                                                     ║
║  ┌─────────────────────────────────────────────────────────────┐   ║
║  │                 SPRING BOOT (Platform)                       │   ║
║  │  Auto-configuration · Starters · Actuator · Embedded Server │   ║
║  │  Externalized Config · Production-ready features            │   ║
║  └──────────────────────┬──────────────────────────────────────┘   ║
║                         │                                          ║
║  ┌──────────────────────┼──────────────────────────────────────┐   ║
║  │              SPRING FRAMEWORK (Foundation)                   │   ║
║  │                                                              │   ║
║  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │   ║
║  │  │  IoC / DI    │  │     AOP      │  │     MVC      │      │   ║
║  │  │  Container   │  │  Aspects     │  │  Web/REST    │      │   ║
║  │  │  Bean Life   │  │  Declarative │  │  Dispatcher  │      │   ║
║  │  │  Cycle       │  │  Tx Mgmt     │  │  Servlet     │      │   ║
║  │  └──────────────┘  └──────────────┘  └──────────────┘      │   ║
║  │                                                              │   ║
║  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │   ║
║  │  │  Data Access │  │     Web      │  │  Integration  │      │   ║
║  │  │  JDBC/ORM    │  │  WebSocket   │  │  JMS/AMQP    │      │   ║
║  │  │  Tx          │  │  WebFlux     │  │  Scheduling  │      │   ║
║  │  └──────────────┘  └──────────────┘  └──────────────┘      │   ║
║  └─────────────────────────────────────────────────────────────┘   ║
║                                                                     ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ ║
║  │ Spring Data │  │ Spring      │  │ Spring      │  │ Spring     │ ║
║  │ JPA/Redis/  │  │ Security    │  │ Cloud       │  │ Integration│ ║
║  │ Mongo/...   │  │ OAuth2/SAML │  │ Gateway/    │  │ RabbitMQ/  │ ║
║  │             │  │             │  │ Config/     │  │ Kafka      │ ║
║  │             │  │             │  │ Discovery   │  │            │ ║
║  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘ ║
║                                                                     ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐ ║
║  │ Spring      │  │ Spring for  │  │ Spring Native / GraalVM     │ ║
║  │ Batch       │  │ GraphQL     │  │ AOT compilation             │ ║
║  └─────────────┘  └─────────────┘  └─────────────────────────────┘ ║
║                                                                     ║
╚═════════════════════════════════════════════════════════════════════╝
```

### 2.2 Module Dependency Relationships

```
┌──────────────────────────────────────────────────────┐
│                    Your Application                   │
├──────────────────────────────────────────────────────┤
│                     Spring Boot                       │
├──────────────┬───────────────┬───────────────────────┤
│ Spring Data  │ Spring        │ Spring Cloud / Other   │
│ (JPA, Redis) │ Security      │ (Batch, Integration)   │
├──────────────┴───────┬───────┴───────────────────────┤
│              Spring Framework                         │
│  (Core, MVC, AOP, Data Access, Transaction)          │
├──────────────────────────────────────────────────────┤
│                    JDK / JRE                          │
└──────────────────────────────────────────────────────┘
```

### 2.3 Version History & Compatibility

| Spring Framework | Spring Boot | Java Baseline | Key Changes |
|-----------------|-------------|---------------|-------------|
| 2.x (2006) | - | Java 5 | First annotation support |
| 3.x (2009) | - | Java 6 | JavaConfig (@Configuration, @Bean) |
| 4.x (2013) | 1.x | Java 8 | Conditional annotations, WebSocket |
| 5.x (2017) | 2.x | Java 8+ | Reactive (WebFlux), Kotlin support |
| 6.x (2022) | 3.x | Java 17+ | Jakarta EE migration, AOT, Virtual Threads, ProblemDetail |

---

## 3. Prerequisites

Before diving into the Spring ecosystem, ensure you have solid knowledge of:

### 3.1 Hard Prerequisites

| Area | Specific Knowledge | Verification |
|------|-------------------|-------------|
| **Java SE** | Collections, Generics, Annotations, Reflection, Lambda, Streams, Optional | Can you write a custom annotation and read it via reflection? |
| **HTTP & REST** | HTTP methods, status codes, headers, content negotiation, RESTful design | Can you explain the difference between PUT and PATCH? |
| **SQL & Database** | SELECT/JOIN/aggregation, connection pool concept, transaction ACID | Can you write a 3-table join query? |
| **JSON/XML** | Serialization/deserialization, Jackson annotations | Do you know @JsonProperty vs @JsonIgnore? |
| **Build Tool** | Maven or Gradle basics: dependencies, plugins, lifecycle | Can you create a multi-module Maven project? |
| **Git** | Branching, merging, rebasing, PR workflow | Basic proficiency expected |

### 3.2 Soft Prerequisites (Strongly Recommended)

| Area | Reason |
|------|--------|
| **Design Patterns** | Spring heavily uses Factory, Proxy, Template Method, Strategy, Observer patterns |
| **Proxy (JDK Dynamic / CGLIB)** | Spring AOP relies on proxies — understanding them is non-negotiable |
| **JUnit 5** | Testing is integral to Spring development |

### 3.3 Development Environment Setup

```bash
# Minimum requirements
Java:     OpenJDK 17 LTS or later (Spring Boot 3.x) / Java 11 (Spring Boot 2.x)
Build:    Maven 3.8+ or Gradle 7.5+
IDE:      IntelliJ IDEA Ultimate (recommended) / Eclipse STS
Database: MySQL 8.0+ or PostgreSQL 14+ (for practice projects)
Docker:   Docker Desktop (for local development, Testcontainers)

# Recommended SDK Manager for JDK versioning
# https://sdkman.io/ (Linux/Mac) or https://github.com/frekele/leiningen (Windows)
```

---

## 4. Learning Roadmap & Time Estimate

### 4.1 8-Week Intensive Plan

This is the most important stage of your Java backend education. Allocate **4-6 hours daily** for deep learning.

```
Week 1-2:   Spring Core (IoC, DI, Bean Lifecycle, AOP, Events)
Week 3:     Spring MVC (REST APIs, Validation, Exception Handling, Interceptors)
Week 4-5:   Spring Boot (Auto-configuration, Actuator, Testing, Configuration)
Week 6-7:   Data Access (MyBatis, JPA, Spring Data, Transaction Management)
Week 8:     Build Tools (Maven/Gradle), Review, and Consolidation
```

### 4.2 Daily Learning Pattern

```
┌─────────┬──────────────────────────────────────────────────┐
│ Time     │ Activity                                         │
├─────────┼──────────────────────────────────────────────────┤
│ 60 min   │ Read theory + architecture concepts              │
│ 90 min   │ Follow-along coding exercises                    │
│ 60 min   │ Work on practice project (apply what you learned)│
│ 30 min   │ Review, summarize, answer interview questions    │
└─────────┴──────────────────────────────────────────────────┘
```

### 4.3 Weekly Milestones

| Week | Milestone | Deliverable |
|------|-----------|-------------|
| 1 | Understand IoC/DI deeply, configure beans via XML + annotations + JavaConfig | Can explain BeanFactory vs ApplicationContext |
| 2 | Bean lifecycle, AOP, events, property management, @Async | Can implement custom annotation with AOP |
| 3 | Build REST APIs with validation, global exception handling, interceptors | A complete REST controller with all validation patterns |
| 4 | Spring Boot auto-configuration, custom starters, Actuator | Can write a custom Spring Boot starter |
| 5 | Testing: slices, integration, Testcontainers | Test coverage > 80% for REST API |
| 6 | MyBatis: dynamic SQL, caching, MyBatis-Plus | Complex query with dynamic SQL |
| 7 | JPA: entities, relationships, Spring Data JPA, performance tuning | Solve N+1 in all relationship queries |
| 8 | Maven/Gradle mastery, multi-module projects | Deliver complete practice project |

---

## 5. Practice Projects

### 5.1 Project 1: Blog System (Recommended for Beginners)

A full-featured blog platform demonstrating all Spring concepts.

**Core Features:**

```
┌─────────────────────────────────────────────────────────────────────┐
│                    BLOG SYSTEM - FEATURE MAP                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  User Module                                                        │
│  ├── Registration / Login (Spring Security)                         │
│  ├── Profile management (CRUD)                                     │
│  └── Role-based access: Admin, Author, Reader (Spring Security)    │
│                                                                     │
│  Article Module                                                     │
│  ├── CRUD with rich text (REST API)                                │
│  ├── Pagination, sorting, filtering (Spring Data + Specification)  │
│  ├── Category and tag management                                   │
│  └── Full-text search (Elasticsearch or MySQL fulltext)            │
│                                                                     │
│  Comment Module                                                     │
│  ├── Nested comments (self-referencing entity)                     │
│  ├── Auditable: createdBy, createdAt, updatedAt (JPA Auditing)     │
│  └── Moderation workflow                                           │
│                                                                     │
│  Statistics & Analytics                                             │
│  ├── View counting (Redis)                                         │
│  ├── Popular articles ranking                                      │
│  └── Admin dashboard (Actuator + custom metrics)                   │
│                                                                     │
│  Infrastructure                                                     │
│  ├── Global exception handling (RestControllerAdvice)              │
│  ├── Request/response logging (Interceptor)                        │
│  ├── API rate limiting (Interceptor + Redis)                       │
│  └── Caching (Spring Cache + Redis)                                │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**Technology Stack:**

```properties
# Build
build=gradle 8.x (Kotlin DSL)

# Core
spring-boot=3.2.x
java=17

# Data
database=MySQL 8.0 (JPA + MyBatis combined)
cache=Redis 7.x (Spring Cache)
search=Elasticsearch 8.x (optional)

# Infrastructure
spring-docs=springdoc-openapi (OpenAPI 3.0)
test=JUnit 5 + Testcontainers + REST Assured
deploy=Docker Compose
```

### 5.2 Project 2: Online Learning Platform (Recommended for Advanced)

A more complex microservices-oriented project.

**Core Modules:**
- **Course Service**: course CRUD, curriculum management, pricing
- **User Service**: registration, authentication, enrollment, progress tracking
- **Payment Service**: order, payment gateway integration, refund
- **Notification Service**: email, SMS, in-app notification (event-driven)
- **Review & Rating Service**: course reviews, Q&A

**Technologies:**
- Spring Boot 3.2, Spring Cloud Gateway, Eureka/Nacos
- Spring Security + JWT + OAuth2
- Kafka/RabbitMQ for event-driven communication
- Redis for caching and session management
- Distributed transactions (Seata / Saga pattern)

### 5.3 Minimum Viable Deliverable

Even the simplest project should demonstrate:

```java
// 1. Dependency Injection with multiple strategies
@Component
@RequiredArgsConstructor  // Constructor injection
public class ArticleService {
    private final ArticleRepository repository;
    private final CacheManager cacheManager;
}

// 2. REST API with full validation
@RestController
@RequestMapping("/api/v1/articles")
@Validated
public class ArticleController {
    @PostMapping
    public ResponseEntity<Result<ArticleVO>> create(
            @Valid @RequestBody ArticleCreateRequest request) { ... }
}

// 3. Database interaction (JPA or MyBatis)
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);
    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword%")
    List<Article> searchByTitle(@Param("keyword") String keyword);
}

// 4. Global exception handling
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) { ... }
}

// 5. Caching
@Cacheable(value = "articles", key = "#id")
public Article findById(Long id) { ... }

// 6. Unit and integration testing
@SpringBootTest
@AutoConfigureMockMvc
class ArticleControllerTest {
    @Test
    void shouldCreateArticle_whenRequestValid() { ... }
}
```

---

## 6. Career Relevance: What Interviewers Expect

### 6.1 By Experience Level

| Level | Spring Expectation | Interview Focus |
|-------|-------------------|-----------------|
| **Junior (0-2 yrs)** | Can build CRUD REST APIs with Spring Boot | @Autowired, @RestController, basic JPA |
| **Mid-level (2-5 yrs)** | Deep lifecycle knowledge, transaction management, AOP | @Transactional isolation, Bean scopes, proxy internals |
| **Senior (5-8 yrs)** | Can design system architecture, performance tuning, customize auto-config | Spring Cloud, caching strategies, async messaging |
| **Staff/Architect (8+ yrs)** | Framework design decisions, build vs buy, migration strategy | Evolution from Spring 2.x to 6.x, Jakarta migration, GraalVM |

### 6.2 Top 20 Spring Interview Questions (Across All Levels)

**Fundamentals:**
1. What is IoC and how does Spring implement it?
2. Describe the Spring bean lifecycle in detail.
3. How does @Autowired work? What's the difference between @Inject, @Resource, and @Autowired?
4. How does Spring resolve circular dependencies? (Three-level cache)
5. What's the difference between singleton and prototype scope? What's the prototype-in-singleton pitfall?

**AOP:**
6. How does Spring AOP work? What's the difference between JDK dynamic proxy and CGLIB?
7. What's the difference between @Transactional on a public vs private method?
8. Explain AOP advice types: @Before, @After, @Around, @AfterReturning, @AfterThrowing.

**Spring MVC:**
9. Describe the request processing flow from DispatcherServlet to response.
10. How does @RequestBody work? How does HttpMessageConverter fit in?
11. How do you implement global exception handling? Compare HandlerExceptionResolver, @ExceptionHandler, @ControllerAdvice.

**Spring Boot:**
12. How does @SpringBootApplication work? Explain @EnableAutoConfiguration.
13. How does Spring Boot's auto-configuration mechanism work? What are @Conditional annotations?
14. What's the order of Spring Boot's externalized configuration (priority levels)?
15. How do you create a custom Spring Boot starter?

**Data Access:**
16. Explain MyBatis $ vs # — when would you use each?
17. What's the N+1 problem in JPA? How do you solve it?
18. Explain transaction propagation in Spring. What's REQUIRES_NEW vs NESTED?

**Build:**
19. What's the difference between Maven's compile, provided, and runtime scope?
20. Maven vs Gradle: when would you choose each?

### 6.3 Red Flags in Interviews

```
┌─────────────────────────────────────────────────────────────────────┐
│                    INTERVIEW RED FLAGS                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ❌ "Spring Boot and Spring Framework are the same thing"           │
│     → Shows fundamental misunderstanding                            │
│                                                                     │
│  ❌ "I always use field injection with @Autowired"                  │
│     → Shows lack of enterprise experience (field injection is       │
│       widely considered harmful for testability)                    │
│                                                                     │
│  ❌ "I never write unit tests for controllers"                      │
│     → Shows lack of testing discipline                              │
│                                                                     │
│  ❌ "XML config is outdated, I only use annotations"                 │
│     → Shows inexperience with legacy enterprise codebases           │
│                                                                     │
│  ❌ "I don't need to understand AOP, Spring handles it"             │
│     → Shows superficial knowledge                                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.4 Salary Impact

In the Chinese job market (reference):

| Skill | Salary Premium |
|-------|---------------|
| Spring Boot CRUD | Base (~¥150-250K/yr junior) |
| + Deep Spring Core + AOP | +20-30% |
| + Spring Cloud + Microservices | +40-60% |
| + System Architecture + Performance | +80-150% |
| Full mastery (architect) | ¥500K-1M+/yr |

---

## 7. Document Map

This stage contains the following documents — read them in order:

```
┌─────────────────────────────────────────────────────────────────────┐
│                   STAGE 4 DOCUMENT MAP                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  01-Spring-Core-IoC与DI.md                                          │
│  ├── IoC/DI concepts                                               │
│  ├── Bean lifecycle (deep: post processors, aware)                 │
│  ├── AOP fundamentals                                              │
│  ├── Event mechanism                                               │
│  └── Property management, SpEL                                     │
│                                                                     │
│  02-Spring-MVC与RESTful.md                                          │
│  ├── DispatcherServlet flow                                        │
│  ├── REST API development                                          │
│  ├── Validation (JSR-380)                                          │
│  ├── Global exception handling                                     │
│  ├── Interceptors, CORS, file upload                               │
│  └── Testing with MockMvc                                          │
│                                                                     │
│  03-Spring-Boot核心.md                                              │
│  ├── Auto-configuration deep dive                                  │
│  ├── Custom starters                                               │
│  ├── Actuator, Metrics, Prometheus                                 │
│  ├── Configuration (17 levels)                                     │
│  ├── Logging, DevTools                                             │
│  ├── Testing slices                                                │
│  └── 2.x → 3.x migration                                           │
│                                                                     │
│  04-MyBatis与JPA.md                                                 │
│  ├── MyBatis: architecture, dynamic SQL, caching, plugins          │
│  ├── MyBatis-Plus: auto CRUD, pagination, logic delete             │
│  ├── JPA: entity mapping, relationships, JPQL, Criteria            │
│  ├── Spring Data JPA: repositories, query methods, auditing        │
│  ├── Performance: N+1, batch, projection                           │
│  └── MyBatis vs JPA comparison                                     │
│                                                                     │
│  05-Maven与Gradle构建.md                                            │
│  ├── Maven: POM, lifecycle, dependency management, multi-module    │
│  ├── Gradle: DSL, task model, plugins, performance                 │
│  ├── BOM, dependency convergence                                   │
│  └── Enterprise build pipeline                                     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 8. Resource List

### 8.1 Official Documentation

| Resource | URL | Priority |
|----------|-----|----------|
| Spring Framework Reference | https://docs.spring.io/spring-framework/reference/ | Must read |
| Spring Boot Reference | https://docs.spring.io/spring-boot/docs/current/reference/ | Must read |
| Spring Guides (Guides) | https://spring.io/guides | Follow along |
| Spring Initializr | https://start.spring.io/ | Always use |
| Baeldung Spring Tutorials | https://www.baeldung.com/spring-tutorial | Supplementary |

### 8.2 Books

| Title | Author | Stage |
|-------|--------|-------|
| "Spring in Action, 6th Edition" | Craig Walls | Beginner-Intermediate |
| "Spring Start Here" | Laurentiu Spilca | Beginner |
| "Pro Spring 6" | Iuliana Cosmina et al. | Intermediate-Advanced |
| "Spring Boot in Practice" | Somnath Musib | Intermediate |
| "High-Performance Java Persistence" | Vlad Mihalcea | Advanced (JPA) |

### 8.3 Online Courses

| Platform | Course | Price |
|----------|--------|-------|
| Udemy | "Spring Boot 3, Spring 6 & Hibernate" by Chad Darby | Paid (frequent sales) |
| Baeldung | "Learn Spring" beginner course | Paid |
| YouTube | "Spring Boot Tutorial" by Amigoscode | Free |
| YouTube | "Spring Framework" by Java Brains | Free (classic) |

### 8.4 Practice Platforms

| Platform | Purpose |
|----------|---------|
| LeetCode | Not applicable (Spring ≠ algorithms) |
| CodeWars | Spring katas available |
| GitHub | Clone real Spring projects, read source code |
| Stack Overflow | Search before asking, contribute answers |

### 8.5 Must-Follow Engineers (Chinese)

| Name | Platform |
|------|----------|
| 江南一点雨 | Bilibili, WeChat (Spring series) |
| 程序员DD | WeChat (Spring Cloud) |
| 纯洁的微笑 | WeChat (Spring Boot) |
| 周志明 (fenixsoft) | GitHub (JVM, Spring understanding) |

---

## 9. Final Words

> **Spring is the most important investment you will make in your Java career.**

It is a vast ecosystem — you will not master everything in 8 weeks. Focus on the core (IoC, DI, AOP, MVC, Boot) and build projects. Depth over breadth. Return to advanced topics (Cloud, Security, Reactive) when you encounter them in real work.

The documents that follow in this stage are designed to be reference materials you will return to throughout your career. Each contains:
- Practical code examples you can run immediately
- Architecture diagrams for visualization
- Interview questions categorized by difficulty
- Enterprise patterns and anti-patterns
- Version-specific notes (JDK 17+, Spring Boot 3.x)

**Start with 01-Spring-Core-IoC与DI.md and write code as you read.**

---

> "Spring Framework is not just a framework — it's a programming model that completely changes how you think about enterprise Java development."
> — Rod Johnson, Creator of Spring Framework
