# Mini Core Banking

A Spring Boot REST API for a mini core banking system with authentication, account management, transactions, and loan handling. It uses JWT-based security, PostgreSQL for persistence, and exposes OpenAPI/Swagger documentation.

> Status: Actively under development. This README documents the current, detected stack and configuration. Where details aren’t present in the codebase, TODO notes are included for later completion.

## Stack
- Language: Java 17
- Framework: Spring Boot (parent 4.0.1)
- Modules/Starters:
  - spring-boot-starter-webmvc (REST)
  - spring-boot-starter-data-jpa (JPA/Hibernate)
  - spring-boot-starter-security (JWT-based auth via custom filter)
  - spring-boot-starter-validation (Jakarta validation)
  - springdoc-openapi-starter-webmvc-ui (Swagger/OpenAPI)
  - Lombok (annotations)
- Database: PostgreSQL (via JDBC + HikariCP)
- Auth: JWT (jjwt 0.12.3)
- Build/Package Manager: Maven (wrapper included)

## Entry Point
- Main class: `com.banking.MiniCoreBankingApplication`
- Application context path: `/api/v1` (configured in `application.yml`)
- Default port: `8080`

## Requirements
- Java 17 (JDK 17)
- Maven (or just use the included Maven Wrapper `mvnw`/`mvnw.cmd`)
- PostgreSQL running and accessible

## Setup
1. Clone the repository.
2. Ensure PostgreSQL is running and a database is available (default: `banking_db`).
3. Configure environment variables (recommended) or edit `src/main/resources/application.yml`.

### Configuration (application.yml)
The project ships with sensible local defaults in `src/main/resources/application.yml`:
```
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/banking_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update  # Use 'validate' in production
    show-sql: true
server:
  port: 8080
  servlet:
    context-path: /api/v1

jwt:
  secret: ${JWT_SECRET:your-very-secure-secret-key-change-in-production-minimum-256-bits}
  expiration: 3600000
  refresh-expiration: 604800000

springdoc:
  api-docs.path: /api-docs
  swagger-ui.path: /swagger-ui.html

cors:
  allowed-origins: http://localhost:3000
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: "*"
  allow-credentials: true
```

## How to Run
You can run with the Maven wrapper:

- Dev run (hot reload if your IDE supports devtools):
  - Windows: `mvnw.cmd spring-boot:run`
  - Unix-like: `./mvnw spring-boot:run`

- Build a jar:
  - Windows: `mvnw.cmd clean package`
  - Unix-like: `./mvnw clean package`
  - Then run: `java -jar target/mini-core-banking-0.0.1-SNAPSHOT.jar`

Application will start on `http://localhost:8080/api/v1` by default.

## API Docs (Swagger/OpenAPI)
- OpenAPI JSON: `http://localhost:8080/api/v1/api-docs`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`

## Authentication
- Public endpoints (per `SecurityConfig`): `/auth/**`, Swagger (`/swagger-ui/**`, `/api-docs/**`, `/swagger-ui.html`).
- All other endpoints require authentication.
- Admin endpoints require role `ADMIN` (paths under `/admin/**`).

JWT is returned by the login endpoint and must be sent as `Authorization: Bearer <token>`.

### Quick Start (Auth)
Register a user:
```
POST /api/v1/auth/register
Content-Type: application/json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "StrongP@ssw0rd"
}
```

Login:
```
POST /api/v1/auth/login
Content-Type: application/json
{
  "email": "john.doe@example.com",
  "password": "StrongP@ssw0rd"
}
```
Response contains the JWT token (see `LoginResponse`). Use it for subsequent requests.

> TODO: Document how to create or promote an `ADMIN` user for accessing `/admin/**` endpoints (e.g., via DB seeding or an admin creation flow).

## Environment Variables
These can override application.yml values:

- `SPRING_DATASOURCE_URL` – JDBC URL (e.g., `jdbc:postgresql://localhost:5432/banking_db`)
- `SPRING_DATASOURCE_USERNAME` – DB username
- `SPRING_DATASOURCE_PASSWORD` – DB password
- `JWT_SECRET` – secret key for signing JWTs (use a strong, 256-bit+ secret in production)
- `SERVER_PORT` – server port (default 8080)

> Tip: You can also set other Spring Boot config via environment variables using relaxed binding, e.g., `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` for production.

## Database
- Uses PostgreSQL. Default credentials are for local development.
- JPA `ddl-auto: update` is enabled by default for convenience. In production, switch to `validate` and manage schema via migrations.

> TODO: If Flyway or Liquibase is intended, add the dependency and include migration scripts and instructions here.

## Project Structure
```
mini-core-banking/
├─ pom.xml
├─ mvnw, mvnw.cmd
├─ HELP.md
├─ src/
│  ├─ main/
│  │  ├─ java/com/banking/
│  │  │  ├─ MiniCoreBankingApplication.java            # Entry point
│  │  │  ├─ config/                                    # Security, CORS, OpenAPI config
│  │  │  ├─ controller/                                # REST controllers (auth, account, loan, transaction, admin)
│  │  │  ├─ dto/                                       # Request/response DTOs
│  │  │  ├─ entity/                                    # JPA entities (User, Account, Loan, ...)
│  │  │  ├─ exception/                                 # Global exception handler and custom exceptions
│  │  │  ├─ repository/                                # Spring Data JPA repositories
│  │  │  ├─ security/                                  # JWT filter, utilities, UserDetailsService
│  │  │  ├─ service/                                   # Business services
│  │  │  └─ util/                                      # Helpers (generators, calculators)
│  │  └─ resources/
│  │     └─ application.yml                            # App configuration
│  └─ test/
│     └─ java/com/banking/MiniCoreBankingApplicationTests.java
```

## Available Scripts / Commands
Using Maven wrapper:
- `mvnw clean` – clean build
- `mvnw test` – run tests
- `mvnw spring-boot:run` – run the application
- `mvnw package` – build a runnable jar under `target/`

> There are no custom shell scripts in the repo. All lifecycle is through Maven.

## Testing
- Unit/Integration tests run with Maven Surefire:
  - `mvnw test`
- Example test class: `src/test/java/com/banking/MiniCoreBankingApplicationTests.java`

> TODO: Add endpoint-level tests (e.g., with `@WebMvcTest`, `@SpringBootTest`) and database integration tests. Document coverage and how to run them with profiles.

## Security Highlights
- Stateless sessions with JWT authentication
- Spring Security with role-based access (`ROLE_ADMIN` for `/admin/**`)
- Password hashing using BCrypt (`strength = 12`)

## API Surface (High-level)
- `/auth/**` – registration and login
- `/admin/**` – admin-only operations
- `/accounts/**` – account operations
- `/transactions/**` – transactions
- `/loans/**` – loan application, approval/rejection, repayments

> TODO: List detailed endpoints, payloads, and sample responses per controller. Swagger UI provides the authoritative live documentation.

## CORS
Configured via `CorsConfig` and `application.yml` with default allowed origin `http://localhost:3000`. Adjust `cors.allowed-origins` for your frontend origin(s).

## Observability & Logging
- Log levels for security and SQL are elevated in `application.yml` for local debugging.
- Customize under `logging.level.*` as needed.

## Deployment
- Build jar: `mvnw clean package`
- Run: `java -jar target/mini-core-banking-0.0.1-SNAPSHOT.jar`

> TODO: Provide Dockerfile and containerization steps if desired (image build, env wiring, health checks). Consider adding CI/CD workflow and profiles (dev/test/prod).

## License
- No explicit license file detected.

> TODO: Choose and add a `LICENSE` file (e.g., MIT, Apache-2.0). Update `pom.xml` `<licenses>` section accordingly.

---

### Troubleshooting
- Verify DB connectivity and credentials.
- Ensure `JWT_SECRET` is set to a sufficiently strong value in non-local environments.
- Check Swagger UI at `/api/v1/swagger-ui.html` to validate endpoints are available.
