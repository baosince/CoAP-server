package com.github.dmtk;

import java.io.IOException;
import java.io.StringReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SensorNetworkServer extends CoapServer implements Observer {

    private final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
    private final ComPort port = new ComPort();
    private final ComPort port2 = new ComPort();
    private final DS18B20 ds18b20 = DS18B20.getInstance();
    private final byte networkSize = 4;
    private final Map<Integer, SensorNode> nodes = new HashMap<>();
    private final Uart u;
    private final static Logger log = LogManager.getLogger(SensorNetworkServer.class);

    public SensorNetworkServer() throws SocketException {

        port.startHexReader("/dev/ttyS2");
        port2.start("/dev/ttyS3");
        //port.start("/dev/ttyACM0");

        SensorNode sn1 = new SensorNode(1, CentralSensorNodeResource.values());
        nodes.put(1, sn1);
        add(sn1);

        for (int i = 2; i <= networkSize; i++) {
            SensorNode sn = new SensorNode(i);
            nodes.put(i, sn);
            add(sn);
        }

        u = new Uart();
        add(u);
        addEndpoints();
        port.addObserver(this);
        port2.addObserver(this);
        ds18b20.addObserver(this);

    }

    /**
     * Add individual endpoints listening on default CoAP port on all IPv4
     * addresses of all network interfaces.
     */
    private void addEndpoints() {
        for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
            // only binds to IPv4 addresses and localhost
            if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
                InetSocketAddress bindToAddress = new InetSocketAddress(addr, COAP_PORT);
                addEndpoint(new CoapEndpoint(bindToAddress));
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {

        String line = ((HardwareInterface) o).getData();

        if (line.contains("AAAA")) {
            parseWSNMessage(line);
        }else if (line.endsWith(">")) {
            parseXMLData(line);
        }else{
            double value = Double.parseDouble(line);
            nodes.get(1).update(CentralSensorNodeResource.temperature_DS18B20, value);
        }
    }
    
    

    private void parseWSNMessage(String line) {
        u.update(line);
        Pattern p = Pattern.compile("AAAA([A-F,0-9]{2,})BBBB");
        Matcher m = p.matcher(line);
        String data = "";
        if (m.find()) {
            data = m.group(1);
        }

        List<String> list = new ArrayList<>();
        Pattern p2 = Pattern.compile("([A-F,0-9]{2})");
        Matcher m2 = p2.matcher(data);
        while (m2.find()) {
            list.add(m2.group(1));
        }

        if (list.get(0).equals("CC")) {
            u.update(line);
        } else if (list.get(3).equals("84")) {

            int id = Integer.parseInt(list.get(2), 16);
            SensorNode sn = nodes.get(id);

            int lastIndex = list.size() - 1;

            String hexTemp = list.get(lastIndex - 1) + list.get(lastIndex);
            int tempCode = Integer.parseInt(hexTemp, 16);
            double temp;
            if (tempCode < 32768) {
                temp = tempCode * 0.0625;
            } else {
                tempCode = (~(short) tempCode) + 1;
                temp = tempCode * 0.0625 * (-1);
            }
            sn.update(ResourceName.temperature, temp);
            String hexVoltage = list.get(lastIndex - 3) + list.get(lastIndex - 2);
            double voltage = ((double) Integer.parseInt(hexVoltage, 16)) / 1000;//mV to V
            sn.update(ResourceName.voltage, voltage);
            String hexAnalogValue = list.get(lastIndex - 5) + list.get(lastIndex - 4);
            double analogValue = Integer.parseInt(hexAnalogValue, 16);
            sn.update(ResourceName.analogValue, analogValue);

        } else if (list.get(0).equals("07")) {
            int lastIndex = list.size() - 1;
            u.update("New node #" + list.get(lastIndex - 1) + " join");
        } else if (list.get(0).equals("DD")) {
            int networkSize = Integer.parseInt(list.get(1), 16);
            u.update("Network size = " + networkSize);
        } else if (list.get(0).equals("05")) {
            int nodeId = Integer.parseInt(list.get(2), 16);
            u.update("Node discovery = " + nodeId);

        }
    }

    private void parseXMLData(String line) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(line));
            Document doc = db.parse(is);
            NodeList xmlnodes = doc.getElementsByTagName("value");

            for (int i = 0; i < xmlnodes.getLength(); i++) {
                Element element = (Element) xmlnodes.item(i);
                String resourceName = element.getAttribute("property") + "_" + element.getAttribute("sensor");
                double value = Double.parseDouble(element.getTextContent());
                nodes.get(1).update(resourceName, value);
            }

        } catch (ParserConfigurationException | SAXException | IOException ex) {
            System.out.println(line);
            log.error(ex + " " + line);
        }
    }

    class Uart extends CoapResource {

        String rxData = "";

        public Uart() {
            super("UART");
            setObservable(true);
        }

        @Override
        public void handleGET(CoapExchange exchange) {

            // respond to the request
            exchange.respond(rxData);

        }

        @Override
        public void handlePOST(CoapExchange exchange) {

            // respond to the request
            port.write(exchange.getRequestText());
            exchange.respond("Send");

        }

        @Override
        public void handlePUT(CoapExchange exchange) {

            // respond to the request
            exchange.respond("Put method");

        }

        @Override
        public void handleDELETE(CoapExchange exchange) {

            // respond to the request
            rxData = "";
            exchange.respond("Clean RX DATA");

        }

        public void update(String line) {

            rxData += line + " | ";
            this.changed();
        }

    }
}
