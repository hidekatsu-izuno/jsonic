package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Base64;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class SerializableHintTest {
    public static class FieldBean {
        @JSONHint(type = Serializable.class)
        public Serializable value;
    }

    public static class MethodBean {
        private Serializable value;
        @JSONHint(type = Serializable.class)
        public Serializable getValue() { return value; }
        @JSONHint(type = Serializable.class)
        public void setValue(Serializable value) { this.value = value; }
    }

    public static class CombinedBean {
        @JSONHint(type = Serializable.class, serialized = true)
        public Serializable value;
    }

    public static class Marker implements Serializable {
        private static final long serialVersionUID = 1L;
        static boolean read;
        static boolean written;
        private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
            read = true;
            input.defaultReadObject();
        }
        private void writeObject(ObjectOutputStream output) throws IOException {
            written = true;
            output.defaultWriteObject();
        }
    }

    private static void assertRemoved(org.junit.jupiter.api.function.Executable action) {
        Throwable error = assertThrows(JSONException.class, action);
        while (error.getCause() != null) error = error.getCause();
        assertInstanceOf(UnsupportedOperationException.class, error);
        assertTrue(error.getMessage().contains("@JSONHint(type=Serializable.class)"));
    }

    @Test
    void refusesSerializedInputBeforeReadObjectRuns() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(new Marker());
        }
        String value = Base64.getEncoder().encodeToString(bytes.toByteArray());
        String input = "{\"value\":\"" + value + "\"}";
        Marker.read = false;
        for (Class<?> type : new Class<?>[] {FieldBean.class, MethodBean.class, CombinedBean.class}) {
            assertRemoved(() -> JSON.decode(input, type));
            assertRemoved(() -> new JSON().parse(new StringReader(input), type));
            assertRemoved(() -> new JSON().convert(Collections.singletonMap("value", value), type));
            // Exercise the legacy tree binding path as well as typed decoding.
            JSON legacy = new JSON();
            legacy.setMaxDepth(128);
            assertRemoved(() -> legacy.parse(input, type));
            assertRemoved(() -> JSON.decode("{\"value\":null}", type));
        }
        assertFalse(Marker.read);
    }

    @Test
    void refusesOutputBeforeWriteObjectRuns() {
        FieldBean field = new FieldBean();
        field.value = new Marker();
        MethodBean method = new MethodBean();
        method.setValue(new Marker());
        CombinedBean combined = new CombinedBean();
        combined.value = new Marker();
        Marker.written = false;
        for (Object bean : new Object[] {field, method, combined, new FieldBean()}) {
            assertRemoved(() -> JSON.encode(bean));
        }
        assertFalse(Marker.written);
    }
}
