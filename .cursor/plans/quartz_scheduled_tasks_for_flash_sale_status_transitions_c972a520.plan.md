---
name: Quartz Scheduled Tasks for Flash Sale Status Transitions
overview: "Implement Quartz scheduled tasks to automatically transition FlashSale statuses: DRAFT→ACTIVE when start time passes, and ACTIVE→COMPLETED when end time passes. Both tasks will run at a configurable interval."
todos:
  - id: "1"
    content: "Add repository query methods to FlashSaleRepository: findDraftSalesReadyToActivate and findActiveSalesReadyToComplete"
    status: completed
  - id: "2"
    content: "Add service methods to FlashSalesService: activateDraftSales() and completeActiveSales()"
    status: completed
  - id: "3"
    content: Create ActivateDraftSalesJob class implementing Quartz Job interface
    status: completed
  - id: "4"
    content: Create CompleteActiveSalesJob class implementing Quartz Job interface
    status: completed
  - id: "5"
    content: Create QuartzConfig configuration class with scheduler, job details, and triggers
    status: completed
  - id: "6"
    content: Add scheduler interval configuration property to application-api-service.yaml
    status: completed
isProject: false
---

# Implementation Plan: Quartz Scheduled Tasks for Flash Sale Status Transitions

## Overview

Create Quartz scheduled jobs that automatically manage FlashSale status transitions based on time:

- **ActivateDraftSalesJob**: Transitions DRAFT sales to ACTIVE when their start time has passed
- **CompleteActiveSalesJob**: Transitions ACTIVE sales to COMPLETED when their end time has passed

Both jobs will run at a configurable interval specified in `application.yaml`.

## Components to Create/Modify

### 1. Repository Methods (`FlashSaleRepository.java`)

Add two query methods to find sales that need status updates:

- `findDraftSalesReadyToActivate(OffsetDateTime currentTime)` - Finds DRAFT sales where `startTime <= currentTime`
- `findActiveSalesReadyToComplete(OffsetDateTime currentTime)` - Finds ACTIVE sales where `endTime <= currentTime`

Both queries should use `LEFT JOIN FETCH` to eagerly load items and products (similar to existing `findDraftSalesWithinDays`).

### 2. Service Methods (`FlashSalesService.java`)

Add two transactional methods:

- `activateDraftSales()` - Queries for ready DRAFT sales, updates their status to ACTIVE, saves them
- `completeActiveSales()` - Queries for ready ACTIVE sales, updates their status to COMPLETED, saves them

Both methods should include logging for monitoring and debugging.

### 3. Quartz Job Classes

Create two job classes in `services/`:

- **`ActivateDraftSalesJob.java`** - Implements `Job`, injects `FlashSalesService`, calls `activateDraftSales()`
- **`CompleteActiveSalesJob.java`** - Implements `Job`, injects `FlashSalesService`, calls `completeActiveSales()`

Both jobs should handle exceptions gracefully and log execution details.

### 4. Quartz Configuration (`config/QuartzConfig.java`)

Create a configuration class that:

- Configures `SchedulerFactoryBean` with JDBC job store (using existing PostgreSQL datasource)
- Defines two `JobDetail` beans for the jobs
- Defines two `Trigger` beans that fire at the configured interval (using `SimpleTrigger` or `CronTrigger`)
- Uses `@Value` to read interval from `app.scheduler.interval-seconds` property

### 5. Configuration Properties (`application-api-service.yaml`)

Add scheduler configuration:

```yaml
app:
  settings:
    min-sale-duration-minutes: 5
  scheduler:
    interval-seconds: 30  # Default: check every 30 seconds
```

### 6. Database Migration (if needed)

Quartz requires tables for job persistence. Spring Boot Quartz starter should auto-create these, but verify if a manual migration is needed for production.

## Implementation Details

### Repository Queries

- Use JPQL queries with proper JOIN FETCH to avoid N+1 problems
- Order results by time for consistent processing
- Filter by status AND time condition

### Service Layer

- Use `@Transactional` to ensure atomic updates
- Log the count of sales updated in each execution
- Handle edge cases (e.g., sales already in target status)

### Job Execution

- Jobs should be stateless and idempotent
- Include try-catch blocks for error handling
- Log start/completion of job execution

### Configuration

- Use Spring Boot's Quartz auto-configuration where possible
- Configure job store to use JDBC (not in-memory) for persistence across restarts
- Set appropriate misfire policies for triggers

## Testing Considerations

- Unit tests for service methods
- Integration tests for Quartz jobs (using `@QuartzTest` or similar)
- Verify status transitions occur at correct times
- Test with different interval configurations