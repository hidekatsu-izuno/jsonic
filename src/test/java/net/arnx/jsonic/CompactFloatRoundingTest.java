package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;
import net.arnx.jsonic.parse.CompactNumber;

public class CompactFloatRoundingTest {
    private static void same(long value, int scale) {
        int expected = Float.floatToRawIntBits(BigDecimal.valueOf(value, scale).floatValue());
        int actual = Float.floatToRawIntBits(CompactNumber.of(value, scale).floatValue());
        assertEquals(expected, actual, () -> value + " scale=" + scale);
    }

    @Test public void operandsAndFallbackPreserveFloatRoundingBits() {
        for (long value : new long[] {0, -1, 1, (1L << 24) - 1, 1L << 24, (1L << 24) + 1,
                -(1L << 24), -(1L << 24) - 1, Long.MIN_VALUE, Long.MAX_VALUE, 9007199254740993L}) {
            for (int scale : new int[] {Integer.MIN_VALUE, -999, -11, -10, -1, 0, 1, 10, 11, 999, Integer.MAX_VALUE}) {
                same(value, scale);
            }
        }
        Random random = new Random(0x53af);
        for (int i = 0; i < 100000; i++) {
            same(random.nextInt(1 << 25) - (1L << 24), random.nextInt(23) - 11);
            same(random.nextLong(), random.nextInt(101) - 50);
        }
    }

    @Test public void wideDecimalsAndExactMidpointsPreserveRounding() {
        Random random = new Random(0x637bc);
        for (int i = 0; i < 100000; i++) {
            same(random.nextLong() % 1000000000000000000L, random.nextInt(18) + 1);
        }
        for (int exponent = 9; exponent <= 56; exponent++) {
            float power = Math.scalb(1f, exponent);
            for (float first : new float[] {Math.nextDown(power), power,
                    Float.intBitsToFloat(Float.floatToRawIntBits(power) + 31)}) {
                BigDecimal midpoint = new BigDecimal((double)first)
                        .add(new BigDecimal((double)Math.nextUp(first))).divide(BigDecimal.valueOf(2));
                for (int scale = Math.max(1, midpoint.scale()); scale <= 18; scale++) {
                    java.math.BigInteger integer = midpoint.setScale(scale).unscaledValue();
                    if (integer.compareTo(java.math.BigInteger.valueOf(999999999999999998L)) > 0) break;
                    long value = integer.longValueExact();
                    for (long nearby : new long[] {value - 1, value, value + 1}) {
                        same(nearby, scale);
                        same(-nearby, scale);
                    }
                }
            }
        }
        for (int scale = 11; scale <= 18; scale++) {
            for (long value : new long[] {1, -1, 2, -2, 31, -31, 16777217}) same(value, scale);
        }
    }

    public static class Value {
        public float field;
        public Float boxed;
        private float setter;
        @JSONHint(type = String.class) public float hinted;
        public void setSetter(float value) { setter = value; }
        public float getSetter() { return setter; }
    }

    private static String outcome(JSON json, String input, Class<?> type, boolean reader) throws Exception {
        try {
            Object value = reader ? json.parse(new StringReader(input), type) : json.parse(input, type);
            return JSON.encode(value);
        } catch (JSONException e) {
            StringBuilder result = new StringBuilder(e.getErrorCode() + ":" + e.getMessage()
                    + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ":" + e.getErrorOffset());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                result.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return result.toString();
        }
    }

    @Test public void arraysFieldsSettersHintsAndErrorPathsMatchLegacy() throws Exception {
        for (boolean reader : new boolean[] {false, true}) {
            for (String input : new String[] {"[0,-0.0,null,1.10,1e-999,1e999]", "[1,\"-0.0\",true,false]",
                    "[1,\"bad\"]", "[1,{}]", "[1,[2]]", "[1e2147483648]", "[1,2]?"}) {
                for (Class<?> type : new Class<?>[] {float[].class, Float[].class, float[][].class}) {
                    assertEquals(outcome(new JSON() {}, input, type, reader), outcome(new JSON(), input, type, reader), input);
                }
            }
            for (String number : new String[] {"16777217", "16777216.000001", "12345.625", "1e-999", "1e999", "-0.0"}) {
                String item = "{\"field\":" + number + ",\"boxed\":" + number + ",\"setter\":" + number
                        + ",\"hinted\":" + number + "}";
                String input = "[" + item + "," + item + "]";
                assertEquals(outcome(new JSON() {}, input, Value[].class, reader),
                        outcome(new JSON(), input, Value[].class, reader), input);
            }
        }
        Value value = new JSON().parse("{\"field\":1,\"field\":2,\"setter\":3}", Value.class);
        assertEquals(2f, value.field);
        assertEquals(3f, value.setter);
    }
}
