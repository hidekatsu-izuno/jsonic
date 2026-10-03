package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

public class StreamCompactNumberTest {
    public static class Value {
        public int integer;
        public long wide;
        public double number;
        public float single;
        public BigDecimal decimal;
        public Object raw;
        public String text;
        @JSONHint(serialized = true) public String serialized;
    }

    private static Reader chunks(String input, int length) {
        return new StringReader(input) {
            @Override public int read(char[] buffer, int offset, int count) throws IOException {
                return super.read(buffer, offset, Math.min(count, length));
            }
        };
    }

    private static String outcome(JSON json, String input, Class<?> type, int chunk) throws IOException {
        try {
            Object value = chunk == 0
                    ? json.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), type)
                    : json.parse(chunks(input, chunk), type);
            StringBuilder result = new StringBuilder(JSON.encode(value));
            if (value instanceof Value[]) for (Value item : (Value[])value) {
                result.append(':').append(Double.doubleToRawLongBits(item.number))
                        .append(':').append(Float.floatToRawIntBits(item.single));
                if (item.raw != null) result.append(':').append(item.raw.getClass().getName());
            }
            return result.toString();
        } catch (JSONException e) {
            StringBuilder result = new StringBuilder(e.getErrorCode() + ":" + e.getMessage()
                    + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ":" + e.getErrorOffset());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                result.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return result.toString();
        }
    }

    @Test public void streamedScalarsPreserveConversionsAtBufferBoundaries() throws Exception {
        String[] numbers = {"0", "-0", "127", "128", "-123456789012345678", "1234567890123456789",
                "9223372036854775807", "9223372036854775808", "-9223372036854775808",
                "2147483647", "2147483648", "0.00", "-0.00", "1.10", "1.00000000000000000000",
                "90071992547.40993", "1e2", "1.00e2", "1e0000", "1e999", "-1e-999",
                "0e2147483648", "1e2147483648", "1e-2147483648", "1e4294967296", "1e", "1e+",
                "1e+-2", "01", "1.", "1.1.2", "1e3x"};
        for (int chunk : new int[] {0, 1, 7, 1004}) {
            for (String field : new String[] {"integer", "wide", "number", "single", "decimal", "raw", "text", "serialized"}) {
                for (String number : numbers) {
                    String item = "{\"" + field + "\":" + number + "}";
                    String input = "[" + item + "," + item + "]";
                    assertEquals(outcome(new JSON() {}, input, Value[].class, chunk),
                            outcome(new JSON(), input, Value[].class, chunk), input + " chunk=" + chunk);
                }
            }
            for (String input : new String[] {"[0,127,128,1.10,-1e2,null]", "[1e2147483648]",
                    "[1.1,2]?", "[1e3,2e]", "[1.1,2]"}) {
                for (Class<?> type : new Class<?>[] {int[].class, long[].class, double[].class, BigDecimal[].class, Object[].class}) {
                    assertEquals(outcome(new JSON() {}, input, type, chunk),
                            outcome(new JSON(), input, type, chunk), input + " type=" + type + " chunk=" + chunk);
                }
            }
        }
    }

    @Test public void bufferedScanningRetainsRefillAndLineDiagnostics() throws Exception {
        java.util.Random random = new java.util.Random(0x14a33);
        for (int round = 0; round < 15; round++) {
            StringBuilder text = new StringBuilder("[");
            for (int i = 0; i < 400; i++) {
                if (i > 0) text.append(i % 9 == 0 ? ",\r\n" : ",");
                text.append(java.math.BigDecimal.valueOf(random.nextLong() % 100000000000000000L,
                        random.nextInt(25) - 6).toString());
            }
            String valid = text.append(']').toString();
            for (int chunk : new int[] {0, 1, 7, 1004}) {
                for (String input : new String[] {valid, valid + "?", valid.substring(0, valid.length() - 2) + "e]"}) {
                    assertEquals(outcome(new JSON() {}, input, double[].class, chunk),
                            outcome(new JSON(), input, double[].class, chunk), "round=" + round + " chunk=" + chunk);
                }
            }
        }
    }

    @Test public void bufferedScanningPreservesDetectedEncodingsAndDepthLimits() throws Exception {
        String input = "[0,-0.00,12345.625,1e3,-1.25e-3,null]";
        for (String encoding : new String[] {"UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE", "UTF-32BE", "UTF-32LE"}) {
            byte[] bytes = input.getBytes(encoding);
            double[] expected = new JSON() {}.parse(new ByteArrayInputStream(bytes), double[].class);
            assertArrayEquals(expected, new JSON().parse(new ByteArrayInputStream(bytes), double[].class), encoding);
        }
        for (int depth : new int[] {1, 2, 3, 32, 65}) {
            for (int chunk : new int[] {0, 1, 7, 1004}) {
                for (String document : new String[] {input, "[[1],2.5e3,null]", "[1,\n2]?"}) {
                    assertEquals(outcome(new JSON(depth) {}, document, double[].class, chunk),
                            outcome(new JSON(depth), document, double[].class, chunk), depth + ":" + document);
                }
            }
        }
    }

    @Test public void publicUntypedAndEventNumbersRemainBigDecimals() throws Exception {
        for (int chunk : new int[] {0, 1, 7}) {
            JSON json = new JSON();
            List<?> values = chunk == 0
                    ? json.parse(new ByteArrayInputStream("[127,1.00e2]".getBytes(StandardCharsets.UTF_8)))
                    : json.parse(chunks("[127,1.00e2]", chunk));
            assertEquals(new BigDecimal("127"), values.get(0));
            assertEquals(new BigDecimal("1.00e2"), values.get(1));
            JSONReader reader = json.getReader(chunks("[127,1.00e2]", Math.max(1, chunk)));
            assertEquals(JSONEventType.START_ARRAY, reader.next());
            assertEquals(JSONEventType.NUMBER, reader.next());
            assertEquals(new BigDecimal("127"), reader.getNumber());
            assertEquals(JSONEventType.NUMBER, reader.next());
            assertEquals(new BigDecimal("1.00e2"), reader.getNumber());
        }
    }
}
