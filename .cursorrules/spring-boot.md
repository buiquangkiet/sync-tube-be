# Quy tắc Cấu hình AI Cursor - SyncTube Backend (Spring Boot & JPA)

Bạn là chuyên gia phát triển Backend với Spring Boot, Spring Data JPA và REST API. Chỉ dùng các pattern đã có trong codebase `synctube` — không tự thêm công nghệ hoặc layer chưa tồn tại.

---

## 1. Stack thực tế (từ `pom.xml`)

- **Framework**: Spring Boot `4.0.6`, Java `26`
- **Starters**: `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`, `spring-boot-starter-websocket`
- **Database**: MySQL (`mysql-connector-j`) — runtime; H2 chỉ dùng cho test
- **Boilerplate**: Lombok (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, …)
- **Package gốc**: `com.example.synctube`

> Lưu ý: `websocket` đã khai báo dependency nhưng **chưa có** config/handler trong source. Không tự tạo WebSocket/STOMP trừ khi được yêu cầu.

---

## 2. Cấu trúc thư mục (`src/main/java/com/example/synctube/`)

```text
com/example/synctube/
├── SynctubeApplication.java
├── config/          # CommandLineRunner, seed data
├── controller/      # REST endpoints — chỉ gọi service, không logic nghiệp vụ
├── dto/
│   ├── request/     # Input từ client
│   └── response/    # Output ra client (không trả entity)
├── entity/          # JPA entities
├── exception/       # Custom exception + GlobalExceptionHandler
├── repository/      # JpaRepository interfaces
└── service/         # Business logic, validation, mapping
```

---

## 3. Entity (`entity/`)

- Annotation: `@Entity`, `@Table(name = "snake_case_plural")`
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- ID: `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)` kiểu `Long`
- Timestamp: `createdAt` (`updatable = false`) và `updatedAt`, set qua `@PrePersist` / `@PreUpdate`
- Quan hệ: `@ManyToOne(fetch = FetchType.LAZY, optional = false)` + `@JoinColumn(name = "...", nullable = false)`
- Giá trị mặc định: dùng `@Builder.Default` (ví dụ `playing = false`, `currentTimeSeconds = 0.0`)
- `@Column`: khai báo `nullable`, `unique`, `length` khi cần

---

## 4. Repository (`repository/`)

- Extend `JpaRepository<Entity, Long>`
- Thêm method theo naming convention Spring Data: `findByCode`, `existsByUsername`, `existsByEmail`, …
- Trả về `Optional<T>` cho `findBy*`, `boolean` cho `existsBy*`
- Không viết `@Query` trừ khi naming convention không đủ — hiện tại project **chưa dùng** `@Query`

---

## 5. DTO

### Request (`dto/request/`)

- Chỉ `@Getter @Setter` — không dùng `@Builder`
- Không dùng `@Valid`, `@NotBlank`, Bean Validation — project **chưa có** validation annotation
- Partial update (ví dụ `RoomUpdateRequest`): dùng kiểu boxed (`Boolean`, `Double`) để phân biệt "không gửi" vs "gửi giá trị"

### Response (`dto/response/`)

- `@Getter @Builder` — immutable style
- Factory method tĩnh `public static XxxResponse from(Entity entity)` map từ entity sang DTO
- **Không** trả `password` ra client (`UserResponse` chỉ có: id, username, email, displayName, createdAt, updatedAt)
- Với quan hệ: flatten field cần thiết (ví dụ `RoomResponse` có `hostId`, `hostUsername` thay vì nested object)

---

## 6. Service (`service/`)

- `@Service` + `@RequiredArgsConstructor`
- Class-level: `@Transactional(readOnly = true)`
- Method ghi (create/update/delete): thêm `@Transactional` trên method
- Inject repository qua `private final`; service khác gọi qua constructor injection (ví dụ `RoomService` → `UserService`)
- Tìm entity: method package-private `findXxxById(Long id)` dùng `orElseThrow(() -> new ResourceNotFoundException("..."))`
- List: `repository.findAll().stream().map(XxxResponse::from).toList()`
- Validation thủ công trong service, throw `BadRequestException("message tiếng Anh")` khi input sai hoặc trùng unique
- Create entity: dùng `Entity.builder()...build()` rồi `repository.save()`
- Update: load entity, set từng field nếu request field `!= null`, rồi `save()`
- Delete: check `existsById` trước, throw `ResourceNotFoundException` nếu không tồn tại, rồi `deleteById`

### Logic đặc thù Room (đã có sẵn)

- Room code: 6 ký tự, charset `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`, sinh bằng `SecureRandom`
- Lookup theo code: `code.toUpperCase()` trước khi query
- Tối đa 10 lần thử sinh code unique; fail thì `BadRequestException("Unable to generate unique room code")`

---

## 7. Controller (`controller/`)

- `@RestController` + `@RequestMapping("/api/{resources}")` + `@RequiredArgsConstructor`
- Inject service: `private final XxxService xxxService`
- Controller **mỏng**: chỉ delegate sang service, không validate, không truy cập repository
- HTTP mapping chuẩn hiện tại:

| Method | Path | Status | Return |
|--------|------|--------|--------|
| GET | `/api/users`, `/api/rooms` | 200 | `List<XxxResponse>` |
| GET | `/api/users/{id}`, `/api/rooms/{id}` | 200 | `XxxResponse` |
| GET | `/api/rooms/code/{code}` | 200 | `RoomResponse` |
| POST | `/api/users`, `/api/rooms` | 201 (`@ResponseStatus(CREATED)`) | `XxxResponse` |
| PUT | `/api/users/{id}`, `/api/rooms/{id}` | 200 | `XxxResponse` |
| DELETE | `/api/users/{id}`, `/api/rooms/{id}` | 204 (`@ResponseStatus(NO_CONTENT)`) | `void` |

- Request body: `@RequestBody XxxRequest` / `RoomUpdateRequest`
- Path variable: `@PathVariable Long id` hoặc `@PathVariable String code`

---

## 8. Exception (`exception/`)

- Custom exception extend `RuntimeException`, constructor nhận `String message`
- Hai loại đang dùng:
  - `ResourceNotFoundException` → HTTP 404
  - `BadRequestException` → HTTP 400
- `GlobalExceptionHandler`: `@RestControllerAdvice`
- Response lỗi: `ResponseEntity<Map<String, String>>` với body `{"error": "<message>"}`
- Không dùng `@ControllerAdvice` riêng, không có error code enum, không có validation error handler

---

## 9. Config & khởi tạo dữ liệu (`config/`)

- Seed data: `@Component` implement `CommandLineRunner`
- Chỉ seed khi DB trống: `if (roomRepository.count() > 0) return;`
- Dùng `repository.save(Entity.builder()...build())` trực tiếp — không qua service

---

## 10. Cấu hình (`application.yaml`)

**Main** (`src/main/resources/application.yaml`):

- `server.port: 8081`
- MySQL: `jdbc:mysql://localhost:3306/synctube`
- JPA: `ddl-auto: update`, `show-sql: true`, `format_sql: true`

**Test** (`src/test/resources/application.yaml`):

- H2 in-memory: `jdbc:h2:mem:synctube`
- JPA: `ddl-auto: create-drop`, `show-sql: false`

---

## 11. Test

- Chỉ có `SynctubeApplicationTests` với `@SpringBootTest` + `contextLoads()`
- Không có integration test controller/service — không tự thêm test phức tạp trừ khi được yêu cầu

---

## 12. Những thứ KHÔNG có trong project (không tự thêm)

- Spring Security / JWT / session auth
- Bean Validation (`@Valid`, `@NotBlank`, …)
- MapStruct hoặc mapper library
- Password hashing (password lưu plain text trong entity)
- WebSocket/STOMP handler (chỉ có dependency)
- CORS config class
- Pagination, sorting, filtering API
- OpenAPI/Swagger
- Flyway/Liquibase migration

Khi cần các tính năng trên, hỏi user trước — không assume.

---

## 13. Checklist khi thêm feature mới

1. Entity (nếu cần bảng mới) → Repository → Request/Response DTO
2. Service: validation + business logic + `from()` mapping
3. Controller: endpoint theo pattern `/api/...` ở mục 7
4. Exception: dùng `BadRequestException` / `ResourceNotFoundException` có sẵn
5. Không expose entity hoặc password ra API response
6. Giữ naming và package structure nhất quán với code hiện tại
