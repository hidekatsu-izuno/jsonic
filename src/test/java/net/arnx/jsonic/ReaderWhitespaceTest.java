package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.IOException;
import java.io.StringReader;
import net.arnx.jsonic.io.ReaderInputSource;
import net.arnx.jsonic.parse.JSONParser;
import org.junit.jupiter.api.Test;

class ReaderWhitespaceTest {
    private static String events(String text, int depth, int chunk, boolean ignore, boolean legacy)
            throws IOException {
        ReaderInputSource[] holder = new ReaderInputSource[1];
        StringBuilder reads = new StringBuilder();
        StringReader reader = new StringReader(text) {
            @Override public int read(char[] chars, int offset, int length) throws IOException {
                reads.append(length).append(':').append(holder[0].getOffset()).append(':')
                        .append(holder[0].getLineNumber()).append(':').append(holder[0].getColumnNumber()).append(';');
                return super.read(chars, offset, Math.min(chunk, length));
            }
        };
        holder[0] = legacy ? new ReaderInputSource(reader) {} : new ReaderInputSource(reader);
        JSON json = new JSON(depth);
        JSONParser parser = new JSONParser(holder[0], depth, true, ignore, json.new Context().getLocalCache());
        StringBuilder result = new StringBuilder();
        try {
            JSONEventType event;
            while ((event = parser.next()) != null) {
                result.append(event).append(':').append(parser.getValue()).append(':').append(parser.getDepth())
                        .append(':').append(holder[0].getOffset()).append(':').append(holder[0].getLineNumber())
                        .append(':').append(holder[0].getColumnNumber()).append(';');
            }
        } catch (JSONException error) {
            result.append(error.getErrorCode()).append(':').append(error.getMessage()).append(':')
                    .append(error.getErrorOffset()).append(':').append(error.getLineNumber()).append(':')
                    .append(error.getColumnNumber());
        }
        return result.append('|').append(reads).toString();
    }

    @Test void whitespaceModesDepthErrorsAndReadCallbacksRemainIdentical() throws Exception {
        for (int depth : new int[] {1, 2, 32, 65}) {
            for (int chunk : new int[] {1, 2, 7, 1024}) {
                for (boolean ignore : new boolean[] {false, true}) {
                    for (String text : new String[] {"", " \t\r\n ", " [ 1,\t2,\r\n3 ] \t",
                            "{ \"a\" \t: \n true,\r\n \"b\": [ null, false ] }",
                            "[1,        ]", "[1, \r\n 2]?", "[1] \t [2]", "{\"a\": \n }",
                            "[\"x\", \r\n \"escaped\\n\"]", " ".repeat(2000) + "[]" + "\t".repeat(1500),
                            "[1," + " \t".repeat(700) + "\r\n2]", "[1,\u000b2]"}) {
                        assertEquals(events(text, depth, chunk, ignore, true),
                                events(text, depth, chunk, ignore, false), text);
                    }
                    String valid = " { \"a\" : [ 1, \r\n 2 ] } \t";
                    for (int length = 0; length <= valid.length(); length++) {
                        String prefix = valid.substring(0, length);
                        assertEquals(events(prefix, depth, chunk, ignore, true),
                                events(prefix, depth, chunk, ignore, false), prefix);
                    }
                }
            }
        }
    }

    @Test void sourceAndParserSubclassesKeepTheirOriginalCallbacks() throws Exception {
        ReaderInputSource source = new ReaderInputSource(new StringReader("[ \t true ]")) {
            @Override public int readSpaceTabPrefix() { throw new AssertionError("Use subclass scanner"); }
        };
        JSON json = new JSON();
        JSONParser parser = new JSONParser(source, 32, true, true, json.new Context().getLocalCache());
        assertEquals(JSONEventType.START_ARRAY, parser.next());
        assertEquals(JSONEventType.BOOLEAN, parser.next());
        assertEquals(JSONEventType.END_ARRAY, parser.next());
        assertNull(parser.next());
        int[] calls = {0};
        parser = new JSONParser(new ReaderInputSource(new StringReader("[ true ]")), 32, true, true,
                json.new Context().getLocalCache()) {
            @Override public boolean isIgnoreWhitespace() { calls[0]++; return super.isIgnoreWhitespace(); }
        };
        while (parser.next() != null) {}
        assertEquals(4, calls[0]);
    }

    @Test void prefixStopsBeforeNewlineAndRetainsMarkAndBack() throws Exception {
        ReaderInputSource source = new ReaderInputSource(new StringReader("[ \t  \r\n1]"));
        assertEquals('[', source.next());
        source.mark();
        assertEquals(4, source.readSpaceTabPrefix());
        assertEquals(" \t  ", source.copy(4));
        assertEquals(5, source.getOffset());
        assertEquals(5, source.getColumnNumber());
        assertEquals('\r', source.next());
        assertEquals(2, source.getLineNumber());
        assertEquals('\n', source.next());
        assertEquals(2, source.getLineNumber());
        assertEquals(0, source.getColumnNumber());
        assertEquals(0, source.readSpaceTabPrefix());
        assertEquals('1', source.next());
        source.back();
        assertEquals('1', source.next());
    }
}
