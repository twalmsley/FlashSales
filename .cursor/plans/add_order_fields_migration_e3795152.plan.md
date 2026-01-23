---
name: Add Order Fields Migration
overview: Add product_id, sold_price, and sold_quantity fields to the Order entity with a Flyway migration, update the entity class, and update seed data. Then run tests and fix any failures.
todos:
  - id: "1"
    content: Create V4__AddOrderFields.sql migration file with product_id, sold_price, and sold_quantity columns
    status: completed
  - id: "2"
    content: Update Order.java entity to add productId, soldPrice, and soldQuantity fields with proper JPA annotations
    status: completed
  - id: "3"
    content: Update Seed_All_Tables.sql to add product_id, sold_price, and sold_quantity values to all INSERT INTO orders statements
    status: completed
  - id: "4"
    content: Run all tests with mvn test and identify any failures
    status: completed
  - id: "5"
    content: Fix any test failures related to the Order entity changes
    status: completed
isProject: false
---

# Add Order Fields Migration

## Overview

Add three new fields to the Order entity: `product_id`, `sold_price`, and `sold_quantity`. This requires a database migration, entity updates, and seed data updates.

## Changes Required

### 1. Database Migration

Create a new Flyway migration file: `V4__AddOrderFields.sql`

The migration will add three columns to the `orders` table:

- `product_id UUID NOT NULL REFERENCES products(id)` - Foreign key to products table
- `sold_price DECIMAL(12, 2) NOT NULL` - Price at which the item was sold
- `sold_quantity INT NOT NULL` - Quantity sold in this order

**Note**: Since we're updating seed data, these columns can be NOT NULL from the start. If there were existing production data, we'd need a two-step migration (add nullable, backfill, then make NOT NULL).

### 2. Order Entity Updates

Update [`src/main/java/uk/co/aosd/flash/domain/Order.java`](src/main/java/uk/co/aosd/flash/domain/Order.java) to add:

- `productId` field: `UUID` with `@ManyToOne` relationship to `Product` (or `@Column` with foreign key - will use `@ManyToOne` for consistency with existing `flashSaleItem` relationship)
- `soldPrice` field: `BigDecimal` (matching the pattern used in `FlashSaleItem.salePrice` and `Product.basePrice`)
- `soldQuantity` field: `Integer`

Add appropriate JPA annotations:

- `@ManyToOne(fetch = FetchType.LAZY)` and `@JoinColumn` for productId
- `@Column` annotations for soldPrice and soldQuantity with appropriate column names

### 3. Seed Data Updates

Update [`src/test/resources/sql/Seed_All_Tables.sql`](src/test/resources/sql/Seed_All_Tables.sql) to include values for the new fields in all `INSERT INTO orders` statements.

For each order:

- `product_id`: Extract from the corresponding `flash_sale_item_id` by looking up which product is associated with that flash sale item in the seed data
- `sold_price`: Use the `sale_price` from the corresponding flash sale item
- `sold_quantity`: Use a reasonable value (typically 1, but can vary for testing purposes)

**Mapping reference from seed data:**

- Orders reference `flash_sale_item_id` values like `'bbbbbbbb-0000-0000-0000-000000000001'`
- Flash sale items have `product_id` values that can be looked up
- Flash sale items have `sale_price` values that should be used

### 4. Testing

Run all existing tests using Maven:

```bash
mvn test
```

Fix any test failures that occur due to:

- Entity mapping issues
- Seed data loading problems
- Any tests that create Order instances without the new fields

## Implementation Details

### Migration File Structure

The migration will follow the pattern of existing migrations (V1, V2, V3) and use PostgreSQL syntax.

### Entity Field Annotations

- `productId`: Use `@ManyToOne` relationship similar to `flashSaleItem` for consistency
- `soldPrice`: Use `@Column(name = "sold_price", nullable = false)` with `@Column(precision = 12, scale = 2)` for DECIMAL
- `soldQuantity`: Use `@Column(name = "sold_quantity", nullable = false)`

### Seed Data Strategy

For each order in the seed file, determine:

1. The `product_id` by tracing through the `flash_sale_item_id` to find the associated product
2. The `sold_price` from the flash sale item's `sale_price`
3. A reasonable `sold_quantity` (default to 1, but can vary for test scenarios)