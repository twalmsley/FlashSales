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
  - Order status: `PENDING`, `PAID`, `FAILED`, or `REFUNDED`
  - Unique constraint: one purchase per user per flash sale item
  - Automatic timestamp tracking

### Stock Management
- Products maintain `total_physical_stock` and `reserved_count`
- Flash sale items track `allocated_stock` and `sold_count`
- A database view `remaining_active_stock` provides real-time visibility into:
  - Active flash sales with remaining inventory
  - Products available for purchase
  - Current stock levels and pricing

## REST APIs

### Admin APIs (`admin-service` profile)

#### Products
- `POST /api/v1/products` - Create a new product
- `GET /api/v1/products` - Get all products
- `GET /api/v1/products/{id}` - Get product by ID
- `PUT /api/v1/products/{id}` - Update product
- `DELETE /api/v1/products/{id}` - Delete product

#### Flash Sales
- `POST /api/v1/admin/flash_sale` - Create a new flash sale
  - Validates sale duration and time ranges
  - Prevents duplicate sales
- `GET /api/v1/admin/admin_api_status` - Check admin API status

### Client APIs (`api-service` profile)

#### Products
- `GET /api/v1/clients/products/{id}` - Get product details (client view)

#### Sales
- `GET /api/v1/clients/sales/active` - Get all active flash sales with remaining stock
- `GET /api/v1/clients/sales/draft/{days}` - Get draft sales scheduled within the next N days

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

## Key Features

- **Inventory Management**: Tracks physical stock, reserved quantities, and allocated inventory per sale
- **Concurrent Access**: Designed to handle high-traffic scenarios with Redis caching
- **Data Integrity**: Database constraints prevent overselling and duplicate entries
- **Status Tracking**: Comprehensive status enums for sales and orders
- **Time-based Queries**: Efficient discovery of active and upcoming sales
- **API Separation**: Distinct admin and client APIs for different use cases

## Testing

The application includes comprehensive test coverage using Testcontainers for integration testing. Run tests with:

```bash
./mvnw test
```

## License

This is a skills demonstration project.
