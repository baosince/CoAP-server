package com.github.dmtk;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TestDS18B20 {

    
    @Test
    public void testParse() {
        String s = "99 01 4b 46 7f ff 07 10 79 : crc=79 YES\n99 01 4b 46 7f ff 07 10 79 t=25562";
        assertTrue(DS18B20.parseTemp(s) == 25.562);
    
    }
}
