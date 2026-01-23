---
name: Flash Sale Cancellation API
overview: Add API endpoint to cancel flash sales (DRAFT or ACTIVE status) with proper stock release handling. The endpoint will be POST /api/v1/admin/flash_sale/{id}/cancel and will release reserved stock similar to the completion logic.
todos:
  - id: "1"
    content: Create FlashSaleNotFoundException exception class following existing exception patterns
    status: completed
  - id: "2"
    content: Add cancelFlashSale() method to FlashSalesService with stock release logic for DRAFT and ACTIVE sales
    status: completed
  - id: "3"
    content: Add POST /api/v1/admin/flash_sale/{id}/cancel endpoint to FlashSaleAdminRestApi controller
    status: completed
  - id: "4"
    content: Add exception handler for FlashSaleNotFoundException in GlobalExceptionHandler
    status: completed
isProject: false
---

# Flash Sale Cancellation Feature

## Overview

Add an admin API endpoint to cancel flash sales. The architecture already supports the `CANCELLED` status, but there's no API endpoint to trigger the cancellation. When cancelling, we need to properly release reserved stock.

## Architecture Context

From the codebase analysis:

- **Status transitions**: `DRAFT` → `CANCELLED` and `ACTIVE` → `CANCELLED` are valid (per architecture diagram)
- **Stock management**: 
  - DRAFT sales: All `allocatedStock` is reserved in `products.reservedCount`
  - ACTIVE sales: `soldCount` items have been sold, unsold stock = `allocatedStock - soldCount`
- **Stock release pattern**: Similar to `completeActiveSales()` in `FlashSalesService`:
  - Calculate unsold stock: `difference = allocatedStock - soldCount`
  - Update `allocatedStock = soldCount` (or 0 for DRAFT)
  - Decrement `products.reservedCount` by the difference

## Implementation Plan

### 1. Create FlashSaleNotFoundException

**File**: `src/main/java/uk/co/aosd/flash/exc/FlashSaleNotFoundException.java`

- Follow pattern of `OrderNotFoundException` and `ProductNotFoundException`
- Include sale ID in constructor and getter
- Extend `RuntimeException`

### 2. Add cancelFlashSale Method to FlashSalesService

**File**: `src/main/java/uk/co/aosd/flash/services/FlashSalesService.java`

- Method signature: `public void cancelFlashSale(UUID saleId)`
- Transactional method
- Logic:

  1. Find sale by ID with items eagerly loaded (use repository method or fetch join)
  2. Throw `FlashSaleNotFoundException` if not found
  3. Validate status: Only `DRAFT` or `ACTIVE` can be cancelled

     - If `COMPLETED`: throw `IllegalArgumentException` with message
     - If already `CANCELLED`: throw `IllegalArgumentException` with message

  1. Release stock for each sale item:

     - **For DRAFT sales**: Release all `allocatedStock` (difference = allocatedStock - 0)
     - **For ACTIVE sales**: Release unsold stock (difference = allocatedStock - soldCount)
     - Update `allocatedStock` to `soldCount` (0 for DRAFT)
     - Decrement `products.reservedCount` by difference

  1. Set sale status to `CANCELLED`
  2. Save sale and items/products

### 3. Add Cancel Endpoint to FlashSaleAdminRestApi

**File**: `src/main/java/uk/co/aosd/flash/controllers/FlashSaleAdminRestApi.java`

- Add `@PostMapping("/flash_sale/{id}/cancel")` method
- Use `@PathVariable String id` (convert to UUID)
- Call `service.cancelFlashSale(UUID.fromString(id))`
- Return `ResponseEntity.ok()` or `ResponseEntity.noContent()` on success
- Let exceptions bubble up to `GlobalExceptionHandler`

### 4. Add Exception Handler for FlashSaleNotFoundException

**File**: `src/main/java/uk/co/aosd/flash/errorhandling/GlobalExceptionHandler.java`

- Add `@ExceptionHandler(FlashSaleNotFoundException.class)`
- Return `HttpStatus.NOT_FOUND` (404)
- Follow pattern of `handleOrderNotFoundException`

### 5. Add Exception Handler for Invalid Sale Status

**File**: `src/main/java/uk/co/aosd/flash/errorhandling/GlobalExceptionHandler.java`

- The existing `handleIllegalArgumentException` should already handle status validation errors
- Verify it returns appropriate status code (BAD_REQUEST)

## Key Implementation Details

### Stock Release Logic

The stock release follows the same pattern as `completeActiveSales()`:

```java
for (FlashSaleItem item : sale.getItems()) {
    int difference = item.getAllocatedStock() - item.getSoldCount();
    if (difference > 0) {
        item.setAllocatedStock(item.getSoldCount());
        items.save(item);
        
        Product product = item.getProduct();
        product.setReservedCount(product.getReservedCount() - difference);
        products.save(product);
    }
}
```

### Status Validation

- **DRAFT**: Can cancel → Release all allocated stock
- **ACTIVE**: Can cancel → Release unsold stock (allocatedStock - soldCount)
- **COMPLETED**: Cannot cancel → Return error
- **CANCELLED**: Already cancelled → Return error

### Repository Query

Need to ensure sale is loaded with items and products. Options:

1. Use `findById()` and rely on `@OneToMany(fetch = FetchType.EAGER)` in `FlashSale` entity
2. Create custom repository method with JOIN FETCH (like `findDraftSalesReadyToActivate`)
3. Use `findById()` and manually load items if needed

The `FlashSale` entity already has `@OneToMany(fetch = FetchType.EAGER)`, so `findById()` should work.

## Files to Modify

1. **Create**: `src/main/java/uk/co/aosd/flash/exc/FlashSaleNotFoundException.java`
2. **Modify**: `src/main/java/uk/co/aosd/flash/services/FlashSalesService.java` - Add `cancelFlashSale()` method
3. **Modify**: `src/main/java/uk/co/aosd/flash/controllers/FlashSaleAdminRestApi.java` - Add cancel endpoint
4. **Modify**: `src/main/java/uk/co/aosd/flash/errorhandling/GlobalExceptionHandler.java` - Add exception handler

## Testing Considerations

After implementation,  add:

- Unit tests for `cancelFlashSale()` method
- Integration tests for the cancel endpoint
- Test cases for:
  - Cancelling DRAFT sale (releases all stock)
  - Cancelling ACTIVE sale with unsold stock (releases unsold stock)
  - Cancelling ACTIVE sale with all stock sold (no release needed)
  - Attempting to cancel COMPLETED sale (error)
  - Attempting to cancel already CANCELLED sale (error)
  - Cancelling non-existent sale (404 error)

## API Design Decision

Using `POST /api/v1/admin/flash_sale/{id}/cancel` instead of `PUT` because:

- Cancellation is an action/operation, not a state update
- POST is more RESTful for operations that have side effects
- Aligns with common REST practices for cancellation endpoints