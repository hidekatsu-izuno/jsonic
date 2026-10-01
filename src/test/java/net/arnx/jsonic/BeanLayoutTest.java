package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BeanLayoutTest {
    private static final List<String> EVENTS = new ArrayList<>();
    public static class Bean {
        private int count;
        private String firstName;
        private Bean child;
        public Bean() { EVENTS.add("new"); }
        public int getCount() { return count; }
        public void setCount(int count) {
            EVENTS.add("count=" + count);
            if (count < 0) throw new IllegalArgumentException("negative");
            this.count = count;
        }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { EVENTS.add("name=" + firstName); this.firstName = firstName; }
        public Bean getChild() { return child; }
        public void setChild(Bean child) { EVENTS.add("child"); this.child = child; }
    }
    public static class Box<T> { public T value; }
    public static class Mixed {
        public Box<String>[] strings;
        public Box<Integer>[] integers;
    }

    private static String outcome(JSON json, String input) {
        EVENTS.clear();
        try {
            Bean[] result = json.parse(input, Bean[].class);
            return JSON.encode(result) + ":" + EVENTS;
        } catch (JSONException e) {
            return e.getMessage() + ":" + e.getErrorOffset() + ":" + e.getCause() + ":" + EVENTS;
        }
    }

    @Test public void reorderedMissingUnknownDuplicateAndAliasKeysPreserveSideEffects() {
        String first = "{\"count\":1,\"firstName\":\"one\",\"child\":null}";
        for (String second : new String[] {
            "{\"count\":2,\"firstName\":\"two\",\"child\":{\"count\":3}}",
            "{\"child\":null,\"firstName\":\"two\",\"count\":2}", "{}", "{\"count\":2}",
            "{\"count\":2,\"firstName\":\"two\",\"child\":null,\"unknown\":true}",
            "{\"count\":-1,\"firstName\":\"two\",\"child\":null,\"count\":2}",
            "{\"count\":2,\"first_name\":\"alias\",\"firstName\":\"two\",\"child\":null}",
            "{\"count\":2,\"firstName\":\"two\",\"child\":{\"count\":-1},\"child\":null}",
            "{\"count\":-1,\"firstName\":\"two\",\"child\":null}",
            "{\"count\":1.5,\"firstName\":\"two\",\"child\":null}"}) {
            String input = "[" + first + "," + second + "," + first + "]";
            assertEquals(outcome(new JSON() {}, input), outcome(new JSON(), input), input);
        }
        String malformed = "[" + first + "," + first + "]?";
        assertEquals(outcome(new JSON() {}, malformed), outcome(new JSON(), malformed));
        assertTrue(EVENTS.isEmpty());
    }

    @Test public void layoutDoesNotCacheResolvedGenericTypes() {
        String input = "{\"strings\":[{\"value\":1},{\"value\":2}],\"integers\":[{\"value\":\"3\"},{\"value\":\"4\"}]}";
        Mixed result = new JSON().parse(input, Mixed.class);
        assertEquals("2", result.strings[1].value);
        assertEquals(Integer.valueOf(4), result.integers[1].value);
        assertEquals(JSON.encode(new JSON() {}.parse(input, Mixed.class)), JSON.encode(result));
    }

    @Test public void stringArraySpecializationRetainsCoercions() {
        for (String input : new String[] {"[]", "[null,\"text\",123.50,true]", "[[\"nested\"],{},null]"}) {
            assertArrayEquals(new JSON() {}.parse(input, String[].class), new JSON().parse(input, String[].class));
        }
    }

    @Test public void layoutIsLocalToOneDocumentAndNamingStyle() {
        JSON json = new JSON();
        for (int i = 0; i < 20; i++) {
            json.setPropertyStyle((i & 1) == 0 ? NamingStyle.LOWER_UNDERSCORE : NamingStyle.UPPER_CAMEL);
            JSON old = new JSON() {};
            old.setPropertyStyle((i & 1) == 0 ? NamingStyle.LOWER_UNDERSCORE : NamingStyle.UPPER_CAMEL);
            String name = (i & 1) == 0 ? "first_name" : "FirstName";
            String item = "{\"" + name + "\":\"value\"}";
            String input = "[" + item + "," + item + "]";
            assertEquals(outcome(old, input), outcome(json, input));
        }
    }
}
