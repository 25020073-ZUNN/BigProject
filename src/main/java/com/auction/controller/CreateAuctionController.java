package com.auction.controller;

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
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller cho màn hình "Tạo phiên đấu giá" (Create Auction).
 * Cho phép người dùng (người bán) đăng tải tài sản mới và thiết lập các thông số cho phiên đấu giá.
 */
public class CreateAuctionController {

    // Định dạng ngày giờ sử dụng để nhập liệu và hiển thị trên giao diện
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // Định dạng ISO sử dụng để gửi dữ liệu về server
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Dịch vụ mạng để gửi yêu cầu tạo phiên lên server
    private final NetworkService networkService = NetworkService.getInstance();

    // --- Các thành phần giao diện FXML ---
    @FXML private ComboBox<String> categoryComboBox; // Lựa chọn danh mục (Electronics, Vehicle, Art)
    @FXML private TextField nameField;               // Tên tài sản
    @FXML private TextArea descriptionArea;         // Mô tả chi tiết
    @FXML private TextField startingPriceField;     // Giá khởi điểm
    @FXML private TextField bidStepField;           // Bước giá tối thiểu
    @FXML private TextField startTimeField;         // Thời gian bắt đầu
    @FXML private TextField endTimeField;           // Thời gian kết thúc
    
    // Các trường dữ liệu riêng cho từng danh mục
    @FXML private TextField brandField;             // Hãng (Electronics)
    @FXML private TextField warrantyMonthsField;    // Tháng bảo hành (Electronics)
    @FXML private TextField manufacturerField;      // Hãng sản xuất (Vehicle)
    @FXML private TextField productionYearField;    // Năm sản xuất (Vehicle)
    @FXML private TextField mileageField;           // Số km đã đi (Vehicle)
    @FXML private TextField artistField;            // Tác giả (Art)
    @FXML private TextField yearCreatedField;       // Năm sáng tác (Art)

    // Các vùng chứa (VBox) để ẩn/hiện trường nhập liệu theo danh mục
    @FXML private VBox electronicsFields;
    @FXML private VBox vehicleFields;
    @FXML private VBox artFields;
    
    @FXML private Label hintLabel;                  // Nhãn gợi ý định dạng
    @FXML private Label selectedImageLabel;         // Hiển thị tên file ảnh đã chọn
    @FXML private Button loginButton;               // Nút Đăng nhập/Đăng xuất

    private String selectedImageUrl;                // Lưu đường dẫn ảnh đã chọn

    /**
     * Khởi tạo giao diện: thiết lập danh mục, giá trị mặc định cho thời gian và đăng ký sự kiện chuyển danh mục.
     */
    @FXML
    public void initialize() {
        // Thiết lập các tùy chọn cho danh mục
        categoryComboBox.setItems(FXCollections.observableArrayList("Electronics", "Vehicle", "Art"));
        categoryComboBox.setValue("Electronics");
        
        // Khi thay đổi danh mục, cập nhật lại các trường nhập liệu tương ứng
        categoryComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateCategoryFields(newValue));

        // Cập nhật nút Login/Logout theo session
        LoginStateHelper.updateLoginButton(loginButton);

        // Gợi ý thời gian mặc định (bắt đầu sau 5 phút, kết thúc sau 3 tháng)
        startTimeField.setText(LocalDateTime.now().plusMinutes(5).format(DATE_TIME_FORMATTER));
        endTimeField.setText(LocalDateTime.now().plusMonths(3).format(DATE_TIME_FORMATTER));
        bidStepField.setText("500000");
        
        hintLabel.setText("Định dạng thời gian: yyyy-MM-dd HH:mm:ss. Bạn có thể đặt phiên kéo dài 3 tháng để làm mẫu.");
        
        // Khởi tạo hiển thị các trường theo danh mục mặc định
        updateCategoryFields(categoryComboBox.getValue());
    }

    /**
     * Xử lý đăng xuất.
     */
    @FXML
    public void handleLogout(ActionEvent event) { LoginStateHelper.handleLogout(event); }

    /**
     * Mở hộp thoại chọn ảnh từ máy tính.
     */
    @FXML
    public void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        // Lọc các định dạng ảnh phổ biến
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );

        File selectedFile = fileChooser.showOpenDialog(((Button) event.getSource()).getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        // Lưu URI của ảnh để hiển thị sau này
        selectedImageUrl = selectedFile.toURI().toString();
        if (selectedImageLabel != null) {
            selectedImageLabel.setText(selectedFile.getName());
        }
    }

    /**
     * Thu thập dữ liệu từ form, kiểm tra tính hợp lệ và gửi yêu cầu tạo phiên đấu giá lên server.
     */
    @FXML
    public void handleCreateAuction(ActionEvent event) {
        try {
            // Kiểm tra trạng thái đăng nhập
            String sellerUsername = UserSession.isLoggedIn() ? UserSession.getLoggedInUser().getUsername() : null;
            if (sellerUsername == null || sellerUsername.isBlank()) {
                AlertHelper.showError("Lỗi", "Vui lòng đăng nhập để đăng bán sản phẩm.");
                return;
            }

            // Đọc dữ liệu từ các trường nhập liệu
            String category = categoryComboBox.getValue();
            String name = requireText(nameField.getText(), "Tên tài sản");
            String description = requireText(descriptionArea.getText(), "Mô tả tài sản");
            BigDecimal startingPrice = parseMoney(startingPriceField.getText(), "Giá khởi điểm");
            BigDecimal bidStep = parseMoney(bidStepField.getText(), "Bước giá");
            LocalDateTime startTime = parseDateTime(startTimeField.getText(), "Thời gian bắt đầu");
            LocalDateTime endTime = parseDateTime(endTimeField.getText(), "Thời gian kết thúc");
            
            // Xây dựng map các thuộc tính đặc thù theo danh mục
            Map<String, Object> attributes = buildAttributes(category);
            if (selectedImageUrl != null && !selectedImageUrl.isBlank()) {
                attributes.put("imageUrl", selectedImageUrl);
            }

            // Gửi dữ liệu bất đồng bộ lên server qua NetworkService
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
                        clearFormForNextEntry(); // Xóa trắng form để nhập tài sản tiếp theo
                    },
                    errorMsg -> AlertHelper.showError("Lỗi", "Không thể tạo phiên đấu giá: " + errorMsg)
            );
        } catch (IllegalArgumentException ex) {
            // Bắt lỗi do dữ liệu nhập vào không hợp lệ (đã được ném ra từ các hàm parse/require)
            AlertHelper.showError("Lỗi", ex.getMessage());
        }
    }

    /**
     * Trích xuất các thuộc tính đặc trưng dựa trên danh mục sản phẩm đang chọn.
     */
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

    /**
     * Ẩn/hiện các nhóm trường nhập liệu đặc thù khi người dùng thay đổi danh mục.
     */
    private void updateCategoryFields(String category) {
        setSectionVisible(electronicsFields, "Electronics".equals(category));
        setSectionVisible(vehicleFields, "Vehicle".equals(category));
        setSectionVisible(artFields, "Art".equals(category));
    }

    /**
     * Hàm tiện ích để điều khiển việc hiển thị của một VBox.
     */
    private void setSectionVisible(VBox section, boolean visible) {
        if (section == null) return;
        section.setVisible(visible);
        section.setManaged(visible);
    }

    /**
     * Xóa trắng các trường nhập liệu sau khi tạo thành công.
     */
    private void clearFormForNextEntry() {
        nameField.clear(); descriptionArea.clear(); startingPriceField.clear();
        brandField.clear(); warrantyMonthsField.clear(); manufacturerField.clear();
        productionYearField.clear(); mileageField.clear(); artistField.clear(); yearCreatedField.clear();
        
        // Reset thời gian mặc định
        startTimeField.setText(LocalDateTime.now().plusMinutes(5).format(DATE_TIME_FORMATTER));
        endTimeField.setText(LocalDateTime.now().plusMonths(3).format(DATE_TIME_FORMATTER));
        bidStepField.setText("500000");
        selectedImageUrl = null;
        if (selectedImageLabel != null) {
            selectedImageLabel.setText("Chưa chọn ảnh");
        }
    }

    /**
     * Kiểm tra văn bản không được để trống.
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " không được để trống.");
        return value.trim();
    }

    /**
     * Chuyển đổi văn bản sang BigDecimal (chỉ giữ lại các chữ số).
     */
    private BigDecimal parseMoney(String rawValue, String fieldName) {
        String normalized = requireText(rawValue, fieldName).replaceAll("[^\\d]", "");
        if (normalized.isBlank()) throw new IllegalArgumentException(fieldName + " không hợp lệ.");
        return new BigDecimal(normalized);
    }

    /**
     * Chuyển đổi văn bản sang số nguyên.
     */
    private int parseInteger(String rawValue, String fieldName) {
        try { return Integer.parseInt(requireText(rawValue, fieldName)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(fieldName + " phải là số nguyên."); }
    }

    /**
     * Chuyển đổi văn bản sang LocalDateTime theo định dạng quy định.
     */
    private LocalDateTime parseDateTime(String rawValue, String fieldName) {
        try { return LocalDateTime.parse(requireText(rawValue, fieldName), DATE_TIME_FORMATTER); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException(fieldName + " phải theo định dạng yyyy-MM-dd HH:mm:ss."); }
    }

    // --- Các phương thức điều hướng Sidebar ---
    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }
    @FXML public void goToProductDetail(ActionEvent event) { SceneNavigator.goToProductDetail(event); }
    @FXML public void goToCreateAuction(ActionEvent event) { SceneNavigator.goToCreateAuction(event); }
    @FXML public void goToLogin(ActionEvent event) { SceneNavigator.goToLogin(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
}
