# BankFlow — Digital Banking & Transaction Platform
## Project Blueprint (read this before Phase 1)

---

## 0. Machine reality check (done)

**Work laptop (this one) — detected:**

| Tool | Status |
|---|---|
| JDK 21 (+24; Maven runs on 21) | OK |
| Maven 3.9.11 | OK |
| Node 24.13 / npm 11.6 | OK |
| Git 2.50 | OK |
| VS Code | OK |
| **MySQL 8.0.42 Community Server** | **OK — service `MySQL80` Running on `localhost:3306`** (X protocol 33060) |
| MySQL Workbench 8.0 / Shell / Router | OK |
| **Docker** | **NOT INSTALLED (cannot install)** |
| **Redis** | **NOT INSTALLED** (no Memurai either) |
| **Kafka** | **NOT INSTALLED** |

MySQL binaries live at `C:\Program Files\MySQL\MySQL Server 8.0\bin` (not on PATH —
call them by full path or add the folder to PATH). Config: `C:\ProgramData\MySQL\MySQL
Server 8.0\my.ini`, datadir under the same folder.

**Conclusion:** the work laptop can compile and run backend + frontend natively, **and
it has a real local MySQL 8** — so the database needs no cloud at all. Only **Redis and
Kafka** move to free cloud managed services over TLS. Docker files still get written —
they are for the personal laptop and for deployment — they just never run here.

**Personal laptop (has Docker Desktop):** `docker compose up -d` brings up MySQL +
Redis + Kafka locally. Same codebase, different Spring profile. Zero code change.

---

## 1. Dual-environment strategy (the core design decision)

Everything infra-related is read from **environment variables**, never hardcoded.
Spring profiles:

```
profile = dev     -> work laptop     -> LOCAL MySQL 8 + Redis Cloud + Confluent Kafka (TLS)
profile = docker  -> personal laptop -> localhost MySQL + Redis + Kafka (all in compose)
profile = local   -> anywhere / CI   -> H2 (MySQL mode) + in-memory cache + local events
```

Note that `dev` is a hybrid: the database is local, the cache and broker are cloud.
That is fine and in fact realistic — plenty of real teams run the DB locally and point
at shared staging infrastructure for the rest.

All profiles use the **same** `application.yml` keys. Only values differ.
`.env.example` is committed; the real `.env` is gitignored.

**Why this is good for interviews:** externalised configuration + profile-based
environment parity is exactly what 12-factor asks for. Not a workaround — the
correct pattern.

### Free cloud services to sign up for (only two now)

| Need | Service | Free tier | Alternative |
|---|---|---|---|
| Redis | **Redis Cloud** | Free 30 MB, real Redis 7 + TLS | Upstash Redis free tier |
| Kafka | **Confluent Cloud** Basic | $400 trial credits, SASL_SSL | Redpanda Serverless |

30 MB Redis is plenty — we cache small JSON objects with short TTLs.
Free-forever Kafka does not really exist; Confluent credits last months at our volume.

**MySQL needs no signup** — it is already running on this laptop.

### Local MySQL setup (Phase 1, one-time)

We will **not** use the `root` account from the application. Phase 1 generates
`db/init/01_create_database_and_user.sql` which creates:

- schema `bankflow` (utf8mb4 / utf8mb4_0900_ai_ci)
- schema `bankflow_test` (used by integration tests so they never touch dev data)
- a dedicated user `bankflow_app` with a generated password, granted only on those
  two schemas

You run that one file once as root through MySQL Workbench (or `mysql.exe -u root -p`).
The generated password is written into the gitignored `.env`, never into chat and never
into git. `application.yml` reads it as `${DB_PASSWORD}`.

Using a least-privilege DB user instead of root is itself an interview talking point.

### Safety nets so the app is NEVER unrunnable

1. **`local` profile** → H2 file DB in MySQL-compatibility mode. Backend boots with
   zero external services at all. Offline fallback only — normal dev and the test
   suite use your real MySQL.
2. **`EventPublisher` interface**, two implementations:
   - `KafkaEventPublisher` (profiles cloud/docker)
   - `LocalEventPublisher` (Spring `ApplicationEventPublisher`, profile local)
   The same handlers run either way. A Kafka outage is not a dead app.
3. **`CacheManager`** is already a Spring abstraction: `RedisCacheManager` for
   cloud/docker, `ConcurrentMapCacheManager` for local. Cache code is identical.

Points 2 and 3 are not over-engineering — they are the Spring abstractions designed
for exactly this, and they make the whole test suite Docker-free.

---

## 2. Roles and authorization model

Three roles in a `roles` table, many-to-many with `users`.

### `ROLE_CUSTOMER` — the bank's end user
- Register, login, refresh token, logout
- View/update own profile, change password
- Open an account (SAVINGS / CURRENT)
- View own accounts and balances
- Deposit, Withdraw, Transfer (own→own, own→beneficiary)
- Manage beneficiaries (add / verify / delete)
- Own transaction history (paged, filterable) + transaction detail
- Download own account statement (date range → PDF/CSV)
- **Hard rule:** can never touch another user's account. Enforced twice — method-level
  `@PreAuthorize("@accountGuard.owns(#accountId)")` *and* repository queries scoped
  by the authenticated user id.

### `ROLE_ADMIN` — bank operations staff
- Admin login (same endpoint; role decides what opens)
- List / search / paginate all users; view any user
- Enable / disable a user
- List all accounts; **block / unblock / freeze** an account
- Monitor all transactions (filter by status, amount, date, account)
- Force-reverse a FAILED or stuck transaction
- Read audit logs
- Dashboard: total users, total accounts, today's txn count and volume, failed txns,
  flagged-fraud count
- **Cannot** deposit/withdraw/transfer on a customer's behalf — deliberate separation
  of duties, and a good thing to be able to explain.

### `ROLE_AUDITOR` — read-only (cheap to add, strong talking point)
- Read-only access to audit logs and transaction monitoring
- No mutation endpoints at all
- Proves authorization is genuinely role-driven, not just "admin vs not-admin".

### Enforcement layers
1. `SecurityFilterChain` — URL patterns (`/api/admin/**` → `hasRole("ADMIN")`)
2. `@PreAuthorize` / `@PostAuthorize` on service methods
3. Ownership guard bean for row-level checks
4. JWT carries `sub`, `uid`, `roles`, `iat`, `exp`. Access token 15 min, refresh token
   7 days; refresh token ids live in Redis so logout can actually revoke them.

---

## 3. What the finished project will actually DO

End-to-end walkthrough:

1. **Customer registers** → `POST /api/auth/register` → user row + `ROLE_CUSTOMER` +
   BCrypt hash. Kafka `user.registered` → notification consumer writes a welcome row.
2. **Logs in** → access + refresh JWT; refresh id cached in Redis.
3. **Opens a savings account** → `ACC0000000001`, balance 0, status ACTIVE.
4. **Deposits 50,000** → in one `@Transactional` boundary:
   - pessimistic lock on the account row (`SELECT ... FOR UPDATE`) so concurrent
     deposits cannot race
   - balance updated; `Transaction` row written with status SUCCESS and a unique
     `reference`; an **idempotency key** means replaying the request is a no-op
   - Redis cache for that account **evicted**
   - Kafka `transaction.created` published **after commit**
     (`@TransactionalEventListener(AFTER_COMMIT)` → no phantom events)
5. **Kafka fans out** — one topic, three consumer groups:
   - `audit-group` → writes an `AuditLog` row (who, what, when, IP, user-agent)
   - `fraud-group` → rules: amount > 2,00,000, or >5 txns in 60 s (counted in Redis),
     or transfer to a brand-new beneficiary → marks txn `FLAGGED`, emits
     `transaction.flagged`
   - `notification-group` → builds an in-app notification
6. **Adds a beneficiary** (another account number) — must be verified before the
   first transfer.
7. **Transfers 10,000** → debit + credit in one transaction, both rows locked in a
   **deterministic order by account id** so two mutual transfers cannot deadlock.
   Insufficient balance / blocked account / unverified beneficiary → domain exception
   → clean 4xx JSON from `@RestControllerAdvice`.
8. **Views history** → paged, sorted, filterable. Second call to "my accounts" is
   served from Redis (visible in logs / Redis Insight).
9. **Downloads a statement** for a date range as PDF.
10. **Hits the API 100 times in a minute** → Redis sliding-window limiter returns
    `429` with a `Retry-After` header.
11. **Admin logs in** → dashboard shows the txn, sees it FLAGGED, opens the audit
    trail, blocks the account → the customer's next withdraw is rejected with
    `ACCOUNT_BLOCKED`.
12. **Everything documented** in Swagger UI; a Postman collection replays the flow.

Demo-able in four minutes. That is what makes it interview-ready.

---

## 4. Architecture

```
  React 18 (Vite) -- Axios (interceptors: attach JWT, auto-refresh on 401)
        |  HTTPS / JSON
        v
  Spring Boot 3.5 / Java 21
        |
  [ RateLimitFilter -> JwtAuthenticationFilter -> SecurityFilterChain ]
        |
   @RestController --> @Service (@Transactional, @PreAuthorize)
                          |          |              |
                          |          |              +--> RedisTemplate / @Cacheable
                          |          +--> KafkaTemplate (after commit)
                          v
                    Spring Data JPA Repository
                          v
                       Hibernate ORM
                          v
                        MySQL 8

   Kafka topics --> @KafkaListener consumers (audit / fraud / notification)
                          +--> write back to MySQL
```

### Kafka topics

| Topic | Producer | Consumer groups |
|---|---|---|
| `transaction.created` | TransactionService | audit, fraud, notification |
| `transaction.flagged` | FraudConsumer | notification, audit |
| `account.status.changed` | AdminAccountService | audit, notification |
| `user.registered` | AuthService | notification, audit |
| `*.DLT` | Spring retry / DLT | manual inspection |

Partitions keyed by `accountId` → all events for one account stay ordered.
Consumers are **idempotent** (dedupe on `eventId`) because Kafka is at-least-once.

### Redis keyspace

| Key | Purpose | TTL |
|---|---|---|
| `account:{id}` | account snapshot | 10 min, evicted on any balance change |
| `user:profile:{id}` | profile | 30 min |
| `txn:status:{ref}` | txn status lookup | 5 min |
| `rl:{userOrIp}:{route}` | rate-limit sliding window | 60 s |
| `refresh:{jti}` | valid refresh tokens (logout revokes) | 7 days |
| `fraud:velocity:{accountId}` | txn count for velocity rule | 60 s |

---

## 5. Backend package structure

```
backend/src/main/java/com/bankflow/
├── BankflowApplication.java
├── config/       SecurityConfig, RedisConfig, KafkaTopicConfig, OpenApiConfig,
│                 JpaAuditingConfig, CorsConfig, AsyncConfig
├── controller/   AuthController, UserController, AccountController,
│                 TransactionController, BeneficiaryController, StatementController,
│                 admin/AdminUserController, admin/AdminAccountController,
│                 admin/AdminTransactionController, admin/AuditLogController,
│                 admin/DashboardController
├── service/      AuthService, UserService, AccountService, TransactionService,
│                 TransferService, BeneficiaryService, StatementService,
│                 AuditService, FraudService, NotificationService, DashboardService
│                 (+ impl/)
├── repository/   UserRepository, RoleRepository, AccountRepository,
│                 BeneficiaryRepository, TransactionRepository,
│                 AuditLogRepository, NotificationRepository
├── entity/       BaseEntity, User, Role, Account, Beneficiary, Transaction,
│                 AuditLog, Notification
│                 + enums/ AccountType, AccountStatus, TransactionType,
│                          TransactionStatus, RoleName, KycStatus
├── dto/          request/  RegisterRequest, LoginRequest, DepositRequest,
│                           WithdrawRequest, TransferRequest, BeneficiaryRequest...
│                 response/ AuthResponse, AccountResponse, TransactionResponse,
│                           PageResponse<T>, ApiError, DashboardResponse
│                 mapper/   entity <-> dto mappers
├── security/     JwtService, JwtAuthenticationFilter, CustomUserDetails,
│                 CustomUserDetailsService, AccountGuard, JwtAuthEntryPoint,
│                 RestAccessDeniedHandler
├── exception/    GlobalExceptionHandler, BusinessException, ErrorCode,
│                 ResourceNotFoundException, InsufficientBalanceException,
│                 AccountBlockedException, DuplicateResourceException
├── kafka/        producer/ EventPublisher (interface), KafkaEventPublisher,
│                           LocalEventPublisher
│                 consumer/ AuditEventConsumer, FraudEventConsumer,
│                           NotificationEventConsumer
│                 event/    BaseEvent, TransactionCreatedEvent,
│                           TransactionFlaggedEvent, AccountStatusChangedEvent,
│                           UserRegisteredEvent
├── redis/        CacheKeys, RateLimitService, RateLimitFilter, RefreshTokenStore,
│                 AccountCacheService
└── util/         AccountNumberGenerator, ReferenceGenerator, MoneyUtils,
                  DateUtils, RequestContext
```

Money is `BigDecimal(19,4)` everywhere. Never `double`. Ever.

---

## 6. Database entities

```
users          id, first_name, last_name, email(uniq), phone(uniq), password_hash,
               enabled, address_line, city, state, pincode, kyc_status,
               created_at, updated_at

roles          id, name(uniq: ROLE_CUSTOMER | ROLE_ADMIN | ROLE_AUDITOR)

user_roles     user_id, role_id                                          (M:N)

accounts       id, account_number(uniq), user_id(FK), type, status,
               balance DECIMAL(19,4), currency, opened_at, version(@Version)

beneficiaries  id, owner_user_id(FK), beneficiary_account_number, nickname,
               bank_ifsc, verified, created_at
               unique(owner_user_id, beneficiary_account_number)

transactions   id, reference(uniq), type, status, amount DECIMAL(19,4),
               from_account_id, to_account_id, balance_after, description,
               idempotency_key(uniq), created_at
               index(from_account_id, created_at), index(status)

audit_logs     id, actor_user_id, action, entity_type, entity_id, details JSON,
               ip_address, user_agent, created_at

notifications  id, user_id, title, message, channel, read_flag, created_at
```

Flyway migrations (`V1__init.sql`, `V2__seed_roles_and_admin.sql`, ...), with
`ddl-auto=validate`. Not `ddl-auto=update` — real projects version their schema.

---

## 7. Frontend structure (React + Vite + JavaScript)

```
frontend/src/
├── api/          axiosClient.js (baseURL, JWT interceptor, 401 refresh queue)
│                 authApi.js accountApi.js txnApi.js beneficiaryApi.js adminApi.js
├── auth/         AuthContext.jsx, useAuth.js, ProtectedRoute.jsx, RoleRoute.jsx
├── components/   Navbar Sidebar Card DataTable Pagination Modal AmountInput
│                 StatusBadge Loader Toast ErrorBoundary ConfirmDialog
├── pages/
│   ├── auth/     Login.jsx Register.jsx
│   ├── customer/ Dashboard AccountList AccountDetail OpenAccount Deposit
│   │             Withdraw Transfer Beneficiaries TransactionHistory
│   │             TransactionDetail Statement Profile
│   └── admin/    AdminDashboard Users UserDetail Accounts
│                 TransactionMonitor AuditLogs
├── routes/       AppRoutes.jsx
├── styles/       variables.css global.css (plain CSS3 — spec says CSS3, no Tailwind)
└── utils/        formatMoney.js formatDate.js validators.js
```

React Router v6 with nested layouts. `RoleRoute` renders admin pages only for ADMIN
and redirects everyone else to `/403`.

---

## 8. Testing plan (all Docker-free — runs on the work laptop)

| Layer | Tooling | What is tested |
|---|---|---|
| Unit | JUnit 5 + Mockito | AuthService (duplicate email, bad password, token claims), AccountService (open/close/block), TransactionService (deposit, withdraw insufficient funds, idempotency replay), TransferService (same-account reject, blocked account, unverified beneficiary, balance math), FraudService rules, RateLimitService |
| Web slice | `@WebMvcTest` + `spring-security-test` | validation 400s, 401 without token, 403 customer→admin endpoint, response JSON shape |
| Integration | `@SpringBootTest` + **real local MySQL `bankflow_test`** + Flyway | full deposit → transfer → history flow over real JPA, real InnoDB |
| Kafka | `@EmbeddedKafka` (spring-kafka-test) | event published only after commit; every consumer group receives; consumer idempotency |
| Concurrency | `@SpringBootTest` + ExecutorService + **real MySQL** | 20 parallel withdraws on one account never overdraw |
| Coverage | JaCoCo, gate at 75% on service + security | |

Two things make this whole suite Docker-free:
- **`@EmbeddedKafka`** runs a real broker in-JVM.
- **Local MySQL 8** gives real InnoDB semantics — which matters enormously for the
  concurrency test, because `SELECT ... FOR UPDATE` row locking is exactly what we are
  verifying and H2 would not prove it. This is a real upgrade over the earlier plan.

H2 stays configured under the `local` profile as an offline fallback only.

---

## 9. Docker and CI/CD

**`docker-compose.yml`** — authored here, executed on the personal laptop:
`mysql:8.4`, `redis:7-alpine`, `apache/kafka:3.8` in **KRaft mode (no Zookeeper)**,
`kafka-ui`, `backend` (multi-stage Dockerfile), `frontend` (nginx serving the Vite
build). Healthchecks plus `depends_on: condition: service_healthy` so the backend
waits for MySQL.

**GitHub Actions** (`.github/workflows/ci.yml`) — runs on GitHub's Linux runners,
where Docker is free:
1. `backend` — JDK 21, cache `~/.m2`, `mvn verify`, MySQL 8 + Redis 7 as **service
   containers**, upload JaCoCo report
2. `frontend` — Node 20, `npm ci`, `npm run lint`, `npm run build`
3. `images` — build and push both images to GHCR on `main`
4. `deploy` — hit the Render deploy hook

CI gives real MySQL/Redis integration tests without any local Docker.

**Deployment (free, no AWS):**
- Backend → **Render** Web Service from the Dockerfile (free tier sleeps after 15 min
  idle — fine for a demo)
- Frontend → **Vercel** or Netlify (static, instant)
- MySQL → Aiven, Redis → Redis Cloud, Kafka → Confluent (same as the cloud profile)
- **Railway** is the fallback if Render's cold start is annoying

For the deployed environment the DB cannot be your laptop, so production gets a free
managed MySQL — **Aiven for MySQL** (free plan) or **TiDB Cloud Serverless**. That is
a Phase 11 concern only; local dev keeps using your own MySQL 8.

---

## 10. Phase plan — the app runs after every phase

| # | Phase | Deliverable | Runnable? |
|---|---|---|---|
| 1 | Scaffold | Git repo, Maven backend, Vite frontend, `.gitignore`, `.env.example`, DB init SQL, `dev` profile on **local MySQL**, `/api/health` → 200, React calls it | yes, both start |
| 2 | Entities | 8 entities, enums, repositories, Flyway V1+V2 against real MySQL, seeded roles + admin | yes, schema builds |
| 3 | Security | JWT, filter, register/login/refresh/logout, 3 roles, `@PreAuthorize`, ownership guard | yes, auth works |
| 4 | Banking ops | accounts, deposit, withdraw, transfer (locking + idempotency), beneficiaries, history, statement | yes, full API |
| 5 | Frontend | all customer + admin pages, Axios interceptors, protected routes | yes, full UI |
| 6 | Redis | cloud Redis wired, caching + eviction, rate limiter, refresh-token store | yes |
| 7 | Kafka | cloud Kafka wired, topics, after-commit producer, 3 consumers, DLT | yes |
| 8 | Tests | full suite per section 8, JaCoCo | yes, `mvn verify` green |
| 9 | Docker | Dockerfiles + compose + `.dockerignore` (authored here, verified on personal laptop) | yes, on personal laptop |
| 10 | CI/CD | GitHub Actions, 4 jobs | yes |
| 11 | Deploy | Render + Vercel live URLs | yes, public |
| 12 | Docs | Swagger, README, ER diagram, architecture diagram, Postman collection | yes |

Phases 1–8 are fully doable on the work laptop. Phase 9 is authored here and
verified on the personal laptop.

---

## 11. Moving to the personal laptop

```bash
git clone https://github.com/<you>/bankflow.git
cd bankflow
cp .env.example .env          # fill secrets, or just use the docker profile
docker compose up -d          # MySQL + Redis + Kafka + kafka-ui
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
cd frontend && npm install && npm run dev
```

That is the whole handover. Because config is env-var driven, nothing else changes.

---

## 12. Interview talking points this project earns you

- Pessimistic locking plus deterministic lock ordering to prevent lost updates and
  deadlocks in money transfer
- Idempotency keys so a retried payment never double-charges
- Publishing Kafka events **after commit**, not inside the transaction
- At-least-once delivery → idempotent consumers, DLT for poison messages
- Cache-aside with explicit invalidation on writes (the genuinely hard part of caching)
- Sliding-window rate limiting in Redis
- Defence in depth on authorization: URL level + method level + row level
- Refresh-token revocation (stateless JWT with a stateful revocation list)
- Flyway over `ddl-auto`; `BigDecimal` over `double`
- Profile-based environment parity, zero hardcoded infrastructure

---

## Decisions already made (say the word to change any)

1. Spring Boot **3.5.x**, Java **21** (Maven is already on 21)
2. **Vite** for React, not CRA (CRA is deprecated)
3. Plain **CSS3** with CSS variables, no Tailwind (spec says CSS3)
4. **Flyway** migrations, `ddl-auto=validate`
5. **Local MySQL 8.0.42** for dev and tests; **Redis Cloud + Confluent Cloud** for
   cache and broker; managed MySQL only for the Phase 11 deployment
8. A least-privilege DB user `bankflow_app`, not `root`
6. Repo name **`bankflow`**, monorepo layout: `backend/` + `frontend/`
7. `ROLE_AUDITOR` added as a third role
