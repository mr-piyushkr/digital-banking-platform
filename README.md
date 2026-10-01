# 🏦 BankFlow — Digital Banking & Transaction Platform

<p align="center">
  <strong>A full-stack digital banking platform built with Spring Boot, React, MySQL, Redis and Apache Kafka.</strong>
</p>

<p align="center">
  A secure, scalable and transaction-focused banking application featuring customer banking,
  fund transfers, beneficiary management, fraud detection, audit logging, caching,
  event-driven processing and administrative operations.
</p>

<p align="center">

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)

</p>

<p align="center">

![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

</p>

---

## 📖 About The Project

**BankFlow** is a full-stack digital banking and transaction management platform designed to simulate real-world banking workflows and backend engineering practices.

The platform provides customers with secure access to banking services including account management, deposits, withdrawals, fund transfers, beneficiary management, transaction history and account statements.

The platform also provides administrative capabilities for managing users and accounts, monitoring transactions, reviewing suspicious activity and maintaining audit records.

The backend is built using **Java, Spring Boot, Spring Security, Spring Data JPA and Hibernate**, while the frontend uses **React, React Router, Axios and plain CSS3**.

The system uses **MySQL** as the primary relational database, **Redis** for caching, rate limiting and token management, and **Apache Kafka** for asynchronous event processing.

---

# ✨ Key Features

## 🔐 Authentication & Authorization

- Secure user registration
- JWT-based authentication
- Access and refresh token mechanism
- Secure password hashing using BCrypt
- Token refresh support
- Logout and refresh-token revocation
- Role-based access control
- Method-level authorization
- Account ownership validation
- Protected API endpoints
- Separate customer, admin and auditor permissions

---

## 👤 Customer Banking

Customers can:

- Register and authenticate
- Manage their profile
- Change passwords
- Open bank accounts
- View account details
- View account balances
- Manage savings accounts
- Manage current accounts
- Deposit funds
- Withdraw funds
- Transfer funds
- Manage beneficiaries
- View transaction history
- View transaction details
- Generate account statements
- View notifications

---

## 🏦 Account Management

The platform supports:

- Savings accounts
- Current accounts
- Unique account number generation
- Account status management
- Active accounts
- Blocked accounts
- Frozen accounts
- Account balance tracking
- Account ownership validation

---

## 💰 Deposits & Withdrawals

### Deposits

- Secure deposit workflow
- Transaction creation
- Transaction reference generation
- Balance updates
- Idempotency protection
- Transaction status management
- Database transaction boundaries

### Withdrawals

- Secure withdrawal workflow
- Insufficient balance validation
- Account status validation
- Transaction creation
- Idempotency protection
- Balance consistency
- Concurrent withdrawal protection

---

## 💸 Fund Transfers

BankFlow supports secure account-to-account transfers.

Features include:

- Own-account transfers
- Beneficiary transfers
- Beneficiary verification
- Balance validation
- Blocked account validation
- Transaction limits
- Idempotency keys
- Transaction references
- Atomic debit and credit operations
- Pessimistic locking
- Deterministic account lock ordering
- Deadlock prevention

Money transfers are executed inside transactional boundaries to maintain account consistency.

---

## 👥 Beneficiary Management

Customers can:

- Add beneficiaries
- View beneficiaries
- Verify beneficiaries
- Delete beneficiaries
- Assign beneficiary nicknames
- Store bank account information
- Prevent duplicate beneficiaries
- Prevent invalid self-beneficiary configurations

A beneficiary must satisfy the required verification rules before being used for transfers.

---

## 📊 Transaction Management

BankFlow provides detailed transaction management including:

- Transaction references
- Transaction types
- Transaction status
- Transaction amount
- Source account
- Destination account
- Balance after transaction
- Transaction description
- Idempotency key
- Creation timestamp
- Pagination
- Filtering
- Transaction detail lookup

---

## 📄 Account Statements

Customers can generate account statements using date ranges.

Statements include:

- Account information
- Opening balance
- Closing balance
- Total credits
- Total debits
- Transaction count
- Transaction details
- Date-range filtering

Statement data can be generated in downloadable formats such as PDF and CSV.

---

# 🧑‍💼 Administration

BankFlow provides dedicated administrative operations.

### User Management

- View users
- Search users
- Paginate users
- View user details
- Enable users
- Disable users

### Account Management

- View all accounts
- Search accounts
- Filter accounts
- Block accounts
- Unblock accounts
- Freeze accounts
- Monitor account status

### Transaction Monitoring

- View transactions
- Search transactions
- Filter by transaction status
- Filter by amount
- Filter by date
- Filter by account
- Review flagged transactions
- Reverse eligible failed/stuck transactions

### Audit Monitoring

- View audit logs
- Track actor information
- Track actions
- Track affected entities
- Store request IP
- Store user-agent information
- Maintain event history

### Dashboard

The administration dashboard provides information such as:

- Total users
- Total accounts
- Transaction count
- Transaction volume
- Failed transactions
- Flagged transactions

---

# 🕵️ Fraud Detection

BankFlow includes rule-based fraud detection.

Fraud rules include scenarios such as:

- High-value transactions
- Excessive transaction velocity
- Transfers involving newly added beneficiaries
- Suspicious transaction patterns

Transactions meeting configured fraud rules can be marked as:

```text
FLAGGED
```

The flagged transaction can then be reviewed through administrative monitoring.

Redis is used to maintain short-lived transaction velocity information, while Kafka enables asynchronous fraud-event processing.

---

# 📝 Audit Logging

Important banking operations are designed to produce audit information.

Audit records can contain:

- Actor/user
- Action
- Entity type
- Entity ID
- Event details
- IP address
- User-agent
- Timestamp

This provides traceability for security-sensitive operations.

---

# 🔔 Notifications

The notification system supports in-application notifications for important banking events.

Examples include:

- Account events
- Transaction events
- Fraud alerts
- Beneficiary-related events
- Administrative account status changes

Kafka consumers can independently process notification events without blocking the main transaction workflow.

---

# ⚡ Redis

Redis is used as a high-speed supporting data store.

### Redis Responsibilities

- Account caching
- User profile caching
- Transaction status caching
- Refresh-token storage
- Refresh-token revocation
- API rate limiting
- Fraud velocity tracking

### Example Keyspace

```text
account:{id}
user:profile:{id}
txn:status:{reference}
rl:{userOrIp}:{route}
refresh:{jti}
fraud:velocity:{accountId}
```

### Cache Strategy

Account data is cached for frequently accessed operations and invalidated whenever balance-changing operations occur.

---

# 📨 Apache Kafka

Kafka provides the event-driven communication layer.

### Event Topics

```text
transaction.created
transaction.flagged
account.status.changed
user.registered
```

### Consumer Groups

```text
audit-group
fraud-group
notification-group
```

A single event can therefore be processed independently by multiple consumers.

```text
                    ┌─────────────────┐
                    │ Transaction API │
                    └────────┬────────┘
                             │
                             ▼
                     transaction.created
                             │
                             ▼
                       ┌───────────┐
                       │   Kafka   │
                       └─────┬─────┘
                             │
             ┌───────────────┼───────────────┐
             ▼               ▼               ▼
       Audit Consumer   Fraud Consumer   Notification
             │               │               │
             ▼               ▼               ▼
           MySQL           MySQL            MySQL
```

Kafka consumers are designed to support idempotent processing because Kafka provides at-least-once delivery semantics.

---

# 🔒 Security Architecture

BankFlow uses defence-in-depth authorization.

### Security Layers

```text
Request
   │
   ▼
Rate Limit
   │
   ▼
JWT Authentication
   │
   ▼
SecurityFilterChain
   │
   ▼
Controller
   │
   ▼
@PreAuthorize
   │
   ▼
Service Layer
   │
   ▼
Ownership Guard
   │
   ▼
Repository
```

### Security Features

- JWT authentication
- BCrypt password hashing
- Role-based authorization
- Method-level authorization
- Ownership checks
- Protected admin APIs
- Refresh-token revocation
- Least-privilege database user
- Input validation
- Global exception handling
- Secure transaction processing

---

# 🧠 Engineering Practices

## 💵 BigDecimal for Money

Financial values use:

```java
BigDecimal
```

instead of floating-point types.

Database amounts use:

```text
DECIMAL(19,4)
```

This prevents common floating-point precision problems in financial calculations.

---

## 🔁 Idempotency

Money-moving operations support idempotency keys.

Example:

```http
Idempotency-Key: 7f9d3e1a-transaction
```

If a client retries the same request, the system can recognize the previous operation and prevent duplicate financial processing.

---

## 🔐 Pessimistic Locking

Concurrent balance operations use database row locking.

```sql
SELECT ... FOR UPDATE
```

This prevents concurrent requests from incorrectly modifying the same balance.

---

## 🔀 Deterministic Lock Ordering

For transfers involving two accounts, account rows are locked in deterministic order.

This reduces the possibility of deadlocks when two accounts simultaneously transfer money to each other.

---

## 🗃️ Database Migrations

Flyway manages database schema changes.

The application does not rely on Hibernate automatically modifying the database schema.

---

# 🏗️ System Architecture

```text
                         ┌─────────────────────┐
                         │      React 19        │
                         │   Vite + Axios       │
                         └──────────┬──────────┘
                                    │
                              HTTPS / JSON
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │    Spring Boot      │
                         │       Java 21       │
                         └──────────┬──────────┘
                                    │
               ┌────────────────────┼────────────────────┐
               │                    │                    │
               ▼                    ▼                    ▼
        Controllers             Security             Services
               │                    │                    │
               │                 JWT/RBAC         Business Logic
               │                                         │
               └────────────────────┬────────────────────┘
                                    │
                                    ▼
                              Spring Data JPA
                                    │
                                    ▼
                                Hibernate
                                    │
                                    ▼
                                MySQL 8

                  ┌─────────────────┴─────────────────┐
                  │                                   │
                  ▼                                   ▼
                Redis                               Kafka
                  │                                   │
          Cache / Rate Limit              Audit / Fraud / Notification
```

---

# 🛠️ Technology Stack

## ☕ Backend

<p align="center">
<img src="https://skillicons.dev/icons?i=java,spring,maven" />
</p>

| Technology | Purpose |
|---|---|
| Java 21 | Backend development |
| Spring Boot 3.5 | Application framework |
| Spring MVC | REST API development |
| Spring Security | Authentication & authorization |
| JWT | Token-based authentication |
| Spring Data JPA | Persistence abstraction |
| Hibernate | ORM |
| Flyway | Database migration |
| Maven | Build management |
| Lombok | Boilerplate reduction |

---

## ⚛️ Frontend

<p align="center">
<img src="https://skillicons.dev/icons?i=react,vite,js,html,css" />
</p>

| Technology | Purpose |
|---|---|
| React 19 | Frontend framework |
| React Router | Application routing |
| Axios | API communication |
| Vite | Frontend tooling |
| JavaScript | Application development |
| HTML5 | Structure |
| CSS3 | Styling |
| Lucide React | Interface icons |

---

## 🗄️ Database

<p align="center">
<img src="https://skillicons.dev/icons?i=mysql" />
</p>

| Technology | Purpose |
|---|---|
| MySQL 8 | Primary database |
| JPA | Persistence API |
| Hibernate | ORM |
| Flyway | Database migrations |
| H2 | Local fallback/testing |

---

## ⚡ Cache & Performance

<p align="center">
<img src="https://skillicons.dev/icons?i=redis" />
</p>

- Redis
- Spring Cache
- Cache-aside strategy
- Cache invalidation
- Rate limiting
- Refresh-token storage
- Fraud velocity tracking

---

## 📨 Messaging

<p align="center">
<img src="https://skillicons.dev/icons?i=kafka" />
</p>

- Apache Kafka
- Kafka producers
- Kafka consumers
- Consumer groups
- Event-driven processing
- Idempotent consumers
- Retry handling
- Dead-letter topics

---

## 🧪 Testing

<p align="center">
<img src="https://skillicons.dev/icons?i=java" />
</p>

- JUnit 5
- Mockito
- Spring Boot Test
- Spring Security Test
- Web MVC testing
- Integration testing
- Embedded Kafka
- Concurrency testing
- JaCoCo

---

## 🐳 DevOps & CI/CD

<p align="center">
<img src="https://skillicons.dev/icons?i=docker,githubactions,git,github,linux" />
</p>

- Docker
- Docker Compose
- Multi-stage Docker builds
- Git
- GitHub
- GitHub Actions
- GitHub Container Registry
- Linux

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
│       │   │   ├── SecurityConfig.java
│       │   │   ├── CorsConfig.java
│       │   │   ├── JpaAuditingConfig.java
│       │   │   ├── RedisConfig.java
│       │   │   ├── KafkaTopicConfig.java
│       │   │   ├── OpenApiConfig.java
│       │   │   └── AsyncConfig.java
│       │   │
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   ├── UserController.java
│       │   │   ├── AccountController.java
│       │   │   ├── TransactionController.java
│       │   │   ├── BeneficiaryController.java
│       │   │   ├── StatementController.java
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
│       │   │   ├── response/
│       │   │   └── mapper/
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
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   ├── BusinessException.java
│       │   │   ├── ErrorCode.java
│       │   │   ├── ResourceNotFoundException.java
│       │   │   ├── InsufficientBalanceException.java
│       │   │   ├── AccountBlockedException.java
│       │   │   └── DuplicateResourceException.java
│       │   │
│       │   ├── kafka/
│       │   │   ├── producer/
│       │   │   ├── consumer/
│       │   │   └── event/
│       │   │
│       │   ├── redis/
│       │   │   ├── CacheKeys.java
│       │   │   ├── RateLimitService.java
│       │   │   ├── RateLimitFilter.java
│       │   │   ├── RefreshTokenStore.java
│       │   │   └── AccountCacheService.java
│       │   │
│       │   ├── repository/
│       │   │
│       │   ├── security/
│       │   │   ├── JwtService.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   ├── CustomUserDetails.java
│       │   │   ├── CustomUserDetailsService.java
│       │   │   ├── AccountGuard.java
│       │   │   └── RestAccessDeniedHandler.java
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
│       │
│       └── test/
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
│       │   └── adminApi.js
│       │
│       ├── auth/
│       │   ├── AuthContext.jsx
│       │   ├── ProtectedRoute.jsx
│       │   ├── RoleRoute.jsx
│       │   └── useAuth.js
│       │
│       ├── components/
│       │   ├── Navbar
│       │   ├── Sidebar
│       │   ├── Card
│       │   ├── DataTable
│       │   ├── Pagination
│       │   ├── Modal
│       │   ├── AmountInput
│       │   ├── StatusBadge
│       │   ├── Loader
│       │   ├── Toast
│       │   ├── ErrorBoundary
│       │   └── ConfirmDialog
│       │
│       ├── pages/
│       │   ├── auth/
│       │   ├── customer/
│       │   └── admin/
│       │
│       ├── routes/
│       │   └── AppRoutes.jsx
│       │
│       ├── styles/
│       │   ├── tokens.css
│       │   └── global.css
│       │
│       └── utils/
│           ├── formatMoney.js
│           ├── formatDate.js
│           └── validators.js
│
├── db/
│   └── init/
│       └── 01_create_database_and_user.sql
│
├── .github/
│   └── workflows/
│       └── ci.yml
│
├── docker-compose.yml
├── .dockerignore
├── .env.example
├── .gitignore
├── BLUEPRINT.md
├── DESIGN.md
└── README.md
```

---

# 🔌 API Modules

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
GET  /api/users/me
PUT  /api/users/me
POST /api/users/me/change-password
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

## 🧑‍💼 Admin APIs

### Users

```http
GET  /api/admin/users
GET  /api/admin/users/{userId}
POST /api/admin/users/{userId}/enable
POST /api/admin/users/{userId}/disable
```

### Accounts

```http
GET  /api/admin/accounts
POST /api/admin/accounts/{accountId}/status
```

### Transactions

```http
GET  /api/admin/transactions
GET  /api/admin/transactions/{reference}
POST /api/admin/transactions/{reference}/clear-flag
```

### Audit Logs

```http
GET /api/admin/audit-logs
```

### Dashboard

```http
GET /api/admin/dashboard
```

---

# 🗄️ Database Design

BankFlow uses MySQL as the primary relational database.

### Core Tables

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

### User

```text
id
first_name
last_name
email
phone
password_hash
enabled
address
city
state
pincode
kyc_status
created_at
updated_at
```

### Account

```text
id
account_number
user_id
type
status
balance
currency
opened_at
version
```

### Transaction

```text
id
reference
type
status
amount
from_account_id
to_account_id
balance_after
description
idempotency_key
created_at
```

### Beneficiary

```text
id
owner_user_id
beneficiary_account_number
nickname
bank_ifsc
verified
created_at
```

### Audit Log

```text
id
actor_user_id
action
entity_type
entity_id
details
ip_address
user_agent
created_at
```

### Notification

```text
id
user_id
title
message
channel
read_flag
created_at
```

---

# 🧪 Testing Strategy

BankFlow uses multiple testing layers.

### Unit Testing

```text
JUnit 5
Mockito
```

Tests cover:

- Authentication
- Account operations
- Deposits
- Withdrawals
- Transfers
- Idempotency
- Beneficiary rules
- Fraud rules
- Rate limiting

### Web Testing

```text
@WebMvcTest
Spring Security Test
```

Validates:

- Request validation
- Authentication
- Authorization
- HTTP status codes
- API response structures

### Integration Testing

```text
@SpringBootTest
MySQL
Flyway
```

Tests:

- Real database persistence
- Transaction workflows
- Account locking
- Deposit/withdrawal consistency
- Transfer consistency

### Kafka Testing

```text
Embedded Kafka
spring-kafka-test
```

Tests:

- Event publishing
- Consumer processing
- Consumer groups
- Event idempotency
- Post-commit event behavior

### Concurrency Testing

Concurrent transactions are used to verify that multiple withdrawals cannot incorrectly overdraw an account.

---

# 🐳 Docker

The Docker environment contains the complete application infrastructure.

```text
┌─────────────────────────────────────────────┐
│              Docker Compose                 │
│                                             │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐ │
│  │ MySQL   │   │  Redis  │   │  Kafka  │ │
│  └─────────┘   └─────────┘   └─────────┘ │
│                                             │
│  ┌─────────┐   ┌─────────┐                 │
│  │ Backend │   │ Frontend│                 │
│  └─────────┘   └─────────┘                 │
│                                             │
└─────────────────────────────────────────────┘
```

Docker configuration includes:

- Backend multi-stage build
- Frontend build
- MySQL
- Redis
- Kafka
- Kafka UI
- Health checks
- Service dependencies
- Environment-based configuration

---

# 🔄 CI/CD

GitHub Actions automates the project's build and verification workflow.

The CI pipeline includes:

### Backend

```text
JDK 21
Maven
Unit Tests
Integration Tests
MySQL
Redis
JaCoCo
```

### Frontend

```text
Node.js
npm ci
Lint
Production Build
```

### Container Workflow

```text
Build Docker Images
        │
        ▼
GitHub Container Registry
        │
        ▼
Deployment Workflow
```

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

## Clone Repository

```bash
git clone https://github.com/mr-piyushkr/bankflow.git
cd bankflow
```

---

## Environment Configuration

```bash
cp .env.example .env
```

Configure the required environment variables:

```env
DB_PASSWORD=your_database_password
JWT_SECRET=your_jwt_secret
```

Never commit real secrets to Git.

---

## Database Setup

Run the database initialization script:

```bash
mysql -u root -p < db/init/01_create_database_and_user.sql
```

The application uses a dedicated database user instead of the MySQL root account.

---

## Start Backend

```bash
cd backend
mvn spring-boot:run
```

Backend:

```text
http://localhost:8080
```

---

## Start Frontend

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

# 📚 API Documentation

The application provides API documentation through OpenAPI/Swagger.

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

---

# 🧭 Application Flow

```text
User Registration
       │
       ▼
Authentication
       │
       ▼
JWT Access + Refresh Token
       │
       ▼
Open Bank Account
       │
       ▼
Deposit / Withdraw
       │
       ▼
Add & Verify Beneficiary
       │
       ▼
Fund Transfer
       │
       ▼
Transaction Created
       │
       ├───────────────┐
       ▼               ▼
    Audit            Fraud
       │               │
       └───────┬───────┘
               ▼
         Notification
               │
               ▼
        Transaction History
               │
               ▼
         Account Statement
```

---

# 🎨 Frontend Design

BankFlow follows an enterprise banking interface style.

### Design Principles

- Clean banking interface
- Data-focused layouts
- Institutional color palette
- Plain CSS3
- CSS design tokens
- Responsive layouts
- Accessible components
- Consistent spacing
- Clear financial status indicators
- Skeleton loading states
- Confirmation dialogs for destructive operations

### Financial Formatting

Currency values use Indian number formatting:

```text
₹1,50,000.00
```

Account numbers use grouped monospace formatting:

```text
ACC0 0000 0001
```

Dates use human-readable formats rather than raw ISO timestamps.

---

# ♿ Accessibility

The frontend follows accessibility-oriented practices including:

- WCAG AA contrast
- Keyboard navigation
- Visible focus states
- Semantic HTML
- Accessible tables
- Proper form labels
- Focus management
- Accessible dialogs
- Screen-reader-friendly loading states
- Text labels alongside color indicators

---

# 🌙 Dark Mode

The application supports dark mode through centralized CSS design tokens.

Components consume theme variables instead of hardcoded colors, allowing consistent theme behavior throughout the application.

---

# 🔮 Future Enhancements

Potential future improvements include:

- OTP authentication
- Email verification
- SMS notifications
- Advanced KYC workflow
- Scheduled transfers
- Recurring payments
- Advanced fraud scoring
- Machine-learning-based fraud detection
- Multi-currency accounts
- International transfers
- Enhanced analytics
- Advanced reporting
- Centralized observability
- Distributed tracing
- Structured logging
- Kubernetes deployment
- Mobile application
- Additional banking products

---

# 🧠 Why BankFlow?

BankFlow is designed around practical software engineering concepts that appear in real financial applications:

- Secure authentication
- Role-based authorization
- Transaction management
- Database consistency
- Concurrent transaction handling
- Idempotency
- Event-driven architecture
- Distributed caching
- Rate limiting
- Fraud detection
- Audit logging
- Automated testing
- Containerization
- CI/CD
- API documentation

The project focuses on building a system that is not only functional, but also structured around maintainability, security and scalability.

---

# 📁 Documentation

Additional project documentation:

```text
BLUEPRINT.md
DESIGN.md
README.md
```

### BLUEPRINT.md

Contains:

- Architecture decisions
- Infrastructure configuration
- Security model
- Database design
- Redis architecture
- Kafka architecture
- Testing strategy
- Docker configuration
- CI/CD architecture
- Deployment design

### DESIGN.md

Contains:

- UI design system
- Typography
- Colors
- Spacing
- Accessibility
- Financial formatting
- Interaction rules
- Dark mode
- CSS conventions

---

# 🤝 Contributing

Contributions are welcome.

```bash
git checkout -b feature/your-feature
```

Make your changes, test them locally and commit:

```bash
git add .
git commit -m "Add your feature"
git push origin feature/your-feature
```

For major changes, open an issue first to discuss the proposed improvement.

---

# ⭐ Show Your Support

If you find BankFlow useful or interesting:

- ⭐ Star the repository
- 🍴 Fork the repository
- 🐛 Report issues
- 💡 Suggest improvements
- 🤝 Contribute
- 📢 Share the project

---

# 👨‍💻 Author

## Piyush Kumar

**Full Stack Developer**

Building full-stack applications with Java, Spring Boot, React, REST APIs, databases, event-driven systems and modern development practices.

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

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files, to deal in the Software
without restriction, including without limitation the rights to use, copy,
modify, merge, publish, distribute, sublicense, and/or sell copies of the
Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

---

<p align="center">
  <strong>🏦 BankFlow</strong>
</p>

<p align="center">
  Digital Banking & Transaction Platform
</p>

<p align="center">
  Built with Java • Spring Boot • React • MySQL • Redis • Apache Kafka
</p>