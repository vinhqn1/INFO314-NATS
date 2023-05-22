import io.nats.client.*;
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;

/**
 * Take the NATS URL on the command-line.
 */
public class StockPublisher {
    public static String natsURL = "localhost:4222";
  public static void main(String... args) throws IOException, InterruptedException {

      if (args.length > 0) {
          natsURL = args[0];
      }

      HashMap<String, ArrayList<String>> markets = new HashMap<>();

    //   try (BufferedReader reader = new BufferedReader(new FileReader("stocks.txt"))) {
    //     String line;
    //     while ((line = reader.readLine()) != null) {
    //         String[] parts = line.split(":");
    //         if (parts.length == 2) {
    //             String market = parts[0].trim();
    //             String[] symbols = parts[1].trim().split(" ");
    //             ArrayList<String> symbolList = new ArrayList<>();
    //             for (String symbol : symbols) {
    //                 symbolList.add(market + ":" + symbol.trim());
    //             }
    //             markets.put(market, symbolList);
    //         }
    //     }
    //   } catch (IOException e) {
    //     e.printStackTrace();
    //   }
    //   System.out.println(markets.toString());

//      System.console().writer().println("Starting stock publisher....");

    //   for (Map.Entry<String, ArrayList<String>> entry : markets.entrySet()) {
    //     String market = entry.getKey();
    //     ArrayList<String> symbols = entry.getValue();

    //     StockMarket stockMarket = new StockMarket(StockPublisher::publishMessage, symbols.toArray(new String[0]));
    //     Thread thread = new Thread(stockMarket);
    //     thread.start();
    //   }

      StockMarket sm1 = new StockMarket(StockPublisher::publishMessage, "NASDAQ.AMZN", "MSFT", "GOOG");
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
    public synchronized static void publishMessage(String symbol, int adjustment, int price) {
        try (Connection nc = Nats.connect(natsURL)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            String message = "<message sent=\"" + timestamp + "\"><stock><name>" + symbol + "</name><adjustment>" + adjustment + "</adjustment><adjustedPrice>" + price + "</adjustedPrice></stock></message>";
            nc.publish("MARKET." + symbol, message.getBytes());
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    } 
}