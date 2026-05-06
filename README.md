# 🏟️ Sportify Booking — Microservices

> **Stack:** Quarkus 3.8 · PostgreSQL · Keycloak · Docker

## Architecture

```
Client → API Gateway (8080) → [Keycloak JWT validation]
                             ├── auth-service    (8081) → auth_db
                             ├── field-service   (8082) → field_db
                             ├── booking-service (8083) → booking_db
                             └── payment-service (8084) → payment_db
```

## Project Structure

```
backend/
├── pom.xml                         # Parent POM
├── docker-compose.yml
├── common-lib/                     # Shared DTOs & exceptions
│   └── src/main/java/com/sportify/common/
│       ├── dto/ApiResponse.java
│       └── exception/
│           ├── ServiceException.java
│           └── GlobalExceptionMapper.java
│
├── api-gateway/                    # :8080 — Route & validate JWT
│   └── src/main/resources/application.properties
│
├── auth-service/                   # :8081 — User profile + Keycloak
│   └── src/main/java/com/sportify/auth/
│       ├── entity/User.java
│       ├── dto/AuthDto.java
│       └── resource/AuthResource.java
│
├── field-service/                  # :8082 — Sports, Locations, Fields, Prices
│   └── src/main/java/com/sportify/field/
│       ├── entity/{Sport, Location, FieldType, Field, Price}.java
│       └── resource/FieldResource.java
│
├── booking-service/                # :8083 — Bookings
│   └── src/main/java/com/sportify/booking/
│       ├── entity/Booking.java
│       ├── dto/BookingDto.java
│       ├── client/FieldServiceClient.java
│       └── resource/BookingResource.java
│
└── payment-service/                # :8084 — Payments (VNPay/MoMo)
    └── src/main/java/com/sportify/payment/
        ├── entity/Payment.java
        ├── dto/PaymentDto.java
        └── resource/PaymentResource.java
```

## Quick Start

### 1. Start Infrastructure

```bash
cd backend
docker-compose up -d auth-db field-db booking-db payment-db keycloak
```

### 2. Configure Keycloak

1. Mở http://localhost:8080 → đăng nhập `admin/admin123`
2. Tạo realm: `sportify`
3. Tạo client: `auth-service`, `field-service`, `booking-service`, `payment-service`
4. Tạo roles: `USER`, `ADMIN`

### 3. Build & Run Services (Dev Mode)

```bash
# Terminal 1 — build common-lib trước
cd backend/common-lib && mvn install -q

# Terminal 2
cd backend/auth-service && mvn quarkus:dev

# Terminal 3
cd backend/field-service && mvn quarkus:dev

# Terminal 4
cd backend/booking-service && mvn quarkus:dev

# Terminal 5
cd backend/payment-service && mvn quarkus:dev
```

### 4. Swagger UI

| Service | URL |
|---|---|
| auth-service | http://localhost:8081/q/swagger-ui |
| field-service | http://localhost:8082/q/swagger-ui |
| booking-service | http://localhost:8083/q/swagger-ui |
| payment-service | http://localhost:8084/q/swagger-ui |

## API Summary

### Auth Service
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /api/v1/auth/register | ❌ | Register |
| POST | /api/v1/auth/login | ❌ | Login → JWT |
| GET | /api/v1/auth/me | ✅ | My profile |

### Field Service
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/v1/fields | ❌ | List fields |
| GET | /api/v1/fields/{id}/availability | ❌ | Check availability |
| GET | /api/v1/fields/{id}/price | ❌ | Calculate price |
| POST | /api/v1/fields | ADMIN | Create field |

### Booking Service
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /api/v1/bookings | ✅ | Create booking |
| GET | /api/v1/bookings | ✅ | My bookings |
| PATCH | /api/v1/bookings/{id}/cancel | ✅ | Cancel |

### Payment Service
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /api/v1/payments | ✅ | Initiate payment |
| GET | /api/v1/payments/booking/{id} | ✅ | Payment by booking |
| POST | /api/v1/payments/vnpay/callback | ❌ | VNPay callback |

## Entity → Service Mapping

| Entity | Service | Database |
|---|---|---|
| User | auth-service | auth_db |
| Sport | field-service | field_db |
| Location | field-service | field_db |
| FieldType | field-service | field_db |
| Field | field-service | field_db |
| Price | field-service | field_db |
| Booking | booking-service | booking_db |
| Payment | payment-service | payment_db |

## Cross-Service Communication

```
booking-service → field-service  : REST (check availability, get price)
payment-service → booking-service: REST (confirm booking after payment)
```
> **Không** dùng @ManyToOne cross-service. Thay bằng **ID reference**.

## Ports

| Service | Port |
|---|---|
| Keycloak | 8180 |
| auth-service | 8081 |
| field-service | 8082 |
| booking-service | 8083 |
| payment-service | 8084 |
| auth-db | 5432 |
| field-db | 5433 |
| booking-db | 5434 |
| payment-db | 5435 |
