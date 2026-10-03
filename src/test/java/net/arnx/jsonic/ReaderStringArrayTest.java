package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ReaderStringArrayTest {
    private static String outcome(JSON json, String input, Type type, int chunk, boolean stream) {
        try {
            Object value;
            if (stream) value = json.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), type);
            else value = json.parse(new StringReader(input) {
                @Override public int read(char[] chars, int offset, int length) throws java.io.IOException {
                    return super.read(chars, offset, Math.min(chunk, length));
                }
            }, type);
            return value == null ? "null" : Arrays.deepToString((Object[])value);
        } catch (JSONException error) {
            StringBuilder result = new StringBuilder(error.getErrorCode() + ":" + error.getMessage()
                    + ":" + error.getLineNumber() + ":" + error.getColumnNumber() + ":" + error.getErrorOffset());
            for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
                result.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return result.toString();
        } catch (java.io.IOException error) {
            return error.getClass().getName() + ":" + error.getMessage();
        }
    }

    @Test void flatMixedNestedAndMalformedValuesKeepTheOriginalResults() {
        for (int depth : new int[] {1, 2, 3, 32, 65}) {
            for (int chunk : new int[] {1, 7, 4096}) {
                for (Type type : new Type[] {String[].class, String[][].class}) {
                    for (String input : new String[] {"[]", "null", "[\"\",null,\"日本語😀\",\"quote\\\"\"]",
                            "[1,1.00,1e2,-0.0,true,false,null]", "[\"x\",[],[1],{\"a\":[2]}]",
                            "[{\"a\":1},\"x\"]", "[\"a\",\"bad\\q\"]", "[\"a\",]", "[\"a\"]?"}) {
                        for (boolean stream : new boolean[] {false, true}) {
                            JSON legacy = new JSON(depth) {}, actual = new JSON(depth);
                            legacy.setNumberFormat("0.00"); actual.setNumberFormat("0.00");
                            assertEquals(outcome(legacy, input, type, chunk, stream),
                                    outcome(actual, input, type, chunk, stream), input);
                        }
                    }
                    String valid = "[\"x\",null,1.00,\"escaped\\n\"]";
                    for (int length = 0; length <= valid.length(); length++) {
                        String prefix = valid.substring(0, length);
                        assertEquals(outcome(new JSON(depth) {}, prefix, type, chunk, false),
                                outcome(new JSON(depth), prefix, type, chunk, false), prefix);
                    }
                }
            }
        }
    }

    @Test void bufferBoundariesAndNestedFallbacksKeepAllElements() {
        for (int count : new int[] {0, 1, 31, 32, 33, 127, 128, 129, 1023, 1024, 1025}) {
            StringBuilder text = new StringBuilder("[");
            for (int i = 0; i < count; i++) {
                if (i > 0) text.append(',');
                if (i % 13 == 0) text.append("null");
                else text.append('"').append("item-").append(i).append('"');
            }
            String input = text.append(']').toString();
            String separator = count == 0 ? "" : ",";
            for (String value : new String[] {input, input + "?",
                    input.substring(0, input.length() - 1) + separator + "{}]",
                    input.substring(0, input.length() - 1) + separator + "[1]]"}) {
                assertEquals(outcome(new JSON() {}, value, String[].class, 4096, false),
                        outcome(new JSON(), value, String[].class, 4096, false), "count=" + count);
            }
        }
    }

    private static String genericOutcome(JSON json, String text, int mode) {
        GenericArrayType type = new GenericArrayType() {
            private int calls;
            @Override public Type getGenericComponentType() {
                calls++;
                if (mode == 1 && calls == 2) throw new IllegalStateException("component");
                return mode == 2 && calls > 1 ? Object.class : String.class;
            }
            @Override public String toString() { return "String[]"; }
        };
        return outcome(json, text, type, 7, false);
    }

    @Test void genericArrayCallbacksAndFailuresKeepTheOriginalPath() {
        for (int mode : new int[] {0, 1, 2}) {
            for (String input : new String[] {"[]", "[\"x\",null,1.00,true]", "[{},\"x\"]", "[\"x\"]?"}) {
                assertEquals(genericOutcome(new JSON() {}, input, mode), genericOutcome(new JSON(), input, mode), input);
            }
        }
    }
}
