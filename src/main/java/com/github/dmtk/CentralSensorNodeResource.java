package com.github.dmtk;

enum CentralSensorNodeResource {

    //temperature_BMP180("Temperature_BMP180"), pressure_BMP180("Pressure_BMP180"), ambientTemperature_TMP006("AmbientTemperature_TMP006"), objectTemperature_TMP006("ObjectTemperature_TMP006");
    temperature_DS18B20("Temperature_DS18B20");
private final String name;       

    private CentralSensorNodeResource(String s) {
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
