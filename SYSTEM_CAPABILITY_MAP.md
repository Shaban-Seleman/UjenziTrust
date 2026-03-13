# System Capability Map

## 1. Executive Overview

NyumbaTrust is a multi-domain platform spanning identity, marketplace transactions, escrow operations, construction workflows, inspections, disbursement processing, ledger posting, admin operations, and local demo infrastructure.

This inventory is based on the current repository state across:

- Spring Boot controllers, services, orchestrators, scheduled jobs, entities, and Flyway migrations
- Next.js App Router pages, components, API proxy/client layers, and admin tools
- Database schemas across `users`, `market`, `ops`, `build`, and `ledger`

### Capability Summary

- Total backend capabilities inventoried: 55
- `COMPLETE`: 40
- `PARTIAL`: 1
- `BACKEND ONLY`: 11
- `MISSING`: 3

### High-Level Platform Map

```text
Platform
├── Authentication & Identity
│   ├── JWT login and session issuance
│   ├── current actor lookup
│   ├── role/ownership authorization
│   ├── user directory lookup
│   └── Next.js proxy + HttpOnly cookie session
├── Marketplace
│   ├── property create/edit/publish/list/detail
│   ├── offer submit/counter/accept/reject/withdraw
│   └── reservation list/cancel
├── Escrow / Financial Core
│   ├── reservation-to-escrow creation
│   ├── escrow list/detail
│   └── escrow disbursement visibility
├── Construction Management
│   ├── project create/list/detail
│   ├── participant assignment
│   ├── project activation
│   ├── milestone create/list/detail
│   ├── evidence submission
│   ├── single and multi-party approval
│   └── retention release
├── Inspection System
│   ├── schedule inspection
│   ├── complete inspection
│   ├── list milestone inspections
│   └── inspection read by id
├── Disbursement / Payments
│   ├── disbursement order creation
│   ├── outbox enqueue and dispatch
│   ├── settlement webhook processing
│   └── admin event monitoring
├── Ledger & Accounting
│   ├── append-only journal posting
│   ├── hash chain
│   ├── idempotent postings
│   └── admin journal monitoring
├── Admin & Operations
│   ├── monitoring dashboard
│   ├── demo reset/seed controls
│   └── admin-only access model
├── Monitoring & Reliability
│   ├── health/prometheus exposure
│   ├── correlation IDs
│   ├── structured error contract
│   ├── scheduler jobs
│   └── integration and integrity suites
└── Demo Infrastructure
    ├── local-only seed engine
    ├── investor scenario data
    └── frontend quick-start helpers
```

## 2. Domain Capability Tables

### Authentication & Identity

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| JWT login and session issuance | `users/api/AuthController`, `users/service/AuthService`, `common/security/JwtService`, `users.sessions` | `apps/web/app/api/auth/login/route.ts`, `apps/web/app/(auth)/login/page.tsx`, `apps/web/components/auth/LoginPageClient.tsx` | COMPLETE |
| Current actor lookup | `GET /auth/me` in `users/api/AuthController` | `apps/web/app/api/auth/me/route.ts`, `apps/web/components/auth/useActor.ts` | COMPLETE |
| Service-layer role and ownership enforcement | `common/security/AuthorizationService`, method usage across `market`, `build`, `ops`, `demo` services | `RequireAuth`, `RoleGate`, route-aware sidebar visibility | COMPLETE |
| User directory lookup for assignment flows | `GET /users/directory` in `users/api/UserDirectoryController` | `listDirectoryUsers()` in `apps/web/lib/api/endpoints.ts`, project assignment selectors in `apps/web/app/(app)/app/construction/projects/[id]/page.tsx` | COMPLETE |
| Frontend-owned HttpOnly session via proxy | Backend returns token JSON from `AuthService.login`; backend validates token IDs via `users.sessions` | Next proxy reads cookie, injects `Authorization` header through `/api/proxy/[...path]` and `/api/auth/*` | COMPLETE |

### Marketplace

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| Create draft property | `POST /market/properties` in `market/api/PropertyController`, `market/service/PropertyService.createDraft` | `apps/web/app/(app)/app/marketplace/properties/new/page.tsx` | COMPLETE |
| Edit property | `PUT /market/properties/{propertyId}` in `PropertyController`, `PropertyService.update` | Edit dialog in `apps/web/app/(app)/app/marketplace/properties/[id]/page.tsx` | COMPLETE |
| Publish property | `POST /market/properties/{propertyId}/publish` in `PropertyController`, `PropertyService.publish` | Property detail action in `apps/web/app/(app)/app/marketplace/properties/[id]/page.tsx` | COMPLETE |
| Browse property inventory | `GET /market/properties`, `GET /market/properties/mine` in `PropertyController` | `apps/web/app/(app)/app/marketplace/properties/page.tsx` with owner/seller-aware view selection | COMPLETE |
| View property detail | `GET /market/properties/{propertyId}` in `PropertyController` | `apps/web/app/(app)/app/marketplace/properties/[id]/page.tsx` | COMPLETE |
| Submit offer | `POST /market/properties/{propertyId}/offers` in `market/api/OfferController`, `market/service/OfferService.submit` | Buyer form on property detail page | COMPLETE |
| Negotiate offers: counter, accept, reject, withdraw | `POST /market/offers/{offerId}/counter|accept|reject|withdraw` in `OfferController`, `OfferService` | `apps/web/app/(app)/app/marketplace/offers/page.tsx` | COMPLETE |
| List and cancel reservations | `GET /market/reservations`, `POST /market/reservations/{reservationId}/cancel` in `OfferController`, `OfferService.cancelReservation` | `apps/web/app/(app)/app/marketplace/reservations/page.tsx` | COMPLETE |

### Escrow / Financial Core

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| Offer acceptance creates reservation and purchase escrow | `market/service/OfferService.accept`, `market/orchestrator/PropertyPurchaseOrchestrator`, `ops/service/EscrowService.createEscrowIdempotent` | Triggered from offer actions page when seller accepts an offer | COMPLETE |
| Escrow list and detail visibility | `GET /ops/escrows`, `GET /ops/escrows/{escrowId}` in `ops/api/OpsReadController`, `ops/service/OpsReadService` | `apps/web/app/(app)/app/escrows/page.tsx`, `apps/web/app/(app)/app/escrows/[id]/page.tsx` | COMPLETE |
| Escrow timeline and disbursement visibility | Escrow entity timestamps + `GET /ops/escrows/{escrowId}/disbursements` in `OpsReadController` | `EscrowTimeline` and disbursement table in escrow detail page | COMPLETE |
| Explicit operator funding/release controls for escrow lifecycle | No dedicated controller/service endpoints surfaced for manual escrow funding/release actions | Read-only escrow UI only | MISSING |

### Construction Management

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| Create draft construction project and escrow | `POST /build/projects` in `build/api/BuildProjectController`, `build/service/BuildProjectService.createDraft` | `apps/web/app/(app)/app/construction/projects/new/page.tsx` | COMPLETE |
| List and view projects | `GET /build/projects`, `GET /build/projects/{projectId}` in `BuildProjectController` | `apps/web/app/(app)/app/construction/projects/page.tsx`, `apps/web/app/(app)/app/construction/projects/[id]/page.tsx` | COMPLETE |
| Assign contractor and inspector | `POST /build/projects/{projectId}/assign` in `BuildProjectController`, `BuildProjectService.assignParticipants` | Assignment dialog with role-filtered user selectors on project detail page | COMPLETE |
| Activate project | `POST /build/projects/{projectId}/activate` in `BuildProjectController`, `BuildProjectService.activate` | Activation action on project detail page | COMPLETE |
| Create milestone | `POST /build/projects/{projectId}/milestones` in `build/api/MilestoneController`, `build/service/MilestoneService.create` | Milestone creation dialog on project detail page | COMPLETE |
| List and view milestones | `GET /build/projects/{projectId}/milestones`, `GET /build/milestones/{milestoneId}` in `MilestoneController`, `MilestoneService` | Milestone table and timeline on project detail page | COMPLETE |
| Submit milestone evidence | `POST /build/milestones/{milestoneId}/submit` in `MilestoneController`, `MilestoneService.submitEvidence` | Contractor evidence dialog on project detail page | COMPLETE |
| Approve milestone (single payout) | `POST /build/milestones/{milestoneId}/approve` in `MilestoneController`, `build/orchestrator/MilestoneOrchestrator` | Owner action on project detail page | COMPLETE |
| Approve milestone (multi-party split payout) | `POST /build/milestones/{milestoneId}/approve-multi` in `MilestoneController`, `build/orchestrator/MultiPartyMilestoneOrchestrator` | Split approval dialog with live allocation math on project detail page | COMPLETE |
| Release milestone retention | `POST /build/milestones/{milestoneId}/retention-release` in `MilestoneController`, `build/orchestrator/RetentionOrchestrator` | Owner action on project detail page | COMPLETE |

### Inspection System

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| Schedule inspection | `POST /build/inspections/schedule` in `build/api/InspectionController`, `build/orchestrator/InspectionOrchestrator.schedule` | Inspector action on project detail page | COMPLETE |
| Complete inspection | `POST /build/inspections/{inspectionId}/complete` in `InspectionController`, `InspectionOrchestrator.complete` | Inspector completion dialog on project detail page | COMPLETE |
| List milestone inspections | `GET /build/milestones/{milestoneId}/inspections` in `MilestoneController`, `build/service/InspectionService.listByMilestone` | `listMilestoneInspections()` and milestone completion flow on project detail page | COMPLETE |
| Read inspection by id | `GET /build/inspections/{inspectionId}` in `InspectionController`, `InspectionService.getVisibleById` | Client helper `getInspection()` exists, but there is no dedicated inspection detail screen yet | PARTIAL |

### Disbursement / Payment Processing

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| Create disbursement orders and outbox events during payouts | `ops/orchestrator/DisbursementOrchestrator`, `ops/service/DisbursementService`, `ops/service/OutboxService` | Triggered indirectly by offer acceptance, milestone approval, and retention release flows | BACKEND ONLY |
| Dispatch outbox events to bank adapter with retries | `ops/service/OutboxDispatcher`, `ops/bank/BankAdapter` | No direct operator action; visible through admin monitoring only | BACKEND ONLY |
| Verify and process settlement webhooks | `POST /ops/webhooks/settlement` in webhook controller, `ops/service/WebhookService`, `WebhookVerificationService` | No direct end-user UI; effects are visible in escrow/project state | BACKEND ONLY |
| Monitor outbox events | `GET /ops/outbox` in `ops/api/OpsReadController` | Admin dashboard table in `apps/web/components/admin/AdminMonitoringDashboard.tsx` | COMPLETE |
| Monitor webhook events | `GET /ops/webhooks/events` in `OpsReadController` | Admin dashboard table in `AdminMonitoringDashboard.tsx` | COMPLETE |

### Ledger & Accounting

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| Append-only, balanced, idempotent ledger posting with hash chain | `ledger/service/LedgerPostingService`, append-only DB triggers in Flyway, `ledger/hash_chain` | No end-user UI required | BACKEND ONLY |
| Ledger journal entry visibility for admins | `GET /ledger/journal-entries` in `ledger/api/LedgerReadController`, `ledger/service/LedgerReadService` | Admin dashboard ledger table | COMPLETE |
| Multi-party payable account mapping by payee type | `ledger/service/LedgerAccountCodes`, `build/orchestrator/MultiPartyMilestoneOrchestrator`, `ops/service/DisbursementService.payableAccountFor` | No dedicated UI required | BACKEND ONLY |
| Retention release financial postings | `build/orchestrator/RetentionOrchestrator`, `ledger/service/LedgerTemplateService.retentionReleaseAuthorized` | Triggered from project detail action; financial internals are backend-only | BACKEND ONLY |

### Admin & Operations

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| Canonical admin monitoring entry point | Admin-protected read endpoints in `ops`, `ledger`, `demo` modules | `apps/web/app/(app)/admin/page.tsx`, `AdminMonitoringDashboard` | COMPLETE |
| Demo reset/seed tools | `demo/DemoAdminController`, `demo/DemoSeeder` | `DemoSetupButton`, `DemoQuickStartCard`, admin page | COMPLETE |
| Admin navigation consolidation | N/A backend; frontend route ownership decision | Sidebar routes admin users to `/admin`; legacy `/app/admin` redirects there | COMPLETE |
| Admin-only access enforcement | `AuthorizationService.requireAdmin`, demo controller checks, read-service visibility checks | `RoleGate` on admin UI plus backend enforcement | COMPLETE |

### Monitoring & Reliability

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| Health and Prometheus exposure | Spring management config in `src/main/resources/application.yml` | No UI required | BACKEND ONLY |
| Correlation IDs and structured request logging | `common/web/CorrelationIdFilter`, log pattern config in `application.yml` | No UI required | BACKEND ONLY |
| Standardized API error contract with `Detail` | `common/error/GlobalExceptionHandler` and API error DTOs | Frontend `ApiError` normalization preserves backend `Detail` | COMPLETE |
| Scheduler jobs for expiries, retention, and outbox | `market/job/OfferExpiryJob`, `market/job/ReservationExpiryJob`, `build/job/RetentionReleaseJob`, `ops/service/OutboxDispatcher` | No UI required | BACKEND ONLY |
| Webhook replay protection and signature verification | `ops/service/WebhookVerificationService` | No UI required | BACKEND ONLY |
| Testcontainers integration and integrity suites | `src/test/java/com/uzenjitrust/integration/*`, `src/test/java/com/uzenjitrust/integrity/*` | No UI required | BACKEND ONLY |

### Demo Infrastructure

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| Local-only demo reset and seed engine | `demo/DemoSeeder`, `demo/DemoAdminController`, `app.demo.enabled` + `local` profile guards | Admin demo controls and quick-start affordances | COMPLETE |
| Investor demo scenario data | `DemoSeeder.seedInvestorScenario()` creates users, properties, offers, escrows, projects, milestones, disbursements, retention states | `DemoQuickStartCard` and seeded app flows | COMPLETE |
| Demo mode gating and operator guidance | `@Profile("local")`, `@ConditionalOnProperty(app.demo.enabled=true)` | `lib/config/demo.ts`, admin helpers, session-level “seeded” signals | COMPLETE |

### Additional Data-Model Domains

| Capability | Backend Implementation | Frontend Coverage | Status |
|---|---|---|---|
| Property media workflow | `market.property_media` schema exists in Flyway, but no active controller/service workflow surfaced | No UI | MISSING |
| Change order workflow | `build.change_orders` schema exists in Flyway, but no active controller/service/UI workflow surfaced | No UI | MISSING |

## 3. End-to-End Workflow Maps

### Marketplace Flow

**Property → Offer → Reservation**

- Backend components:
  - `PropertyController` / `PropertyService`
  - `OfferController` / `OfferService`
  - `MarketReadService`
- Frontend components:
  - Property list: `apps/web/app/(app)/app/marketplace/properties/page.tsx`
  - Property create/detail: `.../properties/new/page.tsx`, `.../properties/[id]/page.tsx`
  - Offers: `.../marketplace/offers/page.tsx`
  - Reservations: `.../marketplace/reservations/page.tsx`
- Integration points:
  - Buyer submits offer from property detail
  - Seller counters/accepts/rejects from offers page
  - Reservation visibility and cancellation follow acceptance

### Escrow Flow

**Reservation → Escrow → Funding/State → Disbursements**

- Backend components:
  - `OfferService.accept`
  - `PropertyPurchaseOrchestrator`
  - `EscrowService`
  - `OpsReadController` / `OpsReadService`
- Frontend components:
  - Offers page initiates the flow
  - Escrow list/detail pages expose resulting records and lifecycle state
  - `EscrowTimeline` visualizes state progression
- Integration points:
  - Accepting an offer creates reservation + escrow idempotently
  - Disbursement visibility comes from escrow detail
  - Explicit operator funding/release UI is not currently implemented

### Construction Flow

**Project → Milestone → Evidence → Inspection → Approval → Disbursement → Retention Release**

- Backend components:
  - `BuildProjectController` / `BuildProjectService`
  - `MilestoneController` / `MilestoneService`
  - `InspectionController` / `InspectionOrchestrator` / `InspectionService`
  - `MilestoneOrchestrator`
  - `MultiPartyMilestoneOrchestrator`
  - `RetentionOrchestrator`
- Frontend components:
  - Project list and create pages
  - Project detail page with participant assignment, activation, milestone create/evidence/approval/inspection/retention actions
  - `MilestoneTimeline`
- Integration points:
  - Project creation provisions construction escrow on backend
  - Owner assigns contractor/inspector and activates project
  - Contractor submits evidence
  - Inspector schedules and completes inspection
  - Owner approves single or split payout
  - Retention release can be triggered when eligible

### Inspection Flow

**Schedule → Complete → Result → Milestone summary**

- Backend components:
  - `InspectionController`
  - `InspectionOrchestrator`
  - `InspectionService`
  - milestone summary enrichment via `InspectionService.applyLatestInspectionSummary`
- Frontend components:
  - Project detail page
  - Milestone timeline inspection badges/fields
- Integration points:
  - Scheduling returns an inspection record
  - Completion posts report JSON containing result
  - Milestone read/list responses expose `inspectionStatus`, `inspectionResult`, `inspectionCompletedAt`, `inspectionId`

### Payments Flow

**Milestone approval → Disbursement order → Outbox → Settlement webhook → Ledger**

- Backend components:
  - `MilestoneOrchestrator` / `MultiPartyMilestoneOrchestrator`
  - `DisbursementOrchestrator`
  - `OutboxService`
  - `OutboxDispatcher`
  - `WebhookService`
  - `DisbursementService`
  - `LedgerPostingService`
- Frontend components:
  - Project detail approval actions
  - Admin monitoring dashboard for outbox/webhook/ledger read visibility
  - Escrow/project read surfaces that reflect settlement outcomes
- Integration points:
  - Approval creates disbursements and outbox rows transactionally
  - Dispatcher submits payouts asynchronously
  - Verified webhook settles disbursement and posts settlement ledger entries
  - Milestone transitions to `PAID` only after all disbursements settle

## 4. Infrastructure Overview

### Idempotency Mechanisms

- Escrows use business keys through `EscrowService.createEscrowIdempotent`
- Disbursements and outbox events use business keys and idempotency keys
- Ledger entries use `(entryType, referenceId, idempotencyKey)` reuse semantics in `LedgerPostingService`
- Webhook dedupe persists `ops.webhook_events.event_id`
- Milestone payout splits persist business keys

### Outbox / Event Delivery

- Disbursement creation and outbox insert happen inside transactional orchestrators
- `OutboxDispatcher` polls pending events and uses exponential backoff on failure
- Bank submission occurs outside business transactions through the adapter

### Webhook Processing

- `/ops/webhooks/settlement` is public by design
- `WebhookVerificationService` enforces HMAC validation and replay window checks
- `WebhookService` persists webhook event first, dedupes on `event_id`, then settles the matching disbursement

### Ledger Rules

- Posting is centralized in `LedgerPostingService`
- Balance validation is enforced per currency
- Hash-chain continuity is enforced through `ledger.hash_chain`
- Append-only integrity is reinforced by database triggers introduced in Flyway

### Access Control

- `SecurityConfig` enables stateless JWT security
- `AuthorizationService` centralizes role + ownership checks for owner, seller, buyer, contractor, inspector, and admin
- Frontend mirrors access expectations via `RequireAuth`, `RoleGate`, and role-aware navigation

### Error Contract

- `GlobalExceptionHandler` returns standardized error payloads carrying human-readable detail
- Frontend `ApiError` normalization preserves backend `Detail`

### Operational Tooling

- Admin dashboard shows outbox, webhook, and ledger entry lists
- Demo reset/seed controls are local-only and guarded by feature flag plus admin role
- Auth sanity check exists in the dashboard for session verification

## 5. Remaining System Gaps

### P0 – Platform Integrity Risk

- Swagger/OpenAPI exposure is currently permitted in `SecurityConfig` without visible profile gating. Production restriction should be enforced explicitly.
- Admin monitoring is read-only. There are no operator remediation controls for failed outbox items or problematic webhook events.
- Escrow lifecycle state changes beyond creation and passive visibility are not exposed as explicit operator workflows, which leaves part of the money-state lifecycle dependent on backend-only triggers and seeded/demo data.

### P1 – Operational Gap

- Inspection read-by-id exists but has no dedicated frontend detail view or admin inspection console.
- Ledger visibility is summary-only; there is no journal line drilldown, account balance view, or hash-chain inspection UI.
- Disbursements are visible only from escrow detail; there is no first-class disbursement monitoring screen.
- Health and Prometheus endpoints exist but are not surfaced in any admin or ops UI.

### P2 – Product Improvement

- Property media has schema support but no implemented API or frontend flow.
- Change orders have schema support but no exposed business workflow.
- Public browsing is intentionally routed into authenticated marketplace access; there is still no true marketing/public property discovery surface.

### P3 – Polish / Optimization

- The route tree contains compatibility shims and duplicated group paths such as `/app` and `/app/app`, which work but add cognitive overhead.
- Admin links point into anchored dashboard sections rather than dedicated filtered views.
- Dashboard metrics are aggregate counts only; there is no richer drill-through or actor-specific summary state.

## 6. Suggested Next Development Waves

### Wave 3 – Operations & Observability

- Restrict Swagger/OpenAPI exposure by production profile
- Add admin views for disbursements, outbox retries, webhook replay/failure triage
- Add health, scheduler, and queue status visibility to admin monitoring
- Add ledger drilldown: journal lines, entry detail, account-level views

### Wave 4 – Platform Hardening

- Add explicit escrow operator workflows where business rules require human action
- Strengthen audit surfaces around settlements, reversals, and payout failures
- Extend integrity test coverage for admin/ops and authorization edge cases
- Reduce route duplication and consolidate compatibility shims

### Wave 5 – Product Expansion

- Implement property media upload and moderation flows
- Implement change-order workflow for construction scope adjustments
- Add dedicated inspection detail/history views
- Consider a true anonymous marketplace or public marketing layer if product strategy requires it

## 7. Notes on Canonical Frontend Flow Ownership

- Canonical marketplace path: `/app/marketplace/properties`
- Canonical construction path: `/app/construction/projects`
- Canonical admin path: `/admin`
- Public property routes currently redirect into authenticated marketplace flow, which matches the current backend auth posture
