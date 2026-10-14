# Đặc tả thiết kế — Campus Hub sự kiện

**Ngày:** 2026-10-14  
**Nhóm:** Nguyễn Đăng Dương và các thành viên được cung cấp trong yêu cầu  
**Trạng thái:** Chờ chủ dự án duyệt đặc tả

## 1. Bối cảnh và mục tiêu

Thông tin hoạt động trong trường thường nằm rải rác ở nhiều kênh, khiến sinh viên khó tìm sự kiện phù hợp và ban tổ chức khó ghi nhận đăng ký. Campus Hub sự kiện cung cấp một nơi để xem, lọc, đọc chi tiết và đăng ký tham gia các sự kiện sinh viên. Đây là một sản phẩm độc lập được xây dựng theo yêu cầu SRS; trong workspace hiện chưa có mã nguồn Campus Hub Base để kế thừa, vì vậy báo cáo sẽ nêu rõ phạm vi tự xây dựng, không khẳng định đã sửa đổi một mã nguồn nền không có sẵn.

## 2. Người dùng và phạm vi

### Người dùng

- Sinh viên/khách: tìm sự kiện, xem chi tiết và gửi đăng ký.
- Ban tổ chức: quản lý dữ liệu sự kiện ban đầu bằng dữ liệu khởi tạo. Phiên bản hiện tại không cung cấp quyền quản trị công khai.

### Trong phạm vi

1. Trang danh sách sự kiện sắp diễn ra, tìm theo từ khóa và lọc theo danh mục.
2. Trang chi tiết gồm tên, mô tả, thời gian, địa điểm, danh mục, sức chứa và số chỗ còn lại.
3. Đăng ký bằng họ tên và email; xác thực đầu vào, chống đăng ký trùng cho cùng một sự kiện và không vượt sức chứa.
4. API backend, lưu dữ liệu bền vững trong PostgreSQL và dữ liệu sự kiện khởi tạo để có thể dùng ngay.
5. Giao diện tiếng Việt, tương thích màn hình điện thoại/máy tính, có trạng thái tải, rỗng và lỗi.
6. Kiểm thử tự động, Docker Compose, quy trình CI/CD và báo cáo nghiệm thu theo SRS.

### Ngoài phạm vi phiên bản đầu

- Đăng nhập/SSO sinh viên, thanh toán, QR check-in, gửi email thật, hủy đăng ký và bảng quản trị có xác thực.
- Công khai danh sách người tham dự hoặc email.
- Thu thập dữ liệu cá nhân thật trước khi có HTTPS và chính sách vận hành phù hợp.

## 3. Luồng nghiệp vụ

1. Người dùng mở trang chủ; ứng dụng hiển thị các sự kiện sắp diễn ra.
2. Người dùng tìm kiếm/lọc và mở một sự kiện để xem chi tiết, thời điểm, địa điểm và chỗ còn lại.
3. Người dùng gửi họ tên và email. Backend chuẩn hóa email, kiểm tra dữ liệu và trạng thái/sức chứa của sự kiện.
4. Trong một giao dịch cơ sở dữ liệu, backend khóa/đồng bộ cập nhật sức chứa và ghi đăng ký; ràng buộc duy nhất ngăn cùng email đăng ký cùng sự kiện nhiều lần.
5. UI xác nhận thành công hoặc hiển thị lỗi có thể xử lý: sự kiện không tồn tại, dữ liệu sai, đăng ký trùng, sự kiện đã đầy/đóng.

## 4. Dữ liệu và quy tắc

### Event

- `id`: UUID, khóa chính.
- `title`: bắt buộc, độ dài hữu hạn.
- `category`: danh mục hiển thị/lọc.
- `description`: nội dung mô tả.
- `location`: địa điểm.
- `starts_at`, `ends_at`: thời gian có múi giờ; kết thúc phải sau bắt đầu.
- `capacity`: số nguyên dương.
- `registration_open`: trạng thái nhận đăng ký.
- `created_at`, `updated_at`.

### Registration

- `id`: UUID, khóa chính.
- `event_id`: khóa ngoại tới Event.
- `full_name`: bắt buộc, đã trim và giới hạn độ dài.
- `email_normalized`: bắt buộc, lowercase/trim; không trả về qua API công khai.
- `created_at`.
- Unique constraint `(event_id, email_normalized)`.

Số chỗ còn lại được tính từ sức chứa trừ số đăng ký đã lưu; không lưu một bộ đếm có thể lệch. Cập nhật đăng ký cần giao dịch và khóa hàng sự kiện hoặc cơ chế tương đương để các yêu cầu đồng thời không vượt sức chứa. Email được dùng chỉ để chống trùng ở phiên bản này; API không trả email, không có endpoint công khai liệt kê đăng ký, và cần dữ liệu giả trong demo.

## 5. API

- `GET /api/events?query=&category=` — danh sách sự kiện sắp diễn ra; chỉ trả trường công khai và số chỗ còn.
- `GET /api/events/{id}` — chi tiết sự kiện; `404` nếu không tồn tại.
- `POST /api/events/{id}/registrations` — nhận `{ "fullName": "...", "email": "..." }`; trả `201` cùng mã đăng ký/trạng thái xác nhận, không trả email.

Mã phản hồi thống nhất: `400` cho payload không hợp lệ; `404` cho sự kiện không tồn tại; `409` cho đăng ký trùng, hết chỗ hoặc đã đóng; `503` khi kho dữ liệu tạm không sẵn sàng. Lỗi trả cấu trúc JSON an toàn, không lộ stack trace hay thông tin hạ tầng.

## 6. Kiến trúc đề xuất

- Một ứng dụng Spring Boot phục vụ API và các tài nguyên tĩnh React đã build; một image/container ứng dụng.
- React + TypeScript cho giao diện; Spring Boot REST, Bean Validation, Spring Data JPA cho backend; PostgreSQL là nguồn dữ liệu chính.
- Docker Compose chạy ứng dụng và PostgreSQL trên VPS. Docker database không publish ra Internet; volume giữ dữ liệu qua lần cập nhật container.
- GitHub Actions chạy kiểm tra, build image và đưa image lên GHCR. Job triển khai qua SSH chỉ chạy khi repository đã có secrets được cấu hình; trên VPS cập nhật container mà giữ nguyên database volume.
- Cấu hình, mật khẩu và khóa SSH chỉ qua GitHub Secrets/biến môi trường; không commit bí mật.
- VPS đích dự kiến là `4.230.8.209`; chưa xác nhận hệ điều hành, domain, DNS, cổng tường lửa và cấu hình HTTPS. Public endpoint chỉ được công bố sau khi kiểm tra các điều kiện này. HTTPS là điều kiện trước khi thu thập dữ liệu người tham dự thật. Nếu chỉ dùng IP/HTTP để demo, cần dữ liệu thử nghiệm, không phải email thật.

## 7. Kiểm tra và tiêu chí nghiệm thu

1. Danh sách và bộ lọc trả dữ liệu chính xác; trang chi tiết hiển thị dữ liệu sự kiện và số chỗ còn.
2. Đăng ký hợp lệ tạo đúng một bản ghi và trả xác nhận; họ tên/email sai nhận `400`.
3. Đăng ký lặp cùng sự kiện nhận `409`; email không xuất hiện trong phản hồi/endpoint công khai.
4. Khi sự kiện đầy/đóng, đăng ký bị từ chối; thử nghiệm đồng thời không làm số đăng ký vượt sức chứa.
5. Không tìm thấy sự kiện nhận `404`; lỗi cơ sở dữ liệu không làm lộ nội bộ.
6. Kiểm thử tầng service/API và kiểm thử tích hợp với PostgreSQL; CI chạy cùng bộ kiểm thử.
7. `docker compose up` khởi chạy ứng dụng và database; dữ liệu vẫn còn sau khi thay container ứng dụng.
8. Chỉ ghi nhận URL công khai, HTTPS, ảnh triển khai và kết quả live sau khi thực sự triển khai và xác minh.

## 8. Báo cáo và bàn giao

Báo cáo `.docx` theo SRS sẽ có danh sách 10 thành viên, mục tiêu/phạm vi, các luồng và giao diện, chức năng/API, mô hình dữ liệu, kiến trúc phần mềm và triển khai, khác biệt với base (nêu rõ base không có trong workspace), kiểm thử, giới hạn và bằng chứng thực tế. Repo và URL công khai sẽ được thêm sau khi được tạo/được cung cấp; placeholder không được trình bày như một URL hoạt động.

Danh sách nhóm:

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

## 9. Rủi ro và câu hỏi triển khai còn mở

- Cần URL repository GitHub đích hoặc quyền tạo repository dưới tài khoản đã xác thực.
- Cần xác nhận hệ điều hành, domain (nếu có), DNS, cổng mạng và cấu hình VPS; thông tin truy cập chỉ cấu hình bằng secrets, không gửi private key/mật khẩu trong chat.
- Mật khẩu VPS vừa được gửi trong hội thoại; cần đổi mật khẩu trước khi sử dụng lại tài khoản.
- Nếu chưa có domain, endpoint IP có thể dùng cho demo nhưng không thu thập email thật qua HTTP.
- SRS đề cập Campus Hub Base nhưng mã nguồn tương ứng chưa được tìm thấy; nếu nhóm có repo/base riêng, cần cung cấp để có thể tích hợp và mô tả delta chính xác.
