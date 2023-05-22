import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import io.nats.client.*;

public class SEC {
  public static void main(String... args) throws Exception {
    Connection nc = Nats.connect("nats://localhost:4222");
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    Dispatcher d = nc.createDispatcher((msg) -> {
      System.out.println("Received a message.");
      String xml = new String(msg.getData());

      try {
        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        Element root = document.getDocumentElement();

        // Parse the order receipt XML structure
        String orderId = root.getAttribute("orderId");
        String clientName = ((Element) root.getElementsByTagName("clientName").item(0)).getTextContent();
        String brokerName = ((Element) root.getElementsByTagName("brokerName").item(0)).getTextContent();
        double amount = Double.parseDouble(((Element) root.getElementsByTagName("amount").item(0)).getTextContent());

        // Process the extracted information
        System.out.println("Received order receipt:");
        System.out.println("Order ID: " + orderId);
        System.out.println("Client Name: " + clientName);
        System.out.println("Broker Name: " + brokerName);
        System.out.println("Amount: " + amount);

        // Log suspicious transactions
        if (amount > 5000) {
            try (FileWriter fw = new FileWriter("suspicions.log", true)) {
              fw.write(String.format("%s, %s, %s, %s, %.2f\n", timestamp, client, broker, order, amount));
            } catch (IOException e) {
              e.printStackTrace();
            }
          }

      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    d.subscribe(">");
  }
}
