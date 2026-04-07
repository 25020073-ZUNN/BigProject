package com.uet.bidding;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField; // Thư viện mới để dùng ô chữ
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

    // Nối với ô tìm kiếm
    @FXML
    private TextField txtSearch;

    @FXML
    private TableView<Product> tableProducts;
    @FXML
    private TableColumn<Product, String> colId;
    @FXML
    private TableColumn<Product, String> colName;
    @FXML
    private TableColumn<Product, String> colPrice;
    @FXML
    private TableColumn<Product, String> colSeller;
    @FXML
    private TableColumn<Product, String> colTime;

    private ObservableList<Product> productList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("seller"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));

        productList = FXCollections.observableArrayList(
                new Product("SP001", "Điện thoại iPhone 15 Pro Max", "25.000.000 đ", "Nguyễn Văn A", "2 ngày 05:12:00"),
                new Product("SP002", "Laptop ASUS ROG Strix", "30.500.000 đ", "Trần Thị B", "0 ngày 12:30:00"),
                new Product("SP003", "Đồng hồ Rolex Submariner", "150.000.000 đ", "Lê Văn C", "5 ngày 01:15:00"),
                new Product("SP004", "Mô hình Gundam mạ vàng", "5.200.000 đ", "Phạm D", "1 ngày 08:00:00")
        );

        tableProducts.setItems(productList);
        // --- TÍNH NĂNG MỚI: Lắng nghe cú click chuột của người dùng ---
        tableProducts.setOnMouseClicked(event -> {
            // Kiểm tra xem người dùng có bấm đúp chuột (2 lần) và có đang chọn 1 dòng nào không
            if (event.getClickCount() == 2 && tableProducts.getSelectionModel().getSelectedItem() != null) {

                // Lấy ra món đồ đang được chọn
                Product selectedProduct = tableProducts.getSelectionModel().getSelectedItem();

                try {
                    // 1. Tải bản vẽ của màn hình Chi tiết
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/auction-detail.fxml"));
                    javafx.scene.Parent root = loader.load();

                    // 2. Tìm ông quản lý của màn hình Chi tiết để nhét dữ liệu món đồ vào
                    AuctionDetailController detailController = loader.getController();
                    detailController.setProductData(selectedProduct);

                    // 3. Hiển thị cửa sổ mới lên
                    javafx.stage.Stage stage = new javafx.stage.Stage();
                    stage.setTitle("Chi tiết sản phẩm");
                    stage.setScene(new javafx.scene.Scene(root, 400, 400));
                    stage.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // --- PHÉP THUẬT TÌM KIẾM Ở ĐÂY ---
    @FXML
    public void handleSearch(ActionEvent event) {
        // 1. Lấy chữ người dùng nhập vào (chuyển thành chữ thường để dễ so sánh)
        String keyword = txtSearch.getText().toLowerCase();

        // 2. Nếu người dùng xóa hết chữ trong ô, hiển thị lại toàn bộ danh sách
        if (keyword.isEmpty()) {
            tableProducts.setItems(productList);
            return;
        }

        // 3. Tạo một giỏ hàng trống để đựng kết quả tìm được
        ObservableList<Product> filteredList = FXCollections.observableArrayList();

        // 4. Lục tung kho hàng lên
        for (Product product : productList) {
            // Nếu tên sản phẩm có chứa chữ người dùng gõ vào
            if (product.getName().toLowerCase().contains(keyword)) {
                filteredList.add(product); // Ném vào giỏ hàng kết quả
            }
        }

        // 5. Cập nhật cái bảng bằng giỏ hàng kết quả vừa tìm được
        tableProducts.setItems(filteredList);
    }

    public static class Product {
        private String id;
        private String name;
        private String price;
        private String seller;
        private String time;

        public Product(String id, String name, String price, String seller, String time) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.seller = seller;
            this.time = time;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPrice() { return price; }
        public String getSeller() { return seller; }
        public String getTime() { return time; }
    }
}