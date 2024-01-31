package eu.giulianogorgone.fluidswipe.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class UtilsTest {
    @Test
    void testCompareOSVersionString() {
        Assertions.assertEquals(0, Utils.compareOSVersionString("0", "0"));
        Assertions.assertEquals(1, Utils.compareOSVersionString("13.6", "10.7"));
        Assertions.assertEquals(-1, Utils.compareOSVersionString("10.7", "10.7.5"));
        Assertions.assertEquals(0, Utils.compareOSVersionString("10.7.1", "10.7.1"));
        Assertions.assertEquals(1, Utils.compareOSVersionString("10.7.0.0.0.1", "10.7"));

        Assertions.assertThrowsExactly(NullPointerException.class, () -> Utils.compareOSVersionString(null, null));
        Assertions.assertThrowsExactly(NumberFormatException.class, () -> Utils.compareOSVersionString("+10", "10"));
        Assertions.assertThrowsExactly(NumberFormatException.class, () -> Utils.compareOSVersionString("a10", "1ew0"));
        Assertions.assertThrowsExactly(NumberFormatException.class, () -> Utils.compareOSVersionString("a10,32", "1ew0"));
        Assertions.assertThrowsExactly(NumberFormatException.class, () -> Utils.compareOSVersionString("10,32", "1ew0"));

        Assertions.assertEquals(Utils.compareOSVersionString("10.7.5", "10.8.1.2.3.4.5"), Utils.compareOSVersionString("10.7.5.0.0.0.0", "10.8.1.2.3.4.5"));
        Assertions.assertEquals(Utils.compareOSVersionString("10.8.1.2.3.4.5", "10.7.5"), Utils.compareOSVersionString("10.8.1.2.3.4.5", "10.7.5.0.0.0.0"));
    }
}
