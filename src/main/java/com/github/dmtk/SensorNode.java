package com.github.dmtk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class SensorNode extends CoapResource {

    private static final String defaultnodeName = "SensorNode";
    private final static Logger log = LogManager.getLogger(SensorNode.class);

    public SensorNode(int id) {

        // set resource identifier
        super(defaultnodeName + id);
        for (ResourceName resourceName : ResourceName.values()) {
            this.add(new Resource(resourceName.toString()));
        }

    }

    public SensorNode(int id, Object[] resourceNames) {

        // set resource identifier
        super(defaultnodeName + id);
        for (Object resourceName : resourceNames) {
            this.add(new Resource(resourceName.toString()));
        }

    }

    class Resource extends CoapResource {

        private double value = 0;

        public Resource(String name) {
            super(name);
            setObservable(true);
        }

        @Override
        public void handleGET(CoapExchange exchange) {

            // respond to the request
            exchange.respond("" + value);

        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
            this.changed();
        }

    }

    public void update(Object name, double value) {

        try {
            this.getChild(name.toString()).setValue(value);
        } catch (NullPointerException ex) {
            log.error("Resource does not exist:"+name.toString());
        }
    }

    @Override
    public Resource getChild(String name) {
        return (Resource) super.getChild(name);
    }

}
