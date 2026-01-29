# Getting Started

This guide will help you set up and run the Flash Sales Demo Application on your local machine.

## How to Pull the Repo

Clone the repository to your local machine:

```bash
git clone <repository-url>
cd flash
```

Replace `<repository-url>` with the actual repository URL.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 25** - Required for running the Spring Boot application
- **Maven** - Build tool (or use the included Maven wrapper `./mvnw`)
- **Docker and Docker Compose** - Required for running infrastructure services (PostgreSQL, Redis, RabbitMQ)

## Building the Application

To build the application without running tests:

```bash
./mvnw clean install -DskipTests
```

This will compile the application and create the necessary artifacts without executing the test suite.

**Note**: For a full build including tests, run:
```bash
./mvnw clean install
```

## Starting Infrastructure Services

The application requires PostgreSQL, Redis, and RabbitMQ to run. These services are configured in `compose.yaml` and can be started using Docker Compose:

```bash
docker compose up -d
```

This command will:
- Start PostgreSQL on port 5432
- Start Redis on port 6379
- Start RabbitMQ on port 5672 (management UI on port 15672)

The `-d` flag runs the containers in detached mode (in the background).

To verify the services are running:
```bash
docker compose ps
```

## Database Migrations

Flyway automatically applies database migrations on application startup. The following migrations are included:

- `V1__CreateTables.sql` - Initial schema with products, flash sales, items, and orders
- `V2__ReservedQuantityForProducts.sql` - Adds reserved_count to products
- `V3__RemainingActiveStockView.sql` - Creates view for active sales with remaining stock
- `V4__AddOrderFields.sql` - Adds product_id, sold_price, and sold_quantity to orders table
- `V5__AddDispatchedOrderStatus.sql` - Adds DISPATCHED status to order_status enum
- `V6__AddOrderUserIndexes.sql` - Adds indexes on user_id and composite indexes for efficient order queries
- `V7__CreateUsersTable.sql` - Creates users table for authentication with username, email, password, and roles

No manual migration steps are required - Flyway handles this automatically when the application starts.

## Starting the Application

Once the infrastructure services are running, start the Spring Boot application:

```bash
./mvnw spring-boot:run
```

The application will start on port 8080 by default. You should see startup logs indicating:
- Database connection established
- Flyway migrations applied
- Application context loaded
- Server started on port 8080

## Loading Seed Data

The application includes a seed data file that populates the database with test data for development and testing purposes. This is optional but useful for quickly getting started with pre-configured users, products, flash sales, and orders.

### What's Included in the Seed Data

The seed file (`src/test/resources/sql/Seed_All_Tables.sql`) contains:
- **2 test users** (user and admin) with pre-configured credentials
- **15 products** with various stock levels
- **10 flash sales** in different statuses (DRAFT, ACTIVE, COMPLETED)
- **18 flash sale items** linking products to sales
- **17 orders** in various states for testing order workflows

### Loading the Seed Data

1. Ensure your PostgreSQL database is running (via `docker compose up -d`)

2. Load the seed data using `psql`:
   ```bash
   psql -h localhost -U postgres -d postgres -f src/test/resources/sql/Seed_All_Tables.sql
   ```
   
   When prompted, enter the password: `password`

   **Note**: The database name is `postgres` (as configured in `compose.yaml` and application configuration).

3. Verify the data was loaded:
   ```bash
   psql -h localhost -U postgres -d postgres
   ```
   Then run:
   ```sql
   SELECT username, email, roles FROM users;
   SELECT COUNT(*) FROM products;
   SELECT COUNT(*) FROM flash_sales;
   ```

## Logging In

After loading the seed data, you can log in using the pre-configured test users (see [Test User Credentials](#test-user-credentials) below).

### UI Login

1. Navigate to `http://localhost:8080/login` in your browser
2. Enter your username (or email) and password
3. Submit the form
4. Upon successful authentication, you'll be redirected to the home page

**Note**: The UI uses form-based session authentication. Session attributes (`isAuthenticated`, `isAdmin`, `userId`) are set for UI access control.

### API Login

Use the REST API endpoint for programmatic access:

```bash
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "password"
}
```

**Response**: Returns a JWT token along with user information. Include this token in subsequent API requests using the `Authorization: Bearer <token>` header.

**Note**: The API uses JWT-based authentication (stateless), while the UI uses form-based session authentication (stateful).

## Health and Readiness Probes

The application exposes Spring Boot Actuator health endpoints suitable for Kubernetes liveness and readiness probes:

- **`/actuator/health/liveness`** — Liveness: process is alive (ping only). Use for Kubernetes liveness probes.
- **`/actuator/health/readiness`** — Readiness: process plus external dependencies (database, Redis, RabbitMQ). Returns 200 when the app can serve traffic; returns 503 if DB, Redis, or RabbitMQ is down. Use for Kubernetes readiness probes.
- **`/actuator/health`** — Full health (all indicators). Use for debugging; response shows which component failed when unhealthy.

All health endpoints are unauthenticated so orchestrators can probe them without credentials.

## Accessing Swagger UI

Once the application is running, you can access the interactive API documentation at:

```
http://localhost:8080/swagger-ui.html
```

Swagger UI provides a web-based interface to explore and test all available REST API endpoints.

## Test User Credentials

After loading the seed data, you can use these pre-configured test users:

| Username | Email | Password | Role |
|----------|-------|----------|------|
| `user` | `user@example.com` | `password` | USER |
| `admin` | `admin@example.com` | `password` | ADMIN_USER |

**Using Test Users**:
- **UI Login**: Navigate to `/login` and use either username or email with password `password`
- **API Login**: Use the `/api/v1/auth/login` endpoint with username `user` or `admin` and password `password`

**Note**: These are test credentials for development only. In production, you should create your own users and never use these default passwords.

## Stopping the System

To stop the application and infrastructure services:

1. **Stop the Spring Boot application**: Press `Ctrl+C` in the terminal where the application is running

2. **Stop Docker Compose services**:
   ```bash
   docker compose down
   ```
   
   This will stop and remove all containers. To also remove volumes (which will delete all data):
   ```bash
   docker compose down -v
   ```

**Warning**: Using `docker compose down -v` will permanently delete all database data. Use with caution.

## Next Steps

- Explore the [README.md](README.md) for detailed information about the application architecture, APIs, and features
- Check out the Swagger UI at `http://localhost:8080/swagger-ui.html` to explore the REST APIs
- Review the admin dashboard at `http://localhost:8080/admin` (requires admin login)
- Browse active flash sales at `http://localhost:8080/sales`
