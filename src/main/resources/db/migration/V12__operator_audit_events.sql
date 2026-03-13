CREATE TABLE IF NOT EXISTS ops.operator_audit_events (
    id UUID PRIMARY KEY,
    actor_user_id UUID NULL,
    actor_roles JSONB NULL,
    action_type TEXT NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id TEXT NULL,
    outcome TEXT NOT NULL,
    reason TEXT NULL,
    correlation_id TEXT NULL,
    request_path TEXT NULL,
    request_method TEXT NULL,
    metadata JSONB NULL,
    error_detail TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_operator_audit_actor_created
    ON ops.operator_audit_events (actor_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_operator_audit_resource_created
    ON ops.operator_audit_events (resource_type, resource_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_operator_audit_action_created
    ON ops.operator_audit_events (action_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_operator_audit_outcome_created
    ON ops.operator_audit_events (outcome, created_at DESC);
