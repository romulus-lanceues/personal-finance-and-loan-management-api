# Personal Finance & Loan Management API

A RESTful backend API for tracking personal finances and loan amortization schedules.
Built with Java and Spring Boot, targeting the banking and fintech sector.

> **This project is currently under active development.**

---
## Tech Stack
- **Java 21**
- **Spring Boot 4.1.0**
- **Spring Security** — JWT authentication with refresh tokens
- **Spring Data JPA** — ORM and database access
- **PostgreSQL** — Primary database
- **Redis** — Caching layer
- **Docker / Docker Compose** — Containerized local development
- **Flyway** — Database migrations

---


## What It Does?
A personal financial ledger that lets users track their bank accounts, wallets,
and cash in one place. Users can record deposits, withdrawals, and transfers
between accounts, and view a monthly spending summary grouped by category.

No real money moves — this is a record-keeping system that models how real
banking applications work under the hood.

---


## Current Features

### Authentication
- User registration and login with JWT access tokens and refresh tokens

### Accounts
- Create and manage multiple named accounts (Savings, Checking, Cash)
- Multi-currency support
- Soft close and soft delete — financial records are never permanently removed


### Transactions
- Record deposits, withdrawals, and transfers between accounts
- Idempotency key support to prevent duplicate transaction processing
- Transfer creates two ledger entries — debit and credit — linked by a shared transfer reference
- Look up transactions by ID or reference number
- Filter transactions by account
- Monthly spending summary 

---

## Project Structure

```text
src
└── main
    └── java
        └── com
            └── financeapi
                ├── controller/      # REST API controllers (HTTP endpoints)
                ├── service/         # Business logic and application services
                ├── repository/      # Spring Data JPA repositories
                ├── entity/          # JPA entity classes (database models)
                ├── dto/
                │   ├── request/     # Request DTOs received from clients
                │   └── response/    # Response DTOs returned to clients
                ├── enums/           # Application enums
                ├── security/        # Custom UserDetails
                ├── exception/       # Custom exceptions and global exception handling
                └── config/          # Application configuration (Spring Security and RSAKeyConfig)
```

---

### Environment Variables

Create environmental variables for the ff:

- DB_URL (The database URL)
- DB_USERNAME (The database username)
- DB_PASSWORD (The database password)
- JWT_PRIVATE_KEY (For JWT token private key)
- JWT_PUBLIC_KEY (For JWT toke public key)

---
- [ ] Loan creation and amortization schedule generation
- [ ] Loan payment tracking with automatic transaction recording
- [ ] Early repayment simulation
- [ ] Loan comparison
- [ ] Redis caching on spending summaries
- [ ] Audit logging via Spring AOP
- [ ] Unit tests with JUnit 5 and Mockito
- [ ] Swagger UI documentation
- [ ] Deployment to Railway or AWS

## Author

**Lance Jade A. Buela** — Aspiring Backend Developer
Targeting junior backend roles in banking and fintech.

[GitHub](https://github.com/romulus-lanceues) · [LinkedIn](https://www.linkedin.com/in/lance-b-8a4b473a7/)