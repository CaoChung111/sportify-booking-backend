# Sportify Booking Service

**Booking Service** là vi dịch vụ chịu trách nhiệm quản lý toàn bộ vòng đời đặt sân thể thao trong hệ thống nền tảng Sportify. Dịch vụ này đảm bảo tính toàn vẹn của dữ liệu đặt sân, ngăn chặn xung đột lịch (double-booking) và giao tiếp chặt chẽ với các dịch vụ khác (Field Service, Payment Service) để hoàn thành nghiệp vụ.

Được xây dựng trên nền tảng **Quarkus**, dịch vụ sử dụng **Hibernate ORM with Panache** cho Database Access và **MicroProfile REST Client** để gọi các dịch vụ khác với các cơ chế **Fault Tolerance** (Retry, Timeout, CircuitBreaker).

---

## 🚀 Tính Năng Chính (Features)

- **Quản lý Đặt Sân**: Cho phép người dùng tạo, xem danh sách và xem chi tiết các đơn đặt sân của mình.
- **Phòng Tránh Xung Đột Lịch (Double Booking Prevention)**: Thuật toán kiểm tra xung đột thời gian dựa trên `fieldId`, `bookingDate`, `startTime`, `endTime` ngay trên database nội bộ và cả hệ thống Field Service.
- **Giá Cả Động (Dynamic Pricing)**: Tích hợp với Field Service để tính toán giá trị đơn đặt sân theo khung giờ (ngày thường, cuối tuần, giờ vàng).
- **Quản Lý Trạng Thái Vòng Đời**: Theo dõi trạng thái Booking từ `PENDING` (chờ thanh toán), `CONFIRMED` (đã thanh toán thành công), `COMPLETED` (đã sử dụng), cho đến `CANCELLED` (đã huỷ).
- **Bảo Mật Nội Bộ**: Phân tách các API dành cho User (phải có JWT với userId hợp lệ) và các API nội bộ (chỉ dành cho Payment Service hoặc Admin gọi qua mạng nội bộ/mTLS).

---

## 🔄 Luồng Xử Lý Nghiệp Vụ (Workflows)

### 1. Luồng Tạo Đơn Đặt Sân (Real-time Booking Flow)
Khi người dùng thực hiện một request đặt sân:
1. **Validate**: Kiểm tra tính hợp lệ của thời gian (`endTime` phải sau `startTime`, có `bookingDate`).
2. **Fetch Field Data**: Sử dụng `FieldServiceClient` gọi sang **Field Service** để lấy thông tin chi tiết sân (Tên sân, địa điểm) làm *snapshot* (bản chụp dữ liệu) lưu vào hoá đơn.
3. **Check Availability**: 
   - Kiểm tra xem sân có đang ở trạng thái `AVAILABLE` hay không (chống đặt sân khi đang bảo trì).
   - Gọi API `/availability` của Field Service để xác minh kép.
4. **Conflict Check**: Tra cứu trong DB của Booking Service xem trong khung giờ đó đã có ai đặt sân thành công (hoặc đang giữ chỗ) hay chưa. Nếu có $\rightarrow$ Trả về `409 Conflict`.
5. **Calculate Price**: Gọi API `/price` của Field Service để lấy giá cho đúng khung giờ này.
6. **Persist**: Tạo `Booking` với trạng thái `PENDING` và lưu vào Database.

### 2. Luồng Thanh Toán (Payment & Confirmation Flow)
- Sau khi Booking được tạo (trạng thái `PENDING`), User sẽ tiến hành thanh toán qua **Payment Service**.
- Khi Payment Service ghi nhận thanh toán thành công từ cổng thanh toán (VNPay), nó sẽ gọi API nội bộ `PATCH /api/v1/bookings/{id}/confirm` của Booking Service.
- Booking Service cập nhật trạng thái đơn thành `CONFIRMED`. Lịch đặt chính thức được chốt.

### 3. Luồng Huỷ Đơn (Cancellation Flow)
- Người dùng chỉ có thể gọi `PATCH /api/v1/bookings/{id}/cancel` khi đơn đang ở trạng thái `PENDING`.
- Nếu đơn đã thanh toán (`CONFIRMED`), không thể huỷ qua endpoint này để tránh các vấn đề liên đới về hoàn tiền (Refund). 

---

## 🔌 Giao Tiếp Liên Dịch Vụ (Inter-Service Communication)

Booking Service không hoạt động độc lập mà giao tiếp chặt chẽ với:

### 1. Phụ thuộc vào Field Service (Downstream)
Sử dụng **MicroProfile REST Client** (`FieldServiceClient`) để lấy thông tin Sân, check lịch trống và tính giá.
- Tích hợp **Fault Tolerance**:
  - `@Retry(maxRetries = 3)`: Tự động gọi lại nếu mạng gặp lỗi chập chờn.
  - `@Timeout(2000)`: Ngắt request nếu Field Service phản hồi quá 2 giây (tránh treo thread).
  - `@CircuitBreaker`: Tự động ngắt mạch (mở circuit) nếu Field Service bị lỗi liên tục, giúp bảo vệ toàn bộ hệ thống khỏi hiệu ứng Domino (Cascade Failure).

### 2. Cung cấp API cho Payment Service (Upstream)
- `GET /api/v1/bookings/{id}/internal`: Cho phép Payment Service truy xuất thông tin đơn giá, thông tin user mà không cần vượt qua bộ lọc quyền sở hữu user.
- `PATCH /api/v1/bookings/{id}/confirm`: Được Payment Service gọi để đổi trạng thái đơn hàng sau khi thanh toán thành công.

---

## 🌐 API Endpoints

### User APIs (Yêu cầu JWT)
| Method | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `POST` | `/api/v1/bookings` | Tạo một đơn đặt sân mới |
| `GET` | `/api/v1/bookings` | Lấy danh sách tất cả các đặt sân của user hiện tại |
| `GET` | `/api/v1/bookings/{id}` | Lấy chi tiết một đơn đặt sân (chỉ chủ đơn mới xem được) |
| `PATCH`| `/api/v1/bookings/{id}/cancel` | Huỷ đơn đặt sân (chỉ khi đang ở trạng thái `PENDING`) |

### Internal / Admin APIs (Permit All / Internal Network)
| Method | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/api/v1/bookings/{id}/internal` | Lấy chi tiết booking bỏ qua kiểm tra quyền (dành cho Payment Service) |
| `PATCH`| `/api/v1/bookings/{id}/confirm` | Xác nhận đơn đặt sân sau khi thanh toán xong (Payment Service gọi) |
| `PATCH`| `/api/v1/bookings/{id}/complete`| Đánh dấu đã sử dụng xong sân (Admin/Cron job gọi) |

---

## 🛠 Cấu Trúc Mã Nguồn

```text
src/main/java/com/sportify/booking/
├── client/          # REST Clients gọi external microservices (FieldServiceClient)
├── dto/             # Data Transfer Objects cho Request/Response
├── entity/          # Các JPA Entity (Booking) tích hợp Panache
├── resource/        # JAX-RS REST Controllers (BookingResource)
└── service/         # Nơi chứa toàn bộ Business Logic nghiệp vụ (BookingService)
```
