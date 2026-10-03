package net.arnx.jsonic.parse;

import java.math.BigDecimal;

/** Internal decimal token for typed binding. Never exposed as an untyped value. */
public final class CompactNumber {
    // Bound shared immutable tokens to common small nonnegative integers.
    private static final CompactNumber[] SMALL = new CompactNumber[1024];
    private static final long[] POWERS = new long[19];
    static {
        for (int i = 0; i < SMALL.length; i++) SMALL[i] = new CompactNumber(i, 0);
        POWERS[0] = 1;
        for (int i = 1; i < POWERS.length; i++) POWERS[i] = POWERS[i - 1] * 10;
    }

    private final long value;
    private final int scale;

    private CompactNumber(long value, int scale) {
        this.value = value;
        this.scale = scale;
    }

    public static CompactNumber of(long value, int scale) {
        return scale == 0 && value >= 0 && value < SMALL.length
                ? SMALL[(int)value] : new CompactNumber(value, scale);
    }

    public BigDecimal decimalValue() { return BigDecimal.valueOf(value, scale); }

    public long longValueExact() {
        if (scale == 0) return value;
        if (scale > 0 && scale < POWERS.length && value % POWERS[scale] == 0) {
            return value / POWERS[scale];
        }
        return decimalValue().longValueExact();
    }

    public int intValueExact() {
        if (scale == 0 && (int)value == value) return (int)value;
        if (scale >= 0 && scale < POWERS.length && value % POWERS[scale] == 0) {
            long integer = value / POWERS[scale];
            if ((int)integer == integer) return (int)integer;
        }
        // Preserve the original exception, including its message.
        return decimalValue().intValueExact();
    }

    public float floatValue() { return floatValue(value, scale); }

    /** Internal allocation-free conversion for a validated decimal token. */
    public static float floatValue(long value, int scale) {
        if (scale == 0) return (float)value;
        // Each operand is exact in float; use one float operation, never a
        // double intermediate that could introduce a second rounding.
        if (value >= -(1L << 24) && value <= (1L << 24)) {
            if (scale > 0 && scale <= 10) return (float)value / (float)POWERS[scale];
            if (scale < 0 && scale >= -10) return (float)value * (float)POWERS[-scale];
        }
        return scaledFloatValue(value, scale);
    }

    private static float scaledFloatValue(long value, int scale) {
        if (scale > 0 && scale < POWERS.length
                && value >= -999999999999999999L && value <= 999999999999999999L) {
            if (value == 0) return 0f;
            long magnitude = value < 0 ? -value : value;
            long divisor = POWERS[scale];
            float candidate = (float)((double)magnitude / (double)divisor);
            int direction = floatRoundingDirection(magnitude, divisor, candidate);
            if (direction != 0 && direction != 2) {
                candidate = Float.intBitsToFloat(Float.floatToRawIntBits(candidate) + direction);
                direction = floatRoundingDirection(magnitude, divisor, candidate);
            }
            // Accept only an exact midpoint comparison, including ties to even.
            if (direction == 0) return value < 0 ? -candidate : candidate;
        }
        return BigDecimal.valueOf(value, scale).floatValue();
    }

    private static int floatRoundingDirection(long numerator, long divisor, float candidate) {
        int bits = Float.floatToRawIntBits(candidate);
        long significand = (bits & ((1 << 23) - 1)) | (1 << 23);
        int exponent = (bits >>> 23) - 127 - 23;
        long residual;
        if (exponent < 0) {
            int shift = -exponent;
            // In this domain the candidate is normal and shift <= 83. The
            // exact products require at most 84 bits, including tiny values.
            if (shift >= 128) return 2;
            long low = shift < 64 ? numerator << shift : 0;
            long high = shift < 64 ? numerator >>> (64 - shift) : numerator << (shift - 64);
            long product = significand * divisor;
            residual = low - product;
            high -= Math.multiplyHigh(significand, divisor)
                    + (Long.compareUnsigned(low, product) < 0 ? 1 : 0);
            if (high != (residual >> 63)) return 2;
        } else {
            // The guarded quotient is <= 10^17. This scaled divisor and its
            // product with a 24-bit significand remain below 10^18 + one ulp.
            divisor <<= exponent;
            residual = numerator - significand * divisor;
        }
        if (residual > 2 * divisor || residual < -2 * divisor) return 2;
        long distance = residual >= 0 ? residual * 2
                : -residual * (significand == (1 << 23) ? 4 : 2);
        if (distance < divisor || (distance == divisor && (bits & 1) == 0)) return 0;
        return residual > 0 ? 1 : -1;
    }

    public double doubleValue() { return doubleValue(value, scale); }

    /** Internal allocation-free conversion for a validated decimal token. */
    public static double doubleValue(long value, int scale) {
        if (scale == 0) return (double)value;
        // Both operands are exactly representable; division supplies the same
        // correctly rounded result as the original decimal conversion.
        if (value >= -(1L << 53) && value <= (1L << 53) && scale >= 0 && scale < POWERS.length) {
            return (double)value / (double)POWERS[scale];
        }
        if (value >= -(1L << 53) && value <= (1L << 53) && scale < 0 && scale > -POWERS.length) {
            return (double)value * (double)POWERS[-scale];
        }
        return scaledDoubleValue(value, scale);
    }

    private static double scaledDoubleValue(long value, int scale) {
        if (scale > 0 && scale < POWERS.length
                && value >= -999999999999999999L && value <= 999999999999999999L) {
            long magnitude = value < 0 ? -value : value;
            long divisor = POWERS[scale];
            double candidate = (double)magnitude / (double)divisor;
            int direction = roundingDirection(magnitude, divisor, candidate);
            if (direction != 0 && direction != 2) {
                // A rounded numerator can put the candidate next to the correct
                // double. Validate that neighbor too; never assume it is right.
                candidate = Double.longBitsToDouble(Double.doubleToRawLongBits(candidate) + direction);
                direction = roundingDirection(magnitude, divisor, candidate);
            }
            if (direction == 0) return value < 0 ? -candidate : candidate;
        }
        return BigDecimal.valueOf(value, scale).doubleValue();
    }

    /** 0: nearest; +/-1: neighbor to test; 2: outside the exact validation range. */
    private static int roundingDirection(long numerator, long divisor, double candidate) {
        long bits = Double.doubleToRawLongBits(candidate);
        long significand = (bits & ((1L << 52) - 1)) | (1L << 52);
        int exponent = (int)(bits >>> 52) - 1023 - 52;
        long residual;
        if (exponent < 0) {
            int shift = -exponent;
            if (shift >= 64) return 2;
            // Exact 128-bit difference: numerator * 2^shift - significand * divisor.
            long low = numerator << shift;
            long product = significand * divisor;
            residual = low - product;
            long high = (numerator >>> (64 - shift)) - Math.multiplyHigh(significand, divisor)
                    - (Long.compareUnsigned(low, product) < 0 ? 1 : 0);
            if (high != (residual >> 63)) return 2;
        } else {
            // In the guarded 18-digit, positive-scale domain the candidate is
            // at most 10^17, so exponent <= 4 and this product fits in a long.
            divisor <<= exponent;
            residual = numerator - significand * divisor;
        }
        // With divisor <= 10^18, even the fourfold midpoint comparison
        // remains within signed long when |residual| <= 2 * divisor.
        if (residual > 2 * divisor || residual < -2 * divisor) return 2;
        // Immediately below a power of two, the adjacent double is half as far away.
        long distance = residual >= 0 ? residual * 2
                : -residual * (significand == (1L << 52) ? 4 : 2);
        if (distance < divisor || (distance == divisor && (bits & 1) == 0)) return 0;
        return residual > 0 ? 1 : -1;
    }
}
