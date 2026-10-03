package net.arnx.jsonic;

import java.util.Arrays;

/** Parses a whole integer document before exposing an int[] or long[]. */
final class IntegralArrayDecoder {
    private final String input;
    private final int length;
    private final boolean wide;
    private int position;
    private int[] integers;
    private long[] longs;
    private int count;

    private IntegralArrayDecoder(String input, boolean wide) {
        this.input = input;
        this.length = input.length();
        this.wide = wide;
    }

    static Object scan(int maxDepth, String input, boolean wide) {
        if (maxDepth <= 2) return null;
        int firstArray = input.indexOf('[');
        if (firstArray < 0 || input.indexOf('"') >= 0 || input.indexOf('{') >= 0
                || input.indexOf('[', firstArray + 1) >= 0) return null;
        IntegralArrayDecoder scanner = new IntegralArrayDecoder(input, wide);
        scanner.whitespace();
        if (!scanner.take('[')) return null;
        scanner.whitespace();
        if (scanner.take(']')) {
            scanner.whitespace();
            return scanner.position == scanner.length ? (wide ? new long[0] : new int[0]) : null;
        }
        int capacity = Math.min(128, Math.max(8, scanner.length / 2));
        if (wide) scanner.longs = new long[capacity];
        else scanner.integers = new int[capacity];
        do {
            scanner.whitespace();
            if (wide && scanner.count == scanner.longs.length) {
                scanner.longs = Arrays.copyOf(scanner.longs, StringScanner.grownArrayCapacity(scanner.count, scanner.position, scanner.length));
            } else if (!wide && scanner.count == scanner.integers.length) {
                scanner.integers = Arrays.copyOf(scanner.integers, StringScanner.grownArrayCapacity(scanner.count, scanner.position, scanner.length));
            }
            if (input.startsWith("null", scanner.position)) {
                scanner.position += 4;
            } else if (!scanner.number()) {
                // The legacy path validates syntax before reporting conversion
                // failures, including integer overflow and fractional values.
                return null;
            }
            scanner.count++;
            scanner.whitespace();
            if (scanner.take(']')) {
                scanner.whitespace();
                if (scanner.position != scanner.length) return null;
                if (wide) return scanner.count == scanner.longs.length ? scanner.longs
                        : Arrays.copyOf(scanner.longs, scanner.count);
                return scanner.count == scanner.integers.length ? scanner.integers
                        : Arrays.copyOf(scanner.integers, scanner.count);
            }
        } while (scanner.take(','));
        return null;
    }

    private boolean number() {
        boolean negative = take('-');
        if (position == length) return false;
        char digit = input.charAt(position);
        if (digit < '0' || digit > '9') return false;
        long value = 0;
        if (digit == '0') {
            position++;
        } else {
            // Accumulate negatively to include Long.MIN_VALUE without overflow.
            long limit = negative ? Long.MIN_VALUE : -Long.MAX_VALUE;
            long multiplyLimit = limit / 10;
            do {
                if (value < multiplyLimit) return false;
                value *= 10;
                if (value < limit + (digit - '0')) return false;
                value -= digit - '0';
                position++;
                if (position == length) break;
                digit = input.charAt(position);
            } while (digit >= '0' && digit <= '9');
        }
        if (!negative) value = -value;
        if (wide) longs[count] = value;
        else {
            if ((int)value != value) return false;
            integers[count] = (int)value;
        }
        return true;
    }

    private void whitespace() {
        while (position < length) {
            char c = input.charAt(position);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') break;
            position++;
        }
    }

    private boolean take(char c) {
        if (position == length || input.charAt(position) != c) return false;
        position++;
        return true;
    }
}
