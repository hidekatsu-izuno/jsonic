package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MapFormatterPlanTest {
    public static class Value {
        public int number;
        public String text;
    }

    public static class Hinted {
        @JSONHint(format = "000.00") public Map<String, Object> map;
    }

    private static Object values(int count) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<Object, Object> map = new LinkedHashMap<>();
            map.put(null, "ignored");
            for (int j = 0; j < 20; j++) {
                Object value;
                switch (i % 4 == 0 ? (i + j) % 7 : j % 7) {
                case 0: value = i + j; break;
                case 1: value = "text\"\n日本語"; break;
                case 2: value = (i + j) + 0.5; break;
                case 3: value = null; break;
                case 4: value = new String[] {"a", "b"}; break;
                case 5: {
                    Value bean = new Value(); bean.number = i; bean.text = "item"; value = bean; break;
                }
                default: value = Boolean.TRUE;
                }
                map.put(i % 2 == 0 ? "key" + j : j, value);
            }
            if (i % 3 == 0) map.put("cycle", map);
            values.add(map);
        }
        return values;
    }

    @Test void varyingTypesDepthCyclesPrettyAndHintsMatchLegacy() throws Exception {
        for (int count : new int[] {1, 2, 3, 100}) {
            for (int depth : new int[] {1, 2, 5, 32}) {
                for (boolean pretty : new boolean[] {false, true}) {
                    JSON fast = new JSON(depth), legacy = new JSON(depth) {};
                    fast.setPrettyPrint(pretty); legacy.setPrettyPrint(pretty);
                    fast.setSuppressNull(true); legacy.setSuppressNull(true);
                    Object data = values(count);
                    assertEquals(legacy.format(data), fast.format(data));
                    StringWriter oldWriter = new StringWriter(), newWriter = new StringWriter();
                    legacy.format(data, oldWriter); fast.format(data, newWriter);
                    assertEquals(oldWriter.toString(), newWriter.toString());
                }
            }
        }
        Hinted bean = new Hinted();
        bean.map = new LinkedHashMap<>(); bean.map.put("a", 1.5); bean.map.put("b", 2.5);
        assertEquals(new JSON() {}.format(new Hinted[] {bean, bean, bean, bean}),
                new JSON().format(new Hinted[] {bean, bean, bean, bean}));
    }

    public static class Failing {
        static int calls;
        @Override public String toString() { return "Failing"; }
        public int getValue() {
            if (++calls == 4) throw new IllegalArgumentException("getter failed");
            return calls;
        }
    }

    private static final class Key {
        final List<String> calls;
        final int id;
        Key(List<String> calls, int id) { this.calls = calls; this.id = id; }
        @Override public String toString() { calls.add("key" + id); return "key" + id; }
    }

    private static String outcome(JSON json, List<String> calls) {
        Failing.calls = 0;
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<Object, Object> map = new LinkedHashMap<>();
            map.put(new Key(calls, i), new Failing());
            map.put("id", i);
            values.add(map);
        }
        try { return json.format(values); }
        catch (JSONException error) {
            StringBuilder text = new StringBuilder(error.getErrorCode() + ":" + error.getMessage());
            for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
                text.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return text.toString();
        }
    }

    @Test void keyGetterCallsAndErrorPathsMatchLegacy() {
        List<String> oldCalls = new ArrayList<>(), newCalls = new ArrayList<>();
        String oldResult = outcome(new JSON() {}, oldCalls);
        int oldGetterCalls = Failing.calls;
        assertEquals(oldResult, outcome(new JSON(), newCalls));
        assertEquals(oldGetterCalls, Failing.calls);
        assertEquals(oldCalls, newCalls);
    }

    @Test void escapedRepeatedKeysChangingOrderAndCacheLimitMatchLegacy() throws Exception {
        String[] keys = {"\"\\\n\r\t\b\f\0\177\u2028\u2029日本語", "", "k".repeat(64),
                "k".repeat(65), "\ud800\udc00", "\ud800", "<>&"};
        List<Map<String, Object>> values = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int j = 0; j < keys.length; j++) {
                int index = i % 4 == 0 ? keys.length - 1 - j : j;
                map.put(keys[index], i + j);
            }
            values.add(map);
        }
        for (boolean pretty : new boolean[] {false, true}) {
            JSON fast = new JSON(), legacy = new JSON() {};
            fast.setPrettyPrint(pretty); legacy.setPrettyPrint(pretty);
            assertEquals(legacy.format(values), fast.format(values));
            StringWriter oldWriter = new StringWriter(), newWriter = new StringWriter();
            legacy.format(values, oldWriter); fast.format(values, newWriter);
            assertEquals(oldWriter.toString(), newWriter.toString());
        }
    }
}
