import io.nats.client.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import java.util.HashMap;
import java.io.IOException;
import javax.xml.parsers.*;

import java.io.StringReader;
public class StockBroker {

	public static String natsURL = "localhost:4222";
	public static String name;
	public static HashMap<String, Integer> currentMarket;
	public static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	public static void main(String... args) throws Exception {
		if (args.length > 0) {
			name = args[0];
		}
		currentMarket = new HashMap<>();

		new Thread(() -> getMarket()).start();
		new Thread(() -> handleClient()).start();
		System.in.read();
	}

	private synchronized static void getMarket() {
		try (Connection nc = Nats.connect(natsURL);) {
			System.out.println("connection made");
			Dispatcher d = nc.createDispatcher((msg) -> {
				String response = new String(msg.getData());
				System.out.println(response);
				try {
					DocumentBuilder builder = factory.newDocumentBuilder();	
					Document document = builder.parse(new InputSource(new StringReader(response)));
					Element root = document.getDocumentElement();
					String symbol = root.getElementsByTagName("name").item(0).getTextContent();
					int adjustedPrice = Integer.parseInt(root.getElementsByTagName("adjustedPrice").item(0).getTextContent());
					currentMarket.put(symbol, adjustedPrice);
					System.console().writer().println(currentMarket.get(symbol));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
			d.subscribe("MARKET.*");
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
	} 

	private synchronized static void handleClient() {
		try (Connection nc = Nats.connect(natsURL);) {
			System.out.println("connection made");
			Dispatcher d = nc.createDispatcher((msg) -> {
				String response = new String(msg.getData());
				System.out.println(response);
				String completeOrder = processOrder(response);
			});
			d.subscribe("BROKER."+name);
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
	}

	private static String processOrder(String order) {
		// <order><buy symbol="(name)" amount="(number of shares)" /></order>

		// <orderReceipt><(original order here)><complete amount="(amount after fee)" /></orderReceipt>
		// <orderReceipt><sell symbol="MSFT" amount="40" /><complete amount="180000" /></orderReceipt>
		// <orderReceipt><buy symbol="MSFT" amount="40" /><complete amount="180000" /></orderReceipt>
		String symbol;
		int shares;
		String buySell;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();	
			Document document = builder.parse(new InputSource(new StringReader(order)));
			Element orderElement = document.getDocumentElement();
			Element orderNode = (Element) orderElement.getChildNodes().item(0);
			symbol = orderNode.getAttribute("symbol");
			shares = Integer.parseInt(orderElement.getAttribute("amount"));
			buySell = orderElement.getTagName();
			String orderString = order.replace("<order>", "").replace("</order>", "");
			String response = "<orderReceipt>" + orderString;
			int symbolPrice = currentMarket.get(symbol); 
			int total = symbolPrice * shares;
			int fee = total / 10;
			int completeAmount = 0;
			if (buySell.equals("buy")) {
				completeAmount = total + fee;
			} else if (buySell.equals("sell")) {
				completeAmount = total - fee;
			}
			response += "<complete amount=\"" + completeAmount + "\" /></orderReceipt";
			return response;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return "";
	}
}