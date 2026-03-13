# Wave 2 Remediation v2 Summary

## Features Implemented

### 1) Inspection completion UI
- Added inspector-facing inspection actions on the construction project detail page:
  - schedule inspection
  - complete inspection as `PASS` or `FAIL`
- Completion dialog now sends the backend-required `reportJson` payload.
- Project and milestone queries refetch after schedule/complete so milestone status updates immediately.
- Because backend still lacks an inspection read/list endpoint, the UI caches scheduled inspection IDs per milestone for the current session and supports manual inspection ID entry as a fallback.

### 2) Property edit UI
- Added property editing to the property detail page with:
  - prefilled edit dialog
  - React Hook Form + Zod validation
  - real backend `PUT` update call
  - success/error toasts and query refresh
- Edit action is role-gated to `SELLER`, `OWNER`, and `ADMIN`.
- Edit button is disabled when the property is not in an editable frontend-approved state (`DRAFT` or `PUBLISHED`).

### 3) Multi-party milestone approval UX hardening
- Hardened the split payout dialog to show:
  - gross amount
  - retention amount
  - net allocatable amount
  - supplier allocation
  - inspector allocation
  - contractor remainder
  - remaining allocatable balance
- Added live reconciliation and blocked submit when splits exceed the milestone net amount.
- Supplier and inspector payees now use directory-backed selectors instead of raw UUID entry.

### 4) Participant selection UX improvement
- Confirmed backend supports role-filtered user selection via `/users/directory`.
- Kept and improved the existing selection flow:
  - clearer helper text
  - active-role dropdowns
  - loading/empty/error guidance
  - readable option labels (`email/phone + role`)

### 5) Property browsing cleanup
- Revalidated backend auth policy: property browsing remains authenticated-only.
- Public property routes now redirect directly to login with a preserved `next` destination:
  - `/properties`
  - `/property/[id]`
- Public landing page CTA now points to the authenticated marketplace flow.

## Routes / Pages Affected
- `apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
- `apps/web/app/(app)/app/marketplace/properties/[id]/page.tsx`
- `apps/web/app/(public)/page.tsx`
- `apps/web/app/(public)/properties/page.tsx`
- `apps/web/app/(public)/property/[id]/page.tsx`

## Backend Endpoints Used
- `GET /build/projects/{projectId}`
- `GET /build/projects/{projectId}/milestones`
- `GET /build/milestones/{milestoneId}`
- `POST /build/inspections/schedule`
- `POST /build/inspections/{inspectionId}/complete`
- `POST /build/milestones/{milestoneId}/approve-multi`
- `POST /build/projects/{projectId}/assign`
- `GET /users/directory?role=CONTRACTOR|INSPECTOR|SUPPLIER`
- `GET /market/properties/{propertyId}`
- `PUT /market/properties/{propertyId}`
- `POST /market/properties/{propertyId}/publish`

## UX Improvements Made
- Added loading/disabled states to new property edit, inspection, and split approval actions.
- Preserved backend `Detail` messages in toasts.
- Added inline validation feedback for property edit and inspection completion.
- Added clearer payout math and participant selection guidance on construction flows.

## Verification
- `npm run build` passed in `apps/web`
- `npm run typecheck` passed in `apps/web`

## Remaining Gaps After Wave 2
- Backend still has no inspection read/list endpoint by project or milestone, so inspection completion cannot be fully discovery-driven after page reload.
- Inspection result data (`PASS` vs `FAIL`) is not exposed back through milestone/project read payloads, so the timeline can only reflect the milestone status transition to `INSPECTED`.
- Property create/edit still use separate forms; a shared marketplace property form component would be a reasonable Wave 3 cleanup.
