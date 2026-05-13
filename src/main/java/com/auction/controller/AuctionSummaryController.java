package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.application.Platform;

import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;

import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.util.SceneNavigator;
import com.auction.util.LoginStateHelper;
import com.auction.util.PriceFormatter;

public class AuctionSummaryController {

    @FXML private Button loginButton;
    @FXML private Label lblName;
    @FXML private Label lblFinalPrice;
    @FXML private Label lblWinner;
    @FXML private Label lblSeller;
    @FXML private ListView<String> lvTopBidders;
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    private Auction auctionData;

    @FXML
    public void initialize() {
        LoginStateHelper.updateLoginButton(loginButton);
    }

    /**
     * Khởi tạo dữ liệu màn hình tổng kết từ đối tượng auction truyền vào.
     */
    public void setAuctionData(Auction data) {
        this.auctionData = data;
        Platform.runLater(this::populateData);
    }

    private void populateData() {
        if (auctionData == null) return;

        // Cập nhật thông tin cơ bản
        lblName.setText(auctionData.getItem().getName());
        lblSeller.setText(auctionData.getSeller().getUsername());
        
        String finalPriceStr = auctionData.getCurrentPrice().toPlainString();
        lblFinalPrice.setText(PriceFormatter.formatCurrency(finalPriceStr) + " VND");

        if (auctionData.getHighestBidder() != null) {
            lblWinner.setText(auctionData.getHighestBidder().getUsername());
        } else {
            lblWinner.setText("Không có ai đặt giá");
        }

        // Lấy lịch sử đặt giá
        List<BidTransaction> bidHistory = auctionData.getBidHistory();
        if (bidHistory != null && !bidHistory.isEmpty()) {
            populateChart(bidHistory);
            populateTopBidders(bidHistory);
        } else {
            lvTopBidders.getItems().add("Chưa có lượt đặt giá nào.");
        }
    }

    private void populateChart(List<BidTransaction> bidHistory) {
        priceChart.getData().clear();
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Lịch sử giá");

        // Vẽ biểu đồ dựa trên thứ tự lượt đặt giá (Bid Order)
        for (int i = 0; i < bidHistory.size(); i++) {
            BidTransaction bid = bidHistory.get(i);
            BigDecimal amount = bid.getBidAmount();
            series.getData().add(new XYChart.Data<>(i + 1, amount.doubleValue()));
        }
        priceChart.getData().add(series);
    }

    private void populateTopBidders(List<BidTransaction> bidHistory) {
        lvTopBidders.getItems().clear();

        // Lọc top 10 lượt đặt giá cao nhất
        List<BidTransaction> topBids = bidHistory.stream()
            .sorted((b1, b2) -> b2.getBidAmount().compareTo(b1.getBidAmount())) // Giảm dần
            .limit(10)
            .collect(Collectors.toList());

        for (int i = 0; i < topBids.size(); i++) {
            BidTransaction bid = topBids.get(i);
            String username = bid.getBidder() != null ? bid.getBidder().getUsername() : "Unknown";
            String amount = PriceFormatter.formatCurrency(bid.getBidAmount().toPlainString());
            // Lấy giờ đặt giá
            String time = bid.getBidTime().toString().replace("T", " ").substring(0, 19);

            String entry = String.format("Top %d: %s - %s VND\n(%s)", i + 1, username, amount, time);
            lvTopBidders.getItems().add(entry);
        }
    }

    // --- Điều hướng ---
    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToLogin(ActionEvent event) { SceneNavigator.goToLogin(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
}
