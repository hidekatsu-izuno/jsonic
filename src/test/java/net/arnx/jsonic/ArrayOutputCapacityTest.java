package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ArrayOutputCapacityTest {
    public static class Hinted {
        @JSONHint(serialized = true) public String[] values;
    }

    @Test void defaultStringArraysRetainNullEscapesDepthAndDestinations() throws Exception {
        String[] samples = {null, "", "plain", "日本語😀", "\"\\\n\t\0\177\u2028\u2029", "\ud800", "\udc00", "<>&"};
        for (int count : new int[]{0, 1, 2, 100}) {
            String[] values = new String[count];
            Arrays.setAll(values, i -> samples[i % samples.length]);
            for (int depth : new int[]{1, 2, 3, 32}) for (boolean pretty : new boolean[]{false, true}) {
                JSON legacy = new JSON(depth){}, actual = new JSON(depth);
                legacy.setPrettyPrint(pretty); actual.setPrettyPrint(pretty);
                for (Object input : new Object[]{values, new Object[]{values, values}}) {
                    String expected = legacy.format(input);
                    assertEquals(expected, actual.format(input));
                    StringBuilder builder = new StringBuilder("prefix:"); actual.format(input, builder);
                    assertEquals("prefix:" + expected, builder.toString());
                    StringWriter writer = new StringWriter(); actual.format(input, writer);
                    assertEquals(expected, writer.toString());
                }
            }
        }
        Hinted hinted = new Hinted(); hinted.values = new String[]{"1", "true", "null"};
        assertEquals(new JSON(){}.format(hinted), new JSON().format(hinted));
    }

    @Test void capacityEstimatesRetainHeterogeneousOutputAndGetterCalls() throws Exception {
        for (int count : new int[]{16, 100}) for (int shape = 0; shape < 4; shape++) {
            Object[] values = new Object[count];
            for (int i = 0; i < count; i++) {
                values[i] = i < 2 ? "a".repeat(2000) : shape == 0 ? null : shape == 1 ? "short"
                        : shape == 2 ? new String[]{"日本語", "\"\n"} : values;
            }
            for (int depth : new int[]{1, 2, 3, 32}) {
                JSON actual = new JSON(depth), legacy = new JSON(depth){};
                assertEquals(legacy.format(values), actual.format(values));
                StringBuilder out = new StringBuilder("prefix:"); actual.format(values, out);
                assertEquals("prefix:" + legacy.format(values), out.toString());
            }
        }
        CountingGetter[] values = new CountingGetter[100];
        Arrays.setAll(values, i -> new CountingGetter());
        CountingGetter.calls = 0;
        String expected = new JSON(){}.format(values);
        assertEquals(100, CountingGetter.calls);
        CountingGetter.calls = 0;
        assertEquals(expected, new JSON().format(values));
        assertEquals(100, CountingGetter.calls);
    }

    public static class CountingGetter {
        static int calls;
        public String getName() { calls++; return "item"; }
    }

    @Test void customPreformatStillRunsForEveryStringAndNull() {
        int[] calls = {0};
        JSON json = new JSON() {
            @Override protected Object preformat(Context context, Object value) throws Exception {
                if (value instanceof String) { calls[0]++; return "changed:" + value; }
                return super.preformat(context, value);
            }
            @Override protected Object preformatNull(Context context, java.lang.reflect.Type type) throws Exception {
                calls[0]++; return "missing";
            }
        };
        assertEquals("[\"changed:a\",\"missing\",\"changed:b\"]", json.format(new String[]{"a", null, "b"}));
        assertEquals(3, calls[0]);
    }
}
