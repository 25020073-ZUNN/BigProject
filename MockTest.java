import com.auction.network.client.AuctionPayloadMapper;
import com.auction.model.Auction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class MockTest {
    public static void main(String[] args) throws Exception {
        String json = Files.readString(Paths.get("sync.log"));
        // Extract the JSON part from "Received: {...}"
        json = json.substring(json.indexOf("{"));
        
        Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();
            
        Map<String, Object> message = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        List<Map<String, Object>> rawAuctions = (List<Map<String, Object>>) payload.get("auctions");
        
        System.out.println("Raw size: " + rawAuctions.size());
        
        List<Auction> mapped = AuctionPayloadMapper.toAuctions(rawAuctions);
        System.out.println("Mapped size: " + mapped.size());
    }
}
