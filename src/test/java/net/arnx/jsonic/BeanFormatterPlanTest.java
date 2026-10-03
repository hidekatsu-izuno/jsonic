package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

public class BeanFormatterPlanTest {
    public static class Bean {
        public int count = 123;
        public boolean active = true;
        public double number = 123.5;
        public String text = "quote\"\\\n日本語\u2028";
        public String missing;
        public Date date = new Date(1234567890000L);
        public BigDecimal amount = new BigDecimal("1234.50");
        public String[] tags = {"one", null, "two"};
        public int[] integers = {1, 2, 3};
        public Object dynamic;
        public ArrayList<Object> list = new ArrayList<>();
        @JSONHint(type = String.class) public int hinted = 42;
        @JSONHint(name = "quote\"\\\n") public String renamed = "value";
    }
    public static class BadBean {
        public String getValue() throws IOException { throw new IOException("getter failed"); }
        @Override public String toString() { return "BadBean"; }
    }
    public static class DateSubclass extends Date {
        private static final long serialVersionUID = 1L;
        DateSubclass() { super(987654321000L); }
    }

    private static void settings(JSON json, boolean pretty, boolean suppress, NamingStyle style, boolean formats) {
        json.setPrettyPrint(pretty);
        json.setSuppressNull(suppress);
        json.setPropertyStyle(style);
        json.setLocale(Locale.US);
        json.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (formats) { json.setNumberFormat("#,##0.000"); json.setDateFormat("yyyy-MM-dd"); }
    }

    @Test public void knownAndChangingTypesPreserveOutputAcrossSettingsAndDestinations() throws Exception {
        Bean first = new Bean();
        Bean second = new Bean();
        first.dynamic = first;
        second.dynamic = new long[] {1, 2};
        second.date = new DateSubclass();
        first.list.add("text"); second.list.add(second.tags);
        Object[] input = {first, second, first};
        for (boolean pretty : new boolean[] {false, true}) {
            for (boolean suppress : new boolean[] {false, true}) {
                for (boolean formats : new boolean[] {false, true}) {
                    for (NamingStyle style : new NamingStyle[] {null, NamingStyle.LOWER_UNDERSCORE, NamingStyle.UPPER_CAMEL}) {
                        JSON reference = new JSON() {};
                        JSON fast = new JSON();
                        settings(reference, pretty, suppress, style, formats);
                        settings(fast, pretty, suppress, style, formats);
                        String expected = reference.format(input);
                        assertEquals(expected, fast.format(input));
                        StringBuilder builder = new StringBuilder("prefix");
                        fast.format(input, builder);
                        assertEquals("prefix" + expected, builder.toString());
                        StringWriter writer = new StringWriter();
                        fast.format(input, writer);
                        assertEquals(expected, writer.toString());
                    }
                }
            }
        }
    }

    private static String failure(JSON json, Object value) {
        try { return json.format(value); }
        catch (JSONException e) {
            StringBuilder error = new StringBuilder();
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                error.append(cause.getClass().getName()).append(':').append(cause.getMessage()).append('\n');
            }
            return error.toString();
        }
    }

    @Test public void nestedFormatterFailuresPreserveWrappingAndPaths() {
        Bean bean = new Bean();
        bean.list.add(new BadBean());
        Object[] input = {new Bean(), bean};
        assertEquals(failure(new JSON() {}, input), failure(new JSON(), input));
        bean.list.clear(); bean.tags = null; bean.dynamic = new BadBean();
        assertEquals(failure(new JSON() {}, input), failure(new JSON(), input));
    }
}
