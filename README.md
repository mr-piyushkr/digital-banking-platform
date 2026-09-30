# 🏦 BankFlow — Digital Banking & Transaction Platform

<p align="center">
  <strong>A production-oriented full-stack digital banking platform built with Spring Boot, React, MySQL, Redis and Apache Kafka.</strong>
</p>

<p align="center">
  Designed to demonstrate real-world banking workflows, secure authorization, transactional money movement, fraud detection, event-driven architecture, caching, rate limiting, testing, CI/CD and cloud deployment.
</p>

<p align="center">

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Authentication-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

</p>

<p align="center">

![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

</p>

---

## 📌 Project Overview

**BankFlow** is a full-stack digital banking and transaction platform designed around realistic banking operations rather than a simple CRUD application.

The platform provides customer banking operations such as:

- User registration and authentication
- JWT-based authorization
- Savings and current accounts
- Account balances
- Deposits
- Withdrawals
- Fund transfers
- Beneficiary management
- Transaction history
- Account statements
- User profile management
- Notifications

It also includes an administration layer for:

- User management
- Account management
- Account blocking/unblocking/freezing
- Transaction monitoring
- Fraud review
- Audit logs
- Banking dashboard
- Role-based access control

The planned architecture extends the platform with **Redis caching and rate limiting**, **Apache Kafka event-driven processing**, automated testing, Docker, CI/CD and cloud deployment.

The architecture and infrastructure decisions are documented in `BLUEPRINT.md`, while the frontend design rules are documented in `DESIGN.md`.

---

# 🚦 Current Project Status

The project is being developed incrementally. Every phase is intended to leave the application in a runnable state.

### Current Progress

| Phase | Description | Status |
|---|---|---|
| 1 | Project Scaffold + Design System + Health Check | ✅ Complete |
| 2 | JPA Entities + Flyway + Repositories | ✅ Complete |
| 3 | Spring Security + JWT + RBAC | ✅ Complete |
| 4 | Accounts + Deposits + Withdrawals + Transfers + Beneficiaries + Statements | ✅ Complete |
| 5 | Complete React Customer/Admin UI | 🔜 Next |
| 6 | Redis Caching + Rate Limiting | ⏳ Pending |
| 7 | Apache Kafka + Event Consumers | ⏳ Pending |
| 8 | Full Testing + JaCoCo | ⏳ Pending |
| 9 | Docker + Docker Compose | ⏳ Pending |
| 10 | GitHub Actions CI/CD | ⏳ Pending |
| 11 | Cloud Deployment | ⏳ Pending |
| 12 | Swagger + ER Diagram + Postman + Final Documentation | ⏳ Pending |

### Current Milestone

**Completed through Phase 4 — Banking Operations Backend**

The next major milestone is **Phase 5 — Complete React Customer & Admin Interface**.

---

# ✨ Key Features

## 🔐 Authentication & Authorization

- User registration
- Secure login
- BCrypt password hashing
- JWT access tokens
- Refresh token flow
- Logout support
- Role-based authorization
- Method-level authorization with `@PreAuthorize`
- Account ownership validation
- Protected customer endpoints
- Protected admin endpoints
- Auditor read-only access model
- Separation of customer and administrative responsibilities

---

## 👤 Customer Banking

- Customer profile
- Profile update
- Password change
- Account opening
- Savings account support
- Current account support
- Account balance management
- Account details
- Account number generation
- Account status handling

---

## 💰 Money Movement

### Deposit

- Deposit money into an account
- Transaction creation
- Transaction reference generation
- Idempotency support
- Transaction status tracking

### Withdrawal

- Withdraw money from an account
- Insufficient balance validation
- Account status validation
- Idempotency support
- Transaction history

### Transfer

- Account-to-account transfers
- Own-account transfers
- Beneficiary-based transfers
- Verified beneficiary requirement
- Balance validation
- Daily transfer limits
- Transaction creation
- Idempotency support
- Deterministic account locking
- Deadlock prevention

---

## 👥 Beneficiary Management

- Add beneficiary
- List beneficiaries
- Verify beneficiary
- Delete beneficiary
- Prevent duplicate beneficiaries
- Prevent adding own account as beneficiary
- Require verification before external transfers

---

## 📊 Transaction Management

- Transaction references
- Transaction types
- Transaction statuses
- Transaction history
- Pagination
- Account-specific transaction history
- Transaction detail lookup
- Idempotency keys
- Balance-after tracking

---

## 📄 Account Statements

- Account-specific statements
- Date-range filtering
- Opening balance calculation
- Closing balance
- Credit totals
- Debit totals
- Transaction count
- Maximum statement range validation

---

## 🛡️ Security

BankFlow uses multiple authorization layers:

1. URL-level authorization
2. Method-level authorization
3. Account ownership checks
4. Repository-level user scoping
5. JWT authentication
6. BCrypt password hashing

The architecture intentionally prevents customers from accessing another customer's account.

---

## 🧑‍💼 Admin Operations

The planned admin console supports:

- User listing
- User search
- User detail
- Enable/disable users
- Account listing
- Account filtering
- Account status management
- Transaction monitoring
- Transaction filtering
- Fraud flag review
- Audit log viewing
- Dashboard statistics

Administrative operations deliberately do not allow staff to perform customer money movement.

---

## 🕵️ Fraud Detection

BankFlow includes a fraud-screening design based on configurable rules.

Current fraud service logic includes:

- High-value transaction detection
- Transaction velocity detection
- Large transfer to recently added beneficiary
- Transaction flagging
- Fraud reason tracking
- Admin review flow

The planned Kafka architecture will process fraud events asynchronously.

---

# 🧠 Important Engineering Concepts

BankFlow is designed to demonstrate more than basic CRUD development.

### 🔒 Pessimistic Locking

Money movement uses database row locking to protect balances during concurrent transactions.

```sql
SELECT ... FOR UPDATE
```

### 🔁 Idempotency

Every money-moving request can carry an idempotency key.

This prevents retries from accidentally creating duplicate financial transactions.

### 🔀 Deterministic Lock Ordering

Transfers lock source and destination accounts in ascending account-ID order.

This prevents the classic:

```text
Account A → Account B
Account B → Account A
```

deadlock scenario.

### 💵 BigDecimal

Financial amounts use:

```java
BigDecimal
```

instead of floating-point types.

The database model uses:

```text
DECIMAL(19,4)
```

### 🗃️ Flyway

Database schema changes are version controlled using Flyway migrations.

Hibernate is not responsible for automatically modifying the production schema.

### 🔐 Defence in Depth

Authorization is enforced at multiple levels instead of trusting only a frontend check or URL rule.

---

# 🏗️ Architecture

```text
                         ┌──────────────────────┐
                         │      React 19        │
                         │   Vite + Axios       │
                         └──────────┬───────────┘
                                    │
                              HTTPS / JSON
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    Spring Boot 3.5   │
                         │       Java 21        │
                         └──────────┬───────────┘
                                    │
                  ┌─────────────────┼─────────────────┐
                  │                 │                 │
                  ▼                 ▼                 ▼
             Controllers        Services          Security
                  │                 │                 │
                  │          @Transactional       JWT/RBAC
                  │                 │
                  ▼                 ▼
             DTO / Mapper       Repositories
                                    │
                                    ▼
                              Hibernate / JPA
                                    │
                                    ▼
                              ┌────────────┐
                              │   MySQL 8   │
                              └────────────┘

       Planned Event-Driven Architecture
       
                              ┌────────────┐
                              │    Kafka   │
                              └─────┬──────┘
                                    │
                ┌───────────────────┼───────────────────┐
                ▼                   ▼                   ▼
          Audit Consumer      Fraud Consumer     Notification
                                                    Consumer
```

---

# 🧩 Technology Stack

## ☕ Backend

<p>
<img src="https://skillicons.dev/icons?i=java,spring,maven" />
</p>

| Technology | Purpose |
|---|---|
| Java 21 | Backend programming language |
| Spring Boot 3.5 | Application framework |
| Spring MVC | REST API development |
| Spring Security | Authentication & authorization |
| JWT | Stateless authentication |
| Spring Data JPA | Persistence layer |
| Hibernate | ORM |
| Flyway | Database migrations |
| Maven | Build management |
| Lombok | Boilerplate reduction |

---

## ⚛️ Frontend

<p>
<img src="https://skillicons.dev/icons?i=react,vite,css,js,html" />
</p>

| Technology | Purpose |
|---|---|
| React 19 | UI framework |
| React Router 7 | Client-side routing |
| Axios | HTTP client |
| Vite | Frontend build tool |
| JavaScript | Frontend language |
| CSS3 | Styling |
| Lucide React | UI icons |

The frontend intentionally uses **plain CSS with design tokens** rather than Tailwind or a utility CSS framework.

---

## 🗄️ Database

<p>
<img src="https://skillicons.dev/icons?i=mysql" />
</p>

| Technology | Purpose |
|---|---|
| MySQL 8 | Primary relational database |
| JPA | Persistence abstraction |
| Hibernate | ORM |
| Flyway | Schema versioning |
| H2 | Offline/test fallback |

---

## ⚡ Caching & Rate Limiting

<p>
<img src="https://skillicons.dev/icons?i=redis" />
</p>

### Planned

- Redis account caching
- User profile caching
- Transaction status caching
- Refresh token storage/revocation
- Sliding-window rate limiting
- Fraud velocity tracking
- Cache eviction after balance-changing operations

---

## 📨 Event-Driven Messaging

<p>
<img src="https://skillicons.dev/icons?i=kafka" />
</p>

### Planned

Apache Kafka will handle:

- Transaction events
- Fraud events
- Audit events
- Notification events
- Dead-letter processing
- Independent consumer groups
- Idempotent event processing

---

## 🐳 DevOps & CI/CD

<p>
<img src="https://skillicons.dev/icons?i=docker,githubactions,linux,git,github" />
</p>

### Planned

- Docker
- Docker Compose
- Multi-stage Docker builds
- GitHub Actions
- GitHub Container Registry
- Automated backend tests
- Frontend lint/build
- Container image publishing
- Deployment automation

---

# 🗂️ Project Structure

```text
bankflow/
│
├── backend/
│   ├── pom.xml
│   │
│   └── src/
│       ├── main/
│       │   ├── java/com/bankflow/
│       │   │
│       │   ├── config/
│       │   │   ├── CorsConfig.java
│       │   │   ├── JpaAuditingConfig.java
│       │   │   ├── SecurityConfig.java
│       │   │   └── StaffAccountSeeder.java
│       │   │
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   ├── UserController.java
│       │   │   ├── AccountController.java
│       │   │   ├── TransactionController.java
│       │   │   ├── BeneficiaryController.java
│       │   │   ├── StatementController.java
│       │   │   ├── NotificationController.java
│       │   │   ├── HealthController.java
│       │   │   │
│       │   │   └── admin/
│       │   │       ├── AdminUserController.java
│       │   │       ├── AdminAccountController.java
│       │   │       ├── AdminTransactionController.java
│       │   │       ├── AuditLogController.java
│       │   │       └── DashboardController.java
│       │   │
│       │   ├── dto/
│       │   │   ├── request/
│       │   │   └── response/
│       │   │
│       │   ├── entity/
│       │   │   ├── User.java
│       │   │   ├── Role.java
│       │   │   ├── Account.java
│       │   │   ├── Beneficiary.java
│       │   │   ├── Transaction.java
│       │   │   ├── AuditLog.java
│       │   │   ├── Notification.java
│       │   │   └── enums/
│       │   │
│       │   ├── exception/
│       │   │   ├── BusinessException.java
│       │   │   ├── ErrorCode.java
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   ├── AccountBlockedException.java
│       │   │   ├── InsufficientBalanceException.java
│       │   │   ├── DuplicateResourceException.java
│       │   │   └── ResourceNotFoundException.java
│       │   │
│       │   ├── kafka/
│       │   │   ├── DomainEventBridge.java
│       │   │   ├── consumer/
│       │   │   ├── event/
│       │   │   └── producer/
│       │   │
│       │   ├── repository/
│       │   │
│       │   ├── security/
│       │   │   ├── JwtService.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   ├── CustomUserDetails.java
│       │   │   ├── CustomUserDetailsService.java
│       │   │   ├── AccountGuard.java
│       │   │   └── RefreshTokenStore.java
│       │   │
│       │   ├── service/
│       │   │   ├── AuthService.java
│       │   │   ├── UserService.java
│       │   │   ├── AccountService.java
│       │   │   ├── TransactionService.java
│       │   │   ├── TransferService.java
│       │   │   ├── BeneficiaryService.java
│       │   │   ├── StatementService.java
│       │   │   ├── AuditService.java
│       │   │   ├── FraudService.java
│       │   │   ├── NotificationService.java
│       │   │   └── DashboardService.java
│       │   │
│       │   └── util/
│       │
│       ├── resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   ├── application-local.yml
│       │   └── db/
│       │       └── migration/
│       │           ├── V1__init.sql
│       │           └── V2__seed_roles.sql
│       │
│       └── test/
│           ├── java/
│           └── resources/
│
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── eslint.config.js
│   │
│   └── src/
│       ├── api/
│       │   ├── axiosClient.js
│       │   ├── authApi.js
│       │   ├── accountApi.js
│       │   ├── txnApi.js
│       │   ├── beneficiaryApi.js
│       │   ├── notificationApi.js
│       │   └── adminApi.js
│       │
│       ├── auth/
│       │   ├── AuthContext.jsx
│       │   ├── ProtectedRoute.jsx
│       │   ├── RoleRoute.jsx
│       │   ├── tokenStore.js
│       │   └── useAuth.js
│       │
│       ├── components/
│       │   ├── Button.jsx
│       │   ├── Card.jsx
│       │   ├── Field.jsx
│       │   ├── Notice.jsx
│       │   ├── PageLoader.jsx
│       │   ├── SkeletonRows.jsx
│       │   └── StatusBadge.jsx
│       │
│       ├── styles/
│       │   ├── tokens.css
│       │   ├── global.css
│       │   └── components.css
│       │
│       ├── utils/
│       │   ├── formatMoney.js
│       │   └── formatDate.js
│       │
│       ├── App.jsx
│       ├── App.css
│       └── main.jsx
│
├── db/
│   └── init/
│       └── 01_create_database_and_user.sql
│
├── .env.example
├── .gitignore
├── BLUEPRINT.md
├── DESIGN.md
└── README.md
```

---

# 🗃️ Database Design

BankFlow uses a relational MySQL schema.

### Core Entities

```text
users
roles
user_roles
accounts
beneficiaries
transactions
audit_logs
notifications
```

### Relationships

```text
User
 │
 ├── User Roles ─── Role
 │
 ├── Accounts
 │     │
 │     └── Transactions
 │
 ├── Beneficiaries
 │
 └── Notifications
```

---

# 🔌 API Modules

Base URL:

```text
/api
```

## ❤️ Health

```http
GET /api/health
```

---

## 🔐 Authentication

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

---

## 👤 User

```http
GET    /api/users/me
PUT    /api/users/me
POST   /api/users/me/change-password
```

---

## 🏦 Accounts

```http
POST /api/accounts
GET  /api/accounts
GET  /api/accounts/{accountId}
GET  /api/accounts/number/{accountNumber}
```

---

## 💰 Transactions

```http
POST /api/transactions/deposit
POST /api/transactions/withdraw
POST /api/transactions/transfer

GET /api/transactions/account/{accountId}
GET /api/transactions/{reference}
```

Money-moving requests support an idempotency header:

```http
Idempotency-Key: <unique-request-key>
```

---

## 👥 Beneficiaries

```http
POST   /api/beneficiaries
GET    /api/beneficiaries
POST   /api/beneficiaries/{beneficiaryId}/verify
DELETE /api/beneficiaries/{beneficiaryId}
```

---

## 📄 Statements

```http
GET /api/statements/account/{accountId}
```

Example:

```text
/api/statements/account/1?from=2026-01-01&to=2026-01-31
```

---

## 🔔 Notifications

```http
GET  /api/notifications
GET  /api/notifications/unread-count
POST /api/notifications/mark-all-read
```

---

# 🧑‍💼 Admin APIs

## Users

```http
GET  /api/admin/users
GET  /api/admin/users/{userId}
POST /api/admin/users/{userId}/enable
POST /api/admin/users/{userId}/disable
```

## Accounts

```http
GET  /api/admin/accounts
POST /api/admin/accounts/{accountId}/status
```

## Transactions

```http
GET  /api/admin/transactions
GET  /api/admin/transactions/{reference}
POST /api/admin/transactions/{reference}/clear-flag
```

## Audit Logs

```http
GET /api/admin/audit-logs
```

## Dashboard

```http
GET /api/admin/dashboard
```

---

# 👮 Roles & Permissions

BankFlow defines three primary roles.

| Role | Purpose |
|---|---|
| `ROLE_CUSTOMER` | Normal banking customer |
| `ROLE_ADMIN` | Bank operations/admin staff |
| `ROLE_AUDITOR` | Read-only monitoring and audit access |

### Customer

Can:

- Manage own profile
- Open accounts
- View own accounts
- Deposit
- Withdraw
- Transfer
- Manage beneficiaries
- View transaction history
- Generate statements
- View notifications

Cannot access another customer's account.

### Admin

Can:

- Manage users
- Manage account status
- Monitor transactions
- Review flagged transactions
- View audit logs
- View dashboard information

Admins deliberately do not perform customer deposits, withdrawals or transfers.

### Auditor

Read-only access for:

- Transaction monitoring
- Audit information

No mutation endpoints.

---

# 🎨 Frontend Design System

The frontend follows a banking-oriented enterprise design rather than a generic template.

### Design Principles

- Institutional banking visual language
- No decorative gradients
- No glassmorphism
- No neon effects
- No excessive rounded cards
- No unnecessary illustrations
- Data-focused layouts
- Dense financial tables
- Clear status indicators
- Accessible interactions
- Plain CSS with design tokens

### Financial Formatting

All monetary values use Indian number formatting:

```text
₹1,50,000.00
```

Account numbers and transaction references use monospace formatting.

Example:

```text
ACC0 0000 0001
```

### Accessibility

The UI is designed around:

- WCAG AA contrast
- Keyboard focus states
- Semantic tables
- Proper labels
- Focus-trapped dialogs
- Accessible loading states
- Accessible notifications

### Dark Mode

Dark mode is implemented through CSS design tokens and theme variables.

---

# ⚙️ Environment Profiles

BankFlow uses environment-specific Spring profiles.

| Profile | Database | Cache/Broker | Purpose |
|---|---|---|---|
| `dev` | Local MySQL 8 | Managed Redis + Kafka | Work/development environment |
| `docker` | MySQL in Docker | Redis + Kafka in Docker | Full local infrastructure |
| `local` | H2 | In-memory | Offline fallback |

The important design principle is that infrastructure configuration is externalized through environment variables.

---

# 💻 Local Development

## Prerequisites

```text
JDK 21
Maven 3.9+
Node.js 20+
npm
MySQL 8
Git
```

---

## 1. Clone Repository

```bash
git clone https://github.com/mr-piyushkr/bankflow.git
cd bankflow
```

---

## 2. Configure Environment

Copy:

```bash
cp .env.example .env
```

Configure:

```env
DB_PASSWORD=your_database_password
JWT_SECRET=your_jwt_secret
```

Never commit the real `.env` file.

---

## 3. Create Database

The project includes:

```text
db/init/01_create_database_and_user.sql
```

Run:

```bash
mysql -u root -p < db/init/01_create_database_and_user.sql
```

The script creates:

```text
bankflow
bankflow_test
```

and a dedicated application user:

```text
bankflow_app
```

The application should not connect to MySQL using `root`.

---

## 4. Start Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend:

```text
http://localhost:8080
```

Health endpoint:

```text
http://localhost:8080/api/health
```

---

## 5. Start Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

# 🧪 Testing

The project already contains backend tests for the implemented phases.

### Current Test Areas

- Health endpoint
- Persistence mappings
- JWT service
- Authorization rules
- Authentication service
- Money movement integration
- Database integration

Run:

```bash
cd backend
mvn test
```

The final testing phase will expand this into:

- JUnit 5
- Mockito
- Spring Security Test
- `@WebMvcTest`
- `@SpringBootTest`
- Embedded Kafka
- Concurrency tests
- JaCoCo coverage
- 75% service/security coverage gate

---

# 🔄 Planned Kafka Architecture

The completed architecture will use Kafka topics such as:

```text
transaction.created
transaction.flagged
account.status.changed
user.registered
```

and dead-letter topics:

```text
*.DLT
```

Consumer groups:

```text
audit-group
fraud-group
notification-group
```

The same transaction event can therefore independently trigger:

```text
Transaction
     │
     ▼
Kafka
 ┌───┼───────────────┐
 ▼   ▼               ▼
Audit Fraud       Notification
 │     │               │
 ▼     ▼               ▼
MySQL MySQL          MySQL
```

Events are designed to be processed idempotently because Kafka provides at-least-once delivery semantics.

---

# ⚡ Planned Redis Architecture

Redis will be used for:

```text
Account Cache
User Profile Cache
Transaction Status Cache
Refresh Token Store
Rate Limiting
Fraud Velocity Tracking
```

Planned key examples:

```text
account:{id}
user:profile:{id}
txn:status:{reference}
rl:{userOrIp}:{route}
refresh:{jti}
fraud:velocity:{accountId}
```

---

# 🐳 Docker

Docker support is part of the upcoming infrastructure phase.

The planned Docker Compose environment will contain:

```text
MySQL
Redis
Kafka
Kafka UI
Backend
Frontend
```

The backend and frontend will use multi-stage Docker builds.

---

# 🚀 CI/CD

The planned GitHub Actions pipeline will contain four major jobs.

```text
GitHub Push
     │
     ├── Backend
     │     ├── JDK 21
     │     ├── Maven verify
     │     ├── MySQL service
     │     ├── Redis service
     │     └── JaCoCo
     │
     ├── Frontend
     │     ├── npm ci
     │     ├── lint
     │     └── build
     │
     ├── Images
     │     └── Build + Push to GHCR
     │
     └── Deploy
           └── Render Deploy Hook
```

---

# ☁️ Deployment Roadmap

The planned deployment architecture is:

```text
Frontend
   │
   ▼
Vercel / Netlify

Backend
   │
   ▼
Render

Database
   │
   ▼
Managed MySQL

Cache
   │
   ▼
Redis Cloud

Messaging
   │
   ▼
Confluent Cloud
```

AWS is intentionally not required for the planned deployment.

---

# 🗺️ Development Roadmap

## Phase 1 — Scaffold

```text
✅ Git repository
✅ Maven backend
✅ React/Vite frontend
✅ Environment configuration
✅ MySQL setup
✅ Health endpoint
✅ Frontend design system
```

## Phase 2 — Persistence

```text
✅ Entities
✅ Enums
✅ Repositories
✅ Flyway migrations
✅ Seed roles
```

## Phase 3 — Security

```text
✅ Spring Security
✅ JWT authentication
✅ Registration
✅ Login
✅ Refresh token
✅ Logout
✅ Role-based authorization
✅ Ownership guard
```

## Phase 4 — Banking Operations

```text
✅ Accounts
✅ Savings/current account support
✅ Deposit
✅ Withdrawal
✅ Transfer
✅ Beneficiaries
✅ Transaction history
✅ Statements
✅ Idempotency
✅ Account locking
✅ Daily limits
```

## Phase 5 — Frontend

```text
⬜ Customer login/register
⬜ Customer dashboard
⬜ Account list
⬜ Account details
⬜ Open account
⬜ Deposit UI
⬜ Withdrawal UI
⬜ Transfer UI
⬜ Beneficiary management
⬜ Transaction history
⬜ Transaction details
⬜ Statement UI
⬜ Profile management
⬜ Notifications
⬜ Admin dashboard
⬜ Admin user management
⬜ Admin account management
⬜ Transaction monitoring
⬜ Audit log UI
⬜ Responsive layouts
⬜ Final dark mode integration
```

## Phase 6 — Redis

```text
⬜ Redis connection
⬜ Cache manager
⬜ Account caching
⬜ Profile caching
⬜ Transaction status caching
⬜ Cache invalidation
⬜ Refresh-token storage
⬜ Rate limiting
⬜ Fraud velocity tracking
```

## Phase 7 — Kafka

```text
⬜ Kafka configuration
⬜ Kafka producers
⬜ Transaction events
⬜ User registration events
⬜ Account status events
⬜ Fraud events
⬜ Audit consumer
⬜ Fraud consumer
⬜ Notification consumer
⬜ Consumer idempotency
⬜ Retry handling
⬜ Dead-letter topics
```

## Phase 8 — Testing

```text
⬜ Complete unit test suite
⬜ Web MVC tests
⬜ Security tests
⬜ Integration tests
⬜ Embedded Kafka tests
⬜ Concurrency tests
⬜ Redis tests
⬜ JaCoCo
⬜ 75% coverage gate
```

## Phase 9 — Docker

```text
⬜ Backend Dockerfile
⬜ Frontend Dockerfile
⬜ Docker Compose
⬜ MySQL container
⬜ Redis container
⬜ Kafka container
⬜ Kafka UI
⬜ Health checks
⬜ Multi-stage builds
```

## Phase 10 — CI/CD

```text
⬜ GitHub Actions
⬜ Backend verification
⬜ Frontend lint/build
⬜ Integration services
⬜ JaCoCo reporting
⬜ GHCR image publishing
⬜ Deployment workflow
```

## Phase 11 — Deployment

```text
⬜ Managed MySQL
⬜ Redis Cloud
⬜ Confluent Cloud
⬜ Render backend
⬜ Vercel/Netlify frontend
⬜ Environment variables
⬜ Production CORS
⬜ Production verification
```

## Phase 12 — Documentation

```text
⬜ Swagger/OpenAPI
⬜ ER diagram
⬜ Architecture diagram
⬜ Postman collection
⬜ API documentation
⬜ Deployment documentation
⬜ Final README
⬜ Project demo flow
```

---

# 🔮 Future Enhancements

After the planned 12 phases, possible extensions include:

- OTP-based authentication
- Email notifications
- SMS notification integration
- KYC document workflow
- Beneficiary approval workflow
- Scheduled transfers
- Recurring payments
- Transaction export
- Advanced fraud rules
- Fraud risk scoring
- Admin analytics
- Account statement PDF generation
- Multi-currency support
- Transaction search improvements
- Observability with metrics and tracing
- Centralized logging
- API versioning
- Production monitoring
- Kubernetes deployment

---

# 🎯 Interview Talking Points

BankFlow is designed to demonstrate practical backend engineering concepts.

### Financial Transactions

- Pessimistic row locking
- Deterministic locking order
- Transaction boundaries
- Idempotency
- Balance consistency
- BigDecimal monetary calculations

### Security

- JWT authentication
- BCrypt
- Role-based access control
- Method-level authorization
- Row-level ownership checks
- Refresh-token revocation

### Database

- MySQL
- JPA/Hibernate
- Flyway
- Database constraints
- Indexing
- Transaction isolation
- `SELECT FOR UPDATE`

### Distributed Systems

- Kafka
- Event-driven processing
- At-least-once delivery
- Idempotent consumers
- Dead-letter queues
- Asynchronous fraud processing

### Performance

- Redis caching
- Cache invalidation
- Sliding-window rate limiting
- Database pagination

### DevOps

- Docker
- Docker Compose
- GitHub Actions
- GHCR
- Cloud deployment

---

# 📚 Documentation

Project documentation:

```text
BLUEPRINT.md
```

Contains:

- Architecture
- Infrastructure strategy
- Phase roadmap
- Security model
- Database design
- Redis design
- Kafka design
- Testing strategy
- Docker strategy
- CI/CD strategy
- Deployment strategy

Frontend design rules:

```text
DESIGN.md
```

Contains:

- UI design system
- Typography
- Colors
- Spacing
- Accessibility
- Financial number formatting
- Interaction rules
- Dark mode
- CSS conventions

---

# 🤝 Development Workflow

```bash
git checkout -b feature/your-feature
```

Make changes and verify locally:

```bash
git add .
git commit -m "Add your feature"
git push origin feature/your-feature
```

Keep commits focused and descriptive.

---

# ❤️ Show Your Support

If you find this project useful or interesting:

- ⭐ Star the repository
- 🍴 Fork the project
- 🐛 Report issues
- 💡 Suggest improvements
- 🤝 Contribute
- 📢 Share the project

Every bit of support helps improve the project.

---

# 👨‍💻 Author

## Piyush Kumar

**Full Stack Developer**

Passionate about building scalable backend systems, modern web applications, secure APIs and production-oriented software architectures.

---

# 🔗 Let's Connect

### 🌐 Portfolio

https://my-portfolio-umber-zeta-11.vercel.app/

### 💻 GitHub

https://github.com/mr-piyushkr

### 💼 LinkedIn

https://linkedin.com/in/piyushkumar06

### 📧 Email

0602.piyushkumar@gmail.com

---

# 📄 License

This project is licensed under the **MIT License**.

```text
MIT License

Copyright (c) 2026 Piyush Kumar

```

---

<p align="center">
  <strong>🏦 BankFlow — Building a realistic digital banking platform, one phase at a time.</strong>
</p>

<p align="center">
  Made with ☕ Java, Spring Boot, React and a lot of engineering.
</p>