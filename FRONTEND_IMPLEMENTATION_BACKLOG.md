# FRONTEND_IMPLEMENTATION_BACKLOG

## Auth / Navigation

### Task: Route Main Sidebar To Actionable Module Pages
- Why it matters: users currently hit placeholder routes and miss implemented forms/actions.
- Backend dependency: none.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/components/layout/Sidebar.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/dashboard/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/marketplace/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/escrows/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/construction/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/admin/page.tsx`
- Priority: **P0**

### Task: Consolidate `/admin` and `/app/admin` Into One Admin UX
- Why it matters: admin monitoring tools are discoverability-fragmented.
- Backend dependency: none.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/admin/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/admin/page.tsx`
- Priority: **P0**

## Marketplace

### Task: Add Offer Counter Flow
- Why it matters: seller negotiation loop is incomplete without countering.
- Backend dependency: `POST /market/offers/{offerId}/counter` already available.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/marketplace/offers/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/schemas.ts`
- Priority: **P0**

### Task: Add Reservation Cancellation Action
- Why it matters: reservation lifecycle control is backend-supported but unusable in UI.
- Backend dependency: `POST /market/reservations/{reservationId}/cancel` already available.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/marketplace/reservations/page.tsx`
- Priority: **P0**

### Task: Add Property Edit Form
- Why it matters: property management is incomplete without update.
- Backend dependency: `PUT /market/properties/{propertyId}` already available.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/marketplace/properties/[id]/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/marketplace/properties/[id]/edit/page.tsx` (new)
- Priority: **P1**

## Construction

### Task: Fix Milestone Evidence Submit Payload Contract
- Why it matters: current submit action likely fails due request shape mismatch.
- Backend dependency: `POST /build/milestones/{milestoneId}/submit` expects `{ evidence, notes }`.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`
- Priority: **P0**

### Task: Add Project Assign Participants Flow
- Why it matters: projects cannot proceed to active state without contractor/inspector assignment.
- Backend dependency: `POST /build/projects/{projectId}/assign`.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`
- Priority: **P0**

### Task: Add Project Activation Action
- Why it matters: owners need a visible path to activate draft projects.
- Backend dependency: `POST /build/projects/{projectId}/activate`.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`
- Priority: **P0**

### Task: Add Milestone Creation Form
- Why it matters: milestone lifecycle currently depends on preseeded data.
- Backend dependency: `POST /build/projects/{projectId}/milestones`.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/components/construction/` (new form component)
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`
- Priority: **P0**

### Task: Add Inspection Completion Flow
- Why it matters: inspection workflow remains half-implemented; payout trigger path not fully operable in UI.
- Backend dependency: `POST /build/inspections/{inspectionId}/complete`.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
- Priority: **P1**

### Task: Harden Multi-Party Approval UX
- Why it matters: current split dialog likely causes invalid requests and investor-demo friction.
- Backend dependency: `POST /build/milestones/{id}/approve-multi` expects complete split math and business keys.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/schemas.ts`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/components/construction/MilestoneTimeline.tsx`
- Priority: **P1**

## Escrow / Ops Monitoring

### Task: Add Better Ops Table Filters and Drill-Down
- Why it matters: pilot/prod ops need faster triage for outbox/webhook/ledger anomalies.
- Backend dependency: existing list endpoints; optional future query params.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/admin/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/components/tables/DataTable.tsx`
- Priority: **P1**

### Task: Add Escrow/Disbursement Actionable Diagnostics
- Why it matters: detail views are read-only and lack quick troubleshooting context.
- Backend dependency: existing read endpoints in ops module.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(app)/app/escrows/[id]/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/components/escrow/EscrowTimeline.tsx`
- Priority: **P2**

## Demo Tools

### Task: Expose Separate Demo Reset and Seed Controls
- Why it matters: ops/devs may want reset-only or seed-only runs while troubleshooting.
- Backend dependency: `/admin/demo/reset` and `/admin/demo/seed` already exist.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/components/demo/DemoSetupButton.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/lib/api/endpoints.ts`
- Priority: **P1**

### Task: Clarify Demo Role Access and Route Map In UI
- Why it matters: seeded users can miss implemented flows due route split and role gating.
- Backend dependency: none.
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/components/demo/DemoQuickStartCard.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/README.md`
- Priority: **P2**

## Public Site

### Task: Align Public Listing UX With Backend Security Policy
- Why it matters: current public pages suggest anonymous access while backend requires auth.
- Backend dependency: decision required (open read endpoints or require login-only UX).
- Suggested frontend files to add/change:
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(public)/properties/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(public)/property/[id]/page.tsx`
  - `/Users/shabani.chande/Documents/projects/UjenziTrust-core/UjenziTrust/apps/web/app/(public)/page.tsx`
- Priority: **P1**
