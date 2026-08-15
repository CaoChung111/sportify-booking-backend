# Field Service — Sportify Booking Platform

## Tổng quan

**Field Service** là dịch vụ quản lý toàn bộ danh mục sân thể thao trong hệ thống Sportify. Service này là **nguồn dữ liệu tham chiếu trung tâm** cung cấp thông tin về vị trí (Location), môn thể thao (Sport), loại sân (FieldType), sân cụ thể (Field) và bảng giá (Price). Các service khác (đặc biệt là **Booking Service**) gọi tới Field Service để kiểm tra trạng thái sân và tính tiền đặt sân.

- **Framework**: Quarkus (Jakarta EE)
- **Port**: `8082`
- **Database**: MySQL — `field_db`
- **Bảo mật**: OIDC / JWT — endpoint public dùng `@PermitAll`, endpoint quản lý dùng `@RolesAllowed("ADMIN")`
- **Migration**: Flyway (tự động chạy khi khởi động)
- **API Docs**: Swagger UI tại `http://localhost:8082/q/swagger-ui`

---

## Mô hình Dữ liệu (Domain Model)

Cấu trúc phân cấp của dữ liệu:

```
Sport (Môn thể thao)
  └── FieldType (Loại sân — sân 5 người, sân 7 người, ...)
         └── Field (Sân cụ thể)
                └── Thuộc về Location (Địa điểm)

Location (Địa điểm)
  └── Price (Bảng giá — theo FieldType + khung giờ + loại ngày)
```

---

## Cấu trúc Database (`field_db`)

### Bảng `sports` — Môn thể thao

| Cột    | Kiểu          | Ràng buộc        | Mô tả                         |
|--------|---------------|------------------|-------------------------------|
| `id`   | `BIGINT`      | PK               |                               |
| `name` | `VARCHAR(50)` | NOT NULL         | Tên môn (Bóng đá, Cầu lông)  |
| `slug` | `VARCHAR(50)` | NOT NULL, UNIQUE | Định danh URL-safe (`bong-da`)|

**Dữ liệu mẫu**: `Bóng đá` (bong-da), `Cầu lông` (cau-long), `Bóng rổ` (bong-ro)

---

### Bảng `locations` — Địa điểm / Cơ sở

| Cột       | Kiểu           | Ràng buộc | Mô tả                     |
|-----------|----------------|-----------|---------------------------|
| `id`      | `BIGINT`       | PK        |                           |
| `name`    | `VARCHAR(100)` | NOT NULL  | Tên cơ sở                 |
| `address` | `TEXT`         | NOT NULL  | Địa chỉ đầy đủ            |
| `region`  | `VARCHAR(50)`  | NOT NULL  | Khu vực / Tỉnh thành       |
| `hotline` | `VARCHAR(20)`  |           | Số điện thoại liên hệ     |

**Dữ liệu mẫu**: `Sportify Hà Nội`, `Sportify Bắc Ninh`

---

### Bảng `field_types` — Loại sân

| Cột               | Kiểu           | Ràng buộc | Mô tả                              |
|-------------------|----------------|-----------|-------------------------------------|
| `id`              | `BIGINT`       | PK        |                                     |
| `sport_id`        | `BIGINT`       | FK→sports | Liên kết với môn thể thao           |
| `name`            | `VARCHAR(100)` | NOT NULL  | Tên loại sân (VD: Sân 5 người)      |
| `player_capacity` | `INT`          | NOT NULL  | Số người tối đa có thể chơi         |

---

### Bảng `fields` — Sân cụ thể

| Cột            | Kiểu          | Ràng buộc         | Mô tả                                   |
|----------------|---------------|-------------------|-----------------------------------------|
| `id`           | `BIGINT`      | PK                |                                         |
| `location_id`  | `BIGINT`      | FK→locations      | Thuộc địa điểm nào                      |
| `field_type_id`| `BIGINT`      | FK→field_types    | Loại sân                                |
| `name`         | `VARCHAR(50)` | NOT NULL          | Tên sân (VD: Sân A1)                    |
| `status`       | `VARCHAR(20)` | NOT NULL, DEFAULT | Trạng thái: `AVAILABLE` / `MAINTENANCE` |

**Indexes**: `idx_fields_location`

---

### Bảng `prices` — Bảng giá

| Cột             | Kiểu            | Ràng buộc      | Mô tả                                         |
|-----------------|-----------------|----------------|-----------------------------------------------|
| `id`            | `BIGINT`        | PK             |                                               |
| `location_id`   | `BIGINT`        | FK→locations   | Áp dụng cho địa điểm nào                      |
| `field_type_id` | `BIGINT`        | FK→field_types | Áp dụng cho loại sân nào                      |
| `start_time`    | `TIME`          | NOT NULL       | Giờ bắt đầu khung giá                         |
| `end_time`      | `TIME`          | NOT NULL       | Giờ kết thúc khung giá                        |
| `price`         | `DECIMAL(15,2)` | NOT NULL       | Đơn giá mỗi giờ (VND)                         |
| `day_type`      | `VARCHAR(20)`   | NOT NULL       | Loại ngày: `WEEKDAY` / `WEEKEND` / `HOLIDAY`  |

**Indexes**: `idx_prices_location` (location_id, field_type_id)

---

## API Endpoints

Base Path: `/api/v1/fields`

| Method  | Endpoint              | Bảo mật            | Mô tả                                        |
|---------|-----------------------|--------------------|----------------------------------------------|
| `GET`   | `/`                   | `@PermitAll`       | Lấy tất cả sân (lọc theo location / sport)   |
| `GET`   | `/{id}`               | `@PermitAll`       | Lấy chi tiết một sân                         |
| `GET`   | `/{id}/availability`  | `@PermitAll`       | Kiểm tra sân có mở cửa không                 |
| `GET`   | `/{id}/price`         | `@PermitAll`       | Tính giá cho khung giờ đặt                   |
| `POST`  | `/`                   | `@RolesAllowed(ADMIN)` | Tạo sân mới (Admin)                      |
| `PUT`   | `/{id}`               | `@RolesAllowed(ADMIN)` | Cập nhật thông tin sân (Admin)           |
| `PATCH` | `/{id}/status`        | `@RolesAllowed(ADMIN)` | Thay đổi trạng thái sân (Admin)          |

---

## Chi Tiết Nghiệp Vụ

### 1. Lấy danh sách sân (`GET /`)

**Query Params**:
- `locationId` (tùy chọn): lọc theo địa điểm
- `sportId` (tùy chọn): lọc theo môn thể thao

**Luồng xử lý**:
```
1. Nếu có locationId → Truy vấn Field.findByLocation(locationId)
   Nếu không → Lấy tất cả Field.listAll()

2. Nếu có sportId → Lọc thêm các sân có fieldType.sport.id = sportId

3. Map sang FieldResponse (bao gồm tên sân, tên địa điểm, loại sân, môn thể thao)
```

**Response** (HTTP 200):
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Sân A1",
      "status": "AVAILABLE",
      "locationId": 1,
      "locationName": "Sportify Hà Nội",
      "fieldTypeId": 2,
      "fieldTypeName": "Sân 5 người",
      "sportName": "Bóng đá"
    }
  ]
}
```

---

### 2. Kiểm tra Trạng thái Sân (`GET /{id}/availability`)

**Query Params**: `date` (YYYY-MM-DD), `startTime` (HH:MM), `endTime` (HH:MM)

> Endpoint này được **Booking Service** gọi nội bộ qua RestClient trước khi tạo đơn đặt sân.

**Luồng xử lý** (`FieldService.isAvailable`):
```
1. Tìm Field theo id → 404 nếu không tồn tại

2. Kiểm tra field.status == AVAILABLE
   ├─ AVAILABLE  → trả về true (sân mở cửa)
   └─ MAINTENANCE → trả về false (sân đang bảo trì)

⚠️  Lưu ý: Endpoint này CHỈ kiểm tra trạng thái vận hành của sân.
    Việc kiểm tra xung đột lịch đặt cụ thể được thực hiện bởi Booking Service.
```

**Response**:
```json
{ "success": true, "data": true }
```

---

### 3. Tính Giá Động (`GET /{id}/price`) — Nghiệp vụ cốt lõi

**Query Params**: `date` (YYYY-MM-DD), `startTime` (HH:MM), `endTime` (HH:MM)

> Endpoint này được **Booking Service** gọi nội bộ để xác định số tiền người dùng phải trả.

**Luồng xử lý** (`FieldService.calculatePrice`) — chi tiết:

```
Bước 1: Tìm Field theo id → 404 nếu không tồn tại

Bước 2: Phân loại loại ngày (resolveDayType):
  - Nếu date là Thứ 7 hoặc Chủ nhật → DayType = WEEKEND
  - Còn lại → DayType = WEEKDAY

Bước 3: Lấy locationId và fieldTypeId từ field

Bước 4: Khớp quy tắc giá (Price Rule Matching):
  Truy vấn: location_id = ? AND field_type_id = ?
            AND start_time <= startTime AND end_time > startTime
            AND day_type = <DayType>

  Kịch bản 1: Tìm thấy Price rule cho đúng loại ngày → Dùng luôn
  Kịch bản 2: Không tìm thấy (WEEKEND không có giá riêng):
              → Fallback sang WEEKDAY: tìm lại với day_type = WEEKDAY
  Kịch bản 3: Vẫn không tìm thấy → 400 BAD REQUEST:
              "No price rule found for field X at HH:MM"

Bước 5: Tính thời lượng thuê:
  durationMinutes = endTime - startTime (phút)
  hours = durationMinutes / 60.0

Bước 6: Tính tổng tiền:
  totalPrice = priceRule.price × hours

Bước 7: Trả về PriceResponse
```

**Ví dụ thực tế**:
- Sân thuộc `Sportify Hà Nội`, loại `Sân 5 người`
- Đặt ngày Thứ 6 (WEEKDAY), từ `17:00` đến `19:00` (2 giờ)
- Giá rule khớp: `150,000 VND/giờ` (khung 16:00–22:00, WEEKDAY)
- **Tổng tiền = 150,000 × 2 = 300,000 VND**

**Response**:
```json
{
  "success": true,
  "data": {
    "fieldId": 1,
    "totalPrice": 300000.00,
    "currency": "VND"
  }
}
```

---

### 4. Tạo Sân Mới (`POST /`) — Admin Only

**Request Body** (`CreateFieldRequest`):
```json
{
  "locationId": 1,
  "fieldTypeId": 2,
  "name": "Sân B3"
}
```

**Validation**: Tất cả 3 trường là bắt buộc (`@NotNull`, `@NotBlank`).

**Luồng xử lý**:
```
1. Tìm Location theo locationId → 404 nếu không tồn tại
2. Tìm FieldType theo fieldTypeId → 404 nếu không tồn tại
3. Tạo Field mới với status = AVAILABLE (mặc định)
4. Lưu vào DB và trả về FieldResponse (HTTP 201)
```

---

### 5. Thay đổi Trạng thái Sân (`PATCH /{id}/status`) — Admin Only

**Query Param**: `status` — `AVAILABLE` hoặc `MAINTENANCE`

**Luồng xử lý**:
```
1. Tìm Field theo id → 404 nếu không tồn tại
2. Đổi status = Status.valueOf(status.toUpperCase())
   (Sẽ ném IllegalArgumentException nếu giá trị không hợp lệ)
3. Lưu thay đổi

Nghiệp vụ:
  AVAILABLE   → Sân mở cửa, sẵn sàng nhận đặt lịch
  MAINTENANCE → Sân đóng cửa bảo trì, Booking Service sẽ từ chối đặt
```

---

## Xử lý Lỗi

| Trường hợp                          | HTTP Status | Message                              |
|-------------------------------------|-------------|--------------------------------------|
| Không tìm thấy Field                | `404`       | Field not found: {id}                |
| Không tìm thấy Location             | `404`       | Location not found: {id}             |
| Không tìm thấy FieldType            | `404`       | FieldType not found: {id}            |
| Không có Price Rule phù hợp         | `400`       | No price rule found for field X at T |
| Tạo sân thiếu trường bắt buộc       | `400`       | Validation error                     |

---

## Cấu hình (application.properties)

| Property                        | Giá trị mặc định                     | Mô tả                       |
|---------------------------------|--------------------------------------|-----------------------------|
| `quarkus.http.port`             | `8082`                               | Port của service             |
| `quarkus.datasource.jdbc.url`   | `jdbc:mysql://localhost:3306/field_db`| Kết nối DB                  |
| `quarkus.oidc.auth-server-url`  | `http://localhost:8180/realms/sportify`| OIDC validate JWT           |
| `quarkus.flyway.migrate-at-start`| `true`                              | Tự chạy migration khi start |

**Dev Mode**: OIDC validation bị tắt, DB được `drop-and-create` khi start.

---

## Quan hệ với Các Service Khác

```
┌─────────────────────────────────────────────────────────────┐
│                       Field Service                         │
│                                                             │
│  GET /api/v1/fields/{id}                                    │
│  GET /api/v1/fields/{id}/availability?date=&startTime=&endTime= │
│  GET /api/v1/fields/{id}/price?date=&startTime=&endTime=    │
│               ▲          ▲             ▲                    │
└───────────────┼──────────┼─────────────┼────────────────────┘
                │          │             │
         ┌──────┴──────────┴─────────────┴──────────┐
         │           Booking Service                 │
         │  FieldServiceClient (MicroProfile REST)   │
         │  Gọi để:                                  │
         │   1. Xác thực sân tồn tại + lấy tên sân  │
         │   2. Kiểm tra trạng thái AVAILABLE        │
         │   3. Tính giá totalPrice cho booking      │
         └───────────────────────────────────────────┘
```

---

## Thiết kế Bảng Giá — Ví dụ Thực tế

Ví dụ cấu hình bảng giá cho `Sportify Hà Nội` - `Sân 5 người Bóng đá`:

| Khung giờ     | Loại ngày | Giá/giờ        |
|---------------|-----------|----------------|
| 06:00 – 11:00 | WEEKDAY   | 100,000 VND    |
| 11:00 – 16:00 | WEEKDAY   | 80,000 VND     |
| 16:00 – 22:00 | WEEKDAY   | 150,000 VND    |
| 06:00 – 22:00 | WEEKEND   | 180,000 VND    |

- Đặt Thứ 7 từ 8:00–10:00 → Khớp WEEKEND rule → 180,000 × 2 = **360,000 VND**
- Đặt Thứ 2 từ 17:00–19:00 → Khớp WEEKDAY 16:00–22:00 → 150,000 × 2 = **300,000 VND**
- Đặt Chủ nhật, không có WEEKEND rule → Fallback WEEKDAY → Dùng giá ngày thường

---

## Chạy Local (Dev Mode)

```bash
cd field-service
./mvnw quarkus:dev
```

> Đảm bảo MySQL đang chạy tại `localhost:3306` với database `field_db`.
> Flyway sẽ tự động tạo schema và seed dữ liệu mẫu (sports, locations) từ `V1__init_field.sql`.
