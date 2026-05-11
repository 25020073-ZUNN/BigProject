# BigProject - Giai thich chi tiet

## Tong quan

Project hien tai la mot ung dung dau gia viet bang Java, gom 4 phan chinh:

- `JavaFX` cho giao dien
- `model/service/dao` cho logic nghiep vu
- `socket network` cho client-server
- `MySQL` cho luu user

Sau refactor, toan bo code da duoc gom ve namespace `com.auction`.

Entry point chinh dang dung la:

- [Main.java](/home/hecker/BigProject/src/main/java/com/auction/Main.java:1)

Main se:

1. khoi dong embedded socket server
2. load giao dien `giaodien.fxml`
3. cho nguoi dung thao tac qua JavaFX

## Cau truc package

### `com.auction`

Day la tang UI/controller va entry point.

- [Main.java](/home/hecker/BigProject/src/main/java/com/auction/Main.java:1)
  - entry point chinh
  - start embedded server
  - mo `giaodien.fxml`

- [HomeController.java](/home/hecker/BigProject/src/main/java/com/auction/HomeController.java:1)
  - controller chinh cua nhieu FXML
  - xu ly `login`, `register`, `search`, `bid`, `switchScene`
  - hien tai da goi network that qua `NetworkService`

- [UserSession.java](/home/hecker/BigProject/src/main/java/com/auction/UserSession.java:1)
  - luu nguoi dang nhap trong memory
  - duoc UI dung de biet dang o trang thai login hay logout

- [AuctionListController.java](/home/hecker/BigProject/src/main/java/com/auction/AuctionListController.java:1)
  - controller cu cho giao dien bang (`TableView`)
  - van compile duoc
  - khong phai flow UI chinh hien tai

- [AuctionDetailController.java](/home/hecker/BigProject/src/main/java/com/auction/AuctionDetailController.java:1)
  - controller cu cho man hinh chi tiet dang bang
  - dung `AuctionService` noi bo

- [MainApp.java](/home/hecker/BigProject/src/main/java/com/auction/MainApp.java:1)
  - entry point cu
  - mo `auctions.fxml`
  - khong phai main class dang chay trong Maven

- [LoginController.java](/home/hecker/BigProject/src/main/java/com/auction/LoginController.java:1)
  - hien dang rong
  - chua duoc dung

### `com.auction.model`

Day la domain model.

- [Entity.java](/home/hecker/BigProject/src/main/java/com/auction/model/Entity.java:1)
  - lop cha cua cac doi tuong domain
  - co `id`, `createdAt`, `updatedAt`

- [Auction.java](/home/hecker/BigProject/src/main/java/com/auction/model/Auction.java:1)
  - object trung tam cua he thong dau gia
  - chua `item`, `seller`, `currentPrice`, `highestBidder`, `bidHistory`
  - `placeBid(...)` la rule quan trong nhat

- [BidTransaction.java](/home/hecker/BigProject/src/main/java/com/auction/model/BidTransaction.java:1)
  - luu moi lan dat gia

### `com.auction.model.item`

- [Item.java](/home/hecker/BigProject/src/main/java/com/auction/model/item/Item.java:1)
  - abstract class cho item
  - chua ten, mo ta, gia khoi diem, gia hien tai, start/end time, sellerId

- [Electronics.java](/home/hecker/BigProject/src/main/java/com/auction/model/item/Electronics.java:1)
  - item dien tu

- [Vehicle.java](/home/hecker/BigProject/src/main/java/com/auction/model/item/Vehicle.java:1)
  - item phuong tien

- [Art.java](/home/hecker/BigProject/src/main/java/com/auction/model/item/Art.java:1)
  - item nghe thuat

- [ItemStatus.java](/home/hecker/BigProject/src/main/java/com/auction/model/item/ItemStatus.java:1)
  - enum trang thai item

### `com.auction.model.user`

- [User.java](/home/hecker/BigProject/src/main/java/com/auction/model/user/User.java:1)
  - abstract class cha cho user
  - co `username`, `fullname`, `email`, `passwordHash`, `balance`, `active`
  - co `deposit`, `withdraw`, `verifyPassword`

- [Bidder.java](/home/hecker/BigProject/src/main/java/com/auction/model/user/Bidder.java:1)
  - nguoi dat gia

- [Seller.java](/home/hecker/BigProject/src/main/java/com/auction/model/user/Seller.java:1)
  - nguoi ban

- [Admin.java](/home/hecker/BigProject/src/main/java/com/auction/model/user/Admin.java:1)
  - quan tri vien

- [InsufficientFundsException.java](/home/hecker/BigProject/src/main/java/com/auction/model/user/InsufficientFundsException.java:1)
  - exception custom khi rut qua so du

### `com.auction.factory`

- [ItemFactory.java](/home/hecker/BigProject/src/main/java/com/auction/factory/ItemFactory.java:1)
  - tao item theo loai
  - co `createItem`, `createElectronics`, `createVehicle`, `createArt`

### `com.auction.service`

- [AuctionService.java](/home/hecker/BigProject/src/main/java/com/auction/service/AuctionService.java:1)
  - quan ly auction in-memory
  - load sample data
  - tim auction, dat gia

- [UserService.java](/home/hecker/BigProject/src/main/java/com/auction/service/UserService.java:1)
  - auth/register bang memory
  - fallback khi DB khong san sang

- [AuthService.java](/home/hecker/BigProject/src/main/java/com/auction/service/AuthService.java:1)
  - auth thong nhat
  - uu tien DB qua `UserDao`
  - fallback ve `UserService`

- [NetworkService.java](/home/hecker/BigProject/src/main/java/com/auction/service/NetworkService.java:1)
  - client-side service de UI goi server socket
  - co `login`, `register`, `getAuctions`, `placeBid`, `ping`

- [AuctionManager.java](/home/hecker/BigProject/src/main/java/com/auction/service/AuctionManager.java:1)
  - hien dang rong

### `com.auction.dao`

- [UserDao.java](/home/hecker/BigProject/src/main/java/com/auction/dao/UserDao.java:1)
  - truy cap DB cho user
  - co `login`, `register`, `getAllUsers`, `isDatabaseAvailable`

### `com.auction.config`

- [DBConnection.java](/home/hecker/BigProject/src/main/java/com/auction/config/DBConnection.java:1)
  - tao ket noi JDBC den MySQL
  - mac dinh:
    - url: `jdbc:mysql://localhost:3307/auctions_db`
    - user: `root`
    - password: `123456`

### `com.auction.network`

- [Message.java](/home/hecker/BigProject/src/main/java/com/auction/network/Message.java:1)
  - object gui qua socket
  - co `type`, `payload`, `success`, `message`

- [Server.java](/home/hecker/BigProject/src/main/java/com/auction/network/Server.java:1)
  - backend socket
  - nhan request va tra response
  - handler hien co:
    - `LOGIN`
    - `REGISTER`
    - `GET_AUCTIONS`
    - `PLACE_BID`
    - `PING`

### `com.auction.client.network`

- [ServerConnection.java](/home/hecker/BigProject/src/main/java/com/auction/client/network/ServerConnection.java:1)
  - lop cap thap de mo socket va gui/nhan `Message`

### `com.auction.observer`

- [AuctionObserver.java](/home/hecker/BigProject/src/main/java/com/auction/observer/AuctionObserver.java:1)
- [AuctionSubject.java](/home/hecker/BigProject/src/main/java/com/auction/observer/AuctionSubject.java:1)

Hai class nay hien dang rong, du kien de lam notify/realtime sau.

### `com.auction.util`

- [UserSession.java](/home/hecker/BigProject/src/main/java/com/auction/util/UserSession.java:1)
  - hien dang rong
  - khong phai class session dang duoc UI dung

## JavaFX trong project

### FXML la gi

FXML la file XML mo ta giao dien JavaFX.

Cac file chinh:

- [giaodien.fxml](/home/hecker/BigProject/src/main/resources/giaodien.fxml:1)
- [login.fxml](/home/hecker/BigProject/src/main/resources/login.fxml:1)
- [register.fxml](/home/hecker/BigProject/src/main/resources/register.fxml:1)
- [product-detail.fxml](/home/hecker/BigProject/src/main/resources/product-detail.fxml:1)
- [sessions.fxml](/home/hecker/BigProject/src/main/resources/sessions.fxml:1)
- [auctions.fxml](/home/hecker/BigProject/src/main/resources/auctions.fxml:1)

Moi file FXML co the co:

- `fx:controller`
- `fx:id`
- `onAction`

Vi du:

- `fx:controller="com.auction.controller.HomeController"`
- `onAction="#handleLogin"`

Nghia la khi bam nut, JavaFX se goi method `handleLogin(...)` trong `HomeController`.

### Controller inject nhu the nao

Neu trong FXML co:

- `TextField fx:id="usernameField"`

va trong controller co:

```java
@FXML
private TextField usernameField;
```

thi JavaFX se inject control vao field do.

### Scene switching

Trong [HomeController.java](/home/hecker/BigProject/src/main/java/com/auction/HomeController.java:1), method `switchScene(...)` duoc dung de chuyen trang:

- `goToHome` -> `giaodien.fxml`
- `goToLogin` -> `login.fxml`
- `goToRegister` -> `register.fxml`
- `goToProductDetail` -> `product-detail.fxml`
- `goToSessions` -> `sessions.fxml`
- `goToContact` -> `contact.fxml`
- `goToNews` -> `news.fxml`

### CSS

File style:

- [style.css](/home/hecker/BigProject/src/main/resources/style.css:1)

FXML tham chieu CSS qua:

- `stylesheets="@style.css"`

## Luong chay chinh

### 1. Bat dau app

1. chay `Main`
2. `Main` start embedded `Server`
3. `Main` load `giaodien.fxml`
4. `HomeController.initialize()` duoc goi
5. UI hien ra

### 2. Login

1. user nhap username/password
2. `HomeController.handleLogin(...)`
3. goi `NetworkService.login(...)`
4. `NetworkService` dung `ServerConnection`
5. gui `Message(LOGIN, payload)`
6. `Server.handleLogin(...)`
7. `AuthService.login(...)`
8. `AuthService`:
   - neu DB san sang -> `UserDao`
   - neu khong -> `UserService`
9. server tra response
10. UI map ve `User`
11. luu vao `UserSession`

### 3. Register

1. user nhap form dang ky
2. `HomeController.handleRegister(...)`
3. goi `NetworkService.register(...)`
4. server tao `User`
5. `AuthService.register(...)`
6. server tra ket qua
7. UI thong bao thanh cong/that bai

### 4. Search

1. user nhap tu khoa
2. `HomeController.handleSearch(...)`
3. goi `NetworkService.getAuctions()`
4. server lay du lieu tu `AuctionService`
5. UI loc client-side va hien popup ket qua

Luu y:

- hien tai du lieu FXML tren card van chu yeu la text hard-code
- search da goi server that, nhung chua render lai toan bo card dong

### 5. Bid

1. user can login truoc
2. vao `product-detail.fxml`
3. nhap gia vao `bidAmountField`
4. `HomeController.handleBid(...)`
5. UI tim auction phu hop
6. gui `PLACE_BID`
7. `Server` goi `AuctionService.placeBid(...)`
8. `Auction.placeBid(...)` kiem tra rule
9. server tra gia moi
10. UI hien thong bao thanh cong

## Network flow

### Message

Client va server noi chuyen bang object [Message.java](/home/hecker/BigProject/src/main/java/com/auction/network/Message.java:1).

No chua:

- `requestId`
- `type`
- `payload`
- `success`
- `message`

### Client side

`HomeController` -> `NetworkService` -> `ServerConnection`

### Server side

`ServerConnection` gui socket -> `Server` -> `AuthService` / `AuctionService`

### Request-response hien co

- `PING`
- `LOGIN`
- `REGISTER`
- `GET_AUCTIONS`
- `PLACE_BID`

## Database

### Docker compose

File:

- [docker-compose.yml](/home/hecker/BigProject/src/main/resources/Database/docker-compose.yml:1)

MySQL config:

- port: `3307`
- database: `auctions_db`
- root password: `123456`

### Schema

File:

- [01_schema.sql](/home/hecker/BigProject/src/main/resources/Database/init/01_schema.sql:1)

File nay:

- tao bang `users`
- seed user mau

### Password hien tai

Project dang hash password bang:

- `String.valueOf(password.hashCode())`

Day chi phu hop de demo. Neu muon dung that, nen doi sang BCrypt hoac Argon2.

## Test

Hien co 2 unit test:

- [AuctionTest.java](/home/hecker/BigProject/src/test/java/com/auction/model/AuctionTest.java:1)
- [ItemFactoryTest.java](/home/hecker/BigProject/src/test/java/com/auction/factory/ItemFactoryTest.java:1)

No test:

- logic dat gia
- logic tao item

Lenh test:

```bash
mvn test
```

Lenh build:

```bash
mvn -DskipTests compile
```

Lenh chay UI:

```bash
mvn javafx:run
```

## Phan nao dang la legacy/chua dung

- `MainApp`
  - flow cu

- `AuctionListController`
  - flow cu theo `TableView`

- `AuctionDetailController`
  - flow cu theo man hinh detail bang

- `LoginController`
  - rong

- `com.auction.util.UserSession`
  - rong

- `AuctionObserver`, `AuctionSubject`
  - rong

## Diem manh hien tai

- da merge namespace sach ve `com.auction`
- UI da noi sang network that cho cac action chinh
- auth da thong nhat
- co DB config va schema user
- co test nen

## Diem chua hoan thien

- nhieu card trong FXML van hard-code
- auction/bid chua luu DB that
- observer/realtime chua lam
- controller chinh `HomeController` dang gan qua nhieu trach nhiem

## Goi y thu tu doc code

Neu muon doc de hieu nhanh nhat, nen doc theo thu tu:

1. [Main.java](/home/hecker/BigProject/src/main/java/com/auction/Main.java:1)
2. [HomeController.java](/home/hecker/BigProject/src/main/java/com/auction/HomeController.java:1)
3. [NetworkService.java](/home/hecker/BigProject/src/main/java/com/auction/service/NetworkService.java:1)
4. [Server.java](/home/hecker/BigProject/src/main/java/com/auction/network/Server.java:1)
5. [AuthService.java](/home/hecker/BigProject/src/main/java/com/auction/service/AuthService.java:1)
6. [UserDao.java](/home/hecker/BigProject/src/main/java/com/auction/dao/UserDao.java:1)
7. [AuctionService.java](/home/hecker/BigProject/src/main/java/com/auction/service/AuctionService.java:1)
8. [Auction.java](/home/hecker/BigProject/src/main/java/com/auction/model/Auction.java:1)
9. [Item.java](/home/hecker/BigProject/src/main/java/com/auction/model/item/Item.java:1)
10. [User.java](/home/hecker/BigProject/src/main/java/com/auction/model/user/User.java:1)
