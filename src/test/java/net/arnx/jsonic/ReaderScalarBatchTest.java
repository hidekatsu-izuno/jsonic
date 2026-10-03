package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

import net.arnx.jsonic.io.ReaderInputSource;
import net.arnx.jsonic.parse.CompactNumber;
import net.arnx.jsonic.parse.JSONParser;
import org.junit.jupiter.api.Test;

class ReaderScalarBatchTest {
    private static Reader reader(String input, int chunk) {
        return new StringReader(input) {
            @Override public int read(char[] chars, int offset, int length) throws IOException {
                return super.read(chars, offset, Math.min(chunk, length));
            }
        };
    }

    private static String outcome(JSON json, String input, Class<?> type, int chunk) {
        try {
            Object value = json.parse(reader(input, chunk), type);
            return value instanceof double[] ? Arrays.toString((double[])value) : Arrays.toString((String[])value);
        } catch (JSONException error) {
            StringBuilder result = new StringBuilder(error.getErrorCode() + ":" + error.getMessage() + ":"
                    + error.getLineNumber() + ":" + error.getColumnNumber() + ":" + error.getErrorOffset());
            for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
                result.append(':').append(cause.getClass().getName()).append(':').append(cause.getMessage());
            }
            return result.toString();
        } catch (IOException error) {
            return error.toString();
        }
    }

    @Test void batchesMixedValuesAndFailuresRetainTheOriginalConversion() {
        String[] tokens = {"null", "0", "-0.0", "123.5", "1e3", "9007199254740993.12345", "1e309",
                "\"text\"", "\"\"", "\"日本語😀\"", "\"quote\\\"\"", "\"123\"", "true", "false", "{}", "[]", "[1]"};
        Random random = new Random(5601);
        for (Class<?> type : new Class<?>[]{double[].class, String[].class}) {
            for (int count : new int[]{0, 1, 31, 32, 33, 127, 128, 129, 1024, 4097}) {
                StringBuilder text = new StringBuilder("[");
                for (int i = 0; i < count; i++) {
                    if (i != 0) text.append(',');
                    text.append(i % 13 == 0 ? "null" : type == double[].class ? Integer.toString(i) : "\"item-" + i + "\"");
                }
                String prefix = text.toString();
                for (String suffix : new String[]{"]", "]?", ",]", ",0.0]", ",\"bad\"]", ",{}]", ",[]]", ",1e]", ",nullX]"}) {
                    for (int chunk : new int[]{3, 64, 1004}) {
                        String input = prefix + suffix;
                        assertEquals(outcome(new JSON(){}, input, type, chunk), outcome(new JSON(), input, type, chunk),
                                type + " count=" + count + " chunk=" + chunk + " suffix=" + suffix);
                    }
                }
            }
            for (int trial = 0; trial < 100; trial++) {
                StringBuilder text = new StringBuilder("[");
                for (int i = 0; i < 50; i++) {
                    if (i != 0) text.append(',');
                    text.append(tokens[random.nextInt(tokens.length)]);
                }
                String input = text.append(']').toString();
                for (int chunk : new int[]{1, 7, 1004}) {
                    assertEquals(outcome(new JSON(){}, input, type, chunk), outcome(new JSON(), input, type, chunk), input);
                }
            }
            for (int depth : new int[]{1, 2, 3, 32, 65}) {
                for (String input : new String[]{"[null,123,\"abc\",[1],true]", "\uFEFF [null, 1,\r\n\"x\"] \t"}) {
                    assertEquals(outcome(new JSON(depth){}, input, type, 7), outcome(new JSON(depth), input, type, 7));
                }
            }
            String valid = type == double[].class ? "[null,1,-0.0,2e3]" : "[null,\"\",\"日本語\",\"x\\n\"]";
            for (int length = 0; length <= valid.length(); length++) {
                String prefix = valid.substring(0, length);
                assertEquals(outcome(new JSON(){}, prefix, type, 1004), outcome(new JSON(), prefix, type, 1004), prefix);
            }
        }
    }

    private static String trace(String text, Class<?> type, int chunk, boolean legacy) throws Exception {
        ReaderInputSource[] source = {null}; JSONParser[] parser = {null}; StringBuilder reads = new StringBuilder();
        Reader input = new StringReader(text) {
            @Override public int read(char[] chars, int offset, int length) throws IOException {
                Object value = parser[0].getValue();
                if (value instanceof CompactNumber) value = ((CompactNumber)value).decimalValue();
                reads.append(offset).append('/').append(length).append(':').append(source[0].getOffset()).append(':')
                        .append(source[0].getLineNumber()).append(':').append(source[0].getColumnNumber()).append(':')
                        .append(parser[0].getDepth()).append(':').append(value).append(';');
                return super.read(chars, offset, Math.min(chunk, length));
            }
        };
        source[0] = legacy ? new ReaderInputSource(input){} : new ReaderInputSource(input);
        JSON json = new JSON(); JSONReader reader = new JSONReader(json.new Context(), source[0], false, true);
        Field field = JSONReader.class.getDeclaredField("parser"); field.setAccessible(true);
        parser[0] = (JSONParser)field.get(reader);
        try {
            Object result = reader.readTyped(type);
            return (result instanceof double[] ? Arrays.toString((double[])result) : Arrays.toString((String[])result)) + reads;
        } catch (JSONException error) {
            return error.getMessage() + ":" + error.getErrorOffset() + reads;
        }
    }

    @Test void refillsObserveTheSameCursorParserValuesAndReadRequests() throws Exception {
        for (Class<?> type : new Class<?>[]{double[].class, String[].class}) {
            String prefix = type == double[].class ? "[null,1,123.5" + ",null,1,123.5".repeat(200)
                    : "[null,\"one\",\"日本語\"" + ",null,\"one\",\"日本語\"".repeat(200);
            for (int chunk : new int[]{1, 2, 7, 64, 1004}) {
                for (String suffix : new String[]{"]", ",{}]", ",\"bad\\q\"]", ",\r\nnull]", ",nullX]", ",1e]"}) {
                    assertEquals(trace(prefix + suffix, type, chunk, true), trace(prefix + suffix, type, chunk, false),
                            type + " chunk=" + chunk + " suffix=" + suffix);
                }
            }
        }
    }
}
