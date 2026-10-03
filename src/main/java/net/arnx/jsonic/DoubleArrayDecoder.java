package net.arnx.jsonic;

import java.util.Arrays;
import net.arnx.jsonic.parse.CompactNumber;

/** Validates a whole numeric double[] document before exposing a primitive result. */
final class DoubleArrayDecoder {
    private final String input;
    private final int length;
    private int position;
    private double[] values;
    private int count;

    private DoubleArrayDecoder(String input) {
        this.input = input;
        length = input.length();
    }

    static double[] scan(int maxDepth, String input) {
        if (maxDepth <= 2) return null;
        int firstArray = input.indexOf('[');
        if (firstArray < 0 || input.indexOf('"') >= 0 || input.indexOf('{') >= 0
                || input.indexOf('[', firstArray + 1) >= 0) return null;
        DoubleArrayDecoder scanner = new DoubleArrayDecoder(input);
        scanner.whitespace();
        if (!scanner.take('[')) return null;
        scanner.whitespace();
        if (scanner.take(']')) {
            scanner.whitespace();
            return scanner.position == scanner.length ? new double[0] : null;
        }
        scanner.values = new double[Math.min(128, Math.max(8, scanner.length / 2))];
        do {
            scanner.whitespace();
            if (scanner.count == scanner.values.length) scanner.values = Arrays.copyOf(scanner.values, StringScanner.grownArrayCapacity(scanner.count, scanner.position, scanner.length));
            if (input.startsWith("null", scanner.position)) {
                scanner.position += 4;
                scanner.values[scanner.count] = 0.0;
            } else if (!scanner.number()) {
                return null;
            }
            scanner.count++;
            scanner.whitespace();
            if (scanner.take(']')) {
                scanner.whitespace();
                if (scanner.position != scanner.length) return null;
                return scanner.count == scanner.values.length ? scanner.values : Arrays.copyOf(scanner.values, scanner.count);
            }
        } while (scanner.take(','));
        return null;
    }

    static double[] scanQuoted(int maxDepth, String input) {
        if (maxDepth <= 2) return null;
        int firstArray = input.indexOf('[');
        if (firstArray < 0 || input.indexOf('{') >= 0
                || input.indexOf('[', firstArray + 1) >= 0) return null;
        // This path accepts ordinary numeric strings; escaped forms use the
        // original decoder before allocating a candidate result array.
        if (input.indexOf('\\') >= 0) return null;
        DoubleArrayDecoder scanner = new DoubleArrayDecoder(input);
        scanner.whitespace();
        if (!scanner.take('[')) return null;
        scanner.whitespace();
        if (scanner.take(']')) {
            scanner.whitespace();
            return scanner.position == scanner.length ? new double[0] : null;
        }
        scanner.values = new double[Math.min(128, Math.max(8, scanner.length / 2))];
        do {
            scanner.whitespace();
            if (scanner.count == scanner.values.length) scanner.values = Arrays.copyOf(scanner.values, StringScanner.grownArrayCapacity(scanner.count, scanner.position, scanner.length));
            if (input.startsWith("null", scanner.position)) {
                scanner.position += 4;
                scanner.values[scanner.count] = 0.0;
            } else if (scanner.position < scanner.length && input.charAt(scanner.position) == '"') {
                if (!scanner.quotedNumber()) return null;
            } else if (!scanner.number()) {
                return null;
            }
            scanner.count++;
            scanner.whitespace();
            if (scanner.take(']')) {
                scanner.whitespace();
                if (scanner.position != scanner.length) return null;
                return scanner.count == scanner.values.length ? scanner.values : Arrays.copyOf(scanner.values, scanner.count);
            }
        } while (scanner.take(','));
        return null;
    }

    private boolean quotedNumber() {
        position++;
        boolean negative = position < length && input.charAt(position) == '-';
        if (!number() || !take('"')) return false;
        // String conversion retains the sign of zero, including underflow.
        if (negative && values[count] == 0) values[count] = -0.0;
        return true;
    }

    private boolean number() {
        boolean negative = take('-');
        int first = position;
        if (position == length) return false;
        char c = input.charAt(position);
        if (c < '0' || c > '9') return false;
        long value = c - '0';
        position++;
        if (c != '0') {
            while (position < length) {
                c = input.charAt(position);
                if (c < '0' || c > '9') break;
                if (position - first >= 18) return false;
                value = value * 10 + c - '0';
                position++;
            }
        }
        int digits = position - first;
        int scale = 0;
        if (take('.')) {
            int fraction = position;
            while (position < length) {
                c = input.charAt(position);
                if (c < '0' || c > '9') break;
                if (++digits > 18) return false;
                value = value * 10 + c - '0';
                position++;
            }
            scale = position - fraction;
            if (scale == 0) return false;
        }
        if (position < length && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {
            position++;
            boolean negativeExponent = take('-');
            if (!negativeExponent) take('+');
            int exponentStart = position;
            int exponent = 0;
            while (position < length) {
                c = input.charAt(position);
                if (c < '0' || c > '9') break;
                // Larger exponents retain the original parser's scale diagnostics.
                if (position - exponentStart >= 3) return false;
                exponent = exponent * 10 + c - '0';
                position++;
            }
            if (position == exponentStart) return false;
            scale += negativeExponent ? exponent : -exponent;
        }
        values[count] = CompactNumber.doubleValue(negative ? -value : value, scale);
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
