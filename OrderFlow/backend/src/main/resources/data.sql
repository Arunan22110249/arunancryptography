MERGE INTO organizations AS target
USING (
  VALUES
    ('11111111-1111-1111-1111-111111111111', 'Acme Retail', 'acme', CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'Northwind Labs', 'northwind', CURRENT_TIMESTAMP)
) AS src(id, name, slug, created_at)
ON target.id = src.id
WHEN NOT MATCHED THEN
  INSERT (id, name, slug, created_at)
  VALUES (src.id, src.name, src.slug, src.created_at);

MERGE INTO users AS target
USING (
  VALUES
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'admin@acme.test', 'Platform Admin', 'ADMIN', '$2b$10$ZjmElwvXz9cXZI3KOOy8f.Mo2w4GVUHoWq0GN5QPvg5MU5ltI9.xa', CURRENT_TIMESTAMP),
    ('44444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', 'customer@acme.test', 'Retail Customer', 'CUSTOMER', '$2b$10$ZjmElwvXz9cXZI3KOOy8f.Mo2w4GVUHoWq0GN5QPvg5MU5ltI9.xa', CURRENT_TIMESTAMP),
    ('55555555-5555-5555-5555-555555555555', '22222222-2222-2222-2222-222222222222', 'ops@northwind.test', 'Ops User', 'OPERATOR', '$2b$10$ZjmElwvXz9cXZI3KOOy8f.Mo2w4GVUHoWq0GN5QPvg5MU5ltI9.xa', CURRENT_TIMESTAMP)
) AS src(id, organization_id, email, full_name, role, password_hash, created_at)
ON target.organization_id = src.organization_id AND target.email = src.email
WHEN NOT MATCHED THEN
  INSERT (id, organization_id, email, full_name, role, password_hash, created_at)
  VALUES (src.id, src.organization_id, src.email, src.full_name, src.role, src.password_hash, src.created_at);

MERGE INTO products AS target
USING (
  VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'SKU-1001', 'Aurora Lamp', 'home', 129.99, true, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'SKU-1002', 'Nimbus Chair', 'furniture', 249.00, true, CURRENT_TIMESTAMP),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '11111111-1111-1111-1111-111111111111', 'SKU-1003', 'Coastal Mug', 'kitchen', 18.50, true, CURRENT_TIMESTAMP),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', '22222222-2222-2222-2222-222222222222', 'SKU-2001', 'Signal Watch', 'wearables', 199.00, true, CURRENT_TIMESTAMP)
) AS src(id, organization_id, sku, name, category, price, active, created_at)
ON target.organization_id = src.organization_id AND target.sku = src.sku
WHEN NOT MATCHED THEN
  INSERT (id, organization_id, sku, name, category, price, active, created_at)
  VALUES (src.id, src.organization_id, src.sku, src.name, src.category, src.price, src.active, src.created_at);

MERGE INTO inventory AS target
USING (
  VALUES
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 100, 0, 0, 0, CURRENT_TIMESTAMP),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 50, 0, 0, 0, CURRENT_TIMESTAMP),
    ('12121212-1212-1212-1212-121212121212', '11111111-1111-1111-1111-111111111111', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 75, 0, 0, 0, CURRENT_TIMESTAMP),
    ('13131313-1313-1313-1313-131313131313', '22222222-2222-2222-2222-222222222222', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 30, 0, 0, 0, CURRENT_TIMESTAMP)
) AS src(id, organization_id, product_id, available_quantity, reserved_quantity, sold_quantity, version, updated_at)
ON target.organization_id = src.organization_id AND target.product_id = src.product_id
WHEN NOT MATCHED THEN
  INSERT (id, organization_id, product_id, available_quantity, reserved_quantity, sold_quantity, version, updated_at)
  VALUES (src.id, src.organization_id, src.product_id, src.available_quantity, src.reserved_quantity, src.sold_quantity, src.version, src.updated_at);
