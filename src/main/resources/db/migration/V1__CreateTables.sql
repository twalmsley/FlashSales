-- 1. Enable UUID extension if you prefer UUIDs over Serial IDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Create Enums for Status Tracking
CREATE TYPE sale_status AS ENUM ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED');
CREATE TYPE order_status AS ENUM ('PENDING', 'PAID', 'FAILED', 'REFUNDED');

---

-- 3. Products Table (Master Catalog)
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_price DECIMAL(12, 2) NOT NULL,
    total_physical_stock INT NOT NULL DEFAULT 0 CHECK (total_physical_stock >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Flash Sales Table (Event Metadata)
CREATE TABLE flash_sales (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status sale_status DEFAULT 'DRAFT',
    CONSTRAINT valid_time_range CHECK (end_time > start_time)
);

-- 5. Flash Sale Items (Junction table for overlapping sales)
-- This table handles the specific inventory allocated to a specific event
CREATE TABLE flash_sale_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    flash_sale_id UUID NOT NULL REFERENCES flash_sales(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    allocated_stock INT NOT NULL CHECK (allocated_stock >= 0),
    sold_count INT NOT NULL DEFAULT 0 CHECK (sold_count <= allocated_stock),
    sale_price DECIMAL(12, 2) NOT NULL,
    
    -- Ensure a product isn't added to the same sale twice
    UNIQUE(flash_sale_id, product_id)
);

-- 6. Orders Table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL, -- In a real app, this would FK to a 'users' table
    flash_sale_item_id UUID NOT NULL REFERENCES flash_sale_items(id),
    status order_status DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Business Rule: One purchase per user per flash sale item
    UNIQUE(user_id, flash_sale_item_id)
);

---

-- 7. Performance & Discovery Indexes
-- Index for the "Discovery" use case (finding active sales)
CREATE INDEX idx_flash_sales_active ON flash_sales (status, start_time, end_time);

-- Index for the "Sync Worker" (reconciling sold counts)
CREATE INDEX idx_orders_sale_item ON orders (flash_sale_item_id);

-- 8. Audit Trigger (Update the 'updated_at' column automatically)
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_product_modtime 
    BEFORE UPDATE ON products 
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();

