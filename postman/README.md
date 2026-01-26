# Postman Collection for Flash Sales API

This directory contains Postman collections and environments for testing the Flash Sales Demo App API.

## Files

- `Flash_Sales_API.postman_collection.json` - Complete API collection with all endpoints
- `Flash_Sales_Local.postman_environment.json` - Local development environment variables
- `README.md` - This file

## Quick Start

### 1. Import into Postman

1. Open Postman
2. Click **Import** button (top left)
3. Import both files:
   - `Flash_Sales_API.postman_collection.json`
   - `Flash_Sales_Local.postman_environment.json`
4. Select the **Flash Sales - Local** environment from the environment dropdown (top right)

### 2. Start the Application

Make sure your application is running:
```bash
./mvnw spring-boot:run
```

The default port is `8080` (configured in the environment).

### 3. Authentication

**IMPORTANT**: Most endpoints require authentication. You need to authenticate before using the API.

#### Step 1: Register or Login

The collection includes authentication endpoints in the **Authentication** folder:

1. **Register** - Create a new user account
   - Uses `username`, `email`, and `password` from environment variables
   - Automatically saves the JWT token to `jwtToken` environment variable
   - Default credentials are pre-configured in the environment

2. **Login** - Authenticate with existing credentials
   - Uses `username` (or email) and `password` from environment variables
   - Automatically saves the JWT token to `jwtToken` environment variable

#### Step 2: Token is Automatically Used

The collection is configured to automatically add the JWT token to all protected endpoints:
- The token is sent in the `Authorization` header as `Bearer <token>`
- Public endpoints (like `/api/v1/auth/**`) don't require the token
- The token is automatically retrieved from the `jwtToken` environment variable

#### Step 3: Admin Endpoints

For **Admin APIs** (product management, flash sale creation), you need a user with `ADMIN_USER` role:
- Regular registration creates users with `USER` role
- Admin users must be created directly in the database or through a database migration
- Once you have an admin user, login with those credentials to get an admin token

#### Authentication Flow Example

1. **Register** (or **Login** if you already have an account)
   ```json
   POST /api/v1/auth/register
   {
       "username": "testuser",
       "email": "test@example.com",
       "password": "TestPassword123!"
   }
   ```
   Response includes a `token` field - this is automatically saved to the environment.

2. **Use Protected Endpoints**
   - The JWT token is automatically added to all requests
   - No manual configuration needed!

3. **Check Current User** (optional)
   - Use **Get Current User** to verify your authentication
   - Returns your user information including role

#### Environment Variables for Authentication

- `jwtToken`: Automatically set after login/register (don't set manually)
- `username`: Your username (default: "testuser")
- `email`: Your email (default: "test@example.com")
- `password`: Your password (default: "TestPassword123!")
- `userId`: Automatically set after login/register
- `userRole`: Automatically set after login/register (USER or ADMIN_USER)

### 4. Recommended Testing Workflow

**Before starting**: Make sure you've authenticated (see Step 3 above). For admin operations, you need an admin user.

#### Step 1: Authenticate (if not already done)
1. Use **Authentication > Login** or **Authentication > Register**
2. The JWT token will be automatically saved

#### Step 2: Create a Product (Admin API)
1. Use **Admin APIs > Create Product**
2. The request body is pre-filled with example data
3. Send the request
4. The product ID will be automatically saved to the `productId` environment variable (from the `Location` header)

#### Step 3: Create a Flash Sale (Admin API)
1. Use **Admin APIs > Create Flash Sale**
2. **IMPORTANT**: Update the `startTime` and `endTime` in the request body:
   - `startTime`: Set to a future time (e.g., 1 hour from now)
   - `endTime`: Set to a later time (e.g., 8 hours from now)
   - Format: `2026-01-24T10:00:00Z` (ISO 8601)
3. The `productId` will be automatically used from the environment
4. Send the request
5. The sale will be created with status `DRAFT`
6. The sale ID will be automatically saved to the `saleId` environment variable

#### Step 4: Wait for Sale to Become Active
- The sale will automatically become `ACTIVE` when `startTime` is reached
- The scheduled job runs every 30 seconds to activate sales
- You can check the application logs to see when it activates

#### Step 5: Get Active Sales (Client API)
1. Use **Client APIs > Get Active Sales**
2. This will show all active sales with remaining stock
3. **The `flashSaleItemId` is now included in the response!**
4. The test script will automatically extract and save the first `flashSaleItemId` to the environment variable
5. If you need a different item, manually copy the `flashSaleItemId` from the response and update the environment variable

#### Step 6: Create an Order (Client API)
1. Use **Client APIs > Create Order**
2. The request body should already have:
   - `userId`: A UUID (default is set in environment)
   - `flashSaleItemId`: Automatically set from Step 4
   - `quantity`: Number of items to order (default is 2)
3. Send the request
4. The order will be created with status `PENDING`
5. The order ID will be automatically saved to `orderId` environment variable

#### Step 7: Monitor Order Processing
- Check application logs to see:
  - Payment processing
  - Status changes (PENDING → PAID → DISPATCHED)
  - Or (PENDING → FAILED) if payment fails
- The order is processed asynchronously via RabbitMQ
- You can check RabbitMQ management UI at `http://localhost:15672` (username: `rabbit`, password: `rabbit`)

#### Step 8: Refund Order (Client API) - Optional
1. Use **Client APIs > Refund Order**
2. This requires the order to be in `PAID` status
3. The `orderId` should already be set from Step 5
4. Wait for payment processing to complete before attempting refund

## Environment Variables

### API Configuration
- `baseUrl`: API base URL (default: http://localhost:8080)

### Authentication (auto-set after login/register)
- `jwtToken`: JWT authentication token (automatically set, don't edit manually)
- `userId`: Current user ID (automatically set)
- `username`: Username for login/register (default: "testuser")
- `email`: Email for registration (default: "test@example.com")
- `password`: Password for login/register (default: "TestPassword123!")
- `userRole`: User role - USER or ADMIN_USER (automatically set)

### API Workflow Variables (auto-set during testing)
- `productId`: Product ID (auto-set after creating a product)
- `flashSaleItemId`: Flash sale item ID (auto-set after getting active sales)
- `orderId`: Order ID (auto-set after creating an order)
- `saleId`: Flash sale ID (auto-set after creating a flash sale)
- `hasActiveSales`: Whether active sales exist (auto-set)

## Example Request Bodies

### Create Product
```json
{
    "name": "Test Product",
    "description": "A test product description",
    "totalPhysicalStock": 100,
    "basePrice": 99.99,
    "reservedCount": 0
}
```

### Create Flash Sale
```json
{
    "title": "Summer Flash Sale",
    "startTime": "2026-01-24T10:00:00Z",
    "endTime": "2026-01-24T18:00:00Z",
    "status": "DRAFT",
    "products": [
        {
            "id": "your-product-id-here",
            "reservedCount": 50
        }
    ]
}
```

**Note**: Update the times to be in the future! The sale must have:
- `startTime` in the future
- `endTime` at least 5 minutes after `startTime`

### Create Order
```json
{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "flashSaleItemId": "your-flash-sale-item-id-here",
    "quantity": 2
}
```

### Active Sales Response Example
```json
[
    {
        "saleId": "547cf74d-7b64-44ea-b70f-cbcde09cadc9",
        "flashSaleItemId": "ab3b715e-e2c2-4c28-925d-83ac93c32d02",
        "title": "Summer Flash Sale",
        "startTime": "2026-01-24T10:00:00Z",
        "endTime": "2026-01-24T18:00:00Z",
        "productId": "11111111-1111-1111-1111-111111111111",
        "allocatedStock": 50,
        "soldCount": 5,
        "salePrice": 79.99
    }
]
```

**Note**: The `flashSaleItemId` is now included in the response, making it easy to create orders!

## Testing Tips

### 1. Time-sensitive Operations
Flash sales have start and end times. Make sure:
- `startTime` is in the future when creating a sale
- `endTime` is after `startTime` and at least 5 minutes later
- The sale must be ACTIVE (not DRAFT) to create orders
- Check the sale's `endTime` - orders cannot be created after the sale ends

### 2. Stock Validation
- Ensure product has enough `totalPhysicalStock`
- When creating a flash sale, `reservedCount` must be <= `totalPhysicalStock`
- When creating an order, quantity must be <= available stock (allocatedStock - soldCount)

### 3. Order Status Flow
- **PENDING** → (payment processing) → **PAID** → **DISPATCHED**
- **PENDING** → (payment failed) → **FAILED**
- **PAID** → (refund) → **REFUNDED**

### 4. Payment Service Configuration
The payment service is mocked. Configure it in `application.yaml`:
```yaml
app:
  payment:
    always-succeed: true   # Always succeed (for testing)
    always-fail: false     # Always fail (for testing failures)
    success-rate: 0.9      # 90% success rate (default)
```

### 5. Check Application Logs
The application logs all operations at INFO level, including:
- Order creation
- Payment processing
- Status changes
- Email notifications (logged as INFO messages)

### 6. RabbitMQ Monitoring
- Access RabbitMQ Management UI: `http://localhost:15672`
- Username: `rabbit`
- Password: `rabbit`
- Check queue depths and message rates
- Monitor consumer activity

## Troubleshooting

### 401 Unauthorized
- **Problem**: You're not authenticated or your token has expired
- **Solution**: 
  1. Use **Authentication > Login** to get a new token
  2. Make sure the `jwtToken` environment variable is set
  3. Check that the token is being sent (look at request headers in Postman)
  4. Verify your credentials are correct

### 403 Forbidden
- **Problem**: Your user doesn't have the required role (e.g., trying to access admin endpoints with a regular user)
- **Solution**: 
  1. Admin endpoints require `ADMIN_USER` role
  2. Regular registration creates users with `USER` role
  3. You need to create an admin user in the database or use existing admin credentials
  4. Check your `userRole` environment variable after login

### 404 Not Found
- Check that the ID exists and is correct
- Verify the endpoint URL is correct

### 400 Bad Request
- Check request body format (must be valid JSON)
- Verify all required fields are present
- Check validation rules (e.g., quantity must be positive)

### Sale Not Active
- Ensure the sale's `endTime` hasn't passed
- Check that the sale status is `ACTIVE` (not `DRAFT`)
- Wait for the scheduled job to activate the sale (runs every 30 seconds)

### Insufficient Stock
- Check available stock: `allocatedStock - soldCount >= requestedQuantity`
- Verify the product has enough `totalPhysicalStock`

### Invalid Order Status
- Ensure order is in the correct status for the operation
- Refund requires order to be in `PAID` status
- Dispatch requires order to be in `PAID` status

### Order Not Processing
- Check that RabbitMQ is running: `docker compose ps`
- Verify RabbitMQ connection in application logs
- Check that consumers are running (check application logs)
- Verify queues exist in RabbitMQ Management UI

### flashSaleItemId Not Set
- The test script in "Get Active Sales" should auto-set it
- If not set, manually copy the `flashSaleItemId` from the response
- Ensure the active sales response contains at least one sale

## Quick Test Sequence

1. **Authenticate** → **Login** or **Register** → `jwtToken` auto-saved
2. **Create Product** (Admin API) → `productId` auto-saved
3. **Create Flash Sale** (Admin API, with future times) → `saleId` auto-saved → wait for activation
4. **Get Active Sales** (Client API) → `flashSaleItemId` auto-saved (from response)
5. **Create Order** (Client API) → `orderId` auto-saved
6. **Check logs** for order processing (payment, dispatch, etc.)
7. **Refund Order** (optional, requires PAID status)

## Running Postman Tests

### Manual Testing
1. Select the **Flash Sales - Local** environment
2. Run requests in sequence following the workflow above
3. Check responses and environment variables after each request

### Using Postman Collection Runner
1. Click on the **Flash Sales API** collection
2. Click **Run** button
3. Select which requests to run
4. Configure iterations and delays if needed
5. Click **Run Flash Sales API**
6. Review the test results

### Using Newman (Command Line)
If you have Newman installed:
```bash
newman run postman/Flash_Sales_API.postman_collection.json \
  -e postman/Flash_Sales_Local.postman_environment.json \
  --delay-request 1000
```

## Notes

- All timestamps should be in ISO 8601 format: `2026-01-24T10:00:00Z`
- UUIDs must be in valid UUID format
- The `flashSaleItemId` is now automatically extracted from the active sales response
- Environment variables are automatically updated by test scripts in the collection
- Make sure Docker services (PostgreSQL, Redis, RabbitMQ) are running before testing
