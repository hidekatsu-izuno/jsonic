package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class BooleanArrayDecoderTest {
    private static String outcome(JSON json, String input, Class<?> type, int source) throws Exception {
        try {
            Object result = source == 0 ? json.parse(input, type) : source == 1
                    ? json.parse(new StringBuilder(input), type) : json.parse(new StringReader(input), type);
            return JSON.encode(result);
        } catch (JSONException e) {
            StringBuilder text = new StringBuilder(e.getErrorCode() + ":" + e.getMessage()
                    + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ":" + e.getErrorOffset());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                text.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return text.toString();
        }
    }

    @Test public void completeValidationDepthAndFallbacksMatchLegacy() throws Exception {
        for (String input : new String[] {"[]", " [true,false,null] \r\n", "[null,null]", "null", "true",
                "[true,]", "[true,false]?", "[true,\nfalsex]", "[trueX]", "[nullx]", "[False]", "[true false]",
                "[true,\"false\",\"no\",\"1\",\"yes\",\"\"]", "[false,[true],{},null]", "[true,0,0.0,-0.0,0e0,0e1,0.0e1]",
                "[true,1e999]", "[true,1e2147483648]", "[true,1e]", "[true,0?]", "[true/*comment*/,false]"}) {
            for (int depth : new int[] {1, 2, 3, 32, 65}) {
                for (int source = 0; source < 3; source++) {
                    for (Class<?> type : new Class<?>[] {boolean[].class, Boolean[].class, boolean[][].class}) {
                        assertEquals(outcome(new JSON(depth) {}, input, type, source),
                                outcome(new JSON(depth), input, type, source), input);
                    }
                }
            }
        }
        String valid = "[true,null,false,true]";
        for (int length = 0; length <= valid.length(); length++) {
            String input = valid.substring(0, length);
            assertEquals(outcome(new JSON() {}, input, boolean[].class, 0), outcome(new JSON(), input, boolean[].class, 0));
        }
        assertArrayEquals(new boolean[] {true, false, false}, new JSON().parse("[true,null,false]", new TypeReference<boolean[]>() {}));
        for (String number : new String[] {"0", "-0", "1", "-1", "999999999999999999", "-999999999999999999",
                "1000000000000000000", "9223372036854775808", "00", "01", "-01", "0.0", "0.0e1", "0e1"}) {
            String input = "[false," + number + ",true]";
            assertEquals(outcome(new JSON() {}, input, boolean[].class, 0), outcome(new JSON(), input, boolean[].class, 0));
        }
    }

    @Test public void randomizedArraysGrowingPastInitialCapacityMatchLegacy() throws Exception {
        Random random = new Random(0x475a1);
        for (int round = 0; round < 300; round++) {
            StringBuilder text = new StringBuilder("[");
            int count = random.nextInt(600);
            for (int i = 0; i < count; i++) {
                if (i > 0) text.append(random.nextInt(9) == 0 ? ",\r\n" : ",");
                int value = random.nextInt(3);
                text.append(value == 0 ? "null" : value == 1 ? "true" : "false");
            }
            String input = text.append(']').toString();
            for (String value : new String[] {input, input + "?", input.substring(0, input.length() - 1)}) {
                assertEquals(outcome(new JSON() {}, value, boolean[].class, 0), outcome(new JSON(), value, boolean[].class, 0));
            }
        }
    }

    @Test public void quotedValuesAndIncompleteTokensMatchLegacy() throws Exception {
        for (String input : new String[] {"[\"true\",\"false\",null,true,0,1]",
                "[\"true\",\" false \",\"TRUE\",\"NaN\",\"nan\",\"0.0\",\"\"]",
                "[\"true\",\"\\u0066alse\"]", "[\"true\",\"false\"]?",
                "[\"true\"x]", "[\"false\" true]", "[\"true\",\"\\q\"]",
                "[\"true\",\"[\"]", "[\"true\",\"{\"]"}) {
            for (int depth : new int[] {1, 2, 3, 32, 65}) {
                assertEquals(outcome(new JSON(depth) {}, input, boolean[].class, 0),
                        outcome(new JSON(depth), input, boolean[].class, 0), input);
            }
        }
        String input = "[\"true\",null,\"false\",true,0]";
        for (int length = 0; length <= input.length(); length++) {
            String prefix = input.substring(0, length);
            assertEquals(outcome(new JSON() {}, prefix, boolean[].class, 0),
                    outcome(new JSON(), prefix, boolean[].class, 0), prefix);
        }
        Random random = new Random(0x7163a);
        for (int round = 0; round < 100; round++) {
            StringBuilder text = new StringBuilder("[");
            for (int i = 0, count = random.nextInt(600); i < count; i++) {
                if (i > 0) text.append(',');
                String[] tokens = {"\"true\"", "\"false\"", "null", "true", "false", "0", "1"};
                text.append(tokens[random.nextInt(tokens.length)]);
            }
            String value = text.append(']').toString();
            assertEquals(outcome(new JSON() {}, value, boolean[].class, 0),
                    outcome(new JSON(), value, boolean[].class, 0), value);
        }
    }

    @Test public void customPostparseIsStillInvoked() {
        class CustomJSON extends JSON {
            int calls;
            @Override protected <T> T postparse(Context context, Object value, Class<? extends T> type, Type genericType) throws Exception {
                if (type == boolean.class) calls++;
                return super.postparse(context, value, type, genericType);
            }
        }
        CustomJSON json = new CustomJSON();
        assertArrayEquals(new boolean[] {true, false, false}, json.parse("[true,null,false]", boolean[].class));
        assertEquals(3, json.calls);
    }
}
