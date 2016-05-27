package com.github.dmtk;

enum ResourceName {

    voltage("Voltage"), temperature("Temperature"), analogValue("AnalogValue");
    
    private final String name;       

    private ResourceName(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        return (otherName == null) ? false : name.equals(otherName);
    }

    @Override
    public String toString() {
       return this.name;
    }
}
