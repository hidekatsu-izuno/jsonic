package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import org.junit.jupiter.api.Test;

class ReaderFlatArrayTest {
    private static final class ChunkReader extends Reader {
        private final String input;
        private final int chunk;
        private int position;
        ChunkReader(String input, int chunk) { this.input = input; this.chunk = chunk; }
        @Override public int read(char[] target, int offset, int length) {
            if (length == 0) return 0;
            if (position == input.length()) return -1;
            int count = Math.min(Math.min(length, chunk), input.length() - position);
            input.getChars(position, position + count, target, offset);
            position += count;
            return count;
        }
        @Override public void close() {}
    }

    private static String outcome(JSON json, String input, Class<?> type, int chunk) {
        try {
            Object value = json.parse(new ChunkReader(input, chunk), type);
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
        } catch (IOException error) {
            return error.getClass().getName() + ":" + error.getMessage();
        }
    }

    @Test void flatAndNestedFallbacksRetainValuesAndErrorPrecedence() {
        for (Class<?> type : new Class<?>[] {int[].class, long[].class, float[].class, double[].class, boolean[].class,
                byte[].class, short[].class, char[].class}) {
            for (int chunk : new int[] {1, 7, 4096}) {
                for (int depth : new int[] {1, 2, 3, 32, 65}) {
                    for (String input : new String[] {"[]", "[1,2,null,-3]", "[\"-0\",-0.0,\"1.25\",1e3]",
                            "[true,false,\"true\",null]", "[0.0,0e0,\"false\"]", "[1,[],[2],{}]",
                            "[1,\"bad\"]", "[1,\"bad\"]?", "[2147483648,9223372036854775808]",
                            "[1e999,-1e-999]", "[1,{\"a\":[2]}]", "[1/*comment*/,2]", "[1,]", "null"}) {
                        assertEquals(outcome(new JSON(depth) {}, input, type, chunk),
                                outcome(new JSON(depth), input, type, chunk), type + " " + input);
                    }
                    String valid = "[1,null,\"2\",3]";
                    for (int length = 0; length <= valid.length(); length++) {
                        String prefix = valid.substring(0, length);
                        assertEquals(outcome(new JSON(depth) {}, prefix, type, chunk),
                                outcome(new JSON(depth), prefix, type, chunk), prefix);
                    }
                }
            }
        }
    }

    @Test void refillAndNumberFormatRetainLargeArrayResultsAndErrors() {
        for (Class<?> type : new Class<?>[] {int[].class, long[].class, float[].class, double[].class, boolean[].class}) {
            StringBuilder text = new StringBuilder("[");
            for (int i = 0; i < 1600; i++) {
                if (i > 0) text.append(',');
                if (i % 13 == 0) text.append("null");
                else if (type == boolean[].class) text.append((i & 1) == 0);
                else {
                    boolean quoted = (type == float[].class || type == double[].class) && (i & 1) != 0;
                    if (quoted) text.append('"');
                    text.append(i - 800);
                    if (quoted) text.append(".625\"");
                }
            }
            String valid = text.append(']').toString();
            for (int chunk : new int[] {1, 11, 4096}) {
                for (String format : new String[] {null, "0.00"}) {
                    for (String input : new String[] {valid, valid + "?", valid.substring(0, valid.length() - 1) + ",\"bad\"]",
                            valid.substring(0, valid.length() - 1) + ",{}]"}) {
                        JSON legacy = new JSON() {}, actual = new JSON();
                        legacy.setNumberFormat(format); actual.setNumberFormat(format);
                        assertEquals(outcome(legacy, input, type, chunk), outcome(actual, input, type, chunk),
                                type + " chunk=" + chunk + " format=" + format);
                    }
                }
            }
        }
    }
}
