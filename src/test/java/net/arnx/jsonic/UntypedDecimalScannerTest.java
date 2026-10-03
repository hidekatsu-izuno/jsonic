package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;
import net.arnx.jsonic.io.CharSequenceInputSource;
import net.arnx.jsonic.io.InputSource;
import net.arnx.jsonic.io.ReaderInputSource;
import net.arnx.jsonic.io.StringInputSource;
import net.arnx.jsonic.parse.JSONParser;

public class UntypedDecimalScannerTest {
    /** Delegating without a concrete source type selects the original scanner. */
    private static final class OriginalSource implements InputSource {
        private final InputSource source;
        OriginalSource(InputSource source) { this.source = source; }
        public int next() throws IOException { return source.next(); }
        public void back() { source.back(); }
        public int mark() throws IOException { return source.mark(); }
        public void copy(StringBuilder text, int length) { source.copy(text, length); }
        public String copy(int length) { return source.copy(length); }
        public long getLineNumber() { return source.getLineNumber(); }
        public long getColumnNumber() { return source.getColumnNumber(); }
        public long getOffset() { return source.getOffset(); }
        public String toString() { return source.toString(); }
    }

    private static InputSource source(String input, int chunk) {
        if (chunk == -2) return new StringInputSource(input);
        if (chunk < 0) return new CharSequenceInputSource(input);
        if (chunk == 0) return new CharSequenceInputSource(new StringBuilder(input));
        return new ReaderInputSource(new StringReader(input) {
            @Override public int read(char[] buffer, int offset, int length) throws IOException {
                return super.read(buffer, offset, Math.min(length, chunk));
            }
        });
    }

    private static String events(InputSource input, int depth, boolean whitespace) throws IOException {
        JSON json = new JSON(depth);
        JSONParser parser = new JSONParser(input, depth, true, whitespace, json.new Context().getLocalCache());
        StringBuilder text = new StringBuilder();
        try {
            JSONEventType event;
            while ((event = parser.next()) != null) {
                Object value = parser.getValue();
                text.append(event).append(':').append(value).append(':').append(parser.getDepth())
                        .append(':').append(input.getLineNumber()).append(':').append(input.getColumnNumber())
                        .append(':').append(input.getOffset()).append(';');
                if (event == JSONEventType.NUMBER && value != null) {
                    BigDecimal number = assertInstanceOf(BigDecimal.class, value);
                    text.append(number.unscaledValue()).append('/').append(number.scale()).append(';');
                }
            }
        } catch (JSONException e) {
            text.append(e.getErrorCode()).append(':').append(e.getMessage()).append(':')
                    .append(e.getLineNumber()).append(':').append(e.getColumnNumber()).append(':').append(e.getErrorOffset());
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                text.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
        }
        return text.toString();
    }

    @Test public void customSourceNextCallsArePreserved() throws Exception {
        String input = "[123,1.25,12e2]";
        for (boolean reader : new boolean[] {false, true}) {
            int[] expected = {0};
            int[] actual = {0};
            InputSource original = reader ? countingReader(input, expected) : countingString(input, expected);
            InputSource candidate = reader ? countingReader(input, actual) : countingString(input, actual);
            assertEquals(events(new OriginalSource(original), 32, true), events(candidate, 32, true));
            assertEquals(expected[0], actual[0]);
        }
    }

    private static InputSource countingReader(String text, int[] calls) {
        return new ReaderInputSource(new StringReader(text)) {
            @Override public int next() throws IOException { calls[0]++; return super.next(); }
        };
    }

    private static InputSource countingString(String text, int[] calls) {
        return new StringInputSource(text) {
            @Override public int next() { calls[0]++; return super.next(); }
        };
    }

    @Test public void valuesScalesEventsAndPositionsMatchOriginalScanner() throws Exception {
        for (String number : new String[] {"0", "-0", "127", "128", "-0.00", "1.10", "1.00e2", "1e-999", "1e999",
                "123456789000.12345", "0.123456789012345678", "999999999999999999", "9223372036854775807",
                "-9223372036854775808", "1e0000", "1e2147483648", "1e-2147483648", "1e4294967296",
                "01", "1.", "1e", "1e+", "1e+-2", "1.1.2", "1e3x"}) {
            for (String input : new String[] {"[" + number + ",2]", "[" + number + "\r\n,2]?",
                    "{a:" + number + ",b:[0,1.00]}", number, number + " 2"}) {
                for (int chunk : new int[] {-2, -1, 0, 1, 7, 1004}) {
                    for (int depth : new int[] {1, 2, 3, 32, 65}) {
                        for (boolean whitespace : new boolean[] {false, true}) {
                            assertEquals(events(new OriginalSource(source(input, chunk)), depth, whitespace),
                                    events(source(input, chunk), depth, whitespace), input + " chunk=" + chunk);
                        }
                    }
                }
            }
        }
        Random random = new Random(0x6537);
        StringBuilder input = new StringBuilder("[");
        for (int i = 0; i < 500; i++) {
            if (i > 0) input.append(i % 9 == 0 ? ",\r\n" : ",");
            input.append(BigDecimal.valueOf(random.nextLong() % 100000000000000000L, random.nextInt(24) - 5));
        }
        input.append(']');
        for (String text : new String[] {input.toString(), input + "?"}) {
            for (int chunk : new int[] {-2, -1, 1, 7, 1004}) {
                assertEquals(events(new OriginalSource(source(text, chunk)), 32, true), events(source(text, chunk), 32, true));
            }
            for (String encoding : new String[] {"UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE", "UTF-32BE", "UTF-32LE"}) {
                byte[] bytes = text.getBytes(encoding);
                assertEquals(events(new OriginalSource(new ReaderInputSource(new ByteArrayInputStream(bytes))), 32, true),
                        events(new ReaderInputSource(new ByteArrayInputStream(bytes)), 32, true), encoding);
            }
        }
    }
}
