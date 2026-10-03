package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class WideBeanIntegerTest {
    public static class Value {
        public long field;
        public Long boxed;
        private long setter;
        @JSONHint(type = String.class) public long hinted;
        public long getSetter() { return setter; }
        public void setSetter(long value) { setter = value; }
    }

    private static String outcome(JSON json, String input, boolean reader) throws Exception {
        try {
            return JSON.encode(reader ? json.parse(new StringReader(input), Value[].class) : json.parse(input, Value[].class));
        } catch (JSONException e) {
            StringBuilder text = new StringBuilder(e.getErrorCode() + ":" + e.getMessage()
                    + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ":" + e.getErrorOffset());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                text.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return text.toString();
        }
    }

    @Test public void nineteenDigitBoundsAndFallbackDiagnosticsMatchLegacy() throws Exception {
        for (boolean reader : new boolean[] {false, true}) {
            for (String number : new String[] {"999999999999999999", "1000000000000000000", "-1000000000000000000",
                    "9223372036854775807", "-9223372036854775808", "9223372036854775808", "-9223372036854775809",
                    "9999999999999999999", "10000000000000000000", "-10000000000000000000",
                    "9223372036854775807.0", "-9223372036854775808.0", "9223372036854775807e-1",
                    "-9223372036854775808e-1", "1000000000000000000e-1", "1000000000000000000e1"}) {
                String item = "{\"field\":" + number + ",\"boxed\":" + number + ",\"setter\":" + number
                        + ",\"hinted\":" + number + "}";
                for (String input : new String[] {"[" + item + "," + item + "]", "[" + item + "]?",
                        "[{\"field\":0,\"field\":" + number + "}]"}) {
                    assertEquals(outcome(new JSON() {}, input, reader), outcome(new JSON(), input, reader), input);
                }
            }
        }
    }
}
