package com.auction.controller;

import com.auction.dao.UserDao;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import com.auction.util.UserSession;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho màn hình tạo tài sản đấu giá thật.
 *
 * Ghi chú nghiệp vụ:
 * - Màn hình này không tạo "dữ liệu minh họa" trong RAM.
 * - Khi nhấn lưu, dữ liệu sẽ được xác thực rồi ghi thật vào MySQL.
 * - Mỗi danh mục tài sản sẽ yêu cầu thêm một nhóm thuộc tính riêng để DAO
 *   có thể dựng lại đúng subclass của `Item` sau này.
 */
public class CreateAuctionController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuctionService auctionService = AuctionService.getInstance();
    private final UserDao userDao = new UserDao();

    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private ComboBox<String> sellerComboBox;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField startingPriceField;
    @FXML
    private TextField bidStepField;
    @FXML
    private TextField startTimeField;
    @FXML
    private TextField endTimeField;
    @FXML
    private TextField brandField;
    @FXML
    private TextField warrantyMonthsField;
    @FXML
    private TextField manufacturerField;
    @FXML
    private TextField productionYearField;
    @FXML
    private TextField mileageField;
    @FXML
    private TextField artistField;
    @FXML
    private TextField yearCreatedField;
    @FXML
    private Label hintLabel;
    @FXML
    private Button loginButton;

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList("Electronics", "Vehicle", "Art"));
        categoryComboBox.setValue("Electronics");

        updateLoginState();

        List<String> sellers = userDao.findActiveSellers().stream()
                .map(User::getUsername)
                .toList();
        sellerComboBox.setItems(FXCollections.observableArrayList(sellers));

        if (UserSession.isLoggedIn() && "SELLER".equalsIgnoreCase(UserSession.getLoggedInUser().getRole())) {
            sellerComboBox.setValue(UserSession.getLoggedInUser().getUsername());
        } else if (!sellers.isEmpty()) {
            sellerComboBox.setValue(sellers.get(0));
        }

        startTimeField.setText(LocalDateTime.now().plusMinutes(5).format(DATE_TIME_FORMATTER));
        endTimeField.setText(LocalDateTime.now().plusMonths(3).format(DATE_TIME_FORMATTER));
        bidStepField.setText("500000");
        hintLabel.setText("Định dạng thời gian: yyyy-MM-dd HH:mm:ss. Bạn có thể đặt phiên kéo dài 3 tháng để làm mẫu.");
    }

    private void updateLoginState() {
        if (UserSession.isLoggedIn()) {
            User user = UserSession.getLoggedInUser();
            if (loginButton != null) {
                loginButton.setText("Đăng xuất (" + user.getUsername() + ")");
                loginButton.setOnAction(this::handleLogout);
            }
        } else {
            if (loginButton != null) {
                loginButton.setText("Đăng nhập");
                loginButton.setOnAction(this::goToLogin);
            }
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        UserSession.logout();
        updateLoginState();
        try {
            goToHome(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCreateAuction(ActionEvent event) {
        try {
            String sellerUsername = sellerComboBox.getValue();
            User seller = userDao.findByUsername(sellerUsername);
            if (seller == null) {
                showError("Không tìm thấy người bán hợp lệ trong cơ sở dữ liệu.");
                return;
            }

            String category = categoryComboBox.getValue();
            BigDecimal startingPrice = parseMoney(startingPriceField.getText(), "Giá khởi điểm");
            BigDecimal bidStep = parseMoney(bidStepField.getText(), "Bước giá");
            LocalDateTime startTime = parseDateTime(startTimeField.getText(), "Thời gian bắt đầu");
            LocalDateTime endTime = parseDateTime(endTimeField.getText(), "Thời gian kết thúc");

            Map<String, Object> attributes = buildAttributes(category);

            boolean created = auctionService.createAuction(
                    category,
                    requireText(nameField.getText(), "Tên tài sản"),
                    requireText(descriptionArea.getText(), "Mô tả tài sản"),
                    startingPrice,
                    bidStep,
                    startTime,
                    endTime,
                    seller,
                    attributes
            );

            if (!created) {
                showError("Không thể tạo phiên đấu giá. Vui lòng kiểm tra lại kết nối DB hoặc dữ liệu nhập.");
                return;
            }

            showInformation("Tạo phiên thành công", "Tài sản và phiên đấu giá đã được lưu thật vào cơ sở dữ liệu.");
            clearFormForNextEntry();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private Map<String, Object> buildAttributes(String category) {
        Map<String, Object> attributes = new HashMap<>();

        /*
         * Ghi chú rất quan trọng:
         * Mỗi danh mục được map sang một subclass `Item` khác nhau ở tầng domain.
         * Vì vậy form phải gom đúng bộ thuộc tính cho từng loại.
         * Nếu đẩy sai thuộc tính, ItemFactory sẽ không thể dựng đúng object.
         */
        switch (category) {
            case "Electronics" -> {
                attributes.put("brand", requireText(brandField.getText(), "Hãng điện tử"));
                attributes.put("warrantyMonths", parseInteger(warrantyMonthsField.getText(), "Số tháng bảo hành"));
            }
            case "Vehicle" -> {
                attributes.put("manufacturer", requireText(manufacturerField.getText(), "Hãng xe"));
                attributes.put("year", parseInteger(productionYearField.getText(), "Năm sản xuất"));
                attributes.put("mileage", parseInteger(mileageField.getText(), "Số km đã đi"));
            }
            case "Art" -> {
                attributes.put("artist", requireText(artistField.getText(), "Tên tác giả"));
                attributes.put("yearCreated", parseInteger(yearCreatedField.getText(), "Năm sáng tác"));
            }
            default -> throw new IllegalArgumentException("Danh mục tài sản không hợp lệ.");
        }

        return attributes;
    }

    private void clearFormForNextEntry() {
        nameField.clear();
        descriptionArea.clear();
        startingPriceField.clear();
        brandField.clear();
        warrantyMonthsField.clear();
        manufacturerField.clear();
        productionYearField.clear();
        mileageField.clear();
        artistField.clear();
        yearCreatedField.clear();
        startTimeField.setText(LocalDateTime.now().plusMinutes(5).format(DATE_TIME_FORMATTER));
        endTimeField.setText(LocalDateTime.now().plusMonths(3).format(DATE_TIME_FORMATTER));
        bidStepField.setText("500000");
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        return value.trim();
    }

    private BigDecimal parseMoney(String rawValue, String fieldName) {
        String normalized = requireText(rawValue, fieldName).replaceAll("[^\\d]", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không hợp lệ.");
        }
        return new BigDecimal(normalized);
    }

    private int parseInteger(String rawValue, String fieldName) {
        try {
            return Integer.parseInt(requireText(rawValue, fieldName));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " phải là số nguyên.");
        }
    }

    private LocalDateTime parseDateTime(String rawValue, String fieldName) {
        try {
            return LocalDateTime.parse(requireText(rawValue, fieldName), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " phải theo định dạng yyyy-MM-dd HH:mm:ss.");
        }
    }

    @FXML
    public void goToHome(ActionEvent event) {
        switchScene(event, "giaodien.fxml");
    }

    @FXML
    public void goToAuctionList(ActionEvent event) {
        switchScene(event, "auction-detail.fxml");
    }

    @FXML
    public void goToProductDetail(ActionEvent event) {
        switchScene(event, "product-detail.fxml");
    }

    @FXML
    public void goToCreateAuction(ActionEvent event) {
        switchScene(event, "create-auction.fxml");
    }

    @FXML
    public void goToLogin(ActionEvent event) {
        switchScene(event, "login.fxml");
    }

    @FXML
    public void goToSessions(ActionEvent event) {
        switchScene(event, "sessions.fxml");
    }

    @FXML
    public void goToNews(ActionEvent event) {
        switchScene(event, "news.fxml");
    }

    @FXML
    public void goToContact(ActionEvent event) {
        switchScene(event, "contact.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/" + fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene currentScene = stage.getScene();
            if (currentScene == null) {
                stage.setScene(new Scene(root, 1380, 920));
            } else {
                currentScene.setRoot(root);
            }
        } catch (IOException e) {
            showError("Không thể tải giao diện: " + fxmlFile);
        }
    }

    private void showInformation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
