CREATE TABLE market.properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    location TEXT,
    asking_price NUMERIC(18,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    status VARCHAR(32) NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_properties_owner ON market.properties (owner_user_id);
CREATE INDEX idx_properties_status ON market.properties (status);
CREATE INDEX idx_properties_price ON market.properties (asking_price);

CREATE TABLE market.offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES market.properties(id),
    buyer_user_id UUID NOT NULL,
    seller_user_id UUID NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_offer_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_offers_property_id ON market.offers (property_id);
CREATE INDEX idx_offers_buyer_id ON market.offers (buyer_user_id);
CREATE INDEX idx_offers_status ON market.offers (status);
CREATE UNIQUE INDEX uq_offer_accepted_per_property
    ON market.offers (property_id)
    WHERE status = 'ACCEPTED';

CREATE TABLE market.offer_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offer_id UUID NOT NULL REFERENCES market.offers(id),
    event_type VARCHAR(64) NOT NULL,
    actor_user_id UUID NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_offer_events_offer_id ON market.offer_events (offer_id);

CREATE TABLE market.property_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES market.properties(id),
    offer_id UUID NOT NULL REFERENCES market.offers(id),
    buyer_user_id UUID NOT NULL,
    seller_user_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    reserved_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reservations_property_id ON market.property_reservations (property_id);
CREATE INDEX idx_reservations_status ON market.property_reservations (status);
CREATE UNIQUE INDEX uq_active_reservation_per_property
    ON market.property_reservations (property_id)
    WHERE status = 'ACTIVE';

CREATE TABLE market.property_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES market.properties(id),
    object_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (property_id, object_key)
);

CREATE INDEX idx_property_media_property_id ON market.property_media (property_id);
