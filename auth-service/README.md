# Auth Service — Sportify Booking Platform

## Tổng quan

**Auth Service** là dịch vụ chịu trách nhiệm quản lý xác thực và định danh người dùng trong hệ thống Sportify. Service này **không tự quản lý mật khẩu** mà ủy quyền hoàn toàn việc xác thực định danh cho **Keycloak** (Identity Provider). Auth Service đóng vai trò cầu nối giữa ứng dụng và Keycloak, đồng thời duy trì bản sao hồ sơ người dùng trong database nội bộ của mình.

- **Framework**: Quarkus (Jakarta EE)
- **Port**: `8081`
- **Database**: MySQL — `auth_db`
- **Identity Provider**: Keycloak `24.0.3` (Realm: `sportify`)
- **Bảo mật**: OIDC / JWT (Bearer Token)
- **API Docs**: Swagger UI tại `http://localhost:8081/q/swagger-ui`

---

## Kiến trúc Nội bộ

```
AuthResource (REST Layer)      ← Nhận HTTP request, validate input
      │
AuthService (Business Logic)   ← Xử lý nghiệp vụ
      │                \
 Keycloak Admin API    Keycloak Token Endpoint (OIDC)
      │
User (JPA Entity / DB)         ← Lưu hồ sơ người dùng nội bộ
```

---

## Cấu trúc Database (`auth_db`)

### Bảng `users`

| Cột          | Kiểu           | Ràng buộc           | Mô tả                                    |
|--------------|----------------|---------------------|------------------------------------------|
| `id`         | `BIGINT`       | PK, AUTO_INCREMENT  | ID nội bộ                                |
| `username`   | `VARCHAR(50)`  | NOT NULL, UNIQUE    | Tên đăng nhập                            |
| `email`      | `VARCHAR(100)` | NOT NULL, UNIQUE    | Email người dùng                         |
| `phone`      | `VARCHAR(20)`  | NOT NULL, UNIQUE    | Số điện thoại                            |
| `full_name`  | `VARCHAR(100)` |                     | Tên đầy đủ                              |
| `keycloak_id`| `VARCHAR(100)` | UNIQUE              | ID người dùng trên Keycloak (`sub` claim)|
| `status`     | `VARCHAR(20)`  | NOT NULL, DEFAULT   | Trạng thái: `ACTIVE` / `INACTIVE`        |
| `created_at` | `DATETIME`     | NOT NULL            | Thời điểm tạo                            |

**Indexes**: `idx_users_keycloak_id`, `idx_users_email`

---

## API Endpoints

Base Path: `/api/v1/auth`

| Method | Endpoint      | Bảo mật     | Mô tả                            |
|--------|---------------|-------------|----------------------------------|
| `POST` | `/register`   | `@PermitAll`| Đăng ký tài khoản mới            |
| `POST` | `/login`      | `@PermitAll`| Đăng nhập, lấy Access/Refresh Token |
| `POST` | `/refresh`    | `@PermitAll`| Làm mới Access Token             |
| `GET`  | `/me`         | JWT Required| Lấy thông tin profile người dùng |
| `PUT`  | `/me`         | JWT Required| Cập nhật thông tin profile        |

---

## Chi Tiết Nghiệp Vụ

### 1. Đăng ký Tài khoản (`POST /register`)

**Request Body** (`RegisterRequest`):
```json
{
  "username": "nguyenvana",
  "email": "nguyenvana@gmail.com",
  "password": "matkhau123",
  "fullName": "Nguyễn Văn A",
  "phone": "0901234567"
}
```

**Validation**:
- `username`: bắt buộc, dài 3–50 ký tự
- `email`: bắt buộc, đúng định dạng email
- `password`: bắt buộc, tối thiểu 8 ký tự

**Luồng xử lý** (`AuthService.register`):

```
1. Kiểm tra trùng lặp email trong DB nội bộ
   └─ Nếu trùng → 409 CONFLICT: "Email already registered"

2. Xây dựng UserRepresentation cho Keycloak:
   - username, email, firstName = fullName
   - enabled = true, emailVerified = true
   - credential (password, non-temporary)

3. Gọi Keycloak Admin API: POST /admin/realms/sportify/users
   ├─ 409 → 409 CONFLICT: "Username already exists on Keycloak"
   ├─ 400 → 400 BAD REQUEST + chi tiết lỗi từ Keycloak
   └─ 201 → Lấy Keycloak ID từ Header "Location"

4. Lưu User vào DB nội bộ:
   - username, email, phone, fullName, keycloakId
   - status = ACTIVE
   - createdAt = now()

5. Trả về UserProfileResponse (HTTP 201)
```

**Response** (HTTP 201):
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 1,
    "username": "nguyenvana",
    "email": "nguyenvana@gmail.com",
    "fullName": "Nguyễn Văn A",
    "phone": "0901234567",
    "status": "ACTIVE"
  }
}
```

---

### 2. Đăng nhập (`POST /login`)

**Request Body** (`LoginRequest`):
```json
{
  "username": "nguyenvana",
  "password": "matkhau123"
}
```

**Luồng xử lý** (`AuthService.login`):

```
1. Xây dựng form OIDC Password Grant:
   - grant_type = password
   - client_id  = sportify-app  (Public Client)
   - username, password

2. Gọi Keycloak Token Endpoint:
   POST /realms/sportify/protocol/openid-connect/token

3. Nếu status != 200 → 400 BAD REQUEST + chi tiết lỗi Keycloak

4. Parse response → trả về TokenResponse
```

> **Lưu ý quan trọng**: Auth Service dùng **Public Client** (`sportify-app`) để lấy token cho người dùng.
> Nhưng để gọi **Keycloak Admin API** (tạo user), Service dùng **Confidential Client** (`auth-service`) với `CLIENT_CREDENTIALS` grant.

**Response** (HTTP 200):
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsIn...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "expiresIn": 300,
    "tokenType": "Bearer"
  }
}
```

---

### 3. Làm mới Token (`POST /refresh?refreshToken=...`)

**Luồng xử lý** (`AuthService.refreshToken`):

```
1. Xây dựng form với grant_type = refresh_token

2. Gọi Keycloak Token Endpoint với refresh_token cũ

3. Nếu token hết hạn / không hợp lệ → 400: "Invalid or expired refresh token"

4. Trả về TokenResponse mới (access_token mới + refresh_token mới)
```

---

### 4. Lấy Profile Người dùng (`GET /me`)

**Yêu cầu**: Header `Authorization: Bearer <access_token>`

**Luồng xử lý** (`AuthService.getProfile`):

```
1. Quarkus OIDC tự động validate JWT từ Keycloak
2. Inject JsonWebToken → lấy jwt.getSubject() (= Keycloak ID / "sub" claim)
3. Tìm User trong DB nội bộ theo keycloakId
   └─ Không tìm thấy → 404 NOT FOUND
4. Trả về UserProfileResponse
```

---

### 5. Cập nhật Profile (`PUT /me`)

**Yêu cầu**: Header `Authorization: Bearer <access_token>`

**Luồng xử lý** (`AuthService.updateProfile`):

```
1. Validate JWT → lấy keycloakId
2. Tìm User trong DB nội bộ → 404 nếu không thấy
3. Cập nhật chỉ các trường cho phép: fullName, phone
   (username và email KHÔNG được phép thay đổi qua API này)
4. Lưu và trả về UserProfileResponse mới
```

---

## Xử lý Lỗi

| Trường hợp                         | HTTP Status | Message                          |
|------------------------------------|-------------|----------------------------------|
| Email đã tồn tại trong DB          | `409`       | Email already registered         |
| Username đã tồn tại trên Keycloak  | `409`       | Username already exists          |
| Keycloak từ chối tạo user          | `400`       | Keycloak user creation failed    |
| Sai username/password khi login    | `400`       | Login failed + chi tiết Keycloak |
| Refresh token hết hạn              | `400`       | Invalid or expired refresh token |
| Không tìm thấy profile             | `404`       | User not found                   |

---

## Cấu hình (application.properties)

| Property                                     | Giá trị mặc định                    | Mô tả                               |
|----------------------------------------------|--------------------------------------|-------------------------------------|
| `quarkus.http.port`                          | `8081`                               | Port của service                    |
| `quarkus.datasource.jdbc.url`                | `jdbc:mysql://localhost:3306/auth_db`| Kết nối DB                          |
| `quarkus.oidc.auth-server-url`               | `http://localhost:8180/realms/sportify` | Keycloak OIDC URL                |
| `quarkus.keycloak.admin-client.client-id`    | `auth-service`                       | Confidential Client để gọi Admin API|
| `quarkus.keycloak.admin-client.grant-type`   | `CLIENT_CREDENTIALS`                 | Grant type cho Admin API            |
| `keycloak.public-client-id`                  | `sportify-app`                       | Public Client để lấy token user     |

**Dev Mode**: OIDC validation bị tắt (`%dev.quarkus.oidc.enabled=false`) để dễ test local.

---

## Sơ đồ Quan hệ với Keycloak

```
                    ┌──────────────────────────────────────────┐
                    │               Keycloak                   │
                    │  Realm: sportify                         │
                    │  ┌──────────────┐  ┌──────────────────┐ │
                    │  │ sportify-app │  │   auth-service   │ │
                    │  │ (Public)     │  │ (Confidential)   │ │
                    │  │ Password     │  │ Client Cred.     │ │
                    │  │ Grant        │  │ → Admin API      │ │
                    │  └──────────────┘  └──────────────────┘ │
                    └─────────────┬────────────────────────────┘
                                  │
              ┌───────────────────┴──────────────────────┐
              │              Auth Service                 │
              │  POST /login  → Token Endpoint            │
              │  POST /register → Admin API (create user) │
              │  GET /me → validate JWT sub → DB lookup   │
              └──────────────────────────────────────────┘
```

---

## Chạy Local (Dev Mode)

```bash
cd auth-service
./mvnw quarkus:dev
```

> Đảm bảo MySQL đang chạy tại `localhost:3306` với database `auth_db` và Keycloak tại `localhost:8180`.
> Ở Dev mode, OIDC validation bị tắt — các endpoint `/me` vẫn hoạt động nhưng không cần JWT thực.
