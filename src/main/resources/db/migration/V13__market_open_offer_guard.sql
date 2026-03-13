create unique index if not exists uq_market_offers_open_offer_per_buyer_property
    on market.offers (property_id, buyer_user_id)
    where status in ('SUBMITTED', 'COUNTERED');
