package com.uet.bidding.model.user;

import com.uet.bidding.model.Entity;

public abstract class User extends Entity {

    // ATTRIBUTES

    // Để private để đảm bảo tính đóng gói (encapsulation),
    // tránh việc code bên ngoài sửa trực tiếp username.
    private String username;

    // Email cũng để private để chỉ được thay đổi thông qua setter,
    // từ đó mình có thể kiểm tra email có hợp lệ hay không.
    private String email;

    // Mật khẩu không lưu raw password mà lưu hash để bảo mật hơn.
    // Để private nhằm ngăn truy cập trực tiếp từ bên ngoài.
    private String passwordHash;

    // balance phải để private để bảo vệ dữ liệu tài chính.
    // Không cho phép code khác tự ý user.balance = ... vì sẽ rất nguy hiểm.
    // Việc thay đổi số dư phải đi qua deposit() và withdraw()
    // để đảm bảo luôn có kiểm tra hợp lệ.
    private long balance;

    // Trạng thái hoạt động của user.
    // Để private để chỉ thay đổi qua method kiểm soát.
    private boolean active;


    // CONSTRUCTOR
    protected User(String username, String email, String passwordHash) {
        super();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.balance = 0L;
        this.active = true;
    }

    // GETTER
    public String getUsername() {
        return username;
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
        // Setter được dùng để kiểm tra tính hợp lệ trước khi gán.
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
        // Chỉ cho nạp số tiền dương.
        if (amount <= 0) {
            throw new IllegalArgumentException("AMOUNT MUST BE POSITIVE");
        } else {
            this.balance += amount;
        }
    }

    public void withdraw(long amount) {
        // withdraw phải là public vì đây là "hành vi" mà đối tượng User
        // cho phép bên ngoài sử dụng, giống như chức năng rút tiền ở ATM.
        // Tuy nhiên, dù public thì vẫn không ai sửa trực tiếp balance được,
        // mà phải đi qua method này.

        // Kiểm tra số tiền rút phải hợp lệ.
        if (amount <= 0) {
            throw new IllegalArgumentException("AMOUNT MUST BE POSITIVE");
        }

        // Kiểm tra không được rút quá số dư hiện có.
        // Đây là lý do cần method withdraw(), vì nếu cho sửa balance trực tiếp
        // thì sẽ không có lớp bảo vệ logic này.
        else if (amount > this.getBalance()) {
            throw new InsufficientFundsException(
                    "Need " + amount + " but only have " + this.getBalance()
            );
        }

        // Nếu hợp lệ thì mới trừ tiền.
        else {
            this.balance -= amount;
        }
    }
    public boolean verifyPassword(String rawPassword) {
        return this.passwordHash.equals(String.valueOf(rawPassword.hashCode()));
    }
    public abstract String getRole();
    @Override
    public void printInfo() {
        System.out.println("=== " + getRole() + " ===");
        System.out.println("ID       : " + getId());
        System.out.println("Username : " + username);
        System.out.println("Email    : " + email);
        System.out.println("Balance  : " + balance + " VND");
        System.out.println("Active   : " + active);
    }
}