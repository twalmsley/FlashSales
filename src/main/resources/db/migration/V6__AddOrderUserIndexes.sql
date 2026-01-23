-- Add index on user_id for efficient user order queries
CREATE INDEX idx_orders_user_id ON orders (user_id);

-- Add composite index for filtered queries (status and date range)
CREATE INDEX idx_orders_user_status_created ON orders (user_id, status, created_at);
