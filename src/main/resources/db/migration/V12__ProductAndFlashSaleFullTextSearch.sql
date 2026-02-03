-- Full-text search support for products (name + description) and flash_sales (title).
-- Adds tsvector columns with GIN indexes and triggers to keep them updated.

-- Products: search_vector from name (weight A) and description (weight B)
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(name, '')), 'A')
        || setweight(to_tsvector('english', coalesce(description, '')), 'B')
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_products_search_vector ON products USING GIN (search_vector);

-- Flash sales: search_vector from title
ALTER TABLE flash_sales
    ADD COLUMN IF NOT EXISTS search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('english', coalesce(title, ''))) STORED;

CREATE INDEX IF NOT EXISTS idx_flash_sales_search_vector ON flash_sales USING GIN (search_vector);
