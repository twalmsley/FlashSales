-- Recreate remaining_active_stock view to include product details (name, description, base_price).
DROP VIEW IF EXISTS remaining_active_stock;

CREATE VIEW remaining_active_stock AS
SELECT
    fs.id,
    fs.title,
    fs.start_time,
    fs.end_time,
    fsi.id AS item_id,
    fsi.product_id,
    fsi.allocated_stock,
    fsi.sold_count,
    fsi.sale_price,
    p.name AS product_name,
    p.description AS product_description,
    p.base_price AS base_price
FROM flash_sales fs
JOIN flash_sale_items fsi ON fs.id = fsi.flash_sale_id
JOIN products p ON fsi.product_id = p.id
WHERE
    fs.status = 'ACTIVE'
    AND fsi.sold_count < fsi.allocated_stock;
