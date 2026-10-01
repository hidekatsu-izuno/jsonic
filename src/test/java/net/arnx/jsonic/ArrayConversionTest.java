package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ArrayConversionTest {
    @Test
    public void referencePrimitiveAndGenericArrays() {
        assertArrayEquals(new String[] {"a", null, "b"}, JSON.decode("[\"a\",null,\"b\"]", String[].class));
        assertArrayEquals(new String[] {"a"}, JSON.decode("\"a\"", String[].class));
        assertArrayEquals(new String[0], JSON.decode("[]", String[].class));
        assertArrayEquals(new int[] {1, 2}, JSON.decode("[1,2]", int[].class));
        assertArrayEquals(new int[] {3}, JSON.decode("3", int[].class));
        String[][] nested = JSON.decode("[[\"a\"],null,[\"b\"]]", String[][].class);
        assertArrayEquals(new String[] {"a"}, nested[0]);
        assertNull(nested[1]);
        assertArrayEquals(new String[] {"b"}, nested[2]);
        List<Integer>[] generic = JSON.decode("[[1,2],null,[3]]", new TypeReference<List<Integer>[]>() {});
        assertEquals(Arrays.asList(1, 2), generic[0]);
        assertNull(generic[1]);
        assertEquals(Arrays.asList(3), generic[2]);
    }

    @Test
    public void incompatibleCustomValuesKeepReflectiveAssignmentError() {
        JSON json = new JSON() {
            @SuppressWarnings("unchecked")
            @Override protected <T> T postparse(Context context, Object value,
                    Class<? extends T> cls, Type type) throws Exception {
                if (cls == String.class) return (T)new Object();
                return super.postparse(context, value, cls, type);
            }
        };
        for (String input : Arrays.asList("[\"a\"]", "\"a\"")) {
            try {
                json.parse(input, String[].class);
                fail("Expected incompatible array element to fail");
            } catch (JSONException e) {
                assertEquals(JSONException.POSTPARSE_ERROR, e.getErrorCode());
                assertTrue(e.getCause() instanceof IllegalArgumentException);
                assertTrue(e.getMessage().contains("[0]"));
            }
        }
    }
}
