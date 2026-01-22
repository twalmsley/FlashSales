CREATE VIEW remaining_active_stock AS 
SELECT 
    fs.id,
    fs.title,
    fs.start_time,
    fs.end_time,
    fsi.id as item_id,
    fsi.product_id,
    fsi.allocated_stock,
    fsi.sold_count,
    fsi.sale_price
FROM flash_sales fs
JOIN flash_sale_items fsi ON fs.id = fsi.flash_sale_id
WHERE 
    fs.status = 'ACTIVE'
    AND fsi.sold_count < fsi.allocated_stock;
