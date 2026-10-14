# Triển khai Campus Hub lên VPS

## VPS đã khảo sát

- Ubuntu 24.04 LTS, x86_64, 2 vCPU, RAM 842 MiB, đĩa root 29 GiB.
- Chưa có Docker hoặc project trong `/opt/campus-hub-events` tại thời điểm kiểm tra.
- SSH là dịch vụ duy nhất đang lắng nghe; UFW đang inactive. Chỉ thêm luật inbound 80 nếu kiểm tra HTTP sau khi triển khai xác nhận cổng bị chặn ở host firewall.
- Không có swap được cấu hình khi khảo sát; ứng dụng/JVM và PostgreSQL dùng giới hạn bộ nhớ để phù hợp máy nhỏ.

## Quy trình khởi tạo một lần

Kết nối bằng tài khoản quản trị VPS rồi cài Docker Engine và Compose plugin từ package Ubuntu:

```sh
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
sudo usermod -aG docker dun
```

Tạo thư mục triển khai, sao chép `deploy/compose.production.yaml` và `deploy/campus-hub-deploy.sh` từ repository, sau đó cài script khóa lệnh:

```sh
sudo install -d -o root -g docker -m 0750 /opt/campus-hub-events
sudo install -o root -g docker -m 0640 deploy/compose.production.yaml /opt/campus-hub-events/compose.production.yaml
sudo install -o root -g root -m 0755 deploy/campus-hub-deploy.sh /usr/local/sbin/campus-hub-deploy
```

Tạo credential DB trực tiếp trên VPS; chỉ file local trên host được giữ mật khẩu:

```sh
sudo sh -c 'umask 077; printf "DB_USERNAME=campus_hub\nDB_PASSWORD=%s\n" "$(openssl rand -hex 32)" > /opt/campus-hub-events/.env'
sudo chown root:docker /opt/campus-hub-events/.env
sudo chmod 0640 /opt/campus-hub-events/.env
```

Thêm public key riêng của GitHub Actions vào `~/.ssh/authorized_keys` của `dun` với forced command và giới hạn sau; giữ nguyên các key đang có:

```text
restrict,command="/usr/local/sbin/campus-hub-deploy" ssh-ed25519 <public-key>
```

Tài khoản `dun` cần quyền Docker để chạy deploy wrapper. Key này không được cấp PTY, port forwarding, agent forwarding hoặc shell tùy ý. `SSH_ORIGINAL_COMMAND` chỉ chấp nhận lệnh `deploy` kèm một SHA-1 Git commit 40 ký tự; wrapper tự tạo image reference GHCR từ SHA đó.

## GitHub Secrets

Thiết lập các repository secrets sau, không đưa chúng vào file theo dõi Git:

- `SSH_HOST`: địa chỉ VPS.
- `SSH_USER`: `dun`.
- `SSH_PRIVATE_KEY`: private key chỉ dành cho deploy.
- `SSH_KNOWN_HOSTS`: dòng host key Ed25519 đã xác minh từ `known_hosts`.

Khi workflow publish image chạy thành công cho push trên `main`, workflow deploy kết nối SSH với kiểm tra host key nghiêm ngặt và yêu cầu wrapper chạy đúng image tag theo SHA. GHCR image được pull ẩn danh, PostgreSQL không mở cổng host và dùng named volume để giữ dữ liệu.

## Xác minh sau triển khai

```sh
curl -fsS http://<VPS_IP>/actuator/health/readiness
curl -fsS http://<VPS_IP>/api/events
```

Tạo một đăng ký với địa chỉ thử nghiệm, gửi lại cùng email để xác nhận nhận `409`, kiểm tra không có email trong payload phản hồi. Kiểm tra lưu trữ bằng cách recreate riêng app container và truy vấn `registeredCount` lại. Không chạy `docker compose down -v`.

Đây là demo HTTP theo IP, không dùng email hay dữ liệu người tham dự thật. Cần domain và HTTPS trước khi dùng dữ liệu thật.
