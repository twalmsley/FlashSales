---
name: Bulk Order Consistency Test
overview: Create an API-level integration test that fires many concurrent orders at a sale and verifies database consistency, ensuring no oversold stock, no unfulfillable PAID orders, correct product stock updates, and accurate sale item sold counts.
todos:
  - id: create-test-class
    content: Create BulkOrderConsistencyTest class with @SpringBootTest, @Testcontainers, and container setup
    status: completed
  - id: setup-test-data
    content: Implement setup method to create products, flash sale, and sale items with test data
    status: completed
  - id: concurrent-orders
    content: Implement concurrent order creation using ExecutorService to fire many orders via MockMvc
    status: completed
  - id: async-wait
    content: Implement waiting logic for async processing (payment, dispatch, failed payment) using polling
    status: completed
  - id: consistency-queries
    content: "Create SQL queries/methods to check: oversold stock, unfulfillable PAID orders, product stock updates, sold_count accuracy"
    status: completed
  - id: assertions
    content: Implement assertions for all consistency checks and verify database integrity
    status: completed
isProject: false
---

# Bulk Order Consistency Test Plan

## Overview

Create a comprehensive API-level integration test that simulates a high-load flash sale scenario with concurrent order creation, then verifies database consistency across multiple dimensions.

## Test Structure

### Test Class: `BulkOrderConsistencyTest`

- Location: `src/test/java/uk/co/aosd/flash/controllers/BulkOrderConsistencyTest.java`
- Type: Full-stack integration test using `@SpringBootTest` with `@Testcontainers`
- Setup: PostgreSQL and RabbitMQ containers via `@ServiceConnection`

## Test Flow

### 1. Setup Phase

- Create test data:
  - Multiple products with sufficient physical stock
  - One active flash sale with multiple sale items (each product allocated to the sale)
  - Each sale item with limited allocated stock (e.g., 100 units per item)
- Configure payment service to have a realistic success rate (e.g., 80-90%) to simulate failures
- Capture initial state:
  - Product `total_physical_stock` values
  - Sale item `allocated_stock` and `sold_count` values

### 2. Concurrent Order Creation

- Fire many concurrent orders (e.g., 500-1000 orders) across all sale items
- Use `ExecutorService` with multiple threads to simulate real-world concurrency
- Each order targets a random sale item with random quantity (1-5 units)
- Use unique user IDs to avoid unique constraint violations
- Make HTTP requests via `MockMvc` to `/api/v1/clients/orders`
- Collect all created order IDs

### 3. Async Processing Wait

- Wait for RabbitMQ consumers to process all orders:
  - Payment processing (PENDING → PAID/FAILED)
  - Dispatch processing (PAID → DISPATCHED)
  - Failed payment handling (FAILED status + sold_count decrement)
- Use polling with timeout (e.g., Awaitility or manual polling with `Thread.sleep`)
- Wait until all orders reach terminal states: `PAID`, `FAILED`, `REFUNDED`, or `DISPATCHED`
- Ensure no orders remain in `PENDING` state

### 4. Consistency Verification Queries

Create SQL queries to verify data integrity:

#### Query 1: No Oversold Stock

```sql
SELECT fsi.id, fsi.allocated_stock, fsi.sold_count, 
       (fsi.sold_count - fsi.allocated_stock) as oversold
FROM flash_sale_items fsi
WHERE fsi.sold_count > fsi.allocated_stock;
```

Expected: No rows returned

#### Query 2: No Unfulfillable PAID Orders

```sql
SELECT o.id, o.status, o.sold_quantity, fsi.allocated_stock, fsi.sold_count,
       (fsi.allocated_stock - fsi.sold_count) as available_stock
FROM orders o
JOIN flash_sale_items fsi ON o.flash_sale_item_id = fsi.id
WHERE o.status = 'PAID'
  AND o.sold_quantity > (fsi.allocated_stock - fsi.sold_count);
```

Expected: No rows returned (all PAID orders should be fulfillable)

#### Query 3: Product Stock Correctly Updated for DISPATCHED Orders

```sql
SELECT p.id, p.total_physical_stock, 
       COALESCE(SUM(o.sold_quantity), 0) as total_dispatched_quantity,
       (initial_stock.total_physical_stock - COALESCE(SUM(o.sold_quantity), 0)) as expected_stock
FROM products p
LEFT JOIN orders o ON o.product_id = p.id AND o.status = 'DISPATCHED'
-- Compare with initial stock (would need to track this)
GROUP BY p.id, p.total_physical_stock;
```

Note: This query needs initial stock values captured in setup

#### Query 4: Sale Item Sold Count Matches Successful Orders

```sql
SELECT fsi.id, fsi.sold_count,
       COALESCE(SUM(CASE WHEN o.status IN ('PENDING', 'PAID', 'DISPATCHED') 
                    THEN o.sold_quantity ELSE 0 END), 0) as order_quantity_sum
FROM flash_sale_items fsi
LEFT JOIN orders o ON o.flash_sale_item_id = fsi.id
GROUP BY fsi.id, fsi.sold_count
HAVING fsi.sold_count != COALESCE(SUM(CASE WHEN o.status IN ('PENDING', 'PAID', 'DISPATCHED') 
                                      THEN o.sold_quantity ELSE 0 END), 0);
```

Expected: No rows returned (sold_count should match sum of successful order quantities)

### 5. Assertions

- Assert Query 1 returns no rows (no oversold stock)
- Assert Query 2 returns no rows (no unfulfillable PAID orders)
- Assert Query 3 shows correct product stock (initial stock - dispatched quantities)
- Assert Query 4 returns no rows (sold_count matches order quantities)
- Additional checks:
  - Total orders created matches expected count (accounting for failures)
  - Sum of all `sold_count` values across sale items doesn't exceed `allocated_stock`
  - Product `total_physical_stock` is non-negative

## Implementation Details

### Dependencies

- Use existing testcontainers setup (PostgreSQL, RabbitMQ, Redis)
- Add Awaitility dependency if not present (for async waiting)
- Use `@Autowired` repositories for direct database queries

### Key Files to Create/Modify

1. **New Test File**: `src/test/java/uk/co/aosd/flash/controllers/BulkOrderConsistencyTest.java`

   - Full-stack test with containers
   - Concurrent order creation logic
   - Async processing wait logic
   - Consistency verification methods

2. **SQL Query Helper** (optional): Create a helper class or methods for consistency queries

   - Can use `@Autowired JdbcTemplate` or repository methods
   - Or use native queries via repository interfaces

### Configuration

- Set payment service success rate via `@TestPropertySource` or application properties
- Configure reasonable timeouts for async processing (e.g., 30-60 seconds)

### Error Handling

- Handle expected exceptions (InsufficientStockException, SaleNotActiveException)
- Count successful vs failed order creations
- Log statistics for debugging

## Test Data Strategy

- Create 3-5 products with high physical stock (e.g., 1000 units each)
- Create 1 active flash sale
- Allocate moderate stock per sale item (e.g., 100 units) to ensure some orders fail
- Use time-based sale (start in past, end in future) to ensure it's active

## Performance Considerations

- Use thread pool with reasonable size (e.g., 20-50 threads)
- Consider using `CompletableFuture` for better async handling
- Add timing metrics to understand test performance