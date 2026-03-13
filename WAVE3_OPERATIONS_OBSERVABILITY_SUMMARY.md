# Wave 3 – Operations & Observability Summary

## What Was Added

### Backend

- Admin disbursement monitoring APIs
  - `GET /ops/disbursements`
  - `GET /ops/disbursements/{disbursementId}`
- Outbox investigation APIs
  - Reused `GET /ops/outbox` with filtering
  - Added `GET /ops/outbox/{eventId}`
  - Added `POST /ops/outbox/{eventId}/retry`
- Webhook investigation APIs
  - Reused `GET /ops/webhooks/events` with filtering
  - Added `GET /ops/webhooks/events/{eventId}`
- Ledger drill-down APIs
  - Reused `GET /ledger/journal-entries` with filtering
  - Added `GET /ledger/journal-entries/{journalEntryId}`
- System health API
  - Added `GET /ops/system-health`
- Scheduler visibility support
  - Added in-memory job execution tracking for:
    - offer expiry job
    - reservation expiry job
    - retention release job
    - outbox dispatcher
- Production-safe docs hardening
  - Added `app.docs.enabled`
  - Swagger/OpenAPI now disabled by default
  - Local profile enables docs
  - `OpenApiConfig` is conditional on docs flag
  - security permit list only includes Swagger endpoints when docs are enabled

### Frontend

- `/admin` remains the canonical admin console
- Expanded admin console sections:
  - Disbursements
  - Outbox
  - Webhooks
  - Ledger
  - System Health
- Added detail dialogs for:
  - disbursement detail
  - outbox event detail
  - webhook event detail
  - ledger entry detail
- Added outbox retry confirmation flow for failed events
- Added filter controls for the new admin read models

## Backend Endpoints Added or Reused

### Reused and extended

- `GET /ops/outbox`
- `GET /ops/webhooks/events`
- `GET /ledger/journal-entries`

### Added

- `GET /ops/disbursements`
- `GET /ops/disbursements/{disbursementId}`
- `GET /ops/outbox/{eventId}`
- `POST /ops/outbox/{eventId}/retry`
- `GET /ops/webhooks/events/{eventId}`
- `GET /ledger/journal-entries/{journalEntryId}`
- `GET /ops/system-health`

## Admin Pages / Sections Added

All additions are inside the existing admin console at `/admin`.

- System snapshot panel
  - overall health
  - database health
  - pending outbox count
  - failed outbox count
- Disbursement monitoring tab
  - filters by status and payee type
  - detail drill-down
- Outbox monitoring tab
  - filters by status and event type
  - payload inspection
  - retry action for failed events
- Webhook monitoring tab
  - filters by provider, status, and event type
  - payload inspection
  - related disbursement / escrow / milestone references
- Ledger monitoring tab
  - filters by entry type and reference id
  - journal line drill-down
  - hash-chain metadata visibility
- System health tab
  - job schedules
  - last success / last error
  - scheduling enabled flag
  - docs enabled flag

## Operator Actions Added

### Safe outbox retry

- Action: `POST /ops/outbox/{eventId}/retry`
- Scope: `ADMIN` only
- Eligibility: failed outbox events only
- Safety model:
  - event is re-queued, not duplicated
  - downstream idempotency remains in force
  - operator action is logged server-side
- Frontend includes explicit confirmation before retry

## Important Backend Implementation Notes

- Outbox dispatcher now transitions terminally failing events to `FAILED` after max automatic retry attempts
- Manual retry resets eligible outbox events to `PENDING` with `nextAttemptAt=now`
- New admin read models are DTO-based, not raw entity exposure
- Webhook detail responses do not expose stored signature values
- Ledger detail includes journal lines and hash-chain metadata without requiring DB access

## Verification

### Completed

- `mvn -q -DskipTests test-compile`
- `mvn -q -Dtest=DocsExposureConfigTest test`
- `npm run typecheck`
- `npm run build`

### Blocked in this environment

- `OpsObservabilityIntegrationTest` could not execute because the repository uses Testcontainers and Docker socket access is unavailable in this sandbox

## Intentionally Deferred

- Webhook reprocess endpoint
  - read-only investigation was implemented
  - replay/reprocess was deferred because safe replay semantics should be designed explicitly, not guessed
- Richer audit persistence for operator actions
  - current implementation logs operator retries
  - no new audit table or event stream was introduced in this wave
- Dedicated admin routes beyond `/admin`
  - the admin console remains a single coherent surface rather than fragmenting into multiple pages

## Deferred / Future Ops Enhancements

- Webhook replay / reprocess flow with explicit idempotent semantics
- Dedicated disbursement failure remediation tooling
- Ledger account balance and hash-chain verification views
- Prometheus / Grafana metrics integration
- Richer scheduler telemetry persisted beyond in-memory process lifetime
- Role-specific finance / operations consoles if non-admin operator roles are introduced
