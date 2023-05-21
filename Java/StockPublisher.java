import io.nats.client.*;
import java.util.*;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Take the NATS URL on the command-line.
 */
public class StockPublisher {
  public static void main(String... args) throws IOException, InterruptedException {
      String natsURL = "nats://127.0.0.1:4222";
      if (args.length > 0) {
          natsURL = args[0];
      }


      StockMarket sm1 = new StockMarket(StockPublisher::publishMessage, "AMZN", "MSFT", "GOOG");
      new Thread(sm1).start();
      StockMarket sm2 = new StockMarket(StockPublisher::publishMessage, "ACTV", "BLIZ", "ROVIO");
      new Thread(sm2).start();
      StockMarket sm3 = new StockMarket(StockPublisher::publishMessage, "GE", "GMC", "FORD");
      new Thread(sm3).start();
    }

    public synchronized static void publishDebugOutput(String symbol, int adjustment, int price){
        System.console().writer().printf("PUBLISHING %s: %d -> %f\n", symbol, adjustment, (price / 100.f));
    }
    // When you have the NATS code here to publish a message, put "publishMessage" in
    // the above where "publishDebugOutput" currently is
    public synchronized static void publishMessage(String symbol, int adjustment, int price){

        // <message sent="(timestamp)">
        //     <stock>
        //         <name>(symbol)</name>
        //         <adjustment>(amount)</adjustment>
        //         <adjustedPrice>(price after adjustment)</adjustedPrice>
        //     </stock>
        // </message>
        try {
            Connection nc = Nats.connect("localhost:4222");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            String message = "<message sent=\"" + timestamp + "\"><stock><name>" + symbol + "</name><adjustment>" + adjustment + "</adjustment><adjustedPrice>" + price + "</adjustedPrice></stock></message>";
            nc.publish("stockmarket", message.getBytes());
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
        
        // Dispatcher d = nc.createDispatcher((msg) -> {
        //     String response = new String(msg.getData(), StandardCharsets.UTF_8);
        // });
        
        // d.subscribe("subject");
    } 
}