# Wave 1 Residual Fixes Summary

## Scope Completed
Implemented the scoped Wave 1 residual cleanup in `apps/web`:

1. Participant assignment UX hardening (construction project detail)
2. Public vs private property browsing resolution
3. Investor demo friction reduction across entry/navigation points

No auth architecture changes were made.

## What Was Resolved

### 1) Participant Assignment UX (manual UUID fallback improved)
- Updated assignment dialog in:
  - `apps/web/app/(app)/app/construction/projects/[id]/page.tsx`
- Improvements:
  - Clear explanation that backend currently requires direct user IDs
  - Explicit field labels for contractor and inspector IDs
  - UUID placeholder example and expected format helper text
  - Inline validation errors from Zod/RHF
  - Context panel showing current owner/contractor/inspector IDs for copy/reference
  - Prefill form from current project assignment when dialog opens

### 2) Public vs Private Property Browsing Decision
- Backend policy confirmed from security config:
  - `src/main/java/com/uzenjitrust/common/config/SecurityConfig.java`
  - Requests are authenticated by default (`anyRequest().authenticated()`), with explicit public exceptions only (auth login/docs/health/webhooks).
- Final frontend decision: **canonical property browsing is authenticated-only**.
- Implemented by redirecting public listing/detail routes to canonical authenticated marketplace paths:
  - `apps/web/app/(public)/properties/page.tsx` -> `/app/marketplace/properties`
  - `apps/web/app/(public)/property/[id]/page.tsx` -> `/app/marketplace/properties/{id}`
- This removes misleading pseudo-public pages that would fail at runtime for anonymous users.

### 3) Investor Demo Friction Reduction
- Updated public landing copy and CTA in:
  - `apps/web/app/(public)/page.tsx`
- Changes:
  - Added explicit note that marketplace browsing requires sign-in
  - Replaced ambiguous “Browse Listings” CTA with “Go to Marketplace” linking to canonical path

## Backend Support Check Result (Participant Lookup)
- User discovery/search endpoint for assignment was checked and **not found** in backend controllers.
- Current backend endpoints support auth (`/auth/login`, `/auth/me`) but do not expose a role-filtered or general user list/search API.
- Result: retained manual-ID assignment with improved usability instead of fake/mock picker behavior.

## Verification
- `npm run build` passed in `apps/web`.
- `npm run typecheck` passed in `apps/web`.

## Remaining Limitations
- Contractor/inspector selection still depends on known user UUIDs until backend adds a user lookup/search endpoint suitable for assignment workflows.
