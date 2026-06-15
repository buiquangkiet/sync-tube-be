# Hướng dẫn lệnh Spring Boot — Synctube

Tài liệu này tổng hợp các lệnh thường dùng khi phát triển project **Synctube** (Spring Boot 4.0.6 + Maven + MySQL).

---

## 1. Yêu cầu trước khi chạy

- **Java 26** (theo `pom.xml`)
- **MySQL** đang chạy
- Database **`synctube`** đã tạo trong Navicat
- Cấu hình trong `src/main/resources/application.yaml` khớp với Navicat:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/synctube
    username: root
    password: 123456
```

> **Lưu ý:** Tên connection Navicat (ví dụ `DB-DEV`) **không phải** tên database. Tên database phải là `synctube`.

---

## 2. Maven Wrapper — lệnh cơ bản

Mở terminal tại thư mục project `synctube`.

### Windows (PowerShell / CMD)

```powershell
# Chạy ứng dụng
.\mvnw.cmd spring-boot:run

# Compile code
.\mvnw.cmd compile

# Chạy test
.\mvnw.cmd test

# Đóng gói file .jar
.\mvnw.cmd package

# Bỏ qua test khi build
.\mvnw.cmd package -DskipTests

# Dọn build cũ rồi build lại
.\mvnw.cmd clean package

# Cài dependency (tải thư viện)
.\mvnw.cmd dependency:resolve
```

### Linux / macOS

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

---

## 3. Chạy ứng dụng

### Cách 1: Maven (khuyên dùng khi dev)

```powershell
cd c:\Users\buiqu\Desktop\synctube
.\mvnw.cmd spring-boot:run
```

### Cách 2: Chạy file JAR sau khi build

```powershell
.\mvnw.cmd package -DskipTests
java -jar target\synctube-0.0.1-SNAPSHOT.jar
```

### Cách 3: Chạy từ IDE (IntelliJ IDEA)

1. Mở file `SynctubeApplication.java`
2. Chuột phải → **Run 'SynctubeApplication'**

### Dừng ứng dụng

- Terminal: `Ctrl + C`
- IDE: nút **Stop** (hình vuông đỏ)

---

## 4. Kiểm tra app đã chạy chưa

Mặc định Spring Boot chạy tại:

```
http://localhost:8080
```

Thử gọi API:

```powershell
curl http://localhost:8080/api/users
```

Nếu trả về `[]` (mảng rỗng) → app chạy OK, chưa có user nào.

---

## 5. Các lệnh Maven thường gặp

| Lệnh | Mô tả |
|------|--------|
| `.\mvnw.cmd spring-boot:run` | Chạy app ở chế độ dev |
| `.\mvnw.cmd compile` | Biên dịch source code |
| `.\mvnw.cmd test` | Chạy unit test |
| `.\mvnw.cmd clean` | Xóa thư mục `target/` |
| `.\mvnw.cmd package` | Build ra file `.jar` |
| `.\mvnw.cmd clean install` | Clean + build + cài vào local repo |
| `.\mvnw.cmd dependency:tree` | Xem cây dependency |

### Chạy một test class cụ thể

```powershell
.\mvnw.cmd test -Dtest=SynctubeApplicationTests
```

### Chạy app với profile khác (nếu có)

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 6. Cấu hình Spring Boot (`application.yaml`)

File: `src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: synctube
  datasource:
    url: jdbc:mysql://localhost:3306/synctube
    username: root
    password: 123456
  jpa:
    hibernate:
      ddl-auto: update    # tự tạo/cập nhật bảng khi chạy app
    show-sql: true        # in SQL ra console
    properties:
      hibernate:
        format_sql: true  # format SQL cho dễ đọc
```

### Giá trị `ddl-auto` thường dùng

| Giá trị | Ý nghĩa |
|--------|---------|
| `update` | Tự tạo/sửa bảng theo entity (dev) |
| `create` | Xóa và tạo lại bảng mỗi lần chạy |
| `create-drop` | Tạo khi start, xóa khi stop |
| `validate` | Chỉ kiểm tra schema, không sửa |
| `none` | Không làm gì với schema |

### Đổi port server

Thêm vào `application.yaml`:

```yaml
server:
  port: 9090
```

App sẽ chạy tại `http://localhost:9090`.

---

## 7. Gọi API bằng curl (PowerShell)

### User

**Tạo user**

```powershell
curl -Method POST http://localhost:8080/api/users `
  -ContentType "application/json" `
  -Body '{"username":"john","email":"john@example.com","password":"secret","displayName":"John"}'
```

**Lấy danh sách user**

```powershell
curl http://localhost:8080/api/users
```

**Lấy user theo id**

```powershell
curl http://localhost:8080/api/users/1
```

**Cập nhật user**

```powershell
curl -Method PUT http://localhost:8080/api/users/1 `
  -ContentType "application/json" `
  -Body '{"displayName":"John Doe"}'
```

**Xóa user**

```powershell
curl -Method DELETE http://localhost:8080/api/users/1
```

### Room

**Tạo phòng** (cần có `hostId` — id user vừa tạo)

```powershell
curl -Method POST http://localhost:8080/api/rooms `
  -ContentType "application/json" `
  -Body '{"name":"Watch Party","hostId":1,"videoUrl":"https://youtube.com/watch?v=abc"}'
```

**Lấy phòng theo mã join**

```powershell
curl http://localhost:8080/api/rooms/code/ABC123
```

**Cập nhật trạng thái phát video**

```powershell
curl -Method PUT http://localhost:8080/api/rooms/1 `
  -ContentType "application/json" `
  -Body '{"playing":true,"currentTimeSeconds":30.5}'
```

---

## 8. Cấu trúc project

```
synctube/
├── src/main/java/com/example/synctube/
│   ├── SynctubeApplication.java    # Entry point
│   ├── controller/                 # REST API
│   ├── service/                    # Logic nghiệp vụ
│   ├── repository/                 # Truy vấn database
│   ├── entity/                     # Bảng DB (User, Room)
│   ├── dto/                        # Request / Response
│   └── exception/                  # Xử lý lỗi
├── src/main/resources/
│   └── application.yaml            # Cấu hình app
├── src/test/                       # Test
└── pom.xml                         # Dependency Maven
```

### Luồng xử lý request

```
Client → Controller → Service → Repository → MySQL
```

---

## 9. Lỗi thường gặp & cách xử lý

### `Unable to determine Dialect without JDBC metadata`

**Nguyên nhân:** Không kết nối được MySQL (sai host, port, password, hoặc DB chưa tạo).

**Cách fix:**
1. Test connection trong Navicat thành công trước
2. Kiểm tra `application.yaml` trỏ đúng database `synctube`
3. Đảm bảo MySQL service đang chạy

### `1045 - Access denied for user 'root'@'localhost'`

**Nguyên nhân:** Sai mật khẩu MySQL.

**Cách fix:** Sửa `password` trong `application.yaml` cho khớp Navicat.

### `Port 8080 was already in use`

**Nguyên nhân:** Có app khác (hoặc instance cũ) đang dùng port 8080.

**Cách fix:**
- Dừng process cũ, hoặc
- Đổi port trong `application.yaml` (`server.port`)

### Bảng không xuất hiện trong Navicat

**Cách fix:**
1. Chạy app thành công (không lỗi startup)
2. Refresh database `synctube` trong Navicat
3. Kiểm tra `ddl-auto: update` đã bật

---

## 10. Workflow dev hàng ngày

```powershell
# 1. Bật MySQL (XAMPP / service / Docker)

# 2. Vào thư mục project
cd c:\Users\buiqu\Desktop\synctube

# 3. Chạy app
.\mvnw.cmd spring-boot:run

# 4. Test API (tab terminal khác)
curl http://localhost:8080/api/users

# 5. Sửa code → app tự reload nếu bật DevTools (chưa thêm)
#    Hoặc dừng (Ctrl+C) rồi chạy lại spring-boot:run

# 6. Trước khi commit, chạy test
.\mvnw.cmd test
```

---

## 11. Lệnh hữu ích khác

### Xem dependency conflict

```powershell
.\mvnw.cmd dependency:tree
```

### Build nhanh, bỏ test

```powershell
.\mvnw.cmd clean package -DskipTests
```

### Chạy với log debug (khi cần debug lỗi startup)

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--debug
```

---

## 12. OAuth & biến môi trường

Tạo OAuth app và set biến môi trường trước khi chạy:

| Biến | Mô tả |
|------|--------|
| `GOOGLE_CLIENT_ID` | Client ID Google OAuth |
| `GOOGLE_CLIENT_SECRET` | Client secret Google |
| `FACEBOOK_CLIENT_ID` | App ID Facebook |
| `FACEBOOK_CLIENT_SECRET` | App secret Facebook |
| `JWT_SECRET` | Secret JWT (tối thiểu 32 ký tự) |
| `FRONTEND_URL` | URL FE callback (mặc định `http://localhost:5174`) |

**Redirect URI đăng ký trên provider:**

- Google: `http://localhost:8081/login/oauth2/code/google`
- Facebook: `http://localhost:8081/login/oauth2/code/facebook`

**Luồng đăng nhập:**

1. FE redirect → `http://localhost:8081/oauth2/authorization/google` (hoặc `facebook`)
2. Sau OAuth → BE redirect → `{FRONTEND_URL}/auth/callback?token=...`
3. FE lưu JWT, gọi `GET /api/auth/me` với header `Authorization: Bearer <token>`

**FE `.env`:**

```env
VITE_ENABLE_WEBSOCKET=true
VITE_WS_URL=ws://localhost:8081/ws
VITE_OAUTH_BASE_URL=http://localhost:8081
```

---

## 13. API endpoints tóm tắt

| Method | URL | Auth | Mô tả |
|--------|-----|------|--------|
| GET | `/api/auth/me` | JWT | User hiện tại |
| POST | `/api/auth/logout` | JWT | Đăng xuất (client xóa token) |
| GET | `/oauth2/authorization/{provider}` | — | Bắt đầu OAuth |
| GET | `/api/rooms/code/{code}` | — | Xem phòng (qua link) |
| POST | `/api/rooms` | JWT | Tạo phòng |
| DELETE | `/api/rooms/code/{code}` | JWT | Xóa phòng (host) |
| GET | `/api/rooms/code/{code}/playlist` | — | Danh sách playlist |
| POST | `/api/rooms/code/{code}/playlist/items` | JWT | Thêm video |
| DELETE | `/api/rooms/code/{code}/playlist/items/{id}` | JWT | Xóa video |
| PUT | `/api/rooms/code/{code}/playlist/reorder` | JWT | Sắp xếp lại |
| POST | `/api/rooms/code/{code}/playback/play` | JWT | Phát |
| POST | `/api/rooms/code/{code}/playback/pause` | JWT | Tạm dừng |
| POST | `/api/rooms/code/{code}/playback/seek` | JWT | Seek |
| POST | `/api/rooms/code/{code}/playback/next` | JWT | Bài tiếp |
| POST | `/api/rooms/code/{code}/playback/prev` | JWT | Bài trước |
| POST | `/api/rooms/code/{code}/playback/ended` | JWT | Video kết thúc |
| POST | `/api/rooms/code/{code}/playback/anchor` | JWT | Cập nhật anchor |

> `GET /api/rooms` (list tất cả phòng) đã **bị vô hiệu** — phòng private, chỉ vào qua link.

---

## 14. Tài liệu tham khảo

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Maven Commands](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)
