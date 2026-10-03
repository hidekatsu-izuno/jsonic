package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Random;
import org.junit.jupiter.api.Test;

class QuotedNativeFloatingArrayTest {
    private static String outcome(JSON json, String input, Class<?> type) {
        try {
            Object values = json.parse(input, type);
            if (values == null) return "null";
            StringBuilder bits = new StringBuilder();
            for (int i = 0; i < Array.getLength(values); i++) {
                bits.append(type == float[].class ? Float.floatToRawIntBits(Array.getFloat(values, i))
                        : Double.doubleToRawLongBits(Array.getDouble(values, i))).append(',');
            }
            return bits.toString();
        } catch (JSONException error) {
            StringBuilder text = new StringBuilder(error.getErrorCode() + ":" + error.getMessage()
                    + ":" + error.getLineNumber() + ":" + error.getColumnNumber() + ":" + error.getErrorOffset());
            for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
                text.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return text.toString();
        }
    }

    @Test void negativeZeroSyntaxAndFallbackDiagnosticsMatchLegacy() {
        for (Class<?> type : new Class<?>[] {float[].class, double[].class}) {
            for (String input : new String[] {"[\"0\",\"-0\",\"-0.0\",\"-0e999\",-0.0,null]",
                    "[\"1.25\",2.5,\"-1e-999\",\"1e999\",\"1E+3\"]",
                    "[\"9007199254740993.0\",\"1.000000059604644775\"]",
                    "[\"00001\",\"+1\",\".5\",\"1.\",\" 1.25 \"]",
                    "[\"\\u0031.25\",\"1.5f\",\"0x1.4p3\"]",
                    "[\"NaN\",\"Infinity\",\"-Infinity\",\"\"]",
                    "[\"1e2147483648\",\"1e0000\"]", "[\"1\",true,false]",
                    "[\"1\",[2],{}]", "[\"1\",\"bad\"]", "[\"1\",\"bad\"]?",
                    "[\"1\"x]", "[\"1e+\"]", "[\"1\\q\"]", "[\"1\",\n2]?", "[\"1\",]"}) {
                for (int depth : new int[] {1, 2, 3, 32, 64, 65}) {
                    assertEquals(outcome(new JSON(depth) {}, input, type), outcome(new JSON(depth), input, type), input);
                }
            }
            String valid = "[\"-12345678901234567.8\",null,3,\"-0.0\"]";
            for (int length = 0; length <= valid.length(); length++) {
                String prefix = valid.substring(0, length);
                assertEquals(outcome(new JSON() {}, prefix, type), outcome(new JSON(), prefix, type), prefix);
            }
        }
    }

    @Test void numberFormatsAndLocalesRetainLegacyConversion() {
        for (Class<?> type : new Class<?>[] {float[].class, double[].class}) {
            for (Locale locale : new Locale[] {Locale.US, Locale.GERMANY, Locale.JAPAN}) {
                for (String format : new String[] {null, "#,##0.00", "000.00", "invalid'"}) {
                    JSON legacy = new JSON() {}, actual = new JSON();
                    legacy.setLocale(locale); actual.setLocale(locale);
                    legacy.setNumberFormat(format); actual.setNumberFormat(format);
                    for (String input : new String[] {"[\"1.25\",\"-0.0\",2.5]", "[\"1,234.5\",\"2,50\"]",
                            "[1.25,2.5]", "[\"bad\"]?"}) {
                        assertEquals(outcome(legacy, input, type), outcome(actual, input, type), input);
                    }
                }
            }
        }
    }

    @Test void randomQuotedAndUnquotedDecimalsMatchBits() {
        Random random = new Random(0x36471);
        for (int round = 0; round < 300; round++) {
            StringBuilder text = new StringBuilder("[");
            for (int i = 0, count = random.nextInt(400); i < count; i++) {
                if (i > 0) text.append(',');
                if (random.nextInt(13) == 0) { text.append("null"); continue; }
                boolean quoted = random.nextBoolean();
                if (quoted) text.append('"');
                text.append(BigDecimal.valueOf(random.nextLong() % 1000000000000000000L,
                        random.nextInt(161) - 80).toString());
                if (quoted) text.append('"');
            }
            String input = text.append(']').toString();
            for (Class<?> type : new Class<?>[] {float[].class, double[].class}) {
                for (String value : new String[] {input, input + "?"}) {
                    assertEquals(outcome(new JSON() {}, value, type), outcome(new JSON(), value, type), value);
                }
            }
        }
    }
}
