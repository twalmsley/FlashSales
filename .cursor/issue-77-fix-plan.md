## Fix Plan for Issue #77

### Problem Summary
Users and admins can only browse products and flash sales by listing all items. There is no search functionality to find specific products (by name/description) or flash sales (by title), which hurts discoverability and user experience.

### Root Cause Analysis
- **Products**: `GET /api/v1/products` in `ProductRestApi` delegates to `ProductsService.getAllProducts()`, which calls `repository.findAll()` with no query parameters. The `ProductRepository` has no search methods.
- **Flash sales**: `GET /api/v1/admin/flash_sale` already supports optional `status`, `startDate`, and `endDate` filters via `FlashSalesService.getAllFlashSales()` and `FlashSaleRepository.findAllWithFilters()`, but there is no text search on `title`.
- The database schema supports searchable text: `products` has `name` (VARCHAR 255) and `description` (TEXT); `flash_sales` has `title` (VARCHAR 255). No full-text search indexes or search APIs exist today.

### Proposed Solution
1. **API layer**: Add optional `search` (and product filters: price range; flash sales already have status/date) to the existing list endpoints so that existing clients continue to work and new clients can pass search/filters.
2. **Backend**: Use PostgreSQL full-text search (tsvector/tsquery) for products (name + description) and flash sales (title), with a new Flyway migration for FTS columns/indexes and repository methods that accept search + filters.
3. **Service layer**: Extend `ProductsService` and `FlashSalesService` to accept search and filter params and call the new repository methods; keep caching semantics consistent (e.g. cache key includes search/filters or skip cache for search).
4. **UI**: Add search inputs and optional filters to the admin product list and flash sale list pages; optionally add result count and “no results” messaging.
5. **Later/optional**: Search result highlighting and search analytics (popular searches, no-results queries) can be follow-up work.

### Affected Components
- **API**
  - `ProductRestApi.java` – add optional `@RequestParam(required = false) String search` and optional `minPrice`/`maxPrice` to `GET /api/v1/products`; delegate to service with these params.
  - `FlashSaleAdminRestApi.java` – add optional `@RequestParam(required = false) String search` to `GET /api/v1/admin/flash_sale` (status/startDate/endDate already exist).
- **Service**
  - `ProductsService.java` – add method (e.g. `getAllProducts(String search, BigDecimal minPrice, BigDecimal maxPrice)`) or overload; implement with repository; adjust `@Cacheable` key to include params or use a separate cache key / no cache when search/filters present.
  - `FlashSalesService.java` – extend `getAllFlashSales(SaleStatus, OffsetDateTime, OffsetDateTime)` to accept `String search` and pass through to repository.
- **Repository**
  - `ProductRepository.java` – add method(s) for search + optional price range (JPQL or native query using FTS).
  - `FlashSaleRepository.java` – extend `findAllWithFilters` (or add overload) to accept `String search` and apply FTS on `title`.
- **Database**
  - New Flyway migration (e.g. `V11__ProductAndFlashSaleFullTextSearch.sql`): add generated/updated `tsvector` column(s) and GIN index for `products` (name + description) and `flash_sales` (title); optional trigger or application-maintained tsvector.
- **Web**
  - `AdminWebController.java` (or equivalent) – product list and flash sale list handlers: accept request params for search and product price range; pass to service.
  - `admin/products/list.html` – add search input and optional price range inputs; form GET to same list URL with params.
  - `admin/sales/list.html` – add search input for title; form GET to same list URL with params.
- **Tests**
  - `ProductRestApiGetAllProductsTest.java` – extend with tests for `?search=`, `?minPrice=`, `?maxPrice=`, and combined; empty/blank search behavior.
  - `FlashSaleAdminRestApiManagementTest.java` – add tests for `?search=` on list endpoint; empty/blank search.
  - `ProductsServiceTest.java` – tests for search and price filters.
  - `FlashSalesServiceManagementTest.java` (or equivalent) – tests for search in list.
  - Repository tests or integration tests for FTS (search returns expected rows; no FTS match returns empty).
  - Optional: integration test with real DB (Testcontainers) to validate FTS behavior.

### Testing Strategy
- [ ] Unit tests: `ProductsService` – search only, price range only, search + price, null/blank search (behaves like no filter), no results.
- [ ] Unit tests: `FlashSalesService` – list with search term, null/blank search (unchanged behavior).
- [ ] Controller tests: `GET /api/v1/products?search=query`, `?minPrice=1&maxPrice=10`, `?search=xy&minPrice=0`; verify 200 and response body.
- [ ] Controller tests: `GET /api/v1/admin/flash_sale?search=title`; verify 200 and list filtered by title.
- [ ] Edge cases: empty string search, very long search string, special characters (escape in tsquery); price range with min &gt; max (validate and return 400 or empty).
- [ ] Repository/DB: FTS returns correct products/sales; products with description match, flash sales with title match; no regression when search is null (all returned when no other filters).

### Implementation Steps
1. **Database**: Add migration `V11__ProductAndFlashSaleFullTextSearch.sql`: add `search_vector` (or similar) tsvector to `products` (e.g. `setweight(to_tsvector('english', coalesce(name,'')), 'A') || setweight(to_tsvector('english', coalesce(description,'')), 'B')`) and to `flash_sales` on `title`; create GIN index on each; use trigger or generated column to keep tsvector updated on insert/update.
2. **ProductRepository**: Add `List<Product> findAllWithSearchAndPrice(@Param("search") String search, @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice)` with native or JPQL that uses the FTS column when search present and applies price filters when provided.
3. **FlashSaleRepository**: Extend `findAllWithFilters` (or add `findAllWithFiltersAndSearch`) to accept `String search` and add FTS condition on title when search is non-blank; keep existing status/date logic.
4. **ProductsService**: Add overload or new method `getAllProducts(String search, BigDecimal minPrice, BigDecimal maxPrice)`; when all null/empty, call existing `findAll()` for backward compatibility and cache key `'all'`; otherwise call new repository method; decide cache policy (e.g. no cache when any filter present, or key = search+min+max).
5. **FlashSalesService**: Add `String search` parameter to `getAllFlashSales`; pass to repository; keep existing status/date behavior.
6. **ProductRestApi**: Add optional `search`, `minPrice`, `maxPrice` to `getAllProducts()`; validate min &lt;= max if both present; call service with params.
7. **FlashSaleAdminRestApi**: Add optional `search` to `getAllFlashSales()`; call service with search.
8. **Admin web**: In product list and flash sale list, add search form (and product price inputs if desired); submit GET with query params; show “No results” when list is empty and search/filters were applied.
9. **Docs**: Update README/OpenAPI for new query params on `GET /api/v1/products` and `GET /api/v1/admin/flash_sale`.
10. **Tests**: Add and run all tests above; ensure full suite passes (`mvn test`).

### Risk Assessment
- **Breaking change**: None if search and filters are optional and default behavior (no params) unchanged.
- **Performance**: FTS with GIN index is typically fast; monitor query time if catalogs grow large.
- **Caching**: Listing with search/filters should not use the same cache as “all products” to avoid wrong results; either exclude from cache or key by params.
- **Security**: Sanitize/escape search input for tsquery to avoid injection (e.g. use parameterised queries and restrict to safe characters or use `plainto_tsquery`).
- **UI**: Ensure empty/blank search is treated as “no search” so the list shows all (or current filters only).
