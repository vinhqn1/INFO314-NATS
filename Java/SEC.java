import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import io.nats.client.*;

public class SEC {
  public static void main(String... args) throws Exception {
    Connection nc = Nats.connect("nats://localhost:4222");
    Dispatcher d = nc.createDispatcher((msg) -> {
        System.out.println("Received a message.");
       if (msg.getSubject().contains("_INBOX")) {
         String xml = new String(msg.getData());
         try {
           DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
           DocumentBuilder builder = factory.newDocumentBuilder();
           Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));

           document.getDocumentElement().normalize();
           Element root = document.getDocumentElement();

           NodeList buy = root.getElementsByTagName("buy");
           NodeList sell = root.getElementsByTagName("sell");

           String timestamp = root.getAttribute("sent");
           String broker = root.getAttribute("broker");
           String stock = ((Element) root.getElementsByTagName("stock").item(0)).getAttribute("symbol");

           int price = -1;
           int quantity = -1;
           int transactionValue;

           if (buy.getLength() > 0) {
              price = Integer.valueOf(((Element) buy.item(0)).getAttribute("price"));
              quantity = Integer.valueOf(((Element) buy.item(0)).getAttribute("quantity"));
           } else if (sell.getLength() > 0) {
              price = Integer.valueOf(((Element) sell.item(0)).getAttribute("price"));
              quantity = Integer.valueOf(((Element) sell.item(0)).getAttribute("quantity"));
           }

           transactionValue = price * quantity;
           
           // Assuming suspicious transaction is above $5000 or 5000 in your units.
           if (transactionValue > 5000) {
             try (FileWriter fw = new FileWriter("suspicious.log", true)) {
                 fw.write(String.format("%s, %s, %s, %s, %d, %d, %d\n", timestamp, msg.getSubject(), broker, stock, price, quantity, transactionValue));
             } catch (IOException e) {
                 e.printStackTrace();
             }
           }
         } catch (Exception e) {
            e.printStackTrace();
         }
       }
    });
    d.subscribe(">");
  }
}
