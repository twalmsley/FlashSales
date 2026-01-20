ALTER TABLE products ADD COLUMN reserved_count INT NOT NULL DEFAULT 0 CHECK (reserved_count <= total_physical_stock);
