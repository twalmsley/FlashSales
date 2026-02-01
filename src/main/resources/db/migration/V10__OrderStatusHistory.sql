-- Order status history: when each status change happened and who changed it (if admin)
CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status order_status NOT NULL,
    to_status order_status NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by_user_id UUID
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history (order_id);
