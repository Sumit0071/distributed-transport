# Uber Clone - Distributed Ride-Sharing System

A production-grade, distributed ride-sharing backend implemented with Spring Boot, PostgreSQL, and Redis. This system features real-time driver matching using spatial indexing, distributed locking, and a resilient marketplace engine.

## 🚀 System Architecture

### Core Components
- **Spatial Dispatch Engine**: Uses Redis GEO indexing (`GEOADD`, `GEORADIUS`) for low-latency driver discovery within a 5km radius.
- **Distributed Marketplace Logic**: 
  - **Grid-based Bucketing**: Divides the map into 100x100 grid cells to track real-time demand and supply.
  - **Surge Pricing**: Dynamic multipliers (1.0x - 3.0x) calculated based on demand/supply ratios.
  - **Hotspot Detection**: Identifies areas with high demand and low supply for system optimization.
- **Asynchronous Retry System**: 
  - Failed matches are offloaded to a Redis-backed queue.
  - A background worker processes retries with **Exponential Backoff** (2s, 5s, 10s, 20s, 40s).
  - **Dead Letter Queue (DLQ)**: Persists ride IDs that exceed maximum retry attempts for manual intervention.
- **Concurrency Control**: Distributed locking using Redis (`SET NX PX`) ensures a driver is never double-assigned to multiple rides.
- **Fault Tolerance**: 
  - **Circuit Breaker**: Automatically falls back to DB-based matching if Redis becomes unavailable.
  - **Idempotency**: Prevents duplicate ride requests via `X-Idempotency-Key` header support.
  - **Graceful Shutdown**: Ensures the retry worker finishes current batches without data loss.

## 🛠 Technology Stack
- **Backend Framework**: Spring Boot 3.2.2 with Java 21
- **Data Persistence**: 
  - **PostgreSQL 15+**: Primary relational storage for users, profiles, and ride history.
  - **Redis**: High-performance spatial indexing, distributed locking, and real-time marketplace metrics.
- **Messaging**: Spring WebSocket (STOMP) for real-time ride state updates to clients.
- **Infrastructure**: 
  - **Spring Data JPA & Hibernate**: Robust ORM and data access layer.
  - **Maven**: Dependency management and build automation.
  - **Lombok**: Reduced boilerplate for domain models and DTOs.
  - **Spring Validation**: JSR-303/JSR-380 based input validation.

## 📋 Features
- **Production Matching**: Atomic distributed locking and GEO-spatial search.
- **Live Movement Sync**: Driver location updates with a 20-meter threshold to optimize Redis write load.
- **Smart Dispatch**: Scoring based on Driver Rating (60%) and ETA (40%).
- **Resilient Retry**: Background matching worker with self-healing cache logic.
- **Marketplace Intelligence**: Real-time surge, hotspots, and demand/supply tracking.
- **Observability**: Comprehensive metrics for matching latency, success rates, and system health.
- **Security**: Role-based access control (RIDER, DRIVER, ADMIN) and input sanitization.

## 🗄 Database Design

### Schema Overview
The system uses a highly indexed PostgreSQL schema designed for high-concurrency ride operations.

#### Core Tables
- **`users`**: Central identity store (Rider, Driver, Admin).
- **`driver_profiles`**: Extended driver data including vehicle details, availability, and real-time coordinates.
- **`rides`**: Transactional store for ride requests, status history, and fare details.
- **`payments`**: Records completed transactions for rides.

### Critical Indexes
```sql
-- Fast lookup for auth and roles
CREATE INDEX idx_user_phone ON users(phone);
CREATE INDEX idx_user_role ON users(role);

-- Spatial and availability optimization
CREATE INDEX idx_driver_available ON driver_profiles(is_available);
CREATE INDEX idx_driver_location ON driver_profiles(current_latitude, current_longitude);

-- Ride lifecycle performance
CREATE INDEX idx_ride_status ON rides(status);
CREATE INDEX idx_ride_requested_at ON rides(requested_at DESC);
```

## 🔑 Redis Key Design
| Key Pattern | Purpose | TTL / Type |
|-------------|---------|------------|
| `driver:location:{id}` | Real-time coordinates and metadata | HASH |
| `drivers:geo` | Spatial index for all available drivers | GEOSET |
| `driver:lock:{id}` | Distributed lock for driver assignment | 30s / STRING |
| `queue:ride:retry` | Asynchronous matching retry queue | LIST |
| `queue:ride:dead` | Dead Letter Queue for failed matches | LIST |
| `ride:retry:count:{id}` | Current retry attempt for a ride | 10m / STRING |
| `ride:retry:time:{id}` | Timestamp for next scheduled retry | 10m / STRING |
| `area:demand:{cellId}` | Local demand counter for grid cell | 5m / STRING |
| `area:supply:{cellId}` | Local supply counter for grid cell | 5m / STRING |
| `hotspots:active` | Set of currently active hotspot cell IDs | SET |
| `idempotency:ride:{key}` | Stores processed idempotency keys | 10m / STRING |

## 🔄 Ride Lifecycle & State Machine
The system enforces strict state transitions to maintain data consistency.

1. **REQUESTED**: Rider submits a request. System attempts initial match via Redis GEO.
2. **ACCEPTED**: Driver is assigned via atomic lock. Lock is released only after DB persistence.
3. **STARTED**: Driver confirms pickup. Journey tracking begins.
4. **COMPLETED**: Journey ends. Payment is processed. Driver marked AVAILABLE in both DB and Redis GEO.
5. **CANCELLED**: Terminal state for aborted rides (user cancel or system timeout).

### Transition Rules
- Only `AVAILABLE` drivers can transition a ride to `ACCEPTED`.
- State transitions are wrapped in `@Transactional` to ensure atomicity across DB and Cache.

## 🚦 API Reference

### Ride Operations
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/rides/request` | Create ride request (supports `X-Idempotency-Key`) |
| `POST` | `/api/rides/{id}/accept` | Driver accepts an assigned ride |
| `POST` | `/api/rides/{id}/start` | Driver marks the journey as started |
| `POST` | `/api/rides/{id}/complete` | Driver completes the journey and triggers payment |
| `POST` | `/api/rides/{id}/cancel` | Aborts the ride request |

### Driver Operations
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/drivers/nearest` | Spatial search for drivers near coordinates |
| `POST` | `/api/drivers/location` | Stream live location (20m movement threshold applied) |

### System & Observability
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/system/health` | Comprehensive health check (DB, Redis, Metrics) |
| `GET` | `/api/system/market-stats` | Real-time marketplace intelligence (Surge, Hotspots) |

## ⚙️ How to Run
1. **Database Setup**: Create a PostgreSQL database named `uber_clone`.
2. **Cache Setup**: Ensure Redis is running on `localhost:6379`.
3. **Environment**: Update `src/main/resources/application.yml` with your local credentials.
4. **Build & Run**: 
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

## 🛡 System Guarantees
- **No Double Assignment**: Guaranteed by atomic Redis `SET NX` locking.
- **Eventually Consistent Matching**: Background worker ensures matches are eventually fulfilled via exponential backoff.
- **Failure Resilience**: Automatic fallback to DB matching if Redis becomes unreachable (Circuit Breaker).
- **Data Integrity**: Enforced by foreign key constraints and transactional state transitions.
