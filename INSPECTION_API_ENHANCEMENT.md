# Inspection API Enhancement

## New Backend Endpoints

### `GET /build/milestones/{milestoneId}/inspections`
Returns the inspections attached to a milestone, newest first.

Response fields:
- `inspectionId`
- `milestoneId`
- `scheduledAt`
- `inspectorId`
- `status`
- `result`
- `reportJson`
- `completedAt`
- `createdAt`

Authorization:
- `OWNER`
- `CONTRACTOR`
- `INSPECTOR`
- `ADMIN`

### `GET /build/inspections/{inspectionId}`
Returns a single inspection using the same response shape as the list endpoint.

Behavior:
- `404` if the inspection does not exist
- `403` if the actor cannot access the associated project

## Milestone Response Enhancements

Milestone responses now include the latest inspection summary fields:
- `inspectionStatus`
- `inspectionResult`
- `inspectionCompletedAt`
- `inspectionId`

These values are populated from the most recent inspection for the milestone.

## Frontend Changes

The frontend no longer depends on an in-memory inspection ID cache.

Updated flow:
1. Schedule inspection
2. Read inspections from `GET /build/milestones/{milestoneId}/inspections`
3. Complete the latest or selected inspection
4. Refetch milestone/project queries so summary fields update the timeline

Timeline improvements:
- inspection result badge (`PASS` / `FAIL`)
- inspection completion timestamp

## Notes

- Inspection read APIs reuse existing project access rules.
- Milestone summary enrichment is implemented without heavy joins by looking up the latest inspection per milestone.
- The backend still does not expose a broader project-level inspection list endpoint; this enhancement is milestone-scoped by design.
