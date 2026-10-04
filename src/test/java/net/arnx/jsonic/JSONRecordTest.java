package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JSONRecordTest {
    public record Person(String name, int age) {}
    private record PrivateRecord(String value) {}
    record Empty() {}
    record Envelope<T>(T value, List<T> items) {}
    record Group(Person person, List<Person> people, Person[] array) {}
    record Hinted(@JSONHint(name = "display_name", ordinal = 0) String name,
            @JSONHint(format = "uuuu/MM/dd") LocalDate date,
            @JSONHint(ignore = true) int secret) {}
    record Styled(String firstName, int birthYear) {}
    record Primitives(boolean b, byte x, short s, int i, long l, float f, double d, char c, String text) {}
    record Validated(int value) {
        Validated {
            if (value < 0) throw new IllegalArgumentException("negative value");
        }
    }
    record Accessor(String value) {
        @Override public String value() { return value.toUpperCase(); }
    }
    record ExplicitAccessor(@JSONHint(name = "text") String value) {
        @Override public String value() { return value; }
    }
    record NestedGeneric<T>(Map<String, List<T>> values, T[] array) {}
    record Typed(@JSONHint(type = Person.class) Object person) {}
    public static class Bean {
        public Person person;
    }

    @Test void roundTrip() throws Exception {
        Person person = new Person("Alice", 20);
        String text = "{\"age\":20,\"name\":\"Alice\"}";
        assertEquals(text, JSON.encode(person));
        assertEquals(person, JSON.decode(text, Person.class));
        assertEquals(person, JSON.decode(new StringReader(text), Person.class));
        assertEquals(person, new JSON().convert(Map.of("name", "Alice", "age", 20), Person.class));
        assertEquals(new PrivateRecord("hello"), JSON.decode(JSON.encode(new PrivateRecord("hello")), PrivateRecord.class));
        assertEquals(new Empty(), JSON.decode("{}", Empty.class));
        assertEquals("{}", JSON.encode(new Empty()));
        assertNull(JSON.decode("null", Person.class));
        assertEquals("{\"value\":\"HELLO\"}", JSON.encode(new Accessor("hello")));
        assertEquals("{\"text\":\"hello\"}", JSON.encode(new ExplicitAccessor("hello")));
        assertEquals(new ExplicitAccessor("hello"), JSON.decode("{\"text\":\"hello\"}", ExplicitAccessor.class));
    }

    @Test void nestedAndGeneric() throws Exception {
        Person person = new Person("Alice", 20);
        Group group = new Group(person, List.of(person), new Person[] {person, person});
        Group copy = JSON.decode(JSON.encode(group), Group.class);
        assertEquals(group.person(), copy.person());
        assertEquals(group.people(), copy.people());
        assertArrayEquals(group.array(), copy.array());
        TypeReference<Envelope<Person>> type = new TypeReference<>() {};
        Envelope<Person> envelope = new Envelope<>(person, List.of(person));
        assertEquals(envelope, JSON.decode(JSON.encode(envelope), type));
        assertEquals(envelope, JSON.decode(new StringReader(JSON.encode(envelope)), type));
        TypeReference<List<Person>> listType = new TypeReference<>() {};
        assertEquals(List.of(person, person), JSON.decode(JSON.encode(List.of(person, person)), listType));
        assertEquals(List.of(person, person), JSON.decode(new StringReader(JSON.encode(List.of(person, person))), listType));
        TypeReference<NestedGeneric<Person>> nestedType = new TypeReference<>() {};
        NestedGeneric<Person> nested = new NestedGeneric<>(Map.of("people", List.of(person)), new Person[] {person});
        NestedGeneric<Person> nestedCopy = JSON.decode(JSON.encode(nested), nestedType);
        assertEquals(nested.values(), nestedCopy.values());
        assertArrayEquals(nested.array(), nestedCopy.array());
        assertEquals(new Typed(person), JSON.decode(JSON.encode(new Typed(person)), Typed.class));
        Bean bean = new Bean();
        bean.person = person;
        assertEquals(person, JSON.decode(JSON.encode(bean), Bean.class).person);
        assertEquals(person, JSON.decode(new StringReader(JSON.encode(bean)), Bean.class).person);
    }

    @Test void hintsAndNaming() {
        Hinted value = new Hinted("Alice", LocalDate.of(2026, 10, 4), 123);
        String text = "{\"display_name\":\"Alice\",\"date\":\"2026/10/04\"}";
        assertEquals(text, JSON.encode(value));
        assertEquals(new Hinted("Alice", value.date(), 0), JSON.decode(text, Hinted.class));
        assertEquals(new Hinted("Alice", value.date(), 0), JSON.decode(text.substring(0, text.length() - 1)
                + ",\"secret\":999}", Hinted.class));
        JSON json = new JSON();
        json.setPropertyStyle(NamingStyle.LOWER_UNDERSCORE);
        Styled styled = new Styled("Alice", 2000);
        assertEquals("{\"birth_year\":2000,\"first_name\":\"Alice\"}", json.format(styled));
        assertEquals(styled, json.parse(json.format(styled), Styled.class));
        assertEquals(styled, JSON.decode("{\"first_name\":\"Alice\",\"birth-year\":2000}", Styled.class));
    }

    @Test void defaultsAndFailures() {
        Primitives defaults = new Primitives(false, (byte)0, (short)0, 0, 0, 0, 0, '\0', null);
        assertEquals(defaults, JSON.decode("{}", Primitives.class));
        assertEquals(defaults, JSON.decode("{\"i\":null,\"unknown\":1}", Primitives.class));
        Primitives populated = new Primitives(true, (byte)2, (short)3, 4, 5, 6.5f, 7.5, 'A', "text");
        assertEquals(populated, JSON.decode(JSON.encode(populated), Primitives.class));
        assertEquals(new Person("last", 0), JSON.decode("{\"name\":\"first\",\"name\":\"last\"}", Person.class));
        assertThrows(JSONException.class, () -> JSON.decode("{\"value\":-1}", Validated.class));
        assertThrows(JSONException.class, () -> JSON.decode("{\"age\":2147483648}", Person.class));
        assertThrows(JSONException.class, () -> JSON.decode("[]", Person.class));
    }

    @Test void customizedCallbacksAndPrettyPrint() {
        JSON json = new JSON() {
            @Override protected String normalize(String name) { return "r_" + name; }
        };
        Person person = new Person("Alice", 20);
        assertEquals("{\"r_age\":20,\"r_name\":\"Alice\"}", json.format(person));
        assertEquals(person, json.parse(json.format(person), Person.class));
        json.setPrettyPrint(true);
        assertEquals(person, json.parse(json.format(person), Person.class));
    }
}
