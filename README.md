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

## Authentication & Security

The application uses JWT (JSON Web Token) based authentication:

- **User Registration**: Users can register with username, email, and password
- **User Roles**: Supports `USER` and `ADMIN_USER` roles
- **JWT Tokens**: Stateless authentication using JWT tokens in the `Authorization: Bearer <token>` header
- **Password Security**: Passwords are hashed using BCrypt with strength 12
- **Protected Endpoints**: 
  - Admin endpoints require `ADMIN_USER` role
  - Client endpoints require authentication (except public product browsing)
  - Authentication endpoints are public
- **Security Headers**: Includes HSTS, frame options, and content type options

## Key Features

- **Inventory Management**: Tracks physical stock, reserved quantities, and allocated inventory per sale
- **Concurrent Access**: Designed to handle high-traffic scenarios with Redis caching
- **Data Integrity**: Database constraints prevent overselling and duplicate entries
- **Status Tracking**: Comprehensive status enums for sales and orders
- **Time-based Queries**: Efficient discovery of active and upcoming sales
- **API Separation**: Distinct admin and client APIs for different use cases
- **Authentication**: JWT-based stateless authentication with role-based access control
- **Order Processing**: Asynchronous order processing with RabbitMQ for payment and dispatch workflows

## Testing

The application includes comprehensive test coverage using Testcontainers for integration testing. Run tests with:

```bash
./mvnw test
```

## License

This is a skills demonstration project.
