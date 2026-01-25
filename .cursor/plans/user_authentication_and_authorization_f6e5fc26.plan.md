---
name: User Authentication and Authorization
overview: Implement comprehensive user management, JWT-based authentication, and role-based authorization. Remove userId parameters from API methods and secure all endpoints except those that must remain public (e.g., health checks, registration, login).
todos:
  - id: "1"
    content: Add Spring Security and JWT dependencies to pom.xml
    status: completed
  - id: "2"
    content: Create User domain model (User entity, UserRole enum) and UserRepository
    status: completed
  - id: "3"
    content: Create Flyway migration V7__CreateUsersTable.sql for users table
    status: completed
  - id: "4"
    content: "Create DTOs: RegisterDto, LoginDto, AuthResponseDto, UserDto"
    status: completed
  - id: "5"
    content: Implement JwtTokenProvider service for token generation and validation
    status: completed
  - id: "6"
    content: Implement JwtAuthenticationFilter and JwtAuthenticationEntryPoint
    status: completed
  - id: "7"
    content: Create SecurityConfig with JWT authentication and role-based authorization
    status: completed
  - id: "8"
    content: Implement UserService with registration, authentication, and user lookup methods
    status: completed
  - id: "9"
    content: Create AuthController with /register, /login, and /me endpoints
    status: completed
  - id: "10"
    content: Create SecurityUtils helper to extract current user from SecurityContext
    status: completed
  - id: "11"
    content: "Update ClientRestApi: remove userId parameters, extract from JWT"
    status: completed
  - id: "12"
    content: "Update CreateOrderDto: remove userId field"
    status: completed
  - id: "13"
    content: Update OrderService methods to accept userId from context where needed
    status: completed
  - id: "14"
    content: Add @PreAuthorize annotations to all admin controllers (ProductRestApi, FlashSaleAdminRestApi, AdminOrderRestApi, AdminRestApi)
    status: completed
  - id: "15"
    content: Update OpenApiConfig to include JWT security scheme for Swagger documentation
    status: completed
  - id: "16"
    content: Update all existing tests to include JWT authentication and create test utilities
    status: completed
  - id: "17"
    content: Add tests for authentication endpoints, JWT validation, and role-based access control
    status: completed
isProject: false
---

# User Management and Authentication Implementation Plan

## Overview

This plan implements user management, JWT-based authentication, and role-based authorization for the Flash Sales application. All REST endpoints will require authentication except registration, login, and health/actuator endpoints. Admin endpoints will require the `ADMIN_USER` role.

## Architecture Changes

### 1. User Domain Model

- Create `User` entity with fields: `id` (UUID), `username`, `email`, `password` (hashed), `roles` (enum: `USER`, `ADMIN_USER`)
- Create `UserRepository` interface
- Create Flyway migration `V7__CreateUsersTable.sql` to add `users` table
- Create `UserDto`, `RegisterDto`, `LoginDto` for API communication

### 2. Security Configuration

- Add Spring Security dependencies to `pom.xml`:
  - `spring-boot-starter-security`
  - `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (for JWT handling)
  - `bcrypt` or use Spring Security's built-in `BCryptPasswordEncoder`
- Create `SecurityConfig` class in `uk.co.aosd.flash.config`:
  - Configure JWT authentication filter chain
  - Permit public access to: `/api/v1/auth/**`, `/actuator/**`, `/swagger-ui.html`, `/v3/api-docs/**`
  - Require authentication for all other endpoints
  - Require `ADMIN_USER` role for `/api/v1/admin/**` and `/api/v1/products/**`
  - Configure password encoder (BCrypt)
  - Disable CSRF for stateless JWT API

### 3. JWT Token Management

- Create `JwtTokenProvider` service:
  - Generate JWT tokens with user ID, username, and roles
  - Validate and parse JWT tokens
  - Extract user information from tokens
- Create `JwtAuthenticationFilter`:
  - Intercept requests and extract JWT from `Authorization: Bearer <token>` header
  - Validate token and set authentication context
- Create `JwtAuthenticationEntryPoint` for handling unauthorized access

### 4. Authentication Endpoints

- Create `AuthController` in `uk.co.aosd.flash.controllers`:
  - `POST /api/v1/auth/register` - User registration (public)
  - `POST /api/v1/auth/login` - User login (public, returns JWT token)
  - `GET /api/v1/auth/me` - Get current user info (authenticated)

### 5. User Service

- Create `UserService`:
  - `register(RegisterDto)` - Create new user with hashed password
  - `authenticate(LoginDto)` - Validate credentials and return JWT token
  - `findByUsername(String)` - Find user by username
  - `findById(UUID)` - Find user by ID
  - Default admin user creation (optional, for initial setup)

### 6. Controller Updates

#### Remove userId Parameters

- **ClientRestApi.java**:
  - `getOrderById(String orderId)` - Remove `userId` parameter, extract from JWT
  - `getOrders(...)` - Remove `userId` parameter, extract from JWT
  - `refundOrder(String orderId)` - Extract userId from JWT to validate ownership
- **CreateOrderDto.java**:
  - Remove `userId` field, extract from JWT in controller
- **OrderService.java**:
  - Update method signatures to accept `userId` from context rather than parameters where appropriate

#### Add Authentication Context

- Create `SecurityUtils` helper class to extract current user ID and roles from SecurityContext
- Update controllers to use `SecurityUtils.getCurrentUserId()` instead of parameters

### 7. Admin Endpoint Protection

- Add `@PreAuthorize("hasRole('ADMIN_USER')")` to admin controllers:
  - `ProductRestApi` - All methods
  - `FlashSaleAdminRestApi` - All methods
  - `AdminOrderRestApi` - All methods
  - `AdminRestApi` - All methods

### 8. Database Migration

- Create `V7__CreateUsersTable.sql`:
  ```sql
  CREATE TABLE users (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      username VARCHAR(50) UNIQUE NOT NULL,
      email VARCHAR(100) UNIQUE NOT NULL,
      password VARCHAR(255) NOT NULL,
      roles VARCHAR(20) NOT NULL DEFAULT 'USER',
      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
  );
  CREATE INDEX idx_users_username ON users(username);
  CREATE INDEX idx_users_email ON users(email);
  ```


### 9. Testing Updates

- Update all existing tests to include JWT authentication
- Create test utilities for generating test JWT tokens
- Add tests for:
  - User registration and login
  - JWT token validation
  - Role-based access control
  - Unauthorized access scenarios
- Update integration tests to authenticate before making requests

### 10. OpenAPI/Swagger Updates

- Update `OpenApiConfig` to include security scheme for JWT
- Add security requirements to protected endpoints in Swagger documentation
- Add example JWT token in Swagger UI

## Implementation Order

1. Add dependencies and create User domain model
2. Create database migration
3. Implement JWT token provider and authentication filter
4. Create security configuration
5. Implement UserService and AuthController
6. Update controllers to remove userId parameters and use JWT
7. Add role-based authorization to admin endpoints
8. Update all tests
9. Update OpenAPI documentation

## Files to Create

- `src/main/java/uk/co/aosd/flash/domain/User.java`
- `src/main/java/uk/co/aosd/flash/domain/UserRole.java` (enum)
- `src/main/java/uk/co/aosd/flash/repository/UserRepository.java`
- `src/main/java/uk/co/aosd/flash/dto/RegisterDto.java`
- `src/main/java/uk/co/aosd/flash/dto/LoginDto.java`
- `src/main/java/uk/co/aosd/flash/dto/AuthResponseDto.java`
- `src/main/java/uk/co/aosd/flash/dto/UserDto.java`
- `src/main/java/uk/co/aosd/flash/services/UserService.java`
- `src/main/java/uk/co/aosd/flash/services/JwtTokenProvider.java`
- `src/main/java/uk/co/aosd/flash/security/JwtAuthenticationFilter.java`
- `src/main/java/uk/co/aosd/flash/security/JwtAuthenticationEntryPoint.java`
- `src/main/java/uk/co/aosd/flash/security/SecurityConfig.java`
- `src/main/java/uk/co/aosd/flash/security/SecurityUtils.java`
- `src/main/java/uk/co/aosd/flash/controllers/AuthController.java`
- `src/main/resources/db/migration/V7__CreateUsersTable.sql`

## Files to Modify

- `pom.xml` - Add security and JWT dependencies
- `src/main/java/uk/co/aosd/flash/controllers/ClientRestApi.java` - Remove userId params, add auth
- `src/main/java/uk/co/aosd/flash/dto/CreateOrderDto.java` - Remove userId field
- `src/main/java/uk/co/aosd/flash/services/OrderService.java` - Update to accept userId from context
- `src/main/java/uk/co/aosd/flash/config/OpenApiConfig.java` - Add JWT security scheme
- All test files - Add authentication setup

## Security Considerations

- Passwords must be hashed using BCrypt (never stored in plain text)
- JWT tokens should have reasonable expiration times (e.g., 24 hours)
- Use HTTPS in production
- Validate JWT token signature and expiration on every request
- Admin endpoints must verify role, not just authentication
- User can only access their own orders (enforced in service layer)