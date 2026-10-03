package net.arnx.jsonic.benchmark;

import java.util.List;
import java.util.Map;
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
public class TypedTreeBenchmark {
    @Param({"1", "100"}) public int count;
    @Param({"object", "list", "mapArray"}) public String type;
    private JSON json;
    private JsonMapper jackson;
    private Class<?> target;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        target = "object".equals(type) ? Object.class : "list".equals(type) ? List.class : Map[].class;
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            text.append("{\"active\":true,\"id\":").append(i)
                    .append(",\"name\":\"item-").append(i)
                    .append("\",\"price\":123.5,\"tags\":[\"new\",\"sale\"]}");
        }
        input = text.append(']').toString();
        if (!JSON.encode(jsonicDecode()).equals(JSON.encode(jacksonDecode()))) {
            throw new IllegalStateException("Typed tree payloads differ");
        }
    }

    @Benchmark public Object jsonicDecode() { return json.parse(input, target); }
    @Benchmark public Object jacksonDecode() throws Exception { return jackson.readValue(input, target); }
}
