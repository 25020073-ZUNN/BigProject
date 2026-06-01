# PROJECT_EXPLANATION_VI - Phân tích sâu codebase Aurex Auction

Tài liệu này được viết cho developer mới cần hiểu kiến trúc, luồng xử lý và logic nghiệp vụ chính của dự án JavaFX Maven `auction-ui`. Codebase là một hệ thống đấu giá dùng JavaFX làm client, TCP socket JSON làm giao thức client-server, MySQL làm nguồn dữ liệu chính, và Cloudinary hoặc HTTP local server để lưu ảnh tài sản.

## Mục lục

1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Cấu trúc package](#2-cấu-trúc-package)
3. [Dependency analysis](#3-dependency-analysis)
4. [Entry point và lifecycle JavaFX](#4-entry-point-và-lifecycle-javafx)
5. [Phân tích package-by-package](#5-phân-tích-package-by-package)
6. [Class analysis](#6-class-analysis)
7. [Line-by-line/block-by-block cho class quan trọng](#7-line-by-lineblock-by-block-cho-class-quan-trọng)
8. [JavaFX UI analysis](#8-javafx-ui-analysis)
9. [Service layer analysis](#9-service-layer-analysis)
10. [Network layer analysis](#10-network-layer-analysis)
11. [Authentication và security](#11-authentication-và-security)
12. [Data models và database mapping](#12-data-models-và-database-mapping)
13. [Flow analysis cho use case chính](#13-flow-analysis-cho-use-case-chính)
14. [Sequence diagrams](#14-sequence-diagrams)
15. [Call graph](#15-call-graph)
16. [State management](#16-state-management)
17. [Concurrency analysis](#17-concurrency-analysis)
18. [Error handling](#18-error-handling)
19. [Configuration analysis](#19-configuration-analysis)
20. [Design pattern analysis](#20-design-pattern-analysis)
21. [Code smell và đề xuất cải thiện](#21-code-smell-và-đề-xuất-cải-thiện)

---

## 1. Tổng quan dự án

### Mục đích dự án

Dự án xây dựng một nền tảng đấu giá trực tuyến desktop. Người dùng có thể đăng ký, đăng nhập, xem danh sách phiên đấu giá, xem chi tiết tài sản, đặt giá, dùng auto-bid, tạo phiên đấu giá mới, chỉnh sửa hoặc xóa phiên chưa bắt đầu, xem lịch sử đấu giá và quản trị tài khoản/phiên nếu là admin.

### Chức năng chính

- Đăng ký, đăng nhập, đăng xuất, cập nhật hồ sơ, xóa tài khoản mềm.
- Quên mật khẩu bằng OTP gửi email qua SMTP.
- Xem trang chủ, danh mục phiên đấu giá, chi tiết tài sản, phòng đấu giá realtime, tổng kết phiên.
- Tạo phiên đấu giá với tài sản loại `Electronics`, `Vehicle`, `Art`.
- Upload ảnh sản phẩm qua Cloudinary nếu có cấu hình, hoặc lưu local và phục vụ qua HTTP.
- Đặt giá thủ công, kiểm tra số dư, không cho seller tự bid.
- Auto-bid dựa trên giá hiện tại, bước giá tối thiểu, bước cấu hình và mức tối đa.
- Anti-sniping: nếu bid trong 2 phút cuối thì kéo dài phiên thêm 2 phút.
- Admin xem user/auction, khóa/mở user, xóa phiên đấu giá.
- Đồng bộ realtime snapshot auction từ server tới client qua message `AUCTION_SYNC`.

### Kiến trúc tổng thể

```text
JavaFX UI
  ↓ event handlers
Controller
  ↓ FxAsync / NetworkService
TCP JSON Client
  ↓ newline-delimited JSON
Server
  ↓ Service
DAO
  ↓ JDBC transaction
MySQL

Image upload:
Controller
  ↓ Base64 in attributes
Server
  ↓ ImageStorageService
Cloudinary hoặc uploads/images + local HTTP server
```

### Công nghệ sử dụng

- Java 17.
- JavaFX 21.0.2: UI controls, FXML, scene/stage.
- Maven: build, dependency management, `javafx-maven-plugin`.
- MySQL Connector/J 8.3.0: JDBC.
- Gson 2.11.0: serialize/deserialize `Message` JSON.
- Cloudinary HTTP5 2.3.2: upload ảnh.
- JBCrypt 0.4: hash/check password.
- JavaMail 1.6.2: gửi OTP reset password.
- JUnit 5.10.2: test.
- Docker Compose: MySQL, TCP server, media storage.

---

## 2. Cấu trúc package

```text
src/main/java/com/auction
├── Main.java
├── config
│   └── DBConnection.java
├── controller
│   ├── AdminDashboardController.java
│   ├── AssetDetailController.java
│   ├── AuctionCatalogController.java
│   ├── AuctionDetailController.java
│   ├── AuctionHistoryController.java
│   ├── AuctionListController.java
│   ├── AuctionSummaryController.java
│   ├── CreateAuctionController.java
│   ├── HomeController.java
│   └── UserProfileController.java
├── dao
│   ├── AuctionDao.java
│   └── UserDao.java
├── factory
│   └── ItemFactory.java
├── model
│   ├── Auction.java
│   ├── BidTransaction.java
│   ├── Entity.java
│   ├── item
│   └── user
├── network
│   ├── Message.java
│   ├── client
│   └── server
├── observer
├── service
└── util
```

| Package | Trách nhiệm |
|---|---|
| `com.auction` | Entry point JavaFX (`Main`) và lifecycle app. |
| `config` | Đọc cấu hình DB từ env, `.env.local`, system property, default. |
| `controller` | JavaFX FXML controllers, nhận event UI, validate form cơ bản, gọi service/network. |
| `dao` | Truy cập MySQL bằng JDBC, transaction, mapping row sang domain object. |
| `factory` | Tạo polymorphic `Item` theo category. |
| `model` | Domain model: entity, auction, bid, item, user. |
| `network` | Protocol message, TCP client, TCP server, JSON payload mapping. |
| `observer` | Observer pattern cho cập nhật auction. |
| `service` | Business layer: auth, auction, auto-bid, image storage, mock user legacy. |
| `util` | JavaFX helper, async helper, navigation, theme, session, validation, formatting. |

---

## 3. Dependency analysis

Không có parent pom và không có multi-module. `pom.xml` định nghĩa một module Maven duy nhất:

```xml
<groupId>com.auction</groupId>
<artifactId>auction-ui</artifactId>
<version>1.0-SNAPSHOT</version>
<maven.compiler.source>17</maven.compiler.source>
<javafx.version>21.0.2</javafx.version>
```

| Dependency | Nhóm ảnh hưởng | Dùng để làm gì | Vì sao cần |
|---|---|---|---|
| `org.openjfx:javafx-controls` | UI | `Button`, `Label`, `TableView`, `ListView`, `LineChart`, `Scene`, `Stage`. | Đây là nền tảng UI desktop. |
| `org.openjfx:javafx-fxml` | UI | `FXMLLoader`, binding FXML tới controller qua `fx:id`, `onAction`. | Cho phép tách layout `.fxml` khỏi Java controller. |
| `com.mysql:mysql-connector-j` | Database | `DriverManager.getConnection`, JDBC tới MySQL. | DAO cần đọc/ghi bảng `users`, `items`, `auctions`, `bid_transactions`. |
| `com.google.code.gson:gson` | Serialization / Network | `Message.toJson()` và `Message.fromJson()`. | TCP protocol dùng newline-delimited JSON. |
| `com.cloudinary:cloudinary-http5` | Network / Media | Upload ảnh lên Cloudinary, tạo optimized delivery URL. | Ảnh sản phẩm cần URL mà mọi client tải được. |
| `org.junit.jupiter:junit-jupiter` | Test | Unit tests cho model, factory, observer, service, util. | Bảo vệ logic domain và helper. |
| `org.mindrot:jbcrypt` | Security | Hash password khi register/reset, verify password khi login. | Không lưu raw password trong DB. |
| `com.sun.mail:javax.mail` | Network / Security | Gửi OTP reset password qua SMTP. | Hỗ trợ flow quên mật khẩu. |

Plugin:

| Plugin | Vai trò |
|---|---|
| `maven-compiler-plugin` | Compile Java 17 bằng `<release>17</release>`. |
| `maven-surefire-plugin` | Chạy JUnit tests. |
| `javafx-maven-plugin` | Chạy app JavaFX với main class `com.auction.Main`. |

---

## 4. Entry point và lifecycle JavaFX

### Main class

`com.auction.Main` kế thừa `javafx.application.Application`. Maven plugin cũng trỏ về class này:

```xml
<mainClass>com.auction.Main</mainClass>
```

### Flow launch

```text
main(String[] args)
  ↓
Application.launch()
  ↓
JavaFX runtime tạo instance Main
  ↓
start(Stage stage)
  ↓
logDatabaseConfiguration()
  ↓
FxAsync warm-up NetworkService.isServerReachable()
  ↓
FXMLLoader load /fxml/giaodien.fxml
  ↓
FXML tạo HomeController
  ↓
ThemeManager.applyTheme(scene)
  ↓
stage.setTitle / setScene / setMinWidth / show
```

### Chi tiết `Main.start(Stage)`

- `startEmbeddedServer()` đang bị comment. Nghĩa là app client mặc định kỳ vọng server TCP chạy riêng, ví dụ qua Docker hoặc `Server.main`.
- `logDatabaseConfiguration()` in URL/user DB và gọi `AuthService.getInstance().isDatabaseAvailable()`.
- `FxAsync.run(...)` chạy warm-up mạng ở thread pool daemon để lần click đầu không bị lazy connect.
- `FXMLLoader(getClass().getResource("/fxml/giaodien.fxml"))` load home screen.
- `ThemeManager.getInstance().applyTheme(scene)` áp dụng dark/light theme đang lưu trong `Preferences`.
- Stage đặt title, minimum size `1280x820`, center và show.

### Stop lifecycle

`stop()` dừng `embeddedServer` nếu từng được start. Vì server embedded hiện đang comment, thường không có gì để stop.

---

## 5. Phân tích package-by-package

### `config`

`DBConnection`

- Vai trò: utility class tạo `Connection` JDBC mới cho mỗi thao tác DAO.
- Được gọi bởi: `UserDao`, `AuctionDao`, `Server.handleDatabaseStatus`, `Main.logDatabaseConfiguration`.
- Gọi tới: `DriverManager`, `Files`, `System.getenv`, `System.getProperty`.
- Logic cấu hình ưu tiên: environment variable → `.env.local` → JVM system property → default.

### `dao`

`UserDao`

- Vai trò: CRUD và auth database cho bảng `users`.
- Được gọi bởi: `AuthService`, `AuctionDao.mapUser`, `Server`, `AdminDashboardController` gián tiếp qua network.
- Gọi tới: `DBConnection`, `BCrypt`, model user subclasses.
- Logic chính: register, login, find, update profile, soft delete, update password, set active.

`AuctionDao`

- Vai trò: nguồn dữ liệu chính của đấu giá.
- Được gọi bởi: `AuctionService`.
- Gọi tới: `DBConnection`, `UserDao`, `ItemFactory`, `Auction`, `BidTransaction`.
- Logic chính: load snapshot, sync trạng thái theo thời gian, create/update/delete auction, place bid transaction-safe, anti-sniping.

### `service`

`AuthService`

- Singleton, bọc `UserDao`.
- Đảm bảo DB available trước login/register.

`AuctionService`

- Singleton, extends `AuctionSubject`.
- Giữ snapshot `List<Auction>` trong RAM, nhưng DB vẫn là source of truth.
- Sau thao tác ghi thành công thì `refreshAuctions()` và `notifyObservers()`.

`AutoBidStrategy`

- Pure business logic: quyết định bid tiếp theo hoặc dừng.
- Không phụ thuộc UI/network.

`ImageStorageService`

- Upload ảnh Cloudinary nếu có cấu hình, fallback local storage.
- Nếu local, start HTTP server tại `/images/`.

`UserService`

- Legacy/mock in-memory service, dùng `CopyOnWriteArrayList`.
- Không phải nguồn auth chính của app TCP hiện tại.

### `network`

`Message`

- Envelope chung cho request/response/broadcast.
- Field quan trọng: `requestId`, `type`, `payload`, `success`, `message`.

`NetworkService`

- Client-side facade dùng bởi controller.
- Quản lý `ServerConnection`, `pendingResponses`, listener realtime.

`ServerConnection`

- TCP socket client, JSON từng dòng, reader thread daemon.

`Server`

- TCP server đa client.
- Dispatch `Message.Type` tới handler tương ứng.
- Gọi service/DAO, trả `Message.success/failure`.
- Broadcast `AUCTION_SYNC` khi auction snapshot thay đổi.

`AuctionPayloadMapper`

- Chuyển `Map<String,Object>` từ network thành domain object `Auction`, `Item`, `User`, `BidTransaction`.

### `controller`

Controllers là tầng UI. Chúng không truy cập DB trực tiếp; phần lớn gọi `NetworkService` trong `FxAsync`.

| Controller | Màn hình | Vai trò |
|---|---|---|
| `HomeController` | `giaodien`, `login`, `register`, `forgot-password` | Trang chủ, login/register nhanh, reset password, render upcoming auctions. |
| `AuctionCatalogController` | `auctions.fxml` | Danh sách auction, search/filter/sort, card layout, mở detail. |
| `AssetDetailController` | `asset-detail.fxml` | Trang trung gian chi tiết tài sản, countdown, nút vào phòng đấu giá/tổng kết. |
| `AuctionDetailController` | `product-detail.fxml` | Phòng đấu giá realtime, bid, auto-bid, history, chart, countdown. |
| `AuctionSummaryController` | `auction-summary.fxml` | Tổng kết phiên, winner, final price, top bidders, chart. |
| `CreateAuctionController` | `create-auction.fxml` | Tạo/chỉnh sửa auction, dynamic fields, image upload. |
| `AuctionHistoryController` | `auction-history.fxml` | Lịch sử bid và danh sách tài sản đã đăng bán. |
| `UserProfileController` | `user-profile.fxml` | Hồ sơ user, update profile, delete account. |
| `AdminDashboardController` | `admin-dashboard.fxml` | Dashboard admin, quản lý user và auction. |
| `AuctionListController` | legacy | TableView cũ đọc từ `AuctionService`. |

### `model`

- `Entity`: base id/timestamps.
- `Auction`: aggregate root cho phiên đấu giá.
- `BidTransaction`: một lượt bid.
- `Item`: abstract item.
- `Electronics`, `Vehicle`, `Art`: item polymorphism.
- `User`, `RegisteredUser`, `Bidder`, `Seller`, `Admin`: user hierarchy.

### `util`

| Class | Vai trò |
|---|---|
| `FxAsync` | Chạy task nặng ngoài JavaFX Application Thread, callback bằng `Platform.runLater`. |
| `SceneNavigator` | Chuyển scene, load FXML, truyền data vào controller, apply theme. |
| `ThemeManager` | Singleton quản lý dark mode bằng `Preferences`. |
| `UserSession` | Static global state user đang đăng nhập. |
| `LoginStateHelper` | Cập nhật nút login/logout. |
| `AlertHelper` | Dialog info/error/confirmation. |
| `ValidationUtil` | Validate username/email/password. |
| `PriceFormatter` | Format tiền VND. |
| `AuctionImageLoader` | Cache ảnh JavaFX, tối ưu URL Cloudinary. |

---

## 6. Class analysis

### `Main`

Fields:

- `private Server embeddedServer`: server nhúng, hiện không tự start vì dòng `startEmbeddedServer()` bị comment.

Methods:

- `start(Stage)`: entry lifecycle JavaFX; warm-up network, load `giaodien.fxml`, apply theme, show window.
- `stop()`: stop server nếu có.
- `startEmbeddedServer()`: tạo `Server`, chạy trên daemon thread `auction-embedded-server`, bắt `BindException`.
- `logDatabaseConfiguration()`: in DB URL/user và trạng thái connected/disconnected.
- `main(String[])`: gọi `launch()`.

### `DBConnection`

Fields:

- `DEFAULT_URL`, `DEFAULT_USER`, `DEFAULT_PASSWORD`: fallback DB config.
- `LOCAL_ENV`: map read-only từ `.env.local`.

Methods:

- `getConnection()`: tạo connection mới bằng `DriverManager.getConnection`.
- `getConfiguredUrl/getConfiguredUser()`: phục vụ log/status.
- `getConfig(...)`: priority env → `.env.local` → system property → default.
- `loadLocalEnv/readLocalEnv`: tìm và parse `.env.local`.
- `stripQuotes`: bỏ quote quanh value.

Lifecycle: utility class, constructor private ném `IllegalStateException`.

### `UserDao`

Fields: không có state dài hạn, mỗi method tự mở connection.

Methods quan trọng:

- `usernameExists/emailExists`: query `SELECT 1`.
- `register(String...)`: hash password bằng BCrypt rồi insert role `USER`.
- `register(User)`: insert từ object, dùng passwordHash có sẵn.
- `login(username,password)`: tìm by username/email và active, check password.
- `checkPassword`: hỗ trợ cả BCrypt (`$2a$`, `$2b$`, `$2y$`) và legacy `String.valueOf(hashCode())`.
- `findById/findByUsername/findByEmail/findAll`: đọc user.
- `mapRowToUser`: tạo subclass theo role, set full name, active, deposit balance.
- `updateProfile/deleteAccount/updatePassword/setUserActive`: update DB.

Side effects: ghi DB, in stack trace khi lỗi SQL.

### `AuctionDao`

Fields:

- `ANTI_SNIPING_WINDOW_MINUTES = 2`, `ANTI_SNIPING_EXTENSION_MINUTES = 2`.
- `userDao`: mapper user từ row.
- `stateSyncLock`, `lastStateSyncAtMillis`: throttle sync trạng thái tối thiểu 1 giây/lần.

Methods:

- `findAllAuctions()`: gọi sync state, join `auctions/items/users`, map auction, load bid history.
- `synchronizeAuctionStates()`: transaction cập nhật finished/active/status theo thời gian và trừ tiền người thắng cho phiên vừa hết hạn.
- `createAuction(item,seller,bidStep)`: transaction insert `items`, insert `auctions`.
- `placeBid(auction,bidder,amount)`: transaction lock auction row bằng `FOR UPDATE`, validate, update price/end time/highest bidder, insert bid history, commit, update object RAM.
- `resolveEffectiveEndTime`: anti-sniping, nếu còn 0-120 giây thì cộng 2 phút.
- `deleteAuction(auctionId)`: transaction delete bid history, auction, item.
- `isAuctionStarted`: kiểm tra `now >= start_time`; mặc định true nếu lỗi để an toàn.
- `getAuctionSellerId`: phục vụ authorization seller.
- `updateAuction`: transaction update item và auction, reset current price bằng starting price.

Side effects: ghi DB; đặt bid cũng gọi `auction.placeBid` sau commit để đồng bộ object hiện tại.

### `AuctionService`

Fields:

- `static AuctionService instance`: singleton lazy.
- `auctionDao`, `userDao`: dependencies tự tạo.
- `List<Auction> auctions`: snapshot trong RAM.

Methods:

- `refreshAuctions`: nếu DB down thì snapshot rỗng; nếu DB up thì load từ DAO rồi notify observers.
- `getAllAuctions/getAllItems/getAuctionByItem/getAuctionById`: trả dữ liệu snapshot.
- `createAuction`: validate seller, price, bid step, time; tạo item bằng `ItemFactory`; gọi DAO; refresh nếu thành công.
- `placeBid`: gọi DAO; refresh nếu thành công.
- `sellerDeleteAuction/updateAuction`: enforce owner và chưa bắt đầu thông qua DAO.
- `addAuctionObserver/removeAuctionObserver`: đăng ký observer và push snapshot ngay.

### `AuthService`

Fields:

- `static final AuthService instance`.
- `UserDao userDao`.

Methods:

- `login`: null guard, ensure DB available, gọi `userDao.login`, trả `Optional<User>`.
- `register`: null guard, ensure DB available, gọi `userDao.register`.
- `isDatabaseAvailable`: delegate.
- `ensureDatabaseAvailable`: ném `IllegalStateException` nếu DB down.

### `Server`

Fields:

- `port`, `authService`, `auctionService`, `imageStorageService`, `userDao`.
- `ExecutorService executor = Executors.newCachedThreadPool()`.
- `Set<ClientSession> clientSessions = ConcurrentHashMap.newKeySet()`.
- `auctionBroadcastObserver`: observer gọi `broadcastAuctionSnapshot`.
- `resetTokens`: map email → OTP/expiry.
- `running`, `serverSocket`.

Methods:

- `start`: start image service, bind `ServerSocket`, accept client, submit mỗi client vào thread pool.
- `stop`: close server socket, stop image service, remove observer, shutdown executor.
- `handleClient`: tạo reader/writer UTF-8, gửi initial `AUCTION_SYNC`, read từng dòng JSON, parse request, call `handleRequest`, send response.
- `handleRequest`: switch theo `Message.Type`.
- `handleLogin/register`: validate và gọi auth.
- `handlePlaceBid`: refresh auctions, tìm auction by itemId, tìm bidder, kiểm tra seller/self-bid và balance, gọi service.
- `handleCreateAuction/updateAuction`: parse payload, attach image URL, parse money/time, gọi service.
- `handleGetUsers/setUserActive/deleteAuction`: kiểm tra role admin.
- `handleRequestPasswordReset/resetPassword`: tạo OTP 6 chữ số, expiry 5 phút, gửi email, update password BCrypt.
- Payload builders: `userPayload`, `auctionPayload`, `itemPayload`, `bidPayload`.
- Broadcast: `broadcastAuctionSnapshot`, `buildAuctionSyncMessage`.

### `NetworkService`

Fields:

- `DEFAULT_HOST`, `DEFAULT_PORT`: system properties `auction.server.host/port`, default `127.0.0.1:5050`.
- `sharedConnection`: reusable TCP connection.
- `pendingResponses`: requestId → future.
- `auctionUpdateListeners`: realtime UI listeners.
- `latestAuctionSnapshot`: cache snapshot mới nhất.

Methods:

- `getConnection`: synchronized lazy connect, clear pending responses khi reconnect.
- `send`: tạo `Message`, đưa future vào pending, gửi qua `ServerConnection`, chờ tối đa 15 giây.
- Public APIs: `login`, `register`, `getAuctions`, `placeBid`, `createAuction`, `updateAuction`, `sellerDeleteAuction`, `getUsers`, `setUserActive`, `deleteAuction`, `requestPasswordReset`, `resetPassword`.
- `handleIncomingMessage`: nếu `AUCTION_SYNC` thì update snapshot/listeners; nếu response thì complete future theo `requestId`.
- `ensureSuccess`: convert server failure thành `IOException`.

### `ServerConnection`

Fields:

- `Socket socket`, `BufferedReader reader`, `PrintWriter writer`.
- `Consumer<Message> messageHandler`.
- `Thread readerThread`.

Methods:

- Constructor: connect với timeout 3 giây, mở UTF-8 reader/writer, start daemon reader thread.
- `send`: synchronized write một JSON line.
- `readLoop`: read line, parse Gson, dispatch.
- `close`: close socket.

### `Message`

Fields:

- `requestId`: correlation id.
- `type`: enum request/response/broadcast.
- `payload`: `Map<String,Object>`.
- `success`: server result.
- `message`: error/detail.

Methods:

- `success/failure`: factory response.
- `toJson/fromJson`: Gson.
- Getters trả bản copy payload để giảm mutation ngoài ý muốn.

### Domain classes

`Entity`

- Fields: `id`, `createdAt`, `updatedAt`.
- Constructor sinh UUID và timestamps.
- `touch()` update `updatedAt`.

`Item`

- Fields: name, description, starting/current price, start/end time, status, sellerId, imageUrl.
- `isAuctionActive`: now nằm giữa start/end.
- `updatePrice`: chỉ update nếu newPrice lớn hơn currentPrice.
- Abstract `getCategory`, `printInfo`.

`Electronics`, `Vehicle`, `Art`

- Kế thừa `Item`.
- Thêm field riêng: brand/warranty, manufacturer/year/mileage, artist/yearCreated.

`Auction`

- Aggregate của item, seller, price, highestBidder, active/finished, bidHistory.
- `placeBid` synchronized để bảo vệ object RAM; validate open, bidder not null, bid > current, bidder không phải seller; update price, item price, add `BidTransaction`, update user stats.
- `addHistoricalBid`: dùng khi DAO hydrate history từ DB mà không chạy lại nghiệp vụ.
- `closeAuction`: set inactive/finished, add won auction cho highest bidder.

`BidTransaction`

- Gắn `Auction`, `User bidder`, `BigDecimal bidAmount`, `LocalDateTime bidTime`.

`User`

- Fields: username, fullname, email, passwordHash, balance, active, joined/won/listed ids, rating.
- `deposit/withdraw`: validate số tiền; withdraw ném `InsufficientFundsException`.
- `verifyPassword`: legacy hashCode check, chỉ còn phù hợp mock `UserService`.
- Abstract `getRole`.

`Admin`, `RegisteredUser`, `Seller`, `Bidder`

- Subclass role-specific. `Admin` có thêm managed user/auction ids và activate/deactivate helpers.

### Controller classes

`HomeController`

- Dependencies: `NetworkService`, `UserSession`, `ValidationUtil`, `FxAsync`, `ThemeManager`.
- Fields UI: login/register/forgot form, password toggle fields, user panel labels, upcoming auction container.
- Methods chính:
  - `initialize`: setup toggles, login state, clock, realtime listener, render upcoming auctions.
  - `handleLogin/handleQuickLogin`: validate username/password, gọi `networkService.login`, lưu `UserSession`.
  - `handleRegister`: validate username/email/password/confirm, gọi `networkService.register`.
  - `handleSendResetToken/handleConfirmResetPassword`: OTP reset password.
  - `renderUpcomingAuctions/createAuctionCard`: build card UI từ snapshot server.

`AuctionCatalogController`

- Dependencies: `NetworkService`, `AuctionPayloadMapper`, `AuctionUpdateListener`.
- Fields: search/category/status/sort filters, `FlowPane auctionListContainer`.
- Logic: load auctions từ latest snapshot hoặc server, filter keyword/category/status, sort, render cards, click card gọi `SceneNavigator.navigateToAssetDetail`.

`AssetDetailController`

- Fields: labels thông tin tài sản/giá/countdown, image view, action buttons.
- `setAuctionData`: nhận `Auction` từ navigator rồi render.
- `startCountdown`: dùng `Timeline` mỗi giây cập nhật remaining time.
- `configureActionButtons`: chỉ cho join khi running; summary khi finished.

`AuctionDetailController`

- Dependencies: `NetworkService`, `AutoBidStrategy`, `FxAsync`, `UserSession`.
- Fields: current auction, countdown timeline, auto-bid state, bid history list, price chart.
- `handleBid`: validate login, current auction, amount; gọi `networkService.placeBid` async.
- `handleActivateAutoBid`: parse max/step, validate step >= minimum increment, enable và gọi `maybeExecuteAutoBid`.
- `maybeExecuteAutoBid`: nếu user không dẫn đầu, strategy quyết định bid; nếu đủ balance thì gửi bid async.
- `handleAuctionsUpdated`: nhận snapshot realtime, refresh auction hiện tại, redraw UI.
- `navigateToSummary`: khi hết giờ hoặc finished thì chuyển summary.

`CreateAuctionController`

- Fields: category, common fields, category-specific fields, selected image, edit mode.
- `initialize`: setup combo category và default values.
- `setEditMode`: nhận auction cũ, pre-fill form, đổi button sang update.
- `handleChooseImage`: dùng `FileChooser`.
- `handleCreateAuction`: validate form, build attributes, attach base64 image, gọi create/update network.
- `buildAttributes`: map field riêng theo category.

`AuctionHistoryController`

- Fields: two `TableView`: `MyBidRow` và `MySaleRow`.
- `setupBidTable/setupSaleTable`: cell value/cell factory, format price/time/status/action buttons.
- `loadHistoryData`: async get auctions, map sang domain, process bids/sales của current user.
- `processBids`: gom các bid của user, tính total/active/won/amount.
- `processSales`: gom auctions do user bán, tính active/sold/revenue.
- Sale row action: detail, edit nếu chưa bắt đầu, delete nếu chưa bắt đầu.

`AdminDashboardController`

- Fields: dashboard labels, users/auctions `ObservableList`, table filters.
- `loadDashboardData`: async get users và auctions.
- `renderDashboard`: update stats and tables.
- `handleToggleSelectedUser`: gọi `setUserActive`.
- `handleDeleteSelectedAuction`: confirm rồi gọi admin delete.
- `isAdminLoggedIn`: guard role admin từ `UserSession`.

`UserProfileController`

- Fields: labels, editable full name/email, notification.
- `loadUserProfile`: render current session.
- `refreshLoggedInUser`: get latest current user từ server.
- `handleUpdateProfile`: validate email, gọi network update, update session.
- `handleDeleteAccount`: confirm, soft delete account, logout.

`AuctionSummaryController`

- Nhận `Auction` qua `setAuctionData`.
- Render final price, winner, seller, bid chart, top bidders.

`AuctionListController`

- Legacy controller đọc trực tiếp `AuctionService.getAllItems()` vào `TableView`.
- Ít phù hợp với kiến trúc TCP hiện tại vì bypass `NetworkService`.

---

## 7. Line-by-line/block-by-block cho class quan trọng

### `Main.start`

```java
logDatabaseConfiguration();
```

In DB URL/user và kiểm tra DB. Đây là diagnostic trước khi UI mở.

```java
FxAsync.run(() -> {
    NetworkService.getInstance().isServerReachable();
    return null;
}, ignored -> {}, ignored -> {});
```

Chạy ping server trên background thread. Không update UI, chỉ warm-up connection.

```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/giaodien.fxml"));
Scene scene = new Scene(loader.load());
ThemeManager.getInstance().applyTheme(scene);
```

Load FXML home. FXMLLoader tạo node tree và controller tương ứng từ `fx:controller`. Sau đó theme được áp dụng vào scene/root.

### `DBConnection.getConfig`

```text
1. System.getenv(envKey)
2. LOCAL_ENV.get(envKey)
3. System.getProperty(propertyKey)
4. defaultValue
```

Logic này giúp cùng code chạy được trong Docker, máy dev local, hoặc khi truyền `-Ddb.url=...`.

### `AuctionDao.placeBid`

```java
synchronizeAuctionStates();
```

Trước khi bid, trạng thái phiên được sync theo thời gian thực để tránh bid vào phiên đã hết hạn.

```sql
SELECT a.current_price, a.seller_id, a.finished, a.active, i.bid_step, i.end_time
FROM auctions a
JOIN items i ON i.id = a.item_id
WHERE a.id = ?
FOR UPDATE
```

`FOR UPDATE` lock row trong transaction. Nếu hai client bid cùng lúc, transaction thứ hai phải chờ transaction thứ nhất commit/rollback.

```java
if (!active || finished) rollback;
if (bidder.getId().equals(sellerId)) rollback;
if (amount.compareTo(currentPrice.add(bidStep)) < 0) rollback;
```

Ba rule nghiệp vụ:

- Phiên phải active và chưa finished.
- Seller không được tự bid.
- Amount phải lớn hơn hoặc bằng `currentPrice + bidStep`.

```java
LocalDateTime effectiveEndTime = resolveEffectiveEndTime(currentEndTime);
```

Anti-sniping. Nếu bid trong 2 phút cuối thì `end_time` được cộng thêm 2 phút.

```java
UPDATE auctions SET current_price=?, highest_bidder_id=? ...
UPDATE items SET current_price=?, end_time=? ...
INSERT INTO bid_transactions ...
conn.commit();
```

Ba thao tác ghi phải cùng commit. Nếu một bước lỗi, rollback để không có trạng thái nửa chừng.

```java
auction.placeBid(bidder, amount);
```

Sau commit DB, object RAM hiện tại được cập nhật để response tức thời nhất quán.

### `AuctionDao.synchronizeAuctionStates`

Block 1 trừ tiền winner cho phiên vừa hết hạn:

```sql
UPDATE users u
JOIN auctions a ON u.id = a.highest_bidder_id
JOIN items i ON i.id = a.item_id
SET u.balance = u.balance - a.current_price
WHERE a.finished = FALSE
  AND ? >= i.end_time
  AND a.highest_bidder_id IS NOT NULL
```

Vì filter `a.finished = FALSE`, mỗi phiên chỉ bị trừ tiền một lần trước khi block 2 đánh dấu finished.

Block 2 sync state:

```sql
SET a.active = CASE WHEN now between start/end THEN TRUE ELSE FALSE END,
    a.finished = CASE WHEN now >= end_time THEN TRUE ELSE FALSE END,
    i.status = OPEN/RUNNING/FINISHED
```

UI không phải source of truth cho trạng thái. Server/DAO tự tính từ timestamp DB.

### `Server.handleClient`

```text
accept socket
  ↓
create BufferedReader/PrintWriter UTF-8
  ↓
add ClientSession
  ↓
send initial AUCTION_SYNC
  ↓
while readLine()
  ↓
Message.fromJson(line)
  ↓
handleRequest(request)
  ↓
session.send(response)
```

Mỗi client có một task riêng trong cached thread pool. JSON protocol là mỗi message một dòng.

### `NetworkService.send`

```java
Message request = new Message(type, payload);
CompletableFuture<Message> pendingResponse = new CompletableFuture<>();
pendingResponses.put(request.getRequestId(), pendingResponse);
getConnection().send(request);
return pendingResponse.get(15, TimeUnit.SECONDS);
```

Client không đọc response trực tiếp trong thread gửi. Reader thread của `ServerConnection` nhận mọi message và gọi `handleIncomingMessage`. Nếu `requestId` khớp, future được complete.

### `HomeController.handleLogin`

Luồng block:

```text
read username/password from fields
  ↓
ValidationUtil.isUsernameValid
  ↓
FxAsync.run
  ↓
networkService.login(username,password)
  ↓
UserSession.login(user)
  ↓
updateLoginState()
```

Network chạy ngoài UI thread. Callback thành công quay lại JavaFX thread qua `Platform.runLater` trong `FxAsync`.

### `AuctionDetailController.handleBid`

```text
check logged in
  ↓
check currentAuction exists
  ↓
parse amount
  ↓
FxAsync.run networkService.placeBid(itemId, username, amount)
  ↓
server validates and writes DB
  ↓
AUCTION_SYNC broadcast
  ↓
handleAuctionsUpdated refreshes current screen
```

### `AuctionDetailController.maybeExecuteAutoBid`

```text
guard autoBidEnabled / no in-flight request / currentAuction exists
  ↓
if current user is highest bidder: wait
  ↓
AutoBidStrategy.decide(currentPrice, minimumIncrement, autoBidStep, autoBidMaximum)
  ↓
if stop: disable auto-bid and show reason
  ↓
if bid > balance: disable
  ↓
FxAsync.run placeBid(...)
  ↓
on success: maybe continue if still needed
```

### `CreateAuctionController.handleCreateAuction`

```text
read common fields
  ↓
parse money and datetime
  ↓
buildAttributes(category)
  ↓
attachImagePayload(attributes) as Base64
  ↓
if editMode: networkService.updateAuction(...)
else: networkService.createAuction(...)
  ↓
server stores image and writes DB
```

---

## 8. JavaFX UI analysis

### FXML → Controller mapping

| FXML | Controller | Màn hình |
|---|---|---|
| `giaodien.fxml` | `HomeController` | Trang chủ. |
| `login.fxml` | `HomeController` | Login form. |
| `register.fxml` | `HomeController` | Register form. |
| `forgot-password.fxml` | `HomeController` | Reset password. |
| `auctions.fxml` | `AuctionCatalogController` | Catalog auction. |
| `asset-detail.fxml` | `AssetDetailController` | Chi tiết tài sản trước khi vào phòng. |
| `product-detail.fxml` | `AuctionDetailController` | Phòng đấu giá realtime. |
| `auction-summary.fxml` | `AuctionSummaryController` | Tổng kết phiên. |
| `create-auction.fxml` | `CreateAuctionController` | Tạo/sửa phiên. |
| `auction-history.fxml` | `AuctionHistoryController` | Lịch sử bid/sale. |
| `admin-dashboard.fxml` | `AdminDashboardController` | Admin console. |
| `user-profile.fxml` | `UserProfileController` | Hồ sơ user. |
| `contact.fxml`, `news.fxml` | thường dùng navigation/static UI | Nội dung thông tin. |

### Event flow chuẩn

```text
Button Click
  ↓
@FXML handler in Controller
  ↓
Validate input
  ↓
FxAsync.run nếu có network/DB
  ↓
NetworkService request
  ↓
Server handler
  ↓
Service/DAO
  ↓
Response hoặc AUCTION_SYNC
  ↓
Controller callback
  ↓
Update UI controls
```

### Bindings, ObservableList, CellFactory

- `AdminDashboardController`: dùng `ObservableList<Map<String,Object>>` cho `usersTable` và `auctionsTable`, `FilteredList` hoặc predicate search để lọc.
- `AuctionHistoryController`: dùng `TableView<MyBidRow>` và `TableView<MySaleRow>`, custom cell factory để format `BigDecimal`, `LocalDateTime`, status/result badge và action buttons.
- `AuctionDetailController`: dùng `ListView<String>` cho bid history và `LineChart<Number,Number>` cho giá theo lượt bid.
- `AuctionCatalogController` và `HomeController`: render cards thủ công vào `FlowPane`, không dùng binding list component.

### Navigation

`SceneNavigator` là router tập trung:

- `switchScene(event, fxmlFile)` load FXML đơn giản.
- `navigateToAssetDetail(stage, auction)` load FXML rồi gọi `AssetDetailController.setAuctionData`.
- `navigateToAuctionDetailOrSummary(stage, auction)` chọn `product-detail.fxml` nếu chưa finished, `auction-summary.fxml` nếu finished.
- `navigateToEditAuction(stage, auction)` load create screen và gọi `CreateAuctionController.setEditMode`.

---

## 9. Service layer analysis

### `AuthService`

Responsibility:

- Xác thực đăng nhập/đăng ký.
- Bảo vệ thao tác auth khi DB unavailable.

Public API:

- `login(username,password)`.
- `register(user)`.
- `isDatabaseAvailable()`.

Business logic:

- Không cho login/register khi DB down.
- Không tự validate format sâu; server/controller làm validate trước.

Error handling:

- Ném `IllegalStateException` nếu DB unavailable.
- Trả `Optional.empty()` nếu login fail.

### `AuctionService`

Responsibility:

- Đóng vai trò business facade cho auction.
- Đồng bộ snapshot từ DAO.
- Quản lý observer.

Public API:

- `refreshAuctions`, `getAllAuctions`, `getAuctionByItem`, `getAuctionById`.
- `createAuction`, `placeBid`, `deleteAuction`.
- `sellerDeleteAuction`, `updateAuction`.
- `addAuctionObserver`, `removeAuctionObserver`.

Business rules:

- Price/bidStep phải > 0.
- `endTime` phải sau `startTime`.
- Seller chỉ sửa/xóa auction của chính mình.
- Seller chỉ sửa/xóa khi phiên chưa bắt đầu.

### `AutoBidStrategy`

Input:

- `currentPrice`, `minimumIncrement`, `configuredStep`, `maximumAmount`.

Logic:

- `effectiveStep = max(configuredStep, minimumIncrement)`.
- `minimumAllowed = currentPrice + minimumIncrement`.
- `preferredBid = currentPrice + effectiveStep`.
- `candidateBid = min(preferredBid, maximumAmount)`.
- Nếu candidate dưới minimumAllowed thì stop.
- Ngược lại bid candidate.

### `ImageStorageService`

Responsibility:

- Lưu ảnh upload và trả URL public.

Flow:

```text
storeImage(bytes, filename)
  ↓
if Cloudinary configured: upload and return optimized secure URL
else:
  create uploads/images
  resolve safe extension
  write UUID.ext
  return http://host:port/images/UUID.ext
```

Security:

- Normalize path và kiểm tra `target.startsWith(storageRoot)`.
- Giới hạn extension regex `\\.[a-z0-9]{1,10}`.

---

## 10. Network layer analysis

### Protocol

- Transport: TCP socket.
- Encoding: UTF-8.
- Framing: mỗi JSON message là một dòng.
- Serialization: Gson.
- Envelope: `Message`.

### Request flow

```text
UI Controller
  ↓
NetworkService public method
  ↓
send(Message.Type, payload)
  ↓
ServerConnection.send(JSON line)
  ↓
Server.handleClient readLine
  ↓
Server.handleRequest switch(type)
  ↓
Service/DAO
  ↓
Message.success/failure
```

### Response flow

```text
Server session.send(response JSON)
  ↓
ServerConnection.readLoop
  ↓
NetworkService.handleIncomingMessage
  ↓
pendingResponses[requestId].complete(message)
  ↓
NetworkService public method returns payload/domain object
  ↓
Controller callback updates UI
```

### Realtime broadcast

```text
AuctionService.refreshAuctions()
  ↓
AuctionSubject.notifyObservers()
  ↓
Server.auctionBroadcastObserver
  ↓
broadcastAuctionSnapshot()
  ↓
Message.Type.AUCTION_SYNC
  ↓
All connected clients
  ↓
NetworkService.latestAuctionSnapshot
  ↓
AuctionUpdateListener callbacks
```

### Không dùng HTTP REST

Codebase không dùng Retrofit/OkHttp/REST client cho nghiệp vụ chính. `HttpServer` trong `ImageStorageService` chỉ phục vụ ảnh local. Cloudinary SDK dùng HTTP nội bộ để upload ảnh.

---

## 11. Authentication và security

### Login flow

```text
User nhập username/password
  ↓
HomeController.handleLogin
  ↓
ValidationUtil.isUsernameValid
  ↓
NetworkService.login
  ↓
Server.handleLogin
  ↓
AuthService.login
  ↓
UserDao.login
  ↓
BCrypt.checkpw hoặc legacy hashCode check
  ↓
User payload
  ↓
UserSession.login(user)
```

### Token/JWT/OAuth

Không có JWT, OAuth hoặc session token server-side. Trạng thái đăng nhập chỉ được lưu trong client bằng `UserSession.loggedInUser`. Server tin vào username trong payload cho một số thao tác, sau đó kiểm tra role/owner bằng DB.

### Password storage

- Register mới và reset password dùng BCrypt.
- Seed data trong SQL dùng legacy hash `1216985755` cho password `"password"` theo `String.hashCode`.
- `UserDao.checkPassword` hỗ trợ cả hai dạng để tương thích dữ liệu cũ.

### Reset password

```text
REQUEST_PASSWORD_RESET(emailOrUsername)
  ↓
Server tìm user by username/email
  ↓
Sinh OTP 6 chữ số
  ↓
Lưu resetTokens[email] expiry now + 5 phút
  ↓
sendEmail async qua JavaMail

RESET_PASSWORD(emailOrUsername, token, newPassword)
  ↓
Tìm user
  ↓
Check token tồn tại, đúng, chưa hết hạn
  ↓
BCrypt.hashpw(newPassword)
  ↓
UserDao.updatePassword(email, hash)
  ↓
Remove token
```

### Logout

Logout là client-side:

```text
LoginStateHelper.handleLogout
  ↓
UserSession.logout()
  ↓
AlertHelper.showInformation
  ↓
SceneNavigator.goToHome
```

Không có server session để invalidate.

---

## 12. Data models và database mapping

### Schema

`users`

| Column | Model field | Ý nghĩa |
|---|---|---|
| `id` | `User.id` | UUID/string primary key. |
| `username` | `User.username` | Unique login name. |
| `full_name` | `User.fullname` | Tên đầy đủ. |
| `email` | `User.email` | Unique email. |
| `password` | `User.passwordHash` | BCrypt hoặc legacy hash. |
| `role` | `User.getRole()` | `ADMIN`, `USER`, `SELLER`, fallback `BIDDER`. |
| `balance` | `User.balance` | Số dư VND dạng `BIGINT`. |
| `active` | `User.active` | Soft delete/lock. |

`items`

| Column | Model field | Ý nghĩa |
|---|---|---|
| `id` | `Item.id` | Item primary key. |
| `seller_id` | `Item.sellerId` | FK user seller. |
| `name`, `description` | `Item.name/description` | Metadata hiển thị. |
| `category` | `Item.getCategory()` | `Electronics`, `Vehicle`, `Art`. |
| `starting_price`, `current_price` | `Item.startingPrice/currentPrice` | Giá. |
| `bid_step` | `Auction.minimumBidStep` | Bước giá tối thiểu. |
| `start_time`, `end_time` | `Item.startTime/endTime` | Window đấu giá. |
| `status` | `ItemStatus` | `OPEN/RUNNING/FINISHED/PAID/CANCELED`. |
| `brand`, `warranty_months` | `Electronics` | Field riêng điện tử. |
| `manufacturer`, `production_year`, `mileage` | `Vehicle` | Field riêng xe. |
| `artist`, `year_created` | `Art` | Field riêng nghệ thuật. |
| `image_url` | `Item.imageUrl` | URL ảnh. |

`auctions`

| Column | Model field | Ý nghĩa |
|---|---|---|
| `id` | `Auction.id` | Auction primary key. |
| `item_id` | `Auction.item.id` | FK unique tới item. |
| `seller_id` | `Auction.seller.id` | FK seller. |
| `highest_bidder_id` | `Auction.highestBidder.id` | FK current leader. |
| `starting_price`, `current_price` | `Auction.starting/currentPrice` | Giá trong aggregate. |
| `active`, `finished` | `Auction.active/finished` | State server-side. |

`bid_transactions`

| Column | Model field | Ý nghĩa |
|---|---|---|
| `id` | `BidTransaction.id` | Bid id. |
| `auction_id` | `BidTransaction.auction.id` | Phiên liên quan. |
| `bidder_id` | `BidTransaction.bidder.id` | Người bid. |
| `bid_amount` | `BidTransaction.bidAmount` | Số tiền bid. |
| `bid_time` | `BidTransaction.bidTime` | Thời điểm bid. |

### Network DTO/payload

Không có package `dto` riêng. DTO là `Map<String,Object>` trong `Message.payload`.

`auctionPayload` server trả:

```text
auctionId, itemId, itemName, description, category,
sellerId, sellerName,
startingPrice, currentPrice, bidStep,
active, finished, startTime, endTime,
seller, highestBidder, item, bidHistory
```

`itemPayload` có nested `attributes` tùy category.

`AuctionPayloadMapper` nhận payload này và tái dựng domain object trên client.

---

## 13. Flow analysis cho use case chính

### Login Flow

```text
User nhập tài khoản
  ↓
HomeController.handleLogin
  ↓
ValidationUtil
  ↓
FxAsync.run
  ↓
NetworkService.login
  ↓
Server.handleLogin
  ↓
AuthService.login
  ↓
UserDao.login
  ↓
users table
  ↓
Message.success(userPayload)
  ↓
UserSession.login
  ↓
update UI
```

### Load Data Flow

```text
AuctionCatalogController.renderAuctions
  ↓
Task/FxAsync background
  ↓
NetworkService.getLatestAuctionSnapshot or getAuctions
  ↓
AuctionPayloadMapper.toAuctions
  ↓
filter + sort
  ↓
FlowPane cards
```

### Create Auction Flow

```text
CreateAuctionController.handleCreateAuction
  ↓
parse/validate fields
  ↓
buildAttributes
  ↓
attachImagePayload Base64
  ↓
NetworkService.createAuction
  ↓
Server.handleCreateAuction
  ↓
ImageStorageService.storeImage
  ↓
AuctionService.createAuction
  ↓
ItemFactory.createItem
  ↓
AuctionDao.createAuction transaction
  ↓
items + auctions
  ↓
refreshAuctions + AUCTION_SYNC
```

### Place Bid Flow

```text
AuctionDetailController.handleBid
  ↓
NetworkService.placeBid(itemId, username, amount)
  ↓
Server.handlePlaceBid
  ↓
auctionService.refreshAuctions
  ↓
find auction by item id
  ↓
userDao.findByUsername
  ↓
balance/self-bid checks
  ↓
AuctionService.placeBid
  ↓
AuctionDao.placeBid transaction with FOR UPDATE
  ↓
bid_transactions insert
  ↓
refreshAuctions
  ↓
Server observer broadcasts AUCTION_SYNC
  ↓
AuctionDetailController updates price/history/chart
```

### Update Auction Flow

```text
AuctionHistoryController sale row "Sửa"
  ↓
SceneNavigator.navigateToEditAuction
  ↓
CreateAuctionController.setEditMode
  ↓
User edits form
  ↓
NetworkService.updateAuction
  ↓
Server.handleUpdateAuction
  ↓
AuctionService.updateAuction
  ↓
check seller owns auction
  ↓
check auction not started
  ↓
AuctionDao.updateAuction transaction
```

### Delete Auction Flow

Admin:

```text
AdminDashboardController.handleDeleteSelectedAuction
  ↓
NetworkService.deleteAuction(adminUsername, auctionId)
  ↓
Server checks admin role
  ↓
AuctionService.deleteAuction
  ↓
AuctionDao.deleteAuction
```

Seller:

```text
AuctionHistoryController btnDelete
  ↓
NetworkService.sellerDeleteAuction
  ↓
Server finds seller
  ↓
AuctionService.sellerDeleteAuction
  ↓
check owner + not started
  ↓
AuctionDao.deleteAuction
```

---

## 14. Sequence diagrams

### Login

```text
User -> HomeController: click Login
HomeController -> ValidationUtil: validate username
HomeController -> FxAsync: run network task
FxAsync -> NetworkService: login(username,password)
NetworkService -> ServerConnection: send LOGIN JSON
ServerConnection -> Server: TCP line
Server -> AuthService: login
AuthService -> UserDao: login
UserDao -> MySQL: SELECT users
MySQL -> UserDao: row
UserDao -> AuthService: User/null
AuthService -> Server: Optional<User>
Server -> NetworkService: Message.success/failure
NetworkService -> HomeController: User
HomeController -> UserSession: login(user)
HomeController -> UI: update panels/buttons
```

### API call generic

```text
Controller -> NetworkService: public API call
NetworkService -> pendingResponses: put requestId future
NetworkService -> ServerConnection: send JSON
ServerConnection -> Server: message
Server -> Server: handleRequest switch(type)
Server -> Service/DAO: business logic
Server -> ServerConnection: response JSON with same requestId
ServerConnection -> NetworkService: readLoop dispatch
NetworkService -> pendingResponses: complete future
NetworkService -> Controller: return result
```

### Data loading

```text
AuctionCatalogController -> NetworkService: getLatestAuctionSnapshot()
alt snapshot empty
  AuctionCatalogController -> NetworkService: getAuctions()
  NetworkService -> Server: GET_AUCTIONS
  Server -> AuctionService: getAllAuctions()
  Server -> NetworkService: auctions payload
end
AuctionCatalogController -> AuctionPayloadMapper: toAuctions
AuctionCatalogController -> UI: render cards
```

### Navigation

```text
AuctionCatalogController -> SceneNavigator: navigateToAssetDetail(stage, auction)
SceneNavigator -> FXMLLoader: load asset-detail.fxml
FXMLLoader -> AssetDetailController: create controller
SceneNavigator -> AssetDetailController: setAuctionData(auction)
AssetDetailController -> UI: populate labels/image/countdown
SceneNavigator -> Stage.Scene: setRoot(root)
ThemeManager -> Scene: applyTheme
```

### Logout

```text
User -> LoginStateHelper/HomeController: click Logout
Controller -> UserSession: logout()
Controller -> AlertHelper: showInformation
Controller -> SceneNavigator: goToHome
```

---

## 15. Call graph

### `HomeController`

```text
HomeController
├── NetworkService
│   ├── login/register/getAuctions/placeBid
│   ├── requestPasswordReset/resetPassword
│   └── getCurrentUser
├── FxAsync
├── UserSession
├── ValidationUtil
├── SceneNavigator
└── ThemeManager
```

### `AuctionDetailController`

```text
AuctionDetailController
├── NetworkService
│   ├── placeBid
│   ├── getAuctions
│   ├── addAuctionUpdateListener
│   └── removeAuctionUpdateListener
├── AutoBidStrategy
├── AuctionPayloadMapper
├── FxAsync
├── SceneNavigator
└── AuctionImageLoader
```

### `Server`

```text
Server
├── AuthService
│   └── UserDao
├── AuctionService
│   ├── AuctionDao
│   │   ├── DBConnection
│   │   ├── UserDao
│   │   └── ItemFactory
│   └── AuctionSubject
├── UserDao
├── ImageStorageService
│   ├── Cloudinary
│   └── HttpServer
└── Message
```

### `AuctionService`

```text
AuctionService
├── AuctionDao
│   ├── findAllAuctions
│   ├── createAuction
│   ├── placeBid
│   ├── updateAuction
│   └── deleteAuction
├── UserDao
├── ItemFactory
└── AuctionSubject.notifyObservers
```

---

## 16. State management

### Singleton/global state

| State | Nơi lưu | Ý nghĩa |
|---|---|---|
| Current user | `UserSession.loggedInUser` static | Session client-side hiện tại. |
| Network connection | `NetworkService.sharedConnection` | TCP connection dùng chung. |
| Pending requests | `NetworkService.pendingResponses` | Map request đang chờ response. |
| Latest auction snapshot | `NetworkService.latestAuctionSnapshot` | Cache broadcast gần nhất. |
| Auction snapshot server-side | `AuctionService.auctions` | Snapshot RAM từ MySQL. |
| Observers | `AuctionSubject.observers` | UI/server listeners. |
| Theme | `ThemeManager.darkMode` + `Preferences` | Dark/light mode. |
| Image cache | `AuctionImageLoader.CACHE` | LRU cache JavaFX `Image`. |
| Reset tokens | `Server.resetTokens` | OTP reset password 5 phút. |

### Source of truth

- Users/items/auctions/bids: MySQL.
- AuctionService snapshot: cache RAM, phải refresh.
- UI state: chỉ là view state.
- Login session: chỉ client-side, không có server token.

---

## 17. Concurrency analysis

### JavaFX UI thread

Tất cả update UI phải chạy trên JavaFX Application Thread. Code dùng:

- `FxAsync`: background executor rồi `Platform.runLater`.
- `Platform.runLater` trực tiếp trong auction update listeners.
- `Timeline`: countdown tick chạy callback trên FX thread.

### Background/network threads

| Component | Threading |
|---|---|
| `FxAsync.EXECUTOR` | Cached daemon thread pool tên `fx-async-worker`. |
| `Server` | `Executors.newCachedThreadPool`, mỗi client một task. |
| `ServerConnection` | Daemon reader thread `auction-network-reader`. |
| `ImageStorageService` local HTTP | `HttpServer` executor default nếu local. |
| `sendEmail` | Thread riêng `email-sender`. |
| `AuctionDetailController` | `Timeline` cho countdown, network qua `FxAsync`. |

### Thread safety

- `AuctionDao.placeBid` dùng DB transaction + `FOR UPDATE` để chống race condition bid.
- `Auction.placeBid` synchronized cho object RAM.
- `AuctionService` public methods phần lớn synchronized để bảo vệ snapshot.
- `AuctionSubject` dùng `CopyOnWriteArrayList`.
- `NetworkService.pendingResponses` dùng `ConcurrentHashMap`.
- `NetworkService.auctionUpdateListeners` dùng concurrent set.
- `AuctionImageLoader.CACHE` dùng synchronized block.

### Nguy cơ

- `Client` legacy dùng cùng `executor` cho listener và async request, đồng thời có `sendRequest` blocking đọc `input`; class này không nên dùng chung với protocol hiện tại.
- `UserSession.loggedInUser` static không volatile/synchronized; thực tế chủ yếu dùng FX thread, nhưng background callback có thể set qua FX thread do `FxAsync`.
- `NetworkService.notifyAuctionListeners` chỉ gọi listener với auction đầu tiên trong snapshot để giảm `runLater`; listener phải tự đọc latest snapshot nếu cần toàn bộ danh sách.

---

## 18. Error handling

### Controller

- Hiển thị lỗi bằng `AlertHelper.showError`, label notification hoặc status label.
- Network errors từ `FxAsync` đi vào `onError`.
- Form validation ném hoặc báo lỗi trước khi gọi network.

### Network

- `NetworkService.ensureSuccess` ném `IOException` nếu `Message.success=false`.
- Timeout 15 giây khi server không phản hồi.
- IOException khi connection hỏng sẽ `closeConnection`.
- `ServerConnection.readLoop` bắt lỗi parse JSON và log stderr.

### Server

- `handleRequest` bao toàn bộ switch trong try/catch, convert exception thành `Message.failure`.
- Handler cụ thể trả failure message rõ hơn cho validation/auth.
- `handleClient` bắt exception per client, remove session finally.

### DAO

- Dùng try-with-resources đóng connection/statement/resultset.
- Transaction methods rollback khi exception.
- Nhiều catch đang `e.printStackTrace()` và return false/null; caller chuyển thành failure message.

### Custom exception

- `InsufficientFundsException`: domain exception cho withdraw trong model user.

---

## 19. Configuration analysis

### `.env.local` / `.env.local.example`

Keys:

- `DB_URL`: JDBC URL, default `jdbc:mysql://localhost:3307/auction_db`.
- `DB_USER`: default `root`.
- `DB_PASSWORD`: default `123456`.
- `CLOUDINARY_URL`: full Cloudinary URL.
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`: alternative Cloudinary config.
- `CLOUDINARY_FOLDER`: default `auction-items`.
- `SERVER_PORT`: documented, nhưng code server dùng system property `auction.server.port`; Docker có thể truyền env riêng.

### JVM system properties

- `db.url`, `db.user`, `db.password`: đọc bởi `DBConnection`.
- `auction.server.host`, `auction.server.port`: đọc bởi `NetworkService`.
- `auction.media.port`, `auction.media.dir`, `auction.public.host`: đọc bởi `ImageStorageService`.
- `cloudinary.url`, `cloudinary.cloudName`, `cloudinary.apiKey`, `cloudinary.apiSecret`, `cloudinary.folder`: đọc bởi `ImageStorageService`.

### `smtp.properties`

- `smtp.host`, `smtp.port`, `smtp.auth`, `smtp.starttls`.
- `smtp.user`, `smtp.password`.
- Được `Server.sendEmail` đọc từ classpath. Lưu ý file hiện chứa credential thật, nên nên chuyển sang env/secret.

### Database init

`src/main/resources/Database/init/01_schema.sql` tạo:

- `users`.
- `items`.
- `auctions`.
- `bid_transactions`.

Và seed `admin`, `user1`, `user2` với password legacy.

---

## 20. Design pattern analysis

| Pattern | Vị trí | Giải thích |
|---|---|---|
| MVC | FXML + Controller + Service/DAO/Model | FXML là View, Controller xử lý event, Model/Service/DAO xử lý domain/data. |
| Singleton | `AuthService`, `AuctionService`, `NetworkService`, `ThemeManager`, `UserService` | Dùng instance toàn cục cho service/state chung. |
| Factory | `ItemFactory` | Tạo `Electronics`, `Vehicle`, `Art` theo category. |
| Observer | `AuctionObserver`, `AuctionSubject`, `AuctionService`, `Server` | AuctionService notify; Server broadcast snapshot. |
| Repository/DAO | `UserDao`, `AuctionDao` | Tách SQL khỏi service/controller. |
| Strategy | `AutoBidStrategy` | Tách thuật toán auto-bid khỏi controller. |
| Adapter/Mapper | `AuctionPayloadMapper` | Chuyển Map payload thành domain object. |
| Facade | `NetworkService`, `AuctionService` | Controller gọi API đơn giản thay vì biết socket/DAO chi tiết. |

---

## 21. Code smell và đề xuất cải thiện

### Coupling

- Controller phụ thuộc mạnh vào `NetworkService` singleton và `UserSession` static. Điều này nhanh cho đồ án nhưng khó test controller.
- `Server` vừa dispatch protocol, vừa validate, vừa gửi email, vừa build payload. Nên tách thành handler/service riêng khi dự án lớn hơn.

Đề xuất:

- Tạo `AuthControllerService`, `AuctionCommandHandler`, `AdminHandler`.
- Tạo interface cho `NetworkClient` để mock trong UI tests.

### DTO chưa rõ ràng

Payload dùng `Map<String,Object>` ở cả client/server. Ưu điểm là nhanh, nhược điểm là dễ sai key/runtime cast.

Đề xuất:

- Tạo DTO records: `AuctionDto`, `ItemDto`, `UserDto`, `BidDto`, `CreateAuctionRequest`.
- Gson serialize DTO thay vì Map.

### Security

- Không có server-side session token. Client gửi username trong payload; server kiểm tra role/owner bằng DB ở một số flow, nhưng auth identity vẫn yếu.
- `smtp.properties` chứa credential trong resource.
- Seed password legacy hashCode vẫn được hỗ trợ.

Đề xuất:

- Thêm login token/session id server-side hoặc JWT.
- Yêu cầu mọi request nhạy cảm có token.
- Chuyển SMTP credential sang env/secret.
- Migration toàn bộ password legacy sang BCrypt khi user login thành công.

### Transaction/payment logic

- `synchronizeAuctionStates` trừ tiền winner khi phiên hết hạn, nhưng không ghi ledger/payment transaction riêng.
- Chưa thấy rule hoàn tiền/hold tiền cho outbid; chỉ check balance lúc bid.

Đề xuất:

- Tạo bảng `wallet_transactions`.
- Có cơ chế reserve/hold bid amount hoặc settlement rõ ràng.
- Đảm bảo không trừ balance âm nếu user đã thay đổi balance giữa lúc bid và lúc finish.

### UI lifecycle

- Một số controller đăng ký listener và remove khi scene/window đổi, nhưng cần audit kỹ mọi navigation để tránh listener leak.
- Card rendering thủ công nhiều code lặp giữa Home/Catalog.

Đề xuất:

- Tạo component/helper `AuctionCardFactory`.
- Chuẩn hóa lifecycle remove listener qua interface `DisposableController`.

### Large classes

- `HomeController` rất lớn, xử lý home, login, register, forgot password, upcoming auctions.
- `Server` rất lớn, nhiều responsibility.
- `AuctionDetailController` chứa cả realtime UI, bid, auto-bid, chart, countdown.

Đề xuất:

- Chia `HomeController` thành `HomeController`, `LoginController`, `RegisterController`, `ForgotPasswordController`.
- Chia `Server` thành route handlers.
- Tách `AutoBidControllerState` hoặc `BidPanelPresenter`.

### Consistency

- Có `Client` legacy dùng object streams, trong khi `NetworkService/ServerConnection/Server` dùng JSON line protocol. Điều này gây hiểu nhầm.
- Có `AuctionListController` legacy đọc trực tiếp `AuctionService`, bypass server.
- `UserService` mock còn tồn tại song song với `AuthService/UserDao`.

Đề xuất:

- Đánh dấu rõ `legacy` bằng package hoặc xóa nếu không dùng.
- Không để hai protocol client cùng tồn tại nếu không cần.

### Encoding/comment quality

- Một số comment/source hiển thị mojibake trong terminal, có thể do mismatch encoding trước đó.

Đề xuất:

- Chuẩn hóa toàn bộ file UTF-8.
- Thêm `.editorconfig` với `charset = utf-8`.

### JavaFX best practices

- Controller tạo UI node thủ công nhiều, khó style/test.
- Một số text/icon dùng emoji trực tiếp trong FXML, có thể render khác nhau theo OS.

Đề xuất:

- Dùng CSS classes ổn định, icon library nếu có.
- Tách repeated UI thành custom controls hoặc FXML includes.

---

## Tóm tắt kiến trúc trong một sơ đồ

```text
FXML Views
  ↓ fx:controller / onAction
Controllers
  ↓ validate + FxAsync
NetworkService
  ↓ ServerConnection TCP JSON
Server
  ↓ switch Message.Type
AuthService / AuctionService / ImageStorageService
  ↓
UserDao / AuctionDao / Cloudinary or Local HTTP
  ↓
MySQL / uploads/images

Realtime:
AuctionDao write
  ↓
AuctionService.refreshAuctions
  ↓
AuctionSubject.notifyObservers
  ↓
Server.broadcastAuctionSnapshot
  ↓
NetworkService.latestAuctionSnapshot
  ↓
AuctionUpdateListener in controllers
```

