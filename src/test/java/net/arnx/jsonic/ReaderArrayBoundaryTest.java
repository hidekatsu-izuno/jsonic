package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import org.junit.jupiter.api.Test;

class ReaderArrayBoundaryTest {
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

    @Test void exactBufferBoundariesRetainValuesNestedFallbacksAndErrors() {
        for (int count : new int[] {0, 1, 31, 32, 33, 127, 128, 129, 1023, 1024, 1025}) {
            for (Class<?> type : new Class<?>[] {int[].class, long[].class, float[].class, double[].class, boolean[].class}) {
                StringBuilder text = new StringBuilder("[");
                for (int i = 0; i < count; i++) {
                    if (i > 0) text.append(',');
                    if (i % 13 == 0) text.append("null");
                    else if (type == boolean[].class) text.append((i & 1) == 0);
                    else text.append(i - count / 2);
                }
                String input = text.append(']').toString();
                String separator = count == 0 ? "" : ",";
                for (String value : new String[] {input, input + "?",
                        input.substring(0, input.length() - 1) + separator + "{}]",
                        input.substring(0, input.length() - 1) + separator + "2147483648]"}) {
                    assertEquals(outcome(new JSON() {}, value, type, 4096), outcome(new JSON(), value, type, 4096),
                            type + " count=" + count);
                }
            }
        }
    }

    private static String callbackFailure(JSON json) throws Exception {
        java.lang.reflect.GenericArrayType target = new java.lang.reflect.GenericArrayType() {
            private int calls;
            @Override public java.lang.reflect.Type getGenericComponentType() {
                if (++calls == 2) throw new IllegalStateException("component");
                return int.class;
            }
            @Override public String toString() { return "int[]"; }
        };
        try {
            json.parse(new StringReader("[]"), target);
            return "success";
        } catch (JSONException error) {
            return error.getErrorCode() + ":" + error.getMessage() + ":"
                    + error.getCause().getClass().getName() + ":" + error.getCause().getMessage();
        }
    }

    @Test void emptyArrayCallbackFailureStillDisplaysTheEmptyArray() throws Exception {
        assertEquals(callbackFailure(new JSON() {}), callbackFailure(new JSON()));
    }
}
