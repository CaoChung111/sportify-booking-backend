# 📚 TÀI LIỆU API CHO FRONTEND (API SPECIFICATION)

Tài liệu này tổng hợp toàn bộ các endpoint được expose qua API Gateway (chạy tại cổng `8090`), kèm theo cấu trúc Request body (nếu có) và Response.
- **Base URL:** `http://localhost:8090`
- **Định dạng trả về chung (`ApiResponse<T>`):**

```json
{
  "success": true,
  "message": "Thông báo (có thể null)",
  "data": { ... } // Payload dữ liệu kiểu T
}
```

---

## 1. 🔐 AUTH SERVICE (`/api/v1/auth`)

### Endpoints
- `POST /api/v1/auth/register`: Đăng ký tài khoản mới.
  - Body: `RegisterRequest`
  - Data Response: `UserProfileResponse`
- `POST /api/v1/auth/login`: Đăng nhập lấy token.
  - Body: `LoginRequest`
  - Data Response: `TokenResponse`
- `POST /api/v1/auth/refresh`: Làm mới token.
  - Body: `RefreshTokenRequest`
  - Data Response: `TokenResponse`
- `GET /api/v1/auth/me`: Lấy thông tin user hiện tại (Yêu cầu Header `Authorization: Bearer <token>`).
  - Data Response: `UserProfileResponse`
- `PUT /api/v1/auth/me`: Cập nhật thông tin user (Yêu cầu Token).
  - Body: `UpdateProfileRequest`
  - Data Response: `UserProfileResponse`

### Cấu trúc DTO (Data Transfer Objects)

**RegisterRequest:**
```json
{
  "username": "String (3-50 chars)",
  "email": "String (valid email format)",
  "password": "String (min 8 chars)",
  "fullName": "String",
  "phone": "String (Vietnamese format: +84... hoặc 0...)"
}
```

**LoginRequest:**
```json
{
  "username": "String",
  "password": "String"
}
```

**RefreshTokenRequest:**
```json
{
  "refreshToken": "String"
}
```

**UpdateProfileRequest:**
```json
{
  "fullName": "String (max 100 chars)",
  "phone": "String (Vietnamese format)"
}
```

**TokenResponse:**
```json
{
  "accessToken": "String",
  "refreshToken": "String",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

**UserProfileResponse:**
```json
{
  "id": 1,
  "username": "String",
  "email": "String",
  "fullName": "String",
  "phone": "String",
  "status": "String",
  "createdAt": "String (ISO-8601)"
}
```

---

## 2. 🏟️ FIELD SERVICE

### Endpoints

**Địa điểm (Locations) - `/api/v1/locations`:**
- `GET /api/v1/locations`: Lấy danh sách địa điểm. Data Response: `List<LocationResponse>`
- `GET /api/v1/locations/{id}`: Chi tiết địa điểm. Data Response: `LocationResponse`
- `POST /api/v1/locations`: Tạo địa điểm (Admin). Body: `CreateLocationRequest` | Data Response: `LocationResponse`
- `PUT /api/v1/locations/{id}`: Sửa địa điểm (Admin). Body: `CreateLocationRequest` | Data Response: `LocationResponse`
- `DELETE /api/v1/locations/{id}`: Xoá địa điểm (Admin). Data Response: `null`

**Môn thể thao (Sports) - `/api/v1/sports`:**
- `GET /api/v1/sports`: Lấy ds môn. Data Response: `List<SportResponse>`
- `GET /api/v1/sports/{id}`: Chi tiết môn. Data Response: `SportResponse`
- `GET /api/v1/sports/slug/{slug}`: Chi tiết qua slug. Data Response: `SportResponse`
- `POST /api/v1/sports`: Tạo môn (Admin). Body: `CreateSportRequest` | Data Response: `SportResponse`
- `PUT /api/v1/sports/{id}`: Sửa môn (Admin). Body: `CreateSportRequest` | Data Response: `SportResponse`
- `DELETE /api/v1/sports/{id}`: Xoá môn (Admin). Data Response: `null`

**Loại sân (FieldTypes) - `/api/v1/field-types`:**
- `GET /api/v1/field-types?sportId=`: Lấy ds loại sân (tuỳ chọn lọc theo môn). Data Response: `List<FieldTypeResponse>`
- `GET /api/v1/field-types/{id}`: Chi tiết loại sân. Data Response: `FieldTypeResponse`
- `POST /api/v1/field-types`: Tạo loại sân (Admin). Body: `CreateFieldTypeRequest` | Data Response: `FieldTypeResponse`
- `PUT /api/v1/field-types/{id}`: Sửa loại sân (Admin). Body: `CreateFieldTypeRequest` | Data Response: `FieldTypeResponse`
- `DELETE /api/v1/field-types/{id}`: Xoá loại sân (Admin). Data Response: `null`

**Sân thể thao (Fields) - `/api/v1/fields`:**
- `GET /api/v1/fields?name=&locationId=&sportId=`: Lấy ds sân. Data Response: `List<FieldResponse>`
- `GET /api/v1/fields/{id}`: Chi tiết sân. Data Response: `FieldResponse`
- `GET /api/v1/fields/{id}/availability`: Check trạng thái khả dụng. Data Response: `Boolean`
- `GET /api/v1/fields/{id}/price?date=&startTime=&endTime=`: Tính giá (Dynamic Pricing) theo khung giờ. Data Response: `PriceResponse`
- `POST /api/v1/fields`: Tạo sân (Admin). Body: `CreateFieldRequest` | Data Response: `FieldResponse`
- `PUT /api/v1/fields/{id}`: Cập nhật sân (Admin). Body: `CreateFieldRequest` | Data Response: `FieldResponse`
- `PATCH /api/v1/fields/{id}/status?status=`: Đổi trạng thái (`AVAILABLE` hoặc `MAINTENANCE`) (Admin). Data Response: `null`
- `DELETE /api/v1/fields/{id}`: Xóa sân (Admin) — yêu cầu sân phải ở trạng thái `MAINTENANCE` trước khi xóa. Data Response: `null`

**Bảng giá (Prices) - `/api/v1/prices`:**
- `GET /api/v1/prices?locationId=&fieldTypeId=`: Lấy bảng giá (Admin). Data Response: `List<PriceRuleResponse>`
- `GET /api/v1/prices/{id}`: Chi tiết giá (Admin). Data Response: `PriceRuleResponse`
- `POST /api/v1/prices`: Tạo quy tắc giá (Admin). Body: `CreatePriceRequest` | Data Response: `PriceRuleResponse`
- `PUT /api/v1/prices/{id}`: Sửa quy tắc giá (Admin). Body: `CreatePriceRequest` | Data Response: `PriceRuleResponse`
- `DELETE /api/v1/prices/{id}`: Xoá quy tắc giá (Admin). Data Response: `null`

### Cấu trúc DTO

**CreateLocationRequest:**
```json
{
  "name": "String",
  "address": "String",
  "region": "String",
  "hotline": "String"
}
```

**LocationResponse:**
```json
{
  "id": 1,
  "name": "String",
  "address": "String",
  "region": "String",
  "hotline": "String",
  "totalFields": 5
}
```

**CreateSportRequest:**
```json
{
  "name": "String",
  "slug": "String (a-z, 0-9, -)"
}
```

**SportResponse:**
```json
{
  "id": 1,
  "name": "String",
  "slug": "String"
}
```

**CreateFieldTypeRequest:**
```json
{
  "sportId": 1,
  "name": "String",
  "playerCapacity": 10
}
```

**FieldTypeResponse:**
```json
{
  "id": 1,
  "name": "String",
  "playerCapacity": 10,
  "sportId": 1,
  "sportName": "String",
  "sportSlug": "String"
}
```

**CreateFieldRequest:**
```json
{
  "locationId": 1,
  "fieldTypeId": 1,
  "name": "String"
}
```

**FieldResponse:**
```json
{
  "id": 1,
  "name": "String",
  "status": "String",
  "locationId": 1,
  "locationName": "String",
  "locationAddress": "String",
  "locationRegion": "String",
  "locationHotline": "String",
  "fieldTypeId": 1,
  "fieldTypeName": "String",
  "playerCapacity": 10,
  "sportId": 1,
  "sportName": "String",
  "sportSlug": "String"
}
```

**PriceResponse (Khi Frontend gọi endpoint tính giá trước khi đặt):**
```json
{
  "fieldId": 1,
  "fieldName": "String",
  "totalPrice": 150000.00,
  "pricePerHour": 100000.00,
  "durationHours": 1.5,
  "currency": "VND",
  "dayType": "WEEKDAY"
}
```

**CreatePriceRequest:**
```json
{
  "locationId": 1,
  "fieldTypeId": 1,
  "startTime": "06:00",
  "endTime": "11:00",
  "price": 100000.00,
  "dayType": "WEEKDAY" // WEEKDAY, WEEKEND, HOLIDAY
}
```

**PriceRuleResponse:**
```json
{
  "id": 1,
  "locationId": 1,
  "locationName": "String",
  "fieldTypeId": 1,
  "fieldTypeName": "String",
  "startTime": "06:00:00",
  "endTime": "11:00:00",
  "price": 100000.00,
  "currency": "VND",
  "dayType": "WEEKDAY"
}
```

---

## 3. 📅 BOOKING SERVICE (`/api/v1/bookings`)
Yêu cầu Header `Authorization: Bearer <token>` cho mọi endpoint phía dưới (User đang đăng nhập).

### Endpoints
- `POST /api/v1/bookings`: Tạo đơn đặt sân.
  - Body: `CreateBookingRequest`
  - Data Response: `BookingResponse`
- `GET /api/v1/bookings`: Lấy danh sách đặt sân của user hiện tại.
  - Data Response: `List<BookingResponse>`
- `GET /api/v1/bookings/{id}`: Lấy chi tiết đơn đặt sân.
  - Data Response: `BookingResponse`
- `PATCH /api/v1/bookings/{id}/cancel`: Huỷ đặt sân (chỉ huỷ được nếu trạng thái `PENDING`).
  - Data Response: `null`

*(Các endpoint internal `/confirm`, `/complete` dùng nội bộ)*

### Cấu trúc DTO

**CreateBookingRequest:**
```json
{
  "fieldId": 1,
  "bookingDate": "2024-05-20",
  "startTime": "18:00",
  "endTime": "19:30",
  "note": "String (Tuỳ chọn)"
}
```

**BookingResponse:**
```json
{
  "id": 1,
  "userId": 1,
  "fieldId": 1,
  "fieldName": "String",
  "locationName": "String",
  "bookingDate": "2024-05-20",
  "startTime": "18:00:00",
  "endTime": "19:30:00",
  "durationHours": 1.5,
  "totalPrice": 150000.00,
  "status": "PENDING", // PENDING, CONFIRMED, COMPLETED, CANCELLED
  "note": "String",
  "createdAt": "String",
  "updatedAt": "String"
}
```

---

## 4. 💳 PAYMENT SERVICE (`/api/v1/payments`)
Yêu cầu Header `Authorization: Bearer <token>` cho các endpoint (ngoại trừ webhook/callback).

### Endpoints
- `POST /api/v1/payments`: Khởi tạo thanh toán cho một booking đang `PENDING`.
  - Body: `CreatePaymentRequest`
  - Data Response: `PaymentResponse`
- `GET /api/v1/payments`: Lấy lịch sử thanh toán của user đang đăng nhập.
  - Data Response: `List<PaymentResponse>`
- `GET /api/v1/payments/{id}`: Chi tiết một giao dịch thanh toán.
  - Data Response: `PaymentResponse`
- `GET /api/v1/payments/booking/{bookingId}`: Lấy thanh toán dựa trên booking ID.
  - Data Response: `PaymentResponse`
- `PATCH /api/v1/payments/{id}/confirm-cash`: Xác nhận trả tiền mặt (Dành cho Admin).
  - Data Response: `PaymentResponse`

*(Các callback `/vnpay/callback`, `/momo/callback` Frontend không cần gọi trực tiếp)*

### Cấu trúc DTO

**CreatePaymentRequest:**
```json
{
  "bookingId": 1,
  "paymentMethod": "CASH" // Hoặc VNPAY, MOMO
}
```

**PaymentResponse:**
```json
{
  "id": 1,
  "bookingId": 1,
  "userId": 1,
  "amount": 150000.00,
  "paymentMethod": "CASH", // CASH, VNPAY, MOMO
  "paymentStatus": "PENDING", // PENDING, SUCCESS, FAILED
  "txnRef": "String",
  "paymentUrl": "http://sandbox.vnpayment.vn/...", // URL để redirect user đi thanh toán (chỉ có khi dùng VNPAY/MOMO)
  "createdAt": "String",
  "updatedAt": "String"
}
```
