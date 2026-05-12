# Tài liệu Chi tiết Nghiệp vụ Hệ thống Sportify Booking (Backend)

Hệ thống **Sportify Backend** được thiết kế theo kiến trúc **Microservices**, cung cấp nền tảng đặt sân thể thao thời gian thực. Hệ thống bao gồm các dịch vụ độc lập giao tiếp với nhau để đảm bảo tính mở rộng và toàn vẹn dữ liệu.

## Các Dịch Vụ Cốt Lõi (Core Services)

1. **Auth Service**: Quản lý xác thực và người dùng, tích hợp Keycloak.
2. **Field Service**: Quản lý danh mục sân, vị trí, môn thể thao và tính toán giá động (Dynamic Pricing).
3. **Booking Service**: Dịch vụ cốt lõi xử lý đặt sân, kiểm tra xung đột lịch (Real-time).
4. **Payment Service**: Tích hợp các cổng thanh toán bên thứ ba (VNPAY, MoMo).
5. **API Gateway**: Cổng điều hướng luồng giao tiếp mạng và bảo vệ các service bên trong.

---

## Chi Tiết Luồng Nghiệp Vụ Từng Service

### 1. Auth Service (Dịch vụ Xác thực)
Dịch vụ này không tự mã hóa và lưu trữ mật khẩu mà ủy quyền (delegate) việc quản lý định danh cho **Keycloak**.
- **Luồng Đăng ký (Register)**:
  - Hệ thống tiếp nhận thông tin người dùng và gửi request tạo User trên Keycloak.
  - Khi Keycloak tạo thành công (HTTP 201), hệ thống lấy `Keycloak ID` từ Header `Location`.
  - Lưu hồ sơ người dùng (Profile) vào Database local của Auth Service kèm theo `Keycloak ID` để đồng bộ.
- **Luồng Đăng nhập (Login)**:
  - Khách hàng cung cấp username/password.
  - Auth Service chuyển tiếp thông tin tới Keycloak qua luồng **OIDC Password Grant**.
  - Keycloak trả về **Access Token** (JWT) và **Refresh Token**.
  - Các Service khác trong hệ thống sẽ tự động parse và xác thực JWT này.

### 2. Field Service (Dịch vụ Quản lý Sân bãi)
Quản lý dữ liệu tham chiếu về cấu trúc sân (Vị trí -> Loại sân -> Sân).
- **Quản lý Hoạt động Sân**: Cho phép Admin thêm/sửa/đổi trạng thái sân (Ví dụ: Đóng cửa bảo trì - Chuyển sang KHÔNG AVAILABLE).
- **Kiểm tra trạng thái (IsAvailable)**: Cung cấp API nội bộ cho phép Booking Service kiểm tra xem một sân có đang được mở cửa cho thuê hay không.
- **Nghiệp vụ Tính Giá Động (Dynamic Pricing)**:
  - **Phân loại Ngày**: Hệ thống tự động nhận diện ngày đặt là Ngày thường (Weekday) hay Cuối tuần (Weekend).
  - **Khớp Quy Tắc Giá (Price Rule)**: Dựa vào Loại sân, Vị trí và Khung giờ bắt đầu (StartTime), hệ thống tìm ra đơn giá theo giờ. Nếu cuối tuần không có quy tắc giá riêng, sẽ tự động dùng giá ngày thường (Fallback).
  - **Tính Tổng Tiền**: Lấy đơn giá nhân với khoảng thời gian thuê (quy ra giờ).

### 3. Booking Service (Dịch vụ Đặt sân - Real-time Core)
Xử lý logic cốt lõi nhất: **Chống đặt trùng lịch (Double Booking) trong thời gian thực**.
- **Luồng Tạo Đơn Đặt Sân (Create Booking)**:
  1. **Xác thực sân**: Dùng RestClient gọi nội bộ sang `Field Service` để kiểm tra sân có tồn tại và đang mở cửa (`AVAILABLE`) hay không.
  2. **Kiểm tra Xung đột Lịch (Real-time Conflict Resolution)**: Truy vấn trực tiếp vào Database của Booking Service để đảm bảo không có bất kỳ đơn đặt sân nào khác (`Booking.hasConflict`) đang chiếm dụng khung giờ yêu cầu của sân này.
  3. **Tính Tiền**: Gọi `Field Service` để lấy giá chính xác cho khung giờ này.
  4. **Ghi nhận Đơn**: Tạo đơn đặt sân với trạng thái `PENDING` (Chờ thanh toán) và khóa (lock) tạm thời slot đó đối với những người khác.
- **Hủy Đơn (Cancel)**: Khách hàng chỉ được phép hủy đơn khi trạng thái đang là `PENDING`. Nếu đã thanh toán (`CONFIRMED`), thao tác hủy sẽ bị chặn (yêu cầu nghiệp vụ hoàn tiền phức tạp hơn).
- **Xác nhận Đơn (Confirm)**: Đây là API nội bộ được bảo vệ, **chỉ** `Payment Service` mới được phép gọi sau khi đã xác minh thanh toán thành công để đổi trạng thái đơn sang `CONFIRMED`.

### 4. Payment Service (Dịch vụ Thanh toán)
Chịu trách nhiệm tương tác với bên ngoài (Third-party Payment Gateways) và đảm bảo tính toàn vẹn giao dịch tài chính.
- **Luồng Khởi tạo Thanh toán (Initiate Payment)**:
  1. Xác minh qua `Booking Service` rằng đơn đặt sân có tồn tại và đang ở trạng thái `PENDING`.
  2. **Chống thanh toán đúp**: Kiểm tra xem đơn đặt sân này đã từng được thanh toán thành công (`SUCCESS`) hay chưa.
  3. Sinh mã Giao dịch nội bộ (TxnRef) và tạo bản ghi Payment (trạng thái `PENDING`).
  4. **Tạo Payment URL**: Với VNPAY, hệ thống đóng gói các tham số (Số tiền, TxnRef, v.v.), mã hóa bảo mật toàn bộ dữ liệu này bằng thuật toán **HMAC SHA512** và Secret Key để tạo URL chuyển hướng khách hàng sang trang của VNPAY.
- **Luồng Nhận Kết quả (Webhook/Callback)**:
  - Khi khách hàng thanh toán xong, VNPAY/MoMo sẽ tự động gọi lại API callback của Payment Service.
  - **Bảo mật**: Payment Service tính toán lại chữ ký điện tử (Hash) để đối chiếu với chữ ký VNPAY gửi sang, nhằm chống giả mạo thông tin.
  - **Cập nhật Giao dịch**:
    - Nếu thành công (Code = `00`): Cập nhật Payment thành `SUCCESS` -> Gọi ngay API nội bộ của `Booking Service` để xác nhận đơn đặt sân (Đổi Booking sang `CONFIRMED`).
    - Nếu thất bại: Cập nhật Payment thành `FAILED`. Khách hàng có thể thử thanh toán lại.
