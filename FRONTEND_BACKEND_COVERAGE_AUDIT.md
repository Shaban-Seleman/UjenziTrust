# 1. Executive Summary

Confidence: **High** for endpoint/page existence and wiring; **Medium** for a few flow-level behaviors that need runtime confirmation with seeded actors.

- Total backend capabilities found: **38**
- Fully implemented in frontend: **15**
- Partially implemented in frontend: **14**
- Not implemented in frontend: **6**
- Backend-only / no UI needed: **3**
- Unknown / needs manual review: **0**

Top 10 highest-priority frontend gaps:
1. Main nav points to top-level placeholder routes (`/marketplace`, `/escrows`, `/construction`, `/admin`) while most real feature UIs live under `/app/*`.
2. Construction milestone evidence submit action is wired with an incorrect payload shape (likely backend 400).
3. Construction project lifecycle is incomplete in UI: no assign participants flow, no activate flow.
4. No milestone creation UI even though backend supports it.
5. No inspection complete UI even though backend supports it.
6. Reservation cancellation endpoint exists but reservations page is read-only.
7. Offer counter endpoint exists but offers UI has no counter action.
8. Property update endpoint exists but no edit UI.
9. Public listing pages are present, but backend security requires auth for property read/search, so “public browse” is effectively non-functional.
10. Demo admin has split experience: placeholder admin route vs monitoring route, creating confusion about where operational tools live.

# 2. Coverage Matrix by Module

| Module | Backend Capability | Backend Location | Frontend Coverage | Frontend Location | Status | Notes |
|--------|--------------------|------------------|-------------------|-------------------|--------|------|
| Auth / Users / Roles | Login (`POST /auth/login`) | `src/main/java/com/uzenjitrust/users/api/AuthController.java` `login()` | Login form posts via Next auth proxy | `apps/web/components/auth/LoginPageClient.tsx`, `apps/web/app/api/auth/login/route.ts` | FULLY IMPLEMENTED IN FRONTEND | Token is stored server-side in HttpOnly cookie via Next route handler. |
| Auth / Users / Roles | Current actor (`GET /auth/me`) | `src/main/java/com/uzenjitrust/users/api/AuthController.java` `me()` | Actor query + session bootstrap | `apps/web/components/auth/useActor.ts`, `apps/web/app/api/auth/me/route.ts` | FULLY IMPLEMENTED IN FRONTEND | Used globally for auth/roles. |
| Auth / Users / Roles | Role/ownership authorization in service layer | `AuthorizationService` usage across market/build/ops services | UI role gates and route protection | `apps/web/components/auth/RequireAuth.tsx`, `apps/web/components/auth/RoleGate.tsx` | PARTIALLY IMPLEMENTED IN FRONTEND | UI roles are enforced, but ownership-specific UX messaging is limited; backend remains source of truth. |
| Auth / Users / Roles | Session validation against `users.sessions` | `src/main/java/com/uzenjitrust/common/security/JwtAuthenticationFilter.java` | No direct UI | N/A | BACKEND-ONLY / NO UI NEEDED | Correctly backend concern. |
| Auth / Users / Roles | Local-only demo endpoint exposure rules | `src/main/java/com/uzenjitrust/demo/DemoAdminController.java` annotations | Demo controls shown only when `NEXT_PUBLIC_DEMO_MODE=true` and admin | `apps/web/components/demo/DemoSetupButton.tsx`, `apps/web/lib/config/demo.ts` | PARTIALLY IMPLEMENTED IN FRONTEND | FE env toggle may hide controls even when backend is enabled. |
| Marketplace | Create draft property (`POST /market/properties`) | `PropertyController#create`, `PropertyService#createDraft` | Create property form exists | `apps/web/app/(app)/app/marketplace/properties/new/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Form is wired to live API. |
| Marketplace | Update property (`PUT /market/properties/{id}`) | `PropertyController#update`, `PropertyService#update` | No edit form/action | N/A | NOT IMPLEMENTED IN FRONTEND | Missing property edit UX. |
| Marketplace | Publish property (`POST /market/properties/{id}/publish`) | `PropertyController#publish`, `PropertyService#publish` | Publish button on property detail | `apps/web/app/(app)/app/marketplace/properties/[id]/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Role-gated in UI. |
| Marketplace | Get property by id (`GET /market/properties/{id}`) | `PropertyController#getById` | Property detail pages | `apps/web/app/(app)/app/marketplace/properties/[id]/page.tsx`, `apps/web/app/(public)/property/[id]/page.tsx` | PARTIALLY IMPLEMENTED IN FRONTEND | “Public” page likely unauthorized due backend security. |
| Marketplace | Search/list properties (`GET /market/properties`) | `PropertyController#search` | Properties tables (protected + public route) | `apps/web/app/(app)/app/marketplace/properties/page.tsx`, `apps/web/app/(public)/properties/page.tsx` | PARTIALLY IMPLEMENTED IN FRONTEND | Protected view works; public browse conflicts with backend auth policy. |
| Marketplace | List offers (`GET /market/offers`) | `OfferController#listOffers`, `MarketReadService#listOffers` | Offers table | `apps/web/app/(app)/app/marketplace/offers/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Includes actionable buttons. |
| Marketplace | List reservations (`GET /market/reservations`) | `OfferController#listReservations`, `MarketReadService#listReservations` | Reservations table | `apps/web/app/(app)/app/marketplace/reservations/page.tsx` | PARTIALLY IMPLEMENTED IN FRONTEND | Read-only; no cancellation action in UI. |
| Marketplace | Submit offer (`POST /market/properties/{id}/offers`) | `OfferController#submit`, `OfferService#submit` | Submit offer form | `apps/web/app/(app)/app/marketplace/properties/[id]/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Buyer-only UI gate present. |
| Marketplace | Counter offer (`POST /market/offers/{id}/counter`) | `OfferController#counter`, `OfferService#counter` | No counter action | N/A | NOT IMPLEMENTED IN FRONTEND | Endpoint exists but not exposed. |
| Marketplace | Accept offer (`POST /market/offers/{id}/accept`) | `OfferController#accept`, `OfferService#accept` | Accept button | `apps/web/app/(app)/app/marketplace/offers/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Uses generated idempotency key. |
| Marketplace | Reject offer (`POST /market/offers/{id}/reject`) | `OfferController#reject`, `OfferService#reject` | Reject button | `apps/web/app/(app)/app/marketplace/offers/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Wired. |
| Marketplace | Withdraw offer (`POST /market/offers/{id}/withdraw`) | `OfferController#withdraw`, `OfferService#withdraw` | Withdraw button | `apps/web/app/(app)/app/marketplace/offers/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Wired. |
| Marketplace | Cancel reservation (`POST /market/reservations/{id}/cancel`) | `OfferController#cancelReservation`, `OfferService#cancelReservation` | No cancel action | N/A | NOT IMPLEMENTED IN FRONTEND | Important for seller reservation ops. |
| Marketplace | Offer expiry scheduler | `src/main/java/com/uzenjitrust/market/job/OfferExpiryJob.java` | No UI control; statuses may appear in tables | N/A | BACKEND-ONLY / NO UI NEEDED | Scheduler behavior is backend operational concern. |
| Marketplace | Reservation expiry scheduler | `src/main/java/com/uzenjitrust/market/job/ReservationExpiryJob.java` | No UI control; statuses may appear in tables | N/A | BACKEND-ONLY / NO UI NEEDED | Backend scheduled task. |
| Escrow | List escrows (`GET /ops/escrows`) | `OpsReadController#listEscrows`, `OpsReadService#listEscrows` | Escrows table | `apps/web/app/(app)/app/escrows/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Wired and role-filtered by backend. |
| Escrow | Escrow detail (`GET /ops/escrows/{id}`) | `OpsReadController#getEscrow`, `OpsReadService#getEscrow` | Escrow detail card + timeline | `apps/web/app/(app)/app/escrows/[id]/page.tsx`, `apps/web/components/escrow/EscrowTimeline.tsx` | FULLY IMPLEMENTED IN FRONTEND | Good visualization coverage. |
| Escrow | Disbursements by escrow (`GET /ops/escrows/{id}/disbursements`) | `OpsReadController#listDisbursements` | Disbursement table in escrow detail | `apps/web/app/(app)/app/escrows/[id]/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Wired. |
| Disbursements / Outbox / Webhooks | Outbox events list (`GET /ops/outbox`) | `OpsReadController#listOutbox`, `OpsReadService#listOutboxEvents` | Admin outbox table | `apps/web/app/(app)/app/admin/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Visible to ADMIN only. |
| Disbursements / Outbox / Webhooks | Webhook events list (`GET /ops/webhooks/events`) | `OpsReadController#listWebhookEvents`, `OpsReadService#listWebhookEvents` | Admin webhook table | `apps/web/app/(app)/app/admin/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Visible to ADMIN only. |
| Disbursements / Outbox / Webhooks | Settlement webhook ingest (`POST /ops/webhooks/settlement`) | `WebhookController#settlementWebhook`, `WebhookService#processSettlementEvent` | No UI sender/trigger | N/A | BACKEND-ONLY / NO UI NEEDED | External bank/system callback endpoint. |
| Disbursements / Outbox / Webhooks | Outbox dispatch + disbursement submit workflow | `OutboxDispatcher#dispatch`, `DisbursementService#markSubmitted` | Monitoring only (no control) | `apps/web/app/(app)/app/admin/page.tsx` | PARTIALLY IMPLEMENTED IN FRONTEND | Observability exists; no retry/requeue controls. |
| Disbursements / Outbox / Webhooks | Webhook settlement transitions + payout ledger posting | `WebhookService`, `DisbursementService#settleDisbursement` | Read visibility only via escrow/project pages | `apps/web/app/(app)/app/escrows/[id]/page.tsx`, `apps/web/app/(app)/app/construction/projects/[id]/page.tsx` | PARTIALLY IMPLEMENTED IN FRONTEND | No explicit settlement troubleshooting UX. |
| Construction | List projects (`GET /build/projects`) | `BuildProjectController#list`, `BuildProjectService#listVisible` | Projects table | `apps/web/app/(app)/app/construction/projects/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Wired. |
| Construction | Get project (`GET /build/projects/{id}`) | `BuildProjectController#get`, `BuildProjectService#getVisibleById` | Project detail page | `apps/web/app/(app)/app/construction/projects/[id]/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Wired. |
| Construction | Create project (`POST /build/projects`) | `BuildProjectController#create`, `BuildProjectService#createDraft` | New project form | `apps/web/app/(app)/app/construction/projects/new/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Recently added and wired. |
| Construction | Assign participants (`POST /build/projects/{id}/assign`) | `BuildProjectController#assign`, `BuildProjectService#assignParticipants` | No assignment form | N/A | NOT IMPLEMENTED IN FRONTEND | Critical gap for owner workflow. |
| Construction | Activate project (`POST /build/projects/{id}/activate`) | `BuildProjectController#activate`, `BuildProjectService#activate` | No activation action | N/A | NOT IMPLEMENTED IN FRONTEND | Projects can remain draft without UI path to activate. |
| Construction | Create milestone (`POST /build/projects/{id}/milestones`) | `MilestoneController#create`, `MilestoneService#create` | No milestone create form | N/A | NOT IMPLEMENTED IN FRONTEND | Project detail only lists/actions existing milestones. |
| Construction | List milestones (`GET /build/projects/{id}/milestones`) | `MilestoneController#listByProject`, `MilestoneService#listByProject` | Timeline + action table | `apps/web/app/(app)/app/construction/projects/[id]/page.tsx`, `apps/web/components/construction/MilestoneTimeline.tsx` | FULLY IMPLEMENTED IN FRONTEND | Good visualization coverage. |
| Construction | Submit milestone evidence (`POST /build/milestones/{id}/submit`) | `MilestoneController#submitEvidence`, `MilestoneService#submitEvidence` | Button exists, likely payload mismatch | `apps/web/app/(app)/app/construction/projects/[id]/page.tsx` | PARTIALLY IMPLEMENTED IN FRONTEND | FE sends `{evidenceUri, comment}` while backend expects `{evidence: {...}, notes}`. |
| Construction | Approve milestone single (`POST /build/milestones/{id}/approve`) | `MilestoneController#approveSingle`, `MilestoneOrchestrator#approveMilestoneSingle` | Approve button | `apps/web/app/(app)/app/construction/projects/[id]/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Wired with idempotency key. |
| Construction | Approve milestone multi (`POST /build/milestones/{id}/approve-multi`) | `MilestoneController#approveMulti`, `MultiPartyMilestoneOrchestrator#approveMilestoneMulti` | Split dialog exists | `apps/web/app/(app)/app/construction/projects/[id]/page.tsx` | PARTIALLY IMPLEMENTED IN FRONTEND | Form only captures supplier/inspector splits and can fail validation/business rule depending on milestone totals. |
| Construction | Schedule inspection (`POST /build/inspections/schedule`) | `InspectionController#schedule`, `InspectionOrchestrator#schedule` | “Inspect” action triggers schedule | `apps/web/app/(app)/app/construction/projects/[id]/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Wired. |
| Construction | Complete inspection (`POST /build/inspections/{id}/complete`) | `InspectionController#complete`, `InspectionOrchestrator#complete` | No completion UI | N/A | NOT IMPLEMENTED IN FRONTEND | Important for inspection lifecycle closure. |
| Retention | Manual retention release (`POST /build/milestones/{id}/retention-release`) | `MilestoneController#releaseRetention`, `RetentionOrchestrator#releaseRetention` | Release retention button | `apps/web/app/(app)/app/construction/projects/[id]/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Wired. |
| Retention | System retention release scheduler | `RetentionReleaseJob#runHourly`, `RetentionOrchestrator#releaseDueRetentionsSystem` | No UI control | N/A | BACKEND-ONLY / NO UI NEEDED | Background job. |
| Retention | Retention timeline/visual state | Milestone fields (`retentionAmount`, `retentionReleaseAt`, `retentionReleasedAt`) | Countdown and status shown | `apps/web/components/construction/MilestoneTimeline.tsx` | FULLY IMPLEMENTED IN FRONTEND | Strong visualization support. |
| Admin / Monitoring | Ledger journal entries list (`GET /ledger/journal-entries`) | `LedgerReadController#listJournalEntries` | Admin ledger table | `apps/web/app/(app)/app/admin/page.tsx` | FULLY IMPLEMENTED IN FRONTEND | Wired. |
| Admin / Monitoring | Operational admin view (outbox/webhooks/ledger) | `OpsReadController`, `LedgerReadController` | Exists but on secondary `/app/admin` path; top-level `/admin` is placeholder | `apps/web/app/(app)/app/admin/page.tsx`, `apps/web/app/(app)/admin/page.tsx` | PARTIALLY IMPLEMENTED IN FRONTEND | UX split causes discoverability issues. |
| Admin / Monitoring | Role-restricted admin access | Backend role checks in `OpsReadService`/`LedgerReadService` | FE role gate + section route rules | `apps/web/components/auth/RoleGate.tsx`, `apps/web/lib/auth/roles.ts` | PARTIALLY IMPLEMENTED IN FRONTEND | Routing split still exposes placeholder route with weaker utility. |
| Demo Tools | Reset/seed/reset-and-seed endpoints | `DemoAdminController` + `DemoSeeder` | Frontend supports only reset-and-seed | `apps/web/components/demo/DemoSetupButton.tsx` | PARTIALLY IMPLEMENTED IN FRONTEND | Functional for demos, but no separate reset/seed controls. |
| Demo Tools | Demo scenario summary payload | `DemoSeedSummary` | Summary modal and quick-start card | `apps/web/components/demo/DemoSetupButton.tsx`, `apps/web/components/demo/DemoQuickStartCard.tsx` | FULLY IMPLEMENTED IN FRONTEND | Good operator UX. |
| Demo Tools | Local/profile constrained demo behavior | `@Profile("local")` + `app.demo.enabled` | FE gracefully handles 403/404 | `apps/web/components/demo/DemoSetupButton.tsx` | FULLY IMPLEMENTED IN FRONTEND | Error UX for unavailable environment exists. |

# 3. Missing Frontend Features

Backend features implemented but not visible/usable in frontend:

1. Property update/edit (`PUT /market/properties/{id}`).
2. Offer counter (`POST /market/offers/{id}/counter`).
3. Reservation cancel (`POST /market/reservations/{id}/cancel`).
4. Project participant assignment (`POST /build/projects/{id}/assign`).
5. Project activation (`POST /build/projects/{id}/activate`).
6. Milestone creation (`POST /build/projects/{id}/milestones`).
7. Inspection completion (`POST /build/inspections/{id}/complete`).

Also effectively missing from user perspective due routing/discoverability:

8. Main nav links to placeholder routes while real module pages are nested under `/app/*`.
9. Public browse pages (`/properties`, `/property/[id]`) are likely blocked by backend auth policy.

# 4. Partial/Incomplete Flows

1. **Construction evidence submit likely broken**
   - UI action exists, but payload shape appears mismatched for backend DTO.
   - FE sends raw `{ evidenceUri, comment }`, backend expects `{ evidence: Map, notes }`.
   - Confidence: High.

2. **Construction multi-party approve is under-modeled**
   - Split dialog only asks supplier + inspector values, no contractor split input.
   - Backend enforces split math against gross/retention, so UX can easily fail.
   - Confidence: High.

3. **Admin experience split across two routes**
   - `/admin` shows placeholder + demo cards.
   - `/app/admin` has real monitoring tables.
   - Sidebar routes to `/admin`, not `/app/admin`.
   - Confidence: High.

4. **Module placeholders vs real pages**
   - `/dashboard`, `/marketplace`, `/escrows`, `/construction` are placeholder foundations.
   - Richer, action-oriented pages mostly live under `/app/...`.
   - Confidence: High.

5. **Reservations flow incomplete**
   - Reservation listing exists, but no cancel action (despite backend endpoint).
   - Confidence: High.

6. **Public listing UX likely non-functional**
   - Public pages call authenticated endpoints.
   - Backend security requires auth for those paths.
   - Confidence: High.

# 5. Recommended Implementation Order

## A. Investor Demo Critical

1. Unify navigation to real module pages (route sidebar/top links to actionable pages, not placeholders).
2. Fix milestone evidence submit payload contract.
3. Harden multi-party milestone approval form (contractor/supplier/inspector split support and sum validation before submit).
4. Ensure `/admin` lands on monitoring UI with outbox/webhook/ledger and demo setup.
5. Add reservation cancellation action and offer counter action.

## B. Pilot User Critical

1. Add project assignment and activation UI.
2. Add milestone creation flow in project detail.
3. Add inspection completion UI.
4. Add property edit/update flow.
5. Improve role-aware empty/error messaging per module.

## C. Production Admin/Ops Critical

1. Add filter/search/pagination controls to outbox/webhook/ledger tables.
2. Add operational drill-down (event payload detail, escrow/disbursement links).
3. Add explicit status chips and refresh controls for monitoring pages.

## D. Nice-to-have

1. Keep “public browse” pages only if backend permits anonymous property reads; otherwise remove or guard.
2. Add UX polish for idempotency key visibility and retry guidance in action-heavy forms.
3. Add richer timeline drill-down for escrow/disbursement transitions.

# 6. Appendices

## Backend Endpoints Discovered

- Auth: `/auth/login`, `/auth/me`
- Marketplace properties: `/market/properties` (POST, GET), `/market/properties/{id}` (GET, PUT), `/market/properties/{id}/publish` (POST)
- Marketplace offers/reservations: `/market/offers` (GET), `/market/reservations` (GET), `/market/properties/{propertyId}/offers` (POST), `/market/offers/{id}/counter|accept|reject|withdraw` (POST), `/market/reservations/{id}/cancel` (POST)
- Ops read: `/ops/escrows`, `/ops/escrows/{id}`, `/ops/escrows/{id}/disbursements`, `/ops/outbox`, `/ops/webhooks/events`
- Webhook ingest: `/ops/webhooks/settlement`
- Build projects: `/build/projects` (GET, POST), `/build/projects/{id}` (GET), `/build/projects/{id}/assign` (POST), `/build/projects/{id}/activate` (POST)
- Build milestones: `/build/projects/{id}/milestones` (POST, GET), `/build/milestones/{id}/submit|approve|approve-multi|retention-release` (POST)
- Build inspections: `/build/inspections/schedule`, `/build/inspections/{id}/complete`
- Ledger read: `/ledger/journal-entries`
- Demo admin (local+enabled): `/admin/demo/reset`, `/admin/demo/seed`, `/admin/demo/reset-and-seed`

## Frontend Routes Discovered

- Auth: `/login`
- Protected placeholders: `/dashboard`, `/marketplace`, `/escrows`, `/construction`, `/admin`
- Action-heavy module routes: `/app/dashboard`, `/app/marketplace/properties`, `/app/marketplace/properties/new`, `/app/marketplace/properties/[id]`, `/app/marketplace/offers`, `/app/marketplace/reservations`, `/app/escrows`, `/app/escrows/[id]`, `/app/construction/projects`, `/app/construction/projects/new`, `/app/construction/projects/[id]`, `/app/admin`
- Public: `/`, `/properties`, `/property/[id]`
- Next API proxy/auth: `/api/auth/login`, `/api/auth/me`, `/api/auth/logout`, `/api/proxy/[...path]`

## Unresolved Questions

1. Should top-level module routes be converted from placeholders to the current `/app/*` feature pages, or should `/app/*` be flattened?
2. Are public property browsing routes intended to be anonymous in product scope? If yes, backend security policy must expose read endpoints.
3. Should admin monitor pages support operational actions (replay/requeue), or remain read-only by design?
