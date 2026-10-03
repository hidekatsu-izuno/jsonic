package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class PrimitiveArrayContextTest {
    private static String outcome(JSON json, String input, Class<?> type, boolean reader) throws Exception {
        try {
            return JSON.encode(reader ? json.parse(new StringReader(input), type) : json.parse(input, type));
        } catch (JSONException e) {
            StringBuilder result = new StringBuilder(e.getErrorCode() + ":" + e.getMessage()
                    + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ":" + e.getErrorOffset());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                result.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return result.toString();
        }
    }

    @Test public void successfulValuesAndFailuresAtLargeIndicesRetainPaths() throws Exception {
        StringBuilder prefix = new StringBuilder("[");
        for (int i = 0; i < 140; i++) prefix.append(i).append(',');
        for (boolean reader : new boolean[] {false, true}) {
            for (Class<?> type : new Class<?>[] {double[].class, float[].class, int[].class, long[].class, boolean[].class}) {
                for (String suffix : new String[] {"null]", "\"bad\"]", "1.1]", "2147483648]",
                        "9223372036854775808]", "1e999]", "false]", "[1]]", "1]?"}) {
                    String input = prefix + suffix;
                    for (int depth : new int[] {1, 2, 3, 32, 65}) {
                        assertEquals(outcome(new JSON(depth) {}, input, type, reader),
                                outcome(new JSON(depth), input, type, reader), type + ":" + depth + ":" + suffix);
                    }
                }
            }
            for (String input : new String[] {"[[1,null],[2,\"bad\"]]", "[[1],[2,1.1]]", "[[1],[2,null]]"}) {
                for (Class<?> type : new Class<?>[] {double[][].class, float[][].class, int[][].class,
                        long[][].class, boolean[][].class}) {
                    assertEquals(outcome(new JSON() {}, input, type, reader),
                            outcome(new JSON(), input, type, reader), type + ":" + input);
                }
            }
        }
    }
}
