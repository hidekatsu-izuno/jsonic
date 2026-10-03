package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class StringTreeDecoderTest {
    private static void fingerprint(StringBuilder text, Object value) {
        if (value == null) { text.append("null;"); return; }
        text.append(value.getClass().getName()).append(':');
        if (value instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
                fingerprint(text, entry.getKey());
                fingerprint(text, entry.getValue());
            }
        } else if (value instanceof List<?>) {
            for (Object element : (List<?>)value) fingerprint(text, element);
        } else if (value instanceof BigDecimal) {
            BigDecimal number = (BigDecimal)value;
            text.append(number.unscaledValue()).append('/').append(number.scale()).append('/')
                    .append(number.precision()).append('/').append(number.toString()).append('/')
                    .append(number.toEngineeringString());
        } else {
            String string = value.toString();
            text.append(string.length()).append(':').append(string);
        }
        text.append(';');
    }

    private static String outcome(JSON json, String input, int source) throws Exception {
        try {
            Object value = source == 0 ? json.parse(input) : source == 1
                    ? json.parse(new StringBuilder(input)) : json.parse(new StringReader(input));
            StringBuilder text = new StringBuilder();
            fingerprint(text, value);
            return text.toString();
        } catch (JSONException e) {
            StringBuilder text = new StringBuilder(e.getErrorCode() + ":" + e.getMessage()
                    + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ":" + e.getErrorOffset());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                text.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return text.toString();
        }
    }

    @Test public void treeTypesScalesOrderAndFallbackDiagnosticsMatchLegacy() throws Exception {
        for (String input : new String[] {"[]", "{}", " [null,true,false,0,-0.00,1.10,1e2,1.00e2] \r\n",
                "{\"b\":1,\"a\":2,\"b\":3}", "[{\"a\":1},{\"a\":2},{\"ab\":3},{\"a\":4}]",
                "[{\"\\u0061\":1},{\"\\u0061\":2},{\"a\":3}]", "{\"quote\\\"\\n\":\"日本語\\u2028\\q\"}",
                "[[{\"a\":1}],[{\"a\":2,\"b\":[null]}]]", "[9223372036854775807,-9223372036854775808]",
                "[9999999999999999999,0.123456789012345678,1e999,1e-999,1e2147483648,1e4294967296]",
                "[{a:1}]", "{\"a\":1,}", "[1,]", "[1/*comment*/,2]", "['abc']", "{\"a\":\"\\u12z4\"}",
                "[{\"a\":1},{\"a\"x:2}]", "[1e,2]", "[01,2]", "[1.1.2]", "[trueX]", "[1,2]?",
                "[1,\n2]?", "{}{}", "null", "123", "\"text\"", "", " ["}) {
            for (int depth : new int[] {1, 2, 3, 4, 32, 64, 65}) {
                for (int source = 0; source < 3; source++) {
                    assertEquals(outcome(new JSON(depth) {}, input, source), outcome(new JSON(depth), input, source), input);
                }
            }
        }
        String valid = "[{\"a\":[\"\\u0061\\n\",12.50,true,null]}]";
        for (int length = 0; length <= valid.length(); length++) {
            String prefix = valid.substring(0, length);
            assertEquals(outcome(new JSON() {}, prefix, 0), outcome(new JSON(), prefix, 0), prefix);
        }
    }

    private static String randomValue(Random random, int depth) {
        int kind = random.nextInt(depth < 5 ? 6 : 4);
        if (kind == 0) return "null";
        if (kind == 1) return random.nextBoolean() ? "true" : "false";
        if (kind == 2) return BigDecimal.valueOf(random.nextLong() % 1000000000000000000L, random.nextInt(35) - 7).toString();
        if (kind == 3) return "\"" + (random.nextBoolean() ? "text\\\"\\n\\u2028" : "日本語") + random.nextInt(5) + "\"";
        StringBuilder text = new StringBuilder(kind == 4 ? "[" : "{");
        int count = random.nextInt(5);
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            if (kind == 5) text.append("\"key").append(random.nextInt(3)).append("\":");
            text.append(randomValue(random, depth + 1));
        }
        return text.append(kind == 4 ? ']' : '}').toString();
    }

    @Test public void randomizedNestedTreesAndMalformedSuffixesMatchLegacy() throws Exception {
        Random random = new Random(0x635bb);
        for (int round = 0; round < 500; round++) {
            String input = "[" + randomValue(random, 0) + "," + randomValue(random, 0) + "]";
            for (String value : new String[] {input, input + "?", input.substring(0, input.length() - 1)}) {
                assertEquals(outcome(new JSON() {}, value, 0), outcome(new JSON(), value, 0), value);
            }
        }
    }
}
