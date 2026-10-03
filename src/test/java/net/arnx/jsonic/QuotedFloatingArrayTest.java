package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class QuotedFloatingArrayTest {
    private static String outcome(JSON json, String input, Class<?> type, boolean reader) throws Exception {
        try {
            return JSON.encode(reader ? json.parse(new StringReader(input), type) : json.parse(input, type));
        } catch (JSONException e) {
            StringBuilder text = new StringBuilder(e.getErrorCode() + ":" + e.getMessage()
                    + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ":" + e.getErrorOffset());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                text.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return text.toString();
        }
    }

    @Test public void stringsFormatsAndErrorPathsMatchOriginalConverter() throws Exception {
        String prefix = "[" + "0,".repeat(140);
        for (String format : new String[] {null, "#,##0.00", "invalid'"}) {
            for (boolean reader : new boolean[] {false, true}) {
                for (Class<?> type : new Class<?>[] {float[].class, double[].class}) {
                    for (String value : new String[] {"", " ", " -0.0 ", "+1.25", "0x1.4p3", "1.5f",
                            "NaN", "Infinity", "-Infinity", "1e999", "1e-999", "1,234.5", "bad", "1.2junk"}) {
                        String input = prefix + '"' + value + "\"]";
                        JSON legacy = new JSON() {};
                        JSON actual = new JSON();
                        legacy.setNumberFormat(format);
                        actual.setNumberFormat(format);
                        assertEquals(outcome(legacy, input, type, reader), outcome(actual, input, type, reader), input);
                    }
                    JSON legacy = new JSON() {};
                    JSON actual = new JSON();
                    legacy.setNumberFormat(format);
                    actual.setNumberFormat(format);
                    assertEquals(outcome(legacy, "[1,null,true,false]", type, reader),
                            outcome(actual, "[1,null,true,false]", type, reader));
                }
            }
        }
    }
}
