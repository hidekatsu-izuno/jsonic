package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class TypedDecoderTest {
    public static class Bean {
        public int count;
        public String firstName;
        public String[] tags;
        public Bean child;
        public Object extra;
    }

    public static class Box<T> {
        public T value;
        public T[] values;
    }

    private static final List<String> EVENTS = new ArrayList<String>();

    public static class Observed {
        private int count;
        private Observed child;
        public Observed() { EVENTS.add("new"); }
        public int getCount() { return count; }
        public void setCount(int value) { EVENTS.add("count=" + value); count = value; }
        public Observed getChild() { return child; }
        public void setChild(Observed value) { EVENTS.add("child"); child = value; }
    }

    private static JSON legacy() { return new JSON() {}; }

    private static String outcome(JSON json, String input, Type type) {
        try {
            Object result = json.parse(input, type);
            return "OK:" + JSON.encode(result);
        } catch (JSONException e) {
            return "ERROR:" + e.getErrorCode() + ":" + e.getMessage() + ":"
                    + e.getLineNumber() + ":" + e.getColumnNumber() + ":" + e.getErrorOffset()
                    + ":" + (e.getCause() == null ? "" : e.getCause().getClass().getName());
        }
    }

    private static void equivalent(String input, Type type) {
        assertEquals(outcome(legacy(), input, type), outcome(new JSON(), input, type), input);
    }

    @Test
    public void duplicatesKeepLastValueAndFirstInsertionOrder() {
        String input = "{\"count\":\"invalid\",\"child\":{\"count\":3},\"count\":2} " ;
        EVENTS.clear();
        Observed result = new JSON().parse(input, Observed.class);
        assertEquals(2, result.getCount());
        assertEquals(3, result.getChild().getCount());
        assertEquals(Arrays.asList("new", "count=2", "new", "count=3", "child"), EVENTS);
        List<String> expected = new ArrayList<String>(EVENTS);
        EVENTS.clear();
        legacy().parse(input, Observed.class);
        assertEquals(expected, EVENTS);

        EVENTS.clear();
        new JSON().parse("{\"child\":{\"count\":1},\"child\":{\"count\":2}}", Observed.class);
        assertEquals(Arrays.asList("new", "new", "count=2", "child"), EVENTS);
    }

    @Test
    public void malformedDocumentsNeverConstructBeans() {
        for (String input : Arrays.asList("{\"count\":1", "{\"count\":1} garbage",
                "[{\"count\":1},{\"count\":2", "{\"count\":1,\"ignored\": [1,}")) {
            EVENTS.clear();
            Type type = input.startsWith("[") ? Observed[].class : Observed.class;
            String expected = outcome(legacy(), input, type);
            assertTrue(expected.startsWith("ERROR:"));
            assertTrue(EVENTS.isEmpty());
            assertEquals(expected, outcome(new JSON(), input, type));
            assertTrue(EVENTS.isEmpty());
        }
    }

    @Test
    public void conversionErrorsHaveTheSameSideEffectsAndPaths() {
        for (String input : Arrays.asList("{\"child\":{\"count\":1},\"count\":\"bad\"}",
                "{\"count\":1,\"child\":{\"count\":1.5}}", "{\"count\":2147483648}")) {
            EVENTS.clear();
            String expected = outcome(legacy(), input, Observed.class);
            List<String> effects = new ArrayList<String>(EVENTS);
            EVENTS.clear();
            assertEquals(expected, outcome(new JSON(), input, Observed.class));
            assertEquals(effects, EVENTS);
        }
    }

    @Test
    public void aliasesUnknownFieldsAndUntypedSubtrees() {
        for (String input : Arrays.asList(
                "{\"firstName\":\"a\",\"first_name\":\"b\",\"firstName\":\"c\"}",
                "{\"ignored\":{\"count\":3},\"count\":4}",
                "{\"extra\":{\"a\":1,\"b\":[2,null],\"a\":3},\"child\":{\"count\":5}}",
                "{}", "null", "[]", "1", "\"a\"")) {
            equivalent(input, Bean.class);
        }
        Bean bean = JSON.decode("{\"extra\":{\"number\":1.25}}", Bean.class);
        assertEquals(LinkedHashMap.class, bean.extra.getClass());
        assertEquals(new BigDecimal("1.25"), ((Map<?, ?>)bean.extra).get("number"));
        Object[] array = JSON.decode("[{\"n\":1},[2]]", Object[].class);
        assertEquals(LinkedHashMap.class, array[0].getClass());
        assertEquals(ArrayList.class, array[1].getClass());
        Serializable[] serializable = JSON.decode("[{\"n\":1}]", Serializable[].class);
        assertEquals(LinkedHashMap.class, serializable[0].getClass());
    }

    @Test
    public void genericTypesAndAllTypedInputOverloads() throws Exception {
        Type type = new TypeReference<Box<Bean>>() {}.getType();
        String input = "{\"value\":{\"count\":3,\"firstName\":\"日本語\\n\\\"\"},\"values\":[{\"count\":4},null]}";
        equivalent(input, type);
        JSON json = new JSON();
        String expected = JSON.encode(json.parse(input, type));
        assertEquals(expected, JSON.encode(json.parse(new StringBuilder(input), type)));
        assertEquals(expected, JSON.encode(json.parse(new StringBuffer(input), type)));
        assertEquals(expected, JSON.encode(json.parse(new StringReader(input), type)));
        assertEquals(expected, JSON.encode(json.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), type)));
        equivalent("[[{\"count\":2}],null]", new TypeReference<List<Bean>[]>() {}.getType());
    }

    public static class Hints {
        @JSONHint(serialized = true)
        public String raw;
        @JSONHint(type = String.class)
        public BigDecimal decimal;
        @JSONHint(name = "renamed")
        public Bean child;
    }

    @Test
    public void hintsAndScalarConversionsUseExistingRules() {
        equivalent("{\"raw\":{\"n\":1},\"decimal\":\"1.25\",\"renamed\":{\"count\":2}}", Hints.class);
        for (String input : Arrays.asList("[]", "[null]", "[0,-1,2147483647,-2147483648]",
                "[1.25]", "[1e2]", "[2147483648]", "[\"12\"]", "{\"b\":2,\"a\":1}")) {
            equivalent(input, int[].class);
            equivalent(input, long[].class);
            equivalent(input, double[].class);
            equivalent(input, String[].class);
        }
    }

    @Test
    public void depthLimitsAndLargeDepthFallback() {
        String input = "[{\"count\":1,\"child\":{\"count\":2,\"extra\":[1,{\"n\":3}]}}]";
        for (int depth = 0; depth < 7; depth++) {
            JSON reference = legacy();
            reference.setMaxDepth(depth);
            JSON json = new JSON(depth);
            assertEquals(outcome(reference, input, Bean[].class), outcome(json, input, Bean[].class), "depth=" + depth);
        }
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 200; i++) deep.append('[');
        deep.append('0');
        for (int i = 0; i < 200; i++) deep.append(']');
        JSON reference = legacy();
        reference.setMaxDepth(256);
        assertEquals(outcome(reference, deep.toString(), Object[].class),
                outcome(new JSON(256), deep.toString(), Object[].class));
    }

    public static class Throwing {
        public int count;
        public Throwing() { EVENTS.add("throwing"); throw new IllegalStateException("constructor"); }
    }

    @Test
    public void constructorExceptionsAreNotRetried() {
        EVENTS.clear();
        String expected = outcome(legacy(), "{\"count\":1}", Throwing.class);
        assertEquals(Arrays.asList("throwing"), EVENTS);
        EVENTS.clear();
        assertEquals(expected, outcome(new JSON(), "{\"count\":1}", Throwing.class));
        assertEquals(Arrays.asList("throwing"), EVENTS);
    }

    @Test
    public void largeBuffersAndLongStrings() throws Exception {
        StringBuilder input = new StringBuilder("[");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) input.append(',');
            input.append("{\"count\":").append(i).append(",\"tags\":[\"a\",\"b\"]}");
        }
        input.append(']');
        equivalent(input.toString(), Bean[].class);
        Bean[] streamed = new JSON().parse(new StringReader(input.toString()), Bean[].class);
        assertEquals(1000, streamed.length);
        assertEquals(999, streamed[999].count);
        char[] chars = new char[100000];
        Arrays.fill(chars, 'x');
        equivalent("{\"firstName\":\"" + new String(chars) + "\"}", Bean.class);
    }

    @Test
    public void differentialInputsCoverFallbackAndErrorPaths() {
        Random random = new Random(417);
        String[] values = {"null", "true", "false", "0", "1.5", "2147483648", "\"hello\"",
                "\"日本語\\n\"", "[]", "[1,null]", "{}", "{\"count\":2}", "[\"x\",\"y\"]"};
        String[] names = {"count", "firstName", "tags", "child", "extra", "first_name", "unknown"};
        for (int trial = 0; trial < 300; trial++) {
            StringBuilder input = new StringBuilder("{");
            int count = random.nextInt(12);
            for (int i = 0; i < count; i++) {
                if (i > 0) input.append(',');
                input.append('"').append(names[random.nextInt(names.length)]).append("\":")
                        .append(values[random.nextInt(values.length)]);
            }
            input.append('}');
            equivalent(input.toString(), Bean.class);
            equivalent("[" + input + ",null," + input + "]", Bean[].class);
        }
    }
}
