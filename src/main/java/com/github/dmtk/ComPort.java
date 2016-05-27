package com.github.dmtk;

import java.util.Observable;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ComPort extends Observable {

    private SerialPort serialPort;
    private String data = "";
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final static Logger log = LogManager.getLogger(ComPort.class);

    public void start(String portName) {
        try {
            init(portName);
            //Устанавливаем ивент лисенер и маску
            serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);

        } catch (SerialPortException ex) {
            log.error(ex);
        }

    }

    public void startHexReader(String portName) {
        try {
            init(portName);
            //Устанавливаем ивент лисенер и маску
            serialPort.addEventListener(new PortHexReader(), SerialPort.MASK_RXCHAR);

        } catch (SerialPortException ex) {
            log.error(ex);
        }

    }

    private void init(String portName) throws SerialPortException {

        //Передаём в конструктор имя порта
        serialPort = new SerialPort(portName);

        //Открываем порт
        serialPort.openPort();

        //Выставляем параметры
        serialPort.setParams(SerialPort.BAUDRATE_9600,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
        //Включаем аппаратное управление потоком
        //serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN
        //       | SerialPort.FLOWCONTROL_RTSCTS_OUT);
        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

    }

    private class PortReader implements SerialPortEventListener {

        String portData = "";

        @Override
        public void serialEvent(SerialPortEvent event) {

            try {
                
                portData += serialPort.readString();
                if (portData.endsWith("\n")) {
                    data = "<data>" + portData + "</data>";
                    //log.debug(data);
                    portData = "";
                    setChanged();
                    notifyObservers();
                }
            } catch (SerialPortException ex) {
                log.error(ex);
            }

        }

    }

    private class PortHexReader implements SerialPortEventListener {

        private String portdata = "";

        @Override
        public void serialEvent(SerialPortEvent event) {

            String rxChar;
            try {
                rxChar = serialPort.readHexString();
                portdata += rxChar;
                
                if (portdata.contains("BB BB") || portdata.contains("BBBB")) {

                    data = portdata.replaceAll("\\s+", "");
                    //log.debug(data);
                    setChanged();
                    notifyObservers();
                    portdata = "";

                }

            } catch (SerialPortException ex) {
                log.error(ex);
            }

        }

    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] bdata = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bdata[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return bdata;
    }

    public String getData() {

        return data;
    }

    public void write(String str) {
        try {

            byte[] arr = hexStringToByteArray(str);
            serialPort.writeBytes(arr);

        } catch (SerialPortException ex) {
            log.error(ex);
        }
    }

    public void close() {
        try {
            serialPort.closePort();
        } catch (SerialPortException ex) {
            log.error(ex);
        }

    }

}
