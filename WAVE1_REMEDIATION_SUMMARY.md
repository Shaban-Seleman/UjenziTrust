# Wave 1 Remediation Summary

## What Was Fixed

### 1) Navigation now lands on real working pages
- Updated primary sidebar module links to real feature routes instead of placeholder pages.
- Main entries now point to:
  - Dashboard -> `/app/dashboard`
  - Marketplace -> `/app/marketplace/properties`
  - Escrows -> `/app/escrows`
  - Construction -> `/app/construction/projects`
  - Admin -> `/admin`
- Updated active-state matching so module nav remains highlighted across subroutes.

Files:
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/components/layout/Sidebar.tsx`

### 2) Admin route consolidation
- Made `/admin` the canonical admin landing route and connected it to the real monitoring UI.
- Legacy `/app/admin` now redirects to `/admin`.
- Added an admin monitoring dashboard component with:
  - Demo setup tools
  - Outbox events
  - Webhook events
  - Ledger entries
  - Quick links including disbursement monitoring via escrows
- Admin-only gating is enforced through existing guards and `RoleGate`.

Files:
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/components/admin/AdminMonitoringDashboard.tsx`
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/admin/page.tsx`
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/admin/page.tsx`

### 3) Milestone evidence submit payload corrected
- Replaced the incorrect ad hoc payload with backend-compatible payload:
  - `{ evidence: {...}, notes: "..." }`
- Added a validated evidence submit dialog on project detail milestones.
- Added success/error toasts and refetch behavior.

Files:
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/construction/projects/[id]/page.tsx`

### 4) Project assignment UI added
- Added OWNER-only "Assign Participants" action on project detail.
- Added validated form for:
  - `contractorUserId` (UUID)
  - `inspectorUserId` (UUID)
- Refetches project + milestones on success.

Files:
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`

### 5) Project activation action added
- Added OWNER-only "Activate Project" confirmation flow.
- Only shown when project status is `DRAFT`.
- Uses success/error toasts and data refresh.

Files:
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`

### 6) Milestone creation form added
- Added OWNER-only "Create Milestone" dialog on project detail.
- Uses React Hook Form + Zod validation.
- Supports required and optional fields aligned to backend request DTO.
- On success: closes dialog, toasts, refetches.

Files:
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`

### 7) Reservation cancellation UI added
- Added reservation row action to cancel active reservations.
- Action is role-gated for SELLER/OWNER and requires confirmation.
- Includes loading state, toast feedback, and table refresh.

Files:
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/marketplace/reservations/page.tsx`
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`

### 8) Offer counter flow added
- Added seller/owner/admin "Counter Offer" dialog from offers table.
- Validated counter amount and optional notes.
- Added status-aware enable/disable behavior for counter/accept/reject/withdraw actions.
- Includes loading state, success/error toasts, and list refresh.

Files:
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/marketplace/offers/page.tsx`
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`

### 9) Placeholder dead-ends removed from primary entry routes
- Converted top-level placeholder module pages to redirects into real feature pages:
  - `/dashboard` -> `/app/dashboard`
  - `/marketplace` -> `/app/marketplace/properties`
  - `/escrows` -> `/app/escrows`
  - `/construction` -> `/app/construction/projects`

Files:
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/dashboard/page.tsx`
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/marketplace/page.tsx`
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/escrows/page.tsx`
- `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/construction/page.tsx`

## Routes Changed

- Canonical admin path: `/admin` (now real monitoring dashboard)
- Legacy admin alias: `/app/admin` (redirects to `/admin`)
- Module top-level routes now redirect to real implementations:
  - `/dashboard`
  - `/marketplace`
  - `/escrows`
  - `/construction`
- Sidebar primary links now target `/app/*` feature pages (except `/admin`).

## Backend Endpoints Wired (Wave 1)

Already wired/fixed:
- `POST /build/milestones/{milestoneId}/submit` (payload corrected)
- `POST /build/projects/{projectId}/assign`
- `POST /build/projects/{projectId}/activate`
- `POST /build/projects/{projectId}/milestones`
- `POST /market/reservations/{reservationId}/cancel`
- `POST /market/offers/{offerId}/counter`

Continued usage (existing):
- `POST /market/offers/{offerId}/accept`
- `POST /market/offers/{offerId}/reject`
- `POST /market/offers/{offerId}/withdraw`
- `GET /ops/outbox`
- `GET /ops/webhooks/events`
- `GET /ledger/journal-entries`

## Validation / Build Status

- `npm run typecheck` passed.
- `npm run build` passed.

## Remaining Blockers / Assumptions

1. Project assignment currently uses manual UUID entry for contractor/inspector because no user-search endpoint is exposed to the frontend.
2. Reservation cancel button is shown for SELLER/OWNER roles; backend still enforces ownership and may reject non-owner sellers, with backend `Detail` surfaced in toasts.
3. Multi-party milestone approval UX still uses supplier/inspector split inputs only (existing behavior retained in this wave).
4. Public listing routes remain in place; backend auth policy still determines whether they function anonymously.
