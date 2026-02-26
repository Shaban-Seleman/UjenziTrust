# AGENTS.md

## Project Overview

This repository implements a production-grade Real Estate + Escrow + Construction Management SaaS platform for Tanzania & Diaspora investors.

This platform handles real money.
Financial correctness, immutability, idempotency, and authorization integrity are non-negotiable.

---

# 🔒 NON-NEGOTIABLE SYSTEM RULES

## 1) Ledger is Append-Only

- NEVER update or delete rows in:
  - ledger.journal_entries
  - ledger.journal_lines
- Corrections must be done via reversal entries.
- Every entry must be balanced (DR == CR).
- Every entry must be idempotent.
- Every entry must participate in the hash-chain.

Any UPDATE/DELETE on ledger rows is a critical bug.

---

## 2) All Money Movements Go Through LedgerPostingService

No service may:

- Directly modify escrow balances
- Directly modify payable balances
- Directly modify account balances

All financial changes must:

- Generate a PostJournalCommand
- Be posted via LedgerPostingService
- Be wrapped in a transaction

---

## 3) Idempotency is Mandatory (DB-enforced)

Every external or state-changing action must be idempotent using DB unique constraints.

Must enforce uniqueness for:

- ops.escrows.business_key
- ops.disbursement_orders.business_key
- ops.outbox_events.idempotency_key
- ops.webhook_events.event_id
- build.milestone_payout_splits.business_key

Services must handle duplicate-key violations safely and return the existing record.

Never rely on application-level dedupe only.

---

## 4) Outbox Pattern is Mandatory

- No direct bank calls inside business transactions.
- Disbursement creation must:
  1. Insert disbursement row
  2. Insert outbox event
  3. Commit transaction
- Dispatcher processes outbox asynchronously with retries.
- Webhook updates state and posts settlement ledger entry.

---

## 5) Pessimistic Locks Required

Must use DB-level pessimistic locks for:

- Accept offer (lock property row)
- Milestone approval
- Retention release
- Settlement processing
- Escrow release
- Reservation cancellation

Avoid race conditions at DB level.

---

# 🔐 SECURITY RULES (NON-NEGOTIABLE)

## 6) Every State Change Requires an Authenticated Actor

- Every state-changing service/orchestrator must receive actorUserId.
- actorUserId MUST come from SecurityContext (JWT), not from request body.
- Never trust userId fields in JSON payload.

Missing actor identity propagation is a critical bug.

---

## 7) Authorization Must Be Enforced in the Service Layer

Controllers may do basic checks, but the service/orchestrator is the source of truth.

Required role + ownership rules:

### Marketplace

- Publish property → OWNER/SELLER and must own the property
- Submit/withdraw offer → BUYER and must be the buyer
- Accept/reject/cancel reservation → SELLER and must own the property / be offer seller

### Construction

- Create/activate project → OWNER and must be project owner
- Submit milestone evidence → CONTRACTOR and must be assigned contractor
- Approve milestone → OWNER and must be project owner
- Schedule/complete inspection → INSPECTOR and must be assigned inspector
- Retention release → OWNER and must be project owner

### Admin

- Media moderation / overrides → ADMIN only

Missing role/ownership check is a critical bug.

---

## 8) No Cross-User Access

A user must never be able to:

- access another user’s escrow
- approve another user’s milestone
- withdraw/accept another user’s offer
- view private documents/media without authorization

Ownership checks must be performed using DB relationships, not client-submitted IDs.

---

## 9) Webhook Endpoints Are Public but Must Be Verified

Webhook processing must:

- Verify HMAC signature
- Enforce replay timestamp window
- Dedupe event_id (persist webhook_events first)
- Log correlationId + event_id
- Never allow disabling verification in production profile

Never trust webhook payloads blindly.

---

# 📦 MODULE BOUNDARIES

## ledger

Owns: accounts, journal_entries, journal_lines, hash-chain, posting templates  
Must not depend on: market, build, ops

## ops

Owns: escrows, disbursements, outbox, webhook events, bank adapter  
Can depend on: ledger  
Must not depend on: market, build

## market

Owns: properties, offers, offer_events, reservations, media  
Can depend on: ops + common  
Must not depend on: ledger directly

## build

Owns: projects, milestones, inspections, splits, retention scheduling  
Can depend on: ops + ledger (posting factory)  
Must not depend on: market

## users

Owns: users, roles, auth endpoints, token utilities  
Used by: all modules (via common/security)

## common

Owns: errors, idempotency helpers, security utils, canonical JSON, time provider  
No business logic

---

# 💰 FINANCIAL POSTING RULES

## Standard account codes

1010 Bank Clearing (ASSET)  
2010 Buyer Escrow Liability (LIABILITY)  
2030 Payable to Contractor (LIABILITY)  
2040 Payable to Inspector (LIABILITY)  
2050 Payable to Buyer (LIABILITY)  
2060 Platform Fees Holding (LIABILITY)  
2080 Retention Payable (LIABILITY)  
2090 Payable to Supplier (LIABILITY)  
5020 Penalty Income (INCOME)

Do not rename account codes without migration.

---

## Milestone payout logic

Gross = G  
Retention = R  
Splits sum = G - R

Multi-party rule:
Sum(splits.amount) + retention == gross

Milestone becomes PAID only when:

- ALL related disbursement_orders are SETTLED.

---

## Retention release

Allowed only when:

- milestone.status = PAID
- now() >= retention_release_at

Posting:
DR 2080
CR 2030
then payout settlement:
DR 2030
CR 1010

---

# 🧾 WEBHOOK RULES

Webhook must:

- Verify signature + replay window
- Dedupe event_id
- Post settlement ledger entry (DR payableAccountCode, CR 1010)
- Update disbursement SETTLED
- If milestone_id present:
  - mark PAID only when all milestone disbursements settled
  - set paid_at
  - set retention_release_at

---

# 🧪 TESTING REQUIREMENTS

Minimum integration tests:

1. Double reservation prevented.
2. Escrow idempotent by business_key.
3. Milestone approval creates ledger + disbursement(s) + outbox.
4. Webhook settlement posts ledger + marks milestone PAID when all splits settled.
5. Retention release job works from retention_release_at.

Use Testcontainers for Postgres.

---

# 🛑 PROHIBITED ACTIONS

- Direct SQL money updates bypassing services.
- Removing unique constraints “to make tests pass”.
- Disabling idempotency.
- Skipping hash-chain validation.
- Shipping endpoints without service-level authorization checks.

---

# 📡 API ERROR STANDARD

Error format:

{
"Type": "...",
"Title": "...",
"Status": 400,
"Detail": "Human readable explanation",
"Instance": "...",
"Extensions": { ... }
}

Use capital "Detail".

---

# 🧠 DESIGN PHILOSOPHY

This system is:

- ledger-first
- idempotent by design
- append-only
- outbox-driven
- race-condition safe
- audit-friendly
- authorization-safe

When unsure:
Favor safety over convenience.
Favor DB guarantees over in-memory assumptions.
