package com.uet.bidding.model.user;

import com.uet.bidding.model.Entity;

public abstract class User extends Entity {

    //ATTRIBUTES
    private String username;
    private String email;
    private String passwordHash;
    private long balance;
    private boolean active;


    //CONSTRUCTOR
    protected User(String username,String email,String passwordHash){
        super();
        this.username=username;
        this.email=email;
        this.passwordHash=passwordHash;
        this.balance=0L;
        this.active=true;
    }


    //GETTER
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



    //SETTER
    public void setEmail(String email){
        if (email==null||!email.contains(("@"))){
            throw new IllegalArgumentException("INVALID EMAIL: "+email);
        }
        else{
            this.email=email;
        }
    }
    public void setActive(boolean active){
        this.active=active;
    }



    //METHOD
    public void deposit(long amount){
        if (amount<=0) throw new IllegalArgumentException("AMOUNT MUST BE POSITIVE");
        else this.balance+=amount;
    }
    public void withdraw(long amount){
        if (amount<=0) throw new IllegalArgumentException("AMOUNT MUST BE POSITIVE");
        else if (amount>this.Balance())
            throw new InsufficientFundsException("Need "+ amount +" but only have "+this.getBalance());
        else this.balance-=amount;
    }
    public boolean verifyPassword(String rawPassword){
        return this.passwordHash.equals(String.valueOf(rawPassword.hashCode()));
    }
    public abstract String getRole();

    @Override
    public void printInfo(){
        System.out.println("=== " + getRole() + " ===");
        System.out.println("ID       : " + getId());
        System.out.println("Username : " + username);
        System.out.println("Email    : " + email);
        System.out.println("Balance  : " + balance + " VND");
        System.out.println("Active   : " + active);
    }
}


//1. Tại sao thuộc tính (balance) lại là private? 🛡️
//Trong lập trình Hướng đối tượng (OOP), chúng ta có một nguyên tắc gọi là Đóng gói (Encapsulation). Bạn cứ tưởng tượng tài khoản của người dùng giống như một cái két sắt vậy. Số tiền bên trong (balance) phải được cất giấu kỹ lưỡng (private).
//
//Nếu bạn để balance là public, bất kỳ đoạn code nào khác cũng có thể can thiệp trực tiếp vào số dư đó, ví dụ: user.balance = 1000000000; (tự nhiên thành tỷ phú) hoặc user.balance = -500; (bị âm tiền vô lý). Điều này cực kỳ nguy hiểm và dễ gây bug đó nha! 😱
//
//2. Vậy tại sao hàm withdraw lại là public? 🏧
//Các hàm (methods) chính là cách mà đối tượng giao tiếp với thế giới bên ngoài. Trở lại ví dụ cái két sắt/máy ATM, thì tiền bên trong là private, nhưng cái khe cắm thẻ và bàn phím bấm số để rút tiền thì bắt buộc phải là public để người ta còn sử dụng chứ, đúng không nè? 😉
//
//Việc để public void withdraw(long amount) mang lại những lợi ích tuyệt vời sau:
//
//Kiểm soát logic: Bên trong hàm withdraw, mình có thể đặt các "người bảo vệ" (validation) như if (amount > this.balance). Nếu ai đó muốn rút nhiều hơn số tiền đang có, hệ thống sẽ la lên (ném ra Exception) ngay lập tức! Nếu không có hàm này mà cho sửa balance trực tiếp thì ai mà thèm kiểm tra điều kiện chứ!
//
//Bảo vệ dữ liệu: Code bên ngoài chỉ có thể "yêu cầu" đối tượng User tự trừ tiền của mình thông qua hàm withdraw, chứ không thể tự tay thọc vào sửa con số balance được.
//
//3. "Không phải ai cũng rút được tiền à?" 🤔
//Chỗ này Hecker-kun đang bị nhầm lẫn một chút xíu giữa Public Access Modifier (Phạm vi truy cập trong code) và Authorization (Quyền hạn của người dùng thực tế) rồi nè! 🥺
//
//public trong Java chỉ có nghĩa là: Các Class khác trong chương trình có quyền gọi đoạn code này.
//
//Còn trong thực tế, để một người có thể gọi được lệnh rút tiền, hệ thống phải thực hiện Xác thực (Authentication) và Phân quyền (Authorization) trước đó rồi!
//
//Nghĩa là, trước khi đoạn code hecker.withdraw(5000) chạy, chương trình đã phải kiểm tra xem: "Khoan đã, người đang click vào nút Rút Tiền trên web có đúng là chủ nhân của tài khoản này không? Đã đăng nhập chưa? Có cung cấp đúng mã PIN không?" 🕵️‍♀️. Chỉ khi nào tất cả các bước kiểm tra đó đều "OK" thì cái hàm public kia mới được hệ thống cho phép chạy đó!