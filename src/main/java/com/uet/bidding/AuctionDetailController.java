package com.uet.bidding;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AuctionDetailController {
    @FXML private Label lblName;
    @FXML private Label lblPrice;
    @FXML private Label lblSeller;
    @FXML private Label lblTime;

    // Khai báo thêm 2 món đồ mới nối từ giao diện sang
    @FXML private TextField txtBidAmount;
    @FXML private javafx.scene.image.ImageView imgProduct;
    @FXML private Label lblMessage;

    public void setProductData(AuctionListController.Product product) {
        // Thêm dòng này vào hàm setProductData
        imgProduct.setImage(new javafx.scene.image.Image("https://via.placeholder.com/150"));

        lblName.setText(product.getName());
        lblPrice.setText("Giá hiện tại: " + product.getPrice());
        lblSeller.setText("Đăng bởi: " + product.getSeller());
        lblTime.setText("Thời gian còn lại: " + product.getTime());
    }

    // Nâng cấp hàm xử lý khi bấm nút "Ra giá ngay"
    @FXML
    public void handleBid(ActionEvent event) {
        // Lấy số tiền người dùng nhập vào
        String bidMoney = txtBidAmount.getText();

        // Kiểm tra xem người dùng có nhập gì chưa
        if (bidMoney.isEmpty()) {
            lblMessage.setStyle("-fx-text-fill: red;"); // Đổi màu chữ thành đỏ
            lblMessage.setText("❌ Bạn chưa nhập số tiền!");
            return;
        }

        // Nếu đã nhập tiền thì báo thành công!
        lblMessage.setStyle("-fx-text-fill: #28a745;"); // Đổi màu chữ thành xanh lá
        lblMessage.setText("🎉 Chúc mừng! Bạn đã ra giá: " + bidMoney + " VNĐ");

        // (Tùy chọn) Xóa trắng ô nhập để người dùng có thể nhập giá khác
        txtBidAmount.clear();
    }
}