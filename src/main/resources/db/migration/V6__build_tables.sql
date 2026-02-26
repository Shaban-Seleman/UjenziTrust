CREATE TABLE build.projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL,
    contractor_user_id UUID,
    inspector_user_id UUID,
    escrow_id UUID REFERENCES ops.escrows(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    retention_rate NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_owner ON build.projects (owner_user_id);
CREATE INDEX idx_projects_status ON build.projects (status);

CREATE TABLE build.milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES build.projects(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sequence_no INT NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    retention_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    due_date DATE,
    paid_at TIMESTAMPTZ,
    retention_release_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_milestone_amount_positive CHECK (amount > 0),
    CONSTRAINT uq_milestone_project_sequence UNIQUE (project_id, sequence_no)
);

CREATE INDEX idx_milestones_project_id ON build.milestones (project_id);
CREATE INDEX idx_milestones_status ON build.milestones (status);
CREATE INDEX idx_milestones_retention_due ON build.milestones (retention_release_at, status);

CREATE TABLE build.milestone_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    milestone_id UUID NOT NULL REFERENCES build.milestones(id),
    submitted_by UUID NOT NULL,
    evidence JSONB NOT NULL,
    notes TEXT,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(32) NOT NULL
);

CREATE INDEX idx_submissions_milestone_id ON build.milestone_submissions (milestone_id);

CREATE TABLE build.inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES build.projects(id),
    milestone_id UUID REFERENCES build.milestones(id),
    inspector_user_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    scheduled_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    report JSONB,
    fee_amount NUMERIC(18,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inspections_project_id ON build.inspections (project_id);
CREATE INDEX idx_inspections_status ON build.inspections (status);

CREATE TABLE build.milestone_payout_splits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    milestone_id UUID NOT NULL REFERENCES build.milestones(id),
    payee_type VARCHAR(64) NOT NULL,
    payee_id UUID NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    business_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_split_amount_positive CHECK (amount > 0),
    CONSTRAINT uq_split_business_key UNIQUE (business_key)
);

CREATE INDEX idx_splits_milestone_id ON build.milestone_payout_splits (milestone_id);

CREATE TABLE build.change_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES build.projects(id),
    requested_by UUID NOT NULL,
    amount_delta NUMERIC(18,2) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_change_orders_project_id ON build.change_orders (project_id);
CREATE INDEX idx_change_orders_status ON build.change_orders (status);

ALTER TABLE ops.disbursement_orders
    ADD CONSTRAINT fk_disbursement_milestone
    FOREIGN KEY (milestone_id) REFERENCES build.milestones(id);
