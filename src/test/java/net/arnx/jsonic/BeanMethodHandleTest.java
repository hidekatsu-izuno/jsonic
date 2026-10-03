package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BeanMethodHandleTest {
    public static class Arguments {
        private long number;
        private String text;
        public long getNumber() { return number; }
        public void setNumber(long value) { number = value; }
        public String getText() { return text; }
        public void setText(String value) { text = value; }
    }

    private static class PrivateBean extends Arguments {
        private int own;
        public int getOwn() { return own; }
        // Invoked reflectively when parsing PrivateBean.
        @SuppressWarnings("unused")
        public void setOwn(int value) { own = value; }
    }

    public static class Throwing {
        private final int mode;
        Throwing(int mode) { this.mode = mode; }
        private void fail() throws Throwable {
            if (mode == 0) throw new IOException("checked");
            if (mode == 1) throw new IllegalArgumentException("runtime");
            if (mode == 2) throw new AssertionError("error");
            throw new Throwable("throwable");
        }
        public String getValue() throws Throwable { fail(); return null; }
        public void setValue(String value) throws Throwable { fail(); }
    }

    @FunctionalInterface private interface Call { Object run() throws Throwable; }
    private static String outcome(Call call) {
        try { return "ok:" + call.run(); }
        catch (Throwable e) {
            StringBuilder text = new StringBuilder();
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                text.append(cause.getClass().getName()).append(':').append(cause.getMessage()).append(';');
            }
            return text.toString();
        }
    }

    @Test public void getterThrowablesAndPrivateFallbackMatchReflection() throws Exception {
        JSON json = new JSON();
        BeanProperties.ReadProperty read = BeanProperties.readable(json.new Context(), Throwing.class)[0];
        assertNotNull(read.getter);
        for (int mode = 0; mode < 4; mode++) {
            Throwing target = new Throwing(mode);
            assertEquals(outcome(() -> read.property.get(target)), outcome(() -> read.get(target)));
        }
        for (BeanProperties.ReadProperty property : BeanProperties.readable(json.new Context(), PrivateBean.class)) {
            if (property.name.equals("own")) assertNull(property.getter);
        }
        PrivateBean bean = json.parse("{\"own\":7,\"number\":12}", PrivateBean.class);
        assertEquals(7, bean.getOwn());
        assertEquals(12, bean.getNumber());
        JSON custom = new JSON() {};
        assertNull(BeanProperties.readable(custom.new Context(), Arguments.class)[0].getter);
    }

    public static class Base {
        protected String value;
        @JSONHint(ignore = true) public final List<String> calls = new ArrayList<>();
        public String getValue() { calls.add("get"); return value; }
        public void setValue(String value) { calls.add("base:" + value); this.value = value; }
    }
    public static class Override extends Base {
        public void setValue(String value) { calls.add("override:" + value); this.value = value; }
    }

    @Test public void virtualDispatchAndInvocationCountsMatchLegacy() {
        String input = "[{\"value\":\"old\",\"value\":\"new\"},{\"value\":\"two\"}]";
        Override[] expected = new JSON() {}.parse(input, Override[].class);
        Override[] actual = new JSON().parse(input, Override[].class);
        for (int i = 0; i < actual.length; i++) assertEquals(expected[i].calls, actual[i].calls);
        assertEquals(new JSON() {}.format(expected), new JSON().format(actual));
        for (int i = 0; i < actual.length; i++) assertEquals(expected[i].calls, actual[i].calls);
    }
}
