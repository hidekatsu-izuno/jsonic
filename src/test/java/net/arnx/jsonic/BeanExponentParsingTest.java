package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class BeanExponentParsingTest {
    public static class Values {
        public int integer;
        public long wide;
        public double number;
        public float single;
        public BigDecimal decimal;
        public Object raw;
        public String text;
        @JSONHint(serialized = true) public String serialized;
    }

    private static String outcome(JSON json, String input) {
        try {
            Values[] values = json.parse(input, Values[].class);
            StringBuilder result = new StringBuilder(JSON.encode(values));
            if (values != null) for (Values value : values) {
                if (value != null) result.append(':').append(Double.doubleToRawLongBits(value.number))
                        .append(':').append(Float.floatToRawIntBits(value.single));
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

    @Test public void exponentBindingPreservesAllScalarConversionsAndDiagnostics() {
        for (String field : new String[] {"integer", "wide", "number", "single", "decimal", "raw", "text", "serialized"}) {
            for (String number : new String[] {"1e0", "1.00e0", "1e+3", "1E-3", "0e999", "-1e-999", "1e999",
                    "123456789012345678e-12", "2147483647e0", "2147483648e0", "9223372036854775807e0",
                    "1.1e3", "1.1e-3", "-0.00e3", "1e000", "1e0000", "1e2147483648", "1e-2147483648",
                    "1e", "1e+", "1e-", "1e+-3", "1e3.4", "1e3x"}) {
                String item = "{\"" + field + "\":" + number + "}";
                String input = "[" + item + "," + item + "]";
                assertEquals(outcome(new JSON() {}, input), outcome(new JSON(), input), input);
            }
        }
        for (String input : new String[] {"[{\"integer\":1e999,\"integer\":2}]",
                "[{\"number\":1e3},{\"number\":2e3}]?", "[{\"number\":1e3},{\"number\":2e}]"}) {
            assertEquals(outcome(new JSON() {}, input), outcome(new JSON(), input), input);
        }
    }
}
