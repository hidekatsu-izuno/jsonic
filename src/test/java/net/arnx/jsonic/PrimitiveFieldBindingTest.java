package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class PrimitiveFieldBindingTest {
    public static class Fields {
        public int integer;
        public long wide;
        public double decimal;
        public volatile boolean flag;
        public Integer boxed;
        @JSONHint(type = String.class) public double hinted;
        public final int fixed = 7;
        public int viaSetter;
        public void setViaSetter(int value) {
            if (value < 0) throw new IllegalArgumentException("setter failed");
            viaSetter = value + 10;
        }
    }

    private static String outcome(JSON json, String input, boolean reader) throws Exception {
        try {
            Fields[] result = reader ? json.parse(new StringReader(input), Fields[].class)
                    : json.parse(input, Fields[].class);
            StringBuilder bits = new StringBuilder(JSON.encode(result));
            if (result != null) for (Fields value : result) {
                if (value != null) bits.append(':').append(Double.doubleToRawLongBits(value.decimal));
            }
            return bits.toString();
        } catch (JSONException e) {
            StringBuilder error = new StringBuilder(e.getErrorCode() + ":" + e.getMessage());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                error.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return error.toString();
        }
    }

    private static void compare(String input) throws Exception {
        for (boolean reader : new boolean[] {false, true}) {
            assertEquals(outcome(new JSON() {}, input, reader), outcome(new JSON(), input, reader), input);
        }
    }

    @Test public void fieldConversionsPreserveValuesErrorsAndSetterPrecedence() throws Exception {
        for (String field : new String[] {"integer", "wide", "decimal", "flag", "boxed", "hinted", "fixed", "viaSetter"}) {
            for (String value : new String[] {"null", "true", "false", "0", "-0.0", "1.000", "1.5", "-1",
                    "2147483648", "-2147483649", "9007199254740993", "9223372036854775807",
                    "9223372036854775808", "1e300", "1e-400", "1e400", "\"12\"", "\"bad\"", "[]", "[2]", "{}"}) {
                String item = "{\"" + field + "\":" + value + "}";
                compare("[" + item + "," + item + "]");
            }
        }
        Fields item = new JSON().parse("[{\"viaSetter\":3}]", Fields[].class)[0];
        assertEquals(13, item.viaSetter);
    }

    @Test public void arrayDispatchRetainsNullScalarAndNestedFallbacks() throws Exception {
        for (String item : new String[] {"null", "3", "true", "\"text\"", "[]", "[{}]", "{}", "{\"integer\":1.5}"}) {
            compare("[{\"integer\":1}," + item + ",{\"integer\":2}]");
        }
        compare("[{\"integer\":1.5,\"integer\":2},{\"integer\":3}]");
        compare("[{\"viaSetter\":-1,\"viaSetter\":2},{\"viaSetter\":3}]");
    }
}
