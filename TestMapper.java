import com.auction.network.client.NetworkService;
import com.auction.network.client.AuctionPayloadMapper;
import com.auction.model.Auction;
import java.util.List;
import java.util.Map;

public class TestMapper {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting network service...");
        NetworkService ns = NetworkService.getInstance();
        ns.login("test1", "123456");
        List<Map<String, Object>> raw = ns.getAuctions();
        System.out.println("Raw size: " + raw.size());
        if (!raw.isEmpty()) {
            System.out.println("First raw: " + raw.get(0));
        }
        List<Auction> mapped = AuctionPayloadMapper.toAuctions(raw);
        System.out.println("Mapped size: " + mapped.size());
    }
}
