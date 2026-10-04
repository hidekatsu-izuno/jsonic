package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class BigIntegerSecurityTest {
    public static class Bean {
        public BigInteger value;
    }

    @Test
    void rejectsLargeExponentsThroughEveryBindingPath() {
        for (String input : new String[]{"1e100000000", "-1e2147483647", "1e-100000000", "1e-2147483647"}) {
            assertThrows(JSONException.class, () -> JSON.decode(input, BigInteger.class));
            assertThrows(JSONException.class, () -> JSON.decode(new StringReader(input), BigInteger.class));
            assertThrows(JSONException.class, () -> JSON.decode("{\"value\":" + input + "}", Bean.class));
            assertThrows(JSONException.class, () -> JSON.decode("[" + input + "]", BigInteger[].class));
            assertThrows(JSONException.class, () -> new JSON().convert(new BigDecimal(input), BigInteger.class));
        }
    }

    @Test
    void keepsExactIntegersZeroAndTheDigitBoundary() {
        assertEquals(BigInteger.TEN.pow(9999), JSON.decode("1e9999", BigInteger.class));
        assertThrows(JSONException.class, () -> JSON.decode("1e10000", BigInteger.class));
        for (String input : new String[]{"0e2147483647", "0e-2147483647"}) {
            assertEquals(BigInteger.ZERO, JSON.decode(input, BigInteger.class));
        }
        assertEquals(BigInteger.valueOf(-123), JSON.decode("-123.000", BigInteger.class));
        assertEquals(BigInteger.valueOf(100), JSON.decode("1.00e2", BigInteger.class));
        assertThrows(JSONException.class, () -> JSON.decode("1.01", BigInteger.class));
        assertThrows(JSONException.class, () -> JSON.decode("0.1", BigInteger.class));
        assertEquals(BigInteger.TEN.pow(10000), JSON.decode("\"1" + "0".repeat(10000) + "\"", BigInteger.class));
        assertEquals(new BigDecimal("1e100000000"), JSON.decode("1e100000000", BigDecimal.class));
    }
}
