# Flash Sales

## 1. The "Flash-Sale" Inventory System

This project simulates a high-traffic e-commerce event where thousands of users try to buy limited items simultaneously.

- **The Architecture:**
    
    - **Main Service (Spring Boot):** Handles REST requests for viewing products and placing orders.
        
    - **Redis Cache:** Stores "Real-time Stock" counts. Instead of hitting Postgres for every click, you decrement the stock in Redis using atomic operations (like `DECR`).
        
    - **Background Worker (Separate Process):** A scheduled task or consumer that periodically syncs the "Sold" count from Redis back to PostgreSQL to ensure data persistence.
            
    - **Concurrency:** It must handle "Double-sell" problems (selling 101 items when there are only have 100).
        
    - **Scaling:** It can dockerize the Main Service and spin up 3 instances. Redis acts as the shared "Source of Truth" between them.
        
    - **Resilience:** What happens if Redis goes down? It will need a fallback strategy to Postgres.

---

## 2. Core Architecture & Process Interaction

It will implement two distinct processes (which can run in separate Docker containers):

1. **API Service (The Hot Path):**
    
    - **Role:** Handles incoming REST traffic.
        
    - **Interaction:** When a user buys an item, the service checks and decrements stock in **Redis** using an atomic Lua script or `DECR`.
        
    - **Scaling:** It can run 5 instances of this service; they all talk to the same Redis instance.
        
2. **Inventory Sync Worker (The Reliable Path):**
    
    - **Role:** A background "Scheduled" task (using `@Scheduled` in Spring).
        
    - **Interaction:** Every 10 seconds, it reads the "Sold Count" from Redis and executes a batch update to **PostgreSQL**. This ensures that even if Redis is wiped, the DB eventually reflects reality.
        

---
