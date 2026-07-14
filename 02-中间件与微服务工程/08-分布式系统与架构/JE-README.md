# Stage 5: 分布式与微服务 (Distributed Systems & Microservices)

> **Target Audience**: Senior Java Engineers / Architects
> **Estimated Time**: 6-8 weeks (full-time focused study)
> **Prerequisites**: Stage 1-4 (Java SE, Java EE/SSM, Spring Boot, Data Access & Caching)

---

## Table of Contents

1. [Why Microservices?](#why-microservices)
2. [Evolution: Monolith to Microservices](#evolution-monolith-to-microservices)
3. [Core Challenges of Distributed Systems](#core-challenges-of-distributed-systems)
4. [Stage Roadmap](#stage-roadmap)
5. [Technology Stack](#technology-stack)
6. [Practice Project: 微服务版在线商城](#practice-project-微服务版在线商城)
7. [Learning Resources](#learning-resources)
8. [Assessment Criteria](#assessment-criteria)
9. [Production Readiness Checklist](#production-readiness-checklist)

---

## Why Microservices?

In the modern Java backend landscape, monolithic architectures struggle to meet the demands of rapid iteration, independent scaling, and team autonomy. Microservices address these challenges by decomposing a system into **small, autonomous services** that can be developed, deployed, and scaled independently.

### Business Drivers

| Driver | Monolithic Pain | Microservice Solution |
|---|---|---|
| **Time-to-market** | One bug blocks entire release | Independent deployment per service |
| **Team scale** | Merge conflicts, coordination overhead | Teams own services end-to-end |
| **Scalability** | Scale entire app, waste resources | Scale only bottleneck services |
| **Technology diversity** | Locked into one stack | Polyglot per service (e.g., Java + Python ML) |
| **Resilience** | One OOM kills everything | Bulkhead isolation, circuit breakers |
| **Compliance** | Hard to audit data boundaries | Service boundaries map to compliance domains |

### When NOT to Use Microservices

- Team size < 10 engineers
- Product is in discovery/validation phase (MVP)
- Domain is simple with clear boundaries already handled by modular monolith
- No DevOps maturity (CI/CD, containerization, monitoring)
- Latency is the absolute top priority (in-process call vs network call)

> **Rule of Thumb**: Start with a modular monolith, extract services when you feel the pain.

---

## Evolution: Monolith to Microservices

```
                    +---------------------+
                    |      Monolith       |
                    |  [UI][Biz][Data]    |
                    +----------+----------+
                               |
                    +----------+----------+
                    |   SOA (Service-     |
                    |   Oriented Arch)    |
                    |  ESB heavy, shared  |
                    |  schemas, SOAP/WS-* |
                    +----------+----------+
                               |
                    +----------+----------+
                    |   Microservices     |
                    |  Smart endpoints,   |
                    |  dumb pipes,        |
                    |  decentralized      |
                    +---------------------+
                    |   Service Mesh      |
                    |  (Istio/Linkerd)    |
                    |  Sidecar proxy      |
                    +---------------------+
```

### Monolithic Architecture

**Advantages:**
- Simple development and debugging (single process, single IDE project)
- Low latency (in-process calls, no network overhead)
- Transactional consistency (ACID via single database)
- Single deployment unit, simple CI/CD pipeline
- Easy end-to-end testing

**Pain Points:**
- **Scaling**: Must scale the entire application, even if only one module is bottlenecked
- **Coupling**: Tight coupling between modules; a change in one place can break others
- **Deploy friction**: Full regression test required for every deployment; deployment frequency decreases
- **Technology lock-in**: Hard to adopt new frameworks or languages incrementally
- **Onboarding overhead**: Large codebase overwhelms new developers

### SOA (Service-Oriented Architecture)

- Introduced service abstraction but relied heavily on **ESB (Enterprise Service Bus)**
- Shared schemas (often XML/XSD) created coupling at the integration layer
- Heavy protocols (SOAP, WS-*) added complexity
- SOA failed to deliver on its promises largely due to **centralized governance** and **heavy middleware**

### Microservices

Key characteristics as defined by Martin Fowler and James Lewis:

1. **Componentization via Services** — Services are independently deployable units
2. **Organized around Business Capabilities** — Cross-functional teams own services
3. **Products not Projects** — Team owns the service throughout its lifecycle
4. **Smart Endpoints and Dumb Pipes** — Business logic in services, messaging is thin
5. **Decentralized Governance** — Teams choose their own tech stack (within reason)
6. **Decentralized Data Management** — Each service owns its data store (Database per Service)
7. **Infrastructure Automation** — DevOps, CI/CD, containers
8. **Design for Failure** — Resilience patterns built in
9. **Evolutionary Design** — Services can be replaced, merged, or split

### Service Mesh (The Next Step)

```
                    +----------------------------+
                    |     Kubernetes Cluster     |
                    +----------------------------+
                    |  +------+    +------+      |
                    |  | App  |    | App  |      |
                    |  |Proxy |    |Proxy |      |
                    |  +------+    +------+      |
                    |       |           |        |
                    |  +----+-----------+----+   |
                    |  |    Control Plane    |   |
                    |  |  Pilot, Mixer,     |   |
                    |  |  Citadel (Istio)    |   |
                    |  +---------------------+   |
                    +----------------------------+
```

Service Mesh moves service-to-service communication concerns (discovery, retry, circuit breaking, observability) into a **sidecar proxy** layer, decoupling them from application code. Istio, Linkerd, Consul Connect are leading implementations.

---

## Core Challenges of Distributed Systems

### 1. Service Discovery

How does Service A find the network address of Service B?

| Pattern | Description | Tools |
|---|---|---|
| **Client-side** | Client queries registry, load-balances across instances | Netflix Eureka, Nacos, Consul |
| **Server-side** | Load balancer (LB) accepts request, forwards to instance | AWS ALB, Kubernetes Service |

### 2. Distributed Configuration

- Managing configuration across dozens of services and environments
- Real-time config refresh without restart
- Secrets management (encryption, rotation)

**Tools**: Nacos Config, Spring Cloud Config, Consul KV, Apollo (Ctrip), Kubernetes ConfigMap/Secret

### 3. Fault Tolerance

In a distributed system, failures are inevitable. Key patterns:

| Pattern | Purpose |
|---|---|
| Circuit Breaker | Stop cascading failures when a downstream service fails |
| Bulkhead | Isolate resources to prevent one failure from taking down everything |
| Retry with backoff | Transient failure recovery |
| Timeout | Bound the time a service waits for a response |
| Fallback | Graceful degradation when a service is unavailable |

### 4. Observability

Traditional "logging into servers" doesn't work at scale.

| Pillar | What | Tools |
|---|---|---|
| **Logging** | Structured, correlated events | ELK, Loki, Grafana |
| **Metrics** | Aggregated numerical data | Prometheus, Micrometer, Grafana |
| **Tracing** | End-to-end request flow | Jaeger, Zipkin, Micrometer Tracing |

**Three Golden Signals** (Google SRE):
- **Latency** — Time to service a request
- **Traffic** — Demand on the system
- **Errors** — Rate of failed requests
- **Saturation** — How "full" the service is

### 5. Data Consistency

ACID transactions don't span services. Alternatives:

| Pattern | Description |
|---|---|
| **Saga** | Sequence of local transactions with compensating actions on failure |
| **Eventual Consistency** | Accept temporary inconsistency, converge over time |
| **TCC (Try-Confirm/Cancel)** | Reserve resources upfront, confirm or cancel |
| **Outbox Pattern** | Write event to local outbox table, reliably publish to MQ |

---

## Stage Roadmap

```
Week 1-2:  ┌──────────────────────────────────────┐
            │  01-微服务架构设计                     │
            │  Microservice Architecture           │
            │  Design patterns, DDD, CAP, Saga     │
            └──────────────────────────────────────┘

Week 3-4:  ┌──────────────────────────────────────┐
            │  02-Spring-Cloud组件                  │
            │  Nacos, Gateway, Resilience4j,       │
            │  Sentinel, Seata, OpenFeign          │
            └──────────────────────────────────────┘

Week 5:    ┌──────────────────────────────────────┐
            │  03-Redis实战                         │
            │  Distributed lock, cache patterns,   │
            │  cluster, performance optimization   │
            └──────────────────────────────────────┘

Week 6:    ┌──────────────────────────────────────┐
            │  04-消息队列                           │
            │  Kafka + RabbitMQ deep dive,         │
            │  reliability, event-driven           │
            └──────────────────────────────────────┘

Week 7:    ┌──────────────────────────────────────┐
            │  05-Docker容器化                      │
            │  Docker, Compose, K8s basics,        │
            │  CI/CD pipeline                      │
            └──────────────────────────────────────┘

Week 8:    ┌──────────────────────────────────────┐
            │  Project: 微服务版在线商城             │
            │  Integrate everything learned       │
            └──────────────────────────────────────┘
```

---

## Technology Stack

### Core Framework

```
Spring Boot 3.x          → Application framework
Spring Cloud 2023.x      → Distributed system patterns
Spring Cloud Alibaba     → Nacos, Sentinel, Seata
```

### Service Infrastructure

```
Nacos                    → Registry + Config Center
Spring Cloud Gateway     → API Gateway (WebFlux-based)
Resilience4j             → Circuit Breaker + Rate Limiter + Retry
Sentinel                 → Flow control + circuit breaking
Micrometer Tracing       → Distributed tracing (with Zipkin/Jaeger)
```

### Data & Middleware

```
Redis 7.x                → Cache + Distributed Lock + Session
Kafka 3.x / RabbitMQ 3.x → Message Queue
MySQL 8.x + ShardingSphere → Data persistence + sharding
Seata                    → Distributed transactions
```

### DevOps

```
Docker 24.x              → Container runtime
Docker Compose           → Local orchestration
Kubernetes 1.28+         → Production orchestration (introduction)
GitLab CI / Jenkins      → CI/CD pipeline
Prometheus + Grafana     → Metrics & monitoring
ELK / Loki               → Logging
```

### Dependency Versions (as of 2025-2026)

```xml
<!-- Spring Boot 3.2.x -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<!-- Spring Cloud 2023.0.x -->
<properties>
    <spring-cloud.version>2023.0.1</spring-cloud.version>
    <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
</properties>
```

---

## Practice Project: 微服务版在线商城

A full-stack microservices online shopping system that exercises every concept in this stage.

### Architecture Overview

```
                         +------------------+
                         |   Spring Cloud   |
                         |     Gateway      |
                         |   (Port 8080)    |
                         +-------+----------+
                                 |
          +----------------------+-----------------------+
          |                      |                       |
+---------v--------+   +--------v--------+   +----------v--------+
|   User Service   |   |  Order Service   |   |  Product Service  |
|  (Port 8081)     |   |  (Port 8082)     |   |  (Port 8083)      |
|  User auth, JWT   |   |  Order mgmt      |   |  Product catalog  |
|  Address mgmt     |   |  Saga orchest.   |   |  Inventory mgmt   |
+--------+---------+   +--------+---------+   +---------+---------+
         |                      |                        |
+--------v---------+   +--------v---------+   +---------v---------+
|  MySQL: users    |   |  MySQL: orders   |   |  MySQL: products  |
|  Redis: session  |   |  Redis: lock     |   |  Redis: cache     |
+------------------+   +------------------+   +-------------------+
                                |
                    +-----------v-----------+
                    |    Payment Service     |
                    |   (Port 8084)          |
                    |   Mock payment APIs    |
                    |   Seata TCC tx         |
                    +-----------+------------+
                                |
                    +-----------v-----------+
                    |         Nacos          |
                    |   Registry + Config    |
                    |   (Port 8848)          |
                    +-----------------------+


                         +------------------+
                         |    Kafka/RabbitMQ|
                         |  Order events    |
                         |  Inventory events|
                         |  Payment events  |
                         +------------------+
```

### Service Breakdown

| Service | Responsibilities | Tech Highlights |
|---|---|---|
| **User Service** | Registration, login, JWT auth, address CRUD | Spring Security, Redis session, Nacos config |
| **Product Service** | Product catalog, search, inventory, category tree | Redis cache (Cache-Aside), Bloom filter |
| **Order Service** | Order CRUD, Saga orchestration, status machine | Seata Saga, Kafka events, distributed lock |
| **Payment Service** | Mock payment processing, refunds | TCC pattern, idempotency, retry |
| **Gateway** | Routing, auth filter, rate limiting, CORS | Spring Cloud Gateway, Sentinel |

### Key Learning Objectives Practiced

1. **Service registration & discovery** via Nacos
2. **Centralized configuration** with Nacos Config, dynamic refresh
3. **API Gateway** routing, authentication, rate limiting
4. **Declarative HTTP client** with OpenFeign
5. **Circuit breaker** with Resilience4j fallbacks
6. **Distributed tracing** via Micrometer Tracing + Zipkin
7. **Distributed lock** with Redis (Redisson)
8. **Cache patterns** with Redis (Cache-Aside, Bloom filter)
9. **Message-driven architecture** with Kafka/RabbitMQ
10. **Distributed transactions** with Seata (Saga + TCC)
11. **Containerization** with Docker + Docker Compose
12. **CI/CD pipeline** (GitLab CI or Jenkins)

---

## Learning Resources

### Books

| Book | Why |
|---|---|
| **Building Microservices (2nd ed.)** — Sam Newman | The definitive guide to microservice architecture |
| **Microservices Patterns** — Chris Richardson | 44+ patterns for microservice design (Saga, CQRS, etc.) |
| **Spring Microservices in Action (2nd ed.)** — John Carnell | Hands-on Spring Cloud implementation |
| **Designing Data-Intensive Applications** — Martin Kleppmann | Distributed systems theory that underpins everything |
| **Domain-Driven Design** — Eric Evans | The foundation for service decomposition |
| **Implementing DDD** — Vaughn Vernon | Practical DDD with examples |

### Online Resources

- [Spring Cloud Reference](https://docs.spring.io/spring-cloud/docs/current/reference/html/)
- [Spring Cloud Alibaba Documentation](https://sca.aliyun.com/)
- [Microservices.io (Chris Richardson)](https://microservices.io/)
- [CNCF Cloud Native Landscape](https://landscape.cncf.io/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Redis Documentation](https://redis.io/docs/)

### Interview Preparation

After completing this stage, you should be able to answer:

1. "Compare CAP theorem and BASE theory. How do they apply to microservice design?"
2. "Explain the Saga pattern. Choreography vs orchestration — when to use each?"
3. "How does Spring Cloud Gateway differ from Zuul? What are the tradeoffs?"
4. "Design a distributed locking mechanism with Redis. What are the failure scenarios?"
5. "How does Kafka achieve high throughput? Explain zero-copy and sequential IO."
6. "Compare RabbitMQ vs Kafka for an order processing system."
7. "How would you migrate a monolith to microservices? What patterns would you use?"
8. "Explain the Circuit Breaker states and transition conditions."
9. "How do you ensure idempotency in a distributed payment system?"
10. "How does Nacos support both CP and AP? When would you choose each?"
11. "Describe the distributed tracing data model (trace/span). How do you propagate context?"
12. "What is the Outbox pattern and what problem does it solve?"

---

## Assessment Criteria

Upon completing this stage, you should demonstrate:

| Competency | Level |
|---|---|
| Design a microservice architecture from business requirements | Design |
| Implement service registration, discovery, and configuration with Nacos | Implement |
| Build an API Gateway with routing, filtering, and rate limiting | Implement |
| Apply circuit breaker, retry, and bulkhead patterns with Resilience4j | Apply |
| Configure distributed tracing and understand trace context propagation | Configure |
| Implement Redis caching patterns and distributed locks | Implement |
| Design and implement event-driven communication with Kafka/RabbitMQ | Design |
| Containerize services with Docker and orchestrate with Compose | Implement |
| Implement distributed transactions with Seata (Saga, TCC) | Implement |
| Analyze and troubleshoot distributed system failures | Analyze |

### Bloom's Taxonomy Mapping

```
Remember    → Know CAP theorem, BASE, service mesh concepts
Understand  → Explain tradeoffs between consistency models
Apply       → Configure Nacos, Gateway, Resilience4j
Analyze     → Diagnose distributed tracing data, identify bottlenecks
Evaluate    → Compare MQ technologies, choose appropriate patterns
Create      → Design a microservice architecture from scratch
```

---

## Production Readiness Checklist

### Before Going to Production

- [ ] All services have health check endpoints (`/actuator/health`)
- [ ] Graceful shutdown configured (`server.shutdown=graceful`)
- [ ] Circuit breakers configured with sensible thresholds
- [ ] Rate limiting configured at the gateway level
- [ ] Distributed tracing spans are sampled appropriately (head-based or tail-based)
- [ ] Logs are structured (JSON) and shipped to central logging
- [ ] Metrics are exported to Prometheus (`/actuator/prometheus`)
- [ ] Redis cache size and eviction policy are set
- [ ] Kafka/RabbitMQ topics have adequate partitions/replicas
- [ ] Database connection pools sized correctly (not too large)
- [ ] Docker images are minimal (multi-stage build, distroless base)
- [ ] Container CPU/memory limits and requests are set
- [ ] Secrets are externalized (not in code, not in images)
- [ ] CI/CD pipeline includes integration tests and canary analysis

### Disaster Recovery

- [ ] Multi-region deployment strategy documented
- [ ] Data backup and restore procedures tested
- [ ] Circuit breaker thresholds reviewed quarterly
- [ ] Chaos engineering exercises scheduled (e.g., Chaos Monkey)

---

## File Structure of This Stage

```
05-分布式与微服务/
├── README.md                          # This file — overview
├── 01-微服务架构设计.md                # Microservice architecture design
├── 02-Spring-Cloud组件.md             # Spring Cloud components
├── 03-Redis实战.md                     # Redis in practice
├── 04-消息队列.md                       # Message queues
├── 05-Docker容器化.md                  # Docker containerization
└── project/                            # Practice project code (if applicable)
```

---

## Quick Start: Minimum Viable Stack

For a quick hands-on start, set up this minimal stack:

```yaml
# docker-compose.yml — minimal dev environment
version: "3.8"
services:
  # Service Registry & Config Center
  nacos:
    image: nacos/nacos-server:v2.3.2
    ports:
      - "8848:8848"
      - "9848:9848"
    environment:
      MODE: standalone

  # Cache & Distributed Lock
  redis:
    image: redis:7.2.5-alpine
    ports:
      - "6379:6379"

  # Message Queue
  kafka:
    image: bitnami/kafka:3.7.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_CFG_NODE_ID: 0
      KAFKA_CFG_PROCESS_ROLES: controller,broker
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 0@localhost:9093
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092

  # Tracing Visualization
  zipkin:
    image: openzipkin/zipkin:3.1.0
    ports:
      - "9411:9411"
```

```bash
docker compose up -d nacos redis kafka zipkin
```

Then start building services using `start.spring.io` with dependencies:
- Spring Web, Spring Cloud Starter, Nacos Discovery, Nacos Config
- Spring Boot Actuator, Micrometer Tracing
- Spring Data Redis, Spring for Apache Kafka
- Gateway (for the gateway service)

---

## Next Steps After This Stage

After mastering Stage 5, proceed to advanced topics:

| Topic | Description |
|---|---|
| **Cloud-Native & Kubernetes Deep Dive** | Production K8s operators, service mesh (Istio), helm charts |
| **Reactive Programming** | WebFlux, Reactor, RSocket |
| **Big Data & Streaming** | Flink, Spark, stream processing |
| **Performance Engineering** | JVM tuning, profiling, load testing |
| **Security Deep Dive** | OAuth 2.1 / OIDC, Keycloak, Zero Trust |
| **Platform Engineering** | Internal developer platforms, Backstage |

---

> **"A distributed system is one in which the failure of a computer you didn't even know existed can render your own computer unusable."** — Leslie Lamport
