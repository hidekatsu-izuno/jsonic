package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class IntegralArrayDecoderTest {
    private static String outcome(JSON json, String input, Class<?> type) {
        try {
            return JSON.encode(json.parse(input, type));
        } catch (JSONException e) {
            StringBuilder result = new StringBuilder(e.getErrorCode() + ":" + e.getMessage()
                    + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ":" + e.getErrorOffset());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                result.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return result.toString();
        }
    }

    @Test public void nativeIntegersAndFallbacksPreserveValuesAndDiagnostics() {
        for (Class<?> type : new Class<?>[] {int[].class, long[].class}) {
            for (String input : new String[] {"[]", " [0,-0,1,-1,null,127,128] \r\n", "[null,null]",
                    "[-2147483648,2147483647]", "[-2147483649,2147483648]",
                    "[-9223372036854775808,9223372036854775807]", "[-9223372036854775809]",
                    "[9223372036854775808]", "[12345678901234567890123456789]",
                    "[1.0,1e2]", "[1.1]", "[0e999]", "[1e2147483648]",
                    "[1,\"2\",3]", "[1,[2],{},null]", "[1,true]", "[1,2,]", "[01,2]",
                    "[.1,-.2]", "[1/*comment*/,2]", "[1,2]?", "[1,\n2.3.4]", "[1,2,", "[nullx]",
                    "[9223372036854775808,?]", "[2147483648,?]", "null", "1"}) {
                for (int depth : new int[] {1, 2, 3, 32, 65}) {
                    assertEquals(outcome(new JSON(depth) {}, input, type), outcome(new JSON(depth), input, type),
                            input + " type=" + type + " depth=" + depth);
                }
            }
            String valid = "[-123,null,2147483647]";
            for (int end = 0; end <= valid.length(); end++) {
                String input = valid.substring(0, end);
                assertEquals(outcome(new JSON() {}, input, type), outcome(new JSON(), input, type), input);
            }
        }
        assertArrayEquals(new int[] {1, 2}, new JSON().parse("[1,2]", new TypeReference<int[]>() {}));
        assertArrayEquals(new long[] {Long.MIN_VALUE, Long.MAX_VALUE}, new JSON().parse(
                "[-9223372036854775808,9223372036854775807]", new TypeReference<long[]>() {}));
    }

    @Test public void randomizedArraysGrowAndMatchLegacyExactly() {
        Random random = new Random(0x314ad);
        for (Class<?> type : new Class<?>[] {int[].class, long[].class}) {
            for (int round = 0; round < 300; round++) {
                StringBuilder input = new StringBuilder("[");
                int count = random.nextInt(500);
                for (int i = 0; i < count; i++) {
                    if (i > 0) input.append(',');
                    if (random.nextInt(15) == 0) input.append("null");
                    else input.append(type == long[].class ? random.nextLong() : random.nextInt());
                }
                input.append(']');
                assertEquals(outcome(new JSON() {}, input.toString(), type),
                        outcome(new JSON(), input.toString(), type), input.toString());
            }
        }
    }
}
