import io.nats.client.*;
import java.util.*;
import java.io.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import java.text.SimpleDateFormat;

import javax.xml.parsers.*;

public class StockMonitor {
    private static HashSet<String> trackingStocks = new HashSet<>();
    private static HashSet<String> createdFiles = new HashSet<>();
    public static void main(String... args) throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        for (String arg : args) {
            trackingStocks.add(arg);
        }
        Connection nc = Nats.connect("localhost:4222");
        Dispatcher d = nc.createDispatcher((msg) -> {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                    
                Document document = builder.parse(new ByteArrayInputStream(msg.getData()));
                            
                Element root = document.getDocumentElement();
                
                NodeList stockNameList = root.getElementsByTagName("name");
                NodeList adjustmentList = root.getElementsByTagName("adjustment");
                NodeList adjustedList = root.getElementsByTagName("adjustedPrice");
                
                if (stockNameList.getLength() != 1 || adjustmentList.getLength() != 1 || adjustedList.getLength() != 1) {
                    return;
                }
                String stockName = stockNameList.item(0).getTextContent();
                String adjustment = adjustmentList.item(0).getTextContent();
                String adjusted = adjustedList.item(0).getTextContent();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timestamp = dateFormat.format(new Date());

                if (trackingStocks.size() > 0 && !trackingStocks.contains(stockName)) {
                    return;
                }

                // write to file
                if (!createdFiles.contains(stockName)) {
                    try {
                        File myObj = new File(stockName + "-price.log");
                        myObj.createNewFile();
                        createdFiles.add(stockName);
                    } catch (IOException e) {
                        System.out.println("An error occurred.");
                        e.printStackTrace();
                    }
                }
                
                try {
                    FileWriter fileWriter = new FileWriter(stockName + "-price.log", true);
                    fileWriter.write(stockName + " " + timestamp + " " + adjustment + " " + adjusted + "\n");
                    fileWriter.close();
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                System.out.println(timestamp + " " + adjustment + " " + adjusted);
            } catch (Exception e) {
                // TODO: handle exception
            }
        });
        
        d.subscribe("stockmarket");
    }
}