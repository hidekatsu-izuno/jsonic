package net.arnx.jsonic.parse;

import java.math.BigDecimal;

/** Internal decimal token for typed binding. Never exposed as an untyped value. */
public final class CompactNumber {
    private static final CompactNumber[] SMALL = new CompactNumber[11];
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

    public double doubleValue() {
        // Both operands are exactly representable; division supplies the same
        // correctly rounded result as the original decimal conversion.
        if (value >= -(1L << 53) && value <= (1L << 53) && scale >= 0 && scale < POWERS.length) {
            return (double)value / (double)POWERS[scale];
        }
        return decimalValue().doubleValue();
    }
}
