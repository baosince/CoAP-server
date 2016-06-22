package com.github.dmtk;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Observable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DS18B20 extends Observable implements HardwareInterface {

    private final static Logger log = LogManager.getLogger(DS18B20.class);
    private static String data = "";
    private static DS18B20 ds18b20;

    private DS18B20() {
        DS18B20Listener.start();
    }

    public static DS18B20 getInstance() {
        
            if (ds18b20 == null) {
                ds18b20 = new DS18B20();
            }
        
        return ds18b20;
    }

    static class DS18B20Listener {

        static void start() {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (true) {
                        try {
                            data = DS18B20.parseTemp(DS18B20.read()) + "";
                            ds18b20.setChanged();
                            ds18b20.notifyObservers();
                            Thread.sleep(750);
                        } catch (InterruptedException ex) {
                            log.error(ex);
                        }
                    }

                }
            });
            t.start();
        }

    }

    public static String read() {
        String fileName = "/sys/bus/w1/devices/28-0000064948a8/w1_slave";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

        } catch (IOException ex) {
            log.error(ex);
        }
        return sb.toString();
    }

    public static double parseTemp(String s) {
        String temp = s.substring(s.indexOf("t=") + 2);
        double result = Double.parseDouble(temp);
        return result / 1000;
    }

    @Override
    public String getData() {
        return data;
    }

}
