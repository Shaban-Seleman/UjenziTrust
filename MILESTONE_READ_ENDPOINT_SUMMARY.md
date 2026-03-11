# Milestone Read Endpoint Summary

## Endpoint Added

- `GET /build/milestones/{milestoneId}`

This makes milestone a first-class readable resource without adding milestone edit semantics.

## Access Behavior

- `OWNER` can read milestones for projects they own
- `CONTRACTOR` can read milestones for projects they are assigned to
- `INSPECTOR` can read milestones for projects they are assigned to
- `ADMIN` can read milestones through the existing project access allowance
- Unauthorized actors receive `403`
- Missing milestones return `404`

The new read path reuses the existing project access rule in `MilestoneService`.

## Frontend Usage Updated

The frontend still uses the project-scoped milestone list for the main project page and timeline.

The new single-milestone read endpoint is now used only where it improves freshness and clarity:

- milestone evidence dialog
- multi-party split approval dialog

Those dialogs can now fetch the selected milestone directly and show fresh milestone-specific details without refetching the full module first.

## Why PATCH Was Not Added

`PATCH`/`PUT /build/milestones/{milestoneId}` was intentionally not added to keep scope tight and avoid introducing new edit workflow, validation, and authorization semantics.

The goal of this change is read clarity only:

- direct milestone fetches
- better dialog freshness
- cleaner path for future deep links or detail views
