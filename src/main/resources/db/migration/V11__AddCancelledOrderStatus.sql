-- Add CANCELLED status to order_status enum (user-initiated cancel of PENDING order)
ALTER TYPE order_status ADD VALUE 'CANCELLED';
