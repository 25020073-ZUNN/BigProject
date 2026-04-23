package com.auction;

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

public class AuctionListController implements Initializable {

    @FXML
    private TextField txtSearch;

    @FXML
    private TableView<Item> tableProducts;
    @FXML
    private TableColumn<Item, String> colId;
    @FXML
    private TableColumn<Item, String> colName;
    @FXML
    private TableColumn<Item, String> colPrice;
    @FXML
    private TableColumn<Item, String> colSeller;
    @FXML
    private TableColumn<Item, String> colTime;

    private ObservableList<Item> productList;
    private final AuctionService auctionService = AuctionService.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Ánh xạ các cột với thuộc tính của class Item
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerId"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // Lấy dữ liệu từ Service thực tế
        productList = FXCollections.observableArrayList(auctionService.getAllItems());
        tableProducts.setItems(productList);

        tableProducts.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableProducts.getSelectionModel().getSelectedItem() != null) {
                Item selectedItem = tableProducts.getSelectionModel().getSelectedItem();
                openDetailWindow(selectedItem);
            }
        });
    }

    private void openDetailWindow(Item item) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/auction-detail.fxml"));
            javafx.scene.Parent root = loader.load();

            AuctionDetailController detailController = loader.getController();
            detailController.setItemData(item);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Product Detail: " + item.getName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String keyword = txtSearch.getText().toLowerCase();
        if (keyword.isEmpty()) {
            tableProducts.setItems(productList);
            return;
        }

        ObservableList<Item> filteredList = FXCollections.observableArrayList();
        for (Item item : productList) {
            if (item.getName().toLowerCase().contains(keyword)) {
                filteredList.add(item);
            }
        }
        tableProducts.setItems(filteredList);
    }
}
