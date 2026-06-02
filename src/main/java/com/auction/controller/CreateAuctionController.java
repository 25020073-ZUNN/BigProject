package com.auction.controller;

import com.auction.network.client.NetworkService;
import com.auction.util.FxAsync;
import com.auction.util.UserSession;
import com.auction.util.SceneNavigator;
import com.auction.util.AlertHelper;
import com.auction.util.LoginStateHelper;
import com.auction.util.ThemeManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.auction.model.Auction;
import com.auction.model.item.Item;

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
    @FXML private Button themeToggleBtn;             // Nút chuyển đổi giao diện sáng/tối
    @FXML private Button loginButton;               // Nút Đăng nhập/Đăng xuất
    @FXML private Button createButton;              // Nút Tạo phiên đấu giá

    private File selectedImageFile;                 // Lưu file ảnh đã chọn

    // --- Chế độ chỉnh sửa (Edit Mode) ---
    private boolean editMode = false;               // Đang ở chế độ chỉnh sửa hay tạo mới
    private String editAuctionId;                    // ID phiên đấu giá đang chỉnh sửa
    private Auction editAuction;                     // Dữ liệu phiên đang chỉnh sửa

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
        endTimeField.setText(LocalDateTime.now().plusDays(1).format(DATE_TIME_FORMATTER));
        bidStepField.setText("50000");
        configureSelectedImageLabel();
        
        hintLabel.setText("Định dạng thời gian: yyyy-MM-dd HH:mm:ss. Bạn có thể đặt phiên kéo dài 1 ngày để làm mẫu.");
        
        // Khởi tạo hiển thị các trường theo danh mục mặc định
        updateCategoryFields(categoryComboBox.getValue());
        updateThemeButton();
    }

    /**
     * Chuyển form sang chế độ chỉnh sửa: điền sẵn dữ liệu từ Auction hiện có.
     * Được gọi bởi SceneNavigator.navigateToEditAuction().
     */
    public void setEditMode(Auction auction) {
        if (auction == null) return;
        this.editMode = true;
        this.editAuctionId = auction.getId();
        this.editAuction = auction;

        Item item = auction.getItem();

        // Điền dữ liệu chung
        nameField.setText(item.getName());
        descriptionArea.setText(item.getDescription());
        startingPriceField.setText(item.getStartingPrice().toPlainString());
        if (auction.getMinimumBidStep() != null) {
            bidStepField.setText(auction.getMinimumBidStep().toPlainString());
        }
        startTimeField.setText(item.getStartTime().format(DATE_TIME_FORMATTER));
        endTimeField.setText(item.getEndTime().format(DATE_TIME_FORMATTER));

        // Hiển thị thông tin ảnh hiện tại
        if (selectedImageLabel != null && item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
            String filename = item.getImageUrl().substring(item.getImageUrl().lastIndexOf('/') + 1);
            selectedImageLabel.setText(filename);
            selectedImageLabel.setTooltip(new Tooltip("Đã có ảnh cũ: " + item.getImageUrl() + "\nGiữ nguyên nếu không chọn ảnh mới."));
        }

        // Chọn đúng danh mục
        String category = item.getCategory();
        if (category != null) {
            categoryComboBox.setValue(category);
            updateCategoryFields(category);
        }

        // Điền dữ liệu riêng theo danh mục
        fillCategorySpecificFields(item);

        // Cập nhật giao diện
        if (createButton != null) {
            createButton.setText("Cập nhật phiên đấu giá");
        }
        if (hintLabel != null) {
            hintLabel.setText("Đang chỉnh sửa phiên đấu giá. Chỉ có thể sửa khi phiên chưa bắt đầu.");
        }
    }

    /**
     * Điền dữ liệu các trường đặc thù theo danh mục sản phẩm.
     */
    private void fillCategorySpecificFields(Item item) {
        // Sử dụng reflection-free approach: dựa vào instanceof
        if (item instanceof com.auction.model.item.Electronics elec) {
            if (brandField != null) brandField.setText(elec.getBrand() != null ? elec.getBrand() : "");
            if (warrantyMonthsField != null) warrantyMonthsField.setText(String.valueOf(elec.getWarrantyMonths()));
        } else if (item instanceof com.auction.model.item.Vehicle vehicle) {
            if (manufacturerField != null) manufacturerField.setText(vehicle.getManufacturer() != null ? vehicle.getManufacturer() : "");
            if (productionYearField != null) productionYearField.setText(String.valueOf(vehicle.getYear()));
            if (mileageField != null) mileageField.setText(String.valueOf(vehicle.getMileage()));
        } else if (item instanceof com.auction.model.item.Art art) {
            if (artistField != null) artistField.setText(art.getArtist() != null ? art.getArtist() : "");
            if (yearCreatedField != null) yearCreatedField.setText(String.valueOf(art.getYearCreated()));
        }
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

        selectedImageFile = selectedFile;
        if (selectedImageLabel != null) {
            selectedImageLabel.setText(shortenFileName(selectedFile.getName(), 42));
            selectedImageLabel.setTooltip(new Tooltip(selectedFile.getAbsolutePath()));
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
            if (selectedImageFile != null) {
                attachImagePayload(attributes);
            } else if (editMode && editAuction != null && editAuction.getItem().getImageUrl() != null) {
                attributes.put("imageUrl", editAuction.getItem().getImageUrl());
            }

            // Gửi dữ liệu bất đồng bộ lên server qua NetworkService
            String buttonLabel = editMode ? "Cập nhật phiên đấu giá" : "Lưu phiên đấu giá";
            if (createButton != null) {
                createButton.setDisable(true);
                createButton.setText(editMode ? "Đang cập nhật..." : "Đang tạo phiên...");
            }

            if (editMode) {
                // --- Chế độ chỉnh sửa: gọi updateAuction ---
                FxAsync.run(
                        () -> {
                            networkService.updateAuction(
                                    editAuctionId, category, name, description,
                                    startingPrice.toPlainString(), bidStep.toPlainString(),
                                    startTime.format(ISO_FORMATTER), endTime.format(ISO_FORMATTER),
                                    sellerUsername, attributes
                            );
                            return null;
                        },
                        result -> {
                            if (createButton != null) {
                                createButton.setDisable(false);
                                createButton.setText(buttonLabel);
                            }
                            AlertHelper.showInformation("Cập nhật thành công", "Thông tin sản phẩm và phiên đấu giá đã được cập nhật.");
                            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                            SceneNavigator.goToAuctionHistory(new ActionEvent(event.getSource(), null));
                        },
                        errorMsg -> {
                            if (createButton != null) {
                                createButton.setDisable(false);
                                createButton.setText(buttonLabel);
                            }
                            AlertHelper.showError("Lỗi", "Không thể cập nhật phiên đấu giá: " + errorMsg);
                        }
                );
            } else {
                // --- Chế độ tạo mới ---
                FxAsync.run(
                        () -> {
                            // 1. Kiểm tra trùng phiên (cùng tên, cùng người bán)
                            java.util.List<java.util.Map<String, Object>> existing = networkService.getAuctions();
                            boolean isDuplicate = existing.stream().anyMatch(a -> {
                                String existingName = String.valueOf(a.getOrDefault("itemName", ""));
                                String existingSeller = String.valueOf(a.getOrDefault("sellerName", ""));
                                return name.equalsIgnoreCase(existingName) && sellerUsername.equalsIgnoreCase(existingSeller);
                            });
                            if (isDuplicate) {
                                throw new RuntimeException("Phiên đấu giá cho tài sản \"" + name + "\" đã được bạn tạo trước đó.");
                            }

                            // 2. Tạo phiên đấu giá mới
                            networkService.createAuction(
                                    category, name, description,
                                    startingPrice.toPlainString(), bidStep.toPlainString(),
                                    startTime.format(ISO_FORMATTER), endTime.format(ISO_FORMATTER),
                                    sellerUsername, attributes
                            );

                            // 3. Lấy lại danh sách và chuyển đổi sang Auction
                            java.util.List<java.util.Map<String, Object>> updated = networkService.getAuctions();
                            java.util.Map<String, Object> matchedPayload = updated.stream()
                                    .filter(a -> name.equalsIgnoreCase(String.valueOf(a.getOrDefault("itemName", "")))
                                            && sellerUsername.equalsIgnoreCase(String.valueOf(a.getOrDefault("sellerName", ""))))
                                    .findFirst()
                                    .orElse(null);

                            if (matchedPayload != null) {
                                return com.auction.network.client.AuctionPayloadMapper.toAuction(matchedPayload);
                            }
                            return null;
                        },
                        (newAuction) -> {
                            if (createButton != null) {
                                createButton.setDisable(false);
                                createButton.setText(buttonLabel);
                            }
                            AlertHelper.showInformation("Tạo phiên thành công", "Tài sản và phiên đấu giá đã được lưu vào CSDL.");
                            if (newAuction != null) {
                                javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                                SceneNavigator.navigateToAssetDetail(stage, newAuction);
                            } else {
                                clearFormForNextEntry(); // Xóa trắng form để nhập tài sản tiếp theo
                            }
                        },
                        errorMsg -> {
                            if (createButton != null) {
                                createButton.setDisable(false);
                                createButton.setText(buttonLabel);
                            }
                            AlertHelper.showError("Lỗi", "Không thể tạo phiên đấu giá: " + errorMsg);
                        }
                );
            }
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
        selectedImageFile = null;
        if (selectedImageLabel != null) {
            selectedImageLabel.setText("Chưa chọn ảnh");
        }
    }

    private void configureSelectedImageLabel() {
        if (selectedImageLabel == null) {
            return;
        }
        selectedImageLabel.setMaxWidth(360);
        selectedImageLabel.setWrapText(false);
        selectedImageLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
    }

    private String shortenFileName(String fileName, int maxLength) {
        if (fileName == null || fileName.length() <= maxLength) {
            return fileName;
        }

        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        int availableBaseLength = Math.max(8, maxLength - extension.length() - 3);
        if (baseName.length() <= availableBaseLength) {
            return baseName + extension;
        }
        return baseName.substring(0, availableBaseLength) + "..." + extension;
    }

    private void attachImagePayload(Map<String, Object> attributes) {
        try {
            byte[] imageBytes = Files.readAllBytes(selectedImageFile.toPath());
            attributes.put("imageBase64", Base64.getEncoder().encodeToString(imageBytes));
            attributes.put("imageFileName", selectedImageFile.getName());
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc file ảnh đã chọn.");
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

    @FXML
    public void toggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggleTheme(scene);
        updateThemeButton();
    }

    private void updateThemeButton() {
        if (themeToggleBtn != null) {
            themeToggleBtn.setText(
                    ThemeManager.getInstance().isDarkMode()
                            ? "☀"
                            : "\uD83C\uDF19"
            );
        }
    }

    // --- Các phương thức điều hướng Sidebar ---
    @FXML public void goToHome(ActionEvent event) { SceneNavigator.goToHome(event); }
    @FXML public void goToAuctionList(ActionEvent event) { SceneNavigator.goToAuctionList(event); }

    @FXML public void goToCreateAuction(ActionEvent event) { SceneNavigator.goToCreateAuction(event); }
    @FXML public void goToLogin(ActionEvent event) { SceneNavigator.goToLogin(event); }
    @FXML public void goToSessions(ActionEvent event) { SceneNavigator.goToSessions(event); }
    @FXML public void goToNews(ActionEvent event) { SceneNavigator.goToNews(event); }
    @FXML public void goToContact(ActionEvent event) { SceneNavigator.goToContact(event); }
}
