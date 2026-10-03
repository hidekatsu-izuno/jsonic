package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class FloatArrayDecoderTest {
    private static String outcome(JSON json, String input) {
        try {
            float[] values = json.parse(input, float[].class);
            if (values == null) return "null";
            StringBuilder bits = new StringBuilder();
            for (float value : values) bits.append(Float.floatToRawIntBits(value)).append(',');
            return bits.toString();
        } catch (JSONException e) {
            StringBuilder error = new StringBuilder(e.getErrorCode() + ":" + e.getMessage()
                    + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ":" + e.getErrorOffset());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                error.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return error.toString();
        }
    }

    @Test public void nativeArraysAndFallbacksPreserveBitsAndDiagnostics() {
        for (String input : new String[] {"[]", " [ 0,-0,-0.0,1.0,null,2.50 ] \r\n", "[null,null]",
                "[999999999999999999.9]", "[9223372036854775807]", "[1e3,1e-400,1e400]", "[1E+3,-2.5e-2,0e999,-1e-999]", "[1e2147483648]",
                "[1e,1]", "[1e+,1]", "[1e0000,2]",
                "[1,\"2\",3]", "[1,[2],{},null]", "[1,true]", "[1,2,]", "[01,2]", "[.1,-.2]",
                "[1/*comment*/,2]", "[1,2]?", "[1,\n2.3.4]", "[1,2,", "[nullx]", "null", "1"}) {
            for (int depth : new int[] {1, 2, 3, 32, 65}) {
                JSON fast = new JSON(depth);
                JSON legacy = new JSON(depth) {};
                assertEquals(outcome(legacy, input), outcome(fast, input), input + " / " + depth);
            }
        }
        assertArrayEquals(new float[] {1, 2}, new JSON().parse("[1,2]", new TypeReference<float[]>() {}));
        String valid = "[-12345678901234567.8,null,3]";
        for (int end = 0; end <= valid.length(); end++) {
            String input = valid.substring(0, end);
            assertEquals(outcome(new JSON() {}, input), outcome(new JSON(), input), input);
        }
    }

    @Test public void randomizedArraysGrowAndMatchLegacyExactly() {
        Random random = new Random(133087);
        for (int round = 0; round < 500; round++) {
            StringBuilder input = new StringBuilder("[");
            int count = random.nextInt(300);
            for (int i = 0; i < count; i++) {
                if (i != 0) input.append(',');
                if (random.nextInt(15) == 0) input.append("null");
                else input.append(java.math.BigDecimal.valueOf(random.nextLong() % 100000000000000000L,
                        (random.nextInt(121) - 50)).toString());
            }
            input.append(']');
            assertEquals(outcome(new JSON() {}, input.toString()), outcome(new JSON(), input.toString()));
        }
    }
}
