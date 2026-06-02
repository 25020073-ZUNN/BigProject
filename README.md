# 🔨 HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (ONLINE AUCTION SYSTEM) - NHÓM 6 UET
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-orange?style=for-the-badge&logo=oracle&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-%2300f.svg?style=for-the-badge&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

---
![Static Badge](https://img.shields.io/badge/coverage-95%25-orange)
![Maintenance](https://img.shields.io/badge/Maintainance-yes-green.svg)
[![Static Badge](https://img.shields.io/badge/github-repo-blue?logo=github)](https://github.com/25020073-ZUNN/BigProject)
![Generic badge](https://img.shields.io/badge/BUILD-PASSING-<COLOR>.svg)
![GitHub license](https://img.shields.io/github/license/Naereen/StrapDown.js.svg)
![Static Badge](https://img.shields.io/badge/star-1-yellow)

![Static Badge](https://img.shields.io/badge/Project_Manager-Tan_Dung-indigo)
![Static Badge](https://img.shields.io/badge/Logic-Trung_Dung-pink)
![Static Badge](https://img.shields.io/badge/UX/contributors-Van_Thuyet-purple)
![Static Badge](https://img.shields.io/badge/contributors-Pham_Trung-aqua)

[![GitHub watchers](https://img.shields.io/github/watchers/Naereen/StrapDown.js.svg?style=social&label=Watch&maxAge=2592000)](https://github.com/25020073-ZUNN/BigProject/watchers)
[![GitHub followers](https://img.shields.io/github/followers/Naereen.svg?style=social&label=Follow&maxAge=2592000)](https://github.com/Naereen?tab=followers)

## 📋 1. Giới thiệu Bài toán & Phạm vi Hệ thống

### 1.1. Mô tả Bài toán
Đấu giá trực tuyến là một mô hình kinh doanh thương mại điện tử năng động đòi hỏi tính **chính xác cao, bảo mật chặt chẽ và cập nhật thời gian thực (Real-time)**. 
Hệ thống **Online Auction System** này được phát triển nhằm giải quyết bài toán giao dịch tài sản minh bạch giữa người mua và người bán dưới mô hình đấu giá tăng dần (English Auction). Hệ thống đảm bảo tính toàn vẹn tài chính, tự động hóa quá trình cạnh tranh giá cả và đồng bộ thông tin ngay tức khắc tới tất cả người tham gia mà không có độ trễ.

### 1.2. Phạm vi Hệ thống
*   **Quản lý người dùng:** Đăng ký, đăng nhập, bảo mật mật khẩu, cập nhật hồ sơ cá nhân và phân quyền đa luồng người dùng (`ADMIN`, `USER` đóng vai trò song song `SELLER` / `BIDDER`).
*   **Ví tài chính giả lập:** Mỗi tài khoản sở hữu một số dư ví. Hệ thống kiểm soát dòng tiền ký quỹ đấu giá an toàn, tự động hoàn trả cọc cho người giữ giá cũ khi có người trả giá cao hơn.
*   **Quản lý tài sản đấu giá:** Hỗ trợ sản phẩm đa phân khúc (Đồ điện tử, Phương tiện, Tác phẩm nghệ thuật) với các thuộc tính động đặc thù.
*   **Đấu giá Real-time & Auto-bid:** Cập nhật giá sản phẩm tức thì tới tất cả các client đang trực tuyến qua kết nối TCP dài hạn. Hỗ trợ robot tự động tính toán nâng giá đấu thầu theo giới hạn người dùng thiết lập.
*   **Bảng điều trị hành chính (Admin Dashboard):** Admin kiểm soát toàn quyền hệ thống, khóa/mở người dùng phá rối hoặc hủy các phiên đấu giá vi phạm.
*   **Quản lý tự trị của Người bán:** Người bán được sửa hoặc xóa các sản phẩm của mình khi phiên đấu giá chưa diễn ra.

---

## 🛠 2. Công nghệ Sử dụng & Yêu cầu Cài đặt

### 2.1. Công nghệ Sử dụng
*   **Backend & Core Logic:** Java JDK 17, Maven 3.8+.
*   **Giao diện đồ họa (Client UI):** JavaFX 17+, FXML, Vanilla CSS cho giao diện Sáng/Tối (Light/Dark Mode).
*   **Cơ sở dữ liệu:** MySQL 8.0 (chạy container hóa qua Docker).
*   **Giao thức truyền thông:** Custom TCP Sockets truyền tải dữ liệu JSON (newline-delimited JSON) tuần tự hóa qua thư viện **Google Gson**.
*   **Bảo mật mật khẩu:** Thư viện **jBCrypt** mã hóa mật khẩu một chiều cực mạnh chống tấn công dò mã.
*   **Lưu trữ hình ảnh:** API đám mây **Cloudinary** kết hợp máy chủ tệp cục bộ (Local Image Server) làm phương án dự phòng.
*   **Khôi phục mật khẩu:** Thư viện **JavaMail API** hỗ trợ gửi mã OTP xác nhận qua cổng SMTP Gmail.

### 2.2. Yêu cầu Hệ thống & Môi trường chạy (Prerequisites)
Hãy đảm bảo máy tính của bạn đã cài đặt các công cụ sau trước khi khởi chạy:
1.  **Java JDK 17** (Đã cấu hình biến môi trường `JAVA_HOME`).
2.  **Maven 3.8+** (Để quản lý thư viện và chạy JavaFX).
3.  **Docker & Docker Compose** (Khuyên dùng để khởi tạo nhanh MySQL và các dịch vụ).
4.  **Git** (Để clone dự án).

---

## 📂 3. Cấu trúc Thư mục và Các Module chính

Dưới đây là sơ đồ cấu trúc các lớp cốt lõi của hệ thống dưới package chính `com.auction`:

```text
src/main/java/com/auction/
│
├── Main.java                        # Điểm khởi chạy Client JavaFX & kiểm tra DB
│
├── config/                          # Cấu hình hệ thống
│   └── DBConnection.java            # Quản lý kết nối JDBC MySQL (Port 3307)
│
├── factory/                         # Áp dụng Factory Design Pattern
│   └── ItemFactory.java             # Khởi tạo đa hình Electronics, Vehicle, Art
│
├── model/                           # Tầng mô tả thực thể dữ liệu (Entities)
│   ├── Entity.java                  # Lớp cha tối cao sinh UUID tự động
│   ├── Auction.java                 # Thông tin phiên đấu giá & lịch sử trả giá
│   ├── BidTransaction.java          # Nhật ký một lượt trả giá thầu
│   ├── item/                        # Các lớp mô tả sản phẩm (Art, Vehicle, Electronics...)
│   └── user/                        # Các lớp mô tả người dùng (Admin, Seller, Bidder...)
│
├── dao/                             # Tầng giao tiếp cơ sở dữ liệu (JDBC SQL)
│   ├── UserDao.java                 # Thao tác bảng 'users' (Đăng nhập, đăng ký, khóa user)
│   └── AuctionDao.java              # Thao tác an toàn tiền tệ với DB Transactions trên auctions
│
├── observer/                        # Áp dụng Observer Design Pattern
│   ├── AuctionObserver.java         # Interface lắng nghe sự kiện đồng bộ
│   └── AuctionSubject.java          # Quản lý và thông báo khi dữ liệu thay đổi
│
├── service/                         # Tầng logic nghiệp vụ (Business Logic)
│   ├── AuctionService.java          # Bộ nhớ RAM Snapshot đồng bộ dữ liệu đấu giá
│   ├── AuthService.java             # Nghiệp vụ đăng nhập, bảo mật tài khoản
│   ├── AutoBidStrategy.java         # Thuật toán tính giá đấu thầu tự động
│   └── ImageStorageService.java     # Tải và đồng bộ ảnh lên Cloudinary / Local
│
├── network/                         # Tầng mạng truyền tải dữ liệu
│   ├── Message.java                 # Định dạng gói tin API JSON qua TCP
│   ├── client/                      # Kết nối TCP phía Client (NetworkService & readLoop)
│   └── server/                      # Server đa luồng xử lý đồng thời & Broadcast
│
├── util/                            # Tầng tiện ích hỗ trợ đồ họa & định dạng
│   ├── FxAsync.java                 # Tiện ích chạy mạng ngầm không treo UI JavaFX
│   ├── ThemeManager.java            # Quản lý giao diện Sáng / Tối (Dark mode)
│   └── SceneNavigator.java          # Điều phối chuyển hướng màn hình FXML
│
└── controller/                      # Tầng điều khiển giao diện JavaFX
    ├── HomeController.java          # Điều khiển màn hình chính
    ├── AuctionDetailController.java # Xử lý đặt giá, Auto-bid
    └── AdminDashboardController.java# Giao diện quản trị viên tối cao
```

---

## 🚀 4. Thứ tự & Câu lệnh Khởi chạy cụ thể (Hỗ trợ Đa Hệ điều hành)

Hệ thống hoạt động theo kiến trúc Client - Server. Do đó, **bạn bắt buộc phải chạy Server trước, đảm bảo Server sẵn sàng kết nối, sau đó mới chạy Client**.

### CẤU HÌNH MÔI TRƯỜNG (Tùy chọn)
Nếu bạn có tài khoản Cloudinary để lưu ảnh trên đám mây, hãy tạo file `.env.local` tại thư mục gốc của dự án và điền thông tin cấu hình:
```env
CLOUDINARY_URL=cloudinary://<api_key>:<api_secret>@<cloud_name>
```
*(Nếu không cấu hình, hệ thống sẽ tự động lưu trữ hình ảnh sản phẩm vào bộ nhớ máy cục bộ thông qua Docker volume).*

---

### BƯỚC 1: KHỞI CHẠY DATABASE & TCP SERVER (SERVER SIDE)

Có 2 cách để khởi chạy phía Server tùy thuộc vào môi trường máy của bạn:

#### CÁCH A: Chạy nhanh qua Docker Compose (Khuyên dùng - Hoạt động trên mọi OS)
Mở Terminal (trên Linux/macOS) hoặc PowerShell/CMD (trên Windows), di chuyển tới thư mục gốc dự án `BigProject` và chạy lệnh:

*   **Trên mọi hệ điều hành (Windows, Linux, macOS):**
    ```bash
    docker compose up --build -d
    ```
    *Lệnh này sẽ tự động khởi dựng 3 dịch vụ:*
    *   **MySQL Database** chạy ở cổng host **`3307`** (Tự động nạp schema dữ liệu ban đầu).
    *   **TCP Auction Server** chạy ở cổng host **`5050`**.
    *   **Local Image Server** chạy ở cổng host **`8081`** (Lưu trữ ảnh cục bộ).

*   **Lệnh theo dõi trạng thái Server:**
    ```bash
    # Xem log hoạt động thời gian thực của Server để biết Client kết nối chưa
    docker compose logs -f server
    ```

#### CÁCH B: Chạy thủ công không qua Docker
1.  Hãy chắc chắn bạn có một máy chủ MySQL đang chạy độc lập trên máy ở cổng `3306` (hoặc cấu hình lại URL trong `.env.local`).
2.  Chạy Server bằng Maven qua dòng lệnh:
    *   **Trên Windows (PowerShell / CMD):**
        ```powershell
        mvn exec:java -Dexec.mainClass="com.auction.network.server.Server"
        ```
    *   **Trên Linux / macOS (Terminal):**
        ```bash
        mvn exec:java -Dexec.mainClass="com.auction.network.server.Server"
        ```

---

### BƯỚC 2: KHỞI CHẠY GIAO DIỆN CLIENT JAVAFX (CLIENT SIDE)

Sau khi phía Server đã sẵn sàng và báo trạng thái hoạt động tốt, mở một cửa sổ Terminal/PowerShell mới độc lập tại thư mục dự án và chạy Client JavaFX:

#### 1. Trên hệ điều hành Windows:
*   **Chạy qua PowerShell hoặc Command Prompt:**
    ```powershell
    mvn javafx:run
    ```

#### 2. Trên hệ điều hành macOS hoặc Linux:
*   **Chạy qua Terminal:**
    ```bash
    mvn javafx:run
    ```

#### *Lưu ý quan trọng cho Linux/macOS khi gặp lỗi đồ họa JavaFX:*
Nếu môi trường máy Linux/macOS của bạn thiếu thư viện đồ họa hiển thị X11 hoặc Wayland, hãy chắc chắn bạn chạy trong môi trường có desktop đồ họa hỗ trợ, hoặc cài đặt bổ sung OpenJFX tương thích:
```bash
# Trên Ubuntu/Debian:
sudo apt-get install openjfx
```

---

### ⚠️ Lưu ý quan trọng khi Dọn dẹp Tài nguyên (Port Cleanup)
Vì hệ thống sử dụng các cổng cố định trên máy host (**`5050`** cho TCP Server, **`3307`** cho Database, và **`8081`** cho Image Server), để tránh lỗi xung đột cổng mạng với các đồ án khác khi chấm điểm, **hãy luôn giải phóng cổng sau khi dừng kiểm thử bằng lệnh:**

```bash
docker compose down
```
*(Nếu muốn xóa sạch toàn bộ dữ liệu trong Database để chạy lại từ đầu như mới, sử dụng lệnh: `docker compose down -v`).*

---

## 🏆 5. Danh sách Chức năng đã hoàn thành

Hệ thống đã được thiết kế hoàn chỉnh và tích hợp đầy đủ các chức năng nghiệp vụ cao cấp của một sàn đấu giá thực thụ:

| STT | Phân hệ Chức năng | Chức năng chi tiết đã hoàn thành | Mẫu thiết kế áp dụng |
| :--- | :--- | :--- | :--- |
| **1** | **Xác thực người dùng** | Đăng nhập tài khoản nhanh chóng, đăng ký tài khoản mới mã hóa bảo mật. Cập nhật hồ sơ cá nhân (Họ tên, Email), xóa tài khoản bảo toàn dữ liệu (Soft Delete). | *Singleton*, *jBCrypt* |
| **2** | **Quên & Đặt lại mật khẩu** | Yêu cầu sinh OTP ngẫu nhiên gửi trực tiếp về Email người dùng qua luồng SMTP ngầm. Đặt lại mật khẩu mới có xác thực OTP hết hạn sau 5 phút. | *Multithreading SMTP* |
| **3** | **Quản lý tài sản đa hình** | Thêm mới và quản lý sản phẩm đấu giá thuộc 3 danh mục lớn: Đồ điện tử, Xe cộ, Nghệ thuật với các thuộc tính động riêng biệt. | *Factory Pattern* |
| **4** | **Đặt giá thầu thời gian thực** | Đặt giá thầu cho sản phẩm. Đồng bộ dữ liệu lập tức tới tất cả các Client đang online cùng xem thay đổi giá mà không cần reload trang. | *TCP Socket Broadcast*, *Observer Pattern* |
| **5** | **Giao dịch tài chính an toàn** | Cơ chế ký quỹ ví điện tử giả lập tự động. Trừ tiền người đấu giá mới nhất, tự động cộng hoàn cọc trả lại cho người giữ giá cao nhất cũ. | *Database Transaction (Rollback an toàn)* |
| **6** | **Đấu giá tự động (Auto-bid)** | Tự động tăng giá đấu thầu thay thế cho người chơi khi có đối thủ cạnh tranh, đảm bảo giá nâng tối ưu và không vượt quá hạn mức tối đa của ví. | *Strategy Pattern* |
| **7** | **Quyền hạn Admin** | Bảng quản lý Admin tối cao: xem danh sách tài khoản, khóa/mở hoạt động tài khoản phá rối, xóa các phiên đấu giá lỗi hoặc vi phạm. | *Admin Authorization* |
| **8** | **Quyền hạn Người bán** | Người bán tự quản lý sản phẩm của mình: Được quyền chỉnh sửa thông tin hoặc hủy phiên đấu giá khi phiên đó chưa chính thức bắt đầu. | *Owner Authorization* |
| **9** | **Đồng bộ hóa hình ảnh** | Tải ảnh sản phẩm thông minh. Ưu tiên đồng bộ trực tiếp lên dịch vụ lưu trữ đám mây Cloudinary, tự động fallback lưu cục bộ nếu mất mạng. | *Cloudinary API* |
| **10**| **Tùy biến giao diện** | Thay đổi chủ đề giao diện Sáng / Tối (Light & Dark Mode) mượt mà bằng CSS để cá nhân hóa trải nghiệm người dùng. | *CSS Theme Manager* |

---

Chúc bạn có một buổi trải nghiệm và báo cáo đồ án thành công rực rỡ! Nếu bạn có bất kỳ câu hỏi nào về các thuật toán hay các lớp trong dự án, mình luôn sẵn sàng đồng hành hỗ trợ bạn! 🚀

