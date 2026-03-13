# Operator Audit Persistence Summary

## What Was Added

NyumbaTrust now persists sensitive operator and admin actions in a structured audit trail.

Implemented components:

- Persistent audit table in `ops.operator_audit_events`
- Reusable backend audit writer service
- Audit integration for selected operator/admin workflows
- Admin-only read APIs for audit investigation
- Admin console audit tab with filters and detail view

## Schema / Table Added

Flyway migration:

- `src/main/resources/db/migration/V12__operator_audit_events.sql`

Table:

- `ops.operator_audit_events`

Stored fields:

- `id`
- `actor_user_id`
- `actor_roles`
- `action_type`
- `resource_type`
- `resource_id`
- `outcome`
- `reason`
- `correlation_id`
- `request_path`
- `request_method`
- `metadata`
- `error_detail`
- `created_at`

Indexes added:

- actor + newest-first lookup
- resource lookup
- action type lookup
- outcome lookup

## Backend Service

Primary service:

- `src/main/java/com/uzenjitrust/ops/service/OperatorAuditService.java`

Service API:

- `recordSuccess(...)`
- `recordNoop(...)`
- `recordFailure(...)`
- `recordForbidden(...)`
- `list(...)`
- `get(...)`

Implementation notes:

- Writes run in `REQUIRES_NEW`
- Audit persistence never fail-closes the main business flow
- Correlation ID is captured from MDC when present
- Request path and method are captured from request context when present
- Current actor and roles are resolved from security context when available

## Actions Audited

Integrated action types:

- `OUTBOX_EVENT_RETRY`
- `DEMO_RESET`
- `DEMO_SEED`
- `DEMO_RESET_AND_SEED`
- `PROJECT_ASSIGN_PARTICIPANTS`
- `PROJECT_ACTIVATED`
- `MILESTONE_CREATED`
- `MILESTONE_APPROVE_MULTI`
- `MILESTONE_RETENTION_RELEASE`

Special audit action types used for investigation:

- `OPERATOR_ACTION_FAILED`
- `ADMIN_FORBIDDEN_ACCESS_ATTEMPT`

## Backend Endpoints Added / Reused

Added in existing ops controller:

- `GET /ops/operator-audit-events`
- `GET /ops/operator-audit-events/{auditEventId}`

Reused audited operator endpoints:

- `POST /ops/outbox/{eventId}/retry`
- `POST /admin/demo/reset`
- `POST /admin/demo/seed`
- `POST /admin/demo/reset-and-seed`
- existing project / milestone / retention operator flows in build services and orchestrators

Authorization:

- audit read endpoints are ADMIN-only
- forbidden retry attempts are persisted as audit events

## Frontend Admin UI Added

Primary UI:

- `apps/web/components/admin/AdminMonitoringDashboard.tsx`

New admin section:

- `Operator Audit` tab in the existing `/admin` console

Frontend support added:

- `apps/web/lib/api/endpoints.ts`
- `apps/web/lib/api/schemas.ts`

UI capabilities:

- audit event table
- filters by action type and outcome
- detail dialog with metadata, reason, error detail, correlation ID, request path, and request method
- loading, empty, and error states

## Privacy / Sanitization Decisions

Sanitization rules are enforced in the audit writer:

- do not persist tokens
- do not persist passwords
- do not persist secrets
- do not persist authorization headers
- do not persist signatures

Metadata is sanitized recursively and long text values are truncated.

This wave does not store raw sensitive request bodies or bank secrets.

## Validation Status

Validated successfully:

- `mvn -q -DskipTests test-compile`
- `npm run build`
- `npm run typecheck`

Test coverage added:

- `src/test/java/com/uzenjitrust/integration/OperatorAuditIntegrationTest.java`

Current execution limitation in this environment:

- `mvn -Dtest=OperatorAuditIntegrationTest test` is blocked in this sandbox because Testcontainers cannot access Docker here
- the test should be executed in a Docker-enabled local environment

## Deferred Future Audit Enhancements

- append-only signed audit chain for tamper evidence
- before/after diffs for more admin actions
- date-range and actor filters in the frontend
- export/search enhancements
- SIEM forwarding or external audit sink integration
- webhook reprocess auditing if webhook replay is introduced later
