package net.arnx.jsonic.benchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import net.arnx.jsonic.JSON;

/** Warmed, shared-instance and convenience-API POJO benchmarks. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class BeanBenchmark {
    @Param({"1", "100"})
    public int count;

    @Param({"ascii", "japanese"})
    public String text;

    @Param({"plain", "scientific"})
    public String priceNotation;

    private JSON json;
    private JsonMapper jackson;
    private Item[] items;
    private String input;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        items = new Item[count];
        for (int i = 0; i < count; i++) {
            Item item = new Item();
            item.id = i;
            item.setName("japanese".equals(text) ? "日本語の商品\"名\n" + i : "item-" + i);
            item.active = (i % 2 == 0);
            item.price = "scientific".equals(priceNotation) ? 1.2345e100 * (i + 1) : 123.5 + i;
            item.tags = new String[] {"new", "sale"};
            items[i] = item;
        }
        input = jackson.writeValueAsString(items);
        if (!input.equals(json.format(items))) {
            throw new IllegalStateException("Serialized payloads differ");
        }
        if (!input.equals(jackson.writeValueAsString(json.parse(input, Item[].class)))) {
            throw new IllegalStateException("Decoded payload differs");
        }
        if (!input.equals(jackson.writeValueAsString(jsonicReaderDecode()))) {
            throw new IllegalStateException("Reader decoded payload differs");
        }
    }

    @Benchmark public String jsonicEncode() { return json.format(items); }
    @Benchmark public String jsonicStaticEncode() { return JSON.encode(items); }
    @Benchmark public Item[] jsonicDecode() { return json.parse(input, Item[].class); }
    @Benchmark public Item[] jsonicStaticDecode() { return JSON.decode(input, Item[].class); }
    @Benchmark public String jacksonEncode() throws Exception { return jackson.writeValueAsString(items); }
    @Benchmark public Item[] jacksonDecode() throws Exception { return jackson.readValue(input, Item[].class); }

    @Benchmark public Item[] jsonicReaderDecode() throws Exception {
        return json.parse(new java.io.StringReader(input), Item[].class);
    }
    @Benchmark public Item[] jacksonReaderDecode() throws Exception {
        return jackson.readValue(new java.io.StringReader(input), Item[].class);
    }

    public static class Item {
        public int id;
        private String name;
        public boolean active;
        public double price;
        public String[] tags;

        public String getName() { return name; }
        public void setName(String value) { name = value; }
    }
}
