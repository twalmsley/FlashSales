-- Add new fields to orders table: product_id, sold_price, and sold_quantity
ALTER TABLE orders 
    ADD COLUMN product_id UUID NOT NULL REFERENCES products(id),
    ADD COLUMN sold_price DECIMAL(12, 2) NOT NULL,
    ADD COLUMN sold_quantity INT NOT NULL;

-- Add index for product_id lookups
CREATE INDEX idx_orders_product_id ON orders (product_id);
