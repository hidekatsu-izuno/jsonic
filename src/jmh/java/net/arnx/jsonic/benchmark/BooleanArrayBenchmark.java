package net.arnx.jsonic.benchmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class BooleanArrayBenchmark {
    @Param({"literal", "quoted", "numeric", "numericLast", "escaped"}) public String distribution;
    @Param({"100", "1000"}) public int count;
    private JSON json;
    private JsonMapper jackson;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            if (i % 13 == 0) text.append("null");
            else if ("numeric".equals(distribution) || ("numericLast".equals(distribution) && i == count - 1)) {
                text.append(i % 2 == 0 ? 0 : 1);
            } else if ("escaped".equals(distribution)) {
                text.append(i % 2 != 0 ? "\"\\u0074rue\"" : "\"\\u0066alse\"");
            } else {
                if ("quoted".equals(distribution)) text.append('"');
                text.append(i % 2 != 0);
                if ("quoted".equals(distribution)) text.append('"');
            }
        }
        input = text.append(']').toString();
        if (!JSON.encode(jsonicDecode()).equals(JSON.encode(jacksonDecode()))) throw new IllegalStateException("Boolean values differ");
    }
    @Benchmark public boolean[] jsonicDecode() { return json.parse(input, boolean[].class); }
    @Benchmark public boolean[] jacksonDecode() throws Exception { return jackson.readValue(input, boolean[].class); }
}
