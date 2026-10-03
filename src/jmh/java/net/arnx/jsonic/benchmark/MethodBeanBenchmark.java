package net.arnx.jsonic.benchmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

/** Same payload as BeanBenchmark, using methods for every property. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class MethodBeanBenchmark {
    @Param({"1", "100"}) public int count;
    private JSON json;
    private JsonMapper jackson;
    private Item[] values;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        values = new Item[count];
        for (int i = 0; i < count; i++) {
            Item item = new Item();
            item.setId(i);
            item.setName("item-" + i);
            item.setActive(i % 2 == 0);
            item.setPrice(123.5 + i);
            item.setTags(new String[] {"new", "sale"});
            values[i] = item;
        }
        input = jackson.writeValueAsString(values);
        if (!input.equals(json.format(values)) || !input.equals(jackson.writeValueAsString(json.parse(input, Item[].class)))) {
            throw new IllegalStateException("Bean payloads differ");
        }
    }

    @Benchmark public String jsonicEncode() { return json.format(values); }
    @Benchmark public Item[] jsonicDecode() { return json.parse(input, Item[].class); }
    @Benchmark public String jacksonEncode() throws Exception { return jackson.writeValueAsString(values); }
    @Benchmark public Item[] jacksonDecode() throws Exception { return jackson.readValue(input, Item[].class); }

    public static class Item {
        private int id;
        private String name;
        private boolean active;
        private double price;
        private String[] tags;
        public int getId() { return id; }
        public void setId(int value) { id = value; }
        public String getName() { return name; }
        public void setName(String value) { name = value; }
        public boolean isActive() { return active; }
        public void setActive(boolean value) { active = value; }
        public double getPrice() { return price; }
        public void setPrice(double value) { price = value; }
        public String[] getTags() { return tags; }
        public void setTags(String[] value) { tags = value; }
    }
}
