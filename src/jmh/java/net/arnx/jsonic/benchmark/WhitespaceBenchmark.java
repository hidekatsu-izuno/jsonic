package net.arnx.jsonic.benchmark;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import net.arnx.jsonic.JSON;
import net.arnx.jsonic.JSONEventType;
import net.arnx.jsonic.JSONReader;
import tools.jackson.databind.json.JsonMapper;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class WhitespaceBenchmark {
    @Param({"array", "bean"}) public String payload;
    @Param({"compact", "pretty", "indented"}) public String layout;
    private JSON json;
    private JsonMapper jackson;
    private String input;
    private Class<?> target;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        Object values;
        if ("array".equals(payload)) {
            int[] numbers = new int[1000];
            for (int i = 0; i < numbers.length; i++) numbers[i] = i;
            values = numbers;
            target = int[].class;
        } else {
            BeanBenchmark.Item[] items = new BeanBenchmark.Item[100];
            for (int i = 0; i < items.length; i++) {
                BeanBenchmark.Item item = new BeanBenchmark.Item();
                item.id = i; item.setName("item-" + i); item.active = (i & 1) == 0;
                item.price = 123.5 + i; item.tags = new String[] {"new", "sale"}; items[i] = item;
            }
            values = items;
            target = BeanBenchmark.Item[].class;
        }
        input = "compact".equals(layout) ? jackson.writeValueAsString(values)
                : jackson.writerWithDefaultPrettyPrinter().writeValueAsString(values);
        if ("indented".equals(layout)) {
            if ("array".equals(payload)) {
                StringBuilder text = new StringBuilder("[\r\n");
                for (int i = 0; i < 1000; i++) text.append("        ").append(i).append(i == 999 ? "\r\n]" : ",\r\n");
                input = text.toString();
            } else input = input.replace("\n", "\r\n        ");
        }
        String expected = jackson.writeValueAsString(values);
        if (!expected.equals(jackson.writeValueAsString(jsonicDecode()))
                || !expected.equals(jackson.writeValueAsString(jacksonDecode()))) {
            throw new IllegalStateException("Decoded values differ");
        }
    }

    @Benchmark public Object jsonicDecode() throws Exception { return json.parse(new StringReader(input), target); }
    @Benchmark public Object jacksonDecode() throws Exception { return jackson.readValue(new StringReader(input), target); }
    @Benchmark public int jsonicStringEvents() throws Exception {
        JSONReader reader = json.getReader(input);
        int count = 0;
        JSONEventType event;
        while ((event = reader.next()) != null) {
            if (event == JSONEventType.STRING || event == JSONEventType.NAME) count += reader.getString().length();
            else count++;
        }
        return count;
    }
}
