package net.arnx.jsonic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.arnx.jsonic.JSON.Context;

/** Builds ordinary untyped trees without the event dispatch layer. */
final class StringTreeDecoder extends StringScanner {
    private static final Object FAILED = new Object();

    private StringTreeDecoder(Context context, String input) {
        super(context, input);
    }

    static Object scan(Context context, String input) {
        if (context.getMaxDepth() <= 2 || context.getMaxDepth() > 64) return null;
        int start = 0;
        while (start < input.length()) {
            char c = input.charAt(start);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') break;
            start++;
        }
        if (start == input.length()) return null;
        char first = input.charAt(start);
        if (first != '[' && first != '{') return null;
        StringTreeDecoder scanner = new StringTreeDecoder(context, input);
        scanner.position = start;
        Object result = scanner.value(0);
        if (result == FAILED) return null;
        scanner.whitespace();
        return scanner.position == scanner.length ? result : null;
    }

    private Object value(int depth) {
        if (depth + 1 >= maxDepth) return FAILED;
        whitespace();
        if (position == length) return FAILED;
        switch (input.charAt(position)) {
        case '{': return object(depth);
        case '[': return array(depth);
        case '"': {
            String text = string();
            return text != null ? text : FAILED;
        }
        case 't': return literal("true", Boolean.TRUE);
        case 'f': return literal("false", Boolean.FALSE);
        case 'n': return literal("null", null);
        default: {
            Object number = number();
            return number != null ? number : FAILED;
        }
        }
    }

    private Object literal(String literal, Object value) {
        if (!input.startsWith(literal, position)) return FAILED;
        position += literal.length();
        return value;
    }

    private Object object(int depth) {
        position++;
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        whitespace();
        if (take('}')) return values;
        int ordinal = 0;
        do {
            whitespace();
            if (position == length || input.charAt(position) != '"') return FAILED;
            String key = name(depth, ordinal++);
            if (key == null) return FAILED;
            whitespace();
            if (!take(':')) return FAILED;
            Object value = value(depth + 1);
            if (value == FAILED) return FAILED;
            // LinkedHashMap retains the first key order and the last value.
            values.put(key, value);
            whitespace();
            if (take('}')) return values;
        } while (take(','));
        return FAILED;
    }

    private Object array(int depth) {
        position++;
        whitespace();
        if (take(']')) return new ArrayList<>(0);
        ArrayList<Object> values = new ArrayList<>(2);
        do {
            Object value = value(depth + 1);
            if (value == FAILED) return FAILED;
            values.add(value);
            whitespace();
            if (take(']')) {
                values.trimToSize();
                return values;
            }
            if (!take(',')) return FAILED;
            whitespace();
            if (position < length && input.charAt(position) == '{') repeatedObjects = true;
        } while (true);
    }
}
