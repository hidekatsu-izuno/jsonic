package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Member;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class BeanPropertiesTest {
    public static class Bean {
        public String firstName = "Alice";
        public transient String secret = "hidden";
        @JSONHint(name = "quote\"\\\n\u2028", ordinal = 0)
        public int number = 7;
        @JSONHint(ignore = true)
        public int ignored = 9;
        private String lastName = "Smith";
        public String getLastName() { return lastName; }
        public void setLastName(String value) { lastName = value; }
    }

    public static class Box<T> {
        public T value;
        public List<T> values;
    }

    public static class FormattedBean {
        public BigDecimal amount = new BigDecimal("1234.5");
        public String optional;
    }

    @Test
    public void namingSettingsAndEscapedKeysRemainIndependent() {
        JSON json = new JSON();
        NamingStyle[] styles = {null, NamingStyle.NOOP, NamingStyle.LOWER_CASE,
                NamingStyle.LOWER_CAMEL, NamingStyle.LOWER_SPACE, NamingStyle.LOWER_HYPHEN,
                NamingStyle.LOWER_UNDERSCORE, NamingStyle.UPPER_CASE, NamingStyle.UPPER_CAMEL,
                NamingStyle.UPPER_SPACE, NamingStyle.UPPER_HYPHEN, NamingStyle.UPPER_UNDERSCORE};
        for (int round = 0; round < 2; round++) {
            for (NamingStyle style : styles) {
                json.setPropertyStyle(style);
                JSON reference = new JSON() {};
                reference.setPropertyStyle(style);
                Bean bean = new Bean();
                bean.firstName = "Bob";
                bean.setLastName("Jones");
                bean.number = 42;
                String expected = reference.format(bean);
                assertEquals(expected, json.format(bean));
                Bean decoded = json.parse(expected, Bean.class);
                assertEquals("Bob", decoded.firstName);
                assertEquals("Jones", decoded.getLastName());
                assertEquals(42, decoded.number);
                assertEquals(expected, json.format(decoded));
            }
        }
        // Convenience APIs must not inherit another instance's naming style.
        assertEquals(new JSON() {}.format(new Bean()), JSON.encode(new Bean()));
        assertEquals("Bob", JSON.decode("{\"firstName\":\"Bob\"}", Bean.class).firstName);
    }

    @Test
    public void dynamicSubclassHooksAreNotShared() {
        class CustomJSON extends JSON {
            String prefix = "a_";
            boolean hide;
            @Override protected String normalize(String name) { return prefix + name; }
            @Override protected boolean ignore(Context context, Class<?> target, Member member) {
                return super.ignore(context, target, member)
                        || (hide && member.getName().equals("firstName"));
            }
        }
        // Populate the standard plan before exercising the custom one.
        JSON.encode(new Bean());
        JSON.decode("{}", Bean.class);
        CustomJSON json = new CustomJSON();
        assertTrue(json.format(new Bean()).contains("\"a_firstName\""));
        assertEquals("Bob", json.parse("{\"a_firstName\":\"Bob\"}", Bean.class).firstName);
        json.prefix = "b_";
        assertTrue(json.format(new Bean()).contains("\"b_firstName\""));
        assertFalse(json.format(new Bean()).contains("\"a_firstName\""));
        assertEquals("Bob", json.parse("{\"b_firstName\":\"Bob\"}", Bean.class).firstName);
        json.hide = true;
        assertFalse(json.format(new Bean()).contains("firstName"));
        assertEquals("Alice", json.parse("{\"b_firstName\":\"Bob\"}", Bean.class).firstName);
    }

    @Test
    public void statefulNamingStyleIsNotShared() {
        class MutableStyle extends NamingStyle {
            String prefix = "a_";
            MutableStyle() { super("NOOP"); }
            @Override public String to(String value) { return prefix + value; }
        }
        MutableStyle style = new MutableStyle();
        JSON json = new JSON();
        json.setPropertyStyle(style);
        assertTrue(json.format(new Bean()).contains("\"a_firstName\""));
        assertEquals("Bob", json.parse("{\"a_firstName\":\"Bob\"}", Bean.class).firstName);
        style.prefix = "b_";
        assertTrue(json.format(new Bean()).contains("\"b_firstName\""));
        assertEquals("Bob", json.parse("{\"b_firstName\":\"Bob\"}", Bean.class).firstName);
    }

    @Test
    public void genericTypesAreResolvedForEachTargetType() {
        for (int i = 0; i < 3; i++) {
            Box<Integer> integers = JSON.decode("{\"value\":12,\"values\":[1,2]}",
                    new TypeReference<Box<Integer>>() {});
            Box<String> strings = JSON.decode("{\"value\":\"abc\",\"values\":[\"x\",\"y\"]}",
                    new TypeReference<Box<String>>() {});
            assertEquals(Integer.valueOf(12), integers.value);
            assertEquals(Arrays.asList(1, 2), integers.values);
            assertEquals("abc", strings.value);
            assertEquals(Arrays.asList("x", "y"), strings.values);
        }
    }

    @Test
    public void formattingSettingsAreNotCapturedBySharedPlans() {
        JSON json = new JSON();
        FormattedBean bean = new FormattedBean();
        String initial = json.format(bean);
        assertTrue(initial.contains("\"optional\":null"));
        json.setSuppressNull(true);
        json.setNumberFormat("00000.00");
        json.setPrettyPrint(true);
        String changed = json.format(bean);
        assertFalse(changed.contains("optional"));
        assertTrue(changed.contains("\"01234.50\""));
        assertTrue(changed.contains("\n"));
        assertEquals(initial, JSON.encode(bean));
    }

    public static class ParallelBean {
        public String firstName;
        public int itemCount;
    }

    @Test
    public void concurrentFirstUsePublishesCompletePlans() throws Exception {
        final int threads = 8;
        final CyclicBarrier start = new CyclicBarrier(threads);
        final JSON normal = new JSON();
        final JSON uppercase = new JSON();
        uppercase.setPropertyStyle(NamingStyle.UPPER_UNDERSCORE);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            java.util.ArrayList<Future<Void>> futures = new java.util.ArrayList<Future<Void>>();
            for (int i = 0; i < threads; i++) {
                final int worker = i;
                futures.add(executor.submit(new Callable<Void>() {
                    @Override public Void call() throws Exception {
                        boolean upper = worker % 2 == 0;
                        JSON json = upper ? uppercase : normal;
                        String input = upper ? "{\"FIRST_NAME\":\"Bob\",\"ITEM_COUNT\":42}"
                                : "{\"firstName\":\"Bob\",\"itemCount\":42}";
                        start.await(10, TimeUnit.SECONDS);
                        for (int j = 0; j < 100; j++) {
                            ParallelBean bean = json.parse(input, ParallelBean.class);
                            assertEquals("Bob", bean.firstName);
                            assertEquals(42, bean.itemCount);
                            assertEquals(input, json.format(bean));
                        }
                        return null;
                    }
                }));
            }
            for (Future<Void> future : futures) future.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }
}
