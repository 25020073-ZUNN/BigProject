# BigProject
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Git](https://img.shields.io/badge/git-%23F05033.svg?style=for-the-badge&logo=git&logoColor=white)

![Static Badge](https://img.shields.io/badge/coverage-95%25-orange)
![Maintenance](https://img.shields.io/badge/Maintainance-yes-green.svg)
[![Static Badge](https://img.shields.io/badge/github-repo-blue?logo=github)](https://github.com/25020073-ZUNN/BigProject)
![Generic badge](https://img.shields.io/badge/BUILD-PASSING-<COLOR>.svg)
![GitHub license](https://img.shields.io/github/license/Naereen/StrapDown.js.svg)
![Static Badge](https://img.shields.io/badge/star-1-yellow)

![Static Badge](https://img.shields.io/badge/Project_Manager-Tan_Dung-indigo)
![Static Badge](https://img.shields.io/badge/contributors-Trung_Dung-pink)
![Static Badge](https://img.shields.io/badge/UX/contributors-Van_Thuyet-purple)
![Static Badge](https://img.shields.io/badge/contributors-Pham_Trung-aqua)

[![GitHub watchers](https://img.shields.io/github/watchers/Naereen/StrapDown.js.svg?style=social&label=Watch&maxAge=2592000)](https://github.com/25020073-ZUNN/BigProject/watchers)
[![GitHub followers](https://img.shields.io/github/followers/Naereen.svg?style=social&label=Follow&maxAge=2592000)](https://github.com/Naereen?tab=followers)

---

## 📋 Giới thiệu
Dự án đấu giá trực tuyến (Auction System) được phát triển bằng Java, hỗ trợ kiến trúc Client-Server qua giao thức TCP, tích hợp lưu trữ ảnh Cloudinary và cơ sở dữ liệu MySQL.

## 🛠 Yêu cầu hệ thống (Prerequisites)
Trước khi bắt đầu, hãy đảm bảo máy của bạn đã cài đặt:
*   **Docker** & **Docker Compose** (Khuyên dùng để triển khai nhanh)
*   **Java JDK 17** (Nếu muốn chạy hoặc phát triển code trực tiếp)
*   **Maven 3.8+** (Nếu muốn build thủ công)
*   **Git** (Để clone dự án)

## 🚀 Hướng dẫn cài đặt nhanh (Quick Start)

### 1. Clone Repository
Mở terminal và chạy lệnh:
```bash
git clone https://github.com/25020073-ZUNN/BigProject.git
cd BigProject
```

### 2. Cấu hình môi trường (Tùy chọn)
Để sử dụng tính năng upload ảnh lên Cloud, bạn cần tạo file `.env.local` tại thư mục gốc và điền thông tin Cloudinary (Nếu không có, hệ thống sẽ tự động lưu ảnh vào bộ nhớ máy/Docker volume):

```env
CLOUDINARY_URL=cloudinary://<api_key>:<api_secret>@<cloud_name>
# Hoặc cấu hình chi tiết:
CLOUDINARY_CLOUD_NAME=your_name
CLOUDINARY_API_KEY=your_key
CLOUDINARY_API_SECRET=your_secret
```

### 3. Khởi chạy với Docker (Recommended)
Đây là cách nhanh nhất để chạy toàn bộ hệ thống (bao gồm Database và TCP Server):

```bash
# Khởi động hệ thống
docker compose up --build -d
```

*Hệ thống sẽ khởi tạo:*
- **MySQL Database:** Cổng `3307`
- **TCP Auction Server:** Cổng `5050`
- **Local Image Server:** Cổng `8081` (Dùng khi không có Cloudinary)

### 4. Chạy Client JavaFX
Sau khi Server trong Docker đã báo `healthy`, bạn có thể khởi động giao diện người dùng từ máy host:

```bash
mvn javafx:run
```

---

## 🐳 Chi tiết về Docker

### ⚠️ Lưu ý quan trọng khi chấm bài / Chạy nhiều Project cùng lúc
Hệ thống sử dụng các cổng cố định trên máy host:
- **`5050`**: TCP Server
- **`3307`**: MySQL Database
- **`8081`**: Local Image Server

**Nếu Giảng viên / Người chấm thi cần chạy nhiều đồ án trên cùng một máy, vui lòng thực hiện dọn dẹp hệ thống cũ trước khi chạy bài mới để tránh XUNG ĐỘT CỔNG (Lỗi: Port is already allocated):**

```bash
# Bắt buộc chạy lệnh này ở thư mục của project CŨ (nếu có) trước khi test project này, 
# và chạy lệnh này ở project NÀY sau khi test xong:
docker compose down
```
Lệnh `down` sẽ dừng và giải phóng toàn bộ các cổng để máy tính sẵn sàng cho lần chạy tiếp theo.

### Quản lý Container
| Lệnh | Mô tả |
| :--- | :--- |
| `docker compose up --build -d`| Khởi chạy hệ thống lần đầu tiên (có build lại code Server) |
| `docker compose up -d` | Khởi chạy hệ thống (từ lần thứ 2 trở đi, siêu nhanh) |
| `docker compose logs -f` | Xem log của toàn bộ hệ thống |
| `docker compose logs -f server`| Xem log riêng của TCP Server (xem Client kết nối chưa) |
| `docker compose down` | Dừng các dịch vụ và giải phóng cổng (Luôn khuyên dùng sau khi test xong) |
| `docker compose down -v` | **Cẩn thận:** Dừng dịch vụ và XÓA SẠCH dữ liệu database/ảnh |

### Lưu trữ dữ liệu (Persistence)
Dữ liệu của bạn được đảm bảo không bị mất khi restart Docker nhờ vào **Volumes**:
- `mysql_data`: Lưu trữ toàn bộ cơ sở dữ liệu MySQL.
- `uploaded_images`: Lưu trữ ảnh nếu bạn không sử dụng Cloudinary.

---

## 🔧 Cấu hình nâng cao
Nếu bạn muốn chạy trực tiếp không qua Docker, hãy đảm bảo có MySQL server chạy ở cổng `3306` và cập nhật thông tin trong file `.env.local` hoặc biến môi trường:
- `DB_URL`: JDBC connection string.
- `DB_USER`, `DB_PASSWORD`: Thông tin đăng nhập DB.
- `SERVER_PORT`: Cổng chạy TCP Server (mặc định 5050).

---

## 🛠 Xử lý lỗi thường gặp (Troubleshooting)

### Kiểm tra trạng thái Docker
Trước khi chạy lệnh `docker compose`, hãy đảm bảo Docker daemon đang hoạt động:

#### Trên Linux:
1.  **Kiểm tra trạng thái:** `systemctl status docker`
2.  **Khởi động nếu chưa chạy:** `sudo systemctl start docker`
3.  **Lỗi quyền truy cập (`unix:///var/run/docker.sock`):** 
    Chạy lệnh sau để thêm user vào nhóm docker (sau đó đăng xuất và đăng nhập lại):
    ```bash
    sudo usermod -aG docker $USER
    ```

#### Trên Windows/macOS:
1.  Mở ứng dụng **Docker Desktop**.
2.  Chờ cho biểu tượng cá voi ở thanh taskbar báo trạng thái **"Running"**.
3.  Nếu gặp lỗi "Docker Engine stopped", hãy thử Restart lại Docker Desktop.

### Kiểm tra kết nối nhanh
Mở terminal và gõ:
```bash
docker ps
```
Nếu lệnh trả về tiêu đề cột (dù không có container nào đang chạy), Docker đã sẵn sàng. Nếu trả về lỗi "Command not found" hoặc "Cannot connect", hãy quay lại bước kiểm tra trạng thái ở trên.

