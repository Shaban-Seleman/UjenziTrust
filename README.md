# Uzenji Trust Core

Production-oriented MVP backend for Tanzania-focused Real Estate + Escrow + Construction Management flows.

## Stack
- Java 21
- Spring Boot 3.x
- PostgreSQL 15+
- Flyway migrations
- Spring Security JWT + RBAC
- OpenAPI (`/swagger-ui/index.html`)
- Testcontainers integration tests

## Run locally
1. Start infrastructure:
```bash
docker compose up -d postgres minio
```
2. Run app:
```bash
mvn spring-boot:run
```
3. Open docs:
- [Swagger UI](http://localhost:8080/swagger-ui/index.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)

## Auth (MVP)
- Seed admin user:
  - email: `admin@uzenji.local`
- Use `/auth/login` to get a JWT.
- Send `Authorization: Bearer <token>` on state-changing endpoints.

## Integration tests
```bash
mvn test
```

Implemented integration scenarios:
- acceptOffer prevents double reservation
- escrow creation idempotent by business_key
- milestone approval creates disbursement + ledger entry
- webhook settlement posts ledger and marks milestone PAID only when all split disbursements settle
- retention release job creates retention disbursement + ledger entry
