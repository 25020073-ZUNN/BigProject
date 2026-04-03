package com.uet.bidding.model;

class BidTransaction extends Entity{


    //attributes
    private final String auctionId;
    private final String bidderId;
    private final long amount;
    private final long timestamp;
    private final boolean isAutoBid;



    //Constructor


    public BidTransaction(String auctionId, String bidderId, long amount,boolean isAutoBid){
        super();
        if (amount<=0) throw new IllegalArgumentException("amount must be positive");
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp=System.currentTimeMillis();
        this.isAutoBid=isAutoBid;
    }


    //Getter
    public String getAuctionId() {
        return auctionId;
    }
    public String getBidderId() {
        return bidderId;
    }
    public long getAmount() {
        return amount;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public boolean isAutoBid() {
        return isAutoBid;
    }


    @Override
    public void printInfo() {
        System.out.printf("Bid [%s] by [%s] for %,d VND at %d %s%n", auctionId, bidderId, amount, timestamp,
                isAutoBid ? "(AUTO)" : "");
    }
    public String toJson() {
        return String.format(
                "{\"type\":\"BID\",\"auctionId\":\"%s\",\"bidderId\":\"%s\"," +
                        "\"amount\":%d,\"timestamp\":%d,\"isAutoBid\":%b}",
                auctionId, bidderId, amount, timestamp, isAutoBid
        );
    }

    // tra ve chuoi kieu; {"type":"BID","auctionId":"A01","bidderId":"Hecker","amount":5000000,"timestamp":1712050000,"isAutoBid":true}
    //Dung JSON de frontend doc duoc, gui qua networksocket
}

//giai thich o day
//giai thich: %s: Dưới đây là ý nghĩa của từng "chỗ trống" trong cái khuôn của bạn:
//%s (String): Dành cho chuỗi văn bản.
//Cái %s đầu tiên sẽ được lấp bằng auctionId.
//Cái %s thứ hai sẽ được lấp bằng bidderId.
//%,d (Decimal với dấu phẩy) Chữ d là để điền một số nguyên (amount - số tiền).
//Dấu phẩy , yêu cầu Java tự động thêm dấu phân cách hàng nghìn. Ví dụ: Số 1500000 sẽ tự động biến thành 1,500,000
//%d (Decimal): Chỉ là điền một số nguyên bình thường không có dấu phẩy (ở đây là biến timestamp - thời gian).
//%n (Newline): Ký tự xuống dòng an toàn
//isAutoBid ? "(AUTO)" : "" là sao?
//Đây gọi là Toán tử 3 ngôi (Ternary Operator), một cách viết tắt cực nhanh của if-else:
//Nó đang hỏi: "Người dùng này có đang bật chế độ Tự động đấu giá (isAutoBid) không?"
//Nếu Có (true) Nó ném chữ "(AUTO)" vào chỗ trống %s cuối cùng.
//Nếu Không (false)  Nó ném một chuỗi rỗng "" (không in ra gì cả).