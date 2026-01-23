-- Seed data for all tables in the Flash Sales application
-- This file populates products, flash_sales, flash_sale_items, and orders
-- ============================================================================
-- 1. PRODUCTS
-- ============================================================================
-- Insert sample products into the master catalog
INSERT INTO products (
        id,
        name,
        description,
        base_price,
        total_physical_stock,
        reserved_count
    )
VALUES (
        '11111111-1111-1111-1111-111111111111',
        'Quantum X1 Smartphone',
        'Flagship smartphone with 200MP camera',
        899.99,
        500,
        50
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        'CloudTab Pro 11',
        'High-performance tablet for creators',
        649.00,
        300,
        30
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        'SonicBuds Elite',
        'Noise-cancelling wireless earbuds',
        149.50,
        1200,
        100
    ),
    (
        '44444444-4444-4444-4444-444444444444',
        'VoltBook Air 13',
        'Ultra-slim laptop with 20hr battery life',
        1099.00,
        150,
        20
    ),
    (
        '55555555-5555-5555-5555-555555555555',
        'AeroWatch Series 5',
        'Smartwatch with advanced health tracking',
        299.00,
        800,
        80
    ),
    (
        '66666666-6666-6666-6666-666666666666',
        'PixelFlow Monitor 27',
        '4K UHD professional color monitor',
        449.99,
        100,
        10
    ),
    (
        '77777777-7777-7777-7777-777777777777',
        'Titan Gaming Chair',
        'Ergonomic racing-style gaming chair',
        349.00,
        250,
        25
    ),
    (
        '88888888-8888-8888-8888-888888888888',
        'NovaPod Speaker',
        'Spatial audio smart home speaker',
        199.00,
        600,
        60
    ),
    (
        '99999999-9999-9999-9999-999999999999',
        'GamerPro Keyboard',
        'Mechanical RGB tactile keyboard',
        129.00,
        1000,
        100
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Precision Mouse Z',
        '16000 DPI wireless gaming mouse',
        79.99,
        1500,
        150
    ),
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        'StreamCam 4K',
        'Ultra-wide angle webcam for streaming',
        169.00,
        400,
        40
    ),
    (
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        'BaseLink Hub',
        '10-in-1 USB-C docking station',
        89.00,
        900,
        90
    ),
    (
        'dddddddd-dddd-dddd-dddd-dddddddddddd',
        'SecureDrive 2TB',
        'Encrypted external SSD',
        189.00,
        550,
        55
    ),
    (
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
        'PowerGrid 20K',
        '20000mAh fast-charging power bank',
        59.99,
        2000,
        200
    ),
    (
        'ffffffff-ffff-ffff-ffff-ffffffffffff',
        'ClearView VR Goggles',
        'Next-gen virtual reality headset',
        399.00,
        80,
        8
    );
-- ============================================================================
-- 2. FLASH SALES
-- ============================================================================
-- Insert flash sales with various statuses and dates
-- DRAFT sales (upcoming sales)
INSERT INTO flash_sales (id, title, start_time, end_time, status)
VALUES (
        'aaaaaaaa-0000-0000-0000-000000000001',
        'New Year Tech Sale',
        (CURRENT_TIMESTAMP + INTERVAL '2 days'),
        (
            CURRENT_TIMESTAMP + INTERVAL '2 days' + INTERVAL '4 hours'
        ),
        'DRAFT'
    ),
    (
        'aaaaaaaa-0000-0000-0000-000000000002',
        'Valentine''s Day Special',
        (CURRENT_TIMESTAMP + INTERVAL '5 days'),
        (
            CURRENT_TIMESTAMP + INTERVAL '5 days' + INTERVAL '6 hours'
        ),
        'DRAFT'
    ),
    (
        'aaaaaaaa-0000-0000-0000-000000000003',
        'Spring Electronics Blowout',
        (CURRENT_TIMESTAMP + INTERVAL '10 days'),
        (
            CURRENT_TIMESTAMP + INTERVAL '10 days' + INTERVAL '8 hours'
        ),
        'DRAFT'
    ),
    (
        'aaaaaaaa-0000-0000-0000-000000000004',
        'Weekend Flash Sale',
        (CURRENT_TIMESTAMP + INTERVAL '1 day'),
        (
            CURRENT_TIMESTAMP + INTERVAL '1 day' + INTERVAL '2 hours'
        ),
        'DRAFT'
    );
-- ACTIVE sales (currently running)
INSERT INTO flash_sales (id, title, start_time, end_time, status)
VALUES (
        'bbbbbbbb-0000-0000-0000-000000000001',
        'Holiday Mega Sale',
        (CURRENT_TIMESTAMP - INTERVAL '1 hour'),
        (CURRENT_TIMESTAMP + INTERVAL '3 hours'),
        'ACTIVE'
    ),
    (
        'bbbbbbbb-0000-0000-0000-000000000002',
        'Midday Tech Deals',
        (CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
        (
            CURRENT_TIMESTAMP + INTERVAL '2 hours 30 minutes'
        ),
        'ACTIVE'
    );
-- COMPLETED sales (past sales)
INSERT INTO flash_sales (id, title, start_time, end_time, status)
VALUES (
        'cccccccc-0000-0000-0000-000000000001',
        'Black Friday Extravaganza',
        (CURRENT_TIMESTAMP - INTERVAL '30 days'),
        (
            CURRENT_TIMESTAMP - INTERVAL '30 days' + INTERVAL '24 hours'
        ),
        'COMPLETED'
    ),
    (
        'cccccccc-0000-0000-0000-000000000002',
        'Cyber Monday Deals',
        (CURRENT_TIMESTAMP - INTERVAL '25 days'),
        (
            CURRENT_TIMESTAMP - INTERVAL '25 days' + INTERVAL '12 hours'
        ),
        'COMPLETED'
    ),
    (
        'cccccccc-0000-0000-0000-000000000003',
        'End of Year Clearance',
        (CURRENT_TIMESTAMP - INTERVAL '10 days'),
        (
            CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '6 hours'
        ),
        'COMPLETED'
    );
-- CANCELLED sales
INSERT INTO flash_sales (id, title, start_time, end_time, status)
VALUES (
        'dddddddd-0000-0000-0000-000000000001',
        'Cancelled Summer Sale',
        (CURRENT_TIMESTAMP + INTERVAL '20 days'),
        (
            CURRENT_TIMESTAMP + INTERVAL '20 days' + INTERVAL '4 hours'
        ),
        'CANCELLED'
    );
-- ============================================================================
-- 3. FLASH SALE ITEMS
-- ============================================================================
-- Link products to flash sales with allocated stock and sale prices
-- Items for DRAFT sales
INSERT INTO flash_sale_items (
        id,
        flash_sale_id,
        product_id,
        allocated_stock,
        sold_count,
        sale_price
    )
VALUES -- New Year Tech Sale (DRAFT)
    (
        '11111111-0000-0000-0000-000000000001',
        'aaaaaaaa-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        50,
        0,
        699.99
    ),
    (
        '11111111-0000-0000-0000-000000000002',
        'aaaaaaaa-0000-0000-0000-000000000001',
        '22222222-2222-2222-2222-222222222222',
        30,
        0,
        499.00
    ),
    (
        '11111111-0000-0000-0000-000000000003',
        'aaaaaaaa-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333',
        100,
        0,
        99.99
    ),
    -- Valentine's Day Special (DRAFT)
    (
        '22222222-0000-0000-0000-000000000001',
        'aaaaaaaa-0000-0000-0000-000000000002',
        '55555555-5555-5555-5555-555555555555',
        80,
        0,
        199.00
    ),
    (
        '22222222-0000-0000-0000-000000000002',
        'aaaaaaaa-0000-0000-0000-000000000002',
        '88888888-8888-8888-8888-888888888888',
        60,
        0,
        149.00
    ),
    -- Spring Electronics Blowout (DRAFT)
    (
        '33333333-0000-0000-0000-000000000001',
        'aaaaaaaa-0000-0000-0000-000000000003',
        '44444444-4444-4444-4444-444444444444',
        20,
        0,
        899.00
    ),
    (
        '33333333-0000-0000-0000-000000000002',
        'aaaaaaaa-0000-0000-0000-000000000003',
        '66666666-6666-6666-6666-666666666666',
        10,
        0,
        349.99
    ),
    (
        '33333333-0000-0000-0000-000000000003',
        'aaaaaaaa-0000-0000-0000-000000000003',
        'ffffffff-ffff-ffff-ffff-ffffffffffff',
        8,
        0,
        299.00
    ),
    -- Weekend Flash Sale (DRAFT)
    (
        '44444444-0000-0000-0000-000000000001',
        'aaaaaaaa-0000-0000-0000-000000000004',
        '99999999-9999-9999-9999-999999999999',
        100,
        0,
        89.00
    ),
    (
        '44444444-0000-0000-0000-000000000002',
        'aaaaaaaa-0000-0000-0000-000000000004',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        150,
        0,
        59.99
    );
-- Items for ACTIVE sales
INSERT INTO flash_sale_items (
        id,
        flash_sale_id,
        product_id,
        allocated_stock,
        sold_count,
        sale_price
    )
VALUES -- Holiday Mega Sale (ACTIVE)
    (
        'bbbbbbbb-0000-0000-0000-000000000001',
        'bbbbbbbb-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        100,
        45,
        749.99
    ),
    (
        'bbbbbbbb-0000-0000-0000-000000000002',
        'bbbbbbbb-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333',
        200,
        120,
        119.50
    ),
    (
        'bbbbbbbb-0000-0000-0000-000000000003',
        'bbbbbbbb-0000-0000-0000-000000000001',
        '77777777-7777-7777-7777-777777777777',
        50,
        25,
        279.00
    ),
    (
        'bbbbbbbb-0000-0000-0000-000000000004',
        'bbbbbbbb-0000-0000-0000-000000000001',
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
        300,
        180,
        39.99
    ),
    -- Midday Tech Deals (ACTIVE)
    (
        'bbbbbbbb-0000-0000-0000-000000000005',
        'bbbbbbbb-0000-0000-0000-000000000002',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        80,
        35,
        129.00
    ),
    (
        'bbbbbbbb-0000-0000-0000-000000000006',
        'bbbbbbbb-0000-0000-0000-000000000002',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        150,
        60,
        69.00
    ),
    (
        'bbbbbbbb-0000-0000-0000-000000000007',
        'bbbbbbbb-0000-0000-0000-000000000002',
        'dddddddd-dddd-dddd-dddd-dddddddddddd',
        100,
        40,
        149.00
    );
-- Items for COMPLETED sales
INSERT INTO flash_sale_items (
        id,
        flash_sale_id,
        product_id,
        allocated_stock,
        sold_count,
        sale_price
    )
VALUES -- Black Friday Extravaganza (COMPLETED)
    (
        'cccccccc-0000-0000-0000-000000000001',
        'cccccccc-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        200,
        200,
        649.99
    ),
    (
        'cccccccc-0000-0000-0000-000000000002',
        'cccccccc-0000-0000-0000-000000000001',
        '22222222-2222-2222-2222-222222222222',
        150,
        150,
        449.00
    ),
    (
        'cccccccc-0000-0000-0000-000000000003',
        'cccccccc-0000-0000-0000-000000000001',
        '44444444-4444-4444-4444-444444444444',
        50,
        50,
        899.00
    ),
    -- Cyber Monday Deals (COMPLETED)
    (
        'cccccccc-0000-0000-0000-000000000004',
        'cccccccc-0000-0000-0000-000000000002',
        '33333333-3333-3333-3333-333333333333',
        300,
        300,
        99.50
    ),
    (
        'cccccccc-0000-0000-0000-000000000005',
        'cccccccc-0000-0000-0000-000000000002',
        '55555555-5555-5555-5555-555555555555',
        200,
        200,
        199.00
    ),
    -- End of Year Clearance (COMPLETED)
    (
        'cccccccc-0000-0000-0000-000000000006',
        'cccccccc-0000-0000-0000-000000000003',
        '66666666-6666-6666-6666-666666666666',
        30,
        28,
        349.99
    ),
    (
        'cccccccc-0000-0000-0000-000000000007',
        'cccccccc-0000-0000-0000-000000000003',
        '88888888-8888-8888-8888-888888888888',
        100,
        95,
        149.00
    );
-- ============================================================================
-- 4. ORDERS
-- ============================================================================
-- Create sample orders for active and completed sales
-- Orders for ACTIVE sales
INSERT INTO orders (
        id,
        user_id,
        flash_sale_item_id,
        product_id,
        sold_price,
        sold_quantity,
        status,
        created_at
    )
VALUES -- Orders for Holiday Mega Sale (ACTIVE)
    (
        '11111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '10000000-0000-0000-0000-000000000001',
        'bbbbbbbb-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        749.99,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '45 minutes'
    ),
    (
        '11111111-aaaa-aaaa-aaaa-aaaaaaaaaaab',
        '10000000-0000-0000-0000-000000000002',
        'bbbbbbbb-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        749.99,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '40 minutes'
    ),
    (
        '11111111-aaaa-aaaa-aaaa-aaaaaaaaaaac',
        '10000000-0000-0000-0000-000000000003',
        'bbbbbbbb-0000-0000-0000-000000000002',
        '33333333-3333-3333-3333-333333333333',
        119.50,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '35 minutes'
    ),
    (
        '11111111-aaaa-aaaa-aaaa-aaaaaaaaaaad',
        '10000000-0000-0000-0000-000000000004',
        'bbbbbbbb-0000-0000-0000-000000000002',
        '33333333-3333-3333-3333-333333333333',
        119.50,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '30 minutes'
    ),
    (
        '11111111-aaaa-aaaa-aaaa-aaaaaaaaaaae',
        '10000000-0000-0000-0000-000000000005',
        'bbbbbbbb-0000-0000-0000-000000000003',
        '77777777-7777-7777-7777-777777777777',
        279.00,
        1,
        'PENDING',
        CURRENT_TIMESTAMP - INTERVAL '25 minutes'
    ),
    (
        '11111111-aaaa-aaaa-aaaa-aaaaaaaaaaaf',
        '10000000-0000-0000-0000-000000000006',
        'bbbbbbbb-0000-0000-0000-000000000004',
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
        39.99,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '20 minutes'
    ),
    (
        '11111111-aaaa-aaaa-aaaa-aaaaaaaa0010',
        '10000000-0000-0000-0000-000000000007',
        'bbbbbbbb-0000-0000-0000-000000000004',
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
        39.99,
        1,
        'FAILED',
        CURRENT_TIMESTAMP - INTERVAL '15 minutes'
    ),
    -- Orders for Midday Tech Deals (ACTIVE)
    (
        '22222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        '10000000-0000-0000-0000-000000000008',
        'bbbbbbbb-0000-0000-0000-000000000005',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        129.00,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '20 minutes'
    ),
    (
        '22222222-bbbb-bbbb-bbbb-bbbbbbbbbbbc',
        '10000000-0000-0000-0000-000000000009',
        'bbbbbbbb-0000-0000-0000-000000000005',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        129.00,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '18 minutes'
    ),
    (
        '22222222-bbbb-bbbb-bbbb-bbbbbbbbbbbd',
        '10000000-0000-0000-0000-000000000010',
        'bbbbbbbb-0000-0000-0000-000000000006',
        'cccccccc-cccc-cccc-cccc-cccccccccccc',
        69.00,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '10 minutes'
    ),
    (
        '22222222-bbbb-bbbb-bbbb-bbbbbbbbbbbe',
        '10000000-0000-0000-0000-000000000011',
        'bbbbbbbb-0000-0000-0000-000000000007',
        'dddddddd-dddd-dddd-dddd-dddddddddddd',
        149.00,
        1,
        'PENDING',
        CURRENT_TIMESTAMP - INTERVAL '5 minutes'
    );
-- Orders for COMPLETED sales (historical data)
INSERT INTO orders (
        id,
        user_id,
        flash_sale_item_id,
        product_id,
        sold_price,
        sold_quantity,
        status,
        created_at
    )
VALUES -- Orders for Black Friday Extravaganza (COMPLETED)
    (
        '33333333-cccc-cccc-cccc-cccccccccccc',
        '20000000-0000-0000-0000-000000000101',
        'cccccccc-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        649.99,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '30 days' + INTERVAL '1 hour'
    ),
    (
        '33333333-cccc-cccc-cccc-cccccccccccd',
        '20000000-0000-0000-0000-000000000102',
        'cccccccc-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        649.99,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '30 days' + INTERVAL '2 hours'
    ),
    (
        '33333333-cccc-cccc-cccc-ccccccccccce',
        '20000000-0000-0000-0000-000000000103',
        'cccccccc-0000-0000-0000-000000000002',
        '22222222-2222-2222-2222-222222222222',
        449.00,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '30 days' + INTERVAL '3 hours'
    ),
    -- Orders for Cyber Monday Deals (COMPLETED)
    (
        '44444444-dddd-dddd-dddd-dddddddddddd',
        '20000000-0000-0000-0000-000000000201',
        'cccccccc-0000-0000-0000-000000000004',
        '33333333-3333-3333-3333-333333333333',
        99.50,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '25 days' + INTERVAL '2 hours'
    ),
    (
        '44444444-dddd-dddd-dddd-ddddddddddde',
        '20000000-0000-0000-0000-000000000202',
        'cccccccc-0000-0000-0000-000000000005',
        '55555555-5555-5555-5555-555555555555',
        199.00,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '25 days' + INTERVAL '4 hours'
    ),
    -- Orders for End of Year Clearance (COMPLETED)
    (
        '55555555-eeee-eeee-eeee-eeeeeeeeeeee',
        '20000000-0000-0000-0000-000000000301',
        'cccccccc-0000-0000-0000-000000000006',
        '66666666-6666-6666-6666-666666666666',
        349.99,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '1 hour'
    ),
    (
        '55555555-eeee-eeee-eeee-eeeeeeeeeeef',
        '20000000-0000-0000-0000-000000000302',
        'cccccccc-0000-0000-0000-000000000007',
        '88888888-8888-8888-8888-888888888888',
        149.00,
        1,
        'PAID',
        CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '2 hours'
    ),
    (
        '55555555-eeee-eeee-eeee-eeeeeeeeeeed',
        '20000000-0000-0000-0000-000000000303',
        'cccccccc-0000-0000-0000-000000000007',
        '88888888-8888-8888-8888-888888888888',
        149.00,
        1,
        'REFUNDED',
        CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '3 hours'
    );
-- ============================================================================
-- Summary
-- ============================================================================
-- This seed file creates:
-- - 15 products
-- - 10 flash sales (4 DRAFT, 2 ACTIVE, 3 COMPLETED, 1 CANCELLED)
-- - 18 flash sale items
-- - 17 orders (11 for ACTIVE sales, 6 for COMPLETED sales)
--
-- The data includes:
-- - DRAFT sales with start times in the future (1-10 days ahead)
-- - ACTIVE sales currently running
-- - COMPLETED sales from the past
-- - CANCELLED sales
-- - Various order statuses (PAID, PENDING, FAILED, REFUNDED)
-- - Realistic sold_count values for active sales showing partial sales