package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class PlainStringParsingTest {
    private void compare(String input) throws Exception {
        JSON json = new JSON();
        Object expected;
        try {
            expected = json.parse(new StringReader(input));
        } catch (JSONException error) {
            for (CharSequence source : new CharSequence[] {input, new StringBuilder(input), new StringBuffer(input)}) {
                JSONException actual = assertThrows(JSONException.class, () -> json.parse(source), input);
                assertEquals(error.getMessage(), actual.getMessage(), input);
            }
            return;
        }
        for (CharSequence source : new CharSequence[] {input, new StringBuilder(input), new StringBuffer(input)}) {
            assertEquals(expected, json.parse(source), input);
        }
    }

    @Test public void boundariesEscapesAndErrorPositionsMatchReader() throws Exception {
        for (String input : new String[] {
            "[\"\",\"ascii\",\"日本語😀\",\"a'b\"]", "{'a':'b',\"x\":\"y\"}",
            "[\"a\\nb\",\"a\\u1234\",\"a\\\"b\",\"a\\\\b\"]",
            "\r\n[\"plain\",\"next\",?]", "[\"plain\",\"unterminated",
            "[\"a\nb\"]", "[\"a\u007fb\"]", "[\"a\\q\"]", "[\"a\\u12x4\"]",
            "[\"" + "x".repeat(20000) + "\",\"end\"]"}) compare(input);
        for (int c = 0; c < 128; c++) compare("[\"before" + (char)c + "after\"]");
    }

    @Test public void repeatedStringsAndCacheCollisionsMatchReader() throws Exception {
        Random random = new Random(71021);
        StringBuilder document = new StringBuilder("[");
        for (int i = 0; i < 2000; i++) {
            if (i > 0) document.append(',');
            StringBuilder value = new StringBuilder();
            for (int j = random.nextInt(64); j > 0; j--) value.append((char)('a' + random.nextInt(26)));
            document.append(JSON.encode(i % 3 == 0 ? "repeated" : value.toString()));
        }
        compare(document.append(']').toString());
    }
}
