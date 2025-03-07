import io.nats.client.*;

import java.io.*;
import java.time.Duration;
import java.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;

/**
 * Take the NATS URL on the command-line.
 */
public class StockBrokerClient {
    public static Map<String, Integer> portfolio = new HashMap<>();
    public static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    //Made assumption that with buy -> it will always be below
    //Made assumption that with sell -> it will always be above
    private static Map<String, int[]> buy = new HashMap<>();
    private static Map<String, Integer> sell = new HashMap<>();
    private static Connection nc;
    public static void main(String[] args) throws IOException, InterruptedException {
        String stockBroker = "test";
        String portfolioPath = "./Clients/portfolio-1.xml";
        String strategyPath = "./Clients/strategy-1.xml";
        String client = "client-example";
        String natsURL = "localhost:4222";

        if(args.length > 1) {
            stockBroker = args[0];

        }
        if(args.length == 2) {
            natsURL = args[1];
        }
        // assumption: clients strategy and portfolio will always be in the clients folder path
        if(args.length == 5) {
            client = args[2];
            portfolioPath = "./Clients/" + client + "/" + args[3];
            strategyPath = "./Clients/" + client + "/" + args[4];
        }

        nc = Nats.connect(natsURL);
        buildPortfolio(portfolioPath);
        buildStrategy(strategyPath);
        String finalStockBroker = stockBroker;
        String finalPortfolioPath = portfolioPath;
        Dispatcher dispatcher = nc.createDispatcher((msg) -> {
            String xml = new String(msg.getData());
            DocumentBuilder dBuilder = null;
            try {
                dBuilder = dbf.newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));
                String symbol = doc.getElementsByTagName("name").item(0).getTextContent();
                String priceEle = doc.getElementsByTagName("adjustedPrice").item(0).getTextContent();
                int price = Integer.parseInt(priceEle);

                // need to validate if client has enough stock to sell
                boolean requestSucessful = false;
                if(sell.containsKey(symbol) && price > sell.get(symbol)){
                    requestSucessful = handleRequest("sell", finalStockBroker, symbol, portfolio.get(symbol));
                } else if(buy.containsKey(symbol) && price < buy.get(symbol)[0]) {
                    requestSucessful = handleRequest("buy", finalStockBroker, symbol, buy.get(symbol)[1]);
                }
                if(requestSucessful) {
                    updatePortfolio(finalPortfolioPath);
                }

            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }

        });
		dispatcher.subscribe("MARKET.*.*");
    }
    public static void updatePortfolio(String portfolioPath) throws ParserConfigurationException {

        DocumentBuilder docBuilder = dbf.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("portfolio");
        doc.appendChild(rootElement);
        for(String symbol: portfolio.keySet()) {
            Element stock = doc.createElement("stock");
            stock.setAttribute("symbol", symbol);
            stock.setTextContent(Integer.toString(portfolio.get(symbol)));

            rootElement.appendChild(stock);
        }


        //...create XML elements, and others...

        // write dom document to a file
        try (FileOutputStream output =
                     new FileOutputStream(portfolioPath)) {
            writeXml(doc, output);
        } catch (IOException | TransformerException e) {
            e.printStackTrace();
        }
    }

    public static boolean handleRequest(String method, String stockBroker, String symbol, int shares) {
        if(shares > 0) {
            StringBuilder request = new StringBuilder();
            request.append("<order>");
            request.append("<");
            request.append(method);
            request.append(" symbol=\"");
            request.append(symbol);
            request.append("\" amount=\"");
            request.append(shares);
            request.append("\" /></order>");
            String send = request.toString();
            System.out.println(send);
            try {
                Message msg = nc.request("BROKER."+stockBroker, send.getBytes(), Duration.ofSeconds(100));
                System.out.println(new String(msg.getData()));
                if(method.equals("sell")) {
                    portfolio.put(symbol, 0);
                } else {
                    portfolio.put(symbol, portfolio.get(symbol) + shares);
                }
            return true;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

        return false;

    }
    public static void buildPortfolio(String portfolioPath) {
        try {
            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(portfolioPath));
            NodeList list = doc.getElementsByTagName("stock");
            for(int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node;

                    // get symbol's attribute
                    String symbol = element.getAttribute("symbol");
                    // get text
                    int amount = Integer.parseInt(node.getTextContent());
                    portfolio.put(symbol, amount);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }
    public static void buildStrategy(String strategyPath) {
        try {
            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(strategyPath));
            NodeList list = doc.getElementsByTagName("when");
            for(int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node;

                    // get symbol's attribute
                    String symbol = element.getElementsByTagName("stock").item(0).getTextContent();
                    if(element.getElementsByTagName("below").getLength() == 1) {
                        String below = element.getElementsByTagName("below").item(0).getTextContent();
                        String amount = element.getElementsByTagName("buy").item(0).getTextContent();
                        int[] items = new int[2];
                        items[0] = Integer.parseInt(below);
                        items[1] = Integer.parseInt(amount);
                        buy.put(symbol, items);
                    } else {
                        String above = element.getElementsByTagName("above").item(0).getTextContent();
                        sell.put(symbol, Integer.parseInt(above));
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }
    private static void writeXml(Document doc,
                                 OutputStream output)
            throws TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);

    }
}

