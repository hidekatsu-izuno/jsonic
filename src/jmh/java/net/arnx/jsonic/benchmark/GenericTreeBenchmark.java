package net.arnx.jsonic.benchmark;

import java.lang.reflect.Type;
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
public class GenericTreeBenchmark {
    @Param({"1", "100"}) public int count;
    @Param({"list", "mapArray"}) public String type;
    private JSON json;
    private JsonMapper jackson;
    private Type target;
    private tools.jackson.core.type.TypeReference<?> jacksonTarget;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        target = "list".equals(type) ? new net.arnx.jsonic.TypeReference<List<Map<String, Object>>>() {}.getType()
                : new net.arnx.jsonic.TypeReference<Map<String, Object>[]>() {}.getType();
        jacksonTarget = "list".equals(type) ? new tools.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
                : new tools.jackson.core.type.TypeReference<Map<String, Object>[]>() {};
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            text.append("{\"active\":true,\"id\":").append(i)
                    .append(",\"name\":\"item-").append(i)
                    .append("\",\"price\":123.5,\"tags\":[\"new\",\"sale\"]}");
        }
        input = text.append(']').toString();
        if (!JSON.encode(jsonicDecode()).equals(JSON.encode(jacksonDecode()))) {
            throw new IllegalStateException("Generic tree payloads differ");
        }
    }

    @Benchmark public Object jsonicDecode() { return json.parse(input, target); }
    @Benchmark public Object jacksonDecode() throws Exception { return jackson.readValue(input, jacksonTarget); }
}
