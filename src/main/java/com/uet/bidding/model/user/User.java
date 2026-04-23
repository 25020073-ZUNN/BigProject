package com.uet.bidding.model.user;
import com.uet.bidding.model.Entity;
public abstract class User extends Entity {

    // ATTRIBUTES

    // Để private để đảm bảo tính đóng gói (encapsulation),
    // tránh cho code bên ngoài sửa trực tiếp dữ liệu.
    private String username;
    private String fullname;

    // Email để private và chỉ cho đổi qua setter
    // để có thể kiểm tra dữ liệu hợp lệ trước khi gán.
    private String email;

    // Lưu hash thay vì mật khẩu gốc để tăng bảo mật.
    private String passwordHash;

    // balance để private để không ai có thể sửa trực tiếp kiểu:
    // user.balance = 999999;
    // Muốn thay đổi số dư phải đi qua deposit() / withdraw()
    // để đảm bảo có kiểm tra logic.
    private long balance;

    // Trạng thái hoạt động của tài khoản.
    private boolean active;


    // CONSTRUCTOR
    protected User(String username, String email, String passwordHash) {
        this(username, username, email, passwordHash);
    }

    protected User(String username,String fullname, String email, String passwordHash) {
        super();
        this.username = username;
        this.fullname=fullname;
        this.email = email;
        this.passwordHash = passwordHash;
        this.balance = 0L;
        this.active = true;
    }

    // GETTER
    public String getUsername() {
        return username;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public long getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    // SETTER
    public void setEmail(String email) {
        // Chỉ cho phép email hợp lệ mới được gán.
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("INVALID EMAIL: " + email);
        } else {
            this.email = email;
        }
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // METHOD
    public void deposit(long amount) {
        // Nạp tiền thì số tiền phải dương.
        if (amount <= 0) {
            throw new IllegalArgumentException("AMOUNT MUST BE POSITIVE");
        } else {
            this.balance += amount;
        }
    }

    public void withdraw(long amount) {
        // Hàm này để public vì đây là hành vi mà đối tượng User
        // cho phép bên ngoài gọi tới.
        // Nhưng dù public, code ngoài vẫn không thể sửa balance trực tiếp,
        // mà chỉ có thể "yêu cầu" User tự rút tiền thông qua method này.

        // Kiểm tra số tiền rút phải hợp lệ.
        if (amount <= 0) {
            throw new IllegalArgumentException("AMOUNT MUST BE POSITIVE");
        }

        // Không được rút quá số dư hiện có.
        else if (amount > this.getBalance()) {
            throw new InsufficientFundsException(
                    "Need " + amount + " but only have " + this.getBalance()
            );
        }

        // Hợp lệ thì mới trừ tiền.
        else {
            this.balance -= amount;
        }
    }

    public boolean verifyPassword(String rawPassword) {
        return this.passwordHash.equals(String.valueOf(rawPassword.hashCode()));
    }

    public abstract String getRole();

    // Chỉ giữ @Override nếu Entity có method printInfo()
    public void printInfo() {
        System.out.println("=== " + getRole() + " ===");
        System.out.println("ID       : " + getId());
        System.out.println("Username : " + username);
        System.out.println("Email    : " + email);
        System.out.println("Balance  : " + balance + " VND");
        System.out.println("Active   : " + active);
    }
}