package net.arnx.jsonic;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.parse.CompactNumber;
import net.arnx.jsonic.util.LocalCache;

/**
 * Validates ordinary String documents directly into the deferred binding buffer.
 * Unsupported syntax, numeric forms or depth discard this buffer and let the
 * original parser reproduce its exact behavior and diagnostics. No user code
 * runs in this scanner.
 */
final class StringDecoder {
    private final String input;
    private final int length;
    private final int maxDepth;
    private final LocalCache cache;
    private final TypedDecoder buffer;
    private int position;
    private boolean repeatedObjects;
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

    private StringDecoder(Context context, String input) {
        this.input = input;
        length = input.length();
        maxDepth = context.getMaxDepth();
        cache = context.getLocalCache();
        buffer = new TypedDecoder(length);
    }

    static TypedDecoder scan(Context context, String input, Class<?> type) {
        if (!TypedDecoder.supports(context, type)) return null;
        StringDecoder decoder = new StringDecoder(context, input);
        decoder.whitespace();
        if (decoder.position == decoder.length) return null;
        char first = input.charAt(decoder.position);
        if (type.isArray() ? first != '[' : first != '{') return null;
        if (!decoder.value(0)) return null;
        decoder.whitespace();
        return decoder.position == decoder.length ? decoder.buffer : null;
    }

    private void whitespace() {
        while (position < length) {
            char c = input.charAt(position);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') break;
            position++;
        }
    }

    private boolean value(int depth) {
        // Near the truncation boundary keep the original depth semantics.
        if (depth + 1 >= maxDepth) return false;
        whitespace();
        if (position == length) return false;
        switch (input.charAt(position)) {
        case '{': return object(depth);
        case '[': return array(depth);
        case '"': {
            String value = string();
            if (value == null) return false;
            buffer.add(JSONEventType.STRING, value);
            return true;
        }
        case 't': return literal("true", JSONEventType.BOOLEAN, Boolean.TRUE);
        case 'f': return literal("false", JSONEventType.BOOLEAN, Boolean.FALSE);
        case 'n': return literal("null", JSONEventType.NULL, null);
        default: return number();
        }
    }

    private boolean object(int depth) {
        position++;
        buffer.add(JSONEventType.START_OBJECT, null);
        whitespace();
        if (take('}')) {
            buffer.add(JSONEventType.END_OBJECT, null);
            return true;
        }
        int ordinal = 0;
        do {
            whitespace();
            if (position == length || input.charAt(position) != '"') return false;
            String name = name(depth, ordinal++);
            if (name == null) return false;
            buffer.add(JSONEventType.NAME, name);
            whitespace();
            if (!take(':') || !value(depth + 1)) return false;
            whitespace();
            if (take('}')) {
                buffer.add(JSONEventType.END_OBJECT, null);
                return true;
            }
        } while (take(','));
        return false;
    }

    private boolean array(int depth) {
        position++;
        buffer.add(JSONEventType.START_ARRAY, null);
        whitespace();
        if (take(']')) {
            buffer.add(JSONEventType.END_ARRAY, null);
            return true;
        }
        do {
            if (!value(depth + 1)) return false;
            whitespace();
            if (take(']')) {
                buffer.add(JSONEventType.END_ARRAY, null);
                return true;
            }
            if (!take(',')) return false;
            whitespace();
            // Enable prediction only once an array actually repeats objects.
            // Single beans and single-element arrays pay no cache allocation.
            if (position < length && input.charAt(position) == '{') repeatedObjects = true;
        } while (true);
    }

    private String name(int depth, int ordinal) {
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

    private boolean take(char expected) {
        if (position == length || input.charAt(position) != expected) return false;
        position++;
        return true;
    }

    private boolean literal(String literal, JSONEventType event, Object value) {
        if (!input.startsWith(literal, position)) return false;
        position += literal.length();
        buffer.add(event, value);
        return true;
    }

    private String string() {
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

    private boolean number() {
        boolean negative = take('-');
        int first = position;
        if (position == length) return false;
        char c = input.charAt(position);
        if (c < '0' || c > '9') return false;
        long value = c - '0';
        position++;
        // Leading zeroes are not accepted by this fast path. Keeping the
        // integer and fractional loops separate avoids testing decimal state
        // and the first digit again for every character.
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
        // Exponents, invalid suffixes and incomplete input use the original parser.
        if (position == length) return false;
        char end = input.charAt(position);
        if (end != ',' && end != ']' && end != '}' && end != ' ' && end != '\t' && end != '\r' && end != '\n') return false;
        buffer.add(JSONEventType.NUMBER, CompactNumber.of(negative ? -value : value,
                scale));
        return true;
    }
}
