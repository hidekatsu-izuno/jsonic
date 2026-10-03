package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringWriter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class PrimitiveNumberOutputTest {
    private static String expected(double[] values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            double value = values[i];
            if (!Double.isFinite(value)) out.append('"');
            out.append(Double.toString(value));
            if (!Double.isFinite(value)) out.append('"');
        }
        return out.append(']').toString();
    }

    private static String expected(float[] values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            float value = values[i];
            if (!Float.isFinite(value)) out.append('"');
            out.append(Float.toString(value));
            if (!Float.isFinite(value)) out.append('"');
        }
        return out.append(']').toString();
    }

    private static void outputs(Object value, String expected) throws Exception {
        JSON json = new JSON();
        assertEquals(expected, json.format(value));
        StringBuilder builder = new StringBuilder("日本語:");
        json.format(value, builder);
        assertEquals("日本語:" + expected, builder.toString());
        StringBuffer buffer = new StringBuffer("日本語:");
        json.format(value, buffer);
        assertEquals("日本語:" + expected, buffer.toString());
        StringWriter writer = new StringWriter();
        json.format(value, writer);
        assertEquals(expected, writer.toString());
        Appendable appendable = new Appendable() {
            private final StringBuilder text = new StringBuilder();
            public Appendable append(CharSequence value) { text.append(value); return this; }
            public Appendable append(CharSequence value, int from, int to) { text.append(value, from, to); return this; }
            public Appendable append(char value) { text.append(value); return this; }
            public String toString() { return text.toString(); }
        };
        json.format(value, appendable);
        assertEquals(expected, appendable.toString());
    }

    @Test public void directOutputPreservesAllNumericTextAcrossDestinations() throws Exception {
        double[] doubles = {0, -0.0, Double.MIN_VALUE, Double.MIN_NORMAL, Double.MAX_VALUE,
                Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1.2345e100, 9007199254740993d};
        float[] floats = {0, -0.0f, Float.MIN_VALUE, Float.MIN_NORMAL, Float.MAX_VALUE,
                Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 1.234567f};
        outputs(doubles, expected(doubles));
        outputs(floats, expected(floats));
        outputs(new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}, "[-2147483648,-1,0,1,2147483647]");
        outputs(new long[] {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE}, "[-9223372036854775808,-1,0,1,9223372036854775807]");
        outputs(new long[] {(long)Integer.MIN_VALUE - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, (long)Integer.MAX_VALUE + 1},
                "[-2147483649,-2147483648,2147483647,2147483648]");
        outputs(new short[] {Short.MIN_VALUE, 0, Short.MAX_VALUE}, "[-32768,0,32767]");
        outputs(new Object[] {Integer.MIN_VALUE, Long.MAX_VALUE, (short)-123, (byte)-1, 1.234567f},
                "[-2147483648,9223372036854775807,-123,255," + Double.toString((double)1.234567f) + "]");
        outputs(new AtomicInteger(3) { @Override public String toString() { return "99"; } }, "99");
        Random random = new Random(0x773ad);
        doubles = new double[10000]; floats = new float[10000];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = Double.longBitsToDouble(random.nextLong());
            floats[i] = Float.intBitsToFloat(random.nextInt());
        }
        outputs(doubles, expected(doubles));
        outputs(floats, expected(floats));
    }

    @Test public void formattedAndPrettyOutputRetainsExistingRules() throws Exception {
        Object[] values = {new int[] {1, -2}, new long[] {1234567890123L}, new float[] {1.2345f, Float.NaN},
                new double[] {-0.0, 1.2345e100, Double.POSITIVE_INFINITY}, 1.2345f, Integer.MIN_VALUE};
        for (boolean pretty : new boolean[] {false, true}) {
            for (String format : new String[] {null, "0.00"}) {
                JSON json = new JSON(); json.setPrettyPrint(pretty); json.setNumberFormat(format);
                StringWriter writer = new StringWriter(); json.format(values, writer);
                assertEquals(writer.toString(), json.format(values));
                StringBuilder builder = new StringBuilder(); json.format(values, builder);
                assertEquals(writer.toString(), builder.toString());
            }
        }
    }
}
