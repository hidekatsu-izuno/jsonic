package net.arnx.jsonic.benchmark;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

/** Untyped mixed data, with repeated and varying map keys. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class MapBenchmark {
    @Param({"1", "2", "100", "1000"}) public int count;
    @Param({"plain", "japanese", "escaped", "unique", "escapedUnique"}) public String keys;
    private JSON json;
    private JsonMapper jackson;
    private Map<String, Object>[] values;
    private String input;

    @SuppressWarnings("unchecked")
    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        values = (Map<String, Object>[])new Map<?, ?>[count];
        for (int i = 0; i < count; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put(key("active", i), i % 2 == 0);
            item.put(key("id", i), i);
            item.put(key("name", i), "item-" + i);
            item.put(key("price", i), 123.5 + i);
            item.put(key("tags", i), new String[] {"new", "sale"});
            values[i] = item;
        }
        input = jackson.writeValueAsString(values);
        if (!input.equals(json.format(values)) || !input.equals(json.format(json.parse(input)))) {
            throw new IllegalStateException("Map payloads differ");
        }
    }

    private String key(String name, int i) {
        if ("japanese".equals(keys)) return "項目_" + name;
        if ("escaped".equals(keys)) return "\"\n\\_" + name;
        if ("escapedUnique".equals(keys)) return "\"\n\\_" + name + "_" + i;
        if ("unique".equals(keys)) return name + "_" + i;
        return name;
    }

    @Benchmark public String jsonicEncode() { return json.format(values); }
    @Benchmark public Object jsonicDecode() { return json.parse(input); }
    @Benchmark public String jacksonEncode() throws Exception { return jackson.writeValueAsString(values); }
    @Benchmark public Object jacksonDecode() throws Exception { return jackson.readValue(input, Object.class); }
}
