---
name: Release unsold stock on sale completion
overview: When a sale is marked as complete, release unsold stock by reducing allocatedStock on sale items and decreasing product reservedCount by the difference between allocated and sold quantities.
todos:
  - id: update-complete-active-sales
    content: Modify FlashSalesService.completeActiveSales() to iterate through sale items and release unsold stock by reducing allocatedStock and product reservedCount
    status: completed
  - id: add-logging
    content: Add appropriate logging for stock release operations
    status: completed
  - id: todo-1769175340762-7tgif7idz
    content: Run all tests and fix any failures
    status: completed
isProject: false
---

# Release Unsold Stock on Sale Completion

## Overview

When a flash sale is completed, we need to release the unsold stock back to the available pool. This involves:

1. Calculating the difference between `allocatedStock` and `soldCount` for each sale item
2. Reducing `allocatedStock` to match `soldCount` (or reduce by the difference)
3. Decreasing the product's `reservedCount` by the same amount to free up stock for future sales

## Current State

- `FlashSalesService.completeActiveSales()` currently only marks sales as COMPLETED
- `findActiveSalesReadyToComplete()` already eagerly loads items and products via JOIN FETCH
- `FlashSaleItem` has `allocatedStock` and `soldCount` fields
- `Product` has `reservedCount` field that tracks stock reserved for active sales

## Implementation

### Modify `FlashSalesService.completeActiveSales()`

Update the method in [`src/main/java/uk/co/aosd/flash/services/FlashSalesService.java`](src/main/java/uk/co/aosd/flash/services/FlashSalesService.java) to:

1. After marking a sale as COMPLETED (line 192-193), iterate through the sale's items
2. For each `FlashSaleItem`:

   - Calculate: `difference = item.getAllocatedStock() - item.getSoldCount()`
   - If `difference > 0`:
     - Update `item.setAllocatedStock(item.getSoldCount())` to reduce allocated stock to match sold count
     - Get the associated product: `item.getProduct()`
     - Decrease product's reserved count: `product.setReservedCount(product.getReservedCount() - difference)`
     - Save both the item and product using `items.save(item)` and `products.save(product)`
     - Log the stock release for debugging

3. Handle edge cases:

   - Skip items where `difference <= 0` (all allocated stock was sold)
   - Ensure `reservedCount` doesn't go negative (add validation if needed)

### Code Structure

The logic should be added inside the loop that completes sales (after line 193), processing each completed sale's items:

```java
// After marking sale as COMPLETED
for (final FlashSaleItem item : sale.getItems()) {
    final int difference = item.getAllocatedStock() - item.getSoldCount();
    if (difference > 0) {
        // Reduce allocated stock to match sold count
        item.setAllocatedStock(item.getSoldCount());
        items.save(item);
        
        // Release reserved count from product
        final Product product = item.getProduct();
        final int newReservedCount = product.getReservedCount() - difference;
        product.setReservedCount(newReservedCount);
        products.save(product);
        
        log.debug("Released {} unsold units for product {} in sale {}", 
            difference, product.getId(), sale.getId());
    }
}
```

## Notes

- The transaction is already `@Transactional`, so all changes will be committed together
- Items and products are already loaded via JOIN FETCH, so no additional queries needed
- This ensures that unsold stock becomes available for allocation to new sales