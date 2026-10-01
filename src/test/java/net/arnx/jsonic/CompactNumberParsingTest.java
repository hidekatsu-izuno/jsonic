package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class CompactNumberParsingTest {
    public static class Values {
        public Object raw;
        public BigDecimal decimal;
        public String text;
        @JSONHint(serialized = true) public String serialized;
    }

    private static String outcome(JSON json, String input, Class<?> target) {
        try {
            Object result = json.parse(input, target);
            if (result instanceof double[]) {
                StringBuilder bits = new StringBuilder();
                for (double v : (double[])result) bits.append(Double.doubleToRawLongBits(v)).append(',');
                return bits.toString();
            }
            return JSON.encode(result);
        } catch (JSONException e) {
            return e.getErrorCode() + ":" + e.getMessage() + ":" + e.getLineNumber()
                    + ":" + e.getColumnNumber() + ":" + e.getErrorOffset() + ":"
                    + (e.getCause() == null ? "" : e.getCause().getClass().getName() + ":" + e.getCause().getMessage());
        } catch (RuntimeException e) {
            return e.getClass().getName() + ":" + e.getMessage();
        }
    }

    private static void compare(String number) {
        for (Class<?> target : new Class<?>[] {int[].class, long[].class, double[].class,
                float[].class, BigDecimal[].class, String[].class}) {
            String input = "[" + number + "]";
            assertEquals(outcome(new JSON() {}, input, target), outcome(new JSON(), input, target), input + " -> " + target);
        }
    }

    @Test public void boundariesRoundingAndErrors() {
        for (String number : new String[] {"0", "-0", "0.0", "-0.00", "1.000", "0.1", "-0.1",
                "2147483647", "2147483648", "-2147483648", "-2147483649", "2147483648.1",
                "99999999999999999.1", "9007199254740991", "9007199254740992", "9007199254740993",
                "999999999999999999", "9223372036854775807", "-9223372036854775808",
                "9223372036854775808", "0.00000000000000001", "1e3", "1e-400", "1e400",
                "1e2147483648", "01", "-01", "1.", "1e", "1e+", "--1", "1.2.3", "1+2",
                "1\n", "1\r\n", "1\t", "1 ", "1x", "1/*comment*/"}) compare(number);
        for (String input : new String[] {"[1", "[1.", "[1e", "[0.5,2,", "[1,\n?]", "[1\n,?]"}) {
            assertEquals(outcome(new JSON() {}, input, int[].class), outcome(new JSON(), input, int[].class), input);
        }
    }

    @Test public void randomizedDecimalsMatchLegacyBitsAndExactConversion() {
        Random random = new Random(872104);
        for (int i = 0; i < 3000; i++) {
            long value = random.nextLong() % 100000000000000000L;
            int scale = random.nextInt(18);
            compare(BigDecimal.valueOf(value, scale).toPlainString());
        }
    }

    @Test public void untypedHintsAndReaderKeepDecimalValues() throws Exception {
        String input = "{\"raw\":[1.00,-0.0,123.5],\"decimal\":1.00,\"text\":1.00,\"serialized\":1.00}";
        Values actual = new JSON().parse(input, Values.class);
        assertEquals(JSON.encode(new JSON() {}.parse(input, Values.class)), JSON.encode(actual));
        assertEquals(new BigDecimal("1.00"), ((java.util.List<?>)actual.raw).get(0));
        assertEquals(new BigDecimal("1.00"), actual.decimal);
        assertEquals("1.00", actual.text);
        assertEquals((Object)new JSON().parse(new StringReader(input)), (Object)new JSON().parse(input));
        JSONReader reader = new JSON().getReader("[1.00]");
        assertEquals(JSONEventType.START_ARRAY, reader.next());
        assertEquals(JSONEventType.NUMBER, reader.next());
        assertEquals(new BigDecimal("1.00"), reader.getNumber());
    }
}
