import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class TestSync {
    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", 5050);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line = reader.readLine();
            System.out.println("Received: " + line);
        }
    }
}
