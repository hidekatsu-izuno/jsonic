package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import net.arnx.jsonic.io.InputSource;
import net.arnx.jsonic.io.ReaderInputSource;
import net.arnx.jsonic.io.StringInputSource;
import net.arnx.jsonic.parse.JSONParser;
import org.junit.jupiter.api.Test;

class ParserTransitionTest {
    private static String events(String text, int depth, boolean ignore, boolean interpreter, int chunk,
            boolean legacy, boolean traceCalls) throws IOException {
        InputSource input = chunk == 0 ? new StringInputSource(text) : new ReaderInputSource(new StringReader(text) {
            @Override public int read(char[] buffer, int offset, int length) throws IOException {
                return super.read(buffer, offset, Math.min(chunk, length));
            }
        });
        StringBuilder calls = new StringBuilder();
        if (traceCalls) {
            InputSource original = input;
            input = new InputSource() {
                @Override public int next() throws IOException {
                    int value = original.next(); calls.append("n").append(value).append(';'); return value;
                }
                @Override public void back() { calls.append("b;"); original.back(); }
                @Override public int mark() throws IOException { int value = original.mark(); calls.append("m").append(value).append(';'); return value; }
                @Override public void copy(StringBuilder out, int length) { calls.append("c").append(length).append(';'); original.copy(out, length); }
                @Override public String copy(int length) { calls.append("s").append(length).append(';'); return original.copy(length); }
                @Override public long getLineNumber() { return original.getLineNumber(); }
                @Override public long getColumnNumber() { return original.getColumnNumber(); }
                @Override public long getOffset() { return original.getOffset(); }
                @Override public String toString() { return original.toString(); }
            };
        }
        JSON json = new JSON(depth);
        JSONParser parser = legacy
                ? new JSONParser(input, depth, interpreter, ignore, json.new Context().getLocalCache()) {}
                : new JSONParser(input, depth, interpreter, ignore, json.new Context().getLocalCache());
        StringBuilder result = new StringBuilder();
        try {
            JSONEventType type;
            while ((type = parser.next()) != null) {
                Object value = parser.getValue();
                result.append(type).append(':').append(value).append(':').append(parser.getDepth())
                        .append(':').append(input.getLineNumber()).append(':').append(input.getColumnNumber())
                        .append(':').append(input.getOffset()).append(';');
                if (value instanceof BigDecimal) {
                    BigDecimal number = (BigDecimal)value;
                    result.append(number.unscaledValue()).append('/').append(number.scale()).append(';');
                }
            }
        } catch (JSONException error) {
            result.append(error.getErrorCode()).append(':').append(error.getMessage())
                    .append(':').append(error.getLineNumber()).append(':').append(error.getColumnNumber())
                    .append(':').append(error.getErrorOffset());
        }
        return result.append('|').append(calls).toString();
    }

    @Test void eventsValuesDepthAndPositionsRetainBothWhitespaceModes() throws Exception {
        for (boolean ignore : new boolean[] {false, true}) {
            for (boolean interpreter : new boolean[] {false, true}) {
                for (int depth : new int[] {1, 2, 3, 32, 65}) {
                    for (int chunk : new int[] {0, 1, 7, 1024}) {
                        for (String text : new String[] {"[1,2,3]", "{\"a\":1,\"b\":2}",
                                "[{},[],null,true,false,\"a\",1.00e2]", "{\"a\": [1,2],\"b\":{\"c\":[]}}",
                                "[1,\r\n2, \t3]", "{\"a\" :\n 1, \"b\":2}", "[1,]", "{\"a\":}",
                                "{\"a\":1,}", "[1,2}", "[1,\"bad\\q\"]", "[1,2]?", "[1,2] [3,4]",
                                "[1/*x*/,2]", "[1,2e]", "[1,01]", ""}) {
                            assertEquals(events(text, depth, ignore, interpreter, chunk, true, false),
                                    events(text, depth, ignore, interpreter, chunk, false, false), text);
                        }
                        String valid = "{\"a\":[1,2],\"b\":null}";
                        for (int length = 0; length <= valid.length(); length++) {
                            String prefix = valid.substring(0, length);
                            assertEquals(events(prefix, depth, ignore, interpreter, chunk, true, false),
                                    events(prefix, depth, ignore, interpreter, chunk, false, false), prefix);
                        }
                    }
                }
            }
        }
    }

    @Test void customInputSourceOperationsRemainInTheSameOrder() throws Exception {
        for (int chunk : new int[] {0, 1, 7, 1024}) {
            for (String text : new String[] {"[1,2.00,{},[3],true,null]", "{\"a\":1,\"b\":[2,3]}",
                    "[1,\r\n2]?", "{\"a\":1,\"b\":}", "[1,2] [3]"}) {
                assertEquals(events(text, 32, true, true, chunk, true, true),
                        events(text, 32, true, true, chunk, false, true), text);
            }
        }
    }

    private static String reentrantEvents(String text, int trigger, boolean legacy) throws IOException {
        InputSource original = new StringInputSource(text);
        JSONParser[] holder = new JSONParser[1];
        boolean[] fired = {false};
        StringBuilder result = new StringBuilder();
        InputSource input = new InputSource() {
            @Override public int next() throws IOException {
                if (!fired[0] && original.getOffset() == trigger) {
                    fired[0] = true;
                    JSONEventType type = holder[0].next();
                    result.append("nested:").append(type).append(':').append(holder[0].getValue()).append(';');
                }
                return original.next();
            }
            @Override public void back() { original.back(); }
            @Override public int mark() throws IOException { return original.mark(); }
            @Override public void copy(StringBuilder out, int length) { original.copy(out, length); }
            @Override public String copy(int length) { return original.copy(length); }
            @Override public long getLineNumber() { return original.getLineNumber(); }
            @Override public long getColumnNumber() { return original.getColumnNumber(); }
            @Override public long getOffset() { return original.getOffset(); }
            @Override public String toString() { return original.toString(); }
        };
        JSON json = new JSON();
        holder[0] = legacy ? new JSONParser(input, 32, true, true, json.new Context().getLocalCache()) {}
                : new JSONParser(input, 32, true, true, json.new Context().getLocalCache());
        try {
            JSONEventType type;
            while ((type = holder[0].next()) != null) {
                result.append(type).append(':').append(holder[0].getValue()).append(';');
            }
        } catch (JSONException error) {
            result.append(error.getErrorCode()).append(':').append(error.getMessage())
                    .append(':').append(error.getErrorOffset());
        }
        return result.toString();
    }

    @Test void customInputSourceReentrySeesTheSameIntermediateState() throws Exception {
        assertEquals(reentrantEvents("[1,2,3]", 3, true), reentrantEvents("[1,2,3]", 3, false));
        assertEquals(reentrantEvents("{\"a\":1,\"b\":2}", 7, true),
                reentrantEvents("{\"a\":1,\"b\":2}", 7, false));
        assertEquals(reentrantEvents("{\"a\":1}", 5, true), reentrantEvents("{\"a\":1}", 5, false));
    }
}
