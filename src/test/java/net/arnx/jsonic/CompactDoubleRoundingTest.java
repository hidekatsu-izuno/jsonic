package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import net.arnx.jsonic.parse.CompactNumber;
import org.junit.jupiter.api.Test;

public class CompactDoubleRoundingTest {
    private static void check(long value, int scale) {
        long expected = Double.doubleToRawLongBits(BigDecimal.valueOf(value, scale).doubleValue());
        long actual = Double.doubleToRawLongBits(CompactNumber.of(value, scale).doubleValue());
        assertEquals(expected, actual, () -> value + " / 10^" + scale);
    }

    @Test public void randomizedFullCompactRangeMatchesBigDecimalBits() {
        Random random = new Random(117043);
        for (int i = 0; i < 200000; i++) {
            check(random.nextLong() % 1000000000000000000L, random.nextInt(19));
        }
    }

    @Test public void negativeScaleMultiplicationMatchesBigDecimalBits() {
        Random random = new Random(131201);
        for (int i = 0; i < 20000; i++) check(random.nextLong() % 10000000000000000L, -1 - random.nextInt(18));
    }

    @Test public void binaryMidpointsAndPowerOfTwoBoundariesMatchBigDecimal() {
        for (int exponent = -7; exponent <= 59; exponent++) {
            double power = Math.scalb(1.0, exponent);
            for (double value : new double[] {Math.nextDown(power), power, Math.nextUp(power)}) {
                BigDecimal midpoint = new BigDecimal(value).add(new BigDecimal(Math.nextUp(value)))
                        .divide(BigDecimal.valueOf(2));
                for (int scale = 1; scale <= 18; scale++) {
                    BigDecimal unscaled = midpoint.movePointRight(scale).setScale(0, RoundingMode.FLOOR);
                    if (unscaled.compareTo(BigDecimal.valueOf(999999999999999990L)) > 0) continue;
                    long base = unscaled.longValueExact();
                    for (long offset = -3; offset <= 3; offset++) {
                        check(base + offset, scale);
                        check(-base - offset, scale);
                    }
                }
            }
        }
    }

    @Test public void longAndScaleLimitsRetainFallbackBehavior() {
        for (long value : new long[] {Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE, Long.MAX_VALUE - 1,
                -1000000000000000000L, 1000000000000000000L, 0, 1, -1,
                (1L << 53) - 1, 1L << 53, (1L << 53) + 1}) {
            for (int scale : new int[] {-20, -1, 0, 1, 5, 18, 19, 300, 400}) check(value, scale);
        }
    }
}
