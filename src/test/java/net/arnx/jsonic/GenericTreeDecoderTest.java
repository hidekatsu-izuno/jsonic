package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GenericTreeDecoderTest {
    private static void fingerprint(StringBuilder out, Object value) {
        if (value == null) { out.append("null;"); return; }
        out.append(value.getClass().getName()).append(':');
        if (value instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
                fingerprint(out, entry.getKey()); fingerprint(out, entry.getValue());
            }
        } else if (value instanceof Iterable<?>) {
            for (Object item : (Iterable<?>)value) fingerprint(out, item);
        } else if (value.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(value); i++) fingerprint(out, Array.get(value, i));
        } else if (value instanceof BigDecimal) {
            BigDecimal number = (BigDecimal)value;
            out.append(number.unscaledValue()).append('/').append(number.scale()).append('/').append(number.precision());
        } else out.append(value);
        out.append(';');
    }

    private static String outcome(JSON json, String input, Type type) {
        try {
            StringBuilder out = new StringBuilder();
            fingerprint(out, json.parse(input, type));
            return out.toString();
        } catch (JSONException error) {
            StringBuilder out = new StringBuilder(error.getErrorCode() + ":" + error.getMessage()
                    + ":" + error.getLineNumber() + ":" + error.getColumnNumber() + ":" + error.getErrorOffset());
            for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
                out.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return out.toString();
        }
    }

    @Test void typedTreesAndConversionErrorsMatchLegacy() {
        for (Type type : new Type[] {
                new TypeReference<List<Map<String, Object>>>() {}.getType(),
                new TypeReference<Map<String, Object>[]>() {}.getType(),
                new TypeReference<Map<String, BigDecimal>>() {}.getType(),
                new TypeReference<Map<Integer, Double>>() {}.getType(),
                new TypeReference<List<List<BigDecimal>>>() {}.getType()}) {
            for (String input : new String[] {"[]", "{}", "[null,true,1.00,1e2]",
                    "{\"z\":1.00,\"a\":[null,2],\"z\":3}",
                    "[{\"a\":1},{\"a\":2},{\"ab\":3}]", "[[{\"a\":1}]]",
                    "[9223372036854775808,0.00]", "[1e9999]", "{a:1}", "[1,]",
                    "[1/*comment*/,2]", "[1,2]?", "[1,\n2]?", "{}{}", "null", "123", ""}) {
                for (int depth : new int[] {1, 2, 3, 4, 32, 64, 65}) {
                    assertEquals(outcome(new JSON(depth) {}, input, type), outcome(new JSON(depth), input, type),
                            type + ":" + input + ":" + depth);
                }
            }
        }
    }

    @Test void prefixesAndRepeatedKeysMatchLegacy() {
        String input = "[{\"a\":[\"\\u0061\\n\",12.50,true,null]},{\"a\":1,\"a\":2}]";
        for (Type type : new Type[] {
                new TypeReference<List<Map<String, Object>>>() {}.getType(),
                new TypeReference<Map<String, Object>[]>() {}.getType()}) {
            for (int length = 0; length <= input.length(); length++) {
                String prefix = input.substring(0, length);
                assertEquals(outcome(new JSON() {}, prefix, type), outcome(new JSON(), prefix, type), prefix);
            }
        }
    }
}
