package com.auction.network.client;

import com.auction.factory.ItemFactory;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lớp AuctionPayloadMapper chịu trách nhiệm chuyển đổi (mapping) các dữ liệu thô (payload) 
 * nhận được từ mạng (dưới dạng Map) sang các đối tượng domain của ứng dụng như Auction, User, Item, v.v.
 */
public final class AuctionPayloadMapper {

    // Định dạng thời gian mặc định cho việc parse dữ liệu ngày tháng
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Constructor riêng tư để ngăn việc khởi tạo đối tượng vì đây là lớp tiện ích (utility class)
    private AuctionPayloadMapper() {
    }

    /**
     * Chuyển đổi danh sách các payload thành danh sách các đối tượng Auction.
     * 
     * @param payloads Danh sách các Map chứa dữ liệu đấu giá.
     * @return Danh sách các đối tượng Auction đã được chuyển đổi.
     */
    public static List<Auction> toAuctions(List<Map<String, Object>> payloads) {
        List<Auction> auctions = new ArrayList<>();
        for (Map<String, Object> payload : payloads) {
            try {
                Auction auction = toAuction(payload);
                if (auction != null) {
                    auctions.add(auction);
                }
            } catch (Exception e) {
                // Ghi nhận lỗi nếu một payload không thể parse được
                System.err.println("[MAPPER] Lỗi parse auction: " + payload);
                e.printStackTrace();
            }
        }
        return auctions;
    }

    /**
     * Chuyển đổi một payload đơn lẻ thành một đối tượng Auction.
     * 
     * @param payload Map chứa dữ liệu của một cuộc đấu giá.
     * @return Đối tượng Auction hoặc null nếu payload không hợp lệ.
     */
    @SuppressWarnings("unchecked")
    public static Auction toAuction(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        // Lấy thông tin Item từ payload
        Map<String, Object> itemPayload = payload.get("item") instanceof Map<?, ?> rawItem
                ? (Map<String, Object>) rawItem
                : payload;
        
        // Lấy thông tin Seller từ payload hoặc tạo mặc định nếu không có
        Map<String, Object> sellerPayload = payload.get("seller") instanceof Map<?, ?> rawSeller
                ? (Map<String, Object>) rawSeller
                : Map.of(
                "id", payload.getOrDefault("sellerId", ""),
                "username", payload.getOrDefault("sellerName", ""),
                "fullName", payload.getOrDefault("sellerName", ""),
                "email", payload.getOrDefault("sellerName", "seller") + "@example.com",
                "role", "SELLER",
                "balance", 0L,
                "active", true
        );

        User seller = toUser(sellerPayload);
        Item item = toItem(itemPayload);
        if (seller == null || item == null) {
            return null;
        }

        // Khởi tạo đối tượng Auction
        Auction auction = new Auction(item, seller, parseAmount(payload.get("startingPrice")));
        auction.setId(stringValue(payload.get("auctionId")));
        auction.setCurrentPrice(parseAmount(payload.get("currentPrice")));
        auction.setMinimumBidStep(parseAmount(payload.get("bidStep")));
        auction.setActive(Boolean.parseBoolean(String.valueOf(payload.getOrDefault("active", true))));
        auction.setFinished(Boolean.parseBoolean(String.valueOf(payload.getOrDefault("finished", false))));

        // Parse thông tin người trả giá cao nhất hiện tại nếu có
        if (payload.get("highestBidder") instanceof Map<?, ?> rawHighestBidder) {
            auction.setHighestBidder(toUser((Map<String, Object>) rawHighestBidder));
        }

        // Parse lịch sử trả giá
        if (payload.get("bidHistory") instanceof List<?> rawHistory) {
            for (Object entry : rawHistory) {
                if (entry instanceof Map<?, ?> rawBid) {
                    BidTransaction transaction = toBidTransaction(auction, (Map<String, Object>) rawBid);
                    if (transaction != null) {
                        auction.addHistoricalBid(transaction);
                    }
                }
            }
        }

        return auction;
    }

    /**
     * Chuyển đổi payload thành đối tượng Item sử dụng ItemFactory.
     * 
     * @param payload Map chứa dữ liệu sản phẩm.
     * @return Đối tượng Item.
     */
    @SuppressWarnings("unchecked")
    private static Item toItem(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        String category = stringValue(payload.get("category"));
        Item item = ItemFactory.createItem(
                category,
                stringValue(payload.get("name")),
                stringValue(payload.get("description")),
                parseAmount(payload.get("startingPrice")),
                parseDateTime(payload.get("startTime")),
                parseDateTime(payload.get("endTime")),
                stringValue(payload.get("sellerId")),
                payload.get("attributes") instanceof Map<?, ?> rawAttributes
                        ? (Map<String, Object>) rawAttributes
                        : Map.of()
        );

        item.setId(stringValue(payload.get("id")));
        item.setCurrentPrice(parseAmount(payload.get("currentPrice")));
        item.setImageUrl(stringValue(payload.get("imageUrl")));
        return item;
    }

    /**
     * Chuyển đổi payload thành một giao dịch trả giá (BidTransaction).
     * 
     * @param auction Cuộc đấu giá liên quan.
     * @param payload Map chứa dữ liệu giao dịch.
     * @return Đối tượng BidTransaction.
     */
    private static BidTransaction toBidTransaction(Auction auction, Map<String, Object> payload) {
        User bidder = null;
        if (payload.get("bidder") instanceof Map<?, ?> rawBidder) {
            bidder = toUser(castMap(rawBidder));
        }
        if (bidder == null) {
            return null;
        }

        BidTransaction transaction = new BidTransaction(auction, bidder, parseAmount(payload.get("bidAmount")));
        transaction.setId(stringValue(payload.get("id")));
        transaction.setBidTime(parseDateTime(payload.get("bidTime")));
        return transaction;
    }

    /**
     * Chuyển đổi payload thành đối tượng User (Admin, Seller, hoặc Bidder).
     * 
     * @param payload Map chứa dữ liệu người dùng.
     * @return Đối tượng User cụ thể dựa trên vai trò (role).
     */
    private static User toUser(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        String username = stringValue(payload.get("username"));
        String fullName = stringValue(payload.getOrDefault("fullName", username));
        String email = stringValue(payload.getOrDefault("email", username + "@example.com"));
        String role = stringValue(payload.getOrDefault("role", "BIDDER"));

        User user;
        // Phân loại người dùng dựa trên vai trò
        if ("ADMIN".equalsIgnoreCase(role)) {
            user = new Admin(username, email, "", "STANDARD");
        } else if ("SELLER".equalsIgnoreCase(role)) {
            user = new Seller(username, fullName, email, "");
        } else {
            user = new Bidder(username, fullName, email, "");
        }

        user.setId(stringValue(payload.get("id")));
        user.setFullname(fullName);
        user.setActive(Boolean.parseBoolean(String.valueOf(payload.getOrDefault("active", true))));

        // Xử lý số dư tài khoản nếu có
        long balance = Long.parseLong(String.valueOf(payload.getOrDefault("balance", 0L)));
        if (balance > 0) {
            user.deposit(balance);
        }
        return user;
    }

    /**
     * Chuyển đổi giá trị Object sang BigDecimal cho các trường tiền tệ/giá cả.
     */
    private static BigDecimal parseAmount(Object value) {
        return new BigDecimal(String.valueOf(value == null ? "0" : value));
    }

    /**
     * Chuyển đổi giá trị Object sang LocalDateTime dựa trên định dạng ISO.
     */
    private static LocalDateTime parseDateTime(Object value) {
        return LocalDateTime.parse(String.valueOf(value), DATE_FORMATTER);
    }

    /**
     * Chuyển đổi giá trị Object sang String, trả về chuỗi rỗng nếu là null.
     */
    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * Ép kiểu Object sang Map<String, Object>.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object rawMap) {
        return (Map<String, Object>) rawMap;
    }
}
