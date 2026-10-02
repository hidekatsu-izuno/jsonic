package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class PrimitiveArrayBindingTest {
    private static String outcome(JSON json, String input, Class<?> type, boolean reader) throws Exception {
        try {
            Object value = reader ? json.parse(new StringReader(input), type) : json.parse(input, type);
            return JSON.encode(value);
        } catch (JSONException e) {
            StringBuilder message = new StringBuilder(e.getErrorCode() + ":" + e.getMessage());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                message.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return message.toString();
        }
    }

    @Test public void scalarFallbacksAndNestedErrorPathsMatchLegacy() throws Exception {
        for (Class<?> type : new Class<?>[] {int[].class, long[].class, double[].class, boolean[].class,
                int[][].class, long[][].class, double[][].class, boolean[][].class}) {
            for (String value : new String[] {"null", "true", "false", "1", "1.5", "1e30", "1e400",
                    "2147483648", "9223372036854775808", "\"2\"", "\"bad\"", "[]", "{}", "[null]", "[1,2]"}) {
                String input = type.getComponentType().isArray() ? "[[1],[0," + value + "]]" : "[0," + value + "]";
                for (boolean reader : new boolean[] {false, true}) {
                    assertEquals(outcome(new JSON() {}, input, type, reader),
                            outcome(new JSON(), input, type, reader), type + ": " + input);
                }
            }
            assertEquals(outcome(new JSON() {}, "[]", type, false), outcome(new JSON(), "[]", type, false));
        }
    }
}
