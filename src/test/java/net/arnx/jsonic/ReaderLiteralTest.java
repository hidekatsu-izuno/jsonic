package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.StringReader;
import net.arnx.jsonic.io.ReaderInputSource;
import net.arnx.jsonic.parse.JSONParser;
import org.junit.jupiter.api.Test;

class ReaderLiteralTest {
    @Test void internedAndNonInternedArgumentsRetainTheSameProbeBehavior() throws Exception {
        for (String expected : new String[] {"true", "false", "null", "word"}) {
            for (String argument : new String[] {expected, new String(expected)}) {
                ReaderInputSource input = new ReaderInputSource(new StringReader(expected + "X"));
                input.next(); input.back(); input.mark();
                assertTrue(input.readLiteral(argument));
                assertEquals(expected.length(), input.getOffset());
                assertEquals(expected, input.copy(expected.length()));
                assertEquals('X', input.next());
                input = new ReaderInputSource(new StringReader(expected.substring(0, expected.length() - 1) + "X"));
                input.next(); input.back(); input.mark();
                assertFalse(input.readLiteral(argument));
                assertEquals(0, input.getOffset());
            }
        }
    }

    private static String events(String text, int depth, int chunk, boolean interpreter, boolean legacy)
            throws IOException {
        StringReader reader = new StringReader(text) {
            @Override public int read(char[] chars, int offset, int length) throws IOException {
                return super.read(chars, offset, Math.min(chunk, length));
            }
        };
        ReaderInputSource input = legacy ? new ReaderInputSource(reader) {} : new ReaderInputSource(reader);
        JSON json = new JSON(depth);
        JSONParser parser = new JSONParser(input, depth, interpreter, false, json.new Context().getLocalCache());
        StringBuilder result = new StringBuilder();
        try {
            JSONEventType type;
            while ((type = parser.next()) != null) {
                result.append(type).append(':').append(parser.getValue()).append(':').append(parser.getDepth())
                        .append(':').append(input.getLineNumber()).append(':').append(input.getColumnNumber())
                        .append(':').append(input.getOffset()).append(';');
            }
        } catch (JSONException error) {
            result.append(error.getErrorCode()).append(':').append(error.getMessage())
                    .append(':').append(error.getLineNumber()).append(':').append(error.getColumnNumber())
                    .append(':').append(error.getErrorOffset());
        }
        return result.toString();
    }

    @Test void bufferedLiteralsAndFailuresRetainAllEventsAndPositions() throws Exception {
        for (int depth : new int[] {1, 2, 3, 32, 65}) {
            for (int chunk : new int[] {1, 2, 3, 7, 1024}) {
                for (boolean interpreter : new boolean[] {false, true}) {
                    for (String text : new String[] {"[true,false,null]", "{\"a\":true,\"b\":[false,null]}",
                            "true", "false", "null", "true false null", "[trueX]", "[false0]", "[null0]",
                            "[trux]", "[falsx]", "[nulL]", "[true,]", "[null]?", "[\r\ntrue,\nfalse,null]"}) {
                        assertEquals(events(text, depth, chunk, interpreter, true),
                                events(text, depth, chunk, interpreter, false), text);
                    }
                    String valid = "[true,false,null]";
                    for (int length = 0; length <= valid.length(); length++) {
                        String prefix = valid.substring(0, length);
                        assertEquals(events(prefix, depth, chunk, interpreter, true),
                                events(prefix, depth, chunk, interpreter, false), prefix);
                    }
                }
            }
        }
    }

    @Test void successfulAndFailedProbesRetainCursorMarkAndBacktracking() throws Exception {
        for (String literal : new String[] {"true", "false", "null"}) {
            ReaderInputSource input = new ReaderInputSource(new StringReader(literal + ","));
            assertEquals(literal.charAt(0), input.next());
            input.back();
            input.mark();
            assertTrue(input.readLiteral(literal));
            assertEquals(literal, input.copy(literal.length()));
            assertEquals(literal.length(), input.getOffset());
            assertEquals(literal.length(), input.getColumnNumber());
            input.back();
            assertEquals(literal.charAt(literal.length() - 1), input.next());
            assertEquals(',', input.next());
            input = new ReaderInputSource(new StringReader(literal.substring(0, literal.length() - 1) + "X"));
            assertEquals(literal.charAt(0), input.next());
            input.back();
            input.mark();
            assertFalse(input.readLiteral(literal));
            assertEquals(0, input.getOffset());
            assertEquals(literal.charAt(0), input.next());
        }
        ReaderInputSource input = new ReaderInputSource(new StringReader("a\nb"));
        assertEquals('a', input.next());
        input.back();
        input.mark();
        assertFalse(input.readLiteral("a\nb"));
        assertEquals(0, input.getOffset());
        assertEquals(1, input.getLineNumber());
    }

    @Test void sourceSubclassKeepsItsOriginalScannerCallbacks() throws Exception {
        ReaderInputSource source = new ReaderInputSource(new StringReader("[true,false,null]")) {
            @Override public boolean readLiteral(String expected) {
                throw new AssertionError("Parser must keep the subclass scanner");
            }
        };
        JSON json = new JSON();
        JSONParser parser = new JSONParser(source, 32, true, true, json.new Context().getLocalCache());
        assertEquals(JSONEventType.START_ARRAY, parser.next());
        assertEquals(JSONEventType.BOOLEAN, parser.next());
        assertEquals(Boolean.TRUE, parser.getValue());
        assertEquals(JSONEventType.BOOLEAN, parser.next());
        assertEquals(Boolean.FALSE, parser.getValue());
        assertEquals(JSONEventType.NULL, parser.next());
        assertEquals(JSONEventType.END_ARRAY, parser.next());
        assertEquals(null, parser.next());
    }
}
