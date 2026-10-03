package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Array;
import org.junit.jupiter.api.Test;

class HighDepthPrimitiveArrayTest {
    private static String outcome(JSON json, String input, Class<?> type) {
        try {
            Object value = json.parse(input, type);
            if (value == null) return "null";
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < Array.getLength(value); i++) {
                if (type == float[].class) result.append(Float.floatToRawIntBits(Array.getFloat(value, i)));
                else if (type == double[].class) result.append(Double.doubleToRawLongBits(Array.getDouble(value, i)));
                else result.append(Array.get(value, i));
                result.append(',');
            }
            return result.toString();
        } catch (JSONException error) {
            StringBuilder result = new StringBuilder(error.getErrorCode() + ":" + error.getMessage()
                    + ":" + error.getLineNumber() + ":" + error.getColumnNumber() + ":" + error.getErrorOffset());
            for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
                result.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return result.toString();
        }
    }

    @Test void flatValuesAndFallbacksRetainLegacyBehaviorAtLargeDepths() {
        for (Class<?> type : new Class<?>[] {int[].class, long[].class, float[].class, double[].class, boolean[].class}) {
            for (int depth : new int[] {1, 2, 3, 64, 65, 1024, Integer.MAX_VALUE}) {
                for (String input : new String[] {"[]", "[1,2,-3,0,null]", "[\"1.25\",-0.0,\"-0\",1e3]",
                        "[true,false,\"true\",null]", "[2147483648,-9223372036854775808,9223372036854775807]",
                        "[9223372036854775808]", "[1.5,\"bad\"]", "[1.5,\"bad\"]?", "[1e999,-1e-999]",
                        "[0.0,0e0,\"false\"]", "[\"\\u0031\",2]", "[1,[2],{}]", "[1,]\n?",
                        "[1/*comment*/,2]", "[1,2]\n?", "[0x10,NaN]", "null"}) {
                    assertEquals(outcome(new JSON(depth) {}, input, type), outcome(new JSON(depth), input, type),
                            type + " depth=" + depth + " " + input);
                }
                String valid = "[1, null, -2, 3]";
                for (int length = 0; length <= valid.length(); length++) {
                    String prefix = valid.substring(0, length);
                    assertEquals(outcome(new JSON(depth) {}, prefix, type), outcome(new JSON(depth), prefix, type), prefix);
                }
            }
        }
    }

    @Test void largeArraysAndTrailingErrorsRetainEveryElement() {
        for (int count : new int[] {129, 1000, 4097}) {
            for (Class<?> type : new Class<?>[] {int[].class, long[].class, float[].class, double[].class, boolean[].class}) {
                StringBuilder text = new StringBuilder("[");
                for (int i = 0; i < count; i++) {
                    if (i > 0) text.append(',');
                    if (i % 13 == 0) text.append("null");
                    else if (type == boolean[].class) text.append((i & 1) == 0);
                    else {
                        boolean quoted = (type == float[].class || type == double[].class) && (i & 1) != 0;
                        if (quoted) text.append('"');
                        text.append(i - count / 2);
                        if (quoted) text.append('"');
                    }
                }
                String input = text.append(']').toString();
                for (String value : new String[] {input, input + "?"}) {
                    assertEquals(outcome(new JSON() {}, value, type), outcome(new JSON(), value, type),
                            type + " count=" + count);
                }
            }
        }
    }
}
