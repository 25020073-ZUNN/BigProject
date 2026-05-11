package com.auction.service;

import com.auction.dao.AuctionDao;
import com.auction.dao.UserDao;
import com.auction.factory.ItemFactory;
import com.auction.model.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dịch vụ nghiệp vụ cho phiên đấu giá thật.
 *
 * Điểm thay đổi quan trọng so với phiên bản cũ:
 * - Không còn nạp dữ liệu mẫu bằng `loadSampleData()`.
 * - Dữ liệu được đọc/ghi từ MySQL thông qua `AuctionDao`.
 * - Danh sách `auctions` chỉ đóng vai trò snapshot trong RAM để phục vụ UI nhanh hơn.
 * - Sau mỗi thao tác tạo phiên hoặc đặt giá, service sẽ nạp lại từ DB để đồng bộ trạng thái.
 */
public class AuctionService {

    private static AuctionService instance;

    private final AuctionDao auctionDao;
    private final UserDao userDao;
    private List<Auction> auctions;

    private AuctionService() {
        this.auctionDao = new AuctionDao();
        this.userDao = new UserDao();
        this.auctions = new ArrayList<>();
        refreshAuctions();
    }

    public static synchronized AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    /**
     * Nạp lại toàn bộ phiên đấu giá từ DB.
     *
     * Ghi chú:
     * Hàm này là "nguồn sự thật" mới của module auction.
     * Khi DB chưa sẵn sàng, service sẽ trả danh sách rỗng thay vì bơm dữ liệu giả,
     * để người dùng biết rõ hệ thống đang thiếu dữ liệu thật chứ không bị đánh lừa bởi mock.
     */
    public synchronized void refreshAuctions() {
        if (!userDao.isDatabaseAvailable()) {
            this.auctions = new ArrayList<>();
            return;
        }
        this.auctions = new ArrayList<>(auctionDao.findAllAuctions());
    }

    public synchronized List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions);
    }

    public synchronized List<Item> getAllItems() {
        return auctions.stream()
                .map(Auction::getItem)
                .collect(Collectors.toList());
    }

    public synchronized Auction getAuctionByItem(Item item) {
        if (item == null) {
            return null;
        }
        return auctions.stream()
                .filter(a -> a.getItem().getId().equals(item.getId()))
                .findFirst()
                .orElse(null);
    }

    public synchronized Auction getAuctionById(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return null;
        }
        return auctions.stream()
                .filter(a -> auctionId.equals(a.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Tạo tài sản + phiên đấu giá mới từ dữ liệu người dùng nhập trên form.
     */
    public synchronized boolean createAuction(
            String itemType,
            String name,
            String description,
            BigDecimal startingPrice,
            BigDecimal bidStep,
            LocalDateTime startTime,
            LocalDateTime endTime,
            User seller,
            Map<String, Object> attributes
    ) {
        if (seller == null) {
            throw new IllegalArgumentException("Không tìm thấy người bán hợp lệ.");
        }
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0.");
        }
        if (bidStep == null || bidStep.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bước giá phải lớn hơn 0.");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải lớn hơn thời gian bắt đầu.");
        }

        Item item = ItemFactory.createItem(
                itemType,
                name,
                description,
                startingPrice,
                startTime,
                endTime,
                seller.getId(),
                attributes
        );

        boolean created = auctionDao.createAuction(item, seller, bidStep);
        if (created) {
            refreshAuctions();
        }
        return created;
    }

    /**
     * Đặt giá thật cho một phiên.
     *
     * Ghi chú:
     * Sau khi DAO ghi thành công xuống DB, service nạp lại snapshot trong RAM
     * để các màn hình JavaFX đang dùng chung singleton này nhìn thấy cùng trạng thái mới.
     */
    public synchronized boolean placeBid(Auction auction, User bidder, BigDecimal amount) {
        if (auction == null || bidder == null || amount == null) {
            return false;
        }

        boolean success = auctionDao.placeBid(auction, bidder, amount);
        if (success) {
            refreshAuctions();
        }
        return success;
    }
}
