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

## Investor demo (seed + end-to-end flow)
1. Ensure local profile is active so passwordless dev login is allowed:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
2. Run the demo flow script:
```bash
chmod +x scripts/run-investor-demo.sh
./scripts/run-investor-demo.sh
```

The script performs a full flow using seeded demo actors:
- owner/seller creates and publishes a property
- buyer submits an offer; seller accepts (reservation + purchase escrow)
- owner creates project, assigns participants, activates project
- owner creates milestone; contractor submits evidence
- owner approves milestone (ledger + disbursement + outbox)
- webhook settlement is simulated with HMAC verification
- milestone transitions to `PAID`; monitoring endpoints are checked (`ops/outbox`, `ops/webhooks/events`, `ledger/journal-entries`)

Seeded actor emails:
- `investor.owner@ujenzi.demo` (`OWNER`, `SELLER`)
- `diaspora.buyer@ujenzi.demo` (`BUYER`)
- `contractor.site@ujenzi.demo` (`CONTRACTOR`)
- `inspector.qa@ujenzi.demo` (`INSPECTOR`)
- `ops.admin@ujenzi.demo` (`ADMIN`)

## Local-only demo seed engine
Warning: this is local-only. It truncates all business data, including `ledger.*`, and must not be enabled outside the `local` profile.

1. Start the app with the local profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
2. Log in as an admin and call the reset + seed endpoint:
```bash
curl -X POST http://localhost:8080/admin/demo/reset-and-seed?scenario=investor_v1 \
  -H "Authorization: Bearer <admin-jwt>"
```

Available endpoints:
- `POST /admin/demo/reset`
- `POST /admin/demo/seed?scenario=investor_v1`
- `POST /admin/demo/reset-and-seed?scenario=investor_v1`

Demo accounts (local profile only):
- `demo-admin@nyumbatrust.local` (`ADMIN`)
- `demo-seller@nyumbatrust.local` (`SELLER`)
- `demo-owner@nyumbatrust.local` (`OWNER`)
- `demo-contractor@nyumbatrust.local` (`CONTRACTOR`)
- `demo-inspector@nyumbatrust.local` (`INSPECTOR`)
- `demo-buyer-1@nyumbatrust.local` through `demo-buyer-5@nyumbatrust.local` (`BUYER`)

Shared demo password (local profile only):
- `Demo123!`
