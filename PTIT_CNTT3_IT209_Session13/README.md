# Campus Hub — Sự kiện sinh viên

Campus Hub là mini project web responsive bằng tiếng Việt để khám phá, lọc và đăng ký sự kiện sinh viên. Ứng dụng được dựng theo SRS đã nhận; mã nguồn Campus Hub Base ban đầu không có trong workspace nên repository này là một triển khai mới, không tuyên bố là phần chỉnh sửa trực tiếp trên project base.

Repository: [github.com/pedguedes090/campus-hub-events](https://github.com/pedguedes090/campus-hub-events)

## Chức năng

- Xem danh sách sự kiện sắp diễn ra, lọc kết hợp theo từ khóa và danh mục.
- Xem chi tiết, thời gian, địa điểm, sức chứa và số chỗ còn lại.
- Đăng ký với họ tên và email; báo lỗi rõ ràng khi email trùng, sự kiện hết chỗ hoặc đã đóng đăng ký.
- Không công khai email trong API phản hồi. Email được chuẩn hóa và ràng buộc duy nhất theo sự kiện; kiểm tra sức chứa và ghi đăng ký trong transaction có khóa hàng sự kiện.
- Giao diện hỗ trợ trạng thái tải, rỗng, lỗi/thử lại và breakpoint cho điện thoại, tablet, desktop.

## Kiến trúc

```text
Browser
  └── Spring Boot + React static assets (public port 80)
        └── private Docker network
              └── PostgreSQL 17 + named volume campus_hub_production_data
```

Frontend React/TypeScript/Vite được build vào executable Spring Boot JAR. PostgreSQL là nguồn dữ liệu chính, schema được quản lý bằng Flyway. Cổng 80 phục vụ cả giao diện React và các API `/api/...`. PostgreSQL chạy trên network riêng và không mở cổng ra host. Image container chạy bằng user không phải root, filesystem read-only và có readiness health check.

## Triển khai công khai (Production VPS)

- **URL ứng dụng trực tiếp:** [http://4.230.8.209/](http://4.230.8.209/)
- **Tình trạng:** Đã triển khai và nghiệm thu hoạt động thực tế qua Internet.
- **Cấu hình:** Chạy qua Docker Compose trên máy chủ Azure Ubuntu 24.04 LTS (`deploy/compose.production.yaml`). Dữ liệu sự kiện và đăng ký được lưu trữ bền vững tại named volume `campus_hub_production_data`.
- **Bảo mật mạng:** Azure NSG mở TCP 80 (HTTP) và TCP 22 (SSH). PostgreSQL không mở cổng ra bên ngoài. Các thao tác cấu hình Nginx reverse proxy và TLS (HTTPS) có sẵn tài liệu và file mẫu trong thư mục `deploy/` cho giai đoạn gắn tên miền chính thức.

## Chạy local bằng Docker Compose

Cần Docker Engine hoặc Docker Desktop có Compose plugin.

```sh
cp .env.example .env
```

Thay `DB_PASSWORD` trong `.env` bằng mật khẩu dài, ngẫu nhiên, chỉ dùng local. Không commit `.env`.

```sh
docker compose up --build -d
docker compose ps
```

Mở `http://localhost:8080`. Kiểm tra sẵn sàng và API:

```sh
curl -fsS http://localhost:8080/actuator/health/readiness
curl -fsS http://localhost:8080/api/events
```

Dừng app mà vẫn giữ dữ liệu PostgreSQL:

```sh
docker compose down
```

Volume `campus_hub_data` vẫn còn sau lệnh này. Không dùng `docker compose down -v` nếu cần giữ dữ liệu.

## Chạy khi phát triển

Cần Java 21, Node.js 24, npm và PostgreSQL 17. Tạo database `campus_hub`, sau đó đặt các biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` theo kết nối local. Trong terminal riêng:

```sh
cd frontend
npm ci
npm run dev
```

```sh
./gradlew bootRun
```

Vite chuyển tiếp `/api` sang Spring Boot trên `localhost:8080`. Để tạo JAR có giao diện production:

```sh
./gradlew test bootJar
```

## API

| Method | Endpoint | Mục đích |
|---|---|---|
| `GET` | `/api/events?query=&category=` | Tìm sự kiện sắp diễn ra; bộ lọc có thể kết hợp |
| `GET` | `/api/events/{id}` | Chi tiết sự kiện |
| `POST` | `/api/events/{id}/registrations` | Đăng ký, body gồm `fullName`, `email`; trả `201` và mã đăng ký, không trả email |
| `GET` | `/actuator/health/readiness` | Tình trạng sẵn sàng |

Ví dụ đăng ký với dữ liệu kiểm thử:

```sh
curl -X POST http://localhost:8080/api/events/11111111-1111-4111-8111-111111111111/registrations \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Sinh viên kiểm thử","email":"student@example.com"}'
```

## Kiểm thử và CI

```sh
./gradlew test bootJar
npm --prefix frontend test -- --run
npm --prefix frontend run build
```

GitHub Actions chạy kiểm thử/build và dựng image trên pull request hoặc push vào `main`. Khi CI của push `main` thành công, workflow riêng xuất image vào GHCR với tag commit và `latest`.

## Nhóm thực hiện

1. Đinh Quang Huy
2. Nguyễn Đình Sơn
3. Phùng Quang Tiến
4. Phạm Việt An
5. Triệu Quốc Bình
6. Nguyễn Thị Yến
7. Phan Thuận Thành
8. Nguyễn Đức Anh
9. Nguyễn Việt Trung
10. Đỗ Hoàng Anh

## Giới hạn hiện tại

- Dữ liệu sự kiện mẫu phục vụ demo; chưa có màn hình quản trị sự kiện hay xác thực/ủy quyền quản trị.
- Chỉ dùng dữ liệu giả lập trong bản demo. Không nhập địa chỉ email thật khi truy cập qua HTTP. Khi triển khai IP-only HTTP, thông tin đăng ký không được bảo vệ bằng TLS; cần domain và HTTPS trước khi nhận thông tin thật.
- CI/GHCR, repository public, VPS và URL công khai chỉ được đánh dấu hoàn tất sau khi kiểm tra thực tế.
- Không khai báo giấy phép mã nguồn mở trong repository.
