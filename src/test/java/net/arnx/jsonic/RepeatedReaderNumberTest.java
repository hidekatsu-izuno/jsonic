package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RepeatedReaderNumberTest {
    private static String outcome(JSON json, String input, Class<?> type, boolean stream) throws Exception {
        try {
            Object value = stream ? json.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), type)
                    : json.parse(new StringReader(input), type);
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

    @Test void repeatedTokensAndScaleChangesPreserveResultsAndRawDiagnostics() throws Exception {
        for (Class<?> type : new Class<?>[] {int[].class, long[].class, float[].class, double[].class, boolean[].class}) {
            for (String value : new String[] {"100000", "100000.0", "100000.00", "1.25", "-0.0", "-1e-999"}) {
                StringBuilder text = new StringBuilder("[");
                for (int i = 0; i < 1500; i++) {
                    if (i > 0) text.append(',');
                    text.append(i % 13 == 0 ? "null" : value);
                    if (i % 19 == 0) text.append(",\n").append(value).append(", ").append(value);
                }
                String valid = text.append(']').toString();
                for (boolean stream : new boolean[] {false, true}) {
                    for (String input : new String[] {valid, valid + "?",
                            valid.substring(0, valid.length() - 1) + ",\"bad\"]",
                            valid.substring(0, valid.length() - 1) + ",1.0,1.00,{}]"}) {
                        assertEquals(outcome(new JSON() {}, input, type, stream), outcome(new JSON(), input, type, stream),
                                type + " value=" + value + " stream=" + stream);
                    }
                }
            }
        }
    }
}
