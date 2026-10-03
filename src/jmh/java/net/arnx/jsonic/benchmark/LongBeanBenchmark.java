package net.arnx.jsonic.benchmark;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class LongBeanBenchmark {
    @Param({"small", "wide"}) public String distribution;
    @Param({"100"}) public int count;
    private JSON json;
    private JsonMapper jackson;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        Item[] items = new Item[count];
        for (int i = 0; i < count; i++) {
            Item item = new Item();
            item.id = "wide".equals(distribution) ? (i % 2 == 0 ? Long.MIN_VALUE + i : Long.MAX_VALUE - i) : i;
            item.boxed = item.id;
            item.setValue(item.id);
            items[i] = item;
        }
        input = jackson.writeValueAsString(items);
        String expected = json.format(jackson.readValue(input, Item[].class));
        if (!expected.equals(json.format(json.parse(input, Item[].class)))
                || !expected.equals(json.format(json.parse(new StringReader(input), Item[].class)))) {
            throw new IllegalStateException("Decoded long bean values differ");
        }
    }

    @Benchmark public Item[] jsonicDecode() { return json.parse(input, Item[].class); }
    @Benchmark public Item[] jacksonDecode() throws Exception { return jackson.readValue(input, Item[].class); }

    public static class Item {
        public long id;
        public Long boxed;
        private long value;
        public long getValue() { return value; }
        public void setValue(long value) { this.value = value; }
    }
}
