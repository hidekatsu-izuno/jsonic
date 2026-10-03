package net.arnx.jsonic.benchmark;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

/** Typed primitive binding through Reader, including indices above the boxing cache. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class TypedArrayBenchmark {
    @Param({"string", "reader"}) public String source;
    @Param({"double", "int", "long", "boolean"}) public String type;
    @Param({"100", "1000"}) public int count;
    @Param({"0", "100000"}) public int start;
    private JSON json;
    private JsonMapper jackson;
    private Class<?> target;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
        target = "double".equals(type) ? double[].class : "int".equals(type) ? int[].class
                : "long".equals(type) ? long[].class : boolean[].class;
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            if (i % 13 == 0) text.append("null");
            else if (target == boolean[].class) text.append((i & 1) == 0);
            else text.append(start + i);
        }
        input = text.append(']').toString();
        if (!JSON.encode(jsonicDecode()).equals(JSON.encode(jacksonDecode()))) {
            throw new IllegalStateException("Decoded typed values differ");
        }
    }

    @Benchmark public Object jsonicDecode() throws Exception { return "reader".equals(source) ? json.parse(new StringReader(input), target) : json.parse(input, target); }
    @Benchmark public Object jacksonDecode() throws Exception { return "reader".equals(source) ? jackson.readValue(new StringReader(input), target) : jackson.readValue(input, target); }
}
