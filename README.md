# BankFlow — Digital Banking & Transaction Platform

A full-stack banking application: customer accounts, deposits, withdrawals,
fund transfers with beneficiary management, and an admin console for user
management, account blocking, transaction monitoring and audit trails.

Built with Spring Boot and React, backed by MySQL, with Redis for caching and
rate limiting and Kafka for event-driven audit, fraud and notification
processing.

---

## Status

Built in phases. Each phase leaves the application runnable.

| Phase | Scope | Status |
|---|---|---|
| 1 | Project scaffold, profiles, health check, design system | Complete |
| 2 | JPA entities, Flyway migrations, repositories | Next |
| 3 | Spring Security, JWT, role-based authorization | Planned |
| 4 | Accounts, deposit, withdraw, transfer, beneficiaries, statements | Planned |
| 5 | React customer and admin screens | Planned |
| 6 | Redis caching, cache invalidation, rate limiting | Planned |
| 7 | Kafka producers and consumers | Planned |
| 8 | JUnit 5 / Mockito test suite | Planned |
| 9 | Docker and Docker Compose | Planned |
| 10 | GitHub Actions CI/CD | Planned |
| 11 | Deployment (Render + Vercel) | Planned |
| 12 | Swagger/OpenAPI, ER diagram, Postman collection | Planned |

Design and architecture decisions are recorded in [BLUEPRINT.md](BLUEPRINT.md).
Frontend rules are in [DESIGN.md](DESIGN.md).

---

## Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, React Router 7, Axios, plain CSS3, Vite 8 |
| Backend | Java 21, Spring Boot 3.5, Spring MVC, Spring Security, JWT |
| Persistence | MySQL 8, Spring Data JPA, Hibernate, Flyway |
| Cache | Redis (caching, rate limiting, refresh-token revocation) |
| Messaging | Apache Kafka (audit, fraud, notification consumers) |
| Testing | JUnit 5, Mockito, Spring Boot Test, embedded Kafka |
| Build | Maven, npm |
| CI/CD | GitHub Actions, Docker, Docker Compose |

---

## Architecture

```
  React ── Axios
     │  HTTPS / JSON
     ▼
  Spring Boot
     │
  [ RateLimitFilter → JwtAuthenticationFilter → SecurityFilterChain ]
     │
  @RestController → @Service (@Transactional, @PreAuthorize)
     │                  │            │
     │                  │            └─► Redis (cache, rate limit)
     │                  └─► Kafka (published after commit)
     ▼
  Spring Data JPA → Hibernate → MySQL

  Kafka topics → audit / fraud / notification consumers → MySQL
```

A transaction is written inside one database transaction with the account rows
locked, and the Kafka event is published only after that transaction commits.
Three consumer groups read the same topic independently: one writes the audit
log, one applies fraud rules, one queues notifications.

---

## Running locally

### Prerequisites

- JDK 21
- Maven 3.9+
- Node 20+
- MySQL 8 running on `localhost:3306`

### 1. Configure

```bash
cp .env.example .env
```

Fill in `DB_PASSWORD` and `JWT_SECRET`. The backend reads `.env` through
`spring.config.import`, and Vite reads the `VITE_*` keys from the same file, so
there is one place to configure and nothing is hardcoded.

### 2. Create the schemas

Run `db/init/01_create_database_and_user.sql` once as MySQL root. It creates the
`bankflow` and `bankflow_test` schemas and a least-privilege `bankflow_app`
user — the application never connects as root.

```bash
mysql -u root -p < db/init/01_create_database_and_user.sql
```

### 3. Start the backend

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Start the frontend

```bash
cd frontend && npm install && npm run dev
```

The app is served at `http://localhost:5173` and proxies `/api` to port 8080,
so the browser stays same-origin in development just as it does behind nginx in
production.

### Profiles

| Profile | Database | Cache / broker | Use |
|---|---|---|---|
| `dev` | local MySQL 8 | managed Redis and Kafka | day-to-day development |
| `docker` | MySQL in Compose | Redis and Kafka in Compose | full local stack |
| `local` | H2, MySQL mode | in-memory, in-process events | offline, no services needed |

Because every component reads Spring's `CacheManager` and an `EventPublisher`
interface rather than Redis and Kafka directly, the `local` profile needs no
code changes.

---

## Tests

```bash
cd backend && mvn verify
```

Unit tests cover the services with Mockito. Web-slice tests assert the security
rules — that an unauthenticated request is rejected and that a customer cannot
reach an admin endpoint. Integration tests run against real MySQL so that
`SELECT ... FOR UPDATE` row locking is genuinely exercised, including a
concurrency test that fires parallel withdrawals at one account and asserts it
can never be overdrawn. Kafka tests use an in-JVM broker, so no Docker is
required to run the suite.

---

## Project layout

```
backend/    Spring Boot application (com.bankflow)
frontend/   React application
db/init/    One-time MySQL schema and user setup
BLUEPRINT.md  Architecture, phase plan, infrastructure decisions
DESIGN.md     Frontend design system and its rules
```

---

## Notes on a few decisions

- **Money is `BigDecimal(19,4)`**, never a floating-point type.
- **Flyway owns the schema**; Hibernate runs with `ddl-auto: none`, so the
  database is never altered implicitly.
- **Transfers lock both account rows in a deterministic order** by account id,
  which is what prevents a deadlock when two accounts transfer to each other at
  the same time.
- **Every write carries an idempotency key**, so a retried request cannot double
  charge an account.
- **Kafka consumers are idempotent**, because delivery is at-least-once.
- **Amounts are formatted with Indian digit grouping** (`₹1,50,000.00`) and
  tabular numerals, so figures align down a column.
