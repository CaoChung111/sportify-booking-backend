# Hướng dẫn Chạy Hệ thống Sportify Booking Backend

## Yêu cầu Cài đặt Trước

| Phần mềm | Phiên bản | Tải về |
|----------|-----------|--------|
| **Java JDK** | 17+ | https://adoptium.net |
| **Maven** | 3.9+ | https://maven.apache.org |
| **Docker Desktop** | Mới nhất | https://docker.com |
| **MySQL** (local, nếu không dùng Docker) | 8.0 | https://dev.mysql.com |

> Kiểm tra phiên bản: `java -version`, `mvn -version`, `docker -v`

---

## Cách 1: Chạy Local (Không Docker) — Nhanh nhất để dev

### Bước 1 — Cài Keycloak

Keycloak là bắt buộc để Auth Service hoạt động. Chạy bằng Docker (chỉ cần 1 lệnh):

```bash
docker run -d --name sportify-keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin123 \
  quay.io/keycloak/keycloak:24.0.3 start-dev
```

Keycloak sẽ chạy tại: **http://localhost:8180**

### Bước 2 — Cấu hình Keycloak Realm

Mở trình duyệt → `http://localhost:8180` → Đăng nhập `admin / admin123`

**Tạo Realm:**
1. Click **"Create realm"** (góc trên trái)
2. **Realm name**: `sportify`
3. Click **Create**

**Tạo Client `sportify-app`** (Public Client cho người dùng đăng nhập):
1. Vào **Clients** → **Create client**
2. **Client ID**: `sportify-app`
3. **Client authentication**: `OFF` (Public)
4. **Valid redirect URIs**: `http://localhost:*`
5. Save

**Tạo Client `auth-service`** (Confidential Client cho Admin API):
1. Vào **Clients** → **Create client**
2. **Client ID**: `auth-service`
3. Click **Next**
4. Bật **Client authentication**: `ON`
5. Bật **Service account roles**: `ON` (nếu có tùy chọn này)
6. Click **Save**
7. **CẤP QUYỀN ADMIN (Rất quan trọng - Nếu thiếu sẽ bị lỗi 401/403):**
   - Chuyển sang tab **Service account roles** (hoặc tab **Roles**)
   - Click **Assign role**
   - Click nút **Filter by realm roles** chuyển thành **Filter by clients**
   - Tìm và chọn `realm-management manage-users`
   - Click **Assign**
8. Vào tab **Credentials** → Copy **Client Secret**
9. Mở file `auth-service/src/main/resources/application.properties`
10. Cập nhật dòng: `quarkus.keycloak.admin-client.client-secret=<SECRET_VỪA_COPY>`

**Tạo Role `ADMIN`** (cho các endpoint quản lý):
1. Vào **Realm roles** → **Create role**
2. **Role name**: `ADMIN`
3. Save

### Bước 3 — Tạo Database

Mở MySQL client (MySQL Workbench, DBeaver, hoặc terminal):

```sql
-- Cách 1: Chạy file tổng hợp
source C:/Users/Acer/Documents/Ky_6/Java_nang_cao/backend/init_databases.sql;

-- Cách 2: Tạo thủ công từng DB (nếu Flyway tự tạo bảng)
CREATE DATABASE auth_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE field_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo user chung (nếu dùng user 'sportify' thay vì root)
CREATE USER 'sportify'@'localhost' IDENTIFIED BY 'sportify123';
GRANT ALL PRIVILEGES ON auth_db.*    TO 'sportify'@'localhost';
GRANT ALL PRIVILEGES ON field_db.*   TO 'sportify'@'localhost';
GRANT ALL PRIVILEGES ON booking_db.* TO 'sportify'@'localhost';
GRANT ALL PRIVILEGES ON payment_db.* TO 'sportify'@'localhost';
FLUSH PRIVILEGES;
```

> **Lưu ý quan trọng**: Nếu bạn dùng `dev mode` (Flyway tự tạo bảng từ migration files), chỉ cần tạo 4 database trống là đủ. **Không cần chạy** `init_databases.sql`.

### Bước 4 — Kiểm tra cấu hình application.properties

Mỗi service có file `src/main/resources/application.properties`. Kiểm tra và điều chỉnh nếu cần:

**auth-service** (`src/main/resources/application.properties`):
```properties
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/auth_db
quarkus.datasource.username=root        # hoặc sportify
quarkus.datasource.password=123456      # đổi thành password của bạn
quarkus.oidc.auth-server-url=http://localhost:8180/realms/sportify
quarkus.keycloak.admin-client.server-url=http://localhost:8180
quarkus.keycloak.admin-client.client-secret=<SECRET_KEYCLOAK>
```

**field-service** (`src/main/resources/application.properties`):
```properties
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/field_db
quarkus.datasource.username=root
quarkus.datasource.password=123456
```

**booking-service** (`src/main/resources/application.properties`):
```properties
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/booking_db
quarkus.datasource.username=root
quarkus.datasource.password=123456
field-service/mp-rest/url=http://localhost:8082
```

**payment-service** (`src/main/resources/application.properties`):
```properties
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/payment_db
quarkus.datasource.username=root
quarkus.datasource.password=123456
booking-service/mp-rest/url=http://localhost:8083
vnpay.tmn-code=YOUR_TMN_CODE        # đăng ký tại sandbox.vnpayment.vn
vnpay.hash-secret=YOUR_HASH_SECRET
```

### Bước 5 — Chạy Các Service (Mở 4 Terminal)

Chạy **theo thứ tự** (service sau phụ thuộc service trước):

**Terminal 1 — Field Service** (Port 8082):
```bash
cd C:\Users\Acer\Documents\Ky_6\Java_nang_cao\backend\field-service
mvn quarkus:dev
```

**Terminal 2 — Auth Service** (Port 8081):
```bash
cd C:\Users\Acer\Documents\Ky_6\Java_nang_cao\backend\auth-service
mvn quarkus:dev
```

**Terminal 3 — Booking Service** (Port 8083):
```bash
cd C:\Users\Acer\Documents\Ky_6\Java_nang_cao\backend\booking-service
mvn quarkus:dev
```

**Terminal 4 — Payment Service** (Port 8084):
```bash
cd C:\Users\Acer\Documents\Ky_6\Java_nang_cao\backend\payment-service
mvn quarkus:dev
```

> **Dev Mode** tự động:
> - Hot reload khi thay đổi code
> - Tắt OIDC validation (`%dev.quarkus.oidc.enabled=false`) — không cần JWT thật
> - Flyway tự động tạo và seed bảng
> - Swagger UI luôn bật

---

## Cách 2: Chạy Toàn bộ bằng Docker Compose — Đơn giản nhất

### Bước 1 — Build tất cả service

```bash
cd C:\Users\Acer\Documents\Ky_6\Java_nang_cao\backend
mvn clean package -DskipTests
```

### Bước 2 — Khởi động toàn bộ hệ thống

```bash
docker-compose up -d
```

Docker Compose sẽ tự động khởi động:
- 4 MySQL databases (ports 3306–3309)
- Keycloak + PostgreSQL (port 8180)
- 4 Microservices (ports 8081–8084)
- API Gateway (port 8090)

### Bước 3 — Kiểm tra trạng thái

```bash
docker-compose ps
docker-compose logs -f auth-service
```

### Dừng hệ thống

```bash
docker-compose down          # dừng nhưng giữ data
docker-compose down -v       # dừng và xóa sạch data
```

---

## Kiểm tra Sau Khi Chạy

### Swagger UI (API Documentation)

| Service | Swagger URL |
|---------|-------------|
| Auth Service | http://localhost:8081/q/swagger-ui |
| Field Service | http://localhost:8082/q/swagger-ui |
| Booking Service | http://localhost:8083/q/swagger-ui |
| Payment Service | http://localhost:8084/q/swagger-ui |

### Health Check

```bash
curl http://localhost:8081/q/health
curl http://localhost:8082/q/health
curl http://localhost:8083/q/health
curl http://localhost:8084/q/health
```

### Test Luồng Nghiệp vụ Cơ bản (Dev Mode)

```bash
# 1. Đăng ký tài khoản (Dev mode: không cần Keycloak thật)
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test01","email":"test01@gmail.com","password":"12345678","fullName":"Test User","phone":"0901234567"}'

# 2. Xem danh sách sân
curl http://localhost:8082/api/v1/fields

# 3. Xem danh sách địa điểm
curl http://localhost:8082/api/v1/locations

# 4. Tính giá sân (field_id=1, Thứ 3, 17:00-19:00)
curl "http://localhost:8082/api/v1/fields/1/price?date=2025-06-10&startTime=17:00&endTime=19:00"

# 5. Đặt sân (Dev mode: userId=1 mặc định)
curl -X POST http://localhost:8083/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{"fieldId":1,"bookingDate":"2025-06-10","startTime":"17:00","endTime":"19:00","note":"Nhóm 4 người"}'

# 6. Xem lịch đặt sân
curl http://localhost:8083/api/v1/bookings

# 7. Khởi tạo thanh toán
curl -X POST http://localhost:8084/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"bookingId":1,"paymentMethod":"VNPAY"}'
```

---

## Thứ Tự Khởi Động (Quan trọng)

```
MySQL → Keycloak → field-service → auth-service → booking-service → payment-service
```

Lý do: `booking-service` gọi `field-service`, `payment-service` gọi `booking-service`.

---

## Xử Lý Lỗi Thường Gặp

| Lỗi | Nguyên nhân | Giải pháp |
|-----|-------------|-----------|
| `Connection refused :3306` | MySQL chưa chạy | Khởi động MySQL service |
| `Keycloak connection failed` | Keycloak chưa sẵn sàng | Chờ ~30s sau khi start Keycloak |
| `Flyway migration failed` | Database chưa tồn tại | Tạo database trước: `CREATE DATABASE auth_db;` |
| `401 Unauthorized` | Dev mode: OIDC enabled | Thêm `%dev.quarkus.oidc.enabled=false` |
| `field-service connection refused` | field-service chưa chạy | Chạy field-service trước booking-service |
| `Client secret not found` | Chưa cấu hình Keycloak client | Làm lại Bước 2 — Cấu hình Realm |
