package net.arnx.jsonic;

import java.util.Arrays;

/** Validates a whole boolean array before returning a primitive result. */
final class BooleanArrayDecoder {
    private final String input;
    private final int length;
    private int position;

    private BooleanArrayDecoder(String input) {
        this.input = input;
        length = input.length();
    }

    static boolean[] scan(int maxDepth, String input) {
        if (maxDepth <= 2) return null;
        int firstArray = input.indexOf('[');
        if (firstArray < 0 || input.indexOf('{') >= 0
                || input.indexOf('[', firstArray + 1) >= 0) return null;
        BooleanArrayDecoder scanner = new BooleanArrayDecoder(input);
        scanner.whitespace();
        if (!scanner.take('[')) return null;
        scanner.whitespace();
        if (scanner.take(']')) {
            scanner.whitespace();
            return scanner.position == scanner.length ? new boolean[0] : null;
        }
        boolean[] values = new boolean[Math.min(128, Math.max(8, input.length() / 2))];
        int count = 0;
        do {
            scanner.whitespace();
            boolean value;
            if (input.startsWith("true", scanner.position)) {
                value = true;
                scanner.position += 4;
            } else if (input.startsWith("false", scanner.position)) {
                value = false;
                scanner.position += 5;
            } else if (input.startsWith("null", scanner.position)) {
                value = false;
                scanner.position += 4;
            } else if (scanner.position < scanner.length && input.charAt(scanner.position) == '"') {
                if (input.startsWith("\"true\"", scanner.position)) {
                    value = true;
                    scanner.position += 6;
                } else if (input.startsWith("\"false\"", scanner.position)) {
                    value = false;
                    scanner.position += 7;
                } else return null;
            } else {
                int integer = scanner.integer();
                if (integer < 0) return null;
                value = integer != 0;
            }
            if (count == values.length) values = Arrays.copyOf(values, StringScanner.grownArrayCapacity(count, scanner.position, scanner.length));
            values[count++] = value;
            scanner.whitespace();
            if (scanner.take(']')) {
                scanner.whitespace();
                if (scanner.position != scanner.length) return null;
                return count == values.length ? values : Arrays.copyOf(values, count);
            }
        } while (scanner.take(','));
        return null;
    }

    private int integer() {
        take('-');
        int first = position;
        if (position == length) return -1;
        char digit = input.charAt(position);
        if (digit < '0' || digit > '9') return -1;
        if (digit == '0') {
            position++;
            return 0;
        }
        do {
            // A bounded integer token always yields a nonzero BigDecimal of
            // scale zero. Fractional/exponent forms retain the legacy path:
            // in particular 0.0 is true whereas 0 is false in BooleanConverter.
            if (position - first >= 18) return -1;
            position++;
            if (position == length) break;
            digit = input.charAt(position);
        } while (digit >= '0' && digit <= '9');
        return 1;
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
