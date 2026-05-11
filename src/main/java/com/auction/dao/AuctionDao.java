package com.auction.dao;

import com.auction.config.DBConnection;
import com.auction.factory.ItemFactory;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.user.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO phụ trách toàn bộ thao tác đọc/ghi dữ liệu đấu giá thật.
 *
 * Ghi chú quan trọng:
 * - Lớp này là cầu nối giữa dữ liệu quan hệ trong MySQL và object Java của hệ thống.
 * - Sau khi bỏ dữ liệu mẫu trong RAM, mọi phiên đấu giá "thật" phải đi qua DAO này.
 * - Các thao tác quan trọng như tạo phiên hoặc đặt giá đều được bọc transaction
 *   để tránh trạng thái nửa chừng: ví dụ đã tăng giá trong bảng `auctions`
 *   nhưng lại chưa ghi lịch sử vào `bid_transactions`.
 */
public class AuctionDao {

    private final UserDao userDao = new UserDao();

    /**
     * Lấy toàn bộ phiên đấu giá và dựng lại object domain đầy đủ.
     */
    public List<Auction> findAllAuctions() {
        synchronizeAuctionStates();

        String sql = """
                SELECT a.id AS auction_id,
                       a.seller_id AS auction_seller_id,
                       a.highest_bidder_id,
                       a.starting_price AS auction_starting_price,
                       a.current_price AS auction_current_price,
                       a.active,
                       a.finished,
                       i.id AS item_id,
                       i.seller_id AS item_seller_id,
                       i.name,
                       i.description,
                       i.category,
                       i.starting_price AS item_starting_price,
                       i.current_price AS item_current_price,
                       i.bid_step,
                       i.start_time,
                       i.end_time,
                       i.status,
                       i.brand,
                       i.warranty_months,
                       i.manufacturer,
                       i.production_year,
                       i.mileage,
                       i.artist,
                       i.year_created
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                ORDER BY i.end_time ASC, i.created_at DESC
                """;

        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Auction auction = mapAuction(rs);
                loadBidHistory(conn, auction);
                auctions.add(auction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return auctions;
    }

    /**
     * Đồng bộ trạng thái phiên đấu giá dựa trên thời gian thực trong DB.
     *
     * Ý nghĩa:
     * - Nếu phiên chưa đến giờ bắt đầu: trạng thái item là OPEN.
     * - Nếu phiên đang trong khoảng hiệu lực: trạng thái item là RUNNING.
     * - Nếu phiên đã quá hạn: tự động đánh dấu FINISHED và khóa đặt giá.
     *
     * Đây là lớp bảo vệ cần thiết để khi ứng dụng khởi động lại sau nhiều ngày,
     * dữ liệu trong DB vẫn phản ánh đúng trạng thái thời gian thực mà không cần dựa vào UI.
     */
    private void synchronizeAuctionStates() {
        String sql = """
                UPDATE auctions a
                JOIN items i ON i.id = a.item_id
                SET a.active = CASE
                                   WHEN CURRENT_TIMESTAMP >= i.start_time AND CURRENT_TIMESTAMP < i.end_time THEN TRUE
                                   ELSE FALSE
                               END,
                    a.finished = CASE
                                     WHEN CURRENT_TIMESTAMP >= i.end_time THEN TRUE
                                     ELSE FALSE
                                 END,
                    i.status = CASE
                                   WHEN CURRENT_TIMESTAMP < i.start_time THEN 'OPEN'
                                   WHEN CURRENT_TIMESTAMP >= i.start_time AND CURRENT_TIMESTAMP < i.end_time THEN 'RUNNING'
                                   ELSE 'FINISHED'
                               END
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tạo một phiên đấu giá thật trong DB.
     *
     * Luồng ghi dữ liệu gồm 2 bước:
     * 1. Ghi thông tin tài sản vào bảng `items`
     * 2. Tạo phiên tương ứng trong bảng `auctions`
     *
     * Hai bước này bắt buộc cùng thành công hoặc cùng thất bại, vì nếu chỉ có `items`
     * mà không có `auctions` thì hệ thống sẽ xuất hiện "tài sản mồ côi".
     */
    public boolean createAuction(Item item, User seller, BigDecimal bidStep) {
        String insertItemSql = """
                INSERT INTO items(
                    id, seller_id, name, description, category,
                    starting_price, current_price, bid_step, start_time, end_time, status,
                    brand, warranty_months, manufacturer, production_year, mileage, artist, year_created
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String insertAuctionSql = """
                INSERT INTO auctions(
                    id, item_id, seller_id, highest_bidder_id,
                    starting_price, current_price, active, finished
                ) VALUES (?, ?, ?, NULL, ?, ?, TRUE, FALSE)
                """;

        Auction auction = new Auction(item, seller, item.getStartingPrice());
        auction.setMinimumBidStep(bidStep);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql);
                 PreparedStatement auctionStmt = conn.prepareStatement(insertAuctionSql)) {

                fillItemInsertStatement(itemStmt, item, bidStep);
                itemStmt.executeUpdate();

                auctionStmt.setString(1, auction.getId());
                auctionStmt.setString(2, item.getId());
                auctionStmt.setString(3, seller.getId());
                auctionStmt.setLong(4, item.getStartingPrice().longValueExact());
                auctionStmt.setLong(5, item.getCurrentPrice().longValueExact());
                auctionStmt.executeUpdate();

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Ghi một lượt đặt giá thật xuống DB.
     *
     * Hàm này khóa dòng phiên đấu giá bằng `FOR UPDATE` để giảm nguy cơ hai client
     * cùng đọc một `current_price` cũ rồi đều ghi đè lên nhau.
     */
    public boolean placeBid(Auction auction, User bidder, BigDecimal amount) {
        String lockAuctionSql = """
                SELECT a.current_price, a.seller_id, a.finished, a.active, i.bid_step
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                WHERE a.id = ?
                FOR UPDATE
                """;

        String updateAuctionSql = """
                UPDATE auctions
                SET current_price = ?, highest_bidder_id = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        String updateItemSql = """
                UPDATE items
                SET current_price = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        String insertBidSql = """
                INSERT INTO bid_transactions(id, auction_id, bidder_id, bid_amount, bid_time)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement lockStmt = conn.prepareStatement(lockAuctionSql)) {
                lockStmt.setString(1, auction.getId());

                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }

                    boolean active = rs.getBoolean("active");
                    boolean finished = rs.getBoolean("finished");
                    BigDecimal currentPrice = BigDecimal.valueOf(rs.getLong("current_price"));
                    BigDecimal bidStep = BigDecimal.valueOf(rs.getLong("bid_step"));
                    String sellerId = rs.getString("seller_id");

                    if (!active || finished) {
                        conn.rollback();
                        return false;
                    }

                    if (bidder.getId().equals(sellerId)) {
                        conn.rollback();
                        return false;
                    }

                    BigDecimal minimumAllowed = currentPrice.add(bidStep);
                    if (amount.compareTo(minimumAllowed) < 0) {
                        conn.rollback();
                        return false;
                    }

                    try (PreparedStatement updateAuctionStmt = conn.prepareStatement(updateAuctionSql);
                         PreparedStatement updateItemStmt = conn.prepareStatement(updateItemSql);
                         PreparedStatement insertBidStmt = conn.prepareStatement(insertBidSql)) {

                        updateAuctionStmt.setLong(1, amount.longValueExact());
                        updateAuctionStmt.setString(2, bidder.getId());
                        updateAuctionStmt.setString(3, auction.getId());
                        updateAuctionStmt.executeUpdate();

                        updateItemStmt.setLong(1, amount.longValueExact());
                        updateItemStmt.setString(2, auction.getItem().getId());
                        updateItemStmt.executeUpdate();

                        BidTransaction transaction = new BidTransaction(auction, bidder, amount);
                        insertBidStmt.setString(1, transaction.getId());
                        insertBidStmt.setString(2, auction.getId());
                        insertBidStmt.setString(3, bidder.getId());
                        insertBidStmt.setLong(4, amount.longValueExact());
                        insertBidStmt.setTimestamp(5, Timestamp.valueOf(transaction.getBidTime()));
                        insertBidStmt.executeUpdate();

                        conn.commit();

                        auction.setMinimumBidStep(bidStep);
                        auction.placeBid(bidder, amount);
                        return true;
                    }
                }
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loadBidHistory(Connection conn, Auction auction) throws SQLException {
        String sql = """
                SELECT id, bidder_id, bid_amount, bid_time
                FROM bid_transactions
                WHERE auction_id = ?
                ORDER BY bid_time ASC
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auction.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User bidder = userDao.findById(rs.getString("bidder_id"));
                    if (bidder == null) {
                        continue;
                    }

                    BigDecimal bidAmount = BigDecimal.valueOf(rs.getLong("bid_amount"));
                    BidTransaction transaction = new BidTransaction(auction, bidder, bidAmount);
                    transaction.setId(rs.getString("id"));
                    transaction.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());

                    auction.addHistoricalBid(transaction);
                }
            }
        }
    }

    private Auction mapAuction(ResultSet rs) throws SQLException {
        User seller = userDao.findById(rs.getString("auction_seller_id"));
        if (seller == null) {
            throw new SQLException("Không tìm thấy seller cho auction: " + rs.getString("auction_id"));
        }

        Item item = mapItem(rs);
        Auction auction = new Auction(item, seller, BigDecimal.valueOf(rs.getLong("auction_starting_price")));
        auction.setId(rs.getString("auction_id"));
        auction.setCurrentPrice(BigDecimal.valueOf(rs.getLong("auction_current_price")));
        auction.setActive(rs.getBoolean("active"));
        auction.setFinished(rs.getBoolean("finished"));
        auction.setMinimumBidStep(BigDecimal.valueOf(rs.getLong("bid_step")));

        String highestBidderId = rs.getString("highest_bidder_id");
        if (highestBidderId != null && !highestBidderId.isBlank()) {
            auction.setHighestBidder(userDao.findById(highestBidderId));
        }

        return auction;
    }

    private Item mapItem(ResultSet rs) throws SQLException {
        String category = rs.getString("category");
        String sellerId = rs.getString("item_seller_id");
        BigDecimal startingPrice = BigDecimal.valueOf(rs.getLong("item_starting_price"));
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();

        Map<String, Object> attributes = new HashMap<>();
        if ("Electronics".equalsIgnoreCase(category)) {
            attributes.put("brand", rs.getString("brand"));
            attributes.put("warrantyMonths", rs.getInt("warranty_months"));
        } else if ("Vehicle".equalsIgnoreCase(category)) {
            attributes.put("manufacturer", rs.getString("manufacturer"));
            attributes.put("year", rs.getInt("production_year"));
            attributes.put("mileage", rs.getInt("mileage"));
        } else if ("Art".equalsIgnoreCase(category)) {
            attributes.put("artist", rs.getString("artist"));
            attributes.put("yearCreated", rs.getInt("year_created"));
        }

        Item item = ItemFactory.createItem(
                category,
                rs.getString("name"),
                rs.getString("description"),
                startingPrice,
                startTime,
                endTime,
                sellerId,
                attributes
        );

        item.setId(rs.getString("item_id"));
        item.setCurrentPrice(BigDecimal.valueOf(rs.getLong("item_current_price")));
        item.setStatus(parseItemStatus(rs.getString("status")));
        return item;
    }

    private void fillItemInsertStatement(PreparedStatement stmt, Item item, BigDecimal bidStep) throws SQLException {
        stmt.setString(1, item.getId());
        stmt.setString(2, item.getSellerId());
        stmt.setString(3, item.getName());
        stmt.setString(4, item.getDescription());
        stmt.setString(5, item.getCategory());
        stmt.setLong(6, item.getStartingPrice().longValueExact());
        stmt.setLong(7, item.getCurrentPrice().longValueExact());
        stmt.setLong(8, bidStep.longValueExact());
        stmt.setTimestamp(9, Timestamp.valueOf(item.getStartTime()));
        stmt.setTimestamp(10, Timestamp.valueOf(item.getEndTime()));
        stmt.setString(11, item.getStatus().name());

        Map<String, Object> detailValues = extractItemDetails(item);
        stmt.setString(12, (String) detailValues.get("brand"));
        setNullableInteger(stmt, 13, (Integer) detailValues.get("warrantyMonths"));
        stmt.setString(14, (String) detailValues.get("manufacturer"));
        setNullableInteger(stmt, 15, (Integer) detailValues.get("productionYear"));
        setNullableInteger(stmt, 16, (Integer) detailValues.get("mileage"));
        stmt.setString(17, (String) detailValues.get("artist"));
        setNullableInteger(stmt, 18, (Integer) detailValues.get("yearCreated"));
    }

    private Map<String, Object> extractItemDetails(Item item) {
        Map<String, Object> values = new HashMap<>();
        values.put("brand", null);
        values.put("warrantyMonths", null);
        values.put("manufacturer", null);
        values.put("productionYear", null);
        values.put("mileage", null);
        values.put("artist", null);
        values.put("yearCreated", null);

        if ("Electronics".equalsIgnoreCase(item.getCategory()) && item instanceof com.auction.model.item.Electronics electronics) {
            values.put("brand", electronics.getBrand());
            values.put("warrantyMonths", electronics.getWarrantyMonths());
        } else if ("Vehicle".equalsIgnoreCase(item.getCategory()) && item instanceof com.auction.model.item.Vehicle vehicle) {
            values.put("manufacturer", vehicle.getManufacturer());
            values.put("productionYear", vehicle.getYear());
            values.put("mileage", vehicle.getMileage());
        } else if ("Art".equalsIgnoreCase(item.getCategory()) && item instanceof com.auction.model.item.Art art) {
            values.put("artist", art.getArtist());
            values.put("yearCreated", art.getYearCreated());
        }
        return values;
    }

    private ItemStatus parseItemStatus(String rawStatus) {
        try {
            return ItemStatus.valueOf(rawStatus);
        } catch (Exception ignored) {
            return ItemStatus.OPEN;
        }
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }
}
