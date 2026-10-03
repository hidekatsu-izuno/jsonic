package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.StringReader;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ReaderBooleanBufferTest {
    private static String outcome(JSON json, String text, Type type) {
        try {
            return Arrays.toString((boolean[])json.parse(new StringReader(text), type));
        } catch (JSONException error) {
            StringBuilder result = new StringBuilder(error.getErrorCode() + ":" + error.getMessage()
                    + ":" + error.getLineNumber() + ":" + error.getColumnNumber() + ":" + error.getErrorOffset());
            for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
                result.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return result.toString();
        } catch (java.io.IOException error) {
            return error.toString();
        }
    }

    private static String input(int count) {
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            text.append(i % 13 == 0 ? "null" : (i & 1) == 0 ? "true" : "false");
        }
        return text.append(']').toString();
    }

    @Test void boundariesMixedFallbacksAndErrorsRetainNullAndValuePositions() {
        for (int count : new int[] {0, 1, 31, 32, 33, 63, 64, 65, 127, 128, 129, 1023, 1024, 1025}) {
            String text = input(count);
            String separator = count == 0 ? "" : ",";
            for (int depth : new int[] {1, 2, 3, 32, 65}) {
                for (String value : new String[] {text, text + "?",
                        text.substring(0, text.length() - 1) + separator + "1]",
                        text.substring(0, text.length() - 1) + separator + "\"true\"]",
                        text.substring(0, text.length() - 1) + separator + "\"bad\"]",
                        text.substring(0, text.length() - 1) + separator + "{}]",
                        text.substring(0, text.length() - 1) + separator + "[]]"}) {
                    assertEquals(outcome(new JSON(depth) {}, value, boolean[].class),
                            outcome(new JSON(depth), value, boolean[].class), "count=" + count);
                }
            }
        }
    }

    private static String callbackFailure(JSON json, String text) {
        GenericArrayType type = new GenericArrayType() {
            private int calls;
            @Override public Type getGenericComponentType() {
                if (++calls == 2) throw new IllegalStateException("component");
                return boolean.class;
            }
            @Override public String toString() { return "boolean[]"; }
        };
        return outcome(json, text, type);
    }

    @Test void failedGenericCallbackStillReportsTheOriginalBooleanAndNullArray() {
        for (int count : new int[] {0, 31, 32, 33, 128, 1024}) {
            String text = input(count);
            assertEquals(callbackFailure(new JSON() {}, text), callbackFailure(new JSON(), text), "count=" + count);
        }
    }
}
