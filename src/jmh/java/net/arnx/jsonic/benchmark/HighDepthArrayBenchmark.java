package net.arnx.jsonic.benchmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

/** Flat primitive arrays with a configured depth limit above the scanner guard. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class HighDepthArrayBenchmark {
    @Param({"float", "double", "int", "long", "boolean"}) public String type;
    @Param({"32", "65"}) public int maxDepth;
    @Param({"1000"}) public int count;
    private JSON json;
    private JsonMapper jackson;
    private Class<?> target;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON(maxDepth);
        jackson = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
        target = "float".equals(type) ? float[].class : "double".equals(type) ? double[].class
                : "int".equals(type) ? int[].class : "long".equals(type) ? long[].class : boolean[].class;
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            if (i % 13 == 0) text.append("null");
            else if (target == boolean[].class) text.append((i & 1) == 0);
            else text.append(100000 + i);
        }
        input = text.append(']').toString();
        if (!JSON.encode(jsonicDecode()).equals(JSON.encode(jacksonDecode()))) {
            throw new IllegalStateException("Decoded typed values differ");
        }
    }

    @Benchmark public Object jsonicDecode() { return json.parse(input, target); }
    @Benchmark public Object jacksonDecode() throws Exception { return jackson.readValue(input, target); }
}
