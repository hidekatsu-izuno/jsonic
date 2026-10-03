package net.arnx.jsonic;

import java.math.BigDecimal;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.util.LocalCache;

/** Bounded lexer for fully validated untyped String documents. */
abstract class StringScanner {
    /** Estimate only when an array actually grows; bound prediction slack. */
    static int grownArrayCapacity(int count, int position, int length) {
        int doubled = count * 2;
        if (doubled < 0) return doubled;
        long estimate = (long)length * count / Math.max(1, position) + 8;
        return (int)Math.max(doubled, Math.min(Math.max(doubled, 4096), estimate));
    }

    final String input;
    final int length;
    final int maxDepth;
    final LocalCache cache;
    int position;
    boolean repeatedObjects;
    private Name[][] names;

    private static final class Name {
        final String value;
        final int start;
        final int length;
        Name(String value, int start, int end) {
            this.value = value;
            this.start = start;
            this.length = end - start;
        }
    }

    StringScanner(Context context, String input) {
        this.input = input;
        length = input.length();
        maxDepth = context.getMaxDepth();
        cache = context.getLocalCache();
    }

    final void whitespace() {
        while (position < length) {
            char c = input.charAt(position);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') break;
            position++;
        }
    }

    final String name(int depth, int ordinal) {
        // A fixed number of positions per depth bounds work and retained names.
        if (!repeatedObjects || ordinal >= 16) return string();
        if (names == null) names = new Name[maxDepth][];
        Name[] row = names[depth];
        if (row == null) names[depth] = row = new Name[16];
        Name name = row[ordinal];
        if (name != null) {
            // Compare the whole previously validated quoted token, including
            // escapes and its closing quote. Prefix matches cannot skip input.
            if (input.regionMatches(position, input, name.start, name.length)) {
                position += name.length;
                return name.value;
            }
            return string();
        }
        int start = position;
        String value = string();
        if (value != null) row[ordinal] = new Name(value, start, position);
        return value;
    }

    final boolean take(char expected) {
        if (position == length || input.charAt(position) != expected) return false;
        position++;
        return true;
    }

    final String string() {
        int start = ++position;
        StringBuilder escaped = null;
        while (position < length) {
            char c = input.charAt(position++);
            if (c == '"') {
                if (escaped == null) return cache.getString(input, start, position - 1);
                escaped.append(input, start, position - 1);
                return cache.getString(escaped);
            }
            if (c < 0x20 || c == 0x7f) return null;
            if (c != '\\') continue;
            if (escaped == null) escaped = cache.getCachedBuffer();
            escaped.append(input, start, position - 1);
            if (position == length) return null;
            c = input.charAt(position++);
            switch (c) {
            case 'b': c = '\b'; break;
            case 'f': c = '\f'; break;
            case 'n': c = '\n'; break;
            case 'r': c = '\r'; break;
            case 't': c = '\t'; break;
            case 'u': {
                if (length - position < 4) return null;
                int code = 0;
                for (int i = 0; i < 4; i++) {
                    char digit = input.charAt(position++);
                    int hex = digit >= '0' && digit <= '9' ? digit - '0'
                            : digit >= 'a' && digit <= 'f' ? digit - 'a' + 10
                            : digit >= 'A' && digit <= 'F' ? digit - 'A' + 10 : -1;
                    if (hex < 0) return null;
                    code = (code << 4) | hex;
                }
                c = (char)code;
                break;
            }
            default: break; // Match the legacy parser's permissive escapes.
            }
            escaped.append(c);
            start = position;
        }
        return null;
    }

    final Object number() {
        boolean negative = take('-');
        int first = position;
        if (position == length) return null;
        char c = input.charAt(position);
        if (c < '0' || c > '9') return null;
        long value = c - '0';
        position++;
        // Leading zeroes are not accepted by this fast path. Keeping the
        // integer and fractional loops separate avoids testing decimal state
        // and the first digit again for every character.
        if (c != '0') {
            while (position < length) {
                c = input.charAt(position);
                if (c < '0' || c > '9') break;
                if (position - first >= 18) {
                    // The uncommon 19th digit needs an overflow check. A
                    // negative Long.MIN_VALUE may use magnitude 2^63; the
                    // final negation intentionally retains its signed bits.
                    int digit = c - '0';
                    if (value > 922337203685477580L
                            || (value == 922337203685477580L && digit > (negative ? 8 : 7))) return null;
                    value = value * 10 + digit;
                    position++;
                    if (position < length && input.charAt(position) >= '0' && input.charAt(position) <= '9') return null;
                    break;
                }
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
                if (++digits > 18) return null;
                value = value * 10 + c - '0';
                position++;
            }
            scale = position - fraction;
            if (scale == 0) return null;
        }
        if (position == length) return null;
        char end = input.charAt(position);
        if (end == 'e' || end == 'E') {
            int exponent = exponent();
            if (exponent == Integer.MIN_VALUE || position == length) return null;
            scale -= exponent;
            end = input.charAt(position);
        }
        // Invalid suffixes and incomplete input retain the original diagnostics.
        if (end != ',' && end != ']' && end != '}' && end != ' ' && end != '\t' && end != '\r' && end != '\n') return null;
        long signed = negative ? -value : value;
        return BigDecimal.valueOf(signed, scale);
    }

    private int exponent() {
        position++;
        boolean negative = take('-');
        if (!negative) take('+');
        int start = position;
        int result = 0;
        while (position < length) {
            char c = input.charAt(position);
            if (c < '0' || c > '9') break;
            // Keep large-scale and overflow diagnostics in the original parser.
            if (position - start >= 3) return Integer.MIN_VALUE;
            result = result * 10 + c - '0';
            position++;
        }
        return position == start ? Integer.MIN_VALUE : negative ? -result : result;
    }
}
