---
name: Order Management with RabbitMQ
overview: Implement order creation, processing, and status management with RabbitMQ queues for asynchronous processing, including payment processing, refunds, dispatch, and email notifications.
todos:
  - id: "1"
    content: Add DISPATCHED status to OrderStatus enum and create database migration V5
    status: completed
  - id: "2"
    content: "Create DTOs: CreateOrderDto, OrderResponseDto, RefundOrderDto"
    status: completed
  - id: "3"
    content: "Create exception classes: SaleNotActiveException, InsufficientStockException, OrderNotFoundException, InvalidOrderStatusException"
    status: completed
  - id: "4"
    content: Create RabbitMQ configuration with queues, exchanges, and bindings
    status: completed
  - id: "5"
    content: Create NotificationService with dummy email sender (logs as INFO)
    status: completed
  - id: "6"
    content: Create PaymentService mock implementation
    status: completed
  - id: "7"
    content: Create OrderService with all business logic methods
    status: completed
  - id: "8"
    content: "Create RabbitMQ consumers: OrderProcessingConsumer, FailedPaymentConsumer, DispatchConsumer, RefundConsumer"
    status: completed
  - id: "9"
    content: Update ClientRestApi with order creation and refund endpoints
    status: completed
  - id: "10"
    content: Update GlobalExceptionHandler with new exception handlers
    status: completed
  - id: "11"
    content: Update repositories with new query methods
    status: completed
  - id: "12"
    content: Create Graphviz diagram for order status transitions
    status: completed
  - id: "13"
    content: Add RabbitMQ container to TestcontainersConfiguration
    status: completed
  - id: "14"
    content: Create comprehensive tests for all new functionality
    status: completed
  - id: "15"
    content: Run full test suite and fix any failures
    status: completed
isProject: false
---

# Order Management with RabbitMQ Integration

## Overview

This plan implements a complete order management system with asynchronous processing using RabbitMQ. The system handles order creation, payment processing, refunds, dispatch, and user notifications.

## Architecture Components

### 1. Database Changes

- **File**: `src/main/resources/db/migration/V5__AddDispatchedOrderStatus.sql`
- Add `DISPATCHED` to the `order_status` enum in PostgreSQL
- Update the Java `OrderStatus` enum to include `DISPATCHED`

### 2. Domain Model Updates

- **File**: `src/main/java/uk/co/aosd/flash/domain/OrderStatus.java`
- Add `DISPATCHED` status value

### 3. DTOs

- **File**: `src/main/java/uk/co/aosd/flash/dto/CreateOrderDto.java`
- Fields: `userId` (UUID), `flashSaleItemId` (UUID), `quantity` (Integer)
- Validation annotations

- **File**: `src/main/java/uk/co/aosd/flash/dto/OrderResponseDto.java`
- Fields: `orderId` (UUID), `status` (OrderStatus), `message` (String)

- **File**: `src/main/java/uk/co/aosd/flash/dto/RefundOrderDto.java`
- Fields: `orderId` (UUID)

### 4. Exception Classes

- **File**: `src/main/java/uk/co/aosd/flash/exc/SaleNotActiveException.java`
- Thrown when sale has ended (checking end time)

- **File**: `src/main/java/uk/co/aosd/flash/exc/InsufficientStockException.java`
- Thrown when not enough stock available

- **File**: `src/main/java/uk/co/aosd/flash/exc/OrderNotFoundException.java`
- Thrown when order doesn't exist

- **File**: `src/main/java/uk/co/aosd/flash/exc/InvalidOrderStatusException.java`
- Thrown when order status transition is invalid

### 5. RabbitMQ Configuration

- **File**: `src/main/java/uk/co/aosd/flash/config/RabbitMQConfig.java`
- Define queues: `order.processing`, `order.payment.failed`, `order.dispatch`, `order.refund`
- Define exchanges and bindings
- Configure message converters

### 6. Services

#### OrderService

- **File**: `src/main/java/uk/co/aosd/flash/services/OrderService.java`
- `createOrder(CreateOrderDto)`: Validates sale is active (check end time), checks stock, creates PENDING order, increments soldCount, queues for processing
- `processOrderPayment(UUID orderId)`: Attempts payment, updates to PAID and queues for dispatch, or updates to FAILED and queues for failed payment
- `handleRefund(UUID orderId)`: Decrements soldCount, sets status to REFUNDED, queues for refund processing, notifies user
- `processFailedPayment(UUID orderId)`: Decrements soldCount, sets status to FAILED, notifies user
- `processDispatch(UUID orderId)`: Validates PAID status, sets to DISPATCHED, decrements product totalPhysicalStock and reservedCount, notifies user

#### PaymentService (Mock)

- **File**: `src/main/java/uk/co/aosd/flash/services/PaymentService.java`
- `processPayment(UUID orderId, BigDecimal amount)`: Mock implementation that randomly succeeds/fails or always succeeds based on configuration

#### NotificationService

- **File**: `src/main/java/uk/co/aosd/flash/services/NotificationService.java`
- `sendOrderConfirmation(UUID userId, UUID orderId)`: Logs as INFO
- `sendPaymentFailedNotification(UUID userId, UUID orderId)`: Logs as INFO
- `sendRefundNotification(UUID userId, UUID orderId)`: Logs as INFO
- `sendDispatchNotification(UUID userId, UUID orderId)`: Logs as INFO

### 7. RabbitMQ Consumers

- **File**: `src/main/java/uk/co/aosd/flash/consumers/OrderProcessingConsumer.java`
- Listens to `order.processing` queue
- Calls `OrderService.processOrderPayment()`

- **File**: `src/main/java/uk/co/aosd/flash/consumers/FailedPaymentConsumer.java`
- Listens to `order.payment.failed` queue
- Calls `OrderService.processFailedPayment()`

- **File**: `src/main/java/uk/co/aosd/flash/consumers/DispatchConsumer.java`
- Listens to `order.dispatch` queue
- Calls `OrderService.processDispatch()`

- **File**: `src/main/java/uk/co/aosd/flash/consumers/RefundConsumer.java`
- Listens to `order.refund` queue
- Calls `NotificationService.sendRefundNotification()`

### 8. REST API Updates

- **File**: `src/main/java/uk/co/aosd/flash/controllers/ClientRestApi.java`
- Add `POST /api/v1/clients/orders`: Create new order
- Add `POST /api/v1/clients/orders/{orderId}/refund`: Request refund

### 9. Exception Handler Updates

- **File**: `src/main/java/uk/co/aosd/flash/errorhandling/GlobalExceptionHandler.java`
- Add handlers for new exception types

### 10. Repository Updates

- **File**: `src/main/java/uk/co/aosd/flash/repository/OrderRepository.java`
- Add query methods: `findByIdWithFlashSaleItem`, `findByIdWithProduct`

- **File**: `src/main/java/uk/co/aosd/flash/repository/FlashSaleItemRepository.java`
- Add method: `decrementSoldCount(UUID id, int decrement)`

- **File**: `src/main/java/uk/co/aosd/flash/repository/ProductRepository.java`
- Add method: `decrementStock(UUID id, int quantity)` for both totalPhysicalStock and reservedCount

### 11. Graphviz Diagram

- **File**: `docs/order_status_transitions.dot`
- State transition diagram showing: PENDING → PAID → DISPATCHED, PENDING → FAILED, PAID → REFUNDED

### 12. Test Updates

- **File**: `src/test/java/uk/co/aosd/flash/TestcontainersConfiguration.java`
- Add RabbitMQ container configuration

- **Files**: Multiple test files for:
- OrderService tests (create, process payment, refund, failed payment, dispatch)
- ClientRestApi order creation tests
- RabbitMQ consumer tests
- Integration tests with Testcontainers

## Implementation Details

### Order Creation Flow

1. Validate sale is active (check `endTime > currentTime`, not status)
2. Check available stock: `allocatedStock - soldCount >= requestedQuantity`
3. Create Order with PENDING status
4. Atomically increment `soldCount` on FlashSaleItem
5. Queue order ID to `order.processing` queue
6. Return OrderResponseDto with PENDING status or rejection reason

### Payment Processing Flow

1. Consumer receives order ID from `order.processing` queue
2. Call PaymentService.processPayment()
3. If successful: Update order to PAID, queue to `order.dispatch`
4. If failed: Update order to FAILED, queue to `order.payment.failed`

### Refund Flow

1. Validate order is in PAID status
2. Decrement `soldCount` on FlashSaleItem
3. Update order status to REFUNDED
4. Queue to `order.refund` for notification
5. Notify user

### Failed Payment Flow

1. Decrement `soldCount` on FlashSaleItem
2. Update order status to FAILED
3. Notify user

### Dispatch Flow

1. Validate order is in PAID status (hasn't been refunded)
2. Update order status to DISPATCHED
3. Decrement `totalPhysicalStock` and `reservedCount` on Product
4. Notify user

## Key Considerations

- All stock operations must be atomic (use database constraints and optimistic locking)
- Queue messages should contain order IDs, not full order objects
- Email notifications are logged as INFO messages for now
- Payment service is a mock that can be configured for testing
- All operations are transactional where appropriate