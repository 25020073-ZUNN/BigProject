# 🔨 HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (ONLINE AUCTION SYSTEM) - TEAM 6
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-orange?style=for-the-badge&logo=oracle&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-%2300f.svg?style=for-the-badge&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

---
![Static Badge](https://img.shields.io/badge/coverage-95%25-orange)
![Maintenance](https://img.shields.io/badge/Maintainance-yes-green.svg)
[![Static Badge](https://img.shields.io/badge/github-repo-blue?logo=github)](https://github.com/25020073-ZUNN/BigProject)
![Generic badge](https://img.shields.io/badge/BUILD-PASSING-green.svg)
![GitHub license](https://img.shields.io/github/license/Naereen/StrapDown.js.svg)
![Static Badge](https://img.shields.io/badge/star-1-yellow)

![Static Badge](https://img.shields.io/badge/Project_Manager-Tan_Dung-indigo)
![Static Badge](https://img.shields.io/badge/Logic-Trung_Dung-pink)
![Static Badge](https://img.shields.io/badge/UX/contributors-Van_Thuyet-purple)
![Static Badge](https://img.shields.io/badge/contributors-Pham_Trung-aqua)

---

## 1. Giới thiệu dự án
**Aurex Auction** là một nền tảng đấu giá trực tuyến được xây dựng nhằm cung cấp môi trường đấu giá minh bạch, an toàn và tức thời. Hệ thống hỗ trợ người dùng tham gia đấu giá các loại tài sản (Điện tử, Nghệ thuật, Phương tiện) với các tính năng tự động hóa thông minh.

**Phạm vi hệ thống:**
- Quản lý tài khoản và vai trò người dùng (User, Admin).
- Đăng tải và quản lý sản phẩm đấu giá.
- Tham gia đấu giá thời gian thực qua Socket.
- Xử lý các nghiệp vụ nâng cao: Tự động gia hạn, Đấu giá tự động, và Trực quan hóa dữ liệu.

---

## 2. Công nghệ và Yêu cầu hệ thống
### Công nghệ sử dụng:
- **Ngôn ngữ:** Java 17.
- **Giao diện:** JavaFX 21 (FXML & CSS).
- **Mạng:** TCP Socket, JSON (Gson).
- **Cơ sở dữ liệu:** MySQL 8.0.
- **Ảo hóa:** Docker & Docker Compose.
- **Thư viện khác:** BCrypt (Bảo mật), Cloudinary (Lưu trữ ảnh), JavaMail (OTP).

### Yêu cầu cài đặt:
- **Java JDK 17** trở lên.
- **Maven 3.8+**.
- **Docker** (Khuyến khích dùng để chạy Server & DB nhanh chóng).
- **MySQL** (Nếu không dùng Docker).

---

## 3. Cấu trúc thư mục chính
```text
BigProject/
├── docs/               # Tài liệu dự án (Báo cáo PDF, REPORT.md)
├── src/main/java/      # Mã nguồn Java
│   └── com/auction/
│       ├── controller/ # Xử lý logic giao diện JavaFX
│       ├── dao/        # Tầng truy xuất dữ liệu (Data Access Object)
│       ├── model/      # Các thực thể nghiệp vụ (User, Item, Auction...)
│       ├── network/    # Xử lý Socket Client/Server
│       └── service/    # Logic nghiệp vụ chính (Auth, Auction, Image...)
├── src/main/resources/ # FXML, CSS và hình ảnh
├── Dockerfile          # Cấu hình đóng gói Server
├── docker-compose.yml  # Phối hợp chạy Server và MySQL
└── pom.xml             # Quản lý phụ thuộc Maven
```

---

## 4. Hướng dẫn chạy chương trình

### Bước 1: Khởi chạy Cơ sở dữ liệu và Server
**Cách 1: Sử dụng Docker (Khuyến nghị - Chạy được trên Linux, MacOS, Windows)**
```bash
# Tại thư mục gốc dự án
docker-compose up -d
```

**Cách 2: Chạy thủ công bằng Maven**
1. Đảm bảo bạn đã tạo database `auction_db` trong MySQL và cấu hình đúng trong `src/main/java/com/auction/config/DBConnection.java`.
2. Chạy Server:
```bash
mvn compile exec:java -Dexec.mainClass="com.auction.network.server.Server"
```

### Bước 2: Khởi chạy Client (Giao diện người dùng)
Mở một Terminal mới và chạy lệnh sau (Tương thích Linux, MacOS, Windows):
```bash
mvn javafx:run
```

---

## 5. Danh sách chức năng đã hoàn thành
- [x] **Quản lý người dùng:** Đăng ký, Đăng nhập, Bảo mật BCrypt, Xác thực OTP.
- [x] **Quản lý sản phẩm:** Tạo phiên đấu giá, Phân loại tài sản (Factory Pattern), Lưu trữ ảnh Cloudinary.
- [x] **Đấu giá Real-time:** Cập nhật giá tức thời qua Socket (Observer Pattern).
- [x] **Concurrency:** Xử lý đấu giá đồng thời bằng Pessimistic Locking.
- [x] **Nâng cao:** Thuật toán Anti-sniping (Gia hạn tự động).
- [x] **Nâng cao:** Trực quan hóa biến động giá bằng LineChart.
- [x] **Triển khai:** Đóng gói Container hóa với Docker.

---

## 6. Tài liệu và Video Demo
- **Báo cáo chi tiết (PDF):** [Xem tại đây](docs/baocaoreal.pdf)
- **Video hướng dẫn & Demo:** [Xem trên YouTube/Drive](https://drive.google.com/file/d/1374l06QdAPYJLwBMahNG8Q0qm9O4gHEy/view)

---
*Dự án thực hiện bởi Team 6 - 2026*
