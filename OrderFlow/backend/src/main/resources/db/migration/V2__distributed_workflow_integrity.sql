ALTER TABLE payments ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS inventory_reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    status VARCHAR(32) NOT NULL CHECK (status IN ('RESERVED', 'RELEASED', 'CONSUMED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (order_id, organization_id, product_id)
);

ALTER TABLE inventory ADD CONSTRAINT inventory_nonnegative_quantities
    CHECK (available_quantity >= 0 AND reserved_quantity >= 0 AND sold_quantity >= 0);
ALTER TABLE orders ADD CONSTRAINT orders_valid_status
    CHECK (status IN ('PENDING', 'INVENTORY_RESERVED', 'PAYMENT_PENDING', 'CONFIRMED', 'CANCELLED'));

CREATE INDEX IF NOT EXISTS idx_inventory_reservations_order_tenant
    ON inventory_reservations(order_id, organization_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_order_tenant
    ON payments(order_id, organization_id);
CREATE INDEX IF NOT EXISTS idx_processed_events_consumer
    ON processed_events(consumer_name, processed_at);

ALTER TABLE processed_events DROP CONSTRAINT IF EXISTS processed_events_event_id_key;
ALTER TABLE processed_events ADD CONSTRAINT processed_events_event_consumer_key UNIQUE (event_id, consumer_name);
