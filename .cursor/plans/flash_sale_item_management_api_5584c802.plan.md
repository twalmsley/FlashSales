---
name: Flash Sale Item Management API
overview: Implement three admin endpoints for managing flash sale items (add, update, delete) with proper reservedCount maintenance and comprehensive test coverage.
todos:
  - id: create-dtos
    content: Create UpdateFlashSaleItemDto and AddFlashSaleItemDto with validation annotations
    status: completed
  - id: create-exception
    content: Create FlashSaleItemNotFoundException exception class
    status: completed
  - id: add-repository-methods
    content: Add findByIdAndFlashSaleId and existsByFlashSaleIdAndProductId methods to FlashSaleItemRepository
    status: completed
  - id: implement-add-items
    content: Implement addItemsToFlashSale service method with reservedCount management
    status: completed
  - id: implement-update-item
    content: Implement updateFlashSaleItem service method with reservedCount management
    status: completed
  - id: implement-remove-item
    content: Implement removeFlashSaleItem service method with reservedCount management
    status: completed
  - id: add-controller-endpoints
    content: Add POST, PUT, DELETE endpoints to FlashSaleAdminRestApi controller
    status: completed
  - id: add-exception-handler
    content: Add FlashSaleItemNotFoundException handler to GlobalExceptionHandler
    status: completed
  - id: write-service-tests
    content: Write comprehensive service layer tests for all three methods
    status: completed
  - id: write-controller-tests
    content: Write controller layer tests for all three endpoints
    status: completed
  - id: run-all-tests
    content: Run all tests and fix any failures
    status: completed
isProject: false
---

# Flash Sale Item Management API Implementation Plan

## Overview

Implement three new admin endpoints for managing flash sale items:

- `POST /api/v1/admin/flash_sale/{id}/items` - Add items to existing sale
- `PUT /api/v1/admin/flash_sale/{id}/items/{itemId}` - Update allocated stock or sale price
- `DELETE /api/v1/admin/flash_sale/{id}/items/{itemId}` - Remove item from sale

All operations must maintain correct `reservedCount` values for products.

## Key Requirements

### Business Rules

1. **Status Restrictions**: Only DRAFT sales can have items added/updated/deleted (similar to `deleteFlashSale`)
2. **Stock Validation**: 

- Cannot allocate more stock than available (totalPhysicalStock - reservedCount)
- Cannot reduce allocatedStock below soldCount

3. **ReservedCount Management**:

- Adding items: Increase product.reservedCount by allocatedStock
- Updating allocatedStock: Adjust product.reservedCount by the difference
- Deleting items: Decrease product.reservedCount by (allocatedStock - soldCount)

4. **Validation**:

- Sale must exist
- Item must exist and belong to the sale
- Product must exist
- No duplicate products in a sale (unique constraint: flash_sale_id + product_id)

## Implementation Details

### 1. Create DTOs

**File**: `src/main/java/uk/co/aosd/flash/dto/UpdateFlashSaleItemDto.java`

- Fields: `Integer allocatedStock` (optional), `BigDecimal salePrice` (optional)
- Both fields optional, at least one required
- Validation: `@Min(0)` for allocatedStock, `@Positive` for salePrice

**File**: `src/main/java/uk/co/aosd/flash/dto/AddFlashSaleItemDto.java`

- Fields: `@NotEmpty String productId`, `@Min(0) Integer allocatedStock`, `BigDecimal salePrice` (optional, defaults to product basePrice)
- Reuse `SaleProductDto` pattern but add salePrice

### 2. Create Exception

**File**: `src/main/java/uk/co/aosd/flash/exc/FlashSaleItemNotFoundException.java`

- Similar to `FlashSaleNotFoundException`
- Fields: `UUID itemId`
- Add handler in `GlobalExceptionHandler` (404 response)

### 3. Add Repository Methods

**File**: `src/main/java/uk/co/aosd/flash/repository/FlashSaleItemRepository.java`

- Add: `Optional<FlashSaleItem> findByIdAndFlashSaleId(UUID itemId, UUID flashSaleId)`
- Add: `boolean existsByFlashSaleIdAndProductId(UUID flashSaleId, UUID productId)`

### 4. Implement Service Methods

**File**: `src/main/java/uk/co/aosd/flash/services/FlashSalesService.java`

#### `addItemsToFlashSale(UUID saleId, List<AddFlashSaleItemDto> items)`

- Load sale with items (use `findByIdWithItems`)
- Validate sale exists and is DRAFT
- For each item:
- Validate product exists
- Check product not already in sale (unique constraint)
- Validate stock availability: `currentReserved + newAllocated <= totalPhysicalStock`
- Create FlashSaleItem with allocatedStock, soldCount=0, salePrice (or basePrice)
- Update product.reservedCount: `reservedCount += allocatedStock`
- Save all items and products
- Return updated FlashSaleResponseDto

#### `updateFlashSaleItem(UUID saleId, UUID itemId, UpdateFlashSaleItemDto updateDto)`

- Load sale with items
- Validate sale exists and is DRAFT
- Find item by itemId and saleId
- Validate item exists
- If updating allocatedStock:
- Validate: `newAllocatedStock >= item.soldCount`
- Calculate difference: `diff = newAllocatedStock - oldAllocatedStock`
- Validate product has enough stock: `product.reservedCount + diff <= product.totalPhysicalStock`
- Update item.allocatedStock
- Update product.reservedCount: `reservedCount += diff`
- If updating salePrice:
- Update item.salePrice
- Save item and product
- Return updated FlashSaleResponseDto

#### `removeFlashSaleItem(UUID saleId, UUID itemId)`

- Load sale with items
- Validate sale exists and is DRAFT
- Find item by itemId and saleId
- Validate item exists
- Calculate stock to release: `allocatedStock - soldCount`
- Update product.reservedCount: `reservedCount -= (allocatedStock - soldCount)`
- Delete item
- Save product
- Return updated FlashSaleResponseDto

### 5. Add Controller Endpoints

**File**: `src/main/java/uk/co/aosd/flash/controllers/FlashSaleAdminRestApi.java`

#### `POST /api/v1/admin/flash_sale/{id}/items`

- Accept `List<AddFlashSaleItemDto>`
- Call `service.addItemsToFlashSale()`
- Return 201 Created with location header
- Handle: 404 (sale not found), 400 (validation/stock errors), 409 (duplicate product)

#### `PUT /api/v1/admin/flash_sale/{id}/items/{itemId}`

- Accept `UpdateFlashSaleItemDto`
- Call `service.updateFlashSaleItem()`
- Return 200 OK with updated sale
- Handle: 404 (sale/item not found), 400 (validation errors)

#### `DELETE /api/v1/admin/flash_sale/{id}/items/{itemId}`

- Call `service.removeFlashSaleItem()`
- Return 204 No Content
- Handle: 404 (sale/item not found), 400 (status error)

### 6. Add Exception Handler

**File**: `src/main/java/uk/co/aosd/flash/errorhandling/GlobalExceptionHandler.java`

- Add handler for `FlashSaleItemNotFoundException` → 404

### 7. Write Tests

#### Service Layer Tests

**File**: `src/test/java/uk/co/aosd/flash/services/FlashSalesServiceItemManagementTest.java`

- Test `addItemsToFlashSale`:
- Success: add single item, multiple items
- Failures: sale not found, sale not DRAFT, product not found, duplicate product, insufficient stock
- Verify reservedCount updates correctly
- Test `updateFlashSaleItem`:
- Success: update allocatedStock only, update salePrice only, update both
- Failures: sale/item not found, sale not DRAFT, allocatedStock < soldCount, insufficient stock
- Verify reservedCount updates correctly
- Test `removeFlashSaleItem`:
- Success: remove item with no sales, remove item with some sales
- Failures: sale/item not found, sale not DRAFT
- Verify reservedCount updates correctly

#### Controller Layer Tests

**File**: `src/test/java/uk/co/aosd/flash/controllers/FlashSaleAdminRestApiItemManagementTest.java`

- Test POST endpoint: success cases, validation errors, 404, 409
- Test PUT endpoint: success cases, validation errors, 404
- Test DELETE endpoint: success cases, 404, 400 (status errors)
- Use `@WebMvcTest` with mocked service (similar to `FlashSaleAdminRestApiManagementTest`)

#### Integration Tests (Optional)

- Consider adding integration tests that verify database state after operations

## Files to Create/Modify

### New Files

1. `src/main/java/uk/co/aosd/flash/dto/UpdateFlashSaleItemDto.java`
2. `src/main/java/uk/co/aosd/flash/dto/AddFlashSaleItemDto.java`
3. `src/main/java/uk/co/aosd/flash/exc/FlashSaleItemNotFoundException.java`
4. `src/test/java/uk/co/aosd/flash/services/FlashSalesServiceItemManagementTest.java`
5. `src/test/java/uk/co/aosd/flash/controllers/FlashSaleAdminRestApiItemManagementTest.java`

### Modified Files

1. `src/main/java/uk/co/aosd/flash/repository/FlashSaleItemRepository.java` - Add query methods
2. `src/main/java/uk/co/aosd/flash/services/FlashSalesService.java` - Add three service methods
3. `src/main/java/uk/co/aosd/flash/controllers/FlashSaleAdminRestApi.java` - Add three endpoints
4. `src/main/java/uk/co/aosd/flash/errorhandling/GlobalExceptionHandler.java` - Add exception handler

## Testing Strategy

1. **Unit Tests**: Service layer with mocked repositories
2. **Controller Tests**: MockMvc with mocked service
3. **Edge Cases**: 

- Update allocatedStock to exactly soldCount
- Add item when product already reserved by another sale
- Remove item when soldCount > 0

4. **ReservedCount Verification**: All tests verify product.reservedCount is updated correctly

## Notes

- Follow existing patterns from `createFlashSale`, `deleteFlashSale`, `updateFlashSale`
- Use `@Transactional` on all service methods
- Use `findByIdWithItems` to avoid lazy loading issues
- Log all operations for debugging
- Maintain consistency with existing error handling patterns