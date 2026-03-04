# java-faces-quick-start

A production-ready **Jakarta EE 10 kickstarter** for building Java Faces applications. Ships with a complete **login system** out-of-the-box — registration, e-mail verification, password reset — so you can focus on building your features instead of your auth plumbing.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Architecture — MVSC](#architecture--mvsc)
3. [Project Structure](#project-structure)
4. [Login System Features](#login-system-features)
5. [Prerequisites](#prerequisites)
6. [Local Development](#local-development)
7. [Environment Variables (`.env`)](#environment-variables-env)
8. [Running on WildFly (local)](#running-on-wildfly-local)
9. [Running Unit Tests](#running-unit-tests)
10. [CI/CD — Production Deployment](#cicd--production-deployment)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Jakarta EE API | **Jakarta EE 10** (JSF 4.0, CDI 4, JPA 3.1) |
| UI Components | **PrimeFaces 13.0.10** (`jakarta` classifier) |
| ORM | **Hibernate 6.4** (bundled in WAR) |
| Database | **MySQL 8** |
| Password hashing | **jBCrypt 0.4** (BCrypt work factor 12) |
| Scheduled cleanup | **Quartz 2.3.2** |
| Config secrets | **dotenv-java** (`.env` file) |
| Server | **WildFly 30+** |
| Build | **Maven 3.9+ / Java 17** |

---

## Architecture — MVSC

This project follows an **MVSC** pattern (Model – Service – View – Controller), a clean-separation variant of the classic MVC adapted for JSF/CDI applications:

```
┌─────────────────────────────────────────────────────────────────┐
│  Browser  ──►  View (XHTML/PrimeFaces)                         │
│                   │                                             │
│                   ▼                                             │
│            Controller  (JSF CDI beans)                         │
│           @RequestScoped / @SessionScoped                       │
│                   │                                             │
│                   ▼                                             │
│            Service  (business logic)                            │
│           @ApplicationScoped, interfaces + impl                 │
│                   │                                             │
│                   ▼                                             │
│            Repository  (JPA / JDBC)                             │
│           @ApplicationScoped, RESOURCE_LOCAL                    │
│                   │                                             │
│                   ▼                                             │
│            Model  (JPA @Entity classes)                         │
│            User · VerificationToken · PasswordResetToken        │
└─────────────────────────────────────────────────────────────────┘
```

| Layer | Responsibility | Key classes |
|---|---|---|
| **Model** | JPA entities — no logic | `User`, `VerificationToken`, `PasswordResetToken` |
| **Service** | Business rules, BCrypt, SMTP | `UserService` / `UserServiceImpl`, `EmailServiceImpl` |
| **View** | PrimeFaces XHTML pages | `login.xhtml`, `register.xhtml`, `dashboard/`, … |
| **Controller** | Binds View ↔ Service; manages navigation and session | `LoginController`, `RegisterController`, `PasswordResetController`, `VerifyEmailController`, `DashboardController` |

**Why MVSC instead of plain MVC?**  
The dedicated **Service** layer decouples business logic from JSF lifecycle concerns. Controllers stay thin (validate → delegate → navigate), making every layer independently unit-testable with plain Mockito — no running server required.

---

## Project Structure

```
java-faces-quick-start/
├── .env.example                          ← copy to .env and fill in your secrets
├── .github/
│   └── workflows/
│       └── ci-cd.yml                     ← Build → E2E → SSH Deploy pipeline
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/facesapp/
    │   │   ├── batch/
    │   │   │   └── UnverifiedUserCleanupJob.java   ← Quartz job (daily cleanup)
    │   │   ├── config/
    │   │   │   ├── EnvConfig.java                  ← CDI bean: .env → typed getters
    │   │   │   └── QuartzSchedulerListener.java    ← @WebListener: starts/stops Quartz
    │   │   ├── controller/
    │   │   │   ├── LoginController.java
    │   │   │   ├── RegisterController.java
    │   │   │   ├── PasswordResetController.java
    │   │   │   ├── VerifyEmailController.java
    │   │   │   └── DashboardController.java
    │   │   ├── model/
    │   │   │   ├── User.java
    │   │   │   ├── VerificationToken.java
    │   │   │   └── PasswordResetToken.java
    │   │   ├── repository/
    │   │   │   └── UserRepository.java             ← JPA RESOURCE_LOCAL, all DML
    │   │   ├── service/
    │   │   │   ├── UserService.java                ← interface
    │   │   │   ├── UserServiceImpl.java
    │   │   │   ├── EmailService.java               ← interface
    │   │   │   ├── EmailServiceImpl.java
    │   │   │   └── AuthException.java
    │   │   └── util/
    │   │       ├── AuthFilter.java                 ← @WebFilter: guards /dashboard/*
    │   │       └── PasswordUtil.java               ← BCrypt hash / verify
    │   ├── resources/
    │   │   └── META-INF/persistence.xml            ← JPA unit "FacesAppPU"
    │   └── webapp/
    │       ├── login.xhtml
    │       ├── register.xhtml
    │       ├── forgot-password.xhtml
    │       ├── reset-password.xhtml
    │       ├── verify-email.xhtml
    │       ├── dashboard/
    │       └── WEB-INF/
    │           ├── web.xml
    │           ├── faces-config.xml
    │           ├── beans.xml
    │           ├── jboss-deployment-structure.xml  ← isolates bundled Hibernate
    │           └── templates/layout.xhtml          ← PrimeFaces master template
    └── test/
        └── java/com/example/facesapp/
            ├── unit/
            │   ├── UserServiceImplTest.java        ← 17 Mockito unit tests
            │   └── PasswordUtilTest.java
            └── e2e/
                ├── LoginEmailRequiredE2ETest.java
                └── LoginNoEmailVerificationE2ETest.java
```

---

## Login System Features

| Feature | Detail |
|---|---|
| Registration | Name + e-mail + password; duplicate e-mail rejected |
| E-mail verification | UUID token, 24 h TTL; gate-kept by `EMAIL_VERIFICATION_REQUIRED` flag |
| Login | BCrypt check; blocks unverified users only when verification is required |
| Password reset | 1 h single-use token sent by e-mail; same response whether address exists or not (prevents e-mail enumeration) |
| Session guard | `AuthFilter` redirects unauthenticated `/dashboard/*` requests to `/login.xhtml` |
| Stale account cleanup | Quartz job runs daily at 03:00 — deletes unverified accounts older than 24 h in FK order |

---

## Prerequisites

- **Java 17** (Temurin or OpenJDK)
- **Maven 3.9+**
- **MySQL 8** (local or Docker)
- **WildFly 30+** — [download](https://www.wildfly.org/downloads/)

---

## Local Development

### 1. Clone and configure

```bash
git clone https://github.com/vitorhugo-java/java-faces-quick-start.git
cd java-faces-quick-start
cp .env.example .env
```

Open `.env` and fill in your values (see [Environment Variables](#environment-variables-env) below).

### 2. Create the database

```sql
CREATE DATABASE facesapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'facesapp_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON facesapp.* TO 'facesapp_user'@'localhost';
FLUSH PRIVILEGES;
```

> Hibernate's `hbm2ddl.auto=update` creates all tables automatically on first startup.

### 3. Build the WAR

```bash
mvn clean package -DskipTests
# → target/facesapp.war
```

---

## Environment Variables (`.env`)

Copy `.env.example` to `.env` at the root of the project. The file is **git-ignored**; never commit real credentials.

```dotenv
# ─── Database ─────────────────────────────────────────────────────────────────
DB_URL=jdbc:mysql://localhost:3306/facesapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=facesapp_user
DB_PASSWORD=change_me_strong_password
DB_POOL_SIZE=10

# ─── Application ──────────────────────────────────────────────────────────────
APP_BASE_URL=http://localhost:8080/facesapp
APP_SECRET_KEY=change_me_64_char_secret_key_for_tokens_here_1234567890abcdef

# ─── Email (SMTP) ─────────────────────────────────────────────────────────────
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=no-reply@yourdomain.com
MAIL_STARTTLS_ENABLE=true

# ─── Email Verification ───────────────────────────────────────────────────────
# true  → user must verify e-mail within 24 h before first login
# false → account is active immediately after registration
EMAIL_VERIFICATION_REQUIRED=true

# ─── Cleanup Scheduler (Quartz cron: second minute hour dom month dow [year]) ─
# Default: every day at 03:00:00
CLEANUP_CRON=0 0 3 * * ?
```

### Variable Reference

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_URL` | ✅ | — | Full JDBC connection URL |
| `DB_USERNAME` | ✅ | — | MySQL user |
| `DB_PASSWORD` | ✅ | — | MySQL password |
| `DB_POOL_SIZE` | ❌ | `10` | Connection pool size |
| `APP_BASE_URL` | ✅ | `http://localhost:8080/facesapp` | Public URL used in verification/reset e-mails |
| `APP_SECRET_KEY` | ✅ | — | Secret used for token signing (min 32 chars) |
| `MAIL_HOST` | ✅ | `smtp.gmail.com` | SMTP server host |
| `MAIL_PORT` | ❌ | `587` | SMTP port (587 = STARTTLS, 465 = SSL) |
| `MAIL_USERNAME` | ✅ | — | SMTP authentication user |
| `MAIL_PASSWORD` | ✅ | — | SMTP password / app password |
| `MAIL_FROM` | ❌ | `no-reply@example.com` | Sender address shown in e-mails |
| `MAIL_STARTTLS_ENABLE` | ❌ | `true` | Enable STARTTLS for SMTP |
| `EMAIL_VERIFICATION_REQUIRED` | ❌ | `true` | Require e-mail verification before login |
| `CLEANUP_CRON` | ❌ | `0 0 3 * * ?` | Quartz cron for stale-account cleanup |

> **Gmail tip:** Use an [App Password](https://support.google.com/accounts/answer/185833) when 2-Step Verification is enabled.

---

## Running on WildFly (local)

```bash
# Start WildFly in standalone mode
$WILDFLY_HOME/bin/standalone.sh

# Build and copy the WAR to WildFly's deployment scanner directory
mvn clean package -DskipTests
cp target/facesapp.war $WILDFLY_HOME/standalone/deployments/

# WildFly deploys automatically; watch for:
#   .../deployments/facesapp.war.deployed
```

Open your browser at: **http://localhost:8080/facesapp**

> **Note:** Place `.env` in the directory from which WildFly is launched (usually `$WILDFLY_HOME`) or set the variables as OS environment variables. dotenv-java silently falls back to the OS environment when the file is not found.

---

## Running Unit Tests

```bash
# Run all 17 unit tests (no server or DB required)
mvn test
```

```bash
# Run E2E tests (requires a running WildFly + APP_BASE_URL set)
mvn verify -Pe2e
```

---

## CI/CD — Production Deployment

The pipeline defined in `.github/workflows/ci-cd.yml` has three sequential jobs:

```
push to main
     │
     ▼
┌──────────────────────────┐
│  1. Build & Unit Tests   │  ← mandatory gate (MySQL service container)
│     mvn clean verify     │
└──────────┬───────────────┘
           │  passes
     ┌─────┴──────┐
     ▼            ▼
┌──────────┐  ┌──────────────────────────┐
│ 2. E2E   │  │  3. Deploy to WildFly    │
│  Tests   │  │     (SSH hot-deploy)     │
│(optional)│  └──────────────────────────┘
└──────────┘
```

### Job 1 — Build & Unit Tests

- Spins up a **MySQL 8 service container** automatically
- Creates a minimal `.env` from GitHub Actions environment
- Runs `mvn clean verify`
- Uploads `facesapp.war` and Surefire reports as artifacts

### Job 2 — E2E Tests _(optional, `continue-on-error: true`)_

- Runs only on pushes to `main`
- Uses **Selenium + WebDriverManager** with headless Chrome
- Reads `APP_BASE_URL` from the `E2E_APP_URL` secret
- A failure here does **not** block the deployment

### Job 3 — SSH Deploy

Requires these **GitHub Secrets** to be configured in your repository (Settings → Secrets and variables → Actions):

| Secret | Description |
|---|---|
| `DEPLOY_SSH_PRIVATE_KEY` | Private key for SSH access to the production server |
| `DEPLOY_HOST` | Hostname or IP of the production server |
| `DEPLOY_USER` | SSH username on the production server |
| `E2E_APP_URL` | _(optional)_ Full URL to the running app, used by E2E tests |

The deploy step:
1. `scp` the WAR to `/tmp/facesapp.war` on the remote server
2. SSH in and atomically swap the WAR into WildFly's deployment directory
3. Polls for a `.deployed` or `.failed` marker (up to 150 s) and exits accordingly

### GitHub Environments

The `deploy` job uses the `production` **GitHub Environment**, which lets you add manual-approval gates or branch protection rules (Settings → Environments → production).

### Triggering the pipeline

| Event | Runs Build+Test | Runs E2E | Runs Deploy |
|---|---|---|---|
| Push to `main` | ✅ | ✅ | ✅ |
| Push to `develop` | ✅ | ❌ | ❌ |
| Pull request → `main` | ✅ | ❌ | ❌ |

---

## License

[MIT](LICENSE)
