# Flash Sales Demo Application

A Spring Boot application demonstrating a high-traffic e-commerce flash sale system. This application manages products, flash sales, inventory allocation, and order processing with support for concurrent access patterns.

## Overview

This application provides a complete flash sale management system with separate admin and client-facing APIs. It handles product catalog management, flash sale creation and scheduling, inventory allocation, and real-time stock tracking.

## Core Functionality

### Product Management
- **Products** represent items in the catalog with:
  - Name, description, and base price
  - Total physical stock count
  - Reserved count (stock allocated to active flash sales)
  - Automatic timestamp tracking (created_at, updated_at)

### Flash Sales
- **Flash Sales** are time-limited promotional events with:
  - Title and scheduled start/end times
  - Status lifecycle: `DRAFT` → `ACTIVE` → `COMPLETED` or `CANCELLED`
  - Validation: end time must be after start time
  
- **Flash Sale Items** link products to flash sales with:
  - Allocated stock (inventory reserved for the sale)
  - Sold count (tracking purchases)
  - Sale price (may differ from base price)
  - Constraint: sold_count cannot exceed allocated_stock
  - Unique constraint: a product can only appear once per flash sale

### Orders
- **Orders** track customer purchases with:
  - User ID and flash sale item reference
  - Product ID, sold price, and sold quantity
  - Order status: `PENDING`, `PAID`, `FAILED`, `REFUNDED`, or `DISPATCHED`
  - Unique constraint: one purchase per user per flash sale item
  - Automatic timestamp tracking
  - Status transitions: PENDING → PAID → DISPATCHED (or PENDING → FAILED, PAID → REFUNDED)

### Stock Management
- Products maintain `total_physical_stock` and `reserved_count`
- Flash sale items track `allocated_stock` and `sold_count`
- A database view `remaining_active_stock` provides real-time visibility into:
  - Active flash sales with remaining inventory
  - Products available for purchase
  - Current stock levels and pricing

## User Interface (UI)

The application includes a web-based user interface built with Thymeleaf templates and Bootstrap 5.3.0 for styling. The UI provides both public and authenticated user experiences, with role-based access control for administrative features.

### Available Pages

#### Public Pages
- **Home** (`/`) - Landing page with quick navigation links
- **Login** (`/login`) - User authentication page
- **Register** (`/register`) - New user registration page
- **Active Sales** (`/sales`) - Browse active flash sales (public access)

#### Authenticated User Pages
- **My Orders** (`/orders`) - View order history and order details (requires authentication)

#### Admin Pages (Requires ADMIN_USER role)
All admin pages are accessible at `/admin/**` and require the `ADMIN_USER` role:

- **Admin Dashboard** (`/admin`) - Overview and navigation to admin features
- **Products Management** (`/admin/products`) - View, create, edit, and manage products
- **Flash Sales Management** (`/admin/sales`) - Create and manage flash sales, add items, set pricing
- **Orders Management** (`/admin/orders`) - View all orders, filter by status/date, update order status
- **Analytics Dashboard** (`/admin/analytics`) - View sales performance, revenue metrics, product performance, and order statistics

### Navigation Structure

The UI includes a responsive navigation bar that adapts based on authentication status:
- **Public users**: See Home, Active Sales, Login, and Register links
- **Authenticated users**: See Home, Active Sales, My Orders, and Logout links
- **Admin users**: See all user links plus Admin link with access to admin dashboard

## User Registration & Login

**Quick Start**: If you've loaded the seed data (see [Loading Seed Data](#loading-seed-data)), you can immediately log in using the test users:
- Username: `user` or `admin` (password: `password`)
- These users are pre-configured with USER and ADMIN_USER roles respectively

### UI Registration & Login

#### Registration via Web UI
1. Navigate to `/register` in your browser
2. Fill in the registration form with:
   - Username (must be unique)
   - Email (must be unique)
   - Password
3. Submit the form
4. Upon successful registration, you'll be redirected to the login page
5. Log in with your username/email and password

#### Login via Web UI
1. Navigate to `/login` in your browser
2. Enter your username (or email) and password
3. Submit the form
4. Upon successful authentication, you'll be redirected to the home page
5. Session attributes (`isAuthenticated`, `isAdmin`, `userId`) are set for UI access control

### API Registration & Login

The application also provides REST API endpoints for programmatic access:

#### Registration via API
```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "your_username",
  "email": "your_email@example.com",
  "password": "your_password"
}
```

**Response**: Returns a JWT token along with user information.

#### Login via API
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "your_username",
  "password": "your_password"
}
```

**Response**: Returns a JWT token along with user information.

**Note**: The API uses JWT-based authentication (stateless), while the UI uses form-based session authentication (stateful). See the [Authentication & Security](#authentication--security) section for details.

## Admin Role Management

To grant a user administrative privileges, you need to manually update the user's role in the database. There is currently no UI for role management.

### Promoting a User to ADMIN_USER

1. Connect to your PostgreSQL database:
   ```bash
   psql -h localhost -U postgres -d postgres
   ```
   
   When prompted, enter the password: `password`

2. Update the user's role:
   ```sql
   UPDATE users SET roles = 'ADMIN_USER' WHERE username = 'your_username';
   ```

   Or by email:
   ```sql
   UPDATE users SET roles = 'ADMIN_USER' WHERE email = 'your_email@example.com';
   ```

3. **Important**: The user must log out and log back in for the role change to take effect. This is because:
   - UI authentication stores role information in the session during login
   - API authentication validates roles from the JWT token, which is generated at login time

### Verifying Role Changes

- **For UI**: After logging out and logging back in, admin users will see the "Admin" link in the navigation bar and can access `/admin` pages
- **For API**: Generate a new JWT token by logging in again via the API endpoint

## REST APIs

### Authentication APIs (Public)

All authentication endpoints are public and do not require authentication:
- `POST /api/v1/auth/register` - Register a new user account (returns JWT token)
- `POST /api/v1/auth/login` - Authenticate user with username/email and password (returns JWT token)
- `GET /api/v1/auth/me` - Get current authenticated user information (requires authentication)

### Admin APIs (`admin-service` profile)

All admin endpoints require `ADMIN_USER` role and JWT authentication.

#### Products
- `POST /api/v1/products` - Create a new product
- `GET /api/v1/products` - Get all products (public, no auth required)
- `GET /api/v1/products/{id}` - Get product by ID (requires ADMIN_USER)
- `PUT /api/v1/products/{id}` - Update product
- `DELETE /api/v1/products/{id}` - Delete product
- `GET /api/v1/products/{id}/stock` - Get product stock details (physical, reserved, available)
- `PUT /api/v1/products/{id}/stock` - Update product total physical stock

#### Flash Sales
- `POST /api/v1/admin/flash_sale` - Create a new flash sale
  - Validates sale duration and time ranges
  - Prevents duplicate sales
- `GET /api/v1/admin/flash_sale` - List all flash sales with optional status and date filters
- `GET /api/v1/admin/flash_sale/{id}` - Get flash sale details by ID
- `PUT /api/v1/admin/flash_sale/{id}` - Update flash sale
- `DELETE /api/v1/admin/flash_sale/{id}` - Delete flash sale (only DRAFT status)
- `POST /api/v1/admin/flash_sale/{id}/cancel` - Cancel flash sale (DRAFT or ACTIVE status)
- `POST /api/v1/admin/flash_sale/{id}/items` - Add items to a DRAFT flash sale
- `PUT /api/v1/admin/flash_sale/{id}/items/{itemId}` - Update flash sale item (DRAFT sales only)
- `DELETE /api/v1/admin/flash_sale/{id}/items/{itemId}` - Remove flash sale item (DRAFT sales only)
- `GET /api/v1/admin/admin_api_status` - Check admin API status

#### Orders (Admin)
- `GET /api/v1/admin/orders` - List all orders with optional filters (status, date range, user ID)
- `GET /api/v1/admin/orders/{id}` - Get order details by ID (admin view)
- `PUT /api/v1/admin/orders/{id}/status` - Update order status with proper stock adjustments

#### Analytics
- `GET /api/v1/admin/analytics/sales` - Get sales performance metrics (total sales, items sold, conversion rates, top performing sales)
- `GET /api/v1/admin/analytics/revenue` - Get revenue reporting metrics (total revenue, average order value, refund rates)
- `GET /api/v1/admin/analytics/products` - Get product performance metrics (top selling products, stock utilization, low stock alerts)
- `GET /api/v1/admin/analytics/orders` - Get order statistics (order counts by status, average order quantity, success rates)

All analytics endpoints support optional date range filters (`startDate`, `endDate`) in ISO-8601 format.

### Client APIs (`api-service` profile)

All client endpoints require JWT authentication (except where noted).

#### Products
- `GET /api/v1/clients/products/{id}` - Get product details (client view)

#### Sales
- `GET /api/v1/clients/sales/active` - Get all active flash sales with remaining stock
- `GET /api/v1/clients/sales/draft/{days}` - Get draft sales scheduled within the next N days

#### Orders
- `POST /api/v1/clients/orders` - Create a new order for an active flash sale item
- `GET /api/v1/clients/orders` - Get user's order history with optional filters (status, date range)
- `GET /api/v1/clients/orders/{orderId}` - Get order details by ID (user's own orders only)
- `POST /api/v1/clients/orders/{orderId}/refund` - Request refund for a PAID order

## Technology Stack

- **Framework**: Spring Boot 4.0.1
- **Language**: Java 25
- **Database**: PostgreSQL 18.1 (with Flyway migrations)
- **Cache**: Redis 8.4.0 (for high-performance read operations)
- **Messaging**: RabbitMQ 4.2.2 (Spring Modulith events)
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Testing**: Testcontainers for integration tests
- **UI Framework**: Thymeleaf (server-side templating)
  - **Note**: Thymeleaf is used in this demo to keep the application in a single repository. In a production scenario, a separate frontend framework (e.g., NuxtJS or React) would be preferred for better separation of concerns and modern frontend development practices.

## Architecture

### Database Schema

The application uses PostgreSQL with the following key tables:

1. **products** - Master product catalog
2. **flash_sales** - Flash sale event metadata
3. **flash_sale_items** - Product-to-sale mappings with inventory allocation
4. **orders** - Purchase records
5. **users** - User accounts with authentication credentials and roles

### Caching Strategy

Redis is configured for caching:
- Product data (1-minute TTL)
- Active sales (1-minute TTL)
- Draft sales (1-minute TTL)

This reduces database load during high-traffic periods while maintaining data freshness.

### Scheduled Jobs

The application uses Quartz for scheduled tasks:
- **ActivateDraftSalesJob**: Runs every 30 seconds (configurable via `app.scheduler.interval-seconds`), transitions DRAFT sales to ACTIVE when their start time is reached
- **CompleteActiveSalesJob**: Runs every 30 seconds, transitions ACTIVE sales to COMPLETED when their end time has passed and releases unsold stock back to products

### Message Queue

RabbitMQ is used for asynchronous order processing:
- **order.processing**: New orders for payment processing
- **order.dispatch**: Paid orders ready for dispatch
- **order.payment.failed**: Failed payments for stock release
- **order.refund**: Refund notifications

### Profiles

The application supports multiple Spring profiles:
- `admin-service` - Enables admin APIs for product and flash sale management
- `api-service` - Enables client-facing APIs for browsing products and sales

Both profiles can be active simultaneously.

## Getting Started

### Prerequisites
- Java 25
- Maven
- Docker and Docker Compose

### Running the Application

1. Start infrastructure services:
   ```bash
   docker compose up -d
   ```
   This starts PostgreSQL, Redis, and RabbitMQ.

2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

3. Access Swagger UI:
   ```
   http://localhost:8080/swagger-ui.html
   ```

### Database Migrations

Flyway automatically applies database migrations on startup:
- `V1__CreateTables.sql` - Initial schema with products, flash sales, items, and orders
- `V2__ReservedQuantityForProducts.sql` - Adds reserved_count to products
- `V3__RemainingActiveStockView.sql` - Creates view for active sales with remaining stock
- `V4__AddOrderFields.sql` - Adds product_id, sold_price, and sold_quantity to orders table
- `V5__AddDispatchedOrderStatus.sql` - Adds DISPATCHED status to order_status enum
- `V6__AddOrderUserIndexes.sql` - Adds indexes on user_id and composite indexes for efficient order queries
- `V7__CreateUsersTable.sql` - Creates users table for authentication with username, email, password, and roles

### Loading Seed Data

The application includes a seed data file that populates the database with test data for development and testing purposes. This is optional but useful for quickly getting started with pre-configured users, products, flash sales, and orders.

#### What's Included in the Seed Data

The seed file (`src/test/resources/sql/Seed_All_Tables.sql`) contains:
- **2 test users** (user and admin) with pre-configured credentials
- **15 products** with various stock levels
- **10 flash sales** in different statuses (DRAFT, ACTIVE, COMPLETED)
- **18 flash sale items** linking products to sales
- **17 orders** in various states for testing order workflows

#### Loading the Seed Data

1. Ensure your PostgreSQL database is running (via `docker compose up -d`)

2. Load the seed data using `psql`:
   ```bash
   psql -h localhost -U postgres -d postgres -f src/test/resources/sql/Seed_All_Tables.sql
   ```
   
   When prompted, enter the password: `password`

   **Note**: The database name is `postgres` (as configured in `compose.yaml` and application configuration).

3. Verify the data was loaded:
   ```sql
   psql -h localhost -U postgres -d postgres
   ```
   Then run:
   ```sql
   SELECT username, email, roles FROM users;
   SELECT COUNT(*) FROM products;
   SELECT COUNT(*) FROM flash_sales;
   ```

#### Test User Credentials

After loading the seed data, you can use these pre-configured test users:

| Username | Email | Password | Role |
|----------|-------|----------|------|
| `user` | `user@example.com` | `password` | USER |
| `admin` | `admin@example.com` | `password` | ADMIN_USER |

**Using Test Users**:
- **UI Login**: Navigate to `/login` and use either username or email with password `password`
- **API Login**: Use the `/api/v1/auth/login` endpoint with username `user` or `admin` and password `password`

**Note**: These are test credentials for development only. In production, you should create your own users and never use these default passwords.

## Authentication & Security

The application implements a **dual authentication architecture** to support both REST API access and web UI access:

### API Authentication (JWT-based, Stateless)

API endpoints (`/api/**`) use JWT-based authentication:

- **Authentication Method**: Stateless JWT tokens
- **Token Location**: `Authorization: Bearer <token>` header
- **Session Management**: No session management (stateless)
- **CSRF Protection**: Disabled for API endpoints (not needed for stateless APIs)
- **Filter Chain**: Uses `JwtAuthenticationFilter` to extract and validate tokens
- **Filter Order**: API security filter chain has `@Order(1)` priority

**How it works**:
1. Client calls `/api/v1/auth/register` or `/api/v1/auth/login`
2. Server returns a JWT token containing user ID, username, and roles
3. Client includes token in `Authorization: Bearer <token>` header for subsequent API requests
4. `JwtAuthenticationFilter` validates the token and sets authentication in `SecurityContext`
5. No session is created - each request is authenticated independently

### UI Authentication (Form-based, Stateful)

Web UI endpoints (non-API routes) use form-based session authentication:

- **Authentication Method**: Form-based login with session management
- **Login Page**: `/login` (custom login page)
- **Login Processing**: `/login` (POST)
- **Session Management**: Stateful sessions with `SecurityContextRepository`
- **CSRF Protection**: Enabled for form submissions
- **Filter Chain**: UI security filter chain has `@Order(2)` priority

**How it works**:
1. User navigates to `/login` and submits credentials
2. Spring Security authenticates via `CustomUserDetailsService`
3. `CustomAuthenticationSuccessHandler`:
   - Persists `SecurityContext` using `SecurityContextRepository`
   - Stores session attributes (`isAuthenticated`, `isAdmin`, `userId`)
   - Redirects to home page
4. Subsequent requests use session cookie (`JSESSIONID`) for authentication
5. Session attributes enable role-based UI rendering (e.g., showing admin links)

### Common Security Features

Both authentication methods share:

- **User Registration**: Users can register with username, email, and password
- **User Roles**: Supports `USER` and `ADMIN_USER` roles
- **Password Security**: Passwords are hashed using BCrypt with strength 12
- **User Service**: Both methods use the same `UserService` and `UserRepository` for user management
- **Security Headers**: Includes HSTS, frame options, and content type options

### Protected Endpoints

- **Public Endpoints**: 
  - Authentication APIs (`/api/v1/auth/**`)
  - Public UI pages (`/`, `/login`, `/register`, `/sales`)
  - Swagger UI (`/swagger-ui.html`)
- **Authenticated Endpoints**: 
  - Client APIs (`/api/v1/clients/**`) - require JWT authentication
  - User UI pages (`/orders`) - require form-based authentication
- **Admin Endpoints**: 
  - Admin APIs (`/api/v1/admin/**`, `/api/v1/products/**`) - require `ADMIN_USER` role and JWT
  - Admin UI pages (`/admin/**`) - require `ADMIN_USER` role and form-based authentication

## Key Features

- **Inventory Management**: Tracks physical stock, reserved quantities, and allocated inventory per sale
- **Concurrent Access**: Designed to handle high-traffic scenarios with Redis caching
- **Data Integrity**: Database constraints prevent overselling and duplicate entries
- **Status Tracking**: Comprehensive status enums for sales and orders
- **Time-based Queries**: Efficient discovery of active and upcoming sales
- **API Separation**: Distinct admin and client APIs for different use cases
- **Authentication**: JWT-based stateless authentication with role-based access control
- **Order Processing**: Asynchronous order processing with RabbitMQ for payment and dispatch workflows
- **Analytics & Reporting**: Comprehensive analytics API for sales performance, revenue metrics, product performance, and order statistics

## Testing

The application includes comprehensive test coverage using Testcontainers for integration testing. Run tests with:

```bash
./mvnw test
```

## License

This is a skills demonstration project.
