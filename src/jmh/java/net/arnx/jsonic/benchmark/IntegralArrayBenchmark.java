package net.arnx.jsonic.benchmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

/** Signed integer arrays, including long boundaries and mixed quoted values. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class IntegralArrayBenchmark {
    @Param({"int", "long"}) public String type;
    @Param({"small", "wide", "quoted", "mixed"}) public String distribution;
    @Param({"100"}) public int count;
    private JSON json;
    private JsonMapper jackson;
    private String input;
    private Class<?> target;
    private Object values;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        target = "long".equals(type) ? long[].class : int[].class;
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            if ("quoted".equals(distribution) && (i & 1) != 0) text.append('"');
            if ("mixed".equals(distribution)) {
                text.append(i % 4 == 0 ? (long)Integer.MIN_VALUE + i : i % 4 == 1 ? (long)Integer.MAX_VALUE - i
                        : i % 4 == 2 ? (target == long[].class ? Long.MAX_VALUE - i : Integer.MAX_VALUE - i) : -i);
            } else text.append("wide".equals(distribution)
                    ? (target == long[].class ? Long.MAX_VALUE - i : Integer.MAX_VALUE - i) : i);
            if ("quoted".equals(distribution) && (i & 1) != 0) text.append('"');
        }
        input = text.append(']').toString();
        values = json.parse(input, target);
        if (!JSON.encode(values).equals(JSON.encode(jackson.readValue(input, target)))) {
            throw new IllegalStateException("Decoded integral values differ");
        }
    }

    @Benchmark public String jsonicEncode() { return json.format(values); }
    @Benchmark public String jacksonEncode() throws Exception { return jackson.writeValueAsString(values); }
    @Benchmark public Object jsonicDecode() { return json.parse(input, target); }
    @Benchmark public Object jacksonDecode() throws Exception { return jackson.readValue(input, target); }
}
