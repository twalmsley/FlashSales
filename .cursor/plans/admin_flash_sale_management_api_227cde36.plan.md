---
name: Admin Flash Sale Management API
overview: "Implement admin flash sale management endpoints: list with filters, get by ID, update, and delete (with stock release for DRAFT sales)."
todos:
  - id: create-dtos
    content: "Create DTOs: FlashSaleItemDto, FlashSaleResponseDto, UpdateFlashSaleDto"
    status: completed
  - id: add-repository-methods
    content: Add findAllWithFilters and findByIdWithItems methods to FlashSaleRepository
    status: completed
  - id: add-service-methods
    content: Add getAllFlashSales, getFlashSaleById, updateFlashSale, deleteFlashSale methods to FlashSalesService
    status: completed
  - id: add-controller-endpoints
    content: Add GET, GET by ID, PUT, and DELETE endpoints to FlashSaleAdminRestApi
    status: in_progress
  - id: write-service-tests
    content: Write service tests for getAllFlashSales, getFlashSaleById, updateFlashSale, deleteFlashSale
    status: pending
  - id: write-controller-tests
    content: Write controller tests for all four endpoints with success and error cases
    status: pending
  - id: verify-existing-tests
    content: Run all existing tests to ensure no regressions
    status: pending
isProject: false
---

# Admin Flash Sale Management API

## Overview

Implement four admin endpoints for managing flash sales:

- `GET /api/v1/admin/flash_sale` - List all flash sales with optional filters (status, date range)
- `GET /api/v1/admin/flash_sale/{id}` - Get flash sale details
- `PUT /api/v1/admin/flash_sale/{id}` - Update flash sale (title, times, etc.)
- `DELETE /api/v1/admin/flash_sale/{id}` - Delete flash sale (only DRAFT status), releasing reserved stock

## Implementation Details

### 1. DTOs

Create response and update DTOs in `src/main/java/uk/co/aosd/flash/dto/`:

**FlashSaleItemDto.java** - For nested items in responses:

- `id` (UUID as String)
- `productId` (UUID as String)
- `productName` (String)
- `allocatedStock` (Integer)
- `soldCount` (Integer)
- `salePrice` (BigDecimal)

**FlashSaleResponseDto.java** - For GET responses:

- `id` (UUID as String)
- `title` (String)
- `startTime` (OffsetDateTime)
- `endTime` (OffsetDateTime)
- `status` (SaleStatus)
- `items` (List<FlashSaleItemDto>)

**UpdateFlashSaleDto.java** - For PUT requests:

- `title` (String, optional, with validation)
- `startTime` (OffsetDateTime, optional, with validation)
- `endTime` (OffsetDateTime, optional, with validation)
- Validation: endTime must be after startTime if both provided

### 2. Repository Methods

Add to `FlashSaleRepository.java`:

```java
@Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product " +
       "WHERE (:status IS NULL OR fs.status = :status) " +
       "AND (:startDate IS NULL OR fs.startTime >= :startDate) " +
       "AND (:endDate IS NULL OR fs.endTime <= :endDate) " +
       "ORDER BY fs.startTime ASC")
List<FlashSale> findAllWithFilters(
    @Param("status") SaleStatus status,
    @Param("startDate") OffsetDateTime startDate,
    @Param("endDate") OffsetDateTime endDate);

@Query("SELECT DISTINCT fs FROM FlashSale fs LEFT JOIN FETCH fs.items item LEFT JOIN FETCH item.product WHERE fs.id = :id")
Optional<FlashSale> findByIdWithItems(@Param("id") UUID id);
```

### 3. Service Methods

Add to `FlashSalesService.java`:

**getAllFlashSales(status, startDate, endDate)**:

- Call repository `findAllWithFilters` with nullable parameters
- Map entities to `FlashSaleResponseDto` list
- Return list

**getFlashSaleById(id)**:

- Call repository `findByIdWithItems` 
- Throw `FlashSaleNotFoundException` if not found
- Map entity to `FlashSaleResponseDto`
- Return DTO

**updateFlashSale(id, updateDto)**:

- Find flash sale by ID (throw `FlashSaleNotFoundException` if not found)
- Validate: if both startTime and endTime provided, endTime must be after startTime
- Validate: if updating times, check minimum duration (reuse existing validation logic)
- Update allowed fields: title, startTime, endTime
- Save and return updated entity mapped to DTO

**deleteFlashSale(id)**:

- Find flash sale by ID (throw `FlashSaleNotFoundException` if not found)
- Validate: only DRAFT status can be deleted (throw `IllegalArgumentException` otherwise)
- Release reserved stock: for each FlashSaleItem, subtract `allocatedStock - soldCount` from product's `reservedCount`
- Delete flash sale (cascade will handle items via `ON DELETE CASCADE` in DB, but we need to release stock first)
- Log the operation

### 4. Controller Endpoints

Add to `FlashSaleAdminRestApi.java`:

**GET /api/v1/admin/flash_sale**:

- Accept optional query params: `status` (String), `startDate` (OffsetDateTime), `endDate` (OffsetDateTime)
- Parse status enum (handle invalid values with 400 Bad Request)
- Call service `getAllFlashSales`
- Return 200 OK with list

**GET /api/v1/admin/flash_sale/{id}**:

- Parse UUID from path variable
- Call service `getFlashSaleById`
- Return 200 OK with DTO, or 404 if not found

**PUT /api/v1/admin/flash_sale/{id}**:

- Parse UUID from path variable
- Accept `UpdateFlashSaleDto` in request body
- Call service `updateFlashSale`
- Return 200 OK with updated DTO, or 404 if not found, or 400 for validation errors

**DELETE /api/v1/admin/flash_sale/{id}**:

- Parse UUID from path variable
- Call service `deleteFlashSale`
- Return 204 No Content on success, or 404 if not found, or 400 if not DRAFT status

### 5. Stock Release Logic

For DELETE operation (DRAFT sales only):

- Since DRAFT sales have `soldCount = 0`, release `allocatedStock` units
- For each FlashSaleItem:
  - Calculate: `difference = allocatedStock - soldCount` (will be `allocatedStock` for DRAFT)
  - Update product: `product.reservedCount = product.reservedCount - difference`
  - Save product
- Then delete the flash sale (database cascade will delete items)

### 6. Error Handling

- `FlashSaleNotFoundException` - return 404
- `IllegalArgumentException` (invalid status for delete, validation errors) - return 400
- Invalid UUID format - return 400
- Invalid enum values - return 400

## Files to Modify

1. `src/main/java/uk/co/aosd/flash/dto/FlashSaleItemDto.java` (new)
2. `src/main/java/uk/co/aosd/flash/dto/FlashSaleResponseDto.java` (new)
3. `src/main/java/uk/co/aosd/flash/dto/UpdateFlashSaleDto.java` (new)
4. `src/main/java/uk/co/aosd/flash/repository/FlashSaleRepository.java` (add query methods)
5. `src/main/java/uk/co/aosd/flash/services/FlashSalesService.java` (add service methods)
6. `src/main/java/uk/co/aosd/flash/controllers/FlashSaleAdminRestApi.java` (add endpoints)

## Test Files to Create

1. `src/test/java/uk/co/aosd/flash/services/FlashSalesServiceManagementTest.java` (new) - Service tests
2. `src/test/java/uk/co/aosd/flash/controllers/FlashSaleAdminRestApiManagementTest.java` (new) - Controller tests

## Notes

- Use `LEFT JOIN FETCH` in repository queries to eagerly load items and products, avoiding lazy loading issues
- Follow existing patterns: use `@Transactional` for service methods that modify data
- Reuse existing validation logic for sale duration from `createFlashSale`
- For date filtering, use `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)` on controller parameters
- DELETE only works for DRAFT status to prevent deleting active/completed sales
- Stock release must happen before deletion to maintain data integrity

## Testing Strategy

### Service Tests (`FlashSalesServiceManagementTest.java`)

Test all service methods with mocked repositories:

**getAllFlashSales tests:**

- Should return all sales when no filters provided
- Should filter by status only
- Should filter by date range only
- Should filter by status and date range
- Should return empty list when no matches

**getFlashSaleById tests:**

- Should return flash sale when found
- Should throw FlashSaleNotFoundException when not found
- Should eagerly load items and products

**updateFlashSale tests:**

- Should update title only
- Should update startTime only
- Should update endTime only
- Should update all fields
- Should validate endTime after startTime
- Should validate minimum duration
- Should throw FlashSaleNotFoundException when not found

**deleteFlashSale tests:**

- Should delete DRAFT sale and release all allocated stock
- Should throw FlashSaleNotFoundException when not found
- Should throw IllegalArgumentException for ACTIVE status
- Should throw IllegalArgumentException for COMPLETED status
- Should throw IllegalArgumentException for CANCELLED status
- Should verify stock release: product.reservedCount decreases by allocatedStock

### Controller Tests (`FlashSaleAdminRestApiManagementTest.java`)

Use `@WebMvcTest` with `@MockitoBean` for FlashSalesService:

**GET /api/v1/admin/flash_sale tests:**

- Should return 200 with list of sales (no filters)
- Should return 200 with filtered results (status only)
- Should return 200 with filtered results (date range only)
- Should return 200 with filtered results (status and date range)
- Should return 400 for invalid status enum value
- Should return 400 for invalid date format

**GET /api/v1/admin/flash_sale/{id} tests:**

- Should return 200 with flash sale DTO when found
- Should return 404 when not found
- Should return 400 for invalid UUID format

**PUT /api/v1/admin/flash_sale/{id} tests:**

- Should return 200 with updated DTO (title only)
- Should return 200 with updated DTO (times only)
- Should return 200 with updated DTO (all fields)
- Should return 404 when not found
- Should return 400 for invalid UUID format
- Should return 400 for validation errors (endTime before startTime)
- Should return 400 for validation errors (duration too short)

**DELETE /api/v1/admin/flash_sale/{id} tests:**

- Should return 204 No Content for DRAFT sale
- Should return 404 when not found
- Should return 400 for invalid UUID format
- Should return 400 for ACTIVE status
- Should return 400 for COMPLETED status
- Should return 400 for CANCELLED status

### Test Patterns

- Use JUnit 5 (`@Test`, `@BeforeEach`, `@BeforeAll`)
- Use Mockito for mocking (`@MockitoBean`, `Mockito.when()`, `verify()`)
- Use `@WebMvcTest` for controller tests with `@Import({ ErrorMapper.class, GlobalExceptionHandler.class })`
- Use `ObjectMapper` with `JavaTimeModule` for JSON serialization in controller tests
- Follow existing test naming: `should[Action][Condition]`
- Verify service method calls with `verify(service, times(n)).method()`
- Test both success and error paths

### Running Tests

After implementation:

1. Run all tests: `mvn test`
2. Verify existing tests still pass (especially `FlashSalesServiceCancelFlashSaleTest`, `FlashSaleRestApiCancelSaleTest`, etc.)
3. Run new tests individually to verify they pass
4. Check for any test failures and fix issues