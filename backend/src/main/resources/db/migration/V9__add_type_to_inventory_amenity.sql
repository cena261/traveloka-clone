-- inventory.V9__add_type_to_inventory_amenity.sql
ALTER TABLE inventory.amenity
    ADD COLUMN IF NOT EXISTS type VARCHAR(30);

-- (tuỳ chọn) fill default
UPDATE inventory.amenity SET type = 'PROPERTY' WHERE type IS NULL;
