package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.TimeZone;
import net.arnx.jsonic.util.LocalCache;
import net.arnx.jsonic.io.ReaderInputSource;
import net.arnx.jsonic.parse.JSONParser;
import org.junit.jupiter.api.Test;

class ReaderPlainStringTest {
    private static String events(String text, int chunk, int depth, String encoding, boolean legacy)
            throws IOException {
        ReaderInputSource input;
        if (encoding == null) {
            StringReader reader = new StringReader(text) {
                @Override public int read(char[] chars, int offset, int length) throws IOException {
                    return super.read(chars, offset, Math.min(chunk, length));
                }
            };
            input = legacy ? new ReaderInputSource(reader) {} : new ReaderInputSource(reader);
        } else {
            ByteArrayInputStream stream = new ByteArrayInputStream(text.getBytes(Charset.forName(encoding)));
            input = legacy ? new ReaderInputSource(stream) {} : new ReaderInputSource(stream);
        }
        JSON json = new JSON(depth);
        JSONParser parser = new JSONParser(input, depth, true, false, json.new Context().getLocalCache());
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
                    .append(':').append(error.getErrorOffset());
        }
        return result.toString();
    }

    @Test void valuesPositionsErrorsAndRefillsMatchTheOriginalScanner() throws Exception {
        for (int depth : new int[] {1, 2, 32}) {
            for (int chunk : new int[] {1, 2, 7, 1024}) {
                for (String text : new String[] {"[\"\",\"same\",\"same\",\"日本語😀\"]",
                        "{\"a\":\"text\",\"b\":[\"other\",{\"c\":\"same\"}]}",
                        "[\"quote\\\"\",\"slash\\\\\",\"line\\n\",\"unicode\\u1234\"]",
                        "[\"bad\\q\"]", "[\"bad\nline\"]", "[\"bad\u007f\"]", "[\"unfinished",
                        "\"root\" \"second\"", "[\r\n\"text\",\n\"next\"]",
                        "[\"" + "x".repeat(1003) + "\",\"after\"]",
                        "[\"" + "日本語".repeat(700) + "\",\"after\"]"}) {
                    assertEquals(events(text, chunk, depth, null, true),
                            events(text, chunk, depth, null, false), text);
                }
                String valid = "{\"a\":[\"plain\",\"日本語\"],\"b\":\"escaped\\n\"}";
                for (int length = 0; length <= valid.length(); length++) {
                    String prefix = valid.substring(0, length);
                    assertEquals(events(prefix, chunk, depth, null, true),
                            events(prefix, chunk, depth, null, false), prefix);
                }
            }
        }
        for (String encoding : new String[] {"UTF-8", "UTF-16", "UTF-16LE", "UTF-16BE", "UTF-32LE", "UTF-32BE"}) {
            String text = "[\"日本語😀\",\"plain\",\"escaped\\n\",\"" + "x".repeat(1024) + "\"]";
            assertEquals(events(text, 1024, 32, encoding, true), events(text, 1024, 32, encoding, false), encoding);
        }
    }


    @Test void bulkPrefixStopsBeforeQuotesEscapesAndControlCharacters() throws Exception {
        for (String text : new String[] {"\"plain\"", "\"plain\\n\"", "\"plain\nline\"", "\"plain\u007f\""}) {
            ReaderInputSource input = new ReaderInputSource(new StringReader(text));
            assertEquals('"', input.next());
            input.mark();
            assertEquals(5, input.readPlainStringPrefix('"'));
            assertEquals("plain", input.copy(5));
            assertEquals(6, input.getOffset());
            assertEquals(6, input.getColumnNumber());
            assertEquals(text.charAt(6), input.next());
        }
    }

    @Test void bulkPrefixCanReachTheBufferEndWithoutRefilling() throws Exception {
        ReaderInputSource input = new ReaderInputSource(new StringReader("\"" + "x".repeat(2000) + "\""));
        assertEquals('"', input.next());
        int available = input.mark();
        assertEquals(available, input.readPlainStringPrefix('"'));
        assertEquals("x".repeat(available), input.copy(available));
        assertEquals(available + 1, input.getOffset());
        assertEquals('x', input.next());
    }

    @Test void customInputAndCacheKeepTheirOriginalCallbacks() throws Exception {
        ReaderInputSource subclass = new ReaderInputSource(new StringReader("[\"plain\"]")) {
            @Override public int readPlainStringPrefix(int quote) {
                throw new AssertionError("Parser must keep the subclass scanner");
            }
        };
        JSON json = new JSON();
        JSONParser parser = new JSONParser(subclass, 32, true, true, json.new Context().getLocalCache());
        assertEquals(JSONEventType.START_ARRAY, parser.next());
        assertEquals(JSONEventType.STRING, parser.next());
        assertEquals("plain", parser.getValue());
        assertEquals(JSONEventType.END_ARRAY, parser.next());
        assertNull(parser.next());
        int[] calls = {0};
        LocalCache custom = new LocalCache("net.arnx.jsonic.Messages", Locale.ROOT, TimeZone.getTimeZone("UTC")) {
            @Override public String getString(CharSequence chars) {
                calls[0]++;
                return super.getString(chars);
            }
        };
        ReaderInputSource input = new ReaderInputSource(new StringReader("[\"plain\"]"));
        parser = new JSONParser(input, 32, true, true, custom);
        assertEquals(JSONEventType.START_ARRAY, parser.next());
        assertEquals(JSONEventType.STRING, parser.next());
        assertEquals("plain", parser.getValue());
        assertEquals(1, calls[0]);
        assertEquals(JSONEventType.END_ARRAY, parser.next());
        assertNull(parser.next());
    }
}
