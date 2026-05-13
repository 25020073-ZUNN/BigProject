package com.auction.controller;

import com.auction.dao.UserDao;
import com.auction.model.user.User;
import com.auction.network.client.NetworkService;
import com.auction.util.FxAsync;
import com.auction.util.UserSession;
import com.auction.util.SceneNavigator;
import com.auction.util.AlertHelper;
import com.auction.util.LoginStateHelper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho màn hình tạo tài sản đấu giá thật.
 */
public class CreateAuctionController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final NetworkService networkService = NetworkService.getInstance();
    private final UserDao userDao = new UserDao();

    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> sellerComboBox;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startingPriceField;
    @FXML private TextField bidStepField;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private TextField brandField;
    @FXML private TextField warrantyMonthsField;
    @FXML private TextField manufacturerField;
    @FXML private TextField productionYearField;
    @FXML private TextField mileageField;
    @FXML private TextField artistField;
    @FXML private TextField yearCreatedField;
    @FXML private Label hintLabel;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList("Electronics", "Vehicle", "Art"));
        categoryComboBox.setValue("Electronics");

        LoginStateHelper.updateLoginButton(loginButton);

        List<String> sellers = userDao.findActiveSellers().stream()
                .map(User::getUsername).toList();
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

    @FXML
    public void handleLogout(ActionEvent event) { LoginStateHelper.handleLogout(event); }

    @FXML
    public void handleCreateAuction(ActionEvent event) {
        try {
            String sellerUsername = sellerComboBox.getValue();
            if (sellerUsername == null || sellerUsername.isBlank()) {
                AlertHelper.showError("Lỗi", "Vui lòng chọn người bán.");
                return;
            }

            String category = categoryComboBox.getValue();
            String name = requireText(nameField.getText(), "Tên tài sản");
            String description = requireText(descriptionArea.getText(), "Mô tả tài sản");
            BigDecimal startingPrice = parseMoney(startingPriceField.getText(), "Giá khởi điểm");
            BigDecimal bidStep = parseMoney(bidStepField.getText(), "Bước giá");
            LocalDateTime startTime = parseDateTime(startTimeField.getText(), "Thời gian bắt đầu");
            LocalDateTime endTime = parseDateTime(endTimeField.getText(), "Thời gian kết thúc");
            Map<String, Object> attributes = buildAttributes(category);

            FxAsync.run(
                    () -> {
                        networkService.createAuction(
                                category, name, description,
                                startingPrice.toPlainString(), bidStep.toPlainString(),
                                startTime.format(ISO_FORMATTER), endTime.format(ISO_FORMATTER),
                                sellerUsername, attributes
                        );
                    },
                    () -> {
                        AlertHelper.showInformation("Tạo phiên thành công", "Tài sản và phiên đấu giá đã được lưu vào CSDL.");
                        clearFormForNextEntry();
                    },
                    errorMsg -> AlertHelper.showError("Lỗi", "Không thể tạo phiên đấu giá: " + errorMsg)
            );
        } catch (IllegalArgumentException ex) {
            AlertHelper.showError("Lỗi", ex.getMessage());
        }
    }

    private Map<String, Object> buildAttributes(String category) {
        Map<String, Object> attributes = new HashMap<>();
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
        nameField.clear(); descriptionArea.clear(); startingPriceField.clear();
        brandField.clear(); warrantyMonthsField.clear(); manufacturerField.clear();
        productionYearField.clear(); mileageField.clear(); artistField.clear(); yearCreatedField.clear();
        startTimeField.setText(LocalDateTime.now().plusMinutes(5).format(DATE_TIME_FORMATTER));
        endTimeField.setText(LocalDateTime.now().plusMonths(3).format(DATE_TIME_FORMATTER));
        bidStepField.setText("500000");
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " không được để trống.");
        return value.trim();
    }

    private BigDecimal parseMoney(String rawValue, String fieldName) {
        String normalized = requireText(rawValue, fieldName).replaceAll("[^\\d]", "");
        if (normalized.isBlank()) throw new IllegalArgumentException(fieldName + " không hợp lệ.");
        return new BigDecimal(normalized);
    }

    private int parseInteger(String rawValue, String fieldName) {
        try { return Integer.parseInt(requireText(rawValue, fieldName)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(fieldName + " phải là số nguyên."); }
    }

    private LocalDateTime parseDateTime(String rawValue, String fieldName) {
        try { return LocalDateTime.parse(requireText(rawValue, fieldName), DATE_TIME_FORMATTER); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException(fieldName + " phải theo định dạng yyyy-MM-dd HH:mm:ss."); }
    }

    // --- Điều hướng ---
    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToProductDetail(ActionEvent event) { SceneNavigator.goToProductDetail(event); }
    @FXML public void goToCreateAuction(ActionEvent event) { SceneNavigator.goToCreateAuction(event); }
    @FXML public void goToLogin(ActionEvent event) { SceneNavigator.goToLogin(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
}
