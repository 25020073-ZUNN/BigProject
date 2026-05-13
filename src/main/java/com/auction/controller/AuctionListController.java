package com.auction.controller;

import com.auction.model.item.Item;
import com.auction.service.AuctionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * AuctionListController - Bộ điều khiển cho giao diện danh sách các phiên đấu
 * giá.
 * Quản lý việc hiển thị dữ liệu lên TableView, tìm kiếm sản phẩm và chuyển
 * hướng sang chi tiết.
 */
public class AuctionListController implements Initializable {

    @FXML
    private TextField txtSearch; // Ô nhập liệu tìm kiếm sản phẩm

    @FXML
    private TableView<Item> tableProducts; // Bảng hiển thị danh sách sản phẩm
    @FXML
    private TableColumn<Item, String> colId; // Cột ID
    @FXML
    private TableColumn<Item, String> colName; // Cột tên sản phẩm
    @FXML
    private TableColumn<Item, String> colPrice; // Cột giá hiện tại
    @FXML
    private TableColumn<Item, String> colSeller; // Cột ID người bán
    @FXML
    private TableColumn<Item, String> colTime; // Cột thời gian kết thúc

    private ObservableList<Item> productList; // Danh sách dữ liệu quan sát được (JavaFX)
    private final AuctionService auctionService = AuctionService.getInstance(); // Service lấy dữ liệu

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Ánh xạ các cột trong TableView với các thuộc tính tương ứng của lớp Item
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerId"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // Lấy toàn bộ danh sách mặt hàng từ AuctionService
        productList = FXCollections.observableArrayList(auctionService.getAllItems());
        tableProducts.setItems(productList);

        // Xử lý sự kiện khi click vào dòng trong bảng
        tableProducts.setOnMouseClicked(event -> {
            // Nếu nhấn đúp chuột (Double click) và có dòng được chọn
            if (event.getClickCount() == 2 && tableProducts.getSelectionModel().getSelectedItem() != null) {
                Item selectedItem = tableProducts.getSelectionModel().getSelectedItem();
                openDetailWindow(selectedItem); // Mở cửa sổ chi tiết sản phẩm
            }
        });
    }

    /**
     * Mở cửa sổ chi tiết cho một mặt hàng cụ thể.
     */
    private void openDetailWindow(Item item) {
        try {
            // Nạp file FXML cho giao diện chi tiết
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/product-detail.fxml"));
            javafx.scene.Parent root = loader.load();

            // Lấy Controller của màn hình chi tiết và truyền dữ liệu mặt hàng sang
            AuctionDetailController detailController = loader.getController();
            detailController.setItemData(item);

            // Tạo và hiển thị Stage (Cửa sổ) mới
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Chi tiết sản phẩm: " + item.getName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace(); // Ghi nhận lỗi nếu không mở được cửa sổ
        }
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn tìm kiếm hoặc Enter trong ô Search.
     */
    @FXML
    public void handleSearch(ActionEvent event) {
        String keyword = txtSearch.getText().toLowerCase(); // Lấy từ khóa và chuyển thành chữ thường

        // Nếu ô tìm kiếm trống, hiển thị lại toàn bộ danh sách ban đầu
        if (keyword.isEmpty()) {
            tableProducts.setItems(productList);
            return;
        }

        // Lọc danh sách sản phẩm theo tên
        ObservableList<Item> filteredList = FXCollections.observableArrayList();
        for (Item item : productList) {
            if (item.getName().toLowerCase().contains(keyword)) {
                filteredList.add(item);
            }
        }
        tableProducts.setItems(filteredList); // Cập nhật bảng với danh sách đã lọc
    }
}
