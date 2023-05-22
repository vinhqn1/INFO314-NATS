import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import io.nats.client.*;

public class SEC {
    private static final String LOG_FILE_PATH = "suspicions.log";

    public static void main(String... args) throws Exception {
        Connection nc = Nats.connect("nats://localhost:4222");
        Dispatcher d = nc.createDispatcher((msg) -> {
            System.out.println("Received a message.");

            if (msg.getSubject().contains("_INBOX")) {
                String xml = new String(msg.getData());

                try {
                    FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    InputSource inputSource = new InputSource(new StringReader(xml));
                    Document document = builder.parse(inputSource);

                    document.getDocumentElement().normalize();
                    Element root = document.getDocumentElement();

                    NodeList buy = root.getElementsByTagName("buy");
                    NodeList complete = root.getElementsByTagName("complete");

                    String symbol = ((Element) buy.item(0)).getAttribute("symbol");
                    int amount = Integer.parseInt(((Element) complete.item(0)).getAttribute("amount"));

                    LocalDateTime timestamp = LocalDateTime.now();
                    String formattedTimestamp = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    String client = ""; // Replace with the appropriate value
                    String broker = ""; // Replace with the appropriate value
                    String orderSent = ""; // Replace with the appropriate value

                    String logEntry = String.format("[Timestamp]: %s\n[Client]: %s\n[Broker]: %s\n[Order Sent]: %s\n[Amount]: %d\n\n",
                            formattedTimestamp, client, broker, orderSent, amount);

                    fw.write(logEntry);
                    fw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        d.subscribe(">");
    }
}
