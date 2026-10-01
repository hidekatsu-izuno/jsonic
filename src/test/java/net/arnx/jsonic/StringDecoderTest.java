package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class StringDecoderTest {
    public static class Bean {
        public int count;
        public double price;
        public String text;
        public Bean child;
        public Object extra;
    }

    private static String outcome(JSON json, String input) {
        try {
            Bean[] beans = json.parse(input, Bean[].class);
            return JSON.encode(beans);
        } catch (JSONException e) {
            return e.getErrorCode() + ":" + e.getMessage() + ":" + e.getLineNumber()
                    + ":" + e.getColumnNumber() + ":" + e.getErrorOffset() + ":"
                    + (e.getCause() == null ? "" : e.getCause().getClass().getName() + ":" + e.getCause().getMessage());
        } catch (RuntimeException e) {
            return e.getClass().getName() + ":" + e.getMessage();
        }
    }

    private static void compare(String input) {
        assertEquals(outcome(new JSON() {}, input), outcome(new JSON(), input), input);
    }

    @Test public void escapesAndNamesMatchLegacy() {
        for (String escaped : new String[] {"", "plain", "日本語😀", "a\\nb\\tc\\rd", "\\b\\f\\/\\\\\\\"",
                "\\u0061\\uD83D\\uDE00", "\\uD800", "\\q", "\\'", "\\u007f", "a\\\nb", "a\\\u0000b"}) {
            compare("[{\"te\\u0078t\":\"" + escaped + "\",\"text\":\"" + escaped + "\"}]");
        }
        for (int c = 0; c < 128; c++) compare("[{\"text\":\"before" + (char)c + "after\"}]");
    }

    @Test public void mutatedDocumentsMatchLegacyDiagnostics() {
        Random random = new Random(93511);
        String valid = "[{\"count\":12,\"price\":123.50,\"text\":\"日本語\\nabc\",\"extra\":[true,false,null,{}]}]";
        for (int i = 0; i < 1500; i++) {
            int position = random.nextInt(valid.length() + 1);
            char c = (char)random.nextInt(128);
            compare(valid.substring(0, position) + c + valid.substring(position));
            if (position < valid.length()) compare(valid.substring(0, position) + valid.substring(position + 1));
        }
    }

    private static Object value(Random random, int depth) {
        switch (random.nextInt(depth == 0 ? 4 : 6)) {
        case 0: return null;
        case 1: return random.nextBoolean();
        case 2: return BigDecimal.valueOf(random.nextInt(10000) - 5000, random.nextInt(5));
        case 3: return "値\"\\\n😀" + random.nextInt(50);
        case 4: {
            List<Object> result = new ArrayList<>();
            for (int i = random.nextInt(5); i > 0; i--) result.add(value(random, depth - 1));
            return result;
        }
        default: {
            Map<String, Object> result = new LinkedHashMap<>();
            for (int i = random.nextInt(5); i > 0; i--) result.put("key" + i, value(random, depth - 1));
            return result;
        }
        }
    }

    @Test public void nestedValuesAndFallbacksMatchLegacy() throws Exception {
        Random random = new Random(622851);
        for (int i = 0; i < 400; i++) {
            Map<String, Object> bean = new LinkedHashMap<>();
            bean.put("count", random.nextInt(100));
            bean.put("price", BigDecimal.valueOf(random.nextLong(), random.nextInt(8)));
            bean.put("text", value(random, 0));
            bean.put("extra", value(random, 4));
            compare(JSON.encode(new Object[] {bean}));
        }
        for (String number : new String[] {"1e3", "1e-500", "1e500", "9999999999999999999999999999.00"}) {
            String input = "[{\"extra\":" + number + "}]";
            compare(input);
            assertEquals(JSON.encode(new JSON().parse(new StringReader(input), Bean[].class)),
                    JSON.encode(new JSON().parse(input, Bean[].class)));
        }
    }

    @Test public void repeatedQuotedNamesKeepEscapesPrefixesAndDepths() {
        String escaped = "{\"co\\u0075nt\":1,\"te\\u0078t\":\"one\",\"extra\":{\"x\":1}}";
        for (String last : new String[] {escaped,
                "{\"count\":2,\"text\":\"two\",\"extra\":{\"y\":2}}",
                "{\"countSuffix\":3,\"text\":\"three\"}",
                "{\"co\\u0075nt\":1,\"te\\u0078t\":\"one\",\"count\":2}",
                "{\"co\\u0075nt\":1,\"te\\u0078t\"bad:2}",
                "{\"co\\u0075nt\":1,\"te\\u0078t",
                "{\"text\":\"first\",\"count\":2}"}) {
            compare("[" + escaped + "," + escaped + "," + last + "]");
        }
        Map<String, Object> wide = new LinkedHashMap<>();
        for (int i = 0; i < 40; i++) wide.put("key" + i, i);
        Map<String, Object> bean = new LinkedHashMap<>();
        bean.put("extra", wide);
        compare(JSON.encode(new Object[] {bean, bean, bean, bean}));
    }
}
