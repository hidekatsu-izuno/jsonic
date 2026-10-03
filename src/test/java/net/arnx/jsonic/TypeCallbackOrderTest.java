package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class TypeCallbackOrderTest {
    private static final class TrackingType implements ParameterizedType {
        final List<String> calls = new ArrayList<>();
        final Class<?> second;
        final boolean fail;
        int rawCalls;

        TrackingType(Class<?> second, boolean fail) { this.second = second; this.fail = fail; }

        @Override public Type getRawType() {
            calls.add("raw" + ++rawCalls);
            if (rawCalls == 1) return Map.class;
            if (fail) throw new IllegalStateException("second raw type");
            return second;
        }
        @Override public Type[] getActualTypeArguments() {
            calls.add("arguments");
            return new Type[] {String.class, Object.class};
        }
        @Override public Type getOwnerType() { calls.add("owner"); return null; }
        @Override public String toString() { calls.add("string"); return "TrackingType"; }
    }

    private static String outcome(JSON json, String input, TrackingType type) {
        try {
            Object value = json.parse(input, type);
            return (value == null ? "null" : value.getClass().getName() + ':' + JSON.encode(value));
        } catch (RuntimeException error) {
            StringBuilder out = new StringBuilder(error.getClass().getName()).append(':').append(error.getMessage());
            if (error instanceof JSONException) {
                JSONException jsonError = (JSONException)error;
                out.append(':').append(jsonError.getErrorCode()).append(':').append(jsonError.getLineNumber())
                        .append(':').append(jsonError.getColumnNumber()).append(':').append(jsonError.getErrorOffset());
            }
            for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
                out.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return out.toString();
        }
    }

    @Test void rawTypeCallsValuesExceptionsAndOrderMatchLegacy() {
        for (Class<?> second : new Class<?>[] {Map.class, TreeMap.class, List.class, Map[].class, null}) {
            for (boolean fail : new boolean[] {false, true}) {
                for (String input : new String[] {"{\"b\":1,\"a\":2}", "[{\"a\":1}]", "{}", "[]",
                        "[1,2]", "null", "?", "[1", "{\"a\":1,}", "{\"a\":1}?"}) {
                    TrackingType legacy = new TrackingType(second, fail);
                    TrackingType fast = new TrackingType(second, fail);
                    assertEquals(outcome(new JSON() {}, input, legacy), outcome(new JSON(), input, fast), input);
                    assertEquals(legacy.calls, fast.calls, input);
                }
            }
        }
    }

    private static final class TrackingArray implements GenericArrayType {
        final List<String> calls = new ArrayList<>();
        final Class<?> second;
        int componentCalls;
        TrackingArray(Class<?> second) { this.second = second; }
        @Override public Type getGenericComponentType() {
            calls.add("component" + ++componentCalls);
            return componentCalls == 1 ? Map.class : second;
        }
        @Override public String toString() { calls.add("string"); return "TrackingArray"; }
    }

    private static String arrayOutcome(JSON json, String input, TrackingArray type) {
        try {
            Object value = json.parse(input, type);
            return value == null ? "null" : value.getClass().getName() + ':' + JSON.encode(value);
        } catch (RuntimeException error) {
            return error.getClass().getName() + ':' + error.getMessage();
        }
    }

    @Test void genericComponentCallsMatchLegacy() {
        for (Class<?> second : new Class<?>[] {Map.class, String.class, Object.class, null}) {
            for (String input : new String[] {"[{\"a\":1}]", "{}", "[]", "[1,2]", "null", "?", "[1", "[1]?"}) {
                TrackingArray legacy = new TrackingArray(second);
                TrackingArray fast = new TrackingArray(second);
                assertEquals(arrayOutcome(new JSON() {}, input, legacy), arrayOutcome(new JSON(), input, fast), input);
                assertEquals(legacy.calls, fast.calls, input);
            }
        }
    }
}
